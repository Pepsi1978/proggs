# Bekannte Bugs & Fallen: App-Widgets (Jetpack Glance + klassische RemoteViews/AppWidgetProvider)

> **PFLICHT-LESEN vor JEDER Arbeit an Homescreen-Widgets** (`*.kt` mit `GlanceAppWidget`/
> `GlanceAppWidgetReceiver`/`provideGlance`/`LazyColumn`(glance) ODER klassisch
> `AppWidgetProvider`/`RemoteViews`/`RemoteViewsService`/`RemoteViewsFactory`, `appwidget-provider`-XML).
> Kuratiert aus offizieller Doku (developer.android.com/develop/ui/compose/glance) + **Franks realen
> Production-Vorfaellen** (Entropie Reductor, 2026-05-11). Loesungen sind **funktionserhaltend**
> (Direktive #3) — Glance wird nicht "weggelassen", sondern bewusst gegen klassische Widgets getauscht,
> wo Glance ein Property nicht exposed.
>
> **Zweite Seite der Medaille (Praevention):** [`best-practices/android/app-widgets.md`](../../best-practices/android/app-widgets.md)
> — Bezugstabelle „Bug-Abschnitt ↔ Best-Practice" ganz unten.
>
> **Stand:** recherchiert am **2026-06-19** (Web-Check + eigene Vorfaelle) fuer:
> **Anker:** glance-appwidget=1.1.1  <!-- maschinenlesbar fuer check-version-anchor.py -->
> (stable 1.1.1 / Okt 2024; 1.2.0-rc01 / Dez 2025; 1.3.0-alpha01 / Mai 2026).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | ⭐ Scrollbar in Glance-`LazyColumn` soll **versteckt** werden | Geht in Glance **NICHT** (LazyColumn = ListView, Glance exposed kein Scrollbar-Modifier; Launcher wenden das Receiver-Theme nicht zuverlaessig an). → klassisches `AppWidgetProvider` + `ListView android:scrollbars="none"` | §1 |
| 2 | ⭐ Klickbarer **Toggle/Schalter direkt im Widget** | Unzuverlaessig (Launcher-Caching Nova/One UI + Glance-Worker-Queue-Dedup). → Toggle in **App-Settings** auslagern; im Widget nur Status-Indikator + Tap oeffnet Settings | §2 |
| 3 | ⭐ Glance-Bug an einem Property, das Glance **nicht exposed** | **Max 3 Iterationen**, dann klassisch (AppWidgetProvider + RemoteViewsService). Nicht endlos Workarounds raten | §3 |
| 4 | Klassisches RemoteViews-Layout: Widget bleibt **komplett weiss** | Inflate failt an `<View>` — RemoteViews akzeptiert nur eine **feste Klassenliste** (kein `<View>`). → `<ImageView>` als Spacer/Hintergrund | §4 |
| 5 | Pillen/Buttons **eckig statt rund** nach Farb-Setzen | `setInt(id,"setBackgroundColor",..)` ueberschreibt das Shape-Drawable mit ColorDrawable. → `setColorStateList(id,"setBackgroundTintList", ColorStateList.valueOf(c))` (tintet das Shape, behaelt Ecken) | §5 |
| 6 | **Custom-Font** im Glance-Widget | NICHT unterstuetzt — nur System-Fonts (`FontFamily.Monospace` etc.) | §6 |
| 7 | LazyColumn **verliert Scrollposition** / langsam bei Update (A12+) | `items(itemId = { ... })` setzen | §7 |
| 8 | DB-/Netzwerk-Arbeit fuers Widget | In `provideGlance()` laden (laeuft im WorkManager-Worker), NICHT im Composable; `provideContent {}` nur fuer UI | §8 |
| 9 | `<View>`-Tags per `sed` ersetzen schlaegt fehl (mehrzeilig) | `sed` arbeitet zeilenweise → Python `re.sub(r'<View(\s)', r'<ImageView\1', s)` | §5 |

---

## 🔗 Bezugs-Tabelle: Bug ↔ Best-Practice

| Bug-Abschnitt (diese Datei) | Best-Practice-Gegenpart (`best-practices/android/app-widgets.md`) |
|---|---|
| §1–§3 Glance-Grenzen → klassisch | §1 Entscheidung: Glance vs. klassisch (+ 3-Iterationen-Regel) |
| §4–§5 RemoteViews-Fallen | §3 Klassische RemoteViews richtig bauen |
| §2 Toggle | §2 Interaktions-Pattern (Toggle in Settings) |
| §6–§8 Glance-Limits/State/Update | §2 Glance richtig nutzen (SizeMode, provideGlance, State, itemId) |

---

## 1. Glance kann den `LazyColumn`-Scrollbar NICHT verstecken ⭐ (Franks Vorfall: 11 Iterationen)
**Symptom:** In einem Glance-Widget mit scrollbarer Liste soll der Scrollbar unsichtbar sein — bekommt man in Glance nicht weg.
**Ursache:** Glance-`LazyColumn` wird intern in ein **`ListView` mit RemoteViews-Collection** uebersetzt. Glance exposed den Scrollbar (`scrollbars`/`scrollbarStyle`) **nicht** ueber sein Modifier-System. Und Launcher (Nova, Stock, One UI) wenden das Receiver-`android:theme` mit `listViewStyle`/scrollbar-Properties **nicht zuverlaessig** auf die intern generierten Glance-RemoteViews an.
**Versionen:** glance-appwidget 1.0–1.2 (per Design).
**Gescheiterte Ansaetze (NICHT wiederholen):** `actionRunCallback`/`actionStartActivity`, `PreferencesGlanceStateDefinition`+`currentState`, `updateAppWidgetState`-Tick-Hack, direkter AppWidget-Broadcast, `update` statt `updateAll`, Receiver-Theme `listViewStyle`, transparente Thumb/Track-Drawables, `padding(end=…)`, Reflection auf `partiallyUpdateAppWidget`.
**FIX (funktionserhaltend):** Kompletter Umbau auf **klassisches `AppWidgetProvider` + `RemoteViewsService` + `RemoteViewsFactory`**, Layout mit `<ListView android:scrollbars="none" … />`. **Eine XML-Zeile, garantiert wirksam.** Aufwand ~600–800 Zeilen Kotlin + 5–6 XML-Layouts — weniger als die kumulierte Zeit gescheiterter Glance-Workarounds.
**Quelle:** Vorfall Entropie Reductor 2026-05-11; developer.android.com/develop/ui/compose/glance/build-ui (LazyColumn → ListView).

## 2. Klickbarer Toggle DIREKT im Widget ist unzuverlaessig ⭐
**Symptom:** Ein Toggle/Filter-Schalter direkt im Glance-Widget schaltet mal, mal nicht (besonders Nova Launcher, Samsung One UI).
**Ursache:** Launcher cachen Widgets aggressiv UND Glance dedupliziert seine interne Worker-Queue → der State-Roundtrip kommt nicht zuverlaessig durch.
**Versionen:** glance-appwidget (per Design, launcher-abhaengig).
**FIX (funktionserhaltend):** **Toggle aus dem Widget heraus in die App-Einstellungen** verlegen. Im Widget nur ein **nicht-klickbarer Status-Indikator** + ein Tap, der die Widget-Settings oeffnet (Radio/Switch dort). Settings schalten via normalem `setXxx` + `updateAll`-Pfad — bewiesen zuverlaessig auch unter Nova + One UI.
**Quelle:** Vorfall Entropie Reductor 2026-05-11.

## 3. 3-Iterationen-Regel: Glance-Property nicht exposed → klassisch ⭐
**Symptom:** Ein Glance-Bug betrifft ein View-Property, das Glance nicht in seinem Modifier-System anbietet (Scrollbar-Sichtbarkeit, ungewoehnliche Layout-Properties, `listViewStyle`).
**Ursache:** Glance ist absichtlich ein begrenzter Compose-Layer ueber RemoteViews — viele klassische View-Properties sind nicht erreichbar.
**FIX:** Nach **max. 3 gescheiterten Iterationen** (1 Recherche-Cycle: Doku + 1–2 Foren) auf **klassisches `AppWidgetProvider` + `RemoteViewsService`** umstellen statt 10+ hoffnungsvolle Code-Tweaks. **Faustregel:** Glance gewinnt bei einfachen Widgets; klassisch gewinnt, sobald komplexe Custom-Layouts oder ungewoehnliche Properties gebraucht werden.
**Quelle:** Vorfall Entropie Reductor 2026-05-11 (11 Iterationen) + `feedback_iteration_stop_rule`.

## 4. Klassisches RemoteViews: Widget bleibt komplett weiss (`<View>` nicht erlaubt)
**Symptom:** Nach Umstieg auf klassische RemoteViews ist das Widget komplett weiss — Inflate fehlgeschlagen.
**Ursache:** `RemoteViews` akzeptiert nur eine **feste Liste** von View-Klassen: `FrameLayout`, `LinearLayout`, `RelativeLayout`, `GridLayout`, `TextView`, `ImageView`, `Button`, `ImageButton`, `ProgressBar`, `ListView`, `GridView`, `StackView`, `ViewFlipper`, `AdapterViewFlipper`, `AnalogClock`, `Chronometer`, `RadioGroup`. **`<View>` ist explizit NICHT dabei** (haeufig als Spacer/Hintergrund genutzt).
**Versionen:** alle (per Design RemoteViews).
**FIX (funktionserhaltend):** Statt `<View>` ein **`<ImageView>`** als Spacer/Hintergrund/Container nutzen.
**Quelle:** Vorfall Entropie Reductor 2026-05-11; developer.android.com (RemoteViews).

## 5. `setBackgroundColor` zerstoert runde Ecken (Shape-Drawable weg) + sed-Falle
**Symptom:** Pillen/Buttons mit Shape-Drawable (rounded corners) werden nach Farb-Setzen **eckig**.
**Ursache:** `RemoteViews.setInt(id, "setBackgroundColor", color)` ersetzt das **gesamte** Hintergrund-Drawable durch ein `ColorDrawable` → die runden Ecken des Shape-Drawables gehen verloren.
**Versionen:** alle.
**FIX (funktionserhaltend):** `setColorStateList(id, "setBackgroundTintList", ColorStateList.valueOf(color))` — das **tintet** das Shape-Drawable und behaelt Ecken/Oval. Ausnahme: vollflaechige, formlose Hintergruende duerfen weiter `setBackgroundColor`.
**sed-Falle (Tooling):** `<View>`→`<ImageView>` per `sed 's/<View />'` greift NICHT bei mehrzeiligen Tags (`<View\n …\n/>`), weil `sed` zeilenweise arbeitet → Python `re.sub(r'<View(\s)', r'<ImageView\1', content)`.
**Quelle:** Vorfall Entropie Reductor 2026-05-11.

## 6. Glance: Custom-Fonts NICHT unterstuetzt
**Symptom:** Eigene Schrift im Glance-Widget wird ignoriert.
**Ursache:** Glance unterstuetzt nur **System-Fonts** (RemoteViews-Limit). `FontFamily.Monospace`/`Serif`/`SansSerif`, Gewicht und Groesse gehen — eigene `.ttf` nicht.
**Versionen:** glance-appwidget (per Design).
**FIX:** Mit System-Font + `FontWeight`/`fontSize` arbeiten; braucht es zwingend eine eigene Schrift → klassisch mit `ImageView` (Text als Bild rendern) oder Widget-Design ohne Custom-Font.
**Quelle:** developer.android.com/develop/ui/compose/glance/build-ui.

## 7. LazyColumn verliert Scrollposition / ist langsam ohne `itemId`
**Symptom:** Beim Widget-Update springt die Liste an den Anfang; Updates ruckeln.
**Ursache:** Ohne stabile Item-IDs kann das ListView die Position nicht halten (Android 12+).
**Versionen:** glance-appwidget, Android 12+.
**FIX:** `items(items = list, itemId = { it.id.hashCode().toLong() }) { … }`.
**Quelle:** developer.android.com/develop/ui/compose/glance/build-ui.

## 8. Schwere Arbeit im Composable statt in `provideGlance()`
**Symptom:** Widget-Update langsam/blockt; Ressourcen/DB-Zugriffe am falschen Ort.
**Ursache:** Seit dem WorkManager-Session-basierten Update-Modell laeuft `provideGlance()` bereits in einem Worker — der richtige Ort fuer DB-/Netzwerk-/Ressourcen-Laden. Das Composable (`provideContent {}`) soll nur UI beschreiben.
**Versionen:** glance-appwidget 1.0+.
**FIX:** Daten in `provideGlance()` (vor `provideContent {}`) laden, Ergebnis in den Content geben. State persistent ueber `GlanceStateDefinition`/Repository, in-Widget-State via `remember { mutableStateOf() }` + `rememberCoroutineScope()` fuer async.
**Quelle:** developer.android.com/develop/ui/compose/glance/build-ui.

---

## Quellen (Stand 2026-06-19)
- developer.android.com/develop/ui/compose/glance/build-ui (Limits, provideGlance, SizeMode, LazyColumn=ListView, keine Custom-Fonts, itemId, Resource-IDs)
- developer.android.com/jetpack/androidx/releases/glance (Versionen: 1.1.1 / 1.2.0-rc01 / 1.3.0-alpha01)
- Eigene Production-Vorfaelle Entropie Reductor 2026-05-11 (Glance-Scrollbar 11 Iterationen, Toggle, RemoteViews-Fallen) — Memories `feedback_widget_scrollbar_master`, `feedback_widget_classic_over_glance`, `feedback_glance_widget_toggle_pattern`, `feedback_remoteviews_pitfalls`.
- ProAndroidDev / Medium (Husayn Hakeem, Max Kazantsev) — Glance unter Kontrolle bringen.
