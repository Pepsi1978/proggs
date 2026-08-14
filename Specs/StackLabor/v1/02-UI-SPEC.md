# UI-Spec — StackLabor
Stand: 14.08.2026 · Stufe: v1 · Plattform(en): Android

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/StackLabor/v2/messung/`. Widerspricht ein Satz von
> hier der Messung, ist der Satz ueberholt — nicht die Messung falsch.

## 1. Gestalterische Grundhaltung

StackLabor ist ein **Laborinstrument, kein Ratgeber**. Es zeigt Zustände, nicht Meinungen: Was
grün ist, ist grün, und was nicht bedient wird, sieht auch so aus. Die Oberfläche ist hell,
klar und dicht — Frank arbeitet hier mit vielen Zeilen auf wenig Fläche und muss den Zustand
einer Liste in einem Blick erfassen können.

Die Gestaltung darf und soll aufwendig sein: Tiefe, Verläufe, Glas, Schimmer, Auren und
Bewegung sind ausdrücklich gewünscht. **Aber sie ordnen sich der Ablesbarkeit unter.** Die drei
Ampelfarben sind die lautesten Elemente im Bild; kein Schmuck darf mit ihnen um Aufmerksamkeit
konkurrieren. Deshalb ist die Akzentfarbe bewusst so gewählt, dass sie keiner Ampelfarbe
ähnelt.

Jede Zeile trägt sechs Informationen (Zustand, Name, Löslichkeit, Dosis, Frequenz, Wirkung auf
die Ziele). Der Weg dahin führt nicht über mehr Elemente nebeneinander, sondern über **zwei
Zeilen und eine ruhige Hierarchie**: Was Frank sucht, steht links oben; was er nur manchmal
braucht, klein darunter.

## 2. Erscheinungen (Themes)

Beide Erscheinungen sind **vollständig und gleichwertig**. Umgeschaltet wird über das
Sonne/Mond-Symbol im Kopf von B-01 (F-22).

### 2.1 Hell — **Standard-Erscheinung**

| Rolle | Wert | Verwendung | Kontrast |
|---|---|---|---|
| Grund | `#F5F7FA` | Bildschirmhintergrund, deaktivierte Karte | — |
| Fläche / Karte | `#FFFFFF` | Stack-Karte, Mittel-Eintrag, Blätter | 1,05:1 auf Grund |
| Erhöhte Fläche | `#F1F5F9` | Sockel, Chips, Kopfleiste | 1,1:1 |
| Rand | `#E2E8F0` | Trenner 1 dp, Kartenkontur | 1,3:1 |
| Text stark | `#0F172A` | Mittel-Name, Titel, Zieltext | 17,4:1 auf Karte |
| Text schwach | `#64748B` | zweite Zeile, Zeitpunkte, Metazeile | 4,8:1 |
| Akzent | `#4F46E5` | Auswerten-Knopf, aktives Häkchen, Verweise | 7,6:1 |
| Ampel grün (Fläche) | `#047857` | Kantenbalken, Zählpunkt | 4,9:1 |
| Ampel gelb (Fläche) | `#D97706` | Kantenbalken, Zählpunkt | 3,3:1 |
| Ampel gelb (Text) | `#B45309` | Kurzgrund, Begründungszeile | 5,9:1 |
| Ampel rot | `#DC2626` | Kantenbalken, Aura, Warntext | 4,5:1 |
| Ampel rot (kräftig) | `#B91C1C` | Überschrift einer Fehlerkarte | 6,0:1 |
| Ampel grau („nicht bedient") | `#94A3B8` | Kantenbalken, Zählpunkt | 2,8:1 |
| Löslich wasser | `#059669` | 8 dp Punkt, gefüllt | 4,0:1 |
| Löslich fett | `#FFFFFF` mit 1,5 dp Rand `#64748B` | 8 dp Punkt, hohl | Rand 4,8:1 |
| Deaktiviert | `#CBD5E1` | Kantenbalken und Punkte im ausgegrauten Eintrag | 1,7:1 (bewusst schwach) |

> **Wichtig:** Die üblichen hellen Ampelfarben (`#34D399`, `#FBBF24`, `#F87171`) sind auf
> weißer Fläche zu blass und wurden deshalb für die helle Erscheinung nachgedunkelt.

### 2.2 Dunkel — gleichwertige zweite Erscheinung

| Rolle | Wert | Verwendung |
|---|---|---|
| Grund | `#0B0E14` | Bildschirmhintergrund |
| Fläche / Karte | `#141A24` | Karten, Blätter |
| Erhöhte Fläche | `#1B2330` | Sockel, Chips, Kopfleiste |
| Rand | `#243040` | Trenner, Kontur |
| Text stark | `#E6EAF2` | Namen, Titel |
| Text schwach | `#9AA6B8` | zweite Zeile, Metazeile |
| Akzent | `#22D3EE` | Auswerten-Knopf, aktives Häkchen |
| Ampel grün | `#34D399` | Kantenbalken, Zählpunkt |
| Ampel gelb | `#FBBF24` | Kantenbalken, Zählpunkt |
| Ampel rot | `#F87171` | Kantenbalken, Aura |
| Ampel grau | `#64748B` | „nicht bedient" |
| Löslich wasser | `#34D399` | 8 dp Punkt, gefüllt |
| Löslich fett | `#FFFFFF` ohne Rand (auf dunkel selbst tragend) | 8 dp Punkt, gefüllt |
| Deaktiviert | `#334155` | ausgegrauter Eintrag |

## 3. Typografie

Schriftfamilie: **Inter** (oder die Systemschrift, wenn der Designer nichts anderes wählt),
Schnitte Regular 400, Medium 500, SemiBold 600.
Franks Systemschrift steht auf **90 %** — alle sp-Werte sind so gerechnet, dass sie damit passen.

| Rolle | Größe | Gewicht | Zeilenhöhe | Laufweite |
|---|---|---|---|---|
| Bildschirmtitel (B-01) | 22 sp | 600 | 28 dp | −0,2 |
| Kopfzeile eines Bildschirms | 17 sp | 600 | 22 dp | 0 |
| Stack-Name auf der Karte | 16 sp | 600 | 20 dp | 0 |
| Mittel-Name (Zeile 1) | 15 sp | 500 | 20 dp | 0 |
| Zieltext | 14 sp | 500 | 18 dp | 0 |
| Zweite Zeile (Dosis, Frequenz) | 12 sp | 400 | 16 dp | +0,1 |
| Metazeile (Zeitstempel, Modell) | 12 sp | 400 | 16 dp | +0,1 |
| Ziel-Nummer im Kreis | 11 sp | 600 | — | 0 |
| Fließtext der Auswertung (B-07) | 15 sp | 400 | 22 dp | 0 |
| Geräte-Code (B-11) | 40 sp | 600 | 48 dp | +2,0 |

## 4. Maße und Raster

Grundraster **4 dp**. Alle Abstände sind Vielfache davon.

| Maß | Wert |
|---|---|
| Bildschirmrand seitlich | 12 dp |
| Kartenbreite (Cover) | 273 dp (= 297 − 2 × 12) |
| Nutzbare Höhe (Cover) | 421 dp (= 469 − 24 Statusleiste − 24 Gestenleiste) |
| Innenabstand einer Karte | 12 dp |
| Abstand zwischen Karten | 8 dp |
| Kopfleiste eines Unterbildschirms | 56 dp |
| Kopfbereich B-01 | 96 dp |
| Ziel-Streifen (zugeklappt) B-02 | 40 dp |
| Sortier-/Suchleiste B-02 | 36 dp |
| Auswerten-Sockel B-02 (fest) | 52 dp |
| **Verbleibende Listenhöhe B-02** | **237 dp = 4 Einträge** |
| Mittel-Eintrag | 273 × 56 dp, + 1 dp Trenner = **57 dp Takt** |
| Ziel-Eintrag | 273 × 40 dp |
| Stack-Karte B-01 | 273 × 76 dp |
| Mindest-Tippfläche | 44 × 44 dp |
| Schwebender Plus-Knopf | 56 dp, 16 dp vom Rand |

## 5. Formen und Tiefe

| Bauteil | Radius | Rand | Tiefe |
|---|---|---|---|
| Karte (Stack, Mittel) | 12 dp | 1 dp `Rand` | Schatten 2 dp, beim Ziehen 8 dp |
| Blatt (von unten) | 20 dp oben | keiner | Schatten 16 dp + Abdunklung 32 % |
| Knopf (Sockel) | 12 dp | keiner | Fläche `Akzent` |
| Chip (Sortierung) | vollrund | 1 dp bei inaktiv | keine |
| Ampel-Kantenbalken | 2 dp links | — | bei Rot zusätzlich Aura (siehe Motion M-21) |
| Löslichkeits-Punkt | vollrund, 8 dp Ø | 1,5 dp nur beim fettlöslichen (hell) | keine |
| Nummernkreis (Ziel) | vollrund, 20 dp Ø | 1 dp | keine |
| Eingabefeld | 12 dp | 1 dp, im Fehlerfall `Ampel rot` | keine |

**Verläufe:** Kartenrand als 1,5 dp starker Verlauf `Akzent` → transparent im Uhrzeigersinn.
Kopfbereich B-01 mit wanderndem Verlauf `Akzent` → `#0EA5E9` (siehe Motion M-16).
**Weichzeichner:** 24 dp, ausschließlich auf **festen** Flächen (Kopfleiste, Auswerten-Sockel,
Blätter) — **niemals** über der scrollenden Liste.

## 6. Bildschirme

| Kennung | Bildschirm | Zweck | Start? | führt zu |
|---|---|---|---|---|
| B-01 | Hauptbildschirm (Stack-Übersicht) | Alle Stacks auf einen Blick, Zugang zu allem | **ja** | B-02, B-03, B-09, B-10, B-13, B-14 |
| B-02 | Stack-Detail | Der Arbeitsplatz | nein | B-04, B-05, B-06, B-07, B-08, B-14, B-15 |
| B-03 | Ziel-Katalog | Ziele einmal anlegen | nein | — |
| B-04 | Ziele dieses Stacks (Blatt) | Ankreuzen, Ampeln und Gründe ansehen | nein | B-03, B-06, B-12 |
| B-05 | Mittel bearbeiten (Blatt) | Stammdaten + Stack-Dosis | nein | — |
| B-06 | Aufschlüsselung (Blatt) | Mittel → Ziele oder Ziel → Mittel | nein | — |
| B-07 | Auswertung im Vollbild | Voller Text, Vorlesen | nein | — |
| B-08 | Eigene Fragen (Blatt) | Fragen je Stack | nein | — |
| B-09 | Alle Stacks zusammen | Tagesgesamtdosis, übergreifende Konkurrenzen | nein | B-07 |
| B-10 | Einstellungen | Vorlesen, Codex, Daten, Darstellung | nein | B-11 |
| B-11 | Codex-Anmeldung | Geräte-Flow | nein | — |
| B-12 | Ziele ordnen (Vollbild) | Drag & Drop über die volle Höhe | nein | — |
| B-13 | Stack bearbeiten (Blatt) | Name, Zeitpunkt, Einnahme-Hinweis | nein | — |
| B-14 | Mittel-Katalog | Alle Mittel, Suche, Zusammenführen | nein | B-05 |
| B-15 | Auswertungs-Historie (Blatt) | Letzte fünf Läufe, Vergleich | nein | B-07 |

---

### B-01 — Hauptbildschirm

**Aufbau von oben nach unten:**
1. **Kopfbereich 96 dp** — Titel „StackLabor" 22 sp bei x = 16. Rechts drei Symbolknöpfe à 44 dp mit 4 dp Zwischenraum (140 dp gesamt): Sonne/Mond (F-22), Zielscheibe → B-03, Zahnrad → B-10. Darunter der Umschalter **Frei / Dienst** (F-27) als Chip-Paar, nur sichtbar, wenn mindestens ein Mittel zwei Dosen hat.
2. **Leiste „Alle Stacks zusammen prüfen" 48 dp**, volle Breite, rechts der Zeitstempel der letzten Gesamtprüfung.
3. **Stack-Karten**, je 76 dp + 8 dp Abstand:
   - 3 dp Kantenbalken links = Sammelampel (schlechteste Ziel-Ampel)
   - Name 16 sp · darunter Zeitpunkt und Einnahme-Hinweis 12 sp `Text schwach`
   - Unten drei Zählpunkte à 8 dp („● 3 ● 1 ● 1") und „12 Mittel"
   - Ein kleiner Punkt rechts oben, wenn ein offener Hinweis aus F-02 vorliegt
4. **Schwebender Plus-Knopf 56 dp**, 16 dp vom rechten unteren Rand → B-13.

**Rechnung:** 421 − 96 − 48 = 277 dp → **3 Karten vollständig + 25 dp Anschnitt** (von sechs).

**Zustände:** leer (keine Stacks — Text „Noch kein Stack" mit Knopf) · nie ausgewertet (Balken grau) · veraltet (Balken gestrichelt) · Codex nicht angemeldet (Leiste zeigt „Anmelden") · offline (Leiste ausgegraut, Karten voll bedienbar).

---

### B-02 — Stack-Detail

**Aufbau:**
1. **Kopfleiste 56 dp** — Zurück 44 dp · Stack-Name 17 sp, darunter Zeitpunkt 12 sp · Überlaufmenü 44 dp (Stack bearbeiten → B-13, Eigene Fragen → B-08, Historie → B-15).
2. **Ziel-Streifen 40 dp** — 3 dp Kantenbalken (schlechteste Ziel-Ampel) · „Ziele 5 · ● 3 ● 1 ● 1" · Pfeil rechts. Tippen öffnet B-04 als Überlagerung.
3. **Sortier- und Suchleiste 36 dp** — Chips „Löslichkeit" und „Einnahme" je 92 dp · Lupe 36 dp (klappt die Suchzeile aus).
4. **Mittel-Liste** — 237 dp, Takt 57 dp → **4 Einträge sichtbar** + 9 dp Anschnitt.
5. **Auswerten-Sockel 52 dp, fest am unteren Rand** — Knopf 273 × 44 dp, darüber eine 4 dp hohe Verlaufskante, damit die Liste sichtbar darunter verschwindet.

**Der Ziel-Bereich verdrängt die Liste nicht.** Aufgeklappt legt er sich als Überlagerung
(max 281 dp) darüber — würde er verdrängen, blieben 77 dp und damit 1,3 Mittel übrig.

**Zustände:** leer (keine Mittel) · lädt (Schimmer über 4 Platzhaltern) · Auswertung läuft (Sockel wird Fortschrittsbalken mit Abbrechen) · veraltet (Sockel bernstein, „Stand veraltet — neu auswerten") · Codex-Fehler (64 dp hohe Karte über dem Sockel, je nach Fehlerart mit passendem Knopf) · offline (Sockel ausgegraut, Ampeln und Häkchen arbeiten weiter) · offener Hinweis aus F-02 (Schnipsel 72 dp über dem Sockel, 20 Sekunden stehend, mit „Behalten" / „Doch entfernen").

---

### B-03 — Ziel-Katalog

Kopfleiste 56 dp · Suchzeile 40 dp · Zeilen 56 dp (Zieltext 15 sp, darunter „in 4 Stacks
verwendet" 12 sp, rechts Stift 44 dp) · schwebender Plus-Knopf.
325 dp → 5 Zeilen + Anschnitt.
**Zustände:** leer · Löschwarnung, wenn das Ziel in Stacks verwendet wird (mit deren Namen).

---

### B-04 — Ziele dieses Stacks (Blatt)

Griff 24 dp · Titel 40 dp („Ziele — Morgen-Stack Teil 1") · Zeilen 48 dp: Kästchen 22 dp links,
Nummer, Zieltext, Ampel als Kantenbalken. Maximale Höhe 70 % = 328 dp → 5 Ziele + Anschnitt.
Fußzeile 52 dp mit zwei Knöpfen: **„Ordnen"** (→ B-12) und **„Fertig"**.
Tippen auf eine Ampel öffnet B-06 (Richtung Ziel → Mittel). Tippen auf die Zeile klappt die
Begründung auf.
**Zustände:** leer (Katalog ohne Ziele — „Erst Ziele anlegen" mit Sprung zu B-03) · keine Bewertung (alle Ampeln grau).

---

### B-05 — Mittel bearbeiten (Blatt)

Höhe 90 % = 421 dp, scrollend, zwei erkennbar getrennte Bereiche.

**Stammdaten** (mit Hinweis „gilt in N Stacks"): Name 72 dp · Löslichkeit als drei Chips 72 dp
(Wasser / Fett / Beides) · Darreichungsform 72 dp · Hersteller 72 dp · Durchfallrisiko-Schalter
56 dp · Beistoffe 72 dp.
**In diesem Stack**: eine 72-dp-Zeile mit drei Feldern nebeneinander — Stückzahl 88 dp ×
Menge 88 dp + Einheit 88 dp, darunter die Vorschau „2 × 80 mg = 160 mg" · zweite Dosis-Variante
72 dp (optional) · Frequenz 72 dp · „alterniert mit" 72 dp · Kombi-Gruppe 72 dp ·
Zusatztext für die KI 120 dp.
Fester Sockel 60 dp mit „Sichern".

**Zustände:** neu · bearbeiten · Pflichtfeld leer (Feld rot umrandet, Sichern gesperrt) · löschen.

---

### B-06 — Aufschlüsselung (Blatt)

Höhe 70 %. Kopf 56 dp nennt den Gegenstand (Mittelname **oder** Zieltext). Je Gegenseite ein
Block von 76 dp: Nummer 20 dp · Text 15 sp · Urteilsmarke (stützt / neutral / stört) ·
Begründung zwei Zeilen 12 sp. Ein Schalter „auch neutrale zeigen".
**Zustände:** keine Bewertung vorhanden · Bewertung veraltet (Hinweiszeile im Kopf).

---

### B-07 — Auswertung im Vollbild

Kopfleiste 56 dp · Metazeile 32 dp („ausgewertet 14.08. 12:28 · Terra · hoch") · Fließtext
15 sp mit 22 dp Zeilenhöhe, Rand 16 dp · fester Sockel 60 dp mit Vorlesen / Pause / Stopp und
drei Pegelbalken.
**Zustände:** lädt (Text baut sich wortweise auf, darüber die Fortschrittserzählung) · fertig ·
Vorlesen läuft · Vorlese-Fehler · offline (der zuletzt gespeicherte Text bleibt lesbar).

---

### B-08 — Eigene Fragen (Blatt)

Kopf 56 dp · Zeilen 60 dp (Fragetext bis zwei Zeilen, Wischen löscht) · schwebender Plus-Knopf.
**Zustände:** leer („Ohne eigene Fragen antwortet die Auswertung allgemein").

---

### B-09 — Alle Stacks zusammen

Kopfleiste 56 dp · Abschnitt **„Tagesgesamtdosis"** mit Zeilen à 44 dp (Wirkstoff 14 sp links,
Summe rechtsbündig auf 100 dp, darunter klein die beteiligten Stacks; 3 dp Balken bei
auffälliger Menge) · Abschnitt **„Konkurrenzen über Stacks hinweg"** als Karten à 72 dp ·
fester Sockel 52 dp „Alles prüfen".
Der Fließtext wird über **denselben** Vollbild-Bildschirm B-07 gelesen und vorgelesen.
**Zustände:** identisch zu B-02 (leer, lädt, veraltet, offline, nicht angemeldet, Fehler).

---

### B-10 — Einstellungen

Kopfleiste 56 dp, danach vier Rubriken mit 32-dp-Überschriften und Zeilen à 56 dp:
- **Vorlesen** — Anbieter · Stimme · Tempo · Pause zwischen Absätzen · automatische Abschaltung · Verbrauch
- **Codex** — Konto (angemeldet als …) · Modell · Denkstufe
- **Daten** — Exportieren · Importieren · Startbestand einlesen · Letzte Sicherung wiederherstellen
- **Darstellung** — Hell/Dunkel · Bewegung reduzieren

**Zustände:** angemeldet / nicht angemeldet · Import läuft · Warnung vor dem Startbestand-Einlesen
(überschreibt 72 Einträge) · Verbrauchszähler leer.

---

### B-11 — Codex-Anmeldung

Kopf 56 dp · Code 40 sp mittig in einem 96 dp hohen Feld · Verifizierungsadresse 44 dp ·
Knopf „Seite öffnen" 52 dp · Wartezeile 40 dp mit Pulsring.
**Zustände:** wartet · Code abgelaufen (neuer Code auf Knopfdruck) · verweigert · Netzfehler ·
bereits angemeldet (zeigt „Abmelden").

---

### B-12 — Ziele ordnen (Vollbild)

Kopfleiste 56 dp („Ziele ordnen — <Stack>") · Liste über die **volle** verbleibende Höhe
(365 dp → 9 Ziele sichtbar) · kein Sockel, damit die Ablagefläche maximal ist.
Hier und nur hier findet das Ziehen der Ziele statt (M-01 bis M-06).
**Zustände:** weniger als zwei Ziele (Ziehen ausgesetzt, Hinweiszeile).

---

### B-13 — Stack bearbeiten (Blatt)

Griff 24 dp · Titel 40 dp · Name 72 dp · Zeitpunkt 72 dp · Einnahme-Hinweis 72 dp ·
Sockel 60 dp mit „Sichern" und, beim Bearbeiten, „Stack löschen" in `Ampel rot`.
**Zustände:** neu · bearbeiten · Löschwarnung mit Anzahl der enthaltenen Mittel.

---

### B-14 — Mittel-Katalog

Kopfleiste 56 dp · Suchzeile 40 dp · Zeilen 56 dp (Name 15 sp mit Löslichkeitspunkten, darunter
„in 3 Stacks · Kapsel · Thorne" 12 sp) · schwebender Plus-Knopf · Überlaufmenü mit „Zusammenführen".
**Zustände:** leer · Suche ohne Treffer (mit „Neu anlegen") · Zusammenführen-Auswahl (zwei
Einträge markiert, Frank wählt den bleibenden).

---

### B-15 — Auswertungs-Historie (Blatt)

Kopf 56 dp · fünf Zeilen à 64 dp (Zeitpunkt, Modell, Kurzbilanz „4 grün · 1 gelb · 0 rot") ·
zwei Zeilen auswählbar → Vergleich als Liste der Unterschiede je Ziel („Ziel 4: gelb → grün").
**Zustände:** weniger als zwei Läufe (Vergleich ausgegraut).

---

## 6a. Der Mittel-Eintrag — exakt

Karte 273 × **56 dp**, Radius 12 dp, darunter 1 dp Trenner → **57 dp Takt**.

- **Kantenbalken 3 dp** ganz links, über die volle Höhe, Radius 2 dp links, Farbe = Ampel des Mittels.
- Inhalt beginnt bei **x = 13 dp** (3 dp Balken + 10 dp Luft).
- Rechts das **Häkchen-Kästchen 22 dp** in einer 44 × 44 dp Tippfläche, rechter Innenabstand 8 dp
  → **Textspalte 208 dp**.

**Zeile 1** (y = 8 … 28 dp):
Löslichkeitspunkte 8 dp Ø (ein Punkt belegt 14 dp, zwei Punkte 25 dp) · dann der **Name 15 sp,
`Text stark`, einzeilig, max 183 dp**.
Geprüft: „PEA (Palmitoylethanolamid)" ≈ 177 dp ✔ · „Acetyl-L-Carnitin (ALCAR)" ≈ 163 dp ✔ ·
„Nicotinamid-Ribosid (NR)" ≈ 163 dp ✔.
**Regel bei Überlauf:** erst den Klammerzusatz weglassen, dann Auslassungszeichen am Ende —
**nie** mitten im Wortstamm brechen.

**Zeile 2** (y = 30 … 48 dp, 12 sp, `Text schwach`):
Links die Dosis „2 × 80 mg = 160 mg" (max 130 dp) · „· Pulver" nur, wenn die Form nicht Kapsel
ist · „· alle 3 Tage" nur, wenn die Frequenz nicht täglich ist (verdrängt dann die Form) ·
rechtsbündig der **Kurzgrund auf 78 dp** in der Ampel-Textfarbe („stört 3, 7"), bei Grün leer.

**Deaktivierter Eintrag:** Fläche = `Grund` statt `Fläche` (nicht erhöht), Balken `#CBD5E1`,
alle Texte auf 38 % Deckkraft, Löslichkeitspunkte entsättigt auf `#94A3B8`, Kästchen leer,
kein Schatten, keine Aura.

**Kombi-Gruppe:** 2 dp starke Klammerlinie am linken Rand über alle Mitglieder, darüber eine
Kopfzeile 32 dp „zusammen einnehmen" mit dem Gruppen-Häkchen (zeigt einen Teilzustand, wenn nur
manche Mitglieder aktiv sind).

## 6b. Der Ziel-Eintrag — exakt

Zeile 273 × **40 dp**:
Kantenbalken 3 dp links (Ampel des Ziels) · **Nummernkreis 20 dp Ø** bei x = 13 dp, Zahl 11 sp ·
**Zieltext 14 sp ab x = 41 dp, Breite 192 dp**, einzeilig mit Auslassungszeichen ·
**Ziehgriff 24 dp** bei x = 241 dp in einer 44 dp Tippfläche (nur auf B-12).

**Begründung bei Rot oder Gelb:** eine **eigene aufklappende Zeile**, keine Sprechblase —
eine Blase würde die Nachbarn verdecken und mit dem Ziehen kollidieren. Tippen auf die Zeile
lässt die Höhe in 200 ms auf 40 + n × 16 + 8 dp wachsen; bei drei Zeilen zu 12 sp sind das
**96 dp**. Der Text beginnt bei x = 41 dp, Breite 216 dp, in `Text schwach`; der Kantenbalken
läuft über die gesamte aufgeklappte Höhe durch. Beim Ziehen klappt die Begründung automatisch zu.

## 7. Ikonografie und Bilder

Symbolsatz **Material Symbols, Variante Rounded, Strichstärke 400**, Größe 24 dp
(Kopfzeilen-Symbole 22 dp).

| Zweck | Symbol |
|---|---|
| Hell/Dunkel | `light_mode` / `dark_mode` |
| Ziel-Katalog | `target` (Zielscheibe) |
| Einstellungen | `settings` |
| Mittel-Katalog | `inventory_2` |
| Hinzufügen | `add` |
| Suchen | `search` |
| Ziehgriff | `drag_indicator` |
| Vorlesen / Pause / Stopp | `volume_up` / `pause` / `stop` |
| Auswerten | `auto_awesome` |
| Historie | `history` |
| Aufschlüsselung | `insights` |
| Zurück | `arrow_back` |
| Überlauf | `more_vert` |

Es gibt **keine Fotos und keine Illustrationen**. Die einzigen Bildelemente sind die Ampeln,
die Löslichkeitspunkte und die Symbole. App-Symbol: eine stilisierte Kapsel im Akzentton auf
hellem Grund, in der Dunkelfassung umgekehrt.

## 8. Texte

Alle festen Beschriftungen wörtlich, in dieser Schreibweise:

| Ort | Text |
|---|---|
| B-01 Titel | `StackLabor` |
| B-01 Leiste | `Alle Stacks zusammen prüfen` |
| B-01 leer | `Noch kein Stack. Tippe auf das Plus, um deinen ersten anzulegen.` |
| B-01 Dosis-Umschalter | `Frei` / `Dienst` |
| B-02 Sockel | `Diesen Stack auswerten` |
| B-02 Sockel veraltet | `Stand veraltet — neu auswerten` |
| B-02 Sockel läuft | `Auswertung läuft — Abbrechen` |
| B-02 Sortierchips | `Löslichkeit` / `Einnahme` |
| B-02 Ziel-Streifen | `Ziele {n}` |
| B-02 Hinweis F-02 | `{Mittel} stützt Ziel {…} · stört Ziel {…}` mit `Behalten` / `Doch entfernen` |
| B-02 Ziehen gesperrt | `Reihenfolge lässt sich nur in der Ansicht „Einnahme" ändern.` |
| B-02 leer | `Noch kein Mittel in diesem Stack.` |
| B-04 Knöpfe | `Ordnen` / `Fertig` |
| B-04 leer | `Erst Ziele anlegen` |
| B-05 Vorschau | `{Stückzahl} × {Menge} {Einheit} = {Gesamt} {Einheit}` |
| B-05 Stammdaten-Hinweis | `Gilt in {n} Stacks` |
| B-06 Schalter | `Auch neutrale zeigen` |
| B-06 ohne Bewertung | `Dieser Stack wurde noch nicht ausgewertet.` |
| B-07 Metazeile | `ausgewertet {Datum} {Uhrzeit} · {Modell} · {Denkstufe}` |
| B-08 leer | `Ohne eigene Fragen antwortet die Auswertung allgemein.` |
| B-09 Überschriften | `Tagesgesamtdosis` / `Konkurrenzen über Stacks hinweg` |
| B-10 Rubriken | `Vorlesen` · `Codex` · `Daten` · `Darstellung` |
| B-10 Warnung | `Der Startbestand überschreibt alle vorhandenen Stacks. Fortfahren?` |
| B-11 | `Öffne diese Seite und gib den Code ein:` / `Seite öffnen` |
| B-12 Titel | `Ziele ordnen — {Stack}` |
| Ampel grau | `nicht bedient` |
| Fehler REAUTH | `Anmeldung abgelaufen.` mit `Neu anmelden` |
| Fehler QUOTA | `Kontingent erschöpft. Wieder verfügbar in {Zeit}.` mit `Später erneut` |
| Fehler NETWORK | `Keine Verbindung.` mit `Erneut versuchen` |
| Offline-Hinweis | `Ohne Netz — Ampeln und Häkchen funktionieren weiter.` |
| Rückgängig | `Entfernt` mit `Rückgängig` |

Platzhalter stehen in geschweiften Klammern und werden zur Laufzeit ersetzt.

## 9. Barrierefreiheit

Die App ist nur für Frank; es gelten keine Store-Vorgaben. Trotzdem verbindlich:
- **Mindest-Tippfläche 44 × 44 dp** für jedes Bedienelement — auch dort, wo das sichtbare
  Element kleiner ist (Häkchen 22 dp, Ziehgriff 24 dp, Löslichkeitspunkt 8 dp).
- **Kontrast**: Text mindestens 4,5:1, Flächenfarben mindestens 3:1 — siehe die Tabellen in §2.
- **Farbe ist nie das einzige Merkmal.** Jede Ampel trägt zusätzlich den Kurzgrund als Text
  („stört 3, 7"), jede graue Ampel das Wort „nicht bedient". Der fettlösliche Punkt ist hohl,
  der wasserlösliche gefüllt — auch ohne Farbunterschied unterscheidbar.
- **Große Systemschrift**: Bis 130 % müssen alle Zeilen lesbar bleiben. Der Mittel-Eintrag darf
  dabei auf 64 dp wachsen; der Name bekommt dann eine zweite Zeile, statt abgeschnitten zu werden.
- Jedes Symbol ohne Beschriftung trägt eine Vorlesebeschreibung.

## 10. Zweispaltiges Layout (Innendisplay, 440 × 583 dp)

| Bildschirm | Aufteilung |
|---|---|
| B-01 | Kartenraster 2 × 212 dp — alle sechs Stacks ohne Scrollen |
| B-02 | Links Ziele 176 dp **dauerhaft offen** (die Überlagerung entfällt), rechts Mittel-Liste + Auswertung 264 dp |
| B-09 | Links Tagesgesamtdosis, rechts Konkurrenzen |
| B-10 | Links Rubriken 160 dp, rechts der Inhalt |
| B-03, B-07, B-11, B-12, B-14 | einspaltig, zentriert auf max 440 dp |
| Blätter (B-04, B-05, B-06, B-08, B-13, B-15) | bleiben Blätter, Breite gedeckelt auf 400 dp |

## 11. Offene Fragen

Keine über die in `00-PROJEKT.md` §6 genannten hinaus. Alle gestalterischen Aussagen dieses
Dokuments sind ohnehin Absicht und werden vom Entwurf des Designers überholt.
