---
name: self-improve
description: Systematic self-improvement of the Claude Code development environment (~10-30 min, token-intensive). ONLY use when the user explicitly says "/self-improve", "verbessere dich", "optimiere deine Umgebung", "check dein Setup", "update alles", "mach mich besser", "aktualisiere alles", "pruef mein System", "System-Check", or "Umgebung pruefen". NEVER run this proactively or automatically — only on manual user request.
---

# Self-Improve v5.19 — Systematic + Creative + Intelligence Environment Optimization

**Before doing ANYTHING, show this overview in German:**

```
╔══════════════════════════════════════════════════════════════╗
║  Self-Improve Skill v5.19 — Deine Entwicklungsumgebung      ║
║  pruefen, aktualisieren, KREATIV ERFORSCHEN                  ║
║  Cross-Platform: macOS + Windows                             ║
╠══════════════════════════════════════════════════════════════╣
║  Stufe 1: SCAN — env-checker Agent (Full/Quick)              ║
║  Stufe 2: DEEP-DIVE — 8 Researcher + Smart-Cache (git-diff) ║
║  Stufe 3: IMPROVE — Updates + Report + Meta-Improve          ║
║  Stufe 4: CREATIVE — 6 Linsen + Benchmark + Build            ║
║  Stufe 5: SUPER INTELLIGENZ — Schlauer denken, nicht nur     ║
║           Fehler vermeiden                                    ║
║  Stufe 6: FEHLER-DAUERHAFTIGKEIT — Null Fehler, dauerhaft    ║
║  Danach: GitHub-Sync (Pepsi1978/proggs)                      ║
╚══════════════════════════════════════════════════════════════╝
```

**Geschaetzte Dauer:** Standard ~10-15 Min | Thorough ~20-30 Min | Focus ~25-35 Min
Wenn der Lauf nach 25 Minuten noch in Stufe 4+ ist: Warnung an den Benutzer —
"Der Lauf dauert ungewoehnlich lang. Soll ich restliche Stufen ueberspringen?"

## Zentrales Whiteboard — Nervensystem des Gesamtsystems (KRITISCH)

Das Whiteboard (`.claude/agent-memory/shared/MEMORY.md`) ist die EINZIGE zentrale Wissensdatei
fuer ALLE Komponenten des Claude Code Systems: Agents, Skills, Hooks, Plugins.
_MCP-Server koennen das Whiteboard nicht lesen (kein Dateisystem-Zugriff). Ihre Ergebnisse werden von den aufrufenden Agents ins Whiteboard geschrieben._

**Self-Improve ist der WICHTIGSTE Konsument dieses Whiteboards:**
1. **ZUERST lesen**: Vor jeder Aktion das Whiteboard komplett lesen
2. **Offene Fehler fixen**: Sektion "Offene Fehler & Probleme" — JEDER Eintrag mit Status OFFEN
   MUSS in diesem Lauf gefixt oder zumindest adressiert werden.
   Fehlerformat im Whiteboard: Quelle, Symptom, Ursache, Betroffene Dateien, Fix-Vorschlag, Status.
   Der "Fix-Vorschlag" sagt dir konkret was zu tun ist — folge ihm oder verbessere ihn.
3. **Benutzer-Feedback lesen**: `~/.claude/projects/*/memory/feedback_*.md` enthaelt
   direkte Korrekturen und Praeferenzen des Benutzers. Diese MUESSEN gelesen werden, weil:
   - `~/.claude/projects/*/memory/project_*.md` — aktiver Projektstatus und Blocker (vor Verbesserungen lesen!)
   - `~/.claude/projects/*/memory/MEMORY.md` — Memory-Index (zeigt alle verfuegbaren Memory-Dateien)
   - Feedback-Muster die systemweit gelten → als neue Regel oder Hook implementieren
   - Wiederholte Korrekturen → deuten auf fehlendes automatisches Verhalten hin
   - Auf BEIDEN Plattformen relevant: macOS hat eigenes Memory-Verzeichnis, aber der
     Inhalt muss funktional identisch wirken
4. **Claude-Mem Observations lesen**: `~/.claude/homunculus/projects/*/observations.jsonl`
   enthaelt Cross-Session-Wissen. Pruefen ob relevante Beobachtungen fuer aktuelle Verbesserungen
   nutzbar sind (z.B. wiederkehrende Probleme die ueber mehrere Sessions beobachtet wurden).
5. **Systemzustand aktualisieren**: Nach Aenderungen die Sektion "Systemzustand" updaten
6. **Erkenntnisse eintragen**: Alles was fuer andere Agents/Skills nuetzlich ist → reinschreiben
7. **Keine separaten Wissens-Dateien**: Es gibt NUR dieses eine Whiteboard fuer System-Wissen.
   Die Benutzer-Feedback-Memories sind die EINZIGE Ausnahme — sie sind an das Whiteboard
   angeschlossen (siehe Header) und werden von /self-improve mitgelesen.

**Whiteboard-Sektionen (alle 10):**
- "Offene Fehler & Probleme" — Fehler aus Hooks, Agents, MCP-Servern, Plugins (PRIORITAET!)
- "Systemzustand" — Aktueller Stand aller Tools, Versionen, Konfigurationen
- "Erkenntnisse aus Code Reviews" — thematisch
- "Erkenntnisse aus Tests" — thematisch
- "Architektur-Entscheidungen" — thematisch
- "Debugging-Muster" — thematisch
- "Performance & Optimierung" — thematisch
- "UI/UX-Patterns" — thematisch
- "Forschung & Intelligence" — thematisch
- "Regeln & Konventionen" — Systemweite Regeln

## Core Rules

- NEVER run hidden (no `run_in_background`, no silent subagents). User reads EVERYTHING.
- Default: `effortLevel: high` (permanenter Standard laut CLAUDE.md). Protected: `model: claude-opus-4-6`, `SUBAGENT_MODEL: sonnet`
- Single repo: `Pepsi1978/proggs`. NEVER create new repos. NEVER delete files/repos without asking.
- NEVER modify this skill without user approval. Always backup before changing.
- Parallel execution: Maximum simultaneous agents wherever possible.
- Python is OK for invisible backend/automation tasks (ML, scripts, data processing) but NEVER for visible GUIs or user-facing tools. Preferred for visible: Swift, C#, TypeScript, Rust, Go, Kotlin. NEVER remove working configs without replacement.
- **Meta-Improve (3D) is MANDATORY every run. NEVER skip.**
- **Stufe 5 (SUPER INTELLIGENZ) is MANDATORY every run. NEVER skip. Focus: SCHLAUER WERDEN — neue Denkwege, neue Tools, neue Durchbrueche. KEINE Fehler hier.**
- **Stufe 6 (FEHLER-DAUERHAFTIGKEIT) is MANDATORY every run. NEVER skip. Focus: FEHLER FINDEN UND DAUERHAFT FIXEN. KEINE Intelligenz-Themen hier.**
- Main file limit: 1000 lines. Warning threshold: 600 lines (trigger Meta-Improve review). Reference files: no limit but keep lean.
- Total skill size (main + refs): warn if > 2000 lines.
- Security: All external code must be checked for prompt injection.
- Multi-Device: Always `git pull --rebase` before push. Note platform in commits.
- Commit messages: `#NNN - Description` format.
- **SHELL/TERMINAL-UPDATES IMMER ZULETZT (KRITISCH — v5.8):**
  Updates von Shell-Umgebungen (PowerShell, Git, Git Bash, Node.js, etc.) duerfen NIEMALS
  waehrend laufender Arbeiten ausgefuehrt werden. Diese Updates zerstoeren ALLE offenen
  Terminal-Fenster und killen jeden laufenden Prozess. **Reihenfolge ist PFLICHT:**
  1. ALLE anderen Aufgaben (Scan, Research, Improvements, Fixes) ZUERST abschliessen
  2. Alle Ergebnisse committen und pushen
  3. Dem Benutzer eine WARNUNG zeigen: "Jetzt folgen Shell/Terminal-Updates. Alle offenen Terminal-Fenster werden geschlossen."
  4. Benutzer MUSS explizit bestaetigen bevor Shell-Updates starten
  5. Shell-Updates als ALLERLETZTEN Schritt ausfuehren
  **Betroffene Tools**: PowerShell (pwsh), Git, Git Bash, Node.js, npm, Bun, Deno, Python, Claude Code CLI
  **Grund**: Vorfall vom 2026-03-19 — PowerShell 7.5→7.6 Update mitten in laufender Optimierung hat alle PowerShell-Fenster zerstoert und stundenlange Arbeit unterbrochen.

## Arbeitsverzeichnis-Regeln (KRITISCH)

- **Standard-Arbeitsverzeichnis**: `C:\Users\barwa\proggs` (Windows) / `~/proggs` (macOS).
  Self-Improve arbeitet AUSSCHLIESSLICH in diesem Repository. Alle Dateien, Commits, Pushes
  gehen nach `Pepsi1978/proggs`.
- **VERBOTENES Verzeichnis**: `C:\Users\barwa\Codex` / `~/Codex` — dieses Verzeichnis gehoert
  AUSSCHLIESSLICH dem Codex-CLI-Tool. Claude Code darf dort NIEMALS lesen, schreiben oder
  in irgendeiner Form darauf zugreifen. Jeder Versuch ist ein schwerer Regelverstoss.
  Grund: Codex und Claude Code arbeiten parallel aber in getrennten Arbeitsbereichen.
  Jede Ueberschneidung kann zu Datenverlust und Konflikten fuehren.

## Thoroughness Detection

- **Standard**: "/self-improve" without extras → Stufe 1 quick, R1+R5+R6+R8 only (cached R2-R4)
- **Thorough**: "sehr ausfuehrlich", "full check" → Stufe 1 full, ALL 8 researchers (R1-R8)
- **Focus**: "Fokus [topic]" → Thorough + deep-dive into topic

### Modus-Vergleich (was laeuft wann)

| Stufe | Standard | Thorough | Focus |
|-------|----------|----------|-------|
| 0 Meta-Check | ✅ (wenn scores vorhanden) | ✅ | ✅ |
| 1 Scan | Quick | Full (inkl. Android, Agent-Tiers) | Full + Topic-Vertiefung |
| 2 Research | R1, R5, R6, R8 (R2-R4 gecacht) | R1-R8 (kein Cache) | R1-R8 + R7 (Focus-Topic) |
| 3 Improve | Top 5 Fehler fixen | Alle Fehler fixen | Alle + Topic-Vertiefung |
| 4 Creative | Minimum-Deliverables | Alle 6 Linsen | Alle 6 Linsen + Topic |
| 5 Intelligence | ✅ (immer) | ✅ (immer) | ✅ (immer) |
| 6 Durability | 6A, 6B, 6E (Quick) | 6A-6F (komplett) | 6A-6F (komplett) |
| Geschaetzte Dauer | ~10-15 Min | ~20-30 Min | ~25-35 Min |

## Platform Detection (FIRST STEP)

```bash
uname -s   # Darwin=macOS, MINGW*=Windows
```
Rule: Never `brew` on Windows, never `winget` on macOS.
Windows: Use `pwsh` for complex commands (write temp `.ps1` files).

## Stufe 0: META-CHECK (NEW v5.1)

**Schritt 0: Whiteboard komplett lesen und Zusammenfassung erstellen (PFLICHT vor allem anderen).**
Lese `.claude/agent-memory/shared/MEMORY.md` vollstaendig und erstelle eine kompakte Zusammenfassung:
- Anzahl offener Fehler (Status: OFFEN)
- Letzte Systemzustand-Aktualisierung
- Wichtigste Regeln & Konventionen
- Offene Forschungs-Findings (Status: UMZUSETZEN)
- **Compound Effect Tracker (PFLICHT seit v5.20):**
  - Zaehle die Eintraege in "Compound Effect Erfolge" → als Metrik ausgeben
  - Pruefen: Wurden seit dem letzten Lauf neue Compound Effects dokumentiert?
  - Wenn KEINE neuen: Warnung "Keine neuen Compound Effects — Selbstbeobachtung aktiv?"
  - Compound Effects pro Woche als Trend tracken (Ziel: steigend)
  - Pruefen ob Direktive #1 (Superintelligenz) und #2 (Selbstbeobachtung) noch in allen
    Speicherorten vorhanden sind (rules/, CLAUDE.md, Whiteboard, Memory)
Diese Zusammenfassung wird als Kontext an den evolution-analyst weitergegeben.

**Before env-checker**: Start `evolution-analyst` agent in parallel with Stufe 1.
The evolution-analyst reads `~/.claude/session-scores.jsonl` and `~/.claude/agent-memory/shared/MEMORY.md`
to identify quality trends and recurring weaknesses BEFORE the scan begins.
Pass the whiteboard summary (from Schritt 0) as additional context to the evolution-analyst prompt.

Its output feeds into Stufe 2: If quality is declining in a specific area (e.g., Rust builds),
the corresponding researcher gets a focused prompt. If capability gaps are detected,
R6 Creative is directed to search for solutions to those specific gaps.

If `session-scores.jsonl` doesn't exist or has < 3 entries: skip Stufe 0 silently.

**Score Plausibility Check (v5.3 — MANDATORY before trend analysis):**
1. Load last 5 entries from `session-scores.jsonl`
2. Check if `total_turns > 0` in at least 3 of 5 entries
3. Check if `tool_calls > 0` in at least 3 of 5 entries
4. If BOTH fail: **WARNING** — "Session-Scorer liefert Dummy-Daten!" Then:
   - Load newest non-agent transcript from `~/.claude/projects/*/` (UUID filename, not `agent-*`)
   - Parse first 5 JSONL lines and check if `entry.type` or `entry.role` format is used
   - If format mismatch: report as ❌ DEFECT and add to fix list
5. If plausible: proceed with trend analysis normally

## Stufe 1: SCAN

Delegate to `env-checker` agent: `mode: quick|full`, `Platform: [detected]`, `Date: [today]`.
Full mode includes: Android deep-scan, agent tiers, language readiness, rules, git config.
**Stufe 1 and 2 start SIMULTANEOUSLY after Stufe 0 completes.**

**Semantic Search Index (v5.14 — parallel mit Stufe 1):**
Wenn `~/proggs/mcp-code-search/` existiert: Pruefe ob der Index aktuell ist.
- Pruefe `~/proggs/.code-search/.last-index-time` — wenn aelter als 24h oder nicht vorhanden: Neu indexieren.
**Indexierung**: Wenn noetig, ausfuehren:
  ```bash
  bash ~/.claude/scripts/semantic-search-healthcheck.sh --reindex
  ```
- Im Report melden: "Semantic Index: [N] Dateien, [N] Chunks (aktualisiert/bereits aktuell)"
- **Voraussetzung**: Ollama muss laufen. Wenn nicht → ueberspringen mit Hinweis.

**Semantic Search Integritaetstest (v5.17 — PFLICHT bei jedem Lauf):**
Fuehre den externen Healthcheck aus:
```bash
bash ~/.claude/scripts/semantic-search-healthcheck.sh   # macOS/Linux
pwsh ~/.claude/scripts/semantic-search-healthcheck.ps1  # Windows
```
Prueft 4 Kategorien: Grundfunktion, Pointer-Konsistenz, Infrastruktur, Parallelitaets-Sicherheit.
- Alle OK → ✅ Melden
- WARN → ⚠️ Melden + Cleanup versuchen
- FAIL → ❌ Melden + Auto-Reparatur (Details im Skript-Output)
Jeden Fehler in MEMORY.md unter "Offene Fehler & Probleme" eintragen.

## Stufe 2: DEEP-DIVE

**Load researcher templates from**: [self-improve-ref/researchers.md](self-improve-ref/researchers.md)

Key changes (accumulated through v5.15):
- **8 researchers** (R1-R8). R6=Creative, R7=Focus Deep-Dive, R8=Intelligence Researcher (NEW v5.11).
- **Smart-Cache (v5.17 — git-diff-basiert, ersetzt TTL)**:
  Cache location: `~/.claude/self-improve-cache/` (NOT in shared memory — prevents contamination).
  **Invalidierung**: Pruefen ob sich Dateien unter `~/.claude/agents/`, `~/.claude/hooks/`,
  `~/.claude/rules/` oder `settings.json` seit dem letzten Cache-Zeitstempel geaendert haben.
  Wenn ja → betroffene Researcher (R2, R3, R4) neu starten. Wenn nein → gecachte Ergebnisse nutzen.
  Zeitstempel: `~/.claude/self-improve-cache/.last-cache-time`
  | Researcher | Cache-Regel | Grund |
  |------------|-------------|-------|
  | R1 Updates | IMMER frisch | Claude Code Releases aendern sich schnell |
  | R2 Plugins | Gecacht (env-diff) | Nur neu wenn Plugins/Settings sich geaendert haben |
  | R3 Parallel | Gecacht (env-diff) | Nur neu wenn Agents/Hooks sich geaendert haben |
  | R4 Versionen | Gecacht (env-diff) | Nur neu wenn Tool-Konfiguration sich geaendert hat |
  | R5 Security | IMMER frisch | Sicherheit darf nie gecacht werden |
  | R6 Creative | IMMER frisch | Kreativitaet braucht frische Ideen |
  | R7 Focus | NUR im Focus-Modus | Wird nur bei explizitem Fokus-Thema gestartet |
  | R8 Intelligence | IMMER frisch | Intelligenz-Forschung muss immer aktuell sein |
  **Thorough-Modus**: Ignoriert Cache komplett — alle Researcher laufen frisch.
  Details zur Implementierung: siehe [self-improve-ref/researchers.md](self-improve-ref/researchers.md)
- **R2 self-validates**: Runs `gh api repos/{owner}/{repo}` before recommending plugins.
- **Spawn ALL active researchers in ONE message block.**

### Researcher-Resilienz (v5.9 — KRITISCH)

Researcher-Agents koennen mit internen Fehlern abstuerzen. Der Gesamt-Lauf darf NIEMALS
dadurch haengenbleiben. Fuer JEDEN Researcher gilt:

1. **Absturz erkennen**: Wenn ein Agent einen Fehler zurueckgibt, keinen Output liefert,
   oder nach 5 Minuten nicht antwortet → als GESCHEITERT markieren.
2. **Sofort weitermachen**: Die anderen Researcher laufen weiter — ein gescheiterter Agent
   blockiert NICHT den Rest. Stufe 2 wartet nur auf die noch laufenden Agents.
3. **1x Neustart versuchen**: Den gescheiterten Researcher EINMAL neu starten mit
   kleinerem Scope (z.B. nur die wichtigste Teilfrage). Wenn auch der Neustart scheitert → ueberspringen.
4. **Fehler dokumentieren**: Jeden Absturz sofort ins Whiteboard eintragen (Sektion "Offene Fehler & Probleme" in `.claude/agent-memory/shared/MEMORY.md`):
   ```
   ### [Datum] Researcher R[N] Absturz
   - **Agent**: R[N] — [Name]
   - **Fehler**: [Fehlermeldung oder "kein Output nach Timeout"]
   - **Neustart**: [Erfolgreich / Gescheitert]
   - **Auswirkung**: [Was fehlt im Report wegen dem Absturz]
   ```
5. **Im Final Summary nennen**: Unter "Probleme waehrend des Laufs" jeden Absturz auflisten
   mit Empfehlung, ob der Researcher beim naechsten Lauf manuell getriggert werden sollte.
6. **Ursachenanalyse**: Nach Abschluss aller Stufen die MEMORY.md unter "Offene Fehler & Probleme"-Eintraege pruefen —
   wenn ein Researcher 2+ Mal abgestuerzt ist, dessen Prompt vereinfachen oder aufteilen.

**Faustregel**: Lieber ein Researcher-Ergebnis weniger als ein komplett steckengebliebener Lauf.

### R8: Intelligence Researcher (Stufe 2 — parallel mit R1-R7)
**Stufe 2 verwendet den dedizierten `intelligence-researcher` Agent** statt eines generischen
Researcher-Prompts. Der Agent hat:
- Findings werden in MEMORY.md unter "Forschung & Intelligence" gespeichert, mit Status pro Finding
- Liest vorherige Findings und sucht gezielt NEUE Ideen (kein Duplicate Research)
- Output-Format mit expliziter JA/NEIN-Empfehlung pro Finding
Spawn agent via Agent tool: type=intelligence-researcher, name=r8-intelligence

**Gueltige Status-Werte fuer Forschungs-Findings:**
`UMZUSETZEN` (jetzt implementieren) | `EVALUIERT` (bewertet, noch nicht entschieden) | `VERWORFEN` (abgelehnt) | `BESTAETIGT` (bereits umgesetzt).
**NIEMALS `OFFEN` in der Forschungs-Sektion verwenden** — `OFFEN` gehoert ausschliesslich in "Offene Fehler & Probleme".

After researchers return: **mandatory cross-validation table** (Researcher claims vs. scan data).
For plugin recommendations: **mandatory security review** (spawn researcher per plugin).

Apply validated updates: brew/winget/pkg, rustup, dotnet, plugins, settings drift.
**ACHTUNG**: Shell/Terminal-Updates werden hier NICHT ausgefuehrt — siehe "Deferred Shell Updates" Sektion unten.
Commit if files changed.

## Stufe 3: IMPROVE

**Load report template + creative research details from**: [self-improve-ref/report-and-creative.md](self-improve-ref/report-and-creative.md)

### 3A: Full Knowledge Integration (UPGRADED v5.15 — Single Whiteboard)
**Before improvements**: Read the SINGLE central whiteboard:
`.claude/agent-memory/shared/MEMORY.md` — contains ALL knowledge: errors, system state,
agent insights, conventions, research findings. There are NO other knowledge files.

**Analyze "Offene Fehler & Probleme" section**: Count errors by source. Fix ALL with Status OFFEN.
If any error type has 3+ entries → propose a preventive hook or rule.
**Analyze "Systemzustand" section**: Is it still current? Update if environment changed.

**PFLICHT — Fix the TOP 5 most critical open issues from knowledge files:**
Prioritize by severity: security issues > agent crashes > broken scorers > stale data > cosmetic.
For each of the top 5: fix it in this run. This is not optional.
**After each fix**: Update the Status field from `OFFEN` to `GEFIXT (today's date)`. Only then move on to the next error.
**Remaining issues**: List ALL additional unfixed problems at the end of the report under
"Offene Probleme (nicht in diesem Lauf gefixt)" so the user can decide which to tackle next.

**After improvements**: Update "Systemzustand" in MEMORY.md with new environment knowledge.

### 3B: Creative Improvements + Challenger Review (UPGRADED v5.4)
Implement at least 1 concrete improvement per run: new hooks, agents, rules, configs.
Use the `quality-gate` agent for quality checks instead of 3 separate agents.

**Challenger Gate (NEW v5.4 — MANDATORY):**
After planning improvements but BEFORE implementing them, spawn the `challenger` agent:
- Feed it the list of planned improvements
- The challenger must find at least 1 weakness per improvement
- Address each weakness before implementing
This prevents the self-improve skill from making changes that make things worse.

### 3C: Structured Report + Entscheidungsliste (PFLICHT — v5.12)
Use the template from reference file. Includes new **Performance-Benchmark** section.
Add a "Shared Knowledge" section showing what code-reviewer learned and what self-improve contributed.

**ENTSCHEIDUNGSLISTE (v5.12 — PFLICHT am Ende jedes Reports):**
ALLE Findings aus ALLEN Stufen muessen am Ende als eine einzige Entscheidungsliste fuer den
Benutzer zusammengefasst werden. Der Benutzer ist KEIN Programmierer und will nicht selbst
aus 50 Findings herausfiltern was wichtig ist. Format:

```markdown
## Entscheidungsliste — Was soll umgesetzt werden?

### A) Sofort umsetzbar (kann ich JETZT machen)
| # | Was genau? | Aufwand | Was bringt es dir? | Empfehlung | Ja/Nein? |
|---|-----------|---------|-------------------|------------|----------|
| A1 | [Konkreter Vorschlag] | [5 Min / 30 Min] | [Nutzen in 1 Satz] | JA/NEIN + Grund | |

### B) Mittlerer Aufwand (10-60 Min)
[Gleiche Spalten]

### C) Strategisch (betrifft wie das System langfristig arbeitet)
[Gleiche Spalten]

### D) Nicht empfohlen (mit Begruendung warum NICHT)
| # | Was | Warum NICHT |
```

**REGELN fuer die Entscheidungsliste:**
- JEDER Researcher-Fund, JEDE Verbesserung, JEDER Fix muss in genau EINER Zeile landen
- "Was genau?" muss fuer einen Nicht-Programmierer verstaendlich sein
- "Was bringt es dir?" erklaert den konkreten Nutzen, nicht das technische Detail
- "Empfehlung" ist IMMER JA oder NEIN mit kurzem Grund
- Benutzer antwortet nur: "Mach A1, A3, B2" — das ist alles was er sagen muss
- KEINE reinen Informationstabellen ohne Handlungsaufforderung
- KEINE Findings die nur "zur Info" praesentiert werden — entweder Handlung oder "Nicht empfohlen"

### 3D: Meta-Improve (MANDATORY)
Present 3 suggestions for THIS SKILL. Apply only after user approval. Backup first.

## Stufe 4: CREATIVE RESEARCH

**Details in**: [self-improve-ref/report-and-creative.md](self-improve-ref/report-and-creative.md)

Core principle: **DISCOVER → EXPLAIN → BUILD → TEACH.**
Non-Programmer Rule: Plain German, analogies, concrete scenarios.
6 Thinking Lenses + Performance Benchmark Lens (NEW v5.0).

Die 6+1 Denk-Linsen:
1. **Biologische Linse** — Wie loest die Natur dieses Problem? (Schwarmintelligenz, Evolution, Immunsystem)
2. **Oekonmische Linse** — Was kostet es? Was spart es? ROI-Rechnung.
3. **Sicherheits-Linse** — Was kann schiefgehen? Worst-Case-Analyse.
4. **Benutzer-Linse** — Was wuerde der Benutzer davon halten? Einfacher? Schneller?
5. **Zukunfts-Linse** — Wo steht dieses System in 6 Monaten? Skalierbarkeit.
6. **Eleganz-Linse** — Gibt es eine einfachere Loesung? Weniger Teile, weniger Komplexität.
7. **Performance-Linse** (Benchmark) — Messbare Verbesserung: tokens, Zeit, Qualitaet.

**Performance Research (The Benchmark — NEW v5.0):**
Goal: **Maximum quality at minimum token cost at maximum speed.**
- Measure build times, agent token consumption, cache effectiveness, startup time
- Track metrics across runs to identify trends
- Optimize the ratio: quality / (tokens * time)

**Minimum deliverables per run:**
- 1 new artifact (hook/skill/agent/config)
- 3 Handlungsvorschlaege for environment/workflow
- Kreativitaets-Reflexion section
- Knowledge Integration table (available vs. used features)

## Stufe 5: SUPER INTELLIGENZ (PFLICHT — NIEMALS UEBERSPRINGEN)

**Diese Stufe laeuft bei JEDEM Durchlauf — egal ob Standard, Thorough oder Focus.**
**Ziel: Claude zum BESTEN AI-Programmierer der Welt machen. SUPER INTELLIGENZ.**
**Hier geht es NUR um SCHLAUER WERDEN — nicht um Fehler fixen (das ist Stufe 6).**

### 5A: Kognitive Werkzeuge (Intelligenz-Multiplikatoren)
1. **Researcher spawnen** _(Nutzt R8-Ergebnisse statt eigenem Researcher-Spawn — siehe 5C)_: "Welche neuen MCP-Server, Claude-Code-Plugins oder CLI-Tools wurden in den letzten 30 Tagen veroeffentlicht die das DENKEN verbessern?" (Wissensgraphen, Verifikation, Semantic Search, Code-Analyse, Formal Verification, Codebase-Embeddings)
2. **Bewerten**: Wuerde dieses Tool Claude bei ECHTEN Aufgaben schlauer machen? (Nicht nur cool, sondern nuetzlich)
3. **1 Tool pro Lauf installieren oder testen** — wenn es den Qualitaets-Check besteht
4. Neue Tools in MEMORY.md dokumentieren: Was es tut, wann es hilft, wie es getriggert wird

### 5B: Neue Denkmuster (Kognitive Durchbrueche)
Recherchiere und implementiere neue Denkstrategien die Claude FUNDAMENTAL schlauer machen:
1. **Tree-of-Thought**: Bei komplexen Entscheidungen mehrere Loesungspfade parallel explorieren
2. **Chain-of-Verification**: Nach jedem Code-Output automatisch Verifikationsfragen stellen
3. **Reflexion-Loops**: Nach jedem Fehler Meta-Analyse: "Welches Denkmuster war fehlerhaft?"
4. **Multi-Agent-Debate**: Bei wichtigen Entscheidungen `challenger` gegen `architect` antreten lassen
5. **Competitive Coding**: 2 `coder`-Agents bauen verschiedene Implementierungen, `judge` waehlt Gewinner
6. **Self-Play**: Gleiches Problem zweimal mit verschiedenen Ansaetzen loesen und vergleichen
**REGEL**: Mindestens 1 neues Denkmuster pro Lauf TESTEN — nicht nur darueber reden.

### 5C: Intelligence-Researcher-Ergebnisse umsetzen (PFLICHT — KEIN eigener Researcher mehr!)

**ACHTUNG v5.11**: Stufe 5C spawnt KEINEN eigenen Researcher mehr. Stattdessen nutzt sie die
Ergebnisse von **R8 (Intelligence Researcher)**, der bereits in Stufe 2 parallel mitgelaufen ist.

1. **R8-Ergebnisse lesen**: Die 5+ Findings des Intelligence Researchers auswerten
2. **Priorisieren**: Welche Findings sind SOFORT umsetzbar vs. langfristig?
3. **Mindestens 1 Finding SOFORT umsetzen** — nicht "spaeter", JETZT. (z.B. neues Tool installieren, neuen Agent-Prompt verbessern, neues Denkmuster als Rule implementieren)
4. **Die anderen Findings dem Benutzer praesentieren** und in MEMORY.md unter "Forschung & Intelligence" speichern.
   Gueltige Status-Werte: `UMZUSETZEN` | `EVALUIERT` | `VERWORFEN` | `BESTAETIGT`. **NIEMALS `OFFEN`** in dieser Sektion.
5. **Kompetitive Erkenntnisse** (was Konkurrenz besser macht) als Prioritaet behandeln — hier ist der groesste Hebel

**REGEL**: Wenn R8 abgestuerzt ist (Researcher-Resilienz, v5.9): Stufe 5C darf EINMAL einen
Ersatz-Researcher spawnen mit kleinerem Scope (nur Dimensionen 2+3: Kognitive Werkzeuge + Kompetitive Analyse).
Das ist der EINZIGE Fall in dem 5C noch einen eigenen Researcher startet.

### 5D: Wissensquellen erschliessen (Neue Informationsquellen)
1. **Codebase-Semantik**: Gibt es einen MCP-Server der die Codebase semantisch indexiert? (z.B. Zilliz claude-context, CodeGraphContext) → Testen und ggf. installieren
2. **Doku-Indizierung**: Koennen haeufig genutzte APIs/Frameworks automatisch indiziert werden?
3. **Akademische Quellen**: MCP-Server fuer arxiv, ACM, IEEE — Zugang zu neuester Forschung?
4. **Erfahrungswissen**: Kann episodic-memory besser genutzt werden? Werden vergangene Loesungen gefunden?
**REGEL**: Pro Lauf mindestens 1 neue Wissensquelle bewerten. Wenn nuetzlich → einbinden.

### 5E: Intelligenz-Score (Fortschritt messen)
Am Ende jedes Laufs: Berechne und zeige den **IQ-Score** (Intelligence Quotient der Umgebung):

| Dimension | Max | Messung (Qualitaets-gewichtet) |
|-----------|-----|-------------------------------|
| Praevention | 20 | Fehler mit [OK] DAUERHAFT Prevention = 2 Pkt, mit [!] FRAGIL = 1 Pkt, ohne = 0 |
| Gedaechtnis | 20 | Abschnitte in MEMORY.md die in letzten 30 Tagen aktualisiert wurden = 2 Pkt/Abschnitt |
| Konventionen | 20 | Regeln in "Regeln & Konventionen" die in letzten 5 Sessions angewandt wurden = 2 Pkt/Regel |
| Regeln | 20 | Rules die in letzten 5 Sessions relevant waren = 3 Pkt, nie relevant = 1 Pkt |
| Werkzeuge | 20 | Kognitive Tools die tatsaechlich GENUTZT werden = 3 Pkt, nur installiert = 1 Pkt |
| **Gesamt** | **100** | **Summe — Qualitaet zaehlt mehr als Quantitaet** |

Zeige: `IQ-Score: XX/100 (vorher: YY → Veraenderung: +/-Z)`
**Baseline lesen**: Den letzten `iq_score`-Wert aus `session-scores.jsonl` lesen.
Falls kein `iq_score`-Feld vorhanden: "Erstmessung (kein Vergleichswert)".
Speichere in `session-scores.jsonl` als neues Feld `iq_score`.

## Stufe 6: FEHLER-DAUERHAFTIGKEIT (PFLICHT — NIEMALS UEBERSPRINGEN)

**Ziel: NULL FEHLER. Jeden Bug finden, dauerhaft eliminieren, verhindern dass er wiederkommt.**
**Hier geht es NUR um FEHLER — nicht um Intelligenz oder Features (das ist Stufe 5).**

**Aufteilung (v5.13):**
- **IMMER (Quick-Check):** 6A (Aktive Fehlersuche), 6B (Fehler-Muster → Praevention), 6E (Dauerhaftigkeits-Pruefung)
- **NUR Thorough-Modus:** 6C (Korrektur-Analyse), 6D (Regel-/Agent-Audit), 6F (Lern-Extraktion)
Im Standard-Modus werden 6C/6D/6F uebersprungen — sie sind analyse-intensiv und bringen nur bei
ausfuehrlichen Laeufen genug Mehrwert.

### 6A: Aktive Fehlersuche (3 parallele Scans)
Spawne 3 Agents parallel — jeder sucht an einer anderen Stelle nach Problemen:
1. **Whiteboard-Scanner**: Lies `.claude/agent-memory/shared/MEMORY.md` komplett → Finde ALLE unfixten Probleme (Status: OFFEN), veraltete Eintraege, Widersprueche. Jedes Problem MUSS in diesem Lauf gefixt werden.
2. **Umgebungs-Scanner**: Pruefe Hooks (laufen sie?), Agents (stimmen Prompts?), Rules (sind sie aktuell?), Settings (Drift?), session-scorer (liefert er echte Daten?). Finde AKTUELLE Fehler die noch niemand bemerkt hat.
3. **Zukunfts-Scanner**: Analysiere die letzten 3 Aenderungen in MEMORY.md unter "Offene Fehler & Probleme" — welche NEUEN Fehlertypen koennten in Zukunft auftreten? Fuer jeden potenziellen Fehler: Praeventiven Hook oder Rule vorschlagen.

### 6B: Fehler-Muster → Praevention (Aus vergangenen Fehlern lernen)
1. Lies MEMORY.md unter "Offene Fehler & Probleme": Zaehle Fehler pro Kategorie
2. Finde den haeufigsten Fehlertyp der NOCH KEINEN praeventiven Hook/Rule hat
3. **BAUE den Hook oder die Rule** — nicht vorschlagen, BAUEN. Jeder Lauf = 1 neuer Schutz.
4. Trage den neuen Schutz in MEMORY.md unter "Offene Fehler & Probleme" als "Prevention" beim entsprechenden Fehler ein

### 6C: Korrektur-Analyse (Aus Benutzer-Korrekturen lernen)
1. Lies `session-scores.jsonl`: Finde Sessions mit corrections > 3
2. Analysiere die session-scores Daten — Transkripte sind zu gross zum Laden, nutze stattdessen die Score-Metadaten (corrections, total_turns, tool_calls) um Muster zu erkennen
3. Extrahiere die 3 haeufigsten Korrektur-TYPEN (z.B. "falsches Ziel", "ungefragt geaendert", "falsche Datei")
4. Fuer jeden Typ: Pruefe ob eine Rule existiert die das verhindert. Wenn nicht → Rule erstellen.
5. Trage Erkenntnisse in MEMORY.md unter "Debugging-Muster" ein

### 6D: Regel- und Agent-Audit (Sind Schutzmassnahmen wirksam?)
1. Lies ALLE Dateien in `~/.claude/rules/` — zaehle Regeln
2. Fuer jede Regel pruefen: Wurde sie in den letzten 5 Sessions jemals relevant?
3. **Veraltete Regeln entfernen** (nach Bestaetigung) — tote Regeln belasten den Kontext
4. **Fehlende Regeln identifizieren**: Gibt es wiederkehrende Probleme in MEMORY.md unter "Offene Fehler & Probleme" fuer die KEINE Regel existiert?
5. Agent-Prompt-Audit: Pruefe ob Senior-Agents optimale Prompts haben. Wiederholte schlechte Ergebnisse → Prompt verbessern.

### 6E: Dauerhaftigkeits-Pruefung (Fixes die HALTEN)
1. Pruefe die letzten 5 Fixes in MEMORY.md unter "Offene Fehler & Probleme": Ist der Fix noch wirksam? (Hook existiert? Rule geladen? Config unveraendert?)
2. Wenn ein Fix nicht mehr wirkt: **Sofort reparieren** und Ursache dokumentieren
3. **REGEL**: Jeder Fix muss DAUERHAFT sein — keine temporaeren Workarounds.
4. Trage Dauerhaftigkeits-Status in MEMORY.md unter "Offene Fehler & Probleme" ein: `[OK] DAUERHAFT` oder `[!] FRAGIL (Grund)`
5. **Archivierung alter GEFIXT-Eintraege**: Pruefe ALLE Eintraege mit Status `GEFIXT` in "Offene Fehler & Probleme".
6. **Cross-Platform FUNKTIONALE Paritaet (v5.20 — PFLICHT):**
   Fuer JEDES Hook-Paar (.sh + .ps1) pruefen: Tun beide das GLEICHE?
   - Gleiche Whiteboard-Writes? Gleiche Outputs? Gleiche Seiteneffekte?
   - Nicht nur "existiert die Datei" — sondern "macht sie dasselbe"
   - Wenn NICHT: Als OFFEN ins Whiteboard loggen mit Fix-Vorschlag
   - Hintergrund: pending-admin-updates hatte monatelang unterschiedliches Verhalten
     auf macOS (Whiteboard-Write) vs Windows (nur Desktop-Script) ohne dass es auffiel.
   Eintraege die aelter als 30 Tage sind: In einen `<!-- ARCHIV (Datum): Zusammenfassung -->` Kommentarblock
   am Anfang der Sektion verschieben. So bleibt die aktive Liste uebersichtlich.

### 6F: Lern-Extraktion (Was lehrt uns jeder Fehler?)
Fuer JEDEN neuen Fehler der in diesem Lauf gefunden wurde:
1. **Root Cause**: Warum ist er passiert? (Nicht das Symptom, die URSACHE)
2. **Systemische Lektion**: Was sagt dieser Fehler ueber das SYSTEM?
3. **Praevention**: Konkreter Hook, Rule oder Agent-Aenderung die diesen Fehlertyp FUER IMMER verhindert
4. Trage Lektion in MEMORY.md unter "Regeln & Konventionen" ein (als neue Regel)

## Focus Mode

Focus adds depth, doesn't skip standard checks. **Stufe 5+6 laufen IMMER — auch im Focus-Modus.**
**Built-in focus topics**: Rust, Android, Security, TypeScript, Performance, Cross-Platform, Self-Improve.
For any focus: After checklist-based checks, explore BEYOND the checklist.

## Cross-Platform Sync (PFLICHT — v5.1)

**REGEL: Jede Verbesserung muss plattformuebergreifend wirksam werden.**
Alle Aenderungen die /self-improve macht — neue Hooks, Agents, Skills, Configs, Settings —
muessen bei GitHub hinterlegt werden, damit andere Plattformen (Windows, macOS) beim
naechsten Session-Start automatisch die neuesten Verbesserungen erhalten.

**Was synchronisiert werden muss:**
1. Hook-Scripts (*.ps1 + *.sh + *.ts) → `~/proggs/claude-code-setup/hooks/`
2. Agent-Definitionen (*.md) → `~/proggs/claude-code-setup/agents/`
3. Commands + Unterordner → `~/proggs/claude-code-setup/commands/` (inkl. self-improve-ref/)
4. Rules → `~/proggs/claude-code-setup/rules/`
5. CLAUDE.md → `~/proggs/CLAUDE.md`
6. **settings-reference.json** → `~/proggs/claude-code-setup/settings-reference.json`
   (Hooks, Plugins, Marketplaces, Env — wird von auto-sync in settings.json gemerged)
7. **Knowledge-Dateien** → `~/proggs/claude-code-setup/agent-memory/shared/`
   (MEMORY.md — das einzige zentrale Whiteboard, plattformuebergreifendes Wissen)

**Was NICHT synchronisiert wird (maschinenspezifisch):**
- settings.json direkt (Permissions, LSP-Pfade sind plattformabhaengig)
- Plugin-Caches (~/.claude/plugins/cache/)
- Session-Transcripts, session-scores.jsonl

**Sync ausfuehren:**
```bash
bash ~/.claude/scripts/self-improve-sync.sh "#NNN - Self-improve sync"   # macOS/Linux
pwsh ~/.claude/scripts/self-improve-sync.ps1 "#NNN - Self-improve sync"  # Windows
```
Falls die Sync-Skripte nicht existieren: Manuell kopieren (cp-Befehle oben als Fallback).

## Deferred Shell Updates (NACH allem anderen — v5.8)

**Dieser Schritt kommt NACH dem Final Summary und NACH dem GitHub-Sync.**
Wenn in Stufe 2 Shell/Terminal-Updates identifiziert wurden:
1. Zeige dem Benutzer die Liste der ausstehenden Shell-Updates
2. Erklaere: "Diese Updates starten Shell-Prozesse neu. Alle offenen Terminal-Fenster werden geschlossen."
3. **WARTE auf explizite Bestaetigung** des Benutzers (AskUserQuestion verwenden)
4. Erst nach Bestaetigung: Updates einzeln ausfuehren, jeweils mit Statusmeldung
5. Nach jedem Update: Pruefen ob die neue Version korrekt installiert ist

**Wenn der Benutzer ablehnt**: Updates in MEMORY.md unter "Offene Fehler & Probleme" eintragen (mit Status: DEFERRED).
**NIEMALS Shell-Updates ohne Bestaetigung ausfuehren. NIEMALS waehrend andere Aufgaben laufen.**

## Final Summary

Always end with: Change counter, Gesamtstatus, GitHub sync, offene Punkte,
naechster empfohlener Lauf, Commit/Push-Status als LETZTER Satz.

**Session-Score-Trend (NEW v5.1):** Read `~/.claude/session-scores.jsonl`, compute 5-session
moving average, compare to previous period. Display as: `Evolution: X.X → Y.Y (↑/↓/→)`.
If < 5 entries: show "Evolution: Noch zu wenig Daten (N/5 Sessions)".

---
<!-- Skill Version: v5.19 | Date: 2026-03-21 | Changes: v5.19 — Restructured: extracted bash scripts, added mode comparison table, consolidated rules, added workspace/timeout constraints. Previous: v5.18 — Removed all Termux references (Termux deinstalled, no longer a supported platform). -->
