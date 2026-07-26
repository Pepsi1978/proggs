// Effekte sind der haeufigste Grund fuer "sieht fast richtig aus": Elevation, Schatten, Blur und
// Ripple werden von jeder Plattform anders beschrieben. Hier wird jede Quellangabe in exakt eine
// CSS-Entsprechung uebersetzt, damit weder Aufbau noch Pruefung raten muessen.

// Offizielle Material-3-Schattenstufen (dp 0/1/3/6/8/12) in ihrer Web-Entsprechung.
const materialElevationShadows: Array<{ dp: number; css: string }> = [
  { dp: 0, css: "none" },
  { dp: 1, css: "0px 1px 2px 0px rgba(0, 0, 0, 0.3), 0px 1px 3px 1px rgba(0, 0, 0, 0.15)" },
  { dp: 3, css: "0px 1px 2px 0px rgba(0, 0, 0, 0.3), 0px 2px 6px 2px rgba(0, 0, 0, 0.15)" },
  { dp: 6, css: "0px 4px 8px 3px rgba(0, 0, 0, 0.15), 0px 1px 3px 0px rgba(0, 0, 0, 0.3)" },
  { dp: 8, css: "0px 6px 10px 4px rgba(0, 0, 0, 0.15), 0px 2px 3px 0px rgba(0, 0, 0, 0.3)" },
  { dp: 12, css: "0px 8px 12px 6px rgba(0, 0, 0, 0.15), 0px 4px 4px 0px rgba(0, 0, 0, 0.3)" }
];

export function elevationToShadow(dp: number): string {
  if (!Number.isFinite(dp) || dp <= 0) return "none";
  const exact = materialElevationShadows.find((level) => level.dp === dp);
  if (exact) return exact.css;
  // Zwischenwerte proportional aus der naechsthoeheren Stufe ableiten statt auf ein Level zu runden:
  // eine 4dp-Karte darf nicht wie eine 6dp-Karte aussehen.
  const upper = materialElevationShadows.find((level) => level.dp >= dp) ?? materialElevationShadows.at(-1)!;
  const lower = [...materialElevationShadows].reverse().find((level) => level.dp <= dp) ?? materialElevationShadows[0]!;
  if (upper.dp === lower.dp) return scaleShadow(upper.css, dp / (upper.dp || 1));
  const ratio = (dp - lower.dp) / (upper.dp - lower.dp);
  return lower.css === "none" ? scaleShadow(upper.css, Math.max(0.25, ratio)) : mixShadow(lower.css, upper.css, ratio);
}

const shadowNumbers = (css: string) => [...css.matchAll(/(-?\d+(?:\.\d+)?)px/g)].map((match) => Number(match[1]));

function scaleShadow(css: string, factor: number): string {
  let index = 0;
  return css.replace(/(-?\d+(?:\.\d+)?)px/g, (_all, value: string) => { index += 1; return `${round(Number(value) * factor)}px`; });
}

function mixShadow(lower: string, upper: string, ratio: number): string {
  const upperNumbers = shadowNumbers(upper);
  if (shadowNumbers(lower).length !== upperNumbers.length) return upper;
  let index = 0;
  return lower.replace(/(-?\d+(?:\.\d+)?)px/g, (_all, value: string) => `${round(Number(value) + (upperNumbers[index++]! - Number(value)) * ratio)}px`);
}

const round = (value: number) => Math.round(value * 100) / 100;

// WPF/WinUI: Direction 0° zeigt nach rechts und laeuft gegen den Uhrzeigersinn; der Bildschirm
// zaehlt y nach unten, deshalb wird die Y-Komponente gespiegelt.
export function wpfDropShadowToCss(options: { blurRadius?: number; shadowDepth?: number; direction?: number; opacity?: number; color?: string }): string {
  const depth = options.shadowDepth ?? 5;
  const direction = options.direction ?? 315;
  const blur = options.blurRadius ?? 5;
  const opacity = Math.max(0, Math.min(1, options.opacity ?? 1));
  const radians = (direction * Math.PI) / 180;
  const offsetX = round(depth * Math.cos(radians));
  const offsetY = round(-depth * Math.sin(radians));
  return `${offsetX}px ${offsetY}px ${round(blur)}px ${colorWithOpacity(options.color ?? "#000000", opacity)}`;
}

// SwiftUI/CoreAnimation geben einen Gauss-Radius an; CSS erwartet den doppelten Wert fuer
// dieselbe optische Weichheit.
export function swiftShadowToCss(options: { radius: number; x?: number; y?: number; color?: string; opacity?: number }): string {
  return `${round(options.x ?? 0)}px ${round(options.y ?? 0)}px ${round(options.radius * 2)}px ${colorWithOpacity(options.color ?? "#000000", options.opacity ?? 0.33)}`;
}

export function colorWithOpacity(color: string, opacity: number): string {
  if (opacity >= 0.999) return color;
  const hex = /^#([0-9a-f]{6})$/i.exec(color.trim());
  if (hex) {
    const value = hex[1]!;
    return `rgba(${Number.parseInt(value.slice(0, 2), 16)}, ${Number.parseInt(value.slice(2, 4), 16)}, ${Number.parseInt(value.slice(4, 6), 16)}, ${round(opacity)})`;
  }
  const rgba = /^rgba?\(([^)]+)\)$/i.exec(color.trim());
  if (rgba) {
    const parts = rgba[1]!.split(",").map((part) => part.trim());
    const existing = parts.length > 3 ? Number(parts[3]) : 1;
    return `rgba(${parts[0]}, ${parts[1]}, ${parts[2]}, ${round((Number.isFinite(existing) ? existing : 1) * opacity)})`;
  }
  return color;
}

// Material-Ripple als reine CSS/JS-freie Entsprechung: gleiche Deckkraft, gleiche Dauer.
export const rippleCss = (color: string, pressedOpacity = 0.12) => `background-color: ${colorWithOpacity(color, pressedOpacity)}; transition: background-color 100ms linear`;

export const stateLayerOpacity = { hovered: 0.08, focused: 0.1, pressed: 0.1, dragged: 0.16 } as const;

// Der Effekt-Leitfaden geht als feste Regel in jeden Aufbau-Prompt: ohne ihn erfindet die KI
// eigene Schattenwerte und die Rekonstruktion weicht sichtbar ab.
export const effectGuidance = [
  "EFFEKT-REGELN (verbindlich, keine eigenen Werte erfinden):",
  "- Android-Elevation/`shadow(Xdp)`/`cardElevation` NUR über die im Faktenblatt gelieferte box-shadow-Zeile abbilden.",
  "- WPF-`DropShadowEffect` und SwiftUI-`.shadow(...)` ebenso: exakt die gelieferte CSS-Zeile verwenden.",
  "- Klickbare Flächen bekommen die Original-Zustandsschichten: hover 8 %, focus 10 %, pressed 10 %, dragged 16 % der Vordergrundfarbe als overlay — nicht heller/dunkler „schätzen“.",
  "- Ripple/Highlight über ein ::after-Overlay mit der Originalfarbe, Dauer 100 ms linear.",
  "- Blur (`RenderEffect`, `.blur`, `BlurEffect`, `backdrop-filter`) mit dem gelieferten Radius als `filter`/`backdrop-filter`, nicht als halbtransparente Fläche ersetzen.",
  "- Verläufe exakt mit den gelieferten Stopps und Winkeln; Strichstärken als `border`/`outline` in exakter Breite und Farbe.",
  "- Deckkraft (`alpha`, `Opacity`, `.opacity`) als `opacity` übernehmen, nicht in die Farbe einrechnen, außer die Quelle tut es selbst.",
  "- ANIMATIONEN sind Teil des Designs, kein Beiwerk: jede im Faktenblatt als (animation) gelieferte Zeile MUSS am passenden Element als CSS-Animation/Transition erscheinen — mit genau dieser Dauer und Kurve. Eine Dauerschleife braucht zusätzlich ihre `@keyframes werft-loop`-Definition (z. B. pulsierender Ring: Skalierung 1 → 1.12 und Deckkraft 0.6 → 0).",
  "- Zustände müssen SICHTBAR reagieren: Aufnahme-/Wiedergabeknöpfe, Schalter, Auswahl, Ausklapper und Blätter (Bottom Sheets) schalten im HTML wirklich um — mit `:hover`, `:active`, `:focus-visible`, `:checked` oder einem kurzen Inline-Skript direkt in diesem Bildschirm. Ein Bedienelement, das nichts tut, ist ein Fehler."
].join("\n");
