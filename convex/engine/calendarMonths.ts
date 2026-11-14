import { addMonths, type CalendarDate, type CalendarMonth, formatMonth } from "./calendarDate";
import { type CalendarState, calendarState } from "./calendarState";
import type { MonthLateFees } from "./monthLateFees";
import type { RentPeriod } from "./rentPeriods";

export type { CalendarState } from "./calendarState";

export interface CalendarMonthStatus {
  month: CalendarMonth;
  state: CalendarState;
  rentCents: number;
  coveredCents: number;
  owedCents: number;
  hasUnpaidLateFee: boolean;
  dueDate?: CalendarDate;
  graceEnd?: CalendarDate;
  wasLate: boolean;
  paidByGraceEndCents?: number;
  lateFeeCents?: number;
  lateFeeUnpaidCents?: number;
  suggestedLateFeeCents?: number;
}

export interface CalendarContext {
  today: CalendarDate;
  graceEnd: (period: RentPeriod) => CalendarDate;
  coveredCents: (period: RentPeriod) => number;
  wasLate: (period: RentPeriod) => boolean;
  paidByGraceEndCents: (period: RentPeriod) => number;
  lateFeesByMonth: ReadonlyMap<CalendarMonth, MonthLateFees>;
  suggestedLateFeeCents: (rentCents: number) => number;
}

const MONTHS_PER_YEAR = 12;

export function calendarMonths(
  year: number,
  periodsInYear: readonly RentPeriod[],
  context: CalendarContext,
): CalendarMonthStatus[] {
  const periodByMonth = new Map(periodsInYear.map((period) => [period.month, period]));
  const january = formatMonth({ year, month: 1 });
  return Array.from({ length: MONTHS_PER_YEAR }, (_, index) => {
    const month = addMonths(january, index);
    const period = periodByMonth.get(month);
    return period === undefined ? notATenantMonth(month) : tenantMonth(period, context);
  });
}

function notATenantMonth(month: CalendarMonth): CalendarMonthStatus {
  return {
    month,
    state: "notATenant",
    rentCents: 0,
    coveredCents: 0,
    owedCents: 0,
    hasUnpaidLateFee: false,
    wasLate: false,
  };
}

function tenantMonth(period: RentPeriod, context: CalendarContext): CalendarMonthStatus {
  const coveredCents = context.coveredCents(period);
  const graceEnd = context.graceEnd(period);
  const charged = period.dueDate <= context.today;
  const graceOver = charged && context.today > graceEnd;
  const wasLate = graceOver && context.wasLate(period);
  const lateFees = context.lateFeesByMonth.get(period.month);
  return {
    month: period.month,
    state: calendarState({
      today: context.today,
      dueDate: period.dueDate,
      graceEnd,
      amountCents: period.amountCents,
      coveredCents,
    }),
    rentCents: period.amountCents,
    coveredCents,
    owedCents: charged ? period.amountCents - coveredCents : 0,
    hasUnpaidLateFee: lateFees !== undefined && lateFees.unpaidCents > 0,
    dueDate: period.dueDate,
    graceEnd,
    wasLate,
    paidByGraceEndCents: graceOver ? context.paidByGraceEndCents(period) : undefined,
    lateFeeCents: lateFees?.cents,
    lateFeeUnpaidCents: lateFees?.unpaidCents,
    suggestedLateFeeCents:
      wasLate && lateFees === undefined ? context.suggestedLateFeeCents(period.amountCents) : undefined,
  };
}
