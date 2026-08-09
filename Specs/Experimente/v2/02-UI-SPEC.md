# UI-Spec — Experimente-SPEC-v1
Stand: 2026-08-09 · Stufe: v2 · Plattform: Android (Kotlin / Jetpack Compose)


Alle Werte sind **deterministisch aus dem Design gemessen**, nicht geschätzt. Sie sind verbindlich.

## 1. Gestalterische Grundhaltung

Warm im Grundton, sachlich im Aufbau. Die Wärme kommt aus den Farben und der Serifen-Überschrift,
die Klarheit aus Typografie, Abständen und Ordnung — nicht aus zusätzlicher Farbe. Die App soll
sich anfühlen wie ein gut gemachtes Notizbuch, das zuhört, nicht wie ein Gerät, das misst: keine
Diagramme, keine Zähler, keine Abzeichen, kein Fortschrittsbalken. Frank benutzt sie morgens und
abends, und sie bittet ihn um Dinge, vor denen er sich womöglich drückt — eine Oberfläche, die
dabei zusätzlich antreibt, arbeitet gegen die Sache. Jede spätere Entscheidung misst sich daran:
*Beruhigt sie, oder drängt sie?*

---

## 2. Erscheinungen (Themes)

### 2.1 Dunkel (Standard) — `21dunkelstandard` (dark)

| Rolle | Wert |
|-------|------|
| `Grund` | `#151210` |
| `Fläche` | `#201B17` |
| `Erhöht` | `#2A231D` |
| `Rand` | `#38302A` |
| `Rand weich` | `#2C251F` |
| `Text` | `#F4EEE7` |
| `Gedämpft` | `#A99C8F` |
| `Blass` | `#6E635A` |
| `Aktion` | `#C4623C` |
| `Aktion gedeckt` | `#3A231A` |
| `Erledigt` | `#6F8F6A` |
| `Erledigt gedeckt` | `#22301F` |
| `Warnung` | `#D8A03C` |

### 2.2 Hell — `22hell` (light)

| Rolle | Wert |
|-------|------|
| `Grund` | `#F8F4EE` |
| `Fläche` | `#FFFFFF` |
| `Erhöht` | `#FFFFFF` |
| `Rand` | `#E6DCD0` |
| `Rand weich` | `#EFE8DF` |
| `Text` | `#1E1915` |
| `Gedämpft` | `#6C6157` |
| `Blass` | `#9C9186` |
| `Aktion` | `#B0522E` |
| `Aktion gedeckt` | `#F6E6DD` |
| `Erledigt` | `#5A7A55` |
| `Erledigt gedeckt` | `#E6EFE3` |
| `Warnung` | `#9A6A12` |

## 3. Typografie

| Rolle | Familie | Größe | Gewicht | Zeilenhöhe | Laufweite | Quelle |
|-------|---------|-------|---------|------------|-----------|--------|
| design.html:font(3) ×61 | Inter, sans-serif | — | — | — | — | `design.html` |
| design.html:font(4) ×14 | Fraunces, serif | — | — | — | — | `design.html` |
| design.html:font(5) ×14 | "JetBrains Mono", monospace | — | — | — | — | `design.html` |
| design.html:font(0) ×3 | system-ui, -apple-system, "Segoe UI", Roboto, sans-serif | — | — | — | — | `design.html` |
| Bildschirmtitel | Fraunces | 28 | 600 | 34 | 0 | `02-UI-SPEC.md` |
| Abschnittstitel | Fraunces | 22 | 600 | 28 | 0 | `02-UI-SPEC.md` |
| Kartentitel | Fraunces | 19 | 600 | 25 | 0 | `02-UI-SPEC.md` |
| Fließtext | Inter | 16 | 400 | 25 | 0 | `02-UI-SPEC.md` |
| Fließtext klein | Inter | 14 | 400 | 21 | 0 | `02-UI-SPEC.md` |
| Knopfbeschriftung | Inter | 16 | 500 | 20 | 0.2 | `02-UI-SPEC.md` |
| Zwischenüberschrift | Inter | 13 | 600 | 17 | 0.6 | `02-UI-SPEC.md` |
| Daten und Zahlen | JetBrains Mono | 13 | 400 | 18 | 0 | `02-UI-SPEC.md` |
| Stufe / Dauer | JetBrains Mono | 12 | 400 | 16 | 0.4 | `02-UI-SPEC.md` |

## 4. Maße und Raster

| Name | px | Original | Quelle |
|------|----|----------|--------|
| --radius-eingabefeld ×5 | 14 | `14px` | `design.html` |
| --radius-karte ×5 | 20 | `20px` | `design.html` |
| --radius-dialog ×5 | 24 | `24px` | `design.html` |
| --radius-vollrund ×4 | 9999 | `9999px` | `design.html` |
| --b07-radius-vollrund | 999 | `999px` | `design.html` |

## 5. Formen und Tiefe

| Name | Radius | Quelle |
|------|--------|--------|
| design.html:radius(5) ×5 | `inherit` | `design.html` |
| design.html:radius(13) ×5 | `20px` | `design.html` |
| design.html:radius(0) ×4 | `999px` | `design.html` |
| Eingabefeld ×2 | `14 dp` | `02-UI-SPEC.md` |
| Reiter, Chip, Stufen-Etikett ×2 | `vollrund` | `02-UI-SPEC.md` |
| design.html:radius(14) ×2 | `24px` | `design.html` |
| Karte | `20 dp` | `02-UI-SPEC.md` |
| Dialog | `24 dp` | `02-UI-SPEC.md` |
| design.html:radius(4) | `50%` | `design.html` |
| design.html:radius(7) | `20px 20px 6px 20px` | `design.html` |
| design.html:radius(8) | `20px 20px 20px 6px` | `design.html` |
| design.html:radius(11) | `14px` | `design.html` |
| design.html:radius(12) | `9999px` | `design.html` |
| design.html:radius(15) | `0` | `design.html` |


| Effekt | Art | CSS | Quelle |
|--------|-----|-----|--------|
| design.html:shadow(0) ×2 | shadow | `0 6px 24px rgba(0, 0, 0, 0.28)` | `design.html` |
| design.html:gradient(7) ×2 | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--Aktion-gedeckt) 88%, var(--Text) 6%), var(--Aktion-gedeckt))` | `design.html` |
| design.html:shadow(2) | shadow | `0 0 20px color-mix(in srgb, currentColor 18%, transparent), inset 0 1px 0 color-mix(in srgb, currentColor 18%, transparent)` | `design.html` |
| design.html:gradient(3) | gradient | `background-image: linear-gradient(
        to right,
        var(--rand) 0 1px,
        transparent 1px calc(100% / 12)
      ),
      repeating-linear-gradient(
        to right,
        var(--gedaempft) 0 1px,
        transparent 1px calc(100% / 6)
      )` | `design.html` |
| design.html:gradient(4) | gradient | `background-image: radial-gradient(circle at 92% 8%, color-mix(in srgb, var(--aktion) 18%, transparent) 0, transparent 32%),
    radial-gradient(circle at 0% 82%, color-mix(in srgb, var(--erledigt) 12%, transparent) 0, transparent 28%),
    var(--grund)` | `design.html` |
| design.html:gradient(5) | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--aktion), var(--text) 10%), var(--aktion) 58%, color-mix(in srgb, var(--aktion), #000000 16%))` | `design.html` |
| design.html:gradient(6) | gradient | `background-image: radial-gradient(circle at 92% 8%, color-mix(in srgb, var(--Aktion) 18%, transparent) 0, transparent 32%),
    radial-gradient(circle at 0% 82%, color-mix(in srgb, var(--Erledigt) 12%, transparent) 0, transparent 28%),
    var(--Grund)` | `design.html` |
| design.html:gradient(9) | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--Aktion), var(--Text) 10%), var(--Aktion) 58%, color-mix(in srgb, var(--Aktion), #000000 16%))` | `design.html` |

## 6. Bildschirme

| Kennung | Bildschirm | Start | führt zu | Dateien je Erscheinung |
|---------|------------|-------|----------|------------------------|
| B-01 | Heute (`B-01`) | ja | B-02, B-03, B-08 | `bildschirme/21dunkelstandard/…-heute.html`<br>`bildschirme/22hell/…-heute.html` |
| B-02 | Gespräch (`B-02`) | — | B-01 | `bildschirme/21dunkelstandard/…-gespr-ch.html`<br>`bildschirme/22hell/…-gespr-ch.html` |
| B-03 | Auswertung (`B-03`) | — | B-01 | `bildschirme/21dunkelstandard/…-auswertung.html`<br>`bildschirme/22hell/…-auswertung.html` |
| B-08 | Einstellungen (`B-08`) | — | B-09 | `bildschirme/21dunkelstandard/…-einstellungen.html`<br>`bildschirme/22hell/…-einstellungen.html` |
| B-09 | Selbstbild (`B-09`) | — | B-08 | `bildschirme/21dunkelstandard/…-selbstbild.html`<br>`bildschirme/22hell/…-selbstbild.html` |
| B-06 | Erkenntnisse (`B-06`) | — | — | `bildschirme/21dunkelstandard/…-erkenntnisse.html`<br>`bildschirme/22hell/…-erkenntnisse.html` |
| B-07 | Logbuch (`B-07`) | — | — | `bildschirme/21dunkelstandard/…-logbuch.html`<br>`bildschirme/22hell/…-logbuch.html` |
| B-05 | Merkliste (`B-05`) | — | — | `bildschirme/21dunkelstandard/…-merkliste.html`<br>`bildschirme/22hell/…-merkliste.html` |
| B-04 | Wünsche &amp; Ziele (`B-04`) | — | — | `bildschirme/21dunkelstandard/…-w-nsche-amp-ziele.html`<br>`bildschirme/22hell/…-w-nsche-amp-ziele.html` |

> **Achtung:** 1 gemessene Bildschirme wurden im Design nicht aufgebaut und fehlen hier: Experimente-SPEC-v1.

### B-01 — Heute

Startbildschirm: ja · Quelle: `B-01`

**Aufbau von oben nach unten**

- `<section.werft-b01>`
  - `<header.werft-b01__topbar>`
  - `<main.werft-b01__content>`
  - `<nav.werft-b01__bottomnav>`
  - `<script>` — „(() => { const root = document.currentScr“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Hellmodus einschalten | button | F-26 schaltet nacheinander auf Hellmodus, Dunkelmodus und Automatik um, zeigt dafür Sonne, Mondsichel oder A mit seitlichen Strahlen und lässt Automatik sofort sowie bei späteren Systemwechseln der Systemdarstellung folgen |
| 2 | Einstellungen | a | führt zu `B-08` |
| 3 | Lage einsprechen | button | löst `F-01` aus |
| 4 | Lieber tippen | button | Lage tippen |
| 5 | Heutige Lage | textarea | **ohne Ziel und ohne Aufgabe — beim Rückimport klären** |
| 6 | Text mit KI verbessern | button | löst `F-02` aus |
| 7 | Weiter | button | löst `F-03` aus |
| 8 | Auf die Merkliste legen | article | löst `F-06` aus |
| 9 | Auf die Merkliste legen | button | löst `F-05` aus |
| 10 | Von der Merkliste nehmen | article | löst `F-06` aus |
| 11 | Von der Merkliste nehmen | button | löst `F-05` aus |
| 12 | Andere Vorschläge | button | löst `F-04` aus |
| 13 | Gespräch zum Experiment | a | führt zu `B-02` |
| 14 | Nicht umgesetzt | button | löst `F-13` aus |
| 15 | Eine Aufgabe auswählen, die heute wirklich fertig werden soll. | button | löst `F-08` aus |
| 16 | Alles andere beiseitelegen und den ersten Schritt machen. | button | löst `F-08` aus |
| 17 | Wie ist es gelaufen? | a | führt zu `B-03` |
| 18 | Heute | a | führt zu `B-01` |
| 19 | Ziele | a | führt zu `B-04` |
| 20 | Merkliste | a | führt zu `B-05` |
| 21 | Erkenntnisse | a | führt zu `B-06` |
| 22 | Logbuch | a | führt zu `B-07` |

**Bewegungen auf diesem Bildschirm**

- `M-10` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-02` M-02 — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-11` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-12` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-04` M-04 — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-13` Übergang transform — 120 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-07` M-07 — 180 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-14` Übergang transform — 120 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-15` Übergang color — 120 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-16` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-41` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-42` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-52` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-53` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-54` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-55` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-56` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-57` Übergang opacity — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-58` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-02 — Gespräch

Startbildschirm: nein · Quelle: `B-02`

**Aufbau von oben nach unten**

- `<section.b02-screen>`
  - `<header.b02-topbar>`
  - `<main.b02-main>`
  - `<form.b02-composer>`
- `<script>` — „(() => { const screen = document.currentS“
  - `<span.b02-loading-dots>`

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Zurück zu Heute | button | führt zu `B-01` |
| 2 | Nachricht eingeben | input | löst `F-09` aus |
| 3 | Gespräch aufnehmen | button | löst `F-09` aus |

**Bewegungen auf diesem Bildschirm**

- `M-17` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-18` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-02` m-02-atmen — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-03 — Auswertung

Startbildschirm: nein · Quelle: `B-03`

**Aufbau von oben nach unten**

- `<section.werft-b03>`
  - `<header.werft-b03__topbar>`
  - `<main.werft-b03__main>`
- `<script>` — „(() => { const root = document.getElement“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Zurück | button | führt zu `B-01` |
| 2 | Auswertung einsprechen | button | löst `F-10` aus |
| 3 | Auswertungstext | textarea | Auswertungstext bearbeiten |
| 4 | Text mit KI verbessern | button | löst `F-02` aus |
| 5 | Weiter | button | löst `F-11` aus |
| 6 | Auswertung vorlesen | button | löst `F-12` aus |
| 7 | Nochmal versuchen | button | löst `F-10` aus |
| 8 | Überspringen | button | Zum nächsten Experiment springen |
| 9 | Fertig | button | führt zu `B-01` |

**Bewegungen auf diesem Bildschirm**

- `M-19` Übergang background-color — 120 ms, `ease`, einmal (siehe Motion-Spec)
- `M-02` m-02 — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-20` Übergang background-color — 120 ms, `ease`, einmal (siehe Motion-Spec)
- `M-21` Übergang color — 120 ms, `ease`, einmal (siehe Motion-Spec)
- `M-08` m-08 — 400 ms, `ease`, einmal (siehe Motion-Spec)
- `M-09` m-09 — 1800 ms, `linear`, endlos (siehe Motion-Spec)
- `M-22` Übergang opacity — 140 ms, `ease`, einmal (siehe Motion-Spec)
- `M-23` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-08 — Einstellungen

Startbildschirm: nein · Quelle: `B-08`

**Aufbau von oben nach unten**

- `<section.werft-b08>`
  - `<header.werft-b08__topbar>`
  - `<main.werft-b08__scroll>`
- `<script>` — „(() => { const root = document.getElement“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Modell für Experimente | select | löst `F-22` aus |
| 2 | Effort für Experimente | select | löst `F-22` aus |
| 3 | Modell für das Logbuch | select | löst `F-22` aus |
| 4 | Effort für das Logbuch | select | löst `F-22` aus |
| 5 | Stimmenanbieter | select | löst `F-23` aus |
| 6 | Google-Stimme | select | löst `F-23` aus |
| 7 | Stimme aufnehmen | button | löst `F-23` aus |
| 8 | Microsoft-Edge-Stimme | select | löst `F-23` aus |
| 9 | 1.0 | input | löst `F-23` aus |
| 10 | Probe hören | button | löst `F-23` aus |
| 11 | Anmelden | button | löst `F-24` aus |
| 12 | •••••••••••• | input | löst `F-24` aus |
| 13 | 08:00 | input | löst `F-25` aus |
| 14 | Erinnerung morgens | input | löst `F-25` aus |
| 15 | 20:30 | input | löst `F-25` aus |
| 16 | Erinnerung abends | input | löst `F-25` aus |
| 17 | werft-b08-theme | input | löst `F-26` aus |
| 18 | Selbstbild | button | führt zu `B-09` |

**Bewegungen auf diesem Bildschirm**

- `M-24` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-25` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-26` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-27` Übergang left — 200 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-28` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-29` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-30` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-31` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-09 — Selbstbild

Startbildschirm: nein · Quelle: `B-09`

**Aufbau von oben nach unten**

- `<section.b09-screen>`
  - `<header.b09-topbar>`
  - `<main.b09-content>`
  - `<script>` — „(() => { const screen = document.curr“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Zurück zu Einstellungen | button | führt zu `B-08` |
| 2 | Selbstbild | textarea | löst `F-21` aus |
| 3 | Text mit KI verbessern | button | löst `F-02` aus |
| 4 | Selbstbild einsprechen | button | löst `F-21` aus |

**Bewegungen auf diesem Bildschirm**

- `M-32` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-33` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-02` m-02-atmen — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-06 — Erkenntnisse

Startbildschirm: nein · Quelle: `B-06`

**Aufbau von oben nach unten**

- `<section.b06-screen>`
  - `<header.b06-topbar>`
  - `<main.b06-content>`
  - `<nav.b06-bottom-nav>`

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Heute | button | führt zu `B-01` |
| 2 | Ziele | button | führt zu `B-04` |
| 3 | Merkliste | button | führt zu `B-05` |
| 4 | Erkenntnisse | button | führt zu `B-06` |
| 5 | Logbuch | button | führt zu `B-07` |

**Bewegungen auf diesem Bildschirm**

- `M-34` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-35` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-07 — Logbuch

Startbildschirm: nein · Quelle: `B-07`

**Aufbau von oben nach unten**

- `<section.b07-screen>`
  - `<header.b07-topbar>`
  - `<main.b07-content>`
  - `<nav.b07-bottom-nav>`
- `<script>` — „(() => { const screen = document.getEleme“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Letzte 15 Tage | button | Letzte 15 Tage anzeigen |
| 2 | Langzeit | button | Langzeit anzeigen |
| 3 | Heute | button | führt zu `B-01` |
| 4 | Ziele | button | führt zu `B-04` |
| 5 | Merkliste | button | führt zu `B-05` |
| 6 | Erkenntnisse | button | führt zu `B-06` |
| 7 | Logbuch | button | führt zu `B-07` |

**Bewegungen auf diesem Bildschirm**

- `M-36` Übergang transform — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-37` Übergang color — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-38` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-39` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-40` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-41` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-42` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-05 — Merkliste

Startbildschirm: nein · Quelle: `B-05`

**Aufbau von oben nach unten**

- `<section.b05-screen>`
  - `<header.b05-topbar>`
  - `<main.b05-content>`
  - `<section.b05-create-surface>`
  - `<button.b05-fab>`
  - `<nav.b05-bottomnav>`
  - `<dialog.b05-dialog>`
- `<script>` — „(() => { const screen = document.currentS“
  - `<path>` — „'; return svg; }; const armDelete“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Eigenes Experiment einsprechen | button | löst `F-18` aus |
| 2 | Eigenes Experiment | textarea | löst `F-18` aus |
| 3 | Text mit KI verbessern | button | löst `F-02` aus |
| 4 | Speichern | button | löst `F-18` aus |
| 5 | Eigenes Experiment anlegen | button | löst `F-18` aus |
| 6 | Heute | button | führt zu `B-01` |
| 7 | Ziele | button | führt zu `B-04` |
| 8 | Merkliste | button | führt zu `B-05` |
| 9 | Erkenntnisse | button | führt zu `B-06` |
| 10 | Logbuch | button | führt zu `B-07` |
| 11 | Abbrechen | button | **ohne Ziel und ohne Aufgabe — beim Rückimport klären** |
| 12 | Löschen | button | löst `F-19` aus |

**Bewegungen auf diesem Bildschirm**

- `M-02` m-02-atmen — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-44` Übergang opacity — 100 ms, `linear`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-65` Übergang transform — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-66` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-67` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-68` Übergang box-shadow — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

### B-04 — Wünsche &amp; Ziele

Startbildschirm: nein · Quelle: `B-04`

**Aufbau von oben nach unten**

- `<section.b04-screen>`
  - `<header.b04-topbar>`
  - `<main.b04-content>`
  - `<section.b04-create-layer>`
  - `<button.b04-fab>`
  - `<nav.b04-bottom-nav>`
- `<script>` — „(() => { const screen = document.currentS“

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Ziel einsprechen | button | löst `F-20` aus |
| 2 | Ziel | textarea | löst `F-20` aus |
| 3 | Text mit KI verbessern | button | löst `F-02` aus |
| 4 | Speichern | button | löst `F-20` aus |
| 5 | Ziel anlegen | button | löst `F-20` aus |
| 6 | Heute | button | führt zu `B-01` |
| 7 | Ziele | button | führt zu `B-04` |
| 8 | Merkliste | button | führt zu `B-05` |
| 9 | Erkenntnisse | button | führt zu `B-06` |
| 10 | Logbuch | button | führt zu `B-07` |

**Bewegungen auf diesem Bildschirm**

- `M-41` Übergang opacity — 200 ms, `linear`, einmal (siehe Motion-Spec)
- `M-42` Übergang visibility — 0 ms, `linear`, einmal (siehe Motion-Spec)
- `M-47` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-02` m-02 — 3200 ms, `ease-in-out`, endlos (siehe Motion-Spec)
- `M-48` Übergang opacity — 120 ms, `ease-out`, einmal (siehe Motion-Spec)
- `M-49` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-50` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-51` Übergang color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-59` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-60` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-61` Übergang background-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-62` Übergang border-color — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-63` werft-screen-fade — 200 ms, `cubic-bezier(.4, 0, .6, 1)`, einmal (siehe Motion-Spec)
- `M-64` werft-screen-detail — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal (siehe Motion-Spec)
- `M-69` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-70` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-71` Übergang transform — 120 ms, `cubic-bezier(.3, 0, .8, .15)`, einmal (siehe Motion-Spec)
- `M-72` Übergang color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-73` Übergang background-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-74` Übergang border-color — 200 ms, `ease`, einmal (siehe Motion-Spec)
- `M-75` werft-screen-fade — 120 ms, `ease`, einmal (siehe Motion-Spec)

> **Aus v1 übernommen.** Das Messpaket liefert zu Symbolen und Texten nichts (`assets` und `texte` in `design-tokens.json` sind leer) — hier zählt die Absicht aus v1. Die im Design tatsächlich verwendeten Beschriftungen wurden gegen diese Liste geprüft und stimmen überein.

## 7. Ikonografie und Bilder

> **Aus v1 übernommen.** Das Messpaket liefert zu Symbolen und Texten nichts (`assets` und `texte` in `design-tokens.json` sind leer) — hier zählt die Absicht aus v1. Die im Design tatsächlich verwendeten Beschriftungen wurden gegen diese Liste geprüft und stimmen überein.



- **Satz:** Material Symbols Rounded, **Linie** (nicht gefüllt), Strichstärke 2 dp,
  Gewicht 400, Grad 0.
- **Ausnahme:** aktive Zustände werden gefüllt gezeigt — das Lesezeichen bei einem gemerkten
  Vorschlag, das Kästchen bei einer erledigten Aufgabe, das aktive Feld der unteren Leiste.
- **Verwendete Symbole:** `mic` · `stop` · `keyboard` · `bookmark` / `bookmark_filled` ·
  `refresh` · `chat_bubble` · `volume_up` · `check_box_outline_blank` / `check_box` ·
  `add` · `settings` · `arrow_back` · `edit` · `delete` · `flag` · `history` · `lightbulb` ·
  `today` · `auto_awesome` (für die KI-Auswertung).
- **Keine Bilder, keine Illustrationen, keine Maskottchen.** Leere Zustände tragen einen Satz
  Text und höchstens ein einzelnes gedämpftes Symbol.

---

## 8. Texte

Alle festen Beschriftungen wörtlich. Platzhalter in ⟨spitzen Klammern⟩.

| Ort | Text |
|-----|------|
| B-01 leer | „Wie ist deine Lage heute?" |
| B-01 leer, Untertitel | „Was für ein Tag ist das? Was liegt vor dir?" |
| B-01 Textknopf | „Lieber tippen" |
| Überall | „Text mit KI verbessern" / „Zurücknehmen" |
| B-01 nach der Lage | „Weiter" |
| B-01 Zwischenüberschrift | „FÜNF VORSCHLÄGE FÜR HEUTE" |
| Vorschlagskarte, Etikett | „von deiner Merkliste" |
| B-01 unter den Karten | „Andere Vorschläge" |
| B-01 Zwischenüberschrift | „LÄUFT" / „LÄUFT (⟨n⟩ VON 3)" |
| B-01 Zwischenüberschrift | „HEUTE ZU TUN" |
| B-01 Experimentkarte | „Tag ⟨n⟩ von ⟨m⟩" / „Nicht umgesetzt" |
| B-01 Grenze erreicht | „Drei Experimente laufen. Schließ eines ab, bevor du ein neues beginnst." |
| B-01 abends | „Wie ist es gelaufen?" |
| B-02 leer | „Frag mich, wie du das angehen könntest." |
| B-02 Textfeld | „Oder tippen …" |
| B-03 Titel | „Wie ist es gelaufen?" |
| B-03 Haken-Stand | „⟨n⟩ von ⟨m⟩ erledigt" |
| B-03 | „Überspringen" / „Fertig" |
| B-03 leer | „Heute läuft kein Experiment." |
| B-04 Untertitel | „Was möchtest du erreichen? Die Vorschläge tasten dich Schritt für Schritt heran." |
| B-04 leer | „Noch keine Ziele. Sprich das erste ein." |
| B-05 leer | „Nichts gemerkt. Wenn dir ein Vorschlag gefällt, tipp auf das Lesezeichen." |
| B-06 Untertitel | „Was sich aus deinen Auswertungen ergeben hat." |
| B-06 leer | „Noch nichts. Das wächst mit deinen Auswertungen." |
| B-07 Reiter | „Letzte 15 Tage" · „Langzeit" |
| B-07 leer | „Noch nichts aufgeschrieben." |
| B-08 Hinweis | „Das Logbuch darf ein anderes Modell benutzen als die Experimente." |
| B-08 | „Probe hören" · „Stimme aufnehmen" · „Anmelden" · „Abmelden" |
| B-09 Untertitel | „Alles, was die App dauerhaft über dich wissen soll. Je mehr hier steht, desto genauer treffen die Vorschläge." |
| B-09 Platzhalter | „Wer bist du? Was prägt dich? Was war? Sprich einfach drauflos." |
| Wartezustand | „Ich sehe mir deine letzten Tage an …" |
| Fehler, kein Netz | „Dafür brauche ich Netz." |
| Fehler, Mikrofon | „Ohne Mikrofon kann ich dich nicht hören." |
| Fehler, leer | „Da war nichts zu hören." |
| Fehler, Anmeldung | „Deine Anmeldung ist abgelaufen." |
| Fehler, Kontingent | „Dein Kontingent ist erschöpft." |
| Fehler, Antwort | „Die Antwort war unbrauchbar." |
| Fehler, Verbessern | „Der Text konnte nicht verbessert werden." |
| Fehler, Stimme | „Für diese Stimme fehlt der Schlüssel." |
| Überall | „Nochmal versuchen" |
| Erinnerung morgens | „Wie ist deine Lage heute?" |
| Erinnerung abends | „Wie ist es gelaufen?" |

---

---

## 9. Barrierefreiheit

Die App ist ausschließlich für Frank. Es gelten keine Store-Vorgaben. Festgelegt wurde nur,
was sich aus der Gestaltung ergibt:

- **Mindest-Tippfläche 48 × 48 dp** für jedes bedienbare Element (folgt aus „luftig, große
  Tippflächen").
- **Kontrast:** Text auf Grund erreicht in beiden Erscheinungen mindestens 7:1
  (Dunkel: `#F4EEE7` auf `#151210` · Hell: `#1E1915` auf `#F8F4EE`). Gedämpfter Text
  mindestens 4,5:1.
- **Große Systemschrift** wird übernommen; Karten wachsen mit, Texte werden nie abgeschnitten.
- **Reduzierte Bewegung** wird beachtet — siehe Motion-Spec §8.

---

## 10. Offene Fragen

- Design-Fakten stammen aus dem Spec-Paket (00-PROJEKT.md, 01-FUNKTIONS-SPEC.md, 02-UI-SPEC.md, 03-MOTION-SPEC.md), nicht aus Quellcode: die Software existiert noch nicht.
- Funktionen aus dem Spec — das ausloesende Bedienelement traegt data-werft-funktion mit dieser Kennung: F-01 = Lage einsprechen; F-02 = Text mit KI verbessern; F-03 = Fünf Vorschläge erzeugen; F-04 = Vorschläge aktualisieren; F-05 = Vorschlag auf die Merkliste legen; F-06 = Experiment auswählen und starten; F-07 = To-Do-Liste des Tages; F-08 = Aufgabe abhaken; F-09 = Gespräch zum Experiment; F-10 = Auswertung einsprechen; F-11 = KI-Auswertung erzeugen; F-12 = Auswertung vorlesen; F-13 = Experiment abschließen; F-14 = Logbuch fortschreiben; F-15 = Tagesverdichtung nach 15 Tagen; F-16 = Logbuch-Eintrag ändern oder löschen; F-17 = Erkenntnisse fortschreiben; F-18 = Merkliste: eigenes Experiment anlegen; F-19 = Merkliste: Eintrag löschen; F-20 = Wünsche & Ziele pflegen; F-21 = Selbstbild pflegen; F-22 = Modell und Effort wählen; F-23 = Stimme und Vorlesen einstellen; F-24 = Zugänge einrichten; F-25 = Erinnerungen einstellen; F-26 = Erscheinung umschalten
