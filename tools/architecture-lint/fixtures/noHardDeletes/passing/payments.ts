import type { Id } from "../../_generated/dataModel";
import type { MutationCtx, MutationCtxWithTable } from "../../_generated/server";

export async function cancelPayment(ctx: MutationCtx, paymentId: Id<"payments">): Promise<void> {
  await ctx.db.patch(paymentId, { deletedAt: 1 });
  await ctx.db.patch("payments", paymentId, { deletedAt: 2 });
}

export async function cancelPaymentThroughTable(ctx: MutationCtxWithTable, paymentId: Id<"payments">): Promise<void> {
  await ctx.db.table("payments").patch(paymentId, { deletedAt: 3 });
}

export async function forgetPushToken(ctx: MutationCtx, tokenId: Id<"pushTokens">): Promise<void> {
  await ctx.db.delete(tokenId);
  await ctx.db.delete("pushTokens", tokenId);
}
