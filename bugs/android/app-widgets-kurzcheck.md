# App-Widgets (Jetpack Glance + klassische RemoteViews/AppWidgetProvider) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ Scrollbar in Glance-`LazyColumn` soll **versteckt** werden | Geht in Glance **NICHT** (LazyColumn = ListView, Glance exposed kein Scrollbar-Modifier; Launcher wenden das Receiver-Theme nicht zuverlaessig an). → klassisches `AppWidgetProvider` + `ListView android:scrollbars="none"` | §1 |
| 2 | ⭐ Klickbarer **Toggle/Schalter direkt im Widget** | Unzuverlaessig (Launcher-Caching Nova/One UI + Glance-Worker-Queue-Dedup). → Toggle in **App-Settings** auslagern; im Widget nur Status-Indikator + Tap oeffnet Settings | §2 |
| 3 | ⭐ Glance-Bug an einem Property, das Glance **nicht exposed** | **Max 3 Iterationen**, dann klassisch (AppWidgetProvider + RemoteViewsService). Nicht endlos Workarounds raten | §3 |
| 4 | Klassisches RemoteViews-Layout: Widget bleibt **komplett weiss** | Inflate failt an `<View>` — RemoteViews akzeptiert nur eine **feste Klassenliste** (kein `<View>`). → `<ImageView>` als Spacer/Hintergrund | §4 |
| 5 | Pillen/Buttons **eckig statt rund** nach Farb-Setzen | `setInt(id,"setBackgroundColor",..)` ueberschreibt das Shape-Drawable mit ColorDrawable. → `setColorStateList(id,"setBackgroundTintList", ColorStateList.valueOf(c))` (tintet das Shape, behaelt Ecken) | §5 |
| 6 | **Custom-Font** im Glance-Widget | NICHT unterstuetzt — nur System-Fonts (`FontFamily.Monospace` etc.) | §6 |
| 7 | LazyColumn **verliert Scrollposition** / langsam bei Update (A12+) | `items(itemId = { ... })` setzen | §7 |
| 8 | DB-/Netzwerk-Arbeit fuers Widget | In `provideGlance()` laden (laeuft im WorkManager-Worker), NICHT im Composable; `provideContent {}` nur fuer UI | §8 |
| 9 | `<View>`-Tags per `sed` ersetzen schlaegt fehl (mehrzeilig) | `sed` arbeitet zeilenweise → Python `re.sub(r'<View(\s)', r'<ImageView\1', s)` | §5 |
