# M1.2 — Drag-and-Drop · Aufruf

Der echte Code aus `GenialeIdeen/app/src/main/java/de/frank/genialeideen/ui/ListenScreen.kt`.
Vier Stellen, dann zieht es.

## 1 — Zustand und Randscrollen

```kotlin
val listState = rememberLazyListState()
val zustand = rememberReorderState(listState, bereich to gewaehlteKategorie)
val reduziert = LocalBewegungReduziert.current   // das CompositionLocal der App
ReorderAutoScroll(zustand)
```

`listKey` (hier `bereich to gewaehlteKategorie`) setzt den Zustand zurück, wenn
eine **andere** Liste angezeigt wird. Ohne das hängt nach dem Umschalten der
Kategorie noch die alte Karte am Finger.

## 2 — Die gezogene Reihenfolge lebt lokal

Das ist der Block, der leicht vergessen wird — **ohne ihn setzt ein Daten-Refresh
aus der Datenbank eine laufende Geste zurück**, und die Karte springt dem Finger
weg:

```kotlin
var reihenfolge by remember(bereich, gewaehlteKategorie) { mutableStateOf(liste.map(IdeeEntity::id)) }
LaunchedEffect(liste) {
    val ids = liste.map(IdeeEntity::id)
    reihenfolge = if (zustand.draggedId == null) ids else {
        // Aktualisierte Ideentexte dürfen eine laufende Sortiergeste nicht zurücksetzen.
        val vorhanden = ids.toSet()
        val lokal = reihenfolge.toSet()
        reihenfolge.filter { it in vorhanden } + ids.filter { it !in lokal }
    }
}
val nachId = remember(liste) { liste.associateBy(IdeeEntity::id) }
val sortiert = remember(reihenfolge, nachId) { reihenfolge.mapNotNull(nachId::get) }
```

Dieser Teil bleibt bewusst in der App: Er hängt an ihrem Datentyp
(`IdeeEntity`) und an ihrer Datenquelle. Das Modul sieht nur die `Long`-Liste.

## 3 — Die Geste an die Liste, nicht an den Griff

```kotlin
LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize().reorderViewport(
        state = zustand,
        order = { reihenfolge },
        onMove = { von, nach ->
            reihenfolge = reihenfolge.toMutableList().apply { add(nach, removeAt(von)) }
        },
        onDrop = { viewModel.schreibeReihenfolge(reihenfolge) },
        reducedMotion = reduziert,
    ),
    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 120.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
```

`reorderViewport` gehört an das **feste** Sichtfenster. An den Griff gehängt
würde es mitwandern, sobald zwei Karten tauschen — und die Geste bräche mitten
im Ziehen ab.

`onMove` verschiebt nur die lokale Liste, `onDrop` schreibt sie weg. Dadurch
gibt es während der Geste keinen Datenbankzugriff.

## 4 — Zeile und Griff

```kotlin
items(sortiert, key = { idee -> idee.id }) { idee ->
    Box(reorderItem(zustand, idee.id, reduziert)) {
        IdeenKarte(
            idee = idee,
            griff = reorderHandle(zustand, idee.id),
            aufTipp = { aufIdee(idee) },
            // …
        )
    }
}
```

`key` ist Pflicht und muss der `Long` sein, den auch `order` liefert — das Modul
findet die Karte über den Lazy-Item-Key.

`reorderItem` ist kein `Modifier.`-Aufruf: Der Empfänger ist der `LazyItemScope`,
deshalb steht es ohne Präfix da.

Den `griff`-Modifier legt die App auf die Fläche, die gegriffen werden soll — bei
GenialeIdeen ein Griffsymbol am Rand der Karte. Wer die ganze Karte greifbar
machen will, setzt ihn auf die Karte selbst.
