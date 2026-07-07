# Kotlin (Sprache + K2-Compiler, Android/Compose-Kontext) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Code baute unter 1.9, bricht unter 2.0 | K2 ist default, nicht abschaltbar — explizit typisieren, nie Code entfernen | §1.1 |
| 2 | `catch(e: Exception)` um Coroutine | `CancellationException` re-werfen, nie verschlucken | §2.1 |
| 3 | Coroutine-Scope/Builder | Kein `GlobalScope`, kein eigenes `Job()` an Builder, kein `runBlocking` auf Main | §2.2, §2.6 |
| 4 | Flow in Compose sammeln | `collectAsStateWithLifecycle()`, nie `collectAsState()` | §4.1 |
| 5 | StateFlow via `stateIn` | `WhileSubscribed(5000)`, nie `0` | §4.2 |
| 6 | Composable recomposed staendig | Instabile Params (`List`/`var`) → `@Immutable`/`ImmutableList` | §6.1 |
| 7 | User-State weg nach Rotation | `rememberSaveable` statt `remember` | §7.1 |
| 8 | `LaunchedEffect(Unit)`/`(true)` | Alle veraenderlichen Werte als Keys — sonst verdaechtig | §8.1 |
| 9 | Release-Crash nur minifiziert | R8 keep-Rules fuer Serialization/Reflection/Coroutines, nie Klasse loeschen | §10.3 |
| 10 | KSP `too old`-Fehler | KSP-Version exakt zur Kotlin-Version; KSP1 endet ab Kotlin 2.3 | §10.1 |
| 11 | Compose-Build-Konflikt | Compose-Compiler ist Gradle-Plugin, `composeOptions` entfernen | §10.2 |
| 12 | JVM-Target inkonsistent | `kotlin { jvmToolchain(17) }` statt source/targetCompatibility | §10.4 |
