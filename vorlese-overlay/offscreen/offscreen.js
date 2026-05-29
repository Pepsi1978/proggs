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
 * Nachrichten vom Service-Worker (target: "offscreen"):
 *   ENQUEUE  { url }                  — fertiges Audio (Google) abspielen
 *   EDGE_SPEAK { text, voice, rate }  — Edge hier synthetisieren + abspielen
 *   STOP                              — Wiedergabe + laufende Synthese abbrechen
 * Rueckmeldungen an den Worker (target: "background"):
 *   OFFSCREEN_STARTED | OFFSCREEN_ENDED | OFFSCREEN_ERROR
 */
import * as edge from "../engines/edge-tts.js";
import { splitIntoChunks } from "../engines/chunker.js";

const queue = [];
let current = null;
let playing = false;
let gen = 0; // Abbruch-Zaehler: STOP und jede neue EDGE_SPEAK erhoehen ihn

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

function bytesToBlobUrl(bytes) {
	return URL.createObjectURL(new Blob([bytes], { type: "audio/mpeg" }));
}

function enqueue(url) {
	queue.push(url);
	if (!playing) {
		send({ type: "OFFSCREEN_STARTED" });
		playNext();
	}
}

function playNext() {
	if (queue.length === 0) {
		playing = false;
		current = null;
		send({ type: "OFFSCREEN_ENDED" });
		return;
	}
	playing = true;
	const url = queue.shift();
	const audio = new Audio(url);
	current = audio;

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
		if (current === audio) playNext();
	};
	audio.onerror = () => {
		cleanup();
		if (current === audio) {
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
		if (current === audio) playNext();
	});
}

function stopAll() {
	queue.length = 0;
	if (current) {
		try {
			current.pause();
			current.src = "";
		} catch (e) {
			/* egal */
		}
	}
	current = null;
	playing = false;
}

// ----- Edge-Synthese (WebSocket HIER -> DNR-User-Agent-Regel greift) --------
async function edgeSpeak(text, voice, rate, myGen) {
	const chunks = splitIntoChunks(text, 1200);
	let any = false;
	for (const part of chunks) {
		let bytes;
		try {
			bytes = await edge.synthesize(part, voice, rate);
		} catch (e) {
			// Einmal mit frischem Token erneut versuchen.
			try {
				bytes = await edge.synthesize(part, voice, rate);
			} catch (e2) {
				if (myGen === gen)
					send({ type: "OFFSCREEN_ERROR", message: humanError(e2) });
				return;
			}
		}
		if (myGen !== gen) return; // zwischenzeitlich gestoppt/ersetzt
		if (bytes && bytes.length > 0) {
			enqueue(bytesToBlobUrl(bytes));
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
			enqueue(msg.url);
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
