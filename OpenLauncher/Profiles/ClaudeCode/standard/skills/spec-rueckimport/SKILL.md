---
name: spec-rueckimport
description: >-
  Stufe 2 der Programm-Pipeline. Liest den Ruecklauf des Designers aus
  ~/proggs/Designs/Outbox/ — das ZIP <App>-SPEC-v2.zip mit den fortgeschriebenen Specs
  und dem Werft-Design-Paket —, gleicht ihn gegen Specs/<App>/v1/ ab, fragt fuer jedes neu
  hinzugekommene Bedienelement nach, was es tun soll (Gestrichenes wird NICHT gefragt: das
  Design gewinnt), und schreibt daraus das
  Bau-Spec-Paket Specs/<App>/v2/ mit 01-FUNKTIONS-SPEC.md (v1 plus die neuen Funktionen
  aus dem Design), 02-UI-SPEC.md und 03-MOTION-SPEC.md (beide mit den gemessenen Werten
  aus dem Design), AENDERUNGEN.md und BAU-AUFTRAG.md fuer Stufe 3. Nutze diesen Skill
  IMMER wenn der Benutzer sagt "Rueckimport", "Rueckimport <App>", "spec-rueckimport",
  "/spec-rueckimport", "Design ist fertig", "Design zurueckgekommen", "Outbox einlesen",
  "Design einlesen", "Design aus der Outbox", "Werft-Paket einlesen", "Design zu Spec",
  "Spec v2 bauen", "neues Spec aus dem Design", "Stufe 2", "das Design ist zurueck,
  mach weiter", "Designer hat geliefert", "aus dem Design ein Spec machen". NICHT nutzen,
  um ein Erst-Spec zu erstellen (dann: spec-schmiede) oder um zu bauen
  (dann: design-umsetzer).
---

# Spec-Rueckimport — Stufe 2 der Programm-Pipeline

Der Designer hat geliefert. Deine Aufgabe: aus dem Ruecklauf und dem Erst-Spec ein
**Bau-Spec** machen, das Stufe 3 ohne Rueckfragen umsetzen kann.

Du baust hier **keinen Code**. Du schreibst Text — aber Text, der ab jetzt verbindlich ist.

Der Gesamtablauf steht in **`~/proggs/Specs/README.md`**, der verbindliche Aufbau jeder
Datei in **`~/proggs/Specs/FORMAT.md`**. Beide liest du zu Beginn. Weicht dieser Skill von
`FORMAT.md` ab, gilt `FORMAT.md`.

## Die Grundhaltung

**Gemessen schlaegt beschrieben.** Wo ein Wert im Design gemessen vorliegt, wird er
uebernommen — nicht gerundet, nicht vereinheitlicht, nicht "verbessert". Die Absicht aus v1
zaehlt nur dort, wo das Design schweigt.

**Das Design gewinnt.** Was im Ruecklauf nicht mehr vorkommt, ist gestrichen — samt seiner
Funktion. Loescht der Designer einen Knopf, weil er ihm nicht gefaellt, verschwindet mit ihm,
was dahinter haengt. Das Erst-Spec liegt unveraendert in `Designs/Inbox/` als Rueckfallebene;
deshalb wird ueber Streichungen **nicht** gefragt, sondern nur in `AENDERUNGEN.md` berichtet.

**Eine Ausnahme, und nur diese:** meldet das Werft-Paket `vollstaendigkeit.nichtAufgebaut`,
dann wurde ein Bildschirm gemessen, aber nicht gebaut. Das ist keine Entscheidung des
Designers, sondern eine misslungene Erzeugung. Sie wird dem Benutzer **gemeldet** — gefragt
wird auch hier nicht.

**Neues wird erfragt, nicht erfunden.** Hat der Designer ein Bedienelement ergaenzt, dessen
Aufgabe nirgends steht, fragst du. Du erfindest keine Funktion dazu.

---

## Ablauf

0. Ruecklauf finden und zuordnen
1. Ruecklauf vollstaendig lesen
2. Abgleich gegen v1
3. Rueckfrage-Runde
4. `Specs/<App>/v2/` schreiben
5. Vollstaendigkeits-Nachweis und Uebergabe an Stufe 3

Melde zu Beginn: "Rueckimport gestartet. Ich lese den Ruecklauf aus `Designs/Outbox/`,
vergleiche ihn mit `Specs/<App>/v1/` und frage dich zu allem, was neu dazugekommen ist.
Am Ende liegt das Bau-Spec unter `Specs/<App>/v2/`."

---

## Phase 0 — Ruecklauf finden und zuordnen

1. `Specs/README.md` und `Specs/FORMAT.md` lesen. Aktuelles Datum ermitteln.
2. `ls ~/proggs/Designs/Outbox/` — den Ruecklauf zum genannten App-Namen suchen.
   **Der Regelfall ist ein ZIP:** `Designs/Outbox/<App>-SPEC-v2.zip`, von Werft Studio
   ueber *Projekt als ZIP herunterladen* dort abgelegt.

**Dateiname unklar? Fragen, nicht raten.** Gibt es keine Datei, deren Name zum App-Namen
passt, oder passen mehrere, dann listest du den Inhalt von `Designs/Outbox/` mit Datum und
Groesse auf und fragst: "Welche Datei soll ich umsetzen?" Erst nach der Antwort geht es
weiter. Nie die neueste Datei einfach annehmen.

3. **ZIP auspacken.** Nach `~/proggs/Designs/Outbox/<App>/` entpacken
   (PowerShell: `Expand-Archive -Path … -DestinationPath …`). Vorher pruefen, ob der
   Zielordner schon existiert — wenn ja, fragen, ob ueberschrieben wird.
   Danach den Archivinhalt auflisten und melden, was drin war.

4. **Feststellen, was im Ruecklauf steckt.** Nach dem Auspacken koennen vorliegen:

| Fund | Rolle |
|------|-------|
| `01-FUNKTIONS-SPEC.md`, `02-UI-SPEC.md`, `03-MOTION-SPEC.md` | Der **Vollausbau**: Werft hat die Specs selbst fortgeschrieben und fuer die Zielplattform uebersetzt. Dann ist Phase 1b/2 nur noch Pruefung, nicht Rekonstruktion |
| `SPEC.md` (Zusammenstellung) | Die vom Designer fortgeschriebene Gesamtspec. Quelle fuer **Absicht, neue Funktionen, Begruendungen** |
| `WERFT-DESIGN/` | Das Werft-Paket. Quelle fuer **alle gemessenen Zahlenwerte** und fuer den Augenschein |
| `*.dc.html` | Ein Claude-Design statt eines Werft-Pakets. Dann ist die `.dc.html` die Wertequelle |

   Liegen mehrere vor, gilt bei Widerspruch: **Zahlen aus dem Werft-Paket, Absicht aus der
   Spec-Datei.** Bringt der Ruecklauf die drei Spec-Dateien schon mit, uebernimmst du sie
   und **pruefst** sie (Phase 5), statt sie neu zu erzeugen.

5. Fund zeigen und bestaetigen lassen:
   "Ich habe fuer `<App>` gefunden: … Ist das der Ruecklauf, den ich einlesen soll?"
6. **Zielplattform feststellen.** Sie steht **ganz oben in `00-PROJEKT.md`** als Stempel
   („Zielplattform fuer diesen Bau: …, beim Herunterladen gewaehlt“). Dieser Stempel gilt vor
   jeder aelteren Angabe weiter unten in derselben Datei — er haelt fest, wofuer der Benutzer
   das Paket tatsaechlich heruntergeladen hat. Fehlt er, gilt §2 bzw. Teil D der `SPEC.md`.
   Sie entscheidet, welchen Bau-Weg Stufe 3 nimmt — Android/Compose,
   Windows/WPF oder macOS/SwiftUI. Sie wird in `00-PROJEKT.md` von v2 und in
   `BAU-AUFTRAG.md` §2 unmissverstaendlich festgehalten. Fehlt sie oder widerspricht sie
   v1, **fragen**.
7. `ls ~/proggs/Specs/<App>/v1/` — die Vergleichsbasis muss da sein. Fehlt sie,
   sofort melden: ohne v1 gibt es keinen Abgleich und damit kein v2. Dann fragen, ob
   stattdessen aus dem Design allein ein Spec gebaut werden soll (das ist ein anderer,
   schwaecherer Lauf — ohne `AENDERUNGEN.md`).
8. `ls ~/proggs/Specs/<App>/v2/` pruefen. Existiert es schon, **nicht** ueberschreiben —
   fragen, ob ergaenzt wird oder ob es `v3` wird.

---

### Aktualitaet des Ruecklaufs nachweisen — bevor irgendetwas gelesen wird

> Ein veralteter Export sieht aus wie ein aktueller. Die ganze Kette laeuft dann sauber
> durch und baut den **falschen Entwurf** — ohne dass irgendwo ein Fehler auftaucht.

Darum zuerst den Stand des Ruecklaufs zeigen und bestaetigen lassen:

```bash
# Wann wurde exportiert? Der Zeitstempel IM Archiv zaehlt, nicht der der Datei —
# die Datei traegt den Zeitpunkt des Herunterladens.
unzip -l "Designs/Outbox/<App>*.zip" | head -5
```

Dem Benutzer beides nennen und **ausdruecklich** fragen:

> „Der Export im Archiv ist vom ⟨Datum, Uhrzeit⟩. Hast du seither im Design-Programm noch
> etwas geaendert? Wenn ja, lade den Entwurf bitte neu herunter — sonst baue ich den alten."

Erst nach einer klaren Antwort weiterarbeiten. Kommt keine, wird **nicht** angenommen, der
Stand sei aktuell — es wird nachgefragt.

**Besser als jede Nachfrage waere, den Entwurf selbst zu holen.** Solange der Download ein
Handgriff des Benutzers ist, kann er schiefgehen: falsches Archiv, alter Stand, vergessener
Export. Liegt ein Zugang zum Design-Server vor (bei Werft Studio unter `~/SK/werft-studio/`:
Adresse, Anmeldung, Zertifikat), holt Stufe 2 den aktuellen Stand selbst und weist ihn nach.
Der Briefkasten in `Designs/Outbox/` bleibt dann nur der Rueckfallweg.

---

## Phase 1 — Ruecklauf vollstaendig lesen

### 1a. Die Spec-Datei des Designers

Die Spec-Dateien aus dem ausgepackten ZIP **vollstaendig** lesen — auch wenn sie lang sind,
notfalls in mehreren Baecken mit `offset`/`limit`. Achte auf:

- **Kennungen** (`B-`, `F-`, `M-`) — sie sollen dieselben sein wie in v1.
- Markierungen **`NEU`** und **`ENTFALLEN`**. Sind sie da, sparen sie Rueckfragen.
  Fehlen sie, ist das kein Abbruch — der Abgleich in Phase 2 findet die Unterschiede ohnehin.

### 1b. Das Werft-Paket — Werte lesen, nicht schaetzen

Liegt `Designs/Outbox/<App>/WERFT-DESIGN/` vor, ist es die Wertequelle. Reihenfolge:

1. **`design-tokens.json` vollstaendig lesen.** Aufbau:
   - `erscheinungen[]` — je Erscheinung `id`, `art` (light/dark/other) und eine
     **vollstaendige** `tokens`-Tabelle. **ALLE** kommen ins UI-Spec, nicht nur die erste.
   - `bildschirme[]` — nummeriert, mit `istStart`, `navigiertZu` und je Erscheinung dem
     Pfad zur Einzeldatei. Das ist die Bildschirmliste, fertig und ohne Raten.
   - `farben`, `masse`, `typografie`, `formen`, `effekte`, `assets`, `texte` — die
     gemessenen Werte fuer die Abschnitte 2 bis 8 des UI-Specs.
   - `vollstaendigkeit.nichtAufgebaut` — hier stehen Bildschirme, die dem Design **fehlen**.
     Ist die Liste nicht leer, dem Benutzer melden, **bevor** v2 geschrieben wird.
   - `plattform` und `geometrie` — gegen `00-PROJEKT.md` aus v1 pruefen. Weicht die
     Plattform ab, melden und fragen.
2. **`bildschirme/design.css` auf Bewegung durchsuchen.** Das ist der Schritt, der am
   leichtesten vergessen wird und ohne den das Motion-Spec luecken hat:
   `grep -oE "@keyframes [a-zA-Z0-9_-]+|animation:[^;]*|transition:[^;]*"`.
   **Der Abschnitt `effekte` in `design-tokens.json` ist erfahrungsgemaess deutlich
   unvollstaendiger als die CSS** — in einem realen Paket standen dort 5 Eintraege,
   waehrend die CSS 36 `@keyframes` enthielt. Die CSS ist die Wahrheit fuer Bewegung;
   `effekte` ist nur ein Auszug. Fuer jede gefundene Bewegung Dauer, Kurve, Verzoegerung,
   Wiederholungsart und Richtung woertlich uebernehmen.
3. **Jeden Bildschirm in JEDER Erscheinung ansehen**
   (`bildschirme/<erscheinung>/<nr>-<name>.html`). Ein Bildschirm gilt erst als erfasst,
   wenn er in allen Erscheinungen angesehen wurde — dort steht das fertige Markup mit
   seinen Klassen, die Regeln dazu in `design.css`.
4. **`design.html` fuer den Klickweg.** `data-werft-navigate="<ziel-id>"` ist die
   Navigation, `data-screen-id` / `data-screen-name` die Bildschirm-Kennung.
5. **`DESIGN-SPEC.md`** ist die lesbare Fassung derselben Werte und enthaelt die
   Bildschirm-Tabelle — sie ist die Abhakliste fuer Phase 5.
6. **Den Entwurf VERMESSEN — der wichtigste Schritt der ganzen Stufe.**

   > Jede Zusammenfassung verliert etwas, und was das Spec nicht sagt, erfindet die
   > Umsetzung. Deshalb wird hier nichts zusammengefasst: Ein Skript oeffnet jeden
   > Bildschirm im Browser und liest fuer **jedes** Element die **berechneten** Werte aus.
   > Vererbung, `var()`, `color-mix()` und die gestaffelten Regelschichten sind darin
   > bereits aufgeloest. Das Ergebnis ist vollstaendig **durch Konstruktion**.

   ```powershell
   .\references\messe-design.ps1 `
       -Design ~/proggs/Designs/Outbox/<App> `
       -Ziel   ~/proggs/Specs/<App>/v2
   ```

   Erzeugt je Erscheinung und Bildschirm:
   - `Specs/<App>/v2/messung/<erscheinung>/<name>.json` — Kasten (x/y/Breite/Hoehe),
     alle formgebenden Eigenschaften, `::before`/`::after`, Texte, Beschriftungen,
     `data-werft-funktion`, Navigationsziele und die `@keyframes`.
   - `Specs/<App>/v2/bilder/<erscheinung>/<name>.png` — dasselbe als Bild.

   **Diese Messung ist ab hier die verbindliche Bauvorlage.** Das UI-Spec beschreibt sie
   lesbar, ersetzt sie aber nicht. Steht etwas in der Messung und nicht im Spec, gilt die
   Messung — und das Spec ist zu ergaenzen.

   **Sieh dir jedes gerenderte Bild danach selbst an.** Was du im Bild siehst und in der
   Messung nicht wiederfindest, ist ein Fehler des Messfuehlers — melden, nicht uebergehen.
7. **Aufbau je Bildschirm aus dem Bild beschreiben.** Eine Liste der Bedienelemente ist
   KEIN Aufbau. Aus dem gerenderten Bild je Bildschirm festhalten:
   - Steht die Beschriftung **ueber** oder **neben** ihrem Feld?
   - Sind Abschnitte **Karten** (Flaeche, Rand, Schatten) oder nur Text mit Abstand?
   - Wie breit ist ein Bedienelement — volle Breite oder nur so breit wie sein Inhalt?
   - Schwebt die untere Leiste (Abstand zu den Kanten, eigener Radius) oder sitzt sie bündig?
   - Traegt das aktive Feld eine Pille, eine Linie oder nur Farbe?

   In Lauf 01 fehlte genau das: das UI-Spec listete fuer B-08 nur die Bedienelemente, und
   Stufe 3 hat die Anordnung erfunden — Beschriftungen kamen neben statt ueber die Felder,
   Abschnitte wurden nicht zu Karten. Ergebnis war ein anderer Bildschirm.
8. **Effekte den Bauteilen ZUORDNEN.** Eine Tabelle `gradient(5) = linear-gradient(…)` ohne
   Angabe, wozu sie gehoert, ist ein Anhang und keine Bauanweisung — sie wird beim Bauen
   uebersprungen. Jeder Verlauf, Schatten und Schein bekommt sein Bauteil:

   | Bauteil | Effekt | Wert |
   |---------|--------|------|
   | Sprechknopf | Verlauf | `linear-gradient(145deg, mix(Aktion,Text 10%), Aktion 58%, mix(Aktion,#000 16%))` |
   | Sprechknopf | Schein | `0 16px 32px Aktion/30%`, `inset 0 2px 0 Text/28%` |
   | Karte | Schatten | `0 16px 32px #000/18%`, `0 0 24px Aktion/7%`, `inset 0 1px 0 Text/12%` |

   Finde die Zuordnung ueber den Selektor in der CSS, nicht ueber Vermutung.

### 1c. Ein Claude-Design statt eines Werft-Pakets

Liegt statt `WERFT-DESIGN/` eine `*.dc.html` vor, wird diese **vollstaendig** gelesen und
die Werte daraus extrahiert (Farbvariablen je `data-t`-Theme, `font-size`/`font-weight`/
`line-height`, `padding`/`margin`/`gap`, `border-radius`, `box-shadow`, Verlaeufe,
`@keyframes`/`animation`/`transition`). `support.js` ist reine Laufzeit und wird **nicht**
gelesen. Details zum Dateiaufbau stehen in
`../design-umsetzer/references/design-format.md`.

---

## Phase 2 — Abgleich gegen v1

Jetzt wird verglichen — mechanisch, Kennung fuer Kennung. Lies dazu alle vier Dateien aus
`Specs/<App>/v1/`.

**Bildschirme:** Fuer jede `B-`-Kennung aus v1 und jeden Bildschirm im Ruecklauf eine Zeile:

| Kennung | in v1 | im Ruecklauf | Bewertung |
|---------|-------|--------------|-----------|
| B-01 | Start | Start | unveraendert |
| B-04 | — | Verlauf-Filter | **NEU** |
| B-06 | Export | — | **ENTFALLEN** |

**Bedienelemente:** Fuer **jedes** antippbare Element im Design pruefen, ob es in v1 eine
Entsprechung hat — entweder eine Funktion (`F-`) oder ein Navigationsziel (`B-`). Elemente
ohne beides sind **Kandidaten fuer neue Funktionen** und gehen in Phase 3.

**Gestaltung — hier gewinnt IMMER die Messung.** Wo das Gemessene von der v1-Absicht
abweicht (Farbe, Schrift, Mass, Form, Tiefe, Bewegung), ist das kein Fehler — der Designer
darf das. Aber es reicht **nicht**, es nur festzuhalten:

> **Die widersprechende v1-Aussage wird in v2 GESTRICHEN, nicht danebengestellt.**

Stehen in v2 die gemessenen Werte **und** ein v1-Satz, der ihnen widerspricht, dann baut
Stufe 3 nach dem lesbaren Satz und ignoriert die Tabelle — und kann sich dabei auf das Spec
berufen. Genau so ging in Lauf 01 die gesamte Tiefen-Schicht verloren: v2 enthielt die
gemessenen Verlaeufe und Schatten **und** den v1-Satz „Keine Schatten. Keine Verlaeufe."
Gebaut wurde nach dem Satz.

Darum: Jede v1-Gestaltungsaussage gegen die Messung pruefen. Wird sie widerlegt, kommt sie
**nicht** nach v2 — stattdessen ein Eintrag in `AENDERUNGEN.md`:

| v1 sagte | Das Design misst | In v2 gilt |
|----------|------------------|------------|
| „Keine Schatten, keine Verlaeufe" | 40 `box-shadow`, 10 Verlaeufe | die Messung; v1-Satz gestrichen |

**Achte besonders auf gestaffelte CSS:** Ein Werft-Paket kann dieselben Bauteile zweimal
beschreiben — eine flache Grundschicht und darueber eine Schicht, die auf
`.werft-screen[data-screen-id="B-xx"]` eingeschraenkt ist und Verlaeufe, Schatten, Auren und
schwebende Leisten ergaenzt. **Die obere Schicht ist das, was der Benutzer im Design-Programm
sieht — sie ist verbindlich.** Pruefe im Zweifel gegen `DESIGN-SPEC.md`: was der Designer
dort als Effekt auffuehrt, gehoert zum Entwurf.

**Bewegung:** Welche `M-`-Kennungen aus v1 finden sich im Design wieder, welche Bewegungen
sind neu dazugekommen, welche fehlen?

**Funktionen:** Welche `F-`-Kennungen aus v1 haben im Design keinen Ort mehr? Eine Funktion
ohne Bildschirm ist entweder vergessen worden oder absichtlich gestrichen — das ist eine
Frage fuer Phase 3, keine Entscheidung fuer dich.

---

## Phase 3 — Rueckfrage-Runde

Erst fragen, dann schreiben. Wie im Grilling: **eine Frage nach der anderen, jede mit
deiner Empfehlung.**

Zu fragen ist:

1. Fuer **jedes neue Bedienelement ohne Aufgabe**: "Der Designer hat auf `B-03` einen
   Knopf 'Teilen' eingebaut, den es in v1 nicht gab. Was soll er tun? Mein Vorschlag: …"
   Aus der Antwort wird eine neue Funktion mit der naechsten freien `F-`-Kennung.
2. Fuer **jeden neuen Bildschirm**: Was ist sein Zweck, welche Funktionen liegen darauf,
   wie kommt man hin und wieder zurueck?
3. **Ueber Entfallenes wird nicht gefragt.** Was im Design fehlt, faellt aus v2 heraus —
   auch die zugehoerige Funktion. Es wird in `AENDERUNGEN.md` festgehalten, damit
   nachvollziehbar bleibt, was der Designer weggelassen hat, aber es haelt den Lauf nicht auf.
4. Bei **echten Widerspruechen** zwischen Design und Funktions-Spec (das Design zeigt einen
   Ablauf, den `01-FUNKTIONS-SPEC.md` anders beschreibt): vorlegen und entscheiden lassen.
   Hier gilt **nicht** automatisch "Design gewinnt" — beim Verhalten gewinnt das
   Funktions-Spec, und ein Widerspruch ist deshalb eine Frage.

Alles, was auch nach der Runde offen bleibt, kommt woertlich unter *Offene Fragen* in die
betroffene Datei und zusaetzlich in `BAU-AUFTRAG.md` §5.

---

## Phase 4 — `Specs/<App>/v2/` schreiben

Nach `~/proggs/Specs/<App>/v2/`, im Aufbau **exakt nach `Specs/FORMAT.md`**.
`v1` wird dabei **nicht angefasst**.

| Datei | Woraus |
|-------|--------|
| `00-PROJEKT.md` | aus v1 uebernommen; nur geaendert, wo der Ruecklauf etwas umwirft (z. B. Plattform, Zielgeraet) |
| `01-FUNKTIONS-SPEC.md` | v1 **minus** was im Design fehlt, **plus** die in Phase 3 geklaerten neuen Funktionen |
| `02-UI-SPEC.md` | die **gemessenen** Werte aus dem Design — alle Erscheinungen, alle Bildschirme, alle Texte |
| `03-MOTION-SPEC.md` | die **gemessenen** Bewegungen, je mit `@keyframes`-Namen bzw. Fundstelle als Quelle |
| `04-ONBOARDING-SPEC.md`, `05-RECHT-SPEC.md` | aus dem Ruecklauf uebernommen, falls vorhanden — Werft misst sie nicht, es reicht sie durch |
| `AENDERUNGEN.md` | das Ergebnis von Phase 2 und 3, vollstaendig |
| `BAU-AUFTRAG.md` | die Einstiegsdatei fuer Stufe 3, kurz und verweisend |

**Regeln beim Schreiben:**

- **Kennungen bleiben.** Was in v1 `B-03` war, bleibt `B-03`. Neues bekommt die naechste
  freie Nummer. Nur so laesst sich am Ende nachweisen, dass nichts verlorenging.
- **Werte woertlich.** Hex mit allen Stellen, Alpha ungerundet, Dauern in ms wie gemessen,
  Kurven als vollstaendiges `cubic-bezier(...)`. Kein "ca.", kein "etwa 20 px".
- **Jede Bewegung mit Quelle.** `03-MOTION-SPEC.md` nennt je Bewegung, woher der Wert
  stammt (`@keyframes pm-fx-breathe` in `bildschirme/design.css`). Das macht Stufe 3
  ueberpruefbar.
- **Jedes Bedienelement hat ein Ziel.** Im UI-Spec steht hinter jedem antippbaren Element
  entweder eine `F-`-Kennung oder eine `B-`-Kennung. Keines bleibt ohne.
- **`BAU-AUFTRAG.md` §4 ist die Abhakliste**: alle `B-`, `F-`, `M-` und `A-`-Kennungen als
  Liste. Stufe 3 hakt sie ab.

---

## Phase 5 — Vollstaendigkeits-Nachweis und Uebergabe

Bevor du fertig meldest, jeden Punkt pruefen:

- Ist **jeder** Bildschirm aus `design-tokens.json` bzw. der Tabelle in `DESIGN-SPEC.md`
  in `02-UI-SPEC.md` beschrieben — in **jeder** Erscheinung?
- Ist **jede** `@keyframes`/`animation`/`transition` aus `design.css` in
  `03-MOTION-SPEC.md` gelandet, oder ausdruecklich als unbenutzt vermerkt?
- Hat **jedes** antippbare Element eine `F-` oder `B-`-Kennung als Ziel?
- Ist **jede** `F-`-Kennung, die im Design noch ein Bedienelement hat, auch in v2 vorhanden?
- Ist **jede** in v2 fehlende `F-`-Kennung aus v1 in `AENDERUNGEN.md` als gestrichen aufgefuehrt?
- Steht **jede** Erscheinung mit vollstaendiger Farbtabelle im UI-Spec?
- Ist `vollstaendigkeit.nichtAufgebaut` leer — und wenn nicht, ist es gemeldet?

Dann berichten:

1. Die sechs Dateien mit Pfad.
2. Die Kernaussage aus `AENDERUNGEN.md` in fuenf Zeilen: was neu ist, was entfiel,
   was sich gestalterisch verschoben hat.
3. Die noch offenen Fragen.
4. **Direkt an Stufe 3 uebergeben.** Sind keine Fragen offen, rufst du **sofort** den
   `design-umsetzer` fuer `<App>` auf — der Benutzer muss dafuer nichts eintippen.
   Sage nur den einen Satz: "Alles geklaert. Ich uebergebe an den Design-Umsetzer,
   er baut `<App>` fuer `<Plattform>`."
   Sind noch Fragen offen, legst du sie zuerst vor; erst nach ihrer Klaerung wird
   uebergeben. Ein Bau-Auftrag mit offenen Fragen geht nicht in den Bau.

---

## Was NIEMALS passieren darf

- ❌ `Specs/<App>/v1/` veraendern oder ueberschreiben — v1 ist der Beweis, wogegen verglichen wurde.
- ❌ Einen gemessenen Wert runden, vereinheitlichen oder "schoener" machen.
- ❌ Das Motion-Spec allein aus `design-tokens.json` → `effekte` bauen — die
  `bildschirme/design.css` wird **immer** mitgelesen, dort stehen die meisten Bewegungen.
- ❌ Eine Erscheinung weglassen, weil sie "nur die helle Variante" ist.
- ❌ Fuer ein neu hinzugekommenes Bedienelement eine Funktion **erfinden**, statt zu fragen.
- ❌ Eine im Design gestrichene Funktion gegen den Willen des Designs nach v2 retten — oder
  ihr Wegfallen unerwaehnt lassen: sie gehoert in `AENDERUNGEN.md`.
- ❌ Kennungen neu vergeben oder umnummerieren.
- ❌ Bei Widerspruch zwischen Design und Funktions-Spec eigenmaechtig entscheiden.
- ❌ Ein Bedienelement ohne Ziel im UI-Spec stehen lassen.
- ❌ Fertig melden, solange `vollstaendigkeit.nichtAufgebaut` Eintraege hat, die dem
  Benutzer nicht gemeldet wurden.
- ❌ Code schreiben oder ein Projekt anlegen — das ist Stufe 3.
- ❌ Vom Aufbau in `Specs/FORMAT.md` abweichen.
