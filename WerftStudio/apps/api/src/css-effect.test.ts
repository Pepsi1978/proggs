import { describe, expect, it } from "vitest";
import { cascadeTraps, changedDeclarations, designScale, parseCss, parseDom, selectorMatches, specificity, summariseEffect } from "./css-effect.js";

// Der gemeldete Vorfall in klein: eine Klassenregel wird geändert, aber ein bildschirmweiter
// Element-Reset gewinnt die Kaskade. Der Lauf meldete „30 Änderungen übernommen" — sichtbar war nichts.
const design = (padding: string) => `<!doctype html><html><head><style>
.pm-start-screen button { margin: 0; padding: 0; }
.pm-start__hook { display: flex; padding: ${padding}; border-radius: 20px; }
.pm-settings-about { padding: 18px 16px; }
</style></head><body><div class="werft-screens">
<section class="werft-screen" data-screen-id="s1" data-screen-name="Start"><div class="pm-start-screen"><button class="pm-start__hook">A</button><button class="pm-start__hook">B</button></div></section>
<section class="werft-screen" data-screen-id="s2" data-screen-name="Einstellungen"><div class="pm-settings-about">C</div></section>
</div></body></html>`;

describe("CSS lesen und gewichten", () => {
  it("liest Regeln, Deklarationen und !important", () => {
    const rules = parseCss(".a { color: red; padding: 0 !important } @media print { .b { color: blue } }");
    expect(rules[0]).toMatchObject({ selectors: [".a"], media: "" });
    expect(rules[0]!.declarations).toEqual([
      { property: "color", value: "red", important: false },
      { property: "padding", value: "0", important: true }
    ]);
    expect(rules[1]).toMatchObject({ selectors: [".b"], media: "@media print" });
  });

  it("berechnet die Spezifität wie ein Browser", () => {
    expect(specificity(".pm-start__hook")).toEqual([0, 1, 0]);
    expect(specificity(".pm-start-screen button")).toEqual([0, 1, 1]);
    expect(specificity("#id .a b")).toEqual([1, 1, 1]);
    expect(specificity('a[data-x="1"]')).toEqual([0, 1, 1]);
  });

  it("liest den Elementbaum samt Bildschirmzuordnung", () => {
    const nodes = parseDom(design("16px"));
    const knopf = nodes.find((node) => node.classes.has("pm-start__hook"));
    expect(knopf).toMatchObject({ tag: "button", screen: "Start" });
    expect(nodes.find((node) => node.classes.has("pm-settings-about"))?.screen).toBe("Einstellungen");
  });

  it("trifft Selektoren über Nachfahren und Kind hinweg", () => {
    const knopf = parseDom(design("16px")).find((node) => node.classes.has("pm-start__hook"))!;
    expect(selectorMatches(knopf, ".pm-start__hook").matched).toBe(true);
    expect(selectorMatches(knopf, ".pm-start-screen button").matched).toBe(true);
    expect(selectorMatches(knopf, ".pm-start-screen > button").matched).toBe(true);
    expect(selectorMatches(knopf, ".pm-settings-screen button").matched).toBe(false);
    // Geschwister-Kombinatoren werden als unsicher gemeldet statt geraten.
    expect(selectorMatches(knopf, ".x + button").certain).toBe(false);
  });
});

describe("Wirkung einer Änderung", () => {
  it("erkennt eine Änderung, die die Kaskade verliert", () => {
    const summary = summariseEffect(design("16px"), design("18px"));
    const treffer = summary.findings.find((finding) => finding.selector === ".pm-start__hook");
    expect(treffer).toMatchObject({ status: "ueberschrieben", property: "padding", beatenBy: ".pm-start-screen button", beatenValue: "0" });
    expect(summary.tot).toBe(1);
    expect(summary.briefing).toContain("bleibt WIRKUNGSLOS");
    expect(summary.briefing).toContain(".pm-start-screen button");
  });

  it("erkennt eine Änderung, die wirklich ankommt, samt Bildschirm", () => {
    const vorher = design("16px");
    const nachher = vorher.replace(".pm-settings-about { padding: 18px 16px; }", ".pm-settings-about { padding: 22px 20px; }");
    const summary = summariseEffect(vorher, nachher);
    expect(summary.findings[0]).toMatchObject({ selector: ".pm-settings-about", status: "wirksam" });
    expect(summary.screens).toEqual(["Einstellungen"]);
    expect(summary.briefing).toBeUndefined();
  });

  it("meldet einen Selektor, den es im Design gar nicht gibt", () => {
    const vorher = design("16px");
    const nachher = vorher.replace("</style>", ".gibt-es-nicht { padding: 40px; }</style>");
    expect(summariseEffect(vorher, nachher).findings.at(-1)).toMatchObject({ selector: ".gibt-es-nicht", status: "kein-element", elements: 0 });
  });

  it("hält eine Regel für wirksam, wenn nur eine Sonderausprägung sie schlägt", () => {
    // Sonst wäre jede Regel mit einer Varianten-Zusatzklasse fälschlich „tot" — ein Fehlalarm, der
    // das Modell auf eine falsche Fährte schickt.
    const vorher = '<style>.zeile { height: 60px } .zeile.gross { height: 80px }</style><div class="zeile">a</div><div class="zeile gross">b</div>';
    const nachher = vorher.replace(".zeile { height: 60px }", ".zeile { height: 64px }");
    expect(summariseEffect(vorher, nachher).findings[0]).toMatchObject({ selector: ".zeile", status: "wirksam" });
  });

  it("wertet !important als Sieger, auch bei schwächerem Selektor", () => {
    const vorher = '<style>.a { padding: 10px } div { padding: 0 !important }</style><div class="a">x</div>';
    const nachher = vorher.replace(".a { padding: 10px }", ".a { padding: 20px }");
    expect(summariseEffect(vorher, nachher).findings[0]).toMatchObject({ status: "ueberschrieben", beatenValue: "0 !important" });
  });

  it("meldet nichts, wenn sich nur die Einrückung ändert", () => {
    const vorher = "<style>.a {\n  padding: 10px;\n}</style><div class=\"a\">x</div>";
    const nachher = "<style>.a {\n    padding:   10px;\n}</style><div class=\"a\">x</div>";
    expect(changedDeclarations(vorher, nachher)).toEqual([]);
  });
});

describe("Design verstehen, bevor gearbeitet wird", () => {
  it("nennt die Kaskadenfallen des Designs", () => {
    const traps = cascadeTraps(design("16px"));
    expect(traps.some((trap) => trap.startsWith(".pm-start-screen button"))).toBe(true);
    expect(traps.join(" ")).toContain("padding: 0");
  });

  it("misst die tatsächlich benutzte Formensprache", () => {
    const scale = designScale(design("16px"));
    expect(scale.abstaende).toContain("16px");
    expect(scale.radien).toEqual(["20px"]);
  });
});
