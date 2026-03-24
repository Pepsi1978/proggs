#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const REPO_ROOT = "C:\\Users\\barwa\\GeminiCLI";
const MCP_CONFIG_PATH = path.join(REPO_ROOT, ".mcp.json");

function log(msg) { console.log(`[mcp-path-check] ${msg}`); }
function warn(msg) { console.error(`⚠️ [mcp-path-check] WARNUNG: ${msg}`); }

if (!fs.existsSync(MCP_CONFIG_PATH)) {
  log("Keine .mcp.json gefunden. Überspringe Check.");
  process.exit(0);
}

try {
  const config = JSON.parse(fs.readFileSync(MCP_CONFIG_PATH, "utf8"));
  let issues = 0;

  for (const [name, server] of Object.entries(config.mcpServers || {})) {
    // 1. Check command path
    if (server.command && server.command.startsWith("/")) {
      if (!fs.existsSync(server.command)) {
        warn(`Server "${name}": Command-Pfad existiert nicht: ${server.command}`);
        issues++;
      }
    }

    // 2. Check args for absolute paths
    if (Array.isArray(server.args)) {
      server.args.forEach(arg => {
        if (typeof arg === "string" && arg.startsWith("/") && arg.includes("/Users/")) {
          if (!arg.startsWith(REPO_ROOT)) {
            warn(`Server "${name}": Absoluter Pfad außerhalb des aktuellen Workspace gefunden: ${arg}`);
            issues++;
          } else if (!fs.existsSync(arg)) {
            warn(`Server "${name}": Pfad existiert nicht: ${arg}`);
            issues++;
          }
        }
      });
    }

    // 3. Check env vars
    if (server.env) {
      for (const [key, value] of Object.entries(server.env)) {
        if (typeof value === "string" && value.startsWith("/") && value.includes("/Users/")) {
          if (!value.startsWith(REPO_ROOT)) {
            warn(`Server "${name}": Env-Var ${key} zeigt außerhalb des Workspace: ${value}`);
            issues++;
          } else if (!fs.existsSync(value)) {
            warn(`Server "${name}": Env-Var ${key} Pfad existiert nicht: ${value}`);
            issues++;
          }
        }
      }
    }
  }

  if (issues === 0) {
    log("Alle MCP-Pfade sind valide.");
  } else {
    log(`${issues} Problem(e) in .mcp.json gefunden.`);
    process.exit(1);
  }
} catch (e) {
  warn(`Fehler beim Parsen der .mcp.json: ${e.message}`);
  process.exit(1);
}
