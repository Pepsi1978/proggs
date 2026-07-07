# Konfigurationsdateien & vollständiges Schema — Best Practices (Stand 2026-06-18, OpenCode CLI)

> Quellen-Legende: `offiziell` = opencode.ai/docs oder das offizielle JSON-Schema `opencode.ai/config.json`.
> `extern` = Drittquelle (GitHub-Issues, Blogs). Gilt für **Windows und macOS**.

---

## 1. Format der Config-Dateien

OpenCode unterstützt **JSON und JSONC** (JSON mit `// Kommentaren` und Trailing Commas). `offiziell`
- Server-/Runtime-Config: `opencode.json` oder `opencode.jsonc`
- TUI-Config (Theme, Keybinds, Maus): eigene Datei `tui.json` / `tui.jsonc`
- `$schema`-Feld liefert Autovervollständigung/Validierung: `"$schema": "https://opencode.ai/config.json"`
  (TUI: `"https://opencode.ai/tui.json"`).

---

## 2. Speicherorte & Präzedenz (welche Config überschreibt welche)

### Präzedenz-Reihenfolge (spätere Quellen überschreiben frühere) `offiziell`

| Rang | Quelle | Pfad / Trigger |
|---|---|---|
| 1 (niedrigste) | Remote config | `.well-known/opencode`-Endpoint der Org |
| 2 | **Global config** | `~/.config/opencode/opencode.json` |
| 3 | Custom config | Env `OPENCODE_CONFIG=/pfad/datei.json` |
| 4 | **Projekt-config** | `opencode.json` im Projektroot |
| 5 | `.opencode`-Verzeichnisse | `.opencode/` (agents, commands, plugins …) |
| 6 | Inline config | Env `OPENCODE_CONFIG_CONTENT` |
| 7 | Managed (Datei) | macOS `/Library/Application Support/opencode/`; Linux `/etc/opencode/`; Windows `%ProgramData%\opencode` |
| 8 (höchste) | macOS Managed Preferences | `.mobileconfig` via MDM, Domain `ai.opencode.managed` |

**Wichtigstes Prinzip:** Configs werden **gemerged, nicht ersetzt.** Bei Konflikt gewinnt die spätere
Quelle nur für den konfliktbehafteten Schlüssel. `offiziell`

### Pfade je Betriebssystem

**macOS / Linux:** `offiziell`
- Global: `~/.config/opencode/opencode.json`; TUI `~/.config/opencode/tui.json`; Rules `~/.config/opencode/AGENTS.md`
- Mit `XDG_CONFIG_HOME`: `$XDG_CONFIG_HOME/opencode/opencode.json`
- Credentials: `~/.local/share/opencode/auth.json`; MCP-OAuth: `~/.local/share/opencode/mcp-auth.json`

**Windows** (Doku leicht widersprüchlich — deshalb ausführlich):
- Offizielle Doku nennt plattformübergreifend `~/.config/opencode/opencode.json` → auf Windows
  `%USERPROFILE%\.config\opencode\opencode.json` (also `C:\Users\<Name>\.config\opencode\...`). `offiziell`
- In der Praxis nutzen manche Komponenten `%APPDATA%\opencode\` oder `%LOCALAPPDATA%\opencode\` (offene
  Issues #8235, #251/#265). `extern`
- **Verlässlichste Methode:** `opencode debug paths` zeigt die exakt verwendeten Pfade (home, data, bin,
  log, cache, config, state). **Vor dem Anlegen der Datei ausführen.** `extern` (hungyi.net, Issue #1235)
- `XDG_CONFIG_HOME` wird auch auf Windows respektiert.
- Managed-Pfad Windows: `%ProgramData%\opencode` (Admin).

### Wichtige Pfad-/Verhaltens-Umgebungsvariablen `offiziell`

| Env-Var | Wirkung |
|---|---|
| `OPENCODE_CONFIG` | Pfad zu Custom-Config (zwischen global und projekt geladen) |
| `OPENCODE_CONFIG_DIR` | Custom-Verzeichnis, wie `.opencode` durchsucht; lädt nach global → kann überschreiben |
| `OPENCODE_CONFIG_CONTENT` | Inline-Config als JSON-String |
| `OPENCODE_TUI_CONFIG` | Pfad zu Custom-TUI-Config |
| `OPENCODE_DISABLE_CLAUDE_CODE` | Deaktiviert komplette `.claude`-Kompatibilität |
| `OPENCODE_DISABLE_CLAUDE_CODE_PROMPT` | Deaktiviert nur `~/.claude/CLAUDE.md` |
| `OPENCODE_DISABLE_CLAUDE_CODE_SKILLS` | Deaktiviert nur `.claude/skills` |

> **Unterverzeichnisse jetzt im PLURAL:** `agents/`, `commands/`, `modes/`, `plugins/`, `skills/`, `tools/`,
> `themes/`. Singular (`agent/`) wird nur aus Abwärtskompatibilität noch unterstützt. `offiziell`

### Variablen-Substitution in der Config `offiziell`
- **Env-Vars:** `{env:VARIABLE_NAME}` → z.B. `"apiKey": "{env:ANTHROPIC_API_KEY}"` (leer, falls nicht gesetzt).
- **Datei-Inhalt:** `{file:path/to/file}` → fügt Dateiinhalt ein. Relativ zum Config-Verzeichnis oder
  absolut (`/...`/`~/...`). Z.B. `"apiKey": "{file:~/.secrets/openai-key}"`. Ideal für Secrets/große Instructions.

---

## 3. Vollständiges Top-Level-Schema (alle Schlüssel aus `opencode.ai/config.json`) `offiziell`

| Schlüssel | Bedeutung | Beispielwert |
|---|---|---|
| `$schema` | Schema-Referenz | `"https://opencode.ai/config.json"` |
| `model` | Hauptmodell `provider/model` | `"anthropic/claude-sonnet-4-5"` |
| `small_model` | Günstiges Modell für Hilfstasks | `"anthropic/claude-haiku-4-5"` |
| `provider` | Custom-Provider & Modell-Overrides | `{ ... }` |
| `agent` | Agent-Konfiguration | `{ "code-reviewer": {...} }` |
| `mode` | **@deprecated** → durch `agent` ersetzt | |
| `default_agent` | Default-Agent (muss primär sein), Fallback `build` | `"plan"` |
| `mcp` | MCP-Server-Definitionen | `{ ... }` |
| `tools` | Tools global an/aus (bool je Tool/Glob) | `{ "write": false }` |
| `permission` | Berechtigungs-Regeln | `{ "edit": "ask", "bash": "ask" }` |
| `instructions` | Array zusätzlicher Regel-/Kontextdateien & Globs | `["CONTRIBUTING.md", ".cursor/rules/*.md"]` |
| `disabled_providers` | Provider-Blockliste | `["openai"]` |
| `enabled_providers` | Provider-Allowlist | `["anthropic", "openai"]` |
| `username` | Eigener Anzeigename | `"frank"` |
| `share` | `manual` (Default) / `auto` / `disabled` | `"manual"` |
| `autoshare` | **@deprecated** → `share` | |
| `autoupdate` | `true` / `false` / `"notify"` | `false` |
| `snapshot` | Datei-Snapshots für Undo/Redo (Default `true`); große Repos `false` | `false` |
| `shell` | Standard-Shell für Terminal & Bash-Tool | `"pwsh"` (Win) / `"/bin/zsh"` |
| `server` | Server-Optionen (für `serve`/`web`) | `{ "port": 4096 }` |
| `formatter` | `true`/`false`/Objekt | `true` |
| `lsp` | `true`/`false`/Objekt | `true` |
| `plugin` | npm-Plugins (String oder `[name, optionsObj]`) | `["opencode-helicone-session"]` |
| `watcher` | File-Watcher Ignore-Patterns (Glob) | `{ "ignore": ["node_modules/**"] }` |
| `compaction` | Kontext-Kompaktierung steuern | `{ "auto": true, "prune": false, "reserved": 10000 }` |
| `attachment` | Bild-Anhang-Limits & Auto-Resize | `{ "image": { "max_width": 2000 } }` |
| `command` | Custom-Commands (Templates) | `{ "test": { "template": "..." } }` |
| `skills` | Zusätzliche Skill-Pfade & URLs | `{ "paths": ["~/skills"] }` |
| `references` | Benannte Git-/lokale Referenzen | `{ "lib": { "repository": "..." } }` |
| `reference` | **@deprecated** → `references` | |
| `tool_output` | Schwellen, ab denen Tool-Output gekürzt wird | `{ "max_lines": 2000, "max_bytes": 51200 }` |
| `logLevel` | `DEBUG`/`INFO`/`WARN`/`ERROR` | `"INFO"` |
| `enterprise` | Enterprise-URL | `{ "url": "https://..." }` |
| `experimental` | Instabile Optionen (s. §10) | `{ "batch_tool": true }` |

> `theme` und `keybinds` als Top-Level in `opencode.json` sind **deprecated** → gehören in `tui.json`
> (Legacy wird automatisch migriert). `offiziell`

**Server-Block:** `port`, `hostname` (Default `0.0.0.0` bei aktivem mDNS), `mdns`, `mdnsDomain`
(`opencode.local`), `cors`.
**Compaction-Block:** `auto` (true), `prune` (false), `reserved`, `tail_turns` (2), `preserve_recent_tokens`.
**Attachment/Image:** `auto_resize` (true), `max_width` (2000), `max_height` (2000), `max_base64_bytes` (5242880).

---

## 4. `model` und `small_model` `offiziell`

- Format überall: **`provider_id/model_id`** — z.B. `"anthropic/claude-sonnet-4-5"`, `"opencode/gpt-5.1-codex"`.
- Bei Custom-Provider ist `provider_id` der Schlüssel aus dem `provider`-Block.
- `small_model` ist ein separates, günstigeres Modell für Lightweight-Tasks (Titel etc.). Default: OpenCode
  versucht automatisch ein billigeres Modell, sonst Fallback aufs Hauptmodell.
- **Auswahl-Priorität beim Start:** 1. CLI-Flag `--model`/`-m` → 2. `model` in der Config → 3. zuletzt
  benutztes Modell → 4. erstes Modell nach interner Priorität.
```json
{ "$schema": "https://opencode.ai/config.json",
  "model": "anthropic/claude-sonnet-4-5",
  "small_model": "anthropic/claude-haiku-4-5" }
```

---

## 5. `provider`-Block (eigene Provider/Modelle, baseURL, npm) `offiziell`

| Feld | Bedeutung |
|---|---|
| `provider.<id>` | Frei wählbare Provider-ID (= `/connect`-ID) |
| `npm` | AI-SDK-Paket: `@ai-sdk/openai-compatible` (für `/v1/chat/completions`), `@ai-sdk/openai` (für `/v1/responses`) |
| `name` | Anzeigename |
| `options.baseURL` | API-Endpoint |
| `options.apiKey` | Optional via `{env:VAR}`; nur nötig, wenn nicht `/connect` genutzt |
| `options.headers` | Custom-Header |
| `options.timeout` / `chunkTimeout` / `headerTimeout` | Timeouts (ms; Default 300000; `false`=aus) |
| `options.setCacheKey` | Prompt-Cache-Key erzwingen (Default `false`) |
| `models.<id>.name` / `.id` | Anzeigename / echte Model-ID überschreiben |
| `models.<id>.limit.context` / `.output` | Max. Input-/Output-Tokens (bei Custom-Providern selbst setzen!) |
| `models.<id>.options` / `.headers` / `.variants` | Modell-spezifisch |

**Vollständiges Custom-Provider-Beispiel:**
```json
{ "$schema": "https://opencode.ai/config.json",
  "provider": {
    "myprovider": {
      "npm": "@ai-sdk/openai-compatible",
      "name": "My AI Provider",
      "options": { "baseURL": "https://api.myprovider.com/v1", "apiKey": "{env:MY_API_KEY}" },
      "models": { "my-model-name": { "name": "My Model", "limit": { "context": 200000, "output": 65536 } } }
    } } }
```

**Bestehenden Provider nur überschreiben** (nur `options`):
```json
{ "provider": { "anthropic": { "options": { "baseURL": "https://api.anthropic.com/v1" } } } }
```

**Lokale Modelle (Ollama / LM Studio):**
```json
{ "provider": {
    "ollama": { "npm": "@ai-sdk/openai-compatible", "name": "Ollama (local)",
      "options": { "baseURL": "http://localhost:11434/v1" }, "models": { "llama2": { "name": "Llama 2" } } },
    "lmstudio": { "npm": "@ai-sdk/openai-compatible", "name": "LM Studio (local)",
      "options": { "baseURL": "http://127.0.0.1:1234/v1" }, "models": { "google/gemma-3n-e4b": {} } } } }
```

**Auth:** Keys über `/connect` (TUI) → `~/.local/share/opencode/auth.json`. `opencode auth list` zeigt sie.
Alternativ `options.apiKey` mit `{env:VAR}`. `disabled_providers` hat Vorrang vor `enabled_providers`.

---

## 6. `permission`-Block (edit/bash/webfetch — allow/ask/deny) `offiziell`

Drei Aktionen: `"allow"` (ohne Rückfrage), `"ask"` (Rückfrage), `"deny"` (blockiert).

**Drei Formen:**
```json
{ "permission": "allow" }                                          // global alles
{ "permission": { "*": "ask", "bash": "allow", "edit": "deny" } }  // pro Tool + Catch-all
{ "permission": {                                                  // granular (Pattern-Matching)
    "bash": { "*": "ask", "git *": "allow", "rm *": "deny" },
    "edit": { "*": "deny", "packages/web/src/content/docs/*.mdx": "allow" } } }
```

> **Regelauswertung:** Pattern-Matching, **die LETZTE passende Regel gewinnt** → Catch-all `"*"` zuerst,
> spezifischere Regeln danach. Wildcards: `*` (0+ Zeichen), `?` (1 Zeichen). `~`/`$HOME` am Anfang expandiert.

**Permission-Keys:** `read`, `edit` (deckt edit/write/patch), `glob`, `grep`, `list`, `bash`, `task`
(Subagents), `skill`, `lsp`, `question`, `webfetch`, `websearch`, `todowrite`, `external_directory`,
`doom_loop`.

**Defaults:** Die meisten Permissions sind `"allow"`; `doom_loop` + `external_directory` sind `"ask"`;
`read` ist `"allow"`, aber `.env`-Dateien sind per Default `deny`:
```json
{ "permission": { "read": { "*": "allow", "*.env": "deny", "*.env.*": "deny", "*.env.example": "allow" } } }
```

**External Directories** (außerhalb des Workspace):
```json
{ "permission": { "external_directory": { "~/projects/personal/**": "allow" },
                  "edit": { "~/projects/personal/**": "deny" } } }
```
Permissions sind **pro Agent überschreibbar** (Agent-Regeln gewinnen). Im Markdown-Agent via Frontmatter.

---

## 7. `agent`-Block & `instructions` `offiziell`

**Agents** (Schema `AgentConfig`): `model`, `variant`, `temperature`, `top_p`, `prompt`, `description`,
`mode` (`subagent`/`primary`/`all`), `hidden`, `disable`, `color`, `steps`, `permission`, `options`,
`tools` (deprecated → `permission`). Eingebaut: `plan`, `build`, `general`, `explore`, `title`, `summary`,
`compaction`. (Details in `agents-modes.md`.)

**Instructions & Rules:**
```json
{ "instructions": ["CONTRIBUTING.md", "docs/guidelines.md", ".cursor/rules/*.md", "packages/*/AGENTS.md",
  "https://raw.githubusercontent.com/org/rules/main/style.md"] }
```
- Array von Pfaden/Glob-Mustern; **Remote-URLs erlaubt** (5s-Timeout).
- AGENTS.md zentral; Fallbacks `CLAUDE.md` (Projekt) / `~/.claude/CLAUDE.md` (global) + `~/.claude/skills/`.
- Alle Instructions-Dateien werden **mit** den AGENTS.md kombiniert. (Details in `agents-md-memory.md`.)

---

## 8. `mcp`-Block (Kurzform; Details in `plugins-mcp-skills.md`) `offiziell`

**Local:** `type:"local"`, `command` (Array), `cwd`, `environment`, `enabled`, `timeout` (5000ms).
**Remote:** `type:"remote"`, `url`, `enabled`, `headers`, `oauth`, `timeout`.
```json
{ "mcp": {
    "mcp_everything": { "type": "local", "command": ["npx", "-y", "@modelcontextprotocol/server-everything"] },
    "context7": { "type": "remote", "url": "https://mcp.context7.com/mcp" } } }
```

---

## 9. `formatter` und `lsp` `offiziell`

Beide: weglassen/`false` = aus, `true` = Built-ins an, Objekt = Built-ins + Overrides/Custom.
```json
{ "formatter": {
    "prettier": { "disabled": true },
    "custom-prettier": { "command": ["npx", "prettier", "--write", "$FILE"], "extensions": [".js", ".ts"] } },
  "lsp": { "typescript": { "disabled": true } } }
```

---

## 10. `experimental`-Block (instabil) `offiziell`

Felder: `disable_paste_summary`, `batch_tool`, `openTelemetry`, `primary_tools`, `continue_loop_on_deny`,
`mcp_timeout`, `policies`. Policies steuern Provider-Zugriff:
```json
{ "experimental": { "policies": [ { "effect": "deny", "action": "provider.use", "resource": "openai" } ] } }
```

---

## 11. Beispiel: sinnvolle globale `~/.config/opencode/opencode.json`
```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "model": "anthropic/claude-sonnet-4-5",
  "small_model": "anthropic/claude-haiku-4-5",
  "username": "frank",
  "autoupdate": "notify",
  "share": "disabled",
  "shell": "pwsh",
  "instructions": ["~/.config/opencode/AGENTS.md"],
  "permission": {
    "*": "ask",
    "read": { "*": "allow", "*.env": "deny" },
    "bash": { "*": "ask", "git *": "allow", "grep *": "allow", "rm *": "deny" },
    "edit": "ask"
  },
  "provider": { "anthropic": { "options": { "apiKey": "{env:ANTHROPIC_API_KEY}" } } },
  "disabled_providers": ["openai"],
  "mcp": { "context7": { "type": "remote", "url": "https://mcp.context7.com/mcp" } }
}
```

## 12. Beispiel: projektspezifische `opencode.json` (committen!)
```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "instructions": ["CONTRIBUTING.md", "docs/guidelines.md", "packages/*/AGENTS.md"],
  "formatter": true,
  "lsp": true,
  "watcher": { "ignore": ["node_modules/**", "dist/**", ".git/**"] },
  "permission": { "bash": { "*": "ask", "npm run *": "allow", "git push *": "deny" } },
  "agent": {
    "reviewer": { "mode": "subagent", "model": "anthropic/claude-sonnet-4-5", "tools": { "write": false, "edit": false } }
  }
}
```

---

## 13. Best Practices `offiziell` / `extern`

- **Global vs. Projekt:** Persönliches (Hauptmodell, `username`, `autoupdate`, persönliche Rules) → **global**.
  Team-/projektbezogenes (instructions, Formatter, LSP, Bash-Permissions, Custom-Agents) → **projekt**.
- **Committen:** Projekt-`opencode.json` + `AGENTS.md` ins Git (gleiches Schema, team-weit geteilt).
- **NICHT committen / geheim halten:** API-Keys **niemals** im Klartext. Stattdessen `{env:VAR}` /
  `{file:~/.secrets/...}` oder `/connect` (speichert außerhalb des Repos).
- **`.env`-Schutz:** Default-`read`-Deny beibehalten.
- **Permission-Härtung:** Catch-all `"*": "ask"` zuerst, dann gezielte `allow`/`deny`. Destruktives explizit
  denyen (`"rm *": "deny"`, `"git push *": "deny"`).
- **MCP sparsam:** MCP-Server fressen Kontext-Tokens (besonders GitHub-MCP) → lieber pro Agent aktivieren.
- **Snapshots bei großen Repos:** `"snapshot": false` (Preis: kein UI-Undo).
- **Windows-Pfad zuerst verifizieren:** `opencode debug paths` — effektive Pfade variieren je nach
  Version/`XDG_CONFIG_HOME`/`APPDATA`. Bei Migration von Claude Code funktioniert `~/.claude/CLAUDE.md` als Fallback.

---

## Deprecated (nicht mehr verwenden, nur Altbestand)
`mode` → `agent`; `autoshare` → `share`; `reference` → `references`; `tools` (an/aus) → `permission`;
`maxSteps` → `steps`; `layout` (immer `stretch`); `theme`/`keybinds` Top-Level → `tui.json`.

## Quellen
**Offiziell:** opencode.ai/docs/config, opencode.ai/config.json (JSON-Schema, autoritativ),
/permissions, /rules, /models, /mcp-servers, /providers.
**Extern:** hungyi.net (OpenCode Configuration Path Discovery, Windows `opencode debug paths`);
GitHub-Issues #8235, #6669, #1235 (Windows-Pfad-Verhalten).

> Das offizielle JSON-Schema enthält mehr Schlüssel als die Doku-Seite (`logLevel`, `tool_output`, `skills`,
> `references`, `enterprise`, `compaction.tail_turns`, `experimental.batch_tool` …) — hier alle aus der
> autoritativen Quelle dokumentiert.
