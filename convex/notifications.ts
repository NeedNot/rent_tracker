import { v } from "convex/values";
import { internal } from "./_generated/api";
import { internalMutation } from "./_generated/server";
import { latePushMessage } from "./latePushMessage";
import { latePushTokens } from "./latePushRecipients";
import { loadLateRentFacts } from "./lateRentFacts";
import { localDateAt } from "./localDate";
import { localHourAt } from "./localTime";

export const LATE_PUSH_LOCAL_HOUR = 9;

export const dispatchHourly = internalMutation({
  args: {},
  returns: v.null(),
  handler: async (ctx) => {
    const now = Date.now();
    const groups = await ctx.db.query("groups").collect();
    const due = groups.filter(
      (group) => group.archivedAt === undefined && localHourAt(now, group.timeZone) === LATE_PUSH_LOCAL_HOUR,
    );
    for (const group of due) {
      await ctx.scheduler.runAfter(0, internal.notifications.planGroupPush, { groupId: group._id, now });
    }
    return null;
  },
});

export const planGroupPush = internalMutation({
  args: { groupId: v.id("groups"), now: v.number() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const group = await ctx.db.get(args.groupId);
    if (group === null || group.archivedAt !== undefined) {
      return null;
    }
    const localDate = localDateAt(args.now, group.timeZone);
    const alreadySent = await ctx.db
      .query("notificationLog")
      .withIndex("by_group_local_date", (q) => q.eq("groupId", group._id).eq("localDate", localDate))
      .unique();
    if (alreadySent !== null) {
      return null;
    }
    const message = latePushMessage(await loadLateRentFacts(ctx, group, localDate));
    if (message === null) {
      return null;
    }
    await ctx.db.insert("notificationLog", { groupId: group._id, localDate });
    const tokens = await latePushTokens(ctx, group._id);
    if (tokens.length > 0) {
      await ctx.scheduler.runAfter(0, internal.pushDelivery.send, { groupId: group._id, ...message, tokens });
    }
    return null;
  },
});
