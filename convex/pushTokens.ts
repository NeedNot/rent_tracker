import { v } from "convex/values";
import type { Doc } from "./_generated/dataModel";
import { internalMutation, type MutationCtx, mutation } from "./_generated/server";
import { requireAccount } from "./access";
import { validationFailed } from "./appError";
import { pushPlatform } from "./validators";

export const PUSH_TOKEN_MAX_LENGTH = 4096;

export const register = mutation({
  args: { platform: pushPlatform, token: v.string() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const account = await requireAccount(ctx);
    const token = requireValidToken(args.token);
    const existing = await tokenRow(ctx, token);
    if (existing === null) {
      await ctx.db.insert("pushTokens", { accountId: account._id, platform: args.platform, token });
    } else if (existing.accountId !== account._id || existing.platform !== args.platform) {
      await ctx.db.patch(existing._id, { accountId: account._id, platform: args.platform });
    }
    return null;
  },
});

export const unregister = mutation({
  args: { token: v.string() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const account = await requireAccount(ctx);
    const existing = await tokenRow(ctx, args.token);
    if (existing !== null && existing.accountId === account._id) {
      await ctx.db.delete(existing._id);
    }
    return null;
  },
});

export const removeGone = internalMutation({
  args: { tokens: v.array(v.string()) },
  returns: v.null(),
  handler: async (ctx, args) => {
    for (const token of args.tokens) {
      const existing = await tokenRow(ctx, token);
      if (existing !== null) {
        await ctx.db.delete(existing._id);
      }
    }
    return null;
  },
});

function tokenRow(ctx: MutationCtx, token: string): Promise<Doc<"pushTokens"> | null> {
  return ctx.db
    .query("pushTokens")
    .withIndex("by_token", (q) => q.eq("token", token))
    .unique();
}

function requireValidToken(token: string): string {
  const trimmed = token.trim();
  if (trimmed === "" || trimmed.length > PUSH_TOKEN_MAX_LENGTH) {
    throw validationFailed("The push token sent by the app is not valid.", { field: "token" });
  }
  return trimmed;
}
