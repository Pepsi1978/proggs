import { bauplanFuer } from "./bauplan.js";
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
export type ExportDocument = { head: string; css: string; sections: ExportSection[]; width?: number; height?: number; schriftUrls?: string[] };

const styleBlockPattern = /<style\b[^>]*>([\s\S]*?)<\/style\s*>/gi;

/**
 * Zieht alle `@import`-Regeln an den ANFANG eines Stylesheets.
 *
 * Der Grund ist ein Fehler, der ein ganzes Design entstellt hat, ohne irgendwo aufzufallen:
 * Ein Entwurf holt seine Schriften per `@import url("https://fonts.googleapis.com/…")`. Nach der
 * CSS-Kaskaden-Spezifikation ist eine `@import`-Regel aber NUR gueltig, wenn sie VOR allen anderen
 * Regeln steht — erlaubt davor sind allein `@charset` und `@layer`-Anweisungen. Steht eine einzige
 * Stilregel davor, verwirft jeder Browser den Import STILL: keine Warnung, keine Fehlermeldung,
 * kein Eintrag in der Konsole. Die Folge ist, dass die Schrift des Entwurfs nie laedt, alles in der
 * Systemschrift erscheint und der Umsetzer die App mit der falschen Schrift nachbaut. Schrift ist
 * das Auffaelligste an einem Design — genau sie ging damit verloren.
 *
 * Real getroffen: ein Design mit Fraunces, Inter und JetBrains Mono. Der `@import` stand an
 * Zeichen 6427 seines Style-Blocks, 6425 Zeichen Regeln davor. Keine der drei Schriften kam an —
 * weder in der Studio-Vorschau noch in einer der exportierten Dateien.
 *
 * Hier wird nichts entfernt und nichts umgeschrieben: die Importe werden nur nach VORN gezogen, in
 * ihrer urspruenglichen Reihenfolge. Danach sind sie gueltig, und die Kaskade der uebrigen Regeln
 * bleibt unberuehrt — eine `@import`-Regel nimmt an der Kaskade ohnehin nicht als Stilregel teil.
 */
export function importeVoranstellen(css: string): { css: string; importe: string[] } {
  const importe: string[] = [];
  const rest = css.replace(importRulePattern, (regel) => { importe.push(regel.trim()); return ""; });
  if (!importe.length) return { css, importe };
  return { css: `${importe.join("\n")}\n${rest.replace(/^[ \t]*\r?\n/, "")}`, importe };
}

// Die URL eines Imports enthaelt selbst Semikolons (`family=Inter:wght@400;500;600`) — ein Muster,
// das bis zum ersten `;` liest, wuerde sie mitten entzweischneiden und einen kaputten Import
// hinterlassen. Deshalb wird zuerst die Klammer bzw. die Zeichenkette geschlossen und erst danach
// das abschliessende Semikolon gesucht.
const importRulePattern = /@import\s+(?:url\(\s*(?:"[^"]*"|'[^']*'|[^)]*)\s*\)|"[^"]*"|'[^']*')[^;]*;/gi;

/**
 * Reparierte Style-Bloecke im Dokument selbst — damit auch `design.html` und die Studio-Vorschau
 * ihre Schriften laden, nicht nur die exportierten Einzeldateien. Jeder `<style>`-Block ist ein
 * eigenes Stylesheet und wird deshalb einzeln behandelt.
 */
export function styleBloeckeReparieren(html: string): string {
  return html.replace(styleBlockPattern, (all, css: string) => {
    const repariert = importeVoranstellen(css).css;
    // Ersatz als Funktion: ein `$` im CSS (etwa in `content: "$"`) wuerde sonst als
    // Ersetzungsmuster gedeutet und die Datei stillschweigend verstuemmeln.
    return repariert === css ? all : all.replace(css, () => repariert);
  });
}

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
  // Die Style-Bloecke werden zu EINER `design.css` zusammengefuehrt. Dabei rutscht ein `@import`
  // fast zwangslaeufig hinter eine andere Regel und ist damit laut Spezifikation ungueltig —
  // deshalb wird er hier nach vorn gezogen (Sicherheitsnetz; der eigentliche Weg sind die
  // `<link>`-Verweise im Kopf jeder Bildschirmdatei).
  const css = importeVoranstellen([...html.matchAll(styleBlockPattern)].map((block) => block[1]!).join("\n")).css;
  // Die Schriftverweise stehen mal als `@import` im Stil, mal als `<link>` im Kopf — beide Wege
  // werden gelesen, damit keine Schrift verloren geht.
  const gefundeneSchriften = schriftUrls(html);
  const preview = /<meta\s+name="werft-preview"\s+content='([^']*)'/i.exec(html)?.[1];
  let width: number | undefined, height: number | undefined;
  if (preview) {
    try {
      const parsed = JSON.parse(preview.replace(/&#39;/g, "'")) as { width?: number; height?: number };
      if (typeof parsed.width === "number") width = parsed.width;
      if (typeof parsed.height === "number") height = parsed.height;
    } catch { /* Ohne lesbare Geometrie bleibt die Breite offen — der Bildschirm bringt sie selbst mit. */ }
  }
  return { head: html.slice(0, html.indexOf("</head>") + 7), css, sections, ...(width ? { width } : {}), ...(height ? { height } : {}), ...(gefundeneSchriften.length ? { schriftUrls: gefundeneSchriften } : {}) };
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

/**
 * Loest die BREITENBEZOGENEN Media Queries auf eine feste Zielbreite auf.
 *
 * Der Grund ist der Fehler, der die ganze Kette verdorben hat: Media Queries fragen die
 * FENSTERbreite, nicht die Breite des Bildschirms. Die Vorschau im Studio steckt in einem
 * Geraeterahmen — dort greift `@media (max-width: 480px)`. Die exportierte Einzeldatei hat
 * keinen Rahmen: im Browserfenster des Betrachters (und im Messfuehler) greift dieselbe Regel
 * NICHT, und der Bildschirm sieht anders aus als im Studio. Genau so ist ein Einstellungs-
 * bildschirm mit „Beschriftung ueber dem Feld" als „Beschriftung neben dem Feld" im Code
 * gelandet — ohne dass irgendwo ein Fehler auftauchte.
 *
 * Hier werden deshalb alle breitenbezogenen Bedingungen bei der Zielbreite ausgewertet:
 * was zutrifft, wird als flache Regel uebernommen (in Original-Reihenfolge, damit die Kaskade
 * erhalten bleibt); was nicht zutrifft, entfaellt. Bedingungen, die NICHT an der Breite haengen
 * (`prefers-color-scheme`, `prefers-reduced-motion`, `hover`, `print`), bleiben unangetastet —
 * sie sind im Original richtig und muessen beim Betrachter weiter greifen.
 *
 * `design.css` bleibt vollstaendig erhalten. Das Ergebnis wird als zusaetzlicher Style-Block
 * NACH ihr eingebunden: verlustfrei, und die richtige Fassung gewinnt.
 */
export function breitenRegelnFuer(css: string, breite: number): string {
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
    index = cursor;
    if (!/^@media\b/i.test(selector)) continue;

    // Nur Bedingungen anfassen, die ueberhaupt an der Breite haengen.
    const bedingungen = [...selector.matchAll(/\((min|max)-width:\s*([\d.]+)(px|em|rem)\)/gi)];
    if (!bedingungen.length) continue;

    const trifftZu = bedingungen.every(([, art, wert, einheit]) => {
      // `em`/`rem` in Media Queries beziehen sich immer auf die Grundschrift (16px), nie auf
      // eine geerbte Schriftgroesse — das ist in der Spezifikation so festgelegt.
      const px = einheit!.toLowerCase() === "px" ? Number(wert) : Number(wert) * 16;
      return art!.toLowerCase() === "max" ? breite <= px : breite >= px;
    });
    if (!trifftZu) continue;

    // Bleibt neben der Breite noch eine andere Bedingung (z. B. `and (hover: hover)`), muss die
    // beim Betrachter weiter geprueft werden — dann nur die Breiten-Bedingung herausnehmen.
    const restlich = selector
      .replace(/^@media\s*/i, "")
      .replace(/\((min|max)-width:\s*[\d.]+(px|em|rem)\)/gi, "")
      .replace(/\band\b/gi, " ")
      .replace(/\s+/g, " ")
      .trim();
    const innen = /@media|@supports|@container/i.test(body) ? breitenRegelnFuer(body, breite) : body;
    if (!innen.trim()) continue;
    parts.push(restlich ? `@media ${restlich} {\n${innen}\n}` : innen);
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

/**
 * Die Schriften des Entwurfs — als VERWEIS im Dokumentkopf, nicht als `@import` im Stylesheet.
 *
 * So macht es auch Claude Design in seinem Uebergabepaket (nachgeprueft an einem echten
 * `.dc.html`-Buendel): im Kopf stehen `<link rel="preconnect">` auf die Schriftauslieferung und
 * danach ein `<link rel="stylesheet">` auf das Schriftverzeichnis. Kein `@import` irgendwo.
 * Das ist auch die Empfehlung von web.dev — ein `@import` blockiert das Rendern zusaetzlich,
 * weil der Browser erst das aeussere Stylesheet laden muss, um von der Schrift zu erfahren.
 *
 * Fuer den Export ist der Kopf-Verweis ausserdem die einzige ROBUSTE Stelle: sobald mehrere
 * Style-Bloecke zu einer `design.css` zusammengefuehrt werden, rutscht ein `@import` fast
 * zwangslaeufig hinter eine andere Regel — und ist dann laut Spezifikation ungueltig.
 */
export type Schriftverweis = { familie: string; quelle: "verzeichnis" | "eingebettet" | "system"; url?: string; gewichte?: string[] };

const fontVerzeichnisse = /fonts\.googleapis\.com|fonts\.bunny\.net|use\.typekit\.net|fonts\.gstatic\.com/i;

// Die generischen Familien der CSS-Spezifikation plus die verbreiteten Systemschrift-Kuerzel.
// Sie bezeichnen „nimm, was das System hat" — dafuer muss nichts beschafft werden.
const generischeFamilien = /^(?:serif|sans-serif|monospace|cursive|fantasy|system-ui|ui-serif|ui-sans-serif|ui-monospace|ui-rounded|math|emoji|fangsong|-apple-system|blinkmacsystemfont|segoe ui|roboto|helvetica|helvetica neue|arial)$/i;

/** Die URLs, die ein Schriftverzeichnis ansprechen — aus `@import`-Regeln und `<link>`-Tags. */
export function schriftUrls(quelltext: string): string[] {
  const urls = new Set<string>();
  for (const regel of quelltext.match(importRulePattern) ?? []) {
    const url = /url\(\s*(?:"([^"]*)"|'([^']*)'|([^)]*))\s*\)|"([^"]*)"|'([^']*)'/.exec(regel);
    const wert = (url?.[1] ?? url?.[2] ?? url?.[3] ?? url?.[4] ?? url?.[5] ?? "").trim();
    if (wert && fontVerzeichnisse.test(wert)) urls.add(wert);
  }
  for (const tag of quelltext.match(/<link\b[^>]*>/gi) ?? []) {
    const href = /href\s*=\s*(?:"([^"]*)"|'([^']*)')/i.exec(tag);
    const wert = (href?.[1] ?? href?.[2] ?? "").trim();
    if (wert && fontVerzeichnisse.test(wert) && /stylesheet/i.test(tag)) urls.add(wert);
  }
  return [...urls];
}

/**
 * Welche Schriftfamilien der Entwurf benutzt und WOHER jede kommt.
 *
 * Das ist der vierte Bestandteil eines vollstaendigen Uebergabepakets: die Asset-Verweise. Ohne
 * ihn weiss der Umsetzer nicht, dass er auf Android `Fraunces` als Downloadable Font einrichten
 * oder mitliefern muss — er baut mit der Systemschrift, und das Ergebnis sieht sofort anders aus.
 * Eine Familie ohne Quelle wird deshalb ausdruecklich BENANNT statt stillschweigend hingenommen.
 */
export function schriftverweise(css: string, quelltext: string, facts?: DesignFacts): Schriftverweis[] {
  const urls = schriftUrls(`${css}\n${quelltext}`);
  // Aus `family=Fraunces:wght@400;600&family=Inter:wght@400;500` die Familien und ihre Gewichte.
  const ausVerzeichnis = new Map<string, { url: string; gewichte: string[] }>();
  for (const url of urls) {
    for (const treffer of url.matchAll(/family=([^:&]+)(?::([^&]*))?/gi)) {
      const familie = decodeURIComponent(treffer[1]!.replace(/\+/g, " ")).trim();
      const gewichte = [...(treffer[2] ?? "").matchAll(/(\d{3})/g)].map((m) => m[1]!);
      if (familie) ausVerzeichnis.set(familie.toLowerCase(), { url, gewichte: [...new Set(gewichte)] });
    }
  }
  // Eingebettete Schriften: `@font-face { font-family: "X" }`.
  const eingebettet = new Set<string>();
  for (const block of css.match(/@font-face\s*\{[^}]*\}/gi) ?? []) {
    const name = /font-family\s*:\s*(?:"([^"]*)"|'([^']*)'|([^;}]+))/i.exec(block);
    const wert = (name?.[1] ?? name?.[2] ?? name?.[3] ?? "").trim();
    if (wert) eingebettet.add(wert.toLowerCase());
  }
  // Alle benutzten Familien: aus den gemessenen Fakten UND direkt aus dem Quelltext, damit auch
  // eine nur inline gesetzte `font-family` erfasst wird.
  const benutzt = new Map<string, string>();
  const merke = (liste: string) => {
    const erste = liste.split(",")[0]?.trim().replace(/^["']|["']$/g, "");
    if (!erste) return;
    if (/^(?:inherit|initial|unset|revert|currentcolor)$/i.test(erste)) return;
    // Generische Familien sind KEINE fehlende Schrift — sie zu melden wuerde die Warnung
    // entwerten, und eine Warnung, die immer kommt, liest irgendwann niemand mehr.
    if (generischeFamilien.test(erste)) return;
    benutzt.set(erste.toLowerCase(), erste);
  };
  for (const type of facts?.typography ?? []) if (type.family) merke(type.family);
  // Der Wert kann direkt mit einem Anfuehrungszeichen beginnen (`font-family:"JetBrains Mono"`) —
  // ein Muster, das erst ein anfuehrungszeichenfreies Zeichen verlangt, uebersieht genau diese.
  for (const treffer of `${css}\n${quelltext}`.matchAll(/font-family\s*:\s*([^;}]+)/gi)) merke(treffer[1]!);

  return [...benutzt.entries()].map(([schluessel, familie]) => {
    const verzeichnis = ausVerzeichnis.get(schluessel);
    if (verzeichnis) return { familie, quelle: "verzeichnis" as const, url: verzeichnis.url, ...(verzeichnis.gewichte.length ? { gewichte: verzeichnis.gewichte } : {}) };
    if (eingebettet.has(schluessel)) return { familie, quelle: "eingebettet" as const };
    return { familie, quelle: "system" as const };
  });
}

/**
 * Der Kopf-Block fuer die Schriften einer exportierten Bildschirmdatei — 1:1 nach dem Vorbild des
 * echten Claude-Design-Buendels: erst `preconnect`, dann `stylesheet`.
 */
export function schriftKopf(urls: string[]): string {
  if (!urls.length) return "";
  const hosts = new Set<string>();
  for (const url of urls) { try { hosts.add(new URL(url).origin); } catch { /* relative Angabe braucht kein preconnect */ } }
  // `fonts.googleapis.com` liefert das Verzeichnis, `fonts.gstatic.com` die Schriftdateien selbst —
  // ohne den zweiten preconnect wartet der Browser die Verbindung erst beim Nachladen ab.
  if ([...hosts].some((host) => /fonts\.googleapis\.com/i.test(host))) hosts.add("https://fonts.gstatic.com");
  return [
    ...[...hosts].map((host) => `<link rel="preconnect" href="${escapeHtml(host)}"${/gstatic/i.test(host) ? ' crossorigin="anonymous"' : ""}>`),
    ...urls.map((url) => `<link rel="stylesheet" href="${escapeHtml(url)}">`)
  ].join("\n");
}

const escapeHtml = (value: string) => value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const pad = (value: number, total: number) => String(value).padStart(String(total).length, "0");

// Der Kern der Beschwerde: „nur der Dunkelmodus wurde heruntergeladen". Ab hier ist jeder Bildschirm
// in JEDER Erscheinung eine eigene, fuer sich geoeffnete Datei — nichts muss mehr umgeschaltet werden.
export function screenDocuments(document: ExportDocument, variants: ThemeVariant[], title: string): ExportFile[] {
  if (!document.sections.length) return [];
  const files: ExportFile[] = [{ path: `${exportRoot}bildschirme/design.css`, content: document.css }];
  const appearances = variants.length ? variants : [{ id: "standard", name: "Standard", kind: "other" as const, color: "#ffffff", tokens: {} }];
  // Die Breite, FUER die entworfen wurde. Sie entscheidet, welche Media Queries gelten — und sie
  // muss in der Datei stehen, damit Betrachter und Messfuehler nicht raten muessen.
  const breite = document.width;
  const aufgeloest = breite ? breitenRegelnFuer(document.css, breite) : "";
  // Die Schriftverweise gehoeren in den KOPF jeder Datei — sonst rendert der Bildschirm in der
  // Systemschrift, und der Umsetzer baut die App nach einer Vorlage, die er nie richtig gesehen hat.
  const schriften = schriftKopf(document.schriftUrls ?? []);

  // Die CSS, die fuer die Zielbreite wirklich gilt — Grundlage fuer die Anordnung im Bauplan.
  const cssFuerBreite = breite ? `${document.css}\n${aufgeloest}` : document.css;

  for (const variant of appearances) {
    for (const [index, section] of document.sections.entries()) {
      const scheme = variant.kind === "dark" ? "dark" : "light";
      const nummer = pad(index + 1, document.sections.length);

      // Der Bauplan: die Layout-HIERARCHIE dieses Bildschirms. Er ist die verbindliche Vorlage —
      // eine Hierarchie gilt bei jeder Breite, eine Koordinate nur bei der gemessenen.
      files.push({
        path: `${exportRoot}bauplan/${variant.id}/${nummer}-${section.slug}.json`,
        content: `${JSON.stringify(
          bauplanFuer({
            kennung: section.id,
            name: section.name,
            html: section.html,
            css: cssFuerBreite,
            ...(breite ? { breite } : {}),
            erscheinung: variant.id,
          }),
          null,
          2,
        )}\n`,
      });

      files.push({
        path: `${exportRoot}bildschirme/${variant.id}/${nummer}-${section.slug}.html`,
        content: `<!doctype html>
<html lang="de" data-werft-theme="${escapeHtml(variant.id)}" data-theme="${scheme}" style="color-scheme: ${scheme};">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
${breite ? `<meta name="werft-render-width" content="${breite}">\n` : ""}<title>${escapeHtml(title)} — ${escapeHtml(section.name)} — ${escapeHtml(variant.name)}</title>
${schriften ? `<!--
  Die Schriften des Entwurfs — als Kopf-Verweis, nicht als @import im Stil. Ein @import ist laut
  CSS-Spezifikation nur gueltig, wenn er VOR allen anderen Regeln steht; beim Zusammenfuehren der
  Style-Bloecke zu design.css rutscht er dahinter und wird von jedem Browser STILL verworfen.
  Genau so ist ein Entwurf mit drei eigenen Schriften komplett in der Systemschrift gerendert
  worden, ohne dass irgendwo ein Fehler auftauchte.
-->
${schriften}\n` : ""}<link rel="stylesheet" href="../design.css">
<style>
*, *::before, *::after { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
.werft-screen { display: block !important; position: relative; overflow: hidden; }
</style>
${aufgeloest ? `<!--
  Die breitenbezogenen Media Queries aus design.css, ausgewertet fuer ${breite} dp — die Breite,
  fuer die dieser Bildschirm entworfen wurde. Ohne diesen Block haengt das Aussehen am Fenster
  des Betrachters: eine Regel wie "max-width: 480px" greift im Studio-Geraeterahmen, im breiten
  Browserfenster aber nicht — und die Datei zeigt eine Anordnung, die es im Entwurf nie gab.
  design.css bleibt unveraendert; hier gewinnt nur, was bei ${breite} dp wirklich gilt.
-->
<style data-werft-aufgeloest="${breite}">
${aufgeloest}
</style>
` : ""}</head>
<body>
<div class="werft-screens"${breite ? ` style="width: ${breite}px;"` : ""}>
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

export type PackageReport = {
  bildschirmeImDesign: number; bildschirmeExportiert: number; erscheinungen: number;
  dateienJeBildschirm: number; nichtAufgebaut: string[];
  /** Schriftfamilien, die der Entwurf benutzt, fuer die aber keine Quelle im Paket steht. */
  schriftenOhneQuelle?: string[];
};

export function buildExportPackage(input: PackageInput): { files: ExportFile[]; report: PackageReport; documents: Array<{ path: string; html: string }> } {
  const variants = themeVariants(input.facts.themes);
  // `styleBloeckeReparieren` zieht ungueltig positionierte `@import`-Regeln nach vorn — damit auch
  // `design.html` selbst und die Studio-Vorschau ihre Schriften laden, nicht nur die Einzeldateien.
  const prepared = input.designs.map((design) => ({ ...design, html: ensureThemeControls(styleBloeckeReparieren(design.html), variants, input.facts) }));
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
  // Die Schriften sind der vierte Bestandteil des Uebergabepakets. Eine Familie ohne Quelle wird
  // BENANNT — sonst baut der Umsetzer stillschweigend mit der Systemschrift weiter.
  const schriften = schriftverweise(document.css, base?.html ?? "", input.facts);
  const ohneQuelle = schriften.filter((schrift) => schrift.quelle === "system").map((schrift) => schrift.familie);
  const report: PackageReport = {
    bildschirmeImDesign: Math.max(document.sections.length, measured.length),
    bildschirmeExportiert: document.sections.length,
    erscheinungen: variants.length,
    dateienJeBildschirm: Math.max(1, variants.length),
    nichtAufgebaut: missing,
    ...(ohneQuelle.length ? { schriftenOhneQuelle: ohneQuelle } : {})
  };
  files.push({ path: `${exportRoot}design-tokens.json`, content: designTokens(input, variants, document, report, schriften) });
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

function designTokens(input: PackageInput, variants: ThemeVariant[], document: ExportDocument, report: PackageReport, schriften: Schriftverweis[] = []): string {
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
    // Die Schrift-Verweise: welche Familie der Entwurf benutzt und woher sie kommt. `system` heisst,
    // dass das Paket keine Quelle mitbringt — dann MUSS der Umsetzer sie beschaffen (auf Android
    // z. B. als Downloadable Font), sonst sieht die App sofort anders aus als der Entwurf.
    schriften,
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
| \`bauplan/<erscheinung>/<nr>-<name>.json\` | **Die Layout-Hierarchie jedes Bildschirms — die verbindliche Bauvorlage.** Sagt je Bauteil, wie es seine Kinder anordnet (Spalte, Zeile, Raster, Abstand, Ausrichtung, Innenabstand), in welcher Reihenfolge sie stehen, welche Funktion daran hängt, wohin es führt, welche Einträge eine Auswahlliste hat und welche Grenzen ein Schieberegler. Breitenunabhängig gültig. |
| \`bildschirme/design.css\` | Das gemeinsame Stylesheet aller Bildschirme. |
| \`design-tokens.json\` | Alle gemessenen Werte maschinenlesbar (Erscheinungen mit vollständigen Token-Tabellen, Bildschirme, Farben, Maße, Typografie, **Schriften mit Herkunft**, Formen, Effekte, Assets, Texte). |
| \`DESIGN-SPEC.md\` | Dieselben Werte als lesbare Spezifikation inklusive Bildschirm-Tabelle. |
| Übriger ZIP-Inhalt | Das unveränderte Originalprojekt mit allen Begleitdateien (Bilder, Fonts, Audio, Daten). |

## Erscheinungen

${variants.length ? variants.map((variant) => `- **${variant.name}** — \`${variant.id}\` (${variant.kind}), ${Object.keys(variant.tokens).length} Farbtoken`).join("\n") : "- Dieses Design bringt nur eine Erscheinung mit."}

## Für den Design-Umsetzer

1. **Baue nach \`bauplan/<erscheinung>/<nr>-<name>.json\`, nicht nach Koordinaten.** Der Bauplan sagt, wie die Teile zueinander stehen — eine Hierarchie gilt bei jeder Breite, eine Koordinate nur bei der Breite, bei der sie gemessen wurde. Widersprechen sich Bauplan und ein gemessener Kasten, **gewinnt der Bauplan**.
2. \`design-tokens.json\` ist die verbindliche Quelle für alle **Werte** — Farben, Maße, Schriften, Radien, Effekte. Nichts daraus schätzen oder runden. Sie sagt *womit* gebaut wird, der Bauplan sagt *wie*.
3. **Ersetze kein Bedienelement durch ein anderes.** Steht im Bauplan \`tag: "select"\` mit \`eintraege\`, wird eine Auswahlliste gebaut — kein Feld, das bei jedem Druck weiterschaltet. Steht dort \`bereich\` mit \`von\`/\`bis\`/\`schritt\`, wird ein Schieberegler gebaut — kein Knopfsatz.
4. \`versteckt: true\` heißt **Zustand**, nicht Wegfall: Ladezustand, Fehlerkarte, leerer Zustand. Alle diese Zustände werden gebaut.
5. Die Bildschirmdateien in \`bildschirme/\` tragen im Kopf \`<meta name="werft-render-width">\` und einen aufgelösten Style-Block: sie sehen damit überall so aus wie im Studio. Wer sie vermisst, setzt die Fensterbreite **vor** dem Laden auf diesen Wert.
6. \`bildschirme/<erscheinung>/\` zeigt, wie **jeder** Bildschirm in **jeder** Erscheinung aussehen muss.
7. \`design.html\` zeigt den Klickweg: \`data-werft-navigate="<ziel-id>"\` ist die Navigation.
8. Alle Erscheinungen werden als umschaltbare Themes umgesetzt, nicht nur die zuerst sichtbare.
9. **Die Schriften aus \`design-tokens.json\` → \`schriften\` sind verbindlich.** Steht dort \`quelle: "verzeichnis"\` mit einer URL, ist die Familie samt \`gewichte\` genau so einzurichten (auf Android als heruntergeladene oder mitgelieferte Schrift, nicht als Systemschrift-Ersatz). Steht dort \`quelle: "system"\`, bringt das Paket keine Quelle mit — dann muss die Familie beschafft werden, bevor gebaut wird. Eine ersetzte Schrift ist der auffälligste Unterschied zum Entwurf, den es gibt.
10. Vollständig ist die Umsetzung erst, wenn jeder Bildschirm aus der Tabelle in \`DESIGN-SPEC.md\` im Code nachweisbar ist.
${report.nichtAufgebaut.length ? `\n> **Achtung:** ${report.nichtAufgebaut.length} erkannte Bildschirme wurden beim Aufbau nicht erzeugt: ${report.nichtAufgebaut.join(", ")}. Sie fehlen in diesem Paket.\n` : ""}${report.schriftenOhneQuelle?.length ? `\n> **Achtung — Schriften ohne Quelle:** ${report.schriftenOhneQuelle.join(", ")}. Diese Familien benutzt der Entwurf, aber das Paket bringt sie nicht mit. Vor dem Bauen beschaffen, sonst rendert die App in einer anderen Schrift als der Entwurf.\n` : ""}`;
}
