#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export const REPO_ROOT = "/Users/frank/GeminiCLI";
export const BRIDGE_REGISTRY_REPO_PATH = "Gemini-Setup/bridges/bridge-registry.json";
export const BRIDGE_REGISTRY_PATH = path.join(REPO_ROOT, BRIDGE_REGISTRY_REPO_PATH);

export function readJson(filePath, fallback) {
  if (!fs.existsSync(filePath)) {
    return fallback;
  }
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

export function resolveRepoPath(repoPath) {
  return path.join(REPO_ROOT, `${repoPath || ""}`.replace(/\//g, path.sep));
}

export function loadBridgeRegistry() {
  const registry = readJson(BRIDGE_REGISTRY_PATH, null);
  if (!registry) {
    throw new Error(`Missing bridge registry: ${BRIDGE_REGISTRY_REPO_PATH}`);
  }
  return registry;
}

export function loadBridgeContext(bridgeId) {
  const registry = loadBridgeRegistry();
  const bridgeEntry = registry.bridges?.[bridgeId];
  if (!bridgeEntry) {
    throw new Error(`Unknown bridge id: ${bridgeId}`);
  }

  // Schema-Verschmelzung: Unterstütze sowohl 'bridge_files' als auch 'spec'/'state'
  const bridgeFiles = bridgeEntry.bridge_files || [
    bridgeEntry.spec,
    bridgeEntry.state,
    ...(bridgeEntry.sources || [])
  ].filter(Boolean);

  const jsonFile = bridgeFiles.find(f => f && f.endsWith(".json"));
  if (!jsonFile) {
    throw new Error(`Bridge entry for ${bridgeId} is missing a .json file.`);
  }
  const bridgeJsonPath = resolveRepoPath(jsonFile);
  const bridgeJson = readJson(bridgeJsonPath, {});

  return {
    registry,
    bridgeEntry,
    bridgeJson,
    bridgeFiles,
    statePath: resolveRepoPath(bridgeEntry.state || bridgeJson.state_file),
    sourceLabel: bridgeEntry.source_label || bridgeJson.source_label || bridgeId,
    sourcePaths: bridgeEntry.sources || bridgeEntry.source_paths || bridgeJson.tracked_paths || [],
    triggerPhrases: bridgeEntry.trigger_phrases || bridgeJson.trigger_phrases || []
  };
}
