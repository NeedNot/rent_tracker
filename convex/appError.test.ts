import { ConvexError } from "convex/values";
import { describe, expect, it } from "vitest";
import { appError, validationFailed } from "./appError";

describe("appError", () => {
  it("carries the code, message and details as ConvexError data", () => {
    const error = appError("EditConflict", "Sam changed this tenant while you were editing.", { version: 3 });

    expect(error).toBeInstanceOf(ConvexError);
    expect(error.data).toEqual({
      code: "EditConflict",
      message: "Sam changed this tenant while you were editing.",
      details: { version: 3 },
    });
  });

  it("defaults details to an empty object", () => {
    expect(validationFailed("Name is required.").data).toEqual({
      code: "ValidationFailed",
      message: "Name is required.",
      details: {},
    });
  });
});
