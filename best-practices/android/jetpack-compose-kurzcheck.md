# Jetpack Compose Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Architektur/State-Fluss | UDF: State runter, Events rauf; Business-Logik nie ins Composable | §1 |
| 2 | State geteilt/zu tief | State-Hoisting (`value`+`onValueChange`) auf gemeinsamen Vorfahren | §1 |
| 3 | Screen-State + ViewModel-Grenze | EIN immutable State; `collectAsStateWithLifecycle()` + `stateIn` | §1 |
| 4 | Composable skippt nicht | `ImmutableList`/`@Immutable`; `@Immutable` nie luegen | §2 |
| 5 | `remember`/`rememberSaveable` | `by remember { mutableStateOf }` (Imports!); Saveable nur klein | §3 |
| 6 | Liste reaktiv halten | `mutableStateListOf` statt `mutableStateOf(list)` | §3 |
| 7 | Side-Effect waehlen | Richtige API je Fall; Keys = gelesene Werte; kritisch in `viewModelScope` | §4 |
| 8 | Lazy-Liste / Pager | Stabiler eindeutiger `key`+`contentType`; `fillParentMaxSize()` | §5 |
| 9 | Modifier-Reihenfolge | Layout → `clip`→`background`→`border` → `clickable`; eigene via `Modifier.Node` | §6 |
| 10 | Theming / Insets | `dynamicColor`+API-Guard; Scaffold-`innerPadding`, keine Doppel-Insets | §7 |
| 11 | Navigation | type-safe Routes, nur IDs; geteiltes VM am Parent-Entry | §8 |
| 12 | Tablet/Foldable | `windowSizeClass` statt `Configuration.orientation` | §9 |
| 13 | Animation | `animate*AsState` deklarativ binden, nicht in `SideEffect`/`LaunchedEffect` | §10 |
| 14 | Performance/Preview | Release (R8) messen; Reads deferren; `@Preview` stateless | §11 |
