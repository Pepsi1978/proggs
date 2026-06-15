# Cowork Git-Push — Best Practices (Stand 2026-06-15, Git 2.34.1)

> Präventionsseite zu `bugs/claude-tooling/cowork-git-push.md`. Sagt, wie man Git-Push aus
> der Cowork-VM (Arbeitsbaum auf virtiofs/FUSE-Brücke) von vornherein richtig macht.

## Goldene Regeln
1. **Nie nacktes `git`. Immer `bash ~/proggs/cowork-git.sh`** — kapselt alle Schutzmaßnahmen. (offiziell-analog: WSL-Doku „Repo nicht über die Brücke beschreiben")
2. **git-dir auf VM-ext4, Arbeitsbaum auf der Brücke** (`--git-dir`/`--work-tree` IMMER zusammen + absolut). Verhindert nicht-löschbare Locks + dubious-ownership. (git-scm git/git-config)
3. **Commits git-intern bauen, nicht aus dem Mount stagen:** `hash-object -w` → `update-index --cacheinfo`/`--force-remove` → `write-tree` → `commit-tree -p origin/main` → `update-ref` → push. Umgeht Truncation/Stale/dirty-Worktree komplett. (git-scm Plumbing-Doku)
4. **Datenverlust-Wächter aktiv lassen:** Byte-Vergleich gegen origin vor jedem Commit; verdächtige Schrumpfung/Phantom-Löschung → Abbruch. Bewusste Schrumpfung nur mit `COWORK_ALLOW_SHRINK=1`. (extern: virtiofs-Truncation belegt, Anthropic #38993)
5. **Nach jedem Schreiben Dateiende prüfen** (`tail -1`, `wc -l/-c`) — nie nur den Anfang.
6. **`core.fileMode false` + `core.quotePath false`** setzen. (git-scm git-config)
7. **`.gitattributes` im Repo-Root** mit `* text=auto` + `eol`/`binary` committen statt sich auf `core.autocrlf` zu verlassen; `git add --renormalize .` einmalig. (git-scm gitattributes; GitHub-Doku)
8. **Git-LFS-Dateien aus der VM NICHT anfassen** (`skip-worktree`); LFS-Pflege nur vom Windows-Rechner mit installiertem git-lfs. Pre-Push: `git lfs fsck --pointers`. (git-lfs-Doku; GitHub 100-MiB-Limit)
9. **Build-/Abhängigkeits-Berge ignorieren** (`**/build/`, `**/.gradle/`, `**/node_modules/`). (Performance/WSL-Doku)
10. **Bei „fetch first" nie `--force`** — Commit per Plumbing auf den frischen `origin/main` neu aufsetzen (Retry-Schleife). `--force-with-lease` wird durch vorheriges `fetch` ausgehebelt. (Atlassian; GitHub-Doku)
11. **Token sicher:** `.git/credentials` (0600), in `.gitignore`, nie von `add -A` erfassbar; in VM-lokale Kopie übernehmen. (git-scm git-credential-store)
12. **`safe.directory`** für git-dir UND work-tree setzen, wenn „dubious ownership" auftritt. (git-scm; CVE-2022-24765)

## Bug ↔ Best-Practice Bezug
| Best-Practice | adressiert Bug-Abschnitt |
|---------------|--------------------------|
| Regel 2/3 | §1, §10, §11, §17, §18, §19 |
| Regel 4/5 | §10, §11, §12, §14 |
| Regel 6 | §4 |
| Regel 7 | §8, §9 |
| Regel 8 | §6, §7 |
| Regel 9 | §22 |
| Regel 10 | §15, §16, §17 |
| Regel 11/12 | §2, §20, §21 |
