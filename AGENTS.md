# AGENTS.md — Agent-Optimized Context for Claude Code Agents

> This file is for AGENTS (coder, tester, reviewer, optimizer), not humans.
> It provides the minimum context needed for efficient autonomous work.
> Source: arXiv 2601.20404 — structured agent context reduces tool calls by 15-30%.

## Repository Structure

```
~/proggs/                          # Monorepo: Pepsi1978/proggs
  BestJournalAndroid/              # Kotlin/Compose journal app (Google Play)
  BestJournalFrank/                # Kotlin/Compose journal app (clone, no Firebase, no Premium)
  claude-code-setup/               # Claude Code config sync (hooks, rules, agents, settings)
  ClaudeCodexVoiceOverlay-macOS/   # Swift voice overlay
  ClaudeVoiceOverlay-Windows/      # C#/WPF voice overlay
  Tampermonkey/                    # Browser userscripts (JavaScript)
  QuizVerse/                       # Kotlin quiz app
  mcp-code-search/                 # Local semantic search server
  tools/                           # Shared utilities
```

## Build Commands

| Project | Build | Test | Lint |
|---------|-------|------|------|
| BestJournalAndroid | `cd BestJournalAndroid && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| BestJournalFrank | `cd BestJournalFrank && ./gradlew assembleDebug` | `./gradlew test` | `./gradlew lint` |
| Tampermonkey | N/A | Manual browser test | `bunx biome check <file>` |
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

`#NNN - Description` (English). Number auto-incremented from last commit.

## Quality Requirements

- Every feature must pass `quality-gate` agent before commit
- Build must be green before commit (no broken builds pushed)
- Cross-platform: .ps1 hooks need .sh counterpart (and vice versa)
- Do not leave project files untracked. Anything an agent creates or edits in the repo must be committed and pushed to GitHub, except secrets/local/generated files which must be explicitly ignored or replaced with a safe template.
