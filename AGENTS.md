# Codex Workspace Instructions

- Read `/Users/frank/Codex/codex-setup/agent-memory/shared/MEMORY.md` before system-level or setup-level work.
- Read `## Oberste Direktive` first and align your work to it before changing code, config, automation, MCP wiring, or memory.
- Treat `/Users/frank/Codex/codex-setup/agent-memory/shared/MEMORY.md` as the only operational whiteboard for Codex in this workspace.
- Never use `proggs`, `claude-code-setup/`, `~/.claude/...`, or Claude whiteboards as Codex memory or control paths.
- When writing Codex findings to the whiteboard, use `codex-setup/scripts/whiteboard-insert.*` or `codex-setup/scripts/writeback-enforcer.*`; do not append directly to `MEMORY.md`.
- For validated `codex-setup/` changes, automatically create a focused commit and push it to `origin/main` without waiting for another user prompt.
- If a `codex-setup/` sync also needs `AGENTS.md` or `.github/workflows/codex-setup-validate.yml`, include those files in the same Codex-setup sync commit.
- End every final response with exactly one git status line: `Committed.` if changes were only committed, or `Gepusht in <path>, plattformuebergreifend.` if the codex-setup sync was pushed and is available cross-platform.
- If `pwsh` is available locally, use it on macOS to run `.ps1` validation and parity checks before relying on CI or Windows-only feedback.
- For `QuizVerse` Android runs on macOS, use `ANDROID_SDK_ROOT=/Users/frank/Library/Android/sdk`, emulator binary `/opt/homebrew/share/android-commandlinetools/emulator/emulator`, adb `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`, project root `/Users/frank/Codex/QuizVerse`, and default AVD `Pixel7_API35` so the simulator can be launched without rediscovery.
- Always use the OpenAI developer documentation MCP server if you need to work with the OpenAI API, ChatGPT Apps SDK, Codex, or related docs without me having to explicitly ask.
- Prefer `openaiDeveloperDocs` for official OpenAI documentation and `code-search` for local codebase exploration.
