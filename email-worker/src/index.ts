import { WorkerEntrypoint } from "cloudflare:workers";
import { signInCodeEmail } from "./signInCodeEmail";

const SENDER_NAME = "Rent Tracker";

export type Env = {
  readonly EMAIL: SendEmail;
  readonly EMAIL_FROM: string;
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
    return new Response("Not found", { status: 404 });
  }
}
