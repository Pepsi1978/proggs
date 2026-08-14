import { describe, expect, it } from "vitest";
import { checkShellConsistency, renderShellInstructions, styleIndex, summarizeShell } from "./shell-consistency.js";

const leiste = (hoehe: string, schrift: string, symbol: string, aktiv: string) => `
<div class="inhalt">Inhalt</div>
<nav class="reiterleiste" style="height: ${hoehe}; padding: 6px 12px; background: #14161c; border-radius: 0;">
  <button class="reiter" style="font-size: ${schrift};"${aktiv === "heute" ? ' aria-current="true"' : ""} data-werft-navigate="B-01"><svg width="${symbol}" height="${symbol}"></svg>Heute</button>
  <button class="reiter" style="font-size: ${schrift};"${aktiv === "gespraech" ? ' aria-current="true"' : ""} data-werft-navigate="B-02"><svg width="${symbol}" height="${symbol}"></svg>Gespräch</button>
  <button class="reiter" style="font-size: ${schrift};"${aktiv === "logbuch" ? ' aria-current="true"' : ""} data-werft-navigate="B-07"><svg width="${symbol}" height="${symbol}"></svg>Logbuch</button>
</nav>`;

describe("summarizeShell", () => {
  it("liest Höhe, Beschriftungen, Schrift- und Symbolgrößen aus der Leiste", () => {
    const shell = summarizeShell(leiste("64px", "12px", "24", "heute"), "");
    expect(shell?.entries).toEqual(["Heute", "Gespräch", "Logbuch"]);
    expect(shell?.height).toBe("64px");
    expect(shell?.padding).toBe("6px 12px");
    expect(shell?.background).toBe("#14161c");
    expect(shell?.fontSizes).toEqual(["12px", "12px", "12px"]);
    expect(shell?.iconSizes).toEqual(["24×24", "24×24", "24×24"]);
  });

  it("findet die Leiste auch, wenn ihre Maße im gemeinsamen Stylesheet stehen", () => {
    const markup = `<nav class="tabs"><button class="tab">Eins</button><button class="tab">Zwei</button></nav>`;
    const shell = summarizeShell(markup, ".tabs { height: 56px; background: #fff; } .tab { font-size: 11px; }");
    expect(shell?.height).toBe("56px");
    expect(shell?.fontSizes).toEqual(["11px", "11px"]);
  });

  it("meldet nichts, wo es keine Leiste gibt", () => {
    expect(summarizeShell("<div class='karte'>Nur Inhalt</div>", "")).toBeUndefined();
  });

  it("nimmt die größte Leiste, nicht die erstbeste Kopfzeile", () => {
    const markup = `<header class="appbar"><button>Zurück</button><button>Menü</button></header>${leiste("64px", "12px", "24", "heute")}`;
    expect(summarizeShell(markup, "")?.entries).toEqual(["Heute", "Gespräch", "Logbuch"]);
  });
});

describe("checkShellConsistency", () => {
  const screens = (zweite: string, dritte: string) => [
    { id: "B-01", name: "Heute", markup: leiste("64px", "12px", "24", "heute"), isStart: true },
    { id: "B-02", name: "Gespräch", markup: zweite },
    { id: "B-07", name: "Logbuch", markup: dritte }
  ];

  it("schweigt, wenn alle Bildschirme dieselbe Leiste tragen", () => {
    expect(checkShellConsistency(screens(leiste("64px", "12px", "24", "gespraech"), leiste("64px", "12px", "24", "logbuch")), "")).toEqual([]);
  });

  it("meldet abweichende Höhe, Schrift- und Symbolgröße", () => {
    const issues = checkShellConsistency(screens(leiste("64px", "17px", "24", "gespraech"), leiste("48px", "12px", "30", "logbuch")), "");
    const aspekte = issues.map((issue) => `${issue.screenId}:${issue.aspect}`);
    expect(aspekte).toContain("B-02:Schriftgrößen der Einträge");
    expect(aspekte).toContain("B-07:Höhe der Leiste");
    expect(aspekte).toContain("B-07:Symbolgrößen der Einträge");
    expect(issues.every((issue) => issue.screenId !== "B-01")).toBe(true);
  });

  it("meldet eine ganz fehlende Leiste", () => {
    const issues = checkShellConsistency(screens(leiste("64px", "12px", "24", "gespraech"), "<div class='karte'>Nur Inhalt</div>"), "");
    expect(issues.map((issue) => issue.aspect)).toContain("Wiederkehrende Leiste fehlt");
  });

  it("meldet abweichende Anzahl der Einträge", () => {
    const kurz = `<nav class="reiterleiste" style="height: 64px; padding: 6px 12px; background: #14161c; border-radius: 0;"><button class="reiter" style="font-size: 12px;">Heute</button><button class="reiter" style="font-size: 12px;">Gespräch</button></nav>`;
    const issues = checkShellConsistency(screens(kurz, leiste("64px", "12px", "24", "logbuch")), "");
    expect(issues.find((issue) => issue.aspect === "Anzahl der Einträge")).toMatchObject({ screenId: "B-02", expected: "3", actual: "2" });
  });

  it("schlägt keinen Alarm, wenn nur ein einzelner Bildschirm eine Leiste führt", () => {
    const ohne = "<div class='karte'>Nur Inhalt</div>";
    const liste = [
      { id: "B-01", name: "Heute", markup: leiste("64px", "12px", "24", "heute"), isStart: true },
      { id: "B-02", name: "Gespräch", markup: ohne },
      { id: "B-07", name: "Logbuch", markup: ohne }
    ];
    expect(checkShellConsistency(liste, "")).toEqual([]);
  });

  it("wertet weniger als zwei Bildschirme gar nicht aus", () => {
    expect(checkShellConsistency([{ id: "B-01", name: "Heute", markup: leiste("64px", "12px", "24", "heute"), isStart: true }], "")).toEqual([]);
  });
});

describe("styleIndex", () => {
  it("führt mehrere Regeln derselben Klasse zusammen und überspringt @-Regeln", () => {
    const index = styleIndex("@media print { .tab { color: red; } } .tab { height: 20px; } .tab { font-size: 9px; }");
    expect(index.get("tab")).toMatchObject({ height: "20px", "font-size": "9px" });
  });
});

describe("renderShellInstructions", () => {
  it("formuliert die Abweichungen als Auftrag mit Referenzbildschirm", () => {
    const text = renderShellInstructions([{ screenId: "B-07", screenName: "Logbuch", aspect: "Höhe der Leiste", expected: "64px", actual: "48px" }], "Heute");
    expect(text).toContain("ZEICHENGLEICH");
    expect(text).toContain("Heute");
    expect(text).toContain("Höhe der Leiste: verbindlich ist „64px“, dieser Bildschirm hat „48px“.");
  });

  it("bleibt still, wenn nichts abweicht", () => {
    expect(renderShellInstructions([], "Heute")).toBe("");
  });
});
