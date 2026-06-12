# Designentscheidungen

Pro Eintrag: was entschieden wurde, warum, und wo es in der Spec abweicht.

## 1. Single-Module statt Multi-Module

**Spec §2:** „Single-Module-Projekt mit Package-Trennung — schneller im Build als Multi-Module".
**Umsetzung:** Genau so. Ein `:app`-Modul mit Packages für `data`, `domain`, `presentation`, `di`.

## 2. Kotlin-Version + AGP

**Entschieden:** Kotlin 2.1.0, AGP 8.7.3, Compose BOM 2025.01.01, Hilt 2.55, Room 2.7.0, Java 17.
**Begründung:** Identisch zu Frank's bestehender BestJournalAndroid-App — proven-stable und gut getestet. Die android-development.md-Rules empfehlen neuere Versionen (AGP 9.1, Kotlin 2.3, Compose BOM 2026.03), aber für ein neues Projekt im selben Repo ist Konsistenz wichtiger als Bleeding-Edge. Migration auf neuere Versionen kann später erfolgen.

## 3. EncryptedSharedPreferences trotz „deprecated"-Hinweis

**Spec §2 + §6.1 + §20:** „EncryptedSharedPreferences mit MasterKey (AES256_GCM)".
**Aktuelle Rules:** „EncryptedSharedPreferences deprecated → DataStore + Tink".
**Entschieden:** Spec gewinnt. Begründung:
- `androidx.security.security-crypto:1.1.0-alpha06` ist weiterhin gepflegt und in Production aktiv.
- DataStore + Tink würde eine eigene Krypto-Schicht erfordern — mehr Code, mehr Risiko.
- Migration ist trivial möglich, falls die Bibliothek wirklich entfernt wird.

## 4. EntropyEntries direkt in der UI (kein Domain-Mapping)

**Architekturhinweis:** Clean Architecture rät zu separaten Domain-Models.
**Entschieden:** UI nutzt `EntropyEntryEntity` direkt.
**Begründung:** Solo-Projekt, niedrige Komplexität, identische Felder. Mapping wäre 1:1 und reine Bürokratie. Falls später ein Feld nur intern persistiert werden soll (z. B. ein interner Score-Cache), führe ich Mapping ein. Für Stufe 1+2 reicht das Direktnutzen.

## 5. Bottom-Nav mit FAB-Mic statt klassischem NavigationBar

**Referenzbilder 11/15/21/25:** Mic-Button mittig, ragt über die Bar hinaus.
**Umsetzung:** Custom `CosmosBottomBar` mit 4 Tab-Slots + zentralem `MicButton`-FAB. Material3 `NavigationBar` würde diesen Mic-FAB nicht zentral rendern.

## 6. Mic-Aufnahme: einfacher Lifecycle in Stufe 1

**Spec §8.1:** Tap-Tap UND Long-Press.
**Stufe 1:** Nur Tap-Tap (erste Tippen startet, zweites stoppt). Long-Press kommt mit Stufe 2 zusammen mit Haptic Feedback und Wave-Visualizer (`MediaRecorder.maxAmplitude`-Sampling).

## 7. `ProcessEntryUseCase` mit suspend-sicherem try/catch

**Bug fix:** Erste Version benutzte `runCatching {}.onFailure {}`. `onFailure`-Lambda ist nicht `suspend`, kann also keine suspend-Funktion (`entries.upsert`) aufrufen. Lösung: explizites `try { ... } catch { ... }` direkt im suspend-Body. Ein Eintrag wird IMMER persistiert — auch bei API-Fehler — damit der Nutzer den Eintrag nicht verliert (Spec §19).

## 8. „Aus Profil ins Gedächtnis übernehmen": Heuristik in Stufe 1

**Spec §6.5:** „Triggert eine KI-Operation, die den Profiltext in 5–15 prägnante Memory-Einträge destilliert."
**Stufe 1:** Einfache Heuristik — Absatzgrenzen erkennen, max. 8 Einträge pro Aufruf, jeden Absatz mit 20–400 Zeichen Länge übernehmen.
**Stufe 4:** Voller Gemini-Aufruf mit System-Prompt-Architektur.

## 9. Default-Prompts beim ersten Start

**Spec §6.4:** Drei Default-Prompts vorinstalliert (Status: aktiv).
**Umsetzung:** `BootstrapViewModel` in `MainActivity` prüft beim Start `prompts.count() == 0` und installiert dann die drei Defaults aus der Spec wortwörtlich.

## 10. „KI-Vorschläge prüfen" / „Aus Profil neu generieren" — Stubs in Settings

Diese Buttons sind aktuell nicht-funktional auf Memory-Screen — sie folgen mit Stufe 4 (KI-Vorschlagsmechanik im Wissenschaftler-Modus). Der „Manuell hinzufügen"-Button ist voll funktional.

## 11. KI-Modell-Auswahl Stufe 1 nur Radio-Buttons, nicht Dynamic-Listing

**Spec §6.2:** TTS-Voice-Liste wird beim ersten Verbinden mit Google-TTS-API abgerufen.
**Stufe 1:** Statische Listen (3 Whisper, 3 Gemini, 2 Sprachen). TTS-Listing kommt mit Stufe 4.

## 12. Anti-Tampering / Backup-Rules

`android:allowBackup="false"` + explizite `data_extraction_rules` und `backup_rules` schließen `encrypted_secrets.xml` von Cloud-Backup und Device-Transfer aus.

## 13. Schichtcode-Parser

Spec §15.5 definiert das Regex-Muster für Tag/Nacht-Schicht-Codes. Implementierung folgt mit Stufe 2 zusammen mit Google-Calendar-OAuth.

## 14. Status-Balken in Stufe 1

**Spec §4.1:** Drei gewichtete Komponenten — Biomarker (40 %), Aufgaben-Reduktion (35 %), Kontext-Stimmigkeit (25 %).
**Stufe 1:** Nur Aufgaben-Reduktion (Gewicht 100 %). Berechnung: `100 - (sumSeverity * 100 / (count * 10))`. Sehr einfaches Modell — wird mit Stufe 2 erweitert.

## 15. AppAuth, Vico, ExoPlayer schon im Build, aber nicht aktiviert

Die Bibliotheken sind in `libs.versions.toml` und `app/build.gradle.kts` schon eingebunden. Sie sind ungenutzt in Stufe 1, aber dadurch ist der Build bereit für Stufen 2-4 ohne weitere Gradle-Sync-Runde.

## 16. Theme-Switch via System-Default

`isSystemInDarkTheme()` als Standard. Manueller Override per `AppSettings.themeIsDarkFlow` ist vorbereitet, aber kein UI-Toggle in Stufe 1. Spec verlangt nur „Hell und Dunkel-Modus", erfüllt durch System-Default-Tracking.

## 17. Mic-Button-FAB-Größe

**Spec §4.2:** „Größe: 96dp auf Dashboard 1".
**Umsetzung:** 64dp im FAB-Slot der Bottom-Bar — größer würde die Bar zu hoch machen. Auf reinen Aufnahme-Screens (Stufe 4 — Voice-Overlay) bekommt er 96dp.

## 18. Repository-Pattern minimal in Stufe 1

Nur 3 Repositories (Entry, Prompt, Memory) — die anderen Entitäten haben aktuell nur DAOs und werden direkt von ViewModels via Hilt injiziert (z. B. zukünftig `BiomarkerSnapshotDao` im `BiomarkerScreen`-VM von Stufe 2). Das spart Code, da kein Repository nur ein DAO durchreichen würde.

## 19. ProGuard im Debug-Build

Debug-Build ist nicht minified, Release-Build hat `isMinifyEnabled = true` + `isShrinkResources = true` mit ProGuard-Rules für Kotlinx-Serialization, Retrofit, OkHttp, Room und Hilt.

## 20a. Drive-Backup als JSON statt DB-File

**Spec:** orientiert an BestJournalAndroid, dort wird die ganze SQLite-DB gesichert.
**Entschieden:** EntropieReductor sichert nur die EntropyEntries als JSON-Datei.
**Begruendung:**
- Kompakter (typisch <100 KB statt mehrere MB)
- Transparenter Format (JSON ist debugbar, DB-File ist binaer)
- Einfacheres Mergen ueber updatedAt — kein WAL-Checkpoint, keine Schema-Versionierung-Probleme
- Memory + API-Keys + Profil bleiben absichtlich lokal — Datenschutz

## 20b. Coalescing statt Queueing

**Problem:** Wenn der User in 5 Sekunden 10 Eintraege bearbeitet, sollen nicht 10 Drive-Uploads gestapelt werden.
**Loesung:** SyncCoordinator mit Mutex (nie zwei Uploads gleichzeitig) + 1500ms-Debounce-Window. Pending-Job wird durch neuen Trigger ersetzt; ein laufender Upload wird NICHT abgebrochen, statt dessen merken wir dirtyDuringUpload und feuern danach noch einen Run. So konvergiert das System bei beliebig vielen schnellen Aenderungen zu maximal 2 Uploads (einer mit Stand X, einer mit Stand X+N).

## 20c. GoogleSignInClient (legacy) statt CredentialManager

**Aktuelle Empfehlung:** CredentialManager (seit 2024).
**Entschieden:** GoogleSignInClient — funktioniert ohne Web-Client-ID-Konfiguration und unterstuetzt direkt das `requestScopes(DRIVE_APPDATA)`-Pattern, das wir fuer `GoogleAuthUtil.getToken` brauchen. Ein Wechsel auf CredentialManager wuerde zusaetzlichen JWT-Roundtrip benoetigen, ohne funktionalen Mehrwert in Stage 1. Migration moeglich in Stage 4.

## 20d. Theme-Toggle: SYSTEM/LIGHT/DARK statt nur Boolean

**Spec:** "Hell- und Dunkel-Modus".
**Entschieden:** Drei Zustaende — SYSTEM, LIGHT, DARK — wie BestJournalAndroid.
**Begruendung:** Reine Bool waere unschoen weil "Auto"-Mode (= System folgen) der Default sein soll. ThemeMode-Enum ist explizit, persistiert via DataStore (nicht EncryptedPrefs — kein Geheimnis), zykelt im Toggle.

## 20e. Repository-Hooks via Lazy<SyncCoordinator>

**Problem:** EntryRepository → SyncCoordinator → EntryRepository ist ein Hilt-Zyklus.
**Loesung:** dagger.Lazy<SyncCoordinator> im EntryRepository, get() wird erst beim ersten Aufruf aufgeloest. Damit kein Init-Cycle. Pattern aus dem Hilt-Doku-Pattern fuer zyklische Module.

## 20f. Auto-Restore exakt einmal pro Process

**Problem:** Bei Theme-Toggle wird die Activity recreated, MainActivity.onCreate laeuft erneut. Wenn StartupViewModel in init {} ein Restore startet, laeuft das doppelt.
**Loesung:** `@Volatile var startupRanThisProcess` als Companion-Object-Flag. Wird beim Process-Start auf false initialisiert (statisch), nach dem ersten Restore-Versuch auf true. Damit triggert auch ein Activity-Recreate keinen weiteren Restore.

## 21. Audio-Format AAC in M4A statt OGG

**Spec §2:** „M4A/AAC, mono, 16 kHz".
**Umsetzung:** Genau so. Groq Whisper akzeptiert M4A, Datei wird sofort nach erfolgreicher Transkription gelöscht. Wenn ich das später auf OGG umstellen wollen sollte (kleinere Dateien), wäre das 1 Zeile Änderung in `AudioRecorder`.

## 22. AppAuth fuer Whoop UND Google Calendar (Stufe 2)

**Spec §15.4 + §15.5:** "OAuth-Flow analog zu Whoop, mit Google-Sign-In".
**Entschieden:** Beide Endpunkte ueber AppAuth (OpenID-Foundation-Library). Google-Sign-In aus Stufe 1 bleibt ausschliesslich fuer Drive-Backup, Calendar nutzt einen separaten AppAuth-Flow mit dem `calendar.readonly`-Scope.
**Begruendung:**
- AppAuth liefert echte Refresh-Tokens fuer Background-Sync — Google-Sign-In ist nur fuer kurzlebige Access-Tokens praktisch.
- Whoop ist nicht Google — AppAuth ist die einzige sinnvolle Option.
- Ein Persistenz-Pfad (`AuthState.jsonSerializeString` in EncryptedSharedPreferences) deckt beide Endpunkte ab.
- Frank kann zwei verschiedene Google-Konten verwenden (Drive vs. Calendar) wenn er will.

## 23. Schichtcode-Parser als deterministischer Domain-Code

**Spec §15.5:** Regex-basiert, "Tag 1-4" + optionaler X/F-Marker, "Nacht 1-4", "U/Urlaub".
**Umsetzung:** Pure-Kotlin-Object `ShiftCodeParser` mit zwei Regexes. Inklusive 18 Unit-Tests die alle Spec-Beispiele plus Edge-Cases (Whitespace, Lowercase, Tag5 = unbekannt) abdecken.
**Begruendung:** Deterministische Logik gehoert in die Domain, nicht ins Repository. Tests koennen ohne Hilt + Room laufen.

## 24. CalendarRepository: leere Tage werden mit UNBEKANNT initialisiert

**Spec §15.5:** Nicht explizit gefordert.
**Entschieden:** Vor dem Auswerten der Events legen wir alle Tage im Sync-Fenster (-30 bis +30) als UNBEKANNT an. Erst danach ueberschreiben echte Schicht-Events sie. Tage ohne Schichteintrag bleiben UNBEKANNT mit Default-Schlaffenster 22:00-06:00.
**Begruendung:** Damit hat der CalculateBucketsUseCase und der StatusObserver immer einen CalendarDay zur Verfuegung — auch fuer Wochen ohne Schichtdienst-Events. Vermeidet Null-Pruefungen ueberall im Code.

## 25. Whoop-Pagination mit Exponential-Backoff bei 429

**Spec §15.4:** "Mit Retry-Logik und Exponential Backoff bei 429".
**Umsetzung:** Generischer `paged()`-Helper im WhoopRepository. Bei HTTP 429 wird mit 1s, 2s, 4s gewartet (max 3 Retries). Andere Fehler werfen sofort.
**Begruendung:** Spezifikation 1:1 erfuellt. Whoop's Rate-Limit von ~100 Req/Min wird mit dem 30-Tage-Sync (typisch 30-90 Records pro Endpoint) selten erreicht — der Backoff ist Defensive-Coding.

## 26. BiomarkerSnapshot-ID = "cycle-{whoop_cycle_id}"

**Spec §5.1:** `BiomarkerSnapshot.id: String` — Format nicht festgelegt.
**Entschieden:** ID ist `"cycle-${whoopCycle.id}"`, was bei Re-Sync exakt denselben Eintrag ueberschreibt (UPSERT auf Primary Key).
**Begruendung:** Idempotente Sync-Operation — kein Duplikate-Problem bei mehrfachem Sync, keine Ueberschneidungen mit zukuenftigen Snapshots aus anderen Quellen (z.B. Manual-Logging).

## 27. CalculateStatusUseCase: Re-Skalierung wenn Whoop fehlt

**Spec §4.1:** 40/35/25-Gewichtung.
**Entschieden:** Wenn `BiomarkerSnapshot == null`, faellt die 40-%-Komponente weg, und die verbleibenden Komponenten werden mit 60/40-Gewichtung re-skaliert (Aufgaben 60, Kontext 40).
**Begruendung:** Der Status-Balken sieht sonst bei Stufe-1-Nutzern (kein Whoop verbunden) immer kaputt-niedrig aus. Re-Skalierung ist die saubere Loesung — das `breakdown.biomarkerScore == null` zeigt im Detail-Sheet "Whoop nicht verbunden" an.

## 28. KiQuestion-Cache als DataStore (nicht Room)

**Spec §10.4:** Nicht festgelegt wo die aktuelle Frage liegt.
**Entschieden:** Eigener Preferences-DataStore "ki_question_state" mit den Feldern triggerKey, text, rationale, related_ids und dismissed_until_ms.
**Begruendung:** Nur eine Frage zur Zeit — kein Tabellen-Design noetig. DataStore ist 50 Zeilen einfacher als ein Room-DAO. Die Frage-Historie ist nicht relevant; nur die aktuelle.

## 29. ShiftAwareNotifier: Stufe-2-MVP verwirft im Schlaf statt zu verzoegern

**Spec §16.4:** "Notification verzoegern bis zum Wachzeitpunkt + 15 Min".
**Entschieden:** In Stufe 2 (MVP) verwerfen wir Notifications die im Schlaffenster auftreten, statt sie zu verzoegern. Stufe 4 baut die WorkManager-Verzoegerung mit Deep-Link auf einen DelayedNotificationWorker auf.
**Begruendung:** WorkManager-setInitialDelay mit Notification-Payload braucht einen separaten Worker, der die Notification dann postet — das ist in Stufe 2 zu viel Infrastruktur. MVP-Verhalten "im Schlaf nicht stoeren" reicht voellig fuer den ersten Beta-Test.

## 30. KiQuestionWorker — periodisch alle 30 Minuten

**Spec §10.4:** "alle 30 Min."
**Umsetzung:** PeriodicWorkRequest mit 30 Min Intervall. Bei jedem Lauf wird der Generator gestartet, der die fuenf Trigger sequentiell prueft. Bei Treffer wird die Frage in den Cache geschrieben und eine schichtbewusste Notification ausgeloest (nur wenn neuer triggerKey).
**Begruendung:** WorkManager-Periodic-Tasks haben minimal 15min, also ist 30min sicher. Die UI beobachtet den Repository-Flow und zeigt die Frage sofort beim Cache-Update.

## 31. Stufe 4 — TTS: MediaPlayer statt ExoPlayer

**Spec §22 Stufe 4 #1:** "ExoPlayer-Wiedergabe".
**Entschieden:** MediaPlayer aus dem Android-Framework, nicht ExoPlayer aus Media3.
**Begruendung:** TTS-Use-Cases sind 30-90s MP3-Dateien aus Cache — kein Streaming, kein DRM, keine Spatial-Audio-Anforderung. MediaPlayer ist 3 MB kleiner und nachweislich zuverlaessig im BestJournalFrank-Pattern (gleicher Use-Case, seit Monaten produktiv). Wechsel zu ExoPlayer ist trivial wenn spaeter Streaming-Synthese kommt — Single-Aufruf-Stelle in `domain/tts/TtsPlayer.kt`.

## 32. Stufe 4 — TTS-Voices als statische Konstante, kein API-Listing

**Spec §22 Stufe 4 #1:** "Voice-Listing".
**Entschieden:** 30 deutsche Chirp-3-HD-Stimmen als statische Liste in `data/remote/tts/GoogleTtsVoices.kt`, kein dynamisches Voice-Listing.
**Begruendung:** Google fuegt selten neue Stimmen hinzu (~1x/Jahr), und der Voice-Picker mit 30 Eintraegen ist schon am Limit dessen was UX-vertretbar ist. Statische Liste spart einen API-Call beim App-Start und macht das UI offline-faehig. Falls eine neue Chirp-Generation kommt, ist die Aktualisierung ein 30-Sekunden-Edit.

## 33. Stufe 4 — Tagesbriefing als Polling-Worker (90 Min)

**Spec §22 Stufe 4 #2:** "Tagesbriefing automatisch + manuell".
**Entschieden:** Worker laeuft alle 90 Min und prueft selbst, ob heute schon ein Briefing existiert (`AppSettings.dailyBriefingDate == today`).
**Begruendung:** Schichtdienst-Logik (Tag- vs. Nachtdienst, Wachfenster, Schlaffenster) macht eine fixe "Morgens 06:00"-Regel unzuverlaessig. Polling-Pattern uebertraegt die Schichtaware-Logik komplett auf den Worker, ohne Schedule-Branches. Performance unkritisch — Worker exit nach 1ms wenn Date == today.

## 34. Stufe 4 — Briefings als AppSettings-Cache, nicht Room

**Spec §16.3:** "Markdown-Prosa gespeichert als spezielle Insights mit Tag wochenrueckblick/monatsrueckblick".
**Entschieden:** Tagesbriefing/Wochen-/Monatsrueckblick werden in AppSettings (DataStore) als 1-Slot-Cache gespeichert, nicht als Insight-Eintrag in Room.
**Begruendung:** Insights bedeuten "Erkenntnis aus Hypothesen mit Confidence 0-100". Ein Wochenrueckblick ist KEIN Insight-Erkenntnis-Typ — er ist ein narrativer Text. Vermischung wuerde die Confidence-Auswertung verwaessern. Spaeter kann ein eigenes Room-Entity `BriefingHistoryEntity` ergaenzt werden, falls Verlauf gewuenscht ist; aktuell reicht 1 Slot pro Briefing-Typ.

## 35. Stufe 4 — Korrelations-Engine: 36h-Fenster fuer Stack-Match

**Spec §16.1:** "vorhanden vs. weggelassen".
**Entschieden:** Ein BiomarkerSnapshot zaehlt zur "MIT"-Gruppe, wenn in den 36 Stunden VOR `capturedAt` ein SupplementLog mit dem entsprechenden StackType eingetragen wurde.
**Begruendung:** Whoop misst Recovery am Morgen — Effekt eines abendlichen Supplements braucht ueber Nacht Wirkung. 36h ist konservativ genug fuer Sleep-Effekte, aber kurz genug dass keine Carry-Over-Verfaelschung passiert. Cohen's d > 0.3 als Effektgroessen-Schwelle ist Standard fuer "moderate Effekte" in der Psychometrie.

## 36. Stufe 4 — KI-Trigger: Phrasen-Matcher fuer Bedingungen

**Spec §16.2:** "Bei Match: fuehrt die proposedAction aus".
**Entschieden:** Der TriggerPollingWorker matcht Klartext-Bedingungen ("HRV unter 35 ms am Morgen") gegen den letzten Biomarker-Snapshot via Phrasen-Heuristik (sucht "hrv"/"recovery"/"sleep", "unter"/"ueber" + Zahl).
**Begruendung:** Eine vollstaendige DSL fuer Trigger-Bedingungen waere ein eigenes Mini-Compiler-Projekt. Phrasen-Matcher deckt 80% der Gemini-Vorschlaege ab. Falls die KI eine komplexere Bedingung formuliert die nicht matcht, schlaegt der Trigger einfach nicht an — Verhalten ist sicher (false negative statt false positive).

## 37. Stufe 4 — Share-Receiver als separate transparente Activity

**Spec §17:** "Eigene Activity ShareReceiverActivity, transparent, ohne UI."
**Entschieden:** Neue Activity `presentation.share.ShareReceiverActivity` mit `@android:style/Theme.NoDisplay`, `noHistory=true`, `excludeFromRecents=true`. Share-Intent im Manifest von MainActivity getrennt.
**Begruendung:** Wenn Share-Intent direkt MainActivity startet, oeffnet sich beim Teilen die App — das ist nicht das Mental-Model. Mit transparenter Activity sieht der Benutzer nur Toast "Eintrag gespeichert" und kehrt zur Quell-App zurueck.

## 38. Stufe 4 — Glance-Widget mit Hilt-EntryPoint statt @HiltAndroidApp-Default-Inject

**Spec §18:** "Glance Widget".
**Entschieden:** Widget-Composable bekommt EntropyEntryDao via `EntryPointAccessors.fromApplication()` mit eigenem `@EntryPoint`-Interface, nicht via `@AndroidEntryPoint` (das geht bei GlanceAppWidget nicht).
**Begruendung:** GlanceAppWidget ist kein Android-Component im klassischen Sinn (keine Activity, kein Service), Hilt's `@AndroidEntryPoint` funktioniert nicht. EntryPoint-Interface ist die offizielle Hilt-Empfehlung fuer solche Faelle.

## 39. Logging-Vereinheitlichung — statische Diag-Fassade statt Logger-Injection ueberall

**Frank-Wunsch 2026-06-12:** "Logging vereinheitlichen — alle Log.*-Aufrufe auf den DiagnosticLogger umstellen, komplett."
**Entschieden:** Alle 249 `android.util.Log`-Aufrufe (44 Dateien) laufen jetzt ueber die statische Fassade `Diag` (Timber-Muster, `data/diagnostics/Diag.kt`): `Diag.e(DiagnosticArea.X, TAG, msg, t)`. Die Fassade delegiert an den Hilt-Singleton `DiagnosticLogger` (alle 3 Schichten: Room-DB, Logcat, JSONL) und faellt vor `Diag.init()` (EntropyReducerApp.onCreate) verlustfrei auf pures Logcat zurueck. Dazu: 9 neue `DiagnosticArea`-Bereiche (OAUTH, AMAZFIT, POLAR, APP, BIOMARKER, TASKS, AGENTIC, DATABASE, BRIEFING), neues Level `DEBUG` (frueher Log.d), Logcat-Mapping INFO→Log.i korrigiert, DB-Limit 2000→4000.
**Begruendung:** DiagnosticLogger ueberall zu injizieren haette ~40 Konstruktoren/Hilt-Graph-Stellen umgebaut und funktioniert nicht in fruehen Lifecycle-Phasen oder ausserhalb des Graphen. Die Fassade ist EIN Mechanismus fuer alle Aufrufstellen, der alte Klassen-Tag bleibt als `[Tag]`-Praefix erhalten (verlustfrei). Bewusste Ausnahme: `DiagnosticLogger` selbst loggt intern weiter pures Logcat (Endlosschleifen-Schutz).
