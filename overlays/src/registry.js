// ============================================================
// registry.js — DIE ZENTRALE DATEI
// ------------------------------------------------------------
// Pro Webseite ein Profil. Das Layout ist ein 2-Spalten-Raster:
//   layout.right = rechte Spalte (von UNTEN nach oben)
//   layout.left  = linke  Spalte (von UNTEN nach oben)
// So bleibt die Leiste kompakt statt einer hohen Reihe.
//
// EINE NEUE SEITE HINZUFUEGEN:
//   1) Hier unten ein neues Profil-Objekt ergaenzen (match + layout).
//   2) Die URL-Muster der Seite in manifest.json -> content_scripts.matches
//      eintragen (host_permissions sind nur fuer Groq/Gemini noetig).
//   3) Erweiterung in chrome://extensions neu laden. Fertig.
//
// Verfuegbare Button-Schluessel (siehe CATALOG in ui.js):
//   mic, enter, paste, copy, clear, gemini, promptFrank, promptGeneral, memory
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	// Von Frank festgelegte Anordnung (jeweils von UNTEN nach oben):
	//   rechte Spalte:  Mic, Enter, Frank-Prompt, Kopieren [, Memory ganz oben]
	//   linke  Spalte:  X (Leeren), Gemini (ueber X), 10.-Klasse, Einfuegen
	const RIGHT = ["mic", "enter", "promptFrank", "copy"];
	const RIGHT_MEM = [...RIGHT, "memory"]; // Memory ganz oben rechts
	const LEFT = ["clear", "gemini", "promptGeneral", "paste"];
	const GENERIC_RIGHT = ["mic", "copy"];
	const GENERIC_LEFT = ["clear", "paste"];

	// Standard-Position fuer KI-Chat-Overlays (unten rechts, ueber dem Eingabefeld).
	const CHAT_POS = { right: 16, bottom: 124, shiftLeft: 11.34 };

	OV.SITE_PROFILES = [
		// ── Google Uebersetzer: nur Spracheingabe + Basis-Tools (2x2) ──
		{
			id: "translate",
			label: "Google Uebersetzer",
			match: (host) => /(^|\.)translate\.google\.(com|de)$/.test(host),
			uiPos: { right: 27, bottom: 87 },
			layout: { right: ["mic", "copy"], left: ["clear", "paste"] },
		},

		// ── KI-Chat-Seiten: volle Leiste (Whisper + Gemini-Prompt-Builder) ──
		{
			id: "chatgpt",
			label: "ChatGPT",
			match: (host) => host === "chatgpt.com" || host === "chat.openai.com",
			uiPos: { right: 16, bottom: 124, shiftLeft: 11.34 },
			gemini: true,
			layout: { right: RIGHT_MEM, left: LEFT }, // mit Memory (9)
		},
		{
			id: "claude",
			label: "Claude",
			match: (host) => host === "claude.ai" || host === "www.claude.ai",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT }, // ohne Memory (8)
		},
		{
			id: "gemini",
			label: "Gemini",
			match: (host) => host === "gemini.google.com",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT_MEM, left: LEFT }, // mit Memory (9)
		},
		{
			id: "grok",
			label: "Grok",
			match: (host) => host === "grok.com" || host === "www.grok.com",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT },
		},
		{
			id: "aistudio",
			label: "Google AI Studio",
			match: (host) =>
				host === "aistudio.google.com" || host === "www.aistudio.google.com",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT },
		},
		{
			id: "platformopenai",
			label: "OpenAI Platform",
			match: (host) => host === "platform.openai.com",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT },
		},
		{
			id: "mistral",
			label: "Mistral",
			match: (host) => host === "chat.mistral.ai",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT },
		},
		{
			id: "lmarena",
			label: "LMArena",
			match: (host) =>
				host === "arena.ai" ||
				host === "web.arena.ai" ||
				host === "chat.lmsys.org" ||
				host === "arena.lmsys.org",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT },
		},
		{
			id: "notebooklm",
			label: "NotebookLM",
			match: (host) => host === "notebooklm.google.com",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT },
		},
		{
			id: "cortex",
			label: "Cortex-Dashboard",
			match: (host) => host === "10.8.0.1",
			uiPos: CHAT_POS,
			gemini: true,
			layout: { right: RIGHT, left: LEFT },
		},
	];

	OV.findProfile = (host) => {
		for (const p of OV.SITE_PROFILES) {
			try {
				if (p.match(host)) return p;
			} catch {}
		}
		return null;
	};

	// Fuer vom Nutzer explizit freigegebene Webseiten: die Funktionen, die mit
	// beliebigen normalen Eingabefeldern funktionieren, ohne Chat-spezifische Annahmen.
	OV.createGenericProfile = () => ({
		id: "generic",
		label: "Diese Webseite",
		uiPos: { right: 16, bottom: 96 },
		layout: { right: GENERIC_RIGHT, left: GENERIC_LEFT },
	});
})();
