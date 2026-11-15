import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { requireAccount, requireGroup, requireMember, requireOwner, requirePermission } from "./access";
import { validationFailed } from "./appError";
import { groupDoc, membershipDoc, tenantDoc } from "./documentValidators";
import { requireExpectedVersion } from "./editConflict";
import { groupSettingsFields, validGroupIdentity, validGroupSettings } from "./groupSettingsInput";
import { groupSummary, groupSummaryOf } from "./groupSummary";
import { loadGroupTenantSummaries } from "./groupTenantSummaries";
import { tenantSummary } from "./tenantSummary";

const DEFAULT_GRACE_DAYS = 5;

export const create = mutation({
  args: groupDoc.pick("name", "timeZone"),
  returns: v.id("groups"),
  handler: async (ctx, args) => {
    const { _id: ownerId } = await requireAccount(ctx);
    const groupId = await ctx.db.insert("groups", {
      ...validGroupIdentity(args),
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

export const updateSettings = mutation({
  args: { groupId: v.id("groups"), expectedVersion: v.number(), ...groupSettingsFields.fields },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { accountId } = await requirePermission(ctx, args.groupId, "settings", "full");
    const group = await requireGroup(ctx, args.groupId);
    await requireExpectedVersion(
      ctx,
      { version: group.version, changedBy: group.lastEditedBy },
      args.expectedVersion,
      "group",
    );
    await ctx.db.patch(group._id, {
      ...validGroupSettings(args),
      version: group.version + 1,
      lastEditedAt: Date.now(),
      lastEditedBy: accountId,
    });
    return null;
  },
});

export const archive = mutation({
  args: { groupId: v.id("groups") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { accountId } = await requireOwner(ctx, args.groupId);
    const group = await requireGroup(ctx, args.groupId);
    if (group.archivedAt !== undefined) {
      throw validationFailed("This group is already archived.");
    }
    const now = Date.now();
    await ctx.db.patch(group._id, {
      archivedAt: now,
      version: group.version + 1,
      lastEditedAt: now,
      lastEditedBy: accountId,
    });
    return null;
  },
});

export const unarchive = mutation({
  args: { groupId: v.id("groups") },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { accountId } = await requireOwner(ctx, args.groupId);
    const group = await requireGroup(ctx, args.groupId);
    if (group.archivedAt === undefined) {
      throw validationFailed("This group is not archived.");
    }
    await ctx.db.patch(group._id, {
      archivedAt: undefined,
      version: group.version + 1,
      lastEditedAt: Date.now(),
      lastEditedBy: accountId,
    });
    return null;
  },
});

export const screen = query({
  args: { groupId: v.id("groups"), asOf: v.number() },
  returns: v.object({
    groups: v.array(groupDoc),
    tenants: v.array(tenantDoc),
    tenantSummaries: v.array(tenantSummary),
    groupSummaries: v.array(groupSummary),
    memberships: v.array(membershipDoc),
  }),
  handler: async (ctx, args) => {
    const membership = await requireMember(ctx, args.groupId);
    const group = await requireGroup(ctx, args.groupId);
    const { tenants, tenantSummaries } = await loadGroupTenantSummaries(ctx, group, args.asOf);
    return {
      groups: [group],
      tenants,
      tenantSummaries,
      groupSummaries: [groupSummaryOf(group._id, tenants, tenantSummaries)],
      memberships: [membership],
    };
  },
});
