import { describe, expect, it } from "vitest";
import type { CalendarMonth } from "./calendarDate";
import { lateFee, ledgerInput, payment, RENT, term } from "./ledgerBuilder.fixture";
import type { TenantLedgerInput } from "./ledgerInput";
import { computeTenantLedger } from "./tenantLedger";

function monthOf(input: TenantLedgerInput, month: CalendarMonth) {
  return computeTenantLedger(input).calendarMonths.find((status) => status.month === month);
}

describe("calendar month state", () => {
  it.each([
    ["paid when fully covered", "2026-01-20", [payment("2026-01-10", RENT)], "paid", 0],
    ["paid when fully covered inside grace", "2026-01-03", [payment("2026-01-02", RENT)], "paid", 0],
    ["partial when some is covered after grace", "2026-01-20", [payment("2026-01-02", 50000)], "partial", 100000],
    ["due, not late when partly covered in grace", "2026-01-03", [payment("2026-01-02", 50000)], "dueNotLate", 100000],
    ["partial on the day after grace ends", "2026-01-07", [payment("2026-01-02", 50000)], "partial", 100000],
    ["owed when nothing is covered after grace", "2026-01-07", [], "owed", RENT],
    ["due, not late when nothing is covered inside grace", "2026-01-06", [], "dueNotLate", RENT],
  ])("a charged month is %s", (_, today, payments, state, owedCents) => {
    expect(monthOf(ledgerInput({ today, payments }), "2026-01")).toMatchObject({ state, owedCents, rentCents: RENT });
  });

  it("shows a future month as upcoming, and as prepaid only when credit fully covers it", () => {
    const input = ledgerInput({ today: "2026-01-10", payments: [payment("2026-01-02", RENT * 2 + 50000)] });

    expect(monthOf(input, "2026-02")).toMatchObject({ state: "prepaid", coveredCents: RENT, owedCents: 0 });
    expect(monthOf(input, "2026-03")).toMatchObject({ state: "upcoming", coveredCents: 50000, owedCents: 0 });
    expect(monthOf(input, "2026-04")).toMatchObject({ state: "upcoming", coveredCents: 0 });
  });

  it("shows months before move-in and after move-out as not a tenant", () => {
    const ledger = computeTenantLedger(
      ledgerInput({ today: "2026-12-20", tenant: { moveIn: "2026-03-01", moveOut: "2026-08-31" } }),
    );

    expect(ledger.calendarMonths.map((status) => status.state)).toEqual([
      "notATenant",
      "notATenant",
      "owed",
      "owed",
      "owed",
      "owed",
      "owed",
      "owed",
      "notATenant",
      "notATenant",
      "notATenant",
      "notATenant",
    ]);
  });

  it("shows a year with no rent charges as all blank", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-03-10", calendarYear: 2024 }));

    expect(ledger.calendarMonths.every((status) => status.state === "notATenant")).toBe(true);
    expect(ledger.calendarMonths[0]?.month).toBe("2024-01");
  });

  it("builds any requested year, projecting credit into it", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-12-10",
        calendarYear: 2027,
        tenant: { moveIn: "2026-12-01" },
        payments: [payment("2026-12-02", RENT * 3)],
      }),
    );

    expect(ledger.calendarMonths.slice(0, 3).map((status) => status.state)).toEqual(["prepaid", "prepaid", "upcoming"]);
  });

  it("places a period in the square of the month its due date falls in", () => {
    const input = ledgerInput({
      today: "2026-03-16",
      tenant: { moveIn: "2026-03-01" },
      rentTerms: [term("2026-03", RENT, 15)],
    });

    expect(monthOf(input, "2026-03")).toMatchObject({ state: "dueNotLate", rentCents: RENT });
  });

  it("marks a month whose late fee is unpaid, and clears the marker once the fee is paid", () => {
    const fee = lateFee("2026-01-07", 7500, "2026-01");
    const unpaid = ledgerInput({ today: "2026-01-20", debts: [fee], payments: [payment("2026-01-10", RENT)] });
    const paid = ledgerInput({ today: "2026-01-20", debts: [fee], payments: [payment("2026-01-10", RENT + 7500)] });

    expect(monthOf(unpaid, "2026-01")).toMatchObject({ state: "paid", hasUnpaidLateFee: true });
    expect(monthOf(paid, "2026-01")).toMatchObject({ state: "paid", hasUnpaidLateFee: false });
  });
});
