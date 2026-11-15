import type { InviteMessage } from "./inviteEmail";

const MAX_TEXT_LENGTH = 200;
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export async function inviteMessageFrom(request: Request): Promise<InviteMessage | null> {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return null;
  }
  if (typeof body !== "object" || body === null) {
    return null;
  }
  const { email, inviterName, groupName, expiresAt } = body as Record<string, unknown>;
  if (
    !isText(email) ||
    !EMAIL_SHAPE.test(email) ||
    !isText(inviterName) ||
    !isText(groupName) ||
    typeof expiresAt !== "number" ||
    !Number.isSafeInteger(expiresAt)
  ) {
    return null;
  }
  return { email, inviterName, groupName, expiresAt };
}

function isText(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0 && value.length <= MAX_TEXT_LENGTH;
}
