# Agents — Best Practices (Stand 2026-06-05, Claude Code 2.1.165)

> Recherche-Datum: 2026-05-30 | Claude Code Version: 2.1.158
> Quellen: Offizielle Anthropic-Doku (code.claude.com/docs, anthropic.com), Changelog 2.1.153–2.1.158
> Vorherige Version: 2026-05-28 | 2.1.153

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Custom-Agent starten | `subagent_type:"general-purpose"` + Prompt, NIE Custom-`subagent_type` | Agent-Tool |
| 2 | Subagent-Modell | via `CLAUDE_CODE_SUBAGENT_MODEL` (überschreibt Frontmatter) → Policy `opus[1m]` | Modell-Aufloesung |
| 3 | Parallelitaet | 3–5 Subagents Sweet-Spot; Workflows max 16 gleichzeitig / 1000 total | Parallelisierung |
| 4 | Subagent-Crash (`Prompt too long`) | `tools:`-Whitelist + `ENABLE_TOOL_SEARCH`; kein Auto-Compact → Orchestrator-Resume | Frontmatter / Modell |
| 5 | Agent Teams | nur wenn Teammates kommunizieren (3–4x teurer) | Agent Teams |
| 6 | Dynamic Workflows | deterministische Orchestrierung; Agenten erben Session-Modell | Dynamic Workflows |
| 7 | Memory | `memory`-Feld nur fuer Agents, die ueber Sessions lernen sollen | Memory-Persistenz |

---

## NEU in 2.1.154–2.1.158: Überblick der wichtigsten Änderungen

| Feature | Version | Typ |
|---------|---------|-----|
| Dynamic Workflows (`/workflows`, JS-Orchestrierung bis 1.000 Agents) | 2.1.154 | NEU (Research Preview) |
| Claude Opus 4.8 als Standard-Modell (High Effort by Default) | 2.1.154 | NEU |
| Background Shell Commands (`! <cmd>`, `--bg --exec`) | 2.1.154 | NEU |
| Streaming Tool Execution immer aktiv (kein Feature-Flag mehr) | 2.1.154 | Verbesserung |
| `/simplify` als Cleanup-Review | 2.1.154 | NEU |
| `/effort ultracode` (xhigh + Auto-Workflows) | 2.1.154 | NEU |
| `EnterWorktree` wechselt mid-session zwischen Worktrees | 2.1.157 | Verbesserung |
| `agent`-Feld in `settings.json` für dispatched Sessions | 2.1.157 | NEU |
| Worktrees bleiben nach Session-Ende entsperrt | 2.1.157 | Bugfix |
| Plugins in `.claude/skills` automatisch geladen | 2.1.157 | Verbesserung |
| Auto-Mode auf Bedrock/Vertex/Foundry für Opus 4.7/4.8 | 2.1.158 | Verbesserung |

Quelle: `https://code.claude.com/docs/en/changelog` | offiziell | 2026-05-30

---

## Dynamic Workflows (NEU ab v2.1.154) — WICHTIGSTES NEUES FEATURE

### Was sind Dynamic Workflows?

Ein Dynamic Workflow ist ein **JavaScript-Orchestrierungs-Script**, das Claude on-the-fly schreibt, und das eine separate Runtime im Hintergrund ausführt — während die Session responsiv bleibt. Im Gegensatz zu normalen Subagents (wo Claude Orchestrator ist und jedes Ergebnis in Claude's Context landet) hält das Script selbst die Schleife, das Branching und die Zwischenergebnisse. Claude's Context enthält nur die finale Antwort.

### Wann Workflows vs. Subagents vs. Skills

| | Subagents | Skills | Workflows |
|--|-----------|--------|-----------|
| Was | Ein Worker den Claude spawnt | Instruktionen denen Claude folgt | Ein Script das die Runtime ausführt |
| Orchestrator | Claude (turn-by-turn) | Claude | Das Script |
| Zwischenergebnisse | Claude's Context | Claude's Context | Script-Variablen |
| Wiederholbar | Worker-Definition | Die Instruktionen | Die Orchestrierung selbst |
| Skalierung | Wenige Tasks pro Turn | Wie Subagents | Dutzende bis Hunderte Agents/Run |
| Unterbrechung | Startet Turn neu | Startet Turn neu | Resumable in derselben Session |

**Faustregel:** Workflow nutzen wenn die Aufgabe mehr Agents braucht als ein Konversations-Loop koordinieren kann, oder wenn die Orchestrierung als wiederholbares Script codifiziert werden soll.

### Typische Use Cases für Workflows
- Codebase-weite Bug-Sweeps über viele Dateien
- 500-Datei-Migrationen
- Research-Fragen die Quellen gegeneinander cross-checken
- Pläne die aus mehreren unabhängigen Winkeln entworfen werden sollen

### Limits (wichtig!)

| Constraint | Wert | Grund |
|------------|------|-------|
| Max concurrent Agents | **16** | Lokale Ressourcen schonen |
| Max total Agents per Run | **1.000** | Verhindert Runaway-Loops |
| User-Input mid-run | **Nicht möglich** | Nur Permission-Prompts können pausieren |
| Direkte Filesystem-/Shell-Zugriffe | **Nicht erlaubt** | Nur Agents dürfen lesen/schreiben/ausführen |
| Session-Ende während Run | Workflow bricht ab | Resume funktioniert NUR in derselben Session |

### Workflow starten — 3 Methoden

**Methode 1: Keyword "workflow" im Prompt**
```text
Run a workflow to audit every API endpoint under src/routes/ for missing auth checks
```
Claude Code markiert das Wort und schreibt ein Workflow-Script statt normal zu arbeiten.

**Methode 2: Bundled Workflows**
```text
/deep-research What changed in the Kotlin coroutines API between 1.7 and 1.9?
```
`/deep-research` ist der eingebaute Workflow (Web-Recherche + Cross-Checking + Synthesebericht).

**Methode 3: ultracode-Modus (automatisch)**
```text
/effort ultracode
```
Mit `ultracode` plant Claude automatisch für jede substantielle Aufgabe einen Workflow. Kombiniert `xhigh`-Reasoning mit automatischer Workflow-Orchestrierung. Setzt sich am Session-Ende zurück.

### `/workflows` — Monitoring & Steuerung

```text
/workflows
```

| Taste | Aktion |
|-------|--------|
| `↑` / `↓` | Phase oder Agent auswählen |
| `Enter` / `→` | In Phase/Agent reinbohren (Prompt, Tool-Calls, Ergebnis) |
| `Esc` | Ebene zurück |
| `p` | Run pausieren / fortsetzen |
| `x` | Agent oder ganzen Workflow stoppen |
| `r` | Laufenden Agent neu starten |
| `s` | Script als Workflow-Command speichern |

### Workflow-Commands speichern & wiederverwenden

Nach einem erfolgreichen Run: `s` drücken und Speicherort wählen:
- `.claude/workflows/` — Projekt-spezifisch, im Repo geteilt
- `~/.claude/workflows/` — Global, nur lokaler Nutzer

Danach als `/<name>` verfügbar (erscheint in `/`-Autocomplete). Projekt-Workflows haben Vorrang vor globalen.

### Deaktivierung (wenn nötig)
- Toggle in `/config`: "Dynamic workflows"
- `"disableWorkflows": true` in `~/.claude/settings.json`
- `CLAUDE_CODE_DISABLE_WORKFLOWS=1` Umgebungsvariable

### Best Practices — Dynamic Workflows

- **Für Frank**: Bei Codebase-Audits über viele Dateien (z.B. BestJournalAndroid strings.xml-Konsistenzprüfung über 26 Sprachen) Workflow statt 20+ parallele Subagents nutzen — übersichtlicher und resumable
- **Modell-Kontrolle**: Vor großem Run `/model` prüfen — alle Workflow-Agents nutzen Session-Modell
- **Erlaubnisse vorbereiten**: Shell-Commands/Web-Fetches die Agents brauchen, vorab in Allowlist — verhindert Prompts mitten im Run
- **ultracode sparsam**: Deutlich mehr Token-Verbrauch; nur für Aufgaben die das wirklich rechtfertigen
- **Resume-Grenze beachten**: Session-Ende killt den Run; bei langen Workflows Session offen halten

### Verfügbarkeit

Research Preview. Benötigt v2.1.154+. Verfügbar auf allen bezahlten Plans, Anthropic API, Amazon Bedrock, Google Cloud Vertex AI, Microsoft Foundry. Auf Pro erst in `/config` aktivieren.

Quelle: `https://code.claude.com/docs/en/workflows` | offiziell | 2026-05-30

---

## Claude Opus 4.8 — Neues Standard-Modell (ab v2.1.154)

### Was ist Opus 4.8?

Das neue leistungsstärkste Modell von Anthropic. Verbesserte Coding-, Agentic- und Reasoning-Fähigkeiten bei gleichem Preis wie Opus 4.7. Kernmerkmal: Erhöhte Ehrlichkeit (flaggt Unsicherheiten, weniger unbelegte Aussagen). Läuft standardmäßig auf **High Effort**.

### Effort-Levels (neu geordnet in v2.1.154)

Die Labels wurden von "Speed"/"Intelligence" zu **"Faster"/"Smarter"** umbenannt. Es gibt jetzt 6 Stufen:

| Stufe | Kurzname | Wann nutzen |
|-------|----------|-------------|
| `low` | Faster | Schnelle, einfache Anfragen |
| `medium` | — | Routine-Arbeit |
| `high` | — | **Standard für Opus 4.8** — beste Balance Qualität/UX |
| `xhigh` | Smarter | Schwierige Tasks, lange Workflows |
| `ultracode` | — | xhigh + automatische Workflows (Sessions-Reset) |
| `max` | — | Höchste Token-Ausgabe |

**Wichtig für Frank:** `session-guard`-Hook setzt `high` bei echtem Neustart (nicht bei Compact/Resume). Manuelle Änderungen via `/effort <level>` bleiben bis Session-Ende erhalten.

### Opus 4.8 Fast Mode

- **Geschwindigkeit**: 2.5× schneller
- **Preis**: $10/M Input, $50/M Output (3× günstiger als zuvor)
- **Aktivierung**: `/model claude-opus-4-8` dann `/fast on`
- `CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE` ist deprecated — entfernt am 2026-06-01

### Modellwahl für Agents: Wann Opus 4.8 vs. Sonnet?

| Aufgabe | Empfehlung | Begründung |
|---------|------------|------------|
| Architektur, Design, Deep Debugging | **Opus 4.8** | Tiefes Reasoning, urteilt zuverlässiger |
| Dynamic Workflows (Orchestrierung) | **Opus 4.8** | Agentic-Stärke, bessere Aufgaben-Dekomposition |
| Code Review (Sicherheit) | **Opus 4.8** | Erkennt Sicherheitslücken zuverlässiger |
| Schnelle Implementierung (Coder-Agents) | **Sonnet** | Schnell, fokussiert, günstig |
| Researcher-Agents | **Sonnet** | Web-Lookup braucht kein tiefes Reasoning |
| Explore-Subagents (Codebase-Scan) | **Haiku** | Billig + schnell für reine Suche |
| Parallele Implementierung (3-5 Coder) | **Sonnet** | Sweet Spot für Masse-Parallelisierung |

**Aktualisierte Frontmatter-Empfehlung:**
```yaml
# Statt claude-sonnet-4-5 / claude-opus-4-5 jetzt:
model: claude-opus-4-8   # für Architect, Debugger, Code-Reviewer
model: claude-sonnet-4-6  # für Coder, Researcher, Batch-Reviewer
```

Quelle: `https://www.anthropic.com/news/claude-opus-4-8` | offiziell | 2026-05-30
Quelle: `https://code.claude.com/docs/en/changelog` | offiziell | 2026-05-30

---

## Background Shell Commands (NEU ab v2.1.154)

### Was

In `claude agents` können Shell-Commands als attachable/detachable Background-Sessions gestartet werden:

```bash
# Im claude agents Dispatch-Input:
! <command>

# Als CLI-Flag:
claude --bg --exec '<command>'
```

### Best Practices

- Für lang laufende Prozesse (Builds, Tests, Server) die weiter laufen sollen wenn man detacht
- `claude --bg --exec` wenn man einen Background-Prozess direkt beim CLI-Start anhängen will
- Separates Monitoring via `/workflows` oder `claude agents` — der Prozess bleibt im System auch wenn man die Session verlässt
- **Nicht verwechseln** mit `background: true` in Agent-Frontmatter (das ist für automatische Background-Agents)

Quelle: `https://code.claude.com/docs/en/changelog` (v2.1.154) | offiziell | 2026-05-30

---

## `agent`-Feld in `settings.json` (NEU ab v2.1.157)

### Was

Dispatched Sessions (Background-Agents) können jetzt einen Standard-Agent-Namen aus `settings.json` erhalten. Der `--agent <name>`-Flag überschreibt die Setting.

```json
// ~/.claude/settings.json oder .claude/settings.json
{
  "agent": "researcher"
}
```

```bash
# Override beim Starten:
claude --agent coder
```

### Best Practices

- Projekt-spezifischen Default-Agent in `.claude/settings.json` setzen wenn ein Projekt hauptsächlich einen Agent-Typ nutzt
- Global `settings.json` unverändert lassen — Projekt-Setting überschreibt ohnehin
- Nützlich für CI/CD-Workflows wo derselbe Agent-Typ immer gebraucht wird

Quelle: `https://code.claude.com/docs/en/changelog` (v2.1.157) | offiziell | 2026-05-30

---

## `EnterWorktree` — Mid-Session-Switch (verbessert ab v2.1.157)

### Was

`EnterWorktree` kann jetzt zwischen Claude-verwalteten Worktrees **mitten in einer Session** wechseln — nicht mehr nur beim Start. Worktrees bleiben nach Session-Ende **entsperrt**, sodass `git worktree remove/prune` funktioniert.

### Was sich konkret ändert

**Vor v2.1.157:**
- `EnterWorktree` nur beim Session-Start möglich
- Worktrees blieben nach Session-Ende gesperrt → manuelle Bereinigung nötig

**Ab v2.1.157:**
- Mid-Session-Switch zwischen Worktrees möglich
- Nach Session-Ende: Worktree entsperrt → `git worktree remove .claude/worktrees/<name>` direkt möglich

### Best Practices

- Bei parallelen Agents mit `isolation: worktree`: keine manuelle Bereinigung mehr nötig nach Session-Ende
- `git worktree prune` kann jetzt ohne Fehler laufen nach Abschluss einer Agent-Session
- **Frank's Workflow**: Nach parallelen Coder-Agents in Worktrees direkt mit `git worktree list` prüfen ob alle entsperrt sind

Quelle: `https://code.claude.com/docs/en/changelog` (v2.1.157) | offiziell | 2026-05-30

---

## Streaming Tool Execution (immer aktiv ab v2.1.154)

### Was

Streaming Tool Execution war bisher hinter einem Feature-Flag. Ab v2.1.154 ist es **immer aktiv** — auch auf Bedrock, Vertex und Foundry.

### Bedeutung für Agent-Workflows

- Tool-Aufrufe von Subagents werden sofort sichtbar gestreamt (nicht erst am Ende des Tool-Calls)
- Schnelleres Feedback bei langen Tool-Chains
- Kein Setup mehr nötig; gilt automatisch für alle Agents

Quelle: `https://code.claude.com/docs/en/changelog` (v2.1.154) | offiziell | 2026-05-30

---

## Frontmatter-Felder (vollständige Referenz — aktualisiert)

### Was
Subagents werden als Markdown-Dateien in `~/.claude/agents/` (global) oder `.claude/agents/` (Projekt) definiert. Die YAML-Frontmatter steuert Verhalten, Modell, Tools und Isolation.

### Alle Felder (aktuell)

| Feld | Werte / Typ | Bedeutung |
|------|-------------|-----------|
| `name` | Pflicht, lowercase-hyphens | Eindeutiger Bezeichner für Routing |
| `description` | Pflicht, String | Routing-Beschreibung für automatische Delegation |
| `tools` | Allowlist, Array | Erlaubte Tools; alle geerbt wenn weggelassen |
| `disallowedTools` | Denylist, Array | Verbotene Tools (subtrahiert vom Pool) |
| `model` | `sonnet` / `opus` / `haiku` / Full-ID / `inherit` | Modell des Subagents; Default: inherit |
| `permissionMode` | `default` / `acceptEdits` / `auto` / `dontAsk` / `bypassPermissions` / `plan` | Permission-Level des Subagents |
| `maxTurns` | Integer | Maximale Turn-Anzahl |
| `skills` | Array von Skill-Referenzen | Beim Start vorgeladener Skill-Content |
| `mcpServers` | Server-Namen oder Inline-Definitionen | MCP-Server für diesen Subagent |
| `hooks` | Lifecycle-Hook-Definitionen | Subagent-spezifische Hooks |
| `memory` | `user` / `project` / `local` | Persistente Memory über Sessions |
| `background` | `true` / `false` | Als Hintergrundprozess starten |
| `effort` | `low` / `medium` / `high` / `xhigh` / `max` | Thinking-Budget des Subagents |
| `isolation` | `worktree` | Isolierter Git-Worktree für den Subagent |
| `color` | `red` / `blue` / `green` / `yellow` / `purple` / `orange` / `pink` / `cyan` | Farbe in der Agent-View |
| `initialPrompt` | String | Automatisch gesendeter erster Turn (für `--agent`-Modus) |

### Best Practices
- `name` und `description` präzise — Claude nutzt `description` für automatisches Routing
- `tools` gezielt einschränken (Principle of Least Privilege): Researcher-Agent braucht kein `Write`-Tool
- `model: haiku` für einfache Routing-/Analyse-Aufgaben; `model: claude-opus-4-8` für Architect/Debugger
- `effort` nur erhöhen wenn nötig — `xhigh`/`max` ist 3-5x teurer als `medium`
- `isolation: worktree` IMMER wenn Agent Dateien ändert und parallel zu anderen läuft (ab v2.1.157: Worktrees automatisch entsperrt)
- Plugin-Subagents (in `plugins/`): Können KEINE `hooks`, `mcpServers` oder `permissionMode` nutzen

Quelle: `https://code.claude.com/docs/en/sub-agents` | offiziell | Stand 2026-05-30

---

## MCP-Server in Agent-Frontmatter (Policy-Enforcement ab v2.1.153)

### Was — Sicherheitslücke geschlossen

**Ab v2.1.153** respektieren Frontmatter-MCP-Server dieselben Policies wie alle anderen MCP-Server (`--strict-mcp-config`, `--bare`-Modus, Remote-Mode, Enterprise Managed MCP Config, Allow/Deny-Policies).

**`--strict-mcp-config`**: Entfernt inline `mcpServers` NICHT mehr aus explizit übergebenen Agent-Definitionen (`--agents`/SDK). Geblockte Subagent-MCP-Server erzeugen jetzt sichtbare Warnung statt stilles Scheitern.

### Best Practices
- Enterprise-Umgebungen: Auf v2.1.153+ aktualisieren — alte Policy-Umgehung war Sicherheitsrisiko
- Nach Upgrade: Neue Warnungen zeigen welche Frontmatter-MCP-Server bisher unbemerkt geblockt waren

Quelle: `https://code.claude.com/docs/en/changelog` (v2.1.153) | offiziell | 2026-05-28

---

## `subagent_type: 'claude'` — Worktree-Bug behoben (v2.1.153)

**Vor v2.1.153**: Outputs in `.gitignore`-Pfade gingen still verloren (temporärer Worktree-Bug).
**Ab v2.1.153**: Behoben. Outputs in gitignored Pfade zuverlässig.

Quelle: `https://code.claude.com/docs/en/changelog` (v2.1.153) | offiziell | 2026-05-28

---

## Modell-Auflösung (Model Resolution)

Die Prioritätskette (höchste zuerst):

1. `CLAUDE_CODE_SUBAGENT_MODEL` Umgebungsvariable
2. Per-Invocation `model`-Parameter
3. Frontmatter `model`-Feld
4. Eltern-Modell (erbt aufrufende Session)

### Best Practices
- `CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-4-6` für Kosten-optimierte Sessions (alle Subagents auf Sonnet)
- Frontmatter für permanente Agent-Definitionen; Per-Invocation für situative Überschreibungen
- Spezialisierte Agents (Debugger, Architekt) mit `model: claude-opus-4-8` festlegen
- Researcher-Agents: `model: claude-sonnet-4-6`

Quelle: `https://code.claude.com/docs/en/sub-agents` | offiziell | 2026-05-30

---

## Built-in Subagents

| Typ | Modell | Zweck |
|-----|--------|-------|
| `Explore` | Claude Haiku | Schnelles Erkunden von Codebase, Datei-Suche |
| `Plan` | Hauptmodell | Aufgabenplanung und Schritt-für-Schritt-Strategie |
| `general-purpose` | CLAUDE_CODE_SUBAGENT_MODEL oder Hauptmodell | Allgemeine Delegation |

- NIEMALS `subagent_type: "mein-custom-agent"` — Custom Agents mit `"general-purpose"` + eigenem Prompt
- `subagent_type: "claude"` für direkte Claude-Instanzen (ab v2.1.153: ohne Worktree-Bug)

Quelle: `https://code.claude.com/docs/en/sub-agents` | offiziell | 2026-05-30

---

## Agent-Tool (früher: Task-Tool)

Das `Agent`-Tool (umbenannt von `Task` in v2.1.63) startet Subagents in eigenem Kontext ohne Zugriff auf Eltern-Konversation.

### Best Practices
- Subagents IMMER vollständigen Kontext mitgeben — sie erben NICHTS von der Eltern-Session
- Mehrere unabhängige Agent-Tool-Aufrufe in EINEM Antwortblock für Parallelisierung
- Datei-Ownership strikt trennen: Zwei Agents NIEMALS dieselbe Datei gleichzeitig
- `isolation: worktree` verhindert Datei-Konflikte automatisch

Quelle: `https://code.claude.com/docs/en/sub-agents` | offiziell | 2026-05-30

---

## Parallelisierung

### Best Practices
- **3-5 parallele Agents** ist der Sweet Spot für normale Subagents — mehr bringt kaum Geschwindigkeit
- **Dynamic Workflows** wenn 16-100+ parallele Agents gebraucht werden
- Parallele Tool-Calls (Bash, Read, Glob, Grep) in einem Block immer besser als sequenziell
- Muster: 3-5 `coder`-Agents (Sonnet) für Implementierung, dann 1 `code-reviewer` (Opus 4.8)
- Bei Batch-Migrationen: `/batch`-Command (bis 10x schneller, eigene Git-Worktrees)

```yaml
# Beispiel: Paralleler Coder-Agent mit Isolation
---
name: coder
description: Implementiert Code-Änderungen in isoliertem Worktree
model: claude-sonnet-4-6
isolation: worktree
tools:
  - Read
  - Write
  - Edit
  - Bash
---
```

Quelle: `https://code.claude.com/docs/en/sub-agents` | offiziell | 2026-05-30

---

## Agent Teams (Experimentell)

- Lead-Agent koordiniert via `TaskCreate`, Teammates können untereinander kommunizieren
- Nur nutzen wenn Teilaufgaben voneinander **abhängen** (3-4x teurer als normale Subagents)
- `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` benötigt
- Windows: Kein Split-Screen (braucht tmux), Teammates laufen trotzdem

Quelle: `https://code.claude.com/docs/en/agent-teams` | offiziell | 2026-05-28

---

## Agent View / Background Agents

- `background: true` in Frontmatter → immer als Hintergrundprozess
- `color`-Feld für Unterscheidbarkeit in Agent-View
- Ab v2.1.154: Agent View auch auf Bedrock/Vertex/Foundry ohne Telemetry
- Ab v2.1.157: Claude-verwaltete Worktrees nach Session-Ende entsperrt

Quelle: `https://code.claude.com/docs/en/agent-view` | offiziell | 2026-05-30

---

## Memory-Persistenz (`memory`-Feld)

| Wert | Scope | Persistenz |
|------|-------|-----------|
| `user` | Global | Über alle Projekte |
| `project` | Projektbezogen | Nur im aktuellen Projekt |
| `local` | Session-lokal | Nur aktuelle Session |

- `memory: project` für Agents die projektspezifisches Wissen aufbauen (z.B. `code-reviewer`)
- `memory: user` nur für Agents die globale Präferenzen lernen sollen

Quelle: `https://code.claude.com/docs/en/sub-agents` | offiziell | 2026-05-30

---

## Zusammenfassung: Status aller Features

| Feature | Version | Status |
|---------|---------|--------|
| Dynamic Workflows (`/workflows`, bis 1.000 Agents) | v2.1.154 | Research Preview |
| Claude Opus 4.8 (High Effort Default) | v2.1.154 | Stabil |
| Background Shell Commands (`! <cmd>`, `--bg --exec`) | v2.1.154 | Stabil |
| `/effort ultracode` (xhigh + Auto-Workflows) | v2.1.154 | Stabil |
| Streaming Tool Execution immer aktiv | v2.1.154 | Stabil |
| `agent`-Feld in `settings.json` | v2.1.157 | Stabil |
| `EnterWorktree` mid-session | v2.1.157 | Stabil |
| Worktrees nach Session-Ende entsperrt | v2.1.157 | Bugfix |
| Auto-Mode auf Bedrock/Vertex/Foundry (Opus 4.7/4.8) | v2.1.158 | Stabil |
| MCP-Server Frontmatter Policy-Enforcement | v2.1.153 | Bugfix (Security) |
| `subagent_type:'claude'` Worktree-Bug | v2.1.153 | Bugfix |
| Agent View / Background Agents | v2.1.139+ | Research Preview → Stabil |
| Agent Teams | v2.1.32+ | Experimentell |
| `isolation: worktree` | Früher | Stabil |

---

## Externe Quellen (ergänzend, nicht offiziell bestätigt)

> `extern` — DevelopersIO (https://dev.classmethod.jp/en/articles/20260529-claude-code-updates-v2-1-154/)

- Dynamic Workflows cap bei 1.000 Subagents verhindert endlose Schleifen
- Empfehlung: `View raw script` nutzen vor dem ersten Run um Script zu verstehen
- `/effort ultracode` spart Zeit bei komplexen Tasks, ist aber deutlich teurer

> `extern` — ChatForest Builder's Log (https://chatforest.com/builders-log/claude-opus-4-8-dynamic-workflows-parallel-subagents-builder-architecture/)

- Workflow-Script ist lesbares JavaScript — kann nach Speichern inspiziert und angepasst werden
- Adversarial Review als Workflow-Pattern: Agents prüfen gegenseitig Ergebnisse bevor sie gemeldet werden

> `extern` — PubNub Blog (https://www.pubnub.com/blog/best-practices-for-claude-code-sub-agents/)

- Single-Responsibility-Prinzip: Jeder Agent macht genau EINE Sache gut
- Permission Hygiene: Minimale Tool-Berechtigungen pro Agent
- `maxTurns` setzen um endlose Loops zu verhindern
- Agent-Antworten strukturiert formatieren (JSON) für bessere Weiterverarbeitung

---

<!-- CHECKPOINT: fertig — alle Changelog-Einträge 2.1.154–2.1.158 integriert. Nächste Recherche bei Version 2.1.163+ oder wenn Dynamic Workflows aus Research Preview geht. -->

---

### Update 2026-06-05 (Claude Code 2.1.165) — Agents & Workflows

(Fixes laut Changelog ueberwiegend 2.1.161-2.1.163.)

**1. `--tools`: Grep/Glob auf nativen Builds jetzt wirksam (2.1.162/163)**
- **Was:** Auf nativen macOS/Linux-Builds (Embedded Search) wurden `Grep`/`Glob` in `--tools`-Listen still ignoriert; jetzt liefern sie die dedizierten Such-Tools.
- **Best Practice:** In eigenen Agent-Definitionen (`~/.claude/agents/*.md`, Frontmatter `tools:`) Grep und Glob explizit nennen, wenn der Agent auf macOS laufen soll. Haelt zudem den Start-Sockel schlank (nur gelistete Tool-Schemas geladen → verstaerkt `subagent-crash-proofing`).
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**2. Worktree-Isolation gefixt (2.1.161)**
- **Was:** Workflow-Agents mit `isolation:"worktree"` in Background-Sessions durften ihre eigenen Worktree-Dateien nicht editieren — gefixt.
- **Best Practice:** `isolation:"worktree"` ist jetzt zuverlaessig fuer parallele Coder-Agents an verschiedenen Dateien.
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**3. Background-Sessions: Stale-Modell gefixt (2.1.161)**
- **Was:** Background-Sessions booteten auf dem Modell aus der Daemon-Env statt aus `settings.json` — `CLAUDE_CODE_SUBAGENT_MODEL` wurde ignoriert. Jetzt ist `settings.json` die Modell-Wahrheit.
- **Best Practice:** Die Policy `CLAUDE_CODE_SUBAGENT_MODEL = opus[1m]` (abgesichert via `session-guard`) greift jetzt konsistent auf allen Agent-Typen, auch Background.
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**4. `claude agents --json` Feld `waitingFor` (2.1.162)** — zeigt, worauf eine blockierte Session wartet (z.B. Permission-Prompt). Nuetzlich fuer Monitoring-Scripts. `[offiziell]`

**Betrifft eigene Werkzeuge:** Punkt 1 rechtfertigt, in eigenen Agent-Definitionen `Grep`/`Glob` explizit in die `tools:`-Whitelist aufzunehmen — besonders auf macOS (vorher faktisch tot). Verstaerkt zugleich den Crash-Schutz (schlanker Start-Sockel).
