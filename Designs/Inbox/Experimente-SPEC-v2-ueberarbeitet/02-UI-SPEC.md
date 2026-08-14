# UI-Spec — Experimente
Stand: 14.08.2026, 11.36 Uhr · Stufe: **v2, ueberarbeitet (Stand der gebauten App)** · Plattform: Android (Kotlin / Jetpack Compose)

Alle Werte sind **deterministisch aus dem Design gemessen**, nicht geschätzt. Sie sind verbindlich.

> **Zwei Entwürfe, eine Oberfläche.** Diese Fassung hat **zwei** Quellen, und beide liegen im Paket:
>
> | Quelle | Wo | Was daraus stammt |
> |--------|-----|-------------------|
> | **Werft-Paket** (v1) | `WERFT-DESIGN/` | Farben, Typo-Skala, Radien, Maße und der Aufbau von `B-01` bis `B-09` — alles maschinell gemessen |
> | **Fold-Außendisplay-Entwurf** | `CLAUDE-DESIGN/Experimente Fold-Aussendisplay.dc.html` | **`B-10` (Monitor)**, die Effektschicht `E-01` bis `E-24` im laufenden Bild, die sechsfeldrige untere Leiste und die zwölf zusätzlichen Schriftrollen aus §3 |
>
> `B-10` war in v1 als **„noch nicht gebaut"** ausgeschrieben. Er ist inzwischen im
> Fold-Entwurf aufgebaut **und** in der App umgesetzt; die Werte unten sind aus dem gebauten
> Bildschirm, nicht mehr abgeleitet. Wo beide Quellen dasselbe Bauteil beschreiben, gilt der
> **Fold-Entwurf** — er ist der jüngere.

## 1. Gestalterische Grundhaltung

**Warm im Grundton, modern in der Erscheinung, reich an Bewegung.** Die Farbwelt und die
Serifen-Überschrift bleiben unverändert — sie sind das Gesicht der App. Was sich ändert, ist
die Oberfläche darüber: Sie ist nicht mehr still, sondern lebendig. Glasflächen, ein langsam
wandernder Lichtgrund, farbiger Schein an allem, was gerade wichtig ist, Federphysik unter
jedem Druck, Partikel an den Wendepunkten des Tages.

Frank hat das ausdrücklich so gewollt: **maximale Effekte, überall.** Die frühere Fassung
dieses Abschnitts forderte das Gegenteil („keine Diagramme, keine Zähler … beruhigt sie, oder
drängt sie?"). Sie ist damit **überholt** und gilt nicht mehr. Was aus ihr bleibt, sind zwei
Punkte, die nichts mit Effekten zu tun haben:

- **Die App misst Frank nicht.** Keine Punkte, keine Serien, keine Abzeichen, keine Noten.
  Effekte feiern einen Moment — sie bewerten keine Leistung. Der Funke beim Start eines
  Experiments ist Freude, kein Belohnungssystem.
- **Kein Effekt trägt Information allein.** Alles, was ein Leuchten, eine Bewegung oder ein
  Partikel aussagt, steht zusätzlich als Text oder Form da. Auf der Stufe *Aus* (F-41) ist die
  App vollständig bedienbar.

Die neue Messlatte für jede gestalterische Entscheidung lautet: *Sieht das aus wie eine App
von heute — und bleibt es dabei ruhig genug, dass man morgens um sieben hineinschauen mag?*

**Der Monitor (B-10) ist das Gesicht der App.** Er wird beim Öffnen als Erstes gesehen und
bekommt deshalb die aufwendigste Behandlung: der Lichtgrund, die Glasleisten, die
Kipp-Parallaxe der Karten, der wandernde Lichtsaum um alles, was läuft.

Die vollständige Liste der verbindlichen Effekte steht in **§7 Effekte**.

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
| `Auf Aktion` **NEU in v2** | `#FFF6F1` |
| `Glas` **NEU in v2** | `rgba(21, 18, 16, .62)` mit 24 px Weichzeichnung (`E-03`) |

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
| `Auf Aktion` **NEU in v2** | `#FFF6F1` |
| `Glas` **NEU in v2** | `rgba(248, 244, 238, .66)` mit 24 px Weichzeichnung (`E-03`) |

> **Vierzehn Rollen statt dreizehn.** `Auf Aktion` ist die Schrift **auf** der Aktionsfläche —
> in beiden Erscheinungen derselbe Wert. Sie fehlte in v1, und ohne sie stand die Beschriftung
> des betonten Knopfes im Hellmodus in *Text* auf *Aktion*: lesbar, aber nicht der Entwurf.
> Material 3 hat weniger Rollen, als der Entwurf braucht (`Rand weich`, `Blass`,
> `Aktion gedeckt`, `Erledigt gedeckt`) — deshalb eine eigene Palette statt einer verbogenen
> `ColorScheme`. `color-mix(in srgb, A, B p%)` wird als lineare Mischung im sRGB-Raum
> nachgebildet; ohne sie fehlt den Verläufen und Lichtsäumen die Plastizität.

## 3. Typografie

| Rolle | Familie | Größe | Gewicht | Zeilenhöhe | Laufweite | Quelle |
|-------|---------|-------|---------|------------|-----------|--------|
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(3) ×61 | Inter, sans-serif | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(4) ×14 | Fraunces, serif | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(5) ×14 | "JetBrains Mono", monospace | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:font(0) ×3 | system-ui, -apple-system, "Segoe UI", Roboto, sans-serif | — | — | — | — | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| Bildschirmtitel | Fraunces | 28 | 600 | 34 | 0 | `02-UI-SPEC.md` |
| Abschnittstitel | Fraunces | 22 | 600 | 28 | 0 | `02-UI-SPEC.md` |
| Kartentitel | Fraunces | 19 | 600 | 25 | 0 | `02-UI-SPEC.md` |
| Fließtext | Inter | 16 | 400 | 25 | 0 | `02-UI-SPEC.md` |
| Fließtext klein | Inter | 14 | 400 | 21 | 0 | `02-UI-SPEC.md` |
| Knopfbeschriftung | Inter | 16 | 500 | 20 | 0.2 | `02-UI-SPEC.md` |
| Zwischenüberschrift | Inter | 13 | 600 | 17 | 0.6 | `02-UI-SPEC.md` |
| Daten und Zahlen | JetBrains Mono | 13 | 400 | 18 | 0 | `02-UI-SPEC.md` |
| Stufe / Dauer | JetBrains Mono | 12 | 400 | 16 | 0.4 | `02-UI-SPEC.md` |

### 3.1 Zwölf weitere Rollen — NEU in v2

Der Fold-Entwurf setzt Größen, die die Skala oben nicht kennt. Sie sind verbindlich wie die
übrigen; eine Näherung durch die nächstliegende Rolle gilt als nicht erfüllt.

| Rolle | Familie | Größe / Zeilenhöhe | Gewicht | Wo sie steht |
|-------|---------|--------------------|---------|--------------|
| Bildschirmtitel klein | Fraunces | 26 / 32 | 600 | Titel von `B-03`, `B-08`, `B-09` |
| Gesprächstitel | Fraunces | 20 / 26 | 600 | der Experimentname in der Kopfleiste von `B-02` |
| Gruppentitel | Fraunces | 17 / 23 | 600 | „Experimente" / „Logbuch" in `B-08` |
| Kartentext | Inter | 15 / 23 | 400 | die Beschreibung auf einer Karte |
| Aufgabenzeile | Inter | 15 / 22 | 400 | eine Zeile der To-Do-Liste |
| Knopf klein | Inter | 15 / 20 | 500 | die Knöpfe auf den Karten |
| Reiter | Inter | 14 / 20 | 500 | die drei Reiter von `B-07` |
| Feldbeschriftung | Inter | 13 / 18 | 400 | die Beschriftung über einem Auswahlfeld |
| Erklärung | Inter | 13 / 19 | 400 | der erklärende Satz unter einem Abschnitt |
| Umschalter | Inter | 13 / 18 | 500 | der Morgen/Abend-Umschalter auf `B-01` (`F-49`) |
| Leiste | Inter | 11 / 14 | 400 | die Beschriftung der sechs Leistenfelder |
| Zahl | JetBrains Mono | 15 / 20 | 500 | Tempo-Anzeige in `B-08`, Tagewahl (`F-43`), Uhrzeit |
| Ringstand | JetBrains Mono | 10.5 / 13 | 500 | der Stand im Fortschrittsring |

> **Die drei Schriften sind als Datei eingebettet**, nicht über `googlefonts` geladen.
> Heruntergeladene Schriften kommen verzögert an, bis dahin zeichnet die App System-Sans — und
> genau in diesem Zustand entstehen Bilder, die „irgendwie anders" aussehen, ohne dass man den
> Grund sieht. Alle drei liegen als **variable Schrift** vor, deshalb je Gewicht eine eigene
> `FontVariation`. Die px des Entwurfs sind bei `density: 1` unmittelbar sp.

## 4. Maße und Raster

| Name | px | Original | Quelle |
|------|----|----------|--------|
| --radius-eingabefeld ×5 | 14 | `14px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --radius-karte ×5 | 20 | `20px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --radius-dialog ×5 | 24 | `24px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --radius-vollrund ×4 | 9999 | `9999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| --b07-radius-vollrund | 999 | `999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |

## 5. Formen und Tiefe

| Name | Radius | Quelle |
|------|--------|--------|
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(5) ×5 | `inherit` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(13) ×5 | `20px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(0) ×4 | `999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| Eingabefeld ×2 | `14 dp` | `02-UI-SPEC.md` |
| Reiter, Chip, Stufen-Etikett ×2 | `vollrund` | `02-UI-SPEC.md` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(14) ×2 | `24px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| Karte | `20 dp` | `02-UI-SPEC.md` |
| Dialog | `24 dp` | `02-UI-SPEC.md` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(4) | `50%` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(7) | `20px 20px 6px 20px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(8) | `20px 20px 20px 6px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(11) | `14px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(12) | `9999px` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:radius(15) | `0` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |


| Effekt | Art | CSS | Quelle |
|--------|-----|-----|--------|
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:shadow(0) ×2 | shadow | `0 6px 24px rgba(0, 0, 0, 0.28)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(7) ×2 | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--Aktion-gedeckt) 88%, var(--Text) 6%), var(--Aktion-gedeckt))` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:shadow(2) | shadow | `0 0 20px color-mix(in srgb, currentColor 18%, transparent), inset 0 1px 0 color-mix(in srgb, currentColor 18%, transparent)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(3) | gradient | `background-image: linear-gradient(
        to right,
        var(--rand) 0 1px,
        transparent 1px calc(100% / 12)
      ),
      repeating-linear-gradient(
        to right,
        var(--gedaempft) 0 1px,
        transparent 1px calc(100% / 6)
      )` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(4) | gradient | `background-image: radial-gradient(circle at 92% 8%, color-mix(in srgb, var(--aktion) 18%, transparent) 0, transparent 32%),
    radial-gradient(circle at 0% 82%, color-mix(in srgb, var(--erledigt) 12%, transparent) 0, transparent 28%),
    var(--grund)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(5) | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--aktion), var(--text) 10%), var(--aktion) 58%, color-mix(in srgb, var(--aktion), #000000 16%))` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(6) | gradient | `background-image: radial-gradient(circle at 92% 8%, color-mix(in srgb, var(--Aktion) 18%, transparent) 0, transparent 32%),
    radial-gradient(circle at 0% 82%, color-mix(in srgb, var(--Erledigt) 12%, transparent) 0, transparent 28%),
    var(--Grund)` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |
| .werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html:gradient(9) | gradient | `background-image: linear-gradient(145deg, color-mix(in srgb, var(--Aktion), var(--Text) 10%), var(--Aktion) 58%, color-mix(in srgb, var(--Aktion), #000000 16%))` | `.werft-generated/019fe79c-9efe-757f-867f-19f80caa5a78/1/design.html` |

## 6. Bildschirme

> **Die untere Leiste — verbindlich für alle Hauptbildschirme.** Sie hat in dieser Fassung
> **sechs** Felder statt fünf, in dieser festen Reihenfolge:
> **Monitor · Heute · Ziele · Merkliste · Erkenntnisse · Logbuch.**
> Sie ist auf B-10, B-01, B-04, B-05, B-06 und B-07 identisch, schwebt mit 12 dp Rand und
> Radius 24 dp, ist eine Glasfläche (`E-03`), und jedes Feld ist mindestens 48 dp hoch. Bei
> 412 dp Breite bleiben je Feld 68 dp — die Beschriftung steht in Inter 11/14, das Symbol
> darüber misst 24 dp. F-27 (Wischen) läuft über alle sechs in genau dieser Reihenfolge.
> **Wo in den Bedienelement-Tabellen unten nur fünf Felder aufgeführt sind, ist das der
> Stand des alten Designs; das Monitor-Feld kommt überall als erstes hinzu.**

| Kennung | Bildschirm | Start | führt zu | Dateien je Erscheinung |
|---------|------------|-------|----------|------------------------|
| **B-10** | **Monitor (`B-10`)** | **ja** | **B-01, B-02, B-03, B-04, B-05, B-06, B-07, B-08** | **gebaut** — `CLAUDE-DESIGN/Experimente Fold-Aussendisplay.dc.html`, beide Erscheinungen |
| B-01 | Heute (`B-01`) | nein — **war Start, ist es nicht mehr** | B-02, B-03, B-08, B-10 | `bildschirme/21dunkelstandard/…-heute.html`<br>`bildschirme/22hell/…-heute.html` |
| B-02 | Gespräch (`B-02`) | — | B-01 | `bildschirme/21dunkelstandard/…-gespr-ch.html`<br>`bildschirme/22hell/…-gespr-ch.html` |
| B-03 | Auswertung (`B-03`) | — | B-01 | `bildschirme/21dunkelstandard/…-auswertung.html`<br>`bildschirme/22hell/…-auswertung.html` |
| B-08 | Einstellungen (`B-08`) | — | B-09 | `bildschirme/21dunkelstandard/…-einstellungen.html`<br>`bildschirme/22hell/…-einstellungen.html` |
| B-09 | Selbstbild (`B-09`) | — | B-08 | `bildschirme/21dunkelstandard/…-selbstbild.html`<br>`bildschirme/22hell/…-selbstbild.html` |
| B-06 | Erkenntnisse (`B-06`) | — | — | `bildschirme/21dunkelstandard/…-erkenntnisse.html`<br>`bildschirme/22hell/…-erkenntnisse.html` |
| B-07 | Logbuch (`B-07`) | — | — | `bildschirme/21dunkelstandard/…-logbuch.html`<br>`bildschirme/22hell/…-logbuch.html` |
| B-05 | Merkliste (`B-05`) | — | — | `bildschirme/21dunkelstandard/…-merkliste.html`<br>`bildschirme/22hell/…-merkliste.html` |
| B-04 | Wünsche &amp; Ziele (`B-04`) | — | — | `bildschirme/21dunkelstandard/…-w-nsche-amp-ziele.html`<br>`bildschirme/22hell/…-w-nsche-amp-ziele.html` |

> **Achtung:** Der Eintrag „Experimente-SPEC-v1" in der Vollständigkeitsmessung des Designs
> ist ein Import-Artefakt (der Projektname selbst), kein fehlender Bildschirm.
>
> **Stand v2: alle zehn Bildschirme sind gebaut.** `B-10` ist im Fold-Entwurf entstanden und in
> beiden Erscheinungen umgesetzt. Es fehlt keiner mehr.

### B-10 NEU — Monitor

Startbildschirm: **ja** · Quelle: `CLAUDE-DESIGN/Experimente Fold-Aussendisplay.dc.html`

> **Stand v2: gebaut und gemessen.** Die Werte unten stammen nicht mehr aus einer Ableitung,
> sondern aus dem aufgebauten Bildschirm. Seitenrand 16 dp (nicht 20), Kartenradius 20 dp,
> Innenabstand 21 dp an der Laufkarte / 18 dp an der Wartekarte, Knopfhöhe 48 dp, schwebende
> Leiste 12 dp Rand / Radius 24 dp / 64 dp hoch.

**Aufbau von oben nach unten**

1. **Kopfleiste** (Glas, `E-03`; Höhe 64 dp, unter der Statusleiste)
   - links: Bildschirmtitel **„Monitor"** (Fraunces 28/34, 600)
   - rechts: Erscheinungsschalter (F-26) und Einstellungen (→ B-08), je 48 × 48 dp
2. **Zeile „Steht an: 4 · Läuft: 2"** (JetBrains Mono 13, gedämpft) — die Zahlen zählen bei
   Änderung hoch (`E-20`)
3. **Abschnitt „Läuft"** (Zwischenüberschrift Inter 13/17, 600, Laufweite 0.6)
   - je Experiment eine **Laufkarte**: Radius 20 dp, Innenabstand 21 dp, Fläche `Erhöht`,
     wandernder Lichtsaum am Rand (`E-06`), farbiger Schein nach außen (`E-05`)
   - Inhalt: Titel (Fraunces 19/25) · Etikett **Stufe** und **„Tag 2 von 3"** (JetBrains Mono
     12, vollrund) · Herkunftsetikett · Stand der heutigen Aufgaben („3 von 5") mit
     Fortschrittsring (56 dp, Ring 4 dp, Spur *Rand weich*, Bogen *Aktion* mit rundem Ende,
     Beginn oben bei −90°)
   - **Die Tagesangabe ist antippbar** (`F-42`, `E-26`): 32 dp hoch statt 24, mit Rand
     *Aktion* 55 %. Ein Druck öffnet „Wie lange?"
   - **NEU in v2:** aufgeklappt steht unter den Aufgaben die Zeile
     „4 Auswertungen bisher — jede einzeln unter „Wie ist es gelaufen?"" (JetBrains Mono 12,
     *Blass*), sobald es mindestens eine gibt (`F-45`)
   - aufgeklappt (F-40): vollständige Beschreibung, Zwischenüberschrift **„AUFGABEN HEUTE"**,
     die heutigen Aufgaben als Punkte, Knopf **„Gespräch"** (→ B-02, Gewicht 1) und
     **„Wie ist es gelaufen?"** (→ B-03, Gewicht 1,4)
   - **Genaue Schichtung des Rahmens:** Schein `0 0 22px Aktion 20%` → Radius 20 dp →
     wandernder Lichtsaum (`E-06`, 1,5 dp) → Radius 18,5 dp → Fläche *Erhöht* → Lichtsaum oben
     (`E-04`, *Text* 14 %)
   - sind keine vorhanden: leerer Zustand, Satz siehe §8
4. **Die eine To-Do-Liste des Tages** (F-07), nach Experimenten gruppiert — unverändert
   übernommen von B-01, mit Haken-Effekt `E-17`
5. **Abschnitt „Steht an"**
   - je Experiment eine **Wartekarte**: gleicher Radius, Fläche `Fläche`, **ohne** Lichtsaum
     (nur Laufendes leuchtet), Deckkraft der Fläche 92 %
   - Inhalt links ein **Griff** (20 dp, `Symbole.Griff`) zum Umsortieren (F-38), daneben
     Titel · Etikett „fordernd · 3 Tage" (antippbar, `F-42`) · Herkunftsetikett, rechts ein
     **Kreuz** (36 dp Tippfläche, Symbol 20 dp) zum Herausnehmen (F-39)
   - darunter der Knopf **„Starten"** über die volle Breite (Höhe 48 dp, vollrund,
     Verlauf `Aktion`)
   - aufgeklappt (F-40) zeigt sie die Beschreibung in *Kartentext* / *Gedämpft*
   - ziehbar am Griff nach langem Druck (F-38; ein Schritt = 96 dp), nach links wischbar
     (F-39; Auslösung ab 110 dp, mit dem Warnton `E-25`)
   - sind keine vorhanden: leerer Zustand, Satz siehe §8
6. **Schwebender Plus-Knopf** (F-35): 64 × 64 dp, vollrund, Verlauf `Aktion`, Schein `E-05`,
   Abstand 24 dp vom rechten Rand, 24 dp über der unteren Leiste
7. **Untere Leiste** (Glas, `E-03`; schwebend, 12 dp Rand, Radius 24 dp) — **sechs Felder**:
   **Monitor · Heute · Ziele · Merkliste · Erkenntnisse · Logbuch**

**Bedienelemente**

| Nr. | Element | Art | Was es tut |
|-----|---------|-----|------------|
| 1 | Erscheinung umschalten | button | löst `F-26` aus |
| 2 | Einstellungen | a | führt zu `B-08` |
| 3 | Laufkarte auf-/zuklappen | article | löst `F-40` aus |
| 4 | Gespräch zum Experiment | a | führt zu `B-02` |
| 5 | Wie ist es gelaufen? | a | führt zu `B-03` |
| 6 | Aufgabe abhaken | button | löst `F-08` aus |
| 7 | Wartekarte auf-/zuklappen | article | löst `F-40` aus |
| 8 | Starten | button | löst `F-37` aus |
| 9 | Karte verschieben | article | löst `F-38` aus |
| 10 | Aus dem Monitor nehmen | button | löst `F-39` aus |
| 10a **NEU** | Tagesangabe / Dauer-Etikett | button | löst `F-42` aus — öffnet „Wie lange?" |
| 10b **NEU** | Tagewahl im Dialog | stepper | löst `F-43` aus |
| 10c **NEU** | Vorschläge holen (nur im Zustand `LEER`) | button | führt zu `B-01` |
| 11 | Eigenes Experiment anlegen | button | löst `F-35` aus |
| 12 | Eigenes Experiment einsprechen | button | löst `F-35` aus |
| 13 | Eigenes Experiment | textarea | löst `F-35` aus |
| 14 | Text mit KI verbessern | button | löst `F-02` aus |
| 15 | Speichern | button | löst `F-35` aus |
| 16 | Monitor | a | führt zu `B-10` |
| 17 | Heute | a | führt zu `B-01` |
| 18 | Ziele | a | führt zu `B-04` |
| 19 | Merkliste | a | führt zu `B-05` |
| 20 | Erkenntnisse | a | führt zu `B-06` |
| 21 | Logbuch | a | führt zu `B-07` |

**Zustände**

| Zustand | Was B-10 zeigt |
|---------|----------------|
| `LEER` | Der Satz aus §8 **mittig**, 104 dp unter dem Kopf, in *Abschnittstitel* mit
Zeilenhöhe 30, darunter 24 dp Abstand und der Textknopf **„Vorschläge holen"** (→ `B-01`); der
Plus-Knopf atmet (`M-79`). Beide Abschnitte werden **gar nicht** gezeichnet |
| `NUR_ANSTEHEND` | Nur „Steht an"; „Läuft" ist ganz ausgeblendet, nicht als leere Hülle |
| `LAEUFT` | Beide Abschnitte, dazu die To-Do-Liste des Tages |
| `VOLL` | Drei laufen — alle „Starten"-Knöpfe gesperrt mit dem Hinweis aus F-37 |
| `ANLEGEN` | Die Anlegefläche liegt über dem Bildschirm, der Grund ist weichgezeichnet (`E-03`) und mit *Grund* 62 % hinterlegt |
| *lädt* | **Drei** Schimmer-Skelette statt Karten (`E-13`), je 118 dp hoch, Radius 20 dp, Fläche *Fläche*, 1 dp Rand *Rand weich* |

**Die Anlegefläche (`ANLEGEN`) — gemeinsam für `B-10`, `B-04` und `B-05`**

Sie fährt von unten herein (`M-80`, 320 ms) und legt sich über den Bildschirm: oben Radius
24 dp, Fläche *Fläche*, Lichtsaum, Innenabstand 16 dp seitlich / 24 dp oben / 28 dp unten.

1. **Titel** (Abschnittstitel) und **Unterzeile** (Fließtext klein, *Gedämpft*) — je Ort anders:
   `B-10` „Eigenes Experiment" / „Einsprechen oder tippen. Es steht danach unter „Steht an"." ·
   `B-05` „Eigenes Experiment" / „Einsprechen oder tippen. Es liegt danach auf der Merkliste." ·
   `B-04` „Neues Ziel" / „Ein Wunsch oder ein Ziel, an dem sich die Vorschläge ausrichten."
2. **Sprechknopf** (56 dp, vollrund, Fläche *Aktion*, Symbol 26 dp) und **Eingabefeld**
   (Mindesthöhe 120 dp, Innenabstand 14 dp) nebeneinander, 12 dp Abstand
3. **Tagewahl** (`F-43`) mit der Beschriftung „Wie viele Tage soll es laufen?" — nur auf `B-10`
   und `B-05`, nicht bei Zielen
4. Drei Knöpfe: **„Abbrechen"** (Gewicht 1) · **„Verbessern"** bzw. **„Zurücknehmen"**
   (Gewicht 1, in *Aktion*) · **„Speichern"** (betont, Gewicht 1,2)

**Ein Druck neben die Fläche legt sie beiseite und verwirft nichts** (`F-58`). Die Meldungen
liegen **über** der Anlegefläche — vorher lagen sie darunter: die Störung war unsichtbar, und
der Druck auf „Nochmal" traf die Fläche dahinter und schloss die Anlegefläche.

**Bewegungen auf diesem Bildschirm**

- `M-76` Lichtgrund wandert — 24000 ms, `cubic-bezier(0.42, 0, 0.58, 1)`, endlos
- `M-77` Glasleiste verdichtet sich beim Scrollen — 200 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-78` Karte kippt zur Neigung des Geräts — Federphysik, dauerhaft
- `M-79` Plus-Knopf atmet — 3200 ms, `cubic-bezier(0.42, 0, 0.58, 1)`, endlos
- `M-80` Anlegefläche fährt herein — 320 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-81` Neue Karte fliegt ein und funkelt — 480 ms, Federphysik, einmal
- `M-82` Übernommener Vorschlag fliegt zum Monitor-Feld — 520 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-83` Karte wandert von „Steht an" nach „Läuft" — 400 ms, Federphysik, einmal
- `M-84` Funken beim Start — 1200 ms, `linear`, einmal
- `M-85` Karte hebt sich beim Ziehen ab — 160 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-86` Karte klappt auf — 280 ms, Federphysik, einmal
- `M-87` Fortschrittsring füllt sich mit Leuchtspur — 600 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-88` Zahl zählt hoch — 400 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-89` Schimmer über dem Skelett — 1400 ms, `linear`, endlos
- `M-90` Lichtsaum wandert um die Laufkarte — 6000 ms, `linear`, endlos
- `M-91` Bildschirmwechsel mit Weichzeichnen und Skalieren — 260 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-92` Geteiltes Element beim Wechsel — 300 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-93` Lichtblüte beim Abschließen — 1600 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-94` Leistenfeld leuchtet auf — 240 ms, `cubic-bezier(.2, 0, 0, 1)`, einmal
- `M-95` Gestaffeltes Erscheinen der Karten — 240 ms je Karte, 60 ms Versatz, `cubic-bezier(.2, 0, 0, 1)`, einmal

### B-01 — Heute

Startbildschirm: **nein** (seit v1-überarbeitet; `B-10` ist der Start) · Quelle: `B-01`

> **Geändert in v2.** Vier Dinge sind dazugekommen:
> 1. **Die Datumszeile** ganz oben: das Datum links in JetBrains Mono 13 / *Gedämpft*, in
>    **Großbuchstaben**, Form „MONTAG, 14. AUGUST 2026" — rechts daneben der
>    **Morgen/Abend-Umschalter** (`F-49`): zwei Pillen in einer Spur `color-mix(Text 8%)`,
>    je 36 dp hoch, Innenabstand 4 dp, die aktive auf *Fläche* mit Schrift in *Aktion*.
> 2. **Der Zustand `AUFNAHME`**: unter dem Sprechknopf stehen die **Wellenform** (`E-18`) und
>    die **Sekundenanzeige** „00:07" (JetBrains Mono 13, *Gedämpft*).
> 3. **Der Zustand `WARTET`**: die Wartekarte „Ich sehe mir an, was ich über dich weiß …".
> 4. **Die Abendkarte** ersetzt die alte Laufkarte auf `B-01`: Titel, darunter
>    „Tag 2 von 3 · 3 von 5 erledigt", eine Trennlinie, die heutigen Aufgaben mit Kästchen zum
>    Abhaken (je durch eine 1-dp-Linie getrennt), darunter **„Gespräch"** und
>    **„Nicht umgesetzt"** nebeneinander und **„Wie ist es gelaufen?"** betont über die volle
>    Breite.
>
> **Die Vorschlagskarte** trägt jetzt oben rechts das Merken-Symbol (40 dp Tippfläche, füllt
> sich beim Merken), darunter Beschreibung, die Meta-Zeile „leicht · 1 Tag", bei einem
> Merklisten-Vorschlag das Etikett „von deiner Merkliste" — und die zwei Knöpfe
> **„In den Monitor"** (umrandet, in *Aktion*) und **„Jetzt starten"** (betont). Sie lässt sich
> wischen (`E-22`, `E-25`).

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
| 5 | Heutige Lage | textarea | **Geklärt in dieser Fassung:** nimmt den transkribierten Text auf und bleibt frei bearbeitbar (F-01 Schritt 7, F-28) |
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
| 23 **NEU** | In den Monitor | button | löst `F-36` aus — legt den Vorschlag unter „Steht an"; die übrigen Karten bleiben stehen |
| 24 **NEU** | Jetzt starten | button | löst `F-06` aus — übernimmt und startet in einem Zug; gesperrt, wenn drei laufen |
| 25 **NEU** | Monitor | a | führt zu `B-10` |

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

> **Geändert in v2.**
> - **An jeder Blase steht ein Lautsprecher** (`F-47`, 40 dp): bei eigenen Runden **links**
>   davon, bei Antworten **rechts** — so bleibt er am Rand und verschiebt die Blase nicht aus
>   ihrer Flucht.
> - Die Blasen sind höchstens **300 dp** breit; eigene rechts mit Radius `20 20 6 20` auf
>   *Erhöht*, die der KI links mit `20 20 20 6` auf *Fläche* und 1 dp Rand.
> - Die Kopfleiste trägt den **Experimentnamen** (Fraunces 20/26, einzeilig mit Auslassung) und
>   darunter „Tag 2 von 3".
> - Die **Eingabeleiste** unten ist eine Glasfläche, 96 dp hoch: ein Feld (56 dp, Radius 14 dp,
>   *Erhöht*, 1 dp Rand) mit Platzhalter „Nachricht eingeben" und dem Mikrofon (48 dp) darin,
>   daneben der runde **Sende-Knopf** (56 dp, Fläche *Aktion*, Symbol 26 dp).
> - **Auswertungen stehen hier nicht mehr als Blase.** Der leere Zustand sagt, wo sie sind:
>   „Hier steht das Gespräch zu diesem Experiment. / Deine Auswertungen findest du unter „Wie
>   ist es gelaufen?" — dort steht jeder Tag einzeln."
> - Der Wartezustand sind **drei Punkte** à 6 dp in einer KI-Blase (`M-98`).

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

> **Geändert in v2, am stärksten von allen Bildschirmen.** Er arbeitet **ein** Experiment ab,
> nicht mehr alle offenen der Reihe nach.
>
> **Aufbau von oben nach unten**
> 1. Kopfleiste mit Zurück-Pfeil und dem Titel „Auswertung" (Fraunces 26/32)
> 2. Titel des Experiments (Kartentitel) und darunter „Tag 2 von 3 · fordernd"
> 3. Je nach Zustand: der **Sprechknopf** mit „Erzähl, was daraus geworden ist." und
>    „Lieber tippen" · das **Eingabefeld** (Mindesthöhe 140 dp) mit „Text mit KI verbessern"
>    und „Weiter" · die **Wartekarte** „Ich denke darüber nach …" · die **Antwortkarte**
> 4. **Die Antwortkarte** (Radius 20 dp, Fläche *Fläche*, 1 dp Rand, Innenabstand 18 dp):
>    Zwischenüberschrift **„EINSCHÄTZUNG"** in *Aktion*, rechts der Lautsprecher; darunter der
>    Text mit dem **Mitlese-Streifen** (`E-21`, Hinterlegung *Aktion* 26 %); darunter
>    „Nochmal versuchen"
> 5. **NEU — die Abschlussfrage** (`F-44`): eine 1-dp-Trennlinie, dann „Wie soll es
>    weitergehen?" bzw. „Der letzte geplante Tag ist erreicht. Wie soll es weitergehen?",
>    darunter **„Weiterführen"** (umrandet, *Aktion*) und **„Abschließen"** (betont)
>    nebeneinander, darunter **„Zwischenstand"** / **„Später entscheiden"** links und
>    **„Nicht umgesetzt"** rechts als Textknöpfe
> 6. **NEU — der Verlauf** (`F-45`): Zwischenüberschrift „VERLAUF · 4 AUFNAHMEN", darunter je
>    Aufnahme eine **Klappkarte**
>
> **Die Klappkarte einer Aufnahme:** zugeklappt „Tag 2" (JetBrains Mono 12, *Aktion*), darunter
> „13.08.2026, 19:41 Uhr" (*Blass*), rechts ggf. „Abschluss" in *Erledigt* und der Lautsprecher
> (40 dp), ganz rechts der Klapp-Pfeil, der sich um 180° dreht (`M-96`). Aufgeklappt:
> „WAS ICH ERZÄHLT HABE" mit Franks Wortlaut (Fließtext, *Text*), darunter „EINSCHÄTZUNG" mit
> dem Text der KI (Kartentext, *Gedämpft*).
>
> **Die Lichtblüte** (`E-16`, `M-93`) liegt beim Abschließen über dem ganzen Bildschirm.

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

> **Geändert in v2. Sieben Abschnitte in dieser Reihenfolge:** Modelle (`F-22`) · Stimme
> (`F-23`) · Zugänge (`F-24`) · Erinnerungen (`F-25`) · **Effekte** (`F-41`) · Erscheinung
> (`F-26`) · Über mich (→ `B-09`). Darunter steht die **Version**.
>
> - **Auswahlfelder sind echte Aufklapplisten**, keine Weiterschalter: 48 dp hoch, Radius 14 dp,
>   1 dp Rand, Fläche *Erhöht*, rechts ein Dreieck, das sich beim Öffnen um 180° dreht
>   (`M-97`). Die Liste trägt die Sprache der App (Fläche *Fläche*, 1 dp Rand, Radius 14 dp,
>   je Eintrag mindestens 48 dp, höchstens 360 dp hoch); **der gewählte Eintrag steht in
>   *Aktion* mit einem Haken davor** — die Farbe trägt die Auswahl nicht allein.
>   Stimmkennungen stehen in JetBrains Mono, sie sind Daten, kein Fließtext.
> - **Die Gerätecodekarte** (`F-54`): Radius 14 dp, Fläche *Erhöht*, Rand *Aktion* 55 %,
>   Innenabstand 16 dp. Oben „Diesen Code auf der geöffneten Seite eintippen"
>   (Feldbeschriftung), darunter der Code in **JetBrains Mono 26/32 mit 3 sp Laufweite** in
>   *Aktion*, daneben **„Kopieren"**, darunter die Adresse (*Blass*) und
>   **„Seite erneut öffnen"**.
> - **Die Schlüsselfelder**: Name links, Zweck rechts (*Blass*), darunter das verdeckte Feld
>   (48 dp) und daneben ein **Auge** (48 dp, Radius 14 dp) zum Sichtbarmachen.
> - **Die Erinnerungszeile** (56 dp, Radius 14 dp, *Erhöht*, 1 dp Rand): Name links; rechts
>   **Wecker-Symbol und Uhrzeit als eine Tippfläche** (`F-55`) und der Schalter (48 × 28 dp,
>   Knopf 22 dp weiß, wandert in 200 ms — `M-27`). Darunter der Satz „Tipp auf den Wecker, um
>   die Uhrzeit zu ändern."
> - **Die Uhr** (`F-55`) ist ein Dialog mit 24-Stunden-Anzeige, Radius 20 dp, Fläche *Fläche*.
> - **Effekte und Erscheinung** sind **Pillenreihen** (44 dp hoch, Spur `color-mix(Text 8%)`,
>   5 dp Innenabstand): „Voll · Gedämpft · Aus" und **„Dunkel · Hell · Wie das System"**.
>   Unter den Effekten steht „Auf Aus bleibt jede Funktion vollständig bedienbar."
> - **Die Sprechgeschwindigkeit** ist ein Schieber von 0,70 bis 1,30 mit 11 Zwischenschritten;
>   der Wert steht rechts oben als „1,00" (JetBrains Mono 15, *Aktion*), unter dem Schieber die
>   drei Marken „0,70 · 1,00 · 1,30".
> - **„Meine Stimme"** (`F-53`) zeigt die Auswahl der registrierten Stimmen mit Namen, darunter
>   **„Neue Stimme aufnehmen"** / **„Aufnahme beenden"** und **„Neu laden"**, darunter
>   **„Gewählte Stimme löschen"** in *Warnung* und der Satz „Deine eigene Stimme wird über
>   Alibaba erzeugt. Der Zugang dafür steht unter „Zugänge"."
> - **Die Version** steht ganz unten, 20 dp abgesetzt, in JetBrains Mono 12 / *Blass*:
>   „Version 0.14.0 (13.08.2026, 20:05 Uhr)". Sie wird **aus der Quelle abgeleitet**, nicht
>   doppelt hinterlegt — damit ein angekommenes Update sofort erkennbar ist.

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

> **Geändert in v2** (`F-52`). Von oben nach unten: das große Feld (Mindesthöhe **360 dp**,
> Platzhalter „Erzähl der App, wer du bist.") · die **Standzeile** „Noch nicht gespeichert." in
> *Warnung* oder „Gespeichert." in *Erledigt* · der betonte Knopf **„Speichern"** über die volle
> Breite · daneben **„Text mit KI verbessern"** / **„Zurücknehmen"** (Gewicht 1) und ein
> Mikrofon-Knopf (56 × 48 dp, Radius 14 dp, Fläche *Aktion*) · unten der Satz „Dein Selbstbild
> geht als erster Block in jede Anfrage ein — es prägt alle Vorschläge und Einschätzungen."
> Der Zurück-Pfeil sichert vor dem Verlassen.

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

> **Geändert in v2 — es gibt einen dritten Reiter** (`F-46`). Die drei heißen kurz
> **„15 Tage" · „Langzeit" · „Auswertungen"**; sie liegen direkt unter der Kopfleiste, 16 dp vom
> Rand, in einer Spur `color-mix(Text 8%)` mit 5 dp Innenabstand, je Reiter 40 dp hoch,
> vollrund, Inter 14/20; der aktive trägt die Fläche *Fläche* und Schrift in *Text*.
>
> **Jeder Tageseintrag** trägt jetzt: das Datum in *Aktion* (JetBrains Mono 12), rechts einen
> **Lautsprecher** (`F-47`), darunter die erste Zeile als Titel (Kartentitel, 90 Zeichen), den
> vollen Text (Kartentext, *Gedämpft*), darunter **„Ändern"** links und einen **Papierkorb**
> rechts. Das Löschen fragt einmal zurück: statt des Papierkorbs erscheinen
> **„Wirklich löschen"** (in *Warnung*) und **„Behalten"**. „Ändern" macht daraus ein
> Eingabefeld (Mindesthöhe 160 dp) mit „Abbrechen" und „Sichern".
>
> **Der Reiter *Auswertungen*** zeigt je Experiment ein **Fach**: Titel (Kartentitel) und
> darunter „4 Aufnahmen · zuletzt 13.08.2026, 19:41 Uhr" (JetBrains Mono 12, *Gedämpft*); das
> jüngste Fach steht offen. Darin liegen dieselben Klappkarten wie auf `B-03`, je 12 dp
> abgesetzt.

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

> **Geändert in v2.** Die Anlegefläche ist **dasselbe Blatt** wie auf `B-10` und `B-04` (siehe
> `B-10`) und trägt die **Tagewahl** (`F-43`). Jede Karte zeigt Titel und die Meta-Zeile
> „mittel · 2 Tage", rechts einen **Papierkorb** (40 dp) — gelöscht wird **ohne** Rückfrage
> (`F-19`). Der Dialog aus dem alten Entwurf entfällt.

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
| 11 | Abbrechen | button | löst `F-33` aus — **geklärt in dieser Fassung:** schließt die Anlegefläche, ohne zu speichern; der Text wird verworfen |
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

## 7. Effekte

Dieser Abschnitt ist **neu** und in dieser Fassung der Kern des Auftrags: Frank will eine
durchgehend moderne App mit maximalen Effekten. Jeder Effekt hat eine Kennung `E-…`, ist
verbindlich, und nennt seinen Weg in Jetpack Compose. Die zugehörigen Dauern und Kurven
stehen im Motion-Spec (`M-76` bis `M-95`).

**Drei Regeln stehen über allem:**

1. **Kein Effekt trägt Information allein.** Was ein Leuchten sagt, sagt daneben auch ein
   Wort oder eine Form.
2. **Jeder Effekt hat eine Rückfallebene**, die auf `minSdk 26` funktioniert und auf der
   Stufe *Aus* (F-41) greift.
3. **Bildrate vor Pracht.** Kein Effekt darf die App unter 60 fps drücken; auf Geräten mit
   120 Hz ist 120 fps das Ziel. Wo beides nicht geht, gewinnt die Bildrate.

### 7.1 Grund und Fläche

| Kennung | Effekt | Wie | Rückfallebene |
|---------|--------|-----|---------------|
| `E-01` | **Lichtgrund** — zwei bis drei weiche Farbkreise (`Aktion` 18 %, `Erledigt` 12 %) über `Grund`, die langsam wandern (`M-76`) | `Brush.radialGradient` in einem `Canvas` hinter dem Inhalt, Mittelpunkte über `infiniteRepeatable` bewegt | Standbild derselben Verläufe |
| `E-02` | **Filmkorn** — feine Rauschtextur über dem Grund, Deckkraft 3 %, verhindert Farbstreifen in den Verläufen | gekacheltes Rausch-Bitmap, `BlendMode.Overlay` | entfällt ersatzlos |
| `E-03` | **Glasflächen** — Kopfleiste, untere Leiste, Anlegefläche und Dialoge zeichnen den Grund weich | ab API 31 `Modifier.graphicsLayer { renderEffect = BlurEffect(24f, 24f) }` auf einer Kopie des Hintergrunds | halbdurchlässige Fläche (`Erhöht`, 88 %) mit 1 dp Rand `Rand weich` |
| `E-04` | **Lichtsaum** — 1 dp Innenkante oben an jeder Karte, Verlauf von `Text` 18 % nach transparent | `Modifier.drawWithContent` mit `linearGradient` | einfacher Rand `Rand` |
| `E-05` | **Schein** — farbiger Schein nach außen an allem, was gerade aktiv ist (`Aktion` 18 %, Radius 20 dp) | `Modifier.shadow(20.dp, spotColor = Aktion, ambientColor = Aktion)` bzw. gezeichneter Radialverlauf | entfällt |
| `E-06` | **Wandernder Rand** — um laufende Karten läuft ein Lichtband einmal in 6 s herum (`M-90`) | `Brush.sweepGradient` mit rotierendem Winkel als Rahmen | statischer Rand `Aktion` 40 % |
| `E-24` | **Tageszeit-Stimmung** — der Farbton des Lichtgrunds wandert über den Tag: morgens wärmer, abends tiefer | Farbwerte aus der Uhrzeit interpoliert, Wechsel über 60 s | fester Farbton |

### 7.2 Berührung und Bewegung

| Kennung | Effekt | Wie | Rückfallebene |
|---------|--------|-----|---------------|
| `E-07` | **Federphysik** — jeder Druck sinkt ein und schwingt zurück, statt linear zu skalieren | `animateFloatAsState(spring(dampingRatio = 0.55f, stiffness = 380f))` auf `scale` | `tween(120)` |
| `E-08` | **Kipp-Parallaxe** — Karten neigen sich mit dem Gerät, höchstens ±6°, Inhalt versetzt sich um bis zu 4 dp (`M-78`) | `SensorManager` (Rotationsvektor), gedämpft, auf `graphicsLayer { rotationX/rotationY }` | aus |
| `E-09` | **Scroll-Parallaxe** — Kopfbereich und Lichtgrund bewegen sich mit 0,4× der Scrollstrecke, die Glasleiste verdichtet sich (`M-77`) | `LazyListState.firstVisibleItemScrollOffset` → `graphicsLayer` und Deckkraft | aus |
| `E-10` | **Gestaffeltes Erscheinen** — Listen bauen sich mit 60 ms Versatz je Eintrag auf (`M-95`) | `AnimatedVisibility` mit index-abhängiger `delayMillis` | alles gleichzeitig |
| `E-11` | **Geteilte Elemente** — beim Öffnen einer Karte wandert sie sichtbar zum Zielbildschirm (`M-92`) | `SharedTransitionLayout` (Compose 1.7+) mit `sharedElement` auf Titel und Fläche | einfaches Überblenden |
| `E-12` | **Bildschirmwechsel** — der abgehende Bildschirm wird weichgezeichnet und auf 96 % verkleinert, der neue kommt scharf und auf 104 % beginnend herein (`M-91`) | `AnimatedContent` mit `scaleIn/scaleOut` plus `BlurEffect` ab API 31 | Überblenden |
| `E-22` | **Wischbare Vorschlagskarten** — Vorschlagskarten auf B-01 lassen sich mit dem Finger kippen und wegschieben; nach rechts = in den Monitor (F-36), nach links = verworfen | `Modifier.draggable` + `graphicsLayer { rotationZ = versatz * 0.06f }` | nur die Knöpfe |

### 7.3 Zustand und Rückmeldung

| Kennung | Effekt | Wie | Rückfallebene |
|---------|--------|-----|---------------|
| `E-13` | **Schimmer-Skelett** — beim Laden stehen Kartenumrisse mit wanderndem Lichtstreifen (`M-89`) | `Brush.linearGradient` mit animiertem Versatz | ruhige graue Fläche |
| `E-14` | **Wartezustand der KI** — der bestehende `M-09` bekommt zusätzlich einen Lichtstreifen, der durch die entstehende Antwortfläche zieht | wie `E-13`, in `Aktion` | `M-09` allein |
| `E-15` | **Funken beim Start** — 24 kleine Lichtpunkte steigen aus der Karte auf und verlöschen (`M-84`) | `Canvas` mit Partikelliste, Lebensdauer 1200 ms, Schwerkraft nach oben | einmaliges Aufleuchten |
| `E-16` | **Lichtblüte beim Abschließen** — ein Ring aus Licht dehnt sich aus der abgeschlossenen Karte, dazu 40 Partikel (`M-93`) | `Canvas`, Radius über `tween(1600)` | einmaliges Aufleuchten |
| `E-17` | **Haken** — der Haken zeichnet sich (`M-06`), ein kurzer Lichtblitz läuft über die Zeile, die Zeile dämpft sich, dazu Haptik | `PathMeasure` auf dem Haken-Pfad + `linearGradient`-Blitz | nur der gezeichnete Haken |
| `E-18` | **Aufnahme-Wellenform** — während der Aufnahme zeigt ein Kranz aus 48 Balken den echten Mikrofonpegel | Pegel aus dem `AudioRecord`-Puffer (RMS, 32 ms), `Canvas` | nur der atmende Ring `M-02` |
| `E-19` | **Pulsringe** — drei Ringe steigen gestaffelt aus dem Sprechknopf auf | drei `infiniteRepeatable` mit 0/400/800 ms Versatz | ein Ring (`M-02`) |
| `E-20` | **Zählende Zahlen** — Anzahlen und Fortschritt zählen hoch statt zu springen (`M-88`) | `animateIntAsState` | Sprung |
| `E-21` | **Mitlesen beim Vorlesen** — der gerade gesprochene Abschnitt wird hervorgehoben | Zeitmarken des Anbieters, sonst gleichmäßig über die Dauer geschätzt | keine Hervorhebung |
| `E-23` | **Haptik** — feste Muster: kurz (10 ms) beim Abhaken · doppelt beim Aufnahmebeginn und -ende (`M-03`, `0/25/60/25`) · aufsteigend beim Starten (`0/20/40/30/40/45` mit Stärke `0/70/0/140/0/220`) · lang-weich beim Abschließen (`0/220`, Stärke `0/90`) · Fehler = zwei harte Stöße (`0/60/80/60`, Stärke `0/255/0/255`) | `VibrationEffect.createWaveform` | aus |

### 7.5 Fünf weitere Effekte — NEU in v2

| Kennung | Effekt | Wie | Rückfallebene |
|---------|--------|-----|---------------|
| `E-25` | **Wisch-Rückmeldung** — beim Wischen scheint durch die Karte, was gleich geschieht: nach rechts `Aktion` 18 % mit dem Wort **„IN DEN MONITOR"** rechtsbündig, nach links `Warnung` 16 % mit **„VERWORFEN"** linksbündig (`B-01`); an einer anstehenden Karte (`B-10`) nur der Warnton. Die Deckkraft wächst mit der Wischweite | Fläche über der Karte plus Text in *Zwischenüberschrift*, `alpha` aus dem Wischweg | nur die Knöpfe |
| `E-26` | **Antippbares Etikett** — ein Etikett, hinter dem eine Handlung steht (die Tagesangabe, `F-42`), ist **32 dp** hoch statt 24 und trägt einen Rand in `Aktion` 55 %; es unterscheidet sich damit sichtbar von den bloßen Angaben daneben | `Modifier.border` + `semantics { contentDescription = … }` | unverändert, der Rand bleibt |
| `E-27` | **Klapp-Pfeil** — jede Klappkarte trägt rechts einen Pfeil, der zugeklappt nach unten zeigt („da ist mehr") und sich beim Öffnen um 180° dreht (`M-96`) | `Modifier.rotate(animierterWinkel)` | Pfeil ohne Drehung |
| `E-28` | **Gestrichelter Rand** — jeder leere Zustand steht in einem Kasten mit `1px dashed` *Rand*, Radius 20 dp, Innenabstand 20 dp | gezeichnet mit `PathEffect.dashPathEffect(6 dp, 5 dp)` — Compose hat dafür kein fertiges Bauteil | durchgezogener Rand |
| `E-29` | **Filmkorn als Kachel** — das Rauschen (`E-02`) wird als **eine** 120 × 120 große Kachel erzeugt und danach wiederholt gezeichnet | `ShaderBrush(ImageShader(kachel, Repeated, Repeated))`, Deckkraft 3,5 % | entfällt |

> **`E-29` ist kein Schönheitsdetail.** Die erste Fassung zeichnete das Rauschen Punkt für Punkt
> bei jedem Bild — auf dem Fold-Außendisplay rund **273 000 Rechtecke je Bild**. Die App hing
> sofort (ANR). Eine gekachelte Textur kostet einen einzigen Zeichenbefehl. Wer `E-02` neu baut,
> baut es gekachelt.

### 7.4 Leistung, Akku und Grenzen

- **Dauerbewegungen halten an**, sobald der Bildschirm nicht sichtbar ist
  (`LifecycleEventEffect(ON_STOP)`), und laufen beim Zurückkehren weiter.
- **Höchstens zwei Dauerbewegungen** je Bildschirm gleichzeitig sichtbar. Der Lichtgrund
  (`E-01`) zählt als eine davon.
- **Partikel** (`E-15`, `E-16`) laufen nur auf ausdrückliche Handlung, nie automatisch, nie
  mehr als eine Wolke gleichzeitig, höchstens 40 Punkte.
- **Weichzeichnen** (`E-03`, `E-12`) nur über `RenderEffect` ab API 31 — **nie** durch
  wiederholtes Skalieren von Bitmaps.
- **Bei Energiesparmodus** (`PowerManager.isPowerSaveMode`) gilt automatisch mindestens die
  Stufe *Gedämpft* (F-41).
- **Auf *Gedämpft* verschwinden Dauerbewegungen nicht** — sie stehen still und bleiben mit
  **45 % Deckkraft** sichtbar. Erst auf *Aus* werden sie ausgeblendet. So bleibt erkennbar,
  dass dort etwas ist.
- **Der Neigungssensor wird auf *Gedämpft* und *Aus* gar nicht erst angemeldet** — ein stiller
  Sensor kostet keinen Strom.
- **Weichzeichnen** ist nur ab API 31 echtes `RenderEffect`; darunter und auf *Aus* greift die
  halbdurchlässige Fläche (*Erhöht*, 88 %). `minSdk` ist 26, dieser Fall tritt also wirklich ein.
- **Bei „Bewegung reduzieren"** gilt Motion-Spec §8 und zusätzlich F-41.

## 8. Leere Zustände und feste Texte

Diese Sätze sind **wörtlich** zu übernehmen.

| Ort | Text |
|-----|------|
| B-10, beide Abschnitte leer | „Hier steht noch nichts. Leg dir eines an oder hol dir Vorschläge." |
| B-10, „Läuft" leer | „Noch läuft nichts. Starte eines von unten." |
| B-10, „Steht an" leer | „Nichts vorgemerkt." |
| B-10, drei laufen | „Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst." |
| B-01, keine Lage | „Wie ist deine Lage heute?" |
| B-04 leer | „Noch keine Ziele. Sprich eines ein." |
| B-05 leer | „Die Merkliste ist leer." |
| B-06 leer | „Noch keine Erkenntnisse. Sie wachsen aus den Auswertungen." |
| B-07, beide Reiter leer | „Das Logbuch beginnt mit dem ersten Tag." |
| B-09 leer | „Erzähl der App, wer du bist." |
| kein Netz | „Dafür brauche ich Netz." |
| Aufnahme ohne Ton | „Da war nichts zu hören." |
| Verbessern fehlgeschlagen | „Der Text konnte nicht verbessert werden." |
| Anmeldung abgelaufen | „Deine Anmeldung ist abgelaufen." |
| Kontingent erschöpft | „Dein Kontingent ist erschöpft." |
| Antwort unbrauchbar | „Die Antwort war unbrauchbar." |
| Mikrofon abgelehnt | „Ohne Mikrofon kann ich dich nicht hören." |
| Stimme ohne Schlüssel | „Für diese Stimme fehlt der Schlüssel." |

### 8.1 Weitere feste Texte — NEU in v2

Auch diese sind **wörtlich** zu übernehmen.

| Ort | Text |
|-----|------|
| B-10, Zählzeile | „Steht an: 4 · Läuft: 2" |
| B-10, Zwischenüberschriften | „LÄUFT" · „HEUTE ZU TUN" · „STEHT AN" · „AUFGABEN HEUTE" |
| B-10, Knopf im leeren Zustand | „Vorschläge holen" |
| B-10, Verlaufszeile auf der Laufkarte | „4 Auswertungen bisher — jede einzeln unter „Wie ist es gelaufen?"" |
| B-01, Untertitel zur Lage | „Was für ein Tag ist das? Was liegt vor dir?" |
| B-01, Wartezustand | „Ich sehe mir an, was ich über dich weiß …" |
| B-01, Abend, eines | „Ein Experiment wartet auf deine Auswertung." |
| B-01, Abend, mehrere | „3 Experimente warten auf deine Auswertung." |
| B-01, Überschrift der Vorschläge | „FÜNF VORSCHLÄGE FÜR HEUTE" |
| B-01, drei laufen | „Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst. Im Monitor siehst du sie." |
| B-02 leer | „Hier steht das Gespräch zu diesem Experiment.<br>Deine Auswertungen findest du unter „Wie ist es gelaufen?" — dort steht jeder Tag einzeln." |
| B-03, Aufforderung | „Erzähl, was daraus geworden ist." |
| B-03, Wartezustand | „Ich denke darüber nach …" |
| B-03, Abschlussfrage | „Wie soll es weitergehen?" |
| B-03, Abschlussfrage am letzten Tag | „Der letzte geplante Tag ist erreicht. Wie soll es weitergehen?" |
| B-03, Verlauf | „VERLAUF · 4 AUFNAHMEN" · „WAS ICH ERZÄHLT HABE" · „EINSCHÄTZUNG" |
| B-07, Reiter *Auswertungen* leer | „Noch keine Auswertung. Sie entsteht, wenn du erzählst, wie es gelaufen ist." |
| B-07, Löschen | „Wirklich löschen" · „Behalten" |
| B-09, Stand | „Noch nicht gespeichert." · „Gespeichert." · „Schon gespeichert." |
| B-09, Wirkung | „Dein Selbstbild geht als erster Block in jede Anfrage ein — es prägt alle Vorschläge und Einschätzungen." |
| Leeres Feld beim Speichern | „Da steht noch nichts. Sprich etwas ein oder tipp es." |
| Auswertung ohne Experiment | „Zu welchem Experiment gehört das? Öffne es im Monitor." |
| Kein Groq-Schlüssel | „Für die Spracherkennung fehlt der Groq-Schlüssel. Er steht in den Einstellungen." |
| Mikrofon abgelehnt (lang) | „Ohne Mikrofon kann ich dich nicht hören. Die Erlaubnis steht in den Systemeinstellungen." |
| Aufnahme startet nicht | „Die Aufnahme ließ sich nicht starten." |
| Rückfall auf die Gerätestimme | „Google Chirp 3 HD: der Schlüssel wird abgelehnt. Ich lese mit der Stimme des Geräts vor." |
| Gerätestimme antwortet nicht | „Auch die Stimme des Geräts antwortet nicht. Sie lässt sich in den Android-Einstellungen unter „Sprachausgabe" einrichten." |
| Schlüssel abgelehnt | „Der Schlüssel für diese Stimme wird abgelehnt. Prüf ihn in den Einstellungen." |
| Nichts zum Vorlesen | „Hier ist nichts zum Vorlesen." |
| Anmeldung abgelaufen (lang) | „Deine Anmeldung ist abgelaufen. In den Einstellungen kannst du sie erneuern." |
| Kontingent erschöpft (lang) | „Dein Kontingent ist erschöpft. Versuch es später noch einmal." |
| Selbstbild nicht gesichert | „Das Selbstbild ließ sich nicht speichern. …" |
| Abschluss nicht gesichert | „Der Abschluss ließ sich nicht speichern." |

**Bestätigungen** (Hinweiszeile unten, verschwindet nach 2600 ms):

| Anlass | Text |
|--------|------|
| Übernommen | „„Kalt duschen" steht jetzt im Monitor unter „Steht an"." |
| Schon im Monitor | „„Kalt duschen" steht schon im Monitor." |
| Gemerkt | „„Kalt duschen" liegt auf der Merkliste." · „… liegt schon auf der Merkliste." |
| Eigenes angelegt | „Steht jetzt unter „Steht an" — 3 Tage." · „Liegt auf der Merkliste — 3 Tage." |
| Umsortiert | „Reihenfolge geändert." |
| Herausgenommen | „„Kalt duschen" ist wieder auf der Merkliste." · „… ist gelöscht." |
| Dauer geändert | „„Kalt duschen" läuft jetzt 8 Tage." · „… endet jetzt nach 2 Tagen." |
| Weitergeführt | „Läuft weiter — jetzt 8 Tage." |
| Zwischenstand | „Zwischenstand gespeichert. Es läuft weiter." |
| Abgeschlossen | „Abgeschlossen. Die Auswertung steht im Logbuch." |
| Nicht umgesetzt | „Nicht umgesetzt — es liegt wieder auf der Merkliste." |
| Weckzeit | „Morgens um 07:30 Uhr." · „Abends um 21:00 Uhr." |
| Code kopiert | „Code kopiert." |
| Angemeldet | „Angemeldet. (name@beispiel.de)" |
| Stimme | „Stimmprobe aufgenommen. Alibaba erzeugt daraus deine Stimme." · „Stimme gelöscht." |
| Selbstbild | „Selbstbild gespeichert." |

> **Ein Hinweis läuft immer über dieselbe Stelle**, an der die Uhr hängt, die ihn nach 2600 ms
> wieder wegnimmt. Elf Stellen setzten ihn vorher direkt — ihre Hinweise blieben für immer
> stehen, auf **jedem** Bildschirm, weil der Hinweis der ganzen App gehört.
>
> **Hinweis und Störung sind zweierlei.** Der Hinweis (unten, 92 dp über der Kante) geht von
> allein. Die **Störung** (oben, 68 dp unter der Kante, mit „Nochmal") bleibt stehen, bis sie
> weggedrückt wird — eine Fehlermeldung, die nach zweieinhalb Sekunden verschwindet, hat Frank
> womöglich nie gelesen.

## 9. Barrierefreiheit

Die App ist ausschließlich für Frank. Es gelten keine Store-Vorgaben. Festgelegt wurde nur,
was sich aus der Gestaltung ergibt:

- **Mindest-Tippfläche 48 × 48 dp** für jedes bedienbare Element (folgt aus „luftig, große
  Tippflächen").
- **Kontrast:** Text auf Grund erreicht in beiden Erscheinungen mindestens 7:1
  (Dunkel: `#F4EEE7` auf `#151210` · Hell: `#1E1915` auf `#F8F4EE`). Gedämpfter Text
  mindestens 4,5:1.
- **Große Systemschrift** wird übernommen; Karten wachsen mit, Texte werden nie abgeschnitten.
- **Reduzierte Bewegung** wird beachtet — siehe Motion-Spec §8 und F-41. Meldet das System
  „Bewegung reduzieren", gilt mindestens die Effekt-Stufe *Gedämpft*, auch wenn *Voll*
  eingestellt ist.
- **Die sechs Felder der unteren Leiste** behalten trotz der zusätzlichen Spalte ihre
  Mindest-Tippfläche: 68 × 48 dp je Feld bei 412 dp Breite. Wird die Systemschrift sehr groß
  gestellt, entfällt zuerst die Beschriftung, nie das Symbol oder die Tippfläche.
- **Kein Effekt ist Bedingung für Bedienbarkeit.** Auf der Stufe *Aus* (F-41) ist jede
  Funktion vollständig erreichbar; kein Zustand wird ausschließlich durch Leuchten, Bewegung
  oder Farbe mitgeteilt.

---

## 10. Offene Fragen

- Design-Fakten stammen aus dem Spec-Paket (00-PROJEKT.md, 01-FUNKTIONS-SPEC.md, 02-UI-SPEC.md, 03-MOTION-SPEC.md), nicht aus Quellcode: die Software existiert noch nicht.
- Funktionen aus dem Spec — das ausloesende Bedienelement traegt data-werft-funktion mit dieser Kennung: F-01 = Lage einsprechen; F-02 = Text mit KI verbessern; F-03 = Fünf Vorschläge erzeugen; F-04 = Vorschläge aktualisieren; F-05 = Vorschlag auf die Merkliste legen; F-06 = Experiment auswählen und starten; F-07 = To-Do-Liste des Tages; F-08 = Aufgabe abhaken; F-09 = Gespräch zum Experiment; F-10 = Auswertung einsprechen; F-11 = KI-Auswertung erzeugen; F-12 = Auswertung vorlesen; F-13 = Experiment abschließen; F-14 = Logbuch fortschreiben; F-15 = Tagesverdichtung nach 15 Tagen; F-16 = Logbuch-Eintrag ändern oder löschen; F-17 = Erkenntnisse fortschreiben; F-18 = Merkliste: eigenes Experiment anlegen; F-19 = Merkliste: Eintrag löschen; F-20 = Wünsche & Ziele pflegen; F-21 = Selbstbild pflegen; F-22 = Modell und Effort wählen; F-23 = Stimme und Vorlesen einstellen; F-24 = Zugänge einrichten; F-25 = Erinnerungen einstellen; F-26 = Erscheinung umschalten; F-27 = Zwischen den Hauptbildschirmen wischen; F-28 = Lage tippen; F-29 = Auswertungstext bearbeiten; F-30 = Auswertung überspringen; F-31 = Logbuch, Reiter Letzte 15 Tage; F-32 = Logbuch, Reiter Langzeit; F-33 = Anlegen abbrechen; F-34 = Monitor sehen; F-35 = Monitor, eigenes Experiment anlegen; F-36 = Vorschlag in den Monitor übernehmen; F-37 = Experiment starten; F-38 = Reihenfolge im Monitor ändern; F-39 = Experiment aus dem Monitor nehmen; F-40 = Monitor-Karte auf- und zuklappen; F-41 = Effekt-Stärke einstellen
