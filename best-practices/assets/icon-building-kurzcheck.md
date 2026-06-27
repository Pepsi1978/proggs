# App-Icon-Building Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Asset für alle Plattformen | EIN 1024² PNG, straight Alpha, quadratisch, Motiv in Safe-Zone | TL;DR 1 |
| 2 | KI-Output / JPEG als Quelle | Nie ohne Alpha; nach Konvertierung min==0 prüfen | TL;DR 2 |
| 3 | OS maskiert (Android/iOS/Tahoe) | Volles Quadrat liefern, nie selbst runden | TL;DR 3 |
| 4 | `.ico`-Konvertierung | Pillow `convert("RGBA")` + `sizes`; IM kein `-flatten`/`-background` | TL;DR 4, Pillow-Workflow |
| 5 | WPF Icon einbinden | `ApplicationIcon` + `Window.Icon`, Datei als `Resource` | WPF |
| 6 | Icon-Änderung auf Windows | Cache-DBs + Explorer neu, IconLocation `<App>.exe,0` | TL;DR 5, WPF |
