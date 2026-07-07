/*
 * edge-tts.js — Engine 1: Microsoft Edge TTS (kostenlos, kein Schluessel).
 *
 * Die gesamte Edge-Logik ist absichtlich in dieses eine Modul gekapselt, damit
 * sie sich leicht reparieren laesst, falls Microsoft das Verfahren aendert
 * (insbesondere das Sec-MS-GEC-Sicherheits-Token).
 *
 * Einheitliche Engine-Schnittstelle (auch in google-tts.js):
 *   listGermanVoices(apiKey?) -> Promise<Array<{ id, label }>>
 *   synthesize(text, voice, rate, apiKey?) -> Promise<Uint8Array>  (MP3-Bytes)
 *
 * Die Werte (Token-Algorithmus, TRUSTED_CLIENT_TOKEN, Frame-Parsing, turn.end)
 * sind aus dem erprobten Best-Journal-Player (EdgeTtsPlayer.kt) uebernommen.
 *
 * Wichtig (Browser-Eigenheit): Ein WebSocket im Browser/Service-Worker erlaubt
 * KEINE eigenen Header (User-Agent, Origin, Cookie). Anders als der OkHttp-Code
 * in Best Journal tragen hier allein die URL-Query-Parameter (vor allem das
 * Sec-MS-GEC-Token). Der Consumer-Endpunkt akzeptiert das.
 */

// Oeffentlicher, fest in Microsoft Edge eingebauter Client-Token — KEIN Geheimnis;
// steht so in jeder Edge-TTS-Implementierung und auch im BestJournal-Repo.
import { diag } from "../diag/diag.js";

const TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"; // gitleaks:allow
// Der Edge-Endpunkt akzeptiert den Handshake NUR mit einem User-Agent, der "Edg/"
// enthaelt (per Kommandozeilen-Test verifiziert: UA allein reicht, Origin/Cookie/
// Version sind egal). Die WebSocket-API im Browser kann den User-Agent nicht setzen
// — das erledigt eine declarativeNetRequest-Regel (rules/edge-ua.json), die ihn auf
// Edg/143 setzt. Diese Version hier muss dazu passen (1-143...).
const CHROMIUM_FULL_VERSION = "143.0.3650.75";
const WIN_EPOCH = 11644473600n; // Sekunden zwischen 1601-01-01 und 1970-01-01

const WSS_BASE =
	"wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1";
const VOICES_URL =
	"https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list";

// MP3, 24 kHz — guter Kompromiss aus Qualitaet und Datenmenge fuer Sprache.
const OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3";

// Watchdog: abbrechen, wenn keine Audio-Bytes ankommen. Der Timer wird bei
// jedem Audio-Stueck zurueckgesetzt, lange Sprache (40s+) ist also NICHT
// betroffen — nur echte Stille loest den Abbruch aus.
const WATCHDOG_INITIAL_MS = 30000; // von onopen bis zum ersten Audio-Byte
const WATCHDOG_IDLE_MS = 30000; // Luecke zwischen zwei Audio-Stuecken

// Bewaehrte deutsche Edge-Stimmen (aus Best Journal) — dienen als Fallback,
// falls der dynamische Stimmen-Endpunkt nicht erreichbar ist.
const FALLBACK_EDGE_VOICES = [
	{
		id: "de-DE-SeraphinaMultilingualNeural",
		label: "★ Seraphina — weiblich (mehrsprachig)",
	},
	{
		id: "de-DE-FlorianMultilingualNeural",
		label: "★ Florian — männlich (mehrsprachig)",
	},
	{ id: "de-DE-KatjaNeural", label: "Katja — weiblich, warm" },
	{ id: "de-DE-KillianNeural", label: "Killian — männlich, warm" },
	{ id: "de-DE-ConradNeural", label: "Conrad — männlich, klar" },
	{ id: "de-DE-AmalaNeural", label: "Amala — weiblich, jung" },
];

// ----- Sec-MS-GEC-Token (SHA-256 ueber gerundete Windows-Ticks) -------------
async function generateSecMsGec() {
	const nowSec = BigInt(Math.floor(Date.now() / 1000));
	let ticks = nowSec + WIN_EPOCH;
	ticks -= ticks % 300n; // auf die naechsten 300 Sekunden abrunden
	ticks *= 10000000n; // in 100-Nanosekunden-Einheiten umrechnen
	const strToHash = ticks.toString() + TRUSTED_CLIENT_TOKEN;
	const digest = await crypto.subtle.digest(
		"SHA-256",
		new TextEncoder().encode(strToHash),
	);
	return [...new Uint8Array(digest)]
		.map((b) => b.toString(16).padStart(2, "0"))
		.join("")
		.toUpperCase();
}

function uuidHex() {
	return crypto.randomUUID().replace(/-/g, "");
}

// Slider 0.5–2.0 -> Edge-Prozent: 1.0 -> "+0%", 1.5 -> "+50%", 0.8 -> "-20%"
function ratePercent(rate) {
	const r = typeof rate === "number" && isFinite(rate) ? rate : 1.0;
	const pct = Math.round((r - 1) * 100);
	return (pct >= 0 ? "+" : "") + pct + "%";
}

function xmlEscape(s) {
	return String(s)
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;");
}

function concatChunks(chunks) {
	let total = 0;
	for (const c of chunks) total += c.length;
	const out = new Uint8Array(total);
	let off = 0;
	for (const c of chunks) {
		out.set(c, off);
		off += c.length;
	}
	return out;
}

// ----- Stimmenliste fuer das Dropdown --------------------------------------
export async function listGermanVoices(/* apiKey */) {
	try {
		const token = await generateSecMsGec();
		const url =
			VOICES_URL +
			"?trustedclienttoken=" +
			TRUSTED_CLIENT_TOKEN +
			"&Sec-MS-GEC=" +
			token +
			"&Sec-MS-GEC-Version=1-" +
			CHROMIUM_FULL_VERSION;
		const res = await fetch(url);
		if (res.ok) {
			const all = await res.json();
			if (Array.isArray(all)) {
				const mapped = all
					.filter((v) => speaksGerman(v))
					.map((v) => ({
						id: v.ShortName,
						label: friendlyLabel(v),
						_primary: v.Locale === "de-DE",
					}));
				if (mapped.length) {
					// de-DE-Stimmen zuerst, dann die mehrsprachigen
					mapped.sort((a, b) => {
						if (a._primary !== b._primary) return a._primary ? -1 : 1;
						return a.label.localeCompare(b.label, "de");
					});
					return mapped.map(({ id, label }) => ({ id, label }));
				}
			}
		}
	} catch (e) {
		diag.log("WARN", "FEHLER", "edge.listGermanVoices:fehler", {
			message: String((e && e.message) || e),
		});
		/* Fallback unten */
	}
	diag.log("WARN", "ZUSTAND", "edge.listGermanVoices:fallback_liste", {
		anzahl: FALLBACK_EDGE_VOICES.length,
	});
	return FALLBACK_EDGE_VOICES.slice();
}

function speaksGerman(v) {
	if (!v || !v.ShortName) return false;
	if (v.Locale === "de-DE") return true;
	const sec = v.SecondaryLocaleList;
	return Array.isArray(sec) && sec.includes("de-DE");
}

function friendlyLabel(v) {
	// Aus "Microsoft Katja Online (Natural) - German (Germany)" "Katja" machen.
	let name = v.FriendlyName || v.ShortName;
	name = name
		.replace(/^Microsoft\s+/i, "")
		.replace(/\s+Online\s*\(Natural\)/i, "")
		.replace(/\s*-\s*.*$/, "")
		.trim();
	const tag = v.Locale === "de-DE" ? "" : " (mehrsprachig)";
	const gender =
		v.Gender === "Female" ? "weiblich" : v.Gender === "Male" ? "männlich" : "";
	const suffix = gender ? ` — ${gender}` : "";
	return `${name}${suffix}${tag}`;
}

// ----- Synthese: ein Textstueck -> MP3-Bytes -------------------------------
export async function synthesize(text, voice, rate /* , apiKey */) {
	const clean = (text || "").trim();
	if (!clean) return new Uint8Array(0);

	const v = voice || "de-DE-KatjaNeural";
	const t0 = performance.now();
	diag.log("INFO", "FUNKTION", "edge.synthesize:eintritt", {
		voice: v,
		rate,
		textLen: clean.length,
	});
	const token = await generateSecMsGec();
	const url =
		WSS_BASE +
		"?TrustedClientToken=" +
		TRUSTED_CLIENT_TOKEN +
		"&Sec-MS-GEC=" +
		token +
		"&Sec-MS-GEC-Version=1-" +
		CHROMIUM_FULL_VERSION;
	// Kein ConnectionId-Query-Param — die funktionierende Browser-Referenz (travisvn)
	// haengt ihn nicht an; weniger Variablen im Handshake.

	return await new Promise((resolve, reject) => {
		let ws;
		try {
			ws = new WebSocket(url);
		} catch (e) {
			reject(new Error("Edge-Verbindung konnte nicht aufgebaut werden."));
			return;
		}
		ws.binaryType = "arraybuffer";

		const chunks = [];
		let settled = false;
		let watchdog = null;

		function finish(fn, arg) {
			if (settled) return;
			settled = true;
			if (watchdog) clearTimeout(watchdog);
			try {
				ws.close();
			} catch (e) {
				/* egal */
			}
			fn(arg);
		}

		function resetWatchdog(ms) {
			if (watchdog) clearTimeout(watchdog);
			watchdog = setTimeout(() => {
				finish(
					reject,
					new Error(
						"Edge-Dienst hat nicht geantwortet — bitte erneut versuchen.",
					),
				);
			}, ms);
		}

		ws.onopen = () => {
			resetWatchdog(WATCHDOG_INITIAL_MS);
			const config =
				"Content-Type:application/json; charset=utf-8\r\n" +
				"Path:speech.config\r\n\r\n" +
				'{"context":{"synthesis":{"audio":{"metadataOptions":' +
				'{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},' +
				'"outputFormat":"' +
				OUTPUT_FORMAT +
				'"}}}}';
			ws.send(config);

			const ssml =
				"X-RequestId:" +
				uuidHex() +
				"\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n" +
				"<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
				"<voice name='" +
				v +
				"'><prosody rate='" +
				ratePercent(rate) +
				"' pitch='+0Hz'>" +
				xmlEscape(clean) +
				"</prosody></voice></speak>";
			ws.send(ssml);
		};

		ws.onmessage = (ev) => {
			if (typeof ev.data === "string") {
				// Text-Frame: turn.start | audio.metadata | turn.end
				if (ev.data.includes("Path:turn.end")) {
					const out = concatChunks(chunks);
					diag.log("INFO", "PERFORMANCE", "edge.synthesize:erfolg", {
						bytes: out.length,
						frames: chunks.length,
						dauer_ms: Math.round(performance.now() - t0),
					});
					finish(resolve, out);
				}
				return;
			}
			// Binaer-Frame: 2 Byte Kopflaenge (BE) + Kopf + MP3-Bytes
			const buf = ev.data;
			if (buf && buf.byteLength > 2) {
				const view = new DataView(buf);
				const headerLen = view.getUint16(0); // Big-Endian (Standard)
				const audioStart = 2 + headerLen;
				if (buf.byteLength > audioStart) {
					chunks.push(new Uint8Array(buf.slice(audioStart)));
					resetWatchdog(WATCHDOG_IDLE_MS);
				}
			}
		};

		ws.onerror = () => {
			diag.log("ERROR", "FEHLER", "edge.synthesize:ws_error", {
				voice: v,
				dauer_ms: Math.round(performance.now() - t0),
			});
			finish(reject, new Error("Edge-Verbindung fehlgeschlagen."));
		};

		ws.onclose = () => {
			if (settled) return;
			// Ohne turn.end geschlossen: wenn schon Audio kam, das nehmen.
			if (chunks.length > 0) {
				diag.log("WARN", "FEHLER", "edge.synthesize:close_ohne_turnend", {
					frames: chunks.length,
					dauer_ms: Math.round(performance.now() - t0),
				});
				finish(resolve, concatChunks(chunks));
			} else {
				diag.log("ERROR", "FEHLER", "edge.synthesize:close_ohne_audio", {
					dauer_ms: Math.round(performance.now() - t0),
				});
				finish(reject, new Error("Edge-Verbindung wurde unerwartet getrennt."));
			}
		};
	});
}
