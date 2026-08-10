/**
 * Der Bauplan — die Layout-HIERARCHIE eines Bildschirms.
 *
 * Warum es das gibt: Bis hierher hat der Export Koordinaten mitgegeben (x, y, Breite, Hoehe),
 * gemessen bei einer bestimmten Fensterbreite. Eine Koordinate gilt aber nur fuer genau diese
 * Breite. Wird bei einer anderen Breite gemessen — oder greift eine Media Query beim Betrachter
 * anders als im Studio —, dann beschreibt sie eine Anordnung, die es im Entwurf nie gab. Genau so
 * ist ein Einstellungsbildschirm mit „Beschriftung ueber dem Feld" als „Beschriftung neben dem
 * Feld" im Code gelandet, ohne dass irgendwo ein Fehler auftauchte.
 *
 * Eine Hierarchie hat dieses Problem nicht. „Diese Karte ist eine Spalte, darin zuerst die
 * Beschriftung, dann das Feld, 6 dp Abstand" bleibt wahr, egal wie breit das Fenster ist.
 *
 * Das ist derselbe Weg, den Claude Design geht: sein Handoff-Buendel enthaelt die
 * Komponentenstruktur als maschinenlesbare Spezifikation, die tatsaechlich benutzten Tokens, die
 * Layout-Hierarchie und die Asset-Verweise — und ausdruecklich kein Bild und keinen
 * verlustbehafteten Export. Gemessen belegt ist der Unterschied auch: Werkzeuge, die von der
 * Dateistruktur arbeiten, treffen den Entwurf deutlich genauer als Werkzeuge, die von einem
 * Screenshot arbeiten (die verlieren „exact spacing values, specific font weights and subtle
 * interaction details").
 *
 * Der Bauplan ersetzt die Messung nicht — er steht ueber ihr. Die Messung loest Vererbung,
 * `var()` und `color-mix()` auf und liefert die Zahlenwerte; der Bauplan sagt, wie die Teile
 * zueinander stehen. Bei Widerspruch gilt der Bauplan, denn die Messung ist aus ihm abgeleitet.
 */

/** Wie ein Element seine Kinder anordnet. */
export type Anordnung = {
  art: "spalte" | "zeile" | "raster" | "fluss";
  /** Abstand zwischen den Kindern, wie im Entwurf gesetzt (`gap`/`row-gap`/`column-gap`). */
  abstand?: string;
  /** Bei Raster: die Spaltenvorgabe, woertlich (`grid-template-columns`). */
  spalten?: string;
  /** Ausrichtung quer zur Laufrichtung (`align-items`). */
  quer?: string;
  /** Ausrichtung in Laufrichtung (`justify-content`). */
  laengs?: string;
  /** Innenabstand des Elements (`padding`). */
  innen?: string;
};

export type Bauteil = {
  tag: string;
  klassen?: string;
  /** `data-werft-funktion` — welche Funktion an diesem Bedienelement haengt. */
  funktion?: string;
  /** `data-werft-navigate` — wohin es fuehrt. */
  fuehrtZu?: string;
  /** `aria-label` — die Beschriftung fuer Barrierefreiheit. */
  beschriftung?: string;
  /** Eigener Text des Elements (ohne den Text der Kinder). */
  text?: string;
  platzhalter?: string;
  /** `hidden` — ein ZUSTAND des Bildschirms, kein Wegfall. */
  versteckt?: boolean;
  /** Bei `<select>`: alle waehlbaren Eintraege. Ohne sie baut ein Modell ein anderes Bauteil. */
  eintraege?: string[];
  /** Bei `<input type="range">`: die Grenzen, damit kein Knopfsatz daraus wird. */
  bereich?: { von?: string; bis?: string; schritt?: string; wert?: string };
  anordnung?: Anordnung;
  kinder?: Bauteil[];
};

export type Bildschirmbauplan = {
  bildschirm: string;
  name: string;
  /** Die Breite, fuer die dieser Bauplan gilt — er ist damit nachpruefbar. */
  breite?: number;
  erscheinung: string;
  baum: Bauteil[];
};

const selbstschliessend = new Set(["br", "hr", "img", "input", "meta", "link", "source", "track", "wbr", "area", "base", "col", "embed", "param"]);

/** Ein Attribut aus einem Start-Tag holen (einfache und doppelte Anfuehrungszeichen). */
function attribut(tag: string, name: string): string | undefined {
  const treffer = new RegExp(`\\b${name}\\s*=\\s*("([^"]*)"|'([^']*)')`, "i").exec(tag);
  const wert = treffer?.[2] ?? treffer?.[3];
  return wert?.trim() ? wert.trim() : undefined;
}

/**
 * Die fuer eine Klassenliste geltenden Layout-Eigenschaften aus der CSS zusammensetzen.
 *
 * Bewusst schlicht: es zaehlt die Reihenfolge im Stylesheet (spaeter gewinnt), genau wie im
 * Browser bei gleicher Spezifitaet. Die uebergebene CSS ist die fuer die Zielbreite bereits
 * aufgeloeste Fassung — dadurch steht hier die Anordnung, die der Entwerfer wirklich sieht.
 */
export function layoutFuer(css: string, klassen: string[]): Record<string, string> {
  const gefunden: Record<string, string> = {};
  const interessant = ["display", "flex-direction", "gap", "row-gap", "column-gap", "grid-template-columns", "align-items", "justify-content", "padding"];
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
    if (/^@/.test(selector)) continue;
    // Trifft der Selektor eine der Klassen? Nur einfache Klassen-Selektoren — zusammengesetzte
    // Zustands-Selektoren (`:hover`, `[data-…]`) gehoeren nicht in den Grundaufbau.
    const trifft = selector.split(",").some((teil) => {
      const t = teil.trim();
      if (/[:\[>+~]/.test(t)) return false;
      const letzte = t.split(/\s+/).pop() ?? "";
      return klassen.some((k) => letzte === `.${k}`);
    });
    if (!trifft) continue;
    for (const eigenschaft of interessant) {
      const wert = new RegExp(`(?:^|;)\\s*${eigenschaft}\\s*:\\s*([^;}]+)`, "i").exec(body)?.[1]?.trim();
      if (wert) gefunden[eigenschaft] = wert.replace(/\s*!important$/i, "");
    }
  }
  return gefunden;
}

/** Aus den Layout-Eigenschaften die Anordnung ableiten. */
export function anordnungAus(layout: Record<string, string>): Anordnung | undefined {
  const display = layout["display"] ?? "";
  const richtung = layout["flex-direction"] ?? "";
  const abstand = layout["gap"] ?? layout["row-gap"] ?? layout["column-gap"];

  let art: Anordnung["art"] = "fluss";
  if (/grid/.test(display)) {
    // Ein Raster mit genau EINER Spalte ist in Wahrheit eine Spalte — und das ist die Aussage,
    // auf die es ankommt: „Beschriftung UEBER dem Feld" statt „daneben".
    const spalten = layout["grid-template-columns"] ?? "";
    const anzahl = spalten.trim() ? spalten.trim().split(/\s+(?![^(]*\))/).length : 1;
    art = anzahl <= 1 ? "spalte" : "raster";
  } else if (/flex/.test(display)) {
    art = /column/.test(richtung) ? "spalte" : "zeile";
  }

  const ergebnis: Anordnung = { art };
  if (abstand) ergebnis.abstand = abstand;
  if (art === "raster" && layout["grid-template-columns"]) ergebnis.spalten = layout["grid-template-columns"];
  if (layout["align-items"]) ergebnis.quer = layout["align-items"];
  if (layout["justify-content"]) ergebnis.laengs = layout["justify-content"];
  if (layout["padding"]) ergebnis.innen = layout["padding"];
  return art === "fluss" && !abstand && !layout["padding"] ? undefined : ergebnis;
}

/**
 * Den HTML-Ausschnitt eines Bildschirms in einen Bauteil-Baum uebersetzen.
 *
 * Kein vollstaendiger HTML-Parser, sondern derselbe schlichte Weg, den der Export schon fuer die
 * Bildschirm-Abschnitte nutzt: Start-Tags suchen, Tiefe zaehlen, Text dazwischen mitnehmen. Das
 * traegt, weil das Markup aus dem Studio selbst kommt und wohlgeformt ist.
 */
export function baumAus(html: string, css: string): Bauteil[] {
  const wurzel: Bauteil[] = [];
  const stapel: Bauteil[][] = [wurzel];
  const offen: Bauteil[] = [];
  const tagMuster = /<(\/?)([a-zA-Z][\w-]*)([^>]*?)(\/?)>|<!--[\s\S]*?-->/g;
  let letzterIndex = 0;
  let treffer: RegExpExecArray | null;

  const textDazu = (bis: number) => {
    const roh = html.slice(letzterIndex, bis).replace(/\s+/g, " ").trim();
    if (!roh) return;
    const ziel = offen[offen.length - 1];
    if (ziel && !ziel.text) ziel.text = roh;
  };

  while ((treffer = tagMuster.exec(html))) {
    if (treffer[0].startsWith("<!--")) { letzterIndex = tagMuster.lastIndex; continue; }
    textDazu(treffer.index);
    letzterIndex = tagMuster.lastIndex;
    const schliessend = treffer[1] === "/";
    const tag = treffer[2]!.toLowerCase();
    const attribute = treffer[3] ?? "";

    if (tag === "script" || tag === "style") {
      // Inhalt ueberspringen — er gehoert nicht zum Aufbau.
      const ende = new RegExp(`</${tag}\\s*>`, "i").exec(html.slice(tagMuster.lastIndex));
      if (!schliessend && ende) { tagMuster.lastIndex += ende.index + ende[0].length; letzterIndex = tagMuster.lastIndex; }
      continue;
    }

    if (schliessend) {
      if (offen.length) { offen.pop(); stapel.pop(); }
      continue;
    }

    const klassen = attribut(attribute, "class");
    const layout = klassen ? layoutFuer(css, klassen.split(/\s+/).filter(Boolean)) : {};
    const bauteil: Bauteil = { tag };
    if (klassen) bauteil.klassen = klassen;
    const funktion = attribut(attribute, "data-werft-funktion");
    if (funktion) bauteil.funktion = funktion;
    const ziel = attribut(attribute, "data-werft-navigate");
    if (ziel) bauteil.fuehrtZu = ziel;
    const label = attribut(attribute, "aria-label");
    if (label) bauteil.beschriftung = label;
    const platzhalter = attribut(attribute, "placeholder");
    if (platzhalter) bauteil.platzhalter = platzhalter;
    if (/\bhidden\b/i.test(attribute)) bauteil.versteckt = true;

    // Ein Schieberegler muss als Schieberegler ankommen, nicht als Knopfsatz.
    if (tag === "input" && /type\s*=\s*["']?range/i.test(attribute)) {
      const bereich: NonNullable<Bauteil["bereich"]> = {};
      const von = attribut(attribute, "min");
      const bis = attribut(attribute, "max");
      const schritt = attribut(attribute, "step");
      const wert = attribut(attribute, "value");
      if (von) bereich.von = von;
      if (bis) bereich.bis = bis;
      if (schritt) bereich.schritt = schritt;
      if (wert) bereich.wert = wert;
      if (Object.keys(bereich).length) bauteil.bereich = bereich;
    }

    const anordnung = anordnungAus(layout);
    if (anordnung) bauteil.anordnung = anordnung;

    stapel[stapel.length - 1]!.push(bauteil);

    const leer = selbstschliessend.has(tag) || treffer[4] === "/";
    if (!leer) {
      bauteil.kinder = [];
      stapel.push(bauteil.kinder);
      offen.push(bauteil);
    }
  }

  // Auswahllisten: die Eintraege aus den Kindern einsammeln und die Kinder danach fallenlassen —
  // sie sind Inhalt der Liste, kein eigener Aufbau.
  const listenFalten = (teile: Bauteil[]) => {
    for (const teil of teile) {
      if (teil.tag === "select" && teil.kinder?.length) {
        teil.eintraege = teil.kinder.filter((k) => k.tag === "option").map((k) => k.text ?? "").filter(Boolean);
        delete teil.kinder;
        continue;
      }
      if (teil.kinder?.length) listenFalten(teil.kinder);
      else delete teil.kinder;
    }
  };
  listenFalten(wurzel);
  return wurzel;
}

/** Der Bauplan eines Bildschirms in einer Erscheinung. */
export function bauplanFuer(input: {
  kennung: string;
  name: string;
  html: string;
  css: string;
  breite?: number;
  erscheinung: string;
}): Bildschirmbauplan {
  return {
    bildschirm: input.kennung,
    name: input.name,
    ...(input.breite ? { breite: input.breite } : {}),
    erscheinung: input.erscheinung,
    baum: baumAus(input.html, input.css),
  };
}
