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

	// Schoene Material-Icons + Notfall-Emojis aus icons.js (zentral, TT-sicher).
	const MIC_ICON = OV.ICONS;
	const MIC_EMOJI = OV.ICON_FALLBACK;

	const supportedSpeech = !!(
		navigator.mediaDevices && navigator.mediaDevices.getUserMedia
	);
	const SpeechRecognitionAPI =
		window.SpeechRecognition || window.webkitSpeechRecognition;
	const supportedWebSpeech = !!SpeechRecognitionAPI;
	const MIN_CHARS_FOR_REWRITE = 6;

	// ── Stille-Halluzination-Schutz (Schicht 1: Sprachinhalt-Vorfilter) ──
	// Whisper/Groq erfindet bei Stille Floskeln ("Vielen Dank"). Reine Stille gar
	// nicht erst senden — das Confidence-Gate (Schicht 2 im Service Worker) faengt
	// reine Stille NICHT (Whisper halluziniert dort mit HOHER Confidence).
	// Nur ABSOLUTE laute Zeit messen (keine Ratio), da Toggle-Mic mit Denkpausen.
	// Konservativ: kurze Befehle ("ja"/"stop") bleiben erhalten.
	// Quelle: bugs/desktop/groq-transkription.md §2.1/§2.3
	const MIN_SPEECH_MS = 150; // min. absolute Sprechzeit zum Senden
	const SPEECH_RMS_THRESHOLD = 0.015; // RMS-Schwelle wie TVO/VoiceAgent

	let micBtn = null;
	let wantsRecording = false;
	let mediaRecorder = null;
	let audioChunks = [];
	let audioStream = null;
	let _micPending = false;
	let speechRecognition = null;
	let livePreviewEl = null;
	let previewActive = false; // true = Live-Vorschau darf das Kaestchen aktualisieren
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
				setSvgIcon(micBtn, MIC_ICON.stop, MIC_EMOJI.stop);
				micBtn.setAttribute("data-state", "listening");
				colorMic("#dc2626");
				micBtn.title = "Spracheingabe laeuft – klicken zum Stop";
				return;
			}
			if (state === "working") {
				setSvgIcon(micBtn, MIC_ICON.spinner, MIC_EMOJI.spinner);
				micBtn.setAttribute("data-state", "working");
				colorMic("#d97706");
				micBtn.title = msg || "Bereinigung laeuft…";
				return;
			}
			if (state === "error") {
				setSvgIcon(micBtn, MIC_ICON.error, MIC_EMOJI.error);
				micBtn.setAttribute("data-state", "error");
				colorMic("#8b0000");
				micBtn.title = msg || "Fehler";
				return;
			}
			setSvgIcon(micBtn, MIC_ICON.mic, MIC_EMOJI.mic);
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
		previewActive = false; // Riegel: ab jetzt keine Vorschau-Updates mehr
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
		previewActive = true; // Vorschau ist jetzt aktiv
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
				if (!previewActive) return; // nach dem Stopp NICHT mehr (Groq gewinnt)
				let finalT = "";
				let interimT = "";
				for (let i = 0; i < event.results.length; i++) {
					if (event.results[i].isFinal)
						finalT += event.results[i][0].transcript;
					else interimT += event.results[i][0].transcript;
				}
				clearTimeout(_liveDebounce);
				_liveDebounce = setTimeout(() => {
					// Live-Untertitel NUR ins schwebende Vorschau-Kaestchen schreiben — NIE ins
					// echte Eingabefeld. Das verhindert das Springen/Flackern im contenteditable
					// und stellt sicher, dass spaeter ausschliesslich die finale Groq-Whisper-
					// Fassung (mit Satzzeichen) ins Feld gelangt — die rohe Vorschau kann nie
					// versehentlich abgeschickt werden.
					if (!previewActive || !livePreviewEl) return;
					const box = livePreviewEl.querySelector(".stt-pv-text");
					if (!box) return;
					const esc = (s) =>
						s
							.replace(/&/g, "&amp;")
							.replace(/</g, "&lt;")
							.replace(/>/g, "&gt;");
					const html =
						'<span class="stt-pv-final">' +
						esc(finalT) +
						"</span>" +
						(interimT
							? '<span class="stt-pv-interim">' + esc(interimT) + "</span>"
							: "");
					setSafeInner(box, finalT || interimT ? html : "…");
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

	// Analysiert das aufgenommene Audio: Das Blob ist komprimiert (webm/opus), daher
	// per Web Audio dekodieren und RMS pro 20ms-Frame messen. Liefert eine Voiced-Timeline
	// (1 = lauter Frame) PLUS die absolute laute Zeit. Gibt null zurueck, wenn nicht
	// messbar (dann wird NICHT gefiltert -> senden, funktionserhaltend).
	const FRAME_MS = 20;
	async function analyzeAudio(blob) {
		const AC = window.AudioContext || window.webkitAudioContext;
		if (!AC) return null;
		const ctx = new AC();
		try {
			const arrayBuf = await blob.arrayBuffer();
			const audioBuf = await ctx.decodeAudioData(arrayBuf);
			const data = audioBuf.getChannelData(0); // erste Spur reicht
			const sr = audioBuf.sampleRate;
			const frame = Math.max(1, Math.round(sr * (FRAME_MS / 1000))); // 20ms
			const count = Math.floor(data.length / frame);
			const voiced = new Uint8Array(count);
			let voicedFrames = 0;
			for (let f = 0; f < count; f++) {
				let sum = 0;
				const base = f * frame;
				for (let j = 0; j < frame; j++) {
					const s = data[base + j];
					sum += s * s;
				}
				if (Math.sqrt(sum / frame) >= SPEECH_RMS_THRESHOLD) {
					voiced[f] = 1;
					voicedFrames++;
				}
			}
			return { speechMs: voicedFrames * FRAME_MS, voiced, frameMs: FRAME_MS };
		} finally {
			try {
				ctx.close();
			} catch {}
		}
	}

	// Schicht 3: Abgleich Whisper-Segment <-> echtes Audio. Ein Segment, dessen
	// Zeitfenster [start,end] im aufgenommenen Audio praktisch still war (< 10% laute
	// Frames), ist eine Trailing-/Pausen-Halluzination ("Ja", "Vielen Dank") — Whisper
	// erzeugt sie mit HOHER Confidence, daher faengt das Confidence-Gate (Schicht 2)
	// sie nicht. Echtes Wort hat Schall im Fenster -> bleibt (funktionserhaltend).
	const SEG_VOICED_RATIO = 0.1;
	function segmentHasSpeech(seg, analysis) {
		const start = Number(seg?.start);
		const end = Number(seg?.end);
		if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start)
			return true; // kein verwertbarer Zeitstempel -> nicht verwerfen
		const { voiced, frameMs } = analysis;
		const startF = Math.max(0, Math.floor((start * 1000) / frameMs));
		const endF = Math.min(voiced.length, Math.ceil((end * 1000) / frameMs));
		if (endF <= startF) return true;
		let v = 0;
		for (let i = startF; i < endF; i++) if (voiced[i]) v++;
		return v / (endF - startF) >= SEG_VOICED_RATIO;
	}

	// Schicht 1: erst pruefen, ob ueberhaupt genug Sprache drin ist. Reine Stille
	// (Knopf gedrueckt, nichts gesagt) gar nicht an Groq senden — verhindert die
	// "Vielen Dank"-Halluzination, spart zudem die 10s-Mindestabrechnung.
	async function maybeTranscribe(audioBlob) {
		let analysis = null;
		try {
			analysis = await analyzeAudio(audioBlob);
		} catch (e) {
			// Decode fehlgeschlagen -> NICHT filtern (funktionserhaltend, nie echte
			// Sprache verwerfen), einfach senden.
			console.warn(
				"[Overlays] Sprachgehalt-Messung fehlgeschlagen, sende trotzdem:",
				e,
			);
		}
		if (analysis && analysis.speechMs < MIN_SPEECH_MS) {
			console.log(
				`[Overlays] STT: nur ${Math.round(analysis.speechMs)}ms Sprache (< ${MIN_SPEECH_MS}ms) -> nicht gesendet (Stille-Schutz)`,
			);
			setMicState("idle");
			removeLivePreview();
			OV.toast("🤫 Keine Sprache erkannt — nichts gesendet.", 2000);
			return;
		}
		transcribe(audioBlob, analysis);
	}

	async function transcribe(audioBlob, analysis) {
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

		// Schicht 3: Segmente gegen das echte Audio abgleichen (Trailing-/Pausen-
		// Halluzination wie "Ja" entfernen). Nur wenn wir Audio analysieren konnten
		// UND der Worker Segmente mit Zeitstempeln lieferte.
		let text;
		if (analysis && Array.isArray(res.segments) && res.segments.length) {
			const kept = res.segments.filter((s) => {
				const ok = segmentHasSpeech(s, analysis);
				if (!ok)
					console.log(
						`[Overlays] STT-Segment verworfen (Audio still ${Number(s.start).toFixed(2)}-${Number(s.end).toFixed(2)}s): "${String(s.text || "").trim()}"`,
					);
				return ok;
			});
			text = kept
				.map((s) => s.text || "")
				.join("")
				.trim();
			// Sicherung: Wuerde der Abgleich ALLES verwerfen, obwohl Sprache da war,
			// ist vermutlich das Whisper-Zeitstempel-Alignment verschoben -> lieber das
			// Roh-Transkript nehmen als einen echten Satz zu verlieren (funktionserhaltend).
			if (!text && (res.text || "").trim()) {
				console.warn(
					"[Overlays] Audio-Abgleich verwarf alle Segmente -> Fallback auf Roh-Transkript",
				);
				text = (res.text || "").trim();
			}
		} else {
			text = (res.text || "").trim();
		}
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
			OV.storage.get("autoGeminiCorrection", false) &&
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
			// Neue Spracheingabe -> alter Gemini-Snapshot (Original/Korrektur) ist hinfaellig.
			OV.actions?.resetGeminiSnapshot?.();
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
					maybeTranscribe(audioBlob);
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
		previewActive = false; // Riegel: ab jetzt darf die Vorschau nichts mehr schreiben — nur noch Groq
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
