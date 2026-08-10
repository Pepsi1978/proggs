# Design zu Code: wie ein Entwurf verlustfrei ankommt

Stand: 10.08.2026, 22:05 · Zwei Läufe: Engine A (Firecrawl + MiniMax, 6 Researcher, 29 Quellen)
und Engine C (Sonnet-5-Schwarm, 7 Researcher, ~80 Quellen) — plus, am wertvollsten, die
**Untersuchung zweier echter Claude-Design-Download-Pakete** (siehe §1a).
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
7. **Schriften als `<link rel="stylesheet">` in den Kopf, NIE als `@import`.** Ein `@import` ist
   nur gültig, wenn er vor allen anderen Regeln steht; sonst verwirft ihn jeder Browser still und
   das Design rendert in der Systemschrift. So macht es auch Claude Design. Details: Almanach B-07.
8. **Layout wie der Browser ableiten, nicht nur aus Klassennamen.** Attribut- und Tag-Selektoren,
   Vorfahren-Kette, Spezifität und besonders das `style`-Attribut gehören dazu — bei einem
   Claude-Design-Paket steckt das Layout praktisch vollständig inline. Details: Almanach B-08.
9. **Die Schrift ist der auffälligste Unterschied.** Stimmen alle Farben und Abstände, aber die
   Schrift ist ersetzt, wirkt das Ergebnis sofort fremd. Herkunft je Familie mitgeben.

---

## 1a. Was ein Claude-Design-Download WIRKLICH enthält (empirisch, nicht recherchiert)

Zwei echte Pakete untersucht (10.08.2026). Das ist der härteste verfügbare Beleg — Anthropic hat
das Format nicht dokumentiert, aber ein ausgepacktes Exemplar lügt nicht.

**Normalfall (frisch heruntergeladen, Android-Entwurf, 7 Einträge):**

```
App.dc.html          3 KB   Einstiegsdatei: Rahmen, Titel, Theme-Vergleich
Phone.dc.html       59 KB   der eigentliche Bildschirm
support.js          66 KB   die Laufzeit, die .dc.html interpretiert
android-frame.jsx   10 KB   der Geräterahmen
.thumbnail          15 KB   Vorschaubild
v1/App.dc.html             ← VERSIONIERT: frühere Fassung im Unterordner
v1/Phone.dc.html
```

**Was ausdrücklich NICHT drin ist:** kein `design-tokens.json`, keine Messung, kein Bauplan, kein
Spec, kein README. Das ist die zentrale Erkenntnis: **Claude Design übersetzt nichts.** Es gibt das
Design als *ausführbares Original* weiter, und Claude Code liest es direkt. Genau daher kommt die
1:1-Treue — nicht aus einem besonders guten Zwischenformat, sondern aus dem Verzicht auf eines.

**Die Bauweise der `.dc.html`:**

| Element | Bedeutung |
|---------|-----------|
| `<x-dc>` | Wurzel des Dokuments |
| `<helmet>` | Kopf-Inhalte: Schrift-`<link>`s und globales `<style>` |
| `<dc-import name="Phone" theme="dark" hint-size="412px,892px">` | Komponenten-Einbindung — **Themes sind ein Parameter derselben Komponente**, nicht getrennte Dateien; die Renderbreite steht als Attribut daneben |
| `{{ themeKey }}` | Platzhalter, den `support.js` auflöst |
| `#fb[data-t^="vital"]`, `#fb[data-t$="-dark"]` | Themes als CSS-Variablenblöcke, adressiert über **Attribut-Selektoren** |
| `style="display:flex;gap:20px;padding:28px 20px 60px"` | **Layout durchgängig inline** — 234 `style`-Attribute, 0 CSS-Klassen |

**Schriften — genau so, wie es richtig ist:**

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous">
<link href="https://fonts.googleapis.com/css2?family=Schibsted+Grotesk:wght@400;500;600;700&…" rel="stylesheet">
```

Kein `@import`. Das deckt sich mit der web.dev-Empfehlung und ist der Grund, warum bei Claude
Design die Schrift ankommt (und bei einem `@import` mitten im Stylesheet nicht — Almanach B-07).

**Sonderfall, NICHT verallgemeinern:** Ein drittes Paket (Windows-Redesign) enthielt zusätzlich
`xaml-export/` mit fertigem `MainWindow.xaml`, `Themes/{Dark,Light}Theme.xaml`,
`Services/ThemeManager.cs` und `screenshots/*.png`. Das lag daran, dass dort das **bestehende
Programm mit eingelesen** worden war — es ist kein Regelverhalten des Downloads. Der Normalfall
oben ist der Maßstab.

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

> ⚠️ **Zuschreibungs-Korrektur (10.08.2026, zweiter Recherchelauf).** Diese beiden Sätze und die
> Formulierung „the design tokens actually used on the canvas" sind **keine Anthropic-Zitate**. Sie
> stammen aus dem Drittanbieter-Blog `claudefa.st` und sind dort redaktioneller Kommentar des
> Autors, nicht als zitierte Anthropic-Aussage gekennzeichnet (direkt im Quellkontext geprüft).
> Von Anthropic selbst wörtlich belegt ist im Wesentlichen ein Satz:
> „When a design is ready to build, Claude packages everything into a handoff bundle that you can
> pass to Claude Code with a single instruction." (anthropic.com/news/claude-design-anthropic-labs,
> 17.04.2026). Die inhaltliche Aussage bleibt durch §1a **empirisch** bestätigt — sie steht jetzt
> aber auf den echten Paketen statt auf einem falsch zugeschriebenen Zitat.

> ⚠️ **`/design-sync` läuft NICHT in beide Richtungen.** Die offizielle Referenz
> (`code.claude.com/docs/en/commands`) dokumentiert `/design-sync [hint]` und `/design-login`
> ausschließlich als Richtung **Repo → Claude-Design-Canvas** (Design-System hochladen). Die
> verbreitete Behauptung einer Zwei-Wege-Synchronisation stammt aus Drittanbieter-Blogs und einem
> ungeprüften X-Post. Eine frühere Fassung dieser Datei hat sie übernommen — hiermit richtiggestellt.

**Nicht öffentlich belegbar bleibt:** ein veröffentlichtes Schema, eine Feldnamen-Dokumentation, eine
Zusage zur Stabilität des Formats. Anthropic hat den internen Aufbau nicht publiziert. Was das
Format praktisch enthält, ist stattdessen **§1a** zu entnehmen — aus echten Paketen gelesen.
Nachbauen lässt sich damit sowohl das Prinzip als auch die konkrete Mechanik (Kopf-Verweise für
Schriften, Themes als Parameter, Renderbreite am Element, Versionsordner).

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

| Nr | Maßnahme | Woraus abgeleitet | Stand 10.08.2026, 22:05 |
|----|----------|-------------------|--------------------------|
| 1 | **Hierarchie exportieren, nicht nur Koordinaten.** Das Bündel braucht den Elternbaum mit Anordnungsart (Spalte/Zeile/Raster) je Bauteil — Koordinaten nur zusätzlich, mit der Breite, bei der sie gelten | §1, §2 | ✅ `bauplan/<erscheinung>/<nr>-<name>.json`; Ableitung liest jetzt auch Attribut-/Tag-Selektoren, Spezifität und Inline-Stile (vorher 11,3 % Verlust) |
| 2 | **Die tatsächlich benutzten Tokens mitgeben**, nicht die ganze Bibliothek | §1 | ⚠️ `design-tokens.json` markiert `verwendet: true/false`, liefert aber beides. Bewusst verlustfrei — der Umsetzer filtert |
| 3 | **Renderbreite in jede Bildschirmdatei schreiben**, damit der Export überall so aussieht wie im Designer | §4, Falle 3 | ✅ `<meta name="werft-render-width">` + aufgelöste Breiten-Regeln; der Messfühler liest sie jetzt statt fest 412 px anzunehmen |
| 4 | **Messfühler:** Breite beim Kontext-Erzeugen setzen, Zoom auf 100 %, an jedem Breakpoint messen | §4 | ✅ Browser wird je Renderbreite neu gestartet (Breite steht beim Erzeugen fest); Breite wird in die Messdatei geschrieben |
| 5 | **Pixel-Diff als Abnahmetor** — Entwurf gegen Gebautes, je Breite und Erscheinung, mit Toleranz | §5 | ❌ offen. `fidelity-check.ts` prüft nur die Import-Richtung (App → Design), nicht Entwurf → Gebautes |
| 6 | **Zuordnen statt übersetzen:** wo eine App schon Bauteile hat, sollte der Entwurf auf sie zeigen, statt neue erzeugen zu lassen | §3 | ❌ offen |
| 7 | **Schriften als Kopf-Verweis, mit Herkunft je Familie** | §1a, Almanach B-07 | ✅ `<link rel="preconnect">` + `<link rel="stylesheet">` in jeder Bildschirmdatei; `design-tokens.json` → `schriften` nennt Quelle, URL und Schnitte; Familie ohne Quelle wird gemeldet |
| 8 | **Rangfolge bei Widerspruch festschreiben:** Bauplan (Anordnung) > Tokens/Messung (Werte) > Prosa | §2, Almanach B-05/B-06 | ✅ in `design-umsetzer` und `spec-rueckimport` verankert; die alte Regel „bei Widerspruch gilt immer die Messung" ist ersetzt |

---

## Quellen

29 Quellen über 6 Researcher. Kernbelege: Anthropic Labs *Introducing Claude Design* · claudefa.st
zum Handoff Bundle · dev.to zu `/design-sync` · Vibe Coder Blog 04/2026 (Werkzeugvergleich mit
Bewertung) · Figma Guide to Dev Mode + Code Connect-Doku · Playwright-Doku (Browser, ElementHandle,
Emulation) + GitHub-Issue microsoft/playwright#36084 · Stack Overflow zu Media Queries und Zoom ·
QA Wolf, Virtuoso QA, Pcloudy zu visueller Regression.

Rohdaten des Laufs: `~/.research-swarm/answer-1.txt` bis `answer-6.txt` (28,8 KB).
