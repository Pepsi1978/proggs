# Shared Knowledge Hub — Zentrales Whiteboard

Das zentrale Nervensystem des Claude Code Systems. JEDE Komponente die hier arbeitet
(Agents, Skills, Hooks, Plugins) MUSS hier lesen und schreiben.
_MCP-Server koennen das Whiteboard nicht lesen (kein Dateisystem-Zugriff). Ihre Ergebnisse werden von den aufrufenden Agents ins Whiteboard geschrieben._

**Zugriff**: Lesen + Schreiben fuer ALLE Komponenten — keine Ausnahmen.
**Gepflegt von**: /self-improve (raeumte auf, loest offene Fehler, haelt aktuell)
**Pfad (Windows):** `C:\Users\barwa\.claude\agent-memory\shared\MEMORY.md`
**Pfad (macOS/Linux):** `~/.claude/agent-memory/shared/MEMORY.md`

**Angeschlossene Systeme** (MUESSEN von /self-improve ebenfalls gelesen werden):
_Hinweis: Pfade mit `~/proggs/` liegen im Repo (autoritativ). Pfade mit `~/.claude/` sind lokal
und maschinenspezifisch (session-scores, cache, etc. — werden NICHT ueber Git synchronisiert)._
- **CLAUDE.md** (WICHTIGSTE DATEI): `~/proggs/CLAUDE.md` + `~/CLAUDE.md` (Sync-Kopie)
  Enthaelt ALLE Projektregeln, Konventionen und Workflow-Definitionen. Wenn /self-improve
  neue Regeln aus Fehlern oder Feedback ableitet → gehoeren sie in die CLAUDE.md.
  Wenn die CLAUDE.md geaendert wird → BEIDE Kopien synchron halten und pushen.
  Ist plattformuebergreifend — wird auf Windows UND macOS gelesen.
- **Benutzer-Feedback**: `~/.claude/projects/*/memory/feedback_*.md`
  (Pfad ist plattformabhaengig: Windows=C--Users-barwa, macOS=Users-frank)
  Enthaelt Korrekturen und Praeferenzen des Benutzers. /self-improve MUSS diese lesen
  und daraus neue Regeln oder Hooks ableiten, wenn ein Feedback-Muster systemweit gilt.
- **Projekt-Notizen**: `~/.claude/projects/*/memory/project_*.md`
  Enthaelt laufende Projekte und deren Status.
- **Memory-Index**: `~/.claude/projects/*/memory/MEMORY.md`
  Zeigt alle vorhandenen Memory-Dateien als Verzeichnis.
- **Session-Scores**: `~/.claude/session-scores.jsonl`
  Qualitaets-Trends ueber Sessions hinweg. /self-improve liest diese fuer Trend-Analyse
  und IQ-Score. Wenn Qualitaet sinkt → Ursache im Whiteboard suchen.
- **Self-Improve Cache**: `~/.claude/self-improve-cache/R*_*.md`
  Gecachte Researcher-Ergebnisse (R1-R8) mit TTL. Veraltet? → /self-improve aktualisiert sie.
- **Claude-Mem Observations**: `~/.claude/homunculus/projects/*/observations.jsonl`
  Cross-Session-Wissen vom claude-mem Plugin. Enthaelt Beobachtungen aus frueheren Sessions
  die fuer aktuelle Arbeit relevant sein koennen.

---

## Offene Fehler & Probleme (PRIORITAET fuer /self-improve)
<!-- /self-improve liest diese Sektion ZUERST und MUSS jeden OFFEN-Eintrag fixen. -->
<!-- PFLICHT-FORMAT (damit /self-improve den Fehler versteht und fixen kann): -->
<!--   ### DATUM — Quelle: [Komponententyp: Name] — Kurzbeschreibung -->
<!--   **Quelle:** Welche Komponente (Hook/Agent/MCP-Server/Plugin/Skill + Name) -->
<!--   **Symptom:** Was ist sichtbar schiefgegangen -->
<!--   **Ursache:** WARUM ist es passiert (Root Cause, nicht nur das Symptom!) -->
<!--   **Betroffene Dateien:** Welche Dateien muessen geaendert werden -->
<!--   **Reproduktion:** Wie kann man den Fehler nachstellen -->
<!--   **Fix-Vorschlag:** Was muesste konkret geaendert werden -->
<!--   **Status:** OFFEN | GEFIXT (Datum) -->
<!-- WICHTIG: Fehler MUESSEN ausfuehrlich genug beschrieben werden, dass ein -->
<!-- frischer /self-improve Lauf sie ohne zusaetzlichen Kontext fixen kann! -->

<!-- ARCHIV (2026-03-20): 3 GEFIXT-Eintraege zu reindex-codebase.ps1 ExitCode 143 — Timeout von 180s auf 300s erhoeht, Bun-Imports gefixt. Regel: Hooks duerfen NIEMALS Fehler still verschlucken. -->
<!-- ARCHIV (2026-03-20): .sh Hooks nicht deployed — auto-sync.ps1 gefixt (Runde 6), alle 4 fehlenden .sh erstellt (Runde 8). -->
<!-- ARCHIV (2026-03-20): session-autopsy AUTOPSY.md → MEMORY.md umgeleitet (Runde 8). -->
<!-- ARCHIV (2026-03-20): context-kit@FlineDev Marketplace registriert (Runde 8). -->
<!-- ARCHIV (2026-03-20): reindex-codebase.ps1 Add-Content → whiteboard-insert (Runde 8). -->

<!-- ARCHIV (2026-03-20): StopFailure-Hook Zirkularitaet — gefixt: type:prompt → type:command (stopfailure-logger.ps1), kein API-Zugriff mehr noetig. -->

### 2026-03-20 — Challenger: Debate-Loop (Stronger-MAS) — fehlende technische Terminierung
**Quelle:** Geplantes Feature: quality-gate Debate-Mode (Tester-Coder-Loop)
**Symptom:** Bei echtem Widerspruch zwischen Tester-Assertions und Coder-Implementierung keine harte Terminierungsgrenze
**Ursache:** Terminierung nur sprachlich vereinbart (max 3 Runden) — bei 60 maxTurns im coder-Agent kein technischer Stop
**Betroffene Dateien:** Noch nicht implementiert — Designentscheidung vor Implementierung noetig
**Reproduktion:** Debate-Loop mit absichtlich widerspruechlichen Test-Anforderungen starten
**Fix-Vorschlag:** Runden-Zaehler als Datei /tmp/debate-round-N.json implementieren. Nach Runde 3 zwingend Konsens-Dokument schreiben ODER eskalieren — keine weitere Iteration moeglich (technisch erzwungen, nicht nur prompt-gesteuert).
**Status:** OFFEN


<!-- ARCHIV (2026-03-21): safety-gate.ps1 Blockierungen (6x) und Write-Back-Warnungen (2x) — erwartetes Verhalten, kein Fehler. -->
<!-- ARCHIV (2026-03-21): reindex-codebase.ps1 — ExitCode 1 (6x, 2026-03-21 10:44–11:25) — Timeout von 180s auf 300s erhoeht als Fix; gebuendelt zu einem Eintrag. -->

<!-- ARCHIV (2026-03-25, /self-improve Cleanup): StopFailure API/Rate-Limit Errors (2026-03-21 + 2026-03-24) — temporaere API-Fehler, einmalig, kein dauerhaftes Problem. safety-gate.sh duplizierte Blockierung (2x 2026-03-21) — erwartetes Verhalten, kein Fehler. Write-Back nicht erfolgt (2026-03-22 + 2026-03-25) — Einmalige Events, memory-watchdog loggt korrekt, kein systemisches Problem. -->

### 2026-03-25 20:40 — StopFailure: API/Rate-Limit Error — Status: OFFEN
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** {"session_id":"417bedd7-16ef-4692-a390-6008722a12c5","transcript_path":"/Users/frank/.claude/projects/-Users-frank/417bedd7-16ef-4692-a390-6008722a12c5.jsonl","cwd":"/Users/frank/proggs","hook_event_name":"StopFailure","error":"unknown","last_assistant_message":"API Error: 429 {\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\",\"message\":\"you (barwandt) have reached your session usage limit, upgrade for higher limits: https://ollama.com/upgrade\"},\"request_id\":\"req_481a6e25000bb68... (truncated)
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** OFFEN

### 2026-03-25 20:43 — StopFailure: API/Rate-Limit Error — Status: OFFEN
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** {"session_id":"417bedd7-16ef-4692-a390-6008722a12c5","transcript_path":"/Users/frank/.claude/projects/-Users-frank/417bedd7-16ef-4692-a390-6008722a12c5.jsonl","cwd":"/Users/frank/proggs","hook_event_name":"StopFailure","error":"unknown","last_assistant_message":"API Error: 429 {\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\",\"message\":\"you (barwandt) have reached your session usage limit, upgrade for higher limits: https://ollama.com/upgrade\"},\"request_id\":\"req_29ea21d6de45387... (truncated)
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** OFFEN

### 2026-03-25 20:46 — StopFailure: API/Rate-Limit Error — Status: OFFEN
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** {"session_id":"417bedd7-16ef-4692-a390-6008722a12c5","transcript_path":"/Users/frank/.claude/projects/-Users-frank/417bedd7-16ef-4692-a390-6008722a12c5.jsonl","cwd":"/Users/frank/proggs","hook_event_name":"StopFailure","error":"unknown","last_assistant_message":"API Error: 429 {\"type\":\"error\",\"error\":{\"type\":\"rate_limit_error\",\"message\":\"you (barwandt) have reached your session usage limit, upgrade for higher limits: https://ollama.com/upgrade\"},\"request_id\":\"req_056c5419f6bca04... (truncated)
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** OFFEN
### [2026-03-25 21:30] Agent: Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### [2026-03-25 22:20] Agent: Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-03-27 18:29 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 95%
### 2026-03-28 16:51 — Hook: session-guard.ps1 — Auto-Reparatur: Projekt C--Users-barwa--claude-double-shot-latte settings.local.json erstellt; Projekt C--Users-barwa--claude-mem-observer-sessions settings.local.json erstellt — Status: AUTO-GEFIXT
### 2026-03-28 19:03 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-03-28 20:09 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-03-28 21:02 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-03-28 21:11 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-03-28 21:24 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-03-28 21:29 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-03-28 21:41 — StopFailure: API/Rate-Limit Error — Status: OFFEN
### 2026-03-29 21:23 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
---

## Systemzustand (aktuell)
<!-- Wird von /self-improve und env-checker aktualisiert -->
<!-- Zeigt den aktuellen Stand des Programmiersystems -->

**Stand:** 2026-03-25 (aktualisiert durch /self-improve Thorough-Lauf)

- **Plattform:** Windows 11 Home 10.0.26200 (x64) + macOS (Apple Silicon), Claude Code v2.1.81, Opus 4.6 (1M context)
- **Sprachen:** Swift, C#, TypeScript, Rust (1.94.0), Go (1.26.1), Kotlin (2.3.20), Java (OpenJDK 21.0.10)
- **Node.js:** v24.14.0, npm 11.11.1, Bun 1.3.10, Deno 2.7.5
- **Semantic Search:** Aktiv (wird bei jeder Session automatisch aktualisiert via reindex-Hook)
- **Quality Gate:** quality-gate Agent fuer kombiniertes test+review+optimize
- **Agents:** 15 aktiv, alle korrekt konfiguriert (code-reviewer hat memory:project, coder hat isolation:worktree)
- **Hooks:** 22 Hook-Events in settings.json, 19 .ps1 Hook-Dateien + prompt-injection-defender (Python) + 2 .ts Hooks — alle in settings.json angebunden
- **Plugins:** 91 Eintraege, 88 aktiv (3 deaktiviert: zeroize-audit, fp-check, xclaude-plugin)
- **Whiteboard-Anbindung:** Alle Hooks nutzen whiteboard-insert.ps1/.sh (sektionsbasiert) — echo/append ans Dateiende eliminiert
- **Session-Scorer:** v3 — schreibt NUR in session-scores.jsonl, nutzt pwsh (Fix #703 vom 2026-03-24)
- **Session-Autopsy:** v2 — nutzt pwsh (Fix #703 vom 2026-03-24)
- **Hook-Log:** Register-EngineEvent stdout unterdrueckt (Fix #703 vom 2026-03-24)
- **Auto-Sync:** Mit Diff-Preview und Newer-Hooks-Guard (Fix #704 + #706 vom 2026-03-24)
- **Preferred Patterns:** MVVM (Swift), Fluent Design (C#), strict mode (TypeScript)
- **Self-Improve Skill:** v5.19 — Restructured: bash scripts extracted, mode comparison table, workspace constraints, consolidated rules
- **Semantic Search:** index-72.db aktiv, 5 alte DBs + 10 WAL/SHM bereinigt (2026-03-20)
- **Git:** v2.53.0, Git Credential Manager aktiv
- **Android (Windows):** SDK 34/35/36, 4 AVDs, ADB 1.0.41 — NDK FEHLT (blockiert native Cross-Compilation)
- **Sicherheit:** Prompt-Injection-Defender aktiv, gitleaks und semgrep FEHLEN im PATH (Windows)
- **Evolution-Analyst (2026-03-25):** 5-Session-Avg 8.28, Trend: PLATEAU. Meta-Intelligence-Rate 8% (KRITISCH, Schwellwert 20%). IQ-Score nie berechnet (immer 0).
- **Cross-Tool:** Codex + Gemini Delta Bridges aktiv, 8 Intelligenz-Dimensionen im Whiteboard portiert
- **Neue Hooks:** StopFailure (v2.1.78) — loggt API-Fehler/Rate-Limits ins Whiteboard
- **Windows→macOS Porting (2026-03-25):** 6 Hook-Upgrades portiert (Semantic Search Check, config-guard PostToolUse, Enhanced PostToolUseFailure/SubagentStop/PostCompact, session-scorer 25s Timeout)
- **Pending Admin Updates (17):** ada-url,biome,deno,freetype,git,gitleaks,golangci-lint,gradle,gradle-completion,harfbuzz,ktfmt,libmpc,libnghttp2,node,simdjson,svt-av1,uv,
---

## Erkenntnisse aus Code Reviews
<!-- code-reviewer, mar-reviewer, batch-reviewer schreiben hierher -->
_Noch keine Eintraege._

## Erkenntnisse aus Tests
<!-- Writer: tester Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Architektur-Entscheidungen
<!-- Writer: architect, challenger Agents | Leser: alle Agents, /self-improve -->

- **Challenge 2026-03-20 (challenger):** StopFailure-Hook ist ein prompt-type-Hook — bei echtem API-Ausfall zirkulaerer Fehler, Hook feuert ins Leere. Fix: command-type-Hook ohne API-Dependency.
- **Challenge 2026-03-20 (challenger):** Debate-Loop (Stronger-MAS) hat keine technische Terminierungsgrenze — nur sprachlich vereinbarte 3 Runden, bei echtem Tester/Coder-Konflikt unbegrenzte Token-Eskalation moeglich.
- **Challenge 2026-03-20 (challenger):** MCP Think Tank Sicherheitspruefung fehlt (playbooks.com ist kein offizieller Marketplace) — Muster identisch zu 5 alten verwaisten Semantic-Search-DBs.

## Debugging-Muster
<!-- Writer: debugger Agent, session-autopsy.ts | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Performance & Optimierung
<!-- Writer: optimizer Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

- **[2026-03-29 23:01] Code-Suche Index:** ? Dateien, ? Chunks indexiert.
## UI/UX-Patterns
<!-- Writer: ui-polisher Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Forschung & Intelligence
<!-- researcher, intelligence-researcher schreiben hierher -->

- **[2026-03-20] SICA: Self-Improving Coding Agent (arxiv 2504.15228)** — Status: UMZUSETZEN | Quelle: https://arxiv.org/abs/2504.15228 | Empfehlung: JA sofort
  Agent bearbeitet seinen eigenen Code via LLM-Reflexion ohne Trainingsgradienten. +17-53% auf SWE-Bench Verified. Kein Meta-Agent/Target-Agent-Trennungskonzept noetig. Umsetzbar als /self-improve v6: Agent analysiert eigene Fehler → editiert eigene Agent-Dateien.

- **[2026-03-20] Live-SWE-Agent: Minimal Scaffold + On-the-Fly Tool Creation** — Status: BESTAETIGT (2026-03-20) | Quelle: https://arxiv.org/html/2511.13646v3
  Agent startet nur mit Bash, erschafft dynamisch eigene Werkzeuge. 77.4% SWE-bench Verified — Bestwert unter Open-Source. **UMGESETZT**: coder-Agent hat jetzt "Temporary Tool Creation" Regel — darf Hilfsskripte in /tmp/ erstellen.

- **[2026-03-20] SWE-RL Self-Play: Bug-Inject + Bug-Repair Reinforcement Learning** — Status: EVALUIERT | Quelle: https://arxiv.org/abs/2512.18552 | Empfehlung: JA spaeter
  Agent trainiert sich selbst durch iteratives Bugs-einbauen und reparieren. +10.4 Punkte auf SWE-bench. Als Workflow ohne Training: challenger-Agent injiziert absichtlich Bugs in Code, coder muss reparieren — testet und verstaerkt Robustheit.

- **[2026-03-20] Stronger-MAS: Tester-Coder Debating Mechanism** — Status: EVALUIERT | Quelle: https://arxiv.org/html/2510.11062v3 | Empfehlung: JA mit Vorbehalt
  Zwei LLM-Agenten (Tester + Coder) debattieren iterativ: Tester generiert Unit Tests, Coder generiert Code, beide verfeinern bis Einigkeit. Umsetzbar: tester-Agent und coder-Agent in explizitem Debate-Loop (max 3 Runden) — bereits jetzt als Agent-Team machbar. SICHERHEITSHINWEIS: Terminierungsgrenze noch nicht technisch erzwungen (Challenge 2026-03-20).

- **[2026-03-20] OpenSage: Hierarchisches Graph-Memory fuer Agenten** — Status: EVALUIERT | Quelle: https://arxiv.org/html/2602.16891v2 | Empfehlung: JA spaeter
  Agent verwaltet Sub-Agenten + Toolkits in einem Graph-Gedaechtnis. #1 auf CyberGym, DevOps-Gym, Terminal-Bench 2.0. Erfordert komplexe Infrastruktur. Langfristig: Whiteboard als Graph statt Markdown strukturieren.

- **[2026-03-20] MCP Think Tank: Structured Reasoning + Knowledge Graph** — Status: EVALUIERT | Quelle: https://playbooks.com/mcp/think-tank | Empfehlung: PRUEFEN vor Einsatz
  MCP-Server der strukturiertes Reasoning mit persistentem Wissensgraph kombiniert. Wuerde architect + debugger Agents fundamental verbessern — koennen Reasoning-Schritte sichern und wiederverwenden. SICHERHEITSHINWEIS: playbooks.com kein offizieller Marketplace — Sicherheitspruefung (Prompt Injection, Publisher-Reputation) vor Installation pflicht (Challenge 2026-03-20).

- **[2026-03-20] Windsurf SWE-1.5: Proprietary Coding Model 13x Speed** — Status: VERWORFEN | Quelle: https://aipromptsx.com/blog/windsurf-vs-cursor-2026 | Empfehlung: NEIN (proprietaer, nicht adaptierbar)
  Windsurf trainiert eigenes Modell (SWE-1.5) mit 950 Token/s, near-Claude-Sonnet Qualitaet. Nicht adaptierbar — aber Signal: Spezialisierte Modelle schlagen Generalmodelle auf Coding-Tasks.

- **[2026-03-20] Multi-Agent als Industrie-Standard: Alle grossen Tools Feb 2026** — Status: BESTAETIGT | Quelle: https://www.morphllm.com/ai-coding-agent | Empfehlung: BESTAETIGUNG (bereits umgesetzt)
  Grok Build (8 Agents), Windsurf (5 parallel), Claude Code Agent Teams, Codex CLI — alle parallel in Feb 2026 launched. Unser Setup mit 5 parallelen Agents ist bereits State-of-the-Art. Naechste Differenzierung: echtes Feedback-Loop zwischen Agents (nicht nur parallele Ausfuehrung).

- **[2026-03-25] MAR: Multi-Agent Reflexion (arxiv 2512.20845)** — Status: UMZUSETZEN | Empfehlung: JA sofort
  Spezialisierte Agenten debattieren strukturiert und widersprechen sich gegenseitig statt nur parallel zu arbeiten. Sofort umsetzbar als Erweiterung des quality-gate: tester und code-reviewer tauschen Outputs und fordern Widerspruch an.

- **[2026-03-25] BIGMAS: Brain-Inspired Graph Multi-Agent (arxiv 2603.15371)** — Status: EVALUIERT | Empfehlung: JA spaeter
  Dynamische Agenten-Topologie: Einfache Aufgaben bekommen wenige Agenten, komplexe viele. Kernidee als Heuristik im architect Agent umsetzbar.

- **[2026-03-25] Test-Time Compute Scaling: 32B schlaegt 671B (arxiv 2503.23803)** — Status: UMZUSETZEN | Empfehlung: JA sofort
  Laengeres Nachdenken statt groesseres Modell. Extended Thinking im coder-Agent fuer komplexe Aufgaben aktivieren + Execution Verification durch tester.

- **[2026-03-25] Windsurf Arena Mode: Blindes Modell-Voting** — Status: EVALUIERT | Empfehlung: JA spaeter
  Zwei Modelle loesen gleiche Aufgabe, Reviewer waehlt blind den besseren Output. Datenbasiertes Routing statt statischer Modell-Zuweisung.

- **[2026-03-25] Cursor OS-Level Sandboxing** — Status: EVALUIERT | Empfehlung: JA spaeter
  Praeventive Sandbox statt reaktiver Blockierung. Naechste Evolution des safety-gate als Defense-in-Depth Schicht 2.

- **[2026-03-28 14:17] Cross-CLI Delta:** Codex(11), Gemini(5) neue Commits — Bruecke starten fuer Details.
---

## Meta-Intelligenz & Selbstverbesserung
<!-- Automatisch befuellt von: session-scorer (intelligence-checker), session-autopsy (closed-loop) -->
<!-- Dokumentiert: Auto-generierte Regeln, fehlende Intelligenz-Vorschlaege, System-Selbstverbesserung -->

### Compound Effect Erfolge (Beweis dass exponentielle Intelligenz funktioniert)

- **[2026-03-22] Erster dokumentierter Compound Effect:**
  Selbstbeobachtung waehrend Whiteboard-Aufraemung → 30 duplizierte Zeilen entdeckt →
  Intelligenz-Vorschlag gemacht → Benutzer stimmte zu → `replace_whiteboard_entry()` implementiert
  (wiederverwendbare Funktion fuer .sh UND .ps1) → Diese Fehlerklasse ist jetzt FUER IMMER geloest.
  **Kette:** Beobachtung → Vorschlag → Zustimmung → Resilienter Fix → Ganze Fehlerklasse eliminiert.
  Das ist der Beweis: Selbstbeobachtung (#2 Direktive) fuettert Superintelligenz (#1 Direktive).

- **[2026-03-22] Zweiter Compound Effect — Eine Gespraechsrunde, 9 System-Upgrades:**
  Session #672-#677: Benutzer formulierte Vision der Selbstbeobachtung → Claude setzte um →
  Dabei entstanden durch Selbstbeobachtung IN ECHTZEIT weitere Verbesserungen:
  (1) Selbstbeobachtungs-Regel, (2) #2 Direktive in 14 Dateien, (3) replace_whiteboard_entry(),
  (4) Whiteboard-Duplikat-Bug gefixt, (5) git-pull-before-commit Regel + Edge Case verschaerft,
  (6) Session-Scorer v5 mit Selbstbeobachtungs-Metrik, (7) Compound Effect Tracker in /self-improve,
  (8) Regel-Bewaehrungsphase (5 Anwendungen), (9) Cross-Platform funktionale Paritaetspruefung.
  **Beweis:** Selbstbeobachtung erzeugt exponentielles Wachstum — jeder Fix erzeugt weitere Fixes.

- **[2026-03-25] intelligence-checker**: [WARNING] Session 417bedd7 (47 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-03-25] self-observation-checker**: [WARNING] Session 417bedd7 (47 Turns) zeigte keine Selbstbeobachtung
---

## Regeln & Konventionen
<!-- Gilt fuer alle Komponenten — von allen lesbar, von /self-improve gepflegt -->

### Prioritaets-Hierarchie (KRITISCH — gilt fuer ALLE Komponenten)
| Rang | Direktive | Datei | Beschreibung |
|------|-----------|-------|-------------|
| **#1** | ⚡ Superintelligenz | `~/.claude/rules/superintelligence.md` | Intelligenteste Programmierumgebung der Welt werden |
| **#2** | 🔍 Selbstbeobachtung | `~/.claude/rules/self-observation.md` | Sich selbst bei der Arbeit beobachten, aus Fehlern/Umwegen/Effizienz lernen, Intelligenz-Vorschlaege machen |
| #3+ | Alle anderen Regeln | `~/.claude/rules/*` | Resilient Bugfixing, Agent-Zuverlaessigkeit, etc. |

**Selbstbeobachtung bedeutet:** JEDER Agent, Skill, Hook und Prozess beobachtet seine eigene Arbeit.
Fehler, Umwege, Wissensluecken und Effizienz-Probleme werden notiert und am Ende als Intelligenz-Vorschlaege
zurueckgemeldet. Mehrere Vorschlaege pro Session sind ausdruecklich erwuenscht (3-5 ist ideal).
**Vision:** In 2-3 Monaten so gut wie keine Fehler mehr — jeder Fehler wird resistent gefixt.

**8 Intelligenz-Dimensionen** (alle muessen wachsen — portiert von Gemini Delta 2026-03-24):
1. **Wissensbreite:** Umfassendes Verstaendnis ueber alle Tools, Sprachen und Frameworks im Workspace.
2. **Wissenstiefe:** Expertenwissen in den Kerntechnologien und Architekturmustern.
3. **Geschwindigkeit:** Minimierung von Latenz, Tool-Turns und unnoetiger Rediscovery.
4. **Qualitaet:** Fehlerfreie, robuste und perfekt idiomatisierte Code- und Setup-Aenderungen.
5. **Autonomie:** Proaktive Problemloesung und Entscheidungsfindung innerhalb der Leitplanken.
6. **Voraussicht:** Antizipation von Seiteneffekten, Regressionsrisiken und kuenftigem Bedarf.
7. **Kreativitaet:** Finden von eleganten, unkonventionellen Loesungen fuer komplexe Probleme.
8. **Meta-Intelligenz:** Faehigkeit zur Selbstbeobachtung und Verbesserung der eigenen Arbeitsweise.

**Workspace Orchestration** (portiert von Gemini Delta 2026-03-24):
- **Zentrales Whiteboard:** Das einzige operative Whiteboard ist `~/proggs/.claude/agent-memory/shared/MEMORY.md`.
- **Cross-Tool-Lernen:** Claude Code darf `codex-setup/` und `Gemini-Setup/` (inkl. deren Whiteboards) als **read-only Vergleichsquellen** lesen.
- **Direktiven-Schutz:** Die drei Haupt-Direktiven (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing) muessen in Whiteboard, CLAUDE.md und rules/ synchron gehalten werden.

- Kein Python fuer User-Interfaces
- Commit-Format: #NNN - Beschreibung (Englisch)
- Kommunikation: Deutsch, Code-Kommentare: Englisch
- quality-gate MUSS nach jedem Feature/Projekt laufen
- Fehler NIEMALS still verschlucken — immer ins Whiteboard loggen
- Neue Dateien/Strukturen: Pruefen ob Whiteboard-Eintrag noetig
- Einziges Repository: Pepsi1978/proggs
- Cross-Platform: Jede Aenderung MUSS auf beiden Plattformen funktionieren
- Status-Meldung: "Committed, gepusht und plattformuebergreifend" nur wenn ehrlich
- Writeback-Enforcer: Sentinel-Daten gehoeren in die thematisch passende Sektion, NICHT ans Dateiende
- GEFIXT-Eintraege archivieren: Nach 30 Tagen koennen GEFIXT-Eintraege in einen Archiv-Kommentar verschoben werden
- Alle Hooks die ins Whiteboard schreiben MUESSEN whiteboard-insert.ps1 (oder .sh Aequivalent) nutzen — Add-Content/appendFileSync ans Dateiende ist VERBOTEN
- Session-Scorer ist ein DATEN-SAMMLER — schreibt NUR in session-scores.jsonl, Analyse macht evolution-analyst
- StopFailure-Hook ist PFLICHT — loggt API-Fehler und Rate-Limits automatisch ins Whiteboard
- Forschungs-Status in "Forschung & Intelligence": UMZUSETZEN | EVALUIERT | VERWORFEN | BESTAETIGT (nicht OFFEN)
- **Compound Effect Dokumentation (PFLICHT)**: Wenn eine Verbesserung aus Selbstbeobachtung heraus entsteht (Fehler bemerkt → Vorschlag → Fix → Fehlerklasse eliminiert), MUSS die Kette in "Meta-Intelligenz & Selbstverbesserung > Compound Effect Erfolge" dokumentiert werden. /self-improve zaehlt und trackt diese als Metrik.
- **Direktiven-Integritaetspruefung**: /self-improve MUSS bei JEDEM Lauf pruefen ob Direktive #1 (Superintelligenz) und #2 (Selbstbeobachtung) in allen Speicherorten vorhanden sind
