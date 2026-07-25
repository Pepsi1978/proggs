import type { DesignFacts, FactScreen } from "./design-facts.js";

// Bisher entstand die gesamte App in EINEM KI-Aufruf: bei mehreren Bildschirmen lief der in die
// Ausgabegrenze, Screens fielen weg und Werte wurden „gerundet". Jetzt baut die KI jeden Screen
// einzeln (und parallel), und dieses Modul setzt sie deterministisch zu einer durchklickbaren
// Datei zusammen — ohne dass ein Modell dabei etwas weglassen kann.

export type ComposedScreen = { id: string; name: string; markup: string; isStart: boolean };

const escapeHtml = (value: string) => value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const escapeAttribute = (value: string) => escapeHtml(value).replace(/'/g, "&#39;");
export const screenSlug = (id: string) => id.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") || "screen";

// Die Theme-Variablen entstehen aus den geparsten Quellfarben, nicht aus einer KI-Antwort: dadurch
// sind Farben ueber alle Screens hinweg garantiert identisch und exakt.
export function themeStyles(facts: DesignFacts): string {
  // Projekte definieren oft mehrere Farbobjekte. Das umfangreichste Schema ist das echte Theme —
  // das erstgefundene waere haeufig nur eine kleine Zusatzpalette.
  const isDark = (theme: DesignFacts["themes"][number]) => /dark|night|dunkel/i.test(theme.id) || /dark|night|dunkel/i.test(theme.name);
  const largest = (candidates: DesignFacts["themes"]) => [...candidates].sort((left, right) => Object.keys(right.tokens).length - Object.keys(left.tokens).length)[0];
  const light = largest(facts.themes.filter((theme) => !isDark(theme))) ?? largest(facts.themes);
  const dark = largest(facts.themes.filter(isDark));
  const toVariables = (theme: typeof light) => theme ? Object.entries(theme.tokens).map(([token, value]) => `  --${token.replace(/[^\w-]/g, "-")}: ${value};`).join("\n") : "";
  const lightBlock = toVariables(light);
  const darkBlock = toVariables(dark);
  if (!lightBlock && !darkBlock) return "";
  return [
    lightBlock ? `:root {\n${lightBlock}\n}` : "",
    darkBlock ? `@media (prefers-color-scheme: dark) {\n  :root:not([data-theme="light"]) {\n${darkBlock.replace(/^/gm, "  ")}\n  }\n}` : "",
    darkBlock ? `:root[data-theme="dark"] {\n${darkBlock}\n}` : ""
  ].filter(Boolean).join("\n");
}

// Die Screen-Leiste ist bewusst ausserhalb der App-Flaeche und wird beim Export nicht mitgezaehlt:
// sie darf die Originalgeometrie nicht um einen einzigen Pixel verschieben.
const shellStyles = `
*, *::before, *::after { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
body { background: #f2f3f5; font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
.werft-screens { position: relative; }
.werft-screen { display: none; position: relative; overflow: hidden; }
.werft-screen[data-active="true"] { display: block; }
.werft-screen-switcher { position: fixed; left: 50%; bottom: 12px; transform: translateX(-50%); z-index: 2147483000; display: flex; gap: 4px; padding: 6px; max-width: calc(100vw - 24px); overflow-x: auto; background: rgba(20, 22, 28, 0.86); border-radius: 999px; backdrop-filter: blur(12px); box-shadow: 0 6px 24px rgba(0, 0, 0, 0.28); font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
.werft-screen-switcher button { flex: 0 0 auto; border: 0; cursor: pointer; padding: 6px 14px; border-radius: 999px; background: transparent; color: rgba(255, 255, 255, 0.72); font-size: 12px; line-height: 18px; white-space: nowrap; }
.werft-screen-switcher button[aria-current="true"] { background: #ffffff; color: #14161c; font-weight: 600; }
@media print { .werft-screen-switcher { display: none; } .werft-screen { display: block !important; } }
`.trim();

// Navigation im Original: jedes Element mit data-werft-navigate wechselt den Screen. Damit ist die
// Rekonstruktion wirklich durchklickbar statt nur ein Standbild.
const shellScript = `
(() => {
  const screens = [...document.querySelectorAll(".werft-screen")];
  if (!screens.length) return;
  const buttons = new Map();
  const show = (id) => {
    const target = screens.find((screen) => screen.dataset.screenId === id) ?? screens[0];
    for (const screen of screens) screen.dataset.active = String(screen === target);
    for (const [key, button] of buttons) button.setAttribute("aria-current", String(key === target.dataset.screenId));
    if (location.hash.slice(1) !== target.dataset.screenId) history.replaceState(null, "", "#" + target.dataset.screenId);
    window.scrollTo(0, 0);
    parent.postMessage({ source: "werft-preview-screen", screenId: target.dataset.screenId, screenName: target.dataset.screenName }, "*");
  };
  const bar = document.createElement("nav");
  bar.className = "werft-screen-switcher";
  bar.setAttribute("aria-label", "Bildschirme");
  for (const screen of screens) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = screen.dataset.screenName || screen.dataset.screenId;
    button.addEventListener("click", () => show(screen.dataset.screenId));
    buttons.set(screen.dataset.screenId, button);
    bar.append(button);
  }
  if (screens.length > 1) document.body.append(bar);
  document.addEventListener("click", (event) => {
    const trigger = event.target instanceof Element ? event.target.closest("[data-werft-navigate]") : null;
    if (!trigger) return;
    const target = trigger.getAttribute("data-werft-navigate");
    if (!target || !screens.some((screen) => screen.dataset.screenId === target)) return;
    event.preventDefault();
    show(target);
  });
  window.addEventListener("message", (event) => { if (event.data && event.data.source === "werft-studio-screen" && event.data.screenId) show(event.data.screenId); });
  window.addEventListener("hashchange", () => show(location.hash.slice(1)));
  show(location.hash.slice(1) || screens.find((screen) => screen.dataset.start === "true")?.dataset.screenId || screens[0].dataset.screenId);
})();
`.trim();

export type ComposeOptions = { title: string; platform: string; width: number; height: number; device: string; density: number; facts: DesignFacts; sharedCss?: string };

export function composeScreens(screens: ComposedScreen[], options: ComposeOptions): string {
  const preview = JSON.stringify({ platform: options.platform, width: options.width, height: options.height, device: options.device, density: options.density });
  const sections = screens.map((screen) => `<section class="werft-screen" data-screen-id="${escapeAttribute(screen.id)}" data-screen-name="${escapeAttribute(screen.name)}"${screen.isStart ? ' data-start="true"' : ""} style="width: ${options.width}px; min-height: ${options.height}px;">\n${screen.markup}\n</section>`).join("\n");
  return `<!doctype html>
<html lang="de">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="werft-preview" content='${preview.replace(/'/g, "&#39;")}'>
<title>${escapeHtml(options.title)}</title>
<style>
${shellStyles}
${themeStyles(options.facts)}
${options.sharedCss ?? ""}
</style>
</head>
<body>
<div class="werft-screens" style="width: ${options.width}px;">
${sections}
</div>
<script>
${shellScript}
</script>
</body>
</html>`;
}

// Die KI liefert pro Screen ein Fragment. Falls sie doch ein ganzes Dokument zurueckgibt, werden
// Rumpf und Stile herausgeloest, statt die Antwort zu verwerfen.
export function extractScreenFragment(response: string): { markup: string; css: string } {
  const cleaned = response.trim().replace(/^```(?:html)?\s*/i, "").replace(/\s*```$/, "");
  const styles = [...cleaned.matchAll(/<style\b[^>]*>([\s\S]*?)<\/style\s*>/gi)].map((match) => match[1]!.trim()).join("\n");
  const bodyMatch = /<body\b[^>]*>([\s\S]*?)<\/body\s*>/i.exec(cleaned);
  const body = bodyMatch ? bodyMatch[1]! : cleaned;
  const markup = body
    .replace(/<style\b[^>]*>[\s\S]*?<\/style\s*>/gi, "")
    .replace(/<!doctype html[^>]*>/gi, "")
    .replace(/<\/?(?:html|head|body)\b[^>]*>/gi, "")
    .replace(/<meta\b[^>]*>/gi, "")
    .replace(/<title\b[^>]*>[\s\S]*?<\/title\s*>/gi, "")
    .trim();
  return { markup, css: styles };
}

export const screenPlanFrom = (facts: DesignFacts, fallbackName: string): FactScreen[] =>
  facts.screens.length ? facts.screens : [{ id: "app", name: fallbackName, kind: "app", source: "—", navigatesTo: [], isStart: true, files: [] }];
