import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";
import {
  debtKind,
  paymentMethod,
  paymentOrder,
  permissionFields,
  permissionSet,
  pushPlatform,
  role,
  softDeleteFields,
  tenantStatus,
  versionFields,
} from "./validators";

export default defineSchema({
  accounts: defineTable({
    authSubject: v.string(),
    displayName: v.string(),
    email: v.optional(v.string()),
    deletionRequestedAt: v.optional(v.number()),
    deletedAt: v.optional(v.number()),
    ...versionFields,
  })
    .index("by_auth_subject", ["authSubject"])
    .index("by_email", ["email"]),

  groups: defineTable({
    name: v.string(),
    timeZone: v.string(),
    paymentOrder,
    graceDays: v.number(),
    lateFeeFlatCents: v.optional(v.number()),
    ownerId: v.id("accounts"),
    archivedAt: v.optional(v.number()),
    ...versionFields,
  }),

  memberships: defineTable({
    groupId: v.id("groups"),
    accountId: v.id("accounts"),
    role,
    ...permissionFields,
    notificationsOn: v.boolean(),
    ...versionFields,
  })
    .index("by_account", ["accountId"])
    .index("by_group_account", ["groupId", "accountId"]),

  invites: defineTable({
    groupId: v.id("groups"),
    email: v.string(),
    permissions: permissionSet,
    invitedBy: v.id("accounts"),
    expiresAt: v.number(),
    acceptedAt: v.optional(v.number()),
    revokedAt: v.optional(v.number()),
  })
    .index("by_email", ["email"])
    .index("by_group", ["groupId"]),

  tenants: defineTable({
    groupId: v.id("groups"),
    name: v.string(),
    contactName: v.optional(v.string()),
    contactPhone: v.optional(v.string()),
    contactEmail: v.optional(v.string()),
    notes: v.optional(v.string()),
    depositCents: v.optional(v.number()),
    moveIn: v.string(),
    moveOut: v.optional(v.string()),
    status: tenantStatus,
    graceDaysOverride: v.optional(v.number()),
    lateFeeFlatOverrideCents: v.optional(v.number()),
    lastPaymentMethod: v.optional(paymentMethod),
    ...versionFields,
  }).index("by_group_status", ["groupId", "status"]),

  rentTerms: defineTable({
    tenantId: v.id("tenants"),
    startMonth: v.string(),
    amountCents: v.number(),
    dueDay: v.number(),
    ...versionFields,
    ...softDeleteFields,
  }).index("by_tenant", ["tenantId"]),

  payments: defineTable({
    tenantId: v.id("tenants"),
    amountCents: v.number(),
    dateReceived: v.string(),
    method: paymentMethod,
    note: v.optional(v.string()),
    clientId: v.string(),
    loggedBy: v.id("accounts"),
    bouncedAt: v.optional(v.number()),
    ...versionFields,
    ...softDeleteFields,
  })
    .index("by_tenant", ["tenantId"])
    .index("by_client_id", ["clientId"]),

  debts: defineTable({
    tenantId: v.id("tenants"),
    kind: debtKind,
    amountCents: v.number(),
    date: v.string(),
    forMonth: v.optional(v.string()),
    note: v.optional(v.string()),
    createdBy: v.id("accounts"),
    ...versionFields,
    ...softDeleteFields,
  })
    .index("by_tenant", ["tenantId"])
    .index("by_tenant_for_month", ["tenantId", "forMonth"]),

  pushTokens: defineTable({
    accountId: v.id("accounts"),
    platform: pushPlatform,
    token: v.string(),
  })
    .index("by_account", ["accountId"])
    .index("by_token", ["token"]),

  notificationLog: defineTable({
    groupId: v.id("groups"),
    localDate: v.string(),
  }).index("by_group_local_date", ["groupId", "localDate"]),
});
