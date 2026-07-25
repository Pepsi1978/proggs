// Deterministisch aus den Projektquellen gelesene Design-Fakten. Diese Werte werden NICHT von der KI
// geschaetzt, sondern geparst — sie sind fuer Aufbau und Pruefung der HTML-Rekonstruktion verbindlich.
// `used` markiert Werte, die nachweislich an einem konkreten Element haengen (Layout-Attribut,
// Compose-Modifier, Theme-Token). Nur diese duerfen nachgemessen werden: eine bloss DEFINIERTE
// Ressourcenfarbe muss im Design nicht vorkommen — sie einzufordern wuerde es verfaelschen.
export type FactColor = { name: string; css: string; source: string; used?: boolean };
export type FactDimension = { name: string; px: number; raw: string; source: string; used?: boolean };
export type FactTypography = { name: string; family?: string; sizePx?: number; weight?: number; lineHeightPx?: number; letterSpacingPx?: number; source: string; used?: boolean };
export type FactShape = { name: string; radiusCss: string; source: string; used?: boolean };
export type FactEffectKind = "shadow" | "blur" | "gradient" | "stroke" | "ripple" | "opacity";
export type FactEffect = { name: string; kind: FactEffectKind; css: string; source: string };
export type FactAsset = { name: string; path: string; kind: "vector" | "bitmap" | "shape" | "color"; svg?: string; css?: string; source: string };
// `files` haelt fest, aus welchen Quellen ein Screen stammt: beim screenweisen Aufbau wird genau
// dieser Originalquelltext erneut mitgeschickt, statt sich auf die verdichtete Evidenz zu verlassen.
export type FactScreen = { id: string; name: string; kind: string; source: string; route?: string; navigatesTo: string[]; isStart: boolean; files: string[] };
export type FactTheme = { id: string; name: string; tokens: Record<string, string>; source: string };
export type FactViewport = { width: number; height: number; device: string; density: number; source: string };

export type DesignFacts = {
  platform: string;
  colors: FactColor[];
  dimensions: FactDimension[];
  typography: FactTypography[];
  shapes: FactShape[];
  effects: FactEffect[];
  assets: FactAsset[];
  screens: FactScreen[];
  themes: FactTheme[];
  strings: Array<{ name: string; value: string }>;
  viewport?: FactViewport;
  notes: string[];
};

export const emptyFacts = (platform: string): DesignFacts => ({ platform, colors: [], dimensions: [], typography: [], shapes: [], effects: [], assets: [], screens: [], themes: [], strings: [], notes: [] });

export function mergeFacts(target: DesignFacts, addition: Partial<DesignFacts>): DesignFacts {
  // Bei Namensgleichheit gewinnt der Eintrag, der nachweislich an einem Element haengt. Ohne diese
  // Regel verwirft die Zusammenfuehrung die `used`-Markierung und der Wert fällt aus der Nachmessung.
  const keyed = <T extends { name: string; used?: boolean }>(existing: T[], incoming: T[] = []) => {
    const seen = new Map(existing.map((item) => [item.name, item] as const));
    for (const item of incoming) {
      const current = seen.get(item.name);
      if (!current) seen.set(item.name, item);
      else if (item.used && !current.used) seen.set(item.name, item);
    }
    return [...seen.values()];
  };
  return {
    platform: target.platform,
    colors: keyed(target.colors, addition.colors),
    dimensions: keyed(target.dimensions, addition.dimensions),
    typography: keyed(target.typography, addition.typography),
    shapes: keyed(target.shapes, addition.shapes),
    effects: keyed(target.effects, addition.effects),
    assets: keyed(target.assets, addition.assets),
    themes: keyed(target.themes, addition.themes),
    strings: keyed(target.strings, addition.strings),
    screens: mergeScreens(target.screens, addition.screens ?? []),
    ...(addition.viewport ?? target.viewport ? { viewport: target.viewport ?? addition.viewport! } : {}),
    notes: [...target.notes, ...(addition.notes ?? [])]
  };
}

function mergeScreens(existing: FactScreen[], incoming: FactScreen[]): FactScreen[] {
  const byId = new Map(existing.map((screen) => [screen.id, screen] as const));
  for (const screen of incoming) {
    const current = byId.get(screen.id);
    if (!current) { byId.set(screen.id, screen); continue; }
    byId.set(screen.id, {
      ...current,
      ...(current.route ? {} : screen.route ? { route: screen.route } : {}),
      isStart: current.isStart || screen.isStart,
      navigatesTo: [...new Set([...current.navigatesTo, ...screen.navigatesTo])],
      files: [...new Set([...current.files, ...screen.files])]
    });
  }
  return [...byId.values()];
}

// Farbwerte werden immer als CSS-taugliche Zeichenkette normalisiert, damit HTML-Aufbau und
// Pruefung dieselbe Schreibweise vergleichen koennen.
export function normalizeHexColor(raw: string): string | undefined {
  const value = raw.trim().replace(/^#/, "");
  if (!/^[0-9a-f]+$/i.test(value)) return undefined;
  const expand = (short: string) => short.split("").map((char) => char + char).join("");
  if (value.length === 3) return `#${expand(value)}`.toLowerCase();
  if (value.length === 4) return rgbaFromArgb(`${expand(value)}`);
  if (value.length === 6) return `#${value}`.toLowerCase();
  if (value.length === 8) return rgbaFromArgb(value);
  return undefined;
}

// Android/WPF schreiben #AARRGGBB, CSS erwartet #RRGGBBAA — ohne diese Umrechnung landen
// Alpha-Werte faelschlich im Rotkanal.
export function rgbaFromArgb(argb: string): string {
  const alpha = Number.parseInt(argb.slice(0, 2), 16) / 255;
  const rgb = argb.slice(2).toLowerCase();
  return alpha >= 0.999 ? `#${rgb}` : `rgba(${Number.parseInt(rgb.slice(0, 2), 16)}, ${Number.parseInt(rgb.slice(2, 4), 16)}, ${Number.parseInt(rgb.slice(4, 6), 16)}, ${Number(alpha.toFixed(4))})`;
}

// Ein logisches dp/sp/pt/DIP entspricht exakt einem CSS-Pixel; nur echte physische Einheiten
// werden umgerechnet, damit Abstaende nicht um Bruchteile verrutschen.
export function dimensionToPx(raw: string): number | undefined {
  const match = /^\s*(-?\d+(?:\.\d+)?)\s*(dp|dip|sp|px|pt|in|mm|em|rem|%|)\s*$/i.exec(raw);
  if (!match) return undefined;
  const value = Number(match[1]);
  if (!Number.isFinite(value)) return undefined;
  switch (match[2]!.toLowerCase()) {
    case "in": return value * 96;
    case "mm": return value * (96 / 25.4);
    case "%": return Number.NaN;
    default: return value;
  }
}

export const factCount = (facts: DesignFacts): number => facts.colors.length + facts.dimensions.length + facts.typography.length + facts.shapes.length + facts.effects.length + facts.assets.length + facts.screens.length + facts.themes.length;

const block = (title: string, lines: string[], limit: number): string => lines.length ? `\n## ${title} (${lines.length})\n${lines.slice(0, limit).join("\n")}${lines.length > limit ? `\n… ${lines.length - limit} weitere` : ""}` : "";

// Kompakte, maschinennahe Darstellung fuer den Prompt: jede Zeile ist ein exakter Messwert mit Quelle.
export function renderFactSheet(facts: DesignFacts, options: { includeAssets?: boolean; screenIds?: string[] } = {}): string {
  const screens = options.screenIds ? facts.screens.filter((screen) => options.screenIds!.includes(screen.id)) : facts.screens;
  const parts = [
    `# VERBINDLICHE DESIGN-FAKTEN (${facts.platform}) — deterministisch aus den Quellen geparst, nicht geschätzt`,
    facts.viewport ? `Quellgeometrie: ${facts.viewport.width}x${facts.viewport.height} px, Gerät ${facts.viewport.device}, Dichte ${facts.viewport.density} (${facts.viewport.source})` : "",
    block("Farben", facts.colors.map((color) => `${color.name} = ${color.css}  [${color.source}]`), 400),
    block("Themes", facts.themes.map((theme) => `${theme.id} „${theme.name}“: ${Object.entries(theme.tokens).map(([token, value]) => `${token}=${value}`).join(", ")}  [${theme.source}]`), 40),
    block("Maße", facts.dimensions.map((dimension) => `${dimension.name} = ${dimension.px}px (${dimension.raw})  [${dimension.source}]`), 400),
    block("Typografie", facts.typography.map((type) => `${type.name}: ${[type.family && `family=${type.family}`, type.sizePx !== undefined && `size=${type.sizePx}px`, type.weight !== undefined && `weight=${type.weight}`, type.lineHeightPx !== undefined && `lineHeight=${type.lineHeightPx}px`, type.letterSpacingPx !== undefined && `letterSpacing=${type.letterSpacingPx}px`].filter(Boolean).join(", ")}  [${type.source}]`), 200),
    block("Formen/Radien", facts.shapes.map((shape) => `${shape.name} = border-radius: ${shape.radiusCss}  [${shape.source}]`), 200),
    block("Effekte — exakt so in CSS übernehmen", facts.effects.map((effect) => `${effect.name} (${effect.kind}) = ${effect.css}  [${effect.source}]`), 250),
    block("Screens", screens.map((screen) => `${screen.id} „${screen.name}“ (${screen.kind}${screen.route ? `, route=${screen.route}` : ""}${screen.isStart ? ", STARTSCREEN" : ""}) → ${screen.navigatesTo.join(", ") || "keine erkannte Navigation"}  [${screen.source}]`), 120),
    options.includeAssets === false ? "" : block("Assets", facts.assets.map((asset) => `${asset.name} (${asset.kind}) → ${asset.path}${asset.svg ? " [SVG inline verfügbar]" : ""}${asset.css ? ` [css: ${asset.css}]` : ""}`), 300),
    block("Texte", facts.strings.map((entry) => `${entry.name} = ${JSON.stringify(entry.value)}`), 300),
    facts.notes.length ? `\n## Hinweise\n${facts.notes.join("\n")}` : ""
  ];
  return parts.filter(Boolean).join("\n");
}

// Inline-SVGs der erkannten Icons: ohne sie erfindet die KI Symbole, statt die echten zu zeigen.
export function renderAssetLibrary(facts: DesignFacts, limit = 60): string {
  const vectors = facts.assets.filter((asset) => asset.svg).slice(0, limit);
  if (!vectors.length) return "";
  return `# ORIGINAL-ICONS (exakt so einbetten, keine Ersatzsymbole)\n${vectors.map((asset) => `--- ${asset.name} (${asset.path}) ---\n${asset.svg}`).join("\n")}`;
}
