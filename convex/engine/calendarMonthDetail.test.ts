import { describe, expect, it } from "vitest";
import type { CalendarMonth } from "./calendarDate";
import { customDebt, lateFee, ledgerInput, payment, RENT } from "./ledgerBuilder.fixture";
import type { PaymentOrder } from "./ledgerInput";
import { computeTenantLedger } from "./tenantLedger";

const AFTER_MARCH_GRACE = "2026-03-10";

function detail(input: Parameters<typeof ledgerInput>[0], month: CalendarMonth) {
  return computeTenantLedger(ledgerInput(input)).calendarMonths.find((status) => status.month === month);
}

function workedExample(paymentOrder: PaymentOrder) {
  return {
    today: AFTER_MARCH_GRACE,
    group: { paymentOrder },
    debts: [lateFee("2026-02-06", 5000, "2026-02")],
    payments: [payment("2026-01-03", RENT), payment("2026-02-04", 100000), payment("2026-03-02", RENT)],
  };
}

describe("calendar month detail on the product spec worked example", () => {
  it("Rent first: February is paid but was late with its $50 fee unpaid; March is partial with $1,000 paid by grace end", () => {
    expect(detail(workedExample("rentFirst"), "2026-02")).toMatchObject({
      state: "paid",
      wasLate: true,
      dueDate: "2026-02-01",
      graceEnd: "2026-02-06",
      paidByGraceEndCents: 100000,
      lateFeeCents: 5000,
      lateFeeUnpaidCents: 5000,
      hasUnpaidLateFee: true,
    });
    expect(detail(workedExample("rentFirst"), "2026-02")?.suggestedLateFeeCents).toBeUndefined();
    expect(detail(workedExample("rentFirst"), "2026-03")).toMatchObject({
      state: "partial",
      wasLate: true,
      dueDate: "2026-03-01",
      graceEnd: "2026-03-06",
      coveredCents: 100000,
      owedCents: 50000,
      paidByGraceEndCents: 100000,
      suggestedLateFeeCents: 7500,
    });
    expect(detail(workedExample("rentFirst"), "2026-03")?.lateFeeCents).toBeUndefined();
  });

  it("By date: the fee is paid before March rent, so March is $950 paid and $550 owed", () => {
    expect(detail(workedExample("byDate"), "2026-02")).toMatchObject({
      state: "paid",
      wasLate: true,
      lateFeeCents: 5000,
      lateFeeUnpaidCents: 0,
      hasUnpaidLateFee: false,
    });
    expect(detail(workedExample("byDate"), "2026-03")).toMatchObject({
      state: "partial",
      coveredCents: 95000,
      owedCents: 55000,
      paidByGraceEndCents: 95000,
    });
  });

  it("March is still due, not late, inside its grace period", () => {
    expect(detail({ ...workedExample("rentFirst"), today: "2026-03-02" }, "2026-03")).toMatchObject({
      state: "dueNotLate",
      wasLate: false,
      graceEnd: "2026-03-06",
      owedCents: 50000,
    });
    expect(
      detail({ ...workedExample("rentFirst"), today: "2026-03-02" }, "2026-03")?.paidByGraceEndCents,
    ).toBeUndefined();
  });
});

describe("calendar month detail", () => {
  it.each([
    ["paid on the last day of grace", "2026-01-06", false, RENT],
    ["paid the day after grace ends", "2026-01-07", true, 0],
  ])("a month %s", (_, dateReceived, wasLate, paidByGraceEndCents) => {
    expect(detail({ today: "2026-01-20", payments: [payment(dateReceived, RENT)] }, "2026-01")).toMatchObject({
      state: "paid",
      wasLate,
      paidByGraceEndCents,
    });
  });

  it.each([
    ["5% of the rent", {}, 7500],
    ["the group's flat fee", { group: { lateFeeFlatCents: 2500 } }, 2500],
    [
      "the tenant's flat fee over the group's",
      { group: { lateFeeFlatCents: 2500 }, tenant: { lateFeeFlatOverrideCents: 1000 } },
      1000,
    ],
  ])("suggests a fee for a late month with none, using %s", (_, overrides, suggestedLateFeeCents) => {
    expect(detail({ today: "2026-01-20", ...overrides }, "2026-01")).toMatchObject({
      wasLate: true,
      suggestedLateFeeCents,
    });
  });

  it("suggests no fee for a month that was not late", () => {
    const month = detail({ today: "2026-01-20", payments: [payment("2026-01-02", RENT)] }, "2026-01");

    expect(month?.suggestedLateFeeCents).toBeUndefined();
  });

  it("sums several late fees for one month and counts only the unpaid part", () => {
    const debts = [
      lateFee("2026-01-07", 5000, "2026-01", { id: "fee-a" }),
      lateFee("2026-01-10", 2500, "2026-01", { id: "fee-b" }),
    ];

    expect(
      detail({ today: "2026-01-20", debts, payments: [payment("2026-01-02", RENT + 5000)] }, "2026-01"),
    ).toMatchObject({
      lateFeeCents: 7500,
      lateFeeUnpaidCents: 2500,
      hasUnpaidLateFee: true,
    });
  });

  it("ignores a late fee dated after today and other kinds of debt", () => {
    const debts = [lateFee("2026-02-07", 5000, "2026-02"), customDebt("2026-01-05", 3000)];

    expect(detail({ today: "2026-01-20", debts }, "2026-02")).toMatchObject({ lateFeeCents: undefined });
    expect(detail({ today: "2026-01-20", debts }, "2026-01")).toMatchObject({ lateFeeCents: undefined });
  });

  it("describes a fully prepaid future month with its dates and no lateness", () => {
    expect(detail({ today: "2026-01-10", payments: [payment("2026-01-02", RENT * 2)] }, "2026-02")).toMatchObject({
      state: "prepaid",
      coveredCents: RENT,
      owedCents: 0,
      dueDate: "2026-02-01",
      graceEnd: "2026-02-06",
      wasLate: false,
    });
  });

  it("describes a partly prepaid future month as upcoming with what is covered", () => {
    const month = detail({ today: "2026-01-10", payments: [payment("2026-01-02", RENT * 2 + 50000)] }, "2026-03");

    expect(month).toMatchObject({ state: "upcoming", coveredCents: 50000, owedCents: 0, wasLate: false });
    expect(month?.paidByGraceEndCents).toBeUndefined();
  });

  it("leaves every detail blank for a month before move-in", () => {
    expect(detail({ today: "2026-03-10", tenant: { moveIn: "2026-03-01" } }, "2026-01")).toEqual({
      month: "2026-01",
      state: "notATenant",
      rentCents: 0,
      coveredCents: 0,
      owedCents: 0,
      hasUnpaidLateFee: false,
      wasLate: false,
    });
  });
});
