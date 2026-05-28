/*
 * google-tts.js — Engine 2: Google Chirp 3 HD (Cloud Text-to-Speech, API-Key).
 *
 * Die Stimmen-Logik ist exakt aus dem Projekt "Best Journal" uebernommen:
 *   - Stimmen dynamisch ueber /v1/voices?languageCode=de-DE abrufen
 *   - auf Namen filtern, die "Chirp3-HD" enthalten (case-insensitive)
 *   - Fallback: beste verfuegbare de-DE-Stimme
 *   - Synthese mit reinem Text (kein SSML), MP3, speakingRate; kein pitch
 *
 * Einheitliche Engine-Schnittstelle (auch in edge-tts.js):
 *   listGermanVoices(apiKey) -> Promise<Array<{ id, label }>>
 *   synthesize(text, voice, rate, apiKey) -> Promise<Uint8Array>  (MP3-Bytes)
 *
 * STUFE 1: nur Signaturen (Stubs). Die echte Umsetzung folgt in Stufe 3.
 */

export async function listGermanVoices(/* apiKey */) {
	// STUFE 3: /v1/voices abrufen, auf Chirp3-HD (de) filtern, Fallback wie Best Journal.
	return [];
}

export async function synthesize(/* text, voice, rate, apiKey */) {
	// STUFE 3: POST /v1/text:synthesize, audioContent (Base64-MP3) dekodieren.
	throw new Error("Google Chirp 3 HD wird in Stufe 3 angebunden.");
}
