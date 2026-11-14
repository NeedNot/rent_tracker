import { type Infer, v } from "convex/values";
import type { Id } from "./_generated/dataModel";
import type { CalendarMonthStatus } from "./engine/calendarMonths";

export const calendarState = v.union(
  v.literal("paid"),
  v.literal("partial"),
  v.literal("owed"),
  v.literal("dueNotLate"),
  v.literal("upcoming"),
  v.literal("prepaid"),
  v.literal("notATenant"),
);

export const calendarMonth = v.object({
  tenantId: v.id("tenants"),
  month: v.string(),
  state: calendarState,
  rentCents: v.number(),
  coveredCents: v.number(),
  owedCents: v.number(),
  hasUnpaidLateFee: v.boolean(),
  dueDate: v.optional(v.string()),
  graceEnd: v.optional(v.string()),
  wasLate: v.boolean(),
  paidByGraceEndCents: v.optional(v.number()),
  lateFeeCents: v.optional(v.number()),
  lateFeeUnpaidCents: v.optional(v.number()),
  suggestedLateFeeCents: v.optional(v.number()),
});

export function calendarMonthsOf(
  tenantId: Id<"tenants">,
  months: readonly CalendarMonthStatus[],
): Infer<typeof calendarMonth>[] {
  return months.map((month) => ({ tenantId, ...month }));
}
