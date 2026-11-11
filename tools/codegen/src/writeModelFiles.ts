import fs from "node:fs";
import path from "node:path";

export function writeModelFiles(modelDir: string, files: Map<string, string>): void {
  fs.rmSync(modelDir, { recursive: true, force: true });
  for (const [relativePath, content] of files) {
    const filePath = path.join(modelDir, relativePath);
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, content);
  }
}
