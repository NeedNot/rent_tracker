import { mutation, query } from "../../_generated/server";
import { requireAccount, requireIdentity } from "../../access";

export const ensure = mutation({
  args: {},
  handler: async (ctx) => {
    await requireIdentity(ctx);
  },
});

export const me = query({
  args: {},
  handler: async (ctx) => {
    await requireAccount(ctx);
    return null;
  },
});
