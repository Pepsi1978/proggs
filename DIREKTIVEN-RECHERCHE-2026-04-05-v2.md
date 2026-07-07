# Direktiven-Recherche v2 — Neue Erkenntnisse seit v1

**Datum:** 2026-04-05 (Abend-Session)
**Methode:** 5 parallele Researcher, ~20 Web-Searches/Fetches
**Basis:** DIREKTIVEN-RECHERCHE-2026-04-05.md (v1) — alle dort beschriebenen Punkte gelten als bekannt und werden NICHT wiederholt

---

## Zusammenfassung: Top-3 Sofortmassnahmen

| # | Massnahme | Warum jetzt? | Aufwand |
|---|----------|-------------|---------|
| **1** | **Prozedurale Memory**: Workflows als "Skill Library" persistieren | Mem0 v1.0.0 hat diesen Typ neu eingeführt — direkt umsetzbar in experience-store.jsonl | 1-2h |
| **2** | **ConfigChange-Hook** als Poka-Yoke Stufe 3 nutzen | In v1 empfohlen aber falsch kategorisiert: ConfigChange kann Aenderungen BLOCKIEREN (exit 2) — staerker als PostToolUse | 1-2h |
| **3** | **Entropy Management**: Automatischer Cleanup-Agent nach je 20 Sessions | MorphLLM, Anthropic und Martin Fowler stimmen ueberein: Codebase-Entropy ist der groesste unsichtbare Killer | 2-4h |

---

## Was bereits implementiert ist (Abgleich mit v1)

Diese Punkte aus v1 sind UMGESETZT — nicht nochmal vorschlagen:

| v1-Punkt | Status |
|----------|--------|
| Circuit Breaker (retry-state.json) | Implementiert laut MEMORY.md |
| Bug-Cases Auto-Writer (PostToolUseFailure) | Aktiv — sichtbar in bug-cases.jsonl |
| False Evidence Table | Noch nicht in CLAUDE.md — offener Punkt |
| ACE Generator-Reflector-Curator (konzeptionell) | In Rules beschrieben, kein Hook |
| ClaudeWatch als Selbstbeobachtungs-Tool | Aktiv installiert |
| Session-Score automatisieren | Hook existiert (session-scorer) |
| MemGPT Memory-Hierarchie | Konzeptionell in Direktiven |
| Bugfix-FSM | Konzeptionell in Direktiven |

---

## Teil 1: DIREKTIVE #1 — Superintelligenz (Neue Erkenntnisse)

### 1.1 Harness ist der entscheidende Wettbewerbsvorteil (NEU — 2026-Konsens)

**Quellen:** Aakash Gupta (Medium), philschmid.de, morphllm.com, Martin Fowler

**Was 2026 neu ist:** "Das Modell ist Commodity. Der Harness ist der Moat." Das bedeutet: Claude Sonnet oder Claude Opus werden irgendwann zur Massenware. Was bleibt, ist die INFRASTRUKTUR drumherum — Regeln, Hooks, Skills, Memory, Guards. Das ist der Compound Intelligence Effect in der Praxis.

**Drei Firmen-Beweise:**
- Manus: 5 Harness-Ueberarbeitungen mit GLEICHEN Modellen → bessere Zuverlaessigkeit
- LangChain: 4 Architektur-Iterationen fuer Deep Research
- Vercel: 80% Tool-Reduzierung → hoehere Erfolgsquote (weniger ist mehr)

**Was das fuer das System bedeutet:** Die Direktiven, Hooks und Skills sind nicht Zubehör — sie SIND der Wert. Jede Verbesserung am Harness ist wichtiger als jeder Prompt.

**Umsetzungsidee (Sofort):** CLAUDE.md bekommt einen neuen Abschnitt "Harness-Version" mit einem Datum — damit ist sichtbar dass der Harness sich weiterentwickelt.

---

### 1.2 IMPACT Framework — Lueckendiagnose (NEU)

**Quelle:** swyx / morphllm.com, IMPACT = Intent, Memory, Planning, Authority, Control Flow, Tools

**Was fehlt im aktuellen Harness:**

| IMPACT-Komponente | Aktueller Stand | Luecke |
|------------------|----------------|--------|
| **Intent** | CLAUDE.md beschreibt Ziele | Keine Eval-Validierung ob Ziele erreicht wurden |
| **Memory** | MEMORY.md + bug-cases.jsonl | Keine prozedurale Memory (Workflows, Ablaufroutinen) |
| **Planning** | Tool-Plan in metacognitive-process.md | Plaene sind nicht editierbar durch Benutzer waehrend Ausfuehrung |
| **Authority** | bypassPermissions + Guards | Gut implementiert |
| **Control Flow** | Agents + Parallel-Muster | Gut implementiert |
| **Tools** | Vielfaeltig vorhanden | Kontext-Pollution durch zu viele Tools moeglich |

**Groesste Luecke: Prozedurale Memory** — Das System weiss WAS es getan hat (bug-cases, trajectories), aber nicht WIE man Aufgaben effizient angeht (Workflows, Ablaufroutinen).

**Umsetzungsidee (Mittelfristig):** experience-store.jsonl um Feld `workflow_pattern` erweitern: Wenn eine Aufgabe gut gelaufen ist, den exakten Ablauf (Tool-Reihenfolge) als Muster speichern. Bei aehnlicher naechster Aufgabe: dieses Muster als Startpunkt nehmen.

---

### 1.3 Prozedurale Memory — Groesste Luecke (NEU — ICML 2025)

**Quellen:** Mem0 v1.0.0 (ECAI 2025), arXiv 2510.04851, mem0.ai/blog/state-of-ai-agent-memory-2026

**Was es ist:** Prozedurale Memory speichert "wie man etwas tut" — nicht Fakten, sondern Workflows, Tool-Reihenfolgen, Ablaufroutinen. Mem0 hat in v1.0.0 diesen Memory-Typ erstmals formalisiert. Ein Agent der prozedurale Memory hat, "modifiziert seine eigenen System-Prompt-Instruktionen basierend auf angesammeltem Nutzerfeedback."

**Warum das fuer Direktive #1 entscheidend ist:** Die 8 Intelligenz-Dimensionen erfordern nicht nur WISSEN was man tut, sondern WIE man es effizient tut. Prozedurale Memory ist die Bruecke zwischen "hat es gemacht" und "macht es beim naechsten Mal sofort richtig."

**Drei Memory-Typen (State of the Art 2026):**
- Episodische Memory: Was ist passiert? → MEMORY.md (vorhanden)
- Semantische Memory: Was weiss ich? → bug-cases.jsonl (vorhanden)
- **Prozedurale Memory**: Wie mache ich es? → FEHLT (Luecke)

**Umsetzungsidee (Sofort, 1-2h):**
Neues Feld in experience-store.jsonl:
```json
{
  "task_type": "android_bug_fix",
  "workflow_steps": ["Grep symptom", "Read file", "Hypothese formulieren", "Edit fix", "Build verify"],
  "success_score": 0.9,
  "avg_tool_calls": 7,
  "reuse_count": 0
}
```
Vor komplexen Aufgaben: Passendes Workflow-Pattern suchen und als Startpunkt nutzen. `reuse_count` steigt bei jedem Einsatz → beliebteste Patterns werden bevorzugt (Pheromon-Prinzip).

---

### 1.4 Hyperagents (Meta AI) — Meta-Level-Modifikation (NEU — arXiv 2603.19461, ICLR 2026)

**Quelle:** arxiv.org/abs/2603.19461

**Was es ist:** Hyperagents sind "selbstreferenzielle Systeme die Task-Agent und Meta-Agent vereinen". Das Besondere: "Die Meta-Level-Modifikation ist selbst editierbar" — nicht nur bessere Loesungen, sondern staendige Verbesserung des VERBESSERUNGSPROZESSES. Hyperagents haben autonom Werkzeuge wie persistente Memory und Performance-Tracking entwickelt — ohne menschliche Anweisungen.

**Umsetzungsideen fuer Claude Code CLI:**

1. **Hook-basiertes Skill-Tracking:** Hooks erfassen Prompt→Output-Paare und analysieren welche Prompt-Strukturen die besten Ergebnisse liefern → Skill-Aufrufe werden automatisch angepasst
2. **Performance-Monitoring-Layer:** Wrapper-Hook misst Qualitaet pro Skill-Aufruf und triggert Skill-Refactoring bei Schwellwert-Unterschreitung
3. **Cross-Domain-Muster-Transfer:** Optimierungen aus einer Sprache (z.B. Android) werden auf aehnliche Aufgaben (iOS) uebertragen und in Config persistiert

---

### 1.5 Meta Context Engineering (MCE) — Co-Evolution von Skills und Kontext (NEU — arXiv 2601.21557)

**Quelle:** arxiv.org/abs/2601.21557

**Was es ist:** MCE ist ein Bi-Level-Framework: Meta-Agent verfeinert Engineering-Skills via "agentic crossover" (lernt aus Skill-Historie + Evaluationen). Base-Agent fuehrt Skills aus und optimiert Kontext. Ergebnis: "5.6-53.8% relative Verbesserung" gegenueber statischen CE-Methoden.

**Kernaussage fuer das System:** Die aktuelle Struktur (Rules → immer gleich geladen) ist statisch. MCE schlaegt vor: Rules evolvieren basierend auf was funktioniert hat. Schlechte Rules werden downgraded, gute upvoted.

**Umsetzungsidee (Langfristig):** ACE-Curator (bereits in Planung aus v1) kombiniert mit MCE: Der Curator haelt nicht nur `helpful_count`/`harmful_count` pro Bullet, sondern auch `co_activation` — welche Bullets zusammen zu Erfolg gefuehrt haben. Zusammen aktivierte erfolgreiche Bullets werden als "Skill-Cluster" gespeichert.

---

### 1.6 Huxley-Goedel Machine (HGM) — Menschliches Niveau (NEU — arXiv 2510.21614, ICLR 2026 Oral)

**Quelle:** arxiv.org/abs/2510.21614

**Was es ist:** HGM erhielt eine Oral Presentation bei ICLR 2026. Es loest das "Metaproductivity-Performance Mismatch" Problem: Ein Agent der auf einem Benchmark gut ist, muss kein gutes Potential fuer Selbstverbesserung haben. HGM misst CMP (Clade-Metaproductivity) — die aggregierte Performance ALLER Nachkommen eines Self-Modification-Schritts. Ergebnis: Menschliches Niveau auf SWE-bench.

**Kernbotschaft fuer Direktive #1:** Die Metrik fuer Selbstverbesserung darf NICHT aktueller Performance-Score sein, sondern zukuenftige Verbesserbarkeit. Das aendert wie wir den Compound Intelligence Effect messen: Nicht "wie gut ist das System heute?" sondern "wie gut wird es in 5 Sessions sein?"

**Umsetzungsidee (Langfristig):** session-scores.jsonl um Feld `improvement_potential` erweitern: Nach jeder Session einschaetzen ob die gemachten Aenderungen die NAECHSTE Session besser machen werden (nicht nur ob diese Session gut war). Trend ueber 10 Sessions = echter Compound-Intelligence-Beweis.

---

### 1.7 Agentic AI und Intelligence Explosion — Societies of Thought (NEU — arXiv 2603.20639)

**Quelle:** arxiv.org/abs/2603.20639 (Evans, Bratton, Agüera y Arcas)

**Was es ist:** Das Paper argumentiert dass die naechste Intelligence Explosion nicht als einzelner Superintelligenz-Geist entsteht, sondern als "komplexe, kombinatorische Gesellschaft die sich spezialisiert wie eine Stadt". Intelligenz ist plural, sozial, relational.

**Kernmechanismus: "Societies of Thought":** Moderne Reasoning-Modelle funktionieren durch "spontane kognitive Debatten die argumentieren, verifizieren und abgleichen". Nicht ein Gedanke — ein Streit zwischen mehreren.

**Umsetzungsidee (Mittelfristig):** Parallele Agents nicht nur zur Beschleunigung, sondern zur VERIFIKATION: Bei wichtigen Entscheidungen zwei unabhaengige Agents die GEGENSAETZLICHE Loesungsansaetze erarbeiten → ein dritter Agent entscheidet. Das ist "Society of Thought" im Mini-Format und erhoeht Qualitaet ohne Menschenbeteiligung.

---

## Teil 2: DIREKTIVE #2 — Selbstbeobachtung (Neue Erkenntnisse)

### 2.1 Intrinsic vs. Extrinsic Metacognition (NEU — arXiv 2506.05109, ICML 2025)

**Quelle:** arxiv.org/abs/2506.05109

**Was es ist:** Das aktuelle System nutzt EXTRINSISCHE Metacognition — menschlich-designte feste Schleifen (3 Kernsensoren, Session-Rueckblick, etc.). Das Paper argumentiert: Echte Selbstverbesserung braucht INTRINSISCHE Metacognition — der Agent entwickelt selbst Bewertungs- und Reflexionsprozesse.

**Die drei Komponenten:**
1. Metacognitive Knowledge: Was weiss ich ueber meine eigenen Faehigkeiten und Grenzen?
2. Metacognitive Planning: Wie entscheide ich was und wie ich lerne?
3. Metacognitive Evaluation: Wie reflektiere ich Lernerfahrungen um kuenftig besser zu lernen?

**Warum das relevant ist:** Die Direktive #2 definiert 3 Kernsensoren (Retry-Zaehler, Korrektur-Zaehler, Drift-Detektor). Das sind extrinsische Mechanismen. Ein intrinsischer Mechanismus waere: Der Agent entscheidet selbst WELCHE Metriken er trackt — und aendert diese Metriken wenn sie nicht nuetzlich sind.

**Umsetzungsidee (Langfristig):** Am Session-Ende nicht nur Vorschlaege ausgeben, sondern auch evaluieren: "Haben die 3 Kernsensoren diese Session geholfen? Wurde Drift erkannt bevor es zu spaet war? Habe ich wirklich etwas Neues gelernt?" Diese Meta-Evaluation in session-scores.jsonl aufnehmen.

---

### 2.2 Telemetrie als Selbstbeobachtungs-Infrastruktur (NEU — Arize.com)

**Quelle:** arize.com/blog/closing-the-loop-coding-agents-telemetry

**Was es ist:** "In Software dokumentiert der Code die App; in KI dokumentieren die Traces die App." Traces erfassen wie viele Schleifen ein Agent durchlaeuft, welche Tools er aufruft und wo Fehler entstehen. Ohne strukturierte Traces "bricht der Verbesserungskreislauf an seinem kritischsten Punkt ab: der Verifikation."

**Feedback-Loop:**
1. Instrumentierung → 2. Ausfuehrung → 3. Analyse der Traces → 4. Evaluierung → 5. Iteration

**Direkter Bezug zu Direktive #2:** Die trajectories.jsonl IST bereits eine rudimentaere Trace-Sammlung. Was fehlt: Automatische Auswertung der Trajectories auf Muster (viele Retries, viele Reads ohne Writes = Drift, etc.).

**Umsetzungsidee (Mittelfristig):** PostToolUse-Hook schreibt kompakte Trace-Eintraege in trajectories.jsonl. Am Session-Ende: Python-Skript analysiert den Trace und berechnet automatisch Retry-Quote, Read/Write-Ratio, Tool-Erfolgsrate. Das macht Direktive #2 messbar statt subjektiv.

---

### 2.3 LLM-optimierte Feedback-Signale — "Positive Prompt Injection" (NEU)

**Quelle:** Martin Fowler — harness-engineering.html

**Was es ist:** "Custom Linter-Meldungen schreiben die Selbstkorrektur-Anweisungen enthalten." Anstatt eines generischen "Error: import not found" gibt der Linter aus: "Import fehlt. Fuer Android-Projekte: `import androidx.compose.runtime.*` in den Imports einfuegen." Das ist eine Linter-Meldung die gleichzeitig die Loesung liefert — "positive prompt injection".

**Warum das fuer Direktive #2 relevant ist:** Hooks koennen nicht nur Fehler melden, sondern auch die Loesung als Kontext mitliefern. Das ist was der bug-case-auto-writer bereits rudimentaer macht, aber viel systematischer angewendet werden kann.

**Umsetzungsidee (Sofort, 1-2h):** Alle bestehenden Hooks ueberpruefen: Geben sie generische Fehlermeldungen oder loesungsorientierte Meldungen? Template fuer Hook-Ausgaben: "FEHLER: [Was]. URSACHE: [Warum]. FIX: [Genau wie]." Das kostet 20% mehr Schreibaufwand beim Hook, spart 80% Debugging-Zeit.

---

### 2.4 Entropy Management — Der unsichtbare Feind (NEU — 2026-Konsens)

**Quellen:** MorphLLM, Martin Fowler, Anthropic, philschmid.de

**Was es ist:** "Ueber Zeit akkumulieren KI-generierte Codebases Entropy — Dokumentation driftet von der Realitaet weg, Namenskonventionen divergieren, toter Code sammelt sich." Context Pollution ist "das Vorhandensein zu vieler irrelevanter, redundanter oder konfligierender Informationen im Kontext die das LLM ablenken und seine Genauigkeit verschlechtern."

**Was das fuer das System bedeutet:** CLAUDE.md, MEMORY.md, bug-cases.jsonl und experience-store.jsonl akkumulieren Entropy mit der Zeit. Veraltete Eintraege, widersprueechliche Regeln, duplizierte Patterns — alle senken die Qualitaet.

**Konkrete Cleanup-Typen (von MorphLLM):**
- Dokumentations-Konsistenz-Agent: Prueft ob Rules noch zur Realitaet passen
- Constraint-Verletzungs-Scanner: Findet widersprueechliche Regeln
- Pattern-Enforcement-Agent: Erkennt divergierende Konventionen
- Dependency-Auditor: Prueft ob alle Referenzen noch gueltig sind

**Umsetzungsidee (Mittefristig):** Neuer Cron-Job "entropy-cleanup" laeuft alle 20 Sessions (via `claude schedule`):
- Liest bug-cases.jsonl und entfernt Duplikate (Jaccard > 0.8) → bereits in v1 vorgeschlagen aber noch nicht umgesetzt
- Liest MEMORY.md und markiert Eintraege aelter als 90 Tage als "zu verifizieren"
- Liest ~/.claude/rules/ und prueft ob alle Beispiele noch zur aktuellen CLAUDE.md-Realitaet passen
- Report: "3 redundante Bug-Cases entfernt, 2 Memory-Eintraege veraltet, 1 Regel widersprueechlich"

---

## Teil 3: DIREKTIVE #3 — Resilient Bugfixing (Neue Erkenntnisse)

### 3.1 Guides + Sensors — Neues Denkmodell (NEU — Martin Fowler)

**Quelle:** martinfowler.com/articles/harness-engineering.html

**Was es ist:** Harness-Regulation besteht aus zwei Komponenten:
- **Guides (Feedforward):** Antizipative Kontrollen die Verhalten VOR der Aktion steuern (z.B. Regeln, Guards, Poka-Yoke)
- **Sensors (Feedback):** Beobachtungsmechanismen die NACH der Aktion zur Selbstkorrektur fuehren (z.B. Linter, Test-Runner, Build-Check)

**Zwei Sensor-Typen:**
- Computational (deterministisch, schnell): Tests, Linter, Type-Checker → IMMER zuerst
- Inferential (semantisch, langsamer): LLM-basierter Code Review → nur bei Bedarf

**Bezug zu Direktive #3:** Das bestehende System hat gute Guides (Regeln, Hooks) aber schwache systematische Sensors. Die "Fix-Induced-Failure-Pruefung" (8 Punkte) ist manuell — ein automatischer Sensor wuerde das nach jedem Fix ausfuehren.

**Umsetzungsidee (Sofort, 1-2h):** Stop-Hook ("computational sensor") prueft nach JEDEM abgeschlossenen Feature automatisch:
1. Existiert ein Test der das geaenderte Verhalten abdeckt? (Grep auf Dateinamen)
2. Gibt es ein .sh-Gegenstueck zur .ps1-Datei? (Cross-Platform-Sensor)
3. Wurde bug-cases.jsonl aktualisiert? (Memory-Sensor)
Falls NEIN: Hook gibt Warnung aus (kein Blocker, nur Erinnerung).

---

### 3.2 ConfigChange-Hook als Poka-Yoke Stufe 3 (KORREKTUR von v1)

**Quelle:** code.claude.com/docs/en/hooks (offizielle Doku, Maerz 2026)

**Was v1 falsch hatte:** v1 empfahl ConfigChange als mittelfristigen Punkt. Das war eine Fehlkategorisierung.

**Was die offizielle Doku sagt:** "ConfigChange Hooks can **block** configuration changes from taking effect. Use **exit code 2** or a JSON decision to prevent the change. When blocked, the new settings are **not applied** to the running session."

**Das ist Poka-Yoke Stufe 3 (Eliminierung)** — nicht nur warnen, sondern VERHINDERN. Damit kann bypassPermissions-Reset hardwaremaeig unmoeglich gemacht werden — ohne den PostToolUse-Hook der nur reagiert nachdem die Aenderung schon drin ist.

**Umsetzungsidee (Sofort, 30min):** config-guard.ps1 auf ConfigChange-Event migrieren statt PostToolUse. Das ist staerker, weil die Aenderung nie appliziert wird statt reaktiv zurueckgesetzt zu werden. Poka-Yoke Stufe 3 statt Stufe 2.

---

### 3.3 Trajectory-Logging als Trainings-Daten (NEU — 2026-Konsens)

**Quellen:** philschmid.de, Arize, LangChain, Cursor-Erfahrung

**Was es ist:** "Cursor trained their Composer model on tool-use trajectories: sequences of actions showing the model how and when to use tools." Und: "Today's agent products like Claude Code are post-trained with models and harnesses in the loop, helping models improve at actions that the harness designers think they should be natively good at."

**Was das bedeutet:** Die trajectories.jsonl ist nicht nur ein Log — es sind potenzielle Trainings-Daten fuer zukuenftige Claude-Versionen. JEDE erfolgreiche Sequenz ("Grep → Read → Hypothese → Edit → Build OK") ist ein Wert.

**Umsetzungsidee (Mittelfristig):** trajectories.jsonl um `training_value`-Score erweitern:
- 1.0: Direkter Erfolg beim ersten Versuch, keine Korrekturen vom Benutzer
- 0.5: Erfolg nach einem Retry
- 0.0: Fehlgeschlagen oder Benutzer hat abgebrochen
Export-Funktion: Alle Eintraege mit score >= 0.8 in `training-candidates.jsonl` kopieren. Das ist der erste Schritt zu echtem Compound-Intelligence durch bessere Modelle.

---

### 3.4 InstructionsLoaded-Hook — Neue Moeglichkeit (NEU — Offizielle Doku Maerz 2026)

**Quelle:** code.claude.com/docs/en/hooks, pixelmojo.io

**Was es ist:** Seit Maerz 2026 gibt es 21 Hook-Events (vorher 14). InstructionsLoaded feuert wenn eine CLAUDE.md oder .claude/rules/*.md Datei in den Kontext geladen wird. Laeuft asynchron (kein Blockieren).

**Was damit moeglich ist:**
- Wenn eine Rule-Datei geladen wird: Automatisch zugehoerige bug-cases und experience-entries als Kontext-Hinweis ausgeben
- "Du laderaet gerade resilient-bugfixing.md. Relevante Bug-Cases: [X, Y, Z]"
- Das macht die Direktiven nicht statisch sondern dynamisch-kontextuell

**Umsetzungsidee (Mittelfristig):** InstructionsLoaded-Hook prueft welche Rule-Datei geladen wird und gibt die letzten 3 relevanten bug-cases als Kontext-Hinweis aus. Macht Direktive #3 beim Laden sofort handlungsrelevant.

---

### 3.5 FileChanged-Hook — Cross-Platform-Pruefung automatisch (NEU — Offizielle Doku)

**Quelle:** code.claude.com/docs/en/hooks

**Was es ist:** FileChanged-Hook feuert wenn eine ueberwachte Datei auf der Festplatte aendert. Das matcher-Feld kontrolliert welche Dateinamen ueberwacht werden (pipe-separiert). FileChanged Hooks haben Zugriff auf CLAUDE_ENV_FILE — Variablen die dort geschrieben werden persistieren in nachfolgenden Bash-Befehlen.

**Umsetzungsidee (Sofort, 30min):** FileChanged-Hook ueberwacht `*.ps1`-Dateien. Wenn eine .ps1 geaendert wird: Pruefen ob das .sh-Gegenstueck auch existiert und aktuell ist. Falls nicht: Warnung ausgeben "Cross-Platform-Pruefung: .sh-Datei fuer [X.ps1] fehlt oder ist aelter." Das automatisiert die manuelle Cross-Platform-Checkliste aus CLAUDE.md.

---

## Teil 4: Agent Harness Engineering — 2026-Erkenntnisse

### 4.1 Das Computer-Analogie-Modell (NEU — philschmid.de)

**Quelle:** philschmid.de/agent-harness-2026

**Was es ist:** Das klarste Denkmodell fuer den aktuellen Harness:
- CPU = Claude-Modell (Rechenpower, wird zur Commodity)
- RAM = Context Window (begrenzt, fluechtig)
- Betriebssystem = Agent Harness (Context Engineering, Hooks, Tool-Handler)
- Anwendung = Agent (spezifische Business-Logik)

**Kernaussage:** "Statische Benchmarks erfassen nicht wie Modelle nach 50+ Tool-Calls abdriften." Genau das misst aber der Session-Drift-Score aus v1.

**Zusaetzliche Erkenntnis:** "Harnesses werden zum Trainings-Datensatz fuer naechste Modell-Generationen." Der aktuelle Harness formt aktiv zukuenftige Claude-Versionen mit.

---

### 4.2 Context Pollution als Hauptfeind (NEU — MorphLLM)

**Quelle:** morphllm.com/agent-engineering

**Was es ist:** "Context pollution is the presence of too much irrelevant, redundant, or conflicting information within the context that distracts the LLM and degrades its reasoning accuracy. Context windows of all sizes will be subject to context pollution."

**MorphLLM's Kernbefund:** "After studying every major coding agent harness, one pattern dominates: **context pollution determines task failure**. The agents that win are the ones that keep their context clean."

**Drei Gegenstrategien:**
1. Context Engineering (was ins Kontext kommt)
2. Architectural Constraints (strukturelle Grenzen)
3. Entropy Management (periodischer Cleanup)

**Bezug zum System:** Die Direktiven-Dateien (~/.claude/rules/) kommen IMMER in den Kontext. Je mehr Regeln, desto mehr Pollution. Das ist der Trade-off. Loesung: Regeln muessen ULTRA-PRAEISE sein (kein Fueller, keine Wiederholungen).

---

### 4.3 Anthropic's eigene Context Engineering-Prinzipien (NEU)

**Quelle:** anthropic.com/engineering/effective-context-engineering-for-ai-agents

**Was Anthropic selbst empfiehlt:**
- "Die kleinste moegliche Menge hochqualitativer Tokens die die gewuenschten Ergebnisse maximiert"
- System Prompts in klar abgegrenzte Abschnitte strukturieren
- **Minimal starten und basierend auf beobachteten Fehlermustern erweitern**
- Kein "ueberladenes Tool-Set" mit Funktionsueberlappung
- Fuer Long-Horizon-Tasks: Structured Note-Taking (Agent-verwaltete externe Speicher)
- "CLAUDE.md Dateien werden naiv in den Context gedroppt — glob/grep ermoeglicht Just-in-Time-Retrieval"

**Wichtige Implikation:** Just-in-Time statt Always-On. Skills die `invocation: auto` haben (aus v1 vorgeschlagen) folgen genau diesem Prinzip. Die grossen Direktiven sollten mittelfristig als Auto-Skills geladen werden, nicht immer.

---

## Teil 5: Neue arXiv Papers (2025-2026, noch nicht in v1)

| Paper | arXiv | Relevanz | Kernbeitrag |
|-------|-------|----------|------------|
| Huxley-Goedel Machine | 2510.21614 | #1 | Oral ICLR 2026; CMP-Metrik fuer Selbstverbesserungs-Potential; Menschliches Niveau SWE-bench |
| Intrinsic Metacognitive Learning | 2506.05109 | #2 | ICML 2025; 3 Komponenten: Knowledge, Planning, Evaluation; Extrinsic→Intrinsic Uebergang |
| Meta Context Engineering (MCE) | 2601.21557 | #1+#2 | Co-Evolution von Skills und Context; 5.6-53.8% Verbesserung |
| Self-Evolving Agents Survey | 2507.21046 | Alle | Aktuellster Ueberblick; What/When/How Taxonomie |
| Agentic AI Intelligence Explosion | 2603.20639 | #1 | "Societies of Thought"; Intelligenz als soziales Phaenomen |
| Modular Procedural Memory | 2510.04851 | #1+#2 | Multi-Agent Workflow Memory; kontinuierlich wachsende Procedural Library |
| Natural-Language Agent Harnesses | 2603.25723 | Alle | NL-definierte Harness-Constraints statt Code |

---

## Teil 6: Priorisierte Umsetzungsvorschlaege (NEUE — nicht in v1)

### Sofort (unter 2 Stunden, hoher Impact)

| # | Massnahme | Mechanismus | Direktive |
|---|----------|-------------|-----------|
| **S1** | **ConfigChange-Hook migrieren** — config-guard.ps1 von PostToolUse auf ConfigChange (blockierend statt reaktiv) | Neuer Hook-Event | #3 |
| **S2** | **FileChanged-Hook fuer Cross-Platform** — bei .ps1-Aenderung automatisch .sh-Gegenstueck pruefen | Neuer Hook | #3 |
| **S3** | **Prozedurale Memory starten** — experience-store.jsonl um `workflow_steps`-Feld erweitern | JSONL-Schema | #1 |
| **S4** | **Hook-Meldungen loesungsorientiert machen** — Template "FEHLER: X. URSACHE: Y. FIX: Z." fuer alle Hooks | Hook-Texte | #2 |
| **S5** | **Bug-Case 403-Muster dokumentieren** — openreview.net und aehnliche akademische Seiten blockieren Fetches | bug-cases.jsonl | #3 |

### Mittelfristig (1-2 Tage)

| # | Massnahme | Mechanismus | Direktive |
|---|----------|-------------|-----------|
| **M1** | **InstructionsLoaded-Hook** — beim Laden von Direktiven automatisch relevante Bug-Cases anzeigen | Neuer Hook | #3 |
| **M2** | **Entropy Cleanup Cron** — alle 20 Sessions: Duplikate in bug-cases entfernen, alte MEMORY.md-Eintraege markieren | `claude schedule` | #1+#2 |
| **M3** | **Society of Thought Pattern** — bei wichtigen Entscheidungen 2 Agents mit gegensaetzlichen Ansaetzen | Agent-Pattern | #1 |
| **M4** | **Telemetrie-Aggregation** — trajectories.jsonl automatisch auf Retry-Quote und Drift-Rate auswerten | Python-Script + SessionEnd-Hook | #2 |
| **M5** | **Prozedurale Memory nutzen** — vor komplexen Aufgaben Workflow-Pattern aus experience-store suchen | PreSession-Integration | #1 |

### Langfristig (1-2 Wochen)

| # | Massnahme | Mechanismus | Direktive |
|---|----------|-------------|-----------|
| **L1** | **HGM-Metrik implementieren** — session-scores um `improvement_potential` fuer naechste Session erweitern | JSONL-Schema | #1 |
| **L2** | **MCE-Mechanismus** — Rules mit `helpful_count` + `co_activation` Tracking (co-evolutionaer) | ACE-Erweiterung | #1+#2 |
| **L3** | **Training-Candidates Export** — trajectories mit score >= 0.8 als potenzielle Trainings-Daten kennzeichnen | JSONL-Schema | #1 |
| **L4** | **Intrinsische Metacognition** — Agent entscheidet selbst welche 3 Kernsensoren er trackt (adaptiv) | Direktive #2 Erweiterung | #2 |
| **L5** | **Auto-Skills fuer Direktiven** — grosse Rules als Auto-Skills statt Always-On (~15k Token Ersparnis aus v1 noch offen) | Skill-Migration | Alle 3 |

---

## Teil 7: Bereits in v1 vorgeschlagen aber noch offen

Diese Punkte aus v1 sind NOCH NICHT umgesetzt — hohe Prioritaet:

| v1-Punkt | Warum noch wichtig |
|----------|-------------------|
| False Evidence Table in CLAUDE.md | Einfach umzusetzen, hoher Impact auf Qualitaet |
| Bugfix-FSM (OBSERVE→HYPOTHESIZE→FIX→VERIFY) | Direktive #3 Kernmechanismus fehlt noch |
| Confidence Decay auf JSONL-Eintraegen | Verhindert Context-Bloat langfristig |
| CBR Selective Retention (Jaccard) | Bug-Cases werden schon auto-erfasst aber nicht dedupliziert |
| Drift-Score Berechnung (Read/Write-Ratio) | Direktive #2 Tracker ist manuell, kein Auto-Score |
| Strategy-Tracking in experience-store (Intrinsic Metacognition) | Verbindet sich mit neuem Procedural-Memory-Konzept |

---

## Quellen-Index (neue Quellen seit v1)

### Neue Papers (noch nicht in v1)
- Huxley-Goedel Machine: arxiv.org/abs/2510.21614
- Intrinsic Metacognitive Learning: arxiv.org/abs/2506.05109
- Meta Context Engineering: arxiv.org/abs/2601.21557
- Self-Evolving Agents Survey: arxiv.org/abs/2507.21046
- Agentic AI Intelligence Explosion: arxiv.org/abs/2603.20639
- Modular Procedural Memory: arxiv.org/abs/2510.04851
- Natural-Language Agent Harnesses: arxiv.org/abs/2603.25723
- Hyperagents (Meta AI): arxiv.org/abs/2603.19461

### Neue Blog-Posts und Doku
- Agent Harnesses 2026: aakashgupta.medium.com
- The importance of Agent Harness: philschmid.de/agent-harness-2026
- IMPACT Framework: morphllm.com/agent-engineering
- Harness Engineering: martinfowler.com/articles/harness-engineering
- Self-Improving Agents Telemetry: arize.com/blog/closing-the-loop-...
- State of AI Agent Memory 2026: mem0.ai/blog/state-of-ai-agent-memory-2026
- Context Engineering (Anthropic): anthropic.com/engineering/effective-context-engineering-for-ai-agents
- Claude Code Hooks Doku (Maerz 2026): code.claude.com/docs/en/hooks
- Claude Code Hooks Guide: pixelmojo.io/blogs/claude-code-hooks-production-quality-ci-cd-patterns
