# Jetpack Compose (Android-UI) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Endlos-Recompose / ANR / OOM | State nie in Composition schreiben, nur in Events/Effekten | §1.1 |
| 2 | Lazy-Liste (Insert/Reorder, Item-Anim) | Immer stabiler `key = { it.id }` an `items()` | §4.1 |
| 3 | Crash „Key … was already used" | Keys eindeutig machen (`distinctBy`/zusammengesetzt) | §4.2 |
| 4 | State weg nach Rotation/Recycling | `rememberSaveable` (klein!) oder ViewModel statt `remember` | §2.1 |
| 5 | Crash `TransactionTooLargeException` | Nur IDs/kleine Werte saven, grosses in ViewModel/Room | §2.7 |
| 6 | Effekt stale / `LaunchedEffect(Unit)` | Key = alle gelesenen veraenderlichen Werte | §3.1 |
| 7 | Langlebiger Effekt nutzt alten Wert | `rememberUpdatedState` statt Effekt-Neustart | §3.3 |
| 8 | Coroutine im Composable-Body | `LaunchedEffect`/`rememberCoroutineScope` nur in Callbacks | §3.5 |
| 9 | Composable skippt nicht (`List`/`copy()`) | `ImmutableList`/`@Immutable`; Strong Skipping cmp per `===` | §1.2 |
| 10 | Modifier-Reihenfolge falsch | Layout → `clip`→`background`→`border` → `clickable` | §5.1 |
| 11 | „Compose ist langsam" | Performance NUR im Release-Build (R8) messen | §10.1 |
| 12 | Jank beim Scrollen/Animieren | Hochfrequente Reads in `graphicsLayer{}`/`offset{}` deferren | §10.3 |
| 13 | Content unter TopBar/hinter NavBar | Scaffold-`innerPadding` IMMER anwenden | §8.1 |
| 14 | Crash „infinity constraints" | Kein verschachteltes gleichachsiges Scrollen | §6.1 |
| 15 | type-safe Nav crasht (custom Typ) | Eigenen `NavType` per `typeMap` registrieren | §7.4 |
| 16 | Gespeicherte Aenderung (DataStore/Room) erscheint erst beim naechsten Tap | Flow per `remember(context){…}` / ViewModel-`stateIn` stabil halten — NIE roh im Composable-Body neu bauen | §2.14 |
| 17 | Rand laeuft ungleichmaessig aus (vorne hell, hinten dunkel) | Kein `sweepGradient` als Border — `linearGradient` mit nur deckenden Stops | §5.3 |
| 19 | Umsortieren wirkt wirkungslos, obwohl die Daten neu sortiert ankommen | Schluessel-Verankerung: `LaunchedEffect(sortierung){ listState.scrollToItem(0) }` | §4.2b |
| 18 | Glanz-/Bevel-Overlay faerbt auch den Text | `drawBehind` nach `background` statt `drawWithContent` | §5.4 |
