# Elgato Stream Deck Plugin-Entwicklung Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Plugin erscheint gar nicht, keine Fehlermeldung | Ordner `<uuid>.sdPlugin` == manifest-UUID, nur `[a-z0-9.-]` | §A1–A3 |
| 2 | Bild fehlt oder Plugin laedt nicht | Image-Pfade OHNE Endung, `CodePath`/PI-Pfad MIT Endung | §A5 |
| 3 | manifest.json laedt nicht trotz gueltigem Inhalt | Als UTF-8 OHNE BOM speichern, JSON validieren | §A10, K2 |
| 4 | `context` extern gespeichert / nach Neustart tot | Nie extern persistieren, nach Reconnect Zustand neu aufbauen | §D3, E4 |
| 5 | Timer/Polls laufen auf totem context weiter | Bei `willDisappear` Timer stoppen + aus Map entfernen | §D4 |
| 6 | `willAppear` feuert mehrfach / je Geraet | Pro context idempotent registrieren, nie eine Instanz annehmen | §D1 |
| 7 | Tastendruck loest doppelt aus | Handler nur EINMAL binden + keyDown debouncen (~75 ms) | §C5, D5 |
| 8 | Felder verschwinden nach `setSettings` | Ganzes Objekt schreiben `{...current, feld}`, aus Event-Args lesen | §G1, G2 |
| 9 | API-Key pro Instanz weg / im Export sichtbar | Secrets NUR via `setGlobalSettings`, nie Action-Settings/Bundle | §G5, N2 |
| 10 | `setTitle` wirkt nicht / Custom-Title weg | Taste als SVG via `setImage()` + `setTitle("")` nur bei echtem Text | §E1, Z4 |
| 11 | Editor friert ein bei haeufigem Update | `setState`/`setImage` dedupen, < 10 Calls/s halten | §E5, M1 |
| 12 | Polling-Plugin wird traege im Hintergrund | Chromium drosselt Timer → SSE/`EventSource` statt `setInterval` | §M2 |
| 13 | Node-Plugin crasht in Restart-Loop / `--no-addons` | Globale `uncaughtException`/`unhandledRejection`, keine `.node`-Module | §I1, I2 |
| 14 | Aenderung erscheint nicht | `streamdeck restart <uuid>`, Software beenden (Lock), nicht als Admin | §E2, K6, N1 |
| 15 | Packen scheitert / Plugin laedt nicht | `streamdeck validate` → `pack`, nie `Compress-Archive` | §J1, K3 |
