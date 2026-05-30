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
 * fertige Audio per ENQUEUE hierher und meldet mit ENQUEUE_DONE, dass alle
 * Stuecke geliefert sind.
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
 * ----- Eindeutiges Wiedergabe-Ende (kein haengendes Stop-Icon) --------------
 * Eine "Session" laeuft von OFFSCREEN_STARTED bis OFFSCREEN_ENDED. OFFSCREEN_ENDED
 * wird NUR gesendet, wenn der Produzent fertig ist (producerDone) UND die Queue
 * leer ist UND nichts mehr spielt. Sonst wuerde bei langen Texten ein kurzzeitig
 * leerer Queue-Zwischenstand ein verfruehtes Ende ausloesen (und durch die
 * asynchrone Nachrichten-Reihenfolge koennte der Status auf "spielt" haengen
 * bleiben -> rotes Stop-Icon trotz fertigem Vorlesen).
 *
 * Nachrichten vom Service-Worker (target: "offscreen"):
 *   ENQUEUE      { url }                        — fertiges Audio (Google) abspielen
 *   ENQUEUE_DONE                                — alle Google-Stuecke geliefert
 *   EDGE_SPEAK   { text, voice, rate, pitch? }  — Edge hier synthetisieren + abspielen
 *   STOP                                        — Wiedergabe + laufende Synthese abbrechen
 *   PAUSE                                       — laufendes Audio pausieren, Queue behalten
 *   RESUME                                      — Wiedergabe an gleicher Stelle fortsetzen
 * Rueckmeldungen an den Worker (target: "background"):
 *   OFFSCREEN_STARTED | OFFSCREEN_ENDED | OFFSCREEN_ERROR
 *   OFFSCREEN_PAUSED  — Pause wurde tatsaechlich ausgefuehrt
 *   OFFSCREEN_RESUMED — Wiedergabe wurde fortgesetzt
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
let sessionActive = false; // true von OFFSCREEN_STARTED bis OFFSCREEN_ENDED
let producerDone = false; // true sobald alle Stuecke geliefert wurden
// playing wird SYNCHRON gesetzt, sobald playNext() ein Item aus der Queue nimmt —
// noch BEVOR (nach einem await) currentSource gesetzt ist. Das schliesst die
// asynchrone Luecke: ohne dieses Flag koennte finishIfDone() (aufgerufen direkt
// nach dem Einreihen) ein verfruehtes OFFSCREEN_ENDED senden, weil currentSource
// in dem Moment noch null ist -> die naechste Passage wuerde die gerade
// startende abwuergen und es waere NICHTS hoerbar.
let playing = false;
let gen = 0; // Abbruch-Zähler: STOP und jede neue EDGE_SPEAK erhöhen ihn

// Pause-Zustand. paused=true hält playNext() an, lässt die Queue intakt.
let paused = false;

// Sample-genaues Pause/Resume fuer den Web-Audio-Pfad: Ein AudioBufferSourceNode
// kann nicht "pausiert" werden — wir merken uns daher den aktuell spielenden
// Buffer plus die bereits gespielte Zeit und starten beim Fortsetzen denselben
// Buffer ab genau diesem Offset neu (src.start(0, offset)). Dadurch liest die
// Wiedergabe exakt an der Stelle weiter, an der pausiert wurde.
let currentBuffer = null; // aktuell spielender AudioBuffer (fuer Resume)
let playStartTime = 0; // ctx.currentTime beim letzten Start des Buffers
let playOffset = 0; // bereits gespielte Sekunden im aktuellen Buffer

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

// true, wenn gerade NICHTS laeuft und auch kein Item in Bearbeitung ist.
// Beruecksichtigt playing (synchron), damit der async-Gap nicht zu einem
// verfruehten Ende fuehrt.
function isBusy() {
	return playing || !!currentSource || !!currentAudio;
}

function enqueueItem(item) {
	queue.push(item);
	if (paused) return; // waehrend Pause nur sammeln, nicht starten
	if (!sessionActive) {
		// Neue Session beginnt -> genau EIN OFFSCREEN_STARTED.
		sessionActive = true;
		send({ type: "OFFSCREEN_STARTED" });
		playNext();
	} else if (!isBusy()) {
		// Session laeuft, aber es spielt gerade nichts (wartete auf Nachschub).
		playNext();
	}
}

// Sendet OFFSCREEN_ENDED nur, wenn wirklich alles fertig ist. Verhindert das
// verfruehte Ende (kein Ton bei mehreren Stuecken) UND das haengende Stop-Icon.
function finishIfDone() {
	if (producerDone && queue.length === 0 && !isBusy() && !paused) {
		sessionActive = false;
		producerDone = false;
		currentBuffer = null;
		playOffset = 0;
		send({ type: "OFFSCREEN_ENDED" });
	}
}

// Startet den aktuell gemerkten Buffer (currentBuffer) ab "offset" Sekunden.
// Wird sowohl fuer den normalen Start (offset 0) als auch fuer Resume genutzt.
function startWebAudioBuffer(offset) {
	const ctx = audioCtx;
	if (!ctx || !currentBuffer) return;
	const src = ctx.createBufferSource();
	src.buffer = currentBuffer;
	src.connect(ctx.destination);
	currentSource = src;
	playStartTime = ctx.currentTime;
	playOffset = offset;
	src.onended = () => {
		if (currentSource === src) {
			currentSource = null;
			currentBuffer = null;
			playOffset = 0;
			playNext();
		}
	};
	try {
		src.start(0, Math.max(0, offset));
	} catch (e) {
		// Falls der Offset (z.B. minimal ueber Bufferlaenge) abgelehnt wird:
		// von vorne starten statt die Wiedergabe zu verlieren.
		try {
			src.start();
		} catch (e2) {
			/* egal — onended/STOP raeumt auf */
		}
	}
}

async function playNext() {
	// Pause-Guard: wenn pausiert, Queue intakt lassen und warten.
	if (paused) return;

	if (queue.length === 0) {
		// Nichts mehr in der Queue. Nur beenden, wenn der Produzent fertig ist —
		// sonst auf weitere Stuecke warten (Session bleibt aktiv).
		playing = false;
		finishIfDone();
		return;
	}
	// SYNCHRON markieren, dass ein Item in Bearbeitung ist — vor jedem await.
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

			// Buffer merken und ab 0 starten — bei Pause/Resume wird derselbe
			// Buffer ueber startWebAudioBuffer(offset) sample-genau fortgesetzt.
			currentBuffer = audioBuf;
			playOffset = 0;
			startWebAudioBuffer(0);
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
			currentBuffer = null;
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
	currentBuffer = null;
	playOffset = 0;
	playStartTime = 0;
	if (currentAudio) {
		try {
			currentAudio.pause();
			currentAudio.src = "";
		} catch (e) {
			/* egal */
		}
	}
	currentAudio = null;
	sessionActive = false;
	producerDone = false;
	playing = false;
}

// ----- Edge-Synthese (WebSocket HIER -> DNR-User-Agent-Regel greift) --------
// pitch: Tonhöhe in Halbtönen (-50 bis +50), Default 0.
async function edgeSpeak(text, voice, rate, myGen, pitch) {
	const pitchVal = typeof pitch === "number" ? pitch : 0;
	diag.log("INFO", "FUNKTION", "offscreen.edgeSpeak:eintritt", {
		voice,
		rate,
		pitch: pitchVal,
		textLen: (text || "").length,
	});
	const chunks = splitIntoChunks(text, 1200);
	let any = false;
	for (const part of chunks) {
		let bytes;
		try {
			bytes = await edge.synthesize(part, voice, rate, pitchVal);
		} catch (e) {
			// Einmal mit frischem Token erneut versuchen.
			diag.log("WARN", "FEHLER", "offscreen.edgeSpeak:retry", {
				message: String((e && e.message) || e),
			});
			try {
				bytes = await edge.synthesize(part, voice, rate, pitchVal);
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
	if (myGen !== gen) return;
	if (!any) {
		send({
			type: "OFFSCREEN_ERROR",
			message: "Keine Audiodaten von Edge erhalten.",
		});
		return;
	}
	// Alle Stuecke synthetisiert und eingereiht -> Ende anstossen, falls die
	// Wiedergabe bereits durch ist.
	producerDone = true;
	finishIfDone();
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
		case "ENQUEUE_DONE":
			// Google hat alle Stuecke geliefert -> Ende anstossen, falls fertig.
			producerDone = true;
			finishIfDone();
			break;
		case "STOP":
			gen++;
			paused = false; // Pause beim expliziten Stop zurücksetzen
			stopAll();
			break;
		case "EDGE_SPEAK": {
			gen++;
			const myGen = gen;
			paused = false; // neue Wiedergabe setzt vorherige Pause zurück
			stopAll();
			edgeSpeak(msg.text, msg.voice, msg.rate, myGen, msg.pitch ?? 0);
			break;
		}
		case "PAUSE":
			// Wiedergabe pausieren: Queue bleibt intakt, gen wird NICHT erhöht.
			if (!paused) {
				paused = true;
				// Web-Audio: bereits gespielte Zeit zum Offset addieren und die
				// laufende Source stoppen. currentBuffer bleibt erhalten, damit
				// RESUME exakt an dieser Stelle wieder aufsetzen kann.
				if (currentSource && audioCtx) {
					try {
						playOffset += Math.max(0, audioCtx.currentTime - playStartTime);
					} catch (e) {
						/* egal — schlimmstenfalls von vorne */
					}
					try {
						currentSource.onended = null;
						currentSource.stop();
					} catch (e) {
						/* egal */
					}
					currentSource = null;
				}
				// Fallback HTMLAudio: nativ pausieren (Element merkt sich die
				// Position selbst). currentAudio NICHT auf null setzen.
				if (currentAudio) {
					try {
						currentAudio.pause();
					} catch (e) {
						/* egal */
					}
				}
				send({ type: "OFFSCREEN_PAUSED" });
			}
			break;
		case "RESUME":
			// Wiedergabe fortsetzen — genau an der pausierten Stelle.
			if (paused) {
				paused = false;
				if (currentBuffer && audioCtx) {
					// 1) Web-Audio: aktuellen Buffer ab gemerktem Offset fortsetzen.
					send({ type: "OFFSCREEN_RESUMED" });
					const maxOff = Math.max(0, currentBuffer.duration - 0.05);
					startWebAudioBuffer(Math.min(playOffset, maxOff));
				} else if (currentAudio) {
					// 2) Fallback: HTMLAudio nativ ab der gemerkten Position fortsetzen.
					send({ type: "OFFSCREEN_RESUMED" });
					currentAudio.play().catch(() => {
						/* egal — onerror raeumt auf */
					});
				} else {
					// 3) Mitten zwischen Stuecken pausiert -> naechstes spielen oder
					//    sauber beenden, falls schon alles fertig ist.
					send({ type: "OFFSCREEN_RESUMED" });
					playNext();
				}
			}
			break;
	}
});
