# Motion-Spec — StackLabor
Stand: 14.08.2026 · Stufe: v1 · Plattform(en): Android

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/StackLabor/v2/messung/`. Widerspricht ein Satz von
> hier der Messung, ist der Satz ueberholt — nicht die Messung falsch.

## 1. Bewegungs-Grundhaltung

Das Innendisplay des Fold 8 läuft mit **120 Hz**. Bewegung, die auf 60 Hz stocken würde, ist
hier tragfähig — und ausdrücklich gewünscht: Frank hat „sehr viele verschiedene Spezialeffekte
in jeder Hinsicht" verlangt.

Das Grundtempo ist **straff, nicht verspielt**: 200–320 ms für alles, was auf eine Bedienung
antwortet. Die Leitkurve ist `cubic-bezier(0.2, 0, 0, 1)` — schneller Anlauf, weiches Ausrollen.

Zwei Bewegungen tragen in dieser App **Bedeutung** statt Schmuck, und sie sind deshalb die
wichtigsten des ganzen Dokuments:
1. **Der Ampel-Wechsel**, wenn Frank ein Häkchen wegnimmt oder ein Ziel höher zieht. Das ist der
   Moment, in dem die App ihren Wert zeigt.
2. **Das Ziehen der Ziele**, weil daran die gesamte Priorisierung hängt.

### Was sich NIEMALS bewegen darf

- **Ampelfarben im Ruhezustand.** Kein Blinken, kein Pulsieren, keine Sättigungsschwankung.
  Ein pulsierendes Rot liest sich als „Alarm", nicht als Gestaltung. Einzige Ausnahme ist die
  bewusst gesetzte Aura an roten Ampeln (M-21) und der einmalige Puls nach einer Änderung (M-09).
- **Dosis- und Einheitszahlen.** Sie werden gelesen, nicht bestaunt.
- **Das Häkchen-Kästchen selbst.** Es wechselt seinen Zustand ohne Sprung oder Skalierung.
- **Die Ziel-Nummern**, außer während einer aktiven Umsortierung (M-03).
- **Der Begründungstext bei Rot oder Gelb.** Er klappt auf und steht dann still.

## 2. Kurven und Dauern

Diese Namen werden überall sonst nur noch referenziert, nie neu erfunden.

| Name | Dauer | Kurve | Wofür |
|---|---|---|---|
| `antwort` | 220 ms | `cubic-bezier(0.2, 0, 0, 1)` | Ausweichen, Umordnen, Listenbewegung |
| `zustand` | 320 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Ampel-Überblendung, Farbwechsel |
| `aufnehmen` | 140 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Anheben beim Ziehen |
| `einrasten` | 260 ms | Spring, Dämpfung 0,75 / Steifigkeit 380 | Loslassen |
| `blatt` | 300 ms | `cubic-bezier(0.05, 0.7, 0.1, 1)` | Blätter von unten |
| `aufklappen` | 380 ms | `cubic-bezier(0.2, 0, 0, 1)` | Ziel-Überlagerung, Begründungszeile |
| `erscheinung` | 420 ms | `cubic-bezier(0.4, 0, 0.2, 1)` | Hell/Dunkel-Wechsel |
| `puls` | 520 ms | `cubic-bezier(0, 0, 0, 1)` | einmaliger Ring an geänderten Ampeln |
| `atem` | 2400 ms | Sinus, endlos | Aura an roten Ampeln |
| `schimmer` | 1400 ms | linear, endlos | Ladeplatzhalter |
| `wandern` | 30 s | linear, endlos | Kopfverlauf B-01 |

## 3. Bewegungen im Einzelnen

### M-01 — Ziel aufnehmen
- **Wo** B-12 · **Auslöser** langes Drücken, 300 ms
- **Was sich ändert** Skalierung 1,0 → 1,04 · Tiefe 1 → 8 dp · dazu ein haptischer Impuls (mittel)
- **Dauer/Kurve** 140 ms `aufnehmen`

### M-02 — Ziele weichen aus
- **Wo** B-12 · **Auslöser** Ziehen über eine andere Zeile
- **Was sich ändert** Y-Verschiebung der Nachbarzeilen um ± 40 dp (Zeilenhöhe)
- **Dauer/Kurve** 220 ms `antwort`, je Zeile 12 ms versetzt

### M-03 — Nummern laufen live mit
- **Wo** B-12 · **Auslöser** während des Ziehens, bei jedem Positionswechsel
- **Was sich ändert** Die Ziffer im Nummernkreis: Überblendung, dazu Y-Versatz 6 dp
- **Dauer/Kurve** 120 ms linear
- **Begründung** Frank sieht die neue Priorität, **während** er noch entscheidet — nicht erst danach.

### M-04 — Loslassen und Einrasten
- **Wo** B-12 · **Auslöser** Finger heben
- **Was sich ändert** Position rastet auf den Zielplatz · Skalierung 1,04 → 1,0 · Tiefe 8 → 1 dp · haptischer Impuls (leicht)
- **Dauer/Kurve** 260 ms `einrasten`

### M-05 — Automatisches Weiterrollen am Rand
- **Wo** B-12 · **Auslöser** Ziehen in die obere oder untere Randzone
- **Was sich ändert** Die Liste rollt weiter. Randzone **64 dp**, Geschwindigkeit linear von 0 auf **900 dp/s** über die Zonentiefe, Start erst nach 120 ms Verweilen (damit leichtes Zittern am Rand nichts auslöst)
- **Begründung** Ohne dies wäre ein Ziel von Position 28 auf Position 1 nicht in einem Zug zu bewegen.

### M-06 — Abbruch
- **Wo** B-12, B-02 · **Auslöser** Ziehen über den Bildschirmrand hinaus und loslassen, oder Zurück-Geste
- **Was sich ändert** Rückflug zur Ausgangsposition · Tiefe 8 → 1 dp
- **Dauer/Kurve** 300 ms `antwort`

### M-07 — Ampel-Überblendung
- **Wo** B-01, B-02, B-04 · **Auslöser** F-14 (Häkchen, Priorität, Zielauswahl, neue Bewertung)
- **Was sich ändert** Die Farbe des Kantenbalkens von der alten auf die neue Ampelfarbe, **niemals hart**. Der Farbweg macht die Richtung lesbar (rot → grün liest sich anders als grün → rot)
- **Dauer/Kurve** 320 ms `zustand`

### M-08 — Ampeln gestaffelt
- **Wo** B-02, B-04 · **Auslöser** wie M-07, wenn mehrere Ampeln gleichzeitig wechseln
- **Was sich ändert** Die Überblendungen starten von oben nach unten versetzt, **45 ms** je Zeile, gedeckelt bei 10 Stufen (also höchstens 450 ms Gesamtwelle)
- **Begründung** Die Welle folgt der Prioritätsreihenfolge — Frank liest zuerst, was oben passiert.

### M-09 — Puls nur an den geänderten Ampeln
- **Wo** B-02, B-04 · **Auslöser** F-14, aber nur für Ampeln, deren Wert sich **tatsächlich** geändert hat
- **Was sich ändert** Ein Ring um den Kantenbalken: Radius + 6 dp, Deckkraft 0,55 → 0
- **Dauer/Kurve** 520 ms `puls`, einmalig
- **Begründung** Bei 30 Zielen ginge der eine wichtige Wechsel sonst unter. Unveränderte Ampeln pulsen **nicht**.

### M-10 — Verbindungsfarbe Mittel ↔ Ziel
- **Wo** B-06 · **Auslöser** Öffnen der Aufschlüsselung
- **Was sich ändert** Der Gegenstand im Kopf und die betroffenen Zeilen bekommen für 900 ms einen 2 dp starken Rand in derselben Farbe
- **Dauer/Kurve** Einblenden 180 ms `antwort`, Halten 520 ms, Ausblenden 400 ms

### M-11 — Warte-Skelett mit Schimmer
- **Wo** B-02, B-07, B-09 · **Auslöser** F-12, F-13 laufen
- **Was sich ändert** Platzhalterflächen mit einem Lichtstreifen, der von links nach rechts wandert; Streifenbreite 40 % der Fläche
- **Dauer/Kurve** Periode 1400 ms `schimmer`, endlos bis zum Ergebnis

### M-12 — Ampeln entsättigt und pulsierend
- **Wo** B-02, B-04, B-09 · **Auslöser** solange eine Auswertung läuft
- **Was sich ändert** Alle Ampeln werden entsättigt (grau) und atmen: Deckkraft 1,0 → 0,45 → 1,0
- **Dauer/Kurve** Periode 1600 ms, `cubic-bezier(0.4, 0, 0.6, 1)`
- **Begründung** Das ist der wichtigste Wartezustand: Frank sieht sofort, dass die angezeigten Farben **gerade nicht gelten** — sonst entscheidet er auf veralteten Ampeln.

### M-13 — Streamender Antworttext
- **Wo** B-07, und als dreizeiliger Auszug auf B-02 · **Auslöser** F-12, F-13
- **Was sich ändert** Der Text erscheint wortweise: je Wort Einblenden + 6 dp Aufwärtsbewegung, 60 ms Abstand zwischen den Wörtern. Darüber läuft eine Fortschrittserzählung („prüfe Wechselwirkungen …", „gewichte Ziel 1–4 …"), deren Text alle paar Sekunden überblendet
- **Dauer/Kurve** je Wort 180 ms `aufnehmen`; Erzähler-Wechsel 240 ms Überblendung
- **Begründung** Aus 20 Sekunden Warten werden 20 Sekunden Lesen. Die Erzählung deckt die Zeit vor dem ersten Wort ab.

### M-14 — Ziel-Überlagerung aufklappen
- **Wo** B-02 → B-04 · **Auslöser** Tippen auf den Ziel-Streifen
- **Was sich ändert** Höhe 0 → max 281 dp; die Zeilen erscheinen gestaffelt mit 30 ms Abstand, gedeckelt bei 12 sichtbaren Zeilen; der Pfeil dreht 180°
- **Dauer/Kurve** 380 ms `aufklappen`, Pfeil 300 ms
- **Begründung** Die Deckelung hält die Kosten gleich, ob 5 oder 30 Ziele vorliegen.

### M-15 — Sprech-Markierung und Pegel
- **Wo** B-07 · **Auslöser** F-16 läuft
- **Was sich ändert** Der gerade gesprochene **Absatz** bekommt eine Hintergrundfläche, deren Kante mitgleitet · am Sprech-Knopf zeigen drei Balken den Pegel (aus `SpeechLoudness`), Nachlauf 90 ms
- **Dauer/Kurve** Kantenbewegung 200 ms `zustand`
- **Begründung** Wortgenaue Marken liefert die vorhandene Sprachkette nicht zuverlässig — absatzweise ist ehrlich und robust.

### M-16 — Dauerbewegung
- **Wo** B-01, B-02 · **Auslöser** keiner, läuft ohne Zutun
- **Was sich ändert**
  - Kopfverlauf auf B-01 wandert `Akzent` → `#0EA5E9` und zurück, Periode **30 s**, nur bei sichtbarem Bildschirm
  - Glanzkante an Karten, Periode **8 s**
  - Der schwebende Plus-Knopf atmet, Skalierung 1,0 → 1,02, Periode **3200 ms**
- **Begründung** Alle Perioden liegen bei **3 Sekunden oder darüber**. Alles Schnellere wirkt unruhig und zieht den Blick von den Ampeln ab.

### M-17 — Faltvorgang
- **Wo** alle Bildschirme · **Auslöser** Gerät wird auf- oder zugeklappt
- **Was sich ändert** Gemeinsame Elemente wandern an ihren neuen Platz; die zweite Spalte schiebt sich von rechts ein (X + 24 dp → 0, Deckkraft 0 → 1, 300 ms, 100 ms verzögert)
- **Dauer/Kurve** 400 ms `antwort`
- **Regel** Scrollposition, geöffnete Blätter und laufende Vorgänge bleiben erhalten — das Aufklappen fühlt sich wie ein Erweitern derselben Ansicht an, nicht wie ein Neustart.

### M-18 — Mittel-Reihenfolge ziehen
- **Wo** B-02, **nur** in der Ansicht „Einnahme" · **Auslöser** langes Drücken, 300 ms
- **Was sich ändert** Wie M-01 bis M-06
- **Regel** In der Ansicht „Löslichkeit" nimmt langes Drücken **nicht** auf, sondern öffnet ein Kontextmenü — es gibt keinen toten Ziehversuch. Eine Kombi-Gruppe wird als Ganzes gezogen.

### M-19 — Erscheinungswechsel
- **Wo** die ganze App · **Auslöser** F-22
- **Was sich ändert** Alle Farbwerte überblenden gleichzeitig
- **Dauer/Kurve** 420 ms `erscheinung`
- **Regel** Offene Blätter bleiben offen und blenden mit. Ein harter Sprung wäre bei Glasflächen und Auren ein sichtbarer Bruch.

### M-20 — Blatt öffnen und schließen
- **Wo** B-04, B-05, B-06, B-08, B-13, B-15 · **Auslöser** F-03, F-09, F-11, F-15, F-29
- **Was sich ändert** Das Blatt fährt von unten ein; dahinter dunkelt der Grund auf 32 % ab
- **Dauer/Kurve** 300 ms `blatt`

### M-21 — Atmende Aura an roten Ampeln
- **Wo** B-01, B-02 · **Auslöser** eine Ampel steht auf Rot
- **Was sich ändert** Ein Glühen um den Kantenbalken, Radius 4 → 8 dp
- **Dauer/Kurve** Periode 2400 ms `atem`, endlos
- **Regel** **Höchstens drei gleichzeitig** — die drei mit dem höchsten Rang. Sonst wird die Liste unruhig und Rot verliert seine Warnwirkung.

### M-22 — Gestaffeltes Einblenden beim Öffnen
- **Wo** B-01, B-02, B-03, B-14 · **Auslöser** Bildschirm wird geöffnet
- **Was sich ändert** Die Einträge erscheinen von oben nach unten, je 40 ms versetzt, mit 12 dp Aufwärtsbewegung
- **Dauer/Kurve** je Eintrag 220 ms `antwort`, gedeckelt bei 8 Elementen

### M-23 — Häkchen-Rückmeldung
- **Wo** B-02 · **Auslöser** F-05
- **Was sich ändert** Das Kästchen selbst bewegt sich **nicht** (siehe §1). Stattdessen: haptischer Impuls (leicht) und die Kartenfläche wechselt auf den ausgegrauten Zustand
- **Dauer/Kurve** 220 ms `antwort` für den Flächen- und Deckkraftwechsel

### M-24 — Wischen zum Entfernen
- **Wo** B-02, B-03, B-08, B-14 · **Auslöser** F-04, F-08, F-11
- **Was sich ändert** Der Eintrag folgt dem Finger; hinter ihm erscheint eine rote Fläche mit Papierkorb. Nach dem Loslassen jenseits der halben Breite gleitet er aus dem Bild, die Lücke schließt sich, und die Rückgängig-Leiste fährt von unten ein
- **Dauer/Kurve** Ausgleiten 220 ms `antwort`, Lückenschluss 220 ms, Leiste 300 ms `blatt`, sichtbar 6 s · haptischer Impuls (schwer) beim Auslösen

## 4. Bildschirmwechsel

| Von | Nach | Art | Dauer | Kurve |
|---|---|---|---|---|
| B-01 | B-02 | Vorwärts: neuer Bildschirm schiebt von rechts (X + 32 dp → 0, Deckkraft 0 → 1); der alte weicht 16 dp nach links und dunkelt leicht ab | 300 ms | `antwort` |
| B-02 | B-01 | Rücklauf: umgekehrt, der alte schiebt nach rechts hinaus | 260 ms | `antwort` |
| B-01 | B-03, B-09, B-10, B-14 | wie B-01 → B-02 | 300 ms | `antwort` |
| B-02 | B-07, B-12 | Vollbild zieht von unten auf (Y + 48 dp → 0, Deckkraft 0 → 1) | 320 ms | `blatt` |
| B-07, B-12 | zurück | nach unten hinaus | 260 ms | `antwort` |
| beliebig | Blatt (B-04, B-05, B-06, B-08, B-13, B-15) | M-20 | 300 ms | `blatt` |
| B-10 | B-11 | wie B-01 → B-02 | 300 ms | `antwort` |

Der **Rücklauf ist immer 40 ms kürzer** als der Hinlauf. Zurückgehen soll sich schneller
anfühlen als Vorwärtsgehen.

## 5. Rückmeldung auf Bedienung

| Element | Rückmeldung |
|---|---|
| Knopf (Sockel, Blatt) | Fläche dunkelt 8 % ab, Skalierung 1,0 → 0,98, 120 ms `antwort`; beim Loslassen zurück |
| Karte (Stack, Mittel) | Wellenring vom Berührungspunkt, 320 ms, Deckkraft 0,12 → 0 |
| Häkchen | M-23 — keine Bewegung des Kästchens, nur Haptik und Flächenwechsel |
| Chip (Sortierung) | Füllung wechselt in 180 ms `antwort` |
| Symbolknopf | Wellenring in einem 40 dp Kreis, 280 ms |
| Langes Drücken | M-01 bzw. Kontextmenü, jeweils mit haptischem Impuls |
| Wischen | M-24 |
| Ziehen | M-01 bis M-06 |

**Haptik:** leicht beim Häkchen und beim Einrasten · mittel beim Aufnehmen eines Ziels ·
schwer beim Auslösen des Wischen-Löschens · doppelt, wenn eine Auswertung eine **neue rote**
Ampel hervorgebracht hat.

## 6. Dauerbewegung

Siehe M-16 (Kopfverlauf 30 s · Glanzkante 8 s · Plus-Knopf 3200 ms) und M-21 (Aura an roten
Ampeln, 2400 ms, höchstens drei gleichzeitig).

Alles andere steht still. Die Grenze ist bewusst gesetzt: **keine Dauerbewegung mit einer
Periode unter 3 Sekunden**, außer der Aura, die als Warnzeichen gemeint ist.

## 7. Lade- und Wartezustände

| Wartefall | Ab wann | Was sich zeigt | Wie es endet |
|---|---|---|---|
| Auswertung (F-12, F-13) | sofort | M-11 Skelett + M-12 entsättigte Ampeln + M-13 streamender Text mit Fortschrittserzählung | Skelett wird vom Inhalt ersetzt (Überblendung 220 ms), Ampeln sättigen sich mit M-07/M-08/M-09 |
| Konkurrenzprüfung (F-02) | nach dem 3-s-Ruhefenster | Ein schmaler unbestimmter Fortschrittsbalken 2 dp unter dem Sockel — **kein** Blockieren der Bedienung | Hinweis-Schnipsel fährt mit M-20 ein |
| Anmeldung (F-17) | sofort | Pulsring an der Wartezeile, Periode 1600 ms | Wechsel auf „angemeldet", Ring verschwindet |
| Vorlesen startet (F-16) | nach 400 ms | Pegelbalken beginnen zu schwingen | Erster Absatz wird markiert |
| Import/Export (F-19, F-20) | sofort | Bestimmter Fortschrittsbalken im Blatt | Meldung „Fertig" |

Dauert eine Auswertung **länger als 45 Sekunden**, ergänzt die Fortschrittserzählung eine
Zeile „Das dauert länger als gewöhnlich." — abgebrochen wird nicht von selbst.

## 8. Reduzierte Bewegung

Meldet das System „Animationen reduzieren" (oder ist der Schalter in B-10 gesetzt):

**Bleibt erhalten — weil es Bedeutung trägt:**
- M-07 Ampel-Überblendung, volle 320 ms. Ein harter Farbsprung wäre nicht nur hässlich, sondern
  ließe Frank die Änderung übersehen.
- M-02 Ausweichen beim Ziehen, volle 220 ms. Ohne sie wäre nicht erkennbar, wohin ein Ziel fällt.
- M-13 Der streamende Text — das ist **Inhalt**, keine Animation.

**Wird abgeschaltet:**
- Alle Dauerbewegungen (M-16, M-21) — vollständig.
- M-09 Puls, M-10 Verbindungsfarbe, M-22 gestaffeltes Einblenden.
- M-12 wird ein **statischer** Graustand statt eines Pulsierens — die Aussage „gilt gerade
  nicht" bleibt also erhalten, nur ohne Bewegung.
- M-11 Schimmer wird eine ruhige Fläche.

**Wird verkürzt auf 0 ms:** alle Bildschirmwechsel (§4), M-14, M-19, M-20, M-24 — die Zustände
wechseln unmittelbar.

## 9. Offene Fragen

Keine. Frank hat den vertiefenden Bewegungs-Durchgang zugunsten des Spec-Baus abgekürzt; alle
hier genannten Werte sind begründete Vorgaben an den Designer und werden von seiner Messung
überholt, sobald der Entwurf zurück ist.
