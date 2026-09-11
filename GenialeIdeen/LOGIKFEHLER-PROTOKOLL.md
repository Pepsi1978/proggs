# Logikfehler-Jagd — Geniale Ideen

Kopf: Geniale Ideen (Android, `C:\Users\barwa\proggs\GenialeIdeen`), 11.09.2026, Zweig: main (per Entscheid AGENTS.md),
Build: `./gradlew assembleSchnell`, Tests: nur androidTest (BestandsMigrationTest, braucht Gerät).
Basislinie: Build ok (Annahme, verifiziert nach Fix-Charge), Test rot (erwartet Schema 3, Code ist 4 → F09).

Karte (jede Datei genau ein Bereich):
| Bereich | Dateien | Ebene | Risiko |
|---|---|---|---|
| Datenmodell | Entities, Daos, Database | Daten | hoch |
| Repository | IdeenRepository | Geschäftslogik | hoch |
| Sicherung | Sicherung, DateiSicherung, BackupStatus | Daten | hoch |
| Suche/Verlauf | Repository.suche, SuchverlaufDao | Geschäftslogik | mittel |
| KI-Chat | CodexAuthManager, CodexModels, DeviceCodeFormat, ActiveCallTracker | Netzwerk | hoch |
| Vorlese-Pipeline | Vorleser, VorleseDienst, Synthese, SprechText | Nebenläufigkeit | hoch |
| TTS-Player | TtsManager, TtsCatalog, Edge-, Google-, Qwen-Player, DeutscheAussprache, SpeechLoudness, QwenVoiceDirectory, QwenVoiceEnrollment | Netzwerk/Plattform | mittel |
| Diktat | MicRecorder, Diktat, GroqTranscriber, GroqModels, SpeechAnalyzer, WhisperHallucinationFilter, VoiceSampleScript | Nebenläufigkeit | hoch |
| Text | UmlautKorrektur | Hilfsfunktionen | niedrig |
| Einstellungen | SecureSettings, AppLockManager | Zustand | mittel |
| Verdrahtung | IdeenApplication, AppContainer, MainActivity, OkHttpShutdown, IdeenLog, IdeenCrashHandler | Plattform | mittel |
| UI-Liste | ListenScreen, DragReorder, KategorieWahl | Oberfläche | mittel |
| UI-Erfassen | ErfassenScreen | Oberfläche | hoch |
| UI-Detail | DetailScreen | Oberfläche | hoch |
| UI-Rest | GenialeIdeenApp, EinstellungenScreen, StimmeScreen, DiagnoseScreen, Stimmenliste, StimmenDropdown, Klappmenue, Knopf3D, Bausteine, Theme | Oberfläche | mittel |

Loop-Zustand: Runde 2 beendet (Stufe 3: Aufrufer/Verträge/Lebenszyklus + Restdateien), 3 neue bestätigt/behoben, Zähler 0.
Nächste: Runde 3 (Stufe 4: Invarianten/Gegenbeweis, Fokus Sicherungs-Abgleich + KI-Verlauf + Filter).
Konvergenzzähler: 0.

Funde:
| ID | Runde | Bereich | Stelle | Kategorie | Schwere | Status | Kurz |
|---|---|---|---|---|---|---|---|
| F01 | 1 | Sicherung | Sicherung.kt:121-155 | Daten/Persistenz | kritisch | behoben | Import ohne Zeitstempel verwirft fast alle Ideen |
| F02 | 1 | Diktat | IdeenViewModel.kt:864-882 + GroqTranscriber.kt:45-49 | Validierung | hoch | behoben | Groq-Prüfung sendet Stille, Schlüssel wird nie geprüft |
| F03 | 1 | UI-Detail | IdeenViewModel.kt:612-669 | Zustand | mittel | behoben | KI-Wiederholen verdoppelt die Nutzerfrage |
| F04 | 1 | Diktat | GroqTranscriber/WhisperHallucinationFilter + Einstellungen | Oberflächenlogik | mittel | behoben | 4 Filter-Schalter ohne jede Wirkung |
| F05 | 1 | UI-Rest | StimmeScreen.kt:74 | Lebenszyklus | mittel | behoben | Aufnahme läuft beim Verlassen weiter |
| F06 | 1 | UI-Erfassen/Detail | ErfassenScreen.kt:101-103, DetailScreen IdeeBearbeiten | Lebenszyklus | mittel | behoben | Rotation verliert Eingaben im Race |
| F07 | 1 | Vorlese-Pipeline | SprechText.kt:249-254 | Anzeige/Vertrag | niedrig | behoben | „12ter Mai“ statt „zwölfter Mai“ |
| F08 | 1 | Diktat | Diktat.kt:109 | Grenzen | niedrig | behoben | coerceIn-Crash bei 1-Byte-Rest |
| F09 | 1 | Datenmodell | BestandsMigrationTest.kt:19 | Test falsch | niedrig | behoben | Test erwartet Schema 3, Code ist 4 |
| F10 | 1 | UI-Detail | IdeenViewModel.kt:391-405 | Oberflächenlogik | niedrig | behoben | Falsche Lösch-Meldung ohne Paar |
| F11 | 2 | UI-Liste | ListenScreen.kt:750 | Oberflächenlogik | niedrig | behoben | Löschdialog lügt bei Zusatz-Kategorie |
| F12 | 2 | UI-Liste | ListenScreen.kt:1085 | Oberflächenlogik | mittel | behoben | Entwurf-Treffer als „Offen“ etikettiert |
| F13 | 2 | UI-Detail | DetailScreen.kt:721 | Barrierefreiheit | niedrig | behoben | Cursor blinkt trotz reduzierter Bewegung |
| K1 | 1 | KI-Chat | EinstellungenScreen + SecureSettings | Klärungsbedarf | hoch | offen | Gemini-Zugang ohne Code-Wirkung (neue Funktion nötig?) |
| K2 | 1 | Vorlese-Pipeline | VorleseDienst.kt:241-250 | Klärungsbedarf | mittel | offen | beenden-vor-start verwaist Dienst (Fix riskant) |

Runden: R1 Stufe 1+2, alle Bereiche geprüft (Dateiliste = Karte), 10/10/0/10, Zähler 0.
R2 Stufe 3, alle Bereiche geprüft (Fix-Verifikation + ListenScreen-Rest, DetailScreen-Rest, StimmenDropdown), 3/3/0/3, Zähler 0.
R3 Stufe 4 (Invarianten/Gegenbeweis: Abgleich, KI-Verlauf, Entwurf-Sitzung, Filter; Bausteine/Knopf3D voll), 0 neue, Zähler 1.
R4 Stufe 5 VOLLRUNDE (Blickwinkel ungeduldiger Benutzer: Doppelklick, Abbruch, leere Eingaben, Rotation über alle Bereiche), 0 neue, Zähler 2 → abgeschlossen.
Build nach Charge: assembleSchnell erfolgreich (nur bekannte Deprecation-Warnungen). Version 1.6.3 auf Gerät R3GL7073MLM installiert.
Restrisiko (kein Fund, gewollte Semantik): ms-gleicher angelegtAm zweier verschiedener Ideen gilt als Dublette (Kommentar Sicherung.kt:146-149).
Abgelehnt (Stichprobe): rotatedVoice-Startindex, FTS-Sonderzeichen→leere Treffer, Entwurfs-Race bei Sitzungswechsel, Aufnahme-Neustart-Race (ms-Fenster, nur verkürzt).
