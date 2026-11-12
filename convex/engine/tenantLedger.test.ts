import { describe, expect, it } from "vitest";
import { customDebt, lateFee, ledgerInput, payment, RENT, term } from "./ledgerBuilder.fixture";
import { computeTenantLedger } from "./tenantLedger";

describe("tenant summary", () => {
  it("splits what is owed into rent, late fees and other charges", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-02-03",
        debts: [lateFee("2026-01-07", 7500, "2026-01"), customDebt("2026-01-20", 4000)],
        payments: [payment("2026-01-10", RENT)],
      }),
    );

    expect(ledger.summary.balance).toEqual({
      rentOwedCents: RENT,
      lateFeesOwedCents: 7500,
      otherOwedCents: 4000,
      creditCents: 0,
    });
  });

  it("keeps money left after every charge as credit", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-01-10", payments: [payment("2026-01-02", 200000)] }));

    expect(ledger.summary.balance).toEqual({
      rentOwedCents: 0,
      lateFeesOwedCents: 0,
      otherOwedCents: 0,
      creditCents: 50000,
    });
    expect(ledger.summary.state).toBe("paidUp");
  });

  it.each([
    ["late with an unpaid month past grace", "2026-01-10", [], [], "late"],
    ["owes inside grace", "2026-01-03", [], [], "owes"],
    [
      "owes an unpaid custom debt with no late month",
      "2026-01-10",
      [payment("2026-01-02", RENT)],
      [customDebt("2026-01-05", 1000)],
      "owes",
    ],
    ["paid up when nothing is owed", "2026-01-10", [payment("2026-01-02", RENT)], [], "paidUp"],
  ])("is %s", (_, today, payments, debts, state) => {
    expect(computeTenantLedger(ledgerInput({ today, payments, debts })).summary.state).toBe(state);
  });

  it("gives the first due date after today as the next due date", () => {
    expect(computeTenantLedger(ledgerInput({ today: "2026-03-01" })).summary.nextDueDate).toBe("2026-04-01");
    expect(computeTenantLedger(ledgerInput({ today: "2026-02-28" })).summary.nextDueDate).toBe("2026-03-01");
  });

  it("has no next due date after the last charged period before move-out", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-03-10", tenant: { moveOut: "2026-03-31" } }));

    expect(ledger.summary.nextDueDate).toBeUndefined();
  });

  it("reports the partial last month that won't be charged", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-03-10", tenant: { moveOut: "2026-03-17" } }));

    expect(ledger.summary.partialMoveOutPeriod).toEqual({ start: "2026-03-01", end: "2026-03-31" });
    expect(ledger.rentPeriods.map((period) => period.month)).toEqual(["2026-01", "2026-02"]);
  });

  it("does not change past balances when rent changes from a later month", () => {
    const before = computeTenantLedger(ledgerInput({ today: "2026-03-10" }));
    const after = computeTenantLedger(
      ledgerInput({ today: "2026-03-10", rentTerms: [term("2026-01", RENT), term("2026-04", 160000)] }),
    );

    expect(after.summary.balance).toEqual(before.summary.balance);
  });
});
