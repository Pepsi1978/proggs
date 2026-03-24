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
  "environment-fixes.json",
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
      category: args.category,
      summary: args.summary,
      context_for_other_clis: args.context,
      symptom_before_fix: args.symptom,
      root_cause: args["root-cause"],
      what_was_fixed: args.what,
      why_it_was_fixed: args.why,
      exact_error_text: args["exact-error"],
      root_cause_why_chain: args["why-chain"],
      related_error_sources_checked: args["related-checks"],
      wrong_pattern_example: args["wrong-pattern"],
      right_pattern_example: args["right-pattern"],
      avoidance_rule: args["avoidance-rule"],
      resilience_summary: args["resilience-summary"],
      fix_induced_failure_review: args["failure-review"],
      verification: args.verification,
      portability_notes: args["portability-notes"],
      created_at: new Date().toISOString()
    };
    ledger.entries.push(entry);
    writeJson(ledgerPath, ledger);
    console.log(`Fix ${entry.id} registriert in ${ledgerPath}`);
    return;
  }
}

main();
