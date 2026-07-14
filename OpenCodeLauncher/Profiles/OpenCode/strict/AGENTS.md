# OpenCode-Projektbasis

> Der OpenCode Launcher lädt die ausführlichen Regeln des ausgewählten Profils zusätzlich als
> unveränderlichen Sitzungssnapshot. Diese kleine Basis gilt unabhängig vom Profil und auf allen Rechnern.

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

## Geschützte Dateien

- `~/.claude/rules/superintelligence.md` — Directive #1 (protected)
- `~/.claude/rules/self-observation.md` — Directive #2 (protected)
- `~/.claude/rules/resilient-bugfixing.md` — Directive #3 (protected)
- `~/.claude/rules/bypass-permissions-permanent.md` — Security (protected)
- `~/.claude/settings.json` permissions section — bypassPermissions must stay
- `.mcp.json` — Platform-specific, never auto-modify
- Any file in `~/Codex/` — Forbidden directory

## Basisregeln

- Secrets nie ins Repository schreiben; lokale Secrets liegen unter `~/SK/<projekt>/`.
- Fremde Änderungen im Worktree weder zurücksetzen noch überschreiben.
- Zwei Agenten dürfen nicht gleichzeitig dieselbe Datei bearbeiten.
- Keine Dateien unter `~/Codex/` verändern.

## Strikte Absicherung (Profil: Strikt)

- Pruefe Annahmen vor Aenderungen anhand des tatsaechlichen Zustands.
- Verifiziere jede Aenderung mit den relevanten Tests oder Builds.
- Pruefe betroffene Aufrufer und moegliche Regressionen.
- Melde verbleibende Unsicherheiten ausdruecklich.
