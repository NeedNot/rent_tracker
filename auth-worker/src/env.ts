import type EmailWorker from "../../email-worker/src/index";

export type Env = {
  readonly DB: D1Database;
  readonly EMAIL: Service<EmailWorker>;
  readonly BETTER_AUTH_URL: string;
  readonly BETTER_AUTH_SECRET: string;
  readonly APP_ORIGIN: string;
  readonly GOOGLE_CLIENT_IDS: string;
};
