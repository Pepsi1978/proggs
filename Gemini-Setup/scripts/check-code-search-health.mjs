#!/usr/bin/env node

import { existsSync, readFileSync } from "fs";
import { join, resolve } from "path";
import { spawnSync } from "child_process";
import { fileURLToPath } from "url";

const DEFAULT_QUERY = "Oberste Direktive gemeinsame Memory-Datei";

function fail(message, details = "") {
	const suffix = details ? `\n${details}` : "";
	console.error(`${message}${suffix}`);
	process.exit(1);
}

function run(command, args, options = {}) {
	const result = spawnSync(command, args, {
		encoding: "utf8",
		...options,
	});

	if (result.error) {
		fail(`Failed to run ${command}.`, result.error.message);
	}

	return result;
}

function runSmokeTest(repoRoot, workspace, query) {
	const clientScript = join(repoRoot, "Gemini-Setup", "scripts", "code-search-mcp-client.mjs");
	const args = [clientScript, "smoke", "--workspace", workspace, "--query", query];
	const result = run(process.execPath, args);

	if (result.status !== 0) {
		fail("Direct code-search MCP smoke test failed.", (result.stderr || result.stdout || "").trim());
	}

	try {
		return JSON.parse(result.stdout);
	} catch (error) {
		fail("Direct code-search MCP smoke test returned invalid JSON.", result.stdout.trim());
	}
}

function main() {
	const scriptDir = fileURLToPath(new URL(".", import.meta.url));
	const repoRoot = "C:\\Users\\barwa\\GeminiCLI";
	const workspace = "C:\\Users\\barwa\\GeminiCLI";
	
    console.log(`Prüfe Code-Suche Gesundheit für ${workspace}...`);
    const smoke = runSmokeTest(repoRoot, workspace, DEFAULT_QUERY);
    
    console.log("- MCP konfiguriert: ja");
    console.log(`- Tools vorhanden: ${smoke.toolsOk ? "ja" : "nein"}`);
    console.log(`- Such-Status: ${smoke.statusOk ? "ok" : "fehlgeschlagen"}`);
    console.log(`- Test-Abfrage: ${smoke.queryOk ? "ok" : "fehlgeschlagen"}`);
    console.log(`- Top-Treffer: ${smoke.queryTopPath}`);
}

main();
