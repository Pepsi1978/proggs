// ============================================================
// editable.js — geteiltes Eingabefeld-Handling
// ------------------------------------------------------------
// Aus translate.user.js + chatgpt.user.js gemergt. Findet das richtige
// Eingabefeld (auch contenteditable/Lexical/role=textbox), liest/schreibt
// Text React-sicher (setViaPaste). Eigene Overlay-Buttons (Klasse ov-btn)
// werden nie als Eingabefeld erkannt.
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	const CFG = {
		postPasteDelayMs: 90,
		reactNudgeDelayMs: 70,
	};

	function isOwnButton(el) {
		return !!(el && el.classList && el.classList.contains("ov-btn"));
	}

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
		if (isOwnButton(el)) return false; // niemals eigene Buttons

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
			return ["text", "search", "email", "url", "tel", "password"].includes(
				type,
			);
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

	function readPromptText(el) {
		if (!el) return "";
		el = resolveEditableTarget(el) || el;
		if (isTextInput(el)) return cleanText(el.value || "");
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

	// ChatGPT: bevorzugt den unteren Composer (fuer Memory/Prompt-Builder)
	function findComposerPrompt() {
		const direct = [
			document.querySelector("textarea#prompt-textarea"),
			document.querySelector("textarea[data-testid='prompt-textarea']"),
			document.querySelector("div#prompt-textarea[contenteditable='true']"),
			document.querySelector(
				"div[data-testid='prompt-textarea'][contenteditable='true']",
			),
			document.querySelector("form textarea#prompt-textarea"),
			document.querySelector(
				"form div[data-testid='prompt-textarea'][contenteditable='true']",
			),
		]
			.map(resolveEditableTarget)
			.filter(Boolean)
			.find(isVisible);
		return direct || findPrompt();
	}

	// ── Targeting: zuletzt fokussiertes Feld merken ──
	let lastUserEditable = null;

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

	function getComposerTargetEditable() {
		const composer = findComposerPrompt();
		if (composer && isVisible(composer)) return composer;
		return getUserTargetEditable();
	}

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

	// ── React/Input Events ──
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
		await OV.sleep(CFG.reactNudgeDelayMs);

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
			await OV.sleep(25);
			setNative(v);
			dispatchReactInput(el, "deleteContentBackward", null);
			dispatchKey(el, "keydown", "Backspace");
			dispatchKey(el, "keyup", "Backspace");
			return;
		}
		try {
			document.execCommand("insertText", false, " ");
			dispatchReactInput(el, "insertText", " ");
			await OV.sleep(20);
			document.execCommand("delete", false, null);
			dispatchReactInput(el, "deleteContentBackward", null);
		} catch {}
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
			.replace(/"/g, "&quot;")
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

		await OV.sleep(CFG.postPasteDelayMs);
		await reactNudge(el);
		await OV.sleep(40);

		const got = normalizeForCompare(readPromptText(el));
		const want = normalizeForCompare(target);
		if (got === want) return true;
		if (got.replace(/\s+/g, " ") === want.replace(/\s+/g, " ")) return true;

		// Letzter Versuch ueber Clipboard (falls Editor nur Paste verarbeitet)
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
			await OV.sleep(CFG.postPasteDelayMs);
			await reactNudge(el);
			await OV.sleep(40);
		}
		const gotAfter = normalizeForCompare(readPromptText(el));
		if (gotAfter === want) return true;
		return gotAfter.replace(/\s+/g, " ") === want.replace(/\s+/g, " ");
	}

	OV.editable = {
		isVisible,
		readPromptText,
		findPrompt,
		findComposerPrompt,
		rememberEditable,
		getUserTargetEditable,
		getComposerTargetEditable,
		setViaPaste,
	};
})();
