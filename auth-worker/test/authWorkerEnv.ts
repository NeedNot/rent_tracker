import type { SignInCodeMessage } from "../../email-worker/src/index";
import type { Env } from "../src/env";
import { migratedSqliteD1 } from "./sqliteD1";

export const AUTH_URL = "https://auth.rent-tracker.test";
export const APP_ORIGIN = "rent-tracker://app";
export const GOOGLE_WEB_CLIENT_ID = "web-client-id.apps.googleusercontent.com";
export const GOOGLE_ANDROID_CLIENT_ID = "android-client-id.apps.googleusercontent.com";

export type TestAuthWorker = {
  readonly env: Env;
  readonly sentSignInCodes: SignInCodeMessage[];
};

export function testAuthWorker(): TestAuthWorker {
  const sentSignInCodes: SignInCodeMessage[] = [];
  const emailWorker = {
    sendSignInCode: async (message: SignInCodeMessage) => {
      sentSignInCodes.push(message);
    },
  };
  return {
    sentSignInCodes,
    env: {
      DB: migratedSqliteD1(),
      EMAIL: emailWorker as unknown as Env["EMAIL"],
      BETTER_AUTH_URL: AUTH_URL,
      BETTER_AUTH_SECRET: "test-secret-that-is-at-least-32-characters-long",
      APP_ORIGIN,
      GOOGLE_CLIENT_IDS: `${GOOGLE_WEB_CLIENT_ID}, ${GOOGLE_ANDROID_CLIENT_ID}`,
    },
  };
}
