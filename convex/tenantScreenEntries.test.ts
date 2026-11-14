import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import {
  type Backend,
  debtRows,
  insertAccount,
  losAngelesTime,
  newBackend,
  paymentRows,
  RENT,
  sam,
  seedTenant,
  signInAsMember,
  tenantRow,
  termRows,
} from "./seed.fixture";

const MARCH_2 = losAngelesTime(2026, 3, 2);

let paymentCount = 0;

function logPayment(t: Backend, tenantId: Id<"tenants">, dateReceived: string, amountCents: number) {
  paymentCount += 1;
  return t.mutation(api.payments.log, { tenantId, clientId: `payment-${paymentCount}`, amountCents, dateReceived });
}

function addLateFee(t: Backend, tenantId: Id<"tenants">, date: string, forMonth: string, amountCents: number) {
  return t.mutation(api.debts.addLateFee, { tenantId, clientId: `fee-${forMonth}`, amountCents, date, forMonth });
}

async function enterWorkedExample() {
  const t = await newBackend();
  const tenantId = await seedTenant(t);
  await logPayment(t, tenantId, "2026-01-03", RENT);
  await logPayment(t, tenantId, "2026-02-04", 100000);
  await addLateFee(t, tenantId, "2026-02-06", "2026-02", 5000);
  await logPayment(t, tenantId, "2026-03-02", RENT);
  return { t, tenantId };
}

async function useByDateOrder(t: Backend, tenantId: Id<"tenants">) {
  const { groupId } = await tenantRow(t, tenantId);
  await t.run((ctx) => ctx.db.patch(groupId, { paymentOrder: "byDate" }));
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: MARCH_2 });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("product spec worked example, entered through the mutations", () => {
  it("Rent first: the tenant owes $550, $500 of it rent and $50 late fee", async () => {
    const { t, tenantId } = await enterWorkedExample();

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.tenantSummaries).toMatchObject([
      { rentOwedCents: 50000, lateFeesOwedCents: 5000, otherOwedCents: 0, creditCents: 0, state: "owes" },
    ]);
  });

  it("By date: the tenant owes $550, all of it rent", async () => {
    const { t, tenantId } = await enterWorkedExample();
    await useByDateOrder(t, tenantId);

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.tenantSummaries).toMatchObject([
      { rentOwedCents: 55000, lateFeesOwedCents: 0, otherOwedCents: 0, creditCents: 0, state: "owes" },
    ]);
  });

  it.each(["rentFirst", "byDate"] as const)("%s: the running balance matches the spec's table", async (order) => {
    const { t, tenantId } = await enterWorkedExample();
    if (order === "byDate") {
      await useByDateOrder(t, tenantId);
    }

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.ledgerRows.map((row) => [row.kind, row.date, row.balanceOwedCents])).toEqual([
      ["rent", "2026-01-01", 150000],
      ["payment", "2026-01-03", 0],
      ["rent", "2026-02-01", 150000],
      ["payment", "2026-02-04", 50000],
      ["debt", "2026-02-06", 55000],
      ["rent", "2026-03-01", 205000],
      ["payment", "2026-03-02", 55000],
    ]);
    expect(screen.ledgerRows.map((row) => row.position)).toEqual([0, 1, 2, 3, 4, 5, 6]);
  });
});

describe("tenants.screen entries", () => {
  it("returns every payment and debt, cancelled and bounced included", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    const { paymentId } = await logPayment(t, tenantId, "2026-01-03", RENT);
    const { paymentId: bouncedId } = await logPayment(t, tenantId, "2026-02-03", 1000);
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" });
    await t.mutation(api.payments.markBounced, { paymentId: bouncedId, expectedVersion: 1 });
    const debtId = await t.mutation(api.debts.addCustom, {
      tenantId,
      clientId: "c1",
      amountCents: 500,
      date: "2026-02-10",
      note: "Key",
    });
    await t.mutation(api.debts.delete, { debtId, expectedVersion: 1, reason: "Mistake" });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.payments).toEqual(await paymentRows(t, tenantId));
    expect(screen.debts).toEqual(await debtRows(t, tenantId));
    expect(screen.payments.map((row) => [row.deletedAt !== undefined, row.bouncedAt !== undefined])).toEqual([
      [true, false],
      [false, true],
    ]);
  });

  it("gives cancelled and bounced rows no running balance", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    const { paymentId } = await logPayment(t, tenantId, "2026-01-03", RENT);
    await t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    const bounced = screen.ledgerRows.find((row) => row.entryId === paymentId);
    expect(bounced).toMatchObject({ kind: "payment", key: `payment:${paymentId}`, tenantId });
    expect(bounced?.balanceOwedCents).toBeUndefined();
    expect(bounced?.balanceCreditCents).toBeUndefined();
  });

  it("names every account that touched an entry or a rent term, once", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    const asSam = await signInAsMember(t, tenantId, { payments: "addEdit", editOthers: true, rentTerms: "addEdit" });
    const { paymentId } = await logPayment(asSam, tenantId, "2026-01-03", RENT);
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" });
    const rentTermId = await asSam.mutation(api.rentTerms.add, {
      tenantId,
      startMonth: "2026-06",
      amountCents: 160000,
      dueDay: 1,
    });
    await asSam.mutation(api.rentTerms.delete, { rentTermId, expectedVersion: 1, reason: "Mistake" });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.accounts.map((account) => account.displayName).sort()).toEqual(["Dana", "Sam"]);
  });

  it("returns an empty account list for a tenant with no entries and no edits", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.accounts).toEqual([]);
    expect(screen.payments).toEqual([]);
    expect(screen.debts).toEqual([]);
    expect(screen.rentTerms).toEqual(await termRows(t, tenantId));
  });

  it("returns only the caller's own membership, with the account id", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    const asSam = await signInAsMember(t, tenantId, { payments: "addEdit", editOthers: true });
    const samId = await t.run((ctx) => insertAccount(ctx, sam));

    const screen = await asSam.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.memberships).toMatchObject([
      { accountId: samId, role: "member", payments: "addEdit", editOthers: true },
    ]);
  });

  it("returns the owner's own membership to the owner", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.memberships).toMatchObject([{ role: "owner" }]);
  });

  it("exposes the late fee suggestion as flat summary fields", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: losAngelesTime(2026, 2, 20) });

    expect(screen.tenantSummaries).toMatchObject([{ lateFeeSuggestionMonth: "2026-02", lateFeeSuggestionCents: 7500 }]);
  });

  it("has no suggestion once the late month has a fee", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await logPayment(t, tenantId, "2026-01-02", RENT);
    await addLateFee(t, tenantId, "2026-02-07", "2026-02", 7500);

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: losAngelesTime(2026, 2, 20) });

    expect(screen.tenantSummaries[0]?.lateFeeSuggestionMonth).toBeUndefined();
    expect(screen.tenantSummaries[0]?.lateFeeSuggestionCents).toBeUndefined();
  });

  it("hides a group's tenant from someone who is not a member", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => insertAccount(ctx, { subject: "auth|eve", email: "eve@example.com", name: "Eve" }));

    const call = t
      .withIdentity({ subject: "auth|eve", email: "eve@example.com", name: "Eve" })
      .query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    await expect(call).rejects.toThrow();
  });
});
