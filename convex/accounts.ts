import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { accountForSubject, requireAccount, requireActive, requireIdentity } from "./access";
import { requireOwnsNoGroups } from "./accountOwnership";
import { profileOf } from "./accountProfile";
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

    await requireOwnsNoGroups(ctx, account._id);

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

function defaultDisplayName(email: string): string {
  const localPart = email.split("@")[0]?.trim() ?? "";
  return (localPart === "" ? email : localPart).slice(0, NAME_MAX_LENGTH);
}
