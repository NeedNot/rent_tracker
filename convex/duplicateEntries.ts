import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { appError } from "./appError";
import { monthName, shortDate } from "./dateText";
import { addDays, type CalendarDate, type CalendarMonth } from "./engine/calendarDate";

export const DUPLICATE_PAYMENT_WINDOW_DAYS = 3;

const DELETED_USER_NAME = "Deleted user";

export type DuplicateMatch = {
  kind: "payment" | "lateFee";
  id: string;
  date: CalendarDate;
  amountCents: number;
  byName: string;
  byAccountId: Id<"accounts">;
  method?: NonNullable<Doc<"payments">["method"]>;
};

export interface DuplicateQuery {
  tenantId: Id<"tenants">;
  payment?: { amountCents: number; dateReceived: CalendarDate };
  lateFeeMonth?: CalendarMonth;
}

export async function requireConfirmedOrUnique(ctx: QueryCtx, query: DuplicateQuery): Promise<void> {
  const payments = await similarPayments(ctx, query);
  const lateFees = await similarLateFees(ctx, query);
  if (payments.length + lateFees.length === 0) {
    return;
  }
  const sentences = [
    ...payments.map((match) => `A similar payment was logged on ${shortDate(match.date)}.`),
    ...(query.lateFeeMonth !== undefined && lateFees.length > 0
      ? [`A late fee for ${monthName(query.lateFeeMonth)} was already added.`]
      : []),
  ];
  throw appError("NeedsConfirmation", `${sentences.join(" ")} Save anyway?`, { matches: [...payments, ...lateFees] });
}

async function similarPayments(ctx: QueryCtx, { tenantId, payment }: DuplicateQuery): Promise<DuplicateMatch[]> {
  if (payment === undefined) {
    return [];
  }
  const earliest = addDays(payment.dateReceived, -DUPLICATE_PAYMENT_WINDOW_DAYS);
  const latest = addDays(payment.dateReceived, DUPLICATE_PAYMENT_WINDOW_DAYS);
  const rows = await ctx.db
    .query("payments")
    .withIndex("by_tenant", (q) => q.eq("tenantId", tenantId))
    .collect();
  const similar = rows.filter(
    (row) =>
      row.deletedAt === undefined &&
      row.bouncedAt === undefined &&
      row.amountCents === payment.amountCents &&
      row.dateReceived >= earliest &&
      row.dateReceived <= latest,
  );
  return await Promise.all(
    similar.map(
      async (row): Promise<DuplicateMatch> => ({
        kind: "payment",
        id: row._id,
        date: row.dateReceived,
        amountCents: row.amountCents,
        byName: await nameOf(ctx, row.loggedBy),
        byAccountId: row.loggedBy,
        ...(row.method !== undefined && { method: row.method }),
      }),
    ),
  );
}

async function similarLateFees(ctx: QueryCtx, { tenantId, lateFeeMonth }: DuplicateQuery): Promise<DuplicateMatch[]> {
  if (lateFeeMonth === undefined) {
    return [];
  }
  const rows = await ctx.db
    .query("debts")
    .withIndex("by_tenant_for_month", (q) => q.eq("tenantId", tenantId).eq("forMonth", lateFeeMonth))
    .collect();
  const similar = rows.filter((row) => row.kind === "lateFee" && row.deletedAt === undefined);
  return await Promise.all(
    similar.map(
      async (row): Promise<DuplicateMatch> => ({
        kind: "lateFee",
        id: row._id,
        date: row.date,
        amountCents: row.amountCents,
        byName: await nameOf(ctx, row.createdBy),
        byAccountId: row.createdBy,
      }),
    ),
  );
}

async function nameOf(ctx: QueryCtx, accountId: Id<"accounts">): Promise<string> {
  const account = await ctx.db.get(accountId);
  return account?.displayName ?? DELETED_USER_NAME;
}
