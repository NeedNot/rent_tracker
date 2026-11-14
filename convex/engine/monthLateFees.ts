import { coveredCents } from "./allocation";
import type { CalendarDate, CalendarMonth } from "./calendarDate";
import { debtCharge } from "./charges";
import type { DebtRow } from "./ledgerInput";

export interface MonthLateFees {
  cents: number;
  unpaidCents: number;
}

export function lateFeesByMonth(
  activeDebts: readonly DebtRow[],
  today: CalendarDate,
  coverage: ReadonlyMap<string, number>,
): Map<CalendarMonth, MonthLateFees> {
  const byMonth = new Map<CalendarMonth, MonthLateFees>();
  for (const debt of activeDebts) {
    if (debt.kind !== "lateFee" || debt.forMonth === undefined || debt.date > today) {
      continue;
    }
    const unpaidCents = debt.amountCents - Math.min(debt.amountCents, coveredCents(coverage, debtCharge(debt).ref));
    const total = byMonth.get(debt.forMonth) ?? { cents: 0, unpaidCents: 0 };
    byMonth.set(debt.forMonth, { cents: total.cents + debt.amountCents, unpaidCents: total.unpaidCents + unpaidCents });
  }
  return byMonth;
}
