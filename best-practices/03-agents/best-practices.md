# Best Practices: Agents / Subagents

> Recherche-Datum: 2026-05-25 | Claude Code Version: 2.1.150
> Quellen: 4 Web-Fetches (3 offiziell, 1 extern) + 3 Web-Searches

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
| `mcpServers` | Server-Namen oder Inline-Definitionen | MCP-Server für diesen Subagent |
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
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-25

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
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-25

---

## Built-in Subagents

### Was
Claude Code v2.1.150 hat drei eingebaute Subagent-Typen die automatisch für bestimmte Aufgaben ausgelöst werden:

| Typ | Modell | Zweck |
|-----|--------|-------|
| `Explore` | Claude Haiku | Schnelles Erkunden von Codebase, Datei-Suche, Kontext sammeln |
| `Plan` | Hauptmodell | Aufgabenplanung und Schritt-für-Schritt-Strategie |
| `general-purpose` | CLAUDE_CODE_SUBAGENT_MODEL oder Hauptmodell | Allgemeine Delegation ohne spezifischen Agent-Typ |

### Best Practices
- `Explore` nutzen für parallele Datei-Scans über große Codebases (Haiku = billig + schnell)
- Eigene Agents mit `subagent_type: "general-purpose"` starten wenn kein spezifischer registrierter Typ existiert
- NIEMALS `subagent_type: "mein-custom-agent"` — Custom Agents immer mit `"general-purpose"` + eigenem Prompt

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-25

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

### Quelle
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-25

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
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-25

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
Offiziell: `https://code.claude.com/docs/en/agent-teams` | Stand: 2026-05-25

---

## Agent View / Background Agents (Research Preview)

### Was
Ab v2.1.139 gibt es "Agent View" — eine Supervision-Schicht für Hintergrund-Agents. Via `claude agents`-Befehl können Agents als eigenständige Prozesse mit eigenem Worktree laufen.

### Konfiguration
- `background: true` in Frontmatter → Agent läuft immer als Hintergrundprozess
- `color`-Feld gibt dem Agent eine Farbe in der Agent-View
- Jede Background-Agent-Session bekommt eigenen Worktree (Datei-Isolation)
- `worktree.bgIsolation` in `settings.json` steuert Isolation-Verhalten

### Best Practices
- Background-Agents für lang laufende Aufgaben nutzen (Builds, umfangreiche Recherchen)
- `initialPrompt` nutzen um Background-Agent sofort bei Start eine Aufgabe zu geben
- `color` vergeben damit mehrere gleichzeitige Agents in der View unterscheidbar sind
- Research Preview: Nicht für produktionskritische Workflows nutzen

### Quelle
Offiziell: `https://code.claude.com/docs/en/agent-view` | Stand: 2026-05-25

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
Offiziell: `https://code.claude.com/docs/en/sub-agents` | Stand: 2026-05-25

---

## Zusammenfassung der wichtigsten Neuerungen (letzte 6 Monate)

| Feature | Version | Status |
|---------|---------|--------|
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
