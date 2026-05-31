// ============================================================
// storage.js — chrome.storage statt GM_getValue/GM_setValue
// ------------------------------------------------------------
// GM_getValue war synchron, chrome.storage ist asynchron. Loesung:
// Beim Start einmal alles in einen synchronen Cache laden, danach
// synchron lesen (OV.storage.get) und asynchron schreiben (OV.storage.set).
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
	};

	const cache = { ...DEFAULTS };

	OV.storage = {
		// Einmal beim Boot aufrufen — fuellt den synchronen Cache.
		async load() {
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
			cache[key] = value;
			try {
				await chrome.storage.local.set({ [key]: value });
			} catch (e) {
				console.warn("[Overlays] storage.set:", e);
			}
		},
	};
})();
