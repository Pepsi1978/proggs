// ==UserScript==
// @name         Claude V.1.2.5
// @namespace    https://claude.ai/
// @version      1.2.5
// @description  Speech-to-Text + Gemini-„Diktat-Bereinigung“ (DE) auf Claude: entfernt Kauderwelsch/Doubletten + setzt Satzbau/Zeichensetzung. Dazu 2 Prompt-Builder Buttons. ProseMirror-kompatible Textübernahme + UI-Reinject (Buttons verschwinden nicht mehr). Debounced Observer (verhindert Lade-Freeze). Fix: strengere Prompt-Feld-Erkennung (kein Seitentext mehr).
// @match        https://claude.ai/*
// @match        https://www.claude.ai/*
// @run-at       document-idle
// @downloadURL  https://raw.githubusercontent.com/Pepsi1978/tampermonkey-skripte/main/scripts/claude.user.js
// @updateURL    https://raw.githubusercontent.com/Pepsi1978/tampermonkey-skripte/main/scripts/claude.user.js
// @grant        GM_xmlhttpRequest
// @grant        GM.xmlHttpRequest
// @grant        GM_getValue
// @grant        GM_setValue
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


  // ============================================================
  // 🔑 API-Key lokal in Tampermonkey speichern
  // ============================================================
  const GEMINI_KEY_STORAGE = "tm_claude_gemini_api_key";
  const GEMINI_MODEL = "models/gemini-3.1-flash-lite-preview";
  const GEMINI_THINKING_LEVEL = "MEDIUM";

  const gmGetValue = typeof GM_getValue === "function" ? GM_getValue : GM?.getValue?.bind(GM);
  const gmSetValue = typeof GM_setValue === "function" ? GM_setValue : GM?.setValue?.bind(GM);
  const gmMenu = typeof GM_registerMenuCommand === "function" ? GM_registerMenuCommand : GM?.registerMenuCommand?.bind(GM);

  async function getStoredApiKey() {
    if (!gmGetValue) return "";
    const v = await Promise.resolve(gmGetValue(GEMINI_KEY_STORAGE, ""));
    return String(v || "").trim();
  }

  async function setStoredApiKey(value) {
    if (!gmSetValue) return false;
    await Promise.resolve(gmSetValue(GEMINI_KEY_STORAGE, String(value || "").trim()));
    return true;
  }

  async function promptForApiKey() {
    const current = await getStoredApiKey();
    const next = window.prompt("Bitte Gemini API-Key eingeben (lokal in Tampermonkey gespeichert):", current || "");
    if (next === null) return null;
    await setStoredApiKey(next);
    return String(next || "").trim();
  }

  // ============================================================
  // UI POSITION
  // ============================================================
  const UI_POS = { rightPx: 16, bottomPx: 200 };

  // IDs, damit wir UI wiederfinden / neu injizieren können
  const UI_IDS = {
    mic: "tm_claude_mic_btn",
    prompt1: "tm_claude_prompt_btn",
    clear: "tm_claude_clear_btn",
    prompt2: "tm_claude_prompt_btn2"
  };
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
  document.body.appendChild(toast);

  let toastTimer = null;
  function showToast(msg, ms = 5500) {
    clearTimeout(toastTimer);
    toast.textContent = msg;
    toast.style.display = "block";
    toastTimer = setTimeout(() => (toast.style.display = "none"), ms);
  }

  if (gmMenu) {
    gmMenu("Claude: Gemini API-Key setzen", async () => {
      const next = await promptForApiKey();
      if (next === null) return;
      showToast(next ? "API-Key gespeichert." : "API-Key gelöscht.");
    });
    gmMenu("Claude: Auto-Korrektur [AN/AUS]", async () => {
      CFG.autoGeminiCorrection = !CFG.autoGeminiCorrection;
      await Promise.resolve(gmSetValue("autoGeminiCorrection", CFG.autoGeminiCorrection));
      showToast(CFG.autoGeminiCorrection ? "✅ Auto-Korrektur aktiviert" : "❌ Auto-Korrektur deaktiviert", 3000);
    });
  }

  // Gespeicherten Auto-Korrektur-Status laden
  (async () => {
    if (gmGetValue) {
      const stored = await Promise.resolve(gmGetValue("autoGeminiCorrection", true));
      CFG.autoGeminiCorrection = stored !== false;
    }
  })();

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

  function isContentEditableLike(el) {
    if (!el) return false;
    if (el.isContentEditable) return true;
    const ce = (el.getAttribute?.("contenteditable") || "").toLowerCase();
    if (ce === "true" || ce === "") return true;
    const role = (el.getAttribute?.("role") || "").toLowerCase();
    if (role === "textbox") return true;
    return false;
  }

  function readPromptText(el) {
    if (!el) return "";
    let v = "";
    if (isTextInput(el)) v = el.value || "";
    // Für ProseMirror ist innerText oft der verlässlichste Weg, Newlines zu bekommen
    if (!v) v = el.innerText || "";
    if (!v) v = el.textContent || "";
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

  function isReasonableComposerSize(el) {
    if (!el) return false;
    const r = el.getBoundingClientRect();
    const vh = Math.max(1, window.innerHeight);
    const vw = Math.max(1, window.innerWidth);

    if (r.width < 120 || r.height < 24) return false;
    if (r.height > vh * 0.55) return false;
    if (r.width > vw * 0.99 && r.height > vh * 0.35) return false;
    return true;
  }

  function isLikelyClaudePromptInput(el) {
    if (!el) return false;

    const id = (el.id || "").toLowerCase();
    const dt = (el.getAttribute?.("data-testid") || "").toLowerCase();
    const aria = (el.getAttribute?.("aria-label") || "").toLowerCase();
    const placeholder = (el.getAttribute?.("placeholder") || "").toLowerCase();

    if (id.includes("prompt-textarea") || dt.includes("prompt-textarea")) return true;
    if (id.includes("prompt") || dt.includes("prompt")) return true;
    if (aria.includes("send a message") || aria.includes("nachricht") || aria.includes("message") || aria.includes("prompt")) return true;
    if (placeholder.includes("send a message") || placeholder.includes("nachricht") || placeholder.includes("message") || placeholder.includes("prompt")) return true;

    if (el.classList?.contains("ProseMirror")) {
      const form = el.closest?.("form");
      if (form && form.querySelector("button[type='submit'], [aria-label*='Send'], [aria-label*='Senden']")) return true;
    }

    const form = el.closest?.("form");
    if (form && form.querySelector("textarea, .ProseMirror, [contenteditable='true']")) {
      const sendBtn = form.querySelector("button[type='submit'], [aria-label*='Send'], [aria-label*='Senden'], [data-testid*='send']");
      if (sendBtn) return true;
    }

    return false;
  }

  function findPrompt() {
    const direct = [
      document.querySelector("textarea#prompt-textarea"),
      document.querySelector("textarea[data-testid='prompt-textarea']"),
      document.querySelector("div#prompt-textarea[contenteditable='true']"),
      document.querySelector("div[data-testid='prompt-textarea'][contenteditable='true']"),
      document.querySelector(".ProseMirror[contenteditable='true']"),
      document.querySelector("form textarea#prompt-textarea"),
      document.querySelector("form textarea[data-testid='prompt-textarea']")
    ].filter(Boolean).find((el) => isVisible(el) && isReasonableComposerSize(el));

    if (direct) return direct;

    const candidates = [
      ...document.querySelectorAll("textarea"),
      ...document.querySelectorAll("input[type='text']"),
      ...document.querySelectorAll("input:not([type])"),
      ...document.querySelectorAll("[contenteditable='true']"),
      ...document.querySelectorAll("[role='textbox']")
    ].filter((el) => isVisible(el) && isReasonableComposerSize(el) && isLikelyClaudePromptInput(el));

    if (!candidates.length) return null;

    candidates.sort((a, b) => scoreCandidate(b) - scoreCandidate(a));

    const top = candidates[0];
    const inner = top.querySelector?.("[contenteditable='true']");
    return inner && isVisible(inner) ? inner : top;
  }

  // ============================================================
  // 🎯 Targeting: nutze das Feld, das der User fokussiert hat
  // ============================================================
  let lastUserEditable = null;

  // UI Buttons werden später initialisiert, hier schon deklariert:
  let micBtn = null;
  let clearBtn = null;
  let promptBtn = null;
  let promptBtn2 = null;

  function isEditableTarget(el) {
    if (!el) return false;
    if (el === document.body || el === document.documentElement) return false;

    // niemals unsere eigenen UI-Buttons als Eingabefeld nehmen
    if (el === micBtn || el === clearBtn || el === promptBtn || el === promptBtn2) return false;

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

    if (el.isContentEditable) return true;
    const ce = (el.getAttribute?.("contenteditable") || "").toLowerCase();
    if (ce === "true" || ce === "") return true;

    const role = (el.getAttribute?.("role") || "").toLowerCase();
    if (role === "textbox") return true;

    return false;
  }

  function pickEditableFromEvent(e) {
    const path = typeof e.composedPath === "function" ? e.composedPath() : null;
    if (Array.isArray(path)) {
      for (const node of path) {
        if (node && node.nodeType === 1 && isEditableTarget(node) && isVisible(node)) return node;
      }
    }
    const t = e.target;
    if (isEditableTarget(t) && isVisible(t)) return t;
    return null;
  }

  function rememberEditable(el) {
    if (isEditableTarget(el) && isVisible(el)) lastUserEditable = el;
  }

  function getUserTargetEditable() {
    const active = document.activeElement;
    if (isEditableTarget(active) && isVisible(active)) return active;
    if (isEditableTarget(lastUserEditable) && isVisible(lastUserEditable)) return lastUserEditable;
    return findPrompt(); // Fallback
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
  // ✅ ProseMirror / Claude: Format-robustes Einfügen
  // ============================================================
  function normalizeForCompare(s) {
    return String(s || "")
      .replace(/[\u200B-\u200D\uFEFF]/g, "")
      .replace(/\r\n/g, "\n")
      .replace(/\r/g, "\n")
      .replace(/[ \t]+\n/g, "\n")
      .replace(/\n[ \t]+/g, "\n")
      .trim();
  }

  function escapeHtml(s) {
    return String(s || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  // WICHTIG: Hier wird absichtlich JEDE Zeile zu einem eigenen <p> gemacht.
  // Leerzeilen werden als <p><br></p> erhalten => echte "Absätze/Leerzeilen".
  function textToProseMirrorHTML(text) {
    const t = String(text ?? "").replace(/\r\n/g, "\n").replace(/\r/g, "\n");
    const lines = t.split("\n");
    const ps = lines.map(line => {
      if (line === "") return "<p><br></p>";
      return `<p>${escapeHtml(line)}</p>`;
    }).join("");
    return `<div>${ps}</div>`;
  }

  function getProseMirrorRoot(el) {
    if (!el) return null;
    if (el.classList?.contains("ProseMirror")) return el;

    const closest = el.closest?.(".ProseMirror");
    if (closest && isVisible(closest) && isContentEditableLike(closest)) return closest;

    const inner = el.querySelector?.(".ProseMirror");
    if (inner && isVisible(inner) && isContentEditableLike(inner)) return inner;

    return null;
  }

  function selectAllIn(el) {
    try {
      el.focus();
      const sel = window.getSelection();
      if (!sel) return false;
      const range = document.createRange();
      range.selectNodeContents(el);
      sel.removeAllRanges();
      sel.addRange(range);
      return true;
    } catch {
      return false;
    }
  }

  function execCmd(cmd, value = null) {
    try {
      return document.execCommand(cmd, false, value);
    } catch {
      return false;
    }
  }

  function tryDispatchPaste(el, plainText, htmlText) {
    // Versuch 1: echter ClipboardEvent (falls Browser/Claude es annimmt)
    try {
      const dt = new DataTransfer();
      dt.setData("text/plain", plainText);
      if (htmlText) dt.setData("text/html", htmlText);

      const ev = new ClipboardEvent("paste", {
        bubbles: true,
        cancelable: true,
        clipboardData: dt
      });

      return el.dispatchEvent(ev);
    } catch {}

    // Versuch 2: Event + defineProperty(clipboardData) (Fallback)
    try {
      const dt = new DataTransfer();
      dt.setData("text/plain", plainText);
      if (htmlText) dt.setData("text/html", htmlText);

      const ev = new Event("paste", { bubbles: true, cancelable: true });
      Object.defineProperty(ev, "clipboardData", { value: dt });
      return el.dispatchEvent(ev);
    } catch {}

    return false;
  }

  function insertWithParagraphCommands(pmEl, text) {
    const t = String(text ?? "").replace(/\r\n/g, "\n").replace(/\r/g, "\n");

    // alles markieren & löschen
    selectAllIn(pmEl);
    execCmd("delete");
    execCmd("insertText", "");

    // Jede \n wird zu echtem Absatz (insertParagraph)
    const parts = t.split(/(\n+)/);

    for (const part of parts) {
      if (!part) continue;

      if (part[0] === "\n") {
        for (let i = 0; i < part.length; i++) {
          execCmd("insertParagraph");
        }
        continue;
      }

      execCmd("insertText", part);
    }
  }

  // ============================================================
  // PASTE-APPLY (mit Claude-ProseMirror Fix)
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

  async function setViaPaste(el, text) {
    if (!el) return false;

    const targetRaw = String(text ?? "")
      .replace(/[\u200B-\u200D\uFEFF]/g, "")
      .replace(/\r\n/g, "\n")
      .replace(/\r/g, "\n");

    const targetCmp = normalizeForCompare(targetRaw);

    // ✅ Claude/ProseMirror: immer am ProseMirror-Root arbeiten
    const pm = getProseMirrorRoot(el);
    const targetEl = pm || el;

    targetEl.focus();

    // --- 1) ProseMirror: versuche Paste-Event (am ähnlichsten zu echtem Einfügen) ---
    if (pm) {
      const html = textToProseMirrorHTML(targetRaw);

      selectAllIn(pm);
      tryDispatchPaste(pm, targetRaw, html);

      await sleep(CFG.postPasteDelayMs);

      const gotA = normalizeForCompare(readPromptText(pm));
      if (gotA === targetCmp) {
        await reactNudge(pm);
        await sleep(40);
        return true;
      }

      // --- 2) ProseMirror: versuche insertHTML mit sauberer Selektion im PM-Root ---
      selectAllIn(pm);
      execCmd("insertHTML", html);

      await sleep(CFG.postPasteDelayMs);

      const gotB = normalizeForCompare(readPromptText(pm));
      if (gotB === targetCmp) {
        await reactNudge(pm);
        await sleep(40);
        return true;
      }

      // --- 3) Ultima Ratio: Paragraph-Kommandos (macht echte Absätze, auch leere) ---
      insertWithParagraphCommands(pm, targetRaw);

      await sleep(CFG.postPasteDelayMs);
      await reactNudge(pm);
      await sleep(40);

      // ProseMirror kann Newlines im readback anders darstellen (innerText vs. model).
      // Daher: Erfolg, wenn nicht leer und grob plausible Länge.
      const gotC = normalizeForCompare(readPromptText(pm));
      return gotC.length >= Math.min(20, targetCmp.length);
    }

    // --- Standard-Feld: weiterhin Copy+Paste / execCommand (so wie du es willst) ---
    try { document.execCommand("selectAll", false, null); } catch {}

    let pasted = false;

    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(targetRaw);
        try { pasted = document.execCommand("paste"); } catch {}
      }
    } catch {}

    if (!pasted) {
      const okCopy = await copyToClipboardFallback(targetRaw);
      if (okCopy) {
        try { pasted = document.execCommand("paste"); } catch {}
      }
    }

    if (!pasted) {
      // Letzter Fallback: Events (kein direktes value-setten als Standardpfad)
      dispatchReactInput(targetEl, "insertFromPaste", targetRaw);
      pasted = true;
    }

    await sleep(CFG.postPasteDelayMs);
    await reactNudge(targetEl);
    await sleep(40);

    // Bei normalen Inputs kann readPromptText trimmen -> Vergleich über normalizeForCompare
    return normalizeForCompare(readPromptText(targetEl)) === targetCmp || normalizeForCompare(readPromptText(targetEl)).length >= Math.min(20, targetCmp.length);
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

  async function geminiGenerate(userPrompt, { temperature = 0.05, maxOutputTokens = 2048 } = {}) {
    let apiKey = await getStoredApiKey();
    if (!apiKey) {
      apiKey = await promptForApiKey();
    }
    if (!apiKey || apiKey.toLowerCase().includes("key hier") || apiKey.includes("PASTE_YOUR_KEY_HERE")) {
      return Promise.reject("API-Key fehlt oder ungültig.");
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
Falls dir etwas zur Aufgabe nicht klar ist frage nochmal den Benutzer. Claude soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
allumfassendes Bild für die Antwort einbezogen wird.

Zielgruppe:
Frank (Benutzer dieses Chats).

Kontext:
Nutze die über Frank gespeicherten Informationen und Erinnerungen (Bio, Ziele, Routinen, Besonderheiten, Projekte, usw.) als
primären Bezugsrahmen für diese Aufgabe. Keine zusätzlichen Kontextabfragen; verwende vorhandenes Wissen.

Format:
Strikte Ausgabestruktur „Einleitung – Hauptteil – Zusammenfassung“. Fachbegriffe sofort kurz erklären (in Klammern); Erläutere alle Zusammenhänge sämtlicher Faktoren sehr genau, damit Frank alle Zusammenhänge (wie, warum etwas so ist) genaustens versteht.
Abkürzungen bei erster Nennung ausschreiben; logisch, faktenbasiert, umfassend, nachvollziehbar. Benutze gegeben falls auch Symbole und Smylies, große Überschriften, Tabelle zu besseren optischen Gliederung.
Mindestwortanzahl: 2000, Maximale Wortanzalhl: 10000, Entscheidend ist, dass die Aufgabe/Frage sehr ausführlich erledigt/beantwortet wird.

Ton:
Wissenschaftlich-professionell, präzise, didaktisch klar, ausführlich, ohne metasprachliche Hinweise oder Floskeln, leicht verständlich.

Pflichtlogik:
Keine Abweichung bei Zielgruppe, Kontext, Format, Ton.
Domänen in der Rolle müssen sichtbar den inhaltlichen Kern der Aufgabe abdecken.
Aufgabe immer als Imperativ mit klarer Output-Erwartung formulieren.
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
Falls dir etwas zur Aufgabe nicht klar ist frage nochmal den Benutzer. Claude soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein
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
Domänen in der Rolle müssen den inhaltlichen Kern der Aufgabe sichtbar abdecken.
Die Aufgabe wird immer 1:1 übernommen, ohne Umformulierung oder Ergänzung.
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

    b.style.right = `${UI_POS.rightPx + rightOffsetPx}px`;
    b.style.bottom = `${UI_POS.bottomPx + bottomOffsetPx}px`;
    b.style.left = "auto";
    b.style.top = "auto";
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
    // Auto-scroll
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

  // ── Web Speech API starten/stoppen ──
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
        // Web Speech stoppt automatisch nach Stille → Neustart solange Aufnahme läuft
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
      speechRecognition = null; // Verhindert Auto-Restart in onend
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

        // Hybrid: textBeforeSpeech + Whisper-Ergebnis kombinieren
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
  // Boot + UI-Reinject (Claude SPA)
  // ============================================================
  function boot() {
    if (!supportedSpeech) {
      showToast("Mikrofon nicht verfügbar (getUserMedia fehlt).", 7000);
    }

    // Falls UI schon existiert (z.B. nach Reinjection), nur Referenzen setzen
    const existingMic = document.getElementById(UI_IDS.mic);
    const existingClear = document.getElementById(UI_IDS.clear);
    const existingP1 = document.getElementById(UI_IDS.prompt1);
    const existingP2 = document.getElementById(UI_IDS.prompt2);

    if (existingMic && existingClear && existingP1 && existingP2) {
      micBtn = existingMic;
      clearBtn = existingClear;
      promptBtn = existingP1;
      promptBtn2 = existingP2;

      styleRoundButton(micBtn, 0, 0);
      styleRoundButton(clearBtn, 0, 52);
      styleRoundButton(promptBtn, 0, 104);
      styleRoundButton(promptBtn2, 0, 156);

      setMicState("idle");
      setPromptBtnState("idle");
      setPromptBtn2State("idle");
      return;
    }

    // Alte Reste entfernen (falls halb vorhanden)
    try { existingMic?.remove(); } catch {}
    try { existingClear?.remove(); } catch {}
    try { existingP1?.remove(); } catch {}
    try { existingP2?.remove(); } catch {}

    micBtn = document.createElement("button");
    micBtn.id = UI_IDS.mic;
    styleRoundButton(micBtn, 0, 0);
    micBtn.innerHTML = MIC_ICON.mic; micBtn.setAttribute("data-state", "idle"); micBtn.classList.add("stt-mic-btn");
    micBtn.title = "Spracheingabe (Start/Stop)";
    micBtn.addEventListener("click", toggleMic);
    document.body.appendChild(micBtn);

    clearBtn = document.createElement("button");
    clearBtn.id = UI_IDS.clear;
    styleRoundButton(clearBtn, 0, 52);
    clearBtn.textContent = "❌";
    clearBtn.style.color = "#c40000";
    clearBtn.title = "Sprechblase leeren";
    clearBtn.addEventListener("click", runClearPrompt);
    document.body.appendChild(clearBtn);

    promptBtn = document.createElement("button");
    promptBtn.id = UI_IDS.prompt1;
    styleRoundButton(promptBtn, 0, 104);
    promptBtn.textContent = "✨";
    promptBtn.title = "Prompt (für Frank) einbetten";
    promptBtn.addEventListener("click", runPromptBuilder);
    document.body.appendChild(promptBtn);

    promptBtn2 = document.createElement("button");
    promptBtn2.id = UI_IDS.prompt2;
    styleRoundButton(promptBtn2, 0, 156);
    promptBtn2.textContent = "🪄";
    promptBtn2.title = "Prompt (allgemein / 12. Klasse) einbetten";
    promptBtn2.addEventListener("click", runPromptBuilderGeneral);
    document.body.appendChild(promptBtn2);

    setMicState("idle");
    setPromptBtnState("idle");
    setPromptBtn2State("idle");
    showToast("✅ Script aktiv. 🎙️ + ❌ + ✨ + 🪄 unten rechts.\nTipp: erst ins Ziel-Eingabefeld klicken, dann Button.", 2800);
  }

  function uiIsMissing() {
    return (
      !document.getElementById(UI_IDS.mic) ||
      !document.getElementById(UI_IDS.clear) ||
      !document.getElementById(UI_IDS.prompt1) ||
      !document.getElementById(UI_IDS.prompt2)
    );
  }

  let uiWatchTimer = null;
  function scheduleEnsureUI() {
    clearTimeout(uiWatchTimer);
    uiWatchTimer = setTimeout(() => {
      if (!document.body) return;
      if (uiIsMissing()) boot();
    }, CFG.uiWatchDebounceMs || 250);
  }

  // MutationObserver: Claude ersetzt oft Teile des DOM -> Buttons verschwinden
  const mo = new MutationObserver(() => scheduleEnsureUI());
  try {
    mo.observe(document.documentElement, { childList: true, subtree: true });
  } catch {}

  // SPA Navigation: pushState/replaceState + popstate
  (function hookHistory() {
    const _push = history.pushState;
    const _repl = history.replaceState;

    history.pushState = function () {
      const r = _push.apply(this, arguments);
      scheduleEnsureUI();
      return r;
    };

    history.replaceState = function () {
      const r = _repl.apply(this, arguments);
      scheduleEnsureUI();
      return r;
    };

    window.addEventListener("popstate", scheduleEnsureUI, true);
  })();

  // Initial boot + kurze Nachkontrolle (Claude mountet manchmal verzögert)
  boot();
  setTimeout(scheduleEnsureUI, 500);
  setTimeout(scheduleEnsureUI, 1500);
  setTimeout(scheduleEnsureUI, 3000);
})();
