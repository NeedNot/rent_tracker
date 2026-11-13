import { convexTest } from "convex-test";
import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import { insertGroup, insertPayment, insertTenantOwingRent } from "./groupSeed.fixture";
import schema from "./schema";
import { modules } from "./test.setup";

const MARCH_20_IN_LOS_ANGELES = Date.UTC(2026, 2, 20, 20);
const LAST_HOUR_OF_GRACE_IN_LOS_ANGELES = Date.UTC(2026, 2, 7, 5);

describe("home.screen", () => {
  it("returns nothing before the first group is created", async () => {
    const t = convexTest(schema, modules);

    const home = await t.query(api.home.screen, { asOf: MARCH_20_IN_LOS_ANGELES });

    expect(home).toEqual({ groups: [], groupSummaries: [] });
  });

  it("summarizes each group: active debt, late count and archived debt apart", async () => {
    const t = convexTest(schema, modules);
    const seeded = await t.run(async (ctx) => {
      const streetId = await insertGroup(ctx, { name: "1st Street" });
      await insertTenantOwingRent(ctx, streetId, 150000, { name: "Late" });
      await insertTenantOwingRent(ctx, streetId, 80000, { name: "Second late" });
      const overpaidId = await insertTenantOwingRent(ctx, streetId, 100000, { name: "In credit" });
      await insertPayment(ctx, overpaidId, { amountCents: 130000, dateReceived: "2026-03-01" });
      await insertTenantOwingRent(ctx, streetId, 50000, {
        name: "Moved out owing",
        status: "archived",
        moveIn: "2026-01-01",
        moveOut: "2026-01-31",
      });
      const emptyId = await insertGroup(ctx, { name: "Empty lot" });
      return { streetId, emptyId };
    });

    const home = await t.query(api.home.screen, { asOf: MARCH_20_IN_LOS_ANGELES });

    expect(home.groups.map((group) => group.name)).toEqual(["1st Street", "Empty lot"]);
    expect(home.groupSummaries).toEqual([
      {
        groupId: seeded.streetId,
        activeTenantCount: 3,
        lateTenantCount: 2,
        totalOwedCents: 230000,
        archivedTenantCount: 1,
        archivedOwedCents: 50000,
      },
      {
        groupId: seeded.emptyId,
        activeTenantCount: 0,
        lateTenantCount: 0,
        totalOwedCents: 0,
        archivedTenantCount: 0,
        archivedOwedCents: 0,
      },
    ]);
  });

  it("counts a tenant inside the grace period as owing but not late", async () => {
    const t = convexTest(schema, modules);
    await t.run(async (ctx) => {
      await insertTenantOwingRent(ctx, await insertGroup(ctx), 150000);
    });

    const home = await t.query(api.home.screen, { asOf: LAST_HOUR_OF_GRACE_IN_LOS_ANGELES });

    expect(home.groupSummaries[0]).toMatchObject({ activeTenantCount: 1, lateTenantCount: 0, totalOwedCents: 150000 });
  });

  it("uses each group's own time zone for today", async () => {
    const t = convexTest(schema, modules);
    const seeded = await t.run(async (ctx) => {
      const losAngelesId = await insertGroup(ctx, { timeZone: "America/Los_Angeles" });
      const londonId = await insertGroup(ctx, { timeZone: "Europe/London" });
      await insertTenantOwingRent(ctx, losAngelesId, 150000);
      await insertTenantOwingRent(ctx, londonId, 150000);
      return { losAngelesId, londonId };
    });

    const home = await t.query(api.home.screen, { asOf: LAST_HOUR_OF_GRACE_IN_LOS_ANGELES });

    expect(home.groupSummaries).toMatchObject([
      { groupId: seeded.losAngelesId, lateTenantCount: 0 },
      { groupId: seeded.londonId, lateTenantCount: 1 },
    ]);
  });

  it("leaves out archived groups", async () => {
    const t = convexTest(schema, modules);
    const openId = await t.run(async (ctx) => {
      const archivedId = await insertGroup(ctx, { name: "Sold", archivedAt: 1_700_000_000_000 });
      await insertTenantOwingRent(ctx, archivedId, 150000);
      return await insertGroup(ctx, { name: "1st Street" });
    });

    const home = await t.query(api.home.screen, { asOf: MARCH_20_IN_LOS_ANGELES });

    expect(home.groups.map((group) => group.name)).toEqual(["1st Street"]);
    expect(home.groupSummaries.map((summary) => summary.groupId)).toEqual([openId]);
  });
});
