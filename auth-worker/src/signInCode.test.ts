import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cookieHeaderFrom, errorCode, post, requestSignInCode, signInWithCode } from "../test/appClient";
import { type TestAuthWorker, testAuthWorker } from "../test/authWorkerEnv";

const EMAIL = "dalton.h@gmail.com";
const MINUTE = 60 * 1000;

function lastCode(auth: TestAuthWorker): string {
  return auth.sentSignInCodes.at(-1)?.code ?? "";
}

function wrongCodeFor(code: string): string {
  return code === "000000" ? "111111" : "000000";
}

describe("email sign-in code", () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["Date"], now: new Date("2026-11-12T12:00:00Z") });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("emails a 6-digit code to the lower-cased address through the email worker", async () => {
    const auth = testAuthWorker();

    const response = await requestSignInCode(auth, "Dalton.H@gmail.com");

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ success: true });
    expect(auth.sentSignInCodes).toEqual([{ email: EMAIL, code: expect.stringMatching(/^\d{6}$/) }]);
  });

  it("signs in with the emailed code, returning the user and a secure session cookie", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);

    const response = await signInWithCode(auth, EMAIL, lastCode(auth));

    expect(response.status).toBe(200);
    expect(await response.json()).toMatchObject({
      user: { id: expect.any(String), email: EMAIL, emailVerified: true },
    });
    expect(response.headers.getSetCookie()).toEqual([
      expect.stringMatching(/^__Secure-better-auth\.session_token=.+; Max-Age=31536000; Path=\/; HttpOnly; Secure/),
    ]);
  });

  it("signs the same email in to the same user every time", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);
    const firstSignIn = (await (await signInWithCode(auth, EMAIL, lastCode(auth))).json()) as { user: { id: string } };
    vi.advanceTimersByTime(MINUTE);
    await requestSignInCode(auth, EMAIL);

    const secondSignIn = (await (await signInWithCode(auth, EMAIL, lastCode(auth))).json()) as { user: { id: string } };

    expect(secondSignIn.user.id).toBe(firstSignIn.user.id);
  });

  it("accepts a code only once", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);
    await signInWithCode(auth, EMAIL, lastCode(auth));

    const reuse = await signInWithCode(auth, EMAIL, lastCode(auth));

    expect(reuse.status).toBe(400);
    expect(await errorCode(reuse)).toBe("INVALID_OTP");
  });

  it("rejects a wrong code with INVALID_OTP and still accepts the right one afterwards", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);

    const wrongGuess = await signInWithCode(auth, EMAIL, wrongCodeFor(lastCode(auth)));
    const rightGuess = await signInWithCode(auth, EMAIL, lastCode(auth));

    expect(wrongGuess.status).toBe(400);
    expect(await errorCode(wrongGuess)).toBe("INVALID_OTP");
    expect(wrongGuess.headers.getSetCookie()).toEqual([]);
    expect(rightGuess.status).toBe(200);
  });

  it("cancels the code on the third wrong guess", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);
    const wrongCode = wrongCodeFor(lastCode(auth));
    await signInWithCode(auth, EMAIL, wrongCode);
    await signInWithCode(auth, EMAIL, wrongCode);
    await signInWithCode(auth, EMAIL, wrongCode);

    const rightGuessAfterThreeWrong = await signInWithCode(auth, EMAIL, lastCode(auth));

    expect(rightGuessAfterThreeWrong.status).toBe(403);
    expect(await errorCode(rightGuessAfterThreeWrong)).toBe("TOO_MANY_ATTEMPTS");
  });

  it("rejects the code with OTP_EXPIRED after 10 minutes", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);
    vi.advanceTimersByTime(10 * MINUTE + 1);

    const response = await signInWithCode(auth, EMAIL, lastCode(auth));

    expect(response.status).toBe(400);
    expect(await errorCode(response)).toBe("OTP_EXPIRED");
  });

  it("still accepts the code just before 10 minutes have passed", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);
    vi.advanceTimersByTime(10 * MINUTE - 1000);

    const response = await signInWithCode(auth, EMAIL, lastCode(auth));

    expect(response.status).toBe(200);
  });

  it("refuses a second code for the same email within 60 seconds and keeps the first code valid", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);
    vi.advanceTimersByTime(59 * 1000);

    const tooSoon = await requestSignInCode(auth, "DALTON.H@gmail.com");
    const signIn = await signInWithCode(auth, EMAIL, lastCode(auth));

    expect(tooSoon.status).toBe(429);
    expect(await errorCode(tooSoon)).toBe("TOO_MANY_SIGN_IN_CODES");
    expect(tooSoon.headers.get("x-retry-after")).toBe("1");
    expect(auth.sentSignInCodes).toHaveLength(1);
    expect(signIn.status).toBe(200);
  });

  it("sends a new code after 60 seconds, which replaces the previous one", async () => {
    const auth = testAuthWorker();
    await requestSignInCode(auth, EMAIL);
    const firstCode = lastCode(auth);
    vi.advanceTimersByTime(MINUTE);

    const secondRequest = await requestSignInCode(auth, EMAIL);

    expect(secondRequest.status).toBe(200);
    expect(auth.sentSignInCodes).toHaveLength(2);
    if (lastCode(auth) !== firstCode) {
      expect(await errorCode(await signInWithCode(auth, EMAIL, firstCode))).toBe("INVALID_OTP");
    }
    expect((await signInWithCode(auth, EMAIL, lastCode(auth))).status).toBe(200);
  });

  it("sends at most 5 codes per hour to one email, whatever address the requests come from", async () => {
    const auth = testAuthWorker();
    for (let sent = 0; sent < 5; sent += 1) {
      await requestSignInCode(auth, EMAIL);
      vi.advanceTimersByTime(2 * MINUTE);
    }

    const sixthInTheHour = await requestSignInCode(auth, EMAIL);
    const otherEmail = await requestSignInCode(auth, "someone.else@gmail.com");
    vi.advanceTimersByTime(50 * MINUTE);
    const afterTheHour = await requestSignInCode(auth, EMAIL);

    expect(sixthInTheHour.status).toBe(429);
    expect(await errorCode(sixthInTheHour)).toBe("TOO_MANY_SIGN_IN_CODES");
    expect(sixthInTheHour.headers.get("x-retry-after")).toBe(String(50 * 60));
    expect(otherEmail.status).toBe(200);
    expect(afterTheHour.status).toBe(200);
  });

  it("limits code requests from one IP address to 3 per minute", async () => {
    const auth = testAuthWorker();
    const ip = "198.51.100.7";
    await requestSignInCode(auth, "one@gmail.com", { ip });
    await requestSignInCode(auth, "two@gmail.com", { ip });
    await requestSignInCode(auth, "three@gmail.com", { ip });

    const fourth = await requestSignInCode(auth, "four@gmail.com", { ip });

    expect(fourth.status).toBe(429);
    expect(fourth.headers.get("x-retry-after")).toBe("60");
    expect(auth.sentSignInCodes).toHaveLength(3);
  });

  it("only issues sign-in codes", async () => {
    const auth = testAuthWorker();

    const response = await post(auth, "/api/auth/email-otp/send-verification-otp", {
      email: EMAIL,
      type: "forget-password",
    });

    expect(response.status).toBe(400);
    expect(await errorCode(response)).toBe("SIGN_IN_CODE_ONLY");
    expect(auth.sentSignInCodes).toEqual([]);
  });

  it("has no password sign-up, password sign-in or password reset", async () => {
    const auth = testAuthWorker();

    const signUp = await post(auth, "/api/auth/sign-up/email", {
      email: EMAIL,
      password: "a-long-password",
      name: "D",
    });
    const signIn = await post(auth, "/api/auth/sign-in/email", { email: EMAIL, password: "a-long-password" });
    const reset = await post(auth, "/api/auth/email-otp/reset-password", {
      email: EMAIL,
      otp: "123456",
      password: "a-long-password",
    });

    expect(await errorCode(signUp)).toBe("EMAIL_PASSWORD_SIGN_UP_DISABLED");
    expect(signIn.ok).toBe(false);
    expect(reset.status).toBe(404);
    expect(cookieHeaderFrom(signIn)).toBe("");
  });
});
