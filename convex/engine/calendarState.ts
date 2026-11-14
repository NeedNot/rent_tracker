import type { CalendarDate } from "./calendarDate";

export type CalendarState = "paid" | "partial" | "owed" | "dueNotLate" | "upcoming" | "prepaid" | "notATenant";

export interface CalendarStateInput {
  today: CalendarDate;
  dueDate: CalendarDate;
  graceEnd: CalendarDate;
  amountCents: number;
  coveredCents: number;
}

export function calendarState({
  today,
  dueDate,
  graceEnd,
  amountCents,
  coveredCents,
}: CalendarStateInput): CalendarState {
  const fullyCovered = coveredCents >= amountCents;
  if (dueDate > today) {
    return fullyCovered ? "prepaid" : "upcoming";
  }
  if (fullyCovered) {
    return "paid";
  }
  if (today <= graceEnd) {
    return "dueNotLate";
  }
  return coveredCents > 0 ? "partial" : "owed";
}
