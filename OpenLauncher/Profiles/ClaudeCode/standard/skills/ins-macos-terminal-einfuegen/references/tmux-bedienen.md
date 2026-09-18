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

## Lesen und sofort absenden

`read` prüft Identität und liefert standardmäßig den aktuellen Bildschirm, bei gleichem Inhalt nur `event: unchanged` und einen frischen `observed`-Token. `--verbose` zeigt alle Metadaten; im Normalfall erscheinen nur geänderte Zustandsfelder. Cursor- und Modusdaten bleiben vollständig Teil der internen Prüfung. `--lines 80` liest bei Bedarf History, `--force-view` wiederholt gezielt eine Ansicht. Der Token ist keine automatische Erkennung von Bereitschaft oder Autorisierung.

Alle inhaltlichen Entscheidungen und die Textdatei **vor** Paste vorbereiten. Bei ausdrücklichem Absendeauftrag:

```sh
python3 "$BRIDGE" read --state "$BRIDGE_RUN/target.json"
# Auftrag sicher als UTF-8-Datei speichern; Hash des autorisierten Inhalts bestimmen.
shasum -a 256 "$BRIDGE_RUN/C17.txt"
python3 "$BRIDGE" submit --state "$BRIDGE_RUN/target.json" \
  --id C17 --text-file "$BRIDGE_RUN/C17.txt" --sha256 'HASH_DES_AUTORISIERTEN_TEXTES' \
  --observed 'TOKEN_AUS_INHALTLICH_GEPRÜFTEM_READ'
```

`submit` erledigt Paste, kurze lokale Nachkontrolle und Enter in **einem** Werkzeugaufruf. Voraussetzungen: exakter Hash, keine andere unklare/offene Zustellung im Ledger, erkannter Claude-Eingaberahmen, vollständig leeres Eingabefeld, Cursor an dessen Anfang, aktive Bracketed-Paste-Unterstützung und frisch geprüfte Identität/Ansicht. Ein leerer Prompt allein beweist trotzdem keine semantische Bereitschaft: laufende Generierung, Freigaben und offene Aufgaben vorab ausschließen.

Nach Paste muss der exakte ungebrochene Text oder genau ein neuer `[Pasted text #N]`-Block ohne fremden Zusatz erscheinen. Der Rahmen und die übrige Ansicht müssen passen. Der Helfer kennt nur den sichtbaren Rahmen dieses Claude-TUI-Formats; andere Layouts, umgebrochene ungekürzte Texte und unbekannte Blockdarstellungen können den konservativen Rückfall auf `pasted` auslösen. Das ist kein Auftrag zum automatischen Wiederholen. Die Blocknummer ist keine Zeilenzahl und kein Inhaltsbeweis; Transporthash und Übergang aus dem leeren Feld ergeben eine begrenzte technische Bestätigung. Parallel eingefügter fremder Inhalt kann trotz dieser Prüfungen nicht atomar ausgeschlossen werden.

Die kurze Renderwartephase beträgt bis zu einer Sekunde zuzüglich Werkzeuglaufzeiten; meist erfolgt die Übergabe unmittelbar. Direkt vor Enter nochmals Identität und Ansicht prüfen. Bei Abweichung kein Enter, bereits eingefügten Text erhalten und gezielt lesen. `enter_sent` belegt nur Transport. Neue Antwort/Arbeitsbeginn getrennt beobachten.

## Nur einfügen oder manuell geklärter Rückfall

```sh
python3 "$BRIDGE" paste --state "$BRIDGE_RUN/target.json" \
  --id C17 --text-file "$BRIDGE_RUN/C17.txt" --observed 'GEPRÜFTER_TOKEN'
python3 "$BRIDGE" read --state "$BRIDGE_RUN/target.json" --force-view
# Nur nach vorhandener Absendeautorisierung und eindeutiger Prüfung des eigenen Entwurfs:
python3 "$BRIDGE" enter --state "$BRIDGE_RUN/target.json" \
  --id C17 --observed 'NEUER_GEPRÜFTER_TOKEN'
```

Bei „nur einfügen“ kein Enter. Keine fremden Entwürfe löschen, keine Vorschläge mit Tab übernehmen. Bei unklarer Ghost-Suggestion gegebenenfalls wenige Promptzeilen mit `capture-pane -p -e` ansehen; Farbe oder Cursorposition allein beweisen keine leere Eingabe. `submit` verweigert nichtleere Textfelder einschließlich Ghost-Suggestions konservativ. Nicht durch neue State-Dateien oder Kennungen umgehen.

Der Helfer übergibt Argumente per `subprocess.run([...])` ohne Shell-Auswertung. Er lädt den Text über stdin in einen zufällig benannten tmux-Puffer, nutzt `paste-buffer -p -r` und löscht nur diesen Puffer. UTF-8, LF und Tab bleiben erhalten; andere Steuerzeichen sind unzulässig. Keine Zeilen einzeln mit Enter senden. `send-keys -l` ersetzt ebenfalls keine sichere Shell-Quotierung. Sprachtext nie in einen interpolierten Shellbefehl oder `eval` einsetzen.

## Schreibschutz und Statusrauschen

Der Beobachtungstoken enthält Prozess-/Pane-Metadaten, Cursor und aktuelle Ansicht. Ausschließlich die numerische Laufzeit in einer vollständig erkannten Launcher-Fußzeile **unter dem Eingaberahmen** wird normalisiert. Pfad, Modell, Preise, Limits, unbekannte Footer und Antworttext bleiben relevant. Breite Zeilenfilter und Spinner-Heuristiken werden nicht verwendet. Die ausgegebene Ansicht bleibt roh, der Vergleich ignoriert nur dieses exakt begrenzte Laufzeitfeld. Bei gekürzter/unbekannter Statuszeile wird nichts normalisiert.

Dadurch ist der Schreibschutz um genau diese Laufzeitzellen schwächer; alle übrigen Änderungen verlangen erneutes Lesen. Ein abgelaufener Token vor einer Mutation ist kein Zustellungsversuch. Versuchstatus erst nach letzter erfolgreicher Vorprüfung unmittelbar vor `paste-buffer` beziehungsweise `send-keys` speichern. Bei Timeout nach diesem Punkt zuerst Zustellung klären.

| Status | Bedeutung |
|---|---|
| Keine neue Kennung im Ledger | Vorprüfung abgebrochen, noch kein Pane-Schreibversuch. Frisch lesen und denselben Auftrag erneut prüfen. |
| `paste_attempted` | Pane-Schreiben versucht; Teilzustellung möglich, kein automatischer Retry. |
| `pasted` | tmux hat Paste angenommen, noch kein Enter. Entwurf bewahren und klären. |
| `enter_attempted` | Enter versucht; bei Fehler nicht automatisch nochmals Enter. |
| `enter_sent` | Enter angenommen; Claude-Annahme anhand neuer Ausgabe prüfen. |

Dies ist kein Exactly-once-Protokoll. Abstürze, Nutzerbedienung und ein Prozesswechsel zwischen Prüfung und Schreiben bleiben Grenzen. Nur ein zustellender Agent pro Dialog. Nach Ende private Laufzeitdateien entfernen, nicht die Claude-Sitzung oder den tmux-Server beenden.
