import { v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { internalMutation, internalQuery, type MutationCtx, type QueryCtx } from "./_generated/server";
import { requireOwnsNoGroups } from "./accountOwnership";
import { appError, validationFailed } from "./appError";
import { EMAIL_TAKEN } from "./authWorker";
import { isPendingInvite } from "./invitePending";
import { DELETED_USER_NAME } from "./validators";

export const authSubjectToAnonymize = internalQuery({
  args: { accountId: v.id("accounts") },
  returns: v.union(v.string(), v.null()),
  handler: async (ctx, args) => {
    const account = await requireAccountRow(ctx, args.accountId);
    if (account.deletedAt !== undefined) {
      return null;
    }
    await requireOwnsNoGroups(ctx, account._id);
    return account.authSubject;
  },
});

export const anonymize = internalMutation({
  args: { accountId: v.id("accounts") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const account = await requireAccountRow(ctx, args.accountId);
    if (account.deletedAt !== undefined) {
      return null;
    }
    await requireOwnsNoGroups(ctx, account._id);
    const now = Date.now();
    await ctx.db.patch(account._id, {
      displayName: DELETED_USER_NAME,
      email: undefined,
      deletedAt: now,
      version: account.version + 1,
    });
    await deletePushTokens(ctx, account._id);
    await deleteMemberships(ctx, account._id);
    await revokeSentInvites(ctx, account._id, now);
    return null;
  },
});

export const authSubjectForEmailChange = internalQuery({
  args: { accountId: v.id("accounts"), email: v.string() },
  returns: v.string(),
  handler: async (ctx, args) => {
    const account = await requireEmailChangeable(ctx, args.accountId, args.email);
    return account.authSubject;
  },
});

export const setEmail = internalMutation({
  args: { accountId: v.id("accounts"), email: v.string() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const account = await requireEmailChangeable(ctx, args.accountId, args.email);
    await ctx.db.patch(account._id, { email: args.email, version: account.version + 1 });
    return null;
  },
});

async function requireAccountRow(ctx: QueryCtx, accountId: Id<"accounts">): Promise<Doc<"accounts">> {
  const account = await ctx.db.get(accountId);
  if (account === null) {
    throw appError("NotFound", "This account no longer exists.");
  }
  return account;
}

async function requireEmailChangeable(
  ctx: QueryCtx,
  accountId: Id<"accounts">,
  email: string,
): Promise<Doc<"accounts">> {
  const account = await requireAccountRow(ctx, accountId);
  if (account.deletedAt !== undefined) {
    throw validationFailed("This account was deleted.");
  }
  const emailOwner = await ctx.db
    .query("accounts")
    .withIndex("by_email", (q) => q.eq("email", email))
    .first();
  if (emailOwner !== null && emailOwner._id !== account._id) {
    throw validationFailed(EMAIL_TAKEN, { field: "email" });
  }
  return account;
}

async function deletePushTokens(ctx: MutationCtx, accountId: Id<"accounts">): Promise<void> {
  const tokens = await ctx.db
    .query("pushTokens")
    .withIndex("by_account", (q) => q.eq("accountId", accountId))
    .collect();
  await Promise.all(tokens.map((token) => ctx.db.delete(token._id)));
}

async function deleteMemberships(ctx: MutationCtx, accountId: Id<"accounts">): Promise<void> {
  const memberships = await ctx.db
    .query("memberships")
    .withIndex("by_account", (q) => q.eq("accountId", accountId))
    .collect();
  await Promise.all(memberships.map((membership) => ctx.db.delete(membership._id)));
}

async function revokeSentInvites(ctx: MutationCtx, accountId: Id<"accounts">, now: number): Promise<void> {
  const invites = await ctx.db
    .query("invites")
    .withIndex("by_invited_by", (q) => q.eq("invitedBy", accountId))
    .collect();
  const pending = invites.filter((invite) => isPendingInvite(invite, now));
  await Promise.all(pending.map((invite) => ctx.db.patch(invite._id, { revokedAt: now })));
}
