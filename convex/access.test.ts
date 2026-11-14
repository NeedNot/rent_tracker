import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import {
  failureOf,
  insertAccount,
  insertGroup,
  insertMembership,
  losAngelesTime,
  newBackend,
  newTenant,
} from "./seed.fixture";

const sam = { subject: "auth|sam", email: "sam@example.com", name: "Sam" };
const asOf = losAngelesTime(2026, 3, 20);

describe("access helpers", () => {
  it("reject a caller who is not signed in", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx));

    const signedOut = t.withIdentity({});

    expect(await failureOf(signedOut.query(api.home.screen, { asOf }))).toMatchObject({ code: "NotSignedIn" });
    expect(await failureOf(signedOut.query(api.groups.screen, { groupId, asOf }))).toMatchObject({
      code: "NotSignedIn",
    });
  });

  it("reject a signed-in caller with no account yet", async () => {
    const t = await newBackend();

    const failure = await failureOf(
      t.withIdentity(sam).mutation(api.groups.create, { name: "2nd Street", timeZone: "America/Chicago" }),
    );

    expect(failure).toMatchObject({ code: "NotSignedIn" });
  });

  it("hide a group from someone who is not a member", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      await insertAccount(ctx, sam);
      return await insertGroup(ctx);
    });
    const asSam = t.withIdentity(sam);

    expect(await failureOf(asSam.query(api.groups.screen, { groupId, asOf }))).toMatchObject({
      code: "PermissionDenied",
    });
    expect(await asSam.query(api.home.screen, { asOf })).toEqual({ groups: [], groupSummaries: [] });
  });

  it("let a read-only member view the group but not add tenants", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const groupId = await insertGroup(ctx);
      await insertMembership(ctx, groupId, await insertAccount(ctx, sam));
      return groupId;
    });
    const asSam = t.withIdentity(sam);

    expect(await asSam.query(api.groups.screen, { groupId, asOf })).toMatchObject({ groups: [{ _id: groupId }] });
    expect(await failureOf(asSam.mutation(api.tenants.add, { groupId, ...newTenant }))).toMatchObject({
      code: "PermissionDenied",
    });
  });

  it("let a member with Add & edit add tenants but not archive them", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const groupId = await insertGroup(ctx);
      await insertMembership(ctx, groupId, await insertAccount(ctx, sam), { tenants: "addEdit" });
      return groupId;
    });
    const asSam = t.withIdentity(sam);

    const tenantId = await asSam.mutation(api.tenants.add, { groupId, ...newTenant });

    expect(await failureOf(asSam.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 }))).toMatchObject({
      code: "PermissionDenied",
    });
  });

  it("give the owner full control whatever the stored levels say", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const groupId = await insertGroup(ctx);
      const membership = await ctx.db
        .query("memberships")
        .withIndex("by_group_account", (q) => q.eq("groupId", groupId))
        .unique();
      if (membership !== null) {
        await ctx.db.patch(membership._id, { tenants: "read" });
      }
      return groupId;
    });

    const tenantId = await t.mutation(api.tenants.add, { groupId, ...newTenant });

    expect(await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 })).toBeNull();
  });

  it("stamp edits with the caller's account", async () => {
    const t = await newBackend();
    const { groupId, samId } = await t.run(async (ctx) => {
      const groupId = await insertGroup(ctx);
      const samId = await insertAccount(ctx, sam);
      await insertMembership(ctx, groupId, samId, { tenants: "full" });
      return { groupId, samId };
    });
    const asSam = t.withIdentity(sam);
    const tenantId = await asSam.mutation(api.tenants.add, { groupId, ...newTenant });

    await asSam.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    expect(await t.run((ctx) => ctx.db.get(tenantId))).toMatchObject({ lastEditedBy: samId });
  });
});
