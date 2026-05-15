# Self-Improve: Researcher Templates (v5.19)

## Research Caching (v5.12 — Git-Diff-basiert statt TTL)

**Neue Logik**: Cache wird NICHT nach fester Zeit invalidiert, sondern wenn sich die Umgebung
tatsaechlich geaendert hat. Das spart Token bei stabilen Umgebungen und garantiert frische
Daten nach echten Aenderungen.

```bash
# Pruefe ob sich ~/.Gemini/ seit dem letzten Cache-Zeitstempel geaendert hat:
CACHE_DIR="$HOME/.Gemini/self-improve-cache"
CACHE_STAMP="$CACHE_DIR/.last-cache-time"

if [ -f "$CACHE_STAMP" ]; then
  LAST_CACHE=$(cat "$CACHE_STAMP")
  # Pruefe Aenderungen an Agents, Hooks, Settings seit letztem Cache
  CHANGES=$(find "$HOME/.Gemini/agents" "$HOME/.Gemini/hooks" "$HOME/.Gemini/rules" \
    -newer "$CACHE_STAMP" -type f 2>/dev/null | wc -l)
  SETTINGS_CHANGED=$(find "$HOME/.Gemini/settings.json" -newer "$CACHE_STAMP" 2>/dev/null | wc -l)
  TOTAL=$((CHANGES + SETTINGS_CHANGED))

  if [ "$TOTAL" -gt 0 ]; then
    echo "CACHE INVALIDIERT: $TOTAL Dateien geaendert seit letztem Lauf"
    # Alle Researcher laufen frisch
  else
    echo "CACHE GUELTIG: Keine Umgebungsaenderungen — R2, R3, R4 aus Cache"
    # Nur R1, R5, R6, R8 laufen (immer frisch)
  fi
else
  echo "KEIN CACHE: Erster Lauf — alle Researcher starten"
fi

# Nach dem Lauf: Zeitstempel aktualisieren
touch "$CACHE_STAMP"
```

**Immer frisch** (unabhaengig vom Cache): R1 (Updates), R5 (Security), R6 (Creative), R8 (Intelligence)
**Cache-gesteuert**: R2 (Plugins), R3 (Parallel), R4 (Versionen)
**Absolute Untergrenze**: Auch bei gueltigem Cache: Alle 14 Tage ALLE Researcher frisch laufen lassen.
**Thorough-Modus**: Ignoriert Cache komplett — alle Researcher laufen immer frisch.

When a run completes, save a snapshot to the cache directory (`~/.Gemini/self-improve-cache/`):
- Write R2, R3, R4 summaries as markdown files (e.g., `R2_plugins.md`, `R3_parallel.md`, `R4_versions.md`)
- Update the timestamp: `touch ~/.Gemini/self-improve-cache/.last-cache-time`
- Do NOT use TTL-based `type: reference` memory entries — these contradict the git-diff cache strategy.

## Researcher Robustness Preamble (v5.11 — PFLICHT fuer JEDEN Researcher)

**JEDER Researcher-Prompt MUSS mit diesem Preamble beginnen.** Kopiere diesen Block
an den Anfang jedes R1-R8 Prompts bevor du ihn an den Sub-Agent schickst:

```
ROBUSTNESS RULES (befolge diese IMMER, sie haben Vorrang vor allem anderen):
1. WEBFETCH-SCHUTZ: Lade NIE eine Seite komplett wenn sie >500 Zeilen hat. Nutze head_limit oder lies nur die ersten 200 Zeilen. Wenn WebFetch fehlschlaegt: EINMAL mit anderer URL wiederholen, dann aufgeben und mit vorhandenen Daten arbeiten.
2. WEBSEARCH-SCHUTZ: Maximal 5 Suchen. Wenn 3 Suchen hintereinander keine brauchbaren Ergebnisse liefern: SOFORT aufhoeren und mit vorhandenen Daten Ergebnis zurueckgeben.
3. KONTEXT-SCHUTZ: Halte deine Antwort unter 300 Zeilen. Fasse Ergebnisse zusammen statt alles zu zitieren. Nur die wichtigsten Fakten, Links und Empfehlungen.
4. FEHLER-BEHANDLUNG: Wenn ein Tool fehlschlaegt: Fehler notieren, alternatives Tool oder alternative Query versuchen. Nach 2 Fehlversuchen: Weitermachen mit dem was du hast. NIEMALS in einer Retry-Schleife haengen bleiben.
5. SELBST-TERMINIERUNG: Wenn du nach 5 Tool-Aufrufen keine neuen Erkenntnisse gewinnst: SOFORT Ergebnis zurueckgeben mit dem Status "TEILWEISE — [was fehlt]". Ein unvollstaendiges Ergebnis ist IMMER besser als ein Absturz.
6. ANTWORT-PFLICHT: Du MUSST IMMER eine Antwort zurueckgeben. Auch wenn leer. Auch wenn fehlerhaft. NIEMALS still haengen bleiben.
```

## Researcher Output Format (v5.23 — PFLICHT fuer ALLE Researcher)

**JEDER Researcher MUSS seine Ergebnisse als JSON-Array zurueckgeben.** Freitext-Antworten
werden nicht akzeptiert. Der Haupt-Gemini parst das JSON und baut daraus automatisch die
Entscheidungsliste. Das verhindert dass Findings vergessen oder falsch zusammengefasst werden.

**Format (PFLICHT — an den Schluss jedes Researcher-Prompts anhaengen):**

```
AUSGABE-FORMAT (PFLICHT — du MUSST dieses Format verwenden):

Schreibe am Ende deiner Antwort einen JSON-Block zwischen ```json und ``` Markern.
Jedes Finding ist ein Objekt im Array:

```json
[
  {
    "id": "R1-1",
    "titel": "Verstaendlicher Titel fuer Nicht-Programmierer",
    "kategorie": "update|sicherheit|intelligenz|plattform|kreativ",
    "problem": "Was ist das Problem das geloest wird? 2-3 Saetze in einfachem Deutsch.",
    "loesung": "Was genau passiert wenn man JA sagt? 2-3 Saetze, konkret.",
    "aufwand": "5 Min|15 Min|30 Min|1 Std|1 Tag",
    "empfehlung": "JA sofort|JA spaeter|NEIN",
    "quelle": "URL oder Paper-Referenz"
  }
]
```

Kategorien:
- "update" = Software-Updates, neue Versionen
- "sicherheit" = CVEs, Vulnerabilities, Supply-Chain-Risiken
- "intelligenz" = Neue Denkwege, kognitive Werkzeuge, Forschungs-Findings
- "plattform" = Hooks, Regeln, Konfiguration, Cross-Platform
- "kreativ" = Unkonventionelle Ideen, neue Ansaetze

REGELN:
- "titel" muss fuer einen Nicht-Programmierer verstaendlich sein
- "problem" und "loesung" muessen jeweils MINDESTENS 2 Saetze lang sein
- Fachbegriffe in Klammern erklaeren: "MCP-Server (ein Zusatz-Werkzeug)"
- KEIN Finding ohne alle 7 Felder
```

**Fuer den Haupt-Gemini (Parsing):** Nach Rueckkehr jedes Researchers den ```json-Block
extrahieren und in eine Gesamtliste zusammenfuehren. Doppelte Findings (gleicher Titel)
entfernen. Die Gesamtliste wird zur Entscheidungsliste im Report.

**JSON-Cache (M3, NEU 2026-05-10 — DGM-Vorbereitung):**
Zusaetzlich zum Markdown-Cache (R*_*.md) wird der JSON-Block jedes Researchers als
strukturierte Datei in `~/.Gemini/self-improve-cache/R*_findings.json` gespeichert.
Format:
```json
{
  "researcher": "R1",
  "run_date": "2026-05-10",
  "findings": [...],  // array of findings als JSON-Objekte
  "session_id": "abc123"  // fuer DGM-Strategie-Verkettung
}
```
Vorteil: Naechster /self-improve-Lauf kann diese Dateien maschinenlesbar einlesen
und Trends erkennen ("Welche Findings hatten in letzten 3 Laeufen Empfehlung JA aber
sind noch nicht umgesetzt?"). Das ist die Datengrundlage fuer den DGM-Pattern (I7) —
Selbstmodifikation auf Basis vorheriger Erfolge.

**Schreibweise (Bash):**
```bash
# Im /self-improve Skill nach jedem Researcher:
echo "$json_block" | jq '.' > ~/.Gemini/self-improve-cache/R${N}_findings.json
```

**Schreibweise (PowerShell):**
```powershell
$json_block | ConvertFrom-Json | ConvertTo-Json -Depth 10 |
    Set-Content "$env:USERPROFILE\.Gemini\self-improve-cache\R${N}_findings.json" -Encoding UTF8
```

**Compatibility:** Der Markdown-Cache (R*_plugins.md, R*_security.md, etc.) bleibt parallel
erhalten fuer menschliche Lesbarkeit. JSON dient ausschliesslich der maschinellen
Auswertung durch zukuenftige Self-Improve-Laeufe.

## Researcher Templates

**MANDATORY: Spawn ALL active researchers in ONE message block.**

### R1 — Gemini CLI Updates (ALWAYS run)
```
"Research Gemini CLI CLI updates and new features. Current version: [version].
Search: (1) changelog since this version, (2) new settings/env vars,
(3) performance improvements, (4) breaking changes.
Spawn sub-agents to verify: one for changelog, one for settings docs.
Return only actionable findings with version numbers. Date: [today]."
```

### R2 — Plugins & Marketplace (skip if no env changes since last cache — git-diff check against ~/.Gemini/agents/, ~/.Gemini/hooks/, ~/.Gemini/rules/; run unconditionally if > 14 days since last run)

**Before spawning R2**: Read `~/.Gemini/settings.json`, extract all `enabledPlugins` keys, and substitute them into the R2 prompt below in place of `[INSTALLED_PLUGINS_LIST]`.

```
"Research new Gemini CLI plugins available in [month+year].
Search: (1) official plugins (anthropics/Gemini-plugins-official),
(2) superpowers-marketplace updates, (3) community plugins for Swift/C#/TypeScript/Rust/Go/Kotlin.

SELF-VALIDATION (MANDATORY): Before recommending ANY plugin:
1. Run `gh api repos/{owner}/{repo}` to verify the repo EXISTS and is NOT archived
2. Check if the plugin name appears in the installed plugins list below
3. Only recommend plugins that pass BOTH checks

Installed plugins (DO NOT recommend these):
[INSTALLED_PLUGINS_LIST]

Return: plugin name, source, what it does, stars, recommendation."
```

### R3 — Parallelization & Automation (skip if no env changes since last cache — git-diff check against ~/.Gemini/agents/, ~/.Gemini/hooks/, ~/.Gemini/rules/; run unconditionally if > 14 days since last run)
```
"Research Gemini CLI agent teams, parallelization, automation best practices as of [today].
Search: (1) Agent Teams patterns, (2) new hook events/types,
(3) skill structuring for large skills, (4) worktree isolation patterns.
Spawn sub-agents per topic. Return actionable patterns with code examples."
```

### R4 — Tool Versions (skip if no env changes since last cache — git-diff check against ~/.Gemini/agents/, ~/.Gemini/hooks/, ~/.Gemini/rules/; run unconditionally if > 14 days since last run)
```
"Find latest stable versions of: [tool list with current versions from scan].
IMPORTANT: Compare against EXACT versions from env-checker scan, not memory.
If exact scan data unavailable, mark as 'unverified'.
Spawn sub-agent to double-check each version against official release pages.
Return table: tool | current | latest | update needed? | how to update"
```

### R5 — Security (ALWAYS run)
```
"Search for known CVEs in: [tools+versions from scan].
Check: (1) CVEs in specific versions, (2) platform-specific issues,
(3) Gemini CLI security advisories, (4) supply chain alerts.
Spawn sub-agents per tool category.
.NET RULE: Map CVE Runtime versions to installed SDK band before flagging.
Return ONLY confirmed vulnerabilities with CVE numbers — no speculation."
```

### R6 — Creative Environment Explorer (ALWAYS run, NEW in v5.0)
```
"You are a CREATIVE researcher. Your job is NOT to check versions or find CVEs.
Your job is to discover NEW IDEAS for improving a Gemini CLI development environment
on macOS and Windows.

Think OUTSIDE the box:
1. Search for blog posts, GitHub repos, and discussions about creative Gemini CLI setups
2. Look for productivity tools, shell tricks, and automation patterns other developers use
3. Find interesting MCP server combinations or hook patterns nobody talks about
4. Look for cross-platform development workflows that are unusually efficient
5. Search for ways to measure and improve AI coding assistant performance
6. Find new CLI tools, TUI tools, or developer utilities released in [current month]

DO NOT search for: version numbers, CVEs, standard tool updates.
DO search for: surprising ideas, unconventional approaches, hidden gems.

For each finding: explain what it is, why it's interesting, and how it could
be adapted for this specific setup (macOS + Windows, Swift/C#/TypeScript/Rust/Go/Kotlin).

Return: at least 3 genuinely novel ideas with links and adaptation proposals."
```

### R7 — Focus Deep-Dive (ONLY in Focus mode, NEW in v5.2)
```
"You are a DEEP-DIVE specialist for the topic: [FOCUS TOPIC].
Your job is to go BEYOND what a general researcher would find.

Research strategy:
1. Search for advanced tutorials, conference talks, and expert blog posts on [FOCUS TOPIC]
2. Find anti-patterns and common mistakes developers make with [FOCUS TOPIC]
3. Look for benchmarks, performance comparisons, and real-world case studies
4. Search for tools, libraries, and frameworks specifically designed for [FOCUS TOPIC]
5. Find architecture patterns and best practices from production apps using [FOCUS TOPIC]
6. Look for upcoming changes, deprecations, or new APIs related to [FOCUS TOPIC]

DO NOT duplicate what R1-R6 already cover (versions, CVEs, general plugins).
DO focus on: deep expertise, edge cases, production-ready patterns, expert opinions.

For each finding: explain what it is, why it matters for production apps,
and provide concrete implementation guidance (code snippets, Gradle deps, config).

Return: at least 5 expert-level insights with implementation details."
```

**RULE**: R7 is ONLY spawned when the user specifies a focus topic (e.g., "Fokus Android Audio").
In standard or thorough mode without focus, R7 is NOT spawned.
R7 runs in parallel with R1-R6 — add it to the same spawn block.

### R8 — Intelligence Researcher (ALWAYS run, NEW in v5.11)
```
"Du bist der INTELLIGENZ-FORSCHER. Dein einziges Ziel: Finde Wege die dieses AI-Coding-System
zum BESTEN PROGRAMMIERER DER WELT zu machen. Nicht inkrementell besser — FUNDAMENTAL schlauer.

Dein Forschungsauftrag hat 5 Dimensionen:

1. **Reasoning-Durchbrueche** (akademische Forschung):
   Suche auf arxiv, ICML, NeurIPS, ICLR, ACL nach Papers zu:
   - AI Agent Self-Improvement und Recursive Self-Refinement
   - Chain-of-Thought Verbesserungen, Tree-of-Thought, Graph-of-Thought
   - Formale Verifikation als Reasoning-Werkzeug (nicht nur als Test)
   - Multi-Agent-Debate und Reflexion-Architekturen
   - Code-spezifische LLM-Benchmarks: Was trennt die besten AI-Coder vom Rest?
   Fuer jedes relevante Paper: Kernidee in 2 Saetzen + wie es KONKRET in Gemini CLI umsetzbar ist.

2. **Kognitive Werkzeuge** (sofort einsetzbar):
   Suche nach MCP-Servern, gemini-setup-Plugins, CLI-Tools die das DENKEN verbessern:
   - Wissensgraphen (Codebase als Graph, Abhaengigkeiten visualisieren)
   - Semantische Code-Suche (Embeddings, Vector-DBs fuer Code)
   - Formal Verification Tools (TLA+, Alloy, Z3 fuer Spec-Pruefung)
   - Statische Analyse auf Steroiden (CodeQL, Semgrep Pro, Infer)
   - Reasoning-Verstaerker (Scratchpad-Tools, Thought-Visualizer)
   Fuer jedes Tool: Name, Link, was es tut, wie es Gemini SCHLAUER macht (nicht nur schneller).

3. **Kompetitive Analyse** (Was macht die Konkurrenz?):
   Suche nach: Cursor, Windsurf, GitHub Copilot Workspace, Codex CLI, Devin, SWE-Agent Benchmarks.
   - Welche Techniken nutzen sie die Gemini CLI NICHT hat?
   - Gibt es oeffentliche Benchmarks (SWE-bench, HumanEval, MBPP) mit neuen Ergebnissen?
   - Welche Patterns oder Workflows machen andere AI-Coder messbar besser?
   - Gibt es Open-Source-Projekte die Agent-Architekturen verbessern?
   ZIEL: Finde mindestens 1 Technik die die Konkurrenz nutzt und die wir SOFORT adaptieren koennen.

4. **Biologisch inspirierte Muster** (Wie denken Elite-Programmierer?):
   Suche nach Studien und Blogs ueber:
   - Wie loesen die besten menschlichen Programmierer Probleme? (Decomposition, Pattern Matching)
   - Welche kognitiven Strategien nutzen 10x-Developer?
   - Pair Programming Patterns die auf Multi-Agent uebertragbar sind
   - Feynman-Technik, Rubber Duck Debugging, Mental Models — automatisierbar?
   ZIEL: 1 menschliche Denkstrategie finden die als Agent-Workflow implementierbar ist.

5. **Selbstverbesserungs-Mechanismen** (Meta-Lernen):
   Suche nach Frameworks und Patterns fuer:
   - AI Agents die aus eigenen Fehlern lernen (Reflexion, Self-Play)
   - Automatische Skill-Extraktion aus erfolgreichen Sessions
   - Prompt-Optimierung durch Feedback-Loops (DSPy, TextGrad)
   - Adaptive Modell-Routing basierend auf Aufgaben-Komplexitaet
   ZIEL: 1 konkreter Mechanismus der das System AUTOMATISCH schlauer macht ueber Zeit.

REGELN:
- KEINE Versions-Checks, KEINE CVEs, KEINE Standard-Updates — das machen andere Researcher.
- NUR Dinge die das DENKEN und die CODE-QUALITAET fundamental verbessern.
- Fuer jeden Fund: Konkret erklaeren was es ist + wie es in DIESES Setup integriert wird.
- Mindestens 5 Findings mit je: Titel, Quelle/Link, Kernidee, Umsetzungsvorschlag.
- Priorisiere: SOFORT UMSETZBAR > theoretisch interessant > langfristig relevant.
- Datum: [today]"
```

**RULE**: R8 runs EVERY time — intelligence must never be cached.
R8 runs in parallel with R1-R7 — add it to the same spawn block.
R8 results are consumed by Stufe 5 (SUPER INTELLIGENZ) as primary input.

---

## Cross-Validation Rules

After all researchers return, validate against scan data:

1. **System state** (scan) > **Memory** > **Web research**
2. **Version claims**: Only flag if local version is LOWER than latest
3. **Security claims**: Verify against OS patch data from scan
4. **"Not installed" claims**: Verify against scan + plugin list
5. **Contradictions**: Trust more specific source
6. **Schema validation**: Before implementing new hooks/settings, verify they exist in actual JSON schema

## Plugin Security Review Template

For each recommended plugin, spawn a researcher:
```
"Security review of Gemini CLI plugin '[owner/repo]'.
Check: (1) GitHub metrics (stars, forks, last commit, maintainer),
(2) marketplace listing, (3) security advisories,
(4) code analysis (plugin.json, hooks, scripts — look for obfuscated code,
suspicious URLs, Base64, data exfiltration, prompt injection),
(5) dependency analysis.
Return: SAFE / CAUTION / UNSAFE + reasoning + red flags."
```
Only install SAFE or CAUTION-without-red-flags. Show assessment to user.

