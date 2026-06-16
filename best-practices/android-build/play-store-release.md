# Play-Store-Release & Policy — Best Practices (Stand 2026-06-14)

> **Zweck:** Wie man BestJournal sauber bei Google Play veröffentlicht — Tracks, Rollout,
> Data-Safety, Policy-Deklarationen, Pre-Launch-Report, Signing, ASO. (AAB/Billing/R8 sind
> separat abgedeckt: `best-practices-gradle.md`, `best-practices-r8.md`.)
> **Versions-Anker:** Play Console / Play-Policy **Stand Juni 2026** · BestJournal targetSdk **35**,
> versionCode **144**, versionName **0.19.11**, AAB, R8-obfuskiert; nutzt `RECORD_AUDIO` (Sprach-/Vorlese),
> Google-Drive-Backup, LLM/TTS-APIs.
> **Gegenstück (Reject-Gründe/Fallen):** [`bugs/android-build/play-store-release.md`](../../bugs/android-build/play-store-release.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Neues Personal-Konto → Production | Closed Test mit **≥12 Testern, 14 zusammenhängende Tage**, dann „Apply for production" | §1 |
| 2 | Track-Reihenfolge | Internal (sofort, ≤100) → Closed → Open → Production; Bundle aus Library promoten | §1 |
| 3 | Update ausrollen | Staged Rollout 5–10 % → Vitals beobachten → hochziehen; **kein Rollback**, nur höherer versionCode | §2 |
| 4 | versionCode | Streng monoton +1 (144→145…), nie wiederverwenden/senken; max 2.100.000.000 | §2 |
| 5 | Kritischen Fix erzwingen | In-App-Updates: Priority 5 (nur per Publisher-API) + Immediate-Flow | §3 |
| 6 | App mit Konto | In-App- **UND Web-URL**-Kontolöschung anbieten (Data-Safety-Pflichtfeld) | §4 |
| 7 | Fotos anhängen | **Photo Picker** statt `READ_MEDIA_IMAGES/VIDEO` (kein Permission, kein Reject) | §4 |
| 8 | Data-Safety-Formular | Jedes SDK (LLM/TTS/Crashlytics) deklarieren; Formular ↔ Privacy-Policy ↔ Verhalten 3-fach abgleichen | §4 |
| 9 | Mikrofon/sensible Daten | **Prominent Disclosure** VOR Permission-Dialog; Tagebuch-PII nie roh in Logs/Crash/Backup | §4, §6 |
| 10 | Drive-Backup-Service | **KEIN `dataSync`-FGS** → Auto-Backup = WorkManager, „Jetzt sichern" = UIDT (sonst Reject + 6h-Falle) | §5 |
| 11 | Mikrofon-FGS | `microphone`-Typ deklarieren, RECORD_AUDIO runtime, sichtbare Notification, Demo-Video | §5 |
| 12 | Vorlesen/Auslösen | AccessibilityService **vermeiden** (TTS-API/Quick-Tile/ACTION_ASSIST); sonst Non-Tool-Declaration | §6 |
| 13 | Pre-Launch-Report | Test-Account hinterlegen (kein Produktiv-Konto); Crashes/Accessibility-Warnungen fixen | §7 |
| 14 | App-Signing | Play App Signing (Google-Key); App-Signing-SHA bei externen APIs eintragen; Upload-Key-Backup (PEM) | §8 |
| 15 | Crash-Stacktraces | AAB + AGP 4.1+ → `mapping.txt` automatisch im Bundle; SafetyNet → **Play Integrity** | §8 |
| 16 | Store-Listing | Titel ≤30, Kurz ≤80, Voll ≤4000; kein „#1/Best/Free", keine Emojis/Keyword-Listen (alle Sprachen) | §9 |
| 17 | targetSdk/16 KB | targetSdk 35 = konform; 16-KB-Page-Size bei nativem Code im Bundle-Explorer prüfen | §9 |

---

## 1) Release-Tracks & Testing-Pflicht

`offiziell`

- **Vier Tracks:** **Internal** (≤100 Tester, AAB in Minuten, keine Voraussetzungen) → **Closed**
  (≤2.000/Liste, Pflicht-Track für neue Personal-Konten) → **Open** (öffentlich, Limit ≥1.000) →
  **Production**. Nutzer bekommen den höchsten Versionscode des Tracks, für den sie berechtigt sind.
- **Testing-Pflicht für NEUE Personal-Konten** (erstellt nach 13.11.2023): **≥12 Tester**, die
  **14 zusammenhängende Tage** durchgehend opted-in sind, dann „Apply for production" + 3-teiliger
  Fragebogen (Tester-Recruiting, App-Mehrwert, Production-Readiness), Review ~7 Tage. **Wichtig:**
  Die früher genannten „20 Tester" wurden am **11.12.2024 auf 12 reduziert**. Org-Konten und ältere
  Personal-Konten sind ausgenommen.
- **Praxis:** 15–20 Tester recruiten (Puffer — ein Dropout setzt die 14-Tage-Uhr zurück); alle am
  selben Tag opt-in lassen; Feedback dokumentieren (wird im Fragebogen abgefragt).
- **Internal App Sharing** (separater Mechanismus): sofortige AAB/APK-Links für Blitz-Tests (beliebige
  Signatur, debuggable, versionCode egal, 100 Nutzer/60 Tage) — zählt aber NICHT für die 12/14-Pflicht.
- **Promotion:** getestetes Bundle „Add from library" wiederverwenden statt neu hochladen.
- **Konto-Setup früh:** Geräte-Verifizierung (echtes Android-Gerät via Console-App), Identitäts-/
  D-U-N-S-Verifizierung — blockiert sonst die erste Veröffentlichung. Ab Sept 2026 erweiterte
  Entwickler-Identitätsverifizierung.
- **Quellen:** https://support.google.com/googleplay/android-developer/answer/14151465 · …/9845334 · …/9844679

## 2) Staged/Managed Rollout & versionCode-Disziplin

`offiziell`

- **Staged Rollout (nur Updates):** klein starten (5–10 %), 24–48 h Android Vitals + Reviews
  beobachten, dann verdoppeln bis 100 %. Erhöhen ist **manuell**. Bei Problem `Manage rollout → Halt`
  (stoppt weitere Verteilung), nach Fix `Resume`. **NEU 2026:** Auch eine 100 %-Version lässt sich
  anhalten → die vorherige gute Version wird wieder ausgeliefert (nicht für bereits-geupdatete Nutzer).
- **KEIN Rollback:** Eine kaputte Version „zurückrollen" geht nicht (Android-Downgrade-Schutz). Halt
  verhindert nur weitere Ausbreitung; Fix nur per **neuem, höherem versionCode**.
- **versionCode:** streng monoton steigend, eindeutig pro Upload (nie wiederverwenden, auch nicht nach
  Löschen), max **2.100.000.000**. Lücken (144→150) sind erlaubt, Senken/Wiederholen nie. Bei AAB
  nur EIN versionCode (Play generiert Splits selbst). Empfehlung: bei monotonem **+1** bleiben.
- **versionName:** SemVer `MAJOR.MINOR.PATCH` (dein `0.19.11` passt); pro Release anheben.
- **Android Vitals Schwellen (Sichtbarkeit!):** user-perceived Crash **< 1,09 %**, ANR **< 0,47 %**,
  per-Device **< 8 %** — Überschreitung kostet Discovery/Ranking. Während jedes Rollouts live prüfen.
- **Managed Publishing:** genehmigte Änderungen sammeln und zum Wunschzeitpunkt freigeben. **Achtung:**
  hält NICHT zurück: Rollout auf 100 % erhöhen, Release-Notes-Updates, Tester-Listen, Preise. Keine
  Änderung nachschieben, während etwas in Review ist (verlängert die Review-SLA).
- **Quellen:** https://support.google.com/googleplay/android-developer/answer/6346149 · …/16285429 · https://developer.android.com/studio/publish/versioning · https://developer.android.com/topic/performance/vitals

## 3) In-App Updates (Sicherheitsnetz gegen kaputte Versionen)

`offiziell`

- **Flexible** (Hintergrund-Download, App nutzbar) für Features; **Immediate** (blockierend) für
  kritische Fixes. Android 5.0+, kein .obb.
- **Update-Priority 0–5** nur über die **Publisher-API** (`edits.tracks.releases.inAppUpdatePriority`,
  z. B. Fastlane `supply`/CI) setzbar — NICHT in der Console-UI; zum Release-Zeitpunkt setzen,
  nachträglich nicht änderbar. Priority 5 + Immediate-Flow, um Nutzer von kaputten Versionen wegzubekommen.
- `clientVersionStalenessDays()` für „erst nach N Tagen erinnern".
- **Quelle:** https://developer.android.com/guide/playcore/in-app-updates

## 4) Data-Safety & Daten-Policies (kritisch für eine Tagebuch-App)

`offiziell`

- **Data-Safety-Formular** Pflicht (auch „keine Daten"). „Collect" = Daten verlassen das Gerät —
  **gilt auch für SDKs** (LLM/TTS-API-Calls mit Tagebuchtext/Audio, Crashlytics, Analytics). Jedes
  SDK einzeln prüfen (Play SDK Index); Formular ↔ Privacy-Policy ↔ echtes Verhalten dreifach abgleichen
  (Mismatch = Reject Nr. 1).
- **Account-Deletion (falls eigenes Login):** In-App-Pfad **UND** eigenständige **Web-URL** (funktioniert
  ohne App-Neuinstallation) im Data-Safety-Feld. Daten wirklich löschen, nicht nur deaktivieren. Wenn
  Backup nur über das **Google-Konto** des Nutzers läuft (kein eigenes App-Konto), greift die Pflicht ggf. nicht.
- **Privacy-Policy:** öffentliche, nicht-editierbare URL (**kein PDF**, nicht geo-gesperrt), mit
  Retention/Deletion-Abschnitt, Entwicklername genannt; Link in App UND Console.
- **Fotos:** **Android Photo Picker** statt `READ_MEDIA_IMAGES/VIDEO` (seit 28.05.2025 erzwungen; ein
  Tagebuch ist kein Galerie-Kernzweck). Merged Manifest prüfen, dass kein SDK die Permission reinzieht.
- **Prominent Disclosure & Consent** für sensible Daten (Mikrofon, Tagebuch-PII-Upload): eigener
  In-App-Dialog VOR dem Runtime-Permission-Request, im Hauptflow, affirmatives „Zustimmen" + Decline,
  nicht nur in Privacy-Policy. **Tagebuch-PII niemals roh in Logs/Crashlytics/Analytics** (redacten —
  deckt sich mit der Observability-Regel „keine PII ins Log").
- **„Ephemeral":** LLM-/TTS-Call gilt nur dann als ephemeral, wenn der Anbieter Inputs nicht
  speichert/loggt/zum Training nutzt — sonst regulär als Collection/Sharing deklarieren.
- **Health/Mental:** Mood-/„mental well-being"-Framing macht Einträge zu sensiblen Health-Daten
  (Pflichten zu Disclosure/sicherer Handhabung); das formelle Health-Apps-Declaration-Form ist primär
  an Health-Connect/Health-Permissions geknüpft — ein reines Text-Tagebuch ohne diese braucht es i. d. R. nicht.
- **Zielgruppe** nicht versehentlich „Everyone (children)" → Families-Policy verschärft alles.
- **Transit:** alles HTTPS/TLS; „encryption in transit" + „deletion request mechanism" im Formular ankreuzen.
- **Quellen:** https://support.google.com/googleplay/android-developer/answer/10787469 · …/10144311 · …/13327111 · …/14115180 · https://developer.android.com/training/data-storage/shared/photopicker

## 5) Foreground-Service-Policy & Deklaration

`offiziell`

- **Console-Deklaration** (App content → Foreground service permissions) Pflicht ab targetSdk 34+:
  pro Typ Beschreibung, System-deferred/interrupted-Impact und **Demo-Video** der user-initiierten
  Auslösung. Genehmigt nur, wenn user-initiiert/-perceptible, vom Nutzer stoppbar, Kern-Feature,
  nur so lange wie nötig.
- **Drive-Backup ≠ `dataSync`-FGS:** Google will für Netzwerk-Transfer **UIDT** (user-initiiert,
  „Jetzt sichern") bzw. **WorkManager** (auto/periodisch). Ein `dataSync`-FGS fürs Backup ist
  Reject-Kandidat UND fällt unter das **6h/24h-Limit** (Android 15, `onTimeout()` → `stopSelf()` sonst
  ANR). → Backup auf WorkManager/UIDT umstellen, dann entfällt die `dataSync`-Deklaration komplett.
- **Mikrofon-FGS:** `microphone`-Typ behalten — Manifest `foregroundServiceType="microphone"` +
  `FOREGROUND_SERVICE_MICROPHONE` + `RECORD_AUDIO` (runtime), sichtbare Notification, user-initiiert,
  kein Start aus `BOOT_COMPLETED`, kein Background-Listening. Gutes Demo-Video.
- **`specialUse`** vermeiden (überdurchschnittlich oft abgelehnt); microphone + UIDT/WorkManager decken alles ab.
- **Merged Manifest** prüfen, dass kein SDK ungewollte `FOREGROUND_SERVICE_*`-Permissions reinzieht.
- **Quellen:** https://support.google.com/googleplay/android-developer/answer/13392821 · …/16559646 · https://developer.android.com/develop/background-work/background-tasks/data-transfer-options · https://developer.android.com/develop/background-work/background-tasks/uidt

## 6) Accessibility-Service & sensible Berechtigungen

`offiziell`

- **AccessibilityService möglichst ganz vermeiden:** Vorlesen über **`TextToSpeech`-API** (keine
  Sonderberechtigung), Auslösen über App-Button/**Quick-Settings-Tile**/`ACTION_ASSIST`. Policy
  verlangt explizit, schmaler gefasste APIs statt der Accessibility-API zu nutzen, wo möglich.
- **Falls unvermeidbar:** App ist KEIN Accessibility-Tool → **`isAccessibilityTool` NICHT setzen**,
  Non-Tool-Declaration ausfüllen + eigenständige In-App-Prominent-Disclosure + Demo-Video; Zweck im
  Store-Listing dokumentieren. **Seit Okt 2025 verboten:** autonomes Initiieren/Planen/Ausführen von
  Aktionen — nur statische, regelbasierte Automatisierung erlaubt. **Nie** RECORD_AUDIO über Accessibility.
- **Minimal-Permissions:** nur was die im Listing beworbenen Features brauchen, inkrementell anfordern.
  Für ein Tagebuch **nicht deklarieren:** `QUERY_ALL_PACKAGES` (stattdessen gezieltes `<queries>`),
  `MANAGE_EXTERNAL_STORAGE` (App-Storage/MediaStore/SAF), `SMS`/`Call Log`, Background-Location,
  `PACKAGE_USAGE_STATS`. Jede dieser sensiblen Permissions braucht sonst eine Declaration-Form +
  Approval — fehlt sie, wird die App entfernt.
- **Mikrofon (Spyware-Policy):** nur im Vordergrund, user-initiiert; legitime laufende Aufnahme nur
  per `microphone`-FGS + Notification + Disclosure. Kein „Always-listening" im Hintergrund.
- **Play Policy Insights** in Android Studio aktivieren (Echtzeit-Policy-Warnungen beim Coden).
- **Quellen:** https://support.google.com/googleplay/android-developer/answer/10964491 · …/16558241 · …/11150561 · …/10158779 · …/14745000

## 7) Pre-Launch-Report (PLR)

`offiziell`

- Läuft nach AAB-Upload auf echten Geräten (Firebase Test Lab): Crashes/ANRs, Android-Kompatibilität,
  Performance, **Accessibility** (Labels/Touch-Target/Kontrast), Screenshots. Vor Production die
  Errors/Warnings beheben.
- **Test-Account hinterlegen** (Settings → Pre-launch report) wenn Login vorhanden — **nie Produktiv-Konto**;
  Auto-Login klappt nicht bei WebView/OpenGL → Robo-Script aufnehmen. „Sign-in with Google" loggt der
  Crawler selbst ein.
- Kein Geo-/Root-Block im Test-Build (sonst „Test failed"). PLR ersetzt KEINEN echten Test-Track
  (IAP/Käufe testet er nicht) → vorher internen/geschlossenen Track laufen lassen.
- **Quellen:** https://support.google.com/googleplay/android-developer/answer/9842757 · …/9844487

## 8) App Signing, Mapping & Play Integrity

`offiziell`

- **Play App Signing** nutzen: Upload-Key (du) signiert das AAB, Google hält den App-Signing-Key
  (4096-bit). **Upload-Key-Verlust ist heilbar** (Reset per PEM), App-Signing-Key bei Google ist
  geschützt. Upload-Key sicher + PEM-Backup aufbewahren.
- **Externe APIs mit dem App-Signing-Key-Fingerprint** registrieren (Maps/OAuth/Drive/App-Links),
  nicht mit dem Upload-Key — sonst funktioniert der Store-Build nicht, der lokale schon. SHA-1/256
  aus Play Console → App signing.
- **Mapping (R8):** AAB + AGP 4.1+ nimmt `mapping.txt` automatisch ins Bundle → Crash-Stacktraces in
  Vitals werden deobfuskiert (kein manuelles Vergessen). Native Symbols via `ndk.debugSymbolLevel='FULL'`.
- **Play Integrity** (SafetyNet ist seit Jan 2025 abgeschaltet): `appRecognitionVerdict=PLAY_RECOGNIZED`
  + `appLicensingVerdict=LICENSED` als Baseline, Device-Tiers gestuft (kein Hard-Block nur auf STRONG →
  echte Nutzer aussperren). Standard-Requests on-demand, nicht cachen. Quota 10.000/Tag.
- **Quellen:** https://support.google.com/googleplay/android-developer/answer/9842756 · …/9848633 · https://developer.android.com/google/play/integrity/overview

## 9) ASO, Store-Listing & targetSdk/16-KB-Pflicht

`offiziell` (Policy/Specs) · `extern` (ASO-Heuristik)

- **targetSdk-Pflicht (present-day):** seit 31.08.2025 müssen neue Apps + alle Updates **API 35+**
  targeten. **BestJournal (35) = konform.** Regel rollt jährlich weiter → targetSdk im Jahresrhythmus mitziehen.
- **16-KB-Page-Size (seit 01.11.2025, greift bei Android-15-Targeting):** Apps ohne nativen Code sind
  automatisch konform; mit nativem Code (z. B. SQLCipher) im **App-Bundle-Explorer** Compliance prüfen.
- **Listing-Limits:** Titel **30**, Kurzbeschreibung **80**, Vollbeschreibung **4000** Zeichen;
  Icon 512×512 PNG, Feature-Graphic 1024×500 (ohne Alpha), **≥2 Screenshots** (besser ≥4 + Large-Screen
  16:9/9:16 für Promo-Sichtbarkeit), optionales YouTube-Video.
- **Metadaten-Policy (gilt in ALLEN Sprachen):** keine Emojis/Sonderzeichen-Spam, kein ALL-CAPS, kein
  „#1/Best/Top/Award", keine Preise/„Free/Sale", kein Keyword-Stuffing (bringt eh kein Ranking), keine
  Wettbewerber-Namen, keine CTAs („Download now") in Kurzbeschreibung/Screenshots, keine irreführenden Icon-Symbole.
- **ASO-Hebel (`extern`):** Haupt-Keyword in den Titel (stärkster Faktor), Lokalisierung, frische
  Reviews + Developer-Antworten (Ranking-Signal), quartalsweise Listing-Pflege; 2026 zählen
  Retention/Conversion als First-Class-Signale.
- **Store-Listing-Experiments:** A/B-Test für Icon/Graphic/Screenshots/Beschreibung; 2026 AI-gestützte
  Lokalisierung (Gemini-Vorbefüllung, Review durch Entwickler).
- **Quellen:** https://developer.android.com/google/play/requirements/target-sdk · https://developer.android.com/guide/practices/page-sizes · https://support.google.com/googleplay/android-developer/answer/9866151 · …/9898842

---

## 🔗 Bezug zum Bug-Almanach (Kopplung)

| Best-Practice-Abschnitt | Reject-Almanach-Abschnitt (`bugs/android-build/play-store-release.md`) |
|-------------------------|------------------------------------------------------------------------|
| §1 (Tracks/Testing) | T1–T8 (Production-Block/Tester zählen/Internal-Sharing/Konto-Verifizierung) |
| §2 (Rollout/versionCode) | R1–R7 (kein Rollback/Auto-Drosselung/versionCode/Managed-Publishing) |
| §3 (In-App Updates) | R8 (Priority nur per API) |
| §4 (Data-Safety) | D1–D9 (SDK-Deklaration/Mismatch/Account-Deletion/Photo-Picker/Privacy-Policy/Disclosure/Health) |
| §5 (Foreground-Service) | F1–F6 (dataSync-Reject/6h-Limit/Demo-Video/SDK-Permission/microphone) |
| §6 (Accessibility/Permissions) | A1–A9 (isAccessibilityTool/autonome Aktionen/QUERY_ALL_PACKAGES/MANAGE_EXTERNAL_STORAGE/Spyware) |
| §7 (Pre-Launch-Report) | P1–P5 (Sign-in/obfuskiert/Test-failed/Geo) |
| §8 (Signing/Integrity) | S1–S9 (wrong-key/API-Fingerprint/Mapping/SafetyNet/Integrity-Tiers) |
| §9 (ASO/Listing/targetSdk) | M1–M11 (targetSdk/16KB/Metadaten-Verstöße/Asset-Specs) |

> **Checkpoint:** Vollständig recherchiert (7 Researcher, offizielle Play-Quellen, Juni 2026).
> Zwei projektkritische Funde: Drive-Backup raus aus `dataSync`-FGS (→ WorkManager/UIDT) und
> AccessibilityService vermeiden (→ TTS-API/Quick-Tile). targetSdk 35 ist konform.
