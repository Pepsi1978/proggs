# ClaudeCodexVoiceOverlay-macOS

Voice-to-text overlay for Electron applications (Claude Desktop, Codex) on macOS.
Press a hotkey → speak → text is typed into the active Electron app.

Feature-complete port of `TerminalVoiceOverlay-macOS` (2026-06): PromptBoard,
prompt history, prompt slots, Gemini profiles 1–10, push-to-talk, waveform,
settings dialog, Google-Drive backup, orientation toggle, collapsed-pill
auto-hide, BTW button, copy/paste/screenshot, AutoEnter HTTP server, global
hotkeys. The ONLY differences from the terminal overlay are the target apps
(Electron instead of terminals) and the clear-input key combo.

## Build

**Always use `build.sh`** — never compile with `swiftc` directly.
`build.sh` handles certificate signing with "Frank Local Dev", which preserves TCC (accessibility) permissions across rebuilds.

```bash
bash build.sh
open build/ClaudeCodexVoiceOverlay.app
```

Direct `swiftc` results in ad-hoc signing → macOS revokes accessibility permissions on every rebuild.
The build links `Carbon` (global hotkeys), `Network` (AutoEnter HTTP server) and `sqlite3` (PromptBoard DB).

## Architecture

The codebase is a 1:1 sibling of `TerminalVoiceOverlay-macOS`. See that
project's `PORTING-INVENTORY.md` / `1-zu-1-PARITY-MANIFEST.md` for the full
component map. Key files:

| File | Purpose |
|------|---------|
| `Config.swift` | API keys from .env, configuration |
| `AudioRecorder.swift` | AVAudioEngine recording + `onLevel` for waveform |
| `GroqWhisperClient.swift` | Groq Whisper API for speech-to-text |
| `GeminiClient.swift` | Gemini API: correction profiles 1–10, title/slot-summary gen, prompt-engineer improve |
| `InputController.swift` | CGEvent keyboard simulation for Electron apps (**Cmd+A+Backspace** to clear) |
| `AppWatcher.swift` | NSWorkspace observer for Electron target apps (Claude Desktop, Codex) |
| `OverlayPanel.swift` + `OverlayExtraButtons` + `OverlayCollapsedMic` | Floating NSPanel overlay UI, orientation, collapsed pill |
| `OverlayOrientation` / `OverlayHorizontalLayout` / `OverlayGlideAnimation` | Layout + glide animation helpers |
| `WaveformView.swift` | 14-bar recording animation |
| `PushToTalkController.swift` | Hold-to-talk detection (NSEvent) |
| `HotkeyRegistry.swift` | Carbon global hotkeys (Cmd+Shift+R/S/I/E/O/C, Cmd+1..9, Cmd+Opt+A..Z) |
| `AutoHideController.swift` | Auto-collapse to mic pill |
| `AutoEnterStatusServer.swift` | 127.0.0.1:5723 HTTP for Stream Deck |
| `PromptBoardModels/Store`, `PromptHistoryStore`, `PromptSlotStore` | SQLite + JSON persistence |
| `PromptBoardPanel`, `PromptInputPanel`, `PromptHistoryPanel`, `PromptBoardDialogs`, `CommonDialogs`, `SettingsDialog` | PromptBoard UI |
| `AlwaysOnPrefixService.swift` | Pre/post always-on prompt chain |
| `GoogleDriveBackupService.swift` | appDataFolder backup/sync |
| `VoiceServiceProvider.swift` | Shared audio/STT/Gemini locator |
| `ErrorDescriptions.swift` | Extracted error description methods |
| `AppDelegate.swift` | App lifecycle, hotkey registration, orchestration (`tvoDebug` defined here) |
| `main.swift` | Entry point + single-instance guard |

## Key Patterns

- **Text insertion**: CGEvent keyboard simulation with clipboard save/restore via `InputController`
- **Field clearing**: Cmd+A + Backspace (Electron-specific, not Ctrl+U)
- **Target apps**: Claude Desktop, Codex (Electron bundle IDs in `AppWatcher.targetBundleIDs`)
- **Thread safety**: DispatchQueue.main.async for all UI updates; `InputController.sendQueue` serialises keystrokes
- **Retries**: DispatchQueue.asyncAfter (never Thread.sleep on main thread)
- **Code signing**: "Frank Local Dev" certificate (not ad-hoc)

## Data storage (independent from terminal overlay)

To stay collision-free with `TerminalVoiceOverlay-macOS`, ALL local data lives
under an app-specific directory:

- PromptBoard DB: `~/Library/Application Support/ClaudeCodexVoiceOverlay/promptboard.db`
- History: `~/Library/Application Support/ClaudeCodexVoiceOverlay/history/`
- Slots: `~/Library/Application Support/ClaudeCodexVoiceOverlay/slots/`

(The terminal overlay uses `PromptBoard/` for the DB and `TerminalVoiceOverlay/`
for history/slots.) To SHARE one prompt library across both overlays, point
these paths at the same directory in `PromptBoardStore`, `PromptHistoryStore`
and `PromptSlotStore`.

## Running alongside the terminal overlay

Both apps can run at once (the overlay only shows over its own target apps), but
two process-global resources collide if both run:
- **Global hotkeys** (Carbon): whichever app registers first wins; the other's
  identical hotkey silently fails.
- **AutoEnter HTTP port 5723**: the second binder fails (handled gracefully —
  logged, non-fatal; Stream Deck talks to whichever bound first).

## Sister Project (CRITICAL)

**TerminalVoiceOverlay-macOS** shares ~95% of the code.
When changing ANY shared file, ALWAYS apply the same change to:
`~/proggs/TerminalVoiceOverlay-macOS/`

### Different files:
- `InputController.swift` (this project) vs `TerminalController.swift` (Terminal)
- `AppWatcher.swift` target bundle IDs: Electron apps vs terminals
- Key combo: Cmd+A+Backspace (select all + delete) vs Ctrl+U (clear line)
- Data dirs: `ClaudeCodexVoiceOverlay/` vs `PromptBoard/` + `TerminalVoiceOverlay/`

## Windows Counterpart

**ClaudeVoiceOverlay-Windows** — same functionality in C#/WPF (currently the
older 5-button version; this macOS port is ahead).

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
