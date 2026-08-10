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
 * ALLE Attribute eines Start-Tags einsammeln — auch die wertlosen wie `hidden`.
 *
 * Sie sind nicht Beiwerk, sondern Teil des Selektor-Abgleichs: Werft Studio adressiert seine
 * Bildschirme ueber `[data-screen-id="B-08"]`, und ein Claude-Design-Paket traegt sein Layout
 * ueberwiegend im `style`-Attribut. Ohne diese Werte bliebe beides unsichtbar.
 */
export function attributeAus(tagInhalt: string): Record<string, string> {
  const gefunden: Record<string, string> = {};
  for (const treffer of tagInhalt.matchAll(/([\w:-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+)))?/g)) {
    const name = treffer[1]!.toLowerCase();
    if (name === "/") continue;
    gefunden[name] = (treffer[2] ?? treffer[3] ?? treffer[4] ?? "").trim();
  }
  return gefunden;
}

/** Ein Element, so wie der Selektor-Abgleich es braucht. */
export type Knoten = { tag: string; klassen: string[]; id?: string; attribute: Record<string, string> };

const layoutEigenschaften = ["display", "flex-direction", "gap", "row-gap", "column-gap", "grid-template-columns", "align-items", "justify-content", "padding"];

// Zustands-Pseudoklassen beschreiben NICHT den Grundaufbau, sondern einen Zustand (gedrueckt,
// ueberfahren, ausgewaehlt). Sie bleiben bewusst ausgeschlossen — sonst stuende im Bauplan die
// Anordnung des Hover-Zustands als die gewoehnliche.
const zustandsPseudo = /::?(?:hover|active|focus|focus-within|focus-visible|checked|disabled|enabled|target|visited|link|before|after|placeholder|selection|first-line|first-letter|backdrop|marker)\b/i;

/** Ein einzelnes Selektor-Glied (`div.karte[data-x="y"]`) gegen ein Element pruefen. */
function gliedTrifft(glied: string, knoten: Knoten): boolean {
  let rest = glied.trim();
  if (!rest || rest === "*") return true;
  // `:is(a, b)` und `:where(a, b)` sind reine Schreibabkuerzungen — in Alternativen aufloesen.
  const gruppe = /^(.*?):(?:is|where)\(([^()]*)\)(.*)$/i.exec(rest);
  if (gruppe) {
    return gruppe[2]!.split(",").some((alternative) => gliedTrifft(`${gruppe[1]}${alternative.trim()}${gruppe[3]}`, knoten));
  }
  const nicht = /^(.*?):not\(([^()]*)\)(.*)$/i.exec(rest);
  if (nicht) {
    if (nicht[2]!.split(",").some((alternative) => gliedTrifft(alternative.trim(), knoten))) return false;
    rest = `${nicht[1]}${nicht[3]}`;
    if (!rest.trim()) return true;
  }
  // Attribut-Bedingungen zuerst herausnehmen — sie enthalten Zeichen, die sonst stoeren.
  for (const treffer of [...rest.matchAll(/\[\s*([\w-]+)\s*(?:([~^$*|]?=)\s*(?:"([^"]*)"|'([^']*)'|([^\]]*))\s*)?\]/g)]) {
    const name = treffer[1]!.toLowerCase();
    if (!(name in knoten.attribute)) return false;
    const erwartet = (treffer[3] ?? treffer[4] ?? treffer[5] ?? "").trim();
    if (!treffer[2]) continue;                      // `[hidden]` — Vorhandensein genuegt
    const ist = knoten.attribute[name] ?? "";
    const passt = treffer[2] === "=" ? ist === erwartet
      : treffer[2] === "^=" ? ist.startsWith(erwartet)
      : treffer[2] === "$=" ? ist.endsWith(erwartet)
      : treffer[2] === "*=" ? ist.includes(erwartet)
      : treffer[2] === "~=" ? ist.split(/\s+/).includes(erwartet)
      : treffer[2] === "|=" ? ist === erwartet || ist.startsWith(`${erwartet}-`)
      : false;
    if (!passt) return false;
  }
  rest = rest.replace(/\[[^\]]*\]/g, "");
  for (const klasse of rest.match(/\.[\w-]+/g) ?? []) if (!knoten.klassen.includes(klasse.slice(1))) return false;
  const id = /#([\w-]+)/.exec(rest);
  if (id && knoten.id !== id[1]) return false;
  const tag = /^([a-zA-Z][\w-]*)/.exec(rest.replace(/[.#][\w-]+/g, ""));
  if (tag && knoten.tag !== "*" && tag[1]!.toLowerCase() !== knoten.tag) return false;
  return true;
}

/**
 * Einen vollstaendigen Selektor gegen die Vorfahren-Kette pruefen (von rechts nach links).
 *
 * `kette` laeuft von der Wurzel bis zum Element; das letzte Glied ist das Element selbst.
 * Unterstuetzt werden Nachfahren (Leerzeichen) und direkte Kinder (`>`). Geschwister-Kombinatoren
 * (`+`, `~`) beschreiben eine Reihenfolge, die der Baum hier nicht mitfuehrt — sie werden
 * uebersprungen statt falsch geraten.
 */
export function selektorTrifft(selektor: string, kette: Knoten[]): boolean {
  if (!kette.length) return false;
  if (zustandsPseudo.test(selektor)) return false;
  if (/[+~]/.test(selektor.replace(/\[[^\]]*\]/g, ""))) return false;
  const glieder = selektor.trim().split(/\s*(>)\s*|\s+/).filter((teil) => teil && teil.trim());
  let ebene = kette.length - 1;
  let direkt = false;
  for (let i = glieder.length - 1; i >= 0; i -= 1) {
    const glied = glieder[i]!;
    if (glied === ">") { direkt = true; continue; }
    if (ebene < 0) return false;
    if (direkt) {
      if (!gliedTrifft(glied, kette[ebene]!)) return false;
      ebene -= 1;
    } else {
      let gefunden = -1;
      for (let suche = ebene; suche >= 0; suche -= 1) if (gliedTrifft(glied, kette[suche]!)) { gefunden = suche; break; }
      if (gefunden < 0) return false;
      ebene = gefunden - 1;
    }
    direkt = false;
  }
  return true;
}

/** Die Spezifitaet eines Selektors als eine vergleichbare Zahl (IDs, dann Klassen, dann Tags). */
export function spezifitaet(selektor: string): number {
  const ohneAttribute = selektor.replace(/\[[^\]]*\]/g, "");
  const ids = (ohneAttribute.match(/#[\w-]+/g) ?? []).length;
  const klassen = (ohneAttribute.match(/\.[\w-]+/g) ?? []).length + (selektor.match(/\[[^\]]*\]/g) ?? []).length;
  const tags = (ohneAttribute.replace(/[.#][\w-]+/g, "").match(/(?:^|[\s>+~])([a-zA-Z][\w-]*)/g) ?? []).length;
  return ids * 10000 + klassen * 100 + tags;
}

/**
 * Die fuer ein Element geltenden Layout-Eigenschaften aus der CSS zusammensetzen.
 *
 * Warum das genauer sein muss als ein Abgleich auf Klassennamen: Die fruehere Fassung hat JEDEN
 * Selektor verworfen, der `[`, `>` oder `:` enthielt, und nur solche gelesen, deren letztes Glied
 * ein reiner Klassen-Selektor war. Damit fiel genau die Schicht heraus, die im Browser GEWINNT —
 * Werft Studio selbst schreibt seine Bildschirm-Regeln als
 * `.werft-screen[data-screen-id="B-08"] .werft-b08__section { padding: 20px }`. Gemessen an einem
 * echten Export gingen so 42 von 371 Layout-Angaben verloren (11,3 %), und zwar die spezifischeren.
 *
 * Noch deutlicher bei einem Uebergabepaket von Claude Design: dort steht das Layout ueberwiegend in
 * `style`-Attributen am Element (nachgezaehlt an einem echten Buendel: 234 `style`-Attribute, davon
 * 66 mit `display`, 72 mit `padding`, 45 mit `gap` — und KEINE einzige CSS-Klasse). Ein Bauplan, der
 * Inline-Stile nicht liest, waere dort vollstaendig leer.
 *
 * Deshalb wird hier wie im Browser gearbeitet: alle Regeln, die auf die Vorfahren-Kette passen,
 * werden nach Spezifitaet und Reihenfolge sortiert angewandt; danach gewinnt der Inline-Stil, und
 * `!important` gewinnt ueber alles.
 */
export function layoutFuerKette(css: string, kette: Knoten[]): Record<string, string> {
  const treffer: Array<{ gewicht: number; reihenfolge: number; body: string }> = [];
  let index = 0, reihenfolge = 0;
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
    reihenfolge += 1;
    if (/^@/.test(selector)) continue;
    // Von allen Alternativen einer Selektor-Liste zaehlt die, die passt — mit ihrer Spezifitaet.
    let bestes = -1;
    for (const teil of selector.split(",")) {
      const t = teil.trim();
      if (!t || !selektorTrifft(t, kette)) continue;
      bestes = Math.max(bestes, spezifitaet(t));
    }
    if (bestes < 0) continue;
    treffer.push({ gewicht: bestes, reihenfolge, body });
  }
  treffer.sort((links, rechts) => links.gewicht - rechts.gewicht || links.reihenfolge - rechts.reihenfolge);

  const gefunden: Record<string, string> = {};
  const wichtig = new Set<string>();
  const uebernehmen = (body: string, inline: boolean) => {
    for (const eigenschaft of layoutEigenschaften) {
      const roh = new RegExp(`(?:^|;)\\s*${eigenschaft}\\s*:\\s*([^;}]+)`, "i").exec(body)?.[1]?.trim();
      if (!roh) continue;
      const istWichtig = /!important\s*$/i.test(roh);
      if (wichtig.has(eigenschaft) && !istWichtig) continue;
      if (istWichtig) wichtig.add(eigenschaft);
      gefunden[eigenschaft] = roh.replace(/\s*!important\s*$/i, "").trim();
      void inline;
    }
  };
  for (const eintrag of treffer) uebernehmen(eintrag.body, false);
  // Der Inline-Stil steht ueber jeder Regel aus dem Stylesheet — und ist bei einem
  // Claude-Design-Paket die Hauptquelle des Layouts.
  const inline = kette[kette.length - 1]?.attribute["style"];
  if (inline) uebernehmen(inline, true);
  return gefunden;
}

/**
 * Kurzform fuer den Abgleich auf eine blosse Klassenliste — ohne Vorfahren-Kette.
 * Bleibt erhalten, damit bestehende Aufrufer und Tests unveraendert weiterlaufen.
 */
export function layoutFuer(css: string, klassen: string[]): Record<string, string> {
  return layoutFuerKette(css, [{ tag: "*", klassen, attribute: {} }]);
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
  // Die Vorfahren-Kette. Ohne sie kann ein Selektor wie
  // `.werft-screen[data-screen-id="B-08"] .werft-b08__section` nicht geprueft werden — und genau
  // diese Regeln sind die, die im Browser gewinnen.
  const kette: Knoten[] = [];
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
      if (offen.length) { offen.pop(); stapel.pop(); kette.pop(); }
      continue;
    }

    const klassen = attribut(attribute, "class");
    const knoten: Knoten = {
      tag,
      klassen: klassen ? klassen.split(/\s+/).filter(Boolean) : [],
      ...(attribut(attribute, "id") ? { id: attribut(attribute, "id")! } : {}),
      attribute: attributeAus(attribute)
    };
    // Der Abgleich braucht das Element MIT seinen Vorfahren — sonst bleiben die spezifischeren
    // Regeln (und der Inline-Stil) unberuecksichtigt.
    const layout = layoutFuerKette(css, [...kette, knoten]);
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
      kette.push(knoten);
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
