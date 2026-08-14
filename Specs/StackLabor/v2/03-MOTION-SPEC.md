# Motion-Spec — StackLabor
Stand: 14.08.2026, 14:25 · Stufe: v2 · Plattform(en): Android

> **Alle Werte sind im Entwurf GEMESSEN.** Quelle je Bewegung ist der `@keyframes`-Name bzw. die
> `animation`/`transition`-Angabe aus `Specs/StackLabor/v2/messung/<erscheinung>/<B-xx>.json`.
> Was hier steht, ist nachprüfbar — nichts davon ist geschätzt.

## 1. Bewegungs-Grundhaltung

Das Innendisplay läuft mit 120 Hz. Der Entwurf nutzt das: 15 Keyframes, ein dauerhaft wandernder
Kopfverlauf, gestaffelte Listen-Einblendungen und ein atmender Plus-Knopf. Das Grundtempo für
alles, was auf eine Bedienung antwortet, liegt bei **220–320 ms**; die Leitkurve ist
`cubic-bezier(0.2, 0, 0, 1)`.

Zwei Bewegungen tragen **Bedeutung** statt Schmuck und sind deshalb unantastbar:
der **Ampel-Wechsel** (M-07) und das **Ziehen der Ziele** (M-01 … M-06).

### Was sich NIEMALS bewegen darf
- **Ampelfarben im Ruhezustand** — kein Blinken, kein Pulsieren. Ausnahmen sind allein die
  bewusst gesetzte Aura an roten Ampeln (M-21) und der einmalige Puls nach einer Änderung (M-09).
- **Dosis- und Einheitszahlen.**
- **Das Häkchen-Kästchen selbst** — es wechselt den Zustand ohne Sprung oder Skalierung.
- **Die Ziel-Nummern** außerhalb einer aktiven Umsortierung.
- **Der Begründungstext** bei Rot oder Gelb — er klappt auf und steht dann still.

## 2. Kurven und Dauern — gemessen

| Name | Dauer | Kurve | Wofür |
|---|---|---|---|
| `antwort` | 220 ms | `cubic-bezier(0.2, 0, 0, 1)` | Ausweichen, Umordnen, Listen-Einblendung (`rein`) |
| `wechsel` | 300 ms | `cubic-bezier(0.2, 0, 0, 1)` | Bildschirmwechsel (`vor`, `zurueck`) |
| `zustand` | 320 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Ampel-Überblendung, Flächenwechsel |
| `blatt` | 300 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Blätter von unten (`blatt`) |
| `vollbild` | 320 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Vollbild von unten (`hoch`) |
| `erscheinung` | 420 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Hell/Dunkel — als `transition` auf `background-color`, `color`, `border-color` |
| `griff` | 220 ms | `cubic-bezier(0.2, 0, 0, 1)` | `transform` beim Ziehen |
| `atem` | 2400 ms | `ease-in-out`, endlos | Aura an roten Ampeln |
| `glanz` | 8000 ms | `linear`, endlos | Glanzkante an Karten |
| `fabatem` | 3200 ms | `ease-in-out`, endlos | Plus-Knopf |
| `wandern` | 30 000 ms | `linear`, endlos | Kopfverlauf B-01 |
| `ring` | 1600 ms | `ease-out`, endlos | Pulsring an der Wartezeile (B-11) |
| `puls` | 520 ms | — | einmaliger Ring an geänderten Ampeln |
| `schimmer` | 1400 ms | `linear`, endlos | Ladeplatzhalter |
| `m12` | 1600 ms | endlos | entsättigtes Pulsieren während der Auswertung |
| `pegel` | — | endlos | Pegelbalken beim Vorlesen |

## 3. Bewegungen im Einzelnen

Jede Zeile nennt ihre **Quelle** in der Messung.

### M-01 — Ziel aufnehmen
**Wo** B-12 · **Auslöser** langes Drücken 300 ms · **Ändert** Skalierung 1,0 → 1,04, Tiefe 1 → 8 dp, haptischer Impuls · **Quelle** `transition: transform 0.22s cubic-bezier(0.2,0,0,1)`

### M-02 — Ziele weichen aus
**Wo** B-12 · **Auslöser** Ziehen über eine Nachbarzeile · **Ändert** Y-Verschiebung um die Zeilenhöhe (40 dp) · **Dauer/Kurve** 220 ms `antwort`, je Zeile versetzt · **Quelle** `transition: transform 0.22s cubic-bezier(0.2,0,0,1)`

### M-03 — Nummern laufen live mit
**Wo** B-12 · **Auslöser** jeder Positionswechsel während des Ziehens · **Ändert** die Ziffer im Nummernkreis, Überblendung + 6 dp Y-Versatz · **Dauer** 120 ms linear
**Begründung** Frank sieht die neue Priorität, während er noch entscheidet.

### M-04 — Loslassen und Einrasten
**Wo** B-12 · **Ändert** Position rastet ein, Skalierung 1,04 → 1,0, Tiefe 8 → 1 dp, haptischer Impuls · **Dauer** 260 ms, Feder (Dämpfung 0,75 / Steifigkeit 380)

### M-05 — Automatisches Weiterrollen
**Wo** B-12 · **Auslöser** Ziehen in die Randzone · **Werte** Randzone 64 dp, Geschwindigkeit linear 0 → 900 dp/s, Start nach 120 ms Verweilen
**Begründung** Ohne dies wäre Position 28 → 1 nicht in einem Zug möglich.

### M-06 — Abbruch
**Wo** B-12, B-02 · **Ändert** Rückflug zur Ausgangsposition, Tiefe 8 → 1 dp · **Dauer** 300 ms `antwort`

### M-07 — Ampel-Überblendung
**Wo** B-01, B-02, B-04, B-12 · **Auslöser** F-14 · **Ändert** die Farbe des Kantenbalkens, **nie hart** · **Dauer/Kurve** 320 ms `cubic-bezier(0.4, 0, 0.2, 1)` · **Quelle** `transition: background-color 0.32s cubic-bezier(0.4,0,0.2,1)` (306 Vorkommen gemessen)

### M-08 — Ampeln gestaffelt
**Wo** B-02, B-04 · **Auslöser** mehrere Ampeln wechseln gleichzeitig · **Werte** Versatz 30 ms je Zeile (gemessen an der `rein`-Staffelung: 0,03 s · 0,06 s · 0,09 s · 0,12 s · 0,15 s), gedeckelt bei 10 Stufen
**Begründung** Die Welle folgt der Prioritätsreihenfolge.

### M-09 — Puls nur an geänderten Ampeln
**Wo** B-02, B-04 · **Auslöser** F-14, nur für tatsächlich geänderte Ampeln · **Ändert** Ring: Deckkraft 0,55 → 0, Skalierung 1 → 2,6 · **Dauer** 520 ms · **Quelle** `@keyframes puls { 0% { opacity:.55; transform:scale(1) } 100% { opacity:0; transform:scale(2.6) } }`

### M-10 — Verbindungsfarbe Mittel ↔ Ziel
**Wo** B-06 · **Auslöser** Öffnen der Aufschlüsselung · **Ändert** Gegenstand und betroffene Zeilen tragen 900 ms lang einen 2 dp Rand in derselben Farbe

### M-11 — Warte-Schimmer
**Wo** B-02, B-07, B-09 · **Auslöser** F-12/F-13 laufen · **Ändert** ein Lichtstreifen wandert über die Platzhalter, X von −140 % auf 240 % · **Dauer** 1400 ms linear, endlos · **Quelle** `@keyframes schimmer { 0% { transform: translateX(-140%) } 100% { transform: translateX(240%) } }`

### M-12 — Ampeln entsättigt und pulsierend
**Wo** B-02, B-04, B-09 · **Auslöser** solange eine Auswertung läuft · **Ändert** Deckkraft 1 → 0,45 → 1, dazu entsättigt · **Dauer** 1600 ms, endlos · **Quelle** `@keyframes m12 { 0% { opacity:1 } 50% { opacity:.45 } 100% { opacity:1 } }`
**Begründung** Der wichtigste Wartezustand: Frank sieht, dass die Farben **gerade nicht gelten**.

### M-13 — Streamender Antworttext
**Wo** B-07, als Auszug auf B-02 · **Ändert** der Text erscheint wortweise, je Wort Einblenden + 6 dp Aufwärtsbewegung; darüber wechselt die Fortschrittserzählung · **Dauer** je Wort 180 ms, Erzähler-Wechsel 240 ms
**Die Erzählung ist im Entwurf hinterlegt:** „stelle den Stack zusammen …" · „prüfe Wechselwirkungen …" · „gewichte Ziel 1–4 …" · „summiere die Wirkstoffmengen …" · „formuliere die Begründungen …" · „ordne die Ampeln zu …"

### M-14 — Ziel-Überlagerung aufklappen
**Wo** B-02 → B-04 · **Ändert** Höhe 0 → max 281 dp, Zeilen gestaffelt, Pfeil dreht 180° · **Dauer** 380 ms `cubic-bezier(0.2, 0, 0, 1)`

### M-15 — Sprech-Markierung und Pegel
**Wo** B-07 · **Auslöser** F-16 läuft · **Ändert** der gesprochene **Absatz** bekommt eine Hintergrundfläche, deren Kante mitgleitet; am Knopf schwingen drei Balken · **Quelle** `@keyframes pegel { 0% { height:4px } 50% { height:14px } 100% { height:5px } }`

### M-16 — Dauerbewegung
**Wo** B-01, B-02
- **Kopfverlauf** — `background-position` 0 % → 100 % → 0 %, **30 s linear endlos** · `@keyframes wandern`
- **Glanzkante** — X von −60 % auf 320 %, Neigung −18°, Deckkraft 0 → 0,5 → 0, **8 s linear endlos** · `@keyframes glanz`
- **Plus-Knopf** — Skalierung 1,0 → 1,02 → 1,0, **3,2 s ease-in-out endlos** · `@keyframes fabatem`

Alle Perioden liegen bei **3 Sekunden oder darüber** (außer der Aura, die Warnzeichen ist).

### M-17 — Faltvorgang
**Wo** alle Bildschirme · **Auslöser** Auf- oder Zuklappen · **Ändert** gemeinsame Elemente wandern; die zweite Spalte schiebt von rechts ein (X + 24 dp → 0, Deckkraft 0 → 1, 300 ms, 100 ms verzögert) · **Dauer** 400 ms `antwort`
**Regel** Scrollposition, geöffnete Blätter und laufende Vorgänge bleiben erhalten.
*(Im Entwurf nicht gebaut — Absicht aus v1, siehe `02-UI-SPEC.md` §10.)*

### M-18 — Mittel-Reihenfolge ziehen
**Wo** B-02, **nur** in der Ansicht „Einnahme" · wie M-01 … M-06
**Regel** In der Ansicht „Löslichkeit" öffnet langes Drücken stattdessen das Kontextmenü (F-31) — es gibt keinen toten Ziehversuch.

### M-19 — Erscheinungswechsel
**Wo** die ganze App · **Auslöser** F-22 · **Ändert** `background-color`, `color` und `border-color` aller Elemente gleichzeitig · **Dauer/Kurve** **420 ms `cubic-bezier(0.4, 0, 0.2, 1)`** · **Quelle** `transition: background-color .42s …, color .42s …, border-color .42s …` (8775 Vorkommen gemessen)
**Regel** Offene Blätter bleiben offen und blenden mit.

### M-20 — Blatt öffnen und schließen
**Wo** B-04, B-05, B-06, B-08, B-13, B-15 · **Ändert** Y von 100 % auf 0, dahinter dunkelt der Grund ab · **Dauer** 300 ms `cubic-bezier(0.05, 0.7, 0.1, 1)` · **Quelle** `@keyframes blatt`

### M-21 — Atmende Aura an roten Ampeln
**Wo** B-01, B-02 · **Auslöser** eine Ampel steht auf Rot · **Ändert** Deckkraft 0,30 → 0,85 → 0,30 bei gleichzeitiger Skalierung 1 → 1,9 → 1 · **Dauer** **2400 ms ease-in-out endlos** · **Quelle** `@keyframes atem`
**Regel** Höchstens drei gleichzeitig — die mit dem höchsten Rang.
Derselbe `ring`-Keyframe (Skalierung 0,8 → 1,6, Deckkraft 0,7 → 0, 1,6 s) dient als Pulsring an der Wartezeile von B-11.

### M-22 — Gestaffeltes Einblenden beim Öffnen
**Wo** B-01, B-02, B-03, B-14 · **Ändert** Y von 12 dp auf 0, Deckkraft 0 → 1 · **Dauer** 220 ms `cubic-bezier(0.2, 0, 0, 1)` `both` · **Versatz** 30–40 ms je Element (gemessen: 0,03 · 0,04 · 0,06 · 0,08 · 0,09 · 0,12 · 0,15 · 0,16 · 0,2 s) · **Quelle** `@keyframes rein`

### M-23 — Häkchen-Rückmeldung
**Wo** B-02 · **Auslöser** F-05 · **Ändert** das Kästchen selbst bewegt sich **nicht**; stattdessen haptischer Impuls und Flächenwechsel auf den ausgegrauten Zustand · **Dauer** 220 ms `antwort`

### M-24 — Wischen zum Entfernen
**Wo** B-02, B-03, B-08, B-14 · **Ändert** der Eintrag folgt dem Finger, dahinter erscheint eine rote Fläche; nach dem Loslassen gleitet er hinaus, die Lücke schließt sich, die Rückgängig-Leiste fährt von unten ein · **Dauer** Ausgleiten 220 ms, Leiste 300 ms `blatt`, sichtbar 6 s · **Quelle** `@keyframes blatt` an der Leiste

## 4. Bildschirmwechsel — gemessen

| Von | Nach | Art | Dauer | Quelle |
|---|---|---|---|---|
| beliebig | vorwärts (B-01 → B-02 usw.) | X von +32 dp auf 0, Deckkraft 0 → 1 | **300 ms** `cubic-bezier(0.2,0,0,1)` | `@keyframes vor` |
| beliebig | zurück | X von −24 dp auf 0, Deckkraft 0 → 1 | **300 ms** `cubic-bezier(0.2,0,0,1)` | `@keyframes zurueck` |
| B-02 | B-07, B-12 (Vollbild) | Y von +48 dp auf 0, Deckkraft 0 → 1 | **320 ms** `cubic-bezier(0.05,0.7,0.1,1)` | `@keyframes hoch` |
| beliebig | Blatt | Y von 100 % auf 0 | **300 ms** `cubic-bezier(0.05,0.7,0.1,1)` | `@keyframes blatt` |

Der Rücklauf bewegt sich über die kürzere Strecke (24 dp statt 32 dp) — er fühlt sich dadurch
schneller an, ohne eine andere Dauer zu brauchen.

## 5. Rückmeldung auf Bedienung

| Element | Rückmeldung | Quelle |
|---|---|---|
| Knopf | Fläche dunkelt ab, Skalierung 0,98 | `transition: background-color 0.18s` |
| Karte | Wellenring vom Berührungspunkt | Material-Standard |
| Häkchen | keine Bewegung des Kästchens, nur Haptik + Flächenwechsel | M-23 |
| Chip | Füllung wechselt | `transition: background-color 0.32s cubic-bezier(0.4,0,0.2,1)` |
| Langes Drücken | M-01 (Einnahme-Ansicht) bzw. Kontextmenü (Löslichkeits-Ansicht) | F-31 |
| Ziehen | `transform 0.22s cubic-bezier(0.2,0,0,1)` | gemessen, 204 Vorkommen |

**Haptik:** leicht beim Häkchen und beim Einrasten · mittel beim Aufnehmen · schwer beim
Auslösen des Wischen-Löschens · doppelt, wenn eine Auswertung eine **neue rote** Ampel ergibt.

## 6. Dauerbewegung

Siehe M-16 (Kopfverlauf 30 s · Glanzkante 8 s · Plus-Knopf 3,2 s) und M-21 (Aura 2,4 s).
Sonst steht alles still.

## 7. Lade- und Wartezustände

| Fall | Was sich zeigt | Ende |
|---|---|---|
| Auswertung (F-12, F-13) | M-11 Schimmer + M-12 entsättigte Ampeln + M-13 streamender Text mit Fortschrittserzählung | Schimmer wird vom Inhalt ersetzt; Ampeln sättigen sich über M-07/M-08/M-09 |
| Konkurrenzprüfung (F-02) | schmaler unbestimmter Balken unter dem Sockel, **ohne** die Bedienung zu blockieren | Hinweis-Schnipsel fährt über M-20 ein |
| Anmeldung (F-17) | Pulsring an der Wartezeile, 1600 ms | Wechsel auf „angemeldet" |
| Vorlesen (F-16) | drei Pegelbalken beginnen zu schwingen | erster Absatz wird markiert |
| Import/Export | bestimmter Fortschrittsbalken im Blatt | Meldung „Fertig" |

Dauert eine Auswertung länger als 45 Sekunden, ergänzt die Erzählung „Das dauert länger als
gewöhnlich." — abgebrochen wird nicht von selbst.

## 8. Reduzierte Bewegung

Der Entwurf hat den Schalter bereits: die Klasse `.reduz` setzt `animation: none !important` auf
**alle** Elemente. Für den Bau gilt die feinere Regel aus v1, weil zwei Bewegungen Bedeutung tragen:

**Bleibt erhalten:** M-07 Ampel-Überblendung (320 ms) · M-02 Ausweichen beim Ziehen (220 ms) ·
M-13 der streamende Text (das ist Inhalt, keine Animation).
**Wird abgeschaltet:** M-16 und M-21 vollständig · M-09 · M-10 · M-22 · M-11 wird eine ruhige
Fläche · M-12 wird ein **statischer** Graustand (die Aussage „gilt gerade nicht" bleibt).
**Auf 0 ms verkürzt:** alle Bildschirmwechsel, M-14, M-19, M-20, M-24.

## 9. Offene Fragen

Keine. Alle 15 Keyframes des Entwurfs sind erfasst; `sc-shine` gehört zur Bühne des Designers
und wird **nicht** gebaut (sie ist nicht Teil der App).
