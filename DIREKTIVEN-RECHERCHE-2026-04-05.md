# Tiefe Recherche: Umsetzung der 3 Direktiven in Claude Code CLI

**Datum:** 2026-04-05
**Methode:** 5 parallele Researcher-Agenten, je max 15 Web-Fetches
**Quellen gesamt:** ~55 Funde aus Papers, Blog-Posts, GitHub-Repos, offizieller Doku

---

## Executive Summary: Die 10 wichtigsten Erkenntnisse

| # | Erkenntnis | Quelle | Direktive | Prioritaet |
|---|-----------|--------|-----------|------------|
| 1 | **Hooks sind staerker als Prompt-Regeln** — binaeere Exit-Codes kann kein LLM umgehen | Paddo.dev, Blake Crosley (95 Hooks in Produktion) | Alle 3 | HOCH |
| 2 | **Function-Level Fault Localization** ist empirisch bewiesen optimal | arXiv 2604.00167 (ICLR 2026) | #3 | HOCH |
| 3 | **Compound Intelligence Effect ist messbar**: 17-53% (SICA), 20%->50% (DGM) | arXiv 2504.15228, 2505.22954 | #1 | HOCH |
| 4 | **ACE Generator-Reflector-Curator** als Stop-Hook implementierbar | arXiv 2510.04618 (Stanford/ICLR 2026) | #1+#2 | HOCH |
| 5 | **25 Hook-Events verfuegbar** — viele ungenutzt (ConfigChange, FileChanged, InstructionsLoaded) | Offizielle Anthropic-Doku | Alle 3 | MITTEL |
| 6 | **Circuit Breaker** bei >3 Retries reduziert Fehlversuche von 8-12 auf ~3 | Askew (9 Monate Produktion) | #2 | HOCH |
| 7 | **Confidence Decay** (5%/Woche auf Memory-Eintraege) verhindert Context-Bloat | Medium Blueprint, mem0.ai | #1 | MITTEL |
| 8 | **Bug-Cases JSONL als ausfuehrbares System** statt Dokumentation (Analyzer-Templates) | Meta DrP (50.000 RCA/Tag) | #3 | HOCH |
| 9 | **Reflexion-Pattern** (NeurIPS 2023): +14% durch verbales Self-Feedback in Memory | arXiv 2303.11366 | #2 | HOCH |
| 10 | **Direktiven als Auto-Skills** statt CLAUDE.md spart ~8.000 Tokens pro Session | Offizielle Skills-Doku | Alle 3 | MITTEL |

---

## Teil 1: DIREKTIVE #1 — Superintelligenz

### 1.1 ACE Generator-Reflector-Curator Pipeline

**Quelle:** arXiv 2510.04618 (Stanford/UC Berkeley, ICLR 2026)

**Was es ist:** ACE behandelt Kontexte als "evolving playbooks" — strukturierte Bullets mit Metadaten (unique ID + helpful/harmful Counter). Generator produziert Reasoning-Trajektorien, Reflector extrahiert Lessons, Curator schreibt kompakte Delta-Updates zurueck. +10,6% auf Agent-Benchmarks.

**Umsetzung in Claude Code CLI:**
- **Stop-Hook** der Session-Transkripte durch Haiku schickt → strukturierte Bullets mit `helpful_count`/`harmful_count`
- Bullets werden in `~/.claude/rules/*.md` als itemisierte Eintraege geschrieben (nicht Fliesstext)
- **PreSession-Hook** liest nur Bullets mit `helpful_count > 2` ein = Curator-Logik
- Rules die sich bewaehren ("helpful") steigen auf, schlechte werden gefiltert

### 1.2 Confidence Decay auf Memory-Eintraegen

**Quelle:** Medium — Sourabh Sharma (Feb 2026), 4-Schichten-Architektur

**Was es ist:** Confidence Decay (~5%/Woche) auf allen Memory-Eintraegen. Eintraege unter 0.3 werden automatisch archiviert. Session-Start liest nur Eintraege mit confidence > 0.5.

**Umsetzung in Claude Code CLI:**
- Jeder JSONL-Eintrag (bug-cases, experience-store) bekommt `confidence`-Feld
- Woechentlicher Cron-Hook (claude schedule) wendet 5% Decay an
- Eintraege unter 0.3 → `archived/`-Verzeichnis
- Verhindert Context-Bloat, alte falsche Patterns werden automatisch "vergessen"

### 1.3 Compound Intelligence Effect quantifizieren

**Quelle:** SICA (arXiv 2504.15228), Darwin Goedel Machine (arXiv 2505.22954)

**Was es ist:** SICA: 17-53% Verbesserung durch Selbst-Editierung. DGM: 20%→50% durch iterative Selbstmodifikation mit evolutionaeerem Archiv.

**Umsetzung in Claude Code CLI:**
- `experience-store.jsonl` um `prevented_in_session`-Counter erweitern
- Wenn bekanntes Pattern einen Bug verhindert → Counter inkrementieren
- Woechentlicher Report: "Diese 5 Patterns haben in 7 Sessions N Fehler verhindert"
- Macht den Compound Effect **sichtbar und messbar** statt abstrakt

### 1.4 MemGPT Memory-Hierarchie

**Quelle:** arXiv 2310.08560 (UC Berkeley)

**Was es ist:** 3 Ebenen: Core Memory (immer aktiv), Recall Memory (durchsuchbar), Archival Memory (on-demand).

**Umsetzung in Claude Code CLI:**
- Core = CLAUDE.md + aktuelle MEMORY.md (immer im Kontext)
- Recall = `bug-cases.jsonl` + `experience-store.jsonl` (via Grep durchsuchbar)
- Archival = Git-History (via `git log --grep`)
- **PreCompact-Hook** rettet wichtige Patterns aus dem Kontext in JSONL bevor komprimiert wird

### 1.5 CLAUDE.md als Execution-Guide (empirisch validiert)

**Quelle:** arXiv 2509.14744 — Empirische Studie ueber reale CLAUDE.md-Dateien

**Was es ist:** Flache Hierarchie (1 H1, median 5 H2, 9 H3) ist optimal. Performance (12,7%), Security (8,7%) und Quality (8,3%) werden fast nie dokumentiert — genau die Luecke die die 3 Direktiven fuellen.

**Umsetzung in Claude Code CLI:**
- CLAUDE.md bleibt Execution-Guide (kurze Pointer auf Rules)
- Vollstaendige Direktiven in `~/.claude/rules/` = Archival Memory
- Jede Direktive hat 1 H2-Abschnitt in CLAUDE.md, kein Deep-Nesting

---

## Teil 2: DIREKTIVE #2 — Selbstbeobachtung

### 2.1 ClaudeWatch als existierende Implementierung

**Quelle:** github.com/blackwell-systems/claudewatch

**Was es ist:** Implementiert bereits alle 3 Tracker: Error-Loop-Erkennung (3. konsekutiver Fehler), Drift-Detektion (8 Reads ohne Write), Agent-Performance-Tracking. 29 MCP-Tools fuer queryable Self-Monitoring.

**Umsetzung:** Bereits installiert und aktiv. Nutzung intensivieren:
- `get_drift_signal` bei jedem 10. Tool-Call aufrufen
- `get_blockers()` bei jedem 2. Fehler
- `extract_current_session_memory` vor jeder Compaction

### 2.2 Circuit Breaker Pattern

**Quelle:** Askew Blog (9 Monate Produktion)

**Was es ist:** Nach 3 fehlgeschlagenen Versuchen: Automatisch eskalieren statt weiter probieren. Reduziert Fehlversuche von 8-12 auf ~3.

**Umsetzung in Claude Code CLI:**
- `PostToolUseFailure`-Hook verwaltet `~/.claude/retry-state.json`
- Format: `{"tool": "Bash", "consecutive_failures": 3, "last_error_hash": "..."}`
- Bei `consecutive_failures >= 3`: Hook gibt exit 2 + Meldung "Circuit Breaker offen"
- Reset bei erfolgreichem Tool-Call desselben Typs

### 2.3 False Evidence Table

**Quelle:** blakecrosley.com — 95 Hooks in Produktion, 9 Monate Erfahrung

**Was es ist:** 3-Spalten-Tabelle in CLAUDE.md: `Claim | Required Evidence | NOT Sufficient`. 7 "Named Failure Modes" mit mechanisch erkennbaren Signalen. Kernaussage: **Hooks sind staerker als Prompt-Instruktionen**.

**Umsetzung in Claude Code CLI:**
- CLAUDE.md um False Evidence Table erweitern:
  | Claim | Required Evidence | NOT Sufficient |
  |-------|------------------|----------------|
  | "Tests bestanden" | Exit-Code 0 von `./gradlew test` | "Sollte funktionieren" |
  | "Build erfolgreich" | Exit-Code 0 von `./gradlew assembleDebug` | "Keine Fehler gesehen" |
  | "Feature implementiert" | Benutzer hat getestet | "Code sieht richtig aus" |
- PostToolUse-Hook scannt auf Hedging-Pattern ("should", "probably", "seems to")

### 2.4 Agent Drift Quantifikation (ASI)

**Quelle:** arXiv 2601.04170

**Was es ist:** Agent Stability Index ueber 12 Dimensionen. 3 Drift-Typen: Semantic, Coordination, Behavioral. Mitigation: Episodic Memory Consolidation, Drift-Aware Routing.

**Umsetzung in Claude Code CLI:**
- Vereinfachter Drift-Score pro 10 Tool-Calls:
  `drift_score = (reads / (reads + writes)) * 0.6 + (bash_errors / total_calls) * 0.4`
- Bei Score > 0.7: Hook feuert "Drift erkannt — bist du noch am Session-Ziel?"
- Gespeichert in `~/.claude/drift-tracker.jsonl`

### 2.5 Reflexion-Pattern (NeurIPS 2023)

**Quelle:** arXiv 2303.11366 (Princeton/MIT)

**Was es ist:** 91% Pass@1 auf HumanEval (GPT-4: 80%). Verbale Reflexion + episodischer Memory-Buffer. Kein Gewichts-Update noetig.

**Umsetzung in Claude Code CLI:**
- `PostToolUseFailure`-Hook macht automatischen Haiku-Call: "Warum hat dieser Befehl versagt?"
- Ergebnis wird als temporaerer Kontext-Block injiziert
- `bug-cases.jsonl` IST der episodische Memory-Buffer aus Reflexion
- Automatischer Schreibzeitpunkt muss erzwungen werden (PostToolUseFailure)

### 2.6 Session-Score automatisieren

**Quelle:** Arize.com LLM Observability, ClaudeWatch

**Was es ist:** 3 messbare Proxy-Metriken ohne LLM-Judge:
- `intent_fidelity = 1 - (drift_events / total_turns)`
- `efficiency = 1 - (retry_count / total_tool_calls)`
- `learning_yield = min(1.0, new_rules_created + memories_saved)`

**Umsetzung in Claude Code CLI:**
- `SessionEnd`-Hook berechnet alle drei automatisch aus JSONL-Logs
- Speichert in `session-scores.jsonl`
- Trend ueber 10 Sessions = Compound-Intelligence-Nachweis

### 2.7 Metacognitive Dual-Process (ReMA)

**Quelle:** arXiv 2505.13763

**Was es ist:** High-Level Meta-Thinking Agent + Low-Level Reasoning Agent. Die Meta-Ebene ueberwacht die Arbeitsebene.

**Umsetzung in Claude Code CLI:**
- Meta-Prompt-Block am Anfang jeder Session: "Zaehle intern Retries, Korrekturen, Drift-Events"
- Stop-Hook ist der einzige Punkt wo Meta-Level und Object-Level kommunizieren
- Der Hook triggert Konsolidierung → strukturierte Intelligenz-Vorschlaege
- Macht Direktive #2 zu einem expliziten Dual-Process-System

---

## Teil 3: DIREKTIVE #3 — Resilient Bugfixing

### 3.1 RepairAgent FSM — Erzwungene Bugfix-Phasen

**Quelle:** arXiv 2403.17134 (ICSE 2025)

**Was es ist:** Finite State Machine steuert Tool-Aufrufreihenfolge: OBSERVE → HYPOTHESIZE → INSTRUMENT → FIX → VERIFY. Verhindert unkontrollierte Spruenge. 164 Bugs gefixt (39 davon erstmalig).

**Umsetzung in Claude Code CLI:**
- State-Tracking in `~/.claude/bugfix-state.json`: `{"phase": "OBSERVE", "bug_id": "..."}`
- PreToolUse-Hook auf `Edit/Write` blockiert wenn `phase != "FIX"`
- PreToolUse-Hook auf `Bash(git commit)` blockiert wenn `phase != "VERIFY"`
- Erzwingt den 5-Schritt-Ablauf aus Direktive #3

### 3.2 AutoFL Navigation-Pattern

**Quelle:** arXiv 2308.05487 (ACM FSE 2024)

**Was es ist:** +233% Method-Level Acc@1 durch navigierende Fehlersuche statt direktem Code-Oeffnen. LLM entscheidet selbst welche Funktionen es inspiziert.

**Umsetzung in Claude Code CLI:**
- "No-Edit-Before-Hypothesis" Guard: PreToolUse-Hook fuer Edit/Write
- Prueft ob in der aktuellen Session bereits Grep-Calls stattfanden
- Blockiert Edit wenn keine vorherige Analyse (Grep → Read → Hypothese)
- Das ist Poka-Yoke Stufe 2 fuer den Debugging-Workflow

### 3.3 3-Minuten-Triage als Stop-Hook

**Quelle:** Augment Code, CodeRabbit Report (AI Code hat 1.7x mehr Issues)

**Was es ist:** 3 Checks decken 60% aller Fehler: Linter, Type-Checker, Tests. Teams die darauf fokussieren reduzieren Regressionen um 60%.

**Umsetzung in Claude Code CLI:**
- Stop-Hook fuehrt aus: `python3 -m py_compile` / `tsc --noEmit` / `./gradlew test`
- Exit 2 wenn irgendetwas rot
- Automatisierte Fix-Induced-Failure-Pruefung (Schritt 3b der Direktive)
- Zusaetzlich: PreToolUse-Hook blockiert leeres `catch {}` (Regex-Pattern)

### 3.4 Bug-Cases als ausfuehrbares System (Meta DrP)

**Quelle:** Meta Engineering Blog (Dez 2025) — 50.000 automatische RCA/Tag, MTTR -80%

**Was es ist:** Standardisierte Analyzer-Templates + Code-Review + Backtesting. Organisationsweite Wiederverwendung schafft exponentiellen Mehrwert.

**Umsetzung in Claude Code CLI:**
- bug-cases.jsonl-Eintraege um `symptom_pattern` (Regex) erweitern
- PostToolUseFailure-Hook matcht jede Fehlermeldung automatisch gegen alle Patterns
- Bei Match: Alten Fix-Vorschlag als Context injizieren
- Bug-Datenbank wird von Dokumentation zu **ausfuehrbarem System**

### 3.5 CBR Selective Retention

**Quelle:** arXiv 2504.06943

**Was es ist:** Nicht jeder Fall lohnt gespeichert zu werden. Utility-basierte Entscheidung: Nur wenn neuer Fall ausreichend verschieden UND wiederholbar.

**Umsetzung in Claude Code CLI:**
- Python-Funktion vor Append in bug-cases.jsonl
- Jaccard-Aehnlichkeit auf Tags + symptom-Text
- Nur bei Aehnlichkeit < 0.7 zu existierenden Eintraegen speichern
- Verhindert Duplikate, haelt Datenbank schlank

### 3.6 Defense in Depth — Schicht 4 automatisieren

**Quelle:** Palo Alto Networks, AWS, arXiv 2603.11088

**Was es ist:** Bestehende Schichten 1-3 (Rules, Hooks, Guards). Fehlend: Schicht 4 (Upstream).

**Umsetzung in Claude Code CLI:**
- PostToolUse-Hook nach Append in bug-cases.jsonl
- Wenn Jaccard-Aehnlichkeit > 0.8 zu existierendem Eintrag (= wiederkehrender Bug)
- Automatisch GitHub-Issue erstellen: `gh issue create --label "recurring-bug"`
- Das ist Poka-Yoke Stufe 2 fuer den Upstream-Meldepfad

### 3.7 Self-Healing Software (biologisch inspiriert)

**Quelle:** arXiv 2504.20093

**Was es ist:** 3 Komponenten analog zum Immunsystem: Sensory (erkennen), AI Core (diagnostizieren), Healing Agents (reparieren).

**Umsetzung in Claude Code CLI:**
- PostToolUse-Hook als "Sensor" liest Build-Output nach jedem Edit
- `debugger`-Agent als "AI Core" formuliert Hypothesen
- `coder`-Agent als "Healing Agent" schreibt den Patch
- Stop-Hook fuehrt automatisch `adb logcat -d | grep -i error` aus (Android)

---

## Teil 4: Claude Code CLI — Technische Architektur

### 4.1 Alle 25 Hook-Events (vollstaendige Referenz)

```
SessionStart, InstructionsLoaded, UserPromptSubmit,
PreToolUse, PermissionRequest, PermissionDenied,
PostToolUse, PostToolUseFailure,
SubagentStart, SubagentStop,
TaskCreated, TaskCompleted,
Stop, StopFailure,
TeammateIdle,
PreCompact, PostCompact,
ConfigChange, CwdChanged, FileChanged,
WorktreeCreate, WorktreeRemove,
Elicitation, ElicitationResult,
SessionEnd
```

**Ungenutzte Events mit hohem Potenzial:**
| Event | Potenzial fuer Direktiven |
|-------|--------------------------|
| `ConfigChange` | Poka-Yoke Stufe 3 fuer Permission-Schutz (blockierbar!) |
| `InstructionsLoaded` | Bug-Warnungen bei Session-Start injizieren |
| `FileChanged` | Automatische Cross-Platform-Pruefung |
| `PostToolUseFailure` | Automatischer Bug-Cases-Writer |
| `PreCompact` | Memory-Rettung vor Kompression |

### 4.2 Hook-Typen

| Typ | Wann nutzen | Beispiel |
|-----|------------|---------|
| `command` | Deterministische Pruefungen | Regex-Guard, Linter |
| `prompt` | Semantische Pruefungen | "Wurde ein Feature entfernt?" |
| `agent` | Komplexe Analyse | Codebase-weite Pruefung |
| `http` | Externe Services | Webhook an Dashboard |

### 4.3 Kritische Bugs/Patterns

1. **`stop_hook_active`-Flag**: IMMER pruefen im Stop-Hook, sonst Infinite Loop
2. **`transcript_path`-Bug**: Immer neueste JSONL per `glob+mtime` suchen
3. **Output-Limit**: 10.000 Zeichen fuer `additionalContext`
4. **Exit-Code 2**: Einziger Weg eine Aktion zu blockieren (nur PreToolUse + ConfigChange)

### 4.4 Direktiven als Auto-Skills (Token-Sparvorschlag)

**Quelle:** code.claude.com/docs/en/skills

Statt alle 3 Direktiven als `~/.claude/rules/` (immer geladen, ~25k Tokens):
- `resilient-bugfixing.skill` mit `invocation: auto` — triggert bei Fehlermeldungen
- `self-observation.skill` mit `invocation: auto` — triggert bei Session-Ende
- `superintelligence.skill` mit `invocation: manual` — nur bei `/self-improve`

**Ersparnis:** ~15.000 Tokens pro Session (nur geladene Skills verbrauchen Kontext)

---

## Teil 5: Akademische Papers (State of the Art)

### Direkt relevant fuer die 3 Direktiven

| Paper | Jahr | Bezug | Kernbeitrag |
|-------|------|-------|-------------|
| SICA — Self-Improving Coding Agent | 2025 | #1 | 17-53% Verbesserung durch Selbst-Editierung |
| Live-SWE-agent | 2025 | #1+#3 | Runtime-Selbstevolution, 77.4% SWE-bench |
| ACE — Agentic Context Engineering | 2025 | #1+#2 | Evolving Playbooks, +10.6% |
| Hyperagents | 2026 | #1+#2 | Meta-Agent der den Modifikationsmechanismus selbst verbessert |
| Reflexion | 2023 | #2 | Verbales Self-Feedback, +14% |
| Fault Localization Granularity | 2026 | #3 | Function-Level ist empirisch optimal |
| Trae Agent Ensemble | 2025 | #3 | 3-Pfad-Ensemble, +10.22% |
| SWE-Search MCTS | 2024 | #3 | Backtracking statt greedy, +23% |
| CBR for LLM Agents | 2025 | #3 | Formale Basis fuer bug-cases.jsonl |
| Darwin Goedel Machine | 2025 | #1 | 20%→50%, emergente Peer-Review |
| Agent-R Trajectory Revisions | 2025 | #2+#3 | Revision Signals am ersten Fehlerpunkt |

---

## Teil 6: Priorisierte Umsetzungsvorschlaege

### Sofort umsetzbar (1-2 Stunden, hoher Impact)

| # | Massnahme | Hook/Mechanismus | Direktive |
|---|----------|-----------------|-----------|
| 1 | Circuit Breaker: Bei >3 gleichen Fehlern automatisch stoppen | `PostToolUseFailure`-Hook + retry-state.json | #2 |
| 2 | Bug-Cases Auto-Writer: Jede Fehlermeldung automatisch loggen | `PostToolUseFailure`-Hook → bug-cases.jsonl | #3 |
| 3 | False Evidence Table in CLAUDE.md | Manuell einfuegen | #2 |
| 4 | Session-Score automatisieren (3 Metriken) | `SessionEnd`-Hook → session-scores.jsonl | #2 |

### Mittelfristig (1-2 Tage, strukturelle Verbesserung)

| # | Massnahme | Hook/Mechanismus | Direktive |
|---|----------|-----------------|-----------|
| 5 | ConfigChange-Hook statt PostToolUse fuer Permission-Guard | Neuer Hook auf `ConfigChange` | #3 |
| 6 | Bugfix-FSM: Erzwungene Phasen OBSERVE→FIX→VERIFY | PreToolUse-Guards + bugfix-state.json | #3 |
| 7 | Drift-Score Berechnung (Read/Write-Ratio) | PostToolUse-Hook + drift-tracker.jsonl | #2 |
| 8 | Confidence Decay auf JSONL-Eintraegen | Woechentlicher Cron-Job | #1 |

### Langfristig (1-2 Wochen, systemische Aenderung)

| # | Massnahme | Hook/Mechanismus | Direktive |
|---|----------|-----------------|-----------|
| 9 | Direktiven als Auto-Skills (spart ~15k Tokens) | Skill-Migration | Alle 3 |
| 10 | Bug-Cases als MCP-Server mit RAG | Neuer MCP-Server | #3 |
| 11 | ACE Curator als Stop-Hook (Generator-Reflector-Curator) | Stop-Hook + Haiku-Call | #1 |
| 12 | Strategy-Tracking in experience-store.jsonl (Intrinsic Metacognition) | JSONL-Schema-Erweiterung | #1 |

---

## Quellen-Index (alle 55+ Quellen)

### Papers
- ACE: arXiv 2510.04618
- Reflexion: arXiv 2303.11366
- A-MEM Zettelkasten: arXiv 2502.12110
- MemGPT: arXiv 2310.08560
- Memory Survey: arXiv 2603.07670
- AutoFL: arXiv 2308.05487
- AgentFL: arXiv 2403.16362
- CLAUDE.md Studie: arXiv 2509.14744
- ReMA Metacognition: arXiv 2505.13763
- Agent Drift ASI: arXiv 2601.04170
- DeepContext: arXiv 2602.16935
- Agent-R: arXiv 2501.11425
- MUSE: arXiv 2411.13537
- SICA: arXiv 2504.15228
- Live-SWE-agent: arXiv 2511.13646
- ToolTree: arXiv 2603.12740
- Trae Agent: arXiv 2507.23370
- Hyperagents: arXiv 2603.19461
- Fault Localization: arXiv 2604.00167
- SWE-Search: arXiv 2410.20285
- Darwin Goedel Machine: arXiv 2505.22954
- CBR for LLM Agents: arXiv 2504.06943
- Uncertainty Survey: arXiv 2503.15850
- RepairAgent: arXiv 2403.17134
- Self-Healing Software: arXiv 2504.20093
- LogSage: arXiv 2506.03691
- APR Survey: arXiv 2506.23749
- Defense in Depth AI: arXiv 2603.11088
- AutoCrashFL: arXiv 2510.22530
- Metacognitive Self-Correction: arXiv 2510.14319
- Georgia Tech TMK: dilab.gatech.edu
- Intrinsic Metacognitive Learning: arXiv 2506.05109

### Blog-Posts & Dokumentation
- Persistent Memory Blueprint: medium.com/@sourabh.node
- State of AI Agent Memory 2026: mem0.ai
- Poka-Yoke + AI: retrocausal.ai, corvair.ai, flowfuse.com
- Hooks Guardrails: paddo.dev
- Metacognitive AI Programming: blakecrosley.com
- LLM Observability: arize.com
- AI Drift Prevention: getmaxim.ai
- Debugging AI Code: augmentcode.com
- CodeRabbit Report: coderabbit.ai
- Meta DrP: engineering.fb.com
- Askew Circuit Breaker: write.as/askew

### GitHub-Repos & Offizielle Doku
- ClaudeWatch: github.com/blackwell-systems/claudewatch
- Claude Code Hooks Mastery: github.com/disler/claude-code-hooks-mastery
- Claude Memory: github.com/codenamev/claude_memory
- MCP Memory Keeper: github.com/mkreyman/mcp-memory-keeper
- Awesome Claude Code: github.com/hesreallyhim/awesome-claude-code
- Offizielle Hooks-Doku: code.claude.com/docs/en/hooks
- Offizielle Skills-Doku: code.claude.com/docs/en/skills
