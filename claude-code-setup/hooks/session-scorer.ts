#!/usr/bin/env bun
/**
 * Session Scorer — Meta-Evolution Artifact #1 (Cross-Platform)
 *
 * Runs at SessionEnd to analyze the current session transcript
 * and extract quality metrics into a JSONL log file.
 *
 * v3: Removed direct MEMORY.md writes (violates Add-Content rule).
 * Trend warnings go to stderr (hook log) — evolution-analyst reads session-scores.jsonl.
 * Added iq_score field (filled by /self-improve Stufe 5E).
 * Fixed error regex false positives, removed hardcoded paths.
 */

import {
	readFileSync,
	appendFileSync,
	readdirSync,
	statSync,
	existsSync,
} from "fs";
import { join } from "path";
import { execFileSync } from "child_process";
import { homedir } from "os";

const HOME = process.env.USERPROFILE || process.env.HOME || "";
if (!HOME) process.exit(0);
const SCORES_FILE = join(HOME, ".claude", "session-scores.jsonl");

function findProjectsDir(): string {
	const claudeDir = join(HOME, ".claude", "projects");
	if (!existsSync(claudeDir)) return "";
	try {
		const entries = readdirSync(claudeDir);
		for (const entry of entries) {
			const fullPath = join(claudeDir, entry);
			if (statSync(fullPath).isDirectory() && entry.startsWith("C--")) {
				return fullPath;
			}
		}
		// macOS fallback: Users-*
		for (const entry of entries) {
			const fullPath = join(claudeDir, entry);
			if (statSync(fullPath).isDirectory() && entry.startsWith("Users-")) {
				return fullPath;
			}
		}
		// Last resort: first directory
		for (const entry of entries) {
			const fullPath = join(claudeDir, entry);
			if (statSync(fullPath).isDirectory()) {
				return fullPath;
			}
		}
	} catch {
		/* ignore */
	}
	return "";
}

const PROJECTS_DIR = findProjectsDir();

interface SessionMetrics {
	date: string;
	session_id: string;
	total_turns: number;
	tool_calls: number;
	errors: number;
	corrections: number;
	quality_score: number;
	iq_score: number; // Filled by /self-improve Stufe 5E, default 0
	meta_intelligence_score: number; // 0=nothing, 1=memory/docs, 2=rules/hooks/agents
	had_intelligence_suggestion: boolean; // true if 💡 pattern found in assistant output
}

function findLatestTranscript(): string | null {
	if (!PROJECTS_DIR) return null;
	try {
		const entries = readdirSync(PROJECTS_DIR);
		let latest = "";
		let latestTime = 0;

		for (const entry of entries) {
			if (!entry.endsWith(".jsonl")) continue;
			if (entry.startsWith("agent-")) continue;
			const fullPath = join(PROJECTS_DIR, entry);
			try {
				const stat = statSync(fullPath);
				if (stat.mtimeMs > latestTime) {
					latestTime = stat.mtimeMs;
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

function analyzeTranscript(path: string): SessionMetrics {
	const content = readFileSync(path, "utf-8");
	const lines = content.trim().split("\n");

	let totalTurns = 0;
	let toolCalls = 0;
	let errors = 0;
	let corrections = 0;
	let hadIntelligenceSuggestion = false;

	// v4: Meta-intelligence tracking — did this session improve the system itself?
	// 0 = no system changes, 1 = minor (memory/CLAUDE.md), 2 = significant (rules/hooks/agents)
	let metaIntelligenceLevel = 0;
	// Paths that indicate system self-improvement — restricted to .claude/ paths
	// to avoid false positives from project files (e.g. game-rules.md, git-hooks/)
	const significantPaths = /\/\.claude\/(rules|hooks|agents)\//i;
	const minorPaths =
		/\/\.claude\/.*(memory|MEMORY\.md|agent-memory)|\/CLAUDE\.md$/i;

	// v3: Added ASCII-safe alternatives for umlauts (cross-platform safety)
	const correctionPatterns = [
		/\bnein\b/i,
		/\bfalsch\b/i,
		/\bnicht das\b/i,
		/\bstop\b/i,
		/\br[uü]ckg[aä]ngig\b/i, // handles both rückgängig and rueckgaengig
		/\bundo\b/i,
		/\bwrong\b/i,
		/\bthat's not\b/i,
		/\bdon't do\b/i,
		/\bmach das nicht\b/i,
		/\bso nicht\b/i,
	];

	// v3: More precise error detection — exclude common non-error patterns
	const errorPattern =
		/\b(error|failed|exception|ENOENT|EPERM|FATAL|CRASHED)\b/i;
	const errorExclusions =
		/error[-\s.]handling|error[-\s.]message|error[-\s.]type|fix[-\s.]the[-\s.]error|catch[-\s.]error|on[-\s.]error|if[-\s.]error|no[-\s.]error/i;

	for (const line of lines) {
		try {
			const entry = JSON.parse(line);
			const entryType = entry.type;
			const msg = entry.message;

			if (entryType === "user" && msg) {
				totalTurns++;
				const text =
					typeof msg.content === "string"
						? msg.content
						: JSON.stringify(msg.content);
				const isQuestion = text.includes("?");
				const hasCodeBlock = text.includes("```");
				const textStart = text.slice(0, 80);
				for (const pattern of correctionPatterns) {
					if (pattern.test(textStart) && !isQuestion && !hasCodeBlock) {
						corrections++;
						break;
					}
				}
			}

			if (entryType === "assistant" && msg) {
				const content = Array.isArray(msg.content) ? msg.content : [];

				// v4: Check assistant text for intelligence suggestions (💡 pattern)
				for (const block of content) {
					if (
						block.type === "text" &&
						typeof block.text === "string" &&
						(block.text.includes("💡") ||
							block.text.includes("Intelligenz-Vorschlag"))
					) {
						hadIntelligenceSuggestion = true;
					}
				}

				const toolUseBlocks = content.filter((b: any) => b.type === "tool_use");
				toolCalls += toolUseBlocks.length;

				// v4: Check tool_use blocks for system self-improvement writes
				for (const block of toolUseBlocks) {
					if (
						(block.name === "Write" || block.name === "Edit") &&
						block.input &&
						typeof block.input.file_path === "string"
					) {
						const fp = block.input.file_path;
						if (significantPaths.test(fp)) {
							metaIntelligenceLevel = 2; // significant: new rule/hook/agent
						} else if (minorPaths.test(fp) && metaIntelligenceLevel < 1) {
							metaIntelligenceLevel = 1; // minor: memory/docs update
						}
					}
				}
			}

			if (entryType === "tool_result" && msg) {
				const output =
					typeof msg.content === "string"
						? msg.content
						: JSON.stringify(msg.content);
				// v3: Only count genuine errors, not mentions of "error" in code/comments
				if (errorPattern.test(output) && !errorExclusions.test(output)) {
					errors++;
				}
			}
		} catch {
			// Skip malformed lines
		}
	}

	// Calculate composite quality score (1-10)
	let score = 8.0;

	if (toolCalls > 0) {
		const errorRate = errors / toolCalls;
		if (errorRate < 0.02) {
			/* no penalty */
		} else if (errorRate < 0.05) score -= 0.5;
		else if (errorRate < 0.15) score -= 1.0;
		else if (errorRate < 0.3) score -= 1.5;
		else score -= 2.0;
	}

	if (totalTurns > 0) {
		const correctionRate = corrections / totalTurns;
		if (correctionRate === 0 && totalTurns >= 10) score += 0.5;
		else if (correctionRate === 0) {
			/* short session */
		} else if (correctionRate < 0.03) score -= 0.5;
		else if (correctionRate < 0.07) score -= 1.5;
		else if (correctionRate < 0.12) score -= 2.5;
		else if (correctionRate < 0.2) score -= 3.5;
		else score -= 5.0;
	}

	if (toolCalls > 50) score += 0.3;
	else if (toolCalls > 20) score += 0.15;

	if (totalTurns > 0) {
		const ratio = toolCalls / totalTurns;
		if (ratio >= 1.0) score += 0.2;
		else if (ratio < 0.5) score -= 0.2;
	}

	score = Math.max(1.0, Math.min(10.0, Math.round(score * 10) / 10));

	const sessionId =
		path.split(/[/\\]/).pop()?.replace(".jsonl", "") || "unknown";

	return {
		date: new Date().toISOString(),
		session_id: sessionId,
		total_turns: totalTurns,
		tool_calls: toolCalls,
		errors,
		corrections,
		quality_score: score,
		iq_score: 0, // Filled by /self-improve, not by the scorer
		meta_intelligence_score: metaIntelligenceLevel,
		had_intelligence_suggestion: hadIntelligenceSuggestion,
	};
}

// v4.1: Direct whiteboard insertion (same approach as session-autopsy.ts)
// Sentinel files DON'T work at SessionEnd because writeback-enforcer only runs on SubagentStop.
// Writing directly via whiteboard-insert.sh/ps1 ensures the entry lands immediately.
function insertViaWhiteboardInsert(section: string, entry: string): void {
	const hookDir = join(homedir(), ".claude", "hooks");
	if (!existsSync(hookDir)) return;

	if (process.platform === "win32") {
		const script = join(hookDir, "whiteboard-insert.ps1");
		if (!existsSync(script)) return;
		const escapedSection = section.replace(/'/g, "''");
		const escapedEntry = entry.replace(/'/g, "''");
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
		} catch {
			// Non-fatal — stderr warning is sufficient
		}
	} else {
		const script = join(hookDir, "whiteboard-insert.sh");
		if (!existsSync(script)) return;
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
		} catch {
			// Non-fatal — stderr warning is sufficient
		}
	}
}

// v4.1: Intelligence suggestion enforcement — warn if session had >10 turns but no 💡 suggestion
// Fixed: Now writes directly to whiteboard instead of sentinel file (sentinel files are never
// picked up at SessionEnd because writeback-enforcer only runs on SubagentStop events)
function checkIntelligenceSuggestion(metrics: SessionMetrics): void {
	if (metrics.total_turns >= 10 && !metrics.had_intelligence_suggestion) {
		console.error(
			`INTELLIGENCE WARNING: Session had ${metrics.total_turns} turns but no intelligence suggestion (💡). ` +
				`The Superintelligence directive requires proactive suggestions in every substantial session.`,
		);

		// Write directly to whiteboard (not sentinel file — those don't work at SessionEnd)
		const date = new Date().toISOString().split("T")[0];
		insertViaWhiteboardInsert(
			"Meta-Intelligenz & Selbstverbesserung",
			`- **[${date}] intelligence-checker**: [WARNING] Session ${metrics.session_id.slice(0, 8)} (${metrics.total_turns} Turns) hatte keinen Intelligenz-Vorschlag`,
		);
	}

	// v4: Log meta-intelligence achievements
	if (metrics.meta_intelligence_score >= 2) {
		console.error(
			`META-INTELLIGENCE: Session improved the system itself (score: ${metrics.meta_intelligence_score}) — ` +
				`rules/hooks/agents were modified. This is exponential growth in action.`,
		);
	}
}

// v3: Trend detection writes to stderr (hook log) instead of MEMORY.md.
// The evolution-analyst agent reads session-scores.jsonl for trend analysis.
function detectTrends(currentMetrics: SessionMetrics): void {
	if (!existsSync(SCORES_FILE)) return;

	const allScores = readFileSync(SCORES_FILE, "utf-8")
		.trim()
		.split("\n")
		.map((l) => {
			try {
				return JSON.parse(l) as SessionMetrics;
			} catch {
				return null;
			}
		})
		.filter((s): s is SessionMetrics => s !== null && s.total_turns > 0)
		.filter((s) => s.total_turns >= 5); // v3: lowered from 10 to 5

	if (allScores.length < 5) return;

	if (allScores.length >= 10) {
		const recent = allScores.slice(-5);
		const avg = recent.reduce((s, m) => s + m.quality_score, 0) / recent.length;
		const prevAvg =
			allScores.slice(-10, -5).reduce((s, m) => s + m.quality_score, 0) / 5;
		if (avg < prevAvg - 0.5) {
			// v3: Write to stderr (shows in hook log) instead of appendFileSync to MEMORY.md
			console.error(
				`TREND WARNING: Quality declining: ${prevAvg.toFixed(1)} → ${avg.toFixed(1)}`,
			);
		}
	}

	if (allScores.length >= 20) {
		const scores = allScores.map((s) => s.quality_score);
		if (scores.length < 2) return;
		const mean = scores.reduce((a, b) => a + b, 0) / scores.length;
		const stdDev = Math.sqrt(
			scores.reduce((sum, s) => sum + (s - mean) ** 2, 0) / (scores.length - 1),
		);
		const ucl = mean + 3 * stdDev;
		const lcl = mean - 3 * stdDev;
		const current = currentMetrics.quality_score;

		if (current > ucl || current < lcl) {
			console.error(
				`SPC SIGNAL: Score ${current} outside limits [${lcl.toFixed(1)}, ${ucl.toFixed(1)}]`,
			);
		}

		const last7 = allScores.slice(-7).map((s) => s.quality_score);
		if (last7.length === 7 && last7.every((s) => s < mean)) {
			console.error(
				`SPC SIGNAL: 7 consecutive sessions below mean (${mean.toFixed(1)})`,
			);
		}
	}
}

function validateMetrics(
	metrics: SessionMetrics,
	transcriptPath: string,
): boolean {
	try {
		const content = readFileSync(transcriptPath, "utf-8");
		const lineCount = content.trim().split("\n").length;
		if (metrics.total_turns === 0 && lineCount > 50) {
			// v3: Log to stderr instead of appendFileSync to MEMORY.md
			console.error(
				`SCORER WARNING: 0 turns parsed from ${lineCount}-line transcript (${metrics.session_id}). Skipping.`,
			);
			return false;
		}
	} catch {
		/* proceed */
	}
	return true;
}

function main(): void {
	const transcript = findLatestTranscript();
	if (!transcript) {
		process.exit(0);
	}

	try {
		const metrics = analyzeTranscript(transcript);
		if (!validateMetrics(metrics, transcript!)) {
			process.exit(0);
		}
		// M1/B4: Deduplication — only write if session_id changed or turns increased by >5
		if (existsSync(SCORES_FILE)) {
			const lines = readFileSync(SCORES_FILE, "utf-8").trim().split("\n");
			const lastLine = lines[lines.length - 1];
			if (lastLine) {
				try {
					const last = JSON.parse(lastLine);
					if (
						last.session_id === metrics.session_id &&
						Math.abs(metrics.total_turns - last.total_turns) <= 5
					) {
						// Same session, turns barely changed — skip duplicate write
						process.exit(0);
					}
				} catch {
					/* parse error — write anyway */
				}
			}
		}
		appendFileSync(SCORES_FILE, JSON.stringify(metrics) + "\n", "utf-8");
		checkIntelligenceSuggestion(metrics);
		detectTrends(metrics);
	} catch {
		process.exit(0);
	}
}

main();
