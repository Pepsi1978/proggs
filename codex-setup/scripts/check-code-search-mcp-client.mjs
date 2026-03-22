#!/usr/bin/env node

import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "fs";
import { tmpdir } from "os";
import { join, resolve } from "path";
import { spawnSync } from "child_process";
import { fileURLToPath } from "url";

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

const scriptDir = fileURLToPath(new URL(".", import.meta.url));
const repoRoot = resolve(join(scriptDir, "..", ".."));
const clientScript = join(scriptDir, "code-search-mcp-client.mjs");
const tempDir = mkdtempSync(join(tmpdir(), "code-search-mcp-client-"));

try {
	const fakeServerPath = join(tempDir, "fake-server.mjs");
	const fakeConfigPath = join(tempDir, "config.toml");
	const fakeWorkspace = join(tempDir, "workspace");
	mkdirSync(fakeWorkspace, { recursive: true });

	writeFileSync(
		fakeServerPath,
		[
			"import process from 'node:process';",
			"let initialized = false;",
			"process.stdin.setEncoding('utf8');",
			"let buffer = '';",
			"function send(message) { process.stdout.write(JSON.stringify(message) + '\\n'); }",
			"function tools() {",
			"  return [",
			"    { name: 'index_codebase' },",
			"    { name: 'search_code' },",
			"    { name: 'search_status' },",
			"  ];",
			"}",
			"function textResponse(id, text) {",
			"  send({ jsonrpc: '2.0', id, result: { content: [{ type: 'text', text }] } });",
			"}",
			"process.stdin.on('data', (chunk) => {",
			"  buffer += chunk;",
			"  while (true) {",
			"    const index = buffer.indexOf('\\n');",
			"    if (index === -1) break;",
			"    const line = buffer.slice(0, index).replace(/\\r$/, '');",
			"    buffer = buffer.slice(index + 1);",
			"    if (!line.trim()) continue;",
			"    const msg = JSON.parse(line);",
			"    if (msg.method === 'initialize') {",
			"      if (process.env.FAKE_MCP_TOKEN !== 'ok') {",
			"        send({ jsonrpc: '2.0', id: msg.id, error: { code: -32000, message: 'missing env' } });",
			"        continue;",
			"      }",
			"      send({ jsonrpc: '2.0', id: msg.id, result: { protocolVersion: msg.params.protocolVersion, capabilities: { tools: {} }, serverInfo: { name: 'fake-code-search', version: '1.0.0' } } });",
			"      continue;",
			"    }",
			"    if (msg.method === 'notifications/initialized') { initialized = true; continue; }",
			"    if (!initialized) {",
			"      if (Object.prototype.hasOwnProperty.call(msg, 'id')) send({ jsonrpc: '2.0', id: msg.id, error: { code: -32001, message: 'not initialized' } });",
			"      continue;",
			"    }",
			"    if (msg.method === 'tools/list') {",
			"      send({ jsonrpc: '2.0', id: msg.id, result: { tools: tools() } });",
			"      continue;",
			"    }",
			"    if (msg.method === 'tools/call') {",
			"      const name = msg.params?.name;",
			"      if (name === 'search_status') {",
			"        textResponse(msg.id, `Index status for ${msg.params.arguments.directory}:\\n- Files indexed: 3\\n- Code chunks: 6\\n- Database: ${process.cwd()}/.code-search/index-test.db\\n- Ollama model: nomic-embed-text (768 dimensions)\\n- Last successful reindex: 2099-01-01T00:00:00.000Z\\n- Last mode: noop\\n- Last write at: 2099-01-01T00:00:00.000Z\\n- Last write mode: incremental`);",
			"        continue;",
			"      }",
			"      if (name === 'search_code') {",
			"        textResponse(msg.id, '### Result 1 (score: 0.999)\\n**/tmp/fake/path.ts** lines 1-2 [ts]\\n```\\nconst ok = true;\\n```');",
			"        continue;",
			"      }",
			"      textResponse(msg.id, `Unhandled tool: ${name}`);",
			"    }",
			"  }",
			"});",
		].join("\n"),
	);

	writeFileSync(
		fakeConfigPath,
		[
			'[mcp_servers."code-search"]',
			`command = ${JSON.stringify(process.execPath)}`,
			"args = [",
			`  ${JSON.stringify(fakeServerPath)},`,
			"]",
			`cwd = ${JSON.stringify(tempDir)}`,
			'env = { FAKE_MCP_TOKEN = "ok" }',
		].join("\n"),
	);

	const toolsResult = run(process.execPath, [
		clientScript,
		"tools",
		"--config",
		fakeConfigPath,
		"--json",
	], { cwd: repoRoot });
	if (toolsResult.status !== 0) {
		fail("Direct code-search MCP client tools self-test failed.", toolsResult.stderr || toolsResult.stdout);
	}

	const smokeResult = run(process.execPath, [
		clientScript,
		"smoke",
		"--config",
		fakeConfigPath,
		"--workspace",
		fakeWorkspace,
		"--json",
	], { cwd: repoRoot });
	if (smokeResult.status !== 0) {
		fail("Direct code-search MCP client smoke self-test failed.", smokeResult.stderr || smokeResult.stdout);
	}

	let parsedTools;
	let parsedSmoke;
	try {
		parsedTools = JSON.parse(toolsResult.stdout);
		parsedSmoke = JSON.parse(smokeResult.stdout);
	} catch (error) {
		fail(
			"Direct code-search MCP client self-test returned invalid JSON.",
			error instanceof Error ? error.message : String(error),
		);
	}

	const toolNames = Array.isArray(parsedTools.tools)
		? parsedTools.tools.map((tool) => tool.name)
		: [];

	if (!toolNames.includes("search_status") || !toolNames.includes("search_code")) {
		fail("Direct code-search MCP client self-test did not list the expected tools.");
	}

	if (!parsedSmoke.toolsOk || !parsedSmoke.statusOk || !parsedSmoke.queryOk) {
		fail("Direct code-search MCP client self-test returned a failing smoke payload.", smokeResult.stdout);
	}

	if (parsedSmoke.queryTopPath !== "/tmp/fake/path.ts") {
		fail("Direct code-search MCP client self-test did not parse the top search result path correctly.", smokeResult.stdout);
	}

	console.log("direct code-search MCP client self-test passed");
} finally {
	rmSync(tempDir, { recursive: true, force: true });
}
