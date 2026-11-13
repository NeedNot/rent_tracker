import type { Infer } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { validationFailed } from "./appError";
import type { PermissionLevel } from "./authPlaceholder";
import { rentTermDoc } from "./documentValidators";
import { type CalendarMonth, compareText, monthOf } from "./engine/calendarDate";
import { requireAmountCents, requireCalendarMonth, requireDueDay } from "./inputValidation";

export const rentTermInput = rentTermDoc.pick("startMonth", "amountCents", "dueDay");

export type RentTermInput = Infer<typeof rentTermInput>;

const START_MONTH_FIELD = "Start month";

export function validRentTermInput(input: RentTermInput): RentTermInput {
  return {
    startMonth: requireCalendarMonth(input.startMonth, START_MONTH_FIELD),
    amountCents: requireAmountCents(input.amountCents, "Rent"),
    dueDay: requireDueDay(input.dueDay),
  };
}

export function activeTerms(terms: readonly Doc<"rentTerms">[]): Doc<"rentTerms">[] {
  return terms.filter((term) => term.deletedAt === undefined);
}

export function firstTermOf(terms: readonly Doc<"rentTerms">[]): Doc<"rentTerms"> | undefined {
  return activeTerms(terms).sort((a, b) => compareText(a.startMonth, b.startMonth))[0];
}

export function isFirstTerm(term: Doc<"rentTerms">, terms: readonly Doc<"rentTerms">[]): boolean {
  return firstTermOf(terms)?._id === term._id;
}

export function requireFreeStartMonth(
  tenant: Doc<"tenants">,
  otherTerms: readonly Doc<"rentTerms">[],
  startMonth: CalendarMonth,
): void {
  if (startMonth < monthOf(tenant.moveIn)) {
    throw validationFailed("A rent change can't start before the move-in month. Edit the existing rent term instead.", {
      field: START_MONTH_FIELD,
    });
  }
  const sameMonth = activeTerms(otherTerms).find((term) => term.startMonth === startMonth);
  if (sameMonth !== undefined) {
    throw validationFailed("A rent term already starts in this month. Edit that rent term instead.", {
      field: START_MONTH_FIELD,
      rentTermId: sameMonth._id,
    });
  }
}

export function withoutTerm(terms: readonly Doc<"rentTerms">[], termId: Id<"rentTerms">): Doc<"rentTerms">[] {
  return terms.filter((term) => term._id !== termId);
}

export function rentTermLevel(startMonths: readonly CalendarMonth[], currentMonth: CalendarMonth): PermissionLevel {
  return startMonths.some((startMonth) => startMonth < currentMonth) ? "full" : "addEdit";
}
