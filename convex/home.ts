import { v } from "convex/values";
import { query } from "./_generated/server";
import { groupDoc } from "./documentValidators";
import { groupSummary, groupSummaryOf } from "./groupSummary";
import { loadGroupTenantSummaries } from "./groupTenantSummaries";

export const screen = query({
  args: { asOf: v.number() },
  returns: v.object({
    groups: v.array(groupDoc),
    groupSummaries: v.array(groupSummary),
  }),
  handler: async (ctx, args) => {
    const everyGroup = await ctx.db.query("groups").collect();
    const groups = everyGroup.filter((group) => group.archivedAt === undefined);
    const groupSummaries = await Promise.all(
      groups.map(async (group) => {
        const { tenants, tenantSummaries } = await loadGroupTenantSummaries(ctx, group, args.asOf);
        return groupSummaryOf(group._id, tenants, tenantSummaries);
      }),
    );
    return { groups, groupSummaries };
  },
});
