import { describe, expect, it } from "vitest";
import { emptyFacts, type DesignFacts } from "./design-facts.js";
import { checkFidelity, fidelityAcceptable, hasIssuesForSources, renderFidelityInstructions } from "./fidelity-check.js";
import { analysisBudget, mapWithConcurrency, reconstructionSourceFiles } from "./import-reconstruction.js";
import { composeScreens, extractScreenFragment, orderScreensByFlow, resolveNavigationTarget, screenPlanFrom, themeStyles, themeVariants } from "./screen-composer.js";

const facts = (patch: Partial<DesignFacts> = {}): DesignFacts => ({ ...emptyFacts("android"), ...patch });
const options = (overrides: Partial<Parameters<typeof composeScreens>[1]> = {}) => ({ title: "Acme", platform: "android", width: 412, height: 915, device: "Pixel 9", density: 2.625, facts: facts(), ...overrides });

describe("Bildschirm-Zusammenbau", () => {
  it("setzt alle Bildschirme in ein durchklickbares Dokument mit Startbildschirm", () => {
    const html = composeScreens([
      { id: "home", name: "Start", markup: "<div>Start</div>", isStart: true },
      { id: "details", name: "Details", markup: '<button data-werft-navigate="home">Zurück</button>', isStart: false }
    ], options());
    expect(html.startsWith("<!doctype html>")).toBe(true);
    expect(html).toContain('data-screen-id="home"');
    expect(html).toContain('data-screen-id="details"');
    expect(html).toContain('data-start="true"');
    expect(html).toContain('data-werft-navigate="home"');
    expect(html).toContain("werft-screen-switcher");
    // Kein Bildschirm darf beim Zusammenbau verloren gehen.
    expect([...html.matchAll(/class="werft-screen"/g)]).toHaveLength(2);
  });

  it("schreibt die tatsächliche Quellgeometrie maschinenlesbar in den Kopf", () => {
    const html = composeScreens([{ id: "a", name: "A", markup: "<i></i>", isStart: true }], options({ width: 1100, height: 720, device: "Acme (Windows)", density: 1, platform: "windows" }));
    expect(html).toContain('name="werft-preview"');
    expect(html).toContain('"width":1100');
    expect(html).toContain('"height":720');
    expect(html).toContain("width: 1100px");
  });

  it("erzeugt Theme-Variablen für Hell und Dunkel aus den gemessenen Farben", () => {
    const css = themeStyles(facts({ themes: [
      { id: "LightColors", name: "LightColors", tokens: { primary: "#3157d5" }, source: "Theme.kt" },
      { id: "DarkColors", name: "DarkColors (Dunkel)", tokens: { primary: "#9db4ff" }, source: "Theme.kt" }
    ] }));
    expect(css).toContain("--primary: #3157d5");
    expect(css).toContain("prefers-color-scheme: dark");
    expect(css).toContain('[data-theme="dark"]');
  });

  // Gemeldet an PerfectMoment und OpenLauncher: von 18 Bildschirmen war nur EINE Erscheinung zu
  // sehen. Frueher landeten hoechstens zwei Themes im Dokument, alle weiteren fielen still weg.
  it("macht JEDE gemessene Erscheinung einzeln wählbar", () => {
    const themes = [
      { id: "LightColors", name: "LightColors", tokens: { primary: "#3157d5", background: "#ffffff" }, source: "Theme.kt" },
      { id: "DarkColors", name: "DarkColors (Dunkel)", tokens: { primary: "#9db4ff", background: "#101318" }, source: "Theme.kt" },
      { id: "ForestColors", name: "ForestColors", tokens: { primary: "#0c7a5b", background: "#f2f7f4" }, source: "Theme.kt" }
    ];
    const css = themeStyles(facts({ themes }));
    expect(css).toContain('[data-werft-theme="lightcolors"]');
    expect(css).toContain('[data-werft-theme="darkcolors"]');
    expect(css).toContain('[data-werft-theme="forestcolors"]');
    const variants = themeVariants(themes);
    expect(variants).toHaveLength(3);
    expect(variants.map((variant) => variant.kind)).toEqual(["light", "dark", "other"]);
    // Die Vorschaufarbe kommt aus dem Hintergrund-Token — daran erkennt man das Theme im Board.
    expect(variants[1]!.color).toBe("#101318");
    // Und das Dokument nennt seine Erscheinungen, damit die Vorschau sie nicht raten muss.
    const html = composeScreens([{ id: "a", name: "A", markup: "<i></i>", isStart: true }], options({ facts: facts({ themes }) }));
    expect(html).toContain('name="werft-themes"');
    expect(html).toContain('"kind":"dark"');
  });

  // Eine Zusatzpalette mit zwei Farben neben einem Theme mit dreissig ist keine Erscheinung der App.
  it("lässt Zusatzpaletten aus der Auswahl heraus, behält aber kleine Themes wenn alle klein sind", () => {
    const big = Object.fromEntries(Array.from({ length: 30 }, (_, index) => [`token${index}`, "#101010"]));
    const variants = themeVariants([
      { id: "AppTheme", name: "AppTheme", tokens: big, source: "Theme.kt" },
      { id: "BadgeColors", name: "BadgeColors", tokens: { badge: "#ff0000", badgeOn: "#ffffff" }, source: "Badge.kt" }
    ]);
    expect(variants.map((variant) => variant.id)).toEqual(["apptheme"]);
    expect(themeVariants([{ id: "OnlyColors", name: "OnlyColors", tokens: { a: "#111111" }, source: "x.kt" }])).toHaveLength(1);
  });

  it("löst auch ein versehentlich komplettes Dokument in ein Fragment auf", () => {
    const fragment = extractScreenFragment("```html\n<!doctype html><html><head><style>.a{color:red}</style><title>X</title></head><body><main>Inhalt</main></body></html>\n```");
    expect(fragment.markup).toBe("<main>Inhalt</main>");
    expect(fragment.css).toBe(".a{color:red}");
    expect(extractScreenFragment("<section>Nur ein Fragment</section>").markup).toBe("<section>Nur ein Fragment</section>");
  });

  it("liefert einen Ersatzbildschirm, wenn keine Screens erkannt wurden", () => {
    expect(screenPlanFrom(facts(), "Acme")).toHaveLength(1);
    expect(screenPlanFrom(facts(), "Acme")[0]).toMatchObject({ name: "Acme", isStart: true });
  });

  it("führt verkürzte Verknüpfungen auf die echte Bildschirm-Kennung zurück", () => {
    // Genau dieser Fall stand im ausgelieferten Design: 50 Verknüpfungen auf „HistoryScreen",
    // während die Bildschirme „compose:HistoryScreen" heissen — jeder Klick lief ins Leere.
    const screens = [{ id: "compose:HistoryScreen", name: "HistoryScreen" }, { id: "compose:StartScreen", name: "StartScreen" }];
    expect(resolveNavigationTarget("HistoryScreen", screens)).toBe("compose:HistoryScreen");
    expect(resolveNavigationTarget("compose:StartScreen", screens)).toBe("compose:StartScreen");
    expect(resolveNavigationTarget("history_screen", screens)).toBe("compose:HistoryScreen");
    // Unbekannte Ziele bleiben unverändert, statt auf einen zufälligen Bildschirm zu zeigen.
    expect(resolveNavigationTarget("Unbekannt", screens)).toBe("Unbekannt");
    const html = composeScreens([
      { id: "compose:StartScreen", name: "StartScreen", markup: '<button data-werft-navigate="HistoryScreen">Verlauf</button>', isStart: true },
      { id: "compose:HistoryScreen", name: "HistoryScreen", markup: "<div>Verlauf</div>", isStart: false }
    ], options());
    expect(html).toContain('data-werft-navigate="compose:HistoryScreen"');
  });

  it("stellt den Startbildschirm nach vorn und ordnet danach nach Navigationsfluss", () => {
    const ordered = orderScreensByFlow([
      { id: "settings", isStart: false, navigatesTo: [] },
      { id: "home", isStart: true, navigatesTo: ["details"] },
      { id: "details", isStart: false, navigatesTo: [] }
    ]);
    expect(ordered.map((screen) => screen.id)).toEqual(["home", "details", "settings"]);
  });

  it("baut den Startbildschirm als erste Sektion ein, auch wenn er zuletzt geliefert wird", () => {
    const html = composeScreens([
      { id: "locked", name: "AppLockedScreen", markup: "<i>gesperrt</i>", isStart: false },
      { id: "home", name: "StartScreen", markup: "<i>start</i>", isStart: true }
    ], options());
    expect(html.indexOf('data-screen-id="home"')).toBeLessThan(html.indexOf('data-screen-id="locked"'));
  });

  it("wählt ohne erkannten Startbildschirm den Einstieg statt eines Sperrbildschirms", () => {
    const plan = screenPlanFrom(facts({ screens: [
      { id: "compose:AppLockedScreen", name: "AppLockedScreen", kind: "composable", source: "MainActivity.kt", navigatesTo: [], isStart: false, files: [] },
      { id: "compose:ChatGptScreen", name: "ChatGptScreen", kind: "composable", source: "Screens.kt", navigatesTo: [], isStart: false, files: [] },
      { id: "compose:StartScreen", name: "StartScreen", kind: "composable", source: "Screens.kt", navigatesTo: ["compose:ChatGptScreen"], isStart: false, files: [] }
    ] }), "Perfect Moment");
    expect(plan[0]).toMatchObject({ name: "StartScreen", isStart: true });
    expect(plan.map((screen) => screen.name)).toEqual(["StartScreen", "ChatGptScreen", "AppLockedScreen"]);
    // Kein Bildschirm darf bei der Umsortierung verloren gehen.
    expect(plan).toHaveLength(3);
  });
});

describe("Nachmessung der Rekonstruktion", () => {
  const measured = facts({
    colors: [{ name: "brand", css: "#3157d5", source: "colors.xml", used: true }],
    dimensions: [{ name: "pad", px: 18, raw: "18dp", source: "dimens.xml", used: true }],
    effects: [{ name: "card", kind: "shadow", css: "0px 1px 2px 0px rgba(0, 0, 0, 0.3)", source: "layout.xml" }],
    shapes: [{ name: "card", radiusCss: "12px", source: "shapes.xml", used: true }]
  });

  it("erkennt exakt übernommene Quellwerte als Treffer", () => {
    const html = `<!doctype html><html><head><meta name="werft-preview" content='{}'></head><body><div style="color:#3157d5;padding:18px;border-radius:12px;box-shadow:0px 1px 2px 0px rgba(0, 0, 0, 0.3)"></div></body></html>`;
    const report = checkFidelity(html, measured);
    expect(report.issues).toHaveLength(0);
    expect(report.score).toBe(100);
    expect(fidelityAcceptable(report)).toBe(true);
  });

  it("meldet jede fehlende Farbe, jedes fehlende Maß und jeden fehlenden Effekt konkret", () => {
    const html = `<!doctype html><html><head><meta name="werft-preview" content='{}'></head><body><div style="color:#3157d6;padding:16px;border-radius:8px"></div></body></html>`;
    const report = checkFidelity(html, measured);
    expect(report.score).toBeLessThan(100);
    const instructions = renderFidelityInstructions(report);
    expect(instructions).toContain("#3157d5");
    expect(instructions).toContain("18px");
    expect(instructions).toContain("12px");
    expect(instructions).toContain("0px 1px 2px 0px rgba(0, 0, 0, 0.3)");
    expect(fidelityAcceptable(report)).toBe(false);
  });

  it("wertet dieselbe Farbe in Hex- und rgb-Schreibweise als identisch", () => {
    const html = `<!doctype html><html><head><meta name="werft-preview" content='{}'></head><body><div style="color:rgb(49,87,213)"></div></body></html>`;
    expect(checkFidelity(html, facts({ colors: [{ name: "brand", css: "#3157d5", source: "colors.xml", used: true }] })).issues).toHaveLength(0);
  });

  it("fordert nur definierte, aber nirgends verwendete Ressourcen NICHT ein", () => {
    // Eine colors.xml enthält fast immer ungenutzte Farben. Sie einzufordern würde die KI zwingen,
    // sie ins Design einzubauen — und damit das Original verfälschen.
    const html = `<!doctype html><html><head><meta name="werft-preview" content='{}'></head><body><div></div></body></html>`;
    const report = checkFidelity(html, facts({
      colors: [{ name: "ungenutzt", css: "#abcdef", source: "colors.xml" }],
      dimensions: [{ name: "ungenutzt", px: 999, raw: "999dp", source: "dimens.xml" }],
      shapes: [{ name: "ungenutzt", radiusCss: "77px", source: "shapes.xml" }]
    }));
    expect(report.issues).toHaveLength(0);
    expect(report.checked).toBe(0);
  });

  it("erkennt einen leer gebliebenen Bildschirm als blockierend", () => {
    const composed = composeScreens([
      { id: "home", name: "Start", markup: "<main><h1>Start</h1><p>Inhalt des Startbildschirms</p></main>", isStart: true },
      { id: "leer", name: "Leer", markup: "", isStart: false }
    ], options());
    const report = checkFidelity(composed, facts({ screens: [{ id: "leer", name: "Leer", kind: "composable", source: "Leer.kt", navigatesTo: [], isStart: false, files: ["Leer.kt"] }] }));
    const blocker = report.issues.find((issue) => issue.area === "Screen");
    expect(blocker?.severity).toBe("blocker");
    expect(blocker?.detail).toContain("Leer");
    expect(blocker?.source).toBe("Leer.kt");
  });

  it("gibt jedem Bildschirm nur die Abweichungen aus seinen eigenen Quellen", () => {
    const report = checkFidelity(`<!doctype html><html><head><meta name="werft-preview" content='{}'></head><body></body></html>`, facts({
      colors: [{ name: "a", css: "#111111", source: "ui/Home.kt", used: true }, { name: "b", css: "#222222", source: "ui/Details.kt", used: true }]
    }));
    expect(hasIssuesForSources(report, ["ui/Home.kt"])).toBe(true);
    const homeOnly = renderFidelityInstructions(report, { sources: ["ui/Home.kt"] });
    expect(homeOnly).toContain("#111111");
    expect(homeOnly).not.toContain("#222222");
  });

  it("behandelt fehlendes Dokument und fehlende Geometrie als blockierend", () => {
    const report = checkFidelity("<div>nur ein Fragment</div>", facts());
    expect(report.issues.filter((issue) => issue.severity === "blocker")).toHaveLength(2);
    expect(fidelityAcceptable(report)).toBe(false);
  });
});

describe("Parallele Verarbeitung", () => {
  it("behält die Ergebnisreihenfolge trotz unterschiedlicher Laufzeiten bei", async () => {
    const items = [50, 10, 30, 5, 20];
    const results = await mapWithConcurrency(items, 3, async (value) => { await new Promise((resolve) => setTimeout(resolve, value / 10)); return value; });
    expect(results).toEqual(items);
  });

  it("überschreitet die Nebenläufigkeitsgrenze nie", async () => {
    let active = 0, peak = 0;
    await mapWithConcurrency(Array.from({ length: 12 }, (_, index) => index), 4, async () => {
      active += 1;
      peak = Math.max(peak, active);
      await new Promise((resolve) => setTimeout(resolve, 5));
      active -= 1;
    });
    expect(peak).toBeLessThanOrEqual(4);
    expect(peak).toBeGreaterThan(1);
  });

  it("gibt den ersten Fehler weiter und startet keine weiteren Aufgaben", async () => {
    let started = 0;
    await expect(mapWithConcurrency(Array.from({ length: 20 }, (_, index) => index), 2, async (value) => {
      started += 1;
      if (value === 1) throw new Error("Analysepaket fehlgeschlagen");
      await new Promise((resolve) => setTimeout(resolve, 5));
      return value;
    })).rejects.toThrow(/Analysepaket fehlgeschlagen/);
    expect(started).toBeLessThan(20);
  });
});

describe("Auswahl der Analysequellen", () => {
  const entry = (path: string, size = 1000) => ({ path, size, mime: "text/plain" });

  it("schickt nur oberflächenbeschreibende Dateien in die teure KI-Analyse", () => {
    // Entscheidend fuer die Laufzeit: Ein Projekt mit hunderten Dokumentations-, Konfigurations-
    // und Ressourcendateien erzeugte daraus dutzende Analysepakete, von denen keines ein Pixel
    // erklaerte. Die Werte aus res/values liest der Extraktor exakt und ohne KI-Aufruf.
    const chosen = reconstructionSourceFiles([
      entry("MainWindow.xaml"),
      entry("Themes/DarkTheme.xaml"),
      entry("app/src/main/res/layout/activity_main.xml"),
      entry("app/src/main/java/acme/ui/HomeScreen.kt"),
      entry("app/src/main/res/values/colors.xml"),
      entry("app/src/main/res/values-night/colors.xml"),
      entry("app/src/main/res/values-fr/strings.xml"),
      entry("app/src/main/java/acme/data/UserRepository.kt"),
      entry("Profiles/ClaudeCode/strict/skills/uebersetzung/references/languages/de.md"),
      entry(".claude/agent-memory/shared/experience-store.jsonl"),
      entry("Profiles/settings.json"),
      entry("build.gradle.kts"),
      entry("gradle.properties"),
      entry("README.md"),
      entry("app/build/generated/Binding.java")
    ]).map((file) => file.path);
    expect(chosen).toEqual(expect.arrayContaining(["MainWindow.xaml", "Themes/DarkTheme.xaml", "app/src/main/res/layout/activity_main.xml", "app/src/main/java/acme/ui/HomeScreen.kt"]));
    for (const excluded of [
      "Profiles/ClaudeCode/strict/skills/uebersetzung/references/languages/de.md",
      ".claude/agent-memory/shared/experience-store.jsonl",
      "Profiles/settings.json",
      "app/src/main/res/values/colors.xml",
      "app/src/main/res/values-night/colors.xml",
      "app/src/main/res/values-fr/strings.xml",
      "build.gradle.kts",
      "gradle.properties",
      "README.md",
      "app/build/generated/Binding.java",
      "app/src/main/java/acme/data/UserRepository.kt"
    ]) expect(chosen, excluded).not.toContain(excluded);
  });

  it("stellt die echte Oberfläche vor mitgelieferte Fremdinhalte", () => {
    // Aus einem echten Lauf: Der Launcher liefert Werkzeug-Profile mit Plugin-Marktplätzen mit.
    // Deren HTML gewann die Reihenfolge, weil „view" in „topology-viewer" traf — die sechs
    // XAML-Fenster der App standen dahinter und wären beim Budget-Deckel weggefallen.
    const chosen = reconstructionSourceFiles([
      entry("Profiles/ClaudeCode/standard/plugins/marketplaces/official/code-modernization/assets/topology-viewer.html"),
      entry("Profiles/ClaudeCode/strict/skills/skill-creator/assets/eval_review.html"),
      entry(".android-shield/l2_latin_fragments.py"),
      entry("add_report_dialog_strings.py"),
      entry("app/src/main/res/xml/network_security_config.xml"),
      entry("app/src/main/assets/legal/de/IMPRINT.html"),
      entry("MainWindow.xaml"),
      entry("Themes/DarkTheme.xaml"),
      entry("ViewModels/MainViewModel.cs")
    ]).map((file) => file.path);
    expect(chosen.slice(0, 2)).toEqual(["MainWindow.xaml", "Themes/DarkTheme.xaml"]);
    for (const excluded of [
      "Profiles/ClaudeCode/standard/plugins/marketplaces/official/code-modernization/assets/topology-viewer.html",
      "Profiles/ClaudeCode/strict/skills/skill-creator/assets/eval_review.html",
      ".android-shield/l2_latin_fragments.py",
      "add_report_dialog_strings.py",
      "app/src/main/res/xml/network_security_config.xml",
      "app/src/main/assets/legal/de/IMPRINT.html"
    ]) expect(chosen, excluded).not.toContain(excluded);
  });

  it("deckelt die Analyse und benennt, was nur über den Aufbau eingeht", () => {
    const many = Array.from({ length: 40 }, (_, index) => entry(`ui/Screen${index}.xaml`, 100_000));
    const { analyzed, skipped } = analysisBudget(many, 3, 200_000);
    expect(analyzed).toHaveLength(6);
    expect(skipped).toHaveLength(34);
    // Selbst eine einzelne riesige Datei wird analysiert statt still zu verschwinden.
    const huge = analysisBudget([entry("ui/Giant.xaml", 5_000_000)], 1, 200_000);
    expect(huge.analyzed).toHaveLength(1);
    expect(huge.skipped).toHaveLength(0);
  });
});
