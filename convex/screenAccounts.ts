import type { Infer } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import type { TenantLedgerRows } from "./tenantLedgerRows";
import type { accountName } from "./validators";

type AccountName = Infer<typeof accountName>;

export async function loadAccountNames(ctx: QueryCtx, rows: TenantLedgerRows): Promise<AccountName[]> {
  const accountIds = new Set([
    ...rows.rentTerms.flatMap((term) => [term.lastEditedBy, term.deletedBy]),
    ...rows.payments.flatMap(paymentAccountIds),
    ...rows.debts.flatMap((debt) => [debt.createdBy, debt.lastEditedBy, debt.deletedBy]),
  ]);
  const accounts = await Promise.all([...accountIds].filter(isDefined).map((accountId) => ctx.db.get(accountId)));
  return accounts.filter(isFound).map((account) => ({ accountId: account._id, displayName: account.displayName }));
}

function paymentAccountIds(payment: Doc<"payments">): (Id<"accounts"> | undefined)[] {
  return [payment.loggedBy, payment.lastEditedBy, payment.deletedBy, payment.bouncedBy];
}

function isDefined<T>(value: T | undefined): value is T {
  return value !== undefined;
}

function isFound(account: Doc<"accounts"> | null): account is Doc<"accounts"> {
  return account !== null;
}
