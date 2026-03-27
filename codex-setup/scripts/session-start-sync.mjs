#!/usr/bin/env node

import { existsSync } from "node:fs";
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

function normalizeGitPath(rawPath, repoRoot) {
  const normalized = `${rawPath || ""}`.trim();
  if (!normalized) {
    return "";
  }

  const absolutePath = path.isAbsolute(normalized)
    ? normalized
    : path.resolve(repoRoot, normalized);
  return absolutePath.replace(/\\/g, "/");
}

function ensureGitHooksPath(repoRoot) {
  const hooksDir = path.join(repoRoot, "codex-setup", "hooks");
  const preCommitHook = path.join(hooksDir, "pre-commit");

  if (!existsSync(preCommitHook)) {
    throw new Error(`Missing Codex pre-commit hook: ${preCommitHook}`);
  }

  const result = spawnSync(
    "git",
    ["config", "--local", "--get", "core.hooksPath"],
    {
      cwd: repoRoot,
      encoding: "utf8",
      stdio: "pipe",
    },
  );

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    const stdout = `${result.stdout || ""}`.trim();
    const stderr = `${result.stderr || ""}`.trim();
    const unexpectedOutput = [stdout, stderr].filter(Boolean).join("\n");
    if (unexpectedOutput) {
      throw new Error(
        `git config --local --get core.hooksPath failed:\n${unexpectedOutput}`,
      );
    }
  }

  const currentHooksPath = normalizeGitPath(result.stdout, repoRoot);
  const desiredHooksPath = hooksDir.replace(/\\/g, "/");

  if (currentHooksPath !== desiredHooksPath) {
    runGit(["config", "--local", "core.hooksPath", desiredHooksPath], {
      cwd: repoRoot,
    });
    process.stdout.write(
      "[session-start-sync] core.hooksPath wurde auf den Codex-Hook-Pfad gesetzt.\n",
    );
    return;
  }

  process.stdout.write(
    "[session-start-sync] core.hooksPath zeigt bereits auf den Codex-Hook-Pfad.\n",
  );
}

function restoreMcpConfig(repoRoot) {
  const helperPath = path.join(
    __dirname,
    "restore-mcp-config.mjs",
  );
  const result = spawnSync(process.execPath, [helperPath], {
    cwd: repoRoot,
    encoding: "utf8",
    stdio: "pipe",
  });

  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    const stderr = `${result.stderr || ""}`.trim();
    const stdout = `${result.stdout || ""}`.trim();
    const details = [stdout, stderr].filter(Boolean).join("\n");
    throw new Error(
      `restore-mcp-config failed${details ? `:\n${details}` : ""}`,
    );
  }

  const output = `${result.stdout || ""}`.trim();
  if (output) {
    process.stdout.write(`${output}\n`);
  }
}

async function main() {
  const repoRoot = runGit(["rev-parse", "--show-toplevel"], {
    cwd: __dirname,
  }).trim();

  process.stdout.write(`[session-start-sync] Arbeitsbereich: ${repoRoot}\n`);
  process.stdout.write(
    "[session-start-sync] Stelle bei Bedarf den lokalen Git-Hook-Pfad wieder her...\n",
  );
  ensureGitHooksPath(repoRoot);
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

  process.stdout.write(
    "\n[session-start-sync] Stelle .mcp.json nach Plattform-Referenz wieder her...\n",
  );
  restoreMcpConfig(repoRoot);

  const status = runGit(["status", "--short"], { cwd: repoRoot });
  printBlock("[session-start-sync] Status nach dem Rebase", status);
}

main().catch((error) => {
  fail(`[session-start-sync] ${error.message}`);
});
