import type { Id } from "./_generated/dataModel";
import { validationFailed } from "./appError";

export const LATE_FEE_CLIENT_ID_SUFFIX = ":lateFee";

export function requireRetryOfSameTenant(existing: { tenantId: Id<"tenants"> }, tenantId: Id<"tenants">): void {
  if (existing.tenantId !== tenantId) {
    throw validationFailed("This request id was already used for another tenant.", { field: "clientId" });
  }
}
