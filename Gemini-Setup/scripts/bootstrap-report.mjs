#!/usr/bin/env node

import {
  BRIDGE_REGISTRY_REPO_PATH,
  REPO_ROOT,
  loadBridgeContext,
  loadBridgeRegistry,
  readJson,
  resolveRepoPath,
} from "./bridge-registry.mjs";

function buildReport() {
  const registry = loadBridgeRegistry();
  const codexContext = loadBridgeContext("codex-delta");

  const report = {
    generated_at: new Date().toISOString(),
    report_kind: "bootstrap-report",
    source_cli: "Gemini CLI",
    registry_path: BRIDGE_REGISTRY_REPO_PATH,
    self: registry.self_registration["Gemini CLI"],
    ledgers: registry.ledger_addresses || registry.ledgers,
    bridges: {
      codex_delta: codexContext
    }
  };

  return report;
}

function main() {
  const report = buildReport();
  if (process.argv.includes("--json")) {
    console.log(JSON.stringify(report, null, 2));
  } else {
    console.log("=== Gemini CLI Bootstrap Report ===");
    console.log(`Generiert am: ${report.generated_at}`);
    console.log(`Registry Pfad: ${report.registry_path}`);
    console.log("\n--- Self Registration ---");
    console.log(`Lokal: ${report.self.local_path}`);
    console.log(`Status: ${report.self.status}`);
    console.log("\n--- Ledgers ---");
    console.log(`Gemini Fixes: ${report.ledgers.gemini_fixes}`);
    console.log(`Gemini Suggestions: ${report.ledgers.gemini_suggestions}`);
    console.log("\n--- Active Bridges ---");
    console.log(`Bridge: codex-delta`);
    console.log(`  Quelle: ${report.bridges.codex_delta.sourceLabel}`);
    console.log(`  Trigger: ${report.bridges.codex_delta.triggerPhrases.join(", ")}`);
  }
}

main();
