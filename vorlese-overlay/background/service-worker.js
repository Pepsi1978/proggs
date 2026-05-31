/*
 * service-worker.js — der Hintergrund-Dienst (ES-Modul).
 *
 * Hier laufen ALLE Netzwerk-Aufrufe (Edge-WebSocket, Google-REST). Der Worker
 * läuft im Ursprung der Erweiterung und umgeht damit die CSP der jeweiligen
 * Webseite (die Hosts stehen in host_permissions).
 *
 * Der Worker kann selbst kein Audio abspielen — dafür erzeugt er ein
 * verstecktes Offscreen-Dokument (chrome.offscreen) und schickt ihm die
 * fertigen Audiodaten (als Data-URL).
 *
 * Datenfluss:
 *   Overlay (SPEAK) -> Worker (Synthese) -> Offscreen (ENQUEUE/Wiedergabe)
 *   Offscreen (STARTED/ENDED/ERROR) -> Worker -> Overlay (Status)
 *
 *   Pause/Resume:
 *   Overlay (TTS_PAUSE/TTS_RESUME/TTS_PAUSE_RESUME) -> Worker -> Offscreen (PAUSE/RESUME)
 *   Offscreen (OFFSCREEN_PAUSED/OFFSCREEN_RESUMED) -> Worker -> Overlay (TTS_STATE)
 */

import * as edge from "../engines/edge-tts.js";
import * as google from "../engines/google-tts.js";
import { splitIntoChunks } from "../engines/chunker.js";
import { diag } from "../diag/diag.js";
import { analyze } from "../diag/insights.js";

// Diagnose-Schicht initialisieren (No-Op solange der Diagnose-Modus aus ist).
diag.init("service-worker");
// Im Service-Worker-Kontext erreichbar machen — für den Playwright-Selbsttest
// (liest die Logs per readAllJsonl) und die manuelle Inspektion in der Konsole.
globalThis.__voDiag = diag;

// Selbsttest bequem aus der Service-Worker-Konsole auslösen: __voSelftest()
// (chrome.runtime.sendMessage erreicht den SW-eigenen Listener NICHT, daher
//  hier der direkte Weg: Diagnose an + VO_SELFTEST_BEGIN an den aktiven Tab.)
globalThis.__voSelftest = function () {
	chrome.storage.local.set({ vo_diag: true }, () => {
		chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
			const tab = tabs && tabs[0];
			if (tab && tab.id != null) {
				chrome.tabs.sendMessage(
					tab.id,
					{ type: "VO_SELFTEST_BEGIN" },
					() => void chrome.runtime.lastError,
				);
			}
		});
	});
	return "Selbsttest gestartet — nach ~25 s: chrome.runtime.sendMessage({type:'VO_DIAG_EXPORT'})";
};

// App-Verbesserungsvorschläge aus den gesammelten Logs ableiten: __voInsights()
// Liefert Kennzahlen + priorisierte Vorschläge und loggt sie lesbar in die Konsole.
globalThis.__voInsights = async function () {
	const jsonl = await diag.readAllJsonl();
	const r = analyze(jsonl);
	console.log("[VO-DIAG] Insights — Kennzahlen:", r.kennzahlen);
	console.log(
		"[VO-DIAG] App-Verbesserungsvorschläge (" + r.vorschlaege.length + "):",
	);
	for (const v of r.vorschlaege) {
		console.log(
			"  [" +
				v.prioritaet +
				"] " +
				v.thema +
				": " +
				v.befund +
				" -> " +
				v.vorschlag,
		);
	}
	return r;
};

// ----- Message-Typ-Konstanten -----------------------------------------------
const MSG = {
	SPEAK: "TTS_SPEAK",
	STOP: "TTS_STOP",
	GET_VOICES: "GET_VOICES",
	TEST: "TTS_TEST",
	STATE: "TTS_STATE",
	// Pause/Resume (Content -> SW -> Offscreen)
	PAUSE: "TTS_PAUSE",
	RESUME: "TTS_RESUME",
	PAUSE_RESUME: "TTS_PAUSE_RESUME", // Toggle: SW prüft isPaused und delegiert
	// Fortschrittsanzeige (SW -> Content)
	PROGRESS: "TTS_PROGRESS",
	// Wort-Highlight-Basis (SW -> Content)
	CHUNK_TIMING: "TTS_CHUNK_TIMING",
	// Status-Werte
	STATE_PAUSED: "paused",
	STATE_RESUMED: "resumed",
	// Kontextmenü (intern)
	CONTEXT_MENU_SPEAK: "CONTEXT_MENU_SPEAK",
};

const OFFSCREEN_PATH = "offscreen/offscreen.html";
let creatingOffscreen = null;

// Generations-Zähler: jede neue SPEAK/STOP erhöht ihn. Ergebnisse einer
// Synthese, deren Generation veraltet ist, werden verworfen (sauberer Abbruch
// auch mitten im Chunking).
let currentGen = 0;
// Tab, an den Status-Meldungen gehen (der zuletzt auslösende Tab).
let activeTabId = null;
// NUTZUNG-Sonden: Zeitmarken für Synthese-Latenz und Wiedergabe-Dauer.
let lastSpeakTs = 0;
let playStartTs = 0;
// Pause-Zustand: true wenn die Wiedergabe aktuell pausiert ist.
let isPaused = false;

function getEngine(name) {
	return name === "google" ? google : edge;
}

// ----- Offscreen-Dokument sicherstellen (Audio-Wiedergabe) ------------------
async function ensureOffscreen() {
	try {
		if (chrome.offscreen.hasDocument) {
			const has = await chrome.offscreen.hasDocument();
			if (has) return;
		}
	} catch (e) {
		/* hasDocument evtl. nicht verfügbar — weiter zur Erstellung */
	}

	if (creatingOffscreen) {
		await creatingOffscreen;
		return;
	}
	creatingOffscreen = chrome.offscreen
		.createDocument({
			url: OFFSCREEN_PATH,
			reasons: ["AUDIO_PLAYBACK"],
			justification: "Vorlesen von markiertem Text — Audio-Wiedergabe.",
		})
		.catch((e) => {
			// Wenn das Dokument bereits existiert, ist das kein echter Fehler.
			if (!String(e && e.message).includes("Only a single offscreen")) throw e;
		});
	try {
		await creatingOffscreen;
	} finally {
		creatingOffscreen = null;
	}
}

// ----- Status an das Content-Script des auslösenden Tabs senden -------------
function notifyTab(tabId, payload) {
	if (tabId == null) return;
	try {
		chrome.tabs.sendMessage(
			tabId,
			payload,
			() => void chrome.runtime.lastError,
		);
	} catch (e) {
		/* Tab evtl. geschlossen */
	}
}

// ----- Nachrichten an das Offscreen-Dokument --------------------------------
function sendToOffscreen(payload) {
	try {
		chrome.runtime.sendMessage(
			Object.assign({ target: "offscreen" }, payload),
			() => void chrome.runtime.lastError,
		);
	} catch (e) {
		/* Offscreen evtl. nicht aktiv */
	}
}

// ----- Pause ausführen (intern, von TTS_PAUSE und Toggle genutzt) -----------
function executePause() {
	isPaused = true;
	sendToOffscreen({ type: "PAUSE" });
	notifyTab(activeTabId, { type: MSG.STATE, state: MSG.STATE_PAUSED });
	diag.log("INFO", "ZUSTAND", "sw:pause", {});
}

// ----- Resume ausführen (intern, von TTS_RESUME und Toggle genutzt) ---------
function executeResume() {
	isPaused = false;
	sendToOffscreen({ type: "RESUME" });
	notifyTab(activeTabId, { type: MSG.STATE, state: MSG.STATE_RESUMED });
	diag.log("INFO", "ZUSTAND", "sw:resume", {});
}

// ----- Nachrichten-Router ---------------------------------------------------
chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
	if (!msg || typeof msg !== "object") return false;

	// ----- Diagnose-Schicht (additiv; im Aus-Zustand No-Op) ------------------
	// Logs des Content-Scripts (anderer Ursprung) zentral ablegen + Export/Clear.
	if (msg.type === "VO_DIAG_LOG") {
		diag.ingest(msg.entry);
		return false;
	}
	if (msg.type === "VO_DIAG_EXPORT") {
		diag
			.exportLogs()
			.then((r) => sendResponse(Object.assign({ ok: true }, r)))
			.catch((e) =>
				sendResponse({ ok: false, error: String((e && e.message) || e) }),
			);
		return true;
	}
	if (msg.type === "VO_DIAG_CLEAR") {
		diag.clearLogs().then(() => sendResponse({ ok: true }));
		return true;
	}
	if (msg.type === "VO_DIAG_COUNT") {
		diag.getCount().then((n) => sendResponse({ count: n }));
		return true;
	}
	// Erweiterung auf die neue Version aktualisieren (entpackte Erweiterung neu
	// vom Datenträger laden). chrome.runtime.reload() BEHAELT chrome.storage.local
	// — anders als "Entfernen + neu laden", das den Speicher (API-Key, Stimme,
	// Position) loescht. Der Aktualisieren-Button im Panel ruft das hier auf.
	if (msg.type === "RELOAD_EXTENSION") {
		diag.log("INFO", "NUTZUNG", "sw:reload_extension", {});
		sendResponse({ ok: true });
		// Nach dem Antworten neu laden, damit der Aufrufer keine Verbindung verliert.
		setTimeout(() => {
			try {
				chrome.runtime.reload();
			} catch (e) {
				/* egal */
			}
		}, 80);
		return true;
	}
	// Selbsttest starten: Diagnose-Modus an + Runner im aktiven Tab auslösen.
	if (msg.type === "VO_SELFTEST_RUN") {
		chrome.storage.local.set({ vo_diag: true }, () => {
			chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
				const tab = tabs && tabs[0];
				if (tab && tab.id != null) {
					chrome.tabs.sendMessage(
						tab.id,
						{ type: "VO_SELFTEST_BEGIN" },
						() => void chrome.runtime.lastError,
					);
					sendResponse({ ok: true, tabId: tab.id });
				} else {
					sendResponse({ ok: false, error: "Kein aktiver Tab gefunden." });
				}
			});
		});
		return true;
	}

	// Rückmeldungen vom Offscreen-Dokument (Wiedergabe-Status)
	if (msg.target === "background") {
		handleOffscreenFeedback(msg);
		return false;
	}
	// Eigene Nachrichten ans Offscreen ignorieren (werden mit-empfangen)
	if (msg.target === "offscreen") return false;

	const tabId = sender.tab ? sender.tab.id : undefined;

	switch (msg.type) {
		case MSG.GET_VOICES:
			handleGetVoices(msg)
				.then((voices) => sendResponse({ voices }))
				.catch((e) => sendResponse({ error: humanError(e) }));
			return true; // asynchrone Antwort

		case MSG.SPEAK:
		case MSG.TEST:
			handleSpeak(msg, tabId);
			return false;

		case MSG.STOP:
			stopPlayback();
			return false;

		// ----- Pause / Resume / Toggle ----------------------------------------
		case MSG.PAUSE:
			executePause();
			return false;

		case MSG.RESUME:
			executeResume();
			return false;

		case MSG.PAUSE_RESUME:
			// Toggle: pausieren wenn läuft, fortsetzen wenn pausiert
			if (isPaused) {
				executeResume();
			} else {
				executePause();
			}
			return false;

		default:
			return false;
	}
});

async function handleGetVoices(msg) {
	const eng = getEngine(msg.engine);
	return await eng.listGermanVoices(msg.apiKey);
}

// ----- Vorlesen: Text -> (Chunks) -> Synthese -> Offscreen ------------------
async function handleSpeak(msg, tabId) {
	const gen = ++currentGen; // bricht alle vorher laufenden Synthesen ab
	isPaused = false; // neues Sprechen setzt Pause-Zustand zurück
	if (tabId != null) activeTabId = tabId;
	lastSpeakTs = performance.now();

	const text = (msg.text || "").trim();
	diag.log("INFO", "FUNKTION", "handleSpeak:eintritt", {
		engine: msg.engine || "edge",
		voice: msg.voice,
		rate: msg.rate,
		textLen: text.length,
		test: msg.type === MSG.TEST,
		gen,
	});
	if (!text) {
		diag.log("WARN", "FEHLER", "handleSpeak:kein_text", {});
		notifyTab(tabId, {
			type: MSG.STATE,
			state: "error",
			message: "Bitte zuerst Text markieren.",
		});
		return;
	}

	try {
		await ensureOffscreen();
		sendToOffscreen({ type: "STOP" }); // evtl. laufende Wiedergabe beenden

		if (msg.engine === "google") {
			// Google: REST-fetch läuft hier im Worker (DNR greift für fetch).
			// Langen Text an Satzgrenzen teilen; das erste Stück ist schnell hörbar.
			const chunks = splitIntoChunks(text, 2500);
			const totalChunks = chunks.length;
			let enqueuedAny = false;
			for (let i = 0; i < chunks.length; i++) {
				const part = chunks[i];

				// Fortschritts-Nachricht vor dem ENQUEUE ans Content senden
				notifyTab(tabId, {
					type: MSG.PROGRESS,
					chunk: i,
					totalChunks,
				});

				// Wort-Timing-Nachricht für Highlight-Timer im Content
				const words = part.split(/\s+/).filter((w) => w.length > 0);
				notifyTab(tabId, {
					type: MSG.CHUNK_TIMING,
					words,
					chunkIndex: i,
					totalChunks,
				});

				const bytes = await google.synthesize(
					part,
					msg.voice,
					msg.rate,
					msg.apiKey,
				);
				if (gen !== currentGen) return; // zwischenzeitlich gestoppt/ersetzt
				if (bytes && bytes.length > 0) {
					sendToOffscreen({ type: "ENQUEUE", url: bytesToDataUrl(bytes) });
					enqueuedAny = true;
				}
			}
			if (gen === currentGen) {
				if (enqueuedAny) {
					// Alle Google-Stuecke geliefert -> Offscreen darf das Ende
					// melden, sobald die Wiedergabe durch ist (kein verfruehtes Ende).
					sendToOffscreen({ type: "ENQUEUE_DONE" });
				} else {
					notifyTab(tabId, {
						type: MSG.STATE,
						state: "error",
						message: "Keine Audiodaten erhalten.",
					});
				}
			}
		} else {
			// Edge: Der WebSocket MUSS im Offscreen-Dokument geöffnet werden.
			// declarativeNetRequest (setzt den nötigen Edge-User-Agent) greift wegen
			// Chromium-Bug 1285664 NICHT auf WebSocket-Upgrades aus einem Service-
			// Worker — wohl aber aus einem normalen Dokument. Chunking + Synthese +
			// Wiedergabe laufen daher komplett im Offscreen.
			//
			// Für Edge: Fortschritts- und Timing-Nachrichten mit einem einzelnen
			// virtuellen Chunk senden (Edge verarbeitet intern, kein Chunk-Split hier).
			notifyTab(tabId, {
				type: MSG.PROGRESS,
				chunk: 0,
				totalChunks: 1,
			});
			const words = text.split(/\s+/).filter((w) => w.length > 0);
			notifyTab(tabId, {
				type: MSG.CHUNK_TIMING,
				words,
				chunkIndex: 0,
				totalChunks: 1,
			});
			sendToOffscreen({
				type: "EDGE_SPEAK",
				text,
				voice: msg.voice,
				rate: msg.rate,
				pitch: msg.pitch ?? 0,
			});
		}
	} catch (e) {
		if (gen !== currentGen) return;
		diag.log("ERROR", "FEHLER", "handleSpeak:fehler", {
			engine: msg.engine || "edge",
			message: String((e && e.message) || e),
			stack: e && e.stack,
		});
		notifyTab(tabId, {
			type: MSG.STATE,
			state: "error",
			message: humanError(e),
		});
	}
}

function stopPlayback() {
	// NUTZUNG: manueller Stop — kurze Spielzeit deutet auf Unzufriedenheit hin
	// (falsche Stimme/Tempo/Text) -> Signal für App-Verbesserungsvorschläge.
	const spielzeit = playStartTs
		? Math.round(performance.now() - playStartTs)
		: 0;
	diag.log("INFO", "NUTZUNG", "manueller_stop", {
		spielzeit_ms: spielzeit,
		frueh_abgebrochen: spielzeit > 0 && spielzeit < 2000,
	});
	currentGen++; // laufende Synthese-Ergebnisse verwerfen
	isPaused = false; // Pause-Zustand zurücksetzen
	sendToOffscreen({ type: "STOP" });
}

// ----- Wiedergabe-Status vom Offscreen an das Overlay weiterreichen ---------
function handleOffscreenFeedback(msg) {
	diag.log(
		msg.type === "OFFSCREEN_ERROR" ? "ERROR" : "INFO",
		msg.type === "OFFSCREEN_ERROR" ? "FEHLER" : "ZUSTAND",
		"offscreen_feedback",
		{ typ: msg.type, message: msg.message },
	);
	switch (msg.type) {
		case "OFFSCREEN_STARTED":
			if (lastSpeakTs)
				diag.log("INFO", "NUTZUNG", "synthese_latenz", {
					ms: Math.round(performance.now() - lastSpeakTs),
				});
			playStartTs = performance.now();
			notifyTab(activeTabId, { type: MSG.STATE, state: "playing" });
			break;
		case "OFFSCREEN_ENDED":
			if (playStartTs) {
				diag.log("INFO", "NUTZUNG", "wiedergabe_dauer", {
					ms: Math.round(performance.now() - playStartTs),
				});
				playStartTs = 0;
			}
			isPaused = false; // Pause-Zustand bei normalem Ende zurücksetzen
			notifyTab(activeTabId, { type: MSG.STATE, state: "stopped" });
			break;
		case "OFFSCREEN_ERROR":
			notifyTab(activeTabId, {
				type: MSG.STATE,
				state: "error",
				message: msg.message || "Audio konnte nicht abgespielt werden.",
			});
			break;
		// Rückmeldungen vom Offscreen nach Pause/Resume
		case "OFFSCREEN_PAUSED":
			notifyTab(activeTabId, { type: MSG.STATE, state: MSG.STATE_PAUSED });
			break;
		case "OFFSCREEN_RESUMED":
			notifyTab(activeTabId, { type: MSG.STATE, state: MSG.STATE_RESUMED });
			break;
	}
}

// ----- MP3-Bytes -> Data-URL (im Worker gibt es kein URL.createObjectURL) ---
function bytesToDataUrl(bytes) {
	let binary = "";
	const step = 0x8000; // in Blöcken, sonst sprengt String.fromCharCode den Stack
	for (let i = 0; i < bytes.length; i += step) {
		binary += String.fromCharCode.apply(null, bytes.subarray(i, i + step));
	}
	return "data:audio/mpeg;base64," + btoa(binary);
}

// ----- Fehlertexte in verständliches Deutsch übersetzen --------------------
function humanError(e) {
	const m = String((e && e.message) || e || "Unbekannter Fehler");
	if (/Failed to fetch|NetworkError|net::|ERR_|Kein Internet/.test(m))
		return "Kein Internet erreichbar.";
	return m;
}

// ----- Tastaturkürzel -------------------------------------------------------
if (chrome.commands && chrome.commands.onCommand) {
	chrome.commands.onCommand.addListener((command) => {
		if (command === "speak-selection") {
			chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
				const tab = tabs && tabs[0];
				if (tab && tab.id != null) {
					chrome.tabs.sendMessage(
						tab.id,
						{ type: "SPEAK_COMMAND" },
						() => void chrome.runtime.lastError,
					);
				}
			});
		} else if (command === "pause-resume") {
			// Toggle: pausieren wenn läuft, fortsetzen wenn pausiert
			if (isPaused) {
				executeResume();
			} else {
				executePause();
			}
		}
	});
}

// ----- Kontextmenü: markierten Text vorlesen --------------------------------
// Wird beim Installieren einmalig angelegt; beim Update-Event ebenfalls,
// damit der Eintrag nach Erweiterungs-Updates immer vorhanden ist.
function setupContextMenu() {
	chrome.contextMenus.removeAll(() => {
		// Eintrag im Seiten-Kontextmenue (Rechtsklick auf markierten Text).
		chrome.contextMenus.create(
			{
				id: "vorlese-lesen",
				title:
					chrome.i18n.getMessage("contextMenuLesen") || "Markierung vorlesen",
				contexts: ["selection"],
			},
			() => void chrome.runtime.lastError,
		);
		// Eintrag im Aktions-Kontextmenue (Rechtsklick auf das Symbol in der
		// Toolbar): Erweiterung auf die neue Version aktualisieren, ohne den
		// gespeicherten API-Key/Stimme/Position zu verlieren.
		chrome.contextMenus.create(
			{
				id: "vorlese-aktualisieren",
				title:
					chrome.i18n.getMessage("contextMenuAktualisieren") ||
					"Auf neue Version aktualisieren",
				contexts: ["action"],
			},
			() => void chrome.runtime.lastError,
		);
	});
}

chrome.runtime.onInstalled.addListener(() => {
	setupContextMenu();
});

// Kontextmenü-Klick: gleiches Verhalten wie das Tastaturkürzel
if (chrome.contextMenus && chrome.contextMenus.onClicked) {
	chrome.contextMenus.onClicked.addListener((info, tab) => {
		// Aktualisieren (Rechtsklick auf das Toolbar-Symbol): neu laden, Speicher bleibt.
		if (info.menuItemId === "vorlese-aktualisieren") {
			diag.log("INFO", "NUTZUNG", "sw:contextmenu_aktualisieren", {});
			try {
				chrome.runtime.reload();
			} catch (e) {
				/* egal */
			}
			return;
		}
		if (info.menuItemId !== "vorlese-lesen") return;
		if (!tab || tab.id == null) return;
		// Diagnose-Sonde: Nutzung über das Kontextmenü erfassen
		diag.log("INFO", "NUTZUNG", "sw:contextmenu_vorlesen", {
			tabId: tab.id,
		});
		chrome.tabs.sendMessage(
			tab.id,
			{ type: "SPEAK_COMMAND" },
			() => void chrome.runtime.lastError,
		);
	});
}

// Linksklick auf das Toolbar-Symbol oeffnet das Einstellungs-Panel direkt auf der
// aktiven Seite (das Zahnrad im Overlay entfaellt dadurch). War die Seite schon
// vor dem Laden der Erweiterung offen, wird das Content-Script per activeTab
// nachinjiziert und der Befehl wiederholt.
const SETTINGS_FILES = [
	"diag/diag-content.js",
	"settings/settings.js",
	"content/overlay.js",
];

function sendOpenSettings(tabId) {
	return new Promise((resolve) => {
		chrome.tabs.sendMessage(tabId, { type: "OPEN_SETTINGS" }, () => {
			const err = chrome.runtime.lastError;
			// Nur "kein Content-Script vorhanden" gilt als Fehlschlag; ein benignes
			// "port closed" (kein sendResponse) wird als Erfolg gewertet.
			const noReceiver =
				err &&
				/Receiving end does not exist|Could not establish connection/i.test(
					err.message || "",
				);
			resolve(!noReceiver);
		});
	});
}

if (chrome.action && chrome.action.onClicked) {
	chrome.action.onClicked.addListener(async (tab) => {
		if (!tab || tab.id == null) return;
		diag.log("INFO", "NUTZUNG", "sw:action_open_settings", { tabId: tab.id });
		if (await sendOpenSettings(tab.id)) return;
		// Self-heal: Content-Script in einen bereits offenen Tab nachladen.
		try {
			await chrome.scripting.executeScript({
				target: { tabId: tab.id },
				files: SETTINGS_FILES,
			});
			await sendOpenSettings(tab.id);
		} catch (e) {
			diag.log("INFO", "FEHLER", "sw:action_inject_failed", {
				error: String(e && e.message),
			});
		}
	});
}

export { ensureOffscreen, notifyTab, humanError };
