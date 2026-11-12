import { describe, expect, it } from "vitest";
import { addDays, type CalendarDate } from "./calendarDate";
import { chargeKey } from "./charges";
import { customDebt, lateFee, ledgerInput, payment, term } from "./ledgerBuilder.fixture";
import type { TenantLedgerInput } from "./ledgerInput";
import { computeTenantLedger } from "./tenantLedger";

const LEDGER_COUNT = 300;
const START: CalendarDate = "2025-01-01";
const SPAN_DAYS = 900;

// mulberry32: a tiny seeded generator keeps failures reproducible without a property-testing library.
function seededRandom(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state + 0x6d2b79f5) | 0;
    let mixed = Math.imul(state ^ (state >>> 15), 1 | state);
    mixed = (mixed + Math.imul(mixed ^ (mixed >>> 7), 61 | mixed)) ^ mixed;
    return ((mixed ^ (mixed >>> 14)) >>> 0) / 4294967296;
  };
}

function randomLedger(seed: number): TenantLedgerInput {
  const random = seededRandom(seed);
  const integer = (min: number, max: number) => min + Math.floor(random() * (max - min + 1));
  const date = () => addDays(START, integer(0, SPAN_DAYS));
  const cents = () => integer(1, 300000);
  const moveIn = date();
  return ledgerInput({
    today: date(),
    group: { paymentOrder: random() < 0.5 ? "rentFirst" : "byDate", graceDays: integer(0, 10) },
    tenant: {
      moveIn,
      moveOut: random() < 0.3 ? addDays(moveIn, integer(0, 400)) : undefined,
      status: random() < 0.2 ? "archived" : "active",
    },
    rentTerms: Array.from({ length: integer(1, 3) }, (_, index) =>
      term(date().slice(0, 7), cents(), integer(1, 31), { id: `term-${index}` }),
    ),
    debts: Array.from({ length: integer(0, 6) }, (_, index) =>
      random() < 0.5
        ? lateFee(date(), cents(), date().slice(0, 7), { id: `fee-${index}`, creationTime: index })
        : customDebt(date(), cents(), { id: `debt-${index}`, creationTime: index }),
    ),
    payments: Array.from({ length: integer(0, 15) }, (_, index) =>
      payment(date(), cents(), { id: `pay-${index}`, creationTime: index, bouncedAt: random() < 0.1 ? 1 : undefined }),
    ),
  });
}

function sumBy<T>(items: readonly T[], key: (item: T) => string, amount: (item: T) => number): Map<string, number> {
  const totals = new Map<string, number>();
  for (const item of items) {
    totals.set(key(item), (totals.get(key(item)) ?? 0) + amount(item));
  }
  return totals;
}

describe.each(Array.from({ length: LEDGER_COUNT }, (_, seed) => seed))("random ledger %i", (seed) => {
  const input = randomLedger(seed);
  const ledger = computeTenantLedger(input);

  it("never allocates more than a payment holds", () => {
    const allocated = sumBy(
      ledger.allocations,
      (allocation) => allocation.paymentId,
      (allocation) => allocation.amountCents,
    );
    for (const payment of input.payments) {
      expect(allocated.get(payment.id) ?? 0).toBeLessThanOrEqual(payment.amountCents);
    }
  });

  it("never allocates more than a charge is worth, and only positive whole cents", () => {
    const allocated = sumBy(
      ledger.allocations,
      (allocation) => chargeKey(allocation.charge),
      (allocation) => allocation.amountCents,
    );
    for (const period of ledger.rentPeriods) {
      expect(allocated.get(`rent:${period.month}`) ?? 0).toBeLessThanOrEqual(period.amountCents);
    }
    for (const debt of input.debts) {
      expect(allocated.get(`debt:${debt.id}`) ?? 0).toBeLessThanOrEqual(debt.amountCents);
    }
    expect(
      ledger.allocations.every(
        (allocation) => Number.isSafeInteger(allocation.amountCents) && allocation.amountCents > 0,
      ),
    ).toBe(true);
  });

  it("balances: owed minus credit equals charges minus payments", () => {
    const { rentOwedCents, lateFeesOwedCents, otherOwedCents, creditCents } = ledger.summary.balance;
    const charged = ledger.rentPeriods.filter((period) => period.charged);
    const charges =
      charged.reduce((total, period) => total + period.amountCents, 0) +
      input.debts.filter((debt) => debt.date <= input.today).reduce((total, debt) => total + debt.amountCents, 0);
    const payments = input.payments
      .filter((item) => item.bouncedAt === undefined)
      .reduce((total, item) => total + item.amountCents, 0);

    expect(rentOwedCents + lateFeesOwedCents + otherOwedCents - creditCents).toBe(charges - payments);
    expect(Math.min(rentOwedCents + lateFeesOwedCents + otherOwedCents, creditCents)).toBe(0);
  });

  it("is never late while archived and always returns twelve calendar months", () => {
    if (input.tenant.status === "archived") {
      expect(ledger.summary.state).not.toBe("late");
    }
    expect(ledger.calendarMonths).toHaveLength(12);
  });
});
