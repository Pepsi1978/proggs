// reindex.ts — Standalone reindex script for the semantic search database.
// Called by the reindex-codebase.sh hook with args: <rootDir> <dbPath>
// Creates a new index DB, then writes the DB filename to current.txt (pointer swap).

import { findCodeFiles, chunkFile } from "./indexer.ts";
import { generateEmbeddings } from "./ollama.ts";
import { VectorStore } from "./store.ts";
import { resolve, join, basename, dirname } from "path";
import { mkdirSync, existsSync, writeFileSync } from "fs";

async function main() {
	const rootDir = resolve(process.argv[2] ?? "");
	const dbPath = resolve(process.argv[3] ?? "");

	if (!rootDir || !dbPath) {
		console.error("Usage: reindex.ts <rootDir> <dbPath>");
		process.exit(1);
	}

	const dbDir = dirname(dbPath);
	const dbName = basename(dbPath);
	if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });

	const store = new VectorStore(dbPath);

	const files = await findCodeFiles(rootDir);
	console.log(`Found ${files.length} files to index`);

	const allChunks: Array<{
		filePath: string;
		startLine: number;
		endLine: number;
		content: string;
		language: string;
	}> = [];
	for (const file of files) {
		const chunks = await chunkFile(file, rootDir);
		allChunks.push(...chunks);
	}
	console.log(`Generated ${allChunks.length} chunks`);

	const BATCH = 32;
	let processed = 0;
	for (let i = 0; i < allChunks.length; i += BATCH) {
		const batch = allChunks.slice(i, i + BATCH);
		const embeddings = await generateEmbeddings(batch.map((c) => c.content));
		store.insertBatch(batch, embeddings);
		processed += batch.length;
		// Progress every 10 batches
		if (Math.floor(i / BATCH) % 10 === 0) {
			console.log(`Progress: ${processed}/${allChunks.length} chunks embedded`);
		}
	}

	store.close();

	// Pointer swap — atomic from the reader's perspective
	writeFileSync(join(dbDir, "current.txt"), dbName);
	console.log(
		`Done: ${dbName} (${files.length} files, ${allChunks.length} chunks)`,
	);
}

main().catch((e) => {
	console.error(e);
	process.exit(1);
});
