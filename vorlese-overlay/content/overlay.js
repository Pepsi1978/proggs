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

	const SAMPLE_TEXT =
		"Dies ist ein Beispielsatz, mit dem du die gewählte Stimme und das Tempo prüfen kannst.";

	let lastSelection = "";
	let isPlaying = false;
	let hintTimer = null;
	// Vom Panel gesetzte Funktion, um Status-/Fehlermeldungen im Panel anzuzeigen.
	let panelStatusHandler = null;

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
	host.style.cssText = "all: initial; position: static;";
	const shadow = host.attachShadow({ mode: "open" });

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
		logLayout("Overlay", container, {
			initial: true,
			gespeichertePosition: !!pos,
		});
	});

	window.addEventListener("resize", () => {
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
			const rect = container.getBoundingClientRect();
			window.VOSettings.setPosition(rect.left, rect.top);
			logLayout("Overlay", container, { nachDrag: true });
		} else if (pressed === speakerBtn) {
			onSpeakerClick();
		} else if (pressed === gearBtn) {
			togglePanel();
		}
	}

	// ----- Lautsprecher-Aktion -------------------------------------------------
	async function onSpeakerClick() {
		if (isPlaying) {
			if (window.VODiag)
				window.VODiag.log("INFO", "UI_EREIGNIS", "speaker_klick:stop", {});
			sendMessage({ type: MSG.STOP });
			setState("stopped");
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
			} else {
				setState(msg.state);
			}
			if (panelStatusHandler) panelStatusHandler(msg.state, msg.message);
		} else if (msg.type === MSG.SPEAK_COMMAND) {
			if (window.VODiag)
				window.VODiag.log("INFO", "UI_EREIGNIS", "overlay.tastenkuerzel", {});
			onSpeakerClick();
		}
	});

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
		$("edge-test").addEventListener("click", () => {
			showStatus("edge-status", "Wird vorgelesen…", "");
			sendMessage({
				type: MSG.TEST,
				engine: "edge",
				text: SAMPLE_TEXT,
				voice: $("edge-voice").value,
				rate: window.VOSettings.clampRate($("edge-rate").value),
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
			setRateUI("google-rate", "google-rate-val", current.google.rate);
			$("google-key").value = current.google.apiKey || "";
			await loadEdgeVoices();
			await loadGoogleVoices();
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
})();
