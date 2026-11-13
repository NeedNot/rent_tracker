import type { Infer } from "convex/values";
import { describe, expect, it } from "vitest";
import type { Doc, Id } from "./_generated/dataModel";
import { groupSummary, groupSummaryOf } from "./groupSummary";
import type { tenantSummary } from "./tenantSummary";

type TenantSummaryRow = Infer<typeof tenantSummary>;
type Owed = Partial<Pick<TenantSummaryRow, "rentOwedCents" | "lateFeesOwedCents" | "otherOwedCents" | "creditCents">>;

interface TenantCase {
  status: Doc<"tenants">["status"];
  state: TenantSummaryRow["state"];
  owed?: Owed;
}

const GROUP_ID = "group-1" as Id<"groups">;

function summarize(cases: readonly TenantCase[]) {
  const tenantId = (index: number) => `tenant-${index}` as Id<"tenants">;
  const tenants = cases.map(({ status }, index) => ({ _id: tenantId(index), status }));
  const tenantSummaries = cases.map(({ state, owed }, index) => ({
    tenantId: tenantId(index),
    rentOwedCents: 0,
    lateFeesOwedCents: 0,
    otherOwedCents: 0,
    creditCents: 0,
    state,
    ...owed,
  }));
  return groupSummaryOf(GROUP_ID, tenants, tenantSummaries);
}

const EMPTY = {
  groupId: GROUP_ID,
  activeTenantCount: 0,
  lateTenantCount: 0,
  totalOwedCents: 0,
  archivedTenantCount: 0,
  archivedOwedCents: 0,
};

describe("groupSummaryOf", () => {
  it.each<{ name: string; tenants: TenantCase[]; expected: Partial<Infer<typeof groupSummary>> }>([
    { name: "a group with no tenants is all zeros", tenants: [], expected: {} },
    {
      name: "total owed adds rent, late fees and other debts of every active tenant",
      tenants: [
        {
          status: "active",
          state: "late",
          owed: { rentOwedCents: 150000, lateFeesOwedCents: 5000, otherOwedCents: 2500 },
        },
        { status: "active", state: "owes", owed: { rentOwedCents: 80000 } },
      ],
      expected: { activeTenantCount: 2, lateTenantCount: 1, totalOwedCents: 237500 },
    },
    {
      name: "one tenant's credit does not reduce another tenant's debt",
      tenants: [
        { status: "active", state: "paidUp", owed: { creditCents: 90000 } },
        { status: "active", state: "owes", owed: { otherOwedCents: 40000 } },
      ],
      expected: { activeTenantCount: 2, totalOwedCents: 40000 },
    },
    {
      name: "archived tenants' debt is counted apart from the active total",
      tenants: [
        { status: "active", state: "late", owed: { rentOwedCents: 100000 } },
        { status: "archived", state: "owes", owed: { rentOwedCents: 30000, lateFeesOwedCents: 5000 } },
        { status: "archived", state: "paidUp" },
      ],
      expected: {
        activeTenantCount: 1,
        lateTenantCount: 1,
        totalOwedCents: 100000,
        archivedTenantCount: 2,
        archivedOwedCents: 35000,
      },
    },
    {
      name: "only late tenants count as late",
      tenants: [
        { status: "active", state: "late", owed: { rentOwedCents: 1 } },
        { status: "active", state: "late", owed: { rentOwedCents: 1 } },
        { status: "active", state: "owes", owed: { rentOwedCents: 1 } },
        { status: "active", state: "paidUp" },
      ],
      expected: { activeTenantCount: 4, lateTenantCount: 2, totalOwedCents: 3 },
    },
    {
      name: "an archived tenant is never counted as late",
      tenants: [{ status: "archived", state: "late", owed: { rentOwedCents: 70000 } }],
      expected: { archivedTenantCount: 1, archivedOwedCents: 70000 },
    },
  ])("$name", ({ tenants, expected }) => {
    expect(summarize(tenants)).toEqual({ ...EMPTY, ...expected });
  });

  it("returns exactly the fields the groupSummary validator declares", () => {
    expect(Object.keys(summarize([])).sort()).toEqual(Object.keys(groupSummary.fields).sort());
  });
});
