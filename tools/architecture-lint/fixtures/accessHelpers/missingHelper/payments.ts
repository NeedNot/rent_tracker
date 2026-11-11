import { v } from "convex/values";
import { query } from "../../_generated/server";

export const list = query({
  args: { groupId: v.string() },
  handler: async () => [],
});
