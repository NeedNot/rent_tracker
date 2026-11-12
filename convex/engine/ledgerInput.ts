import type { CalendarDate, CalendarMonth } from "./calendarDate";

export type PaymentOrder = "rentFirst" | "byDate";

export type TenantStatus = "active" | "archived";

export type DebtKind = "lateFee" | "custom";

export interface GroupSettings {
  paymentOrder: PaymentOrder;
  graceDays: number;
  lateFeeFlatCents?: number;
}

export interface TenantSettings {
  moveIn: CalendarDate;
  moveOut?: CalendarDate;
  status: TenantStatus;
  graceDaysOverride?: number;
  lateFeeFlatOverrideCents?: number;
}

export interface RentTermRow {
  id: string;
  startMonth: CalendarMonth;
  amountCents: number;
  dueDay: number;
  deletedAt?: number;
}

export interface DebtRow {
  id: string;
  kind: DebtKind;
  amountCents: number;
  date: CalendarDate;
  forMonth?: CalendarMonth;
  creationTime: number;
  deletedAt?: number;
}

export interface PaymentRow {
  id: string;
  amountCents: number;
  dateReceived: CalendarDate;
  creationTime: number;
  deletedAt?: number;
  bouncedAt?: number;
}

export interface TenantLedgerInput {
  today: CalendarDate;
  calendarYear: number;
  group: GroupSettings;
  tenant: TenantSettings;
  rentTerms: readonly RentTermRow[];
  debts: readonly DebtRow[];
  payments: readonly PaymentRow[];
}
