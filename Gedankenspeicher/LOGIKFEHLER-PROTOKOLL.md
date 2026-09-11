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

- **Aktuelle Runde:** 1 (läuft)
- **Nächste Tiefenstufe:** 1
- **Konvergenzzähler:** 0
- **Nächster Blickwinkel:** –
- **Offene Fixe:** –

## 4. Funde

(noch keine)

## 5. Rundenübersicht

| Runde | Stufe | Blickwinkel | Alle Bereiche | gemeldet | bestätigt | abgelehnt | Klärung | behoben | verifiziert | Build/Tests | Zähler danach |
|---|---|---|---|---|---|---|---|---|---|---|---|

## 6. Klärungsbedarf

(noch keiner)
