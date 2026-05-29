/*
 * google-tts.js — Engine 2: Google Chirp 3 HD (Cloud Text-to-Speech, API-Key).
 *
 * Die Logik ist aus dem Projekt "Best Journal" uebernommen
 * (BestJournalFrank/.../GoogleCloudTtsPlayer.kt + Constants.kt):
 *   - Synthese ueber POST /v1/text:synthesize mit reinem Text (kein SSML)
 *   - voice.languageCode = "de-DE", audioConfig.audioEncoding = "MP3"
 *   - KEIN pitch (Chirp 3 HD unterstuetzt/ignoriert es)
 *   - Default-Stimme de-DE-Chirp3-HD-Kore
 *   - feste 30er-Stimmenliste als robuster Fallback
 *
 * Ergaenzung gegenueber Best Journal (war dort statisch):
 *   - Stimmen werden PRIMAER dynamisch ueber /v1/voices?languageCode=de-DE
 *     geladen und auf "chirp3-hd" gefiltert. Nur wenn das nicht geht, greift
 *     die feste Best-Journal-Liste. So erscheinen neue Stimmen automatisch.
 *   - speakingRate (Tempo-Regler), das Best Journal nicht hatte.
 *
 * Einheitliche Engine-Schnittstelle (auch in edge-tts.js):
 *   listGermanVoices(apiKey) -> Promise<Array<{ id, label }>>
 *   synthesize(text, voice, rate, apiKey) -> Promise<Uint8Array>  (MP3-Bytes)
 */

import { diag } from "../diag/diag.js";

const SYNTH_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";
const VOICES_URL = "https://texttospeech.googleapis.com/v1/voices";
const DEFAULT_VOICE = "de-DE-Chirp3-HD-Kore";

// Feste Best-Journal-Liste (de-DE-Chirp3-HD-*): Fallback, falls der dynamische
// Abruf nichts liefert. Reihenfolge/Labels 1:1 aus Constants.GOOGLE_TTS_VOICES.
const FALLBACK_GOOGLE_VOICES = [
	// weiblich
	{ id: "de-DE-Chirp3-HD-Achernar", label: "Achernar — weiblich" },
	{ id: "de-DE-Chirp3-HD-Aoede", label: "Aoede — weiblich" },
	{ id: "de-DE-Chirp3-HD-Autonoe", label: "Autonoe — weiblich" },
	{ id: "de-DE-Chirp3-HD-Callirrhoe", label: "Callirrhoe — weiblich" },
	{ id: "de-DE-Chirp3-HD-Despina", label: "Despina — weiblich" },
	{ id: "de-DE-Chirp3-HD-Erinome", label: "Erinome — weiblich" },
	{ id: "de-DE-Chirp3-HD-Gacrux", label: "Gacrux — weiblich" },
	{ id: "de-DE-Chirp3-HD-Kore", label: "Kore — weiblich" },
	{ id: "de-DE-Chirp3-HD-Laomedeia", label: "Laomedeia — weiblich" },
	{ id: "de-DE-Chirp3-HD-Leda", label: "Leda — weiblich" },
	{ id: "de-DE-Chirp3-HD-Pulcherrima", label: "Pulcherrima — weiblich" },
	{ id: "de-DE-Chirp3-HD-Sulafat", label: "Sulafat — weiblich" },
	{ id: "de-DE-Chirp3-HD-Vindemiatrix", label: "Vindemiatrix — weiblich" },
	{ id: "de-DE-Chirp3-HD-Zephyr", label: "Zephyr — weiblich" },
	// männlich
	{ id: "de-DE-Chirp3-HD-Achird", label: "Achird — männlich" },
	{ id: "de-DE-Chirp3-HD-Algenib", label: "Algenib — männlich" },
	{ id: "de-DE-Chirp3-HD-Algieba", label: "Algieba — männlich" },
	{ id: "de-DE-Chirp3-HD-Alnilam", label: "Alnilam — männlich" },
	{ id: "de-DE-Chirp3-HD-Charon", label: "Charon — männlich" },
	{ id: "de-DE-Chirp3-HD-Enceladus", label: "Enceladus — männlich" },
	{ id: "de-DE-Chirp3-HD-Fenrir", label: "Fenrir — männlich" },
	{ id: "de-DE-Chirp3-HD-Iapetus", label: "Iapetus — männlich" },
	{ id: "de-DE-Chirp3-HD-Orus", label: "Orus — männlich" },
	{ id: "de-DE-Chirp3-HD-Puck", label: "Puck — männlich" },
	{ id: "de-DE-Chirp3-HD-Rasalgethi", label: "Rasalgethi — männlich" },
	{ id: "de-DE-Chirp3-HD-Sadachbia", label: "Sadachbia — männlich" },
	{ id: "de-DE-Chirp3-HD-Sadaltager", label: "Sadaltager — männlich" },
	{ id: "de-DE-Chirp3-HD-Schedar", label: "Schedar — männlich" },
	{ id: "de-DE-Chirp3-HD-Umbriel", label: "Umbriel — männlich" },
	{ id: "de-DE-Chirp3-HD-Zubenelgenubi", label: "Zubenelgenubi — männlich" },
];

export const DEFAULT_GOOGLE_VOICE = DEFAULT_VOICE;

// Chirp akzeptiert speakingRate ~0.25–2.0
function clampGoogleRate(rate) {
	const r = typeof rate === "number" && isFinite(rate) ? rate : 1.0;
	return Math.min(2.0, Math.max(0.25, r));
}

// HTTP-Fehler von Google in verstaendliches Deutsch uebersetzen.
async function describeHttpError(res) {
	let detail = "";
	try {
		const j = await res.json();
		detail = (j && j.error && j.error.message) || "";
	} catch (e) {
		/* kein JSON-Body */
	}
	const d = detail.toLowerCase();
	if (res.status === 400 && d.includes("api key not valid"))
		return "Google-API-Key ungültig.";
	if (
		res.status === 403 &&
		(d.includes("has not been used") ||
			d.includes("disabled") ||
			d.includes("not been enabled"))
	)
		return "Text-to-Speech-API im Google-Projekt nicht aktiviert.";
	if (res.status === 401 || res.status === 403)
		return "Google-API-Key ungültig oder ohne Berechtigung.";
	if (res.status === 429)
		return "Google-Kontingent erschöpft — später erneut versuchen.";
	return detail || "Google-Fehler " + res.status + ".";
}

function labelFor(v) {
	const short =
		String(v.name || "")
			.split("-")
			.pop() || v.name;
	const g =
		v.ssmlGender === "FEMALE"
			? "weiblich"
			: v.ssmlGender === "MALE"
				? "männlich"
				: "";
	return g ? short + " — " + g : short;
}

function base64ToBytes(b64) {
	const bin = atob(b64);
	const out = new Uint8Array(bin.length);
	for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
	return out;
}

// ----- Stimmenliste fuer das Dropdown --------------------------------------
export async function listGermanVoices(apiKey) {
	const key = (apiKey || "").trim();
	if (!key) return []; // ohne Key kein Abruf — Panel zeigt "Erst API-Key eintragen"

	let res;
	try {
		res = await fetch(
			VOICES_URL + "?key=" + encodeURIComponent(key) + "&languageCode=de-DE",
		);
	} catch (e) {
		// Netzwerkproblem: bewaehrte Liste, damit der Nutzer trotzdem arbeiten kann
		diag.log("WARN", "FEHLER", "google.listGermanVoices:netzwerk_fallback", {
			message: String((e && e.message) || e),
		});
		return FALLBACK_GOOGLE_VOICES.slice();
	}

	if (!res.ok) {
		// Key-/API-Problem deutlich machen (kein stiller Fallback)
		diag.log("ERROR", "FEHLER", "google.listGermanVoices:http", {
			status: res.status,
		});
		throw new Error(await describeHttpError(res));
	}

	let data;
	try {
		data = await res.json();
	} catch (e) {
		return FALLBACK_GOOGLE_VOICES.slice();
	}

	const all = Array.isArray(data.voices) ? data.voices : [];
	const chirp = all
		.filter(
			(v) =>
				v &&
				typeof v.name === "string" &&
				v.name.toLowerCase().includes("chirp3-hd"),
		)
		.map((v) => ({ id: v.name, label: labelFor(v) }));

	if (chirp.length) {
		chirp.sort((a, b) => a.label.localeCompare(b.label, "de"));
		return chirp;
	}

	// Key gueltig, aber keine Chirp-3-HD-Stimmen geliefert -> Best-Journal-Liste
	return FALLBACK_GOOGLE_VOICES.slice();
}

// ----- Synthese: ein Textstueck -> MP3-Bytes -------------------------------
export async function synthesize(text, voice, rate, apiKey) {
	const clean = (text || "").trim();
	if (!clean) return new Uint8Array(0);

	const key = (apiKey || "").trim();
	if (!key) throw new Error("Kein Google-API-Key eingetragen.");

	const t0 = performance.now();
	diag.log("INFO", "FUNKTION", "google.synthesize:eintritt", {
		voice: voice || DEFAULT_VOICE,
		rate,
		textLen: clean.length,
	});

	const body = {
		input: { text: clean }, // reiner Text — Chirp unterstuetzt SSML nur eingeschraenkt
		voice: { languageCode: "de-DE", name: voice || DEFAULT_VOICE },
		audioConfig: {
			audioEncoding: "MP3",
			speakingRate: clampGoogleRate(rate),
			// bewusst KEIN pitch — wird von Chirp 3 HD ignoriert/nicht unterstuetzt
		},
	};

	let res;
	try {
		res = await fetch(SYNTH_URL + "?key=" + encodeURIComponent(key), {
			method: "POST",
			headers: { "Content-Type": "application/json; charset=utf-8" },
			body: JSON.stringify(body),
		});
	} catch (e) {
		diag.log("ERROR", "FEHLER", "google.synthesize:netzwerk", {
			message: String((e && e.message) || e),
		});
		throw new Error("Kein Internet erreichbar.");
	}

	if (!res.ok) {
		diag.log("ERROR", "FEHLER", "google.synthesize:http", {
			status: res.status,
		});
		throw new Error(await describeHttpError(res));
	}

	const data = await res.json();
	const b64 = data && data.audioContent;
	if (!b64) {
		diag.log("ERROR", "FEHLER", "google.synthesize:keine_audiodaten", {});
		throw new Error("Google lieferte keine Audiodaten.");
	}
	const bytes = base64ToBytes(b64);
	diag.log("INFO", "PERFORMANCE", "google.synthesize:erfolg", {
		bytes: bytes.length,
		dauer_ms: Math.round(performance.now() - t0),
	});
	return bytes;
}
