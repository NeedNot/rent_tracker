import type { Doc } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { addDays, type CalendarDate } from "./engine/calendarDate";
import type { LateRentFacts } from "./latePushMessage";
import { isMonday } from "./localTime";
import { tenantLedgerOf } from "./tenantLedgerAdapter";
import { loadTenantLedgerRows } from "./tenantLedgerRows";

interface TenantLateness {
  lateToday: boolean;
  lateYesterday: boolean;
  owedCents: number;
}

export async function loadLateRentFacts(
  ctx: QueryCtx,
  group: Doc<"groups">,
  today: CalendarDate,
): Promise<LateRentFacts> {
  const activeTenants = await ctx.db
    .query("tenants")
    .withIndex("by_group_status", (q) => q.eq("groupId", group._id).eq("status", "active"))
    .collect();
  const tenants = await Promise.all(activeTenants.map((tenant) => tenantLateness(ctx, group, tenant, today)));
  return {
    groupName: group.name,
    newlyLateCount: tenants.filter((tenant) => tenant.lateToday && !tenant.lateYesterday).length,
    lateCount: tenants.filter((tenant) => tenant.lateToday).length,
    owedCents: tenants.reduce((total, tenant) => total + tenant.owedCents, 0),
    isWeeklyReminderDay: isMonday(today),
  };
}

async function tenantLateness(
  ctx: QueryCtx,
  group: Doc<"groups">,
  tenant: Doc<"tenants">,
  today: CalendarDate,
): Promise<TenantLateness> {
  const rows = await loadTenantLedgerRows(ctx, tenant._id);
  const summaryOn = (date: CalendarDate) => tenantLedgerOf({ group, tenant, rows, today: date }).summary;
  const todaySummary = summaryOn(today);
  const { rentOwedCents, lateFeesOwedCents, otherOwedCents } = todaySummary.balance;
  return {
    lateToday: todaySummary.state === "late",
    lateYesterday: summaryOn(addDays(today, -1)).state === "late",
    owedCents: rentOwedCents + lateFeesOwedCents + otherOwedCents,
  };
}
