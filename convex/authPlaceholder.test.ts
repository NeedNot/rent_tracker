import { convexTest } from "convex-test";
import { describe, expect, it } from "vitest";
import { placeholderAccountId, requireMember, requirePermission } from "./authPlaceholder";
import schema from "./schema";
import { modules } from "./test.setup";

describe("placeholderAccountId", () => {
  it("creates the placeholder account on first use", async () => {
    const t = convexTest(schema, modules);

    const account = await t.run(async (ctx) => ctx.db.get(await placeholderAccountId(ctx)));

    expect(account).toMatchObject({ authSubject: "placeholder", displayName: "You", version: 1 });
  });

  it("returns the same account on every later call", async () => {
    const t = convexTest(schema, modules);

    const first = await t.run((ctx) => placeholderAccountId(ctx));
    const second = await t.run((ctx) => placeholderAccountId(ctx));
    const accounts = await t.run((ctx) => ctx.db.query("accounts").collect());

    expect(second).toBe(first);
    expect(accounts).toHaveLength(1);
  });

  it("leaves other accounts alone", async () => {
    const t = convexTest(schema, modules);

    const otherId = await t.run((ctx) =>
      ctx.db.insert("accounts", { authSubject: "auth|someone", displayName: "Sam", version: 1 }),
    );
    const placeholderId = await t.run((ctx) => placeholderAccountId(ctx));

    expect(placeholderId).not.toBe(otherId);
  });
});

describe("placeholder access helpers", () => {
  it("allow every read and write until sign-in exists", async () => {
    const t = convexTest(schema, modules);

    await t.run(async (ctx) => {
      const ownerId = await placeholderAccountId(ctx);
      const groupId = await ctx.db.insert("groups", {
        name: "1st Street",
        timeZone: "America/Los_Angeles",
        paymentOrder: "rentFirst",
        graceDays: 5,
        ownerId,
        version: 1,
      });

      await expect(requireMember(ctx, groupId)).resolves.toBeUndefined();
      await expect(requirePermission(ctx, groupId, "tenants", "full")).resolves.toBeUndefined();
    });
  });
});
