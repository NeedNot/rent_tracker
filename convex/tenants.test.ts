import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import {
  failureOf,
  losAngelesTime,
  newBackend,
  newTenant,
  RENT,
  seedGroup,
  seedTenant,
  tenantRow,
  termRows,
} from "./seed.fixture";

const everyDetail = {
  name: "Apt 3B",
  contactName: "Sam Lee",
  contactPhone: "555-0100",
  contactEmail: "sam@example.com",
  notes: "Two roommates",
  depositCents: 200000,
  moveIn: "2026-01-17",
  moveOut: "2026-12-31",
  graceDaysOverride: 3,
  lateFeeFlatOverrideCents: 7500,
};

describe("tenants.add", () => {
  it("creates an active tenant at version 1 with its details", async () => {
    const t = newBackend();
    const groupId = await seedGroup(t);

    const tenantId = await t.mutation(api.tenants.add, { groupId, ...everyDetail, amountCents: RENT, dueDay: 5 });

    expect(await tenantRow(t, tenantId)).toMatchObject({ groupId, ...everyDetail, status: "active", version: 1 });
  });

  it("creates the first rent term starting in the move-in month", async () => {
    const t = newBackend();
    const groupId = await seedGroup(t);

    const tenantId = await t.mutation(api.tenants.add, { groupId, ...everyDetail, amountCents: RENT, dueDay: 5 });

    expect(await termRows(t, tenantId)).toMatchObject([
      { tenantId, startMonth: "2026-01", amountCents: RENT, dueDay: 5, version: 1 },
    ]);
  });

  it("trims text and leaves blank optional details unset", async () => {
    const t = newBackend();
    const groupId = await seedGroup(t);

    const tenantId = await t.mutation(api.tenants.add, { groupId, ...newTenant, name: "  Apt 3B ", notes: "   " });
    const tenant = await tenantRow(t, tenantId);

    expect(tenant.name).toBe("Apt 3B");
    expect(tenant).not.toHaveProperty("notes");
  });

  it.each([
    ["a 100-character name", { name: "x".repeat(100) }],
    ["2,000 characters of notes", { notes: "x".repeat(2000) }],
    ["a $0.00 deposit", { depositCents: 0 }],
    ["a last day equal to the move-in date", { moveOut: "2026-01-01" }],
    ["no grace days", { graceDaysOverride: 0 }],
    ["rent of $0.01", { amountCents: 1 }],
    ["rent of $1,000,000.00", { amountCents: 100_000_000 }],
    ["due day 31", { dueDay: 31 }],
  ])("accepts %s", async (_boundary, fields) => {
    const t = newBackend();
    const groupId = await seedGroup(t);

    const tenantId = await t.mutation(api.tenants.add, { groupId, ...newTenant, ...fields });

    expect(await tenantRow(t, tenantId)).toMatchObject({ status: "active" });
  });

  it.each([
    ["a blank name", { name: "  " }, "Name is required."],
    ["a 101-character name", { name: "x".repeat(101) }, "Name must be 100 characters or fewer."],
    ["a 101-character contact name", { contactName: "x".repeat(101) }, "Contact name must be 100 characters or fewer."],
    ["a 101-character phone", { contactPhone: "5".repeat(101) }, "Phone must be 100 characters or fewer."],
    ["a 101-character email", { contactEmail: "x".repeat(101) }, "Email must be 100 characters or fewer."],
    ["2,001 characters of notes", { notes: "x".repeat(2001) }, "Notes must be 2,000 characters or fewer."],
    ["a negative deposit", { depositCents: -1 }, "Security deposit must be between $0.00 and $1,000,000.00."],
    ["a fractional deposit", { depositCents: 0.5 }, "Security deposit must be between $0.00 and $1,000,000.00."],
    ["a move-in date that does not exist", { moveIn: "2026-02-30" }, "Move-in date must be a real date."],
    ["a last day that does not exist", { moveOut: "2026-13-01" }, "Last day in unit must be a real date."],
    [
      "a last day before the move-in date",
      { moveOut: "2025-12-31" },
      "Last day in unit must be on or after the move-in date.",
    ],
    ["negative grace days", { graceDaysOverride: -1 }, "Grace period must be a whole number of days, 0 or more."],
    [
      "a $0.00 late fee override",
      { lateFeeFlatOverrideCents: 0 },
      "Late fee amount must be between $0.01 and $1,000,000.00.",
    ],
    ["rent of $0.00", { amountCents: 0 }, "Rent must be between $0.01 and $1,000,000.00."],
    ["rent over $1,000,000.00", { amountCents: 100_000_001 }, "Rent must be between $0.01 and $1,000,000.00."],
    ["fractional cents of rent", { amountCents: 1500.5 }, "Rent must be between $0.01 and $1,000,000.00."],
    ["due day 0", { dueDay: 0 }, "Due day must be between 1 and 31."],
    ["due day 32", { dueDay: 32 }, "Due day must be between 1 and 31."],
  ])("rejects %s and saves nothing", async (_rule, fields, message) => {
    const t = newBackend();
    const groupId = await seedGroup(t);

    const failure = await failureOf(t.mutation(api.tenants.add, { groupId, ...newTenant, ...fields }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message });
    expect(await t.run((ctx) => ctx.db.query("tenants").collect())).toEqual([]);
    expect(await t.run((ctx) => ctx.db.query("rentTerms").collect())).toEqual([]);
  });

  it("fails with NotFound when the group is missing", async () => {
    const t = newBackend();
    const groupId = await seedGroup(t);
    await t.run((ctx) => ctx.db.delete(groupId));

    const failure = await failureOf(t.mutation(api.tenants.add, { groupId, ...newTenant }));

    expect(failure.code).toBe("NotFound");
  });

  it("fails with NotFound when the group is archived", async () => {
    const t = newBackend();
    const groupId = await seedGroup(t, { archivedAt: 1 });

    const failure = await failureOf(t.mutation(api.tenants.add, { groupId, ...newTenant }));

    expect(failure.code).toBe("NotFound");
  });
});

describe("tenants.edit", () => {
  it("overwrites the details, raises the version and records who edited", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    await t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, ...everyDetail, name: "Apt 4C" });
    const tenant = await tenantRow(t, tenantId);
    const editor = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(tenant).toMatchObject({ ...everyDetail, name: "Apt 4C", version: 2, lastEditedBy: editor?._id });
    expect(tenant.lastEditedAt).toBeTypeOf("number");
  });

  it("clears the optional details the form left empty", async () => {
    const t = newBackend();
    const groupId = await seedGroup(t);
    const tenantId = await t.mutation(api.tenants.add, { groupId, ...everyDetail, amountCents: RENT, dueDay: 1 });

    await t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 3B", moveIn: "2026-01-17" });
    const tenant = await tenantRow(t, tenantId);

    expect(Object.keys(tenant).sort()).toEqual(
      ["_creationTime", "_id", "groupId", "lastEditedAt", "lastEditedBy", "moveIn", "name", "status", "version"].sort(),
    );
  });

  it("rejects a stale version with EditConflict naming who last edited, and saves nothing", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 4C", moveIn: "2026-01-01" });
    const editor = await t.run((ctx) => ctx.db.query("accounts").unique());

    const failure = await failureOf(
      t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 5D", moveIn: "2026-01-01" }),
    );

    expect(failure).toEqual({
      code: "EditConflict",
      message: "You changed this tenant while you were editing.",
      details: { currentVersion: 2, lastEditedBy: editor?._id, lastEditedByName: "You" },
    });
    expect(await tenantRow(t, tenantId)).toMatchObject({ name: "Apt 4C", version: 2 });
  });

  it("reports a conflict without a name when nobody has edited the tenant yet", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(
      t.mutation(api.tenants.edit, { tenantId, expectedVersion: 7, name: "Apt 5D", moveIn: "2026-01-01" }),
    );

    expect(failure).toEqual({
      code: "EditConflict",
      message: "This tenant changed while you were editing.",
      details: { currentVersion: 1 },
    });
  });

  it("rejects a last day before the move-in date", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(
      t.mutation(api.tenants.edit, {
        tenantId,
        expectedVersion: 1,
        name: "Apt 3B",
        moveIn: "2026-01-01",
        moveOut: "2025-12-31",
      }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "Last day in unit" } });
    expect(await tenantRow(t, tenantId)).toMatchObject({ version: 1 });
  });

  it("moves the first rent term along when move-in moves to another month", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });

    await t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 3B", moveIn: "2025-11-15" });
    const editor = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(await termRows(t, tenantId)).toMatchObject([
      { startMonth: "2025-11", amountCents: RENT, dueDay: 1, version: 2, lastEditedBy: editor?._id },
    ]);
  });

  it("leaves the first rent term alone when move-in stays in the same month", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });

    await t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 3B", moveIn: "2026-01-20" });

    expect(await termRows(t, tenantId)).toMatchObject([{ startMonth: "2026-01", version: 1 }]);
  });

  it.each([
    ["onto", "2026-06-01"],
    ["past", "2026-07-01"],
  ])("blocks moving move-in %s the month a later rent term starts", async (_position, moveIn) => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });
    await t.mutation(api.rentTerms.add, { tenantId, startMonth: "2026-06", amountCents: 160000, dueDay: 1 });

    const failure = await failureOf(
      t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 3B", moveIn }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "A rent change starts in or before the new move-in month. Edit or delete that rent term first.",
    });
    expect(await tenantRow(t, tenantId)).toMatchObject({ moveIn: "2026-01-01", version: 1 });
    expect((await termRows(t, tenantId)).map((term) => term.startMonth)).toEqual(["2026-01", "2026-06"]);
  });

  it("ignores a deleted rent term when move-in moves past it", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-01" });
    const rentTermId = await t.mutation(api.rentTerms.add, {
      tenantId,
      startMonth: "2026-06",
      amountCents: 160000,
      dueDay: 1,
    });
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Entered by mistake" });

    await t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 3B", moveIn: "2026-07-01" });

    expect(await tenantRow(t, tenantId)).toMatchObject({ moveIn: "2026-07-01", version: 2 });
  });

  it("fails with NotFound when the tenant is missing", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => ctx.db.delete(tenantId));

    const failure = await failureOf(
      t.mutation(api.tenants.edit, { tenantId, expectedVersion: 1, name: "Apt 3B", moveIn: "2026-01-01" }),
    );

    expect(failure.code).toBe("NotFound");
  });
});

describe("tenants.archive and tenants.restore", () => {
  it("archives an active tenant and raises the version", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });
    const editor = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(await tenantRow(t, tenantId)).toMatchObject({ status: "archived", version: 2, lastEditedBy: editor?._id });
  });

  it("restores an archived tenant and raises the version", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    await t.mutation(api.tenants.restore, { tenantId, expectedVersion: 2 });

    expect(await tenantRow(t, tenantId)).toMatchObject({ status: "active", version: 3 });
  });

  it("rejects archiving a tenant that is already archived", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.tenants.archive, { tenantId, expectedVersion: 2 }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This tenant is already archived." });
    expect(await tenantRow(t, tenantId)).toMatchObject({ version: 2 });
  });

  it("rejects restoring a tenant that is not archived", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.mutation(api.tenants.restore, { tenantId, expectedVersion: 1 }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This tenant is not archived." });
    expect(await tenantRow(t, tenantId)).toMatchObject({ version: 1 });
  });

  it.each(["archive", "restore"] as const)("%s rejects a stale version with EditConflict", async (action) => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.tenants[action], { tenantId, expectedVersion: 1 }));

    expect(failure).toMatchObject({ code: "EditConflict", details: { currentVersion: 2, lastEditedByName: "You" } });
    expect(await tenantRow(t, tenantId)).toMatchObject({ status: "archived", version: 2 });
  });

  it.each(["archive", "restore"] as const)("%s fails with NotFound when the tenant is missing", async (action) => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => ctx.db.delete(tenantId));

    const failure = await failureOf(t.mutation(api.tenants[action], { tenantId, expectedVersion: 1 }));

    expect(failure.code).toBe("NotFound");
  });
});

describe("tenants.screen", () => {
  it("returns the group, the tenant, every rent term and the summary as flat lists", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    const rentTermId = await t.mutation(api.rentTerms.add, {
      tenantId,
      startMonth: "2026-06",
      amountCents: 160000,
      dueDay: 1,
    });
    await t.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Entered by mistake" });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: losAngelesTime(2025, 12, 20) });

    expect(screen.groups).toMatchObject([{ name: "1st Street" }]);
    expect(screen.tenants).toEqual([await tenantRow(t, tenantId)]);
    expect(screen.rentTerms).toEqual(await termRows(t, tenantId));
    expect(screen.rentTerms.map((term) => term.deletedAt !== undefined)).toEqual([false, true]);
    expect(screen.tenantSummaries).toEqual([
      {
        tenantId,
        rentOwedCents: 0,
        lateFeesOwedCents: 0,
        otherOwedCents: 0,
        creditCents: 0,
        state: "paidUp",
        nextDueDate: "2026-01-01",
      },
    ]);
  });

  it.each([
    ["the last hour before the due date owes nothing", losAngelesTime(2026, 1, 31, 23), 0, "paidUp"],
    ["rent is owed from the first hour of the due date", losAngelesTime(2026, 2, 1, 0), RENT, "owes"],
    ["rent is still only owed on the last grace day", losAngelesTime(2026, 2, 6, 23), RENT, "owes"],
    ["rent is late once the grace period has passed", losAngelesTime(2026, 2, 7, 0), RENT, "late"],
    ["a second month's rent adds to the balance", losAngelesTime(2026, 3, 1, 0), 2 * RENT, "late"],
  ] as const)("%s in the group's time zone", async (_rule, asOf, rentOwedCents, state) => {
    const t = newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-01-10" });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf });

    expect(screen.tenantSummaries).toMatchObject([{ rentOwedCents, state }]);
  });

  it("shows an archived tenant's balance without calling it late", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: losAngelesTime(2026, 2, 20) });

    expect(screen.tenants).toMatchObject([{ status: "archived" }]);
    expect(screen.tenantSummaries).toMatchObject([{ rentOwedCents: 2 * RENT, state: "owes" }]);
  });

  it("fails with NotFound when the tenant is missing", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => ctx.db.delete(tenantId));

    const failure = await failureOf(t.query(api.tenants.screen, { tenantId, asOf: losAngelesTime(2026, 2, 20) }));

    expect(failure.code).toBe("NotFound");
  });

  it("rejects a time the app could not have sent", async () => {
    const t = newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.query(api.tenants.screen, { tenantId, asOf: -1 }));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "asOf" } });
  });
});
