import {
  type ActionBuilder,
  actionGeneric,
  type DataModelFromSchemaDefinition,
  type GenericMutationCtx,
  type GenericMutationCtxWithTable,
  internalQueryGeneric,
  type MutationBuilder,
  mutationGeneric,
  type QueryBuilder,
  queryGeneric,
} from "convex/server";
import type schema from "../schema";

export type DataModel = DataModelFromSchemaDefinition<typeof schema>;
export type MutationCtx = GenericMutationCtx<DataModel>;
export type MutationCtxWithTable = GenericMutationCtxWithTable<DataModel>;

export const query: QueryBuilder<DataModel, "public"> = queryGeneric;
export const internalQuery: QueryBuilder<DataModel, "internal"> = internalQueryGeneric;
export const mutation: MutationBuilder<DataModel, "public"> = mutationGeneric;
export const action: ActionBuilder<DataModel, "public"> = actionGeneric;
