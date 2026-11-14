import { v } from "convex/values";
import type { Doc } from "./_generated/dataModel";
import { query } from "./_generated/server";
import { requireAccount } from "./access";
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
    const account = await requireAccount(ctx);
    const memberships = await ctx.db
      .query("memberships")
      .withIndex("by_account", (q) => q.eq("accountId", account._id))
      .collect();
    const memberGroups = await Promise.all(memberships.map((membership) => ctx.db.get(membership.groupId)));
    const groups = memberGroups.filter(
      (group): group is Doc<"groups"> => group !== null && group.archivedAt === undefined,
    );
    const groupSummaries = await Promise.all(
      groups.map(async (group) => {
        const { tenants, tenantSummaries } = await loadGroupTenantSummaries(ctx, group, args.asOf);
        return groupSummaryOf(group._id, tenants, tenantSummaries);
      }),
    );
    return { groups, groupSummaries };
  },
});
