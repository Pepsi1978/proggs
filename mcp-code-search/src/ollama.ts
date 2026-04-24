// Ollama Embedding Client
// Calls Ollama's /api/embed endpoint to generate vector embeddings for code chunks.
// Uses snowflake-arctic-embed2 model (1024 dimensions, 8192 token context).

import { spawnSync, spawn } from "child_process";

const OLLAMA_URL = "http://localhost:11434";
const EMBED_MODEL = "snowflake-arctic-embed2";
const BATCH_SIZE = 32;
const MAX_RETRIES = 3;
const RETRY_BASE_MS = 1000; // exponential backoff: 1s, 2s, 4s
const FETCH_TIMEOUT_MS = 30_000; // 30 second timeout per request

export interface EmbeddingResult {
	embeddings: number[][];
}

/**
 * Attempts to ensure Ollama is running and the model is pulled.
 */
async function ensureOllamaService(): Promise<{ ok: boolean; error?: string }> {
	try {
		const response = await fetch(`${OLLAMA_URL}/api/tags`, {
			signal: AbortSignal.timeout(2000),
		});
		if (response.ok) return { ok: true };
	} catch (err) {
		// Not running, try to start it
		console.error("Ollama not running. Attempting to start...");
		if (process.platform === "win32") {
			spawn("cmd.exe", ["/c", "start", "ollama", "serve"], {
				detached: true,
				stdio: "ignore",
			}).unref();
		} else {
			spawn("ollama", ["serve"], {
				detached: true,
				stdio: "ignore",
			}).unref();
		}

		// Wait for it to start (up to 10 seconds)
		for (let i = 0; i < 10; i++) {
			await new Promise((r) => setTimeout(r, 1000));
			try {
				const response = await fetch(`${OLLAMA_URL}/api/tags`, {
					signal: AbortSignal.timeout(1000),
				});
				if (response.ok) return { ok: true };
			} catch {
				// Continue waiting
			}
		}
		return {
			ok: false,
			error: "Failed to start Ollama service automatically.",
		};
	}
	return { ok: true };
}

async function ensureModelAvailable(): Promise<{
	ok: boolean;
	error?: string;
}> {
	const response = await fetch(`${OLLAMA_URL}/api/tags`);
	const data = (await response.json()) as { models?: { name: string }[] };
	const models = data.models ?? [];
	const hasModel = models.some(
		(m) => m.name === EMBED_MODEL || m.name.startsWith(`${EMBED_MODEL}:`),
	);

	if (!hasModel) {
		console.error(`Model "${EMBED_MODEL}" missing. Pulling...`);
		const pullResult = spawnSync("ollama", ["pull", EMBED_MODEL], {
			encoding: "utf8",
		});
		if (pullResult.status !== 0) {
			return {
				ok: false,
				error: `Failed to pull model ${EMBED_MODEL}: ${pullResult.stderr}`,
			};
		}
	}
	return { ok: true };
}

/**
 * Generate embeddings for one or more text inputs via Ollama.
 * @param texts Array of text strings to embed
 * @returns Array of embedding vectors (each 1024-dimensional)
 * @throws Error if Ollama is not running or model not found
 */
export async function generateEmbeddings(texts: string[]): Promise<number[][]> {
	const health = await checkOllama();
	if (!health.ok) {
		throw new Error(health.error);
	}

	const results: number[][] = [];

	// Process in batches of BATCH_SIZE to avoid overloading Ollama
	for (let i = 0; i < texts.length; i += BATCH_SIZE) {
		const batch = texts.slice(i, i + BATCH_SIZE);

		let response: Response | undefined;
		let lastError: Error | undefined;

		// Retry loop with exponential backoff
		for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
			try {
				response = await fetch(`${OLLAMA_URL}/api/embed`, {
					method: "POST",
					headers: { "Content-Type": "application/json" },
					body: JSON.stringify({ model: EMBED_MODEL, input: batch }),
					signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
				});
				lastError = undefined;
				break; // success — exit retry loop
			} catch (err) {
				lastError = err instanceof Error ? err : new Error(String(err));
				if (attempt < MAX_RETRIES - 1) {
					const delay = RETRY_BASE_MS * 2 ** attempt;
					await new Promise((r) => setTimeout(r, delay));
				}
			}
		}

		if (!response) {
			throw new Error(
				`Cannot connect to Ollama at ${OLLAMA_URL} after ${MAX_RETRIES} attempts. Is Ollama running? (${lastError?.message ?? "unknown"})`,
			);
		}

		if (!response.ok) {
			const body = await response.text().catch(() => "");
			if (response.status === 404) {
				throw new Error(
					`Ollama model "${EMBED_MODEL}" not found. Run: ollama pull ${EMBED_MODEL}`,
				);
			}
			throw new Error(
				`Ollama /api/embed returned HTTP ${response.status}: ${body}`,
			);
		}

		const data = (await response.json()) as EmbeddingResult;

		if (!Array.isArray(data.embeddings)) {
			throw new Error(
				`Unexpected response from Ollama: missing "embeddings" field`,
			);
		}

		results.push(...data.embeddings);
	}

	return results;
}

/**
 * Generate embedding for a single text.
 */
export async function generateEmbedding(text: string): Promise<number[]> {
	const embeddings = await generateEmbeddings([text]);
	return embeddings[0];
}

/**
 * Check if Ollama is running and the embed model is available.
 * Attempts to start Ollama and pull the model if necessary.
 */
export async function checkOllama(): Promise<{ ok: boolean; error?: string }> {
	const serviceHealth = await ensureOllamaService();
	if (!serviceHealth.ok) {
		return serviceHealth;
	}

	const modelHealth = await ensureModelAvailable();
	if (!modelHealth.ok) {
		return modelHealth;
	}

	return { ok: true };
}
