import type { Id } from "../../_generated/dataModel";
import type { MutationCtx } from "../../_generated/server";

export async function deleteAnything(ctx: MutationCtx, id: Id<"payments" | "pushTokens">): Promise<void> {
  await ctx.db.delete(id);
}
