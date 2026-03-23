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

import {
	readFileSync,
	writeFileSync,
	readdirSync,
	statSync,
	existsSync,
	mkdirSync,
} from "fs";
import { join } from "path";
import { execFileSync } from "child_process";
import { homedir } from "os";

const HOME = process.env.USERPROFILE || process.env.HOME || "";

// Windows project directory: find dynamically (C--Users-barwa or similar)
function findProjectsDir(): string {
	const GeminiDir = join("C:/Users/barwa/GeminiCLI/proggs/Gemini-Setup", "projects");
	if (!existsSync(GeminiDir)) return "";
	try {
		const entries = readdirSync(GeminiDir);
		// Windows: C--Users-barwa or similar
		for (const entry of entries) {
			const fullPath = join(GeminiDir, entry);
			if (statSync(fullPath).isDirectory() && entry.startsWith("C--")) {
				return fullPath;
			}
		}
		// macOS fallback: Users- prefix (e.g. Users-barwa)
		for (const entry of entries) {
			const fullPath = join(GeminiDir, entry);
			if (statSync(fullPath).isDirectory() && entry.startsWith("Users-")) {
				return fullPath;
			}
		}
		// Last resort: first directory
		for (const entry of entries) {
			const fullPath = join(GeminiDir, entry);
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
	".Gemini",
	"agent-memory",
	"shared",
	"MEMORY.md",
);

const MIN_CORRECTIONS_TO_WRITE = 3;
const MAX_AUTOPSY_ENTRIES = 10;

// v3: Closed-Loop Learning — auto-generate rules from recurring correction patterns
const LEARNING_STATE_FILE = join("C:/Users/barwa/GeminiCLI/proggs/Gemini-Setup", "learning-loop-state.json");
const AUTO_RULES_DIR = join("C:/Users/barwa/GeminiCLI/proggs/Gemini-Setup", "rules", "auto-learned");
const MAX_AUTO_RULES = 5; // cap to prevent rule bloat
const PATTERN_THRESHOLD = 3; // how many sessions with same type trigger auto-rule
const LOOKBACK_SESSIONS = 10; // how far back to look

interface LearningEntry {
	date: string;
	session_id: string;
	dominant_type: CorrectionType;
	correction_count: number;
}

interface AutoRuleEntry {
	type: CorrectionType;
	date: string;
	file: string;
	trigger_count: number; // how many sessions triggered this rule
}

interface LearningState {
	correction_history: LearningEntry[];
	auto_rules_generated: AutoRuleEntry[];
}

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
	const hookDir = join(homedir(), ".Gemini", "hooks");

	if (!existsSync(hookDir)) {
		process.stderr.write(
			`[session-autopsy] hooks directory not found at ${hookDir} — entry NOT written.\n`,
		);
		return;
	}

	// Escape strings for safe embedding in shell/PowerShell command strings.
	// Single quotes in PS1 are escaped by doubling them; in bash by closing+escaping+reopening.
	const escapedSection = section.replace(/'/g, "''");
	const escapedEntry = entry.replace(/'/g, "''");

	if (process.platform === "win32") {
		const script = join(hookDir, "whiteboard-insert.ps1");
		if (!existsSync(script)) {
			process.stderr.write(
				`[session-autopsy] whiteboard-insert.ps1 not found — entry NOT written.\n`,
			);
			return;
		}
		// Dot-source the library script and call the function directly.
		// -File would only execute the script body, which contains only function definitions.
		try {
			execFileSync(
				"powershell",
				[
					"-NoProfile",
					"-Command",
					`. '${script}'; Insert-WhiteboardEntry -Section '${escapedSection}' -Entry '${escapedEntry}'`,
				],
				{ timeout: 5000 },
			);
		} catch (e) {
			process.stderr.write(`[session-autopsy] whiteboard write failed: ${e}\n`);
		}
	} else {
		const script = join(hookDir, "whiteboard-insert.sh");
		if (!existsSync(script)) {
			process.stderr.write(
				`[session-autopsy] whiteboard-insert.sh not found — entry NOT written.\n`,
			);
			return;
		}
		// Source the library script and call the function.
		// Passing the script directly to bash would only define functions, not call them.
		const bashSection = section.replace(/'/g, "'\\''");
		const bashEntry = entry.replace(/'/g, "'\\''");
		try {
			execFileSync(
				"bash",
				[
					"-c",
					`source '${script}' && insert_whiteboard_entry '${bashSection}' '${bashEntry}'`,
				],
				{ timeout: 5000 },
			);
		} catch (e) {
			process.stderr.write(`[session-autopsy] whiteboard write failed: ${e}\n`);
		}
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
// Closed-Loop Learning (v3)
// ---------------------------------------------------------------------------

// Rule content templates per correction type — these become actual rule files
const AUTO_RULE_CONTENT: Record<CorrectionType, string> = {
	scope_drift: `# Auto-Learned Rule: Scope Drift Prevention

## Rule: Verify Every Action Against Original Intent

Before EVERY action (code change, file creation, refactoring):
1. Re-read the original user request
2. Ask yourself: "Is this DIRECTLY part of what the user asked for?"
3. If the answer is not a clear YES — STOP and ask the user

## Why This Rule Exists
This rule was auto-generated because scope drift was detected in 3+ sessions.
The system was repeatedly working on things the user did not explicitly ask for.

## Signs of Scope Drift
- "While I'm here, let me also..." — NO, do NOT
- "This could be improved by..." — only if the user asked for improvements
- "I noticed this other issue..." — mention it, don't fix it
- Refactoring code that was not part of the original request

## Recovery
If you catch yourself drifting: Stop immediately, state what you were about to do,
and ask: "Soll ich das machen oder zurueck zum urspruenglichen Ziel?"`,

	wrong_file: `# Auto-Learned Rule: File Path Verification

## Rule: Confirm File Paths Before Writing

Before EVERY Write or Edit operation:
1. Read the target file first to verify it exists and is the right file
2. If creating a new file: verify the parent directory exists
3. If the path was mentioned by the user: use it EXACTLY, do not "correct" it

## Why This Rule Exists
This rule was auto-generated because wrong file modifications were detected in 3+ sessions.

## Common Mistakes
- Editing a similarly-named file instead of the requested one
- Creating files in the wrong directory
- Overwriting files that should not be touched`,

	wrong_approach: `# Auto-Learned Rule: Approach Confirmation

## Rule: Explain Approach Before Implementing

Before starting ANY non-trivial implementation:
1. State your planned approach in 1-2 sentences
2. Wait for user confirmation (or proceed only if the approach is obvious)
3. If the user says "nein", "anders", or "so nicht" — STOP and ask for guidance

## Why This Rule Exists
This rule was auto-generated because wrong approaches were detected in 3+ sessions.
The system was implementing solutions in ways the user did not want.`,

	language_issue: `# Auto-Learned Rule: Language Consistency

## Rule: Strict Language Separation

- ALL communication with the user: German (Deutsch)
- ALL code comments: English
- ALL commit messages: English
- ALL variable names: English
- Do NOT mix languages within a single context

## Why This Rule Exists
This rule was auto-generated because language issues were detected in 3+ sessions.`,

	factual_error: `# Auto-Learned Rule: Factual Verification

## Rule: Verify Before Asserting

Before making factual claims about:
- API behavior, function signatures, library versions
- System capabilities, file locations, tool options
- Configuration syntax, command flags

ALWAYS verify by: reading the actual file, checking documentation, or testing the command.
Do NOT guess or rely on training data that may be outdated.

## Why This Rule Exists
This rule was auto-generated because factual errors were detected in 3+ sessions.`,

	other: `# Auto-Learned Rule: General Correction Prevention

## Rule: Ask When Uncertain

When you are not 100% certain about the correct action:
1. State what you think the right approach is
2. Ask the user to confirm
3. Do NOT proceed with assumptions

## Why This Rule Exists
This rule was auto-generated because recurring corrections were detected in 3+ sessions.`,
};

function readLearningState(): LearningState {
	try {
		if (existsSync(LEARNING_STATE_FILE)) {
			const raw = readFileSync(LEARNING_STATE_FILE, "utf-8");
			const parsed = JSON.parse(raw);
			// Validate structure — if corrupt, return fresh state
			if (
				Array.isArray(parsed.correction_history) &&
				Array.isArray(parsed.auto_rules_generated)
			) {
				return parsed as LearningState;
			}
		}
	} catch {
		// JSON parse error or file read error — start fresh
		process.stderr.write(
			"[session-autopsy] learning state corrupt or missing — starting fresh\n",
		);
	}
	return { correction_history: [], auto_rules_generated: [] };
}

function saveLearningState(state: LearningState): void {
	try {
		writeFileSync(LEARNING_STATE_FILE, JSON.stringify(state, null, 2), "utf-8");
	} catch (e) {
		process.stderr.write(
			`[session-autopsy] failed to save learning state: ${e}\n`,
		);
	}
}

function detectAndGenerateRules(state: LearningState): string[] {
	const generated: string[] = [];
	const recent = state.correction_history.slice(-LOOKBACK_SESSIONS);
	if (recent.length < PATTERN_THRESHOLD) return generated;

	// Count occurrences of each dominant type in recent sessions
	const typeCounts: Record<string, number> = {};
	for (const entry of recent) {
		typeCounts[entry.dominant_type] =
			(typeCounts[entry.dominant_type] || 0) + 1;
	}

	// Check if any type exceeds the threshold
	for (const [type, count] of Object.entries(typeCounts)) {
		if (count < PATTERN_THRESHOLD) continue;

		// Check if we already generated a rule for this type
		const existingRuleIdx = state.auto_rules_generated.findIndex(
			(r) => r.type === type,
		);
		if (existingRuleIdx >= 0) {
			const existingRule = state.auto_rules_generated[existingRuleIdx]!;
			// v3.1: Verify the rule file still exists on disk — if deleted (e.g. by
			// /self-improve or user), remove from state so it can be regenerated
			const existingFilePath = join(AUTO_RULES_DIR, existingRule.file);
			if (existsSync(existingFilePath)) {
				// Rule still exists — just update trigger count
				existingRule.trigger_count = count;
				existingRule.date = new Date().toISOString().split("T")[0]!;
				continue;
			} else {
				// Rule was deleted — remove from state to allow regeneration
				state.auto_rules_generated.splice(existingRuleIdx, 1);
				process.stderr.write(
					`[session-autopsy] auto-rule ${existingRule.file} was deleted — removing from state for regeneration\n`,
				);
				// Fall through to regenerate the rule below
			}
		}

		// Check max auto-rules cap
		if (state.auto_rules_generated.length >= MAX_AUTO_RULES) {
			process.stderr.write(
				`[session-autopsy] max auto-rules (${MAX_AUTO_RULES}) reached — skipping ${type}\n`,
			);
			continue;
		}

		// Generate the rule file
		const ruleContent =
			AUTO_RULE_CONTENT[type as CorrectionType] || AUTO_RULE_CONTENT.other;
		const fileName = `auto-learned-${type}.md`;
		const filePath = join(AUTO_RULES_DIR, fileName);

		try {
			// Ensure directory exists (robustness: handle deletion between sessions)
			if (!existsSync(AUTO_RULES_DIR)) {
				mkdirSync(AUTO_RULES_DIR, { recursive: true });
			}

			const header =
				`<!-- Auto-generated by session-autopsy closed-loop on ${new Date().toISOString().split("T")[0]} -->\n` +
				`<!-- Based on ${count} recurring corrections in last ${recent.length} sessions -->\n` +
				`<!-- DO NOT DELETE — this rule will be regenerated if the pattern persists -->\n\n`;

			writeFileSync(filePath, header + ruleContent, "utf-8");

			state.auto_rules_generated.push({
				type: type as CorrectionType,
				date: new Date().toISOString().split("T")[0]!,
				file: fileName,
				trigger_count: count,
			});

			generated.push(
				`Auto-Rule generiert: ${fileName} (${count}x ${type} in ${recent.length} Sessions)`,
			);
			process.stderr.write(
				`[session-autopsy] CLOSED-LOOP: Generated auto-rule ${fileName} (${count}x ${type})\n`,
			);
		} catch (e) {
			process.stderr.write(
				`[session-autopsy] failed to write auto-rule ${fileName}: ${e}\n`,
			);
		}
	}

	return generated;
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

		// v3: Always update learning state, even for clean sessions (tracks absence of corrections too)
		const learningState = readLearningState();

		if (stats.corrections.length >= MIN_CORRECTIONS_TO_WRITE) {
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
			const rule =
				RULE_SUGGESTIONS[dominant[0] as CorrectionType] || "Unbekannt";
			const summary = `- **[${date}] Autopsy ${shortId}**: ${stats.corrections.length} Korrekturen (${rate}%), Hauptmuster: ${dominant[0]} (${dominant[1]}x) — Empfehlung: ${rule}`;
			insertViaWhiteboardInsert("Debugging-Muster", summary);

			// v3: Feed correction data into the learning loop
			learningState.correction_history.push({
				date: date!,
				session_id: stats.sessionId,
				dominant_type: dominant[0] as CorrectionType,
				correction_count: stats.corrections.length,
			});

			// Keep only last 20 entries to prevent unbounded growth
			if (learningState.correction_history.length > 20) {
				learningState.correction_history =
					learningState.correction_history.slice(-20);
			}

			// v3: CLOSED-LOOP — detect recurring patterns and auto-generate rules
			const generated = detectAndGenerateRules(learningState);

			// Log auto-generated rules to whiteboard
			for (const msg of generated) {
				insertViaWhiteboardInsert(
					"Meta-Intelligenz & Selbstverbesserung",
					`- **[${date}] CLOSED-LOOP**: ${msg}`,
				);
			}
		}

		// Always save state (even if no corrections — state file tracks history)
		saveLearningState(learningState);
	} catch (e) {
		// Never block session exit, but log the error
		process.stderr.write(`[session-autopsy] unexpected error: ${e}\n`);
	}

	process.exit(0);
}

main();

