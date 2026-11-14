import { v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { type MutationCtx, mutation, type QueryCtx, query } from "./_generated/server";
import { requireMember, requirePermission } from "./access";
import { allocationRow, allocationRowsOf } from "./allocationEntity";
import { validationFailed } from "./appError";
import { calendarMonth, calendarMonthsOf } from "./calendarMonthEntity";
import { debtDoc, groupDoc, membershipDoc, paymentDoc, rentTermDoc, tenantDoc } from "./documentValidators";
import { requireExpectedVersion } from "./editConflict";
import { type CalendarMonth, monthOf } from "./engine/calendarDate";
import { computeTenantLedger } from "./engine/tenantLedger";
import { requireCalendarYear } from "./inputValidation";
import { ledgerRow, ledgerRowsOf } from "./ledgerRow";
import { localDateAt } from "./localDate";
import { activeTerms, firstTermOf, rentTermInput, validRentTermInput, withoutTerm } from "./rentTermRules";
import { loadAccountNames } from "./screenAccounts";
import { tenantDetails, validTenantDetails } from "./tenantDetails";
import { tenantLedgerInput } from "./tenantLedgerAdapter";
import { loadTenantLedgerRows } from "./tenantLedgerRows";
import { loadActiveGroup, loadTenantScope } from "./tenantScope";
import { tenantSummary, tenantSummaryOf } from "./tenantSummary";
import { accountName } from "./validators";

interface EditStamp {
  lastEditedAt: number;
  lastEditedBy: Id<"accounts">;
}

const tenantVersionArgs = { tenantId: v.id("tenants"), expectedVersion: v.number() };

export const add = mutation({
  args: {
    groupId: v.id("groups"),
    ...tenantDetails.fields,
    ...rentTermInput.pick("amountCents", "dueDay").fields,
  },
  returns: v.id("tenants"),
  handler: async (ctx, args) => {
    await requirePermission(ctx, args.groupId, "tenants", "addEdit");
    const group = await loadActiveGroup(ctx, args.groupId);
    const details = validTenantDetails(args);
    const firstTerm = validRentTermInput({
      startMonth: monthOf(details.moveIn),
      amountCents: args.amountCents,
      dueDay: args.dueDay,
    });
    const tenantId = await ctx.db.insert("tenants", { groupId: group._id, ...details, status: "active", version: 1 });
    await ctx.db.insert("rentTerms", { tenantId, ...firstTerm, version: 1 });
    return tenantId;
  },
});

export const edit = mutation({
  args: { ...tenantVersionArgs, ...tenantDetails.fields },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const { accountId } = await requirePermission(ctx, group._id, "tenants", "addEdit");
    await requireCurrentTenant(ctx, tenant, args.expectedVersion);
    const details = validTenantDetails(args);
    const stamp = editStamp(accountId);
    if (monthOf(details.moveIn) !== monthOf(tenant.moveIn)) {
      await moveFirstTerm(ctx, tenant._id, monthOf(details.moveIn), stamp);
    }
    await ctx.db.patch(tenant._id, { ...details, version: tenant.version + 1, ...stamp });
    return null;
  },
});

export const archive = mutation({
  args: tenantVersionArgs,
  returns: v.null(),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const { accountId } = await requirePermission(ctx, group._id, "tenants", "full");
    await requireCurrentTenant(ctx, tenant, args.expectedVersion);
    if (tenant.status === "archived") {
      throw validationFailed("This tenant is already archived.");
    }
    await ctx.db.patch(tenant._id, { status: "archived", version: tenant.version + 1, ...editStamp(accountId) });
    return null;
  },
});

export const restore = mutation({
  args: tenantVersionArgs,
  returns: v.null(),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const { accountId } = await requirePermission(ctx, group._id, "tenants", "full");
    await requireCurrentTenant(ctx, tenant, args.expectedVersion);
    if (tenant.status === "active") {
      throw validationFailed("This tenant is not archived.");
    }
    await ctx.db.patch(tenant._id, { status: "active", version: tenant.version + 1, ...editStamp(accountId) });
    return null;
  },
});

export const screen = query({
  args: { tenantId: v.id("tenants"), asOf: v.number() },
  returns: v.object({
    groups: v.array(groupDoc),
    tenants: v.array(tenantDoc),
    rentTerms: v.array(rentTermDoc),
    payments: v.array(paymentDoc),
    debts: v.array(debtDoc),
    accounts: v.array(accountName),
    memberships: v.array(membershipDoc),
    tenantSummaries: v.array(tenantSummary),
    ledgerRows: v.array(ledgerRow),
    calendarMonths: v.array(calendarMonth),
    allocations: v.array(allocationRow),
  }),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const membership = await requireMember(ctx, group._id);
    const today = localDateAt(args.asOf, group.timeZone);
    const rows = await loadTenantLedgerRows(ctx, tenant._id);
    const input = tenantLedgerInput({ group, tenant, rows, today });
    const ledger = computeTenantLedger(input);
    return {
      groups: [group],
      tenants: [tenant],
      rentTerms: rows.rentTerms,
      payments: rows.payments,
      debts: rows.debts,
      accounts: await loadAccountNames(ctx, rows),
      memberships: [membership],
      tenantSummaries: [tenantSummaryOf(tenant._id, ledger.summary)],
      ledgerRows: ledgerRowsOf(tenant._id, input, ledger),
      calendarMonths: calendarMonthsOf(tenant._id, ledger.calendarMonths),
      allocations: allocationRowsOf(tenant._id, ledger.allocations),
    };
  },
});

export const calendar = query({
  args: { tenantId: v.id("tenants"), year: v.number(), asOf: v.number() },
  returns: v.object({ calendarMonths: v.array(calendarMonth) }),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    await requireMember(ctx, group._id);
    const calendarYear = requireCalendarYear(args.year);
    const today = localDateAt(args.asOf, group.timeZone);
    const rows = await loadTenantLedgerRows(ctx, tenant._id);
    const ledger = computeTenantLedger(tenantLedgerInput({ group, tenant, rows, today, calendarYear }));
    return { calendarMonths: calendarMonthsOf(tenant._id, ledger.calendarMonths) };
  },
});

async function requireCurrentTenant(ctx: QueryCtx, tenant: Doc<"tenants">, expectedVersion: number): Promise<void> {
  await requireExpectedVersion(
    ctx,
    { version: tenant.version, changedBy: tenant.lastEditedBy },
    expectedVersion,
    "tenant",
  );
}

function editStamp(accountId: Id<"accounts">): EditStamp {
  return { lastEditedAt: Date.now(), lastEditedBy: accountId };
}

async function moveFirstTerm(
  ctx: MutationCtx,
  tenantId: Id<"tenants">,
  moveInMonth: CalendarMonth,
  stamp: EditStamp,
): Promise<void> {
  const terms = await ctx.db
    .query("rentTerms")
    .withIndex("by_tenant", (q) => q.eq("tenantId", tenantId))
    .collect();
  const firstTerm = firstTermOf(terms);
  if (firstTerm === undefined) {
    return;
  }
  const laterTerms = activeTerms(withoutTerm(terms, firstTerm._id));
  if (laterTerms.some((term) => term.startMonth <= moveInMonth)) {
    throw validationFailed(
      "A rent change starts in or before the new move-in month. Edit or delete that rent term first.",
      { field: "Move-in date" },
    );
  }
  await ctx.db.patch(firstTerm._id, { startMonth: moveInMonth, version: firstTerm.version + 1, ...stamp });
}
