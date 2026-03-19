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

const CHUNK_SIZE = 50;
const MAX_FILE_SIZE = 100 * 1024; // 100 KB

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

	// Build ignore patterns from SKIP_DIRS
	const ignore = SKIP_DIRS.flatMap((dir) => [`**/${dir}/**`, `**/${dir}`]);

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
 * Split a file into chunks of ~50 lines.
 * Tries to split at blank lines or function boundaries.
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
		const content = `File: ${relPath}\n${chunkLines.join("\n")}`;

		chunks.push({ filePath: relPath, startLine, endLine, content, language });

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
