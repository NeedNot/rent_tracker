import { describe, expect, it } from "vitest";
import { ledgerInput, payment, RENT } from "./ledgerBuilder.fixture";
import { monthStateChanges } from "./monthStateChanges";
import { computeTenantLedger } from "./tenantLedger";

const TODAY = "2026-03-20";

function statesAfterPayments(payments: ReturnType<typeof payment>[]) {
  return computeTenantLedger(ledgerInput({ today: TODAY, payments })).rentPeriods;
}

describe("month state changes", () => {
  it.each([
    ["no change", [payment("2026-01-02", RENT)], [payment("2026-01-02", RENT)], []],
    [
      "a larger payment clears the oldest month",
      [payment("2026-01-02", RENT)],
      [payment("2026-01-02", RENT * 2)],
      [{ month: "2026-02", before: "owed", after: "paid" }],
    ],
    [
      "a smaller payment leaves its month partial",
      [payment("2026-01-02", RENT)],
      [payment("2026-01-02", RENT / 2)],
      [{ month: "2026-01", before: "paid", after: "partial" }],
    ],
    [
      "a payment into the future credit",
      [],
      [payment("2026-01-02", RENT * 4)],
      [
        { month: "2026-01", before: "owed", after: "paid" },
        { month: "2026-02", before: "owed", after: "paid" },
        { month: "2026-03", before: "owed", after: "paid" },
        { month: "2026-04", before: "upcoming", after: "prepaid" },
      ],
    ],
  ])("%s", (_, before, after, changes) => {
    expect(monthStateChanges(statesAfterPayments(before), statesAfterPayments(after), TODAY)).toEqual(changes);
  });
});
