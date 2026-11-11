import { v } from "convex/values";
import { internalQuery, mutation, query } from "../../_generated/server";
import { requireMember, requirePermission } from "../../access";

export const list = query({
  args: { groupId: v.string() },
  handler: async (ctx, args) => {
    await requireMember(ctx, args.groupId);
    return [];
  },
});

export const log = mutation({
  args: { groupId: v.string() },
  async handler(ctx, args) {
    await requirePermission(ctx, args.groupId, "payments");
  },
});

export const forSupport = internalQuery({
  args: {},
  handler: async () => [],
});
