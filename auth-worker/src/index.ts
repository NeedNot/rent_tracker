import { betterAuth } from "better-auth";
import { AUTH_BASE_PATH, authOptions } from "./authOptions";
import type { Env } from "./env";

export default {
  async fetch(request, env): Promise<Response> {
    const url = new URL(request.url);
    if (url.pathname.startsWith(`${AUTH_BASE_PATH}/`)) {
      return betterAuth(authOptions(env)).handler(request);
    }
    if (request.method === "GET" && url.pathname === "/health") {
      return new Response("ok");
    }
    return new Response("Not found", { status: 404 });
  },
} satisfies ExportedHandler<Env>;
