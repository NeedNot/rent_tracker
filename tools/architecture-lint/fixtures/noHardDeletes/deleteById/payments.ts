import type { Id } from "../../_generated/dataModel";
import type { MutationCtx } from "../../_generated/server";

export async function erasePayment(ctx: MutationCtx, paymentId: Id<"payments">): Promise<void> {
  await ctx.db.delete(paymentId);
}
