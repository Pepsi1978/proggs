// ==UserScript==
// @name         Mistral V.1.3.0
// @namespace    https://chat.mistral.ai/chat
// @version      1.3.0
// @description  Speech-to-Text + Gemini-Korrektur (DE) auf Google Search. Mic-Button fest unten rechts. Kein stilles Fallback. Mit Output-Preview.
// @match        https://chat.mistral.ai/chat*
// @downloadURL  https://raw.githubusercontent.com/Pepsi1978/tampermonkey-skripte/main/scripts/mistral.user.js
// @updateURL    https://raw.githubusercontent.com/Pepsi1978/tampermonkey-skripte/main/scripts/mistral.user.js
// @run-at       document-idle
// @grant        GM_xmlhttpRequest
// @grant        GM.xmlHttpRequest
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM.getValue
// @grant        GM.setValue
// @grant        GM_registerMenuCommand
// @connect      generativelanguage.googleapis.com
// @connect      *.googleapis.com
// @connect      googleapis.com
// @connect      api.groq.com
// ==/UserScript==

(() => {
  "use strict";
    // ── CSS für Mikrofon-Button Animationen ──
    (function(){if(document.getElementById("stt-mic-css"))return;var s=document.createElement("style");s.id="stt-mic-css";s.textContent=".stt-mic-btn{display:flex!important;align-items:center!important;justify-content:center!important;padding:0!important;transition:background .25s,transform .15s,box-shadow .25s!important}.stt-mic-btn:active{transform:scale(.93)!important}.stt-mic-btn[data-state=idle]{background:#2563eb!important;color:#fff!important;border-color:#2563eb!important}.stt-mic-btn[data-state=idle]:hover{background:#1d4ed8!important;transform:scale(1.06)!important}.stt-mic-btn[data-state=listening]{background:#dc2626!important;color:#fff!important;border-color:#dc2626!important;animation:stt-pulse 1.4s ease-in-out infinite!important}.stt-mic-btn[data-state=working]{background:#d97706!important;color:#fff!important;border-color:#d97706!important}.stt-mic-btn[data-state=error]{background:#8b0000!important;color:#fff!important;border-color:#8b0000!important}@keyframes stt-pulse{0%,100%{box-shadow:0 0 0 0 rgba(220,38,38,.45)}50%{box-shadow:0 0 0 14px rgba(220,38,38,0)}}.stt-mic-btn[data-state=working] svg{animation:stt-spin .8s linear infinite}@keyframes stt-spin{to{transform:rotate(360deg)}}#stt-live-preview{position:fixed;bottom:410px;right:16px;max-width:420px;min-width:180px;padding:10px 14px;background:rgba(0,0,0,.88);color:#fff;border-radius:10px;font-size:14px;line-height:1.5;z-index:2147483646;box-shadow:0 4px 20px rgba(0,0,0,.3);max-height:180px;overflow-y:auto;word-wrap:break-word;transition:opacity .25s}#stt-live-preview .stt-pv-label{font-size:11px;color:#aaa;margin-bottom:4px;letter-spacing:.4px}#stt-live-preview .stt-pv-interim{color:#9ca3af;font-style:italic}#stt-live-preview .stt-pv-final{color:#fff}#stt-live-preview .stt-pv-waiting{color:#fbbf24;font-style:italic}";(document.head||document.documentElement).appendChild(s)})();


  // Mehrfach-Injection verhindern (SPA/Reload/History-Edgecases)
  if (window.__TM_MISTRAL_SCRIPT_V103__) return;
  window.__TM_MISTRAL_SCRIPT_V103__ = true;

  const UI_IDS = {
    toast: "tm-mistral-toast",
    mic: "tm-mistral-mic",
    geminiToggle: "tm-mistral-gemini-toggle",
    clear: "tm-mistral-clear",
    prompt: "tm-mistral-prompt",
    prompt2: "tm-mistral-prompt2"
  };

  // ============================================================
  // 🔑 NUR HIER EINTRAGEN
  // ============================================================
  // ⚠️ WICHTIG: API-Key NICHT öffentlich posten. Wenn der Key geleakt ist: rotieren.
  const GEMINI_MODEL = "models/gemini-3.1-flash-lite-preview";
  const GEMINI_THINKING_LEVEL = "MEDIUM";
  const STORAGE_KEYS = {
    geminiApiKey: "tm-mistral-gemini-api-key"
  };

  // ============================================================
  // UI POSITION
  // ============================================================
  const UI_POS = { rightPx: 16, bottomPx: 200 };
  // Android/Edge Mobile-Erkennung (für angepasste Restart-Delays)
  const isMobileAndroid = /Android/i.test(navigator.userAgent);


  // ============================================================
  // GROQ KEY (Whisper Speech-to-Text)
  // ============================================================
  function getGroqKey() {
    let key = "";
    try { key = String(GM_getValue("groqKey", "") || ""); } catch {}
    key = key.trim();
    if (!key) {
      key = (prompt("Bitte Groq API-Key eingeben (kostenlos auf groq.com, wird nur lokal in Tampermonkey gespeichert):") || "").trim();
      if (key) { try { GM_setValue("groqKey", key); } catch {} }
    }
    return key;
  }


  const CFG = {
    minCharsForRewrite: 6,
    requestTimeoutMs: 120000, // 120s
    postPasteDelayMs: 90,
    reactNudgeDelayMs: 60,
    maxRetries: 4,
    retryWaitMs: 1200,
    previewChars: 140,
    grammarMaxOutputTokens: 8192,
    grammarChunkChars: 3500,
    grammarTruncationRatio: 0.85,
    autoGeminiCorrection: true,

    // Groq Whisper Speech-to-Text
    whisperModel: "whisper-large-v3",
    whisperLang: "de"
  };

  // Gespeicherten Auto-Korrektur-Status laden
  if (typeof GM_getValue === "function") {
    CFG.autoGeminiCorrection = GM_getValue("autoGeminiCorrection", true) !== false;
  }

  // Toggle: Auto-Korrektur ein-/ausschalten (Tampermonkey-Menü)
  (function() {
    const reg = typeof GM_registerMenuCommand === "function" ? GM_registerMenuCommand
      : (typeof GM !== "undefined" ? GM?.registerMenuCommand?.bind(GM) : null);
    if (!reg) return;
    reg("🔄 Auto-Korrektur [AN/AUS]", function() {
      CFG.autoGeminiCorrection = !CFG.autoGeminiCorrection;
      if (typeof GM_setValue === "function") GM_setValue("autoGeminiCorrection", CFG.autoGeminiCorrection);
      showToast(CFG.autoGeminiCorrection ? "✅ Auto-Korrektur aktiviert" : "❌ Auto-Korrektur deaktiviert", 3000);
    });
  })();

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

  // Web Speech API für Live-Vorschau (Hybrid-Modus)
  const SpeechRecognitionAPI = window.SpeechRecognition || window.webkitSpeechRecognition;
  const supportedWebSpeech = !!SpeechRecognitionAPI;

  // ── SVG-Icons für Mikrofon-Button (Stil: claude-code-spracheingabe) ──
  const MIC_ICON = {
    mic: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>',
    stop: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>',
    spinner: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 2a10 10 0 0 1 10 10"/></svg>',
    error: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  };

  // ============================================================
  // Toast (reuse statt duplizieren)
  // ============================================================
  let toast = document.getElementById(UI_IDS.toast);
  if (!toast) {
    toast = document.createElement("div");
    toast.id = UI_IDS.toast;
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
    document.body.appendChild(toast);
  }

  let toastTimer = null;
  function showToast(msg, ms = 5500) {
    clearTimeout(toastTimer);
    toast.textContent = msg;
    toast.style.display = "block";
    toastTimer = setTimeout(() => (toast.style.display = "none"), ms);
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


  function normalizeNewlines(s) {
    return String(s || "").replace(/\r\n?/g, "\n");
  }

  function sanitizeForPaste(s) {
    // Wichtig: Newlines behalten, nur "problematische" Zeichen entfernen
    return normalizeNewlines(String(s ?? ""))
      .replace(/[\u200B-\u200D\uFEFF]/g, "")
      .replace(/\u00A0/g, " ")
      .trim();
  }

  // Struktur-sensitiver Compare: wenn Ziel Newlines hat, müssen die im Feld auch erkennbar sein
  function normalizeForStructureCompare(s) {
    return sanitizeForPaste(s)
      .replace(/[ \t]+\n/g, "\n")     // trailing spaces je Zeile weg
      .replace(/\n{3,}/g, "\n\n")     // zu viele Leerzeilen toleranter machen
      .trim();
  }

  function equalTextStructureAware(a, b) {
    const A = normalizeForStructureCompare(a);
    const B = normalizeForStructureCompare(b);
    if (!B) return !A;
    const wantsNewlines = B.includes("\n");
    if (wantsNewlines && !A.includes("\n")) return false;
    return A === B;
  }

  // 🔧 lockerer Textvergleich: ignoriert Zeilenumbrüche/Mehrfachspaces (für Diktat ok)
  function normalizeWhitespaceLoose(s) {
    return cleanText(s).replace(/\s+/g, " ").trim();
  }
  function equalTextLoose(a, b) {
    return normalizeWhitespaceLoose(a) === normalizeWhitespaceLoose(b);
  }

  function isTextInput(el) {
    return el && (el.tagName === "TEXTAREA" || el.tagName === "INPUT");
  }

  function isRoleTextbox(el) {
    return (el?.getAttribute?.("role") || "").toLowerCase() === "textbox";
  }

  function isContentEditableLike(el) {
    if (!el) return false;
    if (el.isContentEditable) return true;
    const ce = (el.getAttribute?.("contenteditable") || "").toLowerCase();
    if (ce === "true" || ce === "" || ce === "plaintext-only") return true;
    return false;
  }

  // WICHTIG: Bei contenteditable innerText bevorzugen, damit Zeilenumbrüche sichtbar bleiben
  function readPromptText(el) {
    if (!el) return "";
    el = resolveEditableTarget(el) || el;
    if (isTextInput(el)) return cleanText(el.value || "");
    if (isContentEditableLike(el) || isRoleTextbox(el)) return cleanText(el.innerText || el.textContent || "");
    return cleanText(el.textContent || el.innerText || "");
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

  function findPrompt() {
    const direct = [
      document.querySelector("textarea#prompt-textarea"),
      document.querySelector("textarea[data-testid='prompt-textarea']"),
      document.querySelector("div#prompt-textarea[contenteditable='true']"),
      document.querySelector("div[data-testid='prompt-textarea'][contenteditable='true']"),
      document.querySelector("form textarea#prompt-textarea"),
      document.querySelector("form textarea[data-testid='prompt-textarea']"),
      document.querySelector("form [contenteditable='true']"),
      document.querySelector("[aria-label*='message' i][contenteditable='true']"),
      document.querySelector("[aria-label*='nachricht' i][contenteditable='true']")
    ].map(resolveEditableTarget).filter(Boolean).find((el) => isVisible(el) && isEditableTarget(el));

    if (direct) return direct;

    const seen = new Set();
    const candidates = [
      ...document.querySelectorAll("textarea"),
      ...document.querySelectorAll("input[type='text']"),
      ...document.querySelectorAll("input:not([type])"),
      ...document.querySelectorAll("[contenteditable='true']"),
      ...document.querySelectorAll("[contenteditable='']"),
      ...document.querySelectorAll("[contenteditable='plaintext-only']"),
      ...document.querySelectorAll("[data-lexical-editor='true']"),
      ...document.querySelectorAll("[role='textbox']")
    ].map(resolveEditableTarget).filter((el) => {
      if (!el || seen.has(el)) return false;
      seen.add(el);
      return isVisible(el) && isEditableTarget(el);
    });

    if (!candidates.length) return null;

    candidates.sort((a, b) => scoreCandidate(b) - scoreCandidate(a));
    return candidates[0] || null;
  }

  // ============================================================
  // 🎯 Targeting: nutze das Feld, das der User fokussiert hat
  // ============================================================
  let lastUserEditable = null;

  // UI Buttons werden später initialisiert, hier schon deklariert:
  let micBtn = null;
  let geminiToggleBtn = null;
  let clearBtn = null;
  let promptBtn = null;
  let promptBtn2 = null;

  function isAriaReadonly(el) {
    return (el?.getAttribute?.("aria-readonly") || "").toLowerCase() === "true";
  }

  function isEditableTarget(el) {
    if (!el) return false;
    if (el === document.body || el === document.documentElement) return false;

    // niemals unsere eigenen UI-Buttons als Eingabefeld nehmen
    if (el === micBtn || el === geminiToggleBtn || el === clearBtn || el === promptBtn || el === promptBtn2) return false;

    const tag = (el.tagName || "").toUpperCase();
    const ariaDisabled = (el.getAttribute?.("aria-disabled") || "").toLowerCase() === "true";

    if (tag === "TEXTAREA") {
      if (el.readOnly || el.disabled || ariaDisabled || isAriaReadonly(el)) return false;
      return true;
    }

    if (tag === "INPUT") {
      if (el.readOnly || el.disabled || ariaDisabled || isAriaReadonly(el)) return false;
      const type = (el.type || "text").toLowerCase();
      const ok = ["text", "search", "email", "url", "tel", "password"].includes(type);
      return ok;
    }

    if (isContentEditableLike(el)) return true;

    if (isRoleTextbox(el)) {
      const inner = el.querySelector?.("textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable=''], [contenteditable='plaintext-only']");
      if (inner) return isEditableTarget(inner);
      return false;
    }

    return false;
  }

  function resolveEditableTarget(el) {
    if (!el || el.nodeType !== 1) return null;

    // Erst ein moegliches inneres Feld aufloesen, damit kein grosser Wrapper genutzt wird.
    const inner = el.querySelector?.("textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable=''], [contenteditable='plaintext-only'], [data-lexical-editor='true'], [role='textbox']");
    if (inner && inner !== el) {
      const resolvedInner = resolveEditableTarget(inner);
      if (resolvedInner && isEditableTarget(resolvedInner)) return resolvedInner;
    }

    if (isTextInput(el) || isContentEditableLike(el)) return el;

    if (isRoleTextbox(el)) {
      const nested = el.querySelector?.("textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable=''], [contenteditable='plaintext-only']");
      if (nested) {
        const resolvedNested = resolveEditableTarget(nested);
        if (resolvedNested && isEditableTarget(resolvedNested)) return resolvedNested;
      }
    }

    let parent = el.parentElement;
    let depth = 0;
    while (parent && depth < 5) {
      if (isTextInput(parent) || isContentEditableLike(parent)) return parent;
      parent = parent.parentElement;
      depth += 1;
    }

    return isEditableTarget(el) ? el : null;
  }

  function pickEditableFromEvent(e) {
    const path = typeof e.composedPath === "function" ? e.composedPath() : null;
    if (Array.isArray(path)) {
      for (const node of path) {
        if (!node || node.nodeType !== 1) continue;
        const resolved = resolveEditableTarget(node);
        if (resolved && isEditableTarget(resolved) && isVisible(resolved)) return resolved;
      }
    }
    const t = resolveEditableTarget(e.target);
    if (isEditableTarget(t) && isVisible(t)) return t;
    return null;
  }

  function rememberEditable(el) {
    const resolved = resolveEditableTarget(el);
    if (isEditableTarget(resolved) && isVisible(resolved)) lastUserEditable = resolved;
  }

  function getUserTargetEditable() {
    const active = resolveEditableTarget(document.activeElement);
    if (isEditableTarget(active) && isVisible(active)) return active;

    const remembered = resolveEditableTarget(lastUserEditable);
    if (isEditableTarget(remembered) && isVisible(remembered)) return remembered;

    const fallback = resolveEditableTarget(findPrompt());
    if (isEditableTarget(fallback) && isVisible(fallback)) return fallback;

    return null;
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
  // PASTE-APPLY (strukturtreu)
  // ============================================================
  async function copyToClipboardFallback(text) {
    const ta = document.createElement("textarea");
    ta.value = text;
    ta.setAttribute("readonly", "true");
    ta.style.position = "fixed";
    ta.style.left = "-9999px";
    ta.style.top = "-9999px";
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    let ok = false;
    try { ok = document.execCommand("copy"); } catch {}
    document.body.removeChild(ta);
    return ok;
  }

  async function ensureClipboard(text) {
    const target = String(text ?? "");
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(target);
        return true;
      }
    } catch {}
    return await copyToClipboardFallback(target);
  }

  function selectAllInside(el) {
    if (!el) return;
    el.focus();

    if (isTextInput(el)) {
      try { el.select(); return; } catch {}
      try { el.setSelectionRange(0, (el.value || "").length); return; } catch {}
      return;
    }

    // contenteditable: nur Inhalt des Elements selektieren
    try {
      const sel = window.getSelection?.();
      if (!sel) throw new Error("no selection");
      sel.removeAllRanges();
      const range = document.createRange();
      range.selectNodeContents(el);
      sel.addRange(range);
    } catch {
      // kein globales selectAll als Fallback, damit keine Seitentexte mitmarkiert werden.
    }
  }

  function escapeHtml(s) {
    return String(s || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function tryDispatchPasteEvent(el, text) {
    try {
      const dt = new DataTransfer();
      dt.setData("text/plain", text);
      const ev = new ClipboardEvent("paste", { bubbles: true, cancelable: true, clipboardData: dt });
      el.dispatchEvent(ev);
      return true;
    } catch {
      return false;
    }
  }

  function tryExecInsertText(text) {
    try {
      return document.execCommand("insertText", false, text);
    } catch {
      return false;
    }
  }

  function tryExecInsertHTML(html) {
    try {
      return document.execCommand("insertHTML", false, html);
    } catch {
      return false;
    }
  }

  async function setViaPaste(el, text) {
    const target = sanitizeForPaste(text);
    el = resolveEditableTarget(el) || el;
    if (!el) return false;

    // Mobile/Edge: direkte Value-Setzung zuerst, da Clipboard/Paste dort oft blockiert ist.
    if (isTextInput(el)) {
      const setter =
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")?.set ||
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set;

      if (setter && (el instanceof HTMLTextAreaElement || el instanceof HTMLInputElement)) setter.call(el, target);
      else el.value = target;

      try { el.focus(); } catch {}
      try { el.setSelectionRange(target.length, target.length); } catch {}
      dispatchReactInput(el, "insertReplacementText", target);
      await sleep(CFG.postPasteDelayMs);
      await reactNudge(el);
      await sleep(40);
      return equalTextStructureAware(readPromptText(el), target) || equalTextLoose(readPromptText(el), target);
    }

    selectAllInside(el);

    let changed = false;

    // 1) Clipboard -> execCommand("paste") (wenn erlaubt)
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(target);
        try { changed = !!document.execCommand("paste"); } catch {}
      }
    } catch {}

    // 2) Clipboard-Fallback Copy -> paste
    if (!changed) {
      const okCopy = await copyToClipboardFallback(target);
      if (okCopy) {
        try { changed = !!document.execCommand("paste"); } catch {}
      }
    }

    await sleep(CFG.postPasteDelayMs);

    // Wenn paste geblockt oder Editor strukturell plattmacht, weiter eskalieren:
    let cur = readPromptText(el);
    if (!equalTextStructureAware(cur, target)) {
      // 3) Paste-Event dispatch (Editor kann eigenen Handler haben)
      tryDispatchPasteEvent(el, target);
      await sleep(CFG.postPasteDelayMs);

      cur = readPromptText(el);
    }

    if (!equalTextStructureAware(cur, target)) {
      // 4) insertText (paste-nahe)
      selectAllInside(el);
      const okIns = tryExecInsertText(target);
      if (okIns) changed = true;
      await sleep(CFG.postPasteDelayMs);
      cur = readPromptText(el);
    }

    if (!equalTextStructureAware(cur, target)) {
      // 5) harte Fallbacks: TextInput direkt setzen / contenteditable als HTML mit <br>
      if (isTextInput(el)) {
        const setter =
          Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")?.set ||
          Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set;

        if (setter && (el instanceof HTMLTextAreaElement || el instanceof HTMLInputElement)) setter.call(el, target);
        else el.value = target;

        dispatchReactInput(el, "insertReplacementText", target);
      } else {
        selectAllInside(el);

        // HTML: Newlines als <br> (Leerzeilen bleiben sichtbar)
        const html = escapeHtml(target).replace(/\n/g, "<br>");
        const okHtml = tryExecInsertHTML(html);

        if (!okHtml) {
          try { el.innerHTML = html; } catch {}
        }

        dispatchReactInput(el, "insertReplacementText", target);
      }

      await sleep(CFG.postPasteDelayMs);
    }

    await reactNudge(el);
    await sleep(40);

    return equalTextStructureAware(readPromptText(el), target);
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
      // Thinking-Parts überspringen (thought: true), nur normalen Text extrahieren
      const out = parts.filter(p => !p?.thought).map(p => (p?.text ?? "")).join("").trim();
      if (out) return out;
    }
    const t = json?.candidates?.[0]?.text;
    if (typeof t === "string" && t.trim()) return t.trim();
    return "";
  }

  async function resolveGeminiApiKey() {
    let key = "";

    if (typeof GM_getValue === "function") {
      key = String(GM_getValue(STORAGE_KEYS.geminiApiKey, "") || "");
    } else if (typeof GM !== "undefined" && typeof GM.getValue === "function") {
      key = String((await GM.getValue(STORAGE_KEYS.geminiApiKey, "")) || "");
    }

    if (!key.trim()) {
      const entered = window.prompt("Bitte gib deinen Gemini API-Key ein (wird lokal in Tampermonkey gespeichert):");
      if (entered && entered.trim()) {
        key = entered.trim();
        if (typeof GM_setValue === "function") {
          GM_setValue(STORAGE_KEYS.geminiApiKey, key);
        } else if (typeof GM !== "undefined" && typeof GM.setValue === "function") {
          await GM.setValue(STORAGE_KEYS.geminiApiKey, key);
        }
      }
    }

    return key.trim();
  }

  async function geminiGenerate(userPrompt, { temperature = 0.05, maxOutputTokens = 2048 } = {}) {
    const apiKey = await resolveGeminiApiKey();
    if (!apiKey) {
      return Promise.reject("API-Key fehlt oder Eingabe abgebrochen.");
    }

    const url = `https://generativelanguage.googleapis.com/v1beta/${GEMINI_MODEL}:generateContent?key=${encodeURIComponent(apiKey)}`;

    const payload = {
      contents: [{ role: "user", parts: [{ text: userPrompt }] }],
      generationConfig: GEMINI_MODEL.includes("gemini-3")
        ? { maxOutputTokens, thinkingConfig: { thinkingLevel: GEMINI_THINKING_LEVEL } }
        : { temperature, maxOutputTokens }
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

  // ---- 1) Intentions-basierte Textkorrektur ----
  function buildGrammarPrompt(text) {
    return `
Du bist ein deutscher Textredakteur für diktierte Spracheingaben.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, präzise und sprachlich hochwertig** zum Ausdruck kommt.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdrücken?
2) Entferne Diktat-Artefakte: Fülllaute ("äh", "ähm", "öhm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
3) Formuliere Sätze so um, dass die erkannte Intention **klar und gut lesbar** wird.
   - Sätze dürfen umstrukturiert werden.
   - Wortwahl darf verbessert werden.
   - Satzgrenzen dürfen neu gesetzt werden.
4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.

GRENZEN (strikt):
- Keine neuen Informationen, Fakten oder Inhalte hinzufügen.
- Keine Vermutungen über nicht Gesagtes.
- Die Intention des Originals muss vollständig erhalten bleiben.
- Sprache: Deutsch.

REGEL:
Gib AUSSCHLIESSLICH den überarbeiteten Text zurück.
Keine Kommentare. Keine Erklärungen. Kein Präfix.

TEXT:
${text}
`.trim();
  }

  function geminiRewriteGrammar(text) {
    return geminiGenerate(buildGrammarPrompt(text), {
      temperature: 0.4,
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
konsensorientiert, erklärt alle Zusammenhänge ganz genau und begründet Empfehlungen ausführlich. Ihr betrachtet die Aufgabe aus allen möglichen verschiedenen Perspektiven. 

Aufgabe:
${taskText}
Falls dir etwas zur Aufgabe nicht klar ist, frage nochmal den Benutzer. Mistral soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
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
konsensorientiert, erklärt alle Zusammenhänge ganz genau und begründet Empfehlungen ausführlich. Ihr betrachtet die Aufgabe aus allen möglichen verschiedenen Perspektiven. 

Aufgabe:
${taskText}
Falls dir etwas zur Aufgabe nicht klar ist frage nochmal den Benutzer. Mistral soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
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
Mindestwortanzahl: 2000, Maximale Wortanzalhl: 10000, Entscheidend ist, dass die Aufgabe/Frage sehr ausführlich erledigt/beantwortet wird.

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
    b.style.zIndex = "9999999";
    b.style.width = "42px";
    b.style.height = "42px";
    b.style.borderRadius = "50%";
    b.style.cursor = "pointer";
    b.style.border = "1px solid rgba(0,0,0,0.2)";
    b.style.background = "white";
    b.style.boxShadow = "0 6px 18px rgba(0,0,0,0.18)";
    b.style.fontSize = "18px";

    b.style.right = `${UI_POS.rightPx + rightOffsetPx}px`;
    b.style.bottom = `${UI_POS.bottomPx + bottomOffsetPx}px`;
    b.style.left = "auto";
    b.style.top = "auto";
  }

  function updateGeminiToggleBtn() {
    if (!geminiToggleBtn) return;
    geminiToggleBtn.textContent = "G";
    geminiToggleBtn.style.fontWeight = "bold";
    geminiToggleBtn.style.fontSize = "18px";
    if (CFG.autoGeminiCorrection) {
      geminiToggleBtn.style.background = "#22c55e";
      geminiToggleBtn.style.color = "#fff";
      geminiToggleBtn.title = "Gemini-Korrektur AN – Klicken zum Deaktivieren";
    } else {
      geminiToggleBtn.style.background = "#ef4444";
      geminiToggleBtn.style.color = "#fff";
      geminiToggleBtn.title = "Gemini-Korrektur AUS – Klicken zum Aktivieren";
    }
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

  // Hybrid-Modus: Web Speech API Live-Vorschau
  let speechRecognition = null;
  let livePreviewEl = null;
  let textBeforeSpeech = "";

  // ── Live-Preview Overlay ──
  function createLivePreview() {
    removeLivePreview();
    livePreviewEl = document.createElement("div");
    livePreviewEl.id = "stt-live-preview";
    livePreviewEl.innerHTML =
      '<div class="stt-pv-label">\u{1F3A4} Live-Vorschau</div>' +
      '<div class="stt-pv-text">\u2026</div>';
    document.body.appendChild(livePreviewEl);
  }

  function updateLivePreview(finalText, interimText) {
    if (!livePreviewEl) return;
    const box = livePreviewEl.querySelector(".stt-pv-text");
    if (!box) return;
    box.innerHTML = "";
    if (finalText) {
      const s = document.createElement("span");
      s.className = "stt-pv-final";
      s.textContent = finalText;
      box.appendChild(s);
    }
    if (interimText) {
      const s = document.createElement("span");
      s.className = "stt-pv-interim";
      s.textContent = (finalText ? " " : "") + interimText;
      box.appendChild(s);
    }
    if (!finalText && !interimText) {
      box.textContent = "\u2026";
    }
    box.scrollTop = box.scrollHeight;
  }

  function setLivePreviewWaiting() {
    if (!livePreviewEl) return;
    const box = livePreviewEl.querySelector(".stt-pv-text");
    if (!box) return;
    box.innerHTML = '<span class="stt-pv-waiting">Whisper transkribiert\u2026</span>';
  }

  function removeLivePreview() {
    if (livePreviewEl) {
      livePreviewEl.remove();
      livePreviewEl = null;
    }
  }

  function startWebSpeech() {
    if (!supportedWebSpeech) return;
    try {
      speechRecognition = new SpeechRecognitionAPI();
      speechRecognition.lang = CFG.whisperLang || "de";
      speechRecognition.continuous = true;
      speechRecognition.interimResults = true;
      speechRecognition.maxAlternatives = 1;

      speechRecognition.onresult = (event) => {
        let finalT = "";
        let interimT = "";
        for (let i = 0; i < event.results.length; i++) {
          if (event.results[i].isFinal) {
            finalT += event.results[i][0].transcript;
          } else {
            interimT += event.results[i][0].transcript;
          }
        }
        // Live-Text direkt ins Eingabefeld schreiben
        const el = getUserTargetEditable();
        if (el) {
          const preview = finalT + interimT;
          const spacer = textBeforeSpeech && !textBeforeSpeech.endsWith(" ") && !textBeforeSpeech.endsWith("\n") ? " " : "";
          setViaPaste(el, textBeforeSpeech + spacer + preview);
        }
      };

      speechRecognition.onerror = (event) => {
        if (event.error === "no-speech" || event.error === "aborted") return;
        console.log("[STT] Web Speech API Fehler (unkritisch):", event.error);
      };

      speechRecognition.onend = () => {
        if (wantsRecording && speechRecognition) {
          try { speechRecognition.start(); } catch {}
        }
      };

      speechRecognition.start();
    } catch (e) {
      console.log("[STT] Web Speech API nicht verfügbar:", e);
      speechRecognition = null;
    }
  }

  function stopWebSpeech() {
    if (speechRecognition) {
      const ref = speechRecognition;
      speechRecognition = null;
      try { ref.abort(); } catch {}
    }
  }

  function groqTranscribe(audioBlob) {
    const groqKey = getGroqKey();
    if (!groqKey) {
      setMicState("error", "Groq API-Key fehlt");
      removeLivePreview();
      showToast("❌ Groq API-Key fehlt.\nTampermonkey-Menü → Groq-Key setzen.", 8000);
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
      removeLivePreview();
      showToast("❌ GM Request API fehlt (Tampermonkey Grants).", 7000);
      setTimeout(() => setMicState("idle"), 3000);
      return;
    }

    setMicState("working", "Whisper transkribiert…");
    setLivePreviewWaiting();
    showToast("🎧 Whisper transkribiert…", 2000);

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
          removeLivePreview();
          showToast("❌ Groq Fehler:\n" + msg, 9000);
          setTimeout(() => setMicState("idle"), 3000);
          return;
        }

        const text = (r.responseText || "").trim();
        if (!text) {
          setMicState("idle");
          removeLivePreview();
          showToast("⚠️ Keine Sprache erkannt.", 3000);
          return;
        }

        const el = getUserTargetEditable();
        if (!el) {
          setMicState("error", "Kein Eingabefeld");
          removeLivePreview();
          showToast("❌ Eingabefeld nicht gefunden.", 5000);
          setTimeout(() => setMicState("idle"), 2500);
          return;
        }

        const base = textBeforeSpeech;
        const spacer = base && !base.endsWith(" ") && !base.endsWith("\n") ? " " : "";
        let combined = base + spacer + text;
        let corrected = false;

        // Gemini Intentions-Korrektur (falls aktiviert)
        if (CFG.autoGeminiCorrection && text.length >= CFG.minCharsForRewrite) {
          try {
            setMicState("working", "Gemini korrigiert…");
            showToast("✨ Gemini korrigiert…", 3000);
            const result = await geminiRewriteGrammarSmart(text);
            if (result && result.trim().length > 0) {
              combined = base + spacer + result.trim();
              corrected = true;
            }
          } catch (err) {
            console.warn("[STT] Gemini-Korrektur fehlgeschlagen:", err);
            showToast("⚠️ Gemini-Korrektur fehlgeschlagen. Roher Text wird verwendet.", 4000);
          }
        }

        const ok = await setViaPaste(el, combined);
        removeLivePreview();
        if (ok) {
          setMicState("idle");
          const preview = text.length > 80 ? text.slice(0, 80) + "…" : text;
          showToast(corrected ? "✨ Korrigiert & eingefügt" : ("✅ " + preview), 3000);
        } else {
          setMicState("error", "Text nicht übernommen");
          showToast("❌ Eingabefeld hat Text nicht übernommen.", 5000);
          setTimeout(() => setMicState("idle"), 2500);
        }
      },

      onerror: () => {
        setMicState("error", "Netzwerk-Fehler");
        removeLivePreview();
        showToast("❌ Netzwerk-Fehler bei Groq API.\nHinweise: @connect, Adblock/Privacy, VPN/Proxy.", 7000);
        setTimeout(() => setMicState("idle"), 3000);
      },
      ontimeout: () => {
        setMicState("error", "Timeout");
        removeLivePreview();
        showToast("❌ Groq API Timeout.", 5000);
        setTimeout(() => setMicState("idle"), 3000);
      }
    });
  }

  function startListening() {
    if (!supportedSpeech) return;

    const t = getUserTargetEditable();
    if (!t) {
      showToast("⚠️ Kein fokussiertes Eingabefeld. Tipp: zuerst ins Ziel-Feld tippen.", 3500);
    } else {
      rememberEditable(t);
    }

    // Text vor Aufnahme merken (für Hybrid-Kombination mit Whisper)
    textBeforeSpeech = t ? readPromptText(t) : "";

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
            removeLivePreview();
            return;
          }

          const audioBlob = new Blob(audioChunks, { type: mediaRecorder.mimeType || "audio/webm" });
          audioChunks = [];
          groqTranscribe(audioBlob);
        };

        mediaRecorder.start(1000);
        setMicState("listening");
        showToast("🎙️ Aufnahme läuft… (Stop über ⏹️)", 1500);

        // Web Speech API parallel starten für Live-Vorschau
        startWebSpeech();
      })
      .catch(err => {
        wantsRecording = false;
        setMicState("error", String(err));
        showToast("❌ Mikrofon-Zugriff fehlgeschlagen:\n" + String(err), 8000);
        setTimeout(() => setMicState("idle"), 3000);
      });
  }

  function stopListening() {
    wantsRecording = false;

    // Web Speech API stoppen
    stopWebSpeech();

    if (mediaRecorder && mediaRecorder.state !== "inactive") {
      setMicState("working", "Aufnahme beendet…");
      setLivePreviewWaiting();
      mediaRecorder.stop();
    } else {
      if (audioStream) {
        audioStream.getTracks().forEach(t => t.stop());
        audioStream = null;
      }
      removeLivePreview();
      setMicState("idle");
    }
  }

  function toggleMic() {
    if (!supportedSpeech) {
      showToast("❌ Mikrofon nicht verfügbar (getUserMedia).", 5000);
      return;
    }
    if (!wantsRecording) startListening();
    else stopListening();
  }

  // ============================================================
  // Prompt Builder (Frank)
  // ============================================================
  async function runPromptBuilder() {
    const el = getUserTargetEditable();
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

      const target = getUserTargetEditable() || el;
      const ok = await setViaPaste(target, finalPrompt);

      if (!ok) {
        await ensureClipboard(finalPrompt);
        setPromptBtnState("idle");
        showToast("⚠️ Automatisches Einfügen wurde blockiert/uneindeutig erkannt.\nPrompt liegt in der Zwischenablage → bitte manuell einfügen (Strg+V).", 9000);
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
    const el = getUserTargetEditable();
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

      const target = getUserTargetEditable() || el;
      const ok = await setViaPaste(target, finalPrompt);

      if (!ok) {
        await ensureClipboard(finalPrompt);
        setPromptBtn2State("idle");
        showToast("⚠️ Automatisches Einfügen wurde blockiert/uneindeutig erkannt.\nPrompt liegt in der Zwischenablage → bitte manuell einfügen (Strg+V).", 9000);
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
  // Boot + UI-Resilience (SPA)
  // ============================================================
  function boot({ silent = false } = {}) {
    if (!document.body) return;

    if (!supportedSpeech && !silent) {
      showToast("Mikrofon nicht verfügbar (getUserMedia fehlt).", 7000);
    }

    // MIC
    micBtn = document.getElementById(UI_IDS.mic);
    if (!micBtn) {
      micBtn = document.createElement("button");
      micBtn.id = UI_IDS.mic;
      styleRoundButton(micBtn, 0, 0);
      document.body.appendChild(micBtn);
    } else {
      styleRoundButton(micBtn, 0, 0);
    }
    micBtn.innerHTML = MIC_ICON.mic; micBtn.setAttribute("data-state", "idle"); micBtn.classList.add("stt-mic-btn");
    micBtn.title = "Spracheingabe (Start/Stop)";
    micBtn.onclick = toggleMic;

    // GEMINI TOGGLE
    geminiToggleBtn = document.getElementById(UI_IDS.geminiToggle);
    if (!geminiToggleBtn) {
      geminiToggleBtn = document.createElement("button");
      geminiToggleBtn.id = UI_IDS.geminiToggle;
      styleRoundButton(geminiToggleBtn, 0, 52);
      geminiToggleBtn.addEventListener("click", async () => {
        CFG.autoGeminiCorrection = !CFG.autoGeminiCorrection;
        await Promise.resolve(GM_setValue("autoGeminiCorrection", CFG.autoGeminiCorrection));
        updateGeminiToggleBtn();
        showToast(CFG.autoGeminiCorrection ? "✅ Gemini-Korrektur aktiviert" : "❌ Gemini-Korrektur deaktiviert", 2000);
      });
      document.body.appendChild(geminiToggleBtn);
    } else {
      styleRoundButton(geminiToggleBtn, 0, 52);
    }

    clearBtn = document.getElementById(UI_IDS.clear);
    if (!clearBtn) {
      clearBtn = document.createElement("button");
      clearBtn.id = UI_IDS.clear;
      styleRoundButton(clearBtn, 0, 104);
      document.body.appendChild(clearBtn);
    } else {
      styleRoundButton(clearBtn, 0, 104);
    }
    clearBtn.textContent = "❌";
    clearBtn.style.color = "#c40000";
    clearBtn.title = "Sprechblase leeren";
    clearBtn.onclick = runClearPrompt;

    // PROMPT Frank
    promptBtn = document.getElementById(UI_IDS.prompt);
    if (!promptBtn) {
      promptBtn = document.createElement("button");
      promptBtn.id = UI_IDS.prompt;
      styleRoundButton(promptBtn, 0, 156);
      document.body.appendChild(promptBtn);
    } else {
      styleRoundButton(promptBtn, 0, 156);
    }
    promptBtn.textContent = "✨";
    promptBtn.title = "Prompt (für Frank) einbetten";
    promptBtn.onclick = runPromptBuilder;

    // PROMPT General
    promptBtn2 = document.getElementById(UI_IDS.prompt2);
    if (!promptBtn2) {
      promptBtn2 = document.createElement("button");
      promptBtn2.id = UI_IDS.prompt2;
      styleRoundButton(promptBtn2, 0, 208);
      document.body.appendChild(promptBtn2);
    } else {
      styleRoundButton(promptBtn2, 0, 208);
    }
    promptBtn2.textContent = "🪄";
    promptBtn2.title = "Prompt (allgemein / 12. Klasse) einbetten";
    promptBtn2.onclick = runPromptBuilderGeneral;

    setMicState("idle");
    updateGeminiToggleBtn();
    setPromptBtnState("idle");
    setPromptBtn2State("idle");

    if (!silent) {
      showToast("✅ Script aktiv. 🎙️ + G + ❌ + ✨ + 🪄 unten rechts.\nTipp: erst ins Ziel-Eingabefeld klicken, dann 🎙️.", 2800);
    }
  }

  // Initial boot
  boot({ silent: false });

  // SPA / React kann DOM austauschen: UI regelmäßig sicherstellen
  const ensureUI = () => {
    const hasMic = !!document.getElementById(UI_IDS.mic);
    const hasGT = !!document.getElementById(UI_IDS.geminiToggle);
    const hasClear = !!document.getElementById(UI_IDS.clear);
    const hasP1 = !!document.getElementById(UI_IDS.prompt);
    const hasP2 = !!document.getElementById(UI_IDS.prompt2);
    if (!hasMic || !hasGT || !hasClear || !hasP1 || !hasP2) boot({ silent: true });
  };

  try {
    const mo = new MutationObserver(() => ensureUI());
    mo.observe(document.documentElement, { childList: true, subtree: true });
  } catch {}

  setInterval(ensureUI, 1200);

})();
