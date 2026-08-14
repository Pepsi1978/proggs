import { describe, expect, it } from "vitest";
import { effectLocation, extractWebFacts } from "./extract-web.js";

describe("effectLocation", () => {
  it("nennt den CSS-Selektor, in dem der Effekt steht", () => {
    const css = ".karte { box-shadow: 0 2px 8px #000; }";
    expect(effectLocation(css, css.indexOf("box-shadow"))).toBe(".karte");
  });

  it("fasst mehrteilige Selektoren zusammen", () => {
    const css = ".a { color: red; }\nnav.reiterleiste, footer .leiste { box-shadow: 0 1px 0 #333; }";
    expect(effectLocation(css, css.indexOf("box-shadow"))).toBe("nav.reiterleiste, footer .leiste");
  });

  it("nennt Tag und Klasse, wenn der Effekt direkt am Element steht", () => {
    const html = `<div class="karte gross" style="box-shadow: 0 2px 8px #000;">Inhalt</div>`;
    expect(effectLocation(html, html.indexOf("box-shadow"))).toBe("div.karte.gross");
  });

  it("bleibt leer, wenn sich kein Ort bestimmen lässt", () => {
    expect(effectLocation("box-shadow: 0 2px 8px #000;", 0)).toBe("");
  });
});

describe("extractWebFacts", () => {
  it("gibt Effekten einen Namen, aus dem hervorgeht, wohin sie gehören", () => {
    const facts = extractWebFacts([{ path: "design.html", text: `<style>.knopf { box-shadow: 0 6px 24px rgba(0,0,0,.28); } .kopf { background: linear-gradient(145deg, #111, #222); }</style>` }]);
    expect(facts.effects?.map((effect) => effect.name)).toEqual([".knopf (shadow)", ".kopf (gradient)"]);
  });

  it("fällt auf die laufende Nummer zurück, wenn kein Ort erkennbar ist", () => {
    const facts = extractWebFacts([{ path: "design.css", text: `box-shadow: 0 1px 2px #000;` }]);
    expect(facts.effects?.[0]?.name).toBe("design.css:shadow(0)");
  });
});
