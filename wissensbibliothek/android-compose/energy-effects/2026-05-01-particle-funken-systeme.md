---
title: "Particle-Spark-Systeme fuer Compose Canvas"
date: 2026-05-01
source: research-agent (Researcher 2 von 5)
project_context: "BestJournalFrank — Energy-Board-Feature fuer die Tagebuch-Timeline"
tags: [compose, canvas, particles, withframemillis, snapshotstatelist, object-pool, android, performance]
related:
  - 2026-05-01-stromfluss-canvas-shader.md
  - 2026-05-01-multi-layer-glow-bloom.md
  - 2026-05-01-overlay-lazycolumn-performance.md
summary: "Goldene Architektur fuer 60fps-Funken-Systeme in Compose: SnapshotStateList + withFrameMillis + drawBehind + Object Pool. Performance-Limits: 20-40 Funken stabil, 150+ kritisch. Mit rekursiven Verzweigungen wie echter Blitz."
---

# Researcher 2: Particle-System fuer Elektrizitaets-Funken in Jetpack Compose

## Kontext

Frage: Wie baut man ein performantes Particle-/Funken-System in Jetpack Compose
Canvas, das kontinuierlich neue Funken spawnt, sie ihre Lebensdauer abtragen
laesst und sauber wieder verschwinden? 60fps-Pflicht.

Anwendung: Entlang einer Stromlinie sollen kleine Funken seitlich abzweigen —
wie bei einer echten Hochspannungsleitung.

---

## Beste Particle-Architektur

**Das Kernproblem:** Compose ist ein deklaratives UI-Framework — es ist nicht
fuer einen klassischen Game-Loop gebaut. Wer pro Frame Recomposition ausloest,
bekommt nie stabile 60fps. Die Loesung ist, den Animationszustand vollstaendig
aus der Composition herauszuhalten und nur beim Zeichnen zu lesen.

### Die goldene Architektur: `drawBehind` + `withFrameMillis` + `SnapshotStateList`

```kotlin
// Daten-Klasse — kein Compose-State drin, plain data
data class Spark(
    var x: Float,
    var y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val bornAtMs: Long,
    val lifetimeMs: Long = (200..400).random().toLong(),
    val path: List<Offset>  // vorgenerierter Zickzack-Pfad, relativ zum Spawn-Punkt
)

// Object Pool — verhindert GC-Druck durch staendiges Allozieren/Freigeben
class SparkPool(private val maxSize: Int = 200) {
    private val pool = ArrayDeque<Spark>(maxSize)
    fun acquire(block: () -> Spark): Spark = pool.removeFirstOrNull() ?: block()
    fun release(spark: Spark) { if (pool.size < maxSize) pool.addLast(spark) }
}

@Composable
fun EnergyBoardWithSparks(
    spawnPoint: () -> Offset  // Lambda! Kein direkter State-Read in Composition
) {
    // SnapshotStateList: Compose SIEHT Aenderungen, KEIN teures Recompose der Elternebene
    val activeSparks = remember { mutableStateListOf<Spark>() }
    val pool = remember { SparkPool() }

    // Game-Loop laeuft in einem Coroutine-Scope — NIE direkt in Composition
    LaunchedEffect(Unit) {
        var lastSpawnMs = 0L
        while (true) {
            withFrameMillis { frameTimeMs ->
                val spawnPos = spawnPoint()  // Lambda wird nur hier ausgewertet

                // Spawn: max alle 80ms einen neuen Funken am Fokuspunkt
                if (frameTimeMs - lastSpawnMs > 80L) {
                    val spark = pool.acquire {
                        Spark(
                            x = spawnPos.x,
                            y = spawnPos.y,
                            velocityX = (-3f..3f).random(),
                            velocityY = (-5f..-1f).random(),
                            bornAtMs = frameTimeMs,
                            path = generateLightningPath(steps = 6, spread = 12f)
                        )
                    }
                    activeSparks.add(spark)
                    lastSpawnMs = frameTimeMs
                }

                // Update: Position und Lebensdauer pruefen
                val toRemove = mutableListOf<Spark>()
                activeSparks.forEach { spark ->
                    spark.x += spark.velocityX
                    spark.y += spark.velocityY
                    if (frameTimeMs - spark.bornAtMs > spark.lifetimeMs) {
                        toRemove.add(spark)
                    }
                }
                toRemove.forEach {
                    activeSparks.remove(it)
                    pool.release(it)
                }
            }
        }
    }

    // drawBehind: STATE WIRD NUR IN DER DRAW-PHASE GELESEN — kein Recompose
    Box(
        Modifier.drawBehind {
            activeSparks.forEach { spark ->
                val age = (System.currentTimeMillis() - spark.bornAtMs).toFloat()
                val alpha = (1f - age / spark.lifetimeMs).coerceIn(0f, 1f)  // Fade-out
                drawSparkPath(spark, alpha)
            }
        }
    )
}
```

**Warum dieser Ansatz?**
- `drawBehind` liest den State direkt in der Draw-Phase — Compose ueberspringt Composition und Layout komplett
- `withFrameMillis` synchronisiert den Loop exakt mit dem VSync-Signal (16,67ms bei 60fps)
- `SnapshotStateList` loest nur das Canvas-Neuzeichnen aus, nicht eine vollstaendige Recomposition des Eltern-Composables
- **Kein einziges `Animatable` pro Partikel** — `Animatable` erzeugt eigene Coroutines und ist fuer hunderte gleichzeitige Objekte viel zu teuer

---

## State-Management: Was genau triggert Recomposition?

| Ansatz | Verhalten | Empfehlung |
|---|---|---|
| `mutableStateListOf<Spark>()` | Jede `.add()/.remove()`-Operation triggert Neuzeichnen des Canvas, **nicht** Recomposition des gesamten Trees | **Ideal** |
| `mutableStateOf(listOf(...))` | Jede Zuweisung einer neuen Liste triggert Recomposition | Zu teuer |
| `ArrayDeque` ohne Compose-State | Kein automatisches Neuzeichnen | Braucht manuelle Invalidierung |
| Pro-Partikel `Animatable` | Je eine Coroutine pro Partikel, viel Overhead | Niemals bei >20 Partikeln |

**Kritische Regel:** Den State-Read in einen Lambda schieben, nicht direkt in
die Composable-Funktion. Das ist der Unterschied zwischen
`Modifier.graphicsLayer(alpha = state.value)` (triggert Recomposition) und
`Modifier.graphicsLayer { alpha = state.value }` (triggert nur Layer-Update).

---

## Spawn-Logik: Nur an interessanten Punkten

Der Spawn-Punkt kommt als Lambda `spawnPoint: () -> Offset` in den Composable
herein — so liest der Game-Loop die aktuelle Fokus-Position nur einmal pro Frame,
direkt in `withFrameMillis`, ohne jemals die Composition zu beruehren.

**Spawn-Rate-Steuerung:**
```kotlin
// Sanftes Burst-Muster: alle 80ms ein Funken am Fokuspunkt,
// aber bei bestimmten Events (z.B. Tipp-Geste) sofort 3-5 Funken auf einmal
val burstCount = if (isEventFrame) (3..5).random() else 1
repeat(burstCount) { spawnOneSpark(spawnPos, frameTimeMs) }
```

---

## Lebensdauer ohne Animatable: Zeit-basiertes Fade-out

Statt eines `Animatable<Float>` pro Funken wird die Lebensdauer rein rechnerisch ermittelt:

```kotlin
// In der drawBehind-Phase, pro Spark:
val elapsed = frameTimeMs - spark.bornAtMs  // ms seit Geburt
val progress = (elapsed / spark.lifetimeMs.toFloat()).coerceIn(0f, 1f)
val alpha = 1f - progress  // lineares Fade-out

// Optionales exponentielles Fade (wirkt schneller sterbend):
val alpha = (1f - progress).pow(2f)

// Farbe: von Weiss-Blau nach Blau-Transparent
val color = Color(
    red = lerp(0.8f, 0.2f, progress),
    green = lerp(0.9f, 0.5f, progress),
    blue = 1f,
    alpha = alpha
)
```

Kein einziger Coroutine-Start, kein einziges Animatable-Objekt. Alles ist
deterministisch und frame-basiert.

---

## Performance-Limits auf Mid-Range-Phones

Basierend auf Unity-Erfahrungswerten, dem Compose-Canvas-Overhead und praktischen
Beobachtungen aus Compose-Particle-Bibliotheken:

| Partikelanzahl | Verhalten auf Mid-Range (z.B. Snapdragon 7xx) |
|---|---|
| 20–50 gleichzeitig | Problemlos 60fps, keine Messung noetig |
| 50–150 gleichzeitig | Noch machbar mit `drawBehind`, kein Object-Pooling noetig |
| 150–300 gleichzeitig | Grenzbereich — Object-Pooling ist Pflicht, komplexe Paths belasten |
| 300+ gleichzeitig | Problematisch — Overdraw und GC-Druck brechen 60fps |

**Fuer unseren Use-Case (Funken entlang einer Stromlinie):** 20–40 gleichzeitig
aktive Funken sind mehr als genug fuer einen ueberzeugenden Effekt. Das liegt
weit unter jedem Limit.

**Overdraw vermeiden:** Funken sollten klein sein (3–8px Laenge) und mit
`blendMode = BlendMode.Screen` gezeichnet werden statt mit normalem Alpha-Blending —
das sieht elektrischer aus und ist nicht teurer.

---

## Elektrizitaets-Optik: Form, Farbe, Bewegung

### Zickzack-Pfad generieren (Midpoint-Displacement-Algorithmus)

```kotlin
fun generateLightningPath(
    steps: Int = 6,
    spread: Float = 15f
): List<Offset> {
    val points = mutableListOf(Offset(0f, 0f), Offset(0f, -30f))  // Aufwaerts
    repeat(steps) {
        val newPoints = mutableListOf<Offset>()
        for (i in 0 until points.size - 1) {
            newPoints.add(points[i])
            val mid = (points[i] + points[i + 1]) / 2f
            val displaced = mid + Offset(
                x = (-spread..spread).random(),
                y = (-spread / 3..spread / 3).random()
            )
            newPoints.add(displaced)
        }
        newPoints.add(points.last())
        points.clear()
        points.addAll(newPoints)
    }
    return points
}
```

### Farb-Palette fuer Elektrizitaet

- Kern: `Color(0.95f, 0.98f, 1.0f)` (fast weisses Blau) bei Alpha 1.0 — der hellste Punkt
- Aeusserer Glow: `Color(0.3f, 0.6f, 1.0f)` (kraeftiges Elektrik-Blau) mit Blur
- Sterbender Funken: `Color(0.1f, 0.3f, 0.9f)` bei Alpha → 0
- Fuer einen waermeren Plasma-Look: `Color(0.8f, 0.5f, 1.0f)` (Violett) als Alternative

### Glow-Effekt ohne teures RenderScript

```kotlin
// Zwei Zeichenoperationen pro Funken: erst dicker Glow, dann heller Kern
drawPath(sparkPath, color = glowColor.copy(alpha = alpha * 0.4f),
    style = Stroke(width = 4f * alpha))
drawPath(sparkPath, color = coreColor.copy(alpha = alpha),
    style = Stroke(width = 1.5f))
```

**Geschwindigkeit:** Funken bewegen sich 60–120px/Sekunde von der Stromlinie weg,
mit leichter Gravitation nach unten (0.5f px/frame Beschleunigung).

---

## Open-Source Bibliotheken — was taugt was?

| Bibliothek | Autor | Architektur | Taugt fuer uns? |
|---|---|---|---|
| **compose-particle-system (Quarks)** | CuriousNikhil | `mutableStateListOf` + LaunchedEffect | Gute Referenz, aber kein Elektrizitaets-Stil |
| **persona** | wangyung | Eigene AnimationSpec-Wrapper | Anpassbarer, aber mehr Overhead |
| **Kotlin Multiplatform Particles** | nezih94 | Compose Multiplatform Canvas | Interessant fuer Cross-Platform |
| **Leonids / android-particles** | plattysoft | Altes View-System | Nicht fuer Compose |

**Empfehlung:** Keiner dieser Libraries direkt nutzen — alle sind fuer generische
Effekte gebaut. Die obige Architektur in ~150 Zeilen Kotlin selbst implementieren
gibt volle Kontrolle ueber das Elektrizitaets-Look.

---

## Unkonventionelle Ideen

### Idee 1: Rekursive Verzweigungen wie echter Blitz

Statt gerader Funken-Pfade werden Haupt-Funken mit einer 20%-Wahrscheinlichkeit
zu einer zweiten, duenneren Verzweigung:

```kotlin
data class Spark(
    // ... Basis-Felder ...
    val generation: Int = 0,  // 0 = Haupt-Funken, 1 = erste Verzweigung, 2 = zweite
    val childSpawned: Boolean = false
)

// Im Update-Loop:
if (!spark.childSpawned && spark.generation < 2 && Random.nextFloat() < 0.2f) {
    val childSpark = spark.copy(
        velocityX = spark.velocityX * 0.7f + (-2f..2f).random(),
        velocityY = spark.velocityY * 0.6f,
        lifetimeMs = spark.lifetimeMs / 2,
        generation = spark.generation + 1,
        path = generateLightningPath(steps = 3, spread = 8f)  // kleiner Pfad
    )
    activeSparks.add(childSpark)
    spark.childSpawned = true
}
```

Das erzeugt Verzweigungen die wie echte Blitze aussehen — selbstaehnliche
Strukturen auf zwei Ebenen tief.

### Idee 2: "Stromlinien-Leck"-Modus

Statt alle Funken gleichmaessig zu spawnen: Der Spawn-Punkt "wandert" mit
leichtem Jitter entlang der Timeline-Linie. Funken entstehen bevorzugt an
Stellen mit hoher Kruemmung der Bahn (wo die "Spannung" am groessten waere).
Dafuer wird die Kruemmung der Bezier-Kurve der Stromlinie an jedem Punkt
berechnet — je staerker die Kurve, desto hoeher die Spawn-Rate. Das sieht
physikalisch korrekt aus und ist ein Alleinstellungsmerkmal.

---

## Referenzen

- [withFrameMillis – Jorge Castillo](https://jorgecastillo.dev/jetpack-compose-await-next-frame)
- [compose-particle-system (Quarks) auf GitHub](https://github.com/CuriousNikhil/compose-particle-system)
- [persona Particle System auf GitHub](https://github.com/wangyung/persona)
- [drawBehind / Recomposition vermeiden – Android Developers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)
- [Custom Canvas Animations in Compose – Rebecca Franks](https://medium.com/androiddevelopers/custom-canvas-animations-in-jetpack-compose-e7767e349339)
- [Recursive Lightning Bolts / Midpoint Displacement](https://craftofcoding.wordpress.com/2021/09/13/recursive-patterns-lightning-bolts/)
- [Unity Mobile Particle Optimization](https://learn.unity.com/tutorial/optimizing-particle-effects-for-mobile-applications)
- [Shimmer Animation ohne Recomposition](https://medium.com/@hzolfagharipour/shimmer-animation-in-jetpack-compose-without-recomposition-04d1317634a7)
- [Compose Performance – Android Developers](https://developer.android.com/develop/ui/compose/performance)

---

## Zusammenfassung in einem Satz

Die optimale Architektur fuer 60fps-Elektrizitaets-Funken in Compose ist:
**`SnapshotStateList` + `withFrameMillis`-Loop + `drawBehind`-Canvas +
Zeit-basiertes Fade-out ohne Animatable + Object-Pooling via `ArrayDeque`** —
mit vorgenerierten Zickzack-Pfaden (Midpoint-Displacement), Doppelschicht-Glow
(dicker Alpha-Strich + duenner heller Kern) und optionalen rekursiven
Verzweigungen fuer echten Blitz-Charakter.
