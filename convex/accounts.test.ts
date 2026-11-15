import { convexTest } from "convex-test";
import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import schema from "./schema";
import { insertGroup, insertMembership, owner } from "./seed.fixture";
import { modules } from "./test.setup";

const dalton = { subject: "auth|dalton", email: "dalton.h@gmail.com" };

function failsWith(code: string) {
  return { data: { code } };
}

describe("accounts.ensure", () => {
  it("creates the account on first sign-in, named after the email's local part", async () => {
    const t = convexTest(schema, modules);

    const ensured = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    expect(ensured).toMatchObject({ displayName: "dalton.h", email: "dalton.h@gmail.com", created: true });
    const stored = await t.run((ctx) => ctx.db.get(ensured.accountId));
    expect(stored).toMatchObject({ authSubject: "auth|dalton", version: 1 });
  });

  it("stores the email lower-cased", async () => {
    const t = convexTest(schema, modules);

    const ensured = await t
      .withIdentity({ subject: "auth|dalton", email: "Dalton.H@Gmail.com" })
      .mutation(api.accounts.ensure, {});

    expect(ensured).toMatchObject({ displayName: "dalton.h", email: "dalton.h@gmail.com" });
  });

  it("returns the same account with created=false on a later sign-in", async () => {
    const t = convexTest(schema, modules);
    const first = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    const second = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    expect(second).toEqual({ ...first, created: false });
    const accounts = await t.run((ctx) => ctx.db.query("accounts").collect());
    expect(accounts).toHaveLength(1);
  });

  it("keeps the chosen display name and stored email when the token later carries another email", async () => {
    const t = convexTest(schema, modules);
    const first = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    await t.withIdentity(dalton).mutation(api.accounts.setDisplayName, { displayName: "Dalton" });

    const second = await t
      .withIdentity({ subject: dalton.subject, email: "new.address@example.com" })
      .mutation(api.accounts.ensure, {});

    expect(second).toEqual({
      accountId: first.accountId,
      displayName: "Dalton",
      email: "dalton.h@gmail.com",
      created: false,
    });
  });

  it("is NotSignedIn without a token", async () => {
    const t = convexTest(schema, modules);

    await expect(t.mutation(api.accounts.ensure, {})).rejects.toMatchObject(failsWith("NotSignedIn"));
  });

  it("is NotSignedIn when the token has no email", async () => {
    const t = convexTest(schema, modules);

    await expect(t.withIdentity({ subject: "auth|nobody" }).mutation(api.accounts.ensure, {})).rejects.toMatchObject(
      failsWith("NotSignedIn"),
    );
  });

  it("names the account after the whole email when nothing comes before the @", async () => {
    const t = convexTest(schema, modules);

    const ensured = await t
      .withIdentity({ subject: "auth|odd", email: "@example.com" })
      .mutation(api.accounts.ensure, {});

    expect(ensured.displayName).toBe("@example.com");
  });

  it("cuts a default name longer than 100 characters down to the limit", async () => {
    const t = convexTest(schema, modules);

    const ensured = await t
      .withIdentity({ subject: "auth|long", email: `${"a".repeat(120)}@example.com` })
      .mutation(api.accounts.ensure, {});

    expect(ensured.displayName).toBe("a".repeat(100));
  });

  it("rejects a second sign-in identity whose email already belongs to an account", async () => {
    const t = convexTest(schema, modules);
    await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    await expect(
      t.withIdentity({ subject: "auth|impostor", email: dalton.email }).mutation(api.accounts.ensure, {}),
    ).rejects.toMatchObject(failsWith("ValidationFailed"));
  });

  it("gives a new empty account to an email whose earlier account was deleted", async () => {
    const t = convexTest(schema, modules);
    const deleted = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    await t.run((ctx) =>
      ctx.db.patch(deleted.accountId, { displayName: "Deleted user", email: undefined, deletedAt: Date.now() }),
    );

    const fresh = await t
      .withIdentity({ subject: "auth|dalton-again", email: dalton.email })
      .mutation(api.accounts.ensure, {});

    expect(fresh).toMatchObject({ displayName: "dalton.h", email: dalton.email, created: true });
    expect(fresh.accountId).not.toBe(deleted.accountId);
  });

  it("is NotSignedIn for a token that still names a deleted account", async () => {
    const t = convexTest(schema, modules);
    const deleted = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    await t.run((ctx) =>
      ctx.db.patch(deleted.accountId, { displayName: "Deleted user", email: undefined, deletedAt: Date.now() }),
    );

    await expect(t.withIdentity(dalton).mutation(api.accounts.ensure, {})).rejects.toMatchObject(
      failsWith("NotSignedIn"),
    );
  });
});

describe("accounts.setDisplayName", () => {
  it("saves the trimmed name and raises the version", async () => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    const saved = await t.withIdentity(dalton).mutation(api.accounts.setDisplayName, { displayName: "  Dalton H  " });

    expect(saved).toBe("Dalton H");
    const stored = await t.run((ctx) => ctx.db.get(accountId));
    expect(stored).toMatchObject({ displayName: "Dalton H", version: 2, lastEditedBy: accountId });
    expect(stored?.lastEditedAt).toBeTypeOf("number");
  });

  it("accepts a name of exactly 100 characters", async () => {
    const t = convexTest(schema, modules);
    await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    const saved = await t.withIdentity(dalton).mutation(api.accounts.setDisplayName, { displayName: "n".repeat(100) });

    expect(saved).toHaveLength(100);
  });

  it.each([
    ["blank", "   "],
    ["longer than 100 characters", "n".repeat(101)],
  ])("rejects a %s name and saves nothing", async (_case, displayName) => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    await expect(t.withIdentity(dalton).mutation(api.accounts.setDisplayName, { displayName })).rejects.toMatchObject(
      failsWith("ValidationFailed"),
    );

    const stored = await t.run((ctx) => ctx.db.get(accountId));
    expect(stored).toMatchObject({ displayName: "dalton.h", version: 1 });
  });

  it("is NotSignedIn without a token", async () => {
    const t = convexTest(schema, modules);

    await expect(t.mutation(api.accounts.setDisplayName, { displayName: "Dalton" })).rejects.toMatchObject(
      failsWith("NotSignedIn"),
    );
  });

  it("is NotSignedIn before the account exists", async () => {
    const t = convexTest(schema, modules);

    await expect(
      t.withIdentity(dalton).mutation(api.accounts.setDisplayName, { displayName: "Dalton" }),
    ).rejects.toMatchObject(failsWith("NotSignedIn"));
  });
});

describe("accounts.me", () => {
  it("returns the caller's own account", async () => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    await t.withIdentity({ subject: "auth|sam", email: "sam@example.com" }).mutation(api.accounts.ensure, {});

    const me = await t.withIdentity(dalton).query(api.accounts.me, {});

    expect(me).toEqual({ accountId, displayName: "dalton.h", email: "dalton.h@gmail.com" });
  });

  it("omits deletionRequestedAt until deletion is requested and includes it afterwards", async () => {
    const t = convexTest(schema, modules);
    await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    const before = await t.withIdentity(dalton).query(api.accounts.me, {});

    const requestedAt = await t.withIdentity(dalton).mutation(api.accounts.requestDeletion, {});

    const after = await t.withIdentity(dalton).query(api.accounts.me, {});
    expect(before.deletionRequestedAt).toBeUndefined();
    expect(after.deletionRequestedAt).toBe(requestedAt);
  });

  it("is NotSignedIn when signed out", async () => {
    const t = convexTest(schema, modules);

    await expect(t.query(api.accounts.me, {})).rejects.toMatchObject(failsWith("NotSignedIn"));
  });

  it("is NotSignedIn before the account exists", async () => {
    const t = convexTest(schema, modules);

    await expect(t.withIdentity(dalton).query(api.accounts.me, {})).rejects.toMatchObject(failsWith("NotSignedIn"));
  });

  it("is NotSignedIn once the account is deleted", async () => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    await t.run((ctx) =>
      ctx.db.patch(accountId, { displayName: "Deleted user", email: undefined, deletedAt: Date.now() }),
    );

    await expect(t.withIdentity(dalton).query(api.accounts.me, {})).rejects.toMatchObject(failsWith("NotSignedIn"));
  });
});

describe("accounts.requestDeletion", () => {
  it("records the request time and raises the version", async () => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});

    const requestedAt = await t.withIdentity(dalton).mutation(api.accounts.requestDeletion, {});

    const stored = await t.run((ctx) => ctx.db.get(accountId));
    expect(stored).toMatchObject({
      deletionRequestedAt: requestedAt,
      version: 2,
      lastEditedAt: requestedAt,
      lastEditedBy: accountId,
    });
  });

  it("keeps the first request time and changes nothing on a repeat request", async () => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    const first = await t.withIdentity(dalton).mutation(api.accounts.requestDeletion, {});
    const storedAfterFirst = await t.run((ctx) => ctx.db.get(accountId));

    const second = await t.withIdentity(dalton).mutation(api.accounts.requestDeletion, {});

    expect(second).toBe(first);
    expect(await t.run((ctx) => ctx.db.get(accountId))).toEqual(storedAfterFirst);
  });

  it("refuses an owner of an active group and names the group", async () => {
    const t = convexTest(schema, modules);
    const groupId = await t.run((ctx) => insertGroup(ctx, { name: "1st Street" }));

    const attempt = t.withIdentity(owner).mutation(api.accounts.requestDeletion, {});

    await expect(attempt).rejects.toMatchObject({
      data: {
        code: "ValidationFailed",
        message: "Transfer ownership of 1st Street first.",
        details: { groups: [{ groupId, name: "1st Street" }] },
      },
    });
    const stored = await t.run((ctx) => ctx.db.query("accounts").first());
    expect(stored?.deletionRequestedAt).toBeUndefined();
  });

  it("refuses an owner of only an archived group", async () => {
    const t = convexTest(schema, modules);
    await t.run((ctx) => insertGroup(ctx, { name: "Maple Court", archivedAt: Date.now() }));

    await expect(t.withIdentity(owner).mutation(api.accounts.requestDeletion, {})).rejects.toMatchObject(
      failsWith("ValidationFailed"),
    );
  });

  it("lists every owned group sorted by name, joined naturally", async () => {
    const t = convexTest(schema, modules);
    await t.run(async (ctx) => {
      await insertGroup(ctx, { name: "Maple Court" });
      await insertGroup(ctx, { name: "1st Street" });
      await insertGroup(ctx, { name: "Birch Lane" });
    });

    await expect(t.withIdentity(owner).mutation(api.accounts.requestDeletion, {})).rejects.toMatchObject({
      data: { message: "Transfer ownership of 1st Street, Birch Lane and Maple Court first." },
    });
  });

  it("lets a member who owns nothing request deletion", async () => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    await t.run(async (ctx) => {
      const groupId = await insertGroup(ctx);
      await insertMembership(ctx, groupId, accountId);
    });

    const requestedAt = await t.withIdentity(dalton).mutation(api.accounts.requestDeletion, {});

    expect(requestedAt).toBeTypeOf("number");
  });

  it("is NotSignedIn once the account is deleted", async () => {
    const t = convexTest(schema, modules);
    const { accountId } = await t.withIdentity(dalton).mutation(api.accounts.ensure, {});
    await t.run((ctx) =>
      ctx.db.patch(accountId, { displayName: "Deleted user", email: undefined, deletedAt: Date.now() }),
    );

    await expect(t.withIdentity(dalton).mutation(api.accounts.requestDeletion, {})).rejects.toMatchObject(
      failsWith("NotSignedIn"),
    );
  });

  it("is NotSignedIn without a token", async () => {
    const t = convexTest(schema, modules);

    await expect(t.mutation(api.accounts.requestDeletion, {})).rejects.toMatchObject(failsWith("NotSignedIn"));
  });
});
