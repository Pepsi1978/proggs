# Jetpack Compose — Best Practices

**Stand:** 2026-06-02 (Best-Practices-Recherchelauf, 7 Researcher, offizielle Quellen zuerst).
**Lokale Ergänzung 2026-09-07:** Zeichenobjekt-Caching und Animationsneutralität in §11; keine neue Web-Recherche oder Versionsaktualisierung.
**Lokale Ergänzung 2026-09-10, 12:57 Uhr:** Von Frank bestätigter Drag-&-Drop-Standard für Android-/Kotlin-Apps in §5.1; Quelle ist die lokale Referenz GenialeIdeen 1.5.14, keine neue Web-Recherche.
**Versions-Anker (live ermittelt aus den `libs.versions.toml`):**
- **BestJournalAndroid:** Compose **BOM 2025.01.01** (UI ~1.7.6, Material3 1.3.1), Kotlin 2.1.0,
  Compose-Compiler-Plugin 2.1.0, navigation-compose 2.8.7, lifecycle-runtime-compose 2.8.7,
  activity-compose 1.9.3, AGP 8.7.3.
- **EntropieReductor:** Compose **BOM 2026.03.00** (UI ~1.10.x, Material3 1.4.0), Kotlin 2.1.0,
  Compose-Compiler-Plugin 2.1.0, navigation-compose 2.8.7, AGP 8.10.0.
- Beide: **Strong Skipping default aktiv** (seit Compiler 2.0.20 / Compose 1.7), Compose-Compiler
  als Gradle-Plugin (`org.jetbrains.kotlin.plugin.compose`), `collectAsStateWithLifecycle`
  vorhanden (lifecycle 2.8.7). Spanne der Empfehlungen: Compose 1.7 → 1.10, Material3 1.3 → 1.4.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/android/jetpack-compose.md`](../../bugs/android/jetpack-compose.md)):
> der Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von
> vornherein richtig macht, damit der Bug gar nicht erst entsteht*. Quellen-Rangordnung: offizielle
> Android/Google-Quelle (developer.android.com, Android Developers Medium/Blog) = Grundwahrheit
> (`offiziell`), Community/Blogs = `extern` (sekundaer, ueberstimmt nie das Offizielle). Jeder Punkt
> traegt sein `offiziell`/`extern`-Label. Punkte, die erst eine neuere Version als BOM 2025.01.01
> brauchen, sind als „Upgrade-Pfad" markiert — fast alle Compose-Best-Practices sind aber
> versionsunabhaengige Denkmuster und gelten in beiden Projekten.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Architektur/State-Fluss | UDF: State runter, Events rauf; Business-Logik nie ins Composable | §1 |
| 2 | State geteilt/zu tief | State-Hoisting (`value`+`onValueChange`) auf gemeinsamen Vorfahren | §1 |
| 3 | Screen-State + ViewModel-Grenze | EIN immutable State; `collectAsStateWithLifecycle()` + `stateIn` | §1 |
| 4 | Composable skippt nicht | `ImmutableList`/`@Immutable`; `@Immutable` nie luegen | §2 |
| 5 | `remember`/`rememberSaveable` | `by remember { mutableStateOf }` (Imports!); Saveable nur klein | §3 |
| 6 | Liste reaktiv halten | `mutableStateListOf` statt `mutableStateOf(list)` | §3 |
| 7 | Side-Effect waehlen | Richtige API je Fall; Keys = gelesene Werte; kritisch in `viewModelScope` | §4 |
| 8 | Lazy-Liste / Pager | Stabiler eindeutiger `key`+`contentType`; `fillParentMaxSize()` | §5 |
| 15 | Drag & Drop / Reihenfolge in Android-/Kotlin-Apps | Franks Standard: gerade, sichtbare Karte; feste Viewport-Geste; reale Layoutplätze; weiche Nachbarn; bidirektionales Randscrollen | §5.1 |
| 9 | Modifier-Reihenfolge | Layout → `clip`→`background`→`border` → `clickable`; eigene via `Modifier.Node` | §6 |
| 10 | Theming / Insets | `dynamicColor`+API-Guard; Scaffold-`innerPadding`, keine Doppel-Insets | §7 |
| 11 | Navigation | type-safe Routes, nur IDs; geteiltes VM am Parent-Entry | §8 |
| 12 | Tablet/Foldable | `windowSizeClass` statt `Configuration.orientation` | §9 |
| 13 | Animation | `animate*AsState` deklarativ binden, nicht in `SideEffect`/`LaunchedEffect` | §10 |
| 14 | Performance/Preview | Release (R8) messen; Reads deferren; `@Preview` stateless | §11 |

---

## 🔗 Bezug zum Bug-Almanach ([`bugs/android/jetpack-compose.md`](../../bugs/android/jetpack-compose.md))

Jeder Best-Practice-Abschnitt hier ist die **Praevention** zu einem Abschnitt im Bug-Almanach —
zwei Seiten derselben Medaille. Tritt einer der dortigen Bugs auf, liefert der Almanach die konkrete
funktionserhaltende Loesung; dieser Abschnitt sagt, wie man ihn von vornherein vermeidet.

| Best-Practice (hier) | Verhindert Bug(s) in `bugs/android/jetpack-compose.md` |
|----------------------|------------------------------------------------|
| §1 Architektur & UDF + State-Hoisting | §2.12 (State zu tief), §2.13 (`collectAsState`); Grundlage gegen §1.1 (Backwards Write) |
| §2 Stabilitaet & Recomposition / Skippability | §1 (1.1–1.6) |
| §3 State & `remember`/`rememberSaveable`/`derivedStateOf` | §2 (2.1–2.11) |
| §4 Side-Effects | §3 (3.1–3.10); Crash §6.7 (Background-Write) |
| §5 Lazy-Layouts & Pager | §4 (4.1–4.8); Crash §6.1/§6.2 (nested infinity), §6.6 (SubcomposeLayout) |
| §5.1 Drag & Drop — verbindlicher Projektstandard | §4.9 (Sprünge, Flattern, falscher Scrollanker beim Umsortieren) |
| §6 Modifier | §5 (5.1–5.2) |
| §7 Material3, Theming & Insets | §8 (8.1–8.6) |
| §8 Navigation-Compose | §7 (7.1–7.9) |
| §9 Adaptive Layouts (Tablet/Foldable) | §8.1 (edge-to-edge-Verschaerfung); allg. (kein 1:1-Pendant) |
| §10 Animation | §9 (9.1–9.5) |
| §11 Performance, Tooling & Testing | §10 (10.1–10.8); Crash §6.3/§6.4 (Context/Owner), §6.5 (@Composable-Kontext) |

---

## 1. Architektur & unidirektionaler Datenfluss (UDF) + State-Hoisting

- **Grundmuster UDF:** „State flows down, events flow up." Schleife: UI erzeugt Event → State-Holder
  aendert State → State fliesst runter, UI zeigt ihn. Merksatz: **„State is; events happen."** State
  existiert immer und wird konsumiert; Events sind transient (Inputs der State-Produktion).
  *(offiziell: developer.android.com/develop/ui/compose/architecture, /topic/architecture/ui-layer/events)*
- **State-Hoisting (`value` + `onValueChange`):** internen State durch zwei Parameter ersetzen —
  `value: T` (Wert) + `onValueChange: (T) -> Unit` (Aenderungs-Event). Drei Regeln: (1) auf
  **mindestens den niedrigsten gemeinsamen Vorfahren** aller Leser heben, (2) auf **mindestens die
  hoechste Ebene, auf der er geaendert** wird, (3) **zwei States, die auf dasselbe Event reagieren,
  gemeinsam hoisten**. *(offiziell: /develop/ui/compose/state-hoisting)*
- **Property Drilling statt Wrapper-Objekten:** nur die wirklich benoetigten Parameter durchreichen
  (`Header(title, subtitle)`, nicht das ganze `News`-Objekt) — minimiert unnoetige Recompositions
  und macht Verantwortlichkeiten sichtbar. *(offiziell: /develop/ui/compose/state)*
- **Stateful vs. Stateless trennen:** zustandslosen **Content**-Composable (State als Parameter,
  haelt nichts) fuer Reuse/Test/Preview anbieten, dazu optional einen stateful **Wrapper** (haelt
  `remember`-State). Stateless = testbar/wiederverwendbar; stateful = bequem. *(offiziell: /develop/ui/compose/state, /architecture)*
  ```kotlin
  @Composable fun HelloContent(name: String, onNameChange: (String) -> Unit) { /* stateless */ }
  @Composable fun HelloScreen() {                                     // stateful wrapper
      var name by rememberSaveable { mutableStateOf("") }
      HelloContent(name = name, onNameChange = { name = it })
  }
  ```
- **EIN immutable UI-State pro Screen:** Screen-State als eine `data class` mit Default-Werten, ODER
  als `sealed interface` (Loading/Success/Error), wenn sich Zustaende ausschliessen. **Single source
  of truth pro Screen** — keine getrennten `stateFlow1`/`stateFlow2` exponieren; mehrere Quellen per
  `combine()` zu EINER State-Klasse zusammenfuehren. *(offiziell: /topic/architecture/ui-layer/state-production, /stateholders)*
- **State-Holder waehlen:** **Plain State Holder** (`@Stable`-Klasse, in der Composition `remember`t,
  folgt dem UI-Lifecycle, darf `rememberLazyListState()`/`rememberNavController()` halten) fuer
  wiederverwendbare Komponenten + komplexe **UI-Logik**; **ViewModel** fuer Screen-Level-State +
  **Business-Logik** (ueberlebt Activity-Recreation, ein Screen = ein ViewModel). Kompositionsregeln:
  UI-Logik-Holder darf von UI-Logik-Holder abhaengen, Screen-Holder von UI-Logik-Holder — **nie**
  Screen↔Screen oder UI-Logik→Screen; **nie** ein ViewModel in einen plain Holder injizieren.
  *(offiziell: /topic/architecture/ui-layer/stateholders)*
- **`rememberXState()`-Pattern** fuer wiederverwendbare Komponenten: Factory mit Default-Parameter,
  damit der Composable testbar/reusable bleibt:
  `fun MessagesList(messages, lazyListState: LazyListState = rememberLazyListState())`. *(offiziell)*
- **ViewModel↔UI-Grenze:**
  - **`collectAsStateWithLifecycle()`** statt `collectAsState()` (lifecycle-aware, stoppt im
    Hintergrund, spart Ressourcen; in `lifecycle-runtime-compose` 2.8.7). *(offiziell: Medium „Consuming flows safely")*
  - `StateFlow` als primaere Output-API mit
    `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` — Pipeline nur aktiv,
    solange UI sichtbar + 5s Nachlauf (ueberlebt kurze Config-Changes). Read-only exponieren:
    `private val _x = MutableStateFlow(...)` + `val x = _x.asStateFlow()`. *(offiziell: /state-production)*
  - **One-shot-Events zu UI-State reduzieren**, NICHT per `Channel`/`SharedFlow`/`LaunchedEffect(Unit)`:
    wenn der Producer (ViewModel) den Consumer (UI) ueberlebt, gibt es keine Delivery-Garantie →
    Events gehen verloren. Stattdessen Event als Feld (`userMessage: String?`) und nach Anzeige
    zuruecksetzen (`LaunchedEffect(msg){ snackbar...; viewModel.userMessageShown() }`).
    *(offiziell: Medium „ViewModel one-off event antipatterns", /ui-layer/events)*
  - **ViewModel nicht nach unten durchreichen** — Kinder bekommen nur State + Lambdas.
  - **Init-Block meiden:** kein async im ViewModel-Konstruktor — `stateIn` defert die Pipeline bis
    ein Subscriber da ist. *(offiziell)*
  - **Caveat:** UI-State-Suspend-Funktionen (`LazyListState.animateScrollToItem()`,
    `DrawerState.close()`) brauchen die `MonotonicFrameClock` → aus `rememberCoroutineScope()`
    aufrufen, nicht aus `viewModelScope` (sonst `IllegalStateException`). *(offiziell)*
- **Wo Compose-Logik NICHT hingehoert:** keine Business-Logik im Composable; Composables sind rein
  praesentational, **idempotent & seiteneffektfrei**, treffen keine Annahmen ueber die
  Ausfuehrungsreihenfolge und aendern State nur ueber Event-Handler. UI-Logik (Scroll/Navigation/
  Hinweise) → Composable/plain Holder; Business-Logik (DB-Save, Bookmark) → Domain/Data-Layer.
  *(offiziell: /architecture, /stateholders)*

---

## 2. Stabilitaet & Recomposition / Skippability

- **Grundbegriffe:** *stabil* = Compose weiss, ob sich ein Wert zwischen Recompositions geaendert hat;
  *skippable* = Composable kann uebersprungen werden, wenn alle Argumente gleich sind; *restartable*
  = Einstiegspunkt, ab dem Recomposition neu starten kann. `@Immutable` = Properties aendern sich nie
  nach Konstruktion (staerkstes Versprechen); `@Stable` = duerfen sich aendern, aber Compose wird
  informiert (z.B. via Compose-State). Stabil-trotz-Mutability sind die Compose-Typen `MutableState`,
  `SnapshotStateList`, `SnapshotStateMap`. *(offiziell: /performance/stability)*
- **Wie der Compiler entscheidet:** `data class` mit nur `val` + stabilen Feldtypen → stabil; ein
  einziges `var` oder ein instabiles Feld → instabil. *(offiziell)*
- **Strong Skipping RICHTIG verstehen** (default seit Compiler 2.0.20 / Compose 1.7 — gilt fuer beide
  Projekte): (a) alle restartable Composables werden skippable, auch mit instabilen Params —
  Vergleich: **stabile Params per `equals()`, instabile per `===` (Referenz)**; (b) Lambdas werden
  automatisch memoiziert (auch mit instabilen Captures). **Was es NICHT loest:** `copy()`/neue Instanz
  mit gleichen Daten → andere Referenz → `===` false → **kein Skip**; ersetzt `@Immutable`/`@Stable`
  **nicht** (wenn man Wert- statt Referenzgleichheit will). Opt-out selten: `@NonSkippableComposable`/
  `@DontMemoize`. *(offiziell: /performance/stability/strongskipping, Medium „Strong Skipping Mode Explained")*
- **Stabile Typen bauen:** alle Properties `val` + immutable Feldtypen; veraenderliche Felder per
  `mutableStateOf(...)` backen (bleibt stabil, weil die Runtime die Aenderung mitbekommt).
  `@Immutable`/`@Stable` **nie luegen** — sie sind ein Vertrag; falsch annotiert bricht Recomposition
  (UI updatet nicht mehr). *(offiziell: /performance/stability/fix)*
- **Collections sind immer instabil** (`List`/`Set`/`Map` sind Interfaces — read-only ≠ stable):
  Loesung A (empfohlen, compile-time-sicher) `kotlinx.collections.immutable`
  (`ImmutableList`/`persistentListOf()`); Loesung B `@Immutable`-Wrapper um die Liste.
  *(offiziell: /performance/stability/fix)*
- **Fremd-Modul-Typen** (kein Compose-Compiler, z.B. `java.time.LocalDateTime`) gelten als instabil →
  **Stability-Configuration-File** (`.conf`, eine Klasse/Pattern pro Zeile, Wildcards) per
  `composeCompiler { stabilityConfigurationFile = ... }`, oder eigener `@Immutable`-Wrapper.
  *(offiziell: /performance/stability/fix)* *(Plugin-Setup im Build → [`gradle.md`](../android-build/gradle.md))*
- **Compiler-Reports/Metrics** (`composeCompiler { reportsDestination/metricsDestination }`) **IMMER
  auf Release** laufen lassen; `<modul>-classes.txt` (Feld-Stabilitaet, `<runtime stability>`) und
  `<modul>-composables.txt` (restartable/skippable, Param-Stabilitaet) auswerten — fehlt `skippable`,
  recomposed das Composable immer. *(offiziell: /performance/stability/diagnose)*
- **Prioritaet beim Fixen:** (1) Strong Skipping (ist an) → (2) Klasse echt immutable (`val`) →
  (3) Immutable Collections → (4) `@Immutable`/`@Stable` als letztes Mittel → (5) Stability-Config
  fuer fremde Typen. **Nicht jedes Composable skippable machen wollen** (Premature Optimization;
  sinnlos bei selten recomposenden, nur-andere-skippable aufrufenden, oder teurem `equals()`).
  *(offiziell: /performance/stability/fix, /diagnose)*

---

## 3. State & `remember` / `rememberSaveable` / `derivedStateOf`

- **`remember` richtig:** `var x by remember { mutableStateOf(default) }` — fuer die `by`-Delegation
  zwingend `import androidx.compose.runtime.getValue` + `...setValue`. Alternativen ohne Imports:
  `val s = remember { mutableStateOf(...) }` (+ `s.value`) oder `val (v, setV) = remember { ... }`.
  *(offiziell: /develop/ui/compose/state)*
- **`remember(key)`:** Key angeben, wenn das gemerkte Ergebnis von einem Parameter abhaengt und bei
  dessen Wechsel neu berechnet werden muss (`remember(avatarRes) { ... }`). Ohne Key behaelt
  `remember` den alten (falschen) Wert ueber den Parameter-Wechsel. *(offiziell)*
- **`rememberSaveable` richtig:** statt `remember`, wenn State Config-Change/Process-Death/
  LazyColumn-Recycling ueberleben soll. **Nur kleine Werte/IDs** (Bundle-Limit ~1 MB, app-weit
  geteilt → sonst `TransactionTooLargeException`); grosse Daten in ViewModel/Room/DataStore und nach
  Restore ueber den Key nachladen. Custom-Typen: `mapSaver`/`listSaver` oder `@Parcelize`. *(offiziell:
  /develop/ui/compose/state-saving)*
- **Listen-State:** `val items = remember { mutableStateListOf<T>() }` (`.add`/`.remove` loesen
  Recompose aus) ODER `var items by remember { mutableStateOf(listOf<T>()) }` + bei Aenderung eine
  **neue** Liste zuweisen. NIE `mutableStateOf(mutableListOf())` in-place mutieren (keine Recomposition).
  Reaktive interne Felder selbst per `mutableStateOf` backen. *(offiziell: /develop/ui/compose/state)*
- **Primitiv-State gegen Autoboxing:** `mutableIntStateOf` / `mutableLongStateOf` /
  `mutableFloatStateOf` / `mutableDoubleStateOf` statt `mutableStateOf(0)` (Compose-Lint
  `AutoboxingStateCreation` warnt). `mutableStateMapOf` analog zu `mutableStateListOf`.
  *(offiziell: runtime package-summary / Lint)*
- **`derivedStateOf` richtig:** NUR wenn der abgeleitete Wert **seltener** wechselt als seine Inputs
  (z.B. `lazyListState.firstVisibleItemIndex > 0` — Index zaehlt hoch, Boolean kippt einmal; aehnlich
  `distinctUntilChanged`). Bei 1:1-Abhaengigkeit (`"$first $last"`) **direkt berechnen** — sonst nur
  Overhead. Nicht-State-Parameter als Key: `remember(param) { derivedStateOf { ... } }`. *(offiziell:
  Medium „When should I use derivedStateOf", /side-effects, /performance/bestpractices)*
- **`key()`-Composable** fuer Identitaet/State-Erhalt, wenn mehrere Composables vom selben Call-Site
  kommen und Side-Effects/internen State haben (`for (m in movies) { key(m.id) { MovieOverview(m) } }`).
  Key muss nur lokal eindeutig sein. In Lazy-Listen der `key`-Parameter (§5.1). *(offiziell: /develop/ui/compose/lifecycle)*
- **Lebensdauer-Entscheidung:** einfacher interner State → `remember`/`rememberSaveable`; geteilt unter
  Geschwistern → zum lowest common ancestor hoisten; komplexe UI-Logik → plain State Holder;
  Business-Logik/App-Daten → ViewModel; muss Process-Death ueberleben → ViewModel + `SavedStateHandle`.
  *(offiziell: /develop/ui/compose/state-hoisting)*

---

## 4. Side-Effects — die richtige API je Situation

| Situation | API |
|---|---|
| Suspend-Arbeit ueber Composable-Lifetime, Neustart bei Wert-Aenderung | `LaunchedEffect(keys)` |
| Coroutine aus Event-Callback (onClick) | `rememberCoroutineScope()` |
| Langlebiger Effekt soll aktuelles Lambda/Wert sehen, ohne Neustart | `rememberUpdatedState` + konstanter Key |
| Registrieren + Cleanup (Observer/Listener/Receiver) | `DisposableEffect` mit `onDispose` |
| Compose-State an Nicht-Compose publizieren (Analytics, ext. Lib) | `SideEffect` |
| Compose-`State` → Flow (mit Operatoren) | `snapshotFlow` in `LaunchedEffect` |
| Externer State (Flow/LiveData/Rx) → Compose-`State` (Zustand!) | `produceState` |
| One-shot-Event (Nav/Snackbar) | als verbrauchbaren **State** modellieren, NICHT Channel/`LaunchedEffect(Unit)` |
| Kritische, nicht-abbrechbare Arbeit (POST, Datei) | `viewModelScope` / WorkManager |

**Faustregel zur Key-Wahl (offiziell):** „Variablen, die in einem Effekt benutzt werden, gehoeren als
Parameter (Key) des Effekts — oder per `rememberUpdatedState`."

- **`LaunchedEffect(keys)`:** Keys = **alle im Block gelesenen veraenderlichen Werte**, die einen
  Neustart ausloesen sollen (Key-Wechsel cancelt die alte Coroutine, startet neu). `LaunchedEffect(Unit/true)`
  nur fuer bewusst genau-einmal — offiziell: „`LaunchedEffect(true)` is as suspicious as a `while(true)`".
  *(offiziell: /develop/ui/compose/side-effects)*
- **`rememberCoroutineScope()`:** NUR in Event-Callbacks (`onClick = { scope.launch { ... } }`), **nie
  im Composable-Body**. *(offiziell)*
- **`rememberUpdatedState`:** langlebiger Effekt mit konstantem Key, der aktuelle Lambdas/Werte lesen
  soll ohne Neustart: `val cur by rememberUpdatedState(onTimeout); LaunchedEffect(Unit){ delay(t); cur() }`.
  *(offiziell)*
- **`DisposableEffect`:** `onDispose` ist Pflicht und muss symmetrisch aufraeumen (`removeObserver`/
  `unregister...`); bei Key-Wechsel erst Cleanup, dann erneute Ausfuehrung. Kein leeres `onDispose {}`.
  *(offiziell)*
- **`SideEffect`:** nur zum Publizieren von Compose-State an Nicht-Compose-Code (laeuft nach jeder
  erfolgreichen Recomposition) — nicht fuer Setup-mit-Cleanup (→ DisposableEffect) oder Suspend (→ LaunchedEffect).
  *(offiziell)*
- **`snapshotFlow`:** State-Reads MUESSEN **im Block** stehen; konflatiert wie `distinctUntilChanged`;
  in `LaunchedEffect` collecten (`snapshotFlow { listState.firstVisibleItemIndex }.map{...}.collect{...}`).
  *(offiziell)*
- **`produceState`:** Nicht-Compose-State → Compose-State; **modelliert State, keine Events** (gleicher
  Wert loest keine Recomposition aus). Fuer diskrete Events Channel/SharedFlow im ViewModel. *(offiziell)*
- **One-shot-Events:** als konsumierbaren State im ViewModel (Snackbar `userMessage: String?` +
  `userMessageShown()`; Navigation ueber Ziel-Zustand + `snapshotFlow{...}.flowWithLifecycle(...)`).
  NICHT naiv an `LaunchedEffect(Unit)` mit Bool-Flag haengen. *(offiziell: /ui-layer/events, Medium-Antipatterns)*
- **Kritische/nicht-abbrechbare Arbeit** (POST, Datei schreiben) in `viewModelScope`/WorkManager — ein
  `LaunchedEffect`/`rememberCoroutineScope` wird beim Verlassen der Composition gecancelt → Datenverlust.
  *(offiziell: Cancellation-Semantik + /ui-layer/events)*

---

## 5. Lazy-Layouts (Column/Row/Grid/StaggeredGrid) & Pager

- **Stabiler, eindeutiger `key = { it.id }`** an `items()`/`itemsIndexed()` — Pflicht fuer korrektes
  State-Handling bei Reorder, `rememberSaveable`-Wiederherstellung und `animateItem()`. Eindeutigkeit
  ist Pflicht (Duplikate → Crash); Key-Typ muss Bundle-faehig sein (Primitiv/Enum/Parcelable).
  *(offiziell: /develop/ui/compose/lists)*
- **`contentType`** fuer heterogene Listen (`items(els, key = {...}, contentType = { it.type })`) —
  Compose recycelt Slots nur zwischen Items gleichen Typs. *(offiziell)*
- **Richtige Overloads:** `items(list)` fuer Elemente, `itemsIndexed(list)` fuer Index+Element,
  `items(count)` nur fuer reine Index-Faelle; DSL `item {}`/`stickyHeader {}`. *(offiziell)*
- **`fillParentMaxSize()`** (nicht `fillMaxSize`) fuer ganzseitige Loading-/Empty-States im Lazy-Item
  (bezieht sich auf den Viewport). Items nie mit 0px-Groesse lassen — sonst komponiert das Lazy-Layout
  alles vorab (Lazy-Effekt weg). *(offiziell)*
- **Kein verschachteltes gleichachsiges Scrollen** (LazyColumn ohne feste Hoehe in
  `Column(verticalScroll)` → `IllegalStateException`): EINE LazyColumn mit `item { Header() }`/`items(...)`/
  `item { Footer() }`. Erlaubt: andere Richtung (LazyColumn in `Row(horizontalScroll)`) oder feste
  Hoehe (nur wenn unvermeidbar). Nicht mehrere unabhaengige Elemente in EINEM `item {}` emittieren.
  *(offiziell)* (Bug-Almanach §6.1/§6.2)
- **`HorizontalPager`/`VerticalPager`** (foundation; Accompanist-Pager deprecated):
  `rememberPagerState(pageCount = { n })`; **`beyondViewportPageCount`** klein halten; seitenabhaengige
  Reaktionen via `snapshotFlow` auf `currentPage` (sofort, UI-Feedback) / `settledPage` (nach
  Animation) / `targetPage`. **Versionsmarke:** `beyondBoundsPageCount` → **`beyondViewportPageCount`**
  umbenannt mit **foundation 1.7** — beide Projekte nutzen bereits den neuen Namen. *(offiziell:
  /develop/ui/compose/layouts/pager; extern: composables.com — Parameter-Versionsbeleg)*
- **`PullToRefreshBox`** (Material3 1.3+, in beiden BOMs) statt Accompanist-`SwipeRefresh`
  (deprecated); `PullToRefreshContainer` aus M3 1.2 wurde durch `PullToRefreshBox`/`Modifier.pullToRefresh`
  ersetzt. *(offiziell: material3.pulltorefresh API)*

---

### 5.1 Drag & Drop — verbindlicher Standard für Android-/Kotlin-Apps

**Geltung:** Bei künftigem Implementieren oder Überarbeiten von Drag & Drop zum **Umsortieren von
Listen** dieses Verhalten standardmäßig übernehmen, sofern Frank nicht ausdrücklich etwas anderes
vorgibt. Das Interaktionsziel gilt auch für andere Android-UI-Techniken; die folgende konkrete
Referenz verwendet Kotlin und Jetpack Compose mit einer vertikalen `LazyColumn`. Für Grids,
horizontale Listen oder appübergreifenden Datentransfer muss die Geometrie entsprechend angepasst
werden; die vertikale Nachbarlogik ist dafür kein unverändert einsetzbarer Algorithmus.

**Herkunft / Evidenz: lokal, vom Benutzer bestätigt.** Frank am 10.09.2026 nach Installation:
„Genauso soll Drag & Drop in Android-Apps, in Kotlin-Apps funktionieren.“
Referenz: **GenialeIdeen 1.5.14**, Fold8 **SM-F971B**, Commit **`0a77c0571`**.
Optimierter Build und Installation erfolgreich; qualitative Bestätigung durch Frank, keine
instrumentierte Performance-Messung oder allgemeine Herstellerempfehlung.

#### Gewünschtes sichtbares Verhalten

- **Gerade und in Originalgröße:** keine Neigung, Drehung oder Vergrößerung der gezogenen Karte.
- **Direkte Fingerführung:** am Griff lang drücken und ziehen. Die Karte folgt ohne künstliche
  Verzögerung dem Finger; Nachbarkarten rücken weich auseinander und zeigen den Einfügeplatz.
- **Nachbarn lesbar lassen:** keine pauschale Abdunklung oder Halbtransparenz der anderen Karten.
  Die darüberliegende Karte bleibt soweit im Sichtfenster vorhanden erkennbar.
- **Im Listenbereich bleiben:** am Rand die Darstellung begrenzen, während die tatsächliche
  Fingerposition weiter erfasst wird. Auch bei einem Finger über der Kopfleiste bleibt die
  gezogene Karte am oberen Listenrand sichtbar. Voraussetzung für vollständige Sichtbarkeit:
  Die Karte passt in die Höhe des nutzbaren Viewports.
- **In beide Richtungen scrollen:** am oberen und unteren Rand kontinuierlich weiterscrollen;
  Zurückziehen und Richtungswechsel funktionieren in derselben Geste. Erster und letzter Platz
  müssen erreichbar sein. Nach dem Ablegen ist normales Scrollen sofort wieder möglich.
- **Weiches Ablegen:** auf genau den durch die lokale Reihenfolge bestimmten Platz gleiten,
  ohne Zurückspringen, Flattern oder erneute Einblend-/Kippanimation.

#### Technische Arbeitsweise — diese Bausteine zusammen übernehmen

1. **Ein Zustand über der Liste:** `ReorderState` hält aktive ID, Fingerposition, Griffversatz,
   sichtbare Kartenoberkante, Ablegefortschritt und Coroutine-Job. Mit `remember(listState,
   listKey)` an die Liste bzw. Kategorie binden, nicht an einzelne recycelte Zeilen.
2. **Stabile IDs:** `items(..., key = { it.id })`; IDs statt Indizes als Identität. Die lokale
   Reihenfolge lebt oberhalb der Items, wird während des Ziehens unmittelbar aktualisiert und
   beim Loslassen über ViewModel/Repository transaktional gespeichert. Inhaltsupdates aus Room
   dürfen die laufende Reihenfolge nicht überschreiben; vorhandene IDs erhalten, gelöschte
   entfernen und neue aufnehmen. Nicht `remember(liste)` für den aktiven Reorder-State verwenden.
3. **Geste am festen Viewport:** Der Griff registriert nur seine Trefferfläche. `pointerInput`
   sitzt an der Liste, nicht an der sich bewegenden Karte. Ablauf: `awaitEachGesture` →
   `awaitFirstDown` → Griff-Treffertest → `awaitLongPressOrCancellation` → `drag`. Übernommene
   Bewegungen konsumieren; vor dem langen Drücken normales Scrollen zulassen. Aktuelle
   Callbacks über `rememberUpdatedState` bereitstellen, ohne den Handler bei jedem Tausch neu
   zu starten. Griffregistrierungen mit `DisposableEffect` entfernen.
4. **Eine Geometriequelle:** ausschließlich aktuelle `listState.layoutInfo.visibleItemsInfo`
   mit `key`, `index`, `offset`, `size` für Tausch und Darstellung nutzen. Keine gespeicherten
   Root-Y-Werte von Zeilen und keine Summen aus Nachbarhöhen. Dadurch sind reale Abstände,
   unterschiedliche Kartenhöhen und das aktuelle Scrollen berücksichtigt.
5. **Koordinaten sauber umrechnen:** In der Referenz ist
   `fingerY = pointerY + layout.viewportStartOffset` und beim Greifen
   `grabOffset = fingerY - startTop`. Während des Ziehens:
   `visualTop = clamp(fingerY - grabOffset, sichtbarerAnfang, sichtbaresEnde - item.size)`.
   Beim Wiederaufnehmen einer gerade abgelegten Karte deren aktuelle visuelle Position als
   `startTop` verwenden. Content-Padding und Listenenden in die Grenzen einbeziehen.
6. **Zeichenversatz statt doppelte Kompensation:**
   `translationY = (visualTop - item.offset) * (1 - landingProgress)` direkt in
   `graphicsLayer { ... }` lesen. Während des Ziehens ist `landingProgress = 0`.
   Scrollbewegungen sind bereits im aktuellen `item.offset` enthalten: NICHT zusätzlich
   `scrollBy`-Rückgabewerte zum Kartenversatz addieren. Hochfrequente Reads bleiben im Layer.
7. **Nachbartausch mit Layout-Sperre:** Nach oben tauschen, wenn die Kartenoberkante die Mitte
   des oberen Nachbarn überschreitet; nach unten, wenn die Kartenunterkante die Mitte des
   unteren Nachbarn überschreitet. Immer die logischen Layoutplätze vergleichen, nicht die
   gerade animierten Pixelpositionen. Vor jedem Tausch müssen `item.index` und der Index der
   aktiven ID in der lokalen Reihenfolge übereinstimmen; auch den Nachbar-Key gegenprüfen.
   Nach einem Tausch bis zum passenden neuen Layout warten. Keine `while`-Tauschschleife mit
   alten Messwerten und keine fast vollständige Überdeckung (ehemals 92 %) als Schwelle.
8. **Sichtfenster beim Tausch erhalten:** Direkt vor der lokalen Listenmutation
   `listState.requestScrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)`
   aufrufen. Dadurch bleibt der numerische Scrollanker erhalten, anstatt dass LazyColumn
   dem umsortierten ersten sichtbaren Key folgt. Nicht bei jedem Tausch an Listenanfang springen.
9. **Nachbarn animieren, aktive Karte direkt führen:** Auf den äußersten Lazy-Item-Container
   zuerst `.reorderRow(state, id)`, dann `.animateItem(...)` legen. `zIndex = 1` für die aktive
   Karte auf dieser Ebene; ein innerer Karten-zIndex reicht nicht über benachbarte Item-Wrapper.
   `placementSpec = null` für die aktive bzw. gerade abgelegte Karte, sonst die unten stehende
   gedämpfte Feder. `fadeInSpec = null`, `fadeOutSpec = null`; keine zusätzliche gestaffelte
   Einblend-/Skalierungsanimation um die sortierbaren Zeilen legen.
10. **Zeitbasiertes Randscrollen:** Ein per `LaunchedEffect(state, state.dragging, density)`
    gestarteter `withFrameNanos`-Loop läuft nur während des Ziehens, auch bei stillstehendem
    Finger. Stärke aus der **ungeklemmten Fingerposition**, nicht aus der begrenzten Karte
    ermitteln. `delta = strength * abs(strength) * maxSpeedPxPerSecond * deltaSeconds`,
    Stärke in `[-1, 1]`. Framezeit auf höchstens 32 ms begrenzen, um nach Hängern nicht zu springen.
    Danach Sichtposition und Tauschziel aktualisieren. Der Referenz-Loop nutzt
    `dispatchRawDelta`, damit `requestScrollToItem` keinen über die ganze Geste gehaltenen
    `scroll {}`-Block abbricht. Das ist eine bewusste interne Lösung für diese kontrollierte
    vertikale Liste: konkurrierende Gesten müssen konsumiert sein. Bei verschachteltem Scrollen,
    Reverse-Layout oder anderer Scrollarchitektur deren Koordination ausdrücklich anpassen.
11. **Ablegen und Abbruch bereinigen:** Randloop sofort beenden, Reihenfolge einmal speichern,
    den letzten Layoutwechsel abwarten, dann `landingProgress` von 0 auf 1 animieren. Erst
    danach aktive ID und Fortschritt löschen. `finally` räumt auch bei Abbruch auf; ein
    Generationszähler verhindert, dass eine alte abgebrochene Ablegeanimation eine neue
    Ziehgeste zurücksetzt. Beim Verlassen der Liste Jobs und Trefferflächen freigeben.
12. **Bewegungsreduktion respektieren:** Nachbaranimation ausschalten, Ablegedauer auf 0 setzen;
    direkte Fingerführung und Randscrollen bleiben nutzbar.

#### Bewährte Startwerte aus GenialeIdeen 1.5.14

| Größe | Referenzwert |
|---|---|
| Nachbaranimation | `spring(dampingRatio = 1f, stiffness = 450f)` |
| Ablegen | `tween(180)`; reduziert: `tween(0)` |
| Randzone | `88.dp`, höchstens ein Drittel der Viewporthöhe |
| Maximale Scrollgeschwindigkeit | `560.dp` pro Sekunde, quadratischer Anstieg zur Kante |
| Maximal berücksichtigte Framezeit | `0.032f` Sekunden |
| Griff-Trefferfläche | `40.dp × 48.dp`, sichtbares Icon `22.dp` |
| Kartenabstand der Referenz | `12.dp`; aus realem Layout berücksichtigen, nicht hart in den Algorithmus schreiben |
| Darstellung | `rotationZ = 0`, `scaleX = scaleY = 1`, volle Deckkraft |

**Wiederverwendung:** Die zusammengehörigen Quelldateien vor Umsetzung lesen:
- [`GenialeIdeen/.../ui/DragReorder.kt`](../../GenialeIdeen/app/src/main/java/de/frank/genialeideen/ui/DragReorder.kt)
  — Zustand, Viewport-Geste, Griffregistrierung, Randscrollen und Ablegen.
- [`GenialeIdeen/.../ui/ListenScreen.kt`](../../GenialeIdeen/app/src/main/java/de/frank/genialeideen/ui/ListenScreen.kt)
  — lokale ID-Reihenfolge, äußere Item-Wrapper, `animateItem`, Griff und Speicherung.
- Reproduzierbarer Ausgangsstand: Commit `0a77c0571`; neue Anwendungen an ihrer eigenen
  Compose-Version und Listenarchitektur ausrichten, Interaktionsstandard beibehalten.
- Fehlerursachen und Prävention: [Bug-Almanach §4.9](../../bugs/android/jetpack-compose.md#49-drag--drop-springt-flattert-oder-verliert-die-karte-am-listenrand).

**Abnahmeszenarien für künftige Umsetzungen:** Mehrere Plätze langsam und schnell verschieben;
am oberen und unteren Rand halten; innerhalb derselben Geste die Richtung wechseln; auf dem ersten
und letzten Platz ablegen; unterschiedliche Kartenhöhen; erneut greifen während des Ablegens;
normales Scrollen danach; Inhaltsupdates während des Ziehens; reduzierte Bewegung. Das beschreibt
das erforderliche Verhalten und ist keine Behauptung, dass jede Randbedingung der Referenz
instrumentiert getestet wurde.

---

## 6. Modifier

- **Reihenfolge ist Logik:** jede Modifier-Funktion arbeitet auf dem Ergebnis der vorherigen.
  Empfohlene Sequenz: **Layout** (`size`/`fillMax*`/`padding`) → **Dekoration** (`clip` → `background`
  → `border`) → **Interaktion** (`clickable`). `clip` vor `border` (sonst weggeschnitten). `padding`
  relativ zu `clickable`: **nach** `clickable` → ganze Flaeche inkl. Padding klickbar; **vor**
  `clickable` → Padding-Rand reagiert nicht. *(offiziell: /develop/ui/compose/modifiers)*
- **Eigene Modifier via `Modifier.Node`/`ModifierNodeElement`**, NICHT `Modifier.composed {}`
  (deprecated-artig, Performance-Probleme). Drei Teile: `Modifier.Node`-Klasse (Logik/State, ueberlebt
  Recompositions) + `ModifierNodeElement` (stateless `data class`, `create()`/`update()`,
  `equals`/`hashCode`) + Factory `fun Modifier.circle(c) = this then CircleElement(c)`. Composable
  Modifier-Factories (Chaining) sind fuer einfache Faelle OK, aber: `CompositionLocal` wird am
  Call-Site aufgeloest, nie uebersprungen, nicht aus der Composition heraushebbar. *(offiziell:
  /develop/ui/compose/custom-modifiers)*
- **`pointerInput(key)` korrekt keyen:** Key steuert Cancel+Restart der Gesten-Coroutine. Frische
  Werte ohne Restart via `rememberUpdatedState`. In Listen **nie den Index** capturen — stabilen Key
  (`pointerInput(item.id)`) zusammen mit `items(..., key = { it.id })`. *(offiziell: /touch-input/pointer-input)*
- **Wiederverwendbare Modifier** (ohne Composition-State-Abhaengigkeit) als top-level
  `val reusable = Modifier.fillMaxWidth().padding(...)` extrahieren, via `reusable.clickable {...}`/
  `other.then(reusable)` anhaengen — nicht je Recomposition neu erzeugen. *(offiziell: /modifiers)*
- **`Modifier.layout {}`-Grundregel:** Child genau einmal messen (`measurable.measure(constraints)`),
  eigene Groesse zurueckgeben, Placeable platzieren (`place(x, y)`). *(offiziell: /custom-modifiers)*

---

## 7. Material3, Theming & Insets

- **`MaterialTheme`** mit `ColorScheme` (`lightColorScheme()`/`darkColorScheme()`), `Typography`,
  `Shapes`; Farbrollen IMMER paaren (`primary`↔`onPrimary` …) — die Tonal-Palette sichert
  Kontrast/Accessibility. *(offiziell: /develop/ui/compose/designsystems/material3)*
- **`dynamicColor` mit API-Guard + statischem Fallback:** *(offiziell)*
  ```kotlin
  val colorScheme = when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark  -> dynamicDarkColorScheme(context)
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !dark -> dynamicLightColorScheme(context)
      dark -> DarkColorScheme                 // statisches Fallback
      else -> LightColorScheme
  }   // dark: Boolean = isSystemInDarkTheme()
  ```
- **Theme in Dialog/BottomSheet/Preview erneut bereitstellen** (eigenes Window/Composition) — sonst
  greifen die Tokens nicht; Previews ins App-Theme wrappen. *(offiziell fuer Preview; Konsequenz fuer Dialog/Sheet)*
- **M3-Komponenten korrekt:** `ExposedDropdownMenuBox` braucht `Modifier.menuAnchor(...)` am Anker-Feld
  — aktuell mit `ExposedDropdownMenuAnchorType` (z.B. `PrimaryEditable`), die parameterlose Variante
  ist deprecated (AnchorType ab Material3 1.3+, gilt fuer beide BOMs). `ModalBottomSheet`: suspend
  `sheetState.hide()` ERST abschliessen, dann Flag im `onDismissRequest` setzen; IME via
  `contentWindowInsets` steuern. *(offiziell: ExposedDropdownMenuAnchorType API, Bottom-sheets-Doku)*
- **Insets / edge-to-edge:** `enableEdgeToEdge()` in `onCreate` vor `setContent` (Android 15 / target
  SDK 35 erzwingt es ohnehin); Manifest `android:windowSoftInputMode="adjustResize"` fuer IME.
  **`Scaffold`-`innerPadding` IMMER anwenden** (`Modifier.consumeWindowInsets(innerPadding)` +
  `contentPadding = innerPadding`) und KEINE zusaetzlichen Inset-Modifier (sonst doppelt). Inset-Typen:
  `safeDrawing` (Standard) / `systemBars` / `ime` / `displayCutout`; Modifier `windowInsetsPadding`/
  `imePadding`/`safeDrawingPadding`. M3-Komponenten (`*TopAppBar`, `NavigationBar/Rail`, Drawer,
  `ModalBottomSheet`) handhaben Insets selbst — nicht zusaetzlich padden; abschaltbar via
  `windowInsets = WindowInsets(0,0,0,0)`. *(offiziell: /system/setup-e2e, /system/insets, /system/material-insets)* (Bug-Almanach §8.1/§8.2)

---

## 8. Navigation-Compose (2.8, type-safe)

- **Type-safe Navigation** (stabil ab Navigation 2.8.0, nav-compose 2.8.7 erfuellt): `@Serializable`-
  Routes (Kotlin-Serialization-Plugin) — `object` ohne Args, `data class` mit Args; `composable<Route>{
  entry -> val r = entry.toRoute() }`, navigieren mit Instanz `navController.navigate(Profile(id))`,
  im ViewModel `savedStateHandle.toRoute<Profile>()`. Eigene `NavType` per `typeMap` registrieren.
  *(offiziell: /guide/navigation/design/type-safety)*
- **Nur IDs/kleine Werte navigieren** — grosse Objekte → `TransactionTooLargeException`; grosse Daten
  ueber geteiltes ViewModel/Repository. *(offiziell: Bundle-Limit; Navigations-Regel)*
- **Geteiltes ViewModel am Parent-`NavBackStackEntry`:**
  `val parent = remember(it) { navController.getBackStackEntry(ParentRoute) }; hiltViewModel(parent)`
  — `remember(it)` an den aktuellen Entry binden. *(offiziell: ViewModel-APIs)*
- **Bottom-Nav Tab-State:** EIN `NavHost` (kein nested pro Tab); beim Wechsel
  `navigate(route){ popUpTo(graph.startDestinationId){ saveState = true }; launchSingleTop = true;
  restoreState = true }`. *(offiziell: /develop/ui/compose/navigation)*
- **Predictive Back:** Voraussetzungen nav-compose 2.8+, Material3 1.3+, Manifest
  `android:enableOnBackInvokedCallback="true"`; `SearchBar`/`ModalBottomSheet` animieren automatisch,
  Custom-Gesten via `PredictiveBackHandler` (`Flow<BackEventCompat>`, `progress` 0..1). Animation
  sichtbar ab Android 14+, Callback wirkt ab Android 13. *(offiziell: /system/predictive-back[-setup])*

---

## 9. Adaptive Layouts (Tablet / Foldable)

- **`currentWindowAdaptiveInfo().windowSizeClass`** statt `Configuration.orientation` (nur binaer,
  ignoriert Resize/Foldable/Split-Screen). Width-Breakpoints: **Compact < 600dp**, **Medium 600–840dp**,
  **Expanded ≥ 840dp** (neuere WindowSizeClass: zusaetzlich Large ≥1200, XLarge ≥1600). Klassen sind
  dynamisch (Orientierung/Multi-Window/Fold), nicht physische Geraetegroesse. Dependency
  `androidx.compose.material3.adaptive:adaptive`. *(offiziell: /layouts/adaptive/use-window-size-classes)*
- **`NavigationSuiteScaffold`** wechselt automatisch Bottom-`NavigationBar` (Compact) ↔ `NavigationRail`
  (Medium/Expanded); Override via `layoutType`. *(offiziell: /layouts/adaptive/build-adaptive-navigation)*
- **`NavigableListDetailPaneScaffold`** (bevorzugt vor `ListDetailPaneScaffold` — eingebaute Navigation
  + Predictive-Back): `rememberListDetailPaneScaffoldNavigator<T>()`, Inhalte in `AnimatedPane {}`,
  Modell `@Parcelize`, bis zu 3 Panes. *(offiziell: /layouts/adaptive/list-detail)*
- **Versions-Hinweis:** material3-adaptive (NavigationSuite, ListDetail) ist ein **eigenes Artefakt**
  mit eigenem Versionsschema (1.1.0+), unabhaengig von der Material3-Core-Version (1.3.1/1.4.0) —
  nicht verwechseln. **Upgrade-Pfad** falls noch nicht eingebunden. *(offiziell: compose-material3-adaptive Releases)*

---

## 10. Animation (richtige API je Fall)

- **`animate*AsState`** fuer einfache Wert-Animation — Ziel **deklarativ** im Body binden
  (`animateFloatAsState(if (enabled) 1f else 0.5f)`), startet automatisch bei Aenderung. **NICHT** in
  `SideEffect`/`LaunchedEffect` setzen (offiziell: „DO NOT use LaunchedEffect for animate*AsState").
  `LaunchedEffect` nur fuer `Animatable.animateTo()`. *(offiziell: /animation/value-based)*
- **`rememberInfiniteTransition`** fuer Endlos (mit `infiniteRepeatable`); animierten Wert **deferred**
  lesen (z.B. `graphicsLayer`, §11) und pausieren/nicht rendern wenn unsichtbar. *(offiziell)*
- **`AnimatedContent`/`Crossfade`:** im Lambda IMMER den **uebergebenen `targetState`** reflektieren
  (offiziell: „You must always use the lambda parameter…"), `contentKey` setzen; Uebergaenge via
  `ContentTransform`/`SizeTransform`. *(offiziell: /animation/composables-modifiers)*
- **Spezial-APIs:** `updateTransition` (mehrere synchron startende Properties); `AnimatedVisibility`
  (Erscheinen/Verschwinden, `MutableTransitionState` fuer Start-bei-Composition, `Modifier.animateEnterExit`);
  `animateContentSize` (**Reihenfolge kritisch:** VOR `size`/`defaultMinSize`); `Animatable`
  (gesten-getrieben/einmalig, feinste Kontrolle mit `LaunchedEffect`). *(offiziell: /animation/choose-api)*

---

## 11. Performance, Tooling & Testing

**Performance**
- **IMMER im Release-Build (R8 / `minifyEnabled true`) messen** — Debug ist by-design langsam und
  maskiert echte Performance. *(offiziell: /develop/ui/compose/performance)* (Bug-Almanach §10.1) *(Build-Config → [`gradle.md`](../android-build/gradle.md))*
- **Baseline Profiles** (`androidx.baselineprofile`-Plugin, AGP 8.0+) fuer kritische Journeys (Startup,
  Navigation, Scroll, Login/Payment) — ~30% schnellere Ausfuehrung ab erstem Start; Compose liefert ein
  Default-Profil mit. *(offiziell: /topic/performance/baselineprofiles/overview)*
- **Deferred state reads:** hochfrequente Werte (Scroll-Offset, Animation) NUR in Lambda-Modifiern
  lesen — `Modifier.graphicsLayer { }`, `offset { IntOffset(0, scrollProvider()) }` (Layout-Phase),
  `drawBehind { }` (Draw-Phase). Read komplett ins Lambda. *(offiziell: /performance/bestpractices)*
- **Lambda-Parameter `() -> T`** statt direktem Wert fuer oft wechselnde Werte (minimiert
  Recomposition-Scope). *(offiziell)*
- **Teure Arbeit** (`sorted()`/`filter`/Format/Bitmap) mit `remember(key) { ... }` cachen oder ins
  ViewModel auslagern. *(offiziell)*
- **Eigene Schatten und Oberflächen:** Größenabhängige Pfade, Brushes, Paints und BlurMaskFilter in `drawWithCache` vorbereiten, in `onDrawBehind` weiterverwenden. Ausstanzen halbtransparenter Flächen, Zeichenreihenfolge, Dichte und Layout-Richtung erhalten. Animierte Werte nicht im Cache einfrieren. *(lokal statisch angewendet: Experimente und StackLaborWerftStudio, 07.09.2026; Bug-Almanach §10.10; keine gemessene Beschleunigung)*
- **Strikt neutrale Animationsoptimierung:** Ein State-Read kann von Composition nach Layout/Draw/Layer verschoben werden, ohne Laufzeit, Kurve oder Startphase zu ändern. Das bedingte Erzeugen einer bisher dauernd laufenden Transition verändert dagegen ihre Phase beim Wiedereintritt und ist kein automatisch gleichwertiger Ersatz. Ebenso vor einem Eager-/Lazy-Wechsel Fokus, Entwürfe, Effekte und Item-State klären. *(lokale Ableitung aus dem appübergreifenden Lauf vom 07.09.2026)*
- **Stabile Lazy-Keys + kleine, fokussierte Composables**; Backwards Writes vermeiden (§1.1 Almanach).
  *(offiziell)*

**Tooling & Testing**
- **`@Preview` STATELESS:** Screen = Funktion von Input-State + Output-Events; `viewModel()`/
  `hiltViewModel()` NIE ins Preview (DI-Graph nicht baubar). Container-Composable mit VM + stateless
  Content mit Mock-Daten; `@PreviewParameter` + `PreviewParameterProvider`; `LocalInspectionMode.current`
  fuer Platzhalter. *(offiziell: /tooling/previews)* (Bug-Almanach §10.6)
- **Compiler-Reports/Metrics auf Release** auswerten (`composeCompiler { reportsDestination/metricsDestination }`)
  — `classes.txt`/`composables.txt`. *(offiziell: /performance/stability/diagnose)* (siehe §2)
- **Compose-UI-Tests:** `createComposeRule()` (isoliert) / `createAndroidComposeRule<Activity>()`
  (Activity-Zugriff); Finder `onNodeWithText`/`onNodeWithTag` + `Modifier.testTag(...)`; Synchronisation
  automatisch, `waitUntil { }`/Idling fuer Async; experimentelle APIs `@OptIn(ExperimentalTestApi::class)`.
  Deps: `ui-test-junit4` (androidTest) + `ui-test-manifest` (debug). *(offiziell: /develop/ui/compose/testing)*
- **Screenshot-Tests:** offiziell ist **Compose Preview Screenshot Testing** — Plugin
  **`com.android.compose.screenshot`**, eigener Source-Set **`screenshotTest/`**, `@PreviewTest`,
  HTML-Diff-Report; mehrere Form-Faktoren via `@Preview`-Parameter (`uiMode`/`fontScale`/Devices).
  *(offiziell: /studio/preview/compose-screenshot-testing)* — **Roborazzi/Paparazzi sind Community/`extern`**
  (in der offiziellen Doku nicht als Alternative genannt). *(extern)*
- **Layout-Inspector Recomposition-Counts nur als Hinweis** — echte Frame-Zeit/Jank via Macrobenchmark
  (`androidx.benchmark:benchmark-macro-junit4`) / JankStats, nur im Release-Build. *(offiziell: /diagnose,
  /baselineprofiles)* (Bug-Almanach §10.7)

---

## Pflege

- Bei jeder neuen offiziellen Compose-/Material3-/Navigation-Empfehlung hier ergaenzen (mit Quelle +
  `offiziell`/`extern` + ggf. Versionsmarke/Upgrade-Pfad), Stand-Header aktualisieren.
- Gegenstueck-Bugs immer auch in [`bugs/android/jetpack-compose.md`](../../bugs/android/jetpack-compose.md) pflegen
  und die Bezugstabelle dort synchron halten (zwei Seiten derselben Medaille — Compound Intelligence).

## Quellen (offiziell, developer.android.com & Android Developers Medium)

- Architektur/State: /develop/ui/compose/architecture, /state, /state-hoisting, /state-saving;
  /topic/architecture/ui-layer/{events,state-production,stateholders}; Medium „ViewModel one-off event
  antipatterns", „Consuming flows safely in Jetpack Compose", „When should I use derivedStateOf".
- Stabilitaet/Performance: /develop/ui/compose/performance[/bestpractices]; /performance/stability[/fix,
  /diagnose, /strongskipping]; Medium „Strong Skipping Mode Explained"; /topic/performance/baselineprofiles/overview.
- Side-Effects/Lifecycle: /develop/ui/compose/side-effects, /lifecycle.
- Lazy/Pager/Modifier: /develop/ui/compose/lists, /layouts/pager, /modifiers, /custom-modifiers,
  /touch-input/pointer-input, material3.pulltorefresh API.
- Material3/Insets/Nav/Adaptive: /develop/ui/compose/designsystems/material3, /system/{setup-e2e,insets,
  material-insets,predictive-back,predictive-back-setup}; /guide/navigation/design/type-safety,
  /develop/ui/compose/navigation; /layouts/adaptive/{use-window-size-classes,build-adaptive-navigation,list-detail};
  compose-material3-adaptive Releases; ExposedDropdownMenuAnchorType API, Bottom-sheets-Doku.
- Animation/Tooling/Testing: /develop/ui/compose/animation/{choose-api,value-based,composables-modifiers};
  /tooling/previews, /testing; /studio/preview/compose-screenshot-testing.
- `extern`: composables.com (Pager-Parameter-Versionsbeleg), github.com/skydoves/compose-performance.
