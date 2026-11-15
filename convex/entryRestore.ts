import type { Doc } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import { validationFailed } from "./appError";

type CancellableEntry = Doc<"payments"> | Doc<"debts"> | Doc<"rentTerms">;

export function requireCancelled(entry: CancellableEntry, entity: string): void {
  if (entry.deletedAt === undefined) {
    throw validationFailed(`This ${entity} is not cancelled.`);
  }
}

export async function clearCancellation(ctx: MutationCtx, entry: CancellableEntry): Promise<void> {
  await ctx.db.patch(entry._id, {
    deletedAt: undefined,
    deletedBy: undefined,
    deleteReason: undefined,
    version: entry.version + 1,
  });
}
