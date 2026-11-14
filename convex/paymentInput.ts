import type { Infer } from "convex/values";
import type { Doc } from "./_generated/dataModel";
import { validationFailed } from "./appError";
import { paymentDoc } from "./documentValidators";
import { optionalNotes, requireAmountCents, requireCalendarDate } from "./inputValidation";
import { localDateAt } from "./localDate";

export const paymentInput = paymentDoc.pick("amountCents", "dateReceived", "method", "note");

export type PaymentInput = Infer<typeof paymentInput>;

const DATE_RECEIVED_FIELD = "Date received";

export function validPaymentInput(input: PaymentInput, group: Doc<"groups">): PaymentInput {
  const dateReceived = requireCalendarDate(input.dateReceived, DATE_RECEIVED_FIELD);
  if (dateReceived > localDateAt(Date.now(), group.timeZone)) {
    throw validationFailed("Date received can't be later than today.", { field: DATE_RECEIVED_FIELD });
  }
  return {
    amountCents: requireAmountCents(input.amountCents, "Amount"),
    dateReceived,
    method: input.method,
    note: optionalNotes(input.note),
  };
}
