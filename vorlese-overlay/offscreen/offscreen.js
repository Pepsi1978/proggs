/*
 * offscreen.js (ES-Modul) — Audio-Wiedergabe UND Edge-WebSocket-Synthese.
 *
 * WICHTIG (Chromium-Bug 1285664): declarativeNetRequest-Regeln — die den von
 * Microsoft verlangten Edge-User-Agent setzen — werden NICHT auf WebSocket-
 * Upgrades angewendet, die aus einem Service-Worker stammen. Aus einem normalen
 * Dokument (wie diesem Offscreen-Dokument) greifen sie aber. Deshalb laeuft die
 * Edge-Synthese (WebSocket) HIER, nicht im Service-Worker.
 *
 * Google-Synthese (reines fetch) bleibt im Service-Worker; der schickt das
 * fertige Audio per ENQUEUE hierher.
 *
 * ----- Wiedergabe ueber die Web Audio API (NICHT ueber <audio>) -------------
 * Die Wiedergabe laeuft bewusst ueber die Web Audio API (AudioContext +
 * AudioBufferSourceNode) statt ueber ein HTMLAudioElement (new Audio()).
 *
 * Grund: Chromes Live-Untertitel (Live Caption / SODA) haengt seine
 * Spracherkennung im Chromium-Quellcode ausschliesslich in den Media-Pipeline-
 * Pfad (AudioRendererImpl) ein — und der bedient nur HTMLMediaElemente
 * (<audio>/<video>/new Audio()). Audio aus der Web Audio API laeuft durch einen
 * anderen Renderer-Pfad OHNE diesen Hook. Dadurch transkribiert Live Caption
 * unser Vorlese-Audio nicht mehr und es erscheinen keine unerwuenschten
 * Untertitel — ohne dass der Nutzer Live Caption global abschalten muss.
 *
 * Sicherheitsnetz (Funktionalitaet bleibt IMMER erhalten): Schlaegt die
 * Web-Audio-Wiedergabe aus irgendeinem Grund fehl (AudioContext nicht
 * verfuegbar, suspendiert, decode-Fehler), schaltet die Wiedergabe automatisch
 * und dauerhaft auf das HTMLAudioElement zurueck. Dann spielt das Audio in
 * jedem Fall — schlimmstenfalls eben wieder mit Untertiteln.
 *
 * Nachrichten vom Service-Worker (target: "offscreen"):
 *   ENQUEUE  { url }                  — fertiges Audio (Google) abspielen
 *   EDGE_SPEAK { text, voice, rate }  — Edge hier synthetisieren + abspielen
 *   STOP                              — Wiedergabe + laufende Synthese abbrechen
 * Rueckmeldungen an den Worker (target: "background"):
 *   OFFSCREEN_STARTED | OFFSCREEN_ENDED | OFFSCREEN_ERROR
 */
import * as edge from "../engines/edge-tts.js";
import { splitIntoChunks } from "../engines/chunker.js";
import { diag } from "../diag/diag.js";

// Diagnose-Schicht initialisieren (No-Op solange der Diagnose-Modus aus ist).
diag.init("offscreen");

// Warteschlange. Jedes Item ist entweder { buffer: ArrayBuffer } (Edge liefert
// rohe MP3-Bytes) oder { url: string } (Google schickt eine Data-URL).
const queue = [];
let currentSource = null; // laufender AudioBufferSourceNode (Web Audio)
let currentAudio = null; // laufendes HTMLAudioElement (Fallback)
let playing = false;
let gen = 0; // Abbruch-Zaehler: STOP und jede neue EDGE_SPEAK erhoehen ihn

let audioCtx = null;
let webAudioBroken = false; // true, sobald Web Audio einmal scheitert -> Fallback

function send(payload) {
	try {
		chrome.runtime.sendMessage(
			Object.assign({ target: "background" }, payload),
			() => void chrome.runtime.lastError,
		);
	} catch (e) {
		/* Worker evtl. inaktiv */
	}
}

// ----- AudioContext (lazy, mit resume gegen evtl. Suspend) ------------------
async function getCtx() {
	if (!audioCtx) {
		const AC = self.AudioContext || self.webkitAudioContext;
		audioCtx = new AC();
	}
	if (audioCtx.state === "suspended") {
		try {
			await audioCtx.resume();
		} catch (e) {
			/* unten faengt die state-Pruefung das ab */
		}
	}
	return audioCtx;
}

// ----- Item -> ArrayBuffer (fuer Web Audio) ---------------------------------
async function itemToArrayBuffer(item) {
	if (item.buffer) return item.buffer; // Edge: schon rohe Bytes
	const resp = await fetch(item.url); // Google: Data-URL -> ArrayBuffer
	return await resp.arrayBuffer();
}

// ----- Item -> Blob-/Data-URL (fuer das HTMLAudioElement-Fallback) ----------
function itemToBlobUrl(item) {
	if (item.url) return item.url; // schon eine (Data-)URL
	return URL.createObjectURL(new Blob([item.buffer], { type: "audio/mpeg" }));
}

function enqueueItem(item) {
	queue.push(item);
	if (!playing) {
		send({ type: "OFFSCREEN_STARTED" });
		playNext();
	}
}

async function playNext() {
	if (queue.length === 0) {
		playing = false;
		currentSource = null;
		currentAudio = null;
		send({ type: "OFFSCREEN_ENDED" });
		return;
	}
	playing = true;
	const item = queue.shift();
	const myGen = gen;

	// ----- Bevorzugt: Web Audio API (umgeht Chromes Live-Untertitel) --------
	if (!webAudioBroken) {
		try {
			const ctx = await getCtx();
			if (myGen !== gen) return; // zwischenzeitlich gestoppt/ersetzt
			if (ctx.state !== "running") throw new Error("AudioContext nicht aktiv");

			const ab = await itemToArrayBuffer(item);
			if (myGen !== gen) return;
			// decodeAudioData "detached" den Buffer -> mit einer Kopie arbeiten,
			// damit das Original fuer ein evtl. Fallback intakt bleibt.
			const audioBuf = await ctx.decodeAudioData(ab.slice(0));
			if (myGen !== gen) return;

			const src = ctx.createBufferSource();
			src.buffer = audioBuf;
			src.connect(ctx.destination);
			currentSource = src;
			src.onended = () => {
				if (currentSource === src) {
					currentSource = null;
					playNext();
				}
			};
			src.start();
			return;
		} catch (e) {
			// Web Audio nicht moeglich -> fuer den Rest der Session auf das
			// HTMLAudioElement zurueckfallen (Audio bleibt funktionsfaehig).
			if (myGen !== gen) return;
			diag.log("WARN", "PERFORMANCE", "offscreen.webaudio_fallback", {
				message: String((e && e.message) || e),
			});
			webAudioBroken = true;
			currentSource = null;
		}
	}

	// ----- Fallback: HTMLAudioElement (kann von Live Caption erfasst werden) -
	let url;
	try {
		url = itemToBlobUrl(item);
	} catch (e) {
		send({
			type: "OFFSCREEN_ERROR",
			message: "Audio konnte nicht vorbereitet werden.",
		});
		playNext();
		return;
	}
	const audio = new Audio(url);
	currentAudio = audio;

	const cleanup = () => {
		if (url.startsWith("blob:")) {
			try {
				URL.revokeObjectURL(url);
			} catch (e) {
				/* egal */
			}
		}
	};

	audio.onended = () => {
		cleanup();
		if (currentAudio === audio) {
			currentAudio = null;
			playNext();
		}
	};
	audio.onerror = () => {
		cleanup();
		if (currentAudio === audio) {
			currentAudio = null;
			send({
				type: "OFFSCREEN_ERROR",
				message: "Audio konnte nicht abgespielt werden.",
			});
			playNext();
		}
	};
	audio.play().catch((e) => {
		cleanup();
		send({
			type: "OFFSCREEN_ERROR",
			message: String(e && e.message ? e.message : e),
		});
		if (currentAudio === audio) {
			currentAudio = null;
			playNext();
		}
	});
}

function stopAll() {
	queue.length = 0;
	if (currentSource) {
		try {
			currentSource.onended = null;
			currentSource.stop();
		} catch (e) {
			/* egal */
		}
	}
	currentSource = null;
	if (currentAudio) {
		try {
			currentAudio.pause();
			currentAudio.src = "";
		} catch (e) {
			/* egal */
		}
	}
	currentAudio = null;
	playing = false;
}

// ----- Edge-Synthese (WebSocket HIER -> DNR-User-Agent-Regel greift) --------
async function edgeSpeak(text, voice, rate, myGen) {
	diag.log("INFO", "FUNKTION", "offscreen.edgeSpeak:eintritt", {
		voice,
		rate,
		textLen: (text || "").length,
	});
	const chunks = splitIntoChunks(text, 1200);
	let any = false;
	for (const part of chunks) {
		let bytes;
		try {
			bytes = await edge.synthesize(part, voice, rate);
		} catch (e) {
			// Einmal mit frischem Token erneut versuchen.
			diag.log("WARN", "FEHLER", "offscreen.edgeSpeak:retry", {
				message: String((e && e.message) || e),
			});
			try {
				bytes = await edge.synthesize(part, voice, rate);
			} catch (e2) {
				diag.log("ERROR", "FEHLER", "offscreen.edgeSpeak:fehlgeschlagen", {
					message: String((e2 && e2.message) || e2),
				});
				if (myGen === gen)
					send({ type: "OFFSCREEN_ERROR", message: humanError(e2) });
				return;
			}
		}
		if (myGen !== gen) return; // zwischenzeitlich gestoppt/ersetzt
		if (bytes && bytes.length > 0) {
			// Rohe Bytes als ArrayBuffer-Kopie in die Queue (fuer Web Audio).
			enqueueItem({ buffer: bytes.slice().buffer });
			any = true;
		}
	}
	if (!any && myGen === gen)
		send({
			type: "OFFSCREEN_ERROR",
			message: "Keine Audiodaten von Edge erhalten.",
		});
}

function humanError(e) {
	const m = String((e && e.message) || e || "Unbekannter Fehler");
	if (/Failed to fetch|NetworkError|net::|ERR_|Kein Internet/.test(m))
		return "Kein Internet erreichbar.";
	return m;
}

chrome.runtime.onMessage.addListener((msg) => {
	if (!msg || msg.target !== "offscreen") return;
	switch (msg.type) {
		case "ENQUEUE":
			enqueueItem({ url: msg.url });
			break;
		case "STOP":
			gen++;
			stopAll();
			break;
		case "EDGE_SPEAK": {
			gen++;
			const myGen = gen;
			stopAll();
			edgeSpeak(msg.text, msg.voice, msg.rate, myGen);
			break;
		}
	}
});
