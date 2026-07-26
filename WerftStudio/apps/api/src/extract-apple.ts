import { normalizeHexColor, type DesignFacts, type FactColor, type FactDimension, type FactEffect, type FactScreen, type FactShape, type FactTypography, type FactViewport } from "./design-facts.js";
import { colorWithOpacity, swiftShadowToCss } from "./effect-catalog.js";
import { attribute, parseXml, walkXml } from "./xml-lite.js";
import type { SourceText } from "./extract-common.js";

// macOS/iOS/iPadOS: SwiftUI-Modifier, AppKit/UIKit-Code, Storyboards und Asset-Kataloge. Ein pt
// entspricht einem CSS-Pixel; Farben liegen im Asset-Katalog als 0..1-Komponenten vor.

const systemColors: Record<string, string> = {
  black: "#000000", white: "#ffffff", clear: "transparent", red: "#ff3b30", orange: "#ff9500", yellow: "#ffcc00",
  green: "#34c759", mint: "#00c7be", teal: "#30b0c7", cyan: "#32ade6", blue: "#007aff", indigo: "#5856d6",
  purple: "#af52de", pink: "#ff2d55", brown: "#a2845e", gray: "#8e8e93", secondary: "#3c3c4399", primary: "#000000", accentColor: "var(--accent-color)"
};

const swiftFontWeights: Record<string, number> = { ultraLight: 100, thin: 200, light: 300, regular: 400, medium: 500, semibold: 600, bold: 700, heavy: 800, black: 900 };
// Die Punktgroessen der Apple-Textstile sind fest vorgegeben; ohne sie wird `.title` frei geraten.
const swiftTextStyles: Record<string, { sizePx: number; weight: number }> = {
  largeTitle: { sizePx: 34, weight: 400 }, title: { sizePx: 28, weight: 400 }, title2: { sizePx: 22, weight: 400 }, title3: { sizePx: 20, weight: 400 },
  headline: { sizePx: 17, weight: 600 }, body: { sizePx: 17, weight: 400 }, callout: { sizePx: 16, weight: 400 }, subheadline: { sizePx: 15, weight: 400 },
  footnote: { sizePx: 13, weight: 400 }, caption: { sizePx: 12, weight: 400 }, caption2: { sizePx: 11, weight: 400 }
};

export function swiftColor(expression: string): string | undefined {
  const named = /^\.?(?:Color\.)?(\w+)$/.exec(expression.trim());
  if (named && systemColors[named[1]!]) return systemColors[named[1]!];
  const rgb = /Color\s*\(\s*(?:\.sRGB\s*,\s*)?red:\s*([\d.]+)\s*,\s*green:\s*([\d.]+)\s*,\s*blue:\s*([\d.]+)\s*(?:,\s*opacity:\s*([\d.]+))?/.exec(expression);
  if (rgb) {
    const channel = (value: string) => Math.round(Math.min(1, Math.max(0, Number(value))) * 255);
    const alpha = rgb[4] === undefined ? 1 : Number(rgb[4]);
    return alpha >= 0.999 ? `#${[rgb[1]!, rgb[2]!, rgb[3]!].map((value) => channel(value).toString(16).padStart(2, "0")).join("")}` : `rgba(${channel(rgb[1]!)}, ${channel(rgb[2]!)}, ${channel(rgb[3]!)}, ${alpha})`;
  }
  const hex = /Color\s*\(\s*hex:\s*(?:"#?([0-9a-fA-F]{3,8})"|0x([0-9a-fA-F]{6,8}))/.exec(expression);
  if (hex) return normalizeHexColor(hex[1] ?? hex[2]!);
  const asset = /Color\s*\(\s*"([^"]+)"/.exec(expression);
  return asset ? `var(--${asset[1]!.replace(/[^\w-]/g, "-")})` : undefined;
}

const swiftDimensionModifiers = /\.(padding|frame|cornerRadius|offset|spacing|lineSpacing|kerning|tracking|shadow|blur|border|strokeBorder|opacity)\s*\(([^()]{0,200}|[^()]*\([^()]*\)[^()]*)\)/g;
const swiftColorDeclaration = /(?:static\s+)?(?:let|var)\s+(\w+)\s*(?::\s*Color)?\s*=\s*(Color\s*\([^)]*\)|\.\w+)/g;
const swiftViewPattern = /struct\s+(\w+)\s*:\s*(?:some\s+)?(View|App|Scene)\b/g;
const swiftNavigationPattern = /(?:NavigationLink|navigationDestination|fullScreenCover|sheet)\s*[({][^)]{0,160}?(?:destination:\s*)?(\w+)\s*\(/g;

export function extractAppleFacts(files: SourceText[]): Partial<DesignFacts> {
  const colors: FactColor[] = [];
  const dimensions: FactDimension[] = [];
  const typography: FactTypography[] = [];
  const shapes: FactShape[] = [];
  const effects: FactEffect[] = [];
  const screens: FactScreen[] = [];
  const notes: string[] = [];
  let viewport: FactViewport | undefined;
  const appleViewports: FactViewport[] = [];

  for (const file of files) {
    if (/\.swift$/i.test(file.path)) readSwift(file, colors, dimensions, typography, shapes, effects, screens, notes);
    if (/Contents\.json$/i.test(file.path) && /\.colorset\//i.test(file.path)) readColorSet(file, colors);
    // Nicht der zuerst gelesene Storyboard gewinnt: alphabetisch stuende LaunchScreen.storyboard
    // vor Main.storyboard, und der Startbildschirm hat mit der App-Geometrie nichts zu tun.
    // Dieselbe Falle wie beim Windows-Extraktor (dort erschien die App auf halber Breite).
    if (/\.(storyboard|xib)$/i.test(file.path)) readStoryboard(file, dimensions, typography, colors, screens, (found) => { appleViewports.push(found); });
    if (/\.(m|mm|h)$/i.test(file.path)) readObjectiveC(file, dimensions, colors);
  }
  // Das Hauptstoryboard gewinnt; sonst die groesste Szene — ein Launch- oder Hilfsbildschirm ist
  // nie groesser als die eigentliche App.
  const isMainScene = (candidate: FactViewport) => /(^|\/)(main|shell|start)[^/]*\.(storyboard|xib)$/i.test(candidate.source);
  const isLaunchScene = (candidate: FactViewport) => /launch/i.test(candidate.source);
  viewport ??= [...appleViewports]
    .sort((left, right) => Number(isMainScene(right)) - Number(isMainScene(left))
      || Number(isLaunchScene(left)) - Number(isLaunchScene(right))
      || right.width * right.height - left.width * left.height)[0];
  return { colors, dimensions, typography, shapes, effects, screens, notes, ...(viewport ? { viewport } : {}) };
}

function readSwift(file: SourceText, colors: FactColor[], dimensions: FactDimension[], typography: FactTypography[], shapes: FactShape[], effects: FactEffect[], screens: FactScreen[], notes: string[]): void {
  for (const match of file.text.matchAll(swiftColorDeclaration)) {
    const css = swiftColor(match[2]!);
    if (css) colors.push({ name: match[1]!, css, source: file.path });
  }
  for (const [style, metrics] of Object.entries(swiftTextStyles)) {
    if (new RegExp(`\\.font\\s*\\(\\s*\\.${style}\\b`).test(file.text)) typography.push({ name: `font.${style}`, sizePx: metrics.sizePx, weight: metrics.weight, source: `${file.path} (Apple-Textstil)`, used: true });
  }
  for (const match of file.text.matchAll(/\.font\s*\(\s*\.system\s*\(\s*size:\s*([\d.]+)(?:\s*,\s*weight:\s*\.(\w+))?/g)) {
    typography.push({ name: `font.system(${match[1]})`, sizePx: Number(match[1]), source: file.path, used: true, ...(match[2] && swiftFontWeights[match[2]] ? { weight: swiftFontWeights[match[2]]! } : {}) });
  }
  for (const match of file.text.matchAll(/\.font\s*\(\s*\.custom\s*\(\s*"([^"]+)"\s*,\s*size:\s*([\d.]+)/g)) {
    typography.push({ name: `font.custom(${match[1]})`, family: match[1]!, sizePx: Number(match[2]), source: file.path, used: true });
  }
  for (const match of file.text.matchAll(swiftDimensionModifiers)) {
    const modifier = match[1]!;
    const body = match[2] ?? "";
    const numbers = [...body.matchAll(/(-?\d+(?:\.\d+)?)/g)].map((value) => Number(value[1]));
    if (modifier === "shadow") {
      const radius = /radius:\s*(-?[\d.]+)/.exec(body);
      if (!radius) continue;
      const colorMatch = /color:\s*([^,)]+)/.exec(body);
      const opacityMatch = /opacity\s*\(\s*([\d.]+)\s*\)/.exec(body);
      effects.push({ name: `${file.path}:shadow(${radius[1]})`, kind: "shadow", css: swiftShadowToCss({
        radius: Number(radius[1]),
        ...(/\bx:\s*(-?[\d.]+)/.exec(body) ? { x: Number(/\bx:\s*(-?[\d.]+)/.exec(body)![1]) } : {}),
        ...(/\by:\s*(-?[\d.]+)/.exec(body) ? { y: Number(/\by:\s*(-?[\d.]+)/.exec(body)![1]) } : {}),
        ...(colorMatch && swiftColor(colorMatch[1]!) ? { color: swiftColor(colorMatch[1]!)! } : {}),
        ...(opacityMatch ? { opacity: Number(opacityMatch[1]) } : {})
      }), source: file.path });
      continue;
    }
    if (modifier === "blur") { if (numbers.length) effects.push({ name: `${file.path}:blur(${numbers[0]})`, kind: "blur", css: `filter: blur(${numbers[0]}px)`, source: file.path }); continue; }
    if (modifier === "opacity") { if (numbers.length && numbers[0]! < 1) effects.push({ name: `${file.path}:opacity(${numbers[0]})`, kind: "opacity", css: `opacity: ${numbers[0]}`, source: file.path }); continue; }
    if (modifier === "cornerRadius") { if (numbers.length) shapes.push({ name: `${file.path}:cornerRadius(${numbers[0]})`, radiusCss: `${numbers[0]}px`, source: file.path, used: true }); continue; }
    if (modifier === "border" || modifier === "strokeBorder") {
      const width = /lineWidth:\s*(-?[\d.]+)/.exec(body)?.[1] ?? numbers.at(-1);
      const colorMatch = /(Color\s*\([^)]*\)|\.\w+)/.exec(body);
      const stroke = colorMatch ? swiftColor(colorMatch[1]!) : undefined;
      if (width !== undefined) effects.push({ name: `${file.path}:border(${width})`, kind: "stroke", css: `border: ${width}px solid ${stroke ?? "currentColor"}`, source: file.path });
      continue;
    }
    if (modifier === "frame") {
      for (const key of ["width", "height", "minWidth", "minHeight", "maxWidth", "maxHeight"]) {
        const value = new RegExp(`${key}:\\s*(-?[\\d.]+)`).exec(body);
        if (value) dimensions.push({ name: `${file.path}:frame.${key}(${value[1]})`, px: Number(value[1]), raw: `${value[1]}pt`, source: file.path, used: true });
      }
      continue;
    }
    for (const value of numbers) dimensions.push({ name: `${file.path}:${modifier}(${value})`, px: value, raw: `${value}pt`, source: file.path, used: true });
  }
  for (const match of file.text.matchAll(/RoundedRectangle\s*\(\s*cornerRadius:\s*(-?[\d.]+)/g)) shapes.push({ name: `${file.path}:RoundedRectangle(${match[1]})`, radiusCss: `${match[1]}px`, source: file.path, used: true });
  const navigations = [...file.text.matchAll(swiftNavigationPattern)].map((match) => `apple:${match[1]}`);
  for (const match of file.text.matchAll(swiftViewPattern)) {
    const name = match[1]!;
    if (match[2] === "App") { notes.push(`SwiftUI-Einstiegspunkt: ${name} (${file.path}).`); continue; }
    screens.push({ id: `apple:${name}`, name, kind: "swiftui-view", source: file.path, navigatesTo: navigations, isStart: /^(?:Content|Root|Main|Home)View$/.test(name), files: [file.path] });
  }
  if (/\.(?:regularMaterial|thinMaterial|ultraThinMaterial|thickMaterial|ultraThickMaterial)\b/.test(file.text)) effects.push({ name: `${file.path}:material`, kind: "blur", css: "backdrop-filter: blur(30px) saturate(180%)", source: file.path });
  for (const match of file.text.matchAll(/LinearGradient\s*\(\s*(?:gradient:\s*Gradient\s*\(\s*)?colors:\s*\[([^\]]{0,300})\]/g)) {
    const stops = match[1]!.split(",").map((entry) => swiftColor(entry.trim())).filter((value): value is string => Boolean(value));
    if (stops.length >= 2) effects.push({ name: `${file.path}:gradient`, kind: "gradient", css: `background-image: linear-gradient(180deg, ${stops.join(", ")})`, source: file.path });
  }
}

// Asset-Kataloge speichern Farbkomponenten als 0..1-Gleitkommazahlen oder als "0xFF"-Hexwerte.
function readColorSet(file: SourceText, colors: FactColor[]): void {
  let parsed: { colors?: Array<{ appearances?: Array<{ value?: string }>; color?: { components?: Record<string, string> } }> };
  try { parsed = JSON.parse(file.text) as typeof parsed; } catch { return; }
  const name = /([^/]+)\.colorset\//i.exec(file.path)?.[1] ?? file.path;
  for (const entry of parsed.colors ?? []) {
    const components = entry.color?.components;
    if (!components) continue;
    const channel = (raw: string | undefined) => {
      if (raw === undefined) return 0;
      const value = raw.trim();
      if (value.startsWith("0x")) return Number.parseInt(value.slice(2), 16);
      const numeric = Number(value);
      return Number.isFinite(numeric) ? (numeric <= 1 && value.includes(".") ? Math.round(numeric * 255) : Math.round(numeric)) : 0;
    };
    const alpha = Number(components.alpha ?? "1");
    const hex = `#${[components.red, components.green, components.blue].map((value) => channel(value).toString(16).padStart(2, "0")).join("")}`;
    const dark = entry.appearances?.some((appearance) => appearance.value === "dark");
    colors.push({ name: dark ? `${name}@dark` : name, css: Number.isFinite(alpha) && alpha < 1 ? colorWithOpacity(hex, alpha) : hex, source: file.path });
  }
}

function readStoryboard(file: SourceText, dimensions: FactDimension[], typography: FactTypography[], colors: FactColor[], screens: FactScreen[], setViewport: (viewport: FactViewport) => void): void {
  const root = parseXml(file.text);
  if (!root) return;
  const initial = attribute(root, "initialViewController");
  walkXml(root, (node) => {
    if (node.tag === "rect" && attribute(node, "key") === "frame") {
      const width = Number(attribute(node, "width") ?? "");
      const height = Number(attribute(node, "height") ?? "");
      if (Number.isFinite(width) && Number.isFinite(height) && width > 200 && height > 200) setViewport({ width, height, device: "Storyboard-Szene", density: 2, source: file.path });
    }
    if (node.tag === "color") {
      const key = attribute(node, "key") ?? "color";
      const red = Number(attribute(node, "red") ?? "");
      if (!Number.isFinite(red)) return;
      const toChannel = (value: string | undefined) => Math.round(Math.min(1, Math.max(0, Number(value ?? "0"))) * 255);
      const alpha = Number(attribute(node, "alpha") ?? "1");
      const hex = `#${[attribute(node, "red"), attribute(node, "green"), attribute(node, "blue")].map((value) => toChannel(value).toString(16).padStart(2, "0")).join("")}`;
      colors.push({ name: `${file.path}:${key}`, css: Number.isFinite(alpha) && alpha < 1 ? colorWithOpacity(hex, alpha) : hex, source: file.path, used: true });
    }
    if (node.tag === "fontDescription") {
      const size = Number(attribute(node, "pointSize") ?? "");
      if (Number.isFinite(size)) typography.push({ name: `${file.path}:${attribute(node, "key") ?? "font"}`, sizePx: size, source: file.path, used: true, ...(attribute(node, "name") ? { family: attribute(node, "name")! } : {}) });
    }
    if (node.tag === "constraint") {
      const constant = Number(attribute(node, "constant") ?? "");
      const identifier = attribute(node, "id") ?? attribute(node, "firstAttribute") ?? "constraint";
      if (Number.isFinite(constant) && constant !== 0) dimensions.push({ name: `${file.path}:${identifier}`, px: constant, raw: `${constant}pt`, source: file.path, used: true });
    }
    if (node.tag === "viewController" || node.tag === "tableViewController" || node.tag === "navigationController") {
      const id = attribute(node, "id");
      if (!id) return;
      const title = attribute(node, "storyboardIdentifier") ?? attribute(node, "customClass") ?? id;
      screens.push({ id: `apple:${title}`, name: title, kind: "view-controller", source: file.path, navigatesTo: [], isStart: id === initial, files: [file.path] });
    }
  });
}

function readObjectiveC(file: SourceText, dimensions: FactDimension[], colors: FactColor[]): void {
  for (const match of file.text.matchAll(/CGRectMake\s*\(\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\)/g)) {
    dimensions.push({ name: `${file.path}:rect.width(${match[3]})`, px: Number(match[3]), raw: `${match[3]}pt`, source: file.path, used: true });
    dimensions.push({ name: `${file.path}:rect.height(${match[4]})`, px: Number(match[4]), raw: `${match[4]}pt`, source: file.path, used: true });
  }
  for (const match of file.text.matchAll(/colorWithRed:\s*([\d.]+)\s*green:\s*([\d.]+)\s*blue:\s*([\d.]+)\s*alpha:\s*([\d.]+)/g)) {
    const channel = (value: string) => Math.round(Math.min(1, Math.max(0, Number(value))) * 255);
    const alpha = Number(match[4]);
    const hex = `#${[match[1]!, match[2]!, match[3]!].map((value) => channel(value).toString(16).padStart(2, "0")).join("")}`;
    colors.push({ name: `${file.path}:color(${match[1]},${match[2]},${match[3]})`, css: alpha < 1 ? colorWithOpacity(hex, alpha) : hex, source: file.path, used: true });
  }
}

export function appleFactCandidates(paths: string[]): string[] {
  return paths.filter((path) => /\.(swift|storyboard|xib|m|mm|h)$/i.test(path) || (/Contents\.json$/i.test(path) && /\.(colorset|imageset|appiconset)\//i.test(path)));
}
