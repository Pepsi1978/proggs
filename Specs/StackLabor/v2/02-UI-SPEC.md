# UI-Spec — StackLabor
Stand: 14.08.2026, 14:25 · Stufe: v2 · Plattform(en): Android

> **Alle Werte in diesem Dokument sind im Entwurf GEMESSEN, nicht beschrieben.** Die verbindliche
> Quelle ist `Specs/StackLabor/v2/messung/<erscheinung>/<B-xx>.json` (30 Dateien: 15 Bildschirme
> × 2 Erscheinungen). Dieses Dokument macht sie lesbar, ersetzt sie aber nicht. Steht ein Wert in
> der Messung und nicht hier, gilt die Messung.
>
> **Umrechnung:** Die `stil`-Werte der Messung sind **direkt dp/sp** (`15px` → `15.sp`,
> `12px` → `12.dp`). Die `kasten`-Werte sind **doppelt** (Darstellungsmaßstab 2×) — für dp
> halbieren. Nachgeprüft am Gerätebereich (594 × 938 → 297 × 469 dp) und an der Mittel-Karte
> (546 × 112 bei `height: 56px` → 273 × 56 dp).

## 1. Gestalterische Grundhaltung

StackLabor ist ein Laborinstrument: hell, klar, dicht. Der Entwurf setzt die Absicht aus v1 um,
ohne sie zu überschreiben — er ergänzt sie um Tiefe. Karten liegen auf einem kühlen Grund und
tragen einen weichen Schatten; der Kopfbereich trägt einen langsam wandernden Farbverlauf, der
das einzige dauerhaft bewegte Element im Bild ist. Die Ampel sitzt als schmaler Farbbalken an der
linken Kante jeder Zeile und ist damit beim Überfliegen der Liste sofort ablesbar, ohne mit den
Löslichkeitspunkten verwechselt zu werden.

## 2. Erscheinungen

Beide vollständig und gleichwertig. Umgeschaltet über das Sonne/Mond-Symbol im Kopf von B-01
(F-22, Übergang 420 ms). Farbwerte wie im Entwurf gemessen.

### 2.1 Hell — Standard

| Rolle | Wert | Verwendung |
|---|---|---|
| `--grund` | `#F5F7FA` | Bildschirmhintergrund, Gerätefläche |
| `--flaeche` | `#FFFFFF` | Karten, Blätter, Listenzeilen |
| `--erh` | `#F1F5F9` | Kopfbereich, Sockel, Chips |
| `--rand` | `#E2E8F0` | Kartenkontur 1 dp, Trenner |
| `--txt` | `#0F172A` | Namen, Titel, Zieltexte |
| `--txt2` | `#64748B` | zweite Zeile, Zeitpunkte, Metazeilen |
| `--akz` | `#4F46E5` | Auswerten-Knopf, aktives Häkchen, aktiver Chip, Plus-Knopf |
| `--akz2` | `#0EA5E9` | zweiter Stop des Kopfverlaufs |
| `--gruen` | `#047857` | Ampel grün |
| `--gelb` | `#D97706` | Ampel gelb (Fläche) |
| `--gelbT` | `#B45309` | Ampel gelb (Text) |
| `--rot` | `#DC2626` | Ampel rot |
| `--rotK` | `#B91C1C` | Ampel rot, kräftig (Fehlerüberschrift) |
| `--grau` | `#94A3B8` | Ampel grau („nicht bedient") |
| `--lwas` | `#059669` | Löslichkeitspunkt wasserlöslich, gefüllt |
| `--lfetf` | `#FFFFFF` | Löslichkeitspunkt fettlöslich, Füllung |
| `--lfetr` | `#64748B` | Löslichkeitspunkt fettlöslich, **Rand** — macht ihn auf Hell sichtbar |
| `--deakt` | `#CBD5E1` | ausgegrauter Eintrag |
| `--sch` | `rgba(15,23,42,.10)` | Kartenschatten |
| `--sch2` | `rgba(15,23,42,.22)` | Plus-Knopf, Blätter |
| `--glas` | `rgba(241,245,249,.72)` | Glasflächen (Kopf, Sockel) |

### 2.2 Dunkel

| Rolle | Wert | Rolle | Wert |
|---|---|---|---|
| `--grund` | `#0B0E14` | `--gruen` | `#34D399` |
| `--flaeche` | `#141A24` | `--gelb` / `--gelbT` | `#FBBF24` |
| `--erh` | `#1B2330` | `--rot` / `--rotK` | `#F87171` |
| `--rand` | `#243040` | `--grau` | `#64748B` |
| `--txt` | `#E6EAF2` | `--lwas` | `#34D399` |
| `--txt2` | `#9AA6B8` | `--lfetf` | `#FFFFFF` |
| `--akz` | `#22D3EE` | `--lfetr` | `transparent` (auf dunkel selbsttragend) |
| `--akz2` | `#0EA5E9` | `--deakt` | `#334155` |
| `--sch` | `rgba(0,0,0,.45)` | `--sch2` | `rgba(0,0,0,.6)` |
| `--glas` | `rgba(27,35,48,.72)` | | |

## 3. Typografie

**Inter** (system-ui als Rückfall), Gewichte **400 · 500 · 600**.
Symbole: **Material Symbols Rounded**, Gewicht 400.
Systemschrift des Geräts steht auf 90 % — die Werte sind darauf gerechnet.

| Rolle | Größe | Gewicht | Vorkommen (gemessen) |
|---|---|---|---|
| Grundtext / Listentext | 14 sp | 400 / 500 | häufigste Größe |
| Mittel-Name, Fließtext | 15 sp | 500 | Zeile 1 des Eintrags, Auswertungstext |
| Stack-Name auf der Karte | 16 sp | 600 | B-01 |
| Kopfzeile eines Bildschirms | 17 sp | 600 | B-02 … B-15 |
| Zweite Zeile, Metazeile | 12 sp | 400 | Dosis, Frequenz, Zeitstempel |
| Kleinschrift, Statusleiste | 11 sp | 400 / 600 | Zählpunkte, Uhrzeit |
| Bildschirmtitel | 20–22 sp | 600 | B-01 „StackLabor" |
| Symbolgrößen | 18 · 20 · 26 sp | 400 | Kopfsymbole, Plus |
| Geräte-Code (B-11) | 40 sp | 600 | Anmeldecode |

## 4. Maße und Raster

> **⚠ Korrektur vom 14.08.2026, nach dem Bau auf dem Gerät nachgemessen.**
>
> Der Entwurfskopf nennt „Cover-Display 297 × 469 dp @ 420 dpi". Diese dp-Angabe ist
> **rechnerisch falsch** und wurde beim Schreiben dieses Spec ungeprüft übernommen.
> Nachgemessen auf dem Gerät (`wm size`, `wm density`, `dumpsys display`):
>
> | | Pixel | Dichte | reale dp |
> |---|---|---|---|
> | Cover (zugeklappt) | 1248 × 1972 | 2,625 | **475 × 751 dp** |
> | Innen (aufgeklappt) | 2448 × 1848 | 2,625 | **932 × 704 dp** |
>
> **Alle Maßangaben in diesem Paket bleiben trotzdem gültig** — sie sind im Entwurf
> gemessen und beziehen sich auf dessen Bezugsbreite von 297 dp. Damit sie auf dem Gerät
> genauso ankommen, rechnet die App in der Dichte des Entwurfs
> (`ui/theme/Massstab.kt`: die Dichte wird so gesetzt, dass die Bildschirmbreite exakt der
> Entwurfsbreite entspricht). Ohne diese Angleichung landet jeder Wert auf einem
> 1,6-mal breiteren Bildschirm: dasselbe Layout in falscher Größe — flache Karten,
> zu kleine Schrift, zu weite Abstände.


Grundraster 4 dp. Gerät 297 × 469 dp, Eckenradius des Geräts 24 dp.

| Maß | Wert (gemessen) |
|---|---|
| Statusleiste | 297 × 24 dp, Innenabstand 0/14 dp |
| Inhaltsbereich | 297 × 421 dp @ (0, 24) |
| Gestenleiste | 297 × 24 dp @ (0, 445), Griff 96 × 4 dp, Radius 2 dp |
| Kopfbereich B-01 | 297 × 97 dp, Fläche `--erh`, darüber Verlaufsschicht 297 × 96 dp |
| Titelzeile B-01 | 297 × 52 dp, Innenabstand 6/12/0/16 dp |
| Dosis-Zeile B-01 | 297 × 26 dp @ (0, 76), Abstand 6 dp, Innenabstand 0/16 dp |
| Leiste „Alle Stacks" | 297 × 49 dp @ (0, 121), Fläche `--flaeche`, Abstand 8 dp, Innenabstand 0/16 dp |
| Kartenbereich B-01 | 297 × 275 dp @ (0, 170), Abstand 8 dp, Innenabstand 8/12/76 dp |
| **Stack-Karte** | **273 × 78 dp**, Radius 12 dp, Rand 1 dp `--rand`, Schatten `0 2 6 rgba(15,23,42,.10)` |
| **Plus-Knopf** | **57 × 57 dp** @ (225, 373), Radius 28 dp, Fläche `--akz`, Schatten `0 6 16 rgba(15,23,42,.22)`, Symbol 26 sp |
| Kopfleiste B-02 … B-15 | 297 × 56 dp |
| Ziel-Streifen B-02 | 297 × 40 dp |
| Sortier-/Suchleiste B-02 | 297 × 36 dp |
| Auswerten-Sockel B-02 | 297 × 52 dp, fest am unteren Rand |
| **Listenhöhe B-02** | **237 dp** → 4 Mittel-Einträge sichtbar |
| **Mittel-Eintrag** | **273 × 56 dp**, Radius 12 dp, + 1 dp Trenner = 57 dp Takt |
| **Ziel-Eintrag** | **273 × 40 dp** |
| Mindest-Tippfläche | 44 × 44 dp |

## 5. Formen und Tiefe

| Bauteil | Radius | Rand | Tiefe |
|---|---|---|---|
| Gerät | 24 dp | — | — |
| Karte (Stack, Mittel) | 12 dp | 1 dp `--rand` | `0 2px 6px rgba(15,23,42,.10)` |
| Plus-Knopf | 28 dp (vollrund) | — | `0 6px 16px rgba(15,23,42,.22)` |
| Blatt | 22–24 dp oben | — | `--sch2` + Abdunklung |
| Chip (Sortierung) | vollrund (999 dp) | 1 dp bei inaktiv | — |
| Ampel-Kantenbalken | `2px 0 0 2px` (links gerundet) | — | bei Rot zusätzlich Aura (M-21) |
| Löslichkeits-Punkt | 50 % (vollrund) | 1,5 dp `--lfetr` **nur beim fettlöslichen** | — |
| Nummernkreis (Ziel) | 50 % | 1 dp | — |
| Eingabefeld | 10–12 dp | 1 dp, im Fehlerfall `--rot` | — |
| Kleinteile (Zählpunkte) | 50 %, 6 dp Ø | — | — |

**Verläufe (gemessen):**
- Kopfbereich B-01: `linear-gradient(110deg, #4F46E5, #0EA5E9, #4F46E5)`, wandert (M-16)
- Glanzkante an Karten: `linear-gradient(90deg, transparent, rgba(255,255,255,.55), transparent)`
- Verlaufskante über dem Sockel: `linear-gradient(rgba(0,0,0,0), rgba(15,23,42,.1))`

**Glasflächen:** `--glas` mit Weichzeichner — ausschließlich auf **festen** Flächen
(Kopfleiste, Auswerten-Sockel, Blätter), **nie** über der scrollenden Liste.

## 6. Bildschirme

| Kennung | Bildschirm | Start? | führt zu | Messung |
|---|---|---|---|---|
| B-01 | Hauptbildschirm | **ja** | B-02, B-03, B-09, B-10, B-13, B-14 | 211 Elemente |
| B-02 | Stack-Detail | nein | B-04, B-05, B-06, B-07, B-08, B-14, B-15 | 359 / 438 |
| B-03 | Ziel-Katalog | nein | — | 142 |
| B-04 | Ziele dieses Stacks (Überlagerung) | nein | B-03, B-06, B-12 | 438 |
| B-05 | Mittel bearbeiten (Blatt) | nein | — | 509 |
| B-06 | Aufschlüsselung (Blatt) | nein | — | 477 |
| B-07 | Auswertung im Vollbild | nein | — | 44 |
| B-08 | Eigene Fragen (Blatt) | nein | — | 460 |
| B-09 | Alle Stacks zusammen | nein | B-07 | 519 |
| B-10 | Einstellungen | nein | B-11 | 137 |
| B-11 | Codex-Anmeldung | nein | — | 29 |
| B-12 | Ziele ordnen (Vollbild) | nein | — | 67 |
| B-13 | Stack bearbeiten (Blatt) | nein | — | 238 |
| B-14 | Mittel-Katalog | nein | B-05 | 513 |
| B-15 | Auswertungs-Historie (Blatt) | nein | B-07 | 505 |

**Der Aufbau jedes Bildschirms steht vollständig in der Messung.** Nachfolgend die tragende
Struktur; jedes Maß ist dort nachprüfbar.

### B-01 — Hauptbildschirm

Von oben nach unten:
1. **Statusleiste** 24 dp — Uhrzeit links (11 sp/600), rechts `signal_cellular_alt` und `battery_full` je 13 sp, Abstand 4 dp.
2. **Kopfbereich** 97 dp, Fläche `--erh`. Darüber eine absolut liegende Verlaufsschicht 96 dp mit `linear-gradient(110deg, --akz, --akz2, --akz)`, die wandert (M-16).
   - **Titelzeile** 52 dp: „StackLabor" 20–22 sp/600 links; rechts drei Symbolknöpfe 44 × 44 dp — `dark_mode` (F-22), `track_changes` → B-03, `settings` → B-10.
   - **Dosis-Zeile** 26 dp: Beschriftung „Dosis", danach zwei Chips **Frei** / **Dienst** (F-27); rechts der Klartext des betroffenen Mittels, z. B. „Venlafaxin 50 mg".
3. **Leiste „Alle Stacks zusammen prüfen"** 49 dp, Fläche `--flaeche`: Symbol `auto_awesome` 20 sp in `--akz`, Text 14 sp/500, rechts der Zeitstempel 11 sp in `--txt2`, dann `chevron_right` 18 sp. Führt zu B-09.
4. **Stack-Karten** — Bereich 275 dp, Abstand 8 dp, Innenabstand 8/12 dp, unten 76 dp frei für den Plus-Knopf. Je Karte 273 × 78 dp:
   - 3 dp **Ampel-Kantenbalken** links über die volle Höhe (Sammelampel).
   - Stack-Name 16 sp/600.
   - Zeitpunkt und Einnahme-Hinweis 12 sp `--txt2`, einzeilig mit Auslassungszeichen.
   - Vier **Zählpunkte** 6 dp Ø mit Zahl: grün, gelb, rot, grau.
   - Rechts „16 Mittel".
   - Bei veralteter Bewertung: gestrichelte Umrandung + Marke **„veraltet"** in `--gelbT`.
   - Einblendung gestaffelt (M-22).
5. **Plus-Knopf** 57 dp, schwebend rechts unten, Symbol `add` 26 sp weiß, atmet (M-16).
6. **Gestenleiste** 24 dp mit Griff 96 × 4 dp.

**Zustände:** leer · nie ausgewertet (Balken grau) · veraltet (gestrichelt) · Codex nicht angemeldet · offline.

### B-02 — Stack-Detail

1. **Kopfleiste** 56 dp: `arrow_back` 44 dp · Stack-Name 17 sp/600, darunter „Zeitpunkt · Hinweis" 12 sp einzeilig ellipsiert · `more_vert` 44 dp (Stack bearbeiten → B-13, Eigene Fragen → B-08, Historie → B-15).
2. **Ziel-Streifen** 40 dp, Fläche `--flaeche`, links 3 dp Ampelbalken (schlechteste Ziel-Ampel): „Ziele 6" 15 sp, vier Zählpunkte, rechts `expand_more`. Tippen öffnet B-04 als **Überlagerung** über der Liste (verdrängt sie nicht).
3. **Sortier- und Suchleiste** 36 dp: Chips „Löslichkeit" (aktiv: Fläche `--akz`, Text weiß) und „Einnahme" (inaktiv: Rand 1 dp), rechts `search` — klappt die Suchzeile aus.
4. **Mittel-Liste** 237 dp, Takt 57 dp → 4 Einträge sichtbar. Aufbau je Eintrag siehe §6a.
5. **Auswerten-Sockel** 52 dp, fest: Knopf „Diesen Stack auswerten" mit `auto_awesome`, Fläche `--akz`, Radius 12 dp; rechts daneben ein Plus-Knopf für F-02. Darüber eine 4 dp hohe Verlaufskante.

**Zustände:** leer · lädt (Schimmer) · Auswertung läuft (Sockel wird Fortschritt + Abbrechen; Ampeln entsättigt pulsierend) · veraltet (Sockel `--gelbT`) · Codex-Fehler (Karte über dem Sockel) · offline · offener Hinweis aus F-02 (Schnipsel über dem Sockel mit „Behalten" / „Doch entfernen") · Rückgängig-Leiste nach dem Wischen (44 dp, Fläche `--txt`, Text `--grund`).

### B-03 — Ziel-Katalog
Kopfleiste 56 dp · Suchzeile · Zeilen mit Zieltext 15 sp und „in N Stacks verwendet" 12 sp, rechts Stift · schwebender Plus-Knopf. Löschwarnung nennt die betroffenen Stacks.

### B-04 — Ziele dieses Stacks (Überlagerung)
Legt sich über die Liste von B-02, höchstens 281 dp. Kästchen 22 dp, Nummer, Zieltext 14 sp, Ampel als Kantenbalken. Tippen auf die Zeile klappt die **Begründung** als eigene Zeile auf (nicht als Sprechblase). Tippen auf die Ampel öffnet B-06. Fußzeile mit **„Ordnen"** (→ B-12) und **„Fertig"**.

### B-05 — Mittel bearbeiten (Blatt)
Zwei getrennte Bereiche: **Stammdaten** (Name, Löslichkeit als drei Chips, Darreichungsform, Hersteller, Durchfallrisiko-Schalter, Beistoffe) mit dem Hinweis „Gilt in N Stacks" — und **In diesem Stack** (Stückzahl × Menge + Einheit mit Live-Vorschau „2 × 80 mg = 160 mg", zweite Dosis-Variante, Frequenz, „alterniert mit", Kombi-Gruppe, Zusatztext für die KI). Fester Sockel „Sichern". Leere Menge sperrt Sichern und rahmt das Feld in `--rot`.

### B-06 — Aufschlüsselung (Blatt)
Kopf nennt den Gegenstand (Mittel **oder** Ziel). Je Gegenseite ein Block: Nummer, Text, Urteilsmarke *stützt / neutral / stört*, Begründung zwei Zeilen 12 sp. Schalter „Auch neutrale zeigen". Beim Öffnen leuchten Gegenstand und betroffene Zeilen (M-10).

### B-07 — Auswertung im Vollbild
Kopfleiste · Metazeile „ausgewertet 14.08. 07:44 · Terra · hoch" 12 sp · Fließtext 15 sp mit 22 dp Zeilenhöhe · fester Sockel mit Vorlesen/Pause/Stopp und drei Pegelbalken (M-15). Wird auch von B-09 aus aufgerufen.

### B-08 — Eigene Fragen (Blatt)
Zeilen mit dem Fragetext (bis zwei Zeilen), Wischen löscht, schwebender Plus-Knopf. Leerzustand: „Ohne eigene Fragen antwortet die Auswertung allgemein."

### B-09 — Alle Stacks zusammen
Abschnitt **„Tagesgesamtdosis"**: je Wirkstoff eine Zeile mit Summe rechtsbündig, darunter klein die beteiligten Stacks; auffällige Mengen tragen einen 3 dp Balken. Abschnitt **„Konkurrenzen über Stacks hinweg"** als Karten. Fester Sockel „Alles prüfen". Vollständiger Zustandssatz wie B-02.

### B-10 — Einstellungen
Vier Rubriken mit Überschrift und Zeilen: **Vorlesen** (Anbieter, Stimme, Tempo, Pause, Abschaltung, Verbrauch) · **Codex** (Konto, Modell, Denkstufe) · **Daten** (Exportieren, Importieren, Startbestand einlesen, Letzte Sicherung wiederherstellen) · **Darstellung** (Hell/Dunkel, Bewegung reduzieren).

### B-11 — Codex-Anmeldung
Code 40 sp/600 mittig in einem hohen Feld · Adresse `auth.openai.com/device` 14 sp in `--akz` · Knopf „Seite öffnen" · Wartezeile mit Pulsring (M-21/`ring`, 1,6 s).

### B-12 — Ziele ordnen (Vollbild)
Kopfleiste „Ziele ordnen — <Stack>" · Liste über die volle verbleibende Höhe, kein Sockel. Je Zeile: durchgehender Ampel-Kantenbalken links, Nummernkreis 20 dp Ø mit Zahl 11 sp, Zieltext 14 sp, Ziehgriff `drag_indicator` rechts in 44 dp Tippfläche. Hier und **nur** hier findet das Ziehen der Ziele statt (M-01 … M-06).

### B-13 — Stack bearbeiten (Blatt)
Name, Zeitpunkt, Einnahme-Hinweis, Sockel „Sichern"; beim Bearbeiten zusätzlich „Stack löschen" in `--rot` mit Nennung der Anzahl enthaltener Mittel.

### B-14 — Mittel-Katalog
Suchzeile · Zeilen mit Name 15 sp samt Löslichkeitspunkten und „in 3 Stacks · Kapsel · Thorne" 12 sp · Plus-Knopf · Überlaufmenü „Zusammenführen" (zwei Einträge markieren, einer bleibt). Ohne Treffer: „Neu anlegen".

### B-15 — Auswertungs-Historie (Blatt)
Fünf Zeilen mit Zeitpunkt, Modell und Kurzbilanz „4 grün · 1 gelb · 0 rot"; zwei auswählbar → Vergleich als Liste der Unterschiede je Ziel. Bei weniger als zwei Läufen ist der Vergleich ausgegraut.

## 6a. Der Mittel-Eintrag — gemessen

Karte **273 × 56 dp**, Radius 12 dp, Fläche `--flaeche`, Rand 1 dp `--rand`,
Schatten `0 2px 6px rgba(15,23,42,.10)`; darunter 1 dp Trenner → **57 dp Takt**.

- **Ampel-Kantenbalken** 3 dp ganz links, volle Höhe, Radius `2px 0 0 2px`, Farbe = Ampel des Mittels.
- **Zeile 1** (Höhe 18–20 dp): **Löslichkeitspunkt** 8 dp Ø (`--lwas` gefüllt bei wasserlöslich; `--lfetf` mit 1,5 dp Rand `--lfetr` bei fettlöslich; beide Punkte nebeneinander bei „beides") · **Name** 15 sp/500 in `--txt`, einzeilig, max ≈ 183 dp. Bei Überlauf: erst den Klammerzusatz weglassen, dann Auslassungszeichen — **nie** im Wortstamm brechen.
- **Zeile 2** (Höhe 16–18 dp, 12 sp, `--txt2`): Dosis „2 × 80 mg = 160 mg" · „· Pulver" nur wenn die Form nicht Kapsel ist · „· alle 3 Tage" nur wenn die Frequenz nicht täglich ist · rechtsbündig der **Kurzgrund** in der Ampel-Textfarbe („stört 3, 7"), bei Grün leer.
- **Häkchen** 22 dp in 44 × 44 dp Tippfläche rechts; gesetzt: Fläche `--akz`, Haken weiß, Radius ≈ 6 dp.
- **Deaktiviert:** Fläche `--grund` statt `--flaeche`, Balken `--deakt`, Texte auf 38 % Deckkraft, Punkte entsättigt, Kästchen leer, kein Schatten.
- **Kombi-Gruppe:** 2 dp Klammerlinie links über alle Mitglieder, darüber eine Kopfzeile „zusammen einnehmen" mit dem Gruppen-Häkchen (Teilzustand möglich).

## 6b. Der Ziel-Eintrag — gemessen

Zeile **273 × 40 dp**: durchgehender **Ampel-Kantenbalken** 3 dp links · **Nummernkreis** 20 dp Ø,
Rand 1 dp, Zahl 11 sp/600 · **Zieltext** 14 sp/500, einzeilig ellipsiert · **Ziehgriff**
`drag_indicator` 24 dp in 44 dp Tippfläche (nur B-12).

**Begründung bei Rot oder Gelb:** eine eigene aufklappende Zeile (keine Sprechblase). Die Höhe
wächst in 200 ms; der Text steht in `--txt2`, der Kantenbalken läuft über die gesamte
aufgeklappte Höhe durch. Beim Ziehen klappt sie automatisch zu.

## 7. Ikonografie

**Material Symbols Rounded**, Gewicht 400. Im Entwurf verwendet:
`dark_mode` / `light_mode` (F-22) · `track_changes` (Ziel-Katalog) · `settings` · `inventory_2`
(Mittel-Katalog) · `add` · `search` · `drag_indicator` · `arrow_back` · `more_vert` ·
`chevron_right` · `expand_more` · `auto_awesome` (Auswerten) · `history` · `insights` ·
`volume_up` / `pause` / `stop` · `signal_cellular_alt` · `battery_full` · `devices_fold`.

Keine Fotos, keine Illustrationen. Bildelemente sind ausschließlich Ampeln, Löslichkeitspunkte
und Symbole.

## 8. Texte

Wie in `../v1/02-UI-SPEC.md` §8 festgelegt und im Entwurf bestätigt. Im Entwurf zusätzlich
gemessen: `Ziele {n}` · `Löslichkeit` / `Einnahme` · `Diesen Stack auswerten` ·
`Alle Stacks zusammen prüfen` · `Dosis` mit `Frei` / `Dienst` · `veraltet` · `Entfernt` /
`RÜCKGÄNGIG` · `Ordnen` / `Fertig` · `Auch neutrale zeigen` · `nicht bedient` ·
`Ziele ordnen — {Stack}` · `auth.openai.com/device` · `Seite öffnen`.

## 9. Barrierefreiheit

- Mindest-Tippfläche 44 × 44 dp — im Entwurf bei allen Symbolknöpfen, Häkchen und Ziehgriffen eingehalten.
- Farbe ist nie das einzige Merkmal: jede Ampel trägt zusätzlich den Kurzgrund als Text, graue Ampeln das Wort „nicht bedient", der fettlösliche Punkt ist hohl statt gefüllt.
- Kontraste wie in §2; die hellen Ampelfarben sind gegenüber den üblichen Tönen nachgedunkelt, damit sie auf Weiß tragen.
- Bis 130 % Systemschrift müssen alle Zeilen lesbar bleiben; der Mittel-Eintrag darf dabei auf 64 dp wachsen und der Name eine zweite Zeile bekommen.

## 10. Zweispaltiges Layout (Innendisplay 440 × 583 dp)

Im Entwurf **nicht** gebaut — er zeigt ausschließlich die zugeklappte Leitgröße. Es gilt daher
unverändert die Absicht aus v1:
B-02 links Ziele 176 dp dauerhaft offen (Überlagerung entfällt), rechts Liste + Auswertung ·
B-09 links Dosen, rechts Konkurrenzen · B-10 links Rubriken, rechts Inhalt · B-01 Kartenraster
2 × 212 dp · übrige einspaltig zentriert auf max 440 dp · Blätter gedeckelt auf 400 dp.
**Das ist der einzige Teil dieses Dokuments, der nicht gemessen ist** — er ist entsprechend
gekennzeichnet und beim Bau als Absicht zu behandeln.

## 11. Offene Fragen

Keine. Alle gestalterischen Werte sind gemessen; die Ausnahme (§10) ist benannt.
