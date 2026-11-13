# 19 – Fertige Aufträge für Claude Code CLI (Phase 2)

Prüfstand `01b200085a7067123e5e21aba8021bc9193033a6` · Audit vom 04.09.2026 · Projekt `C:\Users\barwa\proggs\BestJournalAndroid` (nicht BestJournalFrank).

**So verwenden:** Jeden Block einzeln als Prompt in Claude Code einfügen (Reihenfolge wie nummeriert). Jeder Block ist so formuliert, dass sofort gearbeitet werden kann. Vor Block 1 einmal den Vorspann einfügen.

---

## Vorspann (einmal vor dem ersten Auftrag einfügen)

```
Kontext: Projekt C:\Users\barwa\proggs\BestJournalAndroid (Best Journal Android, com.bestjournal.app). Das Schwesterprojekt BestJournalFrank darf nicht angefasst werden. Grundlage ist das Audit in docs/audit/2026-09-04-release-germany/ (lies zuerst 01_EXECUTIVE_RELEASE_DECISION_GERMANY.md, 13_REQUIRED_CHANGES.md und FINDINGS_REGISTER.json). Arbeitsregeln: Arbeitsbranch audit/release-germany-2026-09-04 anlegen; ein Commit je Befund oder Befundgruppe mit Befundnummer in der Commit-Message (z. B. "BJA-DS-001: Groq nur mit Einwilligung"); Audit-Berichte getrennt vom App-Code committen; kein Force-Push, keine Historie umschreiben; nach jedem Block gradlew.bat :app:lintRelease :app:testReleaseUnitTest und am Ende :app:bundleRelease ausführen und Fehler selbst beheben; Version pro Block bumpen (versionCode +1, versionName Patch) und in SettingsScreen sichtbar lassen; Rechtstexte bei Änderung mit neuem Stand-Datum (echte Systemzeit) und Policy-Version 3.3 versehen; alle 27 Sprachfassungen betroffener Strings und die EN-Vollfassungen nachziehen; jede Änderung als Diff zeigen; im FINDINGS_REGISTER.json den Status des Befunds auf "behoben" setzen. Sprache: Deutsch mit Umlauten.
```

---

## Block 1 – BJA-DS-001: Groq-Transkription nur mit Einwilligung, lokale Option (Blocker)

```
Aufgabe BJA-DS-001 (Release-Blocker). Ziel: Sprachaufnahmen dürfen nur dann an Groq gehen, wenn der Nutzer der Cloud-Transkription zugestimmt hat; sonst ausschließlich lokales Whisper.
1. In app/src/main/java/com/bestjournal/app/data/repository/TranscriptionRepository.kt (Zeilen 65-119): Vor dem Groq-Zweig prüfen: val cloudAllowed = PrivacyGateHelper.hasConsented(context, PrivacyGateHelper.CloudService.Groq). Der Groq-Zweig läuft nur, wenn cloudAllowed && (isSubscribed || isTrialActive || isDebug). Debug-Builds dürfen die Prüfung NICHT umgehen (isDebug darf nur den Abo-Check ersetzen, nie die Einwilligung).
2. In app/src/main/java/com/bestjournal/app/ui/screens/journal/JournalScreen.kt (Zeilen 236-266): Beim Ablehnen im Groq-Dialog (onDecline) explizit PrivacyGateHelper.setConsent(context, Groq, false) setzen und den Dialog beim nächsten Tippen NICHT erneut zeigen, sondern lokal transkribieren; stattdessen einen kleinen Hinweis "Lokale Erkennung aktiv – Cloud-Erkennung in Einstellungen → Datenschutz" (neuer String, 27 Sprachen). Dialog nur zeigen, solange der Nutzer noch nie entschieden hat (neuer Pref-Schlüssel groq_decision_made in PrivacyGateHelper).
3. Neue Einstellung in SettingsScreen im Bereich Datenschutz: "Sprache-zu-Text" mit zwei Optionen "Auf dem Gerät (Standard)" und "Cloud (Groq, USA) – nur Premium/Testphase". Die Option Cloud setzt den Groq-Consent, Gerät entfernt ihn. Bestehender Privacy-Sheet-Schalter bleibt und ist mit derselben Quelle synchron.
4. Strings anpassen: privacy_gate_groq_body: "sofort gelöscht" ersetzen durch "Groq speichert Aufnahmen höchstens 30 Tage zur Missbrauchs- und Störungsanalyse und nutzt sie nicht zum Training" und "Alternative: Lokale Offline-Erkennung in den Einstellungen." durch "Wenn du ablehnst, bleibt die Erkennung auf deinem Gerät (Offline-Modell)." – in allen 27 Sprachen.
5. Datenschutzerklärung (app/src/main/assets/legal/de/DATENSCHUTZ.html Abschnitt 5.1 und docs/bestjournal/privacy-de.html, plus EN-Fassungen): Absatz "Übermittlung" gemäß 13_REQUIRED_CHANGES.md A1 ersetzen; Speicherdauer "bis zu 30 Tage" (Abschnitte 5.1, 11, BIPA-Absatz in 8a).
6. Unit-Test: TranscriptionRepositoryConsentTest – ohne Consent wird groqApi nie aufgerufen (MockK).
7. Version bumpen, Lint/Tests, Commit "BJA-DS-001: Groq-Transkription nur mit Einwilligung, lokale Option", Register-Status "behoben".
```

## Block 2 – BJA-DS-002: Zentrale Gemini-Sperre (Blocker)

```
Aufgabe BJA-DS-002 (Release-Blocker). Ziel: Kein Aufruf an Google Gemini ohne Gemini-Einwilligung – auch nicht durch automatische Läufe.
1. app/src/main/java/com/bestjournal/app/data/remote/ai/FirebaseAiService.kt: Am Anfang von generateContent(): if (!PrivacyGateHelper.hasConsented(context, CloudService.Gemini)) return Result.failure(ConsentMissingException()). Neue Klasse ConsentMissingException in data/remote/ai.
2. app/src/main/java/com/bestjournal/app/ui/screens/journal/JournalViewModel.kt: Zeilen 350-361 (Backfill-Zusammenfassungen), 684-690 (Zusammenfassung beim Speichern) und 957-985 (Auto-Dashboard) nur ausführen, wenn Consent vorliegt; ConsentMissingException still ignorieren (kein Fehler-Toast).
3. app/src/main/java/com/bestjournal/app/ui/screens/retrospective/RetrospectiveViewModel.kt (init Zeilen 198-245, regenerateAll, retryGeneration) und app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardViewModel.kt (init/refreshDashboard): Ohne Consent keinen generateMissing/refreshDashboard starten; stattdessen UI-Zustand aiDisabled=true und im Rückblick-/Dashboard-Screen eine Karte "KI-Funktionen sind deaktiviert. Aktiviere Google Gemini unter Einstellungen → Datenschutz, um Rückblicke und Analysen zu erhalten." mit Button zu den Einstellungen (neue Strings, 27 Sprachen).
4. Wird der Consent in den Einstellungen widerrufen, laufende KI-Jobs abbrechen (viewModelScope-Jobs cancel).
5. Unit-Test FirebaseAiServiceConsentTest: ohne Consent Result.failure(ConsentMissingException), kein Netzwerkaufruf.
6. Datenschutzerklärung 5.6: Satz "Bei der erstmaligen Nutzung einer KI-Funktion (manuell oder automatisch) bitten wir dich um eine explizite Zustimmung" beibehalten – jetzt stimmt er. Version bumpen, Lint/Tests, Commit "BJA-DS-002: zentrale Gemini-Einwilligungssperre", Register-Status "behoben".
```

## Block 3 – BJA-RT-001, DS-005, RT-003, DS-009, ALT-001 (Texte): Rechtstexte korrigieren (Blocker + HOCH + MITTEL)

```
Aufgabe Rechtstexte (BJA-RT-001 Blocker, BJA-DS-005, BJA-RT-003, BJA-DS-009, BJA-ALT-001 Textteil, BJA-VR-001, BJA-RT-002). Maßgebliche Dateien: app/src/main/assets/legal/de/DATENSCHUTZ.html, NUTZUNGSBEDINGUNGEN.html, IMPRESSUM.html und die identischen gehosteten Kopien in C:\Users\barwa\proggs\docs\bestjournal\ (privacy-de.html, agb-de.html, imprint-de.html) sowie die EN-Vollfassungen (legal/en/*.html, privacy-en.html, agb-en.html, imprint-en.html). Die veralteten Kopien docs/DATENSCHUTZ.md/.html, docs/NUTZUNGSBEDINGUNGEN.md, docs/IMPRESSUM.md im Projekt löschen (BJA-DOC-001) oder mit Hinweis "veraltet – maßgeblich sind die Assets" versehen.
Änderungen (Vorher/Nachher-Texte stehen in docs/audit/2026-09-04-release-germany/13_REQUIRED_CHANGES.md, Abschnitte A4, B und C – wörtlich übernehmen):
1. TTS: Sätze zur "lokalen Vorlese-Stimme ohne jede Datenübertragung" in DSE 5.2a und im String privacy_gate_tts_body entfernen (oder erst nach Block 6 durch "Vorlesestimme deines Android-Systems" ersetzen).
2. Groq-Speicherdauer in DSE 5.1, 11 und 8a (BIPA): "bis zu 30 Tage, kein Training".
3. AGB 4.3: Behauptung von DPAs mit Microsoft/Groq durch den Nachher-Text ersetzen.
4. DSE: alle Firebase-Authentication-Passagen entfernen (Abschnitt 2 Tabelle, 5.5, 7.1, 11); 7.1 mit dem Nachher-Text.
5. DSE 5.2: "Einmaliger Download des Sprachmodells" → "Das Sprachmodell ist in der App enthalten".
6. DSE 5.6 "Erhobene Daten": Nachher-Text (alle Einträge, Zeitraum, Nachträge, Analyse-Prompt); String consent_toggle_gemini_body: "Sendet deine Tagebuchtexte (bei Analysen alle Einträge) an Google Gemini (USA)."
7. DSE 5.6 Speicherdauer Gemini: "zeitlich begrenzt zur Missbrauchserkennung, nicht zur Produktverbesserung (Regelung für Nutzer im EWR)"; Abschnitt 11 entsprechend.
8. DSE 8: HBDI-Anschrift "Wilhelmstraße 7, 65185 Wiesbaden, Telefon +49 611 1408-0".
9. DSE 3a und 8a: auf Deutschland-Start kürzen ("Die App wird zunächst in Deutschland angeboten; die App-Sprache folgt deiner Systemsprache"); internationale Rechte-Abschnitte (CCPA … Chile) in einen ausgeklappten Anhang "Hinweise für künftige Länder" verschieben oder entfernen.
10. DSE 11: "Serverlogs" → "Protokolle unserer Cloud Function (Kaufstatus-Prüfung): max. 30 Tage".
11. DSE 5.3 und String consent_toggle_drive_body: "verschlüsselt übertragen und bei Google verschlüsselt gespeichert; keine zusätzliche App-Verschlüsselung".
12. DSE 3.3: Galerie-Kopie offenlegen (oder nach Block 9 als Option beschreiben).
13. Alter: DSE 10 und AGB 3 auf 16 Jahre (Nachher-Text in 13_REQUIRED_CHANGES.md B/ALT-001).
14. AGB 5.3/16 (VR-001) und AGB 10/11/12/5.2 (RT-002): Nachher-Texte aus 13_REQUIRED_CHANGES.md C übernehmen.
15. Impressum: Absatz "Verantwortlich für den Inhalt nach § 18 Abs. 2 MStV" entfernen; Zeile "Sicherheitsmeldungen (Schwachstellen): dev.app.support@gmail.com, Betreff SECURITY" ergänzen (BJA-CRA-001).
16. Neue Angaben in DSE 1/4: Rechtsgrundlage der lokalen Speicherung (Art. 6 Abs. 1 lit. b), Hinweis nach Art. 11 (keine Identifizierung, Rechteausübung in-App), § 25 Abs. 2 Nr. 2 TDDDG für technisch notwendige Kennungen; DSB-Zeile als Platzhalter "[Name, E-Mail des externen Datenschutzbeauftragten]" (BJA-DS-006 – Aussage "nicht erforderlich" streichen).
17. Stand-Datum in allen geänderten Texten auf die echte Systemzeit setzen (Get-Date), Fußzeile "Letzte Aktualisierung" identisch; Constants.CURRENT_POLICY_VERSION = "3.3" mit Kommentar.
18. Alle betroffenen Strings in 27 Sprachen und die 24 Kurzfassungen (legal/<lang>/PRIVACY.html) nachziehen; EN-Vollfassungen inhaltsgleich.
Version bumpen, Lint/Tests, Commits je Befund (RT-001, DS-005, RT-003, DS-009, ALT-001-Text, VR-001, RT-002, DOC-001, CRA-001-Text), Register-Status setzen.
```

## Block 4 – BJA-DS-004: Re-Consent bei Policy-Wechsel (HOCH)

```
Aufgabe BJA-DS-004. In app/src/main/java/com/bestjournal/app/ui/screens/splash/SplashViewModel.kt eine Funktion needsConsent(): consent_shown fehlt ODER prefs.getString(PREF_CONSENT_POLICY_VERSION) != Constants.CURRENT_POLICY_VERSION. SplashScreen.kt Zeile 93 auf needsConsent() umstellen. ConsentViewModel: beim Start vorhandene Toggle-Werte laden (Analytics/Drive aus Prefs, Groq/Gemini/TTS aus PrivacyGateHelper) statt alles auf false; bei Versionswechsel TTS auf false setzen. ConsentScreen: wenn eine ältere Version gespeichert war, oben ein Banner "Wir haben unsere Datenschutzhinweise aktualisiert (Version 3.3). Bitte prüfe deine Auswahl." (neuer String, 27 Sprachen). Unit-Test für needsConsent(). Version bumpen, Lint/Tests, Commit "BJA-DS-004: erneute Einwilligung bei neuer Policy-Version", Register.
```

## Block 5 – BJA-SEC-001 + BJA-SEC-003: Groq über Cloud Function, App Check erzwingen (HOCH/MITTEL)

```
Aufgabe BJA-SEC-001 und BJA-SEC-003. Ziel: Der Groq-Schlüssel verlässt den Server nicht mehr.
1. functions/index.js: Neue HTTPS-Funktion transcribeAudio (onRequest oder onCall mit Multipart-Upload; Region europe-west1; enforceAppCheck: true; Secret GROQ_API_KEY via defineSecret; Limit 25 MB; Weiterleitung an https://api.groq.com/openai/v1/audio/transcriptions mit model whisper-large-v3-turbo, response_format verbose_json, temperature 0, language aus Request; einfaches Rate-Limit je App-Check-Token, z. B. 60 Aufrufe/Stunde, im Speicher oder Firestore-frei über Zeitfenster im Log). Antwort 1:1 zurückgeben. In getSubscriptionStatus enforceAppCheck auf true setzen. Firebase-Konfiguration: firebase functions:secrets:set GROQ_API_KEY (Frank gibt den Wert ein – niemals im Repo). Deploy-Anleitung in functions/README.md.
2. App: GroqApi/NetworkModule auf die Function-URL umstellen (Firebase Functions HTTPS mit App-Check-Header über FirebaseAppCheck.getAppCheckToken oder getHttpsCallable mit Byte-Payload); Constants.REMOTE_CONFIG_GROQ_KEY und den Remote-Config-Fetch in TranscriptionRepository entfernen; DeviceLocale-Sprachcode weiterhin mitsenden.
3. Nach erfolgreichem Umstieg: Remote-Config-Parameter groq_api_key in der Firebase Console löschen und den Groq-Schlüssel im Groq-Konto rotieren (Anleitung ausgeben, nicht selbst ausführen).
4. Datenschutzerklärung 5.1 ergänzen: "Die Aufnahme wird über einen von uns betriebenen Server in der EU (Firebase Functions, Belgien) an Groq weitergeleitet; Groq erhält dadurch nicht deine IP-Adresse." Abschnitt 5.9 Remote Config entsprechend kürzen.
5. Client-Kommentar in SubscriptionStatusService.kt (App Check enforced) bleibt jetzt wahr.
Version bumpen, Lint/Tests, Commits "BJA-SEC-001: Groq-Aufruf über Cloud Function" und "BJA-SEC-003: App Check für Cloud Functions erzwingen", Register.
```

## Block 6 – BJA-DS-003: Vorlesen auf Android-TextToSpeech umstellen (HOCH)

```
Aufgabe BJA-DS-003. Ziel: Vorlesefunktion ohne Datenübertragung.
1. Neue Klasse util/SystemTtsPlayer.kt mit android.speech.tts.TextToSpeech (Locale aus DeviceLocale, Stimmenauswahl über tts.voices, Utterance-Callbacks für onPlaybackStart/onComplete, Stop/Shutdown). Gleiche Schnittstelle wie EdgeTtsPlayer (speak/stop/shutdown).
2. Entscheidung: Edge-TTS (EdgeTtsPlayer.kt, TtsVoiceRegistry-Edge-Stimmen, PrivacyGateHelper.CloudService.EdgeTts, Consent-Toggle TTS, Gate-Strings) vollständig entfernen. Alle Aufrufer (DashboardScreen 139/2399-2412/3866-3880, EntryDetailScreen 178/845/909, RetrospectiveScreen 987/1272-1292) auf SystemTtsPlayer umstellen; Stimmenauswahl in den Einstellungen auf System-Stimmen (voice.name, locale) umbauen; Hinweis, falls keine Stimme installiert ist (Intent ACTION_INSTALL_TTS_DATA).
3. Consent-Screen und Privacy-Sheet: TTS-Schalter entfernen (Analytics, Groq, Gemini, Drive bleiben). Strings consent_toggle_tts_*, privacy_gate_tts_* und Analytics-Events tts_* entfernen bzw. auf lokale Fehler beschränken.
4. Datenschutzerklärung 5.2a und 12a/5.11 (Microsoft) entfernen; Abschnitt 2 Tabelle anpassen; AGB 2 "Vorlesefunktion (auf dem Gerät)"; Paywall/Onboarding-Feature "Premium-Stimmen"/"Lass dir deine Einträge vorlesen" auf System-Stimmen umformulieren (kein Premium-Versprechen für Cloud-Stimmen mehr).
5. Alternative nur falls Frank ausdrücklich Cloud-Stimmen behalten will: Azure AI Speech (learn.microsoft.com/azure/ai-services/speech-service) mit EU-Region hinter einer Cloud Function und Microsoft-DPA – dann DSE 5.2a auf Auftragsverarbeitung mit SCC umschreiben.
Version bumpen, Lint/Tests, Commit "BJA-DS-003: Vorlesen auf System-TTS umgestellt", Register (auch RT-001(a) damit erledigt).
```

## Block 7 – BJA-GP-001: Werbe-ID-Berechtigung entfernen (HOCH)

```
Aufgabe BJA-GP-001. In app/src/main/AndroidManifest.xml (mit xmlns:tools) ergänzen: <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>, <uses-permission android:name="android.permission.ACCESS_ADSERVICES_AD_ID" tools:node="remove"/>, <uses-permission android:name="android.permission.ACCESS_ADSERVICES_ATTRIBUTION" tools:node="remove"/>. Nach dem Build im zusammengeführten Manifest (app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml) prüfen, dass keine AD_ID-Berechtigung mehr enthalten ist. DSE 5.7: "Android Werbe-ID (AAID)" und "Werbe-ID zurücksetzen" streichen; String consent_toggle_analytics_body bleibt. Play-Console-Hinweis in 07_GOOGLE_PLAY_COMPLIANCE.md: Werbe-ID "Nein". Version bumpen, Lint/Tests, Commit "BJA-GP-001: Werbe-ID-Berechtigung aus dem Manifest entfernt", Register.
```

## Block 8 – BJA-ALT-001 (Code) + Krisenhilfe: Altersbestätigung 16+, Nummern ergänzen (HOCH)

```
Aufgabe BJA-ALT-001 (Codeanteil). 1. In OnboardingScreen.kt auf der ersten Seite eine Checkbox "Ich bin mindestens 16 Jahre alt" (String onboarding_age_confirm, 27 Sprachen); "Weiter" erst aktiv, wenn gesetzt; Wert nicht dauerhaft speichern (Selbstauskunft). 2. CrisisHelpDialog.kt: zwei Zeilen ergänzen – "TelefonSeelsorge 116 123 anrufen" und "Nummer gegen Kummer 116 111 (Kinder und Jugendliche, Mo–Sa 14–20 Uhr) anrufen" (Strings, 27 Sprachen; Nummern bleiben deutsch); DSE 12b entsprechend ergänzen. 3. Prompts ai_retro_week_rules/month/year und ai_prompt_summary_rules um den Satz "Gib keine medizinischen, psychologischen oder therapeutischen Diagnosen oder Empfehlungen; verweise bei solchen Themen auf Fachpersonen." ergänzen (27 Sprachen). Version bumpen, Lint/Tests, Commit "BJA-ALT-001: Altersbestätigung 16+, Krisen-Nummern ergänzt", Register.
```

## Block 9 – BJA-KI-001 + BJA-DS-012: Meldefunktion am KI-Output ohne Google-Login (HOCH)

```
Aufgabe BJA-KI-001 (und Alternative zu BJA-DS-012). 1. Neue Cloud Function reportAiContent (functions/index.js, europe-west1, enforceAppCheck: true): nimmt Kategorie (offensive|falsch|schaedlich|sonstiges), Freitext (max 2000 Zeichen), Funktionsname (dashboard|retro|improve|summary), App-Version entgegen und sendet eine E-Mail an dev.app.support@gmail.com (z. B. über Gmail-API mit Service-Account-Delegation oder einen Transaktions-Mail-Dienst – Frank entscheidet; Secret nie im Repo) oder schreibt in Cloud Logging mit Alert. 2. App: In ui/components/AiGeneratedBadge.kt neben dem Badge ein kleines Flaggen-Icon "Melden" (contentDescription!) → ReportAiContentDialog (Kategorie-Chips + Freitext + Hinweis "Keine Tagebuchinhalte werden automatisch mitgesendet") → Aufruf der Function über FirebaseFunctions; Bestätigung per Snackbar; keine Google-Anmeldung nötig. Dialog auch für AiGeneratedBadgeInline (Rückblicke) und den Textverbesserungs-Footer. 3. Bestehenden Settings-Eintrag "KI-Antwort melden" auf den neuen Dialog umstellen; mailto nur als Fallback bei Netzwerkfehler. 4. Optional (empfohlen, löst DS-012): FeedbackSender und RevokeSender ebenfalls über Cloud Functions (sendFeedback, sendRevocation) statt Gmail-Scope senden; dann gmail.send-Scope aus dem OAuth-Consent-Screen entfernen und DSE 5.9a auf "Versand über unseren Server (EU) an unser Postfach" umschreiben. Version bumpen, Lint/Tests, Commit "BJA-KI-001: Meldefunktion am KI-Output", Register.
```

## Block 10 – BJA-SEC-002 + BJA-DS-010 + BJA-GP-004 + BJA-SEC-004: Logging, Analytics-Reset, Alarm-Pfad, FLAG_SECURE (MITTEL/NIEDRIG)

```
Aufgabe Sicherheit klein. 1. app/proguard-rules.pro: -assumenosideeffects class android.util.Log { public static int v(...); public static int d(...); public static int i(...); } ergänzen. 2. PII aus Log.e/Log.w entfernen: AuthRepository.kt Zeilen 25-60 (E-Mail, Web-Client-ID → nur "sign-in ok/failed"), SettingsViewModel.kt:650, JournalViewModel.kt:645 (displayText-Ausschnitt entfernen), TranscriptionRepository.kt:150-156 und 172-176 (seg.text entfernen), SummarizeEntryUseCase.kt:115 (Titel entfernen), GenerateRetrospectiveUseCase.kt:231-234 und 636 (Titel entfernen). 3. SettingsViewModel.deleteAccount: vor signOut FirebaseAnalytics.getInstance(context).resetAnalyticsData() und setAnalyticsCollectionEnabled(false); DSE 7.1 ergänzen "Analytics-Kennung wird zurückgesetzt". 4. DailyReminderManager.kt: exakten Alarmpfad (setExactAndAllowWhileIdle, canScheduleExactAlarms) entfernen, nur setAndAllowWhileIdle/setWindow verwenden. 5. Neue Einstellung "Screenshots blockieren" (Standard aus) in Sicherheit; MainActivity setzt WindowManager.LayoutParams.FLAG_SECURE entsprechend. 6. Beim App-Start cacheDir von recording_*.wav-Resten bereinigen. Version bumpen, Lint/Tests, Commits je Befund, Register.
```

## Block 11 – BJA-DS-008: Galerie-Kopie als Option (MITTEL)

```
Aufgabe BJA-DS-008. In PhotoRepository.kt (saveToGallery, addPhotoFromFile Zeilen 99-106): Galerie-Kopie nur, wenn Pref PREF_SAVE_TO_GALLERY (Standard false) gesetzt ist. Neue Einstellung "Aufnahmen auch in der Galerie speichern" unter Aufnahme/Fotos. DSE 3.3: "Wenn du die Option 'Aufnahmen auch in der Galerie speichern' aktivierst, wird zusätzlich eine Kopie im Ordner DCIM/Camera abgelegt, die für andere Apps sichtbar ist und beim Löschen des Eintrags nicht entfernt wird." Version bumpen, Lint/Tests, Commit, Register.
```

## Block 12 – BJA-KI-002: Maschinenlesbare KI-Kennzeichnung beim Teilen/Export (MITTEL)

```
Aufgabe BJA-KI-002. 1. Zentrale Hilfsfunktion util/AiContentMarker.kt: fügt KI-generiertem Text beim Teilen die sichtbare Fußzeile "— Erstellt mit KI (Google Gemini) in Best Journal —" (String, 27 Sprachen) und eine maschinenlesbare Markierung an (z. B. unsichtbare Unicode-Tag-Sequenz U+E0041..U+E007A, die "AIGENERATED:BESTJOURNAL" kodiert, oder ein Zero-Width-Muster; Vorgehen nach dem EU Code of Practice on Transparency of AI-generated Content dokumentieren in docs/KI-KENNZEICHNUNG.md). 2. Anwenden in DashboardScreen.kt (ACTION_SEND Zeilen 3902-3912), RetrospectiveScreen.kt (Teilen 1448-1470), ShareEntryDialog.kt (nur, wenn der geteilte Text KI-Anteile enthält: Zusammenfassung/verbesserter Text) und PdfExporter.kt (PDF-Metadaten Subject/Keywords "AI-generated content: Google Gemini" plus Fußzeile auf Seiten mit KI-Text). 3. Textverbesserung bleibt ohne maschinenlesbare Markierung (Ausnahme Art. 50 Abs. 2 für unterstützende Bearbeitung), der sichtbare Marker "✨ KI-verbessert" bleibt. Version bumpen, Lint/Tests, Commit, Register.
```

## Block 13 – BJA-OSS-001: Lizenzhinweise (MITTEL)

```
Aufgabe BJA-OSS-001. 1. Gradle: Plugin com.google.android.gms.oss-licenses-plugin (Root und app) einbinden, Abhängigkeit com.google.android.gms:play-services-oss-licenses; in app/build.gradle.kts die packaging-Ausschlüsse auf Duplikate beschränken (nur /META-INF/{AL2.0,LGPL2.1} behalten, DEPENDENCIES/INDEX.LIST nur wenn Build sonst bricht). 2. Einstellungen → Über die App: Eintrag "Open-Source-Lizenzen" → OssLicensesMenuActivity (Theme anpassen). 3. app/src/main/assets/licenses/OFL-Caveat.txt (SIL OFL 1.1 mit Copyright-Zeile der Caveat-Schrift) und MIT-Hinweis für onnxruntime/Whisper sowie Apache-Hinweis für sherpa-onnx in eine eigene Seite "Weitere Lizenzen" (WebView-Asset) aufnehmen, da lokale AARs vom Plugin nicht erfasst werden. Version bumpen, Lint/Tests, Commit, Register.
```

## Block 14 – BJA-WEB-001: Rechtstexte auf Hostinger veröffentlichen (Blocker, teils außerhalb des Codes)

```
Aufgabe BJA-WEB-001 (Release-Blocker). Die GitHub-Pages-Seite ist tot; die Rechtstexte ziehen auf den Hostinger-Server um. Zugangsdaten liegen in C:\Users\barwa\SK (dort nachlesen, nie raten).
1. Zielstruktur festlegen: https://<domain>/bestjournal/ mit index.html, datenschutz.html, datenschutz-en.html, agb.html, agb-en.html, impressum.html, impressum-en.html, konto-loeschen.html (Koreanisch entfällt, solange nur Deutschland bedient wird). Quelle: C:\Users\barwa\proggs\docs\bestjournal\*.html (nach Block 3 aktualisiert).
2. Website-Datenschutzabschnitt in datenschutz.html ergänzen: Hoster Hostinger (Firmierung/Sitz laut Vertrag), Serverstandort, Server-Logfiles (IP, Zeit, URL, User-Agent, Speicherdauer laut Hoster), Rechtsgrundlage Art. 6 Abs. 1 lit. f DSGVO, keine Cookies, keine Analyse, keine externen Schriften/Skripte.
3. Deploy per SFTP/FTP-Skript (Zugang aus SK), HTTPS erzwingen, Weiterleitung http→https.
4. App: Strings legal_url_datenschutz_de/en, legal_url_agb_de/en, legal_url_impressum_de/en, settings_legal_online_url auf die neuen URLs setzen; ko-Varianten auf de/en zeigen lassen; ConsentScreen.kt-Footer und Settings "Online-Version" prüfen.
5. Verifikation: curl -I auf alle URLs = 200; Quelltext ohne <script>/externe <link>; Ergebnis nach docs/audit/2026-09-04-release-germany/evidence/website-check.txt.
6. Play-Console-Felder (Datenschutz-URL, Kontolösch-URL) in 07_GOOGLE_PLAY_COMPLIANCE.md und play-store-metadata aktualisieren.
Version bumpen, Lint/Tests, Commit "BJA-WEB-001: Rechtstexte auf Hostinger, URLs aktualisiert", Register.
```

## Block 15 – BJA-GP-003 + BJA-GP-002 + Store-Unterlagen (MITTEL)

```
Aufgabe Play-Unterlagen. 1. play-store-metadata/data-safety-checklist.md, health-apps-declaration.md, country-exclusion.md durch eine neue Datei play-store-metadata/PLAY-CONSOLE-2026-09.md ersetzen, die 1:1 die Tabellen aus docs/audit/2026-09-04-release-germany/07_GOOGLE_PLAY_COMPLIANCE.md übernimmt (Produktion nur Deutschland, Zielgruppe 16–17/18+, Werbe-ID nein, Health wellness, KI ja mit Meldefunktion, Data Safety). 2. play-store-metadata/listing-de.md anlegen: Titel "Best Journal" (mit Dossier: Eigenname, Entwickler Barwandt Digital Labs, Ausweichtitel "BestJournal – KI-Tagebuch"), Kurzbeschreibung ≤80 Zeichen ohne Superlative, Beschreibung ≤4000 Zeichen ohne Gesundheitsversprechen ("kein Medizinprodukt, keine Therapie" als Satz), Hinweise zu Abo/Testphase/Preisen konsistent mit der App. 3. Checkliste für Store-Assets (Icon 512×512 PNG, Feature-Grafik 1024×500, ≥2 Screenshots ohne personenbezogene Daten und ohne Fremdmarken) mit Ablageort play-store-metadata/assets/. Commit "BJA-GP-003: Play-Console-Unterlagen aktualisiert".
```

## Block 16 – BJA-DS-006: DSFA fertigstellen und DSB (HOCH, Dokumentation)

```
Aufgabe BJA-DS-006. Aus docs/audit/2026-09-04-release-germany/17_DPIA_SCREENING.md und 16_RECORDS_OF_PROCESSING_DRAFT.md die Dokumente docs/compliance/DSFA-BestJournal.md (vollständige DSFA nach Art. 35 Abs. 7: Beschreibung, Notwendigkeit, Risikobewertung mit Eintrittswahrscheinlichkeit/Schwere je Risiko, Maßnahmen, Restrisiko, Beteiligung des DSB, Datum) und docs/compliance/VVT-BestJournal.md (Art. 30) erstellen; Platzhalter für den externen DSB; Transfer-Impact-Assessment-Abschnitt für Google und Groq mit Verweis auf die Nachweise E-03 bis E-06/E-10. Zusätzlich docs/compliance/KI-KOMPETENZ.md (Art. 4 KI-VO: Kenntnisse, Prompt-Regeln, Modellgrenzen, Review-Rhythmus) und docs/compliance/SECURITY-PROZESS.md (CRA: Kontakt, Meldefristen 24h/72h/14 Tage, Update-Rhythmus). Commit "BJA-DS-006: DSFA, VVT, KI-Kompetenz, Sicherheitsprozess".
```

## Block 17 – BJA-TECH-001 (nach Release): Abhängigkeiten (NIEDRIG)

```
Aufgabe BJA-TECH-001 (erst nach dem Release). AGP auf aktuelle 8.x/9.x, Compose-BOM 2026.08.00, Room 2.8.x, play-services-auth 22.x, Firebase-BOM aktuell; EncryptedSharedPreferences durch eigene Keystore-AES-GCM-Verschlüsselung (Tink bereits transitiv vorhanden) mit Migration der bestehenden Prefs ersetzen; Lint TypographyDashes/UnusedResources aufräumen; Whisper-Modell als Play-Asset-Delivery-Pack (install-time) auslagern, um das 200-MB-Limit sicher einzuhalten. Nach jedem Schritt Build/Tests. Commit je Schritt.
```

## Block 18 – Re-Audit nach Umsetzung

```
Aufgabe Re-Audit. Nach Umsetzung der Blöcke 1–16: docs/audit/2026-09-04-release-germany/FINDINGS_REGISTER.json aktualisieren (Status behoben/offen/akzeptiert je Befund, kein zweites Register), 01_EXECUTIVE_RELEASE_DECISION_GERMANY.md und 14_FINAL_RELEASE_CHECKLIST_GERMANY.md neu bewerten, Datenflussregister (03), Rechtstext-Konsistenz (08), Data-Safety-Tabelle (07) und Alterskonzept (06) gegen den neuen Stand prüfen; gradlew.bat :app:lintRelease :app:testReleaseUnitTest :app:bundleRelease; Merged Manifest ohne AD_ID bestätigen; curl auf alle Rechtstext-URLs = 200; Consent-Umgehung durch Grep über alle generateContent-/groqApi-Aufrufer ausschließen. Entscheidung neu ausgeben (A/B/C) mit Begründung.
```
