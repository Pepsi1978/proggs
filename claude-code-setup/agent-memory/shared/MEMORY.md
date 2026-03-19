# Shared Knowledge Hub

> Central whiteboard for all senior agents. Everyone reads, everyone writes.
> Updated by: code-reviewer, tester, architect, debugger, optimizer, self-improve

## From Code-Reviewer
_No entries yet. The code-reviewer will write recurring patterns and conventions here._

## From Tester
_No entries yet. The tester will write common test failures and patterns here._

## From Architect
_No entries yet. The architect will write architecture decisions here._

## From Debugger
_No entries yet. The debugger will write root cause patterns here._

## From Self-Improve
- **2026-03-14**: Shared Knowledge Hub created. Meta-Evolution system initialized.
- **2026-03-14**: effortLevel was reset to "medium" by /model command — ConfigChange hook exists but Stop hook only catches at session end. Gap: need real-time protection.
- **2026-03-14**: Session scorer hook installed for automatic quality tracking.
- **2026-03-18 (Windows Self-Improve v5.7)**: Full environment sync from macOS. 6 critical improvements:
  - **FIXED** config-guard.ps1: effortLevel=medium was allowed with warning only — now BLOCKED (only 'high' passes)
  - **FIXED** safety-gate.ps1: Added 3 missing patterns (git restore ., git branch -D, case-insensitive TRUNCATE)
  - **UPGRADED** debugger + architect agents: Semi-Formal Reasoning Protocol added (Premises→Hypotheses→Refutation→Conclusion)
  - **INSTALLED** uv 0.10.11 + added to PATH. Prompt-Injection-Defender hook now uses python directly.
  - **UPDATED** Claude Code 2.1.76 → 2.1.78 (StopFailure hook, streaming, CRLF fix, security patches)
  - **SYNCED** 89 plugins, 10 marketplaces, 15+ hooks, Shared Knowledge Hub, 2 new skills (android-clean-architecture, android-ninja)
  - **REMOVED** Termux support (user no longer uses Termux)
  - **CREATED** self-improve-cache directory (~/.claude/self-improve-cache/) with per-category TTLs
- **2026-03-14**: Evolution analyst agent created for trend analysis.
- **2026-03-15**: Android Audio rules added to android.md (Oboe, SoundPool, Media3, TarsosDSP, Spatializer, HapticGenerator).
- **2026-03-15**: Worktree symlinks extended: .gradle + build added (saves 700MB+ per worktree).
- **2026-03-15**: Android audio development readiness confirmed: NDK r29 with AAudio.h + OpenSL ES headers present.
- **2026-03-15**: dpconde/claude-android-skill reviewed (SAFE) — recommended for Android Kotlin/Compose development.
- **2026-03-18**: Full env scan — 15 issues found and fixed. Key fixes: Command Injection in notify.sh, config-guard now blocks effortLevel!=high, safety-gate expanded patterns, session-scorer fixed to parse actual transcript format, auto-sync dead code removed. Installed kotlin-lsp + gopls. Manifest + settings-reference synced to high effort.
- **2026-03-18 (Self-Improve v5.7 Standard Run)**: 3 improvements implemented:
  - NEW `codebase-memory-mcp` v0.4.10 — persistent Knowledge Graph for code (99.2% token reduction for structural queries). 4 Skills auto-installed (exploring, tracing, quality, reference).
  - FIXED `optimizer.md` — added Mandatory Write-Back (was the only Senior-Agent without it)
  - Research cache created in `~/.claude/self-improve-cache/` (R1-R4 cached with per-category TTLs)
- **2026-03-18 (Self-Improve v5.6 Thorough Run)**: 5 improvements implemented:
  - NEW `StopFailure` hook — auto-logs API errors (rate limit, auth, network) to FAILURES.md
  - UPGRADED all 9 Opus agents with `effort: high` + differentiated `maxTurns` (architect/debugger: 80, quality-gate: 150, others: 60)
  - ENHANCED session-scorer.ts with Two-Phase SPC (Phase 1: simple trend <20 sessions, Phase 2: UCL/LCL + 7-point run >=20 sessions)
  - CORRECTED memory: Claude Code 2.1.79, Kotlin 2.3.0 (2.3.20 pending), Bun 1.3.11, 88 plugins (86 aktiv + 2 deaktiviert)
  - Updated Homebrew packages (uv 0.10.11)
- **2026-03-18 (Intelligence Run)**: 6 intelligence improvements implemented:
  - NEW `challenger` agent — Devil's Advocate for architecture plans (Multi-Agent Debate pattern)
  - NEW `FAILURES.md` — Failure Pattern Library so bugs are never repeated
  - NEW `PROCEDURES.md` — Procedural Memory for recurring workflows
  - NEW `intent-tracking.md` rule — Intent Drift Detector (reduces 12.1% correction rate)
  - FIXED session-scorer self-validation — no more dummy data (validates turns>0 when transcript>50 lines)
  - UPGRADED all 4 senior agents with mandatory write-back — Knowledge Hub will no longer stay empty
- **2026-03-18 (Self-Improve v5.7 Standard Run #2)**: 8 improvements implemented:
  - **CRITICAL FIX** `intent-anker.sh` — Turn-Counter wurde nie zurueckgesetzt → Session-Goal ging bei neuen Sessions verloren. Fix: session-cleanup.sh loescht Counter bei SessionEnd.
  - **UPGRADED** `intent-anker.sh` — Reminder-Intervall von 20 auf 5 Turns reduziert (Paper: arxiv 2510.07777 zeigt Drift-Onset bei Turn 4-7)
  - **UPGRADED** `session-scorer.ts` v2 — Rate-basiertes Scoring statt count-basiert. Alte Formel konnte 4 und 30 Korrekturen nicht unterscheiden (beides = 5.8). Neue Formel: correctionRate = corrections/turns mit 6 Stufen.
  - **FIXED** `session-scorer.ts` — False-Positive-Reduktion bei Korrektur-Erkennung: Match nur in ersten 80 Zeichen, kein Match bei Fragen (?) oder Code-Bloecken
  - **FIXED** `session-scorer.ts` — SPC-Trendanalyse schliesst jetzt Kurzsessions (<10 turns) aus (Challenger-Fix: verhindert Baseline-Inflation)
  - **UPGRADED** `safety-gate.sh` — 3 fehlende Patterns: `git restore .`, `git branch -D`, case-insensitive SQL (DROP/TRUNCATE)
  - **ADDED** Mandatory Write-Back zu 3 weiteren Agents: evolution-analyst, quality-gate, ui-polisher (jetzt alle 8 Opus-Agents)
  - **CLEANED** session-scores.jsonl — 25 Dummy-Eintraege (total_turns=0) entfernt, 7 valide behalten
  - CMake PATH fixiert, npm 11.11.1, icu4c aktualisiert
  - android-adb MCP-Server `-y` Flag ergaenzt (fehlgeschlagener MCP-Start behoben)

## Recurring Patterns
_Patterns detected across multiple sessions will be logged here automatically._

### [2026-03-19] Evolution Analyst — Trendanalyse (16 Sessions, 2026-03-18 bis 2026-03-19)
- **Quality-Trend**: Stabil zwischen 8.0 und 8.8. Vorperioden-Schnitt (Eintraege 1-8): 8.69. Aktuelle Periode (Eintraege 9-16): 8.63. Delta: -0.06 — STABIL (Schwelle: 0.5).
- **Plausibilitaets-Check**: BESTANDEN. Alle 16 Eintraege haben total_turns > 0. 14 von 16 Eintraege zeigen tool_calls > 0. Ausnahmen: Eintrag 11 (1 Turn, 0 Tool-Calls — vermutlich leere/abgebrochene Session).
- **Corrections-Rate**: 0.0% in ALLEN 16 Sessions — Intent-Anker-Fix vom 18.03. zeigt anhaltende Wirksamkeit. Kein einziger Drift-Vorfall seit dem Fix.
- **Tool/Turn-Ratio**: Aktuell 0.88 (Eintraege 12-16, Durchschnitt). Bereich: 0.0 (Eintrag 11) bis 1.27 (Eintrag 4). Ziel 1.5 wird in KEINER Session erreicht. Sequentielle Toolnutzung dominiert.
- **Error-Rate**: 0.0% in allen Sessions. Kein Fehlertyp sichtbar — aber Erinnerung: Fehlende Build-Latenzen bedeuten versteckte Performance-Probleme koennen unerkannt bleiben.
- **Kurzsessions (<10 Turns)**: 3 von 16 Sessions haben <=7 Turns (Eintraege 4, 10, 11). Diese werden korrekt aus SPC-Trendanalyse ausgeschlossen (Scorer v2 Fix vom 18.03.).
- **Agent Write-Back**: Noch immer keine Eintraege von Code-Reviewer, Tester, Architect oder Debugger seit Mandatory-Write-Back-Fix (18.03.). Technische Enforcement-Luecke bleibt offen.
- **R6 Creative Fokus-Empfehlung**: Hoechste Prioritaet liegt bei Tool/Turn-Ratio-Verbesserung (0.88 vs. Ziel 1.5). Konkret: Recherche zu automatischen Parallelisierungs-Hinweisen und Scoring-Anreizen (Bonus ab Ratio 1.5 im Scorer) — das ist der einzige Hebel der ohne Code-Aenderungen sofort wirkt.

### [2026-03-19] Evolution Analyst — Erstanalyse (3 Sessions, 2026-03-18)
- **Quality-Trend**: Stabil bei 8.8 (alle 3 Sessions identisch). Datenbasis zu klein fuer SPC (benoetigt 20 Sessions, Trend-Analyse ab 10).
- **Corrections-Rate**: 0.0% in allen Sessions — Intent-Anker-Fix vom 18.03. wirksam.
- **Tools/Turn-Ratio**: Durchschnitt 0.92 (Ziel: 1.5+). Sequentielle statt parallele Tool-Calls dominieren. Score-Schwelle von 3.0 im Scorer ist unrealistisch — sollte auf 1.5 gesenkt werden.
- **Agent Write-Back**: Noch keine Eintraege von Senior-Agenten seit Mandatory-Write-Back-Fix (18.03.). Enforcement fehlt technisch — SubagentStop-Hook ausstehend.
- **Prompt-Injection False Positive**: Defender erkennt JSDoc-Kommentare in ~/.claude/hooks/ als "Context Manipulation" — Allowlist fuer vertrauenswuerdige Pfade fehlt.

### [2026-03-19] Evolution Analyst — Performance-Analyse (3 Sessions, Performance-Fokus)
- **Unsichtbare Build-Zeiten**: Session-Scorer erfasst KEINE Build-Latenzen (Gradle, dotnet). Performance-Verluste durch langsame Builds bleiben systemisch unsichtbar. FAILURES.md "Build & Compilation" Sektion ist leer — bedeutet fehlende Dokumentation, nicht fehlende Probleme.
- **Git-fsmonitor fehlt**: `core.fsmonitor` nicht konfiguriert auf Windows. Bekannter 2–10x Speed-Up fuer git-status in grossen Repos. QuickFix: `git config --global core.fsmonitor true`.
- **Defender-Hook-Overhead**: PostToolUse-Defender spawnt Python-Prozess bei JEDEM Read/Bash/Grep/WebFetch. Auf Windows (kein fork, teures CreateProcess) messbar. Daemon-Architektur wuerde ~100–300ms pro Tool-Call sparen.
- **Gradle Config-Cache blockiert**: Challenger bestaetigt Inkompatibilitaet mit AGP 8.x — potenzieller Build-Speed-Gewinn (bis 80% bei wiederholten Builds) nicht nutzbar. R1 soll konkreten AGP-Versionsstand pruefen.
- **Tool/Turn-Ratio unveraendert niedrig**: 0.90–0.95 in allen 3 Sessions. Kein Anstieg trotz CLAUDE.md-Parallelisierungsgebot. Moegliche Ursache: Scorer bewertet Ratio unter 3.0 neutral (kein Abzug, kein Bonus) — kein Anreiz zur Verbesserung. Empfehlung: Parallelisierungs-Bonus im Scorer ab Ratio 1.5 einfuehren.
- **Researcher-Fokus Stufe 2**: R1=Gradle-Build-Profile, R2=Git-fsmonitor+Push-Latenz, R3=Defender-Daemon-Architektur, R4=Parallelisierungsrate-Metrik

### [2026-03-19] Evolution Analyst — Trendanalyse R7 (23 Sessions, vollstaendige Datenbasis)
- **Quality-Trend**: Vorperiode Eintraege 1-9 (Schnitt 8.72), aktuelle Periode Eintraege 14-23 (Schnitt 8.76). Delta: +0.04 — STABIL. Keine statistisch signifikante Bewegung in beiden Richtungen (Schwelle 0.5 weit unterschritten).
- **Plausibilitaets-Check**: BESTANDEN. Die letzten 10 Eintraege zeigen errors=0, corrections=0 in allen Sessions. Session 279141ac (3 Turns, 1 Tool-Call, Score 8.0) ist valide Kurzsession — korrekt erkannt, keine Inflationsgefahr fuer SPC-Baseline.
- **Corrections-Rate**: 0.0% in 23 aufeinanderfolgenden Sessions. Intent-Anker-Fix haelt seit > 1 Tag ohne Rueckfall. Stabilitaet bestaetigt.
- **Tool/Turn-Ratio aktuell**: Session e2d0f78c: 21/30=0.70, 29/40=0.73, 37/50=0.74, 50/61=0.82, 59/70=0.84. Session 59baa6d4: 39/40=0.98, 48/50=0.96, 51/55=0.93, 59/64=0.92. Session 279141ac: 1/3=0.33. Gesamtschnitt letzte 10 Eintraege: 0.80. Rueckgang von frueheren 0.88 auf jetzt 0.80 — Negativtrend sichtbar, Ziel 1.5 weiterhin unerreicht.
- **Agent Write-Back**: Nach 24+ Stunden seit Mandatory-Write-Back-Fix (18.03.) IMMER NOCH keine einzigen Eintraege von Code-Reviewer, Tester, Architect, Debugger. Enforcement-Luecke ist SYSTEMISCH, nicht zufall. SubagentStop-Hook fehlt — hoechste offene Prioritaet.
- **R6 Creative (aktualisiert)**: Parallelisierungs-Bonus im Scorer ab Ratio 1.5 einbauen. Ratio sank von 0.88 auf 0.80 — CLAUDE.md-Parallelisierungsregel wird nicht durch Scoring-Anreize verstaerkt.
- **R8 Intelligence (aktualisiert)**: SubagentStop-Hook Implementierung ist der kritischste Intelligence-Fokus. Ohne technisches Enforcement bleiben alle 4 Senior-Agenten stille Silos — Knowledge Hub nie befuellt.

## Capability Gaps
_Tasks that required missing agents/skills will be logged here._

## From Challenger
- **2026-03-18**: ✅ BEHOBEN — Session-Scorer v2: Kurzzeit-Sessions (< 10 turns) aus SPC ausgeschlossen (Zeile 180), kein Korrektur-Bonus fuer Kurzsessions (Zeile 141). Scoring jetzt rate-basiert statt count-basiert.
- **2026-03-19 (Challenger)**: Speed-Plan 7 Punkte geprueft — KRITISCH: Defender-Allowlist per Pfad ist ein struktureller Security-Blind-Spot. Angreifer koennen praeparierte Dateien in vertrauenswuerdige Pfade schreiben und sind dann unsichtbar. Fix: SHA-256-Hash-Whitelist statt Pfad-Allowlist. Weitere Risiken: /tmp/ Cache instabil auf Windows (Gradle Config-Cache inkompatibel mit AGP 8.x), Einzelmessungen (700ms/300ms) haben zu wenig Datenbasis fuer permanente MEMORY-Eintraege, speed-coding.md-Rule konfligiert mit spec-first.md.
- **2026-03-19 (Challenger)**: 4 Verbesserungen geprueft — KRITISCH: SubagentStop-Prompt-Hook kann MEMORY.md nicht wirklich pruefen (kein Dateisystem-Zugriff aus Prompt-Hook), loest die bekannte Enforcement-Luecke NICHT. Scorer-Bonus ab Ratio 1.5 bestraft aktuell die Haelfte aller Sessions (Baseline 0.88, Max 1.27 — Malus-Schwellenwert von 0.7 trifft reale Sessions). Coder-Selbsttests erzeugen False-Safety durch blinde Flecken. Empfehlung: Echter PowerShell-PostToolUse-Hook fuer Write-Back-Enforcement, Scorer-Schwellenwerte auf Baseline-Daten kalibrieren.
