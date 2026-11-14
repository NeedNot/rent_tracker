import { createLocalJWKSet, decodeProtectedHeader, type JSONWebKeySet, jwtVerify } from "jose";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { errorCode, get, post, signedInCookie } from "../test/appClient";
import { AUTH_URL, type TestAuthWorker, testAuthWorker } from "../test/authWorkerEnv";

const EMAIL = "dalton.h@gmail.com";
const DAY = 24 * 60 * 60 * 1000;
const YEAR = 365 * DAY;
const NOW = new Date("2026-11-12T12:00:00Z");

type SessionResponse = {
  readonly session: { readonly expiresAt: string };
  readonly user: { readonly id: string; readonly email: string };
};

async function jwtFor(auth: TestAuthWorker, cookie: string): Promise<string> {
  const response = await get(auth, "/api/auth/token", { cookie });
  return ((await response.json()) as { token: string }).token;
}

async function currentSession(auth: TestAuthWorker, cookie: string): Promise<SessionResponse> {
  return (await (await get(auth, "/api/auth/get-session", { cookie })).json()) as SessionResponse;
}

describe("session", () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["Date"], now: NOW });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("issues an ES256 JWT with sub, email, issuer, audience and a 15-minute lifetime, verifiable with the JWKS", async () => {
    const auth = testAuthWorker();
    const cookie = await signedInCookie(auth, EMAIL);
    const { user } = await currentSession(auth, cookie);

    const token = await jwtFor(auth, cookie);
    const jwks = (await (await get(auth, "/api/auth/jwks")).json()) as JSONWebKeySet;
    const { payload } = await jwtVerify(token, createLocalJWKSet(jwks), { currentDate: NOW });

    expect(decodeProtectedHeader(token)).toEqual({ alg: "ES256", kid: jwks.keys[0]?.kid });
    expect(payload).toEqual({
      sub: user.id,
      email: EMAIL,
      iss: AUTH_URL,
      aud: "rent-tracker",
      iat: NOW.getTime() / 1000,
      exp: NOW.getTime() / 1000 + 15 * 60,
    });
  });

  it("publishes only the public half of one ES256 key", async () => {
    const auth = testAuthWorker();
    await jwtFor(auth, await signedInCookie(auth, EMAIL));

    const jwks = (await (await get(auth, "/api/auth/jwks")).json()) as JSONWebKeySet;

    expect(jwks.keys).toEqual([
      { kty: "EC", crv: "P-256", alg: "ES256", kid: expect.any(String), x: expect.any(String), y: expect.any(String) },
    ]);
  });

  it("refuses a JWT without a session cookie", async () => {
    const response = await get(testAuthWorker(), "/api/auth/token");

    expect(response.status).toBe(401);
  });

  it("lasts one year and is extended to a full year again whenever it is used", async () => {
    const auth = testAuthWorker();
    const cookie = await signedInCookie(auth, EMAIL);
    const atSignIn = await currentSession(auth, cookie);
    vi.advanceTimersByTime(300 * DAY);

    await jwtFor(auth, cookie);
    const afterUse = await currentSession(auth, cookie);
    vi.advanceTimersByTime(300 * DAY);
    const beyondTheOriginalYear = await get(auth, "/api/auth/token", { cookie });

    expect(new Date(atSignIn.session.expiresAt).getTime()).toBe(NOW.getTime() + YEAR);
    expect(new Date(afterUse.session.expiresAt).getTime()).toBe(NOW.getTime() + 300 * DAY + YEAR);
    expect(beyondTheOriginalYear.status).toBe(200);
  });

  it("expires after a year without use", async () => {
    const auth = testAuthWorker();
    const cookie = await signedInCookie(auth, EMAIL);
    vi.advanceTimersByTime(YEAR + 1000);

    const response = await get(auth, "/api/auth/token", { cookie });

    expect(response.status).toBe(401);
  });

  it("ends on sign-out, after which the cookie no longer yields a JWT", async () => {
    const auth = testAuthWorker();
    const cookie = await signedInCookie(auth, EMAIL);

    const signOut = await post(auth, "/api/auth/sign-out", {}, { cookie });
    const tokenAfterSignOut = await get(auth, "/api/auth/token", { cookie });

    expect(signOut.status).toBe(200);
    expect(await signOut.json()).toEqual({ success: true });
    expect(tokenAfterSignOut.status).toBe(401);
  });

  it("rejects a cookie-carrying request from an untrusted origin and keeps the session", async () => {
    const auth = testAuthWorker();
    const cookie = await signedInCookie(auth, EMAIL);

    const signOut = await post(auth, "/api/auth/sign-out", {}, { cookie, origin: "https://evil.example" });
    const tokenAfterwards = await get(auth, "/api/auth/token", { cookie });

    expect(signOut.status).toBe(403);
    expect(await errorCode(signOut)).toBe("INVALID_ORIGIN");
    expect(tokenAfterwards.status).toBe(200);
  });
});
