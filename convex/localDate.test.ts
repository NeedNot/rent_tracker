import { ConvexError } from "convex/values";
import { describe, expect, it } from "vitest";
import { localDateAt } from "./localDate";

describe("localDateAt", () => {
  it.each([
    ["2026-11-12T07:59:59Z", "America/Los_Angeles", "2026-11-11"],
    ["2026-11-12T08:00:00Z", "America/Los_Angeles", "2026-11-12"],
    ["2026-11-12T08:00:00Z", "UTC", "2026-11-12"],
    ["2026-11-12T18:29:59Z", "Asia/Kolkata", "2026-11-12"],
    ["2026-11-12T18:30:00Z", "Asia/Kolkata", "2026-11-13"],
    ["2026-01-01T04:59:59Z", "America/New_York", "2025-12-31"],
    ["2028-02-29T12:00:00Z", "Europe/London", "2028-02-29"],
  ])("%s in %s is %s", (instant, timeZone, expected) => {
    expect(localDateAt(Date.parse(instant), timeZone)).toBe(expected);
  });

  it("follows the shorter offset once daylight saving time starts", () => {
    expect(localDateAt(Date.parse("2026-03-08T07:59:59Z"), "America/Los_Angeles")).toBe("2026-03-07");
    expect(localDateAt(Date.parse("2026-03-09T06:59:59Z"), "America/Los_Angeles")).toBe("2026-03-08");
    expect(localDateAt(Date.parse("2026-03-09T07:00:00Z"), "America/Los_Angeles")).toBe("2026-03-09");
  });

  it("follows the longer offset once daylight saving time ends", () => {
    expect(localDateAt(Date.parse("2026-11-01T06:59:59Z"), "America/Los_Angeles")).toBe("2026-10-31");
    expect(localDateAt(Date.parse("2026-11-02T07:59:59Z"), "America/Los_Angeles")).toBe("2026-11-01");
    expect(localDateAt(Date.parse("2026-11-02T08:00:00Z"), "America/Los_Angeles")).toBe("2026-11-02");
  });

  it("gives dates two days apart on either side of the date line at the same instant", () => {
    const instant = Date.parse("2026-06-15T10:30:00Z");

    expect(localDateAt(instant, "Pacific/Kiritimati")).toBe("2026-06-16");
    expect(localDateAt(instant, "Pacific/Pago_Pago")).toBe("2026-06-14");
  });

  it("accepts the epoch itself", () => {
    expect(localDateAt(0, "UTC")).toBe("1970-01-01");
  });

  it.each([-1, 1.5, Number.NaN, Number.POSITIVE_INFINITY, Number.MAX_SAFE_INTEGER, Date.UTC(9999, 11, 31) + 1])(
    "rejects asOf %d",
    (asOf) => {
      expect(() => localDateAt(asOf, "UTC")).toThrow(ConvexError);
    },
  );

  it("reports a bad asOf as ValidationFailed", () => {
    expect.assertions(1);
    try {
      localDateAt(1.5, "UTC");
    } catch (error) {
      expect((error as ConvexError<{ code: string }>).data.code).toBe("ValidationFailed");
    }
  });

  it("accepts the last supported instant", () => {
    expect(localDateAt(Date.UTC(9999, 11, 31), "Pacific/Kiritimati")).toBe("9999-12-31");
  });
});
