#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const source = path.join(root, "claude-code-setup");
const state = path.join(root, "codex-setup", "state", "claude-delta-state.json");

const result = {
  audit: "claude-delta",
  source_exists: fs.existsSync(source),
  state_exists: fs.existsSync(state),
  status: fs.existsSync(source) ? "ready-for-read-only-audit" : "source-missing"
};

process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
