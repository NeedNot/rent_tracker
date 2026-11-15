import { describe, expect, it } from "vitest";
import { testAuthWorker } from "../test/authWorkerEnv";
import { callInternal } from "../test/internalClient";

const ACTIONS = ["delete", "change-email", "revoke-sessions"];
const VALID_BODY = { userId: "some-user", email: "new.address@gmail.com" };

describe("internal user routes", () => {
  describe.each(ACTIONS)("POST /internal/users/%s", (action) => {
    it.each([
      ["no secret header", null],
      ["a wrong secret", "guessed-secret"],
      ["a secret that only shares a prefix", "a-long-random"],
    ])("refuses a request with %s", async (_case, secret) => {
      const response = await callInternal(testAuthWorker(), action, VALID_BODY, { secret });

      expect(response.status).toBe(401);
    });

    it.each([undefined, ""])("refuses every request when the worker's shared secret is %j", async (sharedSecret) => {
      const auth = testAuthWorker();

      const response = await callInternal(
        { ...auth, env: { ...auth.env, SHARED_SECRET: sharedSecret } },
        action,
        VALID_BODY,
        {
          secret: "",
        },
      );

      expect(response.status).toBe(401);
    });

    it.each([
      ["malformed JSON", "{not json"],
      ["a JSON array", "[]"],
      ["no userId", {}],
      ["an empty userId", { userId: " " }],
      ["a numeric userId", { userId: 42 }],
    ])("rejects %s with 400", async (_case, body) => {
      const response = await callInternal(testAuthWorker(), action, body);

      expect(response.status).toBe(400);
    });

    it("answers other methods with 404", async () => {
      const response = await callInternal(testAuthWorker(), action, VALID_BODY, { method: "GET" });

      expect(response.status).toBe(404);
    });
  });

  it("answers an unknown internal action with 404 even with the right secret", async () => {
    const response = await callInternal(testAuthWorker(), "promote", VALID_BODY);

    expect(response.status).toBe(404);
  });
});
