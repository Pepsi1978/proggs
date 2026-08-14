import { dimensionToPx, normalizeHexColor, type DesignFacts, type FactColor, type FactDimension, type FactEffect, type FactScreen, type FactShape, type FactTypography } from "./design-facts.js";
import type { SourceText } from "./extract-common.js";

// Web-Projekte bringen ihr CSS schon mit; hier werden nur die Design-Konstanten eingesammelt,
// damit die Fidelity-Pruefung auch bei Web etwas zu vergleichen hat.

const customPropertyPattern = /--([\w-]+)\s*:\s*([^;{}]+)[;}]/g;
const shadowPattern = /box-shadow\s*:\s*([^;{}]+)[;}]/gi;
const radiusPattern = /border-radius\s*:\s*([^;{}]+)[;}]/gi;
const fontPattern = /font-family\s*:\s*([^;{}]+)[;}]/gi;
const gradientPattern = /(linear-gradient|radial-gradient|conic-gradient)\(([^;{}]{0,300})\)/gi;

// Ein Effekt ohne Ort ist ein Wert ohne Auftrag: hiess er nur „design.html:gradient(7)", konnte der
// spaetere Aufbau nicht wissen, an welches Element der Verlauf gehoert — und liess ihn im Zweifel
// ganz weg. Deshalb traegt der Name die Stelle, an der der Effekt im Original steht: den
// CSS-Selektor, oder bei einer Angabe direkt am Element dessen Tag und Klasse.
export function effectLocation(text: string, index: number): string {
  const tagStart = text.lastIndexOf("<", index);
  if (tagStart >= 0 && !text.slice(tagStart, index).includes(">")) {
    const kopf = text.slice(tagStart, index);
    const tag = /^<([\w-]+)/.exec(kopf)?.[1] ?? "";
    const klasse = /\bclass\s*=\s*"([^"]{0,80})"/i.exec(kopf)?.[1]?.trim().split(/\s+/).filter(Boolean).map((wert) => `.${wert}`).join("") ?? "";
    if (tag) return `${tag}${klasse}`;
  }
  const open = text.lastIndexOf("{", index);
  if (open < 0) return "";
  const davor = text.slice(0, open);
  const grenze = Math.max(davor.lastIndexOf("}"), davor.lastIndexOf("{"), davor.lastIndexOf(";"), davor.lastIndexOf(">"));
  return davor.slice(grenze + 1).replace(/\s+/g, " ").trim().slice(0, 120);
}

const effectName = (text: string, index: number, path: string, kind: string, laufend: number): string => {
  const ort = effectLocation(text, index);
  return ort ? `${ort} (${kind})` : `${path}:${kind}(${laufend})`;
};

export function extractWebFacts(files: SourceText[]): Partial<DesignFacts> {
  const colors: FactColor[] = [];
  const dimensions: FactDimension[] = [];
  const typography: FactTypography[] = [];
  const shapes: FactShape[] = [];
  const effects: FactEffect[] = [];
  const screens: FactScreen[] = [];

  for (const file of files) {
    const isStyle = /\.(css|scss|sass|less)$/i.test(file.path);
    const isMarkup = /\.(html?|vue|svelte|jsx|tsx)$/i.test(file.path);
    if (!isStyle && !isMarkup) continue;
    for (const match of file.text.matchAll(customPropertyPattern)) {
      const name = match[1]!;
      const value = match[2]!.trim();
      const color = normalizeHexColor(value) ?? (/^(?:rgba?|hsla?)\(/i.test(value) ? value : undefined);
      if (color) { colors.push({ name: `--${name}`, css: color, source: file.path }); continue; }
      const px = dimensionToPx(value);
      if (px !== undefined && !Number.isNaN(px)) dimensions.push({ name: `--${name}`, px, raw: value, source: file.path });
    }
    for (const match of file.text.matchAll(shadowPattern)) if (!match[1]!.includes("var(")) effects.push({ name: effectName(file.text, match.index, file.path, "shadow", effects.length), kind: "shadow", css: match[1]!.trim(), source: file.path });
    for (const match of file.text.matchAll(radiusPattern)) if (!match[1]!.includes("var(")) shapes.push({ name: effectName(file.text, match.index, file.path, "radius", shapes.length), radiusCss: match[1]!.trim(), source: file.path, used: true });
    for (const match of file.text.matchAll(fontPattern)) typography.push({ name: effectName(file.text, match.index, file.path, "font", typography.length), family: match[1]!.trim(), source: file.path, used: true });
    for (const match of file.text.matchAll(gradientPattern)) effects.push({ name: effectName(file.text, match.index, file.path, "gradient", effects.length), kind: "gradient", css: `background-image: ${match[1]}(${match[2]})`, source: file.path });
    if (/\.html?$/i.test(file.path)) {
      const title = /<title[^>]*>([\s\S]{0,120}?)<\/title>/i.exec(file.text)?.[1]?.trim();
      const name = file.path.split("/").at(-1)!.replace(/\.html?$/i, "");
      screens.push({ id: `web:${file.path}`, name: title || name, kind: "html-page", source: file.path, navigatesTo: [...file.text.matchAll(/href\s*=\s*"(?!https?:|#|mailto:)([^"]+\.html?)"/gi)].map((match) => `web:${match[1]}`), isStart: /^index$/i.test(name), files: [file.path] });
    }
  }
  return { colors, dimensions, typography, shapes, effects, screens };
}

export function webFactCandidates(paths: string[]): string[] {
  return paths.filter((path) => /\.(css|scss|sass|less|html?|vue|svelte|jsx|tsx)$/i.test(path));
}

// Native Apps enthalten fast immer fremdes HTML (Rechtstexte, Hilfeseiten, Werkzeugoberflaechen).
// Deren Schatten, Radien und Schriften gehoeren NICHT zum App-Design — als Quellwert nachgemessen
// wuerden sie falsche Korrekturauftraege ausloesen. Dort zaehlen nur echte Stylesheets.
export function webStyleCandidates(paths: string[]): string[] {
  return paths.filter((path) => /\.(css|scss|sass|less)$/i.test(path));
}
