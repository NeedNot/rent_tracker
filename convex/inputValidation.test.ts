import { ConvexError } from "convex/values";
import { describe, expect, it } from "vitest";
import type { AppErrorData } from "./appError";
import {
  optionalAmountCents,
  optionalCalendarDate,
  optionalGraceDays,
  optionalLabel,
  optionalNonNegativeCents,
  optionalNotes,
  requireAmountCents,
  requireCalendarDate,
  requireCalendarMonth,
  requireDueDay,
  requireGraceDays,
  requireName,
  requireTimeZone,
} from "./inputValidation";

function failureOf(validate: () => unknown): AppErrorData {
  try {
    validate();
  } catch (error) {
    if (error instanceof ConvexError) {
      return error.data as AppErrorData;
    }
    throw error;
  }
  throw new Error("Expected validation to fail");
}

function expectValidationFailed(validate: () => unknown, message: string) {
  expect(failureOf(validate)).toMatchObject({ code: "ValidationFailed", message });
}

describe("requireAmountCents", () => {
  it.each([1, 150000, 100_000_000])("accepts %d cents", (cents) => {
    expect(requireAmountCents(cents, "Rent")).toBe(cents);
  });

  it.each([0, -1, 100_000_001, 1500.5, Number.NaN, Number.POSITIVE_INFINITY, Number.MAX_SAFE_INTEGER + 1])(
    "rejects %d cents",
    (cents) => {
      expectValidationFailed(() => requireAmountCents(cents, "Rent"), "Rent must be between $0.01 and $1,000,000.00.");
    },
  );

  it("names the field in the error details", () => {
    expect(failureOf(() => requireAmountCents(0, "Rent")).details).toEqual({ field: "Rent" });
  });
});

describe("optionalAmountCents", () => {
  it("passes through a missing amount", () => {
    expect(optionalAmountCents(undefined, "Late fee")).toBeUndefined();
  });

  it("accepts an amount inside the limits", () => {
    expect(optionalAmountCents(5000, "Late fee")).toBe(5000);
  });

  it("rejects zero, because a set amount must be positive", () => {
    expectValidationFailed(
      () => optionalAmountCents(0, "Late fee"),
      "Late fee must be between $0.01 and $1,000,000.00.",
    );
  });
});

describe("optionalNonNegativeCents", () => {
  it.each([undefined, 0, 1, 100_000_000])("accepts %s", (cents) => {
    expect(optionalNonNegativeCents(cents, "Security deposit")).toBe(cents);
  });

  it.each([-1, 100_000_001, 0.5, Number.NaN])("rejects %d cents", (cents) => {
    expectValidationFailed(
      () => optionalNonNegativeCents(cents, "Security deposit"),
      "Security deposit must be between $0.00 and $1,000,000.00.",
    );
  });
});

describe("grace days", () => {
  it.each([0, 5, 15])("accepts %d days", (days) => {
    expect(requireGraceDays(days)).toBe(days);
  });

  it.each([-1, 16, 2.5, Number.NaN, Number.POSITIVE_INFINITY])("rejects %d days", (days) => {
    expectValidationFailed(() => requireGraceDays(days), "Grace period must be 0 to 15 whole days.");
  });

  it("passes through a missing override and checks a present one", () => {
    expect(optionalGraceDays(undefined)).toBeUndefined();
    expect(optionalGraceDays(0)).toBe(0);
    expectValidationFailed(() => optionalGraceDays(-1), "Grace period must be 0 to 15 whole days.");
  });
});

describe("requireDueDay", () => {
  it.each([1, 15, 31])("accepts day %d", (day) => {
    expect(requireDueDay(day)).toBe(day);
  });

  it.each([0, 32, -1, 1.5, Number.NaN])("rejects day %d", (day) => {
    expectValidationFailed(() => requireDueDay(day), "Due day must be between 1 and 31.");
  });
});

describe("requireCalendarDate", () => {
  it.each(["2026-01-01", "2026-12-31", "2028-02-29"])("accepts %s", (date) => {
    expect(requireCalendarDate(date, "Move-in date")).toBe(date);
  });

  it.each([
    "2026-02-30",
    "2026-02-29",
    "2026-13-01",
    "2026-00-10",
    "2026-04-31",
    "2026-1-1",
    "26-01-01",
    "2026-01-01T00:00:00Z",
    " 2026-01-01",
    "2026-01-01\n",
    "2026-01",
    "",
  ])("rejects %j", (date) => {
    expectValidationFailed(() => requireCalendarDate(date, "Move-in date"), "Move-in date must be a real date.");
  });

  it("passes through a missing optional date and checks a present one", () => {
    expect(optionalCalendarDate(undefined, "Move-out date")).toBeUndefined();
    expect(optionalCalendarDate("2026-06-30", "Move-out date")).toBe("2026-06-30");
    expectValidationFailed(
      () => optionalCalendarDate("2026-06-31", "Move-out date"),
      "Move-out date must be a real date.",
    );
  });
});

describe("requireCalendarMonth", () => {
  it.each(["2026-01", "2026-12"])("accepts %s", (month) => {
    expect(requireCalendarMonth(month, "Start month")).toBe(month);
  });

  it.each(["2026-00", "2026-13", "2026-1", "2026-01-01", "202601", ""])("rejects %j", (month) => {
    expectValidationFailed(() => requireCalendarMonth(month, "Start month"), "Start month must be a real month.");
  });
});

describe("requireName", () => {
  it("returns the name trimmed", () => {
    expect(requireName("  Apt 3B ", "Name")).toBe("Apt 3B");
  });

  it("accepts 100 characters after trimming", () => {
    expect(requireName(` ${"a".repeat(100)} `, "Name")).toBe("a".repeat(100));
  });

  it.each(["", "   ", "\n\t"])("rejects blank name %j", (name) => {
    expectValidationFailed(() => requireName(name, "Name"), "Name is required.");
  });

  it("rejects 101 characters", () => {
    expectValidationFailed(() => requireName("a".repeat(101), "Name"), "Name must be 100 characters or fewer.");
  });
});

describe("optionalLabel", () => {
  it("returns the label trimmed", () => {
    expect(optionalLabel(" Sam ", "Contact name")).toBe("Sam");
  });

  it.each([undefined, "", "  "])("treats %j as no label", (label) => {
    expect(optionalLabel(label, "Contact name")).toBeUndefined();
  });

  it("accepts 100 characters and rejects 101", () => {
    expect(optionalLabel("a".repeat(100), "Contact name")).toBe("a".repeat(100));
    expectValidationFailed(
      () => optionalLabel("a".repeat(101), "Contact name"),
      "Contact name must be 100 characters or fewer.",
    );
  });
});

describe("optionalNotes", () => {
  it("returns the notes trimmed, keeping inner line breaks", () => {
    expect(optionalNotes(" Pays by check.\nHas a dog. ")).toBe("Pays by check.\nHas a dog.");
  });

  it.each([undefined, "", " \n "])("treats %j as no notes", (notes) => {
    expect(optionalNotes(notes)).toBeUndefined();
  });

  it("accepts 2,000 characters and rejects 2,001", () => {
    expect(optionalNotes("a".repeat(2000))).toBe("a".repeat(2000));
    expectValidationFailed(() => optionalNotes("a".repeat(2001)), "Notes must be 2,000 characters or fewer.");
  });
});

describe("requireTimeZone", () => {
  it.each(["America/Los_Angeles", "Europe/London", "Asia/Kolkata", "Pacific/Kiritimati", "UTC"])(
    "accepts %s",
    (timeZone) => {
      expect(requireTimeZone(timeZone)).toBe(timeZone);
    },
  );

  it.each(["", "Mars/Olympus_Mons", "America/Los Angeles", "Pacific Time"])("rejects %j", (timeZone) => {
    expectValidationFailed(() => requireTimeZone(timeZone), "Time zone is not recognized.");
  });
});
