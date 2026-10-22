/*
 * settings.js — zentrale Einstellungs-Verwaltung.
 *
 * Läuft als Content-Script (vor overlay.js) und stellt die Funktionen
 * über `window.VOSettings` bereit. Alle Werte liegen in chrome.storage.local
 * (NICHT sync) — der Google-API-Key bleibt damit ausschließlich lokal.
 *
 * Gespeichert werden:
 *   - aktive Engine
 *   - pro Engine: gewählte Stimme + Vorlese-Tempo
 *   - Edge: Tonhöhe (pitch, -50 bis +50 Halbtöne)
 *   - Google-API-Key
 *   - Overlay-Position
 *   - autoSpeak: Vorlesen bei Textauswahl automatisch starten
 *   - disabledHosts: Webseiten, auf denen das Overlay ausgeschaltet ist
 */
(function () {
	"use strict";

	if (window.VOSettings) return; // Doppel-Injektion vermeiden

	const STORE_KEY = "vo_settings";
	const POS_KEY = "vo_position";
	const PANEL_POS_KEY = "vo_panel_position";

	const DEFAULTS = Object.freeze({
		activeEngine: "edge", // "edge" | "google" | "gemini" | "azure"
		edge: {
			voice: "de-DE-KatjaNeural", // sinnvolle Standard-Stimme bis die Liste geladen ist
			rate: 1.0,
			pitch: 0, // Tonhöhe in Halbtönen (-50 bis +50), SSML prosody pitch
		},
		google: {
			apiKey: "",
			voice: "", // wird nach Key-Eingabe dynamisch befüllt
			rate: 1.0,
		},
		// Gemini 3.8 TTS (Flash/Flash-Lite steckt in der Kennung "gemini_flash:Leda").
		gemini: {
			voice: "gemini_flash:Leda",
			rate: 1.0,
			keyFree: "", // Schluessel kostenlos (Projekt ohne Abrechnung)
			keyPaid: "", // Schluessel bezahlt (Projekt mit Abrechnung)
			keyChoice: "frei_dann_bezahlt", // | "nur_frei" | "nur_bezahlt"
			chirpFallback: false, // Rueckfall auf Chirp 3 HD statt bezahlt
		},
		// Azure Neural (eigene Speech-Ressource)
		azure: {
			key: "",
			region: "westeurope",
			voice: "azure:de-DE-SeraphinaMultilingualNeural",
			rate: 1.0,
		},
		autoSpeak: false, // Haekchen: markierten Text automatisch vorlesen
		autoMode: false, // A-Button: ab markiertem Wort absatzweise bis Ende vorlesen
		favoriteVoices: [], // mit Stern markierte Lieblings-Stimmen (Liste von IDs)
		draggable: false, // Overlay per Maus verschiebbar (sonst fester Platz)
		disabledHosts: [], // Hostnamen, auf denen das Overlay nicht angezeigt wird
	});

	// Tiefe, robuste Zusammenführung mit den Defaults, damit fehlende Felder
	// (z.B. nach einem Update) immer einen gültigen Wert haben.
	function mergeDefaults(stored) {
		const s = stored && typeof stored === "object" ? stored : {};
		return {
			activeEngine: ["google", "gemini", "azure"].includes(s.activeEngine)
				? s.activeEngine
				: "edge",
			edge: {
				voice: (s.edge && s.edge.voice) || DEFAULTS.edge.voice,
				rate: clampRate((s.edge && s.edge.rate) ?? DEFAULTS.edge.rate),
				pitch: clampPitch((s.edge && s.edge.pitch) ?? DEFAULTS.edge.pitch),
			},
			google: {
				apiKey: (s.google && s.google.apiKey) || "",
				voice: (s.google && s.google.voice) || "",
				rate: clampRate((s.google && s.google.rate) ?? DEFAULTS.google.rate),
			},
			gemini: {
				voice:
					s.gemini && /^gemini_(flash|lite):\w+$/.test(s.gemini.voice || "")
						? s.gemini.voice
						: DEFAULTS.gemini.voice,
				rate: clampRate((s.gemini && s.gemini.rate) ?? DEFAULTS.gemini.rate),
				keyFree: (s.gemini && s.gemini.keyFree) || "",
				keyPaid: (s.gemini && s.gemini.keyPaid) || "",
				keyChoice: ["frei_dann_bezahlt", "nur_frei", "nur_bezahlt"].includes(
					s.gemini && s.gemini.keyChoice,
				)
					? s.gemini.keyChoice
					: DEFAULTS.gemini.keyChoice,
				chirpFallback: !!(s.gemini && s.gemini.chirpFallback),
			},
			azure: {
				key: (s.azure && s.azure.key) || "",
				region: (s.azure && s.azure.region) || DEFAULTS.azure.region,
				voice: (s.azure && s.azure.voice) || DEFAULTS.azure.voice,
				rate: clampRate((s.azure && s.azure.rate) ?? DEFAULTS.azure.rate),
			},
			autoSpeak:
				typeof s.autoSpeak === "boolean" ? s.autoSpeak : DEFAULTS.autoSpeak,
			autoMode:
				typeof s.autoMode === "boolean" ? s.autoMode : DEFAULTS.autoMode,
			draggable:
				typeof s.draggable === "boolean" ? s.draggable : DEFAULTS.draggable,
			// Lieblings-Stimmen: nur eindeutige, nicht-leere String-IDs uebernehmen.
			favoriteVoices: Array.isArray(s.favoriteVoices)
				? Array.from(
						new Set(
							s.favoriteVoices.filter(
								(x) => typeof x === "string" && x.length > 0,
							),
						),
					)
				: [],
			disabledHosts: Array.isArray(s.disabledHosts)
				? Array.from(
						new Set(
							s.disabledHosts
								.map((x) => String(x || "").toLowerCase().trim())
								.filter(Boolean),
						),
					)
				: [],
		};
	}

	// Tonhöhe auf ganzzahligen Wert im Bereich -50 bis +50 Halbtöne begrenzen.
	function clampPitch(v) {
		const n = Math.round(Number(v));
		if (!isFinite(n)) return 0;
		return Math.min(50, Math.max(-50, n));
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
			// Diagnose: gespeicherte Einstellungen (API-Key NICHT, nur ob vorhanden)
			if (window.VODiag)
				window.VODiag.log("INFO", "ZUSTAND", "settings.save", {
					activeEngine: clean.activeEngine,
					edgeVoice: clean.edge.voice,
					edgeRate: clean.edge.rate,
					edgePitch: clean.edge.pitch,
					googleVoice: clean.google.voice,
					googleRate: clean.google.rate,
					hatGoogleKey: !!(clean.google.apiKey || "").trim(),
					autoSpeak: clean.autoSpeak,
					favoritenAnzahl: clean.favoriteVoices.length,
				});
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

	// Position des Einstellungs-Panels (vom Benutzer per Ziehen gesetzt).
	// Bleibt null, solange der Benutzer das Panel nie verschoben hat -> dann
	// positioniert sich das Panel automatisch neben dem Overlay.
	function getPanelPosition() {
		return new Promise((resolve) => {
			try {
				chrome.storage.local.get([PANEL_POS_KEY], (res) => {
					if (chrome.runtime.lastError || !res || !res[PANEL_POS_KEY]) {
						resolve(null);
						return;
					}
					const p = res[PANEL_POS_KEY];
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

	function setPanelPosition(left, top) {
		return new Promise((resolve) => {
			try {
				chrome.storage.local.set({ [PANEL_POS_KEY]: { left, top } }, () =>
					resolve(),
				);
			} catch (e) {
				resolve();
			}
		});
	}

	window.VOSettings = {
		DEFAULTS,
		clampRate,
		clampPitch,
		load,
		save,
		getPosition,
		setPosition,
		getPanelPosition,
		setPanelPosition,
	};
})();
