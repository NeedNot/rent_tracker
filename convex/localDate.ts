import { validationFailed } from "./appError";
import { type CalendarDate, formatDate } from "./engine/calendarDate";

// Keeps every local date inside the four-digit years the engine compares as text.
const MAX_AS_OF_MS = Date.UTC(9999, 11, 31);

export function localDateAt(asOfMs: number, timeZone: string): CalendarDate {
  if (!Number.isSafeInteger(asOfMs) || asOfMs < 0 || asOfMs > MAX_AS_OF_MS) {
    throw validationFailed("The current time sent by the app is not valid.", { field: "asOf" });
  }
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    calendar: "gregory",
    numberingSystem: "latn",
    year: "numeric",
    month: "numeric",
    day: "numeric",
  }).formatToParts(asOfMs);
  const part = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((entry) => entry.type === type)?.value);
  return formatDate({ year: part("year"), month: part("month"), day: part("day") });
}
