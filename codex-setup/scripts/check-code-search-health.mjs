#!/usr/bin/env node

import { mkdtempSync, existsSync, readFileSync, rmSync } from "fs";
import { tmpdir } from "os";
import { join, resolve } from "path";
import { spawnSync } from "child_process";
import { fileURLToPath } from "url";

const DEFAULT_QUERY = "Oberste Direktive gemeinsame Memory-Datei";
const DEFAULT_ATTEMPTS = 2;

function fail(message, details = "") {
	const suffix = details ? `\n${details}` : "";
	console.error(`${message}${suffix}`);
	process.exit(1);
}

function parseArgs(argv) {
	const options = {
		workspace: "",
		query: DEFAULT_QUERY,
		json: false,
	};

	for (let index = 0; index < argv.length; index++) {
		const arg = argv[index];
		if (arg === "--workspace") {
			options.workspace = argv[++index] ?? "";
			continue;
		}
		if (arg === "--query") {
			options.query = argv[++index] ?? "";
			continue;
		}
		if (arg === "--json") {
			options.json = true;
			continue;
		}
		if (arg === "--help" || arg === "-h") {
			console.log(
				[
					"Usage: check-code-search-health.mjs [--workspace <path>] [--query <text>] [--json]",
					"",
					"Checks the local code-search index metadata and runs a fresh Codex MCP smoke test.",
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

function ensureCodeSearchMcpConfigured() {
	const result = run("codex", ["mcp", "get", "code-search"]);
	if (result.status !== 0) {
		fail(
			"code-search MCP lookup failed.",
			(result.stderr || result.stdout || "").trim(),
		);
	}

	const output = `${result.stdout}${result.stderr}`;
	if (!output.includes("enabled: true")) {
		fail("code-search MCP is not enabled.");
	}

	return output.trim();
}

function buildSmokePrompt(workspace, query) {
	const quotedWorkspace = JSON.stringify(workspace);
	const quotedQuery = JSON.stringify(query);

	return [
		"Use only the code-search MCP server.",
		"Do not use web search, shell commands, filesystem reads, or any fallback.",
		"Do not call index_codebase.",
		"Only call search_status and search_code.",
		`Call search_status with directory ${quotedWorkspace}.`,
		`Call search_code with directory ${quotedWorkspace}, limit 1, query ${quotedQuery}.`,
		"Reply with exactly one minified JSON object and nothing else.",
		"Schema:",
		'{"status_ok":true,"query_ok":true,"query_top_path":"absolute path or NONE"}',
		"Rules:",
		"- Set query_top_path to the absolute path of the top hit.",
		"- If there is no hit, use NONE.",
	].join("\n");
}

function runSmokeTest(repoRoot, workspace, query) {
	const tempDir = mkdtempSync(join(tmpdir(), "code-search-health-"));
	const messagePath = join(tempDir, "message.txt");
	let lastFailure = "Fresh Codex exec did not return a parseable JSON response.";

	try {
		for (let attempt = 1; attempt <= DEFAULT_ATTEMPTS; attempt++) {
			const prompt = buildSmokePrompt(workspace, query);
			const result = run("codex", [
				"exec",
				"--skip-git-repo-check",
				"--dangerously-bypass-approvals-and-sandbox",
				"-C",
				repoRoot,
				"-c",
				'model_reasoning_effort="low"',
				"-o",
				messagePath,
				prompt,
			]);

			const rawMessage = existsSync(messagePath)
				? readFileSync(messagePath, "utf8").replace(/\r/g, "").trim()
				: "";
			const execOutput = `${result.stdout}${result.stderr}`.trim();

			try {
				const parsed = JSON.parse(rawMessage);
				if (typeof parsed?.status_ok === "boolean" && typeof parsed?.query_ok === "boolean") {
					return {
						statusOk: parsed.status_ok,
						queryOk: parsed.query_ok,
						queryTopPath: String(parsed.query_top_path ?? "").trim(),
						execOk: result.status === 0,
					};
				}
			} catch {
				// Retry below.
			}

			lastFailure = [
				`Fresh Codex exec returned an unexpected response on attempt ${attempt}.`,
				rawMessage ? `Message: ${rawMessage}` : "Message: <empty>",
				execOutput ? `Logs: ${execOutput}` : "Logs: <empty>",
			].join("\n");
		}
	} finally {
		rmSync(tempDir, { recursive: true, force: true });
	}

	fail("code-search MCP smoke test failed.", lastFailure);
}

function buildReport(options) {
	const scriptDir = fileURLToPath(new URL(".", import.meta.url));
	const repoRoot = resolve(join(scriptDir, "..", ".."));
	const workspace = resolveWorkspace(options.workspace, repoRoot);
	ensureCodeSearchMcpConfigured();

	const smoke = runSmokeTest(repoRoot, workspace, options.query);
	const local = readLocalIndexState(workspace);
	const report = {
		workspace,
		query: options.query,
		mcpConfigured: true,
		freshExecOk: smoke.execOk,
		searchStatusOk: smoke.statusOk,
		queryOk: smoke.queryOk,
		queryTopPath: smoke.queryTopPath,
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
	};

	if (!report.searchStatusOk || !report.queryOk) {
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
		`- search_status snapshot: files=${report.totalFiles ?? "unknown"}, chunks=${report.totalChunks ?? "unknown"}, db=${report.activeDb ?? "unknown"}, last_mode=${report.lastMode ?? "none"}, last_write_mode=${report.lastWriteMode ?? "none"}, last_write_at=${report.lastWriteAt ?? "none"}`,
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
