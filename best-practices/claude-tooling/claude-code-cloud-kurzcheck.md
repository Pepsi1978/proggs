# Claude Code in der Cloud: Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu, oder tritt in
> diesem Bereich ein Fehler auf, dann lies den entscheidenden Abschnitt im VOLLTEXT
> (`claude-code-cloud.md`), nicht nur diese Kurzfassung. Stand 27.09.2026, CLI 2.1.283.

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Cloud erkennen | `CLAUDE_CODE_REMOTE=true`; Hooks mit `[ "$CLAUDE_CODE_REMOTE" = "true" ] \|\| exit 0` beginnen | §1, §3 |
| 2 | Was lädt die Cloud? | Nur Committetes: CLAUDE.md (sonst AGENTS.md), `.claude/settings.json`, `.claude/skills|agents|commands`, `.mcp.json`. **Nichts aus `~/.claude`/OpenLauncher** | §2 |
| 3 | Skill für die Cloud | **Echter Ordner** unter `.claude/skills/<name>/` mit Verweis-SKILL.md, **kein Symlink** (Bug #14836) | §2 |
| 4 | Mehrere Repos | Dann keine Hooks/Permissions/.mcp.json → **immer genau ein Repo** pro Sitzung | §2 |
| 5 | Abhängigkeiten | Großes ins Setup-Skript (gecacht, < 5 min, Exit 0), Leichtes in den SessionStart-Hook; Env über `$CLAUDE_ENV_FILE` | §3 |
| 6 | Secrets | Nie in Environment-Variablen; „API credentials“ (Pro/Max) oder gar nicht in die Cloud | §3 |
| 7 | Netzwerk | Trusted hat **kein `dl.google.com`** → für Android-SDK Custom + Domain; Bun meiden | §4 |
| 8 | Push | Nur auf den eigenen `claude/…`-Branch; `main` nur über PR-Merge | §5 |
| 9 | GitHub-Abfragen | `gh` ist da; bei GraphQL-403 REST nehmen: `gh api repos/O/R/…` | §5 |
| 10 | Auf Codex/Bot-Review warten | Bot-Kommentare kommen **nicht als Ereignis** an → selbst per `gh api …/pulls/N/comments`, `…/issues/N/comments`, `…/reactions` abfragen | §5 |
| 11 | PR-Titel | „Create PR“ nimmt den ältesten Commit → Titel/Text selbst setzen | §5 |
| 12 | Übergabe | Nur Cloud → Terminal; Teleport ist fehleranfällig → lieber pushen + `git pull` | §6 |
| 13 | GUI/Gerät/adb | Geht in der Cloud nicht → lokale Sitzung oder Remote Control | §1, §6 |
| 14 | Android | Cloud programmiert, **GitHub Actions baut und signiert**; nie Keystore in die VM | §7 |
