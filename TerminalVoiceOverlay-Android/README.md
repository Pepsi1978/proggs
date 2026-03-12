# TerminalVoiceOverlay-Android

## Was ist das?

Eine schwebende Spracheingabe-App fuer Android, die als semitransparentes, pillenfoermiges Panel am rechten Bildschirmrand erscheint. Die App funktioniert zusammen mit Termux (oder jeder anderen App) und bietet Sprach-zu-Text-Erkennung ueber die Groq Whisper API, optionale Textkorrektur via Google Gemini, und direktes Einfuegen in die Zwischenablage.

Dies ist das Android-Gegenstueck zu den Windows- (C#/WPF) und macOS- (Swift/AppKit) Versionen.

## Features

- Schwebendes Overlay, das immer ueber Termux (oder jeder anderen App) angezeigt wird
- Sprache-zu-Text via Groq Whisper API
- Optionale Textkorrektur via Google Gemini API
- 6 Buttons: X (Leeren), W (Whisper-Undo), BTW-Mic, Haupt-Mic, G (Gemini-Umschalter), Enter (Auto-Enter)
- Gleiches Aussehen und Bedienung wie die Windows- und macOS-Versionen
- Vertikal ziehbar (Drag)
- Vibrations-Feedback
- Auto-Enter-Modus (fuegt automatisch einen Zeilenumbruch an den Text an)

## Voraussetzungen

- Android 8.0 (Oreo) oder hoeher
- Termux-App installiert (empfohlen, aber nicht zwingend)
- Groq API Key (erforderlich, kostenlos unter [console.groq.com](https://console.groq.com))
- Gemini API Key (optional, nur fuer Textkorrektur benoetigt)

## Installation (Schritt-fuer-Schritt, anfaengerfreundlich)

1. Die APK-Datei auf dein Android-Handy kopieren (per USB-Kabel, Cloud-Dienst, etc.)
2. In den Android-Einstellungen unter **Sicherheit** → **Unbekannte Apps installieren** fuer deinen Dateimanager die Installation erlauben
3. Die APK-Datei antippen und die Installation bestaetigen
4. Die App oeffnen, Groq API Key (und optional Gemini API Key) eingeben
5. **"Save Settings"** antippen, um die Keys zu speichern
6. **"Start Overlay"** antippen, um das schwebende Panel zu starten
7. Android fragt nach der **Overlay-Berechtigung** → erlauben
8. Android fragt nach der **Mikrofon-Berechtigung** → erlauben
9. Das schwebende Pill-Panel erscheint jetzt am rechten Bildschirmrand
10. Zu Termux wechseln, auf den Mic-Button tippen, sprechen, nochmal antippen → Text landet in der Zwischenablage
11. In Termux einfuegen: **Langer Druck** oder **Lautstaerke-runter + V**

## Benutzung der Buttons

| Button | Funktion |
|--------|----------|
| **X** (rot) | Zwischenablage leeren |
| **W** (gruen) | Whisper-Undo: Rohe Transkription (ohne Gemini-Korrektur) in die Zwischenablage kopieren |
| **BTW** (hellblau) | Sekundaere Aufnahme, fuegt "/btw " vor den transkribierten Text |
| **Mic** (dunkelblau) | Hauptaufnahme starten und stoppen |
| **G** (dunkel/gruen) | Gemini-Textkorrektur ein- und ausschalten |
| **Enter** (orange/dunkel) | Auto-Enter ein- und ausschalten (haengt `\n` an den Text an, Termux interpretiert das als Enter-Taste) |

## Button-Farben und Status

| Farbe | Bedeutung |
|-------|-----------|
| Dunkelblau | Idle (Bereit) |
| Hellblau | Idle — BTW-Button |
| Rot pulsierend | Aufnahme laeuft (Mic) |
| Blau pulsierend | Aufnahme laeuft (BTW) |
| Orange | Verarbeitung (Transkription oder Gemini-Korrektur) |
| Gruen (3 Sek.) | Erfolg — Text in Zwischenablage |
| Rot (3 Sek.) | Fehler aufgetreten |

## Build from Source

Voraussetzungen: .NET 10 SDK mit Android Workload

```bash
# Android Workload installieren (einmalig)
dotnet workload install android

# Debug Build
dotnet build

# Release APK erstellen
dotnet publish -c Release -f net10.0-android
```

Die fertige APK liegt dann unter:
`bin/Release/net10.0-android/publish/com.pepsi1978.terminalvoiceoverlay-Signed.apk`

Benoet igte Umgebungsvariablen fuer den Build:

```
JAVA_HOME=C:/Program Files (x86)/Android/openjdk/jdk-17.0.14
ANDROID_HOME=C:/Program Files (x86)/Android/android-sdk
```

## Architektur

| Datei | Zweck |
|-------|-------|
| `MainActivity.cs` | Einstellungen, Berechtigungen, Service starten und stoppen |
| `OverlayService.cs` | ForegroundService mit schwebendem Overlay-Fenster, Button-Handler, State Machine |
| `Services/AudioRecorder.cs` | Android AudioRecord API, 16 kHz Mono PCM, WAV-Export |
| `Services/GroqWhisperClient.cs` | Groq Whisper API Client (Sprache zu Text) |
| `Services/GeminiClient.cs` | Google Gemini API Client (Textkorrektur) |
| `Services/Config.cs` | SharedPreferences-basierte Konfiguration |
| `Models/RecordingState.cs` | Enum fuer den Aufnahmezustand |

## Schwester-Projekte

| Plattform | Projekt | Sprache |
|-----------|---------|---------|
| Windows | ClaudeVoiceOverlay-Windows | C# / WPF |
| Windows | TerminalVoiceOverlay-Windows | C# / WPF |
| macOS | ClaudeCodexVoiceOverlay-macOS | Swift / AppKit |
| macOS | TerminalVoiceOverlay-macOS | Swift / AppKit |
| Android (CLI) | TerminalVoiceOverlay-Termux | TypeScript |
| **Android (GUI)** | **TerminalVoiceOverlay-Android** | **C# / .NET Android** |

Alle Desktop-Versionen teilen sich etwa 80 % der Logik (API-Clients, State Machine, Config). Aenderungen an der Groq- oder Gemini-Integration sollten daher immer in allen Versionen gleichzeitig durchgefuehrt werden.
