import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import {
  failureOf,
  insertAccount,
  insertGroup,
  insertMembership,
  insertPayment,
  insertTenant,
  insertTenantOwingRent,
  losAngelesTime,
  newBackend,
  RENT,
  sam,
  seedGroup,
} from "./seed.fixture";

const MARCH_20_IN_LOS_ANGELES = losAngelesTime(2026, 3, 20);
const LAST_HOUR_OF_GRACE_IN_LOS_ANGELES = losAngelesTime(2026, 3, 6, 21);

describe("groups.create", () => {
  it("creates the group with the new-group defaults and the phone's time zone", async () => {
    const t = await newBackend();

    const groupId = await t.mutation(api.groups.create, { name: "1st Street", timeZone: "America/Chicago" });
    const group = await t.run((ctx) => ctx.db.get(groupId));
    const owner = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(group).toMatchObject({
      name: "1st Street",
      timeZone: "America/Chicago",
      paymentOrder: "rentFirst",
      graceDays: 5,
      ownerId: owner?._id,
      version: 1,
    });
    expect(group).not.toHaveProperty("lateFeeFlatCents");
    expect(group).not.toHaveProperty("archivedAt");
  });

  it("makes the creator the owner with full control", async () => {
    const t = await newBackend();

    const groupId = await t.mutation(api.groups.create, { name: "1st Street", timeZone: "America/Chicago" });
    const memberships = await t.run((ctx) => ctx.db.query("memberships").collect());
    const owner = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(memberships).toHaveLength(1);
    expect(memberships[0]).toMatchObject({
      groupId,
      accountId: owner?._id,
      role: "owner",
      tenants: "full",
      rentTerms: "full",
      payments: "full",
      debts: "full",
      members: "full",
      settings: "full",
      editOthers: true,
      notificationsOn: true,
      version: 1,
    });
  });

  it("trims the group name", async () => {
    const t = await newBackend();

    const groupId = await t.mutation(api.groups.create, { name: "  1st Street ", timeZone: "America/Chicago" });
    const group = await t.run((ctx) => ctx.db.get(groupId));

    expect(group?.name).toBe("1st Street");
  });

  it("accepts a name of exactly 100 characters", async () => {
    const t = await newBackend();

    const groupId = await t.mutation(api.groups.create, { name: "a".repeat(100), timeZone: "America/Chicago" });

    expect(await t.run((ctx) => ctx.db.get(groupId))).not.toBeNull();
  });

  it.each([
    { reason: "a blank name", name: "   ", timeZone: "America/Chicago", field: "Group name" },
    { reason: "a name over 100 characters", name: "a".repeat(101), timeZone: "America/Chicago", field: "Group name" },
    { reason: "an unknown time zone", name: "1st Street", timeZone: "Mars/Olympus_Mons", field: "Time zone" },
    { reason: "an empty time zone", name: "1st Street", timeZone: "", field: "Time zone" },
  ])("rejects $reason and saves nothing", async ({ name, timeZone, field }) => {
    const t = await newBackend();

    await expect(t.mutation(api.groups.create, { name, timeZone })).rejects.toMatchObject({
      data: { code: "ValidationFailed", details: { field } },
    });

    expect(await t.run((ctx) => ctx.db.query("groups").collect())).toEqual([]);
    expect(await t.run((ctx) => ctx.db.query("memberships").collect())).toEqual([]);
  });
});

describe("groups.screen", () => {
  it("returns the group, every tenant with its summary, and the group summary", async () => {
    const t = await newBackend();
    const seeded = await t.run(async (ctx) => {
      const groupId = await insertGroup(ctx);
      const lateId = await insertTenantOwingRent(ctx, groupId, RENT, { name: "Apt 1" });
      const noRentId = await insertTenant(ctx, groupId, { name: "Apt 2" });
      const archivedId = await insertTenantOwingRent(ctx, groupId, 50000, {
        name: "Apt 3",
        status: "archived",
        moveIn: "2026-01-01",
        moveOut: "2026-01-31",
      });
      return { groupId, lateId, noRentId, archivedId };
    });

    const screen = await t.query(api.groups.screen, { groupId: seeded.groupId, asOf: MARCH_20_IN_LOS_ANGELES });

    expect(screen.groups).toEqual([await t.run((ctx) => ctx.db.get(seeded.groupId))]);
    expect(screen.tenants.map((tenant) => tenant.name)).toEqual(["Apt 1", "Apt 2", "Apt 3"]);
    expect(screen.tenantSummaries).toEqual([
      {
        tenantId: seeded.lateId,
        rentOwedCents: RENT,
        lateFeesOwedCents: 0,
        otherOwedCents: 0,
        creditCents: 0,
        state: "late",
        nextDueDate: "2026-04-01",
        lateFeeSuggestionMonth: "2026-03",
        lateFeeSuggestionCents: 7500,
      },
      {
        tenantId: seeded.noRentId,
        rentOwedCents: 0,
        lateFeesOwedCents: 0,
        otherOwedCents: 0,
        creditCents: 0,
        state: "paidUp",
      },
      {
        tenantId: seeded.archivedId,
        rentOwedCents: 50000,
        lateFeesOwedCents: 0,
        otherOwedCents: 0,
        creditCents: 0,
        state: "owes",
      },
    ]);
    expect(screen.groupSummaries).toEqual([
      {
        groupId: seeded.groupId,
        activeTenantCount: 2,
        lateTenantCount: 1,
        totalOwedCents: RENT,
        archivedTenantCount: 1,
        archivedOwedCents: 50000,
      },
    ]);
  });

  it("returns only the caller's own membership", async () => {
    const t = await newBackend();
    const { groupId, samMembershipId } = await t.run(async (ctx) => {
      const groupId = await insertGroup(ctx);
      const samMembershipId = await insertMembership(ctx, groupId, await insertAccount(ctx, sam), { payments: "full" });
      return { groupId, samMembershipId };
    });

    const screen = await t.withIdentity(sam).query(api.groups.screen, { groupId, asOf: MARCH_20_IN_LOS_ANGELES });

    expect(screen.memberships).toMatchObject([{ _id: samMembershipId, role: "member", payments: "full" }]);
  });

  it("leaves out tenants of other groups", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const otherGroupId = await insertGroup(ctx, { name: "2nd Street" });
      await insertTenantOwingRent(ctx, otherGroupId, RENT);
      return await insertGroup(ctx);
    });

    const screen = await t.query(api.groups.screen, { groupId, asOf: MARCH_20_IN_LOS_ANGELES });

    expect(screen.tenants).toEqual([]);
    expect(screen.tenantSummaries).toEqual([]);
    expect(screen.groupSummaries[0]).toMatchObject({ activeTenantCount: 0, totalOwedCents: 0 });
  });

  it("counts a payment received today in the group's time zone", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const id = await insertGroup(ctx);
      const tenantId = await insertTenantOwingRent(ctx, id, RENT);
      await insertPayment(ctx, tenantId, { amountCents: RENT, dateReceived: "2026-03-20" });
      return id;
    });

    const screen = await t.query(api.groups.screen, { groupId, asOf: MARCH_20_IN_LOS_ANGELES });

    expect(screen.tenantSummaries[0]).toMatchObject({ rentOwedCents: 0, state: "paidUp" });
  });

  it("decides lateness on the group's local date, not the server's", async () => {
    const t = await newBackend();
    const seeded = await t.run(async (ctx) => {
      const losAngelesId = await insertGroup(ctx, { timeZone: "America/Los_Angeles" });
      const londonId = await insertGroup(ctx, { timeZone: "Europe/London" });
      await insertTenantOwingRent(ctx, losAngelesId, RENT);
      await insertTenantOwingRent(ctx, londonId, RENT);
      return { losAngelesId, londonId };
    });
    const asOf = LAST_HOUR_OF_GRACE_IN_LOS_ANGELES;

    const losAngeles = await t.query(api.groups.screen, { groupId: seeded.losAngelesId, asOf });
    const london = await t.query(api.groups.screen, { groupId: seeded.londonId, asOf });

    expect(losAngeles.tenantSummaries).toMatchObject([{ state: "owes" }]);
    expect(losAngeles.groupSummaries).toMatchObject([{ lateTenantCount: 0 }]);
    expect(london.tenantSummaries).toMatchObject([{ state: "late" }]);
    expect(london.groupSummaries).toMatchObject([{ lateTenantCount: 1 }]);
  });

  it("lets the owner open an archived group", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx, { archivedAt: 1_700_000_000_000 }));

    const screen = await t.query(api.groups.screen, { groupId, asOf: MARCH_20_IN_LOS_ANGELES });

    expect(screen.groups).toMatchObject([{ _id: groupId, archivedAt: 1_700_000_000_000 }]);
  });

  it("answers NotFound to a member of an archived group", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const id = await insertGroup(ctx, { archivedAt: 1_700_000_000_000 });
      await insertMembership(ctx, id, await insertAccount(ctx, sam));
      return id;
    });

    const failure = await failureOf(
      t.withIdentity(sam).query(api.groups.screen, { groupId, asOf: MARCH_20_IN_LOS_ANGELES }),
    );

    expect(failure).toMatchObject({ code: "NotFound", message: "This group no longer exists." });
  });

  it("answers NotFound for a group that does not exist", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const id = await insertGroup(ctx);
      await ctx.db.delete(id);
      return id;
    });

    await expect(t.query(api.groups.screen, { groupId, asOf: MARCH_20_IN_LOS_ANGELES })).rejects.toMatchObject({
      data: { code: "NotFound" },
    });
  });

  it("rejects an asOf that is not a real timestamp", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx));

    await expect(t.query(api.groups.screen, { groupId, asOf: -1 })).rejects.toMatchObject({
      data: { code: "ValidationFailed", details: { field: "asOf" } },
    });
  });
});

const validSettings = {
  name: "Main Street",
  timeZone: "America/Chicago",
  paymentOrder: "byDate" as const,
  graceDays: 3,
};

describe("groups.updateSettings", () => {
  it("saves the settings, bumps the version and stamps the editor", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx));

    await t.mutation(api.groups.updateSettings, {
      groupId,
      expectedVersion: 1,
      ...validSettings,
      name: " Main Street ",
      lateFeeFlatCents: 5000,
    });
    const group = await t.run((ctx) => ctx.db.get(groupId));
    const owner = await t.run((ctx) => ctx.db.query("accounts").unique());

    expect(group).toMatchObject({ ...validSettings, lateFeeFlatCents: 5000, version: 2, lastEditedBy: owner?._id });
  });

  it("clears the flat late fee when none is sent", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx, { lateFeeFlatCents: 5000 }));

    await t.mutation(api.groups.updateSettings, { groupId, expectedVersion: 1, ...validSettings });

    expect(await t.run((ctx) => ctx.db.get(groupId))).not.toHaveProperty("lateFeeFlatCents");
  });

  it.each([
    { reason: "a blank name", change: { name: " " }, field: "Group name" },
    { reason: "an unknown time zone", change: { timeZone: "Mars/Olympus_Mons" }, field: "Time zone" },
    { reason: "a grace period over 15 days", change: { graceDays: 16 }, field: "Grace period" },
    { reason: "a negative grace period", change: { graceDays: -1 }, field: "Grace period" },
    { reason: "a late fee of zero", change: { lateFeeFlatCents: 0 }, field: "Late fee" },
  ])("rejects $reason and saves nothing", async ({ change, field }) => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx));

    const failure = await failureOf(
      t.mutation(api.groups.updateSettings, { groupId, expectedVersion: 1, ...validSettings, ...change }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field } });
    expect(await t.run((ctx) => ctx.db.get(groupId))).toMatchObject({ name: "1st Street", version: 1 });
  });

  it("reports an edit conflict when the group changed", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx, { version: 3 }));

    const failure = await failureOf(
      t.mutation(api.groups.updateSettings, { groupId, expectedVersion: 2, ...validSettings }),
    );

    expect(failure).toMatchObject({ code: "EditConflict", details: { currentVersion: 3 } });
  });

  it("names who changed the settings in the edit conflict", async () => {
    const t = await newBackend();
    const groupId = await seedGroup(t);
    await t.mutation(api.groups.updateSettings, { groupId, expectedVersion: 1, ...validSettings });

    const failure = await failureOf(
      t.mutation(api.groups.updateSettings, { groupId, expectedVersion: 1, ...validSettings }),
    );

    expect(failure).toMatchObject({ code: "EditConflict", details: { currentVersion: 2, lastEditedByName: "Dana" } });
  });

  it.each([
    { settings: "read" as const, code: "PermissionDenied" },
    { settings: "full" as const, code: undefined },
  ])("Settings: $settings member gets $code", async ({ settings, code }) => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const id = await insertGroup(ctx);
      await insertMembership(ctx, id, await insertAccount(ctx, sam), { settings });
      return id;
    });
    const call = t
      .withIdentity(sam)
      .mutation(api.groups.updateSettings, { groupId, expectedVersion: 1, ...validSettings });

    if (code === undefined) {
      expect(await call).toBeNull();
    } else {
      expect(await failureOf(call)).toMatchObject({ code });
    }
  });

  it("is rejected in an archived group, even for the owner", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx, { archivedAt: 1_700_000_000_000 }));

    const failure = await failureOf(
      t.mutation(api.groups.updateSettings, { groupId, expectedVersion: 1, ...validSettings }),
    );

    expect(failure).toMatchObject({ code: "NotFound", message: "This group no longer exists." });
  });
});

describe("groups.archive and groups.unarchive", () => {
  async function groupWithSettingsFullMember(t: Awaited<ReturnType<typeof newBackend>>, fields = {}) {
    return await t.run(async (ctx) => {
      const id = await insertGroup(ctx, fields);
      await insertMembership(ctx, id, await insertAccount(ctx, sam), { settings: "full", members: "full" });
      return id;
    });
  }

  it("archives the group for the owner and bumps the version", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx));

    await t.mutation(api.groups.archive, { groupId });

    expect(await t.run((ctx) => ctx.db.get(groupId))).toMatchObject({ archivedAt: expect.any(Number), version: 2 });
  });

  it("refuses to archive an archived group", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx, { archivedAt: 1_700_000_000_000 }));

    expect(await failureOf(t.mutation(api.groups.archive, { groupId }))).toMatchObject({ code: "ValidationFailed" });
  });

  it("refuses to unarchive an active group", async () => {
    const t = await newBackend();
    const groupId = await t.run((ctx) => insertGroup(ctx));

    expect(await failureOf(t.mutation(api.groups.unarchive, { groupId }))).toMatchObject({ code: "ValidationFailed" });
  });

  it("denies a member with full Settings and Members the archive", async () => {
    const t = await newBackend();
    const groupId = await groupWithSettingsFullMember(t);

    const failure = await failureOf(t.withIdentity(sam).mutation(api.groups.archive, { groupId }));

    expect(failure).toMatchObject({ code: "PermissionDenied" });
  });

  it("answers NotFound when a member tries to unarchive", async () => {
    const t = await newBackend();
    const groupId = await groupWithSettingsFullMember(t, { archivedAt: 1_700_000_000_000 });

    const failure = await failureOf(t.withIdentity(sam).mutation(api.groups.unarchive, { groupId }));

    expect(failure).toMatchObject({ code: "NotFound" });
  });

  it("unarchive clears archivedAt and restores access for members", async () => {
    const t = await newBackend();
    const groupId = await groupWithSettingsFullMember(t, { archivedAt: 1_700_000_000_000 });
    const asSam = t.withIdentity(sam);
    await t.mutation(api.groups.unarchive, { groupId });

    const screen = await asSam.query(api.groups.screen, { groupId, asOf: MARCH_20_IN_LOS_ANGELES });

    expect(screen.groups[0]).not.toHaveProperty("archivedAt");
  });
});
