# ClaudeVoiceOverlay-Windows

Voice-to-text overlay for Electron applications (Claude Desktop, Codex) on Windows.
Press a hotkey → speak → text is typed into the active Electron app.

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
Creates a self-contained `ClaudeVoiceOverlay.exe` in `./publish/`.
Place a `.env` file next to the .exe with your API keys.

Note: `publish.ps1` auto-detects .NET SDK in user profile (`~/.dotnet`) or system PATH.

### Auto-Rebuild after commit (CRITICAL)

After EVERY committed + pushed code change to this overlay, Claude Code runs the
rebuild+deploy ITSELF, automatically (Frank never starts it manually):

```powershell
pwsh -File ~/proggs/rebuild-overlay.ps1 CVO
```

This kills watcher + exe (by process NAME), builds `publish`, restarts via `watcher.vbs`,
and verifies the process count. Order: edit -> version bump -> commit -> push ->
**rebuild-overlay.ps1** -> status. If the change touches a file SHARED with
TerminalVoiceOverlay, use `Both` instead of `CVO`.
(Source: memory `feedback_overlay_auto_rebuild_after_commit`.)

## Architecture

| File | Purpose |
|------|---------|
| `Services/Config.cs` | API keys from .env, configuration |
| `Services/AudioRecorder.cs` | NAudio microphone recording |
| `Services/GroqWhisperClient.cs` | Groq Whisper API for speech-to-text |
| `Services/GeminiBatchTranscribeClient.cs` | **Default Gemini path.** `gemini-3.5-transcribe` via the Interactions API (`POST /v1beta/interactions`), audio inline as base64 — no WebSocket, no VAD, no file upload. Measured 4,4 s vs 15,1 s for the live variant; WER 2,6 % vs 4,0 % (Groq 4,6 %). Mode `verbatim`, because `smart` drops words |
| `Services/GeminiTranscribeClient.cs` | Gemini Live API (WebSocket) speech-to-text, kept for comparison only |
| `Services/PersonalVocabulary.cs` | Shared word list from `SK/VoiceOverlays/personal-vocabulary.txt`, fed to Gemini as `customVocabulary` |
| `Services/SpeechToTextRouter.cs` | Picks Groq / Gemini / Gemini-Live per recording; falls back to Groq on any technical Gemini failure (quota, network) but NOT on `NoSpeechException` |
| `Services/TranscriptionEngineSetting.cs` | Engine switch (`groq` / `gemini` / `gemini-live`), stored as `SK/VoiceOverlays/transcription-engine.txt` |
| `Services/GeminiClient.cs` | Gemini API for text correction/formatting |
| `Services/AppController.cs` | Win32 keyboard simulation for Electron apps (keybd_event) |
| `Services/AppWatcher.cs` | SetWinEventHook for target app detection |
| `Views/OverlayWindow.xaml` | WPF overlay UI (XAML layout) |
| `Views/OverlayWindow.xaml.cs` | WPF overlay code-behind |
| `Models/RecordingState.cs` | Recording state enum |
| `NativeMethods/Win32.cs` | P/Invoke declarations for Win32 APIs |
| `App.xaml.cs` | App lifecycle, hotkey registration, orchestration |
| `app.manifest` | UAC and DPI settings |

## Key Patterns

- **Text insertion**: Clipboard + Ctrl+V via keybd_event/SendInput with clipboard save/restore
- **Field clearing**: Ctrl+A + Backspace (Electron-specific)
- **Target apps**: Claude Desktop, Codex (process name/window title matching in AppWatcher.cs)
- **Thread safety**: async/await with Dispatcher.Invoke for UI thread
- **Retries**: Task.Delay (never Thread.Sleep on UI thread)
- **Clipboard restore**: Save clipboard before paste, restore after 500ms Task.Delay

## Debugging text insertion: read the probes FIRST (Observability-First)

When text insertion fails (paste verpufft, wrong field, „only after a mouse click"),
**read the live probe log BEFORE attempting any fix** — do not guess at focus hypotheses.

- **Probe log:** `%LOCALAPPDATA%\ClaudeVoiceOverlay\diag.log` (JSON-Lines, written by `DiagLog`).
  Live tail: `Get-Content "$env:LOCALAPPDATA\ClaudeVoiceOverlay\diag.log" -Wait -Tail 30`.
- **`ctx:"CHECKPOINT"`** is the live-logic probe (`AppController.VerifyFocusCheckpoint`): it logs
  expected-vs-actual focus right before Ctrl+V. **`ok:false` = the paste will verpuffen** (focus is
  not on a real input field) — the single most useful line. `ok:true` everywhere = focus logic works.
- **`ctx:"UIA"`** shows which path found the field (Pfad1 FocusedElement / Pfad2/3
  `FindProseMirrorField`) and the element (`type=Edit` for Chat/Cowork, `type=Group name='Prompt'`
  for the Code tab — same `tiptap ProseMirror` field, different ControlType per ARIA role).
- The probes already proved two real bugs (2026-06-15): Cowork-after-task (focus on 'Fortschritt'
  button → FindFirst stuck on RootWebArea) and Code-tab (field is a Group, not Edit/Document).
  Full root-cause + fix: `bugs/desktop/windows-electron-text-injection.md` U10/U11.
- Workflow: read `diag.log` → form ONE hypothesis from real data → fix → reproduce → confirm the
  CHECKPOINT flips to `ok:true`. (Mirrors Direktive #3: messen, dann fixen.)

## Sister Project (CRITICAL)

**TerminalVoiceOverlay-Windows** shares ~80% of the code.
When changing ANY shared file, ALWAYS apply the same change to:
`TerminalVoiceOverlay-Windows/`

### Shared files (nearly identical):
Config.cs, AudioRecorder.cs, GroqWhisperClient.cs, GeminiClient.cs,
App.xaml.cs, Views/, Models/, NativeMethods/

### Different files:
- `AppController.cs` (this project) vs `TerminalController.cs` (Terminal)
- `AppWatcher.cs` (this project) vs `TerminalWatcher.cs` (Terminal)
- Target apps: Electron apps vs terminals
- Key combos: Ctrl+A+Backspace vs Home+Shift+End+Del

## macOS Counterpart

**ClaudeCodexVoiceOverlay-macOS** — same functionality in Swift/AppKit.

| Windows (C#) | macOS (Swift) |
|-------------|---------------|
| Services/AppController.cs | InputController.swift |
| Services/AppWatcher.cs | AppWatcher.swift |
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
- .env file with GROQ_API_KEY (required) and GEMINI_API_KEY (optional)
