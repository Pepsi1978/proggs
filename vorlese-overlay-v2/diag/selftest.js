/*
 * selftest.js — In-App-Diagnose-Runner (klassisches Content-Script).
 *
 * Bedient das Overlay vollautomatisch und loest dabei ALLE Sonden aus
 * (Funktion, UI_EREIGNIS, LAYOUT, FEHLER). Jeder Schritt wird unter Kategorie
 * TEST mit eigener Korrelations-ID geloggt, damit man ihn den ausgeloesten
 * Folge-Ereignissen zeitlich zuordnen kann.
 *
 * Bedienung NICHT ueber interne Funktionen von overlay.js (die sind gekapselt),
 * sondern ueber echte, simulierte Maus-/Tastatur-Ereignisse auf dem offenen
 * Shadow-DOM — also genau der Weg, den auch ein Nutzer nimmt. Dadurch bleibt
 * overlay.js unangetastet (keine Verschlechterung, kein Test-Hook im Release).
 *
 * Ausloesung: Der Service-Worker schaltet den Diagnose-Modus ein und schickt
 * VO_SELFTEST_BEGIN an den aktiven Tab (siehe service-worker.js). Im
 * Normalbetrieb haengt hier nur ein Message-Listener — kein Effekt.
 */
(function () {
	"use strict";

	if (window.__voSelftestLoaded) return; // Idempotenz
	window.__voSelftestLoaded = true;

	const HOST_ID = "vorlese-overlay-host";

	function sleep(ms) {
		return new Promise((r) => setTimeout(r, ms));
	}

	function kid() {
		try {
			return "t" + crypto.randomUUID().slice(0, 8);
		} catch (e) {
			return "t" + Math.floor(Math.random() * 0xffffffff).toString(16);
		}
	}

	// Diagnose-Logger des Content-Scripts (No-Op falls VODiag fehlt/aus)
	function tlog(stufe, ereignis, inhalt, korr) {
		if (window.VODiag)
			window.VODiag.log(stufe, "TEST", "selftest:" + ereignis, inhalt, korr);
	}

	// Sicherstellen, dass der Diagnose-Modus aktiv ist, bevor wir messen
	// (der Worker schaltet ihn ein; das onChanged-Echo ist asynchron).
	async function waitForDiag(maxMs) {
		const start = Date.now();
		while (Date.now() - start < (maxMs || 1500)) {
			if (window.VODiag && window.VODiag.isEnabled()) return true;
			await sleep(50);
		}
		return !!(window.VODiag && window.VODiag.isEnabled());
	}

	function getShadow() {
		const host = document.getElementById(HOST_ID);
		return host && host.shadowRoot ? host.shadowRoot : null;
	}

	function centerOf(el) {
		const r = el.getBoundingClientRect();
		return {
			x: Math.round(r.left + r.width / 2),
			y: Math.round(r.top + r.height / 2),
		};
	}

	// Normaler Klick (fuer Panel-Bedienelemente mit click-Listenern)
	function click(el) {
		if (!el) return false;
		el.dispatchEvent(
			new MouseEvent("click", {
				bubbles: true,
				composed: true,
				cancelable: true,
			}),
		);
		return true;
	}

	// Klick auf die Overlay-Buttons: das Overlay erkennt einen Klick ueber
	// mousedown (auf dem Button) + mouseup (auf window) OHNE Bewegung.
	function pressButton(btn) {
		if (!btn) return false;
		const c = centerOf(btn);
		btn.dispatchEvent(
			new MouseEvent("mousedown", {
				bubbles: true,
				composed: true,
				cancelable: true,
				button: 0,
				clientX: c.x,
				clientY: c.y,
			}),
		);
		window.dispatchEvent(
			new MouseEvent("mouseup", { bubbles: true, button: 0 }),
		);
		return true;
	}

	// Drag: mousedown auf Container + Bewegung > Schwelle + mouseup
	function dragBy(container, dx, dy) {
		const c = centerOf(container);
		container.dispatchEvent(
			new MouseEvent("mousedown", {
				bubbles: true,
				composed: true,
				cancelable: true,
				button: 0,
				clientX: c.x,
				clientY: c.y,
			}),
		);
		for (let i = 1; i <= 3; i++) {
			window.dispatchEvent(
				new MouseEvent("mousemove", {
					bubbles: true,
					button: 0,
					clientX: c.x + (dx * i) / 3,
					clientY: c.y + (dy * i) / 3,
				}),
			);
		}
		window.dispatchEvent(
			new MouseEvent("mouseup", {
				bubbles: true,
				button: 0,
				clientX: c.x + dx,
				clientY: c.y + dy,
			}),
		);
	}

	// Eine echte Text-Markierung erzeugen (fuer den Lautsprecher-Klick)
	function selectTestText() {
		try {
			let node = document.getElementById("__vo_selftest_text");
			if (!node) {
				node = document.createElement("p");
				node.id = "__vo_selftest_text";
				node.textContent =
					"Dies ist ein automatischer Selbsttest-Satz zum Pruefen der Vorlese-Funktion.";
				node.style.cssText = "position:fixed;left:-9999px;top:0;";
				document.body.appendChild(node);
			}
			const range = document.createRange();
			range.selectNodeContents(node);
			const sel = window.getSelection();
			sel.removeAllRanges();
			sel.addRange(range);
			document.dispatchEvent(new Event("selectionchange", { bubbles: true }));
			return node.textContent.length;
		} catch (e) {
			return 0;
		}
	}

	// Formular-Ueberlauf-Pruefung (restliche Phase-3-Anforderung): ragt ein
	// Bedienelement horizontal aus dem Panel? ueberlappen sich zwei Elemente?
	function checkPanelOverflow(panel, korr) {
		try {
			const pr = panel.getBoundingClientRect();
			const page = panel.querySelector(".vo-tabpage.vo-active") || panel;
			const els = page.querySelectorAll(
				"select, input, button, .vo-section, .vo-row",
			);
			let probleme = 0;
			els.forEach((el) => {
				const r = el.getBoundingClientRect();
				if (r.width === 0 && r.height === 0) return; // unsichtbar
				const ueber = [];
				if (r.left < pr.left - 1) ueber.push("links");
				if (r.right > pr.right + 1) ueber.push("rechts");
				if (r.bottom > pr.bottom + 1) ueber.push("unten_ausserhalb_panel");
				if (ueber.length) {
					probleme++;
					tlog(
						"WARN",
						"element_ueberlauf",
						{
							element: el.className || el.tagName,
							ueber,
							el_rect: { x: Math.round(r.left), breite: Math.round(r.width) },
							panel_rect: {
								x: Math.round(pr.left),
								breite: Math.round(pr.width),
							},
						},
						korr,
					);
				}
			});
			tlog(
				"INFO",
				"formular_geprueft",
				{ elemente: els.length, probleme },
				korr,
			);
		} catch (e) {
			tlog(
				"WARN",
				"formular_pruefung_fehler",
				{ message: String((e && e.message) || e) },
				korr,
			);
		}
	}

	async function run() {
		const aktiv = await waitForDiag(1500);
		const startKorr = kid();
		tlog("INFO", "start", { url: location.href, diag_aktiv: aktiv }, startKorr);
		if (!aktiv) {
			// Ohne aktiven Diagnose-Modus waeren alle Folge-Logs No-Ops.
			console.warn(
				"[VO-DIAG] Selbsttest: Diagnose-Modus nicht aktiv — Abbruch.",
			);
			return;
		}

		const shadow = getShadow();
		if (!shadow) {
			tlog(
				"ERROR",
				"kein_overlay",
				{ hostGefunden: !!document.getElementById(HOST_ID) },
				startKorr,
			);
			return;
		}

		const speakerBtn = shadow.querySelector(".vo-speaker");
		const container = shadow.querySelector(".vo-container");
		const panel = shadow.querySelector(".vo-panel");

		// 1) Overlay-Grundzustand
		tlog(
			"INFO",
			"overlay_vorhanden",
			{
				speaker: !!speakerBtn,
				panelHook: !!(window.VOOverlay && window.VOOverlay.toggleSettings),
				container: !!container,
			},
			startKorr,
		);

		// 2) Einstellungs-Panel oeffnen (loest Panel-Layout-Sonde aus)
		let k = kid();
		tlog("INFO", "panel_oeffnen", {}, k);
		if (window.VOOverlay && window.VOOverlay.toggleSettings)
			window.VOOverlay.toggleSettings();
		await sleep(400);
		if (panel) checkPanelOverflow(panel, k);

		// 3) Reiter umschalten: Edge <-> Google
		k = kid();
		tlog("INFO", "tab_edge", {}, k);
		click(shadow.querySelector('.vo-tab[data-tab="edge"]'));
		await sleep(250);
		if (panel) checkPanelOverflow(panel, k);

		k = kid();
		tlog("INFO", "tab_google", {}, k);
		click(shadow.querySelector('.vo-tab[data-tab="google"]'));
		await sleep(250);
		if (panel) checkPanelOverflow(panel, k);

		// 4) Aktive Engine umschalten
		k = kid();
		tlog("INFO", "engine_google", {}, k);
		click(shadow.querySelector('.vo-engine-opt[data-engine="google"]'));
		await sleep(250);
		k = kid();
		tlog("INFO", "engine_edge", {}, k);
		click(shadow.querySelector('.vo-engine-opt[data-engine="edge"]'));
		await sleep(250);

		// 5) Edge-Test (echte Synthese -> Funktions-Sonden in Worker/Offscreen)
		k = kid();
		tlog("INFO", "edge_test_klick", {}, k);
		click(shadow.querySelector('.vo-tab[data-tab="edge"]'));
		await sleep(150);
		click(shadow.querySelector('[data-role="edge-test"]'));
		await sleep(8000); // Synthese + Wiedergabe abwarten

		// 6) Markierung + Lautsprecher (echte Synthese ueber die aktive Engine)
		k = kid();
		const len = selectTestText();
		tlog("INFO", "markierung_lautsprecher", { textLen: len }, k);
		await sleep(150);
		pressButton(speakerBtn);
		await sleep(7000);
		// stoppen (zweiter Klick)
		pressButton(speakerBtn);
		await sleep(300);

		// 7) Overlay verschieben (loest Overlay-Layout-Sonde aus)
		k = kid();
		tlog("INFO", "drag", { dx: -200, dy: -150 }, k);
		if (container) dragBy(container, -200, -150);
		await sleep(300);

		// 8) Fenstergroesse simulieren (Clamp-Pfad)
		k = kid();
		tlog(
			"INFO",
			"resize",
			{ innerWidth: window.innerWidth, innerHeight: window.innerHeight },
			k,
		);
		window.dispatchEvent(new Event("resize"));
		await sleep(300);

		// 9) Panel schliessen
		k = kid();
		tlog("INFO", "panel_schliessen", {}, k);
		const close = panel && panel.querySelector(".vo-close");
		if (close) click(close);
		await sleep(200);

		// Aufraeumen: Test-Markierung entfernen
		const tn = document.getElementById("__vo_selftest_text");
		if (tn) tn.remove();
		try {
			window.getSelection().removeAllRanges();
		} catch (e) {
			/* egal */
		}

		const ende = kid();
		tlog(
			"INFO",
			"ende",
			{ hinweis: "Logs jetzt exportieren (VO_DIAG_EXPORT)" },
			ende,
		);
		console.info("[VO-DIAG] Selbsttest abgeschlossen.");
	}

	chrome.runtime.onMessage.addListener((msg) => {
		if (!msg || msg.type !== "VO_SELFTEST_BEGIN") return;
		run().catch((e) => {
			tlog("ERROR", "runner_absturz", {
				message: String((e && e.message) || e),
				stack: e && e.stack,
			});
		});
	});
})();
