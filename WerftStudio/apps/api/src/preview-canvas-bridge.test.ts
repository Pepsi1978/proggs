import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { injectPreviewCanvasBridge, previewBridgeVersion, rewriteRootRelativeAssets, rewriteRootRelativeCss, rewriteRootRelativeJavaScript } from "./preview-canvas-bridge.js";

// Die Vorschau wird mit ETag ausgeliefert. Haengt dessen Kennung nicht am INHALT der Bruecke, liefert
// der Server auf eine unveraenderte Revision ein 304 — und der Browser zeigt weiter die alte Bruecke
// aus seinem Zwischenspeicher. Genau so blieb eine reparierte Pipette beim Benutzer wirkungslos
// (27.07.2026). Diese Pruefung schliesst den Rueckfall aus.
describe("Kennung der Vorschau-Bruecke", () => {
  it("aendert sich mit dem Inhalt der Bruecke", () => {
    expect(previewBridgeVersion).toMatch(/^[A-Za-z0-9_-]{12}$/);
  });

  it("steht im ETag der Vorschau — und nicht als handgepflegte Zahl", () => {
    const server = readFileSync(new URL("./server.ts", import.meta.url), "utf8");
    expect(server).toContain("${previewBridgeVersion}");
    expect(server).not.toMatch(/canvas-bridge-v\d/);
  });
});

describe("preview canvas bridge", () => {
  it("injects the bridge before the closing body", () => {
    const result = injectPreviewCanvasBridge("<!doctype html><body><main>Design</main></body>");
    expect(result).toContain("data-werft-canvas-bridge");
    expect(result).not.toContain("document.body.style.transform");
    expect(result).not.toContain("overflow = \"hidden\"");
    expect(result.indexOf("data-werft-canvas-bridge")).toBeLessThan(result.indexOf("</body>"));
  });

  it("meldet den Radlauf mit Strg-Zustand und meldet Bildschirme, Größe und Klickziele", () => {
    const result = injectPreviewCanvasBridge("<!doctype html><body><section class=\"werft-screen\"></section></body>");
    // Das Rad allein scrollt, Strg vergrößert: die Leinwand entscheidet anhand des mitgesendeten
    // Strg-Zustands. Ohne ihn wäre jede Radbewegung wieder ein Zoom.
    expect(result).toContain("ctrlKey: Boolean(event.ctrlKey)");
    expect(result).toContain("deltaX: event.deltaX || 0");
    expect(result).toContain("scrollableUnder");
    expect(result).toContain('action: "screens"');
    expect(result).toContain('post({ action: "size"');
    expect(result).toContain('action: "navigate"');
    expect(result).toContain("werftFrame");
    expect(result).toContain("werftScreen");
    expect(result).toContain("data-werft-highlight");
  });

  it("liefert syntaktisch gültiges JavaScript aus", () => {
    // Ein Tippfehler im eingebetteten Skript würde JEDE Vorschau lahmlegen und wäre in einem
    // reinen Textvergleich unsichtbar. `new Function` parst den Code, ohne ihn auszuführen.
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(injectPreviewCanvasBridge("<body></body>"))?.[1];
    expect(code?.length).toBeGreaterThan(500);
    expect(() => new Function(code!)).not.toThrow();
  });

  it("does not inject the bridge twice", () => {
    const once = injectPreviewCanvasBridge("<main>Design</main>");
    expect(injectPreviewCanvasBridge(once)).toBe(once);
  });

  it("keeps root-relative assets inside the signed project preview", () => {
    const result = rewriteRootRelativeAssets('<link href="/assets/app.css"><img src="/images/a.png" srcset="/a.png 1x, /b.png 2x"><style>.x{background:url(\'/bg.png\')}</style><script>const src="/api"; import("/assets/lazy.js")</script>', "/api/v1/previews/project/token");
    expect(result).toContain('href="/api/v1/previews/project/token/assets/app.css"');
    expect(result).toContain('src="/api/v1/previews/project/token/images/a.png"');
    expect(result).toContain("url('/api/v1/previews/project/token/bg.png')");
    expect(result).toContain('srcset="/api/v1/previews/project/token/a.png 1x, /api/v1/previews/project/token/b.png 2x"');
    expect(result).toContain('const src="/api"');
    expect(result).toContain('import("/api/v1/previews/project/token/assets/lazy.js")');
  });

  it("rewrites root-relative resources in separately served CSS and ES modules", () => {
    expect(rewriteRootRelativeCss(".hero{background:url(/images/hero.png)}", "/preview/root")).toContain("url(/preview/root/images/hero.png)");
    expect(rewriteRootRelativeJavaScript('import x from "/chunks/x.js"; export { y } from "/chunks/y.js";', "/preview/root")).toBe('import x from "/preview/root/chunks/x.js"; export { y } from "/preview/root/chunks/y.js";');
  });

  // Die Bruecke ist ein String — kein Compiler prueft sie. Ein Syntaxfehler darin wuerde JEDE
  // Vorschau lahmlegen, ohne dass Typecheck oder Build etwas melden.
  it("keeps the embedded bridge script parsable and offers marking mode", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(code, "Bruecken-Skript nicht gefunden").toBeTruthy();
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain('action: "mark-target"');
    expect(code).toContain("selectorFor");
    expect(code).toContain("markMode");
  });

  // Gemeldet an PerfectMoment: der Hell-Dunkel-Modus liess sich in der Vorschau nicht umschalten.
  // Der Aufbau erzeugt das Dunkel-Theme als :root[data-theme=dark] — gesetzt hat es nie jemand.
  it("can switch the design theme and reports whether a dark one exists", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain("hasDarkTheme");
    expect(code).toContain('data-theme\", data.theme');
    expect(code).toContain('prefers-color-scheme: dark');
  });

  // Die Doku von Claude Design nennt Live-Regler fuer Farbe und Layout. Sie arbeiten hier mit den
  // Token, die der Aufbau AUS DEN PROJEKTQUELLEN erzeugt — nicht mit erfundenen Reglern.
  it("reports the design own colour tokens and can override them live", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain("designTokens");
    expect(code).toContain("applyTune");
    expect(code).toContain("data-werft-tune");
  });

  // Text direkt im Design aendern: alter und neuer Wortlaut stehen exakt fest, die Aenderung
  // braucht keine KI. Bearbeitet wird der Textknoten UNTER DEM ZEIGER — frueher blieb Text in
  // Elementen mit Kindknoten („Sitzung beginnen", „Dauer") unerreichbar.
  it("can edit any visible text in place and reports the exact wording", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain('action: "text-edit"');
    expect(code).toContain("editableTextNode");
    expect(code).toContain("caretPositionFromPoint");
    expect(code).toContain("contenteditable");
    // Die Schreibmarke muss sich vom Hintergrund abheben, sonst tippt man blind.
    expect(code).toContain("caret-color");
    expect(code).toContain("backgroundLuminance");
    // Im Stiftmodus darf kein Schalter mehr kippen, nur weil man seinen Text treffen wollte.
    expect(code).toContain("blockWhileEditing");
    expect(code).toContain('"pointerdown", "mousedown"');
  });

  // Gemeldet an PerfectMoment und OpenLauncher: es liess sich nur EINE Erscheinung sehen. Alle
  // Themes eines Projekts muessen waehlbar sein — und die Wahl gilt fuer ALLE Bildschirme.
  it("kennt alle Erscheinungen des Designs und schaltet dokumentweit um", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain("declaredThemes");
    expect(code).toContain("derivedThemes");
    expect(code).toContain('meta[name="werft-themes"]');
    expect(code).toContain("data-werft-theme");
    expect(code).toContain("applyThemeCss");
    expect(code).toContain('action: "theme-changed"');
  });

  // Ein gewaehltes Referenzgeraet gibt die Flaeche vor; das Design passt sich an, nicht umgekehrt.
  it("zwingt das Dokument auf die Flaeche des gewaehlten Referenzgeraets", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain("applyViewport");
    expect(code).toContain("data-werft-viewport");
    expect(code).toContain('data.action === "viewport"');
    // Gemessen wird der BILDSCHIRM. Wer das Fenster mitmisst, kann nie wieder schmaler werden: das
    // Fenster ist immer so breit wie der Rahmen, den das Studio gerade aufspannt — nach einem
    // aufgeklappten Geraet bliebe die Buehne fuer immer in dessen Breite stehen.
    expect(code).toContain("Math.max(rect.width, screen.scrollWidth || 0)");
    // Die Fensterbreite darf NUR noch als Rueckfall zaehlen, wenn es gar keinen Bildschirm gibt.
    const screenBranch = code!.indexOf("Math.max(rect.width, screen.scrollWidth");
    const windowFallback = code!.indexOf("document.documentElement.scrollWidth");
    expect(screenBranch).toBeGreaterThan(0);
    expect(windowFallback).toBeGreaterThan(screenBranch);
  });

  // Ein Klick ins Design muss sagen, WELCHE Farbe dort gilt und welcher Regler sie aendert.
  it("nimmt Farben aus dem Design auf und belegt die Zuordnung zum Regler", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain('action: "colour-pick"');
    expect(code).toContain("pickMode");
    expect(code).toContain("paintedBackground");
    // Die Zuordnung muss belegt sein: nur wenn die Regel wirklich var(--x) nennt, aendert der
    // Regler diese Stelle. Blosse Farbgleichheit fuehrt sonst auf einen wirkungslosen Regler.
    expect(code).toContain("declaredVariable");
    expect(code).toContain("inheritedVariable");
    // Dass eine Regel eine Variable nennt, genuegt nicht: eine spezifischere Regel kann sie mit
    // einem festen Wert uebersteuern. Nur eine Variable, die die Farbe nachweislich bewegt, darf
    // als Regler dieser Stelle gelten — sonst zeigt die Pipette auf einen wirkungslosen Regler.
    expect(code).toContain("variableAffects");
    expect(code).toContain("exact: Boolean(wirksam)");
    // Kurzform und Langform derselben Eigenschaft muessen beide erkannt werden.
    expect(code).toContain('["background-color", "background"]');
  });

  // Jede Erscheinung hat eigene Farbwerte. Frueher wurden nur die Bloecke ohne Erscheinungs-
  // bedingung gelesen: im Dunkelmodus zeigte und aenderte der Regler die Farben des hellen.
  it("liest die Farbwerte der GERADE geltenden Erscheinung", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain("tokenNames");
    expect(code).toContain("getComputedStyle(document.documentElement)");
    expect(code).toContain('post({ action: "tokens", tokens: designTokens()');
    // Zum Messen wird der eigene Regler-Block stillgelegt, sonst waere der Ausgangswert nach der
    // ersten Aenderung verloren und „zuruecksetzen" haette kein Ziel mehr.
    expect(code).toContain("tuneStyle.disabled = true");
  });

  // Ein Mond- oder Sonnensymbol IM Design soll auch wirklich umschalten.
  it("verdrahtet einen Umschalter im Design selbst", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain("looksLikeThemeToggle");
    expect(code).toContain("data-werft-theme-toggle");
    // Reagiert das Design selbst, bleibt es dabei — sonst kaeme der Wechsel doppelt.
    expect(code).toContain("if (currentThemeId() === before) applyTheme(nextThemeId())");
  });
});
