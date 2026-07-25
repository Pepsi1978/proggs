import type { DesignFacts } from "./design-facts.js";

// Die letzten Prozent Genauigkeit entstehen nicht durch bessere Prompts, sondern durch Nachmessen:
// hier wird das erzeugte HTML gegen die geparsten Quellwerte geprueft und jede Abweichung als
// konkrete, nachbesserbare Anweisung zurueckgegeben.
//
// Streng geprueft wird nur, was nachweislich an einem Element haengt (`used`). Eine bloss
// DEFINIERTE Ressourcenfarbe muss im Design nicht vorkommen — sie einzufordern wuerde die
// Rekonstruktion verfaelschen statt sie zu verbessern.
export type FidelityIssue = { severity: "blocker" | "abweichung"; area: string; detail: string; source?: string };
export type FidelityReport = { issues: FidelityIssue[]; checked: number; matched: number; score: number };

// Diese Theme-Rollen sind in jeder App sichtbar; alle weiteren Token koennen legitim ungenutzt sein.
const coreThemeTokens = /^(?:android:)?(?:color)?(?:primary|secondary|tertiary|background|surface|error|onPrimary|onSecondary|onBackground|onSurface|outline|accent)$/i;

const normalizeColor = (value: string) => value.trim().toLowerCase().replace(/\s+/g, "");
const stripSpaces = (value: string) => value.replace(/\s+/g, " ").trim().toLowerCase();

function htmlColors(html: string): Set<string> {
  const found = new Set<string>();
  for (const match of html.matchAll(/#[0-9a-f]{3,8}\b|rgba?\([^)]*\)|hsla?\([^)]*\)/gi)) found.add(normalizeColor(match[0]));
  return found;
}

// Ein Hexwert und sein rgba()-Zwilling sind derselbe Farbton — ohne diese Gleichsetzung meldet die
// Pruefung staendig falsche Abweichungen.
function colorVariants(css: string): string[] {
  const value = normalizeColor(css);
  const hex = /^#([0-9a-f]{6})$/.exec(value);
  if (hex) {
    const [red, green, blue] = [0, 2, 4].map((offset) => Number.parseInt(hex[1]!.slice(offset, offset + 2), 16));
    return [value, `rgb(${red},${green},${blue})`, `rgba(${red},${green},${blue},1)`];
  }
  const rgba = /^rgba?\(([^)]+)\)$/.exec(value);
  if (rgba) {
    const parts = rgba[1]!.split(",").map((part) => part.trim());
    const numbers = parts.slice(0, 3).map(Number);
    if (numbers.every((number) => Number.isFinite(number))) {
      const asHex = `#${numbers.map((number) => Math.max(0, Math.min(255, Math.round(number))).toString(16).padStart(2, "0")).join("")}`;
      return parts.length > 3 && Number(parts[3]) < 1 ? [value] : [value, asHex, `rgb(${numbers.join(",")})`];
    }
  }
  return [value];
}

const pxValues = (html: string): Set<number> => new Set([...html.matchAll(/(-?\d+(?:\.\d+)?)px/g)].map((match) => Number(match[1])));

// Ein Bildschirm gilt nur dann als aufgebaut, wenn seine Sektion echten Inhalt hat. Der blosse
// Name im data-Attribut beweist nichts — sonst meldet die Pruefung leere Bildschirme als fertig.
const composedScreenIds = (html: string): string[] => [...html.matchAll(/<section\b[^>]*\bdata-screen-id="([^"]*)"/gi)].map((match) => match[1]!);

function emptyScreenIds(html: string): string[] {
  const empty: string[] = [];
  for (const match of html.matchAll(/<section\b[^>]*\bdata-screen-id="([^"]*)"[^>]*>([\s\S]*?)<\/section\s*>/gi)) {
    const content = match[2]!.replace(/<!--[\s\S]*?-->/g, "").replace(/\s+/g, "");
    if (content.length < 24) empty.push(match[1]!);
  }
  return empty;
}

export function checkFidelity(html: string, facts: DesignFacts): FidelityReport {
  const issues: FidelityIssue[] = [];
  const document = html.toLowerCase();
  const flatDocument = stripSpaces(document);
  const presentColors = htmlColors(html);
  const presentPx = pxValues(html);
  let checked = 0, matched = 0;

  const record = (ok: boolean, issue: FidelityIssue) => { checked += 1; if (ok) matched += 1; else issues.push(issue); };

  if (!/<!doctype html/i.test(html.trimStart()) && !/<html[\s>]/i.test(html)) issues.push({ severity: "blocker", area: "Dokument", detail: "Es fehlt ein vollständiges HTML-Dokument ab <!doctype html>." });
  if (!/name\s*=\s*["']werft-preview["']/i.test(html)) issues.push({ severity: "blocker", area: "Geometrie", detail: 'Das Pflicht-Meta <meta name="werft-preview" content=\'{"platform":…,"width":…,"height":…,"device":…,"density":…}\'> fehlt.' });

  for (const theme of facts.themes.slice(0, 12)) {
    for (const [token, value] of Object.entries(theme.tokens).filter(([name]) => coreThemeTokens.test(name)).slice(0, 16)) {
      record(colorVariants(value).some((variant) => presentColors.has(variant)), { severity: "abweichung", area: "Farbe", detail: `Theme „${theme.id}“: ${token} = ${value} (${theme.source}) kommt im HTML nicht vor.`, source: theme.source });
    }
  }
  for (const color of facts.colors.filter((item) => item.used).slice(0, 60)) {
    record(colorVariants(color.css).some((variant) => presentColors.has(variant)), { severity: "abweichung", area: "Farbe", detail: `Am Element verwendete Quellfarbe ${color.name} = ${color.css} (${color.source}) fehlt im HTML.`, source: color.source });
  }
  for (const dimension of facts.dimensions.filter((item) => item.used).slice(0, 80)) {
    if (dimension.px <= 0 || !Number.isFinite(dimension.px)) continue;
    record(presentPx.has(dimension.px), { severity: "abweichung", area: "Maß", detail: `Quellmaß ${dimension.name} = ${dimension.px}px (${dimension.source}) fehlt im HTML.`, source: dimension.source });
  }
  for (const shape of facts.shapes.filter((item) => item.used).slice(0, 30)) {
    record(flatDocument.includes(stripSpaces(shape.radiusCss)), { severity: "abweichung", area: "Radius", detail: `Radius ${shape.name} = ${shape.radiusCss} (${shape.source}) fehlt im HTML.`, source: shape.source });
  }
  for (const type of facts.typography.filter((item) => item.used).slice(0, 30)) {
    if (type.sizePx === undefined) continue;
    record(presentPx.has(type.sizePx), { severity: "abweichung", area: "Typografie", detail: `Schriftgröße ${type.name} = ${type.sizePx}px (${type.source}) fehlt im HTML.`, source: type.source });
  }
  // Effekte sind der haeufigste Abweichungsgrund und haengen immer an einem konkreten Element.
  for (const effect of facts.effects.filter((item) => item.css !== "none").slice(0, 40)) {
    const needle = stripSpaces(effect.css.replace(/^[a-z-]+:\s*/i, ""));
    record(needle.length < 6 || flatDocument.includes(needle), { severity: "abweichung", area: "Effekt", detail: `Effekt ${effect.name} (${effect.kind}) muss exakt als „${effect.css}“ erscheinen (${effect.source}).`, source: effect.source });
  }
  const empty = new Set(emptyScreenIds(html));
  for (const screenId of composedScreenIds(html)) {
    const screen = facts.screens.find((item) => item.id === screenId);
    record(!empty.has(screenId), { severity: "blocker", area: "Screen", detail: `Der Bildschirm „${screen?.name ?? screenId}“ (${screenId}) ist leer geblieben und muss vollständig aufgebaut werden.`, ...(screen?.files[0] ? { source: screen.files[0] } : {}) });
  }
  for (const asset of facts.assets.filter((item) => item.svg).slice(0, 25)) {
    record(document.includes(asset.name.toLowerCase()) || document.includes("<svg"), { severity: "abweichung", area: "Icon", detail: `Original-Icon ${asset.name} (${asset.path}) fehlt; es darf kein Ersatzsymbol verwendet werden.`, source: asset.path });
  }

  return { issues, checked, matched, score: checked ? Math.round((matched / checked) * 1000) / 10 : 100 };
}

// Der Bericht geht als Korrekturauftrag zurueck in die KI. `sources` grenzt ihn auf die Dateien
// eines Bildschirms ein, damit Screen B nicht die Abweichungen von Screen A "reparieren" muss.
export function renderFidelityInstructions(report: FidelityReport, options: { sources?: string[]; limit?: number } = {}): string {
  const limit = options.limit ?? 60;
  const relevant = options.sources?.length
    ? report.issues.filter((issue) => !issue.source || options.sources!.some((file) => issue.source === file || issue.source!.startsWith(file) || file.startsWith(issue.source!)))
    : report.issues;
  const blockers = relevant.filter((issue) => issue.severity === "blocker");
  const deviations = relevant.filter((issue) => issue.severity === "abweichung");
  const lines = [...blockers, ...deviations].slice(0, limit).map((issue, index) => `${index + 1}. [${issue.severity}/${issue.area}] ${issue.detail}`);
  return [
    `Nachmessung: ${report.matched} von ${report.checked} geprüften Quellwerten sind exakt im HTML enthalten (${report.score} %).`,
    lines.length ? `Folgende Punkte MÜSSEN korrigiert werden — jeder Punkt ist ein gemessener Quellwert, keine Geschmacksfrage:\n${lines.join("\n")}` : "Keine messbaren Abweichungen für diesen Bildschirm.",
    relevant.length > limit ? `… ${relevant.length - limit} weitere Abweichungen derselben Art.` : ""
  ].filter(Boolean).join("\n");
}

// Betrifft den Bildschirm ueberhaupt etwas? Sonst wird der teure Korrekturlauf gespart.
export const hasIssuesForSources = (report: FidelityReport, sources: string[]): boolean =>
  report.issues.some((issue) => !issue.source || sources.some((file) => issue.source === file || issue.source!.startsWith(file) || file.startsWith(issue.source!)));

export const fidelityAcceptable = (report: FidelityReport) => !report.issues.some((issue) => issue.severity === "blocker") && report.score >= 97;
