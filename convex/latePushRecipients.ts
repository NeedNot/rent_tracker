import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";

export function receivesLatePushes(membership: Doc<"memberships">): boolean {
  return membership.role === "owner" || membership.payments !== "read";
}

export async function latePushTokens(ctx: QueryCtx, groupId: Id<"groups">): Promise<string[]> {
  const memberships = await ctx.db
    .query("memberships")
    .withIndex("by_group_account", (q) => q.eq("groupId", groupId))
    .collect();
  const recipients = memberships.filter((membership) => membership.notificationsOn && receivesLatePushes(membership));
  const tokens = await Promise.all(
    recipients.map((membership) =>
      ctx.db
        .query("pushTokens")
        .withIndex("by_account", (q) => q.eq("accountId", membership.accountId))
        .collect(),
    ),
  );
  return tokens
    .flat()
    .filter((row) => row.platform === "android")
    .map((row) => row.token);
}
