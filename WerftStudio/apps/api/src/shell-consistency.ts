import { parseXml, walkXml, type XmlNode } from "./xml-lite.js";

// Jeder Bildschirm entsteht in einem eigenen KI-Aufruf, der die uebrigen nicht sieht. Was auf allen
// Bildschirmen gleich aussehen MUSS — die Reiterleiste, die Kopfleiste — faellt dabei jedes Mal ein
// wenig anders aus: andere Hoehe, andere Symbolgroesse, andere Schrift. Im Studio wirkt die App dann
// bei jedem Reiterwechsel wie eine andere Anwendung. Diese Pruefung misst das NACH dem Aufbau
// programmatisch nach und benennt jede Abweichung so, dass der Korrekturlauf sie gezielt beheben
// kann — die Anweisung allein reicht nicht, denn kein Aufruf kann pruefen, was die anderen taten.

export type ShellKind = "leiste";
export type ShellSummary = {
  /** Beschriftungen der Eintraege, in Reihenfolge — der stabilste Fingerabdruck einer Leiste. */
  entries: string[];
  height: string;
  padding: string;
  background: string;
  radius: string;
  fontSizes: string[];
  iconSizes: string[];
};
export type ShellIssue = { screenId: string; screenName: string; aspect: string; expected: string; actual: string };
export type ShellScreen = { id: string; name: string; markup: string; isStart?: boolean };

const shellWords = /(?:^|[-_ ])(?:nav|navigation|tab|tabs|tabbar|reiter|leiste|bottombar|bottomnav|appbar|toolbar|footer|menu)(?:$|[-_ ])/i;
const measured = ["height", "min-height", "padding", "background", "background-color", "border-radius", "font-size"] as const;

const classesOf = (node: XmlNode): string[] => (node.attributes["class"] ?? "").split(/\s+/).filter(Boolean);

// Sieht dieses Element nach der wiederkehrenden Leiste aus? Der Tag `nav` und die Rolle `tablist`
// sind eindeutig; sonst entscheidet der Name der Klasse. Ein einzelner Knopf ist keine Leiste —
// deshalb zaehlen erst mehrere gleichrangige Eintraege.
function looksLikeShell(node: XmlNode): boolean {
  if (node.tag.toLowerCase() === "nav") return true;
  const role = (node.attributes["role"] ?? "").toLowerCase();
  if (role === "tablist" || role === "navigation") return true;
  return [...classesOf(node), node.attributes["data-werft-rolle"] ?? ""].some((value) => shellWords.test(value));
}

const textOf = (node: XmlNode): string => {
  let text = node.text ?? "";
  for (const child of node.children) text += textOf(child);
  return text.replace(/\s+/g, " ").trim();
};

// Die Eintraege einer Leiste sind ihre unmittelbaren, bedienbaren Kinder. Reine Huellen ohne eigene
// Beschriftung werden uebersprungen, sonst zaehlte jede Verschachtelung als weiterer Eintrag.
function entriesOf(node: XmlNode): XmlNode[] {
  const direct = node.children.filter((child) => textOf(child) || child.attributes["data-werft-navigate"] || child.children.length);
  if (direct.length >= 2) return direct;
  return direct.length === 1 ? entriesOf(direct[0]!) : direct;
}

export function declarationsOf(style: string): Record<string, string> {
  const declarations: Record<string, string> = {};
  for (const part of style.split(";")) {
    const index = part.indexOf(":");
    if (index <= 0) continue;
    const name = part.slice(0, index).trim().toLowerCase();
    const value = part.slice(index + 1).trim();
    if (name && value) declarations[name] = value.replace(/\s+/g, " ");
  }
  return declarations;
}

// Ein sehr kleiner Regel-Leser: er ordnet jeder Klasse die Deklarationen zu, die sie im gemeinsamen
// Stylesheet erhaelt. Er loest keine Spezifitaet auf — fuer einen VERGLEICH zweier gleich ermittelter
// Werte reicht das, denn beide Seiten werden auf demselben Weg gelesen.
export function styleIndex(css: string): Map<string, Record<string, string>> {
  const index = new Map<string, Record<string, string>>();
  const withoutComments = css.replace(/\/\*[\s\S]*?\*\//g, "");
  for (const match of withoutComments.matchAll(/([^{}]+)\{([^{}]*)\}/g)) {
    const selectors = match[1]!.trim();
    if (!selectors || selectors.startsWith("@")) continue;
    const declarations = declarationsOf(match[2] ?? "");
    for (const klasse of selectors.matchAll(/\.([A-Za-z0-9_-]+)/g)) {
      const name = klasse[1]!;
      index.set(name, { ...index.get(name), ...declarations });
    }
  }
  return index;
}

function resolved(node: XmlNode, index: Map<string, Record<string, string>>): Record<string, string> {
  const collected: Record<string, string> = {};
  for (const klasse of classesOf(node)) Object.assign(collected, index.get(klasse));
  Object.assign(collected, declarationsOf(node.attributes["style"] ?? ""));
  return collected;
}

const pick = (declarations: Record<string, string>, ...names: string[]): string => {
  for (const name of names) if (declarations[name]) return declarations[name]!;
  return "";
};

function sizeOf(node: XmlNode, index: Map<string, Record<string, string>>): string {
  const declarations = resolved(node, index);
  const width = node.attributes["width"] ?? pick(declarations, "width");
  const height = node.attributes["height"] ?? pick(declarations, "height");
  return width || height ? `${width || "?"}×${height || "?"}` : "";
}

export function summarizeShell(markup: string, css: string): ShellSummary | undefined {
  const root = parseXml(`<werft-root>${markup}</werft-root>`);
  if (!root) return undefined;
  const index = styleIndex(css);
  let shell: XmlNode | undefined;
  let bestEntries: XmlNode[] = [];
  walkXml(root, (node) => {
    if (!looksLikeShell(node)) return;
    const entries = entriesOf(node);
    // Die groesste erkannte Leiste gewinnt: ein Kopfbereich mit zwei Symbolen soll die Reiterleiste
    // mit fuenf Eintraegen nicht verdraengen.
    if (entries.length >= 2 && entries.length > bestEntries.length) { shell = node; bestEntries = entries; }
  });
  if (!shell) return undefined;
  const own = resolved(shell, index);
  const icons: string[] = [];
  for (const entry of bestEntries) walkXml(entry, (node) => { if (node.tag.toLowerCase() === "svg" || node.tag.toLowerCase() === "img") { const size = sizeOf(node, index); if (size) icons.push(size); } });
  return {
    entries: bestEntries.map((entry) => textOf(entry)),
    height: pick(own, "height", "min-height"),
    padding: pick(own, "padding"),
    background: pick(own, "background", "background-color"),
    radius: pick(own, "border-radius"),
    fontSizes: bestEntries.map((entry) => pick(resolved(entry, index), "font-size")),
    iconSizes: icons
  };
}

const listOf = (values: string[]): string => values.map((value) => value || "—").join(" | ");

function compare(expected: ShellSummary, actual: ShellSummary, screen: ShellScreen): ShellIssue[] {
  const issues: ShellIssue[] = [];
  const add = (aspect: string, soll: string, ist: string) => {
    if (!soll && !ist) return;
    if (soll === ist) return;
    issues.push({ screenId: screen.id, screenName: screen.name, aspect, expected: soll || "—", actual: ist || "—" });
  };
  if (expected.entries.length !== actual.entries.length) add("Anzahl der Einträge", String(expected.entries.length), String(actual.entries.length));
  else if (expected.entries.join("|") !== actual.entries.join("|")) add("Beschriftungen der Einträge", listOf(expected.entries), listOf(actual.entries));
  add("Höhe der Leiste", expected.height, actual.height);
  add("Innenabstand der Leiste", expected.padding, actual.padding);
  add("Hintergrund der Leiste", expected.background, actual.background);
  add("Eckenradius der Leiste", expected.radius, actual.radius);
  if (expected.fontSizes.join("|") !== actual.fontSizes.join("|")) add("Schriftgrößen der Einträge", listOf(expected.fontSizes), listOf(actual.fontSizes));
  if (expected.iconSizes.join("|") !== actual.iconSizes.join("|")) add("Symbolgrößen der Einträge", listOf(expected.iconSizes), listOf(actual.iconSizes));
  return issues;
}

/**
 * Vergleicht die wiederkehrende Leiste aller Bildschirme gegen die des Startbildschirms. Fehlt sie
 * auf einem Bildschirm ganz, obwohl die Mehrheit sie fuehrt, ist auch das eine Abweichung — genau so
 * verschwindet eine Reiterleiste sonst unbemerkt auf einem einzelnen Bildschirm.
 */
export function checkShellConsistency(screens: ShellScreen[], css: string): ShellIssue[] {
  if (screens.length < 2) return [];
  const summaries = screens.map((screen) => ({ screen, shell: summarizeShell(screen.markup, css) }));
  const withShell = summaries.filter((entry) => entry.shell);
  // Traegt nur eine Minderheit eine Leiste, ist sie kein gemeinsames Element — dann gibt es nichts
  // zu vereinheitlichen, und eine Meldung waere ein Fehlalarm.
  if (withShell.length < 2 || withShell.length * 2 <= summaries.length) return [];
  const reference = withShell.find((entry) => entry.screen.isStart) ?? withShell[0]!;
  const issues: ShellIssue[] = [];
  for (const entry of summaries) {
    if (entry.screen.id === reference.screen.id) continue;
    if (!entry.shell) {
      issues.push({ screenId: entry.screen.id, screenName: entry.screen.name, aspect: "Wiederkehrende Leiste fehlt", expected: `dieselbe Leiste wie „${reference.screen.name}“ (${listOf(reference.shell!.entries)})`, actual: "keine Leiste im Bildschirm" });
      continue;
    }
    issues.push(...compare(reference.shell!, entry.shell, entry.screen));
  }
  return issues;
}

/**
 * Der Bildschirm, an dem sich die anderen ausrichten — der Startbildschirm, sonst der erste mit
 * Leiste. Der Korrekturlauf muss ihn beim Namen nennen koennen, ohne die Wahl nachzubauen.
 */
export function shellReference(screens: ShellScreen[], css: string): ShellScreen | undefined {
  const withShell = screens.filter((screen) => summarizeShell(screen.markup, css));
  if (withShell.length < 2 || withShell.length * 2 <= screens.length) return undefined;
  return withShell.find((screen) => screen.isStart) ?? withShell[0];
}

/** Formuliert die Abweichungen eines Bildschirms als Auftrag fuer den Korrekturlauf. */
export function renderShellInstructions(issues: ShellIssue[], reference: string): string {
  if (!issues.length) return "";
  const lines = issues.map((issue) => `- ${issue.aspect}: verbindlich ist „${issue.expected}“, dieser Bildschirm hat „${issue.actual}“.`);
  return [
    `Die wiederkehrende Leiste muss ZEICHENGLEICH zu der auf „${reference}“ sein; unterscheiden darf sich nur, welcher Eintrag als aktiv markiert ist.`,
    ...lines
  ].join("\n");
}
