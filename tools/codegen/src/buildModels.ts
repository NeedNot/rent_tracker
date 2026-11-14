import { type ConvexFunction, type Field, type FunctionSpec, publicFunctions, type Validator } from "./functionSpec.ts";
import { packageSegment, pascalCase, propertyName, singular } from "./naming.ts";

export const SHARED_PACKAGE = "shared";

export type KotlinType =
  | { kind: "builtin"; name: "String" | "Long" | "Boolean" | "JsonElement" }
  | { kind: "list"; item: KotlinType }
  | { kind: "map"; value: KotlinType }
  | { kind: "nullable"; inner: KotlinType }
  | { kind: "declared"; name: string };

export interface Property {
  name: string;
  serialName: string;
  type: KotlinType;
  hasDefaultNull: boolean;
}

export type Declaration =
  | { kind: "class"; name: string; properties: Property[] }
  | { kind: "enum"; name: string; values: string[] };

export interface Model {
  packageName: string;
  declaration: Declaration;
}

export class CodegenError extends Error {}

interface Naming {
  typeName: string;
  childPrefix: string;
}

interface Registered {
  declaration: Declaration;
  signature: string;
  location: string;
  files: Set<string>;
}

export function buildModels(spec: FunctionSpec): Model[] {
  const registry = new Map<string, Registered>();
  const entityNames = collectEntityNames(spec);
  for (const fn of publicFunctions(spec)) {
    new FunctionModels(fn, registry, entityNames).build();
  }
  return [...registry.values()]
    .map(({ declaration, files }) => ({
      packageName: files.size > 1 ? SHARED_PACKAGE : packageSegment([...files][0] ?? SHARED_PACKAGE),
      declaration,
    }))
    .sort((a, b) => a.packageName.localeCompare(b.packageName) || a.declaration.name.localeCompare(b.declaration.name));
}

class FunctionModels {
  private readonly fn: ConvexFunction;
  private readonly registry: Map<string, Registered>;
  private readonly entityNames: Map<string, string>;
  private readonly file: string;
  private readonly baseName: string;

  constructor(fn: ConvexFunction, registry: Map<string, Registered>, entityNames: Map<string, string>) {
    this.fn = fn;
    this.registry = registry;
    this.entityNames = entityNames;
    const [file = "", verb = ""] = fn.identifier.replace(/\.js:/, ":").split(":");
    this.file = file;
    this.baseName = pascalCase(file) + pascalCase(verb);
  }

  build(): void {
    this.buildArgs();
    this.buildReturns();
  }

  private buildArgs(): void {
    const args = this.fn.args;
    if (args === undefined || args.type !== "object") {
      throw this.error("args", "has no object args validator");
    }
    if (Object.keys(args.value).length > 0) {
      this.declareClass(args, { typeName: `${this.baseName}Args`, childPrefix: this.baseName }, "args");
    }
  }

  private buildReturns(): void {
    const returns = this.fn.returns;
    if (returns === undefined) {
      throw this.error("returns", "has no returns validator");
    }
    if (returns.type === "null") {
      return;
    }
    if (returns.type === "object") {
      this.declareClass(returns, { typeName: `${this.baseName}Result`, childPrefix: this.baseName }, "returns");
      return;
    }
    const itemName = `${this.baseName}Item`;
    this.toKotlinType(returns, { typeName: itemName, childPrefix: itemName }, "returns");
  }

  private toKotlinType(validator: Validator, naming: Naming, location: string): KotlinType {
    switch (validator.type) {
      case "string":
      case "id":
        return { kind: "builtin", name: "String" };
      case "number":
        return { kind: "builtin", name: "Long" };
      case "boolean":
        return { kind: "builtin", name: "Boolean" };
      case "any":
        return { kind: "builtin", name: "JsonElement" };
      case "object":
        return this.declareClass(validator, naming, location);
      case "array":
        return { kind: "list", item: this.toKotlinType(validator.value, naming, location) };
      case "record":
        return this.toMapType(validator, naming, location);
      case "literal":
        return this.declareEnum([validator], naming, location);
      case "union":
        return this.toUnionType(validator.value, naming, location);
      default:
        throw this.error(location, `uses ${validator.type}, which codegen does not support`);
    }
  }

  private toMapType(validator: Validator & { type: "record" }, naming: Naming, location: string): KotlinType {
    if (validator.keys.type !== "string" && validator.keys.type !== "id") {
      throw this.error(location, "is a record whose keys are not strings");
    }
    return { kind: "map", value: this.fieldType(validator.values, naming, location) };
  }

  private toUnionType(members: Validator[], naming: Naming, location: string): KotlinType {
    const nonNull = members.filter((member) => member.type !== "null");
    const isNullable = nonNull.length < members.length;
    let type: KotlinType;
    if (nonNull.length > 0 && nonNull.every((member) => member.type === "literal")) {
      type = this.declareEnum(nonNull, naming, location);
    } else if (nonNull.length === 1 && nonNull[0] !== undefined) {
      type = this.toKotlinType(nonNull[0], naming, location);
    } else {
      throw this.error(location, "is a union of different types, which codegen does not support");
    }
    return isNullable ? { kind: "nullable", inner: type } : type;
  }

  private declareClass(validator: Validator & { type: "object" }, naming: Naming, location: string): KotlinType {
    const entries = Object.entries(validator.value);
    if (entries.length === 0) {
      throw this.error(location, "is an empty object, which codegen does not support");
    }
    const entityName = this.entityNames.get(JSON.stringify(validator));
    const classNaming = entityName === undefined ? naming : { typeName: entityName, childPrefix: entityName };
    const properties = entries.map(([fieldName, field]) =>
      this.property(fieldName, field, classNaming.childPrefix, `${location}.${fieldName}`),
    );
    return this.register({ kind: "class", name: classNaming.typeName, properties }, location);
  }

  private property(fieldName: string, field: Field, parentPrefix: string, location: string): Property {
    const childNaming = namingForField(fieldName, field.fieldType, parentPrefix);
    const type = this.fieldType(field, childNaming, location);
    return { name: propertyName(fieldName), serialName: fieldName, type, hasDefaultNull: field.optional };
  }

  private fieldType(field: Field, naming: Naming, location: string): KotlinType {
    const type = this.toKotlinType(field.fieldType, naming, location);
    return field.optional && type.kind !== "nullable" ? { kind: "nullable", inner: type } : type;
  }

  private declareEnum(literals: Validator[], naming: Naming, location: string): KotlinType {
    const values = literals.map((literal) => {
      if (literal.type !== "literal" || typeof literal.value !== "string") {
        throw this.error(location, "uses a non-string literal, which codegen does not support");
      }
      return literal.value;
    });
    const constants = new Set(values.map(pascalCase));
    if (constants.size !== values.length) {
      throw this.error(location, `has literals that map to the same Kotlin enum constant: ${values.join(", ")}`);
    }
    return this.register({ kind: "enum", name: naming.typeName, values }, location);
  }

  private register(declaration: Declaration, location: string): KotlinType {
    const signature = JSON.stringify(declaration);
    const where = `${this.fn.identifier} ${location}`;
    const existing = this.registry.get(declaration.name);
    if (existing === undefined) {
      this.registry.set(declaration.name, { declaration, signature, location: where, files: new Set([this.file]) });
    } else if (existing.signature !== signature) {
      throw new CodegenError(
        `Kotlin type ${declaration.name} has two different shapes: ${existing.location} and ${where}. ` +
          "Make the shapes identical or rename one of the fields.",
      );
    } else {
      existing.files.add(this.file);
    }
    return { kind: "declared", name: declaration.name };
  }

  private error(location: string, problem: string): CodegenError {
    return new CodegenError(`${this.fn.identifier} ${location} ${problem}`);
  }
}

function collectEntityNames(spec: FunctionSpec): Map<string, string> {
  const entityNames = new Map<string, string>();
  const visit = (validator: Validator | undefined): void => {
    if (validator === undefined) {
      return;
    }
    if (validator.type === "object") {
      for (const [fieldName, field] of Object.entries(validator.value)) {
        for (const item of arrayItemObjects(field.fieldType)) {
          if (!entityNames.has(JSON.stringify(item))) {
            entityNames.set(JSON.stringify(item), pascalCase(singular(fieldName)));
          }
        }
        visit(field.fieldType);
      }
    } else if (validator.type === "array" || validator.type === "record") {
      visit(validator.type === "array" ? validator.value : validator.values.fieldType);
    } else if (validator.type === "union") {
      validator.value.forEach(visit);
    }
  };
  for (const fn of publicFunctions(spec)) {
    visit(fn.args);
    visit(fn.returns);
  }
  return entityNames;
}

function arrayItemObjects(validator: Validator): Validator[] {
  if (validator.type === "array") {
    return validator.value.type === "object" ? [validator.value] : arrayItemObjects(validator.value);
  }
  if (validator.type === "union") {
    return validator.value.flatMap(arrayItemObjects);
  }
  return [];
}

function namingForField(fieldName: string, validator: Validator, parentPrefix: string): Naming {
  if (isArrayOfObjects(validator)) {
    const entityName = pascalCase(singular(fieldName));
    return { typeName: entityName, childPrefix: entityName };
  }
  const typeName = parentPrefix + pascalCase(fieldName);
  return { typeName, childPrefix: typeName };
}

function isArrayOfObjects(validator: Validator): boolean {
  if (validator.type === "array") {
    return validator.value.type === "object" || isArrayOfObjects(validator.value);
  }
  if (validator.type === "union") {
    return validator.value.some(isArrayOfObjects);
  }
  return false;
}
