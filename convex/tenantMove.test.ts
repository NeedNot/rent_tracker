import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import {
  type Backend,
  debtRows,
  failureOf,
  insertAccount,
  insertGroup,
  insertMembership,
  insertTenantOwingRent,
  losAngelesTime,
  newBackend,
  paymentRows,
  RENT,
  sam,
  tenantRow,
} from "./seed.fixture";

const MARCH_3 = losAngelesTime(2026, 3, 3);

interface Setup {
  t: Backend;
  tenantId: Id<"tenants">;
  fromGroupId: Id<"groups">;
  toGroupId: Id<"groups">;
}

async function setup(): Promise<Setup> {
  const t = await newBackend();
  const fromGroupId = await t.run((ctx) => insertGroup(ctx, { name: "From", graceDays: 5 }));
  const toGroupId = await t.run((ctx) => insertGroup(ctx, { name: "To", graceDays: 0 }));
  const tenantId = await t.run((ctx) => insertTenantOwingRent(ctx, fromGroupId, RENT));
  return { t, tenantId, fromGroupId, toGroupId };
}

async function signInAsSam(t: Backend, grants: Record<Id<"groups">, "read" | "addEdit" | "full">): Promise<Backend> {
  await t.run(async (ctx) => {
    const accountId = await insertAccount(ctx, sam);
    for (const [groupId, tenants] of Object.entries(grants)) {
      await insertMembership(ctx, groupId as Id<"groups">, accountId, { tenants });
    }
  });
  return t.withIdentity(sam);
}

describe("tenants.move", () => {
  it("lets the owner of both groups move a tenant and bumps its version", async () => {
    const { t, tenantId, toGroupId } = await setup();

    await t.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 });

    expect(await tenantRow(t, tenantId)).toMatchObject({ groupId: toGroupId, version: 2, status: "active" });
  });

  it("lets a member with Tenants full in both groups move a tenant", async () => {
    const { t, tenantId, fromGroupId, toGroupId } = await setup();
    const asSam = await signInAsSam(t, { [fromGroupId]: "full", [toGroupId]: "full" });

    await asSam.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 });

    expect(await tenantRow(t, tenantId)).toMatchObject({ groupId: toGroupId });
  });

  it("moves an archived tenant and keeps it archived", async () => {
    const { t, tenantId, toGroupId } = await setup();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    await t.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 2 });

    expect(await tenantRow(t, tenantId)).toMatchObject({ groupId: toGroupId, status: "archived", version: 3 });
  });

  it.each([
    ["Tenants full only in the source group", "source"],
    ["Tenants full only in the target group", "target"],
  ])("denies a member with %s", async (_case, where) => {
    const { t, tenantId, fromGroupId, toGroupId } = await setup();
    const asSam = await signInAsSam(t, {
      [fromGroupId]: where === "source" ? "full" : "read",
      [toGroupId]: where === "target" ? "full" : "read",
    });

    const failure = await failureOf(asSam.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 }));

    expect(failure.code).toBe("PermissionDenied");
    expect((await tenantRow(t, tenantId)).groupId).toBe(fromGroupId);
  });

  it("denies a member with only add/edit in both groups", async () => {
    const { t, tenantId, fromGroupId, toGroupId } = await setup();
    const asSam = await signInAsSam(t, { [fromGroupId]: "addEdit", [toGroupId]: "addEdit" });

    const failure = await failureOf(asSam.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 }));

    expect(failure.code).toBe("PermissionDenied");
  });

  it("denies a member who is not in the target group", async () => {
    const { t, tenantId, fromGroupId, toGroupId } = await setup();
    const asSam = await signInAsSam(t, { [fromGroupId]: "full" });

    const failure = await failureOf(asSam.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 }));

    expect(failure.code).toBe("PermissionDenied");
  });

  it("rejects moving a tenant to its own group", async () => {
    const { t, tenantId, fromGroupId } = await setup();

    const failure = await failureOf(
      t.mutation(api.tenants.move, { tenantId, toGroupId: fromGroupId, expectedVersion: 1 }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "toGroupId" } });
  });

  it("rejects moving into an archived group", async () => {
    const { t, tenantId, toGroupId } = await setup();
    await t.run((ctx) => ctx.db.patch(toGroupId, { archivedAt: Date.now() }));

    const failure = await failureOf(t.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 }));

    expect(failure.code).toBe("NotFound");
  });

  it("reports an edit conflict for a stale version", async () => {
    const { t, tenantId, toGroupId } = await setup();

    const failure = await failureOf(t.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 7 }));

    expect(failure).toMatchObject({ code: "EditConflict", details: { currentVersion: 1 } });
  });

  it("brings rent terms, payments and debts along", async () => {
    const { t, tenantId, toGroupId } = await setup();
    await t.mutation(api.payments.log, {
      tenantId,
      clientId: "p1",
      amountCents: RENT,
      dateReceived: "2026-03-02",
    });
    await t.mutation(api.debts.addCustom, {
      tenantId,
      clientId: "d1",
      amountCents: 2000,
      date: "2026-03-02",
      note: "Key",
    });

    await t.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_3 });
    expect(screen.groups).toMatchObject([{ _id: toGroupId }]);
    expect(screen.rentTerms).toHaveLength(1);
    expect(screen.payments).toHaveLength(1);
    expect(screen.debts).toHaveLength(1);
    expect(await paymentRows(t, tenantId)).toHaveLength(1);
    expect(await debtRows(t, tenantId)).toHaveLength(1);
  });

  it("recalculates balances under the new group's settings", async () => {
    const { t, tenantId, toGroupId } = await setup();
    const before = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_3 });

    await t.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 });
    const after = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_3 });

    expect(before.tenantSummaries[0]).toMatchObject({ state: "owes", rentOwedCents: RENT });
    expect(after.tenantSummaries[0]).toMatchObject({ state: "late", rentOwedCents: RENT });
  });

  it("stops a member of only the old group from seeing the tenant", async () => {
    const { t, tenantId, fromGroupId, toGroupId } = await setup();
    const asSam = await signInAsSam(t, { [fromGroupId]: "full" });
    await t.mutation(api.tenants.move, { tenantId, toGroupId, expectedVersion: 1 });

    const failure = await failureOf(asSam.query(api.tenants.screen, { tenantId, asOf: MARCH_3 }));

    expect(failure.code).toBe("PermissionDenied");
  });
});

describe("tenants.moveTargets", () => {
  it("lists active groups where the caller has Tenants full or owns, except the current one", async () => {
    const { t, tenantId, fromGroupId, toGroupId } = await setup();
    const readOnlyGroupId = await t.run((ctx) => insertGroup(ctx, { name: "Read only" }));
    const archivedGroupId = await t.run((ctx) => insertGroup(ctx, { name: "Archived" }));
    const fullGroupId = await t.run((ctx) => insertGroup(ctx, { name: "Full" }));
    await t.run((ctx) => ctx.db.patch(archivedGroupId, { archivedAt: Date.now() }));
    const asSam = await signInAsSam(t, {
      [fromGroupId]: "full",
      [readOnlyGroupId]: "addEdit",
      [archivedGroupId]: "full",
      [fullGroupId]: "full",
    });

    const targets = await asSam.query(api.tenants.moveTargets, { tenantId, asOf: MARCH_3 });

    expect(targets.groups.map((group) => group._id)).toEqual([fullGroupId]);
    const ownerTargets = await t.query(api.tenants.moveTargets, { tenantId, asOf: MARCH_3 });
    expect(ownerTargets.groups.map((group) => group._id).sort()).toEqual(
      [toGroupId, readOnlyGroupId, fullGroupId].sort(),
    );
  });

  it("summarizes each target group and carries its payment order and grace days", async () => {
    const { t, tenantId, toGroupId } = await setup();
    await t.run((ctx) => insertTenantOwingRent(ctx, toGroupId, RENT));

    const targets = await t.query(api.tenants.moveTargets, { tenantId, asOf: MARCH_3 });

    expect(targets.groups).toMatchObject([{ _id: toGroupId, paymentOrder: "rentFirst", graceDays: 0 }]);
    expect(targets.groupSummaries).toMatchObject([{ groupId: toGroupId, activeTenantCount: 1, totalOwedCents: RENT }]);
  });

  it("requires Tenants full in the tenant's group", async () => {
    const { t, tenantId, fromGroupId } = await setup();
    const asSam = await signInAsSam(t, { [fromGroupId]: "addEdit" });

    const failure = await failureOf(asSam.query(api.tenants.moveTargets, { tenantId, asOf: MARCH_3 }));

    expect(failure.code).toBe("PermissionDenied");
  });
});
