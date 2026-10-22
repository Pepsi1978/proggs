// ============================================================
// background.js — Service Worker (Manifest V3)
// ------------------------------------------------------------
// Ersetzt GM_xmlhttpRequest. Content-Scripts duerfen wegen CORS keine
// Cross-Origin-Requests an Groq/Gemini machen — der Service Worker schon
// (dank host_permissions im Manifest). Content schickt eine Nachricht,
// der Worker holt den API-Key aus chrome.storage, macht den fetch und
// schickt das Ergebnis zurueck.
// ============================================================

importScripts("src/whisper-defaults.js");

const GROQ_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
const REQUEST_TIMEOUT_MS = 120000;
const CONTENT_SCRIPT_FILES = [
	"src/storage.js",
	"src/toast.js",
	"src/icons.js",
	"src/editable.js",
	"src/stt.js",
	"src/gemini.js",
	"src/actions.js",
	"src/ui.js",
	"src/registry.js",
	"src/content.js",
];

function customSiteInfo(url) {
	const pageUrl = new URL(url);
	if (pageUrl.protocol !== "http:" && pageUrl.protocol !== "https:")
		throw new Error("Nur HTTP- und HTTPS-Webseiten werden unterstuetzt.");
	const host = pageUrl.hostname.toLowerCase();
	return {
		host,
		matchPattern: `${pageUrl.protocol}//${host}/*`,
		// Hostnamen bestehen nur aus sicheren Zeichen; Punkte/Doppelpunkte werden
		// ersetzt, damit die ID auch bei IPv6 und lokalen Hosts eindeutig bleibt.
		scriptId: `ov_custom_${pageUrl.protocol.slice(0, -1)}_${host.replace(/[^a-z0-9]/gi, "_")}`,
	};
}

async function registerCustomContentScript(site) {
	try {
		await chrome.scripting.unregisterContentScripts({ ids: [site.scriptId] });
	} catch {
		// Noch nicht registriert ist beim ersten Aktivieren der Normalfall.
	}
	await chrome.scripting.registerContentScripts([
		{
			id: site.scriptId,
			matches: [site.matchPattern],
			css: ["src/overlay.css"],
			js: CONTENT_SCRIPT_FILES,
			runAt: "document_idle",
			persistAcrossSessions: true,
		},
	]);
}

async function injectCustomContentScript(tabId) {
	await chrome.scripting.insertCSS({
		target: { tabId },
		files: ["src/overlay.css"],
	});
	await chrome.scripting.executeScript({
		target: { tabId },
		files: CONTENT_SCRIPT_FILES,
		injectImmediately: true,
	});
}

function pageHasOverlay(tabId) {
	return new Promise((resolve) => {
		chrome.tabs.sendMessage(tabId, { type: "OV_GET_PAGE_STATE" }, (state) => {
			resolve(!chrome.runtime.lastError && !!state);
		});
	});
}

async function enableCustomSite(tabId, url) {
	const site = customSiteInfo(url);
	await registerCustomContentScript(site);

	const stored = await chrome.storage.local.get("ovCustomHosts");
	const hosts = Array.isArray(stored.ovCustomHosts) ? stored.ovCustomHosts : [];
	if (!hosts.includes(site.host)) {
		await chrome.storage.local.set({ ovCustomHosts: [...hosts, site.host] });
	}

	if (Number.isInteger(tabId)) {
		await injectCustomContentScript(tabId);
	}
	return { ok: true, host: site.host };
}

// Dynamische Content-Scripts sollen zwar persistent sein, werden bei einer
// Erweiterungsaktualisierung aber nicht auf jeder Chrome-Version erhalten.
// Die gespeicherte Freigabeliste ist daher die autoritative Quelle und baut
// die Registrierung beim Browser- oder Worker-Start erneut auf.
async function restoreCustomSiteScripts() {
	const stored = await chrome.storage.local.get("ovCustomHosts");
	const hosts = Array.isArray(stored.ovCustomHosts) ? stored.ovCustomHosts : [];
	const enabledHosts = new Set(
		hosts.map((value) => String(value || "").toLowerCase()).filter(Boolean),
	);
	for (const host of enabledHosts) {
		if (!host) continue;
		for (const protocol of ["https:", "http:"]) {
			const site = customSiteInfo(`${protocol}//${host}/`);
			const granted = await chrome.permissions.contains({ origins: [site.matchPattern] });
			if (granted) await registerCustomContentScript(site);
		}
	}

	// Chrome stellt Tabs teils vor dynamischen Scripts wieder her. Dann gibt es
	// noch keinen Listener, obwohl die Domain dauerhaft freigegeben ist.
	const tabs = await chrome.tabs.query({});
	for (const tab of tabs) {
		if (!Number.isInteger(tab.id) || !tab.url) continue;
		let site;
		try {
			site = customSiteInfo(tab.url);
		} catch {
			continue;
		}
		if (!enabledHosts.has(site.host) || (await pageHasOverlay(tab.id))) continue;
		const granted = await chrome.permissions.contains({ origins: [site.matchPattern] });
		if (granted) await injectCustomContentScript(tab.id);
	}
}

async function disableCustomSite(url) {
	const site = customSiteInfo(url);
	const stored = await chrome.storage.local.get("ovCustomHosts");
	const hosts = Array.isArray(stored.ovCustomHosts) ? stored.ovCustomHosts : [];
	await chrome.storage.local.set({
		ovCustomHosts: hosts.filter((host) => host !== site.host),
	});
	try {
		await chrome.scripting.unregisterContentScripts({ ids: [site.scriptId] });
	} catch {
		// Die Registrierung kann bereits durch ein Update entfernt worden sein.
	}
	await chrome.permissions.remove({ origins: [site.matchPattern] });
	return { ok: true, host: site.host };
}

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

// Confidence-Gate fuer Groq verbose_json (Schicht 2 gegen Stille-Halluzination).
// UND-Logik, NICHT ODER: nur verwerfen wenn HOHES no_speech_prob UND schlechtes
// avg_logprob — so bleibt echte leise Sprache (gutes avg_logprob) erhalten.
// Ultrakurze Clips liefern oft KEINE segments -> top-level text durchreichen
// (reine Stille ist bereits durch Schicht 1 im Content-Script gefiltert).
// Quelle: bugs/desktop/groq-transkription.md §2.3.
function filterTranscription(j) {
	const segs = Array.isArray(j?.segments) ? j.segments : null;
	if (!segs || segs.length === 0)
		return { text: String(j?.text || "").trim(), segments: [] };
	const kept = [];
	for (const s of segs) {
		const noSpeech = Number(s?.no_speech_prob ?? 0);
		const avgLp = Number(s?.avg_logprob ?? 0);
		const compR = Number(s?.compression_ratio ?? 0);
		const start = Number(s?.start ?? 0);
		const end = Number(s?.end ?? 0);
		const dur = end - start;
		const segText = String(s?.text ?? "");
		// Diagnose: alle Segmente mit Metadaten loggen (hilft beim Kalibrieren).
		console.log(
			`[Overlays] STT-Segment: no_speech=${noSpeech.toFixed(2)} avg_lp=${avgLp.toFixed(2)} comp=${compR.toFixed(2)} ${start.toFixed(2)}-${end.toFixed(2)}s "${segText.trim()}"`,
		);
		// Stille: hohes no_speech UND schlechtes avg_logprob
		if (noSpeech > 0.6 && avgLp < -1.0) {
			console.log("[Overlays]   -> verworfen (Stille-Confidence)");
			continue;
		}
		// Repetition ("danke danke danke…")
		if (compR > 2.4) {
			console.log("[Overlays]   -> verworfen (Repetition)");
			continue;
		}
		// Mini-Noise: sehr kurzes Segment mit Stille-Verdacht
		if (dur > 0 && dur < 0.4 && noSpeech > 0.6) {
			console.log("[Overlays]   -> verworfen (Mini-Noise)");
			continue;
		}
		kept.push({ text: segText, start, end });
	}
	// Segmente mit Zeitstempeln zurueckgeben -> das Content-Script gleicht sie
	// in Schicht 3 gegen das echte Audio ab (Trailing-Halluzination "Ja").
	return {
		text: kept
			.map((s) => s.text)
			.join("")
			.trim(),
		segments: kept,
	};
}

// ── Groq Whisper Speech-to-Text ──
// Audio kommt als data-URL (base64) vom Content-Script, damit es ueber
// chrome.runtime.sendMessage serialisierbar ist.
// ── Persoenliches Woerterbuch (1:1 wie TVO PersonalVocabulary + BuildVocabularyPrompt) ──
const VOCAB_MAX_WORDS = 100;
// Whisper nimmt max. 224 Prompt-Tokens; 350 Zeichen bleiben auch bei Komposita sicher darunter.
const VOCAB_MAX_PROMPT_CHARS = 350;
const ECHO_MARKER = "Begriffe";
// Nach einem 403 "blocked" fuer large-v3 30 Min. direkt Turbo nehmen. Der Merker liegt
// in chrome.storage, weil der MV3-Service-Worker nach ~30 s Leerlauf beendet wird.
const BLOCKED_KEY = "groqLargeV3BlockedUntil";
const BLOCKED_MS = 30 * 60 * 1000;

function parseVocabulary(raw) {
	const seen = new Set();
	const words = [];
	for (const part of String(raw || "").split(/[\n\r,;]/)) {
		const w = part.trim();
		if (!w || w.startsWith("#")) continue;
		const k = w.toLowerCase();
		if (seen.has(k)) continue;
		seen.add(k);
		words.push(w);
		if (words.length >= VOCAB_MAX_WORDS) break;
	}
	return words;
}

function buildVocabularyPrompt(words) {
	if (!words.length) return null;
	let s = ECHO_MARKER + ": ";
	let added = 0;
	for (const w of words) {
		// Zu lange Eintraege ueberspringen, nicht abbrechen: die uebrigen Begriffe zaehlen.
		if (s.length + w.length + 2 > VOCAB_MAX_PROMPT_CHARS) continue;
		if (added++ > 0) s += ", ";
		s += w;
	}
	return added === 0 ? null : s + ".";
}

function normEcho(s) {
	return String(s || "")
		.toLowerCase()
		.replace(/[^\p{L}\p{N}]/gu, "");
}

// Whisper gibt bei Stille/Gemurmel gern den Prompt selbst aus ("Begriffe: …").
function isPromptEcho(text, prompt) {
	if (!prompt || !String(text || "").trim()) return false;
	const t = normEcho(text);
	const p = normEcho(prompt);
	const marker = normEcho(ECHO_MARKER);
	return t.startsWith(marker) && p.includes(t.slice(marker.length));
}

function isBlockedError(raw) {
	return (
		raw.includes("model_permission_blocked_org") ||
		raw.includes("blocked at the organization level")
	);
}

async function loadVocabularyPrompt() {
	const st = await chrome.storage.local.get(["whisperVocabEnabled", "whisperVocab"]);
	if (st.whisperVocabEnabled === false) return null; // Standard: an
	const raw =
		st.whisperVocab === undefined ? OV_WHISPER.STANDARD_WOERTERBUCH : st.whisperVocab;
	return buildVocabularyPrompt(parseVocabulary(raw));
}

async function postGroq(key, blob, model, lang, prompt) {
	const form = new FormData();
	form.append("file", blob, "recording.webm");
	form.append("model", model);
	form.append("language", lang || "de");
	// prompt nur mit eingeschaltetem persoenlichem Woerterbuch (Schreibweisen von Eigennamen).
	if (prompt) form.append("prompt", prompt);
	// verbose_json liefert pro Segment no_speech_prob/avg_logprob/compression_ratio
	// (bei Groq ohne Mehrkosten/-latenz vs. text) -> Confidence-Gate moeglich.
	// temperature=0 als sinnvolle Basis. Siehe bugs/desktop/groq-transkription.md §2.3/§3.1.
	form.append("response_format", "verbose_json");
	form.append("temperature", "0");
	const r = await withTimeout(
		fetch(GROQ_URL, {
			method: "POST",
			headers: { Authorization: "Bearer " + key },
			body: form,
		}),
		REQUEST_TIMEOUT_MS,
		"Groq",
	);
	return { ok: r.ok, status: r.status, raw: await r.text() };
}

function groqErrorMessage(res) {
	let msg = res.raw.slice(0, 400) || "HTTP " + res.status;
	try {
		const j = JSON.parse(res.raw);
		if (j?.error?.message) msg = j.error.message;
	} catch {}
	return msg;
}

function parseGroqResult(raw) {
	// Schicht 2: Stille-/Repetitions-Segmente herausfiltern (funktionserhaltend).
	// segments (mit Zeitstempeln) gehen ans Content-Script fuer den Audio-Abgleich (Schicht 3).
	try {
		return filterTranscription(JSON.parse(raw));
	} catch {
		// Kein JSON (sollte bei verbose_json nicht passieren) -> roh durchreichen.
		return { text: (raw || "").trim(), segments: [] };
	}
}

async function groqTranscribe({ audioDataUrl, model, lang }) {
	const key = await getKey("groqKey");
	if (!key)
		return { ok: false, error: "Groq API-Key fehlt (Einstellungen oeffnen)." };

	if (!audioDataUrl) return { ok: false, error: "Kein Audio empfangen." };

	try {
		const blob = await (await fetch(audioDataUrl)).blob();
		// Gespeicherte Wahl direkt lesen: wirkt ab der naechsten Aufnahme, ohne Seiten-Reload
		// (der Cache im Content-Script kennt nur den Stand beim Laden der Seite).
		const stored = await chrome.storage.local.get("whisperModel");
		if (stored.whisperModel) model = stored.whisperModel;
		let useModel =
			model === OV_WHISPER.MODEL_LARGE_V3
				? OV_WHISPER.MODEL_LARGE_V3
				: OV_WHISPER.MODEL_TURBO;
		if (useModel === OV_WHISPER.MODEL_LARGE_V3) {
			const st = await chrome.storage.local.get(BLOCKED_KEY);
			if (Number(st[BLOCKED_KEY] || 0) > Date.now()) useModel = OV_WHISPER.MODEL_TURBO;
		}
		const prompt = await loadVocabularyPrompt();

		let res = await postGroq(key, blob, useModel, lang, prompt);
		if (
			!res.ok &&
			useModel === OV_WHISPER.MODEL_LARGE_V3 &&
			isBlockedError(res.raw)
		) {
			// large-v3 ist fuer die Groq-Organisation nicht freigeschaltet: Diktat nicht
			// verlieren, einmal mit Turbo wiederholen und 30 Min. direkt Turbo nehmen.
			console.warn("[Overlays] Groq: large-v3 gesperrt -> Rueckfall auf Turbo (30 Min.)");
			await chrome.storage.local.set({ [BLOCKED_KEY]: Date.now() + BLOCKED_MS });
			useModel = OV_WHISPER.MODEL_TURBO;
			res = await postGroq(key, blob, useModel, lang, prompt);
		}
		if (!res.ok) return { ok: false, error: groqErrorMessage(res) };

		let r2 = parseGroqResult(res.raw);
		if (isPromptEcho(r2.text, prompt)) {
			// Dann einmal ohne Woerterbuch wiederholen statt das Diktat zu verlieren.
			console.warn("[Overlays] Groq: Prompt-Echo erkannt -> erneut ohne Woerterbuch");
			res = await postGroq(key, blob, useModel, lang, null);
			if (!res.ok) return { ok: false, error: groqErrorMessage(res) };
			r2 = parseGroqResult(res.raw);
		}
		return { ok: true, text: r2.text, segments: r2.segments };
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
	if (msg.type === "OV_ENABLE_CUSTOM_SITE") {
		enableCustomSite(msg.tabId, msg.url)
			.then(sendResponse)
			.catch((error) => sendResponse({ ok: false, error: String(error?.message || error) }));
		return true;
	}
	if (msg.type === "OV_DISABLE_CUSTOM_SITE") {
		disableCustomSite(msg.url)
			.then(sendResponse)
			.catch((error) => sendResponse({ ok: false, error: String(error?.message || error) }));
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

// ── "Erweiterung neu laden": offene Seite automatisch mit-neuladen ──
// chrome.runtime.reload() macht Content-Scripts in bereits offenen Tabs ungueltig
// ("Extension context invalidated") -> Buttons/Mic gingen erst nach manuellem F5
// wieder. Die Options-Seite setzt vor dem Reload das Flag ov_reload_tabs_after_update
// (ueberlebt den Reload). Beim SW-Neustart laden wir die aktiven Tabs einmal neu,
// damit ein frisches Content-Script injiziert wird — kein F5 noetig.
function reloadActiveTabsAfterUpdate() {
	try {
		chrome.storage.local.get("ov_reload_tabs_after_update", (res) => {
			if (chrome.runtime.lastError || !res || !res.ov_reload_tabs_after_update)
				return;
			chrome.storage.local.remove("ov_reload_tabs_after_update");
			chrome.tabs.query({ active: true }, (tabs) => {
				if (chrome.runtime.lastError) return;
				for (const t of tabs || []) {
					if (t.id != null) {
						chrome.tabs.reload(t.id, {}, () => void chrome.runtime.lastError);
					}
				}
			});
		});
	} catch (e) {
		console.warn("[Overlays] reload-tabs:", e);
	}
}

reloadActiveTabsAfterUpdate();

restoreCustomSiteScripts().catch((error) =>
	console.warn("[Overlays] custom-site restore:", error),
);
chrome.runtime.onStartup.addListener(() => {
	restoreCustomSiteScripts().catch((error) =>
		console.warn("[Overlays] custom-site startup restore:", error),
	);
});
