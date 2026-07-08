# Cowork Git-Push (KRITISCH — nur in Claude Cowork)

> In Cowork laeuft Git NICHT direkt auf dem Windows-Ordner, sondern ueber eine gemountete Bruecke
> (virtiofs/CBFS) zu einer Linux-VM. Die Bruecke ist beim Lesen UND Schreiben kuerzlich geaenderter
> Dateien unzuverlaessig (Truncation, Padding, Versions-Flackern) und erlaubt kein Loeschen aus der VM.

## Die eine Regel

Aus Cowork NIEMALS nacktes `git commit`/`git push`, IMMER ueber `bash ~/proggs/cowork-git.sh`.
Das Skript faengt alle Mount-Fallen ab und bricht per Datenverlust-Waechter lieber ab als still Daten zu verlieren.

## Nutzung

- `bash cowork-git.sh setup` — origin/main holen, Push-Zugang pruefen. Abwarten bis "Push-Zugang OK".
- `bash cowork-git.sh push "#NNN - Text"` — `add -A` + Waechter + commit + push (ALLE pending Dateien).
- `bash cowork-git.sh push-files "#NNN - Text" datei1 datei2` — committet GEZIELT nur diese Dateien
  (Mount-schonend). **Bevorzugter Weg** fuer saubere, eng begrenzte Commits.
- Env: `COWORK_ALLOW_SHRINK=1` (bewusstes Schrumpfen/Loeschen erlaubt), `COWORK_WORKTREE=<pfad>`.

## Datenverlust-Waechter (wichtigster Schutz)

Laeuft NACH dem Staging, VOR dem Commit: vergleicht Byte-Groesse origin vs. Index je gestagete Datei.
Verdaechtige Schrumpfung (>30 % UND >200 Byte) oder Phantom-Loeschung (als geloescht gestaged, existiert
aber noch) → ABBRUCH, kein Commit. Vergleich gegen origin (stabil) faengt jedes Mount-Lese-Flackern.
Bewusst gewollt: `COWORK_ALLOW_SHRINK=1` voranstellen.

## Weitere abgefangene Mount-Fallen

Nicht loeschbare `.lock` (git-dir auf VM-ext4; `rm -f ~/.cowork-gitdir/proggs/*.lock`) · Datei-Modus
0755 (`core.fileMode false`) · unlesbare Symlinks + Git-LFS-Vollinhalte (skip-worktree, sonst >100 MB →
GitHub-Reject) · Build-Berge (`**/build/ **/.gradle/ **/node_modules/` in `.gitignore`).

## Mount NIE blind vertrauen

Commits git-intern bauen (`git show <ref>:<datei>` → in /tmp aendern → `git hash-object -w` →
`git update-index --cacheinfo`/`--force-remove` → `git commit-tree` → `git update-ref` → push). Nach
JEDEM Schreiben das DATEIENDE pruefen (`tail -1`, `wc -l`) — der Mount schneidet oft das Ende ab.
Loeschen/Umbenennen aus der VM ist gesperrt → git-intern aus dem Tree nehmen.

## Weitere Fakten

Ein Shell-Aufruf ~45 s, Hintergrundprozesse ueberleben nicht → Push in EINEM Aufruf. "fetch first"/
Non-Fast-Forward: Plumbing (read-tree → add → write-tree → commit-tree → update-ref → push), weil
`git rebase` am unsauberen Mount-Arbeitsbaum scheitert. Token: `~/proggs/.git/credentials`, nie ins Log.
`do_push_with_retry` setzt bei Ablehnung per git-internem 3-Wege-Merge (KEIN Force) auf frischen origin auf.

## Selbst-Check vor "gepusht"

setup lief ("Push-Zugang OK")? · ueber `cowork-git.sh` (nicht nackt)? · Waechter meldete keine
verdaechtige Schrumpfung? · Commit enthaelt NUR Gewolltes (keine LFS-Riesen, keine Build-Berge)? ·
auf GitHub bestaetigt (`git ls-remote origin -h refs/heads/main` + Datei-ENDE geprueft)?
