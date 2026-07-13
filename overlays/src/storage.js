// ============================================================
// storage.js — chrome.storage statt GM_getValue/GM_setValue
// ------------------------------------------------------------
// GM_getValue war synchron, chrome.storage ist asynchron. Loesung:
// Beim Start einmal alles in einen synchronen Cache laden, danach
// synchron lesen (OV.storage.get) und asynchron schreiben (OV.storage.set).
//
// Robust gegen "invalidierten Kontext": Wird die Erweiterung aktualisiert,
// aber der Tab NICHT neu geladen, verliert das alte Content-Script den
// Zugriff auf chrome.* (chrome.storage/runtime werden undefined). Dann
// nicht crashen, sondern in-memory weiterarbeiten und EINMAL klar darauf
// hinweisen, dass die Seite neu geladen werden muss (F5).
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	const DEFAULTS = {
		groqKey: "",
		geminiKey: "",
		geminiModel: "",
		autoGeminiCorrection: true,
		whisperModel: "whisper-large-v3-turbo",
		whisperLang: "de",
		ovDisabledHosts: [],
		ovCustomHosts: [],
	};

	const cache = { ...DEFAULTS };

	// Lebt der Erweiterungs-Kontext noch? (nach Update ohne Tab-Reload: nein)
	function ctxAlive() {
		try {
			return !!(
				chrome &&
				chrome.runtime &&
				chrome.runtime.id &&
				chrome.storage &&
				chrome.storage.local
			);
		} catch {
			return false;
		}
	}

	let warnedDead = false;
	function warnContextDead() {
		if (warnedDead) return;
		warnedDead = true;
		console.warn(
			"[Overlays] Erweiterungs-Kontext ungueltig (Update?). Bitte Seite neu laden (F5).",
		);
		try {
			OV.toast?.(
				"🔄 Erweiterung wurde aktualisiert — bitte diese Seite neu laden (F5).",
				9000,
			);
		} catch {}
	}

	OV.ctxAlive = ctxAlive;

	OV.storage = {
		// Einmal beim Boot aufrufen — fuellt den synchronen Cache.
		async load() {
			if (!ctxAlive()) {
				warnContextDead();
				return { ...cache };
			}
			try {
				const all = await chrome.storage.local.get(Object.keys(DEFAULTS));
				for (const k of Object.keys(DEFAULTS)) {
					cache[k] = all[k] !== undefined ? all[k] : DEFAULTS[k];
				}
			} catch (e) {
				console.warn("[Overlays] storage.load:", e);
			}
			return { ...cache };
		},
		get(key, fallback) {
			const v = cache[key];
			return v === undefined ? fallback : v;
		},
		async set(key, value) {
			cache[key] = value; // in-memory immer setzen (Toggle wirkt sofort)
			if (!ctxAlive()) {
				warnContextDead();
				return;
			}
			try {
				await chrome.storage.local.set({ [key]: value });
			} catch (e) {
				console.warn("[Overlays] storage.set:", e);
			}
		},
	};
})();
