# Hilt/Dagger DI + KSP — Best Practices (Stand 2026-06-14)

> **Zweck:** Wie man Dependency Injection mit Hilt/Dagger + KSP in BestJournalAndroid, NEMS und
> EntropieReductor von vornherein **richtig** baut — idiomatisch, offiziell belegt. Die Gegenseite
> (was schiefgeht) steht im Bug-Almanach.
> **Versions-Anker (live aus den Projekten):** Dagger-Hilt **2.55** (released 2026-01-09) · androidx.hilt
> (`hilt-navigation-compose`/`hilt-work`/`hilt-compiler`) **1.2.0** · KSP **2.1.0-1.0.29** · Kotlin **2.1.0** ·
> AGP 8.7.3/8.10.0 · R8 full mode. VoiceKey nutzt **kein** Hilt.
> **Verfügbares Upgrade (offiziell):** androidx.hilt **1.3.0** ist seit 2025-09-10 stabil (1.4.0-beta01 in
> Beta); bringt das neue Artifact `androidx.hilt:hilt-lifecycle-viewmodel-compose` (§4.6). Dagger-Hilt-Latest
> ist 2.59.x — die Patterns hier sind ab 2.55 unverändert; für vollen KSP2-Betrieb ≥ 2.57 (siehe Bug-Almanach V6).
> **Gegenstück (was schiefgeht):** [`bugs/android/hilt-dagger.md`](../../bugs/android/hilt-dagger.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Annotation-Processing | **KSP statt kapt** (kapt im Maintenance-Mode, KSP ~2× schneller); alle Processor auf `ksp(...)` | §1.1 |
| 2 | Dependencies | Lib `implementation`, Compiler `ksp("…:hilt-compiler")`; `kspTest`+`kspAndroidTest` nicht vergessen | §1.2 |
| 3 | Versionen | KSP-Präfix = Kotlin exakt (`2.1.0-1.0.29` ↔ 2.1.0), zentral im Version-Catalog | §1.4 |
| 4 | Gradle-Plugin | `com.google.dagger.hilt.android` IMMER anwenden (Bytecode-Transform → `class App : Application()`) | §1.5 |
| 5 | Plugin-Reihenfolge | Android → Kotlin → Compose-Compiler → KSP → Hilt | §1.6 |
| 6 | Inkrementelle Builds | `hilt { enableAggregatingTask = true }` (isolating processors) | §1.7 |
| 7 | `@InstallIn`-Component | **kleinste** passende Component wählen, nicht reflexartig `SingletonComponent` | §2.1 |
| 8 | Scope setzen? | **unscoped ist Default**; Scope nur bei Korrektheit; rein perf-bedingt `@Reusable` | §2.2 |
| 9 | Interface → Impl | `@Binds` (abstrakt, effizient); `@Provides` nur für Fremdtypen/Builder | §2.3 |
| 10 | Modul mit beidem | `interface`-Modul mit `@Binds` + `companion object` mit `@Provides` | §2.4 |
| 11 | Modul-Sichtbarkeit | Hilt-Module `internal`; ein Modul = ein Thema (Network/Database/Repository) | §2.5 |
| 12 | Gleicher Typ doppelt | eigene `@Qualifier`-Annotation (typsicher) statt `@Named`-String | §2.6 |
| 13 | Context | `@ApplicationContext`/`@ActivityContext`, nie nackter Context | §2.7 |
| 14 | Plugin-/Initializer-Liste | Multibindings `@IntoSet`/`@IntoMap`; aus Kotlin `@JvmSuppressWildcards` | §2.8 |
| 15 | Eigene Klassen | **Constructor-Injection** (`@Inject constructor`) als Default | §3.1 |
| 16 | Framework-Klassen | Field-Injection nur Activity/Fragment/View/Service/Receiver; nie `private` | §3.2 |
| 17 | DI-Wurzel | `@HiltAndroidApp` an Application + Manifest `android:name` | §3.4 |
| 18 | Entry-Points | Activity VOR Fragment `@AndroidEntryPoint` (Hilt an Hilt) | §3.5 |
| 19 | Nicht-Hilt-Code | `@EntryPoint` + `EntryPointAccessors` (Interface bei nutzender Klasse); kein Service-Locator | §3.6 |
| 20 | Laufzeit-Parameter + DI | Assisted Injection (`@AssistedInject`/`@AssistedFactory`) | §3.7 |
| 21 | ViewModel | `@HiltViewModel` + `@Inject constructor`; in Compose `hiltViewModel()` | §4.1 |
| 22 | Nav-Argumente | automatisch injiziertes `SavedStateHandle` (`toRoute()`), überlebt Prozesstod | §4.3 |
| 23 | VM über Screens teilen | `hiltViewModel(parentEntry)` + `remember(backStackEntry)` | §4.4 |
| 24 | Assisted vs SavedState | stabile/serialisierbare IDs → SavedStateHandle; echte Objekte → Assisted (nie kürzeren Lifecycle) | §4.5 |
| 25 | Multi-Module | `@HiltAndroidApp` nur in `:app`; `@Module` ins jeweilige `:core`/`:data` | §5.1, §5.2 |
| 26 | Modul-Kopplung | `implementation` + Aggregation; `api` nur für echte öffentliche API | §5.4 |
| 27 | Setup zentralisieren | Convention-Plugin in `build-logic` (Now in Android) | §5.5 |
| 28 | Dynamic Feature Module | `@EntryPoint` + Dagger `@Component(dependencies=…)` (Hilt aggregiert dort nicht) | §5.7 |
| 29 | `@HiltWorker` | `@AssistedInject` + `Configuration.Provider` (Property!) + Initializer entfernen | §6.1 |
| 30 | Tests | `HiltAndroidRule` `@get:Rule(order=0)` + `hiltRule.inject()` + Custom-Runner | §6.4 |
| 31 | Test-Doppel | `@TestInstallIn` source-set-weit (Default); `@BindValue @JvmField`+`@UninstallModules` pro Test; Fakes statt Mocks | §6.6 |

---

## 1) Build-Setup: KSP & Gradle

### 1.1 KSP statt kapt — der empfohlene Standard
`offiziell`
- kapt ist offiziell im **Maintenance-Mode**; Google empfiehlt Migration zu KSP für alle Processor, die es unterstützen. KSP analysiert Kotlin direkt (kein Java-Stub-Umweg) und ist **bis zu 2× schneller**.
- Migration: `kapt("…:hilt-compiler")` → `ksp("…:hilt-compiler")`; nach der Migration `kapt { }`-Blöcke (`correctErrorTypes`, `useBuildCache`) entfernen. Solange EIN kapt-Processor übrig bleibt, werden weiter Stubs erzeugt und der Speed-Gewinn bleibt aus. Ausnahme Data Binding (kein KSP) → in eigenes Modul isolieren.
- **Quelle:** https://developer.android.com/build/migrate-to-ksp · https://dagger.dev/dev-guide/ksp.html

### 1.2 Korrekte Dependency-Slots (inkl. Test)
`offiziell`
- Laufzeit-Lib über `implementation`, Compiler ausschließlich über `ksp`. Test-Slots nicht vergessen, sonst fehlen `@HiltAndroidTest`-Komponenten.
  ```kotlin
  implementation("com.google.dagger:hilt-android:2.55")
  ksp("com.google.dagger:hilt-compiler:2.55")
  androidTestImplementation("com.google.dagger:hilt-android-testing:2.55")
  kspAndroidTest("com.google.dagger:hilt-compiler:2.55")
  testImplementation("com.google.dagger:hilt-android-testing:2.55")
  kspTest("com.google.dagger:hilt-compiler:2.55")
  ```
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

### 1.3 `@HiltWorker`: zweiter androidx-Compiler ebenfalls über `ksp`
`offiziell`
- `@HiltWorker`-Code generiert `androidx.hilt:hilt-compiler` (zusätzlich zum Dagger-Compiler). In einem KSP-Modul gehört auch dieser in den `ksp`-Slot — KSP-Processor können von kapt generierte Typen nicht auflösen. (Die ältere developer.android.com-Seite zeigt teils noch `kapt` — die autoritative Aussage ist die Dagger-KSP-Doku.)
  ```kotlin
  implementation("androidx.hilt:hilt-work:1.2.0")
  ksp("androidx.hilt:hilt-compiler:1.2.0")
  ```
- **Quelle:** https://dagger.dev/dev-guide/ksp.html · https://developer.android.com/jetpack/androidx/releases/hilt

### 1.4 Version-Catalog: KSP exakt an Kotlin koppeln
`offiziell`
- KSP-Version = `<Kotlin-Version>-<KSP-Build>`. Kotlin 2.1.0 → KSP `2.1.0-1.0.29`. Zentral im `libs.versions.toml`, damit beide nie auseinanderlaufen; `hilt-android` und `hilt-compiler` immer versionsgleich.
- **Quelle:** https://kotlinlang.org/docs/ksp-quickstart.html · https://developer.android.com/build/migrate-to-ksp

### 1.5 Hilt-Gradle-Plugin immer anwenden
`offiziell`
- `com.google.dagger.hilt.android` ist nicht optional: Es transformiert den Bytecode, sodass `@HiltAndroidApp`/`@AndroidEntryPoint`-Klassen direkt die Basisklasse erweitern (`class App : Application()`) statt die generierte `Hilt_App` zu referenzieren → bessere IDE-Completion. Root mit `apply false`, App-Modul ohne Version.
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

### 1.6 Plugin-Reihenfolge
`offiziell` (Doku-Konvention)
- Android → Kotlin(Android) → Compose-Compiler (ab Kotlin 2.0 eigenes Plugin) → KSP → Hilt:
  ```kotlin
  plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
  }
  ```
- **Quelle:** https://dagger.dev/dev-guide/ksp.html · https://developer.android.com/build/migrate-to-ksp

### 1.7 `hilt { enableAggregatingTask = true }` + Argumente nur mit `+=`
`offiziell`
- Macht die Processor **isolating** (nur bei Bedarf aktiv) → kürzere inkrementelle Builds, Test-Component-Sharing, Classpath-Aggregation (erlaubt `implementation` statt `api`). Ersetzt das deprecated `enableExperimentalClasspathAggregation` „without any of its caveats".
- Zusätzliche Processor-Argumente immer additiv (`arguments += [...]`), nie `=` — sonst gehen Hilts eigene Argumente verloren (`Expected @HiltAndroidApp to have a value`).
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

---

## 2) Module-Struktur, Bindings & Scopes

### 2.1 `@InstallIn` Pflicht — kleinste passende Component wählen
`offiziell`
- Jedes Modul `@InstallIn(<Component>::class)`. Wähle die **kleinste** Component, in der das Binding gebraucht wird (app-weit → `SingletonComponent`; VM-gebunden → `ViewModelComponent`; nur Activity → `ActivityComponent` usw.). Child-Components sehen alle Bindings der Vorfahren, nicht umgekehrt.
- **DON'T:** alles reflexartig in `SingletonComponent`.
- **Quelle:** https://dagger.dev/hilt/components · https://dagger.dev/hilt/modules

### 2.2 Scopes sparsam — unscoped ist der Default
`offiziell`
- Standard = **unscoped** (jede Anfrage neue Instanz). Scope (`@Singleton`, `@ViewModelScoped`, …) nur, wenn geteilter Zustand für die Korrektheit nötig ist (Cache, OkHttp/Retrofit, offene DB). „Scoping a binding has a cost on both the generated code size and its runtime performance so use scoping sparingly."
- Rein performance-bedingtes Teilen ohne Lebenszeit-Bindung → **`@Reusable`** statt Component-Scope.
- Der Scope am Binding muss zur Component passen (`@InstallIn(ActivityComponent)` → nur `@ActivityScoped`).
- **Quelle:** https://dagger.dev/hilt/components

### 2.3 `@Binds` für Interface→Impl, `@Provides` für Fremdtypen
`offiziell`
- Eigene Impl eines Interface → `@Binds` (abstrakte Methode, kein Provider-Body, effizient; Impl braucht `@Inject constructor`). `@Provides` nur, wo Hilt nicht konstruktor-injizieren kann: Fremdtypen (Retrofit/OkHttp/Room), Builder-Logik.
- **DON'T:** `@Provides { return impl }`, wo `@Binds` reicht.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android · https://dagger.dev/hilt/modules

### 2.4 Gemischtes Modul: `interface` + `companion object`
`offiziell`
- Hat ein Thema `@Binds` UND `@Provides`, in EINEM Modul kombinieren: Modul als `interface` für die `@Binds`, `companion object` für die statischen `@Provides`.
  ```kotlin
  @Module @InstallIn(SingletonComponent::class)
  interface AnalyticsModule {
      @Binds fun bindService(impl: AnalyticsServiceImpl): AnalyticsService
      companion object {
          @Provides @Singleton
          fun provideConfig(@ApplicationContext ctx: Context) = AnalyticsConfig.from(ctx)
      }
  }
  ```
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android

### 2.5 Module thematisch + `internal` schneiden
`offiziell`
- Ein Modul = ein Thema (`NetworkModule`, `DatabaseModule`, `RepositoryModule`), kein `AppModule`-Sammelbecken. Hilt-Module so privat wie möglich (`internal object`/`internal interface`) — „prevents non-Hilt Dagger components from installing the modules". Hilt installiert allein über die transitive Abhängigkeit, nicht über Sichtbarkeit.
- In Tests ersetzbare Bindings in ein eigenes Modul-Target auslagern.
- **Quelle:** https://dagger.dev/hilt/modules · https://developer.android.com/training/dependency-injection/hilt-multi-module

### 2.6 Eigene `@Qualifier`-Annotation statt `@Named`-String
`offiziell`
- Bei mehreren Bindings desselben Typs (z.B. zwei `OkHttpClient`) je eine eigene `@Qualifier`-Annotation (`@Retention(BINARY)`) — typsicher, compile-time-geprüft, statt magischem `@Named("auth")`-String. Qualifier an Binding UND Injection-Punkt. „If you add a qualifier to a type, add qualifiers to all the possible ways to provide that dependency."
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android

### 2.7 Vordefinierte Default-Bindings nutzen
`offiziell`
- `@ApplicationContext Context` / `@ActivityContext Context`, `Application`/`Activity`/`Fragment`/`View`, `SavedStateHandle` (im `ViewModelComponent`) sind vordefiniert — nicht selbst providen. Nie nackten `Context` erwarten; nie Activity-Context in ein `@Singleton` injizieren (Leak).
- **Quelle:** https://dagger.dev/hilt/components · https://developer.android.com/training/dependency-injection/hilt-android

### 2.8 Multibindings für Plugin-/Initializer-Listen
`offiziell`
- Plugin-/Listen-Architektur: mehrere Module steuern je einen Eintrag bei (`@IntoSet` / `@IntoMap` + `@MapKey`); der Konsument injiziert `Set<T>`/`Map<K,V>`. Leere Mengen mit `@Multibinds` deklarieren; optionale Bindings via `@BindsOptionalOf` → `Optional<T>`. Auf Android große `Class`-Key-Maps mit `@LazyClassKey` (vermeidet eager class loading).
- **Kotlin-Idiom:** am Injektionspunkt `Set<@JvmSuppressWildcards Foo>` — sonst „cannot be provided" (Wildcard `? extends Foo`).
- **Quelle:** https://dagger.dev/dev-guide/multibindings

---

## 3) Injection-Arten & Entry-Points

### 3.1 Constructor-Injection als Default
`offiziell`
- Für jede Klasse, die DU erzeugst (Repositories, UseCases, Datasources, Adapter): `@Inject constructor`. Testbar (per manueller DI isolierbar), immutable (`val`), keine null-Phase.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android

### 3.2 Field-Injection nur für framework-erzeugte Klassen, nie `private`
`offiziell`
- `@Inject lateinit var` nur in Activity/Fragment/View/Service/BroadcastReceiver (Framework besitzt den Konstruktor). Felder dürfen **nicht** `private` sein (Compile-Fehler — generierter Injector liegt in anderem Package). Zugriff erst nach dem Lifecycle-Callback (Activity: `super.onCreate()`).
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android · https://dagger.dev/hilt/android-entry-point.html

### 3.3 Interface über `@Binds` an die `@Inject constructor`-Impl koppeln
`offiziell` (Now in Android)
- Repository/Datasource-Interface in ein Abstraktionsmodul, Impl mit `@Inject constructor`, Interface per `@Binds` daran koppeln. Durchgängige Konvention im offiziellen Now-in-Android-Sample.
- **Quelle:** https://github.com/android/nowinandroid · https://developer.android.com/training/dependency-injection/hilt-android

### 3.4 `@HiltAndroidApp` als Wurzel + Manifest
`offiziell`
- Genau eine `Application` mit `@HiltAndroidApp`, im `AndroidManifest.xml` als `android:name` registriert. Erzeugt den `SingletonComponent` (Eltern aller weiteren Components). Ohne diese Wurzel funktioniert kein Entry-Point.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android

### 3.5 `@AndroidEntryPoint` — Activity VOR Fragment
`offiziell`
- Hilt-Typen hängen nur an Hilt-Typen: Host-Activity zuerst `@AndroidEntryPoint`, dann Fragment. Unterstützt nur `ComponentActivity`-Erben (z.B. `AppCompatActivity`) und androidx-`Fragment`. In Compose reicht die Root-`ComponentActivity` zu annotieren.
- **Quelle:** https://dagger.dev/hilt/android-entry-point.html

### 3.6 `@EntryPoint` + `EntryPointAccessors` für Nicht-Hilt-Code — kein Service-Locator
`offiziell`
- Für Klassen, die Hilt nicht direkt unterstützt (ContentProvider, system-/library-instanziiert): `@EntryPoint @InstallIn(<Component>)`-Interface, Zugriff via `EntryPointAccessors.fromApplication/fromActivity(...)`. Interface bei der **nutzenden** Klasse definieren; Rückgabetypen `public`; Component+Accessor exakt paaren.
- Wo `@Inject` reicht, NIE per `EntryPointAccessors` ziehen (Service-Locator-Antipattern — versteckt Abhängigkeiten, untergräbt Compile-Zeit-Validierung). `@EarlyEntryPoint` nur als Test-Notausgang für `onCreate`-Frühzugriff.
- **Quelle:** https://dagger.dev/hilt/entry-points.html · https://dagger.dev/hilt/early-entry-point.html

### 3.7 Assisted Injection für Laufzeit-Parameter + DI gemischt
`offiziell`
- `@AssistedInject` + `@Assisted` + `@AssistedFactory`, wenn Objekt teils DI-Deps, teils Laufzeitwerte braucht. `@AssistedInject`-Typen können nicht direkt injiziert (nur die Factory) und nicht gescoped werden; Factory-Methode führt die `@Assisted`-Parameter in Konstruktor-Reihenfolge; gleiche Typen mit `@Assisted("name")` disambiguieren.
- **Quelle:** https://dagger.dev/dev-guide/assisted-injection

---

## 4) ViewModel & Jetpack Compose

### 4.1 `@HiltViewModel` + `@Inject constructor`, in Compose `hiltViewModel()`
`offiziell`
- ViewModel mit `@HiltViewModel` + `@Inject constructor`; in Compose immer `hiltViewModel()` (aus `hilt-navigation-compose`) statt `viewModel()`. Scoped automatisch an die aktuelle Destination (`NavBackStackEntry`). ViewModel NIE direkt von Dagger anfordern (Compile-Fehler, Mehrfach-Instanzen) — immer über `ViewModelProvider`/`hiltViewModel()`.
- **Quelle:** https://dagger.dev/hilt/view-model.html · https://developer.android.com/develop/ui/compose/libraries#hilt

### 4.2 Nur Domain-Deps ins ViewModel — framework-frei
`offiziell`
- Nur Repository/UseCase injizieren, keine Android-Framework-Typen (`Context`/`Activity`/`View`) — Leak-Gefahr, schlechte Testbarkeit. Nur Bindings aus `ViewModelComponent` + Eltern verfügbar. Geteilte Instanz über mehrere VMs → `@ActivityRetainedScoped`/`@Singleton`, nicht `@ViewModelScoped` (das ist pro VM verschieden).
- **Quelle:** https://dagger.dev/hilt/view-model.html

### 4.3 Navigations-Argumente über automatisch injiziertes `SavedStateHandle`
`offiziell`
- `SavedStateHandle` in den Konstruktor injizieren (Hilt liefert es im `ViewModelComponent` automatisch); mit type-safe Navigation `savedStateHandle.toRoute<Route>()`. Überlebt Prozesstod. Argumente NICHT per Composable-Parameter „durchreichen".
- **Quelle:** https://dagger.dev/hilt/view-model.html · https://developer.android.com/guide/navigation

### 4.4 ViewModel über Nav-Graph teilen
`offiziell`
- Default: ein VM pro Destination. Für geteilte Flows (Wizard/Formular) den Parent-`NavBackStackEntry` an `hiltViewModel(parentEntry)` geben, mit `remember(backStackEntry)` cachen:
  ```kotlin
  composable("child") { backStackEntry ->
      val parent = remember(backStackEntry) { navController.getBackStackEntry("Parent") }
      val shared = hiltViewModel<ParentViewModel>(parent)
  }
  ```
- **Quelle:** https://developer.android.com/develop/ui/compose/libraries#hiltviewmodel

### 4.5 Assisted vs. SavedStateHandle — richtige Wahl
`offiziell`
- Stabile/serialisierbare Werte (IDs, Routen-Argumente) → `SavedStateHandle` (persistiert, überlebt Prozesstod). Echte, nicht-serialisierbare Laufzeit-Objekte → Assisted Injection: `@HiltViewModel(assistedFactory=…)` + `@AssistedInject`, in Compose `hiltViewModel<VM, Factory>(creationCallback = { it.create(x) })`.
- **Warnung (offiziell):** nie Objekte mit kürzerem Lifecycle (Activity/Fragment/View) per Assisted reichen (Leak). Der Callback wird memoisiert — nach Config-Change NICHT erneut aufgerufen; für aktualisierbare/persistente Werte `SavedStateHandle`.
- **Quelle:** https://dagger.dev/hilt/view-model.html · https://developer.android.com/jetpack/androidx/releases/hilt#1.2.0

### 4.6 UI-State + neues Artifact (Upgrade-Hinweis)
`offiziell`
- UI-State als read-only `StateFlow<UiState>` exponieren (privates `MutableStateFlow` als Backing), in Compose mit `collectAsStateWithLifecycle()` konsumieren (lifecycle-bewusst). Das VM kommt aus Hilt, der State-Mechanismus ist davon entkoppelt.
- **Upgrade:** Ab androidx.hilt **1.3.0** wurden die `hiltViewModel()`-APIs in `androidx.hilt:hilt-lifecycle-viewmodel-compose` (Paket `androidx.hilt.lifecycle.viewmodel.compose`) ausgelagert — nutzbar OHNE transitive `androidx.navigation`-Abhängigkeit. Reines Compose → dieses Artifact; mit Navigation → weiter `hilt-navigation-compose:1.3.0`. (Projekt aktuell 1.2.0 → Upgrade auf 1.3.0 empfohlen.)
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/hilt#1.3.0 · https://developer.android.com/topic/libraries/architecture/viewmodel

---

## 5) Multi-Module-Architektur

### 5.1 `@HiltAndroidApp` nur im `:app`, das alle Hilt-Module transitiv sieht
`offiziell`
- Genau ein `@HiltAndroidApp` im `:app`. Hilt baut zur Compile-Zeit EINEN `SingletonComponent` über den transitiven Graphen — `:app` muss alle Hilt-Module + constructor-injizierten Klassen transitiv erreichen.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-multi-module

### 5.2 `@Module`-Provider dezentral ins `:core`/`:data`-Modul
`offiziell`
- `NetworkModule` → `:core:network`, `DatabaseModule` → `:core:database` usw. — nicht ins `:app`. „High Cohesion / Low Coupling": ein Provider gehört dorthin, wo das Objekt fachlich hingehört. Da `:app` alles transitiv sieht (§5.1), landen die Bindings trotzdem im globalen Component.
- **Quelle:** https://developer.android.com/topic/modularization/patterns

### 5.3 Dependency Inversion über Modulgrenzen
`offiziell`
- Interface ins Abstraktionsmodul (`:core:data`), Impl + `@Binds`-Modul ins Implementierungsmodul. Feature-Module hängen nur an der Abstraktion → austauschbar, testbar, weniger Rebuilds.
- **Quelle:** https://developer.android.com/topic/modularization/patterns

### 5.4 `implementation` + Aggregation; `api` nur für echte öffentliche API
`offiziell`
- Mit `enableAggregatingTask = true` (§1.7) sieht Hilt `@InstallIn`/`@EntryPoint` auch über `implementation`-Abhängigkeiten. `api` nur, wenn ein Typ wirklich öffentliche Modul-API ist. Pauschales `api` erhöht Coupling + Rebuild-Reichweite.
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

### 5.5 Hilt+KSP-Setup über Convention-Plugin zentralisieren
`offiziell` (Now in Android)
- Ein `HiltConventionPlugin` in `build-logic/convention` (included build, NICHT `buildSrc`): wendet KSP-Plugin an, `hilt-compiler` als `ksp`, `hilt-core` als `implementation`, und nur bei Android-Modulen zusätzlich `dagger.hilt.android.plugin` + `hilt-android`. Feature-Module brauchen dann nur `alias(libs.plugins.nowinandroid.hilt)`. Hält die KSP↔Kotlin-Kopplung projektweit konsistent.
- **Quelle:** https://github.com/android/nowinandroid/blob/main/build-logic/README.md

### 5.6 Empfohlener Modul-Graph
`offiziell`
- `:app` (Entry, `@HiltAndroidApp`) → `:feature:*` (isoliert, nicht voneinander abhängig) → `:core:*` (`data`/`network`/`database`/`datastore`/`common`/`model`/`ui`/`designsystem`, hier wohnen die Hilt-Module). Feature↔Feature-Querverbindungen und Zyklen vermeiden; Datenaustausch über geteiltes Daten-Modul (IDs statt Objekte).
- **Quelle:** https://developer.android.com/topic/modularization/patterns · https://github.com/android/nowinandroid

### 5.7 Dynamic Feature Modules: `@EntryPoint` + Dagger `@Component(dependencies=…)`
`offiziell`
- In Dynamic-Feature-Modulen ist die Abhängigkeitsrichtung invertiert → Hilt kann dort NICHT aggregieren. Brücke: `@EntryPoint @InstallIn(SingletonComponent::class)` im `:app` mit den benötigten Bindings, im Feature ein Dagger-`@Component(dependencies = […EntryPoint::class])`, zur Laufzeit per `EntryPointAccessors.fromApplication(...)` auflösen.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-multi-module · https://developer.android.com/training/dependency-injection/dagger-multi-module

---

## 6) `@HiltWorker` (WorkManager) & Test-Setup

### 6.1 `@HiltWorker` + `@AssistedInject`, Worker schlank halten
`offiziell` (+ Now in Android)
- `@HiltWorker` + `@AssistedInject`-Konstruktor mit `@Assisted Context` + `@Assisted WorkerParameters`; eigene Deps normal injiziert. Bevorzugt `CoroutineWorker`. Der Worker enthält keine Geschäftslogik — er delegiert an injizierte Repositories/UseCases (Now in Android `SyncWorker` ist reiner Orchestrator).
  ```kotlin
  @HiltWorker
  class SyncWorker @AssistedInject constructor(
      @Assisted ctx: Context, @Assisted params: WorkerParameters,
      private val repo: SyncRepository
  ) : CoroutineWorker(ctx, params)
  ```
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-jetpack · https://github.com/android/nowinandroid

### 6.2 Application als `Configuration.Provider` (Property!) + `HiltWorkerFactory`
`offiziell`
- `@HiltAndroidApp`-Application implementiert `Configuration.Provider`, injiziert `HiltWorkerFactory` und setzt sie über die **Property** `workManagerConfiguration` (ab WorkManager 2.9 Property statt Methode):
  ```kotlin
  @Inject lateinit var workerFactory: HiltWorkerFactory
  override val workManagerConfiguration: Configuration
      get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
  ```
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-jetpack

### 6.3 Default-`WorkManagerInitializer` aus dem Manifest entfernen
`offiziell`
- Sonst initialisiert `androidx.startup` WorkManager mit der Default-Konfiguration, bevor die eigene `Configuration.Provider` greift → `HiltWorkerFactory` wird nie genutzt:
  ```xml
  <provider android:name="androidx.startup.InitializationProvider"
      android:authorities="${applicationId}.androidx-startup" tools:node="merge">
      <meta-data android:name="androidx.work.WorkManagerInitializer"
          android:value="androidx.startup" tools:node="remove" />
  </provider>
  ```
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-jetpack

### 6.4 `@HiltAndroidTest` + `HiltAndroidRule(order=0)` + `inject()`
`offiziell`
- Test mit `@HiltAndroidTest`; `HiltAndroidRule` als `@get:Rule(order = 0)` VOR allen anderen Rules; `hiltRule.inject()` in `@Before` (Injection passiert NICHT automatisch).
  ```kotlin
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<HiltTestActivity>()
  @Before fun setup() { hiltRule.inject() }
  ```
- **Quelle:** https://dagger.dev/hilt/testing · https://developer.android.com/training/dependency-injection/hilt-testing

### 6.5 Custom-Runner mit `HiltTestApplication`
`offiziell`
- Instrumentation-Tests brauchen `HiltTestApplication` als Application — nur über einen Custom-Runner setzbar (`@Config` geht dort nicht). Test-Compiler `kspAndroidTest`/`kspTest` einbinden. Robolectric: `@Config(application = HiltTestApplication::class)` oder global `robolectric.properties`. `HiltTestApplication` dem `@CustomTestApplication` vorziehen.
  ```kotlin
  class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, ctx: Context?) =
      super.newApplication(cl, HiltTestApplication::class.java.name, ctx)
  }
  // build.gradle: testInstrumentationRunner = "<pkg>.CustomTestRunner"
  ```
- **Quelle:** https://dagger.dev/hilt/instrumentation-testing · https://dagger.dev/hilt/robolectric-testing

### 6.6 Test-Doppel: `@TestInstallIn` (Default) vs. `@BindValue`/`@UninstallModules` (pro Test)
`offiziell`
- Standard-Fakes source-set-weit über `@TestInstallIn(replaces = ProdModule::class)` (schneller Build, keine Duplikate). Pro-Test-Abweichung über `@UninstallModules` + `@BindValue @JvmField` (Wert im **Feld-Initializer**, nicht in `@Before` — sonst injiziert eine früher laufende Activity-Rule null). Fakes statt Mocks (Now in Android-Stil). Test-Module strikt im Test-Source-Set, nie mit `main` mischen. Fragment-Tests: `launchFragmentInHiltContainer` (Hilt unterstützt `FragmentScenario` nicht).
- **Quelle:** https://dagger.dev/hilt/testing

---

## 🔗 Bezug: Best-Practice-Abschnitt ↔ Bug-Abschnitt

> Wechselseitig mit [`bugs/android/hilt-dagger.md`](../../bugs/android/hilt-dagger.md) (dort die Spiegel-Tabelle).

| Best-Practice (hier) | Verwandter Bug-Abschnitt (Almanach) |
|----------------------|-------------------------------------|
| §1.1 KSP statt kapt | R6 kapt+KSP-Mischbetrieb, R8 „options not recognized" |
| §1.2/§1.3 Dependency-Slots | V4 fehlende generierte Klassen, W4 falscher Compiler |
| §1.4 KSP an Kotlin koppeln | V2 KSP-Suffix-Mismatch, V1 Metadata 2.1.0 |
| §1.5 Gradle-Plugin | V3 „Did you forget to apply the Gradle Plugin?" |
| §1.6 Plugin-Reihenfolge | V5 sporadische Plugin-Reihenfolge |
| §1.7 enableAggregatingTask / `+=` | V7, MT2, MT5; V3 (Argument-Überschreiben) |
| §2.1 `@InstallIn` Component | M2 fehlendes `@InstallIn`, M10 Mehrfach-Install |
| §2.2 Scopes sparsam | M5 Scope-Mismatch, M6 ViewModelComponent |
| §2.3/§2.4 `@Binds`/`@Provides` | M1 `@Binds` abstract, M9 `@Provides`-Effizienz, M3 Missing Binding |
| §2.6 Qualifier | M4 Duplicate Bindings |
| §2.7 Context-Qualifier | M7 Context cannot be provided |
| §2.8 Multibindings | M8 leeres Multibinding |
| §3.2 Field-Injection/`private` | A4 Field-Reihenfolge, A5 private Felder |
| §3.4 `@HiltAndroidApp` | A1 vergessen, A2 Manifest fehlt |
| §3.5 `@AndroidEntryPoint` | A3 Fragment ohne Host-Activity |
| §3.6 `@EntryPoint` | A7 ContentProvider, A8 falscher Accessor |
| §3.7/§4.5 Assisted Injection | VM5 Assisted-Versions-Falle |
| §4.1 `@HiltViewModel` | VM1 „Cannot create instance", VM6 direkt aus Dagger |
| §4.3 SavedStateHandle | VM4 leerer SavedStateHandle bei Parent-Scope |
| §4.4 Nav-Graph teilen | VM3 geteilter VM Scope, VM7 doppelte Instanzen |
| §5.x Multi-Module | MT1 Roots, MT2 Aggregation, MT3 Dynamic-Feature-Plugin, MT4 Feature-Module |
| §6.1–6.3 `@HiltWorker` | W1 Initializer, W2/W3 Configuration.Provider, W4 Compiler, W5 `@Assisted` |
| §6.4–6.6 Tests | MT6 Rule-Reihenfolge, MT7 Runner/inject, MT10 UninstallModules, MT9 BindValue, MT11 Fragment |
