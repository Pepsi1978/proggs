# Bug-Almanach: Design-Export und Layout-Messung

Stand: 10.08.2026, 22:05 · Bereich: Design-zu-Code-Pipelines, HTML-Export, Layout-Vermessung
Gegenseite: `best-practices/design/design-zu-code-treue.md`

---

## Kurzcheck (Stufe A — vor Arbeit an Export oder Messfühler lesen)

| Nr | Falle | Sofort-Regel |
|----|-------|--------------|
| B-01 | Media Query greift beim Betrachten anders als im Designer | Renderbreite in die exportierte Datei schreiben |
| B-02 | `setViewportSize` feuert kein `resize` | Breite beim Kontext-Erzeugen setzen, nicht danach |
| B-03 | Browser-Zoom ≠ 100 % verschiebt Breakpoints | Zoom explizit auf 100 % festnageln |
| B-04 | Messung bei einer Breite, Entwurf ist responsiv | An jedem Breakpoint messen, Breite zum Messwert schreiben |
| B-05 | Messung gilt als einzige Wahrheit | Gegenprobe gegen HTML-Baum und kaskadierte CSS |
| B-06 | Absolute Koordinaten als App-Layout | Hierarchie bauen, Koordinaten nur als Nachweis |
| **B-07** | **`@import` steht nach anderen Regeln → Browser verwirft ihn STILL → Design rendert in Systemschrift** | **Schriften als `<link rel="stylesheet">` in den Kopf, nie als `@import`** |
| **B-08** | **Layout-Ableitung liest nur `.klasse`-Selektoren → verliert die gewinnende CSS-Schicht und alle Inline-Stile** | **Wie der Browser abgleichen: Vorfahren-Kette, Attribut-/Tag-Selektoren, Spezifität, `style`-Attribut** |
| **B-09** | **Skalierte Vorschau zeigt am Rand eine helle Linie von einem Pixel** | **Fläche unter dem Rahmen in der Farbe des Designs färben und einen gleichfarbigen 1px-Saum legen (Chromium 600120)** |
| **B-10** | **Runde Ecken der Bühne lassen die Fläche darunter durchscheinen** | **Bei skaliertem Kind clippt `border-radius`+`overflow:hidden` nicht zuverlässig — Rundung weglassen oder `isolation:isolate` setzen (WebKit 98538/77572/72619)** |
| **B-11** | **`backdrop-filter` bleibt wirkungslos, obwohl es im CSS steht** | **Kein `filter`, `opacity<1`, `mask`, `mix-blend-mode` oder `will-change` auf einem VORFAHREN; `-webkit-`-Präfix mitschreiben** |
| **B-12** | **Effekt steht im Faktenblatt, taucht im Aufbau aber nicht auf** | **Effekt-Namen müssen ihren ORT tragen (`.karte (shadow)`); ein Wert ohne Ort wird weggelassen statt geraten** |
| **B-13** | **Jeder Bildschirm baut die Reiterleiste anders** | **Bildschirme entstehen in getrennten Aufrufen — gemeinsame Leisten festschreiben UND nach dem Zusammensetzen programmatisch vergleichen** |

---

## B-01 — Der Export sieht anders aus als der Designer

**Symptom:** Im Design-Werkzeug steht die Beschriftung über dem Feld. Öffnet man die exportierte
HTML-Datei, steht sie daneben. Niemand hat etwas geändert.

**Ursache:** Die richtige Anordnung hängt an einer Media Query (`@media (max-width: 480px)`).
Media Queries fragen die **Fensterbreite**, nicht die Breite des Elements. Die Designer-Vorschau
steckt in einem schmalen Geräterahmen — dort greift die Regel. Die exportierte Datei hat keinen
Rahmen und trägt keine Angabe, bei welcher Breite sie zu rendern ist; im Browserfenster greift
die Regel deshalb nicht.

**Real getroffen:** Werft Studio, 10.08.2026. Der Einstellungen-Bildschirm einer Android-App kam
falsch im Code an, weil die Messung diese falsch gerenderte Variante gelesen hat und danach als
verbindlich galt. Kette: Export → Messung → Spec → Code → Gerät, ohne eine einzige Fehlermeldung.

**Funktionserhaltender Fix:** Die Renderbreite in jede exportierte Bildschirmdatei schreiben —
als Rahmen mit fester Breite oder als maschinenlesbare Angabe im Kopf, an die sich Betrachter und
Messfühler halten. Zusätzlich beim Export selbst prüfen: bei Zielbreite rendern und mit der
Designer-Vorschau vergleichen; bei Abweichung melden statt ausliefern.

---

## B-02 — `setViewportSize` löst kein `resize`-Event aus

**Symptom:** Ein Messskript setzt die Viewport-Breite und liest danach Layoutwerte — es kommen
Werte heraus, die zur alten Breite gehören. Auch Elemente außerhalb des sichtbaren Bereichs sind
zeitweise nicht anklickbar.

**Ursache:** Dokumentiertes Playwright-Verhalten (Issue microsoft/playwright#36084, vom
Maintainer als „documented behavior" bestätigt):

> „`setViewportSize` changes the size of the visible screen but does NOT fire resize event. This
> causes the app to continue rendering outside of the visible area in the original size advertised
> to an app at the time of the start."

**Betroffen:** Playwright (bestätigt, u. a. Regression in 1.51.1), im Grundsatz jedes
CDP-basierte Werkzeug, das die Größe nach dem Laden ändert.

**Fix:** Breite beim **Erzeugen** des Kontexts setzen — `browser.newContext({ viewport: {…} })`
oder `viewport` in der Konfiguration, alternativ `test.use({ viewport })`. Muss sie doch später
gesetzt werden: Relayout erzwingen und abwarten, bevor gemessen wird.

---

## B-03 — Browser-Zoom verschiebt die Breakpoints

**Symptom:** Eine Media Query für `max-width: 1200px` greift erst bei 1320 px.

**Ursache:** Zoom ≠ 100 % skaliert die CSS-Pixel: 1200 × 1,10 = 1320.

**Fix:** Im Messfühler den Zoom explizit auf 100 % setzen (`Ctrl+0` beim Menschen). Ein Hinweis
aus den Quellen: Media Queries in `em`/`rem` sind zoomfest — die Empfehlung ist dort aber
umstritten, also nicht ungeprüft übernehmen.

---

## B-04 — Eine Breite gemessen, Entwurf ist responsiv

**Symptom:** Alles stimmt auf einem Gerät und verrutscht auf einem anderen.

**Ursache:** Eine Messung ist eine Momentaufnahme bei genau einer Breite. Die Umbruchregeln des
Entwurfs sind darin nicht enthalten — sie sind aufgelöst und damit verloren.

**Fix:** An **jeder** Breite messen, an der eine Media Query greift, und die Breite zu jedem
Messwert dazuschreiben. Die Zielbreite gegen die Bezugsgröße des Bauauftrags prüfen und bei
Abweichung anhalten.

---

## B-05 — Die Messung als einzige Wahrheit

**Symptom:** Der Build ist grün, alle Abhaklisten sind abgehakt, das Ergebnis ist trotzdem ein
anderer Bildschirm. Es gibt keine Fehlermeldung, an der man ansetzen könnte.

**Ursache:** Eine Regel wie „bei Widerspruch gilt die Messung" macht eine falsche Messung
unangreifbar. Der Fehler wird durch alle Stufen getragen, statt aufzufallen.

**Fix:** Rangfolge umdrehen — bei Widerspruch gewinnt die **Quelle** (HTML-Baum plus kaskadierte
CSS), weil die Messung aus ihr abgeleitet ist. Die Messung bleibt als Hilfsmittel (löst Vererbung,
`var()`, `color-mix()` auf), aber jede Ableitung braucht eine Gegenprobe.

---

## B-06 — Absolute Koordinaten als Layout der App

**Symptom:** Der erzeugte Bildschirm ist maßhaltig zur Vorlage und verrutscht auf jedem Gerät,
dessen Breite nicht der Messbreite entspricht.

**Ursache:** Koordinaten (`x`, `y`, `breite`, `hoehe`) gelten nur für die Breite, bei der gemessen
wurde. Als Nachweis sind sie richtig, als Layout falsch.

**Belegt durch den Werkzeugvergleich:** Wer von Bild oder Messwert arbeitet, verliert Abstände und
Schriftgewichte (v0, Screenshot-basiert: 3/5); wer von der Dateistruktur arbeitet, ist genauer
(Builder.io: 4/5). Claude Design übergibt darum eine **Layout-Hierarchie** und nennt sein Format
ausdrücklich „not a lossy export".

**Fix:** Aus der Messung die **Hierarchie** ableiten (Elternbaum, Anordnungsart, Reihenfolge) und
danach bauen. Koordinaten nur zum Nachweis behalten — nicht zum Positionieren.

---

## B-07 — Der `@import` steht zu spät, und die Schriften kommen nie an

**Symptom:** Der Entwurf sieht in einer eigenen Schrift aus (etwa Fraunces, Inter, JetBrains Mono),
gerendert erscheint aber überall die Systemschrift. Keine Warnung, keine Fehlermeldung, kein
Eintrag in der Browser-Konsole. Die Schriftnamen stehen korrekt im CSS und werden auch korrekt
gemessen — nur sichtbar wird keine von ihnen.

**Ursache:** Eine `@import`-Regel ist nur gültig, wenn sie **vor allen anderen Regeln** steht.
Normativ, W3C CSS Cascade Level 5:

> „Any @import rules must precede all other valid at-rules and style rules in a style sheet
> (ignoring @charset, @supports-condition, and @layer statement rules) … must not have any other
> valid at-rules or style rules between it and previous @import rules, or else the @import rule is
> invalid."

Und MDN dazu unmissverständlich:

> „As the `@import` at-rule is declared after the styles it is invalid and hence ignored."

**Real getroffen:** Werft Studio, 10.08.2026. Der `@import` auf Google Fonts stand an Zeichen 6427
seines `<style>`-Blocks — 6425 Zeichen Regeln davor. Betroffen war nicht nur der Export: schon die
Studio-Vorschau und die `design.html` rendern damit in der Systemschrift. Verschärfend kommt hinzu,
dass beim Zusammenführen mehrerer `<style>`-Blöcke zu **einer** `design.css` ein `@import` fast
zwangsläufig hinter eine andere Regel rutscht — der Fehler entsteht dort also von selbst.

**Zweite Wirkung, die leicht übersehen wird:** Wird mit der falschen Schrift gerendert, sind auch
alle **gemessenen Textmaße falsch** (Breiten, Höhen, Zeilenumbrüche), weil die Fallback-Schrift
andere Metriken hat. Der Fehler pflanzt sich also in jede abgeleitete Koordinate fort.

**Funktionserhaltender Fix:**
1. Schriften als **Kopf-Verweis** ausliefern, nicht als `@import` —
   `<link rel="preconnect" href="https://fonts.googleapis.com">`,
   `<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous">`,
   `<link rel="stylesheet" href="…">`. Genau so macht es das echte Übergabepaket von Claude Design
   (nachgeprüft an zwei echten `.dc.html`-Bündeln), und web.dev empfiehlt es ausdrücklich, weil ein
   `@import` das Rendern zusätzlich blockiert.
2. Als Sicherheitsnetz beim Zusammenführen von Stylesheets alle `@import`-Regeln **nach vorn
   ziehen** (verlustfrei, die Kaskade der übrigen Regeln bleibt unberührt — ein `@import` nimmt
   ohnehin nicht als Stilregel an ihr teil).
3. Je Schriftfamilie die **Herkunft** ins Übergabepaket schreiben (Verzeichnis mit URL und
   Schnitten / eingebettet / keine Quelle). Eine Familie ohne Quelle wird **benannt**, nicht
   stillschweigend hingenommen.

**Falle beim Fix selbst:** Die URL eines Font-Imports enthält eigene Semikolons
(`family=Inter:wght@400;500;600`). Ein Muster, das bis zum ersten `;` liest, schneidet sie entzwei
und hinterlässt einen kaputten Import. Erst die Klammer bzw. Zeichenkette schließen, dann das
abschließende Semikolon suchen.

---

## B-08 — Die Layout-Ableitung liest nur Klassen-Selektoren

**Symptom:** Der erzeugte Bauplan (bzw. die abgeleitete Anordnung) ist an vielen Stellen leer oder
falsch: eine Karte hat keinen Innenabstand, eine Spalte gilt als Fluss, ein Raster fehlt. Im
Browser sieht derselbe Bildschirm völlig richtig aus.

**Ursache:** Die Ableitung verwirft jeden Selektor, der `[`, `>` oder `:` enthält, und liest nur
solche, deren letztes Glied ein reiner Klassen-Selektor ist. Damit fällt genau die Schicht heraus,
die im Browser **gewinnt** — die spezifischere. Besonders bitter, wenn das eigene Werkzeug seine
Regeln selbst so schreibt: Werft Studio scopet Bildschirm-Regeln als
`.werft-screen[data-screen-id="B-08"] .werft-b08__section { padding: 20px }`.

**Gemessen:** An einem echten Werft-Export gingen **42 von 371** Layout-Angaben verloren (11,3 %),
davon 28 wegen Attribut-Selektoren und 14 wegen reiner Tag-/ID-Selektoren.

**Noch drastischer bei einem Claude-Design-Paket:** Dort steht das Layout in `style`-Attributen am
Element. Nachgezählt an einem echten Bündel: **234 `style`-Attribute** (66 mit `display`, 72 mit
`padding`, 45 mit `gap`, 17 mit `flex-direction`) und **keine einzige CSS-Klasse**. Eine Ableitung
ohne Inline-Stile ist dort vollständig leer — und niemand merkt es, weil kein Fehler auftritt.

**Fix:** Wie der Browser abgleichen, für eine klar benannte Teilmenge:
- die **Vorfahren-Kette** mitführen und Selektoren von rechts nach links prüfen
  (Nachfahre, direktes Kind `>`);
- **Attribut-Selektoren** auswerten (`=`, `^=`, `$=`, `*=`, `~=`, `|=`, bloßes Vorhandensein);
- **Tag- und ID-Anteile** prüfen, nicht nur Klassen;
- `:is()` / `:where()` in Alternativen auflösen, `:not()` prüfen;
- nach **Spezifität** und dann Reihenfolge sortiert anwenden;
- den **Inline-Stil** über alles aus dem Stylesheet stellen, `!important` über alles;
- **Zustands-Pseudoklassen** (`:hover`, `:active`, `:focus`, `:checked`, `::before` …) weiter
  bewusst **ausschließen** — sie beschreiben einen Zustand, nicht den Grundaufbau. Sonst steht die
  Hover-Anordnung im Bauplan als die gewöhnliche.
- Geschwister-Kombinatoren (`+`, `~`) brauchen eine Reihenfolge-Information, die ein reiner
  Elternbaum nicht mitführt: überspringen statt falsch raten.

---

## B-09 — Die skalierte Vorschau zeigt eine helle Linie am Rand

**Symptom:** Ein Design steht in einem iframe fester Größe auf einer Bühne, die per
`transform: scale()` zoomt. An einer oder zwei Kanten — mal links, mal rechts, mal unten — liegt eine
dünne helle Linie, die nicht zum Design gehört. Welche Kante es trifft, wechselt mit dem Zoomfaktor.

**Ursache:** Belegt als Chromium-Fehler **600120**. Bei einem gebrochenen Maßstab fällt die Kante des
Rahmens zwischen zwei Bildschirmpunkte. Der Rand der Ebene wird halbdurchsichtig gezeichnet, und die
Fläche darunter scheint durch. Ist diese Fläche weiß, sieht man eine weiße Linie.

**Real getroffen:** Werft Studio, 14.08.2026. Die Bühne trug fest `background: #fff`; bei jedem
Referenzgerät lag an einer anderen Kante ein heller Strich.

**Funktionserhaltender Fix (zwei Schichten):**
1. Die Fläche unter dem Rahmen bekommt die Farbe, die das Design an seinem Rand wirklich zeichnet
   (aus der Vorschau melden lassen: `getComputedStyle(...).backgroundColor` von Bildschirm, `body`,
   `documentElement` — die erste deckende gewinnt). Dann ist der halbdurchsichtige Rand unsichtbar.
2. Zusätzlich ein `box-shadow: 0 0 0 1px <dieselbe Farbe>` als Saum. Er verlängert die Fläche um
   einen Punkt nach außen, ohne das Layout zu verändern, und deckt jeden Rest zu.

Ergänzend helfen `isolation: isolate` und `backface-visibility: hidden` am Rahmen (eigene Ebene).
Die Größe zu runden hilft nur, wenn man den Zoomfaktor selbst bestimmt.

**Quellen:** https://bugs.chromium.org/p/chromium/issues/detail?id=600120 ·
https://bugs.webkit.org/show_bug.cgi?id=133801 (scale + iframe: unscharfes Rendering)

---

## B-10 — Runde Ecken lassen die Fläche darunter durchscheinen

**Symptom:** Der Vorschau-Rahmen hat `border-radius` und `overflow: hidden`. In den Ecken sieht man
trotzdem helle Zipfel — das Design wird dort nicht sauber beschnitten.

**Ursache:** Ein Kind mit eigener Zeichenebene (ein iframe, ein transformiertes Element) wird vom
gerundeten Beschnitt des Elternteils nicht zuverlässig erfasst. Vier WebKit-Fehler beschreiben das:
**98538**, **231360** (gefixt, bestätigt 09.03.2023), **77572**, **72619**.

**Fix, nach Wirksamkeit geordnet:** `isolation: isolate` am Rahmen (browserübergreifend, ohne
GPU-Kosten) · `border-radius: inherit` am Kind · `mask-image` statt `overflow` · bei einer Vorschau,
die das Design ohnehin randlos zeigen soll, die Rundung ganz weglassen — dann gibt es keine Ecke, an
der etwas durchscheinen könnte.

**Quellen:** https://bugs.webkit.org/show_bug.cgi?id=98538 · .../231360 · .../77572 · .../72619

---

## B-11 — `backdrop-filter` steht im CSS und wirkt trotzdem nicht

**Symptom:** Eine Glasleiste hat `backdrop-filter: blur(12px)`, bleibt aber vollkommen klar.

**Ursache:** Der Effekt sammelt seinen Hintergrund bis zum sogenannten Backdrop-Root. Setzt ein
**Vorfahre** `filter`, `opacity` unter 1, `mask`, `mix-blend-mode`, `will-change` oder selbst
`backdrop-filter`, endet die Sammlung dort — und es bleibt nichts zu verwischen. `transform` auf einem
Vorfahren erzeugt zusätzlich einen neuen Bezugsrahmen und schneidet den Hintergrund ab.

**Weitere belegte Fallen:**
- Chromium ignoriert **verschachtelte** `backdrop-filter` im Kind-Element (Firefox und Safari nicht).
- Firefox-Fehler **1797051**: `filter` auf einem Vorfahren macht das Kind ganz unsichtbar.
- Chromium **380416865**: fehlerhafter Blur bei nicht-ganzzahlig transformierten Vorfahren.
- Safari braucht 2026 weiterhin `-webkit-backdrop-filter`; **lightningcss #537** entfernt das Präfix
  fälschlich beim Bündeln.

**Fix:** Das Glaselement braucht eine eigene halbdurchsichtige Fläche; die genannten Eigenschaften
dürfen auf keinem Vorfahren stehen (notfalls auf ein Geschwisterelement auslagern). Präfix
mitschreiben und nach dem Bündeln nachprüfen, dass es noch da ist.

**Quellen:** https://bugzilla.mozilla.org/show_bug.cgi?id=1797051 ·
https://issues.chromium.org/issues/380416865 · https://havn.blog/2024/03/14/chromium-and-nested.html ·
https://github.com/parcel-bundler/lightningcss/issues/537

---

## B-12 — Der Effekt steht in den Fakten und kommt im Aufbau nicht an

**Symptom:** Das Spec führt Schatten und Verläufe sauber auf. Im nachgebauten Design fehlen sie.

**Ursache:** Der Effekt wurde mit einem laufenden Zähler benannt (`design.html:gradient(7)`). Der
Wert ist da, aber niemand weiß, an welches Element er gehört. Was sich nicht zuordnen lässt, wird
weggelassen — lautlos, ohne Fehlermeldung.

**Real getroffen:** Werft Studio, 14.08.2026 (Experimente-Spec).

**Fix:** Beim Auslesen den Ort mitschreiben: den CSS-Selektor, in dessen Block der Effekt steht, oder
bei einer Angabe am Element dessen Tag und Klasse (`div.karte.gross (shadow)`). Die Bau-Anweisung
verpflichtet zusätzlich darauf, dass kein gelieferter Effekt fehlen darf.

---

## B-13 — Jeder Bildschirm baut die gemeinsame Leiste anders

**Symptom:** Nach dem Einlesen unterscheidet sich die Reiterleiste von Bildschirm zu Bildschirm:
andere Höhe, andere Symbolgrößen, andere Schriftgrößen, andere Effekte. Beim Reiterwechsel wirkt
es wie eine andere Anwendung.

**Ursache:** Jeder Bildschirm entsteht in einem eigenen KI-Aufruf, der die anderen nicht sieht. Ohne
ausdrückliche Auflage erfindet jeder Aufruf die wiederkehrenden Teile neu. Das MobileForge-Paper
benennt Cross-Page-Consistency als größte Schwachstelle solcher Verfahren.

**Fix (drei Schichten):**
1. Die gemeinsamen Teile werden EINMAL festgelegt und in jeden Aufruf wörtlich mitgegeben — nicht
   pro Bildschirm neu beschrieben.
2. Die Bau-Anweisung verpflichtet ausdrücklich auf zeichengleiche Leisten; unterscheiden darf sich
   nur, welcher Eintrag aktiv ist.
3. Nach dem Zusammensetzen vergleicht ein **programmatischer** Prüfer die Leisten aller Bildschirme
   (Höhe, Innenabstand, Hintergrund, Radius, Schrift- und Symbolgrößen, Beschriftungen) gegen den
   Startbildschirm. Die Abweichungen gehen in den vorhandenen Korrekturlauf. Eine Anweisung allein
   genügt nicht — kein Aufruf kann prüfen, was die anderen getan haben.

**Quellen:** MobileForge/„Looks Right, Works Right" (arXiv 2607.28645) ·
https://christinevallaure.substack.com/p/design-system-contracts-the-component
