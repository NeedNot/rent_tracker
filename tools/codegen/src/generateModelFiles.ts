import { buildModels } from "./buildModels.ts";
import type { FunctionSpec } from "./functionSpec.ts";
import { renderModelFiles } from "./renderKotlin.ts";

export function generateModelFiles(spec: FunctionSpec): Map<string, string> {
  return renderModelFiles(buildModels(spec));
}
