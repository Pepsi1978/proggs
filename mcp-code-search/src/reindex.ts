// reindex.ts — Incremental reindex for the semantic search database.
// Default: Only re-indexes files that changed since the last successful run.
// Full reindex: Only when there is no usable active index yet or --full is passed.
//
// Usage:
//   tsx reindex.ts <rootDir>                       # compatibility mode
//   tsx reindex.ts <rootDir> <dbPath>              # incremental
//   tsx reindex.ts <rootDir> <dbPath> --full       # full reindex
//   tsx reindex.ts <rootDir> <dbPath> <stampFile>  # incremental, custom stamp

import { dirname, join, resolve } from "path";
import { existsSync, mkdirSync } from "fs";

import { reindexCodebase } from "./reindex-core.js";

async function main() {
	const rootArg = process.argv[2] ?? "";
	const pathOrFlagArg = process.argv[3] ?? "";
	const arg4 = process.argv[4] ?? "";

	if (!rootArg) {
		console.error(
			"Usage: reindex.ts <rootDir> [<dbPath>] [--full|<stampFile>]",
		);
		process.exit(1);
	}

	const rootDir = resolve(rootArg);
	const dbPathArg = pathOrFlagArg === "--full" ? "" : pathOrFlagArg;
	const dbPath = dbPathArg
		? resolve(dbPathArg)
		: join(rootDir, ".code-search", "index.db");
	const dbDir = dirname(dbPath);
	if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });

	const modeArg = pathOrFlagArg === "--full" ? "--full" : arg4;
	const forceFullReindex = modeArg === "--full";
	const stampFile =
		modeArg && modeArg !== "--full"
			? resolve(modeArg)
			: join(dbDir, ".last-index-time");

	const result = await reindexCodebase({
		rootDir,
		dbDir,
		stampFile,
		forceFullReindex,
		logger: (message) => console.log(message),
	});

	console.log(result.message);
}

main().catch((error) => {
	console.error(error);
	process.exit(1);
});
