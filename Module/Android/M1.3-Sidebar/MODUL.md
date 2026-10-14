# M1.3 — Sidebar

Linke Seitenleiste, die per Wisch von links nach rechts aufgeht und per Wisch
nach links wieder zugeht — ohne Ruckeln, weder beim Auf- noch beim Zuwischen.
Zurück-Taste schließt eine offene Leiste, statt die App zu verlassen.

- **Stand:** v1
- **Plattform:** Android (Kotlin, Jetpack Compose, Material 3)
- **Angelegt:** 14.10.2026 10:03
- **Ordner:** `Module/Android/M1.3-Sidebar/`
- **Kurzname:** `sidebar` — steckt im Namensraum `de.frank.module.sidebar`

## Herkunft

- **App:** GenialeIdeen (Startbildschirm, Kategorien-Seitenleiste)
- **Commit:** `6dd1a8342`
- **Quellstand:** `6dd1a8342` — vor dem ersten Kopieren genommen
- **Dateien:**
  - `GenialeIdeen/app/src/main/java/de/frank/genialeideen/ui/ListenScreen.kt`
    (der `ModalNavigationDrawer` samt `pointerInput`-Wischgeste und der
    Schubladen-`BackHandler`)

## Warum sie nicht ruckelt — die Technik

Das ist der eigentliche Inhalt des Moduls. Wer eine Sidebar von Hand baut,
läuft fast immer in dieselben Ruckler. Die vier Punkte, die sie verhindern:

1. **Der Finger zieht die Leiste nicht mit.** Der native Drawer-Drag lässt
   die Leiste dem Finger folgen; jedes Bild, das während dieser Fingerphase
   zu spät kommt, ist ein sichtbarer Ruckler, und beim Loslassen setzt die
   Restanimation an einem krummen Versatz an. Stattdessen erkennt das Modul
   nur die *Absicht* (waagerecht über `touchSlop`, richtige Richtung) und
   startet dann `open()` bzw. `close()` — **eine durchgehende Animation** von
   Anfang bis Ende.
2. **Die Geste wird im `PointerEventPass.Initial` übernommen** — also *bevor*
   der native Drawer-Drag sie sieht. Danach wird jede Bewegung `consume()`d.
   So schaltet auch beim Zuwischen niemand einen Finger-Versatz vor die
   Animation.
3. **Der native Drag ist fast immer aus:**
   `gesturesEnabled = isOpen && !isAnimationRunning`. Bei geschlossener oder
   gerade laufender Leiste kann er nichts dazwischenfunken. Und während eine
   Animation läuft, wird keine neue Geste angenommen
   (`if (isAnimationRunning) return`), damit sich keine zwei Animationen
   gegenseitig ins Wort fallen.
4. **Senkrechte Gesten und Listen bleiben unberührt.** Ist der Weg senkrecht
   größer als waagerecht → abbrechen. Hat eine scrollende Liste die Geste im
   `Final`-Pass schon verbraucht → abbrechen. Dadurch scrollt die Liste
   dahinter ganz normal und ruckelt auch nicht.

Ein fünfter Punkt liegt beim **Inhalt** der Leiste und damit bei der App —
siehe „Host muss liefern".

## Was drin ist

Eine Datei, `Sidebar.kt`. Nach außen:

| Teil | Was |
|---|---|
| `Sidebar(schublade, drawerContent, modifier, content)` | Der fertige Baustein: `ModalNavigationDrawer` mit Wischgeste und Zurück-Taste. Der Normalfall |
| `Modifier.sidebarWischGeste(schublade, bereichsraum)` | Nur die Geste, für Apps, die den Drawer selbst aufbauen wollen. Dann `gesturesEnabled = schublade.isOpen && !schublade.isAnimationRunning` selbst setzen |

Den `DrawerState` erzeugt die App (`rememberDrawerState(DrawerValue.Closed)`),
weil sie ihn auch zum Schließen nach einem Tipp braucht.

## Der Schnitt

> **Das Modul besitzt die Geste, die Animation und die Zurück-Taste. Die App
> besitzt, was in der Leiste steht und was ein Tipp darin auslöst.**

Nichts gezeichnet: Farben, `ModalDrawerSheet`, Zeilen, Überschrift — alles in
`drawerContent` der App.

## Host muss liefern

| Was | Warum |
|---|---|
| **Inhalt der Leiste ohne `LazyColumn`** — bei überschaubaren Listen eine `Column` mit `verticalScroll(rememberScrollState())` | Eine `LazyColumn` baut ihre Zeilen erst, wenn die Leiste ins Bild kommt — also mitten in der Öffnungsanimation. Genau dieser einmalige Aufbau war in GenialeIdeen ein Ruckler (Commit `81ae0e4f5`, „beschleunige Drawer"). Die `Column` baut alles vor der Geste |
| Nach einem Tipp in der Leiste `scope.launch { schublade.close() }` | Das Schließen nach Auswahl ist App-Logik |
| Eigene `BackHandler` **vor** `Sidebar(...)` aufrufen | Der zuletzt registrierte greift zuerst; so schließt Zurück erst die Leiste, dann greift die App-Logik |
| `androidx.activity:activity-compose` | für `BackHandler` |

## Gespeicherter Zustand

Keiner. Ob die Leiste offen ist, überlebt keinen Neustart.

- —

## Braucht Module

- —

## Mindestens

- Compose BOM 2025.01.01 (Material 3), Kotlin 2.1.0
- `androidx.activity:activity-compose` 1.9.3
- `kotlinx.coroutines`

## Konsumenten

| App | Stand | Pfad der Kopie |
|---|---|---|
| GenialeIdeen | v1 | `GenialeIdeen/app/src/main/java/de/frank/module/sidebar/` |
| Gedankenspeicher | v1 | `Gedankenspeicher/app/src/main/java/de/frank/module/sidebar/` — nutzt nur `sidebarWischGeste` am eigenen Drawer (Geste nur im Verlauf, eigener Scrim, Sitzungsliste bleibt lazy) |

## Abnahme

Kein Test vorhanden — eine Wischgeste über echte Bildwechsel lässt sich ohne
Gerät nicht sinnvoll prüfen. **Abnahme ist der grüne Build plus Auf- und
Zuwischen von Hand.**

## Änderungen

| Version | Datum | Was | Bricht Anbindung | Commit |
|---|---|---|---|---|
| v1 | 14.10.2026 | aus GenialeIdeen herausgelöst. Geste wörtlich verschoben; der `ModalNavigationDrawer` und der Schubladen-`BackHandler` kamen als `Sidebar(...)` mit herein, der Coroutine-Scope für die Geste ist jetzt modulintern. | — | — |
