import { describe, expect, it } from "vitest";
import { customDebt, lateFee, ledgerInput, payment, RENT } from "./ledgerBuilder.fixture";
import type { TenantLedgerInput } from "./ledgerInput";
import { ledgerRows } from "./ledgerRows";
import { computeTenantLedger } from "./tenantLedger";

function rowsFor(input: TenantLedgerInput) {
  const ledger = computeTenantLedger(input);
  return ledgerRows({
    today: input.today,
    rentPeriods: ledger.rentPeriods,
    debts: input.debts,
    payments: input.payments,
    mayNotApplyDebtIds: ledger.summary.mayNotApplyDebtIds,
  });
}

function balancesOf(input: TenantLedgerInput) {
  return rowsFor(input).map(({ key, balanceOwedCents, balanceCreditCents }) => ({
    key,
    balanceOwedCents,
    balanceCreditCents,
  }));
}

const workedExample = ledgerInput({
  today: "2026-03-02",
  debts: [lateFee("2026-02-06", 5000, "2026-02")],
  payments: [payment("2026-01-03", RENT), payment("2026-02-04", 100000), payment("2026-03-02", RENT)],
});

describe("ledgerRows", () => {
  it("runs the product spec's worked example balance entry by entry", () => {
    expect(
      rowsFor(workedExample).map(({ key, position, balanceOwedCents }) => [key, position, balanceOwedCents]),
    ).toEqual([
      ["rent:2026-01", 0, 150000],
      ["payment:pay-2026-01-03", 1, 0],
      ["rent:2026-02", 2, 150000],
      ["payment:pay-2026-02-04", 3, 50000],
      ["debt:fee-2026-02", 4, 55000],
      ["rent:2026-03", 5, 205000],
      ["payment:pay-2026-03-02", 6, 55000],
    ]);
  });

  it("describes a rent row by month, due date and amount", () => {
    const [january] = rowsFor(workedExample);

    expect(january).toEqual({
      key: "rent:2026-01",
      kind: "rent",
      month: "2026-01",
      date: "2026-01-01",
      amountCents: RENT,
      position: 0,
      balanceOwedCents: RENT,
      mayNotApply: false,
    });
  });

  it("describes a payment row by its entry id and date received", () => {
    const [, janPayment] = rowsFor(workedExample);

    expect(janPayment).toMatchObject({
      key: "payment:pay-2026-01-03",
      kind: "payment",
      entryId: "pay-2026-01-03",
      date: "2026-01-03",
      amountCents: RENT,
    });
  });

  it.each([
    {
      name: "an overpayment shows as credit",
      input: ledgerInput({ today: "2026-01-10", payments: [payment("2026-01-02", 200000)] }),
      expected: [
        { key: "rent:2026-01", balanceOwedCents: 150000 },
        { key: "payment:pay-2026-01-02", balanceCreditCents: 50000 },
      ],
    },
    {
      name: "a zero balance is Paid up, not absent",
      input: ledgerInput({ today: "2026-01-10", payments: [payment("2026-01-02", RENT)] }),
      expected: [
        { key: "rent:2026-01", balanceOwedCents: 150000 },
        { key: "payment:pay-2026-01-02", balanceOwedCents: 0 },
      ],
    },
    {
      name: "a cancelled payment has no balance and changes nothing",
      input: ledgerInput({ today: "2026-01-10", payments: [payment("2026-01-02", RENT, { deletedAt: 1 })] }),
      expected: [{ key: "rent:2026-01", balanceOwedCents: 150000 }, { key: "payment:pay-2026-01-02" }],
    },
    {
      name: "a bounced payment has no balance and changes nothing",
      input: ledgerInput({ today: "2026-01-10", payments: [payment("2026-01-02", RENT, { bouncedAt: 1 })] }),
      expected: [{ key: "rent:2026-01", balanceOwedCents: 150000 }, { key: "payment:pay-2026-01-02" }],
    },
    {
      name: "a cancelled debt has no balance and changes nothing",
      input: ledgerInput({ today: "2026-01-10", debts: [customDebt("2026-01-05", 7000, { deletedAt: 1 })] }),
      expected: [{ key: "rent:2026-01", balanceOwedCents: 150000 }, { key: "debt:debt-2026-01-05" }],
    },
    {
      name: "a debt dated after today is not a charge yet",
      input: ledgerInput({ today: "2026-01-10", debts: [customDebt("2026-01-20", 7000)] }),
      expected: [{ key: "rent:2026-01", balanceOwedCents: 150000 }, { key: "debt:debt-2026-01-20" }],
    },
  ])("$name", ({ input, expected }) => {
    expect(balancesOf(input)).toEqual(
      expected.map((row) => ({ balanceOwedCents: undefined, balanceCreditCents: undefined, ...row })),
    );
  });

  it("orders entries on one date as rent, custom debt, late fee, payment, then creation time", () => {
    const input = ledgerInput({
      today: "2026-01-10",
      debts: [
        lateFee("2026-01-01", 1000, "2025-12", { id: "fee" }),
        customDebt("2026-01-01", 2000, { id: "late-created", creationTime: 20 }),
        customDebt("2026-01-01", 3000, { id: "early-created", creationTime: 10 }),
      ],
      payments: [payment("2026-01-01", 500, { id: "pay" })],
    });

    expect(rowsFor(input).map((row) => row.key)).toEqual([
      "rent:2026-01",
      "debt:early-created",
      "debt:late-created",
      "debt:fee",
      "payment:pay",
    ]);
  });

  it("flags a late fee the engine says may not apply", () => {
    const input = ledgerInput({
      today: "2026-03-20",
      debts: [lateFee("2026-02-06", 5000, "2026-02")],
      payments: [payment("2026-02-03", RENT), payment("2026-01-03", RENT), payment("2026-03-01", RENT)],
    });

    const fee = rowsFor(input).find((row) => row.key === "debt:fee-2026-02");

    expect(fee?.mayNotApply).toBe(true);
    expect(rowsFor(input).filter((row) => row.mayNotApply)).toHaveLength(1);
  });

  it.each([
    ["the worked example", workedExample],
    ["a credit balance", ledgerInput({ today: "2026-02-10", payments: [payment("2026-01-02", 500000)] })],
    [
      "bounced, cancelled and future entries",
      ledgerInput({
        today: "2026-02-10",
        debts: [
          customDebt("2026-02-20", 100),
          customDebt("2026-01-05", 200, { deletedAt: 1 }),
          customDebt("2026-01-06", 300),
        ],
        payments: [
          payment("2026-01-02", 1000, { bouncedAt: 1 }),
          payment("2026-01-04", 2000, { deletedAt: 1 }),
          payment("2026-02-02", 40000),
        ],
      }),
    ],
  ])("ends on the summary's net balance for %s", (_name, input) => {
    const { balance } = computeTenantLedger(input).summary;
    const lastRunning = rowsFor(input)
      .filter((row) => row.balanceOwedCents !== undefined || row.balanceCreditCents !== undefined)
      .at(-1);

    const netOwedCents =
      balance.rentOwedCents + balance.lateFeesOwedCents + balance.otherOwedCents - balance.creditCents;

    expect((lastRunning?.balanceOwedCents ?? 0) - (lastRunning?.balanceCreditCents ?? 0)).toBe(netOwedCents);
  });
});
