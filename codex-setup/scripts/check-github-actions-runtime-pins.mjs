#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_WORKFLOW_DIR = path.join(REPO_ROOT, ".github", "workflows");
const DEFAULT_RULES = [
  {
    name: "actions/checkout",
    pattern: /actions\/checkout@v4\b/gi,
    replacement_hint: "actions/checkout@v5",
  },
  {
    name: "actions/setup-node",
    pattern: /actions\/setup-node@v4\b/gi,
    replacement_hint: "actions/setup-node@v5",
  },
];

function fail(message, details = "") {
  const suffix = details ? `\n${details}` : "";
  console.error(`${message}${suffix}`);
  process.exit(1);
}

function usage() {
  return [
    "Usage: check-github-actions-runtime-pins.mjs [--workflow-dir PATH] [--json]",
    "",
    "Scans GitHub Actions workflow files for deprecated runtime pins and reports exact matches.",
  ].join("\n");
}

function parseArgs(argv) {
  const options = {
    workflowDir: DEFAULT_WORKFLOW_DIR,
    json: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--workflow-dir") {
      const value = argv[index + 1];
      if (!value || value.startsWith("--")) {
        fail("Missing value for --workflow-dir.", usage());
      }
      options.workflowDir = path.resolve(value);
      index += 1;
      continue;
    }
    if (arg === "--json") {
      options.json = true;
      continue;
    }
    if (arg === "--help" || arg === "-h") {
      console.log(usage());
      process.exit(0);
    }
    fail(`Unknown argument: ${arg}`, usage());
  }

  return options;
}

function collectWorkflowFiles(workflowDir) {
  if (!fs.existsSync(workflowDir)) {
    fail(`Workflow directory not found: ${workflowDir}`);
  }

  const entries = fs.readdirSync(workflowDir, { withFileTypes: true });
  return entries
    .filter((entry) => entry.isFile() && /\.(ya?ml)$/i.test(entry.name))
    .map((entry) => path.join(workflowDir, entry.name));
}

function scanFile(filePath) {
  const text = fs.readFileSync(filePath, "utf8");
  const matches = [];

  for (const rule of DEFAULT_RULES) {
    rule.pattern.lastIndex = 0;
    let match;
    while ((match = rule.pattern.exec(text)) !== null) {
      matches.push({
        file: path.relative(REPO_ROOT, filePath).replace(/\\/g, "/"),
        pin: match[0],
        replacement_hint: rule.replacement_hint,
        offset: match.index,
      });
    }
  }

  return matches;
}

function main() {
  const options = parseArgs(process.argv.slice(2));
  const files = collectWorkflowFiles(options.workflowDir);
  const findings = files.flatMap(scanFile);

  if (options.json) {
    console.log(
      JSON.stringify(
        {
          workflow_dir: path.relative(REPO_ROOT, options.workflowDir).replace(/\\/g, "/"),
          findings,
        },
        null,
        2,
      ),
    );
    process.exit(findings.length ? 1 : 0);
  }

  if (findings.length === 0) {
    console.log("No deprecated GitHub Actions runtime pins found.");
    return;
  }

  console.error("Deprecated GitHub Actions runtime pins found:");
  for (const finding of findings) {
    console.error(
      `- ${finding.file}: ${finding.pin} -> replace with ${finding.replacement_hint}`,
    );
  }
  process.exit(1);
}

main();
