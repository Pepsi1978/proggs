// ============================================================
// stt.js — Mic-Button + Whisper-Spracheingabe (Groq via Service Worker)
// ------------------------------------------------------------
// Aufnahme per MediaRecorder, Live-Vorschau per Web Speech API, finale
// Transkription per Groq Whisper. Der eigentliche Groq-fetch laeuft im
// Service Worker (background.js) — Audio wird als data-URL uebergeben.
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	const MIC_ICON = {
		mic: '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>',
		stop: '<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>',
		spinner:
			'<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 2a10 10 0 0 1 10 10"/></svg>',
		error:
			'<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
	};

	const supportedSpeech = !!(
		navigator.mediaDevices && navigator.mediaDevices.getUserMedia
	);
	const SpeechRecognitionAPI =
		window.SpeechRecognition || window.webkitSpeechRecognition;
	const supportedWebSpeech = !!SpeechRecognitionAPI;
	const MIN_CHARS_FOR_REWRITE = 6;

	let micBtn = null;
	let wantsRecording = false;
	let mediaRecorder = null;
	let audioChunks = [];
	let audioStream = null;
	let _micPending = false;
	let speechRecognition = null;
	let livePreviewEl = null;
	let textBeforeSpeech = "";

	const ED = () => OV.editable;

	// ── Trusted-Types-sicheres Setzen von SVG/HTML ──
	// Seiten wie grok.com/claude.ai verbieten per CSP "el.innerHTML = string".
	// Dann ueber DOMParser einfuegen, notfalls Emoji als Text (Symbol ist IMMER da).
	function setSvgIcon(el, svgStr, emoji) {
		try {
			el.innerHTML = svgStr;
			return;
		} catch {}
		try {
			el.textContent = "";
			const doc = new DOMParser().parseFromString(svgStr, "image/svg+xml");
			const node = doc.documentElement;
			if (node && node.tagName && node.tagName.toLowerCase() === "svg") {
				el.appendChild(document.importNode(node, true));
				return;
			}
		} catch {}
		el.textContent = emoji || "";
	}

	function setSafeInner(el, htmlStr) {
		try {
			el.innerHTML = htmlStr;
			return;
		} catch {}
		try {
			el.textContent = "";
			const doc = new DOMParser().parseFromString(htmlStr, "text/html");
			for (const n of [...doc.body.childNodes])
				el.appendChild(document.importNode(n, true));
		} catch {
			el.textContent = htmlStr.replace(/<[^>]+>/g, "");
		}
	}

	// Farben INLINE setzen — sonst gewinnt das weisse Inline-Background aus ui.js
	// (styleRoundButton) und das weisse SVG-Icon (stroke=currentColor) waere unsichtbar.
	function colorMic(bg) {
		if (!micBtn) return;
		micBtn.style.setProperty("background", bg, "important");
		micBtn.style.setProperty("color", "#fff", "important");
		micBtn.style.setProperty("border-color", bg, "important");
	}

	function setMicState(state, msg = "") {
		if (!micBtn) return;
		try {
			if (!micBtn.classList.contains("stt-mic-btn"))
				micBtn.classList.add("stt-mic-btn");
			if (state === "listening") {
				setSvgIcon(micBtn, MIC_ICON.stop, "⏹️");
				micBtn.setAttribute("data-state", "listening");
				colorMic("#dc2626");
				micBtn.title = "Spracheingabe laeuft – klicken zum Stop";
				return;
			}
			if (state === "working") {
				setSvgIcon(micBtn, MIC_ICON.spinner, "⏳");
				micBtn.setAttribute("data-state", "working");
				colorMic("#d97706");
				micBtn.title = msg || "Bereinigung laeuft…";
				return;
			}
			if (state === "error") {
				setSvgIcon(micBtn, MIC_ICON.error, "⚠️");
				micBtn.setAttribute("data-state", "error");
				colorMic("#8b0000");
				micBtn.title = msg || "Fehler";
				return;
			}
			setSvgIcon(micBtn, MIC_ICON.mic, "🎙️");
			micBtn.setAttribute("data-state", "idle");
			colorMic("#2563eb");
			micBtn.title = supportedSpeech
				? "Spracheingabe (Start/Stop)"
				: "Speech API nicht verfuegbar";
		} catch (e) {
			console.warn("[Overlays] setMicState:", e);
		}
	}

	// ── Live-Vorschau ──
	function setLivePreviewWaiting() {
		if (!livePreviewEl) return;
		const box = livePreviewEl.querySelector(".stt-pv-text");
		if (box)
			setSafeInner(
				box,
				'<span class="stt-pv-waiting">Whisper transkribiert…</span>',
			);
	}
	function removeLivePreview() {
		if (livePreviewEl) {
			livePreviewEl.remove();
			livePreviewEl = null;
		}
	}
	function createLivePreview() {
		removeLivePreview();
		livePreviewEl = document.createElement("div");
		livePreviewEl.id = "stt-live-preview";
		setSafeInner(
			livePreviewEl,
			'<div class="stt-pv-label">🎤 Live-Vorschau</div><div class="stt-pv-text">…</div>',
		);
		document.body.appendChild(livePreviewEl);
	}

	function startWebSpeech() {
		if (!supportedWebSpeech) return;
		try {
			speechRecognition = new SpeechRecognitionAPI();
			speechRecognition.lang = OV.storage.get("whisperLang", "de");
			speechRecognition.continuous = true;
			speechRecognition.interimResults = true;
			speechRecognition.maxAlternatives = 1;
			let _liveDebounce = null;
			speechRecognition.onresult = (event) => {
				let finalT = "";
				let interimT = "";
				for (let i = 0; i < event.results.length; i++) {
					if (event.results[i].isFinal)
						finalT += event.results[i][0].transcript;
					else interimT += event.results[i][0].transcript;
				}
				clearTimeout(_liveDebounce);
				_liveDebounce = setTimeout(() => {
					const el = ED().getUserTargetEditable();
					if (el) {
						const preview = finalT + interimT;
						const spacer =
							textBeforeSpeech &&
							!textBeforeSpeech.endsWith(" ") &&
							!textBeforeSpeech.endsWith("\n")
								? " "
								: "";
						ED().setViaPaste(el, textBeforeSpeech + spacer + preview);
					}
				}, 120);
			};
			speechRecognition.onerror = (event) => {
				if (event.error === "no-speech" || event.error === "aborted") return;
				console.log("[Overlays] Web Speech (unkritisch):", event.error);
			};
			speechRecognition.onend = () => {
				if (wantsRecording && speechRecognition) {
					try {
						speechRecognition.start();
					} catch {}
				}
			};
			speechRecognition.start();
		} catch (e) {
			console.log("[Overlays] Web Speech nicht verfuegbar:", e);
			speechRecognition = null;
		}
	}
	function stopWebSpeech() {
		if (speechRecognition) {
			const ref = speechRecognition;
			speechRecognition = null;
			try {
				ref.abort();
			} catch {}
		}
	}

	function blobToDataUrl(blob) {
		return new Promise((resolve, reject) => {
			const r = new FileReader();
			r.onload = () => resolve(r.result);
			r.onerror = reject;
			r.readAsDataURL(blob);
		});
	}

	async function transcribe(audioBlob) {
		setMicState("working", "Whisper transkribiert…");
		setLivePreviewWaiting();
		OV.toast("🎧 Whisper transkribiert…", 2000);

		let res;
		try {
			const audioDataUrl = await blobToDataUrl(audioBlob);
			res = await chrome.runtime.sendMessage({
				type: "groqTranscribe",
				audioDataUrl,
				model: OV.storage.get("whisperModel", "whisper-large-v3-turbo"),
				lang: OV.storage.get("whisperLang", "de"),
			});
		} catch (e) {
			res = { ok: false, error: String(e?.message || e) };
		}

		if (!res || !res.ok) {
			setMicState("error", res?.error || "Fehler");
			removeLivePreview();
			OV.toast("❌ Groq Fehler:\n" + (res?.error || "unbekannt"), 9000);
			setTimeout(() => setMicState("idle"), 3000);
			return;
		}

		const text = (res.text || "").trim();
		if (!text) {
			setMicState("idle");
			removeLivePreview();
			OV.toast("⚠️ Keine Sprache erkannt.", 3000);
			return;
		}

		const el = ED().getUserTargetEditable();
		if (!el) {
			setMicState("error", "Kein Eingabefeld");
			removeLivePreview();
			OV.toast("❌ Eingabefeld nicht gefunden.", 5000);
			setTimeout(() => setMicState("idle"), 2500);
			return;
		}

		const base = textBeforeSpeech;
		const spacer =
			base && !base.endsWith(" ") && !base.endsWith("\n") ? " " : "";
		let combined = base + spacer + text;
		let corrected = false;

		// Optionale Gemini-Korrektur (nur wenn Profil Gemini hat + aktiviert)
		const profile = OV.activeProfile;
		if (
			profile?.gemini &&
			OV.storage.get("autoGeminiCorrection", true) &&
			OV.gemini &&
			text.length >= MIN_CHARS_FOR_REWRITE
		) {
			try {
				setMicState("working", "Gemini korrigiert…");
				OV.toast("✨ Gemini korrigiert…", 3000);
				const result = await OV.gemini.rewriteGrammarSmart(text);
				if (result && result.trim().length > 0) {
					combined = base + spacer + result.trim();
					corrected = true;
				}
			} catch (err) {
				console.warn("[Overlays] Gemini-Korrektur fehlgeschlagen:", err);
				OV.toast(
					"⚠️ Gemini-Korrektur fehlgeschlagen. Roher Text wird verwendet.",
					4000,
				);
			}
		}

		const ok = await ED().setViaPaste(el, combined);
		removeLivePreview();
		if (ok) {
			setMicState("idle");
			const preview = text.length > 80 ? text.slice(0, 80) + "…" : text;
			OV.toast(
				corrected ? "✨ Korrigiert & eingefuegt" : "✅ " + preview,
				3000,
			);
			OV.actions?.autoSendIfEnabled?.(el);
		} else {
			setMicState("error", "Text nicht uebernommen");
			OV.toast("❌ Eingabefeld hat Text nicht uebernommen.", 5000);
			setTimeout(() => setMicState("idle"), 2500);
		}
	}

	function startListening() {
		if (!supportedSpeech || _micPending) return;
		_micPending = true;
		const t = ED().getUserTargetEditable();
		if (!t) {
			OV.toast(
				"⚠️ Kein fokussiertes Eingabefeld. Tipp: zuerst ins Ziel-Feld tippen.",
				3500,
			);
		} else {
			ED().rememberEditable(t);
		}
		textBeforeSpeech = t ? ED().readPromptText(t) : "";
		wantsRecording = true;
		audioChunks = [];

		navigator.mediaDevices
			.getUserMedia({ audio: true })
			.then((stream) => {
				_micPending = false;
				audioStream = stream;
				const mimeType =
					typeof MediaRecorder.isTypeSupported === "function"
						? MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
							? "audio/webm;codecs=opus"
							: MediaRecorder.isTypeSupported("audio/webm")
								? "audio/webm"
								: ""
						: "";
				mediaRecorder = new MediaRecorder(stream, mimeType ? { mimeType } : {});
				mediaRecorder.ondataavailable = (e) => {
					if (e.data.size > 0) audioChunks.push(e.data);
				};
				mediaRecorder.onstop = () => {
					stream.getTracks().forEach((tr) => {
						tr.stop();
					});
					audioStream = null;
					if (audioChunks.length === 0) {
						setMicState("idle");
						removeLivePreview();
						return;
					}
					const audioBlob = new Blob(audioChunks, {
						type: mediaRecorder.mimeType || "audio/webm",
					});
					audioChunks = [];
					transcribe(audioBlob);
				};
				mediaRecorder.start(1000);
				setMicState("listening");
				OV.toast("🎙️ Aufnahme laeuft… (Stop ueber ⏹️)", 1500);
				createLivePreview();
				startWebSpeech();
			})
			.catch((err) => {
				_micPending = false;
				wantsRecording = false;
				setMicState("error", String(err));
				OV.toast("❌ Mikrofon-Zugriff fehlgeschlagen:\n" + String(err), 8000);
				setTimeout(() => setMicState("idle"), 3000);
			});
	}

	function stopListening() {
		wantsRecording = false;
		stopWebSpeech();
		if (mediaRecorder && mediaRecorder.state !== "inactive") {
			setMicState("working", "Aufnahme beendet…");
			setLivePreviewWaiting();
			mediaRecorder.stop();
		} else {
			if (audioStream) {
				audioStream.getTracks().forEach((tr) => {
					tr.stop();
				});
				audioStream = null;
			}
			removeLivePreview();
			setMicState("idle");
		}
	}

	function toggleMic() {
		if (!supportedSpeech) {
			OV.toast("❌ Mikrofon nicht verfuegbar (getUserMedia).", 5000);
			return;
		}
		// Erweiterung aktualisiert, aber Tab nicht neu geladen -> chrome.* tot.
		if (OV.ctxAlive && !OV.ctxAlive()) {
			OV.toast(
				"🔄 Erweiterung wurde aktualisiert — bitte diese Seite neu laden (F5).",
				9000,
			);
			return;
		}
		if (!wantsRecording) startListening();
		else stopListening();
	}

	OV.stt = {
		supportedSpeech,
		attachMic(btn) {
			micBtn = btn;
			btn.classList.add("stt-mic-btn");
			btn.onclick = toggleMic;
			setMicState("idle");
		},
		toggleMic,
		setMicState,
	};
})();
