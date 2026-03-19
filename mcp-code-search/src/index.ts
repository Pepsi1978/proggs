// mcp-code-search — Local Semantic Code Search MCP Server
// Uses Ollama + nomic-embed-text for embeddings, sqlite-vec for vector search.
// Tools: index_codebase, search_code, status

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { resolve, join } from "path";

import {
	generateEmbeddings,
	generateEmbedding,
	checkOllama,
} from "./ollama.js";
import { VectorStore } from "./store.js";
import { findCodeFiles, chunkFile } from "./indexer.js";

// Store lives next to the indexed codebase as .code-search/index.db
let store: VectorStore | null = null;
let indexedRoot: string | null = null;

function getStore(rootDir: string): VectorStore {
	const dbDir = join(rootDir, ".code-search");
	const fs = require("fs");
	if (!fs.existsSync(dbDir)) {
		fs.mkdirSync(dbDir, { recursive: true });
	}
	if (!store || indexedRoot !== rootDir) {
		store?.close();
		store = new VectorStore(join(dbDir, "index.db"));
		indexedRoot = rootDir;
	}
	return store;
}

const server = new McpServer({
	name: "code-search",
	version: "1.0.0",
});

// --- Tool: index_codebase ---
server.tool(
	"index_codebase",
	"Index a local codebase for semantic code search. Walks all source files, chunks them, generates embeddings via Ollama, and stores in a local vector database.",
	{
		directory: z
			.string()
			.describe("Absolute path to the codebase root directory to index"),
	},
	async ({ directory }) => {
		const rootDir = resolve(directory);

		// Check Ollama availability
		const ollamaCheck = await checkOllama();
		if (!ollamaCheck.ok) {
			return {
				content: [
					{ type: "text" as const, text: `Error: ${ollamaCheck.error}` },
				],
			};
		}

		const vs = getStore(rootDir);
		vs.clear(); // Full re-index in v1

		// Find all code files
		const files = await findCodeFiles(rootDir);
		if (files.length === 0) {
			return {
				content: [
					{ type: "text" as const, text: `No code files found in ${rootDir}` },
				],
			};
		}

		// Chunk all files
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
			return {
				content: [
					{
						type: "text" as const,
						text: `Files found but no chunks generated (files may be empty or binary)`,
					},
				],
			};
		}

		// Generate embeddings in batches
		const EMBED_BATCH = 32;
		let embeddedCount = 0;

		for (let i = 0; i < allChunks.length; i += EMBED_BATCH) {
			const batch = allChunks.slice(i, i + EMBED_BATCH);
			const texts = batch.map((c) => c.content);

			const embeddings = await generateEmbeddings(texts);

			vs.insertBatch(batch, embeddings);
			embeddedCount += batch.length;
		}

		const stats = vs.stats();
		return {
			content: [
				{
					type: "text" as const,
					text: `Indexed ${stats.totalFiles} files, ${stats.totalChunks} chunks in ${rootDir}.\nDatabase: ${rootDir}/.code-search/index.db`,
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

		// Check Ollama for query embedding
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
		const dbPath = join(rootDir, ".code-search", "index.db");

		const fs = require("fs");
		if (!fs.existsSync(dbPath)) {
			return {
				content: [
					{
						type: "text" as const,
						text: `No index found at ${rootDir}/.code-search/`,
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
						`- Database: ${dbPath}\n` +
						`- Ollama model: nomic-embed-text (768 dimensions)`,
				},
			],
		};
	},
);

// --- Start server ---
const transport = new StdioServerTransport();
await server.connect(transport);
