import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  payments: defineTable({ amountCents: v.number() }),
  history: defineTable({ action: v.string() }),
});
