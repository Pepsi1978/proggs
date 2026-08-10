# Perfect Moment — Design-Brief

**Für den Claude-Code-Designer. Auftrag: alle Bildschirme dieser App als HTML-Muster bauen, damit Frank sie vor der Programmierung optisch beurteilen kann.**

Stand: 19. Juli 2026 · Plattform: Android-Telefon (Samsung Galaxy Fold 6, Hauptbildschirm) · Sprache der gesamten Oberfläche: **Deutsch**

---

## 0. Was diese App ist — in drei Sätzen

Frank legt sich hin, wählt ein Thema, startet eine Sitzung und schließt die Augen. Eine KI erzeugt daraufhin endlos neue Fragen zu diesem Thema, die ihm eine ruhige Stimme vorliest — jede Frage mehrfach, mit einstellbaren Pausen dazwischen. Die Fragen werden **nicht beantwortet**; sie sollen nur wirken.

Das heißt für das Design: Die App wird zu Beginn mit **offenen Augen** bedient und danach eine Stunde lang mit **geschlossenen Augen** gehört. Alles Wichtige passiert in den ersten zehn Sekunden. Danach darf der Bildschirm praktisch verschwinden.

---

## 1. Gestalterische Grundhaltung

**Ruhe vor Information. Wärme vor Kontrast. Bewegung vor Übergang.**

- Nichts blinkt, nichts springt, nichts vibriert.
- Jeder Zustandswechsel wird **überblendet**, niemals hart geschnitten. Richtwert: 600–900 ms, weiche Kurve (`cubic-bezier(0.4, 0.0, 0.2, 1)`).
- Große Freiflächen. Lieber ein Element weniger als eines zu viel.
- Keine harten Rahmen. Abgrenzungen entstehen durch minimale Helligkeitsunterschiede, nicht durch Linien.
- Ecken durchgehend weich: 20 px an Karten, 28 px an Flächen, vollrund an Knöpfen.
- Keine Schlagschatten im Dunkelmodus. Im Hellmodus nur sehr weiche, warme Schatten (`0 2px 24px rgba(120, 84, 24, 0.08)`).

---

## 2. Farbe — Gold in zwei Modi

Beide Modi sind gleichrangig. Umschaltbar in den Einstellungen: **Hell / Dunkel / wie System**. Der Dunkelmodus ist der Standard.

### 2.1 Dunkelmodus (Standard)

Grundgedanke: fast schwarz, aber **warm** schwarz — kein Blaustich. Gold ist die einzige Farbe.

| Rolle | Wert | Verwendung |
|---|---|---|
| Hintergrund | `#0A0806` | Vollflächig. Nahezu AMOLED-schwarz, minimal ins Bräunliche |
| Fläche | `#151009` | Karten, Eingabefelder, Listenzeilen |
| Fläche erhöht | `#1F1710` | Aktive Karte, aufgeklapptes Menü |
| Gold primär | `#D4A24C` | Knöpfe, aktive Zustände, Häkchen, Fortschrittsring |
| Gold hell | `#F0C97A` | Aktuelle Frage, Überschriften, Hervorhebung |
| Gold gedämpft | `#8A6E36` | Inaktive Symbole, Trennlinien |
| Bernstein / Akzent | `#E8873B` | **Nur** der aktive Lautsprecher und der Aufnahmeknopf |
| Text primär | `#F5EEE2` | Fragen, Überschriften |
| Text sekundär | `#A2957F` | Beschriftungen, Hilfetexte |
| Text schwach | `#5A5145` | Vergangene Fragen, Platzhalter |
| Warnung | `#C4634A` | Fehler, Löschen — sparsam, entsättigt |

### 2.2 Hellmodus

Grundgedanke: warmes Pergament, dunkles Altgold. Kein Reinweiß, kein Reinschwarz.

| Rolle | Wert | Verwendung |
|---|---|---|
| Hintergrund | `#FBF6EC` | Vollflächig, cremig |
| Fläche | `#F3EAD9` | Karten, Eingabefelder |
| Fläche erhöht | `#EDE1CA` | Aktive Karte |
| Gold primär | `#A87A2A` | Knöpfe, aktive Zustände |
| Gold dunkel | `#7A5518` | Aktuelle Frage, Überschriften |
| Gold gedämpft | `#C7AE7E` | Inaktive Symbole, Trennlinien |
| Bernstein / Akzent | `#C4661F` | Aktiver Lautsprecher, Aufnahmeknopf |
| Text primär | `#241D12` | Fragen, Überschriften |
| Text sekundär | `#6B5D48` | Beschriftungen |
| Text schwach | `#A2947C` | Vergangene Fragen, Platzhalter |
| Warnung | `#A33F28` | Fehler, Löschen |

### 2.3 Der lebende Hintergrund

Auf beiden Modi liegt ein **sehr langsam atmender Goldschimmer** — ein radialer Verlauf, der über 20 Sekunden von 0 % auf 6 % Deckkraft anschwillt und wieder abfällt, dabei minimal wandert. Er sitzt hinter allem, mittig, mit großem Radius.

```css
/* Dunkelmodus */
background:
  radial-gradient(120% 80% at 50% 35%, rgba(212, 162, 76, 0.10), transparent 60%),
  #0A0806;
animation: atmen 20s ease-in-out infinite;
```

Im Hellmodus dieselbe Bewegung mit `rgba(168, 122, 42, 0.07)`.

Dieses Atmen läuft **auf allen Bildschirmen** und ist die einzige durchgehende Bewegung der App. Auf dem Sitzungsbildschirm wird es langsamer (30 Sekunden), damit es nicht mit dem Vorlese-Takt konkurriert.

---

## 3. Schrift

Zwei Schriften, klar getrennt nach Aufgabe.

**Fragen — eine helle Serifenschrift.** Sie ist das Herz der App und muss sich vom Rest absetzen. Empfehlung: `Newsreader`, `Fraunces` oder `Source Serif 4`, Schnitt **Light (300)**, optisch großzügig.

**Alles andere — eine ruhige Groteske.** Empfehlung: `Inter`, Schnitte 400 / 500 / 600.

| Verwendung | Schrift | Größe | Zeilenabstand | Laufweite |
|---|---|---|---|---|
| Aktuelle Frage (Sitzung) | Serif 300 | 32 px | 1,55 | 0 |
| Kommende / vergangene Fragen | Serif 300 | 20 px | 1,55 | 0 |
| Bildschirm-Überschrift | Sans 600 | 26 px | 1,25 | −0,3 px |
| Karten-Aufhängertext | Sans 500 | 16 px | 1,4 | 0 |
| Beschriftung / Abschnittstitel | Sans 500 | 13 px | 1,3 | +0,8 px, **Großbuchstaben** |
| Fließtext, Hilfetexte | Sans 400 | 15 px | 1,55 | 0 |
| Zahlenwerte an Reglern | Sans 600 | 18 px | 1,0 | 0 |

Fragen werden **linksbündig** gesetzt, nicht zentriert — zentrierter Fließtext liest sich mit müden Augen schlechter. Zeilenlänge maximal 34 Zeichen.

---

## 4. Die Bildschirme

Es gibt **fünf**. Bitte alle fünf als HTML-Muster bauen, jeweils in Hell und Dunkel.

```
① Startbildschirm  →  ② Sitzung  →  (Ende) → ① 
①  ⇄  ③ Verlauf
①  ⇄  ④ Einstellungen  →  ⑤ Unterseiten (Skills / Aufhänger / Stimme / KI)
```

---

### ① Startbildschirm

Der einzige Bildschirm, den Frank mit klarem Kopf bedient. Ziel: **zwei Tipps von „App auf" zu „Augen zu".**

Aufbau von oben nach unten:

**Kopfzeile** (56 px hoch, transparent)
- Links: Wortmarke „Perfect Moment", Sans 600, 18 px, Gold primär
- Rechts: zwei Symbole nebeneinander, je 24 px, Gold gedämpft — Uhr-Symbol (→ Verlauf), Zahnrad (→ Einstellungen)

**Aufhänger-Karussell** (oberes Drittel)
- Waagerecht scrollbare Kartenreihe, angeschnittene nächste Karte rechts sichtbar (ca. 24 px), damit die Wischbarkeit erkennbar ist
- Kartengröße: 168 × 168 px, Radius 20 px, Abstand 12 px
- Karteninhalt: Emoji oben links (32 px), darunter der Aufhängertext (Sans 500, 16 px, max. 4 Zeilen)
- **Inaktiv**: Fläche `#151009`, Text sekundär
- **Ausgewählt**: Fläche erhöht, 1,5 px Rand in Gold primär, Text primär, Emoji leicht vergrößert (1,08×), sanftes Aufleuchten über 400 ms
- Links vor der ersten Karte ein Abstand von 20 px, rechts ebenso

**Fragefenster** (Mitte)
- Beschriftung darüber: „ODER EIGENE FRAGE" (Beschriftungsstil)
- Textfeld, volle Breite minus 40 px, Mindesthöhe 96 px, Radius 20 px, Fläche `#151009`, kein sichtbarer Rand
- Platzhalter: „Was möchtest du hören?" in Text schwach
- Wird ein Aufhänger gewählt, erscheint dessen Text hier **grau vorbelegt und editierbar**. Tippt Frank hinein und ändert etwas, verliert die Karte oben ihre Auswahl.

**Aufnahmeknopf** (unter dem Fragefenster, mittig)
- Kreis, 72 px, Fläche `#151009`, Mikrofonsymbol 28 px in Gold primär
- **Beim Aufnehmen**: Füllung Bernstein, Symbol wird zu einem Quadrat (Stopp), und drei konzentrische Ringe pulsieren nach außen (je 1,6 s, versetzt gestartet, Deckkraft 0,35 → 0)
- **Beim Verarbeiten**: Symbol verschwindet, ein goldener Bogen dreht sich im Kreis
- Darunter ein Statuswort in Text sekundär: „Antippen zum Sprechen" / „Ich höre zu…" / „Einen Moment…"

**Einstellzeile** (unteres Viertel)

Drei Werte nebeneinander, jeder als antippbares Feld mit Beschriftung darüber und Wert darunter:

| PAUSE | WIEDERHOLUNGEN | DAUER |
|---|---|---|
| 8 s | 3× | 30 min |

Antippen öffnet ein tiefliegendes Blatt (Bottom Sheet) mit dem jeweiligen Regler bzw. der Auswahl:
- **Pause zwischen Wiederholungen**: Schieberegler 1–30 s
- **Pause bis zur nächsten Frage**: Schieberegler 1–60 s *(liegt im selben Blatt wie oben, zwei Regler untereinander)*
- **Wiederholungen**: Schieberegler 1–10
- **Dauer**: sieben Knöpfe in zwei Reihen — 10 · 20 · 30 · 45 · 60 · 90 · 120 Minuten

Die Regler sind **golden gefüllte Bahnen** mit einem runden Griff. Der aktuelle Wert steht groß über dem Griff und wandert mit.

**Startknopf** (ganz unten, 24 px über der Systemleiste)
- Volle Breite minus 40 px, Höhe 60 px, vollrund
- Füllung: Verlauf von Gold primär nach Gold hell, waagerecht
- Beschriftung „Sitzung beginnen", Sans 600, 17 px, in der Hintergrundfarbe (`#0A0806`)
- **Gesperrt** (kein Thema gewählt / nicht mit ChatGPT verbunden): Füllung Fläche erhöht, Text schwach, nicht antippbar. Darüber eine Zeile in Text sekundär, die den Grund nennt: „Bitte zuerst ein Thema wählen" bzw. „Bitte zuerst mit ChatGPT verbinden" — Letzteres antippbar, führt in die Einstellungen.

---

### ② Sitzungsbildschirm

Öffnet sich **von unten heraufgleitend über 700 ms** und füllt den ganzen Bildschirm. Kein Zurück-Pfeil in der Kopfzeile — nur der Stopp-Knopf beendet.

**Wichtig: Die Sitzung startet stumm.** Alle 30 Fragen des ersten Blocks stehen sofort da. Erst das Antippen des Lautsprechers beginnt das Vorlesen.

**Kopfzeile** (56 px, schwebt über dem Inhalt, Hintergrund leicht verlaufend nach transparent)
- Links: der Themen-Text, Sans 400, 14 px, Text sekundär, einzeilig, bei Bedarf mit „…" abgekürzt
- Rechts, in dieser Reihenfolge:
  1. **Netz-Punkt**, 8 px Durchmesser: gefüllt in Gold gedämpft = alles gut · hohl (nur Rand) = wartet auf Verbindung
  2. **Lautsprecher-Symbol**, 26 px — der wichtigste Knopf der App:
     - **Stumm** (Ausgangszustand): weißes Lautsprechersymbol mit diagonalem Strich, Text primär, Deckkraft 0,7
     - **Spricht**: Bernstein `#E8873B`, kein Strich, und um das Symbol pulsiert ein weicher Bernstein-Schein (Radius 20 px, Deckkraft 0,25 → 0,5 → 0,25 über 2,4 s)
     - Übergang zwischen beiden über 350 ms überblendet, der Strich zieht sich beim Einschalten von der Mitte nach außen weg

**Fragenliste** (der ganze Rest)

Eine senkrecht scrollende Liste aller Fragen des aktuellen Blocks. Jede Zeile: Emoji links (24 px, oben ausgerichtet), Fragetext rechts daneben, 20 px Abstand zwischen den Zeilen.

Drei Zustände:

| Zustand | Emoji | Text | Größe |
|---|---|---|---|
| **Bereits gesprochen** | Deckkraft 0,3 | Text schwach | Serif 300, 20 px |
| **Aktuell** | volle Deckkraft, 1,15× vergrößert | Gold hell | Serif 300, **32 px** |
| **Kommend** | Deckkraft 0,55 | Text sekundär | Serif 300, 20 px |

Die Liste **scrollt von selbst** so, dass die aktuelle Frage immer bei etwa 40 % der Bildschirmhöhe steht — ruhig, über 800 ms. Frank kann jederzeit selbst scrollen; nach 5 Sekunden ohne Berührung fängt sich die Liste wieder auf die aktuelle Frage.

Der Wechsel von einer Frage zur nächsten wird **überblendet**: die alte schrumpft und verblasst, die neue wächst und leuchtet auf, beide gleichzeitig über 700 ms.

**Fortschrittsring** (fest am unteren Rand, mittig, 88 px über der Systemleiste)
- Kreis von 64 px Durchmesser, 2,5 px Strichstärke
- Der Ring **füllt sich im Takt der eingestellten Pause** — bei 8 Sekunden Pause dreht er einmal in 8 Sekunden. So sieht Frank beim kurzen Blinzeln sofort, wo er ist.
- Während gesprochen wird, steht der Ring still und pulsiert stattdessen leicht in Bernstein
- In der Mitte des Rings: die verbleibende Sitzungszeit, Sans 500, 13 px, Text sekundär, Format `24:18`
- Unter dem Ring: `Frage 7 · Wiederholung 2 von 3`, Sans 400, 12 px, Text schwach

**Stopp-Knopf** (unten rechts, 24 px vom Rand)
- Kreis 48 px, nur ein 1 px Rand in Gold gedämpft, kein Füllung, Quadrat-Symbol 16 px
- Bewusst zurückhaltend — er soll nicht versehentlich getroffen werden

**Nachschub-Hinweis**: Wenn im Hintergrund neue Fragen geholt werden, erscheint am **unteren Ende der Liste** eine Zeile mit drei goldenen Punkten, die nacheinander aufleuchten. Kein Text, kein Ton. Sind die neuen Fragen da, werden sie einfach angehängt und blenden über 600 ms ein.

**Wartezustand** (kein Netz, seit über 2 Minuten): Der Netz-Punkt oben wird hohl, unter dem Fortschrittsring erscheint in Text schwach: „Keine Verbindung — die Sitzung wartet." Sonst ändert sich nichts. Kein Ton, kein Ausrufezeichen.

**Bildschirm-Dimmung**: 30 Sekunden nach der letzten Berührung senkt sich die Helligkeit des gesamten Inhalts über 4 Sekunden auf **12 %**. Der Bildschirm bleibt an. Jede Berührung bringt ihn über 300 ms auf volle Helligkeit zurück.

**Sitzungsende**: Der Timer läuft ab → die aktuelle Frage wird noch fertig gesprochen → der Fortschrittsring wächst über 2 Sekunden zu einem gefüllten Goldkreis und verblasst → alle Fragen blenden auf Deckkraft 0,25 → in der Mitte erscheint groß und ruhig: **„Der perfekte Moment ist hier."** Serif 300, 28 px, Gold hell. Nach 6 Sekunden gleitet der Bildschirm nach unten weg und der Startbildschirm ist wieder da.

---

### ③ Verlauf

Erreichbar über das Uhr-Symbol oben links auf dem Startbildschirm. Gleitet von rechts herein.

- Kopfzeile: Zurück-Pfeil links, Überschrift „Verlauf"
- Darunter eine senkrechte Liste, neueste Sitzung zuerst
- **Eine Sitzungszeile** (Fläche `#151009`, Radius 20 px, Höhe 92 px, Abstand 12 px):
  - Zeile 1: das Thema, Sans 500, 16 px, Text primär, einzeilig
  - Zeile 2: `Gestern, 21:14 · 30 min · 42 Fragen · Seraphina`, Sans 400, 13 px, Text sekundär
  - Rechts: runder Abspiel-Knopf, 40 px, Gold primär auf Fläche erhöht
- Antippen der Zeile öffnet die **Sitzungsansicht**: alle Fragen dieser Sitzung als Liste zum Nachlesen, ganz oben ein breiter Knopf „Erneut abspielen" (dieselben Fragen, keine neue KI-Anfrage)
- Nach links wischen legt einen Löschen-Knopf in Warnfarbe frei
- **Leerer Zustand**: mittig ein großes, schwach goldenes Kreis-mit-Punkt-Symbol und darunter „Noch keine Sitzungen." in Text schwach

---

### ④ Einstellungen

Erreichbar über das Zahnrad. Gleitet von rechts herein. Aufgebaut in **Abschnitten**, jeweils mit einer Beschriftung darüber (Großbuchstaben, Text sekundär) und darunter Zeilen auf Fläche `#151009`.

**ABLAUF**
- Pause zwischen Wiederholungen — Regler 1–30 s
- Pause bis zur nächsten Frage — Regler 1–60 s
- Wiederholungen pro Frage — Regler 1–10
- Sitzungsdauer — 10 / 20 / 30 / 45 / 60 / 90 / 120 min

**INHALT**
- Gesprächsaufhänger — Zeile mit Anzahl rechts, führt zu ⑤a
- Skills — Zeile mit dem Namen des aktiven Skills rechts, führt zu ⑤b

**STIMME**
- Anbieter — Auswahl: Microsoft Edge / Google Chirp 3 HD
- Stimme — führt zu ⑤c
- Google-API-Schlüssel — Eingabefeld, Inhalt als Punkte maskiert, Augensymbol zum Aufdecken

**KI-VERBINDUNG**
- Mit ChatGPT verbinden — führt zu ⑤d. Rechts steht der Zustand: grüner Punkt + E-Mail-Adresse, oder „Nicht verbunden" in Text schwach
- Modell — Ausklappmenü: GPT 5.6 Sol / GPT 5.6 Terra / GPT 5.6 Luna
- Denkstärke — Ausklappmenü: Niedrig / Mittel / Hoch / Sehr hoch

**DARSTELLUNG**
- Erscheinungsbild — drei Knöpfe nebeneinander: Hell · Dunkel · Wie System

**ÜBER**
- Version, Paketname, ein Link zu den Rohdaten

Zeilenhöhe 60 px, Text links, Wert oder Pfeil rechts. Ausklappmenüs öffnen als tiefliegendes Blatt mit großen, gut treffbaren Einträgen; der aktive trägt ein goldenes Häkchen rechts.

---

### ⑤a Gesprächsaufhänger

- Kopfzeile mit Zurück-Pfeil, Überschrift „Gesprächsaufhänger", rechts ein **+** in Gold primär
- Senkrechte Liste aller Aufhänger. Jede Zeile: Emoji links, Text daneben, Griff-Symbol rechts zum Umsortieren
- Antippen öffnet den Bearbeiten-Bildschirm: ein Emoji-Feld (öffnet die Emoji-Tastatur) und ein mehrzeiliges Textfeld, unten „Speichern" und „Löschen"
- **+** öffnet denselben Bildschirm leer
- Die acht vorinstallierten Aufhänger verhalten sich wie alle anderen — änderbar und löschbar

Vorinstalliert:

| | |
|---|---|
| 🌅 | Wie fühlt sich ein schönes Leben an? |
| 🕊️ | Wie fühlt sich ein freies Leben an? |
| 💪 | Wie fühlt sich ein fitter Körper an? |
| 🌙 | Wie schaffe ich es, dass mein Schlaf immer tiefer wird? |
| ✨ | Warum ist das Leben schön? |
| 🧭 | Wie schaffe ich es, dass es mir immer gut geht? |
| 🌲 | Was macht mich im Wald so ruhig? |
| 🔮 | Wie fühlt sich ein Leben ohne Schmerzen an? |

---

### ⑤b Skills

- Kopfzeile mit Zurück-Pfeil, Überschrift „Skills", rechts ein **+**
- Liste aller gespeicherten Skills. Jede Zeile: Name links (Sans 500, 16 px), darunter die ersten 60 Zeichen des Skill-Textes in Text schwach, rechts ein **Häkchen in Gold primär**, wenn dieser Skill aktiv ist
- Es ist immer **genau einer** aktiv. Antippen einer Zeile macht sie aktiv, das Häkchen wandert über 300 ms
- Ein langer Druck oder das Stift-Symbol öffnet den Bearbeiten-Bildschirm: Namensfeld oben, darunter ein großes, scrollbares Textfeld mit dem vollständigen Skill-Text in **Schreibmaschinenschrift, 13 px**, damit lange Anweisungen lesbar bleiben
- Ganz unten im Bearbeiten-Bildschirm ein zusammenklappbarer Bereich **„Betriebsmodus (automatischer Anhang)"** — zeigt den Text, den die App unter jeden Skill hängt, und lässt ihn ändern. Standardmäßig zugeklappt und mit einem kurzen Hinweis versehen, wofür er da ist.
- Vorinstalliert ist ein Skill namens **„Forschungsteam"** mit dem vollständigen 50-Experten-Text

---

### ⑤c Stimme

- Kopfzeile mit Zurück-Pfeil, Überschrift „Stimme"
- Oben zwei Reiter: **Microsoft Edge** · **Google Chirp 3 HD**
- Darunter die Stimmenliste des gewählten Anbieters. Jede Zeile: Name links, Geschlechts-Kennzeichnung in Text schwach daneben, rechts ein runder **Abspiel-Knopf** (36 px) und ganz rechts ein **Häkchen** bei der gewählten Stimme
- Der Abspiel-Knopf spricht einen festen Beispielsatz: *„Wie fühlt es sich an, dass es dir gut geht?"* — während des Abspielens wird das Dreieck zum Quadrat und der Knopf färbt sich bernstein
- Ist der Google-Reiter gewählt, aber kein Schlüssel hinterlegt, ist die Liste ausgegraut und darüber steht: „Bitte zuerst einen Google-API-Schlüssel hinterlegen." mit Link zurück in die Einstellungen
- Stimmen mit dem Zusatz „Multilingual" (Seraphina, Florian) tragen einen kleinen goldenen Stern vor dem Namen

**Edge-Stimmen (6), alle Deutsch:**

| Stimme | Kennzeichnung |
|---|---|
| Seraphina ★ | weiblich · mehrsprachig |
| Florian ★ | männlich · mehrsprachig |
| Katja | weiblich |
| Killian | männlich |
| Conrad | männlich |
| Amala | weiblich |

**Google-Chirp-3-HD-Stimmen (30), alle Deutsch.** Im Muster bitte alle 30 zeigen, damit Frank sieht, wie sich eine so lange Liste anfühlt.

*Weiblich (14):* Achernar · Aoede · Autonoe · Callirrhoe · Despina · Erinome · Gacrux · Kore · Laomedeia · Leda · Pulcherrima · Sulafat · Vindemiatrix · Zephyr

*Männlich (16):* Achird · Algenib · Algieba · Alnilam · Charon · Enceladus · Fenrir · Iapetus · Orus · Puck · Rasalgethi · Sadachbia · Sadaltager · Schedar · Umbriel · Zubenelgenubi

Vorausgewählt sind **Seraphina** (Edge) bzw. **Kore** (Chirp 3 HD).

---

### ⑤d Mit ChatGPT verbinden

Ein eigener, sehr aufgeräumter Bildschirm.

**Nicht verbunden:**
- Mittig ein großes goldenes Kreis-mit-Punkt-Symbol
- Darunter: „Perfect Moment braucht deinen ChatGPT-Zugang, um Fragen zu erzeugen." (Sans 400, 15 px, Text sekundär, zentriert, max. 30 Zeichen pro Zeile)
- Darunter ein großer Knopf „Verbinden"

**Code-Anzeige** (nach dem Antippen):
- Der Code erscheint groß und mittig: **`H4KP-9TRQ`** — Schreibmaschinenschrift, 40 px, Gold hell, Zeichenabstand 4 px, mit einem dezenten Trennstrich zwischen den beiden Vierergruppen
- Darunter: „Gib diesen Code auf der geöffneten Seite ein." und darunter kleiner die Adresse `auth.openai.com/codex/device`
- Darunter ein Knopf „Kopieren" und ein Knopf „Seite erneut öffnen"
- Ganz unten ein langsam kreisender goldener Bogen und „Warte auf Bestätigung…"
- Läuft der Code ab (15 Minuten), wird er ausgegraut durchgestrichen und ein Knopf „Neuen Code holen" erscheint

**Verbunden:**
- Grüner Punkt, die E-Mail-Adresse, darunter „Verbunden seit 12. Juli 2026"
- Ein zurückhaltender Knopf „Verbindung trennen" in Warnfarbe, nur als Umriss

---

## 5. Symbole

Durchgehend **Lucide** in Strichstärke 1,5 px. Konkret gebraucht:

`clock` (Verlauf) · `settings` (Einstellungen) · `mic` / `square` (Aufnahme) · `volume-2` / `volume-x` (Lautsprecher) · `plus` (Hinzufügen) · `check` (Häkchen) · `chevron-right` (weiter) · `chevron-left` (zurück) · `play` / `pause` · `trash-2` (Löschen) · `pencil` (Bearbeiten) · `grip-vertical` (Umsortieren) · `eye` / `eye-off` (Schlüssel aufdecken)

**App-Symbol:** ein Kreis in Gold primär auf fast schwarzem Grund, in dessen Mitte ein kleiner, hell leuchtender Punkt sitzt — der Nullpunkt. Um den Kreis ein sehr feiner, ausblendender Schein.

---

## 6. Bewegung im Überblick

| Was | Dauer | Kurve |
|---|---|---|
| Hintergrund-Atmen | 20 s (Sitzung: 30 s) | `ease-in-out`, endlos |
| Frage-Wechsel | 700 ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| Automatisches Scrollen der Liste | 800 ms | `ease-out` |
| Lautsprecher an/aus | 350 ms | `ease-out` |
| Bernstein-Puls am Lautsprecher | 2,4 s | `ease-in-out`, endlos |
| Aufnahme-Ringe | 1,6 s, dreifach versetzt | `ease-out`, endlos |
| Bildschirm-Dimmung | 4 s | `linear` |
| Aufwachen aus der Dimmung | 300 ms | `ease-out` |
| Sitzungsbildschirm herein | 700 ms | `cubic-bezier(0.2, 0, 0, 1)` |
| Blatt von unten | 400 ms | `cubic-bezier(0.2, 0, 0, 1)` |
| Karten-Auswahl | 400 ms | `ease-out` |

Bei aktiviertem systemweitem „Animationen reduzieren" entfallen Atmen und Pulsieren; Überblendungen bleiben, aber auf 200 ms verkürzt.

---

## 7. Was der Designer liefern soll

Eine einzelne, in sich geschlossene HTML-Datei, in der Frank sich durch alle Bildschirme klicken kann:

1. **Alle fünf Hauptbildschirme** plus die vier Unterseiten von ⑤, verlinkt und navigierbar
2. **Ein Umschalter Hell/Dunkel** an fest sichtbarer Stelle
3. **Der Sitzungsbildschirm als lauffähige Vorführung**: Fragen wechseln automatisch nach einer kurzen Beispielpause, Fortschrittsring läuft mit, Lautsprecher lässt sich umschalten (ohne echten Ton), Liste scrollt automatisch mit, Bildschirm-Dimmung nach 30 Sekunden vorführbar (gerne mit einem kleinen Knopf „Dimmung jetzt zeigen")
4. **Echte Beispielinhalte, kein Blindtext.** Die Fragen im Muster sollen so klingen wie das, was die KI wirklich liefert, zum Beispiel:
   - 🌱 Welche deiner heutigen Entscheidungen hat sich schon ganz leicht angefühlt?
   - 🌊 Woran merkst du gerade, dass dein Körper langsamer wird?
   - 🕯️ Welcher Gedanke von heute Morgen ist inzwischen still geworden?
   - 🧭 Was in deinem Tag zeigt dir, dass du in die richtige Richtung gehst?
   - 🌾 Welche Anspannung hat sich bereits von selbst gelöst, ohne dass du etwas tun musstest?
5. **Alle Zustände dokumentiert** — gesperrter Startknopf, leerer Verlauf, nicht verbunden, Code-Anzeige, Wartezustand ohne Netz, Nachschub-Punkte, Sitzungsende
6. Auf ein Telefonformat ausgelegt (**412 × 915 px**), gerne in einem angedeuteten Geräterahmen mittig auf der Seite

Nur Standard-HTML, CSS und etwas JavaScript in einer Datei. Schriften über Google Fonts, sonst keine externen Abhängigkeiten.

---

## 8. Woran Frank das Ergebnis misst

- Kann er es sich um 21 Uhr im dunklen Zimmer ansehen, ohne geblendet zu werden?
- Kommt er in **zwei Tipps** von „App auf" zu „Augen zu"?
- Sieht er beim kurzen Blinzeln in unter einer Sekunde, wo er ist?
- Fühlt sich die Bewegung nach **Atmen** an — oder nach Benutzeroberfläche?
- Ist das Gold **warm und ruhig** — oder wirkt es billig-glänzend? (Kein Metallic-Verlauf, keine Glanzlichter, kein Glitzern.)
