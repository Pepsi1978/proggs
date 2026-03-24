#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = "C:\\Users\\barwa\\GeminiCLI";
const DEFAULT_LEDGER_PATH = path.join(
  REPO_ROOT,
  "Gemini-Setup",
  "state",
  "implemented-intelligence-suggestions.json",
);
const DEFAULT_LEDGER = {
  version: 1,
  cli: "Gemini CLI",
  entries: [],
};

function readJson(filePath, fallback) {
  if (!fs.existsSync(filePath)) return fallback;
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function writeJson(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, "utf8");
}

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const part = argv[i];
    if (part.startsWith("--")) {
      const key = part.slice(2);
      args[key] = argv[++i];
    } else {
      args._.push(part);
    }
  }
  return args;
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const command = args._[0] || "list";
  const ledgerPath = args.state || DEFAULT_LEDGER_PATH;

  if (command === "list") {
    console.log(JSON.stringify(readJson(ledgerPath, DEFAULT_LEDGER), null, 2));
    return;
  }

  if (command === "add") {
    const ledger = readJson(ledgerPath, DEFAULT_LEDGER);
    const entry = {
      id: args.id,
      summary: args.summary,
      proposal_text: args.proposal,
      context_for_other_clis: args.context,
      why_it_was_suggested: args["suggested-because"],
      why_it_was_implemented: args["implemented-because"],
      how_it_was_implemented: args.implementation,
      bridge_value: args["bridge-value"],
      adoption_guidance: args["adoption-guidance"],
      resilience_summary: args["resilience-summary"],
      future_failure_review: args["future-failure-review"],
      created_at: new Date().toISOString()
    };
    ledger.entries.push(entry);
    writeJson(ledgerPath, ledger);
    console.log(`Intelligenzvorschlag ${entry.id} registriert in ${ledgerPath}`);
    return;
  }
}

main();
