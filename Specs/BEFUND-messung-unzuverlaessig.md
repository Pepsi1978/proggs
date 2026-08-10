# Befund: Warum ein Entwurf nicht 1:1 ankommt

Stand: 10.08.2026, 12.00 Uhr · Aufgenommen nach Lauf 02 der Pipeline `neue-applikation`
Auftraggeber-Frage: „Lag der Fehler im Export von Werft Studio oder in der Umsetzung?"

> **Zweck dieser Datei:** Der nächste Lauf soll nicht bei Null anfangen. Hier steht, was
> **bewiesen** ist, was daraus folgt, und was noch zu prüfen ist. Ziel ist nicht die App
> „Experimente" — Ziel ist, dass ein Entwurf verlässlich zu 100 % im Code ankommt.

---

## 0. Das Systemziel — und der strukturelle Fehler dahinter

Der Auftraggeber hat das Ziel dreimal deutlich gemacht, und es ist **kein App-Ziel**:

> „Es geht darum, dass das grundsätzlich funktioniert, die Pipeline. Wenn ich ein Design in
> Werft Studio bearbeite, soll es auch wirklich so programmiert werden — und es dürfen keine
> Fehler passieren, wenn ich das Design nur herunterlade."

B-08 ist nur der Fall, an dem es aufgefallen ist. Der Fehler ist **struktureller Natur**:

**Die Pipeline überträgt Momentaufnahmen statt der Quelle.**

| Weg | Was übergeben wird | Übersetzungen bis zum Code |
|-----|--------------------|----------------------------|
| **Claude Designs** — das erklärte Vorbild, dort funktioniert es | die `*.dc.html`: **die Quelle selbst**, mit Markup, CSS-Variablen, Themes, Keyframes | **eine**: Quelle → Code |
| **Werft heute** | Messwerte (`messung/*.json`) + gerenderte Bilder (`bilder/*.png`) + Spec-Prosa | **drei**: Design → Messung → Prosa → Code |

Jede Übersetzung kann etwas verlieren. Und eine Momentaufnahme kann **falsch** sein, ohne dass
es irgendwo auffällt — denn sie ist per Skill-Regel die verbindliche Wahrheit. Beim Fall B-08
wurde eine falsche Momentaufnahme dreimal hintereinander sauber weitergetragen: in das Spec, in
den Code, auf das Gerät. Build grün, Abhakliste vollständig, Ergebnis falsch.

**Der strukturelle Fix — die Kernaussage dieses Befundes:**

1. **Die Quelle geht mit und ist maßgeblich.** `WERFT-DESIGN/bildschirme/*.html` und
   `design.css` liegen bereits im Archiv. Sie müssen beim Bauen **gelesen** werden, nicht nur
   als „Augenschein" danebenliegen.
2. **Die Rangfolge wird umgedreht.** Heute gilt: „Widersprechen sich Text und Messung, gilt die
   Messung." Richtig ist: **Widersprechen sich Messung und Quelle, gewinnt die Quelle** — denn
   die Messung ist aus ihr abgeleitet und kann beim Ableiten kaputtgehen. Die Messung bleibt
   nützlich (sie löst Vererbung, `var()` und `color-mix()` auf), aber sie ist **Hilfsmittel,
   nicht Ersatz**.
3. **Jede Ableitung braucht eine Gegenprobe.** Wer messen will, muss danach prüfen können, ob
   das Gemessene zur Quelle passt. Ohne Gegenprobe ist jede Ableitung ein blinder Fleck.
4. **Der Export muss selbsttragend sein.** Eine Bildschirmdatei, die in verschiedenen Fenstern
   verschieden aussieht, ist kein Entwurf, sondern eine Vorlage mit Nebenbedingung. Die
   Renderbreite gehört in die Datei.

Alles Weitere in dieser Datei sind Einzelbefunde, die dieses Muster belegen.

### 0.1 Der Fix gehört nach WerftStudio — und der ist erreichbar

**Wichtig für den nächsten Lauf:** `~/proggs/WerftStudio/` liegt im Repo, der Zugang in
`~/SK/werft-studio/` (Adresse, Anmeldung, Zertifikat). Der Export ist also **kein fremder
Dienst**, sondern eigener Code. Damit ist die Ursache an ihrer Wurzel behebbar, statt sie
nachträglich im Rückimport auszubügeln.

Zu ändern ist die Stelle, die *Projekt als ZIP herunterladen* bedient. Sie muss liefern, was
Claude Designs mit der `.dc.html` liefert: **eine Datei, die überall so aussieht wie im
Designer.** Konkret:

1. **Renderbreite in jede Bildschirmdatei schreiben.** Die Vorschau im Designer steckt in einem
   Handy-Rahmen; die exportierte Datei hat keinen. Deshalb greifen Media Queries beim Betrachten
   und Vermessen anders. Ein `<meta name="viewport">` genügt nicht — die Datei braucht einen
   Rahmen mit der Breite, für die entworfen wurde, oder die Breite als maschinenlesbare Angabe
   im Kopf, an die sich Betrachter und Messfühler halten können.
2. **Die Zielbreite ins `design-tokens.json`** (`geometrie`), damit Stufe 2 sie ohne Raten
   kennt — heute steht dort `plattform: "web"`, was den Bau-Auftrag schon einmal in die Irre
   geführt hat.
3. **Selbstprüfung im Export:** Nach dem Schreiben jede Bildschirmdatei bei der Zielbreite
   rendern und mit der Designer-Vorschau vergleichen. Weichen sie ab, ist der Export fehlerhaft
   und meldet das — statt ein Archiv auszuliefern, das anders aussieht als der Entwurf.

**Die Recherche ist gelaufen (10.08.2026, Engine A, 6 Researcher, 29 Quellen)** und hat den
Vergleich in §0 bestätigt — mit einer wichtigen Präzisierung. Vollständig eingearbeitet in:

- `best-practices/design/design-zu-code-treue.md` (das Verfahren)
- `bugs/design/design-export-und-messung.md` (die sechs Fallen)

**Die drei Kernbelege:**

1. **Claude Design übergibt ein „Handoff Bundle"**: Komponentenstruktur als maschinenlesbare
   Spezifikation, die *tatsächlich verwendeten* Tokens, die **Layout-Hierarchie** und
   Asset-Referenzen. Anthropic grenzt ausdrücklich ab: „Not a PNG. Not a Figma URL that requires a
   plugin." und **„not a lossy export"**. Das Schlüsselwort ist **Hierarchie** — sie ist
   breitenunabhängig, eine Koordinate ist es nicht.
2. **Der Unterschied ist gemessen, nicht behauptet** (Werkzeugvergleich 04/2026): Builder.io, das
   von der Figma-**Dateistruktur** arbeitet, erreicht 4/5; v0, das von einem **Screenshot**
   arbeitet, 3/5 — mit dem Befund „often misses exact spacing values, specific font weights and
   subtle interaction details". Eine Messung ist dieselbe Art Ableitung wie ein Screenshot und
   verliert dasselbe.
3. **Figma geht weiter: zuordnen statt übersetzen.** Code Connect verbindet Design-Komponenten mit
   echtem Code im Repo; Entwickler und KI sehen die Produktionskomponente statt generierten Code.
   Die genaueste Übergabe ist die, bei der gar nichts übersetzt wird.

**Zwei technische Fallen, die den Messfühler direkt betreffen:**

- `setViewportSize` feuert **kein `resize`-Event** (Playwright-Issue #36084, vom Maintainer als
  dokumentiert bestätigt) — nachträglich gesetzte Breiten liefern womöglich alte Layoutwerte. Die
  Breite muss beim **Erzeugen des Kontexts** gesetzt werden.
- **Browser-Zoom ≠ 100 %** verschiebt Breakpoints proportional (1200 px bei 110 % → 1320 px).

**Und der fehlende Schritt:** Pixel-Diff gegen eine Baseline mit Toleranzschwelle, über mehrere
Breiten und Erscheinungen, ist etablierte Standardtechnik (pixelmatch, Resemble.js, Playwright
`toHaveScreenshot`, BackstopJS, Percy). In dieser Pipeline fehlt sie ganz — deshalb konnte die
Abweichung bis aufs Gerät durchlaufen.

---

## 1. Die Antwort: die Fensterbreite beim Rendern

> **Korrektur.** Der erste Befund dieser Datei lautete „der Export ist richtig, nur die Messung
> ist falsch". Das war zu früh geschlossen: Ich hatte zwei Regeln für `.werft-b08__field`
> gefunden und angenommen, die einspaltige stehe ungeschützt danach. Der Benutzer hat
> widersprochen — er hatte die exportierte Datei selbst geöffnet und dort die falsche Anordnung
> gesehen. Er hatte recht. Die Prüfung, wo die Regel wirklich steht, ergab:

```css
@media (max-width: 480px) {
  .werft-b08__field { grid-template-columns: minmax(0, 1fr); align-items: start; row-gap: 6px; }
}
```

**Die richtige Anordnung hängt an einer Media Query — und Media Queries messen die
Fensterbreite, nicht die Elementbreite.**

| Wer rendert | Fensterbreite | Was greift | Ergebnis |
|-------------|---------------|-----------|----------|
| Werft Studio, Vorschau im Handy-Rahmen | schmal (< 480 px) | einspaltig | **richtig** — so sieht es der Benutzer |
| Browser, Datei einfach geöffnet | Fensterbreite des Rechners | zweispaltig | falsch |
| `messe-design.ps1` | Standard-Fensterbreite | zweispaltig | **falsch — und diese Zahlen werden verbindlich** |

Der Bildschirm selbst ist 475 dp breit, läge also unter der Schwelle. Das hilft aber nicht: die
Media Query fragt das **Fenster**, und darin ist der 475-dp-Bildschirm nur ein Kasten.

**Damit liegt der Fehler an zwei Stellen, und beide müssen behoben werden:**

1. **Der Export** (`WERFT-DESIGN/bildschirme/<erscheinung>/<nr>-<name>.html`) sagt **nicht**, bei
   welcher Fensterbreite er zu rendern ist. Ohne diese Angabe zeigt jeder Betrachter — Browser,
   Messfühler, Mensch — eine andere Variante als der Designer. Der Benutzer hat das zuerst
   gesehen: „in dem runtergeladenen Spec sieht es anders aus".
2. **Der Messfühler** setzt das Fenster nicht auf die Zielbreite, bevor er liest.

Der Rest der Kette hat den Fehler dann treu weitergetragen: Messung → Spec → Code → Gerät.

---

## 2. Der schwerere Fehler: die Messung ist unangreifbar

`design-umsetzer/SKILL.md` und `references/messung-umsetzen.md` schreiben fest:

> „Widersprechen sich Text und Messung, gilt die Messung."
> „Was in der Messung steht, wird gebaut. Was nicht darin steht, wird nicht gebaut."

Das ist richtig gemeint — es verhindert, dass ein alter Spec-Satz eine gemessene Farbe
verdrängt. Aber es macht die Messung zur **einzigen** Wahrheit, ohne Gegenprobe. Eine falsch
gemessene Zahl wird dadurch sauber durch Stufe 2, Stufe 3, Build und Installation getragen,
**ohne dass irgendwo ein Fehler auftaucht**. Der Bau ist grün, die Abhaklisten sind abgehakt,
und das Ergebnis ist trotzdem ein anderer Bildschirm.

Genau das ist passiert: Ich habe die Messung befolgt und die CSS, die es besser wusste, nie
gelesen. Aufgefallen ist es erst, als der Benutzer die Designer-Ansicht daneben hielt.

**Folgerung:** Es fehlt ein Schritt, der die Messung gegen eine **unabhängige** Quelle prüft,
bevor sie verbindlich wird.

---

## 3. Weitere belegte Schwachstellen der Kette

| Nr | Stelle | Befund | Beleg |
|----|--------|--------|-------|
| S-1 | `messe-design.ps1` | Liest überschriebene CSS-Regeln statt der kaskadierten Endfassung | §1 dieser Datei |
| S-2 | Skill-Regel | Die Messung ist verbindlich, ohne Gegenprobe gegen HTML/CSS | §2 |
| S-3 | `messe-design.ps1` | Messung ist eine Momentaufnahme bei **einer** Breite (hier 475 dp). Ein responsiver Entwurf hat aber mehrere gültige Anordnungen; die Umbruchregeln (`@media (max-width: 480px)`, `600px`) gehen dabei verloren | 17 Media Queries im Export, keine davon in der Messung abgebildet |
| S-4 | Bau-Auftrag vs. Messpaket | Bau-Auftrag nennt 412 × 915 dp als Bezugsgröße, `werft-screen` misst 475 × 751. Niemand prüft das gegeneinander | `BAU-AUFTRAG.md` §2 gegen die Messdateien |
| S-5 | `symbole-erzeugen.ps1` | Benennt Symbole nach der nächstgelegenen Beschriftung und fasst gleiche Pfade zusammen. Tragen mehrere Bedienelemente dieselbe Beschriftung (fünf × „Hauptnavigation"), verschiebt sich die Zuordnung um eins | Lauf 02, F-05 — behoben über `Leistensymbole.kt` (Zuordnung nach Baumpfad) |
| S-6 | `bildschirm-erzeugen.ps1` | Erzeugt absolut positionierte Bildschirme für die Messbreite. Als Vorlage richtig, als App-Oberfläche falsch — auf jedem anderen Gerät verrutscht alles | Lauf 02, V-05 |
| S-7 | Ablauf | Weil S-6 nicht taugt, entstand eine **zweite, handgeschriebene** Oberfläche — und die entstand **nach** dem Backend und richtete sich nach dessen Bedürfnissen statt nach dem Entwurf. Zwei Wahrheiten, die schlechtere lief | Lauf 02; die neun erzeugten Dateien wurden entfernt |
| S-8 | Abnahme-Tor | Verlangt 9 Bildschirme × 2 Erscheinungen = 18 Bildvergleiche im selben Lauf wie den Bau. In Lauf 01 fiel es aus (kein Gerät), in Lauf 02 blieb es bei vier | `neue-applikation` SKILL.md |

---

## 4. Was der nächste Lauf tun soll

**Reihenfolge, vom Benutzer vorgegeben:** Erst die Oberfläche exakt nach Entwurf, mit ihren
Effekten. Dann in Ruhe lassen. Dann das Verhalten dahinter. Nicht umgekehrt, und nicht zweimal.

1. **Messfühler reparieren (S-1).** Die kaskadierte Endfassung auslesen, nicht eine
   überschriebene Regel. Verdacht: es wird pro Regel gelesen statt `getComputedStyle` auf dem
   fertig gerenderten Element — oder die Einzeldatei wird ohne den vollständigen Regelsatz
   geöffnet. Prüfen an `.werft-b08__field`: die Messung **muss** einspaltig herauskommen.
2. **Gegenprobe einbauen (S-2).** Nach dem Messen automatisch prüfen: Stimmt die gemessene
   Anordnung mit dem HTML-Baum und der Endfassung der CSS zusammen? Jede Abweichung wird
   gemeldet, statt stillschweigend verbindlich zu werden.
3. **Bei der Zielbreite messen (S-3, S-4).** Nicht bei der Fensterbreite des Designers, sondern
   bei der Breite des Zielgeräts — und bei responsiven Entwürfen bei **jeder** Breite, an der
   eine Media Query greift. Die Bezugsgröße aus dem Bau-Auftrag gegen `werft-screen` prüfen und
   bei Abweichung anhalten.
4. **Recherche (vom Benutzer freigegeben, Weg A — Firecrawl + MiniMax):** Wie löst **Claude
   Designs** die Übergabe eines fertigen Entwurfs an den Code? Der Benutzer nennt es als
   Vorbild, weil es dort funktioniert. Konkret zu klären: Übergibt Claude Designs Werte oder
   Struktur? Wird gemessen oder wird der Entwurf als Quelltext weitergegeben? Wie wird
   Responsivität behandelt? Gibt es eine Gegenprobe? Das Ergebnis gehört nach
   `best-practices/` und die gefundenen Fallen in `bugs/`.
5. **Erst danach** die restlichen acht Bildschirme von „Experimente" nach dem geprüften
   Verfahren bauen — je Bildschirm gegen sein Bild, bevor der nächste beginnt.

---

## 5. Was am Ende gelten muss

Ein Entwurf ist erst dann verlässlich umgesetzt, wenn **zwei unabhängige Quellen** dasselbe
sagen: die Messung **und** der Entwurf selbst (HTML-Baum plus kaskadierte CSS). Solange nur
eine Quelle befragt wird, kann ein Fehler in dieser Quelle nicht auffallen — und dann steht am
Ende ein Bildschirm, den niemand entworfen hat, mit einem grünen Build davor.
