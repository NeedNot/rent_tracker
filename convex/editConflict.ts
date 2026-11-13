import type { Id } from "./_generated/dataModel";
import type { QueryCtx } from "./_generated/server";
import { type AppErrorDetails, appError } from "./appError";

export interface VersionedRow {
  version: number;
  changedBy?: Id<"accounts">;
}

export async function requireExpectedVersion(
  ctx: QueryCtx,
  current: VersionedRow,
  expectedVersion: number,
  entity: string,
): Promise<void> {
  if (current.version === expectedVersion) {
    return;
  }
  const editor = current.changedBy === undefined ? null : await ctx.db.get(current.changedBy);
  if (editor === null) {
    throw appError("EditConflict", `This ${entity} changed while you were editing.`, {
      currentVersion: current.version,
    });
  }
  const details: AppErrorDetails = {
    currentVersion: current.version,
    lastEditedBy: editor._id,
    lastEditedByName: editor.displayName,
  };
  throw appError("EditConflict", `${editor.displayName} changed this ${entity} while you were editing.`, details);
}
