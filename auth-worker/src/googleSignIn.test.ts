import { exportJWK, generateKeyPair, SignJWT } from "jose";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { errorCode, get, post, signedInCookie } from "../test/appClient";
import {
  GOOGLE_ANDROID_CLIENT_ID,
  GOOGLE_WEB_CLIENT_ID,
  type TestAuthWorker,
  testAuthWorker,
} from "../test/authWorkerEnv";

const EMAIL = "dalton.h@gmail.com";
const GOOGLE_KEY_ID = "google-test-key";
const GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";

type GoogleIdentity = {
  readonly audience: string;
  readonly email?: string;
  readonly emailVerified?: boolean;
};

type SignInResponse = { readonly user: { readonly id: string; readonly email: string } };

const googleKeys = await generateKeyPair("RS256");

async function googleIdToken({ audience, email = EMAIL, emailVerified = true }: GoogleIdentity): Promise<string> {
  return new SignJWT({ email, email_verified: emailVerified, name: "Dalton H" })
    .setProtectedHeader({ alg: "RS256", kid: GOOGLE_KEY_ID })
    .setIssuer("https://accounts.google.com")
    .setSubject(`google-subject-of-${email}`)
    .setAudience(audience)
    .setIssuedAt()
    .setExpirationTime("1 hour")
    .sign(googleKeys.privateKey);
}

function signInWithGoogle(auth: TestAuthWorker, idToken: string): Promise<Response> {
  return post(auth, "/api/auth/sign-in/social", { provider: "google", idToken: { token: idToken } });
}

async function userIdBehind(auth: TestAuthWorker, cookie: string): Promise<string> {
  const session = (await (await get(auth, "/api/auth/get-session", { cookie })).json()) as SignInResponse;
  return session.user.id;
}

describe("Google sign-in by ID token", () => {
  beforeEach(async () => {
    const googlePublicKey = { ...(await exportJWK(googleKeys.publicKey)), kid: GOOGLE_KEY_ID, alg: "RS256" };
    vi.stubGlobal("fetch", async (input: RequestInfo | URL) => {
      const url = input instanceof Request ? input.url : String(input);
      return url === GOOGLE_CERTS_URL
        ? Response.json({ keys: [googlePublicKey] })
        : new Response(null, { status: 404 });
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it.each([
    ["Web", GOOGLE_WEB_CLIENT_ID],
    ["Android", GOOGLE_ANDROID_CLIENT_ID],
  ])("signs in with an ID token issued to the %s client ID", async (_client, audience) => {
    const auth = testAuthWorker();

    const response = await signInWithGoogle(auth, await googleIdToken({ audience }));

    expect(response.status).toBe(200);
    expect(await response.json()).toMatchObject({ redirect: false, user: { email: EMAIL, emailVerified: true } });
    expect(response.headers.getSetCookie().join()).toContain("__Secure-better-auth.session_token=");
  });

  it("rejects an ID token issued to another app with INVALID_TOKEN", async () => {
    const auth = testAuthWorker();

    const response = await signInWithGoogle(
      auth,
      await googleIdToken({ audience: "another-app.apps.googleusercontent.com" }),
    );

    expect(response.status).toBe(401);
    expect(await errorCode(response)).toBe("INVALID_TOKEN");
  });

  it("lands on the account that first signed in with an email code", async () => {
    const auth = testAuthWorker();
    const emailCodeUserId = await userIdBehind(auth, await signedInCookie(auth, EMAIL));

    const google = await signInWithGoogle(auth, await googleIdToken({ audience: GOOGLE_ANDROID_CLIENT_ID }));

    expect(((await google.json()) as SignInResponse).user.id).toBe(emailCodeUserId);
  });

  it("lets an account created with Google sign in with an email code", async () => {
    const auth = testAuthWorker();
    const google = await signInWithGoogle(auth, await googleIdToken({ audience: GOOGLE_ANDROID_CLIENT_ID }));
    const googleUserId = ((await google.json()) as SignInResponse).user.id;

    const emailCodeUserId = await userIdBehind(auth, await signedInCookie(auth, EMAIL));

    expect(emailCodeUserId).toBe(googleUserId);
  });

  it("does not link a Google identity with an unverified email to an existing account", async () => {
    const auth = testAuthWorker();
    await signedInCookie(auth, EMAIL);

    const response = await signInWithGoogle(
      auth,
      await googleIdToken({ audience: GOOGLE_ANDROID_CLIENT_ID, emailVerified: false }),
    );

    expect(response.ok).toBe(false);
    expect(response.headers.getSetCookie()).toEqual([]);
  });
});
