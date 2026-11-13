# Victron Instant Readout am Rechner (Windows und Mac)

> Stand: 13.11.2026 22:00. Fallen: `bugs/peripherie/victron-bluetooth.md`.

- Ein Skript für beide Rechner: `Jarvis/werkzeuge/pc-bruecke/jarvis_victron.py` (bleak + cryptography). Windows startet es aus
  `jarvis-pc-bruecke.ps1`, der Mac aus einem Faden in `jarvis_pc_bruecke.py`, jede Minute, als eigener Prozess mit Zeitlimit 50 s.
- Schlüssel nur in `~/SK/Victron/victron.json` (Name, MAC, Schlüssel je Gerät), nie im Repo. Fehlt die Datei, meldet die Brücke das
  einmal im Protokoll und scannt nicht.
- Meldung an den Relay: `POST /pc/victron` mit Token per Kopfdatei (nie in der Befehlszeile), `quelle` „PC“ bzw. „Mac“. Handy und
  Tablet zeigen den Stand unterwegs als „über den Rechner zu Hause“.
- Einrichten am Mac: `pip install --break-system-packages bleak cryptography` (Homebrew-Python ist „externally managed“).
