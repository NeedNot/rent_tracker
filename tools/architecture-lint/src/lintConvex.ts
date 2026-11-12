import fs from "node:fs";
import path from "node:path";
import ts from "typescript";
import { checkAccessHelpers } from "./rules/accessHelpers.ts";
import { checkNoHardDeletes } from "./rules/noHardDeletes.ts";
import type { RuleContext, Violation } from "./violation.ts";

const DEFAULT_COMPILER_OPTIONS: ts.CompilerOptions = {
  target: ts.ScriptTarget.ES2022,
  module: ts.ModuleKind.ESNext,
  moduleResolution: ts.ModuleResolutionKind.Bundler,
  strict: true,
  skipLibCheck: true,
  noEmit: true,
};

export function lintConvexDirectory(convexDir: string): Violation[] {
  const files = deployedSourceFiles(convexDir);
  const program = ts.createProgram(files, compilerOptionsFor(convexDir));
  const checker = program.getTypeChecker();

  return files.flatMap((file) => {
    const sourceFile = program.getSourceFile(file);
    if (sourceFile === undefined) {
      return [];
    }
    const context: RuleContext = { sourceFile, checker, relativePath: path.relative(convexDir, file) };
    return [...checkAccessHelpers(context), ...checkNoHardDeletes(context)];
  });
}

// Convex deploys only files whose name has a single dot (so *.test.ts and *.setup.ts are skipped); lint the same set.
function deployedSourceFiles(convexDir: string): string[] {
  return fs
    .readdirSync(convexDir, { recursive: true, encoding: "utf8" })
    .filter((relativePath) => !relativePath.split(path.sep).includes("_generated"))
    .filter((relativePath) => /^[^.]+\.ts$/.test(path.basename(relativePath)))
    .map((relativePath) => path.join(convexDir, relativePath))
    .sort();
}

function compilerOptionsFor(convexDir: string): ts.CompilerOptions {
  const configPath = path.join(convexDir, "tsconfig.json");
  if (!fs.existsSync(configPath)) {
    return DEFAULT_COMPILER_OPTIONS;
  }
  const config = ts.readConfigFile(configPath, ts.sys.readFile);
  return ts.parseJsonConfigFileContent(config.config, ts.sys, convexDir).options;
}
