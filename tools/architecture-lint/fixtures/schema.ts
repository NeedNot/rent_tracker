import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  payments: defineTable({ amountCents: v.number(), deletedAt: v.optional(v.number()) }),
  debts: defineTable({ amountCents: v.number() }),
  rentTerms: defineTable({ amountCents: v.number() }),
  pushTokens: defineTable({ token: v.string() }),
});
