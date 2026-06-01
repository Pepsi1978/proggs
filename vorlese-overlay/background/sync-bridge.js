// ============================================================
// sync-bridge.js — geraeteuebergreifende Synchronisierung (ES-Modul)
// ------------------------------------------------------------
// Spiegelt ausgewaehlte chrome.storage.local-Keys verlustfrei nach
// chrome.storage.sync (Googles geraeteuebergreifendes Backup) und zurueck.
// Dasselbe bewaehrte Muster wie beim Prompt-Board — aber zentral hier im
// Service-Worker, damit ALLE Schreibpfade (Einstellungs-Seitenleiste UND das
// Overlay-Content-Script) automatisch erfasst werden, ohne die UI anzufassen.
//
// WIE ES FUNKTIONIERT
//   - chrome.storage.local bleibt die zuverlaessige Quelle DIESES Geraets.
//   - chrome.storage.sync ist Googles Cloud-Backup fuer den Geraete-Abgleich.
//   - Aendert sich lokal etwas (Whitelist), wird es in die Cloud gespiegelt.
//   - Aendert ein anderes Geraet die Cloud, wird es nach local gespiegelt —
//     wodurch der bestehende local-onChanged-Listener (content/overlay.js,
//     Zeile ~491) die Oberflaeche live aktualisiert. Kein UI-Eingriff noetig.
//
// ECHO-FREI durch WERTE-VERGLEICH (kein Flag-Timing): geschrieben wird nur,
// wenn sich der Zielwert WIRKLICH unterscheidet. Ein Spiegelvorgang loest so
// nie einen zweiten aus (danach sind die Werte gleich -> kein weiterer Write).
//
// LAST-WRITE-WINS: ein Zeitstempel (__sync_ts) entscheidet nur den seltenen
// echten Konflikt (beide Geraete offline unterschiedlich geaendert).
//
// WHITELIST: Nur "vo_settings" wird synchronisiert (Engine, Stimmen, Tempo,
// Pitch, autoSpeak, autoMode, Favoriten, draggable und der Google-API-Key).
// BEWUSST NICHT dabei: vo_position / vo_panel_position — die Overlay-Position
// bleibt pro Geraet lokal, weil verschiedene Bildschirmgroessen sich sonst
// stoeren wuerden.
// ============================================================

// "vo_settings" ist EIN kompaktes Objekt (klar < 8 KB) -> kein Pro-Item-Splitting
// noetig, anders als beim Prompt-Board mit seiner langen Prompt-Liste.
const SYNC_KEYS = ["vo_settings"];
const TS = "__sync_ts";
const PUSH_DEBOUNCE_MS = 800;

const eq = (a, b) => JSON.stringify(a) === JSON.stringify(b);

function getArea(area, keys) {
	return new Promise((resolve) => {
		try {
			chrome.storage[area].get(keys, (v) => {
				resolve(chrome.runtime.lastError ? {} : v || {});
			});
		} catch {
			resolve({});
		}
	});
}

function setArea(area, obj) {
	return new Promise((resolve) => {
		try {
			chrome.storage[area].set(obj, () => {
				if (chrome.runtime.lastError) {
					console.warn(
						"[Vorlese/Sync] " + area + ".set:",
						chrome.runtime.lastError.message,
					);
				}
				resolve();
			});
		} catch (e) {
			console.warn("[Vorlese/Sync] " + area + ".set:", e);
			resolve();
		}
	});
}

// Lokale Aenderungen -> Cloud (nur geaenderte Werte, mit frischem Zeitstempel).
async function pushToSync() {
	const [loc, syn] = await Promise.all([
		getArea("local", SYNC_KEYS),
		getArea("sync", SYNC_KEYS),
	]);
	const diff = {};
	for (const k of SYNC_KEYS) {
		if (loc[k] !== undefined && !eq(loc[k], syn[k])) diff[k] = loc[k];
	}
	if (!Object.keys(diff).length) return; // nichts Neues -> kein Echo
	const ts = Date.now();
	diff[TS] = ts;
	await setArea("sync", diff);
	// Eigenen Zeitstempel mitfuehren (TS ist nicht in SYNC_KEYS -> kein Echo).
	await setArea("local", { [TS]: ts });
}

// Cloud-Aenderungen -> local (loest den bestehenden local-onChanged-Listener
// im Overlay aus -> Oberflaeche aktualisiert sich live).
async function pullFromSync() {
	const [syn, loc] = await Promise.all([
		getArea("sync", [...SYNC_KEYS, TS]),
		getArea("local", [...SYNC_KEYS, TS]),
	]);
	const syncTs = Number(syn[TS]) || 0;
	const localTs = Number(loc[TS]) || 0;
	const diff = {};
	for (const k of SYNC_KEYS) {
		if (syn[k] !== undefined && !eq(syn[k], loc[k])) diff[k] = syn[k];
	}
	if (!Object.keys(diff).length) {
		// Werte gleich — nur den Zeitstempel angleichen, falls veraltet.
		if (syncTs > localTs) await setArea("local", { [TS]: syncTs });
		return;
	}
	if (syncTs >= localTs) {
		// Cloud ist (mindestens) so aktuell -> uebernehmen. Last-write-wins.
		diff[TS] = syncTs;
		await setArea("local", diff);
	} else {
		// Lokal ist nachweislich neuer -> Cloud korrigieren statt ueberschreiben.
		await pushToSync();
	}
}

// Boot-Abgleich: beim Start des Service-Workers einmal abgleichen.
async function reconcile() {
	const [syn, loc] = await Promise.all([
		getArea("sync", [...SYNC_KEYS, TS]),
		getArea("local", [...SYNC_KEYS, TS]),
	]);
	const syncTs = Number(syn[TS]) || 0;
	const localTs = Number(loc[TS]) || 0;
	const syncHasData = SYNC_KEYS.some((k) => syn[k] !== undefined);
	if (syncHasData && syncTs > localTs) {
		await pullFromSync(); // anderes Geraet war zuletzt dran
	} else {
		await pushToSync(); // lokaler Stand ist (mindestens) so aktuell
	}
}

let pushTimer = null;
function schedulePush() {
	clearTimeout(pushTimer);
	pushTimer = setTimeout(() => {
		pushToSync().catch((e) => console.warn("[Vorlese/Sync] push:", e));
	}, PUSH_DEBOUNCE_MS);
}

try {
	chrome.storage.onChanged.addListener((changes, area) => {
		if (area === "local") {
			if (SYNC_KEYS.some((k) => k in changes)) schedulePush();
		} else if (area === "sync") {
			if (SYNC_KEYS.some((k) => k in changes) || TS in changes) {
				pullFromSync().catch((e) => console.warn("[Vorlese/Sync] pull:", e));
			}
		}
	});
} catch (e) {
	console.warn("[Vorlese/Sync] onChanged-Listener:", e);
}

try {
	chrome.runtime.onInstalled.addListener(() => reconcile());
	chrome.runtime.onStartup.addListener(() => reconcile());
} catch {}

// Auch beim normalen Aufwachen des Service-Workers einmal abgleichen.
reconcile().catch((e) => console.warn("[Vorlese/Sync] reconcile:", e));
