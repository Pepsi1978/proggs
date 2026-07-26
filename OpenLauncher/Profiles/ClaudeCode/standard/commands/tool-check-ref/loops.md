# Tool-Check: Loop-Checklisten (v2.0)

## Loop 1 — Oberflaeche (Surface Scan)

Focus on immediately visible issues. Skip anything the pre-scan already caught.

**Bug-Scan:**
- Syntax errors, typos in variable/function names
- Uninitialized or unused variables and imports
- Empty catch/error blocks that swallow errors silently
- Missing null/undefined/nil checks that could cause crashes
- Obvious logic errors (off-by-one, wrong comparisons, inverted conditions)
- Hardcoded values that should be configurable (magic numbers, paths, URLs)
- Deprecated API usage that could break in future versions
- Missing return statements or unreachable code

**Sicherheit (Security):**
- Hardcoded API keys, passwords, tokens, or secrets
- Missing input validation at system boundaries
- Obvious injection vectors (SQL, XSS, command injection)
- Insecure default configurations

**Kreative Ebene:**
Start with these heuristics, then think freely:
- Functions over 50 lines → split candidate
- Nesting depth > 3 → early return candidate
- Repeated code blocks (3+ similar lines) → abstraction candidate
- Boolean parameters → separate function candidate
- Comments explaining "what" not "why" → code clarity issue

Go beyond the checklist: What would make this code a joy to read and maintain?

---

## Loop 2 — Tiefenanalyse (Deep Analysis)

Focus on control flow, state management, and timing. Only NEW findings.

**Bug-Scan:**
- Race conditions (concurrent access, async timing)
- Memory leaks (unclosed streams, unremoved listeners, growing collections)
- Resource leaks (file handles, DB connections, timers without cleanup)
- Error propagation problems (caught too early, lost context, swallowed)
- Edge cases (empty inputs, boundary values, Unicode, locale-specific)
- Callback/Promise/async-await misuse (missing await, unhandled rejections)
- State management bugs (stale closures, shared mutable state)

**Performance:**
- Unnecessary re-computation or re-rendering
- N+1 query patterns (DB/API calls in loops)
- Blocking operations on main thread
- Large data structures copied unnecessarily
- Missing debounce/throttle on high-frequency events
- Excessive object creation in hot paths

**Sicherheit (Security):**
- CSRF/SSRF potential
- Insecure deserialization or eval usage
- Path traversal vulnerabilities
- Missing rate limiting or auth checks
- Improper token/session handling

**Kreative Ebene:**
- Would a design pattern eliminate recurring complexity?
- Would caching cut response time 50%+ or eliminate redundant work?
- Is there a battle-tested library that replaces 100+ lines of custom code?
- Could a consistent error strategy prevent entire categories of bugs?

What if someone with unlimited time and deep care for elegance wrote this?

---

## Loop 3 — Architektur & Kreativ (Architecture & Creative)

Focus on the big picture. Only NEW findings not covered by previous loops.

**Architektur:**
- Is the structure logical? Are responsibilities clearly separated?
- Is the dependency graph clean or circular?
- Is error handling consistent across the codebase?
- Is the code testable? Could tests be added easily?
- Is the API surface clean and well-documented?
- Is configuration separated from logic?
- Are there god objects/functions doing too much?

**Sprachen-Bewertung:**
- Is the chosen language the best fit for this task?
- Would a different language provide significant benefits?
  - Better type safety (JS → TypeScript)
  - Better performance (Python → Rust for CLI)
  - Smaller binary (Python → Go)
  - Better ecosystem for this use case
- Only suggest language change if the benefit is SUBSTANTIAL.

**Kreative Ebene:**
- Could the entire approach be rethought?
- Are there emerging technologies that replace complex custom code?
- Could splitting or merging components improve maintainability?
- Could the tool be self-healing or more resilient?
- Would a plugin/extension system add value?
- What would v2.0 look like starting from scratch?
- Can this be 10x simpler while keeping 90% of functionality?

Bold ideas welcome — the user decides what resonates.

---

## Loop 4 — Optik & UX (Visual Quality & User Experience) — NEU v2.0

Focus on how the tool LOOKS and FEELS. This loop treats visual quality as a first-class
concern — software should look like it was bought in a store, not hacked together.

**Visuelle Konsistenz:**
- Are colors consistent? (Same blue everywhere, not 3 different blues)
- Are spacing/margins/padding uniform? (Same gaps between elements)
- Are font sizes consistent for the same hierarchy level?
- Are icons from the same family/style? (No mixing Material + FontAwesome)
- Are border radii consistent? (All rounded or all sharp, not mixed)
- Are shadows/elevations used consistently?

**Layout & Hierarchie:**
- Is there a clear visual hierarchy? (Headers > Subheaders > Body > Caption)
- Is whitespace used effectively? (Breathing room, not cramped)
- Are related elements grouped visually?
- Is alignment consistent? (Left-aligned text, centered headers, etc.)
- Does the layout break at different window sizes / screen orientations?

**Zustandsbehandlung (State Handling):**
- Are there empty states? ("Keine Daten vorhanden" statt leerer Bildschirm)
- Are there loading states? (Spinner, Skeleton, Progress indicator)
- Are there error states? (Fehlermeldung statt Absturz oder leere Seite)
- Are there success states? (Bestaetigung nach Aktion)
- Are transitions smooth? (Fade, slide — nicht abruptes Erscheinen/Verschwinden)

**Moderne UI-Patterns:**
- Could a dark mode improve the experience?
- Would subtle animations add polish? (Hover effects, micro-interactions)
- Are touch targets large enough for mobile? (min 44x44 pt)
- Are interactive elements clearly distinguishable from static content?
- Would a toast/snackbar system improve feedback?
- Could drag-and-drop simplify a workflow?

**Barrierefreiheit (Accessibility):**
- Is the color contrast sufficient? (WCAG AA: 4.5:1 for text)
- Are interactive elements keyboard-navigable?
- Are images/icons labeled for screen readers?
- Is the text readable at default zoom? (min 14sp/pt body text)

**Fehlende UI-Features (Feature Gaps durch Optik entdecken):**
This is the key creative question of Loop 4: What NEW FEATURES become obvious
when you look at the tool through a visual lens?
- Would a search/filter bar help when there are many items?
- Would a settings page replace hardcoded configs?
- Would a status bar/dashboard give the user better overview?
- Would keyboard shortcuts speed up power users?
- Would an onboarding/tutorial screen help new users?
- Would a "Recent" or "Favorites" section reduce navigation?
- Would visual grouping (tabs, accordion, sidebar) organize complex content?
- Would charts/graphs make data more understandable than tables?

**Plattform-spezifische UI-Checks:**
- **Android/Kotlin**: Material Design 3 compliance, edge-to-edge, dynamic color
- **iOS/Swift**: Human Interface Guidelines, SF Symbols, system colors
- **Windows/C#/WPF**: WinUI 3 / Fluent Design patterns, high-DPI support
- **Web/TypeScript**: Responsive breakpoints, PWA capabilities, print stylesheet
- **CLI Tools**: Help text formatting, colored output, progress bars, `--quiet` flag

**Kreative Optik-Ebene:**
- Would a complete visual refresh (neue Farben, neue Fonts) modernize the tool?
- Could the branding be stronger? (Logo, accent color, consistent typography)
- What would a designer critique about this interface?
- If this tool were in an app store, would the screenshots look professional?
- Could a splash screen or about dialog add polish?
