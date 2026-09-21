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
