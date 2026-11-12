import type { CalendarDate, CalendarMonth } from "./calendarDate";
import type { DebtRow, GroupSettings, PaymentRow, RentTermRow, TenantLedgerInput, TenantSettings } from "./ledgerInput";

export const RENT = 150000;

export const defaultGroup: GroupSettings = { paymentOrder: "rentFirst", graceDays: 5 };

export const defaultTenant: TenantSettings = { moveIn: "2026-01-01", status: "active" };

interface LedgerOverrides {
  today: CalendarDate;
  calendarYear?: number;
  group?: Partial<GroupSettings>;
  tenant?: Partial<TenantSettings>;
  rentTerms?: RentTermRow[];
  debts?: DebtRow[];
  payments?: PaymentRow[];
}

export function ledgerInput(overrides: LedgerOverrides): TenantLedgerInput {
  return {
    today: overrides.today,
    calendarYear: overrides.calendarYear ?? Number(overrides.today.slice(0, 4)),
    group: { ...defaultGroup, ...overrides.group },
    tenant: { ...defaultTenant, ...overrides.tenant },
    rentTerms: overrides.rentTerms ?? [term("2026-01", RENT)],
    debts: overrides.debts ?? [],
    payments: overrides.payments ?? [],
  };
}

export function term(
  startMonth: CalendarMonth,
  amountCents: number,
  dueDay = 1,
  extra: Partial<RentTermRow> = {},
): RentTermRow {
  return { id: `term-${startMonth}`, startMonth, amountCents, dueDay, ...extra };
}

export function payment(dateReceived: CalendarDate, amountCents: number, extra: Partial<PaymentRow> = {}): PaymentRow {
  return { id: `pay-${dateReceived}`, amountCents, dateReceived, creationTime: 0, ...extra };
}

export function lateFee(
  date: CalendarDate,
  amountCents: number,
  forMonth: CalendarMonth,
  extra: Partial<DebtRow> = {},
): DebtRow {
  return { id: `fee-${forMonth}`, kind: "lateFee", amountCents, date, forMonth, creationTime: 0, ...extra };
}

export function customDebt(date: CalendarDate, amountCents: number, extra: Partial<DebtRow> = {}): DebtRow {
  return { id: `debt-${date}`, kind: "custom", amountCents, date, creationTime: 0, ...extra };
}
