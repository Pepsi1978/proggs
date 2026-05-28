/*
 * service-worker.js — der Hintergrund-Dienst (ES-Modul).
 *
 * Hier laufen ALLE Netzwerk-Aufrufe (Edge-WebSocket, Google-REST). Der Worker
 * laeuft im Ursprung der Erweiterung und umgeht damit die CSP der jeweiligen
 * Webseite (die Hosts stehen in host_permissions).
 *
 * Der Worker kann selbst kein Audio abspielen — dafuer erzeugt er ein
 * verstecktes Offscreen-Dokument (chrome.offscreen) und schickt ihm die
 * fertigen Audiodaten (als Data-URL).
 *
 * Datenfluss:
 *   Overlay (SPEAK) -> Worker (Synthese) -> Offscreen (ENQUEUE/Wiedergabe)
 *   Offscreen (STARTED/ENDED/ERROR) -> Worker -> Overlay (Status)
 */

import * as edge from "../engines/edge-tts.js";
import * as google from "../engines/google-tts.js";
import { splitIntoChunks } from "../engines/chunker.js";

const MSG = {
	SPEAK: "TTS_SPEAK",
	STOP: "TTS_STOP",
	GET_VOICES: "GET_VOICES",
	TEST: "TTS_TEST",
	STATE: "TTS_STATE",
};

const OFFSCREEN_PATH = "offscreen/offscreen.html";
let creatingOffscreen = null;

// Generations-Zaehler: jede neue SPEAK/STOP erhoeht ihn. Ergebnisse einer
// Synthese, deren Generation veraltet ist, werden verworfen (sauberer Abbruch
// auch mitten im Chunking).
let currentGen = 0;
// Tab, an den Status-Meldungen gehen (der zuletzt ausloesende Tab).
let activeTabId = null;

function getEngine(name) {
	return name === "google" ? google : edge;
}

// ----- Offscreen-Dokument sicherstellen (Audio-Wiedergabe) -----------------
async function ensureOffscreen() {
	try {
		if (chrome.offscreen.hasDocument) {
			const has = await chrome.offscreen.hasDocument();
			if (has) return;
		}
	} catch (e) {
		/* hasDocument evtl. nicht verfuegbar — weiter zur Erstellung */
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

// ----- Status an das Content-Script des ausloesenden Tabs senden -----------
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

// ----- Nachrichten an das Offscreen-Dokument -------------------------------
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

// ----- Nachrichten-Router ---------------------------------------------------
chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
	if (!msg || typeof msg !== "object") return false;

	// Rueckmeldungen vom Offscreen-Dokument (Wiedergabe-Status)
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
	if (tabId != null) activeTabId = tabId;

	const text = (msg.text || "").trim();
	if (!text) {
		notifyTab(tabId, {
			type: MSG.STATE,
			state: "error",
			message: "Bitte zuerst Text markieren.",
		});
		return;
	}

	try {
		const eng = getEngine(msg.engine);
		await ensureOffscreen();
		sendToOffscreen({ type: "STOP" }); // evtl. laufende Wiedergabe beenden

		// Langen Text an Satzgrenzen aufteilen, damit die Dienste-Limits nicht
		// gerissen werden und das erste Stueck schnell zu hoeren ist.
		const chunks = splitIntoChunks(text, msg.engine === "google" ? 2500 : 1200);

		let enqueuedAny = false;
		for (const part of chunks) {
			const bytes = await eng.synthesize(part, msg.voice, msg.rate, msg.apiKey);
			if (gen !== currentGen) return; // zwischenzeitlich gestoppt/ersetzt
			if (bytes && bytes.length > 0) {
				sendToOffscreen({ type: "ENQUEUE", url: bytesToDataUrl(bytes) });
				enqueuedAny = true;
			}
		}

		if (!enqueuedAny && gen === currentGen) {
			notifyTab(tabId, {
				type: MSG.STATE,
				state: "error",
				message: "Keine Audiodaten erhalten.",
			});
		}
	} catch (e) {
		if (gen !== currentGen) return;
		notifyTab(tabId, {
			type: MSG.STATE,
			state: "error",
			message: humanError(e),
		});
	}
}

function stopPlayback() {
	currentGen++; // laufende Synthese-Ergebnisse verwerfen
	sendToOffscreen({ type: "STOP" });
}

// ----- Wiedergabe-Status vom Offscreen an das Overlay weiterreichen ---------
function handleOffscreenFeedback(msg) {
	switch (msg.type) {
		case "OFFSCREEN_STARTED":
			notifyTab(activeTabId, { type: MSG.STATE, state: "playing" });
			break;
		case "OFFSCREEN_ENDED":
			notifyTab(activeTabId, { type: MSG.STATE, state: "stopped" });
			break;
		case "OFFSCREEN_ERROR":
			notifyTab(activeTabId, {
				type: MSG.STATE,
				state: "error",
				message: msg.message || "Audio konnte nicht abgespielt werden.",
			});
			break;
	}
}

// ----- MP3-Bytes -> Data-URL (im Worker gibt es kein URL.createObjectURL) ---
function bytesToDataUrl(bytes) {
	let binary = "";
	const step = 0x8000; // in Bloecken, sonst sprengt String.fromCharCode den Stack
	for (let i = 0; i < bytes.length; i += step) {
		binary += String.fromCharCode.apply(null, bytes.subarray(i, i + step));
	}
	return "data:audio/mpeg;base64," + btoa(binary);
}

// ----- Fehlertexte in verstaendliches Deutsch uebersetzen ------------------
function humanError(e) {
	const m = String((e && e.message) || e || "Unbekannter Fehler");
	if (/Failed to fetch|NetworkError|net::|ERR_|Kein Internet/.test(m))
		return "Kein Internet erreichbar.";
	return m;
}

// Tastaturkuerzel: markierten Text vorlesen
if (chrome.commands && chrome.commands.onCommand) {
	chrome.commands.onCommand.addListener((command) => {
		if (command !== "speak-selection") return;
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
	});
}

export { ensureOffscreen, notifyTab, humanError };
