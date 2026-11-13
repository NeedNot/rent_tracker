import type { WithoutSystemFields } from "convex/server";
import type { Doc, Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import { placeholderAccountId } from "./authPlaceholder";

type GroupFields = WithoutSystemFields<Doc<"groups">>;
type TenantFields = WithoutSystemFields<Doc<"tenants">>;

export async function insertGroup(ctx: MutationCtx, fields: Partial<GroupFields> = {}): Promise<Id<"groups">> {
  return await ctx.db.insert("groups", {
    name: "1st Street",
    timeZone: "America/Los_Angeles",
    paymentOrder: "rentFirst",
    graceDays: 5,
    ownerId: await placeholderAccountId(ctx),
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
    loggedBy: await placeholderAccountId(ctx),
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
