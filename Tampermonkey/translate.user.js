// ==UserScript==
// @name         Translate V.1.4.4
// @namespace    https://translate.google.com/
// @version      1.4.4
// @updateURL    https://raw.githubusercontent.com/Pepsi1978/proggs/main/Tampermonkey/translate.user.js
// @downloadURL  https://raw.githubusercontent.com/Pepsi1978/proggs/main/Tampermonkey/translate.user.js
// @description  Speech-to-Text (DE) auf Google Translate. Mic-Button unten rechts. Kein stilles Fallback. Mit Output-Preview. API-Key wird in Tampermonkey gespeichert.
// @match        https://translate.google.com/*
// @match        https://www.translate.google.com/*
// @match        https://translate.google.de/*
// @match        https://www.translate.google.de/*
// @run-at       document-idle
// @grant        GM_xmlhttpRequest
// @grant        GM.xmlHttpRequest
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_registerMenuCommand
// @connect      api.groq.com
// ==/UserScript==

(() => {
	"use strict";
	// ── CSS für Mikrofon-Button Animationen (mit Fehlerbehandlung) ──
	try {
		(function () {
			if (document.getElementById("stt-mic-css")) return;
			var s = document.createElement("style");
			s.id = "stt-mic-css";
			s.textContent =
				".stt-mic-btn{display:flex!important;align-items:center!important;justify-content:center!important;padding:0!important;transition:background .25s,transform .15s,box-shadow .25s!important}.stt-mic-btn:active{transform:scale(.93)!important}.stt-mic-btn[data-state=idle]{background:#2563eb!important;color:#fff!important;border-color:#2563eb!important}.stt-mic-btn[data-state=idle]:hover{background:#1d4ed8!important;transform:scale(1.15)!important}.stt-mic-btn[data-state=listening]{background:#dc2626!important;color:#fff!important;border-color:#dc2626!important;animation:stt-pulse 1.4s ease-in-out infinite!important}.stt-mic-btn[data-state=working]{background:#d97706!important;color:#fff!important;border-color:#d97706!important}.stt-mic-btn[data-state=error]{background:#8b0000!important;color:#fff!important;border-color:#8b0000!important}@keyframes stt-pulse{0%,100%{box-shadow:0 0 0 0 rgba(220,38,38,.45)}50%{box-shadow:0 0 0 14px rgba(220,38,38,0)}}.stt-mic-btn[data-state=working] svg{animation:stt-spin .8s linear infinite}@keyframes stt-spin{to{transform:rotate(360deg)}}#stt-live-preview{position:fixed;bottom:310px;right:16px;max-width:420px;min-width:180px;padding:10px 14px;background:rgba(0,0,0,.88);color:#fff;border-radius:10px;font-size:14px;line-height:1.5;z-index:2147483646;box-shadow:0 4px 20px rgba(0,0,0,.3);max-height:180px;overflow-y:auto;word-wrap:break-word;transition:opacity .25s}#stt-live-preview .stt-pv-label{font-size:11px;color:#aaa;margin-bottom:4px;letter-spacing:.4px}#stt-live-preview .stt-pv-interim{color:#9ca3af;font-style:italic}#stt-live-preview .stt-pv-final{color:#fff}#stt-live-preview .stt-pv-waiting{color:#fbbf24;font-style:italic}";
			try {
				(document.head || document.documentElement).appendChild(s);
			} catch (e) {
				document.documentElement.appendChild(s);
			}
		})();
	} catch (e) {
		/* CSS-Animation nicht verfügbar, Buttons funktionieren trotzdem */
	}

	// ============================================================
	// UI POSITION
	// ============================================================
	const UI_POS = { rightPx: 27, bottomPx: 87 };
	// Android/Edge Mobile-Erkennung (für angepasste Restart-Delays)
	const isMobileAndroid = /Android/i.test(navigator.userAgent);

	// ============================================================
	// GROQ KEY (Whisper Speech-to-Text)
	// ============================================================
	function getGroqKey() {
		let key = "";
		try {
			key = String(GM_getValue("groqKey", "") || "");
		} catch {}
		key = key.trim();
		if (!key) {
			key = (
				prompt(
					"Bitte Groq API-Key eingeben (kostenlos auf groq.com, wird nur lokal in Tampermonkey gespeichert):",
				) || ""
			).trim();
			if (key) {
				try {
					GM_setValue("groqKey", key);
				} catch {}
			}
		}
		return key;
	}

	const CFG = {
		requestTimeoutMs: 120000, // 120s
		postPasteDelayMs: 80,
		reactNudgeDelayMs: 60,
		maxRetries: 4,
		retryWaitMs: 1200,
		previewChars: 140,

		// Groq Whisper Speech-to-Text
		whisperModel: "whisper-large-v3",
		whisperLang: "de",
	};

	function setGroqKey() {
		const key = (prompt("Neuen Groq API-Key eingeben:") || "").trim();
		if (key) {
			try {
				GM_setValue("groqKey", key);
			} catch {}
			alert("✅ Groq API-Key gespeichert.");
		} else {
			alert("⚠️ Kein Key eingegeben.");
		}
	}

	function clearGroqKey() {
		try {
			GM_setValue("groqKey", "");
		} catch {}
		alert("🧹 Groq API-Key gelöscht.");
	}

	// Tampermonkey-Menü: Key-Verwaltung
	(function () {
		const reg =
			typeof GM_registerMenuCommand === "function"
				? GM_registerMenuCommand
				: typeof GM !== "undefined"
					? GM?.registerMenuCommand?.bind(GM)
					: null;
		if (!reg) return;
		reg("🎙️ Groq-Key setzen/ändern", setGroqKey);
		reg("🗑️ Groq-Key löschen", clearGroqKey);
	})();

	const supportedSpeech = !!(
		navigator.mediaDevices && navigator.mediaDevices.getUserMedia
	);

	// Web Speech API für Live-Vorschau (Hybrid-Modus)
	const SpeechRecognitionAPI =
		window.SpeechRecognition || window.webkitSpeechRecognition;
	const supportedWebSpeech = !!SpeechRecognitionAPI;

	// ── Icons für Mikrofon-Button (textContent-safe für Trusted Types Seiten) ──
	const MIC_ICON = {
		mic: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>',
		stop: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>',
		spinner:
			'<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 2a10 10 0 0 1 10 10"/></svg>',
		error:
			'<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
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
	toast.style.font =
		"12px/1.35 system-ui, -apple-system, Segoe UI, Roboto, Arial";
	toast.style.boxShadow = "0 6px 24px rgba(0,0,0,0.18)";
	toast.style.background = "rgba(20,20,20,0.92)";
	toast.style.color = "white";
	toast.style.display = "none";
	toast.style.whiteSpace = "pre-wrap";
	try {
		if (document.body) document.body.appendChild(toast);
	} catch (e) {}

	let toastTimer = null;
	function showToast(msg, ms = 5500) {
		if (!toast.isConnected && document.body) {
			try {
				document.body.appendChild(toast);
			} catch (e) {}
		}
		clearTimeout(toastTimer);
		toast.textContent = msg;
		toast.style.display = "block";
		toastTimer = setTimeout(() => (toast.style.display = "none"), ms);
	}

	const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

	// ============================================================
	// Prompt Finder (Fallback) – wir nutzen weiterhin das zuletzt fokussierte Feld
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
		return String(s || "")
			.replace(/[\u200B-\u200D\uFEFF]/g, "")
			.trim();
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
		if (
			aria.includes("message") ||
			aria.includes("nachricht") ||
			aria.includes("send a message")
		)
			bonus *= 2.0;
		if (placeholder.includes("message") || placeholder.includes("nachricht"))
			bonus *= 2.0;

		return area * nearBottom * bonus;
	}

	function findPrompt() {
		const direct = [
			document.querySelector("textarea#prompt-textarea"),
			document.querySelector("textarea[data-testid='prompt-textarea']"),
			document.querySelector("div#prompt-textarea[contenteditable='true']"),
			document.querySelector(
				"div[data-testid='prompt-textarea'][contenteditable='true']",
			),
			document.querySelector("form textarea#prompt-textarea"),
			document.querySelector("form textarea[data-testid='prompt-textarea']"),
			document.querySelector("form [contenteditable='true']"),
			document.querySelector(
				"[aria-label*='message' i][contenteditable='true']",
			),
			document.querySelector(
				"[aria-label*='nachricht' i][contenteditable='true']",
			),
		]
			.map(resolveEditableTarget)
			.filter(Boolean)
			.find((el) => isVisible(el) && isEditableTarget(el));

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
			...document.querySelectorAll("[role='textbox']"),
		]
			.map(resolveEditableTarget)
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

	// UI Button
	let micBtn = null;
	let clearBtn = null;

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
		// eslint-disable-next-line no-undef
		if (
			(typeof micBtn !== "undefined" && el === micBtn) ||
			(typeof clearBtn !== "undefined" && el === clearBtn) ||
			(typeof promptBtn !== "undefined" && el === promptBtn) ||
			(typeof promptBtn2 !== "undefined" && el === promptBtn2) ||
			(typeof memBtn !== "undefined" && el === memBtn)
		)
			return false;

		const tag = (el.tagName || "").toUpperCase();
		const ariaDisabled =
			(el.getAttribute?.("aria-disabled") || "").toLowerCase() === "true";

		if (tag === "TEXTAREA") {
			if (el.readOnly || el.disabled || ariaDisabled || isAriaReadonly(el))
				return false;
			return true;
		}

		if (tag === "INPUT") {
			if (el.readOnly || el.disabled || ariaDisabled || isAriaReadonly(el))
				return false;
			const type = (el.type || "text").toLowerCase();
			const ok = ["text", "search", "email", "url", "tel", "password"].includes(
				type,
			);
			return ok;
		}

		if (isContentEditableLike(el)) return true;

		if (isRoleTextbox(el)) {
			const inner = el.querySelector?.(
				"textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable=''], [contenteditable='plaintext-only']",
			);
			if (inner) return isEditableTarget(inner);
			return false;
		}

		return false;
	}

	function resolveEditableTarget(el) {
		if (!el || el.nodeType !== 1) return null;

		// Erst ein moegliches inneres, praeziseres Feld suchen, damit kein grosser Wrapper erwischt wird.
		const inner = el.querySelector?.(
			"textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable=''], [contenteditable='plaintext-only'], [data-lexical-editor='true'], [role='textbox']",
		);
		if (inner && inner !== el) {
			const resolvedInner = resolveEditableTarget(inner);
			if (resolvedInner && isEditableTarget(resolvedInner))
				return resolvedInner;
		}

		if (isTextInput(el) || isContentEditableLike(el)) return el;

		if (isRoleTextbox(el)) {
			const nested = el.querySelector?.(
				"textarea, input[type='text'], input:not([type]), [contenteditable='true'], [contenteditable=''], [contenteditable='plaintext-only']",
			);
			if (nested) {
				const resolvedNested = resolveEditableTarget(nested);
				if (resolvedNested && isEditableTarget(resolvedNested))
					return resolvedNested;
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
				if (resolved && isVisible(resolved) && isEditableTarget(resolved))
					return resolved;
			}
		}

		const resolvedTarget = resolveEditableTarget(e.target);
		if (
			resolvedTarget &&
			isVisible(resolvedTarget) &&
			isEditableTarget(resolvedTarget)
		)
			return resolvedTarget;
		return null;
	}

	function rememberEditable(el) {
		const resolved = resolveEditableTarget(el);
		if (resolved && isVisible(resolved) && isEditableTarget(resolved))
			lastUserEditable = resolved;
	}

	function getUserTargetEditable() {
		const active = resolveEditableTarget(document.activeElement);
		if (active && isVisible(active) && isEditableTarget(active)) return active;

		const remembered = resolveEditableTarget(lastUserEditable);
		if (remembered && isVisible(remembered) && isEditableTarget(remembered))
			return remembered;

		const fallback = resolveEditableTarget(findPrompt());
		if (fallback && isVisible(fallback) && isEditableTarget(fallback))
			return fallback;

		return null;
	}

	// Fokus/Click-Tracking (auch Shadow DOM)
	document.addEventListener(
		"focusin",
		(e) => rememberEditable(pickEditableFromEvent(e) || document.activeElement),
		true,
	);
	document.addEventListener(
		"pointerdown",
		(e) => rememberEditable(pickEditableFromEvent(e)),
		true,
	);
	document.addEventListener(
		"mousedown",
		(e) => rememberEditable(pickEditableFromEvent(e)),
		true,
	);
	document.addEventListener(
		"click",
		(e) => rememberEditable(pickEditableFromEvent(e)),
		true,
	);

	// ============================================================
	// React/Input Events
	// ============================================================
	function dispatchReactInput(el, inputType, data) {
		try {
			el.dispatchEvent(
				new InputEvent("beforeinput", {
					bubbles: true,
					cancelable: true,
					inputType,
					data,
				}),
			);
		} catch {}
		try {
			el.dispatchEvent(
				new InputEvent("input", { bubbles: true, inputType, data }),
			);
		} catch {
			try {
				el.dispatchEvent(new Event("input", { bubbles: true }));
			} catch {}
		}
		try {
			el.dispatchEvent(new Event("change", { bubbles: true }));
		} catch {}
	}

	function dispatchKey(el, type, key) {
		try {
			el.dispatchEvent(
				new KeyboardEvent(type, { bubbles: true, cancelable: true, key }),
			);
		} catch {}
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
				Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")
					?.set ||
				Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")
					?.set;

			const setNative = (val) => {
				if (
					setter &&
					(el instanceof HTMLTextAreaElement || el instanceof HTMLInputElement)
				)
					setter.call(el, val);
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
		try {
			ok = document.execCommand("copy");
		} catch {}
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
				const setter = Object.getOwnPropertyDescriptor(
					HTMLTextAreaElement.prototype,
					"value",
				)?.set;
				if (setter) setter.call(el, v);
				else el.value = v;
				return;
			}
			if (el instanceof HTMLInputElement) {
				const setter = Object.getOwnPropertyDescriptor(
					HTMLInputElement.prototype,
					"value",
				)?.set;
				if (setter) setter.call(el, v);
				else el.value = v;
				return;
			}
			el.value = v;
		} catch {
			try {
				el.value = v;
			} catch {}
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
			try {
				el.textContent = text;
			} catch {}
		}

		dispatchReactInput(el, "insertReplacementText", String(text ?? ""));
	}

	async function setViaPaste(el, text) {
		const target = String(text ?? "")
			.replace(/\r\n/g, "\n")
			.replace(/\r/g, "\n");
		el = resolveEditableTarget(el) || el;
		if (!el) return false;

		try {
			el.focus();
		} catch {}

		if (isTextInput(el)) {
			setNativeValue(el, target);
			try {
				el.setSelectionRange(target.length, target.length);
			} catch {}
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
				try {
					pasted = document.execCommand("paste");
				} catch {}
			}
		} catch {}

		if (!pasted) {
			const okCopy = await copyToClipboardFallback(target);
			if (okCopy) {
				try {
					pasted = document.execCommand("paste");
				} catch {}
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
	// GM Request Helper
	// ============================================================
	function gmRequest() {
		if (typeof GM !== "undefined" && typeof GM.xmlHttpRequest === "function")
			return GM.xmlHttpRequest;
		if (typeof GM_xmlhttpRequest === "function") return GM_xmlhttpRequest;
		return null;
	}

	// ============================================================
	// UI Button (BOTTOM RIGHT)
	// ============================================================
	// ── Button IDs (Watchdog) ──
	const UI_IDS = { mic: "tm-translate-mic", clear: "tm-translate-clear" };

	function getOrCreateButton(id) {
		let b = document.getElementById(id);
		if (!b) {
			b = document.createElement("button");
			b.id = id;
		}
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
		setUiStyle(
			button,
			"font-family",
			"Apple Color Emoji, Segoe UI Emoji, Noto Color Emoji, sans-serif",
		);
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

		setUiStyle(b, "right", `${UI_POS.rightPx + rightOffsetPx}px`);
		setUiStyle(b, "bottom", `${UI_POS.bottomPx + bottomOffsetPx}px`);
		setUiStyle(b, "left", "auto");
		setUiStyle(b, "top", "auto");
		enforceUiButtonVisibility(b);
		setUiStyle(b, "transition", "transform 0.15s ease, box-shadow 0.25s ease");
		b.addEventListener("mouseenter", () => {
			b.style.transform = "scale(1.15)";
		});
		b.addEventListener("mouseleave", () => {
			b.style.transform = "scale(1)";
		});
	}

	// ── Trusted-Types-sicheres SVG-Icon setzen ──────────────────────────────────
	function setSvgIcon(el, svgStr) {
		try {
			el.innerHTML = svgStr; // Funktioniert auf den meisten Seiten
		} catch (e) {
			// Trusted Types Fallback: SVG via DOMParser (kein innerHTML in aktuellem Dokument)
			try {
				el.textContent = "";
				const doc = new DOMParser().parseFromString(svgStr, "image/svg+xml");
				const node = doc.documentElement;
				if (node && node.tagName === "svg")
					el.appendChild(document.adoptNode(node));
			} catch (e2) {
				// Letzter Fallback: Emoji (immer sicher)
				el.textContent = svgStr.includes("M12 1a3")
					? "🎙️"
					: svgStr.includes("<rect")
						? "⏹️"
						: svgStr.includes("M12 2a10")
							? "⏳"
							: "⚠️";
			}
		}
	}

	// ── Web Animations API (Fallback wenn CSS @keyframes nicht geladen) ─────────
	let _sttAnim = null;
	function _sttStartPulse(el) {
		_sttStopAnims();
		if (!el || !el.animate) return;
		try {
			_sttAnim = el.animate(
				[
					{ boxShadow: "0 0 0 0 rgba(220,38,38,0.45)" },
					{ boxShadow: "0 0 0 14px rgba(220,38,38,0)" },
					{ boxShadow: "0 0 0 0 rgba(220,38,38,0.45)" },
				],
				{ duration: 1400, iterations: Infinity, easing: "ease-in-out" },
			);
		} catch (e) {}
	}
	function _sttStartSpin(el) {
		_sttStopAnims();
		// Nur das SVG-Kind drehen (wie CSS .stt-mic-btn[data-state=working] svg { animation: stt-spin })
		const target = (el && el.querySelector("svg")) || el;
		if (!target || !target.animate) return;
		try {
			_sttAnim = target.animate(
				[{ transform: "rotate(0deg)" }, { transform: "rotate(360deg)" }],
				{ duration: 800, iterations: Infinity, easing: "linear" },
			);
		} catch (e) {}
	}
	function _sttStopAnims() {
		if (_sttAnim) {
			try {
				_sttAnim.cancel();
			} catch (e) {}
			_sttAnim = null;
		}
	}

	function setMicState(state, msg = "") {
		if (!micBtn) return;
		if (!micBtn.classList.contains("stt-mic-btn"))
			micBtn.classList.add("stt-mic-btn");
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
		micBtn.title = supportedSpeech
			? "Spracheingabe (Start/Stop)"
			: "Speech API nicht verfügbar";
	}

	// ============================================================
	// Groq Whisper Speech-to-Text (MediaRecorder + Groq API)
	// ============================================================
	let wantsRecording = false;
	let mediaRecorder = null;
	let audioChunks = [];
	let audioStream = null;
	let _micPending = false;
	let _domObserver = null;
	let _uiInterval = null;

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
		box.innerHTML =
			'<span class="stt-pv-waiting">Whisper transkribiert\u2026</span>';
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
					const spacer =
						textBeforeSpeech &&
						!textBeforeSpeech.endsWith(" ") &&
						!textBeforeSpeech.endsWith("\n")
							? " "
							: "";
					setViaPaste(el, textBeforeSpeech + spacer + preview);
				}
			};

			speechRecognition.onerror = (event) => {
				if (event.error === "no-speech" || event.error === "aborted") return;
				console.log("[STT] Web Speech API Fehler (unkritisch):", event.error);
			};

			speechRecognition.onend = () => {
				if (wantsRecording && speechRecognition) {
					try {
						speechRecognition.start();
					} catch {}
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
			try {
				ref.abort();
			} catch {}
		}
	}

	function groqTranscribe(audioBlob) {
		const groqKey = getGroqKey();
		if (!groqKey) {
			setMicState("error", "Groq API-Key fehlt");
			removeLivePreview();
			showToast(
				"❌ Groq API-Key fehlt.\nTampermonkey-Menü → Groq-Key setzen.",
				8000,
			);
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
			headers: { Authorization: "Bearer " + groqKey },
			data: formData,
			timeout: CFG.requestTimeoutMs,

			onload: async (r) => {
				if (r.status !== 200) {
					let msg = (r.responseText || "").slice(0, 400) || "HTTP " + r.status;
					try {
						const j = JSON.parse(r.responseText);
						if (j?.error?.message) msg = j.error.message;
					} catch {}
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
				const spacer =
					base && !base.endsWith(" ") && !base.endsWith("\n") ? " " : "";
				const combined = base + spacer + text;

				const ok = await setViaPaste(el, combined);
				removeLivePreview();
				if (ok) {
					setMicState("idle");
					const preview = text.length > 80 ? text.slice(0, 80) + "…" : text;
					showToast("✅ " + preview, 3000);
				} else {
					setMicState("error", "Text nicht übernommen");
					showToast("❌ Eingabefeld hat Text nicht übernommen.", 5000);
					setTimeout(() => setMicState("idle"), 2500);
				}
			},

			onerror: () => {
				setMicState("error", "Netzwerk-Fehler");
				removeLivePreview();
				showToast(
					"❌ Netzwerk-Fehler bei Groq API.\nHinweise: @connect, Adblock/Privacy, VPN/Proxy.",
					7000,
				);
				setTimeout(() => setMicState("idle"), 3000);
			},
			ontimeout: () => {
				setMicState("error", "Timeout");
				removeLivePreview();
				showToast("❌ Groq API Timeout.", 5000);
				setTimeout(() => setMicState("idle"), 3000);
			},
		});
	}

	function startListening() {
		if (!supportedSpeech || _micPending) return;
		_micPending = true;

		const t = getUserTargetEditable();
		if (!t) {
			showToast(
				"⚠️ Kein fokussiertes Eingabefeld. Tipp: zuerst ins Ziel-Feld tippen.",
				3500,
			);
		} else {
			rememberEditable(t);
		}

		// Text vor Aufnahme merken (für Hybrid-Kombination mit Whisper)
		textBeforeSpeech = t ? readPromptText(t) : "";

		wantsRecording = true;
		audioChunks = [];

		navigator.mediaDevices
			.getUserMedia({ audio: true })
			.then((stream) => {
				_micPending = false;
				audioStream = stream;

				const mimeType =
					typeof MediaRecorder.isTypeSupported === "function"
						? MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
							? "audio/webm;codecs=opus"
							: MediaRecorder.isTypeSupported("audio/webm")
								? "audio/webm"
								: ""
						: "";

				const options = mimeType ? { mimeType } : {};
				mediaRecorder = new MediaRecorder(stream, options);

				mediaRecorder.ondataavailable = (e) => {
					if (e.data.size > 0) audioChunks.push(e.data);
				};

				mediaRecorder.onstop = () => {
					stream.getTracks().forEach((t) => t.stop());
					audioStream = null;

					if (audioChunks.length === 0) {
						setMicState("idle");
						removeLivePreview();
						return;
					}

					const audioBlob = new Blob(audioChunks, {
						type: mediaRecorder.mimeType || "audio/webm",
					});
					audioChunks = [];
					groqTranscribe(audioBlob);
				};

				mediaRecorder.start(1000);
				setMicState("listening");
				showToast("🎙️ Aufnahme läuft… (Stop über ⏹️)", 1500);

				// Web Speech API parallel starten für Live-Vorschau
				startWebSpeech();
			})
			.catch((err) => {
				_micPending = false;
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
				audioStream.getTracks().forEach((t) => t.stop());
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

	async function runClearPrompt() {
		const el = getUserTargetEditable();
		if (!el) {
			showToast(
				"❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.",
				4500,
			);
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
		micBtn.addEventListener("pointerdown", (e) => e.preventDefault(), true);
		micBtn.addEventListener("mousedown", (e) => e.preventDefault(), true);
		if (!micBtn.isConnected) document.body.appendChild(micBtn);

		clearBtn = getOrCreateButton(UI_IDS.clear);
		styleRoundButton(clearBtn, 0, 52);
		clearBtn.textContent = clearBtn.textContent || "\u274C";
		setUiStyle(clearBtn, "color", "#c40000");
		clearBtn.title = "Sprechblase leeren";
		clearBtn.onclick = runClearPrompt;
		clearBtn.addEventListener("pointerdown", (e) => e.preventDefault(), true);
		clearBtn.addEventListener("mousedown", (e) => e.preventDefault(), true);
		if (!clearBtn.isConnected) document.body.appendChild(clearBtn);

		setMicState("idle");
	}
	// ── UI Watchdog: Buttons nach SPA-Navigation wiederherstellen ──
	let ensureScheduled = false;
	function scheduleEnsureUI() {
		if (ensureScheduled) return;
		ensureScheduled = true;
		setTimeout(() => {
			ensureScheduled = false;
			try {
				mountOrRepairUI();
			} catch (e) {
				console.warn("mountOrRepairUI error:", e);
			}
		}, 300);
	}

	function startUiWatchdog() {
		// MutationObserver: falls die SPA den DOM neu aufbaut
		try {
			if (_domObserver) _domObserver.disconnect();
			_domObserver = new MutationObserver(() => {
				if (
					!document.getElementById("tm-translate-mic") ||
					!document.getElementById("tm-translate-clear")
				)
					scheduleEnsureUI();
			});
			_domObserver.observe(document.documentElement, {
				childList: true,
				subtree: true,
			});
		} catch (e) {
			console.warn("[TM] Init error:", e);
		}

		// History-Hooks: bei SPA-Navigation (pushState/replaceState)
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
		} catch (e) {
			console.warn("[TM] Init error:", e);
		}

		// Fallback-Interval (alle 3 s)
		if (_uiInterval) clearInterval(_uiInterval);
		_uiInterval = setInterval(() => {
			if (
				!document.getElementById("tm-translate-mic") ||
				!document.getElementById("tm-translate-clear")
			)
				scheduleEnsureUI();
		}, 3000);
	}
	function boot() {
		if (!supportedSpeech) {
			showToast("SpeechRecognition nicht verf\u00fcgbar (Chrome/Edge).", 7000);
		}


		try {
			mountOrRepairUI();
		} catch (e) {
			console.error("[STT-translate] mountOrRepairUI:", e);
		}
		try {
			startUiWatchdog();
		} catch (e) {
			console.error("[STT-translate] startUiWatchdog:", e);
		}
		showToast(
			"\u2705 Script aktiv. \uD83C\uDF99\uFE0F + \u274C unten rechts.\nTipp: erst ins Ziel-Eingabefeld klicken, dann \uD83C\uDF99\uFE0F.",
			2800,
		);
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", () => setTimeout(boot, 500));
	} else {
		setTimeout(boot, 500);
	}
})();
