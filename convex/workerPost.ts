const SECRET_HEADER = "X-Rent-Tracker-Secret";

export async function postToWorker(urlVariable: string, path: string, body: object): Promise<Response> {
  const workerUrl = requiredEnv(urlVariable).replace(/\/+$/, "");
  return await fetch(`${workerUrl}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", [SECRET_HEADER]: requiredEnv("WORKER_SHARED_SECRET") },
    body: JSON.stringify(body),
  });
}

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (value === undefined || value === "") {
    throw new Error(`The Convex environment variable ${name} is not set.`);
  }
  return value;
}
