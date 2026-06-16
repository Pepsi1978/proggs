# Cowork

Ablage-Ordner für die **Claude-Cowork-Desktop-App**: hier liegen die gekürzten
Cowork-Varianten unserer Skills als fertige ZIPs zum Hochladen — plus ihre versionierten
Quellen unter `skills-src/`.

## Die fertigen Cowork-Skill-ZIPs (zum Hochladen)

| ZIP | Skill | Was er in Cowork tut |
|-----|-------|----------------------|
| `bug-almanach-recherche.zip` | `bug-almanach-recherche` | Recherchiert bekannte Bugs/Workarounds einer Software (Researcher-Schwarm) und baut einen versionsbewussten Bug-Almanach. Inkl. `references/researcher-prompts.md`. |
| `best-practices.zip` | `best-practices` | Pflegt Best Practices für Harness-Werkzeuge + Projekt-Software, holt den Claude-Code-Changelog verbatim. Inkl. `scripts/update-changelog.{sh,ps1}`. |
| `research.zip` | `research` | Allgemeine, mehrquellige Web-Recherche zu einem Thema und persistiert wiederverwendbare Funde in `best-practices/` + Bug-Almanach. |
| `almanach-update.zip` | `almanach-update` | Aktualisiert BESTEHENDE Bug-Almanache als Batch/Welle per Re-Recherche auf die aktuelle Software-Version (Wrapper um `bug-almanach-recherche`, legt keinen neuen Almanach an). |
| `best-practices-update.zip` | `best-practices-update` | Aktualisiert BESTEHENDE Best-Practices als Welle (Harness + Projekt-Code gegen die Changelogs) und koppelt Bugs in die Almanache zurück (Wrapper um `best-practices`). |

Alle fünf sind **Cowork-tauglich** gebaut: relativer Ablage-Ort im verbundenen Arbeitsordner
(statt festem `~/proggs`-Pfad), Ordner-anlegen-Pflicht, Mount-Schreibfallen beachtet
(Dateiende prüfen / git-intern), Abschluss über `bash ~/proggs/cowork-git.sh push-files`
(Datenverlust-Wächter), und sie enthalten die neue **W3-Logik** (Versions-Anker,
`check-version-anchor`-Hinweis, `bug-almanac-hint`-Pflege, `python bugs/health.py`-Self-Test,
gh-Status-Pflicht „falls Shell verfügbar"). `description` jeweils ≤ 200 Zeichen (Claude.ai-Limit),
einzeilig in Anführungszeichen, `name`-Feld = Ordnername (Cowork-Anforderung).

## Hochladen in Cowork

In der Claude-Desktop-App: **Customize → Skills → „+" → „Upload a skill"** und das jeweilige
`.zip` ablegen. Das ZIP enthält den Skill-Ordner (`<name>/SKILL.md` …), wie Cowork es erwartet.
Danach den Skill per Toggle aktivieren. (Der Upload akzeptiert auch eine einzelne `.md`, aber die
Skills mit `references`/`scripts` brauchen das ZIP.)

## Quellen & Neubau

Die diffbaren Quellen liegen unter `skills-src/<name>/`. Die ZIPs werden daraus gebaut
(LF-normalisiert, Forward-Slash-Pfade, ZIP-interner Ordner = `name`-Feld). Nach einer Änderung an
einer Quelle das betroffene ZIP neu packen und beides committen.

## Warum dieser Ordner eine README braucht

Git lädt **keine leeren Ordner** hoch. Diese `README.md` hält den Ordner dauerhaft im Repo und
auf GitHub sichtbar; weitere Dateien (ZIPs, `skills-src/`) werden ganz normal mit committet.

## Verwandtes

- Push-Setup aus der Sandbox: `../scripts/COWORK-PUSH-SETUP.md` · Wrapper: `../cowork-git.sh`
- Cowork-Regeln: `../bugs/claude-tooling/cowork.md` · `../bugs/claude-tooling/cowork-git-push.md`
- Best Practices: `../best-practices/claude-tooling/cowork.md`
- CLI-Originale der Skills: `~/.claude/skills/bug-almanach-recherche/`, `~/.claude/skills/best-practices/`
