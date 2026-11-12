export type CalendarDate = string;
export type CalendarMonth = string;

interface DateParts {
  year: number;
  month: number;
  day: number;
}

interface MonthParts {
  year: number;
  month: number;
}

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const MONTH_PATTERN = /^(\d{4})-(\d{2})$/;
const MONTHS_PER_YEAR = 12;
const DAYS_PER_ERA = 146097;
const YEARS_PER_ERA = 400;
const UNIX_EPOCH_DAY_OFFSET = 719468;

export function parseDate(date: CalendarDate): DateParts {
  const match = DATE_PATTERN.exec(date);
  const parts = match && { year: Number(match[1]), month: Number(match[2]), day: Number(match[3]) };
  if (!parts || !isValidMonth(parts.month) || parts.day < 1 || parts.day > daysInMonth(parts.year, parts.month)) {
    throw new Error(`Not a calendar date: "${date}"`);
  }
  return parts;
}

export function parseMonth(month: CalendarMonth): MonthParts {
  const match = MONTH_PATTERN.exec(month);
  const parts = match && { year: Number(match[1]), month: Number(match[2]) };
  if (!parts || !isValidMonth(parts.month)) {
    throw new Error(`Not a calendar month: "${month}"`);
  }
  return parts;
}

export function formatDate({ year, month, day }: DateParts): CalendarDate {
  return `${formatMonth({ year, month })}-${pad(day, 2)}`;
}

export function formatMonth({ year, month }: MonthParts): CalendarMonth {
  return `${pad(year, 4)}-${pad(month, 2)}`;
}

export function daysInMonth(year: number, month: number): number {
  const thirtyDayMonths = [4, 6, 9, 11];
  if (month === 2) {
    return isLeapYear(year) ? 29 : 28;
  }
  return thirtyDayMonths.includes(month) ? 30 : 31;
}

export function monthOf(date: CalendarDate): CalendarMonth {
  const { year, month } = parseDate(date);
  return formatMonth({ year, month });
}

export function yearOf(month: CalendarMonth): number {
  return parseMonth(month).year;
}

export function addMonths(month: CalendarMonth, count: number): CalendarMonth {
  const { year, month: monthNumber } = parseMonth(month);
  const index = year * MONTHS_PER_YEAR + (monthNumber - 1) + count;
  return formatMonth({ year: Math.floor(index / MONTHS_PER_YEAR), month: (index % MONTHS_PER_YEAR) + 1 });
}

export function dateInMonth(month: CalendarMonth, day: number): CalendarDate {
  const { year, month: monthNumber } = parseMonth(month);
  return formatDate({ year, month: monthNumber, day: Math.min(day, daysInMonth(year, monthNumber)) });
}

export function compareText(a: string, b: string): number {
  if (a === b) {
    return 0;
  }
  return a < b ? -1 : 1;
}

export function addDays(date: CalendarDate, days: number): CalendarDate {
  return fromDayNumber(toDayNumber(parseDate(date)) + days);
}

function isValidMonth(month: number): boolean {
  return month >= 1 && month <= MONTHS_PER_YEAR;
}

function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % YEARS_PER_ERA === 0;
}

function pad(value: number, width: number): string {
  return String(value).padStart(width, "0");
}

// Howard Hinnant's days_from_civil: years start in March so the leap day falls at the end of the cycle.
function toDayNumber({ year, month, day }: DateParts): number {
  const marchBasedYear = month <= 2 ? year - 1 : year;
  const era = Math.floor(marchBasedYear / YEARS_PER_ERA);
  const yearOfEra = marchBasedYear - era * YEARS_PER_ERA;
  const marchBasedMonth = (month + 9) % MONTHS_PER_YEAR;
  const dayOfYear = Math.floor((153 * marchBasedMonth + 2) / 5) + day - 1;
  const dayOfEra = yearOfEra * 365 + Math.floor(yearOfEra / 4) - Math.floor(yearOfEra / 100) + dayOfYear;
  return era * DAYS_PER_ERA + dayOfEra - UNIX_EPOCH_DAY_OFFSET;
}

function fromDayNumber(dayNumber: number): CalendarDate {
  const shifted = dayNumber + UNIX_EPOCH_DAY_OFFSET;
  const era = Math.floor(shifted / DAYS_PER_ERA);
  const dayOfEra = shifted - era * DAYS_PER_ERA;
  const yearOfEra = Math.floor(
    (dayOfEra - Math.floor(dayOfEra / 1460) + Math.floor(dayOfEra / 36524) - Math.floor(dayOfEra / 146096)) / 365,
  );
  const dayOfYear = dayOfEra - (365 * yearOfEra + Math.floor(yearOfEra / 4) - Math.floor(yearOfEra / 100));
  const marchBasedMonth = Math.floor((5 * dayOfYear + 2) / 153);
  const day = dayOfYear - Math.floor((153 * marchBasedMonth + 2) / 5) + 1;
  const month = marchBasedMonth < 10 ? marchBasedMonth + 3 : marchBasedMonth - 9;
  const year = yearOfEra + era * YEARS_PER_ERA + (month <= 2 ? 1 : 0);
  return formatDate({ year, month, day });
}
