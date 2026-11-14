import { ConvexError, type Value } from "convex/values";

export type AppErrorCode =
  | "NotSignedIn"
  | "PermissionDenied"
  | "NotFound"
  | "ValidationFailed"
  | "EditConflict"
  | "NeedsConfirmation"
  | "TenantArchived"
  | "TenantMoved";

export type AppErrorDetails = Record<string, Value>;

export type AppErrorData = {
  code: AppErrorCode;
  message: string;
  details: AppErrorDetails;
};

export function appError(
  code: AppErrorCode,
  message: string,
  details: AppErrorDetails = {},
): ConvexError<AppErrorData> {
  return new ConvexError<AppErrorData>({ code, message, details });
}

export function validationFailed(message: string, details: AppErrorDetails = {}): ConvexError<AppErrorData> {
  return appError("ValidationFailed", message, details);
}
