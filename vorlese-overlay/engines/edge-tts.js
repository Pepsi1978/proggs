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
 * STUFE 1: nur Signaturen (Stubs). Die echte Umsetzung folgt in Stufe 2.
 */

export async function listGermanVoices(/* apiKey */) {
	// STUFE 2: Stimmenliste vom Edge-Endpunkt laden und auf de-DE filtern.
	return [];
}

export async function synthesize(/* text, voice, rate */) {
	// STUFE 2: WebSocket + Sec-MS-GEC-Token + SSML + Frame-Parsing.
	throw new Error("Edge TTS wird in Stufe 2 angebunden.");
}
