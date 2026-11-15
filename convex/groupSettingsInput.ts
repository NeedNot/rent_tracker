import type { Doc } from "./_generated/dataModel";
import { groupDoc } from "./documentValidators";
import { optionalAmountCents, requireGraceDays, requireName, requireTimeZone } from "./inputValidation";

export const groupSettingsFields = groupDoc.pick("name", "timeZone", "paymentOrder", "graceDays", "lateFeeFlatCents");

export function validGroupIdentity(input: { name: string; timeZone: string }) {
  return { name: requireName(input.name, "Group name"), timeZone: requireTimeZone(input.timeZone) };
}

export function validGroupSettings(input: {
  name: string;
  timeZone: string;
  paymentOrder: Doc<"groups">["paymentOrder"];
  graceDays: number;
  lateFeeFlatCents?: number;
}) {
  return {
    ...validGroupIdentity(input),
    paymentOrder: input.paymentOrder,
    graceDays: requireGraceDays(input.graceDays),
    lateFeeFlatCents: optionalAmountCents(input.lateFeeFlatCents, "Late fee"),
  };
}
