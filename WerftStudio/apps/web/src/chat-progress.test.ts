import { describe, expect, it } from "vitest";
import { chatStepText, designFileLabel } from "./chat-progress";

describe("Fortschritt im Gespräch", () => {
  it("sagt zuerst, was verstanden wurde", () => {
    expect(chatStepText("start", { scope: "global", marked: false })).toBe("Auftrag verstanden — gesamtes Design, alle Bildschirme.");
    expect(chatStepText("start", { scope: "screen", screen: "Einstellungen" })).toContain("Bildschirm „Einstellungen“");
    expect(chatStepText("start", { scope: "marked", marked: true })).toContain("markierter Bereich");
  });

  it("zeigt beim Warten, woran gearbeitet wird", () => {
    expect(chatStepText("denkt", { round: 1, files: 12, screens: 18 })).toBe("Das Modell liest 12 Dateien, 18 Bildschirme und plant die Änderungen …");
    expect(chatStepText("denkt", { round: 2, files: 1, screens: 0, excerpt: true })).toBe("Runde 2: Das Modell liest 1 Datei, fürs Kontextfenster gekürzt und plant die Änderungen …");
  });

  it("meldet das Ergebnis jeder Runde", () => {
    expect(chatStepText("runde", { round: 1, editsApplied: 17, editsFailed: 0 })).toBe("Runde 1 fertig: 17 Änderungen übernommen.");
    expect(chatStepText("runde", { round: 1, editsApplied: 1, editsFailed: 2, salvaged: true })).toBe("Runde 1 fertig: 1 Änderung übernommen, 2 offen, aus abgebrochener Antwort gerettet.");
  });

  it("erklärt eine Nacharbeitsrunde", () => {
    expect(chatStepText("nacharbeit", { round: 2, offen: 3, truncated: false })).toContain("3 Stellen werden nachgebessert");
    expect(chatStepText("nacharbeit", { round: 2, offen: 0, truncated: true })).toContain("abgeschnitten");
  });

  it("nennt Dateien so, wie ein Mensch sie nennt", () => {
    expect(designFileLabel(".werft-generated/019fa36f-d701/1/design.html")).toBe("Design");
    expect(designFileLabel("PerfectMoment/app/src/main/res/values/colors.xml")).toBe("colors.xml");
  });
});
