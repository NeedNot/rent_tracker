import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";

export interface TenantLedgerRows {
  rentTerms: Doc<"rentTerms">[];
  debts: Doc<"debts">[];
  payments: Doc<"payments">[];
}

export async function loadTenantLedgerRows(ctx: QueryCtx, tenantId: Id<"tenants">): Promise<TenantLedgerRows> {
  const [rentTerms, debts, payments] = await Promise.all([
    ctx.db
      .query("rentTerms")
      .withIndex("by_tenant", (q) => q.eq("tenantId", tenantId))
      .collect(),
    ctx.db
      .query("debts")
      .withIndex("by_tenant", (q) => q.eq("tenantId", tenantId))
      .collect(),
    ctx.db
      .query("payments")
      .withIndex("by_tenant", (q) => q.eq("tenantId", tenantId))
      .collect(),
  ]);
  return { rentTerms, debts, payments };
}
