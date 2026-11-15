import { ConvexError } from "convex/values";
import { describe, expect, it } from "vitest";
import type { AppErrorData } from "./appError";
import {
  isAtOrBelow,
  OWNER_PERMISSIONS,
  PERMISSION_PRESETS,
  type PermissionHolder,
  type PermissionSet,
  raisedLevelsWithin,
  requireMayChangePermissions,
  requireMayInvite,
  requireMayRemove,
} from "./permissionRules";

const { viewer, bookkeeper, manager } = PERMISSION_PRESETS;

function member(permissions: PermissionSet): PermissionHolder {
  return { role: "member", ...permissions };
}

const owner: PermissionHolder = { role: "owner", ...viewer };

function failureOf(check: () => void): AppErrorData {
  try {
    check();
  } catch (error) {
    if (error instanceof ConvexError) {
      return error.data as AppErrorData;
    }
    throw error;
  }
  throw new Error("Expected the check to fail");
}

describe("PERMISSION_PRESETS", () => {
  it("defines Viewer, Bookkeeper and Manager as the product spec lists them", () => {
    expect(PERMISSION_PRESETS).toEqual({
      viewer: {
        tenants: "read",
        rentTerms: "read",
        payments: "read",
        debts: "read",
        members: "read",
        settings: "read",
        editOthers: false,
      },
      bookkeeper: { ...viewer, payments: "addEdit", debts: "addEdit" },
      manager: {
        ...viewer,
        tenants: "addEdit",
        rentTerms: "addEdit",
        payments: "addEdit",
        debts: "addEdit",
        editOthers: true,
      },
    });
  });
});

const fullEntries: PermissionSet = { ...PERMISSION_PRESETS.viewer, payments: "full", debts: "full" };

describe("isAtOrBelow", () => {
  it.each<[string, PermissionSet, PermissionSet, boolean]>([
    ["equal sets", manager, manager, true],
    ["viewer under bookkeeper", viewer, bookkeeper, true],
    ["bookkeeper under manager", bookkeeper, manager, true],
    ["manager over bookkeeper (switch and tenants)", manager, bookkeeper, false],
    ["anything under owner", manager, OWNER_PERMISSIONS, true],
    ["one higher category", { ...viewer, settings: "full" }, manager, false],
    ["the switch on against off", { ...viewer, editOthers: true }, bookkeeper, false],
    ["full against addEdit", { ...viewer, payments: "full" }, bookkeeper, false],
    ["the switch on under full payments and debts", { ...viewer, editOthers: true }, fullEntries, true],
  ])("%s", (_case, lower, upper, expected) => {
    expect(isAtOrBelow(lower, upper)).toBe(expected);
  });
});

describe("raisedLevelsWithin", () => {
  it.each<[string, PermissionSet, PermissionSet, PermissionSet, boolean]>([
    ["no change above the ceiling", { ...viewer, tenants: "full" }, { ...viewer, tenants: "full" }, viewer, true],
    [
      "lowering a level above the ceiling",
      { ...viewer, tenants: "full" },
      { ...viewer, tenants: "addEdit" },
      viewer,
      true,
    ],
    ["raising to the ceiling", viewer, bookkeeper, bookkeeper, true],
    ["raising above the ceiling", viewer, { ...viewer, payments: "full" }, bookkeeper, false],
    ["turning the switch on without it", viewer, { ...viewer, editOthers: true }, bookkeeper, false],
    ["turning the switch on with it", viewer, { ...viewer, editOthers: true }, manager, true],
    ["turning the switch on with full payments and debts", viewer, { ...viewer, editOthers: true }, fullEntries, true],
    [
      "turning the switch on with full payments only",
      viewer,
      { ...viewer, editOthers: true },
      { ...viewer, payments: "full" },
      false,
    ],
    ["keeping the switch on without it", manager, { ...manager, tenants: "read" }, bookkeeper, true],
    ["turning the switch off", manager, { ...manager, editOthers: false }, viewer, true],
    ["raising settings to full without it", viewer, { ...viewer, settings: "full" }, manager, false],
  ])("%s", (_case, current, proposed, ceiling, expected) => {
    expect(raisedLevelsWithin(current, proposed, ceiling)).toBe(expected);
  });
});

describe("requireMayChangePermissions", () => {
  it("lets the owner grant everything", () => {
    expect(() => requireMayChangePermissions(owner, viewer, OWNER_PERMISSIONS)).not.toThrow();
  });

  it("rejects a raise above the editor's own level", () => {
    const editor = member({ ...bookkeeper, members: "full" });

    expect(failureOf(() => requireMayChangePermissions(editor, viewer, manager))).toMatchObject({
      code: "PermissionDenied",
    });
  });
});

const inviterWithBookkeeping = member({ ...bookkeeper, members: "addEdit" });
const removerWithAddEdit = member({ ...bookkeeper, members: "addEdit" });

describe("requireMayInvite", () => {
  it.each<[string, PermissionHolder, PermissionSet]>([
    ["the owner invites a manager", owner, manager],
    ["an inviter invites a viewer", inviterWithBookkeeping, viewer],
    ["an inviter invites someone with their own levels", inviterWithBookkeeping, { ...bookkeeper, members: "addEdit" }],
  ])("allows %s", (_case, inviter, invited) => {
    expect(() => requireMayInvite(inviter, invited)).not.toThrow();
  });

  it.each<[string, PermissionHolder, PermissionSet]>([
    ["an inviter to invite a manager", inviterWithBookkeeping, manager],
    ["an inviter to grant more member rights", inviterWithBookkeeping, { ...bookkeeper, members: "full" }],
  ])("denies %s", (_case, inviter, invited) => {
    expect(failureOf(() => requireMayInvite(inviter, invited))).toMatchObject({ code: "PermissionDenied" });
  });
});

describe("requireMayRemove", () => {
  it.each<[string, PermissionHolder, PermissionHolder]>([
    ["the owner to remove a manager", owner, member(manager)],
    ["members full to remove a higher member", member({ ...viewer, members: "full" }), member(manager)],
    ["members add & edit to remove a lower member", removerWithAddEdit, member(viewer)],
    ["members add & edit to remove an equal member", removerWithAddEdit, removerWithAddEdit],
  ])("allows %s", (_case, remover, target) => {
    expect(() => requireMayRemove(remover, target)).not.toThrow();
  });

  it.each<[string, PermissionHolder, PermissionHolder]>([
    ["members add & edit to remove a higher member", removerWithAddEdit, member(manager)],
    ["members read to remove a viewer", member(manager), member(viewer)],
    ["members full to remove the owner", member({ ...manager, members: "full" }), owner],
    ["the owner to remove the owner", owner, owner],
  ])("denies %s", (_case, remover, target) => {
    expect(failureOf(() => requireMayRemove(remover, target))).toMatchObject({ code: "PermissionDenied" });
  });
});
