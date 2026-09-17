# OpenLauncher: normaler Claude-Start verwendet defekten fnm-Wrapper

Am 17.09.2026 zeigte `claude.ps1` aus `fnm_multishells` auf eine fehlende
`node_modules/@anthropic-ai/claude-code/bin/claude.exe`. Die separate Installation
unter `%APPDATA%/npm` war vorhanden und lieferte mit `--version` Claude Code 2.1.274.

Ursache in `BuildClaudeCodeStartScript`: Nur der eingebettete Terminalweg suchte
eine vorhandene EXE. Der normale Start rief `claude` über PATH auf und landete im
defekten Wrapper. Die Auflösung gilt jetzt für beide Terminalarten; alle starten
über dieselbe geprüfte EXE. Fehlt jede Installation, erscheint eine konkrete
Reparaturmeldung. Generierte npm-Wrapper und globale PATH-Einstellungen bleiben unverändert.

Verwandte Prüfung: normaler Start mit/ohne Farbe und eingebetteter Start teilen
dasselbe Template. Profil, Modell, Settings und Effort werden davor aufgebaut.
Die Windows-spezifische fnm/npm-EXE-Auflösung betrifft keine macOS-Shellskripte.
OpenCode und Codex verwenden andere Programme und sind von diesem Claude-Wrapper
nicht abhängig. Keine Änderung an diesen Startwegen.

Regression: `OpenLauncher/tests/verify-claude-resolution.ps1` führt den produktiven
Auflösungsblock für defekte und intakte fnm-Wrapper, native Installation, EXE im PATH
und vollständig fehlende Installation aus. Jede Variante läuft normal/eingebettet
und mit/ohne Farbe; Modell-, Effort- und Settingsargumente müssen erhalten bleiben.
