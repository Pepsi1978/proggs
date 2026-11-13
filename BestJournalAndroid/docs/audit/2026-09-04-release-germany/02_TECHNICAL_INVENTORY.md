# 02 – Technische Bestandsaufnahme

**Prüfgegenstand:** Best Journal Android (`com.bestjournal.app`), 0.21.17 (298) · **Projektpfad:** `C:\Users\barwa\proggs\BestJournalAndroid`
**Prüfstand:** Commit `01b200085a7067123e5e21aba8021bc9193033a6`, `main` · **Prüfdatum:** 04.09.2026 · **Modell:** Claude Fable 5.1
**Beweisklassen:** [T] technisch bestätigt · [D] dokumentierte Anbieteraussage · [R] rechtliche Bewertung · [A] plausible Annahme · [U] ungeklärte externe Tatsache

---

## 1. Manifest (Quelle und zusammengeführt) [T]

**Quelle** (`app/src/main/AndroidManifest.xml`): `INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO`, `CAMERA`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`; Features Kamera/Autofokus/Mikrofon `required=false`; `queries` nur `com.google.android.gms`; `allowBackup=true` mit `backup_rules.xml`/`data_extraction_rules.xml`; `networkSecurityConfig` (Klartext verboten, nur System-CAs); eine exportierte Activity (Launcher); vier nicht exportierte Receiver (Erinnerungen), ein exportierter `BootReminderReceiver` (BOOT_COMPLETED/TIMEZONE/TIME_SET, ohne Extras-Verarbeitung); FileProvider (`files-path photos/`, nicht exportiert); Meta-Daten `firebase_analytics_collection_enabled=false`.

**Zusammengeführt** (`app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`), zusätzlich aus Bibliotheken: `USE_BIOMETRIC`, `USE_FINGERPRINT`, `com.android.vending.BILLING`, `WAKE_LOCK`, `BIND_GET_INSTALL_REFERRER_SERVICE`, **`com.google.android.gms.permission.AD_ID`**, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_AD_ID`, `com.google.android.c2dm.permission.RECEIVE`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Exportierte Bibliothekskomponenten sind nur `RevocationBoundService` (mit Berechtigung) und `ProfileInstallReceiver` (mit `DUMP`-Berechtigung) – Standard, unbedenklich. Kein `android:debuggable`, keine Foreground-Service-Typen, keine Deep Links/App Links, kein `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`, kein Health Connect, keine Standortberechtigung.

## 2. Gradle, Geheimnisse, Build [T]

- `app/build.gradle.kts`: `syncSecretsFromSk` kopiert `google-services*.json` und Keystores aus `~/SK/BestJournalAndroid/` vor `preBuild`; Release-Signatur aus `keystore.properties` (Werte nicht ausgegeben). Debug-Keystore-Passwort „android“ im Klartext (Standard-Debug-Key, unkritisch). `isMinifyEnabled=true`, `isShrinkResources=true`, `buildConfig=true`, keine `buildConfigField`/`resValue`/`manifestPlaceholders` mit Geheimnissen. `noCompress` für `.onnx`/`.txt`.
- `proguard-rules.pro`: Keep-Regeln für Moshi, Retrofit, Drive, Room, Credential Manager, sherpa-onnx (JNI), Billing, Firebase, Hilt; **keine** Regel zum Entfernen von `Log`-Aufrufen.
- Mapping-Datei nach Build: `app/build/outputs/mapping/release/mapping.txt` (93 MB) – für Play-Upload vorhanden.
- `gradle.properties`: nur JVM-Args/AndroidX; keine Geheimnisse. `local.properties`: nicht im Repo.
- Repositories: google, mavenCentral, gradlePluginPortal (`FAIL_ON_PROJECT_REPOS`).

## 3. Abhängigkeiten (releaseRuntimeClasspath, 1.414 Zeilen, Auswahl) [T]

| Bibliothek | Version | Zweck / tatsächliche Nutzung | Netzwerk | Kennungen | Datenschutzrelevanz |
|---|---|---|---|---|---|
| firebase-ai | 17.10.1 | Gemini-Aufrufe (`FirebaseAiService`, Backend `googleAI()`) | ja (Google) | App-Check-Token, Installation-ID | Journaltexte → Google USA (Einwilligung) |
| firebase-analytics / play-services-measurement | 23.2.0 | Nutzungsereignisse (`AnalyticsTracker`, 60+ Events, keine Inhalte) | ja | Instance-ID, **AD_ID** (Berechtigung im Manifest) | Opt-in, Standard aus; Werbe-ID-Deklaration nötig |
| firebase-config | 23.0.1 | liefert `groq_api_key` | ja | Installation-ID | Schlüsselverteilung (BJA-SEC-001) |
| firebase-appcheck-playintegrity / play integrity | 19.0.2 / 1.4.0 | Geräteintegrität für Firebase AI/Functions | ja | Integritäts-Token | berechtigtes Interesse |
| firebase-functions | 22.1.0 | `getSubscriptionStatus` (europe-west1) | ja (EU) | Purchase-Token, Produkt-ID | Kaufstatus |
| firebase-iid / c2dm | transitiv | Installation-ID; FCM-Empfangsberechtigung vorhanden, **kein** Messaging-Code | – | Installation-ID | inaktiv |
| play-services-ads-identifier | 18.0.0 | transitiv (Analytics) – von der App nicht direkt genutzt | – | Werbe-ID | Befund BJA-GP-001 |
| play-services-location | 19.0.0 | transitiv (placereport) – **nicht genutzt**, keine Berechtigung | – | – | inaktiv |
| billing-ktx | 7.1.1 | Abo/Lifetime (`BillingManager`) | ja (Play) | Purchase-Token | Vertrag |
| review-ktx | 2.0.2 | In-App-Bewertung (`InAppReviewHelper`) | ja (Play) | – | gering |
| credentials + googleid + play-services-auth | 1.5.0-alpha06 / 1.1.1 / 21.3.0 | Google Sign-In (E-Mail, Name, Avatar lokal gespeichert); Gmail-/Drive-Token (`GoogleAuthUtil`) | ja | Google-Konto | Einwilligung |
| google-api-services-drive + api-client | v3-rev20241206 / 2.7.0 | appDataFolder-Backup (Scope `DRIVE_APPDATA`) | ja | – | Einwilligung; Apache HttpClient 4.5.14 transitiv |
| okhttp + logging-interceptor | 4.12.0 | Groq (Retrofit), Edge-TTS (WebSocket); Logging nur `BuildConfig.DEBUG` | ja | – | – |
| retrofit + moshi | 2.11.0 / 1.15.1 | Groq-API | – | – | – |
| ktor-client | 3.0.3 | transitiv (firebase-ai) | – | – | – |
| sherpa-onnx (AAR) + onnxruntime | 1.12.34 | lokales Whisper (Offline) | nein | – | keine Übermittlung |
| security-crypto | 1.1.0-alpha06 | EncryptedSharedPreferences (deprecated) | – | – | lokal |
| biometric | 1.2.0-alpha05 | App-Sperre | – | – | lokal (System) |
| room | 2.7.0 | drei SQLite-DBs, unverschlüsselt | – | – | lokal |
| coil, lottie, compose, navigation, hilt, coroutines | – | UI | nein | – | – |
| datastore-preferences | 1.1.7 | transitiv (firebase) | – | – | – |

**Nicht eingebunden (verifiziert):** Crashlytics, Sentry, Werbe-SDKs, Attributions-SDKs, Firebase Auth, Firebase Messaging (nur transitive Empfangsberechtigung), WebView-Tracking. **Eingebunden, aber ungenutzt:** play-services-location (transitiv), ads-identifier (transitiv), FCM-Empfangsberechtigung – Befund BJA-GP-001 bzw. Hinweis.

## 4. Ressourcen, Sprachen, Assets [T]

- 27 Sprachverzeichnisse (`values` = Deutsch als Standard, plus 26 `values-*`). Basis 1.117 Schlüssel; jede Übersetzung 1.101 Schlüssel; die 16 fehlenden sind ausschließlich `translatable="false"`-Schlüssel (URLs, Copyright, Debug-Seed) – keine rechtsrelevanten Lücken. Krisenhilfe-Strings in allen Sprachen mit deutschen Nummern (für DE-Start korrekt).
- Kein `locales_config.xml`, keine `resourceConfigurations`; `BestJournalApp.onCreate` setzt Per-App-Sprachüberschreibungen zurück → App-Sprache folgt immer der Systemsprache.
- Assets: `legal/<sprache>/` (de: DATENSCHUTZ/IMPRESSUM/NUTZUNGSBEDINGUNGEN.html; en: Vollversionen; 24 weitere Sprachen: Kurzfassungen mit Link auf de/en), `whisper/` (151 MB Modelle).
- XML-Konfiguration: `backup_rules.xml`/`data_extraction_rules.xml` schließen Dashboard-/Rückblick-DBs und `entropy_journal_secure_prefs` aus; Haupt-DB `entropy_journal_db` und Klartext-Prefs (`privacy_gate`, `entropy_theme_quick`) werden gesichert. `file_paths.xml`: nur `photos/`. `network_security_config.xml`: Klartext verboten.
- Schrift: `caveat.ttf`, `caveat_bold.ttf` (SIL OFL, ohne Lizenzdatei – BJA-OSS-001). Adaptive Icons ohne Monochrome-Layer (Lint).

## 5. Navigation, Erststart, Oberflächen [T]

`splash` → (`consent` → `legal/*`) → `onboarding` → `main` (Pager: Rückblick, Dashboard, Tagebuch, Einstellungen) → `entry_detail`, `paywall`. Rechtstexte sind vor dem Onboarding aus dem Consent-Screen erreichbar (Asset-WebView ohne JavaScript, externe Links im Systembrowser). Kein Konto-Zwang. Biometrie-Sperre optional (60-s-Toleranz).

## 6. Datenbanken, Prefs, Dateien [T]

- `entropy_journal_db` (v12, Schema exportiert): `journal_entries` (rawText, improvedText, displayText, moodTag, adviceCategoryTags, summary, title, followUpText, isSynced), `entry_photos` (Pfad, isVideo), `entry_follow_ups`. Keine Verschlüsselung (kein SQLCipher).
- `dashboard_db` (Analyse-Blöcke, Belastungswerte, Top-Maßnahmen), `retrospective_db` (Wochen/Monats/Jahres-Rückblicke) – regenerierbar, `fallbackToDestructiveMigration`.
- EncryptedSharedPreferences `entropy_journal_secure_prefs` (Einstellungen, Consent-Flags, Google-Konto, Kaufstatus); Klartext-Prefs `privacy_gate` (Groq/Gemini/TTS-Consent) und `entropy_theme_quick`.
- Dateien: `filesDir/photos/*.jpg|mp4` (+ Kopie in DCIM/Camera bei Kameraaufnahmen), `cacheDir/recording_*.wav` (gelöscht nach Transkription), `cacheDir/tts_audio.mp3` (gelöscht nach Wiedergabe), `cacheDir/drive_merge_temp.db` (Sync), PDF-Export über SAF (`exportToPdf(uri)`).

## 7. Netzwerk, Hintergrund, Benachrichtigungen [T]

- Hosts im App-Code: `api.groq.com` (Bearer-Key aus Remote Config), `wss://speech.platform.bing.com` (Edge-TTS), `gmail.googleapis.com` (Feedback/Widerruf/Meldung mit Nutzer-OAuth), `www.googleapis.com` (Drive/Gmail-Scopes), `findahelpline.com`, `play.google.com` (Links). SDK-Hosts: Firebase (AI, Remote Config, Installations, Analytics, App Check, Functions europe-west1), Play Integrity, Play Billing. Zeitüberschreitungen Groq 30/60/60 s, TTS 10/45 s + Watchdog.
- Hintergrund: keine WorkManager-Worker, keine Foreground Services. AlarmManager inexakt (exakter Pfad tot, BJA-GP-004); Boot-/Zeitzonen-Receiver plant Alarme neu.
- Benachrichtigungen: vier Kanäle; Inhalte sind feste, generische Strings (keine Tagebuchinhalte auf dem Sperrbildschirm).

## 8. Logging, Debug, Tests [T]

- `Log.*` in 25 Dateien (383 Aufrufe); Inhalte mit PII in 7 Stellen (BJA-SEC-002). OkHttp-Body-Logging nur Debug. Debug-Seeder (`TestDataSeeder`) nur `BuildConfig.DEBUG`. App Check Debug-Provider nur Debug (Reflection). Kein `printStackTrace`, kein `println`.
- Unit-Tests: 34 (AiUsageTracker), alle grün. Keine Instrumentierungstests.

## 9. Audio-Pipeline [T]

`AudioRecord` 16 kHz mono PCM → WAV in `cacheDir` → `WavSpeechAnalyzer` (nur RMS-Energie je 20-ms-Frame, keine Stimm-/Emotionsmerkmale) → Groq (verbose_json, Confidence-Filter) oder lokal (sherpa-onnx, 30-s-Chunks) → WAV gelöscht (`JournalViewModel.kt:485,504`). Maximaldauer 10 min. Kein `SpeechRecognizer`.

## 10. Native Bibliotheken / 16 KB [T]

24 `.so` in 4 ABIs (arm64-v8a, armeabi-v7a, x86, x86_64): `libonnxruntime`, `libsherpa-onnx-{jni,c-api,cxx-api}`, `libandroidx.graphics.path`, `libdatastore_shared_counter`. Alle `PT_LOAD`-Segmente mit Ausrichtung 16384 → 16-KB-kompatibel.

---

## Anhang A – Abdeckung

Getrackte Dateien im Projekt: 821 (davon `app/src` 287, `.android-shield` 268 Übersetzungs-Arbeitsdateien, `app/schemas` 16, `docs/audit` 9). Vollständig gelesen: alle Gradle-/Manifest-/XML-Konfigurationsdateien (11), `proguard-rules.pro`, 39 Kotlin-Dateien vollständig (App, Activity, Navigation, DI, Constants, Consent/Splash/LegalDocument, AI-Service, Groq, Transkription, Whisper, RateLimiter, Audio, EdgeTTS, Analytics, Krisenhilfe, KI-Badges, UseCases Improve/Summarize/Analyze, Entities, DAOs-Auszug, Cloud Function, Remote-Services, Auth), 14 große Dateien gezielt per Zeilenbereich (SettingsScreen 5159 Z., DashboardScreen 3941 Z., RetrospectiveScreen, JournalScreen, JournalViewModel, SettingsViewModel, DashboardViewModel, RetrospectiveViewModel, AdviceRepository, GenerateRetrospectiveUseCase, SyncWithDriveUseCase, DriveBackup/RestoreManager, PhotoRepository, PdfExporter-Kopf), `strings.xml` (1.499 Z.) gezielt (Consent, Gates, Prompts, Krisenhilfe, Alter, Paywall, URLs), 26 Übersetzungen per Skript verglichen, alle drei deutschen Rechtstexte (Assets) vollständig, englische und gehostete Fassungen per Hash verglichen, `docs/*.md`, `play-store-metadata/*.md`, `SESSION-RULES.md`, Vorgänger-Audit v8 (Kopf). Binär nicht gelesen: `sherpa-onnx-1.12.34.aar`, `*.onnx`, `*.ttf`, `*.png`, Keystores (bewusst nicht geöffnet). Nicht gelesen: `.android-shield/**` (Übersetzungs-Arbeitsdateien), `übersetzung.md`, `icon-picker*.html`, `MONETARISIERUNG-*.md` (nur Stichwortsuche).

## Anhang B – Ausgeführte Befehle (alle erfolgreich, Exit 0)

1. `git rev-parse --show-toplevel; git branch --show-current; git log -1; git status --short` (16:53)
2. `git ls-files BestJournalAndroid | grep -iE "keystore|jks|local.properties|.env|google-services"` → nur README
3. `gradlew.bat :app:dependencies --configuration releaseRuntimeClasspath` → 1.414 Zeilen
4. Secret-Scan: `git grep` (Muster AIza/gsk_/sk-/PRIVATE KEY/Passwörter) + `git log -p --all -- BestJournalAndroid` (maskiert) → 1 historischer Firebase-Key (BJA-SEC-005); `gitleaks`/`trufflehog` nicht installiert
5. `gradlew.bat :app:lintRelease :app:testReleaseUnitTest --continue` → BUILD SUCCESSFUL 3m16s; Lint 1.133 Meldungen (525 TypographyDashes, 221 TypographyEllipsis, 113 UnusedResources, 41 GradleDependency, 2 MissingPermission, 2 TrustAllX509TrustManager in google-http-client, u. a.); Tests 34/34
6. `gradlew.bat :app:bundleRelease` → BUILD SUCCESSFUL 2m56s; `app-release.aab` 158.848.464 Bytes, signiert (Release-Keystore aus ~/SK)
7. Python-AAB-Analyse (ZIP-Liste, ELF-`PT_LOAD`-Ausrichtung) → 24 `.so`, alle 16 KB; größte Einträge: Whisper-Decoder 124 MB, Mapping 88 MB, Encoder 27 MB
8. Merged-Manifest-Auswertung (`grep`)
9. `curl` auf 6 gehostete URLs (17:11) → alle HTTP 404
10. Python-Vergleich Rechtstexte (App-Asset vs. gehostet, Hash) → DE/EN Datenschutz identisch, AGB/Impressum nur Navigationszeile abweichend
11. Python-Übersetzungsabdeckung (27 Sprachen)
12. `pip install pypdf` + Extraktion DSK-Muss-Liste/WP248 aus lokalen PDF-Kopien
