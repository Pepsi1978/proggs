# Logikfehler-Protokoll – Gedankenspeicher

## 1. Kopf

- **Software:** Gedankenspeicher (Android, Kotlin, Jetpack Compose, Room, OkHttp), `de.frank.gedankenspeicher`, Version 0.6.5 (versionCode 47)
- **Start:** 11.09.2026 19:41
- **Zweig:** `logikfehler-jagd/2026-09-11` in eigenem git-Worktree `C:\Users\barwa\proggs-logikfehler-jagd` (sparse checkout nur `Gedankenspeicher/`). Der gemeinsame Checkout `~/proggs` (main) bleibt unberührt.
- **Build:** `.\gradlew.bat assembleDebug` (im Ordner `Gedankenspeicher`)
- **Tests:** `.\gradlew.bat testDebugUnitTest` (JUnit ist als Abhängigkeit vorhanden, `app/src/test` gab es bei Start nicht)
- **Basislinie:** Build grün (nur SDK-XML-Warnung), 0 Unit-Tests, 0 Instrumented-Tests. Kein Gerät angeschlossen.
- **Verifikation:** Build und Diff-Prüfung, dazu JVM-Regressionstests für reine Kotlin-Logik, wo sie möglich sind.

## 2. Karte

| Nr | Name | Ebene | Dateien | Zeilen | Risiko | Soll |
|---|---|---|---|---|---|---|
| 1 | Einstieg und Plattform | Plattform/Lebenszyklus | `MainActivity.kt`, `AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/dateipfade.xml`, `app/build.gradle.kts` | ~1519 | hoch | Rechte bei Bedarf, SAF-Ordner- und Dateiwahl (dauerhafte Rechte), Biometrie für geschützte Sitzungen, Navigation, drei Vordergrunddienste, allowBackup=false, FileProvider nur für Export-Cache |
| 2 | Zentraler Zustand | Zustandsverwaltung | `ui/HauptViewModel.kt`, `ui/Zustand.kt` | 1850 | hoch | Orchestriert Aufnahme mit Ziel, Transkription und Nachreichen, Überschrift und Titel, Verbesserung mit Rückgängig, KI-Blatt und Auswertung, Vorlesen, Suche, Sperre, Sicherung und Wiederherstellung, Einstellungen. Hält StateFlows konsistent |
| 3 | Datenmodell, Persistenz und Sicherung | Datenzugriff/Persistenz | `data/Modell.kt`, `Datenbank.kt`, `Dao.kt`, `Repository.kt`, `Nachtraege.kt`, `Anhaenge.kt`, `Sicherung.kt`, `settings/Einstellungen.kt` | 1718 | hoch | Entities, DAOs, Migrationen 1→6. Das Repository als einziges Tor setzt die Regeln durch. Anhangsdateien, Nachträge, ZIP-Sicherung mit Prüfung und Wiederherstellung, verschlüsselte Einstellungen |
| 4 | Codex-KI-Anbindung | Netzwerk/Schnittstellen | `auth/*` (CodexAuthManager, CodexAufgaben, CodexModels, DeviceCodeFormat, ActiveCallTracker, IntroQuestionPolicy, QuestionResponseValidator, StreamingQuestionDecoder), `network/OkHttpShutdown.kt` | 1429 | hoch | Device-Code-Anmeldung, Tokens speichern und erneuern, Anfragen (Überschrift, Titel, Verbesserung, Rückfrage, Auswertung), Streaming lesen, Rückfrage prüfen, HTTP-Fehler bewerten, Aufrufe abbrechen |
| 5 | Aufnahme, Transkription und Dienste | Nebenläufigkeit/Hintergrund | `audio/*` (AufnahmeDienst, MicRecorder, GroqTranscriber, GroqModels, SpeechAnalyzer, WhisperHallucinationFilter), `hintergrund/AuswertungsDienst.kt`, `VorleseDienst.kt` | 1065 | hoch | WAV-Aufnahme mit Sprachanalyse. Groq-Transkription ohne erfundenen Text (Filter). Drei Vordergrunddienste mit Fernsteuerung |
| 6 | Vorlese-Steuerung | Geschäftslogik | `tts/Vorleser.kt`, `Absaetze.kt`, `Absatzabspieler.kt`, `GeraetTtsPlayer.kt`, `SpeechLoudness.kt`, `TtsCatalog.kt` | 810 | mittel | Genau eine Wiedergabe. Absätze vorsynthetisieren und der Reihe nach spielen, Pause und Fortsetzen, Rückfall auf die Gerätestimme, Stimmenkatalog, Lautheit |
| 7 | Cloud-Sprachausgabe | Netzwerk/Schnittstellen | `tts/EdgeTtsPlayer.kt`, `GoogleCloudTtsPlayer.kt`, `QwenTtsPlayer.kt`, `QwenVoiceDirectory.kt`, `QwenVoiceEnrollment.kt`, `TtsNetz.kt` | 1453 | mittel | Synthese über Edge (WebSocket), Google Chirp 3 HD und Qwen, eigene Stimmen verwalten. Liefert Audio oder `TtsPlaybackException` |
| 8 | Anhänge-Oberfläche | Präsentation | `ui/verlauf/Anhaenge.kt` | 1679 | mittel | Plus-Menü, Anzeige aller Anhangsarten, Zeichnung, Haftnotiz, Tabelle, Tonaufnahme, Vollbild, Texterkennung |
| 9 | Verlauf und Notizbearbeitung | Präsentation | `ui/verlauf/VerlaufBildschirm.kt`, `Notizkarte.kt`, `BearbeitenBlatt.kt`, `AntwortBearbeitenBlatt.kt` | 1510 | mittel | Gemeinsame Liste aus Notizen und Antworten mit Zustandsanzeigen und Aktionen, Bearbeiten mit Cursor-Einschub und Nachträgen, Auswertung korrigieren |
| 10 | KI-Oberfläche und Reichtext | Präsentation | `ui/verlauf/Reichtext.kt`, `KiKarte.kt`, `ui/ki/KiBlatt.kt`, `ui/einstellungen/ProfileBildschirm.kt` | 1651 | mittel | Markdown-Darstellung, `ohneQuellen`, KI-Karte und KI-Blatt (Rückfrage, Antwort, Websuche, Auswerten), Profil-Editor (höchstens eins aktiv) |
| 11 | Seitenleiste, Suche und Einstellungen | Präsentation | `ui/sitzungen/Schublade.kt`, `ui/suche/SucheBildschirm.kt`, `ui/einstellungen/EinstellungenBildschirm.kt`, `AnmeldungBildschirm.kt` | 1740 | mittel | Reiter (alle, Favoriten, geschützt, Papierkorb, Kategorien), Sitzungsaktionen, Suche ab 2 Zeichen, Einstellungen, Codex-Anmeldung |
| 12 | Theme und UI-Bausteine | Präsentation | `ui/theme/*`, `ui/Dialoge.kt`, `ui/verlauf/Bildspeicher.kt`, `Zwischenspeicher.kt` | 971 | niedrig | Vier Erscheinungen, Maße, Motion, Schriften, Bausteine, Dialoge. LRU-Caches, die das Ergebnis nicht verändern dürfen |

**Nicht prüfrelevant:** `res/drawable`, `res/font`, `res/mipmap-anydpi-v26`, `res/values` (Ressourcen), Gradle-Wrapper, `build/`.

**Nicht vorhandene Ebenen:** keine eigene Hilfsfunktionen-Schicht (verteilt), keine eigene Konfigurations- oder Migrations-Schicht (Migrationen in `Datenbank.kt`, Build in Bereich 1), keine Domänen- oder UseCase-Schicht, keine DI, kein WorkManager.

**Room:** Schema-Version 6, `exportSchema=false`, keine destruktive Migration. Migrationen 1→2 (anhaengeJson), 2→3 (favorit, geschuetzt, geloeschtAm, ordnerId, Tabelle ordner), 3→4 (zuletztGeaendert), 4→5 (nachtragzeitenJson), 5→6 (ordner.art). CASCADE von notiz und ki_antwort auf sitzung. `sitzung.ordnerId` ohne Fremdschlüssel.

**Zentrale Invarianten (vom Kartografen erkannt):**
1. Notiz und KiAntwort gehören zu genau einer Sitzung (CASCADE).
2. Es gibt immer eine Sitzung, die geöffnete liegt nie im Papierkorb, gesperrte sind nur nach Biometrie sichtbar.
3. Was von Hand gesetzt wurde (`titelVonHand`, `ueberschriftVonHand`), überschreibt die KI nie.
4. `textOriginal` wird nur vor der ersten Verbesserung gesetzt.
5. Genau sechs Profile, höchstens eins aktiv, ein leeres lässt sich nicht aktivieren.
6. Notiz-Zustandsmaschine: AUFNEHMEND übersteht keinen Start, `audioPfad` nur solange die Transkription aussteht, höchstens 3 Nachreich-Versuche, leeres Transkript ergibt NICHTS_VERSTANDEN.
7. Der Verlauf ist eine Liste, sortiert nach `zeit`. Verschieben ändert `erstelltAm` nicht, `zuletztGeaendert` steigt nur bei Inhaltsänderungen.
8. Es spricht immer nur einer, eine Aufnahme dauert höchstens 10 Minuten.
9. Der Auswertungskontext enthält FERTIG-Notizen und Antworten und keine Quellenangaben.
10. Sicherung: WAL-Checkpoint, DB schließen vor dem Austausch, altes Nur-DB-Format bleibt einspielbar.

## 3. Loop-Zustand

- **Aktuelle Runde:** 2 UNTERBROCHEN am 11.09.2026 (Nutzer: Token-Limit). Fertig geprüft: Bereiche 3B, 4B, 5A, 6B, 8, 10A, 11, 12. Abgebrochen und neu zu prüfen: 1A, 1B, 2A, 2B, 3A, 4A, 5B, 6A, 7, 9, 10B. Die bisher gemeldeten Funde (noch NICHT triagiert) sind:
  - **Bereich 5 – Aufnahme**
    - L-2-5-01 (mittel): MicRecorder, start() während stop() in joinAll → der Puffer der alten Aufnahme wird geleert, das finally des alten Jobs beendet die neue Aufnahme.
  - **Bereich 4 – KI-Anbindung**
    - L-2-4-01 (niedrig): einzeiler trimmt Anführungszeichen einzeln statt paarweise („Buchtipp „Momo“ → „Buchtipp „Momo“).
  - **Bereich 12 – Oberfläche/Motion**
    - L-2-12-01 (niedrig): bewegungReduziert() liest per remember einen veralteten Wert, dauer() liest frisch.
  - **Bereich 3 – Sicherung**
    - L-2-3-01 (niedrig): Der Steckbrief zählt PDF-Vorschaubilder als Anhänge mit.
    - L-2-3-02 (niedrig): Anhangsspeicher.uebernimm hinterlässt bei einem Kopierfehler eine halbe Datei.
    - L-2-3-03 (niedrig, durch L-1-3-1b): wartend-Dateien ohne Notiz nach einer Wiederherstellung wandern in jede Sicherung.
  - **Bereich 11 – Suche und Einstellungen**
    - L-2-11-1 (hoch, Plattformverhalten prüfen): SQLite lower() setzt Ä/Ö/Ü womöglich nicht klein, dann findet die Suche keine Wörter mit großem Umlaut.
    - L-2-11-2 (mittel): Der Probe-Knopf zeigt „Probe anhalten“ bei jeder Vorlesung.
  - **Bereich 6 – Vorlesen**
    - L-2-6-01 (mittel): Vorleser: der innere catch im Geräte-Rückfall fängt eine CancellationException und meldet einen Fehler.
    - L-2-6-02 (niedrig, durch L-1-2-07b): stoppeLaufendes setzt _absatzNr nicht zurück → Absatz 0 der neuen Vorlesung wird nicht hervorgehoben.
    - L-2-6-03 (niedrig): „Anhalten“ in der Lücke zwischen zwei Absätzen hat keine Wirkung.
  - **Bereich 8 – Anhänge**
    - L-2-8-1 (mittel): Zeichenblatt „Übernehmen“ doppelt getippt → runCatching fängt die CancellationException, es erscheint eine Fehlermeldung und die PNG bleibt verwaist liegen.
    - L-2-8-2 (niedrig): Das NotizAnhangsmenue wird beim Kopieren weggetippt → dieselbe Meldung „cancelled“, verwaiste Datei.
  - **Bereich 10 – KI-Blatt**
    - L-2-10-01 (mittel): Eine leere Rückfrage lässt das KI-Blatt ohne Frage, Fehler und Knopf zurück.
    - L-2-10-02 (mittel): werteAus verwirft keine laufende Antwort-Aufnahme und zählt kiBlattGeneration nicht hoch.
    - L-2-10-03 (niedrig): Das KI-Blatt hält eine Notiz-Aufnahme für die Antwort-Aufnahme.
    - L-2-10-04 (mittel): Websuche „immer“ und „KI entscheidet“ erzeugen denselben Request.

  Nächster Schritt: diese Funde triagieren, die abgebrochenen Bereiche neu prüfen, dann fixen.
- **Nächste Tiefenstufe:** 2 (Datei für Datei, Verträge und Randfälle)
- **Konvergenzzähler:** 0
- **Nächster Blickwinkel:** Stufe 2. Die in Runde 1 geänderten Stellen sind als „kürzlich verändert“ markiert
- **Offene Fixe:** keine
- **Arbeitsweise ab Runde 2 (Vorgabe des Nutzers vom 11.09.2026):** keine Workflow-Läufe, nur einzelne Agenten (Typ `logik-pruefer`, effort high; sonst general-purpose). Orchestrator höchstens xhigh
- **Vorgemeldet für die Triage in Runde 2** (Beobachtungen der Fixer und Verifizierer aus Runde 1, noch nicht triagiert):
  - (a) Anmeldung: Scheitert schon der erste Code-Abruf (Code leer, fehler gesetzt), gibt es keinen Knopf „Neuen Code holen“ (AnmeldungBildschirm).
  - (b) Vorleser: Beim Stopp werden fertig vorsynthetisierte Absatzdateien nicht gelöscht (liesInAbsaetzen, finally).
  - (c) MicRecorder: Ist der Puffer voll oder `read < 0`, endet die Aufnahme stumm; der Pegel bleibt stehen, die Benachrichtigung „hört zu“ bleibt.
  - (d) Reichtext.ohneQuellen: `\n{3,}` wird auch in Code- und SVG-Blöcken gekürzt.
  - (e) Reichtext: „(siehe https://x.de)“ lässt „(siehe )“ stehen.
  - (f) Reichtext: `[ \t]+([.,;:!?])` entfernt die Einrückung vor einem Satzzeichen am Zeilenanfang.
  - (g) VerlaufBildschirm: Nach einem Sprung in eine andere Sitzung mit gleich langer Liste kann der Hervorhebungs-Effekt gegen die alte Liste laufen.
  - (h) markdownLink endet an der ersten „)“: aus [Merkur](…_(Planet)) bleibt „Merkur)“ stehen.

## 4. Funde

Kompaktes Tabellenformat. Die Spalten entsprechen dem Fund-Format aus Abschnitt 7 des Auftrags. Der Beweis steht verkürzt als „Zustand → Ist → Soll“. Die IDs haben die Form L-Runde-Bereich-Nr. Doppelt gemeldete Funde sind unter der ersten ID zusammengeführt.

### Runde 1 (Stufe 1)

| ID | Bereich | Stelle | Kategorie | Schwere | Beweis (Zustand → Ist → Soll) | Status | Triage | Fix | Test | Verifikation |
|---|---|---|---|---|---|---|---|---|---|---|
| L-1-1-01 | 1 | MainActivity.kt:980, 1113-1126 | Daten/UI | hoch | Notiz verschieben → Papierkorb-Sitzung wählbar → Notiz geht beim Leeren verloren. Soll: nur Sitzungen außerhalb des Papierkorbs | verifiziert | `alle()` ohne Filter, gelesen | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-1-02 | 1 | AndroidManifest.xml:36 | Plattform | hoch | Umzug auf ein neues Gerät (targetSdk 36) → EncryptedSharedPreferences ohne Keystore-Schlüssel, Absturz bei jedem Start. Soll: keine Übertragung | verifiziert | allowBackup=false greift ab API 31 nicht für D2D; dataExtractionRules fehlt | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-1-03 | 1 | MainActivity.kt:104-216 | Lebenszyklus | mittel | Prozesstod in der SAF- oder Mikrofon-Abfrage → Callback vor der Komposition → lateinit-Absturz | verifiziert | Ergebnisse kommen in onStart, `modell` wird erst beim ersten Compose-Durchlauf gesetzt | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-1-04 | 1 | MainActivity.kt:333-346 | Bedingung | niedrig | API 26-29, PIN, kein Fingerabdruck → geschützte Sitzungen bleiben dauerhaft zu | verifiziert | DEVICE_CREDENTIAL-Zweig nur ab R | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-01 | 2 | HauptViewModel.kt:1037-1046 | Grenzen | kritisch | zwei Nachträge, dann Speichern → substring(13,7) → Absturz, Bearbeitung verloren | verifiziert | `filter{it<stelle}.maxOrNull()` falsch herum. Doppelt gemeldet von beiden Prüfern | Runde-1-Commit, 2. Anlauf | NachtraegeTest (2) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-2-02 | 2 | HauptViewModel.kt:684-686 | Daten | kritisch | kein Groq-Schlüssel, online aufgenommen → audioPfad=null → Aufnahme verloren, Karte ewig „Wartet“ | verifiziert | datei==null im Online-Pfad. Doppelt gemeldet | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-03 | 2 | HauptViewModel.kt:707-714, Dao.kt:135 | Zustand | hoch | erster Nachreich-Versuch scheitert → FEHLGESCHLAGEN → nie wieder automatisch. Soll: 3 Versuche | verifiziert | `wartende` liest nur WARTET | Runde-1-Commit, 3. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-2-04 | 2 | HauptViewModel.kt:866-879 | Nebenläufigkeit | hoch | App-Start mit Netz → init und onAvailable reichen parallel nach → doppelt, FERTIG wird überschrieben | verifiziert | kein Ausschluss, alter Schnappschuss. Doppelt gemeldet (L-1-2-08) | Runde-1-Commit, 3. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-2-05 | 2/3 | HauptViewModel.kt:903-913, Dao.kt:145 | Bedingung | mittel | Überschrift von Hand geleert → nach dem Neustart setzt die KI wieder eine | verifiziert | ueberschriftVonHand nicht geprüft. Doppelt gemeldet (L-1-3-3) | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-06 | 2 | HauptViewModel.kt:1017-1020, 804-841 | Indizes | mittel | Nachtrag, dann davor tippen oder einschieben → Nachtragszeile zerreißt ein Wort | verifiziert | Stellen werden nicht verschoben. Doppelt gemeldet (L-1-2-10) | Runde-1-Commit, 1. Anlauf | NachtraegeTest (3) | (1) ja (2) nein (3) ja |
| L-1-2-07 | 2 | HauptViewModel.kt:1216-1221 | Zustand | mittel | Aufnahme läuft, Suchtreffer in Sitzung B → Notiz landet in B | verifiziert | springeZu ohne Sperre. Doppelt gemeldet | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-08 | 2 | HauptViewModel.kt:1329-1334 | Verträge | mittel | Groq-Schlüssel eintippen → Nachholen mit Teilschlüssel → 401, Versuch verbraucht | verifiziert | onValueChange je Taste | Runde-1-Commit, 3. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-2-09 | 2 | HauptViewModel.kt:738-779 | Zustand | niedrig | Blatt während der Transkription neu geöffnet → altes Transkript im neuen Blatt | verifiziert | keine Identitätsprüfung | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-2-03b | 2 | HauptViewModel.kt:1418-1420 | Verträge | hoch | Qwen-Schlüssel eintragen, Anbieter Edge → stimmeEdge wird mit der Qwen-Kennung überschrieben | verifiziert | setzeTtsStimme schreibt in den Platz des aktuellen Anbieters | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-04b | 2 | HauptViewModel.kt:1473-1474 | Reihenfolge | hoch | neue Stimme bei Anbieter Edge angelegt → Kennung landet in stimmeEdge, Qwen bleibt alt | verifiziert | Aufrufreihenfolge | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-05b | 2 | HauptViewModel.kt:1471-1476 | Kontrollfluss | mittel | Stimme angelegt → Liste wird nicht neu geladen | verifiziert | Wache `_stimmenLaden` | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-07b | 2/6 | HauptViewModel.kt:220-222, Vorleser.kt:106,210-216 | Nebenläufigkeit | hoch | A liest vor, Tipp auf B → finally des alten Jobs löscht derzeit, quelle und laeuft → Pause wirkungslos, liestVor null | verifiziert | halteAn in lies() plus nicht abgewartetes finally. Zusammengeführt mit L-1-6-3 | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-09b | 2 | HauptViewModel.kt:1595-1670 | Nebenläufigkeit | mittel | Knopf und Hintergrund-Sicherung gleichzeitig → gemeinsamer Entwurf, „vorher“ zerstört | verifiziert | kein Ausschluss | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-2-11 | 2 | HauptViewModel.kt:1388-1392 | Bedingung | niedrig | Notiz wird vorgelesen, Probe gedrückt → stoppt nur, keine Probe | verifiziert | Kommentar: nur eine Probe anhalten | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-3-1a | 3 | Repository.kt:93-99 | Zustand | mittel | geschützte Sitzung A offen, B endgültig gelöscht → offeneSitzung=C, neue Notiz landet unsichtbar in C | verifiziert | Nachfolge auch bei nicht offener Sitzung | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-3-2a | 3 | Repository.kt:93,111 | Daten | niedrig | Papierkorb leeren → Anhangs- und Audiodateien bleiben liegen und wandern in jede Sicherung | verifiziert | CASCADE ohne Dateiaufräumen. Doppelt gemeldet (L-1-3-6) | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-3-3b | 3 | Repository.kt:261 | Bedingung | – | Überschrift von Hand, danach nur Text geändert → Flag false | abgelehnt | ohne beobachtbare Folge: die KI überschreibt keine vorhandene Überschrift, und das Leeren setzt das Flag wieder | – | – | – |
| L-1-3-4a | 3 | Repository.kt:216-219 | Zustand | niedrig | Nachreichen scheitert → Sitzung springt nach oben, obwohl sich am Inhalt nichts geändert hat | verifiziert | Modell.kt: nur bei Inhaltsänderung | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-3-1b | 3 | Sicherung.kt:91-132 | Daten | hoch | offline aufgenommen, gesichert, wiederhergestellt → Aufnahme fehlt, Notiz auf FEHLGESCHLAGEN | verifiziert | files/wartend wird nicht gepackt | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-3-2b | 3 | HauptViewModel.kt:1770-1804 | Fehlerbehandlung | mittel | Kopierfehler beim Einspielen → DB halb, Anhänge weg, trotzdem „Wiederhergestellt“ | verifiziert | nicht atomar, runCatching verschluckt Fehler | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-3-4b | 3 | Sicherung.kt:144-150 | Fehlerbehandlung | niedrig | Checkpoint busy → jüngste Commits fehlen, trotzdem „gesichert“ | verifiziert | Ergebnisspalte wird verworfen | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-3-5 | 3 | Einstellungen.kt:197-210 | Daten | niedrig | Wiederherstellung → „letzte Sicherung“ zeigt die Zeit aus dem Archiv | verifiziert | DRIVE_ZEIT/GROESSE nicht ausgenommen | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-3-7 | 3 | Anhaenge.kt:44,116-120 | Daten | niedrig | Wiederherstellung in einem anderen Android-Benutzerprofil → absolute Pfade zeigen ins Leere | Klärungsbedarf | nur bei profilübergreifender Wiederherstellung. Siehe Abschnitt 6 | – | – | – |
| L-1-4-1 | 4 | CodexAufgaben.kt:368-385 | Null/Leer | mittel | Antwort `{"text":""}` → roher JSON-Text wird als Überschrift gespeichert. Soll: "" | verifiziert | Vertrag „leer, wenn nichts Brauchbares“ | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-4-2 | 4 | CodexAuthManager.kt:249-251,491-500 | Lebenszyklus | mittel | KI-Blatt schließen, während eine Überschrift oder Verbesserung läuft → Aufruf abgebrochen, Überschrift fehlt | verifiziert | ein gemeinsamer Tracker für alle Aufrufe | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-4-3 | 4 | CodexAuthManager.kt:253-255,431-462 | Nebenläufigkeit | niedrig | trennen, während ein Refresh läuft → Tokens werden zurückgeschrieben, wieder verbunden | verifiziert | kein Abgleich | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-4-4 | 4 | CodexAufgaben.kt:383 | Bedingung | niedrig | “Titel” → schließendes ” bleibt stehen | verifiziert | Zeichenmenge unvollständig | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-5-01 | 5 | AufnahmeDienst.kt:97-98 | UI | niedrig | Text sagt „zum Beenden hier tippen“, der Tipp öffnet nur die App | verifiziert | Anzeige widerspricht dem Verhalten | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-5-02 | 5 | VorleseDienst.kt:149-150 | UI | niedrig | „Zum Weiterhören hier tippen“ → öffnet nur die App | verifiziert | dito | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-5-03 | 5 | MicRecorder.kt:133,279 | Einheiten | niedrig | Stimmprobe mit 44,1 kHz → nach 218 s stumm abgeschnitten | verifiziert | Grenze fest auf 16 kHz gerechnet | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-6-1 | 6 | Vorleser.kt:178-209 | Kontrollfluss | hoch | Netzabriss bei Absatz 5 → geschafft=0 → Gerätestimme liest von vorn | verifiziert | Zuweisung nur bei normaler Rückkehr | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-6-2 | 6 | Vorleser.kt:230-246 | Nebenläufigkeit | mittel | Vorsynthese von Absatz 2 scheitert → Absatz 0 wird mitten im Satz abgeschnitten | verifiziert | Fehler eines async-Kindes bricht coroutineScope ab | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-6-4 | 6 | GeraetTtsPlayer.kt:118-145 | Verträge | mittel | onError ohne onStart → Continuation hängt, laeuft bleibt true | verifiziert | Meldung hängt an laeuftGerade | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-6-5 | 6 | Vorleser.kt:180-203 | Fehlerbehandlung | niedrig | Schlüssel fehlt und die Gerätestimme scheitert → drei Meldungen, zweiter Versuch | verifiziert | weg statt derzeit geprüft | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-7-1 | 7 | QwenVoiceDirectory.kt:45-70 | Fehlerbehandlung | mittel | offline oder 401 → leere Liste, keine Meldung, alte Liste überschrieben | verifiziert | catch-Zweig des Aufrufers unerreichbar | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-7-2 | 7 | TtsNetz.kt:24-26 | Ressourcen | niedrig | Abbruch im Wettlauf mit onResponse → Response bleibt offen | verifiziert | resume ohne onCancellation | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-7-3 | 7 | EdgeTtsPlayer.kt:319-322 | Ressourcen | niedrig | Abbruch nach turn.end → MP3 bleibt im Cache | verifiziert | dito | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-8-1 | 8 | ui/verlauf/Anhaenge.kt:236-243 (+486) | Daten | niedrig | Scan-Ersatzweg → JPEG bleibt verwaist | verifiziert | kein Löschen nach der Texterkennung | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-8-2 | 8 | ui/verlauf/Anhaenge.kt:194-203 | UI | niedrig | mehrseitiger Scan → Name „— Seite 1 —“ | verifiziert | Name aus dem Marker | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-8-3 | 8 | ui/verlauf/Anhaenge.kt:174,224-243 | Lebenszyklus | mittel | Prozesstod in der Kamera-App → Foto verloren | verifiziert | Pfad nur in remember | Runde-1-Commit, 3. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-8-4 | 8 | ui/verlauf/Anhaenge.kt:787 | Null/Leer | niedrig | dauerMs=0 → Balken voll, Spulen unmöglich | verifiziert | – | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-8-5 | 8 | ui/verlauf/Anhaenge.kt:767 | UI | niedrig | einseitiges PDF → „1 Seiten“ | verifiziert | – | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-9-01 | 9 | VerlaufBildschirm.kt:121-134 | Zustand/UI | hoch | Sprung aus der Suche → nach 1,2 s scrollt die Liste auf 0, der Treffer ist weg | verifiziert | hebeHervor als Effekt-Schlüssel | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-9-02 | 9 | VerlaufBildschirm.kt:258-260 | UI | mittel | Auswertung läuft → Platzhalter unten, Antwort erscheint oben | verifiziert | Liste absteigend sortiert | Runde-1-Commit, 2. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-9-03 | 9 | Notizkarte.kt:353-354 | Verträge | mittel | Nachtrag vorlesen → Hervorhebung um einen Absatz verschoben | verifiziert | Zeile mit einfachem \n, Absaetze trennt erst bei \n\n | Runde-1-Commit, 3. Anlauf | NachtraegeTest.zeileUndAbschnittstext… | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-9-04 | 9 | VerlaufBildschirm.kt:326 | UI | niedrig | Mikrofon abgelehnt, nur Anhang → Knopf grau, sendet aber | verifiziert | – | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-10-1 | 10 | KiBlatt.kt:162-191, HauptViewModel.kt:1087 | UI | hoch | „Auswerten“ bei leerem Feld → Rückfrage verschwindet, „Nochmal“ verwirft die Frage | verifiziert | fehler-Zweig vor der Rückfrage | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-10-2 | 10 | KiBlatt.kt:128-140 | UI/Zustand | mittel | Grundhaltung „KI entscheidet“, Tipp auf „aus“ → Option weg | verifiziert | Anzeige hängt an der Wahl statt an der Grundhaltung | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-10-3 | 10 | Reichtext.kt:122-129 | Daten | mittel | eingerückte Fortsetzungszeile oder Code → Einrückung und „()“ zerstört | verifiziert | saeubere ohne Code-Schutz | Runde-1-Commit, 2. Anlauf | ReichtextTest (3) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-10-4 | 10 | Reichtext.kt:101 | Bedingung | mittel | „(siehe https://x.de)“ → „(siehe“ | verifiziert | \S+ frisst Satzzeichen | Runde-1-Commit, 2. Anlauf | ReichtextTest (4) | (1) ja (2) nein (3) ja nach Nachbesserung |
| L-1-10-5 | 10 | Reichtext.kt:104 | Bedingung | mittel | „**Quellen:**“ und Liste bleiben stehen | zurückgenommen → Klärungsbedarf | bestätigt | 3 Anläufe, jeder mit neuem Nebeneffekt (zuletzt: „Quelle: https://…“ + Leerzeile löscht die folgende Inhaltsliste). Nach Regel 7 vollständig zurückgenommen, die Regex steht wieder im Ursprungszustand | Tests entfernt | Anlauf 3: (1) teilweise (3) nein |
| L-1-10-6 | 10 | Reichtext.kt:809-811 | Bedingung | mittel | „Nutzer*innen“ → Stern weg, Rest kursiv | verifiziert | – | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-10-7 | 10 | Reichtext.kt:793-795 | Bedingung | niedrig | `C:\Users` in Code → „C:Users“ | verifiziert | – | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-10-8 | 10 | Reichtext.kt:538 | UI | niedrig | Tabellen ohne Zeilen-Trennlinie | verifiziert | fillMaxWidth unter horizontalScroll | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-11-1 | 11 | AnmeldungBildschirm.kt:89-124 | UI | mittel | Anmeldung scheitert, der Code steht → kein Fehlertext, kein „Neuer Code“ | verifiziert | Reihenfolge im when | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-11-2 | 11 | Dao.kt:240-255 | Sichtbarkeit | mittel | Suchtreffer im Papierkorb → Sprung in eine falsche Sitzung | verifiziert | – | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |
| L-1-11-3 | 11 | SucheBildschirm.kt:65,121-138 | UI | mittel | zwei „Neue Sitzung“ → Treffer zu einer Gruppe verschmolzen | verifiziert | groupBy Titel | Runde-1-Commit, 1. Anlauf | keine Testinfrastruktur (android.*/Compose) | (1) ja (2) nein (3) ja |

**Nicht als Fund gezählt (Prüferhinweis):** MainActivity.kt:1008 „Text kopieren“ kopiert den Vorlesetext ohne Aufbau. Absichtlich so, denn daneben gibt es „Text mit Aufbau kopieren“.

## 5. Rundenübersicht

| Runde | Stufe | Blickwinkel | Alle Bereiche | gemeldet | bestätigt | abgelehnt | Klärung | behoben | verifiziert | Build/Tests | Zähler danach |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | Überblick; Zweitprüfer für die Bereiche 2 und 3 | ja (14 Prüfer, alle Dateilisten da) | ~70 (61 nach Zusammenführung) | 59 | 1 | 2 (L-1-3-7, L-1-10-5 nach Rücknahme) | 58 | 58 | Build grün, 13/13 Unit-Tests grün (Basislinie 0) | 0 |

## 6. Klärungsbedarf

- **L-1-10-5 – Quellenblöcke in KI-Auswertungen (nach 3 Anläufen zurückgenommen).** `ohneQuellen` erkennt nur Zeilen, die mit „Quellen:“ beginnen. „**Quellen:**“, „## Quellen“ und die darunter stehende Linkliste bleiben stehen.
  - Deutung A: Nur die einzeilige „Quelle: …“-Zeile soll weg. Dann ist nichts zu tun.
  - Deutung B: Ganze Quellenblöcke (Überschrift + Liste) sollen weg. Dann braucht es eine sauber abgegrenzte Regel, die Inhaltslisten unter einzeiligen Quellenangaben nicht mitnimmt.
  - **Empfehlung:** B, und zwar so: (1) `quellzeile` als ersten Schritt in `saeubere` ausführen, vor dem Entfernen von Adressen, Links und Fußnoten. (2) Eine Liste nur hinter einer reinen Überschrift ohne Inhalt nach dem Doppelpunkt mitnehmen.
- **L-1-3-7 – Anhangspfade nach der Wiederherstellung in einem anderen Benutzerprofil.** `anhaengeJson` und `audioPfad` speichern absolute Pfade (`/data/user/0/…`). Auf demselben Profil und Paket passt das auch nach einer Wiederherstellung auf einem neuen Gerät. Spielt man eine Sicherung aber in einem Arbeitsprofil oder im Secure Folder ein (`/data/user/10/…`), findet die App keinen Anhang.
  - Deutung A: profilübergreifendes Wiederherstellen ist kein unterstützter Fall. Dann ist nichts zu tun.
  - Deutung B: es soll gehen. Dann müssen die Pfade relativ gespeichert oder beim Lesen gegen `filesDir` aufgelöst werden.
  - **Empfehlung:** B, beim Lesen über den Dateinamen auflösen. Das ist aber ein Umbau an einer zentralen Datenstelle, deshalb nicht ohne Freigabe.
