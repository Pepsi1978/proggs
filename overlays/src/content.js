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

	function normalizeHostList(list) {
		return Array.isArray(list)
			? Array.from(
					new Set(
						list
							.map((x) => String(x || "").toLowerCase().trim())
							.filter(Boolean),
					),
				)
			: [];
	}

	function pageDisabled() {
		return normalizeHostList(OV.storage.get("ovDisabledHosts", [])).includes(
			host.toLowerCase(),
		);
	}

	try {
		chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
			if (!msg || msg.type !== "OV_GET_PAGE_STATE") return;
			sendResponse({
				host: host.toLowerCase(),
				profile: profile.label,
				disabled: pageDisabled(),
			});
		});
	} catch (e) {
		console.warn("[Overlays] page-state listener:", e);
	}

	try {
		chrome.storage.onChanged.addListener(async (changes, area) => {
			if (area !== "local" || !changes.ovDisabledHosts) return;
			await OV.storage.load();
			if (pageDisabled()) OV.ui.removeOverlay(profile);
			else boot();
		});
	} catch (e) {
		console.warn("[Overlays] disabled-host listener:", e);
	}

	function boot() {
		if (!document.body) {
			setTimeout(boot, 300);
			return;
		}
		if (pageDisabled()) {
			OV.ui.removeOverlay(profile);
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
