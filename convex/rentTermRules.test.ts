import { describe, expect, it } from "vitest";
import { rentTermLevel } from "./rentTermRules";

describe("rentTermLevel", () => {
  it.each([
    ["a term starting in a past month needs Full control", ["2026-02"], "full"],
    ["a term starting this month needs Add & edit", ["2026-03"], "addEdit"],
    ["a term starting in a future month needs Add & edit", ["2026-04"], "addEdit"],
    ["moving a started term to a future month needs Full control", ["2026-02", "2026-06"], "full"],
    ["moving a future term into the past needs Full control", ["2026-06", "2025-12"], "full"],
    ["moving a future term to another future month needs Add & edit", ["2026-06", "2026-07"], "addEdit"],
  ] as const)("%s", (_rule, startMonths, level) => {
    expect(rentTermLevel(startMonths, "2026-03")).toBe(level);
  });
});
