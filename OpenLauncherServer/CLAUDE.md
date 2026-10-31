# OpenLauncher Server

Version steht in `VERSION` (Form `0.1.9`), pro Commit an diesem Ordner einmal erhöhen; angezeigt im Reiter System. Aufbau und Fallen: `README.md`. Auf dem Server läuft der Code ab dem nächsten Vorspulen (höchstens 5 Minuten), Brücke und Konsole starten selbst neu, tmux nie (ein Neustart beendet alle Sitzungen; `tmux.conf` liest `installieren.sh` nur neu ein). Neue Dienste oder Pakete: `installieren.sh` als root erneut ausführen.
