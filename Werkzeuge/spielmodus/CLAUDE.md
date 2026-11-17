# Spielmodus – Projektregeln

- Version steht in `README.md`, Zeile 3: `Version X.Y.Z · Stand TT.MM.JJJJ, HH:MM Uhr`. Pro Commit genau einmal anheben (Patch +1, neue Funktion Minor +1), Zeit per `Get-Date -Format "dd.MM.yyyy HH:mm"`.
- Nach Änderungen am Wächter: `einrichten.ps1` erneut ausführen (stoppt den alten Wächter, registriert neu). Ohne `-TreiberZip` wird der Treiber nicht angefasst.
- Nur Windows; kein macOS-Gegenstück (Microsoft Store/Gaming Services gibt es dort nicht).
