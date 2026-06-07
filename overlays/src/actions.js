// ============================================================
// actions.js — alle Button-Aktionen (geteilt + ChatGPT-spezifisch)
// ------------------------------------------------------------
// Jede Aktion bekommt das Button-Element, damit sie Lade-/Fehlerzustand
// anzeigen kann. btn.__ovRender() stellt das Standard-Aussehen wieder her
// (von ui.js gesetzt). So bleiben Daten (Registry) und Logik getrennt.
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;
	const ED = () => OV.editable;
	const PREVIEW_CHARS = 140;
	const MIN_CHARS = 6;

	// Toleranter Textvergleich: contenteditable-Felder (ChatGPT/Claude/Gemini) geben
	// beim Zuruecklesen nicht 1:1 denselben String zurueck, den man hineingeschrieben
	// hat (\n<->Zeilenumbruch, geschuetzte Leerzeichen, getrimmte Zeilen). Darum wird
	// fuer den Snapshot-Abgleich normalisiert (Whitespace kollabiert), statt strikt ===.
	function normText(s) {
		return String(s || "")
			.replace(/[​-‍﻿]/g, "")
			.replace(/\s+/g, " ")
			.trim();
	}
	function sameText(a, b) {
		return normText(a) === normText(b);
	}

	function setColors(btn, bg, fg) {
		if (!btn) return;
		btn.style.setProperty("background", bg, "important");
		btn.style.setProperty("color", fg, "important");
	}

	// Fuehrt fn aus, zeigt ⏳ waehrenddessen, ⚠️ bei Fehler, sonst Standard wieder.
	async function withBusy(btn, fn) {
		try {
			if (btn) {
				btn.textContent = "⏳";
				setColors(btn, "#444", "#fff");
			}
			const r = await fn();
			btn?.__ovRender?.();
			return r;
		} catch (e) {
			const msg = String(e?.message || e || "Fehler");
			console.warn("[Overlays] Aktion-Fehler:", msg);
			if (btn) {
				btn.textContent = "⚠️";
				setColors(btn, "#8b0000", "#fff");
				setTimeout(() => btn.__ovRender?.(), 2500);
			}
			OV.toast("❌ " + msg, 9000);
			return undefined;
		}
	}

	// ── Auto-Send (Enter) ──
	let autoSendEnabled = false;
	function autoSendIfEnabled(el) {
		if (!autoSendEnabled || !el) return;
		setTimeout(() => {
			el.dispatchEvent(
				new KeyboardEvent("keydown", {
					key: "Enter",
					code: "Enter",
					keyCode: 13,
					which: 13,
					bubbles: true,
					cancelable: true,
				}),
			);
		}, 300);
	}
	function renderAutoEnter(btn) {
		OV.setSvgIcon(btn, OV.ICONS.enter, OV.ICON_FALLBACK.enter);
		btn.style.setProperty("border-color", "transparent", "important");
		if (autoSendEnabled) {
			setColors(btn, "#16a34a", "#fff");
			btn.title = "Auto-Send AN – Spracheingabe wird automatisch abgeschickt";
		} else {
			setColors(btn, "#64748b", "#fff");
			btn.title = "Auto-Send AUS – Klicken zum Aktivieren";
		}
	}
	function toggleAutoEnter(btn) {
		autoSendEnabled = !autoSendEnabled;
		renderAutoEnter(btn);
		OV.toast(
			autoSendEnabled ? "✅ Auto-Send aktiviert" : "❌ Auto-Send deaktiviert",
			2000,
		);
	}

	// ── Paste / Copy / Clear ──
	async function paste() {
		try {
			const text = await navigator.clipboard.readText();
			if (!text || !text.trim()) return;
			const el = ED().getComposerTargetEditable();
			if (!el) {
				OV.toast("⚠️ Kein Eingabefeld gefunden.", 2000);
				return;
			}
			el.focus();
			const current = ED().readPromptText(el);
			const spacer =
				current && !current.endsWith(" ") && !current.endsWith("\n") ? " " : "";
			const ok = await ED().setViaPaste(el, current + spacer + text);
			OV.toast(ok ? "📋 Eingefuegt!" : "⚠️ Text nicht uebernommen.", 1800);
		} catch {
			OV.toast("⚠️ Kein Zugriff auf Zwischenablage.", 2000);
		}
	}
	function copy() {
		const sel = window.getSelection();
		if (sel && sel.toString().trim()) {
			navigator.clipboard.writeText(sel.toString());
			OV.toast("📋 Kopiert!", 1500);
			ED().getComposerTargetEditable()?.focus();
			return;
		}
		const el = ED().getUserTargetEditable();
		if (el) {
			const text = ED().readPromptText(el);
			if (text.trim()) {
				navigator.clipboard.writeText(text);
				OV.toast("📋 Eingabefeld kopiert!", 1500);
				el.focus();
			}
		}
	}
	async function clear() {
		const el = ED().getUserTargetEditable();
		if (!el) {
			OV.toast(
				"❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.",
				4500,
			);
			return;
		}
		const ok = await ED().setViaPaste(el, "");
		OV.toast(
			ok ? "🧹 Feld geleert." : "❌ Text konnte nicht geloescht werden.",
			ok ? 1600 : 4500,
		);
	}

	// ── Gemini-Toggle (Auto-Korrektur an/aus + Live-Korrektur des Feldtexts) ──
	// Der Schalter macht ZWEIERLEI:
	//   1) persistente Einstellung "autoGeminiCorrection" (Default AUS) — steuert, ob
	//      KUENFTIGE Spracheingaben automatisch korrigiert werden (gelesen in stt.js).
	//   2) Live auf den AKTUELL im Feld stehenden Text: AN korrigiert ihn sofort, AUS
	//      stellt den urspruenglichen (rohen) Text wieder her.
	// Der Snapshot merkt sich Original + Korrektur, damit AUS den Originaltext
	// verlustfrei wiederherstellen kann. Erneutes AN startet bewusst eine NEUE
	// Korrektur (kein Cache) — falls dem Nutzer die erste Korrektur nicht gefiel.
	// Restore ueberschreibt nichts, wenn der Nutzer seither manuell editiert hat.
	let geminiSnap = null; // { el, original, corrected }

	function resetGeminiSnapshot() {
		geminiSnap = null;
	}

	function renderGeminiToggle(btn) {
		OV.setSvgIcon(btn, OV.ICONS.spellcheck, OV.ICON_FALLBACK.spellcheck);
		btn.style.setProperty("border-color", "transparent", "important");
		if (OV.storage.get("autoGeminiCorrection", false)) {
			setColors(btn, "#22c55e", "#fff");
			btn.title =
				"Gemini-Korrektur AN – Klicken stellt den Originaltext wieder her";
		} else {
			setColors(btn, "#94a3b8", "#fff");
			btn.title =
				"Gemini-Korrektur AUS – Klicken korrigiert den aktuellen Text";
		}
	}

	// AN: korrigiert den aktuell im Feld stehenden Text IMMER frisch per Gemini und
	// merkt sich Original + Korrektur. Jedes erneute AN startet eine neue Korrektur
	// (kein Cache), damit eine ungeliebte erste Korrektur verworfen werden kann.
	async function geminiCorrectCurrent(btn) {
		const el = ED().getUserTargetEditable();
		if (!el) {
			OV.toast(
				"✅ Gemini-Korrektur aktiviert. (Kein Eingabefeld fuer die Sofort-Korrektur gefunden — beim naechsten Sprechen wird automatisch korrigiert.)",
				3500,
			);
			return;
		}
		const current = ED().readPromptText(el);

		if (!current || current.length < MIN_CHARS) {
			console.log(
				"[Overlays] Gemini-Toggle AN: Feldtext zu kurz fuer Sofort-Korrektur:",
				current.length,
			);
			geminiSnap = null;
			OV.toast(
				`✅ Gemini-Korrektur aktiviert. (Text zu kurz (${current.length}) fuer die Sofort-Korrektur.)`,
				3000,
			);
			return;
		}
		if (!OV.gemini) {
			OV.toast("⚠️ Gemini nicht verfuegbar.", 3000);
			return;
		}

		await withBusy(btn, async () => {
			OV.toast("✨ Gemini korrigiert den aktuellen Text…", 2500);
			const result = await OV.gemini.rewriteGrammarSmart(current);
			const corrected = (result || "").trim();
			if (!corrected) throw new Error("Gemini lieferte keinen Text zurueck.");
			const ok = await ED().setViaPaste(el, corrected);
			if (!ok)
				throw new Error(
					"Eingabefeld hat den korrigierten Text nicht uebernommen.",
				);
			geminiSnap = { el, original: current, corrected: ED().readPromptText(el) };
			console.log("[Overlays] Gemini-Toggle AN: korrigiert", {
				origLen: current.length,
				corrLen: corrected.length,
			});
			OV.toast("✨ Text korrigiert. Nochmal klicken = Original zurueck.", 2600);
		});
	}

	// AUS: stellt den gemerkten Originaltext wieder her — aber nur, wenn der Feldtext
	// seit der Korrektur unveraendert ist (sonst hat der Nutzer manuell editiert und
	// wir ueberschreiben nichts). Der Snapshot bleibt erhalten, damit ein erneutes AN
	// die Korrektur instant aus dem Cache holen kann.
	async function geminiRestoreOriginal() {
		if (!geminiSnap) {
			console.log(
				"[Overlays] Gemini-Toggle AUS: kein Snapshot — nichts wiederherzustellen.",
			);
			OV.toast("❌ Gemini-Korrektur deaktiviert.", 1800);
			return;
		}
		const el = geminiSnap.el;
		if (!OV.editable.isVisible(el)) {
			console.log(
				"[Overlays] Gemini-Toggle AUS: Zielfeld nicht mehr sichtbar — kein Restore.",
			);
			geminiSnap = null;
			OV.toast("❌ Gemini-Korrektur deaktiviert.", 1800);
			return;
		}
		const live = ED().readPromptText(el);
		if (!sameText(live, geminiSnap.corrected)) {
			// Nutzer hat seit der Korrektur manuell editiert -> nicht ueberschreiben.
			console.log(
				"[Overlays] Gemini-Toggle AUS: Feldtext seit Korrektur geaendert — Original NICHT ueberschrieben.",
			);
			geminiSnap = null;
			OV.toast(
				"❌ Gemini-Korrektur deaktiviert. (Feld wurde manuell geaendert — Original nicht ueberschrieben.)",
				4000,
			);
			return;
		}
		const ok = await ED().setViaPaste(el, geminiSnap.original);
		console.log("[Overlays] Gemini-Toggle AUS: Original wiederhergestellt", {
			ok,
		});
		OV.toast(
			ok
				? "↩️ Originaltext wiederhergestellt."
				: "⚠️ Konnte den Originaltext nicht wiederherstellen.",
			2400,
		);
	}

	async function toggleGemini(btn) {
		const next = !OV.storage.get("autoGeminiCorrection", false);
		await OV.storage.set("autoGeminiCorrection", next);
		renderGeminiToggle(btn);
		if (next) await geminiCorrectCurrent(btn);
		else await geminiRestoreOriginal();
	}

	// ── ChatGPT: Memory-Prompt ──
	const MEMORY_PROMPT = `
Aufgabe: Lies den gesamten Chatverlauf und extrahiere nur neue, stabile Informationen ueber mich, die zukuenftige Antworten deutlich verbessern.

Wichtig: Nichts automatisch speichern – nur Vorschlaege machen.

Deduplizierung / Updates (sehr wichtig):
- Pruefe zuerst alle bereits gespeicherten Erinnerungen ueber mich (Memory/Bio), damit du keine doppelten oder ueberschneidenden Punkte vorschlaegst.
- Wenn eine neue Information eine bestehende Erinnerung klar verbessert, schlage eine Aktualisierung vor statt einen neuen Punkt zu erzeugen.
- Wenn etwas bereits vollstaendig vorhanden ist, lasse es weg.

Regeln:
- Nur Dinge, die voraussichtlich mindestens 3 Monate gueltig sind.
- Maximal 12 Punkte.
- Jeder Punkt: 1 einfacher Satz, ohne Fachwoerter.

Ausgabeformat:
- Liste "Punkt 1 … Punkt N"
- Pro Punkt zusaetzlich in Klammern: "nuetzlich weil …" und "sensibel: ja/nein".

Nachdem ich dir die Punkte 1–N vorgeschlagen habe, nutzt du diesen Prompt:

Ich waehle folgende Punkte zum dauerhaften Speichern aus: [hier nur die Nummern eintragen].
Speichere nur diese Punkte als dauerhafte Erinnerungen, exakt als einfache Saetze. Starte jede neu gespeicherte dauerhafte Erinnerung mit : "Frank"... (Beispiel: Frank mag Kiefernwaelder.)
`.trim();

	async function memory(btn) {
		const el = ED().getComposerTargetEditable();
		if (!el) {
			OV.toast(
				"❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.",
				6500,
			);
			return;
		}
		await withBusy(btn, async () => {
			const ok = await ED().setViaPaste(el, MEMORY_PROMPT);
			if (!ok)
				throw new Error("Eingabefeld hat den Memory-Prompt nicht uebernommen.");
			OV.toast("✅ Memory-Prompt eingefuegt.", 1800);
		});
	}

	// ── ChatGPT: Prompt-Builder ──
	function buildFinalPrompt(taskText, domainsList) {
		const domains = domainsList.join(", ");
		return `
Rolle:
Du bist ein interdisziplinaeres Forscherteam aus den 5 weltweit fuehrenden Wissenschaftlerinnen und Wissenschaftlern, spezialisiert auf ${domains}. Ihr arbeitet evidenzbasiert, kritisch, sehr ausfuehrlich, konsensorientiert, erklaert alle Zusammenhaenge ganz genau und begruendet Empfehlungen ausfuehrlich. Ihr betrachtet die Aufgabe aus allen moeglichen verschiedenen Perspektiven.

Aufgabe:
${taskText}
Falls dir etwas zur Aufgabe nicht klar ist, frage nochmal den Benutzer. ChatGPT soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein allumfassendes Bild fuer die Antwort einbezogen wird.

Zielgruppe:
Frank (Benutzer dieses Chats).

Kontext:
Nutze die ueber Frank gespeicherten Informationen und Erinnerungen (Bio, Ziele, Routinen, Besonderheiten, Projekte, usw.) als primaeren Bezugsrahmen fuer diese Aufgabe. Keine zusaetzlichen Kontextabfragen; verwende vorhandenes Wissen.

Format:
Ausgabestruktur: "Einleitung – Hauptteil – Erkenntnisse". Fachbegriffe/Fremdwoerter bei Erstnennung sofort kurz erklaeren in (Klammern). Abkuerzungen bei Erstnennung ausschreiben und kurz erklaeren in (Klammern). Der Textaufbau ist logisch, faktenbasiert, umfassend, nachvollziehbar. Keine Stichpunkte; Tabellen und andere stilistische Illustrationen, wenn sie die Verstaendlichkeit verbessern. Der letzte Teil fasst alle Erkenntnisse aus dem Hauptteil der Wichtigkeit nach sortiert ausfuehrlich zusammen, mit klaren Ueberschriften und erkennbarer Prioritaet. Jede Erkenntnis hat minimal 2, maximal 13 Zeilen. Maximale Wortanzahl: 10000. Entscheidend ist, dass die Frage sehr ausfuehrlich beantwortet wird.

Ton:
Wissenschaftlich-professionell, praezise, didaktisch klar, ausfuehrlich, ohne metasprachliche Hinweise oder Floskeln, leicht verstaendlich.

Pflichtlogik:
Keine Abweichung bei Zielgruppe, Kontext, Format, Ton.
`.trim();
	}

	function buildFinalPromptGeneral(taskText, domainsList) {
		const domains = domainsList.join(", ");
		return `
Rolle:
Du bist ein interdisziplinaeres Forscherteam aus den 5 weltweit fuehrenden Wissenschaftlerinnen und Wissenschaftlern, spezialisiert auf ${domains}. Ihr arbeitet evidenzbasiert, kritisch, sehr ausfuehrlich, konsensorientiert, erklaert alle Zusammenhaenge ganz genau und begruendet Empfehlungen ausfuehrlich. Ihr betrachtet die Aufgabe aus allen moeglichen verschiedenen Perspektiven.

Aufgabe:
${taskText}
Falls dir etwas zur Aufgabe nicht klar ist, frage nochmal den Benutzer. ChatGPT soll die Aufgabe nicht durch weitere Handlungsanweisungen erweitern! Die Aufgabe soll aus unterschiedlichsten wissenschaftlichen Perspektiven betrachtet werden, sodass ein allumfassendes Bild fuer die Antwort einbezogen wird.

Zielgruppe:
Geschrieben fuer jedermann; verstaendlich fuer Lernende etwa auf Niveau 10. Klasse Realschule.

Kontext:
Kein weiterer Kontext ausser dem Inhalt der Aufgabe. Keine zusaetzlichen Kontextabfragen.

Format:
Ausgabestruktur: "Einleitung – Hauptteil – Erkenntnisse". Fachbegriffe/Fremdwoerter bei Erstnennung sofort kurz erklaeren in (Klammern). Abkuerzungen bei Erstnennung ausschreiben und kurz erklaeren in (Klammern). Der Textaufbau ist logisch, faktenbasiert, umfassend, nachvollziehbar. Keine Stichpunkte; Tabellen und andere stilistische Illustrationen, wenn sie die Verstaendlichkeit verbessern. Der letzte Teil fasst alle Erkenntnisse aus dem Hauptteil der Wichtigkeit nach sortiert ausfuehrlich zusammen, mit klaren Ueberschriften und erkennbarer Prioritaet. Jede Erkenntnis hat minimal 2, maximal 13 Zeilen. Maximale Wortanzahl: 10000. Entscheidend ist, dass die Frage sehr ausfuehrlich beantwortet wird.

Ton:
Wissenschaftlich-professionell, klar und praezise; sehr ausfuehrlich, didaktisch verstaendlich ohne Floskeln. Keine metasprachlichen Hinweise.

Pflichtlogik:
Zielgruppe, Kontext, Format und Ton duerfen niemals abweichen.
`.trim();
	}

	async function runPromptBuilder(btn, builder, labelName) {
		const el = ED().getComposerTargetEditable();
		if (!el) {
			OV.toast(
				"❌ Eingabefeld nicht gefunden. Tipp: erst ins Ziel-Feld klicken.",
				6500,
			);
			return;
		}
		const snap = ED().readPromptText(el);
		if (snap.length < MIN_CHARS) {
			// Klare Erklaerung: der Builder braucht einen vorhandenen Text zum Ausbauen.
			OV.toast(
				`Text zu kurz (${snap.length}) – schreib oder sprich zuerst deine Frage/Aufgabe ins Feld, dann den Button druecken.`,
				4500,
			);
			return;
		}
		await withBusy(btn, async () => {
			OV.toast("✨ Prompt-Builder: Fachdomaenen ableiten…", 1400);
			const domains = await OV.gemini.extractDomainsRobust(snap);
			const finalPrompt = builder(snap, domains);
			const preview = finalPrompt.replace(/\s+/g, " ").slice(0, PREVIEW_CHARS);
			OV.toast(
				"🧱 Vorschau:\n" +
					preview +
					(finalPrompt.length > PREVIEW_CHARS ? " …" : ""),
				3500,
			);
			const ok = await ED().setViaPaste(el, finalPrompt);
			if (!ok)
				throw new Error(
					"Eingabefeld hat den Builder-Prompt nicht uebernommen.",
				);
			OV.toast(
				`✅ Prompt (${labelName}) erstellt. (Domaenen: ${domains.length})`,
				2200,
			);
		});
	}

	OV.actions = {
		autoSendIfEnabled,
		renderAutoEnter,
		toggleAutoEnter,
		paste,
		copy,
		clear,
		renderGeminiToggle,
		toggleGemini,
		resetGeminiSnapshot,
		memory,
		promptFrank: (btn) => runPromptBuilder(btn, buildFinalPrompt, "Frank"),
		promptGeneral: (btn) =>
			runPromptBuilder(btn, buildFinalPromptGeneral, "Allgemein/10. Klasse"),
	};
})();
