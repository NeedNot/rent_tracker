import type { Id } from "../../_generated/dataModel";
import type { MutationCtx } from "../../_generated/server";

export async function eraseHistory(ctx: MutationCtx, historyId: Id<"history">): Promise<void> {
  await ctx.db.delete("history", historyId);
}
