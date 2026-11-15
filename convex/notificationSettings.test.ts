import { describe, expect, it } from "vitest";
import { api } from "./_generated/api";
import { PERMISSION_PRESETS } from "./permissionRules";
import { insertAccount, insertGroup, insertMembership, newBackend, owner, sam } from "./seed.fixture";

describe("notificationSettings.list", () => {
  it("lists groups whose late-rent pushes reach the caller, by name", async () => {
    const t = await newBackend();
    const [elm, first] = await t.run(async (ctx) => {
      const samId = await insertAccount(ctx, sam);
      const elmCourt = await insertGroup(ctx, { name: "Elm Court" });
      const firstStreet = await insertGroup(ctx, { name: "1st Street" });
      const viewerOnly = await insertGroup(ctx, { name: "Viewer Only" });
      const oldMill = await insertGroup(ctx, { name: "Old Mill", archivedAt: 1 });
      await insertMembership(ctx, elmCourt, samId, { ...PERMISSION_PRESETS.bookkeeper, notificationsOn: false });
      await insertMembership(ctx, firstStreet, samId, { role: "owner" });
      await insertMembership(ctx, viewerOnly, samId, PERMISSION_PRESETS.viewer);
      await insertMembership(ctx, oldMill, samId, { role: "owner" });
      return [elmCourt, firstStreet];
    });

    const settings = await t.withIdentity(sam).query(api.notificationSettings.list, {});

    expect(settings).toEqual([
      { groupId: first, groupName: "1st Street", notificationsOn: true },
      { groupId: elm, groupName: "Elm Court", notificationsOn: false },
    ]);
  });
});

describe("notificationSettings.set", () => {
  it("mutes only the caller's own membership without bumping its version", async () => {
    const t = await newBackend();
    const groupId = await t.run(async (ctx) => {
      const id = await insertGroup(ctx);
      await insertMembership(ctx, id, await insertAccount(ctx, sam), PERMISSION_PRESETS.manager);
      return id;
    });

    await t.withIdentity(sam).mutation(api.notificationSettings.set, { groupId, notificationsOn: false });

    const memberships = await t.run((ctx) => ctx.db.query("memberships").collect());
    const ownerId = await t.run((ctx) => insertAccount(ctx, owner));
    expect(
      memberships.map(({ accountId, notificationsOn, version }) => ({ accountId, notificationsOn, version })),
    ).toEqual([
      { accountId: ownerId, notificationsOn: true, version: 1 },
      { accountId: expect.any(String), notificationsOn: false, version: 1 },
    ]);
  });
});
