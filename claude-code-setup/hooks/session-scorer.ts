#!/usr/bin/env bun
/**
 * Session Scorer — Meta-Evolution Artifact #1 (Windows Version)
 *
 * Runs at SessionEnd to analyze the current session transcript
 * and extract quality metrics into a JSONL log file.
 *
 * Metrics tracked:
 * - total_turns: Number of user messages
 * - tool_calls: Number of tool invocations
 * - errors: Number of failed tool calls
 * - corrections: User corrections detected
 * - quality_score: Composite 1-10 score
 */

import {
	readFileSync,
	appendFileSync,
	readdirSync,
	statSync,
	existsSync,
} from "fs";
import { join } from "path";

const HOME = process.env.USERPROFILE || process.env.HOME || "C:\\Users\\barwa";
const SCORES_FILE = join(HOME, ".claude", "session-scores.jsonl");

// Windows project directory path
function findProjectsDir(): string {
	const claudeDir = join(HOME, ".claude", "projects");
	if (!existsSync(claudeDir)) return "";
	try {
		const entries = readdirSync(claudeDir);
		// Find the project dir matching C--Users-barwa or similar
		for (const entry of entries) {
			const fullPath = join(claudeDir, entry);
			if (statSync(fullPath).isDirectory() && entry.startsWith("C--")) {
				return fullPath;
			}
		}
		// Fallback: use first directory
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
}

function findLatestTranscript(): string | null {
	if (!PROJECTS_DIR) return null;
	try {
		const entries = readdirSync(PROJECTS_DIR);
		let latest = "";
		let latestTime = 0;

		for (const entry of entries) {
			if (!entry.endsWith(".jsonl")) continue;
			// Skip agent transcripts — only score main sessions (UUID format)
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

	const correctionPatterns = [
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
				// v2: Context-aware correction detection
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
				const toolUseBlocks = content.filter((b: any) => b.type === "tool_use");
				toolCalls += toolUseBlocks.length;
			}

			if (entryType === "tool_result" && msg) {
				const output =
					typeof msg.content === "string"
						? msg.content
						: JSON.stringify(msg.content);
				if (/error|failed|exception|ENOENT|EPERM/i.test(output)) {
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

	// Parallelization ratio scoring (calibrated against real data: baseline 0.85, max 1.27)
	if (totalTurns > 0) {
		const ratio = toolCalls / totalTurns;
		if (ratio >= 1.0)
			score += 0.2; // Bonus: above-average parallelization
		else if (ratio < 0.5) score -= 0.2; // Malus: very low tool usage
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
	};
}

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
		.filter((s) => s.total_turns >= 10);

	if (allScores.length < 5) return;

	const sharedMemory = join(
		HOME,
		".claude",
		"agent-memory",
		"shared",
		"MEMORY.md",
	);

	if (allScores.length < 20) {
		const recent = allScores.slice(-5);
		const avg = recent.reduce((s, m) => s + m.quality_score, 0) / recent.length;
		if (allScores.length >= 10) {
			const prevAvg =
				allScores.slice(-10, -5).reduce((s, m) => s + m.quality_score, 0) / 5;
			if (avg < prevAvg - 0.5) {
				const warning = `\n- **${currentMetrics.date.split("T")[0]}**: Quality declining: ${prevAvg.toFixed(1)} → ${avg.toFixed(1)} (simple trend)\n`;
				if (existsSync(sharedMemory)) {
					appendFileSync(sharedMemory, warning, "utf-8");
				}
			}
		}
		return;
	}

	// Phase 2: SPC (>= 20 sessions)
	const scores = allScores.map((s) => s.quality_score);
	const mean = scores.reduce((a, b) => a + b, 0) / scores.length;
	const stdDev = Math.sqrt(
		scores.reduce((sum, s) => sum + (s - mean) ** 2, 0) / (scores.length - 1),
	);
	const ucl = mean + 3 * stdDev;
	const lcl = mean - 3 * stdDev;
	const current = currentMetrics.quality_score;
	const warnings: string[] = [];

	if (current > ucl || current < lcl) {
		warnings.push(
			`SPC SIGNAL: Score ${current} outside limits [${lcl.toFixed(1)}, ${ucl.toFixed(1)}]`,
		);
	}

	const last7 = allScores.slice(-7).map((s) => s.quality_score);
	if (last7.length === 7 && last7.every((s) => s < mean)) {
		warnings.push(
			`SPC SIGNAL: 7 consecutive sessions below mean (${mean.toFixed(1)})`,
		);
	}

	if (warnings.length > 0 && existsSync(sharedMemory)) {
		const entry = `\n- **${currentMetrics.date.split("T")[0]}**: ${warnings.join("; ")} [SPC: μ=${mean.toFixed(1)}, σ=${stdDev.toFixed(2)}, N=${scores.length}]\n`;
		appendFileSync(sharedMemory, entry, "utf-8");
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
			const warning = `\n### [${new Date().toISOString().split("T")[0]}] SCORER WARNING: 0 turns parsed from ${lineCount}-line transcript\n- **Session**: ${metrics.session_id}\n- **Action**: Skipped writing dummy score.\n`;
			const failuresPath = join(
				HOME,
				".claude",
				"agent-memory",
				"shared",
				"FAILURES.md",
			);
			if (existsSync(failuresPath)) {
				appendFileSync(failuresPath, warning, "utf-8");
			}
			return false;
		}
	} catch {
		/* proceed */
	}
	return true;
}

function main() {
	const transcript = findLatestTranscript();
	if (!transcript) {
		process.exit(0);
	}

	try {
		const metrics = analyzeTranscript(transcript);
		if (!validateMetrics(metrics, transcript!)) {
			process.exit(0);
		}
		appendFileSync(SCORES_FILE, JSON.stringify(metrics) + "\n", "utf-8");
		detectTrends(metrics);
	} catch {
		process.exit(0);
	}
}

main();
