#!/usr/bin/env node

import {
  BRIDGE_REGISTRY_REPO_PATH,
  REPO_ROOT,
  deriveGithubBlobUrl,
  loadBridgeContext,
  loadBridgeRegistry,
} from "./bridge-registry.mjs";

function parseArgs(argv) {
  const args = { json: false };
  for (const arg of argv) {
    if (arg === "--json") args.json = true;
  }
  return args;
}

function ensureArray(value) {
  return Array.isArray(value) ? value.filter(Boolean) : [];
}

function bridgeEntry(context) {
  return {
    bridge_files: context.bridgeFiles,
    state_file: context.bridgeEntry.state_file,
    audit_title: context.auditTitle,
    trigger_phrases: context.triggerPhrases,
    source_paths: context.sourcePaths,
    audit_git_paths: context.auditGitPaths,
    reference_implementation: ensureArray(context.bridgeJson.reference_implementation),
  };
}

function buildReport() {
  const registry = loadBridgeRegistry();
  const claudeContext = loadBridgeContext("claude-delta");
  const geminiContext = loadBridgeContext("gemini-delta");
  return {
    generated_at: new Date().toISOString(),
    report_kind: "codex-bootstrap-report",
    source_cli: "Codex",
    registry_path: BRIDGE_REGISTRY_REPO_PATH,
    registry_github_url: registry.github_url || deriveGithubBlobUrl("https://github.com/Pepsi1978/proggs/blob/main/codex-setup/bridges/bridge-registry.json", BRIDGE_REGISTRY_REPO_PATH),
    repo_root: REPO_ROOT,
    codex: {
      bootstrap_setup: registry.bootstrap_artifacts?.Codex || {},
      bootstrap_report: registry.bootstrap_report_artifacts?.Codex || {},
      ledgers: registry.ledger_addresses || {},
      bridges: {
        claude_delta: bridgeEntry(claudeContext),
        gemini_delta: bridgeEntry(geminiContext),
      },
    },
    read_only_peers: registry.peer_registry_targets || {},
  };
}

function renderList(label, values, lines) {
  const items = ensureArray(values);
  if (items.length === 0) {
    lines.push(label + ": -");
    return;
  }
  lines.push(label + ":");
  for (const item of items) lines.push("  - " + item);
}

function render(report) {
  const lines = [
    "# Codex Bootstrap Report",
    "Generated: " + report.generated_at,
    "Registry: " + report.registry_path,
    "Registry GitHub: " + (report.registry_github_url || "-"),
    "Repo root: " + report.repo_root,
    "",
    "## Codex",
  ];
  renderList("Bootstrap", report.codex.bootstrap_setup.repo_scripts, lines);
  renderList("Bootstrap report", report.codex.bootstrap_report.repo_scripts, lines);
  lines.push("Ledgers:");
  for (const [key, value] of Object.entries(report.codex.ledgers)) {
    lines.push("  - " + key + ": " + value);
  }
  lines.push("", "## Bridges");
  for (const [key, value] of Object.entries(report.codex.bridges)) {
    lines.push("### " + key);
    renderList("Bridge files", value.bridge_files, lines);
    lines.push("State file: " + (value.state_file || "-"));
    renderList("Audit paths", value.audit_git_paths, lines);
    renderList("Triggers", value.trigger_phrases, lines);
  }
  return lines.join("\n") + "\n";
}

const args = parseArgs(process.argv.slice(2));
const report = buildReport();
process.stdout.write(args.json ? JSON.stringify(report, null, 2) + "\n" : render(report));
