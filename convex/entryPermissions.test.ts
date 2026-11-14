import type { WithoutSystemFields } from "convex/server";
import { ConvexError } from "convex/values";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import type { Doc, Id } from "./_generated/dataModel";
import type { MutationCtx } from "./_generated/server";
import {
  type Backend,
  failureOf,
  insertAccount,
  insertDebt,
  insertMembership,
  insertPayment,
  losAngelesTime,
  newBackend,
  owner,
  sam,
  seedTenant,
  tenantRow,
} from "./seed.fixture";

type MembershipFields = Partial<WithoutSystemFields<Doc<"memberships">>>;
type Outcome = "allowed" | "denied";
type Seed = (ctx: MutationCtx, tenantId: Id<"tenants">, addedBy: Id<"accounts">) => Promise<string>;

interface Caller {
  level: string;
  membership: MembershipFields | "owner";
  changesOwn: Outcome;
  changesOthers: Outcome;
}

interface EntryFunction {
  name: string;
  seed: Seed;
  run: (caller: Backend, id: string) => Promise<unknown>;
}

const MARCH_20 = losAngelesTime(2026, 3, 20);

const callers: Caller[] = [
  { level: "read", membership: { payments: "read", debts: "read" }, changesOwn: "denied", changesOthers: "denied" },
  {
    level: "add & edit",
    membership: { payments: "addEdit", debts: "addEdit" },
    changesOwn: "allowed",
    changesOthers: "denied",
  },
  {
    level: "add & edit with edit others",
    membership: { payments: "addEdit", debts: "addEdit", editOthers: true },
    changesOwn: "allowed",
    changesOthers: "allowed",
  },
  { level: "full", membership: { payments: "full", debts: "full" }, changesOwn: "allowed", changesOthers: "allowed" },
  { level: "owner", membership: "owner", changesOwn: "allowed", changesOthers: "allowed" },
];

const payment = { amountCents: 50000, dateReceived: "2026-03-02" };
const lateFeeDebt = { kind: "lateFee" as const, amountCents: 5000, date: "2026-03-07" };
const customDebt = { kind: "custom" as const, amountCents: 5000, date: "2026-03-07" };
const cancelled = { deletedAt: 1 };

const entryFunctions: EntryFunction[] = [
  {
    name: "payments.edit",
    seed: (ctx, tenantId, loggedBy) => insertPayment(ctx, tenantId, payment, { loggedBy }),
    run: (caller, id) =>
      caller.mutation(api.payments.edit, {
        paymentId: id as Id<"payments">,
        expectedVersion: 1,
        amountCents: 100,
        dateReceived: "2026-03-03",
      }),
  },
  {
    name: "payments.delete",
    seed: (ctx, tenantId, loggedBy) => insertPayment(ctx, tenantId, payment, { loggedBy }),
    run: (caller, id) =>
      caller.mutation(api.payments.delete, { paymentId: id as Id<"payments">, expectedVersion: 1, reason: "Twice" }),
  },
  {
    name: "payments.restore",
    seed: (ctx, tenantId, loggedBy) => insertPayment(ctx, tenantId, payment, { loggedBy, ...cancelled }),
    run: (caller, id) => caller.mutation(api.payments.restore, { paymentId: id as Id<"payments">, expectedVersion: 1 }),
  },
  {
    name: "payments.markBounced",
    seed: (ctx, tenantId, loggedBy) => insertPayment(ctx, tenantId, payment, { loggedBy }),
    run: (caller, id) =>
      caller.mutation(api.payments.markBounced, { paymentId: id as Id<"payments">, expectedVersion: 1 }),
  },
  {
    name: "debts.edit on a late fee",
    seed: (ctx, tenantId, createdBy) => insertDebt(ctx, tenantId, lateFeeDebt, { createdBy }),
    run: (caller, id) =>
      caller.mutation(api.debts.edit, {
        debtId: id as Id<"debts">,
        expectedVersion: 1,
        amountCents: 100,
        date: "2026-03-08",
        forMonth: "2026-03",
      }),
  },
  {
    name: "debts.edit on a custom debt",
    seed: (ctx, tenantId, createdBy) => insertDebt(ctx, tenantId, customDebt, { createdBy }),
    run: (caller, id) =>
      caller.mutation(api.debts.edit, {
        debtId: id as Id<"debts">,
        expectedVersion: 1,
        amountCents: 100,
        date: "2026-03-08",
        note: "Lock",
      }),
  },
  {
    name: "debts.delete",
    seed: (ctx, tenantId, createdBy) => insertDebt(ctx, tenantId, customDebt, { createdBy }),
    run: (caller, id) =>
      caller.mutation(api.debts.delete, { debtId: id as Id<"debts">, expectedVersion: 1, reason: "Mistake" }),
  },
  {
    name: "debts.restore",
    seed: (ctx, tenantId, createdBy) => insertDebt(ctx, tenantId, customDebt, { createdBy, ...cancelled }),
    run: (caller, id) => caller.mutation(api.debts.restore, { debtId: id as Id<"debts">, expectedVersion: 1 }),
  },
];

const creations: { name: string; run: (caller: Backend, tenantId: Id<"tenants">) => Promise<unknown> }[] = [
  {
    name: "payments.log",
    run: (caller, tenantId) =>
      caller.mutation(api.payments.log, { tenantId, clientId: "c1", amountCents: 100, dateReceived: "2026-03-02" }),
  },
  {
    name: "debts.addLateFee",
    run: (caller, tenantId) =>
      caller.mutation(api.debts.addLateFee, {
        tenantId,
        clientId: "c1",
        amountCents: 100,
        date: "2026-03-07",
        forMonth: "2026-03",
      }),
  },
  {
    name: "debts.addCustom",
    run: (caller, tenantId) =>
      caller.mutation(api.debts.addCustom, {
        tenantId,
        clientId: "c1",
        amountCents: 100,
        date: "2026-03-07",
        note: "Lock",
      }),
  },
];

async function outcomeOf(call: Promise<unknown>): Promise<Outcome> {
  try {
    await call;
    return "allowed";
  } catch (error) {
    if (error instanceof ConvexError && error.data.code === "PermissionDenied") {
      return "denied";
    }
    throw error;
  }
}

async function seedCaller(caller: Caller) {
  const t = await newBackend();
  const tenantId = await seedTenant(t);
  const { groupId } = await tenantRow(t, tenantId);
  const ownerId = await t.run((ctx) => insertAccount(ctx, owner));
  const samId = await t.run((ctx) => insertAccount(ctx, sam));
  if (caller.membership === "owner") {
    return { t, tenantId, signedIn: t, own: ownerId, other: samId };
  }
  const membership = caller.membership;
  await t.run((ctx) => insertMembership(ctx, groupId, samId, membership));
  return { t, tenantId, signedIn: t.withIdentity(sam), own: samId, other: ownerId };
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["Date"], now: MARCH_20 });
});

afterEach(() => {
  vi.useRealTimers();
});

describe("entry permissions", () => {
  const cases = entryFunctions.flatMap((entryFunction) =>
    callers.flatMap((caller) => [
      { entryFunction, caller, whose: "their own entry", expected: caller.changesOwn },
      { entryFunction, caller, whose: "someone else's entry", expected: caller.changesOthers },
    ]),
  );

  it.each(cases)(
    "$entryFunction.name for $caller.level on $whose is $expected",
    async ({ entryFunction, caller, whose, expected }) => {
      const { t, tenantId, signedIn, own, other } = await seedCaller(caller);
      const addedBy = whose === "their own entry" ? own : other;
      const id = await t.run((ctx) => entryFunction.seed(ctx, tenantId, addedBy));

      expect(await outcomeOf(entryFunction.run(signedIn, id))).toBe(expected);
    },
  );

  it("explains the denial for someone else's entry", async () => {
    const caller = callers[1] as Caller;
    const { t, tenantId, signedIn, other } = await seedCaller(caller);
    const entryFunction = entryFunctions[0] as EntryFunction;
    const id = await t.run((ctx) => entryFunction.seed(ctx, tenantId, other));

    const failure = await failureOf(entryFunction.run(signedIn, id));

    expect(failure).toEqual({
      code: "PermissionDenied",
      message: "You can only change entries you added.",
      details: {},
    });
  });

  it.each(
    creations.flatMap((creation) =>
      callers.map((caller) => ({ creation, caller, expected: caller.level === "read" ? "denied" : "allowed" })),
    ),
  )("$creation.name for $caller.level is $expected", async ({ creation, caller, expected }) => {
    const { tenantId, signedIn } = await seedCaller(caller);

    expect(await outcomeOf(creation.run(signedIn, tenantId))).toBe(expected);
  });
});
