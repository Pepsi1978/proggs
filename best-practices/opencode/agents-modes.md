# Agents, Custom Agents, Modes, Subagents & Permissions — Best Practices (Stand 2026-06-18, OpenCode CLI)

> Quellen: `offiziell` (opencode.ai/docs) + geprüfte externe Quellen. Gilt für **Windows und macOS**.

> **Wichtige Namensänderung (2026):** „Modes" (Plan/Build) sind ins **Agent**-System überführt. Die alte
> `mode`-Konfiguration (`"mode": {...}`, Ordner `modes/`) ist **deprecated**, funktioniert aber noch.
> Neuer kanonischer Weg: alles über den `agent`-Block bzw. den Frontmatter-Schlüssel `mode`
> (`primary`/`subagent`/`all`). `offiziell` (opencode.ai/docs/modes, /agents)

---

## 1. Agent-Konzept: Primary Agents vs. Subagents `offiziell`

| Typ | Was es ist | Aufruf |
|-----|-----------|--------|
| **Primary Agents** | Hauptassistenten, mit denen man direkt redet | **Tab** durchschalten (`agent_cycle`, rückwärts `shift+tab`) oder `@`-Mention |
| **Subagents** | Spezialisierte Helfer für Teilaufgaben, laufen in **Child-Sessions** | Automatisch durch das Hauptmodell (Task-Tool) ODER manuell `@agent-name` |

### Eingebaute Agents `offiziell`

| Agent | Mode | Was er macht |
|-------|------|-------------|
| **build** | `primary` | Standard-Primary. Alle Tools aktiv. Für die eigentliche Entwicklungsarbeit. |
| **plan** | `primary` | Eingeschränkt. Datei-Edits + `bash` per Default auf **`ask`** → analysieren/planen ohne ungewollte Änderungen. |
| **general** | `subagent` | Allzweck, komplexe Multi-Step-Recherche, voller Tool-Zugriff außer `todo`. Läuft in separater Child-Session. |
| **explore** | `subagent` | Schnell, **read-only** Codebase-Erkundung. Kann keine Dateien ändern. |
| **scout** | `subagent` | **read-only** externe Docs/Dependency-Recherche; kann Dependency-Repos in den Cache klonen. |
| **compaction / title / summary** | `primary`, **hidden** | System-Agents (Kompaktierung / Session-Titel / Zusammenfassungen). Laufen automatisch. |

> build vs. general: Beide (fast) voller Zugriff, aber **general läuft in einer Child-Session** → die
> Hauptsession bleibt sauber. `extern`

---

## 2. Plan-Modus vs. Build-Modus `offiziell`

| | **Build** | **Plan** |
|---|-----------|----------|
| Zweck | Entwicklung, volle Umsetzung | Analyse, Review, Pläne — ohne ungewollte Änderungen |
| Tools | Alle aktiv | Eingeschränkt (read-only-artig) |

Im Plan-Modus standardmäßig gesperrt: `write`, `edit` (Ausnahme: `.opencode/plans/*.md`), `patch`, `bash`.
In der neuen Agents-Doc: für `plan` sind `file edits` + `bash` auf **`ask`**.

**Umschalten:** `Tab` (`agent_cycle`; rückwärts `shift+tab`). Agent-Liste `<leader>a`. Default-Agent per
`"default_agent": "plan"` (muss Primary sein, sonst Fallback `build`).

> **Tipp:** „Use the plan agent to analyze code and review suggestions without making any code changes." `offiziell`

---

## 3. Custom Agents definieren — JSON UND Markdown `offiziell`

### A) Per `opencode.json` — `agent`-Block
```json
{ "$schema": "https://opencode.ai/config.json",
  "agent": {
    "code-reviewer": {
      "description": "Reviews code for best practices and potential issues",
      "mode": "subagent",
      "model": "anthropic/claude-sonnet-4-5",
      "prompt": "You are a code reviewer. Focus on security, performance, and maintainability.",
      "tools": { "write": false, "edit": false }
    }
  },
  "default_agent": "build" }
```

> **WARNUNG vor falscher Blog-Syntax:** Manche Blogs zeigen ein **Array** `"agents": [ { "name":...,
> "systemPrompt":..., "permissions": {"read":true,"write":false} } ]`. Das ist **NICHT** die aktuelle
> Syntax. Korrekt: **Objekt** `"agent": { "<name>": {...} }`, Schlüssel `prompt` (nicht `systemPrompt`),
> Permission-Keys `edit`/`bash`/`webfetch` (nicht read/write/execute/network als Booleans). `offiziell`

### B) Per Markdown-Datei
Ablage (Plural ist Standard): Global `~/.config/opencode/agents/*.md`; Projekt `.opencode/agents/*.md`.
**Der Dateiname wird zum Agent-Namen** — `review.md` → `@review`.
```markdown
---
description: Reviews code for quality and best practices
mode: subagent
model: anthropic/claude-sonnet-4-20250514
temperature: 0.1
permission:
  edit: deny
  bash: deny
---

You are in code review mode. Focus on:
- Code quality and best practices
- Potential bugs and edge cases
- Performance implications
- Security considerations
Provide constructive feedback without making direct changes.
```
YAML-Frontmatter = Konfiguration, Markdown-Body = System-Prompt.

### Interaktiv erstellen
```bash
opencode agent create
```
Fragt: global/projekt → Beschreibung → generiert Prompt + Identifier → erlaubte Permissions auswählen
(Nicht-Gewähltes wird `deny`) → legt die Markdown-Datei an.

### Vollständige Feldliste (Frontmatter / JSON) `offiziell`

| Feld | Pflicht? | Werte | Bedeutung |
|------|----------|-------|-----------|
| `description` | **Ja** | String | Was der Agent tut + wann nutzen. Hilft bei automatischer Auswahl. |
| `mode` | Nein (default `all`) | `primary`\|`subagent`\|`all` | Rolle des Agents |
| `model` | Nein | `provider/model-id` | Modell-Override. Subagents ohne Angabe **erben das Modell des aufrufenden Primary**. |
| `temperature` | Nein | 0.0–1.0 | 0.0–0.2 fokussiert (Analyse/Plan/Review), 0.6–1.0 kreativ |
| `prompt` | Nein | String oder `{file:./pfad.txt}` | Custom System-Prompt |
| `steps` | Nein | Integer | Max. agentische Iterationen (Kostenbremse). `maxSteps` deprecated → `steps`. |
| `disable` | Nein | bool | Agent abschalten |
| `permission` | Nein | Objekt | Pro-Agent-Permissions (mergen/überschreiben global) |
| `tools` | **deprecated** | `{toolname: true/false}` | Alte Tool-An/Aus → besser `permission`. Wildcards (`"mymcp_*": false`). |
| `hidden` | Nein | bool | Subagent aus `@`-Autocomplete verstecken (via Task-Tool weiter aufrufbar) |
| `color` | Nein | Hex/Theme-Farbe | Visuelle Kennzeichnung |
| `top_p` | Nein | 0.0–1.0 | Alternative zu `temperature` |
| *Additional* | Nein | beliebige Keys | Direkt an Provider durchgereicht (z.B. `reasoningEffort: "high"`, `textVerbosity: "low"`) |

---

## 4. Subagents aufrufen `offiziell`

**a) Automatisch** durch das Hauptmodell (Task-Tool) — stärkere Modelle delegieren intelligenter.
**b) Manuell per `@`-Mention:**
```
@general help me search for this function
@explore How many files are in the src/utils directory?
@scout Research the differences between version 3 and 4 of this library
@review Review the file src/auth/login.ts for vulnerabilities
```

**Session-Navigation:** `session_child_first` (`<leader>down`), `session_child_cycle` (`right`),
`session_child_cycle_reverse` (`left`), `session_parent` (`up`).

> **Einschränkung:** Nutzer können jeden Subagent direkt per `@` aufrufen, **auch wenn** dessen
> `task`-Permission das verbietet. Die Sperre wirkt nur auf die **automatische** Modell-Delegation. `offiziell`

---

## 5. Pro-Agent Modellwahl (Token-Spar-Relevanz) `offiziell`/`extern`

**Kernprinzip:** Teures Reasoning für komplexe Agents, billige/schnelle Modelle für simple/häufige Tasks.

| Aufgabe | Modell-Niveau | Begründung |
|---------|--------------|-----------|
| Planung / tiefes Reasoning | stark (z.B. Opus) | Braucht tiefes Reasoning |
| Code-Review | mittel (z.B. Sonnet) | Schnelles, akkurates Feedback |
| Formatierung / Titel / simple Tasks | günstig (z.B. Haiku) | simpel + häufig → billig |

- **`small_model`** setzt ein billiges Modell für Leichtgewicht-Tasks (`"small_model": "anthropic/claude-haiku-4-5"`).
- **Subagents erben das Primary-Modell** ohne eigene Angabe → günstige Subagents brauchen explizites `model`.
- **`steps`** begrenzt Iterationen → verhindert Token-Verbrennung.
```json
{ "agent": {
    "planner":       { "model": "anthropic/claude-opus-4-20250514" },
    "code-reviewer": { "model": "anthropic/claude-sonnet-4-20250514" },
    "formatter":     { "model": "anthropic/claude-haiku-4-20250514" } } }
```

---

## 6. Pro-Agent Tool-Beschränkung & Permissions `offiziell`

Werte: `"allow"` / `"ask"` / `"deny"`.

**Permission-Keys und gesteuerte Tools:**

| Key | Tools |
|-----|-------|
| `read` | `read` |
| `edit` | `write`, `edit`, `apply_patch` (alle Datei-Änderungen) |
| `glob` / `grep` / `list` | gleichnamige |
| `bash` | `bash` |
| `task` | `task` (Subagent-Start) |
| `external_directory` | Dateien außerhalb des Worktrees |
| `todowrite` | `todowrite`, `todoread` |
| `webfetch` / `websearch` / `lsp` / `skill` / `question` | gleichnamige |
| `doom_loop` | Recovery-Prompts bei 3× identischem Tool-Call |

**Granulare Objekt-Syntax** (letzte passende Regel gewinnt → Catch-all `"*"` zuerst):
```markdown
---
description: Code review without edits
mode: subagent
permission:
  edit: deny
  bash:
    "*": ask
    "git diff": allow
    "git log*": allow
    "grep *": allow
  webfetch: deny
---
Only analyze code and suggest changes.
```

**Wildcards:** `*`/`?`. Permission-Keys matchen auch gegen Tool-Namen → `"mymcp_*": "deny"` sperrt alle
Tools eines MCP-Servers.

**Defaults:** meist `"allow"`; `doom_loop` + `external_directory` = `"ask"`; `read` `"allow"` aber
`.env`-Dateien `deny`; **`todowrite` für Subagents standardmäßig aus.**

**Task-Permissions** (welche Subagents ein Agent spawnen darf):
```json
{ "agent": { "orchestrator": { "mode": "primary",
  "permission": { "task": { "*": "deny", "orchestrator-*": "allow", "code-reviewer": "ask" } } } } }
```

---

## 7. Sind Modes und Agents vereinheitlicht? (Stand 2026) — Ja `offiziell`

> „Modes are now configured through the `agent` option in the opencode config. The `mode` option is now deprecated."

- **Alt (deprecated):** `"mode": { "build": {...}, "plan": {...} }`, Markdown in `modes/*.md`, Tool-Booleans.
- **Neu (kanonisch):** alles im `agent`-Block; Frontmatter-Key `mode` = Rolle (`primary`/`subagent`/`all`).
  Build und Plan sind heute zwei eingebaute **Primary Agents.**

**Migrationsempfehlung:** Neue Setups ausschließlich über `agent` + `permission`.

---

## 8. Best Practices + komplette Beispiel-Agents

### Best-Practice-Regeln `offiziell`/`extern`
1. **Richtiger Agent für die richtige Aufgabe** — nicht alles mit `build`. Erkunden → `explore`, planen → `plan`.
2. **Restriktive Permissions per Default** (Review-Agent braucht kein `bash`, Research kein `edit: allow`).
3. **Niedrige Temperatur für technische Tasks** (0.0–0.2).
4. **Markdown für komplexe Agents** (lange Prompts, versioniert), JSON für simple.
5. **Ein Primary, viele Subagents** — nicht mehrere konkurrierende Primaries; lieber ein `orchestrator`-Primary.
6. **Modell nach Komplexität** (Opus/Sonnet/Haiku gestaffelt).
7. **Agents nach Erstellung testen** (`opencode --agent my-new-agent`).
8. **Spezifische statt vager System-Prompts** (Output-Format, Severity-Levels auflisten).

### Komplettes JSON-Setup (mehrere Agents)
```json
{ "$schema": "https://opencode.ai/config.json",
  "model": "anthropic/claude-sonnet-4-5",
  "small_model": "anthropic/claude-haiku-4-5",
  "default_agent": "build",
  "agent": {
    "plan": { "mode": "primary", "model": "anthropic/claude-haiku-4-20250514", "temperature": 0.1,
              "permission": { "edit": "deny", "bash": "deny" } },
    "reviewer": { "description": "Günstiger Code-Reviewer ohne Schreibrechte", "mode": "subagent",
                  "model": "anthropic/claude-sonnet-4-5", "temperature": 0.1, "color": "accent",
                  "permission": { "edit": "deny", "bash": { "*": "deny", "git diff": "allow", "git log*": "allow" }, "webfetch": "deny" } },
    "docs": { "description": "Schreibt und pflegt Projektdokumentation", "mode": "subagent", "permission": { "bash": "deny" } },
    "security-auditor": { "description": "Sicherheits-Audit", "mode": "subagent", "permission": { "edit": "deny" } }
  } }
```

### Komplette Markdown-Agent-Dateien (offizielle Vorlagen)

`~/.config/opencode/agents/docs-writer.md`:
```markdown
---
description: Writes and maintains project documentation
mode: subagent
permission:
  bash: deny
---
You are a technical writer. Create clear, comprehensive documentation.
Focus on: clear explanations, proper structure, code examples, user-friendly language.
```

`~/.config/opencode/agents/security-auditor.md`:
```markdown
---
description: Performs security audits and identifies vulnerabilities
mode: subagent
permission:
  edit: deny
---
You are a security expert. Look for: input validation vulnerabilities, auth/authz flaws,
data exposure risks, dependency vulnerabilities, configuration security issues.
```

---

## Plattform-Hinweise (Windows & macOS) `offiziell`
- **Config-/Agent-Pfade plattformgleich:** Global `~/.config/opencode/agents/*.md` + `~/.config/opencode/opencode.json`;
  Projekt `.opencode/agents/*.md`.
- **Windows:** läuft direkt, aber **WSL empfohlen**. Auf nativem Windows defaulten einige Keybinds anders
  (`input_undo` mit `ctrl+z`; `terminal_suspend` = `none`).
- **macOS:** Managed-Settings (Enterprise) unter `/Library/Application Support/opencode/` + MDM `ai.opencode.managed`.
- **Shell für Bash-Tool:** `"shell": "pwsh"` (Windows) bzw. Auto-Erkennung (macOS).

## Quellen
**Offiziell:** opencode.ai/docs/agents, /modes, /permissions, /config, /tools, /keybinds, /windows-wsl.
**Extern (geprüft):** arceapps.com (Subagents — konsistent mit offizieller Doc); docs.bswen.com (Best
Practices/Modellwahl, aber teils **veraltete Syntax** — als Negativbeispiel, nicht als Syntaxvorlage).
