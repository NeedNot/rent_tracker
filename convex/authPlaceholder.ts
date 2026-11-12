import type { Infer } from "convex/values";
import type { Id } from "./_generated/dataModel";
import type { MutationCtx, QueryCtx } from "./_generated/server";
import type { permissionLevel } from "./validators";

export type PermissionCategory = "tenants" | "rentTerms" | "payments" | "debts" | "members" | "settings";
export type PermissionLevel = Infer<typeof permissionLevel>;

const PLACEHOLDER_AUTH_SUBJECT = "placeholder";

// Milestone 3 replaces this module with real access checks.
export async function requireMember(_ctx: QueryCtx, _groupId: Id<"groups">): Promise<void> {}

export async function requirePermission(
  _ctx: QueryCtx,
  _groupId: Id<"groups">,
  _category: PermissionCategory,
  _level: PermissionLevel,
): Promise<void> {}

export async function placeholderAccountId(ctx: MutationCtx): Promise<Id<"accounts">> {
  const existing = await ctx.db
    .query("accounts")
    .withIndex("by_auth_subject", (q) => q.eq("authSubject", PLACEHOLDER_AUTH_SUBJECT))
    .unique();
  if (existing !== null) {
    return existing._id;
  }
  return await ctx.db.insert("accounts", { authSubject: PLACEHOLDER_AUTH_SUBJECT, displayName: "You", version: 1 });
}
