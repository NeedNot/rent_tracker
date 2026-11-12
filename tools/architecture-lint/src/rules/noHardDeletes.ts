import ts from "typescript";
import { type RuleContext, type Violation, violationAt } from "../violation.ts";
import { forEachDescendant } from "../visit.ts";

export const SOFT_DELETED_TABLES = ["payments", "debts", "rentTerms"];

const DATABASE_WRITER_INTERFACES = ["GenericDatabaseWriter", "GenericDatabaseWriterWithTable", "BaseTableWriter"];
const TABLE_SCOPED_WRITER = "BaseTableWriter";

type TargetTables = string[] | "unknown";

export function checkNoHardDeletes(context: RuleContext): Violation[] {
  const violations: Violation[] = [];

  forEachDescendant(context.sourceFile, (node) => {
    if (!ts.isCallExpression(node) || !ts.isPropertyAccessExpression(node.expression)) {
      return;
    }
    if (node.expression.name.text !== "delete") {
      return;
    }
    const writerInterface = databaseWriterInterface(node, context.checker);
    if (writerInterface === undefined) {
      return;
    }
    if (maySoftDeletedTable(targetTables(node, writerInterface, context.checker))) {
      violations.push(
        violationAt(
          context,
          node,
          "no-hard-delete",
          `${SOFT_DELETED_TABLES.join(", ")} are cancelled with deletedAt, never deleted`,
        ),
      );
    }
  });

  return violations;
}

function databaseWriterInterface(call: ts.CallExpression, checker: ts.TypeChecker): string | undefined {
  const declaration = checker.getResolvedSignature(call)?.getDeclaration();
  const owner = declaration?.parent;
  if (owner === undefined || !ts.isInterfaceDeclaration(owner)) {
    return undefined;
  }
  return DATABASE_WRITER_INTERFACES.includes(owner.name.text) ? owner.name.text : undefined;
}

function targetTables(call: ts.CallExpression, writerInterface: string, checker: ts.TypeChecker): TargetTables {
  if (writerInterface === TABLE_SCOPED_WRITER && ts.isPropertyAccessExpression(call.expression)) {
    const writerType = checker.getTypeAtLocation(call.expression.expression) as ts.TypeReference;
    const tableType = checker.getTypeArguments(writerType)[1];
    return tableType === undefined ? "unknown" : tableNames(tableType);
  }
  const firstArgument = call.arguments[0];
  if (firstArgument === undefined) {
    return "unknown";
  }
  const argumentType = checker.getTypeAtLocation(firstArgument);
  const idTable = argumentType.getProperty("__tableName");
  return tableNames(idTable === undefined ? argumentType : checker.getTypeOfSymbolAtLocation(idTable, firstArgument));
}

function tableNames(type: ts.Type): TargetTables {
  if (type.isUnion()) {
    const names: string[] = [];
    for (const member of type.types) {
      const memberNames = tableNames(member);
      if (memberNames === "unknown") {
        return "unknown";
      }
      names.push(...memberNames);
    }
    return names;
  }
  return type.isStringLiteral() ? [type.value] : "unknown";
}

function maySoftDeletedTable(tables: TargetTables): boolean {
  return tables === "unknown" || tables.some((table) => SOFT_DELETED_TABLES.includes(table));
}
