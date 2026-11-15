import { betterAuth } from "better-auth";
import { authOptions } from "./authOptions";
import type { Env } from "./env";
import { changeEmail, deleteUser, type InternalUserAction, revokeSessions } from "./internalUserActions";
import { jsonObjectFrom } from "./internalUserRequests";
import { matchesSharedSecret } from "./sharedSecret";

export const INTERNAL_USERS_PATH = "/internal/users/";
const SECRET_HEADER = "X-Rent-Tracker-Secret";

const ACTIONS = new Map<string, InternalUserAction>([
  ["delete", deleteUser],
  ["change-email", changeEmail],
  ["revoke-sessions", revokeSessions],
]);

export async function handleInternalUserRequest(request: Request, env: Env): Promise<Response> {
  const action = ACTIONS.get(new URL(request.url).pathname.slice(INTERNAL_USERS_PATH.length));
  if (request.method !== "POST" || action === undefined) {
    return new Response("Not found", { status: 404 });
  }
  if (!matchesSharedSecret(request.headers.get(SECRET_HEADER), env.SHARED_SECRET)) {
    return new Response("Unauthorized", { status: 401 });
  }
  const body = await jsonObjectFrom(request);
  if (body === null) {
    return new Response("Bad request", { status: 400 });
  }
  const { internalAdapter } = await betterAuth(authOptions(env)).$context;
  return await action(internalAdapter, body);
}
