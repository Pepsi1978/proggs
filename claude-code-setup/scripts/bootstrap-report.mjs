#!/usr/bin/env node

import {
	BRIDGE_REGISTRY_REPO_PATH,
	REPO_ROOT,
	deriveGithubBlobUrl,
	loadBridgeContext,
	loadBridgeRegistry,
	readJson,
	resolveRepoPath,
} from "./bridge-registry.mjs";

const ENVIRONMENT_FIX_BRIDGE_REPO_PATH =
	"claude-code-setup/bridges/environment-fix-exchange-bridge.json";
const IMPLEMENTED_SUGGESTION_BRIDGE_REPO_PATH =
	"claude-code-setup/bridges/intelligence-suggestion-exchange-bridge.json";
const ENVIRONMENT_FIX_LEDGER_REPO_PATH =
	"claude-code-setup/state/environment-fixes.json";
const IMPLEMENTED_SUGGESTION_LEDGER_REPO_PATH =
	"claude-code-setup/state/implemented-intelligence-suggestions.json";

const KNOWN_CLIS = ["Claude Code", "Codex", "Gemini CLI"];

function parseArgs(argv) {
	const args = { _: [], json: false, cli: "all" };
	for (let i = 0; i < argv.length; i += 1) {
		const part = argv[i];
		if (!part.startsWith("--")) {
			args._.push(part);
			continue;
		}
		const key = part.slice(2);
		if (key === "json") {
			args.json = true;
			continue;
		}
		const next = argv[i + 1];
		if (!next || next.startsWith("--")) {
			throw new Error(`Missing value for --${key}`);
		}
		args[key] = next;
		i += 1;
	}
	return args;
}

function normalizeCliName(rawCli) {
	const normalized = `${rawCli || "all"}`.trim().toLowerCase();
	if (!normalized || normalized === "all") return "all";
	if (normalized === "claude code" || normalized === "claude-code") {
		return "Claude Code";
	}
	if (normalized === "codex") return "Codex";
	if (
		normalized === "gemini cli" ||
		normalized === "gemini-cli" ||
		normalized === "gemini"
	) {
		return "Gemini CLI";
	}
	throw new Error(
		`Unknown --cli value: ${rawCli}. Expected one of: all, Claude Code, Codex, Gemini CLI.`,
	);
}

function ensureArray(value) {
	return Array.isArray(value) ? value.filter(Boolean) : [];
}

function filterByCli(entries, targetCli) {
	if (targetCli === "all") {
		return entries;
	}
	return entries.filter((entry) => entry.cli === targetCli);
}

function loadJsonFromRepoPath(repoPath, fallback = {}) {
	return readJson(resolveRepoPath(repoPath), fallback);
}

function buildClaudeCodeLedgers(
	registry,
	environmentFixBridge,
	intelligenceBridge,
) {
	const environmentFixLedger = loadJsonFromRepoPath(
		ENVIRONMENT_FIX_LEDGER_REPO_PATH,
		{},
	);
	const implementedSuggestionLedger = loadJsonFromRepoPath(
		IMPLEMENTED_SUGGESTION_LEDGER_REPO_PATH,
		{},
	);

	return {
		environment_fixes: {
			repo_path: ENVIRONMENT_FIX_LEDGER_REPO_PATH,
			github_url:
				environmentFixLedger.github_url ||
				deriveGithubBlobUrl(
					registry.github_url,
					ENVIRONMENT_FIX_LEDGER_REPO_PATH,
				),
			bridge_reference: ensureArray(
				environmentFixBridge.reference_implementation,
			),
		},
		implemented_intelligence_suggestions: {
			repo_path: IMPLEMENTED_SUGGESTION_LEDGER_REPO_PATH,
			github_url:
				implementedSuggestionLedger.github_url ||
				intelligenceBridge.claude_code_github_url ||
				deriveGithubBlobUrl(
					registry.github_url,
					IMPLEMENTED_SUGGESTION_LEDGER_REPO_PATH,
				),
			bridge_reference: ensureArray(
				intelligenceBridge.reference_implementation,
			),
		},
	};
}

function buildEnvironmentFixSourcesForCodex(codexContext) {
	const source = codexContext.exchangeLedgers?.environment_fixes || {};
	return {
		read_only_sources: [
			source.codex_read_only_source,
			...ensureArray(source.codex_read_only_sources),
		].filter(Boolean),
		claude_code_bridge_files: codexContext.bridgeFiles,
		claude_code_trigger_phrases: codexContext.triggerPhrases,
	};
}

function buildEnvironmentFixSourcesForGemini(geminiContext) {
	const source = geminiContext.exchangeLedgers?.environment_fixes || {};
	return {
		read_only_sources: ensureArray(source.gemini_read_only_sources),
		claude_code_bridge_files: geminiContext.bridgeFiles,
		claude_code_trigger_phrases: geminiContext.triggerPhrases,
	};
}

function buildBridgeEntry(context) {
	return {
		bridge_files: context.bridgeFiles,
		state_file: context.bridgeEntry.state_file,
		audit_title: context.auditTitle,
		trigger_phrases: context.triggerPhrases,
		source_paths: context.sourcePaths,
		audit_git_paths: context.auditGitPaths,
		reference_implementation: ensureArray(
			context.bridgeJson.reference_implementation,
		),
	};
}

function buildReportEntries() {
	const registry = loadBridgeRegistry();
	const codexContext = loadBridgeContext("codex-delta");
	const geminiContext = loadBridgeContext("gemini-delta");
	const environmentFixBridge = loadJsonFromRepoPath(
		ENVIRONMENT_FIX_BRIDGE_REPO_PATH,
		{},
	);
	const intelligenceBridge = loadJsonFromRepoPath(
		IMPLEMENTED_SUGGESTION_BRIDGE_REPO_PATH,
		{},
	);

	const reportEntries = [
		{
			cli: "Claude Code",
			registry: {
				repo_path: registry.peer_registry_targets?.["Claude Code"]?.repo_path,
				github_url: registry.peer_registry_targets?.["Claude Code"]?.github_url,
				write_policy:
					registry.peer_registry_targets?.["Claude Code"]?.write_policy,
			},
			bootstrap_setup: registry.bootstrap_artifacts?.["Claude Code"] || {},
			bootstrap_report:
				registry.bootstrap_report_artifacts?.["Claude Code"] || {},
			ledgers: buildClaudeCodeLedgers(
				registry,
				environmentFixBridge,
				intelligenceBridge,
			),
			claude_code_bridge_entrypoints: {
				codex_delta: buildBridgeEntry(codexContext),
				gemini_cli_delta: buildBridgeEntry(geminiContext),
			},
		},
		{
			cli: "Codex",
			registry: registry.peer_registry_targets?.["Codex"] || {},
			bootstrap_setup: registry.bootstrap_artifacts?.["Codex"] || {},
			bootstrap_report: registry.bootstrap_report_artifacts?.["Codex"] || {},
			ledgers: {
				environment_fixes: buildEnvironmentFixSourcesForCodex(codexContext),
				implemented_intelligence_suggestions:
					intelligenceBridge.peer_ledgers?.["Codex"] || {},
			},
			claude_code_bridge_entrypoints: {
				codex_delta: buildBridgeEntry(codexContext),
			},
		},
		{
			cli: "Gemini CLI",
			registry: registry.peer_registry_targets?.["Gemini CLI"] || {},
			bootstrap_setup: registry.bootstrap_artifacts?.["Gemini CLI"] || {},
			bootstrap_report:
				registry.bootstrap_report_artifacts?.["Gemini CLI"] || {},
			ledgers: {
				environment_fixes: buildEnvironmentFixSourcesForGemini(geminiContext),
				implemented_intelligence_suggestions:
					intelligenceBridge.peer_ledgers?.["Gemini CLI"] || {},
			},
			claude_code_bridge_entrypoints: {
				gemini_cli_delta: buildBridgeEntry(geminiContext),
			},
		},
	];

	return {
		generated_at: new Date().toISOString(),
		report_kind: "bootstrap-report",
		source_cli: "Claude Code",
		registry_path: BRIDGE_REGISTRY_REPO_PATH,
		registry_github_url: registry.github_url || "",
		peer_adoption_tasks: ensureArray(registry.peer_adoption_tasks),
		clis: reportEntries,
		repo_root: REPO_ROOT,
	};
}

function renderList(label, values, lines) {
	const items = ensureArray(values);
	if (items.length === 0) {
		lines.push(`${label}: -`);
		return;
	}
	lines.push(`${label}:`);
	for (const item of items) {
		lines.push(`  - ${item}`);
	}
}

function renderCliEntry(entry) {
	const lines = [`## ${entry.cli}`];
	lines.push(
		`Registry: ${entry.registry.repo_path || entry.registry.expected_repo_path || "-"}`,
	);
	if (entry.registry.expected_local_path) {
		lines.push(`Lokale Registry: ${entry.registry.expected_local_path}`);
	}
	lines.push(
		`Registry GitHub: ${entry.registry.github_url || entry.registry.expected_github_url || "-"}`,
	);

	renderList(
		"Bootstrap-Setup",
		entry.bootstrap_setup.repo_scripts ||
			entry.bootstrap_setup.expected_repo_scripts,
		lines,
	);
	renderList(
		"Bootstrap-Setup lokal",
		entry.bootstrap_setup.expected_local_scripts,
		lines,
	);
	renderList(
		"Bootstrap-Setup GitHub",
		entry.bootstrap_setup.github_urls ||
			entry.bootstrap_setup.expected_github_urls,
		lines,
	);

	renderList(
		"Bootstrap-Report",
		entry.bootstrap_report.repo_scripts ||
			entry.bootstrap_report.expected_repo_scripts,
		lines,
	);
	renderList(
		"Bootstrap-Report lokal",
		entry.bootstrap_report.expected_local_scripts,
		lines,
	);
	renderList(
		"Bootstrap-Report GitHub",
		entry.bootstrap_report.github_urls ||
			entry.bootstrap_report.expected_github_urls,
		lines,
	);

	const environmentFixes = entry.ledgers?.environment_fixes || {};
	lines.push(`Environment-Fix-Ledger: ${environmentFixes.repo_path || "-"}`);
	if (environmentFixes.github_url) {
		lines.push(`Environment-Fix-GitHub: ${environmentFixes.github_url}`);
	}
	renderList(
		"Environment-Fix-Read-Only-Quellen",
		environmentFixes.read_only_sources,
		lines,
	);

	const suggestions = entry.ledgers?.implemented_intelligence_suggestions || {};
	lines.push(
		`Implemented-Intelligence-Ledger: ${suggestions.repo_path || suggestions.expected_repo_path || "-"}`,
	);
	if (suggestions.expected_local_path) {
		lines.push(
			`Implemented-Intelligence lokal: ${suggestions.expected_local_path}`,
		);
	}
	lines.push(
		`Implemented-Intelligence GitHub: ${suggestions.github_url || suggestions.expected_github_url || "-"}`,
	);

	const bridgeEntrypoints = entry.claude_code_bridge_entrypoints || {};
	for (const [bridgeId, bridgeEntry] of Object.entries(bridgeEntrypoints)) {
		lines.push(`Bridge-Einstieg ${bridgeId}:`);
		renderList("  Bridge-Dateien", bridgeEntry.bridge_files, lines);
		renderList("  Trigger", bridgeEntry.trigger_phrases, lines);
		renderList(
			"  Referenzimplementierung",
			bridgeEntry.reference_implementation,
			lines,
		);
		lines.push(`  State-Datei: ${bridgeEntry.state_file || "-"}`);
	}

	return lines.join("\n");
}

function renderHuman(report, targetCli) {
	const lines = [
		"Bootstrap-Report — Claude Code Bridge Ecosystem",
		`Generiert: ${report.generated_at}`,
		`Registry: ${report.registry_path}`,
		`Registry GitHub: ${report.registry_github_url || "-"}`,
	];
	renderList("Peer-Adoptionsaufgaben", report.peer_adoption_tasks, lines);

	for (const entry of filterByCli(report.clis, targetCli)) {
		lines.push("");
		lines.push(renderCliEntry(entry));
	}

	return `${lines.join("\n")}\n`;
}

function main() {
	const args = parseArgs(process.argv.slice(2));
	const targetCli = normalizeCliName(args.cli);
	const report = buildReportEntries();
	const filteredReport =
		targetCli === "all"
			? report
			: {
					...report,
					clis: filterByCli(report.clis, targetCli),
				};

	if (args.json) {
		process.stdout.write(`${JSON.stringify(filteredReport, null, 2)}\n`);
		return;
	}

	process.stdout.write(renderHuman(filteredReport, targetCli));
}

main();
