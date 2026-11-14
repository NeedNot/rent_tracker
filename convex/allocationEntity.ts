import { type Infer, v } from "convex/values";
import type { Id } from "./_generated/dataModel";
import type { Allocation } from "./engine/allocation";
import { chargeKey } from "./engine/charges";

export const allocationRow = v.object({
  tenantId: v.id("tenants"),
  key: v.string(),
  paymentId: v.string(),
  chargeKind: v.union(v.literal("rent"), v.literal("debt")),
  month: v.optional(v.string()),
  debtId: v.optional(v.string()),
  amountCents: v.number(),
});

export function allocationRowsOf(
  tenantId: Id<"tenants">,
  allocations: readonly Allocation[],
): Infer<typeof allocationRow>[] {
  return allocations.map(({ paymentId, charge, amountCents }) => ({
    tenantId,
    key: `${paymentId}:${chargeKey(charge)}`,
    paymentId,
    chargeKind: charge.kind,
    month: charge.kind === "rent" ? charge.month : undefined,
    debtId: charge.kind === "debt" ? charge.debtId : undefined,
    amountCents,
  }));
}
