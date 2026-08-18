# Denknotiz — Spec v1
Stand: 18.08.2026 · Stufe: v1 · Plattform(en): Android

Plattform: Android · Zielgerät: Galaxy Z Fold 8 außen und innen · Sprache der Oberfläche: Deutsch  
Herkunft: `Specs/Denknotiz/v1/` · Erzeugt als direkte Baugrundlage

**Werft-Designer auf aktuelle Nutzeranweisung übersprungen. v1 ist direkte Baugrundlage ohne Messpaket.** Alle exakten Werte sind verbindliche Bauwerte; markierte Kleindetails sind abgeleitete technische Entscheidungen. Dieses Dokument ist allein verständlich und setzt keine weitere Datei voraus.

## 0. Was dieses Programm ist — in drei Sätzen

Denknotiz ist Franks privater, chatartig organisierter Denkraum für getippte und diktierte Notizen in thematischen Sitzungen. Eine persistente sichtbare Auswertungsgrenze trennt bereits ausgewertete von neuen Notizen; Codex verarbeitet einen festen Snapshot der neuen Notizen anhand der lokalen Fokusfrage „Worauf soll ich mich konzentrieren?“, ohne stille Kürzung oder erneute Aufnahme alter Inhalte. Aufnahme, Groq-Transkription, KI, TTS, JSON-Sicherung und Fold-Wechsel bilden einen durchgängigen, teilweise offline nutzbaren Arbeitsfluss.

## 1. Bauauftrag

Jeder Bildschirm aus Teil B §6 ist in allen vier Erscheinungen vollständig in Kotlin und Jetpack Compose zu bauen. Verhalten, Daten, Fehler und Grenzen aus Teil A, jede Bewegung aus Teil C sowie alle Abnahmekriterien aus Teil D sind verbindlich; kein sichtbares Bedienelement darf ohne zugeordnete Funktion bleiben. Für den direkten Bau gelten die Werte dieser v1, weil kein Werft-Design und kein Messpaket folgen.

---

## Teil A — Funktions-Spec

### A1. Funktionsübersicht

| Kennung | Funktion | Bildschirm(e) |
|---------|----------|---------------|
| F-01 | Sitzungen anlegen und automatisch betiteln | B-01, B-02 |
| F-02 | Sitzungen organisieren und wiederherstellen | B-02, B-13 |
| F-03 | Sitzung wechseln und durchsuchen | B-01, B-02, B-12 |
| F-04 | Entwurf tippen, diktieren und bestätigen | B-01, B-03 |
| F-05 | Aufnahme im Vorder- und Hintergrund | B-01, B-03 |
| F-06 | Groq-Transkription mit vier Cortex-Filtern | B-03, B-04 |
| F-07 | Notiz speichern und betiteln | B-01 |
| F-08 | Notizen bearbeiten, kopieren, duplizieren und löschen | B-01, B-05 |
| F-09 | Notiztext per KI verbessern und Original wiederherstellen | B-01, B-05 |
| F-10 | Auswertungsgrenze und Snapshot bilden | B-01, B-06 |
| F-11 | Fokus, Profil und Webmodus wählen | B-06 |
| F-12 | KI-Auswertung vollständig und wiederholbar ausführen | B-01, B-06 |
| F-13 | Alte Auswertungsgrundlagen markieren | B-01 |
| F-14 | KI-Antwort verwenden und löschen | B-01 |
| F-15 | Auswertungsprofile verwalten | B-07, B-09 |
| F-16 | Codex verbinden, Modell und Reasoning wählen | B-07, B-08 |
| F-17 | TTS wiedergeben und Anbieter wechseln | B-01, B-10 |
| F-18 | Qwen-Stimmklone verwalten | B-10, B-11 |
| F-19 | JSON-Sicherung exportieren und mergen | B-07, B-14 |
| F-20 | Themes und Einstellungen verwalten | B-07 |
| F-21 | Offline-, Fehler- und Geheimnisbehandlung | B-01, B-07 |
| F-22 | Fold-, Prozess- und Hintergrundzustand erhalten | alle |

### A2. Funktionen im Einzelnen

#### F-01 — Sitzungen anlegen und automatisch betiteln
**Auslöser:** „Neue Sitzung“. **Ablauf:** Room erzeugt sofort eine Sitzung mit UUID, „Neue Sitzung“ und Änderungszeit. Nach der ersten Notiz entsteht lokal ein Titel aus höchstens sechs sinntragenden Wörtern; online wird einmalig ein Codex-Titel versucht. **Daten/Ergebnis:** `Sitzung` wird gespeichert und geöffnet. **Fehler:** KI-Fehler belässt den lokalen Titel und wird nicht automatisch wiederholt. **Regeln:** höchstens 60 Zeichen; manuelle Titel werden nie überschrieben. UUID v4 ist abgeleitete technische Entscheidung.

#### F-02 — Sitzungen organisieren und wiederherstellen
**Auslöser:** Sitzungsmenü/Wischaktion. **Ablauf:** Anheften/Lösen, Umbenennen, Archivieren und Löschen nach Bestätigung; Löschen setzt acht Sekunden `pendingDelete`, Undo stellt alles wieder her. **Daten/Ergebnis:** Sitzung und abhängige Inhalte werden transaktional geändert. **Fehler:** Nach Prozessende entscheidet die persistente Frist über Wiederherstellung oder endgültige Löschung. **Regeln:** angeheftet zuerst, dann `geaendertAm` absteigend; nach Löschung der letzten aktiven Sitzung entsteht automatisch eine neue.

#### F-03 — Sitzung wechseln und durchsuchen
**Auslöser:** Sitzung oder Suchtreffer. **Ablauf:** Room-FTS durchsucht ab zwei Zeichen Sitzungstitel, Notiztitel/-texte und KI-Antworten; Treffer öffnet Sitzung und Zielkarte. **Daten/Ergebnis:** letzter Zugriff und Scrollanker werden gespeichert. **Fehler:** gelöschter Treffer verschwindet. **Regeln:** offline, tolerant gegenüber Groß-/Kleinschreibung und Diakritika; Hintergrundaufträge bleiben ihrer Ursprungssitzung zugeordnet.

#### F-04 — Entwurf tippen, diktieren und bestätigen
**Auslöser:** Texteingabe oder Diktat. **Ablauf:** sitzungsbezogener Entwurf wird laufend persistiert; mehrere bestätigte Diktate werden mit zwei Zeilenumbrüchen an denselben Entwurf angehängt; B-03 verlangt vor Übernahme eine Bestätigung; gespeichert wird erst mit „Notiz speichern“. **Daten/Ergebnis:** `Entwurf` und temporäre `Diktat`-Einträge. **Fehler:** App-Abbruch erhält den Entwurf. **Regeln:** keine Zeichenbegrenzung; Diktattext nie ohne sichtbare Bestätigung speichern.

#### F-05 — Aufnahme im Vorder- und Hintergrund
**Auslöser:** erster Tipp startet, zweiter stoppt; Abbrechen verwirft. **Ablauf:** bedarfsgerechte Mikrofon-/Notification-Berechtigung, Mikrofon-Foreground-Service, 16-kHz-Mono-WAV, Warnung bei 09:30, automatischer Stop bei 10:00, Notification mit Stop/Abbrechen. **Daten/Ergebnis:** temporäre WAV und Diktatstatus gehen zu F-06. **Fehler:** Rechte, Speicher und Audiofokus werden klar gemeldet; bei Anruf wird der bisherige Inhalt gestoppt und zur Bestätigung geführt. **Regeln:** genau eine Aufnahme, mindestens 0,4 Sekunden; Aufnahme pausiert TTS.

#### F-06 — Groq-Transkription mit vier Cortex-Filtern
**Auslöser:** gestoppte Aufnahme oder Retry. **Ablauf:** zwingend 1. RMS-Sprachprüfung vor API, 2. Groq `whisper-large-v3-turbo` Deutsch mit Segmentdaten, 3. Confidence/Repetition/Mini-Noise-Filter, 4. Segment-vs-Audiofenster-Prüfung, 5. Floskelblocklist. Verbleibender Text wird in B-03 editiert/bestätigt. Bei Vollfilter zeigt B-04 Audio anhören, erneut transkribieren oder verwerfen. **Daten/Ergebnis:** Transkript und Filterstatus; nach Übernahme/Verwerfen wird Rohaufnahme gelöscht. **Fehler:** ohne Netz/Key bleibt Audio temporär mit Retry/Verwerfen; nach drei automatischen Versuchen nur manuell. **Regeln:** alle vier Filter unabschaltbar; höchstens zwei parallele FIFO-Transkriptionen; unbestätigtes Audio nach sieben Tagen bereinigen.

#### F-07 — Notiz speichern und betiteln
**Auslöser:** „Notiz speichern“. **Ablauf:** atomar Notiz mit Zeitstempel und sofortigem lokalem Titel speichern, danach Entwurf leeren; Codex darf genau einmal einen Titel liefern, sofern Titel nicht manuell/geändert. **Daten/Ergebnis:** `Notiz`, leerer Entwurf. **Fehler:** Room-Fehler erhält Entwurf; KI-Fehler belässt lokalen Titel. **Regeln:** Titel höchstens 80 Zeichen; manuell nie überschreiben; einmaliger KI-Versuch bleibt auch nach Neustart einmalig.

#### F-08 — Notizen bearbeiten, kopieren, duplizieren und löschen
**Auslöser:** Notizmenü/B-05. **Ablauf:** Titel/Text bearbeiten, Titel als manuell markieren; Titel+Text kopieren; Duplikat mit neuer UUID und Zeit direkt hinter Original; Löschen mit acht Sekunden Undo. **Daten/Ergebnis:** Notiz/Änderungsereignis. **Fehler:** leeren Text nicht speichern, Kopierfehler melden. **Regeln:** Erstellzeit bleibt, `geaendertAm` ändert sich; F-13 aktualisiert alte Grundlagen.

#### F-09 — Notiztext per KI verbessern und Original wiederherstellen
**Auslöser:** „Mit KI verbessern“. **Ablauf:** Codex erhält festen Notizsnapshot und darf nur Sprache/Absätze verbessern, nichts hinzufügen/entfernen; vorherige Fassung wird als Revision gespeichert; Original lässt sich exakt wiederherstellen. **Daten/Ergebnis:** `NotizRevision` und neuer Text. **Fehler:** offline, Abbruch, unvollständige oder bei Paralleländerung konfliktbehaftete Antwort überschreibt nichts. **Regeln:** nur vollständige Finalantwort übernehmen; Titel unberührt.

#### F-10 — Auswertungsgrenze und Snapshot bilden
**Auslöser:** Fokusdialog/Absenden. **Ablauf:** persistente Linie „Bis hier zuletzt ausgewertet“ liegt bei letzter erfolgreicher Snapshot-Notiz; nur fertige, nicht gelöschte Notizen dahinter werden in stabiler Reihenfolge mit ID, Titel, Volltext, Zeit, Position und Hash kopiert. **Daten/Ergebnis:** unveränderlicher `AuswertungsSnapshot`. **Fehler:** ohne neue Notizen kein Start; unfertige Transkriptionen werden genannt. **Regeln:** Fokusdialog, Fehler, Abbruch, spätere Änderungen oder alte bearbeitete Notizen verschieben/ändern Snapshot und Grenze nicht.

#### F-11 — Fokus, Profil und Webmodus wählen
**Auslöser:** KI-Aktion. **Ablauf:** feste lokale Frage „Worauf soll ich mich konzentrieren?“, Franks Antwort, eines von sechs Profilen, „Nur Notizen“ oder „Notizen + Web“, Modell und Reasoning wählen. **Daten/Ergebnis:** Parameter werden im Snapshot fixiert. **Fehler:** leerer Fokus/Prompt sperrt Start, offline lässt Ausfüllen, nicht Absenden zu. **Regeln:** Frage selbst unveränderlich; Webmodus nur für diesen Auftrag.

#### F-12 — KI-Auswertung vollständig und wiederholbar ausführen
**Auslöser:** Absenden/„Exakt wiederholen“. **Ablauf:** Hintergrundauftrag mit Notification; passt Kontext nicht ins Modelllimit, ausschließlich an Notizgrenzen teilen, jede Notiz vollständig blockweise auswerten, danach aus allen Zwischenresultaten plus vollständigem Inventar Gesamtauswertung. Finale Absätze haben 4–8 Sätze, kurze Antworten dürfen kleiner sein. Webmodus liefert Links nur als Liste am Ende. Erst validierte finale Antwort speichert `KiAntwort` und verschiebt Grenze atomar. **Daten/Ergebnis:** Snapshot, Blockstatus, unveränderliche Finalantwort. **Fehler:** Netz/Auth/Rate-Limit/Limit/Prozess/Validierung lassen Grenze unverändert; unvollständiges Streaming verwerfen; einzelne zu große Notiz offen melden. **Regeln:** keine stille Kürzung; Retry exakt gleicher Snapshot, Profiltext, Modell, Reasoning und Webmodus.

#### F-13 — Alte Auswertungsgrundlagen markieren
**Auslöser:** alte Snapshot-Notiz bearbeiten/verbessern/wiederherstellen/löschen. **Ablauf:** Hashvergleich setzt „Grundlage nachträglich bearbeitet“ oder „Grundlage teilweise gelöscht“; exaktes Undo entfernt passenden Marker. **Daten/Ergebnis:** abgeleiteter Antwortstatus. **Fehler:** Reparatur beim nächsten Start. **Regeln:** Antwort/ Grenze unverändert, alte Notiz nie erneut aufnehmen.

#### F-14 — KI-Antwort verwenden und löschen
**Auslöser:** Antwortmenü. **Ablauf:** Original kopieren, per F-17 vorlesen, identisch als neue Notiz hinter Grenze übernehmen oder mit acht Sekunden Undo löschen. **Daten/Ergebnis:** neue Notiz oder Löschmarke. **Fehler:** fehlgeschlagene Aktion verändert Antwort nicht. **Regeln:** Originaltext unveränderlich; Antwortlöschung setzt Grenze nicht zurück.

#### F-15 — Auswertungsprofile verwalten
**Auslöser:** B-09. **Ablauf:** genau sechs Profile; Kurz/Normal/Ausführlich mit festen Namen, editierbaren Prompts und Reset; drei freie mit Name+Prompt und Reset; Aktivierung transaktional exklusiv. **Daten/Ergebnis:** `Auswertungsprofil`. **Fehler:** leeres Profil nicht aktivierbar. **Regeln:** genau eines aktiv, Standard Normal. Prompts: Kurz „Antworte knapp, priorisiere das Wesentliche und nenne konkrete nächste Schritte.“; Normal „Ordne die Notizen, erkenne Zusammenhänge, beantworte den Fokus ausgewogen und konkret.“; Ausführlich „Analysiere gründlich, benenne Muster, Spannungen, Alternativen, Risiken und konkrete nächste Schritte.“ Snapshot bewahrt die verwendete Fassung.

#### F-16 — Codex verbinden, Modell und Reasoning wählen
**Auslöser:** B-08/Cloudfunktion. **Ablauf:** Device-Code Auth mit Polling, Refresh, Fehlern und sicherer Ablage exakt wie PerfectMoment; Modelle GPT-5.6 Sol/Terra/Luna; Reasoning low/medium/high/xhigh/max. **Daten/Ergebnis:** Tokens verschlüsselt, Auswahl lokal. **Fehler:** Ablauf/Ablehnung/Netz speichert keine halbe Auth; Trennen löscht Tokens. **Regeln:** Standard Terra/medium; Ablauf nicht neu erfinden.

#### F-17 — TTS wiedergeben und Anbieter wechseln
**Auslöser:** TTS-Aktion/B-10. **Ablauf:** Absatzteilung, nächstes Zwei-Absatz-Prefetch wie Cortex/Experimente, Start/Pause, Stopp, Absatz zurück/vor, Absatzhighlight und MediaSession-Notification im Hintergrund. Chirp, Edge und Qwen. Wechsel während Wiedergabe nur nach Bestätigung; danach aktuellen Absatz neu starten. **Daten/Ergebnis:** Cache und TTS-Position. **Fehler:** Anbieter/Netz/Key pausiert mit Retry/Wechsel; Prefetchfehler stoppt aktuellen Absatz nicht. **Regeln:** global eine Wiedergabe; Stopp löscht Cache/Position, Pause erhält; Aufnahme pausiert TTS.

#### F-18 — Qwen-Stimmklone verwalten
**Auslöser:** B-11. **Ablauf:** 10–120-Sekunden-Probe aufnehmen/auswählen, anhören, benennen, hochladen, testen, auswählen, umbenennen und lokal/remote löschen. **Daten/Ergebnis:** Probe temporär, Key/Voice-ID verschlüsselt, Name lokal. **Fehler:** ungültige Probe/Upload hinterlässt keinen halben Klon; Remote-Löschfehler bleibt wiederholbar. **Regeln:** Rohprobe nach Erfolg/Verwerfen löschen, nie sichern; 10–120 Sekunden ist abgeleitete technische Entscheidung.

#### F-19 — JSON-Sicherung exportieren und mergen
**Auslöser:** B-14. **Ablauf:** unverschlüsseltes UTF-8-JSON über SAF mit Format, `schemaVersion=1`, Zeit und zulässigen Room-Daten; Import validiert, zeigt Vorschau, merged transaktional; bei gleicher Entitätsart+ID gewinnt lokal. **Daten/Ergebnis:** geräteübergreifendes Backup. **Fehler:** ungültig/neuer/Referenz-/Schreibfehler verändert nichts. **Regeln:** keine Keys, Tokens, Audio, Sprachproben, Voice-IDs oder TTS-Caches; Dateiname `Denknotiz-Backup-YYYYMMDD-HHmm-v1.json`.

#### F-20 — Themes und Einstellungen verwalten
**Auslöser:** B-07. **Ablauf:** Theme, Codex, Profile, Groq, TTS, Backup, Berechtigungen und Über gruppiert; Theme sofort, Secrets verschlüsselt, Diensttest ohne Nutzdaten. **Daten/Ergebnis:** Konfiguration persistent. **Fehler:** ungültiger Schlüssel bleibt ungeprüft. **Regeln:** Hell, Dunkel, Gold-Hell, Gold-Dunkel; Standard Gold-Dunkel, kein Systemtheme; Version+Bump-Zeit anzeigen.

#### F-21 — Offline-, Fehler- und Geheimnisbehandlung
**Auslöser:** Netz-/Dienststatus. **Ablauf:** lokale Anzeige/Bearbeitung/Tippnotizen/Suche/Backup immer; Cloudaktionen melden „Kein Netz“ und erhalten Eingaben; Secrets Keystore-verschlüsselt und logsicher redigiert. **Daten/Ergebnis:** sichere lokale Arbeit. **Fehler:** Keystore-Verlust verlangt nur Dienst-Neuverbindung; HTTP-Fehler nach Auth, Rate-Limit, Server, Timeout, Netz unterscheiden. **Regeln:** keine automatische Cloudübertragung; keine Secrets in Backup, Room, Logs oder Notifications.

#### F-22 — Fold-, Prozess- und Hintergrundzustand erhalten
**Auslöser:** Fold, Größe, Rotation, Hintergrund, Neustart. **Ablauf:** Sitzung, Entwurf, stabiler Scrollanker+Offset, Aufnahme, Transkription, Fokusdialog, Auswertung und TTS liegen außerhalb Composables; Services/WorkManager/MediaSession und idempotente IDs verhindern Doppelstarts. **Daten/Ergebnis:** Zustand wird rekonstruiert. **Fehler:** abgebrochene Jobs werden wiederaufgenommen oder als retrybar markiert; nicht mehr aktive Aufnahme wird gestoppt angeboten. **Regeln:** Fold löst keinen Auftrag neu aus.

### A3. Datenmodell

Alle IDs sind UUID-v4-Strings, Zeitwerte UTC-Epoch-Millis.

| Einheit | Pflichtfelder und Speicherung |
|---------|-------------------------------|
| `Sitzung` | `id`, `titel`, `titelManuell`, `kiTitelVersucht`, `erstelltAm`, `geaendertAm`, `istAngeheftet`, `archiviertAm?`, `auswertungsGrenzeNotizId?`, `pendingDeleteBis?` in Room |
| `Notiz` | `id`, `sitzungId`, `titel`, `text`, `titelManuell`, `kiTitelVersucht`, `quelle`, `erstelltAm`, `geaendertAm`, `sortierPosition`, `duplikatVonId?`, `pendingDeleteBis?` in Room |
| `Entwurf` | `sitzungId`, `text`, `hatDiktat`, `geaendertAm` in Room |
| `Diktat` | `id`, `sitzungId`, `audioPfad?`, `transkript?`, `filterStatus`, `zustand`, `dauerMs`; fachlich Room, Audio temporäres privates Dateisystem |
| `NotizRevision` | `id`, `notizId`, `text`, `grund`, `erstelltAm` in Room |
| `AuswertungsSnapshot` | `id`, `sitzungId`, `fokusAntwort`, `profilName`, `profilPrompt`, `modell`, `reasoning`, `webmodus`, `erstelltAm`, `status` in Room |
| `SnapshotElement` | `snapshotId`, `notizId`, `titel`, vollständiger `text`, `inhaltHash`, `erstelltAm`, `sortierPosition` in Room |
| `KiAntwort` | `id`, `sitzungId`, `snapshotId`, `textOriginal`, `quellenLinks`, `erstelltAm`, Grundlagenmarker, `pendingDeleteBis?` in Room |
| `Auswertungsprofil` | ID 1–6, Typ fest/frei, `name`, `prompt`, `aktiv` in Room |
| Einstellungen | Theme, Terra/medium, Edge-Anbieter, offene Sitzung in DataStore; Scroll-ID+Offset in Room |
| Geheimnisse | ausschließlich Keystore-gestützter verschlüsselter Speicher |

### A4. Zustände

| Einheit | Übergang |
|---------|----------|
| Aufnahme | `bereit → aufnehmend → gestoppt → transkribiert`; Abbrechen → `verworfen`; 10:00 → automatisch gestoppt |
| Transkription | `wartet → sendet → filtert → bestaetigung`; Fehler → `wiederholbar`; Vollfilter → `vollfilterPruefung`; Bestätigung → `uebernommen` + Audio gelöscht |
| Löschen | `sichtbar → pendingDelete(8 s) → geloescht`; Undo → sichtbar |
| Auswertung | `dialog → snapshotFixiert → wartet → blockVerarbeitung → gesamtauswertung → validierung → erfolgreich`; Fehler → wiederholbar, Grenze unverändert |
| TTS | `gestoppt → puffert → spielt ↔ pausiert`; Stopp leert Zustand |
| Auth | `getrennt → codeWirdGeholt → wartetBestaetigung → verbunden`; Fehler/Ablauf → erneuerbar |

### A5. Externe Dienste und Lebenszyklus

| Dienst | Zweck | Anmeldung | Offline |
|--------|-------|-----------|---------|
| Groq | Whisper-Transkription | verschlüsselter API-Key | Audio temporär behalten, Retry/Verwerfen |
| Codex | Titel, Verbesserung, Auswertung | Device-Code exakt wie PerfectMoment | lokale Daten bleiben, klarer Fehler |
| Edge/Chirp/Qwen | TTS | Anbieter-Key verschlüsselt, soweit nötig | pausieren, Retry/Wechsel |
| Qwen | Stimmklonen | Key+Voice-ID verschlüsselt | Verwaltung lesbar, Cloudaktion gesperrt |

Aufnahme läuft per Mikrofon-Foreground-Service weiter, KI/Transkription per persistentem Hintergrundauftrag mit Notification, TTS per MediaSession mit Mediennotification. Fold/Rotation erhalten alle Zustände und starten nichts doppelt. Offene Fragen: keine.

---

## Teil B — UI-Spec

### B1. Grundhaltung

„Ruhige futuristische Werkbank“: klare tiefe Flächen, 1-dp-Lichtkanten, sparsame Akzente, keine Neonüberladung. Manrope für UI, Source Serif 4 für Inhalte, JetBrains Mono für Metadaten. Außen kompakt einspaltig, innen mit dauerhafter 152-dp-Sessionleiste.

### B2. Themes

| Rolle | Gold-Dunkel (Standard) | Gold-Hell | Dunkel | Hell |
|-------|------------------------|-----------|--------|------|
| `background` | `#0B0A08` | `#F7F3E8` | `#090C10` | `#F3F6F8` |
| `surface` | `#14120E` | `#FFFDF7` | `#11161C` | `#FFFFFF` |
| `surfaceRaised` | `#1C1912` | `#FFFFFF` | `#19212A` | `#FFFFFF` |
| `surfaceInput` | `#11100D` | `#F1EBDD` | `#0D1217` | `#EAF0F3` |
| `outline` | `#3D3728` | `#D5C9AE` | `#2D3946` | `#CBD6DC` |
| `lightEdge` | `rgba(255,244,205,0.18)` | `rgba(255,255,255,0.88)` | `rgba(220,238,255,0.16)` | `rgba(255,255,255,0.96)` |
| `accent` | `#D8B65A` | `#8B6A16` | `#6EB7D8` | `#176B89` |
| `accentStrong` | `#F0D487` | `#684B00` | `#A6DDF2` | `#0D4E68` |
| `accentSoft` | `rgba(216,182,90,0.14)` | `rgba(139,106,22,0.12)` | `rgba(110,183,216,0.14)` | `rgba(23,107,137,0.11)` |
| `textPrimary` | `#F4F0E5` | `#241F15` | `#EDF3F7` | `#172127` |
| `textSecondary` | `#B9B09B` | `#5F5748` | `#AAB8C3` | `#4F606A` |
| `textMuted` | `#817A69` | `#817765` | `#748390` | `#71828C` |
| `onAccent` | `#171208` | `#FFFFFF` | `#071318` | `#FFFFFF` |
| `success` | `#87B887` | `#4F7A51` | `#70B99A` | `#39765F` |
| `warning` | `#D7A35E` | `#8C5D18` | `#D2A65A` | `#8C651F` |
| `error` | `#E47770` | `#A83F39` | `#E36F77` | `#A43E47` |
| `scrim` | `rgba(0,0,0,0.62)` | `rgba(25,20,12,0.42)` | `rgba(0,0,0,0.64)` | `rgba(16,27,33,0.40)` |
| `ttsHighlight` | `#332C18` | `#EEE1BC` | `#16313D` | `#D9ECF3` |

### B3. Typografie

Lokale Ressourcen: Manrope 400/500/600/700, Source Serif 4 400/500/600 + Italic 400, JetBrains Mono 400/500/600.

| Rolle | Familie | Größe/Gewicht/Zeilenhöhe/Laufweite |
|-------|---------|------------------------------------|
| App-/Bildschirmtitel | Manrope | 22 sp / 700 / 28 sp / −0,2 sp |
| Sitzungstitel | Manrope | 16 / 600 / 21 / 0 |
| Kartenüberschrift | Manrope | 15 / 600 / 20 / 0 |
| Notiztext | Source Serif 4 | 17 / 400 / 27 / 0 |
| KI-Antwort | Source Serif 4 | 17 / 400 / 28 / 0 |
| Fokusfrage | Source Serif 4 | 20 / 500 / 29 / 0 |
| Eingabe | Source Serif 4 | 17 / 400 / 26 / 0 |
| Primärknopf | Manrope | 14 / 700 / 20 / +0,1 |
| Hilfstext | Manrope | 13 / 400 / 18 / 0 |
| Metadaten | JetBrains Mono | 11 / 500 / 16 / +0,2 |
| Gerätecode | JetBrains Mono | 30 / 600 / 38 / +2,4 |

### B4. Maße, Formen und Tiefe

Grundraster 4 dp. Außen/innen: Seitenrand 12/20 dp; Kopfleiste 56/60; Kartenabstand 10/14; Kartenpadding 14/18; Composer 64–176/68–196; Hauptaktion 56/60; Sessionleiste 272 dp Schublade außen, 152 dp dauerhaft innen; Dialog außen Fenster−24, innen 400 max.; Tippfläche mindestens 48×48. Kartenradius 22 dp, Eingaben 16 dp, Dialoge 28 dp, Aktionen vollrund, überall 1-dp-Lichtkante. Karten: 1 dp Outline plus Lichtkante, Schatten dunkel 0/8/24 `rgba(0,0,0,0.30)`, hell 0/5/18 `rgba(21,33,40,0.10)`. Keine Glasunschärfe, Parallaxe oder kippenden Karten. Unter 420 dp Fensterbreite wird die persistente Leiste zur Schublade; dies ist abgeleitete technische Entscheidung.

### B5. Bildschirme

| ID | Bildschirm | Kernaufbau und Zustände/Aktionen |
|----|------------|----------------------------------|
| B-01 | Denkverlauf, Start | Kopf mit Session/Titel/Suche/Einstellungen; chatartiger Verlauf aus Notiz, Grenze „Bis hier zuletzt ausgewertet“, unveränderlicher KI-Antwort; Auftragsleisten; Composer mit Text, Aufnahme/Stop, Speichern, KI. Leer-, Aufnahme-, Transkriptions-, KI-, Offline-, Fehler-, Undo- und TTS-Zustände. |
| B-02 | Adaptive Sessionleiste | Name, Neue Sitzung, Suche, Angeheftet, Zuletzt geändert, Archiv; außen 272-dp-Schublade, innen 152 dp dauerhaft. Sitzung wechseln, anheften, umbenennen, archivieren, löschen. |
| B-03 | Diktat bestätigen | Sheet „Diktat prüfen“, Dauer/Filterstatus, editierbares Volltranskript, Übernehmen, erneut transkribieren, verwerfen; Laden/Netz/API/Audiofehler. |
| B-04 | Vollfilter-Prüfung | „Kein verlässlicher Text erkannt“, Erklärung der vier Filter, lokale Audioleiste, erneut/verwerfen; bereit, spielt, pausiert, Netzfehler. |
| B-05 | Notiz bearbeiten | Titel, Text, Zeit, KI verbessern/Original, löschen; unverändert/geändert/KI/Vorschau/offline/Konflikt. |
| B-06 | Fokusdialog | „Neue Auswertung“, feste Fokusfrage, Antwort, Profil, Nur Notizen/Notizen+Web, Sol/Terra/Luna, low–max, Snapshotliste, Start; kein Kontext/offline/Auth/unfertige Transkription. |
| B-07 | Einstellungen | Erscheinung, Codex, Profile, Transkription, Vorlesen, Sicherung, Berechtigungen, Über; Keys verdeckt, Zustände verbunden/geprüft/fehlerhaft/offline. |
| B-08 | Codex Device-Code | Adresse, Mono-Code, Restzeit, Browser, Kopieren, Status; laden/warten/erfolgreich/abgelehnt/abgelaufen/offline. |
| B-09 | Profile | sechs Karten, exklusiv aktiv, Name/Prompt, Editor/Reset; feste Namen bei Kurz/Normal/Ausführlich, drei freie. |
| B-10 | TTS und Stimmen | Chirp/Edge/Qwen, Stimmen, Tempo 0,8/1,0/1,2, Probe, Mediensteuerung, Qwen-Verwaltung; puffert/spielt/pausiert/Key/offline/Wechselbestätigung. |
| B-11 | Qwen-Stimmklone | Klonliste und Neu-Sheet mit Name, Aufnahme/Datei, Regeln, Anhören, Erstellen; leer/aufnehmen/bereit/upload/Fehler/Löschen ausstehend. |
| B-12 | Globale Suche | fokussierte Suche, Chips Alles/Sitzungen/Notizen/Auswertungen, gruppierte Treffer; leer/sucht/Treffer/keine. |
| B-13 | Archiv | Suche, sortierte Sitzungen, Wiederherstellen/Umbenennen/Löschen; leer/Treffer/Undo. |
| B-14 | Sicherung | Warnung unverschlüsselt, Export/Import, Vorschau mit Schema/Mengen/Konflikten und „lokal gewinnt“; prüfen/merge/Erfolg/Fehler. |

### B6. Ikonografie, Texte und Barrierefreiheit

Material Symbols Rounded, abgerundete 2-dp-Liniensymbole, 24 dp in 48-dp-Fläche; Karten 22 dp. Inaktiv Linie, aktiv **gefüllt und beschriftet**. Kernsymbole: `menu/side_navigation`, `add_circle`, `search`, `keep`, `archive`, `edit`, `delete`, `content_copy`, `mic`, `stop`, `close`, `auto_awesome`, `auto_fix_high`, `volume_up`, `pause`, `skip_previous`, `skip_next`, `language`, `download`, `upload`. Keine Fotos/Illustrationen. App-Icon: abgerundete Denkspur-Linie endet in Notizkante, Gold auf dunkel (abgeleitete Entscheidung).

Verbindliche Texte umfassen: „Denknotiz“, „Neue Sitzung“, „Notiz schreiben …“, „Notiz speichern“, „Bis hier zuletzt ausgewertet“, „Diktat prüfen“, „Kein verlässlicher Text erkannt“, „Worauf soll ich mich konzentrieren?“, „Nur Notizen“, „Notizen + Web“, „Auswertung starten“, „Grundlage nachträglich bearbeitet“, „Grundlage teilweise gelöscht“, „Codex verbinden“, „Kurz“, „Normal“, „Ausführlich“, „Chirp“, „Edge“, „Qwen“, „JSON-Sicherungen sind unverschlüsselt.“, „Bei gleicher ID gewinnt die lokale Version.“, „Kein Netz“, „Erneut versuchen“, „Rückgängig“.

Kontrast mindestens 4,5:1 für Text und 3:1 für große Inhalte; Tippfläche 48 dp; Bedeutung nie nur Farbe; Systemschrift bis 200 % ohne Abschneiden; TalkBack-Reihenfolge visuell, deutsche Beschreibungen und sparsame Statusansagen. Offene Fragen: keine.

---

## Teil C — Motion-Spec

### C1. Haltung und Kurven

Ruhig, präzise, ursächlich. Kein Buchstabenflug, Kartenwackeln, Parallax, Rotation oder Bewegung beim bloßen Scrollen.

| Name | Dauer | Kurve | Zweck |
|------|-------|-------|-------|
| `standard` | 240 ms | `cubic-bezier(0.2,0,0,1)` | Eintritt/Zustand/Farbe |
| `exit` | 180 ms | `cubic-bezier(0.4,0,1,1)` | Verlassen |
| `foldLayout` | 360 ms | `cubic-bezier(0.2,0,0,1)` | Fold-Layout |
| `sidebarIn` | 300 ms | `cubic-bezier(0.2,0,0,1)` | Sidebar öffnen |
| `sidebarOut` | 220 ms | `cubic-bezier(0.4,0,1,1)` | Sidebar schließen |
| `press` | 80 ms | `cubic-bezier(0.2,0,0,1)` | Scale 0,97 |
| `recordPulse` | 1600 ms | `cubic-bezier(0.4,0,0.6,1)` | Aufnahme subtil |
| `noteIn` | 260 ms | `cubic-bezier(0.2,0,0,1)` | 10 dp + Fade |
| `ttsHighlight` | 160 ms | `cubic-bezier(0.2,0,0,1)` | Absatzhighlight |
| `reducedFadeShort/Long` | 100/160 ms | `cubic-bezier(0.2,0,0,1)` | reduzierte Bewegung |

### C2. Bewegungen

| ID | Wo/Auslöser | Exakte Änderung |
|----|--------------|-----------------|
| M-01 | Vollbildschirm tritt ein | Ziel opacity 0→1, X 12→0 dp; Quelle opacity 1→0,72, X 0→−8; `standard` |
| M-02 | Vollbildschirm zurück | Ziel X 0→12 dp, opacity 1→0; Quelle zurück; `exit` |
| M-03 | Fold B-01/B-02 | Sessionbreite 0→152 dp, Hauptinhalt links 0→152; Scrollanker fest; `foldLayout` |
| M-04 | Sidebar außen | X −272→0, Scrim 0→1; öffnen `sidebarIn`, schließen rückwärts `sidebarOut`, Drag 1:1 |
| M-05 | Druck | Scale 1→0,97→1, je 80 ms `press` |
| M-06 | Aufnahme | Ring Scale 1→1,08→1, opacity 0,34→0,14→0,34, RMS nur opacity; 1600 ms endlos bis Stop |
| M-07 | neue Notiz | Y 10→0 dp, opacity 0→1, keine Skalierung; 260 ms |
| M-08 | TTS Absatz | alter Hintergrund → transparent, neuer transparent → Highlight, linke Kante 0→1; 160 ms |
| M-09 | Dialog/Sheet | opacity 0→1, Y 16→0 bzw. Sheet 100 %→0, Scrim; 240 ms, Exit 180 ms |
| M-10 | Löschen/Undo | opacity 1→0, X 0→−16, Lücke schließen; Undo M-07 |
| M-11 | Grenze nach Erfolg | altes Label Fade-out 180, neues Label Fade-in 240; keine Kartenbewegung |
| M-12 | Theme | alle Farben interpolieren, keine Geometrie; 240 ms |
| M-13 | Fortschritt | determinate Breite je Wert 240; unbekannt drei Opacity-Punkte, 1200-ms-Zyklus |
| M-14 | aktive Navigation | Linie→gefüllt per Crossfade, Label opacity 0,72→1, Fläche transparent→accentSoft; 240 ms |

### C3. Übergänge und Rückmeldung

B-01↔B-02 nutzt 300/220 ms Sidebar. B-01↔B-03/B-04/B-06 nutzt Sheet 240/180 ms. Vollbildnavigation nutzt M-01/M-02. Außen↔innen nutzt M-03 360 ms ohne Navigation. Aktionen nutzen Scale 0,97/80 ms plus leichten Tick; Aufnahme Start/Stop deutlichen Klick; destructive Bestätigung Warnklick; Long Press nach 400 ms; Wischschublade ohne Federung.

Dauerbewegung gibt es nur für M-06 während Aufnahme und M-13 bei aktiv unbekanntem Warten. Ladeanzeige lokaler Abfrage erst ab 200 ms; Transkription/KI/Qwen/Merge zeigen fachliche Phase bzw. determinaten Fortschritt. Reduced Motion: keine Dauerbewegung, Parallaxe oder Rotation; alle Schiebe-/Scale-/Größenbewegungen werden reine Fades von 100–160 ms, TTS-Highlight 100 ms, Haptik bleibt. Offene Fragen: keine.

---

## Teil D — Rahmen und Abnahme

### D1. Zielplattform und Rahmen

Android, Kotlin + Jetpack Compose. Galaxy Z Fold 8 außen 1248×1972 px / Zielraum 297×469 dp; innen 1848×2448 px / Zielraum 440×583 dp. Privat nur Frank, Deutsch, teilweise offline, Room lokal, Secrets verschlüsselt, direkte private Installation. Mindestversion Android 12L/API 32 ist abgeleitete technische Entscheidung.

Nicht enthalten: freier KI-Chat, änderbare Fokusfrage, automatische Auswertung, stille Kürzung, erneute Aufnahme alter Notizen, editierbares KI-Original, dauerhafte bestätigte Rohaufnahme, Cloudsync, automatische Sicherung, Bilder/Anhänge/Tags/Erinnerungen/Teams/Store.

### D2. Abnahme

| ID | Beobachtbares Kriterium |
|----|-------------------------|
| A-01 | Neue Sitzung, lokaler und einmaliger editierbarer KI-Titel, Sortierung letzte Änderung. |
| A-02 | Suche, Pin, Rename, Archiv, Löschen+Undo funktionieren vollständig. |
| A-03 | Außen Schublade, innen dauerhafte Sessionleiste; Sitzung/Entwurf/Scroll bleiben. |
| A-04 | Tippen und mehrere bestätigte Diktate hängen am selben Entwurf. |
| A-05 | Tipp Start/Stop, Abbruch, 10-Minuten-Limit, Hintergrundaufnahme+Notification. |
| A-06 | Alle vier Cortex-Filter laufen nachweisbar. |
| A-07 | Vollfilter bietet Anhören/Retry/Verwerfen; bestätigtes Audio wird gelöscht. |
| A-08 | Jede Notiz Zeit+lokaler Titel; einmaliger KI-Titel; manueller Titel geschützt. |
| A-09 | Edit, Copy, Duplicate, Delete+Undo, KI-Verbesserung+Original funktionieren. |
| A-10 | Persistente sichtbare Grenze; nur fertige neue Notizen dahinter im Kontext. |
| A-11 | Feste Fokusfrage, Profil und Webmodus pro Auftrag. |
| A-12 | Snapshot bleibt trotz späterer Änderungen fest. |
| A-13 | Nur vollständige Finalantwort verschiebt Grenze. |
| A-14 | Retry nutzt exakt identische Snapshotparameter. |
| A-15 | Modelllimit führt zu vollständigen Notizblöcken+Gesamtauswertung, nie stiller Kürzung. |
| A-16 | Alte bearbeitete Notiz wird nicht erneut aufgenommen; alte Antwort markiert. |
| A-17 | Gelöschte alte Grundlage markiert Antwort. |
| A-18 | KI-Original unveränderlich; Copy, TTS, neue Notiz, Delete+Undo. |
| A-19 | Webantwort hat nur am Ende eine Linkliste. |
| A-20 | Antwortabsätze 4–8 Sätze, kurze Antwort darf kleiner sein. |
| A-21 | PerfectMoment-Device-Code; Sol/Terra/Luna; low/medium/high/xhigh/max; Terra/medium. |
| A-22 | Drei feste editierbare+Reset und drei freie Profile; genau eines aktiv. |
| A-23 | KI läuft im Hintergrund mit Fortschritts-/Ergebnisnotification. |
| A-24 | Chirp/Edge/Qwen, Prefetch, komplette Absatzsteuerung/-highlight, Mediennotification. |
| A-25 | Anbieterwechsel nur bestätigt; Qwen-Klone vollständig verwaltbar. |
| A-26 | Versioniertes unverschlüsseltes JSON; Merge; lokale gleiche ID gewinnt. |
| A-27 | Lokale Funktionen offline; Cloudfunktionen klare Netzfehler. |
| A-28 | Fold erhält Sitzung, Entwurf, Scroll, Aufnahme, Transkription, KI und TTS. |
| A-29 | Vier vollständig durchgefärbte Themes, Gold-Dunkel Standard. |
| A-30 | Reduced Motion ohne Dauerbewegung/Parallaxe/Rotation, nur 100–160-ms-Fades. |
| A-31 | Kein toter Knopf; destructive Aktion bestätigt oder Undo; Fehler mit nächstem Schritt. |

### D3. Offene Fragen

Keine. Noch nicht ausdrücklich vorgegebene Kleindetails sind als abgeleitete technische Entscheidungen festgelegt.

## Z. Was ausdrücklich nicht ins Design oder in den Bau gehört

Keine ZIP-Datei und kein Werft-Messpaket erzeugen. Keine zusätzlichen Funktionen oder Navigationsebenen ergänzen, insbesondere keinen freien Chat, keine Anhänge, Tags, Erinnerungen, Cloud-Synchronisierung oder automatische Auswertung. Keine Secrets, Tokens, Audio- oder TTS-Caches sichern und keine alte Notiz still erneut auswerten.
