/*
 * azure-tts.js — Engine 4: Azure Neural (Azure AI Speech, REST cognitiveservices/v1).
 *
 * Port aus Jarvis (Synthese.kt azure()/azureAnfrage(), TtsCatalog.kt):
 *   - eigener Schluessel (KEY 1) + Region (Standard westeurope, auch Portal-Endpunkt erlaubt)
 *   - Kennung "azure:<Stimmname>" inkl. HD-Stimmen (DragonHD)
 *   - 429 -> nach Retry-After (1–60 s) bzw. 3/10/20/30 s erneut (F0: ~20 Anfragen/Minute)
 *   - HD-Stimmen ohne Tempo-Rahmen; <lang> nur bei mehrsprachigen Stimmen
 *
 * Schnittstelle:
 *   listGermanVoices() -> Array<{ id, label }>
 *   synthesize(text, voiceId, rate, cfg) -> Promise<Uint8Array>  (MP3-Bytes)
 */

import { diag } from "../diag/diag.js";

export const DEFAULT_AZURE_VOICE = "azure:de-DE-SeraphinaMultilingualNeural";
export const DEFAULT_AZURE_REGION = "westeurope";
const WAIT_MS = [3000, 10000, 20000, 30000];

const VOICES = [
	{ id: "azure:de-DE-Seraphina:DragonHDLatestNeural", label: "Seraphina HD — weiblich" },
	{ id: "azure:de-DE-Florian:DragonHDLatestNeural", label: "Florian HD — männlich" },
	{ id: "azure:de-DE-SeraphinaMultilingualNeural", label: "Seraphina — weiblich" },
	{ id: "azure:de-DE-KatjaNeural", label: "Katja — weiblich" },
	{ id: "azure:de-DE-AmalaNeural", label: "Amala — weiblich" },
	{ id: "azure:de-DE-ElkeNeural", label: "Elke — weiblich" },
	{ id: "azure:de-DE-GiselaNeural", label: "Gisela (Kind) — weiblich" },
	{ id: "azure:de-DE-KlarissaNeural", label: "Klarissa — weiblich" },
	{ id: "azure:de-DE-LouisaNeural", label: "Louisa — weiblich" },
	{ id: "azure:de-DE-MajaNeural", label: "Maja — weiblich" },
	{ id: "azure:de-DE-TanjaNeural", label: "Tanja — weiblich" },
	{ id: "azure:de-DE-FlorianMultilingualNeural", label: "Florian — männlich" },
	{ id: "azure:de-DE-ConradNeural", label: "Conrad — männlich" },
	{ id: "azure:de-DE-KillianNeural", label: "Killian — männlich" },
	{ id: "azure:de-DE-BerndNeural", label: "Bernd — männlich" },
	{ id: "azure:de-DE-ChristophNeural", label: "Christoph — männlich" },
	{ id: "azure:de-DE-KasperNeural", label: "Kasper — männlich" },
	{ id: "azure:de-DE-KlausNeural", label: "Klaus — männlich" },
	{ id: "azure:de-DE-RalfNeural", label: "Ralf — männlich" },
];

export function listGermanVoices() {
	return VOICES.slice();
}

export function azureName(voiceId) {
	const s = String(voiceId || DEFAULT_AZURE_VOICE);
	return s.startsWith("azure:") ? s.slice(6) : s;
}

/** Regionsname ("westeurope"); nimmt auch den Portal-Endpunkt. null, wenn unbrauchbar. */
export function azureRegion(input) {
	let raw = String(input || "").trim().toLowerCase();
	if (!raw) raw = DEFAULT_AZURE_REGION;
	const m = /^(?:https?:\/\/)?([a-z0-9]+)\.(?:api\.cognitive|tts\.speech)\./.exec(raw);
	const region = m ? m[1] : raw;
	return /^[a-z0-9]{2,40}$/.test(region) ? region : null;
}

function xmlEscape(s) {
	return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;").replace(/'/g, "&apos;");
}

export async function synthesize(text, voiceId, rate, cfg) {
	const clean = (text || "").trim();
	if (!clean) return new Uint8Array(0);
	const c = cfg || {};
	const key = String(c.key || "").replace(/\s/g, "");
	if (!key) throw new Error("Vorlesen fehlgeschlagen: Es ist kein Azure-Schlüssel hinterlegt. Trag ihn in den Einstellungen ein.");
	const region = azureRegion(c.region);
	if (!region)
		throw new Error("Vorlesen fehlgeschlagen: Die Azure-Region „" + String(c.region || "").slice(0, 40) + "“ verstehe ich nicht. Trag den Regionsnamen ein, etwa westeurope.");
	const voice = azureName(voiceId);
	const hd = voice.includes("DragonHD");
	// Steuerzeichen sind in XML verboten; Azure lehnte das ganze SSML sonst mit 400 ab.
	// eslint-disable-next-line no-control-regex
	const escaped = xmlEscape(clean.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, " "));
	const inner = voice.includes("Multilingual") ? "<lang xml:lang='de-DE'>" + escaped + "</lang>" : escaped;
	const r = typeof rate === "number" && isFinite(rate) ? rate : 1.0;
	const pct = Math.round((Math.min(2, Math.max(0.5, r)) - 1) * 100);
	const withRate = hd || pct === 0 ? inner : "<prosody rate='" + (pct > 0 ? "+" : "") + pct + "%'>" + inner + "</prosody>";
	const ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='de-DE'>" +
		"<voice name='" + voice + "'>" + withRate + "</voice></speak>";
	const t0 = performance.now();
	let attempt = 0;
	for (;;) {
		let res;
		try {
			res = await fetch("https://" + region + ".tts.speech.microsoft.com/cognitiveservices/v1", {
				method: "POST",
				headers: {
					"Ocp-Apim-Subscription-Key": key,
					"Content-Type": "application/ssml+xml",
					"X-Microsoft-OutputFormat": "audio-24khz-96kbitrate-mono-mp3",
				},
				body: ssml,
			});
		} catch (e) {
			throw new Error("Kein Internet erreichbar.");
		}
		if (res.ok) {
			const bytes = new Uint8Array(await res.arrayBuffer());
			if (!bytes.length) throw new Error("Azure hat kein Audio geliefert.");
			diag.log("INFO", "PERFORMANCE", "azure.synthesize:erfolg", {
				stimme: voice, zeichen: clean.length, dauer_ms: Math.round(performance.now() - t0),
			});
			return bytes;
		}
		const code = res.status;
		const body = await res.text().catch(() => "");
		if (code === 429 && attempt < WAIT_MS.length) {
			const ra = parseInt(res.headers.get("Retry-After") || "", 10);
			const ms = isFinite(ra) ? Math.min(60000, Math.max(1000, ra * 1000)) : WAIT_MS[attempt];
			diag.log("WARN", "FEHLER", "azure.rate_limit", { ms, versuch: attempt });
			await new Promise((ok) => setTimeout(ok, ms));
			attempt++;
			continue;
		}
		if (code === 401 || code === 403)
			throw new Error("Vorlesen fehlgeschlagen: Azure hat Schlüssel oder Region abgelehnt (Fehler " + code + "). Prüf beides in den Einstellungen.");
		if (code === 429)
			throw new Error("Vorlesen fehlgeschlagen: Azure lässt gerade keine Anfragen zu (Fehler 429): Ratenlimit oder Monatskontingent erschöpft.");
		if (code === 400 && hd)
			throw new Error("Vorlesen fehlgeschlagen: Azure lehnt die HD-Stimme ab (Fehler 400). HD-Stimmen laufen nur mit der Stufe S0 und nicht in jeder Region.");
		throw new Error("Azure antwortete mit Fehler " + code + ": " + body.slice(0, 200));
	}
}
