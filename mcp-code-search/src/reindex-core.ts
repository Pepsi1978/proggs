// reindex-core.ts — Core reindex logic for the semantic search database.
// Handles both full and incremental reindexing of codebases.

import {
	existsSync,
	readFileSync,
	renameSync,
	statSync,
	writeFileSync,
} from "fs";
import { join, relative } from "path";

import { findCodeFiles, chunkFile } from "./indexer.js";
import { generateEmbeddings, checkOllama } from "./ollama.js";
import { VectorStore } from "./store.js";
import { ensureDbDir, listDbCandidates } from "./db-state.js";

export interface ReindexOptions {
	rootDir: string;
	dbDir: string;
	stampFile?: string;
	forceFullReindex?: boolean;
	logger?: (message: string) => void;
}

export interface ReindexResult {
	message: string;
	filesProcessed: number;
	chunksGenerated: number;
	mode: "full" | "incremental";
}

const BATCH_SIZE = 16;

function writeAtomicFile(filePath: string, contents: string): void {
	const tempPath = `${filePath}.tmp`;
	writeFileSync(tempPath, contents, "utf8");
	renameSync(tempPath, filePath);
}

/**
 * Reindex a codebase — either full or incremental.
 *
 * Incremental mode: only processes files modified since the last stamp file.
 * Full mode: clears the index and re-processes all files.
 */
export async function reindexCodebase(
	options: ReindexOptions,
): Promise<ReindexResult> {
	const {
		rootDir,
		dbDir,
		stampFile = join(dbDir, ".last-index-time"),
		forceFullReindex = false,
		logger = () => {},
	} = options;

	// Check Ollama availability
	const ollamaCheck = await checkOllama();
	if (!ollamaCheck.ok) {
		throw new Error(`Ollama not available: ${ollamaCheck.error}`);
	}

	ensureDbDir(dbDir);

	const dbPath = join(dbDir, "index.db");
	const store = new VectorStore(dbPath);

	try {
		// Determine reindex mode
		const hasExistingIndex =
			!forceFullReindex && existsSync(dbPath) && store.stats().totalChunks > 0;
		const isIncremental = hasExistingIndex && !forceFullReindex;

		if (!isIncremental) {
			logger("Starting full reindex...");
			store.clear();
		} else {
			logger("Starting incremental reindex...");
		}

		// Find all code files
		const allFiles = await findCodeFiles(rootDir);
		logger(`Found ${allFiles.length} code files`);

		// Determine which files need processing
		let filesToProcess: string[];

		if (isIncremental && existsSync(stampFile)) {
			const lastIndexTime = statSync(stampFile).mtimeMs;
			filesToProcess = allFiles.filter((filePath) => {
				try {
					return statSync(filePath).mtimeMs > lastIndexTime;
				} catch {
					return false;
				}
			});

			// Also check for deleted files
			const indexedFiles = new Set(store.getIndexedFiles());
			const currentFiles = new Set(
				allFiles.map((f) => relative(rootDir, f).replace(/\\/g, "/")),
			);

			let deletedCount = 0;
			for (const indexedFile of indexedFiles) {
				if (!currentFiles.has(indexedFile)) {
					store.deleteByFilePath(indexedFile);
					deletedCount++;
				}
			}

			if (deletedCount > 0) {
				logger(`Removed ${deletedCount} deleted files from index`);
			}

			if (filesToProcess.length === 0 && deletedCount === 0) {
				return {
					message: "Index is up to date — no changes detected.",
					filesProcessed: 0,
					chunksGenerated: 0,
					mode: "incremental",
				};
			}

			logger(`${filesToProcess.length} files changed since last index`);
		} else {
			filesToProcess = allFiles;
		}

		// Process files
		let filesProcessed = 0;
		let chunksGenerated = 0;

		for (const filePath of filesToProcess) {
			const chunks = await chunkFile(filePath, rootDir);

			if (chunks.length === 0) continue;

			// For incremental: delete old chunks for this file first
			if (isIncremental) {
				const relPath = relative(rootDir, filePath).replace(/\\/g, "/");
				store.deleteByFilePath(relPath);
			}

			// Generate embeddings in batches
			for (let i = 0; i < chunks.length; i += BATCH_SIZE) {
				const batch = chunks.slice(i, i + BATCH_SIZE);
				const texts = batch.map((c) => c.content);
				const embeddings = await generateEmbeddings(texts);

				store.insertBatch(batch, embeddings);
			}

			chunksGenerated += chunks.length;
			filesProcessed++;

			if (filesProcessed % 50 === 0) {
				logger(
					`Progress: ${filesProcessed}/${filesToProcess.length} files, ${chunksGenerated} chunks`,
				);
			}
		}

		// Update stamp file
		const successAt = new Date().toISOString();
		try {
			writeAtomicFile(stampFile, successAt);
		} catch {
			// Non-critical: stamp file write failure
		}

		const mode = isIncremental ? "incremental" : "full";
		const stats = store.stats();
		const currentDbFile = "index.db";
		const state = {
			activeDb: currentDbFile,
			totalFiles: stats.totalFiles,
			totalChunks: stats.totalChunks,
			lastMode: mode,
			lastSuccessAt: successAt,
			lastRunSuccessful: true,
			lastRunIncremental: mode === "incremental",
			lastWriteMode: mode,
			lastWriteAt: successAt,
			lastWriteSuccessful: true,
			lastWriteIncremental: mode === "incremental",
		};

		try {
			writeAtomicFile(join(dbDir, "current.txt"), `${currentDbFile}\n`);
			writeAtomicFile(
				join(dbDir, "state.json"),
				`${JSON.stringify(state, null, 2)}\n`,
			);
		} catch {
			// Non-critical: state file writes should not block a successful index.
		}

		return {
			message:
				`Reindex complete (${mode}): ${filesProcessed} files, ${chunksGenerated} chunks. ` +
				`Total index: ${stats.totalFiles} files, ${stats.totalChunks} chunks.`,
			filesProcessed,
			chunksGenerated,
			mode,
		};
	} finally {
		store.close();
	}
}
