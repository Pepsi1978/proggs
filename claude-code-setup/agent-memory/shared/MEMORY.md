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

## Recurring Patterns
_Patterns detected across multiple sessions will be logged here automatically._

## Capability Gaps
_Tasks that required missing agents/skills will be logged here._

- **2026-03-18**: Quality declining: 7.0 → 6.4 (last 5 sessions)
