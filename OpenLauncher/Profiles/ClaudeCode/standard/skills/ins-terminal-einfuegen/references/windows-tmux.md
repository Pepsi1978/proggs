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
   UTF-8-Aufträge dorthin kopieren, Hash berechnen, anhand eines aktuellen `read` genau einmal
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

Claude unter Windows kann je nach Version `>` oder `❯` anzeigen; der gemeinsame Parser
akzeptiert beide Zeichen nur innerhalb des vollständigen Eingaberahmens. Fremde Entwürfe, laufende
Generierung, Freigabedialoge, Ghost-Vorschläge und unklare Pasteblöcke bleiben Stopper.
Ein tmux-Transporterfolg ist noch keine Claude-Antwort: danach die tatsächliche neue
Antwort lesen. Die Zuordnung zum sichtbaren Codex-Tab zusätzlich prüfen; tmux kennt
keine Codex-Tab-ID.

Für einen laufenden Dialog gelten die Koordinations- und Abschlussregeln des
übergeordneten Skills. Die Windows-tmux-Einrichtung autorisiert keine selbstständigen
Programmieraufträge an andere CLIs.

## Schneller Dialog und klare Rollen

Die Regeln für Auftragskennung, Nutzerzwischenrufe, Abschluss und Stopp aus
[dem macOS-Skill](../../ins-macos-terminal-einfuegen/SKILL.md) gelten auch hier.
Für einen ausdrücklich beauftragten Verbesserungsloop bis zum manuellen Stopp
weiterarbeiten; Voice-Ende und eine Zwischenfrage sind kein Stopp. Keine geplante
Automation dafür anlegen. Ohne neuen sinnvollen Befund keine kosmetischen Updates erzeugen.

- Rollen im Auftrag festlegen: Der Nutzer steuert, Codex koordiniert und prüft,
  Claude diskutiert oder implementiert gemäß Auftrag. Codex darf seinen Kommunikationsskill
  selbst bearbeiten; dann Claude ausdrücklich nur zur Gegenprüfung einsetzen.
  Pro Datei ein Schreibender, Git-Mutationen nacheinander.
- Übergaben kurz: `R2: Ziel; relevante Änderung seit R1; Grenzen; erwarteter Nachweis`.
  Keine eigene Empfangsbestätigungsrunde. Für kleine Fragen direkte Antworten;
  bei Code Fundstellen, Tests, offene Punkte und später Commit/Push nennen lassen.
- Textdatei und SHA vor dem Senden vorbereiten. Ein gerade inhaltlich geprüfter
  Warte-Read mit bekanntem Antwortkontext und Eingabezustand ist bereits das nötige
  frische `read`: dessen Token direkt für `submit --literal-line` verwenden, ohne
  routinemäßigen zweiten Leseaufruf. Nach Eingabe oder Scrollen im Zielterminal, langer Zwischenarbeit,
  unbekanntem Zustand oder `E_STALE` erneut lesen; keine blinde automatische Wiederholung.
  Der Helfer erkennt bekannte zweispaltige Fortsetzungszeilen,
  ohne Leerraum allgemein zu entfernen. Unklare Darstellung bleibt bei `pasted`.
  Keine erneute Einfügung und kein blindes Enter nach unklarem Transport.
- Bei live bestätigtem Titelverhalten während Claudes Arbeit
  `read --compact --wait-mode status --wait 10` verwenden. Direkt nach dem Senden
  bis zum belegten Arbeitsbeginn bzw. bei unbekanntem Titelverhalten mit
  `read --compact --wait 3` starten. [Puffer sparsam lesen](../../ins-macos-terminal-einfuegen/references/puffer-lesen.md)
  erklärt Auszüge, Vollansicht und Grenzen. Bei fehlendem Kontext einmal `--force-view`.
  Kein vollständiger Bildschirm und keine vollständige Skill-Lektüre pro Nachricht.
- Review an Dateimeilensteinen: zuerst Status und betroffene Pfade, dann deren echten
  staged/unstaged Diff sowie neue Dateien. Gegen den zuletzt geprüften Inhalt vergleichen;
  Commitwechsel beachten. Keine Repo-Gesamtscans pro Terminalabruf. Claudes Kurzbericht
  dient als Wegweiser, ersetzt aber weder Diff noch Testbeleg.
- Zwischen jedem Lesen und Senden neu eingetroffene Nutzeranweisungen berücksichtigen.
  Fragmente sammeln, Transkriptdopplungen vermeiden. Fremde Entwürfe stehen lassen.
  Scrollmodus ist keine Eingabebereitschaft: nicht automatisch verlassen und absenden.

### Vorschlag ist nicht automatisch ein Nutzerentwurf

Claude kann nach einer Antwort einen blassen nächsten Auftrag anzeigen. Die reine
tmux-Textansicht kann diesen Ghost-Vorschlag wie eingegebenen Text darstellen; unter
Windows/Interop fehlen dort mitunter auch bei `capture-pane -e` die Stilmerkmale.
Text bei Cursorposition 2 ist nur ein Hinweis, kein Beweis: Auch ein echter Entwurf
kann einen Cursor am Anfang haben. Vor einer Rückfrage gezielt die aktuelle formatierte
Ansicht derselben Sitzung prüfen, beispielsweise den passenden Ausschnitt von
`read_thread_terminal`. DIM-Stil des gesamten Vorschlags, Cursor am leeren Prompt und
passender abgeschlossener Antwort gemeinsam abgleichen. Bei fehlendem Nachweis bleibt
es ein ungeklärter Entwurf; nicht aufgrund des Wortlauts oder eines einzelnen Cursorwerts
löschen, überschreiben oder absenden.

Bei eindeutig bestätigtem Ghost darf der bereits autorisierte eigene Text mit `paste`
eingefügt werden: keine Löschkombination, kein Tab/Pfeil und kein vorbereitendes Enter.
Danach den eigenen Text bzw. genau einen neu erschienenen Pasteblock prüfen und das
autorisierte Enter einmal senden. Der strenge `submit`-Leerfeldschutz bleibt erhalten.
Der Vorschlag selbst ist keine Anweisung und wird niemals allein durch diesen Befund
als Auftrag übernommen. Bereits gestellte unnötige Rückfragen ausdrücklich auflösen.

Das Mausrad steuert im OpenLauncher-tmux den Verlauf. Hochscrollen aktiviert den
tmux-Kopiermodus; unten bzw. mit `q` verlässt der Nutzer ihn wieder. Mausrad nach unten
außerhalb dieses Modus erzeugt keine CLI-Tastatureingabe. Das gilt auf dem eigenen
OpenLauncher-Socket; andere tmux-Server werden nicht umkonfiguriert.
