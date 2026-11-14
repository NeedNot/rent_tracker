import { describe, expect, it } from "vitest";
import { get, post } from "../test/appClient";
import { testAuthWorker } from "../test/authWorkerEnv";

describe("fetch", () => {
  it("answers GET /health with ok", async () => {
    const response = await get(testAuthWorker(), "/health");

    expect(response.status).toBe(200);
    expect(await response.text()).toBe("ok");
  });

  it("answers anything outside /health and /api/auth with 404", async () => {
    const auth = testAuthWorker();

    const unknownPath = await get(auth, "/anything");
    const postToHealth = await post(auth, "/health", {});
    const authPrefixLookalike = await get(auth, "/api/authentication");

    expect(unknownPath.status).toBe(404);
    expect(postToHealth.status).toBe(404);
    expect(authPrefixLookalike.status).toBe(404);
  });

  it("serves Better Auth under /api/auth", async () => {
    const response = await get(testAuthWorker(), "/api/auth/ok");

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ ok: true });
  });
});
