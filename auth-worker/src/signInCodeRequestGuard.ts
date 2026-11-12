import { APIError, createAuthMiddleware } from "better-auth/api";
import { recordSignInCodeRequest } from "./signInCodeRequests";

const SEND_CODE_PATH = "/email-otp/send-verification-otp";

export function signInCodeRequestGuard(db: D1Database) {
  return createAuthMiddleware(async (ctx) => {
    if (ctx.path !== SEND_CODE_PATH) {
      return;
    }
    const { email, type } = ctx.body ?? {};
    if (typeof email !== "string" || type !== "sign-in") {
      throw new APIError("BAD_REQUEST", {
        code: "SIGN_IN_CODE_ONLY",
        message: "Only sign-in codes can be requested",
      });
    }
    const decision = await recordSignInCodeRequest(db, email.toLowerCase(), Date.now());
    if (!decision.allowed) {
      throw new APIError(
        "TOO_MANY_REQUESTS",
        { code: "TOO_MANY_SIGN_IN_CODES", message: "Too many sign-in codes requested. Try again later." },
        { "X-Retry-After": String(decision.retryAfterSeconds) },
      );
    }
  });
}
