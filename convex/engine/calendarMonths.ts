import { addMonths, type CalendarDate, type CalendarMonth, formatMonth } from "./calendarDate";
import type { RentPeriod } from "./rentPeriods";

export type CalendarState = "paid" | "partial" | "owed" | "dueNotLate" | "upcoming" | "prepaid" | "notATenant";

export interface CalendarMonthStatus {
  month: CalendarMonth;
  state: CalendarState;
  rentCents: number;
  coveredCents: number;
  owedCents: number;
  hasUnpaidLateFee: boolean;
}

export interface CalendarContext {
  today: CalendarDate;
  graceEnd: (period: RentPeriod) => CalendarDate;
  coveredCents: (period: RentPeriod) => number;
  monthsWithUnpaidLateFee: ReadonlySet<CalendarMonth>;
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
    const hasUnpaidLateFee = context.monthsWithUnpaidLateFee.has(month);
    const period = periodByMonth.get(month);
    if (period === undefined) {
      return { month, state: "notATenant", rentCents: 0, coveredCents: 0, owedCents: 0, hasUnpaidLateFee };
    }
    const coveredCents = context.coveredCents(period);
    const charged = period.dueDate <= context.today;
    return {
      month,
      state: stateOf(period, coveredCents, context),
      rentCents: period.amountCents,
      coveredCents,
      owedCents: charged ? period.amountCents - coveredCents : 0,
      hasUnpaidLateFee,
    };
  });
}

function stateOf(period: RentPeriod, coveredCents: number, context: CalendarContext): CalendarState {
  const fullyCovered = coveredCents >= period.amountCents;
  if (period.dueDate > context.today) {
    return fullyCovered ? "prepaid" : "upcoming";
  }
  if (fullyCovered) {
    return "paid";
  }
  if (coveredCents > 0) {
    return "partial";
  }
  return context.today <= context.graceEnd(period) ? "dueNotLate" : "owed";
}
