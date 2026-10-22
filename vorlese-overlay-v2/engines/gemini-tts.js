/*
 * gemini-tts.js — Engine 3: Gemini 3.8 Flash TTS / Flash-Lite TTS (REST generateContent).
 *
 * Port aus Jarvis (Synthese.kt gemini()/geminiAnfrage(), TtsCatalog.kt):
 *   - dieselben 30 Stimmnamen wie Chirp 3 HD, Kennung "<provider>:<Name>"
 *     ("gemini_flash:Leda" / "gemini_lite:Leda") — so trennt die Kennung die Modelle
 *   - Schluesselwahl: "Erst kostenlos, dann bezahlt" (Standard), "Nur kostenlos", "Nur bezahlt"
 *   - scheitert der kostenlose Schluessel und es gibt einen Ausweichweg, bleibt er
 *     10 Minuten gesperrt (FREE_PAUSE_MS, gilt fuer alle Aufrufe im Worker)
 *   - Chirp-Rueckfall: statt bezahlt mit derselben Stimme ueber Google Chirp 3 HD
 *   - Gemini liefert rohes PCM (16 Bit mono) -> WAV-Kopf davor
 *   - "Audio zu kurz"-Pruefung (unter 25 Zeichen je Sekunde spricht niemand)
 *
 * Schnittstelle:
 *   listGermanVoices(model) -> Array<{ id, label }>   model = "flash" | "lite"
 *   synthesize(text, voiceId, rate, cfg, googleKey) -> Promise<{ bytes, mime }>
 */

import { diag } from "../diag/diag.js";
import * as google from "./google-tts.js";

const GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
export const MODELS = {
	flash: { provider: "gemini_flash", model: "gemini-3.8-flash-tts", label: "Gemini 3.8 Flash TTS" },
	lite: { provider: "gemini_lite", model: "gemini-3.8-flash-lite-tts", label: "Gemini 3.8 Flash-Lite TTS" },
};
export const KEY_FREE_THEN_PAID = "frei_dann_bezahlt";
export const KEY_ONLY_FREE = "nur_frei";
export const KEY_ONLY_PAID = "nur_bezahlt";
export const DEFAULT_GEMINI_VOICE = "gemini_flash:Leda";

const FREE_PAUSE_MS = 10 * 60_000;
const WAIT_MS = [1000, 3000, 8000];
let freeBlockedUntil = 0;

const NAMES = [
	["Achernar", "w"], ["Aoede", "w"], ["Autonoe", "w"], ["Callirrhoe", "w"], ["Despina", "w"],
	["Erinome", "w"], ["Gacrux", "w"], ["Kore", "w"], ["Laomedeia", "w"], ["Leda", "w"],
	["Pulcherrima", "w"], ["Sulafat", "w"], ["Vindemiatrix", "w"], ["Zephyr", "w"],
	["Achird", "m"], ["Algenib", "m"], ["Algieba", "m"], ["Alnilam", "m"], ["Charon", "m"],
	["Enceladus", "m"], ["Fenrir", "m"], ["Iapetus", "m"], ["Orus", "m"], ["Puck", "m"],
	["Rasalgethi", "m"], ["Sadachbia", "m"], ["Sadaltager", "m"], ["Schedar", "m"],
	["Umbriel", "m"], ["Zubenelgenubi", "m"],
];

/** Der Stimmname, den Gemini erwartet ("Leda"). */
export function geminiName(voiceId) {
	const s = String(voiceId || "");
	const i = s.indexOf(":");
	return i >= 0 ? s.slice(i + 1) : s || "Leda";
}

/** "flash" oder "lite" aus der Kennung. */
export function modelKeyOf(voiceId) {
	return String(voiceId || "").startsWith("gemini_lite:") ? "lite" : "flash";
}

/** Dieselbe Stimme bei Chirp 3 HD — fuer den Rueckfall. */
export function chirpFor(voiceId) {
	return "de-DE-Chirp3-HD-" + geminiName(voiceId);
}

export function listGermanVoices(model) {
	const m = MODELS[model === "lite" ? "lite" : "flash"];
	return NAMES.map(([n, g]) => ({
		id: m.provider + ":" + n,
		label: n + " — " + (g === "w" ? "weiblich" : "männlich"),
	}));
}

function base64ToBytes(b64) {
	const bin = atob(b64);
	const out = new Uint8Array(bin.length);
	for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
	return out;
}

/** Rohes PCM (16 Bit, mono) bekommt einen WAV-Kopf. */
function withWavHeader(pcm, rate) {
	const wav = new Uint8Array(44 + pcm.length);
	const dv = new DataView(wav.buffer);
	const ascii = (at, s) => { for (let i = 0; i < s.length; i++) wav[at + i] = s.charCodeAt(i); };
	ascii(0, "RIFF"); dv.setUint32(4, 36 + pcm.length, true); ascii(8, "WAVE");
	ascii(12, "fmt "); dv.setUint32(16, 16, true); dv.setUint16(20, 1, true); dv.setUint16(22, 1, true);
	dv.setUint32(24, rate, true); dv.setUint32(28, rate * 2, true); dv.setUint16(32, 2, true); dv.setUint16(34, 16, true);
	ascii(36, "data"); dv.setUint32(40, pcm.length, true);
	wav.set(pcm, 44);
	return wav;
}

function wavRate(wav) {
	const r = new DataView(wav.buffer, wav.byteOffset).getUint32(24, true);
	return r > 0 ? r : 24000;
}

/** Fehler mit HTTP-/Inhaltsbezug (nicht Netz) — nur diese loesen den Ausweichweg aus. */
class TtsError extends Error {}

async function postWithRetry(url, key, body, waitOn429) {
	let attempt = 0;
	for (;;) {
		let res;
		try {
			res = await fetch(url, {
				method: "POST",
				headers: { "Content-Type": "application/json; charset=utf-8", "x-goog-api-key": key },
				body,
			});
		} catch (e) {
			throw new Error("Kein Internet erreichbar.");
		}
		const text = await res.text();
		if (res.ok) return text;
		if (res.status === 429 && waitOn429 && attempt < WAIT_MS.length) {
			diag.log("WARN", "FEHLER", "gemini.rate_limit", { ms: WAIT_MS[attempt], versuch: attempt });
			await new Promise((r) => setTimeout(r, WAIT_MS[attempt]));
			attempt++;
			continue;
		}
		if (res.status === 401 || res.status === 403)
			throw new TtsError("Vorlesen fehlgeschlagen: Der Schlüssel wurde abgelehnt (Fehler " + res.status + "). Prüf ihn in den Einstellungen.");
		if (res.status === 429)
			throw new TtsError("Vorlesen fehlgeschlagen: Das Kontingent der Stimme ist erschöpft (Fehler 429).");
		throw new TtsError("Die Stimme antwortete mit Fehler " + res.status + ": " + text.slice(0, 200));
	}
}

async function geminiRequest(text, model, voiceId, key, kind, waitOn429) {
	const t0 = performance.now();
	// Gemini liest den Text woertlich vor; eine Anweisung im Text wuerde mitgesprochen.
	const body = JSON.stringify({
		contents: [{ parts: [{ text }] }],
		generationConfig: {
			responseModalities: ["AUDIO"],
			speechConfig: { voiceConfig: { prebuiltVoiceConfig: { voiceName: geminiName(voiceId) } } },
		},
	});
	const raw = await postWithRetry(GEMINI_URL + model + ":generateContent", key, body, waitOn429);
	let json;
	try { json = JSON.parse(raw); } catch (e) { throw new TtsError("Gemini lieferte keine gültige Antwort."); }
	const cand = json && Array.isArray(json.candidates) ? json.candidates[0] : null;
	const finish = (cand && cand.finishReason) || "";
	const part = cand && cand.content && Array.isArray(cand.content.parts) ? cand.content.parts[0] : null;
	const inline = part && part.inlineData;
	const data = (inline && inline.data) || "";
	if (!data) throw new TtsError("Gemini hat kein Audio geliefert (" + finish + ").");
	if (finish && finish !== "STOP") throw new TtsError("Gemini hat die Stimme abgebrochen (" + finish + ").");
	const pcm = base64ToBytes(data);
	const m = /rate=(\d+)/.exec((inline && inline.mimeType) || "");
	const rate = m ? parseInt(m[1], 10) : 24000;
	const isRiff = pcm.length > 44 && String.fromCharCode(pcm[0], pcm[1], pcm[2], pcm[3]) === "RIFF";
	const wav = isRiff ? pcm : withWavHeader(pcm, rate);
	const seconds = (wav.length - 44) / (wavRate(wav) * 2);
	if (text.length >= 60 && seconds < text.length / 25)
		throw new TtsError("Gemini-Audio zu kurz (" + seconds.toFixed(1) + " s für " + text.length + " Zeichen).");
	diag.log("INFO", "PERFORMANCE", "gemini.synthesize:erfolg", {
		modell: model, schluessel: kind, zeichen: text.length, dauer_ms: Math.round(performance.now() - t0),
	});
	return { bytes: wav, mime: "audio/wav" };
}

/**
 * Reihenfolge laut Schluesselwahl: erst kostenlos, dann bezahlt. Ist der Chirp-Rueckfall an,
 * geht es nach einem Fehler des kostenlosen Schluessels mit derselben Stimme zu Chirp 3 HD.
 * Netzfehler laufen unveraendert durch.
 */
export async function synthesize(text, voiceId, rate, cfg, googleKey) {
	const clean = (text || "").trim();
	if (!clean) return { bytes: new Uint8Array(0), mime: "audio/wav" };
	const c = cfg || {};
	const model = MODELS[modelKeyOf(voiceId)].model;
	const choice = c.keyChoice || KEY_FREE_THEN_PAID;
	const free = String(c.keyFree || "").replace(/\s/g, "");
	const paid = String(c.keyPaid || "").replace(/\s/g, "");
	const chirp = !!c.chirpFallback;
	let freeError = null;
	if (choice !== KEY_ONLY_PAID) {
		const blocked = Date.now() < freeBlockedUntil;
		if (free && !blocked) {
			// Mit Ausweichweg nicht erst bei 429 warten — der naechste Weg ist schneller.
			const fallback = chirp || (choice === KEY_FREE_THEN_PAID && !!paid);
			try {
				return await geminiRequest(clean, model, voiceId, free, "kostenlos", !fallback);
			} catch (e) {
				if (!(e instanceof TtsError)) throw e;
				freeError = e;
				if (fallback) freeBlockedUntil = Date.now() + FREE_PAUSE_MS;
				diag.log("WARN", "FEHLER", "gemini.kostenlos_gescheitert", { modell: model, message: String(e.message).slice(0, 120) });
			}
		}
		if (chirp) {
			diag.log("INFO", "ZUSTAND", "gemini.rueckfall_chirp", { stimme: geminiName(voiceId), gesperrt: blocked });
			const bytes = await google.synthesize(clean, chirpFor(voiceId), rate, googleKey);
			return { bytes, mime: "audio/mpeg" };
		}
		if (choice === KEY_ONLY_FREE) {
			throw freeError || new Error("Vorlesen fehlgeschlagen: Es ist kein kostenloser Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.");
		}
	}
	if (!paid) {
		throw freeError || new Error("Vorlesen fehlgeschlagen: Es ist kein Gemini-Schlüssel für die Stimme hinterlegt. Trag ihn in den Einstellungen ein.");
	}
	return await geminiRequest(clean, model, voiceId, paid, "bezahlt", true);
}
