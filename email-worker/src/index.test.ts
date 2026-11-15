import { describe, expect, it } from "vitest";
import EmailWorker, { type Env } from "./index";

const SENDER = "rent-tracker@hornor.com";
const RECIPIENT = "dalton.h@gmail.com";
const CODE = "493817";
const SHARED_SECRET = "a-long-random-shared-secret";
const INVITE = {
  email: RECIPIENT,
  inviterName: "Dana",
  groupName: "1st Street",
  expiresAt: Date.UTC(2026, 10, 28),
};

type SentEmail = Parameters<SendEmail["send"]>[0];

function recordingEmailBinding() {
  const sent: SentEmail[] = [];
  const binding = {
    async send(message: SentEmail) {
      sent.push(message);
      return { messageId: "test-message" };
    },
  } as SendEmail;
  return { binding, sent };
}

function failingEmailBinding(): SendEmail {
  return {
    async send() {
      throw new Error("sender domain not verified");
    },
  } as SendEmail;
}

function workerWith(emailBinding: SendEmail, sharedSecret: string | undefined = SHARED_SECRET): EmailWorker {
  const env: Env = { EMAIL: emailBinding, EMAIL_FROM: SENDER, SHARED_SECRET: sharedSecret };
  return new EmailWorker({} as ExecutionContext, env);
}

function inviteRequest(body: unknown, secret: string | null = SHARED_SECRET): Request {
  const headers = new Headers({ "Content-Type": "application/json" });
  if (secret !== null) {
    headers.set("X-Rent-Tracker-Secret", secret);
  }
  const payload = typeof body === "string" ? body : JSON.stringify(body);
  return new Request("https://worker.test/invites", { method: "POST", headers, body: payload });
}

describe("sendSignInCode", () => {
  it("sends one email to the recipient from the configured sender", async () => {
    const { binding, sent } = recordingEmailBinding();

    await workerWith(binding).sendSignInCode({ email: RECIPIENT, code: CODE });

    expect(sent).toHaveLength(1);
    expect(sent[0]).toMatchObject({ to: RECIPIENT, from: { email: SENDER, name: "Rent Tracker" } });
  });

  it("puts the code in the subject, the plain text and the HTML", async () => {
    const { binding, sent } = recordingEmailBinding();

    await workerWith(binding).sendSignInCode({ email: RECIPIENT, code: CODE });

    expect(sent[0]).toMatchObject({
      subject: expect.stringContaining(CODE),
      text: expect.stringContaining(CODE),
      html: expect.stringContaining(CODE),
    });
  });

  it.each(["text", "html"] as const)(
    "says in the %s that the code expires in 10 minutes and can be ignored",
    async (part) => {
      const { binding, sent } = recordingEmailBinding();

      await workerWith(binding).sendSignInCode({ email: RECIPIENT, code: CODE });

      expect(sent[0]).toMatchObject({
        [part]: expect.stringMatching(
          /expires in 10 minutes[\s\S]*did not ask for this code, you can ignore this email/,
        ),
      });
    },
  );

  it("rejects when the email binding fails to send", async () => {
    const worker = workerWith(failingEmailBinding());

    const sending = worker.sendSignInCode({ email: RECIPIENT, code: CODE });

    await expect(sending).rejects.toThrow("sender domain not verified");
  });
});

describe("fetch", () => {
  it("answers GET /health with ok", async () => {
    const worker = workerWith(recordingEmailBinding().binding);

    const response = await worker.fetch(new Request("https://worker.test/health"));

    expect(response.status).toBe(200);
    expect(await response.text()).toBe("ok");
  });

  it("answers any other request with 404", async () => {
    const worker = workerWith(recordingEmailBinding().binding);

    const unknownPath = await worker.fetch(new Request("https://worker.test/anything"));
    const postToHealth = await worker.fetch(new Request("https://worker.test/health", { method: "POST" }));

    expect(unknownPath.status).toBe(404);
    expect(postToHealth.status).toBe(404);
  });
});

describe("POST /invites", () => {
  it("sends the invite email to the invited address from the configured sender", async () => {
    const { binding, sent } = recordingEmailBinding();

    const response = await workerWith(binding).fetch(inviteRequest(INVITE));

    expect(response.status).toBe(204);
    expect(sent).toHaveLength(1);
    expect(sent[0]).toMatchObject({
      to: RECIPIENT,
      from: { email: SENDER, name: "Rent Tracker" },
      subject: "Dana invited you to 1st Street on Rent Tracker",
      text: expect.stringContaining("November 28, 2026"),
    });
  });

  it.each([
    ["no secret header", inviteRequest(INVITE, null)],
    ["a wrong secret", inviteRequest(INVITE, "guessed-secret")],
    ["a secret that only shares a prefix", inviteRequest(INVITE, SHARED_SECRET.slice(0, 8))],
  ])("refuses a request with %s", async (_case, request) => {
    const { binding, sent } = recordingEmailBinding();

    const response = await workerWith(binding).fetch(request);

    expect(response.status).toBe(401);
    expect(sent).toEqual([]);
  });

  it.each([undefined, ""])("refuses every request when the shared secret is %j", async (sharedSecret) => {
    const { binding, sent } = recordingEmailBinding();

    const response = await workerWith(binding, sharedSecret).fetch(inviteRequest(INVITE, ""));

    expect(response.status).toBe(401);
    expect(sent).toEqual([]);
  });

  it.each([
    ["a body that is not JSON", "not json"],
    ["a JSON array", [INVITE]],
    ["a missing email", { ...INVITE, email: undefined }],
    ["an email without a domain", { ...INVITE, email: "dalton" }],
    ["a blank inviter name", { ...INVITE, inviterName: "  " }],
    ["a missing group name", { ...INVITE, groupName: undefined }],
    ["an expiry that is not a number", { ...INVITE, expiresAt: "2026-11-28" }],
  ])("rejects %s with 400", async (_case, body) => {
    const { binding, sent } = recordingEmailBinding();

    const response = await workerWith(binding).fetch(inviteRequest(body));

    expect(response.status).toBe(400);
    expect(sent).toEqual([]);
  });

  it("answers GET /invites with 404", async () => {
    const worker = workerWith(recordingEmailBinding().binding);

    const response = await worker.fetch(new Request("https://worker.test/invites"));

    expect(response.status).toBe(404);
  });
});
