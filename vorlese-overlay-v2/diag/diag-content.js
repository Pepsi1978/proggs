/*
 * diag-content.js — Diagnose-Bruecke fuer das Content-Script (klassisches
 * Script, KEIN ES-Modul). Wird im manifest VOR settings.js/overlay.js geladen
 * und stellt window.VODiag bereit (analog zu window.VOSettings).
 *
 * Warum getrennt von diag.js: Das Content-Script laeuft im Ursprung der
 * Webseite und hat KEINEN Zugriff auf die IndexedDB der Erweiterung. Es baut
 * den Eintrag deshalb selbst (gleiche Felder + Maskierung wie diag.js) und
 * schickt ihn per chrome.runtime.sendMessage an den Service-Worker, der ihn
 * zentral ablegt (diag.ingest).
 *
 * Schalter: identisch zu diag.js — chrome.storage.local["vo_diag"]. Aus =
 * sofortiger No-Op (keine Mehrlast, kein veraendertes Verhalten).
 */
(function () {
	"use strict";

	if (window.VODiag) return; // Doppel-Injektion vermeiden (Idempotenz)

	var DIAG_FLAG = "vo_diag";
	var DIAG_LOG_MSG = "VO_DIAG_LOG";
	var COMPONENT = "content";

	var STUFE = {
		INFO: "INFO",
		WARN: "WARN",
		ERROR: "ERROR",
		FATAL: "FATAL",
	};
	var KATEGORIE = {
		FUNKTION: "FUNKTION",
		UI_EREIGNIS: "UI_EREIGNIS",
		LAYOUT: "LAYOUT",
		ZUSTAND: "ZUSTAND",
		PERFORMANCE: "PERFORMANCE",
		FEHLER: "FEHLER",
		TEST: "TEST",
		NUTZUNG: "NUTZUNG",
	};

	var enabled = false; // schneller No-Op-Schalter
	var sessionId = shortId();
	var appVersion = "0.0.0";
	var EXT_ORIGIN = "";

	function shortId() {
		try {
			return crypto.randomUUID().slice(0, 8);
		} catch (e) {
			return Math.floor(Math.random() * 0xffffffff).toString(16);
		}
	}

	function nowIso() {
		return new Date().toISOString();
	}

	// ----- Maskierung (eigene, schlanke Kopie der Logik aus diag.js) ----------
	var SECRET_KEYS =
		/(api[_-]?key|key|token|secret|password|passwort|authorization|auth)/i;

	function mask(value, depth) {
		var d = typeof depth === "number" ? depth : 0;
		if (value == null) return value;
		if (typeof value === "string") return maskString(value);
		if (typeof value === "number" || typeof value === "boolean") return value;
		if (d > 4) return "[…tief verschachtelt]";
		if (Array.isArray(value)) {
			var max = 50;
			var arr = value.slice(0, max).map(function (v) {
				return mask(v, d + 1);
			});
			if (value.length > max)
				arr.push("…(" + (value.length - max) + " weitere)");
			return arr;
		}
		if (typeof value === "object") {
			if (value instanceof ArrayBuffer)
				return "[ArrayBuffer " + value.byteLength + "B]";
			if (ArrayBuffer.isView(value))
				return (
					"[" +
					(value.constructor && value.constructor.name) +
					" " +
					value.byteLength +
					"B]"
				);
			var out = {};
			var keys = Object.keys(value);
			for (var i = 0; i < keys.length; i++) {
				var k = keys[i];
				out[k] = SECRET_KEYS.test(k) ? redact(value[k]) : mask(value[k], d + 1);
			}
			return out;
		}
		return String(value);
	}

	function maskString(s) {
		var out = s;
		out = out.replace(/AIza[0-9A-Za-z_\-]{20,}/g, "AIza…<maskiert>");
		out = out.replace(/key=([0-9A-Za-z_\-]{12,})/gi, "key=<maskiert>");
		if (/^data:/.test(out) && out.length > 80) {
			return out.slice(0, 40) + "…(data-url, " + out.length + " Zeichen)";
		}
		if (out.length > 600)
			return out.slice(0, 600) + "…(" + out.length + " Zeichen)";
		return out;
	}

	function redact(v) {
		// Nur Strings maskieren. Booleans/Zahlen sind keine Geheimnisse — sonst
		// wuerde z.B. ein Flag wie "hatGoogleKey" maskiert, nur weil sein Name
		// "key" enthaelt (die Regex matcht den Schluessel, nicht den Wert).
		if (typeof v !== "string") return v;
		if (v === "") return v;
		if (v.length <= 4) return "<maskiert>";
		return "<maskiert:" + v.length + ">";
	}

	function buildEntry(stufe, kategorie, ereignis, inhalt, korrelationId) {
		return {
			zeitstempel: nowIso(),
			stufe: stufe || STUFE.INFO,
			kategorie: kategorie || KATEGORIE.FUNKTION,
			komponente: COMPONENT,
			ereignis: String(ereignis || ""),
			inhalt: inhalt === undefined ? {} : mask(inhalt),
			sitzung_id: sessionId,
			korrelation_id: korrelationId || null,
			app_version: appVersion,
		};
	}

	function mirrorConsole(entry) {
		var head =
			entry.stufe +
			" " +
			entry.kategorie +
			" " +
			entry.komponente +
			" :: " +
			entry.ereignis;
		try {
			if (entry.stufe === STUFE.ERROR || entry.stufe === STUFE.FATAL)
				console.error("[VO-DIAG]", head, entry.inhalt);
			else if (entry.stufe === STUFE.WARN)
				console.warn("[VO-DIAG]", head, entry.inhalt);
			else console.debug("[VO-DIAG]", head, entry.inhalt);
		} catch (e) {
			/* egal */
		}
	}

	function send(entry) {
		try {
			chrome.runtime.sendMessage(
				{ type: DIAG_LOG_MSG, entry: entry },
				function () {
					// lastError abschoepfen (Worker evtl. gerade inaktiv) — nie werfen
					void (chrome.runtime && chrome.runtime.lastError);
				},
			);
		} catch (e) {
			/* Erweiterung evtl. nicht erreichbar */
		}
	}

	function log(stufe, kategorie, ereignis, inhalt, korrelationId) {
		if (!enabled) return; // No-Op im Aus-Zustand
		try {
			var entry = buildEntry(stufe, kategorie, ereignis, inhalt, korrelationId);
			mirrorConsole(entry);
			send(entry);
		} catch (e) {
			/* Logging darf die Seite nie stoeren */
		}
	}

	// ----- Schalter laden + beobachten ----------------------------------------
	function readFlag() {
		try {
			chrome.storage.local.get([DIAG_FLAG], function (res) {
				// Standardmaessig AN — nur ein explizit gespeichertes false schaltet aus.
				if (chrome.runtime && chrome.runtime.lastError) {
					enabled = true;
					return;
				}
				enabled = !(res && res[DIAG_FLAG] === false);
				if (enabled)
					log(STUFE.INFO, KATEGORIE.ZUSTAND, "diag_init", {
						komponente: COMPONENT,
						url: location && location.href,
					});
			});
		} catch (e) {
			/* ohne Schalter bleibt enabled=false */
		}
	}

	function watchFlag() {
		try {
			chrome.storage.onChanged.addListener(function (changes, area) {
				if (area === "local" && changes[DIAG_FLAG])
					enabled = changes[DIAG_FLAG].newValue !== false;
			});
		} catch (e) {
			/* egal */
		}
	}

	// ----- Gezielte Crash-Handler: NUR Fehler aus unserem Extension-Code ------
	// (window teilt sich mit der Seite -> Seiten-Fehler ausfiltern)
	function installCrashHandlers() {
		try {
			EXT_ORIGIN = chrome.runtime.getURL("");
			appVersion = (chrome.runtime.getManifest() || {}).version || appVersion;
		} catch (e) {
			/* ohne Origin keine Filterung moeglich */
		}
		function isOurs(filename, stack) {
			if (!EXT_ORIGIN) return false;
			if (filename && String(filename).indexOf(EXT_ORIGIN) === 0) return true;
			if (stack && String(stack).indexOf(EXT_ORIGIN) !== -1) return true;
			return false;
		}
		window.addEventListener("error", function (ev) {
			if (!enabled) return;
			if (!isOurs(ev && ev.filename, ev && ev.error && ev.error.stack)) return;
			log(STUFE.FATAL, KATEGORIE.FEHLER, "uncaught_error", {
				message: ev && ev.message,
				quelle: ev && ev.filename,
				zeile: ev && ev.lineno,
				stack: ev && ev.error && ev.error.stack,
			});
		});
		window.addEventListener("unhandledrejection", function (ev) {
			if (!enabled) return;
			var r = ev && ev.reason;
			if (!isOurs(null, r && r.stack)) return;
			log(STUFE.FATAL, KATEGORIE.FEHLER, "unhandled_rejection", {
				message: r && (r.message || String(r)),
				stack: r && r.stack,
			});
		});
	}

	window.VODiag = {
		STUFE: STUFE,
		KATEGORIE: KATEGORIE,
		isEnabled: function () {
			return enabled;
		},
		log: log,
		newCorrelationId: function () {
			return "k" + shortId();
		},
		info: function (kat, ev, inhalt, kid) {
			log(STUFE.INFO, kat, ev, inhalt, kid);
		},
		warn: function (kat, ev, inhalt, kid) {
			log(STUFE.WARN, kat, ev, inhalt, kid);
		},
		error: function (kat, ev, inhalt, kid) {
			log(STUFE.ERROR, kat, ev, inhalt, kid);
		},
		fatal: function (kat, ev, inhalt, kid) {
			log(STUFE.FATAL, kat, ev, inhalt, kid);
		},
	};

	installCrashHandlers();
	watchFlag();
	readFlag();
})();
