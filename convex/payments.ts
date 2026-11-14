import { v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { mutation, type QueryCtx, query } from "./_generated/server";
import { requireMember, requirePermission } from "./access";
import { appError, validationFailed } from "./appError";
import { calendarState } from "./calendarMonthEntity";
import { LATE_FEE_CLIENT_ID_SUFFIX, requireRetryOfSameTenant } from "./clientRetry";
import { validLateFeeInput } from "./debtInput";
import { requireConfirmedOrUnique } from "./duplicateEntries";
import type { CalendarDate } from "./engine/calendarDate";
import { monthStateChanges } from "./engine/monthStateChanges";
import { computeTenantLedger } from "./engine/tenantLedger";
import { requireMayChangeEntry } from "./entryAccess";
import { requireCurrentEntry } from "./entryVersion";
import { requireName } from "./inputValidation";
import { localDateAt } from "./localDate";
import { requireEditable } from "./paymentEditable";
import { paymentInput, validPaymentInput } from "./paymentInput";
import { withProposedPayment } from "./paymentPreview";
import { tenantLedgerInput } from "./tenantLedgerAdapter";
import { loadTenantLedgerRows } from "./tenantLedgerRows";
import { loadTenantScope, requireActiveTenant, type TenantScope } from "./tenantScope";

interface PaymentScope extends TenantScope {
  payment: Doc<"payments">;
}

const ARCHIVED_CHANGE = "change payments and charges";

const paymentVersionArgs = { paymentId: v.id("payments"), expectedVersion: v.number() };

export const log = mutation({
  args: {
    tenantId: v.id("tenants"),
    clientId: v.string(),
    ...paymentInput.fields,
    lateFee: v.optional(v.object({ amountCents: v.number(), forMonth: v.string() })),
    confirmed: v.optional(v.boolean()),
  },
  returns: v.object({ paymentId: v.id("payments"), lateFeeId: v.optional(v.id("debts")) }),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    const { accountId } = await requirePermission(ctx, group._id, "payments", "addEdit");
    if (args.lateFee !== undefined) {
      await requirePermission(ctx, group._id, "debts", "addEdit");
    }
    const earlier = await paymentWithClientId(ctx, args.clientId);
    if (earlier !== null) {
      requireRetryOfSameTenant(earlier, tenant._id);
      return { paymentId: earlier._id, lateFeeId: await earlierLateFeeId(ctx, args.clientId) };
    }
    requireActiveTenant(tenant, ARCHIVED_CHANGE);
    const payment = validPaymentInput(args, groupToday(group));
    const lateFee = args.lateFee && validLateFeeInput({ ...args.lateFee, date: groupToday(group) });
    if (!args.confirmed) {
      await requireConfirmedOrUnique(ctx, {
        tenantId: tenant._id,
        payment,
        lateFeeMonth: lateFee?.forMonth,
      });
    }
    const paymentId = await ctx.db.insert("payments", {
      tenantId: tenant._id,
      ...payment,
      clientId: args.clientId,
      loggedBy: accountId,
      version: 1,
    });
    if (payment.method !== undefined) {
      await ctx.db.patch(tenant._id, { lastPaymentMethod: payment.method });
    }
    if (lateFee === undefined) {
      return { paymentId };
    }
    const lateFeeId = await ctx.db.insert("debts", {
      tenantId: tenant._id,
      kind: "lateFee",
      ...lateFee,
      clientId: `${args.clientId}${LATE_FEE_CLIENT_ID_SUFFIX}`,
      createdBy: accountId,
      version: 1,
    });
    return { paymentId, lateFeeId };
  },
});

export const edit = mutation({
  args: { ...paymentVersionArgs, ...paymentInput.fields },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { payment, tenant, group } = await loadPaymentScope(ctx, args.paymentId);
    const membership = await requirePermission(ctx, group._id, "payments", "addEdit");
    await requireChangeable(ctx, { payment, tenant }, membership, args.expectedVersion);
    requireEditable(payment);
    await ctx.db.patch(payment._id, {
      ...validPaymentInput(args, groupToday(group)),
      version: payment.version + 1,
      lastEditedAt: Date.now(),
      lastEditedBy: membership.accountId,
    });
    return null;
  },
});

export const preview = query({
  args: {
    tenantId: v.id("tenants"),
    asOf: v.number(),
    proposed: v.object({
      paymentId: v.optional(v.id("payments")),
      ...paymentInput.pick("amountCents", "dateReceived").fields,
    }),
  },
  returns: v.object({
    changes: v.array(v.object({ month: v.string(), before: calendarState, after: calendarState })),
  }),
  handler: async (ctx, args) => {
    const { tenant, group } = await loadTenantScope(ctx, args.tenantId);
    await requireMember(ctx, group._id);
    const today = localDateAt(args.asOf, group.timeZone);
    const proposed = validPaymentInput({ ...args.proposed }, today);
    const current = tenantLedgerInput({ group, tenant, rows: await loadTenantLedgerRows(ctx, tenant._id), today });
    const changed = {
      ...current,
      payments: withProposedPayment(current.payments, { paymentId: args.proposed.paymentId, ...proposed }),
    };
    return {
      changes: monthStateChanges(
        computeTenantLedger(current).rentPeriods,
        computeTenantLedger(changed).rentPeriods,
        today,
      ),
    };
  },
});

const cancel = mutation({
  args: { ...paymentVersionArgs, reason: v.string() },
  returns: v.null(),
  handler: async (ctx, args) => {
    const { payment, tenant, group } = await loadPaymentScope(ctx, args.paymentId);
    const membership = await requirePermission(ctx, group._id, "payments", "addEdit");
    await requireChangeable(ctx, { payment, tenant }, membership, args.expectedVersion);
    if (payment.deletedAt !== undefined) {
      throw validationFailed("This payment is already cancelled.");
    }
    await ctx.db.patch(payment._id, {
      deletedAt: Date.now(),
      deletedBy: membership.accountId,
      deleteReason: requireName(args.reason, "Reason"),
      version: payment.version + 1,
    });
    return null;
  },
});

export { cancel as delete };

export const restore = mutation({
  args: paymentVersionArgs,
  returns: v.null(),
  handler: async (ctx, args) => {
    const { payment, tenant, group } = await loadPaymentScope(ctx, args.paymentId);
    const membership = await requirePermission(ctx, group._id, "payments", "addEdit");
    await requireChangeable(ctx, { payment, tenant }, membership, args.expectedVersion);
    const version = payment.version + 1;
    if (payment.deletedAt !== undefined) {
      await ctx.db.patch(payment._id, { deletedAt: undefined, deletedBy: undefined, deleteReason: undefined, version });
    } else if (payment.bouncedAt !== undefined) {
      await ctx.db.patch(payment._id, { bouncedAt: undefined, bouncedBy: undefined, version });
    } else {
      throw validationFailed("This payment is not cancelled or bounced.");
    }
    return null;
  },
});

export const markBounced = mutation({
  args: paymentVersionArgs,
  returns: v.null(),
  handler: async (ctx, args) => {
    const { payment, tenant, group } = await loadPaymentScope(ctx, args.paymentId);
    const membership = await requirePermission(ctx, group._id, "payments", "addEdit");
    await requireChangeable(ctx, { payment, tenant }, membership, args.expectedVersion);
    if (payment.deletedAt !== undefined) {
      throw validationFailed("This payment was cancelled. Restore it before marking it bounced.");
    }
    if (payment.bouncedAt !== undefined) {
      throw validationFailed("This payment is already marked bounced.");
    }
    await ctx.db.patch(payment._id, {
      bouncedAt: Date.now(),
      bouncedBy: membership.accountId,
      version: payment.version + 1,
    });
    return null;
  },
});

async function loadPaymentScope(ctx: QueryCtx, paymentId: Id<"payments">): Promise<PaymentScope> {
  const payment = await ctx.db.get(paymentId);
  if (payment === null) {
    throw appError("NotFound", "This payment no longer exists.");
  }
  return { payment, ...(await loadTenantScope(ctx, payment.tenantId)) };
}

async function requireChangeable(
  ctx: QueryCtx,
  { payment, tenant }: { payment: Doc<"payments">; tenant: Doc<"tenants"> },
  membership: Doc<"memberships">,
  expectedVersion: number,
): Promise<void> {
  requireMayChangeEntry(membership, "payments", payment.loggedBy);
  requireActiveTenant(tenant, ARCHIVED_CHANGE);
  await requireCurrentEntry(ctx, payment, expectedVersion, "payment");
}

function paymentWithClientId(ctx: QueryCtx, clientId: string): Promise<Doc<"payments"> | null> {
  return ctx.db
    .query("payments")
    .withIndex("by_client_id", (q) => q.eq("clientId", clientId))
    .first();
}

async function earlierLateFeeId(ctx: QueryCtx, clientId: string): Promise<Id<"debts"> | undefined> {
  const lateFee = await ctx.db
    .query("debts")
    .withIndex("by_client_id", (q) => q.eq("clientId", `${clientId}${LATE_FEE_CLIENT_ID_SUFFIX}`))
    .first();
  return lateFee?._id;
}

function groupToday(group: Doc<"groups">): CalendarDate {
  return localDateAt(Date.now(), group.timeZone);
}
