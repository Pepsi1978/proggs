// ============================================================
// background.js — Service Worker (Manifest V3)
// ------------------------------------------------------------
// Ersetzt GM_xmlhttpRequest. Content-Scripts duerfen wegen CORS keine
// Cross-Origin-Requests an Groq/Gemini machen — der Service Worker schon
// (dank host_permissions im Manifest). Content schickt eine Nachricht,
// der Worker holt den API-Key aus chrome.storage, macht den fetch und
// schickt das Ergebnis zurueck.
// ============================================================

const GROQ_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
const REQUEST_TIMEOUT_MS = 120000;

async function getKey(name) {
	const obj = await chrome.storage.local.get(name);
	return String(obj[name] || "").trim();
}

function withTimeout(promise, ms, label) {
	let timer;
	const timeout = new Promise((_, reject) => {
		timer = setTimeout(() => reject(new Error(`${label}: Timeout`)), ms);
	});
	return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

// ── Groq Whisper Speech-to-Text ──
// Audio kommt als data-URL (base64) vom Content-Script, damit es ueber
// chrome.runtime.sendMessage serialisierbar ist.
async function groqTranscribe({ audioDataUrl, model, lang }) {
	const key = await getKey("groqKey");
	if (!key)
		return { ok: false, error: "Groq API-Key fehlt (Einstellungen oeffnen)." };

	if (!audioDataUrl) return { ok: false, error: "Kein Audio empfangen." };

	try {
		const blob = await (await fetch(audioDataUrl)).blob();
		const form = new FormData();
		form.append("file", blob, "recording.webm");
		form.append("model", model || "whisper-large-v3-turbo");
		form.append("language", lang || "de");
		form.append("response_format", "text");

		const r = await withTimeout(
			fetch(GROQ_URL, {
				method: "POST",
				headers: { Authorization: "Bearer " + key },
				body: form,
			}),
			REQUEST_TIMEOUT_MS,
			"Groq",
		);
		const text = await r.text();
		if (!r.ok) {
			let msg = text.slice(0, 400) || "HTTP " + r.status;
			try {
				const j = JSON.parse(text);
				if (j?.error?.message) msg = j.error.message;
			} catch {}
			return { ok: false, error: msg };
		}
		return { ok: true, text: (text || "").trim() };
	} catch (e) {
		return { ok: false, error: String(e?.message || e) };
	}
}

// ── Gemini generateContent ──
// In-memory: einmal aufgeloestes, fuer diesen Key funktionierendes Modell.
let resolvedGeminiModel = null;
const GEMINI_DEFAULT_MODEL = "models/gemini-2.5-flash";

function buildGeminiPayload(modelName, prompt, temperature, maxOutputTokens) {
	const generationConfig = modelName.includes("gemini-3")
		? {
				maxOutputTokens: maxOutputTokens || 2048,
				thinkingConfig: { thinkingLevel: "MEDIUM" },
			}
		: {
				temperature: temperature ?? 0.4,
				maxOutputTokens: maxOutputTokens || 2048,
			};
	return {
		contents: [{ role: "user", parts: [{ text: prompt }] }],
		generationConfig,
	};
}

// Ein einzelner generateContent-Aufruf. Liefert {ok, status, text?, error?}.
async function callGeminiOnce(modelName, key, payload) {
	const url =
		`https://generativelanguage.googleapis.com/v1beta/${modelName}:generateContent?key=` +
		encodeURIComponent(key);
	const r = await withTimeout(
		fetch(url, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(payload),
		}),
		REQUEST_TIMEOUT_MS,
		"Gemini",
	);
	const body = await r.text();
	if (!r.ok) {
		let msg = body.slice(0, 800) || `HTTP ${r.status}`;
		try {
			const j = JSON.parse(body);
			msg = j?.error?.message || j?.message || msg;
		} catch {}
		return { ok: false, status: r.status, error: msg };
	}
	const j = JSON.parse(body);
	const parts = j?.candidates?.[0]?.content?.parts;
	let out = "";
	if (Array.isArray(parts)) {
		out = parts
			.filter((p) => !p?.thought)
			.map((p) => p?.text ?? "")
			.join("")
			.trim();
	}
	if (!out && typeof j?.candidates?.[0]?.text === "string")
		out = j.candidates[0].text.trim();
	if (!out)
		return { ok: false, status: 200, error: "Gemini lieferte keinen Text." };
	return { ok: true, text: out };
}

// Fragt die fuer DIESEN Key verfuegbaren Modelle ab und waehlt ein Flash-Modell.
// So ist der Modellname nie mehr "falsch" — egal welche Modelle der Key freischaltet.
async function listFlashModel(key) {
	try {
		const url =
			"https://generativelanguage.googleapis.com/v1beta/models?pageSize=200&key=" +
			encodeURIComponent(key);
		const r = await withTimeout(fetch(url), REQUEST_TIMEOUT_MS, "ListModels");
		if (!r.ok) return null;
		const j = await r.json();
		const usable = (j.models || [])
			.filter((m) =>
				(m.supportedGenerationMethods || []).includes("generateContent"),
			)
			.map((m) => m.name);
		return (
			usable.find((n) => /flash-lite/i.test(n) && /latest/i.test(n)) ||
			usable.find((n) => /flash-lite/i.test(n)) ||
			usable.find((n) => /flash/i.test(n) && /latest/i.test(n)) ||
			usable.find((n) => /flash/i.test(n)) ||
			usable[0] ||
			null
		);
	} catch {
		return null;
	}
}

async function geminiGenerate({ prompt, model, temperature, maxOutputTokens }) {
	const key = await getKey("geminiKey");
	if (!key)
		return {
			ok: false,
			error: "Gemini API-Key fehlt (Einstellungen oeffnen).",
		};

	const configured = (model || "").replace(/^\/+/, "").trim();
	// Reihenfolge: vom Benutzer gesetzt > zuvor aufgeloest > Standard
	const startModel = configured || resolvedGeminiModel || GEMINI_DEFAULT_MODEL;

	const run = async (modelName, n) => {
		try {
			const res = await callGeminiOnce(
				modelName,
				key,
				buildGeminiPayload(modelName, prompt, temperature, maxOutputTokens),
			);
			if (res.ok) {
				resolvedGeminiModel = modelName;
				return { ok: true, text: res.text };
			}
			// Modell nicht gefunden/unterstuetzt -> ein gueltiges per ListModels suchen (einmal)
			const notFound =
				res.status === 404 ||
				/not\s*found|not\s*support|does not exist/i.test(res.error || "");
			if (notFound && n === 0) {
				const fb = await listFlashModel(key);
				if (fb && fb !== modelName) {
					const res2 = await callGeminiOnce(
						fb,
						key,
						buildGeminiPayload(fb, prompt, temperature, maxOutputTokens),
					);
					if (res2.ok) {
						resolvedGeminiModel = fb;
						return { ok: true, text: res2.text };
					}
					return {
						ok: false,
						error: `Gemini-Modell nicht verfuegbar (versucht: ${modelName} + ${fb}). ${res2.error}`,
					};
				}
				return {
					ok: false,
					error: `Kein nutzbares Gemini-Modell fuer diesen API-Key gefunden. (${res.error})`,
				};
			}
			return { ok: false, error: `HTTP ${res.status}: ${res.error}` };
		} catch (e) {
			const msg = String(e?.message || e);
			if (
				(msg.includes("Timeout") || msg.includes("Failed to fetch")) &&
				n < 3
			) {
				await new Promise((r) => setTimeout(r, 1200 * (n + 1)));
				return run(modelName, n + 1);
			}
			return { ok: false, error: msg };
		}
	};

	return run(startModel, 0);
}

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
	if (!msg || !msg.type) return;
	if (msg.type === "groqTranscribe") {
		groqTranscribe(msg).then(sendResponse);
		return true; // async sendResponse
	}
	if (msg.type === "geminiGenerate") {
		geminiGenerate(msg).then(sendResponse);
		return true;
	}
	return false;
});

// Klick auf das Erweiterungs-Icon oeffnet die Einstellungen als Seitenleiste
// (rechts, bleibt offen) statt als Popup oder neuer Tab.
try {
	chrome.sidePanel
		.setPanelBehavior({ openPanelOnActionClick: true })
		.catch((e) => console.warn("[Overlays] sidePanel:", e));
} catch (e) {
	console.warn("[Overlays] sidePanel API nicht verfuegbar:", e);
}
