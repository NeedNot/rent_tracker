import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { requireMember } from "./access";
import { appError } from "./appError";

export interface TenantScope {
  tenant: Doc<"tenants">;
  group: Doc<"groups">;
}

export async function loadActiveGroup(ctx: QueryCtx, groupId: Id<"groups">): Promise<Doc<"groups">> {
  const group = await ctx.db.get(groupId);
  if (group === null || group.archivedAt !== undefined) {
    throw appError("NotFound", "This group no longer exists.");
  }
  return group;
}

export async function loadTenantScope(ctx: QueryCtx, tenantId: Id<"tenants">): Promise<TenantScope> {
  const tenant = await ctx.db.get(tenantId);
  if (tenant === null) {
    throw appError("NotFound", "This tenant no longer exists.");
  }
  const group = await ctx.db.get(tenant.groupId);
  if (group === null) {
    throw appError("NotFound", "This group no longer exists.");
  }
  return { tenant, group };
}

export function requireActiveTenant(tenant: Doc<"tenants">, change = "change rent"): void {
  if (tenant.status === "archived") {
    throw appError("TenantArchived", `This tenant is archived. Restore the tenant to ${change}.`);
  }
}

export async function requireTenantInExpectedGroup(
  ctx: QueryCtx,
  tenant: Doc<"tenants">,
  expectedGroupId: Id<"groups"> | undefined,
): Promise<void> {
  if (expectedGroupId === undefined || tenant.groupId === expectedGroupId) {
    return;
  }
  await requireMember(ctx, expectedGroupId);
  throw appError("TenantMoved", "This tenant was moved to another group.");
}
