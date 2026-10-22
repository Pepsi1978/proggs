// ============================================================
// whisper-defaults.js — gemeinsame Vorgaben fuer Whisper-Modell + Woerterbuch
// ------------------------------------------------------------
// Klassisches Script (kein Modul): background.js laedt es per importScripts,
// options.html per <script src>. Vorbild: TerminalVoiceOverlay (TVO).
// ============================================================
globalThis.OV_WHISPER = {
	MODEL_TURBO: "whisper-large-v3-turbo",
	MODEL_LARGE_V3: "whisper-large-v3",
	// Aktueller TVO-Stand; gilt, solange noch nie etwas gespeichert wurde.
	STANDARD_WOERTERBUCH:
		"Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic",
};
