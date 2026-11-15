import type { WithoutSystemFields } from "convex/server";
import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import { OWNER_PERMISSIONS, PERMISSION_PRESETS } from "./permissionRules";
import {
  type Backend,
  failureOf,
  insertAccount,
  insertMembership,
  insertPayment,
  insertTenant,
  newBackend,
  owner,
  sam,
  seedGroup,
} from "./seed.fixture";

type MembershipFields = Partial<WithoutSystemFields<Doc<"memberships">>>;

interface GroupWithSam {
  t: Backend;
  asSam: Backend;
  groupId: Id<"groups">;
  ownerMembershipId: Id<"memberships">;
  samMembershipId: Id<"memberships">;
}

const lee = { subject: "auth|lee", email: "lee@example.com", name: "Lee" };
const { viewer, bookkeeper, manager } = PERMISSION_PRESETS;
const NOW = Date.UTC(2026, 10, 14);
const DAY = 24 * 60 * 60 * 1000;

async function groupWithSam(samFields: MembershipFields = {}): Promise<GroupWithSam> {
  const t = await newBackend();
  const groupId = await seedGroup(t);
  const { ownerMembershipId, samMembershipId } = await t.run(async (ctx) => ({
    ownerMembershipId: await membershipIdOf(ctx, groupId, await insertAccount(ctx, owner)),
    samMembershipId: await insertMembership(ctx, groupId, await insertAccount(ctx, sam), samFields),
  }));
  return { t, asSam: t.withIdentity(sam), groupId, ownerMembershipId, samMembershipId };
}

async function membershipIdOf(
  ctx: MutationCtx,
  groupId: Id<"groups">,
  accountId: Id<"accounts">,
): Promise<Id<"memberships">> {
  const membership = await ctx.db
    .query("memberships")
    .withIndex("by_group_account", (q) => q.eq("groupId", groupId).eq("accountId", accountId))
    .unique();
  if (membership === null) {
    throw new Error("The membership row is missing");
  }
  return membership._id;
}

async function addLee(t: Backend, groupId: Id<"groups">, fields: MembershipFields = {}): Promise<Id<"memberships">> {
  return await t.run(async (ctx) => insertMembership(ctx, groupId, await insertAccount(ctx, lee), fields));
}

async function membershipRow(t: Backend, membershipId: Id<"memberships">): Promise<Doc<"memberships"> | null> {
  return await t.run((ctx) => ctx.db.get(membershipId));
}

describe("members.screen", () => {
  it("orders the owner first, then members in the order they joined", async () => {
    const { t, groupId, ownerMembershipId, samMembershipId } = await groupWithSam();
    const leeMembershipId = await addLee(t, groupId);
    await t.mutation(api.members.transferOwnership, { groupId, toMembershipId: leeMembershipId });

    const screen = await t.withIdentity(lee).query(api.members.screen, { groupId, asOf: NOW });

    expect(screen.memberships.map((membership) => membership._id)).toEqual([
      leeMembershipId,
      ownerMembershipId,
      samMembershipId,
    ]);
    expect(screen.memberProfiles.map((profile) => profile.displayName)).toEqual(["Lee", "Dana", "Sam"]);
  });

  it("returns every membership with the members' names and emails", async () => {
    const { t, groupId, ownerMembershipId, samMembershipId } = await groupWithSam();

    const screen = await t.query(api.members.screen, { groupId, asOf: NOW });

    expect(screen.memberships.map((membership) => membership._id)).toEqual([ownerMembershipId, samMembershipId]);
    expect(screen.memberProfiles).toMatchObject([
      { displayName: "Dana", email: "dana@example.com" },
      { displayName: "Sam", email: "sam@example.com" },
    ]);
  });

  it("shows a deleted account by its anonymized name without an email", async () => {
    const { t, groupId } = await groupWithSam();
    await t.run(async (ctx) => {
      const samId = await insertAccount(ctx, sam);
      await ctx.db.patch(samId, { displayName: "Deleted user", email: undefined, deletedAt: NOW });
    });

    const { memberProfiles } = await t.query(api.members.screen, { groupId, asOf: NOW });

    expect(memberProfiles[1]).toEqual({ accountId: expect.any(String), displayName: "Deleted user" });
  });

  it("returns only pending invites", async () => {
    const { t, groupId } = await groupWithSam();
    const invitedBy = await t.run((ctx) => insertAccount(ctx, owner));
    const invite = { groupId, permissions: viewer, invitedBy, expiresAt: NOW + DAY };
    await t.run(async (ctx) => {
      await ctx.db.insert("invites", { ...invite, email: "pending@example.com" });
      await ctx.db.insert("invites", { ...invite, email: "accepted@example.com", acceptedAt: NOW - DAY });
      await ctx.db.insert("invites", { ...invite, email: "revoked@example.com", revokedAt: NOW - DAY });
      await ctx.db.insert("invites", { ...invite, email: "expired@example.com", expiresAt: NOW - 1 });
    });

    const { invites } = await t.query(api.members.screen, { groupId, asOf: NOW });

    expect(invites.map((pending) => pending.email)).toEqual(["pending@example.com"]);
  });

  it("is open to a read-only member", async () => {
    const { asSam, groupId } = await groupWithSam();

    const { memberships } = await asSam.query(api.members.screen, { groupId, asOf: NOW });

    expect(memberships).toHaveLength(2);
  });

  it("denies someone outside the group", async () => {
    const { t, groupId } = await groupWithSam();
    await t.run((ctx) => insertAccount(ctx, lee));

    const failure = await failureOf(t.withIdentity(lee).query(api.members.screen, { groupId, asOf: NOW }));

    expect(failure.code).toBe("PermissionDenied");
  });
});

describe("members.mine", () => {
  it("returns the caller's own membership", async () => {
    const { asSam, groupId, samMembershipId } = await groupWithSam(bookkeeper);

    const mine = await asSam.query(api.members.mine, { groupId });

    expect(mine).toMatchObject({ _id: samMembershipId, role: "member", ...bookkeeper });
  });

  it("denies someone outside the group", async () => {
    const { t, groupId } = await groupWithSam();
    await t.run((ctx) => insertAccount(ctx, lee));

    const failure = await failureOf(t.withIdentity(lee).query(api.members.mine, { groupId }));

    expect(failure.code).toBe("PermissionDenied");
  });
});

describe("members.updatePermissions", () => {
  it("lets the owner change a member's permissions and stamps the edit", async () => {
    const { t, samMembershipId } = await groupWithSam();
    const ownerId = await t.run((ctx) => insertAccount(ctx, owner));

    await t.mutation(api.members.updatePermissions, {
      membershipId: samMembershipId,
      expectedVersion: 1,
      permissions: manager,
    });

    expect(await membershipRow(t, samMembershipId)).toMatchObject({
      ...manager,
      version: 2,
      lastEditedBy: ownerId,
      lastEditedAt: expect.any(Number),
    });
  });

  it("lets Members: full grant levels it holds", async () => {
    const { asSam, groupId } = await groupWithSam({ ...manager, members: "full" });
    const leeMembershipId = await addLee(asSam, groupId);

    await asSam.mutation(api.members.updatePermissions, {
      membershipId: leeMembershipId,
      expectedVersion: 1,
      permissions: bookkeeper,
    });

    expect(await membershipRow(asSam, leeMembershipId)).toMatchObject({ ...bookkeeper, version: 2 });
  });

  it.each<[string, MembershipFields]>([
    ["Members: read", { members: "read" }],
    ["Members: add & edit", { members: "addEdit" }],
  ])("denies %s", async (_level, samFields) => {
    const { asSam, groupId } = await groupWithSam(samFields);
    const leeMembershipId = await addLee(asSam, groupId);

    const failure = await failureOf(
      asSam.mutation(api.members.updatePermissions, {
        membershipId: leeMembershipId,
        expectedVersion: 1,
        permissions: viewer,
      }),
    );

    expect(failure.code).toBe("PermissionDenied");
  });

  it("never changes the owner's permissions", async () => {
    const { asSam, ownerMembershipId } = await groupWithSam({ ...manager, members: "full" });

    const failure = await failureOf(
      asSam.mutation(api.members.updatePermissions, {
        membershipId: ownerMembershipId,
        expectedVersion: 1,
        permissions: viewer,
      }),
    );

    expect(failure).toMatchObject({ code: "PermissionDenied", message: "Nobody can change the owner's permissions." });
  });

  it.each<[string, MembershipFields, MembershipFields]>([
    ["unchanged levels above the editor's", { tenants: "full" }, { tenants: "full", payments: "addEdit" }],
    ["a lowered level above the editor's", { tenants: "full" }, { tenants: "addEdit" }],
    ["the switch left on when the editor lacks it", { editOthers: true }, { editOthers: true, debts: "addEdit" }],
  ])("allows %s", async (_case, leeBefore, leeAfter) => {
    const { asSam, groupId } = await groupWithSam({ ...bookkeeper, members: "full" });
    const leeMembershipId = await addLee(asSam, groupId, leeBefore);

    await asSam.mutation(api.members.updatePermissions, {
      membershipId: leeMembershipId,
      expectedVersion: 1,
      permissions: { ...viewer, ...leeAfter },
    });

    expect(await membershipRow(asSam, leeMembershipId)).toMatchObject(leeAfter);
  });

  it.each<[string, MembershipFields]>([
    ["a category above the editor's", { tenants: "addEdit" }],
    ["full where the editor has add & edit", { payments: "full" }],
    ["the switch turned on when the editor lacks it", { editOthers: true }],
    ["settings: full the editor lacks", { settings: "full" }],
  ])("denies raising %s", async (_case, leeAfter) => {
    const { asSam, groupId } = await groupWithSam({ ...bookkeeper, members: "full" });
    const leeMembershipId = await addLee(asSam, groupId);

    const failure = await failureOf(
      asSam.mutation(api.members.updatePermissions, {
        membershipId: leeMembershipId,
        expectedVersion: 1,
        permissions: { ...viewer, ...leeAfter },
      }),
    );

    expect(failure).toMatchObject({
      code: "PermissionDenied",
      message: "You can only grant permissions you hold yourself.",
    });
    expect(await membershipRow(asSam, leeMembershipId)).toMatchObject({ ...viewer, version: 1 });
  });

  it("rejects a save made against an older version", async () => {
    const { t, samMembershipId } = await groupWithSam();
    await t.mutation(api.members.updatePermissions, {
      membershipId: samMembershipId,
      expectedVersion: 1,
      permissions: bookkeeper,
    });

    const failure = await failureOf(
      t.mutation(api.members.updatePermissions, {
        membershipId: samMembershipId,
        expectedVersion: 1,
        permissions: manager,
      }),
    );

    expect(failure).toMatchObject({
      code: "EditConflict",
      message: "Dana changed this member's permissions while you were editing.",
      details: { currentVersion: 2 },
    });
  });
});

describe("members.remove", () => {
  it("lets Members: add & edit remove a member at or below its own permissions", async () => {
    const { asSam, groupId } = await groupWithSam({ ...bookkeeper, members: "addEdit" });
    const leeMembershipId = await addLee(asSam, groupId, bookkeeper);

    await asSam.mutation(api.members.remove, { membershipId: leeMembershipId });

    expect(await membershipRow(asSam, leeMembershipId)).toBeNull();
  });

  it("denies Members: add & edit removing a member with any higher permission", async () => {
    const { asSam, groupId } = await groupWithSam({ ...bookkeeper, members: "addEdit" });
    const leeMembershipId = await addLee(asSam, groupId, { editOthers: true });

    const failure = await failureOf(asSam.mutation(api.members.remove, { membershipId: leeMembershipId }));

    expect(failure.code).toBe("PermissionDenied");
    expect(await membershipRow(asSam, leeMembershipId)).not.toBeNull();
  });

  it("lets Members: full remove any non-owner member", async () => {
    const { asSam, groupId } = await groupWithSam({ ...viewer, members: "full" });
    const leeMembershipId = await addLee(asSam, groupId, { ...manager, members: "full", settings: "full" });

    await asSam.mutation(api.members.remove, { membershipId: leeMembershipId });

    expect(await membershipRow(asSam, leeMembershipId)).toBeNull();
  });

  it("denies Members: read", async () => {
    const { asSam, groupId } = await groupWithSam(manager);
    const leeMembershipId = await addLee(asSam, groupId);

    const failure = await failureOf(asSam.mutation(api.members.remove, { membershipId: leeMembershipId }));

    expect(failure.code).toBe("PermissionDenied");
  });

  it("never removes the owner", async () => {
    const { asSam, ownerMembershipId } = await groupWithSam({ members: "full" });

    const failure = await failureOf(asSam.mutation(api.members.remove, { membershipId: ownerMembershipId }));

    expect(failure).toMatchObject({ code: "PermissionDenied", message: "Nobody can remove the group's owner." });
  });

  it("points a member removing themselves to leaving", async () => {
    const { asSam, samMembershipId } = await groupWithSam({ members: "full" });

    const failure = await failureOf(asSam.mutation(api.members.remove, { membershipId: samMembershipId }));

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "To remove yourself, leave the group instead.",
    });
  });

  it("keeps the removed member's payments attributed to them and leaves invites alone", async () => {
    const { t, groupId, samMembershipId } = await groupWithSam();
    const { samId, paymentId, inviteId } = await t.run(async (ctx) => {
      const samId = await insertAccount(ctx, sam);
      const tenantId = await insertTenant(ctx, groupId);
      const paymentId = await insertPayment(
        ctx,
        tenantId,
        { amountCents: 50000, dateReceived: "2026-03-02" },
        { loggedBy: samId },
      );
      const inviteId = await ctx.db.insert("invites", {
        groupId,
        email: "next@example.com",
        permissions: viewer,
        invitedBy: samId,
        expiresAt: NOW + DAY,
      });
      return { samId, paymentId, inviteId };
    });

    await t.mutation(api.members.remove, { membershipId: samMembershipId });

    expect(await t.run((ctx) => ctx.db.get(paymentId))).toMatchObject({ loggedBy: samId });
    expect(await t.run((ctx) => ctx.db.get(inviteId))).not.toBeNull();
  });
});

describe("members.leave", () => {
  it("removes the caller's own membership", async () => {
    const { asSam, groupId, samMembershipId } = await groupWithSam();

    await asSam.mutation(api.members.leave, { groupId });

    expect(await membershipRow(asSam, samMembershipId)).toBeNull();
  });

  it("asks the owner to transfer ownership first", async () => {
    const { t, groupId, ownerMembershipId } = await groupWithSam();

    const failure = await failureOf(t.mutation(api.members.leave, { groupId }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "Transfer ownership before leaving." });
    expect(await membershipRow(t, ownerMembershipId)).not.toBeNull();
  });
});

describe("members.transferOwnership", () => {
  it("makes the target the owner and gives the old owner the Manager preset", async () => {
    const { t, groupId, ownerMembershipId, samMembershipId } = await groupWithSam();

    await t.mutation(api.members.transferOwnership, { groupId, toMembershipId: samMembershipId });

    expect(await membershipRow(t, samMembershipId)).toMatchObject({ role: "owner", ...OWNER_PERMISSIONS, version: 2 });
    expect(await membershipRow(t, ownerMembershipId)).toMatchObject({ role: "member", ...manager, version: 2 });
  });

  it("moves the group's ownerId to the new owner", async () => {
    const { t, groupId, samMembershipId } = await groupWithSam();
    const samId = await t.run((ctx) => insertAccount(ctx, sam));

    await t.mutation(api.members.transferOwnership, { groupId, toMembershipId: samMembershipId });

    expect(await t.run((ctx) => ctx.db.get(groupId))).toMatchObject({ ownerId: samId });
  });

  it("gives the new owner every owner power", async () => {
    const { t, asSam, groupId, ownerMembershipId, samMembershipId } = await groupWithSam();
    const leeMembershipId = await addLee(t, groupId, manager);
    await t.mutation(api.members.transferOwnership, { groupId, toMembershipId: samMembershipId });

    await asSam.mutation(api.members.updatePermissions, {
      membershipId: ownerMembershipId,
      expectedVersion: 2,
      permissions: { ...OWNER_PERMISSIONS },
    });
    await asSam.mutation(api.members.remove, { membershipId: leeMembershipId });
    const failure = await failureOf(asSam.mutation(api.members.leave, { groupId }));

    expect(await membershipRow(t, ownerMembershipId)).toMatchObject({ role: "member", ...OWNER_PERMISSIONS });
    expect(await membershipRow(t, leeMembershipId)).toBeNull();
    expect(failure.message).toBe("Transfer ownership before leaving.");
  });

  it("denies the old owner owner-only actions after the transfer", async () => {
    const { t, groupId, samMembershipId } = await groupWithSam();
    await t.mutation(api.members.transferOwnership, { groupId, toMembershipId: samMembershipId });

    const failure = await failureOf(
      t.mutation(api.members.transferOwnership, { groupId, toMembershipId: samMembershipId }),
    );

    expect(failure.code).toBe("PermissionDenied");
  });

  it("denies a member with Members: full", async () => {
    const { asSam, groupId, samMembershipId } = await groupWithSam({ members: "full" });

    const failure = await failureOf(
      asSam.mutation(api.members.transferOwnership, { groupId, toMembershipId: samMembershipId }),
    );

    expect(failure).toMatchObject({
      code: "PermissionDenied",
      message: "Only the group owner can transfer ownership.",
    });
  });

  it("rejects a target from another group", async () => {
    const { t, groupId } = await groupWithSam();
    const otherGroupId = await seedGroup(t);
    const strangerMembershipId = await addLee(t, otherGroupId);

    const failure = await failureOf(
      t.mutation(api.members.transferOwnership, { groupId, toMembershipId: strangerMembershipId }),
    );

    expect(failure.code).toBe("NotFound");
  });

  it("rejects transferring to the current owner", async () => {
    const { t, groupId, ownerMembershipId } = await groupWithSam();

    const failure = await failureOf(
      t.mutation(api.members.transferOwnership, { groupId, toMembershipId: ownerMembershipId }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "You already own this group." });
  });
});
