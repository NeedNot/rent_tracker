import { v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { mutation, type QueryCtx, query } from "./_generated/server";
import { requireMember, requirePermission } from "./access";
import { appError, validationFailed } from "./appError";
import { inviteDoc, membershipDoc } from "./documentValidators";
import { requireExpectedVersion } from "./editConflict";
import { isPendingInvite } from "./invitePending";
import { loadMemberProfiles, memberProfile } from "./memberProfile";
import {
  OWNER_PERMISSIONS,
  PERMISSION_PRESETS,
  permissionsOf,
  requireMayChangePermissions,
  requireMayRemove,
} from "./permissionRules";
import { permissionSet } from "./validators";

interface EditStamp {
  lastEditedAt: number;
  lastEditedBy: Id<"accounts">;
}

export const screen = query({
  args: { groupId: v.id("groups"), asOf: v.number() },
  returns: v.object({
    memberships: v.array(membershipDoc),
    memberProfiles: v.array(memberProfile),
    invites: v.array(inviteDoc),
  }),
  handler: async (ctx, args) => {
    await requireMember(ctx, args.groupId);
    const memberships = ownerFirstThenJoinOrder(
      await ctx.db
        .query("memberships")
        .withIndex("by_group_account", (q) => q.eq("groupId", args.groupId))
        .collect(),
    );
    const invites = await ctx.db
      .query("invites")
      .withIndex("by_group", (q) => q.eq("groupId", args.groupId))
      .collect();
    return {
      memberships,
      memberProfiles: await loadMemberProfiles(ctx, memberships),
      invites: invites.filter((invite) => isPendingInvite(invite, args.asOf)),
    };
  },
});

export const mine = query({
  args: { groupId: v.id("groups") },
  returns: membershipDoc,
  handler: async (ctx, args) => {
    return await requireMember(ctx, args.groupId);
  },
});

export const updatePermissions = mutation({
  args: { membershipId: v.id("memberships"), expectedVersion: v.number(), permissions: permissionSet },
  returns: v.null(),
  handler: async (ctx, args) => {
    const target = await loadMembership(ctx, args.membershipId);
    const editor = await requirePermission(ctx, target.groupId, "members", "full");
    if (target.role === "owner") {
      throw appError("PermissionDenied", "Nobody can change the owner's permissions.");
    }
    await requireCurrentMembership(ctx, target, args.expectedVersion);
    requireMayChangePermissions(editor, permissionsOf(target), args.permissions);
    await ctx.db.patch(target._id, {
      ...args.permissions,
      version: target.version + 1,
      ...editStamp(editor.accountId),
    });
    return null;
  },
});

export const remove = mutation({
  args: { membershipId: v.id("memberships") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const target = await loadMembership(ctx, args.membershipId);
    const remover = await requirePermission(ctx, target.groupId, "members", "addEdit");
    if (target._id === remover._id) {
      throw validationFailed("To remove yourself, leave the group instead.");
    }
    requireMayRemove(remover, target);
    await ctx.db.delete(target._id);
    return null;
  },
});

export const leave = mutation({
  args: { groupId: v.id("groups") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const membership = await requireMember(ctx, args.groupId);
    if (membership.role === "owner") {
      throw validationFailed("Transfer ownership before leaving.");
    }
    await ctx.db.delete(membership._id);
    return null;
  },
});

export const transferOwnership = mutation({
  args: { groupId: v.id("groups"), toMembershipId: v.id("memberships") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const currentOwner = await requirePermission(ctx, args.groupId, "members", "full");
    if (currentOwner.role !== "owner") {
      throw appError("PermissionDenied", "Only the group owner can transfer ownership.");
    }
    const newOwner = await loadMembership(ctx, args.toMembershipId);
    if (newOwner.groupId !== args.groupId) {
      throw appError("NotFound", "This member no longer exists.");
    }
    if (newOwner._id === currentOwner._id) {
      throw validationFailed("You already own this group.");
    }
    const stamp = editStamp(currentOwner.accountId);
    await ctx.db.patch(newOwner._id, {
      role: "owner",
      ...OWNER_PERMISSIONS,
      version: newOwner.version + 1,
      ...stamp,
    });
    await ctx.db.patch(currentOwner._id, {
      role: "member",
      ...PERMISSION_PRESETS.manager,
      version: currentOwner.version + 1,
      ...stamp,
    });
    await ctx.db.patch(args.groupId, { ownerId: newOwner.accountId });
    return null;
  },
});

function ownerFirstThenJoinOrder(memberships: Doc<"memberships">[]): Doc<"memberships">[] {
  const ownerRank = (membership: Doc<"memberships">) => (membership.role === "owner" ? 0 : 1);
  return [...memberships].sort((a, b) => ownerRank(a) - ownerRank(b) || a._creationTime - b._creationTime);
}

async function loadMembership(ctx: QueryCtx, membershipId: Id<"memberships">): Promise<Doc<"memberships">> {
  const membership = await ctx.db.get(membershipId);
  if (membership === null) {
    throw appError("NotFound", "This member no longer exists.");
  }
  return membership;
}

async function requireCurrentMembership(
  ctx: QueryCtx,
  membership: Doc<"memberships">,
  expectedVersion: number,
): Promise<void> {
  await requireExpectedVersion(
    ctx,
    { version: membership.version, changedBy: membership.lastEditedBy },
    expectedVersion,
    "member's permissions",
  );
}

function editStamp(accountId: Id<"accounts">): EditStamp {
  return { lastEditedAt: Date.now(), lastEditedBy: accountId };
}
