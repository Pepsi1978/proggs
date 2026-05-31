/*
 * sidepanel.js — Einstellungen des Vorlese-Overlays in der Seitenleiste.
 *
 * Nutzt window.VOSettings (aus ../settings/settings.js) fuer denselben Speicher
 * (chrome.storage.local "vo_settings") wie das Overlay und spricht direkt mit
 * dem Service-Worker (GET_VOICES, TTS_TEST). Aenderungen wirken sofort, weil das
 * Overlay/der Worker die Werte beim naechsten Vorlesen frisch aus dem Speicher
 * liest. Die UI spiegelt 1:1 das fruehere In-Page-Panel.
 */
(function () {
	"use strict";

	const SAMPLE_TEXT =
		"Dies ist ein Beispielsatz, mit dem du die gewählte Stimme und das Tempo prüfen kannst.";

	const MSG = {
		GET_VOICES: "GET_VOICES",
		TEST: "TTS_TEST",
		RELOAD: "RELOAD_EXTENSION",
	};

	const $ = (id) => document.getElementById(id);

	let current = null;
	let lastEdgeVoices = [];
	let lastGoogleVoices = [];

	function send(msg) {
		try {
			chrome.runtime.sendMessage(msg, () => void chrome.runtime.lastError);
		} catch (_) {
			/* Kanal evtl. weg */
		}
	}

	function requestVoices(engine, apiKey) {
		return new Promise((resolve) => {
			try {
				chrome.runtime.sendMessage(
					{ type: MSG.GET_VOICES, engine, apiKey },
					(res) => {
						if (chrome.runtime.lastError || !res) {
							resolve({ voices: [] });
							return;
						}
						resolve(res);
					},
				);
			} catch (_) {
				resolve({ voices: [] });
			}
		});
	}

	async function persist() {
		current = await window.VOSettings.save(current);
	}

	// ----- Favoriten (Sterne) -------------------------------------------------
	function isFavorite(id) {
		return !!(
			current &&
			Array.isArray(current.favoriteVoices) &&
			current.favoriteVoices.includes(id)
		);
	}

	function fillVoiceSelect(sel, voices, selectedId) {
		sel.innerHTML = "";
		const sorted = voices
			.map((v, i) => ({ v, i }))
			.sort((a, b) => {
				const fa = isFavorite(a.v.id) ? 0 : 1;
				const fb = isFavorite(b.v.id) ? 0 : 1;
				return fa - fb || a.i - b.i;
			})
			.map((x) => x.v);
		for (const v of sorted) {
			const opt = document.createElement("option");
			opt.value = v.id;
			opt.textContent = (isFavorite(v.id) ? "★ " : "") + v.label;
			if (v.id === selectedId) opt.selected = true;
			sel.appendChild(opt);
		}
	}

	function updateStar(starEl, sel) {
		if (!starEl || !sel) return;
		const fav = isFavorite(sel.value);
		starEl.textContent = fav ? "★" : "☆";
		starEl.classList.toggle("sp-star-on", fav);
		starEl.title = fav ? "Stern entfernen" : "Diese Stimme mit Stern markieren";
	}

	async function toggleFavorite(sel, starEl, engineKey) {
		if (!sel || !sel.value) return;
		const id = sel.value;
		if (!Array.isArray(current.favoriteVoices)) current.favoriteVoices = [];
		const pos = current.favoriteVoices.indexOf(id);
		if (pos >= 0) current.favoriteVoices.splice(pos, 1);
		else current.favoriteVoices.push(id);
		await persist();
		const voices = engineKey === "google" ? lastGoogleVoices : lastEdgeVoices;
		if (voices && voices.length) fillVoiceSelect(sel, voices, id);
		updateStar(starEl, sel);
	}

	// ----- Status / Slider-Anzeige -------------------------------------------
	function showStatus(el, message, kind) {
		el.textContent = message || "";
		el.classList.toggle("sp-ok", kind === "ok");
		el.classList.toggle("sp-err", kind === "err");
	}

	function setRateUI(range, val, value) {
		range.value = String(value);
		val.textContent = Number(value).toFixed(1) + "×";
	}

	function setPitchUI(range, val, value) {
		const v = Number(value) || 0;
		range.value = String(v);
		val.textContent = (v >= 0 ? "+" : "") + v + " st";
	}

	// ----- Reiter / Engine ----------------------------------------------------
	function showTab(name) {
		document.querySelectorAll(".sp-tab").forEach((t) => {
			t.classList.toggle("sp-active", t.dataset.tab === name);
		});
		document.querySelectorAll(".sp-tabpage").forEach((p) => {
			p.classList.toggle("sp-active", p.dataset.page === name);
		});
	}

	function markEngine(name) {
		document.querySelectorAll(".sp-engine-opt").forEach((o) => {
			o.classList.toggle("sp-active", o.dataset.engine === name);
		});
	}

	// ----- Stimmen laden ------------------------------------------------------
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
		updateStar($("edge-star"), sel);
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
			showStatus($("google-status"), res.error, "err");
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
		showStatus($("google-status"), "", "");
		if (!voices.some((v) => v.id === current.google.voice)) {
			current.google.voice = sel.value;
			await persist();
		}
		updateStar($("google-star"), sel);
	}

	// ----- Verdrahtung --------------------------------------------------------
	function wire() {
		document.querySelectorAll(".sp-engine-opt").forEach((o) =>
			o.addEventListener("click", async () => {
				current.activeEngine = o.dataset.engine;
				markEngine(current.activeEngine);
				showTab(current.activeEngine);
				await persist();
			}),
		);
		document
			.querySelectorAll(".sp-tab")
			.forEach((t) =>
				t.addEventListener("click", () => showTab(t.dataset.tab)),
			);

		// Edge
		$("edge-voice").addEventListener("change", async (e) => {
			current.edge.voice = e.target.value;
			await persist();
			updateStar($("edge-star"), $("edge-voice"));
		});
		$("edge-star").addEventListener("click", () =>
			toggleFavorite($("edge-voice"), $("edge-star"), "edge"),
		);
		$("edge-rate").addEventListener("input", (e) => {
			$("edge-rate-val").textContent = Number(e.target.value).toFixed(1) + "×";
		});
		$("edge-rate").addEventListener("change", async (e) => {
			current.edge.rate = window.VOSettings.clampRate(e.target.value);
			await persist();
		});
		$("edge-pitch").addEventListener("input", (e) => {
			const v = parseInt(e.target.value, 10) || 0;
			$("edge-pitch-val").textContent = (v >= 0 ? "+" : "") + v + " st";
		});
		$("edge-pitch").addEventListener("change", async (e) => {
			const v = Math.max(-50, Math.min(50, parseInt(e.target.value, 10) || 0));
			current.edge.pitch = v;
			await persist();
		});
		$("edge-test").addEventListener("click", () => {
			showStatus($("edge-status"), "Wird vorgelesen…", "");
			send({
				type: MSG.TEST,
				engine: "edge",
				text: SAMPLE_TEXT,
				voice: $("edge-voice").value,
				rate: window.VOSettings.clampRate($("edge-rate").value),
				pitch: parseInt($("edge-pitch").value, 10) || 0,
			});
			setTimeout(() => showStatus($("edge-status"), "", ""), 4000);
		});

		// Google
		$("google-key").addEventListener("change", async (e) => {
			current.google.apiKey = e.target.value.trim();
			await persist();
			await loadGoogleVoices();
		});
		$("google-voice").addEventListener("change", async (e) => {
			current.google.voice = e.target.value;
			await persist();
			updateStar($("google-star"), $("google-voice"));
		});
		$("google-star").addEventListener("click", () =>
			toggleFavorite($("google-voice"), $("google-star"), "google"),
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
					$("google-status"),
					"Bitte zuerst den API-Key eintragen.",
					"err",
				);
				return;
			}
			showStatus($("google-status"), "Wird vorgelesen…", "");
			send({
				type: MSG.TEST,
				engine: "google",
				text: SAMPLE_TEXT,
				voice: $("google-voice").value,
				rate: window.VOSettings.clampRate($("google-rate").value),
				apiKey: key,
			});
			setTimeout(() => showStatus($("google-status"), "", ""), 4000);
		});

		// Overlay verschiebbar
		$("draggable").addEventListener("change", async (e) => {
			current.draggable = !!e.target.checked;
			await persist();
		});

		// Aktualisieren: NUR die Nachricht an den Service-Worker senden. Der laedt
		// die Erweiterung neu UND danach automatisch die offenen Seiten (damit kein
		// totes Content-Script zurueckbleibt). NICHT hier selbst chrome.runtime.reload()
		// aufrufen — sonst stirbt die Seitenleiste, bevor der Worker das Flag setzt.
		$("reload").addEventListener("click", () => {
			const btn = $("reload");
			btn.textContent = "Wird aktualisiert…";
			btn.disabled = true;
			send({ type: MSG.RELOAD });
		});
	}

	// ----- Start --------------------------------------------------------------
	async function init() {
		current = await window.VOSettings.load();

		markEngine(current.activeEngine);
		showTab(current.activeEngine);

		setRateUI($("edge-rate"), $("edge-rate-val"), current.edge.rate);
		setPitchUI($("edge-pitch"), $("edge-pitch-val"), current.edge.pitch);
		setRateUI($("google-rate"), $("google-rate-val"), current.google.rate);
		$("google-key").value = current.google.apiKey || "";
		$("draggable").checked = !!current.draggable;

		wire();

		await loadEdgeVoices();
		await loadGoogleVoices();
	}

	init();
})();
