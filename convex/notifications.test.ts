import { afterEach, describe, expect, it, vi } from "vitest";
import { internal } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import { PERMISSION_PRESETS } from "./permissionRules";
import {
  type Backend,
  insertAccount,
  insertGroup,
  insertMembership,
  insertPayment,
  insertTenantOwingRent,
  losAngelesTime,
  newBackend,
  owner,
  sam,
} from "./seed.fixture";

const RENT_CENTS = 150000;
const PAID_THROUGH_OCTOBER_CENTS = RENT_CENTS * 8;
const lee = { subject: "auth|lee", email: "lee@example.com", name: "Lee" };

const NEWLY_LATE_SATURDAY = losAngelesTime(2026, 11, 7, 9);
const SUNDAY_AFTER = losAngelesTime(2026, 11, 8, 9);
const WEEKLY_MONDAY = losAngelesTime(2026, 11, 9, 9);
const LAST_GRACE_DAY = losAngelesTime(2026, 11, 6, 9);

interface PushScene {
  t: Backend;
  groupId: Id<"groups">;
}

async function sceneWithTenantPaidThroughOctober(groupFields: Partial<Doc<"groups">> = {}): Promise<PushScene> {
  const t = await newBackend();
  const groupId = await t.run(async (ctx) => {
    const id = await insertGroup(ctx, groupFields);
    const tenantId = await insertTenantOwingRent(ctx, id, RENT_CENTS);
    await insertPayment(ctx, tenantId, { amountCents: PAID_THROUGH_OCTOBER_CENTS, dateReceived: "2026-10-01" });
    await insertToken(ctx, await insertAccount(ctx, owner), "owner-phone");
    return id;
  });
  return { t, groupId };
}

function insertToken(
  ctx: MutationCtx,
  accountId: Id<"accounts">,
  token: string,
  platform: Doc<"pushTokens">["platform"] = "android",
) {
  return ctx.db.insert("pushTokens", { accountId, token, platform });
}

async function scheduledPushes(t: Backend) {
  const scheduled = await t.run((ctx) => ctx.db.system.query("_scheduled_functions").collect());
  return scheduled
    .filter((job) => job.name.startsWith("pushDelivery"))
    .map((job) => job.args[0] as { groupId: string; title: string; body: string; tokens: string[] });
}

async function plan(scene: PushScene, now: number) {
  await scene.t.mutation(internal.notifications.planGroupPush, { groupId: scene.groupId, now });
}

afterEach(() => {
  vi.useRealTimers();
});

describe("planGroupPush", () => {
  it("sends the newly-late push on the day after the grace period ends", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();

    await plan(scene, NEWLY_LATE_SATURDAY);

    expect(await scheduledPushes(scene.t)).toEqual([
      {
        groupId: scene.groupId,
        title: "Rent is late",
        body: "1st Street: 1 tenant just became late — 1 late in total, $1,500.00 owed.",
        tokens: ["owner-phone"],
      },
    ]);
  });

  it("sends nothing while the tenant is still inside the grace period", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();

    await plan(scene, LAST_GRACE_DAY);

    expect(await scheduledPushes(scene.t)).toEqual([]);
  });

  it("sends nothing the day after when nobody newly became late and it is not Monday", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();

    await plan(scene, SUNDAY_AFTER);

    expect(await scheduledPushes(scene.t)).toEqual([]);
  });

  it("sends the weekly reminder on Monday while anyone is late", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();

    await plan(scene, WEEKLY_MONDAY);

    expect((await scheduledPushes(scene.t)).map((push) => push.body)).toEqual([
      "1st Street: 1 tenant is late, $1,500.00 owed.",
    ]);
  });

  it("counts tenants already late in the total and the amount owed by every active tenant", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();
    await scene.t.run(async (ctx) => {
      await insertTenantOwingRent(ctx, scene.groupId, RENT_CENTS, { name: "Apt 4C" });
      await insertTenantOwingRent(ctx, scene.groupId, RENT_CENTS, { name: "Apt 5D", status: "archived" });
    });

    await plan(scene, NEWLY_LATE_SATURDAY);

    expect((await scheduledPushes(scene.t)).map((push) => push.body)).toEqual([
      "1st Street: 1 tenant just became late — 2 late in total, $15,000.00 owed.",
    ]);
  });

  it("sends at most one push per group per local day", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();

    await plan(scene, NEWLY_LATE_SATURDAY);
    await plan(scene, NEWLY_LATE_SATURDAY + 60 * 60 * 1000);

    expect(await scheduledPushes(scene.t)).toHaveLength(1);
    const log = await scene.t.run((ctx) => ctx.db.query("notificationLog").collect());
    expect(log.map((row) => row.localDate)).toEqual(["2026-11-07"]);
  });

  it("reaches the owner and members who can log payments with notifications on", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();
    await scene.t.run(async (ctx) => {
      const samId = await insertAccount(ctx, sam);
      await insertMembership(ctx, scene.groupId, samId, PERMISSION_PRESETS.bookkeeper);
      await insertToken(ctx, samId, "sam-phone");
      await insertToken(ctx, samId, "sam-iphone", "ios");
      const leeId = await insertAccount(ctx, lee);
      await insertMembership(ctx, scene.groupId, leeId, PERMISSION_PRESETS.viewer);
      await insertToken(ctx, leeId, "lee-phone");
    });

    await plan(scene, NEWLY_LATE_SATURDAY);

    expect((await scheduledPushes(scene.t)).map((push) => push.tokens.sort())).toEqual([["owner-phone", "sam-phone"]]);
  });

  it("skips members who muted the group", async () => {
    const scene = await sceneWithTenantPaidThroughOctober();
    await scene.t.run(async (ctx) => {
      const ownerMembership = await ctx.db.query("memberships").first();
      if (ownerMembership !== null) {
        await ctx.db.patch(ownerMembership._id, { notificationsOn: false });
      }
      const samId = await insertAccount(ctx, sam);
      await insertMembership(ctx, scene.groupId, samId, { ...PERMISSION_PRESETS.manager, notificationsOn: false });
      await insertToken(ctx, samId, "sam-phone");
    });

    await plan(scene, NEWLY_LATE_SATURDAY);

    expect(await scheduledPushes(scene.t)).toEqual([]);
    const log = await scene.t.run((ctx) => ctx.db.query("notificationLog").collect());
    expect(log).toHaveLength(1);
  });

  it("never pushes for an archived group", async () => {
    const scene = await sceneWithTenantPaidThroughOctober({ archivedAt: 1 });

    await plan(scene, NEWLY_LATE_SATURDAY);

    expect(await scheduledPushes(scene.t)).toEqual([]);
  });
});

describe("dispatchHourly", () => {
  it("plans only groups whose local time just reached 9:00 AM", async () => {
    vi.useFakeTimers({ toFake: ["Date"], now: NEWLY_LATE_SATURDAY });
    const t = await newBackend();
    const [losAngeles] = await t.run(async (ctx) => [
      await insertGroup(ctx, { name: "1st Street" }),
      await insertGroup(ctx, { name: "Elm Court", timeZone: "America/New_York" }),
      await insertGroup(ctx, { name: "Old Mill", archivedAt: 1 }),
    ]);

    await t.mutation(internal.notifications.dispatchHourly, {});

    const scheduled = await t.run((ctx) => ctx.db.system.query("_scheduled_functions").collect());
    expect(scheduled.map((job) => (job.args[0] as { groupId: string }).groupId)).toEqual([losAngeles]);
  });
});
