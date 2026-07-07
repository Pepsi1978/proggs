#!/usr/bin/env node

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const REQUIRED_SECTIONS = [
  "Oberste Direktive",
  "Offene Fehler & Probleme",
  "Systemzustand",
  "Erkenntnisse aus Code Reviews",
  "Erkenntnisse aus Tests",
  "Architektur-Entscheidungen",
  "Debugging-Muster",
  "Performance & Optimierung",
  "UI/UX-Patterns",
  "Forschung & Intelligence",
  "Regeln & Konventionen"
];

function parseArgs(argv) {
  const result = { _: [] };
  for (let i = 0; i < argv.length; i += 1) {
    const item = argv[i];
    if (item.startsWith("--")) {
      const key = item.slice(2);
      const next = argv[i + 1];
      if (!next || next.startsWith("--")) {
        result[key] = true;
      } else {
        result[key] = next;
        i += 1;
      }
    } else {
      result._.push(item);
    }
  }
  return result;
}

function findWorkspaceRoot(explicitRoot) {
  if (explicitRoot) {
    return path.resolve(explicitRoot);
  }
  let current = process.cwd();
  while (true) {
    const candidate = path.join(current, "codex-setup", "agent-memory", "shared", "MEMORY.md");
    if (fs.existsSync(candidate)) {
      return current;
    }
    const parent = path.dirname(current);
    if (parent === current) {
      throw new Error("No Codex workspace found. Expected codex-setup/agent-memory/shared/MEMORY.md.");
    }
    current = parent;
  }
}

function whiteboardPath(root) {
  return path.join(root, "codex-setup", "agent-memory", "shared", "MEMORY.md");
}

function readWhiteboard(root) {
  const file = whiteboardPath(root);
  if (!fs.existsSync(file)) {
    throw new Error(`Missing whiteboard: ${file}`);
  }
  return fs.readFileSync(file, "utf8");
}

function validateSections(text) {
  let cursor = -1;
  for (const section of REQUIRED_SECTIONS) {
    const heading = `## ${section}`;
    const index = text.indexOf(heading);
    if (index === -1) {
      throw new Error(`Missing required section: ${section}`);
    }
    if (index <= cursor) {
      throw new Error(`Section order invalid for: ${section}`);
    }
    cursor = index;
  }
}

function extractSection(text, sectionName) {
  const heading = `## ${sectionName}`;
  const start = text.indexOf(heading);
  if (start === -1) {
    throw new Error(`Missing section: ${sectionName}`);
  }
  const next = text.indexOf("\n## ", start + heading.length);
  const end = next === -1 ? text.length : next;
  return {
    start,
    end,
    bodyStart: text.indexOf("\n", start) + 1,
    text: text.slice(start, end)
  };
}

function directiveToken(text) {
  return crypto.createHash("sha256").update(text, "utf8").digest("hex");
}

function printDirective(root) {
  const text = readWhiteboard(root);
  validateSections(text);
  const directive = extractSection(text, "Oberste Direktive").text.trim();
  process.stdout.write(`${directive}\n`);
}

function printToken(root) {
  const text = readWhiteboard(root);
  validateSections(text);
  const directive = extractSection(text, "Oberste Direktive").text.trim();
  process.stdout.write(`${directiveToken(directive)}\n`);
}

function insertEntry(root, section, entryText, expectedToken) {
  const file = whiteboardPath(root);
  const text = readWhiteboard(root);
  validateSections(text);

  const directive = extractSection(text, "Oberste Direktive").text.trim();
  const currentToken = directiveToken(directive);
  if (expectedToken && expectedToken !== currentToken) {
    throw new Error("Directive token mismatch. Re-read the whiteboard before writing.");
  }

  const sectionInfo = extractSection(text, section);
  const body = text.slice(sectionInfo.bodyStart, sectionInfo.end);
  const trimmedEntry = entryText.trim();
  const newBody = body.trim().length === 0
    ? `\n${trimmedEntry}\n`
    : `\n${trimmedEntry}\n${body.startsWith("\n") ? "" : "\n"}${body.trimStart()}`;

  const updated =
    text.slice(0, sectionInfo.bodyStart) +
    newBody +
    text.slice(sectionInfo.end);

  fs.writeFileSync(file, updated, "utf8");
}

function mergeSentinels(root, sentinelDir) {
  const resolvedDir = sentinelDir
    ? path.resolve(sentinelDir)
    : path.join(root, "codex-setup", "state", "writeback");

  if (!fs.existsSync(resolvedDir)) {
    process.stdout.write("No sentinel directory present.\n");
    return;
  }

  const files = fs.readdirSync(resolvedDir).filter((name) => name.endsWith(".json"));
  if (files.length === 0) {
    process.stdout.write("No sentinel files present.\n");
    return;
  }

  const text = readWhiteboard(root);
  validateSections(text);
  const directive = extractSection(text, "Oberste Direktive").text.trim();
  const token = directiveToken(directive);

  for (const file of files) {
    const fullPath = path.join(resolvedDir, file);
    const payload = JSON.parse(fs.readFileSync(fullPath, "utf8"));
    const section = payload.section || "Forschung & Intelligence";
    const entry = payload.findings || payload.summary || JSON.stringify(payload);
    insertEntry(root, section, `- ${entry}`, token);
    fs.unlinkSync(fullPath);
  }

  process.stdout.write(`Merged ${files.length} sentinel file(s).\n`);
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const command = args._[0];
  const root = findWorkspaceRoot(args.workspace);

  switch (command) {
    case "print-directive":
      printDirective(root);
      break;
    case "directive-token":
      printToken(root);
      break;
    case "insert":
      if (!args.section || !args.text) {
        throw new Error("Usage: insert --section <name> --text <entry> [--token <sha256>]");
      }
      insertEntry(root, args.section, args.text, args.token);
      process.stdout.write("Inserted entry.\n");
      break;
    case "merge-sentinels":
      mergeSentinels(root, args.dir);
      break;
    default:
      throw new Error("Supported commands: print-directive, directive-token, insert, merge-sentinels");
  }
}

try {
  main();
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`${message}\n`);
  process.exit(1);
}
