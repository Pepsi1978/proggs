# Kotlin Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Gradle-Build konfigurieren | `kotlin { jvmToolchain(21) }` + `compilerOptions {}` (kein `kotlinOptions` — 2.3 ERROR) | §1, §10 |
| 2 | Verzweigung ueber sealed/enum | `when` als Expression ohne `else` — Exhaustiveness erzwungen | §1 |
| 3 | Coroutine starten | Dispatcher injizieren, strukturierte Scopes, `CancellationException` re-werfen, kein `GlobalScope`/`runBlocking` | §2 |
| 4 | Flow in Compose anzeigen | `collectAsStateWithLifecycle()` + `stateIn(.., WhileSubscribed(5000), initial)` | §5 |
| 5 | One-shot-Events | `Channel`+`receiveAsFlow()`, nicht SharedFlow | §2, §5 |
| 6 | Compose-Stabilitaet | Strong Skipping ersetzt kein `@Immutable`/`ImmutableList`; `rememberSaveable` fuer User-State | §6, §7 |
| 7 | Collections deklarieren | Read-only `List`/`Set`/`Map`, `buildList{}`, `val`; immutable fuer Compose | §4 |
| 8 | Annotation-Processing | KSP2 statt KAPT (KSP1 ab 2.3 inkompatibel) | §10 |
| 9 | Release minifizieren | R8 fullMode, schmale keep-Rules, Release IMMER testen | §10 |
| 10 | Typsichere IDs / Vorbedingungen | `value class` fuer IDs; `require/check/error`; `!!` vermeiden | §1, §3 |
