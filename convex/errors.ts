import { ConvexError, type Value } from "convex/values";

export type ErrorCode =
  | "NotSignedIn"
  | "PermissionDenied"
  | "NotFound"
  | "ValidationFailed"
  | "EditConflict"
  | "NeedsConfirmation"
  | "TenantArchived"
  | "TenantMoved";

type ErrorDetails = Record<string, Value>;

export function failure(code: ErrorCode, message: string, details: ErrorDetails = {}) {
  return new ConvexError({ code, message, details });
}
