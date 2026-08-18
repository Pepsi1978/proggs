# UI-Spec — Denknotiz
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

> **Direkte Baugrundlage:** Der Werft-Designer wurde auf aktuelle Nutzeranweisung übersprungen. Diese v1 ist ohne Messpaket unmittelbar umzusetzen. Alle exakten Werte dieses Dokuments sind verbindliche, abgeleitete technische Entscheidungen und keine nachträglich zu ersetzenden Werft-Messwerte.

## 1. Gestalterische Grundhaltung

Denknotiz ist eine **ruhige futuristische Werkbank**: konzentriert, präzise und warm, nicht verspielt und nicht wie ein gewöhnlicher Messenger. Inhalt steht auf klar gegliederten, tiefen Flächen; feine 1-dp-Lichtkanten und sparsame Akzente vermitteln technische Wertigkeit ohne Neonüberladung. Source Serif 4 gibt Notizen und KI-Antworten Lesetiefe, Manrope hält Navigation und Bedienung sachlich, JetBrains Mono macht Zeit, Status und Modellmetadaten überprüfbar. Außen ist jede Fläche kompakt und einspaltig, innen entsteht durch die dauerhafte Sessionleiste eine großzügige Werkbank mit stabiler Orientierung.

## 2. Erscheinungen (Themes)

### 2.1 Gold-Dunkel *(Standard)*

| Rolle | Wert | Verwendung |
|-------|------|------------|
| `background` | `#0B0A08` | App-Grundfläche |
| `surface` | `#14120E` | Karten, Seitenleiste |
| `surfaceRaised` | `#1C1912` | Dialoge, aktive Karten |
| `surfaceInput` | `#11100D` | Eingaben |
| `outline` | `#3D3728` | neutrale Ränder/Trenner |
| `lightEdge` | `rgba(255,244,205,0.18)` | 1-dp-Lichtkante oben/links |
| `accent` | `#D8B65A` | Primäraktionen, aktive Symbole |
| `accentStrong` | `#F0D487` | Fokus, TTS-Highlight-Kante |
| `accentSoft` | `rgba(216,182,90,0.14)` | Auswahl, Highlight |
| `textPrimary` | `#F4F0E5` | Haupttext |
| `textSecondary` | `#B9B09B` | Titel zweiter Ebene |
| `textMuted` | `#817A69` | Metadaten |
| `onAccent` | `#171208` | Text auf Akzent |
| `success` | `#87B887` | Erfolg |
| `warning` | `#D7A35E` | Warnung |
| `error` | `#E47770` | Fehler/Löschen |
| `scrim` | `rgba(0,0,0,0.62)` | modaler Hintergrund |
| `ttsHighlight` | `#332C18` | aktiver Absatz |

### 2.2 Gold-Hell

| Rolle | Wert | Verwendung |
|-------|------|------------|
| `background` | `#F7F3E8` | App-Grundfläche |
| `surface` | `#FFFDF7` | Karten, Seitenleiste |
| `surfaceRaised` | `#FFFFFF` | Dialoge, aktive Karten |
| `surfaceInput` | `#F1EBDD` | Eingaben |
| `outline` | `#D5C9AE` | neutrale Ränder/Trenner |
| `lightEdge` | `rgba(255,255,255,0.88)` | 1-dp-Lichtkante oben/links |
| `accent` | `#8B6A16` | Primäraktionen, aktive Symbole |
| `accentStrong` | `#684B00` | Fokus, TTS-Highlight-Kante |
| `accentSoft` | `rgba(139,106,22,0.12)` | Auswahl, Highlight |
| `textPrimary` | `#241F15` | Haupttext |
| `textSecondary` | `#5F5748` | Titel zweiter Ebene |
| `textMuted` | `#817765` | Metadaten |
| `onAccent` | `#FFFFFF` | Text auf Akzent |
| `success` | `#4F7A51` | Erfolg |
| `warning` | `#8C5D18` | Warnung |
| `error` | `#A83F39` | Fehler/Löschen |
| `scrim` | `rgba(25,20,12,0.42)` | modaler Hintergrund |
| `ttsHighlight` | `#EEE1BC` | aktiver Absatz |

### 2.3 Dunkel

| Rolle | Wert | Verwendung |
|-------|------|------------|
| `background` | `#090C10` | App-Grundfläche |
| `surface` | `#11161C` | Karten, Seitenleiste |
| `surfaceRaised` | `#19212A` | Dialoge, aktive Karten |
| `surfaceInput` | `#0D1217` | Eingaben |
| `outline` | `#2D3946` | neutrale Ränder/Trenner |
| `lightEdge` | `rgba(220,238,255,0.16)` | 1-dp-Lichtkante oben/links |
| `accent` | `#6EB7D8` | Primäraktionen, aktive Symbole |
| `accentStrong` | `#A6DDF2` | Fokus, TTS-Highlight-Kante |
| `accentSoft` | `rgba(110,183,216,0.14)` | Auswahl, Highlight |
| `textPrimary` | `#EDF3F7` | Haupttext |
| `textSecondary` | `#AAB8C3` | Titel zweiter Ebene |
| `textMuted` | `#748390` | Metadaten |
| `onAccent` | `#071318` | Text auf Akzent |
| `success` | `#70B99A` | Erfolg |
| `warning` | `#D2A65A` | Warnung |
| `error` | `#E36F77` | Fehler/Löschen |
| `scrim` | `rgba(0,0,0,0.64)` | modaler Hintergrund |
| `ttsHighlight` | `#16313D` | aktiver Absatz |

### 2.4 Hell

| Rolle | Wert | Verwendung |
|-------|------|------------|
| `background` | `#F3F6F8` | App-Grundfläche |
| `surface` | `#FFFFFF` | Karten, Seitenleiste |
| `surfaceRaised` | `#FFFFFF` | Dialoge, aktive Karten |
| `surfaceInput` | `#EAF0F3` | Eingaben |
| `outline` | `#CBD6DC` | neutrale Ränder/Trenner |
| `lightEdge` | `rgba(255,255,255,0.96)` | 1-dp-Lichtkante oben/links |
| `accent` | `#176B89` | Primäraktionen, aktive Symbole |
| `accentStrong` | `#0D4E68` | Fokus, TTS-Highlight-Kante |
| `accentSoft` | `rgba(23,107,137,0.11)` | Auswahl, Highlight |
| `textPrimary` | `#172127` | Haupttext |
| `textSecondary` | `#4F606A` | Titel zweiter Ebene |
| `textMuted` | `#71828C` | Metadaten |
| `onAccent` | `#FFFFFF` | Text auf Akzent |
| `success` | `#39765F` | Erfolg |
| `warning` | `#8C651F` | Warnung |
| `error` | `#A43E47` | Fehler/Löschen |
| `scrim` | `rgba(16,27,33,0.40)` | modaler Hintergrund |
| `ttsHighlight` | `#D9ECF3` | aktiver Absatz |

## 3. Typografie

Schriften werden als lokale App-Ressourcen gebündelt: **Manrope** 400/500/600/700 für UI, **Source Serif 4** 400/500/600 und Italic 400 für Notizen/KI, **JetBrains Mono** 400/500/600 für Metadaten.

| Rolle | Familie | Größe | Gewicht | Zeilenhöhe | Laufweite |
|-------|---------|-------|---------|------------|-----------|
| App-/Bildschirmtitel | Manrope | 22 sp | 700 | 28 sp | −0,2 sp |
| Sitzungstitel | Manrope | 16 sp | 600 | 21 sp | 0 sp |
| Kartenüberschrift | Manrope | 15 sp | 600 | 20 sp | 0 sp |
| Notiztext | Source Serif 4 | 17 sp | 400 | 27 sp | 0 sp |
| KI-Antwort | Source Serif 4 | 17 sp | 400 | 28 sp | 0 sp |
| Fokusfrage | Source Serif 4 | 20 sp | 500 | 29 sp | 0 sp |
| Eingabe | Source Serif 4 | 17 sp | 400 | 26 sp | 0 sp |
| Primärknopf | Manrope | 14 sp | 700 | 20 sp | +0,1 sp |
| Navigation/Zeile | Manrope | 14 sp | 600 | 20 sp | 0 sp |
| Hilfstext | Manrope | 13 sp | 400 | 18 sp | 0 sp |
| Metadaten | JetBrains Mono | 11 sp | 500 | 16 sp | +0,2 sp |
| Gerätecode | JetBrains Mono | 30 sp | 600 | 38 sp | +2,4 sp |

## 4. Maße und Raster

Grundraster 4 dp. Fenster-Inset-Schutz gilt an allen Rändern.

| Maß | Außen | Innen |
|-----|-------|--------|
| Horizontaler Seitenrand Inhalt | 12 dp | 20 dp |
| Oberer/unterer Inhaltsabstand | 12 dp | 16 dp |
| Kopfleiste | 56 dp | 60 dp |
| Abstand Karten | 10 dp | 14 dp |
| Karten-Innenabstand | 14 dp | 18 dp |
| Composer Mindesthöhe / Maximalhöhe | 64 / 176 dp | 68 / 196 dp |
| Runde Hauptaktion | 56 dp | 60 dp |
| Kleine Aktion Tippfläche / Symbol | 44 / 22 dp | 44 / 22 dp |
| Session-Schublade / Sessionleiste | 272 dp überlagernd | 152 dp dauerhaft |
| Hauptinhalt neben Sessionleiste | volle Breite | Restbreite, mindestens 288 dp |
| Dialogbreite | Fenster minus 24 dp | 400 dp, maximal Fenster minus 40 dp |
| Bottom-Sheet Maximalhöhe | 92 % | 88 % |
| Trennlinie | 1 dp | 1 dp |
| Mindest-Tippfläche | 48 × 48 dp | 48 × 48 dp |

**Abgeleitete technische Entscheidung:** Die innere, dauerhaft sichtbare Sessionleiste wird bei verfügbarer Fensterbreite unter 420 dp automatisch zur Außenschublade; auf dem spezifizierten inneren Zielraum ist sie 152 dp breit. Die Entscheidung folgt der tatsächlichen Compose-Fensterbreite, nicht einem Geräte­namen.

## 5. Formen und Tiefe

| Bauteil | Radius | Rand | Schatten / Effekt |
|---------|--------|------|-------------------|
| `.notizkarte`, `.kiantwortkarte` | 22 dp | 1 dp `outline`; oben/links zusätzlich 1 dp `lightEdge` | dunkel: 0/8/24 dp `rgba(0,0,0,0.30)`; hell: 0/5/18 dp `rgba(21,33,40,0.10)` |
| `.eingabe`, `.suchfeld` | 16 dp | 1 dp `outline`; Fokus 1 dp `accent` | keiner |
| `.dialog`, `.bottomSheet` | 28 dp | 1 dp `lightEdge` | dunkel: 0/16/48 dp `rgba(0,0,0,0.44)`; hell: 0/14/40 dp `rgba(21,33,40,0.16)` |
| `.rundeAktion`, `.iconButton` | vollrund | 1 dp `lightEdge` | Hauptaktion 0/6/18 dp `accentSoft` |
| `.sessionleiste` | 0 dp außen; rechts 22 dp als Schublade | rechts 1 dp `outline` | Schublade 8/0/28 dp `rgba(0,0,0,0.32)` |
| `.profilkarte`, `.einstellungsgruppe` | 18 dp | 1 dp `outline` | keiner |
| `.auswertungsgrenze` | vollrundes Label | horizontale 1 dp `outline` | Label auf `background` |
| `.ttsHighlight` | 10 dp | links 2 dp `accentStrong` | Fläche `ttsHighlight` |

Keine Glasunschärfe, keine Parallaxe, keine perspektivisch kippenden Karten. Tiefe entsteht nur durch Fläche, Schatten und Lichtkante.

## 6. Bildschirme

| Kennung | Bildschirm | Zweck | Startbildschirm? | führt zu |
|---------|------------|-------|-------------------|----------|
| B-01 | Denkverlauf | Sitzung lesen, Entwurf, Aufnahme, Auswertung, TTS | **ja** | B-02, B-03, B-05, B-06, B-07, B-12 |
| B-02 | Adaptive Sessionleiste | Sitzungen wechseln und organisieren | innen Bestandteil von B-01 | B-01, B-12, B-13 |
| B-03 | Diktat bestätigen | Transkript prüfen und an Entwurf anhängen | nein | B-01, B-04 |
| B-04 | Vollfilter-Prüfung | temporäres Audio prüfen | nein | B-03, B-01 |
| B-05 | Notiz bearbeiten | Titel/Text pflegen und KI verbessern | nein | B-01 |
| B-06 | Fokusdialog | Snapshot, Fokus, Profil, Webmodus, Modell | nein | B-01, B-09, B-08 |
| B-07 | Einstellungen | Theme, KI, Transkription, TTS, Backup, Über | nein | B-08, B-09, B-10, B-14 |
| B-08 | Codex Device-Code | Codex verbinden | nein | B-07, B-06 |
| B-09 | Profile | sechs Auswertungsprofile verwalten | nein | B-07, B-06 |
| B-10 | TTS und Stimmen | Anbieter, Stimme und Probe verwalten | nein | B-07, B-11 |
| B-11 | Qwen-Stimmklone | Klone anlegen und verwalten | nein | B-10 |
| B-12 | Globale Suche | Inhalte aller Sitzungen finden | nein | B-01 |
| B-13 | Archiv | archivierte Sitzungen suchen/wiederherstellen/löschen | nein | B-01, B-02 |
| B-14 | Sicherung | JSON exportieren/importieren | nein | B-07 |

### B-01 — Denkverlauf *(Startbildschirm)*

**Aufbau von oben nach unten:** 1. Kopfleiste mit Session-Schaltfläche außen, Sitzungstitel, Suchaktion und Einstellungen. Innen beginnt links B-02, rechts steht dieselbe Kopfleiste ohne Session-Schaltfläche. 2. Chatartig von unten wachsender Verlauf aus Notizkarten, Auswertungsgrenze und KI-Antwortkarten. 3. Über dem Composer erscheinen aktive Auftragsleisten für Aufnahme, Transkription, KI und TTS. 4. Composer mit wachsendem Textfeld, runder Aufnahme/Stop-Aktion, Senden „Notiz speichern“ und separater runder KI-Aktion.

**Bauteile:** Notizkarte zeigt Manrope-Titel, JetBrains-Mono-Zeitstempel, Source-Serif-Text und runde Aktionen für TTS/Mehr. KI-Antwortkarte zeigt „Auswertung“, Originaltext, optionale Quellen-Linkliste, Metadaten Profil · Modell · Reasoning · Webmodus sowie Warnchips für veränderte/gelöschte Grundlage. Die Grenze liegt zwischen altem und neuem Kontext als durchgehende Linie mit mittigem Label „Bis hier zuletzt ausgewertet“.

**Zustände:** Leer zeigt „Noch keine Denknotiz“ und „Tippe oder sprich deinen ersten Gedanken.“; Aufnahme zeigt Dauer, Stoppen und Abbrechen; Transkription zeigt Fortschritt; Auswertung zeigt Block n/von m oder „Gesamtauswertung“; offline zeigt nur an betroffenen Cloudaktionen ein Netzsymbol; TTS hebt genau den aktuellen Absatz hervor. Fehlerkarten tragen Fehlertext und „Erneut versuchen“. Pending Delete zeigt Snackbar mit „Rückgängig“ und Countdown ohne numerisch laufende Animation.

**Bedienelemente:** Session → B-02; Suche → B-12; Einstellungen → B-07; Aufnahme/Stop/Abbrechen → F-05; Speichern → F-07; KI → B-06; Notizmenü → F-08/F-09/B-05; KI-Menü → F-14; TTS → F-17.

### B-02 — Adaptive Sessionleiste

**Aufbau:** App-Name „Denknotiz“, breite Aktion „Neue Sitzung“, Suchfeld, Abschnitte „Angeheftet“ und „Zuletzt geändert“, darunter „Archiv“. Jede Zeile zeigt Titel, letzte Änderung und aktiven Zustand. Außen liegt dieselbe Struktur in einer 272-dp-Schublade über Scrim; innen dauerhaft 152 dp links, kompakter mit zweizeiligen Titeln und ohne Scrim.

**Zustände:** Keine Sitzungen erzeugt automatisch eine neue. Suche ohne Treffer zeigt „Keine Sitzung gefunden“. Aktive Sitzung nutzt gefülltes Symbol plus Text, Akzentfläche und 3-dp-Indikator. Archivierte Sitzungen stehen ausschließlich in B-13.

**Bedienelemente:** Tipp → F-03; Neue Sitzung → F-01; Mehr-Menü „Anheften/Lösen“, „Umbenennen“, „Archivieren“, „Löschen“ → F-02; Archiv → B-13; außen Scrim/Wischgeste schließt.

### B-03 — Diktat bestätigen

**Aufbau:** Bottom Sheet/Dialog mit Titel „Diktat prüfen“, Dauer und Filterstatus in Mono, editierbarer vollständiger Transkriptfläche, Audiostatus sowie Aktionen „Übernehmen“, „Erneut transkribieren“ und „Verwerfen“. „Übernehmen“ ist gefüllt, Verwerfen fehlerfarben nur als Textaktion.

**Zustände:** Transkribiert, transkribiert gerade, kein Netz, API-Fehler, Audio fehlt. Während Wiederholung bleibt der bisherige Text sichtbar, aber nicht übernehmbar.

**Bedienelemente:** Übernehmen → F-04 und Audio löschen; erneut → F-06; Verwerfen verlangt Bestätigung und → B-01; Vollfilter → B-04.

### B-04 — Vollfilter-Prüfung

**Aufbau:** Dialog mit Warnsymbol, Titel „Kein verlässlicher Text erkannt“, Erklärung aller vier angewandten Filter, Mono-Dauer/Dateigröße, große Audioleiste mit Start/Pause und Fortschritt sowie „Erneut transkribieren“ und „Aufnahme verwerfen“.

**Zustände:** Audio bereit/spielt/pausiert, Wiederholung läuft, Netzfehler. Keine Textvorschau wird erfunden.

**Bedienelemente:** Audio → lokale temporäre Wiedergabe; erneut → F-06/B-03; verwerfen nach Bestätigung → F-06/B-01.

### B-05 — Notiz bearbeiten

**Aufbau:** Kopfleiste „Notiz bearbeiten“ mit Abbrechen/Speichern; Eingabe „Titel“ und große Eingabe „Text“, unveränderlicher Zeitstempel; Abschnitt „Version“ mit „Mit KI verbessern“ oder „Original wiederherstellen“; unten „Notiz löschen“.

**Zustände:** Unverändert, geändert, KI arbeitet, Verbesserungsvorschau, offline, Konflikt durch parallele Änderung. Speichern ist nur bei nichtleerem Text aktiv.

**Bedienelemente:** Speichern → F-08; KI/Original → F-09; Löschen → F-08; Zurück bei Änderungen fragt „Änderungen verwerfen?“.

### B-06 — Fokusdialog

**Aufbau:** 28-dp-Dialog/Sheet. Oben „Neue Auswertung“ und `{n} neue Notizen`. Darunter fest in Source Serif 4: „Worauf soll ich mich konzentrieren?“, mehrzeilige Antwort, Auswahl „Profil“, Segmentwahl „Nur Notizen“ / „Notizen + Web“, kompakte Auswahl Modell Sol/Terra/Luna und Reasoning low/medium/high/xhigh/max, Snapshot-Hinweis und volle Aktion „Auswertung starten“.

**Zustände:** Bereit, kein neuer Kontext, offline, Codex getrennt, unfertige Transkriptionen, Snapshot wird fixiert. Ein aufklappbarer Bereich „Einbezogene Notizen“ listet Titel und Zeitstempel vollständig, aber bearbeitet nichts.

**Bedienelemente:** Profil → B-09; Codex verbinden → B-08; Start → F-10/F-11/F-12; Abbrechen schließt ohne Grenzänderung.

### B-07 — Einstellungen

**Aufbau:** Scrollseite mit Gruppen in Reihenfolge: „Erscheinung“ (vier Vorschaukacheln), „Codex“ (Verbindung, Modell, Reasoning), „Auswertungsprofile“, „Transkription“ (Groq-Key, Modellanzeige, Test), „Vorlesen“ (Anbieter/Stimme), „Sicherung“, „Berechtigungen“, „Über“ (Version/Bump-Zeitpunkt). Schlüssel sind verdeckt und nur per gedrückter Sichtaktion temporär lesbar.

**Zustände:** verbunden/getrennt, Schlüssel fehlt/ungeprüft/geprüft/fehlerhaft, Berechtigung erlaubt/abgelehnt, offline. Jede Gruppe erklärt lokal, welche Funktion betroffen ist.

**Bedienelemente:** Codex → B-08; Profile → B-09; TTS → B-10; Sicherung → B-14; Theme/Modelle/Keys → F-16/F-20/F-21; Berechtigung → Systemeinstellungen.

### B-08 — Codex Device-Code

**Aufbau:** Titel „Codex verbinden“, Erklärung, Verifizierungsadresse, Gerätecode in JetBrains Mono 30 sp, Restzeit, Aktionen „Im Browser öffnen“ und „Code kopieren“, Statuszeile „Warte auf Bestätigung …“.

**Zustände:** Code laden, wartet, bestätigt, abgelehnt, abgelaufen, offline. Erfolg zeigt „Verbunden“ und kehrt nach 800 ms zurück.

**Bedienelemente:** Browser/Kopieren/Neuer Code/Abbrechen → F-16.

### B-09 — Profile

**Aufbau:** Sechs 18-dp-Karten: Kurz, Normal, Ausführlich, Freies Profil 1–3. Jede zeigt Radioauswahl, Name, Promptvorschau und Bearbeiten. Editor als 28-dp-Sheet mit Name (nur frei editierbar bei freien Profilen), Prompt, „Zurücksetzen“, „Speichern“.

**Zustände:** aktiv, inaktiv, freies Profil leer, geändert, Reset-Bestätigung. Genau eine aktive Karte ist gefüllt und beschriftet.

**Bedienelemente:** Aktivieren/Bearbeiten/Reset/Speichern → F-15; Auswahl aus B-06 kehrt mit gewähltem Profil dorthin zurück, ohne global aktiv zwingend zu ändern.

### B-10 — TTS und Stimmen

**Aufbau:** Anbieter-Segmente Chirp/Edge/Qwen, jeweilige Stimmliste, Tempo 0,8×/1,0×/1,2×, Aktion „Probe hören“, Schlüsselstatus und bei Qwen „Stimmklone verwalten“. Eine laufende Probe zeigt Mediensteuerung mit Pause, Stopp, Absatz zurück/vor.

**Zustände:** bereit, Probe puffert/spielt/pausiert, Anbieter fehlt Schlüssel, offline, Wechselbestätigung.

**Bedienelemente:** Anbieter/Stimme/Probe → F-17; Qwen-Verwaltung → B-11.

### B-11 — Qwen-Stimmklone

**Aufbau:** Kopf „Qwen-Stimmklone“, Aktion „Neuen Stimmklon“, Liste vorhandener Klone mit Aktivstatus, Probe, Umbenennen, Löschen. Erstell-Sheet: Name, Aufnahme oder „Audiodatei wählen“, Qualitätsregeln, lokale Audiovorschau, „Stimme erstellen“.

**Zustände:** leer, Probe aufnehmen, Probe bereit, Upload/Fortschritt, Klon bereit, Fehler, Löschen ausstehend. Leerer Zustand erklärt 10–120 Sekunden klare Sprache.

**Bedienelemente:** Aufnahme/Datei/Anhören/Erstellen/Auswählen/Umbenennen/Löschen → F-18.

### B-12 — Globale Suche

**Aufbau:** fokussiertes Suchfeld in Kopfleiste, Filterchips „Alles“, „Sitzungen“, „Notizen“, „Auswertungen“, Ergebnisse gruppiert nach Sitzung. Treffer zeigt Typ-Icon, Titel, Mono-Zeit, Textausschnitt mit Akzentmarkierung.

**Zustände:** Eingabe fehlt, sucht lokal, Treffer, keine Treffer. Kein Netzstatus nötig.

**Bedienelemente:** Treffer → F-03/B-01; Zurück → vorheriger Scrollzustand.

### B-13 — Archiv

**Aufbau:** Titel „Archiv“, Suchfeld, nach letzter Änderung sortierte Sitzungszeilen. Jede Zeile bietet „Wiederherstellen“ und Mehr-Menü „Umbenennen“, „Löschen“.

**Zustände:** leer „Keine archivierten Sitzungen“, Treffer, pending delete mit Undo.

**Bedienelemente:** Öffnen/Wiederherstellen/Umbenennen/Löschen → F-02/F-03.

### B-14 — Sicherung

**Aufbau:** Warnkarte „JSON-Sicherungen sind unverschlüsselt“, letzte Exportinformation, Aktionen „Sicherung exportieren“ und „Sicherung importieren“. Importvorschau zeigt Schema, Datum, Sitzungs-/Notiz-/Antwortmengen, neue Einträge und lokale ID-Konflikte mit dem Satz „Bei gleicher ID gewinnt die lokale Version“.

**Zustände:** bereit, exportiert, Datei wird geprüft, Vorschau, Merge läuft, Erfolg, ungültige/neue Version, Schreibfehler.

**Bedienelemente:** Export/Dateiauswahl/Import bestätigen/Abbrechen → F-19.

## 7. Ikonografie und Bilder

**Verbindliche Übernahme der empfohlenen Lösung:** abgerundete 2-dp-Liniensymbole. Material Symbols Rounded dienen als Basissatz, 24 dp sichtbar in 48-dp-Tippflächen, Kartenaktionen 22 dp. Inaktive Navigation ist Linie; aktive Zustände sind **gefüllt und beschriftet**. Symbole ohne sichtbare Beschriftung erhalten eine deutsche Semantikbeschreibung.

| Zweck | Symbol |
|-------|--------|
| Sessionleiste | `side_navigation` / ersatzweise `menu` |
| Neue Sitzung | `add_circle` |
| Suche | `search` |
| Anheften | `keep` |
| Archiv | `archive` |
| Umbenennen/Bearbeiten | `edit` |
| Löschen | `delete` |
| Kopieren/Duplizieren | `content_copy` / `file_copy` |
| Aufnahme/Stop/Abbrechen | `mic` / `stop` / `close` |
| KI-Auswertung/Verbessern | `auto_awesome` / `auto_fix_high` |
| TTS/Pause | `volume_up` / `pause` |
| Absatz zurück/vor | `skip_previous` / `skip_next` |
| Grenze | `horizontal_rule` mit `check_circle` |
| Webmodus | `language` |
| Backup | `download` / `upload` |

Keine Fotos, dekorativen Illustrationen oder animierten Hintergrundbilder. Das App-Icon ist **abgeleitete technische Entscheidung:** eine einzelne abgerundete Denkspur-Linie, die in einer kleinen Notizkante endet, 2-dp-Anmutung, Gold auf dunkler Fläche.

## 8. Texte

| Ort | Fester Text |
|-----|-------------|
| B-01 | „Noch keine Denknotiz“ · „Tippe oder sprich deinen ersten Gedanken.“ · „Notiz schreiben …“ · „Notiz speichern“ |
| Grenze | „Bis hier zuletzt ausgewertet“ |
| B-02 | „Denknotiz“ · „Neue Sitzung“ · „Angeheftet“ · „Zuletzt geändert“ · „Archiv“ |
| Sitzungsmenü | „Anheften“ · „Lösen“ · „Umbenennen“ · „Archivieren“ · „Löschen“ |
| B-03 | „Diktat prüfen“ · „Übernehmen“ · „Erneut transkribieren“ · „Verwerfen“ |
| B-04 | „Kein verlässlicher Text erkannt“ · „Audio anhören“ · „Aufnahme verwerfen“ |
| B-05 | „Notiz bearbeiten“ · „Titel“ · „Text“ · „Mit KI verbessern“ · „Original wiederherstellen“ |
| B-06 | „Neue Auswertung“ · „Worauf soll ich mich konzentrieren?“ · „Nur Notizen“ · „Notizen + Web“ · „Auswertung starten“ |
| KI-Warnungen | „Grundlage nachträglich bearbeitet“ · „Grundlage teilweise gelöscht“ |
| B-07 | „Erscheinung“ · „Codex“ · „Auswertungsprofile“ · „Transkription“ · „Vorlesen“ · „Sicherung“ · „Berechtigungen“ · „Über“ |
| Themes | „Hell“ · „Dunkel“ · „Gold-Hell“ · „Gold-Dunkel“ |
| B-08 | „Codex verbinden“ · „Im Browser öffnen“ · „Code kopieren“ · „Warte auf Bestätigung …“ |
| B-09 | „Kurz“ · „Normal“ · „Ausführlich“ · „Freies Profil 1“ · „Freies Profil 2“ · „Freies Profil 3“ · „Zurücksetzen“ |
| B-10 | „Chirp“ · „Edge“ · „Qwen“ · „Probe hören“ · „Stimmklone verwalten“ |
| B-11 | „Qwen-Stimmklone“ · „Neuen Stimmklon“ · „Audiodatei wählen“ · „Stimme erstellen“ |
| B-12 | „Sitzungen und Inhalte durchsuchen …“ · „Alles“ · „Sitzungen“ · „Notizen“ · „Auswertungen“ |
| B-13 | „Keine archivierten Sitzungen“ · „Wiederherstellen“ |
| B-14 | „JSON-Sicherungen sind unverschlüsselt.“ · „Sicherung exportieren“ · „Sicherung importieren“ · „Bei gleicher ID gewinnt die lokale Version.“ |
| Allgemein | „Kein Netz“ · „Erneut versuchen“ · „Abbrechen“ · „Speichern“ · „Rückgängig“ · „Verwerfen“ |

Dynamische Platzhalter stehen in geschweiften Klammern: `{n}`, `{datum}`, `{dauer}`, `{modell}`, `{profil}`.

## 9. Barrierefreiheit

- Textkontrast mindestens 4,5:1, große Texte und Symbole mindestens 3:1; finale Implementierung prüft jede Theme-Rolle gegen ihren konkreten Hintergrund.
- Mindest-Tippfläche 48 × 48 dp. Aktionen unterscheiden sich nie nur durch Farbe, sondern zusätzlich durch Symbol, Text oder Form.
- Systemschrift bis 200 %: Karten/Dialoge wachsen, Metadaten umbrechen, Primäraktionen bleiben sichtbar; keine feste Texthöhe und kein Abschneiden.
- TalkBack-Reihenfolge folgt visuell von oben nach unten. Karten bündeln Titel, Zeit und Typ, bieten Aktionen danach einzeln an.
- Aufnahme, Fehler, Auswertungsgrenze und TTS-Absatz werden semantisch angekündigt; Dauerupdates höchstens alle 10 Sekunden, um TalkBack nicht zu überfluten.
- Animationen beachten §8 der Motion-Spec; haptische Rückmeldung bleibt unabhängig von Bewegung.

## 10. Offene Fragen

Keine. Alle nicht vom Nutzer einzeln benannten Kleindetails sind als abgeleitete technische Entscheidungen festgelegt.
