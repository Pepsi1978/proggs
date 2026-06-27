// ============================================================
// toast.js — stille Ereignisprotokollierung für Content-Scripts
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	const LOG_KEY = "ov_error_log";
	const MAX_LOG_ENTRIES = 200;

	function isProblemMessage(msg) {
		return /(^|\s)(❌|⚠️)|fehler|fehlgeschlagen|nicht gefunden|nicht uebernommen|nicht übernommen|kein zugriff|http\s*\d{3}|timeout/i.test(
			String(msg || ""),
		);
	}

	async function appendLog(level, msg, ctx = {}) {
		try {
			if (typeof chrome === "undefined" || !chrome.storage?.local) return;
			const entry = {
				ts: new Date().toISOString(),
				level,
				msg: String(msg || ""),
				url: location.href,
				ctx,
			};
			const data = await chrome.storage.local.get(LOG_KEY);
			const log = Array.isArray(data[LOG_KEY]) ? data[LOG_KEY] : [];
			log.push(entry);
			await chrome.storage.local.set({
				[LOG_KEY]: log.slice(-MAX_LOG_ENTRIES),
			});
		} catch (e) {
			console.debug("[Overlays] log:", e);
		}
	}

	OV.logEvent = appendLog;

	OV.toast = function showToast(msg, ms = 5500) {
		void ms;
		if (isProblemMessage(msg)) void appendLog("warn", msg, { source: "toast" });
	};

	OV.sleep = (ms) => new Promise((r) => setTimeout(r, ms));
})();
