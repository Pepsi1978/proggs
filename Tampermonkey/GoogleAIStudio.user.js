// ==UserScript==
// @name         AI Studio V.1.4.4
// @namespace    https://aistudio.google.com/prompts/new_chat
// @version      1.4.4
// @updateURL    https://raw.githubusercontent.com/Pepsi1978/proggs/main/Tampermonkey/GoogleAIStudio.user.js
// @downloadURL  https://raw.githubusercontent.com/Pepsi1978/proggs/main/Tampermonkey/GoogleAIStudio.user.js
// @description  Speech-to-Text + Gemini-Korrektur (DE) auf ChatGPT. Mic-Button unten links. Zwei Prompt-Builder Buttons (Frank + für jedermann) über dem Mic. Kein stilles Fallback. Mit Output-Preview.
// @match        https://aistudio.google.com/*
// @match        https://www.aistudio.google.com/*
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
// @updateURL    https://raw.githubusercontent.com/Pepsi1978/proggs/main/Tampermonkey/GoogleAIStudio.user.js
// @downloadURL  https://raw.githubusercontent.com/Pepsi1978/proggs/main/Tampermonkey/GoogleAIStudio.user.js
// ==/UserScript==

(() => {
  "use strict";
        // ── CSS für Mikrofon-Button Animationen (mit Fehlerbehandlung) ──
    try {
      (function(){if(document.getElementById("stt-mic-css"))return;var s=document.createElement("style");s.id="stt-mic-css";s.textContent=".stt-mic-btn{display:flex!important;align-items:center!important;justify-content:center!important;padding:0!important;transition:background .25s,transform .15s,box-shadow .25s!important}.stt-mic-btn:active{transform:scale(.93)!important}.stt-mic-btn[data-state=idle]{background:#2563eb!important;color:#fff!important;border-color:#2563eb!important}.stt-mic-btn[data-state=idle]:hover{background:#1d4ed8!important;transform:scale(1.15)!important}.stt-mic-btn[data-state=listening]{background:#dc2626!important;color:#fff!important;border-color:#dc2626!important;animation:stt-pulse 1.4s ease-in-out infinite!important}.stt-mic-btn[data-state=working]{background:#d97706!important;color:#fff!important;border-color:#d97706!important}.stt-mic-btn[data-state=error]{background:#8b0000!important;color:#fff!important;border-color:#8b0000!important}@keyframes stt-pulse{0%,100%{box-shadow:0 0 0 0 rgba(220,38,38,.45)}50%{box-shadow:0 0 0 14px rgba(220,38,38,0)}}.stt-mic-btn[data-state=working] svg{animation:stt-spin .8s linear infinite}@keyframes stt-spin{to{transform:rotate(360deg)}}#stt-live-preview{position:fixed;bottom:410px;left:16px;max-width:420px;min-width:180px;padding:10px 14px;background:rgba(0,0,0,.88);color:#fff;border-radius:10px;font-size:14px;line-height:1.5;z-index:2147483646;box-shadow:0 4px 20px rgba(0,0,0,.3);max-height:180px;overflow-y:auto;word-wrap:break-word;transition:opacity .25s}#stt-live-preview .stt-pv-label{font-size:11px;color:#aaa;margin-bottom:4px;letter-spacing:.4px}#stt-live-preview .stt-pv-interim{color:#9ca3af;font-style:italic}#stt-live-preview .stt-pv-final{color:#fff}#stt-live-preview .stt-pv-waiting{color:#fbbf24;font-style:italic}";try{(document.head||document.documentElement).appendChild(s);}catch(e){document.documentElement.appendChild(s);}})();
    } catch(e) { /* CSS-Animation nicht verfügbar, Buttons funktionieren trotzdem */ }


  // ============================================================
  // 🔑 API-Key Verwaltung (Tampermonkey Storage)
  // ============================================================
  // ⚠️ WICHTIG: API-Key NICHT öffentlich posten. Wenn der Key geleakt ist: rotieren.
  const GEMINI_API_KEY_DEFAULT = "hier".trim();
  const API_KEY_STORAGE_KEY = "googleAiStudio.geminiApiKey";
  const GEMINI_MODEL = "models/gemini-3.1-flash-lite-preview";
  const GEMINI_THINKING_LEVEL = "MEDIUM";

  // ============================================================
  // UI POSITION
  // ============================================================
  const UI_POS = { leftPx: 27, bottomPx: 124 };
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
    postPasteDelayMs: 80,
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

  // ── Icons für Mikrofon-Button (textContent-safe für Trusted Types Seiten) ──
  const MIC_ICON = {
    mic: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>',
    stop: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>',
    spinner: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 2a10 10 0 0 1 10 10"/></svg>',
    error: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
  };

  // ============================================================
  // Toast
  // ============================================================
  const toast = document.createElement("div");
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
  try { if (document.body) document.body.appendChild(toast); } catch(e) {}

  let toastTimer = null;
  function showToast(msg, ms = 5500) {
    if (!toast.isConnected && document.body) { try { document.body.appendChild(toast); } catch(e) {} }
    clearTimeout(toastTimer);
    toast.textContent = msg;
    toast.style.display = "block";
    toastTimer = setTimeout(() => (toast.style.display = "none"), ms);
  }

  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  // ============================================================
  // API-Key Storage (Tampermonkey)
  // ============================================================
  function normalizeApiKey(value) {
    return String(value || "").trim();
  }

  function readStoredApiKey() {
    if (typeof GM_getValue === "function") {
      return normalizeApiKey(GM_getValue(API_KEY_STORAGE_KEY, ""));
    }
    return normalizeApiKey(localStorage.getItem(API_KEY_STORAGE_KEY));
  }

  function writeStoredApiKey(value) {
    const cleanKey = normalizeApiKey(value);
    if (typeof GM_setValue === "function") {
      GM_setValue(API_KEY_STORAGE_KEY, cleanKey);
    } else {
      localStorage.setItem(API_KEY_STORAGE_KEY, cleanKey);
    }
    return cleanKey;
  }

  function requestApiKey() {
    const input = window.prompt("Bitte Gemini API-Key eingeben (wird in Tampermonkey gespeichert):", "");
    if (!input) {
      showToast("API-Key fehlt. Bitte in Tampermonkey eingeben.");
      return "";
    }
    const stored = writeStoredApiKey(input);
    if (!stored) {
      showToast("API-Key fehlt oder leer.");
      return "";
    }
    showToast("API-Key gespeichert.");
    return stored;
  }

  function getGeminiApiKey() {
    return readStoredApiKey() || GEMINI_API_KEY_DEFAULT;
  }

  function resolveGeminiApiKey() {
    let key = getGeminiApiKey();
    if (!key || key.includes("PASTE_YOUR_KEY_HERE") || key.toLowerCase().includes("key hier") || key.toLowerCase() === "hier") {
      key = requestApiKey();
    }
    return key;
  }

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

  function readPromptText(el) {
    if (!el) return "";

    el = resolveEditableTarget(el) || el;

    if (isTextInput(el)) {
      return cleanText(el.value || "");
    }

    // contenteditable bevorzugt ueber innerText lesen, damit Zeilenumbrueche erhalten bleiben.
    if (isContentEditableLike(el) || isRoleTextbox(el)) {
      return cleanText(el.innerText || el.textContent || "");
    }

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
    ].map(resolveEditableTarget)
      .filter((el) => {
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

function isRoleTextbox(el) {
  return (el?.getAttribute?.("role") || "").toLowerCase() === "textbox";
}

function hasContentEditableEnabled(el) {
  const raw = el?.getAttribute?.("contenteditable");
  if (raw == null) return false;
  const ce = String(raw).toLowerCase();
  return ce === "" || ce === "true" || ce === "plaintext-only";
}

function isContentEditableLike(el) {
  if (!el) return false;
  return !!el.isContentEditable || hasContentEditableEnabled(el);
}

function isAriaReadonly(el) {
  return (el?.getAttribute?.("aria-readonly") || "").toLowerCase() === "true";
}

function isEditableTarget(el) {
  if (!el) return false;
  if (el === document.body || el === document.documentElement) return false;

  // niemals unsere eigenen UI-Buttons als Eingabefeld nehmen
  if ((typeof micBtn !== "undefined" && el === micBtn) || (typeof geminiToggleBtn !== "undefined" && el === geminiToggleBtn) || (typeof clearBtn !== "undefined" && el === clearBtn) || (typeof promptBtn !== "undefined" && el === promptBtn) || (typeof promptBtn2 !== "undefined" && el === promptBtn2) || (typeof memBtn !== "undefined" && el === memBtn)) return false;

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

  // Erst ein moegliches inneres, praeziseres Feld suchen, damit kein grosser Wrapper erwischt wird.
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
      if (resolved && isVisible(resolved) && isEditableTarget(resolved)) return resolved;
    }
  }

  const resolvedTarget = resolveEditableTarget(e.target);
  if (resolvedTarget && isVisible(resolvedTarget) && isEditableTarget(resolvedTarget)) return resolvedTarget;
  return null;
}

function rememberEditable(el) {
  const resolved = resolveEditableTarget(el);
  if (resolved && isVisible(resolved) && isEditableTarget(resolved)) lastUserEditable = resolved;
}

function getUserTargetEditable() {
  const active = resolveEditableTarget(document.activeElement);
  if (active && isVisible(active) && isEditableTarget(active)) return active;

  const remembered = resolveEditableTarget(lastUserEditable);
  if (remembered && isVisible(remembered) && isEditableTarget(remembered)) return remembered;

  const fallback = resolveEditableTarget(findPrompt());
  if (fallback && isVisible(fallback) && isEditableTarget(fallback)) return fallback;

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
  // PASTE-APPLY
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

function normalizeForCompare(s) {
  return String(s || "")
    .replace(/[\u200B-\u200D\uFEFF]/g, "")
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n[ \t]+/g, "\n")
    .replace(/\u00A0/g, " ")
    .trim();
}

function setNativeValue(el, val) {
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
    el.value = v;
  } catch {
    try { el.value = v; } catch {}
  }
}

function escapeHtml(s) {
  return String(s || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function moveCaretToEnd(el) {
  try {
    el.focus();
    const sel = window.getSelection?.();
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
  const html = escapeHtml(String(text ?? "")).replace(/\n/g, "<br>");

  try {
    el.focus();
    el.innerHTML = html;
    moveCaretToEnd(el);
  } catch {
    try { el.textContent = text; } catch {}
  }

  dispatchReactInput(el, "insertReplacementText", String(text ?? ""));
}

async function setViaPaste(el, text) {
  const target = String(text ?? "").replace(/\r\n/g, "\n").replace(/\r/g, "\n");
  el = resolveEditableTarget(el) || el;
  if (!el) return false;

  try { el.focus(); } catch {}

  if (isTextInput(el)) {
    setNativeValue(el, target);
    try { el.setSelectionRange(target.length, target.length); } catch {}
    dispatchReactInput(el, "insertReplacementText", target);
  } else {
    setContentEditablePreserveNewlines(el, target);
  }

  await sleep(CFG.postPasteDelayMs);
  await reactNudge(el);
  await sleep(40);

  const got = normalizeForCompare(readPromptText(el));
  const want = normalizeForCompare(target);
  if (got === want) return true;

  // Browser/Editor duerfen Whitespace leicht normalisieren.
  if (got.replace(/\s+/g, " ") === want.replace(/\s+/g, " ")) return true;

  // Letzter Versuch ueber Clipboard-Weg (falls Editor nur Paste verarbeitet).
  let pasted = false;
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(target);
      try { pasted = document.execCommand("paste"); } catch {}
    }
  } catch {}

  if (!pasted) {
    const okCopy = await copyToClipboardFallback(target);
    if (okCopy) {
      try { pasted = document.execCommand("paste"); } catch {}
    }
  }

  if (pasted) {
    await sleep(CFG.postPasteDelayMs);
    await reactNudge(el);
    await sleep(40);
  }

  const gotAfter = normalizeForCompare(readPromptText(el));
  if (gotAfter === want) return true;
  return gotAfter.replace(/\s+/g, " ") === want.replace(/\s+/g, " ");
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

  function geminiGenerate(userPrompt, { temperature = 0.05, maxOutputTokens = 2048 } = {}) {
    const apiKey = resolveGeminiApiKey();
    if (!apiKey) {
      return Promise.reject("API-Key fehlt oder Platzhalter nicht ersetzt.");
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
Falls dir etwas zur Aufgabe nicht klar ist, frage nochmal den Benutzer. Gemeni soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
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
Falls dir etwas zur Aufgabe nicht klar ist frage nochmal den Benutzer. Gemeni soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
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
  // ── Button IDs (Watchdog) ──
  const UI_IDS = {
    mic:           "tm-aistudio-mic",
    geminiToggle:  "tm-aistudio-gemini-toggle",
    clear:         "tm-aistudio-clear",
    promptFrank:   "tm-aistudio-prompt",
    promptGeneral: "tm-aistudio-prompt2",
  };

  function getOrCreateButton(id) {
    let b = document.getElementById(id);
    if (!b) { b = document.createElement("button"); b.id = id; }
    return b;
  }

  function setUiStyle(el, prop, value) {
    if (!el) return;
    el.style.setProperty(prop, value, "important");
  }

  function enforceUiButtonVisibility(button) {
    if (!button) return;
    setUiStyle(button, "display", "flex");
    setUiStyle(button, "align-items", "center");
    setUiStyle(button, "justify-content", "center");
    setUiStyle(button, "visibility", "visible");
    setUiStyle(button, "opacity", "1");
    setUiStyle(button, "pointer-events", "auto");
    setUiStyle(button, "appearance", "none");
    setUiStyle(button, "-webkit-appearance", "none");
    setUiStyle(button, "box-sizing", "border-box");
    setUiStyle(button, "padding", "0");
    setUiStyle(button, "margin", "0");
    setUiStyle(button, "overflow", "visible");
    setUiStyle(button, "line-height", "1");
    setUiStyle(button, "font-family", "Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif");
    setUiStyle(button, "user-select", "none");
  }

  function styleRoundButton(b, rightOffsetPx = 0, bottomOffsetPx = 0) {
    b.type = "button";
    setUiStyle(b, "position", "fixed");
    setUiStyle(b, "z-index", "2147483647");
    setUiStyle(b, "width", "42px");
    setUiStyle(b, "height", "42px");
    setUiStyle(b, "border-radius", "50%");
    setUiStyle(b, "cursor", "pointer");
    setUiStyle(b, "border", "1px solid rgba(0,0,0,0.2)");
    setUiStyle(b, "background", "white");
    b.style.boxShadow = "0 6px 18px rgba(0,0,0,0.18)"; // kein !important – Animation muss box-shadow überschreiben können
    setUiStyle(b, "font-size", "18px");
    setUiStyle(b, "transition", "transform 0.15s ease, box-shadow 0.25s ease");
    setUiStyle(b, "transform-origin", "center center");
    setUiStyle(b, "transform", "scale(1)");

    setUiStyle(b, "left", `${UI_POS.leftPx + rightOffsetPx}px`);
    setUiStyle(b, "bottom", `${UI_POS.bottomPx + bottomOffsetPx}px`);
    setUiStyle(b, "right", "auto");
    setUiStyle(b, "top", "auto");
    enforceUiButtonVisibility(b);

    b.addEventListener("mouseenter", () => { b.style.setProperty("transform", "scale(1.15)", "important"); });
    b.addEventListener("mouseleave", () => { b.style.setProperty("transform", "scale(1)", "important"); });
  }



  // ── Trusted-Types-sicheres SVG-Icon setzen ──────────────────────────────────
  function setSvgIcon(el, svgStr) {
    try {
      el.innerHTML = svgStr; // Funktioniert auf den meisten Seiten
    } catch(e) {
      // Trusted Types Fallback: SVG via DOMParser (kein innerHTML in aktuellem Dokument)
      try {
        el.textContent = "";
        const doc = new DOMParser().parseFromString(svgStr, "image/svg+xml");
        const node = doc.documentElement;
        if (node && node.tagName === "svg") el.appendChild(document.adoptNode(node));
      } catch(e2) {
        // Letzter Fallback: Emoji (immer sicher)
        el.textContent = svgStr.includes("M12 1a3") ? "🎙️"
                       : svgStr.includes("<rect") ? "⏹️"
                       : svgStr.includes("M12 2a10") ? "⏳" : "⚠️";
      }
    }
  }

  // ── Web Animations API (Fallback wenn CSS @keyframes nicht geladen) ─────────
  let _sttAnim = null;
  function _sttStartPulse(el) {
    _sttStopAnims();
    if (!el || !el.animate) return;
    try {
      _sttAnim = el.animate([
        { boxShadow: "0 0 0 0 rgba(220,38,38,0.45)" },
        { boxShadow: "0 0 0 14px rgba(220,38,38,0)" },
        { boxShadow: "0 0 0 0 rgba(220,38,38,0.45)" }
      ], { duration: 1400, iterations: Infinity, easing: "ease-in-out" });
    } catch(e) {}
  }
  function _sttStartSpin(el) {
    _sttStopAnims();
    // Nur das SVG-Kind drehen (wie CSS .stt-mic-btn[data-state=working] svg { animation: stt-spin })
    const target = (el && el.querySelector("svg")) || el;
    if (!target || !target.animate) return;
    try {
      _sttAnim = target.animate([
        { transform: "rotate(0deg)" },
        { transform: "rotate(360deg)" }
      ], { duration: 800, iterations: Infinity, easing: "linear" });
    } catch(e) {}
  }
  function _sttStopAnims() {
    if (_sttAnim) { try { _sttAnim.cancel(); } catch(e) {} _sttAnim = null; }
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
      _sttStopAnims(); // Laufende Animationen stoppen

      if (state === "listening") {
        setSvgIcon(micBtn, MIC_ICON.stop);
        micBtn.setAttribute("data-state", "listening");
        setUiStyle(micBtn, "background", "#dc2626");
        setUiStyle(micBtn, "color", "#fff");
        setUiStyle(micBtn, "border-color", "#dc2626");
        micBtn.title = "Spracheingabe läuft – klicken zum Stop";
        _sttStartPulse(micBtn); // Pulsierendes rotes Leuchten (wie CSS stt-pulse)
        return;
      }
      if (state === "working") {
        setSvgIcon(micBtn, MIC_ICON.spinner);
        micBtn.setAttribute("data-state", "working");
        setUiStyle(micBtn, "background", "#d97706");
        setUiStyle(micBtn, "color", "#fff");
        setUiStyle(micBtn, "border-color", "#d97706");
        micBtn.title = msg || "Bereinigung läuft…";
        _sttStartSpin(micBtn); // Spinner-SVG rotiert (wie CSS stt-spin)
        return;
      }
      if (state === "error") {
        setSvgIcon(micBtn, MIC_ICON.error);
        micBtn.setAttribute("data-state", "error");
        setUiStyle(micBtn, "background", "#8b0000");
        setUiStyle(micBtn, "color", "#fff");
        setUiStyle(micBtn, "border-color", "#8b0000");
        micBtn.title = msg || "Fehler";
        return;
      }
      // idle
      setSvgIcon(micBtn, MIC_ICON.mic);
      micBtn.setAttribute("data-state", "idle");
      setUiStyle(micBtn, "background", "#2563eb");
      setUiStyle(micBtn, "color", "#fff");
      setUiStyle(micBtn, "border-color", "#2563eb");
      micBtn.title = supportedSpeech ? "Spracheingabe (Start/Stop)" : "Speech API nicht verfügbar";
    }

  function setPromptBtnState(state, msg = "") {
    if (!promptBtn) return;

    if (state === "working") {
      promptBtn.textContent = "⏳";
      setUiStyle(promptBtn, "background", "#444");
      setUiStyle(promptBtn, "color", "white");
      promptBtn.title = msg || "Prompt wird gebaut…";
      return;
    }
    if (state === "error") {
      promptBtn.textContent = "⚠️";
      setUiStyle(promptBtn, "background", "#8b0000");
      setUiStyle(promptBtn, "color", "white");
      promptBtn.title = msg || "Fehler";
      return;
    }

    promptBtn.textContent = "✨";
    setUiStyle(promptBtn, "background", "white");
    setUiStyle(promptBtn, "color", "black");
    promptBtn.title = "Prompt (für Frank) einbetten";
  }

  function setPromptBtn2State(state, msg = "") {
    if (!promptBtn2) return;

    if (state === "working") {
      promptBtn2.textContent = "⏳";
      setUiStyle(promptBtn2, "background", "#444");
      setUiStyle(promptBtn2, "color", "white");
      promptBtn2.title = msg || "Prompt wird gebaut…";
      return;
    }
    if (state === "error") {
      promptBtn2.textContent = "⚠️";
      setUiStyle(promptBtn2, "background", "#8b0000");
      setUiStyle(promptBtn2, "color", "white");
      promptBtn2.title = msg || "Fehler";
      return;
    }

    promptBtn2.textContent = "🪄";
    setUiStyle(promptBtn2, "background", "white");
    setUiStyle(promptBtn2, "color", "black");
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

  // Stubs – kein Overlay, Text wird direkt ins Eingabefeld geschrieben
  function removeLivePreview() {}
  function setLivePreviewWaiting() {}

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
  // Boot
  // ============================================================
  function mountOrRepairUI() {
    if (!document.body) return;

    micBtn = getOrCreateButton(UI_IDS.mic);
    styleRoundButton(micBtn, 0, 0);
    if (!micBtn.getAttribute("data-state")) {
      setSvgIcon(micBtn, MIC_ICON.mic);
      micBtn.setAttribute("data-state", "idle");
      micBtn.classList.add("stt-mic-btn");
    }
    micBtn.title = "Spracheingabe (Start/Stop)";
    micBtn.onclick = toggleMic;
    micBtn.addEventListener("pointerdown", e => e.preventDefault(), true);
    micBtn.addEventListener("mousedown",   e => e.preventDefault(), true);
    if (!micBtn.isConnected) document.body.appendChild(micBtn);

    // GEMINI TOGGLE
    geminiToggleBtn = getOrCreateButton(UI_IDS.geminiToggle);
    styleRoundButton(geminiToggleBtn, 0, 52);
    geminiToggleBtn.addEventListener("click", async () => {
      CFG.autoGeminiCorrection = !CFG.autoGeminiCorrection;
      await Promise.resolve(GM_setValue("autoGeminiCorrection", CFG.autoGeminiCorrection));
      updateGeminiToggleBtn();
      showToast(CFG.autoGeminiCorrection ? "✅ Gemini-Korrektur aktiviert" : "❌ Gemini-Korrektur deaktiviert", 2000);
    });
    if (!geminiToggleBtn.isConnected) document.body.appendChild(geminiToggleBtn);

    clearBtn = getOrCreateButton(UI_IDS.clear);
    styleRoundButton(clearBtn, 0, 104);
    clearBtn.textContent = clearBtn.textContent || "\u274C";
    setUiStyle(clearBtn, "color", "#c40000");
    clearBtn.title = "Sprechblase leeren";
    clearBtn.onclick = runClearPrompt;
    clearBtn.addEventListener("pointerdown", e => e.preventDefault(), true);
    clearBtn.addEventListener("mousedown",   e => e.preventDefault(), true);
    if (!clearBtn.isConnected) document.body.appendChild(clearBtn);

    promptBtn = getOrCreateButton(UI_IDS.promptFrank);
    styleRoundButton(promptBtn, 0, 156);
    promptBtn.textContent = promptBtn.textContent || "\u2728";
    promptBtn.title = "Prompt (f\u00fcr Frank) einbetten";
    promptBtn.onclick = runPromptBuilder;
    promptBtn.addEventListener("pointerdown", e => e.preventDefault(), true);
    promptBtn.addEventListener("mousedown",   e => e.preventDefault(), true);
    if (!promptBtn.isConnected) document.body.appendChild(promptBtn);

    promptBtn2 = getOrCreateButton(UI_IDS.promptGeneral);
    styleRoundButton(promptBtn2, 0, 208);
    promptBtn2.textContent = promptBtn2.textContent || "\uD83E\uDE84";
    promptBtn2.title = "Prompt (allgemein / 12. Klasse) einbetten";
    promptBtn2.onclick = runPromptBuilderGeneral;
    promptBtn2.addEventListener("pointerdown", e => e.preventDefault(), true);
    promptBtn2.addEventListener("mousedown",   e => e.preventDefault(), true);
    if (!promptBtn2.isConnected) document.body.appendChild(promptBtn2);

    setMicState("idle");
    updateGeminiToggleBtn();
    setPromptBtnState("idle");
    setPromptBtn2State("idle");
  }
  // ── UI Watchdog: Buttons nach SPA-Navigation wiederherstellen ──
  let ensureScheduled = false;
  function scheduleEnsureUI() {
    if (ensureScheduled) return;
    ensureScheduled = true;
    setTimeout(() => {
      ensureScheduled = false;
      try { mountOrRepairUI(); } catch (e) { console.warn("mountOrRepairUI error:", e); }
    }, 300);
  }

  function startUiWatchdog() {
    // MutationObserver: falls die SPA den DOM neu aufbaut
    try {
      const mo = new MutationObserver(() => {
        if (!document.getElementById("tm-aistudio-mic") ||
        !document.getElementById("tm-aistudio-gemini-toggle") ||
        !document.getElementById("tm-aistudio-clear") ||
        !document.getElementById("tm-aistudio-prompt") ||
        !document.getElementById("tm-aistudio-prompt2"))
          scheduleEnsureUI();
      });
      mo.observe(document.documentElement, { childList: true, subtree: true });
    } catch (e) {}

    // History-Hooks: bei SPA-Navigation (pushState/replaceState)
    try {
      const _push    = history.pushState;
      const _replace = history.replaceState;
      history.pushState    = function () { const r = _push.apply(this, arguments);    scheduleEnsureUI(); return r; };
      history.replaceState = function () { const r = _replace.apply(this, arguments); scheduleEnsureUI(); return r; };
      window.addEventListener("popstate", scheduleEnsureUI, true);
    } catch (e) {}

    // Fallback-Interval (alle 3 s)
    setInterval(() => {
      if (!document.getElementById("tm-aistudio-mic") ||
        !document.getElementById("tm-aistudio-gemini-toggle") ||
        !document.getElementById("tm-aistudio-clear") ||
        !document.getElementById("tm-aistudio-prompt") ||
        !document.getElementById("tm-aistudio-prompt2"))
        scheduleEnsureUI();
    }, 3000);
  }
function boot() {
    if (!supportedSpeech) {
      showToast("SpeechRecognition nicht verf\u00fcgbar (Chrome/Edge).", 7000);
    }
    try { mountOrRepairUI(); } catch(e) { console.error("[STT-aistudio] mountOrRepairUI:", e); }
    try { startUiWatchdog(); } catch(e) { console.error("[STT-aistudio] startUiWatchdog:", e); }
    showToast("\u2705 Script aktiv. \uD83C\uDF99\uFE0F + G + \u274C + \u2728 + \uD83E\uDEA7 unten rechts.\nTipp: erst ins Ziel-Eingabefeld klicken, dann \uD83C\uDF99\uFE0F.", 2800);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => setTimeout(boot, 500));
  } else {
    setTimeout(boot, 500);
  }
})();
