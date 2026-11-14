import type { Doc } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { type CalendarDate, monthOf, yearOf } from "./engine/calendarDate";
import type { TenantLedgerInput } from "./engine/ledgerInput";
import { computeTenantLedger, type TenantLedger } from "./engine/tenantLedger";
import { loadTenantLedgerRows, type TenantLedgerRows } from "./tenantLedgerRows";

export interface TenantLedgerSource {
  group: Doc<"groups">;
  tenant: Doc<"tenants">;
  rows: TenantLedgerRows;
  today: CalendarDate;
  calendarYear?: number;
}

export function tenantLedgerInput({ group, tenant, rows, today, calendarYear }: TenantLedgerSource): TenantLedgerInput {
  return {
    today,
    calendarYear: calendarYear ?? yearOf(monthOf(today)),
    group: {
      paymentOrder: group.paymentOrder,
      graceDays: group.graceDays,
      lateFeeFlatCents: group.lateFeeFlatCents,
    },
    tenant: {
      moveIn: tenant.moveIn,
      moveOut: tenant.moveOut,
      status: tenant.status,
      graceDaysOverride: tenant.graceDaysOverride,
      lateFeeFlatOverrideCents: tenant.lateFeeFlatOverrideCents,
    },
    rentTerms: rows.rentTerms.map((term) => ({
      id: term._id,
      startMonth: term.startMonth,
      amountCents: term.amountCents,
      dueDay: term.dueDay,
      deletedAt: term.deletedAt,
    })),
    debts: rows.debts.map((debt) => ({
      id: debt._id,
      kind: debt.kind,
      amountCents: debt.amountCents,
      date: debt.date,
      forMonth: debt.forMonth,
      creationTime: debt._creationTime,
      deletedAt: debt.deletedAt,
    })),
    payments: rows.payments.map((payment) => ({
      id: payment._id,
      amountCents: payment.amountCents,
      dateReceived: payment.dateReceived,
      creationTime: payment._creationTime,
      deletedAt: payment.deletedAt,
      bouncedAt: payment.bouncedAt,
    })),
  };
}

export function tenantLedgerOf(source: TenantLedgerSource): TenantLedger {
  return computeTenantLedger(tenantLedgerInput(source));
}

export async function loadTenantLedger(
  ctx: QueryCtx,
  group: Doc<"groups">,
  tenant: Doc<"tenants">,
  today: CalendarDate,
): Promise<TenantLedger> {
  return tenantLedgerOf({ group, tenant, rows: await loadTenantLedgerRows(ctx, tenant._id), today });
}
