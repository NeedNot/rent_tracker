import { WorkerEntrypoint } from "cloudflare:workers";
import { inviteEmail } from "./inviteEmail";
import { inviteMessageFrom } from "./inviteRequest";
import { matchesSharedSecret } from "./sharedSecret";
import { signInCodeEmail } from "./signInCodeEmail";

const SENDER_NAME = "Rent Tracker";
const SECRET_HEADER = "X-Rent-Tracker-Secret";

export type Env = {
  readonly EMAIL: SendEmail;
  readonly EMAIL_FROM: string;
  readonly SHARED_SECRET?: string;
};

export type SignInCodeMessage = {
  readonly email: string;
  readonly code: string;
};

export default class EmailWorker extends WorkerEntrypoint<Env> {
  async sendSignInCode(message: SignInCodeMessage): Promise<void> {
    await this.env.EMAIL.send({
      to: message.email,
      from: { email: this.env.EMAIL_FROM, name: SENDER_NAME },
      ...signInCodeEmail(message.code),
    });
  }

  override async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return new Response("ok");
    }
    if (request.method === "POST" && url.pathname === "/invites") {
      return await this.sendInvite(request);
    }
    return new Response("Not found", { status: 404 });
  }

  private async sendInvite(request: Request): Promise<Response> {
    if (!matchesSharedSecret(request.headers.get(SECRET_HEADER), this.env.SHARED_SECRET)) {
      return new Response("Unauthorized", { status: 401 });
    }
    const invite = await inviteMessageFrom(request);
    if (invite === null) {
      return new Response("Bad request", { status: 400 });
    }
    await this.env.EMAIL.send({
      to: invite.email,
      from: { email: this.env.EMAIL_FROM, name: SENDER_NAME },
      ...inviteEmail(invite),
    });
    return new Response(null, { status: 204 });
  }
}
