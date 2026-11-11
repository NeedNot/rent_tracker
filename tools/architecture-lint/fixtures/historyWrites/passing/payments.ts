import type { Id } from "../../_generated/dataModel";
import type { MutationCtx, MutationCtxWithTable } from "../../_generated/server";
import { writeHistory } from "./history";

export async function editPayment(ctx: MutationCtx, paymentId: Id<"payments">): Promise<void> {
  await ctx.db.patch(paymentId, { amountCents: 100 });
  await ctx.db.patch("payments", paymentId, { amountCents: 150 });
  await writeHistory(ctx, "edit");
}

export async function editPaymentThroughTable(ctx: MutationCtxWithTable, paymentId: Id<"payments">): Promise<void> {
  await ctx.db.table("payments").patch(paymentId, { amountCents: 200 });
}
