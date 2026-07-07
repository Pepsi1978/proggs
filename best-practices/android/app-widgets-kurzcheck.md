# App-Widgets (Jetpack Glance + klassische RemoteViews) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice |
|---|-----------|---------------|
| 1 | Neues Widget planen | **Einfach (Text/Icon/Buttons, kein versteckter Scrollbar, keine Custom-Properties) → Glance. Scrollbare Liste mit verstecktem Scrollbar / ungewoehnliche View-Properties → von Anfang an klassisch** (AppWidgetProvider + RemoteViewsService). |
| 2 | Toggle/Schalter | **NIE klickbar im Widget** — in die App/Widget-Settings auslagern; im Widget nur Status-Indikator + Settings-Tap. |
| 3 | Glance-Bug an nicht-exposed Property | Max **3 Iterationen** (1 Recherche-Cycle), dann klassisch — nicht endlos Workarounds. |
| 4 | Klassisches RemoteViews-Layout | NUR erlaubte View-Klassen; **kein `<View>`** (→ `<ImageView>`); Shape-Drawables per `setBackgroundTintList` faerben, nicht `setBackgroundColor`. |
| 5 | Glance-Daten laden | DB/Netzwerk in `provideGlance()` (laeuft im Worker), nicht im Composable. |
| 6 | Glance-Groessen | `SizeMode.Responsive` (Android 12+) mit konkreten `DpSize`-Buckets statt `Exact` (Performance). |
| 7 | Glance-Liste | `items(itemId = {…})` fuer Scrollposition + Performance (A12+). |
| 8 | Ressourcen/Farben | **Resource-IDs** an Glance geben (`ImageProvider(R.drawable…)`, `background(R.color…)`) statt aufgeloester Werte → kleinere RemoteViews + dynamische Ressourcen. |
