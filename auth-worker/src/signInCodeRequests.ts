import { SIGN_IN_CODE_COOLDOWN_SECONDS, SIGN_IN_CODES_PER_HOUR } from "./signInCodeLimits";

const MILLISECONDS_PER_SECOND = 1000;
const COOLDOWN_MILLISECONDS = SIGN_IN_CODE_COOLDOWN_SECONDS * MILLISECONDS_PER_SECOND;
const HOUR_MILLISECONDS = 60 * 60 * MILLISECONDS_PER_SECOND;

export type SignInCodeRequestDecision =
  | { readonly allowed: true }
  | { readonly allowed: false; readonly retryAfterSeconds: number };

type RecentRequests = {
  readonly requestCount: number;
  readonly oldestRequestedAt: number;
  readonly newestRequestedAt: number;
};

export async function recordSignInCodeRequest(
  db: D1Database,
  email: string,
  now: number,
): Promise<SignInCodeRequestDecision> {
  const hourAgo = now - HOUR_MILLISECONDS;
  const [, recorded] = await db.batch([
    db.prepare("DELETE FROM signInCodeRequest WHERE requestedAt <= ?1").bind(hourAgo),
    db
      .prepare(
        `INSERT INTO signInCodeRequest (email, requestedAt)
         SELECT ?1, ?2
         WHERE NOT EXISTS (SELECT 1 FROM signInCodeRequest WHERE email = ?1 AND requestedAt > ?3)
           AND (SELECT COUNT(*) FROM signInCodeRequest WHERE email = ?1 AND requestedAt > ?4) < ?5`,
      )
      .bind(email, now, now - COOLDOWN_MILLISECONDS, hourAgo, SIGN_IN_CODES_PER_HOUR),
  ]);
  if (recorded?.meta.changes === 1) {
    return { allowed: true };
  }
  return { allowed: false, retryAfterSeconds: await secondsUntilNextRequest(db, email, now) };
}

async function secondsUntilNextRequest(db: D1Database, email: string, now: number): Promise<number> {
  const recent = await db
    .prepare(
      `SELECT COUNT(*) AS requestCount, MIN(requestedAt) AS oldestRequestedAt, MAX(requestedAt) AS newestRequestedAt
       FROM signInCodeRequest WHERE email = ?1 AND requestedAt > ?2`,
    )
    .bind(email, now - HOUR_MILLISECONDS)
    .first<RecentRequests>();
  if (recent === null || recent.requestCount === 0) {
    return 1;
  }
  const cooldownEndsAt = recent.newestRequestedAt + COOLDOWN_MILLISECONDS;
  const hourlyLimitEndsAt =
    recent.requestCount >= SIGN_IN_CODES_PER_HOUR ? recent.oldestRequestedAt + HOUR_MILLISECONDS : 0;
  const allowedAgainAt = Math.max(cooldownEndsAt, hourlyLimitEndsAt);
  return Math.max(1, Math.ceil((allowedAgainAt - now) / MILLISECONDS_PER_SECOND));
}
