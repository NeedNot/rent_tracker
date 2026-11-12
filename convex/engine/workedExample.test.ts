import { describe, expect, it } from "vitest";
import { lateFee, ledgerInput, payment } from "./ledgerBuilder.fixture";
import type { PaymentOrder } from "./ledgerInput";
import { computeTenantLedger } from "./tenantLedger";

function workedExample(paymentOrder: PaymentOrder) {
  return computeTenantLedger(
    ledgerInput({
      today: "2026-03-02",
      group: { paymentOrder, graceDays: 5 },
      debts: [lateFee("2026-02-06", 5000, "2026-02")],
      payments: [payment("2026-01-03", 150000), payment("2026-02-04", 100000), payment("2026-03-02", 150000)],
    }),
  );
}

function firstQuarter(ledger: ReturnType<typeof workedExample>) {
  return ledger.calendarMonths
    .slice(0, 3)
    .map(({ month, state, owedCents, hasUnpaidLateFee }) => ({ month, state, owedCents, hasUnpaidLateFee }));
}

describe("product spec worked example", () => {
  it("Rent first: the Mar 2 payment finishes February's rent, March is $500 short and the fee is unpaid", () => {
    const ledger = workedExample("rentFirst");

    expect(ledger.allocations.filter((allocation) => allocation.paymentId === "pay-2026-03-02")).toEqual([
      { paymentId: "pay-2026-03-02", charge: { kind: "rent", month: "2026-02" }, amountCents: 50000 },
      { paymentId: "pay-2026-03-02", charge: { kind: "rent", month: "2026-03" }, amountCents: 100000 },
    ]);
    expect(firstQuarter(ledger)).toEqual([
      { month: "2026-01", state: "paid", owedCents: 0, hasUnpaidLateFee: false },
      { month: "2026-02", state: "paid", owedCents: 0, hasUnpaidLateFee: true },
      { month: "2026-03", state: "partial", owedCents: 50000, hasUnpaidLateFee: false },
    ]);
    expect(ledger.summary.balance).toEqual({
      rentOwedCents: 50000,
      lateFeesOwedCents: 5000,
      otherOwedCents: 0,
      creditCents: 0,
    });
  });

  it("By date: the Mar 2 payment finishes February, then the fee, and March is $550 short", () => {
    const ledger = workedExample("byDate");

    expect(ledger.allocations.filter((allocation) => allocation.paymentId === "pay-2026-03-02")).toEqual([
      { paymentId: "pay-2026-03-02", charge: { kind: "rent", month: "2026-02" }, amountCents: 50000 },
      { paymentId: "pay-2026-03-02", charge: { kind: "debt", debtId: "fee-2026-02" }, amountCents: 5000 },
      { paymentId: "pay-2026-03-02", charge: { kind: "rent", month: "2026-03" }, amountCents: 95000 },
    ]);
    expect(firstQuarter(ledger)).toEqual([
      { month: "2026-01", state: "paid", owedCents: 0, hasUnpaidLateFee: false },
      { month: "2026-02", state: "paid", owedCents: 0, hasUnpaidLateFee: false },
      { month: "2026-03", state: "partial", owedCents: 55000, hasUnpaidLateFee: false },
    ]);
    expect(ledger.summary.balance).toEqual({
      rentOwedCents: 55000,
      lateFeesOwedCents: 0,
      otherOwedCents: 0,
      creditCents: 0,
    });
  });

  it.each(["rentFirst", "byDate"] as const)(
    "%s: February was late, the tenant owes $550 and is not late now",
    (order) => {
      const ledger = workedExample(order);

      expect(ledger.rentPeriods.find((period) => period.month === "2026-02")).toMatchObject({
        wasLate: true,
        currentlyLate: false,
      });
      expect(ledger.summary).toMatchObject({ state: "owes", mayNotApplyDebtIds: [] });
      expect(ledger.summary.lateFeeSuggestion).toBeUndefined();
    },
  );
});
