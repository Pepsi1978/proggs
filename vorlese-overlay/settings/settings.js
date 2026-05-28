/*
 * settings.js — zentrale Einstellungs-Verwaltung.
 *
 * Laeuft als Content-Script (vor overlay.js) und stellt die Funktionen
 * ueber `window.VOSettings` bereit. Alle Werte liegen in chrome.storage.local
 * (NICHT sync) — der Google-API-Key bleibt damit ausschliesslich lokal.
 *
 * Gespeichert werden:
 *   - aktive Engine
 *   - pro Engine: gewaehlte Stimme + Vorlese-Tempo
 *   - Google-API-Key
 *   - Overlay-Position
 */
(function () {
	"use strict";

	if (window.VOSettings) return; // Doppel-Injektion vermeiden

	const STORE_KEY = "vo_settings";
	const POS_KEY = "vo_position";

	const DEFAULTS = Object.freeze({
		activeEngine: "edge", // "edge" | "google"
		edge: {
			voice: "de-DE-KatjaNeural", // sinnvolle Standard-Stimme bis die Liste geladen ist
			rate: 1.0,
		},
		google: {
			apiKey: "",
			voice: "", // wird nach Key-Eingabe dynamisch befuellt
			rate: 1.0,
		},
	});

	// Tiefe, robuste Zusammenfuehrung mit den Defaults, damit fehlende Felder
	// (z.B. nach einem Update) immer einen gueltigen Wert haben.
	function mergeDefaults(stored) {
		const s = stored && typeof stored === "object" ? stored : {};
		return {
			activeEngine: s.activeEngine === "google" ? "google" : "edge",
			edge: {
				voice: (s.edge && s.edge.voice) || DEFAULTS.edge.voice,
				rate: clampRate((s.edge && s.edge.rate) ?? DEFAULTS.edge.rate),
			},
			google: {
				apiKey: (s.google && s.google.apiKey) || "",
				voice: (s.google && s.google.voice) || "",
				rate: clampRate((s.google && s.google.rate) ?? DEFAULTS.google.rate),
			},
		};
	}

	function clampRate(v) {
		const n = Number(v);
		if (!isFinite(n)) return 1.0;
		return Math.min(2.0, Math.max(0.5, n));
	}

	function load() {
		return new Promise((resolve) => {
			try {
				chrome.storage.local.get([STORE_KEY], (res) => {
					if (chrome.runtime.lastError) {
						resolve(mergeDefaults(null));
						return;
					}
					resolve(mergeDefaults(res ? res[STORE_KEY] : null));
				});
			} catch (e) {
				resolve(mergeDefaults(null));
			}
		});
	}

	function save(settings) {
		return new Promise((resolve) => {
			const clean = mergeDefaults(settings);
			try {
				chrome.storage.local.set({ [STORE_KEY]: clean }, () => resolve(clean));
			} catch (e) {
				resolve(clean);
			}
		});
	}

	function getPosition() {
		return new Promise((resolve) => {
			try {
				chrome.storage.local.get([POS_KEY], (res) => {
					if (chrome.runtime.lastError || !res || !res[POS_KEY]) {
						resolve(null);
						return;
					}
					const p = res[POS_KEY];
					if (typeof p.left === "number" && typeof p.top === "number") {
						resolve({ left: p.left, top: p.top });
					} else {
						resolve(null);
					}
				});
			} catch (e) {
				resolve(null);
			}
		});
	}

	function setPosition(left, top) {
		return new Promise((resolve) => {
			try {
				chrome.storage.local.set({ [POS_KEY]: { left, top } }, () => resolve());
			} catch (e) {
				resolve();
			}
		});
	}

	window.VOSettings = {
		DEFAULTS,
		clampRate,
		load,
		save,
		getPosition,
		setPosition,
	};
})();
