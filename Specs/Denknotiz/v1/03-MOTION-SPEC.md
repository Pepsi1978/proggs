# Motion-Spec — Denknotiz
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

> **Direkte Baugrundlage:** Der Werft-Designer wurde auf aktuelle Nutzeranweisung übersprungen. Diese v1 ist ohne Messpaket unmittelbar umzusetzen; alle Werte sind verbindliche Bauwerte.

## 1. Bewegungs-Grundhaltung

Bewegung bestätigt Ursache und erhält räumliche Orientierung, sie dekoriert nicht. Das Grundgefühl ist ruhig, präzise und leicht gebremst: 240 ms mit `cubic-bezier(0.2,0,0,1)`, Exit schneller mit 180 ms. Aufnahme darf subtil leben, ansonsten bleibt Text beim Lesen still. Niemals bewegt werden Buchstaben einzeln, Karten beim bloßen Scrollen, Hintergründe per Parallaxe oder ruhende Navigation.

## 2. Kurven und Dauern

| Name | Dauer | Kurve | Wofür |
|------|-------|-------|-------|
| `standard` | 240 ms | `cubic-bezier(0.2,0,0,1)` | Eintritt, Zustands- und Farbwechsel |
| `exit` | 180 ms | `cubic-bezier(0.4,0,1,1)` | Verlassen, Ausblenden |
| `foldLayout` | 360 ms | `cubic-bezier(0.2,0,0,1)` | Außen-/Innen-Layoutwechsel |
| `sidebarIn` | 300 ms | `cubic-bezier(0.2,0,0,1)` | Session-Schublade öffnen |
| `sidebarOut` | 220 ms | `cubic-bezier(0.4,0,1,1)` | Session-Schublade schließen |
| `press` | 80 ms | `cubic-bezier(0.2,0,0,1)` | Druckzustand auf Scale 0,97 |
| `recordPulse` | 1600 ms | `cubic-bezier(0.4,0,0.6,1)` | subtile Aufnahmebewegung |
| `noteIn` | 260 ms | `cubic-bezier(0.2,0,0,1)` | Notiz 10 dp + Fade |
| `ttsHighlight` | 160 ms | `cubic-bezier(0.2,0,0,1)` | Absatzwechsel |
| `reducedFadeShort` | 100 ms | `cubic-bezier(0.2,0,0,1)` | reduzierte kleine Wechsel |
| `reducedFadeLong` | 160 ms | `cubic-bezier(0.2,0,0,1)` | reduzierte Bildschirmwechsel |

Nur diese benannten Kurven/Dauern werden verwendet.

## 3. Bewegungen im Einzelnen

### M-01 — Standard-Eintritt
- **Wo** — B-05, B-07 bis B-14
- **Auslöser** — Navigation zu einem Vollbildschirm
- **Was sich ändert** — Ziel `opacity 0 → 1`, `translateX 12 dp → 0 dp`; Quelle `opacity 1 → 0.72`, `translateX 0 → -8 dp`
- **Dauer / Kurve / Verzögerung / Wiederholung** — `standard`, 0 ms, einmalig
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-02 — Standard-Exit
- **Wo** — B-05, B-07 bis B-14
- **Auslöser** — Zurücknavigation
- **Was sich ändert** — Ziel rückwärts `translateX 0 → 12 dp`, `opacity 1 → 0`; darunterliegende Quelle auf Ausgangswerte
- **Dauer / Kurve / Verzögerung / Wiederholung** — `exit`, 0 ms, einmalig
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-03 — Fold-Layout passt sich an
- **Wo** — B-01/B-02
- **Auslöser** — Außen-/Innenwechsel oder Übertritt der adaptiven Breite
- **Was sich ändert** — Sessionleiste `width 0 → 152 dp`, Hauptinhalt `left 0 → 152 dp`; die äußere Schublade ist vorher geschlossen. Kartenbreite wird per Layout interpoliert, Scrollanker bleibt fest.
- **Dauer / Kurve / Verzögerung / Wiederholung** — `foldLayout`, 0 ms, einmalig je Wechsel
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-04 — Session-Schublade öffnet/schließt
- **Wo** — B-02 außen
- **Auslöser** — Session-Schaltfläche, Randwischen, Scrim, Zurück
- **Was sich ändert** — Öffnen `translateX -272 dp → 0 dp`, Scrim `opacity 0 → 1` mit Endfarbe `scrim`; Schließen exakt rückwärts
- **Dauer / Kurve / Verzögerung / Wiederholung** — öffnen `sidebarIn`, schließen `sidebarOut`, 0 ms, einmalig; Drag folgt 1:1 dem Finger
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-05 — Druckrückmeldung
- **Wo** — alle runden Aktionen, gefüllten Knöpfe, Kartenmenü-Aktionen
- **Auslöser** — Pointer down/up
- **Was sich ändert** — `scale 1 → 0.97` beim Druck; beim Loslassen `0.97 → 1`
- **Dauer / Kurve / Verzögerung / Wiederholung** — je Richtung `press`, 0 ms, einmalig
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-06 — Aufnahme pulsiert subtil
- **Wo** — B-01/B-03 Aufnahmeaktion und aktive Auftragsleiste
- **Auslöser** — Zustand `aufnehmend`
- **Was sich ändert** — äußerer Ring `scale 1 → 1.08 → 1`, `opacity 0.34 → 0.14 → 0.34`; Kern bleibt stabil. RMS moduliert nur Enddeckkraft zwischen 0.14 und 0.28, nie die Layoutgröße.
- **Dauer / Kurve / Verzögerung / Wiederholung** — `recordPulse`, 0 ms, endlos bis Stop/Abbruch
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-07 — Notiz tritt ein
- **Wo** — B-01 Notiz- und als Notiz gespeicherte KI-Karte
- **Auslöser** — F-07/F-14 speichert eine neue Notiz
- **Was sich ändert** — `translateY 10 dp → 0 dp`, `opacity 0 → 1`; keine Skalierung
- **Dauer / Kurve / Verzögerung / Wiederholung** — `noteIn`, 0 ms, einmalig; Liste hält die neue Karte sichtbar
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-08 — TTS-Absatzhighlight wechselt
- **Wo** — B-01 Notiz-/KI-Text
- **Auslöser** — MediaSession wechselt Absatz
- **Was sich ändert** — alter Hintergrund `ttsHighlight → transparent`, neue Fläche `transparent → ttsHighlight`, linke Kante `opacity 0 → 1`
- **Dauer / Kurve / Verzögerung / Wiederholung** — `ttsHighlight`, 0 ms, pro Absatzwechsel
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-09 — Dialog/Sheet erscheint
- **Wo** — B-03, B-04, B-06, Editoren in B-09/B-11
- **Auslöser** — Öffnen einer modalen Aufgabe
- **Was sich ändert** — Dialog `opacity 0 → 1`, `translateY 16 dp → 0`; Scrim `opacity 0 → 1`. Bottom Sheet `translateY 100 % → 0`.
- **Dauer / Kurve / Verzögerung / Wiederholung** — Eintritt `standard`, Exit `exit`, 0 ms, einmalig
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-10 — Löschen und Undo
- **Wo** — B-01, B-02, B-13
- **Auslöser** — bestätigtes Löschen oder Undo
- **Was sich ändert** — Löschen `opacity 1 → 0`, `translateX 0 → -16 dp`, danach Listenlücke animiert auf 0; Undo setzt die Karte `translateY 10 dp`, `opacity 0` ein und führt M-07 aus.
- **Dauer / Kurve / Verzögerung / Wiederholung** — Löschen `exit`, Lückenschluss `standard`; Undo `noteIn`
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-11 — Auswertungsgrenze verschiebt sich
- **Wo** — B-01
- **Auslöser** — ausschließlich vollständiger Erfolg von F-12
- **Was sich ändert** — altes Grenzlabel `opacity 1 → 0`; neues Grenzlabel an fester Zielposition `opacity 0 → 1`. Karten und Texte bewegen sich nicht; Lazy-Layout ordnet ohne künstliches Scrollen neu.
- **Dauer / Kurve / Verzögerung / Wiederholung** — altes Label `exit`, neues `standard`, 0 ms, einmalig
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-12 — Theme wechselt
- **Wo** — alle Bildschirme
- **Auslöser** — Theme-Kachel B-07
- **Was sich ändert** — alle Farbrollen interpolieren von aktuellem zum gewählten Wert; keine Geometrieänderung
- **Dauer / Kurve / Verzögerung / Wiederholung** — `standard`, 0 ms, einmalig
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-13 — Fortschritt aktualisiert
- **Wo** — B-01 Auftragsleisten, B-03, B-06, B-11, B-14
- **Auslöser** — Transkriptions-, KI-, Klon- oder Merge-Fortschritt
- **Was sich ändert** — determinate Balkenbreite auf den neuen Prozentwert; bei unbekannter Dauer stattdessen drei statische Statuspunkte, deren Deckkraft nacheinander wechselt
- **Dauer / Kurve / Verzögerung / Wiederholung** — Balken `standard` je Wert; Punkte Zyklus 1200 ms, jeweils 160 ms Fade, nur während Warten
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

### M-14 — Aktiver Navigationszustand
- **Wo** — B-02, B-09, B-10, Filter in B-12
- **Auslöser** — Auswahl
- **Was sich ändert** — Liniensymbol und Label `opacity 0.72 → 1`; Symbol wechselt per Crossfade zur gefüllten Variante, Auswahlfläche `transparent → accentSoft`
- **Dauer / Kurve / Verzögerung / Wiederholung** — `standard`, 0 ms, einmalig
- **Quelle** — v1 direkte Bauvorgabe, kein Messpaket

## 4. Bildschirmwechsel

| Von | Nach | Art | Dauer | Kurve |
|-----|------|-----|-------|-------|
| B-01 | B-02 außen | Schublade von links | 300 ms | `sidebarIn` |
| B-02 | B-01 außen | Schublade nach links | 220 ms | `sidebarOut` |
| B-01 | B-03/B-04/B-06 | Sheet/Dialog von unten + Fade | 240 ms | `standard` |
| B-03/B-04/B-06 | B-01 | nach unten + Fade | 180 ms | `exit` |
| B-01/B-02 | B-05/B-07/B-12/B-13 | M-01 | 240 ms | `standard` |
| B-07 | B-08/B-09/B-10/B-14 | M-01 | 240 ms | `standard` |
| B-10 | B-11 | M-01 | 240 ms | `standard` |
| jeder Vollbildschirm | zurück | M-02 | 180 ms | `exit` |
| Außenlayout | Innenlayout | M-03 ohne Navigationswechsel | 360 ms | `foldLayout` |

## 5. Rückmeldung auf Bedienung

| Element | Rückmeldung | Haptik |
|---------|-------------|--------|
| Runde/gefüllte Aktion | M-05, Ripple auf Bauteil begrenzt | leichter Tick |
| Aufnahme Start/Stop | M-05, Symbol-Crossfade ohne Rotation | deutlicher Klick bei Start und Stop |
| Abbrechen/Löschen | Fehlerfläche beim Druck, erst nach Bestätigung | Warnklick bei finaler Bestätigung |
| Sitzung/Profil/Segment | M-14 | leichter Tick |
| Langer Druck Karte | nach 400 ms Auswahlfläche, dann Menü | Long-Press-Haptik |
| Wischen Session-Schublade | folgt Finger ohne Federüberschwingen | keiner |
| TTS Absatz vor/zurück | M-08 | leichter Tick |

## 6. Dauerbewegung

| Bewegung | Wann | Periode |
|----------|------|---------|
| M-06 Aufnahme | nur solange tatsächlich aufgenommen wird | 1600 ms |
| M-13 Statuspunkte | nur bei unbekannt langem aktivem Warten | 1200 ms |

Keine Dauerbewegung im Ruhezustand, kein Parallaxeffekt, keine Rotation, kein wandernder Hintergrund und kein pulsierender KI-Knopf.

## 7. Lade- und Wartezustände

| Lage | Ab wann | Darstellung | Ende |
|------|---------|-------------|------|
| Lokale Room-Abfrage | ab 200 ms | drei ruhige Platzhalterzeilen, kein Schimmer | `standard` Crossfade zum Inhalt |
| Transkription | sofort nach Stop | Auftragsleiste, Phase „Senden“/„Filtern“ | B-03 per M-09 oder Fehlerfade |
| KI-Auswertung | sofort nach Absenden | Auftragsleiste „Block {n}/{m}“/„Gesamtauswertung“ | Antwort M-07, Grenze M-11 |
| TTS-Prefetch | ab 300 ms ohne Start | kleiner Fortschrittsring ohne Rotation: determinate Bogenfüllung | verschwindet `exit` beim Start |
| Device-Code | sofort | statische Code-Platzhalter und Text „Code wird geladen“ | Code `standard` Fade |
| Qwen-Upload/Backup-Merge | sofort | determinate Balken M-13 | Erfolg/Fehler `standard` Fade |

Warteanzeigen werden nicht eingeblendet, wenn der Vorgang vor 200 ms abgeschlossen ist, außer bei explizit als „sofort“ markierten Netzaufträgen.

## 8. Reduzierte Bewegung

Wenn Android reduzierte Bewegung meldet oder Animationsdauer auf 0 steht:

1. Keine Dauerbewegung: M-06 wird ein statischer starker Aufnahmering; M-13-Statuspunkte stehen statisch, begleitet von Text.
2. Keine Parallaxe und keine Rotation; beide sind auch regulär nicht vorgesehen.
3. Alle Schiebe-, Scale- und Größenbewegungen entfallen. Bildschirm-, Dialog-, Sidebar-, Fold- und Kartenwechsel sind reine Fades.
4. Kleine Zustandswechsel nutzen `reducedFadeShort` 100 ms, Bildschirme/Dialoge/Fold `reducedFadeLong` 160 ms.
5. TTS-Highlight wechselt per 100-ms-Fade und bleibt als Informationszustand erhalten.
6. Haptik und semantische Statusansagen bleiben aktiv.

## 9. Offene Fragen

Keine. Alle Bewegungsdetails sind verbindlich festgelegt.
