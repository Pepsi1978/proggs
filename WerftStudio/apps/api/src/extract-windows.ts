import { dimensionToPx, normalizeHexColor, type DesignFacts, type FactColor, type FactDimension, type FactEffect, type FactScreen, type FactShape, type FactTheme, type FactTypography, type FactViewport } from "./design-facts.js";
import { wpfDropShadowToCss } from "./effect-catalog.js";
import { attribute, parseXml, walkXml, type XmlNode } from "./xml-lite.js";
import type { SourceText } from "./extract-common.js";

// WPF/WinUI messen in geraeteunabhaengigen Pixeln, die 1:1 CSS-Pixeln entsprechen. Ressourcen-
// Dictionaries, Fensterdefinitionen und Effekte lassen sich dadurch exakt uebernehmen.

const namedColors: Record<string, string> = { black: "#000000", white: "#ffffff", transparent: "transparent", red: "#ff0000", green: "#008000", blue: "#0000ff", gray: "#808080", lightgray: "#d3d3d3", darkgray: "#a9a9a9", orange: "#ffa500", yellow: "#ffff00" };

export function xamlColor(raw: string | undefined): string | undefined {
  if (!raw) return undefined;
  const value = raw.trim();
  const direct = normalizeHexColor(value);
  if (direct) return direct;
  const named = namedColors[value.toLowerCase()];
  if (named) return named;
  const staticResource = /^\{(?:Static|Dynamic|Theme)Resource\s+([\w.]+)\}$/i.exec(value);
  return staticResource ? `var(--${staticResource[1]!.replace(/\./g, "-")})` : undefined;
}

// Thickness kennt 1, 2 oder 4 Werte in der Reihenfolge links,oben,rechts,unten — CSS erwartet
// oben,rechts,unten,links. Ohne diese Drehung sitzen Abstaende auf der falschen Seite.
export function thicknessToCss(raw: string | undefined): string | undefined {
  if (!raw) return undefined;
  const parts = raw.split(/[\s,]+/).filter(Boolean).map(Number);
  if (!parts.length || parts.some((value) => !Number.isFinite(value))) return undefined;
  if (parts.length === 1) return `${parts[0]}px`;
  if (parts.length === 2) return `${parts[1]}px ${parts[0]}px`;
  if (parts.length === 4) return `${parts[1]}px ${parts[2]}px ${parts[3]}px ${parts[0]}px`;
  return undefined;
}

export function cornerRadiusToCss(raw: string | undefined): string | undefined {
  if (!raw) return undefined;
  const parts = raw.split(/[\s,]+/).filter(Boolean).map(Number);
  if (!parts.length || parts.some((value) => !Number.isFinite(value))) return undefined;
  return parts.length === 1 ? `${parts[0]}px` : parts.length === 4 ? `${parts[0]}px ${parts[1]}px ${parts[2]}px ${parts[3]}px` : `${parts.join("px ")}px`;
}

const windowTags = new Set(["Window", "Page", "UserControl", "NavigationView", "ContentDialog", "Frame"]);
const dimensionAttributes: Array<[string, string]> = [["Width", "width"], ["Height", "height"], ["MinWidth", "min-width"], ["MinHeight", "min-height"], ["MaxWidth", "max-width"], ["MaxHeight", "max-height"], ["FontSize", "font-size"]];

export function extractWindowsFacts(files: SourceText[]): Partial<DesignFacts> {
  const colors: FactColor[] = [];
  const dimensions: FactDimension[] = [];
  const typography: FactTypography[] = [];
  const shapes: FactShape[] = [];
  const effects: FactEffect[] = [];
  const screens: FactScreen[] = [];
  const themes: FactTheme[] = [];
  const notes: string[] = [];
  let viewport: FactViewport | undefined;

  for (const file of files) {
    if (/\.(xaml|axaml)$/i.test(file.path)) {
      const root = parseXml(file.text);
      if (!root) continue;
      const isResourceDictionary = root.tag.endsWith("ResourceDictionary");
      const name = file.path.split("/").at(-1)!.replace(/\.(xaml|axaml)$/i, "");
      const tokens: Record<string, string> = {};
      walkXml(root, (node) => {
        const key = attribute(node, "Key") ?? attribute(node, "x:Key");
        readXamlBrush(node, key, file.path, colors, tokens);
        if (key && /^(Thickness|CornerRadius|Double|System:Double)$/i.test(node.tag)) {
          const value = node.text.trim();
          const px = dimensionToPx(value) ?? Number(value.split(/[\s,]+/)[0]);
          if (Number.isFinite(px)) dimensions.push({ name: key, px: px as number, raw: value, source: file.path });
        }
        for (const [attributeName, cssName] of dimensionAttributes) {
          const raw = attribute(node, attributeName);
          const px = raw === undefined ? undefined : dimensionToPx(raw);
          if (raw !== undefined && px !== undefined && !Number.isNaN(px)) dimensions.push({ name: `${name}.${elementKey(node)}.${cssName}`, px, raw, source: file.path, used: true });
        }
        for (const [attributeName, cssName] of [["Margin", "margin"], ["Padding", "padding"], ["BorderThickness", "border-width"]] as const) {
          const css = thicknessToCss(attribute(node, attributeName));
          if (css) effects.push({ name: `${name}.${elementKey(node)}.${cssName}`, kind: "stroke", css: `${cssName}: ${css}`, source: file.path });
        }
        const radius = cornerRadiusToCss(attribute(node, "CornerRadius"));
        if (radius) shapes.push({ name: `${name}.${elementKey(node)}`, radiusCss: radius, source: file.path, used: true });
        readXamlTypography(node, `${name}.${elementKey(node)}`, file.path, typography);
        readXamlEffects(node, `${name}.${elementKey(node)}`, file.path, effects);
        readXamlGradient(node, `${name}.${elementKey(node)}`, file.path, effects);
        const opacity = Number(attribute(node, "Opacity") ?? "");
        if (Number.isFinite(opacity) && opacity < 1) effects.push({ name: `${name}.${elementKey(node)}.opacity`, kind: "opacity", css: `opacity: ${opacity}`, source: file.path });
      });
      if (Object.keys(tokens).length) themes.push({ id: name, name: /dark/i.test(file.path) ? `${name} (Dunkel)` : name, tokens, source: file.path });
      if (windowTags.has(root.tag.replace(/^.*:/, ""))) {
        const width = dimensionToPx(attribute(root, "Width") ?? "");
        const height = dimensionToPx(attribute(root, "Height") ?? "");
        const title = attribute(root, "Title") ?? name;
        screens.push({ id: `xaml:${name}`, name: title, kind: root.tag.replace(/^.*:/, "").toLowerCase(), source: file.path, navigatesTo: [], isStart: /main|shell|start/i.test(name), files: [file.path] });
        if (!viewport && width !== undefined && height !== undefined && !Number.isNaN(width) && !Number.isNaN(height)) viewport = { width, height, device: `${title} (Windows)`, density: 1, source: file.path };
      } else if (!isResourceDictionary && root.children.length) {
        screens.push({ id: `xaml:${name}`, name, kind: "xaml-view", source: file.path, navigatesTo: [], isStart: false, files: [file.path] });
      }
    }
    if (/\.(cs|vb|fs)$/i.test(file.path)) readWindowsCodeBehind(file, screens, notes);
  }
  return { colors, dimensions, typography, shapes, effects, screens, themes, notes, ...(viewport ? { viewport } : {}) };
}

const elementKey = (node: XmlNode) => attribute(node, "Name") ?? attribute(node, "x:Name") ?? attribute(node, "Key") ?? attribute(node, "x:Key") ?? node.tag.replace(/^.*:/, "");

function readXamlBrush(node: XmlNode, key: string | undefined, path: string, colors: FactColor[], tokens: Record<string, string>): void {
  if (!key) return;
  if (node.tag.endsWith("SolidColorBrush")) {
    const css = xamlColor(attribute(node, "Color") ?? node.text.trim());
    if (css) { colors.push({ name: key, css, source: path }); tokens[key] = css; }
    return;
  }
  if (node.tag.endsWith("Color")) {
    const css = xamlColor(node.text.trim());
    if (css) { colors.push({ name: key, css, source: path }); tokens[key] = css; }
  }
}

const xamlFontWeights: Record<string, number> = { thin: 100, extralight: 200, ultralight: 200, light: 300, normal: 400, regular: 400, medium: 500, semibold: 600, demibold: 600, bold: 700, extrabold: 800, black: 900 };

function readXamlTypography(node: XmlNode, name: string, path: string, typography: FactTypography[]): void {
  const size = dimensionToPx(attribute(node, "FontSize") ?? "");
  const family = attribute(node, "FontFamily");
  const weightRaw = attribute(node, "FontWeight");
  const spacing = Number(attribute(node, "CharacterSpacing") ?? "");
  const lineHeight = dimensionToPx(attribute(node, "LineHeight") ?? "");
  if (size === undefined && !family && !weightRaw) return;
  const weight = weightRaw ? xamlFontWeights[weightRaw.toLowerCase()] ?? Number(weightRaw) : undefined;
  typography.push({
    name, source: path, used: true,
    ...(family ? { family } : {}),
    ...(size !== undefined && !Number.isNaN(size) ? { sizePx: size } : {}),
    ...(weight !== undefined && Number.isFinite(weight) ? { weight } : {}),
    ...(lineHeight !== undefined && !Number.isNaN(lineHeight) ? { lineHeightPx: lineHeight } : {}),
    // WinUI gibt CharacterSpacing in 1/1000 em an — direkt als px uebernommen waere es um Faktor 1000 falsch.
    ...(Number.isFinite(spacing) && spacing !== 0 ? { letterSpacingPx: (spacing / 1000) * (size && !Number.isNaN(size) ? size : 14) } : {})
  });
}

function readXamlEffects(node: XmlNode, name: string, path: string, effects: FactEffect[]): void {
  if (node.tag.endsWith("DropShadowEffect")) {
    effects.push({ name: `${name}.shadow`, kind: "shadow", css: wpfDropShadowToCss({
      ...(Number.isFinite(Number(attribute(node, "BlurRadius"))) ? { blurRadius: Number(attribute(node, "BlurRadius")) } : {}),
      ...(Number.isFinite(Number(attribute(node, "ShadowDepth"))) ? { shadowDepth: Number(attribute(node, "ShadowDepth")) } : {}),
      ...(Number.isFinite(Number(attribute(node, "Direction"))) ? { direction: Number(attribute(node, "Direction")) } : {}),
      ...(Number.isFinite(Number(attribute(node, "Opacity"))) ? { opacity: Number(attribute(node, "Opacity")) } : {}),
      ...(xamlColor(attribute(node, "Color")) ? { color: xamlColor(attribute(node, "Color"))! } : {})
    }), source: path });
  }
  if (node.tag.endsWith("BlurEffect")) {
    const radius = Number(attribute(node, "Radius") ?? "5");
    if (Number.isFinite(radius)) effects.push({ name: `${name}.blur`, kind: "blur", css: `filter: blur(${radius}px)`, source: path });
  }
  if (node.tag.endsWith("ThemeShadow") || node.tag.endsWith("AttachedCardShadow")) {
    const depth = Number(attribute(node, "BlurRadius") ?? attribute(node, "Offset") ?? "8");
    effects.push({ name: `${name}.shadow`, kind: "shadow", css: wpfDropShadowToCss({ blurRadius: Number.isFinite(depth) ? depth : 8, shadowDepth: 4, direction: 270, opacity: 0.25 }), source: path });
  }
  if (node.tag.endsWith("AcrylicBrush") || node.tag.endsWith("DesktopAcrylicBackdrop")) {
    const tint = xamlColor(attribute(node, "TintColor"));
    effects.push({ name: `${name}.acrylic`, kind: "blur", css: `backdrop-filter: blur(30px) saturate(125%)${tint ? `; background-color: ${tint}` : ""}`, source: path });
  }
}

function readXamlGradient(node: XmlNode, name: string, path: string, effects: FactEffect[]): void {
  if (!node.tag.endsWith("LinearGradientBrush") && !node.tag.endsWith("RadialGradientBrush")) return;
  const stops = node.children.flatMap((child) => child.tag.endsWith("GradientStop") ? [child] : child.children.filter((inner) => inner.tag.endsWith("GradientStop")))
    .map((stop) => { const color = xamlColor(attribute(stop, "Color")); const offset = Number(attribute(stop, "Offset") ?? ""); return color ? `${color}${Number.isFinite(offset) ? ` ${Math.round(offset * 100)}%` : ""}` : undefined; })
    .filter((value): value is string => Boolean(value));
  if (stops.length < 2) return;
  if (node.tag.endsWith("RadialGradientBrush")) { effects.push({ name: `${name}.gradient`, kind: "gradient", css: `background-image: radial-gradient(circle, ${stops.join(", ")})`, source: path }); return; }
  const start = (attribute(node, "StartPoint") ?? "0,0").split(",").map(Number);
  const end = (attribute(node, "EndPoint") ?? "0,1").split(",").map(Number);
  // XAML gibt Start-/Endpunkt relativ an; CSS braucht daraus den Winkel im Uhrzeigersinn ab „nach oben“.
  const angle = Number.isFinite(start[0]) && Number.isFinite(end[0]) ? Math.round((Math.atan2((end[0]! - start[0]!), -(end[1]! - start[1]!)) * 180) / Math.PI + 360) % 360 : 180;
  effects.push({ name: `${name}.gradient`, kind: "gradient", css: `background-image: linear-gradient(${angle}deg, ${stops.join(", ")})`, source: path });
}

function readWindowsCodeBehind(file: SourceText, screens: FactScreen[], notes: string[]): void {
  for (const match of file.text.matchAll(/(?:Navigate|NavigateToType|Frame\.Navigate)\s*\(\s*typeof\s*\(\s*([\w.]+)\s*\)/g)) {
    const target = `xaml:${match[1]!.split(".").at(-1)}`;
    const owner = screens.find((screen) => screen.files.some((path) => path.replace(/\.(xaml|axaml)$/i, "") === file.path.replace(/\.xaml\.cs$|\.cs$/i, "")));
    if (owner && !owner.navigatesTo.includes(target)) owner.navigatesTo.push(target);
  }
  const startup = /StartupUri\s*=\s*"([^"]+)"|MainWindow\s*=\s*new\s+(\w+)/.exec(file.text);
  if (startup) notes.push(`Windows-Startfenster: ${startup[1] ?? startup[2]} (${file.path}). Dieses Fenster ist der erste sichtbare Zustand.`);
}

export function windowsFactCandidates(paths: string[]): string[] {
  return paths.filter((path) => /\.(xaml|axaml)$/i.test(path) || /\.(cs|vb|fs)$/i.test(path));
}
