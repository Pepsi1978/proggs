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
  - CORRECTED memory: Claude Code 2.1.78, Kotlin 2.3.20, Bun 1.3.11, 89 plugins (not 93)
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

## Capability Gaps
_Tasks that required missing agents/skills will be logged here._

## From Challenger
- **2026-03-18**: ✅ BEHOBEN — Session-Scorer v2: Kurzzeit-Sessions (< 10 turns) aus SPC ausgeschlossen (Zeile 180), kein Korrektur-Bonus fuer Kurzsessions (Zeile 141). Scoring jetzt rate-basiert statt count-basiert.
