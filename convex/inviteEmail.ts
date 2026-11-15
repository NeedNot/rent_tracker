import { type Infer, v } from "convex/values";
import { internal } from "./_generated/api";
import { internalAction, internalQuery } from "./_generated/server";
import { isPendingInvite } from "./invitePending";
import { inviterNameOf } from "./inviteReceived";

const INVITES_PATH = "/invites";
const SECRET_HEADER = "X-Rent-Tracker-Secret";

const inviteEmailMessage = v.object({
  email: v.string(),
  inviterName: v.string(),
  groupName: v.string(),
  expiresAt: v.number(),
});

type InviteEmailMessage = Infer<typeof inviteEmailMessage>;

export const message = internalQuery({
  args: { inviteId: v.id("invites"), now: v.number() },
  returns: v.union(inviteEmailMessage, v.null()),
  handler: async (ctx, args) => {
    const invite = await ctx.db.get(args.inviteId);
    if (invite === null || !isPendingInvite(invite, args.now)) {
      return null;
    }
    const group = await ctx.db.get(invite.groupId);
    if (group === null || group.archivedAt !== undefined) {
      return null;
    }
    return {
      email: invite.email,
      inviterName: await inviterNameOf(ctx, invite.invitedBy),
      groupName: group.name,
      expiresAt: invite.expiresAt,
    };
  },
});

export const send = internalAction({
  args: { inviteId: v.id("invites") },
  returns: v.null(),
  handler: async (ctx, args): Promise<null> => {
    const pending: InviteEmailMessage | null = await ctx.runQuery(internal.inviteEmail.message, {
      inviteId: args.inviteId,
      now: Date.now(),
    });
    if (pending !== null) {
      await postToEmailWorker(pending);
    }
    return null;
  },
});

async function postToEmailWorker(body: InviteEmailMessage): Promise<void> {
  const workerUrl = requiredEnv("EMAIL_WORKER_URL").replace(/\/+$/, "");
  const response = await fetch(`${workerUrl}${INVITES_PATH}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", [SECRET_HEADER]: requiredEnv("WORKER_SHARED_SECRET") },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`The email worker refused the invite email with HTTP ${response.status}.`);
  }
}

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (value === undefined || value === "") {
    throw new Error(`The Convex environment variable ${name} is not set.`);
  }
  return value;
}
