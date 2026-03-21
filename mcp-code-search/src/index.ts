// mcp-code-search — Local Semantic Code Search MCP Server
// Uses Ollama + nomic-embed-text for embeddings, sqlite-vec for vector search.
// Tools: index_codebase, search_code, status
// Uses pointer-based DB switching to avoid file locking conflicts during reindex.

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { resolve, join } from "path";
import { existsSync, mkdirSync, readFileSync, readdirSync } from "fs";

import {
	generateEmbeddings,
	generateEmbedding,
	checkOllama,
} from "./ollama.js";
import { VectorStore } from "./store.js";
import { findCodeFiles, chunkFile } from "./indexer.js";

// DB management: uses a single index.db updated in-place by incremental reindex.
// Fallback to old numbered index-N.db via current.txt for backwards compatibility.
let store: VectorStore | null = null;
let currentDbPath: string | null = null;

function resolveCurrentDb(dbDir: string): string {
	// Primary: single index.db (v4+ incremental reindex)
	if (existsSync(join(dbDir, "index.db"))) {
		return join(dbDir, "index.db");
	}
	// Fallback: old pointer-based system (current.txt → index-N.db)
	const pointerFile = join(dbDir, "current.txt");
	if (existsSync(pointerFile)) {
		const pointer = readFileSync(pointerFile, "utf-8").trim();
		if (pointer && existsSync(join(dbDir, pointer))) {
			return join(dbDir, pointer);
		}
	}
	// Last resort: find highest numbered index-N.db
	if (existsSync(dbDir)) {
		let maxN = 0;
		let found = "";
		for (const f of readdirSync(dbDir)) {
			const match = f.match(/^index-(\d+)\.db$/);
			if (match) {
				const n = parseInt(match[1], 10);
				if (n > maxN) {
					maxN = n;
					found = f;
				}
			}
		}
		if (found) return join(dbDir, found);
	}
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

const server = new McpServer({
	name: "code-search",
	version: "1.1.0",
});

// --- Tool: index_codebase ---
server.tool(
	"index_codebase",
	"Full reindex of a local codebase for semantic code search. Clears the existing index and re-indexes all files from scratch. Use this only for first-time indexing or to fix a corrupted index — the SessionStart hook handles incremental updates automatically.",
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

		// Close existing store so we can rewrite the DB
		if (store) {
			store.close();
			store = null;
			currentDbPath = null;
		}

		const dbPath = join(dbDir, "index.db");
		const vs = new VectorStore(dbPath);
		vs.clear();

		const files = await findCodeFiles(rootDir);
		if (files.length === 0) {
			vs.close();
			return {
				content: [
					{
						type: "text" as const,
						text: `No code files found in ${rootDir}`,
					},
				],
			};
		}

		let totalChunks = 0;
		for (const file of files) {
			const chunks = await chunkFile(file, rootDir);
			if (chunks.length === 0) continue;
			const embeddings = await generateEmbeddings(chunks.map((c) => c.content));
			vs.insertBatch(chunks, embeddings);
			totalChunks += chunks.length;
		}

		const stats = vs.stats();
		vs.close();

		return {
			content: [
				{
					type: "text" as const,
					text: `Indexed ${stats.totalFiles} files, ${stats.totalChunks} chunks in ${rootDir}.\nDatabase: ${dbPath}`,
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

// --- Graceful shutdown: close DB on exit ---
function shutdown() {
	if (store) {
		try {
			store.close();
		} catch {
			// best-effort
		}
		store = null;
	}
	process.exit(0);
}
process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);

// --- Start server ---
const transport = new StdioServerTransport();
await server.connect(transport);
