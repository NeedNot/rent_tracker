import type { Doc } from "./_generated/dataModel";
import { validationFailed } from "./appError";
import type { CalendarDate, CalendarMonth } from "./engine/calendarDate";
import {
  optionalNotes,
  requireAmountCents,
  requireCalendarDate,
  requireCalendarMonth,
  requireName,
} from "./inputValidation";

export interface LateFeeInput {
  amountCents: number;
  date: CalendarDate;
  forMonth: CalendarMonth;
  note?: string;
}

export interface CustomDebtInput {
  amountCents: number;
  date: CalendarDate;
  note: string;
}

export interface DebtEditInput {
  amountCents: number;
  date: string;
  forMonth?: string;
  note?: string;
}

export function validLateFeeInput(input: {
  amountCents: number;
  date: string;
  forMonth: string;
  note?: string;
}): LateFeeInput {
  return {
    amountCents: requireAmountCents(input.amountCents, "Amount"),
    date: requireCalendarDate(input.date, "Date"),
    forMonth: requireCalendarMonth(input.forMonth, "For month"),
    note: optionalNotes(input.note),
  };
}

export function validCustomDebtInput(input: { amountCents: number; date: string; note: string }): CustomDebtInput {
  return {
    amountCents: requireAmountCents(input.amountCents, "Amount"),
    date: requireCalendarDate(input.date, "Date"),
    note: requireName(input.note, "Note"),
  };
}

export function validDebtEdit(kind: Doc<"debts">["kind"], input: DebtEditInput): LateFeeInput | CustomDebtInput {
  if (kind === "lateFee") {
    if (input.forMonth === undefined) {
      throw validationFailed("For month is required.", { field: "For month" });
    }
    return validLateFeeInput({ ...input, forMonth: input.forMonth });
  }
  if (input.forMonth !== undefined) {
    throw validationFailed("A custom debt has no month.", { field: "For month" });
  }
  return validCustomDebtInput({ ...input, note: input.note ?? "" });
}
