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
  "researcher": "Forschung & Intelligence",
  "intelligence-researcher": "Forschung & Intelligence",
  "challenger": "Architektur-Entscheidungen",
  "whiteboard-scanner": "Offene Fehler & Probleme",
  "durability-auditor": "Debugging-Muster",
  "claude-delta-scanner": "Forschung & Intelligence",
  "rules-porter": "Regeln & Konventionen",
  "trigger-router": "Regeln & Konventionen",
  "quality-gate": "Erkenntnisse aus Tests",
  "code-reviewer": "Erkenntnisse aus Code Reviews",
  "tester": "Erkenntnisse aus Tests",
  "optimizer": "Performance & Optimierung",
  "ui-polisher": "UI/UX-Patterns",
  "coder": "Erkenntnisse aus Code Reviews",
  "self-improve": "Systemzustand",
  "tool-failure": "Offene Fehler & Probleme",
  "subagent-failure": "Offene Fehler & Probleme"
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

async function resolveWorkspaceRoot(startDir) {
  let current = path.resolve(startDir);
  while (true) {
    const whiteboard = path.join(
      current,
      "codex-setup",
      "agent-memory",
      "shared",
      "MEMORY.md",
    );
    if (await fileExists(whiteboard)) return current;
    const parent = path.dirname(current);
    if (parent === current) return null;
    current = parent;
  }
}

function buildAuthoritativeMemoryPath(workspaceRoot) {
  return path.join(
    path.resolve(workspaceRoot),
    "codex-setup",
    "agent-memory",
    "shared",
    "MEMORY.md",
  );
}

function deriveWorkspaceRootFromMemory(memoryPath) {
  const absoluteMemory = path.resolve(memoryPath);
  const workspaceRoot = path.dirname(
    path.dirname(path.dirname(path.dirname(absoluteMemory))),
  );
  return absoluteMemory === buildAuthoritativeMemoryPath(workspaceRoot)
    ? workspaceRoot
    : null;
}

async function resolveMemoryPath(options = {}) {
  const explicitMemory = options.memory || process.env.CODEX_WHITEBOARD_FILE;
  if (explicitMemory) {
    const resolvedMemory = path.resolve(explicitMemory);
    const workspaceRoot = options.workspace
      ? path.resolve(options.workspace)
      : deriveWorkspaceRootFromMemory(resolvedMemory);

    if (!workspaceRoot) {
      throw new Error(
        "Explicit Codex whiteboard path must match <workspace>/codex-setup/agent-memory/shared/MEMORY.md.",
      );
    }

    const authoritativeMemory = buildAuthoritativeMemoryPath(workspaceRoot);
    if (resolvedMemory !== authoritativeMemory) {
      throw new Error(
        `Refusing non-authoritative whiteboard path: ${resolvedMemory}`,
      );
    }
    if (!(await fileExists(authoritativeMemory))) {
      throw new Error(`No Codex whiteboard found at ${authoritativeMemory}.`);
    }

    return authoritativeMemory;
  }

  const workspaceRoot = options.workspace
    ? path.resolve(options.workspace)
    : await resolveWorkspaceRoot(process.cwd());
  if (!workspaceRoot) {
    throw new Error(
      "No Codex workspace whiteboard found. Run this from a Codex workspace or pass --workspace <root>.",
    );
  }

  const authoritativeMemory = buildAuthoritativeMemoryPath(workspaceRoot);
  if (await fileExists(authoritativeMemory)) return authoritativeMemory;

  throw new Error(`No Codex whiteboard found at ${authoritativeMemory}.`);
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

      try {
        const stat = await fs.stat(lockPath);
        if (Date.now() - stat.mtimeMs > STALE_LOCK_MS) {
          await fs.rm(lockPath, { force: true });
          continue;
        }
      } catch {
        // Race while checking the lock; retry.
      }

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

function normalizeTimestamp(value) {
  if (!value) return new Date();
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
}

function formatTimestamp(dateValue) {
  const date = normalizeTimestamp(dateValue);
  const year = `${date.getFullYear()}`;
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  const hour = `${date.getHours()}`.padStart(2, "0");
  const minute = `${date.getMinutes()}`.padStart(2, "0");
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function extractSectionContent(content, section) {
  const lines = content.split(/\r?\n/);
  const sectionHeader = normalizeSection(section);
  const sectionIndex = lines.findIndex(
    (line) => isSectionHeader(line, sectionHeader),
  );

  if (sectionIndex < 0) {
    throw new Error(`Section not found: ${sectionHeader}`);
  }

  const sectionLines = [lines[sectionIndex]];
  for (let i = sectionIndex + 1; i < lines.length; i += 1) {
    const stripped = lines[i].trimEnd();
    if (stripped.startsWith("## ") || stripped === "---") break;
    sectionLines.push(lines[i]);
  }

  return `${sectionLines.join("\n").trimEnd()}\n`;
}

function validateWhiteboardContent(content) {
  const lines = content.split(/\r?\n/);
  let previousIndex = -1;

  for (const section of REQUIRED_SECTIONS) {
    const sectionHeader = normalizeSection(section);
    const matches = lines
      .map((line, index) => (isSectionHeader(line, sectionHeader) ? index : -1))
      .filter((index) => index >= 0);

    if (matches.length !== 1) {
      throw new Error(
        `Whiteboard integrity error: expected exactly one ${sectionHeader} section, found ${matches.length}.`,
      );
    }

    if (matches[0] <= previousIndex) {
      throw new Error(
        `Whiteboard integrity error: section order is invalid around ${sectionHeader}.`,
      );
    }

    previousIndex = matches[0];
  }

  const directiveBody = extractSectionContent(content, DIRECTIVE_SECTION)
    .split(/\r?\n/)
    .slice(1)
    .map((line) => line.trim())
    .filter(Boolean);

  if (directiveBody.length === 0) {
    throw new Error(
      "Whiteboard integrity error: Oberste Direktive must not be empty.",
    );
  }

  if (!directiveBody.some((line) => line.startsWith("- "))) {
    throw new Error(
      "Whiteboard integrity error: Oberste Direktive must contain directive bullets.",
    );
  }
}

function computeDirectiveToken(content, memory) {
  return createHash("sha256")
    .update(path.resolve(memory))
    .update("\n")
    .update(extractSectionContent(content, DIRECTIVE_SECTION))
    .digest("hex");
}

function assertDirectiveToken({ content, memory, directiveToken }) {
  if (!directiveToken) {
    throw new Error(
      "Missing --directive-token. Read the current Oberste Direktive again before writing.",
    );
  }

  const expectedToken = computeDirectiveToken(content, memory);
  if (directiveToken !== expectedToken) {
    throw new Error(
      "Directive token mismatch. Read the current Oberste Direktive again before writing.",
    );
  }
}

function insertEntryIntoContent(content, section, entry) {
  const lines = content.split(/\r?\n/);
  const sectionHeader = normalizeSection(section);
  const sectionIndex = lines.findIndex(
    (line) => isSectionHeader(line, sectionHeader),
  );

  if (sectionIndex < 0) {
    throw new Error(`Section not found: ${sectionHeader}`);
  }

  let insertIndex = sectionIndex + 1;
  let placeholderIndex = -1;

  for (let i = sectionIndex + 1; i < lines.length; i += 1) {
    const stripped = lines[i].trimEnd();
    if (stripped.startsWith("## ") || stripped === "---") break;
    if (lines[i].trim() === PLACEHOLDER) {
      placeholderIndex = i;
      break;
    }
    insertIndex = i + 1;
  }

  const entryLines = entry.replace(/\r/g, "").split("\n");
  const finalEntryLines = entryLines.at(-1) === "" ? entryLines : [...entryLines, ""];

  if (placeholderIndex >= 0) {
    lines.splice(placeholderIndex, 1, ...finalEntryLines);
  } else {
    lines.splice(insertIndex, 0, ...finalEntryLines);
  }

  return `${lines.join("\n").replace(/\n+$/u, "\n")}`;
}

async function insertEntry({ memory, section, entry, directiveToken }) {
  if (!section || !entry) {
    throw new Error("insert requires --section and --entry");
  }

  const current = await fs.readFile(memory, "utf8");
  validateWhiteboardContent(current);
  assertDirectiveToken({ content: current, memory, directiveToken });
  const updated = insertEntryIntoContent(current, section, entry);
  validateWhiteboardContent(updated);
  await fs.writeFile(memory, updated, "utf8");
}

function buildEntryFromSentinel(sentinel) {
  const timestamp = formatTimestamp(sentinel.timestamp);
  const agent = sentinel.agent || "unknown";
  const findings = sentinel.findings || "(no findings)";
  return `- **[${timestamp}] ${agent}**: ${findings}`;
}

async function mergeSentinels({ memory, dir, directiveToken }) {
  const sentinelDir = path.resolve(dir || process.env.TMPDIR || os.tmpdir());
  let names = [];
  try {
    names = await fs.readdir(sentinelDir);
  } catch {
    return;
  }

  const sentinelPaths = names
    .filter((name) => name.startsWith("agent-writeback-") && name.endsWith(".json"))
    .sort()
    .map((name) => path.join(sentinelDir, name));

  const current = await fs.readFile(memory, "utf8");
  validateWhiteboardContent(current);
  assertDirectiveToken({ content: current, memory, directiveToken });

  let updated = current;
  const processed = [];

  for (const sentinelPath of sentinelPaths) {
    let raw;
    try {
      raw = await fs.readFile(sentinelPath, "utf8");
    } catch {
      continue;
    }

    let parsed;
    try {
      parsed = JSON.parse(raw);
    } catch {
      continue;
    }

    const section =
      parsed.section || SECTION_MAP[parsed.agent || ""] || "Erkenntnisse aus Code Reviews";
    const entry = buildEntryFromSentinel(parsed);
    updated = insertEntryIntoContent(updated, section, entry);
    processed.push(sentinelPath);
  }

  if (updated !== current) {
    validateWhiteboardContent(updated);
    await fs.writeFile(memory, updated, "utf8");
  }

  for (const sentinelPath of processed) {
    await fs.rm(sentinelPath, { force: true });
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const command = args._[0];
  const memory = await resolveMemoryPath(args);
  const lockPath = `${memory}.lock`;

  switch (command) {
    case "resolve-memory":
      console.log(memory);
      return;
    case "check-integrity": {
      const content = await fs.readFile(memory, "utf8");
      validateWhiteboardContent(content);
      console.log(`OK ${computeDirectiveToken(content, memory)}`);
      return;
    }
    case "print-section": {
      if (!args.section) {
        throw new Error("print-section requires --section");
      }
      const content = await fs.readFile(memory, "utf8");
      validateWhiteboardContent(content);
      console.log(extractSectionContent(content, args.section));
      return;
    }
    case "print-directive": {
      const content = await fs.readFile(memory, "utf8");
      validateWhiteboardContent(content);
      console.log(extractSectionContent(content, DIRECTIVE_SECTION));
      return;
    }
    case "directive-token": {
      const content = await fs.readFile(memory, "utf8");
      validateWhiteboardContent(content);
      console.log(computeDirectiveToken(content, memory));
      return;
    }
    case "insert":
      await withLock(lockPath, async () => {
        await insertEntry({
          memory,
          section: args.section,
          entry: args.entry,
          directiveToken: args["directive-token"],
        });
      });
      return;
    case "merge-sentinels":
      await withLock(lockPath, async () => {
        await mergeSentinels({
          memory,
          dir: args["sentinel-dir"],
          directiveToken: args["directive-token"],
        });
      });
      return;
    default:
      throw new Error(
        "Usage: whiteboard-bridge.mjs <resolve-memory|check-integrity|print-section|print-directive|directive-token|insert|merge-sentinels> [--workspace PATH] [--memory PATH]",
      );
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
