# Bekannte Bugs & Fallen: Kotlin (Sprache + K2-Compiler, Android/Compose-Kontext)

> **PFLICHT-LESEN vor JEDER Kotlin-Arbeit.** Kuratierter Bug-Almanach: K2-Compiler-Migration,
> Coroutines, Null-Safety, Flow, data class, Jetpack Compose, Build/Toolchain. Loesungen sind
> funktionserhaltend (nie "Feature weglassen"). Quellen: offizielle Kotlin-Doku (JetBrains),
> YouTrack/GitHub-Issues, Community + harter gh-Issue-Status.
>
> **Stand:** recherchiert am **2026-06-02**, **re-recherchiert am 2026-06-24** (Engine A: Firecrawl+MiniMax)
> fuer **Kotlin 2.1.0** (Projekt-Plugin) bis **2.4.0**
> **Anker:** kotlin=2.1.0  <!-- maschinenlesbar fuer check-version-anchor.py -->
> (CLI), Compose BOM 2026.03, AGP 8.10, compileSdk 35–37. Versionsangaben pro Bug beachten —
> viele K2-Punkte sind "per Design" (gewollte Breaking Changes), viele Coroutine/Compose-Fallen
> sind versionsunabhaengig. Was schon gefixt ist: siehe Fix-Status-Sektion am Ende.
>
> **Versions-Horizont (Re-Recherche 2026-06-24):** Inzwischen released: **Kotlin 2.3.20** (2026-03-16,
> Kotlin/JVM nutzt **Build Tools API** als Default, Gradle-9.3-Kompat) und **Kotlin 2.4.0** (2026-06-03:
> **stabile Context Parameters**, stabile Explicit Backing Fields, stabile common-UUIDs, Java-26-Support,
> Gradle-9.5-Kompat). `kotlinx.coroutines` ist bei **1.11.0**. Euer Pin (Kotlin 2.1.0) bleibt gueltig —
> der Anker ist projekt-gepinnt. Beim geplanten Kotlin-2.3/2.4-Sprung §1.10 beachten.

---

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

---

## 🔗 Bezug zu den Best-Practices ([`best-practices/android/kotlin.md`](../../best-practices/android/kotlin.md))

Zweite Seite der Medaille: Hier steht *was schiefgeht und wie man es loest*, die Best-Practices sagen
*wie man den Bug von vornherein vermeidet*. Pro Bug-Abschnitt der passende Praeventions-Abschnitt —
nach einem Fix immer auch dort verankern, damit der Fehler nicht wiederkommt:

| Bug-Abschnitt (hier) | Praevention in best-practices/android/kotlin.md |
|----------------------|---------------------------------------------------|
| §1 K2-Compiler & Sprach-Migration | §1 Sprache & Idiome (K2) |
| §2 Coroutines | §2 Coroutines & Flow |
| §3 Null-Safety | §3 Null-Safety & Fehlerbehandlung |
| §4 Flow / StateFlow / SharedFlow | §5 Flow / StateFlow in Compose |
| §5 data class | §4 Collections & Immutability |
| §6 Recomposition & Stabilitaet | §6 Compose — Stabilitaet & Recomposition |
| §7 State & remember | §7 Compose — State |
| §8 Side-Effects | §8 Compose — Side-Effects |
| §9 Performance | §9 Compose — Performance |
| §10 Build & Toolchain | §10 Build & Toolchain |

---

## 1. K2-Compiler & Sprach-Migration (Kotlin 2.0 → 2.4)

### 1.1 K2 ist default und nicht abschaltbar (2.0.0)  ⭐
**Symptom:** Code der unter 1.9 baute, bricht unter 2.0 (Inferenz/Smart-Cast/Nullability strenger).
**Versionen:** K2 default ab **2.0.0** (`kotlin.useK2` entfernt). Rollback nur via `languageVersion=1.9` (temporaer).
**FIX:** Fehlerstellen explizit typisieren/casten, Compatibility-Guide durchgehen — NIE Code entfernen.
**Quelle:** kotlinlang.org/docs/k2-compiler-migration-guide.html, compatibility-guide-20.

### 1.2 Smart-Cast aggressiver, aber `val x: T = y` propagiert Narrowing NICHT mehr
**Symptom:** alte `?.invoke()`/Null-Checks ueberfluessig (Warnung); ODER Smart-Cast bricht, wo er in 1.9 ging.
**Ursache:** K2 castet Funktionstyp-Properties/Inline-Lambdas/catch korrekt; bidirektionale Narrowing-
Propagation aber nur noch bei `val x = y` (ohne Typ-Annotation).
**Versionen:** ab 2.0.0 (Smart-Cast), 2.1.0 (Narrowing-Aenderung).
**FIX:** ueberfluessige Null-Checks entfernen; wo Narrowing bricht, Typ-Annotation weglassen oder Type-Guard setzen.

### 1.3 Exhaustiveness strenger (sealed/when)
**Symptom:** bisher gruenes `when` ueber sealed/enum faellt durch.
**Versionen:** ab 2.0.0 (Statement-`when` ueber sealed exhaustiv seit 1.7 Warnung → Error).
**FIX:** fehlende Branches ergaenzen; `when` als Expression (return/Zuweisung) nutzen — dann erzwingt der Compiler Vollstaendigkeit. `else` weglassen, damit neue Subtypen auffallen.

### 1.4 `open val` ohne Initializer → MUST_BE_INITIALIZED (2.0.0)
**FIX:** bei Deklaration initialisieren / `final` / privates Backing-Property mit Getter.

### 1.5 Deprecation→Error in 2.1.0
**Symptom:** `toLowerCase()`/`toUpperCase()`/`Char.toUpperCase()`, `StringBuilder.appendln()`, `android`-MPP-Target → Compile-Error.
**FIX:** `lowercase()`/`uppercase()` (ggf. mit `Locale.getDefault()`), `appendLine()`, `androidTarget`.
**Quelle:** compatibility-guide-21.

### 1.6 Deprecation→Error in 2.2.0
**Symptom:** `kotlinOptions {}` DSL, `-language-version=1.6/1.7`, `kotlin-android-extensions`,
`kotlin.incremental.useClasspathSnapshot`, private Typen in non-private `inline` → Error. Interface-
Methoden werden JVM-Default-Methods (Java-Konflikte).
**FIX:** `compilerOptions {}`, ≥1.8, `kotlin-parcelize`-Plugin, Property loeschen, `inline` privat machen
oder entfernen, ggf. `-jvm-default=disable`.
**Quelle:** compatibility-guide-22.

### 1.7 JSpecify-Nullability jetzt strict (2.1.0)
**Symptom:** `@Nullable`/`@NonNull`/`@NullMarked`-Warnungen aus Java werden Errors.
**FIX:** Nullability fixen; uebergangsweise `-Xnullability-annotations=@org.jspecify.annotations:warning`.

### 1.8 `Map.Entry` nach Map-Modifikation → ConcurrentModificationException (2.1.0)
**FIX:** Entries vor der Modifikation snapshotten (Fail-Fast wird jetzt erzwungen).

### 1.9 K2 `buildList`-Typinferenz schlaegt fehl
**Symptom:** `buildList { }` ohne erkennbaren Elementtyp → Inferenz-Fehler.
**Versionen:** KT-58149.
**FIX:** explizites Typ-Argument `buildList<T> { }`.

### 1.10 Context-Parameter-Overload-Resolution geaendert (2.3.20) → mehrdeutige Aufrufe  ⭐ (Re-Recherche 2026-06-24, Upgrade-relevant)
**Symptom:** Nach dem Upgrade auf **Kotlin 2.3.20** brechen Aufrufe von Funktions-Overloads, die sich
NUR durch Context-Parameter unterscheiden, mit `OVERLOAD_RESOLUTION_AMBIGUITY`.
**Ursache:** Kotlin 2.3.20 hat die Overload-Resolution fuer Context-Parameter geaendert; vorher eindeutige
Aufrufe werden mehrdeutig. Context Parameters sind in **2.4.0 stabil** geworden.
**Versionen:** Verhaltensaenderung ab Kotlin 2.3.20; betrifft Code, der Context-Parameter nutzt.
**FIX (funktionserhaltend):** In 2.4.0 die **expliziten Context-Argumente** verwenden
(`-Xexplicit-context-arguments`, experimentell) — den Aufruf eindeutig machen, NICHT einen Overload loeschen.
Euer Pin (2.1.0) nutzt noch keine Context-Parameter → erst beim 2.3/2.4-Sprung relevant.
**Quelle:** kotlinlang.org/docs/whatsnew-eap.html (What's new in Kotlin 2.4.0) · blog.jetbrains.com/kotlin/2026/06/kotlin-2-4-0-released/

---

## 2. Coroutines

### 2.1 `CancellationException` verschluckt  ⭐ HAEUFIG
**Symptom:** Coroutine laeuft nach `cancel()` weiter, Cleanup bricht ab.
**Ursache:** `catch(e: Exception)` faengt auch `CancellationException` ab und unterbricht die Cancellation.
**Versionen:** unabhaengig.
**FIX:** spezifisch fangen ODER `if (e is CancellationException) throw e` re-werfen.

### 2.2 GlobalScope-Leak / loses Starten
**Symptom:** Job laeuft nach Screen-Verlassen weiter, Akku-Drain, Leak.
**FIX:** `viewModelScope`/`lifecycleScope`/`rememberCoroutineScope` nutzen; in suspend-Funktionen `coroutineScope { }`. Strukturierte Concurrency cancelt automatisch.

### 2.3 Kooperative Cancellation ignoriert
**Symptom:** `job.cancel()` tut nichts in langer Schleife.
**Ursache:** Cancellation ist kooperativ — ohne Suspension-Point keine Pruefung.
**FIX:** in Schleifen `ensureActive()` / `yield()` / `isActive` pruefen.

### 2.4 `runBlocking` auf Main/Hot-Path
**Symptom:** UI friert ein.
**FIX:** in suspend-Kontext nie `runBlocking`; suspendieren. Nur in `main()`/Tests.

### 2.5 try-catch um `launch {}` greift nicht
**Symptom:** Exception aus `launch` wird nicht gefangen, App crasht.
**Ursache:** Exceptions propagieren zum Parent-Job, nicht ins umgebende try-catch.
**FIX:** try-catch INNEN im Coroutine-Block oder `CoroutineExceptionHandler` am Root-`launch`.

### 2.6 Eigenes `Job()`/`SupervisorJob()` an Builder uebergeben
**Symptom:** Kind wird bei Parent-Cancel nicht abgebrochen / Geschwister trotzdem gecancelt.
**Ursache:** uebergebenes Job wird neuer Parent → bricht structured concurrency.
**FIX:** den von `launch{}` gelieferten Job nutzen; fuer unabhaengige Fehler `supervisorScope { }` als Scoping-Funktion.

### 2.7 `async`-Exception erst bei `await()`; Handler im Kind wirkungslos
**FIX:** bei `async` try-catch um `await()`; `CoroutineExceptionHandler` nur am aeussersten `launch` (Root).

### 2.8 `viewModelScope` hat keinen ExceptionHandler → Crash
**Symptom:** ungefangene Exception in `viewModelScope.launch{}` crasht die App.
**Ursache:** `viewModelScope` = `SupervisorJob + Main.immediate`, aber KEIN ExceptionHandler.
**FIX:** try/catch im Body oder expliziten `CoroutineExceptionHandler` an `launch`.

### 2.9 Unnoetiges Dispatcher-Switching
**Symptom:** redundantes `withContext(Dispatchers.IO)` um Retrofit/Room.
**Ursache:** suspend-Funktionen von Retrofit/Room sind bereits main-safe.
**FIX:** kein Wrapping; auf main-safe-Konvention vertrauen.

---

## 3. Null-Safety

### 3.1 Platform Type `String!` aus Java → NPE
**Ursache:** unannotiertes Java liefert Platform Type; Compiler erlaubt Zuweisung als non-null.
**FIX:** Java-Rueckgaben explizit als `String?` behandeln oder Java mit `@Nullable`/`@NonNull` annotieren.

### 3.2 `lateinit` vor Init / fuer primitive/nullable Typen
**Symptom:** `UninitializedPropertyAccessException`; oder Compile-Fehler.
**FIX:** `::prop.isInitialized` pruefen (nur lexikalisch erreichbar); fuer primitive/optionale Werte `by lazy` oder nullable Typ + Default.

### 3.3 `!!` als Workaround
**FIX:** `?.let { }`, Elvis `?:` oder echte Null-Behandlung statt `!!`.

---

## 4. Flow / StateFlow / SharedFlow

### 4.1 `collectAsState()` statt `collectAsStateWithLifecycle()`  ⭐
**Symptom:** Flow sammelt im Hintergrund weiter, Akku-Drain, Recomposition unsichtbarer Screens.
**Ursache:** `collectAsState` stoppt nicht bei STOPPED-Lifecycle.
**Versionen:** `collectAsStateWithLifecycle` ab lifecycle-runtime-compose 2.6+.
**FIX:** `collectAsStateWithLifecycle()` (nutzt `repeatOnLifecycle`).

### 4.2 `WhileSubscribed(0)` → StateFlow hoert auf zu emittieren
**Symptom:** StateFlow stoppt nach kurzer UI-Pause/Recomposition.
**Ursache:** `stopTimeoutMillis = 0` stoppt Upstream sofort, bevor Compose re-subscribed.
**FIX:** `stateIn(scope, SharingStarted.WhileSubscribed(5000), initial)`.

### 4.3 SharedFlow verliert Emissionen / Cold-Flow doppelt
**Symptom:** Events gehen bei Lifecycle-Pausen verloren; ODER Netzwerk-Call feuert pro Collector neu.
**FIX:** one-shot Events ueber `Channel` + `receiveAsFlow()`; geteilte Cold-Flows mit `shareIn`/`stateIn` zu Hot machen; `replay`/Buffer bewusst setzen.

### 4.4 `flowOn` aendert nur Upstream; ViewModel-Flow mehrfach erzeugt
**FIX:** `flowOn` ueber die betroffenen Operatoren setzen (nicht nach `collect`); Flow EINMAL als ViewModel-Property halten, nicht pro Funktionsaufruf neu bauen.

---

## 5. data class

### 5.1 equals/hashCode nur ueber Primary-Constructor-Properties
**Symptom:** Objekte gelten als gleich, obwohl ein Body-Feld differiert.
**FIX:** identitaetsrelevante Felder in den Primaerkonstruktor; sonst `equals`/`hashCode` manuell.

### 5.2 `Array` im Primary Constructor / `copy()` teilt Collection
**Symptom:** gleiche Inhalte ungleich (Array = Referenzvergleich); `copy()` ist Shallow Copy.
**FIX:** `List` statt `Array` (oder `contentEquals`/`contentHashCode`); immutable Collections (`val`, `List`); bei mutable bewusst Deep Copy.

### 5.3 `var`-Properties → kaputter Map-Key
**Symptom:** mutiertes Objekt als Map-Key → kaputter Hash-Bucket.
**FIX:** `val` statt `var` fuer data-class-Felder.

---

> **Abgrenzung (NEU 2026-06-02):** Die Abschnitte 6–9 sind die **kompakten Compose-Grundlagen
> im Kotlin-Kontext**. Der **vollstaendige, tiefe Compose-UI-Almanach** (~74 Bugs: Recomposition,
> State, Side-Effects, Lazy/Pager, Modifier, Crashes, navigation-compose, Material3, Animation,
> Performance/Tooling) ist **`bugs/android/jetpack-compose.md`** — bei echter Compose-UI-Arbeit DORT
> nachschlagen. Bezugstabelle siehe Ende von `jetpack-compose.md`.

## 6. Jetpack Compose — Recomposition & Stabilitaet

### 6.1 Instabile Parameter erzwingen Recomposition  ⭐ HAEUFIG
**Symptom:** Composable recomposed bei jeder Parent-Aenderung, obwohl eigene Daten gleich.
**Ursache:** Compose stuft Klassen mit `var`, Fremd-Modul-Typen, ALLE `List`/`Map`/`Set` als "unstable" → kein Skipping.
**Versionen:** per Design; **Strong Skipping** (stabil ab Kotlin 2.0.20, default 2.1.x) mildert es, ersetzt es NICHT.
**FIX:** `@Immutable`/`@Stable` annotieren, `kotlinx.collections.immutable` (`ImmutableList`), Stability-Config-File fuer Fremdtypen.

### 6.2 Backwards Write → Endlosschleife
**Symptom:** Recomposition jeden Frame, endlos.
**Ursache:** in der Composition wird in einen State geschrieben, der vorher gelesen wurde.
**FIX:** State-Schreibvorgaenge in Side-Effects/Event-Handler verlagern.

### 6.3 Teure Berechnung direkt in der Composition
**FIX:** in `remember(key)` cachen oder ins ViewModel verlagern (nicht `list.sorted()` im Composable-Body).

---

## 7. Compose — State & remember

### 7.1 `remember` ueberlebt keine Config-Change/Process-Death
**Symptom:** Eingabe/Auswahl weg nach Rotation, Dark-Mode, Split-Screen.
**FIX:** `rememberSaveable` fuer User-Input/Scroll/Selektion.

### 7.2 State ueber Tab-/Navigations-Wechsel verloren
**Ursache:** `remember` ist an die disposed Composition gebunden.
**FIX:** State hoisten (ViewModel scoped an NavBackStackEntry) oder `rememberSaveable`. ACHTUNG bekannter Bug: `rememberSaveable` in navigation-compose teils unzuverlaessig (issuetracker 298059596). (Deckt sich mit lokalem Learning `compose_state_across_tab_switch`.)

### 7.3 `derivedStateOf` vergessen
**Symptom:** UI recomposed bei jedem Quellwert, obwohl das abgeleitete Ergebnis selten wechselt.
**FIX:** `val x by remember { derivedStateOf { ... } }` — emittiert nur bei Ergebnis-Aenderung. NICHT fuer 1:1-Abhaengigkeiten (dann unnoetig).

### 7.4 `rememberSaveable` ueberlebt kein App-Swipe-Kill
**Ursache:** User-Dismiss aus Recents ≠ Process-Death.
**FIX:** echte Persistenz (DataStore/DB) fuer kritische Daten.

---

## 8. Compose — Side-Effects

### 8.1 `LaunchedEffect` mit falschem Key  ⭐
**Symptom:** Effekt laeuft nie neu (obwohl Dependency sich aendert) oder staendig.
**Ursache:** `LaunchedEffect(Unit)/(true)` trotz veraenderlicher Abhaengigkeit — oder zu volatile Keys.
**FIX:** alle im Block genutzten abhaengigen Werte als Keys uebergeben. `LaunchedEffect(true)` so verdaechtig wie `while(true)`.

### 8.2 Wert soll Effekt NICHT neustarten → `rememberUpdatedState`
**FIX:** Wert in `rememberUpdatedState` wrappen, Effekt mit konstantem Key. (Deckt sich mit lokalem Learning `compose_waveform_pattern`.)

### 8.3 `DisposableEffect` ohne `onDispose` → Leak; rememberCoroutineScope vs LaunchedEffect
**FIX:** `DisposableEffect(owner){ ...; onDispose{ cleanup() } }`. Composition-/Entry-Arbeit → `LaunchedEffect`; Event-getriebene Arbeit (onClick) → `rememberCoroutineScope().launch`.

---

## 9. Compose — Performance

### 9.1 LazyColumn ohne `key` → State-Mismatch & Animations-Bugs
**Symptom:** ganze Liste recomposed nach Insert/Delete; Item-State landet beim falschen Item.
**FIX:** `items(list, key = { it.id })` + `contentType` fuer heterogene Listen.

### 9.2 State-Read zu frueh / Hot-Value als Wert statt Lambda-Provider
**Symptom:** ganzer Baum recomposed pro Frame bei Scroll/Animation.
**FIX:** Read in `Modifier.offset{ }`/`graphicsLayer{ }`/`drawBehind{ }` verlagern; haeufig wechselnde Werte als `() -> Float` weitergeben (nicht `Float`). State nicht zusaetzlich ausserhalb der Lambda lesen.

---

## 10. Build & Toolchain (K2 / KSP / KAPT / Compose-Plugin / R8 / Gradle)

### 10.1 KAPT laeuft unter K2 im 1.9-Modus / KSP-Version-Mismatch  ⭐
**Symptom:** Annotation-Processing bricht/faellt auf 1.9 zurueck; `ksp-... is too old for kotlin-...`.
**Ursache:** K2 unterstuetzt KAPT nicht nativ (1.9-Kompatmodus); KSP ist hart an die Kotlin-Version gekoppelt.
**FIX:** `kapt.use.k2=true` (Bruecke) ODER zu KSP migrieren; KSP-Plugin IMMER passend zur Kotlin-Version (z.B. `2.1.0-1.0.29`), im Version-Catalog parallel bumpen. ACHTUNG: KSP1 endet 2025, mit **Kotlin 2.3 / AGP 9.0 inkompatibel** → vorher migrieren.
**Quelle:** k2-compiler-migration-guide, github.com/google/ksp/ksp2.md.

### 10.2 Compose-Compiler ist seit Kotlin 2.0 ein Gradle-Plugin
**Symptom:** Build-/Plugin-Konflikt, wenn altes `composeOptions { kotlinCompilerExtensionVersion=... }` noch im Modul steht.
**FIX:** `composeOptions` ENTFERNEN; `org.jetbrains.kotlin.plugin.compose` (version = Kotlin-Version) im Root `apply false`, je Compose-Modul anwenden. Plugin-Reihenfolge: Compose nach Serialization anwenden (sonst `IncompatibleClassChangeError`).
**Quelle:** compose-compiler-migration-guide.

### 10.3 R8/ProGuard: Release-only Crashes  ⭐ (sicherheits-/stabilitaetsrelevant)
**Symptom:** `SerializationException: Serializer not found` / NPE / leere Objekte NUR im minifizierten Release.
**Ursache:** R8 strippt per Reflection/Serialization genutzte Member als "unbenutzt" — kotlinx.serialization
(bes. benannte Companions), Coroutines-volatile-Felder, Gson/Retrofit/Moshi-Modelle.
**Versionen:** per Design; kotlinx.serialization #2385 ist **CLOSED/NOT_PLANNED** → keep-Rules bleiben Pflicht.
**FIX (funktionserhaltend, NIE Klasse loeschen):** schmale keep-Rules — `-keep @kotlinx.serialization.Serializable class *`
(+ `-if @Serializable` fuer benannte Companions), `-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }`,
DTO/`@SerializedName`-Felder pro Modell. So eng wie moeglich, nie pauschal `-keep class **`.
**Quelle:** android-developers.googleblog.com R8 Keep Rules (2025), Kotlin/kotlinx.serialization #2385.

### 10.4 Inkonsistentes JVM-Target (compileJava vs compileKotlin)
**Symptom:** `Inconsistent JVM-target compatibility`, Build bricht; oder `Unknown Kotlin JVM target: NN` bei zu neuem JDK.
**FIX:** `kotlin { jvmToolchain(17) }` statt manueller source/targetCompatibility (setzt beide synchron, Poka-Yoke); bei zu neuem JDK Kotlin-Plugin heben oder jvmTarget begrenzen.

### 10.5 Build-Cache-Restore bricht Kotlin Incremental Compilation
**Symptom:** nach Cache-Hit nicht-inkrementell/veraltete Klassen; "Could not close incremental caches".
**Versionen:** KT-34862 (langjaehrig offen).
**FIX:** Remote-Build-Cache fuer Kotlin-Compile-Tasks deaktivieren (lokalen behalten); gleiche JDK-Major-Version auf allen Maschinen.

### 10.6 Hilt/Dagger mit KSP2
**Symptom:** `PSI has changed since creation` / `not annotated with @AssistedFactory` — nur mit KSP2.
**Versionen:** Dagger/Hilt-KSP2-Inkompat war **dagger #4303 = CLOSED/COMPLETED (2024-12)** → in aktuellen Versionen gefixt; ksp #2545 (Lifetime-Token) ist noch **OPEN**.
**FIX:** Hilt/Dagger auf aktuelle (KSP2-faehige) Version heben; alle Processor auf `ksp`-Konfiguration (nicht `kapt`); Dagger-Optionen als `ksp { arg(...) }`. Notfalls `ksp.useKSP2=false`.

### 10.7 `proguard-android.txt` faellt mit AGP 9.0 weg (Release-Minification)
**Symptom:** Release-Build mit Minification bricht oder aendert Verhalten nach einem AGP-9.0-Upgrade, wenn das Modul `proguard-android.txt` als Default-ProGuard-Basis referenziert.
**Ursache:** AGP 9.0 entfernt die alte Default-Basis `proguard-android.txt`; Basis ist nur noch `proguard-android-optimize.txt` (R8 fullMode, aggressivere Optimierung) — zu breite/fehlende keep-Rules fallen damit haerter auf.
**Versionen:** ab **AGP 9.0** (Projekte aktuell 8.7–8.10 → Upgrade-Pfad). Per Design.
**FIX (funktionserhaltend):** `getDefaultProguardFile("proguard-android-optimize.txt")` verwenden; keep-Rules eng halten und mit dem **R8 Configuration Analyzer** pruefen, welche Regel wie viele Klassen blockiert; Release-Variante mit Minification VOR dem AGP-Upgrade testen. Praevention: best-practices §10 (Build & Toolchain).
**Quelle:** developer.android.com shrink-code / keep-rules-overview / r8-configuration-analyzer (2025); ergaenzt aus dem best-practices-Lauf 2026-06-02 (Rueckkopplung).

---

## 11. Fix-Status — was auf dem aktuellen Stack schon behoben ist

> Versions-Denken: Diese frueheren Bugs sind in den genutzten Versionen (Kotlin 2.1+/aktuelle Libs)
> bereits GEFIXT — nicht mehr als aktiv behandeln. Changelog-/gh-belegt, Stand 2026-06-02 (ergaenzt 2026-06-24).

| Frueherer Bug | Status / gefixt | Beleg |
|---------------|-----------------|-------|
| Incremental Compilation: Inline-Lambda-Call-Sites nicht recompiliert | **gefixt ab Kotlin 2.2.20** | whatsnew2220 |
| Compose Endlos-Recomposition durch falsche Stability-Inferenz | **gefixt ab Compose-Compiler 2.0.10/2.0.20** | whatsnew2020 |
| Dagger/Hilt inkompatibel mit KSP2 | **gefixt — dagger #4303 CLOSED/COMPLETED 2024-12** | gh |
| Compose-Compiler-Version-Matching (manuelle Map noetig) | **entfaellt ab Kotlin 2.0** (Plugin synchron) | developer.android.com |
| Lib-interner Kotlin-Compiler ≠ Projekt-Kotlin → Incremental Compilation bricht (Paparazzi, Android) | **CLOSED/COMPLETED 2026-01-26** — Ursache Versions-Mismatch (Projekt 2.1.0, Lib brauchte 2.1.21) | gh cashapp/paparazzi#2061 |

### Noch NICHT gefixt (Workaround bleibt aktiv)
- K2 Breaking Changes (Abschnitt 1) — **per Design** (gewollt), kein "Fix".
- Coroutine-/Compose-Mechanik (Abschnitte 2,4,6–9) — **versionsunabhaengig** (Denkfehler, kein Bug).
- Kotlin 2.3.20 OOM durch nicht gestoppte KSP-Dispatcher-Threads — **ksp #2817 OPEN** → Workaround: Gradle-Daemon neu starten.
- Hilt KSP2 invalid lifetime token — **ksp #2545 OPEN**.
- kotlinx.serialization R8 keep-Rules — **#2385 CLOSED/NOT_PLANNED** (won't-fix) → keep-Rules dauerhaft.
- Kotlin 2.3 langsamere Compile-Zeiten vs 2.2 — **KT-81883 OPEN**.
- Build-Cache bricht Incremental Compilation — **KT-34862 OPEN**.

**Methodik-Hinweis:** GitHub-Issues (ksp/dagger/kotlinx.serialization) wurden per `gh issue view`
HART verifiziert (OPEN/CLOSED/stateReason). YouTrack-KT-Issues sind JS-gerendert und liessen sich per
WebFetch nur via Snippets lesen — deren Status ist daher mit Vorsicht zu behandeln. Die Researcher
selbst konnten `gh` NICHT ausfuehren (researcher-Agent ohne Bash) — die gh-Pruefung machte der Hauptagent.

---

## Pflicht-Checkliste vor Kotlin-Arbeit
- [ ] Diese Datei gelesen, Stand-Datum gegen die genutzte Kotlin/Compose/AGP-Version abgeglichen?
- [ ] K2-Breaking-Changes der Zielversion bedacht (Abschnitt 1)?
- [ ] Coroutines: CancellationException re-werfen, kein GlobalScope/eigenes Job, kein runBlocking auf Main?
- [ ] Flow in Compose: `collectAsStateWithLifecycle` + `WhileSubscribed(5000)`?
- [ ] Compose: stabile Parameter (`@Immutable`/`ImmutableList`), `rememberSaveable` fuer User-State, korrekte `LaunchedEffect`-Keys?
- [ ] Release-Build (R8): keep-Rules fuer Serialization/Reflection/Coroutines gesetzt + Release getestet?
- [ ] KSP-Version passt exakt zur Kotlin-Version; Compose-Compiler-Plugin statt `composeOptions`?
