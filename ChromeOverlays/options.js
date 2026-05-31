// ============================================================
// options.js — Einstellungsseite (API-Keys + Optionen)
// Ersetzt GM_registerMenuCommand aus den Tampermonkey-Skripten.
// ============================================================

// Nur API-Keys/Modell. Die Auto-Korrektur wird ueber den Overlay-Schalter
// (gruenes Haekchen) gesteuert, nicht hier — daher kein Checkbox-Feld mehr.
const FIELDS = ["groqKey", "geminiKey", "geminiModel"];

async function load() {
	const all = await chrome.storage.local.get(FIELDS);
	for (const id of FIELDS) {
		document.getElementById(id).value = all[id] || "";
	}
}

async function save() {
	const data = {};
	for (const id of FIELDS) data[id] = document.getElementById(id).value.trim();
	await chrome.storage.local.set(data);
	const status = document.getElementById("status");
	status.textContent = "✅ Gespeichert. Seite(n) neu laden zum Aktivieren.";
	setTimeout(() => (status.textContent = ""), 4000);
}

document.getElementById("save").addEventListener("click", save);

// ── Aktualisieren ──
document.getElementById("ver").textContent =
	chrome.runtime.getManifest().version;

// "Erweiterung neu laden" laedt die Erweiterung mit dem neuesten lokalen Code neu
// UND danach automatisch die offene Seite (aktiver Tab) — kein manuelles F5 mehr.
// Das Flag ueberlebt chrome.runtime.reload(); der Service-Worker liest es beim
// Neustart und laedt die aktiven Tabs neu (siehe background.js).
document.getElementById("reload").addEventListener("click", () => {
	const btn = document.getElementById("reload");
	btn.textContent = "Wird neu geladen…";
	btn.disabled = true;
	chrome.storage.local.set({ ov_reload_tabs_after_update: true }, () => {
		chrome.runtime.reload();
	});
});

load();
