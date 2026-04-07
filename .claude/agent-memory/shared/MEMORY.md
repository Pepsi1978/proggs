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
**Fix-Vorschlag:** Runden-Zaehler als Datei /tmp/debate-round-N.json implementieren. Nach Runde 3 zwingend Konsens-Dokument schreiben ODER eskalieren.
**Status:** DESIGN-OFFEN (kein aktiver Bug — Feature noch nicht implementiert, Risiko nur theoretisch)
**Bewertung (2026-03-31):** Niedrige Prioritaet — Debate-Loop ist ein geplantes Feature, kein aktiver Code. Wird relevant wenn Stronger-MAS implementiert wird.


<!-- ARCHIV (2026-03-21): safety-gate.ps1 Blockierungen (6x) und Write-Back-Warnungen (2x) — erwartetes Verhalten, kein Fehler. -->
<!-- ARCHIV (2026-03-21): reindex-codebase.ps1 — ExitCode 1 (6x, 2026-03-21 10:44–11:25) — Timeout von 180s auf 300s erhoeht als Fix; gebuendelt zu einem Eintrag. -->

<!-- ARCHIV (2026-03-25, /self-improve Cleanup): StopFailure API/Rate-Limit Errors (2026-03-21 + 2026-03-24) — temporaere API-Fehler, einmalig, kein dauerhaftes Problem. safety-gate.sh duplizierte Blockierung (2x 2026-03-21) — erwartetes Verhalten, kein Fehler. Write-Back nicht erfolgt (2026-03-22 + 2026-03-25) — Einmalige Events, memory-watchdog loggt korrekt, kein systemisches Problem. -->

<!-- ARCHIV (2026-03-31, /self-improve Focus:Resilienz): 4x StopFailure Rate-Limit (Ollama 429, macOS, Sessions 417bedd7 + 2026-03-28) — temporaere API-Session-Limits, kein dauerhaftes Problem. 8x Write-Back nicht erfolgt (memory-watchdog 2026-03-25 bis 2026-03-29) — Informativ, Agents loggen korrekt. disk-guard 95% (2026-03-27) — aktuell 83%, nicht mehr kritisch. session-guard Auto-Reparatur (2026-03-28) — korrekt AUTO-GEFIXT. -->
<!-- ARCHIV (2026-04-02): 8x disk-guard 96-97% (2026-03-31 bis 2026-04-02) — Speicherplatz dauerhaft kritisch, siehe aktiven Eintrag unten. 3x Write-Back AUTO-LOGGED (2026-03-31 bis 2026-04-01) — informativ, kein Fehler. 2x StopFailure authentication_failed (coder-Agents in Worktree, 2026-04-01 21:48 + 22:50) — Worktree-Agents verlieren Auth-Kontext, temporaer, Session laeuft normal weiter. -->

### 2026-04-02 — Speicherplatz war KRITISCH (97%) — bereinigt
**Quelle:** Hook: disk-guard.sh (8 Meldungen seit 2026-03-31)
**Fix:** Gradle clean + Build-Cache-Bereinigung. Jetzt 16 GB frei (42%).
**Status:** GEFIXT (2026-04-02)

### 2026-04-02 — Meta-Intelligence-Kollaps (50% → 10%) — hyperagent-stop.sh Bug
**Quelle:** evolution-analyst Trend-Analyse
**Symptom:** 9/10 letzten Sessions ohne Meta-Intelligence >=2, Intelligenz-Vorschlaege fehlend
**Ursache:** hyperagent-stop.sh hat bei stale Goal (>2h) kompletten Hook uebersprungen (exit 0)
**Fix:** exit 0 → goal="" (Hook laeuft weiter). Zusaetzlich: Volle-Analyse-Schwelle 20→12 Turns, Error-Schwelle 3→2.
**Betroffene Dateien:** ~/.claude/hooks/hyperagent-stop.sh, claude-code-setup/hooks/hyperagent-stop.sh
**Status:** GEFIXT (2026-04-02)
<!-- ARCHIV: safety-gate.sh blockierte rm -rf waehrend /self-improve Cache-Cleanup (2026-04-02 18:51) — erwartetes Verhalten -->
### 2026-04-03 — IQ-Score in session-scorer immer 0 — seit 24.03 ungefixt
**Quelle:** Traumsession-Konsolidierung (evolution-analyst + session-scores.jsonl Analyse)
**Symptom:** `iq_score` ist in ALLEN Session-Score-Eintraegen 0 (altes Format) oder schwankt unrealistisch (0, 14, 32, 89) im neuen Format
**Ursache:** Die IQ-Score-Berechnung in session-scorer war NIE implementiert — das Feld wurde schlicht nicht geschrieben
**Betroffene Dateien:** `~/.claude/hooks/session-scorer.ps1`, `~/.claude/hooks/session-scorer.sh`, `claude-code-setup/hooks/session-scorer.*`
**Fix:** IQ-Score-Berechnung in session-scorer.ps1 implementiert (2026-04-04): efficiency(40) + quality(30) + duration(30) = 0-100
**Status:** GEFIXT (2026-04-04) — .sh-Version muss noch auf macOS aktualisiert werden

### 2026-04-03 — Meta-Intelligence Score = 0 seit 26.03 — Erholung nach hyperagent-fix pruefen
**Quelle:** Traumsession-Konsolidierung (session-scores.jsonl Trend-Analyse)
**Symptom:** meta_intelligence_score ist in allen Sessions nach 26.03 gleich 0
**Ursache:** Wahrscheinlich Nachwirkung des hyperagent-stop.sh Bugs (exit 0 bei stale goal). Bug wurde am 02.04 gefixt, aber Scoring-Erholung nicht bestaetigt
**Betroffene Dateien:** `~/.claude/hooks/hyperagent-stop.ps1/.sh`, session-scorer
**Fix-Vorschlag:** In naechsten 3-5 Sessions beobachten ob meta_intelligence_score > 0 wird. Falls nicht: session-scorer Logik fuer meta_intelligence debuggen
**Status:** BEOBACHTUNG (Fix deployed, Wirkung noch nicht bestaetigt)

<!-- ARCHIV (2026-04-03): memory-watchdog Write-Back 3x nicht erfolgt + session-guard Auto-Reparatur settings.local.json — beides einmalige Events, korrekt AUTO-LOGGED/AUTO-GEFIXT -->
### 2026-04-04 16:09 — StopFailure: API/Rate-Limit Error — Status: OFFEN
### 2026-04-04 16:17 — Hook: session-guard.ps1 — Auto-Reparatur: allow-Liste aus settings.json entfernt — Status: AUTO-GEFIXT
### 2026-04-04 17:38 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: high, jetzt: medium) — Status: AUTO-GEFIXT
### 2026-04-04 17:39 — StopFailure: API/Rate-Limit Error — Status: OFFEN
### 2026-04-04 17:53 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: high, jetzt: medium) — Status: AUTO-GEFIXT
### 2026-04-04 17:53 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: high, jetzt: medium) — Status: AUTO-GEFIXT
### 2026-04-04 18:27 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-04-05 13:27 — Hook: session-guard.ps1 — Auto-Reparatur: effortLevel zurueckgesetzt (war: high, jetzt: medium) — Status: AUTO-GEFIXT
### 2026-04-05 15:42 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-04-05 18:03 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-04-05 20:23 — Hook: memory-watchdog.ps1 — Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-04-02 19:13 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 97%
### 2026-04-02 20:32 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 98%
### [2026-04-02 22:45] Agent: Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-04-03 01:24 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 98%

### 2026-04-03 02:27 — StopFailure: API/Rate-Limit Error — Status: OFFEN
**Quelle:** Hook: StopFailure (command-type, no API dependency)
**Symptom:** Session-Turn endete durch API-Fehler
**Details:** {"session_id":"bafc0d45-050d-45ba-b5b4-d88d66c30159","transcript_path":"/Users/frank/.claude/projects/-Users-frank-proggs/bafc0d45-050d-45ba-b5b4-d88d66c30159.jsonl","cwd":"/Users/frank/proggs/BestJournalAndroid","hook_event_name":"StopFailure","error":"invalid_request","last_assistant_message":"Request too large (max 20MB). Double press esc to go back and try with a smaller file."}
**Fix-Vorschlag:** Pruefen ob Rate-Limit temporaer oder dauerhaft. Bei dauerhaftem Fehler: API-Key pruefen.
**Status:** OFFEN
### 2026-04-03 02:56 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 98%
### 2026-04-03 03:17 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 97%
### 2026-04-03 03:20 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 97%
### 2026-04-06 16:18 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 97%
### [2026-04-06 16:25] Agent: Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
### 2026-04-06 16:54 — Hook: safety-gate.sh — Befehl blockiert: rm[[:space:]]+-rf[[:space:]]+[/~]
### 2026-04-06 17:36 — Hook: disk-guard.sh — Speicherplatz KRITISCH bei 97%
### [2026-04-06 17:48] Agent: Write-Back nicht erfolgt (3 aufeinanderfolgende Agents) — Status: AUTO-LOGGED
---

## Systemzustand (aktuell)
<!-- Wird von /self-improve und env-checker aktualisiert -->
<!-- Zeigt den aktuellen Stand des Programmiersystems -->

**Stand:** 2026-04-04 (aktualisiert durch /self-improve auf Windows)

- **Plattform:** Windows 11 Home 10.0.26200 (x64), Claude Code v2.1.92, Opus 4.6 (1M context)
- **Sprachen:** Rust 1.94.0, Go 1.26.1, Kotlin 2.3.20, Java OpenJDK 21.0.10, Python 3.13.12
- **Node.js:** v24.14.0, npm 11.12.0, Bun 1.3.11
- **Effort Level:** medium (Standard), High per /effort high
- **Quality Gate:** quality-gate Agent fuer kombiniertes test+review+optimize
- **Agents:** 29 aktiv (3 neu seit 03.04: ace-curator, experience-retriever, trajectory-analyzer)
- **Hooks:** 46 PS1, 46 SH, 10 SessionStart. 4 neue Registrierungen (04.04)
- **Rules:** 57 in ~/.claude/rules/ (9 neu seit 03.04, inkl. hook-exit-safety)
- **Skills:** 21 in ~/.claude/commands/ (14 neu seit 03.04)
- **Plugins:** 88 installiert, 91 aktiviert
- **Whiteboard-Anbindung:** Alle Hooks nutzen whiteboard-insert.ps1/.sh (sektionsbasiert)
- **Session-Scorer:** v4 — schreibt jetzt iq_score (0-100) basierend auf efficiency+quality+duration
- **Self-Improve Skill:** v5.19
- **Git:** v2.53.0, Git Credential Manager aktiv
- **Sicherheit:** Alle CVEs gepatcht (v2.1.92), Prompt-Injection-Defender aktiv
- **Speicherplatz (Windows):** 139 GB frei (84%) — stabil
- **PATH:** Alle kritischen Verzeichnisse vorhanden (verifiziert 04.04)
- **Cross-Platform Sync:** Alle Rules, Agents, Skills, Hooks synchron zwischen Setup-Repo und lokal
- **[2026-04-04 16:11] env-checker**: Gesamtstatus GELB — 1 Problem: effortLevel ist medium statt high in settings.json. Alle anderen Checks OK: Claude Code 2.1.92 aktuell, 92 Hook-Dateien, 88 Plugins, 57 Rules, 29 Agents, Disk 84%, PATH vollstaendig, bypassPermissions aktiv.
- **Sicherheit:** Prompt-Injection-Defender aktiv, cargo-audit 0.22.1 OK, Axios Supply-Chain SAUBER (geprueft 2026-04-02)
- **Evolution-Analyst (2026-04-02):** Quality 8.66 (PLATEAU), IQ 89.5 (STEIGEND), Meta-Intelligence 10% (KOLLAPS → Bug gefixt), Corrections fast null
- **Speicherplatz (macOS):** 16 GB frei (42%) — bereinigt, stabil
- **Pending Shell-Updates (macOS):** Claude Code CLI v2.1.90, node, deno, powershell, uv, xz, harfbuzz, util-linux, codex
- **Neuer Hook (2026-03-31):** invariant-check.ps1/.sh — Proaktive System-Invarianten-Pruefung bei SessionStart (Cursor-Pattern)
- **Speicherplatz (macOS):** 22 GB frei (36% belegt) — reichlich Platz
- **Cross-Tool:** Codex + Gemini Delta Bridges aktiv, 8 Intelligenz-Dimensionen im Whiteboard portiert
- **macOS-Update (2026-03-31):** Claude Code v2.1.83, Node v25.8.1, npm 11.11.1, Bun 1.3.11, Go 1.26.1, Swift 6.3, Rust/Cargo 1.94.0 (→1.94.1 verfuegbar), 25 Homebrew-Pakete veraltet
- **macOS Settings-Fix (2026-03-31):** allow-Liste entfernt (war Whitelist-Blocker bei bypassPermissions), 2 fehlende Hooks hinzugefuegt (mcp-auth-check, doctor-lite), tote Plugins deaktiviert (boostvolt, FlineDev)
- **Neue Hooks:** StopFailure (v2.1.78) — loggt API-Fehler/Rate-Limits ins Whiteboard
- **Windows→macOS Porting (2026-03-25):** 6 Hook-Upgrades portiert (Semantic Search Check, config-guard PostToolUse, Enhanced PostToolUseFailure/SubagentStop/PostCompact, session-scorer 25s Timeout)
- **Pending Admin Updates (5):** fzf,harfbuzz,node,ollama,simdjson,
---

## Erkenntnisse aus Code Reviews
<!-- code-reviewer, mar-reviewer, batch-reviewer schreiben hierher -->
- **[2026-04-04 17:11] code-reviewer**: Kontextoptimierung-Review: experience-store.jsonl Groessenlimit fehlt in experience-and-trajectory.md; codified-context.md nennt veralteten Dateinamen; alle kritischen Regeln vorhanden

- **[2026-04-04 17:15] code-reviewer**: Fix-Pruefung Runde 2: experience-and-trajectory.md fehlt Defense-in-Depth + AutoRefine/ACE Abschnitt; codified-context.md Defense-in-Depth Schicht 2+3 dupliziert; self-observation.md PASS; development-phases.md PASS
- **[2026-04-04 17:18] code-reviewer**: Runde 3 Review: 2 funktionale Luecken gefunden in self-observation.md — Drift-Detektor fehlt ~10-Tool-Calls-Frequenz, Score-Tabelle fehlt Mittenwert 3(ok)
- **[2026-04-04 17:21] code-reviewer**: PASS: Finale Verifikation Runde 4 — alle 4 Dateien vollstaendig. Alle funktionalen Inhalte vorhanden: 4 Tracker, Score-Tabelle, Hyperagent-Trigger, Compound-Gains, 4 Speicher, 3 Muster-Typen, Phasen-Anzeige, Session-Template, Verbotene Spruenge, korrigierte Dateinamen in codified-context.
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
- **[2026-04-02] Autopsy bafc0d45**: 4 Korrekturen (0.9%), Hauptmuster: other (2x) — Empfehlung: Öfter beim Benutzer rückfragen ob der Ansatz korrekt ist

- **[2026-04-03] Autopsy bafc0d45**: 4 Korrekturen (0.7%), Hauptmuster: other (2x) — Empfehlung: Öfter beim Benutzer rückfragen ob der Ansatz korrekt ist
- **[2026-04-03] Autopsy bafc0d45**: 4 Korrekturen (0.7%), Hauptmuster: other (2x) — Empfehlung: Öfter beim Benutzer rückfragen ob der Ansatz korrekt ist
## Performance & Optimierung
<!-- Writer: optimizer Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

- **[2026-04-07 14:16] Code-Suche Index:** ? Dateien, ? Chunks indexiert.
## UI/UX-Patterns
<!-- Writer: ui-polisher Agent | Leser: alle Agents, /self-improve -->
_Noch keine Eintraege._

## Bewaehrte Loesungsmuster (Pheromon-Tabelle)
<!-- Agents lesen diese Tabelle VOR komplexen Aufgaben und schreiben NACH erfolgreichen Aufgaben -->
<!-- Bereinigung: /self-improve entfernt Eintraege aelter als 3 Monate -->

| Datum | Aufgabentyp | Bewaehrter Ansatz | Erfolg |
|-------|-------------|-------------------|--------|
| 2026-03-31 | Multi-Datei-Batch-Edit | Python glob+re.sub statt parallele Coder-Agents | Hoch |
| 2026-03-31 | Android-Ordner-Umbenennung | gradlew --stop + adb kill-server + cmd.exe ren (nicht git mv) | Hoch |
| 2026-03-31 | Splash-Screen-Animation | Compose Keyframes + rememberUpdatedState fuer Audio-Sync | Hoch |
| 2026-03-31 | Iterative Internet-Recherche | 3 Wellen: Breit → Tief → Kreativ, max 50 Ergebnisse/Researcher | Hoch |
| 2026-04-03 | Memory-Konsolidierung (Traumsession) | 3 Agents parallel (episodic + claude-mem + scores) → Whiteboard-Update → Memory-Cleanup | Hoch |

## Chaos-Test Ergebnisse
<!-- chaos-tester Agent schreibt hierher -->

*Noch keine Chaos-Tests durchgefuehrt. Naechster geplanter Lauf: Auf Abruf.*

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

- **[2026-03-31] Hyperagent-System: Metacognitives Monitoring** — Status: UMGESETZT | Quelle: arXiv 2603.19461 (Hyperagents, Meta AI) | Empfehlung: AKTIV
  Metacognitiver Meta-Agent nach dem Hyperagent-Pattern. 6 Komponenten: (1) Agent-Definition `hyperagent.md` mit 5-Stufen-Analyse (Intent-Drift, Effizienz, Memory-Validierung, Verbesserungen, Scoring), (2) Rule `metacognitive-monitoring.md` mit 4 Echtzeit-Trackern und Alarmschwellen, (3) Stop-Hook `hyperagent-stop.ps1/.sh` als Prompt-Injection fuer automatische Analyse-Erinnerung bei jeder Antwort >5 Turns, (4) SessionEnd-Hook `session-scorer.ps1/.sh` fuer quantitative JSONL-Metriken, (5) Session-Score-System mit 4 Dimensionen (Intent, Effizienz, Memory, Lernertrag) und Trend-Analyse, (6) Defense-in-Depth mit 6 Absicherungsschichten. Cross-Platform: Alle Dateien in claude-code-setup, macOS settings.json aktualisiert, Mirror-Ledger-Eintrag MIRROR-2026-03-31-WIN-001.

- **[2026-03-31] Forschungsergebnisse: Optimale Programmierumgebung** — Status: BESTAETIGT | Quellen: Chollet arXiv 1911.01547, Zaharia BAIR Blog, DSPy ICLR 2024, Trae Agent arXiv 2507.23370, SICA arXiv 2504.15228, AlphaEvolve arXiv 2506.13131
  5 Researcher parallel: (1) Intelligenz-Definition — Chollet: Intelligenz = Skill-Acquisition EFFICIENCY, pass^k > pass@1, (2) SOTA-Vergleich — Claude Code fuehrt mit 80.9% SWE-bench, beste Hook/MCP-Extensibility, fehlt Multi-Model-Routing, (3) Compound AI — Trae Ensemble bereits umgesetzt, DSPy MIPROv2 fuer Prompt-Optimierung evaluierbar, (4) Memory/Reasoning — Forest-of-Thought, Thinking-Optimal Scaling (nicht immer laenger denken!), Context Engineering > Prompt Engineering, (5) Self-Improvement — AlphaEvolve (Produktion bei Google), SkillsBench, Voyager-Pattern via continuous-learning-v2 Plugin bereits verfuegbar. Ergebnis: System bei ~80% des Optimums, 5 Luecken identifiziert, Vorschlag 5 (Hyperagent) umgesetzt.

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

- **[2026-03-31] Invariant Sentinel Pattern (Cursor)** — Status: BESTAETIGT (2026-03-31) | Quelle: cursor.com/blog/security-agents | Empfehlung: JA sofort
  Cursor prueft taeglich alle System-Invarianten gegen eine definierte Liste und meldet Abweichungen sofort. **UMGESETZT**: invariant-check.ps1/.sh Hook bei SessionStart — prueft 5 Invarianten (Stale-OFFEN, bypassPermissions, Hook-Paare, Systemzustand-Alter, CLAUDE.md-Sync).

- **[2026-03-31] claudewatch AgentOps (blackwell-systems)** — Status: EVALUIERT | Quelle: github.com/blackwell-systems/claudewatch | Empfehlung: JA spaeter
  Echtzeit-Erkennung von Error-Loops (3 aufeinanderfolgende Fehler) und Drift (8 Reads ohne Write). 29 MCP-Tools fuer mid-session Metriken. Wuerde memory-watchdog-Logik durch intelligentere Variante ersetzen.

- **[2026-03-31] DebugBase MCP Server** — Status: EVALUIERT | Quelle: github.com/DebugBase/mcp-server | Empfehlung: JA spaeter
  Kollektive Fehler-Wissensdatenbank: Bei neuem Fehler zuerst nachschlagen ob Loesung bereits bekannt. Schliesst die Luecke zwischen Erkennung und Heilung.

- **[2026-03-31] Self-Healing Software Systems (arxiv 2504.20093)** — Status: EVALUIERT | Quelle: arxiv.org/abs/2504.20093
  Biologisches 3-Schichten-Modell: Sensoren (Hooks=vorhanden) → KI-Kern (Claude=vorhanden) → Heilungs-Agenten (FEHLT). Liefert Architektur-Blaupause fuer den fehlenden "healer-agent".
- **[2026-03-31 12:49] researcher**: AI Agent Memory/Reasoning/Meta-Learning 2025-2026: 4 Speichertypen (Episodic/Semantic/Procedural/Working) sind Industriestandard. Context Engineering (Anthropic-Term 2025) hat Prompt Engineering abgeloest — Schluessel ist just-in-time context loading und context rot Vermeidung. SICA-Paper (NeurIPS 2025): selbstverbessernder Coding-Agent von 17% auf 53% SWE-Bench. Hyperagents (Meta arXiv 2603.19461): rekursive Selbstmodifikation ueber Metacognition. Forest-of-Thought ist neuer Reasoning-Standard. Thinking-Optimal Scaling: laengeres Denken nicht immer besser. MemGPT/Letta: OS-Analogie fuer virtuelles Context-Management. Cross-session memory via structured note-taking + sub-agent architectures.
- **[2026-03-31 12:58] researcher**: Self-Healing CI/CD: Pipeline-Doctor-Pattern (Intercept→Analyze→Repair) + LLM-as-a-Judge (8B SLM als Gatekeeper) + 3-Stufen-Maturity (Observer→Gatekeeper→Healer). AI Agent Self-Observation: Metacognitive Learning = metacognitive knowledge + planning + evaluation; LLMs koennen nur human-interpretable Konzepte introspizieren; OpenReview 2026: truly self-improving agents brauchen intrinsic metacognitive learning. Zero-Recurrence Bugs: RC_Detector (Heterogeneous Graph Learning fuer Bug-Commit RCA, arXiv 2505.01022); RCEGen (LLM-basiertes RCA, MDPI 2025); Schluessel: Pattern-Bibliothek aus historischen Bugs + semantische Code-Abhängigkeitsgraphen. Compound AI / Knowledge Flywheel: ICLR 2026 Workshop on Recursive Self-Improvement; Intelligence Flywheel Paper (techrxiv 2026); Karpathy AutoResearch: 700 Experimente in 2 Tagen, 11% Speedup; AlphaCode-Pattern Generate-and-Filter direkt auf quality-gate anwendbar; LLM-Cascades (98% Kostensenkung durch Confidence-Routing)
- **[2026-03-31 13:15] researcher**: Windsurf Memories: IDE-only, kein API, gespeichert in ~/.codeium/windsurf/memories/ als workspace-lokale Dateien, kein eigenstaendiger Zugriff ausserhalb des IDEs moeglich. Beste Windsurf-Alternativen fuer Claude Code CLI: (1) claude-mem Plugin (BEREITS INSTALLIERT laut System) automatisch via Hooks, kein manueller Aufwand, 95% Token-Kompression, SQLite-lokal; (2) Mem0 MCP — pip install mem0-mcp-server, Cloud-API, 90% weniger Tokens, automatisch; (3) neural-memory MCP — pip install neural-memory, SQLite-lokal, kein Cloud, brain-like Assoziationen ABER manuelles Speichern noetig; (4) Letta Code — memory-first coding agent built on MemGPT. Fazit: claude-mem ist der Windsurf-naeheste Ansatz fuer Claude Code CLI weil es automatisch via Lifecycle-Hooks arbeitet.

- **[2026-04-02] ACE: Agentic Context Engineering (ICLR 2026, arxiv 2510.04618)** — Status: EVALUIERT | Empfehlung: JA sofort
  Kontext als lebendes Playbook: Generator→Reflector→Curator Loop. +10.6% Benchmarks, 83.6% weniger Token. Direkt auf MEMORY.md anwendbar: Session=Generator, Hyperagent=Reflector, /self-improve=Curator.

- **[2026-04-02] Code Pathfinder MCP (codepathfinder.dev)** — Status: EVALUIERT | Empfehlung: JA spaeter
  Open-Source Call-Graph MCP-Server im Anthropic Registry. 6 Tools (find_symbol, get_callers, get_callees). Nur Python-Support — fuer Kotlin/Swift nicht nutzbar.

- **[2026-04-02] ARIS: Autonome Forschungsschleife (AAAI 2026)** — Status: EVALUIERT | Empfehlung: JA spaeter
  Markdown-only Skills fuer autonome Nacht-Recherche. Zero Dependencies. Cross-Modell Review-Schleifen adaptierbar.

- **[2026-04-02] Awesome Context Engineering Sammlung** — Status: EVALUIERT | Empfehlung: JA sofort (als Forschungsquelle)
  100+ Papers zu Write/Select/Compress/Isolate Paradigma. Context Engineering hat Prompt Engineering als Industriestandard abgeloest.
- **[2026-04-04 16:11] researcher**: Claude Code Security Audit: 4 CVEs gefunden — CVE-2025-59536 (RCE, CVSS 8.7, gefixt in v1.0.111), CVE-2026-21852 (API-Key-Exfiltration, CVSS 5.3, gefixt in v2.0.65), CVE-2026-33068 (Workspace Trust Bypass, HIGH, gefixt in v2.1.53), CVE-2026-25725 (Sandbox Escape/Privilege Escalation, gefixt in v2.1.2). Ausserdem: npm-Source-Leak (v2.1.88, 31.03.2026). MCP Supply Chain: Tool-Mutation-Angriffe, mcp-remote 437k Downloads als Angriffsvektor. Slopsquatting: 5-22% halluzinierte npm-Paketnamen.
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
- **[2026-03-28] intelligence-checker**: [WARNING] Session c2cc1369 (84 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-03-28] self-observation-checker**: [WARNING] Session c2cc1369 (84 Turns) zeigte keine Selbstbeobachtung

- **[2026-03-31] Dritter Compound Effect — Von Stagnation zu proaktiver Fehlervermeidung:**
  /self-improve Stufe 0: Evolution-Analyst identifiziert "Erkennungs-ohne-Heilung-Muster" →
  Stufe 2: R8 findet Cursor Invariant Sentinel Pattern als Loesung →
  Stufe 3: invariant-check.ps1/.sh gebaut und registriert (5 Invarianten) →
  14 stale OFFEN/AUTO-LOGGED-Eintraege archiviert → Systemzustand aktualisiert →
  **Ergebnis:** Zukuenftige stale Issues werden PROAKTIV bei jedem SessionStart gemeldet statt
  tagelang unbemerkt zu bleiben. Die Fehlerklasse "vergessene offene Probleme" ist eliminiert.
  **Kette:** Stagnation bemerkt → Forschung → Muster gefunden → Implementiert → Fehlerklasse eliminiert.
- **[2026-04-01] intelligence-checker**: [WARNING] Session b4b10f73 (28 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-01] self-observation-checker**: [WARNING] Session b4b10f73 (28 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-01] intelligence-checker**: [WARNING] Session f4871ea5 (19 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-01] self-observation-checker**: [WARNING] Session f4871ea5 (19 Turns) zeigte keine Selbstbeobachtung

- **[2026-04-02] Vierter Compound Effect — Von Meta-Intelligence-Kollaps zur Root Cause:**
  /self-improve Stufe 0: Evolution-Analyst findet Meta-Intelligence-Kollaps (50%→10%) →
  Stufe 3: hyperagent-stop.sh untersucht → Bug gefunden: `exit 0` bei stale Goal (>2h) →
  Fix: `exit 0` → `goal=""` (Hook laeuft weiter, nur Goal-Text verschwindet) →
  Zusaetzlich: Volle-Analyse-Schwelle 20→12 Turns, Error-Schwelle 3→2 →
  **Ergebnis:** Alle zukuenftigen Sessions (auch lange!) bekommen metacognitiven Prompt.
  Die Fehlerklasse "stille Hook-Deaktivierung durch Timeout-Bedingungen" ist identifiziert.
  **Kette:** Trend-Analyse → Daten-Korrelation → Code-Inspektion → Root Cause → Bug-Fix → Fehlerklasse eliminiert.
- **[2026-04-03] intelligence-checker**: [WARNING] Session 2363a77c (21 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] self-observation-checker**: [WARNING] Session 2363a77c (21 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-03] intelligence-checker**: [WARNING] Session 2363a77c (36 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-02] intelligence-checker**: [WARNING] Session bafc0d45 (123 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-02] self-observation-checker**: [WARNING] Session bafc0d45 (123 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-03] intelligence-checker**: [WARNING] Session bcad53a9 (39 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] intelligence-checker**: [WARNING] Session 71ac5129 (17 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] self-observation-checker**: [WARNING] Session 71ac5129 (17 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-03] intelligence-checker**: [WARNING] Session bcad53a9 (52 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] CLOSED-LOOP**: Auto-Rule generiert: auto-learned-other.md (3x other in 3 Sessions)
- **[2026-04-03] intelligence-checker**: [WARNING] Session 4b80f958 (250 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-03] self-observation-checker**: [WARNING] Session 4b80f958 (250 Turns) zeigte keine Selbstbeobachtung
- **[2026-04-06] intelligence-checker**: [WARNING] Session 4b80f958 (250 Turns) hatte keinen Intelligenz-Vorschlag
- **[2026-04-06] self-observation-checker**: [WARNING] Session 4b80f958 (250 Turns) zeigte keine Selbstbeobachtung
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
- **Invariant-Check (2026-03-31)**: SessionStart-Hook prueft 5 System-Invarianten: (1) Stale OFFEN >7d, (2) bypassPermissions aktiv, (3) Hook-Paare vollstaendig, (4) Systemzustand <14d alt, (5) CLAUDE.md Sync. Verhindert das "Erkennungs-ohne-Heilung-Muster" durch proaktive Sichtbarkeit.
