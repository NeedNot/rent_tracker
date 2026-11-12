import { describe, expect, it } from "vitest";
import {
  addDays,
  addMonths,
  compareText,
  dateInMonth,
  daysInMonth,
  monthOf,
  parseDate,
  parseMonth,
} from "./calendarDate";

describe("calendar dates", () => {
  it.each(["2026-02-30", "2026-13-01", "2026-00-10", "2026-4-01", "2026/04/01", "", "2027-02-29"])(
    "rejects %j as a calendar date",
    (text) => {
      expect(() => parseDate(text)).toThrow();
    },
  );

  it.each(["2026-00", "2026-13", "26-01", "2026-1"])("rejects %j as a calendar month", (text) => {
    expect(() => parseMonth(text)).toThrow();
  });

  it("accepts the leap day only in leap years", () => {
    expect(parseDate("2028-02-29")).toEqual({ year: 2028, month: 2, day: 29 });
    expect(daysInMonth(2100, 2)).toBe(28);
    expect(daysInMonth(2000, 2)).toBe(29);
  });

  it.each([
    ["2026-01-31", 1, "2026-02-01"],
    ["2026-02-27", 5, "2026-03-04"],
    ["2028-02-27", 5, "2028-03-03"],
    ["2026-12-30", 5, "2027-01-04"],
    ["2026-03-01", -1, "2026-02-28"],
    ["2028-03-01", -1, "2028-02-29"],
    ["2026-01-01", 0, "2026-01-01"],
    ["2026-01-01", 365, "2027-01-01"],
    ["1970-01-01", -1, "1969-12-31"],
  ])("%s plus %i days is %s", (date, days, expected) => {
    expect(addDays(date, days)).toBe(expected);
  });

  it.each([
    ["2026-11", 1, "2026-12"],
    ["2026-12", 1, "2027-01"],
    ["2026-01", -1, "2025-12"],
    ["2026-03", 25, "2028-04"],
  ])("%s plus %i months is %s", (month, count, expected) => {
    expect(addMonths(month, count)).toBe(expected);
  });

  it.each([
    ["2026-02", 31, "2026-02-28"],
    ["2028-02", 30, "2028-02-29"],
    ["2026-04", 31, "2026-04-30"],
    ["2026-03", 31, "2026-03-31"],
    ["2026-03", 15, "2026-03-15"],
  ])("due day in %s for day %i falls on %s", (month, day, expected) => {
    expect(dateInMonth(month, day)).toBe(expected);
  });

  it("finds the month of a date and orders dates as text", () => {
    expect(monthOf("2026-03-02")).toBe("2026-03");
    expect(compareText("2026-02-28", "2026-03-01")).toBeLessThan(0);
    expect(compareText("2026-03-01", "2026-03-01")).toBe(0);
  });
});
