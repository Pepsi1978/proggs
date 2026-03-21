---
name: tool-check
description: Systematic code health scanner (~5-20 min) that audits projects or individual files for bugs, vulnerabilities, performance, visual/UI quality, and creative improvements. Use ONLY when the user explicitly says "/tool-check", "pruefe mein Tool", "check meinen Code", "Code-Gesundheitscheck", "finde Bugs", "analysiere mein Projekt", "Code pruefen", "schau dir den Code an", "was ist kaputt", "Fehler suchen", "pruefe meine App", or "schneller Check". NEVER run automatically or proactively — manual trigger only.
---

# Tool-Check v2.0 — Systematischer Code-Gesundheitscheck

**Before doing ANYTHING, show the user this overview in German:**

```
Tool-Check v2.0 — Code-Gesundheitscheck
========================================

Was passiert jetzt:
Zuerst lasse ich automatische Tools laufen (Linter, LSP),
dann durchlaufe ich 4 Analyse-Schleifen.
Jede Runde graebt tiefer und findet subtilere Probleme.

  Pre-Scan — Automatische Tools
    Linter und LSP-Diagnosen fuer schnelle, objektive Checks

  Loop 1 — Oberflaeche
    Offensichtliche Bugs, Crashes, fehlende Fehlerbehandlung

  Loop 2 — Tiefenanalyse
    Race Conditions, Memory Leaks, Sicherheitsluecken, Performance

  Loop 3 — Architektur & Kreativ
    Sprachenwahl, Strukturverbesserung, alternative Ansaetze

  Loop 4 — Optik & UX (NEU)
    Visuelle Qualitaet, UI-Konsistenz, fehlende UI-Features

Danach:
  Alle Ergebnisse als nummerierte Liste praesentieren.
  Du entscheidest, was davon umgesetzt wird.

Express-Modus: Dateien unter 200 Zeilen → Pre-Scan + 1 Loop.
Abbruch: Sag jederzeit "genug" → Ergebnisse bis hierhin zeigen.
========================================
```

**Then proceed with the skill.**

You are performing a systematic code health check — an automated pre-scan followed by 4 analysis loops at increasing depth. Each loop includes a creative layer. The user is not a programmer — explain everything in German, simply.

## Core Rules

- NEVER run anything in background — user reads along with EVERY action in real-time
- Before each action: short German explanation. After each action: show result.
- NEVER apply fixes during loops — only collect findings. Fixes come after user approval.
- If a critical vulnerability is found (exposed secrets, RCE), report it IMMEDIATELY.
- The user can say "genug" or "stop" at any time → present findings collected so far.

## Available Tools (USE THEM)

Use ALL installed plugins, skills, agents, and MCP servers during analysis:
- **Serena** for semantic code analysis (symbol navigation, references)
- **Context7** for up-to-date library documentation
- **LSP plugins** for type-checking, diagnostics
- **WebSearch/WebFetch** for deprecation notices, security advisories
- **Custom agents** (tester, code-reviewer, optimizer) as parallel subagents
- Any other available tool that improves analysis quality

## Target Detection

Determine what to scan based on user input:
- **Specific file(s)** → scan those files
- **Project/folder** → scan all source files in that folder
- **Unclear** → ask the user what to scan

Detect language(s) automatically. If the project exists on BOTH macOS and Windows
(check for .swift + .cs files, or CLAUDE.md cross-platform references), note it:
"Dieses Projekt ist cross-platform (macOS + Windows) — ich pruefe plattformspezifische Issues."

## Project Size Scaling + Express-Modus

**Express-Modus** (Dateien unter 200 Zeilen ODER Benutzer sagt "schneller Check"):
Pre-Scan + 1 kombinierter Loop (Surface + Deep zusammen). Kein Architektur- oder Optik-Loop.
Spart 2/3 der Zeit. Ideal fuer Tampermonkey-Skripte, Configs, kleine Utilities.

**Klein (unter 10 Dateien):** Read all files. All 4 loops.
**Mittel (10-30 Dateien):** Entry points + core logic first. Secondary files on demand.
**Gross (30+ Dateien):** Focus on architecture and critical paths. Report skipped files.

Always tell the user: "Dein Projekt hat [N] Dateien — ich arbeite im [Express/Klein/Mittel/Gross]-Modus."

## Parallelization + Agent-Resilienz

**Klein/Express:** No subagents — analyze directly.

**Mittel/Gross (10+ files):** Spawn 4 parallel analysis agents (one per loop).
Each agent gets: full file list, language info, pre-scan results, loop instructions.
After all return: merge findings, deduplicate, renumber, present to user.

### Agent-Resilienz (KRITISCH)

Loop-Agents koennen abstuerzen. Der Gesamt-Scan darf NIEMALS haengenbleiben.

1. **Absturz erkennen**: Fehler-Output, kein Output, oder 5-Min-Timeout → GESCHEITERT.
2. **Weitermachen**: Andere Agents laufen weiter — ein gescheiterter blockiert NICHT den Rest.
3. **1x Neustart**: Gescheiterten Agent EINMAL neu starten mit kleinerem Scope.
4. **Bei erneutem Fehler**: Ueberspringen. Im Ergebnis vermerken: "Loop [N] konnte nicht ausgefuehrt werden."
5. **Dokumentieren**: Absturz in `.claude/agent-memory/shared/MEMORY.md` → Abschnitt "Offene Fehler & Probleme" eintragen.

**Faustregel**: Lieber 3 von 4 Loops als gar kein Ergebnis.

**Fixes** werden immer im Hauptgespraech angewandt (nicht in Subagents).

## Shared Knowledge Hub Integration

**Whiteboard**: `.claude/agent-memory/shared/MEMORY.md` (EINZIGE zentrale Wissensdatei)

**Lesen vor dem Scan**: Whiteboard lesen fuer bekannte offene Fehler, die beim jetzigen Scan-Ziel bereits dokumentiert wurden. So werden Doppelbefunde vermieden und der Fokus auf neue Probleme gelegt.

**Schreiben nach dem Scan**: Wenn wiederkehrende Muster oder bisher undokumentierte Probleme gefunden werden:
- **KRITISCHE Findings** (Kategorie Bug, Sicherheitsluecken) → SOFORT beim Entdecken in "Offene Fehler & Probleme" eintragen, OHNE auf User-Bestaetigung zu warten
- **Nicht-kritische Findings** (Verbesserung, Kreativ, Optik) → erst nach User-Bestaetigung eintragen
- Architekturerkenntnisse → in "Architektur-Entscheidungen" eintragen (nach User-Bestaetigung)

## Pre-Scan: Automated Tool Check

Run linters + LSP diagnostics before manual loops.

| Language | Linter / Tool | Command |
|----------|--------------|---------|
| Swift | swiftlint | `swiftlint lint --quiet [path]` |
| TypeScript/JS | Biome | `biome check [path]` |
| Kotlin | detekt / ktlint | `./gradlew detekt` or `ktlint "**/*.kt"` |
| Android | Android Lint | `./gradlew lint` |
| Go | golangci-lint | `golangci-lint run [path]` |
| Rust | cargo clippy | `cargo clippy -- -W warnings` |
| C# | dotnet format | `dotnet format --verify-no-changes [path]` |
| Python | ruff | `ruff check [path]` |
| Dart/Flutter | dart analyze | `dart analyze [path]` |

For standalone files without project setup: `biome check --no-errors-on-unmatched [file]`.

**LSP diagnostics**: Check each file for unused variables, type errors, deprecated APIs, unreachable code.

After: "Pre-Scan abgeschlossen — [N] automatische Findings. Starte Loop 1."

## The 4-Loop Process

**Load detailed loop checklists from**: [tool-check-ref/loops.md](tool-check-ref/loops.md)

Each loop collects findings into a shared list. Categories:
- **Bug** — Broken or could break (crash, data loss, wrong behavior)
- **Verbesserung** — Works but could be better (performance, security, maintainability)
- **Kreativ** — Different approach (new idea, better tool, architectural change)
- **Optik** — Visual/UI issue (inconsistency, missing states, polish)

**No duplicates across loops.** Single running counter (Loop 1: #1-#N, Loop 2: #N+1-#M, etc.)
Numbers in loops MUST match final presentation — NEVER renumber.

**Loop summaries:**
- Loop 1 → "Loop 1 abgeschlossen — [N] neue Findings (gesamt: [M]). Starte Loop 2."
- Loop 2 → "Loop 2 abgeschlossen — [N] neue Findings (gesamt: [M]). Starte Loop 3."
- Loop 3 → "Loop 3 abgeschlossen — [N] neue Findings (gesamt: [M]). Starte Loop 4."
- Loop 4 → "Alle 4 Loops abgeschlossen — [N] Findings insgesamt."

## Presentation

Present ALL findings as a single numbered list:

```
## Tool-Check Ergebnisse fuer [Projektname]

### Sprache(n): [z.B. Kotlin, C#]
### Dateien: [N] | Modus: [Express/Klein/Mittel/Gross]
### Findings: [N] total ([A] Auto, [M] Manuell)
### Agent-Status: [Alle OK / Loop X fehlgeschlagen — siehe Hinweis]

---

### Bugs (muessen gefixt werden)
1. [Datei:Zeile] — Beschreibung | Fix: ... | Risiko: ...

### Verbesserungen (sollte man machen)
N. [Datei:Zeile] — Beschreibung | Fix: ... | Vorteil: ...

### Optik & UX (sichtbare Qualitaet)
M. [Datei:Zeile] — Beschreibung | Umsetzung: ... | Vorher/Nachher: ...

### Kreative Ideen (koennte man machen)
K. [Datei] — Beschreibung | Umsetzung: ... | Aufwand: klein/mittel/gross

---
Welche Punkte soll ich umsetzen?
(z.B. "1, 2, 5" oder "alle Bugs" oder "alles ausser 12")
```

Each finding: exact file+line, `[Auto]` tag if from pre-scan, German description, concrete fix, why it matters.

## Applying Fixes

**Step 0: Confirmation table (MANDATORY).**
```
Verstanden — hier meine Zuordnung:
  Umsetzen:   1, 2, 3, 5, 7
  Erklaeren:  4, 6
  Weglassen:  8, 9, 10
Stimmt das?
```
Wait for confirmation. The user dictates via Whisper — numbers can be misheard.

Then:
1. Apply selected fixes one by one, visibly
2. Build after each thematic block (not after every single fix)
3. Bump version numbers if applicable
4. Run available tests/linters to verify
5. Commit and push
6. Present Fix-Bericht with "Erklaert" section for deferred items

## Meta-Improve (Skill Self-Improvement)

**Load meta-improve details from**: [tool-check-ref/meta-improve.md](tool-check-ref/meta-improve.md)

After every run: self-analysis, line count check, present 3 suggestions (NEVER auto-apply).

## Important Rules

- NEVER apply fixes without user approval — present findings first, wait for selection
- NEVER modify this skill without explicit user approval
- NEVER run automatically — manual trigger only
- NEVER delete code without showing what will be removed
- NEVER create new GitHub repos — all changes to `Pepsi1978/proggs`
- **Transparency**: Every finding includes file, line, description, fix, reasoning
- **Communication**: German with user, English for code/commits
- **Abbruch**: User says "genug"/"stop" → present findings so far, skip remaining loops
- **Agent-Resilienz**: Crashed agents get 1 retry then skip — never hang
- **Scope**: Only analyze what the user asked for. Don't expand without asking.
- **Security**: Critical vulns reported immediately, don't wait for all loops

---
<!-- Skill Version: v2.0 | Date: 2026-03-19 | Lines: ~230/600 (main) | Ref files: loops.md (~200), meta-improve.md (~80) | Total: ~510/2000 | Changes: v2.0 — Major rewrite: Progressive Disclosure (3 files), Loop 4 Optik & UX, Express-Modus, Agent-Resilienz, expanded linter table (Kotlin/Python/Android/Dart), cross-platform detection, early abort, more German triggers + duration in description. -->
