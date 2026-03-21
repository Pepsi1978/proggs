#!/usr/bin/env bun
/**
 * Session Autopsy — Meta-Evolution Artifact #2
 *
 * Runs at SessionEnd to analyze user corrections in the current session,
 * cluster them by type, and write actionable insights to AUTOPSY.md.
 *
 * Only fires when 3+ corrections are detected (otherwise the session was good).
 * Keeps a rolling window of the last 10 autopsy entries.
 */

import { readFileSync, readdirSync, statSync, existsSync } from "fs";
import { join } from "path";
import { execFileSync } from "child_process";
import { homedir } from "os";

const HOME = process.env.USERPROFILE || process.env.HOME || "";

// Windows project directory: find dynamically (C--Users-barwa or similar)
function findProjectsDir(): string {
	const claudeDir = join(HOME, ".claude", "projects");
	if (!existsSync(claudeDir)) return "";
	try {
		const entries = readdirSync(claudeDir);
		// Windows: C--Users-barwa or similar
		for (const entry of entries) {
			const fullPath = join(claudeDir, entry);
			if (statSync(fullPath).isDirectory() && entry.startsWith("C--")) {
				return fullPath;
			}
		}
		// macOS fallback: Users- prefix (e.g. Users-barwa)
		for (const entry of entries) {
			const fullPath = join(claudeDir, entry);
			if (statSync(fullPath).isDirectory() && entry.startsWith("Users-")) {
				return fullPath;
			}
		}
		// Last resort: first directory
		for (const entry of entries) {
			const fullPath = join(claudeDir, entry);
			if (statSync(fullPath).isDirectory()) return fullPath;
		}
	} catch {
		/* ignore */
	}
	return "";
}

const PROJECTS_DIR = findProjectsDir();
// v2: Write to MEMORY.md under "Debugging-Muster" instead of separate AUTOPSY.md
// (complies with "nur ein Whiteboard" rule from Deep-Scan Runde 6)
const MEMORY_FILE = join(
	HOME,
	"proggs",
	".claude",
	"agent-memory",
	"shared",
	"MEMORY.md",
);

const MIN_CORRECTIONS_TO_WRITE = 3;
const MAX_AUTOPSY_ENTRIES = 10;

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type CorrectionType =
	| "scope_drift"
	| "wrong_file"
	| "wrong_approach"
	| "language_issue"
	| "factual_error"
	| "other";

interface CorrectionEntry {
	type: CorrectionType;
	userText: string; // first 200 chars of user turn
	agentText: string; // first 200 chars of preceding assistant turn
}

// ---------------------------------------------------------------------------
// Transcript discovery (same logic as session-scorer.ts)
// ---------------------------------------------------------------------------

function findLatestTranscript(): string | null {
	if (!PROJECTS_DIR) return null;
	try {
		const entries = readdirSync(PROJECTS_DIR);
		let latest = "";
		let latestTime = 0;

		for (const entry of entries) {
			if (!entry.endsWith(".jsonl")) continue;
			// Skip agent transcripts — only process main sessions (UUID format)
			if (entry.startsWith("agent-")) continue;
			const fullPath = join(PROJECTS_DIR, entry);
			try {
				const st = statSync(fullPath);
				if (st.mtimeMs > latestTime) {
					latestTime = st.mtimeMs;
					latest = fullPath;
				}
			} catch {
				// Skip inaccessible files
			}
		}

		return latest || null;
	} catch {
		return null;
	}
}

// ---------------------------------------------------------------------------
// Correction detection (same patterns + context-filter as session-scorer.ts)
// ---------------------------------------------------------------------------

const CORRECTION_PATTERNS = [
	/\bnein\b/i,
	/\bfalsch\b/i,
	/\bnicht das\b/i,
	/\bstop\b/i,
	/\brückgängig\b/i,
	/\bundo\b/i,
	/\bwrong\b/i,
	/\bthat's not\b/i,
	/\bdon't do\b/i,
	/\bmach das nicht\b/i,
	/\bso nicht\b/i,
];

function isCorrection(text: string): boolean {
	const isQuestion = text.includes("?");
	const hasCodeBlock = text.includes("```");
	const textStart = text.slice(0, 80);
	if (isQuestion || hasCodeBlock) return false;
	return CORRECTION_PATTERNS.some((p) => p.test(textStart));
}

// ---------------------------------------------------------------------------
// Clustering
// ---------------------------------------------------------------------------

interface ClusterRule {
	type: CorrectionType;
	// patterns matched against the user text (case-insensitive)
	keywords: RegExp[];
}

// Rules are tested in order; first match wins (more specific → less specific)
const CLUSTER_RULES: ClusterRule[] = [
	{
		type: "wrong_file",
		keywords: [/falsche datei/i, /wrong file/i, /andere datei/i],
	},
	{
		type: "language_issue",
		keywords: [/auf deutsch/i, /auf englisch/i, /\bsprache\b/i],
	},
	{
		type: "scope_drift",
		keywords: [
			/nicht das/i,
			/wollte ich nicht/i,
			/\bwarum\b/i,
			/das hab ich nicht gesagt/i,
		],
	},
	{
		type: "wrong_approach",
		keywords: [/so nicht/i, /\banders\b/i, /nicht so/i, /mach es anders/i],
	},
	{
		type: "factual_error",
		keywords: [/\bfalsch\b/i, /stimmt nicht/i, /\bwrong\b/i, /\bincorrect\b/i],
	},
];

function classifyCorrection(userText: string): CorrectionType {
	for (const rule of CLUSTER_RULES) {
		if (rule.keywords.some((kw) => kw.test(userText))) {
			return rule.type;
		}
	}
	return "other";
}

// ---------------------------------------------------------------------------
// Transcript analysis
// ---------------------------------------------------------------------------

interface TranscriptStats {
	sessionId: string;
	totalTurns: number;
	corrections: CorrectionEntry[];
}

function analyzeTranscript(transcriptPath: string): TranscriptStats {
	const content = readFileSync(transcriptPath, "utf-8");
	const lines = content.trim().split("\n");

	const sessionId =
		transcriptPath.split(/[/\\]/).pop()?.replace(".jsonl", "") || "unknown";

	let totalTurns = 0;
	const corrections: CorrectionEntry[] = [];

	// We need to look at adjacent turns, so keep a sliding window
	let lastAssistantText = "";

	for (const line of lines) {
		let entry: any;
		try {
			entry = JSON.parse(line);
		} catch {
			continue;
		}

		const entryType: string = entry.type;
		const msg = entry.message;
		if (!msg) continue;

		if (entryType === "assistant") {
			// Capture the text of this assistant turn for the next user comparison
			const contentBlocks = Array.isArray(msg.content) ? msg.content : [];
			const textParts: string[] = [];
			for (const block of contentBlocks) {
				if (block.type === "text" && typeof block.text === "string") {
					textParts.push(block.text);
				}
			}
			if (textParts.length === 0 && typeof msg.content === "string") {
				textParts.push(msg.content);
			}
			lastAssistantText = textParts.join(" ").trim().slice(0, 200);
		}

		if (entryType === "user") {
			totalTurns++;

			const rawText =
				typeof msg.content === "string"
					? msg.content
					: JSON.stringify(msg.content);

			if (isCorrection(rawText)) {
				const userSnippet = rawText.trim().slice(0, 200);
				const corrType = classifyCorrection(userSnippet);
				corrections.push({
					type: corrType,
					userText: userSnippet,
					agentText: lastAssistantText || "(no prior assistant turn)",
				});
			}

			// Reset after consuming — next assistant turn replaces it
			lastAssistantText = "";
		}
	}

	return { sessionId, totalTurns, corrections };
}

// ---------------------------------------------------------------------------
// Rule suggestion
// ---------------------------------------------------------------------------

const RULE_SUGGESTIONS: Record<CorrectionType, string> = {
	scope_drift:
		"Vor jeder Aktion prüfen: Ist das Teil der ursprünglichen Anfrage?",
	wrong_file: "Datei-Pfade explizit bestätigen bevor Änderungen gemacht werden",
	wrong_approach:
		"Lösungsansatz kurz erklären bevor mit der Implementierung begonnen wird",
	language_issue: "Kommunikation auf Deutsch, Code/Commits auf Englisch",
	factual_error: "Faktische Aussagen mit Web-Recherche verifizieren",
	other: "Öfter beim Benutzer rückfragen ob der Ansatz korrekt ist",
};

// ---------------------------------------------------------------------------
// MEMORY.md section-based insertion via whiteboard-insert script.
// Delegates to whiteboard-insert.ps1 (Windows) or whiteboard-insert.sh (macOS/Linux)
// so that all locking and section logic lives in one place.
// Uses execFileSync to avoid shell injection (args passed as array, not interpolated).
// ---------------------------------------------------------------------------

function insertViaWhiteboardInsert(section: string, entry: string): void {
	const hookDir = join(homedir(), ".claude", "hooks");

	if (!existsSync(hookDir)) {
		process.stderr.write(
			`[session-autopsy] hooks directory not found at ${hookDir} — entry NOT written.\n`,
		);
		return;
	}

	if (process.platform === "win32") {
		const script = join(hookDir, "whiteboard-insert.ps1");
		if (!existsSync(script)) {
			process.stderr.write(
				`[session-autopsy] whiteboard-insert.ps1 not found — entry NOT written.\n`,
			);
			return;
		}
		// Pass args as array to execFileSync — no shell expansion, injection-safe
		execFileSync(
			"powershell",
			["-NoProfile", "-File", script, "-Section", section, "-Entry", entry],
			{ timeout: 5000 },
		);
	} else {
		const script = join(hookDir, "whiteboard-insert.sh");
		if (!existsSync(script)) {
			process.stderr.write(
				`[session-autopsy] whiteboard-insert.sh not found — entry NOT written.\n`,
			);
			return;
		}
		// Pass args as array — bash receives them as $1 and $2, no shell injection
		execFileSync("bash", [script, section, entry], { timeout: 5000 });
	}
}

// ---------------------------------------------------------------------------
// Markdown formatting
// ---------------------------------------------------------------------------

function escapeCell(text: string): string {
	// Replace pipe characters so they don't break the markdown table
	return text.replace(/\|/g, "\\|").replace(/\n/g, " ");
}

function formatAutopsyEntry(stats: TranscriptStats): string {
	const { sessionId, totalTurns, corrections } = stats;

	const date = new Date().toISOString().split("T")[0];
	const shortId = sessionId.slice(0, 8);
	const rate =
		totalTurns > 0
			? ((corrections.length / totalTurns) * 100).toFixed(1)
			: "0.0";

	// Count by type
	const counts: Record<CorrectionType, number> = {
		scope_drift: 0,
		wrong_file: 0,
		wrong_approach: 0,
		language_issue: 0,
		factual_error: 0,
		other: 0,
	};
	for (const c of corrections) {
		counts[c.type]++;
	}

	// Determine most frequent type
	const dominantType = (
		Object.entries(counts) as [CorrectionType, number][]
	).reduce((best, curr) => (curr[1] > best[1] ? curr : best))[0];

	const dominantCount = counts[dominantType];
	const rule = RULE_SUGGESTIONS[dominantType];

	// Build table rows
	const rows = corrections
		.map((c, i) => {
			const userShort = escapeCell(c.userText.slice(0, 60));
			const agentShort = escapeCell(c.agentText.slice(0, 60));
			return `| ${i + 1} | ${c.type} | "${userShort}" | "${agentShort}" |`;
		})
		.join("\n");

	return (
		`### [${date}] Session Autopsy — ${shortId}\n` +
		`- **Korrekturen gesamt**: ${corrections.length} (Rate: ${rate}%)\n` +
		`- **Häufigstes Muster**: ${dominantType} (${dominantCount} von ${corrections.length})\n` +
		`- **Details**:\n` +
		`  | # | Typ | User sagte | Agent tat vorher |\n` +
		`  |---|-----|-----------|------------------|\n` +
		rows
			.split("\n")
			.map((r) => `  ${r}`)
			.join("\n") +
		`\n` +
		`- **Empfohlene Rule**: ${rule}\n\n`
	);
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

function main(): void {
	try {
		const transcript = findLatestTranscript();
		if (!transcript) {
			process.exit(0);
		}

		const stats = analyzeTranscript(transcript);

		if (stats.corrections.length < MIN_CORRECTIONS_TO_WRITE) {
			// Session was clean — nothing to report
			process.exit(0);
		}

		// v2: Write condensed summary to MEMORY.md "Debugging-Muster" section
		const date = new Date().toISOString().split("T")[0];
		const shortId = stats.sessionId.slice(0, 8);
		const typeCounts: Record<string, number> = {};
		for (const c of stats.corrections) {
			typeCounts[c.type] = (typeCounts[c.type] || 0) + 1;
		}
		const dominant = Object.entries(typeCounts).reduce((a, b) =>
			b[1] > a[1] ? b : a,
		);
		const rate =
			stats.totalTurns > 0
				? ((stats.corrections.length / stats.totalTurns) * 100).toFixed(1)
				: "0.0";
		const rule = RULE_SUGGESTIONS[dominant[0] as CorrectionType] || "Unbekannt";
		const summary = `- **[${date}] Autopsy ${shortId}**: ${stats.corrections.length} Korrekturen (${rate}%), Hauptmuster: ${dominant[0]} (${dominant[1]}x) — Empfehlung: ${rule}`;
		insertViaWhiteboardInsert("Debugging-Muster", summary);
	} catch {
		// Never block session exit
	}

	process.exit(0);
}

main();
