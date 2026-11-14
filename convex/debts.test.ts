import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import {
  debtRow,
  debtRows,
  failureOf,
  losAngelesTime,
  newBackend,
  seedTenant,
  signInAsMember,
  tenantRow,
} from "./seed.fixture";

const MARCH_20 = losAngelesTime(2026, 3, 20);

const lateFee = { amountCents: 7500, date: "2026-03-07", forMonth: "2026-03" };
const custom = { amountCents: 12000, date: "2026-03-10", note: "Replaced lock" };

function lateFeeArgs(tenantId: Id<"tenants">, overrides: Record<string, unknown> = {}) {
  return { tenantId, clientId: "fee-1", ...lateFee, ...overrides };
}

function customArgs(tenantId: Id<"tenants">, overrides: Record<string, unknown> = {}) {
  return { tenantId, clientId: "custom-1", ...custom, ...overrides };
}

async function seedLateFee() {
  const t = await newBackend();
  const tenantId = await seedTenant(t);
  const debtId = await t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId));
  return { t, tenantId, debtId };
}

async function seedCustom() {
  const t = await newBackend();
  const tenantId = await seedTenant(t);
  const debtId = await t.mutation(api.debts.addCustom, customArgs(tenantId));
  return { t, tenantId, debtId };
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: MARCH_20 });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("debts.addLateFee", () => {
  it("saves a late fee at version 1 with its month, note and client id", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const debtId = await t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId, { note: " After grace " }));

    expect(await debtRow(t, debtId)).toMatchObject({
      tenantId,
      kind: "lateFee",
      ...lateFee,
      note: "After grace",
      clientId: "fee-1",
      version: 1,
    });
  });

  it("accepts a date in the future", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const debtId = await t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId, { date: "2026-04-07" }));

    expect(await debtRow(t, debtId)).toMatchObject({ date: "2026-04-07" });
  });

  it.each([
    ["a zero amount", { amountCents: 0 }, "Amount"],
    ["an impossible date", { date: "2026-02-30" }, "Date"],
    ["a month that is not real", { forMonth: "2026-13" }, "For month"],
    ["a note over 2,000 characters", { note: "x".repeat(2001) }, "Notes"],
  ])("rejects %s", async (_name, overrides, field) => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId, overrides)));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field } });
    expect(await debtRows(t, tenantId)).toEqual([]);
  });

  it("asks before adding a second late fee for the same month, naming the first", async () => {
    const { t, tenantId, debtId } = await seedLateFee();

    const [existing] = await debtRows(t, tenantId);

    const failure = await failureOf(t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId, { clientId: "fee-2" })));

    expect(failure).toEqual({
      code: "NeedsConfirmation",
      message: "A late fee for March was already added. Save anyway?",
      details: {
        matches: [
          {
            kind: "lateFee",
            id: debtId,
            date: "2026-03-07",
            amountCents: 7500,
            byName: "Dana",
            byAccountId: existing?.createdBy,
          },
        ],
      },
    });
    expect(await debtRows(t, tenantId)).toHaveLength(1);
  });

  it("saves the second late fee once confirmed", async () => {
    const { t, tenantId } = await seedLateFee();

    await t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId, { clientId: "fee-2", confirmed: true }));

    expect(await debtRows(t, tenantId)).toHaveLength(2);
  });

  it("does not count a cancelled late fee or a fee for another month", async () => {
    const { t, tenantId, debtId } = await seedLateFee();
    await t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: "Mistake" });
    await t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId, { clientId: "fee-2", forMonth: "2026-02" }));

    await t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId, { clientId: "fee-3" }));

    expect(await debtRows(t, tenantId)).toHaveLength(3);
  });

  it("returns the same id and writes nothing when the clientId repeats", async () => {
    const { t, tenantId, debtId } = await seedLateFee();

    const retry = await t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId));

    expect(retry).toBe(debtId);
    expect(await debtRows(t, tenantId)).toHaveLength(1);
  });

  it("rejects a clientId already used for another tenant", async () => {
    const { t, tenantId } = await seedLateFee();
    const other = await t.mutation(api.tenants.add, {
      groupId: (await tenantRow(t, tenantId)).groupId,
      name: "Apt 4",
      moveIn: "2026-01-01",
      amountCents: 100000,
      dueDay: 1,
    });

    const failure = await failureOf(t.mutation(api.debts.addLateFee, lateFeeArgs(other)));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "clientId" } });
  });

  it("refuses an archived tenant", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.debts.addLateFee, lateFeeArgs(tenantId)));

    expect(failure.code).toBe("TenantArchived");
  });
});

describe("debts.addCustom", () => {
  it("saves a custom debt with its note as the entry name", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const debtId = await t.mutation(api.debts.addCustom, customArgs(tenantId));

    const row = await debtRow(t, debtId);
    expect(row).toMatchObject({ kind: "custom", ...custom, clientId: "custom-1", version: 1 });
    expect(row.forMonth).toBeUndefined();
  });

  it.each([
    ["a missing note", { note: "  " }, "Note"],
    ["a note over 100 characters", { note: "x".repeat(101) }, "Note"],
    ["a zero amount", { amountCents: 0 }, "Amount"],
    ["an impossible date", { date: "2026-02-30" }, "Date"],
  ])("rejects %s", async (_name, overrides, field) => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.mutation(api.debts.addCustom, customArgs(tenantId, overrides)));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field } });
    expect(await debtRows(t, tenantId)).toEqual([]);
  });

  it("returns the same id and writes nothing when the clientId repeats", async () => {
    const { t, tenantId, debtId } = await seedCustom();

    const retry = await t.mutation(api.debts.addCustom, customArgs(tenantId));

    expect(retry).toBe(debtId);
    expect(await debtRows(t, tenantId)).toHaveLength(1);
  });

  it("never asks about duplicates", async () => {
    const { t, tenantId } = await seedCustom();

    await t.mutation(api.debts.addCustom, customArgs(tenantId, { clientId: "custom-2" }));

    expect(await debtRows(t, tenantId)).toHaveLength(2);
  });

  it("refuses an archived tenant", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.debts.addCustom, customArgs(tenantId)));

    expect(failure.code).toBe("TenantArchived");
  });
});

describe("debts.edit", () => {
  it("changes a late fee in place, stamps the editor and bumps the version", async () => {
    const { t, debtId } = await seedLateFee();

    await t.mutation(api.debts.edit, {
      debtId,
      expectedVersion: 1,
      amountCents: 9000,
      date: "2026-03-08",
      forMonth: "2026-02",
      note: "Adjusted",
    });

    expect(await debtRow(t, debtId)).toMatchObject({
      kind: "lateFee",
      amountCents: 9000,
      date: "2026-03-08",
      forMonth: "2026-02",
      note: "Adjusted",
      version: 2,
      lastEditedAt: expect.any(Number),
    });
  });

  it("changes a custom debt and keeps its kind", async () => {
    const { t, debtId } = await seedCustom();

    await t.mutation(api.debts.edit, {
      debtId,
      expectedVersion: 1,
      amountCents: 100,
      date: "2026-03-11",
      note: "Lock",
    });

    expect(await debtRow(t, debtId)).toMatchObject({ kind: "custom", amountCents: 100, note: "Lock", version: 2 });
  });

  it("requires a month for a late fee", async () => {
    const { t, debtId } = await seedLateFee();

    const failure = await failureOf(
      t.mutation(api.debts.edit, { debtId, expectedVersion: 1, amountCents: 100, date: "2026-03-08" }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "For month" } });
  });

  it("requires a note for a custom debt", async () => {
    const { t, debtId } = await seedCustom();

    const failure = await failureOf(
      t.mutation(api.debts.edit, { debtId, expectedVersion: 1, amountCents: 100, date: "2026-03-08" }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "Note" } });
  });

  it("rejects a month on a custom debt", async () => {
    const { t, debtId } = await seedCustom();

    const failure = await failureOf(
      t.mutation(api.debts.edit, {
        debtId,
        expectedVersion: 1,
        amountCents: 100,
        date: "2026-03-08",
        note: "Lock",
        forMonth: "2026-03",
      }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "For month" } });
  });

  it("refuses a cancelled charge", async () => {
    const { t, debtId } = await seedLateFee();
    await t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: "Mistake" });

    const failure = await failureOf(
      t.mutation(api.debts.edit, {
        debtId,
        expectedVersion: 2,
        amountCents: 100,
        date: "2026-03-08",
        forMonth: "2026-03",
      }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "This charge was cancelled. Restore it to change it.",
    });
  });

  it("names the person who changed it first when the version is stale", async () => {
    const { t, tenantId, debtId } = await seedLateFee();
    const asSam = await signInAsMember(t, tenantId, { debts: "addEdit", editOthers: true });
    await asSam.mutation(api.debts.edit, {
      debtId,
      expectedVersion: 1,
      amountCents: 100,
      date: "2026-03-08",
      forMonth: "2026-03",
    });

    const failure = await failureOf(
      t.mutation(api.debts.edit, {
        debtId,
        expectedVersion: 1,
        amountCents: 200,
        date: "2026-03-08",
        forMonth: "2026-03",
      }),
    );

    expect(failure).toMatchObject({
      code: "EditConflict",
      message: "Sam changed this charge while you were editing.",
      details: { currentVersion: 2, lastEditedByName: "Sam" },
    });
  });

  it("refuses an archived tenant", async () => {
    const { t, tenantId, debtId } = await seedLateFee();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(
      t.mutation(api.debts.edit, {
        debtId,
        expectedVersion: 1,
        amountCents: 100,
        date: "2026-03-08",
        forMonth: "2026-03",
      }),
    );

    expect(failure.code).toBe("TenantArchived");
  });
});

describe("debts.delete and debts.restore", () => {
  it("cancels the charge with who, when and why, and keeps the row", async () => {
    const { t, debtId } = await seedCustom();

    await t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: " Tenant disputed it " });

    expect(await debtRow(t, debtId)).toMatchObject({
      deletedAt: expect.any(Number),
      deleteReason: "Tenant disputed it",
      version: 2,
    });
  });

  it.each([
    ["no reason", "  ", "Reason is required."],
    ["a reason over 100 characters", "x".repeat(101), "Reason must be 100 characters or fewer."],
  ])("rejects %s", async (_name, reason, message) => {
    const { t, debtId } = await seedCustom();

    const failure = await failureOf(t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message });
  });

  it("rejects a charge that is already cancelled", async () => {
    const { t, debtId } = await seedCustom();
    await t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: "Mistake" });

    const failure = await failureOf(t.mutation(api.debts.delete, { debtId, expectedVersion: 2, reason: "Again" }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This charge is already cancelled." });
  });

  it("restores a cancelled charge", async () => {
    const { t, debtId } = await seedCustom();
    await t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: "Mistake" });

    await t.mutation(api.debts.restore, { debtId, expectedVersion: 2 });

    const row = await debtRow(t, debtId);
    expect(row.version).toBe(3);
    expect([row.deletedAt, row.deletedBy, row.deleteReason]).toEqual([undefined, undefined, undefined]);
  });

  it("rejects restoring a charge that is not cancelled", async () => {
    const { t, debtId } = await seedCustom();

    const failure = await failureOf(t.mutation(api.debts.restore, { debtId, expectedVersion: 1 }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This charge is not cancelled." });
  });

  it("names who cancelled it when restoring with a stale version", async () => {
    const { t, tenantId, debtId } = await seedCustom();
    const asSam = await signInAsMember(t, tenantId, { debts: "addEdit", editOthers: true });
    await asSam.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: "Mistake" });

    const failure = await failureOf(t.mutation(api.debts.restore, { debtId, expectedVersion: 1 }));

    expect(failure).toMatchObject({ code: "EditConflict", details: { lastEditedByName: "Sam" } });
  });

  it.each(["delete", "restore"] as const)("%s refuses an archived tenant", async (action) => {
    const { t, tenantId, debtId } = await seedCustom();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const call =
      action === "delete"
        ? t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: "Mistake" })
        : t.mutation(api.debts.restore, { debtId, expectedVersion: 1 });

    expect((await failureOf(call)).code).toBe("TenantArchived");
  });
});
