const SERVICE_ACCOUNT_VARIABLE = "FCM_SERVICE_ACCOUNT_JSON";
const MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
const JWT_BEARER_GRANT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
const ASSERTION_LIFETIME_SECONDS = 3600;
const MS_PER_SECOND = 1000;

export interface FcmServiceAccount {
  projectId: string;
  clientEmail: string;
  privateKeyPem: string;
  tokenUri: string;
}

export function fcmServiceAccount(): FcmServiceAccount {
  const json = process.env[SERVICE_ACCOUNT_VARIABLE];
  if (json === undefined || json === "") {
    throw new Error(`The Convex environment variable ${SERVICE_ACCOUNT_VARIABLE} is not set.`);
  }
  const parsed = JSON.parse(json) as Record<string, unknown>;
  return {
    projectId: requiredField(parsed, "project_id"),
    clientEmail: requiredField(parsed, "client_email"),
    privateKeyPem: requiredField(parsed, "private_key"),
    tokenUri: typeof parsed.token_uri === "string" ? parsed.token_uri : DEFAULT_TOKEN_URI,
  };
}

export async function fcmAccessToken(account: FcmServiceAccount, nowMs: number): Promise<string> {
  const response = await fetch(account.tokenUri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: JWT_BEARER_GRANT, assertion: await signedAssertion(account, nowMs) }),
  });
  if (!response.ok) {
    throw new Error(`Google refused the FCM access token request with HTTP ${response.status}.`);
  }
  const body = (await response.json()) as { access_token?: unknown };
  if (typeof body.access_token !== "string") {
    throw new Error("Google's token response had no access_token.");
  }
  return body.access_token;
}

async function signedAssertion(account: FcmServiceAccount, nowMs: number): Promise<string> {
  const issuedAt = Math.floor(nowMs / MS_PER_SECOND);
  const header = base64UrlJson({ alg: "RS256", typ: "JWT" });
  const claims = base64UrlJson({
    iss: account.clientEmail,
    scope: MESSAGING_SCOPE,
    aud: account.tokenUri,
    iat: issuedAt,
    exp: issuedAt + ASSERTION_LIFETIME_SECONDS,
  });
  const signingInput = `${header}.${claims}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemBytes(account.privateKeyPem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(signingInput));
  return `${signingInput}.${base64Url(new Uint8Array(signature))}`;
}

function pemBytes(pem: string): ArrayBuffer {
  const base64 = pem.replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0)).buffer;
}

function base64UrlJson(value: object): string {
  return base64Url(new TextEncoder().encode(JSON.stringify(value)));
}

function base64Url(bytes: Uint8Array): string {
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join("");
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function requiredField(parsed: Record<string, unknown>, name: string): string {
  const value = parsed[name];
  if (typeof value !== "string" || value === "") {
    throw new Error(`${SERVICE_ACCOUNT_VARIABLE} has no ${name}.`);
  }
  return value;
}
