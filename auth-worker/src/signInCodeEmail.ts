import type { EmailOTPOptions } from "better-auth/plugins/email-otp";
import type { Env } from "./env";

export function sendSignInCodeEmail(emailWorker: Env["EMAIL"]): EmailOTPOptions["sendVerificationOTP"] {
  return async ({ email, otp }) => {
    await emailWorker.sendSignInCode({ email, code: otp });
  };
}
