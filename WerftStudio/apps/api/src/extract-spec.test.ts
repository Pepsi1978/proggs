import { describe, expect, it } from "vitest";
import { extractSpecFacts } from "./extract-spec.js";

const ui = `# UI-Spec — Testapp
Stand: 2026-08-09 · Stufe: v1 · Plattform(en): Android

## 2. Erscheinungen (Themes)

### Dunkelmodus — \`dunkel\` (dark)

| Rolle | Wert (Hex/RGBA) | Verwendung |
|-------|-----------------|------------|
| Hintergrund | \`#0A0806\` | Vollflächig |
| Gold primär | \`#D4A24C\` | Knöpfe |

### Hellmodus — \`hell\` (light)

| Rolle | Wert (Hex/RGBA) | Verwendung |
|-------|-----------------|------------|
| Hintergrund | \`#FBF6EC\` | Vollflächig |
| Gold primär | \`#A87A2A\` | Knöpfe |

## 3. Typografie

| Rolle | Größe | Gewicht | Zeilenhöhe | Laufweite |
|-------|-------|---------|------------|-----------|
| Titel | 28 px | 600 | 34 px | -0.2 px |

## 4. Maße und Raster

| Name | px | Original |
|------|----|----------|
| Kartenabstand | 16 | 16dp |

## 5. Formen und Tiefe

| Name | Radius |
|------|--------|
| Karte | 20px |

## 6. Bildschirme

| Kennung | Bildschirm | Zweck | Startbildschirm? | führt zu |
|---------|------------|-------|------------------|----------|
| B-01 | Start | Sitzung beginnen | ja | B-02, B-03 |
| B-02 | Verlauf | Vergangene Sitzungen | — | B-01 |
| B-03 | Einstellungen | Anpassen | — | B-01 |

## 8. Texte

| Name | Text |
|------|------|
| start_knopf | "Sitzung starten" |
`;

const motion = `# Motion-Spec — Testapp

## 2. Kurven und Dauern

| Kennung | Bewegung | Dauer | Kurve | Wiederholung |
|---------|----------|-------|-------|--------------|
| M-01 | Atmen des Startknopfs | 4000 ms | \`cubic-bezier(0.4, 0, 0.2, 1)\` | endlos |
| M-02 | Bildschirmwechsel | 400 ms | \`ease-out\` | einmal |
`;

describe("Spec-Extraktor", () => {
  it("liest Bildschirme, Erscheinungen und Bewegungen aus dem Spec", () => {
    const facts = extractSpecFacts([{ path: "02-UI-SPEC.md", text: ui }, { path: "03-MOTION-SPEC.md", text: motion }]);
    expect(facts.screens?.map((s) => s.id)).toEqual(["B-01", "B-02", "B-03"]);
    expect(facts.screens?.[0]).toMatchObject({ name: "Start", isStart: true, navigatesTo: ["B-02", "B-03"] });
    expect(facts.themes?.map((t) => t.id)).toEqual(["dunkel", "hell"]);
    expect(facts.themes?.[0]?.tokens).toMatchObject({ Hintergrund: "#0A0806" });
    expect(facts.typography?.[0]).toMatchObject({ name: "Titel", sizePx: 28, weight: 600 });
    expect(facts.dimensions?.[0]).toMatchObject({ px: 16 });
    expect(facts.shapes?.[0]).toMatchObject({ radiusCss: "20px" });
    expect(facts.strings?.[0]).toMatchObject({ name: "start_knopf", value: "Sitzung starten" });
    const animation = facts.effects?.filter((e) => e.kind === "animation") ?? [];
    expect(animation).toHaveLength(2);
    expect(animation[0]?.css).toContain("infinite");
    expect(animation[0]?.name).toBe("M-01 Atmen des Startknopfs");
  });
});
