#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function fail(message) {
  process.stderr.write(`${message}\n`);
  process.exit(1);
}

function runGit(args, options = {}) {
  const result = spawnSync("git", args, {
    cwd: options.cwd,
    encoding: "utf8",
    stdio: options.stdio || "pipe",
  });

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    const stdout = `${result.stdout || ""}`.trim();
    const stderr = `${result.stderr || ""}`.trim();
    const details = [stdout, stderr].filter(Boolean).join("\n");
    throw new Error(
      `git ${args.join(" ")} failed${details ? `:\n${details}` : ""}`,
    );
  }

  return `${result.stdout || ""}`;
}

function printBlock(title, body) {
  process.stdout.write(`\n${title}\n`);
  const trimmed = `${body || ""}`.trim();
  process.stdout.write(`${trimmed || "(keine aenderungen)"}\n`);
}

async function main() {
  const repoRoot = runGit(["rev-parse", "--show-toplevel"], {
    cwd: __dirname,
  }).trim();

  process.stdout.write(`[session-start-sync] Arbeitsbereich: ${repoRoot}\n`);
  process.stdout.write(
    "[session-start-sync] Hole origin/main und zeige die Differenz vor dem Rebase...\n",
  );

  runGit(["fetch", "origin", "main"], { cwd: repoRoot });

  const incomingStat = runGit(["diff", "--stat", "HEAD..origin/main"], {
    cwd: repoRoot,
  });
  const incomingNames = runGit(
    ["diff", "--name-status", "HEAD..origin/main"],
    { cwd: repoRoot },
  );

  printBlock("[session-start-sync] Incoming diff --stat", incomingStat);
  printBlock("[session-start-sync] Incoming diff --name-status", incomingNames);

  process.stdout.write(
    "\n[session-start-sync] Rebase mit --autostash auf origin/main...\n",
  );
  runGit(["pull", "--rebase", "--autostash", "origin", "main"], {
    cwd: repoRoot,
    stdio: "inherit",
  });

  const status = runGit(["status", "--short"], { cwd: repoRoot });
  printBlock("[session-start-sync] Status nach dem Rebase", status);
}

main().catch((error) => {
  fail(`[session-start-sync] ${error.message}`);
});
