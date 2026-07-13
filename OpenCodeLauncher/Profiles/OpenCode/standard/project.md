# AGENTS.md — Agent-Optimized Context (Claude Code, OpenCode & jede andere CLI)

> This file is for AGENTS (coder, tester, reviewer, optimizer), not humans.
> It provides the minimum context needed for efficient autonomous work.
> Kurz halten: OpenCode befolgt AGENTS.md < ~150 Zeilen deutlich zuverlaessiger;
> ab ~200-400 Zeilen degradiert die Befolgung (best-practices/opencode/agents-md-memory.md).

## Globale Kern-Regeln + Arbeitsregeln aus dem Gehirn

Es gelten die globalen OpenCode-Kernregeln aus dem vom Launcher geladenen globalen Profilsnapshot
(Sprache, Multi-Task, Git, Secrets, Komprimierung, Arbeitsmodi und Direktive 3). Dort steht auch die Pflicht-Startaufgabe:
`Programmierung/Rules` vollständig per `get_category_item` von 1 bis N einzeln laden und danach
„N Regeln aus dem zweiten Gehirn eingelesen.“ bestätigen. Second-Brain-Einträge immer einzeln laden,
nie eine große Kategorie per `get_by_category`. Diese Datei ergänzt nur das proggs-Spezifische.

## Session-Start Git-Sync (PFLICHT)

Vor jeder anderen Session-Arbeit `git fetch origin` ausführen. Ist
`git rev-list --count HEAD..origin/main` größer als 0, sofort mit
`git rebase --autostash origin/main` aktualisieren. Bei Konflikten `git rebase --abort` ausführen und
Frank fragen; nie Konflikte raten. Erst danach die Arbeitsregeln laden und die Aufgabe bearbeiten.

## Repository Structure

```
~/proggs/                          # Monorepo: Pepsi1978/proggs
  BestJournalAndroid/              # Kotlin/Compose journal app (Google Play)
  BestJournalFrank/                # Kotlin/Compose journal app (clone, no Firebase, no Premium)
  claude-code-setup/               # Claude Code config sync (hooks, rules, agents, settings)
  ClaudeCodexVoiceOverlay-macOS/   # Swift voice overlay
  ClaudeVoiceOverlay-Windows/      # C#/WPF voice overlay
  QuizVerse/                       # Kotlin quiz app
  mcp-code-search/                 # Local semantic search server
  tools/                           # Shared utilities
```

## Build Commands

| Project | Build | Test | Lint |
|---------|-------|------|------|
| BestJournalAndroid | `cd BestJournalAndroid && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| BestJournalFrank | `cd BestJournalFrank && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| C#/WPF | `dotnet build -c Release` | `dotnet test` | `dotnet format analyzers` |

## Files Agents Must NEVER Modify

- `~/.claude/rules/superintelligence.md` — Directive #1 (protected)
- `~/.claude/rules/self-observation.md` — Directive #2 (protected)
- `~/.claude/rules/resilient-bugfixing.md` — Directive #3 (protected)
- `~/.claude/rules/bypass-permissions-permanent.md` — Security (protected)
- `~/.claude/settings.json` permissions section — bypassPermissions must stay
- `.mcp.json` — Platform-specific, never auto-modify
- Any file in `~/Codex/` — Forbidden directory

## File Ownership Rules

- Two agents must NEVER edit the same file simultaneously
- Each agent gets assigned files in its prompt — stay within those files
- If you need to read a file another agent owns: READ only, never WRITE

## Android-Specific Conventions (BestJournal apps)

- Package: `com.bestjournal.app` (Android) / `com.entropyjournal` (Frank)
- Architecture: MVVM + Hilt DI + Room DB + Coroutines/Flow
- UI: Jetpack Compose, Material3, Dark Mode with 3 profiles
- Frank app: NO Firebase, NO Premium features, NO analytics
- Both apps must maintain feature parity (see feedback_frank_porting_pattern.md)
- Settings buttons: `wrapContentSize` + `Row(Center)`, never `fillMaxWidth`
- DB migrations: Always export schema JSON to `app/schemas/`
- After ADB install: Always auto-launch the app

## Commit Format

**Commit AND push automatically after EVERY finished task — immediately, of your own accord, WITHOUT
waiting for an instruction from Frank. This is the default, not optional.** Even a single small change
triggers commit+push. For app tasks: commit+push BEFORE build/install/deploy. Full rule: globaler
Profilsnapshot §3 + brain `Programmierung/Rules`.
`#NNN - Description` (English). Number auto-incremented from last commit.

## Quality Requirements

- Quality Gates follow global profile §6: Schnellmodus no pass, Normalmodus at most 2,
  Gruendlichkeitsmodus has no fixed limit and repeats until green; never turn review findings outside
  the user scope into a second task
- Required builds must be green before task completion/deploy; the pre-build commit order from global
  profile §3 remains binding, and build failures require an immediate follow-up fix
- Cross-platform: .ps1 hooks need .sh counterpart (and vice versa)
- Do not leave project files untracked. Anything an agent creates or edits in the repo must be committed and pushed to GitHub, except secrets/local/generated files which must be explicitly ignored or replaced with a safe template.

# Bugfixes niemals ins zweite Gehirn speichern (KRITISCH)

Bugfixes ausschließlich lokal in `.claude/agent-memory/shared/bug-cases.jsonl`, `bugs/` und bei Bedarf
`best-practices/` dokumentieren. Auch nach erfolgreicher Verifikation NIEMALS den `second-brain`-MCP-
Aufruf `remember` dafür verwenden und im Second Brain weder `bugfixes` noch eine Unterkategorie davon
anlegen oder befüllen. Direktive #3 bleibt für Root Cause, funktionserhaltenden Fix und Verifikation
vollständig aktiv; nur die redundante Ablage einzelner Fix-Akten im Second Brain entfällt.
