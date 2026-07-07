# Cowork Git-Push — Best Practices (Stand 2026-06-15, Git 2.34.1)

> Die „richtige Seite der Medaille" zu `bugs/claude-tooling/cowork-git-push.md`: *wie man Git
> committen/pushen aus der Cowork-VM (Arbeitsbaum auf virtiofs/FUSE-Brücke) von vornherein richtig
> macht*. Quelle der Wahrheit ist origin/main; das Werkzeug ist `~/proggs/cowork-git.sh`.
>
> **Versions-Anker:** Git 2.34.1 (Linux-VM), live recherchiert am 2026-06-15 mit 7 parallelen Researchern.
> **Quellen-Rangordnung:** offiziell (git-scm.com, docs.github.com, git-lfs-Manpages, Microsoft Learn,
> QEMU-virtiofsd) = Grundwahrheit; Community als `extern` gelabelt (überstimmt nie Offizielles).
> Jeder Eintrag trägt Quelle + `offiziell`/`extern`-Flag.

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

---

## A. Grundsatz & Cowork-Setup

## A1. Immer über `cowork-git.sh`, nie nacktes git aus der VM
**Best Practice:** Aus Cowork ausschließlich `bash ~/proggs/cowork-git.sh {setup|push|push-files}` nutzen.
**Begründung:** Das Skript bündelt alle hier gelisteten Schutzmaßnahmen (git-dir auf VM-Platte,
fileMode/quotePath, Symlink-/LFS-Guards, Datenverlust-Wächter). Nacktes git stolpert über die Mount-Fallen.
**Quelle:** eigenes Skript + Bug-Almanach `cowork-git-push.md` · `extern`

## A2. Datei-Inhalte git-intern bauen, nicht aus dem Mount stagen
**Best Practice:** Änderungen über `git show origin/main:PFAD > /tmp/x` → bearbeiten → `hash-object -w`
→ `update-index --cacheinfo` → `write-tree` → `commit-tree -p origin/main` → `update-ref` → push.
**Begründung:** Umgeht die unzuverlässige Brücke komplett (Truncation/Stale/dirty-Worktree). Jeder
dieser Plumbing-Befehle ist offiziell als Skript-Primitiv ausgewiesen.
**Quelle:** git-scm Plumbing-Doku (hash-object/commit-tree/update-ref) · `offiziell`

---

## B. Git-Konfiguration (Robustheit & Performance auf der Brücke)

## B1. Mount-Artefakte neutralisieren: fileMode/quotePath/symlinks
**Best Practice:** `core.fileMode false` (Executable-Bit unzuverlässig), `core.quotePath false`
(Umlaut-Pfade unverstümmelt + skript-parsbar), `core.symlinks` bewusst (oft `false`, wenn der Mount
keine echten Symlinks kann). Am robustesten per Aufruf: `git -c core.fileMode=false …`.
**Begründung:** Verhindert Phantom-„mode change"-Diffs und Checkout-Fehler; Git ignoriert nur das
unzuverlässige Bit, ohne Inhalte zu ändern.
**Quelle:** git-scm.com/docs/git-config · `offiziell`

## B2. Performance ohne fsmonitor: preloadIndex + manyFiles + index v4 + skipHash
**Best Practice:** `core.preloadIndex true` (parallele lstat), `feature.manyFiles true`
(zieht `index.version=4` + `core.untrackedCache` nach), `index.skipHash true` (schnellere Index-Writes).
`gc.auto 0` + `git maintenance start` statt blockierendem Auto-GC über die Brücke.
**Begründung:** Reduziert die teure Per-Call-Latenz und die Index-Bytes über die langsame Mount-Schicht.
**Quelle:** git-scm.com/docs/git-config + GitHub Engineering Blog (`offiziell`); `index.skipHash`/Tuning (`extern`)

## B3. fsmonitor auf FUSE/virtiofs NICHT blind aktivieren
**Best Practice:** `core.fsmonitor` standardmäßig AUS lassen. Nur experimentell mit
`fsmonitor.allowRemote true` + `fsmonitor.socketDir <lokales ext4>`; vorher untrackedCache mit
`git update-index --test-untracked-cache` auf Mount-Eignung prüfen.
**Begründung:** Der fsmonitor-Daemon lehnt netzwerk-gemountete Repos per Default ab und nutzt
Unix-Domain-Sockets, die „not on network-mounted filesystems, NTFS, or FAT32" funktionieren — auf
virtiofs/FUSE also riskant. (Falle: blind eingeschaltet bricht/liegt status falsch.)
**Quelle:** git-scm.com/docs/git-fsmonitor--daemon + GitHub Blog „file system monitor" · `offiziell`

## B4. Zeilenenden über `.gitattributes`, nicht über `core.autocrlf`
**Best Practice:** `core.autocrlf false` lassen und Zeilenenden per committeter `.gitattributes`
festschreiben (siehe §F). Optional `merge.renormalize true`.
**Begründung:** Eine repo-lokale `.gitattributes` gilt für alle, unabhängig von lokaler Config →
keine plattformabhängigen Phantom-Diffs.
**Quelle:** docs.github.com „Configuring Git to handle line endings" · `offiziell`

---

## C. Robuste, skriptbare Git-Automatisierung

## C1. Plumbing statt Porcelain im Skript
**Best Practice:** In Skripten `hash-object`, `update-index`, `write-tree`, `commit-tree`, `update-ref`,
`for-each-ref`, `rev-parse`, `cat-file` nutzen — nicht `add`/`commit`/`status`/`branch` parsen.
**Begründung:** Plumbing ist offiziell „designed to be chained together … or called from scripts" und
stabil; Porcelain-Ausgabe ist menschlich und „subject to change at any time".
**Quelle:** git-scm Pro Git „Plumbing and Porcelain" · `offiziell`

## C2. Maschinenlesbar parsen: `--porcelain -z`, `for-each-ref --format`, `rev-parse`
**Best Practice:** `git status --porcelain=v2 -z`, Refs über `git for-each-ref --format=…` (oder
`--shell`), OIDs/Pfade über `git rev-parse`. NUL-Trennung (`-z`) statt zeilenweisem Parsen.
**Begründung:** Porcelain-Format ist „stable across Git versions and regardless of user configuration";
`-z` gibt Pfade „as is and without any quoting" → robust gegen Leerzeichen/Umlaute/Newlines.
**Quelle:** git-scm.com/docs/git-status, /git-for-each-ref · `offiziell`

## C3. Sichere & atomare Ref-Updates (compare-and-swap / Transaktion)
**Best Practice:** `git update-ref <ref> <neu> <alt>` mit drittem Argument (Update nur, wenn `<alt>`
noch stimmt). Mehrere Refs gebündelt: `git update-ref --stdin -z` (All-or-nothing). Vorbedingung
prüfen mit `verify`. Datei aus dem Tree nehmen: `update-index --force-remove`.
**Begründung:** Schützt vor verlorenen Updates bei wanderndem origin; „Update the object name … safely".
**Quelle:** git-scm.com/docs/git-update-ref, /git-update-index · `offiziell`

## C4. Nicht-interaktiv & isoliert laufen
**Best Practice:** `GIT_TERMINAL_PROMPT=0` (kein Hängen an Prompts) + `GIT_ASKPASS`/Credential-Helper;
Config per Aufruf via `git -c key=value` statt global; Secrets via `git --config-env=…` (nicht `-c`,
das in `ps` sichtbar wäre); lange Optionen ausschreiben; `--`/Options-first; Exit-Codes prüfen
(die=128, usage=129); `--no-optional-locks` für Hintergrund-`status`.
**Begründung:** Determinismus, keine hängenden Jobs, keine Secret-Leaks in Prozessliste/History,
versionssichere Skripte.
**Quelle:** git-scm.com/docs/git, /gitcredentials, /gitcli, /api-error-handling · `offiziell`

---

## D. Sichere, verlustfreie Push-Workflows

## D1. `push.default=simple` + `pull.ff=only` / `pull.rebase=true`
**Best Practice:** Default `simple` belassen (pusht nur den aktuellen Branch zum gleichnamigen Upstream).
`git config pull.ff only` (kein stiller Merge-Commit bei Divergenz) bzw. `pull.rebase true` für lineare Historie.
**Begründung:** Konservativste Variante gegen versehentliches Pushen/Merge-Rauschen.
**Quelle:** git-scm.com/docs/git-push, /git-pull · `offiziell`

## D2. Bei „fetch first": integrieren statt forcen, in Retry-Schleife
**Best Practice:** `git fetch` → eigenen Commit per Plumbing auf den frischen `origin/main` als Parent
setzen (`commit-tree -p origin/main`) → push; bei erneuter Ablehnung wiederholen. Mehrere Refs: `--atomic`.
**Begründung:** Git lehnt non-fast-forward ab, um fremde Commits nicht zu verlieren — integrieren bewahrt beide Seiten.
**Quelle:** docs.github.com „dealing with non-fast-forward errors", git-scm.com/docs/git-push (`--atomic`) · `offiziell`

## D3. Force nur mit explizitem Lease — nie nacktes `--force`/`--force-with-lease`
**Best Practice:** Wenn Force unvermeidbar: Save-Point-Ref VOR fetch/rebase setzen und
`--force-with-lease=<branch>:<save-ref>` mit EXPLIZITEM Wert pushen (+ `--force-if-includes`).
**Begründung:** Nacktes `--force-with-lease` wird durch ein vorheriges/automatisches `fetch`
ausgehebelt („interacts very badly with anything that implicitly runs git fetch"). Nur die explizite
`:<expect>`-Form ist nicht-experimentell und sicher.
**Quelle:** git-scm.com/docs/git-push (force-with-lease/force-if-includes) · `offiziell`

## D4. Serverseitige Absicherung: Block force pushes / linear history
**Best Practice:** Auf GitHub für `main` ein Ruleset „Block force pushes" (+ optional „Require linear
history") aktivieren; clientseitig `pre-push`-Hook als Netz.
**Begründung:** Verhindert serverseitig, dass ein fehlkonfiguriertes Skript fremde Commits aus `main` entfernt.
**Quelle:** docs.github.com „available rules for rulesets", git-scm.com/docs/githooks · `offiziell`

---

## E. Authentifizierung & Credentials

## E1. Fine-grained PAT, minimal scoped (oder GitHub-App-Token)
**Best Practice:** Fine-grained PAT auf das EINE Ziel-Repo, nur `contents:write` (+ implizit
`metadata:read`), mit kurzem Ablaufdatum. Für Automatisierung/Skalierung GitHub-App-Installation-Token
(1 h gültig, nutzer-entkoppelt) bevorzugen.
**Begründung:** Kleinster Blast-Radius bei Leak; least privilege.
**Quelle:** docs.github.com „Managing your PATs"/„Keeping API credentials secure" · `offiziell`

## E2. `store` nur als Notlösung; Helper-Wahl bewusst
**Best Practice:** OS-Secure-Storage/Git Credential Manager bevorzugen. `credential.helper store`
ist offiziell „discouraged" (Klartext); `cache` ist für PATs „inherently unsuitable" (geht beim
VM-/Daemon-Tod verloren — in einer ephemeren VM wertlos).
**Begründung:** Klartext-Token auf Platte ist die schwächste Option.
**Quelle:** git-scm.com/docs/git-credential-store, /git-credential-cache, /gitcredentials · `offiziell`

## E3. Credentials-Datei absichern
**Best Practice:** `.git/credentials` mit 0600, außerhalb des Stage-Bereichs (nie von `add -A` erfassbar),
Pfad in `.gitignore`; Token in eine VM-lokale Kopie übernehmen, nicht über den Mount stagen. Token rotieren,
ungenutzte löschen. `credential.useHttpPath true` für repo-genaues Matching.
**Begründung:** Filesystem-Permissions sind der einzige Schutz; verhindert versehentliches Mit-Committen.
**Quelle:** git-scm.com/docs/git-credential-store, /gitcredentials · `offiziell`

## E4. `safe.directory` bei fremdem Owner
**Best Practice:** Bei „dubious ownership" gezielt `git config --global --add safe.directory <pfad>` für
git-dir UND Work-Tree; Wildcard `'*'` nur bewusst auf Single-User-Maschine.
**Begründung:** Seit Git 2.35.2 (CVE-2022-24765) blockiert Git Repos mit fremdem Owner — über die
Mount-Brücke (Windows-Owner vs. VM-Nutzer) der Normalfall.
**Quelle:** NVD CVE-2022-24765, git-scm.com/docs/git-config · `offiziell`

---

## F. `.gitattributes` (Zeilenenden, Binär, LFS)

## F1. Eine committete `.gitattributes` im Repo-Root + `* text=auto`
**Best Practice:** `.gitattributes` (kein Suffix) im Root, committet, erste Zeile `* text=auto`.
Nach Einführung/Änderung: aus sauberem Baum `git add --renormalize . && git commit`. `merge.renormalize true`
gegen EOL-Merge-Konflikte.
**Begründung:** Gilt für alle unabhängig von lokalem `core.autocrlf`; verhindert „ganze Datei geändert"-Diffs.
**Quelle:** git-scm.com/docs/gitattributes, docs.github.com · `offiziell`

## F2. Bewährtes Start-Template (Windows/Linux, Shell + .NET + Bilder + ONNX/AAR-LFS)
```gitattributes
* text=auto
*.cs    text diff=csharp
*.sln   text eol=crlf
*.bat   text eol=crlf
*.cmd   text eol=crlf
*.sh    text eol=lf
*.bash  text eol=lf
*.ps1   text eol=crlf            # working-tree-encoding=UTF-16LE nur falls UTF-16 + alle Clients Git>=2.18
*.md    text diff=markdown
*.png   binary
*.jpg   binary
*.ico   binary
*.pdf   binary
*.onnx  filter=lfs diff=lfs merge=lfs -text
*.aar   filter=lfs diff=lfs merge=lfs -text
*.patch -text
.gitattributes export-ignore
.gitignore     export-ignore
```
**Begründung:** `eol=lf`/`eol=crlf` erzwingen das richtige Ende plattformübergreifend; `binary`
schützt Binärdateien vor Korruption; `-text` am LFS-Pointer + Patch-Dateien verhindert EOL-Umschreiben.
**Quelle:** git-scm.com/docs/gitattributes, docs.github.com, git-lfs-track-Manpage · `offiziell` (+ Template-Sammlung `extern`)

---

## G. Git-LFS-Workflow

## G1. `.gitattributes` ZUERST committen, Muster quoten, Bestand prüfen
**Best Practice:** `git lfs install` (einmalig), `git lfs track '*.onnx'` (Muster in Quotes!),
`.gitattributes` committen, DANN die Dateien. Danach mit `git lfs ls-files`/`git lfs status` verifizieren.
**Begründung:** Eine Datei wird erst beim Commit zum Pointer; ohne vorher committete Regel landet sie
als echter Blob in der History. Quotes verhindern, dass nur bestehende Dateien getrackt werden.
**Quelle:** git-lfs Wiki/Tutorial, docs.github.com „Configuring Git LFS" · `offiziell`

## G2. Altbestand ohne History-Rewrite konvertieren
**Best Practice:** `git lfs migrate import --no-rewrite "<datei>"` (neuer Commit, kein Force) oder
ab Git ≥2.16 `git add --renormalize .`. Nacktes `migrate import` (mit Rewrite) vermeiden bzw. nur
nach Backup + bewusstem Force-Push.
**Begründung:** Schützt vor destruktivem History-Rewrite und kaputten Klonen.
**Quelle:** git-lfs-migrate-Manpage · `offiziell`

## G3. `git lfs fsck --pointers` als Pre-Push-Guard
**Best Practice:** Vor dem Push `git lfs fsck --pointers` laufen lassen (prüft, dass Pointer kanonisch
sind und LFS-pflichtige Dateien wirklich Pointer sind). In Cowork zusätzlich LFS-Dateien per
`skip-worktree` schützen (Mount liefert sie als Vollinhalt). LFS-Pflege nur vom Windows-Rechner.
**Begründung:** Fängt versehentlich als Vollinhalt eingecheckte LFS-Dateien ab, bevor GitHubs
100-MiB-Limit den Push blockt.
**Quelle:** git-lfs-fsck-Manpage, docs.github.com „About large files" · `offiziell`

## G4. Quota/Bandbreite & schnelle Klone kennen
**Best Practice:** LFS-Quota im Blick behalten; `GIT_LFS_SKIP_SMUDGE=1 git clone` für Pointer-only-Klone,
dann gezielt `git lfs pull`. `git lfs prune` (nicht bei geteiltem `lfs.storage`).
**Begründung:** Vermeidet Quota-/Bandbreiten-Überraschungen und beschleunigt CI/Klon.
**Quelle:** git-lfs-config/-prune-Manpage, docs.github.com „Git LFS billing" · `offiziell`

---

## H. Cross-OS-Mount-Prinzipien (WSL/virtiofs)

## H1. Repo möglichst im nativen Linux-FS; sonst git-dir auf VM-Platte
**Best Practice:** Ideal liegt das Repo im VM-ext4. Muss der Arbeitsbaum auf dem Mount bleiben (weil
Windows-Tools dieselben Dateien brauchen), dann wenigstens das `.git`/git-dir auf die VM-Platte legen
(`--git-dir` lokal + `--work-tree` Mount, bzw. `git init --separate-git-dir`).
**Begründung:** Die I/O-intensive Git-Last (Objekte/Index/Refs) gehört aufs schnelle, stabile FS; nur
die Arbeitsdateien auf den Mount.
**Quelle:** Microsoft Learn „WSL filesystems", git-scm.com/docs/git-worktree · `offiziell`

## H2. Nie gleichzeitig von Host UND Guest dieselben Dateien schreiben
**Best Practice:** Datei-Lebenszyklus (create/edit/delete) immer nur von einer Seite. Linux-Dateien
nicht mit Windows-Apps über die Brücke ändern.
**Begründung:** Unterschiedliche Metadaten + delete+recreate-Speichern vieler Tools → Truncation,
Phantom-Löschung, Korruption.
**Quelle:** Microsoft DevBlog „Do not change Linux files using Windows apps and tools" · `offiziell`

## H3. Cache-Kohärenz: kein writeback, `cache=auto`/`none`; sauber beenden
**Best Practice:** writeback-/`cache=always`-Modi meiden (Konsistenz vor Performance); Sessions/Config
sauber mit `wsl --shutdown` abschließen.
**Begründung:** Aggressive Caches erzeugen genau das beobachtete „Versions-Flackern"/veraltete Reads.
**Quelle:** QEMU virtiofsd-Doku, Microsoft Learn „wsl-config" · `offiziell`

---

## Im Skript umgesetzt (Stand 2026-06-15, #46807)
`cowork-git.sh` setzt jetzt um: §B2 Performance-Configs (preloadIndex/index.version 4/skipHash/gc.auto 0);
§C4 `GIT_TERMINAL_PROMPT=0`; §D2 Non-Fast-Forward-Auto-Retry (git-interner 3-Wege-Merge, kein Force,
erhaelt fremde Commits); §E4 `safe.directory`-Vorsorge. fsmonitor (§B3) bewusst weiterhin AUS.

## Bezug: Best-Practice ↔ Bug-Almanach (`bugs/claude-tooling/cowork-git-push.md`)

| Best-Practice | adressiert Almanach-Abschnitt |
|---------------|-------------------------------|
| §A1, §A2, §C1–C3 | §1, §10, §11, §18 (Lock, Truncation, Stale, Plumbing) |
| §B1 | §4 (fileMode/Symlinks) |
| §B2, §B3 | §22, §14 (Performance, racy-git/Index) |
| §B4, §F1, §F2 | §8, §9 (CRLF, Binär/Encoding) |
| §C4, §E1–E3 | §2, §21 (Auth, Klartext-Token) |
| §D1–D3 | §15, §16, §17 (non-fast-forward, Force, Rebase) |
| §E4, §H1 | §19, §20 (GIT_DIR/Work-Tree, dubious ownership) |
| §G1–G4 | §6, §7 (Git-LFS) |
| §H2, §H3 | §10, §12, §13 (Datenverlust, writeback, case-insensitivity) |

## Ehrlichkeits-Hinweis
„offiziell" steht nur bei Beleg aus Hersteller-Doku (git-scm, GitHub, Microsoft, QEMU, git-lfs).
`extern`-Punkte (z. B. `index.skipHash`, Template-Sammlungen, der Plumbing-Replay als „Retry-Schleife")
sind sinnvolle Ableitungen/Community-Empfehlungen und überstimmen nie Offizielles. Cowork-spezifische
Schlüsse (git-intern committen, `cowork-git.sh`) sind aus den offiziellen Prinzipien abgeleitet.
