import { describe, expect, it } from "vitest";
import { type Charge, orderCharges } from "./charges";
import { customDebt, lateFee, ledgerInput, payment, RENT, term } from "./ledgerBuilder.fixture";
import { CREDIT_PROJECTION_LIMIT_PERIODS, computeTenantLedger } from "./tenantLedger";

function charge(id: string, category: Charge["category"], date: string, creationTime = 0): Charge {
  return { ref: { kind: "debt", debtId: id }, category, date, amountCents: 100, creationTime };
}

function ids(charges: Charge[]): string[] {
  return charges.map((item) => (item.ref.kind === "debt" ? item.ref.debtId : item.ref.month));
}

describe("charge order", () => {
  const charges = [
    charge("fee-jan", "lateFee", "2026-01-06"),
    charge("repair-feb", "custom", "2026-02-01", 2),
    charge("rent-feb", "rent", "2026-02-01"),
    charge("utility-feb", "custom", "2026-02-01", 1),
    charge("fee-feb", "lateFee", "2026-02-01"),
    charge("rent-jan", "rent", "2026-01-01"),
  ];

  it("Rent first takes rent by due date, then custom debts by date, then late fees by date", () => {
    expect(ids(orderCharges(charges, "rentFirst"))).toEqual([
      "rent-jan",
      "rent-feb",
      "utility-feb",
      "repair-feb",
      "fee-jan",
      "fee-feb",
    ]);
  });

  it("By date takes every charge oldest first; on the same date rent, custom debt, late fee, then creation time", () => {
    expect(ids(orderCharges(charges, "byDate"))).toEqual([
      "rent-jan",
      "fee-jan",
      "rent-feb",
      "utility-feb",
      "repair-feb",
      "fee-feb",
    ]);
  });
});

describe("allocation", () => {
  it("fills the oldest charge first and splits a payment across charges", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-02-10", payments: [payment("2026-02-03", 200000)] }));

    expect(ledger.allocations).toEqual([
      { paymentId: "pay-2026-02-03", charge: { kind: "rent", month: "2026-01" }, amountCents: RENT },
      { paymentId: "pay-2026-02-03", charge: { kind: "rent", month: "2026-02" }, amountCents: 50000 },
    ]);
  });

  it("applies payments by date received, then creation time, so a back-dated payment goes first", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-02-10",
        payments: [
          payment("2026-02-05", RENT, { id: "logged-first", creationTime: 1 }),
          payment("2026-01-03", RENT, { id: "back-dated", creationTime: 2 }),
        ],
      }),
    );

    expect(ledger.allocations.map((allocation) => [allocation.paymentId, allocation.charge])).toEqual([
      ["back-dated", { kind: "rent", month: "2026-01" }],
      ["logged-first", { kind: "rent", month: "2026-02" }],
    ]);
  });

  it("breaks a same-day tie between payments by creation time", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-01-10",
        payments: [
          payment("2026-01-02", 100000, { id: "second", creationTime: 2 }),
          payment("2026-01-02", 100000, { id: "first", creationTime: 1 }),
        ],
      }),
    );

    expect(ledger.allocations.map((allocation) => [allocation.paymentId, allocation.amountCents])).toEqual([
      ["first", 100000],
      ["second", 50000],
      ["second", 50000],
    ]);
  });

  it("ignores bounced and cancelled payments", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-01-10",
        payments: [
          payment("2026-01-02", RENT, { id: "bounced", bouncedAt: 5 }),
          payment("2026-01-03", RENT, { id: "cancelled", deletedAt: 6 }),
        ],
      }),
    );

    expect(ledger.allocations).toEqual([]);
    expect(ledger.summary.balance.rentOwedCents).toBe(RENT);
  });

  it("ignores cancelled debts and debts dated after today", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-01-10",
        debts: [customDebt("2026-01-05", 2000, { deletedAt: 1 }), customDebt("2026-01-20", 3000)],
      }),
    );

    expect(ledger.summary.balance.otherOwedCents).toBe(0);
  });

  it("projects leftover credit onto future rent periods in order", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-01-10", payments: [payment("2026-01-02", 375000)] }));

    expect(ledger.allocations.map((allocation) => [allocation.charge, allocation.amountCents])).toEqual([
      [{ kind: "rent", month: "2026-01" }, RENT],
      [{ kind: "rent", month: "2026-02" }, RENT],
      [{ kind: "rent", month: "2026-03" }, 75000],
    ]);
    expect(ledger.summary.balance.creditCents).toBe(225000);
  });

  it("never projects credit onto future debts", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-01-10",
        debts: [customDebt("2026-01-20", 3000)],
        payments: [payment("2026-01-02", RENT + 3000)],
      }),
    );

    expect(ledger.allocations.at(-1)).toEqual({
      paymentId: "pay-2026-01-02",
      charge: { kind: "rent", month: "2026-02" },
      amountCents: 3000,
    });
  });

  it("stops projecting credit at move-out", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-01-10",
        tenant: { moveOut: "2026-02-28" },
        payments: [payment("2026-01-02", RENT * 5)],
      }),
    );

    expect(ledger.allocations.map((allocation) => allocation.charge)).toEqual([
      { kind: "rent", month: "2026-01" },
      { kind: "rent", month: "2026-02" },
    ]);
    expect(ledger.summary.balance.creditCents).toBe(RENT * 4);
  });

  it(`stops projecting credit after ${CREDIT_PROJECTION_LIMIT_PERIODS} future periods`, () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-01-10",
        rentTerms: [term("2026-01", 1)],
        payments: [payment("2026-01-02", 100000000)],
      }),
    );

    expect(ledger.allocations).toHaveLength(1 + CREDIT_PROJECTION_LIMIT_PERIODS);
    expect(ledger.rentPeriods.filter((period) => !period.charged)).toHaveLength(CREDIT_PROJECTION_LIMIT_PERIODS);
  });

  it("covers a late fee only after rent under Rent first", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-02-10",
        debts: [lateFee("2026-01-06", 5000, "2026-01")],
        payments: [payment("2026-02-08", RENT * 2)],
      }),
    );

    expect(ledger.summary.balance).toEqual({
      rentOwedCents: 0,
      lateFeesOwedCents: 5000,
      otherOwedCents: 0,
      creditCents: 0,
    });
  });
});
