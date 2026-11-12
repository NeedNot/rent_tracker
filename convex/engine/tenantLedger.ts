import { type Allocation, allocate, coverageByCharge, coveredCents, paymentQueue } from "./allocation";
import { type Balance, balanceOf, totalOwedCents } from "./balance";
import { type CalendarDate, type CalendarMonth, yearOf } from "./calendarDate";
import { type CalendarMonthStatus, calendarMonths } from "./calendarMonths";
import { type Charge, chargeKey, debtCharge, orderCharges, rentCharge } from "./charges";
import { dropWhile, first, take, takeWhile } from "./iterables";
import { type LateFeeSuggestion, lateFeeSuggestion, lateFeesThatMayNotApply } from "./lateFees";
import { graceEnd, type LatenessContext, wasLate } from "./lateness";
import type { DebtRow, TenantLedgerInput, TenantStatus } from "./ledgerInput";
import {
  type DateRange,
  partialMoveOutPeriod,
  type RentPeriod,
  type RentPeriodStatus,
  rentPeriods,
} from "./rentPeriods";

export type TenantState = "late" | "owes" | "paidUp";

export interface TenantSummary {
  balance: Balance;
  state: TenantState;
  nextDueDate?: CalendarDate;
  lateFeeSuggestion?: LateFeeSuggestion;
  partialMoveOutPeriod?: DateRange;
  mayNotApplyDebtIds: string[];
}

export interface TenantLedger {
  summary: TenantSummary;
  rentPeriods: RentPeriodStatus[];
  allocations: Allocation[];
  calendarMonths: CalendarMonthStatus[];
}

export const CREDIT_PROJECTION_LIMIT_PERIODS = 1200;

export function computeTenantLedger(input: TenantLedgerInput): TenantLedger {
  const { today, group, tenant } = input;
  const graceDays = tenant.graceDaysOverride ?? group.graceDays;
  const allPeriods = () => rentPeriods(tenant, input.rentTerms);
  const futurePeriods = () => dropWhile(allPeriods(), (period) => period.dueDate <= today);
  const activeDebts = input.debts.filter((debt) => debt.deletedAt === undefined);
  const payments = paymentQueue(input.payments);

  const chargedPeriods = [...takeWhile(allPeriods(), (period) => period.dueDate <= today)];
  const orderedCharges = orderCharges(
    [...chargedPeriods.map(rentCharge), ...activeDebts.filter((debt) => debt.date <= today).map(debtCharge)],
    group.paymentOrder,
  );
  const settled = allocate(payments, orderedCharges);
  const projected = allocate(settled.unspent, rentCharges(take(futurePeriods(), CREDIT_PROJECTION_LIMIT_PERIODS)));
  const creditedPeriods = [...take(futurePeriods(), distinctCharges(projected.allocations))];
  const allocations = [...settled.allocations, ...projected.allocations];
  const coverage = coverageByCharge(allocations);
  const rentCoveredCents = (period: RentPeriod) => coveredCents(coverage, rentCharge(period).ref);

  const lateness: LatenessContext = { today, graceDays, orderedCharges, payments };
  const periodStatuses = [
    ...chargedPeriods.map((period) => chargedPeriodStatus(period, rentCoveredCents(period), lateness)),
    ...creditedPeriods.map((period) => creditedPeriodStatus(period, rentCoveredCents(period), graceDays)),
  ];

  const balance = balanceOf(orderedCharges, coverage, settled.unspent);
  const periodsInYear = takeWhile(
    dropWhile(allPeriods(), (period) => yearOf(period.month) < input.calendarYear),
    (period) => yearOf(period.month) === input.calendarYear,
  );

  return {
    summary: {
      balance,
      state: tenantState(tenant.status, periodStatuses, balance),
      nextDueDate: first(dropWhile(allPeriods(), (period) => period.dueDate < today))?.dueDate,
      lateFeeSuggestion: lateFeeSuggestion(group, tenant, periodStatuses, activeDebts),
      partialMoveOutPeriod: partialMoveOutPeriod(tenant, input.rentTerms),
      mayNotApplyDebtIds: lateFeesThatMayNotApply(periodStatuses, activeDebts, today),
    },
    rentPeriods: periodStatuses,
    allocations,
    calendarMonths: calendarMonths(input.calendarYear, [...periodsInYear], {
      today,
      graceEnd: (period) => graceEnd(period, graceDays),
      coveredCents: rentCoveredCents,
      monthsWithUnpaidLateFee: monthsWithUnpaidLateFee(activeDebts, today, coverage),
    }),
  };
}

function chargedPeriodStatus(period: RentPeriod, coveredCents: number, lateness: LatenessContext): RentPeriodStatus {
  const late = wasLate(period, lateness);
  return {
    ...period,
    charged: true,
    coveredCents,
    graceEnd: graceEnd(period, lateness.graceDays),
    wasLate: late,
    currentlyLate: late && coveredCents < period.amountCents,
  };
}

function creditedPeriodStatus(period: RentPeriod, coveredCents: number, graceDays: number): RentPeriodStatus {
  return {
    ...period,
    charged: false,
    coveredCents,
    graceEnd: graceEnd(period, graceDays),
    wasLate: false,
    currentlyLate: false,
  };
}

function tenantState(status: TenantStatus, periods: readonly RentPeriodStatus[], balance: Balance): TenantState {
  if (status === "active" && periods.some((period) => period.currentlyLate)) {
    return "late";
  }
  return totalOwedCents(balance) > 0 ? "owes" : "paidUp";
}

function* rentCharges(periods: Iterable<RentPeriod>): Generator<Charge> {
  for (const period of periods) {
    yield rentCharge(period);
  }
}

function distinctCharges(allocations: readonly Allocation[]): number {
  return new Set(allocations.map((allocation) => chargeKey(allocation.charge))).size;
}

function monthsWithUnpaidLateFee(
  activeDebts: readonly DebtRow[],
  today: CalendarDate,
  coverage: ReadonlyMap<string, number>,
): Set<CalendarMonth> {
  const unpaid = activeDebts.filter((debt) => {
    const charge = debtCharge(debt);
    return debt.kind === "lateFee" && debt.date <= today && coveredCents(coverage, charge.ref) < debt.amountCents;
  });
  return new Set(unpaid.flatMap((debt) => (debt.forMonth === undefined ? [] : [debt.forMonth])));
}
