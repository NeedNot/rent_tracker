import { v } from "convex/values";
import { internal } from "./_generated/api";
import { internalAction } from "./_generated/server";
import { type FcmServiceAccount, fcmAccessToken, fcmServiceAccount } from "./fcmAccessToken";

const TOKEN_GONE_ERRORS = new Set(["UNREGISTERED", "SENDER_ID_MISMATCH"]);

export const LATE_RENT_PUSH_TYPE = "lateRent";

interface LatePush {
  groupId: string;
  title: string;
  body: string;
}

type SendOutcome = "sent" | "tokenGone" | { failedWithStatus: number };

export const send = internalAction({
  args: { groupId: v.id("groups"), title: v.string(), body: v.string(), tokens: v.array(v.string()) },
  returns: v.null(),
  handler: async (ctx, args): Promise<null> => {
    const account = fcmServiceAccount();
    const accessToken = await fcmAccessToken(account, Date.now());
    const push: LatePush = { groupId: args.groupId, title: args.title, body: args.body };
    const outcomes = await Promise.all(args.tokens.map((token) => sendOne(account, accessToken, token, push)));
    const goneTokens = args.tokens.filter((_, index) => outcomes[index] === "tokenGone");
    if (goneTokens.length > 0) {
      await ctx.runMutation(internal.pushTokens.removeGone, { tokens: goneTokens });
    }
    const failures = outcomes.filter((outcome) => typeof outcome === "object");
    if (failures.length > 0) {
      const statuses = failures.map((failure) => failure.failedWithStatus).join(", ");
      throw new Error(`FCM refused ${failures.length} of ${args.tokens.length} late-rent pushes (HTTP ${statuses}).`);
    }
    return null;
  },
});

async function sendOne(
  account: FcmServiceAccount,
  accessToken: string,
  token: string,
  push: LatePush,
): Promise<SendOutcome> {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${account.projectId}/messages:send`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({
      message: {
        token,
        data: { type: LATE_RENT_PUSH_TYPE, ...push },
        android: { priority: "HIGH", collapse_key: push.groupId },
      },
    }),
  });
  if (response.ok) {
    return "sent";
  }
  return (await isTokenGone(response)) ? "tokenGone" : { failedWithStatus: response.status };
}

async function isTokenGone(response: Response): Promise<boolean> {
  const body = (await response.json().catch(() => null)) as {
    error?: { details?: { errorCode?: string }[] };
  } | null;
  return body?.error?.details?.some((detail) => TOKEN_GONE_ERRORS.has(detail.errorCode ?? "")) ?? false;
}
