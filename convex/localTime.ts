import { type CalendarDate, parseDate } from "./engine/calendarDate";

const MONDAY = 1;

export function localHourAt(instantMs: number, timeZone: string): number {
  const hour = new Intl.DateTimeFormat("en-US", {
    timeZone,
    hourCycle: "h23",
    hour: "numeric",
    numberingSystem: "latn",
  })
    .formatToParts(instantMs)
    .find((part) => part.type === "hour")?.value;
  return Number(hour);
}

export function isMonday(date: CalendarDate): boolean {
  const { year, month, day } = parseDate(date);
  return new Date(Date.UTC(year, month - 1, day)).getUTCDay() === MONDAY;
}
