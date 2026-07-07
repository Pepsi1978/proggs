# Anti-Halluzinations-Regeln fuer KI-Modelle — Best Practices (Stand 2026-06-27)

> **Worum es geht:** Wie man Coding-/Agent-Modellen per **Regeln** (AGENTS.md, CLAUDE.md,
> System-Prompt, Cursor-/OpenCode-Rules) Halluzinationen abgewoehnt — und besonders, wie man
> **stark halluzinierende, aber sonst gute/guenstige Modelle** (z. B. GLM-4.x Flash/Air, kleine
> Modelle) trotzdem einigermassen sicher nutzbar macht. Plattform-/CLI-uebergreifend
> (OpenCode, Codex, Cursor, Claude Code).
>
> **Quellen:** recherchiert **2026-06-27** (9 Researcher: Firecrawl + MiniMax M3, max Thinking).
> Alle Befunde `extern` (Studien, Hersteller-Docs, Praxis-Blogs, GitHub) — klar gelabelt; eine
> falsche Best-Practice ist schlimmer als keine, daher ist hart getrennt, was **belegt** und was
> **unsicher/Plausibilitaet** ist. Rohergebnisse: `~/.research-swarm/answer-*.txt` (Session 2026-06-27).
>
> **Wichtige Abgrenzung:** *Halluzination* (das Modell **erfindet** etwas — Datei, API, Fakt) ist
> NICHT dasselbe wie *Over-Editing* (das Modell **aendert mehr als noetig**). Over-Editing + Modell-/
> Thinking-Wahl stehen in `best-practices/opencode/vibe-coding-modelle.md`; AGENTS.md-Mechanik
> (Precedence, `/init`) in `best-practices/opencode/agents-md-memory.md`. Diese Datei behandelt NUR
> das Erfinden. Falle "Regeln werden ignoriert": `bugs/claude-tooling/claude-config.md` §1.1.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektuere (`Read` mit
> `limit=80`). Volltext bei tieferem Bedarf (unten).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Modell halluziniert Dateien/Funktionen/APIs | **Tool-first, nicht Memory-first** — erst Read/Grep/Bash, dann behaupten. „Die Datei hat immer recht, dein Gedaechtnis oft nicht." | §1 |
| 2 | Regel-Grundgeruest fuer AGENTS.md/CLAUDE.md | Die **5 Kern-Regeln** (I-don't-know · tool-first · kein Ketten-Raten · sofort zurueckziehen · Quelle nennen) als Block einsetzen | §1 |
| 3 | Modell raet statt zuzugeben | „Ich weiss es nicht" **explizit erlauben** (Abstention) — Raten erzeugt Halluzination | §2, §5 |
| 4 | Modell soll Fakten belegen | **Zitatpflicht** („According to…") — +~20 % Genauigkeit; „kein Beleg = keine Behauptung" | §2 |
| 5 | RAG/Kontext-Antwort | „Antworte **NUR** aus dem bereitgestellten Kontext; fehlt es, sag ‚insufficient data'" | §2, §3 |
| 6 | Staerkster Einzelhebel | **Grounding** (Kontext bereitstellen / Web-Suche / RAG) — Web-Suche allein −73–86 % | §3 |
| 7 | Strukturierte JSON-Ausgabe erzwingen | **Nur MIT Grounding** — Schema-Zwang OHNE Grounding macht es schlimmer (+10–15 pp) | §3 |
| 8 | Reasoning-Felder im Schema | Pflicht-Reasoning-Feld **zuerst** im Schema (Vonage: 23,7 % → 1,0 % Fehler) | §3 |
| 9 | Faktentreue Aufgabe | **Niedrige Temperatur** (0.0–0.3) — Praxis-Empfehlung (kein harter empirischer Beweis) | §4 |
| 10 | Wichtige Fakten absichern | **Chain-of-Verification** — Verifikationsfragen UNABHAENGIG beantworten (sonst Wiederholung) | §5 |
| 11 | Stark halluzinierendes/billiges Modell (GLM-Flash &Co.) | **Thinking AN + enger Scope + Tool-Zwang + Verifikation durch staerkeres Modell** | §6 |
| 12 | Regel MUSS sicher greifen | Prompt-Regeln sind **Bitten**, keine Gesetze — kritisches per **Hook/Code** erzwingen | §7 |
| 13 | „Confidence" des Modells | Confidence ≠ Accuracy (51,4 % selbstsicherer Antworten widerlegt) — nicht darauf verlassen | §5, §8 |

---

## 1. Die 5 Kern-Regeln — das Drop-in-Grundgeruest fuer AGENTS.md / CLAUDE.md

Der direkteste, praxistauglichste Baustein ist ein etablierter **Anti-Hallucination-Block**, der
1:1 in `CLAUDE.md`, `AGENTS.md`, `.cursorrules`, `.windsurfrules` oder jeden Agent-System-Prompt
kopiert wird (`extern`, GitHub-Gist „Anti-Hallucination System for AI Coding Agents", Recherche-Quelle 1 / Researcher 2). Die fuenf Pflicht-Regeln:

| # | Regel | Was sie verlangt |
|---|-------|------------------|
| 1 | **„Ich weiss es nicht" sagen** | Unsicherheit zugeben. „Ich hab's noch nicht geprueft, ich schaue nach" oder „Ich weiss es nicht" statt zu raten. |
| 2 | **Tool-first, nicht Memory-first** | Bevor du ueber **Datei, API, Config, Projekt-Zustand** redest: **erst ein Tool** (Read, Grep, Bash) den IST-Zustand pruefen lassen. „Dein Gedaechtnis, wie der Code funktioniert, ist oft falsch — die Datei hat immer recht." |
| 3 | **Kein Ketten-Raten** | Nach **einer** unverifizierten Vermutung stoppen; keine weiteren Antworten darauf aufbauen. |
| 4 | **Sofort zurueckziehen** | Mitten in der Antwort stoppen, wenn du merkst, dass du falsch liegst; keinen selbstsicher-falschen Satz zu Ende bringen. |
| 5 | **Quelle nennen** | Sagen, aus WELCHER Datei/Zeile/Tool-Ausgabe der Fakt stammt. **„No source = no claim."** |

Der Block deckt laut Quelle ausdruecklich ab: *„code behavior, file contents, API details, project
state, deployment status, visual appearance, config values, error causes"* — also genau die
typischen Coding-Halluzinationen (erfundene Funktionen/APIs/Libraries). Verstaerkende Zusatzregel
aus derselben Quelle: *„NEVER tell the user something ‚is' a certain way unless you have verified it
with your own tools. After making changes, verify the result before claiming success."*

### Fertiger Block zum Kopieren (Deutsch, an Franks Stil angelehnt)

```markdown
## Anti-Halluzination (PFLICHT — kein erfinden)
1. Ich-weiss-es-nicht: Bei Unsicherheit sage "Ich weiss es nicht" oder "noch nicht
   geprueft, ich schaue nach" — NIE raten und als Fakt verkaufen.
2. Tool-first statt Memory-first: Bevor ich etwas ueber eine Datei, Funktion, API,
   Config oder den Projektzustand behaupte, pruefe ich es ZUERST mit einem Tool
   (Read/Grep/Bash). Die Datei hat recht, mein Gedaechtnis oft nicht.
3. Kein Ketten-Raten: Nach genau EINER unverifizierten Vermutung stoppe ich und baue
   nichts weiter darauf auf.
4. Sofort zurueckziehen: Merke ich mitten in der Antwort, dass ich falsch liege, breche
   ich ab — ich bringe keinen selbstsicher-falschen Satz zu Ende.
5. Quelle nennen: Ich sage, aus welcher Datei/Zeile/Tool-Ausgabe ein Fakt stammt.
   Kein Beleg = keine Behauptung. Erfinde NIE Funktionen, Imports, Paketnamen,
   Config-Keys oder API-Methoden — verifiziere sie (grep/Doku) bevor du sie nutzt.
```

> **Wichtig (siehe §7):** Dieser Block ist die *erste* Verteidigungslinie, nicht die letzte.
> Empirisch befolgen Modelle solche Regeln nur teilweise. Was IMMER greifen muss, gehoert in einen
> **Hook/Code** (Franks Hook-System ist genau diese „Gesetzes"-Ebene).

---

## 2. Anti-Halluzinations-Prompt-Bausteine (kopierbare Snippets + belegte Effekte)

Konkrete, in den Quellen belegte Formulierungen (`extern`, Researcher 1-Nachsuche: Learn-Prompting,
Mirascope, Parasoft, „According-to"-Studie):

### 2a. „Ich weiss es nicht" erlauben (Abstention)
```
You are a fact-checking assistant. If you are not confident in an answer, respond:
'I don't have enough information to answer that.' If confident, give your answer with a
short justification.
```
Weitere Varianten: `… If unsure, respond 'I don't know.'` · `If any information is unavailable,
write 'Data not available' instead of estimating.`
> Quelle warnt ehrlich: *„This is a good first line of defense, but nothing is stopping an LLM from
> disregarding those directions with some regularity."* → mit anderen Techniken kombinieren.

### 2b. Raten/Schaetzen verbieten
```
Do not estimate or approximate. If any information is unavailable, write
'Data not available' instead of estimating.
```
Plus Scope begrenzen (verhindert „speculative or tangential statements"):
`In no more than 100 words, summarize … If unsure, respond 'I don't know.'`

### 2c. Zitatpflicht — „According to…"-Prompting  (belegt: +~20 % Genauigkeit)
```
According to <autoritative Quelle>, <Frage>? Please cite specific sources for each claim.
```
Beispiele: `Based on the official documentation…` · `According to peer-reviewed research…` ·
`As stated in the company's quarterly report…`. Wirkung (Researcher 1): die „According to"-Technik
kann die Genauigkeit **um bis zu 20 %** verbessern.

### 2d. Nur aus bereitgestelltem Kontext antworten (RAG-Guardrail)
```
Using ONLY the information in the following document, answer the question below.
If the document doesn't contain the answer, respond with 'insufficient data.'
[DOCUMENT] … [/DOCUMENT]
Question: …
Answer based only on the provided documents.
```
Kombinierbar mit Confidence-Ausgabe:
```
- Main Finding: [one sentence]
- Supporting Evidence: [quotes from context with document references]
- Confidence Level: [0-10]
```

### 2e. Weitere belegte Verstaerker (Researcher 1-Nachsuche)
| Technik | belegter Effekt |
|---------|-----------------|
| **Chain-of-Thought** (Schritt-fuer-Schritt) | bis +30 % Genauigkeit |
| **Step-Back-Prompting** (erst Prinzip, dann Detail) | bis +36 % vs. Standard-CoT |
| **Chain-of-Verification** (CoVe) | bis +23 % (siehe §5) |
| **Self-Verification** | „Identify any claims you're less than 90 % confident about" |

> **Grenze (ehrlich):** *„Prompt engineering … is not a complete solution to AI hallucinations"* —
> kein Silver Bullet. Prompt-Bausteine senken die Rate, eliminieren sie nicht.

---

## 3. Grounding / RAG / strukturierte Ausgabe — der staerkste Hebel (mit Falle)

`extern`, Researcher 3 + 7 (starke Evidenz, u. a. Cross-Provider-Ablation Menxhiqi & Marinova 2026
ueber 6.912 API-Calls; ServiceNow arXiv 2404.08189; Vonage-Produktivsystem; Zep; Stanford-Legal-RAG):

- **Grounding ist der groesste Einzelhebel.** „The model no longer has to know the answer, only to
  read it." Web-Suche aktivieren senkt Halluzination **um 73–86 %** (Researcher 7). RAG senkt
  Recht-Halluzination von **58–82 % (blanke LLMs) auf 17–33 %** (Stanford/Dahl et al. 2024).
- **Volle Architektur (DB-Grounding + geschlossenes Vokabular + JSON-Zwang):** Halluzinationsrate
  **59–74 % → 3,3–14,9 %** (Cross-Provider, stabil ueber Modellgenerationen).
- **⚠️ Die Schema-Falle (C3-Anomalie):** **JSON-Schema-Zwang OHNE Grounding ERHOEHT** die
  Halluzination (+10,1 pp Gen1, +15,1 pp Gen2) — Hypothese: „slot-filling pressure", das Modell
  fuellt Pflichtfelder aus Trainings-Priors. → **Schema-Constraints NIEMALS ohne Grounding-Vokabular.**
- **Reasoning-Felder zuerst im Schema:** Pflicht-Reasoning-Feld als **erstes** required-Feld
  deklarieren, intern halten. Vonage: Fehlerquote **23,7 % → 1,0 %**. Grund: LLMs erzeugen JSON
  tokenweise — fruehe Felder praegen den Pfad.
- **Constrained Decoding** „removes a whole class of hallucination by construction" — wenn die
  Antwort eine bekannte Form hat (Enum, gueltige IDs, Schema), kann eine ganze Halluzinationsklasse
  per Konstruktion verschwinden.
- **Kontextplatzierung:** relevante Fakten an **Anfang oder Ende** des Kontextfensters
  („Lost in the Middle", Liu et al. 2307.03172).
- **Reihenfolge fuer Agentenregeln:** (1) Lookup/Grounding-Vokabular ermitteln → (2) Closed-Vocabulary
  im Prompt/Schema → (3) JSON-Zwang **erst nach 1+2**. Nie Schritt 3 ohne 1+2.
- **Faithfulness ≠ Factuality:** RAG fixt Factuality, aber das Modell kann die gefundene Quelle
  trotzdem falsch lesen (Faithfulness) — Verifikation (§5) bleibt noetig.

**Tool-Zwang speziell** (Function-Calling erzwingen statt Freitext): in den Quellen NICHT als
isolierte Technik mit Zahlen belegt — die Wirkung laeuft ueber Grounding + Schema + den
„tool-first"-Reflex (§1, Regel 2). Plausibel und in Franks Setup bewaehrt, aber als eigener
Zahlen-Beleg `unsicher`.

---

## 4. Sampling-/Decoding-Parameter

`extern`, Researcher 6 (IBM, Hugging-Face/Chip Huyen, machinelearningplus, Thoughtworks, arXiv Min-p):

| Temperatur | Verhalten | Einsatz |
|---|---|---|
| **0.0** (= Greedy = Top-K 1) | deterministisch-naheliegend | Code-Generierung, faktische Q&A |
| 0.1–0.3 | sehr fokussiert | strukturierte Extraktion, Klassifikation |
| 0.5–0.7 (Default 0.7) | balanciert | Chat, Zusammenfassung |
| 0.8–1.0 | kreativ | Brainstorming, kreatives Schreiben |
| 1.2–2.0 | wild | experimentell |

- **Praxis-Empfehlung:** fuer faktentreue/grounded Tasks **niedrige Temperatur (0.0–0.3)**; Greedy ist
  „desirable in less creative or fact-based use cases".
- **⚠️ Ehrliche Einordnung:** Es gibt **keine kontrollierte Studie** in den Quellen, die belegt, dass
  niedrige Temperatur die Halluzinationsrate *quantitativ* senkt — nur Plausibilitaet + Konsistenz.
  Greedy hat zudem Nachteile (repetitiv, generisch).
- **Top-p (Nucleus):** typisch 0.9–0.95; passt sich dynamischer an als Top-K. Bei hoher Temperatur
  laesst Top-p noch Low-Prob-Tokens zu → Inkohaerenz. Top-K + Top-p kombinierbar (Top-K zuerst).
- **Min-p** (kontext-sensitive Schwelle): robust bei hoher Temperatur, kann Greedy uebertreffen
  (Balance Diversitaet/Genauigkeit). Beispiel DeepSeek-R1: `min_p = 0.05`.
- **Frequency-/Presence-Penalty:** in den Quellen **nicht** zu Halluzination behandelt → `unsicher`.

**Faustregel:** Fuer Coding-/Fakten-Agenten **Temperatur niedrig** (0–0.3) — als guenstiger Default,
nicht als Wundermittel.

---

## 5. Verifikation & Selbstpruefung

`extern`, Researcher 4 + 7 (CoVe: Dhuliawala et al. 2023; SelfCheckGPT: Manakul et al. 2023; OpenAI-Abstention 2025):

- **Chain-of-Verification (CoVe), 4 Schritte:** (1) Baseline-Antwort → (2) Verifikationsfragen planen
  → (3) Verifikationsfragen **beantworten** → (4) revidierte Endantwort. **Schluessel:** die
  „factored"-Variante — Verifikationsfragen **unabhaengig vom Originalkontext** beantworten, sonst
  *„models that attend to existing hallucinations … tend to repeat the hallucinations"*.
  Lite-Regel fuer einen Agenten (konditional, spart Token):
  ```
  1. Baseline-Antwort erstellen.
  2. 2-5 konkrete Verifikationsfragen zu Einzelfakten formulieren.
  3. Jede UNABHAENGIG beantworten (ohne die Baseline als Kontext).
  4. Bei Widerspruch: revidieren. Sonst: Baseline behalten.
  ```
- **Self-Consistency** (mehrfach sampeln + Mehrheit) und **SelfCheckGPT** (mehrere Samples zur
  Halluzinations-Erkennung) — Prinzip belegt, **konkrete Reduktionsraten/Token-Kosten in den Quellen
  nicht quantifiziert** (`unsicher`).
- **Abstention als Sicherheits-Feature designen:** Wenn Bewertung unsichere Verweigerung bestraft,
  lernt das Modell zu raten → Halluzination. OpenAI 2025: ein Modell mit **52 % Abstention** macht
  drastisch weniger Fehler als eines mit 1 % Abstention + 75 % Fehlerrate.
- **Confidence ≠ Accuracy:** 51,4 % der hochkonfidenten Gemini-Antworten wurden von anderen Modellen
  widerlegt → der „klingt sicher"-Eindruck ist kein Wahrheitssignal.
- **Multi-Modell-Verifikation** bei Hochrisiko: Modelle widersprechen sich (72,1 % Disagreement in
  Finanzfragen) — ein zweites/staerkeres Modell als Pruefer deckt Erfundenes auf (siehe §6).

---

## 6. Stark halluzinierende / billige Modelle sicher nutzen (GLM-4.x Flash/Air & Co.)

Kern der Frage: ein Modell, das in vielem gut/guenstig ist, aber halluziniert — wie zaehmt man es?
`extern`, Researcher 5 (Z.ai-Docs) + Querschnitt:

**Belegte GLM-Fakten:**
- GLM-4.5 / GLM-4.5-Air / GLM-4.7-Flash sind **Hybrid-Reasoning-Modelle** (Thinking-Modus fuer
  komplexes Reasoning + Tool-Use, Non-Thinking fuer Schnellantworten). GLM-4.7: Interleaved/Preserved/
  Turn-level Thinking (pro Turn steuerbar).
- Default-Sampling-Config (SWE-bench): **temperature 0.6, top_p 1.0**.
- **Tool-Calling ist eine Staerke:** GLM-4.5 erreicht **90,6 % Tool-Calling-Erfolg** (ueber Claude-4-
  Sonnet 89,5 %). → das Modell ist gut darin, Tools korrekt aufzurufen — also **in Tool-Zwang stecken**.

> Konkrete GLM-Flash-Halluzinations-Reports (Reddit/GitHub) lieferten die Firecrawl-Quellen nicht
> (`unsicher`/offen — waere ein B-Eskalations-Fokus).

**Einsatz-Muster (aus dem Gesamtbild abgeleitet) — so wird ein halluzinationsanfaelliges Modell sicher:**
1. **Thinking/Reasoning AN.** Bei GLM explizit `reasoningEffort` setzen (sonst laeuft es ohne) —
   Reasoning senkt Fehler deutlich (vgl. `vibe-coding-modelle.md`).
2. **Enger Scope.** Eine Aufgabe pro Prompt, nicht „mach alles". Komplexe, konditionale Mega-Prompts
   verwirren schwache Modelle → in kleine Agenten/Schritte splitten.
3. **Tool-Zwang statt Freitext.** Es soll Read/Grep/Bash und strukturierte Tools nutzen (seine
   Staerke), nicht aus dem Gedaechtnis behaupten — die 5 Kern-Regeln (§1) in die AGENTS.md.
4. **Grounding liefern.** Relevante Dateien/Doku in den Kontext geben statt das Modell „wissen" zu
   lassen (§3) — verschiebt die Aufgabe von „erinnern" zu „ablesen".
5. **Niedrige Temperatur** fuer Fakten/Code (§4).
6. **Verifikation durch ein staerkeres Modell.** Das billige Modell generiert, ein staerkeres
   (oder eine CoVe-Runde, §5) prueft die heiklen Behauptungen — Generator/Verifier-Trennung.
7. **Code/Hook-Durchsetzung fuer das Unverzichtbare** (§7) — gerade schwache Modelle ignorieren
   Prompt-Regeln eher.

---

## 7. Die unbequeme Wahrheit: Prompt-Regeln werden ignoriert → Code/Hooks sind die Durchsetzung

`extern`, Researcher 2 (Jaroslawicz et al. 2025; Praxis-Artikel „200 Lines of Rules"; YouTube-Test
„Why Your AGENTS.md Rules Are Being Ignored"; GitHub-Issues):

- **Instruction-Compliance sinkt LINEAR mit der Anzahl:** *„double the instructions, halve the
  compliance."* Selbst die besten Modelle befolgen **< 30 %** der Instructions in Agent-Szenarien
  perfekt; Frontier-Thinking-Modelle „max out at ~150–200 instructions" (Jaroslawicz et al. 2025).
- **Real getestet:** Eine `AGENTS.md` mit **zwei** simplen Regeln → **4 von 5 Modellen ignorierten sie**,
  nur 1 befolgte. *„There's no enforcement mechanism … your carefully crafted agents.md is competing
  with the agent's training and default behavior."*
- GitHub-Issues bestaetigen es fuer CLAUDE.md (#15443, #6120, #18660: „read but not reliably followed").
- **Fazit der Praxis:** *„Rules in prompts are requests. Hooks in code are laws."*

**Konsequenzen (decken sich exakt mit Franks System):**
1. **Wenige, knappe, widerspruchsfreie Regeln** — Signal-zu-Rausch. Lange Regelwerke senken die
   Befolgung (siehe auch `bugs/claude-tooling/claude-config.md` §1.2 Context-Rot).
2. **Was IMMER greifen muss → Hook/Code**, nicht Prompt. Beispiele aus den Quellen:
   - **Wrapper-Allowlists:** jeder `git`/Tool-Aufruf laeuft durch einen Wrapper, der nur erlaubte
     Verben zulaesst und alles andere VOR der Ausfuehrung ablehnt („read-only by architecture").
   - **Validator-erzwungene Zitatpflicht:** ein Schema/Validator weigert sich, ein Finding ohne
     `knowledge_refs` zu bauen — *„If you can't cite, you can't claim"* auf Code-Ebene.
   - **Gesperrte System-Prompts + Safeguard-Modell** (Enterprise/Bedrock): Intent-Klassifikation
     SAFE/CAUTION/UNSAFE vor und nach dem Agenten.
   *„The architecture is what survives a prompt injection or a hallucinated tool call."*
3. **Regeln selbst testen** mit dem echten Modell — nicht annehmen, dass sie befolgt werden.

> **Fuer Frank konkret:** Die 5 Kern-Regeln (§1) gehoeren in die OpenCode-`AGENTS.md` (fuer GLM-Flash &
> billige Modelle) — UND das wirklich Kritische (z. B. „nicht ungeprueft committen", Pfad-Guards)
> bleibt bei den **Hooks**, die ohnehin Franks „Gesetzes"-Ebene sind. Genau diese Zweiteilung ist
> laut Forschung der wirksame Weg.

---

## 8. Halluzination messen (damit „besser" belegbar ist)

`extern`, Researcher 7. Kein Einzel-Benchmark erzaehlt die ganze Geschichte — **mindestens zwei
cross-referenzieren**:

| Benchmark | Misst |
|-----------|-------|
| **SimpleQA** | Short-form-Faktualitaet; behandelt Abstention als „first-class outcome" (correct/incorrect/not attempted) |
| **Vectara HHEM** | Grounding-Faithfulness bei Zusammenfassungen |
| **AA-Omniscience** (Acc/Hall/Index) | Wissensfragen + Halluzinationsrate + kombinierter Index |
| **FACTS Grounding** (Google DeepMind) | multidimensionale Faktualitaet |
| **HalluLens / HalluHard** | Halluzination ↔ Verweigerung als interagierende Verhaltensweisen |
| **TruthfulQA / HaluEval / FActScore** | klassische Halluzinations-Benchmarks (in den Quellen nur teils mit Zahlen) |

Praxis-Befunde: GPT-5 (main) 47 % Halluzination / 46 % Accuracy auf SimpleQA; Gemini 3 Pro 88 %
„Halluzination wenn unsicher" → 3.1 Pro auf 50 % gesenkt; Claude 4.1 Opus **0 % durch Abstention**.
Lehre: **Modellwahl domaenenspezifisch** (Summarisierungs-Treue ≠ Wissens-Reliability ≠ Zitat-Treue);
**„halluzinationsfrei"-Marketing nicht glauben** (Stanford: angeblich frei → empirisch 17–33 %).

---

## 9. Zusammenspiel & Querverweise

| Thema | Datei |
|-------|-------|
| Over-Editing, Thinking-Stufen, Modellwahl (GLM-5.2/MiMo/MiniMax) | `best-practices/opencode/vibe-coding-modelle.md` |
| AGENTS.md-Mechanik (Precedence, `/init`, globale vs. Projekt-Regeln) | `best-practices/opencode/agents-md-memory.md` |
| „CLAUDE.md/Regeln werden ignoriert" (Falle, advisory) + Context-Rot | `bugs/claude-tooling/claude-config.md` §1.1 / §1.2 |
| RAG/Grounding-Architektur, Memory-Backends | `best-practices/second-brain/*` |
| Verifier-Agent, typed schema, Tool-Calling-Disziplin | `best-practices/agents/orchestrator-agent.md` |

> **Kein eigenes Bug-Pendant:** Dies ist konzeptionelles, modell-/CLI-uebergreifendes Wissen (wie
> `second-brain/`), daher in `check-coupling.py` als `[INFO]` ungepaart — die eine konkrete *Falle*
> (Regeln werden ignoriert) ist in `bugs/claude-tooling/claude-config.md` §1.1 zurueckgekoppelt.

---

## Quellen (Auswahl, alle `extern`, Stand 2026-06-27)

- Menxhiqi & Marinova, „Hallucination Mitigation in LLM-Based Tool Recommendation" (Preprints.org 2026, 6.912 API-Calls) — Grounding/Schema-Ablation + C3-Anomalie.
- Béchard & Marquez Ayala, „Reducing hallucination in structured outputs via RAG" (arXiv 2404.08189, 2024).
- Vonage Developer Blog, „Eliminating Hallucinations in LLM-Driven Virtual Agents" (2026) — Reasoning-Felder 23,7 %→1,0 %.
- Zep, „How to Reduce LLM Hallucinations" (2026); Parasoft, „Controlling LLM Hallucinations at the Application Level" (2025).
- Dhuliawala et al., „Chain-of-Verification" (2023); Manakul et al., „SelfCheckGPT" (2023).
- Jaroslawicz et al. 2025 (Instruction-Compliance-Decay); GitHub-Gist „Anti-Hallucination System for AI Coding Agents"; GitHub-Issues #15443/#6120/#18660.
- IBM / Chip Huyen / machinelearningplus / Thoughtworks / arXiv Min-p — Sampling-Parameter.
- Z.ai Docs (GLM-4.5/4.7 Thinking-Modi, Sampling-Defaults, Tool-Calling-Rate).
- Researcher-7-Benchmarks: SimpleQA, Vectara HHEM, AA-Omniscience, FACTS, HalluLens; OpenAI „Why language models hallucinate" (2025); Stanford/Dahl et al. 2024 (Legal-RAG).
