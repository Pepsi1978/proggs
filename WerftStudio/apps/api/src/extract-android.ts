import { resolveDrawable } from "./android-drawable.js";
import { dimensionToPx, normalizeHexColor, type DesignFacts, type FactAsset, type FactColor, type FactDimension, type FactEffect, type FactScreen, type FactShape, type FactTheme, type FactTypography } from "./design-facts.js";
import { elevationToShadow } from "./effect-catalog.js";
import { attribute, parseXml, walkXml } from "./xml-lite.js";
import type { SourceText } from "./extract-common.js";

// Android beschreibt Design an vier Stellen: Ressourcen-XML, Drawables, Compose-Quellen und dem
// Manifest. Alle vier werden hier exakt gelesen — Farben, Maße, Typografie, Formen, Effekte und
// Screens entstehen dadurch als Messwerte statt als Vermutung.

const valuesFile = (path: string, name: string) => new RegExp(`(^|/)res/values[^/]*/${name}\\.xml$`, "i").test(path);
const isNightQualifier = (path: string) => /(^|\/)res\/values[^/]*-night[^/]*\//i.test(path);
const themeAttributePattern = /^(?:color|android:color|shape|text|elevation|android:elevation|android:text|android:font|font)/i;

export function extractAndroidFacts(files: SourceText[]): Partial<DesignFacts> {
  const colors: FactColor[] = [];
  const dimensions: FactDimension[] = [];
  const typography: FactTypography[] = [];
  const shapes: FactShape[] = [];
  const effects: FactEffect[] = [];
  const assets: FactAsset[] = [];
  const screens: FactScreen[] = [];
  const themes: FactTheme[] = [];
  const strings: Array<{ name: string; value: string }> = [];
  const notes: string[] = [];
  const colorIndex = new Map<string, string>();
  const dimensionIndex = new Map<string, number>();

  for (const file of files) {
    if (valuesFile(file.path, "colors")) readResourceValues(file, "color", (name, value) => {
      const css = normalizeHexColor(value);
      if (!css) return;
      const key = isNightQualifier(file.path) ? `${name}@night` : name;
      colorIndex.set(key, css);
      colors.push({ name: key, css, source: file.path });
    });
    if (valuesFile(file.path, "dimens")) readResourceValues(file, "dimen", (name, value) => {
      const px = dimensionToPx(value);
      if (px === undefined || Number.isNaN(px)) return;
      dimensionIndex.set(name, px);
      dimensions.push({ name, px, raw: value, source: file.path });
    });
    if (valuesFile(file.path, "strings")) readResourceValues(file, "string", (name, value) => { if (!isNightQualifier(file.path)) strings.push({ name, value }); });
  }

  const resolveColorReference = (raw: string | undefined, night = false): string | undefined => {
    if (!raw) return undefined;
    const direct = normalizeHexColor(raw);
    if (direct) return direct;
    const reference = /^[@?](?:android:)?(?:color|attr)\/(.+)$/.exec(raw.trim());
    if (!reference) return undefined;
    return (night ? colorIndex.get(`${reference[1]!}@night`) : undefined) ?? colorIndex.get(reference[1]!);
  };
  const resolveDimensionReference = (raw: string | undefined): number | undefined => {
    if (!raw) return undefined;
    const direct = dimensionToPx(raw);
    if (direct !== undefined && !Number.isNaN(direct)) return direct;
    const reference = /^@(?:android:)?dimen\/(.+)$/.exec(raw.trim());
    return reference ? dimensionIndex.get(reference[1]!) : undefined;
  };

  // Compose trennt Farbdefinition (Color.kt) und Farbschema (Theme.kt) fast immer auf zwei Dateien.
  // Deshalb werden erst ALLE Farbkonstanten gesammelt und danach die Schemata aufgeloest.
  const composeColorIndex = new Map<string, string>();
  for (const file of files) {
    if (!/\.kts?$/i.test(file.path)) continue;
    for (const match of file.text.matchAll(composeColorPattern)) {
      const hex = match[2]!.slice(2);
      const css = normalizeHexColor(hex.length === 8 ? hex : hex.padStart(8, "f"));
      if (css) composeColorIndex.set(match[1]!, css);
    }
  }

  for (const file of files) {
    if (valuesFile(file.path, "themes") || valuesFile(file.path, "styles")) readStyles(file, isNightQualifier(file.path), resolveColorReference, resolveDimensionReference, themes, typography, effects, shapes);
    if (/(^|\/)res\/drawable[^/]*\/[^/]+\.xml$/i.test(file.path)) readDrawable(file, assets, effects);
    if (/(^|\/)res\/layout[^/]*\/[^/]+\.xml$/i.test(file.path)) readLayout(file, resolveColorReference, resolveDimensionReference, screens, effects, dimensions, colors);
    if (/(^|\/)res\/navigation[^/]*\/[^/]+\.xml$/i.test(file.path)) readNavigationGraph(file, screens);
    if (/AndroidManifest\.xml$/i.test(file.path)) readManifest(file, screens, notes);
    if (/\.kts?$/i.test(file.path)) readComposeSource(file, composeColorIndex, colors, dimensions, typography, shapes, effects, screens, themes, notes);
  }

  const vectorAssets = files.filter((file) => /(^|\/)res\/(drawable|mipmap)[^/]*\//i.test(file.path) && !/\.xml$/i.test(file.path));
  for (const asset of vectorAssets) assets.push({ name: asset.path.split("/").at(-1)!.replace(/\.[^.]+$/, ""), path: asset.path, kind: "bitmap", source: asset.path });

  return { colors, dimensions, typography, shapes, effects, assets, screens, themes, strings, notes };
}

function readResourceValues(file: SourceText, tag: string, add: (name: string, value: string) => void): void {
  const root = parseXml(file.text);
  if (!root) return;
  for (const child of root.children) {
    if (child.tag !== tag) continue;
    const name = attribute(child, "name");
    const value = child.text.trim();
    if (name && value) add(name, value);
  }
}

function readStyles(
  file: SourceText,
  night: boolean,
  resolveColor: (raw: string | undefined, night?: boolean) => string | undefined,
  resolveDimension: (raw: string | undefined) => number | undefined,
  themes: FactTheme[],
  typography: FactTypography[],
  effects: FactEffect[],
  shapes: FactShape[]
): void {
  const root = parseXml(file.text);
  if (!root) return;
  for (const style of root.children) {
    if (style.tag !== "style") continue;
    const name = attribute(style, "name");
    if (!name) continue;
    const tokens: Record<string, string> = {};
    const type: FactTypography = { name: `${name}${night ? "@night" : ""}`, source: file.path };
    for (const item of style.children) {
      if (item.tag !== "item") continue;
      const key = attribute(item, "name") ?? "";
      const raw = item.text.trim();
      const color = resolveColor(raw, night);
      if (color && themeAttributePattern.test(key)) tokens[key] = color;
      if (/textSize$/i.test(key)) { const size = resolveDimension(raw); if (size !== undefined) type.sizePx = size; }
      if (/lineHeight$/i.test(key)) { const height = resolveDimension(raw); if (height !== undefined) type.lineHeightPx = height; }
      if (/letterSpacing$/i.test(key)) { const spacing = Number(raw); if (Number.isFinite(spacing)) type.letterSpacingPx = spacing; }
      if (/fontFamily$/i.test(key) && raw) type.family = raw.replace(/^@font\//, "");
      if (/textStyle$/i.test(key) && /bold/i.test(raw)) type.weight = 700;
      if (/textColor$/i.test(key) && color) tokens[key] = color;
      if (/elevation$/i.test(key)) { const dp = resolveDimension(raw); if (dp !== undefined) effects.push({ name: `${name}.elevation`, kind: "shadow", css: elevationToShadow(dp), source: `${file.path} (${dp}dp)` }); }
      if (/cornerSize|cornerRadius$/i.test(key)) { const radius = resolveDimension(raw); if (radius !== undefined) shapes.push({ name, radiusCss: `${radius}px`, source: file.path, used: true }); }
      if (/alpha$/i.test(key)) { const alpha = Number(raw); if (Number.isFinite(alpha) && alpha < 1) effects.push({ name: `${name}.alpha`, kind: "opacity", css: `opacity: ${alpha}`, source: file.path }); }
    }
    if (Object.keys(tokens).length) themes.push({ id: `${name}${night ? "@night" : ""}`, name: night ? `${name} (Nacht)` : name, tokens, source: file.path });
    if (type.sizePx !== undefined || type.family !== undefined || type.weight !== undefined) typography.push({ ...type, used: true });
  }
}

function readDrawable(file: SourceText, assets: FactAsset[], effects: FactEffect[]): void {
  const resolved = resolveDrawable(file.text);
  if (!resolved) return;
  const name = file.path.split("/").at(-1)!.replace(/\.xml$/i, "");
  assets.push({ name, path: file.path, kind: resolved.kind, source: file.path, ...(resolved.svg ? { svg: resolved.svg } : {}), ...(resolved.css ? { css: resolved.css } : {}) });
  if (resolved.css?.includes("--ripple-color")) effects.push({ name: `${name}.ripple`, kind: "ripple", css: resolved.css, source: file.path });
  if (resolved.css?.includes("gradient")) effects.push({ name: `${name}.gradient`, kind: "gradient", css: resolved.css, source: file.path });
}

const layoutDimensionAttributes: Array<[RegExp, string]> = [
  [/^layout_width$/i, "width"], [/^layout_height$/i, "height"],
  [/^layout_margin$/i, "margin"], [/^layout_marginTop$/i, "margin-top"], [/^layout_marginBottom$/i, "margin-bottom"],
  [/^layout_marginStart$|^layout_marginLeft$/i, "margin-left"], [/^layout_marginEnd$|^layout_marginRight$/i, "margin-right"],
  [/^padding$/i, "padding"], [/^paddingTop$/i, "padding-top"], [/^paddingBottom$/i, "padding-bottom"],
  [/^paddingStart$|^paddingLeft$/i, "padding-left"], [/^paddingEnd$|^paddingRight$/i, "padding-right"],
  [/^textSize$/i, "font-size"], [/^minWidth$/i, "min-width"], [/^minHeight$/i, "min-height"], [/^lineSpacingExtra$/i, "line-height-extra"]
];

const layoutColorAttributes = ["textColor", "background", "backgroundTint", "tint", "cardBackgroundColor", "strokeColor", "iconTint", "indicatorColor", "textColorHint", "drawableTint"];

function readLayout(
  file: SourceText,
  resolveColor: (raw: string | undefined, night?: boolean) => string | undefined,
  resolveDimension: (raw: string | undefined) => number | undefined,
  screens: FactScreen[],
  effects: FactEffect[],
  dimensions: FactDimension[],
  colors: FactColor[]
): void {
  const root = parseXml(file.text);
  if (!root) return;
  const name = file.path.split("/").at(-1)!.replace(/\.xml$/i, "");
  let elementCount = 0;
  walkXml(root, (node) => {
    elementCount += 1;
    const id = attribute(node, "id")?.replace(/^@\+?id\//, "");
    for (const [pattern, cssName] of layoutDimensionAttributes) {
      const raw = Object.keys(node.attributes).find((key) => pattern.test(key.slice(key.indexOf(":") + 1)));
      if (!raw) continue;
      const px = resolveDimension(node.attributes[raw]);
      if (px !== undefined && !Number.isNaN(px)) dimensions.push({ name: `${name}.${id ?? node.tag}.${cssName}`, px, raw: node.attributes[raw]!, source: file.path, used: true });
    }
    for (const attributeName of layoutColorAttributes) {
      const css = resolveColor(attribute(node, attributeName));
      if (css) colors.push({ name: `${name}.${id ?? node.tag}.${attributeName}`, css, source: file.path, used: true });
    }
    const elevation = resolveDimension(attribute(node, "elevation"));
    if (elevation !== undefined && elevation > 0) effects.push({ name: `${name}.${id ?? node.tag}.elevation`, kind: "shadow", css: elevationToShadow(elevation), source: `${file.path} (${elevation}dp)` });
    const alpha = Number(attribute(node, "alpha") ?? "");
    if (Number.isFinite(alpha) && alpha < 1) effects.push({ name: `${name}.${id ?? node.tag}.alpha`, kind: "opacity", css: `opacity: ${alpha}`, source: file.path });
    const strokeWidth = resolveDimension(attribute(node, "strokeWidth"));
    const strokeColor = resolveColor(attribute(node, "strokeColor"));
    if (strokeWidth !== undefined && strokeColor) effects.push({ name: `${name}.${id ?? node.tag}.stroke`, kind: "stroke", css: `border: ${strokeWidth}px solid ${strokeColor}`, source: file.path });
  });
  // Nur echte Bildschirme aufnehmen: winzige Item-Layouts sind Listenzellen, keine Screens.
  if (elementCount >= 3 && !/^(item_|row_|cell_|list_item|.*_item)$/i.test(name)) screens.push({ id: `layout:${name}`, name, kind: "layout-xml", source: file.path, navigatesTo: [], isStart: false, files: [file.path] });
}

function readNavigationGraph(file: SourceText, screens: FactScreen[]): void {
  const root = parseXml(file.text);
  if (!root) return;
  const startDestination = attribute(root, "startDestination")?.replace(/^@\+?id\//, "");
  walkXml(root, (node) => {
    if (!["fragment", "activity", "dialog", "destination"].includes(node.tag)) return;
    const id = attribute(node, "id")?.replace(/^@\+?id\//, "");
    if (!id) return;
    const layout = attribute(node, "layout")?.replace(/^@layout\//, "");
    screens.push({
      id: `nav:${id}`, name: attribute(node, "label") ?? id, kind: "navigation-destination", source: file.path,
      navigatesTo: node.children.filter((child) => child.tag === "action").map((child) => `nav:${(attribute(child, "destination") ?? "").replace(/^@\+?id\//, "")}`).filter((value) => value !== "nav:"),
      isStart: id === startDestination, files: layout ? [layout] : []
    });
  });
}

function readManifest(file: SourceText, screens: FactScreen[], notes: string[]): void {
  const root = parseXml(file.text);
  if (!root) return;
  walkXml(root, (node) => {
    if (node.tag !== "activity") return;
    const name = attribute(node, "name");
    if (!name) return;
    const launcher = node.children.some((filter) => filter.tag === "intent-filter" && filter.children.some((child) => child.tag === "category" && attribute(child, "name") === "android.intent.category.LAUNCHER"));
    const label = attribute(node, "label")?.replace(/^@string\//, "");
    screens.push({ id: `activity:${name.split(".").at(-1)}`, name: label ?? name.split(".").at(-1)!, kind: "activity", source: file.path, navigatesTo: [], isStart: launcher, files: [] });
    if (launcher) notes.push(`Start-Activity laut Manifest: ${name}. Der erste sichtbare Zustand der Rekonstruktion muss dieser Activity entsprechen.`);
  });
}

const composeColorPattern = /\bval\s+(\w+)\s*(?::\s*Color)?\s*=\s*Color\(\s*(0x[0-9a-fA-F]{6,8})\s*\)/g;
const composeDimensionPattern = /\bval\s+(\w+)\s*(?::\s*Dp)?\s*=\s*(-?\d+(?:\.\d+)?)\.dp\b/g;
const composeShapePattern = /\bval\s+(\w+)\s*(?::\s*\w+)?\s*=\s*RoundedCornerShape\(\s*([^)]*)\)/g;
const composeSchemePattern = /\b(?:private\s+)?val\s+(\w+)\s*(?::\s*ColorScheme)?\s*=\s*(lightColorScheme|darkColorScheme|ColorScheme)\s*\(([\s\S]{0,4000}?)\n\s*\)/g;
const composeSchemeEntryPattern = /(\w+)\s*=\s*([\w.]+)/g;
const composeTextStylePattern = /\b(\w+)\s*=\s*TextStyle\(([\s\S]{0,1200}?)\n\s*\)/g;
const composableFunctionPattern = /@Composable[\s\S]{0,400}?\bfun\s+(\w+)\s*\(/g;
const composeRoutePattern = /composable(?:<\s*(\w+)\s*>)?\s*\(\s*(?:route\s*=\s*)?"([^"]*)"?/g;
// Der Rumpf endet an der ersten schliessenden Klammer ODER am Zeilenende — `composable("x") { X() }`
// steht genauso oft einzeilig wie ueber mehrere Zeilen verteilt.
const composeRouteBodyPattern = /composable(?:<\s*(\w+)\s*>)?\s*\(\s*(?:route\s*=\s*)?"([^"]*)"[^)]*\)\s*\{([\s\S]{0,400}?)\}/g;
// Das Farbliteral MUSS vor dem Bezeichner stehen: sonst schluckt `[\w.]+` bei `Color(0xFF3157D5)`
// nur das Wort „Color“ und der eigentliche Farbwert geht verloren.
const composeColorUsagePattern = /\.(?:background|drawBehind)\s*\(\s*(Color\s*\(\s*0x[0-9a-fA-F]{6,8}\s*\)|[\w.]+)|\b(?:color|containerColor|contentColor|tint|backgroundColor|borderColor|textColor)\s*=\s*(Color\s*\(\s*0x[0-9a-fA-F]{6,8}\s*\)|[\w.]+)/g;
const composeNavigatePattern = /navigate\s*\(\s*(?:route\s*=\s*)?["<]?([\w./{}-]+)/g;
const composeModifierDimensionPattern = /\.(padding|size|height|width|offset|spacedBy|shadow|blur|border|clip|absoluteOffset|requiredSize|defaultMinSize)\s*\(([^()]{0,160})\)/g;

function readComposeSource(
  file: SourceText,
  composeColorIndex: Map<string, string>,
  colors: FactColor[],
  dimensions: FactDimension[],
  typography: FactTypography[],
  shapes: FactShape[],
  effects: FactEffect[],
  screens: FactScreen[],
  themes: FactTheme[],
  notes: string[]
): void {
  const text = file.text;
  for (const match of text.matchAll(composeColorPattern)) {
    const css = composeColorIndex.get(match[1]!);
    if (css) colors.push({ name: match[1]!, css, source: file.path });
  }
  // An einem Element gesetzte Farben sind garantiert sichtbar und werden deshalb nachgemessen.
  for (const match of text.matchAll(composeColorUsagePattern)) {
    const expression = (match[1] ?? match[2] ?? "").trim();
    const literal = /Color\s*\(\s*0x([0-9a-fA-F]{6,8})\s*\)/.exec(expression)?.[1];
    const css = literal ? normalizeHexColor(literal.length === 8 ? literal : literal.padStart(8, "f")) : composeColorIndex.get(expression.replace(/^.*\./, ""));
    if (css) colors.push({ name: expression, css, source: file.path, used: true });
  }
  for (const match of text.matchAll(composeDimensionPattern)) dimensions.push({ name: match[1]!, px: Number(match[2]), raw: `${match[2]}.dp`, source: file.path });
  for (const match of text.matchAll(composeShapePattern)) {
    const radii = [...match[2]!.matchAll(/(-?\d+(?:\.\d+)?)\.dp/g)].map((value) => `${Number(value[1])}px`);
    if (radii.length) shapes.push({ name: match[1]!, radiusCss: radii.length === 1 ? radii[0]! : radii.join(" "), source: file.path });
  }
  for (const match of text.matchAll(composeSchemePattern)) {
    const tokens: Record<string, string> = {};
    for (const entry of match[3]!.matchAll(composeSchemeEntryPattern)) {
      const value = composeColorIndex.get(entry[2]!) ?? normalizeHexColor(entry[2]!.replace(/^0x/, ""));
      if (value) tokens[entry[1]!] = value;
    }
    if (Object.keys(tokens).length) themes.push({ id: match[1]!, name: match[2] === "darkColorScheme" ? `${match[1]} (Dunkel)` : match[1]!, tokens, source: file.path });
  }
  for (const match of text.matchAll(composeTextStylePattern)) {
    const body = match[2]!;
    const type: FactTypography = { name: match[1]!, source: file.path };
    const size = /fontSize\s*=\s*(-?\d+(?:\.\d+)?)\.(?:sp|dp)/.exec(body);
    const lineHeight = /lineHeight\s*=\s*(-?\d+(?:\.\d+)?)\.(?:sp|dp)/.exec(body);
    const spacing = /letterSpacing\s*=\s*(-?\d+(?:\.\d+)?)\.(?:sp|dp|em)/.exec(body);
    const weight = /fontWeight\s*=\s*FontWeight\.(\w+)/.exec(body);
    const family = /fontFamily\s*=\s*([\w.]+)/.exec(body);
    if (size) type.sizePx = Number(size[1]);
    if (lineHeight) type.lineHeightPx = Number(lineHeight[1]);
    if (spacing) type.letterSpacingPx = Number(spacing[1]);
    const weightValue = weight ? composeFontWeight(weight[1]!) : undefined;
    if (weightValue !== undefined) type.weight = weightValue;
    if (family) type.family = family[1]!;
    if (type.sizePx !== undefined || type.weight !== undefined || type.family !== undefined) typography.push({ ...type, used: true });
  }
  for (const match of text.matchAll(composeModifierDimensionPattern)) {
    const values = [...match[2]!.matchAll(/(-?\d+(?:\.\d+)?)\.dp/g)].map((value) => Number(value[1]));
    if (!values.length) continue;
    if (match[1] === "shadow") effects.push({ name: `${file.path}:shadow(${values[0]}dp)`, kind: "shadow", css: elevationToShadow(values[0]!), source: file.path });
    else if (match[1] === "blur") effects.push({ name: `${file.path}:blur(${values[0]}dp)`, kind: "blur", css: `filter: blur(${values[0]}px)`, source: file.path });
    else if (match[1] === "border") effects.push({ name: `${file.path}:border(${values[0]}dp)`, kind: "stroke", css: `border-width: ${values[0]}px`, source: file.path });
    else for (const value of values) dimensions.push({ name: `${file.path}:${match[1]}(${value}dp)`, px: value, raw: `${value}.dp`, source: file.path, used: true });
  }
  const routes = [...text.matchAll(composeRoutePattern)].map((match) => match[2] || match[1] || "").filter(Boolean);
  const navigations = [...text.matchAll(composeNavigatePattern)].map((match) => match[1]!).filter(Boolean);
  const start = /startDestination\s*=\s*(?:route\s*=\s*)?["<]?([\w./{}-]+)/.exec(text)?.[1];
  // `composable("home") { HomeScreen() }` verraet die direkte Zuordnung Route→Composable. Ohne sie
  // entstuenden zwei Screens fuer denselben Bildschirm — und er wuerde doppelt aufgebaut.
  const routeToComposable = new Map<string, string>();
  for (const match of text.matchAll(composeRouteBodyPattern)) {
    const route = match[2] || match[1];
    const target = /\b([A-Z]\w*(?:Screen|Page|View|Route|Pane|Dialog|Sheet))\s*\(/.exec(match[3] ?? "")?.[1];
    if (route && target) routeToComposable.set(route, target);
  }
  const screenIdForRoute = (route: string) => routeToComposable.has(route) ? `compose:${routeToComposable.get(route)}` : `route:${route}`;
  for (const match of text.matchAll(composableFunctionPattern)) {
    const name = match[1]!;
    if (!/(?:Screen|Page|Route|View|Activity|Dialog|Sheet|Pane)$/.test(name)) continue;
    const route = [...routeToComposable.entries()].find(([, target]) => target === name)?.[0]
      ?? routes.find((value) => value.toLowerCase().includes(name.replace(/(?:Screen|Page|Route|View|Activity|Dialog|Sheet|Pane)$/, "").toLowerCase()));
    screens.push({ id: `compose:${name}`, name, kind: "composable", source: file.path, ...(route ? { route } : {}), navigatesTo: navigations.map(screenIdForRoute), isStart: route !== undefined && route === start, files: [file.path] });
  }
  if (/NavHost\s*\(/.test(text)) {
    if (start) notes.push(`Compose-Navigation: Startziel „${start}“ (${file.path}). Dieser Screen muss beim Öffnen sichtbar sein.`);
    for (const route of routes) {
      const target = routeToComposable.get(route);
      // Ist der Bildschirm bereits als Composable erfasst, wird hier nur noch Route und Start ergaenzt.
      screens.push({ id: screenIdForRoute(route), name: target ?? route, kind: target ? "composable" : "nav-route", source: file.path, route, navigatesTo: [], isStart: start === route, files: [file.path] });
    }
  }
}

const composeFontWeights: Record<string, number> = { Thin: 100, ExtraLight: 200, Light: 300, Normal: 400, Medium: 500, SemiBold: 600, Bold: 700, ExtraBold: 800, Black: 900 };
function composeFontWeight(name: string): number | undefined {
  if (composeFontWeights[name] !== undefined) return composeFontWeights[name];
  const numeric = /^W(\d)00$/.exec(name);
  return numeric ? Number(numeric[1]) * 100 : undefined;
}

// Nur diese Dateien tragen Designwerte. Der Extraktor laedt sie vollstaendig, waehrend alles
// andere gar nicht erst in die teure KI-Analyse geht.
export function androidFactCandidates(paths: string[]): string[] {
  return paths.filter((path) => /(^|\/)res\/(values|drawable|mipmap|layout|navigation|font|color)[^/]*\/[^/]+\.xml$/i.test(path) || /AndroidManifest\.xml$/i.test(path) || /\.kts?$/i.test(path));
}
