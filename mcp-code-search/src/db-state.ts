// db-state.ts — Database state management for the semantic search index.
// Handles DB directory setup, candidate resolution, and reindex state tracking.

import { existsSync, mkdirSync, readFileSync, readdirSync } from "fs";
import { join } from "path";

export interface DbCandidate {
	path: string;
}

export interface ReindexState {
	lastSuccessAt: string;
	lastMode: string;
}

/** Ensure the .code-search directory exists. */
export function ensureDbDir(dbDir: string): void {
	if (!existsSync(dbDir)) {
		mkdirSync(dbDir, { recursive: true });
	}
}

/**
 * List candidate database files in priority order:
 * 1. Primary: pointer-based current.txt → index-N.db (active snapshot)
 * 2. Fallback: legacy single index.db (v4+ incremental reindex)
 * 3. Last resort: highest-numbered index-N.db
 */
export function listDbCandidates(dbDir: string): DbCandidate[] {
	const candidates: DbCandidate[] = [];

	// Primary: pointer-based system (current.txt → index-N.db)
	const pointerFile = join(dbDir, "current.txt");
	if (existsSync(pointerFile)) {
		try {
			const pointer = readFileSync(pointerFile, "utf-8").trim();
			if (pointer) {
				const pointerPath = join(dbDir, pointer);
				if (
					existsSync(pointerPath) &&
					!candidates.some((c) => c.path === pointerPath)
				) {
					candidates.push({ path: pointerPath });
				}
			}
		} catch {
			// Ignore read errors on pointer file
		}
	}

	// Fallback: legacy single index.db
	const mainDb = join(dbDir, "index.db");
	if (existsSync(mainDb) && !candidates.some((c) => c.path === mainDb)) {
		candidates.push({ path: mainDb });
	}

	// Last resort: find numbered index-N.db files
	if (existsSync(dbDir)) {
		try {
			const entries = readdirSync(dbDir).filter((f) =>
				/^index-\d+\.db$/.test(f),
			);

			entries
				.sort((a, b) => {
					const numA = parseInt(a.match(/\d+/)?.[0] ?? "0", 10);
					const numB = parseInt(b.match(/\d+/)?.[0] ?? "0", 10);
					return numB - numA; // Highest number first
				})
				.forEach((entry) => {
					const entryPath = join(dbDir, entry);
					if (!candidates.some((c) => c.path === entryPath)) {
						candidates.push({ path: entryPath });
					}
				});
		} catch {
			// Ignore readdir errors
		}
	}

	return candidates;
}

/**
 * Resolve the active database path that should be used right now.
 * Returns the best known snapshot path or null if no usable index exists.
 */
export function resolvePreferredDbPath(dbDir: string): string | null {
	const candidates = listDbCandidates(dbDir);
	return candidates[0]?.path ?? null;
}

/**
 * Read the reindex state from the .code-search directory.
 * Returns null if no state file exists.
 */
export function readReindexState(dbDir: string): ReindexState | null {
	const stampFile = join(dbDir, ".last-index-time");

	if (!existsSync(stampFile)) {
		return null;
	}

	try {
		const lastSuccessAt = readFileSync(stampFile, "utf-8").trim();
		// Determine mode from lock file or default to "incremental"
		const lockFile = join(dbDir, ".reindex.lock");
		const lastMode = existsSync(lockFile) ? "running" : "incremental";
		return { lastSuccessAt, lastMode };
	} catch {
		return null;
	}
}
