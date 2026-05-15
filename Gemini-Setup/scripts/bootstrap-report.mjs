#!/usr/bin/env node

import {
  BRIDGE_REGISTRY_REPO_PATH,
  REPO_ROOT,
  loadBridgeContext,
  loadBridgeRegistry,
  readJson,
  resolveRepoPath,
} from "./bridge-registry.mjs";
import path from "node:path";
import fs from "node:fs";

function buildReport() {
  const registry = loadBridgeRegistry();
  const codexContext = loadBridgeContext("codex-delta");
  const claudeContext = loadBridgeContext("claude-code-delta");

  // Lade die neuesten umgesetzten Vorschlaege fuer die Portierung
  const suggestionsPath = path.join(REPO_ROOT, "Gemini-Setup", "state", "implemented-intelligence-suggestions.json");
  const suggestions = fs.existsSync(suggestionsPath) ? JSON.parse(fs.readFileSync(suggestionsPath, "utf8")) : { entries: [] };
  
  // Nimm nur die letzten 5 Vorschlaege fuer den Report
  const recentSuggestions = suggestions.entries.slice(-5).map(s => ({
    id: s.id,
    summary: s.summary,
    adoption_guidance: s.adoption_guidance || "Keine Portierungshinweise hinterlegt."
  }));

  const report = {
    generated_at: new Date().toISOString(),
    report_kind: "bootstrap-report",
    source_cli: "Gemini CLI",
    registry_path: BRIDGE_REGISTRY_REPO_PATH,
    self: registry.self_registration["Gemini CLI"],
    ledgers: registry.ledger_addresses || registry.ledgers,
    bridges: {
      codex_delta: codexContext,
      claude_code_delta: claudeContext
    },
    porting_candidates: recentSuggestions
  };

  return report;
}

function main() {
  const report = buildReport();
  if (process.argv.includes("--json")) {
    console.log(JSON.stringify(report, null, 2));
  } else {
    console.log("=== ­ƒÆí Gemini CLI Bootstrap & Porting Report ===");
    console.log(`Generiert am: ${report.generated_at}`);
    console.log(`Registry Pfad: ${report.registry_path}`);
    
    console.log("\n--- ­ƒÜÇ Portierungshilfe fuer Partner-CLIs (Claude/Codex) ---");
    if (report.porting_candidates.length > 0) {
        report.porting_candidates.forEach(s => {
            console.log(`[${s.id}] ${s.summary}`);
            console.log(`  ­ƒæë Portierung: ${s.adoption_guidance}`);
        });
    } else {
        console.log("Keine neuen Portierungskandidaten gefunden.");
    }

    console.log("\n--- Self Registration ---");
    console.log(`Lokal: ${report.self.local_path}`);
    console.log(`Status: ${report.self.status}`);
    
    console.log("\n--- Active Bridges ---");
    console.log(`Bridge: codex-delta (${report.bridges.codex_delta.sourceLabel})`);
    console.log(`Bridge: claude-code-delta (${report.bridges.claude_code_delta.sourceLabel})`);
  }
}

main();
