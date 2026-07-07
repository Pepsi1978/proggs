# Cowork Git-Push (KRITISCH — verbindlich in Claude Cowork)

> Adressat: Claude, wenn es in **Cowork** (Desktop-App, Linux-Hilfsumgebung auf Windows) arbeitet.
> In Cowork läuft Git NICHT direkt auf dem Windows-Ordner, sondern über eine gemountete Brücke
> (virtiofs/CBFS) zu einer kleinen Linux-VM. Diese Brücke ist beim **Lesen UND Schreiben** kürzlich
> geänderter Dateien unzuverlässig (Truncation, Padding, Versions-Flackern, Linter-Interferenz) und
> erlaubt **kein Löschen/Umbenennen** aus der VM. Diese Regel hält fest, wie aus Cowork zuverlässig
> committet/gepusht wird, OHNE Daten zu verlieren.
> Gesetzt 2026-06-15; gehärtet nach mehreren echten Test-Pushes (Datenverlust-Wächter, push-files).

## Die eine Regel
**Aus Cowork wird NIEMALS mit nacktem `git commit`/`git push` gearbeitet, sondern IMMER über
`bash ~/proggs/cowork-git.sh`.** Das Skript fängt alle bekannten Mount-Fallen ab und hat einen
Datenverlust-Wächter, der einen Commit lieber ABBRICHT als stillen Datenverlust zuzulassen.

## Nutzung
- `bash cowork-git.sh setup` — holt origin/main, prüft Push-Zugang. Abwarten bis **„Push-Zugang OK"**.
- `bash cowork-git.sh push "#NNN - Text"` — `add -A` + Wächter + commit + push (nimmt ALLE pending Dateien mit).
- `bash cowork-git.sh push-files "#NNN - Text" datei1 datei2` — committet **gezielt nur diese Dateien**
  (Mount-schonend: Index = origin, dann nur die genannten Pfade) + Wächter. **Bevorzugter Weg** für
  saubere, eng begrenzte Commits — flutet den Commit nicht mit fremden pending Dateien.
- `bash cowork-git.sh <git-befehl>` — beliebiger git-Befehl mit korrektem git-dir/work-tree.

Umgebungsvariablen:
- `COWORK_ALLOW_SHRINK=1` — erlaubt bewusstes Schrumpfen/Löschen (Wächter wird übersprungen).
- `COWORK_SHRINK_MAX_PCT` (Default 30), `COWORK_SHRINK_MIN_BYTES` (Default 200) — Empfindlichkeit des Wächters.
- `COWORK_WORKTREE=<pfad>` — Arbeitsbaum überschreiben. Nötig, wenn das Skript wegen Mount-Flackern
  aus einer **stabilen VM-Kopie** ausgeführt wird, aber auf den proggs-Mount zeigen soll.

## Warum ein eigenes Skript (Grundproblem)
Der git-Maschinenraum (Index, Locks, Objekte) liegt bewusst auf der **VM-eigenen ext4-Platte**
(`~/.cowork-gitdir/proggs`), nur der Arbeitsbaum bleibt auf der gemounteten Brücke. Auf der Brücke
kann git seine `.lock`-Dateien nicht löschen („Operation not permitted"). Das normale `.git` im
Windows-Ordner bleibt unberührt. Quelle der Wahrheit ist origin/main; das VM-git-dir wird je Session
frisch aus GitHub aufgebaut.

## Die Mount-Fallen (alle vom Skript abgefangen)
1. **Nicht löschbare `.lock`** → git-dir auf VM-ext4. Liegen gebliebener Lock:
   `rm -f ~/.cowork-gitdir/proggs/*.lock` (VM-lokal, gefahrlos).
2. **Datei-Modus immer 0755** → `git config core.fileMode false` (sonst 644→755-Diff bei JEDER Datei).
3. **Unlesbare Symlinks** (`readlink: I/O error`) → `guard_unreadable_symlinks` (skip-worktree).
4. **Git-LFS-Dateien als Vollinhalt** → in origin nur ~130-Byte-Zeiger, über die Brücke echte
   Riesendateien (bis ~262 MB). `guard_lfs_pointers` (skip-worktree); sonst >100 MB → GitHub lehnt ab.
5. **Build-/Abhängigkeits-Berge** → `**/build/`, `**/.gradle/`, `**/node_modules/` in `.gitignore`
   (sonst zieht `add -A` Zehntausende erzeugte Dateien mit — gemessen: 22.881).
6. **DATENVERLUST durch Mount-Lesefehler (wichtigster Schutz)** → `guard_data_loss` läuft NACH dem
   Staging, VOR dem Commit. Für jede gestaged + in origin existierende Datei wird die **Byte-Größe
   origin vs. Index** verglichen. Verdächtige Schrumpfung (Default: >30 % UND >200 Byte) oder eine
   **Phantom-Löschung** (als gelöscht gestaged, existiert aber noch im Worktree) → **ABBRUCH, kein
   Commit/Push**. Der Vergleich geht gegen origin (stabil), fängt also jedes Mount-Lese-Flackern.
   Bewusst gewollt: `COWORK_ALLOW_SHRINK=1` voranstellen.
7. **Mount-Schreib-/Lösch-Unzuverlässigkeit** → Schreibvorgänge (Edit/Write-Tool UND teils `cat >`)
   sind bei größeren Dateien unvollständig/verzögert sichtbar; Dateien lassen sich aus der VM nicht
   löschen/umbenennen. Deshalb: Commits **git-intern** bauen (siehe unten), Löschungen git-intern aus
   dem Tree nehmen, und das Skript bei Bedarf aus einer **stabilen VM-Kopie** ausführen.

## Praktische Hinweise für Cowork
- **Datenoperationen git-intern statt über den Mount** — der robusteste Weg, weil er die Brücke
  komplett umgeht. Datei ändern:
  ```bash
  GITDIR="$HOME/.cowork-gitdir/proggs"; export GIT_DIR="$GITDIR" GIT_WORK_TREE="$HOME/proggs"
  git fetch -q origin main
  git show origin/main:PFAD > /tmp/x            # vollständige, stabile Ausgangsversion
  # ... /tmp/x in /tmp bearbeiten, DATEIENDE prüfen (tail -1, wc -l) ...
  BLOB=$(git hash-object -w /tmp/x)
  git read-tree origin/main
  git update-index --cacheinfo 100644,$BLOB,PFAD     # Datei ändern/hinzufügen
  # git update-index --force-remove PFAD             # Datei löschen (Mount erlaubt kein rm)
  TREE=$(git write-tree)
  NEW=$(git commit-tree "$TREE" -p origin/main -m "#NNN - Text")
  git update-ref refs/heads/main "$NEW"; git push origin "$NEW":main
  ```
- **Nach JEDEM Schreiben das DATEIENDE prüfen** (`tail -1`, `wc -l`) — nicht nur den Anfang. Der
  klassische Mount-Fehler schneidet das Dateiende ab (so gingen schon ~420 Zeilen verloren).
- **Laufzeit-Grenze:** Ein Cowork-Shell-Aufruf läuft max. ~45 s, Hintergrundprozesse überleben den
  Wechsel zwischen Aufrufen NICHT. Push muss in EINEM Aufruf durchlaufen (Fallen 4 + 5 sind Pflicht,
  sonst ist `add -A` zu langsam).
- **„fetch first" / Non-Fast-Forward:** origin/main wandert oft weiter. Neuen Stand holen und den
  eigenen Commit per Plumbing (oben) sauber oben aufsetzen — `git rebase` scheitert am unsauberen
  Mount-Arbeitsbaum (CRLF/LFS).
- **Token:** persistent in `~/proggs/.git/credentials` — niemals roh ins Log.

## Robustheit im Skript (Stand 2026-06-15, #46807)
`ensure_setup` setzt zusaetzlich: `core.preloadIndex true`, `index.version 4`, `index.skipHash true`
(ab Git 2.40), `gc.auto 0`, `safe.directory` fuer git-dir UND work-tree, sowie `GIT_TERMINAL_PROMPT=0`
(kein Haengen an Auth-Prompts). fsmonitor/untrackedCache bewusst AUS (auf FUSE unzuverlaessig).

**Falle 8 — origin wandert (Non-Fast-Forward):** `do_push_with_retry` setzt den eigenen Commit bei
Ablehnung automatisch per **git-internem 3-Wege-Merge** (kein Worktree, KEIN Force) auf den frischen
origin-Stand auf und pusht erneut — bis `COWORK_PUSH_RETRIES` (Default 3). Fremde Commits bleiben
erhalten; bei echtem Konflikt (gleiche Datei beidseitig) sauberer Abbruch.

## Selbst-Check vor „gepusht"
- [ ] `setup` lief, „Push-Zugang OK" gesehen?
- [ ] Über `cowork-git.sh` gepusht (nicht nacktes git)?
- [ ] Wächter meldete „keine verdächtigen Schrumpfungen/Löschungen" (oder Schrumpfung war bewusst)?
- [ ] Commit enthält NUR Gewolltes (keine LFS-Riesendateien, keine Build-Berge, keine fremden Reste)?
- [ ] Auf GitHub bestätigt: `git ls-remote origin -h refs/heads/main` + Datei-Zeilenzahl/-ENDE geprüft?
