#!/usr/bin/env node

import { existsSync, readFileSync, renameSync, unlinkSync, writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function fail(message) {
  process.stderr.write(`${message}\n`);
  process.exit(1);
}

function runGit(args, cwd) {
  const result = spawnSync("git", args, {
    cwd,
    encoding: "utf8",
    stdio: "pipe",
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    const stderr = `${result.stderr || ""}`.trim();
    const stdout = `${result.stdout || ""}`.trim();
    throw new Error(
      `git ${args.join(" ")} failed${stderr || stdout ? `:\n${[stdout, stderr].filter(Boolean).join("\n")}` : ""}`,
    );
  }
  return `${result.stdout || ""}`.trim();
}

function restoreFile(referencePath, targetPath) {
  const reference = readFileSync(referencePath, "utf8");
  let current = "";
  try {
    current = readFileSync(targetPath, "utf8");
  } catch {
    current = "";
  }

  if (current === reference) {
    return false;
  }

  const tempPath = `${targetPath}.tmp`;
  writeFileSync(tempPath, reference, "utf8");
  if (existsSync(targetPath)) {
    unlinkSync(targetPath);
  }
  renameSync(tempPath, targetPath);
  return true;
}

async function main() {
  const repoRoot = runGit(["rev-parse", "--show-toplevel"], __dirname);
  if (process.platform !== "win32") {
    process.stdout.write("[restore-mcp-config] Non-Windows platform, no restore needed.\n");
    return;
  }

  const referencePath = path.join(repoRoot, "codex-setup", "mcp-windows.json");
  const targetPath = path.join(repoRoot, ".mcp.json");

  if (!path.isAbsolute(referencePath) || !path.isAbsolute(targetPath)) {
    fail("[restore-mcp-config] Failed to resolve .mcp.json paths.");
  }

  const changed = restoreFile(referencePath, targetPath);
  process.stdout.write(
    changed
      ? "[restore-mcp-config] Restored .mcp.json from codex-setup/mcp-windows.json.\n"
      : "[restore-mcp-config] .mcp.json already matches codex-setup/mcp-windows.json.\n",
  );
}

main().catch((error) => {
  fail(`[restore-mcp-config] ${error.message}`);
});
