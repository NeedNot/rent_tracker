import type { MutationCtx } from "../../_generated/server";

export async function logPayment(ctx: MutationCtx): Promise<void> {
  await ctx.db.insert("payments", { amountCents: 100 });
  await ctx.db.insert("history", { action: "create" });
}
