# TerminalVoiceOverlay-Windows

Voice-to-text overlay for terminal applications (Windows Terminal, PowerShell, CMD) on Windows.
Press a hotkey → speak → text is typed into the active terminal.

## Build

### Development
```powershell
dotnet build
dotnet run
```

### Release (single .exe)
```powershell
pwsh -File publish.ps1
```
Creates a self-contained `TerminalVoiceOverlay.exe` in `./publish/`.
Place a `.env` file next to the .exe with your API keys.

### Auto-Rebuild after commit (CRITICAL)

After EVERY committed + pushed code change to this overlay, Claude Code runs the
rebuild+deploy ITSELF, automatically (Frank never starts it manually):

```powershell
pwsh -File ~/proggs/rebuild-overlay.ps1 TVO
```

This kills watcher + exe (by process NAME), builds `publish`, restarts via `watcher.vbs`,
and verifies the process count. Order: edit -> version bump -> commit -> push ->
**rebuild-overlay.ps1** -> status. If the change touches a file SHARED with
ClaudeVoiceOverlay, use `Both` instead of `TVO`.
(Source: memory `feedback_overlay_auto_rebuild_after_commit`.)

## Architecture

| File | Purpose |
|------|---------|
| `Services/Config.cs` | API keys from .env, configuration |
| `Services/AudioRecorder.cs` | NAudio microphone recording |
| `Services/GroqWhisperClient.cs` | Groq Whisper API for speech-to-text |
| `Services/GeminiClient.cs` | Gemini API for text correction/formatting |
| `Services/GeminiTranscribeClient.cs` | Gemini Live API (WebSocket) speech-to-text — `gemini-3.5-transcribe-live` only supports `bidiGenerateContent` |
| `Services/SpeechToTextRouter.cs` | Picks Groq vs Gemini per recording; Whisper hallucination guard stays Groq-only |
| `Services/TranscriptionEngineSetting.cs` | Engine switch, stored as `SK/VoiceOverlays/transcription-engine.txt` |
| `Services/TerminalController.cs` | Win32 keyboard simulation for terminals (keybd_event) |
| `Services/TerminalWatcher.cs` | SetWinEventHook for target app detection |
| `Views/OverlayWindow.xaml` | WPF overlay UI (XAML layout) |
| `Views/OverlayWindow.xaml.cs` | WPF overlay code-behind |
| `Models/RecordingState.cs` | Recording state enum |
| `NativeMethods/Win32.cs` | P/Invoke declarations for Win32 APIs |
| `App.xaml.cs` | App lifecycle, hotkey registration, orchestration |
| `app.manifest` | UAC and DPI settings |

## Key Patterns

- **Text insertion**: Clipboard + Ctrl+V via keybd_event/SendInput with clipboard save/restore
- **Line clearing**: Home + Shift+End + Delete (terminal-specific)
- **Target apps**: Windows Terminal, PowerShell, CMD (window class/title matching in TerminalWatcher.cs)
- **Thread safety**: async/await with Dispatcher.Invoke for UI thread
- **Retries**: Task.Delay (never Thread.Sleep on UI thread)
- **Clipboard restore**: Save clipboard before paste, restore after 500ms Task.Delay

## Sister Project (CRITICAL)

**ClaudeVoiceOverlay-Windows** shares ~80% of the code.
When changing ANY shared file, ALWAYS apply the same change to:
`ClaudeVoiceOverlay-Windows/`

### Shared files (nearly identical):
Config.cs, AudioRecorder.cs, GroqWhisperClient.cs, GeminiClient.cs,
App.xaml.cs, Views/, Models/, NativeMethods/

### Different files:
- `TerminalController.cs` (this project) vs `AppController.cs` (Claude)
- `TerminalWatcher.cs` (this project) vs `AppWatcher.cs` (Claude)
- Target apps: terminals vs Electron apps
- Key combos: Home+Shift+End+Del vs Ctrl+A+Backspace

## macOS Counterpart

**TerminalVoiceOverlay-macOS** — same functionality in Swift/AppKit.

| Windows (C#) | macOS (Swift) |
|-------------|---------------|
| Services/TerminalController.cs | TerminalController.swift |
| Services/TerminalWatcher.cs | AppWatcher.swift |
| Services/AudioRecorder.cs | AudioRecorder.swift |
| Services/GroqWhisperClient.cs | GroqWhisperClient.swift |
| Services/GeminiClient.cs | GeminiClient.swift |
| Services/Config.cs | Config.swift |
| Views/OverlayWindow.xaml + .cs | OverlayPanel.swift |
| App.xaml.cs | AppDelegate.swift |
| NativeMethods/Win32.cs | CGEvent (framework) |
| NAudio | AVAudioEngine (framework) |
| SetWinEventHook | NSWorkspace notifications |

### Porting changes from macOS to Windows:
1. Check which macOS file was changed → find the Windows equivalent in the table above
2. Swift patterns → C# equivalents:
   - `DispatchQueue.main.async { }` → `Dispatcher.Invoke(() => { })`
   - `DispatchQueue.asyncAfter(deadline:)` → `Task.Delay().ContinueWith()`
   - `guard let x = optional else { return }` → `if (x == null) return;`
   - `NSPasteboard.general` → `System.Windows.Clipboard`
   - `CGEvent` → `keybd_event` / `SendInput`
3. Build and test: `dotnet build && dotnet run`

## Requirements

- Windows 10/11, x64
- .NET 10.0 SDK (`dotnet --version`)
- NAudio NuGet package (restored automatically)
- .env file with GROQ_API_KEY (required), GEMINI_API_KEY (optional, text correction)
  and GEMINI_TRANSCRIBE_API_KEY / GEMINI_TRANSCRIBE_MODEL (optional, Gemini speech-to-text)
