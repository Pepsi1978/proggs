#!/usr/bin/env node

import fs from "node:fs/promises";
import { createHash } from "node:crypto";
import os from "node:os";
import path from "node:path";

const PLACEHOLDER = "_Noch keine Eintraege._";
const DEFAULT_LOCK_TIMEOUT_MS = 5000;
const STALE_LOCK_MS = 30 * 60 * 1000;
const DIRECTIVE_SECTION = "Oberste Direktive";
const REQUIRED_SECTIONS = [
  DIRECTIVE_SECTION,
  "Offene Fehler & Probleme",
  "Systemzustand",
  "Erkenntnisse aus Code Reviews",
  "Erkenntnisse aus Tests",
  "Architektur-Entscheidungen",
  "Debugging-Muster",
  "Performance & Optimierung",
  "UI/UX-Patterns",
  "Forschung & Intelligence",
  "Regeln & Konventionen",
];

const SECTION_MAP = {
  "env-checker": "Systemzustand",
  "evolution-analyst": "Systemzustand",
  "intelligence-researcher": "Forschung & Intelligence",
  "challenger": "Architektur-Entscheidungen",
  "durability-auditor": "Debugging-Muster",
  "quality-gate": "Erkenntnisse aus Tests",
  "code-reviewer": "Erkenntnisse aus Code Reviews",
  "tester": "Erkenntnisse aus Tests",
  "optimizer": "Performance & Optimierung",
  "coder": "Erkenntnisse aus Code Reviews"
};

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i += 1) {
    const part = argv[i];
    if (part.startsWith("--")) {
      const key = part.slice(2);
      const next = argv[i + 1];
      if (!next || next.startsWith("--")) {
        args[key] = true;
      } else {
        args[key] = next;
        i += 1;
      }
    } else {
      args._.push(part);
    }
  }
  return args;
}

async function fileExists(target) {
  try {
    await fs.access(target);
    return true;
  } catch {
    return false;
  }
}

function buildAuthoritativeMemoryPath(workspaceRoot) {
  return path.join(
    path.resolve(workspaceRoot),
    "Gemini-Setup",
    "agent-memory",
    "shared",
    "MEMORY.md",
  );
}

async function resolveMemoryPath(options = {}) {
  const workspaceRoot = options.workspace || "C:\\Users\\barwa\\GeminiCLI";
  const authoritativeMemory = buildAuthoritativeMemoryPath(workspaceRoot);
  if (await fileExists(authoritativeMemory)) return authoritativeMemory;
  throw new Error(`No Gemini whiteboard found at ${authoritativeMemory}.`);
}

async function sleep(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function withLock(lockPath, fn) {
  const deadline = Date.now() + DEFAULT_LOCK_TIMEOUT_MS;
  while (true) {
    try {
      const handle = await fs.open(lockPath, "wx");
      try {
        return await fn();
      } finally {
        await handle.close();
        await fs.rm(lockPath, { force: true });
      }
    } catch (error) {
      if (error.code !== "EEXIST") throw error;
      if (Date.now() >= deadline) {
        throw new Error(`Lock timeout while waiting for ${lockPath}`);
      }
      await sleep(100);
    }
  }
}

function normalizeSection(section) {
  if (!section) return null;
  return section.startsWith("## ") ? section : `## ${section}`;
}

function isSectionHeader(line, sectionHeader) {
  const trimmed = line.trimEnd();
  return trimmed === sectionHeader || trimmed.startsWith(`${sectionHeader} `);
}

function formatTimestamp(dateValue) {
  const date = dateValue ? new Date(dateValue) : new Date();
  return date.toISOString().replace("T", " ").substring(0, 16);
}

function extractSectionContent(content, section) {
  const lines = content.split(/\r?\n/);
  const sectionHeader = normalizeSection(section);
  const sectionIndex = lines.findIndex((line) => isSectionHeader(line, sectionHeader));
  if (sectionIndex < 0) throw new Error(`Section not found: ${sectionHeader}`);
  const sectionLines = [lines[sectionIndex]];
  for (let i = sectionIndex + 1; i < lines.length; i += 1) {
    if (lines[i].trimEnd().startsWith("## ")) break;
    sectionLines.push(lines[i]);
  }
  return `${sectionLines.join("\n").trimEnd()}\n`;
}

function validateWhiteboardContent(content) {
  for (const section of REQUIRED_SECTIONS) {
    const sectionHeader = normalizeSection(section);
    if (!content.includes(sectionHeader)) {
      throw new Error(`Whiteboard integrity error: missing ${sectionHeader}`);
    }
  }
}

function computeDirectiveToken(content, memory) {
  return createHash("sha256")
    .update(path.resolve(memory))
    .update("\n")
    .update(extractSectionContent(content, DIRECTIVE_SECTION))
    .digest("hex");
}

function insertEntryIntoContent(content, section, entry) {
  const lines = content.split(/\r?\n/);
  const sectionHeader = normalizeSection(section);
  const sectionIndex = lines.findIndex((line) => isSectionHeader(line, sectionHeader));
  if (sectionIndex < 0) throw new Error(`Section not found: ${sectionHeader}`);
  
  let insertIndex = sectionIndex + 1;
  let placeholderIndex = -1;

  for (let i = sectionIndex + 1; i < lines.length; i += 1) {
    if (lines[i].trimEnd().startsWith("## ")) break;
    if (lines[i].trim() === PLACEHOLDER) {
      placeholderIndex = i;
      break;
    }
    insertIndex = i + 1;
  }

  const finalEntry = entry.endsWith("\n") ? entry : `${entry}\n`;

  if (placeholderIndex >= 0) {
    lines.splice(placeholderIndex, 1, finalEntry.trim());
  } else {
    lines.splice(insertIndex, 0, finalEntry.trim());
  }
  return lines.join("\n");
}

async function mergeSentinels({ memory, dir, directiveToken }) {
  const sentinelDir = path.resolve(dir || os.tmpdir());
  console.log(`Suche Sentinels in: ${sentinelDir}`);
  let names = [];
  try { names = await fs.readdir(sentinelDir); } catch (e) { console.log(`Fehler beim Lesen: ${e.message}`); return; }
  const sentinelPaths = names.filter((n) => n.startsWith("agent-writeback-") && n.endsWith(".json"));
  console.log(`Gefundene Sentinels: ${sentinelPaths.length}`);
  if (sentinelPaths.length === 0) return;

  const content = await fs.readFile(memory, "utf8");
  let updated = content;
  for (const name of sentinelPaths) {
    const fullPath = path.join(sentinelDir, name);
    console.log(`Verarbeite: ${name}`);
    const raw = await fs.readFile(fullPath, "utf8");
    const parsed = JSON.parse(raw);
    const section = parsed.section || SECTION_MAP[parsed.agent] || "Systemzustand";
    console.log(`Ziel-Sektion: ${section}`);
    const entry = `- **[${formatTimestamp(parsed.timestamp)}] ${parsed.agent}**: ${parsed.findings}`;
    updated = insertEntryIntoContent(updated, section, entry);
    await fs.rm(fullPath);
  }
  await fs.writeFile(memory, updated, "utf8");
  console.log("Whiteboard erfolgreich aktualisiert.");
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const command = args._[0];
  const memory = await resolveMemoryPath(args);
  const lockPath = `${memory}.lock`;

  switch (command) {
    case "directive-token":
      console.log(computeDirectiveToken(await fs.readFile(memory, "utf8"), memory));
      return;
    case "merge-sentinels":
      await withLock(lockPath, async () => {
        await mergeSentinels({ memory, dir: args["sentinel-dir"] });
      });
      return;
    default:
      console.log("Usage: whiteboard-bridge.mjs <directive-token|merge-sentinels>");
  }
}

main().catch(e => { console.error(e.message); process.exit(1); });
