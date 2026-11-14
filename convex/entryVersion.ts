import type { Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { requireExpectedVersion } from "./editConflict";

interface VersionedEntry {
  version: number;
  lastEditedBy?: Id<"accounts">;
  deletedBy?: Id<"accounts">;
  bouncedBy?: Id<"accounts">;
}

export async function requireCurrentEntry(
  ctx: QueryCtx,
  entry: VersionedEntry,
  expectedVersion: number,
  entity: string,
): Promise<void> {
  const changedBy = entry.deletedBy ?? entry.bouncedBy ?? entry.lastEditedBy;
  await requireExpectedVersion(ctx, { version: entry.version, changedBy }, expectedVersion, entity);
}
