import type { Infer } from "convex/values";
import type { Doc } from "./_generated/dataModel";
import type { PermissionLevel } from "./access";
import { appError } from "./appError";
import type { permissionSet } from "./validators";

export type PermissionSet = Infer<typeof permissionSet>;
export type PermissionHolder = Pick<Doc<"memberships">, "role"> & PermissionSet;
export type PresetName = "viewer" | "bookkeeper" | "manager";

const LEVEL_CATEGORIES = ["tenants", "rentTerms", "payments", "debts", "members", "settings"] as const;

const LEVEL_RANK: Record<PermissionLevel, number> = { read: 0, addEdit: 1, full: 2 };

export const OWNER_PERMISSIONS: PermissionSet = {
  tenants: "full",
  rentTerms: "full",
  payments: "full",
  debts: "full",
  members: "full",
  settings: "full",
  editOthers: true,
};

export const PERMISSION_PRESETS: Record<PresetName, PermissionSet> = {
  viewer: {
    tenants: "read",
    rentTerms: "read",
    payments: "read",
    debts: "read",
    members: "read",
    settings: "read",
    editOthers: false,
  },
  bookkeeper: {
    tenants: "read",
    rentTerms: "read",
    payments: "addEdit",
    debts: "addEdit",
    members: "read",
    settings: "read",
    editOthers: false,
  },
  manager: {
    tenants: "addEdit",
    rentTerms: "addEdit",
    payments: "addEdit",
    debts: "addEdit",
    members: "read",
    settings: "read",
    editOthers: true,
  },
};

export function permissionsOf(holder: PermissionHolder): PermissionSet {
  if (holder.role === "owner") {
    return OWNER_PERMISSIONS;
  }
  const { tenants, rentTerms, payments, debts, members, settings, editOthers } = holder;
  return { tenants, rentTerms, payments, debts, members, settings, editOthers };
}

export function isAtOrBelow(lower: PermissionSet, upper: PermissionSet): boolean {
  return (
    LEVEL_CATEGORIES.every((category) => LEVEL_RANK[lower[category]] <= LEVEL_RANK[upper[category]]) &&
    (!lower.editOthers || holdsEditOthers(upper))
  );
}

export function raisedLevelsWithin(current: PermissionSet, proposed: PermissionSet, ceiling: PermissionSet): boolean {
  const categoriesWithin = LEVEL_CATEGORIES.every(
    (category) =>
      LEVEL_RANK[proposed[category]] <= LEVEL_RANK[current[category]] ||
      LEVEL_RANK[proposed[category]] <= LEVEL_RANK[ceiling[category]],
  );
  const switchWithin = !proposed.editOthers || current.editOthers || holdsEditOthers(ceiling);
  return categoriesWithin && switchWithin;
}

function holdsEditOthers(permissions: PermissionSet): boolean {
  return permissions.editOthers || (permissions.payments === "full" && permissions.debts === "full");
}

export function requireMayChangePermissions(
  editor: PermissionHolder,
  current: PermissionSet,
  proposed: PermissionSet,
): void {
  if (!raisedLevelsWithin(current, proposed, permissionsOf(editor))) {
    throw appError("PermissionDenied", "You can only grant permissions you hold yourself.");
  }
}

export function requireMayInvite(inviter: PermissionHolder, invited: PermissionSet): void {
  if (!isAtOrBelow(invited, permissionsOf(inviter))) {
    throw appError("PermissionDenied", "You can only grant permissions you hold yourself.");
  }
}

export function requireMayRemove(remover: PermissionHolder, target: PermissionHolder): void {
  if (target.role === "owner") {
    throw appError("PermissionDenied", "Nobody can remove the group's owner.");
  }
  const removerPermissions = permissionsOf(remover);
  if (removerPermissions.members === "full") {
    return;
  }
  if (removerPermissions.members === "addEdit" && isAtOrBelow(permissionsOf(target), removerPermissions)) {
    return;
  }
  throw appError("PermissionDenied", "You can only remove members whose permissions are all at or below your own.");
}
