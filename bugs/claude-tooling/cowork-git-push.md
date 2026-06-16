# Bekannte Bugs & Fallen: Git committen/pushen aus Cowork (über Mount-Brücke)

> **PFLICHT vor Git-Arbeit aus Cowork lesen.** Dieser Almanach sammelt dokumentierte
> Fehler beim `git commit`/`git push` aus einer **Linux-VM**, deren Arbeitsbaum über eine
> **virtiofs/FUSE-Brücke** auf einem **Windows-Ordner** liegt (Claude Cowork / Claude Code Desktop).
> Das ist der Bereich, den das Skript `~/proggs/cowork-git.sh` absichert.
>
> **Stand:** recherchiert am 2026-06-15 für Git 2.34.1 (Linux-VM), Arbeitsbaum auf virtiofs/FUSE,
> Remote GitHub (HTTPS-Token). Quelle der Wahrheit ist origin/main.
> **Schwester-Dokumente:** `bugs/claude-tooling/cowork.md` §10a (Git-Abschnitt) ·
> Regel `~/.claude/rules/cowork-git-push.md` · Best-Practices
> `best-practices/claude-tooling/cowork-git-push.md`.

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

---

## A. Lock-Dateien & Prozess-Kollision

## 1. `index.lock`/`*.lock` lässt sich nicht löschen — commit/push hängt   [⭐ HÄUFIG]
**Symptom:** `fatal: Unable to create '.../.git/index.lock': File exists`; `rm -f` darauf
scheitert mit `Operation not permitted`. Folge-Befehle melden „Another git process seems to be running".
**Ursache:** Auf der virtiofs/FUSE-Brücke darf die VM erstellen/schreiben, aber **nicht `unlink`en**
(strukturelle Sperre auf Mount-Ebene, nicht POSIX-Rechte; `touch x && rm x` scheitert ohne Race).
Stale-Lock zusätzlich durch Windows-Datei-Handle-Latenz.
**Versionen:** Claude Code 2.1.121 / Cowork Windows 11, virtiofs. Offen.
**FIX:** **git-dir auf die VM-ext4-Platte** legen (`~/.cowork-gitdir/proggs`), nur der Arbeitsbaum
bleibt auf der Brücke — dort entstehende Locks sind löschbar (`rm -f $GITDIR/*.lock`). Genau das
macht `cowork-git.sh`. Notfalls aus dem Windows-Terminal pushen.
**Quelle:** https://github.com/anthropics/claude-code/issues/55206 · https://github.com/anthropics/claude-code/issues/28546

## 2. `git push`: „could not read Username for github.com"   [⭐ HÄUFIG]
**Symptom:** Commit geht, `push` bricht ab — kein Credential-Manager in der VM.
**Ursache:** Keine interaktive Auth möglich; VM-Home ist ephemer (überlebt Session nicht).
**Versionen:** unabhängig.
**FIX:** Token persistent in `<repo>/.git/credentials` (`https://USER:TOKEN@github.com`) +
`git config credential.helper "store --file=.git/credentials"` (relativ; Remote NICHT ändern).
Datei in `.gitignore`, Permissions 0600. Siehe `COWORK-GIT-PUSH-SETUP.md`.
**Quelle:** cowork.md §10a (selbst erlebt) · https://git-scm.com/docs/git-credential-store

## 3. Zwei „Welten" teilen dieselbe `.git`/config — Index-/Lock-Kollision
**Symptom:** Race auf `index.lock`, „indeterminate state"; `core.worktree`/Token-Pfad passen nur für eine Seite.
**Ursache:** VM-Welt (Mount) und Windows-Terminal nutzen dieselbe `.git`. Gleichzeitige Schreibbefehle kollidieren.
**Versionen:** unabhängig.
**FIX:** Getrenntes git-dir je Welt (VM nutzt `~/.cowork-gitdir`), Operationen serialisieren,
`core.worktree` NICHT in geteilte config schreiben — Work-Tree pro Aufruf via `--work-tree` setzen.
**Quelle:** https://learn.microsoft.com/en-us/azure/devops/repos/git/git-index-lock · https://git-scm.com/docs/git-config

---

## B. Datei-Modus & Symlinks

## 4. `core.fileMode`: alles erscheint geändert (100644 ↔ 100755)   [⭐ HÄUFIG]
**Symptom:** `git status`/`diff` zeigt massenhaft `old mode 100644 / new mode 100755` ohne Inhaltsänderung; Commits ziehen Mode-Diffs mit.
**Ursache:** FUSE/virtiofs (wie WSL-auf-NTFS) bildet Unix-Permission-Bits nicht stabil ab.
**Versionen:** unabhängig.
**FIX:** `git config core.fileMode false` — entfernt keine Funktion, Git ignoriert nur das unzuverlässige Executable-Bit.
**Quelle:** https://git-scm.com/docs/git-config (core.fileMode) · https://github.com/desktop/desktop/issues/9443

## 5. Symlinks nicht lesbar — `readlink: Operation/Input-output error`, `add -A` bricht ab
**Symptom:** `ls`/`git add -A`: `cannot read symbolic link ...: Input/output error`; Abbruch mit „unable to index file … updating files failed".
**Ursache:** Brücke kann bestimmte (Windows-)Symlinks/Junctions nicht auflösen; WSL2 unterstützt keine NT-Symlinks mit absolutem Ziel.
**Versionen:** unabhängig (FUSE/virtiofs, WSL).
**FIX:** Die unlesbaren, bereits getrackten Symlinks per `git update-index --skip-worktree <pfad>` von `add -A` ausnehmen (macht `guard_unreadable_symlinks`). Symlinks ggf. mit relativem Ziel neu anlegen.
**Quelle:** https://github.com/microsoft/WSL/issues/4104 · eigener Vorfall (`cowork-git.sh`)

---

## C. Git-LFS (Pointer vs. Vollinhalt)

## 6. LFS-Zeiger durch echten Riesen-Inhalt ersetzt → GitHub lehnt ab (>100 MiB)   [⭐ KRITISCH]
**Symptom:** `git add -A` packt den Vollinhalt (z. B. 262-MB-`.onnx`) statt des ~130-Byte-Pointers in den Tree; Push: `remote: error: File … exceeds GitHub's file size limit of 100.00 MB`. Später `git checkout`: „Encountered N file(s) that should have been pointers, but weren't".
**Ursache:** Auf der Brücke erscheinen LFS-Dateien als Vollinhalt; `add` nimmt diesen statt des Pointers. GitHub blockt >100 MiB hart (Warnung ab 50 MiB).
**Versionen:** unabhängig; git-lfs-Pattern-Matching case-sensitiv (gefixt git-lfs 3.6.0).
**FIX:** Alle getrackten LFS-Dateien (`.gitattributes` `filter=lfs`) per `skip-worktree` von `add -A` ausnehmen (`guard_lfs_pointers`); LFS-Pflege nur vom Windows-Rechner. Pre-Push-Prüfung: `git lfs fsck --pointers` (ab 3.0.2). Reparatur OHNE History-Rewrite: `git lfs migrate import --no-rewrite "<datei>"` (NIE nacktes `migrate import` → schreibt History neu). Case-insensitive Patterns: `*.[oO][nN][nN][xX] filter=lfs …`.
**Quelle:** https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github · https://github.com/git-lfs/git-lfs/issues/1939 · https://github.com/git-lfs/git-lfs/blob/main/docs/man/git-lfs-migrate.adoc

## 7. Fehlendes git-lfs beim Klon → Smudge-Filter läuft nicht (Pointer-Text statt Datei)
**Symptom:** `smudge filter lfs failed` oder Dateien enthalten nur `version https://git-lfs.github.com/spec/v1 …`.
**Ursache:** git-lfs nicht installiert/registriert (`git lfs install`). In der Cowork-VM ist git-lfs i. d. R. NICHT vorhanden — ein weiterer Grund, LFS-Dateien aus der VM gar nicht anzufassen.
**Versionen:** unabhängig.
**FIX:** In der VM LFS-Dateien per `skip-worktree` unangetastet lassen; echte LFS-Operationen auf dem Windows-Rechner mit installiertem git-lfs. `GIT_LFS_SKIP_SMUDGE=1` für schnelle Pointer-only-Klone.
**Quelle:** https://github.com/git-lfs/git-lfs/issues/4007 · https://docs.gitlab.com/topics/git/lfs/troubleshooting/

---

## D. Zeilenenden (CRLF/LF) & Encoding

## 8. CRLF/`autocrlf`/`safecrlf` — Schein-Diffs, Abbruch, „dirty" Worktree   [⭐ HÄUFIG]
**Symptom:** `warning: CRLF will be replaced by LF`; ganze Dateien als geändert nach Klon; bei `core.safecrlf=true` + gemischten Zeilenenden: `fatal: CRLF would be replaced by LF` → Abbruch. Folgefehler: Rebase scheitert (Worktree „dirty").
**Ursache:** Git normalisiert Zeilenenden beim add/checkout; auf Windows ist `autocrlf=true` Default; gemischte CRLF/LF sind nicht reversibel (safecrlf lehnt ab). `.gitattributes` selbst mit CRLF kann wirkungslos sein.
**Versionen:** unabhängig; `--renormalize` ab Git 2.16.
**FIX:** Eine **committete `.gitattributes` im Repo-Root** mit `* text=auto` (+ `*.sh text eol=lf`, `*.png binary` …) macht das Verhalten deterministisch, unabhängig von lokalem `autocrlf`. Einmalig `git add --renormalize .`. Für Cowork robuster: Commits git-intern bauen (umgeht Worktree-Konversion ganz). `merge.renormalize true` gegen Schein-Merge-Konflikte.
**Quelle:** https://git-scm.com/docs/gitattributes · https://docs.github.com/en/get-started/git-basics/configuring-git-to-handle-line-endings

## 9. Binärdatei als Text behandelt → Korruption; `working-tree-encoding` (UTF-16) bricht
**Symptom:** Binärdatei nach Checkin/Checkout korrupt; UTF-16-Skripte gelten als binär oder kommen kaputt an.
**Ursache:** `* text=auto` rät Text/Binär falsch → CR/LF-Bytes in Binärdaten umgeschrieben. Alte Git-Clients (<2.18) kennen `working-tree-encoding` nicht.
**Versionen:** unabhängig; `working-tree-encoding` ab ~2.18.
**FIX:** Binärtypen explizit markieren (`*.png binary`); Encoding nur wenn nötig (`*.ps1 text working-tree-encoding=UTF-16LE eol=crlf`).
**Quelle:** https://git-scm.com/docs/gitattributes · https://docs.github.com/en/get-started/git-basics/configuring-git-to-handle-line-endings

---

## E. Mount-Kohärenz & Datenverlust (der gefährlichste Bereich)

## 10. Mount liefert abgeschnittenen / 0-Byte / veralteten Inhalt — stiller Datenverlust   [⭐ KRITISCH]
**Symptom:** Die VM liest aus dem Mount eine abgeschnittene/leere/Leerzeichen-gepaddete Datei oder eine ältere Version, OHNE Fehler. Host und Claude Code lesen korrekt. Beim Commit wirkt die Datei „kleiner" → es gehen Zeilen verloren (real beobachtet: ~420 Zeilen weg). Datei „flackert" zwischen Versionen über mehrere Lesungen.
**Ursache:** virtiofs/FUSE Cache-Kohärenz-Lücke: gecachte Größe/Metadaten werden bei host-seitigen Schreibvorgängen nicht invalidiert; `cache=always`/writeback verschärfen das. Viele Windows-Editoren speichern per delete+recreate → Truncation/Phantom-Effekte.
**Versionen:** Cowork virtiofs (Claude Code 2.1.78+), reproduziert auf mehreren Maschinen. Offen.
**FIX:** Mount **nie blind vertrauen.** (1) Nach jedem Schreiben Dateiende prüfen (`tail -1`, `wc -l/-c`). (2) Commits **git-intern** aus stabilen VM-Dateien bauen. (3) **Datenverlust-Wächter:** vor dem Commit jede Datei in Byte-Größe gegen origin vergleichen; verdächtige Schrumpfung → ABBRUCH (macht `guard_data_loss`, Override `COWORK_ALLOW_SHRINK=1`). Vergleich gegen origin (stabil) fängt jedes Mount-Flackern.
**Quelle:** https://github.com/anthropics/claude-code/issues/38993 · #42520 · #50873 · https://lwn.net/Articles/774495/ · https://www.mail-archive.com/pve-devel@lists.proxmox.com/msg25949.html

## 11. „Stale file handle" (ESTALE) bei commit/checkout
**Symptom:** `error: unable to open loose object …: Stale file handle`; danach `cannot update ref …: nonexistent object`. Datei „verschwindet" zwischen zwei Zugriffen.
**Ursache:** virtiofsd/Netz-FS invalidiert das Inode-Handle nach Rename/Recreate (Git schreibt loose objects als Temp→rename). Editoren mit atomic-rename-Save sind Hauptauslöser.
**Versionen:** virtiofsd (2025), auch NFS/9p — unabhängig.
**FIX:** Pfade neu öffnen / Operation wiederholen statt abbrechen; Commits git-intern (kein Worktree-Rename). git-dir auf stabilem VM-FS.
**Quelle:** https://gitlab.com/virtio-fs/virtiofsd/-/issues/206 · https://www.baeldung.com/linux/stale-file-handles

## 12. Writeback-/`cache=always` — Schreibvorgänge verloren / Host-Änderungen unsichtbar
**Symptom:** Im Guest geschriebene/gelöschte Dateien landen nicht durabel auf dem Host; host-seitige Änderungen werden nie sichtbar; gelöschte gemappte Datei nicht neu anlegbar.
**Ursache:** `cache=always`/writeback puffert bis close/fsync bzw. invalidiert nie.
**Versionen:** virtiofsd writeback (kata #2748/#2770); Proxmox entfernte writeback 2025.
**FIX:** writeback meiden, `cache=auto`/`none`. In Cowork nicht direkt steuerbar → daher Dateiende/Größe nach jedem Write verifizieren (Wächter). Datei-Lebenszyklus nur von EINER Seite steuern.
**Quelle:** https://github.com/kata-containers/runtime/issues/2770 · https://lwn.net/Articles/774495/

## 13. case-insensitive NTFS überschreibt Dateien beim Checkout (Datenverlust)
**Symptom:** Repo mit `File.txt` und `file.txt` → beim Checkout über die Brücke überschreibt eine die andere; Phantom-Konflikte.
**Ursache:** NTFS case-insensitive, Linux case-sensitive; `core.ignorecase` mehrdeutig.
**Versionen:** unabhängig.
**FIX:** `git config core.ignorecase` bewusst setzen; pro Verzeichnis NTFS-Case-Sensitivity (`fsutil file setCaseSensitiveInfo`). Doppelnamen vermeiden.
**Quelle:** https://learn.microsoft.com/en-us/windows/wsl/case-sensitivity

## 14. `index file corrupt` / racy-git — instabiler FS verfälscht Erkennung
**Symptom:** `fatal: index file corrupt` / „smaller than expected"; oder Git hält geänderte Datei für unverändert (gleiche mtime-Sekunde).
**Ursache:** Partieller/abgebrochener Index-Write auf instabilem Mount; grobe/flackernde mtime → „racily clean".
**Versionen:** unabhängig.
**FIX:** Index neu aufbauen ohne Datenverlust: `rm -f .git/index && git reset` (Arbeitskopie bleibt). Index auf stabilem VM-FS halten (`GIT_INDEX_FILE`/git-dir VM-lokal). `git update-index --refresh` bei Verdacht.
**Quelle:** https://git-scm.com/docs/racy-git · https://www.codestudy.net/blog/how-to-resolve-error-bad-index-fatal-index-file-corrupt-when-using-git/

---

## F. Push & Plumbing (non-fast-forward, Force, Rebase)

## 15. Non-fast-forward — `! [rejected] (fetch first)`   [⭐ HÄUFIG]
**Symptom:** Push abgelehnt: „remote contains work that you do not have".
**Ursache:** origin/main wanderte weiter (andere Maschine). Schutzmechanismus, kein Bug. Bei häufigem Wandern auch Race direkt zwischen fetch/rebase und push.
**Versionen:** unabhängig.
**FIX:** Integrieren statt forcen: `fetch` → eigenen Commit per Plumbing auf den frischen `origin/main` als Parent setzen (`commit-tree -p origin/main`) → push; bei erneuter Ablehnung in **Retry-Schleife** wiederholen. KEIN `git push --force`.
**Quelle:** https://docs.github.com/en/get-started/using-git/dealing-with-non-fast-forward-errors · https://git-scm.com/docs/git-push

## 16. `--force` / `--force-with-lease` überschreiben fremde Commits
**Symptom:** Commits der anderen Maschine sind auf origin spurlos verschwunden.
**Ursache:** `--force` überschreibt bedingungslos. `--force-with-lease` wird durch ein direkt vorangehendes `fetch` (auch automatisches) **ausgehebelt** → Schutz wirkungslos.
**Versionen:** unabhängig.
**FIX:** In Automatik **kein** Force. Falls unvermeidbar: `--force-with-lease=<ref>:<vorab-gemerkter-SHA>` mit explizitem SHA, nie die implizite Form, und nie `fetch` direkt davor. Bevorzugt: Plumbing-Replay (§15).
**Quelle:** https://www.atlassian.com/blog/it-teams/force-with-lease · https://github.com/sublimehq/sublime_merge/issues/1846

## 17. Rebase scheitert am unsauberen Worktree — `cannot rebase: You have unstaged changes`
**Symptom:** `error: cannot rebase: You have unstaged changes. Please commit or stash them.`
**Ursache:** Git verlangt sauberen Baum; CRLF/LFS/fileMode lassen den Mount-Baum dauerhaft „dirty" erscheinen.
**Versionen:** unabhängig.
**FIX:** Rebase **umgehen** → Commit per Plumbing aufsetzen (braucht keinen sauberen Baum): `read-tree origin/main` → gezielt `add`/`update-index` → `write-tree` → `commit-tree -p origin/main` → `update-ref` → push.
**Quelle:** https://git-scm.com/docs/git-rebase · (Worktree-Rauschen) https://git-scm.com/docs/gitattributes

## 18. Plumbing-Fallstricke: Parent, Blob-Reihenfolge, Löschen, Ref-Schutz
**Symptom:** Commit ohne `-p` (losgelöst) → Push abgelehnt; `update-index --cacheinfo` mit nicht-existentem Blob → „object not found"; Datei aus VM nicht löschbar; falscher `update-ref` verliert lokalen Tip.
**Ursache:** `commit-tree` braucht `-p origin/main`; `--cacheinfo` aktualisiert nur den Index, nicht die Objekt-DB; `--remove` ignoriert noch existierende Worktree-Dateien.
**Versionen:** unabhängig.
**FIX (Reihenfolge):** `SHA=$(git hash-object -w datei)` → `git update-index --add --cacheinfo 100644,$SHA,pfad`; Löschen: `git update-index --force-remove pfad` (Mount erlaubt kein `rm`); `git commit-tree $TREE -p origin/main`; sicher: `git update-ref refs/heads/main <neu> <alt>`. Rettung bei Fehlern: `git reflog`.
**Quelle:** https://git-scm.com/docs/git-commit-tree · https://git-scm.com/docs/git-update-index · https://git-scm.com/docs/git-update-ref

---

## G. Getrenntes GIT_DIR / Work-Tree & Auth

## 19. GIT_DIR ohne GIT_WORK_TREE → CWD wird stiller Work-Tree (Datenverlust-Gefahr)
**Symptom:** `git status` zeigt halbes Home als untracked; `add -A` saugt Fremddateien; `clean -fdx` aus falschem Verzeichnis kann löschen. Oder `fatal: this operation must be run in a work tree`.
**Ursache:** Ist nur `GIT_DIR` gesetzt, gilt das CWD als Work-Tree-Top.
**Versionen:** unabhängig.
**FIX:** `--git-dir` UND `--work-tree` IMMER zusammen + **absolut** setzen (auch `GIT_INDEX_FILE`). Destruktive Befehle nur mit verifiziertem `--work-tree`. Vorab `git --git-dir=A --work-tree=B rev-parse --show-toplevel` prüfen.
**Quelle:** https://git-scm.com/docs/git · https://jdhao.github.io/2020/12/25/git_directory_work-tree_explained/

## 20. `detected dubious ownership` (safe.directory) — fremder Owner über Mount
**Symptom:** Jeder Git-Befehl: „detected dubious ownership in repository at '<pfad>'".
**Ursache:** git-dir/Work-Tree gehören anderem UID (Windows-Owner vs. VM-Nutzer); seit Git 2.35.2 (CVE-2022-24765) blockiert.
**Versionen:** ab 2.35.2.
**FIX:** `git config --global --add safe.directory <pfad>` für **beide** Pfade (git-dir und work-tree); ggf. `'*'` bewusst.
**Quelle:** https://support.atlassian.com/bitbucket-cloud/kb/git-command-returns-fatal-error-detected-dubious-ownership/

## 21. `credential.helper store` — Klartext-Token
**Symptom:** Token unverschlüsselt auf Platte; Exfiltration bei Dateizugriff / versehentlichem Commit.
**Ursache:** `store` legt `https://user:token@host` im Klartext ab (Doku-Warnung).
**Versionen:** unabhängig.
**FIX:** Credentials-Datei außerhalb des Work-Trees / auf VM-Platte, Permissions 0600, Pfad in `.gitignore`, nie von `add -A` erfassbar. Token rotieren. (cowork-git.sh kopiert den Token in eine VM-lokale Datei.)
**Quelle:** https://git-scm.com/docs/git-credential-store

---

## H. Performance

## 22. `git status`/`add` extrem langsam über den Mount
**Symptom:** Arbeitsbaum-Scans dauern sekundenlang (viele `lstat` über die Brücke).
**Ursache:** Jeder stat-Roundtrip geht durch die Mount-/9P-Schicht.
**Versionen:** unabhängig.
**FIX:** `core.preloadindex true`, `core.fscache true`, `core.untrackedCache true` (Vorsicht: untrackedCache/fsmonitor können auf Mounts ohne verlässliche Verzeichnis-mtime FALSCH liegen → im Zweifel aus). Große Build-Bäume per `.gitignore` ausschließen (`**/build/`, `**/.gradle/`, `**/node_modules/`). Repo möglichst auf lokalem FS.
**Quelle:** https://learn.microsoft.com/en-us/windows/wsl/filesystems · https://git-scm.com/docs/git-update-index

---

## Fix-Status (Stand 2026-06-15, Git 2.34.1)

| Frühere/dokumentierte Ursache | Status | Bezug |
|-------------------------------|--------|-------|
| git-lfs Pattern-Matching case-sensitiv | gefixt ab git-lfs **3.6.0** (PR #5699) | §6 |
| `git lfs fsck --pointers` als Guard | verfügbar ab git-lfs **3.0.2** | §6 |
| `git add --renormalize` | verfügbar ab Git **2.16** | §8 |
| `working-tree-encoding` | verfügbar ab Git **~2.18** | §9 |
| `safe.directory` Pflicht | seit Git **2.35.2** (CVE-2022-24765); Wildcard ab 2.36 | §20 |
| virtiofs Truncation/Stale-Metadaten (Cowork) | **offen** (Issues #38993/#42520/#50873) — Workaround aktiv | §10/§11 |
| `index.lock` unlink verweigert (Cowork) | **offen** (#55206) — git-dir auf VM-ext4 | §1 |
| virtiofs writeback-Datenverlust | beim Daemon konfig-abhängig; in Cowork nicht steuerbar → **Workaround aktiv** | §12 |

**Ehrlichkeits-Hinweis:** „gefixt" steht nur, wo eine offizielle Changelog-/Release-/Doku-Quelle es belegt.
Die Cowork-spezifischen virtiofs-Punkte (§1, §10, §11, §12) sind zum Stand offen; sie werden NICHT
durch eine git-Version behoben, sondern durch das Workaround-Skript `cowork-git.sh` (git-dir auf
VM-Platte + Datenverlust-Wächter + git-internes Commit-Bauen). gh-CLI stand in der VM nicht zur
Verfügung → Issue-Status aus den verlinkten Quellen, nicht per `gh` hart verifiziert.

## Bezug: Bug-Abschnitt ↔ Best-Practice (`best-practices/claude-tooling/cowork-git-push.md`)

| Bug-Abschnitt | Best-Practice |
|---------------|---------------|
| §1 Lock · §10 Truncation · §11 Stale · §18 Plumbing | §A1, §A2, §C1–C3 |
| §4 fileMode/Symlinks | §B1 |
| §22 Performance · §14 racy-git/Index | §B2, §B3 |
| §8 CRLF · §9 Binär/Encoding | §B4, §F1, §F2 |
| §2 Auth · §21 Klartext-Token | §C4, §E1–E3 |
| §15 non-fast-forward · §16 Force · §17 Rebase | §D1–D3 |
| §19 GIT_DIR/Work-Tree · §20 dubious ownership | §E4, §H1 |
| §6/§7 Git-LFS | §G1–G4 |
| §12 writeback · §13 case-insensitivity | §H2, §H3 |

## Umsetzung im Skript (Stand 2026-06-15, #46807)
`cowork-git.sh` deckt jetzt zusaetzlich ab: §15 Non-Fast-Forward → **Auto-Retry** (git-interner
3-Wege-Merge, kein Force, erhaelt fremde Commits, `COWORK_PUSH_RETRIES`); §22 Performance →
`core.preloadIndex`/`index.version 4`/`index.skipHash`/`gc.auto 0`; §20 dubious ownership →
`safe.directory`-Vorsorge; plus `GIT_TERMINAL_PROMPT=0`. Deterministisch getestet (Non-FF-Retry
erhaelt fremde Commits; echter Konflikt bricht sauber ab; Datenverlust-Waechter weiter aktiv).

## Pflicht-Checkliste vor `push` aus Cowork
- [ ] `bash cowork-git.sh setup` lief, „Push-Zugang OK" gesehen.
- [ ] Push über `cowork-git.sh` (nie nacktes `git push` aus der VM).
- [ ] Datenverlust-Wächter meldete „keine verdächtigen Schrumpfungen/Löschungen" (oder Schrumpfung war bewusst → `COWORK_ALLOW_SHRINK=1`).
- [ ] Datei-Änderungen git-intern gebaut ODER Dateiende geprüft (`tail -1`, `wc -l`).
- [ ] Commit enthält NUR Gewolltes (keine LFS-Riesendateien, keine Build-Berge, keine fremden pending Dateien).
- [ ] Bei „fetch first": Commit per Plumbing auf frischen `origin/main` aufgesetzt, KEIN Force.
- [ ] Auf GitHub bestätigt: `git ls-remote origin -h refs/heads/main` + Datei-Zeilenzahl/-ENDE.

---

## 🔗 Bezug zur Best-Practices-Gegenseite

Bug-Almanach (diese Datei) ↔ Best-Practices [`best-practices/claude-tooling/cowork-git-push.md`](../../best-practices/claude-tooling/cowork-git-push.md). Die identische Tabelle steht auch dort. Links der *Bug/die Falle*, rechts die *Regel, die sie verhindert*.

| Bug-Abschnitt (dieser Almanach) | Adressiert durch Best-Practice-Regel |
|---------------------------------|--------------------------------------|
| §1, §10, §11, §17, §18, §19 | Regel 2/3 |
| §10, §11, §12, §14 | Regel 4/5 |
| §4 | Regel 6 |
| §8, §9 | Regel 7 |
| §6, §7 | Regel 8 |
| §22 | Regel 9 |
| §15, §16, §17 | Regel 10 |
| §2, §20, §21 | Regel 11/12 |
