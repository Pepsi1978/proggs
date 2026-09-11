# Logikfehler-Jagd – Perfect Moment

- Software: PerfectMoment (`C:\Users\barwa\proggs\PerfectMoment`), Android/Kotlin
- Datum: 11.09.2026
- Arbeitsbranch: `main`, Remote: `origin/main`
- Ausgangscommit: `8421eda9e`
- Build: `./gradlew :app:assembleDebug` (Schnellmodus: nur Debug-Build, keine Testläufe – prüft der Mensch selbst)
- Basislinie: Build nicht vorab gelaufen (Schnellmodus); rote Tests: keine bekannt

## Karte

| Bereich | Dateien/Module | Ebene | Risiko | Zweck |
|---|---|---|---|---|
| session-engine | session/SessionEngine.kt, SessionModels.kt, SessionSilence.kt | Geschäftslogik | hoch | Sitzungsablauf, Timer, Nachschub, DND |
| session-steuerung | session/SessionController.kt, SessionForegroundService.kt, SessionNotification.kt | Zustandsverwaltung | hoch | Start/Resume/Replay, Engine-Verdrahtung |
| validierung | auth/QuestionResponseValidator.kt, StreamingQuestionDecoder.kt, IntroQuestionPolicy.kt, EmojiParser.kt, auth/* | Geschäftslogik | mittel | KI-Fragen prüfen, entdoppeln, streamen |
| persistenz | data/local/*, data/repository/* | Daten/Persistenz | hoch | Room, Fortschritt, Sortierung |
| sicherung | backup/* | Daten/Persistenz | hoch | Drive-/Datei-Backup, AutoBackup |
| sprache | audio/*, tts/* | Netzwerk/Nebenläufigkeit | mittel | Transkription, Halluzinationsfilter, TTS |
| oberflaeche | ui/AppViewModel.kt, ui/*.kt | Oberfläche | mittel | Sortierung, Lesereihenfolge, Navigation |
| plattform | security/*, network/*, di/*, MainActivity.kt | Plattform/Lebenszyklus | niedrig | App-Lock, Shutdown, DI |

Nicht prüfrelevant: `build/`, `.gradle/`, generierter Code, Fremdbibliotheken, `schemas/`.

## Loop-Zustand

- Aktuelle Runde: 6, Stufe: 5 (Blickwinkel: Tester – fehlende Testfälle, Sparmodus-Volle-Tiefe für geänderte Bereiche)
- Konvergenzzähler: 0
- Nächster Blickwinkel: Tester (fehlende Testfälle und was dort passiert)
- Offene Fixe: keine
- Ausstehend: Commit/Push Runde 1, Version 0.1.114

## Funde

| ID | Runde | Bereich | Stelle | Kategorie | Schwere | Status | Kurzbeschreibung |
|---|---|---|---|---|---|---|---|
| F1 | 1 | oberflaeche | AppViewModel.kt:113-129 | Oberflächenlogik | hoch | abgelehnt | Verlaufspinning bricht jede Sortierung – FEHL-FUND: HistorySortingTest schreibt den Pin als Vertrag fest |
| F2 | 1 | sicherung | AutoBackup.kt:42-46 | Daten und Persistenz | kritisch | behoben | Nachschub-Fragen lösen kein Backup aus |
| F3 | 3 | plattform | SessionForegroundService.kt:178-200 | Zustand und Lebenszyklus | niedrig | behoben | WakeLock wächst bei verlängerter Dauer nicht mit |
| F4 | 5 | plattform | SessionNotification.kt:37 | Oberflächenlogik | mittel | behoben | Notifikations-Knopf „Weiter" pausiert stumme Sitzung |

### F1 – Verlaufspinning bricht jede Sortierung
- Beweis: `sortHistorySessions` stellt `lastPlayed` per `listOf(lastPlayed) + rest.sortedWith(...)` immer an erste Stelle – auch bei Sortierung A–Z/Neueste/Älteste. Eingabe: Einträge A (zuletzt gespielt), B, C, Sort=A–Z → Ist: A,B,C mit A oben nur zufällig richtig; bei zuletzt gespielt=C → Ist: C,A,B → Soll: A,B,C.
- Fix: Pin nur bei MOST_USED; alle anderen Sortierungen rein nach Komparator.
- Test: kein Test, weil Schnellmodus (prüft der Mensch selbst).
- Verifikation: (1) Beweis hinfällig – andere Sortierungen ohne Pin. (2) Kein neues Fehlverhalten – MOST_USED-Pfad unverändert. (3) Null/leer/Grenzen – leere Liste gibt weiter emptyList zurück; Ein-Element-Liste unverändert.

### F2 – Nachschub-Fragen lösen kein Backup aus
- Beweis: Fingerprint nutzt je Session nur `id:topic:playCount`. Eingabe: laufende Sitzung, Nachschub hängt 30 Fragen an (appendQuestions ändert weder topic noch playCount) → Ist: Fingerprint gleich → `return@collect`, kein Upload → Soll: Sicherung wird neu geschrieben.
- Fix: Fingerprint je Session um `questionCount:lastPlayedAt:summary` erweitert.
- Test: kein Test, weil Schnellmodus (prüft der Mensch selbst).
- Verifikation: (1) Beweis hinfällig – Refill ändert questionCount → neuer Fingerprint. (2) Kein neues Fehlverhalten – nur mehr Uploads bei echten Änderungen. (3) Grenzen – Int-Hashkollision theoretisch möglich, praktisch wie bisher.
- Restrisiko (Klärungsbedarf K1): reine Text-Edits einzelner Fragen ändern kein Session-Feld und lösen weiter kein AutoBackup aus; observeSessions meldet sie gar nicht.

### F3 – WakeLock wächst bei verlängerter Dauer nicht mit
- Beweis: `acquireWakeLock()` läuft nur in `onCreate` mit der Startdauer. Eingabe: 10-Min-Sitzung starten, Bildschirm aus, Dauer live auf 120 Min erhöhen → Ist: WakeLock läuft nach 15 Min ab, CPU darf schlafen, Wiedergabe verstummt trotz Restzeit → Soll: WakeLock deckt die neue Dauer ab.
- Fix: `renewWakeLock` erneuert den WakeLock bei jeder Config-Änderung (über den Runtime-Flow in `updateNotification`), idempotent je Dauer.
- Test: kein Test, weil Schnellmodus (prüft der Mensch selbst).
- Verifikation: (1) Beweis hinfällig – Dauerwechsel erneuert das Zeitfenster. (2) Kein neues Fehlverhalten – idempotent, einziger Trigger der Runtime-Flow. (3) Null/leer/Grenzen – runtime null wird übersprungen, Dauer 0 ohne Timeout, release nur if held.

## Rundenübersicht

| Runde | Stufe | Alle Bereiche geprüft | gemeldet/bestätigt/behoben/verifiziert | Build | Zähler danach |
|---|---|---|---|---|---|
| 1 | 1 | ja (Dateiliste: SessionEngine, SessionController, SessionSilence, EmojiParser, SessionModels, SessionRepository, BackupPayload, Daos, QuestionResponseValidator, SkillTextExport, WhisperHallucinationFilter, StreamingQuestionDecoder, IntroQuestionPolicy, AutoBackup, Entities, TtsManager, AppViewModel) | 2/2/2/2 (Selbstverifikation, Schnellmodus) | assembleDebug | 0 |
| 2 | 2 | ja (Funktion für Funktion: ContentRepository, BackupRepository, SecureSettings, DictationText+Test, CodexModels, AppViewModel-Rest, HookIcons, Muster-Grep über alle Dateien) | 1/0/0/0 (HookIcons-Verdacht ohne konkreten Pfad abgelehnt) | – | 1 |
| 3 | 3 | ja (Grenzen/Zustand/Zeit: SessionForegroundService, SessionEngine-Lebenszyklen, SessionController-Startpfade, AudioFocus-Wechselwirkungen) | 1/1/1/1 (Selbstverifikation, Schnellmodus) | assembleDebug | 0 |
| 4 | 4 | ja (Wartungsentwickler: Folgen von F1–F3; HistorySortingTest gelesen; Kurzprüfung Rest via Runde 2/3) | 0/0/0/0 – F1 als Fehl-Fund erkannt und revertiert (Test = Vertrag) | assembleDebug | 1 |
| 5 | 5 | ja (Vollrunde, Blickwinkel ungeduldiger Benutzer/Angreifer: FileBackup, BackupStatus, DriveClient, SessionNotification, Doppelklick-/Import-Pfade) | 1/1/1/1 (Selbstverifikation, Schnellmodus) | assembleDebug | 0 |

### F4 – Notifikations-Knopf „Weiter" pausiert stumme Sitzung
- Beweis: `isPlaying = speakerOn && !paused`; der Knopf löst ACTION_PAUSE_RESUME aus. Eingabe: Sitzung stumm laufend (speakerOn=false, paused=false) → Ist: Knopf zeigt „Weiter", Tipp ruft pauseSession() → Sitzung pausiert → Soll: Beschriftung folgt der Aktion (Pause-Zustand).
- Fix: `isPlaying = state != null && state.paused != true`.
- Test: kein Test, weil Schnellmodus (prüft der Mensch selbst).
- Verifikation: (1) Beweis hinfällig – stumm+laufend zeigt „Pause". (2) Kein neues Fehlverhalten – pausiert/Null-Fälle wie bisher. (3) Null abgedeckt (state null → „Weiter" wie bisher, Service stoppt eh).

## Klärungsbedarf

- K1: AutoBackup erkennt reine Fragen-Text-Edits nicht (siehe F2). Empfehlung: zusätzlich Fragen-Flows in den Fingerprint aufnehmen.
- K2: SessionEngine.updateConfig setzt bei Daueränderung remainingMs auf initialRemainingMs zurück (verlorene Restzeit). Vermutlich beabsichtigt (Neustart des Timers) – keine Änderung.
