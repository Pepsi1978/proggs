# Aufbau der Claude-Design-Dateien ("Cloud Designs")

Diese Referenz beschreibt, wie ein mit Claude Designs erstellter Entwurf aufgebaut ist
und **wo genau** die Design-Werte stehen, die 1:1 in Jetpack Compose zu uebertragen sind.

Ein Design-Ordner (unter `~/proggs/Designs/<Name>/`) enthaelt typischerweise vier Dateien:

| Datei | Rolle | Fuer die Umsetzung |
|-------|-------|--------------------|
| `<Name>.dc.html` | Kompletter Prototyp (HTML/CSS/Handlebars) | **PRIMAERE Quelle** — vollstaendig lesen |
| `android-frame.jsx` | Material-3-Geraeterahmen (Bezel, Statusbar, AppBar, NavBar, Keyboard) | Referenz fuer M3-Kontext, nicht selbst nachbauen |
| `support.js` | Generierte React-Runtime (`dc-runtime`) | **Ignorieren** — reine Rendering-Maschinerie |
| `.thumbnail` | Vorschaubild | Visueller Gesamteindruck / Endabgleich |

---

## 1. Die `.dc.html` — hier steckt das ganze Design

Grobstruktur:

```html
<!DOCTYPE html>
<html>
<head>
  <script src="./support.js"></script>   <!-- Runtime, ignorieren -->
</head>
<body>
<x-dc>                                    <!-- Wurzel des Design-Components -->
  <helmet>                                <!-- Kopf: Fonts + globales CSS -->
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
      #fb{ --bg0:#f7f6fe; --acc:#6a5cff; ... }   <!-- Basis-Theme (CSS-Variablen) -->
      #fb[data-t$="-dark"]{ --bg0:#0f0d20; ... }  <!-- Dark-Variante -->
      #fb[data-t^="vital"]{ --acc:#0c8f74; ... }  <!-- Zusatz-Theme "vital" -->
      #fb[data-t="vital-dark"]{ ... }             <!-- dessen Dark-Variante -->
      #fb[data-t^="ember"]{ ... }                 <!-- Zusatz-Theme "ember" -->
      @keyframes fbIn{ ... } @keyframes fbFloat{ ... }   <!-- Animationen -->
    </style>
  </helmet>
  <div id="fb" data-t="{{ themeKey }}" data-glow="{{ glowKey }}" style="...">
    ...  <!-- die eigentlichen Screens/Komponenten -->
  </div>
</x-dc>
<script type="application/json" data-dc-script data-props="...">...</script>  <!-- Props/State -->
</body>
</html>
```

### Wo welche Werte stehen

- **Farben & Themes:** In den CSS-Custom-Properties (`--name:wert;`) im `#fb{ … }`-Block
  und in den `#fb[data-t…]{ … }`-Selektoren. Jeder Selektor = **ein Theme**:
  - `#fb{ … }` = Basis-Theme (Light).
  - `[data-t$="-dark"]` = generische Dark-Ueberschreibung (Suffix `-dark`).
  - `[data-t^="vital"]`, `[data-t^="ember"]` = Zusatz-Themes (Praefix-Match).
  - `[data-t="vital-dark"]` = konkrete Kombination.
  - Merke: `^=` = "beginnt mit", `$=` = "endet mit", `=` = exakt. So erkennst du,
    welche Variablen fuer welche Theme-Kombination gelten (Kaskade beachten:
    spezifischere/spaetere Regeln ueberschreiben).
  - Typische Variablen: `--bg0/--bg1` (Hintergrund-Gradient-Stops), `--card`
    (Kartenfarbe, oft mit Alpha), `--cardBrd` (Kartenrand), `--shadow` (Box-Shadow),
    `--text`/`--sub` (Text/Sekundaertext), `--line` (Trennlinien), `--chip`,
    `--acc`/`--acc2` (Akzent/Verlauf), `--onAcc` (Text auf Akzent), `--accSoft`,
    `--glow` (Leuchteffekt), `--ok`/`--warn`/`--bad` (+ `*Bg`), `--navBg` (Bottom-Nav).
  - **Alpha exakt uebernehmen:** `rgba(106,92,255,.13)` → Compose
    `Color(0x216A5CFF)` (Alpha 0.13·255 ≈ 33 = 0x21; danach RGB). Formel:
    `alphaHex = round(a*255)` zweistellig, dann `0x{AA}{RRGGBB}`.

- **Schrift:** Im `<helmet>` per Google-Fonts-`<link>`/`@import` — Familienname
  (z.B. `Outfit`) und die geladenen Gewichte (`wght@300;400;500;600;700;800`).
  Konkrete Groessen/Gewichte/Zeilenhoehen stehen inline in den `style="…"`-Attributen
  der Elemente (`font-size`, `font-weight`, `line-height`, `letter-spacing`).

- **Abstaende/Groessen/Formen:** In den inline-`style`-Attributen der `<div>`/Elemente
  und im globalen `<style>`: `padding`, `margin`, `gap`, `width/height`, `border-radius`,
  `border`, `box-shadow`, `filter:blur()`, `background:linear-/radial-gradient(...)`.

- **Anordnung/Struktur:** Die Verschachtelung der `<div>`s unter `#fb` = die Screen-
  Struktur. `display:flex; flex-direction; align-items; justify-content; gap` geben
  Reihenfolge und Ausrichtung. Karten, Chips, Listen, Bottom-Nav sind hier als
  verschachtelte Container mit den o.g. Variablen gestylt.

- **Animationen:** `@keyframes <name>{ … }` im `<style>`, angewandt via
  `animation:<name> <dauer> <easing> <delay> <iteration> <direction>;` oder
  `transition:` an einzelnen Elementen. 1:1 in Compose nachbauen (`animate*AsState`,
  `AnimatedVisibility`, `updateTransition`, `rememberInfiniteTransition`/`infiniteRepeatable`).

- **Handlebars-Platzhalter `{{ … }}`:** Der Prototyp ist parametrisiert
  (z.B. `data-t="{{ themeKey }}"`, `data-glow="{{ glowKey }}"`). Die moeglichen Werte
  ergeben sich aus den Theme-Selektoren (Basis, `-dark`, `vital`, `ember`, …) bzw. aus
  dem Props-JSON (siehe unten). `{{ #each }}`/`{{ #if }}`-Bloecke zeigen Wiederholungen
  (Listen) und Bedingungen (Zustaende) — im Compose als `LazyColumn`/`if`-Logik abbilden.

- **Props / State:** Das `<script type="application/json" data-dc-script data-props="…">`
  am Ende enthaelt Default-Props/Preview-Daten des Prototyps (z.B. Beispielinhalte,
  aktiver Theme-Key). Nuetzlich, um sinnvolle Default-Zustaende und Beispieldaten zu
  erkennen — aber Design-Werte (Farben/Abstaende) kommen aus dem CSS, nicht hieraus.

---

## 2. `android-frame.jsx` — Material-3-Rahmen als Kontext

Enthaelt fertige React-Komponenten fuer den Android-Geraeterahmen:
`AndroidDevice`, `AndroidStatusBar`, `AndroidAppBar`, `AndroidListItem`,
`AndroidNavBar`, `AndroidKeyboard` — inkl. einer M3-Farbkonstante `MD_C` (surface,
primary, onSurface …) und M3-konformen Massen (Statusbar-Hoehe 40, Paddings 16 …).

Nutzung: **nur als Referenz**, um zu verstehen, in welchem M3-Rahmen der Entwurf
gedacht ist (Statusbar/AppBar/Gesture-Nav). Diesen Rahmen liefert Android selbst —
also **nicht** als eigenes Composable nachbauen, sondern die echten M3-Bausteine
(`Scaffold`, `TopAppBar`, `NavigationBar`) verwenden. Die `MD_C`-Werte sind der
M3-Default und nur relevant, falls der `.dc.html`-Entwurf sie nicht selbst ueberschreibt.

---

## 3. `support.js` — ignorieren

Kopfzeile: `// GENERATED from dc-runtime/src/*.ts — do not edit.` Es ist die
React/DOM-Runtime, die die `.dc.html` im Browser rendert (`parseDcDocument`,
`createElement` …). Sie enthaelt **kein** Design — niemals als Quelle fuer Werte lesen.

---

## 4. `.thumbnail` — Vorschaubild

Bild-Datei (trotz fehlender Endung meist PNG/Bilddaten). Mit dem Read-Tool visuell
ansehen, um den Gesamteindruck zu erfassen und am Ende (Phase 7) das Umsetzungsergebnis
dagegen zu halten.

---

## Kurz-Checkliste fuer die Extraktion

1. `.dc.html` ganz lesen (ggf. mehrere Reads mit `offset`/`limit`).
2. Alle `--variablen` je Theme-Selektor tabellieren (Basis, `-dark`, Zusatz-Themes).
3. Schriftfamilie + Gewichte aus dem `<helmet>` ziehen; Groessen aus inline-styles.
4. Alle `border-radius`, `box-shadow`, `blur`, Gradients, `padding/gap` sammeln.
5. Screen-Struktur (div-Verschachtelung) und Navigation notieren.
6. Alle `@keyframes` + `animation:`/`transition:` erfassen.
7. `.thumbnail` visuell ansehen; `android-frame.jsx` als M3-Kontext quer lesen.
8. `support.js` ueberspringen.
