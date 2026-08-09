# Von der Messung zum Code — die Übersetzungsvorschrift

Stufe 2 legt unter `Specs/<App>/v2/messung/<erscheinung>/<bildschirm>.json` für **jedes**
Element seine berechneten Werte ab. Dieses Dokument sagt, wie daraus Code wird — Zeile für
Zeile, ohne Auslegung.

> **Grundsatz:** Was in der Messung steht, wird gebaut. Was nicht darin steht, wird nicht
> gebaut. Es gibt keinen dritten Fall. Wo diese Vorschrift eine Lücke hat, wird sie ergänzt
> — nicht das fehlende Stück erfunden.

---

## 1. Aufbau einer Messdatei

```jsonc
{
  "bildschirm": "B-01",
  "elemente": [
    {
      "pfad": "section/header[0]/h1[0]",   // Stellung im Baum, eindeutig
      "tag": "h1",
      "klassen": "werft-b01__title",
      "text": "Heute",
      "funktion": "F-01",                  // data-werft-funktion — welche Funktion hängt dran
      "fuehrtZu": "B-02",                  // data-werft-navigate — wohin es führt
      "beschriftung": "Lage einsprechen",  // aria-label → contentDescription
      "platzhalter": "Oder tippen …",
      "versteckt": true,                   // hidden — ein Zustand, kein Wegfall!
      "kasten": { "x": 20, "y": 16, "breite": 372, "hoehe": 34 },
      "stil": { /* berechnete Werte */ },
      "vorher": { /* ::before */ },
      "nachher": { /* ::after */ }
    }
  ],
  "keyframes": [ { "name": "m-08-antwort", "text": "@keyframes …" } ]
}
```

**`kasten`** ist die Wahrheit über die Anordnung: `x`/`y` relativ zur linken oberen Ecke des
Bildschirms, in px bei Bezugsbreite 412 — also **1:1 dp**. Damit ist jede Frage nach „über
oder neben?", „wie breit?", „wie weit eingerückt?" beantwortet, ohne zu raten.

**`versteckt: true`** heißt **nicht**, dass das Element entfällt. Der Entwurf zeigt damit
einen anderen Zustand desselben Bildschirms (Ladezustand, Fehlerkarte, leerer Zustand). Alle
diese Zustände werden gebaut.

---

## 2. Die Übersetzungstabelle

### Fläche, Rand, Form

| Messung | Compose |
|---------|---------|
| `backgroundColor: "rgb(32, 27, 23)"` | `.background(Color(0xFF201B17))` |
| `backgroundColor: "rgba(32,27,23,0.9)"` | `.background(Color(0xFF201B17).copy(alpha = 0.90f))` |
| `color(srgb 0.12 0.10 0.09 / 0.36)` | Anteile × 255 → `Color(0xFF1F1A17).copy(alpha = 0.36f)` |
| `borderTopWidth: "1px"` + `borderTopColor` | `.border(1.dp, Color(…), form)` |
| `borderTopLeftRadius: "20px"` usw. | `RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)` — die vier Werte in der Reihenfolge oben-links, oben-rechts, unten-rechts, unten-links |
| `borderRadius: "9999px"` / `"50%"` | `CircleShape` bzw. `RoundedCornerShape(percent = 50)` |
| `opacity: "0.48"` | `.alpha(0.48f)` |

**Reihenfolge der Modifier ist bedeutungstragend:** erst Schatten, dann `clip`, dann
`background`, dann `border`, dann `padding`, dann Inhalt. Wer `padding` vor `background`
setzt, färbt die Polsterung mit.

### Verläufe

`backgroundImage: "linear-gradient(145deg, rgb(a), rgb(b) 58%, rgb(c))"`

```kotlin
Brush.linearGradient(
    0.00f to Color(a), 0.58f to Color(b), 1.00f to Color(c),
    start = Offset(0f, 0f), end = Offset(breite, hoehe),   // 145° ≈ von links oben nach rechts unten
)
```

`radial-gradient(circle at 92% 8%, X 0, transparent 32%)` → in `Modifier.drawBehind`:

```kotlin
drawCircle(
    brush = Brush.radialGradient(listOf(X, Color.Transparent),
        center = Offset(size.width * 0.92f, size.height * 0.08f),
        radius = size.minDimension * 0.32f * 2f),
    center = Offset(size.width * 0.92f, size.height * 0.08f),
    radius = size.minDimension * 0.32f * 2f,
)
```

### Schatten

`boxShadow` kann **mehrere** durch Komma getrennte Lagen haben. Jede Lage einzeln übersetzen:

| Lage im CSS | Compose |
|-------------|---------|
| `0 16px 32px rgba(0,0,0,.18)` (Schlagschatten) | `.shadow(elevation = 16.dp, shape = form, clip = false, spotColor = Color.Black.copy(alpha = .45f))` |
| `0 0 24px rgba(Aktion,.07)` (farbiger Schein) | zweiter `.shadow(...)` mit `ambientColor = aktion.copy(alpha = …)` |
| `inset 0 1px 0 rgba(Text,.12)` (Lichtsaum oben) | **kein** Schatten-Modifier — 1 dp Linie innen oben zeichnen (`drawWithContent`) |
| `inset 0 0 12px rgba(…)` (inneres Leuchten) | innerer `Box` mit Rand bzw. radialer Verlauf von innen |

Compose kennt keine `inset`-Schatten. Sie wegzulassen ist **nicht** zulässig — sie tragen die
Plastizität. Immer nachzeichnen.

### Unschärfe hinter einer Fläche

`backdropFilter: "blur(12px) saturate(1.08)"`

```kotlin
Modifier.graphicsLayer {
    renderEffect = RenderEffect.createBlurEffect(12f, 12f, Shader.TileMode.CLAMP).asComposeRenderEffect()
}
```
Ab API 31. Darunter: durchscheinende Fläche ohne Unschärfe **und den Verzicht melden**.

### Schrift

| Messung | Compose |
|---------|---------|
| `fontFamily: "Fraunces, serif"` | `FontFamily` aus `res/font/fraunces_*.ttf` — **eingebettet**, nicht heruntergeladen |
| `fontSize: "22px"` | `22.sp` |
| `lineHeight: "28px"` | `lineHeight = 28.sp` |
| `letterSpacing: "0.6px"` bei 13 px | `letterSpacing = 0.6.sp` |
| `fontWeight: "600"` | `FontWeight.SemiBold` |
| `textTransform: "uppercase"` | `text.uppercase()` — **nicht** vergessen, es ändert das Bild stark |

### Abstände und Anordnung

Aus `kasten` ableiten, nicht aus `margin`/`padding` raten:

- **Innenabstand** = Abstand von der Elementkante zur Kante des ersten Kindes.
- **Abstand zwischen Geschwistern** = `y` des zweiten minus (`y` + `hoehe`) des ersten.
- **Über oder neben?** Gleiches `y` bei verschiedenem `x` → nebeneinander (`Row`).
  Gleiches `x` bei wachsendem `y` → untereinander (`Column`).
- **Volle Breite?** `breite` des Elements ≈ `breite` des Elternteils minus dessen Polsterung.
- **Schwebend?** `position: absolute` mit Abständen zu mehreren Kanten → im `Box` mit
  `align(...)` und `padding`, **nicht** als letztes Kind einer `Column` (das verdrängt).

### Bewegung

`transitionProperty` / `transitionDuration` / `transitionTimingFunction` je Element →
`animate*AsState` mit `tween(dauer, easing = CubicBezierEasing(…))`.
`animationName` verweist auf einen Eintrag in `keyframes` → dessen `text` enthält die Stützstellen.

**Nie** eine gemessene `cubic-bezier` durch `FastOutSlowIn` o. ä. ersetzen.

---

## 3. Der Ablauf je Bildschirm

1. Messdatei lesen. Elemente nach `kasten.y`, dann `kasten.x` sortieren — das ist die
   Lesereihenfolge des Entwurfs.
2. Die Baumstruktur aus `pfad` in Compose-Container übersetzen (`Column`/`Row`/`Box` nach
   `display`, `flexDirection`, `position`).
3. Je Element die Tabelle aus §2 anwenden. **Kein Wert wird überschlagen.**
4. `versteckt: true`-Elemente als Zustände bauen, nicht weglassen.
5. `funktion` und `fuehrtZu` mit dem Funktions-Spec verdrahten.
6. Erst dann der nächste Bildschirm.

## 4. Selbstprüfung, bevor der Bildschirm als fertig gilt

Für jedes Element der Messung eine Zeile:

| Element | in der Messung | im Code | Abweichung |
|---------|----------------|---------|------------|
| `.werft-b01__mic` | 88×88, Verlauf 145°, Schatten 3-lagig, innerer Ring 6 dp | 88×88, Verlauf, Schatten, Ring | — |

Bleibt eine Zeile mit Abweichung: der Bildschirm ist **nicht** fertig.
