/*
 * service-worker.js — der Hintergrund-Dienst (ES-Modul).
 *
 * Hier laufen ALLE Netzwerk-Aufrufe (Edge-WebSocket, Google-REST). Der Worker
 * laeuft im Ursprung der Erweiterung und umgeht damit die CSP der jeweiligen
 * Webseite (die Hosts stehen in host_permissions).
 *
 * Der Worker kann selbst kein Audio abspielen — dafuer erzeugt er ein
 * verstecktes Offscreen-Dokument (chrome.offscreen) und schickt ihm die
 * fertigen Audiodaten.
 *
 * STUFE 1: Nachrichten-Router + Offscreen-Helfer stehen. Die eigentliche
 * Synthese/Wiedergabe wird in Stufe 2 (Edge) und Stufe 3 (Google) angebunden.
 */

import * as edge from "../engines/edge-tts.js";
import * as google from "../engines/google-tts.js";

const MSG = {
	SPEAK: "TTS_SPEAK",
	STOP: "TTS_STOP",
	GET_VOICES: "GET_VOICES",
	TEST: "TTS_TEST",
	STATE: "TTS_STATE",
};

const OFFSCREEN_PATH = "offscreen/offscreen.html";
let creatingOffscreen = null;

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

// ----- Nachrichten-Router ---------------------------------------------------
chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
	if (!msg || typeof msg !== "object") return false;
	if (msg.target === "offscreen") return false; // Antworten des Offscreen-Docs ignorieren

	const tabId = sender.tab ? sender.tab.id : undefined;

	switch (msg.type) {
		case MSG.GET_VOICES:
			handleGetVoices(msg)
				.then((voices) => sendResponse({ voices }))
				.catch((e) => sendResponse({ error: humanError(e) }));
			return true; // asynchrone Antwort

		case MSG.SPEAK:
		case MSG.TEST:
			// STUFE 1: Synthese noch nicht angebunden.
			notifyTab(tabId, {
				type: MSG.STATE,
				state: "error",
				message:
					"Vorlesen wird in Stufe 2 (Edge) bzw. Stufe 3 (Google) angebunden.",
			});
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

function stopPlayback() {
	// Offscreen anweisen, Wiedergabe + Queue zu leeren.
	try {
		chrome.runtime.sendMessage(
			{ target: "offscreen", type: "STOP" },
			() => void chrome.runtime.lastError,
		);
	} catch (e) {
		/* Offscreen evtl. nicht aktiv */
	}
}

// ----- Fehlertexte in verstaendliches Deutsch uebersetzen ------------------
function humanError(e) {
	const m = String((e && e.message) || e || "Unbekannter Fehler");
	if (/Failed to fetch|NetworkError|net::|ERR_/.test(m))
		return "Kein Internet erreichbar.";
	return m;
}

// Tastaturkuerzel (Anbindung in Stufe 4)
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

// Exporte fuer spaetere Stufen (ensureOffscreen wird in Stufe 2 genutzt)
export { ensureOffscreen, notifyTab, humanError };
