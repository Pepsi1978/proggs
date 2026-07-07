# Cortex — Android-App fürs zweite Gehirn

Private Android-App (nur für eigene Geräte, kein Play Store). Kommuniziert mit einem
selbst-gehosteten Vektor-Gehirn über WireGuard-VPN.

## Voraussetzungen

- Android Studio (Ladybug oder neuer)
- JDK 21
- Android SDK 35
- WireGuard-Konfiguration (`SecondBrain.conf`) für den VPS

## Build & Install

```bash
cd CortexAndroid
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Schlüssel eintragen

Alle Schlüssel werden in der App unter **Einstellungen** eingetragen und verschlüsselt
gespeichert (`EncryptedSharedPreferences`). Kein Schlüssel liegt im Repo.

| Schlüssel | Wo eintragen | Wofür |
|-----------|-------------|-------|
| `SB_API_KEY` | Einstellungen → Verbindung | Bearer-Auth für Agent + Brain-API |
| WireGuard-Konfig | Einstellungen → Verbindung (Datei oder Textfeld) | VPN-Tunnel zum VPS |
| `GROQ_API_KEY` | Einstellungen → KI-Schlüssel | Spracheingabe (Whisper) |
| `GEMINI_API_KEY` | Einstellungen → KI-Schlüssel | Vorlesen (TTS) + Verbessern |

**Standard-Ports** (änderbar in den Einstellungen):
- Agent: `8002`
- Brain-API: `8000`
- Dashboard: `8003`
- Server-Host: `10.8.0.1` (WireGuard-VPN-IP)

## Architektur

```
de.frank.cortex
  ├─ CortexApp.kt              (Application, Initialisierung)
  ├─ MainActivity.kt           (Compose-Host, Navigation)
  ├─ ui/
  │   ├─ theme/                (Material 3, Dark/Light, Space Grotesk/Inter/JetBrains Mono)
  │   ├─ chat/                 (Gespräch mit dem Agenten, 2-Schritt-Speicherung)
  │   ├─ dashboard/            (Übersicht, Spektrum, Suche, Bearbeiten, Löschen)
  │   ├─ settings/             (Schlüssel, VPN, Theme, Version)
  │   └─ common/               (TopBar, VPN-Schalter, Hilfs-Composables)
  ├─ data/
  │   ├─ SettingsStore.kt      (EncryptedSharedPreferences-Wrapper)
  │   └─ model/                (DTOs mit Moshi @JsonClass)
  ├─ network/
  │   ├─ AgentApi.kt           (Retrofit: /chat, /categories)
  │   ├─ BrainApi.kt           (Retrofit: /search, /entry, /health)
  │   ├─ DashboardApi.kt       (Retrofit: /api/overview)
  │   └─ ApiClient.kt          (Singleton-Clients, Moshi, externe APIs)
  ├─ vpn/
  │   └─ WireGuardManager.kt   (GoBackend, Tunnel, Status)
  ├─ audio/
  │   ├─ MicRecorder.kt        (AudioRecord → WAV)
  │   └─ PcmPlayer.kt          (AudioTrack für Gemini-PCM)
  └─ observability/
      ├─ CortexLog.kt           (JSON-Lines + Logcat TAG FRANK_CORTEX/LOGIC)
      └─ CortexCrashHandler.kt  (Globaler Crash-Fänger)
```

## Beobachtung (Observability)

- **Logcat:** `adb logcat -s FRANK_CORTEX` (allgemein), `adb logcat -s LOGIC` (Checkpoints)
- **Log-Datei:** `cortex.log.jsonl` im App-internen Speicher
- **Checkpoints:** VPN-Connect, Chat-Send, Speicher-Bestätigung, STT, TTS

## Externe APIs

| Dienst | URL | Auth | VPN nötig |
|--------|-----|------|-----------|
| Agent | `http://10.8.0.1:8002` | Bearer | Ja |
| Brain-API | `http://10.8.0.1:8000` | Bearer | Ja |
| Dashboard | `http://10.8.0.1:8003` | keine | Ja |
| Groq STT | `https://api.groq.com` | Bearer | Nein |
| Gemini TTS | `https://generativelanguage.googleapis.com` | x-goog-api-key | Nein |
