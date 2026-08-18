# UI-Spec — Gedankenspeicher
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

> **Stand: Absicht vor dem Design.** Alle gestalterischen Aussagen dieses Dokuments sind
> Vorgaben AN den Designer, nicht Bauanweisungen. Sobald der Entwurf zurück ist, gilt
> ausschliesslich die Messung in `Specs/Gedankenspeicher/v2/messung/`. Widerspricht ein Satz
> von hier der Messung, ist der Satz überholt — nicht die Messung falsch.

## 1. Gestalterische Grundhaltung

Gedankenspeicher ist ein Ort zum Denken, kein Werkzeugkasten. Der Verlauf ist ruhig, dunkel
und gut lesbar; die Notizen liegen als schwebende Karten darin, mit weichem Schatten und
einem leisen Verlauf, der ihnen Tiefe gibt, ohne vom Text abzulenken.

Die Tiefe steckt in den Rändern, nicht in der Mitte: Kopf- und Fußleiste sind milchiges Glas
mit Unschärfe, unter dem der Verlauf durchscheint und hindurchgleitet. Der Aufnahmeknopf ist
das einzige Bauteil, das leuchtet — er trägt eine weiche Aura in der Akzentfarbe und ist
damit auch mit einem Blick von weitem zu finden.

Die goldenen Erscheinungen sind warmes Altgold, nicht glänzendes Messing: gedeckt, matt,
wertig. Gold ist Akzent, niemals Fläche — es sitzt auf Rändern, Symbolen, dem Aufnahmering
und dem KI-Knopf, und trägt nie einen ganzen Hintergrund.

Vier Erscheinungen sind **gleichrangig**. Jede ist vollständig durchgestaltet, keine ist
eine abgedunkelte Variante einer anderen.

## 2. Erscheinungen (Themes)

### 2.1 Gold-Dunkel *(Vorbelegung)*

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#0D0B07` | Grundfläche aller Bildschirme |
| `hintergrundErhoben` | `#17140D` | Notizkarten |
| `hintergrundGlas` | `rgba(23, 20, 13, 0.72)` | Kopf- und Fußleiste, Blätter (mit 24 px Unschärfe) |
| `rand` | `rgba(201, 162, 39, 0.22)` | Kartenränder, Trennlinien |
| `akzent` | `#C9A227` | Aufnahmering, KI-Knopf, aktive Symbole, Häkchen |
| `akzentGedeckt` | `rgba(201, 162, 39, 0.14)` | Aura, Hervorhebung des gesprochenen Absatzes |
| `textStark` | `#F4EFE2` | Notiztext, KI-Antworten |
| `textMittel` | `#B8AE97` | Überschriften der Karten |
| `textSchwach` | `#77705F` | Zeitstempel, Fußzeilen |
| `fehler` | `#E0645C` | Fehlerzustände |
| `erfolg` | `#7FB069` | Bestätigungen |
| `kiKarte` | `#1C1710` | Hintergrund der KI-Antwortkarte |
| `kiKarteRand` | `rgba(201, 162, 39, 0.45)` | Rand der KI-Antwortkarte — sie hebt sich ab |

### 2.2 Gold-Hell

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#FBF7EE` | Grundfläche |
| `hintergrundErhoben` | `#FFFFFF` | Notizkarten |
| `hintergrundGlas` | `rgba(251, 247, 238, 0.78)` | Leisten, Blätter (24 px Unschärfe) |
| `rand` | `rgba(166, 124, 0, 0.20)` | Kartenränder, Trennlinien |
| `akzent` | `#A67C00` | Aufnahmering, KI-Knopf, aktive Symbole |
| `akzentGedeckt` | `rgba(166, 124, 0, 0.12)` | Aura, Absatz-Hervorhebung |
| `textStark` | `#231E14` | Notiztext |
| `textMittel` | `#5E5647` | Kartenüberschriften |
| `textSchwach` | `#938A76` | Zeitstempel |
| `fehler` | `#B3261E` | Fehlerzustände |
| `erfolg` | `#4C7A34` | Bestätigungen |
| `kiKarte` | `#FDF9F0` | KI-Antwortkarte |
| `kiKarteRand` | `rgba(166, 124, 0, 0.42)` | Rand der KI-Antwortkarte |

### 2.3 Dunkel *(neutral)*

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#0B0C0E` | Grundfläche |
| `hintergrundErhoben` | `#16181C` | Notizkarten |
| `hintergrundGlas` | `rgba(22, 24, 28, 0.72)` | Leisten, Blätter |
| `rand` | `rgba(255, 255, 255, 0.10)` | Ränder, Trennlinien |
| `akzent` | `#3B82F6` | Aufnahmering, KI-Knopf, aktive Symbole |
| `akzentGedeckt` | `rgba(59, 130, 246, 0.16)` | Aura, Absatz-Hervorhebung |
| `textStark` | `#ECEFF4` | Notiztext |
| `textMittel` | `#A8AFBA` | Kartenüberschriften |
| `textSchwach` | `#6B7280` | Zeitstempel |
| `fehler` | `#EF4444` | Fehlerzustände |
| `erfolg` | `#22C55E` | Bestätigungen |
| `kiKarte` | `#111820` | KI-Antwortkarte |
| `kiKarteRand` | `rgba(59, 130, 246, 0.45)` | Rand der KI-Antwortkarte |

### 2.4 Hell *(neutral)*

| Rolle | Wert | Verwendung |
|-------|------|-----------|
| `hintergrund` | `#F6F7F9` | Grundfläche |
| `hintergrundErhoben` | `#FFFFFF` | Notizkarten |
| `hintergrundGlas` | `rgba(246, 247, 249, 0.78)` | Leisten, Blätter |
| `rand` | `rgba(17, 24, 39, 0.10)` | Ränder, Trennlinien |
| `akzent` | `#2563EB` | Aufnahmering, KI-Knopf, aktive Symbole |
| `akzentGedeckt` | `rgba(37, 99, 235, 0.10)` | Aura, Absatz-Hervorhebung |
| `textStark` | `#111827` | Notiztext |
| `textMittel` | `#4B5563` | Kartenüberschriften |
| `textSchwach` | `#9CA3AF` | Zeitstempel |
| `fehler` | `#DC2626` | Fehlerzustände |
| `erfolg` | `#16A34A` | Bestätigungen |
| `kiKarte` | `#F8FAFF` | KI-Antwortkarte |
| `kiKarteRand` | `rgba(37, 99, 235, 0.40)` | Rand der KI-Antwortkarte |

## 3. Typografie

**Inter**, vier Schnitte: 400 Regular, 500 Medium, 600 SemiBold, 700 Bold. Als Schriftpaket
im Build, nicht vom System geladen.

| Rolle | Größe | Gewicht | Zeilenhöhe | Laufweite |
|-------|-------|---------|-----------|-----------|
| Bildschirmtitel | 22 sp | 600 | 28 sp | −0,2 sp |
| Karten-Überschrift | 15 sp | 600 | 20 sp | 0 |
| Notiztext | 16 sp | 400 | 25 sp | 0 |
| KI-Antworttext | 16 sp | 400 | 26 sp | 0 |
| Zeitstempel / Fußzeile | 12 sp | 500 | 16 sp | +0,3 sp |
| Knopfbeschriftung | 15 sp | 600 | 20 sp | +0,1 sp |
| Sitzungsname (Schublade) | 15 sp | 500 | 20 sp | 0 |
| Eingabefeld | 16 sp | 400 | 24 sp | 0 |
| Gerätecode (B-05) | 34 sp | 700 | 40 sp | +4 sp |
| Einstellungs-Beschriftung | 15 sp | 500 | 20 sp | 0 |
| Einstellungs-Erklärung | 13 sp | 400 | 18 sp | 0 |

## 4. Maße und Raster

Grundraster **4 dp**. Alle Abstände sind Vielfache davon.

| Maß | Wert |
|-----|------|
| Seitenrand Bildschirm | 16 dp |
| Abstand zwischen Notizkarten | 12 dp |
| Innenabstand Notizkarte | 16 dp |
| Höhe Kopfleiste | 56 dp |
| Höhe Fußleiste (Ruhe) | 72 dp |
| Höhe Fußleiste (Tastatur offen, Feld gewachsen) | bis 168 dp |
| Aufnahmeknopf | 60 dp Durchmesser |
| Aura um den Aufnahmeknopf | 80 dp Durchmesser |
| KI-Knopf | 48 dp Durchmesser |
| Kleine Knöpfe an der Karte (Lautsprecher, Verbessern) | 36 dp Tippfläche, 20 dp Symbol |
| Schublade Cover-Display | 280 dp breit |
| Schublade Innendisplay | 320 dp breit |
| Höhe Sitzungszeile | 56 dp |
| Blatt (B-03) Höhe | 60 % des Bildschirms, wächst bis 88 % |
| Mindest-Tippfläche überall | 44 × 44 dp |

## 5. Formen und Tiefe

| Bauteil | Radius | Rand | Schatten / Verlauf |
|---------|--------|------|-------------------|
| `.notizkarte` | 20 dp | 1 dp `rand` | Schatten: 0 dp Y 6 dp Blur 18 dp, `rgba(0,0,0,0.28)` dunkel / `rgba(0,0,0,0.08)` hell. Verlauf 145° von `hintergrundErhoben` nach `hintergrundErhoben` +4 % Helligkeit |
| `.kikarte` | 20 dp | 1,5 dp `kiKarteRand` | Schatten wie Notizkarte, zusätzlich Aura außen 24 dp `akzentGedeckt` |
| `header.kopfleiste (glass)` | 0 | unten 1 dp `rand` | `hintergrundGlas` + 24 px Rückwärts-Unschärfe |
| `footer.fussleiste (glass)` | oben 24 dp | oben 1 dp `rand` | `hintergrundGlas` + 24 px Rückwärts-Unschärfe, Schatten nach oben 0 −4 dp 16 dp `rgba(0,0,0,0.24)` |
| `.aufnahmeknopf` | vollrund | 2 dp `akzent` | Radialverlauf von `akzentGedeckt` (Mitte) nach durchsichtig (Rand); im Aufnahmezustand zusätzlich pulsierende Aura (M-05) |
| `.kiknopf` | vollrund | 1,5 dp `akzent` | flach in Ruhe; im Wartezustand wanderndes Leuchten |
| `.eingabefeld` | 22 dp | 1 dp `rand` | keiner |
| `aside.schublade` | rechts 24 dp | rechts 1 dp `rand` | Schatten nach rechts 8 dp 0 32 dp `rgba(0,0,0,0.40)` |
| `.blatt` (B-03) | oben 28 dp | oben 1 dp `rand` | `hintergrundGlas` + 32 px Unschärfe, Schatten 0 −8 dp 32 dp |
| `.profilzeile` | 14 dp | 1 dp `rand` | keiner; aktive Zeile bekommt 1,5 dp `akzent` |
| `.einstellungsgruppe` | 16 dp | 1 dp `rand` | keiner |

**Ausdrücklich gewünscht:** schwebende Karten, Glasleisten, leuchtender Aufnahmeknopf.
**Ausdrücklich nicht gewünscht:** Karten, die sich beim Scrollen perspektivisch neigen —
der Verlauf soll beim Lesen ruhig bleiben.

## 6. Bildschirme

| Kennung | Bildschirm | Zweck | Start? | Führt zu |
|---------|-----------|-------|--------|----------|
| B-01 | Verlauf | Notizen sehen, aufnehmen, tippen, auswerten | **ja** | B-02, B-03, B-04, B-07, B-08 |
| B-02 | Sitzungs-Schublade | Sitzungen wechseln und verwalten | nein | B-01 |
| B-03 | KI-Blatt | Rückfrage und Antwort vor der Auswertung | nein | B-01, B-05 |
| B-04 | Einstellungen | Schlüssel, Codex, Stimme, Erscheinung, Sicherung | nein | B-05, B-06, B-01 |
| B-05 | Codex-Anmeldung | Gerätecode eingeben | nein | B-04 |
| B-06 | Auswertungsprofile | Sechs Profile ansehen und bearbeiten | nein | B-04 |
| B-07 | Suche | Über alle Sitzungen suchen | nein | B-01 |
| B-08 | Notiz bearbeiten | Überschrift und Text ändern | nein | B-01 |

### B-01 — Verlauf *(Startbildschirm)*

**Aufbau von oben nach unten:**

1. **Kopfleiste** (Glas, 56 dp): links das Schubladensymbol (☰), mittig der Titel der offenen
   Sitzung (einzeilig, gekürzt), rechts Lupe (→ B-07) und Zahnrad (→ B-04).
2. **Verlaufsliste**, scrollbar, unten beginnend (neueste unten, wie ein Chat). Enthält
   zeitlich sortiert:
   - **Notizkarte** — oben eine Zeile mit Überschrift (`textMittel`, 600) links und
     Zeitstempel (`textSchwach`) rechts; darunter der Notiztext (`textStark`); unten rechts
     zwei kleine Knöpfe: **Lautsprecher** (F-06) und **Verbessern** bzw. nach einer
     Verbesserung **Rückgängig** (F-07). Langer Druck öffnet das Menü (F-08).
   - **KI-Antwortkarte** — breiter Rand in `akzent`, oben eine Zeile „Auswertung" mit
     Sprechblasensymbol; darunter die Rückfrage in `textMittel` kursiv und Franks Antwort in
     `textSchwach`; darunter der Antworttext in Absätzen mit 12 dp Abstand; unten eine
     Fußzeile mit Profil, Modell, Effort und Websuche-Zustand sowie dem Lautsprecher.
3. **Fußleiste** (Glas, 72 dp): links das **Textfeld** („Notiz tippen …", wächst bis 6
   Zeilen), rechts daneben der **KI-Knopf** (48 dp) und der **Aufnahmeknopf** (60 dp,
   leuchtend). Ist Text im Feld, verwandelt sich der Aufnahmeknopf in einen Senden-Knopf.

**Zustände:**

| Zustand | Darstellung |
|---------|-------------|
| **Leer** (neue Sitzung) | Mittig eine gedämpfte Zeichnung eines Mikrofons, darunter „Sprich einfach los." und kleiner „Alles, was dir zu diesem Thema einfällt — die KI fragst du später." |
| **Lädt** (Sitzungswechsel) | Drei Platzhalterkarten mit sanft wanderndem Schimmer |
| **Notiz transkribiert gerade** | Karte mit Zeitstempel, statt Text drei wandernde Punkte |
| **Notiz wartet auf Transkription** | Karte mit Zeitstempel und der Zeile „Wartet auf Netz" mit Wolkensymbol, in `textSchwach` |
| **Notiz fehlgeschlagen** | Karte mit `fehler`-Rand, Text „Transkription fehlgeschlagen" und Knopf „Nochmal versuchen" |
| **Nichts verstanden** | Karte mit Text „Nichts verstanden" in `textSchwach` und Knopf „Nochmal versuchen" |
| **Aufnahme läuft** | Aufnahmeknopf im Aufnahmezustand mit pulsierender Aura; Textfeld ausgegraut; über der Fußleiste eine Zeile mit der laufenden Dauer (mm:ss) |
| **Auswertung läuft** | KI-Knopf im Wartezustand; im Verlauf eine noch leere KI-Antwortkarte mit wanderndem Leuchten |
| **Vorlesen läuft** | Der betreffende Lautsprecher ist in `akzent` und zeigt ein Stopp-Symbol; der gerade gesprochene Absatz liegt auf `akzentGedeckt` |
| **Kein Mikrofonrecht** | Aufnahmeknopf ausgegraut; Antippen zeigt eine Meldung mit Knopf zu den Systemeinstellungen |

**Bedienelemente:**

| Element | Wirkung |
|---------|---------|
| ☰ Schubladensymbol | öffnet B-02 |
| Lupe | öffnet B-07 |
| Zahnrad | öffnet B-04 |
| Textfeld + Senden | F-02 |
| Aufnahmeknopf | F-01 |
| KI-Knopf | F-09 → öffnet B-03 |
| Lautsprecher an Karte | F-06 |
| Verbessern / Rückgängig an Karte | F-07 |
| Langer Druck auf Karte | Menü zu F-08 |
| Wischen von links am Rand | öffnet B-02 |

### B-02 — Sitzungs-Schublade

**Aufbau:** Kopf mit App-Namen „Gedankenspeicher" und darunter der Knopf **„+ Neue Sitzung"**
(volle Breite, Rand in `akzent`). Darunter die Sitzungsliste: je Zeile 56 dp mit Titel
(einzeilig gekürzt) und darunter in `textSchwach` die Zahl der Notizen und das Datum der
letzten. Die offene Sitzung trägt links einen 3 dp breiten Balken in `akzent` und liegt auf
`akzentGedeckt`. Ganz unten eine Trennlinie und der Eintrag „Einstellungen" mit Zahnrad.

**Zustände:** *Leer* gibt es nicht — es ist immer mindestens eine Sitzung da (F-12).
*Lädt:* fünf Platzhalterzeilen.

**Bedienelemente:** Tipp auf Sitzung → F-13 · Langer Druck → Menü mit *Umbenennen*, *Als
Markdown exportieren* (F-16), *Löschen* · „+ Neue Sitzung" → F-12 · „Einstellungen" → B-04 ·
Tipp auf die abgedunkelte Fläche rechts oder Wischen nach links → schließt.

### B-03 — KI-Blatt

Fährt von unten über B-01, das darunter abgedunkelt und leicht unscharf bleibt.

**Aufbau von oben nach unten:**

1. Ziehgriff (32 × 4 dp, `rand`, mittig).
2. Titel „Auswertung" und darunter in `textSchwach`: „N Notizen seit der letzten Auswertung".
3. Zwei Schalterzeilen: **„Ganze Sitzung einbeziehen"** (aus) und **„Websuche"**
   (vorbelegt aus der Grundeinstellung; steht sie auf *KI entscheidet*, zeigt die Zeile drei
   Wahlfelder statt eines Schalters).
4. Zeile „Profil: <Name des aktiven Profils>", antippbar → B-06.
5. **Die Rückfrage der KI** in `textStark`, 17 sp, 500 — der optisch wichtigste Text des
   Blattes.
6. **Antwortfeld** für Frank, mit Mikrofonknopf rechts darin (spricht die Antwort ein).
7. Knopf **„Auswerten"** (volle Breite, gefüllt in `akzent`, Text auf `hintergrund`).

**Zustände:** *Frage wird geholt* — an Stelle der Rückfrage drei wandernde Punkte, das
Antwortfeld ist gesperrt · *Frage da* — wie oben · *Codex nicht verbunden* — statt der Frage
der Hinweis „Codex ist nicht verbunden" und der Knopf „Jetzt verbinden" (→ B-05) · *Kein
Netz* — Hinweis mit Wiederholen-Knopf · *Antwort wird eingesprochen* — der Mikrofonknopf
pulsiert.

### B-04 — Einstellungen

Gruppen mit je 16 dp Radius, in dieser Reihenfolge:

1. **Erscheinung** — vier Kacheln nebeneinander (je 1:1), jede zeigt eine Miniatur ihrer
   Farbwelt; die gewählte trägt einen 2 dp Rand in `akzent` und ein Häkchen. (F-15)
2. **Codex** — Zeile „Verbindung" mit Zustand (*verbunden als …* / *nicht verbunden*) und
   Knopf *Verbinden* (→ B-05) bzw. *Trennen* · Zeile „Modell" mit drei Wahlfeldern (Sol,
   Terra, Luna) · Zeile „Effort" mit vier Wahlfeldern (minimal, niedrig, mittel, hoch) ·
   Zeile „Websuche" mit drei Wahlfeldern (aus, immer, KI entscheidet). (F-11)
3. **Auswertungsprofile** — eine Zeile mit dem Namen des aktiven Profils und einem Pfeil
   (→ B-06). (F-10)
4. **Transkription** — Feld für den Groq-Schlüssel (verdeckt, mit Augensymbol zum Anzeigen),
   darunter in `textSchwach` „Modell: whisper-large-v3-turbo" als reine Anzeige. (F-03)
5. **Stimme** — Zeile „Dienst" mit vier Wahlfeldern; darunter, je nach Wahl, die Stimmliste
   und die nötigen Schlüsselfelder; Knopf **„Probe hören"**. (F-18)
6. **Sicherung** — Schalter „Nach Google Drive sichern", darunter Zeitpunkt und Größe der
   letzten Sicherung, Knopf „Jetzt sichern" und Knopf „Aus Sicherung wiederherstellen"
   (in `fehler` gerandet). (F-17)
7. **Über** — Versionsnummer und Zeitpunkt des Bumps, als reine Anzeige.

**Zustände:** *Schlüssel fehlt* — das betroffene Feld trägt einen `fehler`-Rand und darunter
steht, wofür er gebraucht wird · *Probe läuft* — der Knopf zeigt ein Stopp-Symbol.

### B-05 — Codex-Anmeldung

Mittig auf leerer Fläche: die Überschrift „Codex verbinden", darunter der **Gerätecode** in
34 sp Bold als zwei Blöcke — **vier Zeichen, Trennstrich, fünf Zeichen** — mit großzügiger
Laufweite. Darunter die Adresse zum Öffnen, dann zwei Knöpfe: **„Im Browser öffnen"**
(gefüllt) und **„Code kopieren"** (nur gerandet). Ganz unten eine Zeile mit einem kleinen
Kreisel und dem Text „Warte auf Bestätigung …".

**Zustände:** *Code wird geholt* — an Stelle des Codes ein Platzhalter mit Schimmer ·
*Wartend* — wie oben · *Abgelaufen* — der Code ist durchgestrichen, darunter Knopf „Neuen
Code holen" · *Erfolg* — Häkchen in `erfolg`, „Verbunden", der Bildschirm schließt sich nach
1,2 s von selbst.

### B-06 — Auswertungsprofile

Sechs Zeilen untereinander, je 14 dp Radius, 12 dp Abstand. Eine Zeile enthält links ein
rundes Häkchenfeld (24 dp), daneben den Profilnamen (600) und darunter zweizeilig gekürzt
den Anweisungstext in `textSchwach`; rechts ein Stiftsymbol.

Die aktive Zeile trägt einen 1,5 dp Rand in `akzent` und liegt auf `akzentGedeckt`.

Ein Tipp auf den Stift (oder auf die Zeile) öffnet den **Editor** als Blatt: Feld
„Name" (einzeilig) und Feld „Anweisung an die KI" (mehrzeilig, wächst), darunter die Knöpfe
„Zurücksetzen" (nur gerandet) und „Speichern" (gefüllt).

**Zustände:** *Leeres Profil* — Name in `textSchwach` („Eigenes Profil 1"), statt der
Textvorschau steht „Noch kein Text — antippen zum Ausfüllen", das Häkchenfeld ist ausgegraut.

### B-07 — Suche

Kopfleiste mit Zurückpfeil und einem sofort fokussierten Suchfeld. Darunter die Treffer,
nach Sitzung gruppiert: je Gruppe eine Kopfzeile mit Sitzungstitel und Trefferzahl, darunter
die Treffer als flache Zeilen mit Überschrift, Zeitstempel und der Textstelle, in der das
Suchwort in `akzent` unterlegt ist.

**Zustände:** *Noch nichts eingegeben* — „Suche in allen Notizen" mittig in `textSchwach` ·
*Kein Treffer* — „Nichts gefunden zu ‚<Suchwort>'" · *Sucht* — schmaler Fortschrittsbalken
unter dem Suchfeld.

### B-08 — Notiz bearbeiten

Kopfleiste mit Abbrechen (links) und Speichern (rechts, in `akzent`). Darunter das Feld
**Überschrift** (einzeilig) und das Feld **Text** (füllt den Rest, scrollt). Unter dem
Textfeld in `textSchwach` der unveränderliche Zeitstempel der Notiz.

**Zustände:** *Unverändert* — Speichern ist ausgegraut · *Geändert* — Speichern ist aktiv;
Zurück fragt „Änderungen verwerfen?".

## 7. Ikonografie und Bilder

**Material Symbols Rounded**, Strichstärke 2 dp, Größe 24 dp (an Karten 20 dp). Keine
gefüllten Varianten außer beim aktiven Zustand des Lautsprechers und beim Häkchen.

| Zweck | Symbol |
|-------|--------|
| Schublade | `menu` |
| Suche | `search` |
| Einstellungen | `settings` |
| Aufnehmen | `mic` |
| Aufnahme beenden | `stop` |
| Senden | `arrow_upward` |
| KI-Auswertung | `auto_awesome` |
| Vorlesen | `volume_up` (aktiv: `stop_circle`) |
| Verbessern | `auto_fix_high` |
| Rückgängig | `undo` |
| Neue Sitzung | `add` |
| Exportieren | `ios_share` |
| Löschen | `delete` |
| Verschieben | `drive_file_move` |
| Kopieren | `content_copy` |
| Wartet auf Netz | `cloud_off` |
| Bearbeiten | `edit` |

Keine Fotos, keine Illustrationen außer der gedämpften Mikrofon-Zeichnung im Leerzustand
von B-01.

## 8. Texte

Alle festen Beschriftungen wörtlich:

| Ort | Text |
|-----|------|
| B-01 Textfeld | „Notiz tippen …" |
| B-01 Leerzustand Titel | „Sprich einfach los." |
| B-01 Leerzustand Erklärung | „Alles, was dir zu diesem Thema einfällt — die KI fragst du später." |
| B-01 Notiz wartet | „Wartet auf Netz" |
| B-01 Notiz Fehler | „Transkription fehlgeschlagen" / Knopf „Nochmal versuchen" |
| B-01 Nichts verstanden | „Nichts verstanden" |
| B-01 kein Mikrofonrecht | „Ohne Mikrofon kann ich dich nicht hören." / Knopf „Einstellungen öffnen" |
| B-02 Kopf | „Gedankenspeicher" |
| B-02 Knopf | „+ Neue Sitzung" |
| B-02 Standardtitel | „Neue Sitzung" |
| B-02 Löschen-Rückfrage | „Sitzung mit {n} Notizen löschen? Das lässt sich nicht rückgängig machen." |
| B-03 Titel | „Auswertung" |
| B-03 Zähler | „{n} Notizen seit der letzten Auswertung" |
| B-03 Keine neuen | „Seit der letzten Auswertung sind keine neuen Notizen dazugekommen." |
| B-03 Schalter 1 | „Ganze Sitzung einbeziehen" |
| B-03 Schalter 2 | „Websuche" |
| B-03 Profilzeile | „Profil: {name}" |
| B-03 Antwortfeld | „Deine Antwort …" |
| B-03 Knopf | „Auswerten" |
| B-03 Codex fehlt | „Codex ist nicht verbunden." / Knopf „Jetzt verbinden" |
| B-04 Gruppen | „Erscheinung" · „Codex" · „Auswertungsprofile" · „Transkription" · „Stimme" · „Sicherung" · „Über" |
| B-04 Erscheinungen | „Hell" · „Dunkel" · „Gold-Hell" · „Gold-Dunkel" |
| B-04 Effort | „minimal" · „niedrig" · „mittel" · „hoch" |
| B-04 Websuche | „aus" · „immer" · „KI entscheidet" |
| B-04 Probe | „Probe hören" |
| B-05 Titel | „Codex verbinden" |
| B-05 Warten | „Warte auf Bestätigung …" |
| B-05 Knöpfe | „Im Browser öffnen" · „Code kopieren" · „Neuen Code holen" |
| B-06 Leeres Profil | „Noch kein Text — antippen zum Ausfüllen" |
| B-06 Editor | „Name" · „Anweisung an die KI" · „Zurücksetzen" · „Speichern" |
| B-07 Platzhalter | „Suche in allen Notizen" |
| B-07 Kein Treffer | „Nichts gefunden zu ‚{wort}'" |
| B-08 Knöpfe | „Abbrechen" · „Speichern" |
| B-08 Verwerfen | „Änderungen verwerfen?" |

Platzhalter stehen in geschweiften Klammern: `{n}`, `{name}`, `{wort}`.

## 9. Barrierefreiheit

- Mindest-Tippfläche **44 × 44 dp** überall, auch bei den 20 dp großen Kartensymbolen.
- Kontrast Text auf Hintergrund mindestens **4,5 : 1** in allen vier Erscheinungen; die
  angegebenen Werte erfüllen das (Gold-Dunkel: `#F4EFE2` auf `#17140D` ≈ 15,8 : 1;
  Gold-Hell: `#231E14` auf `#FFFFFF` ≈ 15,1 : 1).
- Der Akzent trägt **nie allein** eine Bedeutung: das aktive Profil hat Häkchen *und* Rand,
  die offene Sitzung Balken *und* Fläche.
- Bei großer Systemschrift wachsen alle Texte mit; Karten und Leisten wachsen in der Höhe
  mit, nichts wird abgeschnitten. Ab 130 % Schriftgröße bricht die Fußzeile der KI-Karte in
  zwei Zeilen um.
- Jedes Symbol ohne Beschriftung trägt eine Inhaltsbeschreibung für den Screenreader.

## 10. Offene Fragen

Siehe `00-PROJEKT.md` §6. Im Gestaltungsbereich ist nichts offen — alles Weitere entscheidet
der Designer, und seine Messung gilt.
