# Failure Pattern Library

> Every mistake is a lesson. Never make the same mistake twice.
> Updated by: debugger, code-reviewer, tester, self-improve
> Read by: ALL agents before starting work

## How to Use This File

**Agents**: Before debugging or fixing a bug, search this file for similar symptoms.
**Self-Improve**: Analyze patterns here to create preventive hooks and rules.
**Format**: One entry per failure, with symptom, cause, fix, and prevention.

## Format Template

```
### [DATE] Category: Short Title
- **Symptom**: What the user saw or what failed
- **Root Cause**: The actual underlying problem
- **Fix**: What was done to resolve it
- **Prevention**: How to avoid this in the future (hook, rule, test)
- **Files**: Which files were involved
```

---

## Build & Compilation Failures

### [2026-03-19] Performance: Build-Latenzen werden nicht erfasst — strukturelle Luecke
- **Symptom**: Session-Scorer zeigt 0 errors, 0 corrections — aber tatsaechliche Build-Zeiten (Gradle, dotnet) sind nicht im Scoring. Performance-Probleme koennen "unsichtbar" sein, waehrend Quality-Score trotzdem 8.8 zeigt.
- **Root Cause**: Session-Scorer misst nur Turns, Tools, Errors, Corrections — keine Zeitstempel fuer Build-Operationen, kein Latenz-Tracking.
- **Fix**: Kein direkter Fix. Erfordert entweder (a) Build-Zeit-Tracking im Scorer oder (b) separaten Performance-Monitor-Hook.
- **Prevention**: Bei Performance-Analysen immer explizit nachfragen: "Wurden Build-Zeiten gemessen?" Wenn nein: als Datenlücke benennen, nicht als "keine Probleme" interpretieren.
- **Files**: ~/.claude/hooks/session-scorer.ts

## Logic & Runtime Errors

_No entries yet. Agents will add logic errors that recurred or were hard to find._

## Configuration & Environment Issues

### [2026-03-18] Config: Session-Scorer produced dummy data for 25 sessions
- **Symptom**: session-scores.jsonl had total_turns=0 and tool_calls=0 for 25/26 entries
- **Root Cause**: Transcript format mismatch — scorer expected `entry.message` but format varied across Claude Code versions
- **Fix**: Updated scorer to handle current transcript format (v5.3)
- **Prevention**: Added plausibility check in self-improve Stufe 0 — if total_turns=0 in 3+ of last 5 entries, flag as defect
- **Files**: ~/.claude/hooks/session-scorer.ts, session-scores.jsonl

### [2026-03-18] Config: effortLevel silently reset from high to medium
- **Symptom**: Claude Code was running at medium effort without user noticing
- **Root Cause**: `/model` command reset effortLevel as side effect
- **Fix**: config-guard.sh hook now blocks any effortLevel change away from "high"
- **Prevention**: ConfigChange + Stop hooks both monitor effortLevel
- **Files**: ~/.claude/hooks/config-guard.sh, settings.json

### [2026-03-18] Security: Command injection in notify.sh hook
- **Symptom**: Notification hook passed unsanitized input to osascript
- **Root Cause**: Tool name and result were interpolated directly into AppleScript string
- **Fix**: Sanitized all inputs by removing single quotes before passing to osascript
- **Prevention**: safety-gate.sh now checks for common injection patterns in hook scripts
- **Files**: ~/.claude/hooks/notify.sh

## Cross-Platform Issues

### [2026-03-19] Environment: PowerShell-Update waehrend laufender Optimierung zerstoert alle Terminals
- **Symptom**: Waehrend /self-improve lief, wurde PowerShell 7.5→7.6 aktualisiert. ALLE offenen PowerShell-Fenster des Benutzers wurden sofort geschlossen, stundenlange Arbeit unterbrochen.
- **Root Cause**: Shell-Updates ersetzen die laufende Executable (pwsh.exe). Windows kann laufende Prozesse nicht nahtlos migrieren wie Unix. Ein Update der Shell-Runtime killt alle Instanzen.
- **Fix**: Self-Improve Skill v5.8 — Shell/Terminal-Updates werden jetzt in "Deferred Shell Updates" gesammelt und ERST nach Abschluss aller anderen Aufgaben, nach dem Commit, als allerletzter Schritt ausgefuehrt. Benutzer muss vorher explizit bestaetigen.
- **Prevention**:
  - Core Rule im Self-Improve Skill (v5.8): Shell-Updates IMMER zuletzt
  - Benutzer-Bestaetigung PFLICHT vor Shell-Updates
  - Betroffene Tools-Liste: PowerShell, Git, Git Bash, Node.js, npm, Bun, Deno, Python, Claude Code CLI
  - Diese Regel gilt fuer ALLE Aufgaben, nicht nur /self-improve
- **Status**: ✅ DAUERHAFT (Regel in Skill, CLAUDE.md und Memory verankert)
- **Files**: ~/.claude/commands/self-improve.md, ~/CLAUDE.md, Memory/feedback_shell_updates_last.md

## Data & Content Quality

### [2026-03-18] Content: Sopranos Emmy count in quiz question was incorrect
- **Symptom**: Quiz question stated The Sopranos won a wrong number of Emmys
- **Root Cause**: AI-generated trivia not fact-checked before commit
- **Fix**: Corrected the answer in FilmQuestionsMedium3.kt
- **Prevention**: For trivia/factual content, always verify with web search before committing
- **Files**: QuizVerse/app/src/main/java/.../FilmQuestionsMedium3.kt

## Agent & Workflow Failures

### [2026-03-19] Agent: Alle 14 Agents hatten keine Absturz-Sicherung
- **Symptom**: Researcher (R2) stuerzte bei WebFetch von claudecodemarketplace.com ab. Weitere Agents (batch-reviewer, env-checker) hatten kein maxTurns und konnten endlos laufen. Kein Agent hatte Schutz gegen Kontext-Ueberlauf durch grosse Dateien/Diffs.
- **Root Cause**: Fehlende Robustness-Massnahmen in allen Agent-Definitionen: (1) kein maxTurns bei 3 Agents, (2) keine Tool-Fehler-Behandlung, (3) kein Kontext-Schutz, (4) keine Selbst-Terminierung, (5) Orchestratoren hatten keinen Sub-Agent-Failover
- **Fix**: Robustness Protocol zu allen 14 Agents hinzugefuegt — Tool-Fehler-Recovery, Kontext-Schutz (Dateien >500 Zeilen begrenzen, Diffs aufteilen, head_limit), Selbst-Terminierung nach 5 stalled Turns, Sub-Agent-Failover fuer Orchestratoren, Eingabe-Validierung
- **Prevention**: maxTurns jetzt Pflicht fuer jeden neuen Agent. Robustness Protocol ist Standard-Sektion die jeder Agent haben muss.
- **Files**: ~/.claude/agents/*.md (alle 14 Dateien)

### [2026-03-18] Agent: Shared Knowledge Hub empty after 12 days
- **Symptom**: No agent ever wrote to shared/MEMORY.md despite instructions
- **Root Cause**: Agent definitions say "update MEMORY.md" but don't enforce it — agents prioritize their main task and skip the write-back
- **Fix**: Added mandatory write-back sections with explicit triggers to all senior agent definitions
- **Prevention**: Mandatory write-back sections added to all 5 senior agent definitions. Technical verification via SubagentStop hook is NOT yet implemented — write-back relies on agent compliance.
- **Files**: ~/.claude/agents/code-reviewer.md, tester.md, architect.md, debugger.md

### [2026-03-18] Logic: Session-Scorer correction-detection zaehlt false positives
- **Symptom**: Fragen wie "was ist wrong damit?" oder Code-Snippets mit "error" im Text zaehlen als Benutzer-Korrekturen
- **Root Cause**: correctionPatterns matcht auf Woerter-Bag ohne Satzkontext — kein Unterschied zwischen Korrektur und zitiertem Fehlertext oder Fragen
- **Fix**: ✅ IMPLEMENTIERT (v2, 2026-03-18) — Match nur in ersten 80 Zeichen, kein Match wenn "?" oder Code-Block vorhanden (Zeilen 89-95)
- **Prevention**: Vor Aenderungen an correction-detection: manuelle Stichprobe von 10 echten User-Turns aus session-scores.jsonl durchfuehren
- **Files**: ~/.claude/hooks/session-scorer.ts (correctionPatterns, Zeilen 70-74)

### [2026-03-18] Config: Intent-Anker Turn-Counter wurde nie zurueckgesetzt
- **Symptom**: Session-Goal wurde in neuen Sessions nicht gespeichert, Intent-Drift-Schutz unwirksam
- **Root Cause**: /tmp/claude-turn-counter.txt ueberlebt Session-Ends — neue Sessions starten mit Counter > 0 statt 1
- **Fix**: ✅ session-cleanup.sh bereinigt jetzt turn-counter und intent-reminder bei SessionEnd
- **Prevention**: session-cleanup.sh loescht /tmp/claude-turn-counter.txt und /tmp/claude-intent-reminder.txt bei SessionEnd
- **Files**: ~/.claude/hooks/session-cleanup.sh, ~/.claude/hooks/intent-anker.sh

### [2026-03-19] Security: Prompt-Injection-Defender False Positive auf eigenem Hook-Code
- **Symptom**: Defender meldet HIGH SEVERITY bei Lesen von ~/.claude/hooks/session-scorer.ts — "Instruction hidden in code comment"
- **Root Cause**: JSDoc-Kommentare mit Spiegelstrich-Listen (z.B. `* - total_turns:`) werden als versteckte Anweisungen erkannt
- **Fix**: Kein Fix noetig fuer diesen Vorfall (False Positive, eigener Code)
- **Prevention**: Allowlist fuer vertrauenswuerdige Pfade im Defender konfigurieren: ~/.claude/hooks/, ~/.claude/agents/, ~/.claude/commands/. Echte Bedrohungen gehen sonst im Rauschen unter.
- **Files**: ~/.claude/hooks/prompt-injection-defender/post-tool-defender.py

### [2026-03-18] API: Session terminated by unknown (13:44:43)
- **Symptom**: Session ended unexpectedly due to API error
- **Root Cause**: unknown (auto-detected by StopFailure hook)
- **Fix**: Automatic retry on next session start
- **Prevention**: StopFailure hook monitors for recurring patterns
- **Context**: {}
- **Status**: AUTO-LOGGED (incomplete context — review manually if recurring)

### [2026-03-19] Security: Pfad-basierte Allowlist in Security-Scanners schafft ausnutzbaren Blind Spot
- **Symptom**: Prompt-Injection-Defender ueberspringt ~/.claude/hooks/ komplett — praeparierte Datei in diesem Pfad wird nicht mehr gescannt
- **Root Cause**: Pfad-Allowlist loest False-Positive-Problem, aber macht den Annahmen-Fehler, dass Pfad = Vertrauensquelle. Ein Angreifer der Schreibzugriff auf ~/.claude/ hat (z.B. via schadhafter MCP-Server) kann jede Datei dort platzieren.
- **Fix**: SHA-256-Hash-Whitelist statt Pfad-Allowlist. Bekannte gute Dateien per Hash registrieren. Bei Hash-Mismatch: Scan erzwingen. Bei neuen Dateien im Pfad: Scan erzwingen.
- **Prevention**: Regel: Niemals Security-Scanner per Pfad deaktivieren. Nur per kryptographischem Hash (SHA-256 minimum) oder per expliziter Inhalts-Signatur.
- **Files**: ~/.claude/hooks/prompt-injection-defender/post-tool-defender.py

### [2026-03-19] Config: Einzelmessungen als Ground Truth in MEMORY.md persistiert
- **Symptom**: Latenzwerte (z.B. "700ms", "300ms") werden als absolute Fakten in MEMORY.md gespeichert und von Agents als Ground Truth behandelt
- **Root Cause**: Messwerte haengen von Netzwerk, RAM-Auslastung, Tageszeit und parallelen Prozessen ab. n=1 Messungen koennen 10x vom Median abweichen.
- **Fix**: Kein Messwert ohne Messkontext (Datum, Netzwerktyp, Systemlast) in MEMORY.md schreiben. Werte erst nach 5+ Messungen als Durchschnitt eintragen.
- **Prevention**: Vor MEMORY.md-Eintrag mit Latenzwert: Immer "(n=X, Median: Y, Range: Z-W)" erwaehnen.
- **Files**: .claude/agent-memory/shared/MEMORY.md

### [2026-03-19] Design: Prompt-Hook als Enforcement-Mechanismus — strukturelles Anti-Pattern
- **Symptom**: SubagentStop-Prompt-Hook soll pruefen ob MEMORY.md beschrieben wurde — hat aber keinen Dateisystem-Zugriff
- **Root Cause**: Prompt-Hooks sind Instruktionen im Agent-Kontext, keine ausfuehrbaren Scripts. Compliance != Enforcement. Bekanntes Muster: Mandatory-Write-Back gilt seit 18.03., KEIN Agent hat je etwas geschrieben (MEMORY.md Zeile 76+99).
- **Fix**: Enforcement NUR per echtem Script-Hook (PowerShell PostToolUse auf Task-Events) der MEMORY.md per mtime prueft und ggf. Platzhalter injiziert.
- **Prevention**: Wenn "Enforcement" durch einen Prompt umgesetzt werden soll: IMMER als CONCERN markieren. Echter Enforcement = Dateisystem-Operation, nicht Prompt-Text.
- **Files**: .claude/agent-memory/shared/MEMORY.md, SubagentStop-Hook

### [2026-03-19] Metrics: Scoring-Schwellenwerte ohne Baseline-Kalibrierung — False-Incentive
- **Symptom**: Parallelisierungs-Bonus ab Ratio 1.5 eingebaut, obwohl kein einziger realer Run diese Ratio je erreicht hat (Max 1.27, Schnitt 0.88). Malus ab < 0.7 trifft reale Sessions.
- **Root Cause**: Schwellenwerte wurden normativ gesetzt statt empirisch aus bestehenden Session-Daten abgeleitet.
- **Fix**: Bonus-Schwellenwert = Baseline + 20%. Malus-Schwellenwert = Baseline - 30%. Beide alle 20 Sessions automatisch neu kalibrieren (analog Two-Phase-SPC).
- **Prevention**: Vor jeder Metrik-Aenderung zuerst session-scores.jsonl lesen und Ist-Verteilung pruefen. Schwellenwerte muessen erreichbar sein, sonst demotivieren sie statt zu incentivieren.
- **Files**: ~/.claude/hooks/session-scorer.ts, session-scores.jsonl

### [2026-03-19 16:31] Agent: Write-Back nicht erfolgt (automatisch erkannt)
- **Symptom**: Senior-Agent hat MEMORY.md nicht aktualisiert
- **Root Cause**: Mandatory Write-Back wird von Agents ignoriert — kein technisches Enforcement
- **Fix**: Memory Watchdog Hook meldet fehlende Write-Backs
- **Prevention**: Dieser Hook (memory-watchdog.ps1) laeuft bei jedem SubagentStop
- **Status**: AUTO-LOGGED

### [2026-03-19 16:31] Agent: Write-Back nicht erfolgt (automatisch erkannt)
- **Symptom**: Senior-Agent hat MEMORY.md nicht aktualisiert
- **Root Cause**: Mandatory Write-Back wird von Agents ignoriert — kein technisches Enforcement
- **Fix**: Memory Watchdog Hook meldet fehlende Write-Backs
- **Prevention**: Dieser Hook (memory-watchdog.ps1) laeuft bei jedem SubagentStop
- **Status**: AUTO-LOGGED

### [2026-03-19 16:32] Agent: Write-Back nicht erfolgt (automatisch erkannt)
- **Symptom**: Senior-Agent hat MEMORY.md nicht aktualisiert
- **Root Cause**: Mandatory Write-Back wird von Agents ignoriert — kein technisches Enforcement
- **Fix**: Memory Watchdog Hook meldet fehlende Write-Backs
- **Prevention**: Dieser Hook (memory-watchdog.ps1) laeuft bei jedem SubagentStop
- **Status**: AUTO-LOGGED
