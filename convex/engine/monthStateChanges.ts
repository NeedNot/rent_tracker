import type { CalendarDate, CalendarMonth } from "./calendarDate";
import { compareText } from "./calendarDate";
import { type CalendarState, calendarState } from "./calendarState";
import type { RentPeriodStatus } from "./rentPeriods";

export interface MonthStateChange {
  month: CalendarMonth;
  before: CalendarState;
  after: CalendarState;
}

export function monthStateChanges(
  before: readonly RentPeriodStatus[],
  after: readonly RentPeriodStatus[],
  today: CalendarDate,
): MonthStateChange[] {
  const beforeByMonth = stateByMonth(before, today);
  const afterByMonth = stateByMonth(after, today);
  const months = [...new Set([...beforeByMonth.keys(), ...afterByMonth.keys()])].sort(compareText);
  return months
    .map((month) => ({
      month,
      before: beforeByMonth.get(month) ?? "upcoming",
      after: afterByMonth.get(month) ?? "upcoming",
    }))
    .filter((change) => change.before !== change.after);
}

function stateByMonth(periods: readonly RentPeriodStatus[], today: CalendarDate): Map<CalendarMonth, CalendarState> {
  return new Map(periods.map((period) => [period.month, calendarState({ today, ...period })]));
}
