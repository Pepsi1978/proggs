import { orderedScreens } from "./design-extract.js";
import { renderFactSheet, type DesignFacts } from "./design-facts.js";
import { buildSpecPackage, type Vorlage } from "./spec-package.js";
import { bakedThemeCss, repairThemeSelectors, screenSlug, themeStyles, themeSwitcherScript, themeSwitcherStyles, themeVariants, type ThemeVariant } from "./screen-composer.js";

// Der Download war bisher das rohe Importpaket plus EINE Design-Datei. Was das Studio deterministisch
// gemessen hat — Farben, Masse, Typografie, Radien, Effekte, Assets, Texte, Erscheinungen — blieb im
// Server zurueck, und die Erscheinungsumschaltung lebte nur in der Vorschau. Wer die ZIP oeffnete, sah
// deshalb genau eine Erscheinung. Dieses Modul baut daraus ein VOLLSTAENDIGES Uebergabepaket:
// jeder Bildschirm in JEDER Erscheinung als eigene Datei, dazu alle gemessenen Werte maschinenlesbar.

export const exportRoot = "WERFT-DESIGN/";

export type ExportFile = { path: string; content: string };
export type ExportSection = { id: string; name: string; slug: string; isStart: boolean; html: string };
export type ExportDocument = { head: string; css: string; sections: ExportSection[]; width?: number; height?: number };

const styleBlockPattern = /<style\b[^>]*>([\s\S]*?)<\/style\s*>/gi;

// Ein bewusst kleiner Schnitt entlang der Bloecke: die Bildschirme des Aufbaus sind flache
// `<section class="werft-screen">`-Elemente, ihr Inhalt darf aber eigene `<section>` enthalten.
// Deshalb wird gezaehlt statt gierig gesucht.
export function splitDesignDocument(html: string): ExportDocument {
  const sections: ExportSection[] = [];
  const opening = /<section\b[^>]*class="[^"]*\bwerft-screen\b[^"]*"[^>]*>/gi;
  let match: RegExpExecArray | null;
  while ((match = opening.exec(html))) {
    const attributes = match[0];
    const id = /data-screen-id="([^"]*)"/i.exec(attributes)?.[1] ?? `screen-${sections.length + 1}`;
    const name = /data-screen-name="([^"]*)"/i.exec(attributes)?.[1] ?? id;
    let depth = 1;
    const inner = /<section\b|<\/section\s*>/gi;
    inner.lastIndex = opening.lastIndex;
    let end = html.length;
    let token: RegExpExecArray | null;
    while ((token = inner.exec(html))) {
      depth += token[0].toLowerCase().startsWith("</") ? -1 : 1;
      if (depth === 0) { end = token.index + token[0].length; break; }
    }
    sections.push({ id, name, slug: screenSlug(name || id), isStart: /data-start="true"/i.test(attributes), html: html.slice(match.index, end) });
    opening.lastIndex = end;
  }
  const css = [...html.matchAll(styleBlockPattern)].map((block) => block[1]!).join("\n");
  const preview = /<meta\s+name="werft-preview"\s+content='([^']*)'/i.exec(html)?.[1];
  let width: number | undefined, height: number | undefined;
  if (preview) {
    try {
      const parsed = JSON.parse(preview.replace(/&#39;/g, "'")) as { width?: number; height?: number };
      if (typeof parsed.width === "number") width = parsed.width;
      if (typeof parsed.height === "number") height = parsed.height;
    } catch { /* Ohne lesbare Geometrie bleibt die Breite offen — der Bildschirm bringt sie selbst mit. */ }
  }
  return { head: html.slice(0, html.indexOf("</head>") + 7), css, sections, ...(width ? { width } : {}), ...(height ? { height } : {}) };
}

// `:root`- und `html`-Regeln sind die Theme-Definitionen selbst. Fuer die Farbabbildung zaehlen nur die
// Bildschirm-Regeln — sonst wuerde die eingebaute Erscheinung aus ihrer eigenen Definition „gemessen".
export function screenScopedCss(css: string): string {
  const parts: string[] = [];
  let index = 0;
  while (index < css.length) {
    const open = css.indexOf("{", index);
    if (open < 0) break;
    const selector = css.slice(index, open).trim();
    let depth = 1, cursor = open + 1;
    while (cursor < css.length && depth > 0) {
      if (css[cursor] === "{") depth += 1;
      else if (css[cursor] === "}") depth -= 1;
      cursor += 1;
    }
    const body = css.slice(open + 1, cursor - 1);
    if (/^@(?:media|supports|layer|container)/i.test(selector)) {
      const kept = screenScopedCss(body);
      if (kept.trim()) parts.push(`${selector} {\n${kept}\n}`);
    } else if (selector && !/^(?::root|html)\b/i.test(selector)) parts.push(`${selector} {${body}}`);
    index = cursor;
  }
  return parts.join("\n");
}

const themesMeta = (variants: ThemeVariant[]) =>
  `<meta name="werft-themes" content='${JSON.stringify(variants.map(({ id, name, kind, color }) => ({ id, name, kind, color }))).replace(/'/g, "&#39;")}'>`;

// Ein Design, das VOR dieser Fassung aufgebaut wurde, traegt weder Umschalter noch Farbabbildung.
// Statt einen teuren Neuaufbau zu verlangen, wird beides beim Export nachgeruestet — deterministisch.
export function ensureThemeControls(html: string, variants: ThemeVariant[], facts?: DesignFacts): string {
  if (variants.length < 2) return html;
  let result = html.replace(styleBlockPattern, (all, css: string) => all.replace(css, repairThemeSelectors(css, variants)));
  const additions: string[] = [];
  // Fehlt der Variablenblock einer Erscheinung, kann sie nie greifen — dann wird er ergaenzt.
  if (facts && !variants.every((variant) => result.includes(`[data-werft-theme="${variant.id}"]`))) additions.push(themeStyles(facts));
  const baked = bakedThemeCss(screenScopedCss([...result.matchAll(styleBlockPattern)].map((block) => block[1]!).join("\n")), variants);
  if (baked.trim()) additions.push(`/* Farbabbildung: dieselben Regeln in der jeweils anderen Erscheinung. */\n${baked}`);
  if (!result.includes("werft-theme-switcher")) additions.push(themeSwitcherStyles);
  if (additions.length) result = insertBeforeHeadEnd(result, `<style>\n${additions.join("\n")}\n</style>`);
  if (!/name="werft-themes"/i.test(result)) result = insertBeforeHeadEnd(result, themesMeta(variants));
  if (!result.includes("werft-theme-switcher-script")) result = result.replace(/<\/body\s*>/i, `<script data-werft="werft-theme-switcher-script">\n${themeSwitcherScript}\n</script>\n</body>`);
  return result;
}

function insertBeforeHeadEnd(html: string, snippet: string): string {
  return /<\/head\s*>/i.test(html) ? html.replace(/<\/head\s*>/i, `${snippet}\n</head>`) : `${snippet}\n${html}`;
}

const escapeHtml = (value: string) => value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const pad = (value: number, total: number) => String(value).padStart(String(total).length, "0");

// Der Kern der Beschwerde: „nur der Dunkelmodus wurde heruntergeladen". Ab hier ist jeder Bildschirm
// in JEDER Erscheinung eine eigene, fuer sich geoeffnete Datei — nichts muss mehr umgeschaltet werden.
export function screenDocuments(document: ExportDocument, variants: ThemeVariant[], title: string): ExportFile[] {
  if (!document.sections.length) return [];
  const files: ExportFile[] = [{ path: `${exportRoot}bildschirme/design.css`, content: document.css }];
  const appearances = variants.length ? variants : [{ id: "standard", name: "Standard", kind: "other" as const, color: "#ffffff", tokens: {} }];
  for (const variant of appearances) {
    for (const [index, section] of document.sections.entries()) {
      const scheme = variant.kind === "dark" ? "dark" : "light";
      files.push({
        path: `${exportRoot}bildschirme/${variant.id}/${pad(index + 1, document.sections.length)}-${section.slug}.html`,
        content: `<!doctype html>
<html lang="de" data-werft-theme="${escapeHtml(variant.id)}" data-theme="${scheme}" style="color-scheme: ${scheme};">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escapeHtml(title)} — ${escapeHtml(section.name)} — ${escapeHtml(variant.name)}</title>
<link rel="stylesheet" href="../design.css">
<style>
*, *::before, *::after { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
.werft-screen { display: block !important; position: relative; overflow: hidden; }
</style>
</head>
<body>
<div class="werft-screens"${document.width ? ` style="width: ${document.width}px;"` : ""}>
${section.html}
</div>
</body>
</html>`
      });
    }
  }
  return files;
}

export type PackageInput = {
  projectName: string;
  platform: string;
  facts: DesignFacts;
  designs: Array<{ path: string; html: string; label: string }>;
  entryPath: string;
  sourceFiles: Array<{ path: string; size: number }>;
  // Das Erst-Spec aus `Designs/Inbox/<App>-SPEC-v1.zip`, sofern das Projekt daraus entstanden ist.
  // Ohne diese Vorlage kann der Export nicht wissen, welche Bedienelemente NEU sind.
  vorlage?: Vorlage;
  stand?: string;
  // Die Plattform, FUER die gebaut werden soll. Sie ist nicht zwingend die, aus der gemessen wurde:
  // ein aus Android-Quellen aufgebautes Design kann als Windows-Spec heruntergeladen werden. Ohne
  // diese Trennung uebersetzt der Spec-Schreiber in die falsche Richtung.
  zielPlattform?: string;
};

export type PackageReport = { bildschirmeImDesign: number; bildschirmeExportiert: number; erscheinungen: number; dateienJeBildschirm: number; nichtAufgebaut: string[] };

export function buildExportPackage(input: PackageInput): { files: ExportFile[]; report: PackageReport; documents: Array<{ path: string; html: string }> } {
  const variants = themeVariants(input.facts.themes);
  const prepared = input.designs.map((design) => ({ ...design, html: ensureThemeControls(design.html, variants, input.facts) }));
  const base = prepared.find((design) => design.path === input.entryPath) ?? prepared[0];
  const document = base ? splitDesignDocument(base.html) : { head: "", css: "", sections: [] as ExportSection[] };
  const files: ExportFile[] = [];
  for (const design of prepared) files.push({ path: `${exportRoot}${design.path === base?.path ? "design.html" : design.label}`, content: design.html });
  files.push(...screenDocuments(document, variants, input.projectName));
  // Ein Bildschirm, der gemessen wurde, aber nicht im Dokument steht, fehlt dem Umsetzer. Das wird
  // BENANNT statt stillschweigend hingenommen — sonst gilt ein halbes Design als vollstaendig.
  const built = new Set(document.sections.flatMap((section) => [section.id, section.name]));
  const measured = orderedScreens(input.facts);
  const missing = measured.filter((screen) => !built.has(screen.id) && !built.has(screen.name)).map((screen) => screen.name);
  const report: PackageReport = {
    bildschirmeImDesign: Math.max(document.sections.length, measured.length),
    bildschirmeExportiert: document.sections.length,
    erscheinungen: variants.length,
    dateienJeBildschirm: Math.max(1, variants.length),
    nichtAufgebaut: missing
  };
  files.push({ path: `${exportRoot}design-tokens.json`, content: designTokens(input, variants, document, report) });
  files.push({ path: `${exportRoot}DESIGN-SPEC.md`, content: designSpec(input, variants, document, report) });
  files.push({ path: `${exportRoot}LIESMICH.md`, content: readme(input, variants, document, report) });
  // Die drei Specs liegen bewusst im ZIP-WURZELVERZEICHNIS, nicht unter WERFT-DESIGN/: der
  // Rueckimport erwartet sie dort, und sie beschreiben das Programm, nicht nur das Design.
  files.push(...buildSpecPackage({
    projectName: input.projectName, platform: input.zielPlattform ?? input.platform, facts: input.facts,
    variants, document, report, ...(input.vorlage ? { vorlage: input.vorlage } : {})
  }, input.stand ?? new Date().toISOString().slice(0, 10)));
  return { files, report, documents: prepared.map((design) => ({ path: design.path, html: design.html })) };
}

function designTokens(input: PackageInput, variants: ThemeVariant[], document: ExportDocument, report: PackageReport): string {
  const facts = input.facts;
  const screensById = new Map(facts.screens.map((screen) => [screen.id, screen] as const));
  return `${JSON.stringify({
    erzeugtVon: "Werft Studio",
    projekt: input.projectName,
    plattform: input.platform,
    geometrie: facts.viewport ?? (document.width && document.height ? { width: document.width, height: document.height, device: "—", density: 1, source: "design.html" } : undefined),
    vollstaendigkeit: report,
    erscheinungen: variants.map((variant) => ({ id: variant.id, name: variant.name, art: variant.kind, vorschaufarbe: variant.color, tokens: variant.tokens })),
    bildschirme: document.sections.map((section, index) => ({
      nummer: index + 1,
      id: section.id,
      name: section.name,
      istStart: section.isStart,
      navigiertZu: screensById.get(section.id)?.navigatesTo ?? [],
      quelle: screensById.get(section.id)?.source ?? "",
      quelldateien: screensById.get(section.id)?.files ?? [],
      dateien: Object.fromEntries(variants.map((variant) => [variant.id, `bildschirme/${variant.id}/${pad(index + 1, document.sections.length)}-${section.slug}.html`]))
    })),
    farben: facts.colors.map((color) => ({ name: color.name, css: color.css, quelle: color.source, verwendet: color.used === true })),
    masse: facts.dimensions.map((dimension) => ({ name: dimension.name, px: dimension.px, original: dimension.raw, quelle: dimension.source, verwendet: dimension.used === true })),
    typografie: facts.typography.map((type) => ({ name: type.name, schriftart: type.family, groessePx: type.sizePx, staerke: type.weight, zeilenhoehePx: type.lineHeightPx, laufweitePx: type.letterSpacingPx, quelle: type.source })),
    formen: facts.shapes.map((shape) => ({ name: shape.name, radius: shape.radiusCss, quelle: shape.source })),
    effekte: facts.effects.map((effect) => ({ name: effect.name, art: effect.kind, css: effect.css, quelle: effect.source })),
    assets: facts.assets.map((asset) => ({ name: asset.name, art: asset.kind, pfad: asset.path, css: asset.css, svg: asset.svg })),
    texte: facts.strings,
    dateien: input.sourceFiles.map((file) => ({ pfad: file.path, bytes: file.size })),
    hinweise: facts.notes
  }, null, 2)}\n`;
}

function designSpec(input: PackageInput, variants: ThemeVariant[], document: ExportDocument, report: PackageReport): string {
  return [
    `# Design-Spezifikation — ${input.projectName}`,
    "",
    "Alle Werte in dieser Datei sind **deterministisch aus den Projektquellen gemessen**, nicht geschätzt.",
    "Sie sind für die Umsetzung verbindlich.",
    "",
    `- Plattform: ${input.platform}`,
    `- Bildschirme im Design: ${report.bildschirmeExportiert}`,
    `- Erscheinungen: ${variants.length ? variants.map((variant) => `${variant.name} (\`${variant.id}\`, ${variant.kind})`).join(", ") : "eine"}`,
    document.width && document.height ? `- Quellgeometrie: ${document.width}×${document.height} px` : "",
    report.nichtAufgebaut.length ? `- **Nicht aufgebaut (Deckel erreicht):** ${report.nichtAufgebaut.join(", ")}` : "",
    "",
    renderFactSheet(input.facts),
    "",
    "## Bildschirme und ihre Dateien",
    "",
    "| Nr. | Bildschirm | Start | Dateien je Erscheinung |",
    "|-----|------------|-------|------------------------|",
    ...document.sections.map((section, index) => `| ${index + 1} | ${section.name} (\`${section.id}\`) | ${section.isStart ? "ja" : "—"} | ${variants.map((variant) => `\`bildschirme/${variant.id}/${pad(index + 1, document.sections.length)}-${section.slug}.html\``).join("<br>") || "`design.html`"} |`)
  ].filter((line) => line !== "").join("\n") + "\n";
}

function readme(input: PackageInput, variants: ThemeVariant[], document: ExportDocument, report: PackageReport): string {
  return `# Werft-Studio-Designpaket — ${input.projectName}

Dieses Paket enthält das **vollständige** Design: jeden Bildschirm in **jeder** Erscheinung,
dazu alle gemessenen Farben, Maße, Schriften, Radien, Effekte, Assets und Texte.

## Inhalt

| Pfad | Inhalt |
|------|--------|
| \`design.html\` | Das durchklickbare Gesamtdesign — alle ${report.bildschirmeExportiert} Bildschirme, mit Umschalter für Bildschirm **und** Erscheinung (oben rechts). |
| \`bildschirme/<erscheinung>/<nr>-<name>.html\` | Jeder Bildschirm einzeln, fest in dieser Erscheinung. ${report.bildschirmeExportiert} Bildschirme × ${Math.max(1, variants.length)} Erscheinungen = ${report.bildschirmeExportiert * Math.max(1, variants.length)} Dateien. |
| \`bildschirme/design.css\` | Das gemeinsame Stylesheet aller Bildschirme. |
| \`design-tokens.json\` | Alle gemessenen Werte maschinenlesbar (Erscheinungen mit vollständigen Token-Tabellen, Bildschirme, Farben, Maße, Typografie, Formen, Effekte, Assets, Texte). |
| \`DESIGN-SPEC.md\` | Dieselben Werte als lesbare Spezifikation inklusive Bildschirm-Tabelle. |
| Übriger ZIP-Inhalt | Das unveränderte Originalprojekt mit allen Begleitdateien (Bilder, Fonts, Audio, Daten). |

## Erscheinungen

${variants.length ? variants.map((variant) => `- **${variant.name}** — \`${variant.id}\` (${variant.kind}), ${Object.keys(variant.tokens).length} Farbtoken`).join("\n") : "- Dieses Design bringt nur eine Erscheinung mit."}

## Für den Design-Umsetzer

1. \`design-tokens.json\` ist die **verbindliche Quelle** für alle Werte — nichts daraus schätzen oder runden.
2. \`bildschirme/<erscheinung>/\` zeigt, wie **jeder** Bildschirm in **jeder** Erscheinung aussehen muss.
3. \`design.html\` zeigt den Klickweg: \`data-werft-navigate="<ziel-id>"\` ist die Navigation.
4. Alle Erscheinungen werden als umschaltbare Themes umgesetzt, nicht nur die zuerst sichtbare.
5. Vollständig ist die Umsetzung erst, wenn jeder Bildschirm aus der Tabelle in \`DESIGN-SPEC.md\` im Code nachweisbar ist.
${report.nichtAufgebaut.length ? `\n> **Achtung:** ${report.nichtAufgebaut.length} erkannte Bildschirme wurden beim Aufbau nicht erzeugt: ${report.nichtAufgebaut.join(", ")}. Sie fehlen in diesem Paket.\n` : ""}`;
}
