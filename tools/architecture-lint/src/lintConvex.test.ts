import path from "node:path";
import { describe, expect, it } from "vitest";
import { lintConvexDirectory } from "./lintConvex.ts";

const fixtures = path.resolve(import.meta.dirname, "../fixtures");

function rulesBrokenIn(fixture: string): string[] {
  return lintConvexDirectory(path.join(fixtures, fixture)).map((violation) => violation.rule);
}

describe("access-helper rule", () => {
  it("passes when every public handler calls one of the four access helpers and internal functions are exempt", () => {
    expect(rulesBrokenIn("accessHelpers/passing")).toEqual([]);
  });

  it("fails when a public handler calls no access helper", () => {
    expect(rulesBrokenIn("accessHelpers/missingHelper")).toEqual(["access-helper"]);
  });

  it("fails when a handler calls a helper that is not on the allowed list", () => {
    expect(rulesBrokenIn("accessHelpers/unlistedHelper")).toEqual(["access-helper"]);
  });

  it("names every allowed helper in the violation message", () => {
    const [violation] = lintConvexDirectory(path.join(fixtures, "accessHelpers/missingHelper"));

    expect(violation?.message).toBe(
      "handler must call one of requireIdentity, requireAccount, requireMember, requirePermission",
    );
  });

  it("fails when the handler is not written inline", () => {
    expect(rulesBrokenIn("accessHelpers/handlerByReference")).toEqual(["access-helper"]);
  });
});

describe("no-hard-delete rule", () => {
  it("passes when ledger rows are cancelled with a patch and other tables are deleted", () => {
    expect(rulesBrokenIn("noHardDeletes/passing")).toEqual([]);
  });

  it("fails when a payment is deleted by id", () => {
    expect(rulesBrokenIn("noHardDeletes/deleteById")).toEqual(["no-hard-delete"]);
  });

  it("fails when a debt is deleted by table name", () => {
    expect(rulesBrokenIn("noHardDeletes/deleteByTableName")).toEqual(["no-hard-delete"]);
  });

  it("fails when a rent term is deleted through a table-scoped writer", () => {
    expect(rulesBrokenIn("noHardDeletes/tableScopedWriter")).toEqual(["no-hard-delete"]);
  });

  it("fails when the target table could be a soft-deleted one", () => {
    expect(rulesBrokenIn("noHardDeletes/unknownTable")).toEqual(["no-hard-delete"]);
  });
});

describe("violation location", () => {
  it("reports the file and line of the offending call", () => {
    const [violation] = lintConvexDirectory(path.join(fixtures, "noHardDeletes/deleteById"));

    expect(violation).toMatchObject({ file: "payments.ts", line: 5 });
  });
});
