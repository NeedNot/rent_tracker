import { v } from "convex/values";
import type { Doc, Id } from "./_generated/dataModel";
import { mutation, type QueryCtx, query } from "./_generated/server";
import { accountForSubject, requireAccount, requireActive, requireIdentity } from "./access";
import { appError } from "./appError";
import { accountProfile, accountProfileFields, NAME_MAX_LENGTH } from "./validators";

const INITIAL_VERSION = 1;

export const ensure = mutation({
  args: {},
  returns: v.object({ ...accountProfileFields, created: v.boolean() }),
  handler: async (ctx) => {
    const identity = await requireIdentity(ctx);
    const existing = await accountForSubject(ctx, identity.subject);
    if (existing !== null) {
      return { ...profileOf(requireActive(existing)), created: false };
    }

    const email = identity.email?.trim().toLowerCase();
    if (email === undefined || email === "") {
      throw appError("NotSignedIn", "The sign-in token has no email address.");
    }
    const emailOwner = await ctx.db
      .query("accounts")
      .withIndex("by_email", (q) => q.eq("email", email))
      .first();
    if (emailOwner !== null) {
      throw appError("ValidationFailed", "This email already belongs to another account.", { field: "email" });
    }

    const displayName = defaultDisplayName(email);
    const accountId = await ctx.db.insert("accounts", {
      authSubject: identity.subject,
      displayName,
      email,
      version: INITIAL_VERSION,
    });
    return { accountId, displayName, email, created: true };
  },
});

export const setDisplayName = mutation({
  args: { displayName: v.string() },
  returns: v.string(),
  handler: async (ctx, args) => {
    const account = await requireAccount(ctx);
    const displayName = args.displayName.trim();
    if (displayName === "" || displayName.length > NAME_MAX_LENGTH) {
      throw appError("ValidationFailed", `Enter a name of 1 to ${NAME_MAX_LENGTH} characters.`, {
        field: "displayName",
      });
    }
    await ctx.db.patch(account._id, {
      displayName,
      version: account.version + 1,
      lastEditedAt: Date.now(),
      lastEditedBy: account._id,
    });
    return displayName;
  },
});

export const me = query({
  args: {},
  returns: accountProfile.extend({ deletionRequestedAt: v.optional(v.number()) }),
  handler: async (ctx) => {
    const account = await requireAccount(ctx);
    return { ...profileOf(account), deletionRequestedAt: account.deletionRequestedAt };
  },
});

export const requestDeletion = mutation({
  args: {},
  returns: v.number(),
  handler: async (ctx) => {
    const account = await requireAccount(ctx);
    if (account.deletionRequestedAt !== undefined) {
      return account.deletionRequestedAt;
    }

    const ownedGroups = await groupsOwnedBy(ctx, account._id);
    if (ownedGroups.length > 0) {
      throw appError("ValidationFailed", `Transfer ownership of ${joinNames(ownedGroups.map((g) => g.name))} first.`, {
        groups: ownedGroups.map(({ groupId, name }) => ({ groupId, name })),
      });
    }

    const deletionRequestedAt = Date.now();
    await ctx.db.patch(account._id, {
      deletionRequestedAt,
      version: account.version + 1,
      lastEditedAt: deletionRequestedAt,
      lastEditedBy: account._id,
    });
    return deletionRequestedAt;
  },
});

async function groupsOwnedBy(ctx: QueryCtx, accountId: Id<"accounts">) {
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

function joinNames(names: string[]): string {
  if (names.length <= 1) {
    return names.join("");
  }
  return `${names.slice(0, -1).join(", ")} and ${names[names.length - 1]}`;
}

function profileOf(account: Doc<"accounts">) {
  if (account.email === undefined) {
    throw new Error(`Active account ${account._id} has no email`);
  }
  return { accountId: account._id, displayName: account.displayName, email: account.email };
}

function defaultDisplayName(email: string): string {
  const localPart = email.split("@")[0]?.trim() ?? "";
  return (localPart === "" ? email : localPart).slice(0, NAME_MAX_LENGTH);
}
