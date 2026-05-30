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
		CHUNK_TIMING: "TTS_CHUNK_TIMING", // SW → Content: Wort-Liste {words, chunkIndex, totalChunks}
	};

	const SAMPLE_TEXT =
		"Dies ist ein Beispielsatz, mit dem du die gewählte Stimme und das Tempo prüfen kannst.";

	let lastSelection = "";
	let isPlaying = false;
	let hintTimer = null;
	// Vom Panel gesetzte Funktion, um Status-/Fehlermeldungen im Panel anzuzeigen.
	let panelStatusHandler = null;

	// ----- Neue Zustands-Variablen für Pause, Fortschritt und Wort-Highlight ---
	let isPaused = false;
	let highlightTimer = null;
	let currentChunkWords = [];
	let currentChunkWordIndex = 0;
	let lastHighlightedRange = null;
	let progressChunk = 0;
	let progressTotal = 0;
	let autoSpeakTimer = null;

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

	// Auto-Speak: bei Markierungsänderung ggf. automatisch vorlesen
	document.addEventListener(
		"selectionchange",
		() => {
			// Einstellungen asynchron prüfen — wir speichern den Snapshot lokal
			window.VOSettings.load()
				.then((settings) => {
					const autoSpeak = settings && settings.autoSpeak;
					if (!autoSpeak) return;
					try {
						const sel = window.getSelection();
						const text = sel ? sel.toString().trim() : "";
						if (!text) {
							if (autoSpeakTimer) {
								clearTimeout(autoSpeakTimer);
								autoSpeakTimer = null;
							}
							return;
						}
						if (text === lastSelection && isPlaying) return;
						if (autoSpeakTimer) clearTimeout(autoSpeakTimer);
						if (window.VODiag)
							window.VODiag.log(
								"INFO",
								"UI_EREIGNIS",
								"overlay.auto_speak_timer_gestartet",
								{ textLen: text.length },
							);
						autoSpeakTimer = setTimeout(() => {
							autoSpeakTimer = null;
							if (window.VODiag)
								window.VODiag.log(
									"INFO",
									"NUTZUNG",
									"overlay.auto_speak_ausgefuehrt",
									{ textLen: text.length },
								);
							onSpeakerClick();
						}, 1200);
					} catch (e) {
						/* ignorieren */
					}
				})
				.catch(() => {
					/* ignorieren */
				});
		},
		true,
	);

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
	const ICON_GEAR =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19.14 12.94a7.49 7.49 0 0 0 .05-.94 7.49 7.49 0 0 0-.05-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.61-.22l-2.39.96a7 7 0 0 0-1.62-.94l-.36-2.54a.5.5 0 0 0-.5-.42h-3.84a.5.5 0 0 0-.5.42l-.36 2.54a7 7 0 0 0-1.62.94l-2.39-.96a.5.5 0 0 0-.61.22L2.27 8.3a.5.5 0 0 0 .12.64l2.03 1.58c-.03.31-.05.62-.05.94s.02.63.05.94l-2.03 1.58a.5.5 0 0 0-.12.64l1.92 3.32a.5.5 0 0 0 .61.22l2.39-.96c.5.38 1.04.7 1.62.94l.36 2.54a.5.5 0 0 0 .5.42h3.84a.5.5 0 0 0 .5-.42l.36-2.54a7 7 0 0 0 1.62-.94l2.39.96a.5.5 0 0 0 .61-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58zM12 15.5A3.5 3.5 0 1 1 12 8.5a3.5 3.5 0 0 1 0 7z"/></svg>';
	// SVG-Icons für den Pause-Button (Pause-Balken und Play-Dreieck)
	const ICON_PAUSE =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>';
	const ICON_PLAY_TRIANGLE =
		'<svg viewBox="0 0 24 24" aria-hidden="true"><polygon points="5,3 19,12 5,21"/></svg>';

	const container = document.createElement("div");
	container.className = "vo-container";
	container.innerHTML =
		'<div class="vo-hint" part="hint"></div>' +
		'<button class="vo-btn vo-speaker" type="button" title="Markierten Text vorlesen">' +
		ICON_SPEAKER +
		"</button>" +
		// Pause-Button: initial versteckt, wird bei 'playing'/'paused' sichtbar
		'<button class="vo-btn vo-pause-btn" type="button" title="Pause / Weiter" style="display:none;">' +
		ICON_PAUSE +
		"</button>" +
		'<button class="vo-btn vo-gear" type="button" title="Einstellungen">' +
		ICON_GEAR +
		"</button>" +
		// Schmaler Fortschrittsbalken unter dem Button-Container
		'<div class="vo-progress-wrap" style="display:none;">' +
		'<div class="vo-progress"></div>' +
		"</div>" +
		// Aktuelles Wort (KISS-Lösung: kleines Label statt DOM-Manipulation)
		'<div class="vo-current-word"></div>';
	shadow.appendChild(container);

	const speakerBtn = container.querySelector(".vo-speaker");
	const pauseBtn = container.querySelector(".vo-pause-btn");
	const gearBtn = container.querySelector(".vo-gear");
	const hintEl = container.querySelector(".vo-hint");
	const progressWrap = container.querySelector(".vo-progress-wrap");
	const progressBar = container.querySelector(".vo-progress");
	const currentWordEl = container.querySelector(".vo-current-word");

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
		hintTimer = setTimeout(() => hintEl.classList.remove("vo-show"), 2800);
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

	// ----- Pause-Button Sichtbarkeit und Zustand steuern ----------------------
	function setPauseButtonVisible(visible) {
		pauseBtn.style.display = visible ? "" : "none";
	}

	function setPauseButtonState(paused) {
		pauseBtn.innerHTML = paused ? ICON_PLAY_TRIANGLE : ICON_PAUSE;
		pauseBtn.title = paused ? "Weiter vorlesen" : "Pause";
		pauseBtn.classList.toggle("vo-paused", paused);
	}

	// ----- Fortschrittsbalken aktualisieren ------------------------------------
	function setProgress(chunk, total) {
		progressChunk = chunk;
		progressTotal = total;
		if (total > 0) {
			progressWrap.style.display = "";
			const pct = Math.round((chunk / total) * 100);
			progressBar.style.width = pct + "%";
		} else {
			progressWrap.style.display = "none";
			progressBar.style.width = "0%";
		}
	}

	function resetProgress() {
		progressChunk = 0;
		progressTotal = 0;
		progressWrap.style.display = "none";
		progressBar.style.width = "0%";
	}

	// ----- Wort-Highlight (KISS: Label im Overlay) ----------------------------
	function startWordHighlight(words) {
		stopWordHighlight();
		currentChunkWords = Array.isArray(words) ? words : [];
		currentChunkWordIndex = 0;
		if (currentChunkWords.length === 0) return;
		// Geschätzte Zeit pro Wort: 350 ms (konservativ, da Chunk-Dauer unbekannt)
		const msPerWord = 350;
		currentWordEl.textContent = currentChunkWords[0] || "";
		highlightTimer = setInterval(() => {
			currentChunkWordIndex++;
			if (currentChunkWordIndex >= currentChunkWords.length) {
				stopWordHighlight();
				return;
			}
			currentWordEl.textContent = currentChunkWords[currentChunkWordIndex];
		}, msPerWord);
	}

	function stopWordHighlight() {
		if (highlightTimer) {
			clearInterval(highlightTimer);
			highlightTimer = null;
		}
		currentChunkWords = [];
		currentChunkWordIndex = 0;
		currentWordEl.textContent = "";
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

	// Viewport-Clamp bei Resize: Overlay bleibt immer vollständig sichtbar
	function clampCurrentPosition() {
		if (container.style.left && container.style.left !== "auto") {
			const left = parseFloat(container.style.left);
			const top = parseFloat(container.style.top);
			applyPosition(left, top);
		}
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

	// Standard: unten rechts (bis der Benutzer verschiebt)
	container.style.right = "16px";
	container.style.bottom = "16px";

	window.VOSettings.getPosition().then((pos) => {
		if (pos) applyPosition(pos.left, pos.top);
		// Initiale Layout-Sonde erst nach dem CSS-Load + naechstem Frame messen
		// (sonst ungestylte Groesse). Position wird wie bisher sofort angewandt.
		cssReady.then(() =>
			requestAnimationFrame(() =>
				logLayout("Overlay", container, {
					initial: true,
					gespeichertePosition: !!pos,
				}),
			),
		);
	});

	window.addEventListener("resize", () => {
		clampCurrentPosition();
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
			const rect = container.getBoundingClientRect();
			window.VOSettings.setPosition(rect.left, rect.top);
			// Viewport-Clamp nach Drag sicherstellen
			clampCurrentPosition();
			logLayout("Overlay", container, { nachDrag: true });
		} else if (pressed === speakerBtn) {
			onSpeakerClick();
		} else if (pressed === pauseBtn) {
			onPauseClick();
		} else if (pressed === gearBtn) {
			togglePanel();
		}
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
			sendMessage({ type: MSG.STOP });
			setState("stopped");
			// Pause-Zustand und Fortschritt zurücksetzen
			isPaused = false;
			stopWordHighlight();
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
		const settings = await window.VOSettings.load();
		const engine = settings.activeEngine;
		const cfg = settings[engine];
		if (engine === "google" && !(settings.google.apiKey || "").trim()) {
			if (window.VODiag)
				window.VODiag.log(
					"WARN",
					"UI_EREIGNIS",
					"speaker_klick:google_kein_key",
					{},
				);
			showHint(
				"Google braucht einen API-Key — bitte im Zahnrad eintragen.",
				true,
			);
			return;
		}
		if (window.VODiag)
			window.VODiag.log("INFO", "UI_EREIGNIS", "speaker_klick:start", {
				engine,
				voice: cfg.voice,
				rate: cfg.rate,
				textLen: text.length,
			});
		setState("loading");
		// Pitch nur für Edge relevant
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
	chrome.runtime.onMessage.addListener((msg) => {
		if (!msg || typeof msg !== "object") return;

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
				setState("stopped");
				showHint(msg.message || "Es ist ein Fehler aufgetreten.", true);
				// Aufräumen bei Fehler
				isPaused = false;
				stopWordHighlight();
				resetProgress();
				setPauseButtonVisible(false);
				setPauseButtonState(false);
			} else if (msg.state === "playing") {
				setState("playing");
				// Pause-Button einblenden wenn Wiedergabe läuft
				setPauseButtonVisible(true);
				setPauseButtonState(false);
				isPaused = false;
			} else if (msg.state === "paused") {
				isPaused = true;
				// Icon auf Play umschalten (bereit zum Fortsetzen)
				setPauseButtonState(true);
				if (window.VODiag)
					window.VODiag.log("INFO", "ZUSTAND", "overlay.status_paused", {});
			} else if (msg.state === "resumed") {
				isPaused = false;
				// Icon auf Pause zurück (läuft wieder)
				setPauseButtonState(false);
				if (window.VODiag)
					window.VODiag.log("INFO", "ZUSTAND", "overlay.status_resumed", {});
			} else if (msg.state === "stopped") {
				setState("stopped");
				// Alles zurücksetzen
				isPaused = false;
				stopWordHighlight();
				resetProgress();
				setPauseButtonVisible(false);
				setPauseButtonState(false);
				currentWordEl.textContent = "";
			} else {
				setState(msg.state);
			}
			if (panelStatusHandler) panelStatusHandler(msg.state, msg.message);
		} else if (msg.type === MSG.PROGRESS) {
			// Fortschrittsbalken aktualisieren
			const chunk = msg.chunk && typeof msg.chunk === "number" ? msg.chunk : 0;
			const total =
				msg.totalChunks && typeof msg.totalChunks === "number"
					? msg.totalChunks
					: 0;
			setProgress(chunk, total);
		} else if (msg.type === MSG.CHUNK_TIMING) {
			// Wort-Highlight starten
			const words = Array.isArray(msg.words) ? msg.words : [];
			startWordHighlight(words);
		} else if (msg.type === MSG.SPEAK_COMMAND) {
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
		if (open) positionPanel();
	}

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
			'<select class="vo-select" data-role="edge-voice"></select></div>' +
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
			'<select class="vo-select" data-role="google-voice" disabled></select></div>' +
			'<div class="vo-section"><span class="vo-label">Vorlese-Tempo</span>' +
			'<div class="vo-row"><input class="vo-range" type="range" min="0.5" max="2" step="0.1" data-role="google-rate">' +
			'<span class="vo-range-val" data-role="google-rate-val"></span></div></div>' +
			'<button class="vo-test" type="button" data-role="google-test">Test — Beispielsatz vorlesen</button>' +
			'<div class="vo-status" data-role="google-status"></div>' +
			"</div>" +
			// Auto-Speak Toggle (letztes Panel-Element)
			'<div class="vo-section vo-autospeak-section">' +
			'<label class="vo-toggle-row">' +
			'<span class="vo-label" style="margin-bottom:0;">Automatisch vorlesen bei Markierung</span>' +
			'<input type="checkbox" class="vo-toggle" data-role="auto-speak">' +
			"</label></div>" +
			"</div>";

		const $ = (role) => panel.querySelector('[data-role="' + role + '"]');
		panel
			.querySelector(".vo-close")
			.addEventListener("click", () => panel.classList.remove("vo-open"));

		const engineOpts = panel.querySelectorAll(".vo-engine-opt");
		const tabs = panel.querySelectorAll(".vo-tab");
		const pages = panel.querySelectorAll(".vo-tabpage");

		let current = null; // aktuelle Einstellungen im Speicher

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
		function fillVoiceSelect(sel, voices, selectedId) {
			sel.innerHTML = "";
			for (const v of voices) {
				const opt = document.createElement("option");
				opt.value = v.id;
				opt.textContent = v.label;
				if (v.id === selectedId) opt.selected = true;
				sel.appendChild(opt);
			}
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
			fillVoiceSelect(sel, voices, current.edge.voice);
			sel.disabled = false;
			if (!voices.some((v) => v.id === current.edge.voice)) {
				current.edge.voice = sel.value;
				await persist();
			}
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
			fillVoiceSelect(sel, voices, current.google.voice);
			sel.disabled = false;
			showStatus("google-status", "", "");
			if (!voices.some((v) => v.id === current.google.voice)) {
				current.google.voice = sel.value;
				await persist();
			}
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
		});
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
		});
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

		// Auto-Speak Toggle
		$("auto-speak").addEventListener("change", async (e) => {
			current.autoSpeak = e.target.checked;
			await persist();
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
			// Auto-Speak Toggle initialisieren
			$("auto-speak").checked = !!current.autoSpeak;
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
	void lastHighlightedRange;
})();
