// mcp-code-search — Local Semantic Code Search MCP Server
// Uses Ollama + nomic-embed-text for embeddings, sqlite-vec for vector search.
// Tools: index_codebase, search_code, search_status
// Uses pointer-based DB switching so searches stay on a complete index during reindex.

import { existsSync, readFileSync } from "fs";
import { join, resolve } from "path";

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

import { ensureDbDir, listDbCandidates, readReindexState } from "./db-state.js";
import { checkOllama, generateEmbedding } from "./ollama.js";
import { reindexCodebase } from "./reindex-core.js";
import { VectorStore } from "./store.js";

let store: VectorStore | null = null;
let currentDbPath: string | null = null;

function formatError(error: unknown): string {
	return error instanceof Error ? error.message : String(error);
}

function closeCachedStore(): void {
	if (!store) {
		currentDbPath = null;
		return;
	}

	try {
		store.close();
	} catch {
		// Best effort close only.
	}

	store = null;
	currentDbPath = null;
}

function getStore(rootDir: string): VectorStore {
	const dbDir = join(rootDir, ".code-search");
	ensureDbDir(dbDir);

	let lastError: unknown = null;

	for (const candidate of listDbCandidates(dbDir)) {
		if (store && currentDbPath === candidate.path) {
			try {
				store.stats();
				return store;
			} catch (error) {
				lastError = error;
				closeCachedStore();
			}
		}

		try {
			closeCachedStore();
			store = new VectorStore(candidate.path);
			store.stats();
			currentDbPath = candidate.path;
			return store;
		} catch (error) {
			lastError = error;
			console.error(
				`code-search: failed to open ${candidate.path}: ${formatError(error)}`,
			);
			closeCachedStore();
		}
	}

	if (lastError) {
		throw lastError;
	}

	throw new Error(`No usable index database found in ${dbDir}`);
}

interface LastWriteInfo {
	lastWriteAt: string | null;
	lastWriteMode: "full" | "incremental" | null;
	lastWriteSuccessful: boolean;
	lastWriteIncremental: boolean;
}

function readLastWriteInfo(
	dbDir: string,
	state: ReturnType<typeof readReindexState>,
): LastWriteInfo {
	const logPath = join(dbDir, "reindex.log");
	let currentCheckAt: string | null = null;
	let lastWriteAt: string | null = null;
	let lastWriteMode: LastWriteInfo["lastWriteMode"] = null;

	if (existsSync(logPath)) {
		const logText = readFileSync(logPath, "utf8");
		for (const rawLine of logText.split(/\r?\n/)) {
			const line = rawLine.trim();
			const checkMatch = line.match(/^\[(.+?)\]\s+Codex auto-reindex check for /);
			if (checkMatch) {
				currentCheckAt = checkMatch[1] ?? null;
				continue;
			}

			if (line === "Incremental reindex complete.") {
				lastWriteAt = currentCheckAt;
				lastWriteMode = "incremental";
				continue;
			}

			if (line === "Full reindex complete.") {
				lastWriteAt = currentCheckAt;
				lastWriteMode = "full";
			}
		}
	}

	if (!lastWriteMode && state && (state.lastMode === "incremental" || state.lastMode === "full")) {
		lastWriteAt = state.lastSuccessAt;
		lastWriteMode = state.lastMode;
	}

	return {
		lastWriteAt,
		lastWriteMode,
		lastWriteSuccessful: Boolean(lastWriteAt && lastWriteMode),
		lastWriteIncremental: lastWriteMode === "incremental",
	};
}

const server = new McpServer({
	name: "code-search",
	version: "1.2.0",
});

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

		const result = await reindexCodebase({
			rootDir,
			dbDir: join(rootDir, ".code-search"),
			forceFullReindex: true,
		});

		closeCachedStore();

		return {
			content: [{ type: "text" as const, text: result.message }],
		};
	},
);

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

		let vs: VectorStore;
		try {
			vs = getStore(rootDir);
		} catch {
			return {
				content: [
					{
						type: "text" as const,
						text: `No usable index found for ${rootDir}. Run index_codebase first.`,
					},
				],
			};
		}

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
				(result, index) =>
					`### Result ${index + 1} (score: ${result.score.toFixed(3)})\n` +
					`**${result.filePath}** lines ${result.startLine}-${result.endLine} [${result.language}]\n` +
					"```\n" +
					result.content +
					"\n```",
			)
			.join("\n\n");

		return {
			content: [{ type: "text" as const, text: formatted }],
		};
	},
);

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

		let vs: VectorStore;
		try {
			vs = getStore(rootDir);
		} catch {
			return {
				content: [
					{
						type: "text" as const,
						text: `No usable index database found. Run index_codebase first.`,
					},
				],
			};
		}

		const stats = vs.stats();
		const state = readReindexState(dbDir);
		const lastWrite = readLastWriteInfo(dbDir, state);
		const extraState = [
			`\n- Last successful reindex: ${state?.lastSuccessAt ?? "none"}`,
			`- Last mode: ${state?.lastMode ?? "none"}`,
			`- Last run successful: ${state?.lastSuccessAt ? "yes" : "no"}`,
			`- Last run incremental: ${state?.lastMode === "incremental" ? "yes" : "no"}`,
			`- Last write at: ${lastWrite.lastWriteAt ?? "none"}`,
			`- Last write mode: ${lastWrite.lastWriteMode ?? "none"}`,
			`- Last write successful: ${lastWrite.lastWriteSuccessful ? "yes" : "no"}`,
			`- Last write incremental: ${lastWrite.lastWriteIncremental ? "yes" : "no"}`,
		].join("\n");

		return {
			content: [
				{
					type: "text" as const,
					text:
						`Index status for ${rootDir}:\n` +
						`- Files indexed: ${stats.totalFiles}\n` +
						`- Code chunks: ${stats.totalChunks}\n` +
						`- Database: ${currentDbPath}\n` +
						`- Ollama model: nomic-embed-text (768 dimensions)` +
						extraState,
				},
			],
		};
	},
);

function shutdown() {
	closeCachedStore();
	process.exit(0);
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);

const transport = new StdioServerTransport();
await server.connect(transport);
