# Vorlage: die Anbindungsdatei

Die Anbindung ist die **einzige** Datei, die beim Einbau app-spezifisch wird.
Sie liegt neben der Modulkopie und gehört der App.

```
app/src/main/java/de/frank/module/dragreorder/
├── DragReorder.kt     ← Modulkopie, byte-identisch, NIE anfassen
└── Anbindung.kt       ← diese Datei, gehört der App
```

## Kopf (immer übernehmen)

```kotlin
// ──────────────────────────────────────────────────────────────────────
// Anbindung für Modul M1.1 — Drag & Drop Modul
//
// Diese Datei gehört der App, nicht der Bibliothek. Hier kommt alles hin,
// was sich von App zu App unterscheidet: Aussehen, Texte, Schnittstellen.
// Beim Nachziehen des Moduls wird sie NICHT überschrieben.
//
// Die Moduldatei daneben ist tabu — Anpassungen gehören hierher.
// ──────────────────────────────────────────────────────────────────────
```

## Was hineingehört

| | Beispiel |
|---|---|
| Aussehen aus dem Theme der App | `farbe = MaterialTheme.colorScheme.surfaceVariant` |
| Texte der App | `beschriftung = stringResource(R.string.sortieren)` |
| Umsetzung von Schnittstellen | `class NotizSicherungsQuelle : SicherungsQuelle { … }` |
| Bequeme Hülle um den Modulaufruf | `@Composable fun NotizenSortierbar(…)` |

## Was nicht hineingehört

Logik des Moduls. Wenn du hier den Ablauf nachbaust oder korrigierst, ist das
Modul falsch geschnitten — die Korrektur gehört in die Bibliothek und wird von
dort mit „zieh Mx.y nach" verteilt. Sonst hat jede App am Ende ihre eigene,
still abweichende Fassung, und die Bibliothek ist nur noch Dekoration.

## Aufbau

1. Kopf wie oben
2. Vorgabewerte aus dem Theme der **Ziel-App** (nicht der Ursprungs-App)
3. Umsetzungen der Schnittstellen aus Spalte 3 der Klassifikation
4. Eine Hülle, die das Modul mit diesen Werten aufruft — die Aufrufstelle im
   Bildschirm ruft dann nur noch die Hülle auf und bleibt schlank

## Andere Plattformen

Gleiches Muster, andere Endung: `Anbindung.cs` neben der `.cs`-Modulkopie,
`Anbindung.swift` neben der `.swift`-Kopie. Der Kommentarkopf bleibt derselbe,
nur die Kommentarzeichen wechseln (`//` überall, in XAML `<!-- -->`).
