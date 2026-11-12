import { describe, expect, it } from "vitest";
import { groupDoc, rentTermDoc, tenantDoc } from "./documentValidators";
import schema from "./schema";

describe("document validators", () => {
  it.each([
    ["groups", groupDoc],
    ["tenants", tenantDoc],
    ["rentTerms", rentTermDoc],
  ] as const)("%s documents carry the table's fields plus _id and _creationTime", (table, doc) => {
    const tableFields = Object.keys(schema.tables[table].validator.fields);

    expect(Object.keys(doc.fields).sort()).toEqual([...tableFields, "_id", "_creationTime"].sort());
    expect(doc.fields._id).toMatchObject({ kind: "id", tableName: table });
  });
});
