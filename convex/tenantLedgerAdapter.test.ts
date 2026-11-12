import { convexTest } from "convex-test";
import { describe, expect, it } from "vitest";
import type { Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import schema from "./schema";
import { loadTenantLedger, tenantLedgerInput } from "./tenantLedgerAdapter";
import { loadTenantLedgerRows } from "./tenantLedgerRows";
import { modules } from "./test.setup";

const RENT = 150000;
const DELETED_AT = 1_700_000_000_000;

async function seedTenant(ctx: MutationCtx) {
  const accountId = await ctx.db.insert("accounts", { authSubject: "auth|owner", displayName: "Owner", version: 1 });
  const groupId = await ctx.db.insert("groups", {
    name: "1st Street",
    timeZone: "America/Los_Angeles",
    paymentOrder: "byDate",
    graceDays: 5,
    lateFeeFlatCents: 5000,
    ownerId: accountId,
    version: 1,
  });
  const tenantId = await ctx.db.insert("tenants", {
    groupId,
    name: "Apt 3B",
    moveIn: "2026-01-01",
    moveOut: "2026-12-15",
    status: "active",
    graceDaysOverride: 3,
    lateFeeFlatOverrideCents: 2500,
    version: 1,
  });
  return { accountId, groupId, tenantId };
}

async function insertLedgerRows(ctx: MutationCtx, tenantId: Id<"tenants">, accountId: Id<"accounts">) {
  const deleted = { deletedAt: DELETED_AT, deletedBy: accountId, deleteReason: "Entered twice" };
  return {
    term: await ctx.db.insert("rentTerms", {
      tenantId,
      startMonth: "2026-01",
      amountCents: RENT,
      dueDay: 1,
      version: 1,
    }),
    deletedTerm: await ctx.db.insert("rentTerms", {
      tenantId,
      startMonth: "2026-02",
      amountCents: 999900,
      dueDay: 15,
      version: 1,
      ...deleted,
    }),
    lateFee: await ctx.db.insert("debts", {
      tenantId,
      kind: "lateFee",
      amountCents: 2500,
      date: "2026-02-10",
      forMonth: "2026-02",
      createdBy: accountId,
      version: 1,
    }),
    deletedDebt: await ctx.db.insert("debts", {
      tenantId,
      kind: "custom",
      amountCents: 7000,
      date: "2026-01-20",
      createdBy: accountId,
      version: 1,
      ...deleted,
    }),
    payment: await ctx.db.insert("payments", {
      tenantId,
      amountCents: RENT,
      dateReceived: "2026-01-01",
      method: "zelle",
      clientId: "client-1",
      loggedBy: accountId,
      version: 1,
    }),
    bouncedPayment: await ctx.db.insert("payments", {
      tenantId,
      amountCents: RENT,
      dateReceived: "2026-02-01",
      method: "check",
      clientId: "client-2",
      loggedBy: accountId,
      bouncedAt: DELETED_AT,
      version: 1,
    }),
    deletedPayment: await ctx.db.insert("payments", {
      tenantId,
      amountCents: RENT,
      dateReceived: "2026-02-02",
      method: "cash",
      clientId: "client-3",
      loggedBy: accountId,
      version: 1,
      ...deleted,
    }),
  };
}

describe("loadTenantLedgerRows", () => {
  it("loads only the given tenant's rows, cancelled and bounced ones included", async () => {
    const t = convexTest(schema, modules);

    await t.run(async (ctx) => {
      const { accountId, groupId, tenantId } = await seedTenant(ctx);
      const ids = await insertLedgerRows(ctx, tenantId, accountId);
      const neighbourId = await ctx.db.insert("tenants", {
        groupId,
        name: "Apt 4A",
        moveIn: "2026-01-01",
        status: "active",
        version: 1,
      });
      await insertLedgerRows(ctx, neighbourId, accountId);

      const rows = await loadTenantLedgerRows(ctx, tenantId);

      expect(rows.rentTerms.map((term) => term._id)).toEqual([ids.term, ids.deletedTerm]);
      expect(rows.debts.map((debt) => debt._id)).toEqual([ids.lateFee, ids.deletedDebt]);
      expect(rows.payments.map((payment) => payment._id)).toEqual([
        ids.payment,
        ids.bouncedPayment,
        ids.deletedPayment,
      ]);
    });
  });

  it("returns empty lists for a tenant with no entries", async () => {
    const t = convexTest(schema, modules);

    await t.run(async (ctx) => {
      const { tenantId } = await seedTenant(ctx);

      expect(await loadTenantLedgerRows(ctx, tenantId)).toEqual({ rentTerms: [], debts: [], payments: [] });
    });
  });
});

describe("tenantLedgerInput", () => {
  it("maps the group, tenant and every row into the engine input", async () => {
    const t = convexTest(schema, modules);

    await t.run(async (ctx) => {
      const { accountId, groupId, tenantId } = await seedTenant(ctx);
      const ids = await insertLedgerRows(ctx, tenantId, accountId);
      const group = await ctx.db.get(groupId);
      const tenant = await ctx.db.get(tenantId);
      const rows = await loadTenantLedgerRows(ctx, tenantId);
      if (group === null || tenant === null) {
        throw new Error("Seeded rows are missing");
      }
      const creationTime = (row: { _creationTime: number } | undefined) => row?._creationTime;

      const input = tenantLedgerInput({ group, tenant, rows, today: "2027-03-10" });

      expect(input).toEqual({
        today: "2027-03-10",
        calendarYear: 2027,
        group: { paymentOrder: "byDate", graceDays: 5, lateFeeFlatCents: 5000 },
        tenant: {
          moveIn: "2026-01-01",
          moveOut: "2026-12-15",
          status: "active",
          graceDaysOverride: 3,
          lateFeeFlatOverrideCents: 2500,
        },
        rentTerms: [
          { id: ids.term, startMonth: "2026-01", amountCents: RENT, dueDay: 1, deletedAt: undefined },
          { id: ids.deletedTerm, startMonth: "2026-02", amountCents: 999900, dueDay: 15, deletedAt: DELETED_AT },
        ],
        debts: [
          {
            id: ids.lateFee,
            kind: "lateFee",
            amountCents: 2500,
            date: "2026-02-10",
            forMonth: "2026-02",
            creationTime: creationTime(rows.debts[0]),
            deletedAt: undefined,
          },
          {
            id: ids.deletedDebt,
            kind: "custom",
            amountCents: 7000,
            date: "2026-01-20",
            forMonth: undefined,
            creationTime: creationTime(rows.debts[1]),
            deletedAt: DELETED_AT,
          },
        ],
        payments: [
          {
            id: ids.payment,
            amountCents: RENT,
            dateReceived: "2026-01-01",
            creationTime: creationTime(rows.payments[0]),
            deletedAt: undefined,
            bouncedAt: undefined,
          },
          {
            id: ids.bouncedPayment,
            amountCents: RENT,
            dateReceived: "2026-02-01",
            creationTime: creationTime(rows.payments[1]),
            deletedAt: undefined,
            bouncedAt: DELETED_AT,
          },
          {
            id: ids.deletedPayment,
            amountCents: RENT,
            dateReceived: "2026-02-02",
            creationTime: creationTime(rows.payments[2]),
            deletedAt: DELETED_AT,
            bouncedAt: undefined,
          },
        ],
      });
    });
  });
});

describe("loadTenantLedger", () => {
  it("charges rent from the active term and ignores cancelled and bounced rows", async () => {
    const t = convexTest(schema, modules);

    await t.run(async (ctx) => {
      const { accountId, groupId, tenantId } = await seedTenant(ctx);
      await insertLedgerRows(ctx, tenantId, accountId);
      const group = await ctx.db.get(groupId);
      const tenant = await ctx.db.get(tenantId);
      if (group === null || tenant === null) {
        throw new Error("Seeded rows are missing");
      }

      const ledger = await loadTenantLedger(ctx, group, tenant, "2026-02-20");

      expect(ledger.summary.balance).toEqual({
        rentOwedCents: RENT,
        lateFeesOwedCents: 2500,
        otherOwedCents: 0,
        creditCents: 0,
      });
      expect(ledger.summary.state).toBe("late");
      expect(ledger.summary.nextDueDate).toBe("2026-03-01");
      expect(ledger.calendarMonths).toHaveLength(12);
    });
  });

  it("shows a tenant with no rent terms as paid up", async () => {
    const t = convexTest(schema, modules);

    await t.run(async (ctx) => {
      const { groupId, tenantId } = await seedTenant(ctx);
      const group = await ctx.db.get(groupId);
      const tenant = await ctx.db.get(tenantId);
      if (group === null || tenant === null) {
        throw new Error("Seeded rows are missing");
      }

      const ledger = await loadTenantLedger(ctx, group, tenant, "2026-02-20");

      expect(ledger.summary.state).toBe("paidUp");
      expect(ledger.summary.nextDueDate).toBeUndefined();
    });
  });
});
