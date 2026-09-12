# M1.2 — Drag-and-Drop

Karten einer Liste per Langdruck greifen und mit dem Finger umsortieren — die
gezogene Karte klebt am Finger, die Nachbarn weichen weich aus, am Rand scrollt
die Liste mit, und beim Loslassen legt sich die Karte sanft auf ihren Platz.

- **Stand:** v1
- **Plattform:** Android (Kotlin, Jetpack Compose)
- **Angelegt:** 12.09.2026 19:14
- **Ordner:** `Module/Android/M1.2-Drag-and-Drop/`
- **Kurzname:** `draganddrop` — steckt im Namensraum `de.frank.module.draganddrop`

## Herkunft

- **App:** GenialeIdeen (Startbildschirm, die drei Ideen-Listen)
- **Commit:** `c1d94d6fc`
- **Quellstand:** `c1d94d6fc` — vor dem ersten Kopieren genommen; die Quelldateien
  waren während der Herauslösung unverändert
- **Dateien:**
  - `GenialeIdeen/app/src/main/java/de/frank/genialeideen/ui/DragReorder.kt` (vollständig)
  - `GenialeIdeen/app/src/main/java/de/frank/genialeideen/ui/ListenScreen.kt`
    (nur die Platzwechsel-Animation der Nachbarkarten, Zeilen ~310–320)

## Was drin ist

Eine Datei, `DragReorder.kt`. Nach außen:

| Teil | Was |
|---|---|
| `ReorderState` | Der Zustand einer laufenden Geste: wer hängt am Finger, wo steht er, wann wird getauscht. `draggedId`, `dragging`, `isDragging(id)` sind lesbar |
| `rememberReorderState(listState, listKey)` | Erzeugt den Zustand. `listKey` setzt ihn zurück, wenn die Liste wechselt (bei GenialeIdeen: Bereich und Kategorie) |
| `Modifier.reorderViewport(state, order, onMove, onDrop, reducedMotion)` | Die Geste. Gehört an die `LazyColumn`, **nicht** an den Griff |
| `LazyItemScope.reorderItem(state, id, reducedMotion)` | Der Modifier für die Zeile: Hochsetzen, Mitziehen **und** das weiche Ausweichen der Nachbarn. Der Normalfall |
| `Modifier.reorderRow(state, id)` | Nur Hochsetzen und Mitziehen, ohne Ausweich-Animation. Für Listen, die keine `LazyColumn` sind |
| `reorderHandle(state, id)` | Der Modifier für die Greiffläche. Die App entscheidet, welcher Bereich der Karte das ist |
| `ReorderAutoScroll(state)` | Das Randscrollen. Einmal neben die Liste setzen |

## Der Schnitt

> **Das Modul besitzt die Geste und die Geometrie. Die App besitzt die Liste
> und was mit der neuen Reihenfolge passiert.**

Das Modul kennt keinen Datensatz der App — es arbeitet ausschließlich mit
`Long`-Schlüsseln, wie sie schon als `key` in der `LazyColumn` stehen. Die
Reihenfolge liegt in der App (`order`), das Modul sagt nur „tausche Platz X mit
Y" (`onMove`) und „der Finger ist weg" (`onDrop`). Was dabei gespeichert wird,
entscheidet die App.

**Nichts gezeichnet, nichts gestaltet.** Das Modul liefert nur Modifier; wie die
Karte aussieht, wo ihr Griff sitzt und ob sie beim Ziehen einen Schatten wirft,
bleibt Sache der App. Genau das erfüllt die Zusage „gleiche Funktionen, eigenes
Aussehen".

**Die Geometrie kommt ausschließlich aus den echten Lazy-Item-Plätzen** — keine
aufaddierten Höhen, keine Abstände, keine Scrollkorrekturen. Deshalb stimmt das
Ziehen auch bei unterschiedlich hohen Karten, bei laufendem Randscrollen und
über die Kopfleiste hinaus. Wer hier etwas ändert, ändert das Gefühl.

## Host muss liefern

Nichts. Alles läuft über Parameter.

- —

## Gespeicherter Zustand

Keiner. Das Modul liest und schreibt nichts, was den Neustart überlebt — es
meldet die neue Reihenfolge über `onDrop` an die App, und die entscheidet.

- —

## Braucht Module

- —

## Mindestens

- Compose BOM 2025.01.01, Kotlin 2.1.0
- `LazyListState.requestScrollToItem` und `Modifier.animateItem` — beide ab
  Compose Foundation 1.7. Eine ältere App bricht beim Kompilieren
- `kotlinx.coroutines`

## Die Werte, die das Gefühl machen

Sie stehen bewusst als Literale im Modul, nicht als Parameter: Der Bereich ist
so ausgemessen, und ein Parameter würde nur einladen, ihn zu verstellen. Braucht
eine zweite App wirklich andere Werte, wird daraus v2.

| Wert | Wo | Was |
|---|---|---|
| `180 ms` `tween` | `ReorderState.stop` | Das weiche Ablegen. Bei reduzierter Bewegung `0` |
| `spring(1f, 450f)` | `reorderItem` | Das Ausweichen der Nachbarkarten — kritisch gedämpft, schwingt nicht nach |
| `88.dp` | `ReorderAutoScroll` | Breite der Randzone, höchstens ein Drittel des Sichtfensters |
| `560.dp/s` | `ReorderAutoScroll` | Höchstgeschwindigkeit des Randscrollens, quadratisch nach Fingerabstand |
| `0.032f` | `followEdges` | Deckel für den Bildabstand — nach einem Ruckler springt die Liste nicht |

## Konsumenten

| App | Stand | Pfad der Kopie |
|---|---|---|
| GenialeIdeen | v1 | `GenialeIdeen/app/src/main/java/de/frank/module/draganddrop/` |

## Abnahme

Kein Test vorhanden — es gab in der Quell-App keinen, und eine Zeigegeste über
echte Lazy-Layout-Maße lässt sich ohne Gerät nicht sinnvoll prüfen. **Abnahme
ist der grüne Build plus Ziehen von Hand.** Der nächste, der hier etwas ändert,
weiß damit, worauf er sich nicht verlassen kann.

## Änderungen

Eine Zeile je Version. Ändert sich eine öffentliche Funktion, ein Parameter
oder eine Schnittstelle, **muss** „bricht Anbindung" dabeistehen — daran
erkennt `modul-einbauen`, dass Nachziehen mehr ist als Überschreiben. Bauen
andere Module auf diesem auf, hier ebenfalls nennen.

| Version | Datum | Was | Bricht Anbindung | Commit |
|---|---|---|---|---|
| v1 | 12.09.2026 | aus GenialeIdeen herausgelöst. `LocalBewegungReduziert` wurde zum Parameter `reducedMotion`; die Platzwechsel-Animation der Nachbarkarten kam als `reorderItem` aus `ListenScreen` mit herein, weil ohne sie das Ziehen hart wirkt. | — | — |
