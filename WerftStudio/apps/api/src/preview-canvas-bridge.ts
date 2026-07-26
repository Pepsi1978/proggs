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

  // Alle Erscheinungen des Designs — Hell, Dunkel und eigene Farbthemes. Die Liste steht im Dokument
  // (Meta-Angabe des Aufbaus); fehlt sie, weil das Design vor dieser Fassung aufgebaut wurde, wird
  // sie aus den Stilregeln abgeleitet. So bleibt auch ein alter Aufbau umschaltbar.
  const themeSelectorPattern = /\\[data-werft-theme="([^"]+)"\\]/g;
  const styleRules = () => {
    const collected = [];
    try {
      for (const sheet of Array.prototype.slice.call(document.styleSheets)) {
        let rules = null;
        try { rules = sheet.cssRules; } catch (error) { continue; }
        for (const rule of Array.prototype.slice.call(rules || [])) collected.push(rule);
      }
    } catch (error) { /* Fremd-Stylesheets duerfen die Vorschau nicht anhalten */ }
    return collected;
  };
  const hasDarkTheme = () => styleRules().some((rule) => {
    const text = rule.selectorText || rule.conditionText || "";
    return text.indexOf('data-theme="dark"') >= 0 || text.indexOf("prefers-color-scheme: dark") >= 0;
  });
  // Die Vorschaufarbe wird am lebenden Dokument gemessen: kurz umschalten, Farbe lesen, zurueck.
  // Das passiert in einem einzigen Arbeitsschritt und ist deshalb nicht sichtbar.
  const measureThemeColor = (apply, restore) => {
    const root = document.documentElement;
    try {
      apply(root);
      const surface = getComputedStyle(document.body || root).backgroundColor;
      return surface && surface !== "rgba(0, 0, 0, 0)" && surface !== "transparent" ? surface : getComputedStyle(root).backgroundColor;
    } catch (error) { return ""; }
    finally { restore(root); }
  };
  const declaredThemes = () => {
    const meta = document.querySelector('meta[name="werft-themes"]');
    if (meta) {
      try {
        const parsed = JSON.parse(meta.getAttribute("content") || "[]");
        if (Array.isArray(parsed) && parsed.length) return parsed.filter((theme) => theme && typeof theme.id === "string");
      } catch (error) { /* fehlerhafte Angabe: unten wird abgeleitet */ }
    }
    return [];
  };
  const derivedThemes = () => {
    const ids = [];
    for (const rule of styleRules()) {
      const selector = rule.selectorText || "";
      if (!selector) continue;
      themeSelectorPattern.lastIndex = 0;
      let match = themeSelectorPattern.exec(selector);
      while (match) { if (ids.indexOf(match[1]) < 0) ids.push(match[1]); match = themeSelectorPattern.exec(selector); }
    }
    if (ids.length) return ids.map((id) => ({
      id,
      name: id,
      kind: /dark|night|dunkel|nacht/i.test(id) ? "dark" : /light|hell|day|tag/i.test(id) ? "light" : "other",
      color: measureThemeColor((root) => root.setAttribute("data-werft-theme", id), (root) => root.removeAttribute("data-werft-theme"))
    }));
    // Aelteste Fassung: nur ein Dunkel-Block ueber data-theme. Daraus werden zwei Erscheinungen.
    if (!hasDarkTheme()) return [];
    const previous = document.documentElement.getAttribute("data-theme");
    const restore = (root) => previous ? root.setAttribute("data-theme", previous) : root.removeAttribute("data-theme");
    return [
      { id: "light", name: "Hell", kind: "light", color: measureThemeColor((root) => root.setAttribute("data-theme", "light"), restore) },
      { id: "dark", name: "Dunkel", kind: "dark", color: measureThemeColor((root) => root.setAttribute("data-theme", "dark"), restore) }
    ];
  };
  let themeList = [];
  const readThemes = () => {
    const declared = declaredThemes();
    themeList = declared.length ? declared : derivedThemes();
    return themeList;
  };
  const currentThemeId = () => {
    const chosen = document.documentElement.getAttribute("data-werft-theme");
    if (chosen) return chosen;
    const legacy = document.documentElement.getAttribute("data-theme");
    if (legacy && themeList.some((theme) => theme.id === legacy)) return legacy;
    const wanted = legacy === "dark" ? "dark" : legacy === "light" ? "light" : "";
    const match = wanted ? themeList.filter((theme) => theme.kind === wanted)[0] : null;
    return match ? match.id : themeList.length ? themeList[0].id : "";
  };
  // Umgeschaltet wird am Dokument selbst — dadurch gilt die Wahl fuer ALLE Bildschirme zugleich und
  // bleibt beim Blaettern erhalten. Das Attribut data-theme wird mitgesetzt, damit die Regeln greifen, die
  // das Design selbst mitbringt.
  const applyTheme = (themeId) => {
    const theme = themeList.filter((entry) => entry.id === themeId)[0];
    if (!theme) return;
    const root = document.documentElement;
    root.setAttribute("data-werft-theme", theme.id);
    root.setAttribute("data-theme", theme.kind === "dark" ? "dark" : "light");
    root.style.setProperty("color-scheme", theme.kind === "dark" ? "dark" : "light");
    post({ action: "theme-changed", themeId: theme.id });
    measure();
  };
  const nextThemeId = () => {
    if (themeList.length < 2) return "";
    const index = themeList.map((theme) => theme.id).indexOf(currentThemeId());
    return themeList[(index + 1) % themeList.length].id;
  };

  // Die Regler arbeiten mit den Farbtoken, die der Aufbau AUS DEN PROJEKTQUELLEN erzeugt hat —
  // nicht mit erfundenen Reglern. Dadurch trifft jede Anpassung genau die Farbe, die das Design
  // wirklich verwendet, und wirkt auf allen Bildschirmen zugleich.
  const colorLike = /^(#[0-9a-fA-F]{3,8}|rgba?\\(|hsla?\\()/;
  const designTokens = () => {
    const found = {};
    try {
      for (const sheet of Array.prototype.slice.call(document.styleSheets)) {
        let rules = null;
        try { rules = sheet.cssRules; } catch (error) { continue; }
        for (const rule of Array.prototype.slice.call(rules || [])) {
          if (!rule.style || !rule.selectorText || rule.selectorText.indexOf(":root") < 0) continue;
          if (rule.selectorText.indexOf("data-theme") >= 0) continue;
          for (const name of Array.prototype.slice.call(rule.style)) {
            if (name.slice(0, 2) !== "--") continue;
            const value = String(rule.style.getPropertyValue(name)).trim();
            if (colorLike.test(value)) found[name] = value;
          }
        }
      }
    } catch (error) { /* ohne lesbare Stylesheets bleibt die Vorschau bedienbar */ }
    return found;
  };

  let tuneStyle = null;
  const applyTune = (overrides) => {
    if (!tuneStyle) {
      tuneStyle = document.createElement("style");
      tuneStyle.setAttribute("data-werft-tune", "");
      (document.head || document.documentElement).appendChild(tuneStyle);
    }
    const body = Object.keys(overrides || {}).map((name) => "  " + name + ": " + overrides[name] + ";").join("\\n");
    // Hoehere Spezifitaet als ":root", damit die Anpassung auch das Dunkel-Theme uebersteuert.
    tuneStyle.textContent = body ? ":root:root:root {\\n" + body + "\\n}" : "";
  };

  let themeStyleElement = null;
  // Nachgereichte Theme-Bloecke: ein Design, das vor dieser Fassung aufgebaut wurde, kennt seine
  // uebrigen Erscheinungen noch nicht. Das Studio liefert sie aus den Projektquellen nach, ohne dass
  // der teure Neuaufbau laufen muss.
  const applyThemeCss = (css) => {
    if (!themeStyleElement) {
      themeStyleElement = document.createElement("style");
      themeStyleElement.setAttribute("data-werft-themes", "");
      (document.head || document.documentElement).appendChild(themeStyleElement);
    }
    themeStyleElement.textContent = css || "";
  };

  // Ein Design, dessen Farben fest im Markup stehen statt in Variablen, laesst sich nicht umschalten.
  // Statt einen wirkungslosen Schalter anzubieten, wird das ehrlich gemeldet — dann weiss man, dass
  // ein Neuaufbau noetig ist.
  const themesEffective = () => {
    if (themeList.length < 2) return false;
    const root = document.documentElement;
    const previousTheme = root.getAttribute("data-werft-theme");
    const previousLegacy = root.getAttribute("data-theme");
    const sample = (theme) => {
      root.setAttribute("data-werft-theme", theme.id);
      root.setAttribute("data-theme", theme.kind === "dark" ? "dark" : "light");
      const style = getComputedStyle(document.body || root);
      return style.backgroundColor + "|" + style.color;
    };
    try { return sample(themeList[0]) !== sample(themeList[1]); }
    catch (error) { return true; }
    finally {
      if (previousTheme) root.setAttribute("data-werft-theme", previousTheme); else root.removeAttribute("data-werft-theme");
      if (previousLegacy) root.setAttribute("data-theme", previousLegacy); else root.removeAttribute("data-theme");
    }
  };

  const publishScreens = () => {
    const themes = readThemes();
    post({ action: "screens", screens: initialScreens, activeScreenId: screenIdOf(activeScreen()), frameMode, stageMode, hasDarkTheme: hasDarkTheme(), themes, activeThemeId: currentThemeId(), themesEffective: themesEffective(), tokens: designTokens() });
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

  // Das Rad allein scrollt — im Design dort, wo das Original scrollt, sonst wandert die Buehne.
  // Vergroessert wird ausschliesslich mit gedrueckter Strg-Taste (oder per Touchpad-Zoom, den der
  // Browser ebenfalls mit ctrlKey meldet).
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

  const send = (action, event) => post({ action, x: event.clientX, y: event.clientY, deltaX: event.deltaX || 0, deltaY: event.deltaY, ctrlKey: Boolean(event.ctrlKey) });
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
  // Text direkt im Design aendern: der alte und der neue Wortlaut stehen danach exakt fest, die
  // Aenderung braucht also keine KI. Bearbeitet wird der Textknoten UNTER DEM ZEIGER — auch wenn er
  // in einem Element mit Symbolen oder weiteren Kindern steht. Frueher war solcher Text unerreichbar
  // („Sitzung beginnen", „Pause", „Dauer"), weil nur Elemente ganz ohne Kindknoten zaehlten.
  let textMode = false;
  let editing = null;
  let editingWrapper = null;
  let editingBefore = "";
  let editingScreenId = "";
  const textNodeAt = (x, y) => {
    try {
      if (document.caretPositionFromPoint) {
        const position = document.caretPositionFromPoint(x, y);
        if (position && position.offsetNode && position.offsetNode.nodeType === 3) return position.offsetNode;
      }
      if (document.caretRangeFromPoint) {
        const range = document.caretRangeFromPoint(x, y);
        if (range && range.startContainer && range.startContainer.nodeType === 3) return range.startContainer;
      }
    } catch (error) { /* ohne Trefferpunkt wird gleich am Element gesucht */ }
    return null;
  };
  const ownTextNode = (element) => {
    if (!(element instanceof Element)) return null;
    for (const node of Array.prototype.slice.call(element.childNodes)) if (node.nodeType === 3 && node.nodeValue && node.nodeValue.trim()) return node;
    return null;
  };
  const editableTextNode = (event) => {
    const direct = textNodeAt(event.clientX, event.clientY);
    if (direct && direct.nodeValue && direct.nodeValue.trim()) return direct;
    let node = event.target instanceof Element ? event.target : null;
    while (node && node !== document.body && node !== document.documentElement) {
      const own = ownTextNode(node);
      if (own) return own;
      node = node.parentElement;
    }
    return null;
  };
  // Die Schreibmarke muss sich vom Hintergrund abheben: auf dunklem Grund hell, auf hellem dunkel.
  // Sonst tippt man blind, weil der blinkende Strich in der Flaeche verschwindet.
  const backgroundLuminance = (element) => {
    let node = element;
    while (node && node.nodeType === 1) {
      const parsed = /rgba?\\(([^)]+)\\)/.exec(getComputedStyle(node).backgroundColor || "");
      if (parsed) {
        const parts = parsed[1].split(",").map((part) => parseFloat(part));
        if (parts.length < 4 || parts[3] > 0.35) return (0.2126 * parts[0] + 0.7152 * parts[1] + 0.0722 * parts[2]) / 255;
      }
      node = node.parentElement;
    }
    return 1;
  };
  const finishTextEdit = () => {
    if (!editing) return;
    const element = editing;
    const wrapper = editingWrapper;
    const before = editingBefore;
    const screenId = editingScreenId;
    const after = (element.textContent || "").trim();
    editing = null;
    editingWrapper = null;
    element.removeAttribute("contenteditable");
    element.style.removeProperty("outline");
    element.style.removeProperty("caret-color");
    // Der Hilfsrahmen um den Text verschwindet wieder — das Design bleibt so aufgebaut wie zuvor.
    if (wrapper && wrapper.parentNode) {
      const parent = wrapper.parentNode;
      parent.replaceChild(document.createTextNode(wrapper.textContent || ""), wrapper);
      if (parent.normalize) parent.normalize();
    }
    if (after && after !== before) post({ action: "text-edit", before, after, screenId });
  };
  const startTextEdit = (node, x, y) => {
    const parent = node.parentElement;
    if (!parent) return;
    editingScreenId = screenIdOf(parent.closest(".werft-screen") || activeScreen());
    let target = parent;
    let wrapper = null;
    // Steht der Text neben Symbolen oder weiteren Knoten, wird NUR er in einen Hilfsrahmen gefasst.
    // Ohne ihn wuerde das Tippen die Nachbarelemente mitverschieben.
    if (parent.childNodes.length > 1) {
      wrapper = document.createElement("span");
      wrapper.setAttribute("data-werft-text", "");
      parent.replaceChild(wrapper, node);
      wrapper.appendChild(node);
      target = wrapper;
    }
    editing = target;
    editingWrapper = wrapper;
    editingBefore = (target.textContent || "").trim();
    target.setAttribute("contenteditable", "true");
    target.style.setProperty("outline", "2px solid #d97757", "important");
    target.style.setProperty("caret-color", backgroundLuminance(target) < 0.5 ? "#ffffff" : "#111111", "important");
    target.focus();
    // Die Einfuegemarke landet dort, wo geklickt wurde, statt an den Anfang zu springen.
    try {
      const selection = getSelection();
      const range = document.createRange();
      const position = document.caretPositionFromPoint ? document.caretPositionFromPoint(x, y) : null;
      if (position && position.offsetNode && target.contains(position.offsetNode)) range.setStart(position.offsetNode, position.offset);
      else { range.selectNodeContents(target); range.collapse(false); }
      range.collapse(true);
      selection.removeAllRanges();
      selection.addRange(range);
    } catch (error) { /* ohne genaue Marke bleibt der Text trotzdem bearbeitbar */ }
  };
  // Im Stiftmodus darf KEIN Bedienelement mehr schalten: frueher kippte ein Schalter, sobald man
  // nur seinen Text treffen wollte. Nur innerhalb des gerade bearbeiteten Textes arbeitet die Maus
  // normal weiter — dort setzt sie die Schreibmarke und markiert.
  const insideEditing = (target) => Boolean(editing && target instanceof Node && (editing === target || editing.contains(target)));
  const blockWhileEditing = (event) => {
    if (!textMode || insideEditing(event.target)) return;
    event.preventDefault();
    event.stopImmediatePropagation();
  };
  for (const type of ["pointerdown", "mousedown", "pointerup", "mouseup", "dblclick", "change", "submit"]) document.addEventListener(type, blockWhileEditing, true);
  document.addEventListener("click", (event) => {
    if (!textMode) return;
    if (insideEditing(event.target)) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const node = editableTextNode(event);
    finishTextEdit();
    if (node) startTextEdit(node, event.clientX, event.clientY);
  }, true);
  document.addEventListener("keydown", (event) => {
    if (!editing) return;
    if (event.key === "Enter") { event.preventDefault(); finishTextEdit(); }
    if (event.key === "Escape") { editing.textContent = editingBefore; finishTextEdit(); }
  }, true);
  document.addEventListener("focusout", () => { if (editing) finishTextEdit(); }, true);

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

  // Ein Mond- oder Sonnensymbol im Design SOLL das Design umschalten. Viele Rekonstruktionen zeigen
  // das Symbol, ohne dass dahinter etwas passiert. Erkannt wird es an Beschriftung, Titel, Klasse
  // oder Kennung — und geschaltet wird nur, wenn das Design nicht selbst schon reagiert hat.
  const themeWords = /theme|dark|light|mond|moon|sonne|sun|nacht|night|tag\\b|day|darstellung|appearance|erscheinung|hell|dunkel/i;
  const looksLikeThemeToggle = (node) => {
    if (!(node instanceof Element)) return false;
    if (node.hasAttribute && node.hasAttribute("data-werft-theme-toggle")) return true;
    if (node.hasAttribute && node.hasAttribute("data-werft-navigate")) return false;
    const text = (node.textContent || "").trim();
    if (text.length > 28) return false;
    const className = typeof node.className === "string" ? node.className : node.className && node.className.baseVal ? node.className.baseVal : "";
    const haystack = [node.getAttribute("aria-label"), node.getAttribute("title"), node.getAttribute("data-testid"), node.id, className, text].filter(Boolean).join(" ");
    return themeWords.test(haystack);
  };
  document.addEventListener("click", (event) => {
    if (markMode || textMode || themeList.length < 2) return;
    let node = event.target instanceof Element ? event.target : null;
    let found = null;
    for (let depth = 0; node && depth < 4 && !found; depth += 1) { if (looksLikeThemeToggle(node)) found = node; node = node.parentElement; }
    if (!found) return;
    const before = currentThemeId();
    // Erst nachsehen, ob das Design selbst umschaltet: sonst kaeme der Wechsel doppelt und bliebe
    // sichtbar auf dem alten Stand stehen.
    setTimeout(() => { if (currentThemeId() === before) applyTheme(nextThemeId()); }, 90);
  }, false);

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
    if (data.action === "tune") applyTune(data.overrides);
    if (data.action === "theme-css") { applyThemeCss(typeof data.css === "string" ? data.css : ""); publishScreens(); }
    if (data.action === "theme") {
      if (typeof data.themeId === "string" && data.themeId) applyTheme(data.themeId);
      else if (data.theme === "light" || data.theme === "dark") {
        const match = themeList.filter((theme) => theme.kind === data.theme)[0];
        if (match) applyTheme(match.id);
        else { document.documentElement.setAttribute("data-theme", data.theme); measure(); }
      }
    }
    if (data.action === "mark") {
      markMode = Boolean(data.on);
      clearMarkHover();
      document.documentElement.style.cursor = markMode ? "crosshair" : "";
    }
    if (data.action === "text") {
      textMode = Boolean(data.on);
      if (!textMode) finishTextEdit();
      document.documentElement.style.cursor = textMode ? "text" : "";
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
