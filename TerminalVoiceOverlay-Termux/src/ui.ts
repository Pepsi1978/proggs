// ─────────────────────────────────────────────────────────────────────────────
// Terminal Voice Overlay – UI Renderer
// Replicates the macOS capsule-shaped floating overlay in the terminal.
// Uses 256-color ANSI codes (38;5;N / 48;5;N) for precise color matching.
// ─────────────────────────────────────────────────────────────────────────────

// ── Types ──────────────────────────────────────────────────────────────────

export type MicState = "idle" | "recording" | "processing" | "success" | "error";

export interface OverlayState {
  micState: MicState;
  btwMicState?: MicState;
  geminiEnabled: boolean;
  autoEnterEnabled: boolean;
  lastText: string;
  rawText?: string;
  recordingSeconds?: number;
  xCooldown?: boolean;
  activeMic?: "main" | "btw" | null;
}

// ── ANSI helpers ───────────────────────────────────────────────────────────

const ESC = "\x1b[";

/** Reset all attributes */
const RST = "\x1b[0m";

/** Bold */
const BLD = "\x1b[1m";

/** 256-color foreground */
function fg(n: number): string {
  return `${ESC}38;5;${n}m`;
}

/** 256-color background */
function bg(n: number): string {
  return `${ESC}48;5;${n}m`;
}

/** Standard foreground (16-color) */
function fgS(code: number): string {
  return `${ESC}${code}m`;
}

/** Standard background (16-color) */
function bgS(code: number): string {
  return `${ESC}${code}m`;
}

// ── Palette (matched to macOS overlay) ────────────────────────────────────
//
// Color mapping notes:
//   #E53935 (red)        → 256-color 160 (bright red)
//   #64B5F6 (light blue) → 256-color 111 (light cornflower blue)
//   #2A5DA8 (dark blue)  → 256-color 25  (steel blue)
//   #2D2D2D (dark gray)  → 256-color 235 (very dark gray)
//   #43A047 (green)      → 256-color 70  (medium green)
//   #FF9800 (orange)     → 256-color 214 (orange)
//   #FF5252 (light red/pulse) → 256-color 203 (salmon red)
//   White text           → 256-color 231 (pure white)
//   Capsule border       → 256-color 240 (mid gray)
//   Capsule background   → 256-color 233 (almost black, dark gray)

const PAL = {
  // Button BG colors
  bgRed:       bg(160),   // #E53935 – X button, error, recording
  bgRedPulse:  bg(203),   // #FF5252 – recording pulse (lighter red)
  bgBlueLt:    bg(111),   // #64B5F6 – BTW MIC idle
  bgBlueDk:    bg(25),    // #2A5DA8 – Main MIC idle
  bgGray:      bg(235),   // #2D2D2D – inactive toggles, W button
  bgGreen:     bg(70),    // #43A047 – success, Gemini ON
  bgOrange:    bg(214),   // #FF9800 – processing, Auto-Enter ON
  bgProcessing:bg(214),   // same as orange

  // Text colors
  fgWhite:     fg(231),   // button labels
  fgGray:      fg(240),   // border / dim text
  fgGrayDim:   fg(238),   // very dim
  fgRed:       fg(160),
  fgGreen:     fg(70),
  fgOrange:    fg(214),
  fgCyan:      fg(111),
  fgBlueLt:    fg(111),
  fgBlueDk:    fg(25),

  // Capsule shell
  bgCapsule:   bg(233),   // capsule background
  fgBorder:    fg(240),   // border characters
};

// ── Pulse animation state ──────────────────────────────────────────────────

let _pulseOn = false;         // toggled every 500 ms while recording
let _pulseTimer: ReturnType<typeof setInterval> | null = null;
let _pulseCallback: (() => void) | null = null;

function startPulse(onTick: () => void): void {
  if (_pulseTimer !== null) return;
  _pulseCallback = onTick;
  _pulseTimer = setInterval(() => {
    _pulseOn = !_pulseOn;
    if (_pulseCallback) _pulseCallback();
  }, 500);
}

function stopPulse(): void {
  if (_pulseTimer !== null) {
    clearInterval(_pulseTimer);
    _pulseTimer = null;
  }
  _pulseOn = false;
  _pulseCallback = null;
}

// ── Public lifecycle ───────────────────────────────────────────────────────

/** Called once on startup – hides cursor, clears screen. */
export function initUI(): void {
  process.stdout.write("\x1b[?25l"); // hide cursor
  process.stdout.write("\x1b[2J\x1b[H");
}

/** Called on shutdown – restores cursor. */
export function cleanupUI(): void {
  stopPulse();
  process.stdout.write("\x1b[?25h"); // show cursor
  process.stdout.write("\x1b[2J\x1b[H");
}

/** Compatibility alias – used by main.ts */
export function clearScreen(): void {
  process.stdout.write("\x1b[2J\x1b[H");
}

// ── Button BG resolver ────────────────────────────────────────────────────

function getMicBg(
  state: MicState,
  idleBg: string,
  isActive: boolean
): string {
  switch (state) {
    case "recording":
      // Pulse between red and lighter red
      return isActive
        ? (_pulseOn ? PAL.bgRedPulse : PAL.bgRed)
        : PAL.bgRed;
    case "processing":
      return PAL.bgOrange;
    case "success":
      return PAL.bgGreen;
    case "error":
      return PAL.bgRed;
    default:
      return idleBg;
  }
}

// ── Button renderer ───────────────────────────────────────────────────────

/**
 * Render a single button cell.
 * Width is fixed at 7 visible characters: "  LBL  "
 * Returns the ANSI-escaped string (does NOT include newline).
 */
function btn(bgColor: string, label: string): string {
  // Pad label to 3 chars (visible width)
  const padded = label.padStart(3).padEnd(3);
  return `${bgColor}${PAL.fgWhite}${BLD} ${padded} ${RST}`;
}

// ── Word wrap ─────────────────────────────────────────────────────────────

function wordWrap(text: string, maxWidth: number): string[] {
  if (!text) return [];
  const words = text.split(" ");
  const lines: string[] = [];
  let current = "";
  for (const word of words) {
    if (current.length + (current ? 1 : 0) + word.length > maxWidth) {
      if (current) lines.push(current);
      current = word;
    } else {
      current = current ? `${current} ${word}` : word;
    }
  }
  if (current) lines.push(current);
  return lines;
}

// ── Time formatter ────────────────────────────────────────────────────────

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60).toString().padStart(2, "0");
  const s = (seconds % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
}

// ── Capsule line builder ───────────────────────────────────────────────────

// Fixed inner width of the capsule (between │ characters): 12 chars
const INNER = 12;

/** Top border: ╭──────────────╮ */
function capTop(): string {
  return `${PAL.fgBorder}╭${"─".repeat(INNER)}╮${RST}`;
}

/** Bottom border: ╰──────────────╯ */
function capBot(): string {
  return `${PAL.fgBorder}╰${"─".repeat(INNER)}╯${RST}`;
}

/**
 * A button row inside the capsule.
 * Content is centered inside the inner width.
 * Format: │  [BG LABEL ]  │
 */
function capRow(content: string, visibleContentWidth: number): string {
  const totalPad = INNER - visibleContentWidth;
  const left = Math.floor(totalPad / 2);
  const right = totalPad - left;
  return (
    `${PAL.fgBorder}│${RST}` +
    " ".repeat(left) +
    content +
    " ".repeat(right) +
    `${PAL.fgBorder}│${RST}`
  );
}

/**
 * A plain text row – centered, no background.
 * Visible text width must be provided (no ANSI codes counted).
 */
function capText(text: string, visibleWidth: number): string {
  return capRow(text, visibleWidth);
}

// ── Main render ───────────────────────────────────────────────────────────

export function renderOverlay(state: OverlayState): void {
  const micState         = state.micState;
  const btwMicState      = state.btwMicState      ?? "idle";
  const geminiEnabled    = state.geminiEnabled;
  const autoEnterEnabled = state.autoEnterEnabled;
  const lastText         = state.lastText;
  const recordingSeconds = state.recordingSeconds ?? 0;
  const xCooldown        = state.xCooldown        ?? false;
  const activeMic        = state.activeMic        ?? null;

  // Manage pulse timer
  const anyRecording = micState === "recording" || btwMicState === "recording";
  if (anyRecording && _pulseTimer === null) {
    startPulse(() => {
      // Re-render on pulse tick – we need a render callback; stored separately
      renderOverlay(state);
    });
  } else if (!anyRecording && _pulseTimer !== null) {
    stopPulse();
  }

  // ── Button colors ────────────────────────────────────────────────────────

  // X button: red normally, gray during cooldown
  const xBg = xCooldown ? PAL.bgGray : PAL.bgRed;

  // BTW MIC button
  const btwBg = getMicBg(btwMicState, PAL.bgBlueLt, activeMic === "btw");

  // Main MIC button
  const mainBg = getMicBg(micState, PAL.bgBlueDk, activeMic === "main");

  // W button: always gray
  const wBg = PAL.bgGray;

  // G toggle
  const gBg = geminiEnabled ? PAL.bgGreen : PAL.bgGray;

  // ⏎ toggle
  const eBg = autoEnterEnabled ? PAL.bgOrange : PAL.bgGray;

  // ── Button labels ────────────────────────────────────────────────────────

  // BTW MIC label: show state symbol
  function micLabel(s: MicState): string {
    switch (s) {
      case "recording":  return "REC";
      case "processing": return "...";
      case "success":    return " ✓ ";
      case "error":      return " ! ";
      default:           return "🎙 ";
    }
  }

  const xLabel    = " X ";
  const btwLabel  = micLabel(btwMicState);
  const mainLabel = micLabel(micState);
  const wLabel    = " W ";
  const gLabel    = geminiEnabled  ? " G●" : " G ";
  const eLabel    = autoEnterEnabled ? "⏎●" : " ⏎ ";

  // ── Status line ──────────────────────────────────────────────────────────

  let statusText: string;
  let statusColor: string;

  if (activeMic === "main" && micState === "recording") {
    statusText  = `● REC ${formatTime(recordingSeconds)}`;
    statusColor = PAL.fgRed;
  } else if (activeMic === "btw" && btwMicState === "recording") {
    statusText  = `● BTW ${formatTime(recordingSeconds)}`;
    statusColor = PAL.fgBlueLt;
  } else if (micState === "processing" || btwMicState === "processing") {
    statusText  = "Verarbeite...";
    statusColor = PAL.fgOrange;
  } else if (micState === "success" || btwMicState === "success") {
    statusText  = "✓ Fertig!";
    statusColor = PAL.fgGreen;
  } else if (micState === "error" || btwMicState === "error") {
    statusText  = "✗ Fehler";
    statusColor = PAL.fgRed;
  } else {
    statusText  = "Bereit";
    statusColor = PAL.fgGray;
  }

  // ── Terminal width for last-text wrap ────────────────────────────────────

  const termWidth = (process.stdout.columns ?? 80) - 4;

  // ── Assemble output ──────────────────────────────────────────────────────

  const out: string[] = [];

  // Clear screen and move to top-left
  out.push("\x1b[2J\x1b[H");

  // Empty line for breathing room
  out.push("");

  // Capsule top
  out.push("  " + capTop());

  // ── Row 1: X button ──
  // Button visible width = 5 (" X " = 3 + 2 spaces from btn())
  // Actually btn() wraps label in " LBL " = 5 chars
  out.push("  " + capRow(btn(xBg, xLabel), 5));

  // ── Row 2: BTW MIC button ──
  out.push("  " + capRow(btn(btwBg,  btwLabel), 5));

  // ── Row 3: Main MIC button ──
  out.push("  " + capRow(btn(mainBg, mainLabel), 5));

  // ── Row 4: W button ──
  out.push("  " + capRow(btn(wBg, wLabel), 5));

  // ── Row 5: G toggle ──
  out.push("  " + capRow(btn(gBg, gLabel), 5));

  // ── Row 6: ⏎ toggle ──
  // ⏎ is a multi-byte char but displays as 1 column; label stays 5 visible
  out.push("  " + capRow(btn(eBg, eLabel), 5));

  // Capsule bottom
  out.push("  " + capBot());

  // ── Status line ──
  out.push("");
  // Status: "  ● REC 00:05" etc.
  out.push(`  ${statusColor}${BLD}${statusText}${RST}`);
  out.push("");

  // ── Last transcribed text ──
  if (lastText) {
    out.push(`  ${PAL.fgCyan}Letzter Text:${RST}`);
    const wrapped = wordWrap(lastText, termWidth);
    for (const line of wrapped) {
      out.push(`  ${PAL.fgWhite}${line}${RST}`);
    }
    out.push("");
  }

  // ── Key hints ──
  out.push(
    `  ${PAL.fgGrayDim}` +
    `[x] Löschen  ` +
    `[b] BTW Mic  ` +
    `[m] Mic  ` +
    `[w] Undo  ` +
    `[g] Gemini  ` +
    `[e] Enter  ` +
    `[q] Quit` +
    RST
  );
  out.push("");

  // Clear rest of screen and write
  process.stdout.write(out.join("\n") + "\x1b[J");
}
