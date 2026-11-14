import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import {
  type Backend,
  failureOf,
  insertAccount,
  losAngelesTime,
  newBackend,
  paymentRows,
  RENT,
  seedTenant,
  signInAsMember,
} from "./seed.fixture";

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
  const { paymentId: february4 } = await logPayment(t, tenantId, "2026-02-04", 100000);
  await logPayment(t, tenantId, "2026-03-02", RENT);
  return { t, tenantId, february4 };
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: AFTER_MARCH_GRACE });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("payments.preview", () => {
  it("shows March becoming paid when the Feb 4 payment is raised from $1,000 to $1,500", async () => {
    const { t, tenantId, february4 } = await enterWorkedExample();

    const preview = await t.query(api.payments.preview, {
      tenantId,
      asOf: AFTER_MARCH_GRACE,
      proposed: { paymentId: february4, amountCents: RENT, dateReceived: "2026-02-04" },
    });

    expect(preview.changes).toEqual([{ month: "2026-03", before: "partial", after: "paid" }]);
  });

  it("shows February going back to partial when the Feb 4 payment is cut to $200 and March absorbs it", async () => {
    const { t, tenantId, february4 } = await enterWorkedExample();
    await logPayment(t, tenantId, "2026-03-03", 50000);

    const preview = await t.query(api.payments.preview, {
      tenantId,
      asOf: AFTER_MARCH_GRACE,
      proposed: { paymentId: february4, amountCents: 20000, dateReceived: "2026-02-04" },
    });

    expect(preview.changes).toEqual([{ month: "2026-03", before: "paid", after: "partial" }]);
  });

  it("shows a new payment clearing March", async () => {
    const { t, tenantId } = await enterWorkedExample();

    const preview = await t.query(api.payments.preview, {
      tenantId,
      asOf: AFTER_MARCH_GRACE,
      proposed: { amountCents: 50000, dateReceived: "2026-03-08" },
    });

    expect(preview.changes).toEqual([{ month: "2026-03", before: "partial", after: "paid" }]);
  });

  it("lists months of any year, in order, including future months a new payment would prepay", async () => {
    const { t, tenantId } = await enterWorkedExample();

    const preview = await t.query(api.payments.preview, {
      tenantId,
      asOf: AFTER_MARCH_GRACE,
      proposed: { amountCents: 14 * RENT, dateReceived: "2026-03-08" },
    });

    expect(preview.changes[0]).toEqual({ month: "2026-03", before: "partial", after: "paid" });
    expect(preview.changes.at(-1)?.month.startsWith("2027")).toBe(true);
    expect(preview.changes.map((change) => change.month)).toEqual(
      [...preview.changes.map((change) => change.month)].sort(),
    );
  });

  it("returns no changes when the proposal leaves every month as it is", async () => {
    const { t, tenantId, february4 } = await enterWorkedExample();

    const preview = await t.query(api.payments.preview, {
      tenantId,
      asOf: AFTER_MARCH_GRACE,
      proposed: { paymentId: february4, amountCents: 100000, dateReceived: "2026-02-04" },
    });

    expect(preview.changes).toEqual([]);
  });

  it("writes nothing", async () => {
    const { t, tenantId, february4 } = await enterWorkedExample();
    const before = await paymentRows(t, tenantId);

    await t.query(api.payments.preview, {
      tenantId,
      asOf: AFTER_MARCH_GRACE,
      proposed: { paymentId: february4, amountCents: RENT, dateReceived: "2026-02-04" },
    });

    expect(await paymentRows(t, tenantId)).toEqual(before);
  });

  it.each([
    ["an amount of $0.00", { amountCents: 0 }],
    ["a fractional amount", { amountCents: 100.5 }],
    ["a date that does not exist", { dateReceived: "2026-02-30" }],
    ["a date after today", { dateReceived: "2026-03-11" }],
  ])("rejects %s", async (_, fields) => {
    const { t, tenantId } = await enterWorkedExample();

    const failure = await failureOf(
      t.query(api.payments.preview, {
        tenantId,
        asOf: AFTER_MARCH_GRACE,
        proposed: { amountCents: 50000, dateReceived: "2026-03-08", ...fields },
      }),
    );

    expect(failure.code).toBe("ValidationFailed");
  });

  it("rejects previewing a cancelled payment", async () => {
    const { t, tenantId, february4 } = await enterWorkedExample();
    await t.mutation(api.payments.delete, { paymentId: february4, expectedVersion: 1, reason: "Twice" });

    const failure = await failureOf(
      t.query(api.payments.preview, {
        tenantId,
        asOf: AFTER_MARCH_GRACE,
        proposed: { paymentId: february4, amountCents: RENT, dateReceived: "2026-02-04" },
      }),
    );

    expect(failure.code).toBe("ValidationFailed");
  });

  it("lets any member preview and denies a non-member", async () => {
    const { t, tenantId } = await enterWorkedExample();
    const asSam = await signInAsMember(t, tenantId);
    await t.run((ctx) => insertAccount(ctx, { subject: "auth|eve", email: "eve@example.com", name: "Eve" }));
    const asEve = t.withIdentity({ subject: "auth|eve", email: "eve@example.com", name: "Eve" });
    const args = {
      tenantId,
      asOf: AFTER_MARCH_GRACE,
      proposed: { amountCents: 50000, dateReceived: "2026-03-08" },
    };

    const member = await asSam.query(api.payments.preview, args);
    const outsider = await failureOf(asEve.query(api.payments.preview, args));

    expect(member.changes).toHaveLength(1);
    expect(outsider).toMatchObject({ code: "PermissionDenied" });
  });
});
