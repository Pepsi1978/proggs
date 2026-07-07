# TerminalVoiceOverlay-macOS

Voice-to-text overlay for terminal applications (iTerm, Terminal.app, Warp) on macOS.
Press a hotkey → speak → text is typed into the active terminal.

## Build

**Always use `build.sh`** — never compile with `swiftc` directly.
`build.sh` handles certificate signing with "Frank Local Dev", which preserves TCC (accessibility) permissions across rebuilds.

```bash
bash build.sh
open build/TerminalVoiceOverlay.app
```

Direct `swiftc` results in ad-hoc signing → macOS revokes accessibility permissions on every rebuild.

## Architecture

| File | Purpose |
|------|---------|
| `Config.swift` | API keys from .env, configuration |
| `AudioRecorder.swift` | AVAudioEngine microphone recording |
| `GroqWhisperClient.swift` | Groq Whisper API for speech-to-text |
| `GeminiClient.swift` | Gemini API for text correction/formatting |
| `TerminalController.swift` | CGEvent keyboard simulation for terminals (Ctrl+U to clear line) |
| `AppWatcher.swift` | NSWorkspace observer for target app detection |
| `OverlayPanel.swift` | Floating NSPanel overlay UI |
| `ErrorDescriptions.swift` | Extracted error description methods |
| `AppDelegate.swift` | App lifecycle, hotkey registration, orchestration |
| `main.swift` | Entry point |

## Key Patterns

- **Text insertion**: CGEvent keyboard simulation with clipboard save/restore
- **Line clearing**: Ctrl+U (terminal-specific)
- **Target apps**: iTerm2, Terminal.app, Warp (bundle IDs in AppWatcher.swift)
- **Thread safety**: DispatchQueue.main.async for all UI updates
- **Retries**: DispatchQueue.asyncAfter (never Thread.sleep on main thread)
- **Code signing**: "Frank Local Dev" certificate (not ad-hoc)

## Sister Project (CRITICAL)

**ClaudeCodexVoiceOverlay-macOS** shares ~80% of the code.
When changing ANY shared file, ALWAYS apply the same change to:
`~/proggs/ClaudeCodexVoiceOverlay-macOS/`

### Shared files (nearly identical):
Config.swift, AudioRecorder.swift, GroqWhisperClient.swift, GeminiClient.swift,
AppDelegate.swift, AppWatcher.swift, OverlayPanel.swift, ErrorDescriptions.swift, build.sh

### Different files:
- `TerminalController.swift` (this project) vs `InputController.swift` (ClaudeCodex)
- Target apps: terminals vs Electron apps (Claude Desktop, Codex)
- Key combo: Ctrl+U (clear line) vs Cmd+A+Backspace (select all + delete)

## Windows Counterpart

**TerminalVoiceOverlay-Windows** — same functionality in C#/WPF.

| macOS (Swift) | Windows (C#) |
|---------------|-------------|
| TerminalController.swift | Services/TerminalController.cs |
| AppWatcher.swift | Services/TerminalWatcher.cs |
| AudioRecorder.swift | Services/AudioRecorder.cs |
| GroqWhisperClient.swift | Services/GroqWhisperClient.cs |
| GeminiClient.swift | Services/GeminiClient.cs |
| Config.swift | Services/Config.cs |
| OverlayPanel.swift | Views/OverlayWindow.xaml + .cs |
| AppDelegate.swift | App.xaml.cs |
| CGEvent | keybd_event / SendInput (Win32.cs) |
| AVAudioEngine | NAudio |
| NSWorkspace | SetWinEventHook |

## Requirements

- macOS 13.0+, Apple Silicon (arm64)
- Xcode Command Line Tools (`xcode-select --install`)
- .env file with GROQ_API_KEY (required) and GEMINI_API_KEY (optional)
- Accessibility permission in System Settings → Privacy & Security
