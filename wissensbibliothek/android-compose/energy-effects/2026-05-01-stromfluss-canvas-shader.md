---
title: "Stromfluss in Compose Canvas — DashPath, AGSL, Gradient"
date: 2026-05-01
source: research-agent (Researcher 1 von 5)
project_context: "BestJournalFrank — Energy-Board-Feature fuer die Tagebuch-Timeline"
tags: [compose, canvas, agsl, pathEffect, brush, animation, android]
related:
  - 2026-05-01-particle-funken-systeme.md
  - 2026-05-01-multi-layer-glow-bloom.md
  - 2026-05-01-overlay-lazycolumn-performance.md
summary: "Drei produktionsreife Techniken fuer wandernde Energie-Pakete entlang einer Linie in Jetpack Compose. Empfehlung: 2-Schicht-Strategie aus DashPathEffect + Brush.linearGradient, optional AGSL Shader fuer Android 13+."
---

# Researcher 1: Wandernde Energie-Pakete in Jetpack Compose Canvas

## Kontext

Frage: Wie zeichnet man in Jetpack Compose eine Linie/einen Path, auf der sichtbar
ein Energie-Paket entlangwandert (also wandernde helle Stelle / Strom-Impuls)?
Was sind die saubersten Techniken in 2025/2026?

Recherche-Umfang: 8 WebSearches und 7 WebFetches. Drei produktionsreife Techniken
identifiziert.

---

## Technik 1: DashPathEffect mit animiertem Phase-Offset

**Das ist die sauberste und breitbandigste Loesung fuer eine "Strom wandert die Linie entlang"-Optik.**

Das `DashPathEffect` zeichnet eine gestrichelte Linie. Der `phase`-Parameter verschiebt,
wo die Darstellung auf dem Strichmuster beginnt. Animiert man die Phase von 0 bis zur
Gesamtlaenge eines Dash-Intervalls, wandert der helle Strich sichtbar vorwaerts —
exakt wie ein Strom-Impuls.

```kotlin
@Composable
fun ElectricLine(
    from: Offset,
    to: Offset,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "electric")

    // Phase laeuft von 0 bis dashLength — das ist der Wanderungs-Trick
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,          // muss Summe von on+off sein: 30+10 = 40
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00EAFF), Color(0xFF7B2FFF)),
                start = from,
                end = to
            ),
            start = from,
            end = to,
            strokeWidth = 3.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(30f, 10f),
                phase = phase
            )
        )
    }
}
```

**Staerken:**
- Funktioniert ab API 1 (keine Android-Version-Einschraenkung)
- Sehr wenig Code, direkt in `drawLine` oder `drawPath` integrierbar
- `rememberInfiniteTransition` liest den State nur in der Draw-Phase → keine unnoetige Recomposition

**Schwaechen:**
- Reine Strichelung — der "Leuchte-Glow"-Effekt fehlt (wandernde helle Stelle ohne Glow-Halo)
- Fuer einen echten Glanz-Effekt muss man zusaetzlich einen zweiten breiteren transparenten Strich darunter zeichnen

**Bewertung fuer Energie-Linie:** ★★★★☆ — Einstiegs-Loesung, sofort einsatzbereit.

---

## Technik 2: AGSL RuntimeShader mit time-Uniform (Android 13+, API 33)

**Die visuell ueberlegene Loesung mit echter GPU-Berechnung — fuer BestJournal ideal wenn min SDK ≥ 33.**

AGSL (Android Graphics Shader Language) ist Googles Fragment-Shader-Sprache, die
seit Android 13 verfuegbar ist. Man schreibt GLSL-aehnlichen Code direkt als
Kotlin-String und uebergibt ihn per `graphicsLayer` an die GPU.

```kotlin
@Language("AGSL")
val ENERGY_LINE_SHADER = """
    uniform float2 resolution;
    uniform float  time;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / resolution;

        // Wanderndes Energie-Paket: mod() sorgt fuer nahtlose Schleife
        float pulsePos  = mod(time * 0.4, 1.0);
        float dist      = abs(uv.x - pulsePos);

        // Gausssches Glow um das Paket herum
        float glow      = exp(-dist * dist * 60.0);

        // Grundlinie immer sichtbar, Paket leuchtet zusaetzlich
        float baseLine  = 0.12;
        float intensity = baseLine + glow * 0.9;

        // Cyan-Blau mit Leuchtanteil
        half3 col = half3(0.0, 0.85, 1.0) * intensity;
        return half4(col, intensity);
    }
""".trimIndent()

@Composable
fun ShaderEnergyLine() {
    val shader = remember { RuntimeShader(ENERGY_LINE_SHADER) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                time = frameMs / 1000f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .graphicsLayer {
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("resolution", size.width, size.height)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "image")
                    .asComposeRenderEffect()
            }
            .background(Color(0xFF001020))
    )
}
```

**Staerken:**
- Echter physikalisch wirkender Glow-Effekt durch Gauss-Funktion direkt am GPU
- Keinerlei Recomposition — `graphicsLayer` ist eine reine Render-Phase-Operation
- Vollstaendig anpassbar: Mehrere Pakete, Wellenmuster, Farbverlaeufe — alles im Shader-Code
- `withInfiniteAnimationFrameMillis` synchronisiert mit dem Vsync-Signal → kein Frame-Jitter

**Schwaechen:**
- Nur Android 13+ (API 33) — **wichtig fuer BestJournal Min-SDK pruefen**
- Shader-Debugging ist aufwendig (kein Breakpoint, nur visuelles Feedback)
- Fuer aeltere Geraete braucht man einen Fallback

**Bewertung fuer Energie-Linie:** ★★★★★ fuer Android 13+, sonst ★★☆☆☆ wegen Inkompatibilitaet.

---

## Technik 3: Brush.linearGradient mit wandernden Color-Stops

**Die kreativste reine-Compose-Loesung ohne Shader — fuer breite Kompatibilitaet.**

Anstatt eine Linie mit fester Farbe zu zeichnen, erstellt man einen
`Brush.linearGradient` dessen Color-Stop-Positionen pro Frame verschoben werden.
Ein heller Spot an Position X wandert mit der Animation nach vorne, der Rest
der Linie bleibt dunkel.

```kotlin
@Composable
fun GradientEnergyLine(
    from: Offset,
    to: Offset
) {
    val infiniteTransition = rememberInfiniteTransition(label = "energy")

    // offset laeuft von -0.3 bis 1.3 — 0.3 Ueberstand verhindert Spruenge
    val offset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Breite Glow-Linie im Hintergrund (immer sichtbar)
        drawLine(
            color = Color(0xFF003344),
            start = from, end = to,
            strokeWidth = 6.dp.toPx()
        )

        // Wanderndes helles Paket als linearGradient mit engem Spot
        val spotBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f              to Color.Transparent,
                (offset - 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                offset.coerceIn(0f, 1f)             to Color(0xFF00FFFF),
                (offset + 0.15f).coerceIn(0f, 1f) to Color.Transparent,
                1f              to Color.Transparent
            ),
            start = from,
            end = to
        )

        drawLine(
            brush = spotBrush,
            start = from, end = to,
            strokeWidth = 4.dp.toPx()
        )
    }
}
```

**Staerken:**
- Kein Shader noetig — funktioniert auf allen Android-Versionen
- Kombination aus dunkler Grundlinie + hellem Spot wirkt sehr natuerlich
- Leicht erweiterbar auf mehrere versetzt laufende Pakete

**Schwaechen:**
- Erstellt bei jedem Frame einen neuen `ShaderBrush`-Objekt intern → GC-Druck bei 60 FPS
- Besser: `TransformableBrush`-Pattern (siehe unten unter "Unentdeckte Tricks")
- Bei sehr vielen gleichzeitigen Linien (z.B. 20+ Timeline-Punkte) kann das zum Bottleneck werden

**Bewertung fuer Energie-Linie:** ★★★★☆ — sehr gut fuer ueberschaubare Linienzahl.

---

## Empfehlung: Technik-Kombination

Fuer das Energy-Board in BestJournal empfehle ich eine **zweischichtige Strategie:**

**Schicht 1 (Grundlinie):** `DashPathEffect` mit animiertem Phase-Offset — zeichnet
die strukturelle Linie zwischen den Timeline-Punkten, niedrige CPU-Last, alle Geraete.

**Schicht 2 (Glow-Paket):** `Brush.linearGradient` mit wanderndem Color-Stop —
zeichnet ueber Schicht 1 den weichen hellen Lichtimpuls, keine Shader-Abhaengigkeit.

**Optional fuer Android 13+:** `RuntimeShader` als separater `RenderEffect` auf
dem `Box`-Container — ersetzt dann Schicht 2 mit einem physikalisch korrekten
Gaussschen Glow.

Diese Kombination laeuft auf allen Geraeten und sieht auf Android 13+ noch besser aus.

---

## Unentdeckte Tricks

### Trick 1: State nur in der Draw-Phase lesen → keine Recomposition

Das ist der wichtigste Performance-Trick ueberhaupt. Wenn man `animateFloat`
innerhalb von `drawBehind` oder `Canvas {}` liest (nicht direkt in der
Composable-Funktion), triggert Compose **nur die Draw-Phase** neu, nicht
Composition und Layout. Kein Overhead, kein Flackern:

```kotlin
// FALSCH — liest in Composition-Phase:
val phase by infiniteTransition.animateFloat(...)
Canvas { drawLine(phase = phase) }   // erzeugt Recomposition bei JEDEM Frame!

// RICHTIG — liest direkt in Draw-Phase:
val phaseState = infiniteTransition.animateFloat(...)
Canvas {
    val phase = phaseState.value     // State.value hier lesen = nur Draw-Invalidierung
    drawLine(pathEffect = PathEffect.dashPathEffect(..., phase = phase))
}
```

### Trick 2: `PathMeasure.getSegment()` fuer "Linie waechst nach oben"-Animation

Fuer die Einstiegsanimation des Energy-Boards (Timeline-Linie erscheint von oben
nach unten) eignet sich `PathMeasure`:

```kotlin
val pathMeasure = remember { PathMeasure() }
val segmentPath = remember { Path() }

// pathCompletion: 0f → 1f animiert die Linienlaenge
pathMeasure.setPath(fullLinePath, false)
pathMeasure.getSegment(0f, pathCompletion * pathMeasure.length, segmentPath, true)
drawPath(segmentPath, ...)
```

Das kombiniert man dann mit dem wandernden Paket: Erst waechst die Linie auf,
dann faengt der Strom-Impuls an zu wandern.

---

## Referenzen

- [Dot. Dash. Design. — Mastering Lines in Jetpack Compose with PathEffect (ProAndroidDev, 2024)](https://proandroiddev.com/dot-dash-design-c30928484f79)
- [Custom Canvas Animations in Jetpack Compose — Android Developers Blog (Rebecca Franks)](https://medium.com/androiddevelopers/custom-canvas-animations-in-jetpack-compose-e7767e349339)
- [Shimmer Animation in Jetpack Compose Without Recomposition (Hasan Zolfagharipour)](https://medium.com/@hzolfagharipour/shimmer-animation-in-jetpack-compose-without-recomposition-04d1317634a7)
- [Animated Gradient Progress Bar in Compose — Canvas + ShaderBrush (Kappdev)](https://medium.com/@kappdev/creating-a-smooth-animated-progress-bar-in-jetpack-compose-canvas-drawing-and-gradient-animation-ddf07f77bb56)
- [TransformableBrush for Efficient Brush Animations (AmirHossein Aghajari)](https://medium.com/@aghajari/transformablebrush-for-efficient-brush-animations-in-jetpack-compose-eb566278ac5d)
- [Tutorial Android AGSL — Enhance your Compose Component with Shaders (Daniel)](https://medium.com/@spparks_/tutorial-android-agsl-enhance-your-compose-component-with-shaders-0fcdd6f552b2)
- [Unleashing the Power of AGSL in Android Compose (Infocusp)](https://www.infocusp.com/blogs/unleashing-the-power-of-agsl/)
- [GitHub: drinkthestars/shady — AGSL Shader Collection fuer Compose](https://github.com/drinkthestars/shady)
- [GitHub: JumpingKeyCaps/DynamicVisualEffectsAGSL — AGSL Playground Android 13+](https://github.com/JumpingKeyCaps/DynamicVisualEffectsAGSL)
- [Animated Infinity Loader — PathMeasure getSegment Technik (Kappdev)](https://medium.com/@kappdev/creating-an-animated-infinity-loader-with-jetpack-compose-exploring-path-animation-300e472eaf9c)
