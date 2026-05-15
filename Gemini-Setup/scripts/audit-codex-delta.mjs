#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { REPO_ROOT, loadBridgeContext, readJson } from "./bridge-registry.mjs";

const BRIDGE_ID = "codex-delta";
const BRIDGE_CONTEXT = loadBridgeContext(BRIDGE_ID);
const DEFAULT_STATE_PATH = BRIDGE_CONTEXT.statePath;
const TRACKED_PATHS = ["codex-setup"];

const DEFAULT_STATE = {
  version: 1,
  bridge_id: BRIDGE_ID,
  last_reviewed_commit: null,
  last_applied_commit: null
};

function runGit(args) {
  return execFileSync("git", args, { cwd: REPO_ROOT, encoding: "utf8" }).trim();
}

function isPortableSourcePath(sourcePath) {
  return sourcePath.startsWith("codex-setup/");
}

function classifyPath(sourcePath) {
  const filename = path.basename(sourcePath);
  if (sourcePath.includes("/rules/")) return { category: "rule", target_hints: [`Gemini-Setup/rules/${filename}`] };
  if (sourcePath.includes("/agents/")) return { category: "agent", target_hints: [`Gemini-Setup/agents/${filename}`] };
  if (sourcePath.includes("/skills/")) return { category: "skill", target_hints: [`Gemini-Setup/skills/${filename}`] };
  if (sourcePath.includes("/hooks/")) return { category: "hook", target_hints: [`Gemini-Setup/hooks/${filename}`] };
  if (sourcePath.includes("/scripts/")) return { category: "script", target_hints: [`Gemini-Setup/scripts/${filename}`] };
  return { category: "other", target_hints: ["Gemini-Setup/"] };
}

function scanDeltas() {
  const state = readJson(DEFAULT_STATE_PATH, DEFAULT_STATE);
  const range = state.last_reviewed_commit ? `${state.last_reviewed_commit}..HEAD` : "-n 50";
  const rawLog = runGit(["log", range, "--reverse", "--name-status", "--format=%x1e%H%x1f%cI%x1f%s", "--", ...TRACKED_PATHS]);

  const commits = rawLog.split("\u001e").filter(Boolean).map(chunk => {
    const lines = chunk.split("\n");
    const [sha, date, subject] = lines[0].split("\u001f");
    const changes = lines.slice(1).filter(Boolean).map(l => {
      const [status, path] = l.split("\t");
      return { status, path };
    }).filter(c => isPortableSourcePath(c.path));
    return { sha, date, subject, changes };
  }).filter(c => c.changes.length > 0);

  const candidates = commits.map(c => ({
    sha: c.sha,
    subject: c.subject,
    date: c.date,
    changes: c.changes.map(ch => ({ ...ch, classification: classifyPath(ch.path) })),
    strategy: "ADD" // Default for now
  }));

  return { bridge_id: BRIDGE_ID, candidates };
}

function main() {
  const result = scanDeltas();
  console.log("=== Codex Delta Audit f├╝r Gemini CLI ===");
  if (result.candidates.length === 0) {
    console.log("Keine neuen ├änderungen gefunden.");
  } else {
    result.candidates.forEach(c => {
      console.log(`\n[${c.sha.slice(0, 7)}] ${c.subject}`);
      c.changes.forEach(ch => {
        console.log(`  - ${ch.status} ${ch.path} -> Kategorie: ${ch.classification.category}`);
      });
    });
  }
}

main();
