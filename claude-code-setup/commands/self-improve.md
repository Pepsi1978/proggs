---
name: self-improve
description: Systematic self-improvement of the Claude Code development environment (~10-30 min, token-intensive). ONLY use when the user explicitly says "/self-improve", "verbessere dich", "optimiere deine Umgebung", "check dein Setup", "update alles", "mach mich besser", "aktualisiere alles", "pruef mein System", "System-Check", or "Umgebung pruefen". NEVER run this proactively or automatically — only on manual user request.
---

# Self-Improve v5.11 — Systematic + Creative + Intelligence Environment Optimization

**Before doing ANYTHING, show this overview in German:**

```
╔══════════════════════════════════════════════════════════════╗
║  Self-Improve Skill v5.0 — Deine Entwicklungsumgebung       ║
║  pruefen, aktualisieren, KREATIV ERFORSCHEN                  ║
║  Cross-Platform: macOS + Windows + Termux/Android            ║
╠══════════════════════════════════════════════════════════════╣
║  Stufe 1: SCAN — env-checker Agent (Full/Quick)              ║
║  Stufe 2: DEEP-DIVE — 8 Researcher + Smart-Cache (TTL)      ║
║  Stufe 3: IMPROVE — Updates + Report + Meta-Improve          ║
║  Stufe 4: CREATIVE — 6 Linsen + Benchmark + Build            ║
║  Stufe 5: SUPER INTELLIGENZ — Schlauer denken, nicht nur     ║
║           Fehler vermeiden                                    ║
║  Stufe 6: FEHLER-DAUERHAFTIGKEIT — Null Fehler, dauerhaft    ║
║  Danach: GitHub-Sync (Pepsi1978/proggs)                      ║
╚══════════════════════════════════════════════════════════════╝
```

## Zentrales Whiteboard — Nervensystem des Gesamtsystems (KRITISCH)

Das Whiteboard (`.claude/agent-memory/shared/MEMORY.md`) ist die EINZIGE zentrale Wissensdatei
fuer ALLE Komponenten des Claude Code Systems: Agents, Skills, Hooks, MCP-Server, Plugins.

**Self-Improve ist der WICHTIGSTE Konsument dieses Whiteboards:**
1. **ZUERST lesen**: Vor jeder Aktion das Whiteboard komplett lesen
2. **Offene Fehler fixen**: Sektion "Offene Fehler & Probleme" — JEDER Eintrag mit Status OFFEN
   MUSS in diesem Lauf gefixt oder zumindest adressiert werden.
   Fehlerformat im Whiteboard: Quelle, Symptom, Ursache, Betroffene Dateien, Fix-Vorschlag, Status.
   Der "Fix-Vorschlag" sagt dir konkret was zu tun ist — folge ihm oder verbessere ihn.
3. **Benutzer-Feedback lesen**: `~/.claude/projects/*/memory/feedback_*.md` enthalt
   direkte Korrekturen und Praeferenzen des Benutzers. Diese MUESSEN gelesen werden, weil:
   - Feedback-Muster die systemweit gelten → als neue Regel oder Hook implementieren
   - Wiederholte Korrekturen → deuten auf fehlendes automatisches Verhalten hin
   - Auf BEIDEN Plattformen relevant: macOS hat eigenes Memory-Verzeichnis, aber der
     Inhalt muss funktional identisch wirken
4. **Claude-Mem Observations lesen**: `~/.claude/homunculus/projects/*/observations.jsonl`
   enthaelt Cross-Session-Wissen. Pruefen ob relevante Beobachtungen fuer aktuelle Verbesserungen
   nutzbar sind (z.B. wiederkehrende Probleme die ueber mehrere Sessions beobachtet wurden).
5. **Systemzustand aktualisieren**: Nach Aenderungen die Sektion "Systemzustand" updaten
6. **Erkenntnisse eintragen**: Alles was fuer andere Agents/Skills nuetzlich ist → reinschreiben
6. **Keine separaten Wissens-Dateien**: Es gibt NUR dieses eine Whiteboard fuer System-Wissen.
   Die Benutzer-Feedback-Memories sind die EINZIGE Ausnahme — sie sind an das Whiteboard
   angeschlossen (siehe Header) und werden von /self-improve mitgelesen.

**Whiteboard-Sektionen:**
- "Offene Fehler & Probleme" — Fehler aus Hooks, Agents, MCP-Servern, Plugins (PRIORITAET!)
- "Systemzustand" — Aktueller Stand aller Tools, Versionen, Konfigurationen
- "Erkenntnisse aus Code Reviews/Tests/Architektur/Debugging/Performance/UI/Forschung" — thematisch
- "Regeln & Konventionen" — Systemweite Regeln

## Core Rules

- NEVER run hidden (no `run_in_background`, no silent subagents). User reads EVERYTHING.
- Default: `effortLevel: high` (permanenter Standard laut CLAUDE.md). Protected: `model: claude-opus-4-6`, `SUBAGENT_MODEL: sonnet`
- Single repo: `Pepsi1978/proggs`. NEVER create new repos.
- Parallel execution: Maximum simultaneous agents wherever possible.
- Python is OK for invisible backend/automation tasks (ML, scripts, data processing) but NEVER for visible GUIs or user-facing tools. Preferred for visible: Swift, C#, TypeScript, Rust, Go, Kotlin.
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

## Thoroughness Detection

- **Standard**: "/self-improve" without extras → Stufe 1 quick, R1+R5+R6 only (cached R2-R4)
- **Thorough**: "sehr ausfuehrlich", "full check" → Stufe 1 full, ALL 6 researchers
- **Focus**: "Fokus [topic]" → Thorough + deep-dive into topic

## Platform Detection (FIRST STEP)

```bash
uname -s   # Darwin=macOS, MINGW*=Windows
```
Rule: Never `brew` on Windows, never `winget` on macOS.
Windows: Use `pwsh` for complex commands (write temp `.ps1` files).

## Stufe 0: META-CHECK (NEW v5.1)

**Before env-checker**: Start `evolution-analyst` agent in parallel with Stufe 1.
The evolution-analyst reads `~/.claude/session-scores.jsonl` and `~/.claude/agent-memory/shared/MEMORY.md`
to identify quality trends and recurring weaknesses BEFORE the scan begins.

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
**Stufe 0, 1 and 2 start SIMULTANEOUSLY.**

**Semantic Search Index (v5.14 — parallel mit Stufe 1):**
Wenn `~/proggs/mcp-code-search/` existiert: Pruefe ob der Index aktuell ist.
- Pruefe `~/proggs/.code-search/.last-index-time` — wenn aelter als 24h oder nicht vorhanden: Neu indexieren.
- Indexierung laeuft per Bash parallel zu den anderen Stufe-1-Aufgaben:
  ```bash
  cd ~/proggs/mcp-code-search && bun -e "
    import { findCodeFiles, chunkFile } from './src/indexer.ts';
    import { generateEmbeddings } from './src/ollama.ts';
    import { VectorStore } from './src/store.ts';
    import { resolve, join } from 'path';
    import { mkdirSync, existsSync, writeFileSync, readdirSync, unlinkSync } from 'fs';
    const root = resolve('\$HOME/proggs');
    const dbDir = join(root, '.code-search');
    if (!existsSync(dbDir)) mkdirSync(dbDir, { recursive: true });
    // Pointer-based: find next index-N.db number
    let maxN = 0;
    for (const f of readdirSync(dbDir)) { const m = f.match(/^index-(\d+)\.db$/); if (m) maxN = Math.max(maxN, +m[1]); }
    const newName = 'index-' + (maxN+1) + '.db';
    const files = await findCodeFiles(root);
    const chunks = []; for (const f of files) chunks.push(...await chunkFile(f, root));
    const store = new VectorStore(join(dbDir, newName));
    for (let i = 0; i < chunks.length; i += 32) {
      const b = chunks.slice(i, i+32);
      store.insertBatch(b, await generateEmbeddings(b.map(c=>c.content)));
    }
    store.close();
    writeFileSync(join(dbDir, 'current.txt'), newName); // Pointer-swap
    // Cleanup old DBs (best-effort, skips locked files)
    for (const f of readdirSync(dbDir)) { if (f.match(/^index-\d+\.db/) && !f.startsWith(newName.replace('.db',''))) try { unlinkSync(join(dbDir,f)); } catch {} }
    console.log('Indexed: ' + files.length + ' files, ' + chunks.length + ' chunks (' + newName + ')');
  "
  ```
- Im Report melden: "Semantic Index: [N] Dateien, [N] Chunks (aktualisiert/bereits aktuell)"
- **Voraussetzung**: Ollama muss laufen. Wenn nicht → ueberspringen mit Hinweis.

**Semantic Search Integritaetstest (v5.16 — PFLICHT bei jedem Lauf):**
Nach dem Indexieren (oder wenn Index bereits aktuell): Vollstaendigen Integritaetstest durchfuehren.
Prueft 4 Kategorien: Grundfunktion, Pointer-Konsistenz, Aufraeum-Status, Infrastruktur.

**Kategorie 1: Grundfunktion (Ollama → Embedding → DB → Suche)**
```bash
cd ~/proggs/mcp-code-search && bun -e "
  import { VectorStore } from './src/store.ts';
  import { generateEmbedding } from './src/ollama.ts';
  import { resolve, join } from 'path';
  import { existsSync, readFileSync } from 'fs';
  const root = resolve('\$HOME/proggs');
  const dbDir = join(root, '.code-search');
  const pointerFile = join(dbDir, 'current.txt');
  if (!existsSync(pointerFile)) { console.log('FAIL-1A: current.txt fehlt'); process.exit(1); }
  const dbName = readFileSync(pointerFile, 'utf-8').trim();
  if (!existsSync(join(dbDir, dbName))) { console.log('FAIL-1B: ' + dbName + ' existiert nicht'); process.exit(1); }
  const store = new VectorStore(join(dbDir, dbName));
  const stats = store.stats();
  if (stats.totalChunks === 0) { console.log('FAIL-1C: Index leer'); process.exit(1); }
  const emb = await generateEmbedding('test query for health check');
  if (emb.length !== 768) { console.log('FAIL-1D: Embedding ' + emb.length + ' statt 768 dim'); process.exit(1); }
  const results = store.search(emb, 3);
  if (results.length === 0) { console.log('FAIL-1E: Suche 0 Ergebnisse'); process.exit(1); }
  store.close();
  console.log('OK-1: ' + stats.totalFiles + ' Dateien, ' + stats.totalChunks + ' Chunks, DB=' + dbName);
"
```

**Kategorie 2: Pointer-Konsistenz (Zeiger zeigt auf die richtige DB?)**
Pruefe manuell per Bash (kein Bun noetig):
```bash
DBDIR=~/proggs/.code-search
# 2A: current.txt existiert und ist nicht leer
test -s "$DBDIR/current.txt" && echo "OK-2A" || echo "FAIL-2A: current.txt fehlt oder leer"
# 2B: Die referenzierte DB-Datei existiert
POINTER=$(cat "$DBDIR/current.txt" 2>/dev/null)
test -f "$DBDIR/$POINTER" && echo "OK-2B: $POINTER" || echo "FAIL-2B: $POINTER nicht gefunden"
# 2C: Keine verwaisten index-N.db Dateien (mehr als 1 = Cleanup hat nicht funktioniert)
COUNT=$(ls "$DBDIR"/index-*.db 2>/dev/null | wc -l)
if [ "$COUNT" -le 1 ]; then echo "OK-2C: $COUNT DB-Datei(en)"; else echo "WARN-2C: $COUNT DB-Dateien — Cleanup pruefen"; fi
# 2D: Keine verwaisten WAL/SHM-Dateien von ALTEN Indexen
OLD_WAL=$(ls "$DBDIR"/index-*.db-wal "$DBDIR"/index-*.db-shm 2>/dev/null | grep -v "$POINTER" | wc -l)
if [ "$OLD_WAL" -eq 0 ]; then echo "OK-2D: Keine verwaisten WAL/SHM"; else echo "WARN-2D: $OLD_WAL alte WAL/SHM-Dateien — aufraumen"; fi
```

**Kategorie 3: Infrastruktur (Hooks, MCP, Konfiguration)**
Pruefe per Bash:
```bash
# 3A: SessionStart-Hook registriert?
grep -q "reindex-codebase" ~/.claude/settings.json && echo "OK-3A: Hook registriert" || echo "FAIL-3A: Hook fehlt"
# 3B: MCP-Server in .mcp.json konfiguriert?
grep -q "code-search" ~/proggs/.mcp.json && echo "OK-3B: MCP konfiguriert" || echo "FAIL-3B: MCP fehlt"
# 3C: .code-search in .gitignore?
grep -q "code-search" ~/proggs/.gitignore && echo "OK-3C: .gitignore OK" || echo "FAIL-3C: DB wuerde ins Repo committed!"
# 3D: node_modules vorhanden?
test -d ~/proggs/mcp-code-search/node_modules && echo "OK-3D: Dependencies installiert" || echo "FAIL-3D: bun install noetig"
# 3E: Ollama erreichbar?
curl -s -o /dev/null -w "%{http_code}" http://localhost:11434/api/tags | grep -q 200 && echo "OK-3E: Ollama laeuft" || echo "WARN-3E: Ollama nicht erreichbar"
# 3F: nomic-embed-text geladen?
curl -s http://localhost:11434/api/tags | grep -q "nomic-embed-text" && echo "OK-3F: Modell vorhanden" || echo "FAIL-3F: ollama pull nomic-embed-text noetig"
```

**Kategorie 4: Parallelitaets-Sicherheit (Race Conditions)**
Diese Pruefung ist EINMALIG bei der Ersteinrichtung und danach NUR im Thorough-Modus noetig:
- Pruefe ob der MCP-Server `resolveCurrentDb()` bei JEDEM Tool-Call den Pointer neu liest (nicht cached)
- Pruefe ob der Reindex-Hook in eine NEUE DB schreibt (nicht in die aktive)
- Pruefe ob die Cleanup-Logik gesperrte Dateien ueberspringt (try/catch um unlinkSync)
Wenn eine dieser Pruefungen fehlschlaegt → als KRITISCHER DEFEKT in MEMORY.md unter "Offene Fehler & Probleme" eintragen.
**Schnelltest:** `grep -c "resolveCurrentDb" ~/proggs/mcp-code-search/src/index.ts` muss >= 2 sein (einmal Definition, mindestens einmal Aufruf in getStore).

**Auswertung aller 4 Kategorien:**
- Alle OK → ✅ "Semantische Suche: voll funktionsfaehig ([N] Dateien, [N] Chunks)"
- WARN vorhanden → ⚠️ Melden + Cleanup versuchen (alte DBs loeschen, WAL/SHM aufraumen)
- FAIL vorhanden → ❌ Melden + Auto-Reparatur versuchen:
  1. current.txt fehlt → Auto-Discover: hoechste index-N.db finden, Pointer schreiben
  2. Ollama fehlt → Hinweis an Benutzer
  3. Modell fehlt → `ollama pull nomic-embed-text` ausfuehren
  4. DB korrupt → DB loeschen, neu indexieren
  5. Hook fehlt → Hook-Registrierung in settings.json reparieren
  6. .gitignore fehlt → `.code-search/` hinzufuegen
  Jeden Fehler in MEMORY.md unter "Offene Fehler & Probleme" eintragen und Behebungsversuch dokumentieren.

## Stufe 2: DEEP-DIVE

**Load researcher templates from**: [self-improve-ref/researchers.md](self-improve-ref/researchers.md)

Key changes in v5.11:
- **8 researchers** (R1-R8). R6=Creative, R7=Focus Deep-Dive, R8=Intelligence Researcher (NEW v5.11).
- **Smart-Cache with per-category TTLs (NEW v5.7)**:
  Cache location: `~/.claude/self-improve-cache/` (NOT in shared memory — prevents contamination).
  | Researcher | TTL | Grund |
  |------------|-----|-------|
  | R1 Updates | 1 Tag | Claude Code Releases aendern sich schnell |
  | R2 Plugins | 7 Tage | Marketplace aendert sich selten |
  | R3 Parallel | 7 Tage | Patterns sind stabil |
  | R4 Versionen | 3 Tage | Tool-Versionen aendern sich maessig schnell |
  | R5 Security | IMMER frisch | Sicherheit darf nie gecacht werden |
  | R6 Creative | IMMER frisch | Kreativitaet braucht frische Ideen |
  | R8 Intelligence | IMMER frisch | Intelligenz-Forschung muss immer aktuell sein |
  Check: `find ~/.claude/self-improve-cache/R{N}_*.md -mtime -{TTL_DAYS} 2>/dev/null`
  If cache file exists and within TTL: skip researcher, use cached data.
  If cache expired or missing: run researcher, save result to cache file.
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
4. **Fehler dokumentieren**: Jeden Absturz sofort in `~/.claude/agent-memory/shared/MEMORY.md unter "Offene Fehler & Probleme"` eintragen:
   ```
   ## [Datum] Researcher R[N] Absturz
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
**Remaining issues**: List ALL additional unfixed problems at the end of the report under
"Offene Probleme (nicht in diesem Lauf gefixt)" so the user can decide which to tackle next.

**After improvements**: Update "From Self-Improve" in MEMORY.md with new environment knowledge.

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

### 3E: R8 Intelligence Agent (NEW v5.12)
**Stufe 2 verwendet jetzt den dedizierten `intelligence-researcher` Agent** statt eines generischen
Researcher-Prompts. Der Agent hat:
- Eigenes Backlog-File (`~/.claude/agent-memory/shared/MEMORY.md unter "Forschung & Intelligence"`) mit Status pro Finding
- Liest vorherige Findings und sucht gezielt NEUE Ideen (kein Duplicate Research)
- Output-Format mit expliziter JA/NEIN-Empfehlung pro Finding
Spawn: `Agent(subagent_type="intelligence-researcher", name="r8-intelligence")`

## Stufe 4: CREATIVE RESEARCH

**Details in**: [self-improve-ref/report-and-creative.md](self-improve-ref/report-and-creative.md)

Core principle: **DISCOVER → EXPLAIN → BUILD → TEACH.**
Non-Programmer Rule: Plain German, analogies, concrete scenarios.
6 Thinking Lenses + Performance Benchmark Lens (NEW v5.0).

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
1. **Researcher spawnen**: "Welche neuen MCP-Server, Claude-Code-Plugins oder CLI-Tools wurden in den letzten 30 Tagen veroeffentlicht die das DENKEN verbessern?" (Wissensgraphen, Verifikation, Semantic Search, Code-Analyse, Formal Verification, Codebase-Embeddings)
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
4. **Die anderen Findings dem Benutzer praesentieren** und in MEMORY.md unter "Intelligence Backlog" speichern
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
| Praevention | 20 | Fehler mit ✅ DAUERHAFT Prevention = 2 Pkt, mit ⚠️ FRAGIL = 1 Pkt, ohne = 0 |
| Gedaechtnis | 20 | Abschnitte in MEMORY.md die in letzten 30 Tagen aktualisiert wurden = 2 Pkt/Abschnitt |
| Prozeduren | 20 | Workflows in MEMORY.md die verifiziert und aktuell sind = 2 Pkt/Workflow |
| Regeln | 20 | Rules die in letzten 5 Sessions relevant waren = 3 Pkt, nie relevant = 1 Pkt |
| Werkzeuge | 20 | Kognitive Tools die tatsaechlich GENUTZT werden = 3 Pkt, nur installiert = 1 Pkt |
| **Gesamt** | **100** | **Summe — Qualitaet zaehlt mehr als Quantitaet** |

Zeige: `IQ-Score: XX/100 (vorher: YY → Veraenderung: +/-Z)`
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
2. Lade das Transkript der schlimmsten Session (hoechste correction-Rate)
3. Extrahiere die 3 haeufigsten Korrektur-TYPEN (z.B. "falsches Ziel", "ungefragt geaendert", "falsche Datei")
4. Fuer jeden Typ: Pruefe ob eine Rule existiert die das verhindert. Wenn nicht → Rule erstellen.
5. Trage Erkenntnisse in MEMORY.md unter "Error Learnings" ein

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
4. Trage Dauerhaftigkeits-Status in MEMORY.md unter "Offene Fehler & Probleme" ein: `✅ DAUERHAFT` oder `⚠️ FRAGIL (Grund)`

### 6F: Lern-Extraktion (Was lehrt uns jeder Fehler?)
Fuer JEDEN neuen Fehler der in diesem Lauf gefunden wurde:
1. **Root Cause**: Warum ist er passiert? (Nicht das Symptom, die URSACHE)
2. **Systemische Lektion**: Was sagt dieser Fehler ueber das SYSTEM?
3. **Praevention**: Konkreter Hook, Rule oder Agent-Aenderung die diesen Fehlertyp FUER IMMER verhindert
4. Trage Lektion in MEMORY.md unter "Systemische Lektionen" ein

## Focus Mode

Focus adds depth, doesn't skip standard checks. **Stufe 5+6 laufen IMMER — auch im Focus-Modus.**
**Built-in focus topics**: Rust, Android, Security, TypeScript, Performance, Cross-Platform, Self-Improve.
For any focus: After checklist-based checks, explore BEYOND the checklist.

## Cross-Platform Sync (PFLICHT — v5.1)

**REGEL: Jede Verbesserung muss plattformuebergreifend wirksam werden.**
Alle Aenderungen die /self-improve macht — neue Hooks, Agents, Skills, Configs, Settings —
muessen bei GitHub hinterlegt werden, damit andere Plattformen (Windows, Termux) beim
naechsten Session-Start automatisch die neuesten Verbesserungen erhalten.

**Was synchronisiert werden muss:**
1. Hook-Scripts (*.sh + *.ts) → `~/proggs/claude-code-setup/hooks/`
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

```bash
cp ~/.claude/rules/*.md ~/proggs/claude-code-setup/rules/
cp ~/.claude/agents/*.md ~/proggs/claude-code-setup/agents/
cp ~/.claude/commands/self-improve.md ~/proggs/claude-code-setup/commands/
cp -r ~/.claude/commands/self-improve-ref/ ~/proggs/claude-code-setup/commands/self-improve-ref/
cp ~/.claude/hooks/*.sh ~/proggs/claude-code-setup/hooks/
cp ~/.claude/hooks/*.ts ~/proggs/claude-code-setup/hooks/ 2>/dev/null
cp ~/.claude/hooks/*.ps1 ~/proggs/claude-code-setup/hooks/ 2>/dev/null
cp ~/CLAUDE.md ~/proggs/CLAUDE.md
# Knowledge files (cross-platform shared intelligence)
mkdir -p ~/proggs/claude-code-setup/agent-memory/shared/
cp ~/.claude/agent-memory/shared/MEMORY.md ~/proggs/claude-code-setup/agent-memory/shared/
# Nur MEMORY.md — es gibt keine separaten Dateien mehr (alles konsolidiert)
# settings-reference.json muss MANUELL aktualisiert werden wenn Hooks/Plugins/Env sich aendern!
cd ~/proggs && git pull --rebase
git add claude-code-setup/ CLAUDE.md
# Re-stage after async formatters may have modified staged files (race condition fix)
sleep 1 && git add claude-code-setup/ CLAUDE.md
# Use git status instead of git diff --cached --quiet (more robust against formatter hooks)
if git status --porcelain claude-code-setup/ CLAUDE.md | grep -q .; then
  git commit -m "#NNN - Self-improve sync ([Platform])" && git push
fi
```

## Deferred Shell Updates (NACH allem anderen — v5.8)

**Dieser Schritt kommt NACH dem Final Summary und NACH dem GitHub-Sync.**
Wenn in Stufe 2 Shell/Terminal-Updates identifiziert wurden:
1. Zeige dem Benutzer die Liste der ausstehenden Shell-Updates
2. Erklaere: "Diese Updates starten Shell-Prozesse neu. Alle offenen Terminal-Fenster werden geschlossen."
3. **WARTE auf explizite Bestaetigung** des Benutzers (AskUserQuestion verwenden)
4. Erst nach Bestaetigung: Updates einzeln ausfuehren, jeweils mit Statusmeldung
5. Nach jedem Update: Pruefen ob die neue Version korrekt installiert ist

**Wenn der Benutzer ablehnt**: Updates in MEMORY.md unter "Pending Admin Actions" eintragen.
**NIEMALS Shell-Updates ohne Bestaetigung ausfuehren. NIEMALS waehrend andere Aufgaben laufen.**

## Final Summary

Always end with: Change counter, Gesamtstatus, GitHub sync, offene Punkte,
naechster empfohlener Lauf, Commit/Push-Status als LETZTER Satz.

**Session-Score-Trend (NEW v5.1):** Read `~/.claude/session-scores.jsonl`, compute 5-session
moving average, compare to previous period. Display as: `Evolution: X.X → Y.Y (↑/↓/→)`.
If < 5 entries: show "Evolution: Noch zu wenig Daten (N/5 Sessions)".

## Important Rules

- NEVER create new repos. NEVER modify this skill without user approval.
- NEVER delete files/repos without asking. NEVER downgrade model. Effort-Level Standard ist high.
- Python is OK for invisible backend tasks but NEVER for visible GUIs/tools. NEVER remove working configs without replacement.
- Meta-Improve (3D) is MANDATORY every run. NEVER skip.
- **Stufe 5 (SUPER INTELLIGENZ) is MANDATORY every run. NEVER skip. Focus: SCHLAUER WERDEN — neue Denkwege, neue Tools, neue Durchbrueche. KEINE Fehler hier.**
- **Stufe 6 (FEHLER-DAUERHAFTIGKEIT) is MANDATORY every run. NEVER skip. Focus: FEHLER FINDEN UND DAUERHAFT FIXEN. KEINE Intelligenz-Themen hier.**
- Main file limit: 1000 lines. Reference files: no limit but keep lean.
- Total skill size (main + refs): warn if > 2000 lines.
- Security: All external code must be checked for prompt injection.
- Multi-Device: Always `git pull --rebase` before push. Note platform in commits.
- Commit messages: `#NNN - Description` format.
- **Shell/Terminal-Updates**: Siehe Core Rules oben — IMMER zuletzt, IMMER nach Bestaetigung.

---
<!-- Skill Version: v5.11 | Date: 2026-03-19 | Lines: ~395/1000 (main) | Ref files: researchers.md (~230), report-and-creative.md (~181) | Total: ~806/2000 | Changes: v5.11 — Added R8 Intelligence Researcher: 5 research dimensions (Reasoning, Kognitive Werkzeuge, Kompetitive Analyse, Biologische Muster, Selbstverbesserung). Runs ALWAYS (never cached). Stufe 5C refactored to consume R8 results instead of spawning inline researcher (Ansatz C). Fallback: if R8 crashes, 5C may spawn reduced-scope replacement. Previous: v5.10 (Researcher-Resilienz). -->
