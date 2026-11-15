import worker from "../src/index";
import { AUTH_URL, SHARED_SECRET, type TestAuthWorker } from "./authWorkerEnv";

export type InternalRequest = {
  readonly secret?: string | null;
  readonly method?: string;
};

export function callInternal(
  auth: TestAuthWorker,
  action: string,
  body: unknown,
  { secret = SHARED_SECRET, method = "POST" }: InternalRequest = {},
): Promise<Response> {
  const headers = new Headers({ "content-type": "application/json" });
  if (secret !== null) {
    headers.set("X-Rent-Tracker-Secret", secret);
  }
  const payload = typeof body === "string" ? body : JSON.stringify(body);
  return worker.fetch(
    new Request(`${AUTH_URL}/internal/users/${action}`, {
      method,
      headers,
      body: method === "GET" ? undefined : payload,
    }),
    auth.env,
  );
}

export async function rowCount(auth: TestAuthWorker, table: "user" | "session" | "account", userId: string) {
  const column = table === "user" ? "id" : "userId";
  const row = (await auth.env.DB.prepare(`select count(*) as count from "${table}" where "${column}" = ?`)
    .bind(userId)
    .first()) as { count: number };
  return row.count;
}

export async function linkGoogleAccount(auth: TestAuthWorker, userId: string): Promise<void> {
  const now = Date.now();
  await auth.env.DB.prepare(
    `insert into "account" ("id", "accountId", "providerId", "userId", "createdAt", "updatedAt") values (?, ?, 'google', ?, ?, ?)`,
  )
    .bind(`google-account-of-${userId}`, `google-subject-of-${userId}`, userId, now, now)
    .run();
}
