import { afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { internal } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import { type Backend, insertAccount, insertGroup, newBackend, owner } from "./seed.fixture";

const PROJECT_ID = "rent-tracker-test";
const TOKEN_URI = "https://oauth2.example.test/token";
const SEND_URL = `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`;
const ACCESS_TOKEN = "ya29.test-access-token";

let privateKeyPem = "";
let publicKey: CryptoKey;

interface DeliveryScene {
  t: Backend;
  groupId: Id<"groups">;
}

async function rsaKeyPair() {
  const keys = (await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  )) as CryptoKeyPair;
  const pkcs8 = new Uint8Array(await crypto.subtle.exportKey("pkcs8", keys.privateKey));
  const base64 = btoa(Array.from(pkcs8, (byte) => String.fromCharCode(byte)).join(""));
  return { pem: `-----BEGIN PRIVATE KEY-----\n${base64}\n-----END PRIVATE KEY-----\n`, publicKey: keys.publicKey };
}

function stubFcm(sendResponses: Record<string, Response>) {
  const fetchMock = vi.fn(async (url: string, init: RequestInit) => {
    if (url === TOKEN_URI) {
      return Response.json({ access_token: ACCESS_TOKEN, expires_in: 3599 });
    }
    const token = JSON.parse(String(init.body)).message.token as string;
    return sendResponses[token] ?? Response.json({ name: "projects/x/messages/1" });
  });
  vi.stubGlobal("fetch", fetchMock);
  vi.stubEnv(
    "FCM_SERVICE_ACCOUNT_JSON",
    JSON.stringify({
      project_id: PROJECT_ID,
      client_email: "push@rent-tracker-test.iam.gserviceaccount.com",
      private_key: privateKeyPem,
      token_uri: TOKEN_URI,
    }),
  );
  return fetchMock;
}

function fcmError(status: number, errorCode: string): Response {
  return Response.json(
    {
      error: {
        status: "ERROR",
        details: [{ "@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError", errorCode }],
      },
    },
    { status },
  );
}

async function deliveryScene(tokens: string[]): Promise<DeliveryScene> {
  const t = await newBackend();
  const groupId = await t.run(async (ctx) => {
    const accountId = await insertAccount(ctx, owner);
    for (const token of tokens) {
      await ctx.db.insert("pushTokens", { accountId, token, platform: "android" });
    }
    return await insertGroup(ctx);
  });
  return { t, groupId };
}

function send(scene: DeliveryScene, tokens: string[]) {
  return scene.t.action(internal.pushDelivery.send, {
    groupId: scene.groupId,
    title: "Rent is late",
    body: "1st Street: 1 tenant is late, $1,500.00 owed.",
    tokens,
  });
}

async function remainingTokens(t: Backend): Promise<string[]> {
  const rows = await t.run((ctx) => ctx.db.query("pushTokens").collect());
  return rows.map((row) => row.token).sort();
}

function base64UrlBytes(text = ""): Uint8Array<ArrayBuffer> {
  const base64 = text.replace(/-/g, "+").replace(/_/g, "/");
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
}

function fetchCall(fetchMock: ReturnType<typeof stubFcm>, url: string, index = 0): RequestInit {
  const call = fetchMock.mock.calls.filter(([calledUrl]) => calledUrl === url)[index];
  if (call === undefined) {
    throw new Error(`No fetch call ${index} to ${url}`);
  }
  return call[1];
}

beforeAll(async () => {
  const keys = await rsaKeyPair();
  privateKeyPem = keys.pem;
  publicKey = keys.publicKey;
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("pushDelivery.send", () => {
  it("signs a service-account assertion for the FCM messaging scope", async () => {
    const fetchMock = stubFcm({});
    const scene = await deliveryScene(["phone-a"]);

    await send(scene, ["phone-a"]);

    const init = fetchCall(fetchMock, TOKEN_URI);
    const assertion = new URLSearchParams(String(init.body)).get("assertion") ?? "";
    const [header, claims, signature] = assertion.split(".");
    expect(JSON.parse(new TextDecoder().decode(base64UrlBytes(claims)))).toMatchObject({
      iss: "push@rent-tracker-test.iam.gserviceaccount.com",
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: TOKEN_URI,
    });
    const verified = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      publicKey,
      base64UrlBytes(signature),
      new TextEncoder().encode(`${header}.${claims}`),
    );
    expect(verified).toBe(true);
  });

  it("sends one data message per token that opens the group", async () => {
    const fetchMock = stubFcm({});
    const scene = await deliveryScene(["phone-a", "phone-b"]);

    await send(scene, ["phone-a", "phone-b"]);

    expect(fetchMock.mock.calls.filter(([url]) => url === SEND_URL)).toHaveLength(2);
    const init = fetchCall(fetchMock, SEND_URL);
    expect((init.headers as Record<string, string>).Authorization).toBe(`Bearer ${ACCESS_TOKEN}`);
    expect(JSON.parse(String(init.body)).message).toEqual({
      token: "phone-a",
      data: {
        type: "lateRent",
        groupId: scene.groupId,
        title: "Rent is late",
        body: "1st Street: 1 tenant is late, $1,500.00 owed.",
      },
      android: { priority: "HIGH", collapse_key: scene.groupId },
    });
  });

  it("removes tokens FCM reports as unregistered", async () => {
    stubFcm({ "phone-gone": fcmError(404, "UNREGISTERED"), "other-app": fcmError(403, "SENDER_ID_MISMATCH") });
    const scene = await deliveryScene(["phone-a", "phone-gone", "other-app"]);

    await send(scene, ["phone-a", "phone-gone", "other-app"]);

    expect(await remainingTokens(scene.t)).toEqual(["phone-a"]);
  });

  it("fails loudly on other FCM errors and keeps the token", async () => {
    stubFcm({ "phone-a": fcmError(503, "UNAVAILABLE") });
    const scene = await deliveryScene(["phone-a"]);

    await expect(send(scene, ["phone-a"])).rejects.toThrow("FCM refused 1 of 1 late-rent pushes (HTTP 503).");
    expect(await remainingTokens(scene.t)).toEqual(["phone-a"]);
  });

  it("fails when the service account is not configured", async () => {
    const scene = await deliveryScene(["phone-a"]);

    await expect(send(scene, ["phone-a"])).rejects.toThrow(
      "The Convex environment variable FCM_SERVICE_ACCOUNT_JSON is not set.",
    );
  });
});
