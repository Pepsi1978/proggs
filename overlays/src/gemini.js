// ============================================================
// gemini.js — Gemini-Engine (Grammatik-Korrektur + Domaenen-Finder)
// ------------------------------------------------------------
// Reine Gemini-Logik. Der eigentliche fetch laeuft im Service Worker;
// hier nur Prompt-Bau, Parsing und die robuste Domaenen-Extraktion
// (1:1 aus chatgpt.user.js portiert).
// ============================================================
(() => {
	window.__chromeOverlays__ = window.__chromeOverlays__ || {};
	const OV = window.__chromeOverlays__;

	const CFG = {
		grammarMaxOutputTokens: 8192,
		grammarChunkChars: 3500,
		grammarTruncationRatio: 0.85,
	};
	const DOMAIN_CFG = {
		minDomains: 7,
		maxDomains: 10,
		temperature: 0.1,
		maxOutputTokens: 1024,
		repairMaxOutputTokens: 1024,
	};

	function model() {
		// Leer = der Service Worker waehlt automatisch ein fuer den Key gueltiges Modell.
		return OV.storage.get("geminiModel", "");
	}

	// Ruft den Service Worker; wirft bei Fehler (fuer try/catch in actions).
	async function generate(
		prompt,
		{ temperature = 0.4, maxOutputTokens = 2048 } = {},
	) {
		let res;
		try {
			res = await chrome.runtime.sendMessage({
				type: "geminiGenerate",
				prompt,
				model: model(),
				temperature,
				maxOutputTokens,
			});
		} catch (e) {
			throw new Error(String(e?.message || e));
		}
		if (!res || !res.ok) throw new Error(res?.error || "Gemini-Fehler");
		return res.text;
	}

	function cleanText(s) {
		return String(s || "")
			.replace(/[\u200B-\u200D\uFEFF]/g, "")
			.trim();
	}

	// ── Grammatik / Intentions-Korrektur ──
	function buildGrammarPrompt(text) {
		return `
Du bist ein deutscher Textredakteur fuer diktierte Spracheingaben.

AUFGABE:
Du erhaeltst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, praezise und sprachlich hochwertig** zum Ausdruck kommt.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdruecken?
2) Entferne Diktat-Artefakte: Fuelllaute ("aeh", "aehm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
3) Formuliere Saetze so um, dass die erkannte Intention **klar und gut lesbar** wird.
4) Korrigiere Grammatik, Zeichensetzung und Gross-/Kleinschreibung.

GRENZEN (strikt):
- Keine neuen Informationen, Fakten oder Inhalte hinzufuegen.
- Keine Vermutungen ueber nicht Gesagtes.
- Die Intention des Originals muss vollstaendig erhalten bleiben.
- Sprache: Deutsch.

REGEL:
Gib AUSSCHLIESSLICH den ueberarbeiteten Text zurueck. Keine Kommentare. Keine Erklaerungen. Kein Praefix.

TEXT:
${text}
`.trim();
	}

	function rewriteGrammar(text) {
		return generate(buildGrammarPrompt(text), {
			temperature: 0.4,
			maxOutputTokens: CFG.grammarMaxOutputTokens,
		});
	}

	function splitIntoChunksByParagraphs(text, maxChars) {
		const s = String(text || "");
		if (s.length <= maxChars) return [s];
		const paras = s.split(/\n{2,}/g);
		const chunks = [];
		let buf = "";
		const pushBuf = () => {
			if (buf.trim().length) chunks.push(buf);
			buf = "";
		};
		for (let p of paras) {
			p = p.trim();
			if (!p) continue;
			if (p.length > maxChars) {
				pushBuf();
				let start = 0;
				while (start < p.length) {
					let end = Math.min(start + maxChars, p.length);
					if (end < p.length) {
						const windowStart = Math.max(
							start,
							end - Math.floor(maxChars * 0.5),
						);
						const slice = p.slice(windowStart, end);
						const lastDot = Math.max(
							slice.lastIndexOf("."),
							slice.lastIndexOf("!"),
							slice.lastIndexOf("?"),
							slice.lastIndexOf(";"),
							slice.lastIndexOf(":"),
						);
						if (lastDot > -1) end = windowStart + lastDot + 1;
					}
					chunks.push(p.slice(start, end).trim());
					start = end;
				}
				continue;
			}
			const candidate = buf ? buf + "\n\n" + p : p;
			if (candidate.length > maxChars) {
				pushBuf();
				buf = p;
			} else {
				buf = candidate;
			}
		}
		pushBuf();
		return chunks.length ? chunks : [s];
	}

	async function rewriteGrammarSmart(fullText, onProgress) {
		const input = String(fullText || "");
		if (!input.trim()) return input;
		const oneShot = await rewriteGrammar(input);
		const ratio = oneShot.length / Math.max(1, input.length);
		if (ratio >= CFG.grammarTruncationRatio) return oneShot;
		const chunks = splitIntoChunksByParagraphs(input, CFG.grammarChunkChars);
		const results = await Promise.all(
			chunks.map(async (chunk, i) => {
				onProgress?.(i + 1, chunks.length);
				const fixed = await rewriteGrammar(chunk);
				return fixed && fixed.trim().length ? fixed.trim() : chunk.trim();
			}),
		);
		return results.join("\n\n").trim();
	}

	// ── Domaenen-Extraktion (robust, mit Repair-Pass) ──
	function extractTaskOnly(maybePrompt) {
		const s = String(maybePrompt || "");
		const idx = s.search(/\nAufgabe:\s*/i);
		if (idx >= 0) {
			const after = s.slice(idx).replace(/^.*?\nAufgabe:\s*/i, "");
			const endIdx = after.search(
				/\n(Zielgruppe:|Kontext:|Format:|Ton:|Pflichtlogik:|Falls dir)\s*/i,
			);
			const task = endIdx >= 0 ? after.slice(0, endIdx) : after;
			const t = cleanText(task);
			return t.length ? t : cleanText(maybePrompt);
		}
		return cleanText(maybePrompt);
	}

	function normalizeDomain(x) {
		let s = cleanText(x);
		if (!s) return "";
		s = s.replace(/^[[({]+/g, "").replace(/[\])}]+$/g, "");
		s = s.replace(/^["'`]+|["'`]+$/g, "");
		s = s.replace(/^\s*[-–—••]\s*/g, "");
		s = s.replace(/^\s*\d+[.)]\s*/g, "");
		s = s.replace(/^here is the json requested:\s*/i, "");
		s = s.replace(/^(topic_domains|method_domains)\s*:\s*/i, "");
		s = s.replace(/\.$/, "").trim();
		return s;
	}

	function isBannedGeneric(domain) {
		const d = domain.toLowerCase();
		if (d.includes("unterbereich")) return true;
		if (d.includes("fachgebiet")) return true;
		if (d.includes("domaenenanalyse")) return true;
		if (
			d === "naturwissenschaften" ||
			d === "informatik" ||
			d === "ingenieurwissenschaften" ||
			d === "sozialwissenschaften" ||
			d === "rechtswissenschaften" ||
			d === "wirtschaftswissenschaften"
		)
			return true;
		if (d.length < 3) return true;
		return false;
	}

	function isMethodDomain(domain) {
		const d = domain.toLowerCase();
		return (
			d.includes("evidenz") ||
			d.includes("statistik") ||
			d.includes("studienmethod") ||
			d.includes("risiko") ||
			d.includes("trade-off") ||
			d.includes("ethik") ||
			d.includes("systemdenken") ||
			d.includes("anwendungsplanung") ||
			d.includes("evaluation") ||
			d.includes("methodik")
		);
	}

	function parseDomainsLoose(raw) {
		const s = String(raw || "").trim();
		if (!s) return [];
		const candidates = [];
		const quoted = [...s.matchAll(/"([^"]{2,160})"/g)].map((m) => m[1]);
		if (quoted.length) candidates.push(...quoted);
		candidates.push(...s.replace(/[••]/g, " ").split(/[,;\n]/g));
		const out = [];
		const seen = new Set();
		for (const c of candidates) {
			const d = normalizeDomain(c);
			if (!d) continue;
			const key = d.toLowerCase();
			if (key === "topic_domains" || key === "method_domains") continue;
			if (key.includes("here is the json requested")) continue;
			if (isBannedGeneric(d)) continue;
			if (seen.has(key)) continue;
			seen.add(key);
			out.push(d);
		}
		return out;
	}

	function looksSuspiciousRaw(raw) {
		const s = String(raw || "").trim();
		if (!s) return true;
		if (/…|\.{3}|,\s*$/.test(s)) return true;
		if (s.length < 25) return true;
		if ((s.match(/,/g) || []).length < 3) return true;
		return false;
	}

	function buildDomainsPrompt(taskText, minN, maxN) {
		return `
Du bist ein "Fachdomaenen-Detektor".

Ziel:
Leite aus der Aufgabe die spezifischsten passenden Fachdomaenen (wissenschaftliche Disziplinen / Teilgebiete) ab.

Regeln (wichtig):
- Ausgabe NUR als kommagetrennte Liste.
- KEIN JSON. KEINE Klammern. KEINE Anfuehrungszeichen. KEINE Nummerierung. KEINE Bulletpoints. KEINE Saetze.
- Mindestens ${minN} und hoechstens ${maxN} Eintraege.
- Mindestens 5 Eintraege muessen thematische Subdomaenen (Themenkern) sein.
- Maximal 2 Eintraege duerfen methodische Domaenen sein (z.B. Evidenzbewertung, Statistik).
- Verwende spezifische Subdomaenen statt Oberkategorien.
- Keine Abkuerzungen/Abschneidungen. Alles vollstaendig ausschreiben.

AUFGABE:
${taskText}
`.trim();
	}

	function buildDomainsRepairPrompt(taskText, rawList, minN, maxN) {
		return `
Du bekommst eine (moeglicherweise abgeschnittene oder fehlerhafte) Domaenenliste und sollst sie korrigieren.

Aufgabe:
- Ersetze Wortstaemme/Abkuerzungen durch vollstaendig ausgeschriebene Fachdomaenen.
- Entferne generische Oberkategorien und ersetze sie durch spezifische Subdomaenen zum Themenkern.
- Ergebnis: Mindestens ${minN}, hoechstens ${maxN} Eintraege.
- Mindestens 5 thematische Subdomaenen, maximal 2 methodische Domaenen.

Format-Regeln (strikt):
- Ausgabe NUR als kommagetrennte Liste.
- KEIN JSON. KEINE Klammern. KEINE Anfuehrungszeichen. KEINE Nummerierung. KEINE Saetze.

AUFGABE:
${taskText}

ROH-LISTE (zu reparieren):
${rawList}
`.trim();
	}

	function buildDomainsPromptSecondPass(taskText, existingList, needN) {
		return `
Es fehlen noch Fachdomaenen.

REGELN:
- Ausgabe NUR als kommagetrennte Liste.
- KEIN JSON. KEINE Klammern. KEINE Anfuehrungszeichen. KEINE Nummerierung. KEINE Saetze.
- Liefere GENAU ${needN} zusaetzliche Domaenen.
- Wiederhole keine vorhandenen Domaenen.
- Nur Domaenen, die direkt zum Themenkern passen; hoechstens 1 methodische Domaene.

VORHANDEN:
${existingList.join(", ")}

AUFGABE:
${taskText}
`.trim();
	}

	function pickFinalDomains(list, minN, maxN) {
		const topic = list.filter((d) => !isMethodDomain(d));
		const method = list.filter((d) => isMethodDomain(d));
		const final = [];
		const seen = new Set();
		const add = (x) => {
			const k = x.toLowerCase();
			if (seen.has(k)) return;
			seen.add(k);
			final.push(x);
		};
		for (const t of topic) {
			add(t);
			if (final.length >= maxN) break;
		}
		if (final.length < maxN) {
			let mCount = 0;
			for (const m of method) {
				if (mCount >= 2) break;
				add(m);
				mCount++;
				if (final.length >= maxN) break;
			}
		}
		if (final.length < minN) {
			for (const x of list) {
				add(x);
				if (final.length >= minN) break;
			}
		}
		return final.slice(0, maxN);
	}

	async function extractDomainsRobust(taskText) {
		const coreTask = extractTaskOnly(taskText);
		const raw1 = await generate(
			buildDomainsPrompt(
				coreTask,
				DOMAIN_CFG.minDomains,
				DOMAIN_CFG.maxDomains,
			),
			{
				temperature: DOMAIN_CFG.temperature,
				maxOutputTokens: DOMAIN_CFG.maxOutputTokens,
			},
		);
		let list = parseDomainsLoose(raw1);
		const suspicious =
			looksSuspiciousRaw(raw1) || list.length < DOMAIN_CFG.minDomains;
		if (suspicious) {
			const rawRepair = await generate(
				buildDomainsRepairPrompt(
					coreTask,
					raw1,
					DOMAIN_CFG.minDomains,
					DOMAIN_CFG.maxDomains,
				),
				{
					temperature: 0.05,
					maxOutputTokens: DOMAIN_CFG.repairMaxOutputTokens,
				},
			);
			const repaired = parseDomainsLoose(rawRepair);
			if (repaired.length) list = repaired;
		}
		if (list.length < DOMAIN_CFG.minDomains) {
			const need = DOMAIN_CFG.minDomains - list.length;
			const raw2 = await generate(
				buildDomainsPromptSecondPass(coreTask, list, need),
				{
					temperature: 0.08,
					maxOutputTokens: DOMAIN_CFG.maxOutputTokens,
				},
			);
			const add = parseDomainsLoose(raw2);
			const seen = new Set(list.map((x) => x.toLowerCase()));
			for (const a of add) {
				const k = a.toLowerCase();
				if (!seen.has(k) && !isBannedGeneric(a)) {
					list.push(a);
					seen.add(k);
				}
				if (list.length >= DOMAIN_CFG.maxDomains) break;
			}
		}
		list = pickFinalDomains(list, DOMAIN_CFG.minDomains, DOMAIN_CFG.maxDomains);
		if (list.length < 3)
			return [
				"Allgemeinwissen",
				"Interdisziplinaere Analyse",
				"Kritisches Denken",
			];
		return list;
	}

	OV.gemini = { generate, rewriteGrammarSmart, extractDomainsRobust };
})();
