export interface FunctionSpec {
  functions: ConvexFunction[];
}

export interface ConvexFunction {
  identifier: string;
  functionType: string;
  visibility?: { kind: string };
  args?: Validator;
  returns?: Validator;
}

export type Validator =
  | { type: "object"; value: Record<string, Field> }
  | { type: "array"; value: Validator }
  | { type: "record"; keys: Validator; values: Field }
  | { type: "union"; value: Validator[] }
  | { type: "literal"; value: string | number | boolean }
  | { type: "id"; tableName: string }
  | { type: "string" | "number" | "bigint" | "boolean" | "null" | "any" | "bytes" };

export interface Field {
  fieldType: Validator;
  optional: boolean;
}

const CALLABLE_FUNCTION_TYPES = ["Query", "Mutation", "Action"];

export function parseFunctionSpec(json: string): FunctionSpec {
  const spec = JSON.parse(json) as FunctionSpec;
  if (!Array.isArray(spec.functions)) {
    throw new Error("function-spec output has no functions array");
  }
  return spec;
}

export function publicFunctions(spec: FunctionSpec): ConvexFunction[] {
  return spec.functions
    .filter((fn) => fn.visibility?.kind === "public" && CALLABLE_FUNCTION_TYPES.includes(fn.functionType))
    .sort((a, b) => a.identifier.localeCompare(b.identifier));
}
