import { type Infer, v } from "convex/values";
import type { Id } from "./_generated/dataModel";
import type { TenantLedgerInput } from "./engine/ledgerInput";
import { ledgerRows } from "./engine/ledgerRows";
import type { TenantLedger } from "./engine/tenantLedger";

export const ledgerRow = v.object({
  tenantId: v.id("tenants"),
  key: v.string(),
  kind: v.union(v.literal("rent"), v.literal("debt"), v.literal("payment")),
  entryId: v.optional(v.string()),
  month: v.optional(v.string()),
  date: v.string(),
  amountCents: v.number(),
  position: v.number(),
  balanceOwedCents: v.optional(v.number()),
  balanceCreditCents: v.optional(v.number()),
  mayNotApply: v.boolean(),
});

export function ledgerRowsOf(
  tenantId: Id<"tenants">,
  input: TenantLedgerInput,
  ledger: TenantLedger,
): Infer<typeof ledgerRow>[] {
  const engineRows = ledgerRows({
    today: input.today,
    rentPeriods: ledger.rentPeriods,
    debts: input.debts,
    payments: input.payments,
    mayNotApplyDebtIds: ledger.summary.mayNotApplyDebtIds,
  });
  return engineRows.map((row) => ({ tenantId, ...row }));
}
