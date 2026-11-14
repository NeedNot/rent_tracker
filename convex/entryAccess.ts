import type { Doc, Id } from "./_generated/dataModel";
import { appError } from "./appError";

export type EntryCategory = "payments" | "debts";

export function requireMayChangeEntry(
  membership: Doc<"memberships">,
  category: EntryCategory,
  addedBy: Id<"accounts">,
): void {
  const mayChange =
    membership.role === "owner" ||
    membership[category] === "full" ||
    membership.accountId === addedBy ||
    membership.editOthers;
  if (!mayChange) {
    throw appError("PermissionDenied", "You can only change entries you added.");
  }
}
