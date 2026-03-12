// ANSI color codes matching macOS overlay colors
const C = {
  reset: "\x1b[0m",
  bold: "\x1b[1m",
  dim: "\x1b[2m",
  // Button colors
  red: "\x1b[41m\x1b[97m",      // Recording / X button
  orange: "\x1b[43m\x1b[30m",   // Processing
  green: "\x1b[42m\x1b[97m",    // Success / Toggle on
  blue: "\x1b[44m\x1b[97m",     // Mic idle
  gray: "\x1b[100m\x1b[97m",    // Toggle off / Idle
  white: "\x1b[97m",
  redText: "\x1b[91m",
  greenText: "\x1b[92m",
  orangeText: "\x1b[93m",
  blueText: "\x1b[94m",
  grayText: "\x1b[90m",
  cyanText: "\x1b[96m",
};

export type MicState = "idle" | "recording" | "processing" | "success" | "error";

export interface OverlayState {
  micState: MicState;
  geminiEnabled: boolean;
  autoEnterEnabled: boolean;
  lastText: string;
}

export function clearScreen(): void {
  process.stdout.write("\x1b[2J\x1b[H");
}

export function renderOverlay(state: OverlayState): void {
  // Move cursor to top
  process.stdout.write("\x1b[H");

  const micColor = getMicColor(state.micState);
  const micLabel = getMicLabel(state.micState);
  const gColor = state.geminiEnabled ? C.green : C.gray;
  const enterColor = state.autoEnterEnabled ? C.orange : C.gray;

  const lines = [
    "",
    `  ${C.bold}${C.white}Terminal Voice Overlay${C.reset}  ${C.grayText}v1.0 Termux${C.reset}`,
    `  ${C.grayText}${"─".repeat(36)}${C.reset}`,
    "",
    `  ${C.red} X ${C.reset}  Zeile loeschen          ${C.grayText}[x]${C.reset}`,
    "",
    `  ${micColor} ${micLabel} ${C.reset}  ${getMicStatusText(state.micState)}   ${C.grayText}[m]${C.reset}`,
    "",
    `  ${C.gray} W ${C.reset}  Whisper Undo            ${C.grayText}[w]${C.reset}`,
    `  ${gColor} G ${C.reset}  Gemini ${state.geminiEnabled ? "AN" : "AUS"}${" ".repeat(15 - (state.geminiEnabled ? 2 : 3))}${C.grayText}[g]${C.reset}`,
    `  ${enterColor} ⏎ ${C.reset}  Auto-Enter ${state.autoEnterEnabled ? "AN" : "AUS"}${" ".repeat(11 - (state.autoEnterEnabled ? 2 : 3))}${C.grayText}[e]${C.reset}`,
    "",
    `  ${C.grayText}${"─".repeat(36)}${C.reset}`,
    `  ${C.grayText}[q] Beenden${C.reset}`,
    "",
  ];

  if (state.lastText) {
    lines.push(`  ${C.cyanText}Letzter Text:${C.reset}`);
    // Word-wrap at 50 chars
    const wrapped = wordWrap(state.lastText, 50);
    for (const line of wrapped) {
      lines.push(`  ${C.white}${line}${C.reset}`);
    }
    lines.push("");
  }

  // Clear remaining screen area
  const output = lines.join("\n") + "\x1b[J";
  process.stdout.write(output);
}

function getMicColor(state: MicState): string {
  switch (state) {
    case "idle": return C.blue;
    case "recording": return C.red;
    case "processing": return C.orange;
    case "success": return C.green;
    case "error": return C.red;
  }
}

function getMicLabel(state: MicState): string {
  switch (state) {
    case "idle": return "MIC";
    case "recording": return "REC";
    case "processing": return "...";
    case "success": return " ✓ ";
    case "error": return " ! ";
  }
}

function getMicStatusText(state: MicState): string {
  switch (state) {
    case "idle": return "Bereit              ";
    case "recording": return `${C.redText}${C.bold}Aufnahme laeuft...${C.reset}  `;
    case "processing": return `${C.orangeText}Verarbeitung...${C.reset}     `;
    case "success": return `${C.greenText}Eingefuegt!${C.reset}         `;
    case "error": return `${C.redText}Fehler!${C.reset}             `;
  }
}

function wordWrap(text: string, maxWidth: number): string[] {
  const words = text.split(" ");
  const lines: string[] = [];
  let current = "";

  for (const word of words) {
    if (current.length + word.length + 1 > maxWidth) {
      if (current) lines.push(current);
      current = word;
    } else {
      current = current ? `${current} ${word}` : word;
    }
  }
  if (current) lines.push(current);
  return lines;
}
