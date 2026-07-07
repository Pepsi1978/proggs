---
title: "Multi-Layer-Glow & Bloom — Profitechniken in Compose Canvas"
date: 2026-05-01
source: research-agent (Researcher 4 von 5)
project_context: "BestJournalFrank — Energy-Board-Feature fuer die Tagebuch-Timeline"
tags: [compose, canvas, glow, bloom, blendmode, blurmaskfilter, vfx, after-effects, photoshop]
related:
  - 2026-05-01-stromfluss-canvas-shader.md
  - 2026-05-01-particle-funken-systeme.md
  - 2026-05-01-energy-ui-inspirationen.md
summary: "Multi-Layer-Glow mit 6 statt 3 Schichten. Schwarzkoerperstrahlung erklaert warum der Kern weiss ist. Saber-Plugin und Photoshop Outer Glow Geheimnisse. Pulsing-Mathematik mit irrationalen Frequenzverhaeltnissen."
---

# Researcher 4: Elektrisches Gluehen in Compose Canvas — Profitechniken

## Kontext

Frage: Wie erzeugt man in Software-Rendering (Compose Canvas, ohne Vulkan/OpenGL
direkt) ein UEBERZEUGENDES elektrisches Gluehen mit Bloom-Charakter?
Was machen Profi-VFX-Artists, was davon ist in Compose machbar?

---

## Warum der Kern immer weiss ist (und der Halo farbig)

Das ist keine Designentscheidung, sondern Physik. Echte elektrische Boegen
erreichen Temperaturen von 3.000 bis 30.000 K. Nach der Schwarzkoerperstrahlung
(Plancksches Strahlungsgesetz) verschiebt sich das Emissionsmaximum bei
steigender Temperatur in Richtung kuerzerer Wellenlaengen. Bei ca. 6.000 K liegt
das Maximum im Sichtbaren — aber bei extremen Temperaturen wie einem Lichtbogen
strahlt das Objekt ueber das gesamte sichtbare Spektrum so intensiv, dass alle
drei Zapfentypen unseres Auges gleichermassen uebersaettigt werden. Das Ergebnis:
weiss. Der Halo aussen ist kuehler und strahlt bevorzugt im Blau-Cyan-Bereich —
das ist die Zone wo die Intensitaet noch farblich wahrnehmbar ist, ohne alle
Rezeptoren zu ueberwaeltigen.

**Fuer Compose:** Kern = `Color(1f, 1f, 1f, 1f)`, erster Ring = Zielfarbe mit
80% Alpha, aeussere Ringe = Zielfarbe mit fallender Saettigung und Alpha.

---

## 1. Multi-Layer-Glow — Die Mathematik

Profis verwenden nicht 3, sondern mindestens **6 konzentrische Schichten** pro
Gluehen. Warum 6? Weil der menschliche Bloom-Eindruck aus drei physikalisch
unterschiedlichen Phaenomenen besteht — jedes braucht zwei Schichten (eng + weit):

| Schicht | Zweck | Physikalisches Phaenomen |
|---------|-------|------------------------|
| 1+2 | Harter Kern (solid + winziger Bloom) | Eigenlicht der Entladung |
| 3+4 | Mittlerer Halo | Ionisierte Umgebungsluft, Lichtstreuung |
| 5+6 | Weiter Ambient-Bloom | Atmosphaerische Streuung, Auge-/Linseneffekte |

### Pseudo-Code fuer Compose Canvas DrawScope

```kotlin
fun DrawScope.drawElectricGlow(
    path: Path,
    coreColor: Color,     // z.B. Color.White
    glowColor: Color,     // z.B. Color(0xFF03A9F4)
    intensity: Float      // 0f..1f — Puls-Wert aus Animation
) {
    val nativePaint = Paint().asFrameworkPaint()

    // SCHICHT 6 — Weitester Ambient-Bloom (kaelteste Aussenschicht)
    nativePaint.apply {
        color = android.graphics.Color.TRANSPARENT
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 32f
        maskFilter = BlurMaskFilter(48f * intensity, BlurMaskFilter.Blur.NORMAL)
        setColor(glowColor.copy(alpha = 0.08f).toArgb())
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), nativePaint)

    // SCHICHT 5 — Breiter Halo
    nativePaint.apply {
        strokeWidth = 20f
        maskFilter = BlurMaskFilter(28f * intensity, BlurMaskFilter.Blur.NORMAL)
        setColor(glowColor.copy(alpha = 0.15f).toArgb())
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), nativePaint)

    // SCHICHT 4 — Mittlerer Halo (Hauptgluehen)
    nativePaint.apply {
        strokeWidth = 12f
        maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
        setColor(glowColor.copy(alpha = 0.35f).toArgb())
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), nativePaint)

    // SCHICHT 3 — Innerer Halo (intensive Farbe)
    nativePaint.apply {
        strokeWidth = 7f
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        setColor(glowColor.copy(alpha = 0.65f).toArgb())
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), nativePaint)

    // SCHICHT 2 — Harter Rand (warm-weiss, fast kein Blur)
    nativePaint.apply {
        strokeWidth = 3.5f
        maskFilter = BlurMaskFilter(2.5f, BlurMaskFilter.Blur.NORMAL)
        setColor(coreColor.copy(alpha = 0.90f).toArgb())
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), nativePaint)

    // SCHICHT 1 — Solider Kern (kein Blur, volle Deckkraft)
    nativePaint.apply {
        strokeWidth = 2f
        maskFilter = null
        setColor(coreColor.toArgb())
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), nativePaint)
}
```

**Wichtige Faustregeln fuer die Layer-Werte:**
- Alpha pro Schicht folgt einer Potenz-Kurve: `alpha = 0.08, 0.15, 0.35, 0.65, 0.90, 1.0`
- Blur-Radius verdoppelt sich roughly pro Schicht nach aussen: `2.5 → 8 → 16 → 28 → 48`
- StrokeWidth ebenfalls nach aussen wachsend: `2 → 3.5 → 7 → 12 → 20 → 32`

---

## 2. RenderEffect mit Gaussian Blur (API 31+)

Fuer einen noch weicheren, filmischeren Bloom-Look kann man **zusaetzlich** das
gesamte Glow-Composable mit RenderEffect verschwimmen:

```kotlin
fun Modifier.electricBloom(blurRadius: Float = 12f): Modifier = this.then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(
                    blurRadius, blurRadius,
                    Shader.TileMode.DECAL
                )
                .asComposeRenderEffect()
        }
    } else {
        // Fallback fuer API < 31: BlurMaskFilter in Canvas
        Modifier
    }
)
```

**Trick:** Man rendert die Glow-Schichten in ein eigenes `Box`-Composable und
legt dieses mit `electricBloom()` **hinter** den eigentlichen Inhalt. So bekommt
man zweistufiges Blur: einmal im Canvas-Layer selbst (BlurMaskFilter), einmal
als RenderEffect-Blur auf dem Composable. Das entspricht dem professionellen
"Pre-filtered Bloom"-Ansatz aus After Effects.

---

## 3. BlendMode.Plus und BlendMode.Screen — Richtige Verwendung

**BlendMode.Plus** (additives Mischen): Farben werden addiert — ideal fuer
echtes Licht-Ueberlappen. Zwei blaue Lichter = helleres Blau, bis zu Weiss.

**BlendMode.Screen**: Sanfteres additives Mischen, verhindert zu schnelles
Uebersaettigen zu Weiss.

```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    drawIntoCanvas { canvas ->
        canvas.saveLayer(
            bounds = Rect(0f, 0f, size.width, size.height),
            paint = Paint()
        )

        drawRect(Color.Black)

        // Erste Glow-Schicht mit Plus — addiert sich zur Szene
        drawPath(
            path = myGlowPath,
            color = glowColor.copy(alpha = 0.5f),
            style = Stroke(width = 8f),
            blendMode = BlendMode.Plus
        )

        // Zweite Glow-Schicht, breiter
        drawPath(
            path = myGlowPath,
            color = glowColor.copy(alpha = 0.2f),
            style = Stroke(width = 24f),
            blendMode = BlendMode.Plus
        )

        canvas.restore()
    }
}
```

**Empfehlung pro Schicht:**
- Kern (Schicht 1+2): `BlendMode.SrcOver` — fester Kern, keine Additiv-Effekte
- Mittlere Halos (Schicht 3+4): `BlendMode.Plus` — Kernlicht addiert sich
- Aussen-Bloom (Schicht 5+6): `BlendMode.Screen` — sanfter, verhindert Ueberbrennen

**Pflicht:** `CompositingStrategy.Offscreen` im `graphicsLayer` setzen, wenn
BlendModes ausserhalb von `Canvas` auf Composable-Ebene genutzt werden.

---

## 4. Pulsing-Mathematik — Organisches Atmen statt langweiliges sin(t)

Profis ueberlagern **3–4 Sinuswellen mit irrationalen Frequenzverhaeltnissen**
(damit sie nie in Phase kommen und ein sich nie wiederholendes Muster entstehen):

```kotlin
fun organicPulse(timeMs: Long): Float {
    val t = timeMs / 1000f  // Zeit in Sekunden

    // Drei ueberlagerte Wellen mit irrationalen Frequenzverhaeltnissen
    val wave1 = sin(t * 0.7f)          // langsames Grundatmen (1.4s Periode)
    val wave2 = sin(t * 1.618f) * 0.3f // goldener Schnitt — nie synchron mit wave1
    val wave3 = sin(t * 2.7f)  * 0.1f  // schnelles Flackern

    // Kombinieren: 0.0f..1.0f normalisiert
    val raw = (wave1 + wave2 + wave3 + 1.4f) / 2.8f

    // Leicht quadratisch verzerren fuer "Atemschwere" — Inhale schneller als Exhale
    return raw.pow(0.7f).coerceIn(0f, 1f)
}
```

**Warum `pow(0.7f)`:** Normales sin waechst symmetrisch. Die Potenz-Verzerrung
macht den Anstieg schneller als das Abfallen — genau wie echtes Einatmen
(schnell) vs. Ausatmen (langsam). Das ist der Unterschied zwischen "LED blinkt"
und "etwas lebt".

---

## 3 Farbpaletten-Empfehlungen

### Palette 1 — Tesla-Cyan (klassisch elektrisch)

```
Kern:           Color(1f, 1f, 1f, 1f)              // reines Weiss
Kern-Rand:      Color(0.85f, 0.97f, 1f, 0.95f)    // fast weiss, minimaler Blau-Stich
Innerer Halo:   Color(0f, 0.737f, 0.831f, 0.65f)  // #00BBC4 — Cyan
Mittlerer Halo: Color(0f, 0.459f, 0.702f, 0.35f)  // #0175B3 — Tiefblau
Aussen:         Color(0.012f, 0.255f, 0.506f, 0.10f) // #033081 — Nachtblau
```

### Palette 2 — Plasma-Violett (Science-Fiction)

```
Kern:           Color(1f, 1f, 1f, 1f)
Kern-Rand:      Color(0.95f, 0.90f, 1f, 0.90f)   // weiss mit Violett-Touch
Innerer Halo:   Color(0.698f, 0.133f, 1f, 0.60f) // #B222FF — Violett
Mittlerer Halo: Color(0.388f, 0.0f, 0.706f, 0.30f) // #6200B4
Aussen:         Color(0.15f, 0f, 0.30f, 0.08f)    // tiefes Lila
```

### Palette 3 — Sonnenkrone (warm, gefaehrlich)

```
Kern:           Color(1f, 1f, 1f, 1f)
Kern-Rand:      Color(1f, 0.97f, 0.80f, 0.95f)   // warm-weiss
Innerer Halo:   Color(1f, 0.596f, 0.0f, 0.65f)   // #FF9800 — Orange
Mittlerer Halo: Color(0.898f, 0.224f, 0.208f, 0.35f) // #E53935 — Rot
Aussen:         Color(0.40f, 0.05f, 0.05f, 0.09f) // dunkles Dunkelrot
```

---

## 5. Das SDF-Prinzip fuer noch saubereren Glow

Aus der Shadertoy/Shader-Welt: Die ueberzeugendste Glow-Formel in einem einzigen Pass:

```
intensity = clamp(constant / distance, 0, 1) * multiplier
tone_mapped = 1.0 - exp(-intensity)
```

In Compose ohne echten Fragment Shader kann man das annaehern mit
**RadialGradient** dessen Farbstopps diesen Verlauf simulieren:

```kotlin
val glowBrush = Brush.radialGradient(
    0.00f to Color.White,                         // Kern: weiss
    0.05f to glowColor.copy(alpha = 0.95f),       // sofort in Farbe
    0.20f to glowColor.copy(alpha = 0.60f),       // Halo
    0.50f to glowColor.copy(alpha = 0.20f),       // Bloom
    0.80f to glowColor.copy(alpha = 0.05f),       // Atmosphere
    1.00f to Color.Transparent,
    center = center,
    radius = glowRadius,
    tileMode = TileMode.Clamp
)
```

Das Verhaeltnis 0.0→0.05→0.20→0.50→0.80 entspricht einer `1/x`-Kurve,
diskretisiert in 5 Stopps.

---

## Was After Effects "Saber" und Photoshop "Outer Glow" geheim halten

**Saber:** Das Plugin erzeugt den Effekt durch drei aufeinander gestapelte Ebenen:
Core (solid, schmal), Glow (breit, Screen-BlendMode), Distortion (rauscht den Rand
auf). Der entscheidende Parameter ist **Glow Bias** — er verschiebt, wo die Farbe
von Weiss in die Zielfarbe uebergeht. Ein hoher Bias = langer weisser Kern, kurzer
Farbhalo (wirkt heisser). Niedriger Bias = sofort Farbe (wirkt kaelter, mehr wie
Neon als Elektrizitaet).

**Photoshop Outer Glow:** Nutzt morphologische Dilation (Spread-Parameter) +
anschliessend Box-Blur. Der Spread von 0% = reiner Blur-Abfall. Spread 30%+ =
zuerst harte Grenze, dann Blur — das erzeugt das typische "die Farbe klebt am
Rand"-Gefuehl echter heisser Strahlung.

**Ableitung fuer Compose:** Den `spread`-Effekt erreicht man durch die Reihenfolge
der Layer. Schicht 3 (innerer Halo) mit nur `BlurMaskFilter(2f)` und alpha 0.90
erzeugt den haftenden Rand, darueber kommen die grossen Blur-Schichten.

---

## 5 Unentdeckte Tricks

**1. Schicht-Luecke zwischen Kern und erstem Halo:** Zwischen dem soliden Kern
(Schicht 1) und dem ersten Halo (Schicht 3) eine winzige transparente Zone
lassen — strokeWidth auf dem Kern ist 2f, erster Blur beginnt bei strokeWidth 7f.
Dieser 2.5px Spalt erzeugt das optische Knacksen, das echter Elektrizitaet
aehnelt.

**2. Zwei BlurMaskFilter-Typen kombinieren:** `BlurMaskFilter.Blur.NORMAL` fuer
die Kernschichten, `BlurMaskFilter.Blur.OUTER` fuer die Aussenschichten. `OUTER`
blaest den Blur nur nach aussen, nie in die Linie hinein — das erhaelt die
Schaerfe des Kerns bei gleichzeitig weichem Halo.

**3. Tone-Mapping-Approximation durch Alpha-Clamp:** In Compose addieren sich
mehrere halb-transparente Layer nicht zu Weiss, sondern zu einem gedaempften
Wert. Das ist das fehlende Tone-Mapping. Loesung: Kern mit alpha > 0.95 und
`BlendMode.Plus` — dann bricht der Wert an hellen Stellen wirklich zu nahezu
Weiss zusammen.

**4. Perlin-Noise fuer Flackern statt sin():** Statt nur Sinuswellen fuer die
Intensitaet einen einfachen linearen Pseudo-Random-Wert mischen:
`intensity = pulse * 0.85f + Random.nextFloat() * 0.15f`. Das 15%-Rauschen
macht den Unterschied zwischen digitalem und analogem Gluehen. Nicht mehr als
15% — sonst wirkt es nervoes statt elektrisch.

**5. Farb-Temperatur-Drift:** Die Halo-Farbe leicht in Richtung Weiss driften
lassen, wenn der Puls seinen Hoehepunkt erreicht (Analogie: heisser = weisser).
`glowColor.lerp(Color.White, pulseValue * 0.2f)`. Dieser 0–20% Weissanteil
synchronisiert Farbe und Intensitaet und fuehlt sich erschreckend physikalisch
korrekt an.

---

## Referenzen

- [LearnOpenGL — Bloom (klassische Multipass-Theorie)](https://learnopengl.com/Advanced-Lighting/Bloom)
- [Glow Shader in Shadertoy (SDF + 1/x Falloff Erklaerung)](https://inspirnathan.com/posts/65-glow-shader-in-shadertoy/)
- [Yuriy Skul — Glowing Shapes in Android Canvas](https://medium.com/@yuriyskul/different-ways-to-create-glowing-shapes-in-android-canvas-8b73010411fe)
- [Yuriy Skul — RadialGradient Halo fuer Kreise in Compose](https://medium.com/@yuriyskul/shadow-halo-effect-with-transparent-outline-borders-for-circular-shapes-in-jetpack-compose-using-c2d03562e17b)
- [Canopas — RenderEffect in Jetpack Compose (API 31+)](https://medium.com/canopas/how-to-use-render-effects-in-jetpack-compose-for-stunning-visuals-01287d7f00db)
- [Android Developers — BlendMode API Reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/BlendMode)
- [GM Shaders Mini — Bloom (Layer-Gewichtung, Box-Blur Passes)](https://mini.gmshaders.com/p/gm-shaders-mini-bloom)
- [Team Dogpit — No Bloom, No Post Processing: Layered Sprite Glow](https://www.patreon.com/posts/no-bloom-no-post-53965598)
- [Shader Toy Glow Tutorial — Python Arcade Docs](https://api.arcade.academy/en/latest/tutorials/shader_toy_glow/index.html)
- [Video Copilot — Saber Plugin (Core + Glow + Distortion Layers)](https://www.videocopilot.net/blog/2016/03/new-plug-in-saber-now-available-100-free/)
- [GitHub — compose-ShadowGlow](https://github.com/StarkDroid/compose-ShadowGlow)
