import path from "node:path";
import { describe, expect, it } from "vitest";
import { lintConvexDirectory } from "./lintConvex.ts";

const fixtures = path.resolve(import.meta.dirname, "../fixtures");

function rulesBrokenIn(fixture: string): string[] {
  return lintConvexDirectory(path.join(fixtures, fixture)).map((violation) => violation.rule);
}

describe("access-helper rule", () => {
  it("passes when every public handler calls an access helper and internal functions are exempt", () => {
    expect(rulesBrokenIn("accessHelpers/passing")).toEqual([]);
  });

  it("fails when a public handler calls no access helper", () => {
    expect(rulesBrokenIn("accessHelpers/missingHelper")).toEqual(["access-helper"]);
  });

  it("fails when the handler is not written inline", () => {
    expect(rulesBrokenIn("accessHelpers/handlerByReference")).toEqual(["access-helper"]);
  });
});

describe("history rules", () => {
  it("passes when history is inserted only by writeHistory in history.ts", () => {
    expect(rulesBrokenIn("historyWrites/passing")).toEqual([]);
  });

  it("fails when history is inserted outside writeHistory", () => {
    expect(rulesBrokenIn("historyWrites/insertOutsideHelper")).toEqual(["history-write-through-helper"]);
  });

  it("fails when a history row is patched by id", () => {
    expect(rulesBrokenIn("historyWrites/patchById")).toEqual(["history-immutable"]);
  });

  it("fails when a history row is deleted by table name", () => {
    expect(rulesBrokenIn("historyWrites/deleteByTableName")).toEqual(["history-immutable"]);
  });

  it("fails when a history row is replaced through a table-scoped writer", () => {
    expect(rulesBrokenIn("historyWrites/tableScopedWriter")).toEqual(["history-immutable"]);
  });

  it("fails when the target table could be history", () => {
    expect(rulesBrokenIn("historyWrites/unknownTable")).toEqual(["history-immutable"]);
  });
});

describe("violation location", () => {
  it("reports the file and line of the offending call", () => {
    const [violation] = lintConvexDirectory(path.join(fixtures, "historyWrites/insertOutsideHelper"));

    expect(violation).toMatchObject({ file: "payments.ts", line: 5 });
  });
});
