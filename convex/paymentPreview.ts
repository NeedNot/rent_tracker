import { appError } from "./appError";
import type { PaymentRow } from "./engine/ledgerInput";
import { requireEditable } from "./paymentEditable";
import type { PaymentInput } from "./paymentInput";

const PROPOSED_PAYMENT_ID = "proposed";
const LATEST_CREATION_TIME = Number.MAX_SAFE_INTEGER;

export interface ProposedPayment extends Pick<PaymentInput, "amountCents" | "dateReceived"> {
  paymentId?: string;
}

export function withProposedPayment(payments: readonly PaymentRow[], proposed: ProposedPayment): PaymentRow[] {
  const { paymentId, amountCents, dateReceived } = proposed;
  if (paymentId === undefined) {
    return [...payments, { id: PROPOSED_PAYMENT_ID, amountCents, dateReceived, creationTime: LATEST_CREATION_TIME }];
  }
  const existing = payments.find((payment) => payment.id === paymentId);
  if (existing === undefined) {
    throw appError("NotFound", "This payment no longer exists.");
  }
  requireEditable(existing);
  return payments.map((payment) => (payment.id === paymentId ? { ...payment, amountCents, dateReceived } : payment));
}
