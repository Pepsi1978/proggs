# tmux bedienen

Der Helfer nutzt nur Python-Standardbibliothek, `/bin/ps` und das lokal installierte `tmux`. Er läuft genau einen Aufruf lang. Er startet keine Sitzungen, hängt keine Clients ab, verändert keine tmux-Einstellungen und aktiviert keine Freigaben. Die lokale tmux-Manpage beschreibt insbesondere `capture-pane`, `load-buffer`, `paste-buffer -p -r` und `send-keys`.

## Ziel bestimmen und binden

Pfad aus dem tatsächlich geladenen Skill bestimmen. Beispiel für die kanonische Installation:

```sh
BRIDGE='/Users/frank/proggs/OpenLauncher/Profiles/ClaudeCode/standard/skills/ins-macos-terminal-einfuegen/scripts/tmux_bridge.py'
python3 "$BRIDGE" list
```

`list` gibt pro Pane Socket, Server-PID, Session-ID, Pane-ID, Pane-PID, Arbeitsordner, Vordergrundkommando und den zugehörigen Prozessbaum aus (Prozessnamen, keine vollständigen Argumentlisten mit möglichen Secrets). Der Standardaufruf erfasst **nur den Standardserver**. Bei benutzerdefiniertem Server `--socket '/bekannter/absoluter/socket'` ergänzen; fehlende Standard-Panes sind kein Beweis für „kein tmux“. Bekannten Socket aus aktuellem Startkontext verwenden, sonst gezielt klären.

Bestimme die **eigentliche Claude-PID**, nicht einen MCP-Kindprozess, Shell-Wrapper oder eine andere Claude-Instanz. Prozessbaum, Startkontext, Arbeitsverzeichnis und Inhalt der sichtbaren Sitzung müssen zusammenpassen. Bei Unklarheit keine Schreibprobe. Eine eindeutig bestätigte Zuordnung aus dem Gespräch genügt, eine neue Testnachricht ist unnötig.

```sh
BRIDGE_RUN=$(mktemp -d "${TMPDIR:-/tmp}/codex-tmux-bridge.XXXXXX")
chmod 700 "$BRIDGE_RUN"
# Werte aus der bestätigten list-Ausgabe einsetzen; keine historischen IDs kopieren.
python3 "$BRIDGE" bind --state "$BRIDGE_RUN/target.json" \
  --socket '/BESTÄTIGTER/SOCKET' --pane '%BESTÄTIGTE-ID' \
  --agent-pid BESTÄTIGTE_PID --cwd '/BESTÄTIGTER/ARBEITSORDNER'
```

Die Platzhalter sind absichtlich nicht ausführbar. In nachfolgenden Tool-Shells die konkret ermittelten absoluten Pfade verwenden: Shellvariablen überleben Werkzeugaufrufe nicht zwangsläufig. Keine Symlinks oder gemeinsam beschreibbaren Ordner als State-Ablage verwenden. Der Helfer speichert Identität, Prozessstartzeiten, Pufferhash und Zustellungsstatus; keine Rohpuffer. Ein privates Verzeichnis je beauftragtem Dialog, nur ein zustellender Agent. Gleichzeitige Bedienung durch den Nutzer kann nicht atomar ausgeschlossen werden.

`bind` überschreibt eine vorhandene Bindung nicht. Neue State-Datei nur nach bewusster Neuzuordnung; keine zweite Bindung zum Umgehen einer Zustellungssperre. Prüft der Helfer nach `cd`, Prozessneustart oder Sessionwechsel eine andere Identität, hält er an. Nach geklärtem Wechsel neu binden. Verschobene Panes/mehrere angebundene Clients nicht automatisch als dieselbe sichtbare Zieloberfläche behandeln.

## Lesen, einfügen, Enter

```sh
python3 "$BRIDGE" read --state "$BRIDGE_RUN/target.json"
```

Die Antwort enthält `observed` und die begrenzte aktuelle Textsicht. Prüfe zuerst inhaltlich, ob Claude eine **neue Nachricht** erwartet und kein echter Nutzereingabetext vorliegt. Ein Ghost-Vorschlag kann im reinen Textauszug wie Eingabe aussehen: Farbe allein ist kein sicherer Beleg. Bei Bedarf wenige Promptzeilen mit `tmux -S SOCKET capture-pane -p -e -t PANE` auf Stilinformationen prüfen und mit vorheriger Beobachtung vergleichen. Bleibt es unklar, nichts senden. Der Helfer erkennt diese Bedeutung nicht.

Den fertigen Text über eine sichere Datei-Schreibschnittstelle speichern. Bei Shell-Heredocs einen quotierten, im Inhalt garantiert nicht vorkommenden Abschlussmarker verwenden; nie Sprachtext in einen interpolierten Shellbefehl einbauen. Steuerzeichen sind verboten, UTF-8, Zeilenumbrüche und Tabulatoren erlaubt. Die gesamte Nachricht einschließlich Kennung vor Einfügen fertigstellen.

```sh
python3 "$BRIDGE" paste --state "$BRIDGE_RUN/target.json" \
  --id C17 --text-file "$BRIDGE_RUN/C17.txt" --observed 'TOKEN_AUS_READ'
python3 "$BRIDGE" read --state "$BRIDGE_RUN/target.json" --force-view
# Erst nach Kontrolle des eingefügten Entwurfs und bestehendem Absendeauftrag:
python3 "$BRIDGE" enter --state "$BRIDGE_RUN/target.json" \
  --id C17 --observed 'NEUER_TOKEN_AUS_READ'
```

`paste` und `enter` gehören in getrennte Werkzeugaufrufe, damit Nutzerkorrekturen dazwischen berücksichtigt werden. Neue Beobachtungstokens nicht blind übernehmen: Sie belegen nur dieselbe Momentaufnahme, keine leere Eingabe oder Freigabe. Bei fremdem Entwurf, Ghost-Unklarheit, Shell, Generierung oder Freigabedialog anhalten. Nicht Enter senden, um herauszufinden, was passiert.

Der Helfer benutzt `subprocess.run([...])` ohne `shell=True`. Er lädt den UTF-8-Text über stdin in einen zufällig benannten tmux-Puffer, fügt mit `paste-buffer -p -r` ein und entfernt nur diesen Puffer. `-p` setzt Paste-Klammern nur bei vom Ziel aktivierter Unterstützung (`bracket_paste_flag=1`); andernfalls verweigert der Helfer die Eingabe. `-r` erhält LF-Zeilenumbrüche. Keine künstlichen ESC-Sequenzen injizieren und keine Zeilen einzeln mit Enter zustellen. Ein mehrzeilig eingeklappter Claude-Pasteblock ist nicht automatisch vollständig überprüfbar; bei fehlender Vorschau Enter auslassen.

Für gezielte **einzeilige** manuelle Eingabe wäre `send-keys -l` nur mit getrennten Prozessargumenten und ohne Steuerzeichen geeignet. Es macht interpolierte Shellbefehle nicht sicher. Standardweg dieses Skills bleibt der Helfer; keine unquotierten Sprachtexte, `eval`, Command-Substitution oder Tastenkürzel zum Leeren der Eingabe.

## Transportstatus und Wiederholung

| Status | Bedeutung / nächster Schritt |
|---|---|
| `paste_attempted` | Vor dem Schreiben gespeichert. Fehler/Timeout kann trotzdem Teilzustellung bedeuten; lesen und klären. |
| `pasted` | tmux hat Paste angenommen. Entwurf lesen, kein Beleg für Claude-Annahme. |
| `enter_attempted` | Absenden versucht; bei Fehler nicht automatisch nochmals Enter. |
| `enter_sent` | tmux hat Enter angenommen. Erst neue Claude-Ausgabe belegt fachliche Annahme. |

Kennungen im gesamten Dialog eindeutig halten. Ein erneuter Aufruf mit derselben Kennung wird gesperrt. Dies ist Schutz vor versehentlichen Wiederholungen, kein Exactly-once-Protokoll: Abstürze, zwei getrennte Bindungen, Nutzerbedienung und Prozesse, die zwischen Prüfung und Schreiben wechseln, bleiben Grenzen. Bei einem eingefügten Auftrag kann späterer fremder Text nicht anhand des Tokens allein erkannt werden; vor Enter den tatsächlichen Entwurf erneut beurteilen.

Nach Abschluss die konkrete private Laufzeitablage inklusive Auftragsdateien löschen. Nicht den tmux-Server oder die Claude-Sitzung beenden. Keine State-Dateien einchecken.
