# Windows-Terminals über WSL/tmux

OpenLauncher bietet unabhängig von Modell, Anbieter, CLI, Profil und Modus die Option
**tmux** neben den Startbuttons. Windows-CLIs und ihre bisherigen PowerShell-Startskripte
laufen innerhalb von WSL-Interop; nur der Terminalmultiplexer läuft in Linux. Anmeldung,
Windows-Pfade, Hooks, Build-Werkzeuge und Profile bleiben in Windows. Kein Linux-Claude
installieren und keine Anmeldedaten in ein neues Linux-Profil kopieren.

Voraussetzungen: eine Benutzer-Distribution als WSL-Standard, tmux darin, aktivierte
Windows-Interop. OpenLauncher prüft die Distribution und bindet jeden neuen Start an
eine neue `openlauncher-<ID>`-Sitzung auf Socket `openlauncher`. Derselbe kopierte
Startbefehl hängt dieselbe laufende Sitzung wieder an. Ein neuer Startklick erzeugt eine
neue Sitzung. `Strg+B`, danach `D`, trennt die Anzeige; `exit` bzw. CLI-Ende beendet
die Sitzung. Der temporäre Wrapper bleibt zum Wiederanhängen erhalten.

## Zielbindung

1. Den vollständigen Startbefehl/Wrapper der beauftragten sichtbaren Sitzung zuordnen.
   Windows-tmux nicht allein anhand von „rechts“, Pane `%0` oder Prozessname `init` wählen.
2. Distribution aus dem aktuellen Wrapper übernehmen. Alle WSL-Aufrufe mit `--exec`
   ausführen: ohne diesen Schalter kann die Linux-Shell Windows-Backslashes auswerten.
3. Den gemeinsamen Helfer aus `../../ins-macos-terminal-einfuegen/scripts/tmux_bridge.py`
   mit WSL-`python3` aufrufen. Windows-Pfade über `wsl -d <Distribution> --exec wslpath -u`
   übersetzen. Socket mit `tmux -L openlauncher display-message -p '#{socket_path}'`
   ermitteln und dem Helfer über `--socket` übergeben. Seine allgemeinen Transport-,
   Stopp-, Frische- und Zustellungsregeln aus dem macOS-Skill gelten ebenso hier.
4. Besonderheit: Linux sieht die Windows-PowerShell als Interop-Prozess (`init`), nicht
   deren Windows-Claude-Kindprozess. Aus dem eindeutig zugeordneten Wrapper, Pane-CWD,
   Windows-Prozessbaum (PowerShell-Kommando mit genau diesem inneren Startskript und
   zugehöriger CLI) und frischem Pane-Inhalt gemeinsam die Zuordnung prüfen. In `bind`
   die Linux-Interop-PID als `--agent-pid` verwenden. Sie lebt bis zum Ende der
   Windows-PowerShell; das Startskript kehrt nicht in eine interaktive Shell zurück.
   Es handelt sich ausdrücklich nicht um eine Linux-Claude-PID. Bei anderer Startform,
   Prozesswechsel oder fehlender Zuordnung nicht schreiben.
5. Privaten Dialogordner auf dem **Linux-Dateisystem** mit `mktemp -d` erzeugen
   (0700). Nicht unter `/mnt/c`: Windows-Mounts bilden Linux-Dateirechte nicht immer ab.
   UTF-8-Aufträge dorthin kopieren, Hash berechnen, frisch `read`, dann genau einmal
   `submit` beziehungsweise `paste`/`enter`. Keine künstlichen Probeaufträge ohne Auftrag.

Ubuntu-tmux 3.6 meldet hier kein `bracket_paste_flag`. Deshalb unter diesem Windows-Weg
`--literal-line` bei `paste`/`submit` verwenden: ausschließlich eine echte Textzeile,
ohne Tabs oder Steuerzeichen. Der Helfer sendet sie mit `send-keys -l` ohne Shell und
ohne eingebettetes Enter; alle Identitäts-, Hash-, Frische- und Duplikatprüfungen bleiben
aktiv. Mehrzeilige Originalaufträge unverändert in einer gemeinsamen UTF-8-Datei ablegen
und deren Windows-Pfad in einer kurzen einzeiligen Anweisung übergeben. Nicht still
abflachen und keinen Paste-Modus vortäuschen. Bei einem umbrochenen Entwurf kann `submit`
konservativ bei `pasted` anhalten: frisch lesen, den eigenen vollständigen Entwurf
prüfen, dann bei autorisiertem Absenden einmal `enter` mit dem neuen Token.

Claude unter Windows verwendet aktuell `>` statt `❯`; der gemeinsame Parser akzeptiert
beide Zeichen nur innerhalb des vollständigen Eingaberahmens. Fremde Entwürfe, laufende
Generierung, Freigabedialoge, Ghost-Vorschläge und unklare Pasteblöcke bleiben Stopper.
Ein tmux-Transporterfolg ist noch keine Claude-Antwort: danach die tatsächliche neue
Antwort lesen. Die Zuordnung zum sichtbaren Codex-Tab zusätzlich prüfen; tmux kennt
keine Codex-Tab-ID.

Für einen laufenden Dialog gelten die Koordinations- und Abschlussregeln des
übergeordneten Skills. Die Windows-tmux-Einrichtung autorisiert keine selbstständigen
Programmieraufträge an andere CLIs.
