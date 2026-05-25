# Claude-Code Changelog-Archiv (chronologisch)

Master-Zeitleiste aller relevanten offiziellen Aenderungen am Claude-Code-Werkzeug.
Neueste oben. Quellen: code.claude.com/docs (whats-new + Wochenseiten, hooks, skills,
settings, costs, model-config u.a.). Erstlauf 2026-05-25, geprueft bis Version 2.1.150.

---

## 2026 — Woechentliche "What's New"-Highlights

- **KW 20 (Mai 2026):** `claude agents` Dashboard; `/goal` (autonome Zielverfolgung);
  Fast Mode fuer Opus 4.7 (~2,5x schneller); `if`-Hooks; `continueOnBlock` (PostToolUse);
  `terminalSequence` fuer Desktop-Notifications.
- **KW 19:** Plugin-Installation via ZIP/URL; `worktree.baseRef`; Hard Deny Rules.
- **KW 18:** Windows native PowerShell (kein Git Bash mehr noetig); `claude ultrareview` fuer CI.
- **KW 17:** `/ultrareview` oeffentlich; `/recap`; Custom Themes.
- **KW 16:** Opus 4.7 als Default; `xhigh` Effort-Level; Routines (geplante Agenten);
  Mobile Push; Native Binaries.
- **KW 15:** Ultraplan Web-Editor; Monitor-Tool; `/loop --until-green`; `/autofix-pr`.
- **KW 14:** Computer Use (Preview); 500K MCP-Results; `/powerup`.
- **KW 13:** Auto-Mode (Permission); `if`-Hooks (Basis); Native PowerShell Tool.

## Versionierte Aenderungen (Auswahl)

- **v2.1.150** (aktuell) — Basis dieses Erstlaufs.
- **v2.1.149** — [Plugins/MCP/Commands] `/usage` mit Kosten pro Plugin/Skill; Enterprise Managed MCP.
- **v2.1.147** — [Commands] `/simplify` → `/code-review` umbenannt.
- **v2.1.145** — [Skills] eingebaute Skills `/run`, `/verify`, `/run-skill-generator`; [Plugins] verbessertes `validate`.
- **v2.1.143** — [Plugins] Installation via ZIP, Plugin-Dependencies.
- **v2.1.139** — [Hooks] Exec-Form mit `args: string[]`; [Commands] `/goal`, `/scroll-speed`.
- **v2.1.133** — [Hooks] `effort.level` + `$CLAUDE_EFFORT` im Hook-Input.
- **v2.1.132** — [Hooks] `$CLAUDE_CODE_SESSION_ID` in der Bash-Subprozess-Umgebung.
- **v2.1.83** — [Settings] `auto`-Permission-Modus (Anthropic-API).
- **v2.1.63** — [Agents] Task-Tool → Agent-Tool umbenannt.
- **v2.1.32** — [Agents] Agent Teams (experimentell).
- **2026-04-24** — [Agents/MCP] parallele MCP-Initialisierung.
- **2026-01** — [Skills] SKILL.md-Format eingefuehrt.
- **2025-12** — [Settings] `.claude/rules/` mit `paths:`-Frontmatter.

> Details und Best Practices je Punkt: siehe die jeweilige Kategorie-Datei in `best-practices/`.
