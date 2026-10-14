# M1.3 — Sidebar · Aufruf

Der echte Code aus `GenialeIdeen/app/src/main/java/de/frank/genialeideen/ui/ListenScreen.kt`.

```kotlin
val schublade = rememberDrawerState(DrawerValue.Closed)
val bereichsraum = rememberCoroutineScope()

// Eigene BackHandler VOR Sidebar(...) — der der Sidebar greift dann zuerst.
BackHandler(enabled = gewaehlteKategorie != null) { viewModel.waehleKategorie(null) }

Sidebar(
    schublade = schublade,
    drawerContent = {
        ModalDrawerSheet(
            drawerContainerColor = gold.hintergrund,
            drawerContentColor = gold.textPrimaer,
        ) {
            // Column + verticalScroll statt LazyColumn: alle Zeilen stehen schon vor der Geste.
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                KategorieZeile(
                    name = "Alle Ideen",
                    aufTipp = {
                        viewModel.waehleKategorie(null)
                        bereichsraum.launch { schublade.close() }
                    },
                )
                // …
            }
        }
    },
) {
    // Der eigentliche Bildschirm
    Box(Modifier.fillMaxSize()) { /* … */ }
}
```

Aufmachen per Knopf (z. B. Menü-Symbol in der Kopfleiste):

```kotlin
bereichsraum.launch { schublade.open() }
```
