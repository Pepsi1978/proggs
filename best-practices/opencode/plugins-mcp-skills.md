# Plugins, Custom Tools, MCP-Server, Skills & Commands — Best Practices (Stand 2026-06-18, OpenCode CLI)

> Quellen: `offiziell` = opencode.ai/docs oder GitHub `anomalyco/opencode`; `extern` = Community.
> Gilt für **Windows (nativ + WSL) und macOS**.

> **PLURAL-Pfade sind Standard:** `.opencode/` und `~/.config/opencode/` nutzen `agents/`, `commands/`,
> `modes/`, `plugins/`, `skills/`, `tools/`, `themes/`. Singular (`plugin/`, `tool/`) nur noch
> rückwärtskompatibel. Viele ältere Guides liegen hier falsch. `offiziell` (opencode.ai/docs/config)

---

## 1. MCP-Server `offiziell` (opencode.ai/docs/mcp-servers)

MCP-Server werden im `mcp`-Block der `opencode.json` definiert. **Alle Tools eines Servers erscheinen
automatisch** mit Servernamen-Präfix (z.B. `my-mcp_search`).

> **Kosten-Caveat (offiziell):** „When you use an MCP server, it adds to the context. This can quickly add
> up." Besonders der GitHub-MCP frisst sehr viele Tokens → sparsam einsetzen.

### Lokale Server (`type: "local"`)
```jsonc
{ "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "my-local-mcp-server": {
      "type": "local",
      "command": ["npx", "-y", "my-mcp-command"],
      "enabled": true,
      "environment": { "MY_ENV_VAR": "value" }
    } } }
```
Optionen: `type` (Pflicht), `command` (Array, Pflicht), `cwd`, `environment`, `enabled`, `timeout` (Default 5000ms).

Beispiele:
```jsonc
// Test-Server "everything"
{ "mcp": { "mcp_everything": { "type": "local", "command": ["npx", "-y", "@modelcontextprotocol/server-everything"] } } }
// Playwright (Browser-Automation)
{ "mcp": { "playwright": { "type": "local", "command": ["npx", "@playwright/mcp@latest"], "enabled": true } } }
// Filesystem
{ "mcp": { "filesystem": { "type": "local", "command": ["npx", "-y", "@modelcontextprotocol/server-filesystem", "/pfad/zum/projekt"], "enabled": true } } }
```
> **Windows:** `npx`/`bun` müssen im PATH sein; manche MCP-Server erwarten POSIX-Pfade → **WSL** empfohlen.

### Remote Server (`type: "remote"`)
```jsonc
{ "mcp": {
    "my-remote-mcp": { "type": "remote", "url": "https://my-mcp-server.com", "enabled": true,
      "headers": { "Authorization": "Bearer MY_API_KEY" } } } }
```
Optionen: `type` (Pflicht), `url` (Pflicht), `enabled`, `headers`, `oauth` (Object|false), `timeout`.

### OAuth (automatisch, RFC 7591 Dynamic Client Registration) `offiziell`
- **Automatisch:** nur `type`+`url`; bei 401 startet OpenCode den OAuth-Flow, Tokens in
  `~/.local/share/opencode/mcp-auth.json`.
- **Vorregistriert:** `oauth: { clientId, clientSecret, scope, callbackPort (19876), redirectUri }`.
- **Deaktivieren (API-Key-Server):** `"oauth": false` + `headers`.

CLI: `opencode mcp auth <name>`, `opencode mcp list`, `opencode mcp logout <name>`, `opencode mcp debug <name>`.

Praxisbeispiele: Sentry `{ "type": "remote", "url": "https://mcp.sentry.dev/mcp", "oauth": {} }`;
Context7 `{ "type": "remote", "url": "https://mcp.context7.com/mcp", "headers": { "CONTEXT7_API_KEY": "{env:CONTEXT7_API_KEY}" } }`;
Grep by Vercel `{ "type": "remote", "url": "https://mcp.grep.app" }`.

### MCP-Tools verwalten (global / per Agent / Glob)
```jsonc
{ "mcp": { "my-mcp": { "type": "local", "command": ["bun", "x", "my-mcp-command"], "enabled": true } },
  "tools": { "my-mcp*": false },                       // global aus
  "agent": { "my-agent": { "tools": { "my-mcp*": true } } } }  // nur für diesen Agenten an
```

---

## 2. Plugins (JS/TS) `offiziell` (opencode.ai/docs/plugins)

### Wo Plugins liegen / wie geladen
**Lokal:** Projekt `.opencode/plugins/`, global `~/.config/opencode/plugins/` (Windows nativ:
`%USERPROFILE%\.config\opencode\plugins\`).
**Aus npm** (`plugin`-Feld):
```jsonc
{ "$schema": "https://opencode.ai/config.json",
  "plugin": ["opencode-helicone-session", "opencode-wakatime", "@my-org/custom-plugin"] }
```
npm-Plugins werden beim Start automatisch via **Bun** installiert (Cache `~/.cache/opencode/node_modules/`).
Lade-Reihenfolge: globale Config → Projekt-Config → globales Plugin-Verzeichnis → Projekt-Plugin-Verzeichnis.

### Plugin rückstandslos deinstallieren
Es gibt **keinen** `opencode plugin remove`/`uninstall`-Befehl (Issue #30526). Ein per
`opencode plugin <name> --global` installiertes Plugin steckt an bis zu **fünf** Stellen, die
**alle** entfernt werden müssen — sonst lädt OpenCode es beim nächsten Start neu („loading plugins")
und zieht Cache + plugin-eigene Config wieder hoch:
1. **OpenCode schließen** (Prozess beenden, nicht nur Fenster) — sonst regeneriert es die gelöschten Dateien sofort. Sessions bleiben in der DB.
2. `plugin`-Eintrag aus **allen** Config-Dateien: `~/.config/opencode/opencode.jsonc` **und** `~/.config/opencode/tui.json` (`opencode plugin --global` schreibt den Eintrag auch nach `tui.json`!) + evtl. Projekt-`opencode.json`/`.opencode/`. Die `plugin`-Arrays werden über alle Config-Quellen **gemergt** — ein einziger übersehener Eintrag genügt.
3. Falls `~/.config/opencode/package.json` den Eintrag enthält (append-only bei `install --global`): `package.json` + `package-lock.json` + `node_modules` in `~/.config/opencode/` zurücksetzen — OpenCode baut sie beim Start nur aus der aktuellen Config neu auf.
4. Cache-Paket löschen: `~/.cache/opencode/packages/<scope>/` (Windows PowerShell `Remove-Item -Recurse -Force`; `rm -rf ~/…` ist per `bash-guard` blockiert).
5. Plugin-eigene Config löschen (z.B. `~/.config/opencode/dcp.jsonc`).
Endkontrolle: `grep -rl "<scope>" ~/.config/opencode/` leer **und** kein `<scope>` unter
`~/.cache/opencode/packages/`. Andere Plugins bleiben unberührt (funktionserhaltend). Bug-Details:
Almanach `bugs/opencode/opencode-cli.md` §7 #55d.

### Grundstruktur
Ein Plugin ist ein JS/TS-Modul, das Plugin-Funktionen exportiert. Jede Funktion bekommt ein **Kontext-Objekt**
(nicht einzelne Parameter!) und gibt ein Hooks-Objekt zurück:
```js
// .opencode/plugins/example.js
export const MyPlugin = async ({ project, client, $, directory, worktree }) => {
  return { /* Hooks */ }
}
```
Kontext-Felder: `client` (OpenCode-SDK), `project`, `directory`, `worktree`, `$` (Bun-Shell),
`serverUrl`, `experimental_workspace`.

> **Häufigster Fehler (`extern`):** Die Funktion empfängt ein **Kontext-Objekt**, nicht den Client direkt.
> Richtig `async ({ client, $ }) => …`, falsch `async (client) => …`.

TypeScript:
```ts
import type { Plugin } from "@opencode-ai/plugin"
export const MyPlugin: Plugin = async ({ project, client, $, directory, worktree }) => { return {} }
```
Externe Dependencies in lokalen Plugins: `package.json` ins Config-Verzeichnis (`.opencode/package.json`)
→ OpenCode führt beim Start `bun install` aus.

### Vollständige Hook-Liste (maßgeblich aus Quelltyp `Hooks`, GitHub) `offiziell`

| Hook | Zweck |
|------|-------|
| `event` | auf beliebige System-Events reagieren |
| `config` | Config beobachten/anpassen |
| `tool` | **eigene Tools** registrieren |
| `auth` | eigene Auth-Provider |
| `provider` | eigene Modell-Provider |
| `chat.message` | bei neuer Nachricht |
| `chat.params` | LLM-Parameter (temperature, topP, maxOutputTokens …) modifizieren |
| `chat.headers` | LLM-Request-Header setzen |
| `permission.ask` | Permission-Entscheidung beeinflussen |
| `command.execute.before` | vor Command-Ausführung |
| `tool.execute.before` | **vor** Tool-Ausführung (Args ändern/blocken) |
| `tool.execute.after` | **nach** Tool-Ausführung |
| `tool.definition` | Tool-Beschreibung/Schema ändern |
| `shell.env` | Umgebungsvariablen in jede Shell injizieren |
| `experimental.chat.messages.transform` | Nachrichtenliste transformieren |
| `experimental.chat.system.transform` | System-Prompt erweitern |
| `experimental.provider.small_model` | „small model" überschreiben |
| `experimental.session.compacting` | Kompaktierungs-Prompt anpassen |
| `experimental.compaction.autocontinue` | „continue"-Turn nach Compaction steuern |
| `experimental.text.complete` | Text-Vervollständigung anpassen |
| `dispose` | Aufräumen beim Beenden |

> Ein oft zitierter `stop`-Hook steht **nicht** im offiziellen Quelltyp (Stand dev-Branch Juni 2026) — mit Vorsicht.

**Event-Typen** (im `event`-Hook abonnierbar): `command.executed`; `file.edited`, `file.watcher.updated`;
`installation.updated`; `lsp.client.diagnostics`, `lsp.updated`; `message.*`; `permission.asked/replied`;
`server.connected`; `session.created/compacted/deleted/diff/error/idle/status/updated`; `todo.updated`;
`shell.env`; `tool.execute.before/after`; `tui.prompt.append`, `tui.command.execute`, `tui.toast.show`.

### Vollständige Plugin-Beispiele (offiziell)
```js
// Benachrichtigung bei Session-Ende (macOS)
export const NotificationPlugin = async ({ $ }) => ({
  event: async ({ event }) => {
    if (event.type === "session.idle")
      await $`osascript -e 'display notification "Session completed!" with title "opencode"'`
  } })
```
```js
// .env-Schutz
export const EnvProtection = async () => ({
  "tool.execute.before": async (input, output) => {
    if (input.tool === "read" && output.args.filePath.includes(".env")) throw new Error("Do not read .env files")
  } })
```
```ts
// Kontext über Compaction erhalten (sehr relevant für Multi-Agent)
import type { Plugin } from "@opencode-ai/plugin"
export const CompactionPlugin: Plugin = async () => ({
  "experimental.session.compacting": async (input, output) => {
    output.context.push(`## Custom Context\n- Current task status\n- Important decisions\n- Files in progress`)
  } })
```
Strukturiertes Logging statt `console.log`: `await client.app.log({ body: { service, level, message, extra } })`
(Level: debug/info/warn/error).

---

## 3. Custom Tools (`.opencode/tools/*.ts`) `offiziell` (opencode.ai/docs/custom-tools)

Einzelne Funktionen, die das LLM aufrufen kann. Ort: Projekt `.opencode/tools/`, global
`~/.config/opencode/tools/`. **Dateiname = Tool-Name.**
```ts
import { tool } from "@opencode-ai/plugin"
export default tool({
  description: "Query the project database",
  args: { query: tool.schema.string().describe("SQL query to execute") },
  async execute(args) { return `Executed query: ${args.query}` },
})
```
Mehrere Tools pro Datei → Name `<filename>_<exportname>` (z.B. `math_add`, `math_multiply`). `tool.schema`
ist **Zod**. Context (2. `execute`-Arg): `{ agent, sessionID, messageID, directory, worktree }`.
Skript in beliebiger Sprache aufrufbar (Python via `Bun.$`).

**Custom Tool vs. Plugin:** Tool = einzelne LLM-Funktion (kein Hook-Zugriff). Plugin = volles Modul mit
allen Hooks/Events, kann *zusätzlich* Tools über den `tool`-Hook mitliefern. Tool mit gleichem Namen wie
ein Built-in **ersetzt** es.

---

## 4. Skills (SKILL.md) — NATIV unterstützt `offiziell` (opencode.ai/docs/skills)

**Ehrliche Einordnung:** OpenCode hat **First-Party-Skills-Unterstützung** mit `SKILL.md`. Skills werden
on-demand über das native `skill`-Tool geladen. **Skills für Claude Code funktionieren in OpenCode ohne
Änderung** (`.claude/skills/` wird mitgelesen) — kein Plugin-Hack. Das frühere Community-Plugin
`opencode-agent-skills` ist seit der nativen Unterstützung laut eigener README „no longer necessary"
(nur noch Maintenance). `offiziell` / `extern`

**Such-Orte für SKILL.md:** `.opencode/skills/<name>/SKILL.md`, `~/.config/opencode/skills/...`,
`.claude/skills/...`, `~/.claude/skills/...`, `.agents/skills/...`, `~/.agents/skills/...`. Für
projekt-lokale Pfade läuft OpenCode vom CWD aufwärts bis zum Git-Worktree.

**Frontmatter:** `name` (Pflicht), `description` (Pflicht), `license`, `compatibility`, `metadata` (optional).
Name: 1–64 Zeichen, lowercase alphanumerisch mit einzelnen Bindestrichen, = Ordnername (`^[a-z0-9]+(-[a-z0-9]+)*$`).
```markdown
---
name: git-release
description: Create consistent releases and changelogs
license: MIT
compatibility: opencode
metadata:
  audience: maintainers
---
## What I do
- Draft release notes from merged PRs
- Propose a version bump
- Provide a copy-pasteable `gh release create` command
## When to use me
Use this when you are preparing a tagged release.
```
Der Agent lädt per `skill({ name: "git-release" })`. **Skill-Permissions:**
```jsonc
{ "permission": { "skill": { "*": "allow", "pr-review": "allow", "internal-*": "deny", "experimental-*": "ask" } } }
```
Skill-Tool ganz abschalten: `tools: { skill: false }` (Agent-Frontmatter oder `opencode.json`).

---

## 5. Custom Slash-Commands (`.opencode/commands/*.md`) `offiziell` (opencode.ai/docs/commands)

Wiederverwendbare Prompts, per `/name` in der TUI. Orte: global `~/.config/opencode/commands/`, projekt
`.opencode/commands/`. **Dateiname = Command-Name** (`test.md` → `/test`).
```markdown
---
description: Run tests with coverage
agent: build
model: anthropic/claude-3-5-sonnet-20241022
---
Run the full test suite with coverage report and show any failures.
Focus on the failing tests and suggest fixes.
```
JSON-Variante im `command`-Block: `{ "command": { "test": { "template": "...", "description": "...",
"agent": "build", "model": "..." } } }`.

Optionen: `template` (Pflicht), `description`, `agent`, `subtask` (true erzwingt Subagent → eigener Kontext),
`model`.

**Platzhalter:** `$ARGUMENTS` (alle Argumente), `$1`/`$2`/… (Positionsparameter), `` !`command` ``
(Shell-Output injizieren), `@datei` (Dateiinhalt einbinden).
```markdown
---
description: Review recent changes
---
Recent git commits:
!`git log --oneline -10`

Review these changes and suggest improvements.
```

---

## 6. Best Practices

### Welche MCP-Server lohnen
- **Context7** (`https://mcp.context7.com/mcp`) — aktuelle Library-Doku (gegen veraltetes Modellwissen). `offiziell`
- **Grep by Vercel** (`https://mcp.grep.app`) — echte GitHub-Code-Beispiele, leichtgewichtig. `offiziell`
- **Sentry** (`https://mcp.sentry.dev/mcp`) — Issue-/Error-Daten. `offiziell`
- **Playwright** / **Filesystem** — Browser-Automation / Zugriff außerhalb des Workspace. `extern`
- **Vorsicht GitHub-MCP** — Token-Fresser, nur bei Bedarf + per-Agent.

### Wo gute Plugins/MCP-Server finden (Discovery + Seriosität) `offiziell`/`extern`
OpenCode hat **keine eigene Plugin-Registry** — Plugins sind npm-Pakete. Discovery-Reihenfolge:
1. **`opencode.ai/docs/ecosystem`** `offiziell` — kuratiertes Verzeichnis (~35 Plugins + SDKs/Tools/Agents), vom Team gepflegt. **Erste Anlaufstelle, stärkstes Vertrauenssignal.**
2. **`github.com/awesome-opencode/awesome-opencode`** + **`awesome-opencode.com`** `extern` — aktiv gepflegte Community-Liste (Plugins, Themes, Agents, MCP-Server; durchsuchbares Web-Frontend).
3. **MCP-Server:** offizielle Registry **`registry.modelcontextprotocol.io`** (Anthropic/GitHub/Microsoft; Reverse-DNS-Namespace bindet Server an verifizierte Accounts → Impersonation-Schutz) + `github.com/modelcontextprotocol/servers`.

**Seriosität vor Install prüfen:** (a) in offizieller Ecosystem-Liste gelistet = stärkstes Signal; (b) letzte Commits < 3 Monate (OpenCode-API ändert sich schnell, alte Plugins brechen); (c) bekannter Maintainer/Org > anonymer Einzel-Account; (d) Quellcode lesen — Plugins haben vollen FS-/Shell-Zugriff und werden als npm-Paket auto-installiert → Typosquatting/Supply-Chain, Paketname EXAKT prüfen. **Quellen-Falle:** nur `opencode.ai` ist offiziell, NICHT `open-code.ai` (gespiegelter Host).

**Bewährte Plugins (Frank-Setup-relevant):** `opencode-openai-codex-auth` (ChatGPT-Abo statt API, vgl. VoiceAgent-Codex-Provider), `opencode-worktree` (git-Worktrees, parallel-sessions), `opencode-vibeguard` (Secrets/PII-Redaction = Poka-Yoke, Direktive #3), `ocx` (Extension-Manager mit isolierten Profilen), `opencode-notify`/`opencode-notificator` (OS-Notification bei Task-Ende).

### Token-sparsam halten
- **MCP sparsam aktivieren:** jedes MCP-Tool kostet Tokens in JEDER Anfrage → global aus
  (`"tools": { "server*": false }`), per Agent an. `enabled:false` ohne Entfernen.
- **Custom Tools knapp halten:** präzise, kurze `description`, nur nötige `args`.
- **Compaction nutzen:** `experimental.session.compacting`-Hook + `compaction.prune: true`.
- **Skills statt Dauer-Prompt:** on-demand geladen statt permanent im System-Prompt.

### Sicherheit (KRITISCH — externer Code)
- **npm-Plugins werden automatisch via Bun installiert + ausgeführt** → vor Eintrag ins `plugin`-Feld den
  Quellcode auf GitHub prüfen (Stars/Forks/Maintainer/Commits, keine Datenexfiltration, keine verdächtigen
  URLs/Base64-Payloads). Nur vertrauenswürdige Quellen.
- **`tool.execute.before` als Schutzschicht** (gefährliche Befehle blocken via `throw new Error(...)`).
- **Bekanntes Caveat (`extern`, Issue #5894):** `tool.execute.before`-Hooks fangen Tool-Calls von Subagents
  evtl. nicht zuverlässig ab → Guard-Plugins nicht als alleinige Sicherheitsmaßnahme.
- **Permissions als Defense-in-Depth** (`"edit": "ask"`, `"bash": {"rm -rf *": "deny"}`, `internal-*: deny`).
  Auf macOS/Windows per Managed Settings/MDM erzwingbar.
- **Secrets:** `{env:VAR}` / `{file:~/.secrets/key}` statt Klartext in eingecheckter Config.

---

## 7. Pfad-Übersicht je OS

| Was | macOS / Linux | Windows (nativ) | Windows (WSL) |
|-----|---------------|-----------------|---------------|
| Globale Config | `~/.config/opencode/opencode.json` | `%USERPROFILE%\.config\opencode\opencode.json` | im WSL-HOME `~/.config/opencode/` |
| Plugins (global/Projekt) | `~/.config/opencode/plugins/` · `.opencode/plugins/` | `…\plugins\` · `.opencode\plugins\` | gleich |
| Tools / Commands | `…/tools/` · `…/commands/` | `…\tools\` · `…\commands\` | gleich |
| Skills | `.opencode/skills/<n>/SKILL.md` (+ `.claude/skills/`, `.agents/skills/`) | dito mit `\` | gleich |
| MCP-Auth-Token | `~/.local/share/opencode/mcp-auth.json` | analog | im WSL-HOME |
| Managed Config | `/Library/Application Support/opencode/` + MDM | `%ProgramData%\opencode` | `/etc/opencode/` |

**Windows-Empfehlung (offiziell):** OpenCode kann nativ laufen, aber **WSL empfohlen** (FS-Performance,
Terminal-Support, Tool-Kompatibilität). Für reibungslose MCP-/Plugin-Nutzung Repo ins WSL-Dateisystem legen.

## 8. Plugin-Trigger-Modell & konkrete Plugin-Auswahl (recherchiert 2026-06-19)

### Wie Plugins ausgelöst werden — es gibt KEINE Triggerwörter
Häufiges Missverständnis: OpenCode-Plugins springen NICHT auf Triggerwörter an und laufen NICHT bei jeder Anfrage störend mit.

| Typ | Auslösung | Bei jeder Anfrage aktiv? |
|-----|-----------|--------------------------|
| Plugin / Hook | event-getriggert (`tool.execute.before/after`, `session.idle`, `session.created`, `file.edited`, `permission.asked` …) | Nein — nur wenn sein Event eintritt |
| Command | Slash-Befehl, manuell | Nein — nur auf Abruf |
| Skill | on-demand; Modell wählt nach `name`+`description` (kein starres Triggerwort) | Nein — nur bei Bedarf |
| Agent / Mode | eigene Modus-Schicht (z.B. `plan`) | nur im gewählten Modus |

Konsequenz: Ein installiertes Plugin ist „geladen", führt aber nur Code aus, wenn sein konkretes Event passiert. „Erst-Plan/erst-Frage"-Plugins (micode, plannotator) erzwingen daher KEINEN Plan bei trivialen Anfragen. Skill-Trigger-Zuverlässigkeit hängt an einer klaren `description` (nativ wie per Plugin).

### Konkrete Plugin-Auswahl (recherchiert, mit Eignungs-Urteil)
| Plugin | Urteil | Begründung / Konfig |
|--------|--------|---------------------|
| opencode-notifier (mohak34) | EMPFOHLEN für Ton | Einziges mit echten WAV-Tönen auf **Windows+Mac** UND getrennten Events (`permission`/`complete`/`error`). `"plugin":["@mohak34/opencode-notifier@latest"]`, WAV pro Event in `opencode-notifier.json`. notify(kdcokenny)=Windows nur System-Toast; notificator(panta82)=Per-Projekt statt Per-Event. |
| opencode-openai-codex-auth | EMPFOHLEN | Abo statt API-Kosten. |
| opencode-dynamic-context-pruning | EMPFOHLEN | Token-Pruning, passt zu lossless-context. |
| supermemory (self-hosted) | EMPFOHLEN (Eigenbau) | siehe Self-hosted-Memory unten. |
| firecrawl | sinnvoll (Recherche) | **KEIN npm-Plugin "opencode-firecrawl" (existiert nicht auf npm!) — Integration läuft als offizieller MCP-Server `firecrawl-mcp`** (Firecrawl-Team, MIT, aktiv gepflegt) im `mcp`-Block der opencode.json, NICHT im `plugin`-Array. Werkzeug (Agent ruft bei Bedarf), `search` liefert Markdown statt Links. Firecrawl-API-Key (Free 1000 Seiten/Mon), Cloud (USA) oder self-host. Es gibt zwar ein Community-Plugin `@lyculs/opencode-firecrawl` (v1.0.0, Einzel-Dev, ungepflegt) — für Produktiv lieber der offizielle MCP. |
| native Skills statt opencode-skillful | nativ nutzen | skillful überholt + wartungsarm; native Skills laden on-demand identisch. |
| micode | optional | command-getrieben, kein Zwang; native Skills/Brainstorm decken das meist ab; keine Lizenz im Repo. |
| plannotator | optional | Default greift nur im Plan-Modus; `workflow:"manual"` = nur auf Abruf. |
| sentry-monitor | nur mit Self-Host | Default sendet Prompts/Code (siehe Almanach §55c). |
| ocx | erst ab vielen Plugins/Profilen | sonst direkter opencode.json-Eintrag; braucht Bun auf Windows. |
| opencode-worktree | NICHT für „alles auf main" | erzwingt eigene Branches pro Worktree — unvereinbar mit Direkt-auf-main-Workflow. |
| OpenWork | später beobachten | Alpha, Windows schwach; ggf. gegen Cowork-Mount-Probleme in 2-3 Mon. neu bewerten. |

### Self-hosted Memory für OpenCode UND Claude Code (Eigenbau)
> **Vollständiger, recherchierter Bauplan (2026-06-19): [`self-hosted-memory-server.md`](self-hosted-memory-server.md)** — Optionen-Vergleich, Server-Setup, Sicherheit, Config für beide CLIs. Korrektur ggü. unten: supermemory self-host ist ein **Single-Binary** (kein Postgres/Docker im Default).
Ziel „ein Server, beide CLIs": Claude Code spricht MCP nativ, OpenCode via Plugin oder MCP → **ein Memory-Server mit MCP-Endpunkt bedient beide**.
- **Schnellster Weg — Supermemory self-hosted** (MIT): `npx supermemory local` → API auf `localhost:6767`. **Single-Binary** (eingebettete Graph-Engine + lokale WASM-Embeddings, KEIN Postgres/Docker im Default — Postgres+pgvector ist nur der Enterprise-Skalierungspfad). LLM-Step (Summary/Extraktion) default Cloud (`gpt-5.1`) → für Datenhoheit auf lokales Ollama umbiegen (`OPENAI_BASE_URL`). Anbindung: Claude Code via MCP, OpenCode via remote MCP (Plugin verlangt Pro). Details: `self-hosted-memory-server.md`.
- **Alternativen:** mem0 (Apache-2.0, Docker, am meisten Claude-Code-erprobt, Knowledge-Graph); Hindsight (MIT, schlank, MCP-first).
- **Komplett-Eigenbau-Stack:** Postgres+pgvector (Speicher) + Ollama (lokale Embeddings, keine Kosten) + dünner MCP-Server (Tools: add/search/list/forget). Scoping: user-Hash (git-Email) + projekt-Hash (Verzeichnis); Privates `<private>`→`[REDACTED]` vor Speicherung.
- Trigger im supermemory-Plugin: liest auto bei Session-Start, schreibt bei Keywords ("remember"/"save this") + bei ~80% Kontext (Compaction).
- Quellen: github.com/supermemoryai/supermemory · github.com/elvismdev/mem0-mcp-selfhosted · supermemory.ai/docs/integrations/opencode

## Quellen
**Offiziell:** opencode.ai/docs/mcp-servers, /plugins, /custom-tools, /skills, /commands, /config, /ecosystem,
/windows-wsl; GitHub anomalyco/opencode `packages/plugin/src/index.ts` (Hook-Typdefinition).
**Extern:** johnlindquist Plugins-Guide (Kontext-Objekt-Stolperstein); opencode-agent-skills (Maintenance);
Playwright-MCP; vicmuchina/opencode-windows-setup; GitHub-Issue #5894 (Subagent-Bypass);
awesome-opencode (github.com/awesome-opencode/awesome-opencode, awesome-opencode.com);
MCP-Registry (registry.modelcontextprotocol.io, github.com/modelcontextprotocol/servers).
