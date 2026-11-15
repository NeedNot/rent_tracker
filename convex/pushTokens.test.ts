import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import { type Backend, failureOf, insertAccount, newBackend, sam } from "./seed.fixture";

async function tokenRows(t: Backend) {
  const rows = await t.run((ctx) => ctx.db.query("pushTokens").collect());
  return rows.map(({ accountId, platform, token }) => ({ accountId, platform, token }));
}

describe("pushTokens.register", () => {
  it("stores the caller's token once", async () => {
    const t = await newBackend();

    await t.mutation(api.pushTokens.register, { platform: "android", token: " fcm-token " });
    await t.mutation(api.pushTokens.register, { platform: "android", token: "fcm-token" });

    const ownerId = await t.run((ctx) =>
      ctx.db
        .query("accounts")
        .first()
        .then((row) => row?._id),
    );
    expect(await tokenRows(t)).toEqual([{ accountId: ownerId, platform: "android", token: "fcm-token" }]);
  });

  it("moves a token to whoever signs in on that phone next", async () => {
    const t = await newBackend();
    await t.mutation(api.pushTokens.register, { platform: "android", token: "shared-phone" });
    const samId = await t.run((ctx) => insertAccount(ctx, sam));

    await t.withIdentity(sam).mutation(api.pushTokens.register, { platform: "android", token: "shared-phone" });

    expect(await tokenRows(t)).toEqual([{ accountId: samId, platform: "android", token: "shared-phone" }]);
  });

  it("rejects an empty token", async () => {
    const t = await newBackend();

    const failure = await failureOf(t.mutation(api.pushTokens.register, { platform: "android", token: "  " }));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "token" } });
  });

  it("needs a signed-in account", async () => {
    const t = await newBackend();

    const failure = await failureOf(
      t.withIdentity({}).mutation(api.pushTokens.register, { platform: "android", token: "fcm-token" }),
    );

    expect(failure.code).toBe("NotSignedIn");
  });
});

describe("pushTokens.unregister", () => {
  it("removes the caller's token", async () => {
    const t = await newBackend();
    await t.mutation(api.pushTokens.register, { platform: "android", token: "fcm-token" });

    await t.mutation(api.pushTokens.unregister, { token: "fcm-token" });

    expect(await tokenRows(t)).toEqual([]);
  });

  it("leaves another account's token alone", async () => {
    const t = await newBackend();
    await t.mutation(api.pushTokens.register, { platform: "android", token: "dana-phone" });
    await t.run((ctx) => insertAccount(ctx, sam));

    await t.withIdentity(sam).mutation(api.pushTokens.unregister, { token: "dana-phone" });

    expect((await tokenRows(t)).map((row) => row.token)).toEqual(["dana-phone"]);
  });
});
