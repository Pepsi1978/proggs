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
  // Der NavHost steht fast nie in derselben Datei wie die Bildschirme. Farb- UND Routenzuordnung
  // muessen deshalb projektweit vorliegen, bevor die einzelnen Dateien ausgewertet werden.
  const composeColorIndex = new Map<string, string>();
  const kotlinSources = files.filter((file) => /\.kts?$/i.test(file.path));
  for (const file of kotlinSources) {
    for (const match of file.text.matchAll(composeColorPattern)) {
      const hex = match[2]!.slice(2);
      const css = normalizeHexColor(hex.length === 8 ? hex : hex.padStart(8, "f"));
      if (css) composeColorIndex.set(match[1]!, css);
    }
  }
  const navigationIndex = buildComposeNavigationIndex(kotlinSources);
  if (navigationIndex.startComposable) notes.push(`Compose-Navigation ohne NavHost: „${navigationIndex.startComposable}“ ist der Startbildschirm (Aufzählungs-Navigation). Er muss beim Öffnen sichtbar sein.`);

  for (const file of files) {
    if (valuesFile(file.path, "themes") || valuesFile(file.path, "styles")) readStyles(file, isNightQualifier(file.path), resolveColorReference, resolveDimensionReference, themes, typography, effects, shapes);
    if (/(^|\/)res\/drawable[^/]*\/[^/]+\.xml$/i.test(file.path)) readDrawable(file, assets, effects);
    if (/(^|\/)res\/layout[^/]*\/[^/]+\.xml$/i.test(file.path)) readLayout(file, resolveColorReference, resolveDimensionReference, screens, effects, dimensions, colors);
    if (/(^|\/)res\/navigation[^/]*\/[^/]+\.xml$/i.test(file.path)) readNavigationGraph(file, screens);
    if (/AndroidManifest\.xml$/i.test(file.path)) readManifest(file, screens, notes);
    if (/\.kts?$/i.test(file.path)) readComposeSource(file, composeColorIndex, navigationIndex, colors, dimensions, typography, shapes, effects, screens, themes, notes);
  }

  // Bei einer Compose-App ist die Launcher-Activity nur die Huelle — sichtbar ist das
  // Start-Composable. Ohne diese Korrektur galt „MainActivity" als Einstiegsbildschirm.
  const composeStartId = navigationIndex.startComposable ? `compose:${navigationIndex.startComposable}` : undefined;
  if (composeStartId && screens.some((screen) => screen.id === composeStartId)) {
    for (const screen of screens) screen.isStart = screen.id === composeStartId;
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
// Nicht nur Material3: sehr viele Compose-Apps definieren ein EIGENES Farbschema als Datenklasse
// (`val DarkPmColors = PmColors(background = Color(0xFF181209), …)`). Ohne diese Verallgemeinerung
// bleiben deren Themefarben komplett unentdeckt.
const composeSchemePattern = /\b(?:private\s+)?val\s+(\w+)\s*(?::\s*\w+)?\s*=\s*(\w+)\s*\(([\s\S]{0,6000}?)\n\s*\)/g;
const composeSchemeEntryPattern = /(\w+)\s*=\s*(Color\s*\(\s*0x[0-9a-fA-F]{6,8}\s*\)|[\w.]+)/g;
const composeSchemeMinimumColors = 3;
const composeTextStylePattern = /\b(\w+)\s*=\s*TextStyle\(([\s\S]{0,1200}?)\n\s*\)/g;
const composableFunctionPattern = /@Composable[\s\S]{0,400}?\bfun\s+(\w+)\s*\(/g;
const composeRoutePattern = /composable(?:<\s*(\w+)\s*>)?\s*\(\s*(?:route\s*=\s*)?"([^"]*)"?/g;
// Echte NavGraphs schreiben `composable("consent", enterTransition = { fadeIn(tween(600)) }) { … }`:
// die Argumentliste enthaelt selbst Klammern und Zeilenumbrueche. Statt sie zu parsen, wird hinter
// dem Routen-Literal ein Textfenster genommen und darin der erste Bildschirm-Aufruf gesucht.
// Das Textfenster steht im Lookahead: wuerde es mitkonsumiert, verschluckte jeder Treffer den
// jeweils naechsten Eintrag und nur jede zweite Route waere erkannt.
const composeRouteBodyPattern = /composable(?:<\s*(\w+)\s*>)?\s*\(\s*(?:route\s*=\s*)?"([^"]*)"(?=([\s\S]{0,600}))/g;
const composeScreenCallPattern = /\b([A-Z]\w*(?:Screen|Page|View|Route|Pane|Dialog|Sheet))\s*\(/;
// Das Fenster darf nicht in den naechsten Eintrag hineinreichen, sonst erbt eine Route den
// Bildschirm ihres Nachfolgers.
const routeBodyTarget = (window: string): string | undefined => composeScreenCallPattern.exec(window.split(/composable\s*[(<]/)[0]!)?.[1];
// Das Farbliteral MUSS vor dem Bezeichner stehen: sonst schluckt `[\w.]+` bei `Color(0xFF3157D5)`
// nur das Wort „Color“ und der eigentliche Farbwert geht verloren.
const composeColorUsagePattern = /\.(?:background|drawBehind)\s*\(\s*(Color\s*\(\s*0x[0-9a-fA-F]{6,8}\s*\)|[\w.]+)|\b(?:color|containerColor|contentColor|tint|backgroundColor|borderColor|textColor)\s*=\s*(Color\s*\(\s*0x[0-9a-fA-F]{6,8}\s*\)|[\w.]+)/g;
const composeNavigatePattern = /navigate\s*\(\s*(?:route\s*=\s*)?["<]?([\w./{}-]+)/g;
const composeModifierDimensionPattern = /\.(padding|size|height|width|offset|spacedBy|shadow|blur|border|clip|absoluteOffset|requiredSize|defaultMinSize)\s*\(([^()]{0,160})\)/g;

// Sehr viele Compose-Apps navigieren OHNE NavHost: ein `enum class AppScreen { START, … }`, eine
// `when`-Verzweigung auf den Bildschirm und `navigate(AppScreen.X)` an den Schaltflaechen. Wer nur
// NavHost auswertet, findet dort weder Startbildschirm noch ein einziges Klickziel — die
// Rekonstruktion wurde dadurch zu einer Sammlung unverbundener Standbilder.
const enumClassPattern = /\benum\s+class\s+(\w+)\s*(?:\([^)]*\))?\s*(?::[^{]{0,120})?\{([\s\S]{0,4000}?)\}/g;
const enumBranchPattern = /\b([A-Z]\w*)\.(\w+)\s*->\s*([A-Z]\w*)\s*\(/g;
const enumStateHolderPattern = /mutableStateOf(?:<[^>]{0,80}>)?\s*\(([\s\S]{0,240}?)\)/g;
export type ComposeNavigationIndex = {
  routeToComposable: Map<string, string>;
  // "AppScreen.START" → "StartScreen"
  enumScreenToComposable: Map<string, string>;
  // Namen der Bildschirm-Aufzaehlungen, z. B. "AppScreen"
  enumNames: Set<string>;
  startComposable?: string;
};

export function buildComposeNavigationIndex(sources: Array<{ path: string; text: string }>): ComposeNavigationIndex {
  const routeToComposable = new Map<string, string>();
  const enumScreenToComposable = new Map<string, string>();
  const enumValues = new Map<string, string[]>();
  const branches: Array<{ enumName: string; value: string; composable: string }> = [];
  const stateHolders: string[] = [];
  for (const file of sources) {
    for (const match of file.text.matchAll(composeRouteBodyPattern)) {
      const route = match[2] || match[1];
      const target = routeBodyTarget(match[3] ?? "");
      if (route && target) routeToComposable.set(route, target);
    }
    for (const match of file.text.matchAll(enumClassPattern)) {
      const values = match[2]!.split(/[,;\n]/).map((entry) => /^\s*([A-Za-z_]\w*)/.exec(entry)?.[1]).filter((value): value is string => Boolean(value));
      if (values.length) enumValues.set(match[1]!, values);
    }
    // Nur Zweige, die WIRKLICH einen Bildschirm zeigen. Ohne die Endungs- und Aufzaehlungspruefung
    // wurde jedes `when`-Muster mitgenommen und Bausteine wie `Icon(` oder `Column(` galten als
    // Bildschirm — der Startbildschirm war dann ein Symbol.
    for (const match of file.text.matchAll(enumBranchPattern)) {
      if (!/(?:Screen|Page|Route|View|Dialog|Sheet|Pane)$/.test(match[3]!)) continue;
      branches.push({ enumName: match[1]!, value: match[2]!, composable: match[3]! });
    }
    for (const match of file.text.matchAll(enumStateHolderPattern)) stateHolders.push(match[1]!);
  }
  // Die Aufzaehlung steht fast nie in derselben Datei wie die Verzweigung — deshalb erst jetzt pruefen.
  for (const branch of branches) {
    if (!enumValues.get(branch.enumName)?.includes(branch.value)) continue;
    enumScreenToComposable.set(`${branch.enumName}.${branch.value}`, branch.composable);
  }
  // Der Startbildschirm ist der Wert, mit dem der Bildschirmzustand initialisiert wird. Steht dort
  // ein Ausdruck („laufende Sitzung ? SESSION : START“), zaehlt der letzte Zweig — der Normalfall.
  // Erst wenn gar nichts zu lesen ist, gilt der erste Eintrag der Aufzaehlung.
  let startKey: string | undefined;
  for (const holder of stateHolders) {
    const references = [...holder.matchAll(/\b([A-Z]\w*)\.(\w+)\b/g)].map((match) => `${match[1]}.${match[2]}`).filter((key) => enumScreenToComposable.has(key));
    if (references.length) startKey = references.at(-1);
  }
  if (!startKey) {
    for (const [name, values] of enumValues) {
      const candidate = values.map((value) => `${name}.${value}`).find((key) => enumScreenToComposable.has(key));
      if (candidate) { startKey = candidate; break; }
    }
  }
  const enumNames = new Set([...enumScreenToComposable.keys()].map((key) => key.split(".")[0]!));
  return { routeToComposable, enumScreenToComposable, enumNames, ...(startKey && enumScreenToComposable.get(startKey) ? { startComposable: enumScreenToComposable.get(startKey)! } : {}) };
}

function readComposeSource(
  file: SourceText,
  composeColorIndex: Map<string, string>,
  navigationIndex: ComposeNavigationIndex,
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
      const literal = /Color\s*\(\s*0x([0-9a-fA-F]{6,8})\s*\)/.exec(entry[2]!)?.[1];
      const value = literal ? normalizeHexColor(literal.length === 8 ? literal : literal.padStart(8, "f")) : composeColorIndex.get(entry[2]!.replace(/^.*\./, ""));
      if (value) tokens[entry[1]!] = value;
    }
    // Bei einem benannten Material-Schema genuegt ein Farbwert; bei beliebigen Konstruktoraufrufen
    // braucht es mehrere, sonst wird jeder Funktionsaufruf mit einer Farbe zum „Theme“.
    const materialScheme = /^(?:light|dark)?ColorScheme$/i.test(match[2]!);
    if (Object.keys(tokens).length < (materialScheme ? 1 : composeSchemeMinimumColors)) continue;
    const dark = /darkColorScheme/.test(match[2]!) || /dark|night|dunkel/i.test(match[1]!);
    themes.push({ id: match[1]!, name: dark ? `${match[1]} (Dunkel)` : match[1]!, tokens, source: file.path });
    // Die Schemafarben sind per Definition sichtbar und gehoeren damit in die Nachmessung.
    for (const [token, value] of Object.entries(tokens)) colors.push({ name: `${match[1]}.${token}`, css: value, source: file.path, used: true });
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
  const { routeToComposable, enumScreenToComposable, enumNames, startComposable } = navigationIndex;
  // `navigate(AppScreen.HISTORY)` ist eine Aufzaehlungs-Navigation, KEINE Route. Ohne diese
  // Unterscheidung entstuenden Geisterziele wie „route:AppScreen.HISTORY", die auf nichts zeigen.
  const isEnumReference = (value: string) => enumScreenToComposable.has(value) || enumNames.has(value.split(".")[0] ?? "");
  const routes = [...text.matchAll(composeRoutePattern)].map((match) => match[2] || match[1] || "").filter(Boolean);
  const navigations = [...text.matchAll(composeNavigatePattern)].map((match) => match[1]!).filter(Boolean);
  const start = /startDestination\s*=\s*(?:route\s*=\s*)?["<]?([\w./{}-]+)/.exec(text)?.[1];
  // `composable("home") { HomeScreen() }` verraet die direkte Zuordnung Route→Composable. Ohne sie
  // entstuenden zwei Screens fuer denselben Bildschirm — und er wuerde doppelt aufgebaut.
  const screenIdForRoute = (route: string) => routeToComposable.has(route) ? `compose:${routeToComposable.get(route)}` : `route:${route}`;
  // Eine einzige Datei enthaelt oft ALLE Bildschirme (Screens.kt). Wuerde jeder von ihnen die
  // Navigationsziele der ganzen Datei erben, waere am Ende jeder Bildschirm mit jedem verlinkt.
  // Deshalb zaehlt der Textbereich der jeweiligen Funktion — nur bei genau einem Bildschirm pro
  // Datei bleibt die bisherige, dateiweite Zuordnung erhalten.
  const composableMatches = [...text.matchAll(composableFunctionPattern)];
  const screenMatches = composableMatches.filter((match) => /(?:Screen|Page|Route|View|Activity|Dialog|Sheet|Pane)$/.test(match[1]!));
  for (const match of screenMatches) {
    const name = match[1]!;
    const position = composableMatches.indexOf(match);
    const bodyStart = match.index ?? 0;
    const bodyEnd = composableMatches[position + 1]?.index ?? text.length;
    const body = screenMatches.length > 1 ? text.slice(bodyStart, bodyEnd) : text;
    const route = [...routeToComposable.entries()].find(([, target]) => target === name)?.[0]
      ?? routes.find((value) => value.toLowerCase().includes(name.replace(/(?:Screen|Page|Route|View|Activity|Dialog|Sheet|Pane)$/, "").toLowerCase()));
    const routeTargets = (screenMatches.length > 1 ? [...body.matchAll(composeNavigatePattern)].map((entry) => entry[1]!).filter(Boolean) : navigations).filter((value) => !isEnumReference(value)).map(screenIdForRoute);
    // Enum-Navigation: jede im Rumpf genannte Bildschirm-Konstante ist ein echtes Klickziel.
    const enumTargets = [...enumScreenToComposable.entries()]
      .filter(([key, target]) => target !== name && body.includes(key))
      .map(([, target]) => `compose:${target}`);
    screens.push({
      id: `compose:${name}`, name, kind: "composable", source: file.path, ...(route ? { route } : {}),
      navigatesTo: [...new Set([...routeTargets, ...enumTargets])],
      isStart: (route !== undefined && route === start) || name === startComposable,
      files: [file.path]
    });
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
