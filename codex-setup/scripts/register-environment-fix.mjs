#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

function parseArgs(argv) {
  const result = {};
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i];
    const value = argv[i + 1];
    if (!key?.startsWith("--") || value === undefined) {
      throw new Error("Expected --key value pairs.");
    }
    result[key.slice(2)] = value;
  }
  return result;
}

const args = parseArgs(process.argv.slice(2));
const file = path.join(process.cwd(), "codex-setup", "state", "environment-fixes.json");
const data = JSON.parse(fs.readFileSync(file, "utf8"));

const entry = {
  id: args.id,
  summary: args.summary,
  context: args.context,
  symptom: args.symptom,
  root_cause: args.rootCause,
  fixed_at: args.fixedAt,
  verification: args.verification,
  portability_notes: args.portability,
  resilience_summary: args.resilience
};

data.entries.push(entry);
fs.writeFileSync(file, `${JSON.stringify(data, null, 2)}\n`, "utf8");
process.stdout.write(`${entry.id}\n`);
