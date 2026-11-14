import ts from "typescript";
import { type RuleContext, type Violation, violationAt } from "../violation.ts";
import { forEachDescendant } from "../visit.ts";

export const ACCESS_HELPERS = ["requireIdentity", "requireAccount", "requireMember", "requirePermission"];

const PUBLIC_FUNCTION_BUILDERS = ["query", "mutation", "action"];
const GENERATED_SERVER_MODULE = /(^|\/)_generated\/server(\.js)?$/;

type Handler = ts.ArrowFunction | ts.FunctionExpression | ts.MethodDeclaration;

export function checkAccessHelpers(context: RuleContext): Violation[] {
  const violations: Violation[] = [];

  forEachDescendant(context.sourceFile, (node) => {
    if (!ts.isCallExpression(node) || !isPublicFunctionBuilder(node.expression, context.checker)) {
      return;
    }
    const handler = findInlineHandler(node);
    if (handler === undefined) {
      violations.push(
        violationAt(
          context,
          node,
          "access-helper",
          "handler must be an inline function so its access check can be verified",
        ),
      );
    } else if (!callsAccessHelper(handler)) {
      violations.push(
        violationAt(context, handler, "access-helper", `handler must call one of ${ACCESS_HELPERS.join(", ")}`),
      );
    }
  });

  return violations;
}

function isPublicFunctionBuilder(callee: ts.Expression, checker: ts.TypeChecker): boolean {
  if (!ts.isIdentifier(callee)) {
    return false;
  }
  const declaration = checker.getSymbolAtLocation(callee)?.declarations?.[0];
  if (declaration === undefined || !ts.isImportSpecifier(declaration)) {
    return false;
  }
  const importedName = (declaration.propertyName ?? declaration.name).text;
  const moduleSpecifier = declaration.parent.parent.parent.moduleSpecifier;
  return (
    PUBLIC_FUNCTION_BUILDERS.includes(importedName) &&
    ts.isStringLiteral(moduleSpecifier) &&
    GENERATED_SERVER_MODULE.test(moduleSpecifier.text)
  );
}

function findInlineHandler(builderCall: ts.CallExpression): Handler | undefined {
  const definition = builderCall.arguments[0];
  if (definition === undefined) {
    return undefined;
  }
  if (isHandler(definition)) {
    return definition;
  }
  if (!ts.isObjectLiteralExpression(definition)) {
    return undefined;
  }
  for (const property of definition.properties) {
    if (property.name === undefined || !ts.isIdentifier(property.name) || property.name.text !== "handler") {
      continue;
    }
    if (ts.isMethodDeclaration(property)) {
      return property;
    }
    if (ts.isPropertyAssignment(property) && isHandler(property.initializer)) {
      return property.initializer;
    }
  }
  return undefined;
}

function isHandler(node: ts.Node): node is Handler {
  return ts.isArrowFunction(node) || ts.isFunctionExpression(node);
}

function callsAccessHelper(handler: Handler): boolean {
  let found = false;
  forEachDescendant(handler, (node) => {
    if (ts.isCallExpression(node) && ACCESS_HELPERS.includes(calleeName(node.expression) ?? "")) {
      found = true;
    }
  });
  return found;
}

function calleeName(callee: ts.Expression): string | undefined {
  if (ts.isIdentifier(callee)) {
    return callee.text;
  }
  if (ts.isPropertyAccessExpression(callee)) {
    return callee.name.text;
  }
  return undefined;
}
