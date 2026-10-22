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
	let lastGeminiVoices = [];
	let lastAzureVoices = [];
	let currentPageHost = "";

	function send(msg) {
		try {
			chrome.runtime.sendMessage(msg, () => void chrome.runtime.lastError);
		} catch (_) {
			/* Kanal evtl. weg */
		}
	}

	function requestVoices(engine, apiKey, model) {
		return new Promise((resolve) => {
			try {
				chrome.runtime.sendMessage(
					{ type: MSG.GET_VOICES, engine, apiKey, model },
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

	async function getActiveTab() {
		try {
			const tabs = await chrome.tabs.query({ active: true, lastFocusedWindow: true });
			if (tabs && tabs[0]) return tabs[0];
		} catch (_) {
			/* unten Fallback versuchen */
		}
		try {
			const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
			return tabs && tabs[0] ? tabs[0] : null;
		} catch (_) {
			return null;
		}
	}

	function hostFromUrl(url) {
		try {
			const u = new URL(url || "");
			if (u.protocol === "file:") return "lokale-dateien";
			if (u.protocol !== "http:" && u.protocol !== "https:") return "";
			return u.hostname.toLowerCase();
		} catch (_) {
			return "";
		}
	}

	function requestPageState(tabId) {
		return new Promise((resolve) => {
			if (tabId == null) {
				resolve(null);
				return;
			}
			try {
				chrome.tabs.sendMessage(tabId, { type: "VO_GET_PAGE_STATE" }, (res) => {
					if (chrome.runtime.lastError || !res) {
						resolve(null);
						return;
					}
					resolve(res);
				});
			} catch (_) {
				resolve(null);
			}
		});
	}

	function updatePageToggle() {
		const cb = $("page-enabled");
		const help = $("page-enabled-help");
		const hosts = normalizeHostList(current && current.disabledHosts);
		if (!currentPageHost) {
			cb.checked = false;
			cb.disabled = true;
			help.textContent =
				"Das Overlay läuft auf dieser Seite nicht. Bei lokalen Dateien: unter chrome://extensions → Vorlese-Overlay → Details „Zugriff auf Datei-URLs zulassen“ einschalten und die Seite neu laden.";
			return;
		}
		cb.disabled = false;
		cb.checked = !hosts.includes(currentPageHost);
		help.textContent = `Aktuelle Webseite: ${currentPageHost}`;
	}

	async function loadCurrentPage() {
		const tab = await getActiveTab();
		currentPageHost = hostFromUrl(tab && tab.url);
		if (!currentPageHost && tab && tab.id != null) {
			const state = await requestPageState(tab.id);
			currentPageHost = state && state.host ? String(state.host).toLowerCase() : "";
		}
		updatePageToggle();
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
		const voices =
			engineKey === "google"
				? lastGoogleVoices
				: engineKey === "gemini"
					? lastGeminiVoices
					: engineKey === "azure"
						? lastAzureVoices
						: lastEdgeVoices;
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

	function geminiModelOf(voiceId) {
		return String(voiceId || "").startsWith("gemini_lite:") ? "lite" : "flash";
	}

	function geminiNameOf(voiceId) {
		const s = String(voiceId || "");
		const i = s.indexOf(":");
		return i >= 0 ? s.slice(i + 1) : "Leda";
	}

	async function loadGeminiVoices() {
		const sel = $("gemini-voice");
		const model = geminiModelOf(current.gemini.voice);
		$("gemini-model").value = model;
		const res = await requestVoices("gemini", undefined, model);
		const voices = (res && res.voices) || [];
		if (!voices.length) {
			sel.innerHTML = "<option>Keine Stimmen gefunden</option>";
			return;
		}
		lastGeminiVoices = voices;
		fillVoiceSelect(sel, voices, current.gemini.voice);
		if (!voices.some((v) => v.id === current.gemini.voice)) {
			current.gemini.voice = sel.value;
			await persist();
		}
		updateStar($("gemini-star"), sel);
		updateGeminiHint();
	}

	async function loadAzureVoices() {
		const sel = $("azure-voice");
		const res = await requestVoices("azure");
		const voices = (res && res.voices) || [];
		if (!voices.length) {
			sel.innerHTML = "<option>Keine Stimmen gefunden</option>";
			return;
		}
		lastAzureVoices = voices;
		fillVoiceSelect(sel, voices, current.azure.voice);
		if (!voices.some((v) => v.id === current.azure.voice)) {
			current.azure.voice = sel.value;
			await persist();
		}
		updateStar($("azure-star"), sel);
		updateAzureHint();
	}

	// Hinweis, wenn fuer die gewaehlte Schluesselwahl ein Schluessel fehlt.
	function updateGeminiHint() {
		const g = current.gemini;
		const free = !!(g.keyFree || "").trim();
		const paid = !!(g.keyPaid || "").trim();
		let hint = "";
		if (g.keyChoice === "nur_frei" && !free)
			hint = "Es fehlt der kostenlose Gemini-Schlüssel für die Stimme.";
		else if (g.keyChoice === "nur_bezahlt" && !paid)
			hint = "Es fehlt der bezahlte Gemini-Schlüssel für die Stimme.";
		else if (!free && !paid)
			hint = "Es ist kein Gemini-Schlüssel für die Stimme hinterlegt.";
		if (g.chirpFallback && !(current.google.apiKey || "").trim())
			hint = (hint + " Für den Rückfall auf Chirp 3 HD fehlt der Google-Cloud-Key.").trim();
		$("gemini-hint").textContent = hint;
	}

	function updateAzureHint() {
		$("azure-hint").textContent = (current.azure.key || "").trim()
			? ""
			: "Es fehlt der Azure-Speech-Schlüssel.";
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

		// Aktuelle Webseite
		$("page-enabled").addEventListener("change", async (e) => {
			if (!currentPageHost) return;
			const hosts = normalizeHostList(current.disabledHosts);
			const idx = hosts.indexOf(currentPageHost);
			if (e.target.checked && idx >= 0) hosts.splice(idx, 1);
			if (!e.target.checked && idx < 0) hosts.push(currentPageHost);
			current.disabledHosts = hosts;
			await persist();
			updatePageToggle();
		});

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
			updateGeminiHint();
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

		// Gemini 3.8 TTS
		$("gemini-model").addEventListener("change", async (e) => {
			const prov = e.target.value === "lite" ? "gemini_lite" : "gemini_flash";
			current.gemini.voice = prov + ":" + geminiNameOf(current.gemini.voice);
			await persist();
			await loadGeminiVoices();
		});
		$("gemini-voice").addEventListener("change", async (e) => {
			current.gemini.voice = e.target.value;
			await persist();
			updateStar($("gemini-star"), $("gemini-voice"));
		});
		$("gemini-star").addEventListener("click", () =>
			toggleFavorite($("gemini-voice"), $("gemini-star"), "gemini"),
		);
		$("gemini-key-free").addEventListener("change", async (e) => {
			current.gemini.keyFree = e.target.value.trim();
			await persist();
			updateGeminiHint();
		});
		$("gemini-key-paid").addEventListener("change", async (e) => {
			current.gemini.keyPaid = e.target.value.trim();
			await persist();
			updateGeminiHint();
		});
		$("gemini-key-choice").addEventListener("change", async (e) => {
			current.gemini.keyChoice = e.target.value;
			await persist();
			updateGeminiHint();
		});
		$("gemini-chirp").addEventListener("change", async (e) => {
			current.gemini.chirpFallback = !!e.target.checked;
			await persist();
			updateGeminiHint();
		});
		$("gemini-test").addEventListener("click", () => {
			showStatus($("gemini-status"), "Wird vorgelesen…", "");
			send({
				type: MSG.TEST,
				engine: "gemini",
				text: SAMPLE_TEXT,
				voice: $("gemini-voice").value,
				rate: current.gemini.rate,
			});
			setTimeout(() => showStatus($("gemini-status"), "", ""), 4000);
		});

		// Azure Neural
		$("azure-key").addEventListener("change", async (e) => {
			current.azure.key = e.target.value.trim();
			await persist();
			updateAzureHint();
		});
		$("azure-region").addEventListener("change", async (e) => {
			current.azure.region = e.target.value.trim() || "westeurope";
			await persist();
		});
		$("azure-voice").addEventListener("change", async (e) => {
			current.azure.voice = e.target.value;
			await persist();
			updateStar($("azure-star"), $("azure-voice"));
		});
		$("azure-star").addEventListener("click", () =>
			toggleFavorite($("azure-voice"), $("azure-star"), "azure"),
		);
		$("azure-rate").addEventListener("input", (e) => {
			$("azure-rate-val").textContent = Number(e.target.value).toFixed(1) + "×";
		});
		$("azure-rate").addEventListener("change", async (e) => {
			current.azure.rate = window.VOSettings.clampRate(e.target.value);
			await persist();
		});
		$("azure-test").addEventListener("click", () => {
			if (!(current.azure.key || "").trim()) {
				showStatus($("azure-status"), "Bitte zuerst den Azure-Schlüssel eintragen.", "err");
				return;
			}
			showStatus($("azure-status"), "Wird vorgelesen…", "");
			send({
				type: MSG.TEST,
				engine: "azure",
				text: SAMPLE_TEXT,
				voice: $("azure-voice").value,
				rate: window.VOSettings.clampRate($("azure-rate").value),
			});
			setTimeout(() => showStatus($("azure-status"), "", ""), 4000);
		});

		// Overlay verschiebbar
		$("draggable").addEventListener("change", async (e) => {
			current.draggable = !!e.target.checked;
			await persist();
		});

		// Aktualisieren: (1) ALLEN Tabs sagen, sich gleich selbst neu zu laden
		// (das Content-Script macht location.reload() — funktioniert auch nach der
		// Context-Invalidierung). Tabs ohne unser Script ignorieren das. (2) Dann die
		// Erweiterung neu laden. So passiert das F5 automatisch und zuverlaessig.
		$("reload").addEventListener("click", async () => {
			const btn = $("reload");
			btn.textContent = "Wird aktualisiert…";
			btn.disabled = true;
			try {
				const tabs = await chrome.tabs.query({});
				for (const t of tabs) {
					if (t.id != null) {
						chrome.tabs.sendMessage(
							t.id,
							{ type: "RELOAD_PAGE_SOON" },
							() => void chrome.runtime.lastError,
						);
					}
				}
			} catch (_) {
				/* egal — Erweiterung wird trotzdem neu geladen */
			}
			send({ type: MSG.RELOAD });
		});
	}

	// ----- Start --------------------------------------------------------------
	async function init() {
		current = await window.VOSettings.load();
		document.getElementById("version").textContent =
			chrome.runtime.getManifest().version_name ||
			chrome.runtime.getManifest().version;
		await loadCurrentPage();

		markEngine(current.activeEngine);
		showTab(current.activeEngine);

		setRateUI($("edge-rate"), $("edge-rate-val"), current.edge.rate);
		setPitchUI($("edge-pitch"), $("edge-pitch-val"), current.edge.pitch);
		setRateUI($("google-rate"), $("google-rate-val"), current.google.rate);
		$("google-key").value = current.google.apiKey || "";
		$("draggable").checked = !!current.draggable;
		$("gemini-key-free").value = current.gemini.keyFree || "";
		$("gemini-key-paid").value = current.gemini.keyPaid || "";
		$("gemini-key-choice").value = current.gemini.keyChoice || "frei_dann_bezahlt";
		$("gemini-chirp").checked = !!current.gemini.chirpFallback;
		$("azure-key").value = current.azure.key || "";
		$("azure-region").value = current.azure.region || "westeurope";
		setRateUI($("azure-rate"), $("azure-rate-val"), current.azure.rate);

		wire();

		await loadEdgeVoices();
		await loadGoogleVoices();
		await loadGeminiVoices();
		await loadAzureVoices();
	}

	init();
})();
