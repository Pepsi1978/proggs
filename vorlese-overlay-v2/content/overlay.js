/*
 * overlay.js — das schwebende Bedienelement (Content-Script).
 *
 * Verantwortlich fuer:
 *   - Einfuegen des Overlays (Lautsprecher + Zahnrad) in jede Seite, im Shadow DOM
 *   - Zuverlaessiges Erfassen der Text-Markierung (ohne sie beim Klick zu verlieren)
 *   - Verschieben per Ziehen + Merken der Position
 *   - Senden von Befehlen an den Service-Worker (speak / stop / Stimmen / Test)
 *   - Anzeigen des Wiedergabe-Status und von Fehlermeldungen
 *   - Das Einstellungs-Panel (Engine-Umschalter, Edge-/Google-Reiter)
 *   - Pause-Button, Fortschrittsbalken, Wort-Anzeige, Auto-Speak
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
		STATE: "TTS_STATE", // Worker → Content: Status der Wiedergabe
		SPEAK_COMMAND: "SPEAK_COMMAND", // Worker → Content: Tastaturkuerzel ausgeloest
		PAUSE: "TTS_PAUSE", // Content → SW: Wiedergabe pausieren
		RESUME: "TTS_RESUME", // Content → SW: Wiedergabe fortsetzen
		PAUSE_RESUME_TOGGLE: "TTS_PAUSE_RESUME", // Content → SW: Toggle Pause/Resume
		PROGRESS: "TTS_PROGRESS", // SW → Content: Fortschrittsanzeige {chunk, totalChunks}
		RELOAD_EXTENSION: "RELOAD_EXTENSION", // Content → SW: Erweiterung neu laden (Speicher bleibt)
		OPEN_SETTINGS: "OPEN_SETTINGS", // SW → Content: Einstellungs-Panel oeffnen (Toolbar-Klick)
	};

	const SAMPLE_TEXT =
		"Dies ist ein Beispielsatz, mit dem du die gewählte Stimme und das Tempo prüfen kannst.";
	const PAGE_HOST = (location.hostname || "").toLowerCase();

	let lastSelection = "";
	let isPlaying = false;
	let overlayPageDisabled = false;
	let hintTimer = null;
	// Vom Panel gesetzte Funktion, um Status-/Fehlermeldungen im Panel anzuzeigen.
	let panelStatusHandler = null;

	// ----- Zustand für Pause und Fortschritt -----------------------------------
	let isPaused = false;
	let progressChunk = 0;
	let progressTotal = 0;
	// Sicherheitsnetz gegen einen haengenden "spielt"-Zustand (siehe armEndWatchdog).
	let endWatchdog = null;

	// ----- Auto-Lese-Modus (A-Button) ------------------------------------------
	// Wenn aktiv: Ein Doppelklick auf ein Wort liest ab diesem Wort den restlichen
	// Text Absatz fuer Absatz vor (Quellenangaben/Fussnoten/Diagramme werden
	// ausgefiltert). Der Zustand wird in den Einstellungen gespeichert (autoSpeak).
	let autoMode = false; // A-Button an/aus
	let autoQueue = []; // verbleibende Absaetze
	let autoReading = false; // laeuft gerade eine automatische Vorlesung
	let autoTotal = 0; // Gesamtzahl Absaetze der aktuellen Auto-Vorlesung
	let autoTriggerTimer = null; // Debounce fuer den Auswahl-Trigger
	// Haekchen-Viereck (unter dem Zahnrad): "Markierten Text automatisch vorlesen".
	// Spiegelt die Einstellung autoSpeak — synchron mit dem Panel-Haekchen.
	let autoSpeakOn = false;

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

	// Auto-Trigger bei Textauswahl (Ziehen-Markierung ODER Doppelklick — beide
	// enden mit mouseup):
	//   - A-Button (autoMode) an  -> ab der Auswahl absatzweise bis zum Ende vorlesen
	//   - sonst Haekchen (autoSpeak) an -> nur den markierten Text vorlesen
	function onSelectionSettled(e) {
		if (overlayPageDisabled) return;
		// Markierungen im Overlay selbst ignorieren (Buttons/Panel/Drag).
		try {
			const path = e && e.composedPath ? e.composedPath() : [];
			if (path.includes(host)) return;
		} catch (e2) {
			/* ignorieren */
		}
		if (autoTriggerTimer) clearTimeout(autoTriggerTimer);
		// Kurz warten, bis die Auswahl final steht.
		autoTriggerTimer = setTimeout(async () => {
			autoTriggerTimer = null;
			let text = "";
			try {
				const sel = window.getSelection();
				text = sel ? sel.toString().trim() : "";
			} catch (e3) {
				return;
			}
			if (!text) return;
			if (autoMode) {
				// Laufende Auto-Vorlesung nicht durch jede neue Auswahl neu starten.
				startAutoReadFromSelection();
				return;
			}
			let settings = null;
			try {
				settings = await window.VOSettings.load();
			} catch (e4) {
				return;
			}
			if (settings && settings.autoSpeak) {
				if (isPlaying || autoReading) return; // laufende Wiedergabe nicht stoeren
				if (window.VODiag)
					window.VODiag.log(
						"INFO",
						"NUTZUNG",
						"overlay.auto_speak_ausgefuehrt",
						{ textLen: text.length },
					);
				onSpeakerClick();
			}
		}, 450);
	}
	document.addEventListener("mouseup", onSelectionSettled, true);

	// ----- Overlay aufbauen (Shadow DOM) ---------------------------------------
	const host = document.createElement("div");
	host.id = "vorlese-overlay-host";
	host.style.cssText = "all: initial; position: static;";
	const shadow = host.attachShadow({ mode: "open" });

	const styleEl = document.createElement("style");
	shadow.appendChild(styleEl);
	// CSS-Load-Promise: die initiale Layout-Sonde misst erst, wenn dieses
	// asynchron geladene CSS angewandt ist — sonst meldet sie die ungestylte
	// Groesse (volle Viewport-Breite) und koennte Fehlalarme erzeugen.
	const cssReady = fetch(chrome.runtime.getURL("content/overlay.css"))
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
	// SVG-Icons für den Pause-Button (Pause-Balken und Play-Dreieck)
	const ICON_PAUSE =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>';
	const ICON_PLAY_TRIANGLE =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><polygon points="5,3 19,12 5,21"/></svg>';

	const container = document.createElement("div");
	container.className = "vo-container";
	container.innerHTML =
		'<div class="vo-hint" part="hint"></div>' +
		// Auto-Lese-Button (A) — ueber dem Lautsprecher. Leuchtet wenn aktiv.
		'<button class="vo-btn vo-auto-btn" type="button" title="Auto-Vorlesen: Doppelklick auf ein Wort liest ab dort weiter">' +
		// A als SVG-Text exakt im Kreismittelpunkt (text-anchor=middle +
		// dominant-baseline=central) -> mathematisch zentriert, unabhaengig von
		// Schrift-Metriken.
		'<svg class="vo-auto-letter" viewBox="0 0 24 24" aria-hidden="true">' +
		'<text x="12" y="12" text-anchor="middle" dominant-baseline="central">A</text>' +
		"</svg>" +
		"</button>" +
		'<button class="vo-btn vo-speaker" type="button" title="Markierten Text vorlesen">' +
		ICON_SPEAKER +
		"</button>" +
		// Pause-Button: initial versteckt, wird bei 'playing'/'paused' sichtbar
		'<button class="vo-btn vo-pause-btn" type="button" title="Pause / Weiter" style="display:none;">' +
		ICON_PAUSE +
		"</button>" +
		// Haekchen-Viereck am Overlay: schaltet "Markierten Text automatisch
		// vorlesen" direkt (gleiche Einstellung wie das Panel-Haekchen).
		// (Das Einstellungs-Zahnrad entfaellt — Einstellungen oeffnen jetzt ueber
		//  Linksklick auf das Toolbar-Symbol der Erweiterung.)
		'<button class="vo-btn vo-autospeak-box" type="button" title="Markierten Text automatisch vorlesen">' +
		'<span class="vo-autospeak-check">✓</span>' +
		"</button>" +
		// Schmaler Fortschrittsbalken unter dem Button-Container
		'<div class="vo-progress-wrap" style="display:none;">' +
		'<div class="vo-progress"></div>' +
		"</div>";
	shadow.appendChild(container);

	const autoBtn = container.querySelector(".vo-auto-btn");
	const speakerBtn = container.querySelector(".vo-speaker");
	const pauseBtn = container.querySelector(".vo-pause-btn");
	const autoSpeakBox = container.querySelector(".vo-autospeak-box");
	const hintEl = container.querySelector(".vo-hint");
	const progressWrap = container.querySelector(".vo-progress-wrap");
	const progressBar = container.querySelector(".vo-progress");

	const panel = document.createElement("div");
	panel.className = "vo-panel";
	shadow.appendChild(panel);
	let panelBuilt = false;

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

	function isCurrentHostDisabled(settings) {
		return !!(
			PAGE_HOST && normalizeHostList(settings && settings.disabledHosts).includes(PAGE_HOST)
		);
	}

	function mountOverlayHost() {
		if (!host.isConnected) document.documentElement.appendChild(host);
	}

	function unmountOverlayHost() {
		if (isPlaying || autoReading) sendMessage({ type: MSG.STOP });
		clearEndWatchdog();
		isPaused = false;
		autoReading = false;
		autoQueue = [];
		clearParagraphHighlight();
		resetProgress();
		setPauseButtonVisible(false);
		setPauseButtonState(false);
		setState("stopped");
		panel.classList.remove("vo-open");
		if (host.isConnected) host.remove();
	}

	function applyPageDisabledState(disabled) {
		overlayPageDisabled = !!disabled;
		if (overlayPageDisabled) unmountOverlayHost();
		else mountOverlayHost();
	}

	function positionAfterEnable(pos) {
		cssReady.then(() =>
			requestAnimationFrame(() => {
				if (overlayPageDisabled) return;
				positionInitial(pos);
				logLayout("Overlay", container, {
					initial: true,
					angedockt: dockedToOverlays,
					verschiebbar: draggable,
				});

				if (!draggable) {
					let tries = 0;
					const dockTimer = setInterval(() => {
						tries++;
						if (!dockedToOverlays && anchorToOverlays()) dockedToOverlays = true;
						if (dockedToOverlays || tries >= 20) clearInterval(dockTimer);
					}, 300);
				}
			}),
		);
	}

	// ----- Hinweis-Bubble ------------------------------------------------------
	// Auf Wunsch des Benutzers deaktiviert: die erklaerenden Text-Bubbles am Rand
	// (z.B. "Auto-Vorlesen an", "Bitte zuerst Text markieren") werden NICHT mehr
	// angezeigt. showHint bleibt als No-Op bestehen, damit alle bestehenden Aufrufe
	// gefahrlos weiterlaufen und der hintTimer/hintEl unangetastet bleiben.
	function showHint(/* message, isError */) {
		/* bewusst leer — keine Hinweis-Bubbles mehr */
	}
	void hintEl;
	void hintTimer;

	// ----- Wiedergabe-Status am Lautsprecher ----------------------------------
	function setState(state) {
		isPlaying = state === "playing";
		speakerBtn.classList.toggle("vo-playing", state === "playing");
		speakerBtn.classList.toggle("vo-loading", state === "loading");
		speakerBtn.innerHTML = state === "playing" ? ICON_STOP : ICON_SPEAKER;
		speakerBtn.title =
			state === "playing" ? "Vorlesen stoppen" : "Markierten Text vorlesen";
	}

	// ----- Pause-Button Sichtbarkeit und Zustand steuern ----------------------
	function setPauseButtonVisible(visible) {
		pauseBtn.style.display = visible ? "" : "none";
	}

	function setPauseButtonState(paused) {
		pauseBtn.innerHTML = paused ? ICON_PLAY_TRIANGLE : ICON_PAUSE;
		pauseBtn.title = paused ? "Weiter vorlesen" : "Pause";
		pauseBtn.classList.toggle("vo-paused", paused);
	}

	// ----- Fortschrittsbalken (zeitbasiert) ------------------------------------
	// NICHT pro Chunk zaehlen (kurzer/mittlerer Text = 1 Chunk -> bliebe bei 0%),
	// sondern ein gleichmaessig laufender Balken ueber die GESCHAETZTE Vorlese-Dauer
	// (Textlaenge / Tempo). Start/Pause/Weiter folgen den Status-Events.
	let lastSpeechMs = 0;

	function estimateSpeechMs(text, rate) {
		const chars = (text || "").length;
		const r = rate && rate > 0 ? rate : 1;
		// ~14 Zeichen pro Sekunde bei Tempo 1.0 (deutsche Sprache, grobe Schaetzung).
		const seconds = chars / (14 * r);
		return Math.max(800, Math.round(seconds * 1000));
	}

	function startProgressAnim(ms) {
		progressWrap.style.display = "";
		progressBar.style.transition = "none";
		progressBar.style.width = "0%";
		void progressBar.offsetWidth; // Reflow erzwingen, damit 0% greift
		const dur = Math.max(800, ms || 0);
		progressBar.style.transition = "width " + dur + "ms linear";
		progressBar.style.width = "100%";
	}

	function pauseProgressAnim() {
		// Aktuelle Breite (px) einfrieren, damit der Balken stehen bleibt.
		const w = getComputedStyle(progressBar).width;
		progressBar.style.transition = "none";
		progressBar.style.width = w;
	}

	function resumeProgressAnim() {
		const wrapW = progressWrap.clientWidth || 1;
		const curW = parseFloat(getComputedStyle(progressBar).width) || 0;
		const frac = Math.min(1, Math.max(0, curW / wrapW));
		const remainMs = Math.max(
			400,
			Math.round((lastSpeechMs || 0) * (1 - frac)),
		);
		progressBar.style.transition = "none";
		progressBar.style.width = frac * 100 + "%";
		void progressBar.offsetWidth;
		progressBar.style.transition = "width " + remainMs + "ms linear";
		progressBar.style.width = "100%";
	}

	function resetProgress() {
		progressChunk = 0;
		progressTotal = 0;
		progressBar.style.transition = "none";
		progressBar.style.width = "0%";
		progressWrap.style.display = "none";
	}

	// ----- Sicherheitsnetz gegen haengenden "spielt"-Zustand -------------------
	// Falls das abschliessende "stopped" vom Service-Worker verloren geht (z.B.
	// weil der MV3-Worker waehrend langer Wiedergabe neu gestartet ist), bliebe
	// der Status fuer immer auf "spielt": Balken + rotes Stop-Icon haengen und
	// neuer markierter Text wird nicht mehr vorgelesen. Dieser Watchdog setzt den
	// Zustand notfalls selbst zurueck. Er ist BEWUSST sehr grosszuegig bemessen
	// (doppelte geschaetzte Dauer, mind. 30 s), damit eine echte Wiedergabe NIE
	// abgebrochen wird — er greift nur, wenn das Ende-Signal wirklich ausbleibt.
	function clearEndWatchdog() {
		if (endWatchdog) {
			clearTimeout(endWatchdog);
			endWatchdog = null;
		}
	}
	function armEndWatchdog() {
		clearEndWatchdog();
		const timeout = Math.max(30000, (lastSpeechMs || 0) * 2 + 10000);
		endWatchdog = setTimeout(() => {
			endWatchdog = null;
			// Nur eingreifen, wenn laut Overlay noch "gespielt" wird und NICHT pausiert
			// (bei Pause ist ein langes Ausbleiben des Endes voellig normal).
			if (!isPlaying || isPaused) return;
			if (window.VODiag)
				window.VODiag.log("WARN", "ZUSTAND", "overlay.end_watchdog_reset", {
					geschaetzt_ms: lastSpeechMs,
				});
			// Wie ein verspaetetes "stopped" behandeln -> Zustand sauber zuruecksetzen.
			autoReading = false;
			autoQueue = [];
			isPaused = false;
			clearParagraphHighlight();
			setState("stopped");
			resetProgress();
			setPauseButtonVisible(false);
			setPauseButtonState(false);
		}, timeout);
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

	// Rechts-Verankerung wie die Overlays-Erweiterung: gespeichert wird left/top,
	// angewendet wird aber als Abstand zum RECHTEN Rand. So wandert das Overlay
	// beim Oeffnen/Schliessen der Seitenleiste mit dem rechten Rand mit, statt am
	// Rand kleben zu bleiben. null = noch nicht verschoben -> CSS right/bottom gilt.
	let rightOffset = null;
	let topOffset = null;

	function applyPosition(left, top) {
		const c = clampToViewport(left, top);
		const rect = container.getBoundingClientRect();
		const w = rect.width || 56;
		rightOffset = Math.max(4, window.innerWidth - (c.left + w));
		topOffset = c.top;
		container.style.left = "auto";
		container.style.bottom = "auto";
		container.style.right = rightOffset + "px";
		container.style.top = topOffset + "px";
	}

	// Bei Resize bleibt die horizontale Lage durch CSS `right` automatisch am
	// rechten Rand verankert; hier nur sicherstellen, dass nichts aus dem
	// Viewport ragt (links/unten) — vertikal clampen, rechten Abstand begrenzen.
	function clampCurrentPosition() {
		if (rightOffset == null) return; // noch nie verschoben -> CSS-Standard gilt
		const rect = container.getBoundingClientRect();
		const w = rect.width || 56;
		const h = rect.height || 96;
		rightOffset = Math.min(
			Math.max(4, rightOffset),
			Math.max(4, window.innerWidth - w - 4),
		);
		topOffset = Math.min(
			Math.max(4, topOffset),
			Math.max(4, window.innerHeight - h - 4),
		);
		container.style.right = rightOffset + "px";
		container.style.top = topOffset + "px";
	}

	// ----- Layout-Sonde: Geometrie + Sichtbarkeits-Check im Viewport ----------
	// Loggt Position/Groesse eines Overlay-Elements und ob es vollstaendig im
	// sichtbaren Bereich liegt (welche Seite ragt hinaus?). Nur aktiv im
	// Diagnose-Modus -> sonst KEIN getBoundingClientRect (keine Mehrlast).
	function logLayout(komponente, el, extra) {
		if (!(window.VODiag && window.VODiag.isEnabled())) return;
		try {
			const r = el.getBoundingClientRect();
			const vw = window.innerWidth;
			const vh = window.innerHeight;
			const abgeschnitten = [];
			if (r.left < 0) abgeschnitten.push("links");
			if (r.top < 0) abgeschnitten.push("oben");
			if (r.right > vw) abgeschnitten.push("rechts");
			if (r.bottom > vh) abgeschnitten.push("unten");
			const sichtbar =
				abgeschnitten.length === 0 && r.width > 0 && r.height > 0;
			window.VODiag.log(
				sichtbar ? "INFO" : "WARN",
				"LAYOUT",
				komponente + (sichtbar ? ":sichtbar" : ":teilweise_ausserhalb"),
				Object.assign(
					{
						x: Math.round(r.left),
						y: Math.round(r.top),
						breite: Math.round(r.width),
						hoehe: Math.round(r.height),
						sichtbarer_bereich: { breite: vw, hoehe: vh },
						dpr: window.devicePixelRatio,
						bildschirm: { breite: screen.width, hoehe: screen.height },
						abgeschnitten,
					},
					extra || {},
				),
			);
		} catch (e) {
			/* Layout-Sonde darf nie stoeren */
		}
	}

	// Standard: unten rechts (bis angedockt oder verschoben)
	container.style.right = "16px";
	container.style.bottom = "16px";

	// Doppelter Abstand: die Overlays-Buttons haben untereinander ~10px Luecke.
	const OVERLAY_DOCK_GAP = 20;
	let dockedToOverlays = false;
	// Einstellung "Overlay verschiebbar": false = fester Platz (angedockt/Standard),
	// true = per Maus verschiebbar und bleibt wo losgelassen.
	let draggable = false;

	// Die Overlays-Erweiterung (frueher Tampermonkey-Skripte) rendert ihre Buttons
	// als senkrechte Spalte; der Mikrofon-Button (.stt-mic-btn) ist der unterste.
	// Das Vorlese-Overlay dockt rechtsbuendig DARUNTER an (doppelter Abstand),
	// damit es als eigenstaendiges, aber zugehoeriges Overlay erkennbar ist.
	// Existiert keine Overlays-Erweiterung auf der Seite -> fester Platz unten rechts.
	function anchorToOverlays() {
		const mic = document.querySelector(".stt-mic-btn");
		if (!mic) return false;
		const m = mic.getBoundingClientRect();
		if (m.width < 5 || m.height < 5) return false;
		const rect = container.getBoundingClientRect();
		const h = rect.height || 48;
		rightOffset = Math.max(4, window.innerWidth - m.right); // gleiche rechte Kante
		topOffset = Math.max(4, m.bottom + OVERLAY_DOCK_GAP); // doppelter Abstand darunter
		topOffset = Math.min(topOffset, Math.max(4, window.innerHeight - h - 4));
		container.style.left = "auto";
		container.style.bottom = "auto";
		container.style.right = rightOffset + "px";
		container.style.top = topOffset + "px";
		return true;
	}

	// Zurueck an den Standardort: angedockt unter den Overlays, sonst unten rechts.
	function resetToDocked() {
		rightOffset = null;
		topOffset = null;
		dockedToOverlays = false;
		container.style.left = "auto";
		container.style.top = "auto";
		container.style.right = "16px";
		container.style.bottom = "16px";
		if (anchorToOverlays()) dockedToOverlays = true;
	}

	// Verschiebbar -> gespeicherte Position (oder Standard). Nicht verschiebbar ->
	// an die Overlays andocken bzw. fester Standardplatz.
	function positionInitial(pos) {
		if (draggable) {
			if (pos) applyPosition(pos.left, pos.top);
			return;
		}
		if (anchorToOverlays()) {
			dockedToOverlays = true;
			return;
		}
		if (pos) applyPosition(pos.left, pos.top);
	}

	Promise.all([window.VOSettings.getPosition(), window.VOSettings.load()]).then(
		([pos, s]) => {
			applyPageDisabledState(isCurrentHostDisabled(s));
			if (overlayPageDisabled) return;
			draggable = !!(s && s.draggable);
			autoMode = !!(s && s.autoMode);
			applyAutoButton();
			autoSpeakOn = !!(s && s.autoSpeak);
			applyAutoSpeakBox();

			// Position ERST nach CSS-Load + naechstem Frame anwenden (sonst falsche
			// Breitenmessung -> Sprung an den Rand).
			positionAfterEnable(pos);
		},
	);

	window.addEventListener("resize", () => {
		// Nicht verschiebbar + angedockt: rechtsbuendig unter dem Mikrofon bleiben.
		if (!draggable && dockedToOverlays && anchorToOverlays()) return;
		clampCurrentPosition();
	});

	// Aenderungen aus der Seitenleiste live uebernehmen (verschiebbar an/aus).
	chrome.storage.onChanged.addListener((changes, area) => {
		if (area !== "local" || !changes.vo_settings) return;
		const nv = changes.vo_settings.newValue;
		if (!nv) return;
		const wasDisabled = overlayPageDisabled;
		applyPageDisabledState(isCurrentHostDisabled(nv));
		if (overlayPageDisabled) return;
		autoMode = !!nv.autoMode;
		applyAutoButton();
		if (typeof nv.autoSpeak === "boolean" && nv.autoSpeak !== autoSpeakOn) {
			autoSpeakOn = nv.autoSpeak;
			applyAutoSpeakBox();
		}
		const was = draggable;
		draggable = !!nv.draggable;
		if (wasDisabled) {
			window.VOSettings.getPosition().then((pos) => positionAfterEnable(pos));
		}
		// Verschiebbar AUS -> zurueck an den Standardort.
		if (was && !draggable) resetToDocked();
	});

	// ----- Drag + Klick (mousedown mit preventDefault) -------------------------
	// preventDefault auf mousedown ist entscheidend: es verhindert, dass der
	// Klick die aktuelle Text-Markierung aufhebt oder den Fokus stiehlt.
	let drag = null;
	const DRAG_THRESHOLD = 4;

	container.addEventListener("mousedown", (e) => {
		const isLeft = e.button === 0;
		const isRight = e.button === 2;
		// Linke Taste: immer (Klick auf Button bzw. Ziehen wenn verschiebbar).
		// Rechte Taste: nur zum Ziehen und nur wenn das Overlay verschiebbar ist.
		if (!isLeft && !(isRight && draggable)) return;
		e.preventDefault();
		e.stopPropagation();

		// Rechtsklick ist reines Ziehen -> kein Button-Klick ausloesen.
		const pressed = isLeft ? e.target.closest(".vo-btn") : null;
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

	// Rechtsklick auf das Overlay oeffnet kein Kontextmenu, wenn es verschiebbar
	// ist (die rechte Taste dient dann dem Ziehen).
	container.addEventListener("contextmenu", (e) => {
		if (draggable) e.preventDefault();
	});

	function onDragMove(e) {
		if (!drag) return;
		const dx = e.clientX - drag.startX;
		const dy = e.clientY - drag.startY;
		if (!drag.moved && Math.hypot(dx, dy) > DRAG_THRESHOLD) {
			drag.moved = true;
		}
		// Nur tatsaechlich verschieben, wenn das Overlay verschiebbar ist.
		if (drag.moved && draggable) {
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
			// Position nur merken, wenn das Overlay verschiebbar ist; sonst wurde
			// gar nicht bewegt (onDragMove ignoriert die Bewegung).
			if (draggable) {
				const rect = container.getBoundingClientRect();
				window.VOSettings.setPosition(rect.left, rect.top);
				clampCurrentPosition();
				logLayout("Overlay", container, { nachDrag: true });
			}
		} else if (pressed === autoBtn) {
			toggleAutoMode();
		} else if (pressed === speakerBtn) {
			onSpeakerClick();
		} else if (pressed === pauseBtn) {
			onPauseClick();
		} else if (pressed === autoSpeakBox) {
			toggleAutoSpeak();
		}
	}

	// ----- Auto-Lese-Modus (A-Button) ------------------------------------------
	function applyAutoButton() {
		autoBtn.classList.toggle("vo-active", autoMode);
		autoBtn.title = autoMode
			? "Auto-Vorlesen AN — Doppelklick auf ein Wort liest ab dort weiter (erneut klicken zum Ausschalten)"
			: "Auto-Vorlesen: Doppelklick auf ein Wort liest ab dort weiter";
	}

	// ----- Haekchen-Viereck "Markierten Text automatisch vorlesen" -------------
	function applyAutoSpeakBox() {
		autoSpeakBox.classList.toggle("vo-checked", autoSpeakOn);
		autoSpeakBox.title = autoSpeakOn
			? "Automatisch vorlesen AN — markierter Text wird vorgelesen (klicken zum Ausschalten)"
			: "Markierten Text automatisch vorlesen (klicken zum Einschalten)";
	}

	// Schaltet autoSpeak um, speichert und haelt das Panel-Haekchen synchron.
	async function toggleAutoSpeak() {
		autoSpeakOn = !autoSpeakOn;
		applyAutoSpeakBox();
		if (window.VODiag)
			window.VODiag.log("INFO", "UI_EREIGNIS", "overlay.autospeak_box_toggle", {
				an: autoSpeakOn,
			});
		try {
			const s = await window.VOSettings.load();
			s.autoSpeak = autoSpeakOn;
			await window.VOSettings.save(s);
		} catch (e) {
			/* Button-Zustand ist trotzdem gesetzt */
		}
		// Panel-Haekchen synchronisieren (falls das Panel schon gebaut wurde).
		try {
			const cb = panel.querySelector('[data-role="auto-speak"]');
			if (cb) cb.checked = autoSpeakOn;
		} catch (e) {
			/* egal */
		}
		showHint(
			autoSpeakOn ? "Automatisch vorlesen an." : "Automatisch vorlesen aus.",
		);
	}

	async function toggleAutoMode() {
		autoMode = !autoMode;
		applyAutoButton();
		if (window.VODiag)
			window.VODiag.log("INFO", "UI_EREIGNIS", "overlay.auto_modus_toggle", {
				an: autoMode,
			});
		// Zustand dauerhaft speichern (ueberlebt Reloads).
		try {
			const s = await window.VOSettings.load();
			s.autoMode = autoMode;
			await window.VOSettings.save(s);
		} catch (e) {
			/* ignorieren — Button-Zustand ist trotzdem aktiv */
		}
		if (autoMode) {
			showHint("Auto-Vorlesen an: Doppelklick auf ein Wort.");
		} else {
			// Ausschalten beendet eine laufende automatische Vorlesung.
			if (autoReading) {
				autoReading = false;
				autoQueue = [];
				clearParagraphHighlight();
				sendMessage({ type: MSG.STOP });
				setState("stopped");
				resetProgress();
				setPauseButtonVisible(false);
				setPauseButtonState(false);
			}
			showHint("Auto-Vorlesen aus.");
		}
	}

	// Doppelklick im Auto-Modus -> ab dem geklickten Wort den restlichen Text
	// Absatz fuer Absatz vorlesen.
	async function startAutoReadFromSelection() {
		let sel;
		try {
			sel = window.getSelection();
		} catch (e) {
			return;
		}
		if (!sel || sel.rangeCount === 0) return;
		const clickedWord = sel.toString().trim();
		if (!clickedWord) return;

		let range;
		try {
			range = sel.getRangeAt(0);
		} catch (e) {
			return;
		}
		const startBlock = closestReadableBlock(range.startContainer);
		const paras = collectParagraphsFrom(startBlock);
		if (!paras.length) {
			showHint("Hier wurde kein lesbarer Absatz gefunden.");
			return;
		}
		// Nur wenn EIN einzelnes Wort markiert wurde: ersten Absatz ab diesem Wort
		// beginnen. Bei groesseren Markierungen ab dem Absatzanfang vorlesen.
		if (!/\s/.test(clickedWord) && paras[0] && paras[0].text) {
			const idx = paras[0].text
				.toLowerCase()
				.indexOf(clickedWord.toLowerCase());
			if (idx > 0) paras[0].text = paras[0].text.slice(idx);
		}

		if (window.VODiag)
			window.VODiag.log("INFO", "NUTZUNG", "overlay.auto_read_start", {
				absaetze: paras.length,
				wort: clickedWord.slice(0, 40),
			});
		startParagraphQueue(paras);
	}

	// Liefert den Schnitt-Text zwischen einem Range und einem Element — also nur
	// den Teil des Elements, der wirklich markiert ist (bereinigt). So beginnt das
	// Vorlesen exakt am ersten markierten Wort, nicht am Absatzanfang.
	function intersectionText(range, el) {
		try {
			const sub = range.cloneRange();
			const elRange = document.createRange();
			elRange.selectNodeContents(el);
			if (sub.compareBoundaryPoints(Range.START_TO_START, elRange) < 0) {
				sub.setStart(elRange.startContainer, elRange.startOffset);
			}
			if (sub.compareBoundaryPoints(Range.END_TO_END, elRange) > 0) {
				sub.setEnd(elRange.endContainer, elRange.endOffset);
			}
			const frag = sub.cloneContents();
			stripRefs(frag);
			return finalizeText(frag.textContent || "");
		} catch (e) {
			return "";
		}
	}

	// Liefert NUR die Markierung — als Liste von { text, el }-Objekten. "el" ist
	// das LIVE-Absatz-Element (zum Hervorheben), "text" der bereinigte, auf die
	// Markierungsgrenzen beschnittene Vorlese-Text. Bei einer Teil-Markierung wird
	// nichts VOR dem ersten markierten Wort gelesen; mehrere markierte Absaetze
	// ergeben je einen Eintrag.
	function paragraphsFromSelection() {
		let sel;
		try {
			sel = window.getSelection();
		} catch (e) {
			return [];
		}
		if (!sel || sel.rangeCount === 0) return [];
		let range;
		try {
			range = sel.getRangeAt(0);
		} catch (e) {
			return [];
		}
		if (range.collapsed) return [];

		// Live-Bloecke finden, die die Markierung beruehrt.
		const candidates = Array.from(
			document.querySelectorAll(
				"p, li, h1, h2, h3, h4, h5, h6, blockquote, dd, dt, figcaption",
			),
		);
		const result = [];
		for (const el of candidates) {
			if (el.closest(SKIP_ANCESTOR_SELECTOR)) continue;
			if (el.querySelector("p, li, blockquote")) continue;
			let hit = false;
			try {
				hit = range.intersectsNode(el);
			} catch (e) {
				hit = false;
			}
			if (!hit) continue;
			if (!isVisible(el)) continue;
			const t = intersectionText(range, el);
			if (t && t.length >= 2 && /[A-Za-zÀ-ÿ0-9]/.test(t)) {
				result.push({ text: t, el });
			}
		}
		if (result.length) return result;

		// Fallback: Markierung liegt nicht in einem erkannten Block -> reines
		// Markierungs-Fragment bereinigt, Absatz-Element = naechster Block.
		let frag;
		try {
			frag = range.cloneContents();
		} catch (e) {
			return [];
		}
		stripRefs(frag);
		const t = finalizeText(frag.textContent || "");
		if (t && t.length >= 2 && /[A-Za-zÀ-ÿ0-9]/.test(t)) {
			result.push({ text: t, el: closestReadableBlock(range.startContainer) });
		}
		return result;
	}

	// ----- Folgen des gerade vorgelesenen Absatzes -----------------------------
	// Die frueher gelbe Einfaerbung des vorgelesenen Absatzes wurde auf
	// ausdruecklichen Wunsch entfernt: Der Seitentext wird NICHT mehr eingefaerbt.
	// Es wird nur noch sanft zum gerade vorgelesenen Absatz gescrollt, damit man
	// sieht, wo die Vorlesung steht. Kein Seiten-Style wird mehr veraendert.
	let hlEl = null;
	function highlightParagraph(el) {
		clearParagraphHighlight();
		if (!el) return;
		hlEl = el;
		try {
			el.scrollIntoView({ block: "nearest", behavior: "smooth" });
		} catch (e) {
			/* Scrollen darf nie stoeren */
		}
	}
	function clearParagraphHighlight() {
		// Es wird kein Seiten-Style mehr gesetzt -> es gibt nichts zurueckzusetzen.
		hlEl = null;
	}

	// Den naechsten Absatz aus der Queue an den Service-Worker schicken.
	async function speakNextAuto() {
		if (!autoReading) return;
		if (!autoQueue.length) {
			autoReading = false;
			clearParagraphHighlight();
			setState("stopped");
			resetProgress();
			setPauseButtonVisible(false);
			setPauseButtonState(false);
			return;
		}
		const item = autoQueue.shift();
		const text = item && typeof item === "object" ? item.text : item;
		// Sanft zum gerade vorgelesenen Absatz scrollen (keine Farb-Markierung mehr).
		if (item && typeof item === "object" && item.el) {
			highlightParagraph(item.el);
		} else {
			clearParagraphHighlight();
		}
		// Sichtbares Feedback: welcher Absatz gerade vorgelesen/eingelesen wird.
		showHint(
			"Liest vor… Absatz " +
				(autoTotal - autoQueue.length) +
				" von " +
				autoTotal,
		);
		let settings;
		try {
			settings = await window.VOSettings.load();
		} catch (e) {
			autoReading = false;
			return;
		}
		const engine = settings.activeEngine;
		const cfg = settings[engine];
		if (engine === "google" && !(settings.google.apiKey || "").trim()) {
			autoReading = false;
			autoQueue = [];
			clearParagraphHighlight();
			showHint(
				"Google braucht einen API-Key — bitte im Zahnrad eintragen.",
				true,
			);
			setState("stopped");
			return;
		}
		setState("loading");
		lastSpeechMs = estimateSpeechMs(text, cfg.rate);
		const pitch =
			engine === "edge" && settings.edge && settings.edge.pitch !== undefined
				? settings.edge.pitch
				: 0;
		sendMessage({
			type: MSG.SPEAK,
			engine,
			text,
			voice: cfg.voice,
			rate: cfg.rate,
			pitch,
			apiKey: engine === "google" ? settings.google.apiKey : undefined,
		});
	}

	// ----- Absatz-Erkennung fuer das Auto-Vorlesen -----------------------------
	// Findet das naechste lesbare Block-Element zu einem (Text-)Knoten.
	function closestReadableBlock(node) {
		const BLOCK = new Set([
			"P",
			"LI",
			"H1",
			"H2",
			"H3",
			"H4",
			"H5",
			"H6",
			"BLOCKQUOTE",
			"DD",
			"DT",
			"TD",
			"FIGCAPTION",
		]);
		let el = node && node.nodeType === 3 ? node.parentElement : node;
		while (el && el !== document.body) {
			if (el.nodeType === 1 && BLOCK.has(el.tagName)) return el;
			el = el.parentElement;
		}
		// Kein klassischer Block gefunden -> naechstes Element nehmen.
		return node && node.nodeType === 3 ? node.parentElement : node;
	}

	// Vorfahren/Elemente, die NICHT vorgelesen werden (Diagramme, Tabellen,
	// Navigation, Fuss-/Kopfzeilen, Quellen-/Literaturverzeichnisse, Code).
	const SKIP_ANCESTOR_SELECTOR =
		"figure, table, nav, aside, header, footer, code, pre, " +
		"svg, canvas, math, .reference, .references, .footnote, .footnotes, " +
		"#references, #footnotes, .citation, .citations, .reflist, " +
		".mw-references-wrap, .bibliography, .endnotes, .sources, " +
		'[role="note"], [role="navigation"], [role="contentinfo"], ' +
		'[role="doc-bibliography"], [role="doc-endnotes"], [role="doc-footnote"], ' +
		'[aria-hidden="true"]';

	// Sammelt ab startBlock alle nachfolgenden lesbaren Absaetze (in
	// Dokumentreihenfolge), gefiltert + bereinigt (ohne Fussnoten/Quellen).
	function collectParagraphsFrom(startBlock) {
		const result = [];
		if (!startBlock) return result;
		const candidates = Array.from(
			document.querySelectorAll(
				"p, li, h1, h2, h3, h4, h5, h6, blockquote, dd, dt, figcaption",
			),
		);
		// Startindex bestimmen: das erste Element, das startBlock IST oder ihn
		// enthaelt bzw. in Dokumentreihenfolge danach kommt.
		let startIdx = candidates.indexOf(startBlock);
		if (startIdx < 0) {
			// startBlock ist evtl. ein Container (z.B. DIV) -> erstes Kandidaten-
			// Element nehmen, das davon umschlossen wird oder danach kommt.
			for (let i = 0; i < candidates.length; i++) {
				const pos = startBlock.compareDocumentPosition(candidates[i]);
				if (
					candidates[i] === startBlock ||
					pos & Node.DOCUMENT_POSITION_CONTAINED_BY ||
					pos & Node.DOCUMENT_POSITION_FOLLOWING
				) {
					startIdx = i;
					break;
				}
			}
		}
		if (startIdx < 0) startIdx = 0;

		const MAX_PARAGRAPHS = 400; // Sicherheitsgrenze gegen Riesen-Seiten
		for (
			let i = startIdx;
			i < candidates.length && result.length < MAX_PARAGRAPHS;
			i++
		) {
			const el = candidates[i];
			// Verschachtelte Bloecke vermeiden (z.B. li, das ein p enthaelt):
			// nur Bloecke nehmen, die selbst keinen Listen-/Absatz-Block enthalten.
			if (el.querySelector("p, li, blockquote")) continue;
			// In einem zu ueberspringenden Vorfahren? (Tabelle, Quellen, nav, ...)
			if (el.closest(SKIP_ANCESTOR_SELECTOR)) continue;
			// Unsichtbares ueberspringen.
			if (!isVisible(el)) continue;
			const text = cleanParagraphText(el);
			if (text && text.length >= 2 && /[A-Za-zÀ-ÿ0-9]/.test(text)) {
				result.push({ text, el });
			}
		}
		return result;
	}

	function isVisible(el) {
		try {
			const r = el.getBoundingClientRect();
			if (r.width === 0 && r.height === 0) return false;
			const st = getComputedStyle(el);
			return st.display !== "none" && st.visibility !== "hidden";
		} catch (e) {
			return true;
		}
	}

	// Entfernt Quellenangaben, Fussnoten-Marker, Verweise, Diagramme und Code aus
	// einem (geklonten) Element/Fragment — IN PLACE. Wird von cleanParagraphText
	// und vom Markierungs-Pfad (cloneContents) gemeinsam genutzt.
	function stripRefs(root) {
		// 1) Bekannte Quellen-/Fussnoten-/Nicht-Text-Container hart entfernen.
		try {
			root
				.querySelectorAll(
					"sup, sub.reference, .reference, .references, .footnote, " +
						".footnotes, .footnote-ref, .citation, .citations, .cite, cite, " +
						".mw-editsection, .reflist, .mw-references-wrap, .bibliography, " +
						"figure, table, svg, canvas, math, code, pre, " +
						'[role="doc-noteref"], [role="doc-biblioref"], ' +
						'[role="doc-footnote"], [role="doc-endnotes"], ' +
						'a[href*="#cite"], a[href*="#ref"], a[href*="#fn"], a[href*="#note"]',
				)
				.forEach((n) => n.remove());
		} catch (e) {
			/* einzelne Selektoren koennen in Fragmenten fehlschlagen — egal */
		}
		// 2) Quellen-Chips: kurze Inline-Links/Buttons (wie "oreilly", "Claude",
		//    "[1]", "(2024)") sind fast immer Verweise, kein Lesetext. Sie haben
		//    sehr wenig Text -> entfernen. Laengere Link-Texte (Satzteile) bleiben.
		try {
			root
				.querySelectorAll('a, button, [role="button"], [role="link"]')
				.forEach((n) => {
					const txt = (n.textContent || "").trim();
					const words = txt.split(/\s+/).filter(Boolean);
					// <= 3 Woerter ODER <= 25 Zeichen ODER nur Zahlen/Klammern = Quelle.
					if (
						words.length <= 3 ||
						txt.length <= 25 ||
						/^[\s\d.,;:()\[\]–-]+$/.test(txt)
					) {
						n.remove();
					}
				});
		} catch (e) {
			/* egal */
		}
	}

	// Macht aus einem (gefilterten) Roh-Text einen sauberen Vorlese-String.
	function finalizeText(raw) {
		return String(raw || "")
			.replace(/\s+/g, " ")
			.replace(/\[\s*\d+\s*\]/g, "") // [1], [12]
			.replace(/\[\s*[a-z]\s*\]/gi, "") // [a]
			.replace(/\s{2,}/g, " ")
			.trim();
	}

	// Liefert den vorlesbaren Text eines Block-Elements, OHNE Fussnoten-Marker,
	// Quellen-Verweise, Diagramme, Code etc. (auf einer Klon-Kopie gefiltert).
	function cleanParagraphText(el) {
		let clone;
		try {
			clone = el.cloneNode(true);
		} catch (e) {
			return finalizeText(el.innerText || el.textContent || "");
		}
		stripRefs(clone);
		return finalizeText(clone.innerText || clone.textContent || "");
	}

	// ----- Pause-Button Klick --------------------------------------------------
	function onPauseClick() {
		if (window.VODiag)
			window.VODiag.log("INFO", "UI_EREIGNIS", "overlay.pause_klick", {});
		sendMessage({ type: MSG.PAUSE_RESUME_TOGGLE });
	}

	// ----- Lautsprecher-Aktion -------------------------------------------------
	async function onSpeakerClick() {
		if (isPlaying) {
			if (window.VODiag)
				window.VODiag.log("INFO", "UI_EREIGNIS", "speaker_klick:stop", {});
			// Manueller Stop beendet auch eine laufende automatische Vorlesung.
			autoReading = false;
			autoQueue = [];
			clearParagraphHighlight();
			sendMessage({ type: MSG.STOP });
			setState("stopped");
			// Pause-Zustand und Fortschritt zurücksetzen
			isPaused = false;
			resetProgress();
			setPauseButtonVisible(false);
			setPauseButtonState(false);
			return;
		}
		captureSelection();
		const text = (lastSelection || "").trim();
		if (!text) {
			if (window.VODiag)
				window.VODiag.log(
					"WARN",
					"UI_EREIGNIS",
					"speaker_klick:keine_markierung",
					{},
				);
			showHint("Bitte zuerst Text markieren.");
			return;
		}

		// IMMER nur den MARKIERTEN Text vorlesen — in Absaetze aufgeteilt und von
		// Quellenangaben/Fussnoten bereinigt. cloneContents() respektiert die
		// Markierungsgrenzen, daher wird NICHTS vor dem ersten markierten Wort
		// gelesen. Jeder Absatz wird einzeln synthetisiert (erst wenn er dran ist)
		// -> kein Abbruch bei sehr viel markiertem Text.
		const selParas = paragraphsFromSelection();
		if (selParas.length) {
			// Google-Key-Pruefung vorab (speakNextAuto bricht sonst pro Absatz ab).
			const settings = await window.VOSettings.load();
			if (
				settings.activeEngine === "google" &&
				!(settings.google.apiKey || "").trim()
			) {
				showHint(
					"Google braucht einen API-Key — bitte im Zahnrad eintragen.",
					true,
				);
				return;
			}
			if (window.VODiag)
				window.VODiag.log("INFO", "NUTZUNG", "speaker_klick:absatz_queue", {
					absaetze: selParas.length,
				});
			startParagraphQueue(selParas);
			return;
		}

		// Fallback (sollte selten greifen): reiner markierter Text, gereinigt.
		const settings = await window.VOSettings.load();
		const engine = settings.activeEngine;
		const cfg = settings[engine];
		if (engine === "google" && !(settings.google.apiKey || "").trim()) {
			showHint(
				"Google braucht einen API-Key — bitte im Zahnrad eintragen.",
				true,
			);
			return;
		}
		setState("loading");
		lastSpeechMs = estimateSpeechMs(text, cfg.rate);
		const pitch =
			engine === "edge" && settings.edge && settings.edge.pitch !== undefined
				? settings.edge.pitch
				: 0;
		sendMessage({
			type: MSG.SPEAK,
			engine,
			text: finalizeText(text),
			voice: cfg.voice,
			rate: cfg.rate,
			pitch,
			apiKey: engine === "google" ? settings.google.apiKey : undefined,
		});
	}

	// Startet eine absatzweise Vorlesung aus einer Liste von Absatz-Texten.
	// Gemeinsam genutzt von der Mehrfach-Markierung (Lautsprecher/Haekchen) und
	// dem A-Auto-Modus. Jeder Absatz wird einzeln synthetisiert (siehe
	// speakNextAuto), daher kein Crash bei sehr viel Text.
	function startParagraphQueue(paras) {
		// Akzeptiert sowohl { text, el }-Objekte als auch reine Strings.
		autoQueue = (paras || [])
			.map((p) => (typeof p === "string" ? { text: p, el: null } : p))
			.filter((p) => p && p.text && p.text.trim().length > 1);
		if (!autoQueue.length) {
			showHint("Kein lesbarer Text gefunden.");
			return false;
		}
		autoReading = true;
		autoTotal = autoQueue.length;
		isPaused = false;
		showHint(
			"Liest vor… " + autoTotal + (autoTotal === 1 ? " Absatz" : " Absätze"),
		);
		speakNextAuto();
		return true;
	}

	// ----- Kommunikation mit dem Service-Worker --------------------------------
	function sendMessage(msg) {
		try {
			chrome.runtime.sendMessage(msg, () => void chrome.runtime.lastError);
		} catch (e) {
			if (window.VODiag)
				window.VODiag.log("ERROR", "FEHLER", "overlay.sendMessage:fehler", {
					typ: msg && msg.type,
					message: String((e && e.message) || e),
				});
			showHint("Erweiterung nicht erreichbar — Seite neu laden.", true);
		}
	}

	// Stimmenliste vom Worker holen (Antwort: { voices } oder { error }).
	function requestVoices(engine, apiKey) {
		return new Promise((resolve) => {
			try {
				chrome.runtime.sendMessage(
					{ type: MSG.GET_VOICES, engine, apiKey },
					(res) => {
						if (chrome.runtime.lastError || !res) {
							resolve({
								voices: [],
								error: "Stimmen konnten nicht geladen werden.",
							});
							return;
						}
						resolve(res);
					},
				);
			} catch (e) {
				resolve({ voices: [], error: "Erweiterung nicht erreichbar." });
			}
		});
	}

	// Status-/Befehls-Nachrichten vom Worker empfangen
	// Toolbar-Klick (vom Service-Worker) und Selbsttest oeffnen das Panel hierueber.
	window.VOOverlay = window.VOOverlay || {};
	window.VOOverlay.toggleSettings = () => togglePanel();

	chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
		if (!msg || typeof msg !== "object") return;

		if (msg.type === "VO_GET_PAGE_STATE") {
			sendResponse({ host: PAGE_HOST, disabled: overlayPageDisabled });
			return;
		}

		if (msg.type === MSG.OPEN_SETTINGS) {
			if (overlayPageDisabled) return;
			togglePanel();
			return;
		}

		// Nach "Erweiterung aktualisieren": die Seite kurz danach selbst neu laden,
		// damit ein frisches Content-Script geladen wird. location.reload() ist eine
		// Seiten-API und funktioniert auch, nachdem der Extension-Context durch den
		// Reload ungueltig wurde (chrome.* APIs gehen dann nicht mehr, location schon).
		if (msg.type === "RELOAD_PAGE_SOON") {
			setTimeout(() => {
				try {
					location.reload();
				} catch (_) {
					/* egal */
				}
			}, 800);
			return;
		}

		if (msg.type === MSG.STATE) {
			// Vorhandene Diagnose-Sonde erhalten
			if (window.VODiag)
				window.VODiag.log(
					msg.state === "error" ? "ERROR" : "INFO",
					msg.state === "error" ? "FEHLER" : "ZUSTAND",
					"overlay.status_empfangen",
					{ state: msg.state, message: msg.message },
				);

			if (msg.state === "error") {
				clearEndWatchdog();
				setState("stopped");
				showHint(msg.message || "Es ist ein Fehler aufgetreten.", true);
				// Aufräumen bei Fehler — auch die automatische Vorlesung abbrechen.
				isPaused = false;
				autoReading = false;
				autoQueue = [];
				clearParagraphHighlight();
				resetProgress();
				setPauseButtonVisible(false);
				setPauseButtonState(false);
			} else if (msg.state === "playing") {
				setState("playing");
				// Pause-Button einblenden wenn Wiedergabe läuft
				setPauseButtonVisible(true);
				setPauseButtonState(false);
				isPaused = false;
				// Fortschrittsbalken ueber die geschaetzte Dauer laufen lassen.
				startProgressAnim(lastSpeechMs);
				// Sicherheitsnetz scharf schalten, falls das Ende-Signal ausbleibt.
				armEndWatchdog();
			} else if (msg.state === "paused") {
				isPaused = true;
				// Icon auf Play umschalten (bereit zum Fortsetzen)
				setPauseButtonState(true);
				pauseProgressAnim();
				clearEndWatchdog(); // bei Pause ist ein langes Ausbleiben normal
				if (window.VODiag)
					window.VODiag.log("INFO", "ZUSTAND", "overlay.status_paused", {});
			} else if (msg.state === "resumed") {
				isPaused = false;
				// Icon auf Pause zurück (läuft wieder)
				setPauseButtonState(false);
				resumeProgressAnim();
				armEndWatchdog(); // Wiedergabe laeuft wieder -> Netz erneut scharf
				if (window.VODiag)
					window.VODiag.log("INFO", "ZUSTAND", "overlay.status_resumed", {});
			} else if (msg.state === "stopped") {
				// Aktuelles Sicherheitsnetz abbauen — dieser Absatz/Text ist fertig.
				// Folgt eine weitere Passage (Auto-Lese-Modus), schaltet deren
				// "playing"-Event den Watchdog ohnehin wieder scharf.
				clearEndWatchdog();
				// Im Auto-Lese-Modus bedeutet "stopped" nur: dieser Absatz ist
				// fertig -> direkt den naechsten Absatz vorlesen, ohne den Status
				// zurueckzusetzen (sonst flackert der Button zwischen den Absaetzen).
				if (autoReading && autoQueue.length > 0) {
					speakNextAuto();
				} else {
					autoReading = false;
					clearParagraphHighlight();
					setState("stopped");
					isPaused = false;
					resetProgress();
					setPauseButtonVisible(false);
					setPauseButtonState(false);
				}
			} else {
				setState(msg.state);
			}
			if (panelStatusHandler) panelStatusHandler(msg.state, msg.message);
		} else if (msg.type === MSG.PROGRESS) {
			// Chunk-basierter Fortschritt steuert den Balken NICHT mehr (kurzer Text
			// = 1 Chunk -> bliebe bei 0%). Der Balken laeuft jetzt zeitbasiert ueber
			// die Status-Events playing/paused/resumed. Hier bewusst nichts tun.
		} else if (msg.type === MSG.SPEAK_COMMAND) {
			if (overlayPageDisabled) return;
			if (window.VODiag)
				window.VODiag.log("INFO", "UI_EREIGNIS", "overlay.tastenkuerzel", {});
			onSpeakerClick();
		}
	});

	// ----- Diagnose-Sonden für Pause/Resume ------------------------------------
	// Diese werden im onPauseClick und über TTS_STATE-Handler ausgelöst.
	// Zusätzlich: direkte Sonden für Pause/Resume-Bestätigung vom SW
	function onPauseConfirmed() {
		if (window.VODiag)
			window.VODiag.log("INFO", "NUTZUNG", "overlay.pause_ausgeloest", {});
	}
	function onResumeConfirmed() {
		if (window.VODiag)
			window.VODiag.log("INFO", "NUTZUNG", "overlay.resume_ausgeloest", {});
	}

	// ----- Einstellungs-Panel --------------------------------------------------
	function togglePanel() {
		if (!panelBuilt) {
			buildPanel();
			panelBuilt = true;
		}
		const open = panel.classList.toggle("vo-open");
		if (window.VODiag)
			window.VODiag.log("INFO", "UI_EREIGNIS", "panel_toggle", { open });
		if (open) openPanelPositioned();
	}

	// Beim Oeffnen: hat der Benutzer das Panel schon einmal verschoben, wird die
	// gespeicherte Position genutzt — sonst automatisch neben dem Overlay.
	function openPanelPositioned() {
		window.VOSettings.getPanelPosition()
			.then((pos) => {
				if (pos) {
					applyPanelPosition(pos.left, pos.top);
					// Nach dem ersten Frame erneut einpassen (finale Panel-Hoehe).
					requestAnimationFrame(() => {
						if (panel.classList.contains("vo-open"))
							applyPanelPosition(pos.left, pos.top);
					});
				} else {
					positionPanel();
				}
			})
			.catch(() => positionPanel());
	}

	// Panel an eine konkrete Position setzen, geclamppt auf den sichtbaren Bereich.
	function applyPanelPosition(left, top) {
		const pw = panel.offsetWidth || 340;
		const ph = panel.offsetHeight || 300;
		const maxLeft = Math.max(8, window.innerWidth - pw - 8);
		const maxTop = Math.max(8, window.innerHeight - ph - 8);
		const l = Math.min(Math.max(8, left), maxLeft);
		const t = Math.min(Math.max(8, top), maxTop);
		panel.style.left = l + "px";
		panel.style.top = t + "px";
	}

	// Automatische Positionierung neben dem Overlay (wenn keine gespeicherte
	// Position existiert).
	function positionPanel() {
		doPositionPanel();
		// Beim allerersten Oeffnen sind CSS und Inhalt evtl. noch nicht final
		// gerendert, sodass die gemessene Panel-Hoehe zu klein ist und das Panel
		// unten aus dem Bildschirm ragt (war erst beim zweiten Klick korrekt).
		// Nach dem naechsten Frame ist das Layout sicher angewandt -> erneut clampen.
		requestAnimationFrame(() => {
			if (panel.classList.contains("vo-open")) doPositionPanel();
		});
	}

	function doPositionPanel() {
		const c = container.getBoundingClientRect();
		const pw = panel.offsetWidth || 340;
		const ph = panel.offsetHeight || 300;
		let left = c.left - pw - 12;
		if (left < 8) left = Math.min(window.innerWidth - pw - 8, c.right + 12);
		let top = c.top;
		if (top + ph > window.innerHeight - 8)
			top = Math.max(8, window.innerHeight - ph - 8);
		panel.style.left = Math.max(8, left) + "px";
		panel.style.top = Math.max(8, top) + "px";
		logLayout("EinstellungenPanel", panel, { verankert: "neben Overlay" });
	}

	function buildPanel() {
		panel.innerHTML =
			'<div class="vo-panel-head">' +
			'<h2 class="vo-panel-title">Vorlesen — Einstellungen</h2>' +
			'<button class="vo-close" type="button" title="Schließen">&times;</button>' +
			"</div>" +
			'<div class="vo-panel-body">' +
			// Aktive Engine
			'<div class="vo-section">' +
			'<span class="vo-label">Aktive Stimme (nutzt der Lautsprecher)</span>' +
			'<div class="vo-engine-switch">' +
			'<div class="vo-engine-opt" data-engine="edge">Edge TTS</div>' +
			'<div class="vo-engine-opt" data-engine="google">Google Chirp 3 HD</div>' +
			"</div></div>" +
			// Reiter
			'<div class="vo-tabs">' +
			'<button class="vo-tab" type="button" data-tab="edge">Edge TTS</button>' +
			'<button class="vo-tab" type="button" data-tab="google">Google Chirp 3 HD</button>' +
			"</div>" +
			// Edge-Seite
			'<div class="vo-tabpage" data-page="edge">' +
			'<div class="vo-section"><span class="vo-label">Stimme</span>' +
			'<div class="vo-row">' +
			'<select class="vo-select" data-role="edge-voice"></select>' +
			'<button class="vo-star" type="button" data-role="edge-star" title="Diese Stimme mit Stern markieren">☆</button>' +
			"</div></div>" +
			'<div class="vo-section"><span class="vo-label">Vorlese-Tempo</span>' +
			'<div class="vo-row"><input class="vo-range" type="range" min="0.5" max="2" step="0.1" data-role="edge-rate">' +
			'<span class="vo-range-val" data-role="edge-rate-val"></span></div></div>' +
			// Tonhöhe (Pitch) — nur für Edge
			'<div class="vo-section"><span class="vo-label">Tonhöhe (Halbtöne)</span>' +
			'<div class="vo-row"><input class="vo-range" type="range" min="-50" max="50" step="1" data-role="edge-pitch">' +
			'<span class="vo-range-val" data-role="edge-pitch-val"></span></div></div>' +
			'<button class="vo-test" type="button" data-role="edge-test">Test — Beispielsatz vorlesen</button>' +
			'<div class="vo-status" data-role="edge-status"></div>' +
			"</div>" +
			// Google-Seite
			'<div class="vo-tabpage" data-page="google">' +
			'<div class="vo-section"><span class="vo-label">Google-Cloud-API-Key</span>' +
			'<input class="vo-input" type="password" autocomplete="off" spellcheck="false" placeholder="AIza…" data-role="google-key">' +
			'<p class="vo-help">Eigener Google-Cloud-Key mit aktivierter Text-to-Speech-API. ' +
			"Der Gemini-Key funktioniert hier <b>nicht</b>.</p></div>" +
			'<div class="vo-section"><span class="vo-label">Stimme</span>' +
			'<div class="vo-row">' +
			'<select class="vo-select" data-role="google-voice" disabled></select>' +
			'<button class="vo-star" type="button" data-role="google-star" title="Diese Stimme mit Stern markieren">☆</button>' +
			"</div></div>" +
			'<div class="vo-section"><span class="vo-label">Vorlese-Tempo</span>' +
			'<div class="vo-row"><input class="vo-range" type="range" min="0.5" max="2" step="0.1" data-role="google-rate">' +
			'<span class="vo-range-val" data-role="google-rate-val"></span></div></div>' +
			'<button class="vo-test" type="button" data-role="google-test">Test — Beispielsatz vorlesen</button>' +
			'<div class="vo-status" data-role="google-status"></div>' +
			"</div>" +
			// Haekchen: markierten Text automatisch vorlesen (unabhaengig vom A-Button).
			'<div class="vo-section">' +
			'<label class="vo-check-row">' +
			'<input type="checkbox" class="vo-check" data-role="auto-speak">' +
			"<span>Markierten Text automatisch vorlesen</span>" +
			"</label>" +
			'<p class="vo-help">Sobald du Text markierst, wird er vorgelesen. ' +
			"Der A-Button am Overlay ist etwas anderes: er liest ab dem markierten " +
			"Wort den ganzen folgenden Text Absatz fuer Absatz vor.</p>" +
			"</div>" +
			// Aktualisieren-Button (letztes Panel-Element): laedt die Erweiterung
			// neu, OHNE den gespeicherten API-Key/Stimme/Position zu verlieren.
			'<div class="vo-section">' +
			'<button class="vo-reload" type="button" data-role="reload">Auf neue Version aktualisieren</button>' +
			'<p class="vo-help">Laedt die Erweiterung mit der neuesten Version neu. ' +
			"API-Key, Stimme und Position bleiben erhalten. Nutze das nach einem Update — " +
			"NICHT die Erweiterung entfernen und neu laden, das wuerde die Einstellungen loeschen.</p>" +
			"</div>" +
			"</div>";

		const $ = (role) => panel.querySelector('[data-role="' + role + '"]');
		panel
			.querySelector(".vo-close")
			.addEventListener("click", () => panel.classList.remove("vo-open"));

		// Panel per Titelleiste verschiebbar machen (Position wird gemerkt).
		const head = panel.querySelector(".vo-panel-head");
		if (head) {
			head.addEventListener("mousedown", (e) => {
				if (e.target.closest(".vo-close")) return; // Schliessen nicht ziehen
				if (e.button !== 0) return;
				e.preventDefault();
				const rect = panel.getBoundingClientRect();
				const startX = e.clientX;
				const startY = e.clientY;
				const origLeft = rect.left;
				const origTop = rect.top;
				let moved = false;
				const onMove = (ev) => {
					const dx = ev.clientX - startX;
					const dy = ev.clientY - startY;
					if (!moved && Math.hypot(dx, dy) > 3) moved = true;
					if (moved) applyPanelPosition(origLeft + dx, origTop + dy);
				};
				const onUp = () => {
					window.removeEventListener("mousemove", onMove, true);
					window.removeEventListener("mouseup", onUp, true);
					if (moved) {
						const r = panel.getBoundingClientRect();
						window.VOSettings.setPanelPosition(r.left, r.top);
						if (window.VODiag)
							window.VODiag.log("INFO", "UI_EREIGNIS", "panel_verschoben", {
								left: Math.round(r.left),
								top: Math.round(r.top),
							});
					}
				};
				window.addEventListener("mousemove", onMove, true);
				window.addEventListener("mouseup", onUp, true);
			});
		}

		// Haekchen "Markierten Text automatisch vorlesen".
		const autoSpeakCb = $("auto-speak");
		if (autoSpeakCb) {
			autoSpeakCb.addEventListener("change", async (e) => {
				current.autoSpeak = !!e.target.checked;
				await persist();
				// Overlay-Haekchen-Viereck synchron halten.
				autoSpeakOn = current.autoSpeak;
				applyAutoSpeakBox();
			});
		}

		// Aktualisieren-Button: Erweiterung neu laden (Speicher bleibt erhalten).
		// WICHTIG: chrome.runtime.reload() laedt die Erweiterung neu, macht damit
		// aber das Overlay in DIESEM Tab zu einer "verwaisten" Instanz mit totem
		// Verbindungskanal (Vorlesen ginge nicht mehr, der Button drehte ewig).
		// Deshalb laedt das Content-Script danach die SEITE neu -> frisches Overlay
		// mit der neuen Version. Erst Extension neu laden, dann (verzoegert) die Seite.
		const reloadBtn = $("reload");
		if (reloadBtn) {
			reloadBtn.addEventListener("click", () => {
				reloadBtn.textContent = "Wird aktualisiert…";
				reloadBtn.disabled = true;
				let reloaded = false;
				const reloadPage = () => {
					if (reloaded) return;
					reloaded = true;
					try {
						location.reload();
					} catch (e) {
						/* egal */
					}
				};
				try {
					chrome.runtime.sendMessage(
						{ type: MSG.RELOAD_EXTENSION },
						() => void chrome.runtime.lastError,
					);
				} catch (e) {
					/* Kanal evtl. schon weg — Seite trotzdem neu laden */
				}
				// Der Extension genug Zeit geben, sich neu zu laden, dann die Seite
				// neu laden (re-injiziert das frische Content-Script).
				setTimeout(reloadPage, 600);
			});
		}

		const engineOpts = panel.querySelectorAll(".vo-engine-opt");
		const tabs = panel.querySelectorAll(".vo-tab");
		const pages = panel.querySelectorAll(".vo-tabpage");

		let current = null; // aktuelle Einstellungen im Speicher
		let lastEdgeVoices = []; // zuletzt geladene Edge-Stimmen (fuer Stern-Refill)
		let lastGoogleVoices = []; // zuletzt geladene Google-Stimmen (fuer Stern-Refill)

		function showTab(name) {
			tabs.forEach((t) =>
				t.classList.toggle("vo-active", t.dataset.tab === name),
			);
			pages.forEach((p) =>
				p.classList.toggle("vo-active", p.dataset.page === name),
			);
		}
		function markEngine(name) {
			engineOpts.forEach((o) =>
				o.classList.toggle("vo-active", o.dataset.engine === name),
			);
		}
		async function persist() {
			current = await window.VOSettings.save(current);
		}
		function setRateUI(roleRange, roleVal, value) {
			$(roleRange).value = String(value);
			$(roleVal).textContent = Number(value).toFixed(1) + "×";
		}
		function setPitchUI(roleRange, roleVal, value) {
			const v = Number(value) || 0;
			$(roleRange).value = String(v);
			$(roleVal).textContent = (v >= 0 ? "+" : "") + v + " st";
		}
		function showStatus(role, message, kind) {
			const el = $(role);
			el.textContent = message || "";
			el.classList.toggle("vo-ok", kind === "ok");
			el.classList.toggle("vo-err", kind === "err");
		}
		// Pruefen ob eine Stimmen-ID als Favorit (Stern) markiert ist.
		function isFavoriteVoice(id) {
			return !!(
				current &&
				Array.isArray(current.favoriteVoices) &&
				current.favoriteVoices.includes(id)
			);
		}

		function fillVoiceSelect(sel, voices, selectedId) {
			sel.innerHTML = "";
			// Favoriten nach oben sortieren (stabil), Rest in Original-Reihenfolge.
			const sorted = voices
				.map((v, i) => ({ v, i }))
				.sort((a, b) => {
					const fa = isFavoriteVoice(a.v.id) ? 0 : 1;
					const fb = isFavoriteVoice(b.v.id) ? 0 : 1;
					return fa - fb || a.i - b.i;
				})
				.map((x) => x.v);
			for (const v of sorted) {
				const opt = document.createElement("option");
				opt.value = v.id;
				// Favoriten bekommen einen Stern vor dem Namen.
				opt.textContent = (isFavoriteVoice(v.id) ? "★ " : "") + v.label;
				if (v.id === selectedId) opt.selected = true;
				sel.appendChild(opt);
			}
		}

		// Stern-Button (☆/★) an den aktuell im Dropdown gewaehlten Wert anpassen.
		function updateStarButton(starRole, selectRole) {
			const btn = $(starRole);
			const sel = $(selectRole);
			if (!btn || !sel) return;
			const id = sel.value;
			const fav = isFavoriteVoice(id);
			btn.textContent = fav ? "★" : "☆";
			btn.classList.toggle("vo-star-on", fav);
			btn.title = fav ? "Stern entfernen" : "Diese Stimme mit Stern markieren";
		}

		// Favorit-Status der aktuell gewaehlten Stimme umschalten + speichern.
		async function toggleFavoriteVoice(selectRole, starRole, engineKey) {
			const sel = $(selectRole);
			if (!sel || !sel.value) return;
			const id = sel.value;
			if (!Array.isArray(current.favoriteVoices)) current.favoriteVoices = [];
			const pos = current.favoriteVoices.indexOf(id);
			if (pos >= 0) current.favoriteVoices.splice(pos, 1);
			else current.favoriteVoices.push(id);
			await persist();
			if (window.VODiag)
				window.VODiag.log("INFO", "UI_EREIGNIS", "overlay.stimme_favorit", {
					engine: engineKey,
					markiert: pos < 0,
				});
			// Liste neu aufbauen (Favoriten nach oben), Auswahl + Stern aktualisieren.
			const voices = engineKey === "google" ? lastGoogleVoices : lastEdgeVoices;
			if (voices && voices.length) fillVoiceSelect(sel, voices, id);
			updateStarButton(starRole, selectRole);
		}

		async function loadEdgeVoices() {
			const sel = $("edge-voice");
			sel.innerHTML = "<option>Lädt…</option>";
			sel.disabled = true;
			const res = await requestVoices("edge");
			const voices = (res && res.voices) || [];
			if (!voices.length) {
				sel.innerHTML = "<option>Keine Stimmen gefunden</option>";
				return;
			}
			lastEdgeVoices = voices;
			fillVoiceSelect(sel, voices, current.edge.voice);
			sel.disabled = false;
			if (!voices.some((v) => v.id === current.edge.voice)) {
				current.edge.voice = sel.value;
				await persist();
			}
			updateStarButton("edge-star", "edge-voice");
		}

		async function loadGoogleVoices() {
			const sel = $("google-voice");
			const key = (current.google.apiKey || "").trim();
			if (!key) {
				sel.innerHTML = "<option>Erst API-Key eintragen</option>";
				sel.disabled = true;
				return;
			}
			sel.innerHTML = "<option>Lädt…</option>";
			sel.disabled = true;
			const res = await requestVoices("google", key);
			if (res && res.error) {
				sel.innerHTML = "<option>Stimmen nicht ladbar</option>";
				showStatus("google-status", res.error, "err");
				return;
			}
			const voices = (res && res.voices) || [];
			if (!voices.length) {
				sel.innerHTML = "<option>Keine Chirp-3-HD-Stimmen</option>";
				return;
			}
			lastGoogleVoices = voices;
			fillVoiceSelect(sel, voices, current.google.voice);
			sel.disabled = false;
			showStatus("google-status", "", "");
			if (!voices.some((v) => v.id === current.google.voice)) {
				current.google.voice = sel.value;
				await persist();
			}
			updateStarButton("google-star", "google-voice");
		}

		// ----- Verdrahtung -----
		engineOpts.forEach((o) =>
			o.addEventListener("click", async () => {
				current.activeEngine = o.dataset.engine;
				markEngine(current.activeEngine);
				showTab(current.activeEngine);
				await persist();
			}),
		);
		tabs.forEach((t) =>
			t.addEventListener("click", () => showTab(t.dataset.tab)),
		);

		$("edge-voice").addEventListener("change", async (e) => {
			current.edge.voice = e.target.value;
			await persist();
			updateStarButton("edge-star", "edge-voice");
		});
		$("edge-star").addEventListener("click", () =>
			toggleFavoriteVoice("edge-voice", "edge-star", "edge"),
		);
		$("edge-rate").addEventListener("input", (e) => {
			$("edge-rate-val").textContent = Number(e.target.value).toFixed(1) + "×";
		});
		$("edge-rate").addEventListener("change", async (e) => {
			current.edge.rate = window.VOSettings.clampRate(e.target.value);
			await persist();
		});

		// Tonhöhe (Pitch) für Edge
		$("edge-pitch").addEventListener("input", (e) => {
			const v = parseInt(e.target.value, 10) || 0;
			$("edge-pitch-val").textContent = (v >= 0 ? "+" : "") + v + " st";
		});
		$("edge-pitch").addEventListener("change", async (e) => {
			const v = Math.max(-50, Math.min(50, parseInt(e.target.value, 10) || 0));
			if (!current.edge) current.edge = {};
			current.edge.pitch = v;
			await persist();
		});

		$("edge-test").addEventListener("click", () => {
			showStatus("edge-status", "Wird vorgelesen…", "");
			sendMessage({
				type: MSG.TEST,
				engine: "edge",
				text: SAMPLE_TEXT,
				voice: $("edge-voice").value,
				rate: window.VOSettings.clampRate($("edge-rate").value),
				pitch: parseInt($("edge-pitch").value, 10) || 0,
			});
		});

		$("google-key").addEventListener("change", async (e) => {
			current.google.apiKey = e.target.value.trim();
			await persist();
			await loadGoogleVoices();
		});
		$("google-voice").addEventListener("change", async (e) => {
			current.google.voice = e.target.value;
			await persist();
			updateStarButton("google-star", "google-voice");
		});
		$("google-star").addEventListener("click", () =>
			toggleFavoriteVoice("google-voice", "google-star", "google"),
		);
		$("google-rate").addEventListener("input", (e) => {
			$("google-rate-val").textContent =
				Number(e.target.value).toFixed(1) + "×";
		});
		$("google-rate").addEventListener("change", async (e) => {
			current.google.rate = window.VOSettings.clampRate(e.target.value);
			await persist();
		});
		$("google-test").addEventListener("click", () => {
			const key = (current.google.apiKey || "").trim();
			if (!key) {
				showStatus(
					"google-status",
					"Bitte zuerst den API-Key eintragen.",
					"err",
				);
				return;
			}
			showStatus("google-status", "Wird vorgelesen…", "");
			sendMessage({
				type: MSG.TEST,
				engine: "google",
				text: SAMPLE_TEXT,
				voice: $("google-voice").value,
				rate: window.VOSettings.clampRate($("google-rate").value),
				apiKey: key,
			});
		});

		// Status/Fehler aus der Wiedergabe auch im Panel zeigen.
		panelStatusHandler = (state, message) => {
			const page = panel.querySelector(".vo-tabpage.vo-active");
			if (!page) return;
			const el = page.querySelector(".vo-status");
			if (!el) return;
			if (state === "playing") {
				el.textContent = "Spielt…";
				el.className = "vo-status vo-ok";
			} else if (state === "stopped") {
				el.textContent = "";
				el.className = "vo-status";
			} else if (state === "error") {
				el.textContent = message || "Es ist ein Fehler aufgetreten.";
				el.className = "vo-status vo-err";
			}
		};

		// ----- Initialisieren -----
		(async () => {
			current = await window.VOSettings.load();
			markEngine(current.activeEngine);
			showTab(current.activeEngine);
			setRateUI("edge-rate", "edge-rate-val", current.edge.rate);
			// Tonhöhe initialisieren (Default 0 wenn noch nicht vorhanden)
			const pitch =
				current.edge && current.edge.pitch !== undefined
					? current.edge.pitch
					: 0;
			setPitchUI("edge-pitch", "edge-pitch-val", pitch);
			setRateUI("google-rate", "google-rate-val", current.google.rate);
			$("google-key").value = current.google.apiKey || "";
			if ($("auto-speak")) $("auto-speak").checked = !!current.autoSpeak;
			await loadEdgeVoices();
			await loadGoogleVoices();
			// Nach dem (asynchronen) Laden der Stimmen kann sich die Panel-Hoehe
			// geaendert haben — falls noch offen, erneut sauber positionieren.
			if (panel.classList.contains("vo-open")) positionPanel();
		})();
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

	// Nicht verwendete Referenzen sauber halten (verhindert Linter-Warnungen)
	void onPauseConfirmed;
	void onResumeConfirmed;
})();
