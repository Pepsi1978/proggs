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

- Aktuelle Runde: 1, Stufe: 1 (Überblick)
- Konvergenzzähler: 0
- Nächster Blickwinkel: Stufe 2 (Funktion für Funktion)
- Offene Fixe: keine
- Ausstehend: Commit/Push Runde 1, Version 0.1.114

## Funde

| ID | Runde | Bereich | Stelle | Kategorie | Schwere | Status | Kurzbeschreibung |
|---|---|---|---|---|---|---|---|
| F1 | 1 | oberflaeche | AppViewModel.kt:113-129 | Oberflächenlogik | hoch | behoben | Verlaufspinning bricht jede Sortierung |
| F2 | 1 | sicherung | AutoBackup.kt:42-46 | Daten und Persistenz | kritisch | behoben | Nachschub-Fragen lösen kein Backup aus |

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

## Rundenübersicht

| Runde | Stufe | Alle Bereiche geprüft | gemeldet/bestätigt/behoben/verifiziert | Build | Zähler danach |
|---|---|---|---|---|---|
| 1 | 1 | ja (Dateiliste: SessionEngine, SessionController, SessionSilence, EmojiParser, SessionModels, SessionRepository, BackupPayload, Daos, QuestionResponseValidator, SkillTextExport, WhisperHallucinationFilter, StreamingQuestionDecoder, IntroQuestionPolicy, AutoBackup, Entities, TtsManager, AppViewModel) | 2/2/2/2 (Selbstverifikation, Schnellmodus) | assembleDebug | 0 |

## Klärungsbedarf

- K1: AutoBackup erkennt reine Fragen-Text-Edits nicht (siehe F2). Empfehlung: zusätzlich Fragen-Flows in den Fingerprint aufnehmen.
- K2: SessionEngine.updateConfig setzt bei Daueränderung remainingMs auf initialRemainingMs zurück (verlorene Restzeit). Vermutlich beabsichtigt (Neustart des Timers) – keine Änderung.
