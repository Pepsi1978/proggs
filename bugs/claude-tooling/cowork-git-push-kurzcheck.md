# Git committen/pushen aus Cowork (über Mount-Brücke) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `Unable to create '...index.lock': File exists` / `rm` schlägt fehl („Operation not permitted") | git-dir auf VM-ext4 legen; Lock per `rm -f $GITDIR/*.lock` (VM-lokal) | §1 |
| 2 | `git status` zeigt ALLES als geändert (mode 100644→100755) | `git config core.fileMode false` | §4 |
| 3 | `readlink: Input/output error` bei Symlinks; `add -A` bricht ab | unlesbare Symlinks `skip-worktree` | §5 |
| 4 | LFS-Datei (`*.onnx`/`*.aar`) erscheint als Vollinhalt (>100 MB), Push abgelehnt | LFS-Dateien `skip-worktree`; nie aus VM materialisieren | §7 |
| 5 | Datei endet mitten drin / 0 Byte / Leerzeichen-Padding / „flackert" zwischen Versionen | NIE blind committen → Datenverlust-Wächter (Byte-Vergleich gegen origin) | §10 |
| 6 | Commit committet weniger als erwartet (Zeilen fehlen) | Dateiende prüfen (`tail -1`,`wc -l`); Commit git-intern bauen | §10, §11 |
| 7 | `! [rejected] ... (fetch first)` / „remote contains work you do not have" | fetch + Commit per Plumbing auf frischen origin/main aufsetzen; KEIN `--force` | §13 |
| 8 | `cannot rebase: You have unstaged changes` | Rebase umgehen → Plumbing (read-tree→write-tree→commit-tree) | §14 |
| 9 | `could not read Username for github.com` | Token in `.git/credentials` + `credential.helper store --file=` (relativ) | §2 |
| 10 | `detected dubious ownership` | `safe.directory` für git-dir UND work-tree setzen | §17 |
| 11 | CRLF-Warnungen / `fatal: CRLF would be replaced by LF` | `.gitattributes` (`* text=auto`) committen; `core.safecrlf` beachten | §8 |
| 12 | Datei aus VM löschen scheitert („Operation not permitted") | Löschung git-intern: `update-index --force-remove` | §3, §15 |

> **Goldene Grundregel:** Aus Cowork NIE nacktes `git commit`/`git push`, sondern
> `bash ~/proggs/cowork-git.sh` (setzt alle Schutzmaßnahmen) — und Datei-Änderungen für
> Commits **git-intern** bauen (`hash-object -w` → `update-index --cacheinfo` → `commit-tree`),
> nicht blind aus dem Mount stagen.
