# Motion-Spec — Gedankenspeicher
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/Gedankenspeicher/v2/messung/`. Widerspricht ein Satz
> von hier der Messung, ist der Satz überholt — nicht die Messung falsch.

## 1. Bewegungs-Grundhaltung

Zügig und weich. Alles startet sofort und bremst sanft aus — nichts zuckt, nichts lässt
warten. Grunddauer **240 ms**, Grundkurve **`cubic-bezier(0.2, 0, 0, 1)`**.

Frank spricht Notizen oft unterwegs und schnell hintereinander ein. Eine Bewegung, die ihn
aufhält, wäre schlimmer als gar keine; eine, die ruckt, ließe die App billig wirken. Deshalb
dieser Kompromiss: schnell genug, um nie im Weg zu sein, weich genug, damit die schwebenden
Karten und Glasleisten ihre Tiefe behalten.

**Was sich niemals bewegt:**

1. **Der Notiztext und der KI-Antworttext beim Lesen.** Kein Einfliegen von Buchstaben, kein
   Wandern, kein Umbrechen. Steht der Text, steht er still.
2. **Die Reihenfolge der Karten.** Eine Karte, die sich füllt (Transkription, Nachreichen),
   bleibt an ihrer Stelle und springt nicht ans Ende.
3. **Die Kopfleiste.** Sie steht fest, sie fährt beim Scrollen weder weg noch ein.

## 2. Kurven und Dauern

| Name | Dauer | Kurve | Wofür |
|------|-------|-------|-------|
| `standard` | 240 ms | `cubic-bezier(0.2, 0, 0, 1)` | Der Regelfall: Erscheinen, Verschwinden, Wechsel |
| `kurz` | 120 ms | `cubic-bezier(0.2, 0, 0, 1)` | Rückmeldung auf Bedienung, Symbolwechsel |
| `blatt` | 320 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Blätter und Schublade — etwas länger, weil mehr Fläche bewegt wird |
| `weich` | 400 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Erscheinungswechsel, Farbüberblendungen |
| `puls` | 1600 ms | `cubic-bezier(0.4, 0, 0.6, 1)`, endlos | Aufnahmering |
| `wandern` | 2400 ms | `linear`, endlos | Leuchten auf wartenden Karten und Platzhaltern |

Diese sechs Namen werden überall referenziert. Es wird keine siebte Dauer erfunden.

## 3. Bewegungen im Einzelnen

### M-01 — Notizkarte erscheint
- **Wo** — B-01, Verlaufsliste
- **Auslöser** — Eine neue Notiz entsteht (F-01, F-02)
- **Was sich ändert** — `translateY` 16 dp → 0 dp · `opacity` 0 → 1 · `scale` 0,97 → 1,0
- **Dauer / Kurve** — `standard` · keine Verzögerung · einmalig
- Die Liste scrollt gleichzeitig um die Kartenhöhe nach, mit derselben Dauer und Kurve, damit
  die neue Karte sichtbar wird, ohne dass zwei Bewegungen gegeneinander laufen.

### M-02 — Schublade öffnet und schließt
- **Wo** — B-02
- **Auslöser** — Schubladensymbol, Wischen von links, Tipp auf die abgedunkelte Fläche
- **Was sich ändert** — `translateX` −280 dp (Cover) bzw. −320 dp (Innen) → 0 dp; die
  abgedunkelte Fläche über B-01: `opacity` 0 → 0,52
- **Dauer / Kurve** — `blatt` · Schließen mit derselben Dauer rückwärts
- Beim Ziehen mit dem Finger folgt die Schublade unmittelbar; beim Loslassen läuft sie mit
  `blatt` in die nähere Endlage.

### M-03 — KI-Blatt fährt herein
- **Wo** — B-03
- **Auslöser** — KI-Knopf (F-09)
- **Was sich ändert** — `translateY` 100 % → 0 % · abgedunkelte Fläche `opacity` 0 → 0,52 ·
  B-01 dahinter `scale` 1,0 → 0,98 und Unschärfe 0 → 6 px
- **Dauer / Kurve** — `blatt`
- Beim Schließen rückwärts, in derselben Dauer.

### M-04 — Karte verschwindet
- **Wo** — B-01
- **Auslöser** — Notiz löschen oder verschieben (F-08)
- **Was sich ändert** — `opacity` 1 → 0 · `scale` 1,0 → 0,94 · danach schließt sich die
  Lücke: Höhe → 0 dp
- **Dauer / Kurve** — `standard` für das Verblassen, danach `standard` für das Schließen der
  Lücke — nacheinander, nicht gleichzeitig, sonst wirkt es hektisch

### M-05 — Aufnahmering pulsiert *(Dauerbewegung)*
- **Wo** — B-01, Aufnahmeknopf
- **Auslöser** — Aufnahme läuft (F-01)
- **Was sich ändert** — Die Aura um den Knopf: `scale` 1,0 → 1,22, `opacity` 0,50 → 0,12.
  **Die Amplitude folgt der gemessenen Lautstärke:** bei Stille bleibt sie bei `scale` 1,05,
  bei lautem Sprechen erreicht sie 1,22. So sieht Frank, dass das Mikrofon ihn hört.
- **Dauer / Kurve** — `puls`, endlos, bis die Aufnahme endet
- Beim Beenden läuft die Aura in einer `standard`-Bewegung auf `scale` 1,0 / `opacity` 0 aus.

### M-06 — Aufnahmeknopf wechselt sein Symbol
- **Wo** — B-01
- **Auslöser** — Aufnahme startet oder endet; Text im Feld erscheint oder verschwindet
- **Was sich ändert** — Das alte Symbol `rotate` 0° → −90° und `opacity` 1 → 0, das neue
  gleichzeitig `rotate` 90° → 0° und `opacity` 0 → 1
- **Dauer / Kurve** — `kurz`

### M-07 — Wanderndes Leuchten *(Dauerbewegung)*
- **Wo** — B-01, entstehende KI-Antwortkarte; Notizkarte während der Textverbesserung
- **Auslöser** — Codex arbeitet (F-07, F-09)
- **Was sich ändert** — Ein linearer Verlauf aus `akzentGedeckt` (Breite 40 % der Karte)
  wandert von `-40 %` nach `140 %` über die Kartenfläche
- **Dauer / Kurve** — `wandern`, endlos, bis die Antwort da ist
- Trifft der Text ein, hört das Leuchten am Ende seines laufenden Durchgangs auf — es wird
  nicht mitten in der Fläche abgeschnitten.

### M-08 — Überschrift erscheint
- **Wo** — B-01, Kopfzeile einer Notizkarte
- **Auslöser** — Die KI-Überschrift trifft ein (F-05)
- **Was sich ändert** — Die Uhrzeit als Platzhalter `opacity` 1 → 0, die Überschrift
  gleichzeitig `opacity` 0 → 1 und `translateY` 4 dp → 0 dp
- **Dauer / Kurve** — `standard`
- Der Zeitstempel rechts bleibt dabei unangetastet — er bewegt sich nicht.

### M-09 — Vorlese-Absatz wandert mit *(Dauerbewegung)*
- **Wo** — B-01, Notiz- und KI-Antwortkarten
- **Auslöser** — Vorlesen läuft (F-06)
- **Was sich ändert** — Der gerade gesprochene Absatz bekommt `akzentGedeckt` als
  Hintergrund mit 8 dp Radius. Beim Wechsel zum nächsten Absatz verblasst die Hervorhebung
  am alten und erscheint am neuen.
- **Dauer / Kurve** — `standard` je Wechsel
- Läuft der Absatz aus dem Bild, scrollt die Liste ihn sanft in die Mitte — mit `weich`, und
  nur wenn Frank nicht selbst gerade scrollt.

### M-10 — Erscheinung wechselt
- **Wo** — überall
- **Auslöser** — Kachel in B-04 (F-15)
- **Was sich ändert** — Alle Farbwerte überblenden von alt nach neu
- **Dauer / Kurve** — `weich`
- Kein Wischen, kein Aufziehen, kein Kreis, der sich ausbreitet — nur eine Überblendung.

### M-11 — Suchtreffer leuchtet auf
- **Wo** — B-01, nach dem Sprung aus B-07
- **Auslöser** — Frank tippt einen Suchtreffer an (F-14)
- **Was sich ändert** — Die Zielkarte: Hintergrund `hintergrundErhoben` → `akzentGedeckt` →
  `hintergrundErhoben`
- **Dauer / Kurve** — 240 ms hin (`standard`), 200 ms Halten, 400 ms zurück (`weich`)

### M-12 — Häkchen springt um
- **Wo** — B-06, Profilzeilen
- **Auslöser** — Frank tippt ein Häkchenfeld an (F-10)
- **Was sich ändert** — Das alte Häkchen `scale` 1,0 → 0 und `opacity` 1 → 0; das neue
  `scale` 0 → 1,0 mit einem leichten Überschwingen auf 1,12 und zurück; gleichzeitig
  wandert der Rand in `akzent` von der alten zur neuen Zeile (`opacity`-Überblendung)
- **Dauer / Kurve** — `kurz` für das Verschwinden, 240 ms `cubic-bezier(0.34, 1.56, 0.64, 1)`
  für das Erscheinen

## 4. Bildschirmwechsel

| Von | Nach | Art | Dauer | Kurve |
|-----|------|-----|-------|-------|
| B-01 | B-02 | Schublade schiebt von links herein (M-02) | 320 ms | `blatt` |
| B-02 | B-01 | Schublade schiebt nach links hinaus | 320 ms | `blatt` |
| B-01 | B-03 | Blatt fährt von unten herein (M-03) | 320 ms | `blatt` |
| B-03 | B-01 | Blatt fährt nach unten hinaus | 320 ms | `blatt` |
| B-01 / B-02 | B-04 | Schiebt von rechts herein, B-01 gleichzeitig 12 % nach links und `opacity` 1 → 0,6 | 240 ms | `standard` |
| B-04 | B-01 | Rückwärts, gleiche Werte | 240 ms | `standard` |
| B-04 | B-05, B-06 | Schiebt von rechts herein | 240 ms | `standard` |
| B-05, B-06 | B-04 | Rückwärts | 240 ms | `standard` |
| B-01 | B-07 | Überblenden mit `scale` 1,04 → 1,0; das Suchfeld erhält sofort den Fokus | 240 ms | `standard` |
| B-07 | B-01 | Überblenden mit `scale` 1,0 → 1,04 rückwärts | 240 ms | `standard` |
| B-01 | B-08 | Blatt fährt von unten herein | 320 ms | `blatt` |
| B-08 | B-01 | Blatt fährt nach unten hinaus | 320 ms | `blatt` |

Der Rücklauf ist überall der exakte Rückwärtsgang des Hinlaufs, in derselben Dauer.

## 5. Rückmeldung auf Bedienung

| Element | Was passiert | Dauer |
|---------|-------------|-------|
| Aufnahmeknopf | `scale` 1,0 → 0,92 beim Drücken, zurück beim Loslassen; zusätzlich eine kurze Vibration (`EFFECT_TICK`) beim Start **und** beim Ende der Aufnahme | `kurz` |
| KI-Knopf | `scale` 1,0 → 0,94, Rand hellt auf `akzent` +12 % auf | `kurz` |
| Gefüllte Knöpfe („Auswerten", „Speichern") | `scale` 1,0 → 0,97, Fläche dunkelt um 8 % ab | `kurz` |
| Kartensymbole (Lautsprecher, Verbessern) | Kreisförmige Welle in `akzentGedeckt` vom Berührungspunkt aus, 36 dp Durchmesser | 240 ms |
| Notizkarte (langer Druck) | `scale` 1,0 → 0,98 nach 180 ms Haltedauer, gleichzeitig eine Vibration (`EFFECT_HEAVY_CLICK`), danach öffnet das Menü | 180 ms Halten, dann `kurz` |
| Sitzungszeile | Fläche geht auf `akzentGedeckt`, kein Skalieren | `kurz` |
| Wahlfelder und Schalter in B-04 | Der Punkt gleitet, die Fläche überblendet | `standard` |
| Wischen an einer Sitzungszeile | Die Zeile folgt dem Finger; ab 96 dp erscheint dahinter das Löschsymbol in `fehler` | folgt dem Finger |

## 6. Dauerbewegung

Genau drei Dinge bewegen sich ohne Zutun — **sonst nichts**. Der Verlauf bleibt beim Lesen
still.

| Bewegung | Wann | Periode |
|----------|------|---------|
| M-05 Aufnahmering | nur während der Aufnahme | 1600 ms |
| M-07 Wanderndes Leuchten | nur während Codex arbeitet | 2400 ms |
| M-09 Vorlese-Hervorhebung | nur während vorgelesen wird | folgt den Absätzen |

Kein Atmen im Ruhezustand, kein wanderndes Hintergrundlicht, kein pulsierender Rand an
Karten, die nur dastehen.

## 7. Lade- und Wartezustände

| Lage | Ab wann | Was | Wie es verschwindet |
|------|---------|-----|--------------------|
| Sitzung wird geladen | sofort | Drei Platzhalterkarten mit `wandern`-Schimmer | Überblenden auf die echten Karten, `standard` |
| Notiz wird transkribiert | sofort nach dem Aufnahmeende | Drei Punkte in der Karte, die nacheinander auf `opacity` 1 gehen (je 160 ms versetzt, Periode 1200 ms) | Der Text erscheint mit `standard`-Überblendung an ihrer Stelle |
| Rückfrage wird geholt (B-03) | sofort | Dieselben drei Punkte an Stelle der Frage | Die Frage überblendet ein, `standard` |
| Auswertung läuft | sofort | Leere KI-Antwortkarte mit M-07 | Der Text erscheint absatzweise, jeder Absatz mit M-01 |
| Gerätecode wird geholt (B-05) | sofort | Platzhalterblock mit `wandern`-Schimmer | Der Code überblendet ein |
| Sprachprobe wird geholt (B-04) | nach 400 ms | Kleiner Kreisel im Knopf | Verschwindet, wenn der Ton beginnt |
| Suche läuft | nach 200 ms | Schmaler unbestimmter Balken (2 dp) unter dem Suchfeld | Verblasst mit `kurz` |

Ein Ladezustand, der kürzer als 200 ms dauern würde, wird gar nicht erst gezeigt — sonst
blitzt er nur auf und macht die App unruhig. Ausgenommen sind die Fälle, die oben mit
„sofort" stehen: dort ist von vornherein mit längerer Wartezeit zu rechnen.

## 8. Reduzierte Bewegung

Meldet das System „Bewegung reduzieren" (`ANIMATOR_DURATION_SCALE == 0` oder die
Barrierefreiheits-Einstellung):

1. **Alle Dauerbewegungen sind aus.** M-05 zeigt statt des Pulsierens einen statischen Ring
   in voller Deckkraft, solange aufgenommen wird. M-07 zeigt eine ruhige Fläche in
   `akzentGedeckt` statt des wandernden Verlaufs. M-09 hebt den Absatz weiterhin hervor —
   das ist eine Information, keine Zierde —, aber ohne Überblendung: die Hervorhebung
   springt.
2. **Alle Übergänge werden zu reinem Überblenden.** Kein Schieben, kein Skalieren, kein
   Rotieren. Die Schublade und die Blätter blenden ein und aus, statt zu fahren.
3. **Alle Dauern werden halbiert:** `standard` 120 ms, `kurz` 60 ms, `blatt` 160 ms,
   `weich` 200 ms.
4. **Die Vibrationen bleiben.** Sie sind Rückmeldung, keine Bewegung, und beim Aufnehmen die
   einzige Bestätigung, die auch ohne Hinsehen ankommt.
5. Die Schimmer der Platzhalter werden zu einer ruhigen Fläche in `akzentGedeckt`.

## 9. Offene Fragen

Siehe `00-PROJEKT.md` §6. Im Bewegungsbereich ist nichts offen.
