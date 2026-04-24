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
const file = path.join(process.cwd(), "codex-setup", "state", "implemented-intelligence-suggestions.json");
const data = JSON.parse(fs.readFileSync(file, "utf8"));

const entry = {
  id: args.id,
  summary: args.summary,
  proposal_text: args.proposal,
  context_for_other_clis: args.context,
  why_it_was_implemented: args.why,
  how_it_was_implemented: args.how,
  resilience_summary: args.resilience,
  future_failure_review: args.failureReview,
  status: args.status || "implemented",
  created_at: args.createdAt
};

data.entries.push(entry);
fs.writeFileSync(file, `${JSON.stringify(data, null, 2)}\n`, "utf8");
process.stdout.write(`${entry.id}\n`);
