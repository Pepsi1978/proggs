import { dimensionToPx, type DesignFacts, type FactColor, type FactEffect, type FactScreen, type FactShape, type FactTheme, type FactTypography } from "./design-facts.js";
import type { SourceText } from "./extract-common.js";

// Bisher konnte Werft nur Designs MESSEN, die es schon gab: die Extraktoren lesen Kotlin, Swift,
// XAML oder HTML. Ein Spec-Paket aus `Designs/Inbox/` enthaelt aber gar keinen Code — es beschreibt
// eine Software, die es noch nicht gibt. Genau daraus soll der Designer die durchklickbare
// HTML-Fassung bauen. Dieser Extraktor macht aus der Beschreibung dieselben `DesignFacts`, die
// sonst aus Quellcode entstehen; ab da laeuft der vorhandene Weg (Bildschirmplan, KI-Aufbau,
// Zusammensetzen) unveraendert weiter. Format der gelesenen Dateien: ~/proggs/Specs/FORMAT.md.

const einzelSpec = /(^|\/)(?:00-PROJEKT|01-FUNKTIONS-SPEC|02-UI-SPEC|03-MOTION-SPEC|04-ONBOARDING-SPEC|05-RECHT-SPEC)\.md$/i;
const sammelSpec = /(^|\/)SPEC\.md$/i;

export function specFactCandidates(paths: string[]): string[] {
  const einzeln = paths.filter((path) => einzelSpec.test(path));
  // `SPEC.md` ist die Zusammenstellung derselben Inhalte. Liegen die Einzeldateien vor, wuerde sie
  // jeden Wert ein zweites Mal liefern — dann bleibt sie draussen.
  return einzeln.length ? einzeln : paths.filter((path) => sammelSpec.test(path));
}

// ---------------------------------------------------------------- Markdown lesen

const schluessel = (wert: string) =>
  wert.toLowerCase().replaceAll("ä", "a").replaceAll("ö", "o").replaceAll("ü", "u").replaceAll("ß", "ss").replace(/[^a-z0-9]/g, "");

const rein = (wert: string) => wert.replace(/[`*]/g, "").trim();

const zellen = (zeile: string) => zeile.trim().replace(/^\|/, "").replace(/\|$/, "").split("|").map((wert) => wert.trim());

export type Tabelle = { spalten: string[]; zeilen: Array<Record<string, string>> };

// Die Specs werden von zwei Seiten geschrieben: Stufe 1 von Hand nach FORMAT.md, der Export
// maschinell. Beide Fassungen haben dieselben Spalten-BEDEUTUNGEN, aber nicht dieselbe Spaltenzahl.
// Deshalb wird ueber die Kopfzeile zugeordnet und nicht ueber die Position.
export function tabellen(text: string): Tabelle[] {
  const gefunden: Tabelle[] = [];
  const zeilen = text.split(/\r?\n/);
  for (let index = 0; index < zeilen.length; index += 1) {
    if (!/^\s*\|/.test(zeilen[index] ?? "") || !/^\s*\|[\s:|-]+\|?\s*$/.test(zeilen[index + 1] ?? "")) continue;
    const spalten = zellen(zeilen[index]!).map(schluessel);
    const daten: Array<Record<string, string>> = [];
    let lauf = index + 2;
    for (; lauf < zeilen.length && /^\s*\|/.test(zeilen[lauf] ?? ""); lauf += 1) {
      const werte = zellen(zeilen[lauf]!);
      const satz: Record<string, string> = {};
      spalten.forEach((spalte, spaltenIndex) => { satz[spalte] = rein(werte[spaltenIndex] ?? ""); });
      daten.push(satz);
    }
    gefunden.push({ spalten, zeilen: daten });
    index = lauf - 1;
  }
  return gefunden;
}

type Abschnitt = { titel: string; inhalt: string };

function abschnitte(text: string, ebene: "##" | "###"): Abschnitt[] {
  const muster = ebene === "##" ? /^##\s+(?!#)(.+)$/gm : /^###\s+(?!#)(.+)$/gm;
  const koepfe = [...text.matchAll(muster)];
  // Der Titel bleibt ROH: in ihm steht die Kennung der Erscheinung in Backticks
  // (`### Dunkelmodus — \`dunkel\` (dark)`). Wer ihn vorher saeubert, wirft sie weg.
  return koepfe.map((kopf, index) => ({
    titel: kopf[1]!.trim(),
    inhalt: text.slice(kopf.index! + kopf[0].length, index + 1 < koepfe.length ? koepfe[index + 1]!.index! : text.length)
  }));
}

const findeAbschnitt = (text: string, ...worte: string[]): string | undefined =>
  abschnitte(text, "##").find((teil) => worte.some((wort) => schluessel(teil.titel).includes(schluessel(wort))))?.inhalt;

const ersteTabelle = (text: string | undefined, ...pflichtSpalten: string[]): Tabelle | undefined =>
  text ? tabellen(text).find((tabelle) => pflichtSpalten.every((spalte) => tabelle.spalten.includes(spalte))) : undefined;

const wert = (satz: Record<string, string>, ...namen: string[]): string => {
  for (const name of namen) if (satz[name]) return satz[name]!;
  return "";
};

const zahl = (roh: string): number | undefined => {
  const treffer = /-?\d+(?:[.,]\d+)?/.exec(roh.replace(",", "."));
  if (!treffer) return undefined;
  const value = Number(treffer[0]);
  return Number.isFinite(value) ? value : undefined;
};

// ---------------------------------------------------------------- Die einzelnen Faktenarten

function erscheinungen(uiSpec: string, quelle: string): { themes: FactTheme[]; colors: FactColor[] } {
  const bereich = findeAbschnitt(uiSpec, "Erscheinungen", "Themes");
  if (!bereich) return { themes: [], colors: [] };
  const themes: FactTheme[] = [];
  const colors: FactColor[] = [];
  for (const erscheinung of abschnitte(bereich, "###")) {
    const tabelle = ersteTabelle(erscheinung.inhalt, "rolle") ?? tabellen(erscheinung.inhalt)[0];
    if (!tabelle) continue;
    const kennung = /`([A-Za-z0-9_-]+)`/.exec(erscheinung.titel)?.[1];
    const art = /\((dark|light|dunkel|hell|other)\)/i.exec(erscheinung.titel)?.[1]?.toLowerCase();
    const name = erscheinung.titel.replace(/\s*—.*$/, "").trim() || (kennung ?? "Standard");
    const id = kennung ?? (schluessel(name) || `erscheinung${themes.length + 1}`);
    const tokens: Record<string, string> = {};
    for (const zeile of tabelle.zeilen) {
      const rolle = wert(zeile, "rolle", "name", "token");
      const farbe = wert(zeile, "wert", "werthexrgba", "farbe", "css");
      if (!rolle || !farbe) continue;
      tokens[rolle] = farbe;
      colors.push({ name: `${id}.${rolle}`, css: farbe, source: quelle, used: true });
    }
    // Eine Erscheinung ohne Farben ist keine: sie wuerde im Studio als leerer Umschalter erscheinen.
    if (Object.keys(tokens).length) themes.push({ id, name: art ? `${name}` : name, tokens, source: quelle });
  }
  return { themes, colors };
}

function typografie(uiSpec: string, quelle: string): FactTypography[] {
  const tabelle = ersteTabelle(findeAbschnitt(uiSpec, "Typografie"), "rolle") ?? ersteTabelle(findeAbschnitt(uiSpec, "Typografie"), "name");
  if (!tabelle) return [];
  return tabelle.zeilen.flatMap((zeile) => {
    const name = wert(zeile, "rolle", "name");
    if (!name) return [];
    const eintrag: FactTypography = { name, source: quelle, used: true };
    const familie = wert(zeile, "familie", "schriftart", "schrift");
    if (familie && familie !== "—") eintrag.family = familie;
    const groesse = zahl(wert(zeile, "grosse", "groesse", "grossepx", "size"));
    if (groesse !== undefined) eintrag.sizePx = groesse;
    const gewicht = zahl(wert(zeile, "gewicht", "starke", "weight"));
    if (gewicht !== undefined) eintrag.weight = gewicht;
    const zeilenhoehe = zahl(wert(zeile, "zeilenhohe", "zeilenhohepx", "lineheight"));
    if (zeilenhoehe !== undefined) eintrag.lineHeightPx = zeilenhoehe;
    const laufweite = zahl(wert(zeile, "laufweite", "laufweitepx", "letterspacing"));
    if (laufweite !== undefined) eintrag.letterSpacingPx = laufweite;
    return [eintrag];
  });
}

function masse(uiSpec: string, quelle: string): DesignFacts["dimensions"] {
  const tabelle = ersteTabelle(findeAbschnitt(uiSpec, "Masse", "Maße", "Raster"), "name");
  if (!tabelle) return [];
  return tabelle.zeilen.flatMap((zeile) => {
    const name = wert(zeile, "name", "rolle");
    const roh = wert(zeile, "px", "wert", "original", "grosse");
    const px = zahl(roh) ?? (dimensionToPx(roh) ?? undefined);
    return name && px !== undefined && Number.isFinite(px) ? [{ name, px, raw: roh || `${px}px`, source: quelle, used: true }] : [];
  });
}

function formenUndEffekte(uiSpec: string, quelle: string): { shapes: FactShape[]; effects: FactEffect[] } {
  const bereich = findeAbschnitt(uiSpec, "Formen", "Tiefe");
  const shapes: FactShape[] = [];
  const effects: FactEffect[] = [];
  if (!bereich) return { shapes, effects };
  for (const tabelle of tabellen(bereich)) {
    if (tabelle.spalten.includes("radius")) {
      for (const zeile of tabelle.zeilen) {
        const name = wert(zeile, "name", "bauteil", "rolle");
        const radius = wert(zeile, "radius");
        if (name && radius) shapes.push({ name, radiusCss: radius, source: quelle, used: true });
      }
    }
    if (tabelle.spalten.includes("css")) {
      for (const zeile of tabelle.zeilen) {
        const name = wert(zeile, "effekt", "name");
        const css = wert(zeile, "css");
        if (!name || !css) continue;
        const art = schluessel(wert(zeile, "art", "kind"));
        const kind: FactEffect["kind"] = art.includes("schatten") || art.includes("shadow") ? "shadow"
          : art.includes("verlauf") || art.includes("gradient") ? "gradient"
          : art.includes("blur") || art.includes("weich") ? "blur"
          : art.includes("rand") || art.includes("stroke") ? "stroke" : "shadow";
        effects.push({ name, kind, css, source: quelle });
      }
    }
  }
  return { shapes, effects };
}

const zielListe = (roh: string): string[] =>
  roh.split(/[,;]/).map((teil) => rein(teil)).filter((teil) => teil && teil !== "—" && teil !== "-");

function bildschirme(uiSpec: string, quelle: string): FactScreen[] {
  const tabelle = ersteTabelle(findeAbschnitt(uiSpec, "Bildschirme"), "bildschirm");
  if (!tabelle) return [];
  return tabelle.zeilen.flatMap((zeile, index) => {
    const rohName = wert(zeile, "bildschirm", "name");
    if (!rohName) return [];
    // Die Kennung aus dem Spec IST die Bildschirm-Kennung im spaeteren Markup: `composeScreens`
    // schreibt sie als `data-screen-id`. Damit ueberlebt sie jede Umgestaltung im Studio, und der
    // Rueckimport muss nicht mehr ueber Namen raten, was neu ist und was nur umbenannt wurde.
    const kennung = wert(zeile, "kennung", "id") || `B-${String(index + 1).padStart(2, "0")}`;
    const name = rohName.replace(/\s*\([^)]*\)\s*$/, "").trim() || kennung;
    return [{
      id: kennung,
      name,
      kind: "spec",
      source: quelle,
      navigatesTo: zielListe(wert(zeile, "fuhrtzu", "fuhrtzukennungen", "ziel", "navigiertzu")),
      isStart: /^(ja|yes|x|start)$/i.test(wert(zeile, "start", "startbildschirm")),
      files: [quelle]
    }];
  });
}

function texte(uiSpec: string): DesignFacts["strings"] {
  const tabelle = ersteTabelle(findeAbschnitt(uiSpec, "Texte"), "name");
  if (!tabelle) return [];
  return tabelle.zeilen.flatMap((zeile) => {
    const name = wert(zeile, "name", "rolle");
    const roh = wert(zeile, "text", "wert");
    if (!name || !roh) return [];
    let value = roh;
    if (/^".*"$/.test(roh)) { try { value = JSON.parse(roh) as string; } catch { /* Rohtext behalten */ } }
    return [{ name, value }];
  });
}

// Der Designer soll die Bewegungen MITBAUEN, nicht nur nachtraeglich erfahren. Deshalb wird jede
// Bewegung als CSS-Effekt uebergeben — in genau der Schreibweise, die der Aufbau spaeter erwartet.
function bewegungen(motionSpec: string, quelle: string): FactEffect[] {
  const tabelle = ersteTabelle(findeAbschnitt(motionSpec, "Kurven", "Dauern"), "bewegung");
  if (!tabelle) return [];
  return tabelle.zeilen.flatMap((zeile) => {
    const bewegung = wert(zeile, "bewegung", "name");
    if (!bewegung) return [];
    const kennung = wert(zeile, "kennung", "id");
    const dauer = zahl(wert(zeile, "dauer", "dauerms"));
    const kurve = wert(zeile, "kurve", "easing") || "ease";
    const wiederholung = schluessel(wert(zeile, "wiederholung", "repeat"));
    const endlos = wiederholung.includes("endlos") || wiederholung.includes("infinite") || wiederholung.includes("dauer");
    // Der @keyframes-Name traegt die Kennung: damit ist die Bewegung im fertigen HTML wiederfindbar
    // und der Rueckimport kann sie derselben M-Kennung zuordnen wie in v1.
    const name = kennung ? `${kennung.toLowerCase()}-${schluessel(bewegung) || "bewegung"}` : schluessel(bewegung);
    return [{
      name: kennung ? `${kennung} ${bewegung}` : bewegung,
      kind: "animation" as const,
      css: `animation: ${name} ${dauer ?? 300}ms ${kurve}${endlos ? " infinite" : ""}`,
      source: quelle
    }];
  });
}

// Die Funktionen selbst gehoeren nicht zu den Design-Fakten — aber ihre Kennungen schon: der Aufbau
// soll `data-werft-funktion="F-07"` an das Bedienelement schreiben, das eine bekannte Funktion
// ausloest. Ohne diese Liste kann er das nicht, und beim Ruecklauf faellt jedes Element wieder in
// die Beschriftungs-Raterei zurueck.
export function funktionsListe(funktionsSpec: string): Array<{ id: string; name: string }> {
  const ausUeberschriften = [...funktionsSpec.matchAll(/^#{2,4}\s*(F-\d+)\s*[—–-]\s*(.+?)\s*$/gm)]
    .map((treffer) => ({ id: treffer[1]!, name: rein(treffer[2]!) }));
  if (ausUeberschriften.length) return ausUeberschriften;
  // Kurze Specs fuehren die Funktionen nur in der Uebersichtstabelle, ohne eigenen Abschnitt.
  const tabelle = ersteTabelle(findeAbschnitt(funktionsSpec, "Ueberblick", "Überblick", "Funktionen"), "kennung");
  return tabelle
    ? tabelle.zeilen.flatMap((zeile) => {
        const id = wert(zeile, "kennung", "id");
        const name = wert(zeile, "funktion", "name");
        return /^F-\d+$/i.test(id) && name ? [{ id: id.toUpperCase(), name }] : [];
      })
    : [];
}

// ---------------------------------------------------------------- Zusammenfuehren

export function extractSpecFacts(files: SourceText[]): Partial<DesignFacts> {
  const lies = (muster: RegExp): SourceText | undefined => files.find((file) => muster.test(file.path));
  const sammel = lies(sammelSpec);
  const uiDatei = lies(/02-UI-SPEC\.md$/i) ?? sammel;
  const motionDatei = lies(/03-MOTION-SPEC\.md$/i) ?? sammel;
  if (!uiDatei && !motionDatei) return {};
  const uiText = uiDatei?.text ?? "";
  const uiQuelle = uiDatei?.path ?? "Spec";
  const { themes, colors } = erscheinungen(uiText, uiQuelle);
  const { shapes, effects } = formenUndEffekte(uiText, uiQuelle);
  const screens = bildschirme(uiText, uiQuelle);
  const motion = motionDatei ? bewegungen(motionDatei.text, motionDatei.path) : [];
  const notes = [`Design-Fakten stammen aus dem Spec-Paket (${files.map((file) => file.path).join(", ")}), nicht aus Quellcode: die Software existiert noch nicht.`];
  if (!screens.length) notes.push("Im UI-Spec war keine Bildschirm-Tabelle lesbar — der Aufbau faellt auf einen einzelnen Bildschirm zurueck.");
  const funktionsDatei = lies(/01-FUNKTIONS-SPEC\.md$/i) ?? sammel;
  const funktionen = funktionsDatei ? funktionsListe(funktionsDatei.text) : [];
  if (funktionen.length) {
    notes.push(`Funktionen aus dem Spec — das ausloesende Bedienelement traegt data-werft-funktion mit dieser Kennung: ${funktionen.map((funktion) => `${funktion.id} = ${funktion.name}`).join("; ")}`);
  }
  return {
    themes, colors, shapes, screens,
    typography: typografie(uiText, uiQuelle),
    dimensions: masse(uiText, uiQuelle),
    effects: [...effects, ...motion],
    strings: texte(uiText),
    notes
  };
}
