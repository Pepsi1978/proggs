import { describe, expect, it } from "vitest";
import { emptyFacts } from "./design-facts.js";
import { bedienelemente, funktionsSpec, neueBedienelemente, splitKommas, bauwegFor, bewegungenAusCss, uiSpec } from "./spec-package.js";
import type { ExportSection } from "./export-package.js";

const abschnitt = (html: string): ExportSection => ({ id: "B-01", name: "Start", slug: "start", isStart: true, html });

describe("Bedienelemente", () => {
  it("liest Ziel und beschriebene Aufgabe aus dem Markup", () => {
    const gefunden = bedienelemente(abschnitt(`
      <button data-werft-funktion="pausiert die laufende Sitzung">Pause</button>
      <button data-werft-navigate="B-02">Verlauf</button>
      <button data-werft-funktion="F-07">Teilen</button>
      <button>Namenlos</button>
    `));
    expect(gefunden.map((e) => e.label)).toEqual(["Pause", "Verlauf", "Teilen", "Namenlos"]);
    expect(gefunden[0]?.funktion).toBe("pausiert die laufende Sitzung");
    expect(gefunden[1]?.ziel).toBe("B-02");
  });

  it("haelt ein Element mit F-Kennung fuer bekannt, eines mit Beschreibung fuer neu", () => {
    const neu = neueBedienelemente([abschnitt(`
      <button data-werft-funktion="pausiert die laufende Sitzung">Pause</button>
      <button data-werft-funktion="F-07">Teilen</button>
      <button data-werft-navigate="B-02">Verlauf</button>
      <button>Namenlos</button>
    `)], { funktion: "### F-07 — Teilen\nTeilt die Sitzung." });
    expect(neu.map((e) => e.label)).toEqual(["Pause", "Namenlos"]);
  });
});

describe("Funktions-Spec", () => {
  it("uebernimmt die im Studio beschriebene Aufgabe, statt sie offen zu lassen", () => {
    const text = funktionsSpec({
      projectName: "Testapp", platform: "android", facts: emptyFacts("android"), variants: [],
      document: { head: "", css: "", sections: [abschnitt(`
        <button data-werft-funktion="pausiert die laufende Sitzung">Pause</button>
        <button>Namenlos</button>
      `)] },
      report: { bildschirmeImDesign: 1, bildschirmeExportiert: 1, erscheinungen: 1, dateienJeBildschirm: 1, nichtAufgebaut: [] },
      vorlage: { funktion: "# Funktions-Spec — Testapp\n\n### F-01 — Sitzung starten\nStartet." }
    }, bauwegFor("android"), "2026-08-09");
    expect(text).toContain("pausiert die laufende Sitzung");
    expect(text).toContain("F-02");
    expect(text).toContain("OFFEN — beim Rückimport klären");
    expect(text).toContain("Davon 1 mit beschriebener Aufgabe, 1 ohne.");
  });
});

describe("Bewegung", () => {
  it("zerreisst eine cubic-bezier-Kurve im transition nicht am Komma", () => {
    expect(splitKommas("transform 200ms cubic-bezier(0.4, 0, 0.2, 1), opacity 100ms linear")).toHaveLength(2);
    const bewegungen = bewegungenAusCss(".k { transition: transform 200ms cubic-bezier(0.4, 0, 0.2, 1); }");
    expect(bewegungen).toHaveLength(1);
    expect(bewegungen[0]?.dauerMs).toBe(200);
    expect(bewegungen[0]?.kurve.punkte).toEqual([0.4, 0, 0.2, 1]);
  });
});

describe("UI-Spec", () => {
  it("beschreibt jeden Bildschirm einzeln — Aufbau, Bedienelemente und ihre Bewegungen", () => {
    const css = ".pm-pause { animation: m-04-atmen 4000ms ease-in-out infinite; }";
    const section: ExportSection = {
      id: "B-01", name: "Start", slug: "start", isStart: true,
      html: `<section class="werft-screen" data-screen-id="B-01">
        <header class="pm-kopf">Perfect Moment</header>
        <button class="pm-pause" data-werft-funktion="pausiert die laufende Sitzung">Pause</button>
        <button data-werft-navigate="B-02">Verlauf</button>
      </section>`
    };
    const text = uiSpec({
      projectName: "Testapp", platform: "android", facts: emptyFacts("android"), variants: [],
      document: { head: "", css, sections: [section] },
      report: { bildschirmeImDesign: 1, bildschirmeExportiert: 1, erscheinungen: 1, dateienJeBildschirm: 1, nichtAufgebaut: [] }
    }, bauwegFor("android"), "2026-08-09", bewegungenAusCss(css));
    expect(text).toContain("### B-01 — Start");
    expect(text).toContain("pm-kopf");
    expect(text).toContain("pausiert die laufende Sitzung");
    expect(text).toContain("führt zu `B-02`");
    expect(text).toContain("Bewegungen auf diesem Bildschirm");
    // Die im @keyframes-Namen eingebrannte Kennung gewinnt ueber die Zaehlnummer.
    expect(text).toContain("M-04");
  });
});
