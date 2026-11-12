import { query } from "../../_generated/server";

async function requireSignedIn(_ctx: unknown): Promise<void> {}

export const me = query({
  args: {},
  handler: async (ctx) => {
    await requireSignedIn(ctx);
    return null;
  },
});
