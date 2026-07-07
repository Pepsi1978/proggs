#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { REPO_ROOT, loadBridgeRegistry } from "./bridge-registry.mjs";

const DEFAULT_LEDGER_PATH = path.join(
	REPO_ROOT,
	"claude-code-setup",
	"state",
	"implemented-intelligence-suggestions.json",
);
const DEFAULT_PROPOSAL_PREFIX = "💡 Intelligenz-Vorschlag:";
const DEFAULT_RESILIENCE_RULE =
	"Umgesetzte Intelligenzvorschlaege muessen denselben Zukunftsstandard wie Umgebungsfixes haben: verwandte Oberflaechen mitpruefen, update-resistent bauen und die Robustheit ausdruecklich dokumentieren.";

function usage() {
	return [
		"Usage:",
		"  register-intelligence-suggestion.mjs list [--json] [--state PATH]",
		"  register-intelligence-suggestion.mjs add --id ID --summary TEXT --proposal TEXT --context TEXT --suggested-because TEXT --implemented-because TEXT --implementation TEXT --bridge-value TEXT --adoption-guidance TEXT --resilience-summary TEXT --failure-review TEXT [--portable-to CSV] [--artifacts CSV] [--source-cli NAME] [--status STATUS] [--json] [--state PATH]",
	].join("\n");
}

function fail(message) {
	process.stderr.write(`${message}\n`);
	process.exit(1);
}

function parseArgs(argv) {
	const args = { _: [], json: false };
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

function splitCsv(value, fallback = []) {
	if (!value) return fallback;
	return `${value}`
		.split(",")
		.map((item) => item.trim())
		.filter(Boolean);
}

function deriveBlobBase(githubUrl) {
	const normalized = `${githubUrl || ""}`.trim();
	if (!normalized) return "";
	const marker = "/blob/";
	const markerIndex = normalized.indexOf(marker);
	if (markerIndex === -1) return "";
	const branchSlashIndex = normalized.indexOf("/", markerIndex + marker.length);
	if (branchSlashIndex === -1) return "";
	return normalized.slice(0, branchSlashIndex + 1);
}

function deriveGithubBlobUrl(baseUrl, repoPath) {
	const base = deriveBlobBase(baseUrl);
	if (!base) return "";
	return `${base}${repoPath.replace(/\\/g, "/")}`;
}

function ensureText(value, label) {
	const normalized = `${value || ""}`.trim();
	if (!normalized) {
		throw new Error(`Missing required field: ${label}`);
	}
	return normalized;
}

function ensureDetailedText(value, label, minimumLength = 40) {
	const normalized = ensureText(value, label);
	if (normalized.length < minimumLength) {
		throw new Error(
			`${label} must be at least ${minimumLength} characters so other CLIs get enough context.`,
		);
	}
	return normalized;
}

function readJson(filePath, fallback) {
	if (!fs.existsSync(filePath)) {
		return fallback;
	}
	return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function writeJson(filePath, data) {
	fs.mkdirSync(path.dirname(filePath), { recursive: true });
	fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, "utf8");
}

function loadSuggestionRegistry() {
	const fallback = {
		registry_path: "claude-code-setup/bridges/bridge-registry.json",
		github_url: "",
		proposal_prefix: DEFAULT_PROPOSAL_PREFIX,
		global_policies: {
			implemented_intelligence_resilience_rule: DEFAULT_RESILIENCE_RULE,
		},
		peer_registry_targets: {},
	};
	const loaded = loadBridgeRegistry();
	return {
		...fallback,
		...loaded,
		global_policies: {
			...fallback.global_policies,
			...(loaded.global_policies || {}),
		},
		peer_registry_targets:
			loaded.peer_registry_targets || fallback.peer_registry_targets,
	};
}

function buildDefaultLedger(registry = loadSuggestionRegistry()) {
	const claudeRepoPath =
		"claude-code-setup/state/implemented-intelligence-suggestions.json";
	const codexRepoPath =
		"codex-setup/state/implemented-intelligence-suggestions.json";
	const geminiRepoPath =
		"Gemini-Setup/state/implemented-intelligence-suggestions.json";
	const codexRegistryTarget = registry.peer_registry_targets?.["Codex"] || {};
	const geminiRegistryTarget =
		registry.peer_registry_targets?.["Gemini CLI"] || {};

	return {
		version: 2,
		scope: "programming-environment-only",
		ledger_kind: "implemented-intelligence-suggestions",
		proposal_prefix: registry.proposal_prefix || DEFAULT_PROPOSAL_PREFIX,
		github_url: deriveGithubBlobUrl(registry.github_url, claudeRepoPath),
		bridge_registry_path:
			registry.registry_path ||
			"claude-code-setup/bridges/bridge-registry.json",
		bridge_registry_github_url: registry.github_url || "",
		resilience_rule:
			registry.global_policies?.implemented_intelligence_resilience_rule ||
			DEFAULT_RESILIENCE_RULE,
		required_resilience_fields: ["resilience_summary", "future_failure_review"],
		bridge_reference: [
			"claude-code-setup/bridges/intelligence-suggestion-exchange-bridge.md",
			"claude-code-setup/bridges/intelligence-suggestion-exchange-bridge.json",
		],
		peer_ledgers: {
			"Claude Code": {
				repo_path: claudeRepoPath,
				github_url: deriveGithubBlobUrl(registry.github_url, claudeRepoPath),
				write_policy: "read-write-own-workspace",
			},
			Codex: {
				expected_repo_path: codexRepoPath,
				expected_github_url: deriveGithubBlobUrl(
					codexRegistryTarget.expected_github_url,
					codexRepoPath,
				),
				write_policy: "read-only-from-claude-code",
				status: "awaiting-foreign-registration",
			},
			"Gemini CLI": {
				expected_repo_path: geminiRepoPath,
				expected_local_path:
					"%USERPROFILE%/GeminiCLI/state/implemented-intelligence-suggestions.json",
				expected_github_url: deriveGithubBlobUrl(
					geminiRegistryTarget.expected_github_url,
					geminiRepoPath,
				),
				write_policy: "read-only-from-claude-code",
				status: "awaiting-foreign-registration",
			},
		},
		entries: [],
	};
}

function loadLedger(filePath) {
	const defaultLedger = buildDefaultLedger();
	const loaded = readJson(filePath, defaultLedger);
	return {
		...defaultLedger,
		...loaded,
		peer_ledgers: loaded.peer_ledgers || defaultLedger.peer_ledgers,
		bridge_reference: loaded.bridge_reference || defaultLedger.bridge_reference,
		required_resilience_fields:
			loaded.required_resilience_fields ||
			defaultLedger.required_resilience_fields,
		entries: Array.isArray(loaded.entries) ? loaded.entries : [],
	};
}

function ensureProposalText(value, prefix) {
	const normalized = ensureDetailedText(value, "proposal", 20);
	if (!normalized.startsWith(prefix)) {
		throw new Error(`proposal must start with "${prefix}"`);
	}
	return normalized;
}

function buildEntry(args, existingEntry = null) {
	const now = new Date().toISOString();
	const registry = loadSuggestionRegistry();
	const proposalPrefix = registry.proposal_prefix || DEFAULT_PROPOSAL_PREFIX;
	return {
		id: ensureText(args.id || existingEntry?.id, "id"),
		source_cli: ensureText(
			args["source-cli"] || existingEntry?.source_cli || "Claude Code",
			"source-cli",
		),
		summary: ensureText(args.summary || existingEntry?.summary, "summary"),
		proposal_text: ensureProposalText(
			args.proposal || existingEntry?.proposal_text,
			proposalPrefix,
		),
		context_for_other_clis: ensureDetailedText(
			args.context || existingEntry?.context_for_other_clis,
			"context",
		),
		why_it_was_suggested: ensureDetailedText(
			args["suggested-because"] || existingEntry?.why_it_was_suggested,
			"suggested-because",
			30,
		),
		why_it_was_implemented: ensureDetailedText(
			args["implemented-because"] || existingEntry?.why_it_was_implemented,
			"implemented-because",
			30,
		),
		how_it_was_implemented: ensureDetailedText(
			args.implementation || existingEntry?.how_it_was_implemented,
			"implementation",
		),
		bridge_value: ensureDetailedText(
			args["bridge-value"] || existingEntry?.bridge_value,
			"bridge-value",
			30,
		),
		adoption_guidance: ensureDetailedText(
			args["adoption-guidance"] || existingEntry?.adoption_guidance,
			"adoption-guidance",
			30,
		),
		resilience_summary: ensureDetailedText(
			args["resilience-summary"] || existingEntry?.resilience_summary,
			"resilience-summary",
			40,
		),
		future_failure_review: ensureDetailedText(
			args["failure-review"] || existingEntry?.future_failure_review,
			"failure-review",
			40,
		),
		portable_to: splitCsv(
			args["portable-to"],
			existingEntry?.portable_to || ["Codex", "Gemini CLI"],
		),
		artifacts: splitCsv(args.artifacts, existingEntry?.artifacts || []),
		created_at: existingEntry?.created_at || now,
		status:
			`${args.status || existingEntry?.status || "active"}`.trim() || "active",
		...(existingEntry ? { updated_at: now } : {}),
	};
}

function printList(ledger, asJson) {
	if (asJson) {
		process.stdout.write(`${JSON.stringify(ledger, null, 2)}\n`);
		return;
	}

	const lines = [
		"Implemented Intelligence Suggestions",
		`Scope: ${ledger.scope}`,
		`Entries: ${ledger.entries.length}`,
		`GitHub: ${ledger.github_url}`,
	];

	ledger.entries.forEach((entry, index) => {
		lines.push("");
		lines.push(`${index + 1}. ${entry.id}`);
		lines.push(`   Summary: ${entry.summary}`);
		lines.push(`   Proposal: ${entry.proposal_text}`);
		lines.push(`   Context: ${entry.context_for_other_clis}`);
		lines.push(`   Suggested because: ${entry.why_it_was_suggested}`);
		lines.push(`   Implemented because: ${entry.why_it_was_implemented}`);
		lines.push(`   Implementation: ${entry.how_it_was_implemented}`);
		lines.push(`   Bridge value: ${entry.bridge_value}`);
		lines.push(`   Adoption guidance: ${entry.adoption_guidance}`);
		lines.push(`   Resilience: ${entry.resilience_summary}`);
		lines.push(`   Failure review: ${entry.future_failure_review}`);
		if (entry.portable_to?.length) {
			lines.push(`   Portable to: ${entry.portable_to.join(", ")}`);
		}
		if (entry.artifacts?.length) {
			lines.push(`   Artifacts: ${entry.artifacts.join(", ")}`);
		}
	});

	process.stdout.write(`${lines.join("\n")}\n`);
}

function addEntry(args, ledgerPath) {
	const ledger = loadLedger(ledgerPath);
	const existingIndex = ledger.entries.findIndex(
		(entry) => entry.id === args.id,
	);
	const existingEntry =
		existingIndex >= 0 ? ledger.entries[existingIndex] : null;
	const nextEntry = buildEntry(args, existingEntry);

	if (existingIndex >= 0) {
		ledger.entries[existingIndex] = nextEntry;
	} else {
		ledger.entries.push(nextEntry);
	}

	writeJson(ledgerPath, ledger);
	return {
		ledger_path: ledgerPath,
		entry: nextEntry,
		action: existingIndex >= 0 ? "updated" : "added",
	};
}

function main() {
	const args = parseArgs(process.argv.slice(2));
	const command = args._[0] || "list";
	const ledgerPath = path.resolve(args.state || DEFAULT_LEDGER_PATH);

	if (command === "list") {
		printList(loadLedger(ledgerPath), args.json);
		return;
	}

	if (command === "add") {
		const result = addEntry(args, ledgerPath);
		if (args.json) {
			process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
			return;
		}
		process.stdout.write(
			`${result.action} implemented intelligence suggestion ${result.entry.id} in ${result.ledger_path}\n`,
		);
		return;
	}

	throw new Error(usage());
}

try {
	main();
} catch (error) {
	fail(error.message);
}
