# TerminalVoiceOverlay-Android

Floating voice-to-text overlay for Android/Termux. Native Android app built with .NET Android (C#).

## Build

### Debug
```
dotnet build
```

### Release APK
```
dotnet publish -c Release -f net10.0-android
```
APK at: `bin/Release/net10.0-android/publish/com.pepsi1978.terminalvoiceoverlay-Signed.apk`

Environment variables needed:
```
JAVA_HOME=C:/Program Files (x86)/Android/openjdk/jdk-17.0.14
ANDROID_HOME=C:/Program Files (x86)/Android/android-sdk
```

## Architecture

| File | Purpose |
|------|---------|
| `OverlayService.cs` | Core: ForegroundService with floating overlay window (TYPE_APPLICATION_OVERLAY) |
| `MainActivity.cs` | Settings screen, permissions, service lifecycle |
| `Services/AudioRecorder.cs` | Android AudioRecord → WAV (16kHz mono PCM) |
| `Services/GroqWhisperClient.cs` | Groq Whisper API for speech-to-text |
| `Services/GeminiClient.cs` | Gemini API for text correction |
| `Services/Config.cs` | SharedPreferences config (Load/Save/HasGroqKey) |
| `Models/RecordingState.cs` | Recording state enum |

## Key Patterns

- **Overlay**: WindowManager.AddView with TYPE_APPLICATION_OVERLAY + FLAG_NOT_FOCUSABLE
- **Button colors**: Set programmatically via GradientDrawable (not XML selectors)
- **Pulse animation**: Handler.PostDelayed(500ms) toggling between two colors
- **Clipboard**: ClipboardManager.PrimaryClip = ClipData.NewPlainText(...)
- **Auto-Enter**: Appends "\n" to clipboard text (Termux interprets as Enter when pasted)
- **Audio feedback**: Vibrator instead of Console.Beep (mobile has no speaker beep API)
- **Threading**: async/await + Handler for UI thread operations
- **Service lifecycle**: StartForeground with persistent notification, FOREGROUND_SERVICE_TYPE_MICROPHONE

## Sister Projects (CRITICAL)

This shares ~80% logic with the desktop versions. When changing API client logic, apply the same changes to:
- `TerminalVoiceOverlay-Windows/` (C#/WPF)
- `ClaudeVoiceOverlay-Windows/` (C#/WPF)
- `TerminalVoiceOverlay-macOS/` (Swift/AppKit)
- `ClaudeCodexVoiceOverlay-macOS/` (Swift/AppKit)
- `TerminalVoiceOverlay-Termux/` (TypeScript CLI)

### Shared logic (nearly identical across platforms):
Config, GroqWhisperClient, GeminiClient, RecordingState, button state machine

### Android-specific:
AudioRecorder (Android AudioRecord vs NAudio/AVAudioEngine), OverlayService (vs WPF Window/NSPanel), MainActivity (vs App.xaml.cs/AppDelegate)

## .NET Android Gotchas

- `Stream` is ambiguous: use `System.IO.Stream` explicitly (conflicts with `Android.Media.Stream`)
- `Uri` is ambiguous: use `Android.Net.Uri` explicitly (conflicts with `System.Uri`)
- `ClipboardManager.PrimaryClip = ...` not `.SetPrimaryClip(...)` (property, not method)
- Android enums (ChannelIn, Encoding) don't implicitly convert to int
- ForegroundService.TypeMicrophone requires API 30+, guarded by version check
