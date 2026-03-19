// mcp-code-search — Local Semantic Code Search MCP Server
// Uses Ollama + nomic-embed-text for embeddings, sqlite-vec for vector search.
// Tools: index_codebase, search_code, status
// Uses pointer-based DB switching to avoid file locking conflicts during reindex.

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { resolve, join } from "path";
import {
	existsSync,
	mkdirSync,
	readFileSync,
	writeFileSync,
	readdirSync,
	unlinkSync,
} from "fs";

import {
	generateEmbeddings,
	generateEmbedding,
	checkOllama,
} from "./ollama.js";
import { VectorStore } from "./store.js";
import { findCodeFiles, chunkFile } from "./indexer.js";

// Pointer-based DB management: current.txt points to the active DB file.
// The reindex hook writes to a NEW file and only updates the pointer when done.
// This avoids Windows file locking issues during reindex.
let store: VectorStore | null = null;
let currentDbPath: string | null = null;

function resolveCurrentDb(dbDir: string): string {
	const pointerFile = join(dbDir, "current.txt");
	if (existsSync(pointerFile)) {
		const pointer = readFileSync(pointerFile, "utf-8").trim();
		if (pointer && existsSync(join(dbDir, pointer))) {
			return join(dbDir, pointer);
		}
	}
	// Fallback: use index.db directly (backwards compatible)
	return join(dbDir, "index.db");
}

function getStore(rootDir: string): VectorStore {
	const dbDir = join(rootDir, ".code-search");
	if (!existsSync(dbDir)) {
		mkdirSync(dbDir, { recursive: true });
	}

	const targetDb = resolveCurrentDb(dbDir);

	// Reopen if the pointer changed (reindex happened) or first call
	if (!store || currentDbPath !== targetDb) {
		store?.close();
		store = new VectorStore(targetDb);
		currentDbPath = targetDb;
	}
	return store;
}

function nextDbName(dbDir: string): string {
	// Find the highest index-N.db number and return N+1
	let maxN = 0;
	if (existsSync(dbDir)) {
		for (const f of readdirSync(dbDir)) {
			const match = f.match(/^index-(\d+)\.db$/);
			if (match) {
				const n = parseInt(match[1], 10);
				if (n > maxN) maxN = n;
			}
		}
	}
	return `index-${maxN + 1}.db`;
}

function cleanupOldDbs(dbDir: string, currentFile: string): void {
	// Delete old index-N.db files that are not the current one
	if (!existsSync(dbDir)) return;
	for (const f of readdirSync(dbDir)) {
		if (
			f.match(/^index-\d+\.db/) &&
			!f.startsWith(currentFile.replace(".db", ""))
		) {
			try {
				unlinkSync(join(dbDir, f));
			} catch {
				// File still locked — skip, will be cleaned up next time
			}
		}
	}
}

const server = new McpServer({
	name: "code-search",
	version: "1.1.0",
});

// --- Tool: index_codebase ---
server.tool(
	"index_codebase",
	"Index a local codebase for semantic code search. Walks all source files, chunks them, generates embeddings via Ollama, and stores in a local vector database. Safe to run while searching — uses atomic pointer swap.",
	{
		directory: z
			.string()
			.describe("Absolute path to the codebase root directory to index"),
	},
	async ({ directory }) => {
		const rootDir = resolve(directory);

		const ollamaCheck = await checkOllama();
		if (!ollamaCheck.ok) {
			return {
				content: [
					{ type: "text" as const, text: `Error: ${ollamaCheck.error}` },
				],
			};
		}

		const dbDir = join(rootDir, ".code-search");
		if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });

		// Write to a NEW db file (pointer-based, no file locking conflicts)
		const newDbName = nextDbName(dbDir);
		const newDbPath = join(dbDir, newDbName);
		const newStore = new VectorStore(newDbPath);

		const files = await findCodeFiles(rootDir);
		if (files.length === 0) {
			newStore.close();
			try {
				unlinkSync(newDbPath);
			} catch {}
			return {
				content: [
					{ type: "text" as const, text: `No code files found in ${rootDir}` },
				],
			};
		}

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

		if (allChunks.length === 0) {
			newStore.close();
			try {
				unlinkSync(newDbPath);
			} catch {}
			return {
				content: [
					{
						type: "text" as const,
						text: `Files found but no chunks generated (files may be empty or binary)`,
					},
				],
			};
		}

		const EMBED_BATCH = 32;
		for (let i = 0; i < allChunks.length; i += EMBED_BATCH) {
			const batch = allChunks.slice(i, i + EMBED_BATCH);
			const texts = batch.map((c) => c.content);
			const embeddings = await generateEmbeddings(texts);
			newStore.insertBatch(batch, embeddings);
		}

		const stats = newStore.stats();
		newStore.close();

		// Update pointer to new DB (atomic — just a text file write)
		writeFileSync(join(dbDir, "current.txt"), newDbName);

		// Close old store so next getStore() picks up the new pointer
		if (store) {
			store.close();
			store = null;
			currentDbPath = null;
		}

		// Clean up old DB files (best-effort, skips locked files)
		cleanupOldDbs(dbDir, newDbName);

		return {
			content: [
				{
					type: "text" as const,
					text: `Indexed ${stats.totalFiles} files, ${stats.totalChunks} chunks in ${rootDir}.\nDatabase: ${newDbPath}`,
				},
			],
		};
	},
);

// --- Tool: search_code ---
server.tool(
	"search_code",
	"Search the indexed codebase using natural language. Returns the most semantically similar code chunks.",
	{
		query: z
			.string()
			.describe(
				"Natural language search query (e.g. 'database connection handling')",
			),
		directory: z
			.string()
			.describe("Absolute path to the codebase root (must be indexed first)"),
		limit: z
			.number()
			.default(10)
			.describe("Number of results to return (default: 10)"),
	},
	async ({ query, directory, limit }) => {
		const rootDir = resolve(directory);
		const vs = getStore(rootDir);

		const stats = vs.stats();
		if (stats.totalChunks === 0) {
			return {
				content: [
					{
						type: "text" as const,
						text: `No index found for ${rootDir}. Run index_codebase first.`,
					},
				],
			};
		}

		const ollamaCheck = await checkOllama();
		if (!ollamaCheck.ok) {
			return {
				content: [
					{ type: "text" as const, text: `Error: ${ollamaCheck.error}` },
				],
			};
		}

		const queryEmbedding = await generateEmbedding(query);
		const results = vs.search(queryEmbedding, limit);

		if (results.length === 0) {
			return {
				content: [
					{ type: "text" as const, text: `No results found for: "${query}"` },
				],
			};
		}

		const formatted = results
			.map(
				(r, i) =>
					`### Result ${i + 1} (score: ${r.score.toFixed(3)})\n` +
					`**${r.filePath}** lines ${r.startLine}-${r.endLine} [${r.language}]\n` +
					"```\n" +
					r.content +
					"\n```",
			)
			.join("\n\n");

		return {
			content: [{ type: "text" as const, text: formatted }],
		};
	},
);

// --- Tool: search_status ---
server.tool(
	"search_status",
	"Show the status of the semantic code search index for a directory.",
	{
		directory: z.string().describe("Absolute path to the codebase root"),
	},
	async ({ directory }) => {
		const rootDir = resolve(directory);
		const dbDir = join(rootDir, ".code-search");

		if (!existsSync(dbDir)) {
			return {
				content: [
					{
						type: "text" as const,
						text: `No index found at ${rootDir}/.code-search/`,
					},
				],
			};
		}

		const currentDb = resolveCurrentDb(dbDir);
		if (!existsSync(currentDb)) {
			return {
				content: [
					{
						type: "text" as const,
						text: `No index database found. Run index_codebase first.`,
					},
				],
			};
		}

		const vs = getStore(rootDir);
		const stats = vs.stats();

		return {
			content: [
				{
					type: "text" as const,
					text:
						`Index status for ${rootDir}:\n` +
						`- Files indexed: ${stats.totalFiles}\n` +
						`- Code chunks: ${stats.totalChunks}\n` +
						`- Database: ${currentDb}\n` +
						`- Ollama model: nomic-embed-text (768 dimensions)`,
				},
			],
		};
	},
);

// --- Start server ---
const transport = new StdioServerTransport();
await server.connect(transport);
