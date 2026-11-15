const MAX_USER_ID_LENGTH = 100;
const MAX_EMAIL_LENGTH = 100;
const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export type UserReference = {
  readonly userId: string;
};

export type EmailChange = UserReference & {
  readonly email: string;
};

export async function jsonObjectFrom(request: Request): Promise<Record<string, unknown> | null> {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return null;
  }
  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    return null;
  }
  return body as Record<string, unknown>;
}

export function userReferenceFrom(body: Record<string, unknown>): UserReference | null {
  const { userId } = body;
  if (typeof userId !== "string" || userId.trim() === "" || userId.length > MAX_USER_ID_LENGTH) {
    return null;
  }
  return { userId };
}

export function emailChangeFrom(body: Record<string, unknown>): EmailChange | null {
  const user = userReferenceFrom(body);
  if (user === null || typeof body.email !== "string") {
    return null;
  }
  const email = body.email.trim().toLowerCase();
  if (email.length > MAX_EMAIL_LENGTH || !EMAIL_SHAPE.test(email)) {
    return null;
  }
  return { userId: user.userId, email };
}
