# Befund: Warum ein Entwurf nicht 1:1 ankommt

Stand: 10.08.2026, 12.00 Uhr · Aufgenommen nach Lauf 02 der Pipeline `neue-applikation`
Auftraggeber-Frage: „Lag der Fehler im Export von Werft Studio oder in der Umsetzung?"

> **Zweck dieser Datei:** Der nächste Lauf soll nicht bei Null anfangen. Hier steht, was
> **bewiesen** ist, was daraus folgt, und was noch zu prüfen ist. Ziel ist nicht die App
> „Experimente" — Ziel ist, dass ein Entwurf verlässlich zu 100 % im Code ankommt.

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
