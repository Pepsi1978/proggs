// ============================================================
// sync-bridge.js — geraeteuebergreifende Synchronisierung
// ------------------------------------------------------------
// Spiegelt ausgewaehlte chrome.storage.local-Keys verlustfrei nach
// chrome.storage.sync (Googles geraeteuebergreifendes Backup) und zurueck.
// Dasselbe bewaehrte Muster wie beim Prompt-Board — aber zentral hier im
// Service-Worker, damit ALLE Schreibpfade (Options-Seitenleiste UND das
// Overlay-Content-Script) automatisch erfasst werden, ohne die UI anzufassen.
//
// WIE ES FUNKTIONIERT
//   - chrome.storage.local bleibt die zuverlaessige Quelle DIESES Geraets.
//   - chrome.storage.sync ist Googles Cloud-Backup fuer den Geraete-Abgleich.
//   - Aendert sich lokal etwas (Whitelist), wird es in die Cloud gespiegelt.
//   - Aendert ein anderes Geraet die Cloud, wird es nach local gespiegelt —
//     wodurch der bestehende local-onChanged-Listener (ui.js) die Oberflaeche
//     live aktualisiert. Kein Eingriff in die UI noetig.
//
// ECHO-FREI durch WERTE-VERGLEICH (kein Flag-Timing): geschrieben wird nur,
// wenn sich der Zielwert WIRKLICH unterscheidet. Ein Spiegelvorgang loest so
// nie einen zweiten aus (danach sind die Werte gleich -> kein weiterer Write).
//
// LAST-WRITE-WINS: ein Zeitstempel (__sync_ts) entscheidet nur den seltenen
// echten Konflikt (beide Geraete offline unterschiedlich geaendert).
//
// WHITELIST: Nur die unten gelisteten Keys werden synchronisiert. Alles andere
// (Overlay-Position/Drag-Versatz, transiente Reload-Flags) bleibt bewusst LOKAL
// pro Geraet — verschiedene Bildschirmgroessen sollen sich nicht stoeren.
// ============================================================

(() => {
	// Nur die echten Einstellungen — API-Keys eingeschlossen (vom Benutzer so
	// gewuenscht). NICHT dabei: Positions-/Drag-Versatz-Keys, ov_reload_tabs_after_update,
	// pendingReloadTabId (alle transient bzw. geraetespezifisch).
	const SYNC_KEYS = [
		"groqKey",
		"geminiKey",
		"geminiModel",
		"autoGeminiCorrection",
		"whisperModel",
		"whisperLang",
		"ovDraggable",
	];
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
							"[Overlays/Sync] " + area + ".set:",
							chrome.runtime.lastError.message,
						);
					}
					resolve();
				});
			} catch (e) {
				console.warn("[Overlays/Sync] " + area + ".set:", e);
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

	// Cloud-Aenderungen -> local (loest die bestehenden local-onChanged-Listener
	// in der UI aus -> Oberflaeche aktualisiert sich live).
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
			pushToSync().catch((e) => console.warn("[Overlays/Sync] push:", e));
		}, PUSH_DEBOUNCE_MS);
	}

	try {
		chrome.storage.onChanged.addListener((changes, area) => {
			if (area === "local") {
				if (SYNC_KEYS.some((k) => k in changes)) schedulePush();
			} else if (area === "sync") {
				if (SYNC_KEYS.some((k) => k in changes) || TS in changes) {
					pullFromSync().catch((e) => console.warn("[Overlays/Sync] pull:", e));
				}
			}
		});
	} catch (e) {
		console.warn("[Overlays/Sync] onChanged-Listener:", e);
	}

	try {
		chrome.runtime.onInstalled.addListener(() => reconcile());
		chrome.runtime.onStartup.addListener(() => reconcile());
	} catch {}

	// Auch beim normalen Aufwachen des Service-Workers einmal abgleichen.
	reconcile().catch((e) => console.warn("[Overlays/Sync] reconcile:", e));
})();
