// ============================================================
// registry.js — DIE ZENTRALE DATEI
// ------------------------------------------------------------
// Pro Webseite ein Profil. "buttons" ist eine einfache Liste von
// Schluesseln aus dem CATALOG (ui.js), von UNTEN nach OBEN gestapelt.
//
// EINE NEUE SEITE HINZUFUEGEN:
//   1) Hier unten ein neues Profil-Objekt ergaenzen (match + buttons).
//   2) Die URL-Muster der Seite in manifest.json -> content_scripts.matches
//      eintragen (host_permissions sind nur fuer Groq/Gemini noetig, nicht
//      fuer die Seiten selbst).
//   3) Erweiterung in chrome://extensions neu laden. Fertig.
//
// Verfuegbare Button-Schluessel (siehe CATALOG in ui.js):
//   mic, enter, paste, copy, clear, gemini, promptFrank, promptGeneral, memory
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	// Geteilte Button-Sets der KI-Chat-Seiten (alle aus den Tampermonkey-Skripten).
	const CHAT = [
		"mic",
		"enter",
		"paste",
		"copy",
		"clear",
		"gemini",
		"promptFrank",
		"promptGeneral",
	];
	const CHAT_MEM = [...CHAT, "memory"]; // Seiten mit zusaetzlichem Memory-Prompt-Button

	// Standard-Position fuer KI-Chat-Overlays (unten rechts, ueber dem Eingabefeld).
	const CHAT_POS = { right: 16, bottom: 110, shiftLeft: 11.34 };

	OV.SITE_PROFILES = [
		// ── Google Uebersetzer: nur Spracheingabe + Basis-Tools (4 Buttons) ──
		{
			id: "translate",
			label: "Google Uebersetzer",
			match: (host) => /(^|\.)translate\.google\.(com|de)$/.test(host),
			uiPos: { right: 27, bottom: 87 },
			buttons: ["mic", "paste", "copy", "clear"],
		},

		// ── KI-Chat-Seiten: volle Leiste (Whisper + Gemini-Prompt-Builder) ──
		{
			id: "chatgpt",
			label: "ChatGPT",
			match: (host) => host === "chatgpt.com" || host === "chat.openai.com",
			uiPos: { right: 16, bottom: 124, shiftLeft: 11.34 },
			gemini: true,
			buttons: CHAT_MEM, // 9 Buttons (mit Memory)
		},
		{
			id: "claude",
			label: "Claude",
			match: (host) => host === "claude.ai" || host === "www.claude.ai",
			uiPos: CHAT_POS,
			gemini: true,
			buttons: CHAT, // 8 Buttons
		},
		{
			id: "gemini",
			label: "Gemini",
			match: (host) => host === "gemini.google.com",
			uiPos: CHAT_POS,
			gemini: true,
			buttons: CHAT_MEM, // 9 Buttons (mit Memory)
		},
		{
			id: "grok",
			label: "Grok",
			match: (host) => host === "grok.com" || host === "www.grok.com",
			uiPos: CHAT_POS,
			gemini: true,
			buttons: CHAT,
		},
		{
			id: "aistudio",
			label: "Google AI Studio",
			match: (host) =>
				host === "aistudio.google.com" || host === "www.aistudio.google.com",
			uiPos: CHAT_POS,
			gemini: true,
			buttons: CHAT,
		},
		{
			id: "platformopenai",
			label: "OpenAI Platform",
			match: (host) => host === "platform.openai.com",
			uiPos: CHAT_POS,
			gemini: true,
			buttons: CHAT,
		},
		{
			id: "mistral",
			label: "Mistral",
			match: (host) => host === "chat.mistral.ai",
			uiPos: CHAT_POS,
			gemini: true,
			buttons: CHAT,
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
			buttons: CHAT,
		},
		{
			id: "notebooklm",
			label: "NotebookLM",
			match: (host) => host === "notebooklm.google.com",
			uiPos: CHAT_POS,
			gemini: true,
			buttons: CHAT,
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
})();
