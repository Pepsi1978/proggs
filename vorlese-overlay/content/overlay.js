/*
 * overlay.js — das schwebende Bedienelement (Content-Script).
 *
 * Verantwortlich fuer:
 *   - Einfuegen des Overlays (Lautsprecher + Zahnrad) in jede Seite, im Shadow DOM
 *   - Zuverlaessiges Erfassen der Text-Markierung (ohne sie beim Klick zu verlieren)
 *   - Verschieben per Ziehen + Merken der Position
 *   - Senden von Befehlen an den Service-Worker (speak / stop / Stimmen / Test)
 *   - Anzeigen des Wiedergabe-Status und von Fehlermeldungen
 *
 * Es finden KEINE Netzwerk-Aufrufe und KEINE Audio-Wiedergabe hier statt
 * (das wuerde an der CSP der Seite scheitern). Alles laeuft im Service-Worker
 * bzw. im Offscreen-Dokument.
 */
(function () {
	"use strict";

	if (window.__voOverlayLoaded) return;
	window.__voOverlayLoaded = true;

	// ----- Nachrichten-Protokoll (muss mit service-worker.js uebereinstimmen) ---
	const MSG = {
		SPEAK: "TTS_SPEAK",
		STOP: "TTS_STOP",
		GET_VOICES: "GET_VOICES",
		TEST: "TTS_TEST",
		STATE: "TTS_STATE", // Worker -> Content: Status der Wiedergabe
		SPEAK_COMMAND: "SPEAK_COMMAND", // Worker -> Content: Tastaturkuerzel ausgeloest
	};

	let lastSelection = "";
	let isPlaying = false;
	let hintTimer = null;

	// ----- Selektion zuverlaessig erfassen -------------------------------------
	// Sobald irgendwo eine nicht-leere Markierung existiert, merken wir sie uns.
	// Beim Klick auf die Overlay-Buttons fangen wir mousedown mit preventDefault()
	// ab, damit die Markierung NICHT verloren geht.
	function captureSelection() {
		try {
			const sel = window.getSelection();
			const text = sel ? sel.toString() : "";
			if (text && text.trim().length > 0) {
				lastSelection = text;
			}
		} catch (e) {
			/* ignorieren */
		}
	}
	document.addEventListener("selectionchange", captureSelection, true);
	document.addEventListener("mouseup", captureSelection, true);

	// ----- Overlay aufbauen (Shadow DOM) ---------------------------------------
	const host = document.createElement("div");
	host.id = "vorlese-overlay-host";
	// Host selbst nimmt keinen Platz/keine Klicks weg ausserhalb der Buttons.
	host.style.cssText = "all: initial; position: static;";
	const shadow = host.attachShadow({ mode: "open" });

	// CSS aus der Datei laden und in den Shadow Root injizieren.
	const styleEl = document.createElement("style");
	shadow.appendChild(styleEl);
	fetch(chrome.runtime.getURL("content/overlay.css"))
		.then((r) => r.text())
		.then((css) => {
			styleEl.textContent = css;
		})
		.catch(() => {
			/* Fallback: Buttons funktionieren auch ohne Styling */
		});

	// SVG-Icons (inline, damit kein Extra-Request noetig ist)
	const ICON_SPEAKER =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3a4.5 4.5 0 0 0-2.5-4.03v8.06A4.5 4.5 0 0 0 16.5 12zM14 3.23v2.06a7 7 0 0 1 0 13.42v2.06a9 9 0 0 0 0-17.54z"/></svg>';
	const ICON_STOP =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>';
	const ICON_GEAR =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19.14 12.94a7.49 7.49 0 0 0 .05-.94 7.49 7.49 0 0 0-.05-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.61-.22l-2.39.96a7 7 0 0 0-1.62-.94l-.36-2.54a.5.5 0 0 0-.5-.42h-3.84a.5.5 0 0 0-.5.42l-.36 2.54a7 7 0 0 0-1.62.94l-2.39-.96a.5.5 0 0 0-.61.22L2.27 8.3a.5.5 0 0 0 .12.64l2.03 1.58c-.03.31-.05.62-.05.94s.02.63.05.94l-2.03 1.58a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .61.22l2.39-.96c.5.38 1.04.7 1.62.94l.36 2.54a.5.5 0 0 0 .5.42h3.84a.5.5 0 0 0 .5-.42l.36-2.54a7 7 0 0 0 1.62-.94l2.39.96a.5.5 0 0 0 .61-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58zM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7z"/></svg>';

	// Container mit den beiden Buttons + Hinweis-Bubble
	const container = document.createElement("div");
	container.className = "vo-container";
	container.innerHTML =
		'<div class="vo-hint" part="hint"></div>' +
		'<button class="vo-btn vo-speaker" type="button" title="Markierten Text vorlesen">' +
		ICON_SPEAKER +
		"</button>" +
		'<button class="vo-btn vo-gear" type="button" title="Einstellungen">' +
		ICON_GEAR +
		"</button>";
	shadow.appendChild(container);

	const speakerBtn = container.querySelector(".vo-speaker");
	const gearBtn = container.querySelector(".vo-gear");
	const hintEl = container.querySelector(".vo-hint");

	// Panel (Einstellungen) — Aufbau in settings-panel-Funktion (Stufen 2/3)
	const panel = document.createElement("div");
	panel.className = "vo-panel";
	shadow.appendChild(panel);
	let panelBuilt = false;

	document.documentElement.appendChild(host);

	// ----- Hinweis-Bubble ------------------------------------------------------
	function showHint(message, isError) {
		hintEl.textContent = message;
		hintEl.classList.toggle("vo-error", !!isError);
		hintEl.classList.add("vo-show");
		if (hintTimer) clearTimeout(hintTimer);
		hintTimer = setTimeout(() => hintEl.classList.remove("vo-show"), 2600);
	}

	// ----- Wiedergabe-Status am Lautsprecher ----------------------------------
	function setState(state) {
		isPlaying = state === "playing";
		speakerBtn.classList.toggle("vo-playing", state === "playing");
		speakerBtn.classList.toggle("vo-loading", state === "loading");
		speakerBtn.innerHTML = state === "playing" ? ICON_STOP : ICON_SPEAKER;
		speakerBtn.title =
			state === "playing" ? "Vorlesen stoppen" : "Markierten Text vorlesen";
	}

	// ----- Position: wiederherstellen + clampen --------------------------------
	function clampToViewport(left, top) {
		const rect = container.getBoundingClientRect();
		const w = rect.width || 56;
		const h = rect.height || 96;
		const maxLeft = Math.max(0, window.innerWidth - w - 4);
		const maxTop = Math.max(0, window.innerHeight - h - 4);
		return {
			left: Math.min(Math.max(4, left), maxLeft),
			top: Math.min(Math.max(4, top), maxTop),
		};
	}

	function applyPosition(left, top) {
		const c = clampToViewport(left, top);
		container.style.left = c.left + "px";
		container.style.top = c.top + "px";
		container.style.right = "auto";
		container.style.bottom = "auto";
	}

	// Standard: unten rechts (bis der Benutzer verschiebt)
	container.style.right = "16px";
	container.style.bottom = "16px";

	window.VOSettings.getPosition().then((pos) => {
		if (pos) applyPosition(pos.left, pos.top);
	});

	window.addEventListener("resize", () => {
		// Falls per left/top positioniert: erneut clampen, damit das Overlay
		// nach Fenstergroessen-Aenderung sichtbar bleibt.
		if (container.style.left && container.style.left !== "auto") {
			const left = parseFloat(container.style.left);
			const top = parseFloat(container.style.top);
			applyPosition(left, top);
		}
	});

	// ----- Drag + Klick (mousedown mit preventDefault) -------------------------
	// preventDefault auf mousedown ist entscheidend: es verhindert, dass der
	// Klick die aktuelle Text-Markierung aufhebt oder den Fokus stiehlt.
	let drag = null;
	const DRAG_THRESHOLD = 4;

	container.addEventListener("mousedown", (e) => {
		if (e.button !== 0) return;
		e.preventDefault();
		e.stopPropagation();

		const pressed = e.target.closest(".vo-btn");
		const rect = container.getBoundingClientRect();
		drag = {
			pressed,
			startX: e.clientX,
			startY: e.clientY,
			origLeft: rect.left,
			origTop: rect.top,
			moved: false,
		};
		window.addEventListener("mousemove", onDragMove, true);
		window.addEventListener("mouseup", onDragEnd, true);
	});

	function onDragMove(e) {
		if (!drag) return;
		const dx = e.clientX - drag.startX;
		const dy = e.clientY - drag.startY;
		if (!drag.moved && Math.hypot(dx, dy) > DRAG_THRESHOLD) {
			drag.moved = true;
		}
		if (drag.moved) {
			e.preventDefault();
			applyPosition(drag.origLeft + dx, drag.origTop + dy);
		}
	}

	function onDragEnd() {
		window.removeEventListener("mousemove", onDragMove, true);
		window.removeEventListener("mouseup", onDragEnd, true);
		if (!drag) return;
		const wasDrag = drag.moved;
		const pressed = drag.pressed;
		drag = null;

		if (wasDrag) {
			// Position merken
			const rect = container.getBoundingClientRect();
			window.VOSettings.setPosition(rect.left, rect.top);
		} else if (pressed === speakerBtn) {
			onSpeakerClick();
		} else if (pressed === gearBtn) {
			togglePanel();
		}
	}

	// ----- Lautsprecher-Aktion -------------------------------------------------
	async function onSpeakerClick() {
		if (isPlaying) {
			sendMessage({ type: MSG.STOP });
			setState("stopped");
			return;
		}
		captureSelection();
		const text = (lastSelection || "").trim();
		if (!text) {
			showHint("Bitte zuerst Text markieren.");
			return;
		}
		const settings = await window.VOSettings.load();
		const engine = settings.activeEngine;
		const cfg = settings[engine];
		setState("loading");
		sendMessage({
			type: MSG.SPEAK,
			engine,
			text,
			voice: cfg.voice,
			rate: cfg.rate,
			apiKey: engine === "google" ? settings.google.apiKey : undefined,
		});
	}

	// ----- Kommunikation mit dem Service-Worker --------------------------------
	function sendMessage(msg) {
		try {
			chrome.runtime.sendMessage(msg, () => {
				// lastError abfragen, damit Chrome keine Warnung wirft
				void chrome.runtime.lastError;
			});
		} catch (e) {
			showHint("Erweiterung nicht erreichbar — Seite neu laden.", true);
		}
	}

	// Status-/Befehls-Nachrichten vom Worker empfangen
	chrome.runtime.onMessage.addListener((msg) => {
		if (!msg || typeof msg !== "object") return;
		if (msg.type === MSG.STATE) {
			if (msg.state === "error") {
				setState("stopped");
				showHint(msg.message || "Es ist ein Fehler aufgetreten.", true);
			} else {
				setState(msg.state);
			}
		} else if (msg.type === MSG.SPEAK_COMMAND) {
			onSpeakerClick();
		}
	});

	// ----- Einstellungs-Panel --------------------------------------------------
	// Der vollstaendige Aufbau (Engine-Umschalter, Edge-/Google-Reiter) wird in
	// buildPanel() erstellt. Stufe 1 liefert das Grundgeruest; Stufen 2/3
	// verdrahten Stimmen-Listen und Test-Buttons.
	function togglePanel() {
		if (!panelBuilt) {
			buildPanel();
			panelBuilt = true;
		}
		const open = panel.classList.toggle("vo-open");
		if (open) positionPanel();
	}

	function positionPanel() {
		const c = container.getBoundingClientRect();
		const pw = panel.offsetWidth || 340;
		const ph = panel.offsetHeight || 300;
		// bevorzugt links neben dem Overlay, sonst rechts
		let left = c.left - pw - 12;
		if (left < 8) left = Math.min(window.innerWidth - pw - 8, c.right + 12);
		let top = c.top;
		if (top + ph > window.innerHeight - 8)
			top = Math.max(8, window.innerHeight - ph - 8);
		panel.style.left = Math.max(8, left) + "px";
		panel.style.top = Math.max(8, top) + "px";
	}

	function buildPanel() {
		// In Stufe 1 nur ein Platzhalter — wird in Stufe 2/3 ersetzt.
		panel.innerHTML =
			'<div class="vo-panel-head">' +
			'<h2 class="vo-panel-title">Einstellungen</h2>' +
			'<button class="vo-close" type="button" title="Schliessen">&times;</button>' +
			"</div>" +
			'<div class="vo-panel-body">' +
			'<p class="vo-help">Die Einstellungen (Engine-Wahl, Stimmen, Tempo, API-Key) ' +
			"werden in den naechsten Stufen aktiviert.</p>" +
			"</div>";
		panel
			.querySelector(".vo-close")
			.addEventListener("click", () => panel.classList.remove("vo-open"));
	}

	// Panel schliessen, wenn ausserhalb geklickt wird
	document.addEventListener(
		"mousedown",
		(e) => {
			if (!panel.classList.contains("vo-open")) return;
			const path = e.composedPath ? e.composedPath() : [];
			if (!path.includes(panel) && !path.includes(container)) {
				panel.classList.remove("vo-open");
			}
		},
		true,
	);
})();
