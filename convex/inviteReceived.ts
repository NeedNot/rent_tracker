import { type Infer, v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { isPendingInvite } from "./invitePending";
import { DELETED_USER_NAME, permissionSet } from "./validators";

export const invitation = v.object({
  inviteId: v.id("invites"),
  groupId: v.id("groups"),
  groupName: v.string(),
  invitedByName: v.string(),
  permissions: permissionSet,
  expiresAt: v.number(),
});

type Invitation = Infer<typeof invitation>;

export async function loadInvitations(ctx: QueryCtx, account: Doc<"accounts">, asOf: number): Promise<Invitation[]> {
  const { email } = account;
  if (email === undefined) {
    return [];
  }
  const invites = await ctx.db
    .query("invites")
    .withIndex("by_email", (q) => q.eq("email", email))
    .collect();
  const invitations = await Promise.all(
    invites.filter((invite) => isPendingInvite(invite, asOf)).map((invite) => invitationOf(ctx, invite)),
  );
  return invitations.filter((entry): entry is Invitation => entry !== null);
}

export async function inviterNameOf(ctx: QueryCtx, accountId: Id<"accounts">): Promise<string> {
  const account = await ctx.db.get(accountId);
  return account?.displayName ?? DELETED_USER_NAME;
}

async function invitationOf(ctx: QueryCtx, invite: Doc<"invites">): Promise<Invitation | null> {
  const group = await ctx.db.get(invite.groupId);
  if (group === null || group.archivedAt !== undefined) {
    return null;
  }
  return {
    inviteId: invite._id,
    groupId: group._id,
    groupName: group.name,
    invitedByName: await inviterNameOf(ctx, invite.invitedBy),
    permissions: invite.permissions,
    expiresAt: invite.expiresAt,
  };
}
