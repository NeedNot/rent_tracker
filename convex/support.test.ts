import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, internal } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import { INVITE_LIFETIME_MS } from "./invites";
import { PERMISSION_PRESETS } from "./permissionRules";
import {
  type Backend,
  failureOf,
  insertAccount,
  insertGroup,
  insertMembership,
  insertPayment,
  insertTenantOwingRent,
  newBackend,
  owner,
  sam,
} from "./seed.fixture";

interface MemberScene {
  t: Backend;
  groupId: Id<"groups">;
  samId: Id<"accounts">;
}

const lee = { subject: "auth|lee", email: "lee@example.com", name: "Lee" };
const NOW = Date.UTC(2026, 10, 14);
const DAY = 24 * 60 * 60 * 1000;
const AUTH_WORKER_URL = "https://auth.example.workers.dev";
const SECRET = "test-shared-secret";

function stubAuthWorker(...statuses: number[]) {
  const fetchMock = vi.fn(async (_url: string, _init: RequestInit) => new Response(null, { status: 204 }));
  for (const status of statuses) {
    fetchMock.mockImplementationOnce(async () => new Response(null, { status }));
  }
  vi.stubGlobal("fetch", fetchMock);
  vi.stubEnv("AUTH_WORKER_URL", AUTH_WORKER_URL);
  vi.stubEnv("WORKER_SHARED_SECRET", SECRET);
  return fetchMock;
}

function workerCalls(fetchMock: ReturnType<typeof stubAuthWorker>) {
  return fetchMock.mock.calls.map(([url, init]) => ({
    url,
    secret: new Headers(init.headers).get("X-Rent-Tracker-Secret"),
    body: JSON.parse(String(init.body)),
  }));
}

async function samMemberScene(): Promise<MemberScene> {
  const t = await newBackend();
  const { groupId, samId } = await t.run(async (ctx) => {
    const groupId = await insertGroup(ctx);
    const samId = await insertAccount(ctx, sam);
    await insertMembership(ctx, groupId, samId, PERMISSION_PRESETS.manager);
    return { groupId, samId };
  });
  return { t, groupId, samId };
}

async function accountRow(t: Backend, accountId: Id<"accounts">): Promise<Doc<"accounts">> {
  const account = await t.run((ctx) => ctx.db.get(accountId));
  if (account === null) {
    throw new Error("The account row is missing");
  }
  return account;
}

async function anonymize(t: Backend, accountId: Id<"accounts">): Promise<null> {
  return await t.action(internal.support.anonymizeAccount, { accountId });
}

async function ownGroup(t: Backend, accountId: Id<"accounts">, name: string): Promise<Id<"groups">> {
  return await t.run(async (ctx) => {
    const groupId = await insertGroup(ctx, { name, ownerId: accountId });
    await insertMembership(ctx, groupId, accountId, { role: "owner" });
    return groupId;
  });
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

describe("support.deletionRequests", () => {
  it("lists pending deletion requests oldest first with the groups each still owns", async () => {
    const { t, samId } = await samMemberScene();
    const leeId = await t.run((ctx) => insertAccount(ctx, lee));
    await ownGroup(t, leeId, "Oak Court");
    await ownGroup(t, leeId, "Elm Street");
    await t.run(async (ctx) => {
      await ctx.db.patch(samId, { deletionRequestedAt: NOW - DAY });
      await ctx.db.patch(leeId, { deletionRequestedAt: NOW - 2 * DAY });
    });

    const requests = await t.query(internal.support.deletionRequests, {});

    expect(requests).toEqual([
      {
        accountId: leeId,
        displayName: "Lee",
        email: lee.email,
        deletionRequestedAt: NOW - 2 * DAY,
        ownedGroups: [
          { groupId: expect.any(String), name: "Elm Street" },
          { groupId: expect.any(String), name: "Oak Court" },
        ],
      },
      { accountId: samId, displayName: "Sam", email: sam.email, deletionRequestedAt: NOW - DAY, ownedGroups: [] },
    ]);
  });

  it("leaves out accounts that never asked and accounts already deleted", async () => {
    const { t, samId } = await samMemberScene();
    await t.run((ctx) => ctx.db.patch(samId, { deletionRequestedAt: NOW - DAY, deletedAt: NOW }));

    expect(await t.query(internal.support.deletionRequests, {})).toEqual([]);
  });
});

describe("support.anonymizeAccount", () => {
  it("replaces the name, removes the email and marks the account deleted while keeping the request time", async () => {
    stubAuthWorker();
    const { t, samId } = await samMemberScene();
    await t.run((ctx) => ctx.db.patch(samId, { deletionRequestedAt: NOW - DAY }));

    await anonymize(t, samId);

    const account = await accountRow(t, samId);
    expect(account).toMatchObject({ displayName: "Deleted user", deletionRequestedAt: NOW - DAY, version: 2 });
    expect(account.email).toBeUndefined();
    expect(account.deletedAt).toEqual(expect.closeTo(NOW, -3));
  });

  it("deletes the account even when it never requested deletion", async () => {
    stubAuthWorker();
    const { t, samId } = await samMemberScene();

    await anonymize(t, samId);

    expect((await accountRow(t, samId)).deletedAt).toBeDefined();
  });

  it("deletes the account's push tokens and memberships", async () => {
    stubAuthWorker();
    const { t, samId } = await samMemberScene();
    await t.run((ctx) => ctx.db.insert("pushTokens", { accountId: samId, platform: "android", token: "fcm-sam" }));

    await anonymize(t, samId);

    const { tokens, memberships } = await t.run(async (ctx) => ({
      tokens: await ctx.db.query("pushTokens").collect(),
      memberships: await ctx.db
        .query("memberships")
        .withIndex("by_account", (q) => q.eq("accountId", samId))
        .collect(),
    }));
    expect(tokens).toEqual([]);
    expect(memberships).toEqual([]);
  });

  it("revokes the pending invites the account sent and leaves settled ones alone", async () => {
    stubAuthWorker();
    const { t, groupId, samId } = await samMemberScene();
    const invite = {
      groupId,
      permissions: PERMISSION_PRESETS.viewer,
      invitedBy: samId,
      expiresAt: NOW + INVITE_LIFETIME_MS,
    };
    const { pendingId, acceptedId } = await t.run(async (ctx) => ({
      pendingId: await ctx.db.insert("invites", { ...invite, email: lee.email }),
      acceptedId: await ctx.db.insert("invites", { ...invite, email: "kim@example.com", acceptedAt: NOW - DAY }),
    }));

    await anonymize(t, samId);

    const [pending, accepted] = await t.run((ctx) => Promise.all([ctx.db.get(pendingId), ctx.db.get(acceptedId)]));
    expect(pending?.revokedAt).toEqual(expect.closeTo(NOW, -3));
    expect(accepted?.revokedAt).toBeUndefined();
  });

  it("keeps entries pointing at the account so the tenant screen names it Deleted user", async () => {
    stubAuthWorker();
    const { t, groupId, samId } = await samMemberScene();
    const tenantId = await t.run(async (ctx) => {
      const tenantId = await insertTenantOwingRent(ctx, groupId, 150000);
      await insertPayment(ctx, tenantId, { amountCents: 150000, dateReceived: "2026-03-02" }, { loggedBy: samId });
      return tenantId;
    });

    await anonymize(t, samId);

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: NOW });
    expect(screen.payments[0]?.loggedBy).toBe(samId);
    expect(screen.accounts).toContainEqual({ accountId: samId, displayName: "Deleted user" });
  });

  it("asks the auth worker to delete the sign-in user with the shared secret", async () => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();

    await anonymize(t, samId);

    expect(workerCalls(fetchMock)).toEqual([
      { url: `${AUTH_WORKER_URL}/internal/users/delete`, secret: SECRET, body: { userId: sam.subject } },
    ]);
  });

  it("refuses an account that still owns a group, archived ones included, and calls no worker", async () => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();
    const archivedId = await ownGroup(t, samId, "Oak Court");
    await t.run((ctx) => ctx.db.patch(archivedId, { archivedAt: NOW - DAY }));

    const failure = await failureOf(anonymize(t, samId));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "Transfer ownership of Oak Court first." });
    expect(fetchMock).not.toHaveBeenCalled();
    expect((await accountRow(t, samId)).deletedAt).toBeUndefined();
  });

  it("does nothing when run again for an account already deleted", async () => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();
    await anonymize(t, samId);
    const deleted = await accountRow(t, samId);

    await anonymize(t, samId);

    expect(fetchMock).toHaveBeenCalledOnce();
    expect(await accountRow(t, samId)).toEqual(deleted);
  });

  it("leaves the account untouched when the auth worker fails, and succeeds on a rerun", async () => {
    stubAuthWorker(500);
    const { t, samId } = await samMemberScene();
    const before = await accountRow(t, samId);

    await expect(anonymize(t, samId)).rejects.toThrow("HTTP 500");
    expect(await accountRow(t, samId)).toEqual(before);

    await anonymize(t, samId);
    expect((await accountRow(t, samId)).deletedAt).toBeDefined();
  });

  it("fails loudly when the auth worker URL is not configured", async () => {
    const fetchMock = stubAuthWorker();
    vi.stubEnv("AUTH_WORKER_URL", "");
    const { t, samId } = await samMemberScene();

    await expect(anonymize(t, samId)).rejects.toThrow("AUTH_WORKER_URL");
    expect(fetchMock).not.toHaveBeenCalled();
    expect((await accountRow(t, samId)).deletedAt).toBeUndefined();
  });

  it("signs out a token that still names the deleted account", async () => {
    stubAuthWorker();
    const { t, samId } = await samMemberScene();

    await anonymize(t, samId);

    expect(await failureOf(t.withIdentity(sam).query(api.accounts.me, {}))).toMatchObject({ code: "NotSignedIn" });
  });

  it("gives the same email a new empty account when it signs up again", async () => {
    stubAuthWorker();
    const { t, samId } = await samMemberScene();
    await anonymize(t, samId);

    const fresh = await t
      .withIdentity({ subject: "auth|sam-again", email: sam.email })
      .mutation(api.accounts.ensure, {});

    expect(fresh).toMatchObject({ email: sam.email, created: true });
    expect(fresh.accountId).not.toBe(samId);
  });
});

describe("support.changeAccountEmail", () => {
  it("saves the trimmed lower-cased email once the auth worker accepts it", async () => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();

    const email = await t.action(internal.support.changeAccountEmail, { accountId: samId, newEmail: " Sam@New.COM " });

    expect(email).toBe("sam@new.com");
    expect(await accountRow(t, samId)).toMatchObject({ email: "sam@new.com", version: 2 });
    expect(workerCalls(fetchMock)).toEqual([
      {
        url: `${AUTH_WORKER_URL}/internal/users/change-email`,
        secret: SECRET,
        body: { userId: sam.subject, email: "sam@new.com" },
      },
    ]);
  });

  it("refuses an email that already belongs to another account without calling the worker", async () => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();

    const failure = await failureOf(
      t.action(internal.support.changeAccountEmail, { accountId: samId, newEmail: owner.email }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "This email already belongs to another account.",
    });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("refuses with the same message when the auth worker says another sign-in user has the email", async () => {
    stubAuthWorker(409);
    const { t, samId } = await samMemberScene();

    const failure = await failureOf(
      t.action(internal.support.changeAccountEmail, { accountId: samId, newEmail: "taken@example.com" }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "This email already belongs to another account.",
    });
    expect((await accountRow(t, samId)).email).toBe(sam.email);
  });

  it("fails with the status and keeps the old email when the auth worker does not know the user", async () => {
    stubAuthWorker(404);
    const { t, samId } = await samMemberScene();

    await expect(
      t.action(internal.support.changeAccountEmail, { accountId: samId, newEmail: "sam@new.com" }),
    ).rejects.toThrow("HTTP 404");
    expect((await accountRow(t, samId)).email).toBe(sam.email);
  });

  it.each([
    ["an email without a domain", "sam@", "Enter a valid email address."],
    ["an email over 100 characters", `${"s".repeat(95)}@x.com`, "Email must be 100 characters or fewer."],
  ])("refuses %s", async (_case, newEmail, message) => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();

    const failure = await failureOf(t.action(internal.support.changeAccountEmail, { accountId: samId, newEmail }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("refuses a deleted account", async () => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();
    await anonymize(t, samId);
    fetchMock.mockClear();

    const failure = await failureOf(
      t.action(internal.support.changeAccountEmail, { accountId: samId, newEmail: "sam@new.com" }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This account was deleted." });
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe("support.recoverAccount", () => {
  it("changes the email and then signs out every session of the account", async () => {
    const fetchMock = stubAuthWorker();
    const { t, samId } = await samMemberScene();

    const email = await t.action(internal.support.recoverAccount, { accountId: samId, newEmail: "sam@new.com" });

    expect(email).toBe("sam@new.com");
    expect((await accountRow(t, samId)).email).toBe("sam@new.com");
    expect(workerCalls(fetchMock)).toEqual([
      {
        url: `${AUTH_WORKER_URL}/internal/users/change-email`,
        secret: SECRET,
        body: { userId: sam.subject, email: "sam@new.com" },
      },
      { url: `${AUTH_WORKER_URL}/internal/users/revoke-sessions`, secret: SECRET, body: { userId: sam.subject } },
    ]);
  });

  it("revokes no sessions when the email change is refused", async () => {
    const fetchMock = stubAuthWorker(409);
    const { t, samId } = await samMemberScene();

    await failureOf(t.action(internal.support.recoverAccount, { accountId: samId, newEmail: "taken@example.com" }));

    expect(fetchMock).toHaveBeenCalledOnce();
  });
});
