import type { UserIdentity } from "convex/server";
import type { Infer } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { failure } from "./errors";
import type { permissionLevel } from "./validators";

export async function requireIdentity(ctx: QueryCtx): Promise<UserIdentity> {
  const identity = await ctx.auth.getUserIdentity();
  if (identity === null) {
    throw failure("NotSignedIn", "Sign in to continue.");
  }
  return identity;
}

export async function requireAccount(ctx: QueryCtx): Promise<Doc<"accounts">> {
  const identity = await requireIdentity(ctx);
  const account = await accountForSubject(ctx, identity.subject);
  if (account === null) {
    throw failure("NotSignedIn", "Sign in to continue.");
  }
  return requireActive(account);
}

export function accountForSubject(ctx: QueryCtx, authSubject: string): Promise<Doc<"accounts"> | null> {
  return ctx.db
    .query("accounts")
    .withIndex("by_auth_subject", (q) => q.eq("authSubject", authSubject))
    .unique();
}

export function requireActive(account: Doc<"accounts">): Doc<"accounts"> {
  if (account.deletedAt !== undefined) {
    throw failure("NotSignedIn", "This account was deleted.");
  }
  return account;
}

export type PermissionCategory = "tenants" | "rentTerms" | "payments" | "debts" | "members" | "settings";
export type PermissionLevel = Infer<typeof permissionLevel>;

const LEVEL_RANK: Record<PermissionLevel, number> = { read: 0, addEdit: 1, full: 2 };

export async function requireMember(ctx: QueryCtx, groupId: Id<"groups">): Promise<Doc<"memberships">> {
  const account = await requireAccount(ctx);
  const membership = await ctx.db
    .query("memberships")
    .withIndex("by_group_account", (q) => q.eq("groupId", groupId).eq("accountId", account._id))
    .unique();
  if (membership === null) {
    throw failure("PermissionDenied", "You don't have access to this group.");
  }
  return membership;
}

export async function requirePermission(
  ctx: QueryCtx,
  groupId: Id<"groups">,
  category: PermissionCategory,
  level: PermissionLevel,
): Promise<Doc<"memberships">> {
  const membership = await requireMember(ctx, groupId);
  if (membership.role !== "owner" && LEVEL_RANK[membership[category]] < LEVEL_RANK[level]) {
    throw failure("PermissionDenied", "You don't have permission to do that.");
  }
  return membership;
}
