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

// Fuehrt ein Design keine Farbvariablen, treten an ihre Stelle die Farben, die es tatsaechlich
// zeichnet. Sie tragen dieses Praefix und werden direkt in den Regeln ersetzt, in denen sie stehen.
export const measuredPrefix = "--gemessen-";

const tokenWords: Record<string, string> = {
  bg: "Hintergrund", background: "Hintergrund", surface: "Fläche", text: "Schrift", fg: "Schrift", foreground: "Schrift",
  accent: "Akzent", primary: "Primär", secondary: "Sekundär", tertiary: "Tertiär", border: "Rahmen", line: "Linie",
  error: "Fehler", success: "Erfolg", warning: "Warnung", warn: "Warnung", info: "Hinweis", danger: "Gefahr",
  hover: "Überfahren", disabled: "Gesperrt", subtle: "Gedämpft", muted: "Gedämpft", strong: "Kräftig", soft: "Weich",
  card: "Karte", field: "Feld", shadow: "Schatten", outline: "Umriss", container: "Behälter", on: "auf"
};
// „--md-sys-color-on-surface-variant" ist kein Name, den man liest. Die Herkunft bleibt im Titel
// stehen; angezeigt wird eine lesbare Fassung.
export function tokenTitle(name: string): string {
  const bare = name.replace(/^--/, "").replace(/^(gemessen-|md-sys-|md-ref-|mat-|sys-|ref-|werft-|app-|theme-|color-|colour-|c-)+/i, "").replace(/-?colou?r-?/gi, "-");
  const words = bare.split(/[-_]+/).filter(Boolean).map((word) => tokenWords[word.toLowerCase()] ?? word);
  const label = words.join(" ").trim();
  if (!label) return name.replace(/^--/, "");
  return label.charAt(0).toUpperCase() + label.slice(1);
}
