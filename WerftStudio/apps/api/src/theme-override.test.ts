import { describe, expect, it } from "vitest";
import { builtInVariant, colourSpellings, readRules, scopeSelector, themeOverrideCss } from "./theme-override.js";
import type { ThemeVariant } from "./screen-composer.js";

// Wortlaut aus PerfectMoment: das aufgebaute Design trägt die dunklen Farben fest im Stylesheet,
// die hellen stehen nur in den Projektquellen.
const dark: ThemeVariant = { id: "darkpmcolors", name: "DarkPmColors (Dunkel)", kind: "dark", color: "#181209", tokens: { background: "#181209", surface: "#251c10", text1: "#f5eee2" } };
const light: ThemeVariant = { id: "lightpmcolors", name: "LightPmColors", kind: "light", color: "#fbf6ec", tokens: { background: "#fbf6ec", surface: "#f3ead9", text1: "#241d12" } };

describe("Farbabbildung für fest aufgebaute Designs", () => {
  const html = `<!doctype html><html><head><style>
    .pm-screen { background: #181209; color: #f5eee2; padding: 16px; }
    .pm-card, .pm-sheet { background-color: rgb(37, 28, 16); border: 1px solid #181209; }
    .pm-static { padding: 8px; }
    @media (max-width: 480px) { .pm-card { background: #251C10; } }
  </style></head><body><section class="werft-screen"></section></body></html>`;

  it("erkennt die eingebaute Erscheinung und schaltet alle übrigen um", () => {
    expect(builtInVariant(html, [light, dark])?.id).toBe("darkpmcolors");
    const css = themeOverrideCss(html, [dark, light]);
    expect(css).toContain(':root[data-werft-theme="lightpmcolors"] .pm-screen');
    expect(css).toContain("background: #fbf6ec");
    expect(css).toContain("color: #241d12");
    // Für die eingebaute Erscheinung selbst braucht es keine Regel — sie steht schon im Dokument.
    expect(css).not.toContain('data-werft-theme="darkpmcolors"');
  });

  it("trifft dieselbe Farbe auch in der rgb-Schreibweise", () => {
    expect(colourSpellings("#251c10")).toContain("rgb(37, 28, 16)");
    const css = themeOverrideCss(html, [dark, light]);
    expect(css).toContain("background-color: #f3ead9");
  });

  it("behält Bedingungsblöcke und lässt unbeteiligte Regeln unangetastet", () => {
    const css = themeOverrideCss(html, [dark, light]);
    expect(css).toContain("@media (max-width: 480px)");
    expect(css).not.toContain("pm-static");
    expect(css).not.toContain("padding: 16px");
  });

  it("schränkt :root direkt ein statt es zum Nachfahren zu machen", () => {
    expect(scopeSelector(":root", "hell")).toBe(':root[data-werft-theme="hell"]');
    expect(scopeSelector("html.dunkel", "hell")).toBe('html[data-werft-theme="hell"].dunkel');
    expect(scopeSelector(".a, .b", "hell")).toBe(':root[data-werft-theme="hell"] .a, :root[data-werft-theme="hell"] .b');
  });

  it("bildet nichts ab, wenn die Farben des Designs gar nicht aus den Themes stammen", () => {
    const fremd = `<style>.x { background: #123456; }</style>`;
    expect(themeOverrideCss(fremd, [dark, light])).toBe("");
    expect(themeOverrideCss(html, [dark])).toBe("");
  });

  it("liest Regeln samt Deklarationen und überspringt @keyframes", () => {
    const rules = readRules(".a { color: red; } @keyframes puls { from { opacity: 0 } } @media print { .b { color: blue } }");
    expect(rules.map((rule) => rule.selector)).toEqual([".a", ".b"]);
    expect(rules[1]?.media).toBe("@media print");
    expect(rules[0]?.declarations[0]).toEqual({ property: "color", value: "red", important: false });
  });
});
