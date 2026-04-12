# Spec-First Enforcement

## Rule: Specify Before You Code

For non-trivial features (more than a simple bug fix or config change):

1. **Before writing implementation code**, write a spec to `/tmp/current-spec.md`:
   - **Invariants**: What must ALWAYS be true? (e.g., "score is never negative")
   - **Preconditions**: What must the caller guarantee? (e.g., "list must not be empty")
   - **Postconditions**: What does this function guarantee? (e.g., "result is sorted")
   - **Edge Cases**: What is explicitly NOT supported? (e.g., "no Unicode emoji support")

2. **Tests come from specs, not from code.** When the `tester` agent runs, it reads
   `/tmp/current-spec.md` and generates tests that verify the INVARIANTS — not the implementation.

3. **The `code-reviewer` or `mar-reviewer` checks spec compliance**: Does the final code
   fulfill all 4 spec sections? If not, it's a FAIL.

## When to Skip

- Bug fixes under 10 lines
- Config/settings changes
- Documentation updates
- Version bumps
- Quiz question additions

## Why This Matters

Code that passes tests can still solve the WRONG problem. Specs force the agent to
articulate WHAT the code should do before HOW. This catches intent drift at the design
stage instead of during review. (Paper: arxiv 2511.17330 — AutoRocq)
