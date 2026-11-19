# Agenten-Shells: Aliase und Warte-Schleifen, die ewig hängen

Stand: 15.11.2026 (macOS, zsh, Claude Code mit Shell-Schnappschuss, eza 0.2x)

## Bug 1: `ls`-Alias auf eza hängt in Agenten-Shells
- **Symptom:** Ein fertiger Gradle-Bau (APK nach wenigen Minuten da) lief 10 Minuten bis in den Timeout des Bash-Werkzeugs, danach 20 Minuten im Hintergrund weiter.
- **Root Cause:** `~/.zshrc` setzte `alias ls='eza …'`. Claude Code übernimmt Aliase in den Shell-Schnappschuss jeder Sitzung. `ls -t <datei> | head -1` wird zu `eza -t <datei>`; bei eza ist `-t` „--time FELD“, der Pfad wird als Feld verschluckt, eza hat keinen Pfad mehr und liest Dateinamen von der Standardeingabe. Die ist beim Agenten eine offene Pipe: eza wartet ewig, ohne Fehlermeldung.
- **Fix (Stufe 3, Eliminierung):** Die Komfort-Aliase (`ls`, `ll`, `cat`→bat, `tree`) stehen in `~/.zshrc` nur noch, wenn die Shell interaktiv ist, stdin und stdout ein Terminal sind und keine Agenten-Variable gesetzt ist (`CLAUDECODE`, `CODEX_SANDBOX`, `OPENCODE`, `JARVIS_AUFTRAG_ID`). Gilt für neue Sitzungen; eine laufende Sitzung behält ihren alten Schnappschuss.
- **Muster-Erkennung:** Ein Befehl hängt ohne Ausgabe, obwohl die eigentliche Arbeit (Bau, Datei) fertig ist → `ps -o pid,etime,command` auf die Kindprozesse; ein Werkzeug mit 0 % CPU wartet auf stdin. Im Zweifel `command ls` bzw. `\ls` oder `</dev/null` verwenden.

## Bug 2: Warte-Schleife mit `pgrep -f` findet sich selbst
- **Symptom:** `until … ! pgrep -f "gradlew -q assembleDebug"; do sleep 5; done` endete nie.
- **Root Cause:** `pgrep -f` durchsucht die ganze Befehlszeile. Die Schleife selbst (und die wartende Eltern-Shell) enthalten genau diesen Text, also gilt der Bau immer als „läuft noch“.
- **Fix:** Keine eigenen Warte-Schleifen um einen Bau. Den Bau direkt im Vordergrund starten (`bash gradlew assembleDebug -q`, ohne Pipe in `head`), ist er länger, schiebt ihn das Werkzeug in den Hintergrund und meldet sich beim Ende selbst. Muss doch gewartet werden: auf das Ergebnis prüfen (APK-Zeitstempel neuer als der Start, `output-metadata.json`), nie auf Prozessnamen.

## Verwandte Fallen (geprüft)
- `cat`→`bat`: im Nicht-Terminal unkritisch, aber ebenfalls nur noch im echten Terminal aktiv.
- `Jarvis/gradlew` ohne Ausführungsrecht: immer `bash gradlew`; eine Pipe auf grep verschluckt den Fehler, deshalb die APK mit aapt2 prüfen (siehe Gedächtnis „OpenLauncher Server“).

## Absicherung gegen lange Wartezeiten (alle Plattformen, 15.11.2026)
- **Zeitlimit 60 s im Minutentakt:** Bash über `BASH_DEFAULT_TIMEOUT_MS`/`BASH_MAX_TIMEOUT_MS` = 60000 in allen Profil-`settings.json`, PowerShell über `build-ablauf-guard.py`. Claude Code schiebt den Befehl dann in den Hintergrund (Ausnahme: Befehle, die mit `sleep` beginnen, werden abgebrochen). Jede weitere Minute sichtbar: `Werkzeuge/lauf-waechter/minute.py` (FERTIG über `[exited with code N]`, CPU-Zeit-Delta statt Durchschnitt, WARTET bei Dialog/Overlay/sleep). Codex, OpenCode und Co. brechen beim Zeitlimit ab: dort lange Befehle selbst mit `&` und Log starten. Hinweise an das Modell nur per `additionalContext`, `permissionDecisionReason` sieht bei allow nur der Nutzer.
- **Warteschleifen:** Der Wächter blockt `until/while … pgrep -f "<text im eigenen Befehl>"` (auch mehrzeilig); Klammer-Trick, `^`/`$`-Anker und Variablen bleiben erlaubt.
- **Alte Schnappschüsse:** Die befristete Umschreibung `unalias …; eval '<befehl>'` im Bau-Wächter ist seit 19.11.2026 entfernt: Kein Mac-Schnappschuss enthält die Aliase noch. Taucht wieder einer auf, gilt: Ein `unalias` im selben Befehl wirkt nicht (Aliase werden beim Einlesen ersetzt), erst ein inneres `eval` danach.
- **Messwerte Mac:** Jarvis-Bau ohne Änderung 0,5 s, mit Änderung unter 1 Minute (warmer Daemon, Vorwärmen aktiv). Deutlich länger = Fehler, sofort nachsehen.
