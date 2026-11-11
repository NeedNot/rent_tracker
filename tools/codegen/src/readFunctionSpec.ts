import { type ChildProcess, execFile, spawn } from "node:child_process";
import fs from "node:fs";
import { promisify } from "node:util";
import { type FunctionSpec, parseFunctionSpec } from "./functionSpec.ts";

const READY_MESSAGE = "Convex functions ready!";
const READY_TIMEOUT_MS = 120_000;
const FUNCTION_SPEC_MAX_BYTES = 64 * 1024 * 1024;

const execFileAsync = promisify(execFile);

export function readFunctionSpecFile(specPath: string): FunctionSpec {
  return parseFunctionSpec(fs.readFileSync(specPath, "utf8"));
}

export async function readFunctionSpecFromLocalBackend(repositoryRoot: string): Promise<FunctionSpec> {
  const env = { ...process.env, CONVEX_AGENT_MODE: "anonymous" };
  const backend = spawn("npx", ["convex", "dev"], {
    cwd: repositoryRoot,
    env,
    detached: true,
    stdio: ["ignore", "pipe", "pipe"],
  });
  try {
    await waitForOutput(backend, READY_MESSAGE, READY_TIMEOUT_MS);
    const { stdout } = await execFileAsync("npx", ["convex", "function-spec"], {
      cwd: repositoryRoot,
      env,
      maxBuffer: FUNCTION_SPEC_MAX_BYTES,
    });
    return parseFunctionSpec(stdout);
  } finally {
    await stopProcessGroup(backend);
  }
}

function waitForOutput(child: ChildProcess, message: string, timeoutMs: number): Promise<void> {
  return new Promise((resolve, reject) => {
    let output = "";
    const fail = (reason: string) => {
      cleanUp();
      reject(new Error(`${reason}\n\nconvex dev output:\n${output}`));
    };
    const onData = (chunk: Buffer) => {
      output += chunk.toString();
      if (output.includes(message)) {
        cleanUp();
        resolve();
      }
    };
    const onExit = (code: number | null) => fail(`convex dev exited (code ${code}) before its functions were ready`);
    const timer = setTimeout(() => fail(`convex dev was not ready after ${timeoutMs / 1000}s`), timeoutMs);
    const cleanUp = () => {
      clearTimeout(timer);
      child.stdout?.off("data", onData);
      child.stderr?.off("data", onData);
      child.off("exit", onExit);
    };
    child.stdout?.on("data", onData);
    child.stderr?.on("data", onData);
    child.once("exit", onExit);
  });
}

function stopProcessGroup(child: ChildProcess): Promise<void> {
  if (child.pid === undefined || child.exitCode !== null || child.signalCode !== null) {
    return Promise.resolve();
  }
  const exited = new Promise<void>((resolve) => child.once("exit", () => resolve()));
  // Negative pid signals the whole group, so the local backend that convex dev started stops too.
  process.kill(-child.pid, "SIGTERM");
  return exited;
}
