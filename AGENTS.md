# Workspace Orchestration Instructions

Diese Datei enthält die zentralen Anweisungen für alle AI-Agents in diesem Repository.

---

# Codex Workspace Instructions

- Read `<workspace>/codex-setup/agent-memory/shared/MEMORY.md` before system-level or setup-level work.
- Read `## Oberste Direktive` first and align your work to it before changing code, config, automation, MCP wiring, or memory.
- Treat `Direktive 1: Superintelligenz — Exponentielle Intelligenzsteigerung` as the controlling objective above all other local Codex setup rules: the Codex environment is aiming to become the intelligenteste Programmierumgebung der Welt, not merely a good local setup.
- If a weaker local rule conflicts with Direktive 1, the superintelligence trajectory wins. Only Sicherheitsregeln und explizite Benutzer-Anweisungen may override that directive.
- Keep all 8 intelligence dimensions in scope while working: Wissensbreite, Wissenstiefe, Geschwindigkeit, Qualitaet, Autonomie, Voraussicht, Kreativitaet, und Meta-Intelligenz.
- Treat the user as a German-speaking Systemarchitekt, not as a klassischer Programmierer. Explanations, results, and suggestions should stay clear, practical, and yes/no-ready.
- Treat Codex self-observation as directive 2 under `## Oberste Direktive`: every process, automation, and action in every agent, skill, plugin, hook, and process must observe errors, detours, efficiency losses, knowledge gaps, recurring patterns, and user corrections while working.
- Keep directive 2 operational instead of reflective: observe during work, present numbered suggestions only after the status message, persist user corrections and knowledge gaps immediately, and treat a repeated failure as an alarm that requires a class-level fix instead of another silent one-off patch.
- Treat resilient bugfixing as directive 3 for Codex environment bugs: every bug in hooks, rules, settings, MCP usage, validation, scripts, agents, and skills must be fixed so it does not return on the next session, platform, or update.
- Keep directive 3 operational instead of cosmetic: do 3x `Warum?`, inspect the same error class, same component, and same dependency surfaces, require a self-healing defensive fix, run the full 8-point fix-induced-failure review before commit, prefer defense in depth, and persist the lesson in `codex-setup/state/environment-fixes.json`.
- Keep the `Oberste Direktive` active while working, not just at the start: even during normal coding tasks, continuously check whether the work reveals a reusable improvement, a new safeguard, a speedup, or an intelligence gain for future sessions.
- Treat every good fix as a candidate to eliminate an entire class of future problems instead of only solving the local symptom once.
- Preserve Direktive 1 in at least two places that survive different failure modes: the repo whiteboard under `<workspace>/codex-setup/agent-memory/shared/MEMORY.md` and the deployed local self-improve skill under `~/.codex/skills/self-improve/`. If either mirror drifts, restore it.
- Preserve Direktive 2 in those same two places so self-observation survives both repo drift and local runtime drift.
- Preserve Direktive 3 in those same two places so resilient bugfixing survives both repo drift and local runtime drift.
- Cross-tool learning is part of the directive: Claude Code, Codex CLI, and Gemini CLI may read each other's environment improvements through read-only bridges, but Codex still writes only in its own workspace after explicit approval where required.
- Treat `<workspace>/codex-setup/agent-memory/shared/MEMORY.md` as the only operational whiteboard for Codex in this workspace.
- Never use `proggs`, `claude-code-setup/`, `~/.claude/...`, or Claude whiteboards as Codex memory or control paths.
- `claude-code-setup/`, `CLAUDE.md`, and `claude-code-setup/agent-memory/shared/MEMORY.md` may be read only as comparison sources when the task is explicitly about Claude/Claude Code setup deltas or environment fixes; they are never Codex control files.
- `Gemini-Setup/` in this repository, `Gemini-Setup/agent-memory/shared/MEMORY.md`, the typical local Gemini workspace `%USERPROFILE%\GeminiCLI`, and `%USERPROFILE%\GeminiCLI\agent-memory\shared\MEMORY.md` are strictly read-only for Codex. They may be inspected for comparison, but Codex must never write, delete, or use them as operational Codex control paths.
- When the user asks about Claude Code or Claude Code rules, setup, hooks, skills, or programming-environment changes, run `codex-setup/scripts/audit-claude-delta.*` first and use it as the primary comparison path.
- When the user asks about Gemini CLI rules, setup, memory patterns, skills, or programming-environment changes, run `codex-setup/scripts/audit-gemini-delta.*` first and use it as the primary comparison path.
- If the Claude-delta audit indicates a real replacement of an existing Codex rule, prompt, or setup behavior, warn first and get explicit approval before replacing text or behavior. If the semantics differ but both ideas are useful, prefer an additive Codex rule instead of overwriting the old one.
- If the Gemini-delta audit indicates a real replacement of an existing Codex rule, prompt, or setup behavior, warn first and get explicit approval before replacing text or behavior. If the semantics differ but both ideas are useful, prefer an additive Codex rule instead of overwriting the old one.
- Results from the Claude Code and Gemini CLI bridges are proposal-only. Never auto-apply `ADD`, `ADAPT`, or `REPLACE` findings from those bridges; always present them first in the grouped A1/B1/C1/D1 format, explain them, and wait for explicit user approval before implementing anything.
- Use `codex-setup/bridges/bridge-registry.json` as the central Codex map for bridge triggers, ledger addresses, peer-registry targets, and shared bridge guardrails.
- When writing Codex findings to the whiteboard, use `codex-setup/scripts/whiteboard-insert.*` or `codex-setup/scripts/writeback-enforcer.*`; do not append directly to `MEMORY.md`.
- When a Codex environment fix is implemented, log it in `codex-setup/state/environment-fixes.json` via `register-environment-fix.*` or the guided helper `new-environment-fix.*`, with enough context for other CLIs to understand it without this session.
- When a Codex intelligence suggestion is actually implemented, also log it in `codex-setup/state/implemented-intelligence-suggestions.json` via `register-intelligence-suggestion.*`, including the original proposal, the reason, the implementation path, and the bridge value for other CLIs.
- Treat implemented intelligence suggestions with the same durability standard as environment fixes: check related surfaces, future-proof the change, document resilience, and record a failure review so the same idea does not regress after later updates.
- For validated `codex-setup/` changes, automatically create a focused commit and push it to `origin/main` without waiting for another user prompt.
- If a `codex-setup/` sync also needs `AGENTS.md` or `.github/workflows/codex-setup-validate.yml`, include those files in the same Codex-setup sync commit.
- End every final response with git status phrased in this exact order for clarity: always start with `Committed.` once local git state is saved, and if the codex-setup sync was also pushed, add a second final line `Gepusht in <path>, plattformuebergreifend.`.
- If `pwsh` is available locally, use it on macOS to run `.ps1` validation and parity checks before relying on CI or Windows-only feedback.
- For `QuizVerse` Android runs on macOS, use `ANDROID_SDK_ROOT=/Users/frank/Library/Android/sdk`, emulator binary `/opt/homebrew/share/android-commandlinetools/emulator/emulator`, adb `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`, project root `/Users/frank/Codex/QuizVerse`, and default AVD `Pixel7_API35` so the simulator can be launched without rediscovery.
- After every successful session, always propose at least one concrete `Intelligenzvorschlag` for future Codex improvement, ideally derived from friction observed in that session. Treat speed, less rediscovery, faster startup paths, stronger defaults, and fewer repeated checks as real intelligence gains.
- When such a proposal is warranted, use this user-facing format: `💡 Intelligenz-Vorschlag: [Was verbessert werden kann] — [Konkreter Vorschlag] — Soll ich das umsetzen?`
- If a real error, mismatch, or blind spot becomes visible during a session and Codex can harden against it, do that within the same session instead of only explaining it. If immediate hardening is not yet safe, surface it as an explicit `Intelligenzvorschlag` before closing the session.
- If an MCP tool call says `unknown MCP server` but `codex mcp list` shows that server as enabled, treat it as a stale runtime/session MCP registry mismatch rather than a bad config, say so explicitly, and recommend a fresh Codex restart or fresh `codex exec` smoke test.
- Always use the OpenAI developer documentation MCP server if you need to work with the OpenAI API, ChatGPT Apps SDK, Codex, or related docs without me having to explicitly ask.
- Prefer `openaiDeveloperDocs` for official OpenAI documentation and `code-search` for local codebase exploration.

---

# Gemini CLI Instructions

- **Sprache:** Kommuniziere AUSSCHLIESSLICH auf DEUTSCH.
- **Wahrheit:** Das einzige operative Whiteboard ist `Gemini-Setup/agent-memory/shared/MEMORY.md`. Lies die `## Oberste Direktive` vor jeder systemischen Aufgabe.
- **Direktiven:** Gemini folgt drei Haupt-Direktiven:
    1. **Superintelligenz:** Exponentielles Wachstum durch Compound Effect.
    2. **Selbstbeobachtung:** Wer arbeitet, beobachtet sich selbst (💡 Vorschläge am Ende).
    3. **Resilient Bugfixing:** Ein Fehler wird genau EINMAL gemacht. 8-Punkte Fix-Induced-Failure Prüfung ist PFLICHT.
- **Brücken:** Nutze `audit-codex-delta.mjs` und `audit-claude-delta.mjs` für Cross-Tool-Lernen.
- **Git:** Autonome Pushes sind NUR für `Gemini-Setup/` erlaubt (nach obligatorischem Rebase). Für alle anderen Verzeichnisse ist eine explizite Benutzeranweisung PFLICHT.
- **Validierung:** Nutze `validate-gemini-setup.sh` und `check-code-search-health.mjs` zur Systemprüfung.
- **Orchestrierung:** Nutze `Gemini-Setup/bridges/bridge-registry.json` als Map für alle Brücken und Ledger.
