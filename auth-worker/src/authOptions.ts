import type { BetterAuthOptions } from "better-auth";
import { emailOTP } from "better-auth/plugins/email-otp";
import { jwt } from "better-auth/plugins/jwt";
import type { Env } from "./env";
import { sendSignInCodeEmail } from "./signInCodeEmail";
import { SIGN_IN_CODE_ALLOWED_GUESSES, SIGN_IN_CODE_LENGTH, SIGN_IN_CODE_LIFETIME_SECONDS } from "./signInCodeLimits";
import { signInCodeRequestGuard } from "./signInCodeRequestGuard";

export const AUTH_BASE_PATH = "/api/auth";
export const JWT_AUDIENCE = "rent-tracker";
const JWT_LIFETIME = "15 minutes";
const SESSION_LIFETIME_SECONDS = 365 * 24 * 60 * 60;
const EXTEND_SESSION_ON_EVERY_USE = 0;

const UNUSED_EMAIL_OTP_PATHS = [
  "/email-otp/check-verification-otp",
  "/email-otp/verify-email",
  "/email-otp/request-password-reset",
  "/email-otp/reset-password",
  "/forget-password/email-otp",
  "/email-otp/request-email-change",
  "/email-otp/change-email",
];

export function authOptions(env: Env) {
  return {
    baseURL: env.BETTER_AUTH_URL,
    basePath: AUTH_BASE_PATH,
    secret: env.BETTER_AUTH_SECRET,
    database: env.DB,
    trustedOrigins: [env.APP_ORIGIN],
    emailAndPassword: { enabled: false },
    socialProviders: {
      google: { clientId: googleClientIds(env.GOOGLE_CLIENT_IDS) },
    },
    account: {
      accountLinking: { enabled: true },
    },
    session: {
      expiresIn: SESSION_LIFETIME_SECONDS,
      updateAge: EXTEND_SESSION_ON_EVERY_USE,
    },
    rateLimit: {
      enabled: true,
      storage: "database",
    },
    advanced: {
      // Better Auth skips origin checks when NODE_ENV is "test" unless this is set explicitly.
      disableOriginCheck: false,
      useSecureCookies: env.BETTER_AUTH_URL.startsWith("https://"),
      ipAddress: { ipAddressHeaders: ["cf-connecting-ip"] },
    },
    disabledPaths: UNUSED_EMAIL_OTP_PATHS,
    hooks: {
      before: signInCodeRequestGuard(env.DB),
    },
    plugins: [
      emailOTP({
        otpLength: SIGN_IN_CODE_LENGTH,
        expiresIn: SIGN_IN_CODE_LIFETIME_SECONDS,
        allowedAttempts: SIGN_IN_CODE_ALLOWED_GUESSES,
        storeOTP: "hashed",
        sendVerificationOTP: sendSignInCodeEmail(env.EMAIL),
      }),
      jwt({
        jwks: { keyPairConfig: { alg: "ES256" } },
        jwt: {
          issuer: env.BETTER_AUTH_URL,
          audience: JWT_AUDIENCE,
          expirationTime: JWT_LIFETIME,
          definePayload: ({ user }) => ({ email: user.email }),
        },
      }),
    ],
  } satisfies BetterAuthOptions;
}

function googleClientIds(commaSeparated: string): string[] {
  return commaSeparated
    .split(",")
    .map((clientId) => clientId.trim())
    .filter((clientId) => clientId !== "");
}
