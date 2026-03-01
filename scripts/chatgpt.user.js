// ==UserScript==
// @name         ChatGPT V.1.2.2
// @namespace    https://chatgpt.com/
// @version      1.2.2
// @description  Speech-to-Text + Gemini-Diktat-Bereinigung (DE) auf ChatGPT. Mic-Button unten rechts. Zwei Prompt-Builder Buttons (Frank + für jedermann) über dem Mic. Memory-Button links neben dem Mic. Kein stilles Fallback. Mit Output-Preview. Fix: kein "SelectAll" auf ganzer Seite + Memory/Builder immer ins Composer-Feld.
// @match        https://chatgpt.com/*
// @match        https://chat.openai.com/*
// @run-at       document-idle
// @grant        GM_xmlhttpRequest
// @grant        GM.xmlHttpRequest
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_registerMenuCommand
// @connect      generativelanguage.googleapis.com
// @connect      *.googleapis.com
// @connect      googleapis.com
// @connect      api.groq.com
// @updateURL    https://raw.githubusercontent.com/Pepsi1978/tampermonkey-skripte/main/scripts/chatgpt.user.js
// @downloadURL  https://raw.githubusercontent.com/Pepsi1978/tampermonkey-skripte/main/scripts/chatgpt.user.js
// ==/UserScript==

(() => {
  "use strict";
    // ── CSS für Mikrofon-Button Animationen ──
    (function(){if(document.getElementById("stt-mic-css"))return;var s=document.createElement("style");s.id="stt-mic-css";s.textContent=".stt-mic-btn{display:flex!important;align-items:center!important;justify-content:center!important;padding:0!important;transition:background .25s,transform .15s,box-shadow .25s!important}.stt-mic-btn:active{transform:scale(.93)!important}.stt-mic-btn[data-state=idle]{background:#2563eb!important;color:#fff!important;border-color:#2563eb!important}.stt-mic-btn[data-state=idle]:hover{background:#1d4ed8!important;transform:scale(1.06)!important}.stt-mic-btn[data-state=listening]{background:#dc2626!important;color:#fff!important;border-color:#dc2626!important;animation:stt-pulse 1.4s ease-in-out infinite!important}.stt-mic-btn[data-state=working]{background:#d97706!important;color:#fff!important;border-color:#d97706!important}.stt-mic-btn[data-state=error]{background:#8b0000!important;color:#fff!important;border-color:#8b0000!important}@keyframes stt-pulse{0%,100%{box-shadow:0 0 0 0 rgba(220,38,38,.45)}50%{box-shadow:0 0 0 14px rgba(220,38,38,0)}}.stt-mic-btn[data-state=working] svg{animation:stt-spin .8s linear infinite}@keyframes stt-spin{to{transform:rotate(360deg)}}";(document.head||document.documentElement).appendChild(s)})();


  // ============================================================
  // 🔑 GEMINI KEY (lokal in Tampermonkey speichern, NICHT im Code)
  // ============================================================
  // ⚠️ WICHTIG: Deinen API-Key niemals irgendwo öffentlich posten.
  // Er wird NUR lokal in Tampermonkey gespeichert und bei Updates NICHT überschrieben.

  function _tmGetValue(name, fallback) {
    try {
      if (typeof GM_getValue === "function") return GM_getValue(name, fallback);
      if (typeof GM !== "undefined" && typeof GM.getValue === "function") {
        // GM.getValue ist async (Promise). Hier nicht verwenden, um den Rest nicht umzubauen.
        return fallback;
      }
    } catch {}
    return fallback;
  }

  function _tmSetValue(name, value) {
    try {
      if (typeof GM_setValue === "function") return GM_setValue(name, value);
      if (typeof GM !== "undefined" && typeof GM.setValue === "function") {
        // GM.setValue ist async (Promise). Hier nicht verwenden.
        return;
      }
    } catch {}
  }

  function getGeminiKey() {
    let key = _tmGetValue("geminiKey", "") || "";
    key = String(key).trim();
    if (!key) {
      key = (prompt("Bitte Gemini API-Key eingeben (wird nur in Tampermonkey gespeichert):") || "").trim();
      if (key) _tmSetValue("geminiKey", key);
    }
    return key;
  }

  function setGeminiKey() {
    let key = (prompt("Neuen Gemini API-Key eingeben:") || "").trim();
    if (key) {
      _tmSetValue("geminiKey", key);
      alert("✅ Gemini API-Key gespeichert.");
    } else {
      alert("⚠️ Kein Key eingegeben.");
    }
  }

  function clearGeminiKey() {
    _tmSetValue("geminiKey", "");
    alert("🧹 Gemini API-Key gelöscht.");
  }

  // ============================================================
  // 🔑 GROQ KEY (für Whisper Speech-to-Text)
  // ============================================================
  function getGroqKey() {
    let key = _tmGetValue("groqKey", "") || "";
    key = String(key).trim();
    if (!key) {
      key = (prompt("Bitte Groq API-Key eingeben (kostenlos auf groq.com, wird nur in Tampermonkey gespeichert):") || "").trim();
      if (key) _tmSetValue("groqKey", key);
    }
    return key;
  }

  function setGroqKey() {
    let key = (prompt("Neuen Groq API-Key eingeben:") || "").trim();
    if (key) {
      _tmSetValue("groqKey", key);
      alert("✅ Groq API-Key gespeichert.");
    } else {
      alert("⚠️ Kein Key eingegeben.");
    }
  }

  function clearGroqKey() {
    _tmSetValue("groqKey", "");
    alert("🧹 Groq API-Key gelöscht.");
  }

  (function registerMenus() {
    try {
      if (typeof GM_registerMenuCommand === "function") {
        GM_registerMenuCommand("🔑 Gemini-Key setzen/ändern", setGeminiKey);
        GM_registerMenuCommand("🧹 Gemini-Key löschen", clearGeminiKey);
        GM_registerMenuCommand("🎙️ Groq-Key setzen/ändern", setGroqKey);
        GM_registerMenuCommand("🗑️ Groq-Key löschen", clearGroqKey);
      } else if (typeof GM !== "undefined" && typeof GM.registerMenuCommand === "function") {
        GM.registerMenuCommand("🔑 Gemini-Key setzen/ändern", setGeminiKey);
        GM.registerMenuCommand("🧹 Gemini-Key löschen", clearGeminiKey);
        GM.registerMenuCommand("🎙️ Groq-Key setzen/ändern", setGroqKey);
        GM.registerMenuCommand("🗑️ Groq-Key löschen", clearGroqKey);
      }
    } catch {}
  })();

  // ============================================================
  // Modell
  // ============================================================
  const GEMINI_MODEL = "models/gemini-2.5-flash-lite";

  // ============================================================
  // 🆔 UI IDs (für Selbstheilung / Dedupe)
  // ============================================================
  const UI_IDS = {
    toast: "tm_chatgpt_toast",
    mic: "tm_chatgpt_btn_mic",
    mem: "tm_chatgpt_btn_mem",
    clear: "tm_chatgpt_btn_clear",
    promptFrank: "tm_chatgpt_btn_prompt_frank",
    promptGeneral: "tm_chatgpt_btn_prompt_general"
  };

  // ============================================================
  // 💾 MEMORY PROMPT (mit Deduplizierung/Updates)
  // ============================================================
  const MEMORY_PROMPT = `
Aufgabe: Lies den gesamten Chatverlauf und extrahiere nur neue, stabile Informationen über mich, die zukünftige Antworten deutlich verbessern.

Wichtig: Nichts automatisch speichern – nur Vorschläge machen.

Deduplizierung / Updates (sehr wichtig):
- Prüfe zuerst alle bereits gespeicherten Erinnerungen über mich (Memory/Bio), damit du keine doppelten oder überschneidenden Punkte vorschlägst.
- Wenn eine neue Information eine bestehende Erinnerung klar verbessert (präziser, vollständiger, aktueller), schlage eine Aktualisierung vor statt einen neuen Punkt zu erzeugen.
- Wenn etwas bereits vollständig vorhanden ist, lasse es weg.

Regeln:
- Nur Dinge, die voraussichtlich mindestens 3 Monate gültig sind.
- Maximal 12 Punkte.
- Jeder Punkt: 1 einfacher Satz, ohne Fachwörter.
- Keine Wiederholungen, keine Tagesform, keine Kleinigkeiten.

Was du suchen sollst (Priorität):
- Ziele & Prioritäten
- Präferenzen (wie ich Antworten/Tools möchte)
- Persönlichkeit & wiederkehrende Verhaltensmuster (nur wenn mehrfach erkennbar)
- Wiederkehrende Arbeitsweise mit KI (wie ich iteriere/entscheide)
- Langfristige Projekte

Ausgabeformat:
- Liste „Punkt 1 … Punkt N“
- Pro Punkt zusätzlich in Klammern: „nützlich weil …“ und „sensibel: ja/nein“.
- Optional (sehr empfehlenswert): „Beleg: im Chat mehrfach erwähnt“ (keine Zitate nötig)

Nachdem ich dir die Punkte 1–N vorgeschlagen habe, nutzt du diesen Prompt:

Ich wähle folgende Punkte zum dauerhaften Speichern aus: [hier nur die Nummern eintragen].
Speichere nur diese Punkte als dauerhafte Erinnerungen, exakt als einfache Sätze. Starte jede neu gespeicherte dauerhafte Erinnerung mit : "Frank"... (Beispiel: Frank mag Kiefernwälder.)
`.trim();

  // ============================================================
  // UI POSITION
  // ============================================================
  const UI_POS = { rightPx: 16, bottomPx: 16 };
  const UI_SHIFT_LEFT = "3mm"; // ✅ Buttons ~3mm nach links (mehr Abstand zur Scrollbar)
  // Android/Edge Mobile-Erkennung (für angepasste Restart-Delays)
  const isMobileAndroid = /Android/i.test(navigator.userAgent);


  const CFG = {
    minCharsForRewrite: 6,

    requestTimeoutMs: 120000, // 120s

    postPasteDelayMs: 90,
    reactNudgeDelayMs: 70,

    maxRetries: 4,
    retryWaitMs: 1200,

    previewChars: 140,

    // Diktat-Bereinigung / Grammatik (nur noch für Prompt-Builder)
    grammarMaxOutputTokens: 8192,
    grammarChunkChars: 3500,
    grammarTruncationRatio: 0.85,
    dictationCleanupMode: "balanced",

    // Groq Whisper Speech-to-Text
    whisperModel: "whisper-large-v3",
    whisperLang: "de"
  };

  // ============================================================
  // DOMÄNEN-FINDER TUNING
  // ============================================================
  const DOMAIN_CFG = {
    minDomains: 7,
    maxDomains: 10,

    temperature: 0.10,
    maxOutputTokens: 1024,
    repairMaxOutputTokens: 1024
  };

  const supportedSpeech = !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);

  // ── SVG-Icons für Mikrofon-Button (Stil: claude-code-spracheingabe) ──
  const MIC_ICON = {
    mic: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>',
    stop: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>',
    spinner: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 2a10 10 0 0 1 10 10"/></svg>',
    error: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  };

  // ============================================================
  // Toast (robust: ggf. neu anlegen, falls ChatGPT DOM neu baut)
  // ============================================================
  let toast = null;
  let toastTimer = null;

  function ensureToast() {
    let t = document.getElementById(UI_IDS.toast);
    if (!t) {
      t = document.createElement("div");
      t.id = UI_IDS.toast;
      document.body.appendChild(t);
    }
    toast = t;

    toast.style.position = "fixed";
    toast.style.bottom = "14px";
    toast.style.left = "14px";
    toast.style.zIndex = "9999999";
    toast.style.maxWidth = "760px";
    toast.style.padding = "10px 12px";
    toast.style.borderRadius = "10px";
    toast.style.font = "12px/1.35 system-ui, -apple-system, Segoe UI, Roboto, Arial";
    toast.style.boxShadow = "0 6px 24px rgba(0,0,0,0.18)";
    toast.style.background = "rgba(20,20,20,0.92)";
    toast.style.color = "white";
    toast.style.display = "none";
    toast.style.whiteSpace = "pre-wrap";
    toast.style.pointerEvents = "none";
    return toast;
  }

  function showToast(msg, ms = 5500) {
    try {
      if (!document.body) return;
      ensureToast();
      clearTimeout(toastTimer);
      toast.textContent = msg;
      toast.style.display = "block";
      toastTimer = setTimeout(() => {
        if (toast) toast.style.display = "none";
      }, ms);
    } catch {}
  }

  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  // ============================================================
  // Prompt Finder (Fallback)
  // ============================================================
  function isVisible(el) {
    if (!el) return false;
    const r = el.getBoundingClientRect();
    const style = window.getComputedStyle(el);
    return (
      r.width > 60 &&
      r.height > 20 &&
      r.bottom > 0 &&
      r.right > 0 &&
      style.visibility !== "hidden" &&
      style.display !== "none" &&
      style.opacity !== "0"
    );
  }

  function cleanText(s) {
    return String(s || "").replace(/[\u200B-\u200D\uFEFF]/g, "").trim();
  }

  function isTextInput(el) {
    return el && (el.tagName === "TEXTAREA" || el.tagName === "INPUT");
  }

  function isContentEditableEl(el) {
    if (!el) return false;
    if (el.isContentEditable) return true;
    const ce = (el.getAttribute?.("contenteditable") || "").toLowerCase();
    return ce === "true" || ce === "";
  }

  // ✅ FIX: Bei contenteditable innerText bevorzugen (liefert Newlines aus <br>/<div>)
  function readPromptText(el) {
    if (!el) return "";
    let v = "";

    if (isTextInput(el)) {
      v = el.value || "";
      return cleanText(v);
    }

    if (isContentEditableEl(el) || (el.getAttribute?.("role") || "").toLowerCase() === "textbox") {
      v = el.innerText || el.textContent || "";
      return cleanText(v);
    }

    v = el.textContent || "";
    if (!v) v = el.innerText || "";
    return cleanText(v);
  }

  function scoreCandidate(el) {
    const r = el.getBoundingClientRect();
    const area = Math.max(1, r.width * r.height);
    const vh = Math.max(1, window.innerHeight);
    const nearBottom = r.top > vh * 0.45 ? 1.6 : 1.0;

    const id = (el.id || "").toLowerCase();
    const dt = (el.getAttribute?.("data-testid") || "").toLowerCase();
    const aria = (el.getAttribute?.("aria-label") || "").toLowerCase();
    const placeholder = (el.getAttribute?.("placeholder") || "").toLowerCase();

    let bonus = 1.0;
    if (id.includes("prompt")) bonus *= 6;
    if (dt.includes("prompt")) bonus *= 6;
    if (aria.includes("message") || aria.includes("nachricht") || aria.includes("send a message")) bonus *= 2.0;
    if (placeholder.includes("message") || placeholder.includes("nachricht")) bonus *= 2.0;

    return area * nearBottom * bonus;
  }

  // ✅ Composer-Finder: bevorzugt IMMER den unteren Prompt
  function findComposerPrompt() {
    const direct = [
      document.querySelector("textarea#prompt-textarea"),
      document.querySelector("textarea[data-testid='prompt-textarea']"),
      document.querySelector("div#prompt-textarea[contenteditable='true']"),
      document.querySelector("div[data-testid='prompt-textarea'][contenteditable='true']"),
      document.querySelector("form textarea#prompt-textarea"),
      document.querySelector("form textarea[data-testid='prompt-textarea']"),
      document.querySelector("form div[data-testid='prompt-textarea'][contenteditable='true']")
    ].filter(Boolean).find(isVisible);

    if (direct) return direct;

    // Fallback: Suche irgendein sichtbares Element mit prompt-textarea in Nähe unten
    const candidates = [
      ...document.querySelectorAll("textarea#prompt-textarea, textarea[data-testid='prompt-textarea']"),
      ...document.querySelectorAll("div#prompt-textarea[contenteditable='true'], div[data-testid='prompt-textarea'][contenteditable='true']")
    ].filter(isVisible);

    return candidates[0] || null;
  }

  function findPrompt() {
    // (alter Finder, bleibt als allgemeiner Fallback für Diktat/Editoren)
    const direct = [
      document.querySelector("textarea#prompt-textarea"),
      document.querySelector("textarea[data-testid='prompt-textarea']"),
      document.querySelector("div#prompt-textarea[contenteditable='true']"),
      document.querySelector("div[data-testid='prompt-textarea'][contenteditable='true']"),
      document.querySelector("form textarea#prompt-textarea"),
      document.querySelector("form textarea[data-testid='prompt-textarea']")
    ].filter(Boolean).find(isVisible);

    if (direct) return direct;

    const candidates = [
      ...document.querySelectorAll("textarea"),
      ...document.querySelectorAll("input[type='text']"),
      ...document.querySelectorAll("input:not([type])"),
      ...document.querySelectorAll("[contenteditable='true']"),
      ...document.querySelectorAll("[role='textbox']")
    ].filter(isVisible);

    if (!candidates.length) return null;

    candidates.sort((a, b) => scoreCandidate(b) - scoreCandidate(a));

    const top = candidates[0];
    const inner = top.querySelector?.("[contenteditable='true'], textarea, input[type='text'], input:not([type])");
    return inner && isVisible(inner) ? inner : top;
  }

  // ============================================================
  // 🎯 Targeting: nutze das Feld, das der User fokussiert hat
  // ============================================================
  let lastUserEditable = null;

  // UI Buttons werden später initialisiert, hier schon deklariert:
  let micBtn = null;
  let memBtn = null;
  let clearBtn = null;
  let promptBtn = null;
  let promptBtn2 = null;

  function isEditableTarget(el) {
    if (!el) return false;
    if (el === document.body || el === document.documentElement) return false;

    // niemals unsere eigenen UI-Buttons als Eingabefeld nehmen
    if (el === micBtn || el === memBtn || el === clearBtn || el === promptBtn || el === promptBtn2) return false;

    const tag = (el.tagName || "").toUpperCase();
    const ariaDisabled = (el.getAttribute?.("aria-disabled") || "").toLowerCase() === "true";

    if (tag === "TEXTAREA") {
      if (el.readOnly || el.disabled || ariaDisabled) return false;
      return true;
    }

    if (tag === "INPUT") {
      if (el.readOnly || el.disabled || ariaDisabled) return false;
      const type = (el.type || "text").toLowerCase();
      const ok = ["text", "search", "email", "url", "tel", "password"].includes(type);
      return ok;
    }

    // contenteditable ist okay
    if (el.isContentEditable) return true;
    const ce = (el.getAttribute?.("contenteditable") || "").toLowerCase();
    if (ce === "true" || ce === "") return true;

    // role=textbox NUR akzeptieren, wenn es wirklich ein editierbares Element ist
    const role = (el.getAttribute?.("role") || "").toLowerCase();
    if (role === "textbox") {
      if (isContentEditableEl(el)) return true;
      const inner = el.querySelector?.("textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable='']");
      if (inner) return isEditableTarget(inner);
      return false;
    }

    return false;
  }

  // ✅ Wrapper -> echtes Feld auflösen
  function resolveEditable(el) {
    if (!el || el.nodeType !== 1) return null;
    if (isEditableTarget(el) && (isTextInput(el) || isContentEditableEl(el))) return el;

    const inner = el.querySelector?.("textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable='']");
    if (inner && isEditableTarget(inner) && isVisible(inner)) return inner;

    return isEditableTarget(el) ? el : null;
  }

  function pickEditableFromEvent(e) {
    const path = typeof e.composedPath === "function" ? e.composedPath() : null;
    if (Array.isArray(path)) {
      for (const node of path) {
        if (node && node.nodeType === 1) {
          const r = resolveEditable(node);
          if (r && isVisible(r)) return r;
        }
      }
    }
    const t = e.target;
    const rt = resolveEditable(t);
    if (rt && isVisible(rt)) return rt;
    return null;
  }

  function rememberEditable(el) {
    const r = resolveEditable(el);
    if (r && isVisible(r)) lastUserEditable = r;
  }

  function getUserTargetEditable() {
    const active = resolveEditable(document.activeElement);
    if (active && isVisible(active)) return active;
    if (resolveEditable(lastUserEditable) && isVisible(lastUserEditable)) return lastUserEditable;
    return findPrompt(); // Fallback
  }

  // ✅ Für Memory/Builder: immer Composer unten bevorzugen
  function getComposerTargetEditable() {
    const composer = findComposerPrompt();
    if (composer && isVisible(composer)) return composer;
    return getUserTargetEditable();
  }

  // Fokus/Click-Tracking (auch Shadow DOM)
  document.addEventListener("focusin", (e) => rememberEditable(pickEditableFromEvent(e) || document.activeElement), true);
  document.addEventListener("pointerdown", (e) => rememberEditable(pickEditableFromEvent(e)), true);
  document.addEventListener("mousedown", (e) => rememberEditable(pickEditableFromEvent(e)), true);
  document.addEventListener("click", (e) => rememberEditable(pickEditableFromEvent(e)), true);

  // ============================================================
  // React/Input Events
  // ============================================================
  function dispatchReactInput(el, inputType, data) {
    try { el.dispatchEvent(new InputEvent("beforeinput", { bubbles: true, cancelable: true, inputType, data })); } catch {}
    try { el.dispatchEvent(new InputEvent("input", { bubbles: true, inputType, data })); } catch {
      try { el.dispatchEvent(new Event("input", { bubbles: true })); } catch {}
    }
    try { el.dispatchEvent(new Event("change", { bubbles: true })); } catch {}
  }

  function dispatchKey(el, type, key) {
    try { el.dispatchEvent(new KeyboardEvent(type, { bubbles: true, cancelable: true, key })); } catch {}
  }

  async function reactNudge(el) {
    if (!el) return;
    el.focus();

    const current = readPromptText(el);
    dispatchReactInput(el, "insertReplacementText", current);

    await sleep(CFG.reactNudgeDelayMs);

    if (isTextInput(el)) {
      const v = el.value ?? "";
      const setter =
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")?.set ||
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set;

      const setNative = (val) => {
        if (setter && (el instanceof HTMLTextAreaElement || el instanceof HTMLInputElement)) setter.call(el, val);
        else el.value = val;
      };

      setNative(v + " ");
      dispatchReactInput(el, "insertText", " ");
      dispatchKey(el, "keydown", " ");
      dispatchKey(el, "keyup", " ");
      await sleep(25);

      setNative(v);
      dispatchReactInput(el, "deleteContentBackward", null);
      dispatchKey(el, "keydown", "Backspace");
      dispatchKey(el, "keyup", "Backspace");
      return;
    }

    try {
      document.execCommand("insertText", false, " ");
      dispatchReactInput(el, "insertText", " ");
      await sleep(20);
      document.execCommand("delete", false, null);
      dispatchReactInput(el, "deleteContentBackward", null);
    } catch {}
  }

  // ============================================================
  // ✅ FIX: contenteditable sicher setzen (ohne selectAll auf ganzer Seite)
  // ============================================================
  function escapeHtml(s) {
    return String(s || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function moveCaretToEnd(el) {
    try {
      el.focus();
      const sel = window.getSelection();
      if (!sel) return;
      const range = document.createRange();
      range.selectNodeContents(el);
      range.collapse(false);
      sel.removeAllRanges();
      sel.addRange(range);
    } catch {}
  }

  function setContentEditablePreserveNewlines(el, text) {
    if (!el) return;
    const safe = escapeHtml(text);
    const html = safe.replace(/\n/g, "<br>");

    try {
      el.focus();
      // ✅ kein document.execCommand("selectAll")
      el.innerHTML = html;
      moveCaretToEnd(el);
    } catch {
      try { el.textContent = text; } catch {}
    }

    dispatchReactInput(el, "insertReplacementText", text);
  }


  // ============================================================
  // ✅ PASTE-APPLY (ohne execCommand selectAll/paste → kein Ganzseiten-Markieren)
  // ============================================================
  function normalizeForCompare(s) {
    return String(s || "")
      .replace(/\r\n/g, "\n")
      .replace(/[ \t]+/g, " ")
      .replace(/\n{3,}/g, "\n\n")
      .trim();
  }

  function setNativeValue(el, val) {
    if (!el) return;
    const v = String(val ?? "");
    try {
      if (el instanceof HTMLTextAreaElement) {
        const setter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")?.set;
        if (setter) setter.call(el, v);
        else el.value = v;
        return;
      }
      if (el instanceof HTMLInputElement) {
        const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set;
        if (setter) setter.call(el, v);
        else el.value = v;
        return;
      }
      // fallback
      el.value = v;
    } catch {
      try { el.value = v; } catch {}
    }
  }

  async function setViaPaste(el, text) {
    const target = String(text ?? "").replace(/\r\n/g, "\n");
    if (!el) return false;

    try { el.focus(); } catch {}

    // 1) textarea/input: direkt setzen (stabil, schnell, kein Markieren der Seite)
    if (isTextInput(el)) {
      setNativeValue(el, target);
      try { el.setSelectionRange(target.length, target.length); } catch {}
      dispatchReactInput(el, "insertReplacementText", target);

      await sleep(CFG.postPasteDelayMs);
      await reactNudge(el);
      await sleep(60);

      const got = normalizeForCompare(readPromptText(el));
      const want = normalizeForCompare(target);
      if (got === want) return true;

      // toleranter Vergleich
      if (got.replace(/\s+/g, " ") === want.replace(/\s+/g, " ")) return true;
      if (got.length >= Math.min(500, Math.floor(want.length * 0.5)) && want.startsWith(got)) {
        showToast(`⚠️ Prompt vermutlich gekürzt.\nEingefügt: ${got.length} Zeichen von ${want.length}`, 7000);
        return true;
      }
      return false;
    }

    // 2) contenteditable: innerHTML setzen (mit <br>) + InputEvents
    setContentEditablePreserveNewlines(el, target);

    await sleep(CFG.postPasteDelayMs);
    await reactNudge(el);
    await sleep(60);

    const got = normalizeForCompare(readPromptText(el));
    const want = normalizeForCompare(target);

    if (got === want) return true;
    if (got.replace(/\s+/g, " ") === want.replace(/\s+/g, " ")) return true;

    if (got.length >= Math.min(500, Math.floor(want.length * 0.5)) && want.startsWith(got)) {
      showToast(`⚠️ Prompt vermutlich gekürzt.\nEingefügt: ${got.length} Zeichen von ${want.length}`, 7000);
      return true;
    }

    return false;
  }


  // ============================================================
  // Gemini Calls
  // ============================================================
  function gmRequest() {
    if (typeof GM !== "undefined" && typeof GM.xmlHttpRequest === "function") return GM.xmlHttpRequest;
    if (typeof GM_xmlhttpRequest === "function") return GM_xmlhttpRequest;
    return null;
  }

  function extractGeminiText(json) {
    const parts = json?.candidates?.[0]?.content?.parts;
    if (Array.isArray(parts)) {
      const out = parts.map(p => (p?.text ?? "")).join("").trim();
      if (out) return out;
    }
    const t = json?.candidates?.[0]?.text;
    if (typeof t === "string" && t.trim()) return t.trim();
    return "";
  }

  function geminiGenerate(userPrompt, { temperature = 0.05, maxOutputTokens = 2048 } = {}) {
    const apiKey = getGeminiKey();
    if (!apiKey) {
      return Promise.reject("API-Key fehlt. (Tampermonkey-Menü: „Gemini-Key setzen/ändern“)");
    }

    const url = `https://generativelanguage.googleapis.com/v1beta/${GEMINI_MODEL}:generateContent?key=${encodeURIComponent(apiKey)}`;

    const payload = {
      contents: [{ role: "user", parts: [{ text: userPrompt }] }],
      generationConfig: { temperature, maxOutputTokens }
    };

    const req = gmRequest();
    if (!req) return Promise.reject("GM Request API fehlt (Tampermonkey Grants).");

    const sleepLocal = (ms) => new Promise(r => setTimeout(r, ms));

    const attempt = (n) => new Promise((resolve, reject) => {
      req({
        method: "POST",
        url,
        headers: { "Content-Type": "application/json" },
        data: JSON.stringify(payload),
        timeout: CFG.requestTimeoutMs,

        onload: (r) => {
          const body = String(r.responseText || "");
          if (r.status !== 200) {
            let msg = body.slice(0, 800);
            try {
              const j = JSON.parse(body);
              const m = j?.error?.message || j?.message;
              if (m) msg = m;
            } catch {}
            reject(`HTTP ${r.status}: ${msg}`);
            return;
          }

          try {
            const j = JSON.parse(body);
            const out = extractGeminiText(j);
            if (!out) {
              reject("Gemini lieferte keinen Text (Parser fand keinen Output).");
              return;
            }
            resolve(out);
          } catch {
            reject("Antwort konnte nicht gelesen werden (JSON Parse).");
          }
        },

        onerror: () => reject(`Network (Status 0).\nURL: ${url}\nHinweise: @connect, Adblock/Privacy, VPN/Proxy, Firmen-Netz.`),
        ontimeout: () => reject("Timeout")
      });
    }).catch(async (err) => {
      const msg = String(err || "");
      const retryable = msg.includes("Network (Status 0)") || msg === "Timeout";
      if (retryable && n < CFG.maxRetries) {
        await sleepLocal(CFG.retryWaitMs * (n + 1));
        return attempt(n + 1);
      }
      throw err;
    });

    return attempt(0);
  }

  // ---- 1) Diktat-Bereinigung + Grammatik Prompt ----
  function buildGrammarPrompt(text) {
    const mode = (CFG.dictationCleanupMode || "balanced").toLowerCase();

    const modeRules =
      mode === "aggressive"
        ? `
- Entferne auch einzelne Wörter, die im Kontext sehr wahrscheinlich Fehlhörer sind und den Satz entgleisen lassen.
- Glätte Satzstruktur stärker, aber ohne neue Fakten hinzuzufügen.
`
        : mode === "conservative"
        ? `
- Entferne nur extrem offensichtliches Kauderwelsch (z.B. Wortfragmente, lautmalerische Silben, unverbundene Tokens).
- Wenn du unsicher bist, lass lieber stehen als zu viel zu löschen.
`
        : `
- Entferne offensichtliches Kauderwelsch zuverlässig, aber ohne den Text stilistisch umzuschreiben.
- Korrigiere offensichtliche Fehlhörer nur dann, wenn es genau eine sehr naheliegende Korrektur gibt.
`;

    return `
Du bist ein deutscher Diktat-Editor.

AUFGABE (in dieser Reihenfolge):
1) Bereinige Diktat-Artefakte: entferne Fülllaute (z.B. "äh", "ähm", "öhm", "hm", "mhm").
2) Entferne Doppler durch Stottern/ASR: doppelte Wörter oder kurze Wiederholungen (z.B. "ich ich", "das das", "und und").
3) Entferne eindeutig sinnlose Kauderwelsch-Tokens (Wortfragmente, unverbundene Einzelwörter, semantisch nicht anschließbare Einsprengsel).
4) Korrigiere Grammatik, Satzbau, Zeichensetzung und Groß-/Kleinschreibung.
5) Erkenne Satzgrenzen korrekt.

MODUS:
${modeRules.trim()}

WICHTIG (strikt):
- Keine neuen Informationen hinzufügen.
- Keine kreativen Ergänzungen, keine Vermutungen.
- Keine stilistische Umformulierung: Wortwahl und Struktur so nah wie möglich am Original halten.
- Wenn ein Wort falsch erkannt wirkt, aber nicht eindeutig reparierbar ist: entferne es lieber, statt zu raten.

REGEL:
Gib AUSSCHLIESSLICH den bereinigten Text zurück.
Keine Kommentare. Keine Erklärungen.

TEXT:
${text}
`.trim();
  }

  function geminiRewriteGrammar(text) {
    return geminiGenerate(buildGrammarPrompt(text), {
      temperature: 0.03,
      maxOutputTokens: CFG.grammarMaxOutputTokens || 8192
    });
  }

  function splitIntoChunksByParagraphs(text, maxChars) {
    const s = String(text || "");
    if (s.length <= maxChars) return [s];

    const paras = s.split(/\n{2,}/g);
    const chunks = [];
    let buf = "";

    const pushBuf = () => {
      if (buf.trim().length) chunks.push(buf);
      buf = "";
    };

    for (let p of paras) {
      p = p.trim();
      if (!p) continue;

      if (p.length > maxChars) {
        pushBuf();
        let start = 0;
        while (start < p.length) {
          let end = Math.min(start + maxChars, p.length);
          if (end < p.length) {
            const windowStart = Math.max(start, end - Math.floor(maxChars * 0.5));
            const slice = p.slice(windowStart, end);
            const lastDot = Math.max(
              slice.lastIndexOf("."),
              slice.lastIndexOf("!"),
              slice.lastIndexOf("?"),
              slice.lastIndexOf(";"),
              slice.lastIndexOf(":")
            );
            if (lastDot > -1) end = windowStart + lastDot + 1;
          }
          chunks.push(p.slice(start, end).trim());
          start = end;
        }
        continue;
      }

      const candidate = buf ? (buf + "\n\n" + p) : p;
      if (candidate.length > maxChars) {
        pushBuf();
        buf = p;
      } else {
        buf = candidate;
      }
    }

    pushBuf();
    return chunks.length ? chunks : [s];
  }

  async function geminiRewriteGrammarSmart(fullText, onProgress) {
    const input = String(fullText || "");
    if (!input.trim()) return input;

    const oneShot = await geminiRewriteGrammar(input);
    const ratio = oneShot.length / Math.max(1, input.length);
    if (ratio >= (CFG.grammarTruncationRatio || 0.85)) return oneShot;

    const chunks = splitIntoChunksByParagraphs(input, CFG.grammarChunkChars || 3500);
    const outParts = [];

    for (let i = 0; i < chunks.length; i++) {
      onProgress?.(i + 1, chunks.length);
      const fixed = await geminiRewriteGrammar(chunks[i]);
      outParts.push(fixed && fixed.trim().length ? fixed.trim() : chunks[i].trim());
    }

    return outParts.join("\n\n").trim();
  }

  // ============================================================
  // DOMÄNEN-EXTRAKTION (robust + Repair-Pass)
  // ============================================================
  function extractTaskOnly(maybePrompt) {
    const s = String(maybePrompt || "");
    const idx = s.search(/\nAufgabe:\s*/i);
    if (idx >= 0) {
      const after = s.slice(idx).replace(/^.*?\nAufgabe:\s*/i, "");
      const endIdx = after.search(/\n(Zielgruppe:|Kontext:|Format:|Ton:|Pflichtlogik:|Falls dir)\s*/i);
      const task = endIdx >= 0 ? after.slice(0, endIdx) : after;
      const t = cleanText(task);
      return t.length ? t : cleanText(maybePrompt);
    }
    const idx2 = s.search(/\nAUFGABE:\s*/i);
    if (idx2 >= 0) {
      const after = s.slice(idx2).replace(/^.*?\nAUFGABE:\s*/i, "");
      const endIdx = after.search(/\n(Zielgruppe:|Kontext:|Format:|Ton:|Pflichtlogik:|REGEL:)\s*/i);
      const task = endIdx >= 0 ? after.slice(0, endIdx) : after;
      const t = cleanText(task);
      return t.length ? t : cleanText(maybePrompt);
    }
    return cleanText(maybePrompt);
  }

  function normalizeDomain(x) {
    let s = cleanText(x);
    if (!s) return "";
    s = s.replace(/^[\[\(\{]+/g, "").replace(/[\]\)\}]+$/g, "");
    s = s.replace(/^["'`]+|["'`]+$/g, "");
    s = s.replace(/^\s*[-–—•\u2022]\s*/g, "");
    s = s.replace(/^\s*\d+[\.\)]\s*/g, "");
    s = s.replace(/^here is the json requested:\s*/i, "");
    s = s.replace(/^(topic_domains|method_domains)\s*:\s*/i, "");
    s = s.replace(/\.$/, "").trim();
    return s;
  }

  function isBannedGeneric(domain) {
    const d = domain.toLowerCase();
    if (d.includes("unterbereich")) return true;
    if (d.includes("fachgebiet")) return true;
    if (d.includes("domänenanalyse")) return true;
    if (d === "naturwissenschaften" || d === "informatik" || d === "ingenieurwissenschaften" || d === "sozialwissenschaften" || d === "rechtswissenschaften" || d === "wirtschaftswissenschaften") return true;
    if (d.length < 3) return true;
    return false;
  }

  function isMethodDomain(domain) {
    const d = domain.toLowerCase();
    return (
      d.includes("evidenz") ||
      d.includes("statistik") ||
      d.includes("studienmethod") ||
      d.includes("risiko") ||
      d.includes("trade-off") ||
      d.includes("ethik") ||
      d.includes("systemdenken") ||
      d.includes("anwendungsplanung") ||
      d.includes("evaluation") ||
      d.includes("methodik")
    );
  }

  function parseDomainsLoose(raw) {
    const s = String(raw || "").trim();
    if (!s) return [];

    let candidates = [];
    const quoted = [...s.matchAll(/"([^"]{2,160})"/g)].map(m => m[1]);
    if (quoted.length) candidates.push(...quoted);
    candidates.push(...s.replace(/[•\u2022]/g, " ").split(/[,;\n]/g));

    const out = [];
    const seen = new Set();

    for (const c of candidates) {
      const d = normalizeDomain(c);
      if (!d) continue;
      const key = d.toLowerCase();
      if (key === "topic_domains" || key === "method_domains") continue;
      if (key.includes("here is the json requested")) continue;
      if (isBannedGeneric(d)) continue;

      if (seen.has(key)) continue;
      seen.add(key);
      out.push(d);
    }

    return out;
  }

  function looksSuspiciousRaw(raw) {
    const s = String(raw || "").trim();
    if (!s) return true;
    if (/…|\.{3}|,\s*$/.test(s)) return true;
    if (s.length < 25) return true;
    if ((s.match(/,/g) || []).length < 3) return true;
    if (/\bErnähr\b|\bBiogen\b/i.test(s)) return true;
    return false;
  }

  function buildDomainsPrompt(taskText, minN = 7, maxN = 10) {
    return `
Du bist ein "Fachdomänen-Detektor".

Ziel:
Leite aus der Aufgabe die spezifischsten passenden Fachdomänen (wissenschaftliche Disziplinen / Teilgebiete) ab.

Regeln (wichtig):
- Ausgabe NUR als kommagetrennte Liste.
- KEIN JSON. KEINE Klammern. KEINE Anführungszeichen. KEINE Nummerierung. KEINE Bulletpoints. KEINE Sätze.
- Mindestens ${minN} und höchstens ${maxN} Einträge.
- Mindestens 5 Einträge müssen thematische Subdomänen (Themenkern) sein.
- Maximal 2 Einträge dürfen methodische Domänen sein (z.B. Evidenzbewertung, Statistik).
- Verwende spezifische Subdomänen statt Oberkategorien. Vermeide z.B. "Naturwissenschaften", "Informatik", "Recht", "Wirtschaft".
- Keine Abkürzungen/Abschneidungen: keine Wortstämme wie "Biogen", "Ernähr". Alles vollständig ausschreiben.

AUFGABE:
${taskText}
`.trim();
  }

  function buildDomainsRepairPrompt(taskText, rawList, minN = 7, maxN = 10) {
    return `
Du bekommst eine (möglicherweise abgeschnittene oder fehlerhafte) Domänenliste und sollst sie korrigieren.

Aufgabe:
- Ersetze Wortstämme/Abkürzungen durch vollständig ausgeschriebene Fachdomänen.
- Entferne generische Oberkategorien und ersetze sie durch spezifische Subdomänen zum Themenkern.
- Ergebnis: Mindestens ${minN}, höchstens ${maxN} Einträge.
- Mindestens 5 thematische Subdomänen, maximal 2 methodische Domänen.

Format-Regeln (strikt):
- Ausgabe NUR als kommagetrennte Liste.
- KEIN JSON. KEINE Klammern. KEINE Anführungszeichen. KEINE Nummerierung. KEINE Sätze.

AUFGABE:
${taskText}

ROH-LISTE (zu reparieren):
${rawList}
`.trim();
  }

  function buildDomainsPromptSecondPass(taskText, existingList, needN) {
    return `
Es fehlen noch Fachdomänen.

REGELN:
- Ausgabe NUR als kommagetrennte Liste.
- KEIN JSON. KEINE Klammern. KEINE Anführungszeichen. KEINE Nummerierung. KEINE Sätze.
- Liefere GENAU ${needN} zusätzliche Domänen.
- Wiederhole keine vorhandenen Domänen.
- Nur Domänen, die direkt zum Themenkern passen; höchstens 1 methodische Domäne.

VORHANDEN:
${existingList.join(", ")}

AUFGABE:
${taskText}
`.trim();
  }

  function pickFinalDomains(list, minN, maxN) {
    const topic = list.filter(d => !isMethodDomain(d));
    const method = list.filter(d => isMethodDomain(d));

    const final = [];
    const seen = new Set();

    const add = (x) => {
      const k = x.toLowerCase();
      if (seen.has(k)) return;
      seen.add(k);
      final.push(x);
    };

    for (const t of topic) {
      add(t);
      if (final.length >= maxN) break;
    }

    if (final.length < maxN) {
      let mCount = 0;
      for (const m of method) {
        if (mCount >= 2) break;
        add(m);
        mCount++;
        if (final.length >= maxN) break;
      }
    }

    if (final.length < minN) {
      for (const x of list) {
        add(x);
        if (final.length >= minN) break;
      }
    }

    return final.slice(0, maxN);
  }

  async function geminiExtractDomainsRobust(taskText) {
    const coreTask = extractTaskOnly(taskText);

    const raw1 = await geminiGenerate(
      buildDomainsPrompt(coreTask, DOMAIN_CFG.minDomains, DOMAIN_CFG.maxDomains),
      { temperature: DOMAIN_CFG.temperature, maxOutputTokens: DOMAIN_CFG.maxOutputTokens }
    );

    let list1 = parseDomainsLoose(raw1);
    const suspicious = looksSuspiciousRaw(raw1) || list1.length < DOMAIN_CFG.minDomains;

    let list = list1;

    if (suspicious) {
      const rawRepair = await geminiGenerate(
        buildDomainsRepairPrompt(coreTask, raw1, DOMAIN_CFG.minDomains, DOMAIN_CFG.maxDomains),
        { temperature: 0.05, maxOutputTokens: DOMAIN_CFG.repairMaxOutputTokens }
      );
      const repaired = parseDomainsLoose(rawRepair);
      if (repaired.length) list = repaired;
    }

    if (list.length < DOMAIN_CFG.minDomains) {
      const need = DOMAIN_CFG.minDomains - list.length;
      const raw2 = await geminiGenerate(
        buildDomainsPromptSecondPass(coreTask, list, need),
        { temperature: 0.08, maxOutputTokens: DOMAIN_CFG.maxOutputTokens }
      );
      const add = parseDomainsLoose(raw2);
      const seen = new Set(list.map(x => x.toLowerCase()));
      for (const a of add) {
        const k = a.toLowerCase();
        if (!seen.has(k) && !isBannedGeneric(a)) {
          list.push(a);
          seen.add(k);
        }
        if (list.length >= DOMAIN_CFG.maxDomains) break;
      }
    }

    list = pickFinalDomains(list, DOMAIN_CFG.minDomains, DOMAIN_CFG.maxDomains);
    if (list.length < 3) return ["Neurowissenschaften", "Zellbiologie", "Molekularbiologie"];
    return list;
  }

  // ============================================================
  // Final Prompt Builder (Frank)
  // ============================================================
  function buildFinalPrompt(taskText, domainsList) {
    const domains = domainsList.join(", ");
    return `
Rolle:
Du bist ein interdisziplinäres Forscherteam aus den 5 weltweit führenden Wissenschaftlerinnen und Wissenschaftlern,
spezialisiert auf ${domains}. Ihr arbeitet evidenzbasiert, kritisch, sehr ausführlich,
konsensorientiert, erklärt alle Zusammenhänge ganz genau und begründet Empfehlungen ausführlich. Ihr betrachtet die Aufgabe aus allen möglichen verschiedenen Perspektiven. Leitet aus den möglichen, verschiedenen Perspektiven alle relevanten Domänen ab. Es sollen mindestens ${DOMAIN_CFG.minDomains} verschiedene Fachdomänen vom interdisziplinären Forscherteam bei der Beantwortung der Aufgabe berücksichtigt werden.

Aufgabe:
${taskText}
Falls dir etwas zur Aufgabe nicht klar ist, frage nochmal den Benutzer. ChatGPT soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
allumfassendes Bild für die Antwort einbezogen wird.

Zielgruppe:
Frank (Benutzer dieses Chats).

Kontext:
Nutze die über Frank gespeicherten Informationen und Erinnerungen (Bio, Ziele, Routinen, Besonderheiten, Projekte, usw.) als
primären Bezugsrahmen für diese Aufgabe. Keine zusätzlichen Kontextabfragen; verwende vorhandenes Wissen.

Format:
Strikte Ausgabestruktur „Einleitung – Hauptteil – Zusammenfassung“. Fachbegriffe sofort kurz erklären (in Klammern); Erläutere alle Zusammenhänge sämtlicher Faktoren sehr genau, damit Frank alle Zusammenhänge (wie, warum etwas so ist) genaustens versteht.
Abkürzungen bei erster Nennung ausschreiben; logisch, faktenbasiert, umfassend, nachvollziehbar. Benutze gegeben falls auch Symbole und Smilies, große Überschriften, Tabelle zu besseren optischen Gliederung.
Mindestwortanzahl: 2000, Maximale Wortanzalhl: 10000, Entscheidend ist, dass die Aufgabe/Frage sehr ausführlich erledigt/beantwortet wird.

Ton:
Wissenschaftlich-professionell, präzise, didaktisch klar, ausführlich, ohne metasprachliche Hinweise oder Floskeln, leicht verständlich.

Pflichtlogik:
Keine Abweichung bei Zielgruppe, Kontext, Format, Ton.
`.trim();
  }

  // ============================================================
  // Final Prompt Builder (GENERAL / 12. Klasse)
  // ============================================================
  function buildFinalPromptGeneral(taskText, domainsList) {
    const domains = domainsList.join(", ");
    return `
Rolle:
Du bist ein interdisziplinäres Forscherteam aus den 5 weltweit führenden Wissenschaftlerinnen und Wissenschaftlern,
spezialisiert auf ${domains}. Ihr arbeitet evidenzbasiert, kritisch, sehr ausführlich,
konsensorientiert, erklärt alle Zusammenhänge ganz genau und begründet Empfehlungen ausführlich. Ihr betrachtet die Aufgabe aus allen möglichen verschiedenen Perspektiven. Leitet aus den möglichen, verschiedenen Perspektiven alle relevanten Domänen ab. Es sollen mindestens ${DOMAIN_CFG.minDomains} verschiedene Fachdomänen vom interdisziplinären Forscherteam bei der Beantwortung der Aufgabe berücksichtigt werden.

Aufgabe:
${taskText}
Falls dir etwas zur Aufgabe nicht klar ist frage nochmal den Benutzer. ChatGPT soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
allumfassendes Bild für die Antwort einbezogen wird.

Zielgruppe:
Geschrieben für jedermann; verständlich für Lernende etwa auf Niveau 12. Klasse Gymnasium.

Kontext:
Kein weiterer Kontext außer dem Inhalt der Aufgabe. Keine zusätzlichen Kontextabfragen.

Format:
Strikte Ausgabestruktur „Einleitung – Hauptteil – Zusammenfassung“.
Fachbegriffe sofort kurz erklären (in Klammern).
Abkürzungen bei erster Nennung ausschreiben.
Logisch, faktenbasiert, umfassend, nachvollziehbar.
Keine Stichpunkte; Tabellen nur, wenn sie die Verständlichkeit klar verbessern.
Mindestwortanzahl: 2000, Maximale Wortanzalhl: 10000, Entscheidend ist, dass die Aufgabe sehr ausführlich erledigt/beantwortet wird.

Ton:
Wissenschaftlich-professionell, klar und präzise; sehr ausführlich, didaktisch verständlich ohne Floskeln.
Keine metasprachlichen Hinweise oder stilistischen Selbstkommentare.

Pflichtlogik:
Zielgruppe, Kontext, Format und Ton dürfen niemals abweichen.
`.trim();
  }

  // ============================================================
  // UI Buttons (BOTTOM RIGHT)
  // ============================================================
  function styleRoundButton(b, rightOffsetPx = 0, bottomOffsetPx = 0) {
    b.type = "button";
    b.style.position = "fixed";
    b.style.zIndex = "999999";
    b.style.width = "42px";
    b.style.height = "42px";
    b.style.borderRadius = "50%";
    b.style.cursor = "pointer";
    b.style.border = "1px solid rgba(0,0,0,0.2)";
    b.style.background = "white";
    b.style.boxShadow = "0 6px 18px rgba(0,0,0,0.18)";
    b.style.fontSize = "18px";

    // ✅ FIX: +3mm mehr Abstand von rechts => Buttons wandern nach links
    b.style.right = `calc(${UI_POS.rightPx + rightOffsetPx}px + ${UI_SHIFT_LEFT})`;
    b.style.bottom = `${UI_POS.bottomPx + bottomOffsetPx}px`;
    b.style.left = "auto";
    b.style.top = "auto";

    // ✅ FIX: Button soll beim Klicken NICHT den Fokus klauen
    b.tabIndex = -1;
  }

  function preventFocusSteal(btn) {
    try {
      btn.addEventListener("pointerdown", (e) => e.preventDefault(), true);
      btn.addEventListener("mousedown", (e) => e.preventDefault(), true);
    } catch {}
  }

  function setMicState(state, msg = "") {
    if (!micBtn) return;
    if (!micBtn.classList.contains("stt-mic-btn")) micBtn.classList.add("stt-mic-btn");

    if (state === "listening") {
      micBtn.innerHTML = MIC_ICON.stop;
      micBtn.setAttribute("data-state", "listening");
      micBtn.title = "Spracheingabe läuft – klicken zum Stop";
      return;
    }
    if (state === "working") {
      micBtn.innerHTML = MIC_ICON.spinner;
      micBtn.setAttribute("data-state", "working");
      micBtn.title = msg || "Bereinigung läuft…";
      return;
    }
    if (state === "error") {
      micBtn.innerHTML = MIC_ICON.error;
      micBtn.setAttribute("data-state", "error");
      micBtn.title = msg || "Fehler";
      return;
    }

    // idle
    micBtn.innerHTML = MIC_ICON.mic;
    micBtn.setAttribute("data-state", "idle");
    micBtn.title = supportedSpeech ? "Spracheingabe (Start/Stop)" : "Speech API nicht verfügbar";
  }

  function setMemBtnState(state, msg = "") {
    if (!memBtn) return;

    if (state === "working") {
      memBtn.textContent = "⏳";
      memBtn.style.background = "#444";
      memBtn.style.color = "white";
      memBtn.title = msg || "Memory-Prompt wird eingefügt…";
      return;
    }
    if (state === "error") {
      memBtn.textContent = "⚠️";
      memBtn.style.background = "#8b0000";
      memBtn.style.color = "white";
      memBtn.title = msg || "Fehler";
      return;
    }

    memBtn.textContent = "💾";
    memBtn.style.background = "white";
    memBtn.style.color = "black";
    memBtn.title = "Memory-Prompt einfügen";
  }

  function setPromptBtnState(state, msg = "") {
    if (!promptBtn) return;

    if (state === "working") {
      promptBtn.textContent = "⏳";
      promptBtn.style.background = "#444";
      promptBtn.style.color = "white";
      promptBtn.title = msg || "Prompt wird gebaut…";
      return;
    }
    if (state === "error") {
      promptBtn.textContent = "⚠️";
      promptBtn.style.background = "#8b0000";
      promptBtn.style.color = "white";
      promptBtn.title = msg || "Fehler";
      return;
    }

    promptBtn.textContent = "✨";
    promptBtn.style.background = "white";
    promptBtn.style.color = "black";
    promptBtn.title = "Prompt (für Frank) einbetten";
  }

  function setPromptBtn2State(state, msg = "") {
    if (!promptBtn2) return;

    if (state === "working") {
      promptBtn2.textContent = "⏳";
      promptBtn2.style.background = "#444";
      promptBtn2.style.color = "white";
      promptBtn2.title = msg || "Prompt wird gebaut…";
      return;
    }
    if (state === "error") {
      promptBtn2.textContent = "⚠️";
      promptBtn2.style.background = "#8b0000";
      promptBtn2.style.color = "white";
      promptBtn2.title = msg || "Fehler";
      return;
    }

    promptBtn2.textContent = "🪄";
    promptBtn2.style.background = "white";
    promptBtn2.style.color = "black";
    promptBtn2.title = "Prompt (allgemein / 12. Klasse) einbetten";
  }

  // ============================================================
  // Groq Whisper Speech-to-Text (MediaRecorder + Groq API)
  // ============================================================
  let wantsRecording = false;
  let mediaRecorder = null;
  let audioChunks = [];
  let audioStream = null;

  function groqTranscribe(audioBlob) {
    const groqKey = getGroqKey();
    if (!groqKey) {
      setMicState("error", "Groq API-Key fehlt");
      showToast("\u274c Groq API-Key fehlt.\nTampermonkey-Men\u00fc \u2192 Groq-Key setzen.", 8000);
      setTimeout(() => setMicState("idle"), 3000);
      return;
    }

    const formData = new FormData();
    formData.append("file", audioBlob, "recording.webm");
    formData.append("model", CFG.whisperModel);
    formData.append("language", CFG.whisperLang);
    formData.append("response_format", "text");

    const req = gmRequest();
    if (!req) {
      setMicState("error", "GM Request API fehlt");
      showToast("\u274c GM Request API fehlt (Tampermonkey Grants).", 7000);
      setTimeout(() => setMicState("idle"), 3000);
      return;
    }

    setMicState("working", "Whisper transkribiert\u2026");
    showToast("\ud83c\udfa7 Whisper transkribiert\u2026", 2000);

    req({
      method: "POST",
      url: "https://api.groq.com/openai/v1/audio/transcriptions",
      headers: { "Authorization": "Bearer " + groqKey },
      data: formData,
      timeout: CFG.requestTimeoutMs,

      onload: async (r) => {
        if (r.status !== 200) {
          let msg = (r.responseText || "").slice(0, 400) || ("HTTP " + r.status);
          try { const j = JSON.parse(r.responseText); if (j?.error?.message) msg = j.error.message; } catch {}
          setMicState("error", msg);
          showToast("\u274c Groq Fehler:\n" + msg, 9000);
          setTimeout(() => setMicState("idle"), 3000);
          return;
        }

        const text = (r.responseText || "").trim();
        if (!text) {
          setMicState("idle");
          showToast("\u26a0\ufe0f Keine Sprache erkannt.", 3000);
          return;
        }

        const el = getUserTargetEditable();
        if (!el) {
          setMicState("error", "Kein Eingabefeld");
          showToast("\u274c Eingabefeld nicht gefunden.", 5000);
          setTimeout(() => setMicState("idle"), 2500);
          return;
        }

        const cur = readPromptText(el);
        const spacer = cur && !cur.endsWith(" ") && !cur.endsWith("\n") ? " " : "";
        const combined = cur + spacer + text;

        const ok = await setViaPaste(el, combined);
        if (ok) {
          setMicState("idle");
          const preview = text.length > 80 ? text.slice(0, 80) + "\u2026" : text;
          showToast("\u2705 " + preview, 3000);
        } else {
          setMicState("error", "Text nicht \u00fcbernommen");
          showToast("\u274c Eingabefeld hat Text nicht \u00fcbernommen.", 5000);
          setTimeout(() => setMicState("idle"), 2500);
        }
      },

      onerror: () => {
        setMicState("error", "Netzwerk-Fehler");
        showToast("\u274c Netzwerk-Fehler bei Groq API.\nHinweise: @connect, Adblock/Privacy, VPN/Proxy.", 7000);
        setTimeout(() => setMicState("idle"), 3000);
      },
      ontimeout: () => {
        setMicState("error", "Timeout");
        showToast("\u274c Groq API Timeout.", 5000);
        setTimeout(() => setMicState("idle"), 3000);
      }
    });
  }

  function startListening() {
    if (!supportedSpeech) return;

    const t = getUserTargetEditable();
    if (!t) {
      showToast("\u26a0\ufe0f Kein fokussiertes Eingabefeld. Tipp: zuerst ins Ziel-Feld tippen.", 3500);
    } else {
      rememberEditable(t);
    }

    wantsRecording = true;
    audioChunks = [];

    navigator.mediaDevices.getUserMedia({ audio: true })
      .then(stream => {
        audioStream = stream;

        const mimeType = typeof MediaRecorder.isTypeSupported === "function"
          ? (MediaRecorder.isTypeSupported("audio/webm;codecs=opus") ? "audio/webm;codecs=opus"
            : MediaRecorder.isTypeSupported("audio/webm") ? "audio/webm"
            : "")
          : "";

        const options = mimeType ? { mimeType } : {};
        mediaRecorder = new MediaRecorder(stream, options);

        mediaRecorder.ondataavailable = (e) => {
          if (e.data.size > 0) audioChunks.push(e.data);
        };

        mediaRecorder.onstop = () => {
          stream.getTracks().forEach(t => t.stop());
          audioStream = null;

          if (audioChunks.length === 0) {
            setMicState("idle");
            return;
          }

          const audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType || "audio/webm" });
          audioChunks = [];
          groqTranscribe(audioBlob);
        };

        mediaRecorder.start(1000);
        setMicState("listening");
        showToast("\ud83c\udf99\ufe0f Aufnahme l\u00e4uft\u2026 (Stop \u00fcber \u23f9\ufe0f)", 1500);
      })
      .catch(err => {
        wantsRecording = false;
        setMicState("error", String(err));
        showToast("\u274c Mikrofon-Zugriff fehlgeschlagen:\n" + String(err), 8000);
        setTimeout(() => setMicState("idle"), 3000);
      });
  }

  function stopListening() {
    wantsRecording = false;

    if (mediaRecorder && mediaRecorder.state !== "inactive") {
      setMicState("working", "Aufnahme beendet\u2026");
      mediaRecorder.stop();
    } else {
      if (audioStream) {
        audioStream.getTracks().forEach(t => t.stop());
        audioStream = null;
      }
      setMicState("idle");
    }
  }

  function toggleMic() {
    if (!supportedSpeech) {
      showToast("\u274c Mikrofon nicht verf\u00fcgbar (getUserMedia).", 5000);
      return;
    }
    if (!wantsRecording) startListening();
    else stopListening();
  }

  // ============================================================
  // Memory Prompt Button
  // ============================================================
  async function runMemoryPrompt() {
    // ✅ IMMER Composer unten
    const el = getComposerTargetEditable();
    if (!el) {
      setMemBtnState("error", "Eingabefeld nicht gefunden");
      showToast("❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.", 6500);
      setTimeout(() => setMemBtnState("idle"), 2500);
      return;
    }

    try {
      setMemBtnState("working", "Memory-Prompt einfügen…");
      const finalPrompt = MEMORY_PROMPT;

      const preview = finalPrompt.replace(/\s+/g, " ").slice(0, CFG.previewChars);
      showToast("💾 Memory-Prompt (Vorschau):\n" + preview + (finalPrompt.length > CFG.previewChars ? " …" : ""), 3500);

      const ok = await setViaPaste(el, finalPrompt);
      if (!ok) {
        setMemBtnState("error", "Eingabefeld hat Text nicht übernommen");
        showToast("❌ Eingabefeld hat den Memory-Prompt nicht übernommen.", 6500);
        setTimeout(() => setMemBtnState("idle"), 2500);
        return;
      }

      setMemBtnState("idle");
      showToast("✅ Memory-Prompt eingefügt.", 1800);
    } catch (e) {
      const msg = String(e || "Unbekannter Fehler");
      console.warn("Memory-Button Fehler:", msg);
      setMemBtnState("error", msg);
      showToast("❌ Memory-Button Fehler:\n" + msg, 10000);
      setTimeout(() => setMemBtnState("idle"), 2500);
    }
  }

  // ============================================================
  // Prompt Builder (Frank)
  // ============================================================
  async function runPromptBuilder() {
    // ✅ IMMER Composer unten (du willst den fertigen Prompt ja dort abschicken)
    const el = getComposerTargetEditable();
    if (!el) {
      setPromptBtnState("error", "Eingabefeld nicht gefunden");
      showToast("❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.", 6500);
      setTimeout(() => setPromptBtnState("idle"), 2500);
      return;
    }

    const snap = readPromptText(el);
    if (snap.length < CFG.minCharsForRewrite) {
      setPromptBtnState("idle");
      showToast(`Text zu kurz (${snap.length}) – Prompt-Builder nicht gestartet.`, 3500);
      return;
    }

    try {
      setPromptBtnState("working", "Fachdomänen ableiten…");
      showToast("✨ Prompt-Builder: Fachdomänen ableiten…", 1400);

      const domains = await geminiExtractDomainsRobust(snap);

      setPromptBtnState("working", "Gerüst bauen…");
      const finalPrompt = buildFinalPrompt(snap, domains);

      const preview = finalPrompt.replace(/\s+/g, " ").slice(0, CFG.previewChars);
      showToast("🧱 Prompt-Builder (Vorschau):\n" + preview + (finalPrompt.length > CFG.previewChars ? " …" : ""), 3500);

      const ok = await setViaPaste(el, finalPrompt);
      if (!ok) {
        setPromptBtnState("error", "Eingabefeld hat Text nicht übernommen");
        showToast("❌ Eingabefeld hat den Builder-Prompt nicht übernommen.", 6500);
        setTimeout(() => setPromptBtnState("idle"), 2500);
        return;
      }

      setPromptBtnState("idle");
      showToast(`✅ Prompt (Frank) erstellt & übernommen. (Domänen: ${domains.length})`, 2200);
    } catch (e) {
      const msg = String(e || "Unbekannter Fehler");
      console.warn("Prompt-Builder Fehler:", msg);
      setPromptBtnState("error", msg);
      showToast("❌ Prompt-Builder Fehler:\n" + msg, 10000);
      setTimeout(() => setPromptBtnState("idle"), 2500);
    }
  }

  // ============================================================
  // Prompt Builder (GENERAL / 12. Klasse)
  // ============================================================
  async function runPromptBuilderGeneral() {
    // ✅ IMMER Composer unten
    const el = getComposerTargetEditable();
    if (!el) {
      setPromptBtn2State("error", "Eingabefeld nicht gefunden");
      showToast("❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.", 6500);
      setTimeout(() => setPromptBtn2State("idle"), 2500);
      return;
    }

    const snap = readPromptText(el);
    if (snap.length < CFG.minCharsForRewrite) {
      setPromptBtn2State("idle");
      showToast(`Text zu kurz (${snap.length}) – Prompt-Builder nicht gestartet.`, 3500);
      return;
    }

    try {
      setPromptBtn2State("working", "Fachdomänen ableiten…");
      showToast("🪄 Prompt-Builder: Fachdomänen ableiten…", 1400);

      const domains = await geminiExtractDomainsRobust(snap);

      setPromptBtn2State("working", "Gerüst bauen…");
      const finalPrompt = buildFinalPromptGeneral(snap, domains);

      const preview = finalPrompt.replace(/\s+/g, " ").slice(0, CFG.previewChars);
      showToast("🧱 Prompt-Builder (Vorschau):\n" + preview + (finalPrompt.length > CFG.previewChars ? " …" : ""), 3500);

      const ok = await setViaPaste(el, finalPrompt);
      if (!ok) {
        setPromptBtn2State("error", "Eingabefeld hat Text nicht übernommen");
        showToast("❌ Eingabefeld hat den Builder-Prompt nicht übernommen.", 6500);
        setTimeout(() => setPromptBtn2State("idle"), 2500);
        return;
      }

      setPromptBtn2State("idle");
      showToast(`✅ Prompt (Allgemein/12. Klasse) erstellt & übernommen. (Domänen: ${domains.length})`, 2300);
    } catch (e) {
      const msg = String(e || "Unbekannter Fehler");
      console.warn("Prompt-Builder (General) Fehler:", msg);
      setPromptBtn2State("error", msg);
      showToast("❌ Prompt-Builder Fehler:\n" + msg, 10000);
      setTimeout(() => setPromptBtn2State("idle"), 2500);
    }
  }

  async function runClearPrompt() {
    const el = getUserTargetEditable();
    if (!el) {
      showToast("❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.", 4500);
      return;
    }

    const ok = await setViaPaste(el, "");
    if (!ok) {
      showToast("❌ Text konnte nicht gelöscht werden.", 4500);
      return;
    }

    showToast("🧹 Sprechblase geleert.", 1600);
  }

  // ============================================================
  // ✅ UI Mount / Repair (Buttons verschwinden nicht mehr)
  // ============================================================
  function getOrCreateButton(id) {
    let b = document.getElementById(id);
    if (b && b.tagName === "BUTTON") return b;
    if (b) try { b.remove(); } catch {}
    b = document.createElement("button");
    b.id = id;
    return b;
  }

  function mountOrRepairUI() {
    if (!document.body) return;

    ensureToast();

    // Mic (unten rechts)
    micBtn = getOrCreateButton(UI_IDS.mic);
    styleRoundButton(micBtn, 0, 0);
    preventFocusSteal(micBtn);
    /* mic icon wird via setMicState gesetzt */
    micBtn.title = "Spracheingabe (Start/Stop)";
    micBtn.onclick = toggleMic;
    if (!micBtn.isConnected) document.body.appendChild(micBtn);

    // Memory (links neben Mic)
    memBtn = getOrCreateButton(UI_IDS.mem);
    styleRoundButton(memBtn, 52, 0);
    preventFocusSteal(memBtn);
    memBtn.textContent = memBtn.textContent || "💾";
    memBtn.title = "Memory-Prompt einfügen";
    memBtn.onclick = runMemoryPrompt;
    if (!memBtn.isConnected) document.body.appendChild(memBtn);

    clearBtn = getOrCreateButton(UI_IDS.clear);
    styleRoundButton(clearBtn, 104, 0);
    preventFocusSteal(clearBtn);
    clearBtn.textContent = clearBtn.textContent || "❌";
    clearBtn.style.color = "#c40000";
    clearBtn.title = "Sprechblase leeren";
    clearBtn.onclick = runClearPrompt;
    if (!clearBtn.isConnected) document.body.appendChild(clearBtn);

    // Prompt-Builder (Frank) über Mic
    promptBtn = getOrCreateButton(UI_IDS.promptFrank);
    styleRoundButton(promptBtn, 0, 52);
    preventFocusSteal(promptBtn);
    promptBtn.textContent = promptBtn.textContent || "✨";
    promptBtn.title = "Prompt (für Frank) einbetten";
    promptBtn.onclick = runPromptBuilder;
    if (!promptBtn.isConnected) document.body.appendChild(promptBtn);

    // Prompt-Builder (Allgemein) darüber
    promptBtn2 = getOrCreateButton(UI_IDS.promptGeneral);
    styleRoundButton(promptBtn2, 0, 104);
    preventFocusSteal(promptBtn2);
    promptBtn2.textContent = promptBtn2.textContent || "🪄";
    promptBtn2.title = "Prompt (allgemein / 12. Klasse) einbetten";
    promptBtn2.onclick = runPromptBuilderGeneral;
    if (!promptBtn2.isConnected) document.body.appendChild(promptBtn2);

    setMicState("idle");
    setMemBtnState("idle");
    setPromptBtnState("idle");
    setPromptBtn2State("idle");
  }

  let ensureScheduled = false;
  function scheduleEnsureUI() {
    if (ensureScheduled) return;
    ensureScheduled = true;
    setTimeout(() => {
      ensureScheduled = false;
      try { mountOrRepairUI(); } catch {}
    }, 250);
  }

  function startUiWatchdog() {
    // MutationObserver: falls React DOM neu baut und Buttons entfernt
    try {
      const mo = new MutationObserver(() => {
        const missing =
          !document.getElementById(UI_IDS.mic) ||
          !document.getElementById(UI_IDS.mem) ||
          !document.getElementById(UI_IDS.clear) ||
          !document.getElementById(UI_IDS.promptFrank) ||
          !document.getElementById(UI_IDS.promptGeneral);

        if (missing) scheduleEnsureUI();
      });
      mo.observe(document.documentElement, { childList: true, subtree: true });
    } catch {}

    // History Hooks (SPA Navigation)
    try {
      const _push = history.pushState;
      const _replace = history.replaceState;

      history.pushState = function () {
        const r = _push.apply(this, arguments);
        scheduleEnsureUI();
        return r;
      };
      history.replaceState = function () {
        const r = _replace.apply(this, arguments);
        scheduleEnsureUI();
        return r;
      };
      window.addEventListener("popstate", scheduleEnsureUI, true);
    } catch {}

    // Low-cost Watchdog
    setInterval(() => {
      const missing =
        !document.getElementById(UI_IDS.mic) ||
        !document.getElementById(UI_IDS.mem) ||
        !document.getElementById(UI_IDS.clear) ||
        !document.getElementById(UI_IDS.promptFrank) ||
        !document.getElementById(UI_IDS.promptGeneral);
      if (missing) scheduleEnsureUI();
    }, 3000);
  }

  // ============================================================
  // Boot
  // ============================================================
  function boot() {
    if (!supportedSpeech) {
      showToast("Mikrofon nicht verfügbar (getUserMedia fehlt).", 7000);
    }

    mountOrRepairUI();
    startUiWatchdog();

    showToast("✅ Script aktiv. 💾 + ❌ + 🎙️ + ✨ + 🪄 unten rechts.\nTipp: erst ins Ziel-Eingabefeld klicken, dann Button drücken.", 3200);
  }

  setTimeout(boot, 350);
  setTimeout(scheduleEnsureUI, 2000);
})();
