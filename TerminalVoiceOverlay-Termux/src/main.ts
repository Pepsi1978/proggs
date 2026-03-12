import { loadConfig } from "./config.js";
import { AudioRecorder } from "./audio-recorder.js";
import { GroqWhisperClient } from "./groq-client.js";
import { GeminiClient } from "./gemini-client.js";
import { TerminalController } from "./terminal-controller.js";
import { renderOverlay, initUI, cleanupUI, type MicState, type OverlayState } from "./ui.js";

// ── State ──────────────────────────────────────────────────────────────────

interface AppState {
  micState: MicState;
  btwMicState: MicState;
  geminiEnabled: boolean;
  autoEnterEnabled: boolean;
  lastText: string;
  rawText: string;
  recordingSeconds: number;
  xCooldown: boolean;
  activeMic: "main" | "btw" | null;
}

const state: AppState = {
  micState: "idle",
  btwMicState: "idle",
  geminiEnabled: true,
  autoEnterEnabled: false,
  lastText: "",
  rawText: "",
  recordingSeconds: 0,
  xCooldown: false,
  activeMic: null,
};

// ── Instances ──────────────────────────────────────────────────────────────

const recorder = new AudioRecorder();
let groqClient: GroqWhisperClient;
let geminiClient: GeminiClient | null = null;

// ── Timers ─────────────────────────────────────────────────────────────────

let recordingTimer: ReturnType<typeof setInterval> | null = null;
let pulseTimer: ReturnType<typeof setInterval> | null = null;

function startTimers(): void {
  state.recordingSeconds = 0;

  recordingTimer = setInterval(() => {
    state.recordingSeconds += 1;
    render();
  }, 1000);

  pulseTimer = setInterval(() => {
    render();
  }, 500);
}

function stopTimers(): void {
  if (recordingTimer !== null) {
    clearInterval(recordingTimer);
    recordingTimer = null;
  }
  if (pulseTimer !== null) {
    clearInterval(pulseTimer);
    pulseTimer = null;
  }
}

// ── Render ─────────────────────────────────────────────────────────────────

function render(): void {
  const overlayState: OverlayState = {
    micState: state.micState,
    btwMicState: state.btwMicState,
    geminiEnabled: state.geminiEnabled,
    autoEnterEnabled: state.autoEnterEnabled,
    lastText: state.lastText,
    rawText: state.rawText,
    recordingSeconds: state.recordingSeconds,
    xCooldown: state.xCooldown,
    activeMic: state.activeMic,
  };
  renderOverlay(overlayState);
}

// ── Reset helper ──────────────────────────────────────────────────────────

function resetAfterDelay(which: "main" | "btw"): void {
  setTimeout(() => {
    if (which === "main") {
      if (state.micState === "success" || state.micState === "error") {
        state.micState = "idle";
        render();
      }
    } else {
      if (state.btwMicState === "success" || state.btwMicState === "error") {
        state.btwMicState = "idle";
        render();
      }
    }
  }, 2000);
}

// ── Aufnahme-Logik ────────────────────────────────────────────────────────

function startRecording(which: "main" | "btw"): void {
  // Verhindere Doppelaufnahme
  if (state.activeMic !== null) return;

  state.activeMic = which;
  state.recordingSeconds = 0;
  state.lastText = "";
  state.rawText = "";

  if (which === "main") {
    state.micState = "recording";
  } else {
    state.btwMicState = "recording";
  }

  render();
  TerminalController.vibrate(100);
  recorder.start();
  startTimers();
}

async function stopRecording(which: "main" | "btw"): Promise<void> {
  stopTimers();

  if (which === "main") {
    state.micState = "processing";
  } else {
    state.btwMicState = "processing";
  }
  state.activeMic = null;
  render();

  const filePath = recorder.stop();
  if (!filePath) {
    const errMsg = "Keine Aufnahme-Datei gefunden";
    state.lastText = errMsg;
    if (which === "main") {
      state.micState = "error";
    } else {
      state.btwMicState = "error";
    }
    render();
    resetAfterDelay(which);
    return;
  }

  try {
    // Transkription via Groq Whisper
    const rawTranscript = await groqClient.transcribe(filePath);

    if (!rawTranscript || !rawTranscript.trim()) {
      const errMsg = "Kein Text erkannt";
      state.lastText = errMsg;
      if (which === "main") {
        state.micState = "error";
      } else {
        state.btwMicState = "error";
      }
      render();
      resetAfterDelay(which);
      return;
    }

    state.rawText = rawTranscript.trim();
    let finalText = state.rawText;

    // Optionale Gemini-Korrektur
    if (state.geminiEnabled && geminiClient) {
      try {
        const corrected = await geminiClient.correctText(finalText);
        finalText = corrected;
      } catch {
        // Fallback auf Roh-Transkript
      }
    }

    // BTW-Prefix hinzufügen
    if (which === "btw") {
      finalText = `/btw ${finalText}`;
    }

    state.lastText = finalText;

    // Text ins Clipboard
    TerminalController.setClipboard(finalText);
    TerminalController.toast(finalText.length > 60 ? `${finalText.slice(0, 60)}…` : finalText);

    if (which === "main") {
      state.micState = "success";
    } else {
      state.btwMicState = "success";
    }

    TerminalController.vibrate(200);
    render();
    resetAfterDelay(which);
  } catch (err) {
    const errMsg = `Fehler: ${err instanceof Error ? err.message : String(err)}`;
    state.lastText = errMsg;
    if (which === "main") {
      state.micState = "error";
    } else {
      state.btwMicState = "error";
    }
    render();
    resetAfterDelay(which);
  } finally {
    if (filePath) {
      recorder.cleanup(filePath);
    }
  }
}

async function toggleRecording(which: "main" | "btw"): Promise<void> {
  const currentState = which === "main" ? state.micState : state.btwMicState;

  // Verarbeitung läuft — nichts tun
  if (currentState === "processing") return;

  if (currentState === "recording") {
    // Aufnahme stoppen
    await stopRecording(which);
  } else {
    // Nur starten wenn keine andere Aufnahme läuft
    if (state.activeMic !== null) {
      TerminalController.toast("Aufnahme läuft bereits");
      return;
    }
    startRecording(which);
  }
}

// ── Tasten-Handler ────────────────────────────────────────────────────────

function handleClearLine(): void {
  // Cooldown aktiv — ignorieren
  if (state.xCooldown) return;

  if (state.activeMic !== null) {
    // Laufende Aufnahme abbrechen
    stopTimers();
    const filePath = recorder.stop();
    if (filePath) recorder.cleanup(filePath);

    if (state.activeMic === "main") {
      state.micState = "idle";
    } else {
      state.btwMicState = "idle";
    }
    state.activeMic = null;
    state.lastText = "";
    state.rawText = "";
    render();
    TerminalController.toast("Aufnahme abgebrochen");
  } else if (state.micState === "processing" || state.btwMicState === "processing") {
    TerminalController.toast("Verarbeitung laeuft...");
    return;
  } else {
    // Clipboard leeren
    TerminalController.setClipboard("");
    state.lastText = "";
    state.rawText = "";
    render();
    TerminalController.toast("Zwischenablage geloescht");
  }

  // 2s Cooldown setzen
  state.xCooldown = true;
  render();
  setTimeout(() => {
    state.xCooldown = false;
    render();
  }, 2000);
}

function handleWhisperUndo(): void {
  if (!state.rawText) {
    TerminalController.toast("Kein Whisper-Text zum Wiederherstellen");
    return;
  }

  // Gemini-korrigierten Text durch Roh-Whisper-Text ersetzen
  const restoredText =
    state.btwMicState === "success" && state.lastText.startsWith("/btw ")
      ? `/btw ${state.rawText}`
      : state.rawText;

  state.lastText = restoredText;
  TerminalController.setClipboard(restoredText);
  TerminalController.toast("Whisper-Rohtext wiederhergestellt");
  render();
}

function handleGeminiToggle(): void {
  if (!geminiClient) {
    TerminalController.toast("Kein Gemini API-Key konfiguriert");
    return;
  }
  state.geminiEnabled = !state.geminiEnabled;
  render();
  TerminalController.toast(`Gemini ${state.geminiEnabled ? "AN" : "AUS"}`);
}

function handleAutoEnterToggle(): void {
  state.autoEnterEnabled = !state.autoEnterEnabled;
  render();
  TerminalController.toast(`Auto-Enter ${state.autoEnterEnabled ? "AN" : "AUS"}`);
}

// ── Keyboard Input ────────────────────────────────────────────────────────

function setupKeyboardInput(): void {
  if (!process.stdin.isTTY) {
    console.error(
      "Fehler: Kein TTY verfuegbar. Starten Sie das Overlay in einem interaktiven Terminal."
    );
    process.exit(1);
  }

  process.stdin.setRawMode(true);
  process.stdin.resume();
  process.stdin.setEncoding("utf-8");

  process.stdin.on("data", (key: string) => {
    const k = key.toLowerCase();

    switch (k) {
      case "m":
        toggleRecording("main").catch((err) => {
          state.micState = "error";
          state.lastText = `Fehler: ${err instanceof Error ? err.message : String(err)}`;
          state.activeMic = null;
          stopTimers();
          render();
          resetAfterDelay("main");
        });
        break;

      case "b":
        toggleRecording("btw").catch((err) => {
          state.btwMicState = "error";
          state.lastText = `Fehler: ${err instanceof Error ? err.message : String(err)}`;
          state.activeMic = null;
          stopTimers();
          render();
          resetAfterDelay("btw");
        });
        break;

      case "x":
        handleClearLine();
        break;

      case "w":
        handleWhisperUndo();
        break;

      case "g":
        handleGeminiToggle();
        break;

      case "e":
        handleAutoEnterToggle();
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

// ── Lifecycle ─────────────────────────────────────────────────────────────

function shutdown(): void {
  // Laufende Aufnahme stoppen
  if (state.activeMic !== null || recorder.isRecording) {
    stopTimers();
    try {
      const filePath = recorder.stop();
      if (filePath) recorder.cleanup(filePath);
    } catch {
      // Ignore errors during shutdown
    }
  }

  // Timer sicherheitshalber nochmals clearen
  stopTimers();

  // Cursor wieder einblenden + Terminal zurücksetzen
  cleanupUI();

  console.log("Voice Overlay beendet.");
  process.exit(0);
}

function main(): void {
  // TTY-Check vor allem anderen
  if (!process.stdin.isTTY) {
    console.error(
      "Fehler: Kein TTY verfuegbar. Starten Sie das Overlay in einem interaktiven Terminal."
    );
    process.exit(1);
  }

  try {
    const config = loadConfig();

    groqClient = new GroqWhisperClient(
      config.groqApiKey,
      config.whisperModel,
      config.whisperLang
    );

    if (config.geminiApiKey) {
      geminiClient = new GeminiClient(config.geminiApiKey, config.geminiModel);
      state.geminiEnabled = true;
    } else {
      geminiClient = null;
      state.geminiEnabled = false;
    }

    // UI initialisieren (Cursor verstecken, Bildschirm vorbereiten)
    initUI();

    render();

    setupKeyboardInput();

    // Graceful-Shutdown-Handler
    process.on("SIGINT", shutdown);
    process.on("SIGTERM", shutdown);
    process.on("exit", () => {
      // Cursor sicherstellen (falls shutdown() nicht aufgerufen wurde)
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
