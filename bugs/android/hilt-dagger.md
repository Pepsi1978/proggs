# Bekannte Bugs: Dependency Injection mit Hilt/Dagger + KSP (Android)

> PFLICHT-LESEN vor Arbeit an Hilt/Dagger-DI (Module, `@Inject`, `@AndroidEntryPoint`, `@HiltViewModel`,
> `@HiltWorker`) in BestJournalAndroid, NEMS, EntropieReductor.
> Stand: recherchiert am 2026-06-14 mit **7 Researchern parallel** (offizielle Quellen zuerst:
> dagger.dev, developer.android.com, github.com/google/dagger + google/ksp Issues) + dediziertem
> Fix-Status-Lauf (2 Behauptungen am google/dagger-Issue-Tracker verifiziert). ~55 Einträge in 7 Sektionen.
> **Versions-Anker (live aus den Projekten):** Dagger-Hilt **2.55** · androidx.hilt
> (`hilt-navigation-compose` / `hilt-work` / `hilt-compiler`) **1.2.0** · KSP **2.1.0-1.0.29** ·
> Kotlin **2.1.0** · AGP **8.7.3** (BestJournal/NEMS) bzw. **8.10.0** (EntropieReductor) · R8 full mode
> (AGP-8-Default). VoiceKey nutzt **kein** Hilt/KSP.
>
> **Abgrenzung (was steht woanders):** reine Kotlin-/Coroutinen-/K2-Themen → [`kotlin.md`](kotlin.md).
> `@Composable`/Recomposition/Navigation-UI → [`jetpack-compose.md`](jetpack-compose.md). Gradle-Plugin-/
> AGP-/Wrapper-/Catalog-Themen → [`../android-build/gradle.md`](../android-build/gradle.md). R8-Shrinker
> allgemein → [`../android-build/r8.md`](../android-build/r8.md). Hier geht es NUR um den DI-Layer (Hilt/Dagger/KSP).
> Zweite Seite (wie macht man es richtig): `best-practices/android/kotlin.md` (Hilt-Zeile).

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

---

## V) Versionen, Build-Setup & generierte Klassen

### V1. `Provided Metadata instance has version 2.1.0, while maximum supported version is 2.0.0` ⭐ HAEUFIG
- **Symptom:** Build bricht ab: `[Hilt] Provided Metadata instance has version 2.1.0, while maximum supported version is 2.0.0. To support newer versions, update the kotlinx-metadata-jvm library.` (`IllegalArgumentException`). Generierte Klassen fehlen. Tritt beim Sprung auf Kotlin 2.1.0 auf.
- **Ursache:** Ältere Hilt-Versionen (≤ 2.51) bündeln eine `kotlinx-metadata-jvm`, die nur Kotlin-Metadata bis 2.0.0 liest. Kotlin 2.1.0 schreibt Metadata-Version 2.1.0. Reiner Versions-Lag, kein Konfigurationsfehler.
- **Versionen:** betroffen Hilt ≤ 2.54.x mit Kotlin 2.1.0. **Belegt gefixt ab Dagger/Hilt 2.55** (Maintainer + mehrere Nutzer in #4582: „upgrade Dagger to 2.55, I have no problem using Kotlin 2.1.0"). **Der Projekt-Anker 2.55 ist exakt die Fix-Version — dieser Fehler ist bei euch erledigt.**
- **FIX:** Dagger/Hilt auf **2.55** (oder neuer) heben, `hilt-android` und `hilt-compiler` versionsgleich. Kein Kotlin-Downgrade nötig.
- **Quelle:** https://github.com/google/dagger/issues/4582

### V2. KSP-Versionssuffix passt nicht zur Kotlin-Version ⭐ HAEUFIG
- **Symptom:** `ksp-X.Y.Z is too old for kotlin-A.B.C`, `KSP reporting an incorrect version`, teils `NoSuchMethodError`. Annotation-Processing läuft gar nicht, Hilt-Klassen entstehen nicht.
- **Ursache:** KSP-Version ist zweiteilig `<Kotlin-Version>-<KSP-Release>` (z.B. `2.1.0-1.0.29`). Der vordere Teil MUSS exakt der Kotlin-Version entsprechen — KSP bindet den Kotlin-Compiler intern separat ein.
- **Versionen:** alle Kombinationen. **Projekt-Anker korrekt gepaart:** Kotlin 2.1.0 ↔ KSP `2.1.0-1.0.29`. Bei Kotlin-Bump (z.B. 2.1.10) KSP mitziehen (`2.1.10-1.0.31`).
- **FIX:** KSP-Suffix zentral im Version-Catalog (`libs.versions.toml`) an die Kotlin-Version koppeln, damit beide nie auseinanderlaufen.
- **Quelle:** https://github.com/google/ksp/issues/2330 · https://dagger.dev/dev-guide/ksp.html

### V3. `Expected @AndroidEntryPoint/@HiltAndroidApp to have a value. Did you forget to apply the Gradle Plugin?`
- **Symptom:** Compile-Fehler mit genau diesem Text, plus `[Hilt] Processing did not complete.` — obwohl Annotationen vorhanden sind.
- **Ursache:** (a) das Hilt-Gradle-Plugin `com.google.dagger.hilt.android` ist nicht angewendet (es macht per Bytecode-Transformation erst die Basisklassen-Verdrahtung); oder (b) eigene Processor-Argumente überschreiben mit `=` statt `+=` die von Hilt gesetzten Argumente. (Bei KSP2-Mismatch erscheint derselbe Text irreführend — siehe V6.)
- **Versionen:** versionsübergreifend (Setup). 
- **FIX:** Plugin zusätzlich zum KSP-Processor anwenden; eigene Argumente immer `arguments += [...]`:
  ```kotlin
  plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // bei Compose
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
  }
  ```
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

### V4. Generierte Hilt-Klassen fehlen — falsches Compiler-Artefakt / falscher KSP-Slot ⭐ HAEUFIG
- **Symptom:** `Hilt_<Activity>`, `Dagger…HiltComponents`, `*_GeneratedInjector`, `hilt_aggregated_deps` fehlen komplett; später `Unresolved reference: Hilt_…` / `Cannot find symbol`. KSP läuft scheinbar, generiert aber nichts.
- **Ursache:** Hilt-Processor liegt im falschen Konfigurationsslot (`annotationProcessor`/`kapt`) statt `ksp(...)`, oder das falsche Artefakt wird referenziert.
- **Versionen:** versionsübergreifend (Setup).
- **FIX:** Processor über `ksp(...)` mit `com.google.dagger:hilt-compiler` einbinden, Test-Slots nicht vergessen:
  ```kotlin
  implementation("com.google.dagger:hilt-android:2.55")
  ksp("com.google.dagger:hilt-compiler:2.55")
  kspTest("com.google.dagger:hilt-compiler:2.55")
  kspAndroidTest("com.google.dagger:hilt-compiler:2.55")
  ```
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

### V5. Plugin-Reihenfolge im `plugins {}`-Block sporadisch falsch
- **Symptom:** Mal grüner, mal roter Build; gelegentlich fehlende generierte Klassen oder `@AndroidEntryPoint`-Fehler trotz vollständiger Dependencies.
- **Ursache:** Gradle wertet Plugins in Deklarationsreihenfolge aus; KSP muss vor Hilt stehen, der Compose-Compiler bei den Kotlin-Plugins.
- **Versionen:** Kotlin 2.x + KSP + Hilt + Compose-Compiler-Plugin (Compose-Compiler ist ab Kotlin 2.0 ein eigenes Plugin).
- **FIX:** Reihenfolge wie in V3 (Android → Kotlin → Compose-Compiler → KSP → Hilt). Nichts entfernen, nur ordnen.
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

### V6. KSP2 (Default ab KSP 2.0.0) inkompatibel mit älterem Hilt
- **Symptom:** Mit KSP2 brechen `ksp…Kotlin`-Tasks ab: `[Hilt] Expected @AndroidEntryPoint to have a value …`, `[Hilt] Class java.lang.Object is not annotated with @AssistedFactory`, teils `NullPointerException … cannot be cast to … XType` — obwohl KSP1 ohne Codeänderung baut.
- **Ursache:** KSP2 ist eine K2-Neuimplementierung mit geänderten APIs; Daggers XProcessing-Schicht musste erst migriert werden. Ältere Hilt-Versionen sprechen die KSP2-API falsch an → Annotationen wirken „leer". Ab KSP 2.0.0 ist KSP2 standardmäßig aktiv, der Mismatch entsteht oft unbemerkt beim Upgrade.
- **Versionen:** betroffen Hilt ≲ 2.55 mit KSP2/Kotlin 2.0+; KSP2-Support reifte in den 2.56er-Releases, sauber ab **Hilt 2.57**. Hinweis: KSP1 unterstützt kein Kotlin 2.1+ und wird ab Kotlin 2.2.0 entfernt. **Mit dem Anker-Stack (Hilt 2.55 + KSP `2.1.0-1.0.29`) baut die Kette stabil; ein KSP2-Vollumstieg sollte mit Hilt ≥ 2.57 erfolgen.**
- **FIX:** Erste Wahl: Hilt auf KSP2-fähige Version (≥ 2.57) heben. Übergangsbrücke ohne Funktionsverlust: `ksp.useKSP2=false` in `gradle.properties`.
- **Quelle:** https://github.com/google/dagger/issues/4303 · https://github.com/google/ksp/issues/1841 · https://github.com/google/ksp/blob/main/docs/ksp2.md

### V7. `hilt { enableAggregatingTask = true }` fehlt → verlorene `@InstallIn`/`@EntryPoint` + langsame Builds
- **Symptom:** In Multi-Modul-Projekten fehlen `hilt_aggregated_deps`-Einträge; `@InstallIn`-Module/`@EntryPoint` aus `implementation`-Abhängigkeiten „verschwinden" (bei Multibindings erst zur Laufzeit). Jede Änderung rebuildet die Dagger-Komponente.
- **Ursache:** Ohne Aggregations-Task läuft der Processor non-isolating und sieht den transitiven `implementation`-Classpath nicht vollständig.
- **Versionen:** empfohlen ab Hilt 2.x; ersetzt das deprecated `enableExperimentalClasspathAggregation`. (Detail-Symptome in MT2/MT5.)
- **FIX:** `hilt { enableAggregatingTask = true }` im app-/Modul-`build.gradle`. Macht Processors isolating, aktiviert Classpath-Aggregation + Test-Component-Sharing — `implementation` muss NICHT auf `api` gelockert werden.
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html

---

## A) Annotationen & Android-Komponenten-Setup

> Fast alle Punkte hier sind **Setup-/Lifecycle-Fehler** (kein Library-Bug) — in Hilt 2.55 unverändert,
> nur durch korrektes Setup lösbar (nie durch Feature-Weglassen).

### A1. `@HiltAndroidApp` an der Application vergessen / nur in Library-Modul
- **Symptom:** `Expected @HiltAndroidApp to have a value. Did you forget to apply the Gradle Plugin?` bzw. `… is not a valid Component. Did you add or remove code in package hilt_aggregated_deps?`. Kein DI-Container.
- **Ursache:** Annotation fehlt, Plugin nicht angewendet, oder die `@HiltAndroidApp`-Klasse liegt in einem Library- statt im `android-application`-Modul (seit 2.42 strenger).
- **Versionen:** alle inkl. 2.55 (Setup). 
- **FIX:** `@HiltAndroidApp` an die Application im App-Modul + Plugin anwenden (V3). 
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html · https://github.com/google/dagger/issues/3400

### A2. Application nicht im Manifest als `android:name` registriert (STILLER Fehler) ⭐ HAEUFIG
- **Symptom:** Alles kompiliert, aber `@Inject`-Felder sind zur Laufzeit null bzw. `Hilt Activity must be attached to an @HiltAndroidApp Application`. KEIN Compile-Fehler.
- **Ursache:** `<application android:name=".MyApp">` fehlt → Android instanziiert die Default-`Application` statt der Hilt-Application; kein Component-Manager.
- **Versionen:** alle inkl. 2.55 (by design — Manifest bestimmt die Application-Klasse).
- **FIX:** Im Manifest `android:name=".MyApp"` exakt passend zu Klasse/Package setzen.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android · https://github.com/google/dagger/issues/3998

### A3. `@AndroidEntryPoint`-Fragment ohne annotierte Host-Activity ⭐ HAEUFIG
- **Symptom:** Runtime-Crash `Hilt Fragments must be attached to an @AndroidEntryPoint Activity` (erst zur Laufzeit, nicht beim Kompilieren).
- **Ursache:** Ein Hilt-Fragment holt seinen Component aus der Host-Activity; fehlt dort `@AndroidEntryPoint`, gibt es keinen `ActivityComponent`. (Gleiches in Tests mit `EmptyFragmentActivity`.)
- **Versionen:** alle inkl. 2.55. Compile-Time-Check ist Wunsch (#2842), bleibt Runtime-Fehler.
- **FIX:** Host-Activity zuerst `@AndroidEntryPoint`, dann das Fragment. In Tests eine eigene `@AndroidEntryPoint`-Test-Activity (siehe MT7).
- **Quelle:** https://dagger.dev/hilt/android-entry-point.html · https://github.com/google/dagger/issues/2842

### A4. Field-Injection-Reihenfolge — injizierte Felder vor `super.onCreate()` null
- **Symptom:** NPE / `lateinit property has not been initialized`, wenn ein injiziertes Feld im Konstruktor, `init {}` oder vor `super.onCreate()` benutzt wird.
- **Ursache:** Member-Injection passiert erst INNERHALB `super.onCreate()` (Service: `super.onCreate()`, Fragment: `super.onAttach()`). Davor existieren die Felder nicht.
- **Versionen:** alle inkl. 2.55 (by design — Lifecycle).
- **FIX:** Auf injizierte Felder erst NACH dem `super`-Aufruf zugreifen; nie im Konstruktor/`init`.
- **Quelle:** https://dagger.dev/hilt/android-entry-point.html

### A5. `private` bei `@Inject`-Feldern → Compile-Fehler
- **Symptom:** `Dagger does not support injection into private fields`.
- **Ursache:** Der generierte Code (oft in anderem Package) kann ein `private`-Feld nicht setzen.
- **Versionen:** alle inkl. 2.55 (Dagger-Grundregel).
- **FIX:** Sichtbarkeit weglassen: `@Inject lateinit var x: Foo` (nicht `private`). Bei Constructor-Injection irrelevant.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android

### A6. Injection in (system-/manifest-instanziierte) `BroadcastReceiver` schlägt fehl
- **Symptom:** `@Inject`-Felder im Receiver null / NPE in `onReceive()` trotz `@AndroidEntryPoint`.
- **Ursache:** Receiver haben KEINEN eigenen Component; sie werden aus dem `SingletonComponent` injiziert, und Hilt hängt sich nur über `super.onReceive(context, intent)` ein. Fehlt der `super`-Call (oder eine geeignete Basisklasse), bleibt die Injection aus.
- **Versionen:** alle inkl. 2.55 (architekturbedingt).
- **FIX:** Receiver `@AndroidEntryPoint`, Felder non-private, in `onReceive()` ZUERST `super.onReceive(context, intent)`. Muster: abstrakte `HiltBroadcastReceiver`-Basisklasse mit `@CallSuper`.
- **Quelle:** https://dagger.dev/hilt/android-entry-point.html (Fußnote 1) · https://github.com/google/dagger/issues/4903

### A7. Injection in `ContentProvider` direkt versucht (geht nicht)
- **Symptom:** `@AndroidEntryPoint` am ContentProvider unmöglich / Felder null.
- **Ursache:** ContentProvider-`onCreate()` läuft VOR `Application.onCreate()`, also bevor der Hilt-Graph existiert — offiziell nicht als Entry-Point unterstützt.
- **Versionen:** alle inkl. 2.55 (by design — Timing). Feature-Request #2650 offen.
- **FIX:** Über `@EntryPoint @InstallIn(SingletonComponent::class)` + `EntryPointAccessors.fromApplication(appContext, MeinEntryPoint::class.java)` auflösen (Application-Context, nicht `fromActivity`). Sehr früh nötige Deps: `@EarlyEntryPoint`.
- **Quelle:** https://dagger.dev/hilt/entry-points.html · https://github.com/google/dagger/issues/2650

### A8. Falsche Component / falscher Accessor beim `@EntryPoint`-Muster
- **Symptom:** Runtime-Crash bei `EntryPointAccessors.get(...)` (ClassCastException / „component does not implement entry point").
- **Ursache:** Der übergebene Component muss EXAKT der `@InstallIn`-Komponente des Interface entsprechen; oder ein zurückgegebener Typ ist nicht `public`.
- **Versionen:** alle inkl. 2.55 (Anwendungsfehler).
- **FIX:** Strikt paaren: `SingletonComponent`→`fromApplication`, `ActivityComponent`→`fromActivity`, `FragmentComponent`→`fromFragment`. Rückgabetypen `public`. Entry-Point bei der NUTZENDEN Klasse definieren.
- **Quelle:** https://dagger.dev/hilt/entry-points.html

### A9. Retained Hilt-Fragment (`setRetainInstance(true)`) → Runtime-Crash + Leak
- **Symptom:** Exception bei Konfigurationsänderung, wenn ein Hilt-Fragment `setRetainInstance(true)` hat.
- **Ursache:** Der Fragment-Component referenziert die alte Activity; bei Reattach an eine neue Activity-Instanz würde geleakt — Hilt verhindert das bewusst.
- **Versionen:** alle inkl. 2.55 (Schutzmechanismus).
- **FIX:** Zustand in einem `@HiltViewModel` halten (übersteht Config-Changes ohne Leak) statt `setRetainInstance`.
- **Quelle:** https://dagger.dev/hilt/android-entry-point.html (Retained Fragments)

---

## M) Module, Bindings & Scopes

> Überwiegend **by-design / Anwendungsfehler** — gelten für alle Versionen inkl. 2.55. Echte Versionssprünge
> (z.B. `@InstallIn`-Pflicht ab 2.30, `ViewModelComponent` ab 2.31) sind markiert.

### M1. `@Binds`-Methode nicht abstract → „A @Module may not contain both non-static and abstract binding methods" ⭐ HAEUFIG
- **Symptom:** Compile-Fehler `A @Module may not contain both non-static and abstract binding methods` bzw. `@Binds methods must be abstract`, sobald `@Binds` und `@Provides` im selben NICHT-abstrakten Modul stehen.
- **Ursache:** `@Binds` ist rein deklarativ (Dagger generiert die Impl) → muss `abstract` sein. `@Provides` liefert konkret → nicht-abstract/static. Beides im selben konkreten Modul ist verboten.
- **Versionen:** alle inkl. 2.55 (Designregel).
- **FIX:** `@Binds` ins `interface`-Modul, `@Provides` ins `companion object`:
  ```kotlin
  @Module @InstallIn(SingletonComponent::class)
  interface RepoModule {
      @Binds fun bindRepo(impl: RepoImpl): Repo            // abstract, kein Body
      companion object {
          @Provides fun provideClock(): Clock = Clock.systemUTC()
      }
  }
  ```
  Alternative: zwei getrennte Module (`interface` + `object`).
- **Quelle:** https://dagger.dev/hilt/modules.html · https://github.com/google/dagger/issues/1691

### M2. Fehlendes `@InstallIn` am `@Module` → Modul ignoriert / MissingBinding ⭐ HAEUFIG
- **Symptom:** `A @Module may need to be annotated with @InstallIn` ODER (subtiler) das Modul wird still ignoriert → `[Dagger/MissingBinding] … cannot be provided` für genau die Typen, die es liefern soll.
- **Ursache:** Hilt-Module brauchen `@InstallIn`, um zu wissen, in welche Komponente sie gehören. Seit **Hilt 2.30** ist das Pflicht (vorher still ignoriert).
- **Versionen:** Check aktiv seit 2.30, alle ≥ 2.30 inkl. 2.55.
- **FIX:** Passende Component angeben (NICHT den Check via `disableModulesHaveInstallInCheck` abschalten):
  ```kotlin
  @Module @InstallIn(SingletonComponent::class)
  object NetModule { @Provides fun provideJson(): Json = Json {} }
  ```
- **Quelle:** https://dagger.dev/hilt/modules.html · https://dagger.dev/hilt/flags.html

### M3. Missing Binding für Interface/Klasse → „cannot be provided without an @Provides/@Inject"
- **Symptom:** `[Dagger/MissingBinding] …MyRepository cannot be provided without an @Provides-annotated method` (Interface) bzw. `… without an @Inject constructor` (Klasse).
- **Ursache:** Hilt kennt keinen Konstruktionsweg. Interfaces/abstrakte Typen haben keinen `@Inject`-Konstruktor.
- **Versionen:** alle inkl. 2.55 (Anwendungsfehler).
- **FIX:** je nach Typ: (1) Interface→Impl via `@Binds` (Impl braucht `@Inject constructor`); (2) Fremd-Typ via `@Provides`; (3) eigene Klasse direkt `@Inject constructor()`.
- **Quelle:** https://dagger.dev/dev-guide/ · https://github.com/google/dagger/issues/2656

### M4. Duplicate Bindings → „… is bound multiple times" (z.B. zwei `OkHttpClient`) ⭐ HAEUFIG
- **Symptom:** `error: [Dagger/DuplicateBindings] okhttp3.OkHttpClient is bound multiple times`. Klassiker: zwei `OkHttpClient` (mit/ohne Auth-Interceptor), zwei `Retrofit`, zwei `String` (BaseURL + ApiKey).
- **Ursache:** Derselbe Typ wird mehrfach ohne Unterscheidung bereitgestellt (auch wenn eine Library ihn schon liefert).
- **Versionen:** alle inkl. 2.55 (by design). Test-Sonderfall: `@TestInstallIn`/`@UninstallModules` können False-Positive-Duplicates erzeugen (#2726, #3209).
- **FIX:** `@Qualifier` (bevorzugt) oder `@Named` an Binding UND Injection-Punkt:
  ```kotlin
  @Qualifier annotation class AuthClient
  @Provides @AuthClient fun authClient(...): OkHttpClient = ...
  class Api @Inject constructor(@AuthClient client: OkHttpClient)
  ```
- **Quelle:** https://github.com/google/dagger/issues/793

### M5. Scope-Mismatch → „may not reference bindings with different scopes" (`@Singleton` ↔ `@ActivityScoped`) ⭐ HAEUFIG
- **Symptom:** `[Dagger/IncompatiblyScopedBindings] SingletonComponent may not reference bindings with different scopes` bzw. `@ActivityScoped … is scoped to ActivityComponent but …`.
- **Ursache:** Lebenszeit-Verletzung: ein langlebiges Binding (`@Singleton`) darf nicht auf ein kurzlebiges (`@ActivityScoped`) zeigen, sonst hielte der Singleton eine zerstörte Activity (Leak). Die Scope-Annotation muss zum `@InstallIn`-Component passen.
- **Versionen:** alle inkl. 2.55 (DI-Sicherheitsregel).
- **FIX (drei saubere Wege):** (1) Dependency auf `@Singleton`/unscoped hochziehen; (2) Richtung umdrehen (Kurzlebiges injiziert Langlebiges — immer erlaubt); (3) Konsumenten in den passenden kurzlebigen Component (`@ActivityScoped`) verschieben.
- **Quelle:** https://dagger.dev/hilt/components.html · https://github.com/google/dagger/issues/1023

### M6. Falscher Component für ViewModel-Deps → `ActivityRetainedComponent` statt `ViewModelComponent`
- **Symptom:** `[Dagger/MissingBinding]` beim `@HiltViewModel`-Inject ODER eine Dependency wird ungewollt über ALLE ViewModels geteilt statt pro ViewModel.
- **Ursache:** ViewModels werden vom `ViewModelComponent` (`@ViewModelScoped`) injiziert. Wer Deps in `@InstallIn(ActivityRetainedComponent::class)` mit `@ActivityRetainedScoped` ablegt, bekommt eine geteilte Instanz (altes Verhalten vor `ViewModelComponent`).
- **Versionen:** `ViewModelComponent` seit **Hilt 2.31**, alle inkl. 2.55.
- **FIX:**
  ```kotlin
  @Module @InstallIn(ViewModelComponent::class)
  object VmModule {
      @Provides @ViewModelScoped fun provideRepo(): Repo = Repo()
  }
  ```
  `ViewModelComponent` erbt von `ActivityRetainedComponent`+`SingletonComponent` → Zugriff auf Eltern-Bindings bleibt.
- **Quelle:** https://dagger.dev/hilt/components.html · https://github.com/google/dagger/issues/3835

### M7. Falscher/fehlender Context-Qualifier → „android.content.Context cannot be provided"
- **Symptom:** `[Dagger/MissingBinding] android.content.Context cannot be provided …`, obwohl „Context da sein müsste".
- **Ursache:** Hilt bietet nur QUALIFIZIERTE Context-Bindings: `@ApplicationContext` (SingletonComponent) und `@ActivityContext` (ab ActivityComponent). Ein nackter `Context`-Parameter matcht keines.
- **Versionen:** alle inkl. 2.55 (by design).
- **FIX:** Qualifier passend zum Component: `@ActivityContext` in Activity/Fragment/View, `@ApplicationContext` im Singleton:
  ```kotlin
  @Provides fun provideRouter(@ActivityContext ctx: Context): Router = RouterImpl(ctx)
  ```
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-android · https://github.com/google/dagger/issues/2698

### M8. Leeres Multibinding → `Set<T>`/`Map<K,V>` ohne Beiträge „cannot be provided" (`@IntoSet`/`@IntoMap`)
- **Symptom:** `[Dagger/MissingBinding] java.util.Set<Initializer> cannot be provided …`, wenn unter einer Build-Variante KEIN `@IntoSet`/`@IntoMap`-Element beiträgt; oder erwartetes Element fehlt zur Laufzeit.
- **Ursache:** Ein Multibound `Set`/`Map` wird nur implizit deklariert, wenn ≥ 1 Beitrag existiert. Kann die Collection leer sein → keine Deklaration. Bei `@IntoMap` zusätzlich `@MapKey` pro Eintrag Pflicht.
- **Versionen:** alle inkl. 2.55 (by design).
- **FIX:** leere Variante explizit erlauben:
  ```kotlin
  @Module @InstallIn(SingletonComponent::class)
  interface InitModule { @Multibinds fun initializers(): Set<Initializer> }
  ```
  Bei `@IntoMap` eindeutige `@MapKey`/`@ClassKey` setzen.
- **Quelle:** https://dagger.dev/dev-guide/multibindings.html

### M9. `@Provides` statt `@Binds` für Interface — Effizienz-Falle (kein Crash)
- **Symptom:** Kein Fehler, aber unnötiger Factory-Code/Indirektion; Lint/Review meckert „could be @Binds".
- **Ursache:** `@Provides fun provideRepo(impl: RepoImpl): Repo = impl` erzeugt eine echte Provider-Factory, obwohl Dagger die Impl ohnehin bauen kann.
- **Versionen:** alle inkl. 2.55 (Best-Practice).
- **FIX:** durch `@Binds` ersetzen (Modul wird `interface`/`abstract`); Impl braucht `@Inject constructor`. Verhalten 1:1 gleich, weniger Code. (Dann nicht mit `@Provides` im selben konkreten Modul mischen — M1.)
- **Quelle:** https://dagger.dev/tutorial/04-depending-on-interface.html

### M10. `@InstallIn` in mehreren Components → unzulässiges Scoping / fehlende Default-Bindings
- **Symptom:** Compile-Fehler bei `@InstallIn({A::class, B::class})`: unzulässiges Scoping oder MissingBinding für Default-Typen (`Activity`, `Fragment`, `View`).
- **Ursache:** Scopen nur erlaubt, wenn ALLE Ziel-Components denselben Scope/Default kennen (`Fragment`+`Service` teilen keinen). Ein Modul nie gleichzeitig in Child und Ancestor installieren.
- **Versionen:** alle inkl. 2.55 (by design).
- **FIX:** Component-Menge so wählen, dass alle den Scope/Default teilen (z.B. `ViewComponent`+`ViewWithFragmentComponent`), oder nur in der gemeinsamen Ancestor-Komponente installieren; problematischen Scope weglassen (unscoped).
- **Quelle:** https://dagger.dev/hilt/modules.html

---

## VM) ViewModel & Jetpack-Compose-Navigation (`hiltViewModel`)

### VM1. `RuntimeException: Cannot create an instance of class …ViewModel` ⭐ HAEUFIG
- **Symptom:** Crash bei `hiltViewModel()`/`by viewModels()`: `Cannot create an instance of class …ViewModel`, oft `Caused by: NoSuchMethodException: <init> []`.
- **Ursache (fast immer Setup-Lücke):** `@HiltViewModel` vergessen; Host-Activity nicht `@AndroidEntryPoint`; `@HiltAndroidApp` fehlt; falscher Factory-Pfad (`viewModel()` statt `hiltViewModel()`); oder eine Konstruktor-Dependency ist nicht auflösbar.
- **Versionen:** alle inkl. 2.55 (Setup; Release-Variante durch R8 siehe R1).
- **FIX:** `@HiltAndroidApp` (+ Manifest), `@HiltViewModel` + `@Inject constructor`, Host-Activity `@AndroidEntryPoint`, in Compose `import androidx.hilt.navigation.compose.hiltViewModel` und `val vm = hiltViewModel<MyViewModel>()`.
- **Quelle:** https://dagger.dev/hilt/view-model.html · https://developer.android.com/develop/ui/compose/libraries#hilt

### VM2. `hiltViewModel()` in Activity ohne `@AndroidEntryPoint` → Crash
- **Symptom:** `setContent {}` in `ComponentActivity` ohne `@AndroidEntryPoint`; beim ersten `hiltViewModel()` Crash / Default-Factory greift statt Hilt-Factory.
- **Ursache:** `hiltViewModel()` zieht die Factory über `LocalViewModelStoreOwner` (Host-Activity/NavBackStackEntry); ohne `@AndroidEntryPoint` existiert kein Hilt-Factory-Pfad.
- **Versionen:** alle inkl. 2.55 (Setup-Pflicht).
- **FIX:** Host-Activity `@AndroidEntryPoint` annotieren.
- **Quelle:** https://developer.android.com/develop/ui/compose/libraries#hilt

### VM3. Geteilter ViewModel über Nav-Graph: falsches Scoping → doppelte Instanzen ⭐ HAEUFIG
- **Symptom:** Screens, die EINEN VM teilen sollen (Formular/Tab-Flow), bekommen je eine eigene Instanz; Daten gehen „verloren".
- **Ursache:** `hiltViewModel()` ohne Argument scopt an den AKTUELLEN NavBackStackEntry (pro Screen eigene Instanz — by design).
- **Versionen:** dokumentiertes Verhalten, stabil seit hilt-navigation-compose 1.0.0 (inkl. 1.2.0).
- **FIX:** Parent-Graph-Entry übergeben, mit `remember(backStackEntry)` cachen:
  ```kotlin
  composable("child") { backStackEntry ->
      val parent = remember(backStackEntry) { navController.getBackStackEntry("Parent") }
      val shared = hiltViewModel<ParentViewModel>(parent)
  }
  ```
  `remember(...)` ist Pflicht — sonst löst `getBackStackEntry` bei jeder Recomposition neu auf.
- **Quelle:** https://developer.android.com/develop/ui/compose/libraries#hilt

### VM4. `SavedStateHandle` leer/null bei Parent-Graph-scoped ViewModel
- **Symptom:** Navigation-Argumente (z.B. `movieId`) sind im an den Parent gescopten VM null; NPE/`IllegalArgumentException` beim Lesen.
- **Ursache:** Navigation schreibt destinationsspezifische Args in den `SavedStateHandle` des DESTINATIONS-Entry, nicht des Parent-Graph-Entry. Eine Navigation-Eigenheit, kein Hilt-Bug.
- **Versionen:** bestehende Limitierung (Navigation Compose / hilt-navigation-compose 1.2.0).
- **FIX:** Args am Kind-Destination (dessen `SavedStateHandle`/`backStackEntry.arguments`) lesen und per Methode in den geteilten VM setzen; nur wirklich graphweit Geteiltes an die Parent-Route hängen.
- **Quelle:** https://developer.android.com/develop/ui/compose/libraries#hilt

### VM5. Assisted Injection in ViewModel + Compose — Versions-Falle
- **Symptom:** (a) `@HiltViewModel(assistedFactory=…)` + `@AssistedInject` kompiliert nicht („use @Inject rather than @AssistedInject with a ViewModel"); (b) in Compose fehlt der Weg, den Assisted-Parameter zu übergeben (`creationCallback`-Überladung nicht gefunden).
- **Ursache:** ViewModel-Assisted-Injection erst ab **Dagger-Hilt 2.49**; die Compose-`hiltViewModel(creationCallback=…)`-Überladung erst ab **hilt-navigation-compose 1.2.0**.
- **Versionen:** Build-Fehler ≤ 2.48 bzw. ≤ 1.1.0. **Anker-Stack (2.55 + 1.2.0) ist voll kompatibel.**
- **FIX:**
  ```kotlin
  @HiltViewModel(assistedFactory = MovieVMFactory::class)
  class MovieViewModel @AssistedInject constructor(
      @Assisted val movieId: Int, private val repo: MovieRepository
  ) : ViewModel()
  @AssistedFactory interface MovieVMFactory { fun create(movieId: Int): MovieViewModel }
  // Compose:
  val vm = hiltViewModel<MovieViewModel, MovieVMFactory>(creationCallback = { it.create(movieId) })
  ```
  Achtung: Der Callback wird memoisiert — nach Config-Change NICHT erneut aufgerufen; für aktualisierbare Werte `SavedStateHandle`.
- **Quelle:** https://dagger.dev/hilt/view-model.html · https://developer.android.com/jetpack/androidx/releases/hilt

### VM6. ViewModel/Assisted-Factory direkt aus Dagger angefordert → Compile-Fehler
- **Symptom:** Build bricht ab: ViewModel mit `@Inject`/`@AssistedInject`-Konstruktor bzw. dessen `@AssistedFactory` darf nicht direkt aus Dagger angefordert werden (z.B. `@Inject lateinit var fooViewModel`).
- **Ursache:** ViewModels müssen über `ViewModelProvider`/`hiltViewModel()` bezogen werden, sonst entstünden mehrere nicht gespeicherte Instanzen.
- **Versionen:** Compile-Check in allen modernen Versionen inkl. 2.55.
- **FIX:** nie per Feld-/Konstruktor-Injektion anfordern; in Compose `hiltViewModel()`, in Activity/Fragment `by viewModels()`. Custom View: Werte durchreichen statt VM injizieren.
- **Quelle:** https://dagger.dev/hilt/view-model.html

### VM7. Mehrere VMs gleichen Typs pro Screen kollabieren zu einer Instanz
- **Symptom:** Zwei bewusst getrennte VMs desselben Typs (z.B. pro Tab) liefern dasselbe Objekt; Zustände vermischen sich.
- **Ursache:** `hiltViewModel()` memoisiert pro Typ und Owner (NavBackStackEntry) — by design.
- **Versionen:** dokumentiert, inkl. 1.2.0.
- **FIX:** eindeutigen `key`: `hiltViewModel<TabViewModel>(key = "tab_$index")`; oder eigener `ViewModelStoreOwner` via `rememberViewModelStoreOwner()` + `CompositionLocalProvider`.
- **Quelle:** https://developer.android.com/develop/ui/compose/libraries

---

## W) WorkManager-Injection (`@HiltWorker`)

> Relevant in EntropieReductor (nutzt `hilt-work`/`hilt-compiler` 1.2.0). Grenzt an
> [`android-platform.md`](android-platform.md) (WorkManager-Runtime) und
> [`workmanager-notifications.md`](workmanager-notifications.md) (Scheduling/Reschedule) — hier NUR der DI-Teil.

### W1. Default-`WorkManagerInitializer` nicht aus dem Manifest entfernt → „Could not instantiate Worker" ⭐ HAEUFIG
- **Symptom:** Laufzeit-Crash (nicht Compile): `E/WM-WorkerFactory: Could not instantiate <Worker>` + `java.lang.NoSuchMethodException: <Worker>.<init> [Context, WorkerParameters]`. Auch „WorkerFactory … returned null".
- **Ursache:** WorkManager initialisiert sich seit 2.6 automatisch über die `androidx.startup`-`InitializationProvider` (ein ContentProvider!) mit dem Default-`WorkerFactory`, der den `@AssistedInject`-Konstruktor des `@HiltWorker` nicht kennt. Die `HiltWorkerFactory` wird nie wirksam.
- **Versionen:** alle WorkManager ≥ 2.6 (Pflicht-Setup, kein Library-Bug). Vor 2.6 stattdessen `androidx.work.impl.WorkManagerInitializer` entfernen.
- **FIX:** Default-Initializer im Manifest entfernen (+ eigene Konfiguration via W2):
  ```xml
  <provider android:name="androidx.startup.InitializationProvider"
      android:authorities="${applicationId}.androidx-startup"
      android:exported="false" tools:node="merge">
      <meta-data android:name="androidx.work.WorkManagerInitializer"
          android:value="androidx.startup" tools:node="remove" />
  </provider>
  ```
  (`xmlns:tools` im `<manifest>` nicht vergessen.)
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-jetpack · https://github.com/google/dagger/issues/2690

### W2. `Configuration.Provider` nicht (korrekt) implementiert → „Unable to initialize WorkManager"
- **Symptom:** „Unable to initialize WorkManager" beim ersten `WorkManager.getInstance(context)`, oder die `HiltWorkerFactory` wird nie aufgerufen.
- **Ursache:** Nach Entfernen des Default-Initializers (W1) muss die App die Konfiguration selbst liefern. Fehlt `Configuration.Provider`/`setWorkerFactory(...)`, gibt es keine Konfiguration. Häufig auch deprecated `WorkManager.getInstance()` (ohne Context) vor der Init.
- **Versionen:** on-demand-Init ab WorkManager 2.1.0.
- **FIX:**
  ```kotlin
  @HiltAndroidApp
  class MyApp : Application(), Configuration.Provider {
      @Inject lateinit var workerFactory: HiltWorkerFactory
      override val workManagerConfiguration: Configuration
          get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
  }
  ```
  Immer `WorkManager.getInstance(context)`; KEIN zusätzliches `WorkManager.initialize()`.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration

### W3. WorkManager 2.9.0: `Configuration.Provider` als Property statt Methode → Konfiguration ignoriert ⭐ HAEUFIG
- **Symptom:** Trotz scheinbar korrektem Setup wird die `HiltWorkerFactory` nicht benutzt; Worker crasht weiter mit `NoSuchMethodException`. Compiler meckert nicht.
- **Ursache:** In **WorkManager 2.9.0** wurde `Configuration.Provider` von Java auf Kotlin umgeschrieben: aus der Methode `fun getWorkManagerConfiguration(): Configuration` wurde die Property `val workManagerConfiguration`. Wer die alte Methoden-Signatur weiter überschreibt, überschreibt NICHTS mehr (oft fälschlich als „KSP-Bug" gemeldet — war es nicht).
- **Versionen:** Breaking change in WorkManager **2.9.0**; betrifft 2.9–2.11. 2.8.0 nutzte noch die Methode.
- **FIX:** Property-Form verwenden (W2). KEIN Downgrade auf 2.8.0.
- **Quelle:** https://github.com/google/dagger/issues/4058

### W4. Falscher/fehlender KSP-Compiler — `com.google.dagger:hilt-compiler` ≠ `androidx.hilt:hilt-compiler` ⭐ HAEUFIG
- **Symptom:** Mit KSP crasht der Worker mit `NoSuchMethodException`/`Could not instantiate`; die generierte `<Worker>_AssistedFactory` fehlt STILL (kein Compile-Fehler). Nur ohne Dagger-Compiler: „The Hilt Android Gradle plugin is applied but no com.google.dagger:hilt-compiler dependency was found."
- **Ursache:** ZWEI gleichnamige, verschiedene Compiler. `@HiltWorker`-Code generiert **`androidx.hilt:hilt-compiler`**, normaler Hilt-Code **`com.google.dagger:hilt-compiler`**. Bei KSP müssen BEIDE als `ksp(...)` deklariert sein.
- **Versionen:** KSP-Support für `androidx.hilt` erst ab **1.1.0** (vorher nur kapt); mit **1.2.0** voll nutzbar. Anker 1.2.0 ist korrekt.
- **FIX:**
  ```kotlin
  implementation("com.google.dagger:hilt-android:2.55")
  ksp("com.google.dagger:hilt-compiler:2.55")           // Dagger-Hilt core
  implementation("androidx.hilt:hilt-work:1.2.0")
  ksp("androidx.hilt:hilt-compiler:1.2.0")               // generiert @HiltWorker-Factory
  ```
- **Quelle:** https://github.com/google/dagger/issues/4058 · https://developer.android.com/jetpack/androidx/releases/hilt

### W5. Vergessenes `@Assisted` am `@HiltWorker`-Konstruktor
- **Symptom:** Compile-Fehler („Missing binding for Context/WorkerParameters", „Dagger does not support injection … without @Assisted") oder Laufzeit-`NoSuchMethodException`.
- **Ursache:** `@HiltWorker` verlangt `@AssistedInject`-Konstruktor mit `@Assisted Context` UND `@Assisted WorkerParameters`. Typische Fehler: `@Inject` statt `@AssistedInject`; `@Assisted` an einem Pflicht-Parameter vergessen; `@Assisted` versehentlich an eigene Deps.
- **Versionen:** konzeptkonstant über alle androidx.hilt-Versionen (Anwendungsfehler).
- **FIX:**
  ```kotlin
  @HiltWorker
  class SyncWorker @AssistedInject constructor(
      @Assisted ctx: Context,
      @Assisted params: WorkerParameters,
      private val repo: MyRepository            // KEIN @Assisted
  ) : CoroutineWorker(ctx, params)
  ```
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-jetpack

### W6. Doppelte WorkManager-Initialisierung / Konflikt mit App-Startup
- **Symptom:** `IllegalStateException: WorkManager is already initialized` ODER „Unable to initialize WorkManager"; in Multi-Modul `lateinit property workerFactory has not been initialized` (Init vor abgeschlossener Hilt-Injection).
- **Ursache:** Mehrere kollidierende Init-Pfade: Default-Initializer noch aktiv UND eigene `Configuration.Provider`; zusätzliches manuelles `initialize()`; andere App-Startup-`Initializer` ziehen den WM-Init indirekt wieder ein; oder WM wird getriggert, bevor `@HiltAndroidApp` injiziert hat.
- **Versionen:** ab WorkManager 2.6; bei 2.9–2.11 unverändert.
- **FIX:** Genau EINEN Pfad: Default-Initializer raus (W1) + nur `Configuration.Provider` (W2), kein manuelles `initialize()`. Bei aktiv genutztem App-Startup nur den `WorkManagerInitializer`-`<meta-data>` per `tools:node="merge"` entfernen. Reihenfolge-Probleme: eigenen `Initializer<WorkManager>` mit deferred `EntryPointAccessors`-Injection.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration · https://github.com/google/dagger/issues/2601

---

## R) Release-Build (R8/ProGuard) & KSP-vs-kapt-Migration

> Hochrisiko: „Debug ok, Release crasht". Grenzt an [`../android-build/r8.md`](../android-build/r8.md)
> (R8 allgemein) — hier NUR Hilt-spezifische Keep-/Strip-Fälle.

### R1. `@HiltViewModel`-Konstruktor von R8 weg-minifiziert (Regression ab 2.56.1) ⭐ HAEUFIG
- **Symptom:** Debug läuft, **Release crasht** beim Erzeugen eines ViewModels: `NoSuchMethodException: <init> []` → `Cannot create an instance of class …ViewModel` aus `HiltViewModelFactory.create`. Tritt nach Update OHNE Codeänderung auf.
- **Ursache:** Dagger-Commit entfernte die Keep-Regel `-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel` aus den Consumer-Regeln. R8 (full mode, AGP 8.9.x) strippt den injizierten Konstruktor.
- **Versionen:** **betroffen ab Hilt 2.56.1** (Regel-Removal) mit AGP 8.9.x / R8 full mode, reproduziert bis 2.57.1; bei Suche **noch offen** (#4739). **Der Projekt-Anker 2.55 ist NOCH NICHT betroffen — die Keep-Regel ist dort vorhanden.** Beim Upgrade auf ≥ 2.56.1 die Regel manuell setzen.
- **FIX:** in `proguard-rules.pro`:
  ```
  -keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
  ```
- **Quelle:** https://github.com/google/dagger/issues/4739

### R2. `@EntryPoint`/`HiltWrapper_`-Klassen aus minifizierter Library entfernt → ClassCastException
- **Symptom:** Release crasht (Debug ok): `ClassCastException: Cannot cast …Application_HiltComponents_SingletonC to …` aus `EntryPointAccessors.fromApplication`. Die `HiltWrapper_*`-Impl fehlt.
- **Ursache:** Wird Hilt in einer Library/AAR minifiziert (statt erst in der finalen App), kennt R8 die späteren Consumer nicht und entfernt `hilt_aggregated_deps`-Metadaten + EntryPoint-Wrapper als „ungenutzt".
- **Versionen:** strukturelles R8-Verhalten, nicht versionsspezifisch.
- **FIX:** In der Library `-keep class hilt_aggregated_deps.** { *; }` + öffentliche EntryPoint-API keepen. Besser (offiziell): Library NICHT minifizieren, R8 erst in der finalen App laufen lassen.
- **Quelle:** https://github.com/google/dagger/issues/3386

### R3. `Missing class Hilt_*` / `NoClassDefFoundError …_GeneratedInjector`
- **Symptom:** R8 bricht ab: `Missing class …Hilt_MyApplication … apply keep rules in missing_rules.txt`. Laufzeit: `NoClassDefFoundError: …MyApplication_GeneratedInjector`. Z.B. nach Upgrade oder Umstieg auf R8 full mode.
- **Ursache:** Meist Folge-Fehler einer echten Fehlkonfiguration: Hilt-Gradle-Plugin nicht im Classpath, `hilt-android`/`hilt-compiler` versionsverschieden, oder ein Modul nutzt EntryPoints ohne Hilt-Dependency.
- **Versionen:** u.a. 2.51 + AGP 8.3 (R8 full mode); via Konfig-Korrektur lösbar.
- **FIX:** (1) Plugin sicher im Classpath; (2) `hilt-android`/`hilt-compiler` exakt gleiche Version; (3) Hilt-Dependency in JEDEM Modul mit Hilt-Annotationen/EntryPoints; (4) Default-ProGuard-Files aus Library-Modulen entfernen; (5) aktuelle Hilt-Version. ERST danach gezielte `missing_rules.txt`-Regeln.
- **Quelle:** https://github.com/google/dagger/issues/4259

### R4. R8 full mode (AGP-8-Default) strippt reflektiv genutzte Typen ⭐ HAEUFIG
- **Symptom:** Nach AGP ≥ 8.0 crasht der Release-Build (`minifyEnabled true`) beim Start; Debug ok. Klassen/Member/Konstruktoren, die im compat mode überlebten, sind entfernt.
- **Ursache:** Ab AGP 8.0 läuft R8 standardmäßig im full mode (aggressiver). Reflektiv/per Annotation genutzte Typen (Hilt-Generated, Retrofit-Interfaces, Gson/Moshi-Modelle, via `@Provides` zurückgegebene Typen) gelten als ungenutzt; `-keepnames`-Konstruktoren werden nicht mehr automatisch gehalten.
- **Versionen:** ab AGP 8.0, weiterhin in 8.7.3 / 8.10.0.
- **FIX:** `missing_rules.txt` (unter `app/build/outputs/mapping/<variant>/`) sichten und `-keep`/`-dontwarn` übernehmen; reflektive Typen mit `@Keep`/`-keep` (Retrofit-Service-Interfaces, Datenmodelle). Notbrücke temporär: `android.enableR8.fullMode=false`. Volle R8-Konfig zur Diagnose: `app/build/intermediates/proguard-files/full-r8-config.txt`.
- **Quelle:** https://developer.android.com/build/shrink-code · https://github.com/google/dagger/issues/4259

### R5. `@LazyClassKey`-referenzierte Klassen von R8 full mode gemerged → kaputte Multibinding-Map
- **Symptom:** Mit Hilt 2.51 liefert die generierte Multibinding-Map im Release falsche/fehlende Einträge (R8 merged die als Map-Keys dienenden Klassen).
- **Ursache:** `@LazyClassKey` nutzt Klassennamen als Map-Key; R8 full mode darf Klassen mergen → Schlüssel-Integrität verloren, wenn keine schützende Regel mitkommt.
- **Versionen:** betroffen Hilt 2.51 unter R8 full mode. **Gefixt ab 2.52** (Regeln gegen Merge mitgeliefert) — in **2.55 enthalten**.
- **FIX:** Hilt ≥ 2.52 (Anker 2.55 erfüllt das). LazyClassKey-Funktion bleibt erhalten.
- **Quelle:** https://github.com/google/dagger/issues/3197 · https://github.com/google/dagger/releases

### R6. kapt+KSP-Mischbetrieb: KSP kann kapt-generierte Typen nicht auflösen
- **Symptom:** Nach Teil-Migration: `… was unable to process 'Foo' because 'SomeClassFactory' could not be resolved.` Generierte Klassen sind für den Hilt-KSP-Processor unsichtbar.
- **Ursache:** KSP-Processoren können Typen, die von kapt/javac-Processoren generiert werden, NICHT auflösen. Bleibt so ein Processor in `kapt`, scheitert die Auflösung.
- **Versionen:** jede gemischte kapt+KSP-Konfiguration.
- **FIX:** ALLE Hilt-relevanten Processor auf `ksp(...)` umstellen (inkl. `androidx.hilt:hilt-compiler`, KSP-Support ab 1.1.x). `@AutoFactory` ggf. durch `@AssistedInject`/`@AssistedFactory` ersetzen.
- **Quelle:** https://dagger.dev/dev-guide/ksp.html

### R7. `BuildConfig`/DataBinding in `@Provides` nach KSP-Umstieg nicht auflösbar
- **Symptom:** Nur mit KSP: `e: [ksp] ModuleProcessingStep was unable to process '…Module' because 'error.NonExistentClass' could not be resolved.` z.B. bei `@Provides fun … = BuildConfig.FLAG`.
- **Ursache:** Task-Ordering-Bug zwischen KSP-Plugin und AGP (Google-Ticket 301245705): `generate<Variant>BuildConfig`-Quellen liegen beim KSP-Lauf noch nicht auflösbar vor. Fehlendes `correctErrorTypes`-Äquivalent verschärft das.
- **Versionen:** frühe KSP+AGP-Kombinationen; via AGP/KSP-Updates + Workaround.
- **FIX:** BuildConfig-Quellen per `androidComponents { onVariants … }` explizit als Source der `ksp<Variant>Kotlin`-Task setzen (Ticket 301245705); oder aktuelle KSP/AGP.
- **Quelle:** https://issuetracker.google.com/301245705

### R8. „The following options were not recognized by any processor" (kosmetisch, irreführend)
- **Symptom:** KSP-Warnung: `The following options were not recognized by any processor: '[dagger.fastInit, dagger.hilt.android.internal.*, dagger.hilt.internal.useAggregatingRootProcessor]'`.
- **Ursache:** Hilt-interne Compiler-Optionen (vom Gradle-Plugin gesetzt) landen mit KSP teils an einer javac-Stufe, die sie nicht kennt. Funktional harmlos, leicht mit echten Fehlern verwechselbar.
- **Versionen:** u.a. 2.51.1 + KSP 2.0.0; mit aktueller Toolchain (2.55 / KSP 2.1.0) i.d.R. weg.
- **FIX:** Plugin + KSP korrekt anwenden, `hilt-android`/`hilt-compiler` versionsgleich über `ksp(...)`. Warnung ist nicht crash-relevant; NICHT durch Streichen von Hilt-Optionen „lösen".
- **Quelle:** https://github.com/google/ksp/issues/2002

### R9. Inkrementeller KSP-Build nach Löschen eines `@HiltViewModel`/`@AutoBind` → veraltete generierte Klasse
- **Symptom:** Nach Löschen eines `@HiltViewModel` schlägt der inkrementelle Build fehl: `package …ViewModel_HiltModules does not exist`. Voller Clean-Build geht; inkrementell nicht (mit kapt tritt es nicht auf).
- **Ursache:** KSP-Tracking verwirft beim Entfernen einer annotierten Quelle die abhängigen, zuvor generierten Hilt-Klassen nicht korrekt als „orphaned".
- **Versionen:** u.a. 2.48-Reihe; Verbesserungen ab 2.54.1; Anker **2.55 enthält die Korrektur** (KSP #1554).
- **FIX:** sofort `./gradlew clean`; dauerhaft Hilt ≥ 2.54.1 (Anker 2.55 erfüllt das). Kein Code/Feature entfernen.
- **Quelle:** https://github.com/google/dagger/issues/4060 · https://github.com/google/ksp/issues/1554

---

## MT) Multi-Module-Hilt & Hilt-Tests

### MT1. „Cannot process test roots and app roots in the same compilation unit"
- **Symptom:** Kompilierung bricht mit genau dieser Meldung ab.
- **Ursache:** `@HiltAndroidApp`-Root (App) und Test-Roots (`@CustomTestApplication`/`HiltTestApplication`) landen im selben Compilation-Unit (z.B. geteilte Base-Application).
- **Versionen:** alle inkl. 2.55 (Design-Invariante).
- **FIX:** `@HiltAndroidApp` NUR in `main`; Test-Applications strikt in `test`/`androidTest`; keine gemeinsame Base-Application, die beide Roots aggregiert.
- **Quelle:** https://dagger.dev/hilt/testing.html

### MT2. `@InstallIn`-Modul/`@EntryPoint` im Library-Modul wird nicht gefunden (`implementation`-Classpath) ⭐ HAEUFIG
- **Symptom:** Eine Library-Binding ist im App-Modul „missing" oder verschwindet still (bei Multibindings erst zur Laufzeit); ins App-Modul verschoben funktioniert sie plötzlich.
- **Ursache:** Bei `implementation` (statt `api`) kann der Compile-Classpath des App-Moduls die `@InstallIn`/`@EntryPoint`-Info verlieren; Hilt sieht sie beim Aggregieren nicht.
- **Versionen:** inkl. 2.55.
- **FIX:** `hilt { enableAggregatingTask = true }` (siehe V7) — aktiviert Classpath-Aggregation, `implementation` muss NICHT auf `api`. Alternative: Library als `api`. (Bei AGP < 7.0 zusätzlich `android.lintOptions.checkReleaseBuilds = false`.)
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html · https://github.com/google/dagger/issues/1991

### MT3. `Hilt plugin does not know how to configure 'androidComponents'` (Hilt-Plugin auf Dynamic-Feature-Modul)
- **Symptom:** Beim Anwenden von `dagger.hilt.android.plugin` auf ein Modul: `Hilt plugin does not know how to configure 'dagger.hilt.android.plugin.HiltGradlePlugin@…'` bzw. `… how to configure 'extension 'androidComponents''`.
- **Ursache:** Das Hilt-Gradle-Plugin konnte in 2.55/2.56 nicht auf ein **Dynamic-Feature-Modul** (bzw. dessen `androidComponents`-Extension) angewendet werden (verifiziert am Issue-Tracker: #4907 ist Duplikat von #4574).
- **Versionen:** betroffen 2.55 / 2.56 bei Dynamic-Feature-Modul. **Belegt gefixt ab Hilt 2.57** (Maintainer bcorso in #4907: „It should be fixed in 2.57"). Single-Module-Apps (BestJournal/NEMS) sind NICHT betroffen.
- **FIX:** Hilt auf ≥ 2.57 heben, wenn das Hilt-Plugin auf ein Feature-Modul soll. Alternativ: Feature-Modul nicht direkt das Hilt-Plugin geben, sondern Component-Dependencies (MT4).
- **Quelle:** https://github.com/google/dagger/issues/4907 · https://github.com/google/dagger/issues/4574

### MT4. Feature-Module: „missing binding" / Hilt verarbeitet Annotationen nicht (invertierte Abhängigkeit)
- **Symptom:** In Dynamic-Feature-Modulen Compile-Fehler oder „missing binding" für Feature-Bindings — nur im App-Modul sichtbar.
- **Ursache:** Bei Feature-Modulen ist die Abhängigkeitsrichtung invertiert (App hängt nicht vom Feature ab). Hilts Voraussetzung — das die `Application` kompilierende Modul muss alle Hilt-Module transitiv sehen — ist verletzt.
- **Versionen:** Architektur-Constraint, inkl. 2.55.
- **FIX (funktionserhaltend, Feature NICHT weglassen):** Component-Dependencies: im App-Modul ein `@EntryPoint @InstallIn(SingletonComponent::class)`; im Feature eine Dagger-`@Component(dependencies = [...EntryPoint::class])`; zur Laufzeit per `EntryPointAccessors.fromApplication(...)` die Bindings holen.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-multi-module

### MT5. Jede Library-Änderung baut die Dagger-Komponente neu / stale Incremental-kapt
- **Symptom:** Kleine Änderungen → voller Rebuild der aggregierten Komponente; mit kapt werden Cross-Modul-Referenzen manchmal nicht regeneriert (Build schlägt fehl / veraltete Bindings).
- **Ursache:** Ohne Aggregating-Task ist der Processor non-isolating; plus bekannter Incremental-kapt-Bug (#2082/#1684).
- **Versionen:** inkl. 2.55.
- **FIX:** `hilt { enableAggregatingTask = true }` (isolating + Test-Component-Sharing); bei kapt-Stale auf KSP umsteigen (Anker nutzt KSP) bzw. einmal clean bauen.
- **Quelle:** https://dagger.dev/hilt/gradle-setup.html · https://github.com/google/dagger/issues/2082

### MT6. `HiltAndroidRule` läuft nicht zuerst → Activity/Compose-Injection scheitert ⭐ HAEUFIG
- **Symptom:** Bei `ActivityScenarioRule`/`createAndroidComposeRule` schlägt die Injection fehl (NPE bei injizierten Feldern, „component not initialized").
- **Ursache:** `ActivityScenarioRule` ruft `onCreate` auf, was eine fertige Hilt-Komponente voraussetzt. Läuft `HiltAndroidRule` nicht VOR den anderen Rules, ist sie noch nicht da.
- **Versionen:** inkl. 2.55; `order`-Attribut braucht JUnit ≥ 4.13.
- **FIX:**
  ```kotlin
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()
  ```
  Bei JUnit < 4.13 `RuleChain` mit `HiltAndroidRule` als äußerster Rule.
- **Quelle:** https://dagger.dev/hilt/testing.html · https://developer.android.com/training/dependency-injection/hilt-testing

### MT7. Test-Infrastruktur unvollständig — `inject()`, Custom-Runner, KSP-Test-Compiler ⭐ HAEUFIG
- **Symptom:** Bündel verwandter Fehler: injizierte Test-Felder null (`UninitializedPropertyAccessException`); „HiltTestApplication not used"; fehlende Test-Komponenten/MissingBinding im Test.
- **Ursache:** (a) `hiltRule.inject()` in `@Before` vergessen (Injection erfolgt NICHT automatisch); (b) kein Custom-Runner, der `HiltTestApplication` setzt; (c) `kspTest`/`kspAndroidTest`-Compiler fehlt → keine Test-Komponenten.
- **Versionen:** inkl. 2.55.
- **FIX:** (a) `@Before fun init() { hiltRule.inject() }`; (b) Runner anlegen + registrieren:
  ```kotlin
  class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, ctx: Context?) =
      super.newApplication(cl, HiltTestApplication::class.java.name, ctx)
  }
  // build.gradle: testInstrumentationRunner = "<pkg>.CustomTestRunner"
  ```
  (c) `kspTest("com.google.dagger:hilt-compiler:2.55")` + `kspAndroidTest(...)`.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-testing · https://dagger.dev/hilt/testing.html

### MT8. Robolectric + Hilt: falsche Test-Application → Hilt-Komponente fehlt
- **Symptom:** Robolectric-Tests scheitern, weil nicht `HiltTestApplication` läuft.
- **Ursache:** Robolectric nutzt sonst die Standard-Application ohne Hilt-Komponente.
- **Versionen:** inkl. 2.55.
- **FIX:** pro Test `@Config(application = HiltTestApplication::class)` ODER global `robolectric.properties` mit `application=dagger.hilt.android.testing.HiltTestApplication`.
- **Quelle:** https://dagger.dev/hilt/robolectric-testing.html

### MT9. `@BindValue` + `ActivityScenarioRule`: Binding im uninitialisierten Zustand injiziert
- **Symptom:** Eine per `@BindValue` ersetzte Binding ist im Test null, obwohl in `@Before` gesetzt.
- **Ursache:** `ActivityScenarioRule` erzeugt die Activity VOR `@Before`; ein erst in `@Before` initialisiertes `@BindValue`-Feld kommt zu spät.
- **Versionen:** inkl. 2.55.
- **FIX:** `@BindValue`-Feld im Feld-Initializer setzen: `@BindValue @JvmField val fakeBar: Bar = FakeBar()`.
- **Quelle:** https://dagger.dev/hilt/testing.html

### MT10. `@UninstallModules` kann `@TestInstallIn`-Module nicht deinstallieren
- **Symptom:** `@UninstallModules(...)` entfernt eine per `@TestInstallIn` gesetzte Fake-Binding nicht; Doppel-Binding/altes Fake bleibt.
- **Ursache:** `@UninstallModules` deinstalliert ausschließlich `@InstallIn`-Module, nicht `@TestInstallIn`.
- **Versionen:** inkl. 2.55.
- **FIX:** `@TestInstallIn`-Modul splitten: (1) `@TestInstallIn` das nur das Prod-Modul ENTFERNT, (2) normales `@InstallIn`-Modul mit dem Standard-Fake — Letzteres kann `@UninstallModules` gezielt deinstallieren.
- **Quelle:** https://dagger.dev/hilt/testing.html

### MT11. `launchFragmentInContainer` funktioniert nicht mit Hilt
- **Symptom:** Hilt-Fragment-Tests mit `launchFragmentInContainer` injizieren nicht / Compile-/Runtime-Fehler (besonders Multi-Module).
- **Ursache:** `FragmentScenario`/`launchFragmentInContainer` nutzen eine NICHT mit `@AndroidEntryPoint` annotierte Activity (kein offizieller Support).
- **Versionen:** inkl. 2.55; Multi-Module-Probleme in #3552.
- **FIX:** `launchFragmentInHiltContainer`-Helper (aus den Android architecture-samples) verwenden — startet eine `@AndroidEntryPoint`-Test-Container-Activity. Alternativ Hilt-Activity starten und Fragment manuell attachen.
- **Quelle:** https://developer.android.com/training/dependency-injection/hilt-testing · https://github.com/google/dagger/issues/1953

---

## ✅ Fix-Status (was ist schon behoben?)

> **Ehrlichkeits-Regel:** „belegt gefixt" = durch Changelog/Maintainer-Aussage/mehrere Nutzer bestätigt.
> Alles andere bleibt „offen / by-design". `gh`-Verifikation war in dieser Umgebung nicht möglich
> (keine GitHub-CLI in der Sandbox) — Belege stammen aus den verlinkten Issues/Release-Notes;
> zwei Schlüssel-Issues (#4582, #4907/#4574) wurden direkt am Issue-Tracker gegengelesen.

### Belegt gefixt (bis zum Projekt-Anker Hilt 2.55 / KSP 2.1.0-1.0.29)

| Früherer Bug | gefixt ab | Beleg | Bezug |
|--------------|-----------|-------|-------|
| Kotlin-2.1.0-Metadata „max supported 2.0.0" | **Hilt 2.55** | Maintainer + mehrere Nutzer (#4582) | V1 |
| `@LazyClassKey`-Klassen-Merge unter R8 full mode (kaputte Multibinding-Map) | **Hilt 2.52** (in 2.55) | #3197 / Release-Notes 2.52 | R5 |
| Inkrementelles KSP-Tracking nach Löschen `@HiltViewModel` / breite Dirty-Sets | **Hilt 2.54.1** (in 2.55) | #4060 / #4549 / KSP #1554 | R9, V7 |

→ **Diese vier Punkte sind mit eurem Stack (2.55) bereits erledigt** und nur als Historie/Regressions-Wächter gelistet.

### Noch NICHT gefixt bzw. erst NACH 2.55 relevant (Workaround/Beobachtung aktiv)

| Bug | Status | Was tun | Bezug |
|-----|--------|---------|-------|
| `@HiltViewModel`-Konstruktor von R8 entfernt (Release-Crash) | **Regression ab 2.56.1, bei Recherche offen** (#4739) | Auf 2.55 NICHT betroffen; beim Upgrade auf ≥ 2.56.1 die `-keepnames`-Regel manuell setzen | R1 |
| Hilt-Plugin auf Dynamic-Feature-Modul (`androidComponents`) | betrifft 2.55/2.56, **gefixt ab 2.57** (#4574) | Single-Module bleibt unberührt; bei Feature-Modul Hilt ≥ 2.57 oder Component-Dependencies | MT3, MT4 |
| KSP2-Vollbetrieb mit Hilt ≲ 2.55 | KSP2-Support **sauber ab Hilt 2.57** | Anker baut über KSP1-kompatible Kette stabil; KSP2-Umstieg mit Hilt ≥ 2.57 | V6 |
| Alle `@InstallIn`/Scope-/Binding-/Setup-Fälle (Sektion A, M, große Teile W/MT) | **by design** (kein Library-Fix) | korrektes Setup, nie Feature weglassen | A*, M*, W5, MT* |

---

## ✅ Pflicht-Checkliste (vor dem Commit von Hilt-Code mental durchgehen)

- [ ] **Versionen gekoppelt:** KSP-Präfix = Kotlin-Version (2.1.0 ↔ `2.1.0-1.0.29`); `hilt-android` und `hilt-compiler` exakt gleiche Version (2.55). (V1, V2, R3)
- [ ] **Plugin + Slot:** Hilt-Gradle-Plugin angewendet, Processor über `ksp("com.google.dagger:hilt-compiler")` (nicht kapt); bei `@HiltWorker` ZUSÄTZLICH `ksp("androidx.hilt:hilt-compiler")`. (V3, V4, W4)
- [ ] **App registriert:** `@HiltAndroidApp` + `android:name=".MyApp"` im Manifest. (A1, A2)
- [ ] **Entry-Points annotiert:** Host-Activity vor Fragment `@AndroidEntryPoint`; Service/Receiver korrekt (Receiver: `super.onReceive()` zuerst). (A3, A6)
- [ ] **Felder:** `@Inject`-Felder non-private, Zugriff erst nach `super.onCreate()`. (A4, A5)
- [ ] **Module:** jedes `@Module` hat `@InstallIn`; `@Binds` abstract (interface) getrennt von `@Provides` (companion). (M1, M2)
- [ ] **Scopes/Qualifier:** kein langlebig→kurzlebig (M5); doppelte Typen mit `@Qualifier` (M4); ViewModel-Deps in `ViewModelComponent` (M6); Context qualifiziert (M7).
- [ ] **ViewModel/Compose:** `@HiltViewModel` + `hiltViewModel()`; geteilte VMs via `hiltViewModel(parentEntry)` + `remember`. (VM1, VM3)
- [ ] **WorkManager:** Default-`WorkManagerInitializer` entfernt + `Configuration.Provider` als **Property** (WM 2.9+). (W1, W3)
- [ ] **Release-Build getestet:** `assembleRelease` mit `minifyEnabled true` baut UND startet; bei Hilt ≥ 2.56.1 die HiltViewModel-`-keepnames`-Regel gesetzt; `missing_rules.txt` geprüft. (R1, R4)
- [ ] **Multi-Module:** `hilt { enableAggregatingTask = true }`; `@HiltAndroidApp` nur im App-Modul. (MT2, MT1)
- [ ] **Tests:** `HiltAndroidRule` `order = 0` + `hiltRule.inject()` + Custom-Runner mit `HiltTestApplication` + `kspTest`/`kspAndroidTest`-Compiler. (MT6, MT7)

---

## 🔗 Bezug: Bug-Abschnitt ↔ Best-Practices

> Gegenseite (wie macht man es richtig):
> [`best-practices/android/hilt-dagger.md`](../../best-practices/android/hilt-dagger.md)
> (dort die Spiegel-Tabelle Best-Practice-Abschnitt ↔ Bug-Abschnitt).

| Bug-Abschnitt (hier) | Verwandter Best-Practice-Abschnitt |
|----------------------|------------------------------------|
| V1 Metadata 2.1.0 · V2 KSP-Suffix | §1.4 KSP an Kotlin koppeln |
| V3 Plugin fehlt · V5 Reihenfolge | §1.5 Gradle-Plugin · §1.6 Plugin-Reihenfolge |
| V4 generierte Klassen fehlen | §1.2/§1.3 Dependency-Slots |
| V6 KSP2-Inkompat | §1.1 KSP statt kapt (KSP2-Hinweis) |
| V7 enableAggregatingTask | §1.7 enableAggregatingTask |
| A1/A2 `@HiltAndroidApp`/Manifest | §3.4 `@HiltAndroidApp` |
| A3 Fragment ohne Host-Activity | §3.5 `@AndroidEntryPoint` |
| A4/A5 Field-Reihenfolge/private | §3.2 Field-Injection |
| A6 BroadcastReceiver · A7 ContentProvider · A8 Accessor | §3.6 `@EntryPoint` |
| M1 `@Binds` abstract · M9 Effizienz | §2.3/§2.4 `@Binds`/`@Provides` |
| M2 fehlendes `@InstallIn` · M10 | §2.1 `@InstallIn` Component |
| M3 Missing Binding | §2.3 Brücke wählen |
| M4 Duplicate Bindings | §2.6 Qualifier |
| M5 Scope-Mismatch · M6 ViewModelComponent | §2.2 Scopes sparsam |
| M7 Context | §2.7 Context-Qualifier |
| M8 leeres Multibinding | §2.8 Multibindings |
| VM1/VM6 ViewModel | §4.1 `@HiltViewModel` |
| VM3/VM7 geteilter Scope | §4.4 Nav-Graph teilen |
| VM4 SavedStateHandle | §4.3 SavedStateHandle |
| VM5 Assisted | §3.7/§4.5 Assisted Injection |
| W1–W6 `@HiltWorker` | §6.1–6.3 `@HiltWorker` |
| R1/R4 Release-R8 | (Almanach-spezifisch; BP §1.7 Aggregation als Prävention) |
| R6 kapt+KSP | §1.1 KSP statt kapt |
| MT1–MT5 Multi-Module | §5.1–5.7 Multi-Module |
| MT6–MT11 Tests | §6.4–6.6 Test-Setup |
