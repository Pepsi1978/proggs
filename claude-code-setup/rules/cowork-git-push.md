# Cowork Git-Push (KRITISCH — verbindlich in Claude Cowork)

> Adressat: Claude, wenn es in **Cowork** (Desktop-App, Linux-Hilfsumgebung auf Windows) arbeitet.
> In Cowork läuft Git NICHT direkt auf dem Windows-Ordner, sondern über eine gemountete Brücke
> (virtiofs/CBFS) zu einer kleinen Linux-VM. Diese Brücke verzerrt, wie Dateien aussehen, und
> bringt mehrere Fallen mit. Diese Regel hält fest, wie aus Cowork zuverlässig committet/gepusht wird.
> Gesetzt 2026-06-15, nachdem ein echter Testpush fünf Probleme nacheinander aufgedeckt hat.

## Die eine Regel
**Aus Cowork wird NIEMALS mit nacktem `git commit`/`git push` gearbeitet, sondern IMMER über
`bash ~/proggs/cowork-git.sh`.** Das Skript fängt alle bekannten Mount-Fallen ab. Nacktes git
aus der VM hängt an nicht löschbaren Locks, bläht den Commit mit Schein-Änderungen auf oder sprengt
GitHubs 100-MB-Limit.

## Nutzung
- `bash cowork-git.sh setup` — holt den aktuellen Stand von origin/main, prüft den Push-Zugang.
  Abwarten bis **„Push-Zugang OK"** erscheint.
- `bash cowork-git.sh push "#NNN - Text"` — `add -A` + commit + push nach origin/main.
- `bash cowork-git.sh <git-befehl>` — beliebiger git-Befehl mit korrektem git-dir/work-tree
  (z. B. `status`, `log`, `diff`).

## Warum ein eigenes Skript (Grundproblem)
Der git-Maschinenraum (Index, Locks, Objekte) liegt bewusst auf der **VM-eigenen ext4-Platte**
(`~/.cowork-gitdir/proggs`), nur der Arbeitsbaum bleibt auf der gemounteten Brücke. Grund: Auf der
Brücke kann git seine `.lock`-Dateien nicht wieder löschen („Operation not permitted") — dadurch
hängt sonst jeder commit/push. Das normale `.git` im Windows-Ordner bleibt unberührt; beide Welten
stören sich nicht. Quelle der Wahrheit ist origin/main; das VM-git-dir wird je Session frisch aus
GitHub aufgebaut und überlebt VM-Neustarts nicht (gewollt).

## Die 5 Mount-Fallen (alle vom Skript abgefangen)
1. **Nicht löschbare `.lock`-Dateien** → git-dir liegt auf der VM-ext4-Platte (löschen geht dort).
   Liegt doch mal ein `index.lock` herum (z. B. nach einem abgebrochenen Lauf): einfach
   `rm -f ~/.cowork-gitdir/proggs/*.lock` — gefahrlos, weil VM-lokal.
2. **Datei-Modus immer 0755 gemeldet** → ohne Gegenmaßnahme sähe git bei JEDER Datei einen
   644→755-Wechsel. Fix: `git config core.fileMode false` (setzt das Skript in `ensure_setup`).
3. **Unlesbare Symlinks** (`readlink: Input/output error`, z. B. Skill-Verknüpfungen) → `git add -A`
   bricht sonst mit „unable to index file … updating files failed" ab. Fix: `guard_unreadable_symlinks`
   nimmt genau die unlesbaren, bereits getrackten Symlinks per `skip-worktree` aus.
4. **Git-LFS-Dateien erscheinen als Vollinhalt** → in origin liegen `*.onnx`/`*.aar` (siehe
   `.gitattributes`, `filter=lfs`) nur als ~130-Byte-Zeiger, über die Brücke aber als echte
   Riesendateien (bis ~262 MB). Ohne Schutz ersetzt `add -A` die Zeiger durch die Riesendateien →
   GitHub lehnt ab (100-MB-Limit) und das Repository bläht auf. Fix: `guard_lfs_pointers` nimmt alle
   getrackten LFS-Dateien per `skip-worktree` aus. Echte LFS-Pflege passiert vom Windows-Rechner, nie aus Cowork.
5. **Nicht ignorierte Build-/Abhängigkeits-Berge** → `**/build/`, `**/.gradle/`, `**/node_modules/`
   sind in `.gitignore`, sonst zieht `add -A` Zehntausende erzeugte Dateien mit (gemessen: 22.881)
   und wird unbrauchbar langsam. Bei neuen Projekten/Sprachen: passende Build-Ordner ergänzen.

## Praktische Hinweise für Cowork
- **Laufzeit-Grenze:** Ein Cowork-Shell-Aufruf läuft max. ~45 s, und Hintergrundprozesse überleben
  den Wechsel zwischen zwei Shell-Aufrufen NICHT zuverlässig. Der `push` muss daher in EINEM Aufruf
  durchlaufen. Voraussetzung dafür ist, dass Falle 4 + 5 greifen (sonst dauert `add -A` zu lange).
- **„fetch first" / Non-Fast-Forward:** Wandert origin/main während der Arbeit weiter, wird der Push
  abgelehnt. Dann neuen Stand holen und den eigenen Commit sauber oben aufsetzen. Da der gemountete
  Arbeitsbaum aus git-Sicht „unsauber" ist (CRLF/LFS), funktioniert ein normaler `git rebase` oft
  nicht. Bewährtes, worktree-schonendes Verfahren (Plumbing):
  ```bash
  GITDIR="$HOME/.cowork-gitdir/proggs"; export GIT_DIR="$GITDIR" GIT_WORK_TREE="$HOME/proggs"
  git config core.fileMode false
  git fetch -q origin main
  git read-tree origin/main                 # Index = origin-Stand, ohne Worktree-Checkout
  git add -- <nur die gewollten Dateien>    # genau die beabsichtigten Änderungen stagen
  TREE=$(git write-tree)
  NEW=$(git commit-tree "$TREE" -p origin/main -m "#NNN - Text")
  git update-ref refs/heads/main "$NEW"
  git push origin "$NEW":main
  ```
- **Scoped statt alles:** `cowork-git.sh push` macht `add -A` und nimmt damit ALLE noch nicht
  committeten Dateien mit (inkl. liegengebliebener Logs, Temp-DBs, Screenshots). Wenn nur eine
  gezielte Änderung gepusht werden soll, das Plumbing-Verfahren oben mit den konkret gewünschten
  Dateien nutzen — keinen „Test"-Commit mit fremden Dateien fluten.
- **Token:** Der Push-Token liegt persistent in `~/proggs/.git/credentials`
  (Format `https://Pepsi1978:<TOKEN>@github.com`) — siehe `COWORK-GIT-PUSH-SETUP.md`. Niemals roh ins Log.

## Selbst-Check vor „gepusht"
- [ ] `setup` lief, „Push-Zugang OK" gesehen?
- [ ] Push über `cowork-git.sh` (nicht nacktes git)?
- [ ] Commit enthält NUR Gewolltes (keine LFS-Riesendateien, keine Build-Berge, keine fremden Reste)?
- [ ] Auf GitHub bestätigt (`git ls-remote origin -h refs/heads/main` zeigt den neuen Commit)?
