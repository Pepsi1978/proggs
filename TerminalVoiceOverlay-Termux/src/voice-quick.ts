#!/usr/bin/env node
// Non-interactive voice transcription for use within Claude Code
// Usage: node voice-quick.js [seconds] [--no-gemini]
// Records for [seconds] (default: 8), transcribes via Groq Whisper,
// optionally corrects via Gemini, outputs text to stdout.

import { execSync } from "node:child_process";
import { existsSync, unlinkSync } from "node:fs";
import { join } from "node:path";
import { loadConfig } from "./config.js";
import { GroqWhisperClient } from "./groq-client.js";
import { GeminiClient } from "./gemini-client.js";

const args = process.argv.slice(2);
const duration = parseInt(args.find(a => /^\d+$/.test(a)) || "8", 10);
const noGemini = args.includes("--no-gemini");

async function main() {
  const config = loadConfig();
  const tmpDir = process.env["TMPDIR"] || "/data/data/com.termux/files/usr/tmp";
  const wavPath = join(tmpDir, `voice_quick_${Date.now()}.wav`);

  // Stop any previous recording
  try { execSync("termux-microphone-record -q", { stdio: "ignore" }); } catch {}

  process.stderr.write(`Aufnahme laeuft (${duration} Sekunden)...\n`);

  // Record synchronously — termux-microphone-record with -l blocks until done
  execSync(`termux-microphone-record -f "${wavPath}" -l ${duration} -r 16000 -c 1 -e pcm_16bit`, {
    stdio: "ignore",
    timeout: (duration + 5) * 1000,
  });

  // Wait for recording duration + flush
  execSync(`sleep ${duration + 1}`);
  try { execSync("termux-microphone-record -q", { stdio: "ignore" }); } catch {}

  if (!existsSync(wavPath)) {
    process.stderr.write("Fehler: Keine Aufnahmedatei gefunden.\n");
    process.exit(1);
  }

  // Transcribe with Groq Whisper
  process.stderr.write("Transkribiere...\n");
  const groq = new GroqWhisperClient(config.groqApiKey, config.whisperModel, config.whisperLang);
  let text: string;
  try {
    text = await groq.transcribe(wavPath);
  } catch (e: any) {
    process.stderr.write(`Whisper-Fehler: ${e.message}\n`);
    cleanup(wavPath);
    process.exit(1);
  }

  // Optionally correct with Gemini
  if (!noGemini && config.geminiApiKey) {
    process.stderr.write("Gemini-Korrektur...\n");
    const gemini = new GeminiClient(config.geminiApiKey, config.geminiModel);
    try {
      text = await gemini.correctText(text);
    } catch (e: any) {
      process.stderr.write(`Gemini-Fehler (verwende Rohtext): ${e.message}\n`);
    }
  }

  // Copy to clipboard
  try {
    execSync(`termux-clipboard-set "${text.replace(/"/g, '\\"')}"`, { stdio: "ignore" });
  } catch {}

  // Output the result
  process.stdout.write(text + "\n");
  cleanup(wavPath);
}

function cleanup(path: string) {
  try { if (existsSync(path)) unlinkSync(path); } catch {}
}

main().catch(e => {
  process.stderr.write(`Fehler: ${e.message}\n`);
  process.exit(1);
});
