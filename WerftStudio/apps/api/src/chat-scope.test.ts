import { describe, expect, it } from "vitest";
import { designMap, designScreens, designSections, excerptDesign, globalScope, inputCharBudget, reservedOutputTokens, selectChatFiles, sharedClasses } from "./chat-scope.js";

const doc = [
  '<section class="werft-screen" data-screen-id="compose:Start" data-screen-name="Start" data-start="true">',
  '<div class="pm-card"><span class="pm-label">A</span></div></section>',
  '<section class="werft-screen" data-screen-id="compose:Settings" data-screen-name="Einstellungen">',
  '<div class="pm-card"><span class="pm-label">B</span></div></section>'
].join("\n");

describe("Umfang eines Wunsches", () => {
  it("erkennt einen Wunsch für das gesamte Design", () => {
    expect(globalScope("Kannst du bitte das gesamte Design der App ein bisschen verbessern?")).toBe(true);
    expect(globalScope("Ich wollte das gesamte Dunkeldesign von der Perfect Moment App etwas heller machen.")).toBe(true);
    expect(globalScope("Bitte auf allen Bildschirmen mehr Abstand")).toBe(true);
    expect(globalScope("Die Ränder sollen generell in der ganzen App luftiger sein")).toBe(true);
  });

  it("hält einen Wunsch am sichtbaren Bildschirm, wenn er dorthin zeigt", () => {
    expect(globalScope("Mach den Knopf hier größer")).toBe(false);
    expect(globalScope("Auf diesem Bildschirm bitte alle Ecken runder")).toBe(false);
    expect(globalScope("Die Überschrift soll fetter sein")).toBe(false);
  });
});

describe("Design-Landkarte", () => {
  it("liest alle Bildschirme aus dem aufgebauten Dokument", () => {
    expect(designScreens(doc)).toEqual([
      { id: "compose:Start", name: "Start" },
      { id: "compose:Settings", name: "Einstellungen" }
    ]);
  });

  it("findet die mehrfach verwendeten Klassen", () => {
    expect(sharedClasses(doc)).toEqual(["pm-card", "pm-label"]);
  });

  it("stellt Bildschirme und gemeinsame Bausteine als Text bereit", () => {
    const map = designMap(doc, ".werft-generated/1/design.html", "Einstellungen");
    expect(map).toContain('data-screen-id="compose:Settings"');
    expect(map).toContain("Gerade sichtbar: Einstellungen");
    expect(map).toContain("pm-card");
  });
});

describe("Kontextbudget", () => {
  it("rechnet Zeichen statt Bytes und weist große Designs nicht mehr grundlos ab", () => {
    // 200k Kontext, 32k für die Ausgabe reserviert: rund 500k Zeichen Eingabe sind möglich.
    expect(inputCharBudget(200_000, 32_000)).toBeGreaterThan(450_000);
    expect(inputCharBudget(undefined, 32_000)).toBe(700_000);
  });

  it("reserviert Ausgaberaum, ohne das Anbieter-Maximum zu überschreiten", () => {
    expect(reservedOutputTokens(200_000)).toBe(32_000);
    expect(reservedOutputTokens(200_000, 8_192)).toBe(8_192);
    expect(reservedOutputTokens(16_000)).toBe(5_600);
  });
});

describe("Dateiauswahl", () => {
  const files = [
    { path: "design.html", size: 400_000, mime: "text/html" },
    { path: "a.css", size: 20_000, mime: "text/css" },
    { path: "b.js", size: 900_000, mime: "text/javascript" }
  ];

  it("nimmt die Startdatei immer mit und füllt nach Größe auf", () => {
    const { selected, omitted } = selectChatFiles(files, "design.html", 500_000);
    expect(selected.map((file) => file.path)).toEqual(["design.html", "a.css"]);
    expect(omitted.map((file) => file.path)).toEqual(["b.js"]);
  });

  it("verschluckt eine große Startdatei nicht mehr still", () => {
    // Früher fiel jede Datei über 300 KB kommentarlos aus der Auswahl — das Design war unsichtbar.
    expect(selectChatFiles(files, "design.html", 50_000).selected.map((file) => file.path)).toEqual(["design.html"]);
  });
});

describe("Ausschnitt für kleine Kontextfenster", () => {
  const big = [
    "<style>.a{color:red}</style>",
    '<section class="werft-screen" data-screen-id="s1" data-screen-name="Start"><div>eins<section class="inner">verschachtelt</section></div></section>',
    '<section class="werft-screen" data-screen-id="s2" data-screen-name="Einstellungen"><div>zwei</div></section>',
    '<section class="werft-screen" data-screen-id="s3" data-screen-name="Verlauf"><div>drei</div></section>',
    "<script>navigation()</script>"
  ].join("\n");

  it("findet Bildschirme trotz verschachtelter Abschnitte", () => {
    expect(designSections(big).map((section) => section.id)).toEqual(["s1", "s2", "s3"]);
  });

  it("behält Stile, Skripte und den gefragten Bildschirm und kürzt den Rest", () => {
    const { text, omittedScreens } = excerptDesign(big, ["Einstellungen"]);
    expect(omittedScreens).toEqual(["Start", "Verlauf"]);
    expect(text).toContain("<style>.a{color:red}</style>");
    expect(text).toContain("<script>navigation()</script>");
    expect(text).toContain("<div>zwei</div>");
    expect(text).not.toContain("<div>eins");
    expect(text).not.toContain("<div>drei</div>");
    // Die Hülle bleibt: das Modell sieht, dass es die Bildschirme gibt.
    expect(designSections(text).map((section) => section.id)).toEqual(["s1", "s2", "s3"]);
  });

  it("behält ohne Bildschirmangabe den ersten und lässt Dokumente ohne Bildschirme unangetastet", () => {
    expect(excerptDesign(big, []).omittedScreens).toEqual(["Einstellungen", "Verlauf"]);
    expect(excerptDesign("<p>nur Text</p>", ["x"])).toEqual({ text: "<p>nur Text</p>", omittedScreens: [] });
  });
});
