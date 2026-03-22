#!/usr/bin/env node

import { existsSync, readFileSync } from "fs";
import { spawn, spawnSync } from "child_process";
import { fileURLToPath } from "url";
import { homedir } from "os";
import { join, resolve } from "path";

const DEFAULT_QUERY = "Oberste Direktive gemeinsame Memory-Datei";
const DEFAULT_LIMIT = 1;
const REQUEST_TIMEOUT_MS = 10000;
const REQUIRED_TOOLS = ["index_codebase", "search_code", "search_status"];

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

function usage() {
	return [
		"Usage:",
		"  code-search-mcp-client.mjs tools [--json]",
		"  code-search-mcp-client.mjs call <tool> [--args-json <json>] [--json]",
		"  code-search-mcp-client.mjs smoke [--workspace <path>] [--query <text>] [--limit <n>] [--json]",
		"",
		"Directly connects to the local code-search MCP server without codex exec.",
	].join("\n");
}

function parseArgs(argv) {
	const command = argv[0] ?? "";
	if (!command || command === "--help" || command === "-h") {
		console.log(usage());
		process.exit(0);
	}

	const options = {
		command,
		toolName: "",
		argsJson: "{}",
		workspace: "",
		query: DEFAULT_QUERY,
		limit: DEFAULT_LIMIT,
		json: false,
	};

	let index = 1;
	if (command === "call") {
		options.toolName = argv[index] ?? "";
		if (!options.toolName) {
			fail("The call command requires a tool name.", usage());
		}
		index++;
	}

	for (; index < argv.length; index++) {
		const arg = argv[index];
		if (arg === "--args-json") {
			options.argsJson = argv[++index] ?? "";
			continue;
		}
		if (arg === "--workspace") {
			options.workspace = argv[++index] ?? "";
			continue;
		}
		if (arg === "--query") {
			options.query = argv[++index] ?? "";
			continue;
		}
		if (arg === "--limit") {
			const rawValue = argv[++index] ?? "";
			const parsedLimit = Number.parseInt(rawValue, 10);
			if (!Number.isFinite(parsedLimit) || parsedLimit <= 0) {
				fail(`Invalid --limit value: ${rawValue}`);
			}
			options.limit = parsedLimit;
			continue;
		}
		if (arg === "--json") {
			options.json = true;
			continue;
		}
		fail(`Unknown argument: ${arg}`, usage());
	}

	if (command === "smoke" && !options.query.trim()) {
		fail("The smoke query must not be empty.");
	}

	if (!["tools", "call", "smoke"].includes(command)) {
		fail(`Unknown command: ${command}`, usage());
	}

	return options;
}

function parseTomlString(rawValue) {
	const value = rawValue.trim();
	if (value.startsWith('"')) {
		return JSON.parse(value);
	}
	if (value.startsWith("'") && value.endsWith("'")) {
		return value.slice(1, -1);
	}
	throw new Error(`Unsupported TOML string value: ${value}`);
}

function parseCodeSearchConfig() {
	const configPath = join(homedir(), ".codex", "config.toml");
	if (!existsSync(configPath)) {
		fail(`Codex config not found: ${configPath}`);
	}

	const configText = readFileSync(configPath, "utf8");
	let inCodeSearchSection = false;
	let command = "";
	let args = [];
	let cwd = "";

	for (const rawLine of configText.split(/\r?\n/)) {
		const line = rawLine.trim();
		if (!line || line.startsWith("#")) {
			continue;
		}

		const sectionMatch = line.match(/^\[(.+)\]$/);
		if (sectionMatch) {
			inCodeSearchSection = sectionMatch[1] === "mcp_servers.code-search";
			continue;
		}

		if (!inCodeSearchSection) {
			continue;
		}

		const commandMatch = line.match(/^command\s*=\s*(.+)$/);
		if (commandMatch) {
			command = parseTomlString(commandMatch[1]);
			continue;
		}

		const argsMatch = line.match(/^args\s*=\s*(.+)$/);
		if (argsMatch) {
			const parsedArgs = JSON.parse(argsMatch[1]);
			if (!Array.isArray(parsedArgs) || !parsedArgs.every((value) => typeof value === "string")) {
				fail("The code-search args in config.toml are not a string array.");
			}
			args = parsedArgs;
			continue;
		}

		const cwdMatch = line.match(/^cwd\s*=\s*(.+)$/);
		if (cwdMatch) {
			cwd = parseTomlString(cwdMatch[1]);
		}
	}

	if (!command) {
		fail("The code-search MCP server is not configured in ~/.codex/config.toml.");
	}

	return {
		configPath,
		command,
		args,
		cwd: cwd || process.cwd(),
	};
}

function resolveWorkspace(input) {
	if (!input) {
		const fallback = process.cwd();
		const gitResult = run("git", ["-C", fallback, "rev-parse", "--show-toplevel"]);
		return gitResult.status === 0 ? gitResult.stdout.trim() : resolve(fallback);
	}

	const requested = resolve(input);
	if (!existsSync(requested)) {
		fail(`Workspace path does not exist: ${requested}`);
	}

	const gitResult = run("git", ["-C", requested, "rev-parse", "--show-toplevel"]);
	return gitResult.status === 0 ? gitResult.stdout.trim() : requested;
}

function textFromToolResult(response) {
	const content = response?.result?.content;
	if (!Array.isArray(content)) {
		return "";
	}

	return content
		.filter((item) => item && item.type === "text" && typeof item.text === "string")
		.map((item) => item.text)
		.join("\n\n")
		.trim();
}

function topPathFromSearchResult(text) {
	const match = text.match(/\*\*(.+?)\*\*\s+lines\s+\d+-\d+\s+\[/);
	if (match) {
		return match[1];
	}

	if (text.includes("No results found")) {
		return "NONE";
	}

	return "";
}

class CodeSearchMcpClient {
	constructor(config) {
		this.config = config;
		this.child = null;
		this.nextId = 1;
		this.pending = new Map();
		this.stdoutBuffer = "";
		this.stderrBuffer = "";
		this.closed = false;
	}

	async connect() {
		this.child = spawn(this.config.command, this.config.args, {
			cwd: this.config.cwd,
			stdio: ["pipe", "pipe", "pipe"],
		});

		this.child.stdout.setEncoding("utf8");
		this.child.stderr.setEncoding("utf8");

		this.child.stdout.on("data", (chunk) => {
			this.stdoutBuffer += chunk;
			this.processStdout();
		});

		this.child.stderr.on("data", (chunk) => {
			this.stderrBuffer += chunk;
		});

		this.child.on("error", (error) => {
			this.rejectAllPending(`code-search MCP process error: ${error.message}`);
		});

		this.child.on("exit", (code, signal) => {
			const label = `code-search MCP exited early (${code ?? "null"}, ${signal ?? "no signal"})`;
			this.closed = true;
			this.rejectAllPending(label);
		});

		const initializeResponse = await this.request("initialize", {
			protocolVersion: "2025-11-25",
			capabilities: {},
			clientInfo: {
				name: "codex-setup-code-search-client",
				version: "1.0.0",
			},
		});

		this.notify("notifications/initialized");
		return initializeResponse;
	}

	processStdout() {
		while (true) {
			const newlineIndex = this.stdoutBuffer.indexOf("\n");
			if (newlineIndex === -1) {
				return;
			}

			const line = this.stdoutBuffer.slice(0, newlineIndex).replace(/\r$/, "");
			this.stdoutBuffer = this.stdoutBuffer.slice(newlineIndex + 1);
			if (!line.trim()) {
				continue;
			}

			let message;
			try {
				message = JSON.parse(line);
			} catch (error) {
				this.rejectAllPending(`Failed to parse MCP output: ${error instanceof Error ? error.message : String(error)}`);
				return;
			}

			if (Object.prototype.hasOwnProperty.call(message, "id")) {
				const pending = this.pending.get(message.id);
				if (!pending) {
					continue;
				}

				this.pending.delete(message.id);
				clearTimeout(pending.timer);

				if (message.error) {
					pending.reject(new Error(JSON.stringify(message.error)));
				} else {
					pending.resolve(message);
				}
			}
		}
	}

	send(payload) {
		if (!this.child || this.closed) {
			throw new Error("code-search MCP client is not connected.");
		}

		this.child.stdin.write(`${JSON.stringify(payload)}\n`);
	}

	notify(method, params = undefined) {
		this.send({
			jsonrpc: "2.0",
			method,
			...(params === undefined ? {} : { params }),
		});
	}

	request(method, params = undefined) {
		const id = this.nextId++;

		return new Promise((resolvePromise, rejectPromise) => {
			const timer = setTimeout(() => {
				this.pending.delete(id);
				rejectPromise(new Error(`Timed out waiting for ${method} response.`));
			}, REQUEST_TIMEOUT_MS);

			this.pending.set(id, {
				resolve: resolvePromise,
				reject: rejectPromise,
				timer,
			});

			this.send({
				jsonrpc: "2.0",
				id,
				method,
				...(params === undefined ? {} : { params }),
			});
		});
	}

	async listTools() {
		return this.request("tools/list", {});
	}

	async callTool(name, args) {
		return this.request("tools/call", {
			name,
			arguments: args,
		});
	}

	rejectAllPending(message) {
		const error = new Error([message, this.stderrBuffer.trim()].filter(Boolean).join("\n"));
		for (const [id, pending] of this.pending.entries()) {
			clearTimeout(pending.timer);
			pending.reject(error);
			this.pending.delete(id);
		}
	}

	async close() {
		if (!this.child || this.closed) {
			return;
		}

		this.closed = true;
		this.child.kill("SIGTERM");
	}
}

async function withClient(handler) {
	const config = parseCodeSearchConfig();
	const client = new CodeSearchMcpClient(config);

	try {
		await client.connect();
		return await handler(client, config);
	} catch (error) {
		const details = error instanceof Error ? error.message : String(error);
		const stderr = client.stderrBuffer.trim();
		fail("Direct code-search MCP client failed.", [details, stderr].filter(Boolean).join("\n"));
	} finally {
		await client.close();
	}
}

function printTools(result) {
	for (const tool of result.tools) {
		console.log(tool.name);
	}
}

function printCallResult(result) {
	const text = textFromToolResult(result);
	if (text) {
		console.log(text);
		return;
	}

	console.log(JSON.stringify(result, null, "\t"));
}

function printSmokeResult(result) {
	const lines = [
		`code-search MCP smoke for ${result.workspace}`,
		`- Config: ${result.configPath}`,
		`- Tools present: ${result.toolsOk ? "yes" : "no"}`,
		`- search_status: ${result.statusOk ? "ok" : "failed"}`,
		`- search_code: ${result.queryOk ? "ok" : "failed"}`,
		`- Query top hit: ${result.queryTopPath || "NONE"}`,
	];

	console.log(lines.join("\n"));
}

async function runCommand(options) {
	if (options.command === "tools") {
		const result = await withClient(async (client, config) => {
			const response = await client.listTools();
			return {
				configPath: config.configPath,
				tools: response.result?.tools ?? [],
			};
		});

		if (options.json) {
			console.log(JSON.stringify(result, null, "\t"));
		} else {
			printTools(result);
		}
		return;
	}

	if (options.command === "call") {
		let parsedArgs = {};
		try {
			parsedArgs = JSON.parse(options.argsJson);
		} catch (error) {
			fail(
				`Invalid --args-json payload: ${options.argsJson}`,
				error instanceof Error ? error.message : String(error),
			);
		}

		const result = await withClient(async (client, config) => {
			const response = await client.callTool(options.toolName, parsedArgs);
			return {
				configPath: config.configPath,
				tool: options.toolName,
				response,
				text: textFromToolResult(response),
			};
		});

		if (options.json) {
			console.log(JSON.stringify(result, null, "\t"));
		} else {
			printCallResult(result.response);
		}
		return;
	}

	if (options.command === "smoke") {
		const workspace = resolveWorkspace(options.workspace);
		const result = await withClient(async (client, config) => {
			const toolsResponse = await client.listTools();
			const toolNames = Array.isArray(toolsResponse.result?.tools)
				? toolsResponse.result.tools.map((tool) => tool.name)
				: [];
			const toolsOk = REQUIRED_TOOLS.every((name) => toolNames.includes(name));

			const statusResponse = await client.callTool("search_status", {
				directory: workspace,
			});
			const queryResponse = await client.callTool("search_code", {
				directory: workspace,
				query: options.query,
				limit: options.limit,
			});

			const statusText = textFromToolResult(statusResponse);
			const queryText = textFromToolResult(queryResponse);
			const queryTopPath = topPathFromSearchResult(queryText);
			const statusOk = statusText.startsWith(`Index status for ${workspace}:`);
			const queryOk = Boolean(queryText) && queryTopPath !== "NONE";

			return {
				configPath: config.configPath,
				workspace,
				query: options.query,
				limit: options.limit,
				toolNames,
				toolsOk,
				statusOk,
				queryOk,
				statusText,
				queryText,
				queryTopPath: queryTopPath || "NONE",
			};
		});

		if (options.json) {
			console.log(JSON.stringify(result, null, "\t"));
		} else {
			printSmokeResult(result);
		}
	}
}

const options = parseArgs(process.argv.slice(2));
await runCommand(options);
