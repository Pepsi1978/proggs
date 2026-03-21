// reindex.ts — Incremental reindex for the semantic search database.
// Default: Only re-indexes files that changed since the last run.
// Full reindex: Only when DB doesn't exist yet or --full flag is passed.
//
// Usage:
//   tsx reindex.ts <rootDir> <dbPath>              # incremental
//   tsx reindex.ts <rootDir> <dbPath> --full        # full reindex
//   tsx reindex.ts <rootDir> <dbPath> <stampFile>   # incremental, custom stamp

import { findCodeFiles, chunkFile } from "./indexer.ts";
import { generateEmbeddings } from "./ollama.ts";
import { VectorStore } from "./store.ts";
import { resolve, join, relative, dirname } from "path";
import { mkdirSync, existsSync, statSync, writeFileSync } from "fs";

async function main() {
	const rootDir = resolve(process.argv[2] ?? "");
	const dbPath = resolve(process.argv[3] ?? "");
	const arg4 = process.argv[4] ?? "";

	if (!rootDir || !dbPath) {
		console.error("Usage: reindex.ts <rootDir> <dbPath> [--full|<stampFile>]");
		process.exit(1);
	}

	const dbDir = dirname(dbPath);
	if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });

	const forceFullReindex = arg4 === "--full";
	const stampFile =
		arg4 && arg4 !== "--full" ? resolve(arg4) : join(dbDir, ".last-index-time");
	const dbExists = existsSync(dbPath);

	// Decide: full or incremental
	const doFullReindex = forceFullReindex || !dbExists;

	const store = new VectorStore(dbPath);

	// Verify DB integrity before incremental reindex — if corrupt, force full rebuild
	if (!doFullReindex && !store.integrityCheck()) {
		console.warn(
			"DB integrity check FAILED — switching to full reindex to recover",
		);
		await fullReindex(store, rootDir);
	} else if (doFullReindex) {
		await fullReindex(store, rootDir);
	} else {
		await incrementalReindex(store, rootDir, stampFile);
	}

	store.close();
	console.log("Done.");
}

async function fullReindex(store: VectorStore, rootDir: string) {
	console.log("Full reindex...");
	store.clear();

	const files = await findCodeFiles(rootDir);
	console.log(`Found ${files.length} files to index`);

	let totalChunks = 0;
	let filesSkipped = 0;
	for (const file of files) {
		try {
			const chunks = await chunkFile(file, rootDir);
			if (chunks.length === 0) continue;

			const embeddings = await generateEmbeddings(chunks.map((c) => c.content));
			store.insertBatch(chunks, embeddings);
			totalChunks += chunks.length;

			if (totalChunks % 200 === 0) {
				console.log(`Progress: ${totalChunks} chunks embedded`);
			}
		} catch (err) {
			filesSkipped++;
			const msg = err instanceof Error ? err.message : String(err);
			console.error(`  SKIP ${file}: ${msg}`);
		}
	}

	const skipMsg =
		filesSkipped > 0 ? ` (${filesSkipped} skipped due to errors)` : "";
	console.log(
		`Full reindex complete: ${files.length} files, ${totalChunks} chunks${skipMsg}`,
	);
}

async function incrementalReindex(
	store: VectorStore,
	rootDir: string,
	stampFile: string,
) {
	// Determine which files changed since last index
	let stampTime = 0;
	if (existsSync(stampFile)) {
		stampTime = statSync(stampFile).mtimeMs;
	}

	const allFiles = await findCodeFiles(rootDir);
	const indexedFiles = new Set(store.getIndexedFiles());

	// Find changed files (newer than stamp) and new files (not in index)
	const changedFiles: string[] = [];
	const currentFiles = new Set<string>();

	for (const absPath of allFiles) {
		const relPath = relative(rootDir, absPath).replace(/\\/g, "/");
		currentFiles.add(relPath);

		const mtime = statSync(absPath).mtimeMs;
		if (mtime > stampTime || !indexedFiles.has(relPath)) {
			changedFiles.push(absPath);
		}
	}

	// Find deleted files (in index but no longer on disk)
	const deletedFiles: string[] = [];
	for (const indexedFile of indexedFiles) {
		if (!currentFiles.has(indexedFile)) {
			deletedFiles.push(indexedFile);
		}
	}

	if (changedFiles.length === 0 && deletedFiles.length === 0) {
		console.log("Index is up to date — nothing to do.");
		return;
	}

	console.log(
		`Incremental reindex: ${changedFiles.length} changed/new, ${deletedFiles.length} deleted`,
	);

	// Remove deleted files from index
	for (const relPath of deletedFiles) {
		const removed = store.deleteByFilePath(relPath);
		if (removed > 0) {
			console.log(`  Removed ${removed} chunks: ${relPath}`);
		}
	}

	// Re-index changed/new files (per-file error isolation)
	let totalChunks = 0;
	let filesProcessed = 0;
	let filesSkipped = 0;
	for (const absPath of changedFiles) {
		const relPath = relative(rootDir, absPath).replace(/\\/g, "/");

		try {
			// Delete old chunks for this file (if any)
			store.deleteByFilePath(relPath);

			// Generate new chunks
			const chunks = await chunkFile(absPath, rootDir);
			if (chunks.length === 0) continue;

			// Generate embeddings and insert
			const embeddings = await generateEmbeddings(chunks.map((c) => c.content));
			store.insertBatch(chunks, embeddings);

			totalChunks += chunks.length;
			filesProcessed++;
			console.log(
				`  [${filesProcessed}/${changedFiles.length}] ${relPath} (${chunks.length} chunks)`,
			);
		} catch (err) {
			filesSkipped++;
			const msg = err instanceof Error ? err.message : String(err);
			console.error(`  SKIP ${relPath}: ${msg}`);
			// Continue with remaining files — don't let one bad file abort everything
		}
	}

	const stats = store.stats();
	const skipMsg =
		filesSkipped > 0 ? ` (${filesSkipped} skipped due to errors)` : "";
	console.log(
		`Incremental reindex complete: ${filesProcessed} files updated, ${totalChunks} chunks${skipMsg}. ` +
			`Total index: ${stats.totalFiles} files, ${stats.totalChunks} chunks.`,
	);
}

main().catch((e) => {
	console.error(e);
	process.exit(1);
});
