# App-Icon-Building — Windows · Android · macOS Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Schwarze/eckige Ecken | Quelle als PNG mit echtem Alpha (RGBA), Ecken Alpha=0 | §2.1 |
| 2 | OS maskiert (Android/iOS/macOS-Tahoe) | Volles Quadrat liefern, nie selbst runden | §2.2 |
| 3 | Windows-`.ico`/klassisches `.icns` | Form/Padding als Alpha einbacken (einmal) | §2.2 |
| 4 | Icon wechselt nicht (Windows) | Cache-DBs löschen + Explorer neu, nicht nur `ie4uinit` | §4 |
| 5 | Verknüpfung zeigt altes Icon | Neuen IconLocation-Schlüssel: `<App>.exe,0` | §4.3 |
| 6 | Konverter macht Ecken schwarz/weiss | Kein `-flatten`/`-background`/`-border`; Pillow `convert("RGBA")` + `sizes` | §3, §8 |
| 7 | WPF Icon fehlt/single-file bricht | `ApplicationIcon` + `Window.Icon`, Datei als `Resource` | §5.1, §5.2 |
| 8 | Android Motiv abgeschnitten | Kernmotiv in 66dp-Safe-Zone, Background opak ohne Rundung | §6.1, §6.2 |
