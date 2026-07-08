# Cowork Git-Push (KRITISCH -- nur in Claude Cowork)

## Die eine Regel
Aus Cowork NIEMALS nacktes `git commit`/`git push`, IMMER ueber `bash ~/proggs/cowork-git.sh` (Bruecke
zur Linux-VM flackert beim Lesen/Schreiben, kein Loeschen aus der VM).

## Nutzung
- `cowork-git.sh setup` -- origin/main holen, Push-Zugang pruefen (auf "Push-Zugang OK" warten).
- `cowork-git.sh push "#NNN - Text"` -- `add -A` + Waechter + commit + push (ALLE pending Dateien).
- `cowork-git.sh push-files "#NNN - Text" datei...` -- GEZIELT nur diese Dateien (**bevorzugt**).
- Env: `COWORK_ALLOW_SHRINK=1` (bewusst Schrumpfen/Loeschen), `COWORK_WORKTREE=<pfad>`.

## Datenverlust-Waechter
NACH Staging, VOR Commit: Byte-Groesse origin vs. Index je Datei. Schrumpfung (>30% UND >200 Byte) oder
Phantom-Loeschung -> ABBRUCH. Vergleich gegen origin (stabil) faengt Mount-Lese-Flackern. Gewollt:
`COWORK_ALLOW_SHRINK=1`.

## Weitere Mount-Fallen
Nicht loeschbare `.lock`; Datei-Modus 0755 (`core.fileMode false`); unlesbare Symlinks + Git-LFS
(skip-worktree, sonst >100 MB -> Reject); Build-Berge (`.gitignore`). Commits git-intern bauen; nach jedem
Schreiben Dateiende pruefen (`tail -1`/`wc -l`). Ein Shell-Aufruf ~45 s -> Push in EINEM Aufruf.

## Selbst-Check vor "gepusht"
setup lief?; ueber `cowork-git.sh`?; Waechter meldet keine Schrumpfung?; Commit nur Gewolltes (keine
LFS-Riesen/Build-Berge)?; GitHub bestaetigt (`git ls-remote` + Datei-ENDE geprueft)?
