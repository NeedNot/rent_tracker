import { type ApiFromModules, anyApi, type WithoutSystemFields } from "convex/server";
import { ConvexError } from "convex/values";
import { convexTest, type TestConvex } from "convex-test";
import type { Doc, Id } from "./_generated/dataModel";
import type { AppErrorData } from "./appError";
import { placeholderAccountId } from "./authPlaceholder";
import type * as rentTerms from "./rentTerms";
import schema from "./schema";
import type * as tenants from "./tenants";
import { modules } from "./test.setup";

export const api = anyApi as unknown as ApiFromModules<{ tenants: typeof tenants; rentTerms: typeof rentTerms }>;

export type Backend = TestConvex<typeof schema>;

export const RENT = 150000;

export const newTenant = { name: "Apt 3B", moveIn: "2026-01-01", amountCents: RENT, dueDay: 1 };

export function losAngelesTime(year: number, month: number, day: number, hour = 12): number {
  const HOURS_BEHIND_UTC_IN_WINTER = 8;
  return Date.UTC(year, month - 1, day, hour + HOURS_BEHIND_UTC_IN_WINTER);
}

export function newBackend(): Backend {
  return convexTest(schema, modules);
}

export async function seedGroup(
  t: Backend,
  fields: Partial<WithoutSystemFields<Doc<"groups">>> = {},
): Promise<Id<"groups">> {
  return await t.run(async (ctx) =>
    ctx.db.insert("groups", {
      name: "1st Street",
      timeZone: "America/Los_Angeles",
      paymentOrder: "rentFirst",
      graceDays: 5,
      ownerId: await placeholderAccountId(ctx),
      version: 1,
      ...fields,
    }),
  );
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
