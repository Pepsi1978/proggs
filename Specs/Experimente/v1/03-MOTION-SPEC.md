# Motion-Spec — Experimente

Stand: 09.08.2026 · Stufe: v1 · Plattform(en): Android

---

## 1. Bewegungs-Grundhaltung

**Grundtempo 240 ms.** Ruhig, nicht träge. Eine App, die man morgens und abends benutzt, darf
nicht hetzen — aber sie darf auch nicht schleppen, sonst wird die tägliche Bedienung zäh.

**Was sich niemals bewegt — die wichtigste Festlegung dieses Specs:**
Text im Logbuch (B-07, beide Reiter), in den Erkenntnissen (B-06) und in jeder Auswertung
(B-03, KI-Antwort) läuft **nicht** ein, blinkt nicht, schwebt nicht, wird nicht Buchstabe für
Buchstabe aufgebaut und bekommt keinen Auftritt. Er ist einfach da, sobald der Bildschirm da
ist. Das ist Franks Leben, das dort steht — es braucht keine Inszenierung. Ebenso unbewegt
bleiben Zahlen und Daten in JetBrains Mono: sie zählen nie hoch, sie stehen.

Alles andere darf sich bewegen, aber sparsam: **genau eine** Dauerbewegung in der ganzen App.

---

## 2. Kurven und Dauern

Diese Namen werden überall sonst nur noch referenziert, nie neu erfunden.

| Name | Dauer | Kurve | Wofür |
|------|-------|-------|-------|
| `ruhig` | 240 ms | `cubic-bezier(0.2, 0, 0, 1)` | Standard: Erscheinen, Hereinschieben |
| `knapp` | 120 ms | `cubic-bezier(0.3, 0, 0.8, 0.15)` | Druckrückmeldung, Verschwinden |
| `weich` | 400 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | große Flächen, die Auswertung |
| `blenden` | 200 ms | `cubic-bezier(0.4, 0, 0.6, 1)` | Bildschirmwechsel über die untere Leiste |
| `hinaus` | 140 ms | `cubic-bezier(0.4, 0, 1, 1)` | Karten, die weggehen |
| `haken` | 180 ms | `cubic-bezier(0.2, 0, 0, 1)` | ein Haken zeichnet sich |
| `atmen` | 3200 ms | `ease-in-out`, endlos wechselnd | laufende Aufnahme |
| `wandern` | 1800 ms | `linear`, endlos | Wartezustand der KI |

---

## 3. Bewegungen im Einzelnen

### M-01 — Karte sinkt beim Drücken ein

- **Wo:** Jede Karte auf B-01, B-04, B-05, B-07; jede Zeile der To-Do-Liste.
- **Auslöser:** Finger berührt die Karte (Druck beginnt).
- **Was sich ändert:** Größe 100 % → 98 % (vom Mittelpunkt aus).
- **Dauer / Kurve:** 120 ms `knapp`. Beim Loslassen zurück in 120 ms `knapp`.
- **Verzögerung / Wiederholung:** keine / keine.

### M-02 — Sprechknopf atmet während der Aufnahme

- **Wo:** Sprechknopf groß (B-01, B-03, B-04, B-05, B-09) und klein (B-02).
- **Auslöser:** Die Aufnahme läuft.
- **Was sich ändert:** Ein Ring um den Knopf, 2 dp stark, Farbe *Aktion* bei 40 % Deckkraft:
  Größe 100 % → 108 % → 100 %, Deckkraft 40 % → 15 % → 40 %.
- **Dauer / Kurve:** 3200 ms `atmen`.
- **Verzögerung / Wiederholung:** keine / **endlos**, bis die Aufnahme endet.

### M-03 — Vibration bei Aufnahmebeginn und -ende

- **Wo:** Jeder Sprechknopf.
- **Auslöser:** Aufnahme startet; Aufnahme endet.
- **Was sich ändert:** Kurze Vibration (`HapticFeedbackConstants.CONFIRM`).
- **Dauer / Kurve:** systemgegeben.
- **Verzögerung / Wiederholung:** keine / keine.
- **Zweck:** Frank soll ohne Hinsehen wissen, dass der Knopf läuft.

### M-04 — Vorschlagskarten erscheinen gestaffelt

- **Wo:** B-01, Zustand `VORSCHLAEGE`.
- **Auslöser:** Die fünf Vorschläge liegen vor.
- **Was sich ändert:** Je Karte gleichzeitig: Position 8 dp unterhalb → 0 dp,
  Deckkraft 0 % → 100 %.
- **Dauer / Kurve:** je Karte 240 ms `ruhig`.
- **Verzögerung:** Karte 1 bei 0 ms, danach je **40 ms** Versatz (0 / 40 / 80 / 120 / 160 ms).
- **Wiederholung:** keine.

### M-05 — Vorschläge werden ausgetauscht

- **Wo:** B-01, beim Druck auf „Andere Vorschläge".
- **Auslöser:** F-04.
- **Was sich ändert:** Zwei Abschnitte nacheinander:
  1. **Hinaus:** alle fünf alten Karten gleichzeitig, Position 0 → 12 dp nach oben,
     Deckkraft 100 % → 0 %. 140 ms `hinaus`.
  2. **Herein:** die neuen fünf wie M-04.
- **Dauer / Kurve:** 140 ms `hinaus`, dann Wartezustand, dann 240 ms `ruhig` mit Staffelung.
- **Verzögerung / Wiederholung:** keine / keine.
- **Zweck:** Man muss sehen, dass wirklich getauscht wurde, nicht nur der Text sich änderte.

### M-06 — Haken zeichnet sich

- **Wo:** To-Do-Liste auf B-01.
- **Auslöser:** Tippen auf eine Aufgabe.
- **Was sich ändert:** Der Haken wird als Strich von links nach rechts gezeichnet (Pfadlänge
  0 % → 100 %); gleichzeitig füllt sich das Kästchen in *Erledigt*; direkt danach wechselt
  die Textfarbe der Zeile von *Text* auf *Blass*.
- **Dauer / Kurve:** 180 ms `haken` für Haken und Füllung, danach 120 ms `knapp` für die
  Textfarbe.
- **Verzögerung / Wiederholung:** Textfarbe 60 ms verzögert / keine.
- **Rücknahme:** derselbe Ablauf rückwärts, je 120 ms `knapp`.

### M-07 — Merken-Symbol füllt sich

- **Wo:** Vorschlagskarte auf B-01.
- **Auslöser:** Tippen auf das Lesezeichen.
- **Was sich ändert:** Das Linien-Symbol wird zum gefüllten; dabei kurz Größe 100 % → 115 %
  → 100 %.
- **Dauer / Kurve:** 180 ms `haken`.
- **Verzögerung / Wiederholung:** keine / keine.

### M-08 — Auswertung erscheint

- **Wo:** B-03, KI-Antwort; B-02, KI-Blase.
- **Auslöser:** Die Antwort ist da.
- **Was sich ändert:** Die Karte bzw. Blase: Höhe 0 → volle Höhe, Deckkraft 0 % → 100 %.
  **Der Text selbst bewegt sich nicht** — er ist mit der Karte sofort vollständig da.
- **Dauer / Kurve:** 400 ms `weich`.
- **Verzögerung / Wiederholung:** keine / keine.

---

## 4. Bildschirmwechsel

| Von → Nach | Art | Dauer | Kurve |
|-----------|-----|-------|-------|
| Untere Leiste: B-01 ↔ B-04 ↔ B-05 ↔ B-06 ↔ B-07 | reines Überblenden, **kein Schieben** | 200 ms | `blenden` |
| B-01 → B-02 (Gespräch) | von rechts hereinschieben, gleichzeitig Deckkraft 0 → 100 % | 240 ms | `ruhig` |
| B-01 → B-03 (Auswertung) | ebenso | 240 ms | `ruhig` |
| B-08 → B-09 (Selbstbild) | ebenso | 240 ms | `ruhig` |
| jeder Zurück-Weg | nach rechts hinausschieben, Deckkraft 100 → 0 % | 200 ms | `blenden` |
| B-07: Reiter „Letzte 15 Tage" ↔ „Langzeit" | reines Überblenden | 200 ms | `blenden` |
| Dialog öffnet (Löschen-Rückfrage) | Größe 96 % → 100 %, Deckkraft 0 → 100 % | 240 ms | `ruhig` |
| Dialog schließt | Größe 100 % → 96 %, Deckkraft 100 → 0 % | 140 ms | `hinaus` |

**Begründung für das Überblenden in der unteren Leiste:** Die fünf Bereiche liegen
gleichrangig nebeneinander, es gibt kein „tiefer" und kein „zurück". Ein Schieben würde eine
Reihenfolge behaupten, die es nicht gibt.

---

## 5. Rückmeldung auf Bedienung

| Element | Rückmeldung |
|---------|-------------|
| Karte, Listenzeile | M-01 (einsinken auf 98 %) |
| Gefüllter Knopf | Fläche wird 8 % dunkler, 120 ms `knapp` |
| Textknopf | Text wird 20 % heller/dunkler, 120 ms `knapp` |
| Symbol-Knopf | kreisförmige Fläche in *Aktion gedeckt* blendet auf, 120 ms `knapp` |
| Sprechknopf | M-02 (atmen) + M-03 (Vibration) |
| Aufgabe der To-Do-Liste | M-01, dann M-06 |
| Merken-Symbol | M-07 |
| Reiter auf B-07 | die vollrunde Fläche wandert in 200 ms `blenden` zum neuen Reiter |
| Wischen auf einem Merklisten-Eintrag | Die Zeile folgt dem Finger; ab 40 % Breite erscheint der Löschen-Hintergrund |

**Kein Ripple.** Material-Standard-Ripple wird abgeschaltet — es passt nicht zur ruhigen
Grundhaltung. Die Rückmeldung erfolgt über Größe und Fläche.

---

## 6. Dauerbewegung

**Genau eine in der ganzen App:** M-02, der atmende Ring am Sprechknopf während der Aufnahme,
3200 ms Periode, endlos.

Sonst bewegt sich **nichts** von allein: kein pulsierender Punkt, kein wanderndes Glimmen,
kein sich drehendes Symbol, kein automatisch scrollender Text, keine Animation beim Erscheinen
eines Bildschirms außer den unter §4 genannten Wechseln.

Der Wartezustand (§7) ist keine Ausnahme davon — er ist nur sichtbar, solange tatsächlich
gewartet wird, und verschwindet danach vollständig.

---

## 7. Lade- und Wartezustände

**Schwelle: 400 ms.** Dauert ein Vorgang kürzer, zeigt sich kein Wartezustand — sonst
flackert die Oberfläche bei schnellen Antworten.

### M-09 — Wartezustand der KI

- **Wo:** B-01 (Vorschläge erzeugen), B-03 (Auswertung), B-02 (Gesprächsantwort),
  beim Verdichten (F-15).
- **Auslöser:** Eine KI-Anfrage läuft länger als 400 ms.
- **Was sich ändert:** **Kein Kreisel.** Eine ruhige Textzeile in Inter 16 sp, Farbe
  *Gedämpft* — „Ich sehe mir deine letzten Tage an …" — und darüber ein 2 dp hoher Balken
  in voller Breite, Fläche *Erhöht*, durch den ein 30 % breiter Streifen in *Aktion*
  von links nach rechts wandert.
- **Dauer / Kurve:** 1800 ms `wandern`.
- **Verzögerung:** 400 ms (die Schwelle).
- **Wiederholung:** endlos, bis die Antwort da ist.
- **Verschwinden:** Deckkraft 100 % → 0 % in 140 ms `hinaus`, danach erscheint das Ergebnis
  (M-04 oder M-08).

### Weitere Wartezustände

| Wo | Darstellung |
|----|-------------|
| B-02, KI antwortet | Drei ruhende Punkte in einer leeren KI-Blase — sie bewegen sich **nicht**, sie stehen. Nach 400 ms sichtbar. |
| Transkription läuft | Der Sprechknopf hört auf zu atmen und zeigt den Wartebalken aus M-09 unter sich. |
| Vorlesen wird geladen | Das Lautsprecher-Symbol färbt sich in *Aktion*, ohne Bewegung. |

---

## 8. Reduzierte Bewegung

**Pflichtabschnitt.** Meldet das System „Bewegung reduzieren"
(`Settings.Global.ANIMATOR_DURATION_SCALE == 0` oder die entsprechende
Barrierefreiheits-Einstellung):

| Was | Verhalten |
|-----|-----------|
| **Dauerbewegung (M-02)** | **aus.** Der Sprechknopf zeigt die laufende Aufnahme stattdessen dauerhaft eingefärbt in *Aktion* mit vollem Ring — ohne Größenänderung. |
| **Wandernder Balken (M-09)** | **aus.** Der Balken steht still in *Aktion* bei 40 % Deckkraft; die Textzeile bleibt. |
| **Alle Bildschirmwechsel** | nur noch Überblenden, kein Schieben. |
| **Alle Dauern** | **halbiert** (240 → 120 ms, 400 → 200 ms, 200 → 100 ms, 180 → 90 ms, 140 → 70 ms, 120 → 60 ms). |
| **Staffelung (M-04)** | entfällt — alle fünf Karten erscheinen gleichzeitig. |
| **Karten-Einsinken (M-01)** | entfällt; stattdessen kurzes Aufhellen der Fläche um 6 %. |
| **Vibration (M-03)** | **bleibt.** Sie ist keine Bewegung und trägt die Rückmeldung, wenn die sichtbare fehlt. |
| **Haken (M-06)** | zeichnet sich nicht, sondern erscheint sofort; die Textfarbe wechselt in 60 ms. |

---

## 9. Offene Fragen

Keine.
