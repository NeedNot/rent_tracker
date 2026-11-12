import type { CalendarDate, CalendarMonth } from "./calendarDate";
import type { DebtRow, GroupSettings, TenantSettings } from "./ledgerInput";
import type { RentPeriodStatus } from "./rentPeriods";

export interface LateFeeSuggestion {
  forMonth: CalendarMonth;
  amountCents: number;
}

const LATE_FEE_PERCENT = 5;
const CENTS_ROUNDING_HALF = 50;
const PERCENT = 100;

export function percentLateFeeCents(rentCents: number): number {
  return Math.floor((rentCents * LATE_FEE_PERCENT + CENTS_ROUNDING_HALF) / PERCENT);
}

export function suggestedLateFeeCents(group: GroupSettings, tenant: TenantSettings, rentCents: number): number {
  return tenant.lateFeeFlatOverrideCents ?? group.lateFeeFlatCents ?? percentLateFeeCents(rentCents);
}

export function lateFeeSuggestion(
  group: GroupSettings,
  tenant: TenantSettings,
  periods: readonly RentPeriodStatus[],
  activeDebts: readonly DebtRow[],
): LateFeeSuggestion | undefined {
  if (tenant.status === "archived") {
    return undefined;
  }
  const monthsWithFee = new Set(lateFees(activeDebts).map((fee) => fee.forMonth));
  const latest = periods.filter((period) => period.currentlyLate && !monthsWithFee.has(period.month)).at(-1);
  return latest && { forMonth: latest.month, amountCents: suggestedLateFeeCents(group, tenant, latest.amountCents) };
}

export function lateFeesThatMayNotApply(
  periods: readonly RentPeriodStatus[],
  activeDebts: readonly DebtRow[],
  today: CalendarDate,
): string[] {
  const chargedByMonth = new Map(periods.filter((period) => period.charged).map((period) => [period.month, period]));
  return lateFees(activeDebts)
    .filter((fee) => {
      const period = chargedByMonth.get(fee.forMonth);
      return period === undefined || (today > period.graceEnd && !period.wasLate);
    })
    .map((fee) => fee.id);
}

function lateFees(debts: readonly DebtRow[]): (DebtRow & { forMonth: CalendarMonth })[] {
  return debts.filter((debt): debt is DebtRow & { forMonth: CalendarMonth } => {
    return debt.kind === "lateFee" && debt.forMonth !== undefined;
  });
}
