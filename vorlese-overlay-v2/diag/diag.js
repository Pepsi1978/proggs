/*
 * diag.js — zentrale Diagnose-/Logging-Schicht (ES-Modul).
 *
 * Wird von Service-Worker und Offscreen-Dokument importiert (beide laufen im
 * Ursprung der Erweiterung und teilen sich damit dieselbe IndexedDB). Das
 * Content-Script kann hier NICHT direkt schreiben (es laeuft im Ursprung der
 * Webseite) — es schickt seine Eintraege per Nachricht an den Service-Worker,
 * der sie ueber ingest() zentral ablegt (siehe diag-content.js).
 *
 * ----- Grundprinzipien (entsprechen der Diagnose-Direktive) ----------------
 *  - EIN zentraler Schalter: chrome.storage.local["vo_diag"]. Standardmaessig AN;
 *    nur ein explizit gespeichertes false schaltet aus. Im Aus-Zustand ist log()
 *    ein sofortiger No-Op (keine Mehrlast) — das Diagnose-System laeuft sonst
 *    dauerhaft mit (Fehler UND Nutzungssignale fuer App-Verbesserungen).
 *  - Rein ADDITIV: keine bestehende Funktion wird veraendert. Schalter aus =
 *    Verhalten exakt wie vorher.
 *  - Ein Eintrag = eine JSONL-Zeile mit festen Feldern (siehe buildEntry()).
 *  - Geheimnisse (Google-API-Key etc.) werden automatisch maskiert.
 *  - Ausgabe doppelt: IndexedDB-Ringpuffer (fuer den Export als .jsonl) UND
 *    Spiegel auf die jeweilige DevTools-Konsole.
 *
 * Eine Browser-Erweiterung darf nicht frei in eine Datei auf der Platte
 * schreiben (Sandbox). Der "feste, dokumentierte Log-Pfad" ist hier deshalb
 * die IndexedDB "vo_diag" / Store "logs"; exportLogs() laedt sie als echte
 * .jsonl-Datei ueber den Download-Ordner herunter.
 */

const DB_NAME = "vo_diag";
const STORE = "logs";
const META_STORE = "meta";
const MAX_ENTRIES = 5000; // Ringpuffer-Obergrenze (aelteste fallen heraus)
const DIAG_FLAG = "vo_diag"; // chrome.storage.local-Schluessel des Schalters
export const DIAG_LOG_MSG = "VO_DIAG_LOG"; // Nachricht Content -> Worker

const STUFE = Object.freeze({
	INFO: "INFO",
	WARN: "WARN",
	ERROR: "ERROR",
	FATAL: "FATAL",
});
const KATEGORIE = Object.freeze({
	FUNKTION: "FUNKTION",
	UI_EREIGNIS: "UI_EREIGNIS",
	LAYOUT: "LAYOUT",
	ZUSTAND: "ZUSTAND",
	PERFORMANCE: "PERFORMANCE",
	FEHLER: "FEHLER",
	TEST: "TEST",
	NUTZUNG: "NUTZUNG", // Nutzungssignale fuer App-Verbesserungsvorschlaege
});

let component = "unbekannt";
let sessionId = shortId();
let enabled = false; // synchron lesbarer Schnell-Schalter (No-Op wenn false)
let initialized = false;
let dbPromise = null;
let appVersion = "0.0.0";

// ---------------------------------------------------------------------------
// Hilfen
// ---------------------------------------------------------------------------
function shortId() {
	try {
		return crypto.randomUUID().slice(0, 8);
	} catch (e) {
		return Math.floor(Math.random() * 0xffffffff).toString(16);
	}
}

function nowIso() {
	// ISO 8601 mit Millisekunden (toISOString liefert genau das).
	return new Date().toISOString();
}

// Geheimnisse maskieren + uebergrosse Werte kuerzen. Arbeitet auf einer Kopie,
// damit die Originaldaten der App unangetastet bleiben.
const SECRET_KEYS =
	/(api[_-]?key|key|token|secret|password|passwort|authorization|auth)/i;
function mask(value, depth) {
	const d = typeof depth === "number" ? depth : 0;
	if (value == null) return value;
	if (typeof value === "string") return maskString(value);
	if (typeof value === "number" || typeof value === "boolean") return value;
	if (d > 4) return "[…tief verschachtelt]";
	if (Array.isArray(value)) {
		const max = 50;
		const out = value.slice(0, max).map((v) => mask(v, d + 1));
		if (value.length > max) out.push("…(" + (value.length - max) + " weitere)");
		return out;
	}
	if (typeof value === "object") {
		// Spezialfaelle, die nicht sinnvoll serialisierbar sind
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
		const out = {};
		for (const k of Object.keys(value)) {
			if (SECRET_KEYS.test(k)) {
				out[k] = redact(value[k]);
			} else {
				out[k] = mask(value[k], d + 1);
			}
		}
		return out;
	}
	return String(value);
}

function maskString(s) {
	let out = s;
	// Google-Cloud-Keys (AIza...) und lange Bearer-artige Tokens maskieren
	out = out.replace(/AIza[0-9A-Za-z_\-]{20,}/g, "AIza…<maskiert>");
	out = out.replace(/key=([0-9A-Za-z_\-]{12,})/gi, "key=<maskiert>");
	// Data-/Audio-URLs nicht in voller Laenge loggen
	if (/^data:/.test(out) && out.length > 80) {
		return out.slice(0, 40) + "…(data-url, " + out.length + " Zeichen)";
	}
	if (out.length > 600)
		return out.slice(0, 600) + "…(" + out.length + " Zeichen)";
	return out;
}

function redact(v) {
	// Nur Strings maskieren. Booleans/Zahlen sind keine Geheimnisse — sonst wuerde
	// z.B. ein harmloses Flag wie "hatGoogleKey" maskiert, nur weil sein Name
	// "key" enthaelt (die Geheimnis-Regex matcht den Schluessel, nicht den Wert).
	if (typeof v !== "string") return v;
	if (v === "") return v;
	if (v.length <= 4) return "<maskiert>";
	return "<maskiert:" + v.length + ">";
}

// ---------------------------------------------------------------------------
// IndexedDB (Ringpuffer)
// ---------------------------------------------------------------------------
function openDb() {
	if (dbPromise) return dbPromise;
	dbPromise = new Promise((resolve, reject) => {
		let req;
		try {
			req = indexedDB.open(DB_NAME, 1);
		} catch (e) {
			reject(e);
			return;
		}
		req.onupgradeneeded = () => {
			const db = req.result;
			if (!db.objectStoreNames.contains(STORE)) {
				db.createObjectStore(STORE, { keyPath: "seq", autoIncrement: true });
			}
			if (!db.objectStoreNames.contains(META_STORE)) {
				db.createObjectStore(META_STORE, { keyPath: "k" });
			}
		};
		req.onsuccess = () => resolve(req.result);
		req.onerror = () => reject(req.error);
	}).catch((e) => {
		// IndexedDB nicht verfuegbar -> kein DB-Logging, nur Konsole.
		dbPromise = null;
		throw e;
	});
	return dbPromise;
}

async function dbAdd(entry) {
	const db = await openDb();
	await new Promise((resolve, reject) => {
		const tx = db.transaction(STORE, "readwrite");
		tx.oncomplete = resolve;
		tx.onerror = () => reject(tx.error);
		tx.objectStore(STORE).add(entry);
	});
	// Ringpuffer: bei Ueberschreitung die aeltesten Eintraege entfernen.
	await trimRing(db);
}

let trimBusy = false;
async function trimRing(db) {
	if (trimBusy) return;
	trimBusy = true;
	try {
		const count = await new Promise((res, rej) => {
			const tx = db.transaction(STORE, "readonly");
			const r = tx.objectStore(STORE).count();
			r.onsuccess = () => res(r.result);
			r.onerror = () => rej(r.error);
		});
		if (count <= MAX_ENTRIES) return;
		const toDelete = count - MAX_ENTRIES;
		await new Promise((res, rej) => {
			const tx = db.transaction(STORE, "readwrite");
			tx.oncomplete = res;
			tx.onerror = () => rej(tx.error);
			const store = tx.objectStore(STORE);
			const cur = store.openCursor(); // aufsteigend nach seq = aelteste zuerst
			let removed = 0;
			cur.onsuccess = (ev) => {
				const c = ev.target.result;
				if (!c || removed >= toDelete) return;
				c.delete();
				removed++;
				c.continue();
			};
		});
	} catch (e) {
		/* Trim ist Best-Effort */
	} finally {
		trimBusy = false;
	}
}

// ---------------------------------------------------------------------------
// Schalter (chrome.storage.local["vo_diag"])
// ---------------------------------------------------------------------------
function readFlag() {
	return new Promise((resolve) => {
		try {
			chrome.storage.local.get([DIAG_FLAG], (res) => {
				// Standardmaessig AN — nur ein explizit gespeichertes false schaltet aus.
				if (chrome.runtime && chrome.runtime.lastError) return resolve(true);
				resolve(!(res && res[DIAG_FLAG] === false));
			});
		} catch (e) {
			resolve(true);
		}
	});
}

function watchFlag() {
	try {
		chrome.storage.onChanged.addListener((changes, area) => {
			if (area === "local" && changes[DIAG_FLAG]) {
				// Default an: nur ein explizites false schaltet aus.
				enabled = changes[DIAG_FLAG].newValue !== false;
			}
		});
	} catch (e) {
		/* in manchen Kontexten evtl. nicht verfuegbar */
	}
}

// ---------------------------------------------------------------------------
// Eintrag bauen + schreiben
// ---------------------------------------------------------------------------
function buildEntry(stufe, kategorie, ereignis, inhalt, korrelationId) {
	return {
		zeitstempel: nowIso(),
		stufe: stufe || STUFE.INFO,
		kategorie: kategorie || KATEGORIE.FUNKTION,
		komponente: component,
		ereignis: String(ereignis || ""),
		inhalt: inhalt === undefined ? {} : mask(inhalt),
		sitzung_id: sessionId,
		korrelation_id: korrelationId || null,
		app_version: appVersion,
	};
}

function mirrorConsole(entry) {
	const tag = "[VO-DIAG]";
	const head =
		entry.stufe +
		" " +
		entry.kategorie +
		" " +
		entry.komponente +
		" :: " +
		entry.ereignis;
	try {
		if (entry.stufe === STUFE.ERROR || entry.stufe === STUFE.FATAL) {
			console.error(tag, head, entry.inhalt);
		} else if (entry.stufe === STUFE.WARN) {
			console.warn(tag, head, entry.inhalt);
		} else {
			console.debug(tag, head, entry.inhalt);
		}
	} catch (e) {
		/* Konsole evtl. nicht verfuegbar */
	}
}

// Schreibt einen FERTIGEN Eintrag (DB + Konsole). Wird auch von ingest()
// fuer Eintraege benutzt, die das Content-Script geschickt hat.
function writeEntry(entry) {
	mirrorConsole(entry);
	// DB-Schreiben ist async und darf den Aufrufer nie blockieren oder werfen.
	dbAdd(entry).catch(() => {
		/* DB nicht verfuegbar -> Konsole war bereits die Sicherung */
	});
}

// ---------------------------------------------------------------------------
// Oeffentliche API
// ---------------------------------------------------------------------------
function log(stufe, kategorie, ereignis, inhalt, korrelationId) {
	if (!enabled) return; // Schneller No-Op im Aus-Zustand
	try {
		writeEntry(buildEntry(stufe, kategorie, ereignis, inhalt, korrelationId));
	} catch (e) {
		/* Logging darf die App niemals stoeren */
	}
}

// Vom Content-Script empfangene Eintraege zentral ablegen. Der Eintrag ist
// bereits fertig formatiert + maskiert; hier wird er nur noch geschrieben.
function ingest(entry) {
	if (!enabled) return;
	if (!entry || typeof entry !== "object") return;
	try {
		writeEntry(entry);
	} catch (e) {
		/* nie stoeren */
	}
}

function installCrashHandlers() {
	try {
		self.addEventListener("error", (ev) => {
			log(STUFE.FATAL, KATEGORIE.FEHLER, "uncaught_error", {
				message: ev && ev.message,
				quelle: ev && ev.filename,
				zeile: ev && ev.lineno,
				spalte: ev && ev.colno,
				stack: ev && ev.error && ev.error.stack,
			});
		});
		self.addEventListener("unhandledrejection", (ev) => {
			const r = ev && ev.reason;
			log(STUFE.FATAL, KATEGORIE.FEHLER, "unhandled_rejection", {
				message: r && (r.message || String(r)),
				stack: r && r.stack,
			});
		});
	} catch (e) {
		/* in manchen Kontexten kein self.addEventListener */
	}
}

async function init(componentName, opts) {
	component = componentName || component;
	try {
		appVersion = (chrome.runtime.getManifest() || {}).version || appVersion;
	} catch (e) {
		/* ohne Manifest weiter */
	}
	if (!initialized) {
		initialized = true;
		watchFlag();
		if (!opts || opts.crashHandlers !== false) installCrashHandlers();
	}
	enabled = await readFlag();
	// Startmarke (nur wenn aktiv) — macht jede Sitzung im Log auffindbar.
	log(STUFE.INFO, KATEGORIE.ZUSTAND, "diag_init", {
		komponente: component,
		aktiv: enabled,
	});
	return enabled;
}

function isEnabled() {
	return enabled;
}

function newCorrelationId() {
	return "k" + shortId();
}

// Alle Eintraege als JSONL-Text (eine Zeile pro Eintrag, nach seq sortiert).
async function readAllJsonl() {
	const db = await openDb();
	const rows = await new Promise((resolve, reject) => {
		const tx = db.transaction(STORE, "readonly");
		const r = tx.objectStore(STORE).getAll();
		r.onsuccess = () => resolve(r.result || []);
		r.onerror = () => reject(r.error);
	});
	rows.sort((a, b) => (a.seq || 0) - (b.seq || 0));
	return rows
		.map((row) => {
			const { seq, ...rest } = row;
			return JSON.stringify(rest);
		})
		.join("\n");
}

// Export als echte .jsonl-Datei ueber den Download-Ordner (nur Worker:
// chrome.downloads ist im Content-/Offscreen-Kontext nicht verfuegbar).
async function exportLogs() {
	const jsonl = await readAllJsonl();
	const dataUrl =
		"data:application/x-ndjson;base64," +
		btoa(unescape(encodeURIComponent(jsonl)));
	const stamp = nowIso().replace(/[:.]/g, "-");
	const filename = "vorlese-overlay-diag-" + stamp + ".jsonl";
	return await new Promise((resolve, reject) => {
		try {
			chrome.downloads.download(
				{ url: dataUrl, filename, saveAs: false },
				(id) => {
					if (chrome.runtime.lastError)
						reject(new Error(chrome.runtime.lastError.message));
					else resolve({ id, filename });
				},
			);
		} catch (e) {
			reject(e);
		}
	});
}

async function clearLogs() {
	try {
		const db = await openDb();
		await new Promise((res, rej) => {
			const tx = db.transaction(STORE, "readwrite");
			tx.oncomplete = res;
			tx.onerror = () => rej(tx.error);
			tx.objectStore(STORE).clear();
		});
	} catch (e) {
		/* egal */
	}
}

async function getCount() {
	try {
		const db = await openDb();
		return await new Promise((res, rej) => {
			const tx = db.transaction(STORE, "readonly");
			const r = tx.objectStore(STORE).count();
			r.onsuccess = () => res(r.result);
			r.onerror = () => rej(r.error);
		});
	} catch (e) {
		return 0;
	}
}

export const diag = {
	STUFE,
	KATEGORIE,
	init,
	isEnabled,
	log,
	ingest,
	newCorrelationId,
	exportLogs,
	clearLogs,
	getCount,
	readAllJsonl,
	// Komfort-Kuerzel
	info: (kat, ev, inhalt, kid) => log(STUFE.INFO, kat, ev, inhalt, kid),
	warn: (kat, ev, inhalt, kid) => log(STUFE.WARN, kat, ev, inhalt, kid),
	error: (kat, ev, inhalt, kid) => log(STUFE.ERROR, kat, ev, inhalt, kid),
	fatal: (kat, ev, inhalt, kid) => log(STUFE.FATAL, kat, ev, inhalt, kid),
};

export { STUFE, KATEGORIE };
