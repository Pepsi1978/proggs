import { describe, expect, it } from "vitest";
import { applyChatEdits, parseChatResponse, repairBriefing, verifyFileWrite } from "./chat-edit.js";
import { designScreens, globalScope } from "./chat-scope.js";

// Dieser Test spielt den GEMELDETEN Vorfall nach: der Wunsch „das gesamte Design verbessern" lieferte
// eine Modellantwort, die von der Ausgabegrenze mitten im letzten Edit abgeschnitten wurde. Ergebnis
// war „Antwort der KI war kein gültiges JSON — es wurde nichts geändert", obwohl ein Dutzend
// einwandfreier Edits darin stand. Zusätzlich zitiert das Modell die Textstellen mit EINEM Leerzeichen
// Einrückung, während die Datei zwei benutzt — zeichengenau findet man so nichts.

const design = `<!doctype html>
<html lang="de">
<head>
<style>
.pm-settings-setting-row {
  position: relative;
  display: flex;
  width: 100%;
  height: 60px;
  align-items: center;
  padding: 0 16px;
  text-align: left;
}

.pm-settings-setting-row__value {
  flex: 0 0 auto;
  max-width: 164px;
  margin-left: 12px;
  text-align: right;
}

.pm-start__parameter {
  display: flex;
  min-width: 0;
  padding: 12px;
  flex-direction: column;
  align-items: flex-start;
}

.pm-hooks-card-text {
  display: -webkit-box;
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  margin: 0;
  padding: 0 14px;
}

.pm-history-detail__parameter-label {
  color: var(--pm-text2);
}
</style>
</head>
<body>
<div class="werft-screens">
<section class="werft-screen" data-screen-id="compose:Start" data-screen-name="Start" data-start="true"><div class="pm-start__parameter">A</div></section>
<section class="werft-screen" data-screen-id="compose:Settings" data-screen-name="Einstellungen"><div class="pm-settings-setting-row">B</div></section>
<section class="werft-screen" data-screen-id="compose:History" data-screen-name="Verlauf"><div class="pm-hooks-card-text">C</div></section>
</div>
<script>console.log("navigation");</script>
</body>
</html>`;

// 1:1 die Form der gemeldeten Antwort: Einrückung mit einem Leerzeichen, Abbruch mitten im letzten Edit.
const truncatedAnswer = '{"reply":"Erhöhe Innenabstände bei Karten, Parametern und Einstellungszeilen für mehr Luft.","changes":[{"path":"design.html","edits":['
  + '{"find":".pm-settings-setting-row {\\n position: relative;\\n display: flex;\\n width: 100%;\\n height: 60px;\\n align-items: center;\\n padding: 0 16px;","replace":".pm-settings-setting-row {\\n position: relative;\\n display: flex;\\n width: 100%;\\n height: 64px;\\n align-items: center;\\n padding: 0 20px;"},'
  + '{"find":".pm-settings-setting-row__value {\\n flex: 0 0 auto;\\n max-width: 164px;\\n margin-left: 12px;\\n text-align: right;","replace":".pm-settings-setting-row__value {\\n flex: 0 0 auto;\\n max-width: 160px;\\n margin-left: 14px;\\n text-align: left;"},'
  + '{"find":".pm-start__parameter {\\n display: flex;\\n min-width: 0;\\n padding: 12px;","replace":".pm-start__parameter {\\n display: flex;\\n min-width: 0;\\n padding: 14px;"},'
  + '{"find":".pm-hooks-card-text {\\n display: -webkit-box;\\n min-width: 0;\\n flex: 1 1 auto;\\n overflow: hidden;\\n margin: 0;\\n padding: 0 14px;","replace":".pm-hooks-card-text {\\n display: -webkit-box;\\n min-width: 0;\\n flex: 1 1 auto;\\n overflow: hidden;\\n margin: 0;\\n padding: 0 16px;"},'
  + '{"find":".pm-history-detail__parameter-label {';

describe("Der gemeldete Vorfall, Schritt für Schritt", () => {
  it("erkennt den Wunsch als Aufgabe für das gesamte Design", () => {
    expect(globalScope("Kannst du bitte das gesamte Design der App ein bisschen verbessern? Du hast freie Hand.")).toBe(true);
  });

  it("rettet alle vollständigen Edits aus der abgeschnittenen Antwort", () => {
    const parsed = parseChatResponse(truncatedAnswer);
    expect(parsed.truncated).toBe(true);
    expect(parsed.changes).toHaveLength(1);
    expect(parsed.changes[0]?.path).toBe("design.html");
    // Vier vollständige Edits — das fünfte war abgeschnitten und wird korrekt verworfen.
    expect(parsed.changes[0]?.edits).toHaveLength(4);
    expect(parsed.reply).toContain("Innenabstände");
  });

  it("findet die Stellen trotz abweichender Einrückung und wendet sie an", () => {
    const parsed = parseChatResponse(truncatedAnswer);
    const { content, outcomes } = applyChatEdits(design, parsed.changes[0]!.edits);
    expect(outcomes.every((outcome) => outcome.status === "applied")).toBe(true);
    expect(outcomes.every((outcome) => outcome.strategy === "leerraum")).toBe(true);
    expect(content).toContain("padding: 0 20px;");
    expect(content).toContain("padding: 14px;");
    expect(content).toContain("padding: 0 16px;");
    expect(content).toContain("text-align: left;");
    // Was nicht gemeint war, bleibt unangetastet.
    expect(content).toContain(".pm-history-detail__parameter-label {\n  color: var(--pm-text2);\n}");
  });

  it("bestätigt, dass alle Bildschirme und Skripte die Änderung überleben", () => {
    const parsed = parseChatResponse(truncatedAnswer);
    const { content } = applyChatEdits(design, parsed.changes[0]!.edits);
    expect(verifyFileWrite("design.html", design, content)).toEqual([]);
    expect(designScreens(content).map((screen) => screen.name)).toEqual(["Start", "Einstellungen", "Verlauf"]);
    expect(content).toContain('<script>console.log("navigation");</script>');
  });

  it("verlangt für die nächste Runde die fehlenden Restpunkte in kleineren Häppchen", () => {
    const parsed = parseChatResponse(truncatedAnswer);
    const { outcomes } = applyChatEdits(design, parsed.changes[0]!.edits);
    const briefing = repairBriefing([{ path: "design.html", outcomes, written: true, issues: [] }], parsed.truncated);
    expect(briefing).toContain("abgeschnitten");
    expect(briefing).toContain("noch fehlenden Änderungen");
  });

  it("lässt eine zweite Runde auf dem bereits geänderten Stand weiterarbeiten", () => {
    const first = applyChatEdits(design, parseChatResponse(truncatedAnswer).changes[0]!.edits).content;
    const second = parseChatResponse('{"reply":"Rest ergänzt","changes":[{"path":"design.html","edits":[{"find":".pm-history-detail__parameter-label {\\n color: var(--pm-text2);","replace":".pm-history-detail__parameter-label {\\n color: var(--pm-text2);\\n  letter-spacing: 0.4px;"}]}]}');
    const result = applyChatEdits(first, second.changes[0]!.edits);
    expect(result.outcomes[0]?.status).toBe("applied");
    expect(result.content).toContain("letter-spacing: 0.4px;");
    // Die Arbeit der ersten Runde bleibt erhalten.
    expect(result.content).toContain("padding: 0 20px;");
    expect(verifyFileWrite("design.html", first, result.content)).toEqual([]);
  });
});
