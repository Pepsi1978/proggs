# KI-Agenten: Aufbau, Zusammenarbeit, Schwaerme, modelluebergreifender Betrieb

**Recherche vom 09.09.2026** · 21 Researcher parallel ueber drei Engines, je dieselben sieben
Unterthemen · Rohantworten in `~/.research-swarm-A|B/`, `~/.research-agents/C/`

Eingearbeitet in:
- `best-practices/agents/multi-agent-interop.md` (+ Kurzcheck) — **neu**
- `bugs/agents/multi-agent-interop.md` (+ Kurzcheck) — **neu**
- `best-practices/agents/orchestrator-agent.md` — Nachtrag Schwarmgroesse/Parallelitaet
- `bugs/agents/loop-engineering.md` — Nachtrag Endlosschleifen/Selbstkorrektur

---

## Teil 1 — Was ein KI-Agent ist

Anthropic, OpenAI und Google definieren es **strukturell gleich**: ein Modell, das **selbst**
entscheidet, welche Werkzeuge es einsetzt, und dabei in einer Schleife laeuft, bis ein Ziel oder eine
Abbruchbedingung erreicht ist.

Die Abgrenzung nach oben und unten:

| | Wer steuert den Ablauf | Handelt es? |
|---|---|---|
| **Chatbot** | der Nutzer, Zug um Zug | nein — "simple question-and-answer machines" |
| **Workflow** | fest verdrahteter Code | ja, aber auf vorgegebenem Pfad |
| **Agent** | das Modell selbst | ja, Weg und Werkzeugwahl frei |

Anthropic wortwoertlich: *"Workflows are systems where LLMs and tools are orchestrated through
predefined code paths. Agents, on the other hand, are systems where LLMs dynamically direct their own
processes and tool usage."*

**Die Schleife** heisst bei allen dreien dasselbe, nur anders benannt:

| Phase | Anthropic | OpenAI | Google |
|---|---|---|---|
| Kontext holen | gather context | Nutzer-Input + Tool-Ergebnisse | Question |
| Denken | (implizit, kontextgestuetzt) | Model-Reasoning | Thought |
| Handeln | take action | Tool-Call | Action (Extensions/Functions/Data Stores) |
| Beobachten | Ergebnis als "ground truth" | Tool-Ergebnis zurueck | Observation |
| Pruefen | verify work | Guardrails | (Teil der Orchestrierung) |
| Abbrechen | stopping conditions | finale Antwort / Termination-Tool / Timeout | Final Answer |

**Die vier Bausteine:** Modell (Google: "Brain"), Werkzeuge ("Hands"), Instruktionen/Gedaechtnis,
Abbruchbedingung. Anthropic nennt die Basis "augmented LLM" — ein Modell mit Retrieval, Tools und
Memory.

> Merksatz aus dem Claude-Agent-SDK-Umfeld: *"Tools are the primary building blocks of execution."*
> Eine Schleife ohne Werkzeuge ist nur ein Chatbot in einem `while`.

---

## Teil 2 — Einen einzelnen Agenten gut bauen

**Werkzeug-Design ist Prompt-Design.** Das ist der wichtigste Einzelbefund. Tool-Namen,
-Beschreibungen und Parameter sind Teil des Prompts, nicht "nur" API-Spezifikation. Anthropic hat
gemessen, dass kleine Verfeinerungen der Tool-Beschreibungen Claude Sonnet 3.5 auf SWE-bench-Verified-
Bestwert gebracht haben, mit dramatisch reduzierter Fehlerrate.

Konkret:
- **Namespacing** bei vielen Tools (`asana_search`, `jira_search`) — Praefix- gegen Suffix-Schema hat
  messbare Effekte.
- **Sprechende Parameter**: `user_id` statt `user`. Semantische IDs statt roher UUIDs verbessern die
  Trefferquote deutlich, weil sie Halluzinationen reduzieren.
- **Beschreibungen wie Onboarding-Doku**: *"Think of how you would describe your tool to a new hire."*
- **Fehlermeldungen als Lernsignal** — handlungsleitend statt opaker Codes. Die Fehlermeldung soll den
  Agenten zur besseren Strategie fuehren.
- **Test fuer ueberladene Toolsets**: *"If a human engineer can't definitively say which tool should be
  used in a given situation, an AI agent can't be expected to do better."*

**Kontext ist eine sich abnutzende Ressource ("Context Rot").** Je voller das Fenster, desto
schlechter der Abruf einzelner Fakten. Gegenmittel in aufsteigender Haerte: Tool-Ergebnisse loeschen
(leichtester Eingriff) → Kompaktierung mit Zusammenfassung → harter Context-Reset. Dazu
Just-in-Time-Retrieval: Referenzen im Kontext halten, Daten erst zur Laufzeit nachladen.

**Wiederaufnahme braucht externe Artefakte, nicht Kontext.** Anthropics Muster: `progress.txt` mit
protokollierten Aktionen, strukturierte Feature-Liste mit Status (200+ Eintraege, "passing"/"failing"),
beschreibende Git-Commits. Zweck ausdruecklich: der Agent soll beim Wiedereinstieg nicht raten muessen.
Jede neue Sitzung startet mit einem Funktionstest, um Bugs der Vorsitzung zu finden.

**Evaluation ist ein eigenes Handwerk.** 20-50 Aufgaben aus echten Fehlschlaegen reichen zum Start.
Kernprinzip: *"Grade what the agent produced, not the path it took."* Wichtige Metrik-Unterscheidung:
`pass@k` (einer von k reicht) gegen `pass^k` (alle k muessen) — bei 75 % Erfolg je Versuch liegt
`pass^3` nur bei ~42 %.

**Ein konkretes Kostenbeispiel** (Anthropic, Tetris-artige App bauen):

| Aufbau | Zeit | Kosten | Ergebnis |
|---|---|---|---|
| Solo-Agent ohne Harness | 20 Min | 9 $ | Kernfunktion kaputt |
| Voller Harness (Planner/Generator/Evaluator) | 6 Std | 200 $ | vollstaendig funktionierend |
| Vereinfachter Harness mit neuerem Modell | 3:50 Std | 124,70 $ | funktionierend |

Daraus die Leitlinie: *"every component in a harness encodes an assumption about what the model can't
do on its own"* — mit besseren Modellen Komponenten wieder **entfernen**.

---

## Teil 3 — Mehrere Agenten zusammen

### Die Topologien

| Muster | Aufbau | Staerke | Schwaeche |
|---|---|---|---|
| **Orchestrator-Worker** | zentraler Stern | klare Verantwortung, einfaches Debugging | Orchestrator als Flaschenhals |
| **Hierarchie** | mehrere Supervisor-Ebenen | skaliert durch Delegation | Verzoegerung, kritische Knoten |
| **Peer-to-Peer / Swarm** | direkte Handoffs, keine Zentrale | schneller, weniger LLM-Calls | schwer nachvollziehbar, Deadlocks |
| **Blackboard** | gemeinsamer Speicher, alle lesen/schreiben | toleriert Ausfaelle, Zwischenstaende bleiben | duenne Beleglage |
| **Pipeline** | sequenzielle Kette | gut fuer ETL-artige Aufgaben | keine Parallelitaet |
| **Generator-Verifier** | Bauer + separater Pruefer ohne dessen Kontext | bessere Kritik durch sauberen Kontext | doppelte Kosten |

Es gibt **keine anerkannte Standard-Taxonomie** — die Benennungen schwanken je Quelle.

### Der zentrale Streit, aufgeloest

Anthropic: Orchestrator-Worker schlaegt Single-Agent um **+90,2 %** auf internem Recherche-Eval,
kostet aber **~15x Tokens**. Cognition: *"Don't Build Multi-Agents"* — parallele Subagenten treffen
widerspruechliche Annahmen.

Beide haben recht, fuer verschiedene Aufgaben:

> **Lesen parallel, schreiben einspurig.**

Recherche ist read-heavy und zerlegbar → parallel lohnt sich. Coding ist write-heavy → parallele
Schreibzugriffe erzeugen Konflikte, die sich aufsummieren. Cognition hat die Position 2026 selbst
praezisiert zu *"writes stay single-threaded"* — mehrere Agenten liefern Intelligenz (Review,
Konsultation), aber nur einer schreibt.

**Die Zahlen, die man kennen sollte:**
- **MAST-Studie** (UC Berkeley, 1.600+ Traces, 7 Frameworks): Fehlerraten **41-86,7 %**, davon ~37 %
  reine Koordinationsfehler.
- Tokenverbrauch erklaert **80 % der Ergebnisvarianz** bei Anthropic — mehr Tokens ist der
  Wirkmechanismus, nicht die Architektur an sich.
- Modellwahl schlaegt Tokenbudget: ein Modell-Upgrade bringt mehr als eine Verdopplung des Budgets.
- Gartner: ueber 40 % der Agentic-AI-Projekte werden bis Ende 2027 abgebrochen.

### Schwarmgroesse

Umgekehrtes U: Leistung steigt, faellt ab einem Optimum wieder, Kosten steigen **quadratisch**.

| Aufgabentyp | sinnvoll |
|---|---|
| Reasoning-lastig | **2-6 Agenten** (bis −27 % Genauigkeit bei 8) |
| Breit zerlegbare Recherche | 3-5 pro Welle, >10 gesamt |
| Massiv-paralleler Fan-out | 100-300 moeglich, aber unverifizierte Quellenlage |

**Ueberraschung:** Anthropic faehrt **synchrone Wellen von 3-5**, kein kontinuierliches Nachziehen —
und nennt das selbst eine Limitation. Der Begriff "Continuous-Spawning" ist kein etablierter
Fachbegriff; die Literatur spricht von Wellen gegen asynchronen Fan-out.

**Claude Codes eigene Leitplanken:** 20 gleichzeitige Subagenten (`CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS`),
Verschachtelungstiefe 3 (`CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH`). Ein frueherer 200er-Deckel pro
Session wurde wieder entfernt.

---

## Teil 4 — Agenten im Loop

**Selbstkritik ohne externes Feedback funktioniert nicht.** Huang et al. (arXiv 2310.01798): *"LLMs
struggle to self-correct their responses without external feedback, and at times, their performance
even degrades after self-correction."*

Der Grund ist praezise: das Problem liegt im **Finden**, nicht im **Beheben**. Ein Modell kann einen
Fehler gut korrigieren, wenn man ihm sagt wo er ist — es findet ihn nur selbst nicht. Und wenn
Generator und Bewerter dasselbe Modell sind, teilen sie dieselben blinden Flecken.

**Konsequenz:** Bewertung strukturell vom Generator trennen. Deterministischer Test, externer
Kritiker, oder ein Klassifikator mit anderen Fehlermodi — nicht mehr Runden desselben Modells.

**Rundenzahlen:** Self-Refine plateaut bei 2-4 Runden (Sekundaerquelle, nicht primaerverifiziert).
Multi-Agent-Debate konvergiert in 4-8 Runden; ein adaptives Kriterium (KS-Test zwischen den
Antwortverteilungen) spart Rechenzeit bei praktisch null Genauigkeitsverlust.

**Endlosschleifen sind haeufig, nicht selten:** 68 bestaetigte Faelle in 47 Projekten aus 6.549
Repositories. Zwei Drittel entfallen auf LangGraph und AutoGen. Folgen: Kostenexzess und faktischer
DoS in je 95,6 % der Faelle.

> **Die Regel:** Das Abbruchkriterium gehoert **deterministisch in den Code**, nie ins Modell.
> Binaer und messbar ("Coverage > 80 %"), nie subjektiv ("gut genug"). Plus harte
> `max_iterations` als Netz — Praxis-Default 10-15.

Begruendung, warum Modelle schlechte Abbrecher sind: sie wollen hilfreich klingen, und "lass uns hier
aufhoeren" wirkt nicht wie eine hilfreiche Antwort.

**Debate laeuft in die Echo-Kammer:** eine fehlerhafte Mehrheitsmeinung aus geteilten
Fehlvorstellungen unterdrueckt korrekte Minderheitspositionen. Der Hebel ist **Diversitaet**
(verschiedene Modelle, verschiedene Blickwinkel), nicht mehr Runden.

---

## Teil 5 — "Habe ich die dann angestellt?" — modelluebergreifende Zusammenarbeit

Die kurze Antwort: **Du stellst niemanden an. Du legst einen gemeinsamen Arbeitsplatz an.**

Es gibt drei Achsen, und sie werden staendig verwechselt:

```
        MCP  (vertikal)                A2A  (horizontal)
    Agent ──→ Werkzeuge/Daten      Agent ←──→ Agent
    etabliert, Spec 2026-07-28     v1.0/1.0.1, im Uebergang
    AAIF: Anthropic+OpenAI+Block   150+ Orgs — ABER: Anthropic fehlt
```

**MCP** ist die reife Schiene, aber sie loest die falsche Frage: sie verbindet einen Agenten mit
seinen Werkzeugen, nicht mit einem anderen Agenten. Seit 09.12.2025 unter der Agentic AI Foundation
der Linux Foundation, mit Anthropic, OpenAI und Block als Gruendungsmitgliedern. 110+ Mio. monatliche
SDK-Downloads.

**A2A** ist die richtige Achse — aber **Anthropic taucht in der Traegerliste nicht auf**. Das ist der
zentrale Vorbehalt fuer jeden, der einen Claude-Agenten mit einem GPT-Agenten sprechen lassen will.

**OpenAI-Handoffs sind kein Ausweg**: SDK-intern, bleiben "within a single run", keine dokumentierte
Aussage zu Claude oder Gemini.

**"ACP" ist eine Falle:** zwei verschiedene Protokolle mit demselben Kuerzel (Zed = Agent↔Editor,
IBM/BeeAI = Agent↔Agent). Selbst ein arXiv-Paper laesst offen, welches es meint.

**Und ueber allen:** Ein Governance-Paper (arXiv 2606.31498) findet, dass **Abstimmung und
Dissens-Erhaltung bei allen fuenf untersuchten Protokollen vollstaendig fehlen**. Agenten koennen sich
Aufgaben zuwerfen — verhandeln koennen sie nicht.

### Der Weg, der 2026 wirklich traegt

Kein Protokoll, sondern ein **gemeinsames Dateisystem mit Regeln**:

1. **Atomic Claim** — `os.open(pfad, O_CREAT | O_EXCL | O_WRONLY)`. Nur ein Prozess kann die Datei
   exklusiv anlegen. Kein Lock-Gebastel, Exactly-once garantiert.
2. **Output-Existenzcheck** vor der Arbeit — macht jeden Schritt idempotent.
3. **Ledger nur als Nachweis**, nie als alleinige Koordination. Die AWS-Autoren warnen ausdruecklich,
   dass ihr eigenes Ledger-Muster ohne atomare Claims bei Parallelitaet bricht.
4. **Verzeichnisstruktur als Vertrag** — Inbox/Outbox oder Phasen-Ordner, flach und offensichtlich.

Fuer heterogene CLI-Agenten ist das dokumentierte Muster der **Headless-Subprocess**: `claude -p`,
`codex exec`, jeweils mit `--output-format json`. Es gibt **keinen offiziellen Anthropic-Adapter**
fuer fremde CLI-Agenten. Das konkreteste Vorbild ist der Community-Orchestrator **NEEDLE**: SQLite-
Queue mit atomaren Claims, verteilt an Claude Code, Codex, OpenCode und Aider — und **bewusst ohne
Laufzeit-Kommunikationskanal zwischen den Agenten**. Die Koordination passiert vorher, bei der
Zerlegung.

`claude mcp serve` geht auch andersherum: es exponiert Claude Codes Werkzeuge ueber MCP, sodass ein
anderer MCP-Client Claude Code beauftragen kann.

### Was das kostet

- Drei-Agenten-Pipeline ≈ **29.000 Tokens** gegen ~10.000 beim Ein-Agenten-Aequivalent.
- **Tool-Schema-Overhead** macht 60-80 % des Verbrauchs aus, wenn statische Toolsets immer
  mitgeschickt werden.
- Ein Workflow zu 0,50 $ im Test kann bei 100.000 Laeufen/Monat auf 50.000 $/Monat gehen — der
  Orchestrator macht eigene LLM-Calls **zusaetzlich** zu jedem Worker-Call.

### Was das an Sicherheit kostet

- **MCP Tool Poisoning**: ~5,5 % von 1.899 untersuchten Servern; Command Injection bei 43 %;
  SSRF bei 36,7 % von 7.000+ Servern. **Kein universeller Fix** — nur striktes Whitelisting.
- **A2A**: keine erzwungenen Token-Ablaufzeiten in der Spezifikation, Agent-Impersonation,
  Context Poisoning ueber manipulierte Agent Cards.

---

## Teil 6 — Der Modellvergleich aus diesem Lauf

Alle drei Engines bekamen **wortgleich dieselben sieben Unterthemen**.

| | **A** Firecrawl+Tavily→DeepSeek | **B** DeepSeek `:online` | **C** Sonnet-5-Schwarm |
|---|---|---|---|
| Zeichen je Antwort | 10.823 | 10.564 | **19.145** |
| URLs je Antwort | **0** | 39,0 | 36,9 |
| verschiedene Domains | — | **15,1** | 11,9 |
| Unsicherheits-Marker | 3,1 | 3,3 | **11,3** |
| Primaerquellen-Anteil | 5 % (geholt) | 11 % (zitiert) | **44 %** (zitiert) |
| Zeit je Researcher | 400 s+ (2 Ausfaelle) | ~90 s | ~3-4 Min |
| Kosten | ~0,3 ct | ~1,5 ct | Claude-Token |

**Drei harte Befunde:**

1. **Engine A nannte in keiner Antwort eine URL.** Sie zitierte `(Quelle 1: Openlayer)` — einen
   Verweis in die nummerierte Liste, die nur im Prompt stand. Ohne die Quellendatei war die Antwort
   nicht ueberpruefbar. Ursache war der Auswerte-Prompt, nicht das Modell. **Behoben** am 09.09.2026.

2. **B und C teilen nur 8 von 141 Domains.** Fast keine Ueberschneidung. Unter den Domains, die nur B
   fand, war `context.reverso.net` — ein Uebersetzungswoerterbuch — in vier von sieben Unterthemen.
   Das ist genau der Retrieval-Bias, den Anthropic im eigenen System gefunden hat: Subagenten
   bevorzugen SEO-optimierte Seiten vor akademischen PDFs, und automatisierte Evals sehen das nicht.

3. **C ist am ehrlichsten** — dreimal so viele Stellen mit "das konnte ich nicht belegen". Beim
   MCP/A2A-Thema hatte A **null** Unsicherheitsmarker; bei einem Feld, das sich vierteljaehrlich
   aendert, ist das ein Warnsignal, kein Guetesiegel.

**Der strukturelle Grund:** A und B setzen **eine** Suchanfrage ab und verarbeiten das Ergebnis. C
sucht mehrfach, liest an, erkennt "diese Seite zitiert das Original" und holt das Original.

**Einschraenkung zur Methode:** "Primaerquelle" ist eine Stichwortliste (anthropic.com, openai.com,
arxiv.org, offizielle Docs, Spezifikationen). Ein anderer Zuschnitt verschiebt die Prozentzahlen; die
Rangfolge duerfte er nicht umdrehen.

**Zwei Betriebsbefunde aus dem Lauf selbst:**
- Der Zeitdeckel von 400 s je Researcher war fuer A mit `MM_TAVILY=always` zu knapp — zwei von sieben
  liefen leer hinein. Der Flaschenhals ist die **Auswertung** (150.000+ Zeichen), nicht die Suche
  (6 Sekunden). Deckel ist jetzt ueber `RESEARCH_SWARM_TIMEOUT` konfigurierbar.
- Das Ausgabeverzeichnis war fest verdrahtet; zwei gleichzeitige Schwaerme haetten sich die
  Ergebnisse gegenseitig weggeraeumt. Jetzt ueber `RESEARCH_SWARM_OUT` je Lauf trennbar.

---

## Teil 7 — Was davon auf dieses Repo passt

**Was hier schon richtig laeuft:**
- Die Researcher lesen parallel, das Einarbeiten in Almanache macht ausschliesslich der Hauptagent —
  das ist exakt "writes single-threaded, reads parallel", bevor der Begriff hier bekannt war.
- Rohdaten laufen nie durch den teuren Hauptkontext, nur die ~2k-Synthese. Das entspricht Anthropics
  Subagent-Muster (1.000-2.000 Token Rueckgabe).
- Der Auto-Cleanup in `research-swarm.py` adressiert genau den Fremd-Themen-Leak, der in der
  Literatur als Aggregationsproblem beschrieben ist.
- `MEMORY.md`, die Almanache und die Ledger-Dateien sind das, was Anthropic als externe
  Fortschritts-Artefakte fuer die Wiederaufnahme empfiehlt.

**Wo die Recherche dem Setup widerspricht:**
- **7 parallele Researcher** gegen "2-6 bei reasoning-lastigen Aufgaben". Bei sieben unabhaengigen
  Recherchethemen ist 7 vertretbar (breadth-first, Anthropic empfiehlt dort >10). Bei Coding- oder
  Analyseaufgaben waere es zu viel.
- **"Continuous-Spawning ist die OBERSTE Regel"** — Anthropic selbst faehrt Wellen. Continuous ist
  zeitlich besser, aber die Formulierung als oberste Regel ueberzeichnet den Beleg. Der Begriff
  existiert in der Literatur nicht.
- **Keine Effort-Skalierung im Skill.** Anthropic schreibt Regeln wie "1 Agent fuer einfache Fakten,
  2-4 fuer Vergleiche, >10 nur bei komplexer Recherche" direkt in den Lead-Prompt. Der `research`-Skill
  hat das nicht — jede Recherche bekommt dieselbe Breite.

**Was hier fehlt und sich lohnen wuerde:**
- **Kein deterministisches Iterationslimit** in den Loop-Skills. Die 68-Endlosschleifen-Studie ist ein
  starkes Argument, das im Code zu verankern statt im Prompt.
- **Kein Generator-Verifier-Paar.** Der `auto-verify-iterate`-Skill prueft mit demselben Modell, das
  gebaut hat — laut Huang et al. der schwaechste Fall. Ein separater Kritiker mit sauberem Kontext
  waere ein echter Hebel.
- **`isolation: worktree` wird nicht genutzt**, obwohl es fuer Parallelarbeit in einem Monorepo der
  native Mechanismus ist.
- **Kein Eval-Set fuer die eigenen Skills.** 20-50 Faelle aus echten Fehlschlaegen waeren der Einstieg;
  die Bug-Almanache sind faktisch schon die Rohdaten dafuer.
