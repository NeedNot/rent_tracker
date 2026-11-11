import path from "node:path";
import { lintConvexDirectory } from "./lintConvex.ts";
import { formatViolation } from "./violation.ts";

const repositoryRoot = path.resolve(import.meta.dirname, "../../..");
const convexDir = path.resolve(process.argv[2] ?? path.join(repositoryRoot, "convex"));

const violations = lintConvexDirectory(convexDir);

for (const violation of violations) {
  const file = path.relative(repositoryRoot, path.join(convexDir, violation.file));
  console.error(formatViolation({ ...violation, file }));
}
if (violations.length > 0) {
  console.error(`\n${violations.length} architecture violation(s) in ${convexDir}`);
  process.exit(1);
}
console.log(`Architecture rules pass for ${convexDir}`);
