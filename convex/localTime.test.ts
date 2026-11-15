import { describe, expect, it } from "vitest";
import { isMonday, localHourAt } from "./localTime";
import { formatMoney } from "./moneyText";

describe("localHourAt", () => {
  it.each([
    ["2026-11-07T17:00:00Z", "America/Los_Angeles", 9],
    ["2026-07-07T16:00:00Z", "America/Los_Angeles", 9],
    ["2026-11-07T14:00:00Z", "America/New_York", 9],
    ["2026-11-07T03:30:00Z", "Asia/Kolkata", 9],
    ["2026-11-07T00:00:00Z", "UTC", 0],
  ])("%s in %s is hour %i", (instant, timeZone, expected) => {
    expect(localHourAt(Date.parse(instant), timeZone)).toBe(expected);
  });
});

describe("isMonday", () => {
  it.each([
    ["2026-11-09", true],
    ["2026-11-08", false],
    ["2028-02-28", true],
  ])("%s is Monday: %s", (date, expected) => {
    expect(isMonday(date)).toBe(expected);
  });
});

describe("formatMoney", () => {
  it.each([
    [0, "$0.00"],
    [5, "$0.05"],
    [150000, "$1,500.00"],
    [420000, "$4,200.00"],
    [123456789, "$1,234,567.89"],
  ])("%i cents reads %s", (cents, expected) => {
    expect(formatMoney(cents)).toBe(expected);
  });
});
