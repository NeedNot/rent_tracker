import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { internal } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import {
  type Backend,
  debtRow,
  failureOf,
  insertAccount,
  insertDebt,
  insertGroup,
  insertPayment,
  insertRentTerm,
  insertTenantOwingRent,
  newBackend,
  owner,
  paymentRow,
  termRow,
} from "./seed.fixture";

interface TenantScene {
  t: Backend;
  groupId: Id<"groups">;
  tenantId: Id<"tenants">;
  cancelled: { deletedAt: number; deletedBy: Id<"accounts">; deleteReason: string; version: number };
}

type RestorableKind = "payment" | "debt" | "rentTerm";

const NOW = Date.UTC(2026, 10, 14);
const DAY = 24 * 60 * 60 * 1000;
const RENT = 150000;

async function tenantScene(): Promise<TenantScene> {
  const t = await newBackend();
  const { groupId, tenantId, ownerId } = await t.run(async (ctx) => {
    const groupId = await insertGroup(ctx);
    const tenantId = await insertTenantOwingRent(ctx, groupId, RENT);
    return { groupId, tenantId, ownerId: await insertAccount(ctx, owner) };
  });
  return {
    t,
    groupId,
    tenantId,
    cancelled: { deletedAt: NOW - DAY, deletedBy: ownerId, deleteReason: "Typo", version: 2 },
  };
}

async function insertEntry(
  ctx: MutationCtx,
  kind: RestorableKind,
  scene: TenantScene,
  fields: object,
): Promise<string> {
  switch (kind) {
    case "payment":
      return await insertPayment(ctx, scene.tenantId, { amountCents: RENT, dateReceived: "2026-04-01" }, fields);
    case "debt":
      return await insertDebt(ctx, scene.tenantId, { kind: "lateFee", amountCents: 5000, date: "2026-04-08" }, fields);
    case "rentTerm": {
      const termId = await insertRentTerm(ctx, scene.tenantId, { startMonth: "2026-06", amountCents: RENT, dueDay: 1 });
      await ctx.db.patch(termId, fields);
      return termId;
    }
  }
}

async function restore(t: Backend, kind: RestorableKind, id: string): Promise<null> {
  return await t.mutation(internal.support.restoreEntry, { kind, id });
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(NOW);
});

afterEach(() => {
  vi.useRealTimers();
});

describe("support.restoreEntry", () => {
  it("restores a cancelled payment and raises its version", async () => {
    const scene = await tenantScene();
    const paymentId = await scene.t.run((ctx) =>
      insertPayment(ctx, scene.tenantId, { amountCents: RENT, dateReceived: "2026-04-01" }, scene.cancelled),
    );

    await restore(scene.t, "payment", paymentId);

    const row = await paymentRow(scene.t, paymentId);
    expect([row.deletedAt, row.deletedBy, row.deleteReason, row.version]).toEqual([undefined, undefined, undefined, 3]);
  });

  it("brings a cancelled bounced payment back as bounced", async () => {
    const scene = await tenantScene();
    const bounced = { bouncedAt: NOW - 2 * DAY, bouncedBy: scene.cancelled.deletedBy };
    const paymentId = await scene.t.run((ctx) =>
      insertPayment(
        ctx,
        scene.tenantId,
        { amountCents: RENT, dateReceived: "2026-04-01" },
        {
          ...scene.cancelled,
          ...bounced,
        },
      ),
    );

    await restore(scene.t, "payment", paymentId);

    const row = await paymentRow(scene.t, paymentId);
    expect(row).toMatchObject({ ...bounced, version: 3 });
    expect(row.deletedAt).toBeUndefined();
  });

  it("restores a cancelled charge and raises its version", async () => {
    const scene = await tenantScene();
    const debtId = await scene.t.run((ctx) =>
      insertDebt(ctx, scene.tenantId, { kind: "custom", amountCents: 5000, date: "2026-04-08" }, scene.cancelled),
    );

    await restore(scene.t, "debt", debtId);

    const row = await debtRow(scene.t, debtId);
    expect([row.deletedAt, row.deletedBy, row.deleteReason, row.version]).toEqual([undefined, undefined, undefined, 3]);
  });

  it("restores a deleted rent term and raises its version", async () => {
    const scene = await tenantScene();
    const termId = await scene.t.run(async (ctx) => {
      const termId = await insertRentTerm(ctx, scene.tenantId, { startMonth: "2026-06", amountCents: RENT, dueDay: 1 });
      await ctx.db.patch(termId, scene.cancelled);
      return termId;
    });

    await restore(scene.t, "rentTerm", termId);

    const row = await termRow(scene.t, termId);
    expect([row.deletedAt, row.deletedBy, row.deleteReason, row.version]).toEqual([undefined, undefined, undefined, 3]);
  });

  it.each([
    ["payment", "This payment is not cancelled."],
    ["debt", "This charge is not cancelled."],
    ["rentTerm", "This rent term is not cancelled."],
  ] as const)("refuses a %s that is not cancelled", async (kind, message) => {
    const scene = await tenantScene();
    const id = await scene.t.run((ctx) => insertEntry(ctx, kind, scene, {}));

    expect(await failureOf(restore(scene.t, kind, id))).toMatchObject({ code: "ValidationFailed", message });
  });

  it("refuses a bounced payment that was never cancelled", async () => {
    const scene = await tenantScene();
    const id = await scene.t.run((ctx) =>
      insertEntry(ctx, "payment", scene, { bouncedAt: NOW, bouncedBy: scene.cancelled.deletedBy }),
    );

    expect(await failureOf(restore(scene.t, "payment", id))).toMatchObject({ code: "ValidationFailed" });
  });

  it.each([
    ["payment", "This payment no longer exists."],
    ["debt", "This charge no longer exists."],
    ["rentTerm", "This rent term no longer exists."],
  ] as const)("is NotFound for a %s id that names no entry of that kind", async (kind, message) => {
    const scene = await tenantScene();

    expect(await failureOf(restore(scene.t, kind, scene.tenantId))).toEqual({ code: "NotFound", message, details: {} });
    expect(await failureOf(restore(scene.t, kind, "not-an-id"))).toMatchObject({ code: "NotFound", message });
  });

  it("refuses an entry of an archived tenant", async () => {
    const scene = await tenantScene();
    const id = await scene.t.run(async (ctx) => {
      await ctx.db.patch(scene.tenantId, { status: "archived" });
      return insertEntry(ctx, "debt", scene, scene.cancelled);
    });

    expect(await failureOf(restore(scene.t, "debt", id))).toMatchObject({ code: "TenantArchived" });
  });

  it("is NotFound for an entry in an archived group", async () => {
    const scene = await tenantScene();
    const id = await scene.t.run(async (ctx) => {
      await ctx.db.patch(scene.groupId, { archivedAt: NOW - DAY });
      return insertEntry(ctx, "payment", scene, scene.cancelled);
    });

    expect(await failureOf(restore(scene.t, "payment", id))).toMatchObject({
      code: "NotFound",
      message: "This group no longer exists.",
    });
  });

  it("refuses a rent term whose start month another active term now uses", async () => {
    const scene = await tenantScene();
    const id = await scene.t.run(async (ctx) => {
      const termId = await insertEntry(ctx, "rentTerm", scene, scene.cancelled);
      await insertRentTerm(ctx, scene.tenantId, { startMonth: "2026-06", amountCents: RENT + 5000, dueDay: 1 });
      return termId;
    });

    expect(await failureOf(restore(scene.t, "rentTerm", id))).toMatchObject({
      code: "ValidationFailed",
      message: "A rent term already starts in this month. Edit that rent term instead.",
    });
  });
});
