# Agents — Best Practices (Stand 2026-05-28, Claude Code 2.1.153)

> Recherche-Datum: 2026-05-28 | Claude Code Version: 2.1.153
> Quellen: 5 Web-Fetches (4 offiziell, 1 extern) + 4 Web-Searches

---

## Frontmatter-Felder (vollständige Referenz)

### Was
Subagents werden als Markdown-Dateien in `~/.claude/agents/` (global) oder `.claude/agents/` (Projekt) definiert. Die YAML-Frontmatter steuert Verhalten, Modell, Tools und Isolation.

### Alle 17 Felder (offiziell)

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
| `mcpServers` | Server-Namen oder Inline-Definitionen | MCP-Server für diesen Subagent (siehe Sektion unten) |
| `hooks` | Lifecycle-Hook-Definitionen | Subagent-spezifische Hooks |
| `memory` | `user` / `project` / `local` | Persistente Memory über Sessions |
| `background` | `true` / `false` | Als Hintergrundprozess starten |
| `effort` | `low` / `medium` / `high` / `xhigh` / `max` | Thinking-Budget des Subagents |
| `isolation` | `worktree` | Isolierter Git-Worktree für den Subagent |
| `color` | `red` / `blue` / `green` / `yellow` / `purple` / `orange` / `pink` / `cyan` | Farbe in der Agent-View |
| `initialPrompt` | String | Automatisch gesendeter erster Turn (für `--agent`-Modus) |

### Best Practices
- `name` und `description` präzise formulieren — Claude nutzt `description` für automatisches Routing
- `tools` gezielt einschränken (Principle of Least Privilege): ein Researcher-Agent braucht kein `Write`-Tool
- `model: haiku` für einfache Routing-/Analyse-Aufgaben; `opus` nur wenn tiefes Reasoning wirklich nötig
- `effort` nur erhöhen wenn nötig — `xhigh`/`max` ist 3-5x teurer als `medium`
- `isolation: worktree` IMMER wenn der Agent Dateien ändert und parallel zu anderen Agents läuft
- Plugin-Subagents (in `plugins/`): Können KEINE `hooks`, `mcpServers` oder `permissionMode` nutzen (Sicherheitseinschränkung)

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-28

---

## MCP-Server in Agent-Frontmatter (NEU v2.1.153)

### Was — Sicherheitslücke geschlossen

**Vor v2.1.153** ignorierten MCP-Server die im Subagent-Frontmatter (`mcpServers`-Feld) definiert wurden alle Sicherheits- und Verwaltungsrichtlinien:
- `--strict-mcp-config` wurde ignoriert
- `--bare`-Modus wurde ignoriert
- Remote-Mode-Richtlinien wurden ignoriert
- Enterprise Managed MCP Config wurde ignoriert
- Managed-Settings MCP Server Allow/Deny-Policies wurden ignoriert

**Ab v2.1.153** respektieren Frontmatter-MCP-Server dieselben Policies wie alle anderen MCP-Server.

### Was — `--strict-mcp-config`-Verhalten präzisiert

**Vor v2.1.153**: `--strict-mcp-config` entfernte fälschlicherweise inline `mcpServers` aus explizit übergebenen Agent-Definitionen (`--agents` / SDK `agents`).

**Ab v2.1.153**: `--strict-mcp-config` entfernt inline `mcpServers` NICHT mehr aus explizit übergebenen Agent-Definitionen. Gesperrte Subagent-MCP-Server erzeugen jetzt eine **sichtbare Warnung** statt still zu scheitern.

### Best Practices (ab v2.1.153)

- **Enterprise-Umgebungen**: Sofort auf v2.1.153+ aktualisieren — die alte Policy-Umgehung war ein Sicherheitsrisiko
- **Frontmatter `mcpServers`**: Nur Server definieren die auch unter `--strict-mcp-config` erlaubt sind; andernfalls sieht man jetzt die Warnung
- **Blocked-Server-Warnungen**: Neue Warnungen nach Update auf v2.1.153 zeigen, welche Frontmatter-MCP-Server bisher unbemerkt geblockt wurden
- **SDK-Nutzung**: Bei `--agents` oder SDK `agents`-Parameter mit inline `mcpServers` weiterhin erlaubt unter `--strict-mcp-config` — war vorher fälschlicherweise geblockt

```yaml
# Beispiel: Agent mit Frontmatter-MCP-Server (v2.1.153+: respektiert --strict-mcp-config)
---
name: web-researcher
description: Recherchiert im Web via Brave Search MCP
model: claude-sonnet-4-5
mcpServers:
  brave-search:
    command: npx
    args: ["-y", "@modelcontextprotocol/server-brave-search"]
    env:
      BRAVE_API_KEY: "${BRAVE_API_KEY}"
tools:
  - mcp__brave-search__search
---
```

### Quelle
Offiziell: `https://code.claude.com/docs/en/changelog` (v2.1.153) | Stand: 2026-05-28

---

## `subagent_type: 'claude'` — Worktree-Bug behoben (NEU v2.1.153)

### Was — Der Bug

**Vor v2.1.153**: Das Agent-Tool mit `subagent_type: 'claude'` lief in einem **undokumentierten temporären Worktree**. Das bedeutete:
- Outputs die in `.gitignore`-Pfade geschrieben wurden, gingen **still und lautlos verloren**
- Keine Fehlermeldung — der Agent glaubte, er habe erfolgreich geschrieben
- Betroffen: alle Dateipfade die in `.gitignore` oder globalen Git-Ignore-Regeln ausgeschlossen sind (z.B. `build/`, `dist/`, `.env`, `node_modules/`, `*.tmp`)

**Ab v2.1.153**: Der temporäre Worktree wird nicht mehr verwendet. Outputs in gitignored Pfaden gehen nicht mehr verloren.

### Best Practices (ab v2.1.153)

- **Upgrade-Pflicht** wenn Agents Outputs in gitignored Pfade schreiben: Unbedingt auf v2.1.153+ aktualisieren
- **Audit nach Upgrade**: Workflows prüfen die vor v2.1.153 komisch/still scheiterten — manche waren von diesem Bug betroffen
- **Agent-Outputs in gitignored Pfade**: Jetzt zuverlässig. Beispiele: temporäre Build-Artefakte, Cache-Dateien, generierte Reports in `output/` (wenn gitignored)
- **Vorher-Workaround (jetzt obsolet)**: Outputs explizit in versionierte Pfade schreiben um den Bug zu umgehen — das ist nicht mehr nötig
- **`subagent_type: 'claude'`** vs `'general-purpose'`: `'claude'` ist der native Subagent-Typ für direkte Claude-Instanzen; `'general-purpose'` für Custom Agents aus `~/.claude/agents/`

### Checkliste: Bin ich vom Bug betroffen gewesen?

| Symptom | Wahrscheinlichkeit |
|---------|-------------------|
| Agent schreibt in `build/`, `dist/`, `out/`, `tmp/` und nichts erscheint | Hoch |
| Agent schreibt `.env`-Dateien oder Konfigurationen die gitignored sind | Hoch |
| Agent meldet Erfolg aber erzeugte Dateien sind nicht auffindbar | Hoch |
| Agent schreibt nur in versionierte Pfade (`src/`, `lib/`, etc.) | Nicht betroffen |

### Quelle
Offiziell: `https://code.claude.com/docs/en/changelog` (v2.1.153) | Stand: 2026-05-28

---

## `claude agents` — Autocomplete verbessert (NEU v2.1.153)

### Was

Ab v2.1.153 schlägt die Autocomplete-Funktion im Dispatch-Input von `claude agents` nicht mehr nur Projekt-Skills vor, sondern auch:
- **Native Slash Commands** (z.B. `/help`, `/model`, `/effort`, etc.)
- **Bundled Skills** (eingebaute Skills die ohne Installation verfügbar sind)
- Weiterhin: Projekt-Skills aus `.claude/skills/`

### Best Practices

- **Exploratives Arbeiten**: Im Dispatch-Input `/` eingeben und Tab-Completion nutzen — zeigt jetzt alle verfügbaren nativen Commands
- **Bundled Skills entdecken**: In `claude agents` mit Tab sehen was out-of-the-box verfügbar ist, ohne in Dateiverzeichnissen nachschauen zu müssen
- **Background-Agent-Workflows**: `claude agents` + verbessertes Autocomplete erleichtert das schnelle Dispatchen von Tasks an laufende Agents

### Quelle
Offiziell: `https://code.claude.com/docs/en/changelog` (v2.1.153) | Stand: 2026-05-28

---

## Modell-Auflösung (Model Resolution)

### Was
Die Modellwahl für einen Subagent folgt einer festen Prioritätskette (höchste zuerst):

1. **`CLAUDE_CODE_SUBAGENT_MODEL`** Umgebungsvariable — überschreibt alles
2. **Per-Invocation `model`-Parameter** — im Agent-Tool-Aufruf übergeben
3. **Frontmatter `model`-Feld** — in der Agent-Definition
4. **Eltern-Modell** — erbt das Modell der aufrufenden Session

### Best Practices
- `CLAUDE_CODE_SUBAGENT_MODEL` nutzen um alle Subagents einer Session auf Haiku zu setzen (Kosten sparen)
- Frontmatter-`model` für permanente Agent-Definition verwenden; Per-Invocation für situative Überschreibungen
- Spezialisierte Agents (Debugger, Architekt) fest mit `model: claude-opus-4-5` definieren — die brauchen immer Opus
- Researcher-Agents fest mit `model: claude-sonnet-4-5` — schnell und günstig

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-28

---

## Built-in Subagents

### Was
Claude Code v2.1.153 hat drei eingebaute Subagent-Typen die automatisch für bestimmte Aufgaben ausgelöst werden:

| Typ | Modell | Zweck |
|-----|--------|-------|
| `Explore` | Claude Haiku | Schnelles Erkunden von Codebase, Datei-Suche, Kontext sammeln |
| `Plan` | Hauptmodell | Aufgabenplanung und Schritt-für-Schritt-Strategie |
| `general-purpose` | CLAUDE_CODE_SUBAGENT_MODEL oder Hauptmodell | Allgemeine Delegation ohne spezifischen Agent-Typ |

### Best Practices
- `Explore` nutzen für parallele Datei-Scans über große Codebases (Haiku = billig + schnell)
- Eigene Agents mit `subagent_type: "general-purpose"` starten wenn kein spezifischer registrierter Typ existiert
- NIEMALS `subagent_type: "mein-custom-agent"` — Custom Agents immer mit `"general-purpose"` + eigenem Prompt
- `subagent_type: "claude"` für direkte Claude-Instanzen (kein Custom-Agent-Frontmatter) — ab v2.1.153 ohne Worktree-Bug

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-28

---

## Agent-Tool (früher: Task-Tool)

### Was
Das `Agent`-Tool (umbenannt von `Task` in v2.1.63) ist der primäre Mechanismus zum Starten von Subagents. Subagents laufen in eigenem Kontext-Fenster ohne Zugriff auf die Konversations-History des Eltern-Agents.

### Best Practices
- Subagents IMMER vollständigen Kontext mitgeben — sie erben NICHTS von der Eltern-Session
- Mehrere unabhängige Agent-Tool-Aufrufe in EINEM Antwortblock für Parallelisierung
- Datei-Ownership strikt trennen: Zwei Agents dürfen NIEMALS dieselbe Datei gleichzeitig bearbeiten
- Ergebnisse des Subagents werden als Text zurückgegeben — kein direkter State-Share
- `isolation: worktree` verhindert Datei-Konflikte bei paralleler Bearbeitung automatisch
- **Ab v2.1.153**: Outputs in gitignored Pfaden gehen nicht mehr verloren (Worktree-Bug-Fix)

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-28

---

## Parallelisierung

### Was
Claude Code unterstützt echte Parallelisierung durch mehrere gleichzeitige Agent-Tool-Aufrufe in einem Antwortblock. Mit `isolation: worktree` erhält jeder Agent einen isolierten Git-Worktree.

### Best Practices
- **3-5 parallele Agents** ist der Sweet Spot — mehr als 5 bringt kaum Geschwindigkeit, aber viel mehr Token-Kosten
- Parallele Tool-Calls (Bash, Read, Glob, Grep) in einem Block sind immer besser als sequenziell
- Muster: 3-5 `coder`-Agents (Sonnet) für parallele Implementierung, dann 1 `code-reviewer` (Opus)
- Bei Batch-Migrationen: `/batch`-Command nutzen (bis zu 10x schneller, eigene Git-Worktrees)
- `worktree.bgIsolation`-Setting steuert Datei-Isolation für Background-Agents

```yaml
# Beispiel: Paralleler Coder-Agent mit Isolation
---
name: coder
description: Implementiert Code-Änderungen in isoliertem Worktree
model: claude-sonnet-4-5
isolation: worktree
tools:
  - Read
  - Write
  - Edit
  - Bash
---
```

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-28

---

## Agent Teams (Experimentell)

### Was
Agent Teams (ab v2.1.32, aktiviert via `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`) ermöglichen strukturierte Multi-Agent-Koordination mit Lead-Agent und Teammates. Teammates können untereinander kommunizieren — im Gegensatz zu normalen Subagents die nur Ergebnisse zurückgeben.

### Architektur
- **Lead-Agent**: Koordiniert, verteilt Aufgaben via `TaskCreate`
- **Teammates**: Erhalten Aufgaben, arbeiten unabhängig, melden Ergebnisse zurück
- **Hooks**: `TeammateIdle`, `TaskCreated`, `TaskCompleted` für Lifecycle-Management
- **`teammateMode`**: `in-process` / `tmux` / `auto` — steuert wie Teammates gestartet werden

### Best Practices
- Nur einsetzen wenn Teilaufgaben **voneinander abhängen** und Kommunikation zwischen Agents nötig ist
- Für unabhängige Aufgaben immer normale parallele Subagents nutzen (3-4x günstiger)
- Datei-Ownership strikt zuweisen: Jeder Teammate bekommt eigene Dateien
- 5-6 Tasks pro Teammate als Richtwert
- Windows-Limitation: Kein Split-Screen (braucht tmux), Teammates laufen trotzdem korrekt
- Keine verschachtelten Teams (Team innerhalb Team) — nicht unterstützt

### Limitations (offiziell)
- Experimentell — API kann sich ändern
- `tmux`-Modus erfordert tmux-Installation
- Nested Agent Teams nicht unterstützt
- Signifikant höhere Token-Kosten (3-4x vs. normale Subagents)

### Quelle
Offiziell: `https://code.claude.com/docs/en/agent-teams` | Stand: 2026-05-28

---

## Agent View / Background Agents (Research Preview)

### Was
Ab v2.1.139 gibt es "Agent View" — eine Supervision-Schicht für Hintergrund-Agents. Via `claude agents`-Befehl können Agents als eigenständige Prozesse mit eigenem Worktree laufen.

### Konfiguration
- `background: true` in Frontmatter → Agent läuft immer als Hintergrundprozess
- `color`-Feld gibt dem Agent eine Farbe in der Agent-View
- Jede Background-Agent-Session bekommt eigenen Worktree (Datei-Isolation)
- `worktree.bgIsolation` in `settings.json` steuert Isolation-Verhalten
- Ab v2.1.153: `claude agents` PR-Spalte zeigt `PR #N` oder `N PRs` (mehrere PRs)

### Best Practices
- Background-Agents für lang laufende Aufgaben nutzen (Builds, umfangreiche Recherchen)
- `initialPrompt` nutzen um Background-Agent sofort bei Start eine Aufgabe zu geben
- `color` vergeben damit mehrere gleichzeitige Agents in der View unterscheidbar sind
- Research Preview: Nicht für produktionskritische Workflows nutzen
- Ab v2.1.153: Verbessertes Autocomplete im Dispatch-Input (native Slash Commands + bundled Skills)

### Quelle
Offiziell: `https://code.claude.com/docs/en/agent-view` | Stand: 2026-05-28

---

## Memory-Persistenz (`memory`-Feld)

### Was
Das `memory`-Feld steuert welche Memory-Ebene ein Subagent lesen und schreiben kann:

| Wert | Scope | Persistenz |
|------|-------|-----------|
| `user` | Global für den Nutzer | Über alle Projekte |
| `project` | Projektbezogen | Nur im aktuellen Projekt |
| `local` | Session-lokal | Nur aktuelle Session |

### Best Practices
- `memory: project` für Agents die projektspezifisches Wissen aufbauen (z.B. `code-reviewer`)
- `memory: user` nur für Agents die globale Präferenzen lernen sollen
- Code-Reviewer-Agents mit `memory: project` definieren — lernen über Sessions

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-28

---

## Zusammenfassung der wichtigsten Neuerungen

| Feature | Version | Status |
|---------|---------|--------|
| MCP-Server in Frontmatter respektieren `--strict-mcp-config` | v2.1.153 | Bugfix (Security) |
| `subagent_type:'claude'` Worktree-Bug behoben (gitignored Outputs) | v2.1.153 | Bugfix (kritisch) |
| `claude agents` Autocomplete: native Commands + bundled Skills | v2.1.153 | Verbesserung |
| `--strict-mcp-config` entfernt Frontmatter-MCP nicht mehr aus SDK-Agents | v2.1.153 | Bugfix |
| Blocked Subagent-MCP-Server → sichtbare Warnung statt stilles Scheitern | v2.1.153 | Verbesserung |
| `Task`-Tool umbenannt zu `Agent`-Tool | v2.1.63 | Stabil |
| Agent Teams | v2.1.32+ | Experimentell |
| Agent View / Background Agents | v2.1.139+ | Research Preview |
| `color`-Frontmatter-Feld | v2.1.139+ | Stabil |
| `initialPrompt`-Feld | v2.1.139+ | Stabil |
| `isolation: worktree` | Früher | Stabil |
| `effort`-Feld | Früher | Stabil |

---

## Externe Quellen (ergänzend, nicht offiziell bestätigt)

> `extern` — PubNub Blog (https://www.pubnub.com/blog/best-practices-for-claude-code-sub-agents/)

- Single-Responsibility-Prinzip: Jeder Agent macht genau EINE Sache gut
- Permission Hygiene: Minimale Tool-Berechtigungen pro Agent
- Hooks für Lifecycle-Management nutzen (Pre-Task, Post-Task)
- Agent-Outputs validieren bevor sie in nachfolgende Agents fließen
- Explizite Fehlerbehandlung im Agent-Prompt definieren
- `maxTurns` setzen um endlose Loops zu verhindern
- Agent-Antworten strukturiert formatieren (JSON oder klare Sektionen) für bessere Weiterverarbeitung
- Testen mit einzelnem Agent bevor Parallelisierung

> `extern` — DevelopersIO (https://dev.classmethod.jp/en/articles/20260528-claude-code-updates-v2-1-153/)

- Enterprise-Umgebungen: Priorität-Upgrade auf v2.1.153+ wegen MCP-Policy-Enforcement und Credential-Fixes
- Worktree-Bug-Betroffene: Audit aller Workflows die vor v2.1.153 still scheiterten
