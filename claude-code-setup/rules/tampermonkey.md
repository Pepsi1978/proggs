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
- Lint code: `bunx biome check <file>` or `npx prettier --check <file>` (Termux: use prettier)
- Format code: `bunx biome format --write <file>` or `npx prettier --write <file>`
- Test: Manually test in browser before committing (no automated test framework)
