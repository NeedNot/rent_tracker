import { type CalendarDate, type CalendarMonth, parseDate, parseMonth } from "./engine/calendarDate";

const MONTH_NAMES = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
];

const SHORT_NAME_LENGTH = 3;

export function monthName(month: CalendarMonth): string {
  return fullMonthName(parseMonth(month).month);
}

export function shortDate(date: CalendarDate): string {
  const { month, day } = parseDate(date);
  return `${fullMonthName(month).slice(0, SHORT_NAME_LENGTH)} ${day}`;
}

function fullMonthName(monthNumber: number): string {
  return MONTH_NAMES[monthNumber - 1] ?? String(monthNumber);
}
