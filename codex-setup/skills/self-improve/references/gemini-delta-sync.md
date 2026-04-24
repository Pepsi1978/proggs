# Gemini Delta Sync for Codex Self-Improve

Use this reference under the whiteboard's `## Oberste Direktive` when the user asks for anything like:

- compare Gemini CLI with Codex
- port Gemini setup improvements to Codex
- check what is newer in Gemini rules, memory, or setup behavior

## Mandatory Guardrails

- treat `Gemini-Setup/` and Gemini memory paths as read-only comparison sources
- never write, delete, or reorganize files there from a Codex self-improve run
- write only into `codex-setup/`, `AGENTS.md`, or Codex validation files when needed
- run `codex-setup/scripts/audit-gemini-delta.*` before proposing any Gemini-side port

## Required Classification

Every Gemini-delta candidate must be labeled as one of:

- `ADD`
- `ADAPT`
- `REPLACE`

Meaning:

- `ADD`: directly additive to Codex
- `ADAPT`: useful but requires translation into Codex structure
- `REPLACE`: would overwrite existing Codex text or behavior

Proposal rule:

- never auto-apply Gemini bridge findings
- always prepare grouped A1/B1/C1/D1 proposals first
- require explicit user approval before implementing even `ADD` or `ADAPT`

Setup-level bugfixes, memory patterns, and hardening logic are in scope when they improve rules,
validation, runtime glue, workspace separation, or other programming-environment behavior.
