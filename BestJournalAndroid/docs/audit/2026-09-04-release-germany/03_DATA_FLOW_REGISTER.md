# 03 – Datenflussregister

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

Begriffe: *Auftragsverarbeiter* = Dienstleister, der Daten nur im Auftrag und nach Weisung verarbeitet. *Drittland* = Staat außerhalb EU/EWR (hier: USA). *DPF* = EU-US Data Privacy Framework (Angemessenheitsbeschluss der EU-Kommission für zertifizierte US-Unternehmen). *SCC* = Standardvertragsklauseln der EU-Kommission.

---

## 1. Rollenverteilung (entscheidet über die Bewertung)

| Frage | Antwort | Beweis |
|---|---|---|
| Eigener Server des Entwicklers? | Nur eine Cloud Function (`getSubscriptionStatus`, europe-west1) für Kaufstatus; keine Speicherung von Inhalten oder Nutzerkonten. Die App hat **kein serverseitiges Nutzerkonto** (Google-Sign-In wird nur lokal gespeichert, kein Firebase Auth). | `functions/index.js`, `AuthRepository.kt` [T] |
| KI-Schlüssel | Entwicklerschlüssel: Gemini über Firebase-Projekt (App Check), Groq-Schlüssel per Remote Config an den Client. Kein Bring-Your-Own-Key. → Der Entwickler ist **Verantwortlicher**, Google (Gemini, Firebase) und Groq sind **Auftragsverarbeiter** – sofern ein Auftragsverarbeitungsvertrag besteht [U]. Microsoft (Edge-TTS): **kein Vertrag** → Übermittlung an einen Dritten (kein Auftragsverarbeiter). | `Constants.kt`, `FirebaseAiService.kt`, `EdgeTtsPlayer.kt` [T]; DSE 5.2a [T] |
| Identifizierung des Nutzers durch den Entwickler | Nicht möglich (Art. 11 DSGVO): Der Entwickler hält keine Nutzerdatenbank. Betroffenenrechte (Auskunft/Löschung) werden praktisch durch Funktionen in der App erfüllt (Export, Löschen, Widerruf); per E-Mail nur, wenn der Nutzer sich selbst identifiziert (Feedback-Postfach). Die DSE erklärt das nicht ausdrücklich (Empfehlung in 08). | [T]+[R] |
| Reiner Offline-Pfad | Ohne Cloud-Einwilligungen verlassen **keine Inhalte** das Gerät – sobald die Blocker BJA-DS-001/002 behoben sind. Aktuell verlassen Inhalte das Gerät auch ohne Einwilligung (Groq in Testphase, Gemini-Automatik). Unabhängig von Inhalten kontaktieren Firebase-SDKs beim Start Google (Installations-ID, App Check, Remote Config; Analytics nur nach Opt-in). Play Billing/Review kontaktieren Google Play. | [T] |

## 2. Register je Datenart

Legende Spalten: Herkunft · Auslöser · Zweck · Speicherort · Empfänger · Land · Transport · Speicherung · Training · Weitergabe/Unterauftrag · Löschung · Backup · Rechtsgrundlage lt. Text · Nachweis · Klasse

### 2.1 Tagebuchtexte (Rohtext, verbesserter Text, Nachträge, Titel, Zusammenfassung, Stimmungs-Tag, Kategorien)
- Herkunft: Nutzereingabe (Tastatur/Diktat) · Auslöser: Speichern · Zweck: Kernfunktion · Speicherort: `entropy_journal_db` (unverschlüsselt, App-Sandbox) · Empfänger: keiner (lokal) – **außer** bei KI-Funktionen (2.2), Drive-Backup (2.12), System-Backup (2.13), Teilen/Export (2.14) · Löschung: Eintrag löschen (CASCADE Fotos/Nachträge), „Alle Daten löschen“, Kontolöschung, Deinstallation · Backup: ja (System-Backup, Drive) · Rechtsgrundlage lt. DSE: Kernfunktion (Art. 6 Abs. 1 lit. b implizit; DSE nennt keine ausdrücklich für lokale Speicherung → Empfehlung 08) · Nachweis: `JournalEntryEntity.kt`, `JournalEntryDao.kt:75-79`, `SettingsViewModel.kt:945-990` · [T]

### 2.2 KI-Anfragen an Google Gemini (Prompt = System-Prompt aus Strings + **alle** Einträge mit Datum, bzw. Einträge des Zeitraums, bzw. ganzer Eintrag mit Nachträgen; bei eigenen Profilen zusätzlich Nutzer-Prompt; bei Aktualisierung bisherige Belastungswerte je Kategorie)
- Auslöser: manuell (Dashboard aktualisieren, Text verbessern, Rückblick neu) **und automatisch** (Speichern → Zusammenfassung; Auto-Dashboard 3 s nach Speichern; Rückblick-Init beim Öffnen; Profilwechsel) · Zweck: Analyse/Zusammenfassung/Rückblick/Textverbesserung · Speicherort der Antworten: `dashboard_db`, `retrospective_db`, `journal_entries.summary/title/improvedText` · Empfänger: Google LLC (Gemini Developer API via Firebase AI Logic; Modelle gemini-2.5-flash-lite, gemini-3.1-flash-lite, gemini-flash-lite-latest) · Land: USA („in jedem Land“ zwischengespeichert laut Terms) · Transport: TLS + App-Check-Token · Speicherung beim Anbieter: EWR-Regel → Datenregeln der bezahlten Dienste: keine Produktverbesserung, zeitlich begrenzte Protokollierung zur Missbrauchserkennung [D]; genaue Frist [U] · Training: nein (EWR-Regel) [D]; Bedingung Cloud-Billing [U] · Unterauftragsverarbeiter: Google-Subprozessoren lt. DPA [U] · Löschung beim Anbieter: nicht durch Nutzer steuerbar · Metadaten: keine Nutzer-/Installationskennung im Prompt; Gerätesprache als Antwortsprache-Instruktion; Zeitstempel der Einträge · Rechtsgrundlage lt. DSE: Einwilligung Art. 6 Abs. 1 lit. a + Art. 9 Abs. 2 lit. a (Gate) · Nachweis: `AnalyzeEntropyUseCase.kt:20-37`, `AdviceRepository.kt:239-262,416-438`, `GenerateRetrospectiveUseCase.kt:245-271`, `SummarizeEntryUseCase.kt:28-91`, `ImproveTextUseCase.kt:23-66`, `FirebaseAiService.kt:40-60` · [T]/[D]/[U] · **Befunde:** BJA-DS-002, DS-005

### 2.3 Sprachaufnahmen (WAV 16 kHz mono)
- Herkunft: Mikrofon · Auslöser: Mikrofon-Taste · Zweck: Transkription · Speicherort: `cacheDir/recording_<ts>.wav`, gelöscht nach Transkription (Erfolg und Fehler) · Empfänger: Groq, Inc. (Whisper large-v3-turbo) **nur wenn** Abo/Testphase/Debug **und** Datei ≤ 25 MB, sonst lokal (sherpa-onnx). Consent-Prüfung fehlt (BJA-DS-001) · Land: USA · Transport: TLS, Bearer-Key · Metadaten: Dateiname, Sprachcode, IP-Adresse des Nutzers (Direktaufruf) · Speicherung bei Groq: bis zu 30 Tage (Zuverlässigkeit/Missbrauch) sofern ZDR nicht aktiv [D]; Ort: Google-Cloud-Speicher in den USA [D] · Training: nein [D] · Unterauftragsverarbeiter: Google Cloud [D] · Löschung beim Anbieter: nicht durch Nutzer · Biometrie: **keine** – nur Transkription; `WavSpeechAnalyzer` misst lediglich Lautstärke je Frame (kein Stimmabdruck, keine Emotion) · Rechtsgrundlage lt. DSE: Einwilligung; Drittland: SCC (DSE) [U] · Nachweis: `AudioRecorderHelper.kt`, `TranscriptionRepository.kt:43-125`, `WavSpeechAnalyzer.kt:42-60`, `JournalViewModel.kt:374,485,504` · [T]/[D]

### 2.4 Transkriptionen
- Ergebnistext aus 2.3 → wird zu 2.1; ggf. automatisch an Gemini zur Verbesserung (2.2), wenn `PREF_TEXT_IMPROVEMENT_DEFAULT` aktiv (Standard aus). Bis zu 40 Zeichen je verworfenem Segment im Logcat (BJA-SEC-002). [T]

### 2.5 Vorlesen (Text-to-Speech)
- Herkunft: Eintrags-/Dashboard-/Rückblickstext · Auslöser: Vorlesen-Taste (nur bei aktiviertem TTS + Gate) · Empfänger: Microsoft (Consumer-Endpunkt `speech.platform.bing.com`, WebSocket, gefälschte Browser-Kennung) · Land: USA/unbekannt · Vertrag: keiner · Speicherung/Training: unbekannt [U] · Metadaten: IP, Stimme, Sprache, zufällige Verbindungs-IDs, `muid`-Cookie zufällig · Lokal: `cacheDir/tts_audio.mp3`, gelöscht nach Wiedergabe · Rechtsgrundlage lt. DSE: Art. 6 Abs. 1 lit. a + Art. 49 Abs. 1 lit. a · Analytics-Ereignisse `tts_failure`/`tts_watchdog_fired` (nur Opt-in) · Nachweis: `EdgeTtsPlayer.kt:75-213` · [T]/[U] · **Befund:** BJA-DS-003, RT-001(a)

### 2.6 Persönliche Ziele, Stimmungen, Gesundheits-/Psycho-Angaben, Sexualität/Religion/Politik
- Nicht strukturiert abgefragt (nur `moodTag` und Onboarding-Ziele lokal); können in freien Texten/Aufnahmen enthalten sein und werden dann mit 2.1–2.5 verarbeitet. KI-Prompts werten ausdrücklich Gefühle, Belastung, Denk-/Verhaltensmuster, Ziele aus (Art.-9-Relevanz, siehe 04/17). [T]/[R]

### 2.7 Bilder/Videos
- Herkunft: Kamera (FileProvider) oder Systemauswahl · Speicherort: `filesDir/photos/` **plus Kopie in DCIM/Camera** bei Kameraaufnahmen (öffentliche Galerie) · Empfänger: nur Drive-Backup (Opt-in, Fotos/Videos je Schalter Standard an) · nie an KI · Löschung: mit Eintrag (App-Ordner), Galerie-Kopie bleibt · Nachweis: `PhotoRepository.kt:34-121`, `SyncWithDriveUseCase.kt:101-135` · [T] · **Befund:** BJA-DS-008

### 2.8 Konto-/Profildaten, E-Mail-Adresse
- Google Sign-In (Credential Manager): E-Mail, Anzeigename, Avatar-URL → nur in EncryptedSharedPreferences; keine Übermittlung an den Entwickler. Verwendung: Drive-Token, Gmail-Token (Feedback/Widerruf/KI-Meldung). Bei Feedback wird die E-Mail-Adresse als Absender an `dev.app.support@gmail.com` übermittelt (Empfänger: Entwickler; Transport: Gmail-API → Google). Rechtsgrundlage lt. DSE: Art. 6 lit. b (Sign-In), lit. a/f (Feedback). Nachweis: `AuthRepository.kt`, `FeedbackSender.kt`, `RevokeSender.kt` · [T]

### 2.9 Geräteinformationen, IP-Adressen, Kennungen
- IP-Adresse des Nutzers sichtbar für: Google (Firebase-Dienste, Gemini, Play), Groq (Direktaufruf), Microsoft (Direktaufruf). Firebase-Installations-ID (alle Firebase-Dienste), App-Check/Play-Integrity-Token, **Werbe-ID** (Berechtigung vorhanden; Analytics nutzt sie nach Opt-in), Analytics-App-Instance-ID. Keine IMEI/MAC. Rechtsgrundlage lt. DSE: Art. 6 lit. f (App Check, Remote Config, Review), lit. a (Analytics). § 25 TDDDG: Zugriff auf Kennungen im Endgerät – für App Check/Remote Config/Installations als unbedingt erforderlich vertretbar [R], für Analytics/AAID Einwilligung (vorhanden). Nachweis: `BestJournalApp.kt:39-79`, Merged Manifest:44-46 · [T]/[R] · **Befund:** BJA-GP-001

### 2.10 Absturz-/Diagnosedaten
- Kein Crashlytics/Sentry. Google Play sammelt Vitals unabhängig (Play-Konto). Logcat enthält PII (BJA-SEC-002). [T]

### 2.11 Nutzungsstatistiken (Firebase Analytics)
- Opt-in (Standard aus, SDK-seitig deaktiviert bis Consent). Ereignisse ohne Inhalte (z. B. entry_created, paywall_shown, churn_reason_selected mit 4 festen Kategorien, tts_failure mit Stimme/Fehlertext ≤100 Zeichen, onboarding_goals_selected mit gewählten Zielen ≤100 Zeichen). Empfänger Google (USA, DPF+SCC lt. DSE). Speicherdauer 14 Monate (Standard) [D-Kenntnis, nicht am Prüftag verifiziert]. Widerruf: Einstellungen. Nicht zurückgesetzt bei Kontolöschung (BJA-DS-010). Nachweis: `AnalyticsTracker.kt`, `ConsentViewModel.kt:134` · [T]

### 2.12 Google-Drive-Backup (Opt-in)
- Inhalt: komplette `entropy_journal_db` (unverschlüsselt), Fotos/Videos, Custom-Analysen-JSON · Ziel: `appDataFolder` des eigenen Google-Kontos (Scope `DRIVE_APPDATA`) · Empfänger: Google (Drive; für Nutzer-Konto Google Ireland) · Transport: TLS · Löschung: „Cloud-Backup löschen“, Kontolöschung (zweistufig verifiziert), Google-Konto → Apps · Rechtsgrundlage lt. DSE: Einwilligung · Nachweis: `SyncWithDriveUseCase.kt`, `DriveBackupManager.kt:405-455` · [T] · **Befund:** BJA-DS-009 (Verschlüsselungsaussage)

### 2.13 Android-System-Backup (Gerätesetting)
- `allowBackup=true`; enthält `entropy_journal_db` und Klartext-Prefs (`privacy_gate`, Theme), **ausgeschlossen** Dashboard-/Rückblick-DB und EncryptedSharedPreferences · Ziel: Google-Backup-Speicher des Gerätekontos · DSE 5.3a beschreibt das korrekt inkl. Stimmungswert-Hinweis · Nachweis: `backup_rules.xml`, `data_extraction_rules.xml` · [T]

### 2.14 Export/Teilen/Zwischenablage
- PDF-Export über Storage Access Framework (Nutzer wählt Ziel), Fotos optional; Teilen von Einträgen, Dashboard-Analysen und Rückblicken per `ACTION_SEND` (Klartext; KI-Marker „✨ KI-verbessert“ nur bei verbesserten Texten enthalten, bei Dashboard/Rückblick nicht). Keine programmatische Zwischenablage. Nachweis: `PdfExporter.kt`, `ShareEntryDialog.kt:262-280`, `DashboardScreen.kt:3902-3912` · [T] · **Befund:** BJA-KI-002

### 2.15 Kaufdaten
- Play Billing: Purchase-Token, Produkt-ID, Preis → lokal (EncryptedPrefs) und an Cloud Function `getSubscriptionStatus` (europe-west1) → Google Play Developer API (Service-Account-Secret). Keine Inhalte. Speicherung in Cloud-Logging (Token gekürzt auf 12 Zeichen in Fehlerlogs). Rechtsgrundlage lt. DSE: Art. 6 lit. b. Nachweis: `BillingManager.kt`, `SubscriptionStatusService.kt`, `functions/index.js:40-206` · [T]

### 2.16 Benachrichtigungsinhalte
- Feste Strings, keine Inhalte; lokal erzeugt. [T]

### 2.17 Nicht vorhanden (verifiziert)
Kalender, Wearables/Health Connect, Standort, Kontakte, Anzeigen, nutzergenerierte öffentliche Inhalte, WebView-Fremdinhalte (nur lokale Assets ohne JavaScript). [T]

## 3. Consent-Flow-Trace (aus dem Code)

1. **Splash** (`SplashScreen.kt:91-96`): Start-Taste → wenn `consent_shown` fehlt → `consent`; sonst wenn Onboarding fehlt → `onboarding`; sonst `main`. Keine Prüfung der Policy-Version (BJA-DS-004).
2. **Consent-Screen** (`ConsentScreen.kt:98-420`): Titel, drei Versprechen, drei gleich gestaltete Buttons „Alle akzeptieren“ / „Alle ablehnen“ / „Manuelle Auswahl“ (Bottom-Sheet mit fünf Schaltern: Analytics, Groq, Gemini, TTS, Drive; alle Standard aus; „Do not sell“ nur bei en-US-Locale). Links zu Datenschutz/AGB/Impressum (Asset-WebView) und Online-Links (404). Speicherung: `consent_shown`, `consent_policy_version=3.2`, `consent_timestamp`, Analytics/Drive-Flags, `privacy_gate`-Flags; Analytics sofort umgeschaltet; TTS-Schalter gespiegelt. Nachweis: `ConsentViewModel.kt:123-179`.
3. **Onboarding** (Ziele, Funktionsübersicht, Premium-Hinweis, keine Altersabfrage) → `main`.
4. **Per-Use-Gates**: Groq beim Mikrofon (Ablehnen startet trotzdem – DS-001), Gemini bei manuellen Aktionen (Dashboard aktualisieren, Text verbessern, Rückblick neu, Profilwechsel-Regeneration), TTS bei Vorlesen. Gate-Zustimmung wird in `privacy_gate` persistiert; Ablehnen wird nicht persistiert (Dialog erscheint erneut).
5. **Automatische Läufe ohne Gate**: Zusammenfassung/Titel beim Speichern und Nachholen, Auto-Dashboard, Rückblick-Init (DS-002).
6. **Widerruf**: Einstellungen → Datenschutz-Sheet (dieselben fünf Schalter, Do-not-sell-Kaskade), Analytics-Schalter, TTS-Schalter; Groq/Gemini/TTS-Flags werden gesetzt; kein Hinweis, dass laufende automatische Läufe damit enden (sie enden erst nach DS-002-Fix).
7. **Nachweis**: Zeitstempel + Versionsnummer lokal (nur auf dem Gerät; nach Deinstallation weg – für Art. 7 Abs. 1 bei Art. 11-Konstellation vertretbar [R]).
8. **Erneute Einholung bei Änderung**: nicht implementiert (DS-004).
