#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const source = path.join(root, "Gemini-Setup");
const state = path.join(root, "codex-setup", "state", "gemini-delta-state.json");

const result = {
  audit: "gemini-delta",
  source_exists: fs.existsSync(source),
  state_exists: fs.existsSync(state),
  status: fs.existsSync(source) ? "ready-for-read-only-audit" : "source-missing"
};

process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
