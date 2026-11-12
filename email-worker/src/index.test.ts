import { describe, expect, it } from "vitest";
import EmailWorker, { type Env } from "./index";

const SENDER = "rent-tracker@hornor.com";
const RECIPIENT = "dalton.h@gmail.com";
const CODE = "493817";

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

function workerWith(emailBinding: SendEmail): EmailWorker {
  const env: Env = { EMAIL: emailBinding, EMAIL_FROM: SENDER };
  return new EmailWorker({} as ExecutionContext, env);
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
