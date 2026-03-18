#!/usr/bin/env bun
/**
 * Session Scorer — Meta-Evolution Artifact #1
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

import { readFileSync, appendFileSync, readdirSync, statSync, existsSync } from "fs";
import { join } from "path";

const HOME = process.env.HOME || "/Users/frank";
const SCORES_FILE = join(HOME, ".claude", "session-scores.jsonl");
const PROJECTS_DIR = join(HOME, ".claude", "projects", "-Users-frank");

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
    /\bnein\b/i, /\bfalsch\b/i, /\bnicht das\b/i, /\bstop\b/i,
    /\brückgängig\b/i, /\bundo\b/i, /\bwrong\b/i, /\bthat's not\b/i,
    /\bdon't do\b/i, /\bmach das nicht\b/i, /\bso nicht\b/i,
  ];

  for (const line of lines) {
    try {
      const entry = JSON.parse(line);

      // Claude Code transcript format: { type: "user"|"assistant"|"tool_result", message: {...} }
      const entryType = entry.type;
      const msg = entry.message;

      if (entryType === "user" && msg) {
        totalTurns++;
        const text = typeof msg.content === "string"
          ? msg.content
          : JSON.stringify(msg.content);
        for (const pattern of correctionPatterns) {
          if (pattern.test(text)) {
            corrections++;
            break;
          }
        }
      }

      if (entryType === "assistant" && msg) {
        // Tool calls are content blocks with type "tool_use"
        const content = Array.isArray(msg.content) ? msg.content : [];
        const toolUseBlocks = content.filter((b: any) => b.type === "tool_use");
        toolCalls += toolUseBlocks.length;
      }

      if (entryType === "tool_result" && msg) {
        const output = typeof msg.content === "string"
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
  let score = 7.0;

  // Error penalty: -0.5 per error, max -2
  score -= Math.min(errors * 0.5, 2.0);

  // Correction penalty: -0.8 per correction, max -2
  score -= Math.min(corrections * 0.8, 2.0);

  // Tool efficiency: fewer errors per tool call is better
  if (toolCalls > 0) {
    const errorRate = errors / toolCalls;
    if (errorRate < 0.05) score += 0.5;
    else if (errorRate > 0.3) score -= 0.5;
  }

  // Engagement bonus: productive sessions have more tool calls
  if (toolCalls > 20) score += 0.3;
  if (totalTurns > 0 && toolCalls / totalTurns > 5) score += 0.2;

  score = Math.max(1.0, Math.min(10.0, Math.round(score * 10) / 10));

  const sessionId = path.split("/").pop()?.replace(".jsonl", "") || "unknown";

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
      try { return JSON.parse(l) as SessionMetrics; } catch { return null; }
    })
    .filter(Boolean) as SessionMetrics[];

  if (allScores.length < 5) return;

  const recent = allScores.slice(-5);
  const avg = recent.reduce((s, m) => s + m.quality_score, 0) / recent.length;

  if (allScores.length >= 10) {
    const prevAvg = allScores.slice(-10, -5).reduce((s, m) => s + m.quality_score, 0) / 5;

    if (avg < prevAvg - 0.5) {
      const warning = `\n- **${currentMetrics.date.split("T")[0]}**: Quality declining: ${prevAvg.toFixed(1)} → ${avg.toFixed(1)} (last 5 sessions)\n`;
      const sharedMemory = join(HOME, ".claude", "agent-memory", "shared", "MEMORY.md");
      if (existsSync(sharedMemory)) {
        appendFileSync(sharedMemory, warning, "utf-8");
      }
    }
  }
}

function main() {
  const transcript = findLatestTranscript();
  if (!transcript) {
    process.exit(0);
  }

  try {
    const metrics = analyzeTranscript(transcript);
    appendFileSync(SCORES_FILE, JSON.stringify(metrics) + "\n", "utf-8");
    detectTrends(metrics);
  } catch {
    // Fail silently — never block session exit
    process.exit(0);
  }
}

main();
