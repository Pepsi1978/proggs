---
paths:
  - "**/Tampermonkey/**"
  - "**/*.user.js"
---

# Tampermonkey Script Rules

- ALWAYS bump version numbers on ANY change (both @name and @version in UserScript header)
- Commit messages follow format: `#NNN - Description` (sequential numbering)
- Repository: https://github.com/Pepsi1978/proggs
- All scripts share common UI patterns (buttons, overlays) — keep consistent
- Lint code: `bunx biome check <file>` (catches bugs and complexity issues in JS)
- Format code: `bunx biome format --write <file>` (consistent formatting)
- Test: Manually test in browser before committing (no automated test framework)
