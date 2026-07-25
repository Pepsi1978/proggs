import { emptyFacts, mergeFacts, type DesignFacts } from "./design-facts.js";
import { androidFactCandidates, extractAndroidFacts } from "./extract-android.js";
import { appleFactCandidates, extractAppleFacts } from "./extract-apple.js";
import { extractWebFacts, webFactCandidates, webStyleCandidates } from "./extract-web.js";
import { extractWindowsFacts, windowsFactCandidates } from "./extract-windows.js";
import type { SourceText } from "./extract-common.js";

export type FactPlatform = "web" | "android" | "ios" | "ipados" | "macos" | "windows";

// Projekte sind selten sortenrein (Android-App mit Web-Assets, macOS-App mit XAML-Doku). Deshalb
// laufen alle passenden Extraktoren, die Plattformwahl bestimmt nur die Reihenfolge der Wahrheit.
// `screens: false` fuer sekundaere Extraktoren: eine native App enthaelt oft eingebettete
// HTML-Seiten (Rechtstexte in 40 Sprachen, Hilfeseiten). Die sind KEINE App-Bildschirme und
// wuerden die echten Screens aus der Aufbauliste verdraengen.
type PlatformExtractor = { candidates: (paths: string[]) => string[]; extract: (files: SourceText[]) => Partial<DesignFacts>; screens?: boolean };
const extractorsByPlatform: Record<FactPlatform, PlatformExtractor[]> = {
  android: [{ candidates: androidFactCandidates, extract: extractAndroidFacts }, { candidates: webStyleCandidates, extract: extractWebFacts, screens: false }],
  ios: [{ candidates: appleFactCandidates, extract: extractAppleFacts }, { candidates: webStyleCandidates, extract: extractWebFacts, screens: false }],
  ipados: [{ candidates: appleFactCandidates, extract: extractAppleFacts }, { candidates: webStyleCandidates, extract: extractWebFacts, screens: false }],
  macos: [{ candidates: appleFactCandidates, extract: extractAppleFacts }, { candidates: webStyleCandidates, extract: extractWebFacts, screens: false }],
  windows: [{ candidates: windowsFactCandidates, extract: extractWindowsFacts }, { candidates: webStyleCandidates, extract: extractWebFacts, screens: false }],
  web: [{ candidates: webFactCandidates, extract: extractWebFacts }, { candidates: androidFactCandidates, extract: extractAndroidFacts, screens: false }]
};

export function factCandidatePaths(platform: FactPlatform, paths: string[]): string[] {
  const selected = new Set<string>();
  for (const extractor of extractorsByPlatform[platform]) for (const path of extractor.candidates(paths)) selected.add(path);
  return [...selected];
}

export function extractDesignFacts(platform: FactPlatform, files: SourceText[]): DesignFacts {
  let facts = emptyFacts(platform);
  for (const extractor of extractorsByPlatform[platform]) {
    const subset = new Set(extractor.candidates(files.map((file) => file.path)));
    if (!subset.size) continue;
    try {
      const extracted = extractor.extract(files.filter((file) => subset.has(file.path)));
      facts = mergeFacts(facts, extractor.screens === false ? { ...extracted, screens: [] } : extracted);
    }
    catch (error) { facts.notes.push(`Teil-Extraktion übersprungen: ${error instanceof Error ? error.message : "unbekannter Fehler"}`); }
  }
  return dedupeFacts(facts);
}

// Grosse Projekte liefern denselben Messwert hundertfach. Fuer den Prompt zaehlt jeder Wert einmal —
// mit Haeufigkeit, damit die dominante Groesse erkennbar bleibt.
function dedupeFacts(facts: DesignFacts): DesignFacts {
  // Beim Zusammenfassen gewinnt die Verwendung: kommt derselbe Wert einmal als blosse Definition
  // und einmal an einem Element vor, zaehlt er als verwendet — sonst faellt er aus der Nachmessung.
  const collapse = <T extends { name: string; used?: boolean }>(items: T[], key: (item: T) => string): T[] => {
    const groups = new Map<string, { item: T; count: number }>();
    for (const item of items) {
      const identity = key(item);
      const existing = groups.get(identity);
      if (existing) { existing.count += 1; if (item.used && !existing.item.used) existing.item = { ...existing.item, used: true }; }
      else groups.set(identity, { item, count: 1 });
    }
    return [...groups.values()]
      .sort((left, right) => Number(right.item.used ?? false) - Number(left.item.used ?? false) || right.count - left.count)
      .map(({ item, count }) => count > 1 ? { ...item, name: `${item.name} ×${count}` } : item);
  };
  return {
    ...facts,
    colors: collapse(facts.colors, (color) => `${color.name}|${color.css}`),
    dimensions: collapse(facts.dimensions, (dimension) => `${dimension.px}`).slice(0, 300),
    typography: collapse(facts.typography, (type) => `${type.family ?? ""}|${type.sizePx ?? ""}|${type.weight ?? ""}|${type.lineHeightPx ?? ""}`),
    shapes: collapse(facts.shapes, (shape) => shape.radiusCss),
    effects: collapse(facts.effects, (effect) => `${effect.kind}|${effect.css}`),
    assets: facts.assets,
    screens: facts.screens,
    themes: facts.themes,
    strings: facts.strings.slice(0, 400)
  };
}

// Screens werden nach Aussagekraft geordnet: Startbildschirm zuerst, danach alles mit erkannter
// Navigation. Der Nutzer soll die App in der Reihenfolge durchklicken, in der sie wirklich laeuft.
export function orderedScreens(facts: DesignFacts): DesignFacts["screens"] {
  const rank = (screen: DesignFacts["screens"][number]) => (screen.isStart ? 0 : 1) * 1000 - screen.navigatesTo.length - screen.files.length;
  return [...relevantScreens(facts.screens)].sort((left, right) => rank(left) - rank(right) || left.name.localeCompare(right.name));
}

// Eine Activity ist bei Compose- und Layout-Apps nur die Huelle um den eigentlichen Bildschirm.
// Wuerde sie mitgebaut, entstuende ein zusaetzlicher, inhaltsleerer "MainActivity"-Bildschirm —
// ihre einzige eigene Information ist, WELCHER Bildschirm beim Start sichtbar ist.
function relevantScreens(screens: DesignFacts["screens"]): DesignFacts["screens"] {
  // Eine reine Route (`route:consent`) und der Composable, der sie darstellt (`ConsentScreen` mit
  // route=consent), sind derselbe Bildschirm. Steht die Zuordnung erst dateiuebergreifend fest,
  // faellt die Dublette erst hier auf.
  const claimedRoutes = new Set(screens.filter((screen) => !screen.id.startsWith("route:") && screen.route).map((screen) => screen.route!));
  const deduped = screens.filter((screen) => !(screen.id.startsWith("route:") && claimedRoutes.has(screen.route ?? screen.id.slice(6))));
  const withContent = deduped.filter((screen) => !screen.id.startsWith("activity:"));
  if (!withContent.length) return deduped;
  const activityIsStart = deduped.some((screen) => screen.id.startsWith("activity:") && screen.isStart);
  if (!activityIsStart || withContent.some((screen) => screen.isStart)) return withContent;
  // Ohne eigenen Startvermerk erbt der erste inhaltstragende Bildschirm den Start der Activity.
  return withContent.map((screen, index) => index === 0 ? { ...screen, isStart: true } : screen);
}
