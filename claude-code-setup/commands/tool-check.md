---
name: tool-check
description: Systematic code health scanner that audits projects or individual files for bugs, vulnerabilities, performance issues, and creative improvements. Use ONLY when the user explicitly says "/tool-check", "pruefe mein Tool", "check meinen Code", "Code-Gesundheitscheck", "finde Bugs", or "analysiere mein Projekt". NEVER run automatically or proactively — manual trigger only.
---

# Tool-Check: Systematischer Code-Gesundheitscheck

**Before doing ANYTHING, show the user this overview in German:**

```
Tool-Check v1.1 — Code-Gesundheitscheck
========================================

Was passiert jetzt:
Zuerst lasse ich automatische Tools laufen (Linter, LSP),
dann durchlaufe ich 3 manuelle Analyse-Schleifen.
Jede Runde graebt tiefer und findet subtilere Probleme.

  Pre-Scan — Automatische Tools
    Linter und LSP-Diagnosen fuer schnelle, objektive Checks

  Loop 1 — Oberflaeche
    Offensichtliche Bugs, Crashes, fehlende Fehlerbehandlung
    + kreative Verbesserungsideen

  Loop 2 — Tiefenanalyse
    Race Conditions, Memory Leaks, Sicherheitsluecken,
    Performance-Probleme + kreative Verbesserungsideen

  Loop 3 — Architektur & Kreativ
    Sprachenwahl, Strukturverbesserung, moderne APIs,
    alternative Ansaetze + kreative Verbesserungsideen

Danach:
  Alle Ergebnisse als nummerierte Liste praesentieren.
  Du entscheidest, was davon umgesetzt wird.

Zum Schluss:
  Meta-Verbesserung — Vorschlaege fuer den Skill selbst

Sicherheit: Nichts wird geaendert ohne deine Erlaubnis.
Du kannst jeden Schritt live mitlesen.
========================================
```

**Then proceed with the skill.**

You are performing a systematic code health check. This is not a quick lint — you run an automated pre-scan followed by 3 analysis loops, each at increasing depth, to find everything from obvious bugs to subtle architectural issues. Each loop also includes a creative layer where you think beyond "what's broken" and ask "what could be better".

The user is not a programmer. Explain everything in German, in simple terms, so they understand what you found and why it matters.

## Visibility Rules (CRITICAL)

- NEVER run anything in the background (no `run_in_background`, no silent subagents)
- The user MUST be able to read along with EVERY action in real-time
- Before each action, write a short German explanation of what you're about to do
- After each action, write the result so the user can follow the progress
- Think of it as a live protocol: the user reads along like watching a log file

## Target Detection

Determine what to scan based on the user's input:

- **Specific file(s)** → scan those files
- **Project/folder** → scan all source files in that folder
- **Unclear** → ask the user what to scan

Detect the programming language(s) automatically and note them. This determines which checks are relevant (e.g., memory management checks only matter for languages without garbage collection).

## Project Size Scaling

Adjust the analysis depth based on the project size to stay effective:

- **Klein (unter 10 Dateien):** Read all files completely. Analyze everything in every loop.
- **Mittel (10–30 Dateien):** Read entry points, core logic, and API handlers first. Read secondary files (utils, helpers, configs) only when referenced by findings or when time permits.
- **Gross (30+ Dateien):** Focus on the architecture and critical paths. In Loop 1, scan only entry points and core modules. In Loop 2, follow the call chains that Loop 1 flagged. In Loop 3, assess the overall structure. Report which files were skipped and why.

Always tell the user upfront: "Dein Projekt hat [N] Dateien — ich arbeite im [Klein/Mittel/Gross]-Modus."

## Pre-Scan: Automated Tool Check

Before the manual loops, run available linters and LSP diagnostics to catch objective, tool-detectable issues automatically. This frees the manual loops to focus on what only human-like analysis can find.

**Detect and run the appropriate tools based on the project language(s):**

| Language | Linter / Formatter | Command |
|----------|-------------------|---------|
| Swift | swift-format, swiftlint | `swiftlint lint --quiet [path]` |
| TypeScript/JS | Biome | `biome check [path]` |
| Go | golangci-lint | `golangci-lint run [path]` |
| Rust | cargo clippy | `cargo clippy -- -W warnings` |
| C# | dotnet format | `dotnet format --verify-no-changes [path]` |

Also check for LSP diagnostics if available (errors, warnings reported by the language server).

**After running tools:**
- Collect all warnings and errors
- Add them to the shared findings list, tagged as `[Auto]` so the user knows these came from tools
- Briefly report: "Pre-Scan abgeschlossen — [N] automatische Findings. Starte Loop 1 (Oberflaeche)."

If no tools are available for the detected language, skip the pre-scan and note it: "Kein Linter fuer [Sprache] verfuegbar — starte direkt mit Loop 1."

## The 3-Loop Process

Each loop collects findings into a shared list that grows across all 3 loops. Findings are categorized by severity:

- **Bug** — Something that is or could be broken (crash, data loss, wrong behavior)
- **Verbesserung** — Something that works but could be better (performance, maintainability, security)
- **Kreativ** — An idea for doing things differently (new approach, better tools, architectural change)

**CRITICAL: NEVER apply fixes during the loops.** Only collect findings. All fixes are applied after the user reviews and approves the final list.

**CRITICAL: No duplicates across loops.** Each loop builds on the previous ones. Before adding a finding, check if it (or a closely related issue) was already captured in a previous loop or in the pre-scan. If yes, skip it — don't repeat. Each loop should only contribute NEW insights that the previous phases missed.

---

### Loop 1 — Oberflaeche (Surface Scan)

Focus on immediately visible issues that don't require deep code understanding. Skip anything the pre-scan already caught.

**Bug-Scan:**
- Syntax errors, typos in variable/function names
- Uninitialized or unused variables and imports
- Empty catch/error blocks that swallow errors silently
- Missing null/undefined/nil checks that could cause crashes
- Obvious logic errors (off-by-one, wrong comparison operators, inverted conditions)
- Hardcoded values that should be configurable (magic numbers, file paths, URLs)
- Deprecated API usage that could break in future versions
- Missing return statements or unreachable code

**Sicherheit (Security):**
- Hardcoded API keys, passwords, tokens, or secrets
- Missing input validation at system boundaries (user input, API responses)
- Obvious injection vectors (SQL injection, XSS, command injection)
- Insecure default configurations

**Kreative Ebene (Creative Layer):**

Start with these concrete heuristics, then think freely beyond them:
- Functions over 50 lines → candidate for splitting
- Nesting depth > 3 levels → candidate for early returns or extraction
- Repeated code blocks (3+ similar lines) → candidate for abstraction
- Boolean parameters that change behavior → candidate for separate functions
- Comments explaining "what" instead of "why" → the code itself should be clearer

But these are just starting points. The creative layer is about genuine creativity — think about what would make this code a joy to read, maintain, and extend. Consider unconventional ideas, surprising simplifications, or entirely different approaches that nobody asked for but that would make the project better. No idea is too bold here.

After Loop 1, briefly report: "Loop 1 abgeschlossen — [N] neue Findings (gesamt: [M]). Starte Loop 2 (Tiefenanalyse)."

---

### Loop 2 — Tiefenanalyse (Deep Analysis)

Focus on issues that require understanding control flow, state management, and timing. Only add findings that Loop 1 and the pre-scan did not already cover.

**Bug-Scan:**
- Race conditions (concurrent access to shared state, async timing issues)
- Memory leaks (unclosed streams/connections, unremoved event listeners, growing collections)
- Resource leaks (file handles, database connections, timers without cleanup, observer patterns)
- Error propagation problems (errors caught too early, lost error context, swallowed exceptions)
- Edge cases in business logic (empty inputs, boundary values, Unicode, locale-specific behavior)
- Callback/Promise/async-await misuse (missing await, unhandled rejections, callback hell)
- State management bugs (stale closures, shared mutable state, inconsistent state transitions)

**Performance:**
- Unnecessary re-computation or re-rendering
- N+1 query patterns (database, API calls in loops)
- Blocking operations on the main thread (long loops, synchronous I/O)
- Excessive DOM manipulation (no batching, no virtual DOM, no requestAnimationFrame)
- Large data structures copied unnecessarily (spread operator on large arrays/objects)
- Missing debounce/throttle on high-frequency events

**Sicherheit (Security):**
- CSRF/SSRF potential
- Insecure deserialization or eval usage
- Path traversal vulnerabilities
- Missing rate limiting or authentication checks
- Improper token/session handling

**Kreative Ebene (Creative Layer):**

Start with these structural questions, then let your imagination run:
- Would a design pattern eliminate recurring complexity? (Observer, Strategy, Factory, etc.)
- Would caching cut response time by 50%+ or eliminate redundant work?
- Is there a battle-tested library that replaces 100+ lines of custom code?
- Could a consistent error-handling strategy (Result types, error boundaries) prevent entire categories of bugs?

Then go further: What if this code were written by someone who had unlimited time and cared deeply about elegance? What would they change? What would surprise the user in a good way?

After Loop 2, briefly report: "Loop 2 abgeschlossen — [N] neue Findings (gesamt: [M]). Starte Loop 3 (Architektur & Kreativ)."

---

### Loop 3 — Architektur & Kreativ (Architecture & Creative)

Focus on the big picture — is the approach fundamentally right? Only add findings not covered by previous loops.

**Architektur:**
- Is the code structure logical? Are responsibilities clearly separated?
- Is the dependency graph clean or are there circular dependencies?
- Is the error handling strategy consistent across the codebase?
- Is the code testable? Could tests be added easily?
- Is the API surface clean and well-documented?
- Is configuration properly separated from logic?
- Are there god objects/functions that do too much?

**Sprachen-Bewertung (Language Assessment):**
- Is the chosen language the best fit for this task?
- Would a different language provide significant benefits?
  - Better type safety (e.g., JS → TypeScript)
  - Better performance (e.g., Python → Rust for CLI tools)
  - Smaller binary / no runtime dependency (e.g., Python → Go)
  - Better ecosystem/libraries for this use case
  - Better cross-platform support
- **Only suggest a language change if the benefit is substantial.** "Could be 5% faster" is not worth a rewrite. "Would eliminate an entire class of bugs" might be.

**Kreative Ebene (Creative Layer):**

This is the most open loop — think big:
- Could the entire approach be rethought for better results?
- Are there emerging technologies or APIs that could replace complex custom code?
- Could the UX/DX be significantly improved with a different architecture?
- Would splitting or merging components improve maintainability?
- Is there a way to make the tool self-healing or more resilient?
- Could the tool benefit from a plugin/extension system?
- What would a v2.0 look like if you could start from scratch with everything you now know?
- Is there a way to make this tool 10x simpler while keeping 90% of the functionality?

The creative layer thrives on bold ideas. Not every idea needs to be practical — some are sparks that lead to unexpected breakthroughs. Capture them all and let the user decide what resonates.

After Loop 3, report: "Alle 3 Loops abgeschlossen — [N] Findings insgesamt. Erstelle die Zusammenfassung."

---

## After All 3 Loops: Presentation

Present ALL findings as a single numbered list, grouped by category. This is the most important part — the user makes decisions based on this list.

```
## Tool-Check Ergebnisse fuer [Projektname/Dateiname]

### Erkannte Sprache(n): [z.B. JavaScript, TypeScript]
### Analysierte Dateien: [Anzahl]
### Projekt-Modus: [Klein/Mittel/Gross]
### Gefundene Probleme: [Anzahl total] ([N] automatisch, [M] manuell)

---

### Bugs (muessen gefixt werden)
1. [Datei:Zeile] — Beschreibung des Problems
   Fix: Konkrete Loesung
   Risiko: Was passiert wenn man es nicht fixt

2. [Datei:Zeile] — ...
   Fix: ...
   Risiko: ...

### Verbesserungen (sollte man machen)
N. [Datei:Zeile] — Beschreibung
   Fix: Konkrete Loesung
   Vorteil: Was wird dadurch besser

### Kreative Ideen (koennte man machen)
M. [Datei] — Beschreibung der Idee
   Umsetzung: Wie es gemacht werden koennte
   Aufwand: Einschaetzung (klein/mittel/gross)

### Architektur-Vorschlaege (optional, aber spannend)
K. Beschreibung — Begruendung
   Aufwand: Einschaetzung
   Nutzen: Was sich dadurch verbessert

---

Welche Punkte soll ich umsetzen?
(z.B. "1, 2, 5, 8" oder "alle Bugs" oder "alles ausser 12")
```

**Each finding must include:**
- Exact file and line number (where applicable)
- `[Auto]` tag if it came from the pre-scan
- Clear description of the problem in German
- Concrete fix suggestion (not just "fix this" — show what the code should look like)
- Why it matters (what could go wrong, or what improves)

**Wait for the user to choose which items to implement.**

## Applying Fixes

After the user selects items:

1. Apply ONLY the selected fixes, one by one, visibly
2. After each fix, briefly confirm what was changed
3. Bump version numbers if the project uses versioned files (check for version patterns in file headers, package.json, Cargo.toml, Info.plist, etc.)
4. Run any available tests or linters to verify fixes don't break functionality
5. Commit and push (per CLAUDE.md automation rules)
6. Report which fixes were applied and which were skipped

---

## Phase 2: Meta-Improve (Skill Self-Improvement)

After the code analysis is complete (whether or not fixes were applied), this skill analyzes and improves itself. This happens at the end of every run.

### Step 1: Self-Analysis

Reflect on the analysis that just ran:

- Which checks found nothing useful? Could they be replaced with more targeted checks?
- Were there issues that the loops missed but should have caught?
- Did any phase take too long or produce low-value results?
- Were there new patterns or techniques discovered that should become standard checks?
- Was the loop depth distribution right? (Too much surface, not enough depth?)
- Did the creative layer produce genuinely useful ideas?
- Did the pre-scan catch things the manual loops would have found? (Good — that's the point)
- Did the project size scaling work well, or were important files skipped/included unnecessarily?

### Step 2: Line Count Check

Count lines of this skill file:
```
wc -l ~/.claude/commands/tool-check.md
```

- **Under 500 lines**: Suggestions can be made freely
- **500-600 lines**: Warn that the limit is approaching
- **600+ lines**: STOP. Ask the user how to proceed (compress? split into sub-files? remove low-value sections?)

### Step 3: Present Suggestions (NEVER auto-apply!)

```
## Meta-Verbesserung: Vorschlaege fuer Tool-Check

### Vorschlag 1: [Titel]
**Was**: [Was soll geaendert werden]
**Warum**: [Begruendung aus der Analyse]
**Wo im Skill**: [Phase/Zeile die betroffen ist]

### Vorschlag 2: [Titel]
...

### Skill-Status
- Aktuelle Zeilenzahl: [N]/600
- Letzte Meta-Verbesserung: [Datum oder "erste"]

Soll ich diese Aenderungen umsetzen? (Ja/Nein/Teilweise)
```

**CRITICAL**: NEVER modify this skill file without explicit user approval. Only suggest, never auto-apply.

### Step 4: Apply (only after user says yes)

If the user approves:
1. **BACKUP FIRST**: Commit the current skill version to GitHub BEFORE making changes
2. Apply the approved changes to `~/.claude/commands/tool-check.md`
3. Update the version number and date at the bottom of this file
4. Document exactly what changed (old → new)
5. Sync to `~/proggs/claude-code-setup/commands/`

---

## Sync to GitHub

After the skill is modified, sync to the cross-platform repo:

1. Copy: `cp ~/.claude/commands/tool-check.md ~/proggs/claude-code-setup/commands/`
2. Commit and push to `Pepsi1978/proggs` (NEVER create a separate repo):
   ```
   cd ~/proggs && git add claude-code-setup/commands/ && git commit -m "#NNN - Update tool-check skill" && git push
   ```
3. Report sync status

---

## Important Rules

- NEVER apply fixes without user approval — always present findings first, then wait for selection
- NEVER modify this skill file without explicit user approval (Meta-Improve is suggest-only)
- NEVER run this skill automatically — manual trigger only, when the user explicitly requests it
- NEVER delete code without showing what will be removed and getting approval
- **Transparency**: Every finding must include file, line, description, fix, and reasoning. No vague findings.
- **Communication**: German with the user, English for code comments and commits
- **Before modifying this skill**: Always commit the current version as a backup first
- **600-line limit**: If approaching, warn the user. If exceeded, stop and ask.
- **Security**: If a critical vulnerability is found (exposed secrets, remote code execution), report it IMMEDIATELY — don't wait for all loops to finish
- **Visibility**: Everything happens in the main conversation, nothing hidden, no background work
- **Scope respect**: Only analyze and fix what the user asked for. Don't expand scope without asking.
- **No duplicates**: Each loop only adds NEW findings. Check previous loops before adding.
- NEVER create new GitHub repositories. ALL changes go to `Pepsi1978/proggs`.

---
<!-- Skill Version: v1.1 | Date: 2026-03-11 | Last Meta-Improve: none | Lines: 392/600 -->
