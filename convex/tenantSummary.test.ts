import { convexTest } from "convex-test";
import { describe, expect, it } from "vitest";
import type { TenantSummary } from "./engine/tenantLedger";
import schema from "./schema";
import { tenantSummary, tenantSummaryOf } from "./tenantSummary";
import { modules } from "./test.setup";

const engineSummary: TenantSummary = {
  balance: { rentOwedCents: 150000, lateFeesOwedCents: 5000, otherOwedCents: 0, creditCents: 0 },
  state: "late",
  nextDueDate: "2026-04-01",
  lateFeeSuggestion: { forMonth: "2026-03", amountCents: 7500 },
  partialMoveOutPeriod: { start: "2026-12-01", end: "2026-12-31" },
  mayNotApplyDebtIds: ["debt-1"],
};

async function insertTenant() {
  const t = convexTest(schema, modules);
  return await t.run(async (ctx) => {
    const ownerId = await ctx.db.insert("accounts", { authSubject: "auth|owner", displayName: "Owner", version: 1 });
    const groupId = await ctx.db.insert("groups", {
      name: "1st Street",
      timeZone: "America/Los_Angeles",
      paymentOrder: "rentFirst",
      graceDays: 5,
      ownerId,
      version: 1,
    });
    return await ctx.db.insert("tenants", {
      groupId,
      name: "Apt 3B",
      moveIn: "2026-01-01",
      status: "active",
      version: 1,
    });
  });
}

describe("tenantSummaryOf", () => {
  it("keeps the balance, state, next due date and partial move-out period, keyed by tenant", async () => {
    const tenantId = await insertTenant();

    expect(tenantSummaryOf(tenantId, engineSummary)).toEqual({
      tenantId,
      rentOwedCents: 150000,
      lateFeesOwedCents: 5000,
      otherOwedCents: 0,
      creditCents: 0,
      state: "late",
      nextDueDate: "2026-04-01",
      partialMoveOutPeriod: { start: "2026-12-01", end: "2026-12-31" },
    });
  });

  it("returns exactly the fields the tenantSummary validator declares", async () => {
    const tenantId = await insertTenant();

    const summary = tenantSummaryOf(tenantId, engineSummary);

    expect(Object.keys(summary).sort()).toEqual(Object.keys(tenantSummary.fields).sort());
  });
});
