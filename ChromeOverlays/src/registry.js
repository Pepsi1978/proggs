// ============================================================
// registry.js — DIE ZENTRALE DATEI
// ------------------------------------------------------------
// Pro Webseite ein Profil. "buttons" ist eine einfache Liste von
// Schluesseln aus dem CATALOG (ui.js), von UNTEN nach OBEN gestapelt.
//
// EINE NEUE SEITE HINZUFUEGEN:
//   1) Hier unten ein neues Profil-Objekt ergaenzen (match + buttons).
//   2) Die URL-Muster der Seite in manifest.json -> content_scripts.matches
//      und ggf. host_permissions eintragen.
//   3) Erweiterung in chrome://extensions neu laden. Fertig.
//
// Verfuegbare Button-Schluessel (siehe CATALOG in ui.js):
//   mic, enter, paste, copy, clear, gemini, promptFrank, promptGeneral, memory
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	OV.SITE_PROFILES = [
		{
			id: "translate",
			label: "Google Uebersetzer",
			match: (host) => /(^|\.)translate\.google\.(com|de)$/.test(host),
			uiPos: { right: 27, bottom: 87 },
			// 4 Buttons — genau wie das urspruengliche Translate-Tampermonkey-Skript
			buttons: ["mic", "paste", "copy", "clear"],
		},
		{
			id: "chatgpt",
			label: "ChatGPT",
			match: (host) => host === "chatgpt.com" || host === "chat.openai.com",
			uiPos: { right: 16, bottom: 124, shiftLeft: 11.34 },
			gemini: true, // schaltet die Gemini-Features frei (Korrektur, Prompt-Builder)
			// 9 Buttons
			buttons: [
				"mic",
				"enter",
				"paste",
				"copy",
				"clear",
				"gemini",
				"promptFrank",
				"promptGeneral",
				"memory",
			],
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
