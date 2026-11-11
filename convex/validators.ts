import { v } from "convex/values";

export const role = v.union(v.literal("owner"), v.literal("member"));

export const permissionLevel = v.union(v.literal("read"), v.literal("addEdit"), v.literal("full"));

export const settingsLevel = v.union(v.literal("read"), v.literal("full"));

export const permissionFields = {
  tenants: permissionLevel,
  rentTerms: permissionLevel,
  payments: permissionLevel,
  debts: permissionLevel,
  members: permissionLevel,
  settings: settingsLevel,
  editOthers: v.boolean(),
};

export const permissionSet = v.object(permissionFields);

export const paymentOrder = v.union(v.literal("rentFirst"), v.literal("byDate"));

export const tenantStatus = v.union(v.literal("active"), v.literal("archived"));

export const paymentMethod = v.union(
  v.literal("cash"),
  v.literal("check"),
  v.literal("zelle"),
  v.literal("venmo"),
  v.literal("bankTransfer"),
  v.literal("other"),
);

export const debtKind = v.union(v.literal("lateFee"), v.literal("custom"));

export const pushPlatform = v.union(v.literal("android"), v.literal("ios"));

export const historyActor = v.union(v.id("accounts"), v.literal("support"));

export const versionFields = {
  version: v.number(),
  lastEditedAt: v.optional(v.number()),
};

export const softDeleteFields = {
  deletedAt: v.optional(v.number()),
  deletedBy: v.optional(v.id("accounts")),
  deleteReason: v.optional(v.string()),
};
