import { v } from "convex/values";
import { internal } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import { mutation, type QueryCtx } from "./_generated/server";
import { requireAccount, requirePermission } from "./access";
import { appError, validationFailed } from "./appError";
import { requireInviteEmail } from "./inviteEmailAddress";
import { isPendingInvite } from "./invitePending";
import { requireMayInvite } from "./permissionRules";
import { permissionSet } from "./validators";

export const INVITE_LIFETIME_MS = 14 * 24 * 60 * 60 * 1000;

const INVITE_GONE = "This invite no longer exists.";

export const send = mutation({
  args: { groupId: v.id("groups"), email: v.string(), permissions: permissionSet },
  returns: v.id("invites"),
  handler: async (ctx, args) => {
    const inviter = await requirePermission(ctx, args.groupId, "members", "addEdit");
    requireMayInvite(inviter, args.permissions);
    const email = requireInviteEmail(args.email);
    await requireNotMember(ctx, args.groupId, email);
    const now = Date.now();
    const terms = { permissions: args.permissions, invitedBy: inviter.accountId, expiresAt: now + INVITE_LIFETIME_MS };
    const pending = await pendingInviteFor(ctx, args.groupId, email, now);
    let inviteId: Id<"invites">;
    if (pending === null) {
      inviteId = await ctx.db.insert("invites", { groupId: args.groupId, email, ...terms });
    } else {
      inviteId = pending._id;
      await ctx.db.patch(inviteId, terms);
    }
    await ctx.scheduler.runAfter(0, internal.inviteEmail.send, { inviteId });
    return inviteId;
  },
});

export const revoke = mutation({
  args: { inviteId: v.id("invites") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const invite = await ctx.db.get(args.inviteId);
    if (invite === null) {
      throw appError("NotFound", INVITE_GONE);
    }
    await requirePermission(ctx, invite.groupId, "members", "addEdit");
    const now = Date.now();
    if (!isPendingInvite(invite, now)) {
      throw validationFailed("This invite is no longer pending.");
    }
    await ctx.db.patch(invite._id, { revokedAt: now });
    return null;
  },
});

export const accept = mutation({
  args: { inviteId: v.id("invites") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const account = await requireAccount(ctx);
    const now = Date.now();
    const invite = await requireOwnPendingInvite(ctx, args.inviteId, account, now);
    await requireActiveGroup(ctx, invite.groupId);
    const existing = await membershipOf(ctx, invite.groupId, account._id);
    if (existing === null) {
      await ctx.db.insert("memberships", {
        groupId: invite.groupId,
        accountId: account._id,
        role: "member",
        ...invite.permissions,
        notificationsOn: true,
        version: 1,
      });
    }
    await ctx.db.patch(invite._id, { acceptedAt: now });
    return null;
  },
});

export const decline = mutation({
  args: { inviteId: v.id("invites") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const account = await requireAccount(ctx);
    const now = Date.now();
    const invite = await requireOwnPendingInvite(ctx, args.inviteId, account, now);
    await requireActiveGroup(ctx, invite.groupId);
    await ctx.db.patch(invite._id, { declinedAt: now });
    return null;
  },
});

async function requireNotMember(ctx: QueryCtx, groupId: Id<"groups">, email: string): Promise<void> {
  const account = await ctx.db
    .query("accounts")
    .withIndex("by_email", (q) => q.eq("email", email))
    .first();
  if (account !== null && (await membershipOf(ctx, groupId, account._id)) !== null) {
    throw validationFailed("Already a member of this group.", { field: "email" });
  }
}

async function pendingInviteFor(
  ctx: QueryCtx,
  groupId: Id<"groups">,
  email: string,
  now: number,
): Promise<Doc<"invites"> | null> {
  const invites = await ctx.db
    .query("invites")
    .withIndex("by_email", (q) => q.eq("email", email))
    .collect();
  return invites.find((invite) => invite.groupId === groupId && isPendingInvite(invite, now)) ?? null;
}

async function requireOwnPendingInvite(
  ctx: QueryCtx,
  inviteId: Id<"invites">,
  account: Doc<"accounts">,
  now: number,
): Promise<Doc<"invites">> {
  const invite = await ctx.db.get(inviteId);
  if (invite === null || invite.email !== account.email || !isPendingInvite(invite, now)) {
    throw appError("NotFound", INVITE_GONE);
  }
  return invite;
}

async function requireActiveGroup(ctx: QueryCtx, groupId: Id<"groups">): Promise<void> {
  const group = await ctx.db.get(groupId);
  if (group === null || group.archivedAt !== undefined) {
    throw appError("NotFound", "This group no longer exists.");
  }
}

function membershipOf(
  ctx: QueryCtx,
  groupId: Id<"groups">,
  accountId: Id<"accounts">,
): Promise<Doc<"memberships"> | null> {
  return ctx.db
    .query("memberships")
    .withIndex("by_group_account", (q) => q.eq("groupId", groupId).eq("accountId", accountId))
    .unique();
}
