import type { WithoutSystemFields } from "convex/server";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, internal } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import { INVITE_LIFETIME_MS } from "./invites";
import { PERMISSION_PRESETS } from "./permissionRules";
import {
  type Backend,
  failureOf,
  insertAccount,
  insertMembership,
  newBackend,
  owner,
  sam,
  seedGroup,
} from "./seed.fixture";

type MembershipFields = Partial<WithoutSystemFields<Doc<"memberships">>>;

interface InviteSetup {
  t: Backend;
  asLee: Backend;
  groupId: Id<"groups">;
}

const lee = { subject: "auth|lee", email: "lee@example.com", name: "Lee" };
const { viewer, bookkeeper, manager } = PERMISSION_PRESETS;
const NOW = Date.UTC(2026, 10, 14);
const DAY = 24 * 60 * 60 * 1000;
const WORKER_URL = "https://email.example.workers.dev";
const SECRET = "test-shared-secret";
const INVITE_GONE = { code: "NotFound", message: "This invite no longer exists." };
const GROUP_GONE = { code: "NotFound", message: "This group no longer exists." };

// convex-test moves each transaction's clock forward by a millisecond, so stored times land just after the fake time.
const around = (time: number) => expect.closeTo(time, -3);

async function inviteSetup(): Promise<InviteSetup> {
  const t = await newBackend();
  const groupId = await seedGroup(t);
  await t.run((ctx) => insertAccount(ctx, lee));
  return { t, asLee: t.withIdentity(lee), groupId };
}

async function signInSam(t: Backend, groupId: Id<"groups">, fields: MembershipFields): Promise<Backend> {
  await t.run(async (ctx) => insertMembership(ctx, groupId, await insertAccount(ctx, sam), fields));
  return t.withIdentity(sam);
}

async function inviteLee(t: Backend, groupId: Id<"groups">, permissions = viewer): Promise<Id<"invites">> {
  return await t.mutation(api.invites.send, { groupId, email: lee.email, permissions });
}

async function inviteRow(t: Backend, inviteId: Id<"invites">): Promise<Doc<"invites">> {
  const invite = await t.run((ctx) => ctx.db.get(inviteId));
  if (invite === null) {
    throw new Error("The invite row is missing");
  }
  return invite;
}

async function inviteRows(t: Backend): Promise<Doc<"invites">[]> {
  return await t.run((ctx) => ctx.db.query("invites").collect());
}

async function membershipsOf(t: Backend, groupId: Id<"groups">): Promise<Doc<"memberships">[]> {
  return await t.run((ctx) =>
    ctx.db
      .query("memberships")
      .withIndex("by_group_account", (q) => q.eq("groupId", groupId))
      .collect(),
  );
}

async function scheduledEmails(t: Backend): Promise<unknown[]> {
  const jobs = await t.run((ctx) => ctx.db.system.query("_scheduled_functions").collect());
  return jobs.filter((job) => job.name.startsWith("inviteEmail")).map((job) => job.args[0]);
}

async function archive(t: Backend, groupId: Id<"groups">): Promise<void> {
  await t.run((ctx) => ctx.db.patch(groupId, { archivedAt: NOW - DAY }));
}

function stubEmailWorker(status = 200) {
  const fetchMock = vi.fn(async (_url: string, _init: RequestInit) => new Response(null, { status }));
  vi.stubGlobal("fetch", fetchMock);
  vi.stubEnv("EMAIL_WORKER_URL", WORKER_URL);
  vi.stubEnv("WORKER_SHARED_SECRET", SECRET);
  return fetchMock;
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(NOW);
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("invites.send", () => {
  it("stores a pending invite for the normalized email that expires in 14 days", async () => {
    const { t, groupId } = await inviteSetup();

    const inviteId = await t.mutation(api.invites.send, { groupId, email: "  Lee@Example.COM ", permissions: manager });

    const ownerId = await t.run((ctx) => insertAccount(ctx, owner));
    expect(await inviteRow(t, inviteId)).toMatchObject({
      groupId,
      email: "lee@example.com",
      permissions: manager,
      invitedBy: ownerId,
      expiresAt: around(NOW + 14 * DAY),
    });
    expect(INVITE_LIFETIME_MS).toBe(14 * DAY);
  });

  it("schedules the invite email", async () => {
    const { t, groupId } = await inviteSetup();

    const inviteId = await inviteLee(t, groupId);

    expect(await scheduledEmails(t)).toEqual([{ inviteId }]);
  });

  it("refreshes a pending invite's permissions, inviter and 14 days when the email is invited again", async () => {
    const { t, groupId } = await inviteSetup();
    const asSam = await signInSam(t, groupId, { members: "addEdit", payments: "addEdit", debts: "addEdit" });
    const firstId = await inviteLee(t, groupId, viewer);
    vi.setSystemTime(NOW + 3 * DAY);

    const secondId = await inviteLee(asSam, groupId, bookkeeper);

    const samId = await t.run((ctx) => insertAccount(ctx, sam));
    expect(secondId).toBe(firstId);
    expect(await inviteRows(t)).toHaveLength(1);
    expect(await inviteRow(t, firstId)).toMatchObject({
      permissions: bookkeeper,
      invitedBy: samId,
      expiresAt: around(NOW + 17 * DAY),
    });
    expect(await scheduledEmails(t)).toHaveLength(2);
  });

  it("creates a new invite when the earlier one is no longer pending", async () => {
    const { t, groupId } = await inviteSetup();
    const revokedId = await inviteLee(t, groupId);
    await t.mutation(api.invites.revoke, { inviteId: revokedId });

    const newId = await inviteLee(t, groupId);

    expect(newId).not.toBe(revokedId);
    expect(await inviteRows(t)).toHaveLength(2);
  });

  it("rejects an email that already belongs to a member of the group", async () => {
    const { t, groupId } = await inviteSetup();
    await signInSam(t, groupId, {});

    const failure = await failureOf(
      t.mutation(api.invites.send, { groupId, email: "SAM@example.com", permissions: viewer }),
    );

    expect(failure).toEqual({
      code: "ValidationFailed",
      message: "Already a member of this group.",
      details: { field: "email" },
    });
  });

  it.each([
    ["an empty email", "   ", "Email is required."],
    ["an email without a domain", "lee@", "Enter a valid email address."],
    ["an email with spaces", "lee smith@example.com", "Enter a valid email address."],
    ["an email over 100 characters", `${"a".repeat(95)}@example.com`, "Email must be 100 characters or fewer."],
  ])("rejects %s on the email field", async (_case, email, message) => {
    const { t, groupId } = await inviteSetup();

    const failure = await failureOf(t.mutation(api.invites.send, { groupId, email, permissions: viewer }));

    expect(failure).toEqual({ code: "ValidationFailed", message, details: { field: "email" } });
  });

  it("refuses to grant permissions above the inviter's own", async () => {
    const { t, groupId } = await inviteSetup();
    const asSam = await signInSam(t, groupId, { members: "addEdit" });

    const failure = await failureOf(inviteLee(asSam, groupId, bookkeeper));

    expect(failure.code).toBe("PermissionDenied");
    expect(await inviteRows(t)).toEqual([]);
  });

  it("lets a member with Members: add & edit invite at or below their own level", async () => {
    const { t, groupId } = await inviteSetup();
    const asSam = await signInSam(t, groupId, { members: "addEdit" });

    await inviteLee(asSam, groupId, { ...viewer, members: "addEdit" });

    expect(await inviteRows(t)).toHaveLength(1);
  });

  it("denies a member who can only read Members", async () => {
    const { t, groupId } = await inviteSetup();
    const asSam = await signInSam(t, groupId, { members: "read" });

    const failure = await failureOf(inviteLee(asSam, groupId));

    expect(failure.code).toBe("PermissionDenied");
  });

  it("rejects an invite into an archived group", async () => {
    const { t, groupId } = await inviteSetup();
    await archive(t, groupId);

    const failure = await failureOf(inviteLee(t, groupId));

    expect(failure).toMatchObject(GROUP_GONE);
  });
});

describe("invites.revoke", () => {
  it("revokes a pending invite so it no longer shows as pending", async () => {
    const { t, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);

    await t.mutation(api.invites.revoke, { inviteId });

    expect((await inviteRow(t, inviteId)).revokedAt).toEqual(around(NOW));
    expect((await t.query(api.members.screen, { groupId, asOf: NOW })).invites).toEqual([]);
  });

  it("rejects an invite that is no longer pending", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    await asLee.mutation(api.invites.accept, { inviteId });

    const failure = await failureOf(t.mutation(api.invites.revoke, { inviteId }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This invite is no longer pending." });
  });

  it("denies a member who can only read Members", async () => {
    const { t, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    const asSam = await signInSam(t, groupId, { members: "read" });

    const failure = await failureOf(asSam.mutation(api.invites.revoke, { inviteId }));

    expect(failure.code).toBe("PermissionDenied");
  });
});

describe("invites.accept", () => {
  it("adds the invitee as a member with exactly the invited permissions", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId, bookkeeper);

    await asLee.mutation(api.invites.accept, { inviteId });

    const leeId = await t.run((ctx) => insertAccount(ctx, lee));
    const leeMembership = (await membershipsOf(t, groupId)).find((membership) => membership.accountId === leeId);
    expect(leeMembership).toMatchObject({ role: "member", ...bookkeeper, notificationsOn: true, version: 1 });
    expect((await inviteRow(t, inviteId)).acceptedAt).toEqual(around(NOW));
  });

  it("hides the invite from someone signed in with a different email", async () => {
    const { t, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    await t.run((ctx) => insertAccount(ctx, sam));

    const failure = await failureOf(t.withIdentity(sam).mutation(api.invites.accept, { inviteId }));

    expect(failure).toMatchObject(INVITE_GONE);
  });

  it("treats an expired invite as gone", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    vi.setSystemTime(NOW + 15 * DAY);

    const failure = await failureOf(asLee.mutation(api.invites.accept, { inviteId }));

    expect(failure).toMatchObject(INVITE_GONE);
  });

  it("treats a revoked invite as gone", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    await t.mutation(api.invites.revoke, { inviteId });

    const failure = await failureOf(asLee.mutation(api.invites.accept, { inviteId }));

    expect(failure).toMatchObject(INVITE_GONE);
  });

  it("treats a declined invite as gone", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    await asLee.mutation(api.invites.decline, { inviteId });

    const failure = await failureOf(asLee.mutation(api.invites.accept, { inviteId }));

    expect(failure).toMatchObject(INVITE_GONE);
  });

  it("rejects an invite into an archived group", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    await archive(t, groupId);

    const failure = await failureOf(asLee.mutation(api.invites.accept, { inviteId }));

    expect(failure).toMatchObject(GROUP_GONE);
  });

  it("only marks the invite accepted when the invitee is already a member", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId, bookkeeper);
    await t.run(async (ctx) => insertMembership(ctx, groupId, await insertAccount(ctx, lee)));

    await asLee.mutation(api.invites.accept, { inviteId });

    const memberships = await membershipsOf(t, groupId);
    expect(memberships).toHaveLength(2);
    expect(memberships[1]).toMatchObject(viewer);
    expect((await inviteRow(t, inviteId)).acceptedAt).toEqual(around(NOW));
  });
});

describe("invites.decline", () => {
  it("records the decline without adding a member", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);

    await asLee.mutation(api.invites.decline, { inviteId });

    expect((await inviteRow(t, inviteId)).declinedAt).toEqual(around(NOW));
    expect(await membershipsOf(t, groupId)).toHaveLength(1);
  });

  it("removes a declined invite from the group's pending invites", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);

    await asLee.mutation(api.invites.decline, { inviteId });

    expect((await t.query(api.members.screen, { groupId, asOf: NOW })).invites).toEqual([]);
  });

  it("hides the invite from someone signed in with a different email", async () => {
    const { t, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);

    const failure = await failureOf(t.mutation(api.invites.decline, { inviteId }));

    expect(failure).toMatchObject(INVITE_GONE);
  });
});

describe("home.screen invitations", () => {
  it("lists the caller's pending invites with the group and inviter names", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId, bookkeeper);

    const { invitations } = await asLee.query(api.home.screen, { asOf: NOW });

    expect(invitations).toEqual([
      {
        inviteId,
        groupId,
        groupName: "1st Street",
        invitedByName: "Dana",
        permissions: bookkeeper,
        expiresAt: around(NOW + 14 * DAY),
      },
    ]);
  });

  it("leaves out invites that were accepted, declined or sent to someone else", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    const otherGroupId = await seedGroup(t);
    const declinedId = await inviteLee(t, otherGroupId);
    await asLee.mutation(api.invites.decline, { inviteId: declinedId });
    const acceptedId = await inviteLee(t, groupId);
    await asLee.mutation(api.invites.accept, { inviteId: acceptedId });
    await t.mutation(api.invites.send, { groupId, email: "someone@example.com", permissions: viewer });

    const { invitations } = await asLee.query(api.home.screen, { asOf: NOW });

    expect(invitations).toEqual([]);
  });

  it("hides an invite once it has expired", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    await inviteLee(t, groupId);

    const { invitations } = await asLee.query(api.home.screen, { asOf: NOW + 15 * DAY });

    expect(invitations).toEqual([]);
  });

  it("hides an invite into an archived group", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    await inviteLee(t, groupId);
    await archive(t, groupId);

    const { invitations } = await asLee.query(api.home.screen, { asOf: NOW });

    expect(invitations).toEqual([]);
  });

  it("names a deleted inviter as Deleted user", async () => {
    const { t, asLee, groupId } = await inviteSetup();
    await inviteLee(t, groupId);
    await t.run(async (ctx) => {
      await ctx.db.patch(await insertAccount(ctx, owner), { displayName: "Deleted user", email: undefined });
    });

    const { invitations } = await asLee.query(api.home.screen, { asOf: NOW });

    expect(invitations[0]?.invitedByName).toBe("Deleted user");
  });
});

describe("inviteEmail.send", () => {
  it("posts the invite to the email worker with the shared secret", async () => {
    const fetchMock = stubEmailWorker();
    const { t, groupId } = await inviteSetup();
    await inviteLee(t, groupId);

    await t.finishAllScheduledFunctions(vi.runAllTimers);

    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(url).toBe(`${WORKER_URL}/invites`);
    expect(init?.method).toBe("POST");
    expect(new Headers(init?.headers).get("X-Rent-Tracker-Secret")).toBe(SECRET);
    expect(JSON.parse(String(init?.body))).toEqual({
      email: "lee@example.com",
      inviterName: "Dana",
      groupName: "1st Street",
      expiresAt: around(NOW + 14 * DAY),
    });
  });

  it("sends nothing when the invite was revoked before the email went out", async () => {
    const fetchMock = stubEmailWorker();
    const { t, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);
    await t.mutation(api.invites.revoke, { inviteId });

    await t.action(internal.inviteEmail.send, { inviteId });

    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("fails visibly when the worker URL is not configured", async () => {
    const fetchMock = stubEmailWorker();
    vi.stubEnv("EMAIL_WORKER_URL", "");
    const { t, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);

    await expect(t.action(internal.inviteEmail.send, { inviteId })).rejects.toThrow("EMAIL_WORKER_URL");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("fails visibly when the worker refuses the email", async () => {
    stubEmailWorker(401);
    const { t, groupId } = await inviteSetup();
    const inviteId = await inviteLee(t, groupId);

    await expect(t.action(internal.inviteEmail.send, { inviteId })).rejects.toThrow("HTTP 401");
  });
});
