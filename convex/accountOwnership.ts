import type { Doc, Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { validationFailed } from "./appError";

export interface OwnedGroup {
  groupId: Id<"groups">;
  name: string;
}

export async function groupsOwnedBy(ctx: QueryCtx, accountId: Id<"accounts">): Promise<OwnedGroup[]> {
  const memberships = await ctx.db
    .query("memberships")
    .withIndex("by_account", (q) => q.eq("accountId", accountId))
    .collect();
  const owned = memberships.filter((membership) => membership.role === "owner");
  const groups = await Promise.all(owned.map((membership) => ctx.db.get(membership.groupId)));
  return groups
    .filter((group): group is Doc<"groups"> => group !== null)
    .map((group) => ({ groupId: group._id, name: group.name }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

export async function requireOwnsNoGroups(ctx: QueryCtx, accountId: Id<"accounts">): Promise<void> {
  const ownedGroups = await groupsOwnedBy(ctx, accountId);
  if (ownedGroups.length > 0) {
    throw validationFailed(`Transfer ownership of ${joinNames(ownedGroups.map((g) => g.name))} first.`, {
      groups: ownedGroups.map(({ groupId, name }) => ({ groupId, name })),
    });
  }
}

function joinNames(names: string[]): string {
  if (names.length <= 1) {
    return names.join("");
  }
  return `${names.slice(0, -1).join(", ")} and ${names[names.length - 1]}`;
}
