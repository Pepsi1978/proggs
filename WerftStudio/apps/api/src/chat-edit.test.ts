import { describe, expect, it } from "vitest";
import { applyChatEdits, balancedObjects, parseChatResponse, repairBriefing, unwrapResponse, verifyFileWrite } from "./chat-edit.js";

describe("Antwort einlesen", () => {
  it("liest eine saubere JSON-Antwort", () => {
    const parsed = parseChatResponse('{"reply":"fertig","changes":[{"path":"a.html","edits":[{"find":"rot","replace":"blau"}]}]}');
    expect(parsed).toMatchObject({ reply: "fertig", truncated: false, salvaged: false });
    expect(parsed.changes).toEqual([{ path: "a.html", edits: [{ find: "rot", replace: "blau" }] }]);
  });

  it("holt das JSON aus einem Codeblock mit Vorrede", () => {
    const parsed = parseChatResponse('Gerne! Hier die Änderungen:\n```json\n{"reply":"ok","changes":[{"path":"a.css","edits":[{"find":"12px","replace":"16px"}]}]}\n```\nViel Erfolg.');
    expect(parsed.changes[0]?.edits[0]).toEqual({ find: "12px", replace: "16px" });
    expect(parsed.reply).toBe("ok");
  });

  it("rettet fertige Edits aus einer abgeschnittenen Antwort statt alles zu verwerfen", () => {
    // Genau der gemeldete Fall: der Ausgabestrom endet mitten im letzten Edit.
    const raw = '{"reply":"Mehr Luft in den Karten","changes":[{"path":"design.html","edits":['
      + '{"find":".pm-card { padding: 12px;","replace":".pm-card { padding: 16px;"},'
      + '{"find":".pm-row { padding: 0 16px;","replace":".pm-row { padding: 0 20px;"},'
      + '{"find":".pm-hooks-card-text {\\n  padding: 0 14px;","replace":".pm-hooks-ca';
    const parsed = parseChatResponse(raw);
    expect(parsed.truncated).toBe(true);
    expect(parsed.salvaged).toBe(true);
    expect(parsed.reply).toBe("Mehr Luft in den Karten");
    expect(parsed.changes).toEqual([{ path: "design.html", edits: [
      { find: ".pm-card { padding: 12px;", replace: ".pm-card { padding: 16px;" },
      { find: ".pm-row { padding: 0 16px;", replace: ".pm-row { padding: 0 20px;" }
    ] }]);
  });

  it("verwechselt geschweifte Klammern in Zeichenketten nicht mit JSON-Struktur", () => {
    const parsed = parseChatResponse('{"reply":"ok","changes":[{"path":"a.css","edits":[{"find":".a { color: red; }","replace":".a { color: blue; }"}]}]}');
    expect(parsed.changes[0]?.edits[0]?.replace).toBe(".a { color: blue; }");
    expect(parsed.salvaged).toBe(false);
  });

  it("verzeiht ein nachgestelltes Komma", () => {
    expect(parseChatResponse('{"reply":"ok","changes":[{"path":"a.css","edits":[{"find":"x","replace":"y"},]},]}').changes[0]?.edits).toHaveLength(1);
  });

  it("übernimmt ganze Dateien, wenn das Modell sie liefert", () => {
    const parsed = parseChatResponse('{"reply":"neu","files":[{"path":"a.css","content":".a{color:red}"}]}');
    expect(parsed.files).toEqual([{ path: "a.css", content: ".a{color:red}" }]);
  });

  it("zählt offene Klammern für die Abbrucherkennung", () => {
    expect(balancedObjects('{"a":{"b":1}}').open).toBe(0);
    expect(balancedObjects('{"a":{"b":1}').open).toBe(1);
    expect(balancedObjects('{"a":"{{{"}').open).toBe(0);
  });

  it("lässt eine Antwort, die selbst JSON ist, unangetastet", () => {
    expect(unwrapResponse('  {"reply":"x"}  ')).toBe('{"reply":"x"}');
  });
});

describe("Edits anwenden", () => {
  it("ersetzt zeichengenau und lässt den Rest unberührt", () => {
    const { content, outcomes } = applyChatEdits(".a{color:red}.b{color:red}", [{ find: ".a{color:red}", replace: ".a{color:blue}" }]);
    expect(content).toBe(".a{color:blue}.b{color:red}");
    expect(outcomes[0]).toMatchObject({ status: "applied", strategy: "zeichengenau", replaced: 1 });
  });

  it("behandelt Dollarzeichen im Ersatztext wörtlich", () => {
    // `$&` im Ersatztext wurde vom Ersetzer als Rückverweis ausgewertet: aus "$&" wurde der
    // gefundene Text. Das hat Inhalte still verfälscht.
    const { content } = applyChatEdits('label: "Preis"', [{ find: '"Preis"', replace: '"Preis $& $1 100 $"' }]);
    expect(content).toBe('label: "Preis $& $1 100 $"');
  });

  it("findet eine Stelle trotz abweichender Zeilenenden und Einrückung", () => {
    const file = ".pm-card {\r\n    padding: 12px;\r\n    color: red;\r\n}";
    const { content, outcomes } = applyChatEdits(file, [{ find: ".pm-card {\n  padding: 12px;\n  color: red;\n}", replace: ".pm-card {\n  padding: 20px;\n  color: red;\n}" }]);
    expect(content).toContain("padding: 20px;");
    expect(outcomes[0]?.status).toBe("applied");
  });

  it("ändert mit all alle Vorkommen auf einmal", () => {
    const { content, outcomes } = applyChatEdits("#181209 a #181209 b #181209", [{ find: "#181209", replace: "#241B0E", all: true }]);
    expect(content).toBe("#241B0E a #241B0E b #241B0E");
    expect(outcomes[0]?.replaced).toBe(3);
  });

  it("rät nicht, wenn ein nachsichtiger Treffer mehrdeutig wäre", () => {
    const { content, outcomes } = applyChatEdits("padding:  8px;\npadding: 8px;", [{ find: "padding: 8px;", replace: "padding: 12px;" }]);
    // Zeichengenau gibt es genau einen Treffer — der wird genommen, der doppeldeutige bleibt heil.
    expect(content).toBe("padding:  8px;\npadding: 12px;");
    expect(outcomes[0]?.strategy).toBe("zeichengenau");
  });

  it("meldet eine wirklich fehlende Stelle statt still nichts zu tun", () => {
    const { outcomes } = applyChatEdits(".a{}", [{ find: ".gibtesnicht{color:red}", replace: "x" }]);
    expect(outcomes[0]).toMatchObject({ status: "not-found", replaced: 0 });
  });
});

describe("Unversehrtheit prüfen", () => {
  const doc = '<html><body><section class="werft-screen" data-screen-id="a"><style>.x{}</style><div>1</div></section><section class="werft-screen" data-screen-id="b"><div>2</div></section></body></html>';

  it("lässt eine saubere Änderung durch", () => {
    expect(verifyFileWrite("design.html", doc, doc.replace("<div>1</div>", "<div>eins</div>"))).toEqual([]);
  });

  it("verhindert, dass ein Bildschirm verschwindet", () => {
    const broken = doc.replace('<section class="werft-screen" data-screen-id="b"><div>2</div></section>', "");
    expect(verifyFileWrite("design.html", doc, broken).join(" ")).toContain("Bildschirm");
  });

  it("verhindert unpaarige Elemente", () => {
    expect(verifyFileWrite("design.html", doc, doc.replace("</style>", "")).join(" ")).toContain("style");
  });

  it("verhindert eine leere oder halbierte Datei", () => {
    expect(verifyFileWrite("a.css", "x".repeat(5_000), "")).toEqual(["Die Datei wäre danach leer."]);
    expect(verifyFileWrite("a.css", "x".repeat(5_000), "x".repeat(100)).join(" ")).toContain("schrumpfen");
  });

  it("prüft JSON auf Gültigkeit", () => {
    expect(verifyFileWrite("a.json", '{"a":1}', '{"a":')).toHaveLength(1);
    expect(verifyFileWrite("a.json", '{"a":1}', '{"a":2}')).toEqual([]);
  });
});

describe("Rückmeldung für die nächste Runde", () => {
  it("nennt nicht gefundene und mehrdeutige Stellen konkret", () => {
    const briefing = repairBriefing([{ path: "design.html", written: true, issues: [], outcomes: [
      { find: ".fehlt{}", status: "not-found", occurrences: 0, replaced: 0 },
      { find: "padding: 8px;", status: "ambiguous", occurrences: 4, replaced: 0, strategy: "leerraum" }
    ] }], false);
    expect(briefing).toContain("kommt so NICHT in der Datei vor");
    expect(briefing).toContain("4-mal");
  });

  it("weist auf einen Abbruch hin und verlangt kleinere Häppchen", () => {
    expect(repairBriefing([], true)).toContain("kleineren Häppchen");
  });

  it("schweigt, wenn alles geklappt hat", () => {
    expect(repairBriefing([{ path: "a.css", written: true, issues: [], outcomes: [{ find: "x", status: "applied", occurrences: 1, replaced: 1 }] }], false)).toBeUndefined();
  });
});
