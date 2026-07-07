# Kotlin — Best Practices

**Stand:** 2026-06-02 (Best-Practices-Recherchelauf, 5 Researcher, offizielle Quellen zuerst).
**Versions-Anker (live ermittelt):** Kotlin-CLI **2.3.20** (JRE 21), Projekt-Plugin **2.1.0**,
KSP **2.1.0-1.0.29**, AGP **8.7–8.10**, Compose BOM **2025.01 / 2026.03**, JDK **21**.
kotlinx.coroutines **1.11.0**, kotlinx.collections.immutable **0.4.x**.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/android/kotlin.md`](../../bugs/android/kotlin.md)): der
> Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von vornherein
> richtig macht*. Quellen-Rangordnung: offizielle JetBrains/Kotlin/Android-Quelle = Grundwahrheit
> (`offiziell`), Community/Blogs = `extern` (sekundaer, ueberstimmt nie das Offizielle). Jeder Eintrag
> traegt Quelle + `offiziell`/`extern`. Die Projekte laufen auf 2.1.0 — Punkte, die erst 2.2/2.3
> bringen, sind als „Upgrade-Pfad" markiert.

---

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

---

## 🔗 Bezug zum Bug-Almanach ([`bugs/android/kotlin.md`](../../bugs/android/kotlin.md))

Jeder Best-Practice-Abschnitt hier ist die **Praevention** zu einem Abschnitt im Bug-Almanach —
zwei Seiten derselben Medaille. Tritt einer der dortigen Bugs auf, liefert der Almanach die konkrete
funktionserhaltende Loesung; dieser Abschnitt sagt, wie man ihn von vornherein vermeidet. Direkt zur
passenden Loesung springen:

| Best-Practice (hier) | Verhindert Bug(s) in `bugs/android/kotlin.md` |
|----------------------|----------------------------------------|
| §1 Sprache & Idiome (K2) | §1 K2-Compiler & Sprach-Migration (1.1–1.9) |
| §2 Coroutines & Flow | §2 Coroutines (2.1–2.9) |
| §3 Null-Safety & Fehlerbehandlung | §3 Null-Safety (3.1–3.3) |
| §4 Collections & Immutability | §5 data class (5.1–5.3) |
| §5 Flow / StateFlow in Compose | §4 Flow / StateFlow / SharedFlow (4.1–4.4) |
| §6 Compose — Stabilitaet & Recomposition | §6 Recomposition & Stabilitaet (6.1–6.3) |
| §7 Compose — State | §7 State & remember (7.1–7.4) |
| §8 Compose — Side-Effects | §8 Side-Effects (8.1–8.3) |
| §9 Compose — Performance | §9 Performance (9.1–9.2) |
| §10 Build & Toolchain | §10 Build & Toolchain (10.1–10.6) |
| §11 Testing | — (kein Bug-Pendant, reine Praevention) |

---

## 1. Sprache & Idiome (K2)

- **Explizit typisieren statt auf Inferenz hoffen.** K2 (default ab 2.0, nicht abschaltbar) ist bei
  Inferenz/Smart-Cast/Nullability strenger. Oeffentliche API-Signaturen explizit typisieren. *(offiziell: kotlinlang k2-compiler-migration-guide)*
- **`when` ueber sealed/enum als Expression, ohne `else`.** Erzwingt Exhaustiveness; ein neuer Subtyp
  bricht den Build sichtbar. *(offiziell: kotlinlang)*
- **Scope-Funktionen gezielt waehlen:** `let` (nullable-Transform, Rueckgabe=Lambda), `apply`
  (konfigurieren, Rueckgabe=Objekt), `also` (Seiteneffekt/Logging), `run`/`with` (Block+Ergebnis).
  Nicht verschachteln/ueberbenutzen — falsche Wahl gibt subtile Rueckgabe-Bugs. *(offiziell: scope-functions)*
- **`require`/`check`/`error` fuer Vorbedingungen:** `require(arg>0)` → IllegalArgumentException,
  `check(state)` → IllegalStateException, `error("msg")` fuer unerreichbare Zweige. Lesbarer als `if/throw`,
  smart-cast-faehig. *(offiziell: exceptions)*
- **`@JvmInline value class UserId(val raw: Long)`** statt nackter Primitiver — verhindert Verwechslung
  gleichartiger Typen, meist ohne Allokations-Overhead. *(offiziell: inline-classes, stable seit 1.5)*
- **`buildList<T>{}`** mit explizitem Typ-Argument, wenn der Elementtyp nicht offensichtlich ist (K2-Inferenz, KT-58149). *(offiziell)*
- **Library-Module: `explicitApi()`** (explicit API mode) erzwingt Sichtbarkeits- und Rueckgabetyp-
  Angaben; **App-Module:** `progressiveMode = true` zieht neue Sprach-Verschaerfungen frueh rein. *(offiziell: kotlinlang)*
- **Moderne stdlib:** `lowercase()`/`uppercase()` (+ ggf. `Locale.getDefault()`), `appendLine()`,
  `androidTarget` — die alten Varianten sind ab 2.1.0 Error. *(offiziell: compatibility-guide-21)*
- **Aktuelle Gradle-DSL im Code:** `compilerOptions {}` statt `kotlinOptions {}`, `kotlin-parcelize`
  statt `kotlin-android-extensions`. *(offiziell: compatibility-guide-22)*

### Neue Sprach-Features 2.2/2.3 (Upgrade-Pfad — Projekte sind noch auf 2.1)

- **Stable, sofort nutzbar nach Upgrade** *(offiziell: kotlinlang whatsnew-2.2/2.3)*:
  - **Guard conditions in `when`-with-subject** (`is Foo if cond ->`) — spart verschachtelte `if`.
  - **Non-local `break`/`continue`** aus Inline-Lambdas.
  - **Multi-Dollar-String-Interpolation** (`$$"""…"""`) — literal `$` ohne Escaping (Regex, JSON, Templates).
  - **Datenfluss-basierte Exhaustiveness**, **nested type aliases**, **`return` in Expression-Bodies**.
- **Experimental — NICHT in Produktion erzwingen, nur bewusst mit Opt-in** *(offiziell)*:
  - **Explicit backing fields** (`field`-Keyword): erlaubt ein oeffentliches `StateFlow` mit privatem
    `MutableStateFlow`-Backing ohne zweite Property — groesster Nutzen fuer StateFlow/Compose. Noch experimentell.
  - **Context parameters** (`context(...)`) — loesen die alten context receivers ab; experimentell.
  - **Context-sensitive resolution**, **unused-return-value checker** — experimentell, abwarten.
- **Ausblick 2.4 (noch NICHT nutzbar): „Rich Errors"** — Union-Return-Typen (`fun f(): User | FetchError`)
  als native Alternative zu sealed/Result. Proposal-/Experimental-Phase (KotlinConf 2025). Heute nur
  das Design im Hinterkopf behalten, nicht einsetzen. *(offiziell: KotlinConf 2025)*

## 2. Coroutines & Flow

- **Dispatcher injizieren statt hartkodieren.** Den `CoroutineDispatcher` als Konstruktor-Parameter
  reinreichen (Default `Dispatchers.IO`/`Default`), nie im Body fest verdrahten — macht testbar. *(offiziell: developer.android.com coroutines-best-practices)*
- **Strukturierte Concurrency immer.** Scope an den Lebenszyklus binden (`viewModelScope`,
  `lifecycleScope`, `rememberCoroutineScope`), in suspend-Funktionen `coroutineScope {}`. Kein
  `GlobalScope`, kein freies `CoroutineScope(Job())`. *(offiziell: coroutines-basics)*
- **ViewModel erzeugt Coroutines und exponiert Ergebnis als `StateFlow`** — die UI sammelt nur, sie
  startet keine Arbeit. *(offiziell: Android)*
- **`CancellationException` ist heilig:** nie breit `catch (e: Exception)` ohne Re-Throw —
  `if (e is CancellationException) throw e`. *(offiziell: exception-handling)*
- **Lange Schleifen kooperativ:** `ensureActive()`/`yield()`/`isActive`, sonst ignoriert CPU-Arbeit ein `cancel()`. *(offiziell)*
- **`async` → try-catch um `await()`**; `CoroutineExceptionHandler` nur am Root-`launch`; unabhaengige
  Geschwister via `supervisorScope {}`. `viewModelScope.launch{}` braucht eigene Fehlerbehandlung (kein Default-Handler). *(offiziell + extern: Android-Community)*
- **Kein redundantes `withContext(Dispatchers.IO)`** um Retrofit/Room-suspend-Funktionen — die sind main-safe.
  Begrenzte Parallelitaet via `Dispatchers.IO.limitedParallelism(n)` (stabil). *(offiziell)*
- **Flow:** `collectAsStateWithLifecycle()` (nicht `collectAsState()`); ViewModel-StateFlow mit
  `stateIn(scope, WhileSubscribed(5000), initial)` (nicht `0`); one-shot-Events ueber `Channel`+`receiveAsFlow()`
  (nicht SharedFlow — verliert Emissionen); `flowOn` ueber die betroffenen Operatoren (vor `collect`);
  Flow EINMAL als ViewModel-Property halten. *(offiziell: kotlinlang flow / Android)*

## 3. Null-Safety & Fehlerbehandlung

- **`!!` vermeiden** — `?.`, `?:`, `?.let {}` oder echte Null-Behandlung. *(offiziell: null-safety)*
- **Java-Grenzen explizit machen.** Unannotierte Java-Rueckgaben sind Platform-Types (`String!`); an
  der Grenze sofort `val x: String? = javaCall()` statt durch den Code zu propagieren. Java-Seite mit
  JSpecify (`@NullMarked`/`@Nullable`) annotieren — **seit 2.1 sind JSpecify-Verstoesse Compile-Fehler**,
  nicht mehr nur Warnungen. *(offiziell: java-to-kotlin-nullability-guide)*
- **Nullable Boolean in Conditions:** `if (value == true)` statt Umwege. *(offiziell: coding-conventions)*
- **`lateinit`** nur fuer non-null, extern initialisierte Felder (DI/Tests); sonst `by lazy {}` (teuer, einmalig)
  oder nullable+Default (wenn „fehlt" ein gueltiger Zustand ist). Vor riskantem Zugriff `::prop.isInitialized`. *(offiziell: properties)*
- **Fehler modellieren statt werfen:** erwartbare Fehler (Validierung, Netzwerk-Resultat) als
  **sealed-Error-Hierarchie** + `when` ohne `else`; punktuelles Kapseln/Verketten mit
  `runCatching{}`/`Result<T>` (`map`/`recover`/`fold`). Echte Programmierfehler → Exception. *(extern: SoftwareMill/carrion.dev; Sprache selbst neutral)*
- **Antipattern: Exceptions als Control-Flow** — teuer + verschleiert die Signatur; Fehler gehoeren in
  den Rueckgabetyp. *(extern)*

## 4. Collections & Immutability

- **Read-only Interfaces by default:** Parameter/Felder als `List`/`Set`/`Map` deklarieren, nie
  `ArrayList`/`HashSet`; Factory `listOf()`/`mapOf()`. *(offiziell: coding-conventions)*
- **`buildList{}`/`buildMap{}`/`buildSet{}`** statt `mutableListOf()`+`.toList()` — Aufbauen-dann-einfrieren
  ohne Kopier-Overhead. *(offiziell: collections-overview, seit 1.6)*
- **`asSequence()` nur bei langen Operator-Ketten + grossen/lazy Daten oder fruehem Abbruch** (`first`/`take`).
  Bei kurzen Ketten/kleinen Listen sind eager Collections schneller. *(offiziell: sequences)*
- **`kotlinx.collections.immutable` (`persistentListOf()`/`ImmutableList`)** — echte Unveraenderlichkeit
  mit Structural Sharing. Lohnt v.a. fuer **Jetpack Compose** (stabile Typen → Skipping), MVI/Redux-State,
  Thread-Safety. Fuer normalen JVM-Datencode reicht das read-only Interface. *(offiziell: github Kotlin/kotlinx.collections.immutable, v0.4.x pre-1.0)*
- **`data class`-Fallen:** `equals`/`hashCode`/`copy` beruecksichtigen NUR Primaerkonstruktor-Properties;
  `copy()` ist Shallow Copy; identitaetsrelevante Felder `val` + `List` statt `Array`. *(offiziell: data-classes)*

## 5. Flow / StateFlow in Compose

- **`collectAsStateWithLifecycle()`** statt `collectAsState()` — stoppt die Collection bei STOPPED-Lifecycle
  (kein Akku-Drain, keine Hintergrund-Recomposition). *(offiziell: lifecycle-runtime-compose ≥ 2.6)*
- **`stateIn(scope, SharingStarted.WhileSubscribed(5000), initial)`** fuer ViewModel-StateFlows. *(offiziell)*
- **One-shot-Events via `Channel`+`receiveAsFlow()`**, geteilte Cold-Flows mit `shareIn`/`stateIn` zu Hot machen. *(offiziell)*

## 6. Compose — Stabilitaet & Recomposition

- **Strong Skipping Mode ist seit Kotlin 2.0.20 Default** (vorher opt-in): alle restartable Composables
  werden skippable, auch mit instabilen Parametern (Vergleich per `===`), Lambdas mit instabilen Captures
  werden automatisch memoisiert. Opt-out: `@NonSkippableComposable` / `@DontMemoize`. Ersetzt aber **nicht**
  saubere Stabilitaet. *(offiziell: developer.android.com strongskipping)*
- **Instabile Collections fixen:** `List<T>` ist instabil → `ImmutableList`/`persistentListOf()`. *(offiziell: stability/fix)*
- **`@Immutable`/`@Stable` nur mit echter Garantie** (Compiler-Vertrag; nur `val`, mutable Felder via `mutableStateOf()`). *(offiziell)*
- **Stability Configuration File** fuer Fremdklassen (`java.time.LocalDateTime` u.a.) via
  `composeCompiler { stabilityConfigurationFile = … }`, Wildcards moeglich. *(offiziell)*
- **Keine Backwards Writes** (State nach Read im selben Pass schreiben → Endlosschleife); teure Berechnungen
  in `remember(key)` oder ins ViewModel. *(offiziell)*
- **Compose Compiler Metrics/Reports aktivieren**, um instabile Params/nicht-skippable Composables zu finden. *(offiziell: stability/diagnose)*

## 7. Compose — State

- **State hoisten an den lowest common ancestor** (Unidirectional Data Flow) — Composable stateless,
  wiederverwendbar, testbar; State so nah wie moeglich am Verbraucher. *(offiziell: state-hoisting)*
- **`rememberSaveable`** fuer UI-State, der Recomposition + Activity-Recreation + Process-Death ueberlebt
  (`remember` nur fuer reinen Composition-Cache). Bei Tab-/Navigations-Wechsel State an ViewModel scoped an
  NavBackStackEntry hoisten. *(offiziell: state)*
- **`derivedStateOf`** nur bei haeufigem Input/seltenem Output (z.B. `firstVisibleItemIndex > 0`) — NICHT
  fuer simples Kombinieren zweier States. *(offiziell: performance/bestpractices)*
- **Kritische Daten echt persistieren** (DataStore/DB) — `rememberSaveable` ueberlebt kein App-Swipe-Kill. *(offiziell)*

## 8. Compose — Side-Effects

- **`LaunchedEffect`-Keys = alle im Block genutzten veraenderlichen Werte.** `LaunchedEffect(Unit)`/`(true)`
  ist so verdaechtig wie `while(true)`. *(offiziell: side-effects)*
- **`rememberUpdatedState`** fuer Werte, die den Effekt NICHT neustarten sollen (z.B. `onTimeout`). *(offiziell)*
- **`DisposableEffect` immer mit `onDispose { … }`** (Observer/Listener abmelden). Composition-Arbeit →
  `LaunchedEffect`; Event-getriebene Arbeit (onClick) → `rememberCoroutineScope().launch`. *(offiziell)*
- **`produceState`** adaptiert externe Async-Quellen (Flow/Repo) in `State<T>`; **`snapshotFlow`** wandelt
  Compose-State in einen Flow (fuer `map`/`distinctUntilChanged`/`filter`). *(offiziell: side-effects)*

## 9. Compose — Performance

- **Deferred Reads via Lambda-Modifier:** hochfrequente State-Reads (Animation/Scroll) aus der Composition
  heraushalten — `Modifier.offset { IntOffset(…) }` (Layout), `graphicsLayer { … }`/`drawBehind { … }` (Draw)
  statt Parameter-Modifier; haeufig wechselnde Werte als `() -> Float` weitergeben. *(offiziell: performance, deferred reads)*
- **Lazy-Listen mit stabilem `key = { it.id }`** (+ `contentType` bei heterogenen Listen). *(offiziell)*
- **Composables klein und schlank halten**, teure Arbeit aus dem Composition-Pfad; **Baseline Profiles**
  fuer Start-/Scroll-Performance. *(offiziell: performance)*

## 10. Build & Toolchain

- **`compilerOptions {}` typisiert** (`jvmTarget.set(JvmTarget.JVM_21)`, `languageVersion.set(...)`) —
  `kotlinOptions {}` ist deprecated und wird **in Kotlin 2.3.0 ein harter ERROR** (in 2.2 Warnung). *(offiziell: gradle-compiler-options, compatibility-guide-23)*
- **`kotlin { jvmToolchain(21) }`** synchronisiert Java- + Kotlin-Target + Toolchain-JDK in einem Schritt
  (reproduzierbar, unabhaengig vom lokalen JDK). *(offiziell: gradle-configure-project)*
- **KSP2 statt KAPT/KSP1.** KSP2 ist Default seit KSP 2.0.0 (Anfang 2025); **KSP1 deprecated ab Kotlin 2.2,
  inkompatibel ab Kotlin 2.3**; KSP-Version exakt zur Kotlin-Version (`<kotlin>-1.0.x`). KAPT laeuft seit
  2.2.20 per Default im K2-Modus. Nicht aufschieben — KAPT/KSP1-Support endet 2025. *(offiziell: google/ksp, developer.android.com migrate-to-ksp, whatsnew2220)*
- **Compose-Compiler als Gradle-Plugin** (`org.jetbrains.kotlin.plugin.compose`) statt `composeOptions`;
  Metrics/Reports via `composeCompiler { metricsDestination/reportsDestination/stabilityConfigurationFile }`.
  Plugin-Reihenfolge: Compose nach Serialization. *(offiziell: developer.android.com compose/compiler)*
- **R8 fullMode (Default) + schmale keep-Rules**; den **R8 Configuration Analyzer** nutzen, um zu sehen,
  welche Regel wie viele Klassen blockiert. Ziel: keep-Rules vermeiden (Codegen statt Reflection;
  kotlinx.serialization braucht minimale Rules). Release-Variante mit Minification IMMER testen.
  **`proguard-android.txt` faellt mit AGP 9.0 weg** → `proguard-android-optimize.txt` nutzen. *(offiziell: developer.android.com shrink-code, keep-rules, r8-configuration-analyzer)*
- **Configuration Cache + Build Cache + parallel** in `gradle.properties`
  (`org.gradle.configuration-cache=true`, `org.gradle.caching=true`, `org.gradle.parallel=true`).
  **Vorsicht:** Remote-Build-Cache + Kotlin Incremental koennen kollidieren → fuer CI eher Clean-Builds,
  lokalen Cache behalten. *(offiziell: gradle docs; vgl. KT-34862)*
- **Version Catalogs (`libs.versions.toml`) als Standard** + **Convention Plugins** (`build-logic`/`buildSrc`)
  fuer Multi-Modul statt copy-paste — eine Quelle fuer Versionen (haelt Kotlin/KSP/Compose im Gleichlauf). *(offiziell: developer.android.com migrate-to-catalogs, gradle best-practices)*
- **Hilt/Dagger** auf aktuelle, KSP2-faehige Version; alle Processor auf `ksp`-Konfiguration. *(offiziell)*

## 11. Testing (Coroutines/Flow)

- **`runTest { }`** + **`StandardTestDispatcher`/`TestDispatcher`** (injiziert, siehe §2) fuer deterministische
  Coroutine-Tests; virtuelle Zeit statt echtem `delay`. *(offiziell: kotlinx-coroutines-test)*
- **Turbine** fuer Flow-Assertions (`flow.test { assertEquals(…, awaitItem()) }`). *(extern: cashapp/turbine — De-facto-Standard)*

---

## Pflege

Neue belegte Best-Practice (offizielle Quelle bevorzugt) hier ergaenzen, Stand-Header + Versionen
aktualisieren. Bei Versionssprung der genutzten Kotlin/Compose/AGP-Version: kurzer Re-Check, ob
Empfehlungen noch gelten (besonders die „Upgrade-Pfad"-Punkte, sobald die Projekte von 2.1 hochziehen).
Gegenstueck-Bugs immer auch in [`bugs/android/kotlin.md`](../../bugs/android/kotlin.md) pflegen.
