/// <reference types="node" />
import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { DatabaseSync, type SQLInputValue } from "node:sqlite";

const MIGRATIONS_DIRECTORY = join(import.meta.dirname, "../migrations");

type StatementResult = {
  readonly success: true;
  readonly results: unknown[];
  readonly meta: { readonly changes: number; readonly last_row_id: number };
};

class SqliteD1Statement {
  constructor(
    private readonly sqlite: DatabaseSync,
    private readonly sql: string,
    private readonly parameters: SQLInputValue[] = [],
  ) {}

  bind(...parameters: unknown[]): SqliteD1Statement {
    return new SqliteD1Statement(this.sqlite, this.sql, parameters as SQLInputValue[]);
  }

  async all(): Promise<StatementResult> {
    return this.execute();
  }

  async run(): Promise<StatementResult> {
    return this.execute();
  }

  async first(): Promise<unknown> {
    return this.execute().results[0] ?? null;
  }

  execute(): StatementResult {
    const changesBefore = this.totalChanges();
    const results = this.sqlite.prepare(this.sql).all(...this.parameters);
    const { lastInsertRowid } = this.sqlite.prepare("SELECT last_insert_rowid() AS lastInsertRowid").get() as {
      lastInsertRowid: number;
    };
    return {
      success: true,
      results,
      meta: { changes: this.totalChanges() - changesBefore, last_row_id: lastInsertRowid },
    };
  }

  private totalChanges(): number {
    const { totalChanges } = this.sqlite.prepare("SELECT total_changes() AS totalChanges").get() as {
      totalChanges: number;
    };
    return totalChanges;
  }
}

export function migratedSqliteD1(): D1Database {
  const sqlite = new DatabaseSync(":memory:");
  for (const migration of readdirSync(MIGRATIONS_DIRECTORY).sort()) {
    sqlite.exec(readFileSync(join(MIGRATIONS_DIRECTORY, migration), "utf8"));
  }
  const d1 = {
    prepare: (sql: string) => new SqliteD1Statement(sqlite, sql),
    batch: async (statements: SqliteD1Statement[]) => statements.map((statement) => statement.execute()),
    exec: async (sql: string) => sqlite.exec(sql),
  };
  return d1 as unknown as D1Database;
}
