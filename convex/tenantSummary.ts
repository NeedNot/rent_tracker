import { type Infer, v } from "convex/values";
import type { Id } from "./_generated/dataModel";
import type { TenantSummary } from "./engine/tenantLedger";

export const tenantState = v.union(v.literal("late"), v.literal("owes"), v.literal("paidUp"));

export const tenantSummary = v.object({
  tenantId: v.id("tenants"),
  rentOwedCents: v.number(),
  lateFeesOwedCents: v.number(),
  otherOwedCents: v.number(),
  creditCents: v.number(),
  state: tenantState,
  nextDueDate: v.optional(v.string()),
  partialMoveOutPeriod: v.optional(v.object({ start: v.string(), end: v.string() })),
});

export function tenantSummaryOf(tenantId: Id<"tenants">, summary: TenantSummary): Infer<typeof tenantSummary> {
  return {
    tenantId,
    ...summary.balance,
    state: summary.state,
    nextDueDate: summary.nextDueDate,
    partialMoveOutPeriod: summary.partialMoveOutPeriod,
  };
}
