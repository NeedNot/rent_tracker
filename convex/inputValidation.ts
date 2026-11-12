import { validationFailed } from "./appError";
import { type CalendarDate, type CalendarMonth, parseDate, parseMonth } from "./engine/calendarDate";

export const MIN_AMOUNT_CENTS = 1;
export const MAX_AMOUNT_CENTS = 100_000_000;
export const MAX_NAME_LENGTH = 100;
export const MAX_NOTES_LENGTH = 2000;
const MIN_DUE_DAY = 1;
const MAX_DUE_DAY = 31;

export function requireAmountCents(cents: number, field: string): number {
  if (!Number.isSafeInteger(cents) || cents < MIN_AMOUNT_CENTS || cents > MAX_AMOUNT_CENTS) {
    throw validationFailed(`${field} must be between $0.01 and $1,000,000.00.`, { field });
  }
  return cents;
}

export function optionalAmountCents(cents: number | undefined, field: string): number | undefined {
  return cents === undefined ? undefined : requireAmountCents(cents, field);
}

export function optionalNonNegativeCents(cents: number | undefined, field: string): number | undefined {
  if (cents === undefined) {
    return undefined;
  }
  if (!Number.isSafeInteger(cents) || cents < 0 || cents > MAX_AMOUNT_CENTS) {
    throw validationFailed(`${field} must be between $0.00 and $1,000,000.00.`, { field });
  }
  return cents;
}

export function requireGraceDays(graceDays: number): number {
  if (!Number.isSafeInteger(graceDays) || graceDays < 0) {
    throw validationFailed("Grace period must be a whole number of days, 0 or more.", { field: "Grace period" });
  }
  return graceDays;
}

export function optionalGraceDays(graceDays: number | undefined): number | undefined {
  return graceDays === undefined ? undefined : requireGraceDays(graceDays);
}

export function requireDueDay(dueDay: number): number {
  if (!Number.isSafeInteger(dueDay) || dueDay < MIN_DUE_DAY || dueDay > MAX_DUE_DAY) {
    throw validationFailed("Due day must be between 1 and 31.", { field: "Due day" });
  }
  return dueDay;
}

export function requireCalendarDate(date: string, field: string): CalendarDate {
  if (!parses(parseDate, date)) {
    throw validationFailed(`${field} must be a real date.`, { field });
  }
  return date;
}

export function optionalCalendarDate(date: string | undefined, field: string): CalendarDate | undefined {
  return date === undefined ? undefined : requireCalendarDate(date, field);
}

export function requireCalendarMonth(month: string, field: string): CalendarMonth {
  if (!parses(parseMonth, month)) {
    throw validationFailed(`${field} must be a real month.`, { field });
  }
  return month;
}

export function requireName(name: string, field: string): string {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    throw validationFailed(`${field} is required.`, { field });
  }
  return requireAtMost(trimmed, MAX_NAME_LENGTH, field);
}

export function optionalLabel(label: string | undefined, field: string): string | undefined {
  return optionalText(label, MAX_NAME_LENGTH, field);
}

export function optionalNotes(notes: string | undefined, field = "Notes"): string | undefined {
  return optionalText(notes, MAX_NOTES_LENGTH, field);
}

export function requireTimeZone(timeZone: string): string {
  if (!isKnownTimeZone(timeZone)) {
    throw validationFailed("Time zone is not recognized.", { field: "Time zone" });
  }
  return timeZone;
}

function optionalText(text: string | undefined, maxLength: number, field: string): string | undefined {
  const trimmed = text?.trim();
  if (trimmed === undefined || trimmed.length === 0) {
    return undefined;
  }
  return requireAtMost(trimmed, maxLength, field);
}

function requireAtMost(text: string, maxLength: number, field: string): string {
  if (text.length > maxLength) {
    throw validationFailed(`${field} must be ${maxLength.toLocaleString("en-US")} characters or fewer.`, { field });
  }
  return text;
}

function parses(parse: (text: string) => unknown, text: string): boolean {
  try {
    parse(text);
    return true;
  } catch {
    return false;
  }
}

function isKnownTimeZone(timeZone: string): boolean {
  try {
    new Intl.DateTimeFormat("en-US", { timeZone });
    return true;
  } catch {
    return false;
  }
}
