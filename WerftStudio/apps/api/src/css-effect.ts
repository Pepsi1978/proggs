// Die Pipeline hat bisher geprüft, ob TEXT ersetzt wurde — nicht, ob die Änderung WIRKT. Genau daran
// scheiterte ein Lauf, der „30 Änderungen übernommen" meldete, während im Design nichts zu sehen war:
// das Modell hatte `.pm-start__hook { padding: 18px }` geändert, aber im Dokument steht
// `.pm-start-screen button { padding: 0 }` — spezifischer, also gewinnt der Reset. Die Regel sah
// richtig aus und war tot.
//
// Dieser Baustein rechnet die CSS-Kaskade nach: Er liest die Stilblöcke, baut einen schlanken
// Elementbaum aus dem Markup, ermittelt für jede geänderte Deklaration die Elemente, die sie treffen
// soll, und prüft, wer für diese Elemente wirklich gewinnt. Das Ergebnis geht als konkrete
// Rückmeldung an das Modell — und als ehrliche Auskunft an den Benutzer.

export type CssDeclaration = { property: string; value: string; important: boolean };
export type CssRule = { selectors: string[]; declarations: CssDeclaration[]; media: string; order: number };

const stateWords = /:(?:hover|active|focus|focus-visible|focus-within|visited|target|disabled|checked|placeholder|before|after|first-line|first-letter|selection|backdrop|marker)/i;

// ---------------------------------------------------------------- CSS lesen

export function styleBlocksOf(html: string): string {
  return [...html.matchAll(/<style\b[^>]*>([\s\S]*?)<\/style\s*>/gi)].map((match) => match[1]!).join("\n");
}

const stripComments = (css: string) => css.replace(/\/\*[\s\S]*?\*\//g, "");

export function parseCss(css: string, media = "", start = { order: 0 }): CssRule[] {
  const rules: CssRule[] = [];
  const text = media ? css : stripComments(css);
  let index = 0;
  while (index < text.length) {
    const open = text.indexOf("{", index);
    if (open < 0) break;
    const head = text.slice(index, open).trim();
    let depth = 1, cursor = open + 1;
    while (cursor < text.length && depth > 0) {
      if (text[cursor] === "{") depth += 1;
      else if (text[cursor] === "}") depth -= 1;
      cursor += 1;
    }
    const body = text.slice(open + 1, cursor - 1);
    if (head.startsWith("@")) {
      // Bedingungsblöcke behalten ihre Bedingung; @keyframes und @font-face tragen keine Kaskade bei.
      if (/^@(?:media|supports|layer|container)\b/i.test(head)) rules.push(...parseCss(body, head, start));
    } else if (head) {
      const declarations = parseDeclarations(body);
      if (declarations.length) rules.push({ selectors: head.split(",").map((entry) => entry.trim()).filter(Boolean), declarations, media, order: start.order++ });
    }
    index = cursor;
  }
  return rules;
}

export function parseDeclarations(body: string): CssDeclaration[] {
  const declarations: CssDeclaration[] = [];
  let depth = 0, current = "";
  const push = (part: string) => {
    const separator = part.indexOf(":");
    if (separator < 0) return;
    const property = part.slice(0, separator).trim().toLowerCase();
    let value = part.slice(separator + 1).trim();
    if (!property || !value || property.startsWith("/*")) return;
    const important = /!\s*important$/i.test(value);
    if (important) value = value.replace(/!\s*important$/i, "").trim();
    declarations.push({ property, value, important });
  };
  for (const char of body) {
    if (char === "(") depth += 1;
    else if (char === ")") depth = Math.max(0, depth - 1);
    if (char === ";" && depth === 0) { push(current); current = ""; continue; }
    current += char;
  }
  push(current);
  return declarations;
}

// ---------------------------------------------------------------- Spezifität

export function specificity(selector: string): [number, number, number] {
  const clean = selector.replace(/::[\w-]+/g, "").replace(/:not\(([^)]*)\)/g, " $1 ");
  const ids = (clean.match(/#[\w-]+/g) ?? []).length;
  const classes = (clean.match(/\.[\w-]+/g) ?? []).length + (clean.match(/\[[^\]]+\]/g) ?? []).length + (clean.match(/:(?!:)[\w-]+/g) ?? []).length;
  const tags = (clean.replace(/[#.][\w-]+/g, " ").replace(/\[[^\]]+\]/g, " ").replace(/:(?!:)[\w-]+(\([^)]*\))?/g, " ").match(/(^|[\s>+~])([a-zA-Z][\w-]*)/g) ?? []).length;
  return [ids, classes, tags];
}

const beats = (left: [number, number, number], right: [number, number, number]) =>
  left[0] !== right[0] ? left[0] > right[0] : left[1] !== right[1] ? left[1] > right[1] : left[2] > right[2];

// ---------------------------------------------------------------- Markup lesen

export type DomNode = { tag: string; id: string; classes: Set<string>; attrs: Record<string, string>; parent?: DomNode; screen?: string };

const voidTags = new Set(["area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"]);

// Ein bewusst schlanker Markup-Leser: Tag, Kennung, Klassen, Attribute und die Elternkette. Mehr
// braucht die Kaskadenprüfung nicht, und ein vollwertiger Parser wäre hier nur Ballast.
export function parseDom(html: string): DomNode[] {
  const nodes: DomNode[] = [];
  const stack: DomNode[] = [];
  const tagPattern = /<(\/?)([a-zA-Z][\w-]*)((?:"[^"]*"|'[^']*'|[^>"'])*)>/g;
  let match: RegExpExecArray | null;
  let skipUntil: string | undefined;
  while ((match = tagPattern.exec(html))) {
    const [, closing, rawTag, rawAttrs] = match;
    const tag = rawTag!.toLowerCase();
    if (skipUntil) { if (closing && tag === skipUntil) skipUntil = undefined; continue; }
    if (closing) { while (stack.length && stack.at(-1)!.tag !== tag) stack.pop(); stack.pop(); continue; }
    if (tag === "script" || tag === "style") { if (!(rawAttrs ?? "").trimEnd().endsWith("/")) skipUntil = tag; continue; }
    const attrs: Record<string, string> = {};
    for (const attr of (rawAttrs ?? "").matchAll(/([\w:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))/g)) {
      attrs[attr[1]!.toLowerCase()] = attr[2] ?? attr[3] ?? attr[4] ?? "";
    }
    const parent = stack.at(-1);
    const classes = new Set((attrs.class ?? "").split(/\s+/).filter(Boolean));
    const node: DomNode = { tag, id: attrs.id ?? "", classes, attrs, ...(parent ? { parent } : {}) };
    const screen = classes.has("werft-screen") ? (attrs["data-screen-name"] || attrs["data-screen-id"] || "") : parent?.screen;
    if (screen) node.screen = screen;
    nodes.push(node);
    if (!voidTags.has(tag) && !(rawAttrs ?? "").trimEnd().endsWith("/")) stack.push(node);
  }
  return nodes;
}

// ---------------------------------------------------------------- Selektoren treffen

type Compound = { tag?: string; id?: string; classes: string[]; attrs: Array<{ name: string; value?: string }>; stateful: boolean; unknown: boolean };

export function parseCompound(part: string): Compound {
  const compound: Compound = { classes: [], attrs: [], stateful: stateWords.test(part), unknown: false };
  let rest = part;
  for (const attr of rest.matchAll(/\[([\w:-]+)(?:([~^$*|]?=)"?([^\]"]*)"?)?\]/g)) compound.attrs.push({ name: attr[1]!.toLowerCase(), ...(attr[3] === undefined ? {} : { value: attr[3] }) });
  rest = rest.replace(/\[[^\]]*\]/g, "");
  // Zustands- und Strukturpseudos werden entfernt; `:not(...)` macht den Treffer unsicher.
  if (/:not\(|:is\(|:where\(|:has\(/i.test(rest)) compound.unknown = true;
  rest = rest.replace(/::?[\w-]+(\([^)]*\))?/g, "");
  for (const cls of rest.matchAll(/\.([\w-]+)/g)) compound.classes.push(cls[1]!);
  const id = /#([\w-]+)/.exec(rest);
  if (id) compound.id = id[1]!;
  const tag = /^([a-zA-Z][\w-]*)/.exec(rest);
  if (tag) compound.tag = tag[1]!.toLowerCase();
  if (rest.includes("*")) delete compound.tag;
  return compound;
}

const compoundMatches = (node: DomNode, compound: Compound): boolean => {
  if (compound.tag && node.tag !== compound.tag) return false;
  if (compound.id && node.id !== compound.id) return false;
  for (const cls of compound.classes) if (!node.classes.has(cls)) return false;
  for (const attr of compound.attrs) {
    const value = node.attrs[attr.name];
    if (value === undefined) return false;
    if (attr.value !== undefined && value !== attr.value) return false;
  }
  return true;
};

export type MatchResult = { matched: boolean; certain: boolean };

// Von rechts nach links, wie ein Browser. Geschwister-Kombinatoren werden als „unsicher" gemeldet,
// statt geraten zu werden — eine falsche Behauptung wäre schlimmer als keine.
export function selectorMatches(node: DomNode, selector: string): MatchResult {
  const parts = selector.trim().split(/\s*(>)\s*|\s+/).filter(Boolean);
  if (!parts.length) return { matched: false, certain: true };
  if (/[+~]/.test(selector)) return { matched: false, certain: false };
  let certain = true;
  let current: DomNode | undefined = node;
  let index = parts.length - 1;
  let child = false;
  const compound = parseCompound(parts[index]!);
  if (compound.unknown) certain = false;
  if (!compoundMatches(node, compound)) return { matched: false, certain };
  index -= 1;
  while (index >= 0) {
    const part = parts[index]!;
    if (part === ">") { child = true; index -= 1; continue; }
    const wanted = parseCompound(part);
    if (wanted.unknown) certain = false;
    if (child) {
      current = current?.parent;
      if (!current || !compoundMatches(current, wanted)) return { matched: false, certain };
      child = false;
    } else {
      let walker: DomNode | undefined = current?.parent;
      while (walker && !compoundMatches(walker, wanted)) walker = walker.parent;
      if (!walker) return { matched: false, certain };
      current = walker;
    }
    index -= 1;
  }
  return { matched: true, certain };
}

// ---------------------------------------------------------------- Kürzel auflösen

const shorthand: Record<string, string[]> = {
  padding: ["padding-top", "padding-right", "padding-bottom", "padding-left"],
  margin: ["margin-top", "margin-right", "margin-bottom", "margin-left"],
  inset: ["top", "right", "bottom", "left"],
  gap: ["row-gap", "column-gap"],
  "border-radius": ["border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius"],
  border: ["border-width", "border-style", "border-color"],
  background: ["background-color", "background-image", "background-position", "background-size", "background-repeat"],
  font: ["font-family", "font-size", "font-weight", "font-style", "line-height"],
  flex: ["flex-grow", "flex-shrink", "flex-basis"],
  overflow: ["overflow-x", "overflow-y"]
};

export function longhands(property: string): string[] {
  const parts = shorthand[property];
  if (parts) return [property, ...parts];
  for (const [kurz, lang] of Object.entries(shorthand)) if (lang.includes(property)) return [property, kurz];
  return [property];
}

const conflicts = (left: string, right: string) => {
  if (left === right) return true;
  const a = new Set(longhands(left)), b = longhands(right);
  return b.some((entry) => a.has(entry));
};

// ---------------------------------------------------------------- Wirkung prüfen

export type EffectStatus = "wirksam" | "ueberschrieben" | "kein-element" | "unsicher";
export type EffectFinding = {
  selector: string;
  property: string;
  value: string;
  status: EffectStatus;
  elements: number;
  screens: string[];
  beatenBy?: string;
  beatenValue?: string;
};

// Nur Regeln, die im Ruhezustand und auf dem Bildschirm gelten, entscheiden die Kaskade. Ein
// `:hover`-Zwilling oder ein Druck-Stylesheet darf nicht als Gewinner gemeldet werden.
const cascadeRelevant = (rule: CssRule, selector: string) => !stateWords.test(selector) && !/print/i.test(rule.media);

export function analyseEffect(html: string, changes: Array<{ selector: string; property: string; value: string; media: string; order: number }>): EffectFinding[] {
  const rules = parseCss(styleBlocksOf(html));
  const nodes = parseDom(html);
  // Vorsortierung: nur Regeln prüfen, deren rechtester Teil überhaupt zum Element passen kann.
  const schluessel = (selector: string) => {
    const last = selector.trim().split(/\s*>\s*|\s+/).at(-1) ?? selector;
    const compound = parseCompound(last);
    return compound.id ? `#${compound.id}` : compound.classes.length ? `.${compound.classes[0]}` : compound.tag ?? "*";
  };
  const index = new Map<string, Array<{ rule: CssRule; selector: string }>>();
  for (const rule of rules) for (const selector of rule.selectors) {
    if (!cascadeRelevant(rule, selector)) continue;
    const key = schluessel(selector);
    index.set(key, [...(index.get(key) ?? []), { rule, selector }]);
  }
  const kandidaten = (node: DomNode) => [
    ...(node.id ? index.get(`#${node.id}`) ?? [] : []),
    ...[...node.classes].flatMap((cls) => index.get(`.${cls}`) ?? []),
    ...(index.get(node.tag) ?? []),
    ...(index.get("*") ?? [])
  ];
  const findings: EffectFinding[] = [];
  for (const change of changes) {
    const treffer = nodes.filter((node) => selectorMatches(node, change.selector).matched);
    if (!treffer.length) { findings.push({ selector: change.selector, property: change.property, value: change.value, status: "kein-element", elements: 0, screens: [] }); continue; }
    const screens = [...new Set(treffer.map((node) => node.screen).filter((name): name is string => Boolean(name)))];
    const eigene = specificity(change.selector);
    let unsicher = false;
    // Geprüft wird JE ELEMENT. Eine Regel, die nur an einer Sonderausprägung scheitert (etwa an einer
    // Variante mit Zusatzklasse), ist für die übrigen Elemente sehr wohl wirksam — sie als „tot" zu
    // melden wäre ein Fehlalarm, der das Modell auf eine falsche Fährte schickt.
    const stichprobe = treffer.slice(0, 12);
    const verloren: Array<{ selector: string; value: string }> = [];
    for (const node of stichprobe) {
      let sieger: { selector: string; value: string } | undefined;
      for (const { rule, selector } of kandidaten(node)) {
        const passend = rule.declarations.filter((declaration) => conflicts(declaration.property, change.property));
        if (!passend.length) continue;
        if (selector === change.selector && rule.media === change.media) continue;
        const andererTreffer = selectorMatches(node, selector);
        if (!andererTreffer.matched) { if (!andererTreffer.certain) unsicher = true; continue; }
        const fremd = specificity(selector);
        const wichtig = passend.some((declaration) => declaration.important);
        // Gewinnt die fremde Regel? Wichtig schlägt alles, sonst Spezifität, sonst spätere Stelle.
        if (wichtig || beats(fremd, eigene) || (!beats(eigene, fremd) && rule.order > change.order)) {
          sieger = { selector, value: passend.at(-1)!.value + (wichtig ? " !important" : "") };
          break;
        }
      }
      if (sieger) verloren.push(sieger);
    }
    // Nur wenn die Regel bei JEDEM geprüften Element verliert, ist sie wirklich wirkungslos.
    const komplettTot = verloren.length === stichprobe.length && stichprobe.length > 0;
    findings.push({
      selector: change.selector, property: change.property, value: change.value,
      status: komplettTot ? "ueberschrieben" : unsicher ? "unsicher" : "wirksam",
      elements: treffer.length, screens,
      ...(komplettTot ? { beatenBy: verloren[0]!.selector, beatenValue: verloren[0]!.value } : {})
    });
  }
  return findings;
}

// Welche Deklarationen hat der Lauf verändert? Verglichen werden die geparsten Regeln, nicht der
// Rohtext — Einrückung oder Reihenfolge sollen keine Scheinänderung melden.
export function changedDeclarations(before: string, after: string): Array<{ selector: string; property: string; value: string; media: string; order: number }> {
  const alt = new Map<string, CssDeclaration[]>();
  for (const rule of parseCss(styleBlocksOf(before))) for (const selector of rule.selectors) alt.set(`${rule.media}||${selector}`, [...(alt.get(`${rule.media}||${selector}`) ?? []), ...rule.declarations]);
  const changes: Array<{ selector: string; property: string; value: string; media: string; order: number }> = [];
  for (const rule of parseCss(styleBlocksOf(after))) {
    for (const selector of rule.selectors) {
      const vorher = alt.get(`${rule.media}||${selector}`);
      for (const declaration of rule.declarations) {
        const frueher = vorher?.filter((entry) => entry.property === declaration.property).at(-1);
        if (frueher && frueher.value.replace(/\s+/g, " ").trim() === declaration.value.replace(/\s+/g, " ").trim()) continue;
        changes.push({ selector, property: declaration.property, value: declaration.value, media: rule.media, order: rule.order });
      }
    }
  }
  return changes;
}

// ---------------------------------------------------------------- Design verstehen (vor dem Lauf)

const layoutProps = new Set(["padding", "padding-top", "padding-right", "padding-bottom", "padding-left", "margin", "margin-top", "margin-right", "margin-bottom", "margin-left", "gap", "row-gap", "column-gap", "border-radius", "font-size", "line-height", "height", "min-height", "width"]);
const resetTags = new Set(["button", "input", "textarea", "select", "a", "p", "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6", "svg", "img", "figure", "blockquote", "label", "fieldset"]);

// Die Kaskadenfallen dieses Designs: Regeln, die über einen ELEMENTNAMEN greifen und dadurch jede
// reine Klassenregel schlagen. Genau daran sind Läufe gescheitert, die fachlich alles richtig machten
// — sie änderten eine Klassenregel, die gegen `.bildschirm button { padding: 0 }` chancenlos ist.
export function cascadeTraps(html: string, limit = 10): string[] {
  const rules = parseCss(styleBlocksOf(html));
  const treffer: Array<{ text: string; gewicht: number }> = [];
  for (const rule of rules) {
    if (/print/i.test(rule.media)) continue;
    for (const selector of rule.selectors) {
      if (stateWords.test(selector)) continue;
      const teile = selector.trim().split(/\s*>\s*|\s+/).filter(Boolean);
      if (teile.length < 2) continue;
      const letzte = parseCompound(teile.at(-1)!);
      if (!letzte.tag || !resetTags.has(letzte.tag) || letzte.classes.length || letzte.id) continue;
      const relevant = rule.declarations.filter((declaration) => layoutProps.has(declaration.property));
      if (!relevant.length) continue;
      treffer.push({ text: `${selector} { ${relevant.map((d) => `${d.property}: ${d.value}${d.important ? " !important" : ""}`).join("; ")} }`, gewicht: relevant.length + specificity(selector)[1] });
    }
  }
  return [...new Map(treffer.map((entry) => [entry.text, entry])).values()].sort((a, b) => b.gewicht - a.gewicht).slice(0, limit).map((entry) => entry.text);
}

// Die Formensprache des Designs: welche Abstände, Radien und Schriftgrößen tatsächlich benutzt werden.
// Wer sie kennt, ändert im vorhandenen Raster statt krumme Einzelwerte einzustreuen.
export function designScale(html: string): { abstaende: string[]; radien: string[]; schriftgroessen: string[] } {
  const rules = parseCss(styleBlocksOf(html));
  const zaehle = (props: string[]) => {
    const counts = new Map<string, number>();
    for (const rule of rules) for (const declaration of rule.declarations) {
      if (!props.includes(declaration.property)) continue;
      for (const wert of declaration.value.split(/\s+/)) if (/^\d+(?:\.\d+)?px$/.test(wert)) counts.set(wert, (counts.get(wert) ?? 0) + 1);
    }
    return [...counts].sort((a, b) => b[1] - a[1]).slice(0, 10).map(([wert]) => wert)
      .sort((a, b) => Number.parseFloat(a) - Number.parseFloat(b));
  };
  return {
    abstaende: zaehle(["padding", "padding-top", "padding-right", "padding-bottom", "padding-left", "margin", "margin-top", "margin-bottom", "gap", "row-gap", "column-gap"]),
    radien: zaehle(["border-radius"]),
    schriftgroessen: zaehle(["font-size"])
  };
}

// Alles, was das Modell VOR dem ersten Zeichen wissen muss, damit seine Änderungen ankommen.
export function designBriefing(html: string): string {
  const traps = cascadeTraps(html);
  const scale = designScale(html);
  const zeilen: string[] = [];
  if (traps.length) {
    zeilen.push("KASKADENFALLEN — diese Regeln dieses Designs greifen über den Elementnamen und schlagen JEDE reine Klassenregel:");
    zeilen.push(traps.map((trap) => `  ${trap}`).join("\n"));
    zeilen.push("Willst du an einem betroffenen Element etwas ändern, das hier gesetzt wird, hast du zwei Wege: entweder du änderst genau diese Regel, oder du schreibst deine Regel spezifischer (Bildschirm-Container davor, z. B. `.pm-start-screen .pm-start__hook { … }`). Eine reine Klassenregel bleibt sonst WIRKUNGSLOS — sie sieht richtig aus und tut nichts.");
  }
  const raster = [
    scale.abstaende.length ? `Abstände: ${scale.abstaende.join(", ")}` : "",
    scale.radien.length ? `Eckenradien: ${scale.radien.join(", ")}` : "",
    scale.schriftgroessen.length ? `Schriftgrößen: ${scale.schriftgroessen.join(", ")}` : ""
  ].filter(Boolean);
  if (raster.length) zeilen.push(`FORMENSPRACHE — im Design tatsächlich benutzte Werte. Bleib in diesem Raster, statt krumme Einzelwerte einzustreuen.\n  ${raster.join("\n  ")}`);
  return zeilen.join("\n\n");
}

export type EffectSummary = { findings: EffectFinding[]; wirksam: number; tot: number; screens: string[]; briefing?: string; hinweis?: string };

export function summariseEffect(before: string, after: string): EffectSummary {
  const changes = changedDeclarations(before, after);
  if (!changes.length) return { findings: [], wirksam: 0, tot: 0, screens: [] };
  const findings = analyseEffect(after, changes);
  const tot = findings.filter((finding) => finding.status === "ueberschrieben" || finding.status === "kein-element");
  const wirksam = findings.filter((finding) => finding.status === "wirksam" || finding.status === "unsicher");
  const screens = [...new Set(wirksam.flatMap((finding) => finding.screens))];
  // Die Rückmeldung nennt den GEWINNER — damit weiss das Modell, wo es wirklich ansetzen muss.
  const zeilen = tot.slice(0, 12).map((finding) => finding.status === "kein-element"
    ? `„${finding.selector}" trifft kein einziges Element im Design — die Änderung an ${finding.property} ist wirkungslos. Prüfe den Selektor gegen das Markup.`
    : `„${finding.selector} { ${finding.property}: ${finding.value} }" bleibt WIRKUNGSLOS: „${finding.beatenBy} { ${finding.property}: ${finding.beatenValue} }" ist stärker und gewinnt die Kaskade. Ändere stattdessen die gewinnende Regel, oder gib deiner Regel eine höhere Spezifität (z. B. den Bildschirm-Container davor).`);
  return {
    findings, wirksam: wirksam.length, tot: tot.length, screens,
    ...(zeilen.length ? { briefing: `Diese Änderungen sind im Design NICHT sichtbar geworden:\n${zeilen.join("\n")}` } : {}),
    ...(tot.length ? { hinweis: `${tot.length} von ${findings.length} Änderungen blieben ohne sichtbare Wirkung (überschrieben von stärkeren Regeln).` } : {})
  };
}
