import type { WithoutSystemFields } from "convex/server";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import {
  type Backend,
  debtRows,
  failureOf,
  insertPayment,
  losAngelesTime,
  newBackend,
  paymentRow,
  paymentRows,
  seedTenant,
  signInAsMember,
  tenantRow,
} from "./seed.fixture";

const MARCH_20 = losAngelesTime(2026, 3, 20);
const payment = { amountCents: 150000, dateReceived: "2026-03-02", method: "zelle" as const };

function logArgs(tenantId: Id<"tenants">, overrides: Record<string, unknown> = {}) {
  return { tenantId, clientId: "client-1", ...payment, ...overrides };
}

async function seedPayment() {
  const t = await newBackend();
  const tenantId = await seedTenant(t);
  const { paymentId } = await t.mutation(api.payments.log, logArgs(tenantId));
  return { t, tenantId, paymentId };
}

function signInAsSam(
  t: Backend,
  tenantId: Id<"tenants">,
  fields: Partial<WithoutSystemFields<Doc<"memberships">>> = {},
) {
  return signInAsMember(t, tenantId, { payments: "addEdit", debts: "addEdit", ...fields });
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: MARCH_20 });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("payments.log", () => {
  it("saves the payment at version 1, logged by the caller", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const { paymentId, lateFeeId } = await t.mutation(api.payments.log, logArgs(tenantId, { note: "  March rent " }));

    expect(lateFeeId).toBeUndefined();
    expect(await paymentRow(t, paymentId)).toMatchObject({
      tenantId,
      ...payment,
      note: "March rent",
      clientId: "client-1",
      version: 1,
    });
  });

  it("remembers the method on the tenant without counting as a tenant edit", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    await t.mutation(api.payments.log, logArgs(tenantId, { method: "venmo" }));

    expect(await tenantRow(t, tenantId)).toMatchObject({ lastPaymentMethod: "venmo", version: 1 });
  });

  it("saves a payment without a method and leaves the remembered method alone", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(
      api.payments.log,
      logArgs(tenantId, { clientId: "first", method: "check", dateReceived: "2026-02-01" }),
    );

    const { paymentId } = await t.mutation(api.payments.log, {
      tenantId,
      clientId: "second",
      amountCents: 5000,
      dateReceived: "2026-03-10",
    });

    expect((await paymentRow(t, paymentId)).method).toBeUndefined();
    expect(await tenantRow(t, tenantId)).toMatchObject({ lastPaymentMethod: "check" });
  });

  it("saves the late fee in the same call, dated today in the group's time zone", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const { paymentId, lateFeeId } = await t.mutation(
      api.payments.log,
      logArgs(tenantId, { lateFee: { amountCents: 7500, forMonth: "2026-02" } }),
    );

    expect(paymentId).toBeDefined();
    expect(await debtRows(t, tenantId)).toMatchObject([
      {
        _id: lateFeeId,
        kind: "lateFee",
        amountCents: 7500,
        forMonth: "2026-02",
        date: "2026-03-20",
        clientId: "client-1:lateFee",
        version: 1,
      },
    ]);
  });

  it("saves nothing when the late fee is invalid", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(
      t.mutation(api.payments.log, logArgs(tenantId, { lateFee: { amountCents: 7500, forMonth: "2026-13" } })),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "For month" } });
    expect(await paymentRows(t, tenantId)).toEqual([]);
  });

  it.each([
    ["an amount of zero", { amountCents: 0 }, "Amount"],
    ["a fractional amount", { amountCents: 10.5 }, "Amount"],
    ["an impossible date", { dateReceived: "2026-02-30" }, "Date received"],
    ["a date after today", { dateReceived: "2026-03-21" }, "Date received"],
    ["a note over 2,000 characters", { note: "x".repeat(2001) }, "Notes"],
  ])("rejects %s", async (_name, overrides, field) => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.mutation(api.payments.log, logArgs(tenantId, overrides)));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field } });
    expect(await paymentRows(t, tenantId)).toEqual([]);
  });

  it("explains a future date in words", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const failure = await failureOf(t.mutation(api.payments.log, logArgs(tenantId, { dateReceived: "2026-03-21" })));

    expect(failure.message).toBe("Date received can't be later than today.");
  });

  it("accepts today's date in the group's time zone", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);

    const { paymentId } = await t.mutation(api.payments.log, logArgs(tenantId, { dateReceived: "2026-03-20" }));

    expect(await paymentRow(t, paymentId)).toMatchObject({ dateReceived: "2026-03-20" });
  });

  it("refuses an archived tenant", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.payments.log, logArgs(tenantId)));

    expect(failure).toEqual({
      code: "TenantArchived",
      message: "This tenant is archived. Restore the tenant to change payments and charges.",
      details: {},
    });
  });

  it("returns the same ids and writes nothing when the clientId repeats", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    const args = logArgs(tenantId, { lateFee: { amountCents: 7500, forMonth: "2026-02" } });
    const first = await t.mutation(api.payments.log, args);

    const retry = await t.mutation(api.payments.log, args);

    expect(retry).toEqual(first);
    expect(await paymentRows(t, tenantId)).toHaveLength(1);
    expect(await debtRows(t, tenantId)).toHaveLength(1);
  });

  it("rejects a clientId already used for another tenant", async () => {
    const { t, tenantId: first } = await seedPayment();
    const other = await t.mutation(api.tenants.add, {
      groupId: (await tenantRow(t, first)).groupId,
      name: "Apt 4",
      moveIn: "2026-01-01",
      amountCents: 100000,
      dueDay: 1,
    });

    const failure = await failureOf(t.mutation(api.payments.log, logArgs(other)));

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "clientId" } });
    expect(await paymentRows(t, other)).toEqual([]);
  });

  it("needs both payments and debts permission to add a late fee", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    const asSam = await signInAsSam(t, tenantId, { debts: "read" });

    const failure = await failureOf(
      asSam.mutation(api.payments.log, logArgs(tenantId, { lateFee: { amountCents: 100, forMonth: "2026-02" } })),
    );

    expect(failure.code).toBe("PermissionDenied");
    expect(await paymentRows(t, tenantId)).toEqual([]);
  });
});

describe("payments.log duplicate warning", () => {
  async function tenantWithPaymentOn(dateReceived: string, fields: Record<string, unknown> = {}) {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.run((ctx) => insertPayment(ctx, tenantId, { amountCents: 150000, dateReceived }, fields));
    return { t, tenantId };
  }

  it.each([
    ["the same day", "2026-03-02", true],
    ["3 days earlier", "2026-02-27", true],
    ["3 days later", "2026-03-05", true],
    ["4 days earlier", "2026-02-26", false],
    ["4 days later", "2026-03-06", false],
  ])("a same-amount payment %s asks first: %s", async (_name, existingDate, asksFirst) => {
    const { t, tenantId } = await tenantWithPaymentOn(existingDate);

    const call = t.mutation(api.payments.log, logArgs(tenantId));

    if (asksFirst) {
      expect(await failureOf(call)).toMatchObject({ code: "NeedsConfirmation" });
      expect(await paymentRows(t, tenantId)).toHaveLength(1);
    } else {
      await call;
      expect(await paymentRows(t, tenantId)).toHaveLength(2);
    }
  });

  it("lists the match with who logged it and the message to show", async () => {
    const { t, tenantId } = await tenantWithPaymentOn("2026-03-02");
    const [existing] = await paymentRows(t, tenantId);

    const failure = await failureOf(t.mutation(api.payments.log, logArgs(tenantId)));

    expect(failure).toEqual({
      code: "NeedsConfirmation",
      message: "A similar payment was logged on Mar 2. Save anyway?",
      details: {
        matches: [
          {
            kind: "payment",
            id: existing?._id,
            date: "2026-03-02",
            amountCents: 150000,
            method: "zelle",
            byName: "Dana",
            byAccountId: existing?.loggedBy,
          },
        ],
      },
    });
  });

  it("does not ask about a different amount", async () => {
    const { t, tenantId } = await tenantWithPaymentOn("2026-03-02");

    await t.mutation(api.payments.log, logArgs(tenantId, { amountCents: 150001 }));

    expect(await paymentRows(t, tenantId)).toHaveLength(2);
  });

  it.each([
    ["cancelled", { deletedAt: 1 }],
    ["bounced", { bouncedAt: 1 }],
  ])("ignores a %s payment", async (_state, fields) => {
    const { t, tenantId } = await tenantWithPaymentOn("2026-03-02", fields);

    await t.mutation(api.payments.log, logArgs(tenantId));

    expect(await paymentRows(t, tenantId)).toHaveLength(2);
  });

  it("saves when the caller confirms", async () => {
    const { t, tenantId } = await tenantWithPaymentOn("2026-03-02");

    await t.mutation(api.payments.log, logArgs(tenantId, { confirmed: true }));

    expect(await paymentRows(t, tenantId)).toHaveLength(2);
  });

  it("lists the payment and the late fee together and saves both on one confirmation", async () => {
    const { t, tenantId } = await tenantWithPaymentOn("2026-03-02");
    await t.mutation(api.debts.addLateFee, {
      tenantId,
      clientId: "fee-1",
      amountCents: 7500,
      date: "2026-02-06",
      forMonth: "2026-02",
    });
    const args = logArgs(tenantId, { lateFee: { amountCents: 7500, forMonth: "2026-02" } });

    const failure = await failureOf(t.mutation(api.payments.log, args));
    const confirmed = await t.mutation(api.payments.log, { ...args, confirmed: true });

    expect(failure.message).toBe(
      "A similar payment was logged on Mar 2. A late fee for February was already added. Save anyway?",
    );
    expect(failure.details.matches).toMatchObject([
      { kind: "payment" },
      { kind: "lateFee", amountCents: 7500, byName: "Dana" },
    ]);
    expect(confirmed.lateFeeId).toBeDefined();
    expect(await paymentRows(t, tenantId)).toHaveLength(2);
    expect(await debtRows(t, tenantId)).toHaveLength(2);
  });

  it("asks about the late fee alone when only the fee is a duplicate", async () => {
    const t = await newBackend();
    const tenantId = await seedTenant(t);
    await t.mutation(api.debts.addLateFee, {
      tenantId,
      clientId: "fee-1",
      amountCents: 7500,
      date: "2026-02-06",
      forMonth: "2026-02",
    });

    const failure = await failureOf(
      t.mutation(api.payments.log, logArgs(tenantId, { lateFee: { amountCents: 7500, forMonth: "2026-02" } })),
    );

    expect(failure.details.matches).toMatchObject([{ kind: "lateFee" }]);
    expect(await paymentRows(t, tenantId)).toEqual([]);
  });
});

describe("payments.edit", () => {
  it("changes the fields in place, stamps the editor and bumps the version", async () => {
    const { t, paymentId } = await seedPayment();

    await t.mutation(api.payments.edit, {
      paymentId,
      expectedVersion: 1,
      amountCents: 140000,
      dateReceived: "2026-03-03",
      method: "cash",
      note: "Short",
    });

    expect(await paymentRow(t, paymentId)).toMatchObject({
      amountCents: 140000,
      dateReceived: "2026-03-03",
      method: "cash",
      note: "Short",
      version: 2,
      lastEditedAt: expect.any(Number),
    });
  });

  it("clears the method and note when they are left out", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.edit, {
      paymentId,
      expectedVersion: 1,
      amountCents: 100,
      dateReceived: "2026-03-03",
      note: "x",
    });

    await t.mutation(api.payments.edit, {
      paymentId,
      expectedVersion: 2,
      amountCents: 100,
      dateReceived: "2026-03-03",
    });

    const row = await paymentRow(t, paymentId);
    expect(row.method).toBeUndefined();
    expect(row.note).toBeUndefined();
  });

  it("rejects a date after today", async () => {
    const { t, paymentId } = await seedPayment();

    const failure = await failureOf(
      t.mutation(api.payments.edit, { paymentId, expectedVersion: 1, amountCents: 100, dateReceived: "2026-03-21" }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", details: { field: "Date received" } });
  });

  it("names the person who changed it first when the version is stale", async () => {
    const { t, tenantId, paymentId } = await seedPayment();
    const asSam = await signInAsSam(t, tenantId, { editOthers: true });
    await asSam.mutation(api.payments.edit, {
      paymentId,
      expectedVersion: 1,
      amountCents: 100,
      dateReceived: "2026-03-03",
    });

    const failure = await failureOf(
      t.mutation(api.payments.edit, { paymentId, expectedVersion: 1, amountCents: 200, dateReceived: "2026-03-03" }),
    );

    expect(failure).toMatchObject({
      code: "EditConflict",
      message: "Sam changed this payment while you were editing.",
      details: { currentVersion: 2, lastEditedByName: "Sam" },
    });
  });

  it("refuses a cancelled payment", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Entered twice" });

    const failure = await failureOf(
      t.mutation(api.payments.edit, { paymentId, expectedVersion: 2, amountCents: 100, dateReceived: "2026-03-03" }),
    );

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "This payment was cancelled. Restore it to change it.",
    });
  });

  it("refuses a bounced payment", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 });

    const failure = await failureOf(
      t.mutation(api.payments.edit, { paymentId, expectedVersion: 2, amountCents: 100, dateReceived: "2026-03-03" }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "A bounced payment can't be edited." });
  });

  it("refuses an archived tenant", async () => {
    const { t, tenantId, paymentId } = await seedPayment();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(
      t.mutation(api.payments.edit, { paymentId, expectedVersion: 1, amountCents: 100, dateReceived: "2026-03-03" }),
    );

    expect(failure.code).toBe("TenantArchived");
  });
});

describe("payments.delete", () => {
  it("cancels the payment with who, when and why, and keeps the row", async () => {
    const { t, paymentId } = await seedPayment();

    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: " Entered twice " });

    expect(await paymentRow(t, paymentId)).toMatchObject({
      deletedAt: expect.any(Number),
      deleteReason: "Entered twice",
      version: 2,
    });
  });

  it.each([
    ["no reason", "   ", "Reason is required."],
    ["a reason over 100 characters", "x".repeat(101), "Reason must be 100 characters or fewer."],
  ])("rejects %s", async (_name, reason, message) => {
    const { t, paymentId } = await seedPayment();

    const failure = await failureOf(t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message });
    expect((await paymentRow(t, paymentId)).deletedAt).toBeUndefined();
  });

  it("rejects a payment that is already cancelled", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" });

    const failure = await failureOf(
      t.mutation(api.payments.delete, { paymentId, expectedVersion: 2, reason: "Again" }),
    );

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This payment is already cancelled." });
  });

  it("can cancel a bounced payment", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 });

    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 2, reason: "Gave up" });

    expect(await paymentRow(t, paymentId)).toMatchObject({ deletedAt: expect.any(Number), version: 3 });
  });

  it("reports a stale version", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.edit, {
      paymentId,
      expectedVersion: 1,
      amountCents: 100,
      dateReceived: "2026-03-03",
    });

    const failure = await failureOf(
      t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" }),
    );

    expect(failure).toMatchObject({ code: "EditConflict", details: { currentVersion: 2, lastEditedByName: "Dana" } });
  });

  it("refuses an archived tenant", async () => {
    const { t, tenantId, paymentId } = await seedPayment();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(
      t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" }),
    );

    expect(failure.code).toBe("TenantArchived");
  });
});

describe("payments.restore", () => {
  it("clears the cancellation", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" });

    await t.mutation(api.payments.restore, { paymentId, expectedVersion: 2 });

    const row = await paymentRow(t, paymentId);
    expect(row).toMatchObject({ version: 3 });
    expect([row.deletedAt, row.deletedBy, row.deleteReason]).toEqual([undefined, undefined, undefined]);
  });

  it("clears the bounce", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 });

    await t.mutation(api.payments.restore, { paymentId, expectedVersion: 2 });

    const row = await paymentRow(t, paymentId);
    expect([row.bouncedAt, row.bouncedBy, row.version]).toEqual([undefined, undefined, 3]);
  });

  it("brings a cancelled and bounced payment back as bounced, then clears the bounce", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 });
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 2, reason: "Gave up" });

    await t.mutation(api.payments.restore, { paymentId, expectedVersion: 3 });
    const afterFirst = await paymentRow(t, paymentId);
    await t.mutation(api.payments.restore, { paymentId, expectedVersion: 4 });
    const afterSecond = await paymentRow(t, paymentId);

    expect([afterFirst.deletedAt, afterFirst.bouncedAt !== undefined]).toEqual([undefined, true]);
    expect(afterSecond.bouncedAt).toBeUndefined();
  });

  it("rejects a payment that is neither cancelled nor bounced", async () => {
    const { t, paymentId } = await seedPayment();

    const failure = await failureOf(t.mutation(api.payments.restore, { paymentId, expectedVersion: 1 }));

    expect(failure).toMatchObject({
      code: "ValidationFailed",
      message: "This payment is not cancelled or bounced.",
    });
  });

  it("names who cancelled it when the version is stale", async () => {
    const { t, tenantId, paymentId } = await seedPayment();
    const asSam = await signInAsSam(t, tenantId, { editOthers: true });
    await asSam.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" });

    const failure = await failureOf(t.mutation(api.payments.restore, { paymentId, expectedVersion: 1 }));

    expect(failure).toMatchObject({ code: "EditConflict", details: { lastEditedByName: "Sam" } });
  });

  it("refuses an archived tenant", async () => {
    const { t, tenantId, paymentId } = await seedPayment();
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" });
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.payments.restore, { paymentId, expectedVersion: 2 }));

    expect(failure.code).toBe("TenantArchived");
  });
});

describe("payments.markBounced", () => {
  it("marks the payment bounced by the caller and bumps the version", async () => {
    const { t, paymentId } = await seedPayment();

    await t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 });

    expect(await paymentRow(t, paymentId)).toMatchObject({ bouncedAt: expect.any(Number), version: 2 });
  });

  it("rejects a cancelled payment", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.delete, { paymentId, expectedVersion: 1, reason: "Twice" });

    const failure = await failureOf(t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 2 }));

    expect(failure.code).toBe("ValidationFailed");
  });

  it("rejects a payment that is already bounced", async () => {
    const { t, paymentId } = await seedPayment();
    await t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 2 }));

    expect(failure).toMatchObject({ code: "ValidationFailed", message: "This payment is already marked bounced." });
  });

  it("refuses an archived tenant", async () => {
    const { t, tenantId, paymentId } = await seedPayment();
    await t.mutation(api.tenants.archive, { tenantId, expectedVersion: 1 });

    const failure = await failureOf(t.mutation(api.payments.markBounced, { paymentId, expectedVersion: 1 }));

    expect(failure.code).toBe("TenantArchived");
  });
});
