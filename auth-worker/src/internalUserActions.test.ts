import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { get, signedInCookie } from "../test/appClient";
import { type TestAuthWorker, testAuthWorker } from "../test/authWorkerEnv";
import { callInternal, linkGoogleAccount, rowCount } from "../test/internalClient";
import { SIGN_IN_CODE_COOLDOWN_SECONDS } from "./signInCodeLimits";

const EMAIL = "dalton.h@gmail.com";
const OTHER_EMAIL = "sam.k@gmail.com";

type SessionResponse = {
  readonly user: { readonly id: string; readonly email: string; readonly emailVerified: boolean };
};

async function sessionBehind(auth: TestAuthWorker, cookie: string): Promise<SessionResponse | null> {
  return (await (await get(auth, "/api/auth/get-session", { cookie })).json()) as SessionResponse | null;
}

async function signedInTwice(auth: TestAuthWorker, email: string) {
  const firstCookie = await signedInCookie(auth, email);
  vi.advanceTimersByTime(SIGN_IN_CODE_COOLDOWN_SECONDS * 1000);
  const secondCookie = await signedInCookie(auth, email);
  const cookies = [firstCookie, secondCookie];
  if ((await jwtStatuses(auth, cookies)).some((status) => status !== 200)) {
    throw new Error(`${email} did not sign in twice`);
  }
  const userId = (await sessionBehind(auth, firstCookie))?.user.id ?? "";
  return { userId, cookies };
}

async function jwtStatuses(auth: TestAuthWorker, cookies: string[]): Promise<number[]> {
  return Promise.all(cookies.map(async (cookie) => (await get(auth, "/api/auth/token", { cookie })).status));
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: new Date("2026-11-14T12:00:00Z") });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("POST /internal/users/delete", () => {
  it("deletes the user with every session and linked account, so no cookie yields a JWT", async () => {
    const auth = testAuthWorker();
    const { userId, cookies } = await signedInTwice(auth, EMAIL);
    await linkGoogleAccount(auth, userId);

    const response = await callInternal(auth, "delete", { userId });

    expect(response.status).toBe(204);
    expect(await rowCount(auth, "user", userId)).toBe(0);
    expect(await rowCount(auth, "session", userId)).toBe(0);
    expect(await rowCount(auth, "account", userId)).toBe(0);
    expect(await jwtStatuses(auth, cookies)).toEqual([401, 401]);
  });

  it("answers 204 again when the user is already deleted, so a retry is safe", async () => {
    const auth = testAuthWorker();
    const { userId } = await signedInTwice(auth, EMAIL);
    await callInternal(auth, "delete", { userId });

    const retry = await callInternal(auth, "delete", { userId });

    expect(retry.status).toBe(204);
  });

  it("leaves other users signed in", async () => {
    const auth = testAuthWorker();
    const { userId } = await signedInTwice(auth, EMAIL);
    const otherCookie = await signedInCookie(auth, OTHER_EMAIL);

    await callInternal(auth, "delete", { userId });

    expect(await jwtStatuses(auth, [otherCookie])).toEqual([200]);
  });
});

describe("POST /internal/users/revoke-sessions", () => {
  it("ends every session of the user but keeps the user", async () => {
    const auth = testAuthWorker();
    const { userId, cookies } = await signedInTwice(auth, EMAIL);

    const response = await callInternal(auth, "revoke-sessions", { userId });

    expect(response.status).toBe(204);
    expect(await rowCount(auth, "session", userId)).toBe(0);
    expect(await rowCount(auth, "user", userId)).toBe(1);
    expect(await jwtStatuses(auth, cookies)).toEqual([401, 401]);
  });

  it("answers 204 for an unknown user", async () => {
    const response = await callInternal(testAuthWorker(), "revoke-sessions", { userId: "unknown-user" });

    expect(response.status).toBe(204);
  });
});

describe("POST /internal/users/change-email", () => {
  it("stores the trimmed, lowercased email as verified and keeps the user signed in", async () => {
    const auth = testAuthWorker();
    const { userId, cookies } = await signedInTwice(auth, EMAIL);

    const response = await callInternal(auth, "change-email", { userId, email: "  Dalton.New@Gmail.com " });

    expect(response.status).toBe(204);
    expect((await sessionBehind(auth, cookies[0] ?? ""))?.user).toMatchObject({
      id: userId,
      email: "dalton.new@gmail.com",
      emailVerified: true,
    });
  });

  it("lets the member sign in with a code sent to the new email and land on the same user", async () => {
    const auth = testAuthWorker();
    const { userId } = await signedInTwice(auth, EMAIL);
    await callInternal(auth, "change-email", { userId, email: "dalton.new@gmail.com" });

    const newCookie = await signedInCookie(auth, "dalton.new@gmail.com");

    expect((await sessionBehind(auth, newCookie))?.user.id).toBe(userId);
  });

  it("answers 204 when the user already has that email", async () => {
    const auth = testAuthWorker();
    const { userId } = await signedInTwice(auth, EMAIL);

    const response = await callInternal(auth, "change-email", { userId, email: EMAIL.toUpperCase() });

    expect(response.status).toBe(204);
  });

  it("answers 404 for an unknown user", async () => {
    const response = await callInternal(testAuthWorker(), "change-email", {
      userId: "unknown-user",
      email: "dalton.new@gmail.com",
    });

    expect(response.status).toBe(404);
  });

  it("answers 409 when another user already has the email and changes nothing", async () => {
    const auth = testAuthWorker();
    const { userId, cookies } = await signedInTwice(auth, EMAIL);
    await signedInCookie(auth, OTHER_EMAIL);

    const response = await callInternal(auth, "change-email", { userId, email: OTHER_EMAIL });

    expect(response.status).toBe(409);
    expect((await sessionBehind(auth, cookies[0] ?? ""))?.user.email).toBe(EMAIL);
  });

  it.each([
    ["a missing email", {}],
    ["an email without a domain", { email: "dalton.h@" }],
    ["an email with spaces inside", { email: "dalton h@gmail.com" }],
    ["an email over 100 characters", { email: `${"d".repeat(91)}@gmail.com` }],
  ])("rejects %s with 400", async (_case, fields) => {
    const auth = testAuthWorker();
    const { userId } = await signedInTwice(auth, EMAIL);

    const response = await callInternal(auth, "change-email", { userId, ...fields });

    expect(response.status).toBe(400);
  });
});
