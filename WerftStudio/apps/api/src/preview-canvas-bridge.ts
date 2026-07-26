const bridgeMarker = "data-werft-canvas-bridge";

// Die Bruecke wird bei JEDER Auslieferung frisch eingesetzt — nie mitgespeichert. Dadurch bekommt
// auch ein laengst importiertes Projekt sofort die aktuelle Leinwand-Bedienung, ohne dass die teure
// KI-Rekonstruktion erneut laufen muss.
const bridgeScript = `<script ${bridgeMarker}>
(() => {
  const params = new URLSearchParams(location.search);
  const frameMode = params.get("werftFrame") === "1";
  // Buehnen-Modus: das Design steht allein und wird ECHT durchgeklickt — keine Leiste, keine
  // abgefangenen Klicks. Genau so, wie die App auf dem Geraet startet.
  const stageMode = params.get("werftStage") === "1";
  const chromeless = frameMode || stageMode;
  const rawWanted = params.get("werftScreen") || location.hash.slice(1) || "";
  let wantedScreen = rawWanted;
  try { wantedScreen = decodeURIComponent(rawWanted); } catch (error) { wantedScreen = rawWanted; }
  const post = (payload) => { try { parent.postMessage(Object.assign({ source: "werft-preview-canvas" }, payload), "*"); } catch (error) { /* Die Vorschau bleibt auch ohne Studio bedienbar. */ } };
  const screensOf = () => Array.prototype.slice.call(document.querySelectorAll(".werft-screen"));
  const activeScreen = () => screensOf().filter((screen) => screen.dataset.active === "true")[0] || screensOf()[0] || null;
  const screenIdOf = (element) => element && element.dataset ? element.dataset.screenId || "" : "";
  const navigationTargets = (screen) => Array.prototype.slice.call((screen || document).querySelectorAll("[data-werft-navigate]")).map((node) => node.getAttribute("data-werft-navigate")).filter(Boolean);

  // Ohne diese Stile wirkt jedes verknuepfte Element wie ein totes Bild: der Zeiger verraet nicht,
  // dass dahinter ein weiterer Bildschirm liegt.
  const style = document.createElement("style");
  style.setAttribute("data-werft-canvas-style", "");
  style.textContent = [
    "[data-werft-navigate] { cursor: pointer; }",
    "[data-werft-navigate]:hover { outline: 2px solid rgba(217, 119, 87, 0.55); outline-offset: 1px; }",
    "html[data-werft-highlight=\\"on\\"] [data-werft-navigate] { outline: 2px solid #d97757; outline-offset: 1px; background-image: linear-gradient(rgba(217, 119, 87, 0.14), rgba(217, 119, 87, 0.14)); }",
    chromeless ? "html, body { overflow: hidden !important; }" : "",
    chromeless ? ".werft-screen-switcher { display: none !important; }" : "",
    frameMode ? "html[data-werft-frame=\\"single\\"] .werft-screen { display: block !important; }" : ""
  ].filter(Boolean).join("\\n");
  (document.head || document.documentElement).appendChild(style);

  const showScreen = (screenId) => {
    if (!screenId) return;
    // Der Bildschirmwechsel laeuft ueber die im Dokument eingebaute Schaltlogik: sie kennt Verlauf,
    // Ankerlink und Beschriftung. Die Bruecke bittet nur darum, statt eine zweite Wahrheit zu bauen.
    try { window.postMessage({ source: "werft-studio-screen", screenId }, "*"); } catch (error) { /* ohne Schaltlogik bleibt der einzige Bildschirm stehen */ }
  };

  const measure = () => {
    const screen = activeScreen();
    const element = screen || document.body;
    if (!element) return;
    const rect = element.getBoundingClientRect();
    const width = Math.round(Math.max(rect.width, element.scrollWidth || 0, document.documentElement.scrollWidth || 0));
    const height = Math.round(Math.max(rect.height, element.scrollHeight || 0));
    if (!width || !height) return;
    post({ action: "size", width, height, screenId: screenIdOf(screen) });
  };

  // Die Bildschirme heissen intern z. B. "compose:HistoryScreen"; verlinkt wird im Design aber oft
  // die Kurzform "HistoryScreen". Ohne diese Zuordnung findet der Klick sein Ziel nicht — das Design
  // wirkt dann wie ein Standbild, obwohl alle Verknuepfungen vorhanden sind. Die Reparatur laeuft
  // beim Ausliefern und wirkt damit auch fuer laengst aufgebaute Designs.
  const simplify = (text) => String(text).toLowerCase().replace(/^[a-z]+:/, "").replace(/[^a-z0-9]+/g, "");
  const repairNavigationTargets = () => {
    const all = screensOf();
    if (!all.length) return;
    for (const node of Array.prototype.slice.call(document.querySelectorAll("[data-werft-navigate]"))) {
      const target = (node.getAttribute("data-werft-navigate") || "").trim();
      if (!target || all.some((screen) => screenIdOf(screen) === target)) continue;
      const wanted = simplify(target);
      const match = all.filter((screen) => screenIdOf(screen).endsWith(":" + target))[0]
        || all.filter((screen) => (screen.dataset.screenName || "") === target)[0]
        || all.filter((screen) => simplify(screenIdOf(screen)) === wanted)[0]
        || all.filter((screen) => simplify(screen.dataset.screenName || "") === wanted)[0];
      if (match) node.setAttribute("data-werft-navigate", screenIdOf(match));
    }
  };
  repairNavigationTargets();

  // Die Bildschirmliste wird EINMAL erfasst und danach unveraendert gemeldet: im Rahmen-Modus
  // bleibt spaeter nur noch der eigene Bildschirm im Dokument stehen, die Liste muss aber
  // vollstaendig bleiben — sonst haette das Studio nach dem Aufraeumen nur noch einen Bildschirm.
  const initialScreens = screensOf().map((screen) => ({
    id: screenIdOf(screen),
    name: screen.dataset.screenName || screenIdOf(screen),
    isStart: screen.dataset.start === "true",
    links: navigationTargets(screen)
  })).filter((screen) => screen.id);

  // Der Aufbau erzeugt das Dunkel-Theme als ":root[data-theme=dark]" — gesetzt hat das Attribut
  // aber nie jemand, deshalb liess sich der Hell-Dunkel-Modus im Design nicht umschalten.
  // Gemeldet wird nur, ob es ueberhaupt ein zweites Theme gibt; sonst waere der Schalter wirkungslos.
  const hasDarkTheme = () => {
    try {
      for (const sheet of Array.prototype.slice.call(document.styleSheets)) {
        let rules = null;
        try { rules = sheet.cssRules; } catch (error) { continue; }
        for (const rule of Array.prototype.slice.call(rules || [])) {
          const text = rule.selectorText || rule.conditionText || "";
          if (text.indexOf('data-theme="dark"') >= 0 || text.indexOf("prefers-color-scheme: dark") >= 0) return true;
        }
      }
    } catch (error) { /* Fremd-Stylesheets duerfen die Vorschau nicht anhalten */ }
    return false;
  };

  const publishScreens = () => {
    post({ action: "screens", screens: initialScreens, activeScreenId: screenIdOf(activeScreen()), frameMode, stageMode, hasDarkTheme: hasDarkTheme() });
    measure();
  };

  // Ein Rahmen zeigt genau einen Bildschirm. Alle uebrigen Sektionen aus dem Dokument zu nehmen
  // spart bei 18 Bildschirmen 18-mal so viele Knoten je Rahmen — sonst wird die Leinwand zaeh.
  const pruneToActiveScreen = () => {
    if (!frameMode) return;
    // Behalten wird der ANGEFORDERTE Bildschirm — nicht der gerade aktive. Sonst haenge das
    // Ergebnis davon ab, ob die Schaltlogik des Dokuments schon umgeschaltet hat.
    const keep = (wantedScreen ? screensOf().filter((screen) => screenIdOf(screen) === wantedScreen)[0] : null) || activeScreen();
    if (!keep) return;
    for (const screen of screensOf()) if (screen !== keep) screen.remove();
    keep.dataset.active = "true";
    // Ab jetzt bleibt dieser Bildschirm sichtbar, egal was die Schaltlogik danach noch versucht.
    document.documentElement.setAttribute("data-werft-frame", "single");
  };

  // Ein Rad-Dreh soll die Leinwand vergroessern — ausser der Zeiger steht ueber einem Bereich, der
  // im Original wirklich scrollt. Sonst waere ein langer Bildschirm im Rahmen nicht mehr lesbar.
  const scrollableUnder = (target, deltaY) => {
    let node = target instanceof Element ? target : null;
    while (node && node !== document.body && node !== document.documentElement) {
      const style = getComputedStyle(node);
      const scrolls = /(auto|scroll|overlay)/.test(style.overflowY);
      if (scrolls && node.scrollHeight > node.clientHeight + 1) {
        if (deltaY < 0 && node.scrollTop > 0) return true;
        if (deltaY > 0 && node.scrollTop + node.clientHeight < node.scrollHeight - 1) return true;
      }
      node = node.parentElement;
    }
    return false;
  };

  const send = (action, event) => post({ action, x: event.clientX, y: event.clientY, deltaY: event.deltaY });
  let panning = false;
  document.addEventListener("wheel", (event) => {
    if (!event.ctrlKey && scrollableUnder(event.target, event.deltaY)) return;
    event.preventDefault();
    send("wheel", event);
  }, { capture: true, passive: false });
  document.addEventListener("pointerdown", (event) => {
    if (event.button !== 1) return;
    event.preventDefault();
    panning = true;
    document.documentElement.setPointerCapture(event.pointerId);
    document.documentElement.style.cursor = "grabbing";
    send("pan-start", event);
  }, true);
  document.addEventListener("pointermove", (event) => {
    if (!panning) return;
    event.preventDefault();
    send("pan-move", event);
  }, true);
  const endPan = (event) => {
    if (!panning) return;
    panning = false;
    if (document.documentElement.hasPointerCapture(event.pointerId)) document.documentElement.releasePointerCapture(event.pointerId);
    document.documentElement.style.cursor = "";
    send("pan-end", event);
  };
  document.addEventListener("pointerup", endPan, true);
  document.addEventListener("pointercancel", endPan, true);
  document.addEventListener("auxclick", (event) => { if (event.button === 1) event.preventDefault(); }, true);

  // Markieren: im Kommentarmodus wird das Element unter dem Zeiger umrandet und beim Klick mit
  // seinem WOERTLICHEN Ausschnitt gemeldet. Nur so trifft ein Aenderungswunsch spaeter genau das
  // gemeinte Element statt irgendeines gleichnamigen.
  let markMode = false;
  let markHover = null;
  const clearMarkHover = () => { if (markHover) markHover.style.removeProperty("outline"); markHover = null; };
  // Markiert wird genau das Element unter dem Zeiger; Seite und Body selbst sind kein Bereich.
  const markableUnder = (node) => node instanceof Element && node !== document.body && node !== document.documentElement ? node : null;
  const selectorFor = (element) => {
    const parts = [];
    let node = element;
    while (node && node.nodeType === 1 && parts.length < 8) {
      if (node.classList && node.classList.contains("werft-screen")) { parts.unshift('[data-screen-id="' + (node.dataset.screenId || "") + '"]'); break; }
      let part = node.tagName.toLowerCase();
      if (node.id) { parts.unshift(part + "#" + node.id); break; }
      const parent = node.parentElement;
      if (parent) {
        const same = Array.prototype.filter.call(parent.children, (child) => child.tagName === node.tagName);
        if (same.length > 1) part += ":nth-of-type(" + (same.indexOf(node) + 1) + ")";
      }
      parts.unshift(part);
      node = parent;
    }
    return parts.join(" > ");
  };
  const labelFor = (element) => {
    const tag = element.tagName.toLowerCase();
    const cls = element.classList && element.classList.length ? "." + Array.prototype.slice.call(element.classList).slice(0, 2).join(".") : "";
    const text = (element.textContent || "").trim().replace(/\\s+/g, " ").slice(0, 40);
    return tag + cls + (text ? ' \\u201E' + text + '\\u201C' : "");
  };
  document.addEventListener("pointermove", (event) => {
    if (!markMode) return;
    const element = markableUnder(event.target);
    if (element === markHover) return;
    clearMarkHover();
    if (!element) return;
    markHover = element;
    element.style.setProperty("outline", "2px solid #d97757", "important");
  }, true);
  document.addEventListener("click", (event) => {
    if (!markMode) return;
    const element = markableUnder(event.target);
    if (!element) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const rect = element.getBoundingClientRect();
    const screen = element.closest(".werft-screen") || activeScreen();
    post({
      action: "mark-target",
      rect: { x: Math.round(rect.left + window.scrollX), y: Math.round(rect.top + window.scrollY), width: Math.round(rect.width), height: Math.round(rect.height) },
      selector: selectorFor(element),
      label: labelFor(element),
      html: (element.outerHTML || "").slice(0, 8000),
      screenId: screenIdOf(screen),
      screenName: screen && screen.dataset ? screen.dataset.screenName || "" : ""
    });
  }, true);

  // Jeder Klick auf eine Verknuepfung wird gemeldet: das Studio kann daraufhin den Zielbildschirm
  // zeigen. Im Rahmen auf der Leinwand bleibt der Bildschirm selbst stehen, damit die Uebersicht
  // nicht unter dem Zeiger wegspringt.
  document.addEventListener("click", (event) => {
    const element = event.target instanceof Element ? event.target : null;
    const trigger = element ? element.closest("[data-werft-navigate]") : null;
    if (!trigger) {
      if (frameMode) post({ action: "focus", screenId: screenIdOf(activeScreen()) });
      return;
    }
    const target = trigger.getAttribute("data-werft-navigate");
    post({ action: "navigate", to: target, from: screenIdOf(activeScreen()) });
    if (!frameMode) return;
    event.preventDefault();
    event.stopPropagation();
  }, true);

  window.addEventListener("message", (event) => {
    const data = event.data;
    if (!data || data.source !== "werft-studio-canvas") return;
    if (data.action === "highlight") document.documentElement.setAttribute("data-werft-highlight", data.on ? "on" : "off");
    if (data.action === "screen") showScreen(data.screenId);
    if (data.action === "measure") measure();
    if (data.action === "theme" && (data.theme === "light" || data.theme === "dark")) {
      document.documentElement.setAttribute("data-theme", data.theme);
      measure();
    }
    if (data.action === "mark") {
      markMode = Boolean(data.on);
      clearMarkHover();
      document.documentElement.style.cursor = markMode ? "crosshair" : "";
    }
  });

  const observer = new MutationObserver(() => { measure(); });
  const watched = document.querySelector(".werft-screens") || document.body;
  if (watched) observer.observe(watched, { attributes: true, attributeFilter: ["data-active"], subtree: true, childList: true });
  if (typeof ResizeObserver === "function" && document.body) new ResizeObserver(() => measure()).observe(document.body);
  addEventListener("load", () => { publishScreens(); });
  addEventListener("resize", () => measure());

  if (wantedScreen) showScreen(wantedScreen);
  publishScreens();
  setTimeout(() => { pruneToActiveScreen(); publishScreens(); }, 120);
  setTimeout(publishScreens, 400);
})();
</script>`;

const normalizedBase = (previewBase: string) => previewBase.endsWith("/") ? previewBase : `${previewBase}/`;

export function rewriteRootRelativeCss(css: string, previewBase: string): string {
  const base = normalizedBase(previewBase);
  return css.replace(/(url\(\s*["']?)\/(?!\/)/gi, `$1${base}`);
}

export function rewriteRootRelativeJavaScript(code: string, previewBase: string): string {
  const base = normalizedBase(previewBase);
  return code
    .replace(/(\bfrom\s*["'])\/(?!\/)/g, `$1${base}`)
    .replace(/(\bimport\s*["'])\/(?!\/)/g, `$1${base}`)
    .replace(/(\bimport\s*\(\s*["'])\/(?!\/)/g, `$1${base}`);
}

export function rewriteRootRelativeAssets(html: string, previewBase: string): string {
  const base = normalizedBase(previewBase);
  return html
    .replace(/<[^>]+>/g, (tag) => tag
      .replace(/(\b(?:src|href|poster|action)\s*=\s*["'])\/(?!\/)/gi, `$1${base}`)
      .replace(/(\bsrcset\s*=\s*)(["'])(.*?)\2/gi, (_all, prefix: string, quote: string, value: string) => `${prefix}${quote}${value.split(",").map((candidate) => candidate.trim().replace(/^\/(?!\/)/, base)).join(", ")}${quote}`)
      .replace(/(\bstyle\s*=\s*)(["'])(.*?)\2/gi, (_all, prefix: string, quote: string, value: string) => `${prefix}${quote}${rewriteRootRelativeCss(value, base)}${quote}`))
    .replace(/(<style\b[^>]*>)([\s\S]*?)(<\/style\s*>)/gi, (_all, open: string, css: string, close: string) => `${open}${rewriteRootRelativeCss(css, base)}${close}`)
    .replace(/(<script\b[^>]*>)([\s\S]*?)(<\/script\s*>)/gi, (_all, open: string, code: string, close: string) => `${open}${rewriteRootRelativeJavaScript(code, base)}${close}`);
}

export function injectPreviewCanvasBridge(html: string, previewBase?: string): string {
  const withAssets = previewBase ? rewriteRootRelativeAssets(html, previewBase) : html;
  if (withAssets.includes(bridgeMarker)) return withAssets;
  const bodyEnd = withAssets.search(/<\/body\s*>/i);
  if (bodyEnd >= 0) return `${withAssets.slice(0, bodyEnd)}${bridgeScript}${withAssets.slice(bodyEnd)}`;
  return `${withAssets}${bridgeScript}`;
}
