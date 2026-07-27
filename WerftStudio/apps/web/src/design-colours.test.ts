import { describe, expect, it } from "vitest";
import { groupedTokens, hexColour, tokenTitle } from "./design-colours.js";
import { referenceDevices, stageDeviceFor } from "./reference-devices.js";

describe("Farbregler", () => {
  // Gemessen wird am lebenden Dokument; von dort kommen fast immer rgb()-Werte. Wurden sie nicht
  // umgerechnet, stand in JEDEM Regler Schwarz — die Leiste wirkte dadurch wirkungslos.
  it("rechnet jede Schreibweise in den Wert um, den ein Farbwaehler braucht", () => {
    expect(hexColour("rgb(49, 87, 213)")).toBe("#3157d5");
    expect(hexColour("rgba(18, 22, 29, 0.85)")).toBe("#12161d");
    expect(hexColour("rgb(255 0 0)")).toBe("#ff0000");
    expect(hexColour("#ABCDEF")).toBe("#abcdef");
    expect(hexColour("#abc")).toBe("#aabbcc");
    expect(hexColour("#12345678")).toBe("#123456");
    expect(hexColour("transparent")).toBe("");
    expect(hexColour("")).toBe("");
  });

  it("ordnet jede Farbe genau einem Bereich zu und laesst keine liegen", () => {
    const groups = groupedTokens({
      "--color-error": "#9b1c1c", "--color-accent": "#3157d5", "--color-border": "#d8dee8",
      "--color-text-subtle": "#5d6978", "--color-bg": "#f5f7fa", "--zickzack": "#010203"
    });
    expect(groups.map((group) => group.title)).toEqual([
      "Zustände und Meldungen", "Akzent und Bedienelemente", "Linien, Rahmen und Schatten", "Schrift", "Flächen und Hintergründe", "Weitere Farben"
    ]);
    expect(groups.flatMap((group) => group.entries).length).toBe(6);
    // Jede Farbe steht in genau einem Bereich; doppelte oder verlorene Regler waeren beide falsch.
    const names = groups.flatMap((group) => group.entries.map(([name]) => name));
    expect(new Set(names).size).toBe(names.length);
  });

  it("macht aus Token-Namen lesbare Bezeichnungen", () => {
    expect(tokenTitle("--color-bg")).toBe("Hintergrund");
    expect(tokenTitle("--md-sys-color-on-surface")).toBe("Auf Fläche");
    expect(tokenTitle("--color-text-subtle")).toBe("Schrift Gedämpft");
    // Ohne erkennbaren Bestandteil bleibt der Name selbst stehen, statt leer zu sein.
    expect(tokenTitle("--zickzack")).toBe("Zickzack");
  });
});

describe("Referenzgeraete", () => {
  it("dreht das Geraet fuer das Querformat, statt eine zweite Liste zu fuehren", () => {
    const upright = stageDeviceFor("fold6-main", false);
    const turned = stageDeviceFor("fold6-main", true);
    expect(upright).toEqual({ id: "fold6-main", label: "Galaxy Z Fold 6 · aufgeklappt · hoch", width: 928, height: 1080 });
    expect(turned).toEqual({ id: "fold6-main", label: "Galaxy Z Fold 6 · aufgeklappt · quer", width: 1080, height: 928 });
  });

  it("kennt beide Zustaende der Foldables und liefert ohne Wahl kein Geraet", () => {
    expect(stageDeviceFor("", false)).toBeNull();
    expect(stageDeviceFor("gibtsnicht", false)).toBeNull();
    const ids = referenceDevices.map((device) => device.id);
    expect(ids).toContain("fold6-cover");
    expect(ids).toContain("fold6-main");
    expect(ids).toContain("fold8-cover");
    expect(ids).toContain("fold8-main");
    // Aufgeklappt muss deutlich mehr Flaeche haben als zugeklappt — sonst waere die Wahl sinnlos.
    for (const family of ["fold6", "fold8"]) {
      const cover = referenceDevices.find((device) => device.id === `${family}-cover`)!;
      const main = referenceDevices.find((device) => device.id === `${family}-main`)!;
      expect(main.width * main.height).toBeGreaterThan(cover.width * cover.height * 1.4);
    }
  });

  it("haelt jede Geraeteflaeche in einer Groessenordnung, die ein Handy wirklich hat", () => {
    for (const device of referenceDevices) {
      expect(device.width).toBeGreaterThanOrEqual(320);
      expect(device.width).toBeLessThanOrEqual(1100);
      expect(device.height).toBeGreaterThanOrEqual(600);
      expect(device.height).toBeLessThanOrEqual(1400);
      expect(device.source).toMatch(/^\d+ × \d+ px · \d,\d+×$/);
    }
  });
});
