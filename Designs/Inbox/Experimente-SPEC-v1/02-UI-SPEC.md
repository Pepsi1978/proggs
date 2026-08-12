# UI-Spec — Experimente-SPEC-v1
Stand: 12.08.2026, 12.06 Uhr · Stufe: v1, ueberarbeitet (Monitor + Effekte) · Plattform: Android (Kotlin / Jetpack Compose)


Alle Werte sind **deterministisch aus dem Design gemessen**, nicht geschätzt. Sie sind verbindlich.

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
| **B-10 NEU** | **Monitor (`B-10`)** | **ja** | **B-01, B-02, B-03, B-04, B-05, B-06, B-07, B-08** | **noch nicht gebaut — in beiden Erscheinungen zu gestalten** |
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
> **Wirklich noch nicht gebaut ist `B-10` (Monitor)** — er ist in dieser Fassung neu
> hinzugekommen und muss im Design in **beiden** Erscheinungen aufgebaut werden.

### B-10 NEU — Monitor

Startbildschirm: **ja** · Quelle: neu in dieser Fassung, im Design noch nicht aufgebaut

> **An den Gestalter:** Dieser Bildschirm ist neu und muss in **beiden Erscheinungen**
> aufgebaut werden. Er ersetzt B-01 als Startbildschirm. Die hier genannten Maße sind aus dem
> bestehenden Design abgeleitet (Seitenrand 20, Kartenradius 20, Innenabstand 21, Knopfhöhe
> 48, schwebende Leiste 12/24) und gelten, bis sie im Design gemessen wurden.

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
     Fortschrittsring (56 dp, Farbe `Aktion`, Leuchtspur)
   - aufgeklappt (F-40): vollständige Beschreibung, Aufgaben je Tag, Knopf **„Gespräch"**
     (→ B-02) und **„Wie ist es gelaufen?"** (→ B-03)
   - sind keine vorhanden: leerer Zustand, Satz siehe §8
4. **Die eine To-Do-Liste des Tages** (F-07), nach Experimenten gruppiert — unverändert
   übernommen von B-01, mit Haken-Effekt `E-17`
5. **Abschnitt „Steht an"**
   - je Experiment eine **Wartekarte**: gleicher Radius, Fläche `Fläche`, **ohne** Lichtsaum
     (nur Laufendes leuchtet), Deckkraft der Fläche 92 %
   - Inhalt: Titel · Stufe · Dauer · Herkunftsetikett · Knopf **„Starten"** (Höhe 48 dp,
     vollrund, Verlauf `Aktion`)
   - ziehbar (F-38), nach links wischbar (F-39)
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
| `LEER` | Beide Abschnitte leer, die Sätze aus §8, der Plus-Knopf atmet (`M-79`) |
| `NUR_ANSTEHEND` | Nur „Steht an"; „Läuft" ist ganz ausgeblendet, nicht als leere Hülle |
| `LAEUFT` | Beide Abschnitte, dazu die To-Do-Liste des Tages |
| `VOLL` | Drei laufen — alle „Starten"-Knöpfe gesperrt mit dem Hinweis aus F-37 |
| `ANLEGEN` | Die Anlegefläche liegt über dem Bildschirm, der Grund ist weichgezeichnet (`E-03`) |
| *lädt* | Schimmer-Skelette statt Karten (`E-13`) |

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
| `E-23` | **Haptik** — feste Muster: kurz (10 ms) beim Abhaken · doppelt beim Aufnahmebeginn und -ende (`M-03`) · aufsteigend beim Starten · lang-weich beim Abschließen · Fehler = zwei harte Stöße | `HapticFeedbackConstants` bzw. `VibrationEffect.createWaveform` | aus |

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
