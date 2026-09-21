# OpenLauncher: Windows-CLIs über WSL/tmux

Lokal geprüft am 21.09.2026 mit Ubuntu, tmux 3.6 und Windows-Claude 2.1.278.

## Windows-Pfade bei WSL-Aufrufen

`wsl -- wslpath -u C:\...` kann die Backslashes durch die Linux-Shell verlieren.
Ursache: WSLs normale Befehlsübergabe führt eine zusätzliche Shell-Auswertung aus.
`wsl --exec wslpath -u <einzelnes Argument>` und `ProcessStartInfo.ArgumentList`
verhindern diese Interpretation. Die tmux-Startargumente ebenfalls getrennt übergeben,
nicht als zusammengesetzten Shellbefehl. Der Integrationstest verwendet reale Pfade
mit Leerzeichen, Umlauten, Apostroph und Dollarzeichen.

## Linux-tmux sieht Windows-Prozesse als Interop-Host

Claude, Codex und OpenCode bleiben Windows-Programme mit ihren bisherigen Profilen.
Linux-tmux zeigt für die Windows-PowerShell `init`; `/bin/ps` zeigt `pwsh.exe`.
Die Windows-CLI-PID ist keine Linux-PID. Für die Dialogbindung müssen Wrapper,
Windows-Prozessbaum, Pane-Arbeitsordner und Inhalt übereinstimmen. Der gebundene
Interop-Host endet mit dem PowerShell-Startskript; kein interaktiver Shell-Fallback.

## Paste-Status und Eingabezeichen

Ubuntu-tmux 3.6 liefert hier kein `bracket_paste_flag`; der bisherige Mac-Helfer
verweigert daher korrekt sein mehrzeiliges Paste. Kein Flag erfinden oder blind
Bracketed-Paste-Sequenzen senden. `--literal-line` erlaubt explizit nur Text ohne
Steuerzeichen über `send-keys -l`, mit unveränderten Identitäts-, Hash-, Frische-,
Stopp- und Duplikatprüfungen. Echte Mehrzeiler bleiben in einer gemeinsamen Datei.
Der Windows-Claude-Prompt ist `>` statt `❯`; beide benötigen den vollständigen Rahmen.

Der echte zweite Dialog lief über `read` → `submit` (konservativer Rückfall auf
`pasted`, weil Claude seinen Fußzeilenhinweis änderte) → geprüftes `enter` → neue Antwort.
Kein erneutes Einfügen nach dem Rückfall. Der erste Dialog und diese zweite Antwort
wurden ohne Computer Use übertragen und gelesen.

Verwandte Wege geprüft: alle drei Launcher-Startmethoden verwenden denselben optionalen
Wrapper, Profile und Modellauswahl werden davor unverändert aufbereitet. Standardstarts
bleiben ohne WSL möglich. Fehlende Voraussetzungen brechen sichtbar ab, statt tmux
stillschweigend zu überspringen. Die bisherigen Claude-Auflösungs- und Farbtests bestehen.

Reproduzierbare Prüfungen:
- `OpenLauncher/tests/verify-tmux.ps1`: produktiver Wrapper, reale Windows-CLI-Versionen,
  Pfade/Sonderzeichen, fehlendes Startskript, ausschließlich eigene Testsitzungen.
- `ins-macos-terminal-einfuegen/tests/check_literal_line.py`: bytegenauer Transport,
  beide Promptformen, separates Enter, alte Tokens, Duplikate, Steuerzeichen, STOP.

## Mausrad und schnellere Dialogübergaben (21.09.2026)

Ursachenkette Mausrad: `mouse off` überlässt dem Terminal die Interpretation;
im alternativen Bildschirm kann das Rad als Pfeiltaste ankommen; die CLI wählt dann
frühere Eingaben. Nur `mouse on` genügt nicht: die tmux-Standardbindung kann Ereignisse
im alternativen Bildschirm erneut an die Anwendung weiterreichen. Deshalb setzt der
Windows-Launcher auf seinem eigenen Socket `mouse on` und explizite Bindungen:
WheelUpPane öffnet den Kopiermodus, WheelDownPane sendet nur innerhalb dieses Modus.
Auch WheelUpStatus/WheelDownStatus scrollen den aktiven Verlauf; die tmux-Defaults
`previous-window`/`next-window` sind auf dem Launcher-Socket ersetzt. Der Test prüft
dies mit zwei Fenstern, einschließlich Rückkehr zum Eingabemodus am Verlaufsende.
Neue Starts und erneutes Anhängen über neu erzeugte Wrapper erhalten diese Vorgabe.
Andere tmux-Server werden nicht geändert. `tests/check-tmux-mouse.py` prüft in einem
eigenen Testclient SGR-Mausereignisse und bestätigt null Eingabebytes bei der Anwendung.

Ursachenkette Versand: ein langer Originaltext ist einzeilig, die gerenderte Darstellung
enthält Fortsetzungszeilen, der frühere strikte Stringvergleich fällt deshalb zurück.
Zusätzlich verändern Feldhöhe und bekannter Footerhinweis die Umgebung. Der Helfer
erkennt nun ausschließlich die bekannte Zweispalten-Fortsetzung, bewahrt sonstige
Leerzeichen und erlaubt begrenzten Scrollverlust entsprechend dem Feldwachstum.
Andere Zeichen/Umgebungsänderungen bleiben ein Rückfall ohne Enter. Keine pauschale
Whitespace-Normalisierung und keine vollständige Abschaltung der Umgebungsprüfung.

Der optionale Kompaktlesemodus meldet einen positionsgebundenen Ersetzungsbereich.
Er speichert Zeilenhashes, keine Rohtexte; gleiche Zeilen an verschiedenen Positionen
bleiben erhalten. Kürzung verwirft die Folgebasis. Originalansicht per `--force-view`.
`tests/check_speed.py` prüft Umbruch, fremde Zeichen, veränderte Leerzeichen/Umgebung,
identische Zeilen und Vollansicht. Bestehender Literal-Line-Test bleibt grün.
Messung des isolierten Empfängers: automatischer Versand ca. 0,25 s, keine zusätzliche
Modellrunde zwischen Paste und Enter. Dies misst Transport, keinen Modellverbrauch.

Im anschließenden Live-Dialog erreichten R3 und R4 direkt `enter_sent` durch `submit`.
Claude prüfte die Zeitnormalisierung unabhängig an der echten Fußzeile. Ausschließlich
Uhrzeit, Laufzeit und Reset-Restzeiten werden normalisiert, keine Prozentwerte oder
Modell-/Effortdaten. Mehrfachauszüge verwenden geordnete Sequenzvergleiche ohne autojunk;
Tests rekonstruieren Wiederholungen, Löschungen und Leerzeilen. Neue Messung desselben
Empfängers: Änderungsauszug 742 Bytes; die ursprüngliche Vollansicht 1158 Bytes.
Beide Zahlen betreffen verschiedene Ansichten und sind keine Kontingentquote.

Ein blasser Ghost-Vorschlag sah in `capture-pane -p` wie ein Entwurf aus; selbst `-e`
enthielt unter Windows/Interop keine SGR-Daten. Die derselben sichtbaren Sitzung
zugeordnete formatierte Codex-Terminalausgabe zeigte dagegen DIM für den ganzen
Vorschlag, bei Cursor x=2. Cursor allein ist kein Beweis. Nach gemeinsamer Prüfung
funktionierte eigenes `paste` ohne Lösch-/Übernahmetasten, danach geprüftes Enter.
Der strenge automatische Leerfeldschutz bleibt bestehen. Scrollmodusfehler werden vor
dem Frischetoken geprüft, damit Hochscrollen ohne weitere Runde verständlich gemeldet wird.

Der neue optionale Status-Wartemodus wurde in R5 live verwendet: aktive Titel wechseln
zwischen `◐` und `◑`, nach Antwortende zu `✳`. Nur die beiden beobachteten Busy-Frames
werden für das Wecksignal gleichgesetzt; Titel und Frischeprüfung bleiben unverändert.
Ein Live-Aufruf wartete 10042 ms trotz Tool-/Spinneränderungen, anschließend kamen
kompakte Änderungen. Der spätere Wechsel zum Ruhetitel weckte unmittelbar beim Abruf.
Kein Titel gilt als Fertigbeweis. Bei fehlendem Eingaberahmen weckt nur der Übergang,
nicht jeder Folgeaufruf; ein Regressionstest verhindert die sonst entstehende Leseschleife.
Kurz sichtbare Meldungen bleiben eine Grenze der Momentaufnahmen, keine Verlustfreiheit
behaupten. Testvergleich derselben Ansicht: 742 Bytes als Auszug, 991 als Vollansicht.

R8 zeigte die andere Zeitgrenze: Ist ein kompletter Busy/Idle-Zyklus zwischen zwei
Reads vergangen, kann der unveränderte Ruhetitel allein kein Wecken auslösen. Deshalb
bündelt der Statusmodus ausschließlich bei den nachgewiesenen Busy-Titeln; bei allen
anderen Titeln wecken auch Inhalts-/Metadatenänderungen. Ein echter tmux-Test verändert
Antworttext oberhalb des Rahmens bei unverändertem Ruhetitel und prüft sofortige Rückgabe.
R9 bestätigte die Logik und unveränderte STOP-/Identitätsprüfungen im Review.

Der Windows-Einstieg wurde von 29442 auf rund 6037 UTF-8-Bytes verkürzt. Die vollständige
alte Fensteranleitung liegt nun in `references/windows-fenster.md`; ein Vergleich gegen
den vorherigen Commit bestätigt unveränderten Inhalt ab dem bisherigen Schnellweg,
abgesehen von relativen Links. Plattformwahl, Autorisierung, Nutzerzwischenrufe,
Entwurfs-/Stoppschutz, Rollen/Review und Mehrzeilerregel bleiben im Einstieg.
Alle Markdown-Links wurden aufgelöst, Claude prüfte die erhaltenen Regeln unabhängig.
Dies spart Einstiegstext, ist aber keine gemessene Kontingentprozentzahl.

## Bytegenaue lange Übergaben und kompakter Dreierloop (21.09.2026)

Ein direkter langer `send-keys -l`-Auftrag erschien in der echten Windows-Claude-
Eingabe nur als Schlussrest; der Helfer verhinderte das Enter, die genaue Ursache
zwischen tmux, WSL-Interop/ConPTY und Claude-TUI blieb jedoch unbelegt. Direkte
`--literal-line` ist deshalb konservativ auf 512 UTF-8-Bytes begrenzt. Längere oder
mehrzeilige Aufträge laufen über `submit-file`: Die unveränderte, für das Ziel
zugängliche UTF-8-Datei wird bis 8 MiB geprüft, mit Bytezahl und SHA-256 autorisiert,
und nur ein kurzer automatisch erzeugter Verweis gelangt ins Terminal. CRLF bleibt
bytegetreu; große Dateien müssen innerhalb des Modells abschnittsweise gelesen werden.

Der Live-Test R14 übergab 2950 Payload-Bytes mit SHA-256
`b6f31628bdc3dac7e9c9f45b15938de7d7276da9bbce7876a25fd24cb5da1cb3`.
Claude meldete dieselbe Bytezahl und denselben Hash und las den vollständigen Auftrag.
Der isolierte Test verwendet zusätzlich eine 8400-Byte-CRLF-Payload, einen falschen
Hash, einen zu langen Zielpfad und stellt sicher, dass nie Payloadbytes in die
Terminaleingabe geraten. Diese Belege zeigen vollständigen Dateitransport und die
gemeldete Lektüre, keine mathematisch beweisbare semantische Verarbeitung jedes Zeichens.

Ein manuell aus dem Eingabefeld entfernter, nie abgesendeter Versuch kann mit
`resolve` als `cleared` abgeschlossen und unter neuer ID fortgeführt werden.
Kennung und Hash bleiben gesperrt. Voraussetzung sind der ursprüngliche Auftrags-
beziehungsweise Payload-Hash, `--manual-clear-confirmed`, ein frisches leeres Feld
und kein passender Textanfang in 300 Verlaufszeilen; die Aktion sendet keine Taste.
Der Status beschreibt nur den Transport, weder Stopp noch inhaltliches Verwerfen.

Der Dreierloop hält künftig im privaten Dialogordner nur Zielrevision, letzte
Zustellung, offene Nutzeränderungen, Phase und festen Rundenausgang. Vor Commit oder
Updater muss die Zustellung die aktuelle Zielrevision abdecken. Nach Kompaktierung
genügen normalerweise dieser Zustand, Ledger, `git status/log` und eine frische
Pane-Ansicht; vollständige Rohverläufe werden nur bei Widersprüchen nachgeladen.

`resume` setzt diese Wiederaufnahme als einzelne read-only Aktion um. Der Live-Aufruf
gegen dieselbe Windows-Claude-Sitzung lieferte den 119-Byte-Arbeitsstand, keine offenen
Zustellungen, letzte ID R15, den echten Windows-Git-Stand und eine frische Pane-Ansicht
mit Token. Unter WSL wird `git.exe` mit dem per `wslpath -w` übersetzten Repo-Pfad
verwendet; auf nativen Linux-/macOS-Pfaden natives `git`. Arbeitsstand- oder Git-Fehler
werden getrennt gemeldet, während Zielbindung und STOP auch bei einem Signal während
des Git-Abrufs hart bleiben. Git läuft mit `--no-optional-locks`, damit die reine
Statusabfrage nicht mit Commit oder Rebase um den Index konkurriert. Der isolierte
Test prüft zusätzlich einen offenen `pasted`-Eintrag, Duplikatblockade beim Folgesenden,
kaputtes Arbeitsstand-JSON, fehlendes Git-Repository, STOP und weniger JSON-Bytes als
die getrennten Rohabrufe.
