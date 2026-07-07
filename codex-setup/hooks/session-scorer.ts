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
	mkdirSync,
} from "fs";
import { join } from "path";

const HOME = process.env.USERPROFILE || process.env.HOME || "";
if (!HOME) process.exit(0);
const SCORES_FILE = join(HOME, ".codex", "session-scores.jsonl");

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
	intelligence_suggestion_count: number; // v5: count of 💡 suggestions (self-observation metric)
	had_self_observation: boolean; // v5: true if session showed self-observation behavior
}

function findLatestTranscript(): string | null {
	function findLatestJsonl(rootDir: string): string | null {
		if (!existsSync(rootDir)) return null;
		let latest = "";
		let latestTime = 0;

		function visit(dir: string): void {
			for (const entry of readdirSync(dir)) {
				const fullPath = join(dir, entry);
				let stats;
				try {
					stats = statSync(fullPath);
				} catch {
					continue;
				}

				if (stats.isDirectory()) {
					visit(fullPath);
					continue;
				}

				if (!stats.isFile()) continue;
				if (!entry.endsWith(".jsonl")) continue;
				if (entry.startsWith("agent-")) continue;

				if (stats.mtimeMs > latestTime) {
					latestTime = stats.mtimeMs;
					latest = fullPath;
				}
			}
		}

		try {
			visit(rootDir);
		} catch {
			return null;
		}

		return latest || null;
	}

	const sessionsRoot = join(HOME, ".codex", "sessions");
	const latestSessionTranscript = findLatestJsonl(sessionsRoot);
	if (latestSessionTranscript) {
		return latestSessionTranscript;
	}

	const projectsRoot = join(HOME, ".codex", "projects");
	return findLatestJsonl(projectsRoot);
}

function analyzeTranscript(path: string): SessionMetrics {
	const content = readFileSync(path, "utf-8");
	const lines = content.trim().split("\n");

	let totalTurns = 0;
	let toolCalls = 0;
	let errors = 0;
	let corrections = 0;
	let hadIntelligenceSuggestion = false;
	let intelligenceSuggestionCount = 0;
	let hadSelfObservation = false;

	// v5: Self-observation patterns — detect when Codex reflected on its own work process
	const selfObservationPatterns = [
		/Selbstbeobachtung/i,
		/self.?observation/i,
		/bei der Arbeit (aufgefallen|beobachtet|bemerkt)/i,
		/Intelligenz-Vorschlag \d/i, // numbered suggestions indicate structured self-review
		/Umweg|Umwege/i,
		/hätte schneller/i,
		/haette schneller/i,
		/resistent fix/i,
	];

	// v4: Meta-intelligence tracking — did this session improve the system itself?
	// 0 = no system changes, 1 = minor (memory/AGENTS.md), 2 = significant (rules/hooks/agents)
	let metaIntelligenceLevel = 0;
	// Paths that indicate system self-improvement — restricted to .codex/ paths
	// to avoid false positives from project files (e.g. game-rules.md, git-hooks/)
	const significantPaths = /[\\/]\\.codex[\\/](rules|hooks|agents)[\\/]/i;
	const minorPaths =
		/[\\/]\\.codex[\\/].*(memory|MEMORY\.md|agent-memory)|[\\/]CODEX\.md$/i;

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

	function maybeString(value: unknown): string {
		return typeof value === "string" ? value : "";
	}

	function collectText(contentBlocks: unknown): string {
		if (!Array.isArray(contentBlocks)) return "";
		const parts: string[] = [];

		for (const block of contentBlocks) {
			if (!block || typeof block !== "object") continue;
			const typedBlock = block as Record<string, unknown>;
			const blockType = maybeString(typedBlock.type);

			if (
				(blockType === "text" ||
					blockType === "input_text" ||
					blockType === "output_text") &&
				typeof typedBlock.text === "string"
			) {
				parts.push(typedBlock.text);
			}
		}

		return parts.join("\n");
	}

	function inspectPathCandidate(candidate: unknown): void {
		if (typeof candidate !== "string") return;
		if (significantPaths.test(candidate)) {
			metaIntelligenceLevel = 2;
			return;
		}
		if (minorPaths.test(candidate) && metaIntelligenceLevel < 1) {
			metaIntelligenceLevel = 1;
		}
	}

	function inspectToolUsePath(input: unknown): void {
		if (!input || typeof input !== "object") return;
		const typedInput = input as Record<string, unknown>;
		inspectPathCandidate(typedInput.file_path);
		inspectPathCandidate(typedInput.path);
	}

	function inspectApplyPatch(argumentsText: string): void {
		for (const match of argumentsText.matchAll(
			/\*\*\* (?:Add|Update|Delete) File: (.+)/g,
		)) {
			inspectPathCandidate(match[1]?.trim());
		}
	}

	function inspectFunctionCall(name: unknown, rawArguments: unknown): void {
		const functionName = maybeString(name);
		if (!functionName) return;

		if (functionName === "apply_patch") {
			inspectApplyPatch(maybeString(rawArguments));
			return;
		}

		let parsedArguments: unknown = rawArguments;
		if (typeof rawArguments === "string") {
			try {
				parsedArguments = JSON.parse(rawArguments);
			} catch {
				parsedArguments = rawArguments;
			}
		}

		if (!parsedArguments || typeof parsedArguments !== "object") return;
		const typedArguments = parsedArguments as Record<string, unknown>;
		inspectPathCandidate(typedArguments.path);
		inspectPathCandidate(typedArguments.file_path);
		inspectPathCandidate(typedArguments.workdir);
		inspectPathCandidate(typedArguments.command);
	}

	function inspectAssistantText(text: string): void {
		if (!text) return;

		const bulbMatches = text.match(/💡/g);
		if (bulbMatches) {
			intelligenceSuggestionCount += bulbMatches.length;
			hadIntelligenceSuggestion = true;
		}
		if (!hadIntelligenceSuggestion && text.includes("Intelligenz-Vorschlag")) {
			hadIntelligenceSuggestion = true;
			const numberedMatches = text.match(/Intelligenz-Vorschlag\s*\d/g);
			if (numberedMatches) {
				intelligenceSuggestionCount = Math.max(
					intelligenceSuggestionCount,
					numberedMatches.length,
				);
			}
		}
		if (!hadSelfObservation) {
			for (const pattern of selfObservationPatterns) {
				if (pattern.test(text)) {
					hadSelfObservation = true;
					break;
				}
			}
		}
	}

	function inspectUserText(text: string): void {
		if (!text) return;
		totalTurns++;
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

	function inspectToolOutput(output: string): void {
		if (errorPattern.test(output) && !errorExclusions.test(output)) {
			errors++;
		}
	}

	for (const line of lines) {
		try {
			const entry = JSON.parse(line);
			const entryType = entry.type;
			const msg = entry.message;

			if (entryType === "user" && msg) {
				const text =
					typeof msg.content === "string"
						? msg.content
						: JSON.stringify(msg.content);
				inspectUserText(text);
			}

			if (entryType === "assistant" && msg) {
				const content = Array.isArray(msg.content) ? msg.content : [];

				// v4: Check assistant text for intelligence suggestions (💡 pattern)
				for (const block of content) {
					if (block.type === "text" && typeof block.text === "string") {
						inspectAssistantText(block.text);
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
						inspectToolUsePath(block.input);
					}
				}
			}

			if (entryType === "tool_result" && msg) {
				const output =
					typeof msg.content === "string"
						? msg.content
						: JSON.stringify(msg.content);
				inspectToolOutput(output);
			}

			if (entryType === "response_item" && entry.payload) {
				const payload = entry.payload as Record<string, unknown>;
				const payloadType = maybeString(payload.type);

				if (payloadType === "message") {
					const role = maybeString(payload.role);
					const text = collectText(payload.content);
					if (role === "user") {
						inspectUserText(text);
					} else if (role === "assistant") {
						inspectAssistantText(text);
					}
				}

				if (payloadType === "function_call") {
					toolCalls++;
					inspectFunctionCall(payload.name, payload.arguments);
				}

				if (payloadType === "function_call_output") {
					inspectToolOutput(maybeString(payload.output));
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

	// C2: IQ-Score — recalibrated to 0-100 additive scale (2026-03-31)
	// Old formula was multiplicative (quality * efficiency * bonus) which caused 0-18 volatile scores.
	// New: 5 weighted dimensions: quality(0-40) + efficiency(0-20) + intelligence(0-30)
	//   + self-observation(0-5) + meta-intelligence(0-5) = 0-100 total.
	// Additive = more stable: one low dimension doesn't zero-out the entire score.
	// NOTE: Old JSONL entries (before 2026-03-31) have iq_score 0-18. To compare
	// across scales, multiply old values by 5.56 or filter by date.
	const efficiency = Math.min(toolCalls / Math.max(totalTurns, 1), 1.0);
	const qualityDim = (score / 10) * 40; // 0-40: core quality (most important)
	const efficiencyDim = efficiency * 20; // 0-20: tool usage efficiency
	const intelDim = (hadIntelligenceSuggestion ? 15 : 0) + Math.min(intelligenceSuggestionCount, 3) * 5; // 0-30: intelligence suggestions
	const selfObsDim = hadSelfObservation ? 5 : 0; // 0-5: self-observation
	const metaDim = metaIntelligenceLevel * 2.5; // 0-5: meta-intelligence (system improvements)
	const rawIq = qualityDim + efficiencyDim + intelDim + selfObsDim + metaDim;
	const iqScore = Math.round(Math.min(rawIq, 100) * 10) / 10;

	return {
		date: new Date().toISOString(),
		session_id: sessionId,
		total_turns: totalTurns,
		tool_calls: toolCalls,
		errors,
		corrections,
		quality_score: score,
		iq_score: iqScore,
		meta_intelligence_score: metaIntelligenceLevel,
		had_intelligence_suggestion: hadIntelligenceSuggestion,
		intelligence_suggestion_count: intelligenceSuggestionCount,
		had_self_observation: hadSelfObservation,
	};
}

// v5: Intelligence suggestion + self-observation enforcement
// Checks both directives: #1 Superintelligence (suggestions) and #2 Self-Observation
function checkIntelligenceSuggestion(metrics: SessionMetrics): void {
	const sid = metrics.session_id.slice(0, 8);

	// Check #1: Intelligence suggestions (Superintelligence directive)
	if (metrics.total_turns >= 10 && !metrics.had_intelligence_suggestion) {
		console.error(
			`INTELLIGENCE WARNING: Session ${sid} had ${metrics.total_turns} turns but no intelligence suggestion (💡). ` +
				`The Superintelligence directive requires proactive suggestions in every substantial session.`,
		);
	}

	// Check #2: Self-observation (second directive) — sessions with >15 turns should show self-observation
	if (metrics.total_turns >= 15 && !metrics.had_self_observation) {
		console.error(
			`SELF-OBSERVATION WARNING: Session ${sid} had ${metrics.total_turns} turns but no self-observation detected. ` +
				`The #2 directive requires continuous self-observation during work.`,
		);
	}

	// Log positive metrics
	if (metrics.intelligence_suggestion_count >= 3) {
		console.error(
			`EXCELLENT: Session ${sid} produced ${metrics.intelligence_suggestion_count} intelligence suggestions — compound effect in action.`,
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
		mkdirSync(join(HOME, ".codex"), { recursive: true });
		// M1/B4: Deduplication — only write if session_id changed or turns increased by >5
		if (existsSync(SCORES_FILE)) {
			const rawScores = readFileSync(SCORES_FILE, "utf-8").trim();
			const lines = rawScores ? rawScores.split("\n") : [];
			const lastLine = lines.length > 0 ? lines[lines.length - 1] : "";
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
