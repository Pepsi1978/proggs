#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_LEDGER_PATH = path.join(
  REPO_ROOT,
  "codex-setup",
  "state",
  "environment-fixes.json",
);
const DEFAULT_LEDGER = {
  version: 1,
  scope: "programming-environment-only",
  entries: [],
};

function usage() {
  return [
    "Usage:",
    "  register-environment-fix.mjs list [--json] [--state PATH]",
    "  register-environment-fix.mjs add --id ID --category CATEGORY --summary TEXT --context TEXT --symptom TEXT --root-cause TEXT --what TEXT --why TEXT --verification TEXT --portability-notes TEXT [--impact TEXT] [--portable-to CSV] [--artifacts CSV] [--source-cli NAME] [--status STATUS] [--json] [--state PATH]",
  ].join("\n");
}

function fail(message) {
  process.stderr.write(`${message}\n`);
  process.exit(1);
}

function parseArgs(argv) {
  const args = { _: [], json: false };
  for (let i = 0; i < argv.length; i += 1) {
    const part = argv[i];
    if (!part.startsWith("--")) {
      args._.push(part);
      continue;
    }
    const key = part.slice(2);
    if (key === "json") {
      args.json = true;
      continue;
    }
    const next = argv[i + 1];
    if (!next || next.startsWith("--")) {
      throw new Error(`Missing value for --${key}`);
    }
    args[key] = next;
    i += 1;
  }
  return args;
}

function splitCsv(value, fallback = []) {
  if (!value) return fallback;
  return `${value}`
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function ensureText(value, label) {
  const normalized = `${value || ""}`.trim();
  if (!normalized) {
    throw new Error(`Missing required field: ${label}`);
  }
  return normalized;
}

function ensureDetailedText(value, label, minimumLength = 40) {
  const normalized = ensureText(value, label);
  if (normalized.length < minimumLength) {
    throw new Error(`${label} must be at least ${minimumLength} characters so other CLIs get enough context.`);
  }
  return normalized;
}

function readJson(filePath, fallback) {
  if (!fs.existsSync(filePath)) {
    return fallback;
  }
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function writeJson(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, "utf8");
}

function loadLedger(filePath) {
  const loaded = readJson(filePath, DEFAULT_LEDGER);
  return {
    ...DEFAULT_LEDGER,
    ...loaded,
    entries: Array.isArray(loaded.entries) ? loaded.entries : [],
  };
}

function buildEntry(args, existingEntry = null) {
  const now = new Date().toISOString();
  const portableTo = splitCsv(args["portable-to"], existingEntry?.portable_to || [
    "Cloud Code",
    "Gemini CLI",
  ]);
  const artifacts = splitCsv(args.artifacts, existingEntry?.artifacts || []);
  const entry = {
    id: ensureText(args.id || existingEntry?.id, "id"),
    source_cli: ensureText(args["source-cli"] || existingEntry?.source_cli || "Codex", "source-cli"),
    category: ensureText(args.category || existingEntry?.category, "category"),
    summary: ensureText(args.summary || existingEntry?.summary, "summary"),
    context_for_other_clis: ensureDetailedText(
      args.context || existingEntry?.context_for_other_clis,
      "context",
    ),
    symptom_before_fix: ensureDetailedText(
      args.symptom || existingEntry?.symptom_before_fix,
      "symptom",
    ),
    root_cause: ensureDetailedText(
      args["root-cause"] || existingEntry?.root_cause,
      "root-cause",
    ),
    what_was_fixed: ensureText(args.what || existingEntry?.what_was_fixed, "what"),
    why_it_was_fixed: ensureText(args.why || existingEntry?.why_it_was_fixed, "why"),
    verification: ensureDetailedText(
      args.verification || existingEntry?.verification,
      "verification",
    ),
    portability_notes: ensureDetailedText(
      args["portability-notes"] || existingEntry?.portability_notes,
      "portability-notes",
    ),
    impact: `${args.impact || existingEntry?.impact || ""}`.trim(),
    portable_to: portableTo,
    artifacts,
    created_at: existingEntry?.created_at || now,
    status: `${args.status || existingEntry?.status || "active"}`.trim() || "active",
  };

  if (existingEntry) {
    entry.updated_at = now;
  }

  return entry;
}

function printList(ledger, asJson) {
  if (asJson) {
    process.stdout.write(`${JSON.stringify(ledger, null, 2)}\n`);
    return;
  }

  const lines = [
    "Environment Fix Ledger",
    `Scope: ${ledger.scope}`,
    `Entries: ${ledger.entries.length}`,
  ];

  ledger.entries.forEach((entry, index) => {
    lines.push("");
    lines.push(`${index + 1}. ${entry.id}`);
    lines.push(`   Summary: ${entry.summary}`);
    lines.push(`   Context: ${entry.context_for_other_clis}`);
    lines.push(`   Symptom: ${entry.symptom_before_fix}`);
    lines.push(`   Root cause: ${entry.root_cause}`);
    lines.push(`   What: ${entry.what_was_fixed}`);
    lines.push(`   Why: ${entry.why_it_was_fixed}`);
    lines.push(`   Verification: ${entry.verification}`);
    lines.push(`   Portability: ${entry.portability_notes}`);
    if (entry.impact) {
      lines.push(`   Impact: ${entry.impact}`);
    }
    if (entry.portable_to?.length) {
      lines.push(`   Portable to: ${entry.portable_to.join(", ")}`);
    }
  });

  process.stdout.write(`${lines.join("\n")}\n`);
}

function addEntry(args, ledgerPath) {
  const ledger = loadLedger(ledgerPath);
  const existingIndex = ledger.entries.findIndex((entry) => entry.id === args.id);
  const existingEntry = existingIndex >= 0 ? ledger.entries[existingIndex] : null;
  const nextEntry = buildEntry(args, existingEntry);

  if (existingIndex >= 0) {
    ledger.entries[existingIndex] = nextEntry;
  } else {
    ledger.entries.push(nextEntry);
  }

  writeJson(ledgerPath, ledger);
  return {
    ledger_path: ledgerPath,
    entry: nextEntry,
    action: existingIndex >= 0 ? "updated" : "added",
  };
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const command = args._[0] || "list";
  const ledgerPath = path.resolve(args.state || DEFAULT_LEDGER_PATH);

  if (command === "list") {
    printList(loadLedger(ledgerPath), args.json);
    return;
  }

  if (command === "add") {
    const result = addEntry(args, ledgerPath);
    if (args.json) {
      process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
      return;
    }
    process.stdout.write(
      `${result.action} environment fix ${result.entry.id} in ${result.ledger_path}\n`,
    );
    return;
  }

  throw new Error(usage());
}

try {
  main();
} catch (error) {
  fail(error.message);
}
