#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const REPO_ROOT = "/Users/frank/GeminiCLI";
const AGENTS_MD_PATH = path.join(REPO_ROOT, "AGENTS.md");
const RULES_DIR = path.join(REPO_ROOT, "Gemini-Setup", "rules");

const CORE_RULES = {
  "Superintelligenz": "SUPERINTELLIGENZ.md",
  "Selbstbeobachtung": "SELBSTBEOBACHTUNG.md",
  "Resilient Bugfixing": "RESILIENT_BUGFIXING.md"
};

function log(msg) { console.log(`[agents-rules-sync] ${msg}`); }
function warn(msg) { console.error(`⚠️ [agents-rules-sync] WARNUNG: ${msg}`); }

if (!fs.existsSync(AGENTS_MD_PATH)) {
  warn("AGENTS.md nicht gefunden.");
  process.exit(1);
}

const agentsContent = fs.readFileSync(AGENTS_MD_PATH, "utf8");
const geminiSectionMatch = agentsContent.match(/# Gemini CLI Instructions([\s\S]*?)(?=$|---)/);

if (!geminiSectionMatch) {
  warn("Gemini-Sektion in AGENTS.md nicht gefunden.");
  process.exit(1);
}

const geminiSection = geminiSectionMatch[1];
let issues = 0;

for (const [name, filename] of Object.entries(CORE_RULES)) {
  const rulePath = path.join(RULES_DIR, filename);
  if (!fs.existsSync(rulePath)) {
    warn(`Regel-Datei fehlt: ${filename}`);
    issues++;
    continue;
  }

  // Wir prüfen hier nur auf Existenz der Erwähnung in der AGENTS.md
  // Eine tiefere Inhalts-Prüfung könnte zu restriktiv sein, 
  // da AGENTS.md oft eine Zusammenfassung ist.
  if (!geminiSection.includes(name)) {
    warn(`Die Kern-Direktive "${name}" fehlt in der Gemini-Sektion der AGENTS.md.`);
    issues++;
  }
}

if (issues === 0) {
  log("Konsistenz zwischen AGENTS.md und Kern-Regeln ist gewährleistet.");
  process.exit(0);
} else {
  log(`${issues} Inkonsistenz(en) gefunden.`);
  process.exit(1);
}
