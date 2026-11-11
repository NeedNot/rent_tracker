import { describe, expect, it } from "vitest";
import worker from "./index";

describe("fetch", () => {
  it("answers GET /health with ok", async () => {
    const response = await worker.fetch(new Request("https://worker.test/health"));

    expect(response.status).toBe(200);
    expect(await response.text()).toBe("ok");
  });

  it("answers any other request with 404", async () => {
    const unknownPath = await worker.fetch(new Request("https://worker.test/anything"));
    const postToHealth = await worker.fetch(new Request("https://worker.test/health", { method: "POST" }));

    expect(unknownPath.status).toBe(404);
    expect(postToHealth.status).toBe(404);
  });
});
