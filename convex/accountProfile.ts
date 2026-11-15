import type { Doc } from "./_generated/dataModel";

export function profileOf(account: Doc<"accounts">) {
  if (account.email === undefined) {
    throw new Error(`Active account ${account._id} has no email`);
  }
  return { accountId: account._id, displayName: account.displayName, email: account.email };
}
