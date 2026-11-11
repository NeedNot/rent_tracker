import ts from "typescript";

export function forEachDescendant(node: ts.Node, visitor: (node: ts.Node) => void): void {
  ts.forEachChild(node, (child) => {
    visitor(child);
    forEachDescendant(child, visitor);
  });
}
