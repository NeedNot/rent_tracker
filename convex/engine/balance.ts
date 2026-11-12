import { coveredCents, type QueuedPayment } from "./allocation";
import type { Charge, ChargeCategory } from "./charges";

export interface Balance {
  rentOwedCents: number;
  lateFeesOwedCents: number;
  otherOwedCents: number;
  creditCents: number;
}

export function balanceOf(
  charges: readonly Charge[],
  coverage: ReadonlyMap<string, number>,
  unspentPayments: readonly QueuedPayment[],
): Balance {
  const owedIn = (category: ChargeCategory) =>
    charges
      .filter((charge) => charge.category === category)
      .reduce((total, charge) => total + charge.amountCents - coveredCents(coverage, charge.ref), 0);
  return {
    rentOwedCents: owedIn("rent"),
    lateFeesOwedCents: owedIn("lateFee"),
    otherOwedCents: owedIn("custom"),
    creditCents: unspentPayments.reduce((total, payment) => total + payment.amountCents, 0),
  };
}

export function totalOwedCents(balance: Balance): number {
  return balance.rentOwedCents + balance.lateFeesOwedCents + balance.otherOwedCents;
}
