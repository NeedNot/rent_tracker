import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { requireAccount, requireMember } from "./access";
import { receivesLatePushes } from "./latePushRecipients";

const groupNotificationSetting = v.object({
  groupId: v.id("groups"),
  groupName: v.string(),
  notificationsOn: v.boolean(),
});

export const list = query({
  args: {},
  returns: v.array(groupNotificationSetting),
  handler: async (ctx) => {
    const account = await requireAccount(ctx);
    const memberships = await ctx.db
      .query("memberships")
      .withIndex("by_account", (q) => q.eq("accountId", account._id))
      .collect();
    const settings = await Promise.all(
      memberships.filter(receivesLatePushes).map(async (membership) => {
        const group = await ctx.db.get(membership.groupId);
        if (group === null || group.archivedAt !== undefined) {
          return null;
        }
        return { groupId: group._id, groupName: group.name, notificationsOn: membership.notificationsOn };
      }),
    );
    return settings
      .filter((setting) => setting !== null)
      .sort((a, b) => a.groupName.localeCompare(b.groupName, "en-US"));
  },
});

export const set = mutation({
  args: { groupId: v.id("groups"), notificationsOn: v.boolean() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const membership = await requireMember(ctx, args.groupId);
    if (membership.notificationsOn !== args.notificationsOn) {
      await ctx.db.patch(membership._id, { notificationsOn: args.notificationsOn });
    }
    return null;
  },
});
