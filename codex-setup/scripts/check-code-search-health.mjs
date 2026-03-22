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

function parseArgs(argv) {
	const options = {
		configPath: "",
		workspace: "",
		query: DEFAULT_QUERY,
		json: false,
	};

	const readOptionValue = (index, flag) => {
		const value = argv[index + 1] ?? "";
		if (!value || value.startsWith("--")) {
			fail(`Missing value for ${flag}.`);
		}
		return value;
	};

	for (let index = 0; index < argv.length; index++) {
		const arg = argv[index];
		if (arg === "--workspace") {
			options.workspace = readOptionValue(index, arg);
			index++;
			continue;
		}
		if (arg === "--config") {
			options.configPath = readOptionValue(index, arg);
			index++;
			continue;
		}
		if (arg === "--query") {
			options.query = readOptionValue(index, arg);
			index++;
			continue;
		}
		if (arg === "--json") {
			options.json = true;
			continue;
		}
		if (arg === "--help" || arg === "-h") {
			console.log(
				[
					"Usage: check-code-search-health.mjs [--workspace <path>] [--config <path>] [--query <text>] [--json]",
					"",
					"Checks the local code-search index metadata and runs a fresh direct MCP smoke test.",
				].join("\n"),
			);
			process.exit(0);
		}
		fail(`Unknown argument: ${arg}`);
	}

	if (!options.query.trim()) {
		fail("The test query must not be empty.");
	}

	return options;
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

function resolveWorkspace(input, fallbackWorkspace) {
	const requested = input ? resolve(input) : fallbackWorkspace;

	if (!existsSync(requested)) {
		fail(`Workspace path does not exist: ${requested}`);
	}

	const gitResult = run("git", ["-C", requested, "rev-parse", "--show-toplevel"]);
	if (gitResult.status === 0) {
		return gitResult.stdout.trim();
	}

	return requested;
}

function readJson(filePath) {
	try {
		return JSON.parse(readFileSync(filePath, "utf8"));
	} catch {
		return null;
	}
}

function parseLastWriteInfo(logText, state) {
	let currentCheckAt = null;
	let lastWriteMode = null;
	let lastWriteAt = null;

	for (const rawLine of logText.split(/\r?\n/)) {
		const line = rawLine.trim();
		const checkMatch = line.match(/^\[(.+?)\]\s+Codex auto-reindex check for /);
		if (checkMatch) {
			currentCheckAt = checkMatch[1];
			continue;
		}

		if (line === "Incremental reindex complete.") {
			lastWriteMode = "incremental";
			lastWriteAt = currentCheckAt;
			continue;
		}

		if (line === "Full reindex complete.") {
			lastWriteMode = "full";
			lastWriteAt = currentCheckAt;
		}
	}

	if (!lastWriteMode && state && (state.lastMode === "incremental" || state.lastMode === "full")) {
		lastWriteMode = state.lastMode;
		lastWriteAt = state.lastSuccessAt ?? null;
	}

	return {
		lastWriteMode,
		lastWriteAt,
		lastWriteSuccessful: Boolean(lastWriteMode && lastWriteAt),
		lastWriteIncremental: lastWriteMode === "incremental",
	};
}

function readLocalIndexState(workspace) {
	const dbDir = join(workspace, ".code-search");
	const statePath = join(dbDir, "state.json");
	const logPath = join(dbDir, "reindex.log");
	const pointerPath = join(dbDir, "current.txt");
	const state = existsSync(statePath) ? readJson(statePath) : null;
	const logText = existsSync(logPath) ? readFileSync(logPath, "utf8") : "";
	const currentDb = existsSync(pointerPath)
		? readFileSync(pointerPath, "utf8").trim() || null
		: state?.activeDb ?? null;
	const lastWrite = parseLastWriteInfo(logText, state);

	return {
		dbDir,
		activeDb: currentDb,
		totalFiles: state?.totalFiles ?? null,
		totalChunks: state?.totalChunks ?? null,
		lastMode: state?.lastMode ?? null,
		lastSuccessAt: state?.lastSuccessAt ?? null,
		lastRunSuccessful: Boolean(state?.lastSuccessAt),
		lastRunIncremental: state?.lastMode === "incremental",
		...lastWrite,
	};
}

function parseStatusSnapshot(statusText) {
	const readField = (label) => {
		const match = statusText.match(new RegExp(`^- ${label}:\\s+(.+)$`, "m"));
		return match ? match[1].trim() : null;
	};

	const readNumberField = (label) => {
		const value = readField(label);
		if (!value) {
			return null;
		}

		const parsed = Number.parseInt(value, 10);
		return Number.isFinite(parsed) ? parsed : null;
	};

	return {
		databasePath: readField("Database"),
		filesIndexed: readNumberField("Files indexed"),
		codeChunks: readNumberField("Code chunks"),
		lastMode: readField("Last mode"),
		lastWriteMode: readField("Last write mode"),
		lastWriteAt: readField("Last write at"),
	};
}

function runSmokeTest(repoRoot, workspace, query, configPath = "") {
	const clientScript = join(repoRoot, "codex-setup", "scripts", "code-search-mcp-client.mjs");
	const args = [clientScript, "smoke", "--workspace", workspace, "--query", query, "--json"];
	if (configPath) {
		args.push("--config", configPath);
	}
	const result = run(process.execPath, args);

	if (result.status !== 0) {
		fail(
			"Direct code-search MCP smoke test failed.",
			(result.stderr || result.stdout || "").trim(),
		);
	}

	try {
		return JSON.parse(result.stdout);
	} catch (error) {
		fail(
			"Direct code-search MCP smoke test returned invalid JSON.",
			[
				error instanceof Error ? error.message : String(error),
				result.stdout.trim(),
			]
				.filter(Boolean)
				.join("\n"),
		);
	}
}

function buildReport(options) {
	const scriptDir = fileURLToPath(new URL(".", import.meta.url));
	const repoRoot = resolve(join(scriptDir, "..", ".."));
	const workspace = resolveWorkspace(options.workspace, repoRoot);
	const smoke = runSmokeTest(repoRoot, workspace, options.query, options.configPath);
	const local = readLocalIndexState(workspace);
	const mcpSnapshot = parseStatusSnapshot(smoke.statusText);
	const normalizePath = (value) => (typeof value === "string" ? value.replace(/\\/g, "/") : value);
	const normalizedMcpDatabasePath = normalizePath(mcpSnapshot.databasePath);
	const normalizedActiveDb = normalizePath(local.activeDb);
	const mcpStatusMatchesLocal =
		(normalizedMcpDatabasePath?.endsWith(`/${normalizedActiveDb}`) ?? normalizedMcpDatabasePath === normalizedActiveDb) &&
		(mcpSnapshot.filesIndexed === null || local.totalFiles === null || mcpSnapshot.filesIndexed === local.totalFiles) &&
		(mcpSnapshot.codeChunks === null || local.totalChunks === null || mcpSnapshot.codeChunks === local.totalChunks);
	const report = {
		workspace,
		query: options.query,
		mcpConfigured: true,
		directClientOk: smoke.toolsOk,
		searchStatusOk: smoke.statusOk,
		queryOk: smoke.queryOk,
		queryTopPath: smoke.queryTopPath,
		toolNames: smoke.toolNames,
		protocolVersion: smoke.protocolVersion,
		dbDir: local.dbDir,
		activeDb: local.activeDb,
		totalFiles: local.totalFiles,
		totalChunks: local.totalChunks,
		lastMode: local.lastMode,
		lastSuccessAt: local.lastSuccessAt,
		lastRunSuccessful: local.lastRunSuccessful,
		lastRunIncremental: local.lastRunIncremental,
		lastWriteMode: local.lastWriteMode,
		lastWriteAt: local.lastWriteAt,
		lastWriteSuccessful: local.lastWriteSuccessful,
		lastWriteIncremental: local.lastWriteIncremental,
		statusText: smoke.statusText,
		queryText: smoke.queryText,
		mcpDatabasePath: mcpSnapshot.databasePath,
		mcpFilesIndexed: mcpSnapshot.filesIndexed,
		mcpCodeChunks: mcpSnapshot.codeChunks,
		mcpStatusMatchesLocal,
	};

	if (!report.directClientOk || !report.searchStatusOk || !report.queryOk) {
		fail(
			"code-search MCP smoke test reported a failing status.",
			JSON.stringify(report, null, 2),
		);
	}

	return report;
}

function printHumanReport(report) {
	const lines = [
		`code-search health for ${report.workspace}`,
		`- MCP configured: yes`,
		`- Direct MCP client: ${report.directClientOk ? "ok" : "failed"}`,
		`- Protocol version: ${report.protocolVersion ?? "unknown"}`,
		`- Fresh search_status: ok`,
		`- Fresh test query: ok`,
		`- Active DB: ${report.activeDb ?? "unknown"}`,
		`- Files indexed: ${report.totalFiles ?? "unknown"}`,
		`- Code chunks: ${report.totalChunks ?? "unknown"}`,
		`- Last successful run: ${report.lastSuccessAt ?? "none"}`,
		`- Last run mode: ${report.lastMode ?? "none"}`,
		`- Last run successful: ${report.lastRunSuccessful ? "yes" : "no"}`,
		`- Last run incremental: ${report.lastRunIncremental ? "yes" : "no"}`,
		`- Last write at: ${report.lastWriteAt ?? "none"}`,
		`- Last write mode: ${report.lastWriteMode ?? "none"}`,
		`- Last write successful: ${report.lastWriteSuccessful ? "yes" : "no"}`,
		`- Last write incremental: ${report.lastWriteIncremental ? "yes" : "no"}`,
		`- Tools: ${(report.toolNames ?? []).join(", ") || "none"}`,
		`- MCP status matches local index: ${report.mcpStatusMatchesLocal ? "yes" : "no"}`,
		`- search_status snapshot: files=${report.totalFiles ?? "unknown"}, chunks=${report.totalChunks ?? "unknown"}, db=${report.activeDb ?? "unknown"}, last_mode=${report.lastMode ?? "none"}, last_write_mode=${report.lastWriteMode ?? "none"}, last_write_at=${report.lastWriteAt ?? "none"}`,
		`- MCP-reported status: files=${report.mcpFilesIndexed ?? "unknown"}, chunks=${report.mcpCodeChunks ?? "unknown"}, db=${report.mcpDatabasePath ?? "unknown"}`,
		`- test query top hit: ${report.queryTopPath || "NONE"}`,
	];

	console.log(lines.join("\n"));
}

const options = parseArgs(process.argv.slice(2));
const report = buildReport(options);

if (options.json) {
	console.log(JSON.stringify(report, null, "\t"));
} else {
	printHumanReport(report);
}
