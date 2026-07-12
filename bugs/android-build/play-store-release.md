# Bekannte Reject-Gründe & Fallen: Play-Store-Release & Policy

> PFLICHT-LESEN vor Veröffentlichung/Update von BestJournal bei Google Play.
> Stand: tief recherchiert am 2026-06-14 (zwei Läufe, je 7 Researcher — Doku-Lauf + Issue-Tracker-/
> Vorfall-Lauf; ~130 Einträge inkl. Vertiefung §TV/§RV/§DV/§FV/§AV/§SV/§MV mit realen Reject-Mails).
> Versions-Anker: Play-Policy/Console **Juni 2026** · BestJournal targetSdk **35**, versionCode **144**,
> versionName **0.19.11**, AAB, R8; nutzt `RECORD_AUDIO`, Google-Drive-Backup, LLM/TTS-APIs.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/android-build/play-store-release.md`](../../best-practices/android-build/play-store-release.md).

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

# ── Vertiefung (Issue-Tracker-/Vorfall-Lauf 2026-06-14) ──

## TV) Tracks & Testing — reale Vorfälle

### TV1. Production-Reject = schlechte Fragebogen-Antworten (nicht schlechtes Testen) ⭐ HAEUFIG
- **Symptom:** 12/14 erfüllt, „Apply for production" trotzdem abgelehnt.
- **Ursache:** Der 10-Fragen-Production-Questionnaire ist das eigentliche Tor; „No changes were needed"/Lob-Antworten = Reject.
- **Versionen:** 2026.
- **FIX:** Konkretes Feedback nennen (z. B. „3 Tester: Login-Button reagierte auf Android 12 nicht") + welche Änderung folgte; Crashes ehrlich (PLR kennt die Antwort); ~250–300 Zeichen/Frage.
- **Quelle:** https://dev.to/tizoc_araujo_3cd9fb67191f/the-google-play-production-access-questionnaire-...-egh

### TV2. Tester opted-in, zählt aber nicht ⭐ HAEUFIG
- **Symptom:** Tester „hat alles gemacht", Console zählt ihn nicht.
- **Ursache:** APK-/Sideload-Install (nicht aus Store), Install VOR Opt-in, Opt-in mit Konto A + Install mit Konto B, Emulator/Bot/Duplikat, oder still abgebrochener Store-Install.
- **Versionen:** 2026.
- **FIX:** Reihenfolge erzwingen: Opt-in-Link → aus Store installieren → mind. 1× öffnen; exakt dasselbe Konto; echte physische Geräte.
- **Quelle:** https://12testers14days.com/knowledgebase/closed-testing-errors-troubleshooting/testers-installed-but-not-counted

### TV3. „Days not updating" — kein retroaktives Nachzählen
- **Symptom:** Tag-Zähler bleibt stehen.
- **Ursache:** Ein Tag zählt nur, wenn an DEM Tag ≥12 aktiv + Usage-Signal; verlorene Tage werden NIE nachgezählt.
- **Versionen:** 2026 (Verhaltensdesign).
- **FIX:** 12 aktive Tester wiederherstellen, App öffnen lassen, bis 24 h auf Refresh warten.
- **Quelle:** https://12testers14days.com/knowledgebase/closed-testing-errors-troubleshooting/closed-testing-days-not-updating

### TV4. Streak-Reset bei kurzem Drop unter 12 / Konfig-Eingriff
- **Symptom:** 14-Tage-Streak springt auf 0.
- **Ursache:** Aktiver Count kurz < 12 ODER Tester-Listen/Track/Konfig mitten im Test geändert (NEUE Release-Uploads resetten NICHT).
- **Versionen:** 2026.
- **FIX:** Auf 16–20 über-rekrutieren; während 14 Tagen nur App-Updates pushen, Listen/Tracks/Konfig nicht anfassen.
- **Quelle:** https://earezki.com/ai-news/2026-05-17-why-your-google-play-14-day-testing-clock-keeps-resetting...

### TV5. „App not available" — 5 Setup-Bugs blockieren den Test-Start ⭐ HAEUFIG
- **Symptom:** Tester sehen die App nicht, Zähler läuft nie an.
- **Ursache:** (1) Release auf Track A, Tester-Liste an Track B; (2) Liste hochgeladen, aber **Checkbox nicht angehakt** (#1-Ursache); (3) Internal-Link statt Closed-Link geteilt; (4) Länder-Restriktion; (5) „Send changes for review" vergessen.
- **Versionen:** 2026.
- **FIX:** Liste am selben Track + Checkbox „Selected for this track"; Closed-Link `play.google.com/apps/testing/<pkg>`; alle Länder targeten; Änderungen zur Review senden.
- **Quelle:** https://primetestlab.com/blog/google-play-app-not-available-to-testers

### TV6. Account-Termination durch Identity-Association (AI-Enforcement)
- **Symptom:** Sauberes Konto plötzlich „high-risk behavior"/§8.3 terminiert, oft ohne menschliche Prüfung.
- **Ursache:** Google mappt Device-Fingerprint/IP/Zahlungsmethode/Telefon auf zuvor terminierte Konten (gebraucht gekaufte Hardware, geteiltes Büro-IP, gleiche Karte); §10.3 = ohne Reinstatement.
- **Versionen:** Welle 2026.
- **FIX:** In-App-Appeal sofort (Paper-Trail) + Eskalation über Diamond-Product-Expert-Forum + „Plan of Action"-Doku; dedizierte E-Mail/Zahlung/Hardware pro Konto.
- **Quelle:** https://gologin.com/blog/google-play-account-banned/

### TV7. 180-Tage-Appeal-Fenster + 30-Tage-Nachschlag
- **Symptom:** Nach Termination harte Frist, danach permanent geschlossen.
- **Ursache:** Seit 28.01.2026 müssen Appeals binnen 180 Tagen ab Termination; späte Einreichung gibt +30 Tage für Re-Review.
- **Versionen:** seit 28.01.2026.
- **FIX:** Früh einreichen; Frist ab Termination-Datum.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/16659089

### TV8. Org-Verifizierung hängt / D-U-N-S-Mismatch
- **Symptom:** Neues Org-Konto bleibt „verifying your identity"; kein Publish.
- **Ursache:** Verifizierungs-Queue (ab März 2026 Pflicht für alle); D-U-N-S-Profil ≠ Payment-Profile.
- **Versionen:** 2026.
- **FIX:** Bei drohender Fristüberschreitung „Get a 90 day extension" auf der Console-Startseite; D-U-N-S-Felder 1:1 zum Payment-Profile abgleichen; Forum-Eskalation.
- **Quelle:** https://support.google.com/googleplay/android-developer/thread/295672315 · https://support.google.com/googleplay/android-developer/answer/10841920

### TV9. Fake-Tester-Dienste (Emulator/Bot) = Ban-Risiko
- **Symptom:** Sofortige Production-Denial, evtl. Permanent-Ban.
- **Ursache:** Emulator/Bot/Duplikat verstoßen gegen Spam-/Behavior-Policy; Engagement-Time-Messung erkennt Fake-Aktivität.
- **Versionen:** 2026.
- **FIX:** Nur echte Tester auf physischen Geräten mit echter Aktivität; „bezahlt" ≠ verboten, „fake/automatisiert" ist das Risiko.
- **Quelle:** https://20apptester.com/2026/06/03/google-play-closed-testing-12-testers-faq/

## RV) Rollout / In-App-Updates / Publisher-API

### RV1. `AppUpdateInfo` ist Einmal-Token → Retry liefert kein Result ⭐ HAEUFIG
- **Symptom:** Zweiter `startUpdateFlowForResult` startet nichts, kein Callback, Flow hängt still.
- **Ursache:** Jede `AppUpdateInfo` ist nur für genau einen Flow-Start gültig.
- **Versionen:** `app-update` bis 2.1.0, per Design.
- **FIX:** Vor jedem (auch wiederholtem) Start frisches `getAppUpdateInfo()` holen, nie cachen.
- **Quelle:** https://developer.android.com/reference/com/google/android/play/core/appupdate/AppUpdateManager

### RV2. `IntentSender$SendIntentException`-Crash beim Flow-Start
- **Symptom:** App stürzt bei `startUpdateFlowForResult` ab.
- **Ursache:** Gelieferter `IntentSender` zum Sendezeitpunkt ungültig (Play-Prozess neu/Token abgelaufen).
- **Versionen:** native + Flutter.
- **FIX:** In try/catch(`SendIntentException`); im Catch frisches `getAppUpdateInfo()` + neu aufsetzen, nicht crashen.
- **Quelle:** https://github.com/jonasbark/flutter_in_app_update/issues/59

### RV3. Flexible Update bleibt auf `DOWNLOADED` hängen ⭐ HAEUFIG
- **Symptom:** `installStatus==DOWNLOADED`, aber Installation passiert nie.
- **Ursache:** Anders als IMMEDIATE löst FLEXIBLE keinen Auto-Restart aus; App muss `completeUpdate()` rufen.
- **Versionen:** per Design.
- **FIX:** `InstallStateUpdatedListener` + bei `DOWNLOADED` UI/`completeUpdate()`; **zusätzlich** bei jedem `onResume` `getAppUpdateInfo()` prüfen und nachholen; Listener danach deregistrieren.
- **Quelle:** https://developer.android.com/guide/playcore/in-app-updates/kotlin-java

### RV4. IMMEDIATE-Update-Loop / Resume statt Neustart
- **Symptom:** Pause/Resume-Schleife; User „eingesperrt".
- **Ursache:** Flow blind in `onResume` neu gestartet ohne `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS`-Check.
- **Versionen:** wiederkehrend.
- **FIX:** In `onResume` zuerst `getAppUpdateInfo()`; nur bei `DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS` den Flow **fortsetzen**, nicht neu starten.
- **Quelle:** https://github.com/motorro/AppUpdateWrapper

### RV5. Play Core deprecated → `app-update` 2.x + `gms.tasks`-Migration
- **Symptom:** `Task`/`OnSuccessListener` lassen sich nach Migration nicht auflösen.
- **Ursache:** Neue Libs nutzen GMS-Task-API; alte `play.core:core` ist deprecated und wird abgewiesen.
- **Versionen:** `com.google.android.play:app-update:2.1.0`.
- **FIX:** Importe `com.google.android.play.core.tasks.*` → `com.google.android.gms.tasks.*`; nur feingranulare Libs einbinden.
- **Quelle:** https://developer.android.com/reference/com/google/android/play/core/release-notes

### RV6. `UPDATE_NOT_AVAILABLE` im Test trotz Update
- **Symptom:** `updateAvailability()` dauerhaft `UPDATE_NOT_AVAILABLE`.
- **Ursache:** Debug-Build; gleicher versionCode; Internal App Sharing ignoriert `inAppUpdatePriority`; stale Play-Cache (Update-Check ~1×/Tag).
- **Versionen:** per Design.
- **FIX:** Zwei signierte Builds (niedriger→höher) über Internal App Sharing; Play Store öffnen für Cache-Refresh; Priority nur über echte Tracks testen.
- **Quelle:** https://developer.android.com/guide/playcore/in-app-updates/test

### RV7. `clientVersionStalenessDays` ist null nach Release
- **Symptom:** Staleness-basierte Force-Update-Logik greift nie.
- **Ursache:** Wert zählt ab „Play kennt das Update auf dem Gerät", nicht ab Upload — direkt nach Release null.
- **Versionen:** per Design.
- **FIX:** Immer auf null prüfen (`?: 0`); für deterministisches Force-Update serverseitiges Min-Version-Gate.
- **Quelle:** https://developer.android.com/reference/com/google/android/play/core/appupdate/AppUpdateInfo

### RV8. „Outstanding release" / Draft-Upload haltet laufenden Rollout
- **Symptom:** „Cannot create new release" / Rollout unerwartet gehalten.
- **Ursache:** Nicht-100%-Rollout ODER vergessener Draft zählt als „outstanding"; neuer Draft auf demselben Track haltet aktiven Rollout.
- **Versionen:** per Design.
- **FIX:** Auf Publishing-overview discarden oder Rollout abschließen; während aktivem Rollout keinen neuen Draft auf denselben Track.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9859348

### RV9. Länder nach Rollout-Start nicht entfernbar
- **Symptom:** Land lässt sich nicht aus laufendem Rollout nehmen.
- **Ursache:** Länder-Verfügbarkeit nach Start eingefroren — nur hinzufügen.
- **Versionen:** per Design.
- **FIX:** Länderscope vor Start final; sonst Rollout halten + neue Version.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/6346149

### RV10. Review hängt Wochen, kein Auto-Eskalationspfad
- **Symptom:** „In review"/„Checking" über Wochen.
- **Ursache:** Reale Verzögerungen 2025/26 (neue Konten/Production); nachgeschobener Edit setzt die Uhr zurück; kein Auto-Resubmit.
- **Versionen:** 2025/2026.
- **FIX:** Während Review nichts ändern; nach >7 Tagen Case über „Help" öffnen; nicht knapp vor Deadlines einreichen.
- **Quelle:** https://support.google.com/googleplay/android-developer/thread/385736814

### RV11. Publisher-API: „Edit is in an invalid state" / abgelaufen
- **Symptom:** `commit`/`get` schlägt fehl, Edit nicht gefunden.
- **Ursache:** Edit zeitlich begrenzt; ungültig wenn gelöscht/superseded/abgelaufen; lange CI/parallele Builds.
- **Versionen:** androidpublisher v3.
- **FIX:** `insert→uploads→commit` zügig; bei Fehler frischen Edit; parallele Pipelines auf denselben Track serialisieren.
- **Quelle:** https://developers.google.com/android-publisher/api-ref/rest/v3/edits

### RV12. 409 `apkUpgradeVersionConflict` / Fastlane lädt alle APKs
- **Symptom:** Upload bricht mit 409 / „version code already used".
- **Ursache:** versionCode-Kollision, konkurrierende Edits, oder Fastlane `supply` lädt alle gefundenen APKs (inkl. Debug/Flavors).
- **Versionen:** Fastlane laufend.
- **FIX:** versionCode monoton; alte Artefakte vor `supply` aufräumen oder `apk_paths` gezielt; parallele Uploads sperren.
- **Quelle:** https://github.com/fastlane/fastlane/issues/16331

### RV13. `changesNotSentForReview`-Falle (zustandsabhängig)
- **Symptom:** `commit` fehlt mit „must set changesNotSentForReview=true" ODER gespiegelt „must not be set".
- **Ursache:** Im rejected-Zustand muss der Parameter `true` sein; im Normalzustand darf er nicht gesetzt sein.
- **Versionen:** EAS/Fastlane/Codemagic.
- **FIX:** App-Zustand vor Commit prüfen; bei rejected `true` + manuell „Send for review"; sonst weglassen; CI mit Retry-Flag.
- **Quelle:** https://github.com/expo/eas-cli/issues/489

### RV14. „Bundle uploads not completed yet" bei `completed`
- **Symptom:** AAB-Upload bricht bei `release_status: completed`.
- **Ursache:** Race zwischen Upload-Finalisierung und Commit.
- **Versionen:** Fastlane.
- **FIX:** Erst `draft` hochladen, dann separat auf completed/rollout setzen.
- **Quelle:** https://github.com/fastlane/fastlane/issues/21126

### RV15. Release-Notes: Tool zählt Gesamtdokument / „some languages have errors"
- **Symptom:** „length 755, too long (max 500)" trotz kurzer Texte; oder „some languages have errors".
- **Ursache:** 500-Zeichen-Limit gilt **pro Sprache**, manche Tools zählen das ganze Dokument; fehlender Sprach-Fallback / falsches Locale-Tag (`de_DE` statt `de-DE`).
- **Versionen:** Tool-Bug.
- **FIX:** Pro-Sprache-Blöcke getrennt; Default-Sprache muss Notes haben; exakte BCP-47-Tags; bei vielen Sprachen batchweise.
- **Quelle:** https://github.com/microsoft/google-play-vsts-extension/issues/157

## DV) Data-Safety — reale Reject-Vorfälle

### DV1. Firebase-BOM-Bump → Phantom-Datentyp „Email Address"
- **Symptom:** Nach BOM-Upgrade Reject „Personal Info Data Type - Email Address", obwohl keine E-Mail-Auth.
- **Ursache:** Scanner verknüpft transitive Deps mit Firebase-Disclosure-Katalog; BOM-Bump schleppt still neue Datentypen ein.
- **Versionen:** Firebase-BOM 31.1.1; Issue ohne klare Root-Cause.
- **FIX:** Pro Sub-SDK gegen die offizielle Firebase-Disclosure-Tabelle abgleichen; ohne Firebase-Auth keinen Email-Typ deklarieren, Fehl-Zuordnung über Help anfechten; bei jedem BOM-Bump Data-Safety neu reviewen.
- **Quelle:** https://github.com/firebase/firebase-android-sdk/issues/4478

### DV2. Flutter/Expo-Plugins schleusen `READ_MEDIA_*` via merged Manifest ein ⭐ HAEUFIG
- **Symptom:** Photo-/Video-Reject, obwohl Permission nie selbst deklariert.
- **Ursache:** Plugins (`image_picker`/`file_picker`/`permission_handler`) bringen Manifest-Fragmente; Gradle merged sie ins AAB.
- **Versionen:** Policy voll erzwungen 28.05.2025.
- **FIX:** `<uses-permission android:name="…READ_MEDIA_IMAGES" tools:node="remove"/>` (+ VIDEO/AUDIO/READ_EXTERNAL_STORAGE maxSdk32); `xmlns:tools` deklarieren; **merged Manifest** prüfen; Picker-Funktion bleibt.
- **Quelle:** https://dev.to/alaminkarno/bro-my-app-got-rejected-but-i-didnt-even-add-those-permissions-48o7

### DV3. Photo-Picker-Migration crasht auf alten Geräten (null-Intent)
- **Symptom:** Crash auf Android ≤10 beim Bild-Auswählen (`requireNotNull(intent.data)`).
- **Ursache:** Backported Picker liefert in bestimmten Konstellationen null-Intent; vor Release nicht auf Altgeräten getestet.
- **Versionen:** expo-image-picker 14.3.0–14.3.1; generisch.
- **FIX:** `isPhotoPickerAvailable()` prüfen; in `parseResults` defensiv null-prüfen statt `requireNotNull`; auf API 28/29 testen.
- **Quelle:** https://github.com/expo/expo/issues/23020

### DV4. AdMob/Ads ohne zertifizierte CMP/UMP → EU-Consent-Verstoß
- **Symptom:** Ads gedrosselt/blockiert in EEA/UK/CH; Beanstandung.
- **Ursache:** Fehlende Google-zertifizierte CMP + UMP-Consent-Flow; TCF-v2.3-Pflicht bis 28.02.2026.
- **Versionen:** 2026.
- **FIX:** UMP-SDK: bei App-Start `requestConsentInfoUpdate()` + `loadAndShowConsentFormIfRequired()`; „Privacy options"-Widerruf; ohne Ads UMP/AdMob weglassen + in Data-Safety kein Ad-ID-Sharing.
- **Quelle:** https://developers.google.com/admob/android/privacy

### DV5. „Inaccurate disclosure" durch app-kontrollierten WebView
- **Symptom:** Data-Safety-Reject wegen undeklarierter WebView-Datenflüsse.
- **Ursache:** Daten aus einem WebView, dessen Code/Verhalten die App kontrolliert (LLM/TTS-Endpoints), müssen deklariert werden — nur offenes-Web-Browsen ist ausgenommen.
- **Versionen:** laufend.
- **FIX:** Jeden app-kontrollierten WebView/SDK-Endpoint als Collection/Sharing deklarieren, identisch in der Privacy-Policy.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10787469

### DV6. Account-Deletion-Web-URL abgelehnt — 3 konkrete Trigger
- **Symptom:** URL hinterlegt, trotzdem abgelehnt.
- **Ursache:** (1) Redirect zurück in die App; (2) App-/Entwicklername fehlt; (3) nicht erreichbar/nicht direkt/geo-gesperrt.
- **Versionen:** 2025/2026.
- **FIX:** Eigenständige Web-Löschseite, die die Löschung im Web initiierbar macht, App-/Entwicklernamen wie im Listing nennt, ohne Redirect/Zwischenseiten direkt + global erreichbar.
- **Quelle:** https://www.termsfeed.com/blog/google-data-safety-form-delete-account-url/

### DV7. Privacy-Policy-Reject — Name-Mismatch / Geo-Block
- **Symptom:** „invalid Privacy Policy URL" trotz vorhandener Policy.
- **Ursache:** Entwicklername in Policy ≠ Listing; geo-gefencete/CDN-gesperrte Seite; editierbar/PDF.
- **Versionen:** 2025/2026.
- **FIX:** Globale HTML-Seite (kein PDF/Login/Geo-Block), exakter Entwickler-/App-Name; aus mehreren Regionen testen.
- **Quelle:** https://www.termsfeed.com/blog/invalid-privacy-policy-url-google/

### DV8. Prominent-Disclosure sieht aus wie System-UI
- **Symptom:** Reject „looks like system UI" trotz Dialog.
- **Ursache:** Weißer Hintergrund/System-ähnliches Layout; Wording unvollständig.
- **Versionen:** laufend.
- **FIX:** Dialog im App-Theme (nicht weiß); Format „[App] collects/transmits [Datentyp] to enable [Feature], [Szenario]"; vor Datenzugriff, affirmatives Consent, nicht gebündelt.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/11150561

### DV9. AdMob/Analytics: Device-/Advertising-ID nicht deklariert
- **Symptom:** Reject wegen undeklarierter Device-/Advertising-ID.
- **Ursache:** AdMob/Analytics-SDK sammelt IDs; Entwickler deklariert sie nicht.
- **Versionen:** laufend.
- **FIX:** Pro SDK die offizielle Disclosure-Tabelle durchgehen, ALLE Datentypen deklarieren; ungenutzte Ads/Analytics-SDKs entfernen.
- **Quelle:** https://firebase.google.com/docs/android/play-data-disclosure

### DV10. App-Removal-Wellen 2025/2026 + Enforcement-Zahlen
- **Symptom:** „all apps will be removed on [Datum]"/„needs attention".
- **Ursache:** Überlappende Deadlines (Data-Safety/Photo-Picker 28.05.2025/Account-Deletion/Verification); 2025: ~1,75 Mio Einreichungen blockiert, 80.000+ Konten gesperrt.
- **Versionen:** Wellen 2025/2026.
- **FIX:** Policy-Deadlines aktiv monitoren; bei „needs attention" sofort handeln (Verlängerung nicht garantiert); Korrektur früh einreichen (Re-Review dauert Tage).
- **Quelle:** https://www.bleepingcomputer.com/news/security/google-blocked-over-175-million-play-store-app-submissions-in-2025/

## FV) Foreground-Service — reale Reject-/Crash-Vorfälle

### FV1. TTS-Vorlesen im Hintergrund → `mediaPlayback`, NICHT `microphone`/`specialUse` ⭐ projektkritisch
- **Symptom:** Unsicherheit/Reject welcher FGS-Typ fürs Vorlesen.
- **Ursache:** TTS erzeugt hörbare Wiedergabe → Media-Playback; `microphone` ist Aufnahme (falsch), `specialUse` überflüssig+reviewriskant.
- **Versionen:** Android 14/15.
- **FIX:** `foregroundServiceType="mediaPlayback"` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (unbegrenzt erlaubt, **kein 6h-Limit**); microphone- und mediaPlayback-FGS **strikt trennen** (sonst zieht RECORD_AUDIO die Background-Restriktionen aufs Vorlesen).
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/service-types

### FV2. `specialUse` abgelehnt — „permissions do not match core functionality"
- **Symptom:** App wiederholt abgelehnt; specialUse-Begründung verworfen.
- **Ursache:** Generische/geteilte specialUse-Begründung; Funktion nicht user-perceptible/-initiated; Google liest primär den Manifest-`android:value`-Text.
- **Versionen:** targetSdk 34/35.
- **FIX:** specialUse meiden (FGS via Bind statt Start, oder Standardtyp); falls nötig app-spezifische ausführliche Begründung im Manifest, nie Library-Default.
- **Quelle:** https://github.com/joaomgcd/TaskerPluginSample/issues/24 · https://community.appinventor.mit.edu/t/google-play-console-rejected-again-my-trapon/120868

### FV3. `mediaPlayback`-Crashwelle `ForegroundServiceStartNotAllowedException`
- **Symptom:** Crashlytics-Welle (Galaxy/OPPO, v. a. Android 12), nicht lokal reproduzierbar.
- **Ursache:** Nach pause/error/stop fällt der Service aus dem Foreground; folgendes `play()` (Audio-Focus-Wechsel, Track-Übergang) startet FGS aus Background → Crash. Bei TTS nach jedem Satz neu.
- **Versionen:** Android 12–13; teils gefixt Media3 1.6.0.
- **FIX:** ExoPlayer mit automatischem Audio-Focus (`AudioAttributes…, true`); Media3 ≥ 1.6.0 (hält Foreground 10 Min nach pause/stop); oder `onUpdateNotification(startInForegroundRequired=true)`.
- **Quelle:** https://github.com/androidx/media/issues/111

### FV4. `ForegroundServiceDidNotStartInTimeException` nur im Pre-Launch-Report
- **Symptom:** PLR-Crash auf Pixel-Testgeräten, real nicht reproduzierbar.
- **Ursache:** FGS muss kurz nach `startForegroundService()` `startForeground()` rufen; in gedrosselter Test-Umgebung verzögert → Flag zu spät.
- **Versionen:** targetSdk 34.
- **FIX:** `startForeground()` synchron + so früh wie möglich (Channel/Builder vorab); keine schwere Init davor; PLR-only-Crashes kritisch prüfen, nicht blind blocken.
- **Quelle:** https://community.appinventor.mit.edu/t/foreground-service-pre-launch-report-crash-on-android-13-and-14/138449

### FV5. SDK (Firebase Messaging) zieht FGS rein → Typ unklar
- **Symptom:** „Missing foreground service type" als Prod-Crash, obwohl nie selbst `startForeground`.
- **Ursache:** Library startet intern einen FGS; unter Android 14 braucht auch der einen Typ.
- **Versionen:** targetSdk 34.
- **FIX:** Pro Library-Service korrekten Typ + Permission (FCM: `FOREGROUND_SERVICE_REMOTE_MESSAGING` + `remoteMessaging`); nicht pauschal `shortService`.
- **Quelle:** https://www.b4x.com/android/forum/threads/missing-foreground-service-type-google-play-crash-report.163821/

### FV6. WorkManager nutzt intern FGS → Deklaration nötig (oder ohne setForeground)
- **Symptom:** `MissingForegroundServiceTypeException` bei `setForeground`.
- **Ursache:** WorkManager startet für expedited/long-running Work intern einen FGS; Typ am `SystemForegroundService` nötig.
- **Versionen:** Android 14+; WorkManager-Fix in 2.10.5.
- **FIX:** Drive-Backup als **normalen Worker ohne `setForeground`** (deferrable/expedited) → kein FGS, keine Deklaration; nur bei echtem Foreground Typ deklarieren + WorkManager ≥ 2.10.5.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/troubleshooting

### FV7. `shortService`-Missverständnisse
- **Symptom:** `shortService` pauschal gesetzt, dann Timeout/Crash.
- **Ursache:** Nur für <3-Min-Aufgaben; kein Sticky, kann keine anderen FGS starten; `onTimeout()`→Crash.
- **Versionen:** Android 14+.
- **FIX:** Korrekten Typ nach Funktion (Vorlesen→mediaPlayback unbegrenzt; Sync→WorkManager); `shortService` nur für echte Sub-3-Min-Tasks.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/timeout

### FV8. FGS-Demo-Video zeigt Auslöse-Schritt nicht
- **Symptom:** Reject-Loop trotz Video.
- **Ursache:** Video belegt user-initiated/-perceptible/Background-Weiterlauf nicht.
- **Versionen:** targetSdk 34/35.
- **FIX:** User-Aktion → FGS startet → App in Background → Funktion läuft weiter + Notification sichtbar.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/13392821

## AV) Accessibility / Permissions — reale Removals

### AV1. Accessibility-Verschärfung (28.01.2026): „autonom" im Klartext verboten
- **Symptom:** Verschärfter Review für AccessibilityService ab 28.01.2026.
- **Ursache:** Neuer Wortlaut verbietet „autonomously initiate, plan, and execute actions" (KI liest Screen/tippt, Auto-Login, Agenten, „Do it for me").
- **Versionen:** ab 28.01.2026.
- **FIX:** Vorlesen nur als echtes Accessibility-Tool; „Auslösen" an unmittelbare explizite Nutzeraktion koppeln (Nutzer tippt → eine Aktion), nicht autonom; Datennutzung offenlegen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/16585319

### AV2. TTS-Engine-Sichtbarkeit via `<queries>` statt QUERY_ALL_PACKAGES ⭐ projektrelevant
- **Symptom:** App lief auf Android 9, ab Android 11 **kein TTS-Ton mehr** (silent fail, `queryIntentActivities` leer).
- **Ursache:** Package-Visibility-Filter ab API 30 macht die TTS-Engine (eigenes Paket) unsichtbar.
- **Versionen:** targetSdk ≥ 30.
- **FIX:** `<queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>` — KEIN QUERY_ALL_PACKAGES, keine Declaration-Form nötig.
- **Quelle:** https://developer.android.com/training/package-visibility/use-cases

### AV3. QUERY_ALL_PACKAGES — Permitted Uses vs. Reject/Removal
- **Symptom:** Declaration-Form abgelehnt; App entfernt wenn Form fehlt.
- **Ursache:** Installierte-App-Liste = sensibel; nur für Device-Search/Antivirus/File-Manager/Browser/Launcher; Verkauf/Sharing für Analytics verboten.
- **Versionen:** seit Summer 2021.
- **FIX:** Für Vorlese-/Journal-App weglassen; gezieltes `<queries>` (AV2).
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10158779

### AV4. SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM — Declaration-Falle + Remove-Loop
- **Symptom:** Reject ohne Alarm-Kernzweck; oder Update zum **Entfernen** der Permission wird blockiert.
- **Ursache:** `USE_EXACT_ALARM` (API 33+) restricted, nur für Wecker/Kalender; `SCHEDULE_EXACT_ALARM` ab Android 13 denied-by-default.
- **Versionen:** Android 13+.
- **FIX:** Für Journal/Vorlesen weglassen → `setWindowAlarm`/WorkManager; falls zwingend `SCHEDULE_EXACT_ALARM` + `canScheduleExactAlarms()` + inexakter Fallback.
- **Quelle:** https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

### AV5. POST_NOTIFICATIONS — Declaration vs. Runtime
- **Symptom:** Keine Notifications / `requestPermission` liefert sofort denied ohne Dialog.
- **Ursache:** Braucht Manifest UND Runtime-Request; bei targetSdk ≤32 auf Android 13 immer denied ohne Dialog.
- **Versionen:** Android 13+.
- **FIX:** Manifest-Permission + kontextueller `requestPermissions(...)` + targetSdk ≥ 33.
- **Quelle:** https://developer.android.com/develop/ui/views/notifications/notification-permission

### AV6. RECORD_AUDIO — 30-Tage-Grace + Removal-Mechanik
- **Symptom:** Removal „unexpected collection of audio".
- **Ursache:** Mikrofon sensibel → Privacy-Policy + Prominent Disclosure Pflicht; bei Neu-Enforcement ≥30 Tage Frist, danach Removal. Audio über Accessibility = sofort Reject.
- **Versionen:** laufend.
- **FIX:** RECORD_AUDIO nur über direkten user-initiierten Flow (`microphone`-FGS), nie Accessibility; Policy + Disclosure (DV8).
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9899234

### AV7. Developer-Verification-Pflicht (auch Sideload)
- **Symptom:** Ab 2026/2027 muss jede installierte App (auch Sideload/F-Droid) an ein verifiziertes Konto gebunden sein.
- **Ursache:** Ankündigung 25.08.2025; Go-live alle Entwickler März 2026, erste Länder Sept 2026, global 2027.
- **Versionen:** Rollout läuft.
- **FIX:** Verifizierung früh einplanen; Sideload ist kein Reject-Schutzschild mehr.
- **Quelle:** https://techcrunch.com/2025/08/25/google-will-require-developer-verification-for-android-apps-outside-the-play-store/

## SV) PLR / Signing / Integrity — reale Vorfälle

### SV1. Robo-Test scheitert am OAuth-Sign-In (`cloudtestlabaccounts.com`) ⭐ projektrelevant
- **Symptom:** PLR-Crash beim „Sign in with Google" (Code 10 DEVELOPER_ERROR), real funktioniert es.
- **Ursache:** Robo nutzt ein Test-Lab-Konto (`@cloudtestlabaccounts.com`); OAuth verweigert es → Umgebungs-Artefakt.
- **Versionen:** 2026.
- **FIX:** Eigene Test-Account-Credentials in PLR-Settings hinterlegen (Resource-Namen); App-Signing-Key-SHA-1 als OAuth-Client registrieren.
- **Quelle:** https://github.com/firebase/FirebaseUI-Android/issues/1928

### SV2. App-Links/OAuth/Drive brauchen App-Signing-Key-SHA-256, nicht Upload-Key ⭐ projektkritisch
- **Symptom:** App-Links/OAuth/Drive funktionieren lokal, im Store-Build still nicht (für alle Nutzer).
- **Ursache:** #1-Fehlerursache SHA-256-Mismatch — Upload-Key-Fingerprint statt Googles App-Signing-Key in `assetlinks.json`/OAuth-Client.
- **Versionen:** dauerhaft.
- **FIX:** App-Signing-Key-SHA-256 aus Play Console (Setup → App-Signing) verwenden, exakt/uppercase; ggf. beide (Upload+App-Signing) als Array.
- **Quelle:** https://developer.android.com/training/app-links/troubleshoot

### SV3. Upload-Key-Reset: 2–3 Werktage, braucht aktives Play App Signing
- **Symptom:** „signed with a key that is not your upload key".
- **Ursache:** Reset nur möglich, wenn Play App Signing aktiv; sonst Key-Verlust fatal.
- **Versionen:** 2025/2026.
- **FIX:** Neuen Keystore + PEM, Reset-Formular; 2–3 Werktage; Bestandsnutzer unbetroffen. Diagnose `keytool -printcert -jarfile app.aab` gegen Console-Upload-Cert.
- **Quelle:** https://support.google.com/googleplay/android-developer/community-guide/243925915

### SV4. mapping.txt nicht automatisch hochgeladen in CI/Fastlane
- **Symptom:** „No deobfuscation file" trotz AAB + R8; Stacktraces obfuskiert.
- **Ursache:** Programmatischer Upload (Fastlane/Publisher-API) nimmt den Mapping-Pfad nicht mit.
- **Versionen:** Fastlane laufend.
- **FIX:** `mapping.txt` als Artefakt führen + `mapping_paths`/`mapping`-Parameter setzen; zur exakten versionCode passend (wirkt nur prospektiv).
- **Quelle:** https://github.com/fastlane/fastlane/issues/21064

### SV5. Native Debug Symbols > 800 MB
- **Symptom:** Symbol-Upload scheitert / Warnung.
- **Ursache:** `debugSymbolLevel='FULL'` zu groß bei vielen ABIs.
- **Versionen:** 2026.
- **FIX:** `ndk.debugSymbolLevel='SYMBOL_TABLE'` (kleiner, weiterhin lesbar).
- **Quelle:** https://developer.android.com/build/include-native-symbols

### SV6. Integrity `UNRECOGNIZED_VERSION`/`UNEVALUATED` blockt Tester
- **Symptom:** Standard-Request liefert kein `PLAY_RECOGNIZED`; interne Tester ausgesperrt.
- **Ursache:** `UNRECOGNIZED_VERSION` bei Sideload/adb/Test-Track/Debug-Signatur; `UNEVALUATED` bei fehlendem Warmup/Geräte-Vertrauen.
- **Versionen:** 2025/2026.
- **FIX:** Test-Builds über Play (Internal App Sharing) installieren; Token-Provider warm-up + Backoff; `UNEVALUATED` als „unklar" behandeln, Tester nicht hart blocken.
- **Quelle:** https://developer.android.com/google/play/integrity/verdicts

### SV7. Integrity Mai-2025: Custom-ROM/GrapheneOS fallen durch DEVICE_INTEGRITY
- **Symptom:** Legitime Power-User (unlocked Bootloader/Custom-ROM) ausgesperrt.
- **Ursache:** `MEETS_DEVICE_INTEGRITY` verlangt auf Android 13+ gesperrten Bootloader (hardware-backed).
- **Versionen:** Default seit Mai 2025.
- **FIX (App-Seite):** `MEETS_BASIC_INTEGRITY` als Mindestschwelle; höhere Stufen nur für sensible Aktionen; Soft-Block statt Total-Lockout (für Drive/OAuth reicht Basic).
- **Quelle:** https://www.androidauthority.com/google-play-integrity-hardware-attestation-3561592/

### SV8. Integrity-Quota 10.000/Tag + serverseitige Token-Prüfung
- **Symptom:** `TOO_MANY_REQUESTS`/Throttling; manipulierte/„stale" Verdicts.
- **Ursache:** Default 10k/Tag pro Cloud-Project; client-seitige Entschlüsselung ohne `requestHash`/`nonce`-Prüfung.
- **Versionen:** 2026.
- **FIX:** Nicht pro Aktion prüfen, Token cachen, Backoff, Quota-Erhöhung beantragen; serverseitig entschlüsseln, `requestDetails` zuerst gegen Originalanfrage prüfen.
- **Quelle:** https://developers.google.com/android-publisher/quotas · https://developer.android.com/google/play/integrity/classic

### SV9. PLR-WebView-SSL-Finding `onReceivedSslError`
- **Symptom:** Sicherheits-Flag wegen `handler.proceed()` bei SSL-Fehler.
- **Ursache:** WebView (oft in SDK) winkt ungültige Zertifikate durch.
- **Versionen:** dauerhaft.
- **FIX:** `onReceivedSslError` → `handler.cancel()` bei ungültigem Zertifikat; betroffene Lib updaten.
- **Quelle:** https://support.google.com/faqs/answer/7071387

## MV) Metadaten / targetSdk / 16-KB — reale Vorfälle

### MV1. SQLCipher-Legacy bricht 16-KB-Pflicht ⭐ projektkritisch (falls genutzt)
- **Symptom:** Reject „Recompile your app with 16 KB native library alignment"; `libsqlcipher.so` u. a. nicht aligned.
- **Ursache:** Legacy `android-database-sqlcipher` (EOL 2023) nie für 16 KB neu kompiliert.
- **Versionen:** Deadline App-Updates **01.05.2026**; Fix in `sqlcipher-android` ≥ 4.6.1.
- **FIX:** Migration auf `net.zetetic:sqlcipher-android` (≥ 4.6.1, B4X-AAR 4.10.0) — voller DB-Crypto erhalten, KEIN Rewrite.
- **Quelle:** https://www.zetetic.net/blog/2025/06/26/sqlcipher-for-android-16kb-page-size-support/

### MV2. Weitere 16-KB-brechende Libs (Prüf-Matrix)
- **Symptom:** AAB-Upload-Block; konkrete `.so` gelistet.
- **Ursache:** 4-KB-Alignment in: PDFium (`libmodpdfium.so`), RTMP (`librtmp-jni.so`), React Native ≤0.75 (`react-android`/`reanimated`), Flutter <3.16 + `ffmpeg_kit`/`flutter-tflite`/Rive, TensorFlow Lite ≤2.12 (`libtensorflowlite_jni.so`).
- **Versionen:** je Lib; Fixes: RN 0.76/0.77, Flutter ≥3.16, TFLite >2.12, NDK r27+.
- **FIX:** Libs auf 16-KB-fähige Versionen; eigene `.so` mit `-Wl,-z,max-page-size=16384` (NDK r27+); vor Upload `check_elf_alignment.sh` über `/lib/arm64-v8a/*.so`.
- **Quelle:** https://github.com/facebook/react-native/issues/53649 · https://developer.android.com/guide/practices/page-sizes

### MV3. Metadaten-Reject-Loop (generische Begründung)
- **Symptom:** „Metadata policy violation" ohne konkrete Stelle → blindes Ändern → erneut abgelehnt.
- **Ursache:** Reject-Mail nennt nur die Kategorie, nicht das Token.
- **Versionen:** 2026.
- **FIX:** Title/Short/Full systematisch auditieren (ALL-CAPS nur Markenname, keine Fremdmarken/CTAs/„Top/Best/#1"); danach über Help die konkrete Stelle klären statt blind-resubmit.
- **Quelle:** https://support.google.com/googleplay/android-developer/thread/388556003/

### MV4. Metadaten-Verstöße eskalieren zu Account-Suspend
- **Symptom:** Wiederholte Rejects → Termination des Kontos + verknüpfter Konten.
- **Ursache:** „repeated app rejections or removals" triggern Suspension; an Person/Entität gebunden.
- **Versionen:** 2025/2026 (1,75 Mio Apps/80k Konten).
- **FIX:** Reject-Loops vermeiden (jeden Reject voll klären); bei Marken-Themen Doku vorab.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9899234

### MV5. Trademark/Impersonation („official"/Fremdmarke)
- **Symptom:** Reject „Unauthorized Use of Brand or Trademark"; auch trotz vorab eingereichter Doku.
- **Ursache:** Fremdmarke in Name/Description/Screenshots; „official"-Claim; Eigenerklärung statt Brand-Letter.
- **Versionen:** laufend.
- **FIX:** Generisch benennen („Sticker Maker for Messaging"); eigene Icons; bei echter Lizenz Autorisierung vom Rechteinhaber vorab unter App-Content.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9888072

### MV6. Store-Listing-Icon-Experiment geht nicht live
- **Symptom:** Gewinner-Variante „applied", Live-Listing übernimmt das Icon nicht.
- **Ursache:** Play-Console-Bug bei Experiment-Propagierung.
- **Versionen:** 2025 (intermittierend).
- **FIX:** Gewinner-Asset manuell ins Main-Listing hochladen; Experiment beenden, neu publishen.
- **Quelle:** https://support.google.com/googleplay/android-developer/thread/338550767/

### MV7. Screenshot-Rejects (Fremd-Geräterahmen / Tablet-Slot / Feature-Graphic-Alpha)
- **Symptom:** Listing geflaggt/nicht publizierbar.
- **Ursache:** iPhone-Rahmen im Android-Screenshot; Phone-Assets in Tablet-/TV-/Wear-Slot gestreckt; Feature-Graphic mit Alpha/falscher Größe.
- **Versionen:** laufend.
- **FIX:** Nur Android-Rahmen (Wear ohne); pro Form-Faktor eigene Assets (Ratio 1:2–2:1, Text ≤20 %); Feature-Graphic exakt 1024×500 ohne Alpha.
- **Quelle:** https://theapplaunchpad.com/blog/google-play-store-screenshot-requirements/

### MV8. IARC-Content-Rating-Diskrepanz → Override/Territorial-Suspend
- **Symptom:** Reject „misrepresenting an app's content"; während Appeal Territorium suspendiert.
- **Ursache:** Fragebogen-Antworten ≠ realer Inhalt/Audience; Rating-Authority überschreibt.
- **Versionen:** laufend.
- **FIX:** IARC exakt zum Inhalt beantworten; Target-Audience konsistent.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/9859655

### MV9. Capacitor/Ionic targetSdk-Warnung trotz neuem Build
- **Symptom:** „target API level (35) requirements"-Warnung trotz aktueller App.
- **Ursache:** Veraltete Capacitor-Plattform hält `targetSdkVersion` auf 34 in `variables.gradle`.
- **Versionen:** 2026.
- **FIX:** `targetSdkVersion=35` in `variables.gradle`, `npx cap sync`, Capacitor-Android updaten.
- **Quelle:** https://forum.ionicframework.com/t/your-app-is-affected-by-google-play-s-target-api-level-35-requirements/249266

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
| MV1 (SQLCipher 16-KB) | **Deadline App-Updates 01.05.2026** → `sqlcipher-android` ≥ 4.6.1 | zetetic 2025-06-26 |
| AV1 (Accessibility „autonom") | **verschärft seit 28.01.2026** | answer/16585319 |
| DV4 (UMP/TCF v2.3) | **Deadline 28.02.2026** | admob/android/privacy |
| TV4/T4 (12 statt 20 Tester) | geändert 11.12.2024 | answer/14151465 |
| TV7 (180-Tage-Appeal) | verbindlich seit 28.01.2026 | answer/16659089 |
| SV7 (Integrity hardware attestation) | Default seit Mai 2025 | androidauthority |
| AV7 (Developer-Verification) | Go-live März 2026, global 2027 | techcrunch 2025-08-25 |
| FV3 (mediaPlayback-Crash) | teils GEFIXT in Media3 1.6.0 | androidx/media#111 |
| FV6 (WorkManager-FGS-Overlap) | GEFIXT in WorkManager 2.10.5 | fgs/troubleshooting |
| RN/Flutter/TFLite 16-KB (MV2) | GEFIXT: RN 0.76/0.77, Flutter ≥3.16, TFLite >2.12, NDK r27+ | page-sizes |
| Übrige | per Design / dauerhafte Policy / Status unklar | jeweilige Quelle |

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
| TV1–TV9 (Testing-Vertiefung) | §1 (Release-Tracks & Testing-Pflicht) |
| RV1–RV15 (Rollout/Update-Vertiefung) | §2 (Rollout/versionCode), §3 (In-App Updates) |
| DV1–DV10 (Data-Safety-Vertiefung) | §4 (Data-Safety & Daten-Policies) |
| FV1–FV8 (Foreground-Service-Vertiefung) | §5 (Foreground-Service-Policy) |
| AV1–AV7 (Accessibility/Permissions-Vertiefung) | §6 (Accessibility & Berechtigungen) |
| SV1–SV9 (PLR/Signing/Integrity-Vertiefung) | §7 (Pre-Launch-Report), §8 (Signing/Integrity) |
| MV1–MV9 (Metadaten/16-KB-Vertiefung) | §9 (ASO/Store-Listing/targetSdk) |

---

## N) Nachtrag 2026-07-12 (Rechtssicherheits-Audit v8 BestJournal, 7-Researcher-Lauf)

### N1. Boot-Receiver darf ab Android 15 keine dataSync-/mediaProjection-FGS starten
- **Symptom:** App-Crash/ANR beim Geräte-Boot bzw. Play-Reject der FGS-Deklaration.
- **Ursache:** Ab targetSdk 35+ dürfen aus `BOOT_COMPLETED` bestimmte FGS-Typen (u.a. `dataSync`, `mediaProjection`) NICHT gestartet werden.
- **Versionen:** Android 15+, Stand 2026.
- **FIX:** Im Boot-Receiver nur Alarme/Notifications neu planen (BestJournal macht das korrekt); Sync in WorkManager.
- **Quelle:** https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start (offiziell, 12.07.2026)

### N2. Data-Safety-Checkliste aus Vorlage nennt SDKs, die nie eingebaut wurden
- **Symptom:** Data-Safety-Formular deklariert z.B. Crashlytics-„Crash logs", obwohl das SDK fehlt → „inaccurate declaration"-Enforcement (Google gleicht mit echtem Netzwerkverkehr ab; Vorfall BestJournal v8: Checkliste sagte Crashlytics, Gradle hat keins).
- **Ursache:** Checklisten werden aus Vorlagen/alten Audits kopiert statt aus dem Build generiert.
- **Versionen:** Play Console 2025/2026.
- **FIX:** Deklaration IMMER gegen `gradle/libs.versions.toml` + merged Manifest verifizieren, nie von Hand pflegen.
- **Quelle:** https://support.google.com/googleplay/android-developer/answer/10787469 (offiziell, 12.07.2026)

### N3. Billing Library v8 Pflicht ab 31.08.2026
- **Symptom:** Neue Einreichungen/Updates mit Billing < v8 werden ab 31.08.2026 abgelehnt (Verlängerung bis 01.11.2026 beantragbar).
- **Ursache:** Jährliche Billing-Library-Kadenz; BestJournal ist auf 7.1.1.
- **Versionen:** Play Billing 7.x → 8.x, Deadline 31.08.2026.
- **FIX:** Vor der Deadline auf `billing-ktx` 8.x migrieren (Breaking Changes im Migration-Guide prüfen).
- **Quelle:** https://developer.android.com/google/play/requirements/target-sdk + Billing-Release-Notes (offiziell, 12.07.2026)

### N4. DSA-Trader-Status: Bezahl-App verlangt veröffentlichbare Telefonnummer
- **Symptom:** EU-Sichtbarkeit/Updates blockiert, weil Trader-Deklaration unvollständig; private Telefonnummer würde im Store-Listing öffentlich.
- **Ursache:** Seit 17.02.2025 erzwingt Play den DSA-Trader-Status; Trader = Anschrift + Telefon + E-Mail öffentlich.
- **Versionen:** Play Console seit 02/2025, Stand 2026.
- **FIX:** Virtuelle Rufnummer (z.B. sipgate) oder Impressumservice-Zusatz VOR dem ersten Submit besorgen.
- **Quelle:** https://www.verasafe.com/blog/understanding-the-trader-classification-under-the-digital-services-act/ (extern, 12.07.2026)
