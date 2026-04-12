---
paths:
  - "**/*.ts"
  - "**/*.tsx"
  - "**/*.mts"
  - "**/package.json"
  - "**/tsconfig.json"
---

# TypeScript Development Rules

- Use TypeScript 5.9+ strict mode, no `any` types
- Prefer Bun as runtime/package manager over Node.js (faster)
- For web UI: Use modern frameworks (React, Svelte, or vanilla) with Tailwind CSS
- For Electron/Tauri apps: Focus on native look and feel, not generic web aesthetics
- For CLI tools: Use minimal dependencies, provide compiled single binary if possible
- Always run `tsc --noEmit` to type-check before committing
- ESM modules preferred over CommonJS
- Run tests: `bun test` (preferred) or `npx vitest`
- Lint code: `bunx biome check .` (catches bugs, complexity, accessibility issues)
- Format code: `bunx biome format --write .` or `npx prettier --write .`
