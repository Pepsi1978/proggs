import { describe, expect, it } from "vitest";
import { designFileName, designVariantsOf, generatedDesignPathPattern, sameVariantPattern } from "./design-variants.js";

describe("Formatfassungen eines Designs", () => {
  it("gibt jeder Geraeteflaeche eine eigene Datei neben der Grundfassung", () => {
    expect(designFileName()).toBe("design.html");
    expect(designFileName({ width: 928, height: 1080 })).toBe("design-928x1080.html");
    // Hoch und quer sind verschiedene Formate und duerfen sich nicht ueberschreiben.
    expect(designFileName({ width: 1080, height: 928 })).not.toBe(designFileName({ width: 928, height: 1080 }));
  });

  it("raeumt beim Speichern NUR die vorherige Fassung desselben Formats weg", () => {
    const vorhanden = [
      ".werft-generated/019fa2f3-0000-0000-0000-000000000001/1/design.html",
      ".werft-generated/019fa2f3-0000-0000-0000-000000000002/1/design-928x1080.html",
      ".werft-generated/019fa2f3-0000-0000-0000-000000000003/2/design-1080x928.html",
      "index.html"
    ];
    const zuErsetzen = sameVariantPattern(designFileName({ width: 928, height: 1080 }));
    expect(vorhanden.filter((path) => zuErsetzen.test(path))).toEqual([".werft-generated/019fa2f3-0000-0000-0000-000000000002/1/design-928x1080.html"]);
    // Die Grundfassung und die anderen Formate bleiben unberuehrt — sonst waere nach jedem Aufbau
    // nur noch ein einziges Format vorhanden.
    const basis = sameVariantPattern(designFileName());
    expect(vorhanden.filter((path) => basis.test(path))).toEqual([".werft-generated/019fa2f3-0000-0000-0000-000000000001/1/design.html"]);
  });

  it("erkennt alle aufgebauten Formate im Dateiverzeichnis", () => {
    const variants = designVariantsOf([
      "index.html",
      ".werft-generated/019fa2f3-0000-0000-0000-000000000001/1/design.html",
      ".werft-generated/019fa2f3-0000-0000-0000-000000000002/1/design-928x1080.html",
      ".werft-generated/019fa2f3-0000-0000-0000-000000000003/3/design-384x824.html"
    ]);
    expect(variants).toEqual([
      { width: 928, height: 1080, path: ".werft-generated/019fa2f3-0000-0000-0000-000000000002/1/design-928x1080.html" },
      { width: 384, height: 824, path: ".werft-generated/019fa2f3-0000-0000-0000-000000000003/3/design-384x824.html" }
    ]);
    // Die Grundfassung ist keine Formatfassung; sie gilt fuer „Größe des Designs".
    expect(variants.some((variant) => variant.path.endsWith("design.html"))).toBe(false);
  });

  it("zaehlt Grundfassung und Formatfassungen beide als aufgebautes Design", () => {
    expect(generatedDesignPathPattern.test(".werft-generated/019fa2f3-0000-0000-0000-000000000001/1/design.html")).toBe(true);
    expect(generatedDesignPathPattern.test(".werft-generated/019fa2f3-0000-0000-0000-000000000001/1/design-928x1080.html")).toBe(true);
    expect(generatedDesignPathPattern.test("index.html")).toBe(false);
    expect(generatedDesignPathPattern.test(".werft-generated/019fa2f3-0000-0000-0000-000000000001/1/notizen.html")).toBe(false);
  });
});
