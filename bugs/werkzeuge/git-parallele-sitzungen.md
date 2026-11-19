# Git im gemeinsamen Repo: parallele Sitzungen, Vormerken, Worktree-Bau

Stand: 19.11.2026 (git 2.4x, Windows/macOS/Hostinger-Server; gilt für alle Rechner und CLIs)

Im Repo `~/proggs` laufen oft mehrere Sitzungen gleichzeitig. Die Fallen hier kosten jeweils einen
Zusatz-Commit oder einen Fehlbau. Herkunft: Windows-Gedächtnis (`commit-nur-mit-pfaden`,
`parallele-sitzung-nimmt-dateien-mit`), hier für Mac und Server abgelegt, die dieses Gedächtnis nicht laden.

## ⚡ Kurzcheck

| # | Situation | Sofort-Regel |
|---|-----------|--------------|
| 1 | Datei hat fremde Änderungen (oft `MEMORY.md`) | Nur den eigenen Stand vormerken (`git hash-object -w` + `git update-index --cacheinfo`), `git diff --cached --name-only` prüfen, dann `git commit` **ohne** Pfade. `git commit -- <pfad>` nimmt die ganze Arbeitskopie mit. |
| 2 | Vormerken | Nie einen Ordner (`git add <ordner>`), nur einzelne Dateien: Er nimmt halbfertige Dateien paralleler Sitzungen mit (M17). |
| 3 | `git add` nach `git rm` | Gelöschte Pfade nicht erneut nennen: `git add` bricht sonst komplett ab („pathspec … did not match“), und ein mit `;` verketteter Commit enthält nur die Löschung. `add && commit` verketten (M28). |
| 4 | Pfad liegt in einem `.gitignore`-Ordner, ist aber versioniert | `git add -u -- <pfade>`: ein normales `git add` bricht komplett ab („paths are ignored by one of your .gitignore files“) (M39). |
| 5 | Vor dem Bau: `git status --short <App>` zeigt fremde Änderungen (`??` neues Paket, `M build.gradle.kts`) | Gleich im Worktree bauen: `git worktree add --detach <scratchpad>/wt HEAD`, `git diff -- <eigene Pfade> \| git -C <wt> apply`, dort `eintrag.py`, dann bauen; danach `git worktree remove --force` (M9). |
| 6 | Worktree-Bau neben paralleler Sitzung | Direkt vor dem letzten Bau `git fetch && git rebase origin/main`, dann `eintrag.py`, bauen, committen, sofort pushen; nie nach dem Bau bumpen. Scheitert der Push: Rebase, im Versionslog den fremden Stand nehmen, `eintrag.py` neu, neu bauen. Nach jedem Konflikt vor dem Commit `grep -rln '^<<<<<<<' <App>/app/src` (M21). |
| 7 | Ersetzungsskript für Index-Fassung und Arbeitskopie | Erst beide Fassungen fertig berechnen und prüfen, dann schreiben: `open(p,'w').write(f(x))` leert die Datei schon vor `f`. Zeilenenden der Quelle übernehmen. |

Wirkungsprüfung: Maßnahmen M9, M17, M21, M28, M39 in `skills/session-flaschenhals/massnahmen.json`.
