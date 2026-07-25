export type ImportPlatform = "web" | "android" | "ios" | "ipados" | "macos" | "windows";

export type ImportManifestFile = { path: string; size: number; mime: string };

export type PreviewProfile = {
  width: number;
  height: number;
  device: string;
  density: number;
};

export const previewProfiles: Record<ImportPlatform, PreviewProfile> = {
  web: { width: 1440, height: 900, device: "Desktop", density: 1 },
  android: { width: 412, height: 915, device: "Pixel 9", density: 2.625 },
  ios: { width: 393, height: 852, device: "iPhone 15 Pro", density: 3 },
  ipados: { width: 1024, height: 1366, device: "iPad Pro 13", density: 2 },
  macos: { width: 1280, height: 800, device: "macOS Fenster", density: 2 },
  windows: { width: 1280, height: 800, device: "Windows Fenster", density: 1 }
};

export function previewProfileFromHtml(html: string, fallback: PreviewProfile): PreviewProfile {
  const tag = html.match(/<meta\b[^>]*\bname\s*=\s*["']werft-preview["'][^>]*>/i)?.[0]
    ?? html.match(/<meta\b[^>]*\bcontent\s*=\s*["'][^>]*["'][^>]*\bname\s*=\s*["']werft-preview["'][^>]*>/i)?.[0];
  const raw = tag?.match(/\bcontent\s*=\s*(["'])(.*?)\1/i)?.[2];
  if (!raw) return fallback;
  try {
    const value = JSON.parse(raw.replaceAll("&quot;", '"').replaceAll("&#39;", "'").replaceAll("&amp;", "&")) as Partial<PreviewProfile>;
    if (!Number.isFinite(value.width) || !Number.isFinite(value.height) || value.width! < 200 || value.height! < 200 || value.width! > 10_000 || value.height! > 10_000) return fallback;
    return {
      width: Math.round(value.width!),
      height: Math.round(value.height!),
      device: typeof value.device === "string" && value.device.trim() ? value.device.trim().slice(0, 80) : fallback.device,
      density: Number.isFinite(value.density) && value.density! > 0 ? value.density! : fallback.density
    };
  } catch {
    return fallback;
  }
}

const generatedOrThirdParty = /(^|\/)(node_modules|\.git|\.werft-generated|\.gradle|\.idea|\.vs|bin|obj|build|dist|out|target|coverage|logs?|test|tests|__tests__|backend|server|database|__pycache__|vendor)(\/|$)/i;
const sourceExtension = /\.(?:html?|css|scss|sass|less|js|mjs|cjs|jsx|ts|tsx|vue|svelte|xaml|axaml|xml|kt|kts|java|swift|dart|cs|fs|vb|c|cc|cpp|cxx|h|hh|hpp|m|mm|qml|ui|json|json5|toml|ya?ml|properties|gradle|groovy|storyboard|plist|strings|xcstrings|resx|md|txt|razor|cshtml|uxml|uss|aidl|go|lua|pas|php|ps1|py|rb|rs)$/i;
const lowValueName = /(?:^|\/)(?:package-lock\.json|pnpm-lock\.yaml|yarn\.lock|podfile\.lock|gradle\.lockfile)$/i;
// Reine Bau-, Abhaengigkeits- und Werkzeugdateien tragen keinen einzigen Designwert, verbrauchen
// aber volle KI-Analysepakete. Sie draussen zu lassen ist der groesste Geschwindigkeitshebel.
const nonDesignName = /(?:^|\/)(?:build\.gradle(?:\.kts)?|settings\.gradle(?:\.kts)?|gradle\.properties|gradlew(?:\.bat)?|local\.properties|proguard-rules\.pro|CMakeLists\.txt|Package\.swift|Podfile|Cartfile|\.editorconfig|\.gitignore|\.gitattributes|LICENSE(?:\.\w+)?|CHANGELOG\.md|README(?:\.\w+)?|CONTRIBUTING\.md|CODE_OF_CONDUCT\.md|\w+\.csproj|\w+\.sln|\w+\.vcxproj|tsconfig(?:\.\w+)?\.json|jest\.config\.\w+|vitest\.config\.\w+|eslint\.config\.\w+|\.eslintrc(?:\.\w+)?|babel\.config\.\w+)$/i;
// Uebersetzungen und Nicht-Standard-Qualifier beschreiben denselben Screen erneut: fuer die
// Rekonstruktion zaehlt der Standardsatz, sonst analysiert die KI 40× dasselbe Layout.
const localizedResource = /(^|\/)res\/values-(?!night\b)[a-z]{2}(?:-r[A-Z]{2})?(?:-\w+)*\//i;
const localizedApple = /(^|\/)[a-z]{2}(?:-[A-Z]{2})?\.lproj\//i;
const businessLogicPath = /(^|\/)(?:data|domain|network|repository|repositories|api|db|database|dao|entity|entities|model|models|service|services|usecase|usecases|di|inject|worker|analytics|billing|sync|util|utils|helper|helpers|extension|extensions)(\/|$)/i;
const uiPath = /(^|\/)(?:ui|view|views|screen|screens|page|pages|component|components|widget|widgets|compose|presentation|theme|themes|style|styles|res|resources|layout|assets|design|navigation|nav)(\/|$)|\.(?:xaml|axaml|storyboard|xib|qml|uxml|uss|css|scss|sass|less|html?)$/i;

export function reconstructionSourceFiles(files: ImportManifestFile[]): ImportManifestFile[] {
  return files
    .filter((file) => !generatedOrThirdParty.test(file.path) && !lowValueName.test(file.path) && !nonDesignName.test(file.path) && !localizedResource.test(file.path) && !localizedApple.test(file.path) && (sourceExtension.test(file.path) || file.mime.startsWith("text/")))
    // Reine Geschaeftslogik ohne UI-Bezug fliegt raus: sie erklaert kein Pixel, kostet aber Analysezeit.
    .filter((file) => uiPath.test(file.path) || !businessLogicPath.test(file.path))
    .sort((left, right) => sourceScore(right.path) - sourceScore(left.path) || left.path.localeCompare(right.path));
}

function sourceScore(filePath: string): number {
  const lower = filePath.toLowerCase();
  let score = 0;
  if (/(screen|page|view|window|activity|fragment|component|layout|main|app)/.test(lower)) score += 80;
  if (/(theme|style|color|typography|font|dimens?|spacing|shape|drawable|assets?|res\/)/.test(lower)) score += 60;
  if (/(navigation|router|route|state|viewmodel)/.test(lower)) score += 30;
  if (/(test|fixture|sample|example)/.test(lower)) score -= 50;
  return score - lower.split("/").length;
}

// Analysepakete liefen bisher streng nacheinander — bei grossen Projekten ist das der eigentliche
// Zeitfresser. Hier laufen sie mit begrenzter Nebenlaeufigkeit, aber die Ergebnisreihenfolge bleibt
// exakt erhalten, damit die Evidenz reproduzierbar bleibt.
export async function mapWithConcurrency<TInput, TOutput>(items: TInput[], limit: number, worker: (item: TInput, index: number) => Promise<TOutput>): Promise<TOutput[]> {
  const results = new Array<TOutput>(items.length);
  const width = Math.max(1, Math.min(limit, items.length));
  let cursor = 0;
  let failure: unknown;
  let failed = false;
  const runners = Array.from({ length: width }, async () => {
    while (!failed) {
      const index = cursor++;
      if (index >= items.length) return;
      try { results[index] = await worker(items[index]!, index); }
      catch (error) { if (!failed) { failed = true; failure = error; } return; }
    }
  });
  await Promise.all(runners);
  if (failed) throw failure;
  return results;
}

export const reconstructionConcurrency = 4;

export type SourceBatch = { text: string; completedBytes: number; completedFiles: number };

type SourceChunk = { start: number; end: number; text: string };
async function* sourceChunks(content: string | AsyncIterable<Uint8Array>, maxChunkChars: number, overlapChars: number): AsyncGenerator<SourceChunk> {
  let buffer = "";
  let offset = 0;
  const overlap = Math.max(0, Math.min(overlapChars, Math.floor(maxChunkChars / 4)));
  const append = async function* (final: boolean): AsyncGenerator<SourceChunk> {
    while (buffer.length > maxChunkChars || (final && buffer.length > 0)) {
      const length = final ? Math.min(maxChunkChars, buffer.length) : maxChunkChars;
      yield { start: offset, end: offset + length, text: buffer.slice(0, length) };
      if (length === buffer.length) { offset += length; buffer = ""; break; }
      const advance = length - overlap;
      buffer = buffer.slice(advance);
      offset += advance;
    }
  };
  if (typeof content === "string") buffer = content;
  else {
    const decoder = new TextDecoder("utf-8", { fatal: false });
    for await (const bytes of content) {
      buffer += decoder.decode(bytes, { stream: true });
      for await (const chunk of append(false)) yield chunk;
    }
    buffer += decoder.decode();
  }
  for await (const chunk of append(true)) yield chunk;
  if (offset === 0 && !buffer) yield { start: 0, end: 0, text: "" };
}

export async function* buildSourceBatches(
  files: ImportManifestFile[],
  readText: (file: ImportManifestFile) => Promise<string | AsyncIterable<Uint8Array>>,
  maxBatchChars = 220_000,
  maxChunkChars = 80_000,
  overlapChars = 1_500
): AsyncGenerator<SourceBatch> {
  let text = "";
  let completedBytes = 0;
  let completedFiles = 0;
  const flush = () => {
    const batch = { text, completedBytes, completedFiles };
    text = "";
    completedBytes = 0;
    completedFiles = 0;
    return batch;
  };

  for (const file of files) {
    const content = await readText(file);
    let pending: SourceChunk | undefined;
    for await (const chunk of sourceChunks(content, maxChunkChars, overlapChars)) {
      if (!pending) { pending = chunk; continue; }
      const block = `\n--- ${file.path} [Zeichen ${pending.start}-${pending.end}] ---\n${pending.text}\n`;
      if (text && text.length + block.length > maxBatchChars) yield flush();
      text += block;
      pending = chunk;
    }
    if (pending) {
      const block = `\n--- ${file.path} [Zeichen ${pending.start}-${pending.end}; Dateiende] ---\n${pending.text}\n`;
      if (text && text.length + block.length > maxBatchChars) yield flush();
      text += block;
      completedBytes += file.size;
      completedFiles += 1;
    }
  }
  if (text) yield flush();
}

export const reconstructionTodos = [
  "Projektdateien vollständig inventarisieren",
  "Farben, Maße, Typografie und Effekte exakt aus den Quellen messen",
  "UI-Frameworks, Screens und Navigation erkennen",
  "Assets, Icons und Themes auflösen",
  "Alle Bildschirme bearbeitbar aufbauen",
  "Gegen die gemessenen Quellwerte nachprüfen"
] as const;

export function canRestartReconstructionJob(status: string, retryFailed: boolean): boolean {
  return status === "failed" && retryFailed;
}

export type ReconstructionOperationKind = "analysis" | "compaction" | "build" | "verification";
export type ReconstructionTimingSample = { kind: ReconstructionOperationKind; durationMs: number };

const reconstructionOperationWeights: Record<ReconstructionOperationKind, number> = {
  analysis: 1,
  compaction: 0.8,
  build: 2,
  verification: 2
};

export function estimateAnalysisCallCount(totalBytes: number, batchChars = 220_000): number {
  return Math.max(1, Math.ceil(Math.max(0, totalBytes) / batchChars));
}

export function reconstructionTiming(
  samples: ReconstructionTimingSample[],
  currentKind: ReconstructionOperationKind | null,
  currentElapsedMs: number,
  remainingWeight: number
): { phaseProgress: number | null; estimatedRemainingMs: number | null } {
  if (!samples.length) return { phaseProgress: null, estimatedRemainingMs: null };
  const unitDurationMs = Math.max(1, samples.reduce((sum, sample) => sum + sample.durationMs / reconstructionOperationWeights[sample.kind], 0) / samples.length);
  const expectedCurrentMs = currentKind ? unitDurationMs * reconstructionOperationWeights[currentKind] : 0;
  const currentOverdue = currentKind !== null && currentElapsedMs >= expectedCurrentMs;
  const currentRemainingMs = Math.max(0, expectedCurrentMs - Math.max(0, currentElapsedMs));
  return {
    phaseProgress: currentKind ? Math.min(95, Math.max(0, currentElapsedMs) / expectedCurrentMs * 100) : null,
    estimatedRemainingMs: currentOverdue ? null : Math.round(currentRemainingMs + Math.max(0, remainingWeight) * unitDurationMs)
  };
}
