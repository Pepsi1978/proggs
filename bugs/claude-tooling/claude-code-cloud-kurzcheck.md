# Claude Code in der Cloud: Bug-Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Bei einem Fehler im Bereich oder vor Arbeit an Cloud-Abläufen den
> Volltext `claude-code-cloud.md` lesen. Stand 27.09.2026, CLI 2.1.283.

| # | Falle | Gegenmittel |
|---|---|---|
| B1 | Symlink-Skill-Ordner in `.claude/skills/` wird nicht gefunden (#14836, offen) | echter Ordner mit Verweis-SKILL.md |
| B2 | Projekt-Skills fehlen im `/`-Menü der Remote-Sitzung (#95105) | Skill beim Namen nennen; SessionStart-Hinweis |
| B3 | Push 403, leeres „authorized repository set“ | Repo beim Start wählen, neue Sitzung |
| B4 | Codex-/Bot-Reviews kommen nicht als Ereignis an (#62977) | per `gh api …/comments`, `…/reactions` pollen |
| B5 | API-Merge vom Auto-Modus blockiert (#96257) | melden, nicht umgehen |
| B6 | PR-Titel vom ältesten Commit; Phantom-„unpushed commits“ | Titel selbst setzen; `git fetch --prune` |
| B7 | Repo-Hooks ignoriert bei 2+ Repos | genau ein Repo |
| B8 | Repo-Plugins nie registriert (#78119) | nicht darauf bauen |
| B9 | Android `dl.google.com` 403 | Custom-Netzwerk oder CI bauen lassen |
| B10 | Maven-Central-DNS-Fehler (#13372) | CI bauen lassen |
| B11 | Bun, TLS, Allowlist ignoriert | npm/pnpm; Domains einzeln |
| B12 | Teleport nur erster Turn (#94836) | pushen + `git pull` |
| B13 | Remote Control 0 Events / Amnesie | Zwischenstände committen |
| B14 | Kein Browser in der Cloud | lokal / Remote Control |
