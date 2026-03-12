# Terminal Voice Overlay - Termux

## Architecture
- TypeScript/Node.js terminal app for Android Termux
- Port of the macOS TerminalVoiceOverlay (Swift/AppKit) to terminal-based ANSI UI
- Keyboard-driven (stdin raw mode) instead of mouse-driven GUI

## Modules
- `src/main.ts` - Main orchestration, state management, keyboard input
- `src/config.ts` - .env file parser with multiple search paths
- `src/audio-recorder.ts` - termux-microphone-record wrapper (16kHz mono WAV)
- `src/groq-client.ts` - Groq Whisper API client (multipart upload, retry logic)
- `src/gemini-client.ts` - Gemini API client (text correction with German system prompt)
- `src/terminal-controller.ts` - Termux API wrappers (clipboard, notification, vibrate, toast)
- `src/ui.ts` - ANSI terminal UI renderer

## Flow
1. User presses `m` → starts recording via termux-microphone-record
2. User presses `m` again → stops recording, sends WAV to Groq Whisper
3. If Gemini enabled → corrects transcription (fixes tech terms, grammar)
4. Result copied to clipboard via termux-clipboard-set
5. Notification + toast shown to user

## Build
- `bash build.sh` compiles TypeScript to `dist/`
- Uses `node` directly to run tsc (avoids /usr/bin/env shebang issue on Android)
- Requires: `@types/node` (dev dependency)

## Key bindings
- m: toggle mic, x: clear/cancel, w: whisper undo, g: gemini toggle, e: auto-enter toggle, q: quit

## .env file with GROQ_API_KEY (required) and GEMINI_API_KEY (optional)
