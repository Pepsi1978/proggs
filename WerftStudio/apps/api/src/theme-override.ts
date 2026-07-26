import type { ThemeVariant } from "./screen-composer.js";

// Ein bereits aufgebautes Design traegt seine Farben oft als feste Werte im Stylesheet — nicht als
// Variablen. Es liesse sich dann nie umschalten, obwohl das Projekt mehrere Erscheinungen mitbringt
// (gemeldet an PerfectMoment: 18 Bildschirme, alle nur dunkel). Statt den teuren Neuaufbau zu
// verlangen, wird hier eine FARBABBILDUNG erzeugt: jede Regel, die eine Farbe der eingebauten
// Erscheinung nennt, bekommt eine Zwillingsregel mit der Farbe der anderen Erscheinung.
// Das ist deterministisch, braucht keine KI und wirkt sofort auf allen Bildschirmen.

type Declaration = { property: string; value: string; important: boolean };
type Rule = { selector: string; declarations: Declaration[]; media?: string };

const colourValue = /^(?:#[0-9a-fA-F]{3,8}|rgba?\(|hsla?\()/;

export function styleBlocks(html: string): string[] {
  return [...html.matchAll(/<style\b[^>]*>([\s\S]*?)<\/style\s*>/gi)].map((match) => match[1]!);
}

// Ein bewusst kleiner CSS-Leser: er kennt Regeln, Deklarationen und Bedingungsbloecke (@media,
// @supports). Mehr braucht die Abbildung nicht — und ein vollstaendiger Parser waere hier Ballast.
export function readRules(css: string, media?: string): Rule[] {
  const rules: Rule[] = [];
  let index = 0;
  while (index < css.length) {
    const open = css.indexOf("{", index);
    if (open < 0) break;
    const selector = css.slice(index, open).trim();
    let depth = 1;
    let cursor = open + 1;
    while (cursor < css.length && depth > 0) {
      if (css[cursor] === "{") depth += 1;
      else if (css[cursor] === "}") depth -= 1;
      cursor += 1;
    }
    const body = css.slice(open + 1, cursor - 1);
    if (selector.startsWith("@")) {
      // Verschachtelte Bedingungen behalten ihre Bedingung, damit die Zwillingsregel dort landet,
      // wo das Original auch gilt. Regeln ohne eigene Deklarationen (@keyframes) fallen weg.
      if (/^@(?:media|supports|layer|container)/i.test(selector)) rules.push(...readRules(body, media ? `${media} and ${selector.slice(selector.indexOf(" ") + 1)}` : selector));
    } else if (selector) {
      const declarations = readDeclarations(body);
      if (declarations.length) rules.push({ selector, declarations, ...(media ? { media } : {}) });
    }
    index = cursor;
  }
  return rules;
}

function readDeclarations(body: string): Declaration[] {
  const declarations: Declaration[] = [];
  for (const part of body.split(";")) {
    const separator = part.indexOf(":");
    if (separator < 0) continue;
    const property = part.slice(0, separator).trim();
    let value = part.slice(separator + 1).trim();
    if (!property || !value || property.startsWith("/*")) continue;
    const important = /!important$/i.test(value);
    if (important) value = value.replace(/!important$/i, "").trim();
    declarations.push({ property, value, important });
  }
  return declarations;
}

const escapeForPattern = (text: string) => text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

// Dieselbe Farbe steht im Stylesheet mal als `#181209`, mal als `rgb(24, 18, 9)`. Ohne beide
// Schreibweisen bliebe die Haelfte der Regeln unberuehrt und das Design halb umgeschaltet.
export function colourSpellings(value: string): string[] {
  const trimmed = value.trim();
  const hex = /^#([0-9a-fA-F]{6})$/.exec(trimmed);
  if (!hex) return [trimmed];
  const number = Number.parseInt(hex[1]!, 16);
  const red = (number >> 16) & 255, green = (number >> 8) & 255, blue = number & 255;
  return [trimmed, `rgb(${red}, ${green}, ${blue})`, `rgb(${red},${green},${blue})`];
}

// Welche Erscheinung steckt im Dokument? Die, deren Farben dort am haeufigsten wirklich vorkommen.
export function builtInVariant(css: string, variants: ThemeVariant[]): ThemeVariant | undefined {
  const lower = css.toLowerCase();
  const hits = (variant: ThemeVariant) => Object.values(variant.tokens)
    .filter((value) => colourValue.test(value.trim()))
    .filter((value) => colourSpellings(value).some((spelling) => lower.includes(spelling.toLowerCase()))).length;
  const ranked = variants.map((variant) => ({ variant, count: hits(variant) })).sort((left, right) => right.count - left.count);
  // Weniger als zwei Treffer heisst: die Farben stehen gar nicht im Dokument — dann gibt es nichts
  // abzubilden, und ein geratener Ersatz wuerde das Design verfaelschen.
  return ranked[0] && ranked[0].count >= 2 ? ranked[0].variant : undefined;
}

// Der Selektor der Zwillingsregel wird auf die gewaehlte Erscheinung eingeschraenkt. `:root` und
// `html` bekommen das Attribut direkt angehaengt — als Nachfahre geschrieben wuerden sie nie treffen.
export function scopeSelector(selector: string, themeId: string): string {
  return selector.split(",").map((part) => {
    const trimmed = part.trim();
    if (!trimmed) return "";
    if (/^(:root|html)\b/i.test(trimmed)) return trimmed.replace(/^(:root|html)/i, `$1[data-werft-theme="${themeId}"]`);
    return `:root[data-werft-theme="${themeId}"] ${trimmed}`;
  }).filter(Boolean).join(", ");
}

export function themeOverrideCss(html: string, variants: ThemeVariant[]): string {
  if (variants.length < 2) return "";
  const css = styleBlocks(html).join("\n");
  if (!css.trim()) return "";
  const base = builtInVariant(css, variants);
  if (!base) return "";
  const rules = readRules(css);
  if (!rules.length) return "";
  const blocks: string[] = [];
  for (const variant of variants) {
    if (variant.id === base.id) continue;
    // Abgebildet wird je TOKEN, nicht je Farbe: nur so trifft „Hintergrund" auf „Hintergrund".
    const mapping = Object.entries(base.tokens)
      .filter(([token, value]) => colourValue.test(value.trim()) && variant.tokens[token] && variant.tokens[token]!.trim().toLowerCase() !== value.trim().toLowerCase())
      .flatMap(([token, value]) => colourSpellings(value).map((spelling) => ({ pattern: new RegExp(escapeForPattern(spelling), "gi"), to: variant.tokens[token]! })));
    if (!mapping.length) continue;
    const byMedia = new Map<string, string[]>();
    for (const rule of rules) {
      const changed = rule.declarations
        .map((declaration) => {
          const next = mapping.reduce((value, entry) => value.replace(entry.pattern, entry.to), declaration.value);
          return next === declaration.value ? undefined : `${declaration.property}: ${next}${declaration.important ? " !important" : ""}`;
        })
        .filter((entry): entry is string => Boolean(entry));
      if (!changed.length) continue;
      const key = rule.media ?? "";
      byMedia.set(key, [...(byMedia.get(key) ?? []), `${scopeSelector(rule.selector, variant.id)} { ${changed.join("; ")} }`]);
    }
    for (const [media, entries] of byMedia) blocks.push(media ? `${media} {\n${entries.join("\n")}\n}` : entries.join("\n"));
  }
  return blocks.join("\n");
}
