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

	const blob = await (await fetch(audioDataUrl)).blob();
	const form = new FormData();
	form.append("file", blob, "recording.webm");
	form.append("model", model || "whisper-large-v3-turbo");
	form.append("language", lang || "de");
	form.append("response_format", "text");

	try {
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
async function geminiGenerate({ prompt, model, temperature, maxOutputTokens }) {
	const key = await getKey("geminiKey");
	if (!key)
		return {
			ok: false,
			error: "Gemini API-Key fehlt (Einstellungen oeffnen).",
		};

	const usedModel = (model || "models/gemini-3.1-flash-lite").replace(
		/^\/+/,
		"",
	);
	const url =
		`https://generativelanguage.googleapis.com/v1beta/${usedModel}:generateContent?key=` +
		encodeURIComponent(key);

	const generationConfig = usedModel.includes("gemini-3")
		? {
				maxOutputTokens: maxOutputTokens || 2048,
				thinkingConfig: { thinkingLevel: "MEDIUM" },
			}
		: {
				temperature: temperature ?? 0.4,
				maxOutputTokens: maxOutputTokens || 2048,
			};

	const payload = {
		contents: [{ role: "user", parts: [{ text: prompt }] }],
		generationConfig,
	};

	const attempt = async (n) => {
		try {
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
				let msg = body.slice(0, 800);
				try {
					const j = JSON.parse(body);
					msg = j?.error?.message || j?.message || msg;
				} catch {}
				return { ok: false, error: `HTTP ${r.status}: ${msg}` };
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
			if (!out) return { ok: false, error: "Gemini lieferte keinen Text." };
			return { ok: true, text: out };
		} catch (e) {
			const msg = String(e?.message || e);
			if (
				(msg.includes("Timeout") || msg.includes("Failed to fetch")) &&
				n < 3
			) {
				await new Promise((r) => setTimeout(r, 1200 * (n + 1)));
				return attempt(n + 1);
			}
			return { ok: false, error: msg };
		}
	};

	return attempt(0);
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

// Klick auf das Erweiterungs-Icon oeffnet die Einstellungen (API-Keys).
chrome.action.onClicked.addListener(() => {
	chrome.runtime.openOptionsPage();
});
