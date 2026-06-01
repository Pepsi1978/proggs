# Kotlin — Best Practices (Stand 2026-06-02, Version Kotlin 2.1.0 Projekt-Plugin / 2.3.20 CLI, Compose BOM 2026.03, AGP 8.10, compileSdk 35–37)

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/kotlin.md`](../../../bugs/kotlin.md)): der
> Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von
> vornherein richtig macht, damit der Bug gar nicht erst entsteht*. Jede Best-Practice ist
> funktionserhaltend (entfernt nie ein Feature). Quellen-Rangordnung wie im Ordner: offizielle
> JetBrains/Kotlin/Android-Quelle = Grundwahrheit (`offiziell`), Community/Blogs = `extern`.

---

## ⚡ TL;DR — die Defaults, die man einmal richtig setzt

1. **`kotlin { jvmToolchain(17) }`** statt manueller source/targetCompatibility — setzt Java+Kotlin synchron (Poka-Yoke gegen JVM-Target-Mismatch).
2. **`when` als Expression** (Zuweisung/return) ohne `else` bei sealed/enum — der Compiler erzwingt Vollstaendigkeit und meldet neue Subtypen.
3. **Coroutines:** strukturierte Scopes (`viewModelScope`/`coroutineScope{}`), `CancellationException` immer re-werfen, kein `GlobalScope`, kein `runBlocking` auf Main.
4. **Flow→Compose:** `collectAsStateWithLifecycle()` + `stateIn(..., WhileSubscribed(5000), initial)`.
5. **Compose-Stabilitaet von Anfang an:** `@Immutable`-Datentypen + `ImmutableList`/`ImmutableMap` (kotlinx.collections.immutable), `rememberSaveable` fuer User-State.
6. **Release-Build absichern:** schmale R8-keep-Rules fuer Serialization/Reflection/Coroutines und Release-Variante *vor* dem Store testen.
7. **KSP statt KAPT**, KSP-Plugin-Version exakt zur Kotlin-Version im Version-Catalog; Compose-Compiler als Gradle-Plugin (kein `composeOptions`).

---

## 1. Sprache & K2-Compiler

- **Explizit typisieren statt auf Inferenz hoffen.** K2 (default ab 2.0, nicht abschaltbar) ist
  bei Inferenz/Smart-Cast/Nullability strenger. Oeffentliche API-Signaturen explizit typisieren —
  robuster gegen Compiler-Versionswechsel. *(offiziell: k2-compiler-migration-guide)*
- **`when` ueber sealed/enum als Expression, ohne `else`.** Erzwingt Exhaustiveness; ein neuer
  Subtyp bricht den Build sofort sichtbar statt still in einen `else`-Zweig zu fallen. *(offiziell)*
- **Moderne stdlib-APIs verwenden:** `lowercase()`/`uppercase()` (+ ggf. `Locale.getDefault()`),
  `appendLine()`, `androidTarget`. Die alten (`toLowerCase()` etc.) sind ab 2.1.0 Error. *(offiziell: compatibility-guide-21)*
- **Aktuelle Gradle-DSL:** `compilerOptions {}` statt `kotlinOptions {}`, `kotlin-parcelize` statt
  `kotlin-android-extensions`, `languageVersion ≥ 1.8`. Verhindert die 2.2.0-Deprecation-Errors. *(offiziell: compatibility-guide-22)*
- **`buildList<T> { }`** mit explizitem Typ-Argument, wenn der Elementtyp nicht offensichtlich ist
  (K2-Inferenz, KT-58149). *(offiziell)*

## 2. Coroutines

- **Strukturierte Concurrency immer.** Scope an den Lebenszyklus binden (`viewModelScope`,
  `lifecycleScope`, `rememberCoroutineScope`), in suspend-Funktionen `coroutineScope { }`. Kein
  `GlobalScope`, kein loses `CoroutineScope(Job())`. Cancellation propagiert dann automatisch. *(offiziell: coroutines-basics)*
- **`CancellationException` ist heilig.** Niemals breit `catch (e: Exception)` ohne Re-Throw —
  Muster: `catch (e: Exception) { if (e is CancellationException) throw e; … }`. Sonst bricht
  kooperative Cancellation. *(offiziell: exception-handling)*
- **Lange Schleifen kooperativ machen:** `ensureActive()`/`yield()`/`isActive` einbauen, sonst
  ignoriert CPU-Arbeit ein `cancel()`. *(offiziell)*
- **`async` → try-catch um `await()`**; `CoroutineExceptionHandler` nur am aeussersten `launch`
  (Root). Handler im Kind wirkt nicht. Fuer unabhaengige Geschwister `supervisorScope { }`. *(offiziell)*
- **`viewModelScope.launch {}` immer mit eigener Fehlerbehandlung** (try/catch oder
  `CoroutineExceptionHandler`) — der Scope hat keinen Default-Handler, ungefangenes crasht die App. *(extern: Android-Community, deckt sich mit offiziellem viewModelScope-Verhalten)*
- **Kein redundantes `withContext(Dispatchers.IO)`** um Retrofit/Room-suspend-Funktionen — die sind
  bereits main-safe. *(offiziell: Android coroutines-best-practices)*

## 3. Null-Safety

- **Java-Grenzen explizit nullable behandeln.** Unannotierte Java-Rueckgaben sind Platform Types
  (`String!`) — als `String?` behandeln oder die Java-Seite mit `@Nullable`/`@NonNull` annotieren. *(offiziell: java-interop)*
- **`lateinit` nur fuer non-null Referenztypen, die garantiert vor erstem Zugriff gesetzt werden**;
  sonst `by lazy` oder nullable + Default. Vor riskantem Zugriff `::prop.isInitialized`. *(offiziell)*
- **`!!` vermeiden** — `?.let {}`, Elvis `?:` oder echte Null-Behandlung. *(offiziell)*

## 4. Flow / StateFlow in Compose

- **`collectAsStateWithLifecycle()`** statt `collectAsState()` — stoppt die Collection bei
  STOPPED-Lifecycle (kein Akku-Drain, keine Hintergrund-Recomposition). *(offiziell: lifecycle-runtime-compose ≥ 2.6)*
- **`stateIn(scope, SharingStarted.WhileSubscribed(5000), initial)`** fuer ViewModel-StateFlows —
  `WhileSubscribed(0)` wuerde den Upstream bei jeder kurzen UI-Pause stoppen. *(offiziell: StateFlow/SharedFlow-Doku)*
- **One-shot-Events ueber `Channel` + `receiveAsFlow()`**, nicht ueber SharedFlow (das verliert
  Emissionen bei Lifecycle-Pausen). Geteilte Cold-Flows mit `shareIn`/`stateIn` zu Hot machen. *(offiziell)*
- **Flow EINMAL als ViewModel-Property halten**, nicht pro Aufruf neu bauen; `flowOn` ueber die
  betroffenen Operatoren (vor `collect`). *(offiziell)*

## 5. data class & Immutability

- **Identitaetsrelevante Felder in den Primaerkonstruktor** — `equals`/`hashCode` beruecksichtigen
  nur die. Body-Felder, die zur Gleichheit zaehlen sollen, brauchen manuelles `equals`/`hashCode`. *(offiziell)*
- **`List` statt `Array`** in data classes (Array = Referenzvergleich); identitaetsrelevante Felder
  `val`, nicht `var` (sonst kaputter Map-Key). `copy()` ist Shallow Copy — bei mutable Inhalten bewusst Deep Copy. *(offiziell)*

## 6. Compose — Stabilitaet & Recomposition

- **Stabile Parameter von Anfang an.** Compose stuft `var`-Klassen, Fremd-Modul-Typen und ALLE
  `List`/`Map`/`Set` als instabil ein → kein Skipping. Praevention: `@Immutable`/`@Stable`-Datentypen,
  `ImmutableList`/`ImmutableMap` aus `kotlinx.collections.immutable`, Stability-Config-File fuer
  unkontrollierbare Fremdtypen. Strong Skipping (ab 2.0.20) mildert, ersetzt das NICHT. *(offiziell: compose stability)*
- **Keine Backwards Writes** — nie in der Composition in einen State schreiben, der vorher gelesen
  wurde (Endlosschleife). State-Schreibvorgaenge in Side-Effects/Event-Handler. *(offiziell)*
- **Teure Berechnungen nicht im Composable-Body** — in `remember(key)` cachen oder ins ViewModel. *(offiziell)*

## 7. Compose — State

- **`rememberSaveable` fuer User-Input/Scroll/Selektion** (ueberlebt Rotation/Dark-Mode/Split-Screen);
  `remember` nur fuer transienten UI-State. *(offiziell: state-saving)*
- **State hoisten an ViewModel (scoped an NavBackStackEntry)** fuer Tab-/Navigations-Wechsel. Achtung:
  `rememberSaveable` in navigation-compose ist teils unzuverlaessig (issuetracker 298059596). *(offiziell + lokales Learning compose_state_across_tab_switch)*
- **`derivedStateOf`** nur wenn aus haeufig wechselnden Quellen ein selten wechselndes Ergebnis
  abgeleitet wird — nicht bei 1:1-Abhaengigkeit. *(offiziell)*
- **Kritische Daten echt persistieren** (DataStore/DB) — `rememberSaveable` ueberlebt kein App-Swipe-Kill. *(offiziell)*

## 8. Compose — Side-Effects

- **`LaunchedEffect`-Keys = alle im Block genutzten veraenderlichen Werte.** `LaunchedEffect(Unit)`/`(true)`
  ist so verdaechtig wie `while(true)`. *(offiziell: side-effects)*
- **Wert, der den Effekt NICHT neustarten soll → `rememberUpdatedState`** (Effekt mit konstantem Key). *(offiziell + lokales Learning compose_waveform_pattern)*
- **`DisposableEffect` immer mit `onDispose { cleanup() }`.** Composition-/Entry-Arbeit →
  `LaunchedEffect`; Event-getriebene Arbeit (onClick) → `rememberCoroutineScope().launch`. *(offiziell)*

## 9. Compose — Performance

- **`LazyColumn`/`LazyRow` immer mit `key = { it.id }`** (+ `contentType` bei heterogenen Listen) —
  verhindert State-Mismatch und unnoetige Recomposition nach Insert/Delete. *(offiziell)*
- **Haeufig wechselnde Werte als Lambda-Provider** (`() -> Float`) weitergeben und State erst in
  `Modifier.offset{}` / `graphicsLayer{}` / `drawBehind{}` lesen — verschiebt die Invalidation von
  Composition auf Layout/Draw. *(offiziell: compose performance, deferred reads)*

## 10. Build & Toolchain

- **KSP statt KAPT**, und KSP-Plugin-Version IMMER exakt zur Kotlin-Version (z.B. `2.1.0-1.0.29`),
  im Version-Catalog parallel bumpen. KSP1 endet 2025 und ist mit Kotlin 2.3/AGP 9.0 inkompatibel —
  rechtzeitig auf KSP2 migrieren. *(offiziell: ksp2.md)*
- **Compose-Compiler als Gradle-Plugin** (`org.jetbrains.kotlin.plugin.compose`, Version = Kotlin-Version);
  altes `composeOptions { kotlinCompilerExtensionVersion }` entfernen. Plugin-Reihenfolge: Compose
  nach Serialization anwenden. *(offiziell: compose-compiler-migration-guide)*
- **`kotlin { jvmToolchain(17) }`** statt manueller `sourceCompatibility`/`targetCompatibility` —
  haelt Java- und Kotlin-JVM-Target synchron (Poka-Yoke gegen „Inconsistent JVM-target"). *(offiziell: gradle-configure-jvm)*
- **Schmale R8-keep-Rules statt Klassen behalten/loeschen.** Fuer kotlinx.serialization
  (`-keep @kotlinx.serialization.Serializable class *` + `-if @Serializable` fuer benannte Companions),
  Coroutines-volatile-Felder, DTO/`@SerializedName`-Modelle. So eng wie moeglich, nie pauschal
  `-keep class **`. Release-Variante VOR dem Store testen — diese Crashes treten nur minifiziert auf. *(offiziell: android-developers R8 Keep Rules 2025)*
- **Remote-Build-Cache fuer Kotlin-Compile-Tasks aus** (lokalen behalten), gleiche JDK-Major-Version
  auf allen Maschinen — sonst bricht Incremental Compilation (KT-34862). *(extern: bekannter offener KT-Issue)*
- **Hilt/Dagger auf aktuelle, KSP2-faehige Version**; alle Processor auf `ksp`-Konfiguration,
  Dagger-Optionen als `ksp { arg(...) }`. (dagger #4303 KSP2-Inkompat = gefixt 2024-12.) *(offiziell: gh dagger #4303)*

---

## Pflege

Neue belegte Best-Practice (offizielle Quelle bevorzugt) hier ergaenzen, Stand-Header + Version
aktualisieren. Bei Versionssprung der genutzten Kotlin/Compose/AGP-Version: kurzer Re-Check, ob
Empfehlungen noch gelten. Gegenstueck-Bugs immer auch in [`bugs/kotlin.md`](../../../bugs/kotlin.md) pflegen.
