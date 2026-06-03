/*
 * chunker.js — langen Text an Satzgrenzen in handliche Stuecke teilen.
 *
 * Beide TTS-Dienste haben Laengen-Grenzen (Google ~5000 Zeichen pro Aufruf,
 * Edge pro Anfrage ebenfalls begrenzt). Wir teilen darum bewusst kleiner:
 *   - an Satzgrenzen (.!?…), damit die Sprachmelodie natuerlich bleibt
 *   - sehr lange Saetze notfalls an Wort-/Kommagrenzen, im Extremfall hart
 *
 * Der Service-Worker synthetisiert die Stuecke der Reihe nach und legt sie in
 * die Wiedergabe-Queue des Offscreen-Dokuments — dadurch ist das erste Stueck
 * schnell zu hoeren und langer Text spielt nahtlos hintereinander.
 */

import { diag } from "../diag/diag.js";

export function splitIntoChunks(text, maxLen) {
	const limit = typeof maxLen === "number" && maxLen > 50 ? maxLen : 1200;
	const clean = String(text || "")
		.replace(/\s+/g, " ")
		.trim();
	if (!clean) return [];
	if (clean.length <= limit) return [clean];

	// In Saetze zerlegen, Satzzeichen am Stueck behalten.
	const sentences = clean.match(/[^.!?…]+[.!?…]+(?:\s|$)|[^.!?…]+$/g) || [
		clean,
	];

	const chunks = [];
	let buf = "";

	for (let raw of sentences) {
		const s = raw.trim();
		if (!s) continue;

		if (s.length > limit) {
			// Sehr langer Satz: aktuellen Puffer schliessen, dann hart teilen.
			if (buf) {
				chunks.push(buf);
				buf = "";
			}
			for (const piece of hardSplit(s, limit)) chunks.push(piece);
			continue;
		}

		if (buf && buf.length + 1 + s.length > limit) {
			chunks.push(buf);
			buf = s;
		} else {
			buf = buf ? buf + " " + s : s;
		}
	}

	if (buf) chunks.push(buf);
	diag.log("INFO", "PERFORMANCE", "chunker.splitIntoChunks", {
		chunks: chunks.length,
		textLen: clean.length,
		limit,
	});
	return chunks;
}

function hardSplit(s, limit) {
	const out = [];
	let rest = s;
	while (rest.length > limit) {
		// bevorzugt an einer Wortgrenze nahe dem Limit schneiden
		let cut = rest.lastIndexOf(" ", limit);
		if (cut < limit * 0.5) cut = limit; // keine sinnvolle Wortgrenze -> hart
		out.push(rest.slice(0, cut).trim());
		rest = rest.slice(cut).trim();
	}
	if (rest) out.push(rest);
	return out;
}
