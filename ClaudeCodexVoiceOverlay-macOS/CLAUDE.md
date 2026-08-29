# ClaudeCodexVoiceOverlay-macOS

Voice-to-text overlay for Electron applications (Claude Desktop, Codex) on macOS.
Press a hotkey → speak → text is typed into the active Electron app.

## Build

**Always use `build.sh`** — never compile with `swiftc` directly.
`build.sh` handles certificate signing with "Frank Local Dev", which preserves TCC (accessibility) permissions across rebuilds.

```bash
bash build.sh
open build/ClaudeCodexVoiceOverlay.app
```

Direct `swiftc` results in ad-hoc signing → macOS revokes accessibility permissions on every rebuild.

## Architecture

This app is a **full 1:1 port of `TerminalVoiceOverlay-macOS`** (same 34 source files,
same UI, same features). The ONLY differences are the input layer (Electron vs terminal),
the target apps, and per-app data isolation (see below).

### Core
| File | Purpose |
|------|---------|
| `Config.swift` | API keys from .env (`~/SK/VoiceOverlays/.env`), configuration |
| `AudioRecorder.swift` | AVAudioEngine microphone recording |
| `GroqWhisperClient.swift` | Groq Whisper API for speech-to-text |
| `GeminiBatchTranscribeClient.swift` | **Gemini speech-to-text.** `gemini-3.5-transcribe` via the Interactions API (`POST /v1beta/interactions`), audio inline as base64. Measured on Windows: 4,4 s vs 15,1 s for the live variant; WER 2,6 % vs 4,6 % (Groq). Mode `verbatim`, because `smart` drops words |
| `PersonalVocabulary.swift` | Shared word list from `SK/VoiceOverlays/personal-vocabulary.txt`, fed to Gemini as `customVocabulary` |
| `SpeechToTextRouter.swift` | Picks Groq or Gemini per recording; falls back to Groq on technical Gemini failure (quota, network) but NOT on `noSpeech` |
| `TranscriptionEngineSetting.swift` | Engine switch (`groq` / `gemini`), stored as `SK/VoiceOverlays/transcription-engine.txt` |
| `build.sh` | **Fixed source list** — every new `.swift` file must be added there, otherwise it is silently not compiled |
| `GeminiClient.swift` | Gemini API for text correction/formatting |
| `TerminalController.swift` | CGEvent keyboard sim — **Electron variant** (Cmd+A+Backspace). Class name kept identical to TVO so all call sites match; only the keystrokes differ. |
| `AppWatcher.swift` | NSWorkspace observer; targets `com.anthropic.claudefordesktop` + `com.openai.codex` |
| `AppDelegate.swift` | App lifecycle, hotkey registration, orchestration (defines `tvoDebug`) |
| `ErrorDescriptions.swift` · `main.swift` | Error text · entry point |

### Overlay UI
`OverlayPanel`, `OverlayHorizontalLayout`, `OverlayOrientation`, `OverlayGlideAnimation`,
`OverlayCollapsedMic`, `OverlayExtraButtons`, `WaveformView`, `IconPaths`, `AutoHideController`

### Prompt board / history / slots (+ Google Drive backup)
`PromptBoardPanel`, `PromptBoardStore`, `PromptBoardModels`, `PromptBoardDialogs`,
`PromptInputPanel`, `PromptHistoryPanel`, `PromptHistoryStore`, `PromptSlotStore`,
`GoogleDriveBackupService`

### Input / hotkeys / dialogs / services
`HotkeyRegistry` (Carbon), `PushToTalkController`, `SettingsDialog`, `CommonDialogs`,
`AutoEnterStatusServer` (Network), `AlwaysOnPrefixService`, `VoiceServiceProvider`

## Key Patterns

- **Text insertion**: CGEvent keyboard simulation with clipboard save/restore
- **Field clearing**: Cmd+A + Backspace (Electron-specific, not Ctrl+U)
- **Target apps**: Claude Desktop, Codex (Electron bundle IDs in AppWatcher.swift)
- **Thread safety**: DispatchQueue.main.async for all UI updates
- **Retries**: DispatchQueue.asyncAfter (never Thread.sleep on main thread)
- **Code signing**: "Frank Local Dev" certificate (not ad-hoc)
- **Build frameworks**: AppKit, AVFoundation, CoreGraphics, **Carbon** (hotkeys), **Network** (auto-enter server), **-lsqlite3** (prompt stores)

## Data isolation from TVO (CRITICAL)

Both apps run independently and must NOT overwrite each other's prompt data:
- **UserDefaults**: separated automatically by bundle ID (`com.frank.ClaudeCodexVoiceOverlay`).
- **Local stores**: `~/Library/Application Support/ClaudeCodexVoiceOverlay/` (TVO uses its own folder).
- **Drive backup**: own filenames (`promptboard-backup-claudecodex.json`,
  `prompt-history-claudecodex.json`, `prompt-slots-claudecodex.json`) in the shared `appDataFolder`.

> To intentionally SHARE one prompt collection across both apps, revert these names/paths to TVO's.

## Sister Project (CRITICAL)

**TerminalVoiceOverlay-macOS** shares ~80% of the code (now the full feature set).
When changing ANY shared file, ALWAYS apply the same change to:
`~/proggs/TerminalVoiceOverlay-macOS/`

### Different files (this project vs Terminal):
- `TerminalController.swift`: Electron keystrokes (Cmd+A+Backspace) vs terminal (Ctrl+U).
  Same class name + API in both, only the keystrokes/comments differ.
- `AppWatcher.swift`: target bundle IDs (Claude Desktop/Codex vs terminals).
- Data paths/filenames are app-specific for isolation (see above).
- `Info.plist` / `.entitlements` / `build.sh` APP_NAME carry the CVO identity.

## Windows Counterpart

**ClaudeVoiceOverlay-Windows** — same functionality in C#/WPF.

| macOS (Swift) | Windows (C#) |
|---------------|-------------|
| InputController.swift | Services/AppController.cs |
| AppWatcher.swift | Services/AppWatcher.cs |
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
