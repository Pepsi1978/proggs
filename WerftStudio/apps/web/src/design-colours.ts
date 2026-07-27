// Die Farbregler brauchen #rrggbb; gemessen wird am lebenden Dokument aber meist rgb(). Ohne diese
// Umrechnung stuende in jedem Regler Schwarz, egal welche Farbe das Design wirklich verwendet.
export function hexColour(value: string): string {
  const trimmed = (value ?? "").trim();
  if (/^#[0-9a-fA-F]{8}$/.test(trimmed)) return trimmed.slice(0, 7).toLowerCase();
  if (/^#[0-9a-fA-F]{6}$/.test(trimmed)) return trimmed.toLowerCase();
  if (/^#[0-9a-fA-F]{3}$/.test(trimmed)) return `#${trimmed.slice(1).split("").map((part) => part + part).join("")}`.toLowerCase();
  const rgb = /^rgba?\(\s*([\d.]+)[\s,]+([\d.]+)[\s,]+([\d.]+)/i.exec(trimmed);
  if (!rgb) return "";
  const part = (number: string) => Math.max(0, Math.min(255, Math.round(Number(number)))).toString(16).padStart(2, "0");
  return `#${part(rgb[1]!)}${part(rgb[2]!)}${part(rgb[3]!)}`;
}

// Eine lange Liste gleich aussehender Regler sagt nichts. Nach Bereichen geordnet ist dagegen sofort
// erkennbar, welche Farbe wofuer zustaendig ist. Die Reihenfolge entscheidet: „error" ist ein
// Zustand, auch wenn der Name zusaetzlich „primary" enthaelt.
export const tokenGroups: Array<{ id: string; title: string; hint: string; match: RegExp }> = [
  { id: "status", title: "Zustände und Meldungen", hint: "Erfolg, Warnung, Fehler und Hinweise", match: /(success|error|danger|warn|alert|info|positive|negative|critical|caution)/i },
  { id: "accent", title: "Akzent und Bedienelemente", hint: "Schaltflächen, Links und Hervorhebungen", match: /(accent|primary|secondary|tertiary|brand|action|button|link|cta|highlight|selected|active|focus)/i },
  { id: "line", title: "Linien, Rahmen und Schatten", hint: "Trennendes und Umrandungen", match: /(line|border|divider|outline|stroke|separator|shadow|scrim|ring)/i },
  { id: "text", title: "Schrift", hint: "Alles, was gelesen wird", match: /(text|foreground|(^|-)fg(-|$)|label|title|heading|body|caption|ink|placeholder|hint|disabled|muted|subtle)/i },
  { id: "surface", title: "Flächen und Hintergründe", hint: "Grundflächen, Karten und Felder", match: /(bg|background|surface|container|card|sheet|panel|fill|canvas|paper|overlay|backdrop|field|input|hover)/i },
];
const tokenWords: Record<string, string> = {
  bg: "Hintergrund", background: "Hintergrund", surface: "Fläche", text: "Schrift", fg: "Schrift", foreground: "Schrift",
  accent: "Akzent", primary: "Primär", secondary: "Sekundär", tertiary: "Tertiär", border: "Rahmen", line: "Linie",
  error: "Fehler", success: "Erfolg", warning: "Warnung", warn: "Warnung", info: "Hinweis", danger: "Gefahr",
  hover: "Überfahren", disabled: "Gesperrt", subtle: "Gedämpft", muted: "Gedämpft", strong: "Kräftig", soft: "Weich",
  card: "Karte", field: "Feld", shadow: "Schatten", outline: "Umriss", container: "Behälter", on: "auf"
};
// „--md-sys-color-on-surface-variant" ist kein Name, den man liest. Die Herkunft bleibt im Titel
// stehen; angezeigt wird eine lesbare Fassung.
// Fuehrt ein Design keine Farbvariablen, treten an ihre Stelle die Farben, die es tatsaechlich
// zeichnet. Sie tragen dieses Praefix und werden direkt in den Regeln ersetzt, in denen sie stehen.
export const measuredPrefix = "--gemessen-";
export const usesMeasuredColours = (tokens: Record<string, string>) => Object.keys(tokens).some((name) => name.startsWith(measuredPrefix));

export function tokenTitle(name: string): string {
  const bare = name.replace(/^--/, "").replace(/^(gemessen-|md-sys-|md-ref-|mat-|sys-|ref-|werft-|app-|theme-|color-|colour-|c-)+/i, "").replace(/-?colou?r-?/gi, "-");
  const words = bare.split(/[-_]+/).filter(Boolean).map((word) => tokenWords[word.toLowerCase()] ?? word);
  const label = words.join(" ").trim();
  if (!label) return name.replace(/^--/, "");
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export type TokenGroup = { id: string; title: string; hint: string; entries: Array<[string, string]> };
export function groupedTokens(tokens: Record<string, string>): TokenGroup[] {
  const rest = new Map(Object.entries(tokens));
  const groups = tokenGroups.map((group) => {
    const entries: Array<[string, string]> = [];
    for (const [name, value] of [...rest]) if (group.match.test(name)) { entries.push([name, value]); rest.delete(name); }
    return { id: group.id, title: group.title, hint: group.hint, entries: entries.sort((a, b) => a[0].localeCompare(b[0], "de")) };
  }).filter((group) => group.entries.length);
  if (rest.size) groups.push({ id: "other", title: "Weitere Farben", hint: "Nicht eindeutig zuzuordnen", entries: [...rest].sort((a, b) => a[0].localeCompare(b[0], "de")) });
  return groups;
}
