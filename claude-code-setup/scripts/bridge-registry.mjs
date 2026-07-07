#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export const REPO_ROOT = path.resolve(__dirname, "..", "..");
export const BRIDGE_REGISTRY_REPO_PATH =
	"claude-code-setup/bridges/bridge-registry.json";
export const BRIDGE_REGISTRY_PATH = path.join(
	REPO_ROOT,
	BRIDGE_REGISTRY_REPO_PATH,
);

export function readJson(filePath, fallback) {
	if (!fs.existsSync(filePath)) {
		return fallback;
	}
	return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

export function resolveRepoPath(repoPath) {
	return path.join(REPO_ROOT, `${repoPath || ""}`.replace(/\//g, path.sep));
}

export function deriveBlobBase(githubUrl) {
	const normalized = `${githubUrl || ""}`.trim();
	if (!normalized) return "";
	const marker = "/blob/";
	const markerIndex = normalized.indexOf(marker);
	if (markerIndex === -1) return "";
	const branchSlashIndex = normalized.indexOf("/", markerIndex + marker.length);
	if (branchSlashIndex === -1) return "";
	return normalized.slice(0, branchSlashIndex + 1);
}

export function deriveGithubBlobUrl(baseUrl, repoPath) {
	const base = deriveBlobBase(baseUrl);
	if (!base || !repoPath) return "";
	return `${base}${`${repoPath}`.replace(/\\/g, "/")}`;
}

export function loadBridgeRegistry() {
	const registry = readJson(BRIDGE_REGISTRY_PATH, null);
	if (!registry) {
		throw new Error(`Missing bridge registry: ${BRIDGE_REGISTRY_REPO_PATH}`);
	}
	if (!registry.bridges || typeof registry.bridges !== "object") {
		throw new Error("bridge-registry.json must define a bridges object.");
	}
	return registry;
}

function findBridgeJsonRepoPath(bridgeEntry) {
	const jsonRepoPath = (bridgeEntry.bridge_files || []).find((filePath) =>
		`${filePath}`.endsWith(".json"),
	);
	if (!jsonRepoPath) {
		throw new Error("Bridge entry must define a JSON bridge file.");
	}
	return jsonRepoPath;
}

export function normalizeGitTrackedPath(sourcePath) {
	const normalized = `${sourcePath || ""}`.trim().replace(/\\/g, "/");
	if (!normalized) return null;
	if (/^[A-Za-z]:\//u.test(normalized) || normalized.startsWith("/")) {
		return null;
	}
	if (normalized.endsWith("/**")) {
		return normalized.slice(0, -3);
	}
	return normalized;
}

export function deriveGitTrackedPaths(sourcePaths = []) {
	return [...new Set(sourcePaths.map(normalizeGitTrackedPath).filter(Boolean))];
}

export function loadBridgeContext(bridgeId) {
	const registry = loadBridgeRegistry();
	const bridgeEntry = registry.bridges?.[bridgeId];
	if (!bridgeEntry) {
		throw new Error(`Unknown bridge id in registry: ${bridgeId}`);
	}

	const bridgeJsonRepoPath = findBridgeJsonRepoPath(bridgeEntry);
	const bridgeJsonPath = resolveRepoPath(bridgeJsonRepoPath);
	const bridgeJson = readJson(bridgeJsonPath, null);
	if (!bridgeJson) {
		throw new Error(`Missing bridge JSON: ${bridgeJsonRepoPath}`);
	}

	return {
		registry,
		bridgeEntry,
		bridgeJson,
		bridgeFiles: Array.isArray(bridgeEntry.bridge_files)
			? bridgeEntry.bridge_files
			: [],
		bridgeJsonRepoPath,
		bridgeJsonPath,
		statePath: resolveRepoPath(bridgeEntry.state_file),
		sourceLabel:
			bridgeEntry.source_label || bridgeJson.source_label || bridgeId,
		sourcePaths: Array.isArray(bridgeEntry.source_paths)
			? bridgeEntry.source_paths
			: [],
		auditGitPaths:
			Array.isArray(bridgeEntry.audit_git_paths) &&
			bridgeEntry.audit_git_paths.length > 0
				? bridgeEntry.audit_git_paths
				: deriveGitTrackedPaths(bridgeEntry.source_paths || []),
		stateScope:
			bridgeEntry.state_scope ||
			bridgeJson.target_scope ||
			`${bridgeId}-environment-only`,
		auditTitle:
			bridgeEntry.audit_title ||
			`${bridgeEntry.source_label || bridgeJson.source_label || bridgeId} Delta Audit`,
		noDeltaMessage:
			bridgeEntry.no_delta_message ||
			"Keine neuen portablen Delta-Kandidaten seit dem Basisstand.",
		triggerPhrases: Array.isArray(bridgeEntry.trigger_phrases)
			? bridgeEntry.trigger_phrases
			: [],
		proposalOnly:
			bridgeJson.proposal_only ??
			registry.global_policies?.proposal_only ??
			true,
		replacementRequiresConfirmation:
			bridgeJson.replacement_requires_confirmation ??
			registry.global_policies?.replace_requires_confirmation ??
			true,
		readOnlySources: Array.isArray(bridgeJson.read_only_sources)
			? bridgeJson.read_only_sources
			: [],
		exchangeLedgers: bridgeJson.exchange_ledgers || {},
	};
}
