---
title: "Overlay ueber LazyColumn — Performance-Architektur"
date: 2026-05-01
source: research-agent (Researcher 5 von 5)
project_context: "BestJournalFrank — Energy-Board-Feature fuer die Tagebuch-Timeline"
tags: [compose, lazycolumn, layoutinfo, withframenanos, performance, drawWithCache, nestedscroll, android]
related:
  - 2026-05-01-stromfluss-canvas-shader.md
  - 2026-05-01-particle-funken-systeme.md
summary: "Overlay-Architektur ueber LazyColumn mit 60fps. Schluessel: withFrameNanos statt delay, layoutInfo nur in Draw-Phase lesen, drawWithCache fuer Path-Caching, NestedScrollConnection fuer Touch-Pass-through."
---

# Researcher 5: Overlay ueber LazyColumn — Vollstaendige Architektur

## Kontext

Frage: Wie baut man in Jetpack Compose ein Overlay das (a) absolute Positionen
aus LazyListState.layoutInfo liest, (b) kontinuierlich animiert (60fps),
(c) Touch-Eingaben parallel zum Scrolling akzeptiert, ohne dass die Performance
einbricht oder das Scrollen ruckelt?

---

## Empfohlene Gesamt-Architektur

Das Herzstueck ist eine einfache `Box`-Schichtung: Die `LazyColumn` liegt unten,
der animierte Canvas liegt exakt darueber mit `Modifier.matchParentSize()`. Alle
Positions-Informationen kommen ausschliesslich aus `lazyListState.layoutInfo` —
nicht aus Item-Callbacks.

```kotlin
@Composable
fun EnergyBoard(entries: List<JournalEntry>, focusIndex: Int) {
    val listState = rememberLazyListState()

    // Frame-Loop: withFrameNanos synchronisiert mit Display-Refresh (60/90/120 Hz)
    var animNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var prevNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val delta = if (prevNanos == 0L) 0L
                            else (frameNanos - prevNanos).coerceIn(0L, 100_000_000L)
                prevNanos = frameNanos
                animNanos = frameNanos
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Schicht 1: Die eigentliche Liste
        LazyColumn(state = listState) {
            itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                JournalEntryItem(entry, isFocused = index == focusIndex)
            }
        }

        // Schicht 2: Animierter Overlay — matchParentSize = exakt gleiche Groesse wie Box
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .nestedScroll(passThroughConnection)
        ) {
            val info = listState.layoutInfo
            val items = info.visibleItemsInfo
            val vpStart = info.viewportStartOffset
            val t = animNanos / 1_000_000_000f      // Sekunden als Float

            val points = items.map { item ->
                val itemCenterY = (item.offset - vpStart + item.size / 2f)
                Offset(size.width / 2f, itemCenterY)
            }

            val focusItem = items.firstOrNull { it.index == focusIndex }
            val focusCenter = focusItem?.let {
                Offset(size.width / 2f, (it.offset - vpStart + it.size / 2f))
            }

            drawEnergyLines(points, focusCenter, t)
        }
    }
}
```

---

## 1. LazyListState.layoutInfo — welche Felder, wann lesen?

`LazyListState.layoutInfo` liefert ein `LazyListLayoutInfo`-Objekt mit diesen
wichtigen Feldern:

| Feld | Typ | Bedeutung |
|------|-----|-----------|
| `visibleItemsInfo` | `List<LazyListItemInfo>` | Alle gerade sichtbaren Elemente |
| `viewportStartOffset` | `Int` | Start des sichtbaren Bereichs in Pixel (meist 0) |
| `viewportEndOffset` | `Int` | Ende des sichtbaren Bereichs in Pixel |
| `viewportSize` | `IntSize` | Breite + Hoehe des Viewports |

Pro sichtbarem Item (`LazyListItemInfo`):
- `offset`: Pixel-Abstand vom Viewport-Start — **das ist die Y-Koordinate** des Item-Anfangs relativ zur Liste
- `size`: Pixel-Hoehe des Items
- `index`: Position in der Gesamtliste
- `key`: der stabile Schluessel (falls via `key = { }` gesetzt)

**Kritisches Frame-Timing:** `layoutInfo` darf man **nur im Draw-Lambda** oder
**nach dem Composition-Schritt** lesen. Liest man es waehrend der Composition,
triggert jedes Scrollen eine Recomposition der gesamten Elternkomponente. Die
sichere Methode ist, `layoutInfo` **innerhalb des `Canvas`-Lambdas** zu lesen —
dort findet nur die Draw-Phase statt, keine Recomposition.

---

## 2. Frame-Loop-Technik: withFrameNanos ist 2025/2026 Best Practice

```kotlin
LaunchedEffect(Unit) {
    var prev = 0L
    while (true) {
        withFrameNanos { nanos ->
            val delta = (nanos - prev).coerceIn(0L, 100_000_000L) // cap 100ms
            prev = nanos
            animState = nanos
        }
    }
}
```

Warum `withFrameNanos` statt `delay(16)`?
- `withFrameNanos` **synchronisiert mit dem tatsaechlichen Display-Refresh**
  (Choreographer). Bei 90Hz-Displays kommt der Frame alle 11ms, nicht alle 16ms.
- `delay(16)` ist ungenau — der Dispatcher kann zu frueh oder zu spaet aufwachen
- `withFrameNanos` **wartet automatisch** auf den naechsten VSYNC-Pulse
- Das **Delta-Capping** verhindert Animation-Spruenge nach App-Resume aus dem Hintergrund

**Was man NICHT tun sollte:** `while(true) { delay(16) }` — das koppelt die
Animation nicht an den echten Frame-Takt und produziert Jitter.

---

## 3. Zeichnen: drawWithCache fuer maximale Performance

Fuer den Energy-Overlay gilt diese Hierarchie:

- **`drawWithCache`**: Objekte (Path, Brush, Paint) werden gecacht und nur neu
  allokiert wenn sich die Canvas-Groesse oder gelesene States aendern. Ideal
  fuer Linien-Pfade die sich Form nach Form aufbauen.
- **`drawBehind`** / **`Canvas {}`**: Keine automatische Caching — bei jedem
  Frame werden Objekte neu erstellt. Gut genug wenn man nur einfache `drawLine`-
  Aufrufe macht (wenig Allokation).

```kotlin
// drawWithCache: Path-Objekt wird zwischen Frames wiederverwendet
Canvas(modifier = Modifier.matchParentSize().drawWithCache {
    val arcPath = Path()  // einmal allokiert, gecacht
    onDrawBehind {
        arcPath.reset()
        // ... Punkte eintragen
        drawPath(arcPath, brush = glowBrush)
    }
})
```

**Faustregel:** Wenn im Draw-Lambda Objekte wie `Path`, `Paint`, `Brush` oder
`Shader` erstellt werden — `drawWithCache` verwenden. Wenn nur primitive
`drawLine()`-Aufrufe gemacht werden — `drawBehind` reicht.

---

## 4. Recomposition-Strategie

| Technik | Wann | Ergebnis |
|---------|------|---------|
| `graphicsLayer { alpha = state.value }` (Lambda-Form!) | Fuer Alpha/Translation/Scale-Animationen auf ganzen Composables | Nur Draw-Phase, keine Recomposition |
| `derivedStateOf { listState.layoutInfo.visibleItemsInfo.size }` | Wenn man auf Threshold-Ereignisse reagieren will | Recomposition nur wenn sich der abgeleitete Wert aendert |
| `snapshotFlow { listState.layoutInfo }` | Wenn man layoutInfo als Flow benoetigt | Efficient, emittiert nur bei echter Aenderung |
| `neverEqualPolicy()` | Wenn man erzwingen will dass jede State-Schreibung als Aenderung gilt | Selten noetig |

**Goldene Regel:** State **so spaet wie moeglich** lesen. Im Draw-Lambda lesen =
nur Draw-Phase. Im `graphicsLayer {}`-Lambda lesen = nur Draw-Phase. In
`@Composable` lesen = Recomposition.

---

## 5. Touch-Handling ohne Scroll-Konflikte

Das Schluessel-Pattern ist `NestedScrollConnection` als Beobachter-Schicht ueber
der `LazyColumn`:

```kotlin
val passThroughConnection = remember {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Eigene Logik: z.B. Drag-Delta fuer Energie-Intensitaet merken
            dragDelta = available.y
            return Offset.Zero  // WICHTIG: Null zurueckgeben = kein Blocking!
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            return Offset.Zero
        }
    }
}

Box(Modifier.nestedScroll(passThroughConnection)) {
    LazyColumn(state = listState) { ... }
    Canvas(Modifier.matchParentSize()) { ... }
}
```

Fuer **Drag-Events speziell auf dem Canvas** (ohne Scroll-Konflikt):

```kotlin
Canvas(
    modifier = Modifier
        .matchParentSize()
        .pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()       // Event konsumieren NUR fuer eigene Logik
                    touchPosition = change.position
                    // LazyColumn scrollt trotzdem, weil der NestedScroll-Kanal offen ist
                }
            )
        }
)
```

**Wichtig:** `Offset.Zero` aus `onPreScroll` zurueckgeben bedeutet, dass die
LazyColumn alle Scroll-Deltas bekommt. Wuerde man `available` zurueckgeben,
wuerde der Scroll komplett blockiert.

---

## 3 typische Performance-Fallen (mit Loesungs-Pattern)

### Falle 1: `onGloballyPositioned` in LazyColumn-Items

```kotlin
// SCHLECHT — fuer jeden Item bei jedem Scroll-Frame!
Text("...", modifier = Modifier.onGloballyPositioned { coords ->
    positions[index] = coords.positionInRoot()
})
```

Problem: `onGloballyPositioned` traversiert den gesamten UI-Baum fuer jedes Item,
bei jedem Scroll-Frame. Mit 20 sichtbaren Items = 20× pro Frame auf dem
Main-Thread.

Loesung: `layoutInfo.visibleItemsInfo` liest alle Positionen in einem einzigen
Aufruf, bereits im Draw-Context.

### Falle 2: layoutInfo als State-Objekt lesen in der Composition

```kotlin
// SCHLECHT — triggert Recomposition bei JEDEM Scroll-Pixel!
@Composable
fun EnergyOverlay(listState: LazyListState) {
    val items = listState.layoutInfo.visibleItemsInfo  // State-Read in Composition!
    Canvas(...) { drawPoints(items) }
}
```

Loesung: `layoutInfo` direkt im Canvas-Lambda lesen — dann liegt der Read in
der Draw-Phase:

```kotlin
Canvas(...) {
    val items = listState.layoutInfo.visibleItemsInfo  // Kein Recomposition-Trigger
}
```

### Falle 3: Object-Allokation im Frame-Loop

```kotlin
// SCHLECHT — animNaNos aendert sich 60x/Sekunde → LaunchedEffect neu gestartet!
LaunchedEffect(animNanos) {
    // ...
}
```

Loesung: `LaunchedEffect(Unit)` mit interner `while(true)` Schleife — die
Effect-Instanz laeuft ein einziges Mal und allokiert keine Objekte pro Frame.
Zusaetzlich `drawWithCache` verwenden damit Path/Brush im Canvas gecacht wird.

---

## 2 ungewoehnliche Tricks

### Trick 1: `Modifier.onLayoutRectChanged` statt `onGloballyPositioned` (Compose 1.7+)

```kotlin
// NEU in Compose 1.7 (BOM 2025.04.01+)
Modifier.onLayoutRectChanged(
    throttleMillis = 32,   // max 30 Updates/Sekunde
    debounceMillis = 16    // debounced
) { bounds ->
    // bounds.positionInRoot() — leichtgewichtig, nicht main-thread-blockierend
}
```

Warum kaum bekannt: Die API wurde still in Compose 1.7 eingefuehrt ohne grosse
Ankuendigung. Sie hat native Throttling/Debouncing eingebaut —
`onGloballyPositioned` hat das nicht.

### Trick 2: `graphicsLayer`-Lambda fuer "kostenloses" Overlay-Dimmen

```kotlin
Canvas(
    modifier = Modifier
        .matchParentSize()
        .graphicsLayer {
            // Nur dieser Lambda laeuft pro Frame — KEINE Recomposition, KEINE Relayout
            alpha = if (isScrolling) 0.3f else 1.0f
            blendMode = BlendMode.Screen  // Additive Blending fuer Glow-Effekt
        }
)
```

Der `graphicsLayer {}`-Lambda mit State-Reads darin laeuft **ausschliesslich in
der Draw-Phase** — Compose ueberspringt Composition und Layout vollstaendig. Das
ist der schnellste Weg um ein Overlay visuell zu modifizieren ohne irgendetwas
zu recomponieren.

---

## Referenzen

- [Android Dev: Lists und grids mit LazyListState](https://developer.android.com/develop/ui/compose/lists)
- [LazyListState API Reference](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState)
- [Android Dev: Graphics Modifiers (drawWithCache, drawBehind)](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)
- [Android Dev: Nested Scrolling](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/nested-scroll)
- [Android Dev: Side Effects (snapshotFlow, derivedStateOf)](https://developer.android.com/develop/ui/compose/side-effects)
- [Precision Overlays: onGloballyPositioned vs. onLayoutRectChanged](https://medium.com/@TonyGnk/mastering-precision-overlays-in-jetpack-compose-from-ongloballypositioned-to-onlayoutrectchanged-1c2febc476dd)
- [withFrameNanos — await next frame](https://jorgecastillo.dev/jetpack-compose-await-next-frame)
- [Compose UI Performance Secrets Part 2](https://tanishranjan.medium.com/compose-ui-performance-secrets-part-2-5-advanced-techniques-for-ultra-smooth-apps-3dd7d65311c4)
- [drawBehind, drawWithContent, drawWithCache erklaert](https://nameisjayant.medium.com/drawbehind-drawwithcontent-drawwithcache-modifier-in-jetpack-compose-c110108d4c5d)
- [Visibility APIs in Jetpack Compose 1.9 (onLayoutRectChanged)](https://proandroiddev.com/visibility-apis-in-jetpack-compose-1-9-easier-cleaner-but-not-quite-there-yet-9bbfdb60bd6b)
- [Understanding Nested Scrolling in Jetpack Compose](https://medium.com/androiddevelopers/understanding-nested-scrolling-in-jetpack-compose-eb57c1ea0af0)
