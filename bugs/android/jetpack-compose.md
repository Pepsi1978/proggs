# Bekannte Bugs & Fallen: Jetpack Compose (Android-UI)

> **PFLICHT-LESEN vor JEDER Arbeit an Compose-UI** (`.kt` mit `@Composable`/`setContent`,
> Screens, ViewModels-zu-UI-Grenze, Navigation-Compose, Material3, Lazy-Layouts, Animation).
> Kuratiert aus offizieller Doku (developer.android.com), AOSP issuetracker, Compose-Release-Notes,
> Google-Blogs und Community (Stack Overflow, Medium, Reddit) + eigenen Vorfaellen.
> Loesungen sind **funktionserhaltend** — nie "Composable/Feature weglassen" (Direktive #3).
>
> **Zweite Seite der Medaille (Praevention):** Wie man diese Bugs von vornherein vermeidet, steht in
> [`best-practices/android/jetpack-compose.md`](../../best-practices/android/jetpack-compose.md)
> — Bezugstabelle „Bug-Abschnitt ↔ Best-Practice" ganz unten.
>
> **Stand:** recherchiert am **2026-06-02** (7-Researcher-Schwarm), **re-recherchiert am 2026-06-24**
> (Engine A: Firecrawl+MiniMax) fuer die real genutzten Versionen:
> **Anker:** compose-bom=2025.01.01  <!-- maschinenlesbar fuer check-version-anchor.py -->
> - **BestJournalAndroid:** Compose **BOM 2025.01.01** (UI ~1.7.6, Material3 1.3.1), Kotlin 2.1.0,
>   Compose-Compiler-Plugin 2.1.0, navigation-compose 2.8.7, lifecycle 2.8.7, AGP 8.7.3.
> - **EntropieReductor:** Compose **BOM 2026.03.00** (UI ~1.10.x, Material3 1.4.0), Kotlin 2.1.0,
>   Compose-Compiler-Plugin 2.1.0, navigation-compose 2.8.7, AGP 8.10.0.
> - Abgedeckte Spanne: **Compose 1.6 → 1.10**, Material3 1.2 → 1.4. **Strong Skipping** ist seit
>   Compose-Compiler 2.0.20 / Compose 1.7 default aktiv (gilt fuer beide Projekte).
>
> **Ergaenzt 2026-07-02:** §6.8 (`weight(0f)`-Crash aus dynamischen Daten) und §9.6
> (unbedingt erzeugte InfiniteTransition tickt in jedem Zustand — Energie) — CortexAndroid-Funde.
> **Ergaenzt 2026-07-20:** §8.8 (gewichteter Langtext-Editor kollabiert bei `adjustResize` + IME).
>
> **Versions-Horizont (Re-Recherche 2026-06-24):** BOM ist inzwischen bei **2026.06.00** (2026-06-17):
> Compose UI/foundation/animation/runtime **1.11.3**, **Material3 1.4.0** (stabil), **1.12.0-beta01** in
> Arbeit; **Material3 1.5.x noch Alpha** (1.5.0-alpha22). **navigation-compose 2.9.x** ist verfuegbar.
> EntropieReductor (Material3 1.4.0) ist von den 1.4.0-Breaking-Changes in §8.7 direkt betroffen.

---

## Abgrenzung — was hier steht und was woanders (KRITISCH)

Dieser Almanach ist die **tiefe, vollstaendige** Quelle fuer **Compose-UI-Bugs**. Andere Bereiche:

| Gehoert HIERHER (jetpack-compose.md) | Gehoert NICHT hierher |
|--------------------------------------|------------------------|
| Recomposition/Skippability/Stabilitaet, `remember`/`rememberSaveable`/`derivedStateOf`, Side-Effects (`LaunchedEffect`/`DisposableEffect`/`snapshotFlow`), Lazy-Layouts/Pager, Modifier-Reihenfolge, Compose-Crashes, navigation-compose, Material3-Composables, Compose-Animation, Compose-Performance/Tooling | **kotlin.md** = Sprache/K2/Coroutinen-Mechanik (`CancellationException`, `GlobalScope`, Flow-Operatoren, `data class`). **android-platform.md** (geplant) = Lifecycle-Plattform, Room, WorkManager, Permissions, Service. **gradle.md** = Compose-Compiler-**Plugin-Setup**, `composeOptions`-Wegfall, Strong-Skipping-Aktivierung im Build, R8/Baseline-Profile-Build-Config |

> **Hinweis zu kotlin.md:** Dort stehen in den Abschnitten 6–9 bereits Compose-Grundlagen als
> *Kotlin-Kontext* (Recomposition/State/Side-Effects/Performance, kompakt). **Diese Datei ist die
> maßgebliche, vollstaendige Compose-Quelle** — kotlin.md verweist hierher fuer die Tiefe.
> Bezugstabelle ganz unten.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Endlos-Recompose / ANR / OOM | State nie in Composition schreiben, nur in Events/Effekten | §1.1 |
| 2 | Lazy-Liste (Insert/Reorder, Item-Anim) | Immer stabiler `key = { it.id }` an `items()` | §4.1 |
| 3 | Crash „Key … was already used" | Keys eindeutig machen (`distinctBy`/zusammengesetzt) | §4.2 |
| 4 | State weg nach Rotation/Recycling | `rememberSaveable` (klein!) oder ViewModel statt `remember` | §2.1 |
| 5 | Crash `TransactionTooLargeException` | Nur IDs/kleine Werte saven, grosses in ViewModel/Room | §2.7 |
| 6 | Effekt stale / `LaunchedEffect(Unit)` | Key = alle gelesenen veraenderlichen Werte | §3.1 |
| 7 | Langlebiger Effekt nutzt alten Wert | `rememberUpdatedState` statt Effekt-Neustart | §3.3 |
| 8 | Coroutine im Composable-Body | `LaunchedEffect`/`rememberCoroutineScope` nur in Callbacks | §3.5 |
| 9 | Composable skippt nicht (`List`/`copy()`) | `ImmutableList`/`@Immutable`; Strong Skipping cmp per `===` | §1.2 |
| 10 | Modifier-Reihenfolge falsch | Layout → `clip`→`background`→`border` → `clickable` | §5.1 |
| 11 | „Compose ist langsam" | Performance NUR im Release-Build (R8) messen | §10.1 |
| 12 | Jank beim Scrollen/Animieren | Hochfrequente Reads in `graphicsLayer{}`/`offset{}` deferren | §10.3 |
| 13 | Content unter TopBar/hinter NavBar | Scaffold-`innerPadding` IMMER anwenden | §8.1 |
| 14 | Crash „infinity constraints" | Kein verschachteltes gleichachsiges Scrollen | §6.1 |
| 15 | type-safe Nav crasht (custom Typ) | Eigenen `NavType` per `typeMap` registrieren | §7.4 |
| 16 | Gespeicherte Aenderung (DataStore/Room) erscheint erst beim naechsten Tap | Flow per `remember(context){…}` / ViewModel-`stateIn` stabil halten — NIE roh im Composable-Body neu bauen | §2.14 |
| 17 | `weight()` aus dynamischen Daten (Anteile/Zaehler) | Wert 0 crasht (`invalid weight`) — 0-Elemente vorher filtern | §6.8 |
| 18 | Endlos-Animation nur fuer EINEN Zustand (Puls/Blink) | `rememberInfiniteTransition` in den Zustands-Zweig verschieben — sonst tickt sie in JEDEM Zustand (Akku) | §9.6 |
| 19 | Pager-Swipe/Interaktion ruckelt, alle Seiten recomposen | `staticCompositionLocalOf`-Wert stabil remembern (State-backed Holder) — neue Instanz pro Recomposition invalidiert den GESAMTEN Subtree | §1.7 |
| 20 | Chart-Scrubbing/Drag ruckelt trotz `remember` | `remember`-Key zu breit: hochfrequenter State (selectedX) im Key invalidiert die teure Berechnung pro Frame — Heavy/Finalize-Split (`remember(data)` + `remember(selected, heavy)`) | §10.9 |

---

## 1. Recomposition & Stabilitaet / Skippability

### 1.1 Backwards Write → Endlos-Recomposition / ANR / OOM   ⭐ HAEUFIG / KRITISCH
**Symptom:** Composable recomposed jeden Frame ohne Nutzer-Interaktion; CPU/Akku hoch, UI haengt/ANR; im Extremfall `OutOfMemoryError`/`StackOverflowError`. Layout-Inspector zeigt endlos hochzaehlenden Recomposition-Count.
**Ursache:** Ein `State` wird im selben Composition-Durchlauf GESCHRIEBEN, nachdem es GELESEN wurde (z.B. `count++` direkt im Composable-Body statt in einem Event-Lambda). Compose plant sofort die naechste Recomposition.
**Versionen:** per Design, durchgaengig 1.6 → 1.10 (mehrfach bestaetigt: 3 von 7 Researchern).
**FIX:** State NIE im Composition-Scope schreiben — nur in Event-Callbacks (`onClick = { count++ }`) oder in `LaunchedEffect`/`SideEffect`. Abgeleitete Werte via `derivedStateOf`/`remember(key)`. Logik bleibt, nur der Schreibzeitpunkt wandert in Effekt/Callback.
**Quelle:** developer.android.com/develop/ui/compose/performance/bestpractices

### 1.2 Standard-Collections (List/Map/Set) verhindern Skipping   ⭐ HAEUFIG
**Symptom:** Composable mit `List<T>`/`Map`/`Set`-Parameter recomposed bei jeder Parent-Recomposition, obwohl der Inhalt gleich ist. Stability-Report: Klasse `unstable`, Composable `not skippable`.
**Ursache:** `List`/`Map`/`Set` sind nur Interfaces — der Compiler kann Mutierbarkeit nicht ausschliessen und markiert sie defensiv `Unstable`.
**Versionen:** per Design (1.6 → 1.10). Strong Skipping (default ab 1.7) loest es NICHT vollstaendig — siehe §1.3.
**FIX:** `kotlinx.collections.immutable` (`ImmutableList`/`PersistentList`) → vom Compiler als `@Stable` behandelt. Alternativ Liste in eine `@Immutable`-Wrapper-Klasse packen oder `kotlin.collections.*` in der Stability-Config-Datei als stabil deklarieren.
**Quelle:** developer.android.com/develop/ui/compose/performance/stability/fix; newsletter.jorgecastillo.dev (Strong skipping does not fix Kotlin collections)

### 1.3 Strong Skipping vergleicht unstable Params per `===` → `copy()`/neue Instanz = kein Skip   ⭐ HAEUFIG
**Symptom:** Trotz aktivem Strong Skipping recomposed ein Composable mit `data class`-Parameter jedes Mal, obwohl die Feldwerte identisch sind — typisch nach `state.copy(...)` im ViewModel.
**Ursache:** Strong Skipping erlaubt Composables mit unstable Params zu skippen, vergleicht diese aber per **referenzieller** Gleichheit (`===`), nicht strukturell (`equals`). `copy()`/Listen-Rebuild/Mapping erzeugt eine NEUE Instanz → `===` false → kein Skip. Strong Skipping ist KEIN Allheilmittel.
**Versionen:** ab Compose 1.7 / Compiler 2.0.20 (default). Per Design.
**FIX:** Klasse `@Stable`/`@Immutable` markieren (→ struktureller Vergleich) oder echt stabil machen (nur `val`, stabile Feldtypen) und gleiche Instanzen wiederverwenden statt bei jedem Update neu zu erzeugen.
**Quelle:** medium.com/androiddevelopers/jetpack-compose-strong-skipping-mode-explained-cbdb2aa4b900; doveletter.dev (Strong Skipping Misconceptions)

### 1.4 Lambda mit unstable Captures verhindert Skip
**Symptom:** Composable, dem ein Lambda uebergeben wird, recomposed bei jeder Parent-Recomposition.
**Ursache:** Lambdas mit unstable Captures werden ohne Strong Skipping nicht memoiziert → die Runtime alloziert bei jeder Recomposition eine neue Lambda-Instanz → ungleiche Parameter → kein Skip.
**Versionen:** betrifft < 1.7; **gefixt ab Compose 1.7 / Compiler 2.0.20** (Strong Skipping memoiziert solche Lambdas automatisch, keyed auf die Captures).
**FIX:** Auf Compose 1.7+ / Compiler 2.0.20+ (beide Projekte erfuellt). Bei aelteren Versionen Lambda manuell `remember`n.
**Quelle:** developer.android.com/develop/ui/compose/performance/stability/strongskipping

### 1.5 `@Stable`/`@Immutable` vergessen ODER falsch (Vertragsbruch)
**Symptom:** Entweder unnoetige Recompositions (vergessen) — ODER bei FALSCHER Annotation: veraltete/„stuck" UI, weil Compose Aenderungen nicht mehr bemerkt.
**Ursache:** Die Annotationen machen eine Klasse nicht stabil, sie sind ein **Versprechen** an den Compiler. Eine faelschlich annotierte Klasse mit `var`/mutierbaren Feldern laesst Compose noetige Recompositions ueberspringen.
**Versionen:** per Design (1.6 → 1.10).
**FIX:** Nur wirklich unveraenderliche Klassen (`val`, stabile Feldtypen) annotieren. Stability-Report erzeugen und pruefen welche Felder `unstable`/`runtime` sind.
**Quelle:** developer.android.com/develop/ui/compose/performance/stability

### 1.6 Typ aus Fremd-Modul = `runtime`-Stabilitaet → nicht skippable
**Symptom:** Composable mit Parameter aus einer anderen Library/einem anderen Modul ist nicht skippable; Stability-Report zeigt Klasse/Feld als `runtime`.
**Ursache:** Stammt ein Typ aus einem Modul, das nicht mit dem Compose-Compiler kompiliert wurde, kann die Stabilitaet nicht bestimmt werden → `runtime` (oft effektiv unstable).
**Versionen:** per Design (1.6 → 1.10).
**FIX:** Typ in eigene `@Immutable`-Wrapper-Klasse packen ODER Stability-Konfigurationsdatei (`stabilityConfigurationFile`) mit dem voll-qualifizierten Klassennamen anlegen.
**Quelle:** developer.android.com/develop/ui/compose/performance/stability/fix

### 1.7 `staticCompositionLocalOf` mit pro-Recomposition neu erzeugtem Wert → GESAMTER Subtree recomposed   ⭐ EIGENER VORFALL 2026-07-03
**Symptom:** UI ruckelt bei Interaktionen, die den Provider-Host recomposen — z. B. `HorizontalPager`-Swipes, bei denen ALLE Seiten (trotz `beyondViewportPageCount`) jedes Mal komplett neu komponiert werden.
**Ursache:** `staticCompositionLocalOf` trackt Leser NICHT — aendert sich der `provides`-Wert, wird der KOMPLETTE `content`-Block des Providers invalidiert. Wird der Wert (z. B. ein State-Holder wie `MicActionsState(isOpen) { … }`) ohne `remember` im Body erzeugt, ist er bei JEDER Host-Recomposition eine neue Instanz → jede Host-Recomposition (etwa durch einen `pagerState.currentPage`-Read in Composition) recomposed den gesamten Baum darunter. EntropieReductor-Vorfall: 4 Sub-Tab-Screens wurden bei jedem Swipe voll neu komponiert (#47440).
**Versionen:** per Design (alle Compose-Versionen).
**FIX (funktionserhaltend):** Provider-Wert stabil halten: Holder EINMAL per `remember { … }` erzeugen und veraenderliche Felder intern aus `State<T>` lesen (`val isOpen get() = state.value`) statt als eingefrorene Konstruktorwerte. Hochfrequente Reads wie `pagerState.currentPage` fuer Seiteneffekte per `snapshotFlow` beobachten statt in Composition zu lesen. `staticCompositionLocalOf` nur fuer Werte, die sich praktisch nie aendern — sonst `compositionLocalOf` (feingranulares Leser-Tracking).
**Quelle:** developer.android.com/develop/ui/compose/compositionlocal (staticCompositionLocalOf-Absatz) + eigener Vorfall EntropieReductor #47440.

---

## 2. State, `remember` & `rememberSaveable`

### 2.1 `mutableStateOf` ohne `remember` → State-Reset bei jeder Recomposition   ⭐ HAEUFIG
**Symptom:** Eingaben/Zaehler springen auf den Anfangswert zurueck; State „vergisst" sich.
**Ursache:** `var x by mutableStateOf(0)` ohne `remember` erzeugt bei jeder Recomposition ein NEUES State-Objekt mit Initialwert.
**Versionen:** per Design (1.6 → 1.10).
**FIX:** Immer `var x by remember { mutableStateOf(...) }`; fuer Config-Change/Process-Death `rememberSaveable`.
**Quelle:** developer.android.com/develop/ui/compose/state

### 2.2 `mutableStateOf(list)` statt `mutableStateListOf` → UI aktualisiert nicht   ⭐ HAEUFIG
**Symptom:** Element zur Liste hinzufuegen/entfernen oder Property eines Elements aendern → UI zeigt die alte Liste, kein Recompose.
**Ursache:** `mutableStateOf(mutableListOf())` beobachtet nur die **Referenz**. `.add`/`.remove`/`item.x = y` aendern die Referenz nicht → kein State-Read invalidiert.
**Versionen:** per Design (1.6 → 1.10).
**FIX:** `mutableStateListOf<T>()` (oder `.toMutableStateList()`). Sollen auch **interne** Felder von Elementen reaktiv sein: das Feld selbst per `by mutableStateOf()` im Element backen (`mutableStateListOf` beobachtet nur Add/Remove/Replace).
**Quelle:** tigeroakes.com/posts/mutablestateof-list-vs-mutablestatelistof/

### 2.3 `remember` ohne/mit falschem `key` → stale Wert bei Parameter-Wechsel   ⭐ HAEUFIG
**Symptom:** Ein in `remember { ... }` initialisierter/abgeleiteter Wert bleibt alt, wenn sich der Eingabe-Parameter aendert.
**Ursache:** `remember` ohne `key` behaelt seinen Wert ueber die ganze Composition-Lebensdauer; das Lambda wird bei Parameter-Aenderung nicht neu ausgefuehrt.
**Versionen:** per Design (1.6 → 1.10).
**FIX:** Relevante Parameter als Key: `remember(param) { ... }`. Fuer Werte/Callbacks in Effekten, die aktuell sein muessen ohne Effekt-Restart: `rememberUpdatedState`.
**Quelle:** developer.android.com/develop/ui/compose/state

### 2.4 `by mutableStateOf` ohne `getValue`/`setValue`-Import → Compile-Fehler   ⭐ HAEUFIG
**Symptom:** Kompilierfehler `Type 'MutableState<T>' has no method getValue/setValue` / Type-Mismatch. IDE schlaegt den Import oft nicht vor.
**Ursache:** Die Delegate-Operatoren sind Extension-Funktionen in `androidx.compose.runtime` und werden nicht automatisch importiert (IDE-Luecke JetBrains KTIJ-16966).
**Versionen:** alle; IDE-Vorschlags-Bug offen.
**FIX:** `import androidx.compose.runtime.getValue` + `import androidx.compose.runtime.setValue`. Alternativ `val (v, setV) = remember { mutableStateOf(...) }` (braucht die Imports nicht).
**Quelle:** youtrack.jetbrains.com/issue/KTIJ-16966

### 2.5 `var`/`val` mit `mutableStateOf` falsch kombiniert
**Symptom:** Compile-Fehler oder State laesst sich nicht setzen.
**Ursache:** Bei Delegation `by` muss es `var` sein; bei direktem `= mutableStateOf()` ist es `val state` + Zugriff via `state.value`. `val x by mutableStateOf()` oder `var x = mutableStateOf()` sind die typischen Fehlgriffe.
**Versionen:** per Design.
**FIX:** Entweder `var x by remember { mutableStateOf(...) }` (mit Imports) ODER `val x = remember { mutableStateOf(...) }` + `x.value`. Konsistent eine Form.
**Quelle:** developer.android.com/develop/ui/compose/state

### 2.6 `remember` ueberlebt keine Config-Change/Process-Death
**Symptom:** Eingabe/Auswahl/Scroll weg nach Rotation, Dark-Mode-Wechsel, Split-Screen, Kill.
**Ursache:** `remember` ist nur Composition-Cache, nicht persistiert.
**Versionen:** per Design (1.6 → 1.10).
**FIX:** `rememberSaveable` fuer User-Input/Scroll/Selektion. Kritische Daten echt persistieren (DataStore/Room/ViewModel-`SavedStateHandle`).
**Quelle:** developer.android.com/develop/ui/compose/state

### 2.7 `rememberSaveable` `TransactionTooLargeException` bei zu grossem State
**Symptom:** Crash beim Backgrounding/Config-Change/Process-Death, sobald grosse Objekte/Listen via `rememberSaveable` gespeichert werden — oft erst in Produktion.
**Ursache:** `rememberSaveable` legt seinen Wert im saved-instance-state-`Bundle` ab; das Bundle hat ein hartes IPC-Limit (~1 MB, app-weit geteilt).
**Versionen:** per Design (Framework-Limit, alle Versionen).
**FIX (funktionserhaltend):** Nur Schluessel/IDs via `rememberSaveable`, grosse Daten in Room/DataStore/ViewModel auslagern und beim Restore ueber den Key nachladen. NICHT auf `remember` zurueckfallen (verliert State).
**Quelle:** developer.android.com/develop/ui/compose/state-saving

### 2.8 `rememberSaveable` crasht mit nicht-Saveable/nicht-Parcelable-Typ   ⭐ HAEUFIG
**Symptom:** `IllegalArgumentException: ... cannot be saved using the current SaveableStateRegistry`, sobald ein Custom-Typ in `rememberSaveable` gesteckt wird.
**Ursache:** Es gibt keinen Default-Saver fuer beliebige Typen — nur Bundle-faehige (Primitives, String, Parcelable, Serializable).
**Versionen:** per Design (Default-Saver-Wunsch: issuetracker 180042685, won't-fix-artig).
**FIX:** Custom `Saver` (`mapSaver`/`listSaver` oder eigenes `Saver`-Objekt) ODER Typ `@Parcelize`/Parcelable machen.
**Quelle:** issuetracker.google.com/issues/180042685

### 2.9 `remember` in LazyColumn-Item verliert State beim Wegscrollen (Recycling)   ⭐ HAEUFIG
**Symptom:** Per `remember` gehaltener Item-State (Counter, Expand, Checkbox) wird beim Weg- und Zurueckscrollen zurueckgesetzt.
**Ursache:** Per Design — LazyColumn recycelt Items; beim Wiedereintritt laeuft der `remember`-Block neu. `remember` ist nur fuer Recomposition, nicht fuer Recycling.
**Versionen:** per Design (1.6 → 1.10).
**FIX:** `rememberSaveable` statt `remember` fuer Item-lokalen State (ueberlebt Recycling) ODER State pro Item-ID ins ViewModel. Zusaetzlich stabile `key` an die `items()` (§4.1).
**Quelle:** issuetracker.google.com/issues/177245496

### 2.10 `derivedStateOf` ueberbenutzt (1:1-Abhaengigkeiten) → Overhead statt Optimierung   ⭐ HAEUFIG
**Symptom:** `derivedStateOf` ueberall als „Caching"; Performance schlechter statt besser.
**Ursache:** Missverstaendnis — `derivedStateOf` ist KEIN Cache, sondern ein „Frequenz-Reduzierer". Wechselt der abgeleitete Wert genauso oft wie seine Inputs (z.B. `firstName + lastName`), entsteht nur Overhead.
**Versionen:** konzeptionell, alle.
**FIX:** `derivedStateOf` NUR wenn der Output **seltener** wechselt als der Input (z.B. `firstVisibleItemIndex > 0`). Bei 1:1 direkt berechnen.
**Quelle:** medium.com/androiddevelopers/jetpack-compose-when-should-i-use-derivedstateof-63ce7954c11b

### 2.11 `derivedStateOf` mit Nicht-State-Parameter → Aenderung ignoriert (stale)
**Symptom:** `derivedStateOf`-Berechnung liefert dauerhaft den alten Wert, obwohl sich ein Parameter geaendert hat.
**Ursache:** `derivedStateOf` beobachtet nur Compose-`State`. Ein gewoehnlicher `val`/Parameter wird beim Erstellen eingefangen und nicht aktualisiert.
**Versionen:** per Design.
**FIX:** Parameter in echten `State` umwandeln, ODER als Key: `remember(param) { derivedStateOf { ... } }`.
**Quelle:** saurabharora.dev/posts/navigating-pitfalls-when-to-use-derivedStateOf-with-keys/

### 2.12 State zu tief statt gehoisted → Verlust / nicht teilbar
**Symptom:** State geht verloren wenn der innere Composable verschwindet; anderer Composable kann ihn nicht lesen/steuern.
**Ursache:** State im falschen Scope: zu tief (`remember` im inneren Composable) statt im gemeinsamen Eltern-Composable/ViewModel.
**Versionen:** konzeptionell.
**FIX:** State auf den niedrigsten **gemeinsamen** Vorfahren heben (State-Hoisting: `value` + `onValueChange` nach oben), bzw. ueberlebenswichtigen State ins ViewModel.
**Quelle:** developer.android.com/develop/ui/compose/state-hoisting

### 2.13 `collectAsState` statt `collectAsStateWithLifecycle` → Collection im Hintergrund
**Symptom:** Flow sammelt auch im Hintergrund weiter; unnoetige Recompositions, Akku/CPU.
**Ursache:** `collectAsState` ist nicht lifecycle-aware (sammelt auch im STOPPED-Zustand).
**Versionen:** per Design; `collectAsStateWithLifecycle` ab `lifecycle-runtime-compose` 2.6+ (in 2.8.7 vorhanden). *(Grenzfall Flow→Compose — Flow-Mechanik selbst siehe kotlin.md §5.)*
**FIX:** Auf Android `collectAsStateWithLifecycle()`; `collectAsState` nur fuer Nicht-Android-Targets (KMP/Desktop).
**Quelle:** medium.com/androiddevelopers/consuming-flows-safely-in-jetpack-compose-cde014d0d5a3

### 2.14 Roher (cold) Flow im Composable-Body neu erzeugt → `collectAsStateWithLifecycle` verpasst Emissionen   ⭐ HAEUFIG / TÜCKISCH
**Symptom:** Eine per DataStore/Room/Flow gespeicherte Aenderung (add/update/delete) erscheint NICHT sofort in der Liste. Erst eine *unabhaengige* Recomposition (Tap auf ein anderes Element, Dialog oeffnen) zeigt den neuen Stand. Die Anzeige scheint „mit dem gespeicherten Wert nichts zu tun zu haben".
**Ursache:** Der Flow wird DIREKT im Composable-Body erzeugt — z.B. `val s by mentalsFlow(context).collectAsStateWithLifecycle(...)` oder `context.store.data.map{}.collectAsStateWithLifecycle(...)`. Bei JEDER Recomposition entsteht ein NEUES Flow-Objekt → `collectAsStateWithLifecycle` (intern `produceState` mit dem Flow als Key) re-keyed staendig → die laufende Collection-Subscription ist instabil und faengt die Emission nach dem Write nicht zuverlaessig. Erst eine unabhaengige Recomposition startet eine frische Subscription, die den Store frisch liest.
**Tueckisch:** Ein zweiter Screen mit IDENTISCHEM Code kann „funktionieren", wenn er durch viele andere States/Folge-Updates ohnehin staendig recomposed — der Bug ist dann nur verdeckt, nicht weg.
**Versionen:** per Design, 1.6 → 1.10 (DataStore/Room/jeder cold Flow).
**FIX (funktionserhaltend):** Flow EINMAL stabil halten — `val f = remember(context) { mentalsFlow(context) }; val s by f.collectAsStateWithLifecycle(initial)`. Sauberste Form: Flow im ViewModel als `stateIn(viewModelScope, WhileSubscribed(5000), initial)` exponieren und `vm.x.collectAsStateWithLifecycle()`. NIE den rohen cold Flow pro Recomposition neu im Composable bauen. (Verwandt: kotlin.md §4.4, §4.2.)
**Verifikation:** Logcat-Sonde `onEach { Log.d }` am Flow + `Log.d` am Write → nach „WRITTEN" muss SOFORT ein „emit" folgen.
**Quelle:** developer.android.com/develop/ui/compose/state; medium.com/androiddevelopers/consuming-flows-safely-in-jetpack-compose-cde014d0d5a3 — eigener Vorfall EntropieReductor Mentalboard 2026-06-10 (2x aufgetreten: erster Fix zielte faelschlich auf eine `working`-Zwischenkopie statt auf die Flow-Stabilitaet).

---

## 3. Side-Effects (`LaunchedEffect` / `DisposableEffect` / `snapshotFlow` …)

### 3.1 `LaunchedEffect(Unit)`/`(true)` trotz veraenderlicher Abhaengigkeit → stale   ⭐ HAEUFIG
**Symptom:** Effekt startet nie neu, arbeitet mit dem ersten (stale) Wert; UI/Netzwerk-Reaktion auf neue Parameter bleibt aus.
**Ursache:** `LaunchedEffect(Unit)`/`(true)` laeuft genau einmal pro Eintritt. Doku: `LaunchedEffect(true)` ist „as suspicious as a `while(true)`".
**Versionen:** per Design (1.6 → 1.10).
**FIX:** Alle im Block gelesenen veraenderlichen Werte als Keys: `LaunchedEffect(userId, query) { … }`. Konstanten Key nur wenn der Effekt bewusst genau einmal laufen soll.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.2 `LaunchedEffect` mit zu volatilem Key → staendiger Cancel/Neustart   ⭐ HAEUFIG
**Symptom:** Laufende Arbeit (Netzwerk-Call, Animation, `delay`) wird bei jeder Recomposition abgebrochen und neu gestartet → Flackern, nie fertige Requests, hohe Last.
**Ursache:** Als Key wird ein bei jeder Recomposition neu erzeugtes Objekt uebergeben (Lambda, neue data class). Key-Wechsel cancelt die alte Coroutine.
**Versionen:** per Design.
**FIX:** Key stabilisieren (ID statt ganzes Objekt; Key mit `remember` cachen) ODER Wert per `rememberUpdatedState` lesen statt als Key.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.3 `rememberUpdatedState` vergessen → Effekt nutzt stale Lambda/Wert   ⭐ HAEUFIG
**Symptom:** Langlebiger Effekt (`LaunchedEffect(Unit)` mit `delay` + `onTimeout()`) ruft eine veraltete Callback-Version auf.
**Ursache:** Der Effekt soll NICHT neu starten, captured aber das Lambda zur Startzeit.
**Versionen:** per Design.
**FIX:** `val current by rememberUpdatedState(onTimeout)`, im Effekt `current()` aufrufen. Effekt behaelt konstanten Key, liest aber immer aktuell.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.4 `DisposableEffect` ohne/mit leerem `onDispose` → Listener-/Callback-Leak
**Symptom:** Build-Fehler (onDispose ist Pflicht); bei leerem `onDispose {}`: registrierte Listener/Observer/Receiver werden nicht entfernt → Leak, doppelte Callbacks.
**Ursache:** Leerer onDispose taeuscht den Compiler, raeumt aber nichts ab.
**Versionen:** per Design.
**FIX:** In `onDispose` die symmetrische Cleanup-Operation (`removeObserver`/`unregisterReceiver`/`removeListener`). Bei Key-Wechsel raeumt DisposableEffect zuerst auf, dann re-registriert.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.5 Coroutine direkt im Composable-Body gestartet   ⭐ HAEUFIG
**Symptom:** Bei jeder Recomposition eine NEUE Coroutine (oft Hunderte) → doppelte Netzwerk-Calls, Races, Speicheraufbau.
**Ursache:** `rememberCoroutineScope().launch { … }` (oder injizierter Scope) wird im Composable-Body statt in einem Callback aufgerufen.
**Versionen:** per Design.
**FIX:** Suspend-Arbeit in `LaunchedEffect(key)` (an Lifecycle gebunden, cancelt automatisch). `rememberCoroutineScope` NUR in Event-Callbacks (`onClick { scope.launch { … } }`), nie im Body.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.6 `SideEffect`-Missbrauch — laeuft nach JEDER Recomposition
**Symptom:** Code im `SideEffect` (Analytics, Logging) feuert weit oefter als gedacht → doppelte Events, Performance-Einbruch.
**Ursache:** `SideEffect` garantiert Ausfuehrung „after every successful recomposition" — nicht einmalig, nicht keyed.
**Versionen:** per Design.
**FIX:** `SideEffect` nur zum Publizieren von Compose-State an Nicht-Compose-Code. Einmalige/keyed Arbeit → `LaunchedEffect`, Cleanup → `DisposableEffect`.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.7 `snapshotFlow` liest State nicht reaktiv (Read ausserhalb des Blocks)
**Symptom:** Flow emittiert nur den Startwert; bei modifizierendem Code `IllegalStateException`.
**Ursache:** Nur State-Reads INNERHALB `snapshotFlow { … }` werden getrackt. Wird der State vorher in eine Variable gelesen, erkennt snapshotFlow keine Abhaengigkeit.
**Versionen:** per Design.
**FIX:** State direkt im Block lesen: `snapshotFlow { listState.firstVisibleItemIndex }`. Im Block nie State mutieren. (Nutzt `distinctUntilChanged`-Semantik.)
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.8 `produceState` konflatiert Werte — identischer Wert loest keine Recomposition aus
**Symptom:** Erwartete Recomposition bleibt aus, wenn derselbe Wert erneut gesetzt wird; Event-artige Signale gehen verloren.
**Ursache:** Der zurueckgegebene State konflatiert; `produceState` modelliert State, keine Events.
**Versionen:** per Design.
**FIX:** Wiederholte gleiche Signale nicht als State modellieren (Channel/SharedFlow) oder State-Modell mit Sequenznummer. `produceState` nur fuer Nicht-Compose-State → Compose-State.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

### 3.9 `LaunchedEffect` feuert mehrfach/zu frueh bei Navigation (Snackbar, navigate-once)   ⭐ HAEUFIG
**Symptom:** One-Shot-Effekte (Snackbar zeigen, einmal navigieren) laufen mehrfach. Waehrend des Screen-Uebergangs sind alter + neuer Screen kurz gleichzeitig aktiv; der verlassene Screen recomposed und re-triggert.
**Ursache:** Transition-Overlap (per Design) + teils Bug: Lifecycle-Events bei Navigation falsch gefeuert.
**Versionen:** teils per Design, teils Bug — **issuetracker 425901162 (offen)**, navigation-compose 2.8.x; auf Compose-Multiplatform/iOS zusaetzlich JetBrains#3890.
**FIX:** Navigation/Snackbar als konsumierbares **Event** modellieren (One-Shot-Flag im ViewModel, nach Verbrauch zuruecksetzen; oder Channel/SharedFlow). Snackbar an eindeutiger Event-ID; Effekt nicht an `Unit` haengen, wenn er nur bei echtem Event laufen soll.
**Quelle:** issuetracker.google.com/issues/425901162

### 3.10 `LaunchedEffect`/Coroutine bei Recomposition abgebrochen → verlorene Arbeit
**Symptom:** Nicht-idempotente Arbeit (POST, Datei schreiben) wird mittendrin gecancelt, wenn der Key wechselt/der Composable die Composition verlaesst → halbe Schreibvorgaenge.
**Ursache:** Compose cancelt die LaunchedEffect-Coroutine bei Key-Wechsel + Verlassen der Composition.
**Versionen:** per Design.
**FIX:** Kritische/nicht-abbrechbare Arbeit nicht an die Composable-Lifecycle binden, sondern im `viewModelScope` (oder WorkManager); das Composable triggert nur.
**Quelle:** developer.android.com/develop/ui/compose/side-effects

---

## 4. Lazy-Layouts (Column/Row/Grid/StaggeredGrid) & Pager

### 4.1 Fehlende stabile Keys → Scroll-Sprung, State-Mismatch, falsche Item-Animation   ⭐ HAEUFIG
**Symptom:** Nach Insert/Remove/Reorder springt die Scroll-Position; `remember`-State eines Items „wandert" zum falschen Eintrag; `animateItem()` animiert das falsche/kein Item.
**Ursache:** Ohne `key` keyt Compose Items gegen die LISTEN-POSITION. Reihenfolge-Aenderung → Items verlieren ihre Identitaet + remembered State; ScrollPosition-Tracker verliert den Bezug.
**Versionen:** per Design (1.6 → 1.10; mehrfach bestaetigt: 4 von 7 Researchern).
**FIX:** `items(list, key = { it.id })` + `contentType` fuer heterogene Listen. Pflicht, damit `animateItem()` korrekt arbeitet und State konsistent bleibt.
**Quelle:** developer.android.com/develop/ui/compose/lists; issuetracker.google.com/issues/177245496

### 4.2 Duplikat-/leere/null-Keys → Crash „Key … was already used"   ⭐ HAEUFIG
**Symptom:** `IllegalArgumentException: Key "0" was already used. If you are using LazyColumn/Row please make sure you provide a unique key for each item.` — Crash beim Render.
**Ursache:** Das `key`-Lambda liefert einen nicht-eindeutigen Wert (doppelte IDs, mehrere `null`/Default/Leer-Keys, oder mehrere Lazy-Listen teilen denselben Key-Raum in derselben Composition).
**Versionen:** per Design (Eindeutigkeits-Check, 1.6 → 1.10).
**FIX:** Keys garantiert eindeutig — Daten `distinctBy { it.id }` deduplizieren, zusammengesetzten Key `key = { "${it.id}-${it.name}" }`, mehreren Listen ein Praefix `key = { "section_a_${it.id}" }`. Nie potentiell mehrfach vorkommende/leere Werte als Key.
**Quelle:** github.com/wordpress-mobile/WordPress-Android/issues/17702

### 4.3 Verschachteltes gleichachsiges Scrollen → Crash „infinity constraints"
**Symptom:** `IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed.` — siehe ausfuehrlich **§6.1** (Crash-Sektion).
**FIX (Kurz):** Kein zweites Scroll-Element gleicher Richtung verschachteln — alles in EINE LazyColumn (Header als `item {}`), oder dem inneren Container endliche Hoehe geben. Details + Custom-`layout{}`-Workaround in §6.1.
**Quelle:** medium.com (nested LazyColumn infinity constraints) — Volleintrag §6.1

### 4.4 `contentType` vergessen → Performance bei heterogenen Listen
**Symptom:** Bei gemischten Item-Typen (Header/Row/Ad/Footer) stockt das Scrollen; Items werden unnoetig neu komponiert statt Slots wiederverwendet.
**Ursache:** Ohne `contentType` kann nur zwischen Items gleichen (unbekannten) Typs recycelt werden.
**Versionen:** per Design.
**FIX:** `items(list, key = { it.id }, contentType = { it::class })` bzw. konstanter Typ-Marker je Layout-Variante.
**Quelle:** developer.android.com/develop/ui/compose/lists

### 4.5 `items()`-Overload-Verwechslung (count vs. list) → falscher Index/Inhalt
**Symptom:** Falsche Daten pro Zeile, Off-by-one, oder Compile-Fehler.
**Ursache:** `items(count) { index -> }` liefert einen Int-Index, `items(list) { item -> }` das Element, `itemsIndexed(list) { index, item -> }` beides. `items(list.size) { ... }` gibt den Index — wer ihn wie ein Item benutzt, baut Fehler.
**Versionen:** per Design.
**FIX:** Passenden Overload waehlen — `items(list)` fuer Elemente, `itemsIndexed(list)` fuer beides; `items(count)` nur fuer reine Index-Faelle.
**Quelle:** developer.android.com/develop/ui/compose/lists

### 4.6 `fillMaxSize()` statt `fillParentMaxSize()` im Lazy-Item
**Symptom:** Loading-/Empty-Item fuellt nicht den Viewport, oder kollabiert/misst falsch in der unendlich hohen Lazy-Achse.
**Ursache:** Innerhalb eines Lazy-Items ist die Scroll-Achse unbegrenzt — `fillMaxSize()`/`fillMaxHeight()` hat keinen endlichen Bezug.
**Versionen:** per Design.
**FIX:** `Modifier.fillParentMaxSize()` (bzw. `fillParentMaxHeight/Width`) — bezieht sich auf den Viewport der Lazy-Komponente. Typisch fuer ganzseitige Loading-/Empty-States.
**Quelle:** developer.android.com/develop/ui/compose/lists

### 4.7 Pager `beyondBoundsPageCount` → Pre-Load-/Effect-Bugs
**Symptom:** Seiten-gekoppelte Effekte feuern abhaengig vom Wert unterschiedlich; bei hohem Wert massive Recomposition (Lazy-Loading zerstoert).
**Ursache:** Off-Screen vorkomponierte Seiten aendern, wann ein in der Seite stehendes `LaunchedEffect` startet.
**Versionen:** Compose 1.6 → 1.10 (Pager stabil seit 1.4); issuetracker 289088847.
**FIX:** `beyondBoundsPageCount` klein halten (1–2). Seiten-abhaengige Effekte ueber `snapshotFlow { pagerState.currentPage }`/`settledPage` ausloesen, nicht per seiten-internem `LaunchedEffect`.
**Quelle:** issuetracker.google.com/issues/289088847

### 4.8 HorizontalPager disposed Composables nicht zuverlaessig → Leak
**Symptom:** Composables nicht mehr sichtbarer Seiten werden nicht disposed; `DisposableEffect`-Cleanup laeuft nicht/spaet → Player/Listener bleiben aktiv.
**Ursache:** Pager haelt Seiten (via `beyondBoundsPageCount` + internem Verhalten) laenger im Baum; Dispose erst beim Verlassen des Bounds-Bereichs.
**Versionen:** Compose 1.6 → 1.10 (auch in compose-multiplatform berichtet; **JetBrains#4279 CLOSED/COMPLETED**, gh-verifiziert 2026-06-02 — war CMP/iOS-Scroll-Variante).
**FIX:** Cleanup nicht an die Existenz der Composition koppeln, sondern aktiv auf `pagerState.currentPage`/`settledPage` reagieren (Ressourcen pausieren/freigeben wenn nicht aktuelle Seite); `beyondBoundsPageCount` minimal.
**Quelle:** github.com/JetBrains/compose-multiplatform/issues/4279

---

## 5. Modifier

### 5.1 Modifier-Reihenfolge aendert das Ergebnis (padding/size/background/clip/clickable/border)   ⭐ HAEUFIG
**Symptom:** Background/Border faerbt den Padding-Bereich mit (oder zu klein); Border folgt nicht den runden Ecken; Klickflaeche/Ripple zu gross/klein; Element nicht klickbar.
**Ursache:** Modifier werden von links nach rechts angewandt, jeder umschliesst den vorherigen. Typische Fehler: `background` VOR `padding`; `border` VOR `clip`; `clickable` nach `padding` (nur Innenbereich klickbar) bzw. davor (gesamte Flaeche inkl. Padding).
**Versionen:** konzeptionell (1.6 → 1.10).
**FIX:** Reihenfolge bewusst: Layout (`size`/`fillMax`/`padding`) → Dekoration (`clip` → `background` → `border`) → Interaktion (`clickable`). `clip` IMMER vor `border`; `padding` vor `clickable` nur wenn Padding NICHT klickbar sein soll.
**Quelle:** developer.android.com/develop/ui/compose/modifiers

### 5.2 `pointerInput` mit falschem/konstantem `key` → stale Closure
**Symptom:** Gesten-Handler (`detectTapGestures`/`detectDragGestures`) reagiert mit veralteten Werten; ignoriert spaetere Aenderungen.
**Ursache:** `pointerInput(Unit)` startet den Block nur einmal und captured Variablen zum Startzeitpunkt.
**Versionen:** per Design.
**FIX:** Sich aendernde Variable als Key (`pointerInput(parameter) { … }`) ODER `rememberUpdatedState` (`val current by rememberUpdatedState(parameter); pointerInput(Unit) { … current … }`). Bei Listen nie den Index capturen — Item direkt verwenden.
**Quelle:** medium.com/@vh.dev (forcing recomposition when your lambdas stay stuck)

---

## 6. Crashes & Exceptions (konkrete Laufzeit-/Compile-Fehler)

> **Crash-Schnellindex** (exakte Fehlermeldung → Abschnitt):
> | Meldung (Auszug) | Abschnitt |
> |---|---|
> | „Vertically/Horizontally scrollable component was measured with an infinity …" | §6.1 |
> | „Can't represent a size of N in Constraints" | §6.2 |
> | „CompositionLocal Local… not present / No ViewModelStoreOwner was provided" | §6.3 |
> | „… cannot be cast to android.app.Activity" | §6.4 |
> | „@Composable invocations can only happen from the context of a @Composable function" | §6.5 |
> | „Asking for intrinsic measurements of SubcomposeLayout … not supported" | §6.6 |
> | `IllegalStateException` beim MutableState-Write (Background/Non-Snapshot) | §6.7 |
> | „Key … was already used" | §4.2 |
> | Endlos-Recomposition / ANR / OOM (Backwards Write) | §1.1 |

### 6.1 „Vertically/Horizontally scrollable component was measured with infinity constraints"   ⭐ HAEUFIG
**Symptom:** `IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed.` (analog horizontal/Breite).
**Ursache:** Ein scrollbares Composable (LazyColumn, `Modifier.verticalScroll()`) liegt in einem Container mit unbeschraenkter Hauptachse — typisch LazyColumn in `Column(Modifier.verticalScroll())`, oder ein Parent reicht `Int.MAX_VALUE` als maxHeight weiter.
**Versionen:** by-design-Check, durchgaengig 1.6 → 1.10 (beide BOM identisch).
**FIX (funktionserhaltend):** Verschachtelten gleichachsigen Scroll vermeiden — eine LazyColumn fuer alles, Sektionen via `item {}`/`items()`. Wenn echte Verschachtelung noetig: dem inneren Container endliche Hoehe (`Modifier.height(x)`/`heightIn(max = x)`), NICHT das Composable entfernen. Verschiedene Richtungen (LazyColumn in LazyRow) sind erlaubt. Custom-Workaround: in `layout {}` `constraints.maxHeight == Int.MAX_VALUE` pruefen und durch Screen-Hoehe ersetzen.
**Quelle:** medium.com/@neha-saini (nested LazyColumn infinity constraints)

### 6.2 „Can't represent a size of N in Constraints"
**Symptom:** `IllegalArgumentException: Can't represent a size of 2147483647 in Constraints` (Stack: `Constraints$Companion.bitsNeedForSize`).
**Ursache:** Gleiche Wurzel wie §6.1 — eine Infinity-Constraint wird an ein Layout durchgereicht, das eine konkrete Pixelgroesse braucht. Ausloeser: `Modifier.wrapContentSize(unbounded = true)`, ComposeView in LinearLayout mit `weight`, Custom-Layout das unbounded misst.
**Versionen:** beobachtet ab 1.3.0/1.3.1, weiterhin 1.6 → 1.10.
**FIX:** Unendliche Constraint in der Hierarchie ueber dem Layout entfernen — `unbounded = true` weglassen oder durch `heightIn/widthIn`-Grenze ersetzen; ComposeView feste Hoehe statt `weight`.
**Quelle:** slack-chats.kotlinlang.org (scrollable infinity / Can't represent size)

### 6.3 „CompositionLocal Local… not present" / „No ViewModelStoreOwner was provided"
**Symptom:** `IllegalStateException: No ViewModelStoreOwner was provided via LocalViewModelStoreOwner` (analog fuer eigene CompositionLocals: `CompositionLocal LocalX not present`).
**Ursache:** Ein `@Composable` (z.B. `viewModel()`) liest `LocalViewModelStoreOwner`, aber im Baum ist kein Owner bereitgestellt — Compose-Content ausserhalb einer ComponentActivity/NavHost (ComposeView, embedded Views), oder beim Unmount. Eigene `compositionLocalOf` ohne `CompositionLocalProvider` werfen denselben Fehler.
**Versionen:** Compose 1.6 → 1.10; lifecycle-viewmodel-compose alle.
**FIX:** Owner bereitstellen: `CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { … }`. Eigene CompositionLocals via `compositionLocalOf` mit sinnvollem Default oder Provider-Scope korrekt umschliessen — nie das Composable still entfernen.
**Quelle:** developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis

### 6.4 `ClassCastException` beim Cast `LocalContext.current as Activity`   ⭐ HAEUFIG
**Symptom:** `ClassCastException: android.view.ContextThemeWrapper (oder ContextWrapper/Application) cannot be cast to android.app.Activity`.
**Ursache:** Der Context ist je nach Host ein `ContextWrapper`/`ContextThemeWrapper`/`Application`-Context, nicht direkt die Activity.
**Versionen:** Compose 1.6 → 1.10.
**FIX:** Context-Kette sicher durchlaufen statt blind casten:
```kotlin
fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) { if (ctx is Activity) return ctx; ctx = ctx.baseContext }
    throw IllegalStateException("No Activity in Context chain")
}
```
Im Composable `val activity = LocalContext.current.findActivity()` (Pattern aus accompanist/permissions).
**Quelle:** gist.github.com/kibotu/8211a2dfc8ea0f41fcb7f9a79a87b1ff

### 6.5 „@Composable invocations can only happen from the context of a @Composable function" (Compile)
**Symptom:** Compile-Error (kein Laufzeit-Crash).
**Ursache:** Ein `@Composable` (oder CompositionLocal-Read wie `LocalContext.current`/`viewModel()`) wird aus Nicht-Composable-Kontext aufgerufen — in `onClick`, in normaler Funktion, im `LaunchedEffect`-Body ohne Composable-Scope, oder die `@Composable`-Annotation fehlt.
**Versionen:** alle; verschaerft seit Compose 1.2.0; issuetracker 241258671.
**FIX:** Composable-Aufrufe nur aus Composable-Kontext. CompositionLocal-Werte VOR dem Callback lesen (`val ctx = LocalContext.current` im Body, dann `onClick = { use(ctx) }`). UI-Funktion mit `@Composable` annotieren; Business-Logik bleibt normale Funktion.
**Quelle:** issuetracker.google.com/issues/241258671

### 6.6 SubcomposeLayout/BoxWithConstraints: intrinsische Messung nicht unterstuetzt
**Symptom:** `IllegalStateException: Asking for intrinsic measurements of SubcomposeLayout layouts is not supported.` (verwandt: „Asking for measurement result of unmeasured layout modifier").
**Ursache:** Auf einem SubcomposeLayout-basierten Composable (BoxWithConstraints, LazyColumn/Row, TabRow) wird `Modifier.height(IntrinsicSize.Min/Max)`/`width(IntrinsicSize.*)` verwendet — Intrinsics koennen vor Erhalt der Constraints nicht beantwortet werden.
**Versionen:** by design (1.6 → 1.10); google/accompanist#1065 **CLOSED/COMPLETED** (gh-verifiziert 2026-06-02), issuetracker 254645321.
**FIX:** Intrinsic-Size-Modifier um SubcomposeLayout-Komponenten vermeiden; explizite/`heightIn`-Groessen setzen oder das Layout anders dimensionieren.
**Quelle:** revenuecat.com/blog/engineering/subcomposelayout-internals/

### 6.7 `IllegalStateException` beim MutableState-Write aus Background/Non-Snapshot-Kontext
**Symptom:** `IllegalStateException` beim Schreiben in `mutableStateOf` aus einem Background-Thread/Coroutine vor Commit des erzeugenden Snapshots; verwandt: State-Update auf `Dispatchers.IO` loest keine Recomposition aus.
**Ursache:** State wird geaendert, bevor er in einem committeten Snapshot verfuegbar ist — typisch: Coroutine auf `Dispatchers.IO` aus `init` schreibt sofort in frischen MutableState.
**Versionen:** Compose 1.6 → 1.10; issuetracker 237985810.
**FIX:** State-Writes auf den Main-Thread (`withContext(Dispatchers.Main)`) oder in `Snapshot.withMutableSnapshot { … }` kapseln. Hintergrundarbeit auf IO belassen, nur das State-Update auf Main.
**Quelle:** issuetracker.google.com/issues/237985810

### 6.8 `IllegalArgumentException: invalid weight` bei `Modifier.weight(0f)` aus dynamischen Daten
**Symptom:** Crash `IllegalArgumentException: invalid weight; must be greater than zero`, sobald ein Listenelement mit Wert 0 in eine gewichtete Row/Column gerendert wird (z.B. Anteils-Balken: `weight(count / total)` mit `count == 0`).
**Ursache:** `RowScope/ColumnScope.weight()` verlangt strikt `> 0` (`require(weight > 0.0)`). Bei Gewichten, die aus Server-/Nutzerdaten berechnet werden (Kategorie-Zaehler, Prozent-Anteile), ist 0 ein voellig normaler Datenwert — der Crash ist damit ein klassischer Datenrand-Fall, der im Test (alle Kategorien gefuellt) nie auftritt.
**Versionen:** per Design, alle Compose-Versionen.
**FIX (funktionserhaltend):** Elemente mit Anteil 0 VOR dem Rendern der gewichteten Reihe herausfiltern (`entries.filter { it.value > 0 }`) — optisch identisch, da 0-Anteile ohnehin keine Breite haetten. Alternativ `coerceAtLeast(0.0001f)`, wenn das Element sichtbar bleiben soll. (Gefunden 2026-07-02 im CortexAndroid-Dashboard-Spektrum-Balken.)
**Quelle:** developer.android.com Referenz `RowScope.weight` (require weight > 0)

---

## 7. Navigation-Compose (2.7 / 2.8)

### 7.1 `rememberSaveable`/`SavedStateHandle` nicht wiederhergestellt nach Process Death   ⭐ HAEUFIG
**Symptom:** Nach Process Death + Recreation ist `rememberSaveable`-State von Destinationen weg / `SavedStateHandle`-Werte leer.
**Ursache:** Interop zwischen `rememberSaveable`/`SavedStateHandle` und dem NavHost; SavedState am `NavBackStackEntry` nicht korrekt restauriert.
**Versionen:** **Grundfall gefixt** durch Navigation-Abhaengigkeit auf Lifecycle 2.6.2 (in navigation-compose 2.8.7 enthalten). **ABER:** verwandte Restfaelle bleiben gemeldet (nested graphs §7.2; Lazy-Layout-Items issuetracker **400569084, offen**) — siehe Fix-Status §11. **issuetracker 298059596.**
**FIX (sicher + funktionserhaltend):** Kritischen State ins destination-scoped ViewModel mit `SavedStateHandle` heben — ueberlebt Navigation + Process Death zuverlaessig, unabhaengig vom Restfall-Status. State NIE wegwerfen.
**Quelle:** issuetracker.google.com/issues/298059596

### 7.2 Verschachteltes (nested) NavGraph stellt `rememberSaveable` nicht wieder her
**Symptom:** In nested Graphen geht der `rememberSaveable`-State von Sub-Graph-Screens beim Zurueck-/Wechseln verloren.
**Ursache:** SaveableStateHolder-Scoping ueber nested Graph-Grenzen wird nicht korrekt durchgereicht.
**Versionen:** navigation-compose 2.4–2.7-Linie; in spaeteren Releases verbessert; CMP-Aspekt **JetBrains#4735 CLOSED/COMPLETED** (gh-verifiziert 2026-06-02). issuetracker 191059138.
**FIX:** ViewModel an `navController.getBackStackEntry("<parent_route>")` scopen statt `rememberSaveable` ueber Graph-Grenzen.
**Quelle:** issuetracker.google.com/issues/191059138; github.com/JetBrains/compose-multiplatform/issues/4735

### 7.3 `hiltViewModel` am falschen NavBackStackEntry → kein geteiltes ViewModel   ⭐ HAEUFIG
**Symptom:** Jeder Screen bekommt eine eigene ViewModel-Instanz statt der geteilten; geteilter State geht verloren. Oder ein vermeintlich screen-lokales ViewModel ueberlebt zu lange.
**Ursache:** `hiltViewModel()` nimmt per Default den `LocalViewModelStoreOwner` = aktueller `NavBackStackEntry`.
**Versionen:** alle navigation-compose-Versionen (Verhalten, kein Versionsfix).
**FIX:** `val parentEntry = remember(it) { navController.getBackStackEntry("parent_route") }` + `hiltViewModel(parentEntry)`. Entry in `remember` cachen; nie tief im Stack liegende Entries blind greifen.
**Quelle:** developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis

### 7.4 Type-safe Navigation (2.8): Crash mangels `NavType` fuer komplexe Argument-Typen   ⭐ HAEUFIG
**Symptom:** Crash beim Graph-Aufbau bzw. `toRoute()`: `IllegalArgumentException: Route could not find any NavType for argument` bzw. `Cannot cast ... to a NavType`.
**Ursache:** Bei type-safe Routes mit eigenen Datentypen ist kein passender `NavType` registriert (in fruehen 2.8-Alphas zudem ein `toRoute()`-Bug bei custom NavTypes).
**Versionen:** navigation-compose 2.8.0-alpha…; custom-NavType-in-`toRoute()` **gefixt ab 2.8.0-beta01**. Eigene NavTypes weiterhin manuell noetig (bis 2.8.7).
**FIX:** Eigenen `NavType` implementieren + registrieren: `composable<Route>(typeMap = mapOf(typeOf<MyType>() to MyNavType))`.
**Quelle:** developer.android.com/guide/navigation/design/type-safety

### 7.5 Type-safe Route mit `"null"`/leerem String crasht/matcht nicht
**Symptom:** Crash beim Navigieren wenn ein String-Argument `"null"` ist; leere Strings → malformter URL-Path, Route matcht nicht.
**Ursache:** Strings landen im URL-Path; bei `"null"`/leer wird der Path fehlerhaft gebaut/abgeglichen.
**Versionen:** navigation-compose 2.8.0; `"null"`-Fall **gefixt in 2.8.1**.
**FIX:** Auf 2.8.1+ (2.8.7 erfuellt); fuer optionale/leere Strings nullable-Typ + Default statt Leerstring.
**Quelle:** developer.android.com/jetpack/androidx/releases/navigation

### 7.6 Grosse Objekte als type-safe Route-Argument → `TransactionTooLargeException`
**Symptom:** Crash `TransactionTooLargeException` bei/nach Navigation, besonders nach Process Death.
**Ursache:** Die Route wird per Binder-Transaktion persistiert (~1 MB Puffer); grosse Argument-Objekte sprengen es.
**Versionen:** navigation-compose 2.8.x type-safe Navigation.
**FIX:** Nur IDs/kleine Werte navigieren; grosse Objekte ueber geteiltes ViewModel/Repository laden.
**Quelle:** medium.com/androiddevelopers/type-safe-navigation-for-compose-105325a97657

### 7.7 Bottom-Nav Tab-State-Verlust trotz `saveState`/`restoreState`/`launchSingleTop`   ⭐ HAEUFIG
**Symptom:** Beim Tab-Wechsel werden Screens neu erzeugt, State geht verloren; teils Log `Ignoring popBackStack to destination ... not found on the current back stack`.
**Ursache:** Falsche/unvollstaendige Flag-Kombination beim `navigate()` (popUpTo-Ziel falsch, `saveState`/`restoreState` fehlt) bzw. Save-State-Limit bei nested NavHostController.
**Versionen:** navigation-compose 2.5–2.8-Linie; nested-Aspekt JetBrains#4735 (CLOSED/COMPLETED).
**FIX:** `navigate(route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }`. Nested-Faelle: EINEN NavHost mit Tab-Routen statt mehrerer geschachtelter.
**Quelle:** github.com/JetBrains/compose-multiplatform/issues/4735

### 7.8 Predictive Back animiert nicht / wird abgebrochen
**Symptom:** Vorhersagbare Zurueck-Geste zeigt keine Vorschau-Animation, oder die Transition bricht ab wenn der Back-Stack geleert wird.
**Ursache:** Voraussetzungen fehlen (navigation-compose < 2.8 oder Material3 < 1.3.0; `android:enableOnBackInvokedCallback` nicht gesetzt) + Cancel-Bug bei geleertem Back-Stack.
**Versionen:** braucht navigation-compose 2.8.0+ und Material3 1.3.0+; issuetracker 360361303.
**FIX:** navigation-compose 2.8+, Material3 1.3+, `enableOnBackInvokedCallback="true"` setzen.
**Quelle:** issuetracker.google.com/issues/360361303

### 7.9 Deep-Link mit nested Graphen / encodierten Argumenten matcht nicht
**Symptom:** Deep-Link in nested NavGraph baut den Back-Stack nicht korrekt auf; nach Deep-Link-Start auf Nicht-Start-Destination schlaegt Folge-Navigation fehl; URI-Sonderzeichen brechen das Matching.
**Ursache:** Fehlerhafter Back-Stack-Aufbau bei nested Deep-Links + Encoding-Probleme reservierter URI-Zeichen.
**Versionen:** navigation-compose 2.5–2.7-Linie (mehrere Reports), teils in 2.8 verbessert; issuetracker 214383060.
**FIX:** Argumente korrekt URL-encoden; Deep-Link auf der korrekten (Parent-)Graph-Ebene definieren; Back-Stack-Verhalten nach Update verifizieren.
**Quelle:** issuetracker.google.com/issues/214383060

---

## 8. Material3, Theming & Insets (Material3 1.2 → 1.4, Ausblick 1.5-alpha)

### 8.1 Scaffold `innerPadding` ignoriert → Content unter TopBar/hinter NavBar   ⭐ HAEUFIG
**Symptom:** Inhalt liegt unter der TopAppBar oder hinter der System-NavBar — verschaerft seit Android 15 (SDK 35, edge-to-edge erzwungen).
**Ursache:** Das `innerPadding`/`PaddingValues` von `Scaffold` wird nicht (oder doppelt) angewendet. Lint: `UnusedMaterial3ScaffoldPaddingParameter`.
**Versionen:** Material3 alle (1.2–1.4); Schweregrad steigt mit compileSdk 35.
**FIX:** `innerPadding` an den direkten Content (`Modifier.padding(innerPadding)`/`contentPadding`). Wenn eine Kante zu viel ist (z.B. scrollende Liste): PaddingValues-Kopie mit nur den gewollten Kanten. KEINE zusaetzlichen `windowInsetsPadding`-Modifier zusaetzlich zum Scaffold-Padding (sonst doppelt).
**Quelle:** issuetracker.google.com/issues/249727298

### 8.2 ModalBottomSheet von der Tastatur (IME) verdeckt / Content springt   ⭐ HAEUFIG
**Symptom:** Beim Fokussieren eines TextFields im `ModalBottomSheet` deckt die Tastatur den Inhalt ab; Buttons springen/ueberlappen.
**Ursache:** `ModalBottomSheet` verarbeitet IME-Insets nicht zuverlaessig automatisch.
**Versionen:** Material3 1.1–1.4 (lang offen); issuetracker 268380384.
**FIX:** Im Sheet-Content `Modifier.imePadding()` und/oder `WindowInsets.ime` auswerten; `windowInsets`-Parameter des Sheets bewusst setzen.
**Quelle:** issuetracker.google.com/issues/268380384

### 8.3 ModalBottomSheet wird nicht dismissed bei programmatischem Zustand
**Symptom:** Das Sheet schliesst nicht, obwohl der State auf hidden/dismiss gesetzt wurde; inkonsistenter Sichtbarkeitszustand.
**Ursache:** `sheetState.hide()` ist eine **suspend**-Animation, die abgeschlossen sein muss, bevor das Composable (boolean-Flag) entfernt wird.
**Versionen:** Material3 1.1.x–1.2.x (in spaeteren verbessert); issuetracker 281967264.
**FIX:** `scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) showSheet = false }` — erst Animation beenden, dann Flag.
**Quelle:** issuetracker.google.com/issues/281967264

### 8.4 ExposedDropdownMenuBox: Dropdown erscheint nicht ohne `Modifier.menuAnchor()`   ⭐ HAEUFIG
**Symptom:** Nach Material3-Migration oeffnet das ExposedDropdownMenu gar nicht / positioniert falsch.
**Ursache:** `Modifier.menuAnchor()` fehlt am (Outlined)TextField — in Material3 zwingend als Anker. Alte Signatur deprecated.
**Versionen:** Material3 1.1–1.4; neue Anchor-Type-API ab 1.3.x; issuetracker 323694447.
**FIX:** `Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)` am TextField.
**Quelle:** issuetracker.google.com/issues/323694447

### 8.5 `dynamicColor` crasht / falsche Farben unter Android 12
**Symptom:** Falsche Theme-Farben auf Geraeten < Android 12, oder NoSuchMethod bei `dynamic*ColorScheme()`.
**Ursache:** `dynamicLightColorScheme()/dynamicDarkColorScheme()` existieren erst ab API 31; ohne Guard kein Fallback.
**Versionen:** Material3 alle; Plattform-bedingt (< API 31).
**FIX:** `if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamic…ColorScheme(context) else Light/DarkColorScheme`. Statisches Fallback-Schema sauber definieren.
**Quelle:** developer.android.com/develop/ui/compose/designsystems/material3

### 8.6 MaterialTheme/CompositionLocal nicht im Scope (Dialog/Sheet/Preview)
**Symptom:** Theme-Werte fehlen / Komponenten rendern mit Standard-Defaults statt App-Theme — typisch in Previews oder im Dialog/BottomSheet-Window.
**Ursache:** Komponente liegt ausserhalb des `MaterialTheme{}`-Scopes; Dialoge/Sheets oeffnen ein eigenes Window/Composition.
**Versionen:** Material3 alle.
**FIX:** Content von Dialog/Sheet erneut in `MaterialTheme{}` (App-Theme) wrappen; Previews ins Theme einpacken.
**Quelle:** developer.android.com/develop/ui/compose/designsystems/material3

### 8.7 Material3 1.4.0 Breaking Changes (Icons nicht mehr transitiv, NavBar-Label-Farbe)  ⭐ (Re-Recherche 2026-06-24, trifft EntropieReductor)
**Symptom (a):** Nach Upgrade auf **Material3 1.4.0** brechen Icon-Referenzen mit "unresolved reference"
(`Icons.Filled.*` / `Icons.Default.*` nicht gefunden), obwohl vorher alles baute.
**Ursache (a):** Material3 1.4.0 zieht **`material-icons-core` nicht mehr transitiv** mit — die Icon-Packs
muessen explizit als Dependency deklariert werden.
**FIX (a, funktionserhaltend):** `androidx.compose.material:material-icons-core` (bzw. `-extended`)
explizit in den Dependencies/Version-Catalog aufnehmen. Icons NICHT entfernen.
**Symptom (b):** Aktives Label in `NavigationBar`/`NavigationRail` hat ploetzlich eine andere Farbe.
**Ursache (b):** In 1.4.0 ist die Default-Farbe des aktiven Labels von `NavigationBarItem`/
`NavigationRailItem` **`secondary` statt `onSurface`**. Auch ein neues `MotionScheme` wirkt auf alle Komponenten.
**FIX (b):** Gewollte Farbe explizit per `NavigationBarItemDefaults.colors(...)` setzen, falls das alte
Aussehen erwuenscht ist.
**Weiteres:** In 1.4.0-beta01 wurden alle `@ExperimentalMaterial3ExpressiveApi`/`…ComponentOverrideApi`-
APIs aus der stabilen Linie entfernt (Expressive nur in der Alpha-Linie). Neue 1.4.0-Komponenten:
`SecureTextField`/`OutlinedSecureTextField`, Text-Auto-Size, unified `TimePickerDialog`, neues
SearchBar-Modell (`SearchBarState` + `ExpandedFullScreenSearchBar`).
**Versionen:** Material3 1.4.0 (released 2025-09-24). EntropieReductor ist bereits auf 1.4.0.
**Quelle:** developer.android.com/jetpack/androidx/releases/compose-material3 (1.4.0 Release-Notes)

### 8.8 Gewichteter Langtext-Editor kollabiert bei `adjustResize` + IME
**Symptom:** Nach dem Fokussieren eines langen `BasicTextField` öffnet sich die Tastatur, das Textfeld schrumpft auf wenige Zeilen und die angetippte Cursorstelle ist nicht mehr sichtbar.
**Ursache:** `adjustResize` reduziert die verfügbare Fensterhöhe, während feste Geschwister wie Kopfzeile, Namensfeld, Zusatzkarten und Aktionsbuttons ihre Höhe behalten. Ein per `weight(1f)` dimensioniertes Textfeld erhält nur den verbleibenden, nahezu aufgebrauchten Platz.
**FIX (funktionserhaltend):** Bei fokussiertem Langtextfeld und sichtbarer `WindowInsets.ime` die festen Geschwister ausblenden und dieselbe Textfeld-Instanz den verfügbaren Bereich oberhalb der Tastatur ausfüllen lassen. Beim Schließen der IME die Aktionen wieder einblenden. Das Textfeld nicht durch eine zweite Instanz ersetzen, damit Cursor, Auswahl und interner Scrollzustand erhalten bleiben.
**Quelle:** eigener Vorfall PerfectMoment 2026-07-20.

---

## 9. Animation

### 9.1 `animate*AsState` startet bei jeder Recomposition neu (SideEffect-Falle)   ⭐ HAEUFIG
**Symptom:** Animation stottert, springt zurueck oder startet endlos neu statt einmal sauber zu laufen.
**Ursache:** Der `targetValue` wird per `SideEffect { targetValue = … }` gesetzt — `SideEffect` laeuft bei JEDER Recomposition und resettet das Ziel. Auch: `tween(delayMillis=…)` legt die Verzoegerung auf jede Iteration.
**Versionen:** alle (1.6 → 1.10), API-Verhalten.
**FIX:** Ziel nicht in `SideEffect` setzen. Einmal-Trigger `LaunchedEffect(Unit) { targetValue = 1f }`; Endlos-Animation `rememberInfiniteTransition()` + `animateFloat`. Target-State in `remember` halten.
**Quelle:** touchlab.co/jetpack-compose-animations-state-change

### 9.2 `animateFloatAsState` animiert nicht (kein State-Trigger)
**Symptom:** Der animierte Wert bleibt konstant, gar keine Animation.
**Ursache:** `targetValue` ist hartkodiert / aendert sich nie → keine State-Mutation startet die Animation.
**Versionen:** alle, API-Verhalten.
**FIX:** `targetValue` an einen sich tatsaechlich aendernden `mutableStateOf`-Wert binden (nach `LaunchedEffect` oder User-Event).
**Quelle:** developer.android.com/develop/ui/compose/animation/value-based

### 9.3 AnimatedContent/Crossfade — Aufblitzen alter Inhalte / unerwuenschte Animation
**Symptom:** Beim Wechsel blitzt der alte Inhalt kurz auf, oder es wird animiert obwohl sich inhaltlich nichts geaendert hat.
**Ursache:** Im Lambda wird `state` statt des uebergebenen `targetState` reflektiert; oder der ganze `targetState` dient als Key, obwohl sich nur eine Teil-Eigenschaft aendert.
**Versionen:** betroffen 1.6.x; Interruption-/Size-Continuity-Fixes in spaeteren animation-Releases (bis BOM 2026.03.00); issuetracker 314184086.
**FIX:** Im Lambda IMMER den uebergebenen `targetState` reflektieren; `contentKey = { it.id }` setzen, damit nur bei echter Inhaltsaenderung animiert wird.
**Quelle:** developer.android.com/jetpack/androidx/releases/compose-animation

### 9.4 `rememberInfiniteTransition` → Dauer-Recomposition / Akku bei falschem State-Read
**Symptom:** Hohe CPU-Last, warmes Geraet bei simpler Endlos-Animation; Profiler zeigt staendige Recompositions.
**Ursache:** Der animierte Wert wird im Composition-Body gelesen (statt in einem Draw-/Layer-Lambda) → ganzer Teilbaum recomposed pro Frame.
**Versionen:** alle (1.6 → 1.10).
**FIX:** Animierten Wert nur in deferred-Lambdas lesen: `Modifier.graphicsLayer { alpha = anim.value }`/`offset { }`/`drawBehind { }`. Endlos-Animation pausieren wenn nicht sichtbar.
**Quelle:** developer.android.com/develop/ui/compose/performance/bestpractices

### 9.5 `animateContentSize` Sprung / RTL-Fehlausrichtung
**Symptom:** Beim animierten Groessenwechsel springt der Inhalt seitlich/ist falsch ausgerichtet, v.a. mit `Alignment.End/CenterHorizontally` oder RTL.
**Ursache:** Ausrichtungs-Offsets bei RTL falsch berechnet; Container startete nicht von der aktuellen Groesse.
**Versionen:** betroffen ~1.6.x; **gefixt** in spaeterer animation-Release (Tracking startet von aktueller Groesse) — mit BOM 2026.03.00 (~1.8/1.9) behoben; issuetracker 300134177.
**FIX:** compose-animation auf aktuelle BOM heben; bei Restsprung `animateContentSize` VOR Ausrichtungs-/Padding-Modifiern platzieren, Alignment am aeusseren Container.
**Quelle:** issuetracker.google.com/issues/300134177

### 9.6 `rememberInfiniteTransition` unbedingt erzeugt, Wert aber nur in EINEM Zustand gelesen → Dauer-Animations-Ticks (Energie)
**Symptom:** Keine sichtbare Recomposition-Last, aber dauerhafte Frame-Callbacks/Animations-Ticks (Akku!), solange die Composable im Tree ist — auch wenn die Animation optisch gar nicht laeuft. Typisch: Status-Pill in einer immer sichtbaren TopBar mit Puls-Effekt nur fuer den Zustand „verbinde…".
**Ursache:** `rememberInfiniteTransition` + `animateFloat` laufen IMMER weiter, sobald sie Teil der Composition sind — unabhaengig davon, ob irgendjemand die Werte liest. Wird der Wert nur in einem `if (state == X)`-Zweig gelesen, tickt die Animation in ALLEN anderen Zustaenden trotzdem endlos.
**Versionen:** per Design, alle Compose-Versionen.
**FIX (funktionserhaltend + optisch identisch):** Transition UND `animateFloat` IN den Zustands-Zweig verschieben (`if (state == X) { val t = rememberInfiniteTransition(); … }`) — bedingtes `remember` ist in Compose erlaubt; beim Zustandswechsel wird die Animation sauber disposed/neu erstellt. (Gefunden 2026-07-02, CortexAndroid VPN-Pill: Puls tickte in jedem App-Zustand.)
**Quelle:** developer.android.com/develop/ui/compose/animation (InfiniteTransition-Lebensdauer = Composition-Lebensdauer)

---

## 10. Performance, Debug-vs-Release & Tooling

### 10.1 „Compose ist langsam" — Debug-Build als echte Performance fehlgedeutet   ⭐ HAEUFIG
**Symptom:** App ruckelt stark beim Deploy aus Android Studio; in Release ploetzlich fluessig.
**Ursache:** Debug: Compose als Library (nicht vorkompiliert), Live Literals, ART interpretiert anfangs. Kein Bug — Debug-Overhead.
**Versionen:** alle (1.6 → 1.10).
**FIX:** Performance IMMER im Release-Build mit `minifyEnabled true` (R8) messen, nie im Debug. Wichtigster Compose-Performance-Schritt. *(Build-Config selbst → gradle.md.)*
**Quelle:** medium.com/androiddevelopers/why-should-you-always-test-compose-performance-in-release-4168dd0f2c71

### 10.2 Fehlende Baseline Profiles → langsamer erster Start / Jank
**Symptom:** Erster App-Start nach Install/Update langsam, anfangs ruckelnde Scrolls; danach besser.
**Ursache:** Ohne Baseline Profile laeuft Compose zunaechst interpretiert/JIT — der vorkompilierte Pfad fehlt fuer kritische Journeys.
**Versionen:** alle (Compose liefert ein eigenes Profil mit; app-spezifisches zusaetzlich noetig).
**FIX:** `androidx.baselineprofile`-Plugin + Benchmark-Modul; Baseline Profile fuer kritische Journeys generieren (mit R8 schliesst das die Debug/Release-Luecke groesstenteils). *(Build-Setup → gradle.md.)*
**Quelle:** developer.android.com/develop/ui/compose/performance

### 10.3 State im Composition-Body statt im `graphicsLayer{}`/`offset{}`/`drawBehind{}`-Lambda gelesen   ⭐ HAEUFIG
**Symptom:** Jank beim Scrollen/Animieren; Layout-Inspector zeigt extrem hohe Recomposition-Counts am ganzen Teilbaum.
**Ursache:** Haeufig wechselnder State (Scroll-Offset, Animationswert) wird direkt im Body gelesen → Composition + Layout + Draw pro Frame statt nur Draw.
**Versionen:** alle (1.6 → 1.10).
**FIX:** Lambda-Version von `graphicsLayer { }`/`offset { }`/`drawBehind { }` nutzen (Read in Draw-/Layout-Phase). WICHTIG: Read komplett ins Lambda ziehen — wird der Wert aussen berechnet, recomposed es trotzdem.
**Quelle:** github.com/skydoves/compose-performance-skills (deferring-state-reads)

### 10.4 Haeufig wechselnder Wert als Parameter statt als `() -> T`-Lambda   ⭐ HAEUFIG
**Symptom:** Eltern-Composable recomposed komplett, obwohl sich nur ein Wert tief unten aendert.
**Ursache:** Ein oft wechselnder State wird als direkter Parameter durchgereicht → die Recomposition-Scope ist der Parent. Erst der Read im Kind macht die Scope minimal.
**Versionen:** alle (1.6 → 1.10).
**FIX:** Wert als `() -> T`-Lambda uebergeben (`scrollOffset = { state.value }`), Read erst im Kind.
**Quelle:** developer.android.com/develop/ui/compose/performance/bestpractices

### 10.5 Teure Arbeit (sort/filter/format/Bitmap) direkt im Composable-Body   ⭐ HAEUFIG
**Symptom:** Ruckeln beim Scrollen/Tippen; Sortierung/Filter/Formatierung laeuft bei jeder Recomposition.
**Ursache:** `list.sorted()`/`filter`/Datumsformatierung unguarded im Body.
**Versionen:** alle (1.6 → 1.10).
**FIX:** Ergebnis mit `remember(key) { … }` cachen (Key = Eingabe) oder ins ViewModel/Repository auslagern. Abgeleiteter State aus haeufig wechselnden Quellen → `derivedStateOf` (§2.10-Regel beachten).
**Quelle:** developer.android.com/develop/ui/compose/performance/bestpractices

### 10.6 `@Preview` crasht mit `hiltViewModel()` / echtem ViewModel   ⭐ HAEUFIG
**Symptom:** Preview rendert nicht / „Render problem" / Crash, sobald `hiltViewModel()` oder ein DI-ViewModel verwendet wird.
**Ursache:** Hilt baut den Graph erst zur Laufzeit; Preview rendert ohne Application-Context.
**Versionen:** alle (Hilt-/Tooling-bedingt).
**FIX:** Stateless Content-Composable trennen, das nur Daten als Parameter bekommt; im `@Preview` Mock-Daten geben. Wo noetig `LocalInspectionMode.current` pruefen. ViewModel nie direkt im Preview instanziieren.
**Quelle:** developer.android.com/develop/ui/compose/tooling/previews

### 10.7 Layout-Inspector Recomposition-Counts falsch gedeutet
**Symptom:** Optimierung anhand hoher Counts, obwohl die App nicht ruckelt — oder echte Probleme uebersehen.
**Ursache:** Count ist nur ein indirektes Mass; ein teures Composable mit wenig Counts kann mehr kosten als viele billige. Zusatzfalle: LazyColumn ohne `key` zeigt aufgeblaehte Counts.
**Versionen:** alle (Tooling-Interpretation).
**FIX:** Counts nur als Hinweis; echte Frame-Zeit/Jank im Release messen (Macrobenchmark/JankStats). Erst stabile `key` setzen (§4.1), dann Counts deuten.
**Quelle:** medium.com/androiddevelopers/jetpack-compose-debugging-recomposition-bfcf4a6f8d37

### 10.8 Unstable Params ohne Compiler-Reports = Blindflug
**Symptom:** Composables recomposen, obwohl sich Parameter scheinbar nicht aendern; Optimierung ohne Datengrundlage.
**Ursache:** Instabile Parameter → Composable recomposed immer mit dem Parent. Ohne Compiler-Reports sieht man nicht, welche skippable/restartable sind.
**Versionen:** alle; Strong Skipping (ab Compiler 2.0.20) entschaerft viele Faelle (siehe §1.3).
**FIX:** Compose-Compiler-Reports aktivieren (`composeCompiler { reportsDestination/metricsDestination }`), IMMER auf Release laufen lassen; `classes.txt`/`composables.txt` auswerten und instabile Klassen stabilisieren. *(Plugin-Setup → gradle.md.)*
**Quelle:** developer.android.com/develop/ui/compose/performance/stability/diagnose

### 10.9 `remember` mit zu breitem Key — Memoization laeuft trotzdem jeden Frame   ⭐ EIGENER VORFALL 2026-07-03
**Symptom:** Interaktion (Chart-Scrubbing, Drag, Slider) ruckelt, OBWOHL die teure Berechnung in `remember(...)` steckt.
**Ursache:** Ein hochfrequent wechselnder Wert (z.B. `selectedSnapshot` beim Scrubbing — aendert sich pro Move-Event) steht MIT im `remember`-Key → jede Aenderung invalidiert den Cache, die teure Pipeline (groupBy/sortedMap ueber die ganze Historie) laeuft de facto pro Frame. Das `remember` sieht korrekt aus, ist aber fuer den heissen Fall wirkungslos. EntropieReductor-Vorfall (#47449): drei Biomarker-Graph-Karten mit `remember(selectedSnapshot, history) { groupBy-Pipeline }`.
**Versionen:** per Design (alle Compose-Versionen).
**FIX (funktionserhaltend):** Heavy/Finalize-Split: die datenabhaengige Schwerarbeit in `remember(data) { heavy(data) }`, den billigen selektionsabhaengigen Rest in `remember(selected, heavy) { finalize(selected, heavy) }`. Auch abgeleitete Listen (z.B. `reversed()`) in den Heavy-Teil ziehen (sonst Allokation pro Frame). Alternativ `derivedStateOf`, wenn der Selektionswert ein Compose-State ist.
**Quelle:** eigener Vorfall EntropieReductor #47449 · developer.android.com/develop/ui/compose/performance (remember-Guidance)

---

## 11. Fix-Status — was belegt behoben ist und was offen bleibt

> **Ehrlichkeits-Pflicht (Skill-Vorgabe):** Strikt getrennt nach Beleg-Staerke. Compose-Bugs
> liegen ueberwiegend auf **issuetracker.google.com** (Googles Buganizer) — dieser ist
> **NICHT** ueber `gh` pruefbar. Nur GitHub-Issues konnten hart per `gh issue view` verifiziert
> werden.

### Hart per `gh` verifiziert (2026-06-02)
| Issue | Status | Bezug |
|-------|--------|-------|
| JetBrains/compose-multiplatform #4279 | **CLOSED / COMPLETED** (LazyColumn-in-HorizontalPager-Scroll, iOS) | §4.8 |
| JetBrains/compose-multiplatform #4735 | **CLOSED / COMPLETED** (Save-State fuer nested NavHostController) | §7.2, §7.7 |
| google/accompanist #847 | **CLOSED / COMPLETED** (HorizontalPager measure crash) — *accompanist deprecated* | §6.2-nah |
| google/accompanist #1065 | **CLOSED / COMPLETED** (unmeasured/intrinsic layout) | §6.6 |

### Belegt gefixt laut Release-Notes (developer.android.com)
| Frueherer Bug | Gefixt ab | Bezug |
|---------------|-----------|-------|
| Lambda mit unstable Captures nicht memoiziert | **Compose 1.7 / Compiler 2.0.20** (Strong Skipping) | §1.4 |
| Endlos-Recomposition durch falsche Stability-Inferenz | **Compose-Compiler 2.0.10/2.0.20** | §1.1-nah (vgl. kotlin.md §11) |
| type-safe Route String `"null"` crasht | **navigation-compose 2.8.1** | §7.5 |
| custom `NavType` in `toRoute()` | **navigation-compose 2.8.0-beta01** | §7.4 |
| `rememberSaveable`/`SavedStateHandle`+NavHost Grundfall | **via Lifecycle 2.6.2** (in nav 2.8.7) | §7.1 |
| `animateContentSize` RTL/Sprung | **~Compose 1.8/1.9 (BOM 2026.03.00)** | §9.5 |
| Compose-Compiler-Version-Matching (manuelle Map) | **entfaellt ab Kotlin 2.0** (Plugin synchron) | gradle.md |

### Noch NICHT gefixt / per Design (Workaround bleibt aktiv)
- **Mechanik-Fallen** (Backwards Write §1.1, Stabilitaet §1.2/§1.3, State §2.x, Side-Effects §3.x, Modifier §5.x, Lazy-Keys §4.1/§4.2, deferred Reads §10.3) = **per Design / versionsunabhaengig** (Denkfehler, kein „Fix" zu erwarten).
- **issuetracker 425901162** (Lifecycle-Events bei Navigation falsch → LaunchedEffect mehrfach, §3.9) — **offen**.
- **issuetracker 400569084** (`rememberSaveable` in Lazy-Layout-Items, §7.1-nah) — **offen**.
- **issuetracker 298059596 / 191059138** (rememberSaveable+Navigation) — Grundfall via Lifecycle 2.6.2 gemildert, **verwandte Restfaelle gemeldet** → konservativ ViewModel+SavedStateHandle nutzen.
- **issuetracker 268380384** (ModalBottomSheet IME §8.2), **249727298** (Scaffold padding/Lint §8.1), **323694447** (ExposedDropdownMenu §8.4) — Verhalten/Workaround bleibt.
- **180042685** (kein Default-Saver §2.8), **KTIJ-16966** (IDE-Import §2.4) — per Design/won't-fix-artig.

**Methodik-Hinweis:** issuetracker.google.com-Detailseiten sind JS-gerendert/login-pflichtig und liessen sich per WebFetch nur eingeschraenkt lesen — die `issuetracker`-Status oben stammen aus offiziellen developer.android.com-Release-Notes + Researcher-Findings und sind **nicht** hart wie die vier GitHub-Issues verifiziert. Bei naechster Re-Recherche: falls Compose-Issues nach GitHub gespiegelt werden, per `gh issue view` hart pruefen.

---

## Pflicht-Checkliste vor Compose-UI-Arbeit
- [ ] Diese Datei gelesen, Stand-Datum gegen die genutzte Compose-BOM/Material3/navigation-Version abgeglichen?
- [ ] Kein State-Write in der Composition (Backwards Write §1.1)? Schreiben nur in Events/Effekten.
- [ ] Lazy-Listen mit stabilem, **eindeutigem** `key` (+`contentType`) (§4.1/§4.2/§4.4)?
- [ ] User-/Item-State ueber Rotation/Recycling abgesichert (`rememberSaveable` klein / ViewModel) (§2.6/§2.7/§2.9)?
- [ ] `LaunchedEffect`-Keys = alle gelesenen veraenderlichen Werte; `rememberUpdatedState` wo noetig (§3.1/§3.3)? Coroutine nie im Body (§3.5)?
- [ ] Stabile Parameter (`@Immutable`/`ImmutableList`), Strong-Skipping-`===`-Falle bedacht (§1.2/§1.3)?
- [ ] `mutableStateListOf` statt `mutableStateOf(list)` (§2.2)? `remember`-Keys gesetzt (§2.3)?
- [ ] Modifier-Reihenfolge bewusst (Layout → Dekoration → Interaktion) (§5.1)?
- [ ] Scaffold-`innerPadding` angewendet, edge-to-edge/IME-Insets bedacht (§8.1/§8.2)?
- [ ] Navigation: geteiltes ViewModel am Parent-Entry, type-safe NavType registriert, Tab-State mit save/restore (§7.3/§7.4/§7.7)?
- [ ] Performance im **Release**-Build (R8) gemessen, hochfrequente Reads deferred (§10.1/§10.3)? `@Preview` stateless (§10.6)?

---

## 🔗 Bezug zu den Best-Practices (Praevention)

Zweite Seite der Medaille: [`best-practices/android/jetpack-compose.md`](../../best-practices/android/jetpack-compose.md)
sagt, wie man jeden dieser Bugs von vornherein vermeidet. Pro Bug-Abschnitt der passende Praeventions-Abschnitt:

| Bug-Abschnitt (DIESE Datei) | Praevention in `best-practices/android/jetpack-compose.md` |
|------------------------------|------------------------------------------------------------------------|
| §1 Recomposition & Stabilitaet (1.1–1.6) | §2 Stabilitaet & Recomposition / Skippability |
| §2 State & `remember` (2.1–2.13) | §3 State & `remember`/`rememberSaveable`/`derivedStateOf`; §2.12/§2.13 → §1 Architektur & UDF |
| §3 Side-Effects (3.1–3.10) | §4 Side-Effects (richtige API je Fall) |
| §4 Lazy-Layouts & Pager (4.1–4.8) | §5 Lazy-Layouts & Pager |
| §5 Modifier (5.1–5.2) | §6 Modifier |
| §6 Crashes (6.1/6.2 nested, 6.6 Subcompose) | §5 Lazy-Layouts; (6.3/6.4/6.5 Context/Owner/Kontext) → §1/§11 |
| §6.7 Background-Write | §4 Side-Effects |
| §7 Navigation-Compose (7.1–7.9) | §8 Navigation-Compose |
| §8 Material3, Theming & Insets (8.1–8.6) | §7 Material3, Theming & Insets; §8.1 → §9 Adaptive Layouts |
| §9 Animation (9.1–9.5) | §10 Animation |
| §10 Performance, Debug-vs-Release & Tooling (10.1–10.8) | §11 Performance, Tooling & Testing |

---

## Wechselseitiger Bezug zu `kotlin.md`

| Thema | jetpack-compose.md (DIESE Datei, vollstaendig) | kotlin.md (Kotlin-Kontext, kompakt) |
|-------|------------------------------------------------|-------------------------------------|
| Recomposition & Stabilitaet | §1 (1.1–1.6) | §6 (6.1–6.3) |
| State & remember | §2 (2.1–2.13) | §7 (7.1–7.4) |
| Side-Effects | §3 (3.1–3.10) | §8 (8.1–8.3) |
| Lazy/Performance | §4, §10 | §9 (9.1–9.2) |
| Flow → Compose (`collectAsStateWithLifecycle`) | §2.13 (Grenzfall) | §5 (Flow-Mechanik) |
| Compose-Compiler-**Plugin**/Build | nur Verweise (§1.4/§10.8) | → **gradle.md** (10.2) |

> Bei jedem neu erlebten Compose-Bug: hier ergaenzen (Bug + funktionserhaltende Loesung + Versionen),
> Stand-Header aktualisieren. Compound Intelligence — der Almanach waechst.
