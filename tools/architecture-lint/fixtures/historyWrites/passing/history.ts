import type { MutationCtx } from "../../_generated/server";

export async function writeHistory(ctx: MutationCtx, action: string): Promise<void> {
  await ctx.db.insert("history", { action });
}
