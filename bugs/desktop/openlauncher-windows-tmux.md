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
