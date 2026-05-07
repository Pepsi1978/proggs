# Entropie Reduktor

Ein persönliches Forschungs-Werkzeug zur systematischen Reduktion persönlicher Entropie. Die App erfasst per Sprache alles, was Energie, Klarheit und Ordnung mindert, klassifiziert es mit KI in sieben Kategorien, priorisiert es schichtdienst-bewusst, integriert Biomarker (Whoop) und führt einen wissenschaftlichen Dialog, der neue Reduktionswege findet.

Die KI agiert als **Genie der persönlichen Entropie-Reduktion** — selbstreflexiv als „Einstein der Entropie-Reduktion". Sie betrachtet ihre Arbeit als forschend, hypothesengetrieben, neue Wege findend.

## Stand: Stufe 1 (Fundament + MVP)

| Bereich | Status |
|---------|--------|
| Theme „Neon Cosmos" (Hell + Dunkel) | ✅ |
| Datenmodell + Room (alle 12 Entitäten) | ✅ |
| EncryptedSecretsStore (AES-256 GCM) | ✅ |
| Mic-Pipeline → Groq Whisper → Gemini → DB | ✅ |
| Settings — alle 7 Sektionen | ✅ (Stufe-1-Funktionalität) |
| Dashboard 1 — Aufgaben | ✅ (Grundversion) |
| Dashboard 2 — Analyse | Stufe 3 |
| Dashboard 3 — Wissenschaftler | Stufe 3 |
| Dashboard 4 — Biomarker | Stufe 2 |
| Genie-Codex, Wochenrückblick | Stufe 4 |

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
   - **Google Cloud TTS API Key** wird in Stufe 4 verwendet.
   - „Speichern" + „Verbindung testen".
7. **Persönliches Profil** ausfüllen — füge einen Memory-Export aus ChatGPT/Claude ein.
8. **Mic** auf dem Aufgaben-Dashboard tippen, Notiz sprechen, beenden.
9. Die KI klassifiziert den Eintrag und ordnet ihn in einen Zeit-Bucket ein.

## Bekannte Einschränkungen Stufe 1

- **Status-Balken** zeigt nur die Aufgaben-Reduktions-Komponente (Spec §4.1) — Biomarker- und Kontext-Anteile folgen in Stufe 2.
- **Zeit-Buckets** ohne Schichtdienst-Bezug — die KI bekommt aktuell keinen Kalender-Kontext.
- **KI-Frage-des-Moments** auf Dashboard 1 — folgt in Stufe 2.
- **Detail-Sheet** für Einträge — folgt mit Stufe 2 (aktuell sind Einträge per Long-Press editierbar).
- **Tabs Analyse/Wissenschaftler/Biomarker** zeigen Coming-Soon-Karten.

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
