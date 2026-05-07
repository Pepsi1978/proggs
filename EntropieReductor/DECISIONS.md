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
