/*
 * selbsttest.mjs — vollautomatischer Playwright-Selbsttest des Vorlese-Overlays.
 *
 * Ablauf:
 *   1. Chromium MIT geladener (entpackter) Erweiterung starten — headed,
 *      weil MV3-Erweiterungen in echtem Chromium am zuverlaessigsten headed laufen.
 *   2. Diagnose-Modus einschalten + alte Logs leeren.
 *   3. Eine Testseite oeffnen (Content-Scripts + Overlay laden lassen).
 *   4. Den In-App-Runner ausloesen (VO_SELFTEST_RUN -> der Worker schickt
 *      VO_SELFTEST_BEGIN an den aktiven Tab; der Runner bedient das Overlay).
 *   5. Auf Abschluss warten (der Runner braucht ~25 s wegen echter Synthese).
 *   6. Die gesammelten Diagnose-Logs aus der Extension-IndexedDB auslesen und
 *      als tests/selbsttest-log.jsonl schreiben.
 *   7. Kurzauswertung: FEHLER/FATAL + Layout-Markierungen zaehlen. Exit-Code 0
 *      wenn sauber, sonst 1 (fuer die "zwei stille Schleifen"-Iteration).
 *
 * Start:  cd vorlese-overlay-v2/tests && npm install && npm run selbsttest
 * Optional:  VO_TESTSEITE=https://de.wikipedia.org/wiki/Test  npm run selbsttest
 */
import { chromium } from "playwright";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { writeFileSync, rmSync } from "node:fs";

const __dirname = dirname(fileURLToPath(import.meta.url));
const EXT_PFAD = resolve(__dirname, ".."); // -> vorlese-overlay-v2/
const USER_DIR = resolve(__dirname, ".pw-profile");
const LOG_OUT = resolve(__dirname, "selbsttest-log.jsonl");
const TESTSEITE = process.env.VO_TESTSEITE || "https://example.com";
const WARTE_RUNNER_MS = Number(process.env.VO_WARTE_MS || 30000);

function log(...a) {
	console.log("[selbsttest]", ...a);
}

async function findServiceWorker(ctx, timeoutMs) {
	const vorhanden = ctx.serviceWorkers();
	if (vorhanden.length) return vorhanden[0];
	return await ctx.waitForEvent("serviceworker", {
		timeout: timeoutMs || 15000,
	});
}

let ctx;
let exitCode = 0;
try {
	log("Starte Chromium mit geladener Erweiterung:", EXT_PFAD);
	// Frisches Profil erzwingen: ein persistentes Profil cacht den MV3-Service-
	// Worker und wuerde sonst veralteten SW-Code aus einem frueheren Lauf testen.
	try {
		rmSync(USER_DIR, { recursive: true, force: true });
	} catch (e) {
		/* erster Lauf: Profil existiert noch nicht */
	}
	ctx = await chromium.launchPersistentContext(USER_DIR, {
		headless: false,
		args: [
			`--disable-extensions-except=${EXT_PFAD}`,
			`--load-extension=${EXT_PFAD}`,
			"--autoplay-policy=no-user-gesture-required",
		],
	});

	const sw = await findServiceWorker(ctx, 20000);
	log("Service-Worker gefunden.");

	// Diagnose-Modus an + alte Logs leeren
	await sw.evaluate(
		() => new Promise((r) => chrome.storage.local.set({ vo_diag: true }, r)),
	);
	await sw.evaluate(async () => {
		if (globalThis.__voDiag) await globalThis.__voDiag.clearLogs();
	});
	log("Diagnose-Modus an, Logs geleert.");

	// Testseite oeffnen + Overlay/Content-Scripts laden lassen
	const page = await ctx.newPage();
	await page.goto(TESTSEITE, { waitUntil: "domcontentloaded" });
	await page.waitForTimeout(2000);
	log("Testseite geladen:", TESTSEITE);

	// Selbsttest ausloesen: VO_SELFTEST_BEGIN DIREKT an den aktiven Tab senden.
	// (chrome.runtime.sendMessage aus dem SW erreicht den SW-eigenen onMessage-
	//  Listener NICHT — daher kein Umweg ueber VO_SELFTEST_RUN, sondern direkt
	//  tabs.sendMessage an das Content-Script.)
	await sw.evaluate(
		() =>
			new Promise((res) => {
				chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
					const tab = tabs && tabs[0];
					if (tab && tab.id != null) {
						chrome.tabs.sendMessage(
							tab.id,
							{ type: "VO_SELFTEST_BEGIN" },
							() => {
								void chrome.runtime.lastError;
								res();
							},
						);
					} else {
						res();
					}
				});
			}),
	);
	log(
		`Selbsttest ausgeloest — warte ${Math.round(WARTE_RUNNER_MS / 1000)} s auf Abschluss...`,
	);
	await page.waitForTimeout(WARTE_RUNNER_MS);

	// Logs auslesen
	const jsonl = await sw.evaluate(async () =>
		globalThis.__voDiag ? await globalThis.__voDiag.readAllJsonl() : "",
	);
	writeFileSync(LOG_OUT, jsonl, "utf8");
	const zeilen = jsonl.split("\n").filter(Boolean);
	log("Logs geschrieben:", LOG_OUT, `(${zeilen.length} Eintraege)`);

	// App-Verbesserungsvorschlaege aus den frischen Daten ableiten
	const insights = await sw.evaluate(async () =>
		globalThis.__voInsights ? await globalThis.__voInsights() : null,
	);
	if (insights) {
		log("=== App-Verbesserungsvorschlaege ===");
		log("Kennzahlen:", JSON.stringify(insights.kennzahlen));
		for (const v of insights.vorschlaege) {
			log(`  [${v.prioritaet}] ${v.thema}: ${v.vorschlag}`);
		}
	}

	// Kurzauswertung
	let fehler = 0;
	let fatal = 0;
	let layoutWarn = 0;
	for (const z of zeilen) {
		try {
			const e = JSON.parse(z);
			if (e.stufe === "FATAL") fatal++;
			else if (e.stufe === "ERROR") fehler++;
			if (
				e.kategorie === "LAYOUT" &&
				/teilweise_ausserhalb|ueberlauf/.test(e.ereignis)
			)
				layoutWarn++;
		} catch (e) {
			/* defekte Zeile ignorieren */
		}
	}
	log("=== Auswertung ===");
	log(
		"FATAL:",
		fatal,
		"| ERROR:",
		fehler,
		"| LAYOUT-Markierungen:",
		layoutWarn,
	);
	if (fatal > 0 || fehler > 0 || layoutWarn > 0) {
		exitCode = 1;
		log(
			"-> Es gibt Befunde. Details in der .jsonl (Phase 5: auswerten + fixen).",
		);
	} else {
		log("-> Stille Schleife: keine FEHLER/FATAL, keine Layout-Markierungen.");
	}
} catch (e) {
	console.error("[selbsttest] FEHLER:", e && e.stack ? e.stack : e);
	exitCode = 2;
} finally {
	if (ctx) await ctx.close();
}
process.exit(exitCode);
