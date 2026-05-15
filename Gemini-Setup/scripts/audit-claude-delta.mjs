#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { REPO_ROOT, loadBridgeContext, readJson } from "./bridge-registry.mjs";

const BRIDGE_ID = "claude-code-delta";
const BRIDGE_CONTEXT = loadBridgeContext(BRIDGE_ID);
const DEFAULT_STATE_PATH = BRIDGE_CONTEXT.statePath;
const TRACKED_PATHS = ["claude-code-setup", "CLAUDE.md"];

function runGit(args) {
  try {
    return execFileSync("git", args, { cwd: REPO_ROOT, encoding: "utf8" }).trim();
  } catch (e) {
    return "";
  }
}

function classifyPath(sourcePath) {
  const filename = path.basename(sourcePath);
  if (sourcePath.includes("/rules/")) return { category: "rule", target_hints: [`Gemini-Setup/rules/${filename}`] };
  if (sourcePath.includes("/agents/")) return { category: "agent", target_hints: [`Gemini-Setup/agents/${filename}`] };
  if (sourcePath === "CLAUDE.md") return { category: "root-rule", target_hints: ["Gemini-Setup/rules/global.md"] };
  return { category: "other", target_hints: ["Gemini-Setup/"] };
}

function main() {
  console.log("=== Claude Code Delta Audit f├╝r Gemini CLI ===");
  const rawLog = runGit(["log", "-n", "20", "--reverse", "--name-status", "--format=%x1e%H%x1f%cI%x1f%s", "--", ...TRACKED_PATHS]);

  if (!rawLog) {
    console.log("Keine ├änderungen in Claude Code gefunden.");
    return;
  }

  const commits = rawLog.split("\u001e").filter(Boolean).map(chunk => {
    const lines = chunk.split("\n");
    const [sha, date, subject] = lines[0].split("\u001f");
    const changes = lines.slice(1).filter(Boolean).map(l => {
      const parts = l.split("\t");
      return { status: parts[0], path: parts[1] };
    });
    return { sha, subject, changes };
  });

  commits.forEach(c => {
    console.log(`\n[${c.sha.slice(0, 7)}] ${c.subject}`);
    c.changes.forEach(ch => {
      const classification = classifyPath(ch.path);
      console.log(`  - ${ch.status} ${ch.path} -> Kategorie: ${classification.category}`);
    });
  });
}

main();
