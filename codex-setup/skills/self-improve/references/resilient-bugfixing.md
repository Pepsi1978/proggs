# Resilient Bugfixing for Codex Self-Improve

Use this reference under the whiteboard's `## Oberste Direktive` whenever the run fixes a bug in the Codex programming environment itself.

Scope:

- hooks
- rules
- settings
- MCP integration
- validators
- scripts
- agents
- skills

Not in scope:

- normal application bugs in user projects

Required workflow:

1. Find the real root cause, not just the symptom
2. Ask 3x `Warum?`
3. Check same error class, same component, and same dependency surfaces
4. Build a future-proof fix, not a one-off patch
5. Run a fix-induced-failure review before considering the fix done
6. Prefer defense in depth over a single guard
7. Log the fix in `codex-setup/state/environment-fixes.json` with the richer resilient-bugfixing fields

Required logging details:

- exact or recognizable error text
- why-chain
- related surfaces checked
- wrong pattern
- right pattern
- avoidance rule
- resilience summary
- fix-induced-failure review

Goal:

- every environment bug should be made exactly once
- each fix should remove a class of future failures, not only the visible instance
