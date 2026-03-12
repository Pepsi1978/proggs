import { loadConfig } from "./config.js";
import { AudioRecorder } from "./audio-recorder.js";
import { GroqWhisperClient } from "./groq-client.js";
import { GeminiClient } from "./gemini-client.js";
import { TerminalController } from "./terminal-controller.js";
import { clearScreen, renderOverlay, type OverlayState, type MicState } from "./ui.js";

// ── State ──────────────────────────────────────────────
let micState: MicState = "idle";
let geminiEnabled = true;
let autoEnterEnabled = false;
let hasPastedText = false;
let lastRawTranscript = "";
let lastText = "";

// ── Instances ──────────────────────────────────────────
const recorder = new AudioRecorder();
let groqClient: GroqWhisperClient;
let geminiClient: GeminiClient | null = null;

// ── Render ─────────────────────────────────────────────
function render(): void {
  const state: OverlayState = {
    micState,
    geminiEnabled,
    autoEnterEnabled,
    lastText,
  };
  renderOverlay(state);
}

// ── Actions ────────────────────────────────────────────

async function toggleRecording(): Promise<void> {
  if (micState === "processing") return;

  if (recorder.isRecording) {
    await stopRecording();
  } else {
    startRecording();
  }
}

function startRecording(): void {
  hasPastedText = false;
  lastRawTranscript = "";
  lastText = "";
  micState = "recording";
  render();

  TerminalController.vibrate(100);
  recorder.start();
}

async function stopRecording(): Promise<void> {
  micState = "processing";
  render();

  const filePath = recorder.stop();
  if (!filePath) {
    micState = "error";
    lastText = "Keine Aufnahme-Datei gefunden";
    render();
    resetAfterDelay();
    return;
  }

  try {
    // 1) Transcribe with Groq Whisper
    const rawText = await groqClient.transcribe(filePath);

    if (!rawText || !rawText.trim()) {
      micState = "error";
      lastText = "Kein Text erkannt";
      render();
      resetAfterDelay();
      return;
    }

    lastRawTranscript = rawText.trim();
    let finalText = lastRawTranscript;

    // 2) Optional Gemini correction
    if (geminiEnabled && geminiClient) {
      try {
        finalText = await geminiClient.correctText(finalText);
      } catch {
        // Fallback to raw transcript on Gemini error
      }
    }

    // 3) Insert text
    lastText = finalText;
    insertText(finalText);

    micState = "success";
    TerminalController.vibrate(200);
    render();
    resetAfterDelay();
  } catch (err) {
    micState = "error";
    lastText = `Fehler: ${err instanceof Error ? err.message : String(err)}`;
    render();
    resetAfterDelay();
  } finally {
    recorder.cleanup(filePath);
  }
}

function insertText(text: string): void {
  TerminalController.setClipboard(text);
  hasPastedText = true;
  TerminalController.toast("Text in Zwischenablage kopiert");
  TerminalController.notify("Voice Overlay", text);
}

function clearLine(): void {
  if (recorder.isRecording) {
    const filePath = recorder.stop();
    if (filePath) recorder.cleanup(filePath);
    micState = "idle";
    lastText = "";
    hasPastedText = false;
    lastRawTranscript = "";
    render();
    TerminalController.toast("Aufnahme abgebrochen");
    return;
  }
  if (micState === "processing") {
    // Don't interfere while API calls are running
    TerminalController.toast("Verarbeitung laeuft...");
    return;
  }
  // Clear clipboard
  TerminalController.setClipboard("");
  lastText = "";
  hasPastedText = false;
  lastRawTranscript = "";
  render();
  TerminalController.toast("Zwischenablage geloescht");
}

function whisperUndo(): void {
  if (!lastRawTranscript) {
    TerminalController.toast("Kein Whisper-Text zum Wiederherstellen");
    return;
  }
  // Replace Gemini-corrected text with raw Whisper transcript
  lastText = lastRawTranscript;
  insertText(lastRawTranscript);
  TerminalController.toast("Whisper-Rohtext wiederhergestellt");
  render();
}

function toggleGemini(): void {
  geminiEnabled = !geminiEnabled;
  render();
  TerminalController.toast(`Gemini ${geminiEnabled ? "AN" : "AUS"}`);
}

function toggleAutoEnter(): void {
  autoEnterEnabled = !autoEnterEnabled;
  render();
  TerminalController.toast(`Auto-Enter ${autoEnterEnabled ? "AN" : "AUS"}`);
}

function resetAfterDelay(): void {
  setTimeout(() => {
    if (micState === "success" || micState === "error") {
      micState = "idle";
      render();
    }
  }, 3000);
}

// ── Keyboard Input ─────────────────────────────────────

function setupKeyboardInput(): void {
  if (!process.stdin.isTTY) {
    console.error("Fehler: Kein TTY verfuegbar. Starten Sie das Overlay in einem interaktiven Terminal.");
    process.exit(1);
  }

  process.stdin.setRawMode(true);
  process.stdin.resume();
  process.stdin.setEncoding("utf-8");

  process.stdin.on("data", (key: string) => {
    const k = key.toLowerCase();

    switch (k) {
      case "m":
        toggleRecording().catch((err) => {
          micState = "error";
          lastText = `Fehler: ${err instanceof Error ? err.message : String(err)}`;
          render();
          resetAfterDelay();
        });
        break;

      case "x":
        clearLine();
        break;

      case "w":
        whisperUndo();
        break;

      case "g":
        toggleGemini();
        break;

      case "e":
        toggleAutoEnter();
        break;

      case "q":
      case "\x03": // Ctrl+C
        shutdown();
        break;

      default:
        break;
    }
  });
}

// ── Lifecycle ──────────────────────────────────────────

function shutdown(): void {
  if (recorder.isRecording) {
    recorder.stop();
  }
  process.stdout.write("\x1b[?25h"); // Show cursor
  clearScreen();
  console.log("Voice Overlay beendet.");
  process.exit(0);
}

function main(): void {
  try {
    const config = loadConfig();

    groqClient = new GroqWhisperClient(
      config.groqApiKey,
      config.whisperModel,
      config.whisperLang
    );

    if (config.geminiApiKey) {
      geminiClient = new GeminiClient(config.geminiApiKey, config.geminiModel);
    } else {
      geminiEnabled = false;
    }

    // Hide cursor for cleaner UI
    process.stdout.write("\x1b[?25l");

    clearScreen();
    render();

    setupKeyboardInput();

    // Graceful shutdown handlers
    process.on("SIGINT", shutdown);
    process.on("SIGTERM", shutdown);
    process.on("exit", () => {
      process.stdout.write("\x1b[?25h");
    });
  } catch (err) {
    console.error(
      `Fehler beim Starten: ${err instanceof Error ? err.message : String(err)}`
    );
    process.exit(1);
  }
}

main();
