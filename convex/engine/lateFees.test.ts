import { describe, expect, it } from "vitest";
import { percentLateFeeCents } from "./lateFees";
import { lateFee, ledgerInput, payment, RENT, term } from "./ledgerBuilder.fixture";
import { computeTenantLedger } from "./tenantLedger";

describe("suggested late fee amount", () => {
  it.each([
    [123450, 6173],
    [150000, 7500],
    [30, 2],
    [10, 1],
    [9, 0],
    [100000000, 5000000],
  ])("5%% of %i cents rounds half up to %i cents", (rentCents, feeCents) => {
    expect(percentLateFeeCents(rentCents)).toBe(feeCents);
  });

  it.each([
    ["5% of that month's rent by default", {}, {}, 6173],
    ["the group's flat amount over 5%", { lateFeeFlatCents: 5000 }, {}, 5000],
    ["the tenant's flat amount over the group's", { lateFeeFlatCents: 5000 }, { lateFeeFlatOverrideCents: 2500 }, 2500],
  ])("uses %s", (_, group, tenant, amountCents) => {
    const ledger = computeTenantLedger(
      ledgerInput({ today: "2026-01-10", group, tenant, rentTerms: [term("2026-01", 123450)] }),
    );

    expect(ledger.summary.lateFeeSuggestion).toEqual({ forMonth: "2026-01", amountCents });
  });
});

describe("late fee suggestion", () => {
  it("suggests the most recent currently late month that has no active late fee", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-03-20",
        debts: [lateFee("2026-03-07", 7500, "2026-03"), lateFee("2026-02-07", 7500, "2026-02", { deletedAt: 1 })],
      }),
    );

    expect(ledger.summary.lateFeeSuggestion).toEqual({ forMonth: "2026-02", amountCents: 7500 });
  });

  it("suggests nothing when no month is currently late", () => {
    const ledger = computeTenantLedger(
      ledgerInput({ today: "2026-02-20", payments: [payment("2026-02-10", RENT * 2)] }),
    );

    expect(ledger.summary.lateFeeSuggestion).toBeUndefined();
  });

  it("suggests nothing for an archived tenant", () => {
    const ledger = computeTenantLedger(ledgerInput({ today: "2026-02-20", tenant: { status: "archived" } }));

    expect(ledger.summary.lateFeeSuggestion).toBeUndefined();
  });
});

describe("late fee that may not apply", () => {
  it("flags a fee once a back-dated payment shows the month was paid within grace", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-01-20",
        debts: [lateFee("2026-01-07", 7500, "2026-01")],
        payments: [payment("2026-01-05", RENT + 7500)],
      }),
    );

    expect(ledger.summary.mayNotApplyDebtIds).toEqual(["fee-2026-01"]);
  });

  it("does not flag a fee for a month that was late", () => {
    const ledger = computeTenantLedger(
      ledgerInput({ today: "2026-01-20", debts: [lateFee("2026-01-07", 7500, "2026-01")] }),
    );

    expect(ledger.summary.mayNotApplyDebtIds).toEqual([]);
  });

  it("does not flag a fee while its month is still inside grace", () => {
    const ledger = computeTenantLedger(
      ledgerInput({ today: "2026-01-04", debts: [lateFee("2026-01-03", 7500, "2026-01")] }),
    );

    expect(ledger.summary.mayNotApplyDebtIds).toEqual([]);
  });

  it("flags a fee for a month with no rent charge", () => {
    const ledger = computeTenantLedger(
      ledgerInput({
        today: "2026-02-10",
        debts: [
          lateFee("2026-02-10", 7500, "2025-12", { id: "before-move-in" }),
          lateFee("2026-02-10", 7500, "2026-05"),
        ],
      }),
    );

    expect(ledger.summary.mayNotApplyDebtIds).toEqual(["before-move-in", "fee-2026-05"]);
  });
});
