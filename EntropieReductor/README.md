# Entropie Reduktor

Ein persönliches Forschungs-Werkzeug zur systematischen Reduktion persönlicher Entropie. Die App erfasst per Sprache alles, was Energie, Klarheit und Ordnung mindert, klassifiziert es mit KI in sieben Kategorien, priorisiert es schichtdienst-bewusst, integriert Biomarker (Whoop) und führt einen wissenschaftlichen Dialog, der neue Reduktionswege findet.

Die KI agiert als **Genie der persönlichen Entropie-Reduktion** — selbstreflexiv als „Einstein der Entropie-Reduktion". Sie betrachtet ihre Arbeit als forschend, hypothesengetrieben, neue Wege findend.

## Stand: Stufe 4 (Vollausbau — Politur + lernende Mechanismen)

| Bereich | Status |
|---------|--------|
| Theme „Neon Cosmos" (Hell + Dunkel) | ✅ |
| Datenmodell + Room (alle 12 Entitäten) | ✅ |
| EncryptedSecretsStore (AES-256 GCM) | ✅ |
| Mic-Pipeline → Groq Whisper → Gemini → DB | ✅ |
| Settings — alle 8 Sektionen | ✅ |
| Dashboard 1 — Aufgaben | ✅ |
| Dashboard 2 — Analyse | ✅ |
| Dashboard 3 — Wissenschaftler | ✅ |
| Dashboard 4 — Biomarker | ✅ |
| Experiment-Kalender | ✅ |
| Insight Board + Repertoire | ✅ |
| Genie-Codex-Synthese | ✅ |
| Drive-Backup (App-Folder) | ✅ |
| Whoop OAuth + Sync | ✅ |
| Google Calendar OAuth + Sync | ✅ |
| Schicht-bewusste Notifications | ✅ |
| **Stufe 4 — Google Cloud TTS Chirp 3 HD (30 Stimmen)** | ✅ |
| **Stufe 4 — Tagesbriefing (auto + manuell, mit Audio)** | ✅ |
| **Stufe 4 — Wochen- + Monatsrückblick** | ✅ |
| **Stufe 4 — Korrelations-Engine (Cohen's d)** | ✅ |
| **Stufe 4 — KI-Trigger-Engine + Approval-Flow** | ✅ |
| **Stufe 4 — Share-Sheet-Empfänger** | ✅ |
| **Stufe 4 — Home-Screen-Widget (Glance)** | ✅ |

Details siehe `DECISIONS.md`.

## Setup

1. **Android Studio**: Hedgehog (2023.1) oder neuer.
2. **JDK 17** (z. B. Microsoft OpenJDK).
3. **Android SDK 35** + Build-Tools.
4. **Klonen + Bauen**:
   ```
   ./gradlew assembleDebug
   ```
5. **APK installieren**:
   ```
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
6. **Erster Start** — Setup öffnet:
   - **Settings → API-Schlüssel** öffnen.
   - **Groq API Key** eintragen (https://console.groq.com — kostenlos für moderate Nutzung).
   - **Gemini API Key** eintragen (https://aistudio.google.com — Free-Tier ausreichend für persönlichen Gebrauch).
   - **Google Cloud TTS API Key** für Chirp 3 HD-Stimmen (1 Mio Zeichen/Monat gratis, danach $30/Mio).
     Setup: Google Cloud Console → APIs aktivieren → "Cloud Text-to-Speech API" → Anmeldedaten → "API-Schlüssel erstellen".
   - „Speichern" + „Verbindung testen".
   - **Stimme auswählen**: Auf der gleichen Seite → „Stimme anhören" → Picker → eine der 30 deutschen Chirp-3-HD-Stimmen.
7. **Persönliches Profil** ausfüllen — füge einen Memory-Export aus ChatGPT/Claude ein.
8. **Mic** auf dem Aufgaben-Dashboard tippen, Notiz sprechen, beenden.
9. Die KI klassifiziert den Eintrag und ordnet ihn in einen Zeit-Bucket ein.

## Drive-Backup einrichten (einmalig)

Damit das automatische Drive-Backup funktioniert, ist einmalig ein OAuth-Eintrag in der Google Cloud Console nötig:

1. Cloud Console → APIs & Dienste → Anmeldedaten → **OAuth-Client-ID erstellen**
2. Anwendungstyp: **Android**
3. Paketname: `de.frank.entropyreducer.debug` (Debug) oder `de.frank.entropyreducer` (Release)
4. SHA-1: aus dem `debug-shared.keystore` in `~/SK/BestJournalAndroid/` (gleicher Keystore wie BestJournalAndroid). Auslesen: `keytool -list -v -keystore ~/SK/BestJournalAndroid/debug-shared.keystore`
5. APIs aktivieren: **Drive API** (`https://www.googleapis.com/auth/drive.appdata` Scope wird automatisch verwendet)
6. App in der App neu starten → Einstellungen → Datenexport → "Mit Google verbinden"

Bis dieser Schritt erledigt ist, zeigt der Sign-In-Dialog `DEVELOPER_ERROR` (Code 10) — die App fängt das ab und zeigt einen entsprechenden Snackbar mit Hinweis auf dieses README.

**Was synchronisiert wird:** alle Entropie-Eintraege als JSON-Datei (`entropy_reducer_entries_v1.json`) im appDataFolder deines Google-Drive-Kontos. Diese Datei ist nicht im normalen Drive sichtbar — nur diese App kann sie lesen. Memory, Profil und API-Keys bleiben rein lokal.

**Wann synchronisiert wird:** nach jeder Mutation (Anlegen / Status aendern / Loeschen) mit 1.5s Debouncing. Gleichzeitige Aenderungen werden zu einem einzigen Upload zusammengefasst (Coalescing — kein Job-Stacking). Beim App-Start wird einmalig vom Drive nachgeholt, was lokal fehlt (Last-Write-Wins per `updatedAt`).

## Theme-Toggle

Der Sun/Moon-Schalter neben dem Zahnrad-Icon zykelt durch drei Modi:

- **Auto (System):** folgt der Hell-/Dunkel-Einstellung des Geraets (Standard).
- **Hell-Modus:** immer hell.
- **Dunkel-Modus:** immer dunkel — entspricht den Referenzbildern 11–20.

Der gewaehlte Modus wird in den App-Settings persistiert und ueberlebt App-Neustarts.

## Stufe-4-Mechanismen — wie sie laufen

| Mechanismus | Wann läuft er | Was passiert |
|-------------|--------------|--------------|
| **Tagesbriefing** | Polling alle 90 Min | Worker prüft, ob heute schon eines existiert; wenn nicht: kurzes Briefing (max 7 Sätze, 2. Person) per Gemini, gecached in AppSettings, als Audio anhörbar (Chirp 3 HD). |
| **Wochenrückblick** | Sonntag 19:00 lokal | 350 Wörter Fließtext der letzten 7 Tage, mit Notification "anhören". |
| **Monatsrückblick** | 1. des Folgemonats 19:00 (Worker prüft täglich) | 700 Wörter Fließtext der letzten 30 Tage. |
| **Korrelations-Engine** | Täglich 03:30 | Cohen's d zwischen Supplements (vorhanden vs. weggelassen) und HRV/Recovery/Sleep. Schwelle: \|d\| > 0.3 und n ≥ 7 in beiden Gruppen. Loggt Beobachtungen, keine Kausalitätsaussagen. |
| **KI-Trigger-Engine** | Mittwoch + Sonntag 11:00 (Worker prüft täglich) | Gemini schlägt bis zu 3 Trigger vor (Bedingung + Aktion). Zeigen sich als „Ausstehend" in Settings → KI-Trigger. Du nimmst an oder lehnst ab. |
| **Trigger-Polling** | Alle 15 Min | Aktive Trigger werden gegen den letzten Biomarker-Snapshot geprüft. Bei Match feuert eine Notification (6h-Cooldown pro Trigger). |
| **Share-Sheet** | Beim Teilen aus anderer App | Transparente Activity nimmt den Text entgegen, schreibt sofort einen Eintrag mit `source = SHARE_SHEET`, KI verarbeitet im Hintergrund. |
| **Home-Widget** | Update alle 15 Min | 4×2-Glance-Widget: Status-Pille, Top-Aufgabe, großer Mic-Button → öffnet App. |

## Samsung-Hinweis (One UI)

Auf Samsung-Geräten (z.B. Fold 6) bitte App in **Geräte-Pflege → Akku → App-Akku-Verbrauch → Einschränkung aufheben** markieren — sonst killt One UI die WorkManager-Worker im Schlaffenster und Tagesbriefing/Triggers laufen nicht zuverlässig.

## Tech-Stack

- Kotlin 2.1, Jetpack Compose (BOM 2025.01.01), Material 3
- Hilt 2.55, Room 2.7, Retrofit 2.11 + Kotlinx Serialization
- DataStore für UI-Settings, EncryptedSharedPreferences für API-Keys
- AppAuth (vorbereitet für Whoop + Google Calendar OAuth in Stufe 2)
- Media3 ExoPlayer (vorbereitet für Chirp-3-HD-Wiedergabe in Stufe 4)
- Vico für Charts (Stufe 2-4)
- WorkManager (Hintergrund-Sync ab Stufe 2)
- minSdk 28 (Android 9), targetSdk 35 (Android 15)

## Architektur

```
de.frank.entropyreducer
├── data
│   ├── local      Room Database, DAOs, Entities
│   ├── remote     Retrofit (Groq Whisper, Gemini)
│   ├── repository Repositories
│   ├── settings   EncryptedSecretsStore + DataStore-AppSettings
│   └── audio      MediaRecorder + Foreground-Service
├── domain
│   ├── model      Enums, Domain-Models
│   └── usecase    SystemPromptBuilder, ProcessEntryUseCase, …
├── presentation
│   ├── theme      Neon Cosmos
│   ├── components GlassCard, MicButton, StatusBar, EntropyCategoryPill
│   ├── dashboard1 Tasks
│   ├── dashboard  ComingSoonScreen für 2/3/4
│   ├── settings   7 Sub-Screens + zentrale ViewModels
│   └── navigation Routes + AppNavGraph + CosmosBottomBar
└── di            Hilt-Module (Database, Network)
```

## Sicherheit

- API-Keys + OAuth-Tokens nur in EncryptedSharedPreferences (AES-256-GCM mit MasterKey).
- Audio-Dateien werden nach erfolgreicher Transkription sofort gelöscht.
- `android:allowBackup="false"` — verschlüsselte Keys gehören nicht in Cloud-Backup.
- `usesCleartextTraffic="false"` — nur HTTPS.

## Lizenz

Privates Projekt — kein öffentliches Lizenzmodell.
