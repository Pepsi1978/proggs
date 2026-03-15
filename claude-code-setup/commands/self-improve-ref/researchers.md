# Self-Improve: Researcher Templates (v5.0)

## Research Caching

Before spawning researchers, check for a recent snapshot:
```bash
# Path varies by platform — use the project-specific memory directory:
# macOS:   ~/.claude/projects/-Users-frank/memory/reference_last_research_snapshot.md
# Windows: ~/.claude/projects/C--Users-barwa/memory/reference_last_research_snapshot.md
# Termux:  ~/.claude/projects/-data-data-com.termux-files-home/memory/reference_last_research_snapshot.md
SNAPSHOT="$(find ~/.claude/projects/*/memory -name 'reference_last_research_snapshot.md' 2>/dev/null | head -1)"
# If snapshot exists and is < 7 days old: skip R2, R3, R4 (use cached data)
# Always run: R1 (Claude Code Updates), R5 (Security), R6 (Creative)
# If snapshot is > 7 days old or missing: run ALL 6 researchers
```

When a run completes, save a snapshot to memory:
```markdown
---
name: reference_last_research_snapshot
description: Cached research results from last self-improve run — expires after 7 days
type: reference
---
Date: [today]
Expires: [today + 7 days]

## R2 Cache: Plugins
[summary of plugin findings]

## R3 Cache: Parallelization
[summary of patterns found]

## R4 Cache: Tool Versions
[version comparison table]

## R6 Cache: Meta-Evolution (NEW v5.1)
[recommended plugins with status: reviewed/installed/skipped]
[generated ideas with status: built/planned/rejected]
[evolution-analyst findings: quality trend, top weaknesses]
[R6 research approach for next run: what to search for, what to feed into evolution-analyst]
```

## Researcher Templates

**MANDATORY: Spawn ALL active researchers in ONE message block.**

### R1 — Claude Code Updates (ALWAYS run)
```
"Research Claude Code CLI updates and new features. Current version: [version].
Search: (1) changelog since this version, (2) new settings/env vars,
(3) performance improvements, (4) breaking changes.
Spawn sub-agents to verify: one for changelog, one for settings docs.
Return only actionable findings with version numbers. Date: [today]."
```

### R2 — Plugins & Marketplace (skip if cached < 7 days)
```
"Research new Claude Code plugins available in [month+year].
Search: (1) official plugins (anthropics/claude-plugins-official),
(2) superpowers-marketplace updates, (3) community plugins for Swift/C#/TypeScript/Rust/Go/Kotlin.

SELF-VALIDATION (MANDATORY): Before recommending ANY plugin:
1. Run `gh api repos/{owner}/{repo}` to verify the repo EXISTS and is NOT archived
2. Check if the plugin name appears in the installed plugins list below
3. Only recommend plugins that pass BOTH checks

Installed plugins (DO NOT recommend these):
[PASTE enabledPlugins keys from settings.json]

Return: plugin name, source, what it does, stars, recommendation."
```

### R3 — Parallelization & Automation (skip if cached < 7 days)
```
"Research Claude Code agent teams, parallelization, automation best practices as of [today].
Search: (1) Agent Teams patterns, (2) new hook events/types,
(3) skill structuring for large skills, (4) worktree isolation patterns.
Spawn sub-agents per topic. Return actionable patterns with code examples."
```

### R4 — Tool Versions (skip if cached < 7 days)
```
"Find latest stable versions of: [tool list with current versions from scan].
IMPORTANT: Compare against EXACT versions from env-checker scan, not memory.
If exact scan data unavailable, mark as 'unverified'.
Spawn sub-agent to double-check each version against official release pages.
Return table: tool | current | latest | update needed? | how to update"
```

### R5 — Security (ALWAYS run)
```
"Search for known CVEs in: [tools+versions from scan].
Check: (1) CVEs in specific versions, (2) platform-specific issues,
(3) Claude Code security advisories, (4) supply chain alerts.
Spawn sub-agents per tool category.
.NET RULE: Map CVE Runtime versions to installed SDK band before flagging.
Return ONLY confirmed vulnerabilities with CVE numbers — no speculation."
```

### R6 — Creative Environment Explorer (ALWAYS run, NEW in v5.0)
```
"You are a CREATIVE researcher. Your job is NOT to check versions or find CVEs.
Your job is to discover NEW IDEAS for improving a Claude Code development environment
on macOS and Windows.

Think OUTSIDE the box:
1. Search for blog posts, GitHub repos, and discussions about creative Claude Code setups
2. Look for productivity tools, shell tricks, and automation patterns other developers use
3. Find interesting MCP server combinations or hook patterns nobody talks about
4. Look for cross-platform development workflows that are unusually efficient
5. Search for ways to measure and improve AI coding assistant performance
6. Find new CLI tools, TUI tools, or developer utilities released in [current month]

DO NOT search for: version numbers, CVEs, standard tool updates.
DO search for: surprising ideas, unconventional approaches, hidden gems.

For each finding: explain what it is, why it's interesting, and how it could
be adapted for this specific setup (macOS + Windows, Swift/C#/TypeScript/Rust/Go/Kotlin).

Return: at least 3 genuinely novel ideas with links and adaptation proposals."
```

## Cross-Validation Rules

After all researchers return, validate against scan data:

1. **System state** (scan) > **Memory** > **Web research**
2. **Version claims**: Only flag if local version is LOWER than latest
3. **Security claims**: Verify against OS patch data from scan
4. **"Not installed" claims**: Verify against scan + plugin list
5. **Contradictions**: Trust more specific source
6. **Schema validation**: Before implementing new hooks/settings, verify they exist in actual JSON schema

## Plugin Security Review Template

For each recommended plugin, spawn a researcher:
```
"Security review of Claude Code plugin '[owner/repo]'.
Check: (1) GitHub metrics (stars, forks, last commit, maintainer),
(2) marketplace listing, (3) security advisories,
(4) code analysis (plugin.json, hooks, scripts — look for obfuscated code,
suspicious URLs, Base64, data exfiltration, prompt injection),
(5) dependency analysis.
Return: SAFE / CAUTION / UNSAFE + reasoning + red flags."
```
Only install SAFE or CAUTION-without-red-flags. Show assessment to user.
