# Motion-Spec — Experimente
Stand: 14.08.2026, 11.36 Uhr · Stufe: **v2, ueberarbeitet (Stand der gebauten App)** · Plattform: Android (Kotlin / Jetpack Compose)

> **Neu in v2:** fünf Bewegungen `M-96` bis `M-100`, die beim Bauen der Oberfläche entstanden
> sind. Dazu ist §8 um die **Stufen-Semantik** ergänzt: auf *Gedämpft* stehen Dauerbewegungen
> still und bleiben mit 45 % Deckkraft **sichtbar**, erst auf *Aus* verschwinden sie und jede
> gemessene Dauer wird auf **0 ms** gesetzt.


Jede Bewegung ist aus dem Design gemessen und für **Jetpack Compose** übersetzt.
Die angegebene Kurve ist verbindlich — eine eingebaute Standardkurve an ihrer Stelle gilt als nicht erfüllt.

## 2. Kurven und Dauern

> **Nachgetragen in v2:** `M-01`, `M-03`, `M-05` und `M-06` wurden in v1 an mehreren Stellen
> **genannt**, aber nirgends beschrieben. Sie sind gebaut; hier stehen ihre Werte.

| Kennung | Bewegung | Dauer | Kurve | Wiederholung |
|---------|----------|-------|-------|--------------|
| **M-01 NACHGETRAGEN** | Druck sinkt ein (Federphysik `E-07`) | Feder | `spring(0.55, 380)` | einmal |
| **M-03 NACHGETRAGEN** | Haptik bei Aufnahmebeginn und -ende | 110 ms | Rüttelmuster `0/25/60/25` | einmal |
| **M-05 NACHGETRAGEN** | Vorschlagskarten werden ausgetauscht | 240 ms hinaus, dann 240 ms herein je Karte, 60 ms Versatz | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-06 NACHGETRAGEN** | Der Haken zeichnet sich | 180 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-10 | Übergang opacity | 100 ms | `linear` | einmal |
| M-02 | M-02 | 3200 ms | `ease-in-out` | endlos |
| M-11 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-12 | Übergang background-color | 200 ms | `ease` | einmal |
| M-04 | M-04 | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-13 | Übergang transform | 120 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-07 | M-07 | 180 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-14 | Übergang transform | 120 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-15 | Übergang color | 120 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-16 | Übergang color | 200 ms | `ease` | einmal |
| M-17 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-08 | m-08-antwort | 400 ms | `ease-out` | einmal |
| M-18 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-02 | m-02-atmen | 3200 ms | `ease-in-out` | endlos |
| M-19 | Übergang background-color | 120 ms | `ease` | einmal |
| M-02 | m-02 | 3200 ms | `ease-in-out` | endlos |
| M-20 | Übergang background-color | 120 ms | `ease` | einmal |
| M-21 | Übergang color | 120 ms | `ease` | einmal |
| M-08 | m-08 | 400 ms | `ease` | einmal |
| M-09 | m-09 | 1800 ms | `linear` | endlos |
| M-22 | Übergang opacity | 140 ms | `ease` | einmal |
| M-23 | Übergang opacity | 100 ms | `linear` | einmal |
| M-24 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-25 | Übergang border-color | 200 ms | `ease` | einmal |
| M-26 | Übergang background-color | 200 ms | `ease` | einmal |
| M-27 | Übergang left | 200 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-28 | Übergang background-color | 200 ms | `ease` | einmal |
| M-29 | Übergang color | 200 ms | `ease` | einmal |
| M-30 | Übergang background-color | 200 ms | `ease` | einmal |
| M-31 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-32 | Übergang opacity | 100 ms | `linear` | einmal |
| M-33 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-02 | m-02-atmen | 3200 ms | `ease-in-out` | endlos |
| M-34 | Übergang color | 200 ms | `ease` | einmal |
| M-35 | Übergang opacity | 100 ms | `linear` | einmal |
| M-36 | Übergang transform | 200 ms | `linear` | einmal |
| M-37 | Übergang color | 200 ms | `linear` | einmal |
| M-38 | Übergang opacity | 100 ms | `linear` | einmal |
| M-39 | Übergang opacity | 200 ms | `linear` | einmal |
| M-40 | Übergang visibility | 0 ms | `linear` | einmal |
| M-41 | Übergang opacity | 200 ms | `linear` | einmal |
| M-42 | Übergang visibility | 0 ms | `linear` | einmal |
| M-43 | Übergang transform | 120 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | einmal |
| M-02 | m-02-atmen | 3200 ms | `ease-in-out` | endlos |
| M-44 | Übergang opacity | 100 ms | `linear` | einmal |
| M-45 | Übergang transform | 120 ms | `ease-out` | einmal |
| M-46 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-47 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-02 | m-02 | 3200 ms | `ease-in-out` | endlos |
| M-48 | Übergang opacity | 120 ms | `ease-out` | einmal |
| M-49 | Übergang color | 200 ms | `ease` | einmal |
| M-50 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-51 | Übergang color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-52 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-53 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-54 | Übergang color | 200 ms | `ease` | einmal |
| M-55 | Übergang background-color | 200 ms | `ease` | einmal |
| M-56 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-57 | Übergang opacity | 200 ms | `ease` | einmal |
| M-58 | Übergang transform | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-59 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-60 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-61 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-62 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-63 | werft-screen-fade | 200 ms | `cubic-bezier(.4, 0, .6, 1)` | einmal |
| M-64 | werft-screen-detail | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-65 | Übergang transform | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-66 | Übergang border-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-67 | Übergang background-color | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-68 | Übergang box-shadow | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| M-69 | Übergang border-color | 200 ms | `ease` | einmal |
| M-70 | Übergang background-color | 200 ms | `ease` | einmal |
| M-71 | Übergang transform | 120 ms | `cubic-bezier(.3, 0, .8, .15)` | einmal |
| M-72 | Übergang color | 200 ms | `ease` | einmal |
| M-73 | Übergang background-color | 200 ms | `ease` | einmal |
| M-74 | Übergang border-color | 200 ms | `ease` | einmal |
| M-75 | werft-screen-fade | 120 ms | `ease` | einmal |
| **M-76 NEU** | Lichtgrund wandert | 24000 ms | `cubic-bezier(0.42, 0, 0.58, 1)` | endlos, alternate |
| **M-77 NEU** | Glasleiste verdichtet sich beim Scrollen | 200 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-78 NEU** | Karte kippt zur Neigung des Geräts | Feder | `spring(0.75, 200)` | dauerhaft |
| **M-79 NEU** | Plus-Knopf atmet | 3200 ms | `cubic-bezier(0.42, 0, 0.58, 1)` | endlos, alternate |
| **M-80 NEU** | Anlegefläche fährt herein | 320 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-81 NEU** | Neue Karte fliegt ein und funkelt | 480 ms | `spring(0.6, 320)` | einmal |
| **M-82 NEU** | Übernommener Vorschlag fliegt zum Monitor-Feld | 520 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-83 NEU** | Karte wandert nach „Läuft" | 400 ms | `spring(0.7, 260)` | einmal |
| **M-84 NEU** | Funken beim Start | 1200 ms | `linear` | einmal |
| **M-85 NEU** | Karte hebt sich beim Ziehen ab | 160 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-86 NEU** | Karte klappt auf | 280 ms | `spring(0.8, 300)` | einmal |
| **M-87 NEU** | Fortschrittsring füllt sich | 600 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-88 NEU** | Zahl zählt hoch | 400 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-89 NEU** | Schimmer über dem Skelett | 1400 ms | `linear` | endlos |
| **M-90 NEU** | Lichtsaum wandert um die Laufkarte | 6000 ms | `linear` | endlos |
| **M-91 NEU** | Bildschirmwechsel mit Weichzeichnen | 260 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-92 NEU** | Geteiltes Element beim Wechsel | 300 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-93 NEU** | Lichtblüte beim Abschließen | 1600 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-94 NEU** | Leistenfeld leuchtet auf | 240 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal, alternate |
| **M-95** | Gestaffeltes Erscheinen der Karten | 240 ms je Karte, 60 ms Versatz | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-96 NEU** | Klapp-Pfeil dreht sich | 280 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-97 NEU** | Dreieck des Auswahlfelds dreht sich | 120 ms | `cubic-bezier(.2, 0, 0, 1)` | einmal |
| **M-98 NEU** | Denkpunkte im Gespräch | 700 ms, Versatz 0 / 200 / 400 ms | `linear` | endlos, alternate |
| **M-99 NEU** | Balken der Aufnahme-Wellenform | 320 + (i·97) mod 420 ms, Versatz (i·63) mod 380 ms | `cubic-bezier(0.42, 0, 0.58, 1)` | endlos, alternate |
| **M-100 NEU** | Pulsringe am Sprechknopf | 1800 ms, Versatz 0 / 400 / 800 ms | `LinearOutSlowIn` | endlos |

## 3. Bewegungen im Einzelnen

### M-01 — Druck sinkt ein (Federphysik)

- **Wo:** Jedes bedienbare Element (`E-07`) — Knöpfe, Karten, Leistenfelder, Rundknöpfe,
  Etiketten, Klappkarten. Also überall.
- **Auslöser:** Gedrückthalten
- **Was sich ändert:** `scale` 1 → 0,96 und beim Loslassen federnd zurück
- **Dauer / Verzögerung:** Feder, keine feste Dauer / 0 ms
- **Kurve:** `spring(dampingRatio = 0.55, stiffness = 380)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** Entwurf, `transform: scale(.96)` auf `:active`; die Feder aus UI-Spec §7.2 `E-07`
- **Jetpack Compose:** `animateFloatAsState(spring(dampingRatio = 0.55f, stiffness = 380f))` auf
  `Modifier.scale`, gespeist aus `interactionSource.collectIsPressedAsState()`
- **Rückfallebene:** auf der Stufe *Aus* `tween(120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`
  — dieselbe Kurve wie `M-11`

### M-03 — Haptik bei Aufnahmebeginn und -ende

- **Wo:** Jeder Sprechknopf (`F-01`, `F-09`, `F-10`, `F-18`, `F-20`, `F-21`, `F-53`)
- **Auslöser:** Start und Ende einer Aufnahme
- **Was sich ändert:** nichts Sichtbares — ein **doppelter** kurzer Stoß
- **Dauer / Verzögerung:** Muster `0 / 25 / 60 / 25` ms (Gesamtlänge 110 ms) / 0 ms
- **Kurve:** entfällt — Rüttelmuster, keine Interpolation
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** UI-Spec §7.3 `E-23`
- **Jetpack Compose:** `VibrationEffect.createWaveform(longArrayOf(0, 25, 60, 25), -1)`
- **Rückfallebene:** auf der Stufe *Aus* schweigt das Gerät. **Ein Fehlschlag beim Rütteln darf
  nie etwas mitreißen** — er wird vermerkt, und es geht weiter.

### M-05 — Vorschlagskarten werden ausgetauscht

- **Wo:** `B-01`, Zustand `VORSCHLAEGE`, beim Druck auf „Andere Vorschläge" (`F-04`)
- **Auslöser:** `F-04`
- **Was sich ändert:** Die alten fünf Karten gehen **zuerst** hinaus (Deckkraft 1 → 0,
  `translateY` 0 → 10 dp), dann kommen die neuen gestaffelt herein — dieselbe Bewegung wie
  `M-04` / `M-95`, nur mit dem Hinausgehen davor.
- **Dauer / Verzögerung:** 240 ms hinaus, danach 240 ms je Karte mit 60 ms Versatz (bis zur
  fünften)
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** Funktions-Spec `F-04` Schritt 3
- **Jetpack Compose:** `AnimatedContent` über die Liste, `fadeOut + slideOutVertically` gefolgt
  von index-abhängigem `delayMillis`
- **Anmerkung:** Schlägt `F-04` fehl, **bleiben die alten fünf stehen** — dann läuft diese
  Bewegung gar nicht erst an.

### M-06 — Der Haken zeichnet sich

- **Wo:** Jede Aufgabenzeile der To-Do-Liste (`F-08`) — auf `B-10` und auf der Abendkarte von
  `B-01`
- **Auslöser:** Antippen einer Aufgabe
- **Was sich ändert:** Das Kästchen wechselt von leer auf voll, der Haken zeichnet sich entlang
  seines Pfades, ein kurzer Lichtblitz läuft über die Zeile, die Zeile dämpft sich (*Text* →
  *Blass*), das Kästchen färbt sich in *Erledigt*
- **Dauer / Verzögerung:** 180 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** UI-Spec §7.3 `E-17`
- **Jetpack Compose:** `PathMeasure` auf dem Haken-Pfad plus `linearGradient`-Blitz; dazu das
  Haptik-Muster `0 / 10` ms
- **Anmerkung:** **Die Form ändert sich mit, nicht nur die Farbe** — ein leeres Kästchen wird
  ein volles. Rückfallebene: nur der gezeichnete Haken, ohne Blitz.

### M-10 — Übergang opacity

- **Wo:** `.werft-b01__icon-action::after,
.werft-b01__text-action::after,
.werft-b01__primary-action::after,
.werft-b01__nav-item::after,
.werft-b01__todo::after,
.werft-b01__proposal::after,
.werft-b01__mic::after` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-02 — M-02

- **Wo:** `.werft-b01__mic-wrap.is-recording .werft-b01__recording-ring` — auf B-01
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes M-02 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-11 — Übergang transform

- **Wo:** `.werft-b01__mic` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-12 — Übergang background-color

- **Wo:** `.werft-b01__mic` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-04 — M-04

- **Wo:** `.werft-b01__proposal` — auf B-01
- **Auslöser:** erscheinen
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes M-04 in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-13 — Übergang transform

- **Wo:** `.werft-b01__proposal` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-07 — M-07

- **Wo:** `.werft-b01__bookmark.is-saved` — auf B-01
- **Auslöser:** erscheinen
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 180 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes M-07 in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 180, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-14 — Übergang transform

- **Wo:** `.werft-b01__todo` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-15 — Übergang color

- **Wo:** `.werft-b01__todo span` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 120 ms / 60 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, delayMillis = 60, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-16 — Übergang color

- **Wo:** `.werft-b01__nav-item` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-17 — Übergang opacity

- **Wo:** `.b02-back::after,
  .b02-mic-button::after` — auf B-02
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-08 — m-08-antwort

- **Wo:** `.b02-bubble-ai.b02-appearing`
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity, clip-path
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes m-08-antwort in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 400, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-18 — Übergang opacity

- **Wo:** `.b02-input-shell::after` — auf B-02
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-02 — m-02-atmen

- **Wo:** `.b02-mic-button[data-recording="true"] .b02-recording-ring` — auf B-02
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02-atmen in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-19 — Übergang background-color

- **Wo:** `.werft-b03__mic` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-02 — m-02

- **Wo:** `.werft-b03__mic.is-recording::before` — auf B-03
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-20 — Übergang background-color

- **Wo:** `.werft-b03__button` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-21 — Übergang color

- **Wo:** `.werft-b03__button` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-08 — m-08

- **Wo:** `.werft-b03__answer-shell` — auf B-03
- **Auslöser:** erscheinen
- **Was sich ändert:** grid-template-rows, opacity
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes m-08 in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 400, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-09 — m-09

- **Wo:** `.werft-b03__waiting-strip` — auf B-03
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 1800 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** endlos / normal
- **Quelle:** @keyframes m-09 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 1800, easing = LinearEasing), repeatMode = RepeatMode.Restart)`

### M-22 — Übergang opacity

- **Wo:** `.werft-b03__loading-card.is-ending` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 140 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 140, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-23 — Übergang opacity

- **Wo:** `.werft-b03__state-layer::after` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-24 — Übergang opacity

- **Wo:** `.werft-b08__control-shell::after,
  .werft-b08__action::after,
  .werft-b08__nav-row::after,
  .werft-b08__choice::after` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-25 — Übergang border-color

- **Wo:** `.werft-b08__switch-track` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-26 — Übergang background-color

- **Wo:** `.werft-b08__switch-track` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-27 — Übergang left

- **Wo:** `.werft-b08__switch-track::after` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** left
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-28 — Übergang background-color

- **Wo:** `.werft-b08__switch-track::after` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-29 — Übergang color

- **Wo:** `.werft-b08__choice span` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-30 — Übergang background-color

- **Wo:** `.werft-b08__choice span` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-31 — Übergang transform

- **Wo:** `.werft-b08__choice span` — auf B-08
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-32 — Übergang opacity

- **Wo:** `.b09-back::after,
  .b09-improve::after,
  .b09-mic-button::after` — auf B-09
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-33 — Übergang opacity

- **Wo:** `.b09-improve:active` — auf B-09
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-02 — m-02-atmen

- **Wo:** `.b09-mic-wrap.is-recording .b09-mic-ring` — auf B-09
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02-atmen in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-34 — Übergang color

- **Wo:** `.b06-nav-item` — auf B-06
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-35 — Übergang opacity

- **Wo:** `.b06-nav-item::after` — auf B-06
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-36 — Übergang transform

- **Wo:** `.b07-tab-indicator` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-37 — Übergang color

- **Wo:** `.b07-tab` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-38 — Übergang opacity

- **Wo:** `.b07-tab::after,
  .b07-nav-item::after` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-39 — Übergang opacity

- **Wo:** `.b07-tab-panel` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-40 — Übergang visibility

- **Wo:** `.b07-tab-panel` — auf B-07
- **Auslöser:** wechsel
- **Was sich ändert:** visibility
- **Dauer / Verzögerung:** 0 ms / 200 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 0, delayMillis = 200, easing = LinearEasing)`

### M-41 — Übergang opacity

- **Wo:** `.b07-tab-panel.is-active` — auf B-01, B-07, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = LinearEasing)`

### M-42 — Übergang visibility

- **Wo:** `.b07-tab-panel.is-active` — auf B-01, B-07, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** visibility
- **Dauer / Verzögerung:** 0 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 0, easing = LinearEasing)`

### M-43 — Übergang transform

- **Wo:** `.b05-card`
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(0.4, 0, 0.2, 1)` → `cubic-bezier(0.4, 0, 0.2, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))`

### M-02 — m-02-atmen

- **Wo:** `.b05-capture.is-recording .b05-record-ring` — auf B-05
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02-atmen in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-44 — Übergang opacity

- **Wo:** `.b05-hit::after,
  .b05-card::after` — auf B-05
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 100 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 100, easing = LinearEasing)`

### M-45 — Übergang transform

- **Wo:** `.b04-goal-card`
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-46 — Übergang opacity

- **Wo:** `.b04-goal-card::after`
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-47 — Übergang opacity

- **Wo:** `.b04-fab::after,
  .b04-mic::after,
  .b04-action-button::after,
  .b04-nav-item::after` — auf B-04
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-02 — m-02

- **Wo:** `.b04-record-state.is-recording .b04-record-ring` — auf B-04
- **Auslöser:** dauerhaft
- **Was sich ändert:** transform, opacity
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** @keyframes m-02 in bildschirme/design.css
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-48 — Übergang opacity

- **Wo:** `.b04-text-button` — auf B-04
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-49 — Übergang color

- **Wo:** `.b04-nav-item` — auf B-04
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-50 — Übergang background-color

- **Wo:** `.werft-b01,
.b02-screen,
.werft-b03,
.werft-b08,
.b09-screen,
.b06-screen,
.b07-screen,
.b05-screen,
.b04-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-51 — Übergang color

- **Wo:** `.werft-b01,
.b02-screen,
.werft-b03,
.werft-b08,
.b09-screen,
.b06-screen,
.b07-screen,
.b05-screen,
.b04-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-52 — Übergang border-color

- **Wo:** `.werft-b01__theme-control` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-53 — Übergang background-color

- **Wo:** `.werft-b01__theme-control` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-54 — Übergang color

- **Wo:** `.werft-b01__theme-button` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-55 — Übergang background-color

- **Wo:** `.werft-b01__theme-button` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-56 — Übergang transform

- **Wo:** `.werft-b01__theme-button` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-57 — Übergang opacity

- **Wo:** `.werft-b01__theme-button svg` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-58 — Übergang transform

- **Wo:** `.werft-b01__theme-button svg` — auf B-01
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-59 — Übergang background-color

- **Wo:** `.werft-b01__topbar,
.b02-topbar,
.werft-b03__topbar,
.werft-b08__topbar,
.b09-topbar,
.b06-topbar,
.b07-topbar,
.b05-topbar,
.b04-topbar` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-60 — Übergang border-color

- **Wo:** `.werft-b01__topbar,
.b02-topbar,
.werft-b03__topbar,
.werft-b08__topbar,
.b09-topbar,
.b06-topbar,
.b07-topbar,
.b05-topbar,
.b04-topbar` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-61 — Übergang background-color

- **Wo:** `.werft-screen[data-screen-id="B-01"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-04"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-05"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-06"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-07"] nav.werft-primary-nav` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-62 — Übergang border-color

- **Wo:** `.werft-screen[data-screen-id="B-01"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-04"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-05"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-06"] nav.werft-primary-nav,
.werft-screen[data-screen-id="B-07"] nav.werft-primary-nav` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-63 — werft-screen-fade

- **Wo:** `.werft-screen[data-screen-id="B-01"][data-active="true"] .werft-b01,
.werft-screen[data-screen-id="B-04"][data-active="true"] .b04-screen,
.werft-screen[data-screen-id="B-05"][data-active="true"] .b05-screen,
.werft-screen[data-screen-id="B-06"][data-active="true"] .b06-screen,
.werft-screen[data-screen-id="B-07"][data-active="true"] .b07-screen,
.werft-screen[data-screen-id="B-08"][data-active="true"] .werft-b08` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `cubic-bezier(.4, 0, .6, 1)` → `cubic-bezier(0.4, 0, 0.6, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes werft-screen-fade in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f))`

### M-64 — werft-screen-detail

- **Wo:** `.werft-screen[data-screen-id="B-02"][data-active="true"] .b02-screen,
.werft-screen[data-screen-id="B-03"][data-active="true"] .werft-b03,
.werft-screen[data-screen-id="B-09"][data-active="true"] .b09-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity, transform
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes werft-screen-detail in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-65 — Übergang transform

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-66 — Übergang border-color

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-67 — Übergang background-color

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-68 — Übergang box-shadow

- **Wo:** `.werft-b01__proposal,
.werft-b01__experiment-card,
.werft-b01__todo-list,
.werft-b03__answer-card,
.werft-b03__loading-card,
.werft-b03__error-card,
.b02-bubble,
.werft-b08__reminder-row,
.werft-b08__nav-row,
.b05-card,
.b05-create-surface,
.b04-goal-card` — auf B-01, B-03, B-08, B-05
- **Auslöser:** wechsel
- **Was sich ändert:** box-shadow
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)` → `cubic-bezier(0.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-69 — Übergang border-color

- **Wo:** `.werft-b01__input,
.b02-input-shell,
.werft-b03__textarea,
.werft-b08__control-shell,
.b09-editor,
.b05-input,
.b04-goal-input` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-70 — Übergang background-color

- **Wo:** `.werft-b01__input,
.b02-input-shell,
.werft-b03__textarea,
.werft-b08__control-shell,
.b09-editor,
.b05-input,
.b04-goal-input` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-71 — Übergang transform

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** transform
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.3, 0, .8, .15)` → `cubic-bezier(0.3, 0, 0.8, 0.15)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))`

### M-72 — Übergang color

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-73 — Übergang background-color

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-74 — Übergang border-color

- **Wo:** `.werft-b01__primary-action,
.werft-b01__icon-action,
.werft-b01__theme-button,
.b02-back,
.b02-mic-button,
.werft-b03__button,
.werft-b03__mic,
.werft-b03__speaker,
.werft-b08__action,
.b09-back,
.b09-improve,
.b09-mic-button,
.b05-button,
.b05-fab,
.b05-mic,
.b04-fab,
.b04-mic,
.b04-action-button,
.b04-text-button` — auf B-01, B-02, B-03, B-08, B-09, B-05, B-04
- **Auslöser:** wechsel
- **Was sich ändert:** border-color
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-75 — werft-screen-fade

- **Wo:** `.werft-screen[data-screen-id="B-01"][data-active="true"] .werft-b01,
  .werft-screen[data-screen-id="B-02"][data-active="true"] .b02-screen,
  .werft-screen[data-screen-id="B-03"][data-active="true"] .werft-b03,
  .werft-screen[data-screen-id="B-04"][data-active="true"] .b04-screen,
  .werft-screen[data-screen-id="B-05"][data-active="true"] .b05-screen,
  .werft-screen[data-screen-id="B-06"][data-active="true"] .b06-screen,
  .werft-screen[data-screen-id="B-07"][data-active="true"] .b07-screen,
  .werft-screen[data-screen-id="B-08"][data-active="true"] .werft-b08,
  .werft-screen[data-screen-id="B-09"][data-active="true"] .b09-screen` — auf B-01, B-02, B-03, B-08, B-09, B-06, B-07, B-05, B-04
- **Auslöser:** erscheinen
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** @keyframes werft-screen-fade in bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

### M-76 — Lichtgrund wandert

- **Wo:** Der Lichtgrund `E-01` — auf B-10, B-01 und allen Hauptbildschirmen
- **Auslöser:** dauerhaft
- **Was sich ändert:** Mittelpunkte der beiden Farbkreise (je bis zu 18 % der Breite)
- **Dauer / Verzögerung:** 24000 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** neu in dieser Fassung (Effekt `E-01`)
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 24000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-77 — Glasleiste verdichtet sich beim Scrollen

- **Wo:** Kopfleiste und untere Leiste (`E-03`, `E-09`) — auf B-10, B-01, B-04, B-05, B-06, B-07
- **Auslöser:** Scrollen
- **Was sich ändert:** Deckkraft der Glasfläche 60 % → 92 %, Weichzeichnung 12 → 24, Schatten
- **Dauer / Verzögerung:** 200 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung (Effekt `E-09`)
- **Jetpack Compose:** `tween(durationMillis = 200, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-78 — Karte kippt zur Neigung des Geräts

- **Wo:** Lauf- und Wartekarten (`E-08`) — auf B-10
- **Auslöser:** dauerhaft, gesteuert vom Rotationsvektor des Geräts
- **Was sich ändert:** `rotationX`, `rotationY` (höchstens ±6°), Inhaltsversatz bis 4 dp
- **Dauer / Verzögerung:** Federphysik statt fester Dauer
- **Kurve:** `spring(dampingRatio = 0.75f, stiffness = 200f)`
- **Wiederholung / Richtung:** dauerhaft / folgt dem Sensor
- **Quelle:** neu in dieser Fassung (Effekt `E-08`)
- **Jetpack Compose:** `animateFloatAsState(targetValue = neigung, animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f))`

### M-79 — Plus-Knopf atmet

- **Wo:** Der schwebende Plus-Knopf (F-35) — auf B-10, nur im Zustand `LEER`
- **Auslöser:** dauerhaft
- **Was sich ändert:** Größe 100 % → 106 %, Schein 12 % → 22 %
- **Dauer / Verzögerung:** 3200 ms / 0 ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** neu in dieser Fassung, im Takt von `M-02`
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 3200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), repeatMode = RepeatMode.Reverse)`

### M-80 — Anlegefläche fährt herein

- **Wo:** Die Anlegefläche (F-35) — auf B-10
- **Auslöser:** Druck auf den Plus-Knopf
- **Was sich ändert:** Versatz von unten (100 % → 0), Deckkraft, Weichzeichnung des Grundes 0 → 16
- **Dauer / Verzögerung:** 320 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-81 — Neue Karte fliegt ein und funkelt

- **Wo:** Eine neu angelegte Wartekarte (F-35) — auf B-10
- **Auslöser:** Speichern
- **Was sich ändert:** Größe 88 % → 100 %, Deckkraft 0 → 1, dazu `E-15` mit 12 Punkten
- **Dauer / Verzögerung:** 480 ms / 0 ms
- **Kurve:** `spring(dampingRatio = 0.6f, stiffness = 320f)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `spring(dampingRatio = 0.6f, stiffness = 320f)`

### M-82 — Übernommener Vorschlag fliegt zum Monitor-Feld

- **Wo:** Vorschlagskarte (F-36) — auf B-01
- **Auslöser:** Druck auf „In den Monitor"
- **Was sich ändert:** Position zur unteren Leiste, Größe 100 % → 24 %, Deckkraft 1 → 0
- **Dauer / Verzögerung:** 520 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung (Effekt `E-11`)
- **Jetpack Compose:** `tween(durationMillis = 520, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-83 — Karte wandert von „Steht an" nach „Läuft"

- **Wo:** Die gestartete Karte (F-37) — auf B-10
- **Auslöser:** Druck auf „Starten"
- **Was sich ändert:** Position, Flächenfarbe `Fläche` → `Erhöht`, Lichtsaum `E-06` setzt ein
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `spring(dampingRatio = 0.7f, stiffness = 260f)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `spring(dampingRatio = 0.7f, stiffness = 260f)`

### M-84 — Funken beim Start

- **Wo:** Über der gestarteten Karte (`E-15`) — auf B-10
- **Auslöser:** Druck auf „Starten"
- **Was sich ändert:** 24 Lichtpunkte steigen auf, Deckkraft 1 → 0, Größe 100 % → 40 %
- **Dauer / Verzögerung:** 1200 ms / 0 ms, je Punkt bis 200 ms Versatz
- **Kurve:** `linear`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 1200, easing = LinearEasing)`

### M-85 — Karte hebt sich beim Ziehen ab

- **Wo:** Wartekarte beim Verschieben (F-38) — auf B-10
- **Auslöser:** langer Druck
- **Was sich ändert:** Größe 100 % → 104 %, Schatten 6 → 24, Schein `E-05` setzt ein
- **Dauer / Verzögerung:** 160 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 160, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-86 — Karte klappt auf

- **Wo:** Lauf- und Wartekarten (F-40) — auf B-10
- **Auslöser:** Tippen
- **Was sich ändert:** Höhe, Deckkraft des zusätzlichen Inhalts, Drehung des Pfeils um 180°
- **Dauer / Verzögerung:** 280 ms / 0 ms
- **Kurve:** `spring(dampingRatio = 0.8f, stiffness = 300f)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `spring(dampingRatio = 0.8f, stiffness = 300f)`

### M-87 — Fortschrittsring füllt sich mit Leuchtspur

- **Wo:** Der Ring der heutigen Aufgaben auf einer Laufkarte — auf B-10
- **Auslöser:** Abhaken einer Aufgabe (F-08)
- **Was sich ändert:** Winkel des Rings, ein hellerer Punkt läuft der Kante voraus
- **Dauer / Verzögerung:** 600 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-88 — Zahl zählt hoch

- **Wo:** Die Zeile „Steht an: 4 · Läuft: 2" und alle Anzahlen (`E-20`) — auf B-10
- **Auslöser:** Änderung des Wertes
- **Was sich ändert:** der angezeigte Zahlenwert
- **Dauer / Verzögerung:** 400 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `animateIntAsState(targetValue = wert, animationSpec = tween(durationMillis = 400, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))`

### M-89 — Schimmer über dem Skelett

- **Wo:** Ladeskelette (`E-13`) — auf allen Bildschirmen mit Ladezustand
- **Auslöser:** dauerhaft, solange geladen wird
- **Was sich ändert:** Versatz eines Lichtstreifens von −40 % nach 140 %
- **Dauer / Verzögerung:** 1400 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** endlos / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 1400, easing = LinearEasing), repeatMode = RepeatMode.Restart)`

### M-90 — Lichtsaum wandert um die Laufkarte

- **Wo:** Rand jeder laufenden Karte (`E-06`) — auf B-10
- **Auslöser:** dauerhaft, solange das Experiment läuft
- **Was sich ändert:** Winkel eines `sweepGradient` von 0° auf 360°
- **Dauer / Verzögerung:** 6000 ms / 0 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** endlos / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `infiniteRepeatable(animation = tween(durationMillis = 6000, easing = LinearEasing), repeatMode = RepeatMode.Restart)`

### M-91 — Bildschirmwechsel mit Weichzeichnen und Skalieren

- **Wo:** Jeder Wechsel zwischen Bildschirmen (`E-12`) — alle Bildschirme
- **Auslöser:** Navigation
- **Was sich ändert:** abgehend Größe 100 % → 96 % und Weichzeichnung 0 → 12; ankommend 104 % → 100 % und Deckkraft 0 → 1
- **Dauer / Verzögerung:** 260 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung; ersetzt `M-63`/`M-75` (Werfts Vorschau-Blenden)
- **Jetpack Compose:** `tween(durationMillis = 260, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-92 — Geteiltes Element beim Wechsel

- **Wo:** Titel und Fläche einer Karte, die einen neuen Bildschirm öffnet (`E-11`) — B-10 → B-02, B-10 → B-03, B-01 → B-10
- **Auslöser:** Navigation aus einer Karte heraus
- **Was sich ändert:** Position, Größe und Eckenradius des geteilten Elements
- **Dauer / Verzögerung:** 300 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `SharedTransitionLayout` mit `sharedElement(rememberSharedContentState(key), boundsTransform = { _, _ -> tween(300, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)) })`

### M-93 — Lichtblüte beim Abschließen

- **Wo:** Über der abgeschlossenen Karte (`E-16`) — auf B-10 und B-03
- **Auslöser:** Abschluss eines Experiments (F-13)
- **Was sich ändert:** ein Lichtring dehnt sich von 0 auf 180 % der Kartenbreite, Deckkraft 0,6 → 0; dazu 40 Partikel
- **Dauer / Verzögerung:** 1600 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 1600, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-94 — Leistenfeld leuchtet auf

- **Wo:** Ein Feld der unteren Leiste, wenn sich sein Inhalt ändert — alle Hauptbildschirme
- **Auslöser:** neues Experiment im Monitor (F-36), neue Erkenntnis (F-17)
- **Was sich ändert:** Schein `E-05` 0 % → 30 % → 0 %, Symbolgröße 100 % → 118 % → 100 %
- **Dauer / Verzögerung:** 240 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / alternate
- **Quelle:** neu in dieser Fassung
- **Jetpack Compose:** `tween(durationMillis = 240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-95 — Gestaffeltes Erscheinen der Karten

- **Wo:** Alle Listen (`E-10`) — B-10, B-01, B-04, B-05, B-06, B-07
- **Auslöser:** erscheinen
- **Was sich ändert:** Deckkraft 0 → 1, Versatz von unten 16 dp → 0
- **Dauer / Verzögerung:** 240 ms / 60 ms je Eintrag, höchstens 480 ms Gesamtversatz
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in dieser Fassung; verallgemeinert `M-04`
- **Jetpack Compose:** `tween(durationMillis = 240, delayMillis = (index * 60).coerceAtMost(480), easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))`

### M-96 — Klapp-Pfeil dreht sich

- **Wo:** Der Pfeil rechts an jeder Klappkarte (`E-27`) — auf `B-03` (Verlauf), `B-07`
  (Experimentfächer und Aufnahmen)
- **Auslöser:** Auf- und Zuklappen
- **Was sich ändert:** `rotationZ` 0° → 180°
- **Dauer / Verzögerung:** 280 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in v2 (Effekt `E-27`)
- **Jetpack Compose:** `animateFloatAsState(tween(280, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))` auf `Modifier.rotate`

### M-97 — Dreieck des Auswahlfelds dreht sich

- **Wo:** Jedes Auswahlfeld auf `B-08` (`F-22`, `F-23`)
- **Auslöser:** Aufklappen der Liste
- **Was sich ändert:** `rotationZ` 0° → 180°; zugleich wechselt der Rand des Feldes von *Rand*
  auf *Aktion*
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `cubic-bezier(.2, 0, 0, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** neu in v2 — die Entsprechung von `M-13` am neuen Bauteil
- **Jetpack Compose:** `animateFloatAsState(tween(120, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))`

### M-98 — Denkpunkte im Gespräch

- **Wo:** Der Wartezustand auf `B-02`, in einer KI-Blase
- **Auslöser:** Solange die Antwort aussteht
- **Was sich ändert:** Deckkraft dreier Punkte à 6 dp, je 0,25 → 1
- **Dauer / Verzögerung:** 700 ms / 0, 200, 400 ms
- **Kurve:** `linear`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** Entwurf `@keyframes dot` (`0%, 60%, 100% → opacity .25`, `30% → opacity 1`)
- **Jetpack Compose:** `infiniteRepeatable(tween(700, delayMillis = versatz, easing = LinearEasing), RepeatMode.Reverse)`

### M-99 — Balken der Aufnahme-Wellenform

- **Wo:** `E-18`, unter dem Sprechknopf auf `B-01` während der Aufnahme
- **Auslöser:** Laufende Aufnahme
- **Was sich ändert:** `scaleY` je Balken von 0,22 auf 1, Ursprung unten. **24 Balken**, je
  3 dp breit, 36 dp hoch, 3 dp Abstand, Radius 2 dp, Fläche *Aktion*
- **Dauer / Verzögerung:** je Balken `320 + (i·97) mod 420` ms / `(i·63) mod 380` ms
- **Kurve:** `ease-in-out` → `cubic-bezier(0.42, 0, 0.58, 1)`
- **Wiederholung / Richtung:** endlos / alternate
- **Quelle:** Entwurf, je Balken eigene Dauer und eigener Versatz
- **Jetpack Compose:** `infiniteRepeatable(tween(dauerMs, delayMillis = versatz, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), RepeatMode.Reverse)`
- **Anmerkung:** Der Kranz steht auf *Gedämpft* still (45 % Deckkraft); der atmende Ring
  (`M-02`) trägt den Zustand dann allein.

### M-100 — Pulsringe am Sprechknopf

- **Wo:** `E-19`, drei Ringe um den Sprechknopf (88 dp) während der Aufnahme; das Gehäuse misst
  168 dp, damit der größte Ring hineinpasst
- **Auslöser:** Laufende Aufnahme
- **Was sich ändert:** `scale(1) opacity .5` → `scale(1.9) opacity 0`, Rand 2 dp in *Aktion*
- **Dauer / Verzögerung:** 1800 ms / 0, 400, 800 ms
- **Kurve:** `ease-out` → `LinearOutSlowIn`
- **Wiederholung / Richtung:** endlos / restart
- **Quelle:** Entwurf `@keyframes puls`
- **Jetpack Compose:** `infiniteRepeatable(tween(1800, delayMillis = versatz, easing = LinearOutSlowInEasing), RepeatMode.Restart)`
- **Rückfallebene:** auf *Gedämpft* ein einzelner, stehender Ring mit 45 % Deckkraft — die
  Aufnahme bleibt erkennbar; auf *Aus* gar keiner.

## 8. Reduzierte Bewegung

Meldet das System „Bewegung reduzieren“: Dauerbewegung aus, Übergänge auf reines Überblenden,
Dauern halbiert. Diese Regel gilt, solange das Funktions-Spec nichts anderes festlegt.

**Ergänzung dieser Fassung.** Zusätzlich zur Systemmeldung gibt es die Einstellung
**Effekt-Stärke** (F-41) mit den Stufen *Voll · Gedämpft · Aus*. Beide wirken zusammen:

| Lage | Was gilt |
|------|----------|
| System meldet „Bewegung reduzieren" | mindestens *Gedämpft*, auch wenn *Voll* gewählt ist |
| Energiesparmodus an | mindestens *Gedämpft* |
| *Gedämpft* | `M-76`, `M-78`, `M-79`, `M-84`, `M-89`, `M-90`, `M-93` aus; alle Übergänge bleiben |
| *Aus* | zusätzlich `M-77`, `M-80`, `M-81`, `M-82`, `M-83`, `M-85`, `M-86`, `M-87`, `M-91`, `M-92`, `M-95` auf reines Überblenden mit halbierter Dauer |

**Die Federbewegungen** (`M-78`, `M-81`, `M-83`, `M-86`) werden auf *Aus* durch
`tween(120, LinearEasing)` ersetzt, nicht ersatzlos gestrichen — sonst springt die Oberfläche.

**`M-63` und `M-75` (`werft-screen-fade`) sowie `M-64` (`werft-screen-detail`) gehören zu
Werfts Vorschau und sind nicht zu bauen.** An ihrer Stelle steht `M-91`.

### 8.1 Die drei Stufen genau — NEU in v2

| Stufe | Dauerbewegungen | Partikel | Parallaxe | Federphysik | Weichzeichnen, Verläufe, Schein | Haptik | Gemessene Dauern |
|-------|-----------------|----------|-----------|-------------|--------------------------------|--------|------------------|
| **Voll** | laufen | ja | ja | ja | ja | ja | wie gemessen |
| **Gedämpft** | **stehen still, bleiben mit 45 % Deckkraft sichtbar** | nein | nein (Sensor wird nicht angemeldet) | ja | ja | ja | wie gemessen |
| **Aus** | **ausgeblendet** | nein | nein | nein (`tween(120)` statt Feder) | nein | nein | **0 ms** |

**Das ist der Unterschied zu v1:** Dort hieß es, auf *Gedämpft* seien Dauerbewegungen „aus".
Sie sind es nicht — sie **halten an**. Ein Element, das plötzlich ganz verschwindet, nimmt der
Oberfläche ihre Ordnung; eines, das still steht, bleibt an seinem Platz. Erst auf *Aus* wird es
ausgeblendet, so wie der Entwurf es mit `[data-dauerbewegung]{display:none}` schreibt.

**Neu ist außerdem:** Auf *Aus* wird nicht nur „auf Überblenden umgestellt", sondern **jede
gemessene Dauer auf 0 ms gesetzt** (`*{transition:none}`). Die Federbewegungen (`M-78`, `M-81`,
`M-83`, `M-86`) werden durch `tween(120, LinearEasing)` ersetzt, nicht ersatzlos gestrichen —
sonst springt die Oberfläche.

**Und:** Auf *Aus* schweigt die **Haptik** (`E-23`) vollständig.
