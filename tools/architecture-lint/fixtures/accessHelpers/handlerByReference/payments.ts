import { v } from "convex/values";
import { query } from "../../_generated/server";
import { requireMember } from "../../access";

async function listHandler(ctx: unknown, args: { groupId: string }) {
  await requireMember(ctx, args.groupId);
  return [];
}

export const list = query({
  args: { groupId: v.string() },
  handler: listHandler,
});
