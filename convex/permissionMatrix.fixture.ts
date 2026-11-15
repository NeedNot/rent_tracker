import { api } from "./_generated/api";
import type { Id } from "./_generated/dataModel";
import type { PermissionCategory, PermissionLevel } from "./access";
import type { EntryCategory } from "./entryAccess";
import { PERMISSION_PRESETS, type PermissionSet } from "./permissionRules";
import {
  type Backend,
  insertAccount,
  insertMembership,
  newBackend,
  newTenant,
  owner,
  sam,
  seedGroup,
} from "./seed.fixture";

export type Requirement =
  | { kind: "anyMember" }
  | { kind: "owner" }
  | { kind: "nobody" }
  | { kind: "levels"; levels: Partial<Record<PermissionCategory, PermissionLevel>>; alsoEditOthers?: true }
  | { kind: "levelsInBothGroups"; levels: Partial<Record<PermissionCategory, PermissionLevel>> }
  | { kind: "othersEntry"; category: EntryCategory };

export interface MatrixRow {
  action: string;
  variant?: string;
  categories: PermissionCategory[];
  requirement: Requirement;
  archivedGroup?: true;
  ownerTargetsOwnMembership?: true;
  callerInTargetGroup?: Partial<PermissionSet>;
  attempt: (scene: Scene) => Promise<unknown>;
}

export type Caller =
  | { kind: "owner"; label: string }
  | { kind: "nonMember"; label: string }
  | { kind: "member"; label: string; permissions: PermissionSet };

export type Outcome = "allow" | "deny" | "hidden" | "invalid";

export interface Scene {
  t: Backend;
  caller: Backend;
  callerId: Id<"accounts">;
  ownerId: Id<"accounts">;
  groupId: Id<"groups">;
  tenantId: Id<"tenants">;
  targetGroupId: Id<"groups">;
  lee: { accountId: Id<"accounts">; membershipId: Id<"memberships"> };
}

const lee = { subject: "auth|lee", email: "lee@example.com", name: "Lee" };

const LEVEL_RANK: Record<PermissionLevel, number> = { read: 0, addEdit: 1, full: 2 };
const LEVELS: PermissionLevel[] = ["read", "addEdit", "full"];
const SETTINGS_LEVELS: PermissionLevel[] = ["read", "full"];

export function callersFor(row: MatrixRow): Caller[] {
  const levels = row.categories.includes("settings") ? SETTINGS_LEVELS : LEVELS;
  const switchSettings = editOthersMatters(row.requirement) ? [false, true] : [false];
  const members = levels.flatMap((level) =>
    switchSettings
      .filter((editOthers) => !editOthers || level !== "full")
      .map((editOthers) => memberCaller(row.categories, level, editOthers)),
  );
  return [{ kind: "owner", label: "owner" }, { kind: "nonMember", label: "non-member" }, ...members];
}

function editOthersMatters(requirement: Requirement): boolean {
  return requirement.kind === "othersEntry" || (requirement.kind === "levels" && requirement.alsoEditOthers === true);
}

function memberCaller(categories: PermissionCategory[], level: PermissionLevel, editOthers: boolean): Caller {
  const permissions: PermissionSet = { ...PERMISSION_PRESETS.viewer, editOthers };
  for (const category of categories) {
    Object.assign(permissions, { [category]: level });
  }
  const label = `${categories.join(", ")} ${level}${editOthers ? " + editOthers" : ""}`;
  return { kind: "member", label, permissions };
}

export function expectedOutcome(row: MatrixRow, caller: Caller): Outcome {
  if (caller.kind === "nonMember") {
    return "deny";
  }
  if (caller.kind === "owner" && row.ownerTargetsOwnMembership) {
    return "invalid";
  }
  if (caller.kind === "member" && row.archivedGroup) {
    return "hidden";
  }
  return meetsRequirement(row, caller) ? "allow" : "deny";
}

function meetsRequirement(row: MatrixRow, caller: Caller & { kind: "owner" | "member" }): boolean {
  const { requirement } = row;
  if (requirement.kind === "nobody") {
    return false;
  }
  if (caller.kind === "owner") {
    return true;
  }
  const held = caller.permissions;
  switch (requirement.kind) {
    case "anyMember":
      return true;
    case "owner":
      return false;
    case "levels":
      return holdsLevels(held, requirement.levels) && (!requirement.alsoEditOthers || held.editOthers);
    case "levelsInBothGroups":
      return (
        holdsLevels(held, requirement.levels) && holdsLevels(targetGroupPermissions(row, held), requirement.levels)
      );
    case "othersEntry":
      return held[requirement.category] === "full" || (held[requirement.category] === "addEdit" && held.editOthers);
  }
}

function holdsLevels(held: PermissionSet, levels: Partial<Record<PermissionCategory, PermissionLevel>>): boolean {
  return Object.entries(levels).every(
    ([category, level]) => LEVEL_RANK[held[category as PermissionCategory]] >= LEVEL_RANK[level],
  );
}

function targetGroupPermissions(row: MatrixRow, held: PermissionSet): PermissionSet {
  return { ...held, ...row.callerInTargetGroup };
}

export async function buildScene(row: MatrixRow, caller: Caller): Promise<Scene> {
  const t = await newBackend();
  const groupId = await seedGroup(t);
  const targetGroupId = await seedGroup(t, { name: "2nd Street" });
  const tenantId = await t.mutation(api.tenants.add, { groupId, ...newTenant });
  const ids = await t.run(async (ctx) => {
    const leeId = await insertAccount(ctx, lee);
    const leeMembershipId = await insertMembership(ctx, groupId, leeId, PERMISSION_PRESETS.viewer);
    return { ownerId: await insertAccount(ctx, owner), samId: await insertAccount(ctx, sam), leeId, leeMembershipId };
  });
  if (caller.kind === "member") {
    await t.run(async (ctx) => {
      await insertMembership(ctx, groupId, ids.samId, caller.permissions);
      if (row.callerInTargetGroup !== undefined) {
        await insertMembership(ctx, targetGroupId, ids.samId, targetGroupPermissions(row, caller.permissions));
      }
    });
  }
  if (row.archivedGroup) {
    await t.run((ctx) => ctx.db.patch(groupId, { archivedAt: Date.now() }));
  }
  const callerIsOwner = caller.kind === "owner";
  return {
    t,
    caller: callerIsOwner ? t : t.withIdentity(sam),
    callerId: callerIsOwner ? ids.ownerId : ids.samId,
    ownerId: ids.ownerId,
    groupId,
    tenantId,
    targetGroupId,
    lee: { accountId: ids.leeId, membershipId: ids.leeMembershipId },
  };
}
