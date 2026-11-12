import { describe, expect, it } from "vitest";
import type { CalendarMonth } from "./calendarDate";
import { lateFee, ledgerInput, payment, RENT } from "./ledgerBuilder.fixture";
import type { TenantLedgerInput } from "./ledgerInput";
import { computeTenantLedger } from "./tenantLedger";

function periodOf(input: TenantLedgerInput, month: CalendarMonth) {
  return computeTenantLedger(input).rentPeriods.find((period) => period.month === month);
}

describe("late months", () => {
  it.each([
    ["paid in full on the last day of grace is not late", "2026-01-06", RENT, "2026-02-10", false, false],
    ["paid the day after grace ends was late but is no longer late", "2026-01-07", RENT, "2026-01-10", true, false],
    ["a partial payment within grace still counts as late", "2026-01-03", 100000, "2026-01-10", true, true],
  ])("%s", (_, dateReceived, amountCents, today, wasLate, currentlyLate) => {
    const input = ledgerInput({ today, payments: [payment(dateReceived, amountCents)] });

    expect(periodOf(input, "2026-01")).toMatchObject({ wasLate, currentlyLate });
  });

  it.each([
    ["is not late on the last day of grace", 5, undefined, "2026-01-06", false],
    ["is late the day after grace ends", 5, undefined, "2026-01-07", true],
    ["with zero grace days is late the day after the due date", 0, undefined, "2026-01-02", true],
    ["with zero grace days is not late on the due date", 0, undefined, "2026-01-01", false],
    ["uses the tenant's grace override over the group's", 5, 10, "2026-01-08", false],
  ])("an unpaid month %s", (_, graceDays, graceDaysOverride, today, late) => {
    const input = ledgerInput({ today, group: { graceDays }, tenant: { graceDaysOverride } });

    expect(periodOf(input, "2026-01")?.wasLate).toBe(late);
    expect(computeTenantLedger(input).summary.state).toBe(late ? "late" : "owes");
  });

  it("By date lets an earlier late fee take part of a payment, so the next month is late", () => {
    const shared = {
      today: "2026-02-20",
      debts: [lateFee("2026-01-07", 5000, "2026-01")],
      payments: [payment("2026-01-10", RENT), payment("2026-02-02", RENT)],
    };

    expect(periodOf(ledgerInput({ ...shared, group: { paymentOrder: "byDate" } }), "2026-02")?.wasLate).toBe(true);
    expect(periodOf(ledgerInput(shared), "2026-02")?.wasLate).toBe(false);
  });

  it("judges lateness only by payments received and charges dated by the end of grace", () => {
    const input = ledgerInput({
      today: "2026-03-20",
      payments: [payment("2026-01-03", RENT), payment("2026-03-02", RENT * 2)],
    });

    expect(periodOf(input, "2026-02")).toMatchObject({ wasLate: true, currentlyLate: false });
    expect(periodOf(input, "2026-03")).toMatchObject({ wasLate: false, currentlyLate: false });
  });

  it("never marks an archived tenant as late", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-03-20", tenant: { status: "archived" } }));

    expect(ledger.summary.state).toBe("owes");
    expect(ledger.summary.lateFeeSuggestion).toBeUndefined();
  });
});
