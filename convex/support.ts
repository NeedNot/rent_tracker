import { v } from "convex/values";
import { internal } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import { type ActionCtx, internalAction, internalMutation, internalQuery } from "./_generated/server";
import { groupsOwnedBy } from "./accountOwnership";
import { profileOf } from "./accountProfile";
import { changeAuthUserEmail, deleteAuthUser, revokeAuthSessions } from "./authWorker";
import { requireInviteEmail } from "./inviteEmailAddress";
import { restorableKind, restoreCancelledEntry } from "./supportRestore";
import { accountProfileFields } from "./validators";

type RequestedDeletion = Doc<"accounts"> & { deletionRequestedAt: number };

interface EmailChange {
  accountId: Id<"accounts">;
  newEmail: string;
}

export const deletionRequests = internalQuery({
  args: {},
  returns: v.array(
    v.object({
      ...accountProfileFields,
      deletionRequestedAt: v.number(),
      ownedGroups: v.array(v.object({ groupId: v.id("groups"), name: v.string() })),
    }),
  ),
  handler: async (ctx) => {
    const requesters = await ctx.db
      .query("accounts")
      .withIndex("by_deleted_deletion_requested", (q) =>
        q.eq("deletedAt", undefined).gt("deletionRequestedAt", undefined),
      )
      .collect();
    return await Promise.all(
      requesters.filter(hasDeletionRequest).map(async (account) => ({
        ...profileOf(account),
        deletionRequestedAt: account.deletionRequestedAt,
        ownedGroups: await groupsOwnedBy(ctx, account._id),
      })),
    );
  },
});

export const anonymizeAccount = internalAction({
  args: { accountId: v.id("accounts") },
  returns: v.null(),
  handler: async (ctx, args): Promise<null> => {
    const authSubject: string | null = await ctx.runQuery(internal.supportAccounts.authSubjectToAnonymize, args);
    if (authSubject === null) {
      return null;
    }
    await deleteAuthUser(authSubject);
    await ctx.runMutation(internal.supportAccounts.anonymize, args);
    return null;
  },
});

export const changeAccountEmail = internalAction({
  args: { accountId: v.id("accounts"), newEmail: v.string() },
  returns: v.string(),
  handler: async (ctx, args): Promise<string> => {
    return (await changeEmail(ctx, args)).email;
  },
});

export const recoverAccount = internalAction({
  args: { accountId: v.id("accounts"), newEmail: v.string() },
  returns: v.string(),
  handler: async (ctx, args): Promise<string> => {
    const { email, authSubject } = await changeEmail(ctx, args);
    await revokeAuthSessions(authSubject);
    return email;
  },
});

export const restoreEntry = internalMutation({
  args: { kind: restorableKind, id: v.string() },
  returns: v.null(),
  handler: async (ctx, args) => {
    await restoreCancelledEntry(ctx, args.kind, args.id);
    return null;
  },
});

async function changeEmail(ctx: ActionCtx, change: EmailChange): Promise<{ email: string; authSubject: string }> {
  const email = requireInviteEmail(change.newEmail);
  const target = { accountId: change.accountId, email };
  const authSubject: string = await ctx.runQuery(internal.supportAccounts.authSubjectForEmailChange, target);
  await changeAuthUserEmail(authSubject, email);
  await ctx.runMutation(internal.supportAccounts.setEmail, target);
  return { email, authSubject };
}

function hasDeletionRequest(account: Doc<"accounts">): account is RequestedDeletion {
  return account.deletionRequestedAt !== undefined;
}
