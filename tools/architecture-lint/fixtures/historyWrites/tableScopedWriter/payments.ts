import type { Id } from "../../_generated/dataModel";
import type { MutationCtxWithTable } from "../../_generated/server";

export async function replaceHistory(ctx: MutationCtxWithTable, historyId: Id<"history">): Promise<void> {
  await ctx.db.table("history").replace(historyId, { action: "replaced" });
}
