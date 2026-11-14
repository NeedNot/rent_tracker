import { v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { mutation, type QueryCtx } from "./_generated/server";
import { requirePermission } from "./access";
import { appError, validationFailed } from "./appError";
import { requireRetryOfSameTenant } from "./clientRetry";
import { validCustomDebtInput, validDebtEdit, validLateFeeInput } from "./debtInput";
import { requireConfirmedOrUnique } from "./duplicateEntries";
import { requireMayChangeEntry } from "./entryAccess";
import { requireCurrentEntry } from "./entryVersion";
import { requireName } from "./inputValidation";
import { loadTenantScope, requireActiveTenant, type TenantScope } from "./tenantScope";

interface DebtScope extends TenantScope {
  debt: Doc<"debts">;
}

const ARCHIVED_CHANGE = "change payments and charges";

const debtVersionArgs = { debtId: v.id("debts"), expectedVersion: v.number() };

export const addLateFee = mutation({
  args: {
    tenantId: v.id("tenants"),
    clientId: v.string(),
    amountCents: v.number(),
    date: v.string(),
    forMonth: v.string(),
    note: v.optional(v.string()),
    confirmed: v.optional(v.boolean()),
  },
  returns: v.id("debts"),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const { accountId } = await requirePermission(ctx, group._id, "debts", "addEdit");
    const earlier = await debtWithClientId(ctx, args.clientId);
    if (earlier !== null) {
      requireRetryOfSameTenant(earlier, tenant._id);
      return earlier._id;
    }
    requireActiveTenant(tenant, ARCHIVED_CHANGE);
    const lateFee = validLateFeeInput(args);
    if (!args.confirmed) {
      await requireConfirmedOrUnique(ctx, { tenantId: tenant._id, lateFeeMonth: lateFee.forMonth });
    }
    return await ctx.db.insert("debts", {
      tenantId: tenant._id,
      kind: "lateFee",
      ...lateFee,
      clientId: args.clientId,
      createdBy: accountId,
      version: 1,
    });
  },
});

export const addCustom = mutation({
  args: {
    tenantId: v.id("tenants"),
    clientId: v.string(),
    amountCents: v.number(),
    date: v.string(),
    note: v.string(),
  },
  returns: v.id("debts"),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const { accountId } = await requirePermission(ctx, group._id, "debts", "addEdit");
    const earlier = await debtWithClientId(ctx, args.clientId);
    if (earlier !== null) {
      requireRetryOfSameTenant(earlier, tenant._id);
      return earlier._id;
    }
    requireActiveTenant(tenant, ARCHIVED_CHANGE);
    return await ctx.db.insert("debts", {
      tenantId: tenant._id,
      kind: "custom",
      ...validCustomDebtInput(args),
      clientId: args.clientId,
      createdBy: accountId,
      version: 1,
    });
  },
});

export const edit = mutation({
  args: {
    ...debtVersionArgs,
    amountCents: v.number(),
    date: v.string(),
    forMonth: v.optional(v.string()),
    note: v.optional(v.string()),
  },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { debt, tenant, group } = await loadDebtScope(ctx, args.debtId);
    const membership = await requirePermission(ctx, group._id, "debts", "addEdit");
    await requireChangeable(ctx, { debt, tenant }, membership, args.expectedVersion);
    if (debt.deletedAt !== undefined) {
      throw validationFailed("This charge was cancelled. Restore it to change it.");
    }
    await ctx.db.patch(debt._id, {
      ...validDebtEdit(debt.kind, args),
      version: debt.version + 1,
      lastEditedAt: Date.now(),
      lastEditedBy: membership.accountId,
    });
    return null;
  },
});

const cancel = mutation({
  args: { ...debtVersionArgs, reason: v.string() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { debt, tenant, group } = await loadDebtScope(ctx, args.debtId);
    const membership = await requirePermission(ctx, group._id, "debts", "addEdit");
    await requireChangeable(ctx, { debt, tenant }, membership, args.expectedVersion);
    if (debt.deletedAt !== undefined) {
      throw validationFailed("This charge is already cancelled.");
    }
    await ctx.db.patch(debt._id, {
      deletedAt: Date.now(),
      deletedBy: membership.accountId,
      deleteReason: requireName(args.reason, "Reason"),
      version: debt.version + 1,
    });
    return null;
  },
});

export { cancel as delete };

export const restore = mutation({
  args: debtVersionArgs,
  returns: v.null(),
  handler: async (ctx, args) => {
    const { debt, tenant, group } = await loadDebtScope(ctx, args.debtId);
    const membership = await requirePermission(ctx, group._id, "debts", "addEdit");
    await requireChangeable(ctx, { debt, tenant }, membership, args.expectedVersion);
    if (debt.deletedAt === undefined) {
      throw validationFailed("This charge is not cancelled.");
    }
    await ctx.db.patch(debt._id, {
      deletedAt: undefined,
      deletedBy: undefined,
      deleteReason: undefined,
      version: debt.version + 1,
    });
    return null;
  },
});

async function loadDebtScope(ctx: QueryCtx, debtId: Id<"debts">): Promise<DebtScope> {
  const debt = await ctx.db.get(debtId);
  if (debt === null) {
    throw appError("NotFound", "This charge no longer exists.");
  }
  return { debt, ...(await loadTenantScope(ctx, debt.tenantId)) };
}

async function requireChangeable(
  ctx: QueryCtx,
  { debt, tenant }: { debt: Doc<"debts">; tenant: Doc<"tenants"> },
  membership: Doc<"memberships">,
  expectedVersion: number,
): Promise<void> {
  requireMayChangeEntry(membership, "debts", debt.createdBy);
  requireActiveTenant(tenant, ARCHIVED_CHANGE);
  await requireCurrentEntry(ctx, debt, expectedVersion, "charge");
}

function debtWithClientId(ctx: QueryCtx, clientId: string): Promise<Doc<"debts"> | null> {
  return ctx.db
    .query("debts")
    .withIndex("by_client_id", (q) => q.eq("clientId", clientId))
    .first();
}
