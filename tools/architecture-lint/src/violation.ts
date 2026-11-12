import type ts from "typescript";

export type RuleName = "access-helper" | "no-hard-delete";

export interface Violation {
  rule: RuleName;
  file: string;
  line: number;
  column: number;
  message: string;
}

export interface RuleContext {
  sourceFile: ts.SourceFile;
  checker: ts.TypeChecker;
  relativePath: string;
}

export function violationAt(context: RuleContext, node: ts.Node, rule: RuleName, message: string): Violation {
  const position = context.sourceFile.getLineAndCharacterOfPosition(node.getStart(context.sourceFile));
  return {
    rule,
    file: context.relativePath,
    line: position.line + 1,
    column: position.character + 1,
    message,
  };
}

export function formatViolation(violation: Violation): string {
  return `${violation.file}:${violation.line}:${violation.column} ${violation.rule}: ${violation.message}`;
}
