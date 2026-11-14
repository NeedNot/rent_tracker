import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { requireAccount, requireMember } from "./access";
import { appError } from "./appError";
import { groupDoc, tenantDoc } from "./documentValidators";
import { groupSummary, groupSummaryOf } from "./groupSummary";
import { loadGroupTenantSummaries } from "./groupTenantSummaries";
import { requireName, requireTimeZone } from "./inputValidation";
import { tenantSummary } from "./tenantSummary";

const DEFAULT_GRACE_DAYS = 5;

export const create = mutation({
  args: groupDoc.pick("name", "timeZone"),
  returns: v.id("groups"),
  handler: async (ctx, args) => {
    const { _id: ownerId } = await requireAccount(ctx);
    const name = requireName(args.name, "Group name");
    const timeZone = requireTimeZone(args.timeZone);
    const groupId = await ctx.db.insert("groups", {
      name,
      timeZone,
      paymentOrder: "rentFirst",
      graceDays: DEFAULT_GRACE_DAYS,
      ownerId,
      version: 1,
    });
    await ctx.db.insert("memberships", {
      groupId,
      accountId: ownerId,
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
    return groupId;
  },
});

export const screen = query({
  args: { groupId: v.id("groups"), asOf: v.number() },
  returns: v.object({
    groups: v.array(groupDoc),
    tenants: v.array(tenantDoc),
    tenantSummaries: v.array(tenantSummary),
    groupSummaries: v.array(groupSummary),
  }),
  handler: async (ctx, args) => {
    await requireMember(ctx, args.groupId);
    const group = await ctx.db.get(args.groupId);
    if (group === null || group.archivedAt !== undefined) {
      throw appError("NotFound", "This group no longer exists.");
    }
    const { tenants, tenantSummaries } = await loadGroupTenantSummaries(ctx, group, args.asOf);
    return {
      groups: [group],
      tenants,
      tenantSummaries,
      groupSummaries: [groupSummaryOf(group._id, tenants, tenantSummaries)],
    };
  },
});
