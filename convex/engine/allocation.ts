import { type CalendarDate, compareText } from "./calendarDate";
import { type Charge, type ChargeRef, chargeKey } from "./charges";
import type { PaymentRow } from "./ledgerInput";

export interface Allocation {
  paymentId: string;
  charge: ChargeRef;
  amountCents: number;
}

export interface QueuedPayment {
  paymentId: string;
  dateReceived: CalendarDate;
  amountCents: number;
}

export interface AllocationResult {
  allocations: Allocation[];
  unspent: QueuedPayment[];
}

export function paymentQueue(payments: readonly PaymentRow[]): QueuedPayment[] {
  return payments
    .filter((payment) => payment.deletedAt === undefined && payment.bouncedAt === undefined)
    .sort((a, b) => compareText(a.dateReceived, b.dateReceived) || a.creationTime - b.creationTime)
    .map((payment) => ({
      paymentId: payment.id,
      dateReceived: payment.dateReceived,
      amountCents: payment.amountCents,
    }));
}

export function allocate(payments: readonly QueuedPayment[], charges: Iterable<Charge>): AllocationResult {
  const remaining = payments.map((payment) => ({ ...payment }));
  const allocations: Allocation[] = [];
  let next = 0;
  for (const charge of charges) {
    let owedCents = charge.amountCents;
    for (let payment = remaining[next]; payment !== undefined && owedCents > 0; payment = remaining[next]) {
      const amountCents = Math.min(owedCents, payment.amountCents);
      allocations.push({ paymentId: payment.paymentId, charge: charge.ref, amountCents });
      owedCents -= amountCents;
      payment.amountCents -= amountCents;
      if (payment.amountCents === 0) {
        next += 1;
      }
    }
    if (next === remaining.length) {
      break;
    }
  }
  return { allocations, unspent: remaining.slice(next) };
}

export function coverageByCharge(allocations: readonly Allocation[]): Map<string, number> {
  const coverage = new Map<string, number>();
  for (const allocation of allocations) {
    const key = chargeKey(allocation.charge);
    coverage.set(key, (coverage.get(key) ?? 0) + allocation.amountCents);
  }
  return coverage;
}

export function coveredCents(coverage: ReadonlyMap<string, number>, ref: ChargeRef): number {
  return coverage.get(chargeKey(ref)) ?? 0;
}
