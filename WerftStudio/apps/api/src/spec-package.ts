import type { DesignFacts } from "./design-facts.js";
import type { ExportDocument, ExportSection, PackageReport } from "./export-package.js";
import type { ThemeVariant } from "./screen-composer.js";

// Der Export lieferte bisher das gemessene Design — aber niemand konnte daraus BAUEN, ohne die Werte
// von Hand in Compose/WPF/SwiftUI zu uebersetzen und ohne zu wissen, was ein neu gezeichneter Knopf
// tun soll. Dieses Modul schliesst beide Luecken: es schreibt Funktions-, UI- und Motion-Spec in der
// Sprache der ZIELPLATTFORM und meldet jedes Bedienelement, das im Design steht, aber in keiner
// Funktion vorkommt. Ohne diese Meldung baut der Umsetzer einen toten Knopf.
// Der Aufbau der erzeugten Dateien folgt ~/proggs/Specs/FORMAT.md.

export type BauwegId = "compose" | "wpf" | "swiftui" | "web";
export type Bauweg = { id: BauwegId; plattform: string; sprache: string; rahmenwerk: string };

const bauwege: Record<string, Bauweg> = {
  android: { id: "compose", plattform: "Android", sprache: "Kotlin", rahmenwerk: "Jetpack Compose" },
  windows: { id: "wpf", plattform: "Windows", sprache: "C# / .NET", rahmenwerk: "WPF" },
  macos: { id: "swiftui", plattform: "macOS", sprache: "Swift", rahmenwerk: "SwiftUI" },
  ios: { id: "swiftui", plattform: "iOS", sprache: "Swift", rahmenwerk: "SwiftUI" },
  ipados: { id: "swiftui", plattform: "iPadOS", sprache: "Swift", rahmenwerk: "SwiftUI" },
  web: { id: "web", plattform: "Web", sprache: "HTML/CSS", rahmenwerk: "Web" }
};

export const bauwegFor = (platform: string): Bauweg => bauwege[platform.toLowerCase()] ?? bauwege.web!;

// ---------------------------------------------------------------- Kurven und Dauern

export type Kurve = { css: string; punkte?: [number, number, number, number] };

const benannteKurven: Record<string, [number, number, number, number]> = {
  ease: [0.25, 0.1, 0.25, 1], "ease-in": [0.42, 0, 1, 1], "ease-out": [0, 0, 0.58, 1], "ease-in-out": [0.42, 0, 0.58, 1]
};

// `linear` bleibt bewusst ohne Punkte: jede Plattform hat dafuer eine eigene, exakte Entsprechung.
// Eine als cubic-bezier(0,0,1,1) ausgeschriebene Gerade waere zwar rechnerisch gleich, aber im Code
// schlechter lesbar als LinearEasing / .linear.
export function parseKurve(raw: string): Kurve {
  const value = raw.trim().toLowerCase();
  const bezier = /^cubic-bezier\(\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\)$/.exec(value);
  if (bezier) return { css: value, punkte: [Number(bezier[1]), Number(bezier[2]), Number(bezier[3]), Number(bezier[4])] };
  const benannt = benannteKurven[value];
  return benannt ? { css: value, punkte: benannt } : { css: value || "linear" };
}

export function parseDauerMs(raw: string): number | undefined {
  const match = /^(-?[\d.]+)(ms|s)$/i.exec(raw.trim());
  if (!match) return undefined;
  const value = Number(match[1]);
  return Number.isFinite(value) ? (match[2]!.toLowerCase() === "s" ? value * 1000 : value) : undefined;
}

// ---------------------------------------------------------------- Bewegungen aus der CSS lesen

export type Bewegung = {
  id: string;
  name: string;
  ausloeser: "dauerhaft" | "wechsel" | "erscheinen";
  selektor: string;
  eigenschaften: string[];
  dauerMs: number;
  verzoegerungMs: number;
  kurve: Kurve;
  wiederholung: "einmal" | "endlos" | string;
  richtung: string;
  quelle: string;
};

const zeitToken = /^-?[\d.]+m?s$/i;
const richtungToken = /^(normal|reverse|alternate|alternate-reverse)$/i;
const fuellToken = /^(none|forwards|backwards|both)$/i;
const laufToken = /^(running|paused)$/i;
const bereichToken = /^(infinite|\d+(?:\.\d+)?)$/i;
const kurvenToken = /^(linear|ease|ease-in|ease-out|ease-in-out|step-start|step-end)$/i;

// Ein `animation:`-Kurzformwert ist reihenfolgefrei und enthaelt Klammerausdruecke mit Kommas
// (`cubic-bezier(0.4, 0, 0.2, 1)`). Ein simples split(" ") zerreisst genau die Kurve, auf die es
// ankommt — deshalb wird klammerbewusst zerlegt.
export function splitWerte(value: string): string[] {
  const teile: string[] = [];
  let tiefe = 0, aktuell = "";
  for (const zeichen of value) {
    if (zeichen === "(") tiefe += 1;
    if (zeichen === ")") tiefe -= 1;
    if (/\s/.test(zeichen) && tiefe === 0) { if (aktuell) teile.push(aktuell); aktuell = ""; continue; }
    aktuell += zeichen;
  }
  if (aktuell) teile.push(aktuell);
  return teile;
}

// `transition: transform 200ms cubic-bezier(0.4, 0, 0.2, 1)` enthaelt Kommas INNERHALB der Kurve.
// Ein blosses split(",") zerlegt sie in vier Bruchstuecke und erzeugt drei Phantom-Bewegungen mit
// Dauer 0 — die Kurve, auf die es ankommt, geht dabei verloren.
export function splitKommas(value: string): string[] {
  const teile: string[] = [];
  let tiefe = 0, aktuell = "";
  for (const zeichen of value) {
    if (zeichen === "(") tiefe += 1;
    if (zeichen === ")") tiefe -= 1;
    if (zeichen === "," && tiefe === 0) { if (aktuell.trim()) teile.push(aktuell.trim()); aktuell = ""; continue; }
    aktuell += zeichen;
  }
  if (aktuell.trim()) teile.push(aktuell.trim());
  return teile;
}

type Kurzform = { name?: string; dauerMs: number; verzoegerungMs: number; kurve: Kurve; wiederholung: string; richtung: string };

export function parseAnimationKurzform(value: string): Kurzform {
  const teile = splitWerte(value);
  const zeiten: number[] = [];
  let kurve: Kurve = parseKurve("ease");
  let wiederholung = "1", richtung = "normal", name: string | undefined;
  for (const teil of teile) {
    if (zeitToken.test(teil)) { const ms = parseDauerMs(teil); if (ms !== undefined) zeiten.push(ms); continue; }
    if (kurvenToken.test(teil) || /^(cubic-bezier|steps)\(/i.test(teil)) { kurve = parseKurve(teil); continue; }
    if (bereichToken.test(teil)) { wiederholung = teil.toLowerCase(); continue; }
    if (richtungToken.test(teil)) { richtung = teil.toLowerCase(); continue; }
    if (fuellToken.test(teil) || laufToken.test(teil) || teil === "none") continue;
    if (!name) name = teil;
  }
  return { ...(name ? { name } : {}), dauerMs: zeiten[0] ?? 0, verzoegerungMs: zeiten[1] ?? 0, kurve, wiederholung, richtung };
}

// Dieselbe klammerbewusste Zerlegung wie in `screenScopedCss`, hier aber mit Zugriff auf Selektor
// UND Rumpf, weil eine Bewegung ohne ihren Selektor nicht zuzuordnen waere.
function regeln(css: string): Array<{ selektor: string; rumpf: string }> {
  const treffer: Array<{ selektor: string; rumpf: string }> = [];
  let index = 0;
  while (index < css.length) {
    const open = css.indexOf("{", index);
    if (open < 0) break;
    const selektor = css.slice(index, open).trim();
    let tiefe = 1, cursor = open + 1;
    while (cursor < css.length && tiefe > 0) {
      if (css[cursor] === "{") tiefe += 1;
      else if (css[cursor] === "}") tiefe -= 1;
      cursor += 1;
    }
    const rumpf = css.slice(open + 1, cursor - 1);
    if (/^@(?:media|supports|layer|container)/i.test(selektor)) treffer.push(...regeln(rumpf));
    else if (selektor) treffer.push({ selektor, rumpf });
    index = cursor;
  }
  return treffer;
}

const deklaration = (rumpf: string, name: string): string | undefined =>
  new RegExp(`(?:^|;)\\s*${name}\\s*:\\s*([^;]+)`, "i").exec(rumpf)?.[1]?.trim();

// Der belegte Fall aus dem PerfectMoment-Paket: `design-tokens.json` fuehrte 5 Effekte, die CSS
// enthielt 36 `@keyframes`. Wer nur die Faktenliste liest, verliert den groessten Teil der Bewegung.
// Deshalb ist die CSS hier die Quelle und die Faktenliste nur die Ergaenzung.
export function bewegungenAusCss(css: string, quelle = "bildschirme/design.css"): Bewegung[] {
  const keyframes = new Set([...css.matchAll(/@keyframes\s+([A-Za-z0-9_-]+)/gi)].map((match) => match[1]!));
  const gefunden: Bewegung[] = [];
  const gesehen = new Set<string>();
  for (const { selektor, rumpf } of regeln(css)) {
    const animation = deklaration(rumpf, "animation") ?? deklaration(rumpf, "animation-name");
    if (animation && !/^none$/i.test(animation.trim())) {
      const kurz = parseAnimationKurzform(animation);
      const dauer = kurz.dauerMs || parseDauerMs(deklaration(rumpf, "animation-duration") ?? "") || 0;
      const name = kurz.name ?? "unbenannt";
      const schluessel = `a:${selektor}:${name}`;
      if (!gesehen.has(schluessel)) {
        gesehen.add(schluessel);
        gefunden.push({
          id: "", name, ausloeser: kurz.wiederholung === "infinite" ? "dauerhaft" : "erscheinen",
          selektor, eigenschaften: keyframes.has(name) ? eigenschaftenAusKeyframes(css, name) : [],
          dauerMs: dauer, verzoegerungMs: kurz.verzoegerungMs, kurve: kurz.kurve,
          wiederholung: kurz.wiederholung === "infinite" ? "endlos" : kurz.wiederholung === "1" ? "einmal" : `${kurz.wiederholung}×`,
          richtung: kurz.richtung, quelle: keyframes.has(name) ? `@keyframes ${name} in ${quelle}` : quelle
        });
      }
    }
    const transition = deklaration(rumpf, "transition");
    if (transition && !/^none$/i.test(transition.trim())) {
      for (const einzeln of splitKommas(transition)) {
        const teile = splitWerte(einzeln);
        const eigenschaft = teile.find((teil) => !zeitToken.test(teil) && !kurvenToken.test(teil) && !/^(cubic-bezier|steps)\(/i.test(teil)) ?? "all";
        const zeiten = teile.filter((teil) => zeitToken.test(teil)).map((teil) => parseDauerMs(teil) ?? 0);
        const kurvenTeil = teile.find((teil) => kurvenToken.test(teil) || /^(cubic-bezier|steps)\(/i.test(teil));
        const schluessel = `t:${selektor}:${eigenschaft}`;
        if (gesehen.has(schluessel)) continue;
        gesehen.add(schluessel);
        gefunden.push({
          id: "", name: `Übergang ${eigenschaft}`, ausloeser: "wechsel", selektor, eigenschaften: [eigenschaft],
          dauerMs: zeiten[0] ?? 0, verzoegerungMs: zeiten[1] ?? 0, kurve: parseKurve(kurvenTeil ?? "ease"),
          wiederholung: "einmal", richtung: "normal", quelle
        });
      }
    }
  }
  // Die Kennung kommt aus dem @keyframes-Namen, wenn sie dort eingebrannt ist (`m-04-atmen`) —
  // eine blosse Zaehlnummer wuerde sich verschieben, sobald der Designer eine Bewegung einfuegt,
  // und damit genau die Zuordnung zerstoeren, auf der der Abgleich gegen v1 beruht.
  let naechste = gefunden.reduce((hoechste, bewegung) => Math.max(hoechste, eingebrannteKennung(bewegung.name) ?? 0), 0);
  return gefunden.map((bewegung) => {
    const nummer = eingebrannteKennung(bewegung.name) ?? (naechste += 1);
    return { ...bewegung, id: `M-${String(nummer).padStart(2, "0")}` };
  });
}

const eingebrannteKennung = (name: string): number | undefined => {
  const treffer = /^m-?(\d+)\b/i.exec(name);
  return treffer ? Number(treffer[1]) : undefined;
}

function eigenschaftenAusKeyframes(css: string, name: string): string[] {
  const start = new RegExp(`@keyframes\\s+${name}\\s*\\{`, "i").exec(css);
  if (!start) return [];
  let tiefe = 1, cursor = start.index + start[0].length;
  while (cursor < css.length && tiefe > 0) {
    if (css[cursor] === "{") tiefe += 1;
    else if (css[cursor] === "}") tiefe -= 1;
    cursor += 1;
  }
  const rumpf = css.slice(start.index + start[0].length, cursor - 1);
  return [...new Set([...rumpf.matchAll(/(?:^|[;{]\s*)([a-z-]+)\s*:/gi)].map((match) => match[1]!.toLowerCase()))];
}

// ---------------------------------------------------------------- Uebersetzung in die Zielsprache

const f = (value: number) => `${value}f`;
const sekunden = (ms: number) => Number((ms / 1000).toFixed(3));

export function motionCode(bauweg: Bauweg, bewegung: Bewegung): string {
  const { dauerMs, verzoegerungMs, kurve, wiederholung, richtung } = bewegung;
  const endlos = wiederholung === "endlos";
  const umkehr = /alternate/.test(richtung);
  if (bauweg.id === "compose") {
    const easing = kurve.punkte ? `CubicBezierEasing(${kurve.punkte.map(f).join(", ")})` : "LinearEasing";
    const tween = `tween(durationMillis = ${Math.round(dauerMs)}${verzoegerungMs ? `, delayMillis = ${Math.round(verzoegerungMs)}` : ""}, easing = ${easing})`;
    return endlos ? `infiniteRepeatable(animation = ${tween}, repeatMode = RepeatMode.${umkehr ? "Reverse" : "Restart"})` : tween;
  }
  if (bauweg.id === "wpf") {
    const spline = kurve.punkte ? ` KeySpline="${kurve.punkte[0]},${kurve.punkte[1]} ${kurve.punkte[2]},${kurve.punkte[3]}"` : "";
    return `Duration="0:0:${sekunden(dauerMs).toFixed(3)}"${verzoegerungMs ? ` BeginTime="0:0:${sekunden(verzoegerungMs).toFixed(3)}"` : ""}${endlos ? ` RepeatBehavior="Forever"` : ""}${umkehr ? ` AutoReverse="True"` : ""}${spline}`;
  }
  if (bauweg.id === "swiftui") {
    const basis = kurve.punkte
      ? `.timingCurve(${kurve.punkte.join(", ")}, duration: ${sekunden(dauerMs)})`
      : `.linear(duration: ${sekunden(dauerMs)})`;
    const verzoegert = verzoegerungMs ? `${basis}.delay(${sekunden(verzoegerungMs)})` : basis;
    return endlos ? `${verzoegert}.repeatForever(autoreverses: ${umkehr})` : verzoegert;
  }
  return `animation: ${bewegung.name} ${Math.round(dauerMs)}ms ${kurve.css}${verzoegerungMs ? ` ${Math.round(verzoegerungMs)}ms` : ""}${endlos ? " infinite" : ""}${umkehr ? " alternate" : ""}`;
}

// ---------------------------------------------------------------- Bedienelemente im Design

export type Bedienelement = { bildschirm: string; label: string; art: string; ziel?: string; funktion?: string };

// `data-werft-funktion` traegt entweder die Kennung einer bekannten Funktion (`F-07`) oder — bei
// einem im Studio neu gezeichneten Bedienelement — den Satz, den der Benutzer dem Designer gesagt
// hat ("pausiert die laufende Sitzung"). Beides ist Gold wert: das erste trennt bekannt von neu
// ohne Beschriftungs-Raterei, das zweite rettet die Absicht in das Funktions-Spec, statt sie beim
// Rueckimport noch einmal erfragen zu muessen.
const kennungsMuster = /^F-\d+$/i;

const beschriftung = (html: string): string => {
  const aria = /\saria-label="([^"]+)"/i.exec(html)?.[1];
  if (aria) return aria.trim();
  const text = html.replace(/<[^>]*>/g, " ").replace(/&[a-z]+;/gi, " ").replace(/\s+/g, " ").trim();
  return text.slice(0, 80);
};

// Erfasst wird, was der Benutzer anfassen kann. `data-werft-navigate` ist dabei der Beweis, dass ein
// Element schon ein Ziel HAT — nur alles ohne Ziel und ohne Funktion ist ein Kandidat fuer einen
// toten Knopf und muss im Funktions-Spec auftauchen.
export function bedienelemente(section: ExportSection): Bedienelement[] {
  const gefunden: Bedienelement[] = [];
  const muster = /<(button|a|input|select|textarea)\b([^>]*)>|<([a-z][a-z0-9-]*)\b([^>]*(?:role="button"|data-werft-navigate=|data-werft-action=)[^>]*)>/gi;
  let match: RegExpExecArray | null;
  while ((match = muster.exec(section.html))) {
    const tag = (match[1] ?? match[3] ?? "").toLowerCase();
    const attribute = match[2] ?? match[4] ?? "";
    const ziel = /data-werft-navigate="([^"]*)"/i.exec(attribute)?.[1];
    const funktion = /data-werft-funktion="([^"]*)"/i.exec(attribute)?.[1]?.trim();
    const ende = section.html.indexOf(`</${tag}`, muster.lastIndex);
    const inhalt = ende > 0 ? section.html.slice(muster.lastIndex, ende) : "";
    const label = beschriftung(attribute.includes("aria-label") ? attribute : inhalt) || tag;
    gefunden.push({ bildschirm: section.name, label, art: tag, ...(ziel ? { ziel } : {}), ...(funktion ? { funktion } : {}) });
  }
  // Gleiche Beschriftung auf demselben Bildschirm ist dasselbe Element in einem anderen Zustand.
  const gesehen = new Set<string>();
  return gefunden.filter((element) => {
    const schluessel = `${element.bildschirm}|${element.label}|${element.art}`;
    if (gesehen.has(schluessel)) return false;
    gesehen.add(schluessel);
    return true;
  });
}

// ---------------------------------------------------------------- Vorlage aus dem Erst-Spec

export type Vorlage = { projekt?: string; funktion?: string; ui?: string; motion?: string };

export type VorhandeneFunktion = { id: string; name: string };

export function vorhandeneFunktionen(text: string): VorhandeneFunktion[] {
  return [...text.matchAll(/^#{2,4}\s*(F-\d+)\s*[—–-]\s*(.+?)\s*$/gm)].map((match) => ({ id: match[1]!, name: match[2]! }));
}

const naechsteNummer = (funktionen: VorhandeneFunktion[]): number =>
  funktionen.reduce((hoechste, funktion) => Math.max(hoechste, Number(funktion.id.slice(2)) || 0), 0) + 1;

// Ein Element gilt als bekannt, wenn seine Beschriftung im Erst-Funktions-Spec vorkommt. Das ist
// absichtlich grosszuegig: lieber ein bekanntes Element einmal zu viel durchwinken, als den Benutzer
// beim Rueckimport mit Scheinfunden zu ueberschuetten. Was hier durchrutscht, faengt der Abgleich
// gegen v1 im Skill `spec-rueckimport` ohnehin noch einmal ab.
const istBekannt = (element: Bedienelement, funktionsText: string): boolean => {
  // Traegt das Element eine Kennung, ist die Frage entschieden — ohne Raten ueber Beschriftungen.
  if (element.funktion) return kennungsMuster.test(element.funktion);
  const label = element.label.trim().toLowerCase();
  return label.length >= 3 && funktionsText.toLowerCase().includes(label);
};

export function neueBedienelemente(sections: ExportSection[], vorlage: Vorlage): Bedienelement[] {
  const funktionsText = vorlage.funktion ?? "";
  return sections
    .flatMap((section) => bedienelemente(section))
    // Ein Element mit Navigationsziel ist nie tot. Ein Element mit beschriebener Aufgabe schon gar
    // nicht — es ist neu, aber es weiss, was es soll, und kommt MIT dieser Beschreibung ins Spec.
    .filter((element) => !element.ziel && !istBekannt(element, funktionsText));
}

// ---------------------------------------------------------------- Die drei Specs

export type SpecInput = {
  projectName: string;
  platform: string;
  facts: DesignFacts;
  variants: ThemeVariant[];
  document: ExportDocument;
  report: PackageReport;
  vorlage?: Vorlage;
};

const kopf = (titel: string, projekt: string, bauweg: Bauweg, stand: string) =>
  `# ${titel} — ${projekt}\nStand: ${stand} · Stufe: v2 · Plattform: ${bauweg.plattform} (${bauweg.sprache} / ${bauweg.rahmenwerk})\n`;

// Der Erzeuger schreibt die Spec-Kennung als `data-screen-id` ins Markup (siehe composeScreens).
// Steht sie dort, wird sie gelesen — verschiebt der Designer einen Bildschirm, wandert seine
// Kennung mit. Nur wo keine steht, wird gezaehlt.
const bildschirmId = (section: ExportSection, index: number) =>
  /^B-\d+$/i.test(section.id) ? section.id.toUpperCase() : `B-${String(index + 1).padStart(2, "0")}`;

export function funktionsSpec(input: SpecInput, bauweg: Bauweg, stand: string): string {
  const vorlage = input.vorlage?.funktion?.trim();
  const neu = neueBedienelemente(input.document.sections, input.vorlage ?? {});
  const bekannt = vorhandeneFunktionen(vorlage ?? "");
  let nummer = naechsteNummer(bekannt);
  const zeilen: string[] = [];
  if (vorlage) zeilen.push(vorlage, "");
  else {
    zeilen.push(kopf("Funktions-Spec", input.projectName, bauweg, stand), "",
      "> Für dieses Projekt lag beim Import kein Erst-Funktions-Spec vor. Die folgenden Einträge sind",
      "> **aus dem Design abgeleitet** — was jedes Element tun soll, ist noch offen und wird beim",
      "> Rückimport erfragt. Nichts davon ist geraten.", "");
  }
  zeilen.push("## Neu aus dem Design", "");
  if (!neu.length) {
    zeilen.push("Der Designer hat kein Bedienelement ergänzt, das in diesem Spec fehlt. **Kein toter Knopf.**", "");
  } else {
    const beschrieben = neu.filter((element) => element.funktion);
    const offen = neu.filter((element) => !element.funktion);
    zeilen.push(
      `Beim Gestalten sind ${neu.length} Bedienelement(e) dazugekommen, die es im Erst-Spec noch nicht gab.`,
      `Davon ${beschrieben.length} mit beschriebener Aufgabe, ${offen.length} ohne.`,
      "Jedes bekommt eine Kennung, damit der Umsetzer es nicht als Attrappe baut.", "",
      "| Kennung | Bildschirm | Element | Art | Aufgabe |",
      "|---------|------------|---------|-----|---------|");
    // Erst die beschriebenen: das ist der Normalfall, wenn im Studio gesagt wurde, was der Knopf tun
    // soll. Diese Beschreibung IST die Funktion — sie wird woertlich uebernommen, nicht ausgelegt.
    for (const element of beschrieben) {
      zeilen.push(`| F-${String(nummer++).padStart(2, "0")} | ${element.bildschirm} | ${element.label || "—"} | ${element.art} | ${element.funktion} |`);
    }
    for (const element of offen) {
      zeilen.push(`| F-${String(nummer++).padStart(2, "0")} | ${element.bildschirm} | ${element.label || "—"} | ${element.art} | **OFFEN — beim Rückimport klären** |`);
    }
    zeilen.push("");
    if (offen.length) {
      zeilen.push(
        "> Zu den offenen Punkten wurde im Studio nicht gesagt, was sie tun sollen. Sie werden beim",
        "> Rückimport erfragt und **nicht erfunden** — sonst entstünde beim Bauen ein toter Knopf.", "");
    }
  }
  const mitZiel = input.document.sections.flatMap((section) => bedienelemente(section)).filter((element) => element.ziel);
  if (mitZiel.length) {
    zeilen.push("## Navigation aus dem Design", "",
      "Diese Elemente haben im Design bereits ein Ziel und brauchen keine eigene Funktion.", "",
      "| Bildschirm | Element | führt zu |", "|------------|---------|----------|",
      ...mitZiel.map((element) => `| ${element.bildschirm} | ${element.label || "—"} | \`${element.ziel}\` |`), "");
  }
  return zeilen.join("\n");
}

export function uiSpec(input: SpecInput, bauweg: Bauweg, stand: string): string {
  const { facts, variants, document, report } = input;
  const zeilen = [kopf("UI-Spec", input.projectName, bauweg, stand), "",
    "Alle Werte sind **deterministisch aus dem Design gemessen**, nicht geschätzt. Sie sind verbindlich.", ""];

  zeilen.push("## 2. Erscheinungen (Themes)", "");
  if (!variants.length) zeilen.push("Dieses Design bringt nur eine Erscheinung mit.", "");
  for (const variant of variants) {
    zeilen.push(`### ${variant.name} — \`${variant.id}\` (${variant.kind})`, "",
      "| Rolle | Wert |", "|-------|------|",
      ...Object.entries(variant.tokens).map(([rolle, wert]) => `| \`${rolle}\` | \`${wert}\` |`), "");
  }

  zeilen.push("## 3. Typografie", "", "| Rolle | Familie | Größe | Gewicht | Zeilenhöhe | Laufweite | Quelle |",
    "|-------|---------|-------|---------|------------|-----------|--------|",
    ...facts.typography.map((type) => `| ${type.name} | ${type.family ?? "—"} | ${type.sizePx ?? "—"} | ${type.weight ?? "—"} | ${type.lineHeightPx ?? "—"} | ${type.letterSpacingPx ?? "—"} | \`${type.source}\` |`), "");

  zeilen.push("## 4. Maße und Raster", "", "| Name | px | Original | Quelle |", "|------|----|----------|--------|",
    ...facts.dimensions.map((dimension) => `| ${dimension.name} | ${dimension.px} | \`${dimension.raw}\` | \`${dimension.source}\` |`), "");

  zeilen.push("## 5. Formen und Tiefe", "", "| Name | Radius | Quelle |", "|------|--------|--------|",
    ...facts.shapes.map((shape) => `| ${shape.name} | \`${shape.radiusCss}\` | \`${shape.source}\` |`), "");
  const ohneBewegung = facts.effects.filter((effect) => effect.kind !== "animation");
  if (ohneBewegung.length) {
    zeilen.push("", "| Effekt | Art | CSS | Quelle |", "|--------|-----|-----|--------|",
      ...ohneBewegung.map((effect) => `| ${effect.name} | ${effect.kind} | \`${effect.css}\` | \`${effect.source}\` |`), "");
  }

  zeilen.push("## 6. Bildschirme", "", "| Kennung | Bildschirm | Start | führt zu | Dateien je Erscheinung |",
    "|---------|------------|-------|----------|------------------------|");
  const screensById = new Map(facts.screens.map((screen) => [screen.id, screen] as const));
  for (const [index, section] of document.sections.entries()) {
    const ziele = screensById.get(section.id)?.navigatesTo ?? [];
    const dateien = variants.length ? variants.map((variant) => `\`bildschirme/${variant.id}/…-${section.slug}.html\``).join("<br>") : "`design.html`";
    zeilen.push(`| ${bildschirmId(section, index)} | ${section.name} (\`${section.id}\`) | ${section.isStart ? "ja" : "—"} | ${ziele.join(", ") || "—"} | ${dateien} |`);
  }
  zeilen.push("");
  if (report.nichtAufgebaut.length) {
    zeilen.push(`> **Achtung:** ${report.nichtAufgebaut.length} gemessene Bildschirme wurden im Design nicht aufgebaut und fehlen hier: ${report.nichtAufgebaut.join(", ")}.`, "");
  }

  if (facts.assets.length) {
    zeilen.push("## 7. Ikonografie und Bilder", "", "| Name | Art | Pfad |", "|------|-----|------|",
      ...facts.assets.map((asset) => `| ${asset.name} | ${asset.kind} | \`${asset.path}\` |`), "");
  }
  if (facts.strings.length) {
    zeilen.push("## 8. Texte", "", "| Name | Text |", "|------|------|",
      ...facts.strings.map((entry) => `| ${entry.name} | ${JSON.stringify(entry.value)} |`), "");
  }
  zeilen.push("## 10. Offene Fragen", "", facts.notes.length ? facts.notes.map((note) => `- ${note}`).join("\n") : "- keine", "");
  return zeilen.join("\n");
}

export function motionSpec(input: SpecInput, bauweg: Bauweg, stand: string): string {
  const bewegungen = bewegungenAusCss(input.document.css);
  const ausFakten = input.facts.effects.filter((effect) => effect.kind === "animation");
  const zeilen = [kopf("Motion-Spec", input.projectName, bauweg, stand), "",
    `Jede Bewegung ist aus dem Design gemessen und für **${bauweg.rahmenwerk}** übersetzt.`,
    "Die angegebene Kurve ist verbindlich — eine eingebaute Standardkurve an ihrer Stelle gilt als nicht erfüllt.", "",
    "## 2. Kurven und Dauern", "", "| Kennung | Bewegung | Dauer | Kurve | Wiederholung |",
    "|---------|----------|-------|-------|--------------|",
    ...bewegungen.map((bewegung) => `| ${bewegung.id} | ${bewegung.name} | ${Math.round(bewegung.dauerMs)} ms | \`${bewegung.kurve.css}\` | ${bewegung.wiederholung} |`), "",
    "## 3. Bewegungen im Einzelnen", ""];
  if (!bewegungen.length) zeilen.push("Dieses Design enthält keine gemessene Bewegung.", "");
  for (const bewegung of bewegungen) {
    zeilen.push(`### ${bewegung.id} — ${bewegung.name}`, "",
      `- **Wo:** \`${bewegung.selektor}\``,
      `- **Auslöser:** ${bewegung.ausloeser}`,
      `- **Was sich ändert:** ${bewegung.eigenschaften.join(", ") || "—"}`,
      `- **Dauer / Verzögerung:** ${Math.round(bewegung.dauerMs)} ms / ${Math.round(bewegung.verzoegerungMs)} ms`,
      `- **Kurve:** \`${bewegung.kurve.css}\`${bewegung.kurve.punkte ? ` → \`cubic-bezier(${bewegung.kurve.punkte.join(", ")})\`` : ""}`,
      `- **Wiederholung / Richtung:** ${bewegung.wiederholung} / ${bewegung.richtung}`,
      `- **Quelle:** ${bewegung.quelle}`,
      `- **${bauweg.rahmenwerk}:** \`${motionCode(bauweg, bewegung)}\``, "");
  }
  if (ausFakten.length) {
    zeilen.push("## 3b. Zusätzlich gemessene Effekte", "", "| Name | CSS | Quelle |", "|------|-----|--------|",
      ...ausFakten.map((effect) => `| ${effect.name} | \`${effect.css}\` | \`${effect.source}\` |`), "");
  }
  zeilen.push("## 8. Reduzierte Bewegung", "",
    "Meldet das System „Bewegung reduzieren“: Dauerbewegung aus, Übergänge auf reines Überblenden,",
    "Dauern halbiert. Diese Regel gilt, solange das Funktions-Spec nichts anderes festlegt.", "");
  return zeilen.join("\n");
}

export type SpecFile = { path: string; content: string };

export function buildSpecPackage(input: SpecInput, stand: string): SpecFile[] {
  const bauweg = bauwegFor(input.platform);
  const funktion = funktionsSpec(input, bauweg, stand);
  const ui = uiSpec(input, bauweg, stand);
  const motion = motionSpec(input, bauweg, stand);
  const gesamt = [
    `# ${input.projectName} — Spec v2`,
    `Stand: ${stand} · Plattform: ${bauweg.plattform} (${bauweg.sprache} / ${bauweg.rahmenwerk})`,
    `Erzeugt von: Werft Studio beim Herunterladen`, "",
    "Diese Datei ist die Zusammenstellung der drei Specs. Die Einzeldateien daneben sind wortgleich.", "",
    "## Teil A — Funktions-Spec", "", funktion, "",
    "## Teil B — UI-Spec", "", ui, "",
    "## Teil C — Motion-Spec", "", motion, ""
  ].join("\n");
  return [
    { path: "01-FUNKTIONS-SPEC.md", content: funktion },
    { path: "02-UI-SPEC.md", content: ui },
    { path: "03-MOTION-SPEC.md", content: motion },
    { path: "SPEC.md", content: gesamt },
    { path: "00-PROJEKT.md", content: projektSpec(input, bauweg, stand) }
  ];
}

// Das Erst-Projektblatt nennt die Plattform, fuer die geplant wurde. Beim Herunterladen waehlt der
// Benutzer aber ausdruecklich, WOFUER gebaut werden soll — und `spec-rueckimport` liest die
// Plattform genau aus dieser Datei. Ohne den Stempel stuende dort weiter Android, waehrend das
// uebrige Paket Windows beschreibt, und Stufe 3 nimmt den falschen Bau-Weg.
function projektSpec(input: SpecInput, bauweg: Bauweg, stand: string): string {
  const stempel = [
    `> **Zielplattform für diesen Bau: ${bauweg.plattform} (${bauweg.sprache} / ${bauweg.rahmenwerk}).**`,
    `> Beim Herunterladen aus Werft Studio am ${stand} gewählt. Sie gilt vor jeder abweichenden`,
    "> Angabe weiter unten in dieser Datei.", ""
  ].join("\n");
  const vorlage = input.vorlage?.projekt?.trim();
  if (!vorlage) return `# Projekt — ${input.projectName}\nStand: ${stand} · Stufe: v2 · Plattform: ${bauweg.plattform}\n\n${stempel}`;
  // Der Stempel kommt direkt hinter die Kopfzeile, damit er vor dem ersten Abschnitt steht.
  const bruch = vorlage.indexOf("\n\n");
  return bruch < 0 ? `${vorlage}\n\n${stempel}` : `${vorlage.slice(0, bruch + 2)}${stempel}\n${vorlage.slice(bruch + 2)}`;
}
