import worker from "../src/index";
import { APP_ORIGIN, AUTH_URL, type TestAuthWorker } from "./authWorkerEnv";

type AppRequest = {
  readonly cookie?: string;
  readonly origin?: string;
  readonly ip?: string;
};

let nextClientNumber = 1;

function freshClientIp(): string {
  nextClientNumber += 1;
  return `203.0.113.${nextClientNumber % 250}`;
}

function headers({ cookie, origin = APP_ORIGIN, ip = freshClientIp() }: AppRequest): Headers {
  const requestHeaders = new Headers({ origin, "cf-connecting-ip": ip, "content-type": "application/json" });
  if (cookie !== undefined) {
    requestHeaders.set("cookie", cookie);
  }
  return requestHeaders;
}

export function post(auth: TestAuthWorker, path: string, body: unknown, request: AppRequest = {}): Promise<Response> {
  return worker.fetch(
    new Request(`${AUTH_URL}${path}`, { method: "POST", headers: headers(request), body: JSON.stringify(body) }),
    auth.env,
  );
}

export function get(auth: TestAuthWorker, path: string, request: AppRequest = {}): Promise<Response> {
  return worker.fetch(new Request(`${AUTH_URL}${path}`, { headers: headers(request) }), auth.env);
}

export function requestSignInCode(auth: TestAuthWorker, email: string, request: AppRequest = {}): Promise<Response> {
  return post(auth, "/api/auth/email-otp/send-verification-otp", { email, type: "sign-in" }, request);
}

export function signInWithCode(auth: TestAuthWorker, email: string, code: string): Promise<Response> {
  return post(auth, "/api/auth/sign-in/email-otp", { email, otp: code });
}

export function cookieHeaderFrom(response: Response): string {
  return response.headers
    .getSetCookie()
    .map((setCookie) => setCookie.split(";")[0])
    .join("; ");
}

export async function signedInCookie(auth: TestAuthWorker, email: string): Promise<string> {
  await requestSignInCode(auth, email);
  const code = auth.sentSignInCodes.at(-1)?.code ?? "";
  return cookieHeaderFrom(await signInWithCode(auth, email, code));
}

export async function errorCode(response: Response): Promise<unknown> {
  const body = (await response.json()) as { code?: unknown };
  return body.code;
}
