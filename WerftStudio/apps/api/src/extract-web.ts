import { dimensionToPx, normalizeHexColor, type DesignFacts, type FactColor, type FactDimension, type FactEffect, type FactScreen, type FactShape, type FactTypography } from "./design-facts.js";
import type { SourceText } from "./extract-common.js";

// Web-Projekte bringen ihr CSS schon mit; hier werden nur die Design-Konstanten eingesammelt,
// damit die Fidelity-Pruefung auch bei Web etwas zu vergleichen hat.

const customPropertyPattern = /--([\w-]+)\s*:\s*([^;{}]+)[;}]/g;
const shadowPattern = /box-shadow\s*:\s*([^;{}]+)[;}]/gi;
const radiusPattern = /border-radius\s*:\s*([^;{}]+)[;}]/gi;
const fontPattern = /font-family\s*:\s*([^;{}]+)[;}]/gi;
const gradientPattern = /(linear-gradient|radial-gradient|conic-gradient)\(([^;{}]{0,300})\)/gi;

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
    for (const match of file.text.matchAll(shadowPattern)) if (!match[1]!.includes("var(")) effects.push({ name: `${file.path}:shadow(${effects.length})`, kind: "shadow", css: match[1]!.trim(), source: file.path });
    for (const match of file.text.matchAll(radiusPattern)) if (!match[1]!.includes("var(")) shapes.push({ name: `${file.path}:radius(${shapes.length})`, radiusCss: match[1]!.trim(), source: file.path, used: true });
    for (const match of file.text.matchAll(fontPattern)) typography.push({ name: `${file.path}:font(${typography.length})`, family: match[1]!.trim(), source: file.path, used: true });
    for (const match of file.text.matchAll(gradientPattern)) effects.push({ name: `${file.path}:gradient(${effects.length})`, kind: "gradient", css: `background-image: ${match[1]}(${match[2]})`, source: file.path });
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
