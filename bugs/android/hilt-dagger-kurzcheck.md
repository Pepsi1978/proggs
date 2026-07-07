# Dependency Injection mit Hilt/Dagger + KSP (Android) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **V** Versionen/Build-Setup · **A** Annotationen/Komponenten · **M** Module/Bindings/Scopes ·
> **VM** ViewModel/Compose · **W** WorkManager(`@HiltWorker`) · **R** Release-R8/KSP-Migration · **MT** Multi-Module/Tests.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `Provided Metadata instance has version 2.1.0 … max 2.0.0` | Hilt ≥ **2.55** (Anker ist exakt die Fix-Version) | V1 |
| 2 | `ksp … is too old for kotlin …` / Processing läuft nicht | KSP-Präfix = Kotlin-Version exakt (2.1.0 ↔ `2.1.0-1.0.29`) | V2 |
| 3 | `Expected @AndroidEntryPoint/@HiltAndroidApp to have a value. Did you forget to apply the Gradle Plugin?` | Hilt-Gradle-Plugin anwenden; bei KSP2-Mismatch Hilt-Version prüfen | V3, V6 |
| 4 | Generierte `Hilt_*`/`Dagger…HiltComponents` fehlen | `ksp("com.google.dagger:hilt-compiler")` (nicht kapt, nicht falscher Slot) | V4 |
| 5 | `@AndroidEntryPoint`-Felder zur Laufzeit null, kein Compile-Fehler | `android:name=".MyApp"` im Manifest fehlt | A2 |
| 6 | `Hilt Fragments must be attached to an @AndroidEntryPoint Activity` | Host-Activity ZUERST `@AndroidEntryPoint` | A3 |
| 7 | `@Inject`-Feld NPE im Konstruktor/`init` | Zugriff erst NACH `super.onCreate()` | A4 |
| 8 | `Dagger does not support injection into private fields` | `@Inject lateinit var` ohne `private` | A5 |
| 9 | Injection in `BroadcastReceiver` null | `@AndroidEntryPoint` + `super.onReceive()` zuerst | A6 |
| 10 | Injection in `ContentProvider` geht nicht | `@EntryPoint` + `EntryPointAccessors.fromApplication` | A7 |
| 11 | `@Binds methods must be abstract` / „may not contain both…" | `@Binds` ins `interface`, `@Provides` ins `companion object` | M1 |
| 12 | `A @Module may need to be annotated with @InstallIn` | `@InstallIn(...Component::class)` ergänzen (Check NICHT abschalten) | M2 |
| 13 | `cannot be provided without an @Provides/@Inject` | `@Binds`/`@Provides`/`@Inject constructor` als Brücke | M3 |
| 14 | `… is bound multiple times` (z.B. 2× OkHttpClient) | `@Qualifier` an Binding UND Injection-Punkt | M4 |
| 15 | `may not reference bindings with different scopes` | Lebenszeit-Regel: langlebig darf nicht auf kurzlebig zeigen | M5 |
| 16 | ViewModel-Dependency in `ActivityRetainedComponent` geteilt | `@InstallIn(ViewModelComponent)` + `@ViewModelScoped` | M6 |
| 17 | `android.content.Context cannot be provided` | `@ApplicationContext`/`@ActivityContext` (nie nackter Context) | M7 |
| 18 | `Cannot create an instance of class …ViewModel` | `@HiltViewModel` + Host-Activity `@AndroidEntryPoint` + `hiltViewModel()` | VM1 |
| 19 | Geteilter VM pro Screen neu instanziiert | `hiltViewModel(parentEntry)` + `remember(backStackEntry)` | VM3 |
| 20 | `SavedStateHandle` leer bei Parent-scoped VM | Args am Kind-Entry lesen, Zustand über Parent teilen | VM4 |
| 21 | Assisted-VM in Compose unmöglich | `hilt-navigation-compose` ≥ 1.2.0 (`creationCallback`), Hilt ≥ 2.49 | VM5 |
| 22 | `Could not instantiate Worker` / `NoSuchMethodException` | Default-`WorkManagerInitializer` per `tools:node="remove"` raus + `Configuration.Provider` | W1 |
| 23 | `HiltWorkerFactory` greift nach WM-Upgrade nicht | WorkManager 2.9.0: `override val workManagerConfiguration` (Property, nicht Methode) | W3 |
| 24 | `@HiltWorker` baut nicht / Factory fehlt still | BEIDE Compiler: `ksp("…dagger:hilt-compiler")` + `ksp("androidx.hilt:hilt-compiler")` | W4 |
| 25 | Release crasht, Debug ok: `Cannot create …ViewModel` | Ab Hilt **2.56.1**: `-keepnames @…HiltViewModel class * extends …ViewModel` (2.55 noch nicht betroffen) | R1 |
| 26 | `Missing class Hilt_*` / `NoClassDefFoundError …_GeneratedInjector` | Plugin im Classpath + `hilt-android`/`hilt-compiler` versionsgleich + Hilt in JEDEM Modul | R3 |
| 27 | Release strippt reflektiv genutzte Typen (Retrofit/Moshi) | R8 full mode: `missing_rules.txt` sichten, `@Keep`/`-keep` | R4 |
| 28 | kapt+KSP gemischt → Typen nicht auflösbar | ALLE Hilt-relevanten Processor auf `ksp(...)` | R6 |
| 29 | `Cannot process test roots and app roots …` | `@HiltAndroidApp` nur in `main`, Test-Apps in `androidTest` | MT1 |
| 30 | Library-Binding „missing" nur im App-Modul | `hilt { enableAggregatingTask = true }` | MT2 |
| 31 | `Hilt plugin does not know how to configure 'androidComponents'` | Hilt-Plugin auf Dynamic-Feature-Modul → Hilt ≥ **2.57** (#4574) | MT3 |
| 32 | Hilt-UI-Test: Injection scheitert / Felder null | `HiltAndroidRule` `@get:Rule(order = 0)` + `hiltRule.inject()` + Custom-Runner | MT5, MT6 |
