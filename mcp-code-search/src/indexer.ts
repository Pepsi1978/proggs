// Code Indexer — walks directories, chunks code files
// Splits code files into chunks of ~50 lines each, respecting function boundaries where possible.
// Skips binary files, node_modules, .git, build artifacts.

import { glob } from "glob";
import { readFile, stat } from "fs/promises";
import { extname, relative } from "path";

export interface CodeChunk {
	filePath: string;
	startLine: number;
	endLine: number;
	content: string;
	language: string;
}

// File extensions to index, mapped to language name
const LANGUAGE_MAP: Record<string, string> = {
	".ts": "typescript",
	".tsx": "typescript",
	".js": "javascript",
	".jsx": "javascript",
	".py": "python",
	".rs": "rust",
	".go": "go",
	".kt": "kotlin",
	".java": "java",
	".cs": "csharp",
	".swift": "swift",
	".c": "c",
	".cpp": "cpp",
	".h": "c",
	".md": "markdown",
	".json": "json",
	".yaml": "yaml",
	".yml": "yaml",
	".toml": "toml",
	".xml": "xml",
	".html": "html",
	".css": "css",
	".sql": "sql",
	".sh": "shell",
	".ps1": "powershell",
	".gradle": "groovy",
};

// Directories to skip
const SKIP_DIRS = [
	"node_modules",
	".git",
	".gradle",
	"build",
	"dist",
	"target",
	".cache",
	".next",
	"__pycache__",
	".idea",
	".vs",
	"bin",
	"obj",
	"Pods",
	".build",
	".swiftpm",
];

// File patterns to skip even when their extension would otherwise be indexable.
// These are usually machine-generated or minified — indexing them pollutes search
// results with noise AND routinely blows through the embedding model's token limit.
const SKIP_FILE_PATTERNS = [
	"**/package-lock.json",
	"**/pnpm-lock.yaml",
	"**/yarn.lock",
	"**/bun.lock",
	"**/Cargo.lock",
	"**/Gemfile.lock",
	"**/poetry.lock",
	"**/composer.lock",
	"**/*.min.js",
	"**/*.min.css",
	"**/*.map",
	"**/*.lock.json",
	"**/*-lock.json",
];

const CHUNK_SIZE = 40; // target number of lines per chunk
const MAX_FILE_SIZE = 300 * 1024; // 300 KB — snowflake-arctic-embed2 handles larger files
// Conservative char budget per chunk. snowflake-arctic-embed2 has 8192 token context;
// at ~3.5 chars/token that's ~28000 chars. We stay well below (6000) because smaller
// chunks produce sharper semantic matches in retrieval — see retrieval literature.
const MAX_CHUNK_CHARS = 6000;

/**
 * Walk a directory and find all indexable code files.
 * @param rootDir Root directory to walk
 * @returns Array of absolute file paths
 */
export async function findCodeFiles(rootDir: string): Promise<string[]> {
	// Build the extension pattern for glob
	const extensions = Object.keys(LANGUAGE_MAP).map((ext) =>
		// ext starts with ".", strip it for the glob pattern
		ext.slice(1),
	);
	const extPattern = `**/*.{${extensions.join(",")}}`;

	// Build ignore patterns from SKIP_DIRS and SKIP_FILE_PATTERNS.
	const ignore = [
		...SKIP_DIRS.flatMap((dir) => [`**/${dir}/**`, `**/${dir}`]),
		...SKIP_FILE_PATTERNS,
	];

	const matches = await glob(extPattern, {
		cwd: rootDir,
		absolute: true,
		ignore,
		nodir: true,
		dot: false,
	});

	// Filter out files larger than 100 KB
	const results: string[] = [];
	for (const filePath of matches) {
		try {
			const info = await stat(filePath);
			if (info.size <= MAX_FILE_SIZE) {
				results.push(filePath);
			}
		} catch {
			// Skip files we can't stat
		}
	}

	return results.sort();
}

/**
 * Detect language from file extension.
 */
export function detectLanguage(filePath: string): string {
	const ext = extname(filePath).toLowerCase();
	return LANGUAGE_MAP[ext] ?? "unknown";
}

/**
 * Recursively split lines into chunks that fit within the embedding model context.
 * If a chunk is too long, it gets halved. Single lines that exceed the budget are
 * sliced by character count — that's the Poka-Yoke against minified / machine-
 * generated files where one "line" can be hundreds of thousands of characters.
 */
function splitAndPush(
	chunkLines: string[],
	relPath: string,
	startLine: number,
	language: string,
	out: CodeChunk[],
): void {
	const headerPrefix = `File: ${relPath}\n`;
	const content = `${headerPrefix}${chunkLines.join("\n")}`;

	if (content.length <= MAX_CHUNK_CHARS) {
		out.push({
			filePath: relPath,
			startLine,
			endLine: startLine + chunkLines.length - 1,
			content,
			language,
		});
		return;
	}

	// Single line exceeds the budget — slice by characters. Without this guard
	// minified files (e.g. one-line 500k-char bundles) crash Ollama with HTTP 400
	// "input length exceeds the context length".
	if (chunkLines.length <= 1) {
		const line = chunkLines[0] ?? "";
		const usable = Math.max(1, MAX_CHUNK_CHARS - headerPrefix.length);
		for (let offset = 0; offset < line.length; offset += usable) {
			const piece = line.slice(offset, offset + usable);
			out.push({
				filePath: relPath,
				startLine,
				endLine: startLine,
				content: `${headerPrefix}${piece}`,
				language,
			});
		}
		return;
	}

	// Split in half and recurse.
	const mid = Math.floor(chunkLines.length / 2);
	splitAndPush(chunkLines.slice(0, mid), relPath, startLine, language, out);
	splitAndPush(chunkLines.slice(mid), relPath, startLine + mid, language, out);
}

/**
 * Split a file into chunks of ~40 lines.
 * Tries to split at blank lines. Oversized chunks are recursively halved.
 */
export async function chunkFile(
	filePath: string,
	rootDir: string,
): Promise<CodeChunk[]> {
	let raw: string;
	try {
		raw = await readFile(filePath, "utf-8");
	} catch {
		return [];
	}

	// Guard against binary / non-UTF-8 content that slipped through
	if (raw.includes("\0")) {
		return [];
	}

	const lines = raw.split("\n");
	const language = detectLanguage(filePath);
	const relPath = relative(rootDir, filePath).replace(/\\/g, "/");
	const chunks: CodeChunk[] = [];

	let chunkStart = 0; // 0-based index into `lines`

	while (chunkStart < lines.length) {
		// Desired end is chunkStart + CHUNK_SIZE - 1 (0-based, inclusive)
		let chunkEnd = Math.min(chunkStart + CHUNK_SIZE - 1, lines.length - 1);

		// If there are more lines after this chunk, search for a better split point.
		// Walk backwards up to 15 lines looking for a blank line.
		if (chunkEnd < lines.length - 1) {
			let bestSplit = chunkEnd;
			for (
				let i = chunkEnd;
				i >= chunkStart + Math.floor(CHUNK_SIZE / 2);
				i--
			) {
				if (lines[i].trim() === "") {
					bestSplit = i;
					break;
				}
			}
			chunkEnd = bestSplit;
		}

		// 1-based line numbers for the interface
		const startLine = chunkStart + 1;
		const endLine = chunkEnd + 1;

		const chunkLines = lines.slice(chunkStart, chunkEnd + 1);

		// Recursively split chunks that exceed embedding model context
		splitAndPush(chunkLines, relPath, startLine, language, chunks);

		// Advance past the chunk; skip leading blank lines of the next chunk
		chunkStart = chunkEnd + 1;
		while (chunkStart < lines.length && lines[chunkStart].trim() === "") {
			chunkStart++;
		}
	}

	return chunks;
}

/**
 * Index an entire directory: find files, chunk them all.
 * Yields progress updates.
 */
export async function* indexDirectory(rootDir: string): AsyncGenerator<{
	status: "progress" | "done";
	filesFound?: number;
	filesProcessed?: number;
	chunksGenerated?: number;
	currentFile?: string;
}> {
	const files = await findCodeFiles(rootDir);

	yield { status: "progress", filesFound: files.length };

	let filesProcessed = 0;
	let chunksGenerated = 0;

	for (const filePath of files) {
		const relPath = relative(rootDir, filePath).replace(/\\/g, "/");
		const fileChunks = await chunkFile(filePath, rootDir);
		chunksGenerated += fileChunks.length;
		filesProcessed++;

		// Yield a progress update every 10 files
		if (filesProcessed % 10 === 0) {
			yield {
				status: "progress",
				filesFound: files.length,
				filesProcessed,
				chunksGenerated,
				currentFile: relPath,
			};
		}
	}

	yield { status: "done", filesProcessed, chunksGenerated };
}
