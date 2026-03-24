#!/usr/bin/env node

import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "fs";
import { spawn, spawnSync } from "child_process";
import { tmpdir } from "os";
import { join, resolve } from "path";
import { fileURLToPath } from "url";

const DOCS_MCP_URL = "https://developers.openai.com/mcp";
const DOCS_GUIDE_URL = "https://developers.openai.com/api/docs/guides/tools-connectors-mcp";
const DEFAULT_TIMEOUT_MS = 30000;
const DEFAULT_ATTEMPTS = 2;
const DEFAULT_RETRY_DELAY_MS = 1000;
const EARLY_SUCCESS_GRACE_MS = 1500;
const PROMPT = [
	"Use only the openaiDeveloperDocs MCP server.",
	"Do not use web search or any fallback.",
	"Call list_openai_docs with limit 1.",
	"If openaiDeveloperDocs is available in this fresh Codex session, reply with exactly AVAILABLE.",
	"If the MCP server is unavailable, reply with exactly UNAVAILABLE.",
].join("\n");

function fail(message, details = "") {
	const suffix = details ? `\n${details}` : "";
	console.error(`${message}${suffix}`);
	process.exit(1);
}

function usage() {
	return [
		"Usage: check-openai-docs-mcp.mjs [--timeout-ms <ms>] [--attempts <n>] [--retry-delay-ms <ms>] [--json]",
		"",
		"Runs a bounded openaiDeveloperDocs MCP smoke test with a fresh retry.",
	].join("\n");
}

function readOptionValue(argv, index, flag) {
	const value = argv[index + 1] ?? "";
	if (!value || value.startsWith("--")) {
		fail(`Missing value for ${flag}.`, usage());
	}
	return value;
}

function parsePositiveInt(rawValue, flag) {
	const parsed = Number.parseInt(rawValue, 10);
	if (!Number.isFinite(parsed) || parsed <= 0) {
		fail(`Invalid ${flag} value: ${rawValue}`);
	}
	return parsed;
}

function parseArgs(argv) {
	const options = {
		timeoutMs: DEFAULT_TIMEOUT_MS,
		attempts: DEFAULT_ATTEMPTS,
		retryDelayMs: DEFAULT_RETRY_DELAY_MS,
		json: false,
	};

	for (let index = 0; index < argv.length; index++) {
		const arg = argv[index];
		if (arg === "--timeout-ms") {
			options.timeoutMs = parsePositiveInt(readOptionValue(argv, index, arg), arg);
			index++;
			continue;
		}
		if (arg === "--attempts") {
			options.attempts = parsePositiveInt(readOptionValue(argv, index, arg), arg);
			index++;
			continue;
		}
		if (arg === "--retry-delay-ms") {
			options.retryDelayMs = parsePositiveInt(readOptionValue(argv, index, arg), arg);
			index++;
			continue;
		}
		if (arg === "--json") {
			options.json = true;
			continue;
		}
		if (arg === "--help" || arg === "-h") {
			console.log(usage());
			process.exit(0);
		}
		fail(`Unknown argument: ${arg}`, usage());
	}

	return options;
}

function run(command, args, options = {}) {
	const result = spawnSync(command, args, {
		encoding: "utf8",
		maxBuffer: 4 * 1024 * 1024,
		shell: process.platform === "win32" && /\.(cmd|bat)$/i.test(command),
		...options,
	});

	if (result.error && result.error.code !== "ETIMEDOUT") {
		fail(`Failed to run ${command}.`, result.error.message);
	}

	return result;
}

function resolveCodexCommand() {
	const candidates = [];

	if (process.env.CODEX_CLI_BIN) {
		candidates.push(process.env.CODEX_CLI_BIN);
	}
	if (process.env.CODEX_BIN) {
		candidates.push(process.env.CODEX_BIN);
	}

	if (process.platform === "win32" && process.env.APPDATA) {
		candidates.push(join(process.env.APPDATA, "npm", "codex.cmd"));
		candidates.push(join(process.env.APPDATA, "npm", "codex.exe"));
		candidates.push(join(process.env.APPDATA, "npm", "codex.ps1"));
	}

	candidates.push("codex");

	for (const candidate of candidates) {
		if (!candidate) continue;
		if (candidate === "codex") continue;
		if (existsSync(candidate)) {
			return candidate;
		}
	}

	if (process.platform === "win32" && process.env.APPDATA) {
		return join(process.env.APPDATA, "npm", "codex.cmd");
	}

	return "codex";
}

function sleep(ms) {
	Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
}

function readLastLine(text) {
	const lines = text
		.replace(/\r/g, "")
		.split("\n")
		.map((line) => line.trim())
		.filter(Boolean);
	return lines.at(-1) ?? "";
}

function readMessageFile(filePath) {
	if (!existsSync(filePath)) {
		return "";
	}

	return readFileSync(filePath, "utf8");
}

function observedDocsStartup(logText) {
	return logText.includes("mcp startup: ready:") && logText.includes("openaiDeveloperDocs");
}

function observedDocsToolDispatch(logText) {
	return /tool openaiDeveloperDocs\./.test(logText);
}

function observedUnavailableSignal(logText, smokeResult) {
	return (
		smokeResult === "UNAVAILABLE" ||
		/unknown MCP server/i.test(logText) ||
		/openaiDeveloperDocs MCP is not enabled/i.test(logText)
	);
}

async function checkDocsGuideReachable() {
	const controller = new AbortController();
	const timer = setTimeout(() => controller.abort(), 10000);

	try {
		const response = await fetch(DOCS_GUIDE_URL, {
			redirect: "follow",
			signal: controller.signal,
		});

		if (!response.ok) {
			fail(
				"OpenAI docs guide is not reachable.",
				`${DOCS_GUIDE_URL} returned HTTP ${response.status}.`,
			);
		}
	} catch (error) {
		fail(
			"OpenAI docs guide reachability check failed.",
			error instanceof Error ? error.message : String(error),
		);
	} finally {
		clearTimeout(timer);
	}
}

function checkMcpConfig() {
	const result = run(CODEX_COMMAND, ["mcp", "get", "openaiDeveloperDocs"]);
	if (result.status !== 0) {
		fail(
			"Failed to inspect openaiDeveloperDocs MCP configuration.",
			(result.stderr || result.stdout || "").trim(),
		);
	}

	const output = `${result.stdout}${result.stderr}`;
	if (!output.includes("enabled: true")) {
		fail("openaiDeveloperDocs MCP is not enabled.");
	}
	if (!output.includes(DOCS_MCP_URL)) {
		fail("openaiDeveloperDocs MCP points to an unexpected URL.", output.trim());
	}
}

async function runCodexAttempt(repoRoot, options, attempt) {
	const tempDir = mkdtempSync(join(tmpdir(), "check-openai-docs-mcp-"));
	const outputPath = join(tempDir, "message.txt");
	const args = [
		"exec",
		"--skip-git-repo-check",
		"--dangerously-bypass-approvals-and-sandbox",
		"-C",
		repoRoot,
		"-c",
		'model_reasoning_effort="low"',
		"-o",
		outputPath,
		"-",
	];

	return await new Promise((resolveAttempt) => {
		const startedAt = Date.now();
		const child = spawn(CODEX_COMMAND, args, {
			shell: process.platform === "win32" && /\.(cmd|bat)$/i.test(CODEX_COMMAND),
			stdio: ["pipe", "pipe", "pipe"],
		});
		child.stdin.end(`${PROMPT}\n`);
		let stdout = "";
		let stderr = "";
		let launchError = "";
		let terminationMode = "";
		let toolDispatchObservedAt = 0;

		const finish = (statusCode) => {
			const durationMs = Date.now() - startedAt;
			const messageText = readMessageFile(outputPath);
			const smokeResult = readLastLine(messageText);
			const logText = `${stdout}${stderr}`.trim();
			const startupReady = observedDocsStartup(logText);
			const toolInvocationObserved = observedDocsToolDispatch(logText);
			const unavailableSignal = observedUnavailableSignal(logText, smokeResult);
			const explicitAvailable = smokeResult === "AVAILABLE";
			const toolPathConfirmed =
				startupReady && toolInvocationObserved && !unavailableSignal;
			const available = explicitAvailable || toolPathConfirmed;
			const successMode = explicitAvailable
				? "explicit-reply"
				: toolPathConfirmed
					? "tool-dispatch-observed"
					: "";
			const failureReason = available
				? ""
				: terminationMode === "timeout"
					? `timed out after ${options.timeoutMs} ms`
					: terminationMode === "spawn-error"
						? `failed to launch codex exec: ${launchError}`
						: statusCode !== 0
							? `codex exec exited with status ${statusCode}`
							: smokeResult
								? `unexpected reply: ${smokeResult}`
								: "no reply captured";

			clearTimeout(timeoutHandle);
			clearInterval(pollHandle);
			rmSync(tempDir, { recursive: true, force: true });

			resolveAttempt({
				attempt,
				durationMs,
				timedOut: terminationMode === "timeout",
				ok: available,
				smokeResult,
				startupReady,
				toolInvocationObserved,
				successMode,
				failureReason,
				logText,
			});
		};

		const stopChild = (mode) => {
			if (terminationMode) {
				return;
			}
			terminationMode = mode;
			if (child.exitCode === null && !child.killed) {
				child.kill("SIGKILL");
			}
		};

		const evaluateProgress = () => {
			if (terminationMode === "timeout" || terminationMode === "spawn-error") {
				return;
			}

			const smokeResult = readLastLine(readMessageFile(outputPath));
			if (smokeResult === "AVAILABLE") {
				stopChild("early-success");
				return;
			}

			const logText = `${stdout}${stderr}`.trim();
			const startupReady = observedDocsStartup(logText);
			const toolInvocationObserved = observedDocsToolDispatch(logText);
			const unavailableSignal = observedUnavailableSignal(logText, smokeResult);

			if (toolInvocationObserved && toolDispatchObservedAt === 0) {
				toolDispatchObservedAt = Date.now();
			}

			if (
				startupReady &&
				toolInvocationObserved &&
				!unavailableSignal &&
				toolDispatchObservedAt > 0 &&
				Date.now() - toolDispatchObservedAt >= EARLY_SUCCESS_GRACE_MS
			) {
				stopChild("early-success");
			}
		};

		const timeoutHandle = setTimeout(() => {
			stopChild("timeout");
		}, options.timeoutMs);

		const pollHandle = setInterval(evaluateProgress, 250);

		child.stdout.on("data", (chunk) => {
			stdout += chunk.toString("utf8");
			evaluateProgress();
		});

		child.stderr.on("data", (chunk) => {
			stderr += chunk.toString("utf8");
			evaluateProgress();
		});

		child.on("error", (error) => {
			launchError = error instanceof Error ? error.message : String(error);
			stopChild("spawn-error");
		});

		child.on("close", (statusCode) => {
			finish(typeof statusCode === "number" ? statusCode : -1);
		});
	});
}

function buildReport(repoRoot, options, attemptResults) {
	const success = attemptResults.find((attempt) => attempt.ok) ?? null;

	return {
		repoRoot,
		timeoutMs: options.timeoutMs,
		attempts: options.attempts,
		retryDelayMs: options.retryDelayMs,
		configured: true,
		docsReachable: true,
		ok: Boolean(success),
		successAttempt: success?.attempt ?? null,
		successMode: success?.successMode ?? null,
		attemptResults: attemptResults.map((attempt) => ({
			attempt: attempt.attempt,
			ok: attempt.ok,
			timedOut: attempt.timedOut,
			durationMs: attempt.durationMs,
			smokeResult: attempt.smokeResult || "",
			startupReady: attempt.startupReady,
			toolInvocationObserved: attempt.toolInvocationObserved,
			successMode: attempt.successMode || "",
			failureReason: attempt.ok ? "" : attempt.failureReason,
		})),
	};
}

function printHumanSuccess(report) {
	const attemptLabel = report.successAttempt === 1
		? "on the first attempt"
		: `on attempt ${report.successAttempt}/${report.attempts}`;
	const successLabel = report.successMode === "tool-dispatch-observed"
		? "reached a real openaiDeveloperDocs tool dispatch"
		: "replied AVAILABLE";
	console.log(
		`openaiDeveloperDocs MCP configured, official docs reachable, and fresh Codex exec ${successLabel} ${attemptLabel} with a ${report.timeoutMs} ms timeout per attempt`,
	);
}

function printFailure(report, attemptResults) {
	const detailLines = attemptResults.map((attempt) => {
		const lines = [
			`attempt ${attempt.attempt}/${report.attempts}: ${attempt.failureReason}`,
		];
		if (attempt.logText) {
			lines.push(attempt.logText);
		}
		return lines.join("\n");
	});

	fail(
		"Fresh Codex exec did not confirm openaiDeveloperDocs availability within the bounded retry window.",
		detailLines.join("\n\n"),
	);
}

const options = parseArgs(process.argv.slice(2));
const scriptDir = fileURLToPath(new URL(".", import.meta.url));
const repoRoot = resolve(join(scriptDir, "..", ".."));
const CODEX_COMMAND = resolveCodexCommand();

checkMcpConfig();
await checkDocsGuideReachable();

const attemptResults = [];
for (let attempt = 1; attempt <= options.attempts; attempt++) {
	const result = await runCodexAttempt(repoRoot, options, attempt);
	attemptResults.push(result);

	if (result.ok) {
		break;
	}

	if (attempt < options.attempts) {
		sleep(options.retryDelayMs);
	}
}

const report = buildReport(repoRoot, options, attemptResults);

if (options.json) {
	console.log(JSON.stringify(report, null, "\t"));
	if (!report.ok) {
		process.exit(1);
	}
} else if (report.ok) {
	printHumanSuccess(report);
} else {
	printFailure(report, attemptResults);
}
