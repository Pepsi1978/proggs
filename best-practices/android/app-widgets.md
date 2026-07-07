# Best Practices: App-Widgets (Jetpack Glance + klassische RemoteViews)

> **Zweite Seite der Medaille** zum Almanach [`bugs/android/app-widgets.md`](../../bugs/android/app-widgets.md):
> wie man Homescreen-Widgets von vornherein richtig baut, damit die dort gelisteten Fallen gar nicht
> erst entstehen. Quellen: developer.android.com/develop/ui/compose/glance + Franks Production-Erfahrung
> (Entropie Reductor 2026-05-11). **Stand 2026-06-19. Anker: glance-appwidget=1.1.1.**

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice |
|---|-----------|---------------|
| 1 | Neues Widget planen | **Einfach (Text/Icon/Buttons, kein versteckter Scrollbar, keine Custom-Properties) → Glance. Scrollbare Liste mit verstecktem Scrollbar / ungewoehnliche View-Properties → von Anfang an klassisch** (AppWidgetProvider + RemoteViewsService). |
| 2 | Toggle/Schalter | **NIE klickbar im Widget** — in die App/Widget-Settings auslagern; im Widget nur Status-Indikator + Settings-Tap. |
| 3 | Glance-Bug an nicht-exposed Property | Max **3 Iterationen** (1 Recherche-Cycle), dann klassisch — nicht endlos Workarounds. |
| 4 | Klassisches RemoteViews-Layout | NUR erlaubte View-Klassen; **kein `<View>`** (→ `<ImageView>`); Shape-Drawables per `setBackgroundTintList` faerben, nicht `setBackgroundColor`. |
| 5 | Glance-Daten laden | DB/Netzwerk in `provideGlance()` (laeuft im Worker), nicht im Composable. |
| 6 | Glance-Groessen | `SizeMode.Responsive` (Android 12+) mit konkreten `DpSize`-Buckets statt `Exact` (Performance). |
| 7 | Glance-Liste | `items(itemId = {…})` fuer Scrollposition + Performance (A12+). |
| 8 | Ressourcen/Farben | **Resource-IDs** an Glance geben (`ImageProvider(R.drawable…)`, `background(R.color…)`) statt aufgeloester Werte → kleinere RemoteViews + dynamische Ressourcen. |

---

## 1. Die Grundentscheidung: Glance vs. klassisch (WICHTIGSTE Best Practice)

**Glance** ist ein bewusst **begrenzter** Compose-Layer ueber RemoteViews. Es gewinnt bei **einfachen** Widgets
(schnell gebaut, modern). Es **verliert**, sobald ein **klassisches View-Property** gebraucht wird, das Glance
nicht im Modifier-System exposed — am haeufigsten: **Scrollbar verstecken** in einer scrollbaren Liste.

| Anforderung | Empfehlung |
|-------------|-----------|
| Text/Icons/Buttons, fixe oder responsive Groesse, kein versteckter Scrollbar | **Glance** |
| Scrollbare Liste, bei der der **Scrollbar unsichtbar** sein soll | **Klassisch** (AppWidgetProvider + RemoteViewsService + `<ListView android:scrollbars="none">`) — von Anfang an |
| Ungewoehnliche Layout-Properties, `listViewStyle`, volle View-Kontrolle | **Klassisch** |

**3-Iterationen-Regel:** Wenn ein Glance-Widget bereits gebaut ist und ein Wunsch auftaucht, den Glance nicht
exposed: **max. 1 Recherche-Cycle**, dann **kompletter Klassik-Rebuild** (~600–800 Zeilen Kotlin + 5–6 XML) —
das ist weniger Aufwand als 5+ gescheiterte Glance-Workarounds.

## 2. Glance richtig nutzen

- **Laden in `provideGlance()`** (laeuft seit dem WorkManager-Session-Modell im Worker): DB/Netzwerk/Ressourcen
  dort holen, dann `provideContent { … }` nur fuer die UI. State persistent ueber `GlanceStateDefinition`/Repository;
  in-Widget-State via `remember { mutableStateOf() }` + `rememberCoroutineScope()` fuer async-Callbacks.
- **Interaktion** ueber die 4 Action-Handler: `actionRunCallback`, `actionStartActivity`, `actionStartService`,
  `actionStartBroadcastReceiver`. **Toggles NICHT im Widget** (Launcher-Caching + Worker-Dedup → unzuverlaessig)
  → in die App/Widget-Settings auslagern, Widget refresht via `updateAll`.
- **Groesse:** `SizeMode.Responsive(setOf(DpSize(…), …))` mit konkreten Buckets (A12+); `LocalSize.current` fuer
  Layout-Verzweigungen. `Exact` nur wenn noetig (Performance-Overhead, Widget wird bei jedem Resize neu gebaut).
- **Liste:** `LazyColumn { items(items, itemId = { … }) { … } }` — `itemId` setzen (Scrollposition + Speed).
- **Ressourcen-IDs statt Werte:** `ImageProvider(R.drawable.x)`, `GlanceModifier.background(R.color.y)` → kleinere
  RemoteViews-Objekte + dynamische (Tag/Nacht-)Ressourcen. Komplexe Verschachtelung meiden (RemoteViews-Serialisierung).
- **Keine Custom-Fonts** — nur System-Fonts (`FontFamily.Monospace`/`Serif`/`SansSerif`).

## 3. Klassische RemoteViews richtig bauen (der Glance-Escape-Hatch)

- **Nur erlaubte View-Klassen** im Layout (FrameLayout, LinearLayout, RelativeLayout, GridLayout, TextView,
  ImageView, Button, ImageButton, ProgressBar, ListView, GridView, StackView, ViewFlipper, AdapterViewFlipper,
  AnalogClock, Chronometer, RadioGroup). **Statt `<View>` immer `<ImageView>`** (Spacer/Hintergrund/Container).
- **Shape-Drawables faerben:** `setColorStateList(id, "setBackgroundTintList", ColorStateList.valueOf(c))` —
  behaelt runde Ecken/Oval. `setBackgroundColor` nur fuer vollflaechige, formlose Hintergruende.
- **Scrollbare Liste:** `RemoteViewsService` + `RemoteViewsFactory`; im Layout `<ListView android:scrollbars="none">`
  wenn der Scrollbar weg soll (das ist der ganze Grund fuer den Klassik-Weg).
- **Tooling:** Tag-Massenersetzungen (`<View>`→`<ImageView>`) mit Python `re.sub(r'<View(\s)', r'<ImageView\1', s)`
  — `sed` greift bei mehrzeiligen Tags nicht.

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Abschnitt

| Best-Practice (diese Datei) | Bug-Abschnitt (`bugs/android/app-widgets.md`) |
|---|---|
| §1 Entscheidung Glance vs. klassisch + 3-Iterationen | §1, §2, §3 |
| §2 Glance richtig (provideGlance, SizeMode, itemId, State, Toggle) | §2, §6, §7, §8 |
| §3 Klassische RemoteViews | §4, §5 |

## Quellen
- developer.android.com/develop/ui/compose/glance/build-ui · /jetpack/androidx/releases/glance
- Eigene Vorfaelle Entropie Reductor 2026-05-11 (Memories `feedback_widget_scrollbar_master`,
  `feedback_widget_classic_over_glance`, `feedback_glance_widget_toggle_pattern`, `feedback_remoteviews_pitfalls`).
