import ts from "typescript";
import { type RuleContext, type Violation, violationAt } from "../violation.ts";
import { forEachDescendant } from "../visit.ts";

const HISTORY_TABLE = "history";
const HISTORY_FILE = "history.ts";
const WRITE_HISTORY_FUNCTION = "writeHistory";

const WRITE_METHODS = ["insert", "patch", "replace", "delete"];
const DATABASE_WRITER_INTERFACES = ["GenericDatabaseWriter", "GenericDatabaseWriterWithTable", "BaseTableWriter"];
const TABLE_SCOPED_WRITER = "BaseTableWriter";

type TargetTables = string[] | "unknown";

export function checkHistoryWrites(context: RuleContext): Violation[] {
  const violations: Violation[] = [];

  forEachDescendant(context.sourceFile, (node) => {
    if (!ts.isCallExpression(node) || !ts.isPropertyAccessExpression(node.expression)) {
      return;
    }
    const method = node.expression.name.text;
    const writerInterface = databaseWriterInterface(node, context.checker);
    if (!WRITE_METHODS.includes(method) || writerInterface === undefined) {
      return;
    }
    if (!mayTargetHistory(targetTables(node, writerInterface, context.checker))) {
      return;
    }
    if (method !== "insert") {
      violations.push(violationAt(context, node, "history-immutable", `history rows can never be changed (${method})`));
    } else if (!isInsideWriteHistory(node, context)) {
      violations.push(
        violationAt(
          context,
          node,
          "history-write-through-helper",
          `insert history rows only through ${WRITE_HISTORY_FUNCTION} in convex/${HISTORY_FILE}`,
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

function mayTargetHistory(tables: TargetTables): boolean {
  return tables === "unknown" || tables.includes(HISTORY_TABLE);
}

function isInsideWriteHistory(node: ts.Node, context: RuleContext): boolean {
  if (context.relativePath !== HISTORY_FILE) {
    return false;
  }
  for (let current = node.parent; current !== undefined; current = current.parent) {
    if (ts.isFunctionDeclaration(current) && current.name?.text === WRITE_HISTORY_FUNCTION) {
      return true;
    }
    if (
      ts.isVariableDeclaration(current) &&
      ts.isIdentifier(current.name) &&
      current.name.text === WRITE_HISTORY_FUNCTION
    ) {
      return true;
    }
  }
  return false;
}
