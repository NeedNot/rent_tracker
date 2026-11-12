import {
  addDays,
  addMonths,
  type CalendarDate,
  type CalendarMonth,
  compareText,
  dateInMonth,
  monthOf,
} from "./calendarDate";
import type { RentTermRow, TenantSettings } from "./ledgerInput";

export interface RentPeriod {
  month: CalendarMonth;
  dueDate: CalendarDate;
  lastDay: CalendarDate;
  amountCents: number;
}

export interface RentPeriodStatus extends RentPeriod {
  charged: boolean;
  coveredCents: number;
  graceEnd: CalendarDate;
  wasLate: boolean;
  currentlyLate: boolean;
}

export interface DateRange {
  start: CalendarDate;
  end: CalendarDate;
}

export function* rentPeriods(tenant: TenantSettings, rentTerms: readonly RentTermRow[]): Generator<RentPeriod> {
  for (const period of periodsStartedBeforeMoveOut(tenant, rentTerms)) {
    if (staysWholePeriod(tenant, period)) {
      yield period;
    }
  }
}

export function partialMoveOutPeriod(tenant: TenantSettings, rentTerms: readonly RentTermRow[]): DateRange | undefined {
  if (tenant.moveOut === undefined) {
    return undefined;
  }
  for (const period of periodsStartedBeforeMoveOut(tenant, rentTerms)) {
    if (!staysWholePeriod(tenant, period)) {
      return { start: period.dueDate, end: period.lastDay };
    }
  }
  return undefined;
}

function* periodsStartedBeforeMoveOut(
  tenant: TenantSettings,
  rentTerms: readonly RentTermRow[],
): Generator<RentPeriod> {
  const terms = activeTermsNewestFirst(rentTerms);
  if (terms.length === 0) {
    return;
  }
  for (let month = monthOf(tenant.moveIn); ; month = addMonths(month, 1)) {
    const term = termFor(terms, month);
    if (term === undefined) {
      continue;
    }
    const dueDate = dateInMonth(month, term.dueDay);
    if (tenant.moveOut !== undefined && dueDate > tenant.moveOut) {
      return;
    }
    if (dueDate >= tenant.moveIn) {
      yield { month, dueDate, lastDay: addDays(nextDueDate(terms, month, term), -1), amountCents: term.amountCents };
    }
  }
}

function staysWholePeriod(tenant: TenantSettings, period: RentPeriod): boolean {
  return tenant.moveOut === undefined || tenant.moveOut >= period.lastDay;
}

function nextDueDate(terms: readonly RentTermRow[], month: CalendarMonth, currentTerm: RentTermRow): CalendarDate {
  const nextMonth = addMonths(month, 1);
  return dateInMonth(nextMonth, (termFor(terms, nextMonth) ?? currentTerm).dueDay);
}

function termFor(newestFirst: readonly RentTermRow[], month: CalendarMonth): RentTermRow | undefined {
  return newestFirst.find((term) => term.startMonth <= month);
}

function activeTermsNewestFirst(rentTerms: readonly RentTermRow[]): RentTermRow[] {
  return rentTerms
    .filter((term) => term.deletedAt === undefined)
    .sort((a, b) => compareText(b.startMonth, a.startMonth));
}
