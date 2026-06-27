# Hilt/Dagger DI + KSP Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
