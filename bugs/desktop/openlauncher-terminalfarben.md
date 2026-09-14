# OpenLauncher: geerbtes TERM=dumb nach Windows-Update

Am 14.09.2026 meldete der Nutzer ein graues Claude-Logo und veränderte Statusline-Farben nach normalem Start. Die gezielte Prüfung der laufenden Prozesse zeigte NO_COLOR=1, TERM=dumb und leeres COLORTERM im vom Updater gestarteten Launcher. Die neue normale Claude-Sitzung enthielt noch TERM=dumb; eine ältere normale Sitzung nicht.

Ursache: Der korrekt aufgerufene Updater vererbte die Codex-Werkzeugumgebung an die App. Die vorhandene Bereinigung in OpenLauncherService entfernte NO_COLOR, aber nicht TERM=dumb. Dadurch blieb die Angabe einer nicht farbfähigen Konsole wirksam. Der heutige Codex-Button hatte die vorhandene Bereinigung nicht entfernt.

Korrektur: Der Windows-Updater bereitet eine ausschließlich für den neuen Launcher bereinigte Prozessumgebung vor. Zusätzlich entfernen die gemeinsamen CLI-Startskripte TERM=dumb und ein leeres COLORTERM. Echte TERM-/COLORTERM-Fähigkeitsangaben sowie das ausgewählte Profil bleiben erhalten. Keine persistenten Umgebungsvariablen oder Terminalpaletten ändern.

Verwandte Stellen: Die gemeinsame Bereinigung gilt für Claude Code, OpenCode und Codex CLI sowie den eingebetteten Claude-Start. Die macOS-Bereinigung wurde vergleichend angesehen; diese Änderung betrifft die konkret nachgewiesene Windows-Vererbung und wurde nicht auf macOS validiert. Bereits laufende Terminals behalten ihre ursprüngliche Umgebung und müssen für einen sichtbaren Vergleich neu gestartet werden.

Regression: tests/verify-terminal-colors.ps1 prüft die produktiven Skriptblöcke auf Entfernung der beobachteten Agentenwerte, Erhalt echter Farbfähigkeiten und unveränderte Aufruferumgebung. Ein bestandener Skripttest allein belegt noch nicht die sichtbare Farbpalette.

Validierung: Regressionstest bestanden. Version 1.24.43 wurde über update-launcher.ps1 nach Nutzerklick gebaut (0 Warnungen, 0 Fehler) und gestartet. Im neuen laufenden Launcher waren bei anschließender gezielter Prozessprüfung weder TERM=dumb noch NO_COLOR oder andere geprüfte Farb-Overrides vorhanden. Sichtbarer Farbvergleich in einer frisch gestarteten Nutzersitzung bleibt separat zu bestätigen.
