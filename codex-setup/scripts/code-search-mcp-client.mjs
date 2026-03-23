#!/usr/bin/env node

import { existsSync, readFileSync } from "fs";
import { spawn, spawnSync } from "child_process";
import { homedir } from "os";
import { dirname, isAbsolute, join, resolve } from "path";

const DEFAULT_QUERY = "Oberste Direktive gemeinsame Memory-Datei";
const DEFAULT_LIMIT = 1;
const REQUEST_TIMEOUT_MS = 10000;
const REQUIRED_TOOLS = ["index_codebase", "search_code", "search_status"];
const PROTOCOL_VERSIONS = ["2025-11-25", "2025-06-18", "2024-11-05"];

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
		"  code-search-mcp-client.mjs tools [--config <path>] [--json]",
		"  code-search-mcp-client.mjs call <tool> [--config <path>] [--args-json <json>] [--json]",
		"  code-search-mcp-client.mjs smoke [--config <path>] [--workspace <path>] [--query <text>] [--limit <n>] [--json]",
		"",
		"Directly connects to the local code-search MCP server without codex exec.",
	].join("\n");
}

function readOptionValue(argv, index, flag) {
	const value = argv[index + 1] ?? "";
	if (!value || value.startsWith("--")) {
		fail(`Missing value for ${flag}.`, usage());
	}
	return value;
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
		configPath: "",
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
			options.argsJson = readOptionValue(argv, index, arg);
			index++;
			continue;
		}
		if (arg === "--config") {
			options.configPath = readOptionValue(argv, index, arg);
			index++;
			continue;
		}
		if (arg === "--workspace") {
			options.workspace = readOptionValue(argv, index, arg);
			index++;
			continue;
		}
		if (arg === "--query") {
			options.query = readOptionValue(argv, index, arg);
			index++;
			continue;
		}
		if (arg === "--limit") {
			const rawValue = readOptionValue(argv, index, arg);
			index++;
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

function parseTomlInlineTable(rawValue) {
	const value = rawValue.trim();
	if (!value.startsWith("{") || !value.endsWith("}")) {
		throw new Error(`Unsupported TOML inline table: ${value}`);
	}

	const inner = value.slice(1, -1).trim();
	if (!inner) {
		return {};
	}

	const entries = [];
	let current = "";
	let inDoubleQuotes = false;
	let inSingleQuotes = false;

	for (const char of inner) {
		if (char === '"' && !inSingleQuotes) {
			inDoubleQuotes = !inDoubleQuotes;
		} else if (char === "'" && !inDoubleQuotes) {
			inSingleQuotes = !inSingleQuotes;
		}

		if (char === "," && !inDoubleQuotes && !inSingleQuotes) {
			entries.push(current.trim());
			current = "";
			continue;
		}

		current += char;
	}

	if (current.trim()) {
		entries.push(current.trim());
	}

	const parsed = {};
	for (const entry of entries) {
		const match = entry.match(/^([A-Za-z0-9_-]+)\s*=\s*(.+)$/);
		if (!match) {
			throw new Error(`Unsupported TOML inline table entry: ${entry}`);
		}

		parsed[match[1]] = parseTomlString(match[2]);
	}

	return parsed;
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

function parseTomlStringArray(initialValue, lines, startIndex) {
	let value = initialValue.trim();
	let endIndex = startIndex;

	if (value.startsWith("[") && !value.includes("]")) {
		const fragments = [value];
		while (endIndex + 1 < lines.length) {
			endIndex += 1;
			const nextLine = lines[endIndex].trim();
			if (!nextLine || nextLine.startsWith("#")) {
				continue;
			}
			fragments.push(nextLine);
			if (nextLine.includes("]")) {
				break;
			}
		}
		value = fragments.join(" ");
	}

	if (!value.startsWith("[") || !value.includes("]")) {
		throw new Error(`Unsupported TOML string array: ${initialValue}`);
	}

	const normalized = value.replace(/,\s*\]/g, "]");
	const parsed = JSON.parse(normalized);
	if (!Array.isArray(parsed) || !parsed.every((item) => typeof item === "string")) {
		fail("The code-search args in config.toml are not a string array.");
	}

	return {
		value: parsed,
		endIndex,
	};
}

function sanitizeConfigData(configPath, rawConfig) {
	if (!rawConfig || typeof rawConfig !== "object") {
		return null;
	}

	const configDir = dirname(configPath);
	const command = typeof rawConfig.command === "string" ? rawConfig.command.trim() : "";
	const args = Array.isArray(rawConfig.args) ? rawConfig.args : [];
	const cwd = typeof rawConfig.cwd === "string" ? rawConfig.cwd.trim() : "";
	const envSource =
		rawConfig.env && typeof rawConfig.env === "object" && !Array.isArray(rawConfig.env)
			? rawConfig.env
			: {};
	const env = Object.fromEntries(
		Object.entries(envSource)
			.filter(([key, value]) => typeof key === "string" && key)
			.map(([key, value]) => [key, String(value)]),
	);

	if (!command) {
		return null;
	}

	if (!args.every((value) => typeof value === "string")) {
		fail("The code-search args in config.toml are not a string array.");
	}

	return {
		configPath,
		command,
		args,
		cwd: cwd ? (isAbsolute(cwd) ? cwd : resolve(configDir, cwd)) : process.cwd(),
		env,
	};
}

function tryRun(command, args, options = {}) {
	return spawnSync(command, args, {
		encoding: "utf8",
		...options,
	});
}

function parseCodeSearchConfigWithPython(configPath) {
	const pythonScript = [
		"import json",
		"import pathlib",
		"import sys",
		"import tomllib",
		"data = tomllib.loads(pathlib.Path(sys.argv[1]).read_text(encoding='utf-8'))",
		"server = ((data.get('mcp_servers') or {}).get('code-search') or {})",
		"print(json.dumps(server))",
	].join(";");
	const candidates =
		process.platform === "win32"
			? [
					["py", ["-3", "-c", pythonScript, configPath]],
					["python", ["-c", pythonScript, configPath]],
					["python3", ["-c", pythonScript, configPath]],
				]
			: [
					["python3", ["-c", pythonScript, configPath]],
					["python", ["-c", pythonScript, configPath]],
				];

	for (const [command, args] of candidates) {
		const result = tryRun(command, args);
		if (result.error || result.status !== 0) {
			continue;
		}

		try {
			return sanitizeConfigData(configPath, JSON.parse(result.stdout));
		} catch {
			return null;
		}
	}

	return null;
}

function parseCodeSearchConfigFallback(configPath) {
	const configText = readFileSync(configPath, "utf8");
	const configDir = dirname(configPath);
	const lines = configText.split(/\r?\n/);
	let inCodeSearchSection = false;
	let command = "";
	let args = [];
	let cwd = "";
	let env = {};

	for (let index = 0; index < lines.length; index++) {
		const rawLine = lines[index];
		const line = rawLine.trim();
		if (!line || line.startsWith("#")) {
			continue;
		}

		const sectionMatch = line.match(/^\[(.+)\]$/);
		if (sectionMatch) {
			inCodeSearchSection =
				sectionMatch[1] === "mcp_servers.code-search" ||
				sectionMatch[1] === 'mcp_servers."code-search"' ||
				sectionMatch[1] === "mcp_servers.'code-search'";
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
			const parsedArgs = parseTomlStringArray(argsMatch[1], lines, index);
			args = parsedArgs.value;
			index = parsedArgs.endIndex;
			continue;
		}

		const cwdMatch = line.match(/^cwd\s*=\s*(.+)$/);
		if (cwdMatch) {
			cwd = parseTomlString(cwdMatch[1]);
			continue;
		}

		const envMatch = line.match(/^env\s*=\s*(.+)$/);
		if (envMatch) {
			env = parseTomlInlineTable(envMatch[1]);
		}
	}

	if (!command) {
		fail("The code-search MCP server is not configured in ~/.codex/config.toml.");
	}

	return sanitizeConfigData(configPath, {
		command,
		args,
		cwd,
		env,
	});
}

function parseCodeSearchConfig(configPathInput = "") {
	const configPath = resolve(configPathInput || join(homedir(), ".codex", "config.toml"));
	if (!existsSync(configPath)) {
		fail(`Codex config not found: ${configPath}`);
	}

	return parseCodeSearchConfigWithPython(configPath) ?? parseCodeSearchConfigFallback(configPath);
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
		return this.connectWithProtocol(PROTOCOL_VERSIONS[0]);
	}

	async connectWithProtocol(protocolVersion) {
		this.child = spawn(this.config.command, this.config.args, {
			cwd: this.config.cwd,
			env: {
				...process.env,
				...this.config.env,
			},
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
			protocolVersion,
			capabilities: {},
			clientInfo: {
				name: "codex-setup-code-search-client",
				version: "1.0.0",
			},
		});

		this.notify("notifications/initialized");
		this.protocolVersion = protocolVersion;
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

async function withClient(handler, options = {}) {
	const config = parseCodeSearchConfig(options.configPath ?? "");
	const errors = [];

	for (const protocolVersion of PROTOCOL_VERSIONS) {
		const client = new CodeSearchMcpClient(config);
		try {
			await client.connectWithProtocol(protocolVersion);
			return await handler(client, config, protocolVersion);
		} catch (error) {
			const details = error instanceof Error ? error.message : String(error);
			const stderr = client.stderrBuffer.trim();
			errors.push(
				[`protocol=${protocolVersion}`, details, stderr].filter(Boolean).join("\n"),
			);
		} finally {
			await client.close();
		}
	}

	fail("Direct code-search MCP client failed.", errors.join("\n\n"));
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
				protocolVersion: client.protocolVersion,
				tools: response.result?.tools ?? [],
			};
		}, options);

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
				protocolVersion: client.protocolVersion,
				tool: options.toolName,
				response,
				text: textFromToolResult(response),
			};
		}, options);

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
				protocolVersion: client.protocolVersion,
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
		}, options);

		if (options.json) {
			console.log(JSON.stringify(result, null, "\t"));
		} else {
			printSmokeResult(result);
		}
	}
}

const options = parseArgs(process.argv.slice(2));
await runCommand(options);
