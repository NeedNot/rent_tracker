const KOTLIN_HARD_KEYWORDS = new Set([
  "as",
  "break",
  "class",
  "continue",
  "do",
  "else",
  "false",
  "for",
  "fun",
  "if",
  "in",
  "interface",
  "is",
  "null",
  "object",
  "package",
  "return",
  "super",
  "this",
  "throw",
  "true",
  "try",
  "typealias",
  "typeof",
  "val",
  "var",
  "when",
  "while",
]);

export function pascalCase(text: string): string {
  const words = text.split(/[^A-Za-z0-9]+/).filter((word) => word.length > 0);
  const joined = words.map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join("");
  return /^[0-9]/.test(joined) ? `Value${joined}` : joined;
}

export function camelCase(text: string): string {
  const pascal = pascalCase(text);
  return pascal.charAt(0).toLowerCase() + pascal.slice(1);
}

export function propertyName(fieldName: string): string {
  const name = camelCase(fieldName);
  return KOTLIN_HARD_KEYWORDS.has(name) ? `${name}Value` : name;
}

export function singular(word: string): string {
  if (word.endsWith("ies")) {
    return `${word.slice(0, -3)}y`;
  }
  if (/(ss|ch|sh|x)es$/.test(word)) {
    return word.slice(0, -2);
  }
  if (word.endsWith("s") && !word.endsWith("ss")) {
    return word.slice(0, -1);
  }
  return word;
}

export function packageSegment(convexFile: string): string {
  return convexFile
    .split("/")
    .map((segment) => segment.toLowerCase().replace(/[^a-z0-9]/g, ""))
    .join(".");
}
