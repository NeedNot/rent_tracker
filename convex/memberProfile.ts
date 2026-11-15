import { type Infer, v } from "convex/values";
import type { Doc } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { accountName } from "./validators";

export const memberProfile = accountName.extend({ email: v.optional(v.string()) });

type MemberProfile = Infer<typeof memberProfile>;

export async function loadMemberProfiles(ctx: QueryCtx, memberships: Doc<"memberships">[]): Promise<MemberProfile[]> {
  const accounts = await Promise.all(memberships.map((membership) => ctx.db.get(membership.accountId)));
  return accounts.filter(isFound).map(profileOf);
}

function profileOf(account: Doc<"accounts">): MemberProfile {
  const { _id: accountId, displayName, email } = account;
  return { accountId, displayName, ...(email !== undefined && { email }) };
}

function isFound(account: Doc<"accounts"> | null): account is Doc<"accounts"> {
  return account !== null;
}
