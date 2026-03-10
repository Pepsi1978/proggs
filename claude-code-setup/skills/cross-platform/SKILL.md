---
name: cross-platform
description: Use AUTOMATICALLY when developing or modifying code that exists on both macOS (Swift/AppKit) and Windows (C#/WPF), or when creating new cross-platform features. Ensures platform parity, correct platform APIs, and consistent behavior across systems.
---

# Cross-Platform Entwicklung (macOS + Windows)

## Wann

Dieses Skill gilt IMMER wenn:
- Code in einem macOS-Projekt (Swift) UND einem Windows-Projekt (C#/WPF) geaendert oder erstellt wird
- Ein neues Feature auf einer Plattform gebaut wird (dann auch auf der anderen umsetzen)
- Plattformspezifische APIs, Pfade oder Shell-Befehle verwendet werden
- Der User "Feature-Gleichstand" oder "Parität" zwischen Plattformen anfordert

## Projektstruktur-Mapping

| Funktion | macOS (Swift/AppKit) | Windows (C#/WPF) |
|----------|---------------------|-------------------|
| **Build-Skript** | `build.sh` (bash) | `publish.ps1` (PowerShell) |
| **Keyboard-Input** | `InputController.swift` / `TerminalController.swift` (CGEvent) | `TerminalController.cs` / `AppController.cs` (keybd_event/Win32) |
| **Prozess-Erkennung** | `AppWatcher.swift` (NSWorkspace, Bundle IDs) | `AppWatcher.cs` / `TerminalWatcher.cs` (SetWinEventHook, Prozessnamen) |
| **Audio-Aufnahme** | `AudioRecorder.swift` (AVAudioEngine, nativ) | `AudioRecorder.cs` (NAudio NuGet-Paket) |
| **Konfiguration** | `Config.swift` (.env-Parsing) | `Config.cs` (.env-Parsing, typisiert) |
| **Autostart** | LaunchAgent (plist in `~/Library/LaunchAgents/`) | PowerShell-Skript (Shortcut in Startup-Ordner) + `watcher.vbs` |
| **System-Tray** | NSStatusItem (Menueleiste) | NotifyIcon (System Tray) |
| **Code-Signierung** | `codesign` + Entitlements-Datei (Pflicht) | Optional (Smart App Control beachten) |

## Regeln

### 1. Feature-Paritaet sicherstellen

Wenn ein Feature auf einer Plattform implementiert wird, MUSS es auch auf der anderen umgesetzt werden. Ablauf:
1. Feature auf der ersten Plattform fertig implementieren und testen
2. Sofort die Gegenstueck-Implementation auf der anderen Plattform erstellen
3. In der Commit-Message erwaehnen: "Feature-Gleichstand"

### 2. Plattform-spezifische APIs korrekt verwenden

**Tastatur-Simulation:**
- macOS: `CGEvent` mit virtuellen Keycodes (z.B. 0x09 = V, 0x24 = Return)
- Windows: `keybd_event` mit VK-Konstanten (z.B. VK_V, VK_RETURN)
- NIEMALS plattformfremde Keycodes verwenden

**Einfuegen (Paste):**
- macOS: Cmd+V (`.maskCommand` + keyCode 0x09)
- Windows: Ctrl+V (VK_CONTROL + VK_V)

**Clipboard:**
- macOS: `NSPasteboard.general`
- Windows: `System.Windows.Forms.Clipboard` oder `System.Windows.Clipboard`

**Zeile loeschen (Terminal):**
- macOS: Ctrl+U (keyCode 0x08, `.maskControl`)
- Windows: End + 500x Backspace (kein Ctrl+U-Aequivalent im Windows Terminal)

### 3. Pfade und Dateisystem

- macOS: Forward Slashes (`/Users/frank/`), Tilde-Expansion (`~/`)
- Windows: Backslashes (`C:\Users\frank\`), Umgebungsvariablen (`%USERPROFILE%`)
- Config-Suche macOS: Bundle-Pfad > Projekt-Root > CWD > `~/.config/ProjectName/`
- Config-Suche Windows: Exe-Pfad > CWD > `%USERPROFILE%` > `%APPDATA%\ProjectName\`
- IMMER plattformeigene Path-APIs verwenden: `URL(fileURLWithPath:)` (Swift) bzw. `Path.Combine()` (C#)

### 4. Prozess-Erkennung

- macOS: Bundle IDs verwenden (z.B. `"com.apple.Terminal"`, `"com.anthropic.claudefordesktop"`)
- Windows: Prozessnamen verwenden, CASE-INSENSITIVE (z.B. `"WindowsTerminal"`, `"Claude"`)
- ACHTUNG: Windows-Prozessnamen koennen doppelt vorkommen -- immer HWND + Prozess-ID verifizieren

### 5. Audio-Aufnahme

- macOS: AVAudioEngine (nativ, kein externer Package noetig)
- Windows: NAudio NuGet-Paket (v2.2.1+) -- muss in .csproj referenziert sein
- Beide: 16kHz Mono als Standard-Format
- Temp-Dateien: `FileManager.default.temporaryDirectory` (macOS) bzw. `Path.GetTempPath()` (Windows)

### 6. Build-Prozess

**macOS (`build.sh`):**
```
- SDK-Pfad dynamisch: xcrun --show-sdk-path
- Target: arm64-apple-macos13.0 (oder hoeher)
- Code-Signierung: codesign --force --deep --sign "Developer ID"
- Entitlements-Datei beifuegen
- Ergebnis: build/ProjectName.app Bundle
```

**Windows (`publish.ps1`):**
```
- dotnet publish mit: --self-contained true, -p:PublishSingleFile=true
- Runtime: win-x64
- Ergebnis: publish/ProjectName.exe (Self-Contained, ~60-80MB)
```

### 7. .env-Konfiguration harmonisieren

Gemeinsame Variablen (beide Plattformen):
```
GROQ_API_KEY=
GEMINI_API_KEY=
WHISPER_MODEL=whisper-large-v3-turbo
WHISPER_LANG=de
AUDIO_SAMPLE_RATE=16000
AUDIO_CHANNELS=1
```

Plattform-spezifisch:
```
# macOS: Bundle IDs
TARGET_BUNDLE_IDS=com.apple.Terminal,com.anthropic.claudefordesktop

# Windows: Prozessnamen (case-insensitive)
TARGET_PROCESS_NAMES=WindowsTerminal,pwsh,powershell,Claude
```

### 8. README-Dokumentation

Jedes Projekt braucht eine eigene README.md mit:
- Plattform-spezifischen Installationsanweisungen
- Schritt-fuer-Schritt Anleitung fuer Anfaenger (Windows-User, die mit AI-programmierten Apps nicht vertraut sind)
- Fehlerbehebung / Troubleshooting Sektion
- Screenshots wenn moeglich

### 9. UI-Design Richtlinien (plattformuebergreifend)

- Beide Plattformen: Schwebende Overlay-Fenster (Always-on-top)
- macOS: NSPanel mit `.nonactivatingPanel` Style (aktiviert keine App-Wechsel)
- Windows: WPF Window mit `Topmost=true`, `AllowsTransparency=true`
- Einheitliches visuelles Design: gleiche Farben, Icons, Animationen
- Responsives Layout: Anpassung an verschiedene Bildschirmgroessen und DPI-Skalierungen
- Windows: DPI-Awareness beachten (`PerMonitorV2`)

### 10. Threading und Timing

- macOS: `DispatchQueue.main.async` fuer UI-Updates, `usleep()` fuer Verzoegerungen
- Windows: `Dispatcher.Invoke()` fuer UI-Updates, `Thread.Sleep()` + `lock` fuer Thread-Safety
- WICHTIG: Alle UI-Operationen MUESSEN auf dem Main-Thread laufen (beide Plattformen)

## Checkliste bei Cross-Platform Aenderungen

- [ ] Feature auf beiden Plattformen implementiert?
- [ ] Korrekte plattformspezifische APIs verwendet?
- [ ] Pfade mit nativen APIs konstruiert (keine Hardcoded-Pfade)?
- [ ] .env-Variablen konsistent zwischen beiden Plattformen?
- [ ] README auf beiden Seiten aktualisiert?
- [ ] Build-Skript auf der jeweiligen Plattform getestet/angepasst?
- [ ] UI sieht auf beiden Plattformen konsistent aus?
- [ ] Commit-Message erwaehnt Feature-Gleichstand?
