import { allocate, coverageByCharge, coveredCents, type QueuedPayment } from "./allocation";
import { addDays, type CalendarDate } from "./calendarDate";
import { type Charge, rentCharge } from "./charges";
import type { RentPeriod } from "./rentPeriods";

export interface LatenessContext {
  today: CalendarDate;
  graceDays: number;
  orderedCharges: readonly Charge[];
  payments: readonly QueuedPayment[];
}

export function graceEnd(period: RentPeriod, graceDays: number): CalendarDate {
  return addDays(period.dueDate, graceDays);
}

export function isInsideGrace(period: RentPeriod, graceDays: number, today: CalendarDate): boolean {
  return today <= graceEnd(period, graceDays);
}

export function paidByGraceEndCents(period: RentPeriod, context: LatenessContext): number {
  const deadline = graceEnd(period, context.graceDays);
  const charges = context.orderedCharges.filter((charge) => charge.date <= deadline);
  const payments = context.payments.filter((payment) => payment.dateReceived <= deadline);
  const coverage = coverageByCharge(allocate(payments, charges).allocations);
  return coveredCents(coverage, rentCharge(period).ref);
}

export function wasLate(period: RentPeriod, context: LatenessContext): boolean {
  if (isInsideGrace(period, context.graceDays, context.today)) {
    return false;
  }
  return paidByGraceEndCents(period, context) < period.amountCents;
}
