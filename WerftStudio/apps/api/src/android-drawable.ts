import { dimensionToPx, normalizeHexColor } from "./design-facts.js";
import { colorWithOpacity } from "./effect-catalog.js";
import { attribute, parseXml, type XmlNode } from "./xml-lite.js";

// Android-Drawables sind der Hauptgrund fuer falsche Icons und fehlende Button-Optik: ohne echte
// Umwandlung sieht die KI nur einen Dateinamen und erfindet ein Ersatzsymbol.

export type ResolvedDrawable = { kind: "vector" | "shape"; svg?: string; css?: string };

const strokeCaps: Record<string, string> = { butt: "butt", round: "round", square: "square" };
const strokeJoins: Record<string, string> = { miter: "miter", round: "round", bevel: "bevel" };

const colorOf = (raw: string | undefined, alpha?: string): string | undefined => {
  if (!raw || raw.startsWith("@") || raw.startsWith("?")) return undefined;
  const color = normalizeHexColor(raw);
  if (!color) return undefined;
  const opacity = alpha === undefined ? undefined : Number(alpha);
  return opacity !== undefined && Number.isFinite(opacity) && opacity < 1 ? colorWithOpacity(color, opacity) : color;
};

// <vector> nach SVG: Pfaddaten sind bereits SVG-kompatibel, nur Attributnamen und Gruppen-
// Transformationen unterscheiden sich.
export function vectorDrawableToSvg(source: string): string | undefined {
  const root = parseXml(source);
  if (!root || root.tag !== "vector") return undefined;
  const viewportWidth = Number(attribute(root, "viewportWidth") ?? "24");
  const viewportHeight = Number(attribute(root, "viewportHeight") ?? "24");
  const width = dimensionToPx(attribute(root, "width") ?? "24dp") ?? 24;
  const height = dimensionToPx(attribute(root, "height") ?? "24dp") ?? 24;
  const tint = colorOf(attribute(root, "tint"));
  const body = renderVectorChildren(root.children).trim();
  if (!body) return undefined;
  const alpha = Number(attribute(root, "alpha") ?? "1");
  const opacity = Number.isFinite(alpha) && alpha < 1 ? ` opacity="${alpha}"` : "";
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${Number.isFinite(viewportWidth) ? viewportWidth : 24} ${Number.isFinite(viewportHeight) ? viewportHeight : 24}"${tint ? ` fill="${tint}"` : ""}${opacity}>${body}</svg>`;
}

function renderVectorChildren(nodes: XmlNode[]): string {
  return nodes.map((node) => {
    if (node.tag === "path") return renderVectorPath(node);
    if (node.tag === "group") return `<g${groupTransform(node)}>${renderVectorChildren(node.children)}</g>`;
    if (node.tag === "clip-path") { const data = attribute(node, "pathData"); return data ? `<clipPath><path d="${data}"/></clipPath>` : ""; }
    return renderVectorChildren(node.children);
  }).join("");
}

function groupTransform(node: XmlNode): string {
  const pivotX = Number(attribute(node, "pivotX") ?? "0") || 0;
  const pivotY = Number(attribute(node, "pivotY") ?? "0") || 0;
  const rotation = Number(attribute(node, "rotation") ?? "0") || 0;
  const scaleX = Number(attribute(node, "scaleX") ?? "1");
  const scaleY = Number(attribute(node, "scaleY") ?? "1");
  const translateX = Number(attribute(node, "translateX") ?? "0") || 0;
  const translateY = Number(attribute(node, "translateY") ?? "0") || 0;
  const parts: string[] = [];
  if (translateX || translateY) parts.push(`translate(${translateX} ${translateY})`);
  // Android dreht und skaliert um den Pivot; in SVG muss das explizit ausgeschrieben werden.
  if (rotation || (scaleX !== 1 && Number.isFinite(scaleX)) || (scaleY !== 1 && Number.isFinite(scaleY))) {
    if (pivotX || pivotY) parts.push(`translate(${pivotX} ${pivotY})`);
    if (rotation) parts.push(`rotate(${rotation})`);
    if (Number.isFinite(scaleX) && Number.isFinite(scaleY) && (scaleX !== 1 || scaleY !== 1)) parts.push(`scale(${scaleX} ${scaleY})`);
    if (pivotX || pivotY) parts.push(`translate(${-pivotX} ${-pivotY})`);
  }
  return parts.length ? ` transform="${parts.join(" ")}"` : "";
}

function renderVectorPath(node: XmlNode): string {
  const data = attribute(node, "pathData");
  if (!data) return "";
  const fill = colorOf(attribute(node, "fillColor"), attribute(node, "fillAlpha"));
  const stroke = colorOf(attribute(node, "strokeColor"), attribute(node, "strokeAlpha"));
  const strokeWidth = Number(attribute(node, "strokeWidth") ?? "0");
  const fillType = attribute(node, "fillType")?.toLowerCase();
  const attributes = [
    `d="${data}"`,
    `fill="${fill ?? (stroke ? "none" : "currentColor")}"`,
    stroke ? `stroke="${stroke}"` : "",
    stroke && Number.isFinite(strokeWidth) && strokeWidth > 0 ? `stroke-width="${strokeWidth}"` : "",
    strokeCaps[attribute(node, "strokeLineCap")?.toLowerCase() ?? ""] ? `stroke-linecap="${strokeCaps[attribute(node, "strokeLineCap")!.toLowerCase()]}"` : "",
    strokeJoins[attribute(node, "strokeLineJoin")?.toLowerCase() ?? ""] ? `stroke-linejoin="${strokeJoins[attribute(node, "strokeLineJoin")!.toLowerCase()]}"` : "",
    fillType === "evenodd" ? 'fill-rule="evenodd"' : ""
  ].filter(Boolean);
  return `<path ${attributes.join(" ")}/>`;
}

// <shape>, <ripple>, <selector> und <layer-list> beschreiben genau die Button-Optik, die sonst
// "irgendwie anders" aussieht: Radien, Rahmen, Verlaeufe und Zustandsfarben.
export function shapeDrawableToCss(source: string): string | undefined {
  const root = parseXml(source);
  if (!root) return undefined;
  const shape = root.tag === "shape" ? root : findShape(root);
  const declarations: string[] = [];
  if (root.tag === "ripple") {
    const rippleColor = colorOf(attribute(root, "color"));
    if (rippleColor) declarations.push(`--ripple-color: ${rippleColor}`);
  }
  if (!shape) return declarations.length ? declarations.join("; ") : undefined;
  for (const child of shape.children) {
    if (child.tag === "solid") { const fill = colorOf(attribute(child, "color")); if (fill) declarations.push(`background-color: ${fill}`); }
    if (child.tag === "corners") declarations.push(...cornerCss(child));
    if (child.tag === "stroke") {
      const width = dimensionToPx(attribute(child, "width") ?? "");
      const strokeColor = colorOf(attribute(child, "color"));
      if (width !== undefined && strokeColor) declarations.push(`border: ${width}px ${attribute(child, "dashWidth") ? "dashed" : "solid"} ${strokeColor}`);
    }
    if (child.tag === "gradient") { const gradient = gradientCss(child); if (gradient) declarations.push(`background-image: ${gradient}`); }
    if (child.tag === "padding") {
      const sides = ["top", "right", "bottom", "left"].map((side) => dimensionToPx(attribute(child, side) ?? "") ?? 0);
      if (sides.some((value) => value !== 0)) declarations.push(`padding: ${sides[0]}px ${sides[1]}px ${sides[2]}px ${sides[3]}px`);
    }
    if (child.tag === "size") {
      const width = dimensionToPx(attribute(child, "width") ?? "");
      const height = dimensionToPx(attribute(child, "height") ?? "");
      if (width !== undefined) declarations.push(`width: ${width}px`);
      if (height !== undefined) declarations.push(`height: ${height}px`);
    }
  }
  if (attribute(shape, "shape")?.toLowerCase() === "oval") declarations.push("border-radius: 50%");
  return declarations.length ? declarations.join("; ") : undefined;
}

function findShape(node: XmlNode): XmlNode | undefined {
  if (node.tag === "shape") return node;
  for (const child of node.children) { const found = findShape(child); if (found) return found; }
  return undefined;
}

function cornerCss(node: XmlNode): string[] {
  const uniform = dimensionToPx(attribute(node, "radius") ?? "");
  if (uniform !== undefined) return [`border-radius: ${uniform}px`];
  const corners = ["topLeftRadius", "topRightRadius", "bottomRightRadius", "bottomLeftRadius"].map((name) => dimensionToPx(attribute(node, name) ?? "") ?? 0);
  return corners.some((value) => value !== 0) ? [`border-radius: ${corners.map((value) => `${value}px`).join(" ")}`] : [];
}

function gradientCss(node: XmlNode): string | undefined {
  const start = colorOf(attribute(node, "startColor"));
  const center = colorOf(attribute(node, "centerColor"));
  const end = colorOf(attribute(node, "endColor"));
  const stops = [start, center, end].filter((value): value is string => Boolean(value));
  if (stops.length < 2) return undefined;
  const type = attribute(node, "type")?.toLowerCase() ?? "linear";
  if (type === "radial") return `radial-gradient(circle, ${stops.join(", ")})`;
  if (type === "sweep") return `conic-gradient(${stops.join(", ")})`;
  // Android misst den Winkel gegen den Uhrzeigersinn ab „nach rechts"; CSS misst im Uhrzeigersinn ab „nach oben".
  const angle = Number(attribute(node, "angle") ?? "0") || 0;
  return `linear-gradient(${(450 - angle) % 360}deg, ${stops.join(", ")})`;
}

export function resolveDrawable(source: string): ResolvedDrawable | undefined {
  const svg = vectorDrawableToSvg(source);
  if (svg) return { kind: "vector", svg };
  const css = shapeDrawableToCss(source);
  return css ? { kind: "shape", css } : undefined;
}
