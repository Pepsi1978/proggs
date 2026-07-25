import path from "node:path";
import { Open } from "unzipper";

export const importLimits = {
  // Ordnerimporte werden gestreamt und haben kein kuenstliches Projektgroessenlimit. Die hohe
  // Multipart-Grenze ist nur ein Protokollschutz; Archive bleiben gegen Zip-Bombs begrenzt.
  maxUploadFiles: 1_000_000,
  maxFileBytes: Number.MAX_SAFE_INTEGER,
  maxArchiveFiles: 50_000,
  maxArchiveFileBytes: 256 * 1024 * 1024,
  maxArchiveTotalBytes: 1024 * 1024 * 1024
} as const;

export type ImportedFile = { path: string; data: Buffer; mime: string };

const mimeByExtension: Record<string, string> = {
  ".aac": "audio/aac", ".avif": "image/avif", ".css": "text/css; charset=utf-8", ".gif": "image/gif",
  ".htm": "text/html; charset=utf-8", ".html": "text/html; charset=utf-8", ".ico": "image/x-icon",
  ".jpeg": "image/jpeg", ".jpg": "image/jpeg", ".js": "text/javascript; charset=utf-8", ".json": "application/json; charset=utf-8",
  ".m4a": "audio/mp4", ".map": "application/json; charset=utf-8", ".mp3": "audio/mpeg", ".mp4": "video/mp4",
  ".ogg": "audio/ogg", ".otf": "font/otf", ".pdf": "application/pdf", ".png": "image/png",
  ".aidl": "text/plain; charset=utf-8", ".c": "text/plain; charset=utf-8", ".cc": "text/plain; charset=utf-8", ".cjs": "text/javascript; charset=utf-8", ".cpp": "text/plain; charset=utf-8", ".cs": "text/plain; charset=utf-8", ".cshtml": "text/html; charset=utf-8",
  ".dart": "text/plain; charset=utf-8", ".gradle": "text/plain; charset=utf-8", ".groovy": "text/plain; charset=utf-8", ".h": "text/plain; charset=utf-8",
  ".hpp": "text/plain; charset=utf-8", ".java": "text/plain; charset=utf-8", ".kt": "text/plain; charset=utf-8", ".kts": "text/plain; charset=utf-8",
  ".go": "text/plain; charset=utf-8", ".lua": "text/plain; charset=utf-8", ".md": "text/markdown; charset=utf-8", ".mjs": "text/javascript; charset=utf-8", ".pas": "text/plain; charset=utf-8", ".php": "text/plain; charset=utf-8", ".plist": "text/plain; charset=utf-8", ".properties": "text/plain; charset=utf-8", ".ps1": "text/plain; charset=utf-8", ".py": "text/plain; charset=utf-8", ".qml": "text/plain; charset=utf-8",
  ".resx": "application/xml; charset=utf-8", ".scss": "text/css; charset=utf-8", ".storyboard": "application/xml; charset=utf-8", ".strings": "text/plain; charset=utf-8",
  ".razor": "text/plain; charset=utf-8", ".rb": "text/plain; charset=utf-8", ".rs": "text/plain; charset=utf-8", ".svg": "image/svg+xml", ".swift": "text/plain; charset=utf-8", ".toml": "text/plain; charset=utf-8", ".ts": "text/plain; charset=utf-8", ".tsx": "text/plain; charset=utf-8", ".ttf": "font/ttf", ".txt": "text/plain; charset=utf-8", ".uss": "text/css; charset=utf-8", ".uxml": "application/xml; charset=utf-8",
  ".xaml": "application/xml; charset=utf-8", ".xcstrings": "application/json; charset=utf-8", ".xml": "application/xml; charset=utf-8", ".yaml": "text/plain; charset=utf-8", ".yml": "text/plain; charset=utf-8",
  ".wasm": "application/wasm", ".wav": "audio/wav", ".webm": "video/webm", ".webmanifest": "application/manifest+json", ".webp": "image/webp", ".werft": "application/json; charset=utf-8", ".woff": "font/woff", ".woff2": "font/woff2", ".zip": "application/zip"
};

export function mimeForPath(filePath: string): string {
  return mimeByExtension[path.posix.extname(filePath).toLowerCase()] ?? "application/octet-stream";
}

const frontendDropSegments = /(^|\/)(node_modules|\.git|\.werft-generated|bin|obj|target|dist-info|__pycache__|\.gradle|\.idea|\.vs|coverage|logs?|test|tests|__tests__|backend|server|database)(\/|$)/i;
const nativeGeneratedSegments = /(^|\/)(build|out)(\/|$)/i;
const frontendDropNames = /(package-lock\.json|pnpm-lock\.yaml|yarn\.lock|\.map)$/i;
const frontendKeepExtensions = /\.(html?|css|scss|sass|less|svg|png|jpe?g|webp|gif|ico|avif|woff2?|ttf|otf|eot|mp3|wav|ogg|m4a|mp4|webm|wasm|webmanifest|werft|xaml|axaml|xml|jsx|tsx|vue|svelte|dart|json|json5|ya?ml|toml|properties|gradle|groovy|storyboard|plist|strings|xcstrings|resx|qml|ui|md|razor|cshtml|uxml|uss)$/i;
const frontendCodeExtensions = /\.(js|mjs|cjs|ts|kt|kts|java|swift|cs|fs|vb|c|cc|cpp|cxx|h|hh|hpp|m|mm|aidl|go|lua|pas|php|ps1|py|rb|rs)$/i;

// Reproduzierbare Werkzeugausgaben und Fremdabhaengigkeiten enthalten nie Designquellen, blaehen
// aber Upload, Speicher und Analyse massiv auf (und liefern irrefuehrende Report-HTML-Seiten).
// Sie bleiben deshalb IMMER draussen — auch wenn "nur Frontend" abgeschaltet ist.
const alwaysDropSegments = /(^|\/)(node_modules|\.git|\.gradle|\.idea|\.vs|\.vscode|\.svn|\.hg|\.dart_tool|\.next|\.nuxt|\.turbo|\.venv|venv|__pycache__|Pods|Carthage|DerivedData|xcuserdata|\.werft-generated|vendor|bower_components)(\/|$)/i;
const alwaysDropReportSegments = /(^|\/)(reports?|jacoco|javadoc|dokka|kdoc|lint-results|coverage|allure-report|playwright-report|intermediates|kapt|ksp|captures)(\/|$)/i;
const alwaysDropNames = /(\.(?:map|lock|pyc|class|o|obj|a|so|dll|pdb|apk|aab|aar|jar|war|ipa|dmg|exe|msi|dSYM|hprof|log)|package-lock\.json|pnpm-lock\.yaml|yarn\.lock)$/i;

export function isGeneratedArtifact(filePath: string, platform = "web"): boolean {
  if (alwaysDropSegments.test(filePath) || alwaysDropNames.test(filePath)) return true;
  // Bei nativen Projekten ist build/ und out/ garantiert Ausgabe; bei Web kann dort das fertige
  // Bundle liegen, deshalb wird dort nur der Berichtsanteil verworfen.
  if (platform !== "web" && nativeGeneratedSegments.test(filePath)) return true;
  return alwaysDropReportSegments.test(filePath) && (platform !== "web" || nativeGeneratedSegments.test(filePath));
}

export function isFrontendFile(filePath: string, platform = "web"): boolean {
  if (isGeneratedArtifact(filePath, platform)) return false;
  if (frontendDropSegments.test(filePath) || frontendDropNames.test(filePath) || (platform !== "web" && nativeGeneratedSegments.test(filePath))) return false;
  return frontendKeepExtensions.test(filePath) || frontendCodeExtensions.test(filePath);
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
// Werkzeugberichte (Gradle-Testreports, Jacoco, Lint, Dokka, Javadoc) enthalten fast immer eine
// index.html und wuerden das Scoring sonst gewinnen — sie sind NIE das Design der App.
const generatedReportSegments = new Set(["reports", "jacoco", "javadoc", "dokka", "kdoc", "lint-results", "htmlreport", "html-report", "allure-report", "playwright-report", "intermediates", "generated", "tmp", "kapt", "ksp"]);
// Native Projekte legen unter build/ und out/ nur Artefakte ab; dort steht niemals die App-Oberflaeche.
const nativeGeneratedEntrySegments = new Set(["build", "out", "obj", "bin", "deriveddata", "xcuserdata", "captures"]);

export function scoreEntryPath(filePath: string, platform = "web"): number {
  const segments = filePath.toLowerCase().split("/");
  const base = segments.at(-1)!;
  if (segments.some((segment) => generatedReportSegments.has(segment))) return -1000;
  if (platform !== "web" && segments.some((segment) => nativeGeneratedEntrySegments.has(segment))) return -1000;
  let score = 0;
  if (base === "index.html" || base === "index.htm") score += 100;
  if (base.endsWith(".dc.html")) score += 80;
  if (segments.some((segment) => entryPositiveSegments.has(segment))) score += 40;
  if (segments.some((segment) => entryNegativeSegments.has(segment))) score -= 80;
  if (entryNegativeNames.some((name) => base.includes(name))) score -= 40;
  return score - segments.length * 2;
}

export function chooseEntryPath(files: Array<{ path: string }>, platform = "web"): string | undefined {
  const best = files
    .map((file) => file.path)
    .filter((filePath) => /\.html?$/i.test(filePath))
    .sort((left, right) => scoreEntryPath(right, platform) - scoreEntryPath(left, platform) || left.localeCompare(right))[0];
  // Deutlich negatives Scoring = nur Werkzeug-Beifang (z.B. Skill-Reviews in einer Desktop-App):
  // dann lieber KEINE Startseite melden statt eine irrefuehrende Fremd-Seite anzuzeigen.
  return best !== undefined && scoreEntryPath(best, platform) > -20 ? best : undefined;
}

export function validateImportFiles(files: ImportedFile[]): ImportedFile[] {
  if (!files.length) throw new Error("Der Import enthält keine Dateien.");
  const paths = new Set<string>();
  for (const file of files) {
    file.path = normalizeImportPath(file.path);
    const key = file.path.toLocaleLowerCase("en-US");
    if (paths.has(key)) throw new Error(`Der Dateipfad „${file.path}“ kommt mehrfach vor.`);
    paths.add(key);
  }
  return stripCommonRoot(files);
}

export async function expandZip(archive: Buffer): Promise<ImportedFile[]> {
  const directory = await Open.buffer(archive);
  const entries = directory.files.filter((entry) => entry.type === "File" && !entry.path.endsWith("/"));
  if (entries.length > importLimits.maxArchiveFiles) throw new Error(`Das ZIP enthält mehr als ${importLimits.maxArchiveFiles} Dateien. Große Projekte bitte als Ordner importieren.`);
  if (entries.some((entry) => (entry.flags & 1) !== 0)) throw new Error("Passwortgeschützte ZIP-Dateien werden nicht unterstützt.");
  const declaredBytes = entries.reduce((sum, entry) => sum + entry.uncompressedSize, 0);
  if (declaredBytes > importLimits.maxArchiveTotalBytes) throw new Error("Das entpackte ZIP ist größer als 1 GB. Große Projekte bitte als Ordner streamen.");
  const files: ImportedFile[] = [];
  let actualTotalBytes = 0;
  for (const entry of entries) {
    const safePath = normalizeImportPath(entry.path);
    if (entry.uncompressedSize > importLimits.maxArchiveFileBytes) throw new Error(`Die Archivdatei „${safePath}“ ist größer als 256 MB. Große Projekte bitte als Ordner streamen.`);
    const chunks: Buffer[] = [];
    let actualFileBytes = 0;
    for await (const chunk of entry.stream()) {
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      actualFileBytes += buffer.byteLength;
      actualTotalBytes += buffer.byteLength;
      if (actualFileBytes > importLimits.maxArchiveFileBytes) throw new Error(`Die tatsächlich entpackte Archivdatei „${safePath}“ ist größer als 256 MB.`);
      if (actualTotalBytes > importLimits.maxArchiveTotalBytes) throw new Error("Die tatsächlich entpackten ZIP-Daten sind größer als 1 GB.");
      chunks.push(buffer);
    }
    const data = Buffer.concat(chunks, actualFileBytes);
    files.push({ path: safePath, data, mime: mimeForPath(safePath) });
  }
  return validateImportFiles(files);
}
