import { type CalendarDate, type CalendarMonth, compareText } from "./calendarDate";
import type { DebtRow, PaymentOrder } from "./ledgerInput";
import type { RentPeriod } from "./rentPeriods";

export type ChargeRef = { kind: "rent"; month: CalendarMonth } | { kind: "debt"; debtId: string };

export type ChargeCategory = "rent" | "custom" | "lateFee";

export interface Charge {
  ref: ChargeRef;
  category: ChargeCategory;
  date: CalendarDate;
  amountCents: number;
  creationTime: number;
}

const CATEGORY_RANK: Record<ChargeCategory, number> = { rent: 0, custom: 1, lateFee: 2 };

export function rentCharge(period: RentPeriod): Charge {
  return {
    ref: { kind: "rent", month: period.month },
    category: "rent",
    date: period.dueDate,
    amountCents: period.amountCents,
    creationTime: 0,
  };
}

export function debtCharge(debt: DebtRow): Charge {
  return {
    ref: { kind: "debt", debtId: debt.id },
    category: debt.kind,
    date: debt.date,
    amountCents: debt.amountCents,
    creationTime: debt.creationTime,
  };
}

export function chargeKey(ref: ChargeRef): string {
  return ref.kind === "rent" ? `rent:${ref.month}` : `debt:${ref.debtId}`;
}

export function orderCharges(charges: readonly Charge[], paymentOrder: PaymentOrder): Charge[] {
  const compare = paymentOrder === "rentFirst" ? compareRentFirst : compareByDate;
  return [...charges].sort(compare);
}

function compareRentFirst(a: Charge, b: Charge): number {
  return compareCategory(a, b) || compareText(a.date, b.date) || a.creationTime - b.creationTime;
}

function compareByDate(a: Charge, b: Charge): number {
  return compareText(a.date, b.date) || compareCategory(a, b) || a.creationTime - b.creationTime;
}

function compareCategory(a: Charge, b: Charge): number {
  return CATEGORY_RANK[a.category] - CATEGORY_RANK[b.category];
}
