import { validationFailed } from "./appError";
import type { PaymentRow } from "./engine/ledgerInput";

export function requireEditable(payment: Pick<PaymentRow, "deletedAt" | "bouncedAt">): void {
  if (payment.deletedAt !== undefined) {
    throw validationFailed("This payment was cancelled. Restore it to change it.");
  }
  if (payment.bouncedAt !== undefined) {
    throw validationFailed("A bounced payment can't be edited.");
  }
}
