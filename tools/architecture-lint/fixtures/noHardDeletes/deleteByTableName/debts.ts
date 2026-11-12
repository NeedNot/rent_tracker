import type { Id } from "../../_generated/dataModel";
import type { MutationCtx } from "../../_generated/server";

export async function eraseDebt(ctx: MutationCtx, debtId: Id<"debts">): Promise<void> {
  await ctx.db.delete("debts", debtId);
}
