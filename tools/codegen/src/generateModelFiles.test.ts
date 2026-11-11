import fs from "node:fs";
import path from "node:path";
import { describe, expect, it } from "vitest";
import { CodegenError } from "./buildModels.ts";
import type { ConvexFunction, Field, FunctionSpec, Validator } from "./functionSpec.ts";
import { generateModelFiles } from "./generateModelFiles.ts";
import { readFunctionSpecFile } from "./readFunctionSpec.ts";

const fixtures = path.resolve(import.meta.dirname, "../fixtures");

const string: Validator = { type: "string" };
const number: Validator = { type: "number" };

function required(fieldType: Validator): Field {
  return { fieldType, optional: false };
}

function optional(fieldType: Validator): Field {
  return { fieldType, optional: true };
}

function object(fields: Record<string, Field>): Validator {
  return { type: "object", value: fields };
}

function literals(...values: string[]): Validator {
  return { type: "union", value: values.map((value) => ({ type: "literal", value })) };
}

function query(identifier: string, args: Record<string, Field>, returns: Validator): ConvexFunction {
  return { identifier, functionType: "Query", visibility: { kind: "public" }, args: object(args), returns };
}

function generate(...functions: ConvexFunction[]): Map<string, string> {
  return generateModelFiles({ functions } satisfies FunctionSpec);
}

function readExpectedFiles(directory: string): Map<string, string> {
  const files = new Map<string, string>();
  for (const entry of fs.readdirSync(directory, { recursive: true, withFileTypes: true })) {
    if (entry.isFile()) {
      const filePath = path.join(entry.parentPath, entry.name);
      files.set(path.relative(directory, filePath), fs.readFileSync(filePath, "utf8"));
    }
  }
  return files;
}

function sortedEntries(files: Map<string, string>): [string, string][] {
  return [...files].sort(([a], [b]) => a.localeCompare(b));
}

describe("generateModelFiles", () => {
  it("generates the expected Kotlin for screen-shaped queries and a mutation", () => {
    const generated = generateModelFiles(readFunctionSpecFile(path.join(fixtures, "screens.json")));
    const expected = readExpectedFiles(path.join(fixtures, "screens.expected"));

    expect(sortedEntries(generated)).toEqual(sortedEntries(expected));
  });

  it("generates nothing when there are no public functions", () => {
    expect(generate().size).toBe(0);
  });

  it("names entities after the singular of their list key", () => {
    const files = generate(
      query(
        "tenants.js:screen",
        {},
        object({
          calendarMonths: required({ type: "array", value: object({ month: required(string) }) }),
          allocations: required({ type: "array", value: object({ amountCents: required(number) }) }),
          history: required({ type: "array", value: object({ action: required(string) }) }),
        }),
      ),
    );

    expect([...files.keys()].sort()).toEqual([
      "tenants/Allocation.kt",
      "tenants/CalendarMonth.kt",
      "tenants/History.kt",
      "tenants/TenantsScreenResult.kt",
    ]);
  });

  it("names nested objects and enums after their parent and field", () => {
    const files = generate(
      query(
        "groups.js:screen",
        { order: required(literals("rentFirst", "byDate")) },
        object({ settings: required(object({ graceDays: required(number) })) }),
      ),
    );

    expect(files.get("groups/GroupsScreenOrder.kt")).toContain("enum class GroupsScreenOrder {");
    expect(files.get("groups/GroupsScreenResult.kt")).toContain("val settings: GroupsScreenSettings,");
    expect(files.get("groups/GroupsScreenSettings.kt")).toContain("val graceDays: Long,");
  });

  it("defaults optional fields to null but keeps required nullable fields without a default", () => {
    const files = generate(
      query(
        "payments.js:edit",
        {
          note: optional(string),
          bouncedAt: required({ type: "union", value: [number, { type: "null" }] }),
        },
        { type: "null" },
      ),
    );

    const args = files.get("payments/PaymentsEditArgs.kt");
    expect(args).toContain("    val note: String? = null,\n");
    expect(args).toContain("    val bouncedAt: Long?,\n");
  });

  it("renames fields that are not valid Kotlin names", () => {
    const files = generate(query("debts.js:find", { when: required(string), _id: required(string) }, { type: "null" }));

    const args = files.get("debts/DebtsFindArgs.kt");
    expect(args).toContain('    @SerialName("when")\n    val whenValue: String,\n');
    expect(args).toContain('    @SerialName("_id")\n    val id: String,\n');
  });

  it("moves a type used by two Convex files into the shared package", () => {
    const tenantList = object({ tenants: required({ type: "array", value: object({ name: required(string) }) }) });
    const files = generate(query("home.js:screen", {}, tenantList), query("tenants.js:list", {}, tenantList));

    expect(files.has("shared/Tenant.kt")).toBe(true);
    expect(files.get("home/HomeScreenResult.kt")).toContain("import net.neednot.renttracker.model.shared.Tenant\n");
  });

  it("fails when one name is used for two different shapes", () => {
    const run = () =>
      generate(
        query(
          "home.js:screen",
          {},
          object({ tenants: required({ type: "array", value: object({ name: required(string) }) }) }),
        ),
        query(
          "tenants.js:list",
          {},
          object({ tenants: required({ type: "array", value: object({ moveIn: required(string) }) }) }),
        ),
      );

    expect(run).toThrow(CodegenError);
    expect(run).toThrow(/Kotlin type Tenant has two different shapes: home\.js:screen .* and tenants\.js:list/);
  });

  it("fails on int64 numbers", () => {
    expect(() =>
      generate(query("payments.js:log", { amount: required({ type: "bigint" }) }, { type: "null" })),
    ).toThrow("payments.js:log args.amount uses bigint, which codegen does not support");
  });

  it("fails on unions of different types", () => {
    expect(() =>
      generate(query("payments.js:log", { amount: required({ type: "union", value: [string, number] }) }, number)),
    ).toThrow("payments.js:log args.amount is a union of different types, which codegen does not support");
  });

  it("fails when a function has no returns validator", () => {
    const fn: ConvexFunction = { identifier: "app.js:ping", functionType: "Query", visibility: { kind: "public" } };
    fn.args = object({});

    expect(() => generate(fn)).toThrow("app.js:ping returns has no returns validator");
  });
});
