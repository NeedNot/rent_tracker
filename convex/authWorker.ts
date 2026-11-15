import { validationFailed } from "./appError";
import { postToWorker } from "./workerPost";

const AUTH_WORKER_URL = "AUTH_WORKER_URL";
const HTTP_CONFLICT = 409;

export const EMAIL_TAKEN = "This email already belongs to another account.";

export async function deleteAuthUser(userId: string): Promise<void> {
  requireAccepted("delete the user", await postToWorker(AUTH_WORKER_URL, "/internal/users/delete", { userId }));
}

export async function changeAuthUserEmail(userId: string, email: string): Promise<void> {
  const response = await postToWorker(AUTH_WORKER_URL, "/internal/users/change-email", { userId, email });
  if (response.status === HTTP_CONFLICT) {
    throw validationFailed(EMAIL_TAKEN, { field: "email" });
  }
  requireAccepted("change the email", response);
}

export async function revokeAuthSessions(userId: string): Promise<void> {
  requireAccepted(
    "revoke the sessions",
    await postToWorker(AUTH_WORKER_URL, "/internal/users/revoke-sessions", { userId }),
  );
}

function requireAccepted(action: string, response: Response): void {
  if (!response.ok) {
    throw new Error(`The auth worker refused to ${action} with HTTP ${response.status}.`);
  }
}
