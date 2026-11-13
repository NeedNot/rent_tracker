import { type Infer, v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import type { tenantSummary } from "./tenantSummary";

export const groupSummary = v.object({
  groupId: v.id("groups"),
  activeTenantCount: v.number(),
  lateTenantCount: v.number(),
  totalOwedCents: v.number(),
  archivedTenantCount: v.number(),
  archivedOwedCents: v.number(),
});

type TenantStatusRow = Pick<Doc<"tenants">, "_id" | "status">;
type TenantSummaryRow = Infer<typeof tenantSummary>;

export function groupSummaryOf(
  groupId: Id<"groups">,
  tenants: readonly TenantStatusRow[],
  tenantSummaries: readonly TenantSummaryRow[],
): Infer<typeof groupSummary> {
  const summariesWithStatus = (status: TenantStatusRow["status"]) => {
    const tenantIds = new Set(tenants.filter((tenant) => tenant.status === status).map((tenant) => tenant._id));
    return tenantSummaries.filter((summary) => tenantIds.has(summary.tenantId));
  };
  const active = summariesWithStatus("active");
  const archived = summariesWithStatus("archived");
  return {
    groupId,
    activeTenantCount: active.length,
    lateTenantCount: active.filter((summary) => summary.state === "late").length,
    totalOwedCents: owedCents(active),
    archivedTenantCount: archived.length,
    archivedOwedCents: owedCents(archived),
  };
}

function owedCents(summaries: readonly TenantSummaryRow[]): number {
  return summaries.reduce(
    (total, summary) => total + summary.rentOwedCents + summary.lateFeesOwedCents + summary.otherOwedCents,
    0,
  );
}
