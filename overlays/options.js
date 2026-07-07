// ============================================================
// options.js — Einstellungsseite (API-Keys + Optionen)
// Ersetzt GM_registerMenuCommand aus den Tampermonkey-Skripten.
// ============================================================

// Nur API-Keys/Modell. Die Auto-Korrektur wird ueber den Overlay-Schalter
// (gruenes Haekchen) gesteuert, nicht hier — daher kein Checkbox-Feld mehr.
const FIELDS = ["groqKey", "geminiKey", "geminiModel"];

let currentPageHost = "";

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

async function getActiveTab() {
	try {
		const tabs = await chrome.tabs.query({ active: true, lastFocusedWindow: true });
		if (tabs && tabs[0]) return tabs[0];
	} catch (_) {
		/* Fallback unten */
	}
	try {
		const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
		return tabs && tabs[0] ? tabs[0] : null;
	} catch (_) {
		return null;
	}
}

function hostFromUrl(url) {
	try {
		const u = new URL(url || "");
		if (u.protocol !== "http:" && u.protocol !== "https:") return "";
		return u.hostname.toLowerCase();
	} catch (_) {
		return "";
	}
}

function requestPageState(tabId) {
	return new Promise((resolve) => {
		if (tabId == null) {
			resolve(null);
			return;
		}
		try {
			chrome.tabs.sendMessage(tabId, { type: "OV_GET_PAGE_STATE" }, (res) => {
				if (chrome.runtime.lastError || !res) {
					resolve(null);
					return;
				}
				resolve(res);
			});
		} catch (_) {
			resolve(null);
		}
	});
}

async function loadCurrentPageToggle() {
	const cb = document.getElementById("currentSiteEnabled");
	const hint = document.getElementById("currentSiteHint");
	const tab = await getActiveTab();
	currentPageHost = hostFromUrl(tab && tab.url);
	let profile = "";
	if (tab && tab.id != null) {
		const state = await requestPageState(tab.id);
		if (!currentPageHost && state && state.host) {
			currentPageHost = String(state.host).toLowerCase();
		}
		profile = state && state.profile ? ` (${state.profile})` : "";
	}
	const data = await chrome.storage.local.get("ovDisabledHosts");
	const disabled = normalizeHostList(data.ovDisabledHosts).includes(currentPageHost);
	if (!currentPageHost) {
		cb.checked = false;
		cb.disabled = true;
		hint.textContent = "Für diese aktive Seite ist kein Overlay-Profil erreichbar.";
		return;
	}
	cb.disabled = false;
	cb.checked = !disabled;
	hint.textContent = `Aktuelle Webseite: ${currentPageHost}${profile}`;
}

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

document.getElementById("currentSiteEnabled").addEventListener("change", async (e) => {
	if (!currentPageHost) return;
	const data = await chrome.storage.local.get("ovDisabledHosts");
	const hosts = normalizeHostList(data.ovDisabledHosts);
	const idx = hosts.indexOf(currentPageHost);
	if (e.target.checked && idx >= 0) hosts.splice(idx, 1);
	if (!e.target.checked && idx < 0) hosts.push(currentPageHost);
	await chrome.storage.local.set({ ovDisabledHosts: hosts });
	loadCurrentPageToggle();
});

// ── Overlay verschiebbar (1:1 wie beim Vorlese-Overlay) ──
// Aktiv = Overlay per linker/rechter Maustaste verschiebbar, bleibt wo losgelassen.
// Aus = springt an den Standardort zurueck. Das Content-Script reagiert live.
const dragCb = document.getElementById("ovDraggable");
chrome.storage.local.get("ovDraggable").then(({ ovDraggable }) => {
	dragCb.checked = !!ovDraggable;
});
dragCb.addEventListener("change", (e) => {
	chrome.storage.local.set({ ovDraggable: !!e.target.checked });
});

// ── Aktualisieren ──
document.getElementById("ver").textContent =
	chrome.runtime.getManifest().version_name || chrome.runtime.getManifest().version;

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

// ── Fehlerprotokoll ──
const LOG_KEY = "ov_error_log";
async function refreshLog() {
	const box = document.getElementById("logBox");
	const data = await chrome.storage.local.get(LOG_KEY);
	const log = Array.isArray(data[LOG_KEY]) ? data[LOG_KEY] : [];
	if (!log.length) {
		box.textContent = "Keine Fehler gespeichert.";
		return;
	}
	box.textContent = log
		.slice()
		.reverse()
		.map((e) => {
			const url = e.url ? `\n  ${e.url}` : "";
			return `[${e.ts || "ohne Zeit"}] ${e.level || "warn"}: ${e.msg || ""}${url}`;
		})
		.join("\n\n");
}

document.getElementById("refreshLog").addEventListener("click", refreshLog);
document.getElementById("clearLog").addEventListener("click", async () => {
	await chrome.storage.local.set({ [LOG_KEY]: [] });
	refreshLog();
});

load();
loadCurrentPageToggle();
refreshLog();
