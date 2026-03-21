// Vector Store — sqlite-vec powered via better-sqlite3
// Stores code chunks with their embeddings in SQLite.
// Uses sqlite-vec extension for fast approximate nearest neighbor search.
// Note: bun:sqlite on macOS does not support loadExtension, so we use
// better-sqlite3 which works on both Node.js and Bun across all platforms.

import Database from "better-sqlite3";
import * as sqliteVec from "sqlite-vec";

const VECTOR_DIM = 768; // nomic-embed-text dimension

export interface CodeChunk {
	filePath: string;
	startLine: number;
	endLine: number;
	content: string;
	language: string;
}

export interface SearchResult extends CodeChunk {
	score: number; // similarity, 0-1
}

export class VectorStore {
	private db: InstanceType<typeof Database>;

	constructor(dbPath: string) {
		this.db = new Database(dbPath);
		sqliteVec.load(this.db);

		this.db.exec("PRAGMA journal_mode = WAL");
		this.db.exec("PRAGMA busy_timeout = 5000"); // wait up to 5s if DB is locked
		this.db.exec("PRAGMA synchronous = NORMAL"); // safe with WAL, faster than FULL

		this.db.exec(`
			CREATE TABLE IF NOT EXISTS chunks (
				id          INTEGER PRIMARY KEY,
				file_path   TEXT    NOT NULL,
				start_line  INTEGER NOT NULL,
				end_line    INTEGER NOT NULL,
				content     TEXT    NOT NULL,
				language    TEXT    NOT NULL
			)
		`);

		this.db.exec(
			`CREATE VIRTUAL TABLE IF NOT EXISTS vec_chunks USING vec0(embedding float[${VECTOR_DIM}])`,
		);

		// Index for fast file-based lookups (incremental reindex)
		this.db.exec(
			"CREATE INDEX IF NOT EXISTS idx_chunks_file_path ON chunks(file_path)",
		);
	}

	clear(): void {
		this.db.exec("DELETE FROM vec_chunks");
		this.db.exec("DELETE FROM chunks");
	}

	/** Delete all chunks belonging to a file path. Returns number of deleted chunks. */
	deleteByFilePath(filePath: string): number {
		const ids = this.db
			.prepare("SELECT id FROM chunks WHERE file_path = ?")
			.all(filePath) as Array<{ id: number }>;

		if (ids.length === 0) return 0;

		const deleteVec = this.db.prepare("DELETE FROM vec_chunks WHERE rowid = ?");
		const deleteChunk = this.db.prepare("DELETE FROM chunks WHERE id = ?");

		const run = this.db.transaction(() => {
			for (const { id } of ids) {
				deleteVec.run(id);
				deleteChunk.run(id);
			}
		});

		run();
		return ids.length;
	}

	/** Get all unique file paths currently in the index. */
	getIndexedFiles(): string[] {
		const rows = this.db
			.prepare("SELECT DISTINCT file_path FROM chunks ORDER BY file_path")
			.all() as Array<{ file_path: string }>;
		return rows.map((r) => r.file_path);
	}

	insert(chunk: CodeChunk, embedding: number[]): void {
		const insertChunk = this.db.prepare(
			"INSERT INTO chunks (file_path, start_line, end_line, content, language) VALUES (?, ?, ?, ?, ?)",
		);
		const insertVec = this.db.prepare(
			"INSERT INTO vec_chunks (rowid, embedding) VALUES (?, vec_f32(?))",
		);

		const run = this.db.transaction(() => {
			const result = insertChunk.run(
				chunk.filePath,
				chunk.startLine,
				chunk.endLine,
				chunk.content,
				chunk.language,
			);
			const rowId = BigInt(result.lastInsertRowid);
			const buf = new Float32Array(embedding);
			insertVec.run(rowId, buf);
		});

		run();
	}

	insertBatch(chunks: CodeChunk[], embeddings: number[][]): void {
		if (chunks.length !== embeddings.length) {
			throw new Error(
				`Chunk count (${chunks.length}) must match embedding count (${embeddings.length})`,
			);
		}

		const insertChunk = this.db.prepare(
			"INSERT INTO chunks (file_path, start_line, end_line, content, language) VALUES (?, ?, ?, ?, ?)",
		);
		const insertVec = this.db.prepare(
			"INSERT INTO vec_chunks (rowid, embedding) VALUES (?, vec_f32(?))",
		);

		const run = this.db.transaction(() => {
			for (let i = 0; i < chunks.length; i++) {
				const chunk = chunks[i]!;
				const embedding = embeddings[i]!;

				const result = insertChunk.run(
					chunk.filePath,
					chunk.startLine,
					chunk.endLine,
					chunk.content,
					chunk.language,
				);
				const rowId = BigInt(result.lastInsertRowid);
				const buf = new Float32Array(embedding);
				insertVec.run(rowId, buf);
			}
		});

		run();
	}

	search(queryEmbedding: number[], limit: number): SearchResult[] {
		const queryBuf = new Float32Array(queryEmbedding);

		const vecQuery = this.db.prepare(
			"SELECT rowid, distance FROM vec_chunks WHERE embedding MATCH ? ORDER BY distance LIMIT ?",
		);
		const chunkQuery = this.db.prepare(
			"SELECT file_path, start_line, end_line, content, language FROM chunks WHERE id = ?",
		);

		const vecRows = vecQuery.all(queryBuf, limit) as Array<{
			rowid: number;
			distance: number;
		}>;

		const results: SearchResult[] = [];

		for (const { rowid, distance } of vecRows) {
			const row = chunkQuery.get(rowid) as
				| {
						file_path: string;
						start_line: number;
						end_line: number;
						content: string;
						language: string;
				  }
				| undefined;

			if (row == null) continue;

			const score = 1 / (1 + distance);

			results.push({
				filePath: row.file_path,
				startLine: row.start_line,
				endLine: row.end_line,
				content: row.content,
				language: row.language,
				score,
			});
		}

		return results;
	}

	stats(): { totalChunks: number; totalFiles: number } {
		const row = this.db
			.prepare(
				"SELECT COUNT(*) AS total_chunks, COUNT(DISTINCT file_path) AS total_files FROM chunks",
			)
			.get() as { total_chunks: number; total_files: number };

		return {
			totalChunks: row.total_chunks,
			totalFiles: row.total_files,
		};
	}

	/** Run a quick integrity check. Returns true if DB is healthy. */
	integrityCheck(): boolean {
		try {
			const row = this.db.prepare("PRAGMA integrity_check(1)").get() as
				| { integrity_check: string }
				| undefined;
			return row?.integrity_check === "ok";
		} catch {
			return false;
		}
	}

	close(): void {
		try {
			this.db.exec("PRAGMA optimize");
		} catch {
			// best-effort — don't fail on close
		}
		this.db.close();
	}
}
