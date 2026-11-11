import { convexTest } from "convex-test";
import { describe, expect, it } from "vitest";
import schema from "./schema";
import { modules } from "./test.setup";

const ownerPermissions = {
  tenants: "full",
  rentTerms: "full",
  payments: "full",
  debts: "full",
  members: "full",
  settings: "full",
  editOthers: true,
} as const;

describe("schema", () => {
  it("stores a group with a tenant and a payment, readable through indexes", async () => {
    const t = convexTest(schema, modules);

    await t.run(async (ctx) => {
      const ownerId = await ctx.db.insert("accounts", {
        authSubject: "auth|owner",
        displayName: "dalton.h",
        email: "owner@example.com",
        version: 1,
      });
      const groupId = await ctx.db.insert("groups", {
        name: "1st Street",
        timeZone: "America/Los_Angeles",
        paymentOrder: "rentFirst",
        graceDays: 5,
        ownerId,
        version: 1,
      });
      await ctx.db.insert("memberships", {
        groupId,
        accountId: ownerId,
        role: "owner",
        ...ownerPermissions,
        notificationsOn: true,
        version: 1,
      });
      const tenantId = await ctx.db.insert("tenants", {
        groupId,
        name: "Apt 3B",
        moveIn: "2026-01-01",
        status: "active",
        version: 1,
      });
      await ctx.db.insert("payments", {
        tenantId,
        amountCents: 150000,
        dateReceived: "2026-03-02",
        method: "zelle",
        clientId: "client-1",
        loggedBy: ownerId,
        version: 1,
      });

      const membership = await ctx.db
        .query("memberships")
        .withIndex("by_group_account", (q) => q.eq("groupId", groupId).eq("accountId", ownerId))
        .unique();
      const activeTenants = await ctx.db
        .query("tenants")
        .withIndex("by_group_status", (q) => q.eq("groupId", groupId).eq("status", "active"))
        .collect();
      const payment = await ctx.db
        .query("payments")
        .withIndex("by_client_id", (q) => q.eq("clientId", "client-1"))
        .unique();

      expect(membership?.role).toBe("owner");
      expect(activeTenants.map((tenant) => tenant.name)).toEqual(["Apt 3B"]);
      expect(payment?.amountCents).toBe(150000);
    });
  });

  it("rejects a row that breaks a validator", async () => {
    const t = convexTest(schema, modules);

    await expect(
      t.run(async (ctx) => {
        const ownerId = await ctx.db.insert("accounts", {
          authSubject: "auth|owner",
          displayName: "owner",
          version: 1,
        });
        await ctx.db.insert("groups", {
          name: "1st Street",
          timeZone: "America/Los_Angeles",
          // @ts-expect-error paymentOrder only accepts rentFirst or byDate
          paymentOrder: "newestFirst",
          graceDays: 5,
          ownerId,
          version: 1,
        });
      }),
    ).rejects.toThrow(/Validator error.*newestFirst/);
  });
});
