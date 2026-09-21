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
python3 "$BRIDGE" bind --run "$BRIDGE_RUN" \
  --socket '/BESTÄTIGTER/SOCKET' --pane '%BESTÄTIGTE-ID' \
  --agent-pid BESTÄTIGTE_PID --cwd '/BESTÄTIGTER/ARBEITSORDNER'
```

Die Platzhalter sind absichtlich nicht ausführbar. In nachfolgenden Tool-Shells die konkret ermittelten absoluten Pfade verwenden: Shellvariablen überleben Werkzeugaufrufe nicht zwangsläufig. Keine Symlinks oder gemeinsam beschreibbaren Ordner als State-Ablage verwenden. Der Helfer speichert Identität, Prozessstartzeiten, Pufferhash und Zustellungsstatus; keine Rohpuffer. Ein privates Verzeichnis je beauftragtem Dialog, nur ein zustellender Agent. Gleichzeitige Bedienung durch den Nutzer kann nicht atomar ausgeschlossen werden.

`bind` überschreibt eine vorhandene Bindung nicht. Neue State-Datei nur nach bewusster Neuzuordnung; keine zweite Bindung zum Umgehen einer Zustellungssperre. Prüft der Helfer nach `cd`, Prozessneustart oder Sessionwechsel eine andere Identität, hält er an. Nach geklärtem Wechsel neu binden. Verschobene Panes/mehrere angebundene Clients nicht automatisch als dieselbe sichtbare Zieloberfläche behandeln.

## Ein Dialogverzeichnis, kurze Aufrufe

`--run` verlangt ein bereits angelegtes privates Verzeichnis ohne Symlink. Es leitet `target.json` und bei `paste`/`submit` die Datei `ID.txt` ab. Kennungen werden vor dem Pfadbau geprüft; Slash/Punkt-Traversal ist unzulässig. Abgeleitete Textdateien müssen reguläre Dateien ohne Symlink sein. Unter `--run` ist der autorisierte SHA-256 auch bei reinem `paste` Pflicht, damit ein Tippfehler in der ID keinen alten Text unbemerkt einfügt.

Die bisherigen expliziten Optionen `--state`, `--text-file` und `--cancel-file` bleiben verfügbar. Stimmen explizite State-/Textpfade nicht mit `--run` überein, wird abgebrochen. Nicht automatisch einen anderen Ordner oder eine andere Datei wählen. Ein Dialog, ein Ordner, ein zustellender Agent; ein zweiter Ordner wäre eine mögliche Umgehung von Stopp und Zustellungsledger und ist kein Wiederherstellungsweg.

Eine Datei `STOP` neben der State-Datei sperrt Aufrufe auch dann, wenn sie die alte `--state`-Schreibweise nutzen. Der Helfer prüft sie vor Werkzeugaktionen und während Warte-/Renderphasen. Bei Stopp nur eigenes Hilfsmaterial aufräumen; keine neue Zustellung. Ein bereits laufender Betriebssystemaufruf und das letzte kleine Rennen vor einer Mutation bleiben Grenzen. Aufräumen des eigenen tmux-Puffers ist weiterhin erlaubt.

`STOP` bleibt liegen, bis der Nutzer ausdrücklich fortsetzen lässt. Danach darf der ausführende Agent genau dieses Signal entfernen, ohne Ledger, Zielbindung oder Auftragshashes zu löschen. Erst frisch lesen und offene/unklare Zustellung klären. Kein automatisches Resume durch Timeout, kein Neuanlegen des Dialogordners zum Umgehen des Stopps. Ein aufrufspezifisches `--cancel-file` kann zusätzlich verwendet werden.

## Lesen und sofort absenden

`read` prüft Identität und liefert standardmäßig den aktuellen Bildschirm, bei gleichem Inhalt nur `event: unchanged` und einen frischen `observed`-Token. `--verbose` zeigt alle Metadaten; im Normalfall erscheinen nur geänderte Zustandsfelder. Cursor- und Modusdaten bleiben vollständig Teil der internen Prüfung. `--lines 80` liest bei Bedarf History, `--force-view` wiederholt gezielt eine Ansicht. Der Token ist keine automatische Erkennung von Bereitschaft oder Autorisierung.

Nach einer Kontextkompaktierung bündelt `resume --run "$BRIDGE_RUN"` in genau einem
Leseaufruf: die fünf erlaubten Felder aus `arbeitsstand.json` (maximal 1 KiB), nur
offene Ledger-Einträge, letzte ID, `git status -sb` (maximal 20 Zeilen) und
`git log -1 --oneline` sowie eine frische vollständige Pane-Ansicht mit
Beobachtungstoken. Unter WSL nutzt es `git.exe` auf dem Windows-Pfad, sonst natives
`git`; beide laufen mit `--no-optional-locks`, damit die Leseabfrage nicht mit
Commit oder Rebase um den Index konkurriert. Fehler optionaler Teile erscheinen als `arbeitsstand_error` oder
`git_error`; Zielbindung und STOP brechen weiterhin hart ab. `resume` schreibt
nur dieselbe kompakte Lesebasis wie `read` und autorisiert keine Übergabe.

Bei `in_mode=1` ist ein tmux-Modus wie Kopieren/Scrollen aktiv: weiter lesen ist möglich, aber nicht schreiben oder den Modus selbst verlassen. Warte auf den normalen Eingabemodus und lies dann frisch. Ghost-Suggestions können in der ANSI-Ansicht als ganze Zeile oder wortweise mit Dim-Sequenzen markiert sein; eine einzige erwartete Sequenz ist deshalb kein verlässlicher Parser. Beurteile den gesamten Eingabebereich samt Cursor und Dialogzustand, statt eine fehlgeschlagene Musterprüfung mit einem Tastendruck zu umgehen.

Alle inhaltlichen Entscheidungen und die Textdatei **vor** Paste vorbereiten. Bei ausdrücklichem Absendeauftrag:

```sh
python3 "$BRIDGE" read --run "$BRIDGE_RUN"
# Auftrag sicher als UTF-8-Datei speichern; Hash des autorisierten Inhalts bestimmen.
shasum -a 256 "$BRIDGE_RUN/C17.txt"
python3 "$BRIDGE" submit --run "$BRIDGE_RUN" \
  --id C17 --sha256 'HASH_DES_AUTORISIERTEN_TEXTES' \
  --observed 'TOKEN_AUS_INHALTLICH_GEPRÜFTEM_READ'
```

`submit` erledigt Paste, kurze lokale Nachkontrolle und Enter in **einem** Werkzeugaufruf. Voraussetzungen: exakter Hash, keine andere unklare/offene Zustellung im Ledger, erkannter Claude-Eingaberahmen, vollständig leeres Eingabefeld, Cursor an dessen Anfang, aktive Bracketed-Paste-Unterstützung und frisch geprüfte Identität/Ansicht. Ein leerer Prompt allein beweist trotzdem keine semantische Bereitschaft: laufende Generierung, Freigaben und offene Aufgaben vorab ausschließen.

Nach Paste muss der exakte ungebrochene Text oder genau ein neuer `[Pasted text #N]`-Block ohne fremden Zusatz erscheinen. Der Rahmen und die übrige Ansicht müssen passen. Der Helfer kennt nur den sichtbaren Rahmen dieses Claude-TUI-Formats; andere Layouts, umgebrochene ungekürzte Texte und unbekannte Blockdarstellungen können den konservativen Rückfall auf `pasted` auslösen. Das ist kein Auftrag zum automatischen Wiederholen. Die Blocknummer ist keine Zeilenzahl und kein Inhaltsbeweis; Transporthash und Übergang aus dem leeren Feld ergeben eine begrenzte technische Bestätigung. Parallel eingefügter fremder Inhalt kann trotz dieser Prüfungen nicht atomar ausgeschlossen werden.

Die kurze Renderwartephase beträgt bis zu einer Sekunde zuzüglich Werkzeuglaufzeiten; meist erfolgt die Übergabe unmittelbar. Direkt vor Enter nochmals Identität und Ansicht prüfen. Bei Abweichung kein Enter, bereits eingefügten Text erhalten und gezielt lesen. `enter_sent` belegt nur Transport. Neue Antwort/Arbeitsbeginn getrennt beobachten.

## Nur einfügen oder manuell geklärter Rückfall

```sh
python3 "$BRIDGE" paste --run "$BRIDGE_RUN" \
  --id C17 --sha256 'HASH_DES_AUTORISIERTEN_TEXTES' --observed 'GEPRÜFTER_TOKEN'
python3 "$BRIDGE" read --run "$BRIDGE_RUN" --force-view
# Nur nach vorhandener Absendeautorisierung und eindeutiger Prüfung des eigenen Entwurfs:
python3 "$BRIDGE" enter --run "$BRIDGE_RUN" \
  --id C17 --observed 'NEUER_GEPRÜFTER_TOKEN'
```

Bei „nur einfügen“ kein Enter. Keine fremden Entwürfe löschen, keine Vorschläge mit Tab übernehmen. Bei unklarer Ghost-Suggestion gegebenenfalls wenige Promptzeilen mit `capture-pane -p -e` ansehen; Farbe oder Cursorposition allein beweisen keine leere Eingabe. `submit` verweigert nichtleere Textfelder einschließlich Ghost-Suggestions konservativ. Nicht durch neue State-Dateien oder Kennungen umgehen.

Der Helfer übergibt Argumente per `subprocess.run([...])` ohne Shell-Auswertung. Er lädt den Text über stdin in einen zufällig benannten tmux-Puffer, nutzt `paste-buffer -p -r` und löscht nur diesen Puffer. UTF-8, LF und Tab bleiben erhalten; andere Steuerzeichen sind unzulässig. Keine Zeilen einzeln mit Enter senden. `send-keys -l` ersetzt ebenfalls keine sichere Shell-Quotierung. Sprachtext nie in einen interpolierten Shellbefehl oder `eval` einsetzen.

Direkte `--literal-line` ist auf 512 UTF-8-Bytes begrenzt. Für längere oder
mehrzeilige Inhalte `submit-file` mit `--payload-file`, dem in der Zielumgebung
lesbaren `--display-path`, Payload-`--sha256`, frischem `--observed` und bei
Windows `--literal-line` verwenden. Die Payload bleibt unverändert in der Datei;
im Terminal erscheint nur ein kurzer, geprüfter Verweis mit Bytezahl und SHA-256.
Die technische Obergrenze beträgt 8 MiB und ist keine Behauptung, dass ein Modell
diesen Umfang in einem Leseschritt verarbeitet; große Dateien abschnittsweise lesen.
CRLF und sonstiger erlaubter UTF-8-Text bleiben in der Payload bytegetreu erhalten.
Ist der automatisch erzeugte Verweis länger als 512 Bytes, einen kürzeren
`display-path` verwenden.

## Schreibschutz und Statusrauschen

Der Beobachtungstoken enthält Prozess-/Pane-Metadaten, Cursor und aktuelle Ansicht. Ausschließlich die numerische Laufzeit in einer vollständig erkannten Launcher-Fußzeile **unter dem Eingaberahmen** wird normalisiert. Pfad, Modell, Preise, Limits, unbekannte Footer und Antworttext bleiben relevant. Breite Zeilenfilter und Spinner-Heuristiken werden nicht verwendet. Die ausgegebene Ansicht bleibt roh, der Vergleich ignoriert nur dieses exakt begrenzte Laufzeitfeld. Bei gekürzter/unbekannter Statuszeile wird nichts normalisiert.

Dadurch ist der Schreibschutz um genau diese Laufzeitzellen schwächer; alle übrigen Änderungen verlangen erneutes Lesen. Ein abgelaufener Token vor einer Mutation ist kein Zustellungsversuch. Versuchstatus erst nach letzter erfolgreicher Vorprüfung unmittelbar vor `paste-buffer` beziehungsweise `send-keys` speichern. Bei Timeout nach diesem Punkt zuerst Zustellung klären.

| Status | Bedeutung |
|---|---|
| Keine neue Kennung im Ledger | Vorprüfung abgebrochen, noch kein Pane-Schreibversuch. Frisch lesen und denselben Auftrag erneut prüfen. |
| `paste_attempted` | Pane-Schreiben versucht; Teilzustellung möglich, kein automatischer Retry. |
| `pasted` | tmux hat Paste angenommen, noch kein Enter. Entwurf bewahren und klären. |
| `cleared` | Nie abgesendeter eigener Entwurf wurde nachweislich manuell aus dem Feld entfernt; Kennung und Hash bleiben gesperrt, der fachliche Auftrag kann unter neuer ID weiterlaufen. |
| `enter_attempted` | Enter versucht; bei Fehler nicht automatisch nochmals Enter. |
| `enter_sent` | Enter angenommen; Claude-Annahme anhand neuer Ausgabe prüfen. |

Dies ist kein Exactly-once-Protokoll. Abstürze, Nutzerbedienung und ein Prozesswechsel zwischen Prüfung und Schreiben bleiben Grenzen. Nur ein zustellender Agent pro Dialog. Nach Ende private Laufzeitdateien entfernen, nicht die Claude-Sitzung oder den tmux-Server beenden.

Nach nachgewiesener manueller Entfernung eines eigenen offenen Entwurfs darf `resolve`
mit dessen ID, ursprünglichem Auftrags- beziehungsweise Payload-SHA-256 und einem
frischen Beobachtungstoken verwendet
werden. Es verlangt einen leeren Claude-Eingaberahmen, setzt ausschließlich `pasted`
oder `paste_attempted` auf `cleared` und sendet keine Taste.
`--manual-clear-confirmed` hält die beobachtete manuelle Bereinigung fest; ein
markanter Textanfang des Auftrags im jüngeren Pane-Verlauf verhindert die Klärung.
Ist dort nur ein nicht eindeutig zuordenbarer `[Pasted text #N]`-Block sichtbar,
bleibt die erforderliche Sichtprüfung bestehen; die Nummer allein beweist keinen Auftrag.
`--continued-as <ID>`
dokumentiert die Fortführung, ohne den neuen Auftrag selbst zu senden. Eine gesendete
oder auch nur versuchsweise mit Enter bestätigte Zustellung bleibt unverändert. Der
Status beschreibt nur den Transport, nicht Stopp, Verwerfen oder Erledigung des Inhalts.

## Kurze Fehlerantworten

Fehler liefern einen stabilen Code und einen konkreten deutschen Satz: `E_STALE` (Ansicht veraltet), `E_TARGET` (Identität), `E_INPUT` (Eingabe unklar), `E_DUPLICATE` (bereits versucht/offen), `E_TEXT` (Text/Hash), `E_STOP` (Stoppsignal), `E_ARGS` (Argumente), `E_TOOL`/`E_TIMEOUT`/`E_STATE` (lokaler Werkzeug-/Zustandsfehler). Diese Codes lösen keinen automatischen Retry aus. Status und Ledger bestimmen, ob eine Mutation versucht wurde. Ein Rückfall mit `transport: pasted, submitted: false` ist ein bewusst ungesendeter Entwurf, kein fertiger Auftrag.
