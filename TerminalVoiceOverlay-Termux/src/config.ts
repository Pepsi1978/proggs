import { readFileSync, existsSync } from "node:fs";
import { join } from "node:path";
import { homedir } from "node:os";

export interface Config {
  groqApiKey: string;
  whisperModel: string;
  whisperLang: string;
  geminiApiKey?: string;
  geminiModel: string;
}

export function loadConfig(): Config {
  const env = parseEnvFile();

  const groqApiKey = env["GROQ_API_KEY"];
  if (!groqApiKey) {
    throw new Error(
      "GROQ_API_KEY nicht gefunden. Bitte .env Datei anlegen (siehe .env.example)."
    );
  }

  return {
    groqApiKey,
    whisperModel: env["WHISPER_MODEL"] || "whisper-large-v3",
    whisperLang: env["WHISPER_LANG"] || "de",
    geminiApiKey: env["GEMINI_API_KEY"] || undefined,
    geminiModel: env["GEMINI_MODEL"] || "gemini-3.1-flash-lite-preview",
  };
}

function parseEnvFile(): Record<string, string> {
  const searchPaths = [
    join(process.cwd(), ".env"),
    join(homedir(), "projects", "proggs", "TerminalVoiceOverlay-Termux", ".env"),
    join(homedir(), ".config", "TerminalVoiceOverlay", ".env"),
    join(homedir(), ".env"),
  ];

  for (const p of searchPaths) {
    if (existsSync(p)) {
      return parseEnvContents(readFileSync(p, "utf-8"));
    }
  }
  return {};
}

function parseEnvContents(contents: string): Record<string, string> {
  const result: Record<string, string> = {};
  for (const line of contents.split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eqIdx = trimmed.indexOf("=");
    if (eqIdx === -1) continue;
    const key = trimmed.slice(0, eqIdx).trim();
    let value = trimmed.slice(eqIdx + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (value) result[key] = value;
  }
  return result;
}
