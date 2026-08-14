# Entwurf nach Durchgang 2 — StackLabor

Stand: 14.08.2026, 12:46 · Stufe: v1 (Entwurf) · Plattform: Android
Baut auf `ENTWURF-durchgang-1.md` auf (Entscheidungen 1–13 gelten unverändert).

## Entscheidungen 14–16

| Nr | Frage | Entscheidung |
|----|-------|--------------|
| 14 | NEM-Katalog | **Ja, Katalog mit Stammdaten.** Jedes NEM existiert einmal (Name, Löslichkeit, Hersteller, Durchfallrisiko, Darreichungsform). Stacks verweisen per stabiler `nem_id` darauf und legen ihre eigene Dosis fest. Nur so ist die Tagesgesamtdosis (Entscheidung 11) berechenbar. Entsteht beim Einlesen des Startbestands automatisch. |
| 15 | Ziele ordnen | **Anschauen im Überlagerungs-Blatt, Ordnen im Vollbild.** Der Ziel-Streifen in B-02 öffnet ein Blatt (max 281 dp) mit allen Zielen, Ampeln und Gründen. Knopf „Ordnen" führt in ein Vollbild (B-12), wo Drag & Drop mit Auto-Scroll die volle Höhe hat. |
| 16 | Feinheiten (alle vier gewählt) | **Kombi-Gruppen** („zusammen einnehmen", bleiben beim Sortieren beieinander, Gruppen-Häkchen schaltet alle) · **Dosis-Varianten** mit Umschalter Frei/Dienst auf B-01 · **Auswertungs-Historie** (letzte 5 Läufe je Stack, vergleichbar) · **Suche** in Stack-Liste und Katalog |

## Abgeleitet und gesetzt (Durchgang 2)

| Punkt | Festlegung |
|---|---|
| Codex-Modell | In B-10 wählbar. Vorgabe `gpt-5.6-terra`, Denkstufe `high` |
| Antwortdarstellung | Streamend, wortweiser Aufbau (60 ms je Wort); das JSON wird erst am Ende geparst |
| Codex-Fehler | `REAUTH` → Knopf „Neu anmelden" (B-11) · `QUOTA` → Klartext mit Wartezeit · `NETWORK` → ein automatischer Wiederholversuch, danach „Erneut". Nie stumm |
| Ampeln bei Fehler | Bleiben auf dem letzten gültigen Stand sichtbar, Karte trägt „Stand veraltet" |
| Vorlesen | Nur der Fließtext, absatzweise. Nicht die Bewertungstabelle. Läuft über `TtsPlaybackService` weiter, wenn der Bildschirm verlassen wird; Stopp in der Benachrichtigung |
| Langes Drücken in der Ansicht „Löslichkeit" | Kontextmenü (Bearbeiten / Verschieben / Entfernen). Ziehen nur in der Ansicht „Einnahme" |
| Sammelampel B-01 | Schlechteste Ziel-Ampel des Stacks + drei Zählpunkte (Anzahl grün/gelb/rot). Grau, solange nie ausgewertet |
| Import | Fragt immer: Ersetzen / Dazu / Abbrechen. Vorher stiller Sicherungs-Export. Datei trägt `schema_version`; ältere Fassung wird migriert, neuere abgelehnt |
| Reduzierte Bewegung | Wird respektiert: Dauerbewegung und Schmuck aus; Ampel-Überblendung (320 ms) und Ausweichen beim Ziehen (220 ms) bleiben, weil sie Bedeutung tragen |
| Später, nicht jetzt | NEM in einen anderen Stack verschieben/kopieren · Stack duplizieren |

## Das Antwortformat für Codex (F-12/F-13)

**Dünne Tabelle** — nur Zellen, die NICHT neutral sind. Alles Nichtgenannte gilt als neutral.
Bei mehr als 12 aktiven Zielen wird in zwei Anfragen zerlegt und lokal vereinigt.

```json
{ "zellen": [ { "nem": "<nem_id>", "ziel": "<ziel_id>",
      "wirkung": "stuetzt|stoert",
      "staerke": 1,
      "grund": "max 140 Zeichen, ein Satz" } ],
  "konkurrenzen": [ { "nem_a": "<id>", "nem_b": "<id>",
      "art": "aufnahme|wirkung|zeitpunkt",
      "schwere": 2, "grund": "ein Satz" } ],
  "antworten": [ { "frage": "<id>", "text": "Antwort als Fließtext" } ],
  "gesamt": "Fließtext für B-07, Markdown, wird vorgelesen",
  "hinweise": [ "Einnahme-/Dosis-Warnungen, je ein Satz" ] }
```

Im Systemtext verbindlich: *Nur dieses JSON, kein Text davor oder danach. Nicht genannte
NEM×Ziel-Paare gelten als neutral. Alternierende Paare nie als Konkurrenz melden.*

**Mitgeschickt wird je Stack:** Zeitpunkt, Einnahme-Hinweis, je aktivem NEM
{name, gesamtdosis, form, löslichkeit, frequenz, alterniert_mit, hersteller, durchfallrisiko,
zusatztext}, je Ziel {rang, text}, die eigenen Fragen des Stacks.
Deaktivierte NEM (Häkchen weg) werden **nicht** mitgeschickt.
Bei kaputtem JSON: ein Wiederholversuch mit „nur JSON"; scheitert auch der, wird der Fließtext
gespeichert und die Tabelle bleibt leer → Ampeln grau statt falsch.

## Die Rechenregel für die Ampeln (F-14)

**Zielgewicht** aus dem Rang im jeweiligen Stack: Rang 1–3 → g=3 · Rang 4–7 → g=2 · ab Rang 8 → g=1.

**NEM-Ampel** (schlimmster Fall über alle aktiven Ziele, in denen dieses NEM stört): `p = g × staerke`
- **rot** wenn max p ≥ 6 · **gelb** wenn max p ≥ 2 · **grün** wenn keine Störung
- Beispiel: stört ein Ziel auf Rang 2 (g=3) mit Stärke 2 → p=6 → rot. Dasselbe Ziel auf Rang 9
  (g=1) → p=2 → gelb. Genau das erledigt das Umsortieren ohne KI-Abfrage.

**Ziel-Ampel**: `S = Σ(staerke der stützenden aktiven NEM) − Σ(staerke der störenden)`
- **rot** wenn eine Störung der Stärke 3 vorliegt oder S ≤ −1
- **gelb** wenn S = 0…2
- **grün** wenn S ≥ 3 und keine Störung ≥ 2
- **grau „nicht bedient"** wenn kein einziges stützendes NEM vorhanden ist — nicht grün.
  Grau zählt in der Sammelampel wie gelb.

**Sammelampel B-01**: schlechteste Ziel-Ampel des Stacks; grau, wenn die Bewertung fehlt.

## Was die Bewertung ungültig macht (F-23)

| macht ungültig („veraltet", Ampeln bleiben sichtbar) | macht NICHT ungültig (nur Neuberechnung) |
|---|---|
| NEM hinzugefügt oder endgültig entfernt | Häkchen an/aus (F-05) |
| Dosis, Einheit, Stückzahl geändert | Ziel-Priorität per Drag & Drop (F-10) |
| Frequenz oder „alterniert mit" geändert | Sortierung Löslichkeit ↔ Einnahme (F-06) |
| Zusatztext für die KI geändert | Einnahme-Reihenfolge per Drag & Drop (F-07) |
| Ziel in diesem Stack an-/abgewählt | Ziel umbenannt ohne Sinnänderung (leiser Vermerk) |
| Neues Ziel angelegt und hier aktiviert | Hell/Dunkel, Stimme, Modellwahl |
| Eigene Frage angelegt/geändert/gelöscht | Stack umbenannt |
| Einnahme-Hinweis oder Zeitpunkt geändert | Import, der diesen Stack nicht berührt |
| Darreichungsform oder Löslichkeit geändert | Auswertung eines anderen Stacks |
| Dosis-Variante Frei/Dienst gewechselt | — |

## Bildschirme (Stand Durchgang 2)

| Kennung | Bildschirm | Zweck |
|---|---|---|
| B-01 | Hauptbildschirm (Stack-Übersicht) | Stack-Karten mit Sammelampel · Hell/Dunkel · Frei/Dienst · „Alle Stacks zusammen prüfen" · Plus · Wege zu B-03, B-09, B-10, B-14 |
| B-02 | Stack-Detail | Ziel-Streifen (öffnet Blatt) · Sortier- und Suchleiste · NEM-Liste · fester Auswerten-Sockel |
| B-03 | Ziel-Katalog | Ziele einmal anlegen, umbenennen, löschen; zeigt Verwendung |
| B-04 | Ziele dieses Stacks (Blatt) | Ankreuzen + Ampeln + Gründe ansehen; Knopf „Ordnen" → B-12 |
| B-05 | NEM bearbeiten (Blatt) | Stammdaten + stackeigene Dosis, Dosis-Varianten, Frequenz, alterniert mit, Zusatztext |
| B-06 | Aufschlüsselung (Blatt) | NEM → Ziele oder Ziel → NEM, mit Begründungen. Ein Bildschirm, zwei Richtungen |
| B-07 | Auswertung im Vollbild | Voller KI-Text, Zeitstempel, Modell, Vorlese-Sockel. Auch von B-09 aus |
| B-08 | Eigene Fragen (Blatt) | Fragen je Stack anlegen, ändern, löschen |
| B-09 | Alle Stacks zusammen | Tagesgesamtdosis je Wirkstoff + stackübergreifende Konkurrenzen. Vollständiger Zustandssatz wie B-02 |
| B-10 | Einstellungen | Vorlesen · Codex (Konto, Modell, Denkstufe) · Daten (Export/Import/Startbestand) · Erscheinung · Bewegung reduzieren |
| B-11 | Codex-Anmeldung | Geräte-Flow mit Code, Adresse, Wartezustand |
| B-12 | Ziele ordnen (Vollbild) | Drag & Drop mit Auto-Scroll über die volle Höhe |
| B-13 | Stack bearbeiten (Blatt) | Name, Zeitpunkt, Einnahme-Hinweis, Löschen |
| B-14 | NEM-Katalog | Alle NEM mit Stammdaten, Suche, Zusammenführen |
| B-15 | Auswertungs-Historie (Blatt) | Letzte 5 Läufe je Stack, vergleichbar |

## Maße (gemessen/gerechnet, Cover 297 × 469 dp)

- Nutzbare Höhe: 469 − 24 Statusleiste − 24 Gestenleiste = **421 dp**
- Kartenbreite: 297 − 2×12 = **273 dp**
- B-02: Kopf 56 + Ziel-Streifen 40 + Sortier-/Suchleiste 36 + Auswerten-Sockel 52
  → **237 dp Liste = 4 NEM** (Takt 57 dp) + Anschnitt
- **NEM-Eintrag**: Karte 273 × 56 dp, Radius 12. Kantenbalken 3 dp links (volle Höhe, Ampelfarbe).
  Inhalt ab x=13. Häkchen 22 dp in 44×44-Tapfläche rechts → **Textspalte 208 dp**
  - Zeile 1 (20 dp): Löslichkeitspunkte 8 dp Ø (ein Punkt 14 dp Spalte, zwei 25 dp) · Name 15 sp,
    max **183 dp**, einzeilig. Überlauf: erst Klammerzusatz weglassen, dann Ellipse, nie im Wortstamm
  - Zeile 2 (18 dp, 12 sp): Dosis „2 × 80 mg = 160 mg" max 130 dp · „· Pulver" nur wenn Form ≠ Kapsel
    · „· alle 3 Tage" nur wenn Frequenz ≠ täglich · rechtsbündig Kurzgrund 78 dp („stört 3, 7")
  - Deaktiviert: Fläche = Grundfarbe, Balken #CBD5E1, Texte 38 % Deckkraft, Punkte entsättigt #94A3B8
- **Ziel-Eintrag**: 273 × 40 dp. Kantenbalken 3 dp · Nummernkreis 20 dp Ø bei x=13 (11 sp) ·
  Zieltext 14 sp ab x=41, 192 dp · Ziehgriff 24 dp bei x=241 (44-dp-Tapfläche).
  Grund bei Rot/Gelb: aufklappende eigene Zeile, +n×16+8 dp in 200 ms, klappt beim Ziehen zu

## Helle Farbtabelle (Standard-Erscheinung)

| Rolle | Hex | Verwendung | Kontrast |
|---|---|---|---|
| Grund | #F5F7FA | Bildschirmhintergrund, deaktivierte Karte | — |
| Fläche / Karte | #FFFFFF | Stack-Karte, NEM-Eintrag, Blätter | 1,05:1 |
| Erhöhte Fläche | #F1F5F9 | Sockel, Chips, Kopfleiste | 1,1:1 |
| Rand | #E2E8F0 | Trenner 1 dp, Kartenkontur | 1,3:1 |
| Text stark | #0F172A | NEM-Name, Titel | 17,4:1 |
| Text schwach | #64748B | Zeile 2, Zeitpunkte, Metazeile | 4,8:1 |
| Akzent | #4F46E5 | Auswerten-Knopf, Häkchen aktiv, Links | 7,6:1 |
| Ampel grün | #047857 | Kantenbalken, Zählpunkt | 4,9:1 |
| Ampel gelb (Fläche) | #D97706 | Kantenbalken, Zählpunkt | 3,3:1 |
| Ampel gelb (Text) | #B45309 | Kurzgrund, Grundzeile | 5,9:1 |
| Ampel rot | #DC2626 | Kantenbalken, Aura, Warntext | 4,5:1 |
| Ampel rot (kräftig) | #B91C1C | Fehlerkarten-Überschrift | 6,0:1 |
| Löslich wasser | #059669 | 8 dp Punkt gefüllt | 4,0:1 |
| Löslich fett | #FFFFFF + 1,5 dp Rand #64748B | 8 dp Punkt hohl | Rand 4,8:1 |
| Deaktiviert | #CBD5E1 | Balken/Punkte im ausgegrauten Eintrag | 1,7:1 (bewusst) |

> Dunkle Erscheinung als gleichwertige zweite Fassung: Grund #0B0E14, Fläche #141A24,
> Akzent #22D3EE, Ampeln #34D399 / #FBBF24 / #F87171. Beide Fassungen vollständig.

## Bewegungen mit Ort (M-Liste, Stand Durchgang 2)

| Kennung | Bewegung | Wo | Auslöser | Werte |
|---|---|---|---|---|
| M-01 | Ziel aufnehmen | B-12 | Long-Press 300 ms | Scale 1.0→1.04 in 140 ms cubic-bezier(0.05,0.7,0.1,1), Elevation 1→8 dp, Haptik |
| M-02 | Ziele ausweichen | B-12 | Ziehen | 220 ms cubic-bezier(0.2,0,0,1), 12 ms je Zeile versetzt |
| M-03 | Nummern laufen live um | B-12 | Ziehen | Crossfade 120 ms linear + Y-Versatz 6 dp |
| M-04 | Loslassen/Einrasten | B-12 | Loslassen | Spring damping 0.75 / stiffness 380, 260 ms + Haptik |
| M-05 | Auto-Scroll am Rand | B-12 | Ziehen über die Randzone | Zone 64 dp, 0→900 dp/s linear, Start nach 120 ms |
| M-06 | Abbruch-Rückflug | B-12, B-02 | Abbruch | 300 ms, Elevation 8→1 dp |
| M-07 | Ampel-Überblendung | B-01, B-02 | F-14, F-05 | 320 ms cubic-bezier(0.4,0,0.2,1), nie hart |
| M-08 | Ampeln gestaffelt | B-02 | F-14 | 45 ms Versatz je Zeile, gedeckelt bei 10 Stufen / 450 ms |
| M-09 | Puls nur geänderter Ampeln | B-02 | F-14 | Ring +6 dp, Alpha 0.55→0, 520 ms cubic-bezier(0,0,0,1) |
| M-10 | Verbindungsfarbe NEM↔Ziel | B-06 | F-15 | 900 ms, 2 dp Rand |
| M-11 | Warte-Skelett mit Schimmer | B-02, B-09 | F-12, F-13 | Periode 1400 ms, Sweep-Breite 40 % |
| M-12 | Ampeln entsättigt pulsieren | B-02, B-09 | während der Auswertung | Alpha 1.0→0.45→1.0, Periode 1600 ms |
| M-13 | Streamender Antworttext | B-07 | F-12, F-13 | Fade+Up 6 dp in 180 ms, 60 ms je Wort; Erzähl-Crossfade 240 ms |
| M-14 | Ziel-Blatt aufklappen | B-04 | F-09 | 380 ms cubic-bezier(0.2,0,0,1), Zeilen 30 ms gestaffelt, Pfeil 180° in 300 ms |
| M-15 | Sprech-Markierung + Pegelbalken | B-07 | F-16 | Kante 200 ms cubic-bezier(0.4,0,0.2,1), 3 Balken auf `SpeechLoudness` |
| M-16 | Dauerbewegung | B-01, B-02 | Untätigkeit | Kopf-Verlauf 30 s · Glanzkante 8 s · Plus atmet 3200 ms (Scale 1.0→1.02) |
| M-17 | Faltvorgang Cover→Innen | alle | Gerätefaltung | Shared-Element 400 ms, zweite Spalte X +24→0 / Alpha 0→1 in 300 ms, 100 ms Verzug |
| M-18 | NEM-Drag & Drop | B-02 | F-07 (nur Ansicht „Einnahme") | wie M-01 bis M-06 |
| M-19 | Erscheinungswechsel | B-01 + offene Blätter | F-22 | Token-Crossfade 420 ms cubic-bezier(0.4,0,0.2,1); Blätter bleiben offen |
| M-20 | Blatt öffnen/schließen | B-04…B-08, B-13, B-15 | F-03, F-11, F-15 | Slide-Up 300 ms cubic-bezier(0.05,0.7,0.1,1) + Abdunklung 0→0.32 |
| M-21 | Atmende Aura an roter Ampel | B-01, B-02 | rote Ampel | Glühradius 4→8 dp, 2,4 s Sinus, max 3 gleichzeitig |
| M-22 | Gestaffeltes Einblenden | B-01, B-02 | Bildschirm öffnen | 40 ms Versatz, y +12 dp, max 8 Elemente |

**Niemals bewegen:** Ampelfarben im Ruhezustand · Dosis- und Einheitszahlen · das Häkchen-Kästchen ·
Ziel-Nummern außerhalb einer aktiven Umsortierung · der Begründungstext bei Rot/Gelb.

**Reduzierte Bewegung:** alle Dauern 0 ms AUSSER M-07 (320 ms) und M-02 (220 ms); M-12 wird ein
statischer Graustand; das Streaming bleibt (es ist Inhalt, keine Animation); M-16 und M-21 komplett aus.

## Effekte mit Ort

| Effekt | Ort |
|---|---|
| Glasfläche (Weichzeichner 24 dp) | B-02 Auswerten-Sockel + Kopfleiste, B-01 Kopf — **feste Flächen, nie über der Liste** |
| Verlaufsrand 1,5 dp | Stack-Karte B-01, Auswertungs-Karte B-02/B-09; Verlauf Akzent→transparent |
| Atmende Aura | roter Kantenbalken B-01/B-02 (siehe M-21) |
| Schimmer beim Laden | 4 NEM-Platzhalter B-02, Text-Platzhalter B-07, Dosis-Zeilen B-09 |
| Animierter Kopf-Verlauf | B-01 Kopf 96 dp, 30 s Farbwanderung, nur bei sichtbarem Bildschirm |
| Parallax | B-02: Stack-Name schrumpft 17→14 sp über 56 dp Scrollweg, Ziel-Streifen heftet an |
| Gestaffeltes Einblenden | B-01, B-02 (siehe M-22) |
| Tiefenschatten | Karten 2 dp Ruhe / 8 dp beim Ziehen, Blätter 16 dp |
| Haptik | Häkchen leicht · Drag-Aufnahme mittel · Wischen-Löschen schwer · rote Ampel nach Auswertung doppelt |
| Wortweises Aufblenden | B-07 KI-Text 60 ms je Wort; B-02 nur die Auszugs-Karte |

## Zweispaltiges Layout (Innendisplay 440 × 583 dp)

Zweispaltig: **B-02** (links Ziele 176 dp dauerhaft offen, rechts NEM + Auswertung 264 dp — das
Überlagerungs-Blatt entfällt) · **B-09** (links Dosen, rechts Konkurrenzen) · **B-10** (links Rubriken
160 dp, rechts Inhalt) · **B-01** (Kartenraster 2 × 212 dp, alle 6 Stacks ohne Scrollen).
Einspaltig zentriert auf max 440 dp: B-03, B-07, B-11, B-12, B-14.
Blätter bleiben Blätter, Breite gedeckelt auf 400 dp.

## Noch offen (Schlussdurchgang)

- Abnahmekriterien A-01 …
- Feinschliff der Zustandslisten je Bildschirm
- Texte der festen Beschriftungen
