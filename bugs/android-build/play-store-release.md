# Bekannte Reject-Gründe & Fallen: Play-Store-Release & Policy

> PFLICHT-LESEN vor Veröffentlichung/Update von BestJournal bei Google Play.
> Stand: tief recherchiert am 2026-06-14 (7 Researcher parallel, offizielle Play-Quellen).
> Versions-Anker: Play-Policy/Console **Juni 2026** · BestJournal targetSdk **35**, versionCode **144**,
> versionName **0.19.11**, AAB, R8; nutzt `RECORD_AUDIO`, Google-Drive-Backup, LLM/TTS-APIs.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/projekt-code/android-build/best-practices-play-store-release.md`](../../best-practices/projekt-code/android-build/best-practices-play-store-release.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Reject.
> Sektionen: **T** Tracks/Testing · **R** Rollout/versionCode · **D** Data-Safety · **F** Foreground-Service ·
> **A** Accessibility/Permissions · **P** Pre-Launch-Report · **S** Signing/Integrity · **M** Metadaten/ASO/targetSdk.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | „Apply for production" ausgegraut | Neues Personal-Konto: 12 Tester / 14 zusammenhängende Tage Closed Test | T1 |
| 2 | „nur X von 12 Testern" | Einladung ≠ Opt-in; alle am selben Tag opt-in + installieren | T2, T3 |
| 3 | Production-Antrag abgelehnt | Fragebogen konkret (echtes Engagement/Feedback/Änderungen), nicht „App is good" | T5 |
| 4 | „Cannot create new release" | Ausstehenden Rollout auf 100 % bringen oder verwerfen | R1 |
| 5 | Kaputte Version draußen | Kein Rollback — `Halt` + neues höheres versionCode | R2 |
| 6 | versionCode-Upload abgelehnt | Streng monoton, nie wiederverwenden/senken | R4 |
| 7 | Update blockiert (Account-Deletion) | In-App + **Web-URL**-Kontolöschung | D3 |
| 8 | Reject Photo/Video-Permission | `READ_MEDIA_*` raus → Photo Picker; merged Manifest prüfen | D4 |
| 9 | Data-Safety „inaccurate" | Jedes SDK deklarieren; Formular↔Policy↔Verhalten | D1, D5 |
| 10 | Reject Prominent Disclosure | In-App-Dialog VOR Permission, affirmatives Consent | D6 |
| 11 | `dataSync`-FGS abgelehnt | Backup → WorkManager (auto) / UIDT (manuell) | F1 |
| 12 | „dataSync did not stop in timeout" | 6h/24h-Limit; `onTimeout()`→`stopSelf()`; besser UIDT | F2 |
| 13 | FGS-Deklaration abgelehnt | Demo-Video muss user-initiierte Auslösung zeigen | F3 |
| 14 | Removal: AccessibilityService | Kein Accessibility-Tool → TTS-API/Quick-Tile; sonst Non-Tool-Declaration | A1 |
| 15 | Removal: autonome Aktionen | Nur statische regelbasierte Automatisierung (seit Okt 2025) | A2 |
| 16 | Reject QUERY_ALL_PACKAGES | Entfernen → gezieltes `<queries>` | A4 |
| 17 | PLR: Inhalt hinter Login ungetestet | Test-Account hinterlegen (kein Produktiv-Konto) | P1 |
| 18 | Store-Build: Maps/OAuth tot | App-Signing-Key-SHA bei API-Provider eintragen | S2 |
| 19 | Crash-Stacktraces obfuskiert | AAB + AGP 4.1+ → mapping.txt automatisch | S5 |
| 20 | „muss API 35 targeten" | targetSdk 35+ (BestJournal konform); jährlich mitziehen | M1 |
| 21 | „Violation of Metadata policy" | Kein „#1/Best/Free", keine Emojis/Keyword-Listen (alle Sprachen) | M3 |

---

## T) Release-Tracks & Testing-Pflicht

### T1. „Apply for production" ausgegraut/blockiert ⭐ HAEUFIG
- **Symptom:** Production nicht beantragbar trotz fertiger App.
- **Ursache:** Neues Personal-Konto (nach 13.11.2023) hat die Closed-Test-Pflicht nicht erfüllt.
- **Versionen:** Stand 2026.
- **FIX:** Closed Test mit **≥12 opted-in Testern über 14 zusammenhängende Tage**, dann „Apply for production".
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/14151465

### T2. „Nur X von 12 Testern" trotz mehr Einladungen ⭐ HAEUFIG
- **Symptom:** Console zählt weniger Tester als eingeladen.
- **Ursache:** Einladung ≠ Opt-in. Nur wer den Link öffnet, beitritt UND installiert, zählt.
- **Versionen:** 2026.
- **FIX:** Testern explizit „Link öffnen + App installieren" ansagen; Status in der Console prüfen; mit Puffer (15–20) arbeiten.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/14151465

### T3. 14-Tage-Zähler zurückgesetzt
- **Symptom:** Uhr scheint neu zu starten.
- **Ursache:** 14 Tage zählen erst ab dem Zeitpunkt, an dem alle ≥12 **gleichzeitig** opted-in sind; Aussteigen+Wiedereinsteigen muss am Stück 14 Tage ergeben.
- **Versionen:** 2026.
- **FIX:** Alle Tester am selben Tag opt-in lassen; Dropouts sofort durch Ersatz ersetzen (deren Uhr startet neu).
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/14151465

### T4. „20 Tester"-Fehlinformation
- **Symptom:** Veraltete Anleitungen nennen 20 Tester.
- **Ursache:** Am 11.12.2024 von 20 auf **12** reduziert (14 Tage blieb).
- **Versionen:** seit 11.12.2024.
- **FIX:** Mit 12 (Puffer 15–20) planen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/14151465

### T5. Production-Antrag inhaltlich abgelehnt
- **Symptom:** Reject trotz erfüllter Tester-Zahl.
- **Ursache:** Vager Fragebogen („App is good", „No issues"), kein echtes Engagement/Feedback.
- **Versionen:** 2026.
- **FIX:** Konkret: tatsächliche Tester-Nutzung, gesammeltes Feedback, vorgenommene Änderungen, Reifegrad-Begründung.
- **Quelle:** Praxis (primetestlab) + …/14151465

### T6. Tester finden/installieren die App nicht
- **Symptom:** Opt-in-Link funktioniert nicht.
- **Ursache:** App im Track noch „Draft/Pending"; Land nicht getargetet; Store-Suche statt Opt-in-Link.
- **Versionen:** 2026.
- **FIX:** Track publizieren; Land-Targeting prüfen; Opt-in-Link teilen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9845334

### T7. Internal-App-Sharing-Link tot
- **Symptom:** Link funktioniert plötzlich nicht.
- **Ursache:** Nach 60 Tagen abgelaufen oder 100-Nutzer-Limit erreicht.
- **Versionen:** 2026.
- **FIX:** Gleiches AAB/APK neu hochladen → neuer Link. (Internal App Sharing zählt NICHT für die 12/14-Pflicht.)
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9844679

### T8. Erste Veröffentlichung verzögert (Konto-Verifizierung)
- **Symptom:** Account/erste Veröffentlichung hängt.
- **Ursache:** Geräte-Verifizierung (echtes Gerät via Console-App) und/oder Identitäts-/D-U-N-S-Verifizierung ausstehend; neue Konten länger im Review.
- **Versionen:** 2026; erweiterte Identitätsverifizierung ab Sept 2026.
- **FIX:** Geräte-Verifizierung früh; Org-Konto: D-U-N-S bis 30 Tage Vorlauf; ~7+ Tage Review einplanen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/14316361

---

## R) Staged/Managed Rollout & versionCode

### R1. „Cannot create a new release"
- **Symptom:** Neuer Release nicht anlegbar.
- **Ursache:** Ausstehender Release (Staged Rollout < 100 % oder unveröffentlichter Draft) blockiert.
- **Versionen:** 2026.
- **FIX:** Rollout auf 100 % bringen ODER auf Publishing-overview verwerfen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9859348

### R2. Kein Rollback / Rückwärtsrollen ⭐ HAEUFIG
- **Symptom:** Kaputte Version bei z. B. 10 % crasht, man will „zurückrollen" — geht nicht.
- **Ursache:** Android-Downgrade-Schutz; `Halt` stoppt nur weitere Verteilung, holt niemanden zurück.
- **Versionen:** per Design.
- **FIX:** Sofort `Halt` → Fix → **neues AAB mit höherem versionCode** ausrollen. (NEU 2026: 100 %-Version anhalten liefert die vorherige gute Version an neue Nutzer.)
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/16285429

### R3. Google drosselt Rollout selbst (10 % → 1 %)
- **Symptom:** Play reduziert deinen Prozentsatz.
- **Ursache:** Automatisches Sicherheitsnetz bei hoher Crash-Rate.
- **Versionen:** 2026.
- **FIX:** Stopp-Signal ernst nehmen — halten, Crash in Vitals fixen, neue Version; nicht wieder hochdrehen.
- **Quelle:** https://support.google.com/googleplay/android-developer/thread/222780367

### R4. versionCode-Upload abgelehnt ⭐ HAEUFIG
- **Symptom:** „You can't upload an APK with a versionCode you have already used" oder Upload blockiert.
- **Ursache:** versionCode wiederverwendet/gesenkt, oder > 2.100.000.000.
- **Versionen:** per Design.
- **FIX:** Streng monoton +1 (144→145…); Lücken ok, nie wiederholen/senken.
- **Quelle:** https://developer.android.com/studio/publish/versioning

### R5. Android Vitals über Schwelle → Sichtbarkeitsverlust
- **Symptom:** Downloads/Discovery brechen ein, App „funktioniert" aber.
- **Ursache:** user-perceived Crash > 1,09 % / ANR > 0,47 % / per-Device > 8 % → schlechtere Auffindbarkeit + ggf. Store-Warnung.
- **Versionen:** 2026.
- **FIX:** Während jedes Rollouts Vitals live prüfen, unter den Schwellen bleiben; bei Anstieg halten statt erhöhen.
- **Quelle:** https://developer.android.com/topic/performance/vitals/crash

### R6. Managed Publishing hält nicht alles zurück
- **Symptom:** Etwas geht live, obwohl „gehalten".
- **Ursache:** Ausnahmen: Rollout auf 100 % erhöhen, Release-Notes-Updates, Tester-Listen, Preise, Experiment stoppen.
- **Versionen:** 2026.
- **FIX:** Wissen, dass diese Aktionen sofort wirken; kritische Listing-Änderungen separat planen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9859654

### R7. Nachgeschobene Änderung verlängert Review
- **Symptom:** Alles dauert plötzlich länger, „Review and publish" deaktiviert.
- **Ursache:** Review-SLA zählt ab der LETZTEN Einreichung; Nachschub schiebt ans Queue-Ende.
- **Versionen:** 2026.
- **FIX:** Alle Änderungen bündeln, nicht tröpfchenweise nachschieben.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9859654

### R8. Update-Priority in der Console gesucht
- **Symptom:** „Update-Priorität" in der UI nicht auffindbar.
- **Ursache:** `inAppUpdatePriority` nur per Publisher-API setzbar, zum Release-Zeitpunkt.
- **Versionen:** 2026.
- **FIX:** In CI/CD (Fastlane `supply`/eigenes Skript) setzen; Priority 5 + Immediate-Flow für kritische Fixes.
- **Quelle:** https://developers.google.com/android-publisher/api-ref/rest/v3/edits.tracks

---

## D) Data-Safety & Daten-Policies

### D1. Data-Safety „incomplete/inaccurate" (Reject Nr. 1) ⭐ HAEUFIG
- **Symptom:** Reject „Data safety section … inaccurate".
- **Ursache:** SDK sendet still Daten (Crashlytics-Logs, LLM-API mit Tagebuchtext), Formular deklariert nichts.
- **Versionen:** 2026.
- **FIX:** Jedes SDK prüfen (Play SDK Index), alle Off-Device-Transfers deklarieren; Formular ↔ Privacy-Policy ↔ Verhalten dreifach abgleichen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10787469

### D2. „Ephemeral" falsch deklariert
- **Symptom:** Mismatch-Flag trotz „ephemeral".
- **Ursache:** LLM-/TTS-Anbieter speichert/loggt/trainiert auf Inputs → nicht mehr ephemeral.
- **Versionen:** 2026.
- **FIX:** Anbieter-Nichtspeicherung sicherstellen; sonst als reguläre Collection/Sharing deklarieren.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10787469

### D3. Update blockiert — Account-Deletion fehlt (Web-URL) ⭐ HAEUFIG
- **Symptom:** „Data deletion questions" unbeantwortet / Update-Block.
- **Ursache:** Nur In-App-Löschung, keine eigenständige Web-URL — oder Web-URL führt zurück in die App.
- **Versionen:** 2026.
- **FIX:** Web-Ressource (Formular/E-Mail), die ohne App-Neuinstallation funktioniert, ins Data-Safety-Feld. (Wenn nur Google-Konto-Backup ohne eigenes App-Login → Pflicht ggf. nicht.)
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/13327111

### D4. Reject wegen Photo/Video-Permissions ⭐ HAEUFIG
- **Symptom:** Update blockiert/Removal wegen `READ_MEDIA_IMAGES/VIDEO`.
- **Ursache:** Permission im Manifest, obwohl nur gelegentlicher Bild-Anhang nötig (kein Galerie-Kernzweck) — seit 28.05.2025 erzwungen.
- **Versionen:** seit 28.05.2025.
- **FIX:** Permission entfernen, **Photo Picker** nutzen; merged Manifest prüfen, dass kein SDK sie reinzieht.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/14115180

### D5. Privacy-Policy ungültig
- **Symptom:** „Invalid Privacy Policy URL".
- **Ursache:** PDF, geo-gesperrt, editierbar (Google Doc), nicht erreichbar, ohne Retention/Deletion.
- **Versionen:** 2026.
- **FIX:** Öffentliche, nicht-editierbare URL (kein PDF), mit Retention/Deletion-Abschnitt, Entwicklername genannt.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10144311

### D6. Reject wegen fehlender Prominent Disclosure ⭐ HAEUFIG
- **Symptom:** „prominent disclosure and consent" Reject (Mikrofon/sensible Daten).
- **Ursache:** Permission-Dialog ohne vorausgehende In-App-Erklärung; Disclosure nur in Privacy-Policy/Menü versteckt/mit anderen vermischt.
- **Versionen:** 2026.
- **FIX:** Eigenständiger In-App-Dialog im Hauptflow VOR dem Runtime-Request, affirmatives „Zustimmen" + Decline; Tagebuch-PII nie roh in Logs/Crash/Backup.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/11150561

### D7. „Inaccurate Health Apps Declaration"
- **Symptom:** Wiederholte Rejects unter Health-Policy.
- **Ursache:** App als Mental-Health/Mood-Tool beworben, Declaration falsch/zu breit/nicht ausgefüllt.
- **Versionen:** 2026.
- **FIX:** Funktionsumfang ehrlich einordnen; nur deklarieren was zutrifft; Health-Apps-Form nur bei Health-Permissions/Health-Connect nötig.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/13996367

### D8. Familien-Policy ausgelöst
- **Symptom:** Reject wegen Families-Policy.
- **Ursache:** „Everyone (children)" gewählt bei sensiblen Daten (Audio/Foto/PII) + Third-Party-SDKs.
- **Versionen:** 2026.
- **FIX:** Zielgruppe korrekt (Erwachsene/Teens ohne Kinder) setzen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10787469

### D9. Klartext-/unsichere Übertragung sensibler Daten
- **Symptom:** Beanstandung sichere Datenhandhabung.
- **Ursache:** HTTP/veraltetes TLS für API-/Backup-Calls mit Tagebuch-/Audio-Daten.
- **Versionen:** 2026.
- **FIX:** Durchgehend HTTPS/TLS; „encryption in transit" im Formular ankreuzen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10144311

---

## F) Foreground-Service-Policy

### F1. `dataSync`-FGS fürs Drive-Backup abgelehnt ⭐ HAEUFIG
- **Symptom:** FGS-Deklaration abgelehnt / „use WorkManager/UIDT instead".
- **Ursache:** Netzwerk-Upload gehört laut Policy in UIDT (user-initiiert) bzw. WorkManager (auto), nicht in `dataSync`-FGS.
- **Versionen:** 2026, targetSdk 34+.
- **FIX:** Auto-Backup → WorkManager; „Jetzt sichern" → UIDT; `dataSync`-FGS aus Manifest/Console entfernen → Deklaration entfällt.
- **Quelle:** https://developer.android.com/develop/background-work/background-tasks/data-transfer-options

### F2. `dataSync`-Service ANR nach 6h (Android 15)
- **Symptom:** „A foreground service of dataSync did not stop within its timeout" / `ForegroundServiceStartNotAllowedException: Time limit already exhausted`.
- **Ursache:** 6h/24h-Limit unter targetSdk 35; `onTimeout()` nicht behandelt / erneuter Start nach Erschöpfung.
- **Versionen:** Android 15 / targetSdk 35.
- **FIX:** `onTimeout()` → sofort `stopSelf()`; besser ganz auf UIDT/WorkManager (kein hartes 6h-Limit für UIDT).
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### F3. FGS-Deklaration wegen schwachem Demo-Video abgelehnt
- **Symptom:** Reject trotz ausgefüllter Deklaration.
- **Ursache:** Video zeigt user-initiierte Auslösung/Nutzervorteil nicht klar; Beschreibung vage.
- **Versionen:** 2026.
- **FIX:** Video der exakten In-App-Schritte (Nutzer tippt → Feature startet, Notification sichtbar) + deferred/interrupted-Impact präzise; erneut einreichen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/13392821

### F4. Reject wegen FGS-Permission „die wir nicht nutzen"
- **Symptom:** Reject wegen `FOREGROUND_SERVICE_*`, obwohl im eigenen Code ungenutzt.
- **Ursache:** Dritt-SDK zieht die Permission via Manifest-Merger herein.
- **Versionen:** 2026.
- **FIX:** Merged Manifest prüfen (`./gradlew :app:processReleaseManifest`); korrekt deklarieren oder `tools:node="remove"`.
- **Quelle:** https://support.google.com/googleplay/android-developer/thread/289475865

### F5. `microphone`-FGS-Crash/Reject
- **Symptom:** `SecurityException` bei `startForeground()`; oder Reject wegen nicht-perceptible Audio.
- **Ursache:** RECORD_AUDIO runtime fehlt; Start aus `BOOT_COMPLETED`/Hintergrund; verdeckte Aufnahme.
- **Versionen:** Android 14+.
- **FIX:** RECORD_AUDIO vor Start; nur aus sichtbarem/user-initiiertem Kontext; sichtbare Notification; kein Background-Listening.
- **Quelle:** https://developer.android.com/about/versions/15/changes/foreground-service-types

### F6. `specialUse` abgelehnt
- **Symptom:** Reject des `specialUse`-Typs.
- **Ursache:** Google sieht fast immer einen passenderen Standardtyp.
- **Versionen:** 2026.
- **FIX:** `specialUse` vermeiden; microphone + UIDT/WorkManager decken BestJournal ab.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/13392821

---

## A) Accessibility-Service & sensible Berechtigungen

### A1. Removal: App als „Accessibility Tool" deklariert, ist keins ⭐ HAEUFIG
- **Symptom:** App entfernt/abgelehnt.
- **Ursache:** `isAccessibilityTool=true`, obwohl Journal/Assistent kein primäres Disability-Tool ist (Google nennt „assistants/automation/launchers" als nicht-qualifiziert).
- **Versionen:** 2026.
- **FIX:** Flag NICHT setzen; AccessibilityService möglichst ganz vermeiden (TTS-API/Quick-Tile/ACTION_ASSIST); sonst Non-Tool-Declaration + In-App-Disclosure + Demo-Video + Zweck im Store-Listing.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10964491

### A2. Suspension wegen autonomer Aktionen
- **Symptom:** Account-Termination/Removal.
- **Ursache:** Seit Okt 2025 verboten: autonomes Initiieren/Planen/Ausführen von Aktionen über die Accessibility-API.
- **Versionen:** seit 30.10.2025.
- **FIX:** Nur statische, regelbasierte Automatisierung („Trigger X → Action Y"), keine eigenständige Entscheidungslogik.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/16550159

### A3. RECORD_AUDIO über AccessibilityService
- **Symptom:** Reject/Removal.
- **Ursache:** „remote call audio recording" / Umgehen von Plattform-Sicherheit über Accessibility ist verboten.
- **Versionen:** 2026.
- **FIX:** RECORD_AUDIO nur als reguläre Runtime-Permission im Vordergrund, nie über Accessibility.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/16558241

### A4. QUERY_ALL_PACKAGES ohne Permitted Use ⭐ HAEUFIG
- **Symptom:** Reject/Removal wegen Package-Visibility.
- **Ursache:** Installierte-App-Liste ist sensibel; nur für Apps erlaubt, die zwingend ALLE Apps kennen müssen (kein Journal).
- **Versionen:** targetSdk 30+.
- **FIX:** Permission entfernen; gezieltes `<queries>`-Element für die konkret benötigten Packages (z. B. TTS-Engine).
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10158779

### A5. MANAGE_EXTERNAL_STORAGE ohne Permitted Use
- **Symptom:** Reject/Removal.
- **Ursache:** All-files-access nur für File-Manager/Backup/Antivirus etc.; ein Journal braucht es nicht.
- **Versionen:** 2026.
- **FIX:** App-Storage/MediaStore/SAF nutzen; Permission entfernen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10467955

### A6. SMS/Call Log im Manifest ohne Default-Handler
- **Symptom:** Sofort-Reject.
- **Ursache:** Diese Permissions dürfen nur im Manifest stehen, wenn die App registrierter Default-Handler ist.
- **Versionen:** 2026.
- **FIX:** Für BestJournal nicht deklarieren; SMS-OTP via SMS-Retriever-API.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10208820

### A7. Background-Location ohne Begründung
- **Symptom:** Reject.
- **Ursache:** ACCESS_BACKGROUND_LOCATION ohne zwingendes Kern-Feature + Console-Declaration.
- **Versionen:** 2026; Location-Button-Minimum ab 28.10.2026.
- **FIX:** Für ein Journal nicht deklarieren; sonst coarse/foreground/Location-Button.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/16558241

### A8. SYSTEM_ALERT_WINDOW / Overlay-Falle (falls Vorlese-Overlay)
- **Symptom:** Reject (Deceptive) oder FGS startet nicht.
- **Ursache:** Overlay tarnt sich als System-UI; Android 15: FGS-Start aus Hintergrund nur bei sichtbarem Overlay.
- **Versionen:** Android 15+.
- **FIX:** Overlay nur sichtbar/nutzerinitiiert, nicht als System-Dialog tarnen; wenn möglich normale Activity.
- **Quelle:** https://developer.android.com/about/versions/15/behavior-changes-15

### A9. Removal als Spyware (Mikrofon-Hintergrund)
- **Symptom:** App entfernt.
- **Ursache:** Audio-Erfassung im Hintergrund / „Always-listening" ohne Disclosure+Consent.
- **Versionen:** 2026.
- **FIX:** Mikrofon nur im Vordergrund, user-initiiert; laufende Aufnahme nur per `microphone`-FGS + Notification + Disclosure; Audio nie verkaufen/teilen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/14745000

---

## P) Pre-Launch-Report

### P1. Inhalt hinter Login ungetestet
- **Symptom:** PLR „sign-in/crawl issue".
- **Ursache:** Keine Test-Credentials, oder WebView/OpenGL-Login (Auto-Login klappt nicht).
- **Versionen:** 2026.
- **FIX:** Test-Account in PLR-Settings (kein Produktiv-Konto); bei WebView/OpenGL Robo-Script aufnehmen; „Sign-in with Google" loggt Crawler selbst ein.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842757

### P2. Obfuskierte Stacktraces im PLR
- **Symptom:** Crash im PLR unlesbar.
- **Ursache:** R8 ohne Mapping.
- **Versionen:** 2026.
- **FIX:** AAB + AGP 4.1+ → mapping.txt automatisch im Bundle (siehe S5).
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842757

### P3. „Test failed" / kein Report
- **Symptom:** PLR läuft nicht durch.
- **Ursache:** Keine Main-Launch-Activity, Root-Check, oder Geo-/Install-Validation blockt den Crawler.
- **Versionen:** 2026.
- **FIX:** Test-Build ohne Geo-/Root-Block; Test-Lab-IP-Ranges allowlisten.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842757

### P4. Production-Crashes trotz „sauberem" PLR
- **Symptom:** Nach Launch Crashes, die der PLR nicht fand.
- **Ursache:** PLR findet nicht alles; testet keine IAP/Käufe.
- **Versionen:** 2026.
- **FIX:** Vor Production internen/geschlossenen Track laufen lassen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9844487

### P5. Geo-/lizenzabhängige Inhalte fehlen
- **Symptom:** Teile der App im Report ungetestet.
- **Ursache:** Test-Geräte in USA; unlizenziert außerhalb Production/Open.
- **Versionen:** 2026.
- **FIX:** Geo-Requirements im Test-Build entfernen; closed Build mit deaktivierter Licensing-Prüfung.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842757

---

## S) App Signing, Mapping & Play Integrity

### S1. Upload „signed with the wrong key"
- **Symptom:** Upload abgelehnt / „different certificate".
- **Ursache:** AAB mit falschem/altem Upload-Key signiert (häufig nach App-Transfer).
- **Versionen:** 2026.
- **FIX:** Mit korrektem Upload-Key signieren; bei Verlust Upload-Key-Reset (PEM) beantragen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842756

### S2. Store-Build: Maps/OAuth/Drive/App-Links tot ⭐ HAEUFIG
- **Symptom:** Funktioniert lokal, im Store-Build nicht.
- **Ursache:** Nur Upload-Key-Fingerprint bei API-Provider registriert, nicht der App-Signing-Key (Google signiert die finale APK).
- **Versionen:** 2026.
- **FIX:** App-Signing-Key-SHA-1/256 aus Play Console → App signing bei Maps/OAuth/Drive/assetlinks.json eintragen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842756

### S3. App-Signing-Key-Verlust (ohne Play App Signing)
- **Symptom:** Dauerhafter Lockout aus der eigenen App.
- **Ursache:** Selbstverwalteter App-Signing-Key verloren — nicht wiederherstellbar.
- **Versionen:** per Design.
- **FIX:** Play App Signing + Google-generierten Key nutzen; Upload-Key + PEM-Backup sichern.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842756

### S4. Datenfreigabe per Shared Key bricht nach Key-Upgrade
- **Symptom:** Custom-Permission-Sharing bricht auf alten Geräten.
- **Ursache:** Neuer Key erst ab API 33 erzwungen; ≤32 kennt nur Legacy-Key.
- **Versionen:** 2026.
- **FIX:** Key-Upgrade-Folgen für ≤API 32 bedenken.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9842756

### S5. Crash-Stacktraces bleiben obfuskiert ⭐ HAEUFIG
- **Symptom:** Vitals-Crashes unlesbar.
- **Ursache:** Keine Mapping-Datei für die Version (APK-Workflow ohne Upload).
- **Versionen:** 2026.
- **FIX:** AAB + AGP 4.1+ → `mapping.txt` automatisch im Bundle; native Symbols `ndk.debugSymbolLevel='FULL'`.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9848633

### S6. Falsche/versions-fremde Mapping hochgeladen
- **Symptom:** Stacktraces wieder obfuskiert/teilweise.
- **Ursache:** Falsche Mapping-Version; unvollständige Mapping (Third-Party-Lib).
- **Versionen:** 2026.
- **FIX:** Im App-Bundle-Explorer korrigieren; vollständige Mapping inkl. Libs.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9848633

### S7. Alte App mit SafetyNet bricht
- **Symptom:** SafetyNet-Aufrufe liefern immer Fehler.
- **Ursache:** SafetyNet Attestation seit Jan 2025 abgeschaltet.
- **Versionen:** seit Jan 2025.
- **FIX:** Auf Play Integrity migrieren; Nutzer zum Update auffordern.
- **Quelle:** https://developer.android.com/privacy-and-security/safetynet/deprecation-timeline

### S8. Echte Nutzer fälschlich von Integrity geblockt
- **Symptom:** Legitime Nutzer ausgesperrt.
- **Ursache:** Hard-Block nur auf höchste Tiers (STRONG); viele Geräte erreichen das nicht.
- **Versionen:** 2026.
- **FIX:** Tiered Enforcement (BASIC/DEVICE/STRONG abgestuft), erst telemetrie-only (phased rollout).
- **Quelle:** https://developer.android.com/google/play/integrity/overview

### S9. Integrity-Verdict gecacht → Replay
- **Symptom:** Proxy-/Replay-Angriff umgeht Integrity.
- **Ursache:** Verdicts gecacht; Classic ohne nonce-Server-Logik.
- **Versionen:** 2026.
- **FIX:** Nicht cachen; Standard-Request on-demand + requestHash/nonce; Classic nur für High-Value-Aktionen.
- **Quelle:** https://developer.android.com/google/play/integrity/overview

---

## M) Metadaten / ASO / targetSdk

### M1. „App muss API 35 targeten" (Upload-Block)
- **Symptom:** Upload abgelehnt.
- **Ursache:** Seit 31.08.2025 müssen neue Apps + Updates API 35+ targeten (Wear/Auto/TV 34+).
- **Versionen:** seit 31.08.2025; rollt jährlich.
- **FIX:** targetSdk 35+ (BestJournal konform); jährlich mitziehen.
- **Quelle:** https://developer.android.com/google/play/requirements/target-sdk

### M2. 16-KB-Page-Size-Reject (nativer Code)
- **Symptom:** Upload-Reject / Bundle-Explorer-Warnung.
- **Ursache:** Seit 01.11.2025 müssen Android-15-targetende Apps 16-KB-Pages unterstützen; nativer Code auf 4-KB hartcodiert.
- **Versionen:** seit 01.11.2025.
- **FIX:** Apps ohne nativen Code automatisch konform; mit nativem Code (z. B. SQLCipher) Libs aktualisieren + neu kompilieren; im Bundle-Explorer prüfen.
- **Quelle:** https://developer.android.com/guide/practices/page-sizes

### M3. „Violation of Metadata policy" ⭐ HAEUFIG
- **Symptom:** Reject wegen Store-Metadaten.
- **Ursache:** Emojis/Sonderzeichen-Spam, ALL-CAPS, „#1/Best/Top/Award", Preise/„Free/Sale", Keyword-Stuffing, Wettbewerber-Namen, CTAs („Download now") — in IRGENDEINER Sprache.
- **Versionen:** 2026.
- **FIX:** Nur Standardzeichen, normale Schreibung, sachliche Beschreibung; verbotene Begriffe aus allen Assets + allen Übersetzungen entfernen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9898842

### M4. Titel/Beschreibung über Limit
- **Symptom:** Eingabe blockiert/abgeschnitten.
- **Ursache:** Titel > 30, Kurz > 80, Voll > 4000, Release-Notes > 500 Zeichen/Sprache (Deutsch wird schnell eng).
- **Versionen:** 2026.
- **FIX:** Limits einhalten; jede Sprache pflegen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9866151

### M5. Grafik-Assets-Specs verfehlt
- **Symptom:** Asset-Upload abgelehnt / fehlende Promo-Sichtbarkeit.
- **Ursache:** Icon ≠ 512×512 PNG; Feature-Graphic ≠ 1024×500 (mit Alpha); < 2 Screenshots; keine Large-Screen-Screenshots.
- **Versionen:** 2026.
- **FIX:** Specs einhalten; ≥4 Screenshots inkl. Large-Screen (16:9/9:16) für Recommendation-Slots.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9866151

### M6. Irreführende Icon-Symbole / Tagline-Überdeckung
- **Symptom:** Metadata-Reject.
- **Ursache:** New-Message-Dot/Download-Symbol ohne Funktion; Tagline > 20 % der Screenshot-Fläche; Geräte-Rahmen.
- **Versionen:** 2026.
- **FIX:** Keine irreführenden Symbole; Tagline klein; echtes UI zeigen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9898842

---

## ✅ Fix-Status (was ist „per Design" vs. zeitkritisch?)

> Hier kein Software-Versions-Fix, sondern Policy-Geltung. „Aktiv seit" = harte Deadline, „per Design" = dauerhaft.

| Reject | Status | Beleg |
|--------|--------|-------|
| M1 (targetSdk 35) | **aktiv seit 31.08.2025** — BestJournal konform | target-sdk requirements |
| M2 (16-KB-Page-Size) | **aktiv seit 01.11.2025** (Android-15-Targeting) | page-sizes |
| T4 (12 statt 20 Tester) | **geändert 11.12.2024** | answer/14151465 |
| D4 (Photo Picker Pflicht) | **erzwungen seit 28.05.2025** | answer/14115180 |
| A2 (autonome Accessibility-Aktionen) | **verboten seit 30.10.2025** | answer/16550159 |
| S7 (SafetyNet) | **abgeschaltet seit Jan 2025** → Play Integrity | safetynet/deprecation |
| F2 (dataSync 6h-Limit) | per Design (Android 15) | behavior-changes-15 |
| R2 (kein Rollback) | per Design | answer/16285429 |
| Übrige | per Design / dauerhafte Policy | jeweilige Quelle |

---

## 📋 Pflicht-Checkliste (vor jedem Play-Release)

- [ ] **Konto:** Bei neuem Personal-Konto Closed Test 12 Tester / 14 Tage erfüllt (T1–T4)
- [ ] **targetSdk 35+** und 16-KB-Page-Size (bei nativem Code) geprüft (M1, M2)
- [ ] **versionCode** monoton erhöht; **kein** Rollback-Plan, nur Halt + höherer Code (R2, R4)
- [ ] **Data-Safety** ↔ Privacy-Policy ↔ Verhalten abgeglichen; jedes SDK deklariert (D1, D5)
- [ ] **Account-Deletion** In-App + Web-URL (falls eigenes Login) (D3)
- [ ] **Photo Picker** statt READ_MEDIA_*; merged Manifest sauber (D4)
- [ ] **Prominent Disclosure** vor Mikrofon-Request; PII nie in Logs (D6, A9)
- [ ] **Drive-Backup** als WorkManager/UIDT, NICHT `dataSync`-FGS (F1, F2)
- [ ] **Mikrofon-FGS** sauber deklariert + Demo-Video (F3, F5)
- [ ] **AccessibilityService** vermieden oder Non-Tool-Declaration + Disclosure (A1, A2)
- [ ] **QUERY_ALL_PACKAGES / MANAGE_EXTERNAL_STORAGE / SMS / Background-Location** nicht deklariert (A4–A7)
- [ ] **Pre-Launch-Report** sauber; Test-Account hinterlegt (P1, P3)
- [ ] **App-Signing-Key-SHA** bei Maps/OAuth/Drive/App-Links eingetragen; Upload-Key-PEM-Backup (S2, S3)
- [ ] **mapping.txt** im Bundle (AAB + AGP 4.1+); SafetyNet → Play Integrity (S5, S7)
- [ ] **Store-Metadaten** ohne „#1/Best/Free"/Emojis/Keyword-Listen in ALLEN Sprachen (M3)
- [ ] **Android Vitals** unter Schwellen (Crash 1,09 %, ANR 0,47 %); Staged Rollout + Vitals-Monitoring (R5)

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Reject-Abschnitt | Best-Practice-Abschnitt (`best-practices-play-store-release.md`) |
|------------------|------------------------------------------------------------------|
| T1–T8 (Tracks/Testing) | §1 (Release-Tracks & Testing-Pflicht) |
| R1–R8 (Rollout/versionCode) | §2 (Rollout/versionCode), §3 (In-App Updates) |
| D1–D9 (Data-Safety) | §4 (Data-Safety & Daten-Policies) |
| F1–F6 (Foreground-Service) | §5 (Foreground-Service-Policy) |
| A1–A9 (Accessibility/Permissions) | §6 (Accessibility & sensible Berechtigungen) |
| P1–P5 (Pre-Launch-Report) | §7 (Pre-Launch-Report) |
| S1–S9 (Signing/Integrity) | §8 (App Signing, Mapping, Play Integrity) |
| M1–M6 (Metadaten/ASO/targetSdk) | §9 (ASO/Store-Listing/targetSdk) |
