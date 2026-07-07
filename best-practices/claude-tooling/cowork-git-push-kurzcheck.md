# Cowork Git-Push Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Aus Cowork committen/pushen | NIE nacktes git → `bash ~/proggs/cowork-git.sh` | §A1 |
| 2 | Datei für Commit ändern | git-intern bauen: `hash-object -w`→`update-index --cacheinfo`→`commit-tree` | §A2, §C1 |
| 3 | Repo-Lage wählen | Wenn möglich auf VM-ext4; git-dir IMMER auf VM-Platte, Work-Tree auf Mount | §H1, §H5 |
| 4 | Mount meldet alles geändert | `core.fileMode false`, `core.quotePath false`; `core.symlinks` bewusst | §B1 |
| 5 | `git status` langsam | `core.preloadIndex`+`feature.manyFiles`+`index.skipHash`; **fsmonitor NICHT** (auf FUSE experimentell) | §B2, §B3 |
| 6 | Skript parst git-Ausgabe | Plumbing + `--porcelain -z`/`for-each-ref --format`, nie Porcelain-Text | §C2 |
| 7 | Ref sicher setzen | `update-ref <ref> <neu> <alt>` (compare-and-swap); Mehrfach: `--stdin` atomar | §C3 |
| 8 | nicht-interaktiv pushen | `GIT_TERMINAL_PROMPT=0` + `GIT_ASKPASS`/Helper; Secrets via `--config-env` | §C4 |
| 9 | origin/main wandert | integrieren statt forcen; Plumbing-Replay in Retry-Schleife; `pull.ff only` | §D1, §D2 |
| 10 | Force nötig | `--force-with-lease=<ref>:<vorab-SHA>` (+`--force-if-includes`), NIE nacktes `--force` | §D3 |
| 11 | Token/Auth | Fine-grained PAT (1 Repo, `contents:write`, Ablauf) ODER GitHub-App-Token; 0600, in `.gitignore` | §E1, §E3 |
| 12 | Zeilenenden/Binär | committete `.gitattributes` im Root (`* text=auto` + Template); `renormalize` | §F1, §F2 |
| 13 | große Binärdateien | LFS: `.gitattributes` ZUERST committen; `fsck --pointers` als Pre-Push-Guard | §G1, §G3 |
| 14 | fremder Owner | `safe.directory` für git-dir UND Work-Tree | §E4, §H4 |

> **Goldene Grundregel:** Aus Cowork läuft jede Schreib-/Push-Operation über `cowork-git.sh` und
> Datei-Inhalte werden git-intern gebaut — der Mount-Arbeitsbaum wird nie blind als Wahrheit genommen.
