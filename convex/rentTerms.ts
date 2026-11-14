import { v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { mutation, type QueryCtx, query } from "./_generated/server";
import { requireMember, requirePermission } from "./access";
import { appError, validationFailed } from "./appError";
import { requireExpectedVersion } from "./editConflict";
import { type CalendarMonth, monthOf } from "./engine/calendarDate";
import type { RentTermRow } from "./engine/ledgerInput";
import { computeTenantLedger } from "./engine/tenantLedger";
import { requireName } from "./inputValidation";
import { localDateAt } from "./localDate";
import {
  isFirstTerm,
  type RentTermInput,
  rentTermInput,
  rentTermLevel,
  requireFreeStartMonth,
  validRentTermInput,
  withoutTerm,
} from "./rentTermRules";
import { tenantLedgerInput } from "./tenantLedgerAdapter";
import { loadTenantLedgerRows } from "./tenantLedgerRows";
import { loadTenantScope, requireActiveTenant, type TenantScope } from "./tenantScope";
import { tenantSummary, tenantSummaryOf } from "./tenantSummary";

interface RentTermScope extends TenantScope {
  term: Doc<"rentTerms">;
}

const PROPOSED_TERM_ID = "proposed";

const rentTermVersionArgs = { rentTermId: v.id("rentTerms"), expectedVersion: v.number() };

export const add = mutation({
  args: { tenantId: v.id("tenants"), ...rentTermInput.fields },
  returns: v.id("rentTerms"),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const term = validRentTermInput(args);
    await requirePermission(ctx, group._id, "rentTerms", rentTermLevel([term.startMonth], currentMonth(group)));
    requireActiveTenant(tenant);
    requireFreeStartMonth(tenant, await tenantTerms(ctx, tenant._id), term.startMonth);
    return await ctx.db.insert("rentTerms", { tenantId: tenant._id, ...term, version: 1 });
  },
});

export const edit = mutation({
  args: { ...rentTermVersionArgs, ...rentTermInput.fields },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { term, tenant, group } = await loadRentTermScope(ctx, args.rentTermId);
    const changed = validRentTermInput(args);
    const level = rentTermLevel([term.startMonth, changed.startMonth], currentMonth(group));
    const { accountId } = await requirePermission(ctx, group._id, "rentTerms", level);
    requireActiveTenant(tenant);
    await requireCurrentTerm(ctx, term, args.expectedVersion);
    const terms = await tenantTerms(ctx, tenant._id);
    if (isFirstTerm(term, terms) && changed.startMonth !== term.startMonth) {
      throw validationFailed("The first rent term starts in the move-in month. Change the move-in date to move it.", {
        field: "Start month",
      });
    }
    requireFreeStartMonth(tenant, withoutTerm(terms, term._id), changed.startMonth);
    await ctx.db.patch(term._id, {
      ...changed,
      version: term.version + 1,
      lastEditedAt: Date.now(),
      lastEditedBy: accountId,
    });
    return null;
  },
});

const softDelete = mutation({
  args: { ...rentTermVersionArgs, reason: v.string() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { term, tenant, group } = await loadRentTermScope(ctx, args.rentTermId);
    const { accountId } = await requirePermission(
      ctx,
      group._id,
      "rentTerms",
      rentTermLevel([term.startMonth], currentMonth(group)),
    );
    requireActiveTenant(tenant);
    await requireCurrentTerm(ctx, term, args.expectedVersion);
    if (isFirstTerm(term, await tenantTerms(ctx, tenant._id))) {
      throw validationFailed("A tenant's first rent term can't be deleted. Edit it instead.");
    }
    await ctx.db.patch(term._id, {
      deletedAt: Date.now(),
      deletedBy: accountId,
      deleteReason: requireName(args.reason, "Reason"),
      version: term.version + 1,
    });
    return null;
  },
});

export { softDelete as delete };

export const preview = query({
  args: {
    tenantId: v.id("tenants"),
    asOf: v.number(),
    proposed: v.object({ rentTermId: v.optional(v.id("rentTerms")), ...rentTermInput.fields }),
  },
  returns: v.object({ before: tenantSummary, after: tenantSummary }),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    await requireMember(ctx, group._id);
    const today = localDateAt(args.asOf, group.timeZone);
    const current = tenantLedgerInput({ group, tenant, rows: await loadTenantLedgerRows(ctx, tenant._id), today });
    const proposedTerms = args.proposed.rentTermId
      ? withTermChanged(current.rentTerms, args.proposed.rentTermId, validRentTermInput(args.proposed))
      : [...current.rentTerms, { id: PROPOSED_TERM_ID, ...validRentTermInput(args.proposed) }];
    return {
      before: tenantSummaryOf(tenant._id, computeTenantLedger(current).summary),
      after: tenantSummaryOf(tenant._id, computeTenantLedger({ ...current, rentTerms: proposedTerms }).summary),
    };
  },
});

async function loadRentTermScope(ctx: QueryCtx, rentTermId: Id<"rentTerms">): Promise<RentTermScope> {
  const term = await ctx.db.get(rentTermId);
  if (term === null) {
    throw rentTermNotFound();
  }
  return { term, ...(await loadTenantScope(ctx, term.tenantId)) };
}

async function tenantTerms(ctx: QueryCtx, tenantId: Id<"tenants">): Promise<Doc<"rentTerms">[]> {
  return await ctx.db
    .query("rentTerms")
    .withIndex("by_tenant", (q) => q.eq("tenantId", tenantId))
    .collect();
}

async function requireCurrentTerm(ctx: QueryCtx, term: Doc<"rentTerms">, expectedVersion: number): Promise<void> {
  const changedBy = term.deletedBy ?? term.lastEditedBy;
  await requireExpectedVersion(ctx, { version: term.version, changedBy }, expectedVersion, "rent term");
  if (term.deletedAt !== undefined) {
    throw rentTermDeleted();
  }
}

function withTermChanged(
  terms: readonly RentTermRow[],
  rentTermId: Id<"rentTerms">,
  changed: RentTermInput,
): RentTermRow[] {
  const term = terms.find((row) => row.id === rentTermId);
  if (term === undefined) {
    throw rentTermNotFound();
  }
  if (term.deletedAt !== undefined) {
    throw rentTermDeleted();
  }
  return terms.map((row) => (row.id === rentTermId ? { ...row, ...changed } : row));
}

function currentMonth(group: Doc<"groups">): CalendarMonth {
  return monthOf(localDateAt(Date.now(), group.timeZone));
}

function rentTermNotFound() {
  return appError("NotFound", "This rent term no longer exists.");
}

function rentTermDeleted() {
  return validationFailed("This rent term was deleted and can't be changed.");
}
