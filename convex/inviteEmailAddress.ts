import { validationFailed } from "./appError";
import { MAX_NAME_LENGTH } from "./inputValidation";

const EMAIL_FIELD = "email";
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function requireInviteEmail(email: string): string {
  const normalized = email.trim().toLowerCase();
  if (normalized.length === 0) {
    throw validationFailed("Email is required.", { field: EMAIL_FIELD });
  }
  if (normalized.length > MAX_NAME_LENGTH) {
    throw validationFailed(`Email must be ${MAX_NAME_LENGTH} characters or fewer.`, { field: EMAIL_FIELD });
  }
  if (!EMAIL_SHAPE.test(normalized)) {
    throw validationFailed("Enter a valid email address.", { field: EMAIL_FIELD });
  }
  return normalized;
}
