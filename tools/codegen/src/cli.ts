import path from "node:path";
import { parseArgs } from "node:util";
import { CodegenError } from "./buildModels.ts";
import { generateModelFiles } from "./generateModelFiles.ts";
import { readFunctionSpecFile, readFunctionSpecFromLocalBackend } from "./readFunctionSpec.ts";
import { writeModelFiles } from "./writeModelFiles.ts";

const repositoryRoot = path.resolve(import.meta.dirname, "../../..");
const modelDir = path.join(repositoryRoot, "shared/src/commonMain/kotlin/net/neednot/renttracker/model");

const { values } = parseArgs({ options: { spec: { type: "string" } } });

try {
  const spec =
    values.spec === undefined
      ? await readFunctionSpecFromLocalBackend(repositoryRoot)
      : readFunctionSpecFile(path.resolve(process.cwd(), values.spec));
  const files = generateModelFiles(spec);
  writeModelFiles(modelDir, files);
  console.log(`Generated ${files.size} Kotlin model file(s) in ${path.relative(repositoryRoot, modelDir)}`);
} catch (error) {
  if (!(error instanceof CodegenError)) {
    throw error;
  }
  console.error(`codegen: ${error.message}`);
  process.exit(1);
}
