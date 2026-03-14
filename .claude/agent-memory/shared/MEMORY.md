# Shared Knowledge Hub

This file is read and updated by both the code-reviewer agent and the self-improve skill.
It serves as a bridge between code review learnings and environment improvements.

## From Code Reviews (updated by code-reviewer)
<!-- code-reviewer writes patterns, recurring issues, and conventions it discovers here -->
_No entries yet — will be populated after first code review with memory: project._

## From Self-Improve (updated by self-improve skill)
- Environment: macOS, Claude Code v2.1.76, Opus 4.6 with 1M context (2026-03-14)
- Languages: Swift, C#, TypeScript, Rust, Go, Kotlin
- Quality gate: Use `quality-gate` agent for combined test+review+optimize
- Agents: code-reviewer has `memory: project`, coder has `isolation: worktree`
- New plugins: xclaude-plugin (iOS/Xcode MCP), apple-platform-build-tools (Xcode builds)
- Hooks: PostCompact (context summary), SubagentStop (agent result summary)
- Preferred patterns: MVVM (Swift), Fluent Design (C#), strict mode (TypeScript)

## Recurring Issues (shared)
<!-- Both agents can add patterns they see repeatedly -->
_No entries yet._

## Rules & Conventions (shared)
<!-- Project-wide conventions that both reviewing and improving should respect -->
- No Python for user-facing code
- Commit format: #NNN - Description (English)
- Communication: German, code comments: English
- Cross-platform: Always consider macOS + Windows parity
