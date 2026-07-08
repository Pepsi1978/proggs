# Cowork Git-Push (KRITISCH — nur in Claude Cowork)

> In Cowork laeuft Git ueber eine gemountete Bruecke (virtiofs/CBFS) zu einer Linux-VM — beim Lesen UND
> Schreiben kuerzlich geaenderter Dateien unzuverlaessig (Truncation, Padding, Versions-Flackern), kein
> Loeschen aus der VM.

## Die eine Regel
Aus Cowork NIEMALS nacktes `git commit`/`git push`, IMMER ueber `bash ~/proggs/cowork-git.sh`. Faengt
alle Mount-Fallen ab und bricht per Datenverlust-Waechter lieber ab als still Daten zu verlieren.

## Nutzung
- `cowork-git.sh setup` — origin/main holen, Push-Zugang pruefen (auf "Push-Zugang OK" warten).
- `cowork-git.sh push "#NNN - Text"` — `add -A` + Waechter + commit + push (ALLE pending Dateien).
- `cowork-git.sh push-files "#NNN - Text" datei…` — GEZIELT nur diese Dateien (Mount-schonend, **bevorzugt**).
- Env: `COWORK_ALLOW_SHRINK=1` (bewusstes Schrumpfen/Loeschen), `COWORK_WORKTREE=<pfad>`.

## Datenverlust-Waechter (wichtigster Schutz)
NACH Staging, VOR Commit: Byte-Groesse origin vs. Index je gestagete Datei. Verdaechtige Schrumpfung
(>30 % UND >200 Byte) oder Phantom-Loeschung → ABBRUCH. Vergleich gegen origin (stabil) faengt jedes
Mount-Lese-Flackern. Bewusst gewollt: `COWORK_ALLOW_SHRINK=1`.

## Weitere Mount-Fallen (abgefangen)
Nicht loeschbare `.lock` · Datei-Modus 0755 (`core.fileMode false`) · unlesbare Symlinks + Git-LFS
(skip-worktree, sonst >100 MB → Reject) · Build-Berge (`.gitignore`). Commits git-intern bauen; nach
jedem Schreiben Dateiende pruefen (`tail -1`/`wc -l` — Mount schneidet oft ab). Ein Shell-Aufruf ~45 s,
Hintergrundprozesse ueberleben nicht → Push in EINEM Aufruf.

## Selbst-Check vor "gepusht"
setup lief? · ueber `cowork-git.sh` (nicht nackt)? · Waechter meldete keine Schrumpfung? · Commit nur
Gewolltes (keine LFS-Riesen/Build-Berge)? · auf GitHub bestaetigt (`git ls-remote` + Datei-ENDE geprueft)?
