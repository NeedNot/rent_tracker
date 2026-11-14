import type { WithoutSystemFields } from "convex/server";
import { ConvexError } from "convex/values";
import { convexTest, type TestConvexForDataModel } from "convex-test";
import { api } from "./_generated/api";
import type { DataModel, Doc, Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import type { AppErrorData } from "./appError";
import schema from "./schema";
import { modules } from "./test.setup";

type GroupFields = WithoutSystemFields<Doc<"groups">>;
type TenantFields = WithoutSystemFields<Doc<"tenants">>;

export type Backend = TestConvexForDataModel<DataModel>;

export const owner = { subject: "auth|dana", email: "dana@example.com", name: "Dana" };

export const RENT = 150000;

export const newTenant = { name: "Apt 3B", moveIn: "2026-01-01", amountCents: RENT, dueDay: 1 };

export async function newBackend(): Promise<Backend> {
  const t = convexTest(schema, modules);
  await t.run((ctx) => insertAccount(ctx, owner));
  return t.withIdentity(owner);
}

export function losAngelesTime(year: number, month: number, day: number, hour = 12): number {
  const HOURS_BEHIND_UTC_IN_WINTER = 8;
  return Date.UTC(year, month - 1, day, hour + HOURS_BEHIND_UTC_IN_WINTER);
}

export async function insertAccount(
  ctx: MutationCtx,
  identity: { subject: string; email: string; name: string },
): Promise<Id<"accounts">> {
  const existing = await ctx.db
    .query("accounts")
    .withIndex("by_auth_subject", (q) => q.eq("authSubject", identity.subject))
    .unique();
  if (existing !== null) {
    return existing._id;
  }
  return await ctx.db.insert("accounts", {
    authSubject: identity.subject,
    displayName: identity.name,
    email: identity.email,
    version: 1,
  });
}

export async function insertGroup(ctx: MutationCtx, fields: Partial<GroupFields> = {}): Promise<Id<"groups">> {
  const ownerId = await insertAccount(ctx, owner);
  const groupId = await ctx.db.insert("groups", {
    name: "1st Street",
    timeZone: "America/Los_Angeles",
    paymentOrder: "rentFirst",
    graceDays: 5,
    ownerId,
    version: 1,
    ...fields,
  });
  await insertMembership(ctx, groupId, ownerId, { role: "owner" });
  return groupId;
}

export async function insertMembership(
  ctx: MutationCtx,
  groupId: Id<"groups">,
  accountId: Id<"accounts">,
  fields: Partial<WithoutSystemFields<Doc<"memberships">>> = {},
): Promise<Id<"memberships">> {
  return await ctx.db.insert("memberships", {
    groupId,
    accountId,
    role: "member",
    tenants: "read",
    rentTerms: "read",
    payments: "read",
    debts: "read",
    members: "read",
    settings: "read",
    editOthers: false,
    notificationsOn: true,
    version: 1,
    ...fields,
  });
}

export async function insertTenant(
  ctx: MutationCtx,
  groupId: Id<"groups">,
  fields: Partial<TenantFields> = {},
): Promise<Id<"tenants">> {
  return await ctx.db.insert("tenants", {
    groupId,
    name: "Apt 3B",
    moveIn: "2026-03-01",
    status: "active",
    version: 1,
    ...fields,
  });
}

export async function insertRentTerm(
  ctx: MutationCtx,
  tenantId: Id<"tenants">,
  term: { startMonth: string; amountCents: number; dueDay: number },
): Promise<Id<"rentTerms">> {
  return await ctx.db.insert("rentTerms", { tenantId, ...term, version: 1 });
}

export async function insertPayment(
  ctx: MutationCtx,
  tenantId: Id<"tenants">,
  payment: { amountCents: number; dateReceived: string },
): Promise<Id<"payments">> {
  return await ctx.db.insert("payments", {
    tenantId,
    ...payment,
    method: "zelle",
    clientId: `${tenantId}-${payment.dateReceived}`,
    loggedBy: await insertAccount(ctx, owner),
    version: 1,
  });
}

export async function insertTenantOwingRent(
  ctx: MutationCtx,
  groupId: Id<"groups">,
  rentCents: number,
  fields: Partial<TenantFields> = {},
): Promise<Id<"tenants">> {
  const tenantId = await insertTenant(ctx, groupId, fields);
  const startMonth = (fields.moveIn ?? "2026-03-01").slice(0, 7);
  await insertRentTerm(ctx, tenantId, { startMonth, amountCents: rentCents, dueDay: 1 });
  return tenantId;
}

export async function seedGroup(t: Backend, fields: Partial<GroupFields> = {}): Promise<Id<"groups">> {
  return await t.run((ctx) => insertGroup(ctx, fields));
}

export async function seedTenant(t: Backend, fields: Partial<typeof newTenant> = {}): Promise<Id<"tenants">> {
  const groupId = await seedGroup(t);
  return await t.mutation(api.tenants.add, { groupId, ...newTenant, ...fields });
}

export async function tenantRow(t: Backend, tenantId: Id<"tenants">): Promise<Doc<"tenants">> {
  const tenant = await t.run((ctx) => ctx.db.get(tenantId));
  if (tenant === null) {
    throw new Error("The tenant row is missing");
  }
  return tenant;
}

export async function termRows(t: Backend, tenantId: Id<"tenants">): Promise<Doc<"rentTerms">[]> {
  return await t.run((ctx) =>
    ctx.db
      .query("rentTerms")
      .withIndex("by_tenant", (q) => q.eq("tenantId", tenantId))
      .collect(),
  );
}

export async function firstTermRow(t: Backend, tenantId: Id<"tenants">): Promise<Doc<"rentTerms">> {
  const [firstTerm] = await termRows(t, tenantId);
  if (firstTerm === undefined) {
    throw new Error("The tenant has no rent term");
  }
  return firstTerm;
}

export async function termRow(t: Backend, rentTermId: Id<"rentTerms">): Promise<Doc<"rentTerms">> {
  const term = await t.run((ctx) => ctx.db.get(rentTermId));
  if (term === null) {
    throw new Error("The rent term row is missing");
  }
  return term;
}

export async function failureOf(call: Promise<unknown>): Promise<AppErrorData> {
  try {
    await call;
  } catch (error) {
    if (error instanceof ConvexError) {
      return error.data as AppErrorData;
    }
    throw error;
  }
  throw new Error("Expected the call to fail");
}
