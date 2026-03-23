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
  "implemented-intelligence-suggestions.json",
);
const DEFAULT_LEDGER = {
  version: 1,
  scope: "programming-environment-only",
  ledger_kind: "implemented-intelligence-suggestions",
  proposal_prefix: "💡 Intelligenz-Vorschlag:",
  github_url: "",
  bridge_reference: [
    "codex-setup/bridges/intelligence-suggestion-exchange-bridge.md",
    "codex-setup/bridges/intelligence-suggestion-exchange-bridge.json",
  ],
  peer_ledgers: {
    Codex: {
      repo_path: "codex-setup/state/implemented-intelligence-suggestions.json",
      github_url: "",
      write_policy: "read-write-own-workspace",
    },
    "Cloud Code": {
      expected_repo_path:
        "claude-code-setup/state/implemented-intelligence-suggestions.json",
      expected_github_url: "",
      write_policy: "read-only-from-codex",
      status: "awaiting-foreign-registration",
    },
    "Gemini CLI": {
      expected_repo_path:
        "Gemini-Setup/state/implemented-intelligence-suggestions.json",
      expected_local_path:
        "C:/Users/barwa/GeminiCLI/state/implemented-intelligence-suggestions.json",
      expected_github_url: "",
      write_policy: "read-only-from-codex",
      status: "awaiting-foreign-registration",
    },
  },
  entries: [],
};

function usage() {
  return [
    "Usage:",
    "  register-intelligence-suggestion.mjs list [--json] [--state PATH]",
    "  register-intelligence-suggestion.mjs add --id ID --summary TEXT --proposal TEXT --context TEXT --suggested-because TEXT --implemented-because TEXT --implementation TEXT --bridge-value TEXT --adoption-guidance TEXT [--portable-to CSV] [--artifacts CSV] [--source-cli NAME] [--status STATUS] [--json] [--state PATH]",
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
    peer_ledgers: loaded.peer_ledgers || DEFAULT_LEDGER.peer_ledgers,
    bridge_reference: loaded.bridge_reference || DEFAULT_LEDGER.bridge_reference,
    entries: Array.isArray(loaded.entries) ? loaded.entries : [],
  };
}

function buildEntry(args, existingEntry = null) {
  const now = new Date().toISOString();
  return {
    id: ensureText(args.id || existingEntry?.id, "id"),
    source_cli: ensureText(
      args["source-cli"] || existingEntry?.source_cli || "Codex",
      "source-cli",
    ),
    summary: ensureText(args.summary || existingEntry?.summary, "summary"),
    proposal_text: ensureDetailedText(
      args.proposal || existingEntry?.proposal_text,
      "proposal",
      20,
    ),
    context_for_other_clis: ensureDetailedText(
      args.context || existingEntry?.context_for_other_clis,
      "context",
    ),
    why_it_was_suggested: ensureDetailedText(
      args["suggested-because"] || existingEntry?.why_it_was_suggested,
      "suggested-because",
      30,
    ),
    why_it_was_implemented: ensureDetailedText(
      args["implemented-because"] || existingEntry?.why_it_was_implemented,
      "implemented-because",
      30,
    ),
    how_it_was_implemented: ensureDetailedText(
      args.implementation || existingEntry?.how_it_was_implemented,
      "implementation",
    ),
    bridge_value: ensureDetailedText(
      args["bridge-value"] || existingEntry?.bridge_value,
      "bridge-value",
      30,
    ),
    adoption_guidance: ensureDetailedText(
      args["adoption-guidance"] || existingEntry?.adoption_guidance,
      "adoption-guidance",
      30,
    ),
    portable_to: splitCsv(args["portable-to"], existingEntry?.portable_to || [
      "Cloud Code",
      "Gemini CLI",
    ]),
    artifacts: splitCsv(args.artifacts, existingEntry?.artifacts || []),
    created_at: existingEntry?.created_at || now,
    status: `${args.status || existingEntry?.status || "active"}`.trim() || "active",
    ...(existingEntry ? { updated_at: now } : {}),
  };
}

function printList(ledger, asJson) {
  if (asJson) {
    process.stdout.write(`${JSON.stringify(ledger, null, 2)}\n`);
    return;
  }

  const lines = [
    "Implemented Intelligence Suggestions",
    `Scope: ${ledger.scope}`,
    `Entries: ${ledger.entries.length}`,
    `GitHub: ${ledger.github_url}`,
  ];

  ledger.entries.forEach((entry, index) => {
    lines.push("");
    lines.push(`${index + 1}. ${entry.id}`);
    lines.push(`   Summary: ${entry.summary}`);
    lines.push(`   Proposal: ${entry.proposal_text}`);
    lines.push(`   Context: ${entry.context_for_other_clis}`);
    lines.push(`   Suggested because: ${entry.why_it_was_suggested}`);
    lines.push(`   Implemented because: ${entry.why_it_was_implemented}`);
    lines.push(`   Implementation: ${entry.how_it_was_implemented}`);
    lines.push(`   Bridge value: ${entry.bridge_value}`);
    lines.push(`   Adoption guidance: ${entry.adoption_guidance}`);
    if (entry.portable_to?.length) {
      lines.push(`   Portable to: ${entry.portable_to.join(", ")}`);
    }
    if (entry.artifacts?.length) {
      lines.push(`   Artifacts: ${entry.artifacts.join(", ")}`);
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
      `${result.action} implemented intelligence suggestion ${result.entry.id} in ${result.ledger_path}\n`,
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
