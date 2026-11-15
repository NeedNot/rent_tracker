import type { EmailContent } from "./signInCodeEmail";

const PRODUCT_NAME = "Rent Tracker";
const INVITE_LIFETIME = "14 days";

export type InviteMessage = {
  readonly email: string;
  readonly inviterName: string;
  readonly groupName: string;
  readonly expiresAt: number;
};

export function inviteEmail(invite: InviteMessage): EmailContent {
  const expiresOn = new Date(invite.expiresAt).toLocaleDateString("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
    timeZone: "UTC",
  });
  const howToJoin = `To join, install ${PRODUCT_NAME} and sign in with this email address. The invite will be waiting on your Home screen.`;
  const expiry = `This invite expires in ${INVITE_LIFETIME}, on ${expiresOn}.`;
  const ignore = "If you were not expecting this invite, you can ignore this email.";
  const invitation = (inviter: string, group: string) =>
    `${inviter} invited you to the group ${group} on ${PRODUCT_NAME}.`;
  return {
    subject: singleLine(`${invite.inviterName} invited you to ${invite.groupName} on ${PRODUCT_NAME}`),
    text: [invitation(invite.inviterName, `"${invite.groupName}"`), howToJoin, expiry, ignore].join("\n\n"),
    html: [
      `<p>${invitation(escapeHtml(invite.inviterName), `<strong>${escapeHtml(invite.groupName)}</strong>`)}</p>`,
      `<p>${howToJoin}</p>`,
      `<p>${expiry}</p>`,
      `<p>${ignore}</p>`,
    ].join("\n"),
  };
}

function singleLine(text: string): string {
  return text.replace(/\s+/g, " ").trim();
}

function escapeHtml(text: string): string {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
