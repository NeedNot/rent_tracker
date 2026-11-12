import type { Id } from "../../_generated/dataModel";
import type { MutationCtxWithTable } from "../../_generated/server";

export async function eraseRentTerm(ctx: MutationCtxWithTable, rentTermId: Id<"rentTerms">): Promise<void> {
  await ctx.db.table("rentTerms").delete(rentTermId);
}
