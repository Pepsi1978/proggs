# Motion-Spec — Experimente-SPEC-v1
Stand: 2026-08-09 · Stufe: v2 · Plattform: Android (Kotlin / Jetpack Compose)


Jede Bewegung ist aus dem Design gemessen und für **Jetpack Compose** übersetzt.
Die angegebene Kurve ist verbindlich — eine eingebaute Standardkurve an ihrer Stelle gilt als nicht erfüllt.

## 2. Kurven und Dauern

| Kennung | Bewegung | Dauer | Kurve | Wiederholung |
|---------|----------|-------|-------|--------------|
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

## 3. Bewegungen im Einzelnen

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
- **Weitere Fundstellen derselben Bewegung** (beim Rückimport zusammengeführt, Werft hatte 6 Einzeleinträge unter derselben Kennung):
  - `.b02-mic-button[data-recording="true"] .b02-recording-ring` — auf B-02 — Quelle: @keyframes m-02-atmen in bildschirme/design.css
  - `.werft-b03__mic.is-recording::before` — auf B-03 — Quelle: @keyframes m-02 in bildschirme/design.css
  - `.b09-mic-wrap.is-recording .b09-mic-ring` — auf B-09 — Quelle: @keyframes m-02-atmen in bildschirme/design.css
  - `.b05-capture.is-recording .b05-record-ring` — auf B-05 — Quelle: @keyframes m-02-atmen in bildschirme/design.css
  - `.b04-record-state.is-recording .b04-record-ring` — auf B-04 — Quelle: @keyframes m-02 in bildschirme/design.css

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
- **Weitere Fundstellen derselben Bewegung** (beim Rückimport zusammengeführt, Werft hatte 2 Einzeleinträge unter derselben Kennung):
  - `.werft-b03__answer-shell` — auf B-03 — Quelle: @keyframes m-08 in bildschirme/design.css

### M-18 — Übergang opacity

- **Wo:** `.b02-input-shell::after` — auf B-02
- **Auslöser:** wechsel
- **Was sich ändert:** opacity
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease-out` → `cubic-bezier(0, 0, 0.58, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))`

### M-19 — Übergang background-color

- **Wo:** `.werft-b03__mic` — auf B-03
- **Auslöser:** wechsel
- **Was sich ändert:** background-color
- **Dauer / Verzögerung:** 120 ms / 0 ms
- **Kurve:** `ease` → `cubic-bezier(0.25, 0.1, 0.25, 1)`
- **Wiederholung / Richtung:** einmal / normal
- **Quelle:** bildschirme/design.css
- **Jetpack Compose:** `tween(durationMillis = 120, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))`

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


---

### Aus v1 zurückgeholte Bewegungen

> **Aus v1 übernommen — im Design nicht messbar.** Ein statischer HTML-Export kann weder einen Druckzustand, noch Haptik, noch eine zweiphasige Abfolge, noch eine SVG-Pfadanimation abbilden. Diese Bewegungen wurden vom Designer nicht gestrichen; das Medium konnte sie nicht tragen. Beim Rückimport bewusst zurückgeholt.

### M-01 — Karte sinkt beim Drücken ein

- **Quelle:** `Specs/Experimente/v1/03-MOTION-SPEC.md` (nicht im Design messbar)
- **Wo:** Jede Karte auf B-01, B-04, B-05, B-07; jede Zeile der To-Do-Liste.
- **Auslöser:** Finger berührt die Karte (Druck beginnt).
- **Was sich ändert:** Größe 100 % → 98 % (vom Mittelpunkt aus).
- **Dauer / Kurve:** 120 ms `knapp`. Beim Loslassen zurück in 120 ms `knapp`.
- **Verzögerung / Wiederholung:** keine / keine.

### M-03 — Vibration bei Aufnahmebeginn und -ende

- **Quelle:** `Specs/Experimente/v1/03-MOTION-SPEC.md` (nicht im Design messbar)
- **Wo:** Jeder Sprechknopf.
- **Auslöser:** Aufnahme startet; Aufnahme endet.
- **Was sich ändert:** Kurze Vibration (`HapticFeedbackConstants.CONFIRM`).
- **Dauer / Kurve:** systemgegeben.
- **Verzögerung / Wiederholung:** keine / keine.
- **Zweck:** Frank soll ohne Hinsehen wissen, dass der Knopf läuft.

### M-05 — Vorschläge werden ausgetauscht

- **Quelle:** `Specs/Experimente/v1/03-MOTION-SPEC.md` (nicht im Design messbar)
- **Wo:** B-01, beim Druck auf „Andere Vorschläge".
- **Auslöser:** F-04.
- **Was sich ändert:** Zwei Abschnitte nacheinander:
  1. **Hinaus:** alle fünf alten Karten gleichzeitig, Position 0 → 12 dp nach oben,
     Deckkraft 100 % → 0 %. 140 ms `hinaus`.
  2. **Herein:** die neuen fünf wie M-04.
- **Dauer / Kurve:** 140 ms `hinaus`, dann Wartezustand, dann 240 ms `ruhig` mit Staffelung.
- **Verzögerung / Wiederholung:** keine / keine.
- **Zweck:** Man muss sehen, dass wirklich getauscht wurde, nicht nur der Text sich änderte.

### M-06 — Haken zeichnet sich

- **Quelle:** `Specs/Experimente/v1/03-MOTION-SPEC.md` (nicht im Design messbar)
- **Wo:** To-Do-Liste auf B-01.
- **Auslöser:** Tippen auf eine Aufgabe.
- **Was sich ändert:** Der Haken wird als Strich von links nach rechts gezeichnet (Pfadlänge
  0 % → 100 %); gleichzeitig füllt sich das Kästchen in *Erledigt*; direkt danach wechselt
  die Textfarbe der Zeile von *Text* auf *Blass*.
- **Dauer / Kurve:** 180 ms `haken` für Haken und Füllung, danach 120 ms `knapp` für die
  Textfarbe.
- **Verzögerung / Wiederholung:** Textfarbe 60 ms verzögert / keine.
- **Rücknahme:** derselbe Ablauf rückwärts, je 120 ms `knapp`.

## 8. Reduzierte Bewegung

Meldet das System „Bewegung reduzieren“: Dauerbewegung aus, Übergänge auf reines Überblenden,
Dauern halbiert. Diese Regel gilt, solange das Funktions-Spec nichts anderes festlegt.
