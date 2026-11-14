import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import {
  type Backend,
  failureOf,
  insertAccount,
  losAngelesTime,
  newBackend,
  RENT,
  seedTenant,
  signInAsMember,
} from "./seed.fixture";

const MARCH_2 = losAngelesTime(2026, 3, 2);
const AFTER_MARCH_GRACE = losAngelesTime(2026, 3, 10);

let paymentCount = 0;

function logPayment(t: Backend, tenantId: Id<"tenants">, dateReceived: string, amountCents: number) {
  paymentCount += 1;
  return t.mutation(api.payments.log, { tenantId, clientId: `payment-${paymentCount}`, amountCents, dateReceived });
}

async function enterWorkedExample() {
  const t = await newBackend();
  const tenantId = await seedTenant(t);
  await logPayment(t, tenantId, "2026-01-03", RENT);
  await logPayment(t, tenantId, "2026-02-04", 100000);
  await t.mutation(api.debts.addLateFee, {
    tenantId,
    clientId: "fee-2026-02",
    amountCents: 5000,
    date: "2026-02-06",
    forMonth: "2026-02",
  });
  await logPayment(t, tenantId, "2026-03-02", RENT);
  return { t, tenantId };
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: MARCH_2 });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("tenants.screen calendar months and allocations", () => {
  it("returns the 12 months of the current year matching the month detail designs", async () => {
    const { t, tenantId } = await enterWorkedExample();

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: AFTER_MARCH_GRACE });

    expect(screen.calendarMonths.map((month) => month.month)).toHaveLength(12);
    expect(screen.calendarMonths[0]?.month).toBe("2026-01");
    expect(screen.calendarMonths[1]).toEqual({
      tenantId,
      month: "2026-02",
      state: "paid",
      rentCents: RENT,
      coveredCents: RENT,
      owedCents: 0,
      hasUnpaidLateFee: true,
      dueDate: "2026-02-01",
      graceEnd: "2026-02-06",
      wasLate: true,
      paidByGraceEndCents: 100000,
      lateFeeCents: 5000,
      lateFeeUnpaidCents: 5000,
    });
    expect(screen.calendarMonths[2]).toMatchObject({
      state: "partial",
      coveredCents: 100000,
      owedCents: 50000,
      paidByGraceEndCents: 100000,
      wasLate: true,
      suggestedLateFeeCents: 7500,
    });
    expect(screen.calendarMonths[3]).toMatchObject({ state: "upcoming", coveredCents: 0, wasLate: false });
  });

  it("returns every allocation with a unique key, splitting the Mar 2 payment between February and March", async () => {
    const { t, tenantId } = await enterWorkedExample();

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: AFTER_MARCH_GRACE });

    const march2Payment = screen.payments.find((payment) => payment.dateReceived === "2026-03-02");
    const allocationsOfMarch2 = screen.allocations.filter((allocation) => allocation.paymentId === march2Payment?._id);
    expect(allocationsOfMarch2).toMatchObject([
      { tenantId, chargeKind: "rent", month: "2026-02", amountCents: 50000 },
      { tenantId, chargeKind: "rent", month: "2026-03", amountCents: 100000 },
    ]);
    expect(new Set(screen.allocations.map((allocation) => allocation.key)).size).toBe(screen.allocations.length);
  });

  it("allocates to a debt by its id and to prepaid months", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await logPayment(t, tenantId, "2026-01-03", 4 * RENT);
    const debtId = await t.mutation(api.debts.addCustom, {
      tenantId,
      clientId: "parking",
      amountCents: 3000,
      date: "2026-01-05",
      note: "Parking",
    });

    const screen = await t.query(api.tenants.screen, { tenantId, asOf: MARCH_2 });

    expect(screen.allocations.find((allocation) => allocation.chargeKind === "debt")).toMatchObject({
      debtId,
      amountCents: 3000,
    });
    expect(screen.allocations.some((allocation) => allocation.month === "2026-04")).toBe(true);
  });
});

describe("tenants.calendar", () => {
  it("returns blank months before move-in for another year", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t, { moveIn: "2026-03-01" });

    const { calendarMonths } = await t.query(api.tenants.calendar, { tenantId, year: 2026, asOf: AFTER_MARCH_GRACE });

    expect(calendarMonths.map((month) => month.state).slice(0, 4)).toEqual([
      "notATenant",
      "notATenant",
      "owed",
      "upcoming",
    ]);
    const earlier = await t.query(api.tenants.calendar, { tenantId, year: 2025, asOf: AFTER_MARCH_GRACE });
    expect(earlier.calendarMonths.every((month) => month.state === "notATenant")).toBe(true);
  });

  it("shows prepaid months in a later year after an overpayment", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await logPayment(t, tenantId, "2026-01-03", 15 * RENT);

    const { calendarMonths } = await t.query(api.tenants.calendar, { tenantId, year: 2027, asOf: MARCH_2 });

    expect(calendarMonths.map((month) => month.state).slice(0, 4)).toEqual([
      "prepaid",
      "prepaid",
      "prepaid",
      "upcoming",
    ]);
    expect(calendarMonths[0]).toMatchObject({ coveredCents: RENT, dueDate: "2027-01-01" });
  });

  it.each([1899, 10000, 2026.5, Number.NaN])("rejects the year %s", async (year) => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.query(api.tenants.calendar, { tenantId, year, asOf: MARCH_2 }));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "Year" } });
  });

  it("lets any member read the calendar and denies a non-member", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    const asSam = await signInAsMember(t, tenantId);
    await t.run((ctx) => insertAccount(ctx, { subject: "auth|eve", email: "eve@example.com", name: "Eve" }));
    const asEve = t.withIdentity({ subject: "auth|eve", email: "eve@example.com", name: "Eve" });

    const member = await asSam.query(api.tenants.calendar, { tenantId, year: 2026, asOf: MARCH_2 });
    const outsider = await failureOf(asEve.query(api.tenants.calendar, { tenantId, year: 2026, asOf: MARCH_2 }));

    expect(member.calendarMonths).toHaveLength(12);
    expect(outsider).toMatchObject({ code: "PermissionDenied" });
  });

  it("fails with NotFound when the tenant is missing", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => ctx.db.delete(tenantId));

    const failure = await failureOf(t.query(api.tenants.calendar, { tenantId, year: 2026, asOf: MARCH_2 }));

    expect(failure.code).toBe("NotFound");
  });
});
