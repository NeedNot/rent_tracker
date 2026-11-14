import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
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
  return { tenant, group: await loadActiveGroup(ctx, tenant.groupId) };
}

export function requireActiveTenant(tenant: Doc<"tenants">, change = "change rent"): void {
  if (tenant.status === "archived") {
    throw appError("TenantArchived", `This tenant is archived. Restore the tenant to ${change}.`);
  }
}
