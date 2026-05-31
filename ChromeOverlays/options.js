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

// ── Version & Aktualisieren ──
const REMOTE_MANIFEST =
	"https://raw.githubusercontent.com/Pepsi1978/proggs/main/ChromeOverlays/manifest.json";

function cmpVersion(a, b) {
	const pa = String(a).split(".").map(Number);
	const pb = String(b).split(".").map(Number);
	for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
		const x = pa[i] || 0;
		const y = pb[i] || 0;
		if (x !== y) return x - y;
	}
	return 0;
}

async function checkUpdate() {
	const el = document.getElementById("updstatus");
	el.textContent = " — pruefe…";
	el.style.color = "#94a3b8";
	try {
		const r = await fetch(REMOTE_MANIFEST, { cache: "no-store" });
		if (!r.ok) throw new Error("HTTP " + r.status);
		const remote = JSON.parse(await r.text());
		const local = chrome.runtime.getManifest().version;
		if (cmpVersion(remote.version, local) > 0) {
			el.textContent = ` — neue Version ${remote.version} verfuegbar!`;
			el.style.color = "#fbbf24";
		} else {
			el.textContent = " — du hast die neueste Version.";
			el.style.color = "#4ade80";
		}
	} catch (e) {
		el.textContent = " — Pruefung fehlgeschlagen.";
		el.style.color = "#f87171";
		console.warn("[Overlays] update-check:", e);
	}
}

document.getElementById("ver").textContent =
	chrome.runtime.getManifest().version;
document.getElementById("check").addEventListener("click", checkUpdate);
document.getElementById("reload").addEventListener("click", () => {
	chrome.runtime.reload();
});

load();
