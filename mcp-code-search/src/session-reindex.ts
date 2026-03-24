// session-reindex.ts — CLI entry point for incremental reindex at session start.
// Called by the SessionStart hook. Only re-embeds files changed since last index.
// If no index exists yet, falls back to a full reindex automatically.

import { existsSync, renameSync, unlinkSync, readdirSync } from "fs";
import { join, resolve } from "path";

import { reindexCodebase } from "./reindex-core.js";
import { resolvePreferredDbPath } from "./db-state.js";

const rootDir = resolve(process.argv[2] || process.cwd());
const dbDir = join(rootDir, ".code-search");

// Migration: if only pointer-based index-N.db exists (from old full-reindex hook),
// rename it to index.db so reindex-core can do incremental updates on it.
const preferredDb = existsSync(dbDir) ? resolvePreferredDbPath(dbDir) : null;

if (preferredDb && !preferredDb.endsWith("index.db")) {
	const targetDb = join(dbDir, "index.db");
	if (!existsSync(targetDb)) {
		try {
			renameSync(preferredDb, targetDb);
			// Also rename WAL/SHM files if they exist
			for (const suffix of ["-wal", "-shm"]) {
				const src = preferredDb + suffix;
				if (existsSync(src)) {
					renameSync(src, targetDb + suffix);
				}
			}
			console.log(`Migrated ${preferredDb} → index.db`);
		} catch (err) {
			// If rename fails (locked), just proceed — reindex-core will create index.db fresh
			console.warn(`Migration failed (will do full reindex): ${err}`);
		}
	}
}

// Clean up old pointer-based files (current.txt, stale index-N.db)
if (existsSync(dbDir)) {
	const pointerFile = join(dbDir, "current.txt");
	if (existsSync(pointerFile)) {
		try {
			unlinkSync(pointerFile);
		} catch {}
	}

	// Remove leftover index-N.db files (not index.db)
	try {
		for (const f of readdirSync(dbDir)) {
			if (/^index-\d+\.db/.test(f)) {
				try {
					unlinkSync(join(dbDir, f));
				} catch {}
			}
		}
	} catch {}
}

// Run incremental reindex
try {
	const result = await reindexCodebase({
		rootDir,
		dbDir,
		forceFullReindex: false,
		logger: (msg) => console.log(msg),
	});

	console.log(result.message);
	process.exit(0);
} catch (err) {
	console.error(`Reindex failed: ${err}`);
	process.exit(1);
}
