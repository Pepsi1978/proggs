// ============================================================
// options.js — Einstellungsseite (API-Keys + Optionen)
// Ersetzt GM_registerMenuCommand aus den Tampermonkey-Skripten.
// ============================================================

const FIELDS = ["groqKey", "geminiKey", "geminiModel"];
const CHECKS = ["autoGeminiCorrection"];

async function load() {
	const all = await chrome.storage.local.get([...FIELDS, ...CHECKS]);
	for (const id of FIELDS) {
		document.getElementById(id).value = all[id] || "";
	}
	// autoGeminiCorrection: Default true
	document.getElementById("autoGeminiCorrection").checked =
		all.autoGeminiCorrection !== false;
}

async function save() {
	const data = {};
	for (const id of FIELDS) data[id] = document.getElementById(id).value.trim();
	for (const id of CHECKS) data[id] = document.getElementById(id).checked;
	await chrome.storage.local.set(data);
	const status = document.getElementById("status");
	status.textContent = "✅ Gespeichert. Seite(n) neu laden zum Aktivieren.";
	setTimeout(() => (status.textContent = ""), 4000);
}

document.getElementById("save").addEventListener("click", save);
load();
