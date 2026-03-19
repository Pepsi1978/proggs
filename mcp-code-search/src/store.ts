// Vector Store — sqlite-vec powered via bun:sqlite
// Stores code chunks with their embeddings in SQLite.
// Uses sqlite-vec extension for fast approximate nearest neighbor search.

import { Database } from "bun:sqlite";
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
	private db: Database;

	constructor(dbPath: string) {
		this.db = new Database(dbPath);
		sqliteVec.load(this.db);

		this.db.exec("PRAGMA journal_mode = WAL");

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
	}

	clear(): void {
		this.db.exec("DELETE FROM vec_chunks");
		this.db.exec("DELETE FROM chunks");
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
			const rowId = result.lastInsertRowid;
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
				const rowId = result.lastInsertRowid;
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

	close(): void {
		this.db.close();
	}
}
