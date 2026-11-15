import type { Doc } from "./_generated/dataModel";

export function isPendingInvite(invite: Doc<"invites">, now: number): boolean {
  return (
    invite.acceptedAt === undefined &&
    invite.revokedAt === undefined &&
    invite.declinedAt === undefined &&
    invite.expiresAt > now
  );
}
