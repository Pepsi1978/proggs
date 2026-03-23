#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const REPO_ROOT = path.resolve(__dirname, "..", "..");
const DEFAULT_STATE_PATH = path.join(
  REPO_ROOT,
  "codex-setup",
  "state",
  "claude-delta-state.json",
);
const TRACKED_PATHS = ["CLAUDE.md", "claude-code-setup"];
const REPLACE_SUBJECT_RE = /\b(rewrite|replace|remove|delete|drop|rename|overhaul)\b/i;
const BUGFIX_SUBJECT_RE =
  /\b(fix|bug|regression|resilien|harden|hardening|guard|watchdog|repair|safety|stability|parity|healthcheck|health-check|timeout|fallback|validator|validate)\b/i;
const BUGFIX_PATH_RE =
  /(resilient-bugfixing|guard|watchdog|health|parity|safety|failure|autopsy|scorer|fix|repair)/i;

const DEFAULT_STATE = {
  version: 1,
  scope: "claude-environment-only",
  tracked_paths: ["CLAUDE.md", "claude-code-setup/**"],
  replace_requires_confirmation: true,
  additive_bias: true,
  last_reviewed_commit: null,
  last_applied_commit: null,
  last_reviewed_at: null,
  last_applied_at: null,
  notes:
    "Tracks only Claude/Cloud Code programming-environment deltas for Codex.",
};

function parseArgs(argv) {
  const args = { _: [], json: false, all: false, limit: 50 };
  for (let i = 0; i < argv.length; i += 1) {
    const part = argv[i];
    if (!part.startsWith("--")) {
      args._.push(part);
      continue;
    }
    const key = part.slice(2);
    if (key === "json" || key === "all") {
      args[key] = true;
      continue;
    }
    const next = argv[i + 1];
    if (!next || next.startsWith("--")) {
      throw new Error(`Missing value for --${key}`);
    }
    args[key] = next;
    i += 1;
  }
  if (args.limit !== undefined) {
    const parsed = Number.parseInt(`${args.limit}`, 10);
    args.limit = Number.isFinite(parsed) && parsed > 0 ? parsed : 50;
  }
  return args;
}

function runGit(args, options = {}) {
  try {
    return execFileSync("git", args, {
      cwd: REPO_ROOT,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
      ...options,
    }).trimEnd();
  } catch (error) {
    if (options.allowFailure) {
      return null;
    }
    const stderr = error.stderr?.toString().trim();
    throw new Error(stderr || error.message);
  }
}

function toSlash(targetPath) {
  return targetPath.replace(/\\/g, "/");
}

function readJson(filePath, fallback) {
  if (!fs.existsSync(filePath)) {
    return fallback;
  }
  return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function writeJson(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, "utf8");
}

function isValidCommit(ref) {
  if (!ref) return false;
  return Boolean(runGit(["rev-parse", "--verify", ref], { allowFailure: true }));
}

function getLatestRelevantCommit() {
  return runGit(["log", "-1", "--format=%H", "--", ...TRACKED_PATHS], {
    allowFailure: true,
  });
}

function isPortableSourcePath(sourcePath) {
  if (sourcePath === "CLAUDE.md") return true;
  if (!sourcePath.startsWith("claude-code-setup/")) return false;

  return (
    /^claude-code-setup\/rules\/.+\.md$/u.test(sourcePath) ||
    /^claude-code-setup\/agents\/.+\.md$/u.test(sourcePath) ||
    /^claude-code-setup\/commands\/.+/u.test(sourcePath) ||
    /^claude-code-setup\/hooks\/.+/u.test(sourcePath) ||
    /^claude-code-setup\/skills\/.+/u.test(sourcePath) ||
    /^claude-code-setup\/scripts\/.+/u.test(sourcePath) ||
    /^claude-code-setup\/mcp-.+\.json$/u.test(sourcePath) ||
    /^claude-code-setup\/hooks-.+\.json$/u.test(sourcePath) ||
    /^claude-code-setup\/settings.*\.json$/u.test(sourcePath) ||
    sourcePath === "claude-code-setup/manifest.json" ||
    sourcePath === "claude-code-setup/README.md" ||
    sourcePath === "claude-code-setup/SETUP-SEMANTIC-SEARCH.md"
  );
}

function classifyPath(sourcePath) {
  if (sourcePath === "CLAUDE.md") {
    return {
      category: "root-rules",
      target_hints: ["AGENTS.md", "codex-setup/rules/global.md"],
    };
  }

  const filename = path.posix.basename(sourcePath);
  const basename = filename.replace(/\.[^.]+$/u, "");

  if (/^claude-code-setup\/rules\/.+\.md$/u.test(sourcePath)) {
    const mappedName =
      basename === "german-skill-triggers"
        ? "german-trigger-routing"
        : basename === "superintelligence"
          ? "global"
          : basename;
    return {
      category: "rule",
      target_hints: [`codex-setup/rules/${mappedName}.md`],
    };
  }

  if (/^claude-code-setup\/agents\/.+\.md$/u.test(sourcePath)) {
    return {
      category: "agent",
      target_hints: [
        `codex-setup/skills/self-improve/references/agents/${filename}`,
      ],
    };
  }

  if (/^claude-code-setup\/commands\/self-improve/u.test(sourcePath)) {
    return {
      category: "self-improve",
      target_hints: [
        "codex-setup/skills/self-improve/SKILL.md",
        "codex-setup/skills/self-improve/references/",
      ],
    };
  }

  if (/^claude-code-setup\/commands\//u.test(sourcePath)) {
    return {
      category: "command",
      target_hints: [
        "codex-setup/skills/self-improve/references/",
        "codex-setup/rules/german-trigger-routing.md",
      ],
    };
  }

  if (/^claude-code-setup\/hooks\//u.test(sourcePath)) {
    return {
      category: "hook",
      target_hints: ["codex-setup/scripts/", "AGENTS.md"],
    };
  }

  if (/^claude-code-setup\/skills\//u.test(sourcePath)) {
    return {
      category: "skill",
      target_hints: ["codex-setup/skills/self-improve/", "AGENTS.md"],
    };
  }

  if (/^claude-code-setup\/scripts\//u.test(sourcePath)) {
    return {
      category: "script",
      target_hints: ["codex-setup/scripts/", "codex-setup/skills/self-improve/"],
    };
  }

  if (
    /^claude-code-setup\/settings.*\.json$/u.test(sourcePath) ||
    /^claude-code-setup\/hooks-.+\.json$/u.test(sourcePath) ||
    /^claude-code-setup\/mcp-.+\.json$/u.test(sourcePath) ||
    sourcePath === "claude-code-setup/manifest.json"
  ) {
    return {
      category: "environment-config",
      target_hints: [
        "C:/Users/barwa/.codex/config.toml",
        "AGENTS.md",
        "codex-setup/README.md",
      ],
    };
  }

  return {
    category: "setup-doc",
    target_hints: ["codex-setup/README.md", "codex-setup/skills/self-improve/"],
  };
}

function parseNameStatusLine(line) {
  const parts = line.split("\t");
  const status = parts[0] || "";
  const code = status[0] || "";
  if (code === "R" || code === "C") {
    return {
      status: code,
      oldPath: toSlash(parts[1] || ""),
      path: toSlash(parts[2] || ""),
    };
  }
  return {
    status: code || status,
    oldPath: null,
    path: toSlash(parts[1] || ""),
  };
}

function parseCommits(rawLog) {
  if (!rawLog) return [];
  return rawLog
    .split("\u001e")
    .map((chunk) => chunk.trim())
    .filter(Boolean)
    .map((chunk) => {
      const lines = chunk.split(/\r?\n/u).filter(Boolean);
      const [sha = "", timestamp = "", subject = ""] = (lines.shift() || "").split(
        "\u001f",
      );
      const changes = lines.map(parseNameStatusLine).filter((entry) => entry.path);
      return { sha, timestamp, subject, changes };
    });
}

function pickRelevantChanges(commit) {
  return commit.changes
    .map((change) => {
      const effectivePath = change.path || change.oldPath;
      if (!isPortableSourcePath(effectivePath)) return null;
      return {
        ...change,
        path: effectivePath,
        classification: classifyPath(effectivePath),
      };
    })
    .filter(Boolean);
}

function unique(values) {
  return [...new Set(values)];
}

function targetExists(targetHint) {
  if (/^[A-Za-z]:\//u.test(targetHint)) {
    return fs.existsSync(targetHint);
  }
  return fs.existsSync(path.join(REPO_ROOT, targetHint));
}

function determineStrategy({ subject, relevantChanges, overlappingTargets }) {
  const hasDelete = relevantChanges.some((change) => change.status === "D");
  const hasRename = relevantChanges.some((change) => change.status === "R");
  const allAdded = relevantChanges.every((change) => change.status === "A");

  if (
    overlappingTargets.length > 0 &&
    (hasDelete || hasRename || REPLACE_SUBJECT_RE.test(subject))
  ) {
    return "REPLACE";
  }

  if (overlappingTargets.length === 0 && allAdded) {
    return "ADD";
  }

  if (overlappingTargets.length === 0) {
    return "ADD";
  }

  return "ADAPT";
}

function buildRecommendation(strategy) {
  if (strategy === "ADD") {
    return "Als additive Codex-Erweiterung portieren.";
  }
  if (strategy === "REPLACE") {
    return "Vor Ersetzung warnen und ausdrueckliche Freigabe einholen.";
  }
  return "Fuer Codex uebersetzen und additive Integration bevorzugen.";
}

function detectBugfixSignal({ subject, relevantChanges, categories }) {
  if (BUGFIX_SUBJECT_RE.test(subject)) {
    return true;
  }

  if (categories.includes("hook") || categories.includes("script")) {
    return relevantChanges.some((change) => BUGFIX_PATH_RE.test(change.path));
  }

  return relevantChanges.some((change) =>
    /resilient-bugfixing\.md$/i.test(change.path),
  );
}

function buildReason({ strategy, categories, overlappingTargets, bugfixSignal }) {
  if (strategy === "ADD") {
    const base =
      "Die Aenderung hat keinen direkten Codex-Zielkonflikt und wirkt additiv.";
    return bugfixSignal
      ? `${base} Zusaetzlich ist robuste Bugfix- oder Haertungslogik erkennbar.`
      : base;
  }
  if (strategy === "REPLACE") {
    const base = `Die Aenderung beruehrt bestehende Codex-Ziele (${overlappingTargets.join(", ")}).`;
    return bugfixSignal
      ? `${base} Gleichzeitig steckt darin bestehende Fixlogik, die vor einem Ersatz separat bewertet werden soll.`
      : base;
  }
  const base = `Die Aenderung betrifft vorhandene Codex-Bereiche (${categories.join(", ")}), braucht aber Uebersetzung statt blindes Spiegeln.`;
  return bugfixSignal
    ? `${base} Die Quelle enthaelt ausserdem uebernehmbare Bugfix- oder Haertungsmuster.`
    : base;
}

function buildCandidates(commits, state, limit) {
  const candidates = [];

  for (const commit of commits) {
    const relevantChanges = pickRelevantChanges(commit);
    if (relevantChanges.length === 0) continue;

    const categories = unique(
      relevantChanges.map((change) => change.classification.category),
    );
    const targetHints = unique(
      relevantChanges.flatMap((change) => change.classification.target_hints),
    );
    const overlappingTargets = targetHints.filter(targetExists);
    const bugfixSignal = detectBugfixSignal({
      subject: commit.subject,
      relevantChanges,
      categories,
    });
    const strategy = determineStrategy({
      subject: commit.subject,
      relevantChanges,
      overlappingTargets,
    });

    candidates.push({
      source_commit: commit.sha,
      source_timestamp: commit.timestamp,
      source_subject: commit.subject,
      source_paths: relevantChanges.map((change) => change.path),
      source_statuses: unique(relevantChanges.map((change) => change.status)).sort(),
      categories,
      target_hints: targetHints,
      overlapping_targets: overlappingTargets,
      strategy,
      approval_required:
        strategy === "REPLACE" && state.replace_requires_confirmation !== false,
      risk:
        strategy === "REPLACE" ? "high" : strategy === "ADAPT" ? "medium" : "low",
      bugfix_signal: bugfixSignal,
      bugfix_recommendation: bugfixSignal
        ? "Fixlogik und Regression-Schutz fuer Codex mitpruefen und nach Moeglichkeit uebernehmen."
        : "",
      reason: buildReason({ strategy, categories, overlappingTargets, bugfixSignal }),
      recommendation: buildRecommendation(strategy),
    });
  }

  return candidates.slice(0, limit);
}

function loadState(statePath) {
  const loaded = readJson(statePath, DEFAULT_STATE);
  return { ...DEFAULT_STATE, ...loaded };
}

function buildRange(baseCommit) {
  if (!baseCommit) return [];
  return [`${baseCommit}..HEAD`];
}

function scanDeltas(args, statePath) {
  const state = loadState(statePath);
  const latestRelevantCommit = getLatestRelevantCommit();
  const baseCommitRaw = args.since || (args.all ? null : state.last_reviewed_commit);
  const baseCommit = isValidCommit(baseCommitRaw) ? baseCommitRaw : null;
  const baseStatus = baseCommitRaw
    ? baseCommit
      ? "ok"
      : "missing"
    : "none";

  const rangeArgs = buildRange(baseCommit);
  const rawLog = runGit(
    [
      "log",
      "--reverse",
      "--name-status",
      "--find-renames",
      "--format=%x1e%H%x1f%cI%x1f%s",
      ...rangeArgs,
      "--",
      ...TRACKED_PATHS,
    ],
    { allowFailure: true },
  );
  const commits = parseCommits(rawLog || "");
  const candidates = buildCandidates(commits, state, args.limit);

  return {
    generated_at: new Date().toISOString(),
    repo_root: REPO_ROOT,
    scope: state.scope,
    tracked_paths: state.tracked_paths,
    state_path: statePath,
    base_commit: baseCommit,
    base_commit_status: baseStatus,
    latest_relevant_commit: latestRelevantCommit,
    change_count: candidates.length,
    approval_required_count: candidates.filter((item) => item.approval_required).length,
    candidates,
    state,
  };
}

function formatTextReport(result) {
  const lines = [
    "Claude Delta Audit",
    `Scope: ${result.tracked_paths.join(", ")}`,
    `State file: ${result.state_path}`,
    `Base commit: ${result.base_commit || "(none)"}`,
    `Latest relevant commit: ${result.latest_relevant_commit || "(none)"}`,
  ];

  if (result.base_commit_status === "missing") {
    lines.push(
      "Warnung: Der gespeicherte Basis-Commit ist nicht mehr gueltig. Der Audit hat deshalb die gesamte relevante Historie betrachtet.",
    );
  }

  if (result.candidates.length === 0) {
    lines.push("Keine neuen portablen Claude-Setup-Deltas seit dem Basisstand.");
    return `${lines.join("\n")}\n`;
  }

  lines.push(`Gefundene Deltas: ${result.candidates.length}`);

  const groups = [
    {
      code: "A",
      title: "Regeln und Prompts",
      match: (candidate) =>
        candidate.categories.some((category) =>
          ["root-rules", "rule"].includes(category),
        ),
    },
    {
      code: "B",
      title: "Agenten, Skills und Arbeitsprozesse",
      match: (candidate) =>
        candidate.categories.some((category) =>
          ["agent", "self-improve", "skill", "command"].includes(category),
        ),
    },
    {
      code: "C",
      title: "Skripte, Hooks und Validierung",
      match: (candidate) =>
        candidate.categories.some((category) =>
          ["hook", "script", "setup-doc"].includes(category),
        ),
    },
    {
      code: "D",
      title: "Runtime und Konfiguration",
      match: (candidate) =>
        candidate.categories.some((category) =>
          ["environment-config"].includes(category),
        ),
    },
  ];

  for (const group of groups) {
    const items = result.candidates.filter(group.match);
    if (items.length === 0) continue;
    lines.push("");
    lines.push(`${group.code}. ${group.title}`);
    items.forEach((candidate, index) => {
      const label = `${group.code}${index + 1}`;
      lines.push(`${label} [${candidate.strategy}] ${candidate.source_commit.slice(0, 7)} ${candidate.source_subject}`);
      lines.push(`  Kategorien: ${candidate.categories.join(", ")}`);
      lines.push(`  Quellen: ${candidate.source_paths.join(", ")}`);
      lines.push(`  Codex-Ziele: ${candidate.target_hints.join(", ")}`);
      lines.push(`  Bugfix-/Haertungssignal: ${candidate.bugfix_signal ? "ja" : "nein"}`);
      lines.push(`  Grund: ${candidate.reason}`);
      lines.push(`  Empfehlung: ${candidate.recommendation}`);
      if (candidate.bugfix_signal) {
        lines.push(`  Fixlogik: ${candidate.bugfix_recommendation}`);
      }
      if (candidate.approval_required) {
        lines.push(
          "  Warnung: Diese Aenderung wuerde bestehende Codex-Logik beruehren. Vor Ersetzung erst bestaetigen lassen.",
        );
      }
    });
  }

  return `${lines.join("\n")}\n`;
}

function markState(args, statePath, fieldPrefix) {
  const state = loadState(statePath);
  const commit = args.commit || getLatestRelevantCommit();
  if (!commit) {
    throw new Error("No Claude delta commit available to store.");
  }
  if (!isValidCommit(commit)) {
    throw new Error(`Invalid commit: ${commit}`);
  }

  const commitField = `last_${fieldPrefix}_commit`;
  const timestampField = `last_${fieldPrefix}_at`;

  state[commitField] = commit;
  state[timestampField] = new Date().toISOString();
  if (fieldPrefix === "applied") {
    state.last_reviewed_commit = commit;
    state.last_reviewed_at = state.last_applied_at;
  }

  writeJson(statePath, state);
  return state;
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const command = args._[0] || "scan";
  const statePath = path.resolve(args.state || DEFAULT_STATE_PATH);

  if (command === "scan" || command === "status") {
    const result = scanDeltas(args, statePath);
    if (args.json) {
      process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
      return;
    }
    process.stdout.write(formatTextReport(result));
    return;
  }

  if (command === "mark-reviewed") {
    const updated = markState(args, statePath, "reviewed");
    if (args.json) {
      process.stdout.write(`${JSON.stringify(updated, null, 2)}\n`);
      return;
    }
    process.stdout.write(
      `Stored last_reviewed_commit=${updated.last_reviewed_commit} in ${statePath}\n`,
    );
    return;
  }

  if (command === "mark-applied") {
    const updated = markState(args, statePath, "applied");
    if (args.json) {
      process.stdout.write(`${JSON.stringify(updated, null, 2)}\n`);
      return;
    }
    process.stdout.write(
      `Stored last_applied_commit=${updated.last_applied_commit} in ${statePath}\n`,
    );
    return;
  }

  throw new Error(
    "Usage: audit-claude-delta.mjs [scan|status|mark-reviewed|mark-applied] [--json] [--all] [--since REF] [--commit SHA] [--state PATH] [--limit N]",
  );
}

try {
  main();
} catch (error) {
  process.stderr.write(`${error.message}\n`);
  process.exit(1);
}
