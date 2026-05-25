# Best Practices: Agents / Subagents / Agent Teams

> Stand: 2026-05-25 | Claude Code v2.1.150
> Quellen: code.claude.com/docs/en/sub-agents, code.claude.com/docs/en/agent-teams (offiziell)

---

## Was sind Subagents?

Subagents sind spezialisierte Claude-Instanzen die vom Hauptagenten als Werkzeug-Aufruf
gestartet werden. Seit v2.1.63 heißt das Werkzeug offiziell **Agent-Tool** (vorher: Task-Tool).
Der alte Name `Task(...)` funktioniert weiterhin als Alias.

Subagent-Definitionen liegen in `~/.claude/agents/<name>.md` (global) oder
`.claude/agents/<name>.md` (projektlokal). Claude Code scannt Unterverzeichnisse rekursiv.

---

## Subagent-Frontmatter — vollständige Feldreferenz

Alle Felder sind optional außer `name` und `description` (stark empfohlen).

| Feld | Typ | Bedeutung |
|------|-----|-----------|
| `name` | string | **Pflicht für Identifikation.** Interner Bezeichner |
| `description` | string | **Empfohlen.** Wann dieser Agent genutzt werden soll |
| `model` | string | Modell-Override für diesen Agent (z.B. `claude-opus-4-5`) |
| `effort` | string | `low`, `medium`, `high`, `xhigh` |
| `tools` | list | Werkzeuge die der Agent nutzen darf |
| `allowed-tools` | list | Alternative Schreibweise für `tools` |
| `isolation` | string | `worktree` = eigener Git-Worktree (isoliert) |
| `memory` | string | Speicherbereich: `project`, `session`, `none` |
| `background` | bool | `true` = Agent läuft im Hintergrund ohne Kontrolle zurückzugeben |
| `skills` | list | Skills die beim Agent-Start vorgeladen werden |
| `context` | string | Kontext-Modus (meist `fork` für frischen Context) |
| `initialPrompt` | string | System-Prompt der vor dem eigentlichen Prompt eingefügt wird |
| `color` | string | Farbe in der Agent-Ausgabe (Terminal-Farbe) |
| `hooks` | map | Agent-spezifische Hooks |
| `mcpServers` | map | MCP-Server die der Agent nutzen darf |

---

## Modell-Auflösungsreihenfolge (Priorität absteigend)

```
1. CLAUDE_CODE_SUBAGENT_MODEL Umgebungsvariable
2. Expliziter model-Parameter im Agent-Tool-Aufruf
3. model-Feld im Frontmatter der Agent-Definition
4. Modell des Elternagenten (geerbt)
```

**Umgebungsvariable setzen:**
```bash
export CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-4-5
```

---

## Eingebaute Subagents (Built-ins)

Claude Code liefert diese Agents ohne Konfiguration mit:

| Agent-Typ | Modell | Zweck |
|-----------|--------|-------|
| `explore` | claude-haiku-3-5 | Schnelles Erkunden von Codebasen |
| `plan` | (geerbt) | Aufgabenplanung und Strukturierung |
| `general-purpose` | (geerbt, alle Tools) | Standard-Subagent mit allen Werkzeugen |
| `statusline-setup` | claude-sonnet-4-5 | Terminal-Statuszeile konfigurieren |
| `claude-code-guide` | claude-haiku-3-5 | Fragen zu Claude Code beantworten |

---

## Agent-Tool-Aufruf (`Agent(...)`)

```python
# Standard-Aufruf
Agent(
    agent_type="coder",
    prompt="Implementiere die Datenbankschicht für UserRepository.kt",
    context={"files": ["src/data/User.kt"]}
)

# Mit explizitem Modell
Agent(
    agent_type="code-reviewer",
    model="claude-opus-4-5",
    prompt="Führe einen Sicherheits-Review durch"
)

# Allowlist-Syntax: nur bestimmte Agenten erlaubt
# Im Frontmatter: tools: [Agent(coder), Agent(researcher)]
```

---

## CLI-Flag `--agents` (JSON-Syntax)

```bash
claude --agents '[
  {
    "name": "analyzer",
    "description": "Analysiert Code auf Performance-Probleme",
    "model": "claude-opus-4-5",
    "prompt": "Du bist ein Performance-Experte..."
  }
]'
```

Gleiche Felder wie Frontmatter, plus `prompt` für den System-Prompt.

---

## Isolierung via `isolation: worktree`

```yaml
---
name: coder
description: Implementiert Features in isolation
isolation: worktree
---
```

Mit `isolation: worktree` erhält der Agent eine eigene Git-Worktree-Kopie. So können
mehrere `coder`-Agents parallel an verschiedenen Dateien arbeiten ohne Konflikte.

**Wann nutzen:**
- Mehrere Agents arbeiten an unterschiedlichen Features gleichzeitig
- Änderungen sollen erst nach Review in main fließen
- Riskante Experimente die nicht den main-Branch verschmutzen sollen

---

## Cross-Session Memory via `memory`

```yaml
---
name: code-reviewer
description: Führt Code-Reviews durch und lernt dabei
memory: project
---
```

| Wert | Bedeutung |
|------|-----------|
| `project` | Memory persistiert pro Projekt über Sessions hinweg |
| `session` | Memory gilt nur für die aktuelle Session |
| `none` | Kein persistentes Memory (Standard) |

---

## Hintergrund-Agents (`background: true`)

```yaml
---
name: logger
description: Protokolliert im Hintergrund
background: true
---
```

Mit `background: true` kehrt die Kontrolle sofort zum Hauptagenten zurück ohne auf das
Ergebnis des Subagents zu warten. Sinnvoll für Logging, Monitoring, nicht-blockierende
Analyse.

---

## Skills vorladen (`skills:`)

```yaml
---
name: android-coder
description: Android-Entwickler mit vorgeladenem Android-Skill
skills:
  - android-dev
  - kotlin-best-practices
---
```

Vorgeladene Skills stehen dem Subagent vom ersten Token an zur Verfügung.

---

## Plugin-Subagents: Einschränkungen

Plugin-Subagents (Identifier-Format: `plugin-name:subfolder:agent-name`) haben folgende
Einschränkungen gegenüber normalen Subagents:

- `hooks` — **nicht unterstützt** in Plugin-Subagents
- `mcpServers` — **nicht unterstützt** in Plugin-Subagents
- `permissionMode` — **nicht unterstützt** in Plugin-Subagents

---

## Parallelisierungs-Muster

### Parallele Tool-Calls (kein Agent nötig)
```python
# Mehrere unabhängige Operationen in einem Antwortblock
[Read("datei1.kt"), Read("datei2.kt"), Glob("**/*.xml")]
```

### Parallele Subagents (Sweet Spot: 3–5)
```python
# Qualitätsschleife: alle drei gleichzeitig
[
    Agent(agent_type="tester", prompt="..."),
    Agent(agent_type="code-reviewer", prompt="..."),
    Agent(agent_type="optimizer", prompt="...")
]
```

### Implementierung: 3–5 coder-Agents (Sonnet) parallel
```python
# Schnell und günstig — Sonnet für Implementation
[
    Agent(agent_type="coder", model="claude-sonnet-4-5",
          prompt="Implementiere Model-Schicht in User.kt"),
    Agent(agent_type="coder", model="claude-sonnet-4-5",
          prompt="Implementiere View-Schicht in UserScreen.kt"),
    Agent(agent_type="coder", model="claude-sonnet-4-5",
          prompt="Implementiere Repository in UserRepository.kt")
]
# Danach: 1 code-reviewer (Opus) für Qualitätsprüfung
```

**Datei-Ownership-Regel**: Zwei Agents dürfen NIEMALS dieselbe Datei gleichzeitig bearbeiten.

---

## Speed Tiers — Richtiges Modell für die richtige Aufgabe

| Aufgabe | Agent | Empfohlenes Modell | Warum |
|---------|-------|-------------------|-------|
| Architektur, Design | `architect` | claude-opus-4-5 | Tiefes Reasoning |
| Debugging | `debugger` | claude-opus-4-5 | Komplexe Ursachenanalyse |
| Code Review (Security) | `code-reviewer` | claude-opus-4-5 | Sicherheitslücken erkennen |
| Performance-Optimierung | `optimizer` | claude-opus-4-5 | Systemweites Verständnis |
| UI-Verbesserung | `ui-polisher` | claude-opus-4-5 | Design-Expertise |
| Implementation | `coder` | claude-sonnet-4-5 | Schnell, fokussiert, günstig |
| Bulk-Reviews | `batch-reviewer` | claude-sonnet-4-5 | Viele Dateien schnell prüfen |
| Tests schreiben | `tester` | claude-opus-4-5 | Qualität bei Tests wichtig |
| Recherche | `researcher` | claude-sonnet-4-5 | Schnelles Web-Lookup |

**Faustregel**: Opus denkt, Sonnet macht. 3–5 Sonnet-Agents für Implementation,
dann 1 Opus-Agent für Qualitätsprüfung.

---

## Parallele MCP-Initialisierung (ab 2026-04-24)

Seit dem 24. April 2026 initialisieren Subagents ihre MCP-Server parallel statt
sequenziell. Das reduziert die Startzeit bei Agents mit mehreren MCP-Servern
deutlich — besonders bei 5+ parallelen Agents ein messbarer Geschwindigkeitsvorteil.

---

## Agent Teams (Experimentell)

> **Voraussetzung**: `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` Umgebungsvariable + v2.1.32+

Agent Teams ermöglichen es mehreren Agents **miteinander zu kommunizieren** — nicht nur
Ergebnisse zurückzugeben wie normale Subagents.

### Architektur

```
Team Lead (Hauptagent)
  ├── Shared Task List (~/.claude/tasks/)
  ├── Mailbox (asynchrone Nachrichten)
  ├── Teammate 1 (spezialisierter Agent)
  ├── Teammate 2 (spezialisierter Agent)
  └── Teammate 3 (spezialisierter Agent)
```

### Neue Hooks für Teams

| Hook | Wann | Zweck |
|------|------|-------|
| `TeammateIdle` | Teammate wartet auf Aufgaben | Nächste Aufgabe zuweisen |
| `TaskCreated` | Neue Aufgabe in Task-Liste | Routing-Entscheidungen |
| `TaskCompleted` | Aufgabe abgeschlossen | Ergebnis verarbeiten |

### Settings für Teams

```json
{
  "teammateMode": "in-process"
}
```

### Display-Modi

| Modus | Terminal | Anforderung |
|-------|----------|-------------|
| `in-process` | Jedes Terminal | Kein Sonderbedarf |
| `split-panes` | tmux / iTerm2 | Nicht auf Windows Terminal |

### Best Practices für Teams

- **3–5 Teammates** (Sweet Spot — mehr bringt kaum Vorteil, kostet aber 3–4× mehr)
- **5–6 Aufgaben pro Teammate** für optimale Auslastung
- **Datei-Ownership strikt trennen** — kein Teammate editiert Dateien eines anderen
- **Teams NUR wenn Kommunikation nötig** — bei unabhängigen Aufgaben normale
  parallele Subagents verwenden (4× günstiger)

### Bekannte Einschränkungen

- **Keine Session-Wiederaufnahme** mit `in-process`-Modus
- **Keine geschachtelten Teams** (kein Team innerhalb eines Teams)
- **Kein Split-Panes-Modus** auf Windows Terminal

### Wann Teams vs. normale Subagents?

| Szenario | Empfehlung |
|----------|-----------|
| Frontend braucht Backend-API-Entscheidung | **Agent Team** (Kommunikation nötig) |
| 5 Researcher für verschiedene Themen | **Normale Subagents** (unabhängig) |
| Qualitätsschleife: Test + Review + Optimize | **Normale Subagents** (unabhängig) |
| Feature mit abhängigen Frontend/Backend-Teilen | **Agent Team** |
| Übersetzen in 5 Sprachen gleichzeitig | **Normale Subagents** |

---

## Kontext-Weitergabe an Subagents

**Wichtig**: Subagents erben NICHT die Konversations-Historie des Elternagenten.
Alles Wichtige muss im `prompt`-Parameter mitgegeben werden:

```python
Agent(
    agent_type="coder",
    prompt=f"""
    Projekt: BestJournalAndroid (Android-App in Kotlin/Compose)
    Aufgabe: Implementiere UserRepository.kt
    
    Konventionen:
    - MVVM-Architektur
    - Room-Datenbank
    - Coroutines für Async
    
    Betroffene Dateien: {", ".join(files)}
    
    Detaillierte Aufgabe:
    {actual_task}
    """
)
```

---

## Changelog-Highlights (Agents)

| Version / Datum | Änderung |
|-----------------|---------|
| v2.1.63 | Task-Tool → Agent-Tool umbenannt (`Task(...)` bleibt Alias) |
| v2.1.32 | Agent Teams eingeführt (experimentell) |
| 2026-04-24 | Parallele MCP-Initialisierung für Subagents |
| 2026-01 | `isolation: worktree`, `memory`-Feld, `background`-Feld |
| 2025-12 | Background Agents, `skills`-Preloading |

---

## Best Practice Zusammenfassung

1. **3–5 parallele Agents** sind der Sweet Spot — mehr bringt kaum Gewinn
2. **Sonnet für Implementation, Opus für Qualität** — so billig wie möglich so gut wie nötig
3. **`isolation: worktree`** bei parallelen coder-Agents die verschiedene Features bauen
4. **`memory: project`** für lernende Agents (code-reviewer der Muster erkennt)
5. **Kontext großzügig mitgeben** — Agents wissen nichts aus dem Hauptchat
6. **Agent Teams nur bei echter Kommunikation** — 4× teurer als normale Subagents
7. **`background: true`** für Logging/Monitoring das nicht blockieren soll
8. **Datei-Ownership heilig halten** — nie zwei Agents auf dieselbe Datei
9. **`CLAUDE_CODE_SUBAGENT_MODEL`** um alle Subagents auf ein bestimmtes Modell zu setzen
10. **`Agent(agent_type)` Allowlist** im `tools`-Feld um nur bestimmte Agent-Typen zu erlauben

---

> Quellen:
> - [sub-agents — code.claude.com](https://code.claude.com/docs/en/sub-agents) (offiziell)
> - [agent-teams — code.claude.com](https://code.claude.com/docs/en/agent-teams) (offiziell)
> Externe/unbestätigte Angaben: `color`-Feld, `initialPrompt`-Feld (in Doku gesehen, Verhalten extern/unbestaetigt)
