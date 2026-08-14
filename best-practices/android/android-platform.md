# Android-Framework / Platform-SDK — Best Practices

**Stand:** 2026-06-02 (Best-Practices-Recherchelauf, 7 Researcher, offizielle Quellen zuerst).
**Versions-Anker (live ermittelt aus den `libs.versions.toml`):**
- **Beide Apps:** `targetSdk = compileSdk = 36` (Android 16 „Baklava"), Java 17, Kotlin 2.1.0,
  Room **2.7.0**, Hilt 2.55, Lifecycle **2.8.7** (`lifecycle-runtime-compose`,
  `lifecycle-viewmodel-compose`, `lifecycle-process`), activity-compose 1.9.3,
  navigation-compose 2.8.7, core-ktx 1.15.0, Coroutines 1.9.0.
- **BestJournalAndroid:** minSdk **26**, AGP 8.7.3, **AlarmManager**-Reminder (+ `BootReminderReceiver`),
  Firebase, Billing, Drive, **sherpa-onnx** (native `.so` → 16-KB-Page-Size!).
- **EntropieReductor:** minSdk **28**, AGP 8.10.0, **WorkManager 2.10.0** + **hilt-work 1.2.0**
  (Foreground-Worker `dataSync`/`microphone`), `ProcessLifecycleOwner`, Glance-Widget,
  Media3, Health Connect, Maps, DataStore, AppAuth, Drive.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/android/android-platform.md`](../../bugs/android/android-platform.md)):
> der Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von
> vornherein richtig macht, damit der Bug gar nicht erst entsteht*. Quellen-Rangordnung: offizielle
> Android/Google-Quelle (developer.android.com, Android Developers Blog/Medium, Now in Android) =
> Grundwahrheit (`offiziell`), Community/Blogs = `extern` (sekundaer, ueberstimmt nie das Offizielle).
> Jeder Punkt traegt sein `offiziell`/`extern`-Label + Quelle. Fast alle Punkte sind
> versionsunabhaengige Denkmuster und gelten in beiden Projekten; versions-spezifische Pflicht-
> Migrationen (Android 14/15/16) sind als solche markiert.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | App-Struktur planen | 3 Layer (UI→Domain optional→Data), UDF, Single-Module ok | §1 |
| 2 | UI-State im ViewModel | Immutable `UiState`, `stateIn(WhileSubscribed(5000))`, kein Context | §1.3 |
| 3 | State in Compose lesen | Immer `collectAsStateWithLifecycle()`, nie `collectAsState()` | §2.3 |
| 4 | Flow im UI sammeln | `repeatOnLifecycle(STARTED)`, nie `launchWhenX`; Fragment: `viewLifecycleOwner` | §2.1 |
| 5 | `registerForActivityResult` / `registerX` | Frueh + symmetrisch registrieren; jedes hat ein `unregisterX` | §2.6 |
| 6 | Process Death absichern | `SavedStateHandle` + Persistenz; kein `Context`/`View` im ViewModel | §2.5 |
| 7 | `@Database`-`version` erhoeht | Echter Migrationspfad, `exportSchema=true`, `MigrationTestHelper` | §3.1, §3.3, §3.4 |
| 8 | DB-Backup / Drive-Upload | WAL-Checkpoint vorher; nie `fallbackToDestructiveMigration` in Prod | §3.5, §3.2 |
| 9 | DB-Instanz erzeugen / DAO | Hilt-`@Singleton`; DAO `suspend`/`Flow` (off-main) | §3.7, §3.8 |
| 10 | Garantierte Background-Arbeit | WorkManager (ueberlebt Prozess/Reboot); `getStopReason()` loggen | §4.1, §4.8 |
| 11 | `@HiltWorker` nutzen | `Configuration.Provider` + Default-Initializer im Manifest entfernen | §4.9 |
| 12 | Foreground-Service bauen | Drei Dinge gleichzeitig + `startForeground()` binnen ~5 s | §5.1, §5.2 |
| 13 | Notifications senden | Channel (ab 8) + `POST_NOTIFICATIONS` (ab 13) + kein Trampolin | §5.7, §5.8, §5.9 |
| 14 | Permission anfragen | `RequestPermission`-Flow, einzeln pruefen, permanently-denied erkennen | §6.1 |
| 15 | targetSdk-36-Migration | Edge-to-Edge, Predictive Back, adaptive Layouts, 16-KB, FileProvider | §7 |

---

## 🔗 Bezug zum Bug-Almanach ([`bugs/android/android-platform.md`](../../bugs/android/android-platform.md))

Jeder Best-Practice-Abschnitt hier ist die **Praevention** zu einer Sektion im Bug-Almanach —
zwei Seiten derselben Medaille. Tritt einer der dortigen Bugs auf, liefert der Almanach die konkrete
funktionserhaltende Loesung; dieser Abschnitt sagt, wie man ihn von vornherein vermeidet.

| Best-Practice (hier) | Verhindert Bug(s) in `bugs/android/android-platform.md` |
|----------------------|--------------------------------------------------|
| §1 App-Architektur (UI/Domain/Data, UDF, Hilt) | Grundlage gegen §1.5/§1.6 (ViewModel/Process-Death); allg. Disziplin |
| §2 Lifecycle-sicheres Arbeiten | §1 (1.1–1.11) — Flow-Lifecycle, register/unregister, Leaks, Process Death |
| §3 Room-Migrationsstrategie & Runtime | §5 (5.1–5.15) — Migrationen, WAL, Singleton, Threading, CursorWindow |
| §4 WorkManager-Einsatz | §4 (4.1–4.13) — Init, Quota, Constraints, Periodic, Boot/force-stop |
| §5 Foreground-Service & Notifications | §3 (3.1–3.9) + §6.2/§6.3/§6.5 (Channels, Trampolin, Full-Screen-Intent) |
| §6 Permission-Flow + PendingIntent/AlarmManager | §2 (2.1–2.10) + §6.1/§6.4/§6.6–§6.9 (Mutability, Exact-Alarm, Boot, Receiver) |
| §7 Scoped Storage + targetSdk-36-Compliance | §7 (7.1–7.3) + §8 (8.1–8.9) — Edge-to-Edge, Predictive Back, 16-KB, Large-Screen |

---

## 1. Empfohlene App-Architektur (UI/Domain/Data + UDF + Hilt)

> Versions-Anker: targetSdk=compileSdk=36, minSdk 26/28, Kotlin 2.1.0, Hilt 2.55, Compose, Room 2.7.0, Lifecycle 2.8.7. Single-Module, MVVM+Hilt+Compose+Room. Alle Empfehlungen sind versionsneutral gueltig fuer diesen Stack.

### 1.1 Die drei Layer und ihre Verantwortlichkeiten

1. **Trenne in drei Layer mit klarer Abhaengigkeitsrichtung: UI → (Domain optional) → Data.** Abhaengigkeiten zeigen IMMER nach unten — die Data-Schicht kennt die UI-Schicht nie. (offiziell: https://developer.android.com/topic/architecture)
2. **UI-Layer:** Stellt Daten auf dem Bildschirm dar und verarbeitet Nutzer-Interaktionen. Besteht aus UI-Elementen (Compose) + State-Holdern (ViewModel). State-Holder leben so lange wie die UI-Elemente, fuer die sie den State liefern. (offiziell: https://developer.android.com/topic/architecture)
3. **Data-Layer:** Enthaelt die Geschaeftslogik und stellt die App-Daten bereit. Besteht aus Repositories (eines pro Datentyp) + Datenquellen. (offiziell: https://developer.android.com/topic/architecture)
4. **Domain-Layer (optional):** Kapselt komplexe oder zwischen mehreren ViewModels wiederverwendbare Geschaeftslogik in UseCases. Fuer kleine Single-Module-Apps oft nicht noetig (siehe 1.5). (offiziell: https://developer.android.com/topic/architecture)
5. **Halte dich an Separation of Concerns:** Jede Klasse/Methode/Package hat eine klar definierte Verantwortlichkeit. Schreibe NIEMALS die gesamte Logik in Activities — Activities sind reine UI-Container, deren Lebenszyklus das OS kontrolliert (haeufige Zerstoerung/Neuerstellung). (offiziell: https://developer.android.com/topic/architecture)

### 1.2 Unidirektionaler Datenfluss (UDF)

6. **State fliesst nach unten, Events nach oben:** State runter (ViewModel → UI), Events rauf (UI → ViewModel). Das ViewModel verarbeitet Events, aktualisiert den State, der State fliesst zurueck in die UI. (offiziell: https://developer.android.com/topic/architecture/ui-layer)
7. **Single Source of Truth (SSOT):** Jeder Datentyp hat genau einen Besitzer. Nur die SSOT darf die Daten aendern und exponiert sie als **immutable** Typen. Das zentralisiert Aenderungen, schuetzt die Datenintegritaet und macht Bugs leichter nachvollziehbar. (offiziell: https://developer.android.com/topic/architecture)
8. **Wende UDF in ALLEN Layern an** — nicht nur in der UI. Auch Repository → Datenquellen folgt dem SSOT-Prinzip. (offiziell: https://developer.android.com/topic/architecture)

### 1.3 UI-State-Holder (ViewModel) und UiState

9. **Definiere eine immutable `UiState`** als `data class` (zusammengehoerige Felder) oder `sealed interface` (sich gegenseitig ausschliessende Zustaende wie Loading/Success/Error). Benenne sie `<Funktionalitaet>UiState` (z.B. `JournalUiState`). (offiziell: https://developer.android.com/topic/architecture/ui-layer)
10. **Exponiere den State NUR lesbar:** Intern `MutableStateFlow`, nach aussen `StateFlow` via `asStateFlow()` — oder Compose `var uiState by mutableStateOf(...) ; private set`. Niemals den mutablen State nach aussen geben. (offiziell: https://developer.android.com/topic/architecture/ui-layer)
11. **Bevorzuge reaktive `StateFlow`-Erzeugung mit `stateIn`:** Repository-`Flow` per `map` in UiState transformieren und mit `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)` zu StateFlow machen. `WhileSubscribed(5000)` haelt die Pipeline 5 s nach dem letzten Collector aktiv — verhindert unnoetiges Neu-Laden bei kurzer Navigation/Rekomposition und stoppt die Produktion, wenn die UI nicht sichtbar ist. (offiziell: https://developer.android.com/topic/architecture/ui-layer/state-production)
12. **Sammle State lifecycle-bewusst:** In Compose IMMER `collectAsStateWithLifecycle()` (nicht `collectAsState()`) — beobachtet nur, wenn der Composable angezeigt wird. (offiziell: https://developer.android.com/topic/architecture/ui-layer)
13. **Ein zusammenhaengender Stream fuer zusammengehoerige Daten:** Lieber ein `UiState`-Objekt mit allen verwandten Feldern als mehrere getrennte Streams — verhindert Out-of-Sync-State. Abgeleitete Werte als computed properties (z.B. `val canSave get() = isSignedIn && isPremium`). (offiziell: https://developer.android.com/topic/architecture/ui-layer)
14. **Mache ViewModel-Operationen main-safe** ueber `viewModelScope` + Coroutines. Lade-/Fehlerzustaende explizit im UiState abbilden (`isLoading`, `userMessages` oder sealed `Loading/Error`). (offiziell: https://developer.android.com/topic/architecture/ui-layer)
15. **Sealed-Interface-UiState fuer Bildschirm-Zustaende** (Now-in-Android-Muster): `Loading` / `Success(data)` / `Error` — initial `Loading`. (extern: https://medium.com/@YodgorbekKomilo/understanding-statein-and-sharein-in-kotlin-flow-for-jetpack-compose-developers-c081a6ad1dc0)

### 1.4 Data-Layer (Repository, SSOT, offline-first, Mapping)

16. **Repository-Pattern:** Eine Repository-Klasse pro Datentyp (`JournalRepository`), einziger Einstiegspunkt in den Data-Layer. Repositories haengen von Datenquellen ab — nie umgekehrt, und andere Layer greifen NIE direkt auf Datenquellen zu. (offiziell: https://developer.android.com/topic/architecture/data-layer)
17. **Offline-first / SSOT:** Die lokale Datenbank (Room) ist die empfohlene Single Source of Truth. Alle Daten laufen durch die SSOT, Netzdaten werden in die DB geschrieben und die UI beobachtet die DB. (offiziell: https://developer.android.com/topic/architecture/data-layer)
18. **Exponiere immutable Daten:** `Flow<T>` fuer beobachtbare Aenderungen ueber die Zeit, `suspend fun` fuer einmalige CRUD-Operationen. Niemals mutable Strukturen oder interne Cache-Referenzen herausgeben. (offiziell: https://developer.android.com/topic/architecture/data-layer)
19. **Eigene Modelle pro Layer + Mapper:** API-Model (Netzwerk), Entity (Room), Domain-/Business-Model, UI-Model getrennt halten und mappen. Unnoetige Felder pro Layer wegtrimmen, niemals API-Models direkt bis in die UI durchreichen. (offiziell: https://developer.android.com/topic/architecture/data-layer)
20. **Datenquellen generisch benennen:** `JournalRemoteDataSource` / `JournalLocalDataSource` — NICHT nach Implementierungsdetail (`JournalSharedPreferencesDataSource` ist ein Anti-Pattern). Jede Datenquelle hat genau eine Verantwortung und kennt die anderen nicht. (offiziell: https://developer.android.com/topic/architecture/data-layer)
21. **Thread-Sicherheit / Main-Safety:** Alle Data-Layer-Aufrufe main-safe halten (Room/Retrofit sind es bereits). Injiziere `CoroutineDispatcher` (testbar), nutze `Mutex.withLock` fuer In-Memory-Caches. (offiziell: https://developer.android.com/topic/architecture/data-layer)
22. **Waehle den richtigen Scope nach Operations-Lebensdauer:** UI-bezogen → ViewModel/`viewModelScope`; App-bezogen (laeuft weiter, solange App offen) → Application-`CoroutineScope`; geschaeftskritisch (ueberlebt Process Death) → WorkManager. (offiziell: https://developer.android.com/topic/architecture/data-layer)
23. **Domain-Fehler als eigene Exceptions** (z.B. `UserNotAuthenticatedException`), `Flow.catch` nutzen, niemals Exceptions still schlucken. (offiziell: https://developer.android.com/topic/architecture/data-layer)

### 1.5 Domain-Layer: wann UseCases sinnvoll sind (und wann Overkill)

24. **Setze UseCases ein, wenn:** komplexe Geschaeftslogik gekapselt werden muss, dieselbe Logik in mehreren ViewModels gebraucht wird, oder Daten aus mehreren Repositories kombiniert werden. (offiziell: https://developer.android.com/topic/architecture/domain-layer)
25. **UseCases sind Overkill, wenn:** sie nur einen trivialen Repository-Aufruf weiterreichen ohne Mehrwert. Fuer kleine Single-Module-Apps darf das ViewModel direkt aufs Repository zugreifen. UseCases progressiv hinzufuegen, wenn der Bedarf entsteht. (offiziell: https://developer.android.com/topic/architecture/domain-layer)
26. **Benennung:** `<Verb im Praesens> + <Nomen> + UseCase`, z.B. `GetLatestNewsWithAuthorsUseCase`, `LogOutUserUseCase`. (offiziell: https://developer.android.com/topic/architecture/domain-layer)
27. **Eine einzige oeffentliche Methode via `operator fun invoke()`** — der UseCase wird wie eine Funktion aufrufbar (`val date = formatDateUseCase(today)`). (offiziell: https://developer.android.com/topic/architecture/domain-layer)
28. **UseCases sind zustandslos** (keine mutable Daten), haben keinen eigenen Lebenszyklus (an die aufrufende Klasse gebunden), und sind main-safe — lange Blocking-Operationen per `withContext(defaultDispatcher)` auslagern. Sie haengen von Repositories oder anderen UseCases ab. (offiziell: https://developer.android.com/topic/architecture/domain-layer)

### 1.6 Dependency Injection mit Hilt

29. **Hilt-Grundgeruest:** `@HiltAndroidApp` an die `Application`, `@AndroidEntryPoint` an Activity/Service/Receiver, `@HiltViewModel` + `@Inject constructor` am ViewModel, in Compose per `hiltViewModel()` beziehen. In Compose nur die Root-`ComponentActivity` annotieren, nicht einzelne Composables. (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)
30. **Bevorzuge Constructor-Injection** (`@Inject constructor`) — am testbarsten, Hilt validiert den Graph zur Compile-Zeit. (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)
31. **Interface-Binding fuer Testbarkeit:** Binde Interfaces statt konkreter Klassen via `@Binds` in einem `abstract`-Modul, wenn du die Implementierung selbst besitzt (effizienter als `@Provides`). So lassen sich im Test Fakes/Mocks leicht einsetzen. (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)
32. **`@Provides` fuer Fremd-Bibliotheken / Builder-Pattern** (Retrofit, OkHttp), wenn du die Klasse nicht besitzt oder spezielle Erzeugungslogik brauchst — in einem `object`-Modul. (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)
33. **`@InstallIn` mit dem engstmoeglichen Component:** `SingletonComponent` (app-weit), `ViewModelComponent`, `ActivityComponent`. Engerer Scope = bessere Speicherverwaltung. (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)
34. **Scopes sparsam einsetzen:** `@Singleton`, `@ViewModelScoped`, `@ActivityScoped` nur, wenn die Bindung internen State braucht, synchronisiert werden muss oder messbar teuer zu erzeugen ist. Sonst weglassen (geringerer Memory-Overhead). (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)
35. **Context korrekt qualifizieren:** `@ApplicationContext` bzw. `@ActivityContext` statt rohem Context. Bei mehreren Implementierungen desselben Typs Qualifier (`@Qualifier`) fuer ALLE Varianten verwenden. (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)
36. **`@EntryPoint` fuer nicht unterstuetzte Klassen** (ContentProvider) — Bruecke zwischen Hilt-verwaltetem und nicht-verwaltetem Code. (offiziell: https://developer.android.com/training/dependency-injection/hilt-android)

### 1.7 Modularisierung — ehrlich: Single-Module ist fuer kleine Apps voellig ok

37. **Single-Module ist fuer kleine, eigenstaendige Apps die richtige Wahl.** Modularisierung NICHT verfrueht einfuehren: bei Prototyp/MVP, 1-2 Entwicklern, kleinem nicht wachsendem Projekt ueberwiegt der Overhead den Nutzen. Beide Apps hier (Single-Module) sind damit korrekt aufgestellt. (offiziell: https://developer.android.com/topic/modularization)
38. **Modularisiere erst, wenn ein klarer Nutzen entsteht:** Code-Wiederverwendung ueber Apps/Flavors, strikte Sichtbarkeitskontrolle (`internal`), Play Feature Delivery, grosse wachsende Codebasis, Team-Autonomie. Faustregel: erst Monolith, dann modularisieren, wenn der Bedarf da ist. (offiziell: https://developer.android.com/topic/modularization)
39. **Falls modularisiert:** unidirektionale, hierarchische Abhaengigkeiten (keine Zirkel, `core` haengt nie von `feature` ab), Granularitaet vernuenftig (~10-20 Module bei grosser App, nicht 50+ und kein Mono-Modul), klare Namen (`:core:database`, `:feature:auth` statt `:utils`/`:common`). (offiziell: https://developer.android.com/topic/modularization)
40. **Zentralisiere Build-Config & Versionen** (Version Catalog `libs.versions.toml`, gemeinsame SDK-Levels) — auch im Single-Module sinnvoll. (offiziell: https://developer.android.com/topic/modularization)

### 1.8 Klare Don'ts (Anti-Patterns)

41. **KEINE Business-/Geschaeftslogik im Composable** — Composables zeigen State an und melden Events. Logik gehoert ins ViewModel/Domain/Data. (offiziell: https://developer.android.com/topic/architecture/ui-layer)
42. **KEINE Business-Logik im ViewModel** — das ViewModel ist State-Holder, die eigentliche Geschaeftslogik lebt im Domain-/Data-Layer. (offiziell: https://developer.android.com/topic/architecture/ui-layer)
43. **KEINE Android-Framework-Abhaengigkeiten im ViewModel** (kein `Context`, keine `Resources`, keine `Activity`/`View`-Referenz) — verhindert Leaks und macht es untestbar. Wenn Context noetig: `AndroidViewModel` mit `@ApplicationContext` oder String-Resolver injizieren, nie Activity-Context halten. (offiziell: https://developer.android.com/topic/architecture/ui-layer)
44. **KEIN God-ViewModel** — nicht mehrere unzusammenhaengende Verantwortlichkeiten in einer Klasse. Pro Bildschirm/Feature ein ViewModel, Logik in UseCases/Repositories auslagern. (offiziell: https://developer.android.com/topic/architecture)
45. **KEINEN mutablen State nach aussen exponieren** — immer `private set` / `asStateFlow()`; den State nur ueber das ViewModel aendern. (offiziell: https://developer.android.com/topic/architecture/ui-layer)
46. **KEINE Daten in App-Komponenten speichern** (Activity/Service/Receiver sind ephemer — Daten gehen bei Rotation/Low-Memory verloren). Persistente Modelle (DB/Repository) verwenden. (offiziell: https://developer.android.com/topic/architecture)
47. **KEINE Datenquellen-Direktzugriffe aus UI/Domain** und keine SharedPreferences-/Implementierungs-Details in Klassennamen oder oeffentlichen APIs durchschimmern lassen. (offiziell: https://developer.android.com/topic/architecture/data-layer)
48. **KEIN `collectAsState()` statt `collectAsStateWithLifecycle()`** — sonst laeuft die Collection auch im Hintergrund weiter (verschwendete Arbeit, potenzielle Leaks). (offiziell: https://developer.android.com/topic/architecture/ui-layer)

---

## 2. Lifecycle-sicheres Arbeiten (Coroutines, Flow, ViewModel, Compose-Effekte)

### 2.1 Lifecycle-aware Coroutines: der richtige Scope

- **`viewModelScope` fuer ViewModel-Arbeit** — automatisch gecancelt wenn das ViewModel gecleart wird (z. B. fertige Bildschirm-Daten berechnen). Optional kann ein eigener `CoroutineScope` (z. B. `TestScope` fuer Tests, eigener Dispatcher/ExceptionHandler) per Konstruktor injiziert werden (offiziell: https://developer.android.com/topic/libraries/architecture/coroutines)
- **`lifecycleScope` fuer UI-gebundene Arbeit** — an Activity/Fragment-Lifecycle gekoppelt, gecancelt bei `onDestroy`. In Fragments IMMER `viewLifecycleOwner.lifecycleScope` statt `lifecycleScope` nutzen (Ausnahme: DialogFragment), sonst laeuft der Job ueber die View hinaus weiter (offiziell: https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda)
- **`repeatOnLifecycle(Lifecycle.State.STARTED)` statt `launchWhenStarted`/`launchWhenResumed`** — Letztere sind deprecated und unsicher: sie *suspendieren* nur den Consumer, der `callbackFlow`-Producer im Upstream laeuft im Hintergrund weiter und verbrennt CPU/RAM. `repeatOnLifecycle` cancelt den Coroutine-Block beim Unterschreiten von STARTED komplett und startet ihn beim Wiedererreichen neu (offiziell: https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda)
- **Sammelort**: Flows im UI-Layer mit Lifecycle sammeln — in `Activity.onCreate` bzw. `Fragment.onViewCreated`. Das ViewModel beobachtet NIE die UI; Datenfluss geht ViewModel → UI (offiziell: https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda)
- **Hinweis (Versions-Anker)**: `lifecycle-runtime-ktx`/`lifecycle-runtime-compose` 2.8.7 enthalten `repeatOnLifecycle` und `collectAsStateWithLifecycle`; Coroutines 1.9.0 liefert die strukturierte Cancellation. Passt zu Kotlin 2.1.0 / compileSdk 36.

### 2.2 Cold-Flow → StateFlow fuer UI

- **`stateIn(scope=viewModelScope, started=SharingStarted.WhileSubscribed(5_000), initialValue=…)`** — haelt das Upstream-Abo 5 s nach Verschwinden der UI aktiv (ueberlebt Config-Change/kurzes Background), cancelt danach, um Ressourcen zu sparen. Genau auf `collectAsStateWithLifecycle` (STARTED) abgestimmt (offiziell: https://developer.android.com/topic/libraries/architecture/coroutines)

### 2.3 Compose: Flows lifecycle-korrekt konsumieren

- **`collectAsStateWithLifecycle()`** ist der Default fuer Flow→State in Compose: sammelt ab `STARTED`, pausiert ab `STOPPED` — spart Akku, kein manuelles Start/Stop. `minActiveState = Lifecycle.State.RESUMED` nur wenn wirklich erst bei Sichtbarkeit+Fokus gesammelt werden soll (offiziell: https://developer.android.com/topic/libraries/architecture/coroutines)
- **Mehrere Flows** koennen parallel via mehrfaches `collectAsStateWithLifecycle()` gesammelt werden — jeder ist fuer sich lifecycle-sicher (offiziell: https://developer.android.com/topic/libraries/architecture/coroutines)

### 2.4 Compose-Effekt-APIs: das richtige Tool je Zweck

- **`LaunchedEffect(keys)`** fuer suspend-Arbeit ueber die Composition-Lebenszeit: startet beim Eintritt, cancelt beim Verlassen, **restartet bei Key-Wechsel**. Alle im Block genutzten Variablen MUESSEN entweder Key sein oder via `rememberUpdatedState` referenziert werden — zu wenige Keys = stale Werte, zu viele = unnoetige Restarts (offiziell: https://developer.android.com/develop/ui/compose/side-effects)
- **`LaunchedEffect(true)` ist verdaechtig** („so suspicious as a `while(true)`") — immer pruefen ob das wirklich gewollt ist (offiziell: https://developer.android.com/develop/ui/compose/side-effects)
- **`LaunchedEffect` ist an die Composition gekoppelt, NICHT an den Activity-Lifecycle** — fuer sichtbarkeits-/fokus-abhaengige Events (Analytics, Kamera) stattdessen `LifecycleEventEffect`/`LifecycleResumeEffect` nutzen (offiziell: https://developer.android.com/topic/libraries/architecture/coroutines)
- **`DisposableEffect` + `onDispose`** fuer Effekte mit Cleanup (Observer/Listener registrieren ↔ deregistrieren). Der `onDispose`-Block ist Pflicht-Schlussstatement (sonst Build-Fehler); ein leerer `onDispose` ist ein Code-Smell → dann passt vermutlich ein anderes Effect-API besser (offiziell: https://developer.android.com/develop/ui/compose/side-effects)
- **`rememberCoroutineScope()` NUR in Callbacks** (onClick etc.), um Coroutinen ausserhalb der Composition zu starten — niemals in der Composition-Logik selbst (offiziell: https://developer.android.com/develop/ui/compose/side-effects)
- **`rememberUpdatedState(value)`** fuer Werte/Callbacks, die sich aendern duerfen ohne den Effekt neu zu starten (z. B. `onTimeout` in einem Splash) — verhindert teure Effekt-Restarts (offiziell: https://developer.android.com/develop/ui/compose/side-effects)
- **`SideEffect { … }`** um Compose-State an Nicht-Compose-Objekte zu publizieren (laeuft nach jeder erfolgreichen Recomposition); Unidirectional Data Flow nicht brechen (offiziell: https://developer.android.com/develop/ui/compose/side-effects)
- **`LifecycleEventEffect(ON_RESUME)`** fuer one-shot Lifecycle-Events (Analytics) — kann NICHT auf `ON_DESTROY` hoeren (Composition endet vorher) (offiziell: https://developer.android.com/topic/libraries/architecture/lifecycle)
- **`LifecycleStartEffect(key){ … onStopOrDispose{} }`** fuer gepaarte Start/Stop-Ressourcen waehrend Sichtbarkeit (z. B. Location-Updates); **`LifecycleResumeEffect(key){ … onPauseOrDispose{} }`** fuer Ressourcen die nur bei Interaktion laufen (Kamera-Preview, Animation) — Cleanup laeuft exakt beim Lifecycle-Uebergang (offiziell: https://developer.android.com/topic/libraries/architecture/lifecycle)
- **`derivedStateOf` nicht missbrauchen**: einfache Ableitungen (`"$firstName $lastName"`) direkt berechnen; `derivedStateOf` nur wenn der Input oefter wechselt als die UI neu zeichnen muss (z. B. `firstVisibleItemIndex > 0`) (offiziell: https://developer.android.com/develop/ui/compose/side-effects)

### 2.5 ViewModel-Disziplin

- **ViewModel ueberlebt Config-Change, aber NICHT system-initiierten Process Death** → als Backup `SavedStateHandle` fuer kleine, leichtgewichtige Transient-State (Scroll-Position, ausgewaehlte Item-ID, in-progress Texteingabe). Grosse/komplexe Daten gehoeren in lokale Persistenz, nicht in SavedStateHandle (offiziell: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
- **State-Trennung**: Business-Logic-State → ViewModel + `SavedStateHandle`; reiner UI-State → `rememberSaveable` in Compose (offiziell: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
- **SavedState ist an den Task-Stack gebunden** — geht verloren bei Force-Stop, Wischen aus Recents oder Reboot (offiziell: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
- **Kein `Context`/`View`/`Activity`/`Fragment` im ViewModel halten** — wuerde Config-Change-Survival aushebeln und leakt die zerstoerte Activity. Falls App-Context noetig: `AndroidViewModel` oder DI mit `applicationContext` (offiziell: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)

### 2.6 Registrierungen IMMER frueh + symmetrisch

- **`registerForActivityResult()` unbedingt frueh & bedingungslos** registrieren — als Feld-Initializer oder in `onCreate`, NIE in einem Click-Listener/Callback und nie nach STARTED. Der Callback muss bei Activity-/Prozess-Recreation verfuegbar sein, deshalb „unconditionally registered every time your activity is created". Launchen darf man erst ab Lifecycle-State `CREATED`. Registrierung und `launch()` strikt trennen (offiziell: https://developer.android.com/training/basics/intents/result)
- **Jedes `registerX` braucht ein `unregisterX`** im passenden Lifecycle-Callback (Paar onStart/onStop, onResume/onPause, onCreate/onDestroy). Ein laufzeit-registrierter `BroadcastReceiver` haelt eine Context-Referenz → Leak, wenn das Scope den Context ueberlebt: „always unregister your broadcast receiver" (offiziell: https://developer.android.com/develop/background-work/background-tasks/broadcasts)
- **Android 14 (API 34): `ContextCompat.registerReceiver` mit Export-Flag Pflicht** fuer context-registrierte Receiver — `RECEIVER_EXPORTED` nur wenn System/andere Apps senden, sonst `RECEIVER_NOT_EXPORTED` (relevant bei targetSdk 36). Im kleinstmoeglichen Scope registrieren (`LifecycleResumeEffect`/`LifecycleStartEffect` > `onCreate/onDestroy`; bei `DisposableEffect`/ViewModel-Scope den `applicationContext` verwenden) (offiziell: https://developer.android.com/develop/background-work/background-tasks/broadcasts)

### 2.7 App-weite Foreground/Background-Erkennung

- **`ProcessLifecycleOwner` (`lifecycle-process`)** fuer app-weite Foreground/Background-Erkennung statt Activity-Zaehlung selbst zu bauen — `ON_START`/`ON_STOP` feuern beim ersten sichtbaren / letzten unsichtbaren Activity-Uebergang des Prozesses (genutzt im Entropie Reductor). Observer als `DefaultLifecycleObserver` registrieren (offiziell: https://developer.android.com/topic/libraries/architecture/lifecycle)

### 2.8 Leak-Vermeidung & Async-UI-Guard (Don'ts)

- **Handler/Sensor/Listener/Receiver** immer im gegenueberliegenden Lifecycle-Callback freigeben; `Handler.removeCallbacks`, `SensorManager.unregisterListener`, `unregisterReceiver`. Bevorzugt `LifecycleStartEffect`/`LifecycleResumeEffect` in Compose, die das Cleanup automatisch koppeln (offiziell: https://developer.android.com/topic/libraries/architecture/lifecycle)
- **Async-UI-Guard**: vor UI-Updates aus asynchronem Code `isFinishing`/`isDestroyed` pruefen bzw. in Compose nur `MutableState` aktualisieren (sicher, Rendering wartet bis sichtbar) statt direkt Views anzufassen (offiziell: https://developer.android.com/topic/libraries/architecture/lifecycle)
- **Don'ts klar**: ❌ `launchWhenStarted`/`launchWhenResumed` (deprecated, Producer laeuft weiter) ❌ Flow ohne Lifecycle in `onResume` sammeln (sammelt im Background weiter) ❌ `lifecycleScope` statt `viewLifecycleOwner.lifecycleScope` im Fragment ❌ `registerForActivityResult` in einem Callback ❌ Context/View im ViewModel ❌ `DisposableEffect` mit leerem `onDispose` ❌ Variablen im `LaunchedEffect` ohne Key/`rememberUpdatedState`.

---

## 3. Saubere Room-Migrationsstrategie & Runtime-Disziplin

**Versions-Anker:** Room 2.7.0 (beide Apps), Kotlin 2.1.0, KSP 2.1.0-1.0.29, targetSdk 36, minSdk 26/28, Hilt, `room.schemaLocation` via ksp-arg. Room 2.7 ist eine echte Zaesur: KMP-Refactoring, neue `SQLiteDriver`-Architektur, neu implementierter Connection-Pool, KSP2 empfohlen, `room-ktx` ist in `room-runtime` aufgegangen.

### 3.1 Migrationsdisziplin — bei JEDER `@Database`-version-Erhoehung ein echter Pfad

- **Eiserne Regel:** Jede Erhoehung von `version = N` in `@Database` braucht einen lueckenlosen Migrationspfad (1→2→3 …). Fehlt der Pfad, crasht Room beim Oeffnen mit `IllegalStateException: A migration from N to M was required but not found`. (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **`@AutoMigration` zuerst** — fuer Aenderungen, die Room aus den Schema-JSONs ableiten kann (Spalte hinzufuegen mit Default, neue Tabelle, neuer Index). Deklarativ in `@Database`:
  ```kotlin
  @Database(version = 2, entities = [User::class],
            autoMigrations = [AutoMigration(from = 1, to = 2)])
  abstract class AppDatabase : RoomDatabase()
  ```
  (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **Neue Spalte ohne Default = manuelle Migration noetig.** AutoMigration verlangt entweder einen `@ColumnInfo(defaultValue = "...")` oder NOT-NULL mit Default — sonst kann Room den Wert fuer bestehende Zeilen nicht ableiten. (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **`AutoMigrationSpec` fuer mehrdeutige Aenderungen** — Rename/Delete von Tabellen/Spalten kann Room NICHT erraten. Mit Annotationen explizit machen: `@RenameColumn`, `@DeleteColumn`, `@RenameTable`, `@DeleteTable`:
  ```kotlin
  @RenameColumn(tableName = "User", fromColumnName = "name", toColumnName = "fullName")
  class MyAutoMigration : AutoMigrationSpec
  // ... AutoMigration(from = 1, to = 2, spec = AppDatabase.MyAutoMigration::class)
  ```
  `onPostMigrate()` fuer Datenmassage nach der Strukturaenderung ueberschreiben. (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **Manuelle `Migration(N, M)`** fuer komplexe Faelle (Tabelle umbauen, Daten transformieren, Spaltentyp aendern). In Room 2.7 gibt es zwei `migrate`-Ueberladungen — die alte mit `SupportSQLiteDatabase` und die neue Driver-Variante mit `SQLiteConnection`:
  ```kotlin
  val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE Fruit (id INTEGER, name TEXT, PRIMARY KEY(id))")
    }
  }
  // builder.addMigrations(MIGRATION_1_2)
  ```
  (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions, https://developer.android.com/jetpack/androidx/releases/room)

### 3.2 `fallbackToDestructiveMigration` NICHT in Produktion

- **`fallbackToDestructiveMigration()` loescht ALLE Nutzerdaten**, sobald ein Migrationspfad fehlt. In einer App mit echten Userdaten (Journal-Eintraege!) ist das Datenverlust. Nur fuer reine Cache-/Wegwerf-DBs akzeptabel. (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **Selektive Fallbacks**, falls man es dennoch braucht: `fallbackToDestructiveMigrationFrom(1, 2)` (nur aus bestimmten Versionen) oder `fallbackToDestructiveMigrationOnDowngrade()` (nur bei Downgrade — sinnvoll bei Sideload/Beta-Rollback, verhindert sonst Crash). (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **Room 2.7+: explizites `dropAllTables`-Argument.** Die parameterlose Variante ist in neueren Room-Versionen zugunsten von `fallbackToDestructiveMigration(dropAllTables = true)` deprecated; `dropAllTables = true` raeumt wirklich alle Tabellen ab (sauberer Neuaufbau), `false` ist konservativer. Bewusst setzen, nicht raten. (extern: https://proandroiddev.com/why-room-crashes-when-you-change-your-database-and-how-to-fix-it-ca8e3538bf57)

### 3.3 `exportSchema = true` + Schema-JSONs einchecken (Pflicht)

- **`exportSchema = true`** in `@Database` + Schema-Verzeichnis konfigurieren. AutoMigration UND der MigrationTestHelper brauchen die Schema-JSONs beider Versionen — ohne sie ist beides nicht moeglich. (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **Room Gradle Plugin (2.6.0+) statt ksp-arg bevorzugen** — sauberer, cacheable, variant-faehig:
  ```kotlin
  plugins { id("androidx.room") }
  room { schemaDirectory("$projectDir/schemas") }
  ```
  Der reine `ksp { arg("room.schemaLocation=...") }`-Weg funktioniert weiter (so machen es beide Apps aktuell), aber das Plugin ist der empfohlene Stand. (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **Schema-JSONs sind Quellcode** — in Git einchecken. Sie sind die „Vergangenheit" der DB; ohne sie kann eine spaetere Session keine korrekte Migration/keinen Test mehr bauen.

### 3.4 Migrationen testen mit `MigrationTestHelper` (Pflicht-Pattern)

- **Jede Migration einzeln testen.** Schema-Ordner als androidTest-Asset einbinden:
  ```kotlin
  android { sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas") }
  // dependencies: androidTestImplementation("androidx.room:room-testing:2.7.0")
  ```
  ```kotlin
  @get:Rule val helper = MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      AppDatabase::class.java)   // 2.7-Konstruktor; aeltere Variante nimmt canonicalName + FrameworkSQLiteOpenHelperFactory()

  @Test fun migrate1To2() {
    helper.createDatabase(TEST_DB, 1).apply { execSQL("INSERT ..."); close() }
    helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)  // validiert Schema + Daten
  }
  ```
  (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)
- **Zusatztest „alle Migrationen am Stueck"**: DB in Version 1 anlegen, dann mit `addMigrations(*ALL_MIGRATIONS)` voll hochziehen und `openHelper.writableDatabase` oeffnen — faengt fehlende Pfade in der Kette. (offiziell: https://developer.android.com/training/data-storage/room/migrating-db-versions)

### 3.5 WAL: vor jedem Backup/Drive-Upload einen Checkpoint erzwingen (KRITISCH)

- **WAL-Falle:** Room laeuft per Default im WAL-Modus. Committete Transaktionen liegen dann im `-wal`-File, NICHT im Haupt-`.db`. Wer nur das `.db` kopiert/hochlaedt, verliert die juengsten Eintraege oder erzeugt eine korrupte Datei. (offiziell: https://www.sqlite.org/wal.html)
- **Vor jedem Backup eine der beiden Optionen:**
  1. `db.close()` — schliesst die letzte Connection, SQLite checkpointet final und loescht `-wal`/`-shm` → `.db` ist self-contained. (Deckt sich mit der bestehenden Projekt-Regel „WAL-Checkpoint Room.close() vor Backup".)
  2. `PRAGMA wal_checkpoint(TRUNCATE)` — explizit, ohne die DB zu schliessen; `TRUNCATE` laeuft (anders als `PASSIVE`) bis zum Ende durch und leert das WAL.
  (offiziell: https://www.sqlite.org/wal.html)
- **Beim Kopieren immer alle drei Dateien zusammen halten** (`*.db`, `*.db-wal`, `*.db-shm`), wenn man nicht vorher checkpointet. Die saubere Variante ist: checkpointen → nur `.db` sichern. (offiziell: https://www.sqlite.org/wal.html)

### 3.6 Auto-Backup-Falle: DB aus dem Backup ausschliessen statt `allowBackup=false`

- **Nicht `android:allowBackup="false"`** als Holzhammer — das nimmt dem Nutzer das ganze Backup. Stattdessen gezielt die DB-Dateien (inkl. `-wal`/`-shm`) ueber Backup-Regeln ausschliessen. (offiziell: https://developer.android.com/guide/topics/data/autobackup)
- **Android 12+ (`dataExtractionRules`)** und **Android 11- (`fullBackupContent`)** getrennt pflegen — bei minSdk 26/28 sind beide relevant:
  ```xml
  <!-- res/xml/backup_rules.xml (Android 12+) -->
  <data-extraction-rules>
    <cloud-backup>
      <exclude domain="database" path="app_database.db"/>
      <exclude domain="database" path="app_database.db-wal"/>
      <exclude domain="database" path="app_database.db-shm"/>
    </cloud-backup>
    <device-transfer> … gleiche excludes … </device-transfer>
  </data-extraction-rules>
  ```
  **Warum ausschliessen:** WAL-Files im Backup ohne Checkpoint = inkonsistenter/korrupter Restore. Wer ohnehin per eigenem Drive-Sync sichert, will die DB nicht doppelt (und halb) im Google-Auto-Backup haben. (offiziell: https://developer.android.com/guide/topics/data/autobackup)

### 3.7 DB als Singleton — nie pro ViewModel neu bauen

- **Eine `RoomDatabase`-Instanz pro Prozess.** Jede Instanz ist teuer; mehrere Builder fuehren zu Lock-Konflikten und Invalidierungs-Chaos. (offiziell: https://developer.android.com/training/data-storage/room)
- **Mit Hilt:** Provider mit `@Singleton` in einem `@Module`/`@InstallIn(SingletonComponent::class)` — Hilt garantiert die Einzigartigkeit. Ohne DI: `@Volatile`-Feld + `synchronized`-Double-Checked-Locking. NIEMALS die DB im ViewModel-Konstruktor erzeugen. (offiziell: https://developer.android.com/training/data-storage/room)

### 3.8 DAO off-main-thread — `suspend`/`Flow`

- **DAO-Methoden als `suspend fun` oder `Flow<T>`** zurueckgeben, damit Room automatisch off-main arbeitet (in 2.7 ueber den `setQueryCoroutineContext(Dispatchers.IO)`-Kontext steuerbar). (offiziell: https://developer.android.com/training/data-storage/room, https://developer.android.com/jetpack/androidx/releases/room)
  ```kotlin
  @Query("SELECT * FROM user") suspend fun getAll(): List<User>
  @Query("SELECT * FROM user") fun observeAll(): Flow<List<User>>
  ```
- **`allowMainThreadQueries()` nur in Tests** — in Produktion blockiert es den UI-Thread und fuehrt zu ANR. (offiziell: https://developer.android.com/training/data-storage/room)
- **Hinweis 2.7:** `room-ktx` ist in `room-runtime` aufgegangen — die separate `room-ktx`-Dependency kann/soll entfernt werden (Suspend/Flow funktioniert ohne sie). (offiziell: https://developer.android.com/jetpack/androidx/releases/room)

### 3.9 TypeConverter — sauber und null-sicher

- **Genau zwei `@TypeConverter`-Methoden pro Typ** (hin/zurueck), Nullables explizit behandeln:
  ```kotlin
  class Converters {
    @TypeConverter fun fromTimestamp(v: Long?): Date? = v?.let { Date(it) }
    @TypeConverter fun dateToTimestamp(d: Date?): Long? = d?.time
  }
  ```
  (offiziell: https://developer.android.com/training/data-storage/room)
- **`@TypeConverters`-Scope so eng wie moeglich.** Am `@Database` registriert gelten sie ueberall; besser auf Entity/DAO/Feld einschraenken, um versehentliche Anwendung zu vermeiden. Konverter sollten stateless sein. (offiziell: https://developer.android.com/training/data-storage/room)

### 3.10 Room-2.7-Driver/Connection-Pool & grosse BLOBs

- **Auf ≥ 2.7.2 erwaegen.** 2.7.2 fixt u. a. einen SQL-Kommentar-Bug (fuehrende `--`-Kommentare liessen ein `SELECT` faelschlich als Schreib-Query laufen), einen KSP-native-Schema-Export-Bug und einen Gradle-Plugin-Crash bei leerem Schema-Verzeichnis. Beide Apps stehen auf 2.7.0 → Bump auf 2.7.2 ist ein Quick-Win. (offiziell: https://developer.android.com/jetpack/androidx/releases/room)
- **Neuer Connection-Pool (2.7):** getrennte Reader/Writer-Verbindungen; bei langer Connection-Wartezeit loggt 2.7.2 nur noch statt `SQLiteException` zu werfen. Wer den Treiber aktiv setzt: `setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)`. Solange die Apps die Support-API nutzen, ist kein Zwang — der Pool greift im Hintergrund. (offiziell: https://developer.android.com/jetpack/androidx/releases/room)
- **Grosse BLOBs NICHT in die DB.** Das `CursorWindow` ist auf ~2 MB pro Row begrenzt; grosse Bilder/Audio fuehren zu `Row too big to fit into CursorWindow`. Stattdessen Datei/URI im Dateisystem speichern, in der DB nur den Pfad. (offiziell: https://developer.android.com/training/data-storage/room)

### 3.11 KSP2-Umstieg (KSP1 → KSP2)

- **Room 2.7 empfiehlt KSP2 fuer Kotlin 2.0+.** Bei Kotlin 2.1.0 ist KSP2 der Zielzustand, aber vorsichtig umstellen: KSP2 hat strengere Nullability-Regeln in der generierten Kotlin-CodeGen (bei KSP ist Kotlin-CodeGen jetzt Default). Nach dem Umstieg Build + alle Migrationstests gruen verifizieren. (offiziell: https://developer.android.com/jetpack/androidx/releases/room)

---

## 4. Korrekter WorkManager-Einsatz (Background-Arbeit, Doze-bewusst, Hilt)

### 4.1 Das richtige Werkzeug waehlen (WorkManager vs. Coroutine vs. Foreground Service)
- **Coroutine/Thread** (lifecycle-gebunden): NUR fuer Arbeit, die ausschliesslich laeuft, solange die App im Vordergrund sichtbar ist (UI-Berechnungen, Feed-Refresh waehrend der User schaut). Ueberlebt KEIN Backgrounding, keinen Prozesstod, keinen Reboot, keine Systemgarantie (offiziell: https://developer.android.com/develop/background-work/background-tasks).
- **WorkManager** (deferrable + garantiert): Default-Wahl fuer fast alle Background-Aufgaben, die Backgrounding/Prozesstod/Reboot ueberleben muessen — z. B. periodischer Server-Sync, Sensor-/Datensammlung, content-getriggerte Uploads. Persistiert auf Disk, chainbar, deferrable (offiziell: https://developer.android.com/develop/background-work/background-tasks).
- **Foreground Service**: NUR fuer sofortige, nutzersichtbare, nicht-deferrable Daueraufgaben (Standort-Tracking, Navigation, Media-Playback). Zwingt eine Notification, ist ressourcenintensiv und aus dem Background heraus stark eingeschraenkt startbar (offiziell: https://developer.android.com/develop/background-work/services/foreground-services).
- **Faustregel der Doku:** „Default choice: WorkManager for most background tasks." Alternativen wie Geofencing/PiP/Companion-Device-Manager VOR einem aequivalenten Foreground Service pruefen (offiziell: https://developer.android.com/develop/background-work/background-tasks).
- **Don't:** Eine Coroutine im ViewModel/Activity-Scope fuer Arbeit verwenden, die garantiert abgeschlossen werden muss — bei Prozesstod ist sie weg.

### 4.2 WorkRequests richtig aufbauen
- **OneTimeWorkRequest** fuer einmalige Arbeit, **PeriodicWorkRequest** fuer wiederkehrende. Minimum-Intervall ist **15 Minuten**; das optionale `flexInterval` ist nur ein Fenster am Ende des Intervalls und gibt KEINE exakte Timing-Garantie (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- **Brauchst du haeufiger als alle 15 Min?** Periodic geht nicht — nimm einen **selbst-re-enqueuenden OneTimeWorkRequest** (am Ende von `doWork()` den naechsten Lauf mit `enqueueUniqueWork` planen). Trotzdem nichts auf exaktes Timing bauen — Doze/App-Standby verschieben die Ausfuehrung.
- **Constraints minimal halten:** Jede Constraint (`NetworkType`, `RequiresCharging`, `BatteryNotLow`, `DeviceIdle`, `StorageNotLow`) verzoegert die Ausfuehrung zusaetzlich. Nur setzen, was wirklich noetig ist. Werden mehrere Constraints gesetzt, laeuft Arbeit erst, wenn ALLE erfuellt sind; wird waehrend des Laufens eine Constraint verletzt, stoppt WorkManager den Worker (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- **Backoff bei transienten Fehlern:** `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)` — exponentiell ergibt ~20s/40s/80s; Minimum 10 s (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

### 4.3 Eindeutige Arbeit & Spec-Aenderungen (KEEP vs. UPDATE)
- **Immer `enqueueUniqueWork()` / `enqueueUniquePeriodicWork()`** mit stabilem `uniqueWorkName` verwenden, um Doppel-Scheduling zu verhindern (z. B. nach jedem App-Start) (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work).
- **`ExistingWorkPolicy` (OneTime):** `REPLACE` (alte abbrechen), `KEEP` (neue ignorieren wenn vorhanden), `APPEND` / `APPEND_OR_REPLACE` (verketten) (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work).
- **`ExistingPeriodicWorkPolicy` fuer Spec-Aenderungen:** Bei geaenderter Spec (neues Intervall/neue Constraints) **`UPDATE` statt `KEEP`/`REPLACE`** nutzen (WM 2.8+). `UPDATE` aendert die Spec, OHNE den naechsten geplanten Lauf zurueckzusetzen — `REPLACE` ist deprecated, weil es die laufende Instanz killt und das Intervall neu startet (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/update-work; extern: https://medium.com/@nicholas.rose/updating-unique-periodic-work-with-workmanager-583009486417).
- **Don't:** Bei jeder Spec-Aenderung `REPLACE` verwenden — verursacht Intervall-Drift und unnoetige Cancel/Re-Enqueue-Zyklen.

### 4.4 CoroutineWorker korrekt schreiben
- In `doWork()` Geschaeftslogik in **try/catch** kapseln. Transiente Fehler (Netzwerk-Timeout) → `Result.retry()` (nutzt die Backoff-Policy); permanente Fehler → `Result.failure()`; Erfolg → `Result.success()` (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- **Kooperatives Stoppen:** `onStopped()` ueberschreiben, um Ressourcen (DB-/File-Handles) freizugeben; in Schleifen regelmaessig `isStopped` pruefen. Worker werden gestoppt bei: explizitem Cancel, `REPLACE`, verletzten Constraints, 10-Minuten-Deadline, System-Anforderung (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work).
- **10-Min-Limit:** Arbeit > ~10 Minuten wird wahrscheinlich unterbrochen — in Subtasks zerlegen oder Long-Running-Worker (4.6) nutzen (offiziell: https://developer.android.com/develop/background-work/background-tasks).

### 4.5 Expedited Work (dringend, aber quota-bewusst)
- `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` verwenden — bei erschoepfter Quota faellt die Arbeit auf normale Ausfuehrung zurueck, statt sie zu verwerfen. **`DROP_WORK_REQUEST` vermeiden** (wuerde die Arbeit canceln) (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- **Pflicht:** Bei `setExpedited` MUSS der CoroutineWorker `getForegroundInfo()` implementieren — fehlt das, gibt es auf aelteren Plattformversionen Runtime-Crashes (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

### 4.6 Long-Running / Foreground-Worker (dataSync, microphone)
- Fuer lange/sofortige Arbeit `setForeground(ForegroundInfo(...))` (CoroutineWorker, suspend) bzw. `setForegroundAsync(...)` (ListenableWorker) aufrufen — WorkManager managt darunter einen Foreground Service mit Notification (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running).
- **`setForeground()` in try/catch** kapseln (`IllegalStateException`, wenn die App gerade nicht in den Vordergrund darf) (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- **Android 14+ (Pflicht):** `foregroundServiceType` fuer `androidx.work.impl.foreground.SystemForegroundService` im Manifest deklarieren UND zur Laufzeit im `ForegroundInfo` mitgeben:
  ```xml
  <service
     android:name="androidx.work.impl.foreground.SystemForegroundService"
     android:foregroundServiceType="dataSync|microphone"
     tools:node="merge" />
  ```
  Laufzeit: `ForegroundInfo(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC or FOREGROUND_SERVICE_TYPE_MICROPHONE)` — plus die passenden `FOREGROUND_SERVICE_*`-Permissions (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running). Fuer EntropieReductor (dataSync/microphone) muessen beide Typen im Manifest UND im ForegroundInfo stehen.
- Cancel-Action per `WorkManager.getInstance(ctx).createCancelPendingIntent(getId())` an die Notification haengen — kein eigener BroadcastReceiver noetig (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running).

### 4.7 Android 16 / targetSdk 36: JobScheduler-Runtime-Quota
- **Neu in Android 16:** Background-Jobs, die aus einem laufenden Foreground Service gestartet werden, muessen jetzt ihre Runtime-Quota einhalten — das gilt auch fuer WorkManager- und DownloadManager-Jobs. Vor Android 16 gab es bei laufendem FGS kein Ausfuehrungslimit. Die Quota haengt vom App-Standby-Bucket ab und davon, ob der Job im Top-State startet (offiziell: https://developer.android.com/about/versions/16/behavior-changes-all; offiziell: https://developer.android.com/topic/performance/power/power-details).
- **Konsequenz:** Lange Long-Running-Worker koennen die App-Quota erschoepfen. Fuer grosse, user-ausgeloeste Transfers **User-Initiated Data Transfer (UIDT)** / `setUserInitiatedJob` nutzen — diese Jobs sind von der normalen Quota ausgenommen; alternativ den Foreground Service direkt starten (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running; offiziell: https://developer.android.com/develop/background-work/services/fgs/changes).
- **`setImportantWhileForeground` ist ab Android 16 ein No-Op** — nicht mehr darauf verlassen (offiziell: https://developer.android.com/about/versions/16/behavior-changes-all).
- Debugging: `JobScheduler.getPendingJobReasons` / `getPendingJobReasonsHistory` zeigen, warum ein Job wartet (offiziell: https://developer.android.com/about/versions/16/behavior-changes-all).

### 4.8 Beobachten & Stop-Reason loggen
- Status reaktiv ueber Flow beobachten: `getWorkInfoByIdFlow(id)` bzw. `getWorkInfosForUniqueWorkFlow(name)` — auf `WorkInfo.State.SUCCEEDED/FAILED` reagieren. Komplexe Abfragen ueber `WorkQuery` (Tags/States/Namen) (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work).
- **`WorkInfo.getStopReason()` loggen** (WM 2.9+, sinnvolle Werte erst ab API 31): liefert einen klaren Hinweis, warum der vorige Lauf gestoppt wurde (Quota, Constraint, Timeout, …). Die StopReason-Konstanten sind in `WorkInfo` gespiegelt — kein direkter Zugriff auf `JobParameters` noetig (extern: https://developer.android.com/jetpack/androidx/releases/work; extern: https://proandroiddev.com/why-has-my-background-worker-stopped-exploring-android-workmangers-stopreason-a0f743e6411c).

### 4.9 Hilt-Integration (@HiltWorker + Configuration.Provider)
- Worker mit `@HiltWorker` annotieren und Konstruktor mit `@AssistedInject`; `Context` und `WorkerParameters` mit `@Assisted` markieren (kommen von WorkManager zur Laufzeit), restliche Abhaengigkeiten injiziert Hilt (offiziell: https://developer.android.com/reference/androidx/hilt/work/HiltWorker):
  ```kotlin
  @HiltWorker
  class SyncWorker @AssistedInject constructor(
      @Assisted appContext: Context,
      @Assisted params: WorkerParameters,
      private val repo: SyncRepository
  ) : CoroutineWorker(appContext, params) { ... }
  ```
- **Application** implementiert `Configuration.Provider`, injiziert `HiltWorkerFactory` und gibt sie via `setWorkerFactory()` weiter (offiziell: https://developer.android.com/training/dependency-injection/hilt-jetpack):
  ```kotlin
  @HiltAndroidApp
  class MyApp : Application(), Configuration.Provider {
      @Inject lateinit var workerFactory: HiltWorkerFactory
      override val workManagerConfiguration: Configuration
          get() = Configuration.Builder()
              .setWorkerFactory(workerFactory)
              .setMinimumLoggingLevel(Log.INFO)
              .build()
  }
  ```
- **Default-Initializer im Manifest entfernen** (sonst nutzt WorkManager die Default-Factory und der `@AssistedInject`-Konstruktor wird nie aufgerufen) (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration):
  ```xml
  <provider
      android:name="androidx.startup.InitializationProvider"
      android:authorities="${applicationId}.androidx-startup"
      android:exported="false"
      tools:node="merge">
      <meta-data
          android:name="androidx.work.WorkManagerInitializer"
          android:value="androidx.startup"
          tools:node="remove" />
  </provider>
  ```
- **Don't:** `getWorkManagerConfiguration()` vergessen → stiller Fehler (Hilt-Factory nie genutzt). **Don't:** parameterloses `WorkManager.getInstance()` verwenden — immer `WorkManager.getInstance(context)` (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration). Abhaengigkeit: `androidx.hilt:hilt-work:1.2.0` + `androidx.hilt:hilt-compiler` (KSP).

### 4.10 Re-Enqueue nach Boot / Force-Stop
- WorkManager re-scheduled persistierte Arbeit nach Reboot selbst. Nach **Force-Stop** durch den User wird jedoch nichts ausgefuehrt, bis die App wieder geoeffnet wird — deshalb **kritische unique-Arbeit beim App-Start idempotent re-enqueuen** (`enqueueUniquePeriodicWork(..., KEEP/UPDATE, ...)`). Niemals auf exaktes Timing bauen — Doze/App-Standby buendeln und verschieben Ausfuehrungen (offiziell: https://developer.android.com/develop/background-work/background-tasks).

### 4.11 Testen
- **Integrationstest:** `WorkManagerTestInitHelper.initializeTestWorkManager(context, config)` mit `Configuration.Builder().setExecutor(SynchronousExecutor())` fuer synchrone Ausfuehrung. Ueber den `TestDriver` Bedingungen simulieren: `setAllConstraintsMet(id)`, `setInitialDelayMet(id)`, `setPeriodDelayMet(id)` (offiziell: https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing).
- **Unit-Test eines einzelnen Workers (ohne WorkManager-Init):** `TestListenableWorkerBuilder` (empfohlen fuer CoroutineWorker) bzw. `TestWorkerBuilder` — testet nur die `doWork()`-Logik. Abhaengigkeit: `androidx.work:work-testing` (offiziell: https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing).

---

## 5. Sauberer Foreground-Service- & Notification-Umgang

### 5.1 Foreground-Service korrekt deklarieren — die drei Dinge gleichzeitig (ab Android 14 Pflicht)

Ein FGS braucht ab Android 14 (API 34) IMMER drei zusammengehoerige Teile — fehlt einer, fliegt eine Exception:

- **DO** — `foregroundServiceType` im Manifest am `<service>`-Element setzen. Mehrere Typen mit `|` kombinieren (offiziell: https://developer.android.com/develop/background-work/services/fgs/service-types)
  ```xml
  <service android:name=".SyncService"
      android:foregroundServiceType="dataSync|microphone"
      android:exported="false" />
  ```
- **DO** — Basis-Permission `FOREGROUND_SERVICE` **plus** die typ-spezifische `FOREGROUND_SERVICE_*` deklarieren. Pro Typ eine: `dataSync`→`FOREGROUND_SERVICE_DATA_SYNC`, `microphone`→`FOREGROUND_SERVICE_MICROPHONE`, `mediaPlayback`→`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `health`→`FOREGROUND_SERVICE_HEALTH`, `location`→`FOREGROUND_SERVICE_LOCATION` (offiziell: https://developer.android.com/develop/background-work/services/fgs/service-types)
- **DO** — beim Aufruf den Typ als Argument an `startForeground()` uebergeben (per `ServiceCompat.startForeground(...)` mit den `ServiceInfo.FOREGROUND_SERVICE_TYPE_*`-Konstanten, mehrere mit `or` verknuepft) (offiziell: https://developer.android.com/develop/background-work/services/fgs)
- **EntropieReductor-Bezug:** `dataSync` (Drive-Sync) braucht KEINE Runtime-Permission; `microphone` braucht zusaetzlich die `RECORD_AUDIO`-Runtime-Permission und ist „while-in-use"-beschraenkt (offiziell: https://developer.android.com/develop/background-work/services/fgs/service-types)
- **DON'T** — `foregroundServiceType` weglassen, Manifest-Typ und `startForeground()`-Typ-Argument unterschiedlich setzen, oder eine while-in-use-Permission (mic/location/camera) annehmen wenn der Service aus dem Hintergrund gestartet wurde (dann `SecurityException`) (offiziell: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)

### 5.2 Die ~5-Sekunden-Regel: startForeground() ganz am Anfang

- **DO** — `ServiceCompat.startForeground(...)` als ALLERERSTES in `onStartCommand()` aufrufen (binnen ~5 s nach `startForegroundService()`), BEVOR irgendeine Arbeit beginnt. Schwere Arbeit erst danach und off-main-thread (Coroutine/Executor) (offiziell: https://developer.android.com/develop/background-work/services/fgs)
- **DON'T** — Notification erst nach Setup/IO bauen, `startForegroundService()` ohne folgendes `startForeground()` lassen (sonst ANR/Crash), `startService()` (legacy) fuer Foreground-Arbeit nutzen
- **DO** — sauber beenden: `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()`. Notification nicht stehen lassen (offiziell: https://developer.android.com/develop/background-work/services/fgs)

### 5.3 FGS-Start aus dem Hintergrund ist grundsaetzlich verboten (ab Android 12) — legalen Pfad nutzen, nicht Funktion entfernen

Verstoss wirft `ForegroundServiceStartNotAllowedException`. Erlaubte Trigger (Auswahl) (offiziell: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start):

- **DO** — Start nur bei: sichtbarer Activity (`onCreate`/`onStart`), Tap auf Notification/Widget/Bubble (PendingIntent), **high-priority FCM** (vorher `remoteMessage.priority == PRIORITY_HIGH` pruefen — System kann downgraden), exact alarm (`USE_EXACT_ALARM`/`SCHEDULE_EXACT_ALARM`), Geofence-/Activity-Recognition-Event, `BOOT_COMPLETED` (mit Android-14-Typ-Einschraenkungen), Companion Device Manager
- **DON'T** — `startForegroundService()` blind aus einem Background-`BroadcastReceiver.onReceive()` aufrufen; die Funktion „weglassen" statt einen erlaubten Trigger zu nehmen. Den Aufruf defensiv in `try/catch (ForegroundServiceStartNotAllowedException)` kapseln und ggf. auf WorkManager ausweichen (offiziell: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- **DON'T** — sich auf `checkSelfPermission()` verlassen, um while-in-use-Zugriff (mic/location) im Hintergrund zu validieren — es liefert irrefuehrend `GRANTED`, der Zugriff ist trotzdem gesperrt (offiziell: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)

### 5.4 FGS-Timeouts (ab Android 15) — onTimeout() respektieren, Arbeit stueckeln

- **dataSync** und **mediaProcessing**: max **6 Stunden** pro 24-Stunden-Fenster (je Typ getrennt gezaehlt). Wird die App in den Vordergrund geholt, **resettet** der Timer (offiziell: https://developer.android.com/develop/background-work/services/fgs/timeout)
- **shortService**: ~**3 Minuten** ab `startForeground()`; kein `START_STICKY`, darf keinen anderen FGS starten, braucht KEINE eigene `FOREGROUND_SERVICE_*` (nur Basis-Permission); bei kombiniertem Typ wird `shortService` ignoriert (offiziell: https://developer.android.com/develop/background-work/services/fgs/service-types)
- **DO** — `Service.onTimeout(int, int)` ueberschreiben und binnen Sekunden `stopSelf()` aufrufen, sonst `RemoteServiceException: did not stop within its timeout` (Crash) (offiziell: https://developer.android.com/develop/background-work/services/fgs/timeout)
  ```kotlin
  override fun onTimeout(startId: Int, fgsType: Int) {
      // Teilstand sichern, dann sofort beenden
      stopSelf()
  }
  ```
- **DO** — lange dataSync-Arbeit in Etappen stueckeln und Fortschritt persistieren, damit ein Resume nach Timeout verlustfrei ist (offiziell: https://developer.android.com/develop/background-work/services/fgs/timeout)

### 5.5 FGS vs. WorkManager-expedited vs. user-initiated-data-transfer — die richtige Wahl

- **FGS** nur fuer **nutzersichtbare, sofortige, laufende** Arbeit, die weiterlaufen muss wenn die App geschlossen wird; volle Kontrolle ueber Notification/Lifecycle, keine Quota (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- **WorkManager-expedited** (`setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)`) fuer **kurze (< wenige Min), wichtige, sofort startende** Arbeit (z. B. Nachricht senden). Hat Background-Quota nach App-Standby-Bucket. Vor Android 12: `getForegroundInfo()` fuer die Notification implementieren (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- **user-initiated data transfer job** als Mittelweg fuer **explizit nutzergestartete** Up-/Downloads mit langlaufender Background-Erlaubnis (offiziell: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- **EntropieReductor-Bezug:** Drive-Sync via FGS aus WorkManager (`setForeground`/`getForegroundInfo`) ist korrekt, solange der Start aus sichtbarer App oder erlaubtem Trigger kommt — sonst expedited work bevorzugen

### 5.6 mediaPlayback / health — Typ-Hinweise

- **mediaPlayback**: keine Runtime-Permission noetig; ab Android 15 kein Start aus `BOOT_COMPLETED`. Alternative fuer Video: Picture-in-Picture (offiziell: https://developer.android.com/develop/background-work/services/fgs/service-types)
- **health**: ab Android 16 (API 36) **granulare** Health-Permissions statt `BODY_SENSORS` (z. B. `READ_HEART_RATE`, fuer Hintergrund `READ_HEALTH_DATA_IN_BACKGROUND`); App muss eine Activity mit Datenschutzerklaerung bereitstellen, sonst Permission-Entzug (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16)

### 5.7 Notification-Channels (Pflicht ab Android 8)

- **DO** — `createNotificationChannel()` IMMER vor dem ersten `notify()` aufrufen (idempotent, kann bei jedem App-Start laufen). Importance bei Erstellung setzen (offiziell: https://developer.android.com/develop/ui/views/notifications/channels)
- **DO** — `NotificationManagerCompat`/`NotificationChannelCompat` und `NotificationCompat.Builder` fuer versionssichere API nutzen (offiziell: https://developer.android.com/develop/ui/views/notifications/channels)
- **DON'T** — ohne Channel posten (schlaegt ab targetSdk 26 still fehl); Importance, Lichter, Vibration oder Channel-Gruppe NACH `createNotificationChannel()` programmatisch aendern wollen — das ist **unveraenderlich**, nur der Nutzer kann es in den System-Settings aendern (offiziell: https://developer.android.com/develop/ui/views/notifications/channels)
- **DO** — fuer FGS einen eigenen, niedrig-prioren Channel (`IMPORTANCE_LOW`) verwenden, damit die laufende Sync-Notification nicht laut ist

### 5.8 POST_NOTIFICATIONS — Runtime-Permission ab Android 13

- **DO** — `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` deklarieren und **im Kontext** anfragen (nach einer Nutzeraktion, nicht beim ersten Start) via `ActivityResultContracts.RequestPermission()` (offiziell: https://developer.android.com/develop/ui/views/notifications/notification-permission)
- **DO** — vor dem Senden `NotificationManagerCompat.from(ctx).areNotificationsEnabled()` pruefen und bei Ablehnung sauber degradieren (offiziell: https://developer.android.com/develop/ui/views/notifications/notification-permission)
- **DON'T** — beim App-Start sofort anfragen; Ablehnung ignorieren. Ein FGS braucht POST_NOTIFICATIONS **nicht** zum Starten — muss aber eine Notification mitliefern (offiziell: https://developer.android.com/develop/ui/views/notifications/notification-permission)

### 5.9 Kein Notification-Trampolin (ab Android 12)

- **DO** — `PendingIntent.getActivity(...)` mit `FLAG_IMMUTABLE` direkt als `setContentIntent(...)` setzen, sodass der Tap die Activity unmittelbar oeffnet (offiziell: https://developer.android.com/about/versions/12/behavior-changes-12)
- **DON'T** — beim Notification-Tap eine Activity aus einem zwischengeschalteten `Service`/`BroadcastReceiver` starten (Trampolin) — wird ab Android 12 blockiert/verzoegert (offiziell: https://developer.android.com/about/versions/12/behavior-changes-12)

### 5.10 USE_FULL_SCREEN_INTENT — nur Alarm/Anruf (ab Android 14)

- **DO** — `USE_FULL_SCREEN_INTENT` nur fuer echte zeitkritische Anwendungsfaelle (Wecker, eingehender Anruf) nutzen. Ab Android 14 nur fuer `ROLE_DIALER`/`ROLE_EMERGENCY` automatisch gewaehrt — sonst vor Nutzung `NotificationManager.canUseFullScreenIntent()` pruefen und ggf. ueber `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` zu den Settings leiten (offiziell: https://developer.android.com/develop/ui/views/notifications/time-sensitive)
- **DO** — **Fallback** auf eine Heads-up-Notification (`IMPORTANCE_HIGH`-Channel + passende `setCategory(CATEGORY_CALL/CATEGORY_ALARM)`), wenn das Recht fehlt (offiziell: https://developer.android.com/develop/ui/views/notifications/time-sensitive)
- **DON'T** — Full-Screen-Intent fuer Werbung/nicht-dringende Inhalte oder ohne `canUseFullScreenIntent()`-Check verwenden (System kann das Recht bei Missbrauch entziehen) (offiziell: https://developer.android.com/develop/ui/views/notifications/time-sensitive)

### 5.11 Android 16 (targetSdk 36) — verschaerfte Punkte

- **DO** — bei FGS-Typ `health` auf granulare Health-Permissions + Datenschutz-Activity migrieren (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16)
- **DO** — neue **progress-centric notifications** fuer langlaufende, fortschrittsbasierte Aufgaben (z. B. Up-/Download) pruefen (offiziell: https://developer.android.com/about/versions/16/features/progress-centric-notifications)
- **Hinweis** — `scheduleAtFixedRate()` fuehrt unter Android 16 hoechstens EINE verpasste Ausfuehrung sofort nach (vorher alle) — relevant fuer periodische Background-Arbeit (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16)

---

## 6. Moderner Runtime-Permission-Flow + PendingIntent/AlarmManager

### 6.1 Moderner Permission-Flow (Activity Result API) — DO's

- **`registerForActivityResult()` FRUEH registrieren** — in der Initialisierungs-Logik der Activity/Fragment, VOR `onStart()`. Niemals in einem Callback oder zur Laufzeit. Vertraege: `ActivityResultContracts.RequestPermission()` (einzeln) und `ActivityResultContracts.RequestMultiplePermissions()` (mehrere, Callback liefert `Map<String, Boolean>`). Ersetzt das deprecated `onRequestPermissionsResult()` + `requestPermissions()` (Request-Codes verwaltet das System automatisch). Benoetigt `androidx.activity:activity ≥ 1.2.0` — bei euch ist `activity-compose 1.9.3` weit darueber. (offiziell: https://developer.android.com/training/permissions/requesting)
- **In Compose**: `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> ... }`, `.launch(...)` aus einem `onClick`/Side-Effect heraus (nicht aus Composable-Scope). (offiziell: https://developer.android.com/develop/ui/views/notifications/notification-permission)
- **3-Stufen-Entscheidungsbaum vor jeder Nutzung** (offiziell: https://developer.android.com/training/permissions/requesting):
  1. `ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED` → API direkt nutzen.
  2. `ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)` → erklaerende Rationale-UI zeigen (mit „Nein danke"-Option), DANN anfragen.
  3. sonst → `launcher.launch(perm)` direkt.
- **JEDES Mal pruefen** — Permission-Status NIE cachen; vor jeder Operation neu mit `checkSelfPermission` pruefen.
- **Jede Permission EINZELN pruefen** — ab Android 13 keine Gruppen-Annahme: Permission-Gruppen „minimieren nur Systemdialoge", garantieren aber keine gemeinsame Gewaehrung. Pro Permission einzeln `checkSelfPermission`. (offiziell: https://developer.android.com/training/permissions/requesting)
- **Permanently-denied erkennen** — ab Android 11 erscheint nach 2× Ablehnen kein Dialog mehr; Signal: `checkSelfPermission` == DENIED UND `shouldShowRequestPermissionRationale` == `false`. Dann NICHT erneut `launch()` (no-op), sondern per `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` in die App-Einstellungen leiten. (offiziell: https://developer.android.com/training/permissions/requesting)
- **Inkrementell anfragen** — nur die Permission anfragen, die fuer das gerade genutzte Feature noetig ist, im Kontext der Nutzung. Background-Location (`ACCESS_BACKGROUND_LOCATION`, ab Android 10) IMMER separat NACH der Foreground-Location anfragen — niemals buendeln. (offiziell: https://developer.android.com/training/permissions/requesting)
- **Minimal-Permission-Prinzip** — nur deklarieren/anfragen, was wirklich gebraucht wird; bei Bedarf ab Android 13 ungenutzte Permissions per `revokeSelfPermissionOnKill(perm)` / `revokeSelfPermissionsOnKill(list)` zurueckgeben.

### 6.2 Granulare Media-Permissions / Photo Picker

- **BESTE Wahl: Photo Picker (KEINE Permission noetig)** — `ActivityResultContracts.PickVisualMedia` (einzeln) / `PickMultipleVisualMedia` (mehrere). Erfordert KEIN `READ_MEDIA_*`. Filter: `PickVisualMedia.ImageOnly` / `VideoOnly` / `ImageAndVideo` / `SingleMimeType("image/gif")`. Verfuegbarkeit pruefen via `ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)`; fehlt der Picker, faellt die Lib automatisch auf `ACTION_OPEN_DOCUMENT` zurueck. Benoetigt `androidx.activity ≥ 1.7.0` (bei euch 1.9.3 ✓). Fuer dauerhaften Zugriff `takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)`. (offiziell: https://developer.android.com/training/data-storage/shared/photopicker)
- **Nur falls eigener Gallery-Picker noetig** — ab Android 13 (`TIRAMISU`) granular `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` statt `READ_EXTERNAL_STORAGE`; ab Android 14 (`UPSIDE_DOWN_CAKE`) zusaetzlich `READ_MEDIA_VISUAL_USER_SELECTED` fuer „Ausgewaehlte Fotos" (Teilzugriff). Diese Media-Permissions in EINEM `RequestMultiplePermissions`-Aufruf buendeln (Ausnahme zur Inkrementell-Regel — hier sinnvoll, weil 1 Dialog). Status-Check: Vollzugriff = `READ_MEDIA_IMAGES` ODER `READ_MEDIA_VIDEO` granted; Teilzugriff (A14+) = `READ_MEDIA_VISUAL_USER_SELECTED` granted. (offiziell: https://developer.android.com/about/versions/14/changes/partial-photo-video-access)
- **Don't**: `READ_MEDIA_VISUAL_USER_SELECTED` weglassen, wenn man `READ_MEDIA_IMAGES/VIDEO` nutzt → System gewaehrt dann nur temporaeren, session-basierten Zugriff (verfaellt im Hintergrund, Nutzer muss jedes Mal neu auswaehlen). URI-Zugriff als temporaer behandeln, in `onResume()` auffrischen. (offiziell: https://developer.android.com/about/versions/14/changes/partial-photo-video-access)

### 6.3 POST_NOTIFICATIONS (fuer BestJournal-Reminder relevant)

- Manifest: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`. Ab Android 13 (targetSdk ≥ 33, bei euch 36) sind Benachrichtigungen bei Neuinstallation **standardmaessig AUS**; die App steuert selbst WANN der Dialog erscheint. (offiziell: https://developer.android.com/develop/ui/views/notifications/notification-permission)
- **Im Kontext anfragen** — nicht beim ersten Start, sondern wenn der Nutzer ein benachrichtigungs-relevantes Feature aktiviert (z. B. „Reminder einschalten"-Button). Vor dem Senden `NotificationManager.areNotificationsEnabled()` pruefen.
- Versionsbewusst: nur ab `Build.VERSION_CODES.TIRAMISU` `launch(POST_NOTIFICATIONS)`; auf Android 12L und aelter gibt es die Runtime-Permission nicht (Benachrichtigungen sind nach Channel-Erstellung implizit erlaubt). (offiziell: https://developer.android.com/develop/ui/views/notifications/notification-permission)

### 6.4 Package Visibility (`<queries>`, ab Android 11)

- Ab API 30 sehen Apps standardmaessig NICHT mehr alle installierten Pakete (`queryIntentActivities()`, `getPackageInfo()`, `getInstalledApplications()` gefiltert). Sichtbarkeit gezielt deklarieren via `<queries>`: per `<package android:name="...">`, per `<intent>`-Filter, oder per `<provider android:authorities="...">`. (offiziell: https://developer.android.com/training/package-visibility)
- **Don't**: `QUERY_ALL_PACKAGES` verwenden — nur in seltenen Faellen erlaubt, auf Google Play genehmigungspflichtig (Liste installierter Apps = sensible Nutzerdaten). (offiziell: https://developer.android.com/training/package-visibility)

### 6.5 Compose: Accompanist Permissions (der einzige verbliebene Accompanist-Rest)

- `rememberPermissionState(permission)` (einzeln) / `rememberMultiplePermissionsState(permissions)` (mehrere). Status ueber `.status.isGranted` und `.status.shouldShowRationale`. `PermissionState.launchPermissionRequest()` NUR aus Nicht-Composable-Scope (z. B. `Button onClick` oder Side-Effect) aufrufen. (offiziell: https://google.github.io/accompanist/permissions/)
- **Achtung**: `@ExperimentalPermissionsApi` (Opt-in noetig, API kann sich aendern). Fuer einfache Faelle reicht oft das native `rememberLauncherForActivityResult` ohne Extra-Lib.

### 6.6 Android 16 (targetSdk 36) — Permission-Verschaerfungen (EntropieReductor: Health/Bluetooth)

- **BODY_SENSORS → granulare Health-Permissions (PFLICHT ab targetSdk 36)** — `BODY_SENSORS` / `BODY_SENSORS_BACKGROUND` werden durch `android.permissions.health`-Permissions ersetzt: z. B. `READ_HEART_RATE` (statt `BODY_SENSORS`) fuer While-in-use Herzfrequenz/SpO2/Hauttemperatur; `READ_HEALTH_DATA_IN_BACKGROUND` (statt `BODY_SENSORS_BACKGROUND`) fuer Hintergrund-Sensorzugriff. Betrifft `Sensor.TYPE_HEART_RATE`, Health Services `HEART_RATE_BPM`, `FOREGROUND_SERVICE_TYPE_HEALTH`. **Mobile-Apps MUESSEN eine Activity zur Anzeige der Datenschutzerklaerung deklarieren — sonst wird die Permission widerrufen.** (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16)
- **Local-Network-Zugriff (opt-in)** — neue `NEARBY_WIFI_DEVICES`-relevante Einschraenkung fuer lokale Netzwerk-Geraete (fuer Maps/Geraete-Discovery relevant testen). (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16)

### 6.7 PendingIntent — DO's & Don'ts

- **IMMER ein Mutability-Flag setzen** (ab Android 12 / targetSdk 31 Pflicht, sonst Crash): `FLAG_IMMUTABLE` als sicherer Default. `FLAG_MUTABLE` NUR wenn das System den Intent fuellen muss (z. B. direct-reply/Bubbles/inline-Notifications). (offiziell: https://developer.android.com/develop/background-work/services/alarms/schedule)
- **`FLAG_UPDATE_CURRENT`** kombinieren, um bestehende PendingIntents mit gleichem Request-Code zu aktualisieren statt zu duplizieren: `PendingIntent.getBroadcast(ctx, id, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`.
- **Alarm abfragen/loeschen** ohne neuen anzulegen: `FLAG_NO_CREATE or FLAG_IMMUTABLE` → liefert `null` wenn keiner existiert; dann `alarmMgr.cancel(pendingIntent)`. **Don't**: `FLAG_ONE_SHOT`-Alarme lassen sich NICHT canceln. (offiziell: https://developer.android.com/develop/background-work/services/alarms/schedule)

### 6.8 AlarmManager — DO's & Don'ts (BestJournal-Reminder)

- **WorkManager bevorzugen** — fuer planbare Hintergrundarbeit, Daten-Sync, Reboot-ueberlebende Arbeit, flexible Zeiten (min. 15-min-Intervalle). WorkManager respektiert Doze/Battery-Saver und reschedult nach Reboot automatisch. AlarmManager nur fuer ECHTE uhrzeitgenaue Wecker. (offiziell: https://developer.android.com/develop/background-work/services/alarms/schedule)
- **Default = inexakte Alarme** — `set()`, `setWindow()` (≥10 min Fenster ab A12), `setInexactRepeating()`; bei Doze-Ueberlebenspflicht fuer nutzerinitiierte Aktionen `setAndAllowWhileIdle()`.
- **Exakte Alarme NUR bei Kernfunktion** (Wecker/Kalender-Termin): `setExactAndAllowWhileIdle()` oder `setAlarmClock()` (hoechste Prioritaet, weckt Geraet). Ab Android 12 VOR jedem exakten Alarm `if (alarmMgr.canScheduleExactAlarms())` pruefen.
- **Permission-Wahl fuer exakte Alarme** (offiziell: https://developer.android.com/develop/background-work/services/alarms/schedule):
  - `SCHEDULE_EXACT_ALARM` — Nutzer gewaehrt/widerruft in Settings; ab Android 14 bei Neuinstallation NICHT vorgewaehrt. Bei Bedarf via `Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)` zu den Einstellungen leiten; auf den Broadcast `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` lauschen und dann neu planen.
  - `USE_EXACT_ALARM` — automatisch gewaehrt, nicht widerrufbar, ABER laut Google-Play-Policy NUR fuer echte Wecker-/Kalender-Apps zulaessig. Fuer BestJournal-Tagebuch-Reminder NICHT geeignet → `SCHEDULE_EXACT_ALARM`-Flow nutzen oder besser inexakt/WorkManager.
- **Permission-freie Alternative**: `setExact(type, time, "tag", OnAlarmListener, handler)` benoetigt KEIN `SCHEDULE_EXACT_ALARM` (laeuft nur solange Prozess lebt — fuer In-App-Timing geeignet).
- **Nach Reboot neu planen** — Alarme werden beim Shutdown geloescht. `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>` + `BroadcastReceiver` auf `BOOT_COMPLETED`, der Alarme neu setzt. Receiver `android:enabled="false"` im Manifest und erst per `setComponentEnabledSetting(...ENABLED, DONT_KILL_APP)` aktivieren, wenn der Nutzer einen Alarm setzt (spart Boot-Last). (offiziell: https://developer.android.com/develop/background-work/services/alarms/schedule)
- **Don't**: kein wall-clock-`RTC` wo `ELAPSED_REALTIME` reicht; keine haeufigen Wakeups; bei wiederholenden Alarmen mit Netzaufrufen Jitter einbauen.

---

## 7. Scoped Storage + targetSdk-36-Compliance (Android 15/16 sauber migrieren)

> Versions-Anker: targetSdk=compileSdk=36 (Android 16), minSdk 26/28, Jetpack Compose + Material3, activity-compose 1.9.3. Gilt fuer BestJournalAndroid (sherpa-onnx native `.so` → 16-KB-Page-Size!) und EntropieReductor. Beide nutzen FileProvider + grosse Bildschirme (Fold/Tablet).

### 7.1 Storage — der richtige Entscheidungsbaum (Scoped Storage seit Android 10 Default)

- **App-eigene Dateien (kein Permission noetig, seit API 19):** `context.filesDir` / `context.cacheDir` (intern, privat) bzw. `context.getExternalFilesDir(...)` / `getExternalCacheDir()` (extern, app-privat). Fuer alle App-internen Daten — DB-Backups, JSON-Exporte, temporaere Audio-Chunks, Caches. **Kein** `READ/WRITE_EXTERNAL_STORAGE` deklarieren (offiziell: https://developer.android.com/training/data-storage).
- **Geteilte Medien (Bilder/Audio/Video) der eigenen App → `MediaStore`.** Eigene Eintraege brauchen keine Permission; nur fuer den Zugriff auf Medien FREMDER Apps `READ_EXTERNAL_STORAGE` (≤ API 32) bzw. granular `READ_MEDIA_IMAGES/_VIDEO/_AUDIO` (API 33+) (offiziell: https://developer.android.com/training/data-storage).
- **Beliebige Nutzer-Dokumente (Export/Import an frei waehlbaren Ort) → Storage Access Framework (SAF):** `Intent(ACTION_CREATE_DOCUMENT)` zum Speichern, `ACTION_OPEN_DOCUMENT` zum Lesen. Der System-Picker erteilt Zugriff — **null Permissions** noetig. Genau das ist der saubere Weg fuer „Journal als PDF/JSON exportieren" statt `MANAGE_EXTERNAL_STORAGE` (offiziell: https://developer.android.com/training/data-storage).
- **SAF-Zugriff persistieren:** Per Default endet der URI-Zugriff bei Neustart/App-Stop. Fuer dauerhaften Zugriff (z. B. wiederkehrendes Auto-Backup in einen vom Nutzer gewaehlten Ordner): `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` (offiziell: https://developer.android.com/training/data-storage/shared/photopicker). Limit: 5000 Grants pro App, aelteste fallen raus.
- **Photo Picker fuer Bildauswahl (statt READ_MEDIA_IMAGES):** Jetpack `ActivityResultContracts.PickVisualMedia` (einzeln) / `PickMultipleVisualMedia(maxItems)` (mehrere) ab `androidx.activity` 1.7.0+. **Kein Permission noetig**, Nutzer gibt nur ausgewaehlte Dateien frei. Faellt auf `ACTION_OPEN_DOCUMENT` zurueck, wenn Picker fehlt. Launch: `pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))`. Auf alten Geraeten (API 19–29) via Play-services-Backport (`ModuleDependencies`-Service im Manifest) (offiziell: https://developer.android.com/training/data-storage/shared/photopicker).

### 7.2 Permissions — Do's & Don'ts

- **Don't:** `WRITE_EXTERNAL_STORAGE` deklarieren oder erwarten dass es wirkt — ab **API 30 No-Op** (komplett wirkungslos); das System nutzt zweck-basierten statt orts-basierten Zugriff (offiziell: https://developer.android.com/training/data-storage).
- **Don't:** `MANAGE_EXTERNAL_STORAGE` („All files access") verwenden — Play-Policy erlaubt das nur fuer echte Dateimanager/Backup-Apps; sonst Store-Ablehnung. Scoped Storage + SAF + Photo Picker decken praktisch alle legitimen Faelle ab. Der Berechtigungs-Dialog zeigt dem Nutzer „breiter Zugriff" — abschreckend und unnoetig (offiziell: https://developer.android.com/training/data-storage).
- **Do:** Granulare Media-Permissions (`READ_MEDIA_*`) nur wenn die App wirklich auf Medien FREMDER Apps zugreift — fuer eigene Inhalte gar nichts.

### 7.3 FileProvider statt `file://` (Pflicht seit Android 7 / API 24)

- **Do:** Dateien NUR ueber `content://`-URIs teilen, generiert via `FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)`. `file://`-URIs werfen seit API 24 (StrictMode) `FileUriExposedException` beim Weitergeben ueber Prozessgrenzen (Intent/Share) (offiziell: https://developer.android.com/reference/androidx/core/content/FileProvider).
- **Manifest-Provider korrekt deklarieren:**
  ```xml
  <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="${applicationId}.fileprovider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
                 android:resource="@xml/file_paths" />
  </provider>
  ```
  `android:exported="false"` (Sicherheit) + `android:grantUriPermissions="true"` sind Pflicht. `res/xml/file_paths.xml` definiert die freigegebenen Verzeichnisse (`<cache-path>`, `<files-path>`, `<external-files-path>` etc.) (offiziell: https://developer.android.com/reference/androidx/core/content/FileProvider).
- **Beim Teilen IMMER Flag setzen:** `intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)` und URI per `putExtra(Intent.EXTRA_STREAM, contentUri)`. Sonst kann der Empfaenger (z. B. Mail-App) die Datei nicht lesen (offiziell: https://developer.android.com/reference/androidx/core/content/FileProvider).

### 7.4 Edge-to-Edge (erzwungen Android 15, Opt-out entfaellt Android 16)

- **Android 15 (targetSdk 35):** Edge-to-Edge wird erzwungen — Status-/Navbar transparent, Content zeichnet dahinter. **Android 16 (targetSdk 36):** `R.attr#windowOptOutEdgeToEdgeEnforcement` ist deprecated UND disabled — **kein Opt-out mehr moeglich** (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16).
- **Do (Compose, sauber):** In `onCreate()` **vor** `setContent` `enableEdgeToEdge()` aufrufen, dann Insets ueber Material3-`Scaffold` mit `contentWindowInsets = WindowInsets.safeDrawing` behandeln und das gelieferte `innerPadding` aufs Content anwenden. Fuer Einzelfaelle: `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`, `Modifier.systemBarsPadding()`, `Modifier.imePadding()` (Tastatur). `WindowInsets.safeContent` wenn Touch-/Gestenbereiche relevant sind (Bottom Sheets, Carousels) (offiziell: https://developer.android.com/develop/ui/compose/layouts/insets).
- **Don'ts (deprecated UND disabled ab API 35):** `Window.setStatusBarColor()`, `setNavigationBarColor()` (Gesten-Nav), `statusBarColor`-Attr, `setDecorFitsSystemWindows()`, `setNavigationBarDividerColor()` — wirkungslos, nicht mehr verwenden (offiziell: https://developer.android.com/about/versions/15/behavior-changes-15).
- **Icon-Helligkeit (heller/dunkler Bar-Hintergrund):** Status-/Nav-Icons via `WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = true/false` steuern — nicht ueber Bar-Farben (offiziell: https://developer.android.com/develop/ui/compose/layouts/insets).
- **Don't hardcoden:** Keine festen Padding-Werte fuer Bars/Tastatur — Insets sind dynamisch (Tastatur-Animation, Rotation, Fold).

### 7.5 Predictive Back (erzwungen Android 16 / targetSdk 36)

- **Android 16:** System-Back-Animationen (back-to-home, cross-task, cross-activity) sind standardmaessig an. **`onBackPressed()` wird NICHT mehr aufgerufen**, `KeyEvent.KEYCODE_BACK` NICHT mehr dispatcht (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16).
- **Do:** `android:enableOnBackInvokedCallback="true"` im `<application>` (oder pro `<activity>`) setzen (offiziell: https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture).
- **Compose:** `BackHandler(enabled) { ... }` fuer einfaches Abfangen; `PredictiveBackHandler(enabled) { progress -> ... }` mit `Flow<BackEventCompat>` fuer animierte Fortschritts-Anbindung (CancellationException bei Abbruch behandeln) (offiziell: https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture).
- **Views/AndroidX:** `onBackPressedDispatcher.addCallback(this, OnBackPressedCallback(enabled) { handleOnBackPressed() })`. Callback nur aktivieren wenn noetig (z. B. `enabled = hasUnsavedData`) — Single-Responsibility, ueber observable State (StateFlow/Compose State) togglen (offiziell: https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture).
- **Don'ts:** `onBackPressed()` ueberschreiben, `KEYCODE_BACK` abfangen — beides nicht mehr unterstuetzt.

### 7.6 Grosse Screens / adaptive Layouts (Android 16, ≥600dp)

- **Android 16 ignoriert ab smallestWidth 600dp:** `android:screenOrientation`, `android:resizableActivity`, `android:minAspectRatio`, `android:maxAspectRatio` sowie `setRequestedOrientation()`/`getRequestedOrientation()` — die App wird auf Tablet/Fold frei resizable, egal was im Manifest steht (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16).
- **Do:** Adaptiv per `currentWindowAdaptiveInfo().windowSizeClass` (aus `androidx.compose.material3.adaptive`) bauen — Breakpoints COMPACT (<600dp) / MEDIUM (600–840dp) / EXPANDED (≥840dp). Layout-Entscheidungen an die verfuegbare Fensterbreite koppeln, NICHT an „ist Tablet" oder Orientierung (Split-Screen/Fold/ChromeOS aendern die Fensterbreite zur Laufzeit) (offiziell: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes).
- **State robust retten:** Bei Resize/Fold/Rotation wird die Activity neu konfiguriert — UI-State ueber `rememberSaveable` / ViewModel halten, damit nichts verloren geht.
- **Temporaeres Opt-out (laeuft mit API 37 aus, nicht als Dauerloesung):** `<property android:name="android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY" android:value="true" />`. Ausnahmen greifen automatisch fuer Games (`android:appCategory`) (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16).

### 7.7 16-KB-Page-Size (Play-Pflicht ab 01.11.2025 fuer targetSdk 35+ mit nativem Code)

> **BestJournalAndroid ist direkt betroffen (sherpa-onnx `.so`).** EntropieReductor nur, falls native Libs dazukommen — reine Kotlin/Java-Apps sind automatisch kompatibel.

- **Pflicht:** Alle neuen Apps/Updates mit targetSdk 35+ muessen auf 64-bit-Geraeten 16-KB-Page-Size unterstuetzen (offiziell: https://developer.android.com/guide/practices/page-sizes).
- **Build-Tools:** AGP **8.5.1+** und NDK **r28+** (r28 kompiliert 16-KB-aligned by default). Bei NDK r27/aelter Linker-Flags setzen: `-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384` (CMake: `target_link_options(... PRIVATE "-Wl,-z,max-page-size=16384" "-Wl,-z,common-page-size=16384")`) (offiziell: https://developer.android.com/guide/practices/page-sizes).
- **Gradle:** `packaging { jniLibs { useLegacyPackaging = false } }` (= `android:extractNativeLibs="false"`, ab AGP 8.5.1 Pflicht fuer die Ausrichtung) (offiziell: https://developer.android.com/guide/practices/page-sizes).
- **Code-Annahmen entfernen:** Nie `4096` oder `PAGE_SIZE` hardcoden (PAGE_SIZE ist in NDK r27+ im 16-KB-Modus undefiniert) — stattdessen `getpagesize()` bzw. `sysconf(_SC_PAGESIZE)`; `mmap()`-Groessen nicht 4-KB-aligned annehmen (offiziell: https://developer.android.com/guide/practices/page-sizes).
- **Verifizieren/Testen:** `zipalign -v -c -P 16 4 app.apk` zur Pruefung der Ausrichtung; testen im Emulator-Image „Google APIs Experimental 16 KB Page Size" oder Pixel 8+ mit Dev-Option „Boot with 16KB page size". Pruefen: `adb shell getconf PAGE_SIZE` → muss `16384` liefern (offiziell: https://developer.android.com/guide/practices/page-sizes).
- **Drittanbieter-SDKs:** Vor dem Build pruefen, dass alle nativen Dependencies (auch sherpa-onnx-Build) in 16-KB-kompatibler Version vorliegen.

### 7.8 Weitere targetSdk-35/36-Stolpersteine (sauber loesen)

- **Configuration enthaelt jetzt System-Bars (Android 15):** `Configuration.screenWidthDp`/`screenHeightDp` und `Display.getSize()` schliessen ab API 35 die System-Bars EIN (vorher exklusive). Fuer korrekte verfuegbare Groesse `WindowMetricsCalculator` bzw. `WindowInsets`/`ViewGroup` nutzen, nicht `Configuration` zur Layout-Berechnung (offiziell: https://developer.android.com/about/versions/15/behavior-changes-15).
- **Safer Intents (Android 16, opt-in):** Explizite Intents muessen zum Intent-Filter der Ziel-Komponente passen; Intents ohne Action matchen keinen Filter. Aktivieren mit `android:intentMatchingFlags="enforceIntentFilter"` — beim Migrieren Logcat auf `PackageManager`-Meldungen „Intent does not match component's intent filter" pruefen (offiziell: https://developer.android.com/about/versions/16/behavior-changes-16).
- **TLS 1.0/1.1 verboten (Android 15):** Netzwerk-Code auf **TLS 1.2+** stellen; Cleartext-Traffic per Network Security Config / `android:usesCleartextTraffic="false"` ausschliessen (offiziell: https://developer.android.com/about/versions/15/behavior-changes-15).
- **PendingIntent-Background-Launches blockiert (Android 15):** PendingIntent-Ersteller blockieren Hintergrund-Activity-Starts per Default — relevant fuer Notification-Taps; auf den normalen Activity-Launch-Pfad bauen, nicht auf Background-Launch (offiziell: https://developer.android.com/about/versions/15/behavior-changes-15).

### 7.9 Kurz-Checkliste fuer targetSdk-36-Migration

1. `enableEdgeToEdge()` + `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)`, alte Bar-Color-APIs raus.
2. `android:enableOnBackInvokedCallback="true"` + `BackHandler`/`OnBackPressedCallback`, kein `onBackPressed()`.
3. Adaptive Layouts via `WindowSizeClass`, keine Orientierungs-Locks fuer ≥600dp; State via `rememberSaveable`/ViewModel.
4. Nativ? → AGP 8.5.1+, NDK r28+, `useLegacyPackaging=false`, `zipalign -P 16` gruen, im 16-KB-Emulator getestet.
5. Storage: nur `filesDir`/`getExternalFilesDir`/SAF/Photo Picker; kein `WRITE_/MANAGE_EXTERNAL_STORAGE`; Teilen ausschliesslich via FileProvider + `content://` + `FLAG_GRANT_READ_URI_PERMISSION`.
6. TLS 1.2+, kein Cleartext; `Configuration`-Groessen nicht fuers Layout missbrauchen.

---

## 8. Gesten am echten Geraet per adb pruefen (Wischen, langer Druck, Ziehen)

### 8.1 `input swipe` erzeugt KEINEN langen Druck
`adb shell input swipe x1 y1 x2 y2 dauer` sendet DOWN und bewegt den Zeiger **sofort** weiter.
`detectDragGesturesAfterLongPress` (Compose) verlangt aber, dass der Finger ~500 ms **still** liegt,
bevor er sich bewegt — der Wisch wird deshalb als Scrollen gewertet und Drag-and-Drop laesst sich so
nie verifizieren. `adb shell input draganddrop` haelt in vielen ROMs ebenfalls nicht lange genug.

**Loesung — Geste aus Einzel-Ereignissen bauen** (Android 11+):
```
adb shell input motionevent DOWN 400 1327
# 1,3 s warten, damit der lange Druck ausloest
adb shell input motionevent MOVE 400 1250
adb shell input motionevent MOVE 400 1100
adb shell input motionevent UP   400  991
```
Die einzelnen `input motionevent`-Aufrufe gehoeren zur selben Geste, der Zeiger bleibt zwischen den
Aufrufen unten. Zwischen den MOVE-Schritten ~120-150 ms lassen, sonst springt die Liste.

### 8.2 Wisch-Gesten nicht am Bildschirmrand starten
Ein `input swipe`, der naeher als ~100 px am rechten/linken Rand beginnt, wird von der
Gesten-Navigation als **Zurueck** gewertet — die App verlaesst den Bildschirm, statt zu wischen.
Startpunkt in die Bildschirmmitte legen.

### 8.3 `screencap` auf Foldables braucht die Display-ID
`adb exec-out screencap -p > datei.png` schreibt auf Mehrschirm-Geraeten eine **Warnung nach stdout**
und zerstoert damit die PNG-Datei. Stattdessen auf dem Geraet ablegen und ziehen:
`adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png` (oder `-d <display-id>` mitgeben,
IDs via `dumpsys SurfaceFlinger --display-id`).

### 8.4 Am fremden Datenbestand nur mit Rueckweg testen
Wer Gesten am Geraet mit echten Daten prueft, aendert echte Daten. Vorher den Ist-Zustand per
Screenshot festhalten und nach dem Test Schritt fuer Schritt zurueckstellen — oder auf einem
Testprofil arbeiten.

**Stand:** 14.08.2026 · geprueft an Android 16 (Galaxy Z Fold 8), Compose Foundation 1.9.

## ✅ Pflicht-Checkliste vor Framework-/Runtime-Arbeit (Best-Practice-Seite)

- [ ] **Architektur:** 3 Layer, Abhaengigkeiten nach unten, UDF, immutable `UiState` via `StateFlow`+`stateIn(WhileSubscribed(5000))`, `collectAsStateWithLifecycle`, kein Context/Logik im ViewModel? (§1)
- [ ] **Lifecycle:** Flows mit `repeatOnLifecycle(STARTED)`/`collectAsStateWithLifecycle` (Fragment: `viewLifecycleOwner`)? Jedes `registerX` ↔ `unregisterX`? `registerForActivityResult` frueh? Process-Death via `SavedStateHandle`/Persistenz? (§2)
- [ ] **Room:** version-Bump ⇒ `@AutoMigration`/`Migration` geschrieben + getestet (`MigrationTestHelper`)? Kein `fallbackToDestructiveMigration` in Prod? `exportSchema`+Schema-JSONs eingecheckt? **WAL-Checkpoint vor jedem Backup/Drive-Upload**? DB als Hilt-`@Singleton`? DAO `suspend`/`Flow`? Room ≥ 2.7.2 erwogen? (§3)
- [ ] **WorkManager:** richtiges Werkzeug (WM vs. Coroutine vs. FGS)? `enqueueUniqueWork` + `ExistingPeriodicWorkPolicy.UPDATE`? Expedited mit `RUN_AS_NON_EXPEDITED_WORK_REQUEST` + `getForegroundInfo()`? `@HiltWorker`+`Configuration.Provider`+Default-Initializer entfernt? `getStopReason()` geloggt? Re-Enqueue nach Boot/force-stop? (§4)
- [ ] **FGS:** `foregroundServiceType` (Manifest, auch fuer `SystemForegroundService`) + `FOREGROUND_SERVICE_*`-Permission + Typ-Arg in `startForeground()` — alle drei? `startForeground()` sofort (~5 s)? `onTimeout()`→`stopSelf()` (dataSync 6 h / shortService 3 min)? Start nur aus erlaubtem Trigger? (§5)
- [ ] **Notifications:** Channel vor `notify()` (ab API 8)? `POST_NOTIFICATIONS` im Kontext (ab API 13)? Kein Trampolin (PendingIntent.getActivity direkt)? Full-Screen-Intent nur Alarm/Anruf + Fallback? (§5)
- [ ] **Permissions:** moderner `RequestPermission`-Flow (frueh registriert)? jede Permission einzeln + permanently-denied erkannt? inkrementell (Background-Location separat)? Photo Picker statt `READ_MEDIA_*`? `<queries>` deklariert? (§6)
- [ ] **PendingIntent/Alarm:** `FLAG_IMMUTABLE`(+`UPDATE_CURRENT`)? AlarmManager nur fuer echte Uhrzeit, `canScheduleExactAlarms()` geprueft, `USE_EXACT_ALARM` nur Wecker/Kalender? Re-schedule nach Boot? (§6)
- [ ] **targetSdk 36:** `enableEdgeToEdge()`+Insets? `OnBackPressedCallback`/`BackHandler` (kein `onBackPressed()`)? adaptive Layout (`WindowSizeClass`) ≥600dp? native `.so` 16-KB-aligned? Storage nur Sandbox/MediaStore/SAF/Photo Picker + FileProvider? TLS 1.2+? (§7)
