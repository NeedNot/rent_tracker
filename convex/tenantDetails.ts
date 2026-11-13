import type { Infer } from "convex/values";
import { validationFailed } from "./appError";
import { tenantDoc } from "./documentValidators";
import {
  optionalAmountCents,
  optionalCalendarDate,
  optionalGraceDays,
  optionalLabel,
  optionalNonNegativeCents,
  optionalNotes,
  requireCalendarDate,
  requireName,
} from "./inputValidation";

export const tenantDetails = tenantDoc.pick(
  "name",
  "contactName",
  "contactPhone",
  "contactEmail",
  "notes",
  "depositCents",
  "moveIn",
  "moveOut",
  "graceDaysOverride",
  "lateFeeFlatOverrideCents",
);

export type TenantDetails = Infer<typeof tenantDetails>;

const MOVE_OUT_FIELD = "Last day in unit";

export function validTenantDetails(input: TenantDetails): TenantDetails {
  const moveIn = requireCalendarDate(input.moveIn, "Move-in date");
  const moveOut = optionalCalendarDate(input.moveOut, MOVE_OUT_FIELD);
  if (moveOut !== undefined && moveOut < moveIn) {
    throw validationFailed(`${MOVE_OUT_FIELD} must be on or after the move-in date.`, { field: MOVE_OUT_FIELD });
  }
  return {
    name: requireName(input.name, "Name"),
    contactName: optionalLabel(input.contactName, "Contact name"),
    contactPhone: optionalLabel(input.contactPhone, "Phone"),
    contactEmail: optionalLabel(input.contactEmail, "Email"),
    notes: optionalNotes(input.notes),
    depositCents: optionalNonNegativeCents(input.depositCents, "Security deposit"),
    moveIn,
    moveOut,
    graceDaysOverride: optionalGraceDays(input.graceDaysOverride),
    lateFeeFlatOverrideCents: optionalAmountCents(input.lateFeeFlatOverrideCents, "Late fee amount"),
  };
}
