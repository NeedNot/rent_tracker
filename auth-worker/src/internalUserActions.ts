import type { InternalAdapter } from "better-auth";
import { emailChangeFrom, userReferenceFrom } from "./internalUserRequests";

export type InternalUserAction = (users: InternalAdapter, body: Record<string, unknown>) => Promise<Response>;

const noContent = () => new Response(null, { status: 204 });
const badRequest = () => new Response("Bad request", { status: 400 });

export const deleteUser: InternalUserAction = async (users, body) => {
  const user = userReferenceFrom(body);
  if (user === null) {
    return badRequest();
  }
  await users.deleteUser(user.userId);
  return noContent();
};

export const revokeSessions: InternalUserAction = async (users, body) => {
  const user = userReferenceFrom(body);
  if (user === null) {
    return badRequest();
  }
  await users.deleteUserSessions(user.userId);
  return noContent();
};

export const changeEmail: InternalUserAction = async (users, body) => {
  const change = emailChangeFrom(body);
  if (change === null) {
    return badRequest();
  }
  if ((await users.findUserById(change.userId)) === null) {
    return new Response("User not found", { status: 404 });
  }
  const emailOwner = await users.findUserByEmail(change.email);
  if (emailOwner !== null && emailOwner.user.id !== change.userId) {
    return new Response("Email already in use", { status: 409 });
  }
  await users.updateUser(change.userId, { email: change.email, emailVerified: true });
  return noContent();
};
