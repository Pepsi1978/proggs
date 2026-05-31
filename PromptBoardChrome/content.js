// Content script for Prompt Board.
// Runs in every frame of every page. Its job:
//   1. Remember the last editable element the user focused (because clicking
//      the side panel removes focus from the page).
//   2. On request from the side panel, insert text into that element using
//      the correct technique per field type (textarea/input vs contenteditable).
//
// Wrapped in a load guard: the script is registered via manifest AND may be
// injected programmatically (background.js / side panel) into tabs that were
// already open. The guard prevents double-registration of listeners.

(() => {
	if (window.__promptBoardContentLoaded__) return;
	window.__promptBoardContentLoaded__ = true;

	let lastEditable = null;

	function isEditable(el) {
		if (!el || !el.tagName) return false;
		const tag = el.tagName.toUpperCase();
		if (tag === "TEXTAREA") return true;
		if (tag === "INPUT") {
			const t = (el.type || "text").toLowerCase();
			return [
				"text",
				"search",
				"url",
				"email",
				"tel",
				"password",
				"number",
				"",
			].includes(t);
		}
		if (el.isContentEditable) return true;
		return false;
	}

	// Track the last editable element across the whole document (capture phase so
	// it also catches focus inside nested widgets).
	document.addEventListener(
		"focusin",
		(e) => {
			if (isEditable(e.target)) lastEditable = e.target;
		},
		true,
	);

	function resolveTarget() {
		if (isEditable(lastEditable) && lastEditable.isConnected)
			return lastEditable;
		if (isEditable(document.activeElement)) return document.activeElement;
		return null;
	}

	function insertIntoField(el, text) {
		el.focus();

		const tag = el.tagName.toUpperCase();
		if (tag === "TEXTAREA" || tag === "INPUT") {
			// Insert at the caret position, preserving surrounding text.
			const value = el.value ?? "";
			const start =
				typeof el.selectionStart === "number"
					? el.selectionStart
					: value.length;
			const end =
				typeof el.selectionEnd === "number" ? el.selectionEnd : value.length;
			el.value = value.slice(0, start) + text + value.slice(end);
			const caret = start + text.length;
			try {
				el.selectionStart = el.selectionEnd = caret;
			} catch (_) {
				/* some input types disallow selection range */
			}
			el.dispatchEvent(new Event("input", { bubbles: true }));
			el.dispatchEvent(new Event("change", { bubbles: true }));
			return true;
		}

		// contenteditable (e.g. ChatGPT, Gmail body, rich editors).
		// execCommand('insertText') is deprecated but remains the most reliable way
		// to insert into React/Draft.js-style editors. Fall back to the Range API.
		let inserted = false;
		try {
			inserted = document.execCommand("insertText", false, text);
		} catch (_) {
			inserted = false;
		}
		if (!inserted) {
			const sel = window.getSelection();
			if (sel && sel.rangeCount > 0) {
				const range = sel.getRangeAt(0);
				range.deleteContents();
				range.insertNode(document.createTextNode(text));
				range.collapse(false);
			} else {
				el.textContent += text;
			}
			el.dispatchEvent(new Event("input", { bubbles: true }));
		}
		return true;
	}

	chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
		if (!msg || msg.type !== "insertPrompt") return false;

		// With all_frames:true every frame receives this message. Only the frame
		// that actually holds an editable target should respond; the others stay
		// silent so the response callback gets the correct frame's result.
		const target = resolveTarget();
		if (!target) return false;

		try {
			const ok = insertIntoField(target, msg.text || "");
			sendResponse({ ok });
		} catch (e) {
			sendResponse({ ok: false, reason: String(e && e.message) });
		}
		return true;
	});
})();
