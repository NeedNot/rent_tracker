import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  api,
  failureOf,
  firstTermRow,
  losAngelesTime,
  newBackend,
  RENT,
  seedTenant,
  termRow,
  termRows,
} from "./tenantSetup.fixture";

const MARCH_20 = losAngelesTime(2026, 3, 20);
const RAISED_RENT = 160000;
const raiseFromJune = { startMonth: "2026-06", amountCents: RAISED_RENT, dueDay: 5 };

async function seedTenantWithRaise() {
  const t = newBackend();
  const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });
  const rentTermId = await t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune });
  const firstTerm = await firstTermRow(t, tenantId);
  return { t, tenantId, rentTermId, firstTermId: firstTerm._id };
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: MARCH_20 });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("rentTerms.add", () => {
  it("adds a rent term at version 1 for the chosen month", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });

    const rentTermId = await t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune });

    expect(await termRow(t, rentTermId)).toMatchObject({ tenantId, ...raiseFromJune, version: 1 });
  });

  it("accepts a start month that has already passed", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });

    const rentTermId = await t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, startMonth: "2026-02" });

    expect(await termRow(t, rentTermId)).toMatchObject({ startMonth: "2026-02" });
  });

  it("blocks a start month before the move-in month", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-17" });

    const failure = await failureOf(
      t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, startMonth: "2025-12" }),
    );

    expect(failure).toEqual({
      code: "ValidationFailed",
      message: "A rent change can't start before the move-in month. Edit the existing rent term instead.",
      details: { field: "Start month" },
    });
    expect(await termRows(t, tenantId)).toHaveLength(1);
  });

  it("blocks a second term in the move-in month and points at the first term", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-17" });
    const firstTerm = await firstTermRow(t, tenantId);

    const failure = await failureOf(
      t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, startMonth: "2026-01" }),
    );

    expect(failure).toEqual({
      code: "ValidationFailed",
      message: "A rent term already starts in this month. Edit that rent term instead.",
      details: { field: "Start month", rentTermId: firstTerm._id },
    });
    expect(await termRows(t, tenantId)).toHaveLength(1);
  });

  it("blocks a start month another active term already uses", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();

    const failure = await failureOf(t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, dueDay: 10 }));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { rentTermId } });
    expect(await termRows(t, tenantId)).toHaveLength(2);
  });

  it("allows a start month whose only other term was deleted", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" });

    const replacementId = await t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, amountCents: 170000 });

    expect(await termRow(t, replacementId)).toMatchObject({ startMonth: "2026-06", amountCents: 170000 });
    expect(await termRows(t, tenantId)).toHaveLength(3);
  });

  it.each([
    ["a month that does not exist", { startMonth: "2026-13" }, "Start month must be a real month."],
    ["a full date as the month", { startMonth: "2026-06-01" }, "Start month must be a real month."],
    ["rent of $0.00", { amountCents: 0 }, "Rent must be between $0.01 and $1,000,000.00."],
    ["rent over $1,000,000.00", { amountCents: 100_000_001 }, "Rent must be between $0.01 and $1,000,000.00."],
    ["due day 0", { dueDay: 0 }, "Due day must be between 1 and 31."],
    ["due day 32", { dueDay: 32 }, "Due day must be between 1 and 31."],
  ])("rejects %s", async (_rule, fields, message) => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, ...fields }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message });
    expect(await termRows(t, tenantId)).toHaveLength(1);
  });

  it("fails with TenantArchived for an archived tenant", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune }));

    expect(failure.code).toBe("TenantArchived");
    expect(await termRows(t, tenantId)).toHaveLength(1);
  });

  it("fails with NotFound when the tenant is missing", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => ctx.db.delete(tenantId));

    const failure = await failureOf(t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune }));

    expect(failure.code).toBe("NotFound");
  });
});

describe("rentTerms.edit", () => {
  it("changes the term, raises the version and records who edited", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();

    await t.mutation(api.rentTerms.edit, {
      rentTermId,
      expectedVersion: 1,
      startMonth: "2026-07",
      amountCents: 170000,
      dueDay: 15,
    });
    const editor = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(await termRow(t, rentTermId)).toMatchObject({
      startMonth: "2026-07",
      amountCents: 170000,
      dueDay: 15,
      version: 2,
      lastEditedAt: expect.any(Number),
      lastEditedBy: editor?._id,
    });
  });

  it("does not count the term itself as a duplicate start month", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();

    await t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, amountCents: 170000 });

    expect(await termRow(t, rentTermId)).toMatchObject({ startMonth: "2026-06", amountCents: 170000, version: 2 });
  });

  it("rejects a stale version with EditConflict naming who last edited, and saves nothing", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, amountCents: 170000 });
    const editor = await t.run((ctx) => ctx.db.query("accounts").unique());

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, amountCents: 180000 }),
    );

    expect(failure).toEqual({
      code: "EditConflict",
      message: "You changed this rent term while you were editing.",
      details: { currentVersion: 2, lastEditedBy: editor?._id, lastEditedByName: "You" },
    });
    expect(await termRow(t, rentTermId)).toMatchObject({ amountCents: 170000, version: 2 });
  });

  it("blocks moving a term onto another active term's start month", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();
    const septemberTermId = await t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, startMonth: "2026-09" });

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId: septemberTermId, expectedVersion: 1, ...raiseFromJune }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "A rent term already starts in this month. Edit that rent term instead.",
      details: { rentTermId },
    });
    expect(await termRow(t, septemberTermId)).toMatchObject({ startMonth: "2026-09", version: 1 });
  });

  it("allows moving a term onto a month whose only other term was deleted", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();
    const septemberTermId = await t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, startMonth: "2026-09" });
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong month" });

    await t.mutation(api.rentTerms.edit, { rentTermId: septemberTermId, expectedVersion: 1, ...raiseFromJune });

    expect(await termRow(t, septemberTermId)).toMatchObject({ startMonth: "2026-06", version: 2 });
  });

  it("blocks moving a term before the move-in month", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, startMonth: "2025-12" }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "A rent change can't start before the move-in month. Edit the existing rent term instead.",
    });
    expect(await termRow(t, rentTermId)).toMatchObject({ startMonth: "2026-06", version: 1 });
  });

  it("lets the first term change its amount and due day", async () => {
    const { t, firstTermId } = await seedTenantWithRaise();

    await t.mutation(api.rentTerms.edit, {
      rentTermId: firstTermId,
      expectedVersion: 1,
      startMonth: "2026-01",
      amountCents: 140000,
      dueDay: 3,
    });

    expect(await termRow(t, firstTermId)).toMatchObject({ amountCents: 140000, dueDay: 3, version: 2 });
  });

  it("keeps the first term in the move-in month", async () => {
    const { t, firstTermId } = await seedTenantWithRaise();

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, {
        rentTermId: firstTermId,
        expectedVersion: 1,
        startMonth: "2026-03",
        amountCents: RENT,
        dueDay: 1,
      }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "The first rent term starts in the move-in month. Change the move-in date to move it.",
    });
    expect(await termRow(t, firstTermId)).toMatchObject({ startMonth: "2026-01", version: 1 });
  });

  it.each([
    ["a month that does not exist", { startMonth: "2026-00" }],
    ["rent of $0.00", { amountCents: 0 }],
    ["due day 32", { dueDay: 32 }],
  ])("rejects %s", async (_rule, fields) => {
    const { t, rentTermId } = await seedTenantWithRaise();

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, ...fields }),
    );

    expect(failure.code).toBe("ValidationFailed");
    expect(await termRow(t, rentTermId)).toMatchObject({ ...raiseFromJune, version: 1 });
  });

  it("rejects editing a deleted term", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" });

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 2, ...raiseFromJune, amountCents: 170000 }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "This rent term was deleted and can't be changed.",
    });
    expect(await termRow(t, rentTermId)).toMatchObject({ amountCents: RAISED_RENT, version: 2 });
  });

  it("fails with TenantArchived for an archived tenant", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, amountCents: 170000 }),
    );

    expect(failure.code).toBe("TenantArchived");
    expect(await termRow(t, rentTermId)).toMatchObject({ amountCents: RAISED_RENT, version: 1 });
  });

  it("fails with NotFound when the tenant is missing", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();
    await t.run((ctx) => ctx.db.delete(tenantId));

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune }),
    );

    expect(failure.code).toBe("NotFound");
  });
});

describe("rentTerms.delete", () => {
  it("keeps the row and marks it deleted with who, when and why", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();

    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "  Entered by mistake " });
    const deleter = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(await termRows(t, tenantId)).toHaveLength(2);
    expect(await termRow(t, rentTermId)).toMatchObject({
      ...raiseFromJune,
      deletedAt: expect.any(Number),
      deletedBy: deleter?._id,
      deleteReason: "Entered by mistake",
      version: 2,
    });
  });

  it("stops the deleted term from changing the rent", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });
    const rentTermId = await t.mutation(api.rentTerms.add, { tenantId, ...raiseFromJune, startMonth: "2026-02" });

    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Entered by mistake" });
    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_20 });

    expect(screen.tenantSummaries).toMatchObject([{ rentOwedCents: 3 * RENT }]);
  });

  it.each([
    ["a blank reason", "   ", "Reason is required."],
    ["a 101-character reason", "x".repeat(101), "Reason must be 100 characters or fewer."],
  ])("rejects %s", async (_rule, reason, message) => {
    const { t, rentTermId } = await seedTenantWithRaise();

    const failure = await failureOf(t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message });
    expect(await termRow(t, rentTermId)).not.toHaveProperty("deletedAt");
  });

  it("accepts a 100-character reason", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();

    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "x".repeat(100) });

    expect(await termRow(t, rentTermId)).toMatchObject({ deleteReason: "x".repeat(100) });
  });

  it("never deletes the tenant's first term", async () => {
    const { t, firstTermId } = await seedTenantWithRaise();

    const failure = await failureOf(
      t.mutation(api.rentTerms.delete, { rentTermId: firstTermId, expectedVersion: 1, reason: "Start over" }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "A tenant's first rent term can't be deleted. Edit it instead.",
    });
    expect(await termRow(t, firstTermId)).not.toHaveProperty("deletedAt");
  });

  it("never deletes the first term even when it is the only one left", async () => {
    const { t, rentTermId, firstTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" });

    const failure = await failureOf(
      t.mutation(api.rentTerms.delete, { rentTermId: firstTermId, expectedVersion: 1, reason: "Start over" }),
    );

    expect(failure.code).toBe("ValidationFailed");
    expect(await termRow(t, firstTermId)).not.toHaveProperty("deletedAt");
  });

  it("rejects a stale version with EditConflict and keeps the term", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, amountCents: 170000 });

    const failure = await failureOf(
      t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" }),
    );

    expect(failure).toMatchObject({ code: "EditConflict", details: { currentVersion: 2, lastEditedByName: "You" } });
    expect(await termRow(t, rentTermId)).not.toHaveProperty("deletedAt");
  });

  it("names who deleted the term when a stale form saves after the delete", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" });
    const deleter = await t.run((ctx) => ctx.db.query("accounts").unique());

    const failure = await failureOf(
      t.mutation(api.rentTerms.edit, { rentTermId, expectedVersion: 1, ...raiseFromJune, amountCents: 170000 }),
    );

    expect(failure).toMatchObject({ code: "EditConflict", details: { currentVersion: 2, lastEditedBy: deleter?._id } });
  });

  it("rejects deleting a term that is already deleted", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" });

    const failure = await failureOf(
      t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 2, reason: "Again" }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "This rent term was deleted and can't be changed.",
    });
    expect(await termRow(t, rentTermId)).toMatchObject({ deleteReason: "Wrong amount", version: 2 });
  });

  it("fails with TenantArchived for an archived tenant", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(
      t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" }),
    );

    expect(failure.code).toBe("TenantArchived");
    expect(await termRow(t, rentTermId)).not.toHaveProperty("deletedAt");
  });
});

describe("rentTerms.preview", () => {
  it("shows the balance before and after a rent change that starts in the past", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });

    const preview = await t.query(api.rentTerms.preview, {
      tenantId,
      asOf: MARCH_20,
      proposed: { startMonth: "2026-02", amountCents: RAISED_RENT, dueDay: 1 },
    });

    expect(preview.before).toMatchObject({ tenantId, rentOwedCents: 3 * RENT, state: "late" });
    expect(preview.after).toMatchObject({ tenantId, rentOwedCents: RENT + 2 * RAISED_RENT, state: "late" });
  });

  it("shows the balance before and after editing an existing term", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });
    const firstTerm = await firstTermRow(t, tenantId);

    const preview = await t.query(api.rentTerms.preview, {
      tenantId,
      asOf: MARCH_20,
      proposed: { rentTermId: firstTerm._id, startMonth: "2026-01", amountCents: 100000, dueDay: 1 },
    });

    expect(preview.before).toMatchObject({ rentOwedCents: 3 * RENT });
    expect(preview.after).toMatchObject({ rentOwedCents: 300000 });
  });

  it("leaves the balance unchanged for a rent change that starts in the future", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });

    const preview = await t.query(api.rentTerms.preview, { tenantId, asOf: MARCH_20, proposed: raiseFromJune });

    expect(preview.after).toEqual(preview.before);
  });

  it("shows a later due day pushing the next due date out", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });

    const preview = await t.query(api.rentTerms.preview, {
      tenantId,
      asOf: MARCH_20,
      proposed: { startMonth: "2026-04", amountCents: RENT, dueDay: 15 },
    });

    expect(preview.before.nextDueDate).toBe("2026-04-01");
    expect(preview.after.nextDueDate).toBe("2026-04-15");
  });

  it("writes nothing", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });
    const firstTerm = await firstTermRow(t, tenantId);
    const termsBefore = await termRows(t, tenantId);

    await t.query(api.rentTerms.preview, {
      tenantId,
      asOf: MARCH_20,
      proposed: { rentTermId: firstTerm._id, startMonth: "2026-01", amountCents: 100000, dueDay: 1 },
    });

    expect(await termRows(t, tenantId)).toEqual(termsBefore);
  });

  it("fails with NotFound when the term belongs to another tenant", async () => {
    const { t, rentTermId } = await seedTenantWithRaise();
    const otherTenantId = await seedTenant(t);

    const failure = await failureOf(
      t.query(api.rentTerms.preview, {
        tenantId: otherTenantId,
        asOf: MARCH_20,
        proposed: { rentTermId, ...raiseFromJune },
      }),
    );

    expect(failure.code).toBe("NotFound");
  });

  it("rejects previewing a change to a deleted term", async () => {
    const { t, tenantId, rentTermId } = await seedTenantWithRaise();
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Wrong amount" });

    const failure = await failureOf(
      t.query(api.rentTerms.preview, { tenantId, asOf: MARCH_20, proposed: { rentTermId, ...raiseFromJune } }),
    );

    expect(failure.code).toBe("ValidationFailed");
  });

  it.each([
    ["a month that does not exist", { startMonth: "2026-13" }],
    ["rent of $0.00", { amountCents: 0 }],
    ["due day 32", { dueDay: 32 }],
  ])("rejects %s", async (_rule, fields) => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(
      t.query(api.rentTerms.preview, { tenantId, asOf: MARCH_20, proposed: { ...raiseFromJune, ...fields } }),
    );

    expect(failure.code).toBe("ValidationFailed");
  });

  it("fails with NotFound when the tenant is missing", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => ctx.db.delete(tenantId));

    const failure = await failureOf(
      t.query(api.rentTerms.preview, { tenantId, asOf: MARCH_20, proposed: raiseFromJune }),
    );

    expect(failure.code).toBe("NotFound");
  });
});
