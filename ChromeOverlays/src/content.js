// ============================================================
// content.js — Einstiegspunkt
// ------------------------------------------------------------
// Erkennt anhand des Hostnamens das passende Profil, laedt die
// Einstellungen und baut das Overlay. Laeuft als letztes Skript.
// ============================================================
(async () => {
	const OV = window.__chromeOverlays__;
	if (!OV || OV.__booted) return;

	const host = location.hostname;
	const profile = OV.findProfile(host);
	if (!profile) return; // diese Seite hat kein Overlay-Profil

	OV.__booted = true;
	OV.activeProfile = profile;

	await OV.storage.load();

	function boot() {
		if (!document.body) {
			setTimeout(boot, 300);
			return;
		}
		try {
			OV.ui.buildOverlay(profile);
			OV.ui.startWatchdog();
		} catch (e) {
			console.error("[Overlays] boot:", e);
		}

		const missing = [];
		if (!OV.storage.get("groqKey", "")) missing.push("Groq/Whisper");
		if (profile.gemini && !OV.storage.get("geminiKey", ""))
			missing.push("Gemini");

		OV.toast(
			`✅ Overlay aktiv (${profile.label}).` +
				(missing.length
					? `\n⚠️ Fehlende API-Keys: ${missing.join(", ")}\nErweiterungs-Icon anklicken zum Eintragen.`
					: "\nTipp: erst ins Eingabefeld klicken, dann 🎙️."),
			4500,
		);
	}

	setTimeout(boot, 350);
})();
