import { type CalendarDate, type CalendarMonth, compareText } from "./calendarDate";
import type { DebtRow, PaymentRow } from "./ledgerInput";
import type { RentPeriodStatus } from "./rentPeriods";

export type LedgerRowKind = "rent" | "debt" | "payment";

export interface LedgerRow {
  key: string;
  kind: LedgerRowKind;
  entryId?: string;
  month?: CalendarMonth;
  date: CalendarDate;
  amountCents: number;
  position: number;
  balanceOwedCents?: number;
  balanceCreditCents?: number;
  mayNotApply: boolean;
}

export interface LedgerRowsInput {
  today: CalendarDate;
  rentPeriods: readonly RentPeriodStatus[];
  debts: readonly DebtRow[];
  payments: readonly PaymentRow[];
  mayNotApplyDebtIds: readonly string[];
}

type EntryRank = "rent" | "custom" | "lateFee" | "payment";

interface Entry extends Omit<LedgerRow, "position" | "balanceOwedCents" | "balanceCreditCents"> {
  rank: EntryRank;
  creationTime: number;
  balanceEffectCents?: number;
}

const RANK_ORDER: Record<EntryRank, number> = { rent: 0, custom: 1, lateFee: 2, payment: 3 };

export function ledgerRows(input: LedgerRowsInput): LedgerRow[] {
  const rows: LedgerRow[] = [];
  let runningCents = 0;
  for (const [position, entry] of orderedEntries(input).entries()) {
    const { rank: _rank, creationTime: _creationTime, balanceEffectCents, ...row } = entry;
    if (balanceEffectCents === undefined) {
      rows.push({ ...row, position });
      continue;
    }
    runningCents += balanceEffectCents;
    rows.push({ ...row, position, ...balanceFields(runningCents) });
  }
  return rows;
}

function orderedEntries(input: LedgerRowsInput): Entry[] {
  const mayNotApply = new Set(input.mayNotApplyDebtIds);
  return [
    ...input.rentPeriods.filter((period) => period.charged).map(rentEntry),
    ...input.debts.map((debt) => debtEntry(debt, input.today, mayNotApply)),
    ...input.payments.map(paymentEntry),
  ].sort(compareEntries);
}

function rentEntry(period: RentPeriodStatus): Entry {
  return {
    key: `rent:${period.month}`,
    kind: "rent",
    month: period.month,
    date: period.dueDate,
    amountCents: period.amountCents,
    mayNotApply: false,
    rank: "rent",
    creationTime: 0,
    balanceEffectCents: period.amountCents,
  };
}

function debtEntry(debt: DebtRow, today: CalendarDate, mayNotApply: ReadonlySet<string>): Entry {
  const countsAsCharge = debt.deletedAt === undefined && debt.date <= today;
  return {
    key: `debt:${debt.id}`,
    kind: "debt",
    entryId: debt.id,
    date: debt.date,
    amountCents: debt.amountCents,
    mayNotApply: debt.kind === "lateFee" && mayNotApply.has(debt.id),
    rank: debt.kind,
    creationTime: debt.creationTime,
    ...(countsAsCharge && { balanceEffectCents: debt.amountCents }),
  };
}

function paymentEntry(payment: PaymentRow): Entry {
  const countsAsPayment = payment.deletedAt === undefined && payment.bouncedAt === undefined;
  return {
    key: `payment:${payment.id}`,
    kind: "payment",
    entryId: payment.id,
    date: payment.dateReceived,
    amountCents: payment.amountCents,
    mayNotApply: false,
    rank: "payment",
    creationTime: payment.creationTime,
    ...(countsAsPayment && { balanceEffectCents: -payment.amountCents }),
  };
}

function compareEntries(a: Entry, b: Entry): number {
  return (
    compareText(a.date, b.date) ||
    RANK_ORDER[a.rank] - RANK_ORDER[b.rank] ||
    a.creationTime - b.creationTime ||
    compareText(a.key, b.key)
  );
}

function balanceFields(runningCents: number): Pick<LedgerRow, "balanceOwedCents" | "balanceCreditCents"> {
  return runningCents < 0 ? { balanceCreditCents: -runningCents } : { balanceOwedCents: runningCents };
}
