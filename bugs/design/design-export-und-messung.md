# Bug-Almanach: Design-Export und Layout-Messung

Stand: 10.08.2026 · Bereich: Design-zu-Code-Pipelines, HTML-Export, Layout-Vermessung
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
