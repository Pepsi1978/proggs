# Designentwürfe · Genialer Wecker

Vier Entwürfe für eine moderne Wecker- und Schlaf-App, jeder in Hell und Dunkel.
**Nativ umgesetzt seit 1.1.52:** **Schlicht** (das heutige Gold-/Glasdesign, Vorgabe),
**Morgenruhe (B)**, **Traumraum (D)** und **Orbit (C)** stehen in der App unter
Einstellungen → Darstellung → Design zur Wahl. **Nachtatelier (A) ist bewusst nicht übernommen**
und bleibt hier nur als Entwurf stehen. Diese Seiten dienen weiterhin als visuelle Vorlage.

Öffnen: `design/index.html` im Browser, oder über den lokalen Vorschauserver.

## Die vier Richtungen

Sie unterscheiden sich im **Strukturprinzip**, nicht nur in der Farbe — jeder Entwurf bliebe auch
in Graustufen als eigener erkennbar.

| | Richtung | Aufbau | Typografie | Farbwelt |
|---|---|---|---|---|
| **A** | Nachtatelier | redaktionell, einspaltig, Haarlinien statt Kästen | Newsreader (Serif) für Zeit und Titel | Marineblau / Elfenbein / Kupfer |
| **B** | Morgenruhe | der Tag als vertikale Achse, Karten als Stationen | Inter, große Zeilenhöhe | *Entwurf:* Salbei / Creme / Terrakotta — **in der App seit 1.1.55: Leinen / Tintenblau / Messing** |
| **C** | Orbit | Instrumententafel im festen Raster, Ring-Countdown, tabellarische Weckerzeilen | JetBrains Mono für alle Zahlen | Fast-Schwarz / Eisblau / Limette |
| **D** | Traumraum | skulptural: Kuppel, runde Perle, Editor als Bottom-Sheet | Inter, kräftig und rund | *Entwurf:* Pflaume / Rosé / Perlmutt — **in der App seit 1.1.55: warmes Schwarz / Glut-Orange / Bernstein** |

Der Hellmodus von A ist bewusst elfenbein-dominant und nicht das invertierte Dunkel, damit A und C
nicht als dasselbe gelesen werden.

## Abweichung der App von diesen Entwürfen (seit 1.1.55)

Die Seiten hier zeigen weiterhin den **ursprünglichen Entwurfsstand**. Zwei Farbwelten sind in der
App bewusst abgelöst worden; das Strukturprinzip, die Typografie und der Aufbau jeder Variante
bleiben unverändert:

* **Morgenruhe** trug grünstichige Flächen unter einem rostroten Primärton. Das ist ein
  Komplementärpaar ohne vermittelnden Zwischenton und wirkte schmutzig statt ruhig; das
  namensgebende Salbei war zudem nirgends Akzent, sondern nur Untergrund. In der App liegt jetzt
  neutrales Leinen unter tiefem Tintenblau, mit gedecktem Messing als warmem Gegenpunkt.
* **Traumraum** war Pflaume und Rosé. In der App trägt es jetzt ein *warmes* Schwarz (`#0E0B09`)
  mit klarem Orange (`#FF7A33`) und sparsamem Bernstein. Das Schwarz ist bewusst warm, damit es
  sich von Schlichts neutralem `#121212` und Orbits kühlem `#08090C` unterscheidet; das Orange ist
  deutlich rotstichiger als Schlichts Gold, damit Traumraum kein Schlicht-Klon wird.

Verbindlich sind die Werte in `app/src/main/java/de/frank/wecker/design/Paletten.kt`. Jede dort
gesetzte Kombination aus Text-, Primär- und Akzentfarbe auf ihrer Fläche ist gerechnet und liegt
über dem WCAG-AA-Wert von 4,5:1.

Ebenfalls seit 1.1.55 und in keinem Entwurf abgebildet: Die oberste Uhrenkarte steht in allen vier
Designs fest und scrollt nicht mehr mit; zugeklappte Weckerkarten sind innerhalb einer Ansicht
gleich hoch; Dialoge sowie Datums- und Uhrzeitwahl kommen aus der jeweiligen Farbwelt statt vom
System.

## Je Entwurf vier Seiten

Startseite · Editor · Einstellungen · Alarmbildschirm. Umschaltbar über die Leiste unten in der
Einzelansicht und über die Auswahl oben in der Vergleichsgalerie.

Die Inhalte verwenden die **echten Bezeichnungen der App** in Version 1.1.49 — „＋ Wecker",
„Wecker speichern", „Dein Weckablauf", „Alle X Tage", „Gewünschte Schlafdauer", „Weckbereitschaft",
„Vorlauf: 15 Min. vorher", „Ausrichtung", „Schlummern" / „Beenden", „Stimmvariante 3 von 6".

## Feste Momentaufnahme

Alle vier Entwürfe zeigen **denselben stillstehenden Moment**, damit Uhrzeit, Datum, Restzeit und
Weckerkarten zusammenpassen. Bewusst **keine** lebende Uhr neben statischen Beispieldaten.

- **Jetzt:** Freitag, 18.09.2026, 22:41
- **Frühschicht** · 07:00 · alle 35 Tage ab 29.03.2026 → nächster Termin **So, 20.09. · 07:00**,
  das sind 32 Std. 19 Min., in der Schreibweise der App „in 1 Tag 8 Std."
  Schlafenszeit bei 8 Std. Schlaf: Sa 23:00
- **Werktags** · 06:30 · Mo–Fr → nächster Termin **Mo, 21.09. · 06:30** (Freitag 06:30 ist vorbei),
  Schlafenszeit So 22:30
- **Wochenende** · 09:15 · Sa · So → ausgeschaltet
- Der **Alarmbildschirm** ist ausdrücklich ein anderer Moment und als Beispielmoment beschriftet.

Die Regel für die Zeitangabe oben (heute nur Uhrzeit, morgen Uhrzeit zuerst, ab übermorgen Datum
zuerst, bei anderem Jahr mit Jahreszahl) steht in den Einstellungen als **Beispielleiste** — sie
behauptet keinen aktuellen Zustand.

## Bedienung der Vorschau

Knöpfe reagieren, lösen aber nichts aus: „＋ Wecker" und „Einstellungen" wechseln die Demoseite,
„Wecker speichern", „Schlummern" und „Beenden" zeigen eine kurze Rückmeldung, die ausdrücklich
„Vorschau" sagt. Schalter und Regler ändern nur ihre Darstellung. **Keine echten Wecker-,
Benachrichtigungs- oder Geräteaktionen.**

## Technik

Reines lokales HTML/CSS/JS, kein CDN, kein Netzwerkzugriff, keine package.json, kein Build.
Die drei Schriften liegen als Kopien aus `app/src/main/res/font/` unter `gemeinsam/fonts/`.
Die Galerie bindet die Varianten als `<iframe>` ein und steuert sie ausschließlich über
URL-Parameter (`?seite=…&theme=…&breite=…`), weil unter `file://` kein Skriptzugriff über
iframe-Grenzen möglich ist.

Breiten zum Vergleich: **360** (Fold außen), **412** (Handy), **840** (Fold innen).

## Bildmotive

Vier dekorative PNG unter `assets/`, erstellt mit `imagegen`; die Prompts und die Herkunft stehen
in `assets/PROMPTS.md`. Sie sind **ausschließlich Illustration neben Text** — jedes Bedienelement
ist echtes HTML. Alle vier sind quadratisch komponiert und werden als kleines, zurückhaltend
gerahmtes Motivfeld gezeigt; im Hellmodus bleibt ein dunkles Motiv als Feld stehen. Fehlt eine
Datei, zeichnet CSS eine Ersatzfläche, damit das Layout trotzdem beurteilbar ist.

## Offen

- Nichts offen: Schlicht, B, D und C sind in **1.1.52** nativ umgesetzt, A bleibt ausgenommen.
  Die Entwürfe hier bleiben als Vorlage und Vergleichsgrundlage erhalten.
- Der Anzeigefehler bei Monatsweckern (Kartenüberschrift ohne Datum) ist in **1.1.51** behoben;
  Einzelheiten im Projekt-README. Die Vorschauen hier zeigen die Regel bereits richtig.
