import { ConvexError } from "convex/values";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import type { PermissionCategory, PermissionLevel } from "./access";
import type { AppErrorCode } from "./appError";
import type { EntryCategory } from "./entryAccess";
import { INVITE_LIFETIME_MS } from "./invites";
import {
  buildScene,
  callersFor,
  expectedOutcome,
  type MatrixRow,
  type Outcome,
  type Requirement,
  type Scene,
} from "./permissionMatrix.fixture";
import { PERMISSION_PRESETS } from "./permissionRules";
import { insertDebt, insertPayment, insertRentTerm, losAngelesTime, newTenant } from "./seed.fixture";
import { modules } from "./test.setup";

interface EntryAction {
  action: string;
  category: EntryCategory;
  seed: (scene: Scene, addedBy: Id<"accounts">) => Promise<string>;
  act: (scene: Scene, entryId: string) => Promise<unknown>;
}

const NOW = losAngelesTime(2026, 11, 14);
const THIS_MONTH = "2026-11";
const NEXT_MONTH = "2026-12";
const BACK_DATED_MONTH = "2026-06";
const RENT = { amountCents: 160000, dueDay: 1 };
const payment = { amountCents: 50000, dateReceived: "2026-11-02" };
const customDebt = { kind: "custom" as const, amountCents: 5000, date: "2026-11-07" };
const cancelled = { deletedAt: 1 };
const { viewer, bookkeeper } = PERMISSION_PRESETS;

const OUTCOME_OF_CODE: Partial<Record<AppErrorCode, Outcome>> = {
  PermissionDenied: "deny",
  NotFound: "hidden",
  ValidationFailed: "invalid",
};

const FUNCTIONS_SCOPED_TO_THE_CALLER_NOT_A_GROUP = [
  "accounts.ensure",
  "accounts.setDisplayName",
  "accounts.me",
  "accounts.requestDeletion",
  "groups.create",
  "home.screen",
  "invites.accept",
  "invites.decline",
  "notificationSettings.list",
  "pushTokens.register",
  "pushTokens.unregister",
];

const anyMember = { kind: "anyMember" } as const;
const ownerOnly = { kind: "owner" } as const;
const nobody = { kind: "nobody" } as const;

function needs(category: PermissionCategory, level: PermissionLevel): Requirement {
  return { kind: "levels", levels: { [category]: level } };
}

function ownAndOthersEntry({ action, category, seed, act }: EntryAction): MatrixRow[] {
  return [
    {
      action,
      variant: "own entry",
      categories: [category],
      requirement: needs(category, "addEdit"),
      attempt: async (scene) => act(scene, await seed(scene, scene.callerId)),
    },
    {
      action,
      variant: "others' entry",
      categories: [category],
      requirement: { kind: "othersEntry", category },
      attempt: async (scene) => act(scene, await seed(scene, scene.lee.accountId)),
    },
  ];
}

const seedPayment = (scene: Scene, loggedBy: Id<"accounts">) =>
  scene.t.run((ctx) => insertPayment(ctx, scene.tenantId, payment, { loggedBy }));

const seedCancelledPayment = (scene: Scene, loggedBy: Id<"accounts">) =>
  scene.t.run((ctx) => insertPayment(ctx, scene.tenantId, payment, { loggedBy, ...cancelled }));

const seedDebt = (scene: Scene, createdBy: Id<"accounts">) =>
  scene.t.run((ctx) => insertDebt(ctx, scene.tenantId, customDebt, { createdBy }));

const seedCancelledDebt = (scene: Scene, createdBy: Id<"accounts">) =>
  scene.t.run((ctx) => insertDebt(ctx, scene.tenantId, customDebt, { createdBy, ...cancelled }));

const seedTerm = (scene: Scene, startMonth: string) =>
  scene.t.run((ctx) => insertRentTerm(ctx, scene.tenantId, { startMonth, ...RENT }));

const archiveTenant = (scene: Scene) => scene.t.run((ctx) => ctx.db.patch(scene.tenantId, { status: "archived" }));

const setLee = (scene: Scene, permissions: Partial<typeof viewer>) =>
  scene.t.run((ctx) => ctx.db.patch(scene.lee.membershipId, permissions));

const ownerMembershipId = (scene: Scene) =>
  scene.t.run(async (ctx) => {
    const membership = await ctx.db
      .query("memberships")
      .withIndex("by_group_account", (q) => q.eq("groupId", scene.groupId).eq("accountId", scene.ownerId))
      .unique();
    if (membership === null) {
      throw new Error("The owner's membership is missing");
    }
    return membership._id;
  });

const groupRows: MatrixRow[] = [
  {
    action: "groups.updateSettings",
    categories: ["settings"],
    requirement: needs("settings", "full"),
    attempt: (scene) =>
      scene.caller.mutation(api.groups.updateSettings, {
        groupId: scene.groupId,
        expectedVersion: 1,
        name: "1st Street East",
        timeZone: "America/Los_Angeles",
        paymentOrder: "byDate",
        graceDays: 3,
      }),
  },
  {
    action: "groups.archive",
    categories: ["settings"],
    requirement: ownerOnly,
    attempt: (scene) => scene.caller.mutation(api.groups.archive, { groupId: scene.groupId }),
  },
  {
    action: "groups.unarchive",
    categories: ["settings"],
    requirement: ownerOnly,
    archivedGroup: true,
    attempt: (scene) => scene.caller.mutation(api.groups.unarchive, { groupId: scene.groupId }),
  },
  {
    action: "groups.screen",
    categories: ["settings"],
    requirement: anyMember,
    attempt: (scene) => scene.caller.query(api.groups.screen, { groupId: scene.groupId, asOf: NOW }),
  },
];

const tenantRows: MatrixRow[] = [
  {
    action: "tenants.add",
    categories: ["tenants"],
    requirement: needs("tenants", "addEdit"),
    attempt: (scene) =>
      scene.caller.mutation(api.tenants.add, { groupId: scene.groupId, ...newTenant, name: "Apt 4C" }),
  },
  {
    action: "tenants.edit",
    categories: ["tenants"],
    requirement: needs("tenants", "addEdit"),
    attempt: (scene) =>
      scene.caller.mutation(api.tenants.edit, {
        tenantId: scene.tenantId,
        expectedVersion: 1,
        name: "Apt 3B (upstairs)",
        moveIn: newTenant.moveIn,
      }),
  },
  {
    action: "tenants.archive",
    categories: ["tenants"],
    requirement: needs("tenants", "full"),
    attempt: (scene) => scene.caller.mutation(api.tenants.archive, { tenantId: scene.tenantId, expectedVersion: 1 }),
  },
  {
    action: "tenants.restore",
    categories: ["tenants"],
    requirement: needs("tenants", "full"),
    attempt: async (scene) => {
      await archiveTenant(scene);
      return scene.caller.mutation(api.tenants.restore, { tenantId: scene.tenantId, expectedVersion: 1 });
    },
  },
  {
    action: "tenants.move",
    variant: "caller has Tenants full in the target group",
    categories: ["tenants"],
    requirement: { kind: "levelsInBothGroups", levels: { tenants: "full" } },
    callerInTargetGroup: { tenants: "full" },
    attempt: (scene) =>
      scene.caller.mutation(api.tenants.move, {
        tenantId: scene.tenantId,
        expectedVersion: 1,
        toGroupId: scene.targetGroupId,
      }),
  },
  {
    action: "tenants.move",
    variant: "caller has Tenants add & edit in the target group",
    categories: ["tenants"],
    requirement: { kind: "levelsInBothGroups", levels: { tenants: "full" } },
    callerInTargetGroup: { tenants: "addEdit" },
    attempt: (scene) =>
      scene.caller.mutation(api.tenants.move, {
        tenantId: scene.tenantId,
        expectedVersion: 1,
        toGroupId: scene.targetGroupId,
      }),
  },
  {
    action: "tenants.moveTargets",
    categories: ["tenants"],
    requirement: needs("tenants", "full"),
    attempt: (scene) => scene.caller.query(api.tenants.moveTargets, { tenantId: scene.tenantId, asOf: NOW }),
  },
  {
    action: "tenants.screen",
    categories: ["tenants"],
    requirement: anyMember,
    attempt: (scene) => scene.caller.query(api.tenants.screen, { tenantId: scene.tenantId, asOf: NOW }),
  },
  {
    action: "tenants.calendar",
    categories: ["tenants"],
    requirement: anyMember,
    attempt: (scene) => scene.caller.query(api.tenants.calendar, { tenantId: scene.tenantId, year: 2026, asOf: NOW }),
  },
];

const rentTermRows: MatrixRow[] = [
  {
    action: "rentTerms.add",
    variant: "starting this month",
    categories: ["rentTerms"],
    requirement: needs("rentTerms", "addEdit"),
    attempt: (scene) =>
      scene.caller.mutation(api.rentTerms.add, { tenantId: scene.tenantId, startMonth: THIS_MONTH, ...RENT }),
  },
  {
    action: "rentTerms.add",
    variant: "starting before this month",
    categories: ["rentTerms"],
    requirement: needs("rentTerms", "full"),
    attempt: (scene) =>
      scene.caller.mutation(api.rentTerms.add, { tenantId: scene.tenantId, startMonth: BACK_DATED_MONTH, ...RENT }),
  },
  {
    action: "rentTerms.edit",
    variant: "a term starting next month",
    categories: ["rentTerms"],
    requirement: needs("rentTerms", "addEdit"),
    attempt: async (scene) =>
      scene.caller.mutation(api.rentTerms.edit, {
        rentTermId: await seedTerm(scene, NEXT_MONTH),
        expectedVersion: 1,
        startMonth: NEXT_MONTH,
        amountCents: 170000,
        dueDay: 1,
      }),
  },
  {
    action: "rentTerms.edit",
    variant: "a term that started before this month",
    categories: ["rentTerms"],
    requirement: needs("rentTerms", "full"),
    attempt: async (scene) =>
      scene.caller.mutation(api.rentTerms.edit, {
        rentTermId: await seedTerm(scene, BACK_DATED_MONTH),
        expectedVersion: 1,
        startMonth: BACK_DATED_MONTH,
        amountCents: 170000,
        dueDay: 1,
      }),
  },
  {
    action: "rentTerms.edit",
    variant: "moving a term to before this month",
    categories: ["rentTerms"],
    requirement: needs("rentTerms", "full"),
    attempt: async (scene) =>
      scene.caller.mutation(api.rentTerms.edit, {
        rentTermId: await seedTerm(scene, NEXT_MONTH),
        expectedVersion: 1,
        startMonth: BACK_DATED_MONTH,
        ...RENT,
      }),
  },
  {
    action: "rentTerms.delete",
    variant: "a term starting next month",
    categories: ["rentTerms"],
    requirement: needs("rentTerms", "addEdit"),
    attempt: async (scene) =>
      scene.caller.mutation(api.rentTerms.delete, {
        rentTermId: await seedTerm(scene, NEXT_MONTH),
        expectedVersion: 1,
        reason: "Lease renewed at the old rent",
      }),
  },
  {
    action: "rentTerms.delete",
    variant: "a term that started before this month",
    categories: ["rentTerms"],
    requirement: needs("rentTerms", "full"),
    attempt: async (scene) =>
      scene.caller.mutation(api.rentTerms.delete, {
        rentTermId: await seedTerm(scene, BACK_DATED_MONTH),
        expectedVersion: 1,
        reason: "Entered by mistake",
      }),
  },
  {
    action: "rentTerms.preview",
    categories: ["rentTerms"],
    requirement: anyMember,
    attempt: (scene) =>
      scene.caller.query(api.rentTerms.preview, {
        tenantId: scene.tenantId,
        asOf: NOW,
        proposed: { startMonth: NEXT_MONTH, ...RENT },
      }),
  },
];

const paymentRows: MatrixRow[] = [
  {
    action: "payments.log",
    categories: ["payments"],
    requirement: needs("payments", "addEdit"),
    attempt: (scene) =>
      scene.caller.mutation(api.payments.log, {
        tenantId: scene.tenantId,
        clientId: "c1",
        ...payment,
        confirmed: true,
      }),
  },
  {
    action: "payments.log",
    variant: "with a late fee",
    categories: ["payments", "debts"],
    requirement: { kind: "levels", levels: { payments: "addEdit", debts: "addEdit" } },
    attempt: (scene) =>
      scene.caller.mutation(api.payments.log, {
        tenantId: scene.tenantId,
        clientId: "c1",
        ...payment,
        lateFee: { amountCents: 5000, forMonth: THIS_MONTH },
        confirmed: true,
      }),
  },
  ...ownAndOthersEntry({
    action: "payments.edit",
    category: "payments",
    seed: seedPayment,
    act: (scene, id) =>
      scene.caller.mutation(api.payments.edit, {
        paymentId: id as Id<"payments">,
        expectedVersion: 1,
        amountCents: 40000,
        dateReceived: payment.dateReceived,
      }),
  }),
  ...ownAndOthersEntry({
    action: "payments.delete",
    category: "payments",
    seed: seedPayment,
    act: (scene, id) =>
      scene.caller.mutation(api.payments.delete, {
        paymentId: id as Id<"payments">,
        expectedVersion: 1,
        reason: "Logged twice",
      }),
  }),
  ...ownAndOthersEntry({
    action: "payments.restore",
    category: "payments",
    seed: seedCancelledPayment,
    act: (scene, id) =>
      scene.caller.mutation(api.payments.restore, { paymentId: id as Id<"payments">, expectedVersion: 1 }),
  }),
  ...ownAndOthersEntry({
    action: "payments.markBounced",
    category: "payments",
    seed: seedPayment,
    act: (scene, id) =>
      scene.caller.mutation(api.payments.markBounced, { paymentId: id as Id<"payments">, expectedVersion: 1 }),
  }),
  {
    action: "payments.preview",
    categories: ["payments"],
    requirement: anyMember,
    attempt: (scene) =>
      scene.caller.query(api.payments.preview, { tenantId: scene.tenantId, asOf: NOW, proposed: payment }),
  },
];

const debtRows: MatrixRow[] = [
  {
    action: "debts.addLateFee",
    categories: ["debts"],
    requirement: needs("debts", "addEdit"),
    attempt: (scene) =>
      scene.caller.mutation(api.debts.addLateFee, {
        tenantId: scene.tenantId,
        clientId: "c1",
        amountCents: 5000,
        date: "2026-11-07",
        forMonth: THIS_MONTH,
        confirmed: true,
      }),
  },
  {
    action: "debts.addCustom",
    categories: ["debts"],
    requirement: needs("debts", "addEdit"),
    attempt: (scene) =>
      scene.caller.mutation(api.debts.addCustom, {
        tenantId: scene.tenantId,
        clientId: "c1",
        amountCents: 5000,
        date: "2026-11-07",
        note: "Lock change",
      }),
  },
  ...ownAndOthersEntry({
    action: "debts.edit",
    category: "debts",
    seed: seedDebt,
    act: (scene, id) =>
      scene.caller.mutation(api.debts.edit, {
        debtId: id as Id<"debts">,
        expectedVersion: 1,
        amountCents: 6000,
        date: customDebt.date,
        note: "Lock change",
      }),
  }),
  ...ownAndOthersEntry({
    action: "debts.delete",
    category: "debts",
    seed: seedDebt,
    act: (scene, id) =>
      scene.caller.mutation(api.debts.delete, { debtId: id as Id<"debts">, expectedVersion: 1, reason: "Waived" }),
  }),
  ...ownAndOthersEntry({
    action: "debts.restore",
    category: "debts",
    seed: seedCancelledDebt,
    act: (scene, id) => scene.caller.mutation(api.debts.restore, { debtId: id as Id<"debts">, expectedVersion: 1 }),
  }),
];

const memberRows: MatrixRow[] = [
  {
    action: "members.screen",
    categories: ["members"],
    requirement: anyMember,
    attempt: (scene) => scene.caller.query(api.members.screen, { groupId: scene.groupId, asOf: NOW }),
  },
  {
    action: "members.mine",
    categories: ["members"],
    requirement: anyMember,
    attempt: (scene) => scene.caller.query(api.members.mine, { groupId: scene.groupId }),
  },
  {
    action: "members.updatePermissions",
    variant: "lowering a member's levels",
    categories: ["members"],
    requirement: needs("members", "full"),
    attempt: async (scene) => {
      await setLee(scene, bookkeeper);
      return scene.caller.mutation(api.members.updatePermissions, {
        membershipId: scene.lee.membershipId,
        expectedVersion: 1,
        permissions: viewer,
      });
    },
  },
  {
    action: "members.updatePermissions",
    variant: "turning on edit others' entries",
    categories: ["members"],
    requirement: { kind: "levels", levels: { members: "full" }, alsoEditOthers: true },
    attempt: (scene) =>
      scene.caller.mutation(api.members.updatePermissions, {
        membershipId: scene.lee.membershipId,
        expectedVersion: 1,
        permissions: { ...viewer, editOthers: true },
      }),
  },
  {
    action: "members.updatePermissions",
    variant: "the owner's membership",
    categories: ["members"],
    requirement: nobody,
    attempt: async (scene) =>
      scene.caller.mutation(api.members.updatePermissions, {
        membershipId: await ownerMembershipId(scene),
        expectedVersion: 1,
        permissions: viewer,
      }),
  },
  {
    action: "members.remove",
    variant: "a member at or below the caller",
    categories: ["members"],
    requirement: needs("members", "addEdit"),
    attempt: (scene) => scene.caller.mutation(api.members.remove, { membershipId: scene.lee.membershipId }),
  },
  {
    action: "members.remove",
    variant: "a member with a higher level than the caller",
    categories: ["members"],
    requirement: needs("members", "full"),
    attempt: async (scene) => {
      await setLee(scene, { tenants: "addEdit" });
      return scene.caller.mutation(api.members.remove, { membershipId: scene.lee.membershipId });
    },
  },
  {
    action: "members.remove",
    variant: "the owner",
    categories: ["members"],
    requirement: nobody,
    ownerTargetsOwnMembership: true,
    attempt: async (scene) =>
      scene.caller.mutation(api.members.remove, { membershipId: await ownerMembershipId(scene) }),
  },
  {
    action: "members.leave",
    categories: ["members"],
    requirement: anyMember,
    ownerTargetsOwnMembership: true,
    attempt: (scene) => scene.caller.mutation(api.members.leave, { groupId: scene.groupId }),
  },
  {
    action: "members.transferOwnership",
    categories: ["members"],
    requirement: ownerOnly,
    attempt: (scene) =>
      scene.caller.mutation(api.members.transferOwnership, {
        groupId: scene.groupId,
        toMembershipId: scene.lee.membershipId,
      }),
  },
];

const inviteRows: MatrixRow[] = [
  {
    action: "invites.send",
    variant: "granting Viewer",
    categories: ["members"],
    requirement: needs("members", "addEdit"),
    attempt: (scene) =>
      scene.caller.mutation(api.invites.send, {
        groupId: scene.groupId,
        email: "pat@example.com",
        permissions: viewer,
      }),
  },
  {
    action: "invites.send",
    variant: "granting Members full",
    categories: ["members"],
    requirement: needs("members", "full"),
    attempt: (scene) =>
      scene.caller.mutation(api.invites.send, {
        groupId: scene.groupId,
        email: "pat@example.com",
        permissions: { ...viewer, members: "full" },
      }),
  },
  {
    action: "invites.send",
    variant: "granting edit others' entries",
    categories: ["members"],
    requirement: { kind: "levels", levels: { members: "addEdit" }, alsoEditOthers: true },
    attempt: (scene) =>
      scene.caller.mutation(api.invites.send, {
        groupId: scene.groupId,
        email: "pat@example.com",
        permissions: { ...viewer, editOthers: true },
      }),
  },
  {
    action: "invites.revoke",
    categories: ["members"],
    requirement: needs("members", "addEdit"),
    attempt: async (scene) => {
      const inviteId = await scene.t.run((ctx) =>
        ctx.db.insert("invites", {
          groupId: scene.groupId,
          email: "pat@example.com",
          permissions: viewer,
          invitedBy: scene.ownerId,
          expiresAt: NOW + INVITE_LIFETIME_MS,
        }),
      );
      return scene.caller.mutation(api.invites.revoke, { inviteId });
    },
  },
];

const notificationRows: MatrixRow[] = [
  {
    action: "notificationSettings.set",
    categories: ["payments"],
    requirement: anyMember,
    attempt: (scene) =>
      scene.caller.mutation(api.notificationSettings.set, { groupId: scene.groupId, notificationsOn: false }),
  },
];

const matrix: MatrixRow[] = [
  ...groupRows,
  ...tenantRows,
  ...rentTermRows,
  ...paymentRows,
  ...debtRows,
  ...memberRows,
  ...inviteRows,
  ...notificationRows,
];

const cells = matrix.flatMap((row) =>
  callersFor(row).map((caller) => {
    const expected = expectedOutcome(row, caller);
    const action = row.variant === undefined ? row.action : `${row.action} (${row.variant})`;
    return { name: `${action} · ${caller.label} · ${expected}`, row, caller, expected };
  }),
);

async function outcomeOf(call: Promise<unknown>): Promise<Outcome> {
  try {
    await call;
    return "allow";
  } catch (error) {
    const outcome = error instanceof ConvexError ? OUTCOME_OF_CODE[error.data.code as AppErrorCode] : undefined;
    if (outcome === undefined) {
      throw error;
    }
    return outcome;
  }
}

async function publicFunctionNames(): Promise<string[]> {
  const names: string[] = [];
  for (const [path, load] of Object.entries(modules)) {
    if (path.includes("/_generated/")) {
      continue;
    }
    const moduleName = path.replace(/^\.\//, "").replace(/\.[jt]s$/, "");
    for (const [exportName, value] of Object.entries((await load()) as Record<string, unknown>)) {
      if ((value as { isPublic?: boolean } | undefined)?.isPublic === true) {
        names.push(`${moduleName}.${exportName}`);
      }
    }
  }
  return names.sort();
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(NOW);
});

afterEach(() => {
  vi.useRealTimers();
});

describe("permission matrix", () => {
  it.each(cells)("$name", async ({ row, caller, expected }) => {
    const scene = await buildScene(row, caller);

    expect(await outcomeOf(row.attempt(scene))).toBe(expected);
  });

  it("covers every public function that acts inside a group", async () => {
    const covered = new Set([...matrix.map((row) => row.action), ...FUNCTIONS_SCOPED_TO_THE_CALLER_NOT_A_GROUP]);

    const uncovered = (await publicFunctionNames()).filter((name) => !covered.has(name));

    expect(uncovered).toEqual([]);
  });

  it("names only functions that exist", async () => {
    const existing = new Set(await publicFunctionNames());
    const named = [...matrix.map((row) => row.action), ...FUNCTIONS_SCOPED_TO_THE_CALLER_NOT_A_GROUP];

    expect(named.filter((name) => !existing.has(name))).toEqual([]);
  });
});
