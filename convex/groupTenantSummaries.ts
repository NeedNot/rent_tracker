import type { Infer } from "convex/values";
import type { Doc } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { localDateAt } from "./localDate";
import { loadTenantLedger } from "./tenantLedgerAdapter";
import { type tenantSummary, tenantSummaryOf } from "./tenantSummary";

export interface GroupTenantSummaries {
  tenants: Doc<"tenants">[];
  tenantSummaries: Infer<typeof tenantSummary>[];
}

export async function loadGroupTenantSummaries(
  ctx: QueryCtx,
  group: Doc<"groups">,
  asOf: number,
): Promise<GroupTenantSummaries> {
  const today = localDateAt(asOf, group.timeZone);
  const tenants = await ctx.db
    .query("tenants")
    .withIndex("by_group_status", (q) => q.eq("groupId", group._id))
    .collect();
  const tenantSummaries = await Promise.all(
    tenants.map(async (tenant) => {
      const ledger = await loadTenantLedger(ctx, group, tenant, today);
      return tenantSummaryOf(tenant._id, ledger.summary);
    }),
  );
  return { tenants, tenantSummaries };
}
