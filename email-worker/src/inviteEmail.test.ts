import { describe, expect, it } from "vitest";
import { inviteEmail } from "./inviteEmail";

const INVITE = {
  email: "dalton.h@gmail.com",
  inviterName: "Dana",
  groupName: "1st Street",
  expiresAt: Date.UTC(2026, 10, 28, 15),
};

describe("inviteEmail", () => {
  it("names the inviter and the group in the subject", () => {
    expect(inviteEmail(INVITE).subject).toBe("Dana invited you to 1st Street on Rent Tracker");
  });

  it.each(["text", "html"] as const)(
    "says in the %s who invited them to which group, how to join and when it expires",
    (part) => {
      expect(inviteEmail(INVITE)[part]).toMatch(
        /Dana invited you to the group[\s\S]*1st Street[\s\S]*install Rent Tracker and sign in with this email address[\s\S]*expires in 14 days, on November 28, 2026/,
      );
    },
  );

  it("contains no link", () => {
    const { text, html } = inviteEmail(INVITE);

    expect(text).not.toMatch(/https?:/);
    expect(html).not.toMatch(/href|https?:/);
  });

  it("escapes HTML in the inviter and group names", () => {
    const html = inviteEmail({ ...INVITE, inviterName: "<b>Dana</b>", groupName: `Tom & Jerry's "Lot"` }).html;

    expect(html).toContain("&lt;b&gt;Dana&lt;/b&gt;");
    expect(html).toContain("Tom &amp; Jerry&#39;s &quot;Lot&quot;");
    expect(html).not.toContain("<b>Dana</b>");
  });

  it("keeps the subject on one line", () => {
    expect(inviteEmail({ ...INVITE, groupName: "1st\r\nStreet" }).subject).toBe(
      "Dana invited you to 1st Street on Rent Tracker",
    );
  });
});
