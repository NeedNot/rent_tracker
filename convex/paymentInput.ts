import type { Infer } from "convex/values";
import { validationFailed } from "./appError";
import { paymentDoc } from "./documentValidators";
import type { CalendarDate } from "./engine/calendarDate";
import { optionalNotes, requireAmountCents, requireCalendarDate } from "./inputValidation";

export const paymentInput = paymentDoc.pick("amountCents", "dateReceived", "method", "note");

export type PaymentInput = Infer<typeof paymentInput>;

const DATE_RECEIVED_FIELD = "Date received";

export function validPaymentInput(input: PaymentInput, today: CalendarDate): PaymentInput {
  const dateReceived = requireCalendarDate(input.dateReceived, DATE_RECEIVED_FIELD);
  if (dateReceived > today) {
    throw validationFailed("Date received can't be later than today.", { field: DATE_RECEIVED_FIELD });
  }
  return {
    amountCents: requireAmountCents(input.amountCents, "Amount"),
    dateReceived,
    method: input.method,
    note: optionalNotes(input.note),
  };
}
