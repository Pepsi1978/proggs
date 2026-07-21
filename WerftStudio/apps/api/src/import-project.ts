import path from "node:path";
import { Open } from "unzipper";

export const importLimits = {
  maxFiles: 5_000,
  // Upload-Strom: so viele Dateien duerfen ANKOMMEN (der Frontend-Filter sortiert im Strom aus)
  maxUploadFiles: 50_000,
  maxFileBytes: 100 * 1024 * 1024,
  maxTotalBytes: 1024 * 1024 * 1024
} as const;

export type ImportedFile = { path: string; data: Buffer; mime: string };

const mimeByExtension: Record<string, string> = {
  ".aac": "audio/aac", ".avif": "image/avif", ".css": "text/css; charset=utf-8", ".gif": "image/gif",
  ".htm": "text/html; charset=utf-8", ".html": "text/html; charset=utf-8", ".ico": "image/x-icon",
  ".jpeg": "image/jpeg", ".jpg": "image/jpeg", ".js": "text/javascript; charset=utf-8", ".json": "application/json; charset=utf-8",
  ".m4a": "audio/mp4", ".map": "application/json; charset=utf-8", ".mp3": "audio/mpeg", ".mp4": "video/mp4",
  ".ogg": "audio/ogg", ".otf": "font/otf", ".pdf": "application/pdf", ".png": "image/png",
  ".svg": "image/svg+xml", ".ts": "text/plain; charset=utf-8", ".ttf": "font/ttf", ".txt": "text/plain; charset=utf-8",
  ".wav": "audio/wav", ".webm": "video/webm", ".webp": "image/webp", ".woff": "font/woff", ".woff2": "font/woff2", ".zip": "application/zip"
};

export function mimeForPath(filePath: string): string {
  return mimeByExtension[path.posix.extname(filePath).toLowerCase()] ?? "application/octet-stream";
}

export function normalizeImportPath(rawPath: string): string {
  const value = rawPath.replaceAll("\\", "/").normalize("NFC");
  if (!value || value.length > 512 || value.startsWith("/") || /^[a-z]:/i.test(value)) throw new Error("Ungültiger Dateipfad im Import.");
  const segments = value.split("/");
  if (segments.some((segment) => !segment || segment === "." || segment === ".." || segment.includes("\0"))) throw new Error("Unsicherer Dateipfad im Import.");
  return segments.join("/");
}

export function stripCommonRoot(files: ImportedFile[]): ImportedFile[] {
  if (!files.length || files.some((file) => !file.path.includes("/"))) return files;
  const root = files[0]!.path.split("/", 1)[0]!;
  if (!files.every((file) => file.path.startsWith(`${root}/`))) return files;
  return files.map((file) => ({ ...file, path: file.path.slice(root.length + 1) }));
}

// Frontend-Erkennung per Scoring: echte Einstiegsseiten (index.html, Claude-Designs .dc.html,
// dist/public/web-Ordner) gewinnen; Werkzeug- und Hilfs-HTML (node_modules, plugins, skills,
// tests, review/template/viewer …) wird stark abgewertet, damit nie zufaellige Beifang-Dateien
// als Projekt-Frontend erscheinen.
const entryNegativeSegments = new Set(["node_modules", ".git", "obj", "bin", "test", "tests", "__tests__", "coverage", "plugins", "skills", "samples", "examples", "fixtures", "temp", "tmp", "backup", "docs", "doc"]);
const entryPositiveSegments = new Set(["dist", "build", "public", "web", "www", "frontend", "site", "app", "ui", "out", "html"]);
const entryNegativeNames = ["review", "template", "viewer", "eval", "report", "test", "changelog", "readme"];

export function scoreEntryPath(filePath: string): number {
  const segments = filePath.toLowerCase().split("/");
  const base = segments.at(-1)!;
  let score = 0;
  if (base === "index.html" || base === "index.htm") score += 100;
  if (base.endsWith(".dc.html")) score += 80;
  if (segments.some((segment) => entryPositiveSegments.has(segment))) score += 40;
  if (segments.some((segment) => entryNegativeSegments.has(segment))) score -= 80;
  if (entryNegativeNames.some((name) => base.includes(name))) score -= 40;
  return score - segments.length * 2;
}

export function chooseEntryPath(files: ImportedFile[]): string | undefined {
  const best = files
    .map((file) => file.path)
    .filter((filePath) => /\.html?$/i.test(filePath))
    .sort((left, right) => scoreEntryPath(right) - scoreEntryPath(left) || left.localeCompare(right))[0];
  // Deutlich negatives Scoring = nur Werkzeug-Beifang (z.B. Skill-Reviews in einer Desktop-App):
  // dann lieber KEINE Startseite melden statt eine irrefuehrende Fremd-Seite anzuzeigen.
  return best !== undefined && scoreEntryPath(best) > -20 ? best : undefined;
}

export function validateImportFiles(files: ImportedFile[]): ImportedFile[] {
  if (!files.length) throw new Error("Der Import enthält keine Dateien.");
  if (files.length > importLimits.maxFiles) throw new Error(`Der Import enthält mehr als ${importLimits.maxFiles} Dateien.`);
  let totalBytes = 0;
  const paths = new Set<string>();
  for (const file of files) {
    file.path = normalizeImportPath(file.path);
    if (file.data.byteLength > importLimits.maxFileBytes) throw new Error(`Die Datei „${file.path}“ ist größer als 100 MB.`);
    totalBytes += file.data.byteLength;
    if (totalBytes > importLimits.maxTotalBytes) throw new Error("Das entpackte Projekt ist größer als 1 GB.");
    const key = file.path.toLocaleLowerCase("en-US");
    if (paths.has(key)) throw new Error(`Der Dateipfad „${file.path}“ kommt mehrfach vor.`);
    paths.add(key);
  }
  return stripCommonRoot(files);
}

export async function expandZip(archive: Buffer): Promise<ImportedFile[]> {
  const directory = await Open.buffer(archive);
  const entries = directory.files.filter((entry) => entry.type === "File" && !entry.path.endsWith("/"));
  if (entries.length > importLimits.maxFiles) throw new Error(`Das ZIP enthält mehr als ${importLimits.maxFiles} Dateien.`);
  if (entries.some((entry) => (entry.flags & 1) !== 0)) throw new Error("Passwortgeschützte ZIP-Dateien werden nicht unterstützt.");
  const declaredBytes = entries.reduce((sum, entry) => sum + entry.uncompressedSize, 0);
  if (declaredBytes > importLimits.maxTotalBytes) throw new Error("Das entpackte ZIP ist größer als 1 GB.");
  const files: ImportedFile[] = [];
  for (const entry of entries) {
    const safePath = normalizeImportPath(entry.path);
    const data = await entry.buffer();
    files.push({ path: safePath, data, mime: mimeForPath(safePath) });
  }
  return validateImportFiles(files);
}
