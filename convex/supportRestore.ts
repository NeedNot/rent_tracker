import { type Infer, v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import { appError } from "./appError";
import { clearCancellation, requireCancelled } from "./entryRestore";
import { requireFreeStartMonth, withoutTerm } from "./rentTermRules";
import { loadTenantScope, requireActiveTenant } from "./tenantScope";

export const restorableKind = v.union(v.literal("payment"), v.literal("debt"), v.literal("rentTerm"));

type RestorableKind = Infer<typeof restorableKind>;

const ARCHIVED_CHANGE = "change payments and charges";

export async function restoreCancelledEntry(ctx: MutationCtx, kind: RestorableKind, id: string): Promise<void> {
  switch (kind) {
    case "payment":
      return await restorePayment(ctx, id);
    case "debt":
      return await restoreDebt(ctx, id);
    case "rentTerm":
      return await restoreRentTerm(ctx, id);
  }
}

async function restorePayment(ctx: MutationCtx, id: string): Promise<void> {
  const paymentId = ctx.db.normalizeId("payments", id);
  const payment = paymentId === null ? null : await ctx.db.get(paymentId);
  if (payment === null) {
    throw appError("NotFound", "This payment no longer exists.");
  }
  await requireWritableTenant(ctx, payment.tenantId, ARCHIVED_CHANGE);
  requireCancelled(payment, "payment");
  await clearCancellation(ctx, payment);
}

async function restoreDebt(ctx: MutationCtx, id: string): Promise<void> {
  const debtId = ctx.db.normalizeId("debts", id);
  const debt = debtId === null ? null : await ctx.db.get(debtId);
  if (debt === null) {
    throw appError("NotFound", "This charge no longer exists.");
  }
  await requireWritableTenant(ctx, debt.tenantId, ARCHIVED_CHANGE);
  requireCancelled(debt, "charge");
  await clearCancellation(ctx, debt);
}

async function restoreRentTerm(ctx: MutationCtx, id: string): Promise<void> {
  const termId = ctx.db.normalizeId("rentTerms", id);
  const term = termId === null ? null : await ctx.db.get(termId);
  if (term === null) {
    throw appError("NotFound", "This rent term no longer exists.");
  }
  const tenant = await requireWritableTenant(ctx, term.tenantId);
  requireCancelled(term, "rent term");
  const terms = await ctx.db
    .query("rentTerms")
    .withIndex("by_tenant", (q) => q.eq("tenantId", tenant._id))
    .collect();
  requireFreeStartMonth(tenant, withoutTerm(terms, term._id), term.startMonth);
  await clearCancellation(ctx, term);
}

async function requireWritableTenant(
  ctx: MutationCtx,
  tenantId: Id<"tenants">,
  change?: string,
): Promise<Doc<"tenants">> {
  const { tenant, group } = await loadTenantScope(ctx, tenantId);
  if (group.archivedAt !== undefined) {
    throw appError("NotFound", "This group no longer exists.");
  }
  requireActiveTenant(tenant, change);
  return tenant;
}
