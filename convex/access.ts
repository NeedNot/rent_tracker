import type { UserIdentity } from "convex/server";
import type { Doc } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { failure } from "./errors";

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
