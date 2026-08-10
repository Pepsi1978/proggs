# Design zu Code: wie ein Entwurf verlustfrei ankommt

Stand: 10.08.2026 · Recherche über Engine A (Firecrawl + MiniMax), 6 Researcher, 29 Quellen
Anlass: Beim eigenen Werkzeug (`~/proggs/WerftStudio/`) kam ein Bildschirm nicht 1:1 im Code an.
Siehe `Specs/BEFUND-messung-unzuverlaessig.md` für den konkreten Fall.

---

## Kurzcheck (Stufe A — vor jeder Design-zu-Code-Arbeit lesen)

1. **Struktur übergeben, nicht Koordinaten.** Wer von einem Bild oder von Messwerten arbeitet,
   verliert Abstände, Schriftgewichte und Details. Wer von der Layout-**Hierarchie** arbeitet,
   verliert sie nicht. Belegt: Builder.io (Dateistruktur) 4/5 gegen v0 (Screenshot) 3/5.
2. **Eine Koordinate gilt nur für die Breite, bei der gemessen wurde.** Eine Hierarchie gilt
   immer. „Beschriftung über dem Feld" bleibt wahr, `x=156` nicht.
3. **Beim Messen die Breite VOR dem Laden setzen.** `setViewportSize` feuert kein `resize`
   (Playwright-Issue #36084, vom Maintainer als dokumentiert bestätigt) — nachträglich gesetzte
   Breiten können alte Layoutwerte liefern.
4. **Zoom auf 100 % festnageln.** Bei Zoom ≠ 100 % verschieben sich Breakpoints proportional.
5. **An jedem Breakpoint messen**, nicht an einem. Ein responsiver Entwurf hat mehrere gültige
   Anordnungen.
6. **Gegenprobe per Pixel-Diff** gegen eine Baseline, mit Toleranzschwelle — Standardtechnik,
   kein Luxus.

---

## 1. Wie Claude Design es macht — das „Handoff Bundle"

Claude Design (Anthropic) packt den Entwurf beim Export in ein **Handoff Bundle**, das mit einer
einzigen Anweisung an Claude Code übergeben wird.

> „Claude packages everything into a handoff bundle that you can pass to Claude Code with a
> single instruction." — Anthropic Labs, *Introducing Claude Design*

**Inhalt des Bündels:**

| Bestandteil | Was es leistet |
|-------------|----------------|
| Komponentenstruktur als **maschinenlesbare Spezifikation** | sagt, *woraus* der Bildschirm besteht |
| die **tatsächlich verwendeten** Design-Tokens | keine Token-Bibliothek, sondern was auf der Canvas benutzt wurde |
| die **Layout-Hierarchie** | sagt, *wie* die Teile zueinander stehen — breitenunabhängig |
| **Asset-Referenzen** | Bilder und Schriften als Verweis, nicht als Kopie |

**Was es ausdrücklich nicht ist** — diese Abgrenzung ist der Kern:

> „Not a PNG. Not a Figma URL that requires a plugin. A spec file Claude Code can read directly."
> „The format is not a standards-committee compromise like design tokens in JSON. It's whatever
> works best between two models from the same lab."

Und: **„not a lossy export"** — keine verlustbehaftete Übergabe.

Ergänzend gibt es `/design-sync`, das in **beide** Richtungen läuft: Entwurf ins Repo holen, oder
Gebautes zurück auf die Canvas schieben.

**Nicht öffentlich belegbar:** Dateiendung, Schema, ob eine oder mehrere Dateien, ob versioniert.
Das Format ist proprietär. Nachahmen lässt sich das **Prinzip**, nicht die Datei.

---

## 2. Der empirische Beleg: Struktur schlägt Momentaufnahme

Vergleich von Design-zu-Code-Werkzeugen (Vibe Coder Blog, April 2026):

| Werkzeug | Output-Qualität | Eingabe | Befund |
|----------|-----------------|---------|--------|
| **Builder.io** | **4/5** | Figma-**Dateistruktur** | „Excellent for structure and layout when Figma files are well-organized, weaker on complex interactions." Nutzt Layers, Auto-Layout, Varianten, Tokens |
| **Anima** | 3/5 | Figma-Datei | gute visuelle Treue plus Interaktion; Code teils „verbose" |
| **v0** (Vercel) | 3/5 | **Screenshot** | „It often misses exact spacing values, specific font weights, and subtle interaction details that your blueprint specifies precisely." |

**Die Lehre:** Der Qualitätsunterschied liegt nicht am Modell, sondern an der **Eingabe**. Ein
Screenshot ist eine Ableitung — und eine Messung ist es genauso. Beide verlieren dasselbe:
Abstände, Gewichte, Details.

Einschränkung von Builder.io selbst: „garbage in, garbage out" — die Treue hängt daran, wie
ordentlich die Quelldatei aufgebaut ist. Struktur hilft nur, wenn sie sauber ist.

---

## 3. Figmas Weg: nicht übersetzen, sondern zuordnen

Figma **Code Connect** verbindet Design-Komponenten mit echtem Code im Repository. Statt
generiertem Code erscheint die **Produktionskomponente**:

> „When inspecting a component with connected code snippets, developers will see design system
> code from their libraries instead of auto-generated code."

Was übergeben wird:

- **echte Code-Snippets** aus dem eigenen Design-System
- **Komponentenpfade** und **Komponentennamen** im Repo
- **Property-Mappings**: Eigenschaften im Code werden Eigenschaften in Figma zugeordnet, damit
  die Snippets dynamisch und korrekt sind
- **plattformeigene Variablen-Syntax** (Web, iOS, Android) — der Entwickler sieht die Variable so,
  wie sie in *seiner* Sprache heißt
- dieselben Metadaten gehen über den **MCP-Server an KI-Werkzeuge**

**Das stärkste Prinzip daraus:** Die genaueste Übergabe ist die, bei der gar nichts übersetzt
wird — weil das Ziel schon existiert und der Entwurf nur darauf zeigt.

---

## 4. Messen, wenn es sein muss — richtig

Messen bleibt nützlich: es löst Vererbung, `var()` und `color-mix()` auf. Aber es ist ein
**Hilfsmittel**, nie die einzige Wahrheit.

**Falle 1 — Viewport nachträglich gesetzt.** Playwright-Issue #36084:

> „`setViewportSize` changes the size of the visible screen but does NOT fire resize event. This
> causes the app to continue rendering outside of the visible area in the original size advertised
> to an app at the time of the start."

Der Maintainer bestätigt das als dokumentiertes Verhalten. **Folge:** Breite beim Erzeugen des
Kontexts setzen (`browser.newContext({ viewport })` bzw. `viewport` in der Konfiguration), nicht
per `setViewportSize` nach dem Laden. Wird sie doch nachträglich gesetzt, danach ein Relayout
erzwingen und abwarten.

**Falle 2 — Browser-Zoom.** Bei Zoom ≠ 100 % feuern Media Queries proportional verschoben
(1200 px bei 110 % → 1320 px). Ein Messfühler muss den Zoom explizit setzen. Zusatzhinweis aus
den Quellen: Media Queries in `em`/`rem` statt `px` sind zoomfest — in den Quellen allerdings
umstritten.

**Falle 3 — nur eine Breite gemessen.** Ein responsiver Entwurf hat mehrere gültige Anordnungen.
An jeder Breite messen, an der eine Media Query greift, und die Breite zum Messwert dazuschreiben.

---

## 5. Die Gegenprobe: visuelle Regression

Etabliertes Verfahren, in allen Quellen gleich beschrieben:

1. **Baseline** aufnehmen — der Zustand, von dem man weiß, dass er richtig ist (hier: der Entwurf).
2. **Neuer Screenshot** aus dem Gebauten.
3. **Pixelweiser Vergleich** mit **konfigurierbarer Toleranz** — Werkzeuge: `pixelmatch`,
   `Resemble.js`, Playwright `toHaveScreenshot`, BackstopJS, Percy, Chromatic.
4. **Abweichung als Prozentsatz**, Diff-Überlagerung zeigt die Stellen.
5. Prüfung **über mehrere Breiten und Erscheinungen** hinweg, in CI eingebunden.

Erkannt werden damit genau die Fehler, die sonst durchlaufen: Layout-Brüche, Farbverschiebungen,
Schriftprobleme, Ausrichtungsfehler.

---

## 6. Was daraus für WerftStudio folgt

| Nr | Maßnahme | Woraus abgeleitet |
|----|----------|-------------------|
| 1 | **Hierarchie exportieren, nicht nur Koordinaten.** Das Bündel braucht den Elternbaum mit Anordnungsart (Spalte/Zeile/Raster) je Bauteil — Koordinaten nur zusätzlich, mit der Breite, bei der sie gelten | §1, §2 |
| 2 | **Die tatsächlich benutzten Tokens mitgeben**, nicht die ganze Bibliothek | §1 |
| 3 | **Renderbreite in jede Bildschirmdatei schreiben**, damit der Export überall so aussieht wie im Designer | §4, Falle 3 |
| 4 | **Messfühler:** Breite beim Kontext-Erzeugen setzen, Zoom auf 100 %, an jedem Breakpoint messen | §4 |
| 5 | **Pixel-Diff als Abnahmetor** — Entwurf gegen Gebautes, je Breite und Erscheinung, mit Toleranz | §5 |
| 6 | **Zuordnen statt übersetzen:** wo eine App schon Bauteile hat, sollte der Entwurf auf sie zeigen, statt neue erzeugen zu lassen | §3 |

---

## Quellen

29 Quellen über 6 Researcher. Kernbelege: Anthropic Labs *Introducing Claude Design* · claudefa.st
zum Handoff Bundle · dev.to zu `/design-sync` · Vibe Coder Blog 04/2026 (Werkzeugvergleich mit
Bewertung) · Figma Guide to Dev Mode + Code Connect-Doku · Playwright-Doku (Browser, ElementHandle,
Emulation) + GitHub-Issue microsoft/playwright#36084 · Stack Overflow zu Media Queries und Zoom ·
QA Wolf, Virtuoso QA, Pcloudy zu visueller Regression.

Rohdaten des Laufs: `~/.research-swarm/answer-1.txt` bis `answer-6.txt` (28,8 KB).
