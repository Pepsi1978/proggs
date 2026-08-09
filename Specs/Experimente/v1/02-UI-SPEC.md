# UI-Spec — Experimente

Stand: 09.08.2026 · Stufe: v1 · Plattform(en): Android

---

## 1. Gestalterische Grundhaltung

Warm im Grundton, sachlich im Aufbau. Die Wärme kommt aus den Farben und der Serifen-Überschrift,
die Klarheit aus Typografie, Abständen und Ordnung — nicht aus zusätzlicher Farbe. Die App soll
sich anfühlen wie ein gut gemachtes Notizbuch, das zuhört, nicht wie ein Gerät, das misst: keine
Diagramme, keine Zähler, keine Abzeichen, kein Fortschrittsbalken. Frank benutzt sie morgens und
abends, und sie bittet ihn um Dinge, vor denen er sich womöglich drückt — eine Oberfläche, die
dabei zusätzlich antreibt, arbeitet gegen die Sache. Jede spätere Entscheidung misst sich daran:
*Beruhigt sie, oder drängt sie?*

---

## 2. Erscheinungen (Themes)

Beide Erscheinungen sind **gleichrangig** und vollständig zu bauen. Standard: **Dunkel**.

### 2.1 Dunkel (Standard)

| Rolle | Wert | Verwendung |
|-------|------|------------|
| Grund | `#151210` | Bildschirmhintergrund, warmes Schwarzbraun |
| Fläche | `#201B17` | Karten, Listeneinträge, untere Leiste |
| Erhöht | `#2A231D` | ausgewählte Karte, Dialoge, Eingabefelder |
| Rand | `#38302A` | Kartenrand, Trennlinien |
| Rand weich | `#2C251F` | Trennlinien innerhalb einer Karte |
| Text | `#F4EEE7` | Fließtext, Überschriften |
| Gedämpft | `#A99C8F` | Datum, Nebeninformation, Zwischenüberschrift |
| Blass | `#6E635A` | Platzhalter, abgehakte Zeile, ausgegraut |
| Aktion | `#C4623C` | Sprechknopf, Auswahl, „Andere Vorschläge", aktive Leiste |
| Aktion gedeckt | `#3A231A` | Fläche hinter einem Aktions-Symbol |
| Erledigt | `#6F8F6A` | Haken, abgeschlossenes Experiment |
| Erledigt gedeckt | `#22301F` | Fläche hinter einem Haken |
| Warnung | `#D8A03C` | Fehlermeldungen, fehlender Schlüssel |

### 2.2 Hell

| Rolle | Wert | Verwendung |
|-------|------|------------|
| Grund | `#F8F4EE` | Bildschirmhintergrund, warmes Papier |
| Fläche | `#FFFFFF` | Karten, Listeneinträge, untere Leiste |
| Erhöht | `#FFFFFF` | Dialoge, Eingabefelder (Trennung über Rand) |
| Rand | `#E6DCD0` | Kartenrand, Trennlinien |
| Rand weich | `#EFE8DF` | Trennlinien innerhalb einer Karte |
| Text | `#1E1915` | Fließtext, Überschriften |
| Gedämpft | `#6C6157` | Datum, Nebeninformation, Zwischenüberschrift |
| Blass | `#9C9186` | Platzhalter, abgehakte Zeile, ausgegraut |
| Aktion | `#B0522E` | Sprechknopf, Auswahl, „Andere Vorschläge", aktive Leiste |
| Aktion gedeckt | `#F6E6DD` | Fläche hinter einem Aktions-Symbol |
| Erledigt | `#5A7A55` | Haken, abgeschlossenes Experiment |
| Erledigt gedeckt | `#E6EFE3` | Fläche hinter einem Haken |
| Warnung | `#9A6A12` | Fehlermeldungen, fehlender Schlüssel |

Es gibt **keine Bereichsfarben.** Vorschläge werden nicht nach Lebensbereich eingefärbt.

---

## 3. Typografie

| Familie | Schnitte | Wofür |
|---------|----------|-------|
| **Fraunces** | Regular 400, SemiBold 600 | Überschriften, Vorschlagstitel, Experimenttitel |
| **Inter** | Regular 400, Medium 500, SemiBold 600 | Fließtext, Beschreibungen, alles Gesprochene und Geschriebene |
| **JetBrains Mono** | Regular 400 | Datum, „Tag 2 von 3", Haken-Zähler, Uhrzeiten, Stufe |

| Rolle | Familie | Größe | Gewicht | Zeilenhöhe | Laufweite |
|-------|---------|-------|---------|-----------|-----------|
| Bildschirmtitel | Fraunces | 28 sp | 600 | 34 sp | 0 |
| Abschnittstitel | Fraunces | 22 sp | 600 | 28 sp | 0 |
| Kartentitel | Fraunces | 19 sp | 600 | 25 sp | 0 |
| Fließtext | Inter | 16 sp | 400 | 25 sp | 0 |
| Fließtext klein | Inter | 14 sp | 400 | 21 sp | 0 |
| Knopfbeschriftung | Inter | 16 sp | 500 | 20 sp | 0,2 sp |
| Zwischenüberschrift | Inter | 13 sp | 600 | 17 sp | 0,6 sp, Großbuchstaben |
| Daten und Zahlen | JetBrains Mono | 13 sp | 400 | 18 sp | 0 |
| Stufe / Dauer | JetBrains Mono | 12 sp | 400 | 16 sp | 0,4 sp |

---

## 4. Maße und Raster

- **Grundraster: 4 dp.** Alle Abstände sind Vielfache davon.
- **Seitenrand:** 20 dp links und rechts auf allen Bildschirmen.
- **Abstand zwischen Karten:** 12 dp.
- **Innenabstand einer Karte:** 20 dp.
- **Abstand zwischen Abschnitten:** 32 dp.
- **Untere Leiste:** 72 dp hoch, fünf gleich breite Felder.
- **Obere Leiste:** 64 dp hoch, Titel linksbündig.
- **Mindest-Tippfläche:** 48 × 48 dp für jedes bedienbare Element.
- **Sprechknopf groß** (B-01, B-03, B-04, B-05, B-09): 88 dp Durchmesser, mittig.
- **Sprechknopf klein** (B-02): 56 dp Durchmesser, unten rechts.
- **Symbolgröße:** 24 dp in Listen und Leisten, 28 dp an Karten.
- **Textbreite:** höchstens 62 Zeichen je Zeile, damit lange Auswertungen lesbar bleiben.

---

## 5. Formen und Tiefe

| Bauteil | Radius |
|---------|--------|
| Karte | 20 dp |
| Eingabefeld | 14 dp |
| Knopf | 14 dp |
| Reiter, Chip, Stufen-Etikett | vollrund |
| Sprechknopf | vollrund |
| Dialog | 24 dp |

- **Keine Schatten.** Trennung ausschließlich über Fläche und 1 dp Rand.
- **Keine Verläufe** — mit genau einer Ausnahme: der wandernde Verlauf im Wartezustand
  (siehe Motion-Spec, M-09).
- **Kein Weichzeichner.**

---

## 6. Bildschirme

| Kennung | Bildschirm | Zweck | Start? | führt zu |
|---------|-----------|-------|--------|----------|
| B-01 | **Heute** | Der Tageslauf: Lage, Vorschläge, laufende Experimente, To-Do-Liste | **ja** | B-02, B-03, B-08 |
| B-02 | **Gespräch** | Fortlaufendes Gespräch zu einem laufenden Experiment | nein | B-01 |
| B-03 | **Auswertung** | Abends: alle offenen Experimente der Reihe nach auswerten | nein | B-01 |
| B-04 | **Wünsche & Ziele** | Franks persönliche Ziele | nein | — |
| B-05 | **Merkliste** | Aufgehobene Vorschläge, eigene Ideen, nicht Umgesetztes | nein | — |
| B-06 | **Erkenntnisse** | Was Frank über sich gelernt hat | nein | — |
| B-07 | **Logbuch** | Zwei Reiter: Letzte 15 Tage · Langzeit | nein | — |
| B-08 | **Einstellungen** | KI, Stimme, Zugänge, Erinnerungen, Erscheinung | nein | B-09 |
| B-09 | **Selbstbild** | Alles, was die App dauerhaft über Frank wissen soll | nein | B-08 |

**Untere Leiste** (auf B-01, B-04, B-05, B-06, B-07 sichtbar):
`Heute · Ziele · Merkliste · Erkenntnisse · Logbuch`.
**Einstellungen** über ein Zahnrad oben rechts. B-02, B-03 und B-09 haben keine untere Leiste,
sondern einen Zurück-Pfeil oben links.

---

### B-01 — Heute

**Aufbau von oben nach unten:**

1. Obere Leiste: Titel „Heute", rechts das Zahnrad (→ B-08).
2. Datum in JetBrains Mono, gedämpft.
3. **Der wechselnde Hauptbereich** (je nach Zustand, siehe unten).
4. Untere Leiste.

**Zustand `LEER`** — noch keine Lage eingesprochen
- Frage in Fraunces 22 sp: „Wie ist deine Lage heute?"
- Darunter zwei Zeilen Fließtext klein, gedämpft: „Was für ein Tag ist das? Was liegt vor dir?"
- Sprechknopf groß, mittig, Fläche *Aktion*, Mikrofon-Symbol.
- Darunter ein flacher Textknopf „Lieber tippen".

**Zustand `AUFNAHME`**
- Wie `LEER`, aber der Sprechknopf trägt ein Quadrat statt des Mikrofons und einen atmenden
  Ring (M-02). Darunter die laufende Zeit in JetBrains Mono.

**Zustand `LAGE_STEHT`**
- Eingabefeld mit dem transkribierten Text, bearbeitbar, Fläche *Erhöht*.
- Darunter nebeneinander: „Text mit KI verbessern" (Textknopf) und „Weiter" (gefüllter Knopf,
  Fläche *Aktion*).

**Zustand `VORSCHLAEGE`**
- Zwischenüberschrift: „FÜNF VORSCHLÄGE FÜR HEUTE".
- Fünf Karten untereinander. Je Karte:
  - Titel in Fraunces 19 sp.
  - Beschreibung in Inter 16 sp, höchstens drei Zeilen, danach „mehr".
  - Untere Zeile in JetBrains Mono, gedämpft: Stufe (`leicht` · `mittel` · `fordernd`) ·
    Dauer (`1 Tag` / `3 Tage`).
  - Rechts oben ein Merken-Symbol (Lesezeichen), gefüllt wenn bereits gemerkt.
  - **Nur beim Merklisten-Vorschlag:** ein vollrundes Etikett „von deiner Merkliste" in
    *Aktion gedeckt*. Die vier anderen tragen kein Etikett.
- Darunter mittig: „Andere Vorschläge" (Textknopf mit Kreis-Pfeil).

**Zustand `LAEUFT`**
- Zwischenüberschrift: „LÄUFT" (bzw. „LÄUFT (3 VON 3)", wenn drei offen sind).
- Je laufendem Experiment eine Karte: Titel · „Tag 2 von 3" in JetBrains Mono ·
  ein Gesprächs-Symbol rechts (→ B-02) · ein flacher Knopf „Nicht umgesetzt".
- Danach Zwischenüberschrift „HEUTE ZU TUN" und **eine** To-Do-Liste:
  je Experiment eine kleine Zwischenüberschrift mit dem Experimenttitel, darunter seine
  heutigen Aufgaben als antippbare Zeilen mit Kästchen.
  Abgehakte Zeilen: Text in *Blass*, Kästchen gefüllt in *Erledigt*.
- Ist die Höchstzahl erreicht, unter den Karten ein gedämpfter Hinweis:
  „Drei Experimente laufen. Schließ eines ab, bevor du ein neues beginnst."

**Zustand `ABEND`**
- Wie `LAEUFT`, zusätzlich ganz unten ein gefüllter Knopf „Wie ist es gelaufen?" (→ B-03).

**Weitere Zustände**
- *Lädt:* Der Hauptbereich wird durch den Wartezustand ersetzt (M-09) mit dem Text
  „Ich sehe mir deine letzten Tage an …".
- *Fehler:* Karte in *Erhöht* mit Rand in *Warnung*, Fehlertext, Knopf „Nochmal versuchen".
- *Leer, kein Netz:* „Dafür brauche ich Netz." mit demselben Knopf.

---

### B-02 — Gespräch

1. Obere Leiste: Zurück-Pfeil, darunter der Experimenttitel in Fraunces 22 sp und
   „Tag 2 von 3" in JetBrains Mono.
2. Gesprächsfaden, von oben nach unten, neueste Runde unten:
   - **Ich:** rechtsbündige Blase, Fläche *Erhöht*, Radius 20 dp, unten rechts 6 dp.
   - **KI:** linksbündige Blase, Fläche *Fläche* mit 1 dp Rand, Radius 20 dp,
     unten links 6 dp. Rechts unten ein Lautsprecher-Symbol.
3. Unten fest: Sprechknopf klein rechts, links ein Textfeld „Oder tippen …".

**Zustände:** *leer* — eine gedämpfte Zeile: „Frag mich, wie du das angehen könntest."
*Aufnahme* — der kleine Knopf atmet. *Lädt* — drei ruhende Punkte in der KI-Blase.
*Vorlesen läuft* — das Lautsprecher-Symbol ist in *Aktion* eingefärbt.

---

### B-03 — Auswertung

1. Obere Leiste: Zurück-Pfeil, Titel „Wie ist es gelaufen?".
2. Je offenem Experiment ein Abschnitt, untereinander:
   - Titel in Fraunces 19 sp, darunter „Tag 2 von 3" und der Haken-Stand
     („3 von 5 erledigt") in JetBrains Mono, gedämpft.
   - Sprechknopf groß, mittig.
   - Nach der Aufnahme: Eingabefeld mit dem Text, „Text mit KI verbessern", „Weiter".
   - Danach die KI-Antwort in einer Karte mit Rand: bei Zwischentagen zwei bis drei Sätze,
     am letzten Tag die vollständige Auswertung. Rechts oben ein Lautsprecher-Symbol.
   - Ein flacher Knopf „Überspringen" führt zum nächsten Abschnitt.
3. Ist alles ausgewertet: unten ein gefüllter Knopf „Fertig" (→ B-01).

**Zustände:** *leer* — „Heute läuft kein Experiment." *Lädt* — Wartezustand in der Karte.
*Fehler* — Fehlerkarte, Franks eigener Text bleibt sichtbar stehen.

---

### B-04 — Wünsche & Ziele

1. Obere Leiste: Titel „Wünsche & Ziele".
2. Einleitung, zwei Zeilen gedämpft: „Was möchtest du erreichen? Die Vorschläge tasten dich
   Schritt für Schritt heran."
3. Liste der Ziele als Karten: Text in Inter 16 sp, darunter das Anlagedatum in JetBrains Mono.
   Antippen öffnet die Bearbeitung, langer Druck bietet Löschen an.
4. Plus-Knopf unten rechts, vollrund, Fläche *Aktion*.

**Anlegen:** Der Plus-Knopf öffnet eine Fläche mit Sprechknopf groß. Nach der Aufnahme:
Textfeld, „Text mit KI verbessern", „Speichern". Nach dem Speichern **bleibt die Fläche
offen und der Sprechknopf sofort bereit**, damit mehrere Ziele hintereinander gehen.

**Zustände:** *leer* — „Noch keine Ziele. Sprich das erste ein." mit einem Pfeil zum Plus.

---

### B-05 — Merkliste

1. Obere Leiste: Titel „Merkliste".
2. Liste als Karten. Je Karte: Titel in Fraunces · Beschreibung, zwei Zeilen ·
   Stufe und Dauer in JetBrains Mono · links ein kleines Herkunftszeichen:
   Lesezeichen (gemerkt) · Stift (eigene Idee) · Kreis-Pfeil (nicht umgesetzt).
   Bei „nicht umgesetzt" zusätzlich eine gedämpfte Zeile mit dem Vermerk, was im Weg stand.
3. Plus-Knopf unten rechts — Anlegen wie B-04.

**Zustände:** *leer* — „Nichts gemerkt. Wenn dir ein Vorschlag gefällt, tipp auf das
Lesezeichen."

---

### B-06 — Erkenntnisse

1. Obere Leiste: Titel „Erkenntnisse".
2. Einleitung gedämpft: „Was sich aus deinen Auswertungen ergeben hat."
3. Liste, neueste oben. Je Eintrag: Text in Inter 16 sp, darunter das Datum der letzten
   Änderung in JetBrains Mono. Trennlinien in *Rand weich*, keine Karten — es soll sich am
   Stück lesen lassen.

**Zustände:** *leer* — „Noch nichts. Das wächst mit deinen Auswertungen."

---

### B-07 — Logbuch

1. Obere Leiste: Titel „Logbuch".
2. Zwei Reiter, vollrund, Fläche *Erhöht*, der aktive in *Aktion gedeckt* mit Text in *Aktion*:
   **Letzte 15 Tage** · **Langzeit**.
3. **Reiter „Letzte 15 Tage":** je Tag eine Karte — Datum in JetBrains Mono als Kopfzeile,
   darunter der ausführliche Text. Langer Druck öffnet Bearbeiten und Löschen.
4. **Reiter „Langzeit":** je Tag eine schmalere Karte — Datum, darunter der verdichtete Text.
   Enthält der Tag ein Experiment, steht dessen Titel als erste Zeile in Fraunces und rechts
   ein Lautsprecher-Symbol für die Auswertung. Langer Druck ebenso.

**Zustände:** *leer* — „Noch nichts aufgeschrieben." *Bearbeiten* — der Text wird zum
Eingabefeld, darunter „Speichern" und „Abbrechen". *Löschen* — Dialog mit Rückfrage.

---

### B-08 — Einstellungen

Abschnitte untereinander, je mit Zwischenüberschrift:

**KI** — zwei Blöcke:
- *Experimente:* Modell (Auswahlfeld: GPT 5.6 Sol · GPT 5.6 Terra · GPT 5.6 Luna),
  Effort (Niedrig · Mittel · Hoch · Sehr hoch · Maximal)
- *Logbuch:* dieselben beiden Auswahlfelder, unabhängig
- darunter eine gedämpfte Zeile: „Das Logbuch darf ein anderes Modell benutzen als die
  Experimente."

**Stimme** — Anbieter (Google Chirp 3 HD · Meine Stimme · Microsoft Edge), darunter je nach
Wahl die Stimmliste bzw. die Stimmverwaltung mit „Stimme aufnehmen". Schieberegler
Sprechgeschwindigkeit 0,7–1,3. Knopf „Probe hören".

**Zugänge** — Codex: Zustand (angemeldet als … / nicht angemeldet), Knopf „Anmelden" bzw.
„Abmelden". Darunter drei verdeckte Felder: Groq · Google Cloud · Alibaba.

**Erinnerungen** — zwei Zeilen mit Schalter und Uhrzeit: *Morgens* (08:00) · *Abends* (20:30).

**Erscheinung** — drei Möglichkeiten: Hell · Dunkel · Wie das System.

**Über mich** — eine Zeile mit Pfeil: „Selbstbild" (→ B-09).

**Version** — unten, gedämpft, in JetBrains Mono.

---

### B-09 — Selbstbild

1. Obere Leiste: Zurück-Pfeil, Titel „Selbstbild".
2. Einleitung gedämpft, zwei Zeilen: „Alles, was die App dauerhaft über dich wissen soll.
   Je mehr hier steht, desto genauer treffen die Vorschläge."
3. Ein großes Textfeld, das den restlichen Bildschirm füllt, Fläche *Erhöht*, kein Rahmen
   um einzelne Absätze, freier Fließtext.
4. Unten fest: Sprechknopf groß mittig, links daneben „Text mit KI verbessern".
   Gespeichert wird beim Verlassen.

**Zustände:** *leer* — Platzhalter in *Blass*: „Wer bist du? Was prägt dich? Was war?
Sprich einfach drauflos."

---

## 7. Ikonografie und Bilder

- **Satz:** Material Symbols Rounded, **Linie** (nicht gefüllt), Strichstärke 2 dp,
  Gewicht 400, Grad 0.
- **Ausnahme:** aktive Zustände werden gefüllt gezeigt — das Lesezeichen bei einem gemerkten
  Vorschlag, das Kästchen bei einer erledigten Aufgabe, das aktive Feld der unteren Leiste.
- **Verwendete Symbole:** `mic` · `stop` · `keyboard` · `bookmark` / `bookmark_filled` ·
  `refresh` · `chat_bubble` · `volume_up` · `check_box_outline_blank` / `check_box` ·
  `add` · `settings` · `arrow_back` · `edit` · `delete` · `flag` · `history` · `lightbulb` ·
  `today` · `auto_awesome` (für die KI-Auswertung).
- **Keine Bilder, keine Illustrationen, keine Maskottchen.** Leere Zustände tragen einen Satz
  Text und höchstens ein einzelnes gedämpftes Symbol.

---

## 8. Texte

Alle festen Beschriftungen wörtlich. Platzhalter in ⟨spitzen Klammern⟩.

| Ort | Text |
|-----|------|
| B-01 leer | „Wie ist deine Lage heute?" |
| B-01 leer, Untertitel | „Was für ein Tag ist das? Was liegt vor dir?" |
| B-01 Textknopf | „Lieber tippen" |
| Überall | „Text mit KI verbessern" / „Zurücknehmen" |
| B-01 nach der Lage | „Weiter" |
| B-01 Zwischenüberschrift | „FÜNF VORSCHLÄGE FÜR HEUTE" |
| Vorschlagskarte, Etikett | „von deiner Merkliste" |
| B-01 unter den Karten | „Andere Vorschläge" |
| B-01 Zwischenüberschrift | „LÄUFT" / „LÄUFT (⟨n⟩ VON 3)" |
| B-01 Zwischenüberschrift | „HEUTE ZU TUN" |
| B-01 Experimentkarte | „Tag ⟨n⟩ von ⟨m⟩" / „Nicht umgesetzt" |
| B-01 Grenze erreicht | „Drei Experimente laufen. Schließ eines ab, bevor du ein neues beginnst." |
| B-01 abends | „Wie ist es gelaufen?" |
| B-02 leer | „Frag mich, wie du das angehen könntest." |
| B-02 Textfeld | „Oder tippen …" |
| B-03 Titel | „Wie ist es gelaufen?" |
| B-03 Haken-Stand | „⟨n⟩ von ⟨m⟩ erledigt" |
| B-03 | „Überspringen" / „Fertig" |
| B-03 leer | „Heute läuft kein Experiment." |
| B-04 Untertitel | „Was möchtest du erreichen? Die Vorschläge tasten dich Schritt für Schritt heran." |
| B-04 leer | „Noch keine Ziele. Sprich das erste ein." |
| B-05 leer | „Nichts gemerkt. Wenn dir ein Vorschlag gefällt, tipp auf das Lesezeichen." |
| B-06 Untertitel | „Was sich aus deinen Auswertungen ergeben hat." |
| B-06 leer | „Noch nichts. Das wächst mit deinen Auswertungen." |
| B-07 Reiter | „Letzte 15 Tage" · „Langzeit" |
| B-07 leer | „Noch nichts aufgeschrieben." |
| B-08 Hinweis | „Das Logbuch darf ein anderes Modell benutzen als die Experimente." |
| B-08 | „Probe hören" · „Stimme aufnehmen" · „Anmelden" · „Abmelden" |
| B-09 Untertitel | „Alles, was die App dauerhaft über dich wissen soll. Je mehr hier steht, desto genauer treffen die Vorschläge." |
| B-09 Platzhalter | „Wer bist du? Was prägt dich? Was war? Sprich einfach drauflos." |
| Wartezustand | „Ich sehe mir deine letzten Tage an …" |
| Fehler, kein Netz | „Dafür brauche ich Netz." |
| Fehler, Mikrofon | „Ohne Mikrofon kann ich dich nicht hören." |
| Fehler, leer | „Da war nichts zu hören." |
| Fehler, Anmeldung | „Deine Anmeldung ist abgelaufen." |
| Fehler, Kontingent | „Dein Kontingent ist erschöpft." |
| Fehler, Antwort | „Die Antwort war unbrauchbar." |
| Fehler, Verbessern | „Der Text konnte nicht verbessert werden." |
| Fehler, Stimme | „Für diese Stimme fehlt der Schlüssel." |
| Überall | „Nochmal versuchen" |
| Erinnerung morgens | „Wie ist deine Lage heute?" |
| Erinnerung abends | „Wie ist es gelaufen?" |

---

## 9. Barrierefreiheit

Die App ist ausschließlich für Frank. Es gelten keine Store-Vorgaben. Festgelegt wurde nur,
was sich aus der Gestaltung ergibt:

- **Mindest-Tippfläche 48 × 48 dp** für jedes bedienbare Element (folgt aus „luftig, große
  Tippflächen").
- **Kontrast:** Text auf Grund erreicht in beiden Erscheinungen mindestens 7:1
  (Dunkel: `#F4EEE7` auf `#151210` · Hell: `#1E1915` auf `#F8F4EE`). Gedämpfter Text
  mindestens 4,5:1.
- **Große Systemschrift** wird übernommen; Karten wachsen mit, Texte werden nie abgeschnitten.
- **Reduzierte Bewegung** wird beachtet — siehe Motion-Spec §8.

---

## 10. Offene Fragen

Keine.
