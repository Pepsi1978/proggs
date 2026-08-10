import { describe, expect, it } from "vitest";
import { emptyFacts, type DesignFacts } from "./design-facts.js";
import { buildExportPackage, ensureThemeControls, importeVoranstellen, schriftKopf, schriftUrls, schriftverweise, screenScopedCss, splitDesignDocument, styleBloeckeReparieren } from "./export-package.js";
import { composeScreens, repairThemeSelectors, themeVariants } from "./screen-composer.js";

// Der gemeldete Fall aus PerfectMoment: 18 Bildschirme, zwei Erscheinungen — heruntergeladen kam
// alles in Dunkel an, nur der Startbildschirm hell. Ursache: jeder Bildschirm bringt seine Farben
// FEST mit, und die helle Fassung hing an selbst erfundenen Selektoren.
const themes = [
  { id: "DarkPmColors", name: "DarkPmColors (Dunkel)", tokens: { background: "#181209", text1: "#f5eee2" }, source: "Theme.kt" },
  { id: "LightPmColors", name: "LightPmColors", tokens: { background: "#fbf6ec", text1: "#241d12" }, source: "Theme.kt" }
];
const facts = (patch: Partial<DesignFacts> = {}): DesignFacts => ({ ...emptyFacts("android"), themes, ...patch });
const sharedCss = `
.pm-start-screen { --background: #181209; color: #f5eee2; }
.pm-start-screen[data-pm-theme="light"] { --background: #fbf6ec; }
.pm-history-screen { --background: #181209; color: #f5eee2; }
[data-werft-theme="light"] .pm-history-screen { --background: #fbf6ec; }
`;
const options = { title: "Perfect Moment", platform: "android", width: 412, height: 915, device: "Pixel 9", density: 2.625, facts: facts(), sharedCss };
const screens = [
  { id: "compose:StartScreen", name: "StartScreen", markup: '<main class="pm-start-screen"><h1>Start</h1></main>', isStart: true, navigatesTo: ["compose:HistoryScreen"] },
  { id: "compose:HistoryScreen", name: "HistoryScreen", markup: '<main class="pm-history-screen"><section>Verlauf</section></main>', isStart: false }
];

describe("Erscheinungen im ausgelieferten Dokument", () => {
  it("führt erfundene Theme-Selektoren auf die echten Kennungen zurück", () => {
    const variants = themeVariants(themes);
    const repaired = repairThemeSelectors(sharedCss, variants);
    // `[data-werft-theme="light"]` konnte nie treffen — die Erscheinung heisst `lightpmcolors`.
    expect(repaired).toContain('[data-werft-theme="lightpmcolors"] .pm-history-screen');
    expect(repaired).not.toContain('[data-werft-theme="light"]');
    // Ein eigenes Attribut wird auf das Standardattribut umgebogen, das der Umschalter mitsetzt.
    expect(repaired).toContain('.pm-start-screen[data-theme="light"]');
    // Echte Kennungen bleiben unangetastet.
    expect(repairThemeSelectors('[data-werft-theme="darkpmcolors"] .x { color: red }', variants)).toContain('[data-werft-theme="darkpmcolors"]');
  });

  it("backt Umschalter und Farbabbildung fest in das aufgebaute Design", () => {
    const html = composeScreens(screens, options);
    expect(html).toContain("werft-theme-switcher");
    expect(html).toContain("werft-theme-switcher-script");
    // Die Zwillingsregel macht die feste Farbe des Bildschirms in der anderen Erscheinung umschaltbar.
    expect(html).toContain(':root[data-werft-theme="lightpmcolors"] .pm-history-screen');
    expect(html).toContain("#fbf6ec");
    // Und die Leiste bleibt im Studio aus, weil dort die Vorschau-Brücke umschaltet.
    expect(html).toContain("if (window.parent !== window) return;");
  });

  it("rüstet ein vor dieser Fassung aufgebautes Design beim Export nach", () => {
    const alt = `<!doctype html><html><head><title>Alt</title><style>${sharedCss}</style></head><body><div class="werft-screens"><section class="werft-screen" data-screen-id="a" data-screen-name="A" data-start="true"><main class="pm-start-screen">A</main></section></div></body></html>`;
    const nachgeruestet = ensureThemeControls(alt, themeVariants(themes), facts());
    expect(nachgeruestet).toContain('name="werft-themes"');
    expect(nachgeruestet).toContain("werft-theme-switcher");
    expect(nachgeruestet).toContain('[data-werft-theme="lightpmcolors"]');
    expect(nachgeruestet).not.toContain('[data-werft-theme="light"] ');
  });

  it("misst die eingebaute Erscheinung nur an den Bildschirm-Regeln", () => {
    const gefiltert = screenScopedCss(':root { --background: #ffffff; }\n@media (min-width: 10px) { :root { --a: 1 } .x { color: red } }\n.y { color: blue }');
    expect(gefiltert).not.toContain("--background");
    expect(gefiltert).toContain(".x { color: red }");
    expect(gefiltert).toContain(".y");
    expect(gefiltert).toContain("@media (min-width: 10px)");
  });
});

describe("Übergabepaket im Download", () => {
  const paket = () => {
    const html = composeScreens(screens, options);
    return buildExportPackage({
      projectName: "Perfect Moment",
      platform: "android",
      facts: facts({ screens: screens.map((screen) => ({ id: screen.id, name: screen.name, kind: "composable", source: "Screens.kt", navigatesTo: screen.navigatesTo ?? [], isStart: screen.isStart, files: ["Screens.kt"] })) }),
      designs: [{ path: ".werft-generated/job/1/design.html", html, label: "design.html" }],
      entryPath: ".werft-generated/job/1/design.html",
      sourceFiles: [{ path: "app/src/main/java/Screens.kt", size: 120 }]
    });
  };

  it("legt JEDEN Bildschirm in JEDER Erscheinung als eigene Datei ab", () => {
    const { files, report } = paket();
    const pfade = files.map((file) => file.path);
    for (const erscheinung of ["lightpmcolors", "darkpmcolors"]) {
      expect(pfade).toContain(`WERFT-DESIGN/bildschirme/${erscheinung}/1-startscreen.html`);
      expect(pfade).toContain(`WERFT-DESIGN/bildschirme/${erscheinung}/2-historyscreen.html`);
    }
    expect(report.bildschirmeExportiert).toBe(2);
    expect(report.erscheinungen).toBe(2);
    expect(report.nichtAufgebaut).toEqual([]);
    // Jede Einzeldatei steht fest in ihrer Erscheinung und zeigt genau ihren Bildschirm.
    const hell = files.find((file) => file.path.endsWith("lightpmcolors/2-historyscreen.html"))!.content;
    expect(hell).toContain('data-werft-theme="lightpmcolors"');
    expect(hell).toContain('data-theme="light"');
    expect(hell).toContain('data-screen-id="compose:HistoryScreen"');
    expect(hell).not.toContain('data-screen-id="compose:StartScreen"');
    expect(hell).toContain('href="../design.css"');
  });

  it("liefert alle gemessenen Werte maschinenlesbar mit", () => {
    const { files } = paket();
    const tokens = JSON.parse(files.find((file) => file.path.endsWith("design-tokens.json"))!.content);
    expect(tokens.erscheinungen).toHaveLength(2);
    expect(tokens.erscheinungen[0].tokens).toBeTruthy();
    expect(tokens.bildschirme).toHaveLength(2);
    expect(tokens.bildschirme[0].dateien.lightpmcolors).toBe("bildschirme/lightpmcolors/1-startscreen.html");
    expect(tokens.bildschirme[0].navigiertZu).toEqual(["compose:HistoryScreen"]);
    expect(tokens.vollstaendigkeit.bildschirmeExportiert).toBe(2);
    const spec = files.find((file) => file.path.endsWith("DESIGN-SPEC.md"))!.content;
    expect(spec).toContain("StartScreen");
    expect(spec).toContain("HistoryScreen");
    const liesmich = files.find((file) => file.path.endsWith("LIESMICH.md"))!.content;
    expect(liesmich).toContain("design-tokens.json");
    // Die Grundfassung liegt an einem festen, auffindbaren Ort statt unter `.werft-generated/<id>/`.
    expect(files.map((file) => file.path)).toContain("WERFT-DESIGN/design.html");
  });

  it("benennt Bildschirme, die gemessen aber nicht aufgebaut wurden", () => {
    const html = composeScreens([screens[0]!], { ...options, facts: facts() });
    const { report } = buildExportPackage({
      projectName: "Perfect Moment", platform: "android",
      facts: facts({ screens: [
        { id: "compose:StartScreen", name: "StartScreen", kind: "composable", source: "Screens.kt", navigatesTo: [], isStart: true, files: [] },
        { id: "compose:VoiceScreen", name: "VoiceScreen", kind: "composable", source: "Screens.kt", navigatesTo: [], isStart: false, files: [] }
      ] }),
      designs: [{ path: "d.html", html, label: "design.html" }], entryPath: "d.html", sourceFiles: []
    });
    expect(report.nichtAufgebaut).toEqual(["VoiceScreen"]);
    expect(report.bildschirmeImDesign).toBe(2);
    expect(report.bildschirmeExportiert).toBe(1);
  });

  it("zerlegt das Dokument auch bei verschachtelten Abschnitten korrekt", () => {
    const html = composeScreens(screens, options);
    const zerlegt = splitDesignDocument(html);
    expect(zerlegt.sections.map((section) => section.name)).toEqual(["StartScreen", "HistoryScreen"]);
    // Der zweite Bildschirm enthält selbst ein <section> — es darf ihn nicht zerschneiden.
    expect(zerlegt.sections[1]!.html).toContain("<section>Verlauf</section>");
    expect(zerlegt.width).toBe(412);
    expect(zerlegt.css).toContain(".pm-start-screen");
  });
});

/**
 * Der Schrift-Fehler, der ein ganzes Design entstellt hat, ohne aufzufallen.
 *
 * Ein Entwurf holt seine Schriften per `@import url("https://fonts.googleapis.com/…")`. Nach der
 * CSS-Spezifikation ist eine `@import`-Regel nur gueltig, wenn sie VOR allen anderen Regeln steht
 * (W3C CSS Cascade Level 5: "Any @import rules must precede all other valid at-rules and style
 * rules in a style sheet"; MDN: "As the @import at-rule is declared after the styles it is invalid
 * and hence ignored"). Real getroffen: der Import stand an Zeichen 6427 seines Style-Blocks —
 * keine der drei Schriften kam an, weder in der Vorschau noch in einer exportierten Datei.
 *
 * Das echte Uebergabepaket von Claude Design macht es anders und richtig: `<link rel="preconnect">`
 * plus `<link rel="stylesheet">` im Kopf, kein `@import` irgendwo.
 */
const fontUrl = "https://fonts.googleapis.com/css2?family=Fraunces:wght@400;600&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@400&display=swap";

describe("Schriften kommen an", () => {
  it("zieht einen ungueltig platzierten @import an den Anfang", () => {
    const css = `.a { color: red; }
@import url("${fontUrl}");
.b { color: blue; }`;
    const { css: repariert, importe } = importeVoranstellen(css);
    expect(importe).toHaveLength(1);
    expect(repariert.trimStart().startsWith("@import")).toBe(true);
    // Verlustfrei: beide Regeln sind noch da, in ihrer Reihenfolge.
    expect(repariert.indexOf(".a")).toBeLessThan(repariert.indexOf(".b"));
  });

  it("schneidet die URL nicht an ihren eigenen Semikolons entzwei", () => {
    // `wght@400;600` enthaelt Semikolons — ein Muster, das bis zum ersten `;` liest, zerstoert sie.
    const { importe } = importeVoranstellen(`.a{color:red}
@import url("${fontUrl}");`);
    expect(importe[0]).toContain("display=swap");
    expect(importe[0]).toContain("JetBrains+Mono");
  });

  it("laesst ein Stylesheet ohne @import unveraendert", () => {
    const css = ".a { color: red; }";
    expect(importeVoranstellen(css).css).toBe(css);
  });

  it("repariert die Style-Bloecke im Dokument selbst", () => {
    const html = `<html><head><style>.a{color:red}
@import url("${fontUrl}");</style></head><body></body></html>`;
    expect(styleBloeckeReparieren(html)).toMatch(/<style>\s*@import/);
  });

  it("findet die Schrift-URL sowohl im @import als auch im link-Tag", () => {
    expect(schriftUrls(`@import url("${fontUrl}");`)).toEqual([fontUrl]);
    expect(schriftUrls(`<link href="${fontUrl}" rel="stylesheet">`)).toEqual([fontUrl]);
  });

  it("baut den Kopf-Block wie das echte Claude-Design-Paket: preconnect, dann stylesheet", () => {
    const kopf = schriftKopf([fontUrl]);
    expect(kopf).toContain('<link rel="preconnect" href="https://fonts.googleapis.com">');
    expect(kopf).toContain('<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous">');
    expect(kopf.indexOf("preconnect")).toBeLessThan(kopf.indexOf("stylesheet"));
  });

  it("nennt je Schriftfamilie ihre Herkunft und ihre Gewichte", () => {
    const verweise = schriftverweise(`@import url("${fontUrl}");
.t{font-family:Fraunces,serif}
.m{font-family:"JetBrains Mono",monospace}`, "");
    const fraunces = verweise.find((v) => v.familie === "Fraunces");
    expect(fraunces?.quelle).toBe("verzeichnis");
    expect(fraunces?.gewichte).toEqual(["400", "600"]);
    expect(verweise.find((v) => v.familie === "JetBrains Mono")?.quelle).toBe("verzeichnis");
  });

  it("benennt eine Schrift OHNE Quelle, statt sie stillschweigend hinzunehmen", () => {
    const verweise = schriftverweise(".t{font-family:Fraunces,serif}", "");
    expect(verweise.find((v) => v.familie === "Fraunces")?.quelle).toBe("system");
  });

  it("schreibt die Schriftverweise in den Kopf JEDER Bildschirmdatei", () => {
    const html = `<!doctype html><html><head><meta name="werft-preview" content='{"width":412,"height":915}'><style>
.werft-screen{display:block}
@import url("${fontUrl}");
</style></head><body><section class="werft-screen" data-screen-id="B-01" data-screen-name="Heute" data-start="true"><h1>Heute</h1></section></body></html>`;
    const eigeneFacts: DesignFacts = { ...emptyFacts("android"), typography: [{ name: "titel", family: "Fraunces", source: "design.html" }] };
    const { files } = buildExportPackage({
      projectName: "Probe", platform: "android", facts: eigeneFacts, designs: [{ path: "design.html", html, label: "design.html" }],
      entryPath: "design.html", sourceFiles: [{ path: "design.html", size: html.length }]
    });
    const bildschirm = files.find((datei) => /bildschirme\/.+\/1-heute\.html$/.test(datei.path));
    expect(bildschirm).toBeDefined();
    expect(bildschirm!.content).toContain('rel="stylesheet" href="' + fontUrl.replace(/&/g, "&amp;") + '"');
    expect(bildschirm!.content).toContain('rel="preconnect"');
    // Und die Schrift steht als Verweis im maschinenlesbaren Teil.
    const tokens = files.find((datei) => datei.path.endsWith("design-tokens.json"));
    expect(tokens!.content).toContain('"schriften"');
    expect(tokens!.content).toContain("Fraunces");
  });
});
