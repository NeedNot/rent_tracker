import { describe, expect, it } from "vitest";
import { take } from "./iterables";
import { defaultTenant, RENT, term } from "./ledgerBuilder.fixture";
import type { RentTermRow, TenantSettings } from "./ledgerInput";
import { partialMoveOutPeriod, rentPeriods } from "./rentPeriods";

function firstPeriods(tenant: Partial<TenantSettings>, terms: RentTermRow[], count = 4) {
  return [...take(rentPeriods({ ...defaultTenant, ...tenant }, terms), count)];
}

function dueDates(tenant: Partial<TenantSettings>, terms: RentTermRow[], count = 4) {
  return firstPeriods(tenant, terms, count).map((period) => period.dueDate);
}

describe("rent periods", () => {
  it("starts with the first due date on or after move-in", () => {
    expect(dueDates({ moveIn: "2026-01-17" }, [term("2026-01", RENT)], 2)).toEqual(["2026-02-01", "2026-03-01"]);
  });

  it("charges the month when move-in falls on the due date", () => {
    expect(dueDates({ moveIn: "2026-01-15" }, [term("2026-01", RENT, 15)], 1)).toEqual(["2026-01-15"]);
  });

  it("puts rent due on the 31st on the last day of shorter months", () => {
    expect(dueDates({}, [term("2026-01", RENT, 31)])).toEqual(["2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30"]);
  });

  it("puts rent due on the 29th on February 29 in a leap year", () => {
    const periods = firstPeriods({ moveIn: "2028-01-01" }, [term("2028-01", RENT, 29)], 3);

    expect(periods.map((period) => period.dueDate)).toEqual(["2028-01-29", "2028-02-29", "2028-03-29"]);
  });

  it("files each period under the month its due date falls in", () => {
    expect(firstPeriods({}, [term("2026-01", RENT, 15)], 2).map((period) => period.month)).toEqual([
      "2026-01",
      "2026-02",
    ]);
  });

  it("uses the newest term that has started and ignores deleted terms", () => {
    const terms = [term("2026-01", RENT), term("2026-03", 160000), term("2026-02", 999999, 1, { deletedAt: 1 })];

    expect(firstPeriods({}, terms).map((period) => period.amountCents)).toEqual([RENT, RENT, 160000, 160000]);
  });

  it("has no period in months before the first term starts", () => {
    expect(dueDates({}, [term("2026-03", RENT)], 1)).toEqual(["2026-03-01"]);
  });

  it("has no periods without an active term", () => {
    expect(dueDates({}, [term("2026-01", RENT, 1, { deletedAt: 1 })])).toEqual([]);
  });

  it("ends each period the day before the next due date, taking a due-day change into account", () => {
    const periods = firstPeriods({}, [term("2026-01", RENT, 1), term("2026-03", RENT, 15)], 3);

    expect(periods.map((period) => [period.dueDate, period.lastDay])).toEqual([
      ["2026-01-01", "2026-01-31"],
      ["2026-02-01", "2026-03-14"],
      ["2026-03-15", "2026-04-14"],
    ]);
  });

  it("charges the last month when the move-out date is the day before the next due date", () => {
    expect(dueDates({ moveOut: "2026-03-31" }, [term("2026-01", RENT)], 10)).toEqual([
      "2026-01-01",
      "2026-02-01",
      "2026-03-01",
    ]);
  });

  it("does not charge a partial last month and reports it", () => {
    const tenant = { ...defaultTenant, moveOut: "2026-03-20" };
    const terms = [term("2026-01", RENT)];

    expect(dueDates(tenant, terms, 10)).toEqual(["2026-01-01", "2026-02-01"]);
    expect(partialMoveOutPeriod(tenant, terms)).toEqual({ start: "2026-03-01", end: "2026-03-31" });
  });

  it("does not charge the month when the move-out date equals a due date", () => {
    const tenant = { ...defaultTenant, moveOut: "2026-03-01" };

    expect(dueDates(tenant, [term("2026-01", RENT)], 10)).toEqual(["2026-01-01", "2026-02-01"]);
    expect(partialMoveOutPeriod(tenant, [term("2026-01", RENT)])).toEqual({ start: "2026-03-01", end: "2026-03-31" });
  });

  it("reports no partial month when the tenant stays to the end of a period or has not moved out", () => {
    expect(partialMoveOutPeriod({ ...defaultTenant, moveOut: "2026-03-31" }, [term("2026-01", RENT)])).toBeUndefined();
    expect(partialMoveOutPeriod(defaultTenant, [term("2026-01", RENT)])).toBeUndefined();
  });
});
