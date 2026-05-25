# MCP-Server — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

## Überblick

- **Was:** MCP (Model Context Protocol) ist ein offener Standard für AI-Tool-Integrationen. Claude Code verbindet sich über MCP-Server mit externen Tools, Datenbanken und APIs.
- **Best Practice:** MCP-Server einsetzen wenn man Daten aus anderen Tools ins Chat kopiert (Issue-Tracker, Monitoring-Dashboard etc.) — stattdessen Claude direkt lesen und handeln lassen.
- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Transport-Typen: HTTP vs SSE vs stdio

- **Was:** Drei Transport-Typen für MCP-Server-Verbindungen.
- **Best Practice:**

| Transport | Verwendung | Status |
|-----------|-----------|--------|
| `http` (alias: `streamable-http`) | Remote/Cloud-Services — empfohlen | ✅ Standard |
| `sse` | Legacy Remote-Server | ⚠️ Deprecated |
| `stdio` | Lokale Prozesse, System-Zugriff | ✅ Aktiv |

  - SSE ist deprecated — wo verfügbar auf HTTP wechseln
  - `streamable-http` als alias für `http` in JSON-Configs: erlaubt direktes Kopieren von Server-Dokumentation
  - Stdio-Server: `CLAUDE_PROJECT_DIR` ist im Server-Environment gesetzt → Projekt-relative Pfade ohne Working-Directory-Abhängigkeit

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Scopes: local / project / user

- **Was:** Drei Scopes kontrollieren wo Konfiguration gespeichert wird und ob sie geteilt wird.
- **Best Practice:**

| Scope | Geladen in | Geteilt | Gespeichert in |
|-------|-----------|---------|----------------|
| `local` (Standard) | Nur aktuelles Projekt | Nein | `~/.claude.json` |
| `project` | Nur aktuelles Projekt | Ja, via `.mcp.json` in VCS | `.mcp.json` im Projekt-Root |
| `user` | Alle Projekte | Nein | `~/.claude.json` |

  - `local` für persönliche Dev-Server und experimentelle Configs mit Credentials
  - `project` für Team-Infrastruktur (`.mcp.json` in VCS einchecken)
  - `user` für persönliche Tools die projektübergreifend benötigt werden
  - Scope-Priorität (bei Duplikaten): local > project > user > Plugin-Server > claude.ai-Connector

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## .mcp.json-Struktur (Project Scope)

- **Was:** `.mcp.json` im Projekt-Root für Team-geteilte Konfiguration.
- **Best Practice:** Vollständiges Format mit allen unterstützten Optionen:

```json
{
  "mcpServers": {
    "http-server": {
      "type": "http",
      "url": "${API_BASE_URL:-https://api.example.com}/mcp",
      "headers": {
        "Authorization": "Bearer ${API_KEY}"
      },
      "timeout": 600000,
      "alwaysLoad": false,
      "oauth": {
        "scopes": "channels:read chat:write",
        "callbackPort": 8080
      }
    },
    "stdio-server": {
      "type": "stdio",
      "command": "/path/to/server",
      "args": ["--config", "${CLAUDE_PROJECT_DIR:-./}config.json"],
      "env": {
        "DB_URL": "${DB_URL}"
      },
      "timeout": 30000
    }
  }
}
```

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Umgebungsvariablen-Expansion

- **Was:** `.mcp.json` unterstützt `${VAR}` und `${VAR:-default}` Expansion in `command`, `args`, `env`, `url` und `headers`.
- **Best Practice:**
  - `${VAR}` — erweitert auf Wert der Umgebungsvariable
  - `${VAR:-default}` — Fallback-Wert wenn nicht gesetzt
  - Für Secrets: Variable aus Umgebung lesen, nie hardcoden
  - `${CLAUDE_PROJECT_DIR:-.}` als Default wenn Variable möglicherweise fehlt (z.B. außerhalb von Plugin-Kontext)

```json
{
  "mcpServers": {
    "api": {
      "type": "http",
      "url": "${API_URL:-https://api.default.com}/mcp",
      "headers": { "Authorization": "Bearer ${API_TOKEN}" }
    }
  }
}
```

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## CLI-Befehle

### Server hinzufügen

```bash
# HTTP-Server (empfohlen)
claude mcp add --transport http github https://api.githubcopilot.com/mcp/ \
  --header "Authorization: Bearer $GITHUB_PAT"

# Mit Scope
claude mcp add --transport http stripe --scope project https://mcp.stripe.com

# Stdio-Server
claude mcp add --transport stdio airtable \
  --env AIRTABLE_API_KEY=KEY \
  -- npx -y airtable-mcp-server

# Aus JSON
claude mcp add-json weather '{"type":"http","url":"https://api.weather.com/mcp"}'

# Aus Claude Desktop importieren (macOS/WSL)
claude mcp add-from-claude-desktop --scope user
```

### Server verwalten

```bash
claude mcp list           # Alle konfigurierten Server
claude mcp get github     # Details für einen Server
claude mcp remove github  # Server entfernen
claude mcp reset-project-choices  # Genehmigungs-Entscheidungen zurücksetzen
/mcp                      # In-Session: Status und Tool-Anzahl anzeigen
```

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## OAuth 2.0 Authentifizierung

- **Was:** Claude Code unterstützt OAuth 2.0 für Remote-MCP-Server. Server wird als auth-pflichtig markiert bei 401/403-Antworten.
- **Best Practice:**

```bash
# 1. Server hinzufügen
claude mcp add --transport http sentry https://mcp.sentry.dev/mcp

# 2. In Claude Code authentifizieren
/mcp  # → Browser-Flow folgen

# Fester Callback-Port (für pre-registrierte Redirect-URIs)
claude mcp add --transport http --callback-port 8080 my-server https://mcp.example.com/mcp

# Pre-konfigurierte OAuth-Credentials
claude mcp add --transport http \
  --client-id my-client-id --client-secret --callback-port 8080 \
  my-server https://mcp.example.com/mcp
```

  - OAuth-Tokens werden sicher im System-Keychain gespeichert und automatisch refreshed
  - `authServerMetadataUrl` überschreibt Standard-Discovery (ab v2.1.64)
  - `oauth.scopes` pinnt Scopes auf Security-genehmigte Teilmenge

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Dynamische Headers (headersHelper)

- **Was:** Für Custom-Authentifizierung (Kerberos, Short-lived Tokens, internes SSO) ausführt `headersHelper` ein Kommando das Header generiert.
- **Best Practice:**

```json
{
  "mcpServers": {
    "internal-api": {
      "type": "http",
      "url": "https://mcp.internal.example.com",
      "headersHelper": "/opt/bin/get-mcp-auth-headers.sh"
    }
  }
}
```

  - Helper muss JSON-Objekt mit String-Key-Value-Paaren auf stdout schreiben
  - Läuft bei jeder Verbindung fresh (Session-Start + Reconnect) — kein Caching
  - Timeout: 10 Sekunden
  - `CLAUDE_CODE_MCP_SERVER_NAME` und `CLAUDE_CODE_MCP_SERVER_URL` sind im Helper-Environment verfügbar
  - `headersHelper` in project/local scope: Nur nach Workspace-Trust-Dialog

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Tool Search (Standard ab ~v2.1.x)

- **Was:** Tool Search lädt MCP-Tool-Definitionen erst on-demand statt alle beim Session-Start. Nur Tool-Namen laden initial → minimaler Kontext-Verbrauch.
- **Best Practice:**

```bash
# Standard: alle Tools deferred
# (kein Flag nötig)

# Threshold-Modus: lädt sofort wenn unter 10% Kontext
ENABLE_TOOL_SEARCH=auto claude

# Custom Threshold
ENABLE_TOOL_SEARCH=auto:5 claude

# Deaktivieren (alle Tools sofort laden)
ENABLE_TOOL_SEARCH=false claude
```

  - Standard ist `true` (alle deferred). Fällt auf Vertex AI oder bei custom `ANTHROPIC_BASE_URL` auf Upfront-Loading zurück.
  - Erfordert Sonnet 4+ oder Opus 4+. Haiku unterstützt es nicht.
  - `alwaysLoad: true` in Server-Config: Server-Tools laden immer sofort (für kritische, immer benötigte Tools)
  - MCP-Server-Autoren: `server instructions`-Feld nutzen damit Claude weiß wann nach Tools zu suchen ist

```json
{
  "mcpServers": {
    "core-tools": {
      "type": "http",
      "url": "https://mcp.example.com/mcp",
      "alwaysLoad": true
    }
  }
}
```

  - `alwaysLoad` blockiert Startup bis Server verbunden (max 5 Sekunden Timeout) — sparsam einsetzen!

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Output-Limits konfigurieren

- **Was:** MCP-Tool-Output wird bei 10.000 Tokens gewarnt, Default-Maximum 25.000 Tokens.
- **Best Practice:**

```bash
# Global erhöhen
export MAX_MCP_OUTPUT_TOKENS=50000
claude
```

  Für eigene MCP-Server: `anthropic/maxResultSizeChars` in `tools/list`-Response setzen (max 500.000 Zeichen) — dann müssen Nutzer `MAX_MCP_OUTPUT_TOKENS` nicht anpassen:

```json
{
  "name": "get_schema",
  "description": "Returns the full database schema",
  "_meta": { "anthropic/maxResultSizeChars": 200000 }
}
```

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## MCP-Timeouts konfigurieren

- **Was:** Startup-Timeout global, Tool-Execution-Timeout pro Server.
- **Best Practice:**

```bash
# Startup-Timeout (global)
MCP_TIMEOUT=10000 claude     # 10 Sekunden

# Tool-Execution-Timeout (pro Server in .mcp.json)
# "timeout": 600000 = 10 Minuten
# Werte unter 1000ms werden auf 1 Sekunde aufgerundet
# Bei HTTP/SSE: mindestens 60s First-Byte-Budget unabhängig davon
```

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Automatische Reconnection

- **Was:** HTTP/SSE-Server: automatische Reconnection mit exponential Backoff (5 Versuche, start 1s, verdoppelt). Stdio-Server: kein Auto-Reconnect.
- **Best Practice:** Ab v2.1.121: bis zu 3 Versuche bei transient errors (5xx, Connection refused, Timeout) beim initialen Connect. Auth-Fehler und 404 werden nicht retried (Config-Änderung nötig).
- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Channels: Push-Nachrichten von MCP-Servern

- **Was:** MCP-Server können aktiv Nachrichten in die Claude-Session pushen (CI-Ergebnisse, Monitoring-Alerts, Chat-Nachrichten). Server deklariert `claude/channel`-Capability, opt-in mit `--channels` Flag.
- **Best Practice:** Für reaktive Workflows (CI → Claude reagiert automatisch). Dokumentation: `/en/channels` und `/en/channels-reference`.
- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## MCP-Resources mit @-Mentions

- **Was:** MCP-Server können Resources exponieren, die per `@server:protocol://resource/path` referenziert werden.
- **Best Practice:**

```
# Beispiele
@github:issue://123
@docs:file://api/authentication
@postgres:schema://users
```

  - Tippe `@` für Autocomplete aller verfügbaren Resources
  - Resources werden automatisch als Attachments eingebunden
  - Pfade sind fuzzy-searchbar

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## MCP-Prompts als Commands

- **Was:** MCP-Server können Prompts exponieren die als `/mcp__servername__promptname`-Commands verfügbar sind.
- **Best Practice:**

```bash
# Ohne Argumente
/mcp__github__list_prs

# Mit Argumenten
/mcp__github__pr_review 456
/mcp__jira__create_issue "Bug in login" high
```

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Managed MCP Configuration (Enterprise)

- **Was:** Für Teams/Enterprise: zentralisierte Kontrolle über MCP-Server via `managed-mcp.json` + `allowedMcpServers`/`deniedMcpServers`.
- **Best Practice:** Verweis auf `/en/managed-mcp` für vollständige Doku. Ab v2.1.149 auch `allowAllClaudeAiMcps` Managed-Setting für claude.ai Cloud MCP Connectors.
- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Claude Code als MCP-Server

- **Was:** Claude Code selbst kann als stdio-MCP-Server gestartet werden.
- **Best Practice:**

```bash
claude mcp serve
```

Claude Desktop config (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "claude-code": {
      "type": "stdio",
      "command": "/full/path/to/claude",
      "args": ["mcp", "serve"]
    }
  }
}
```

  - Vollpfad zu `claude` angeben (`which claude`). Nur Pfad im PATH reicht nicht immer.
  - Exposiert Claude-Tools (View, Edit, LS etc.) für andere MCP-Clients.

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Empfohlene MCP-Server (Stand Mai 2026)

- **Was:** Offiziell kuratierte und partner-verifizierte Server aus dem Anthropic Directory.
- **Best Practice:** Im Anthropic Directory unter https://claude.ai/directory browsen. Alle dort gelisteten Remote-Server können direkt mit `claude mcp add` hinzugefügt werden.

Häufig genutzter Kern:

| Server | Transport | Befehl |
|--------|-----------|--------|
| GitHub | HTTP | `claude mcp add --transport http github https://api.githubcopilot.com/mcp/ --header "Authorization: Bearer $PAT"` |
| Sentry | HTTP | `claude mcp add --transport http sentry https://mcp.sentry.dev/mcp` |
| Notion | HTTP | `claude mcp add --transport http notion https://mcp.notion.com/mcp` |
| PostgreSQL | stdio | `claude mcp add --transport stdio db -- npx -y @bytebase/dbhub --dsn "postgresql://..."` |
| Playwright | stdio | `claude mcp add --transport stdio playwright -- npx -y @playwright/mcp@latest` |

  - Eigene MCP-Server scaffolden: `mcp-server-dev`-Plugin aus Official Marketplace nutzen (`/mcp-server-dev:build-mcp-server`)

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Diagnose & Troubleshooting

```bash
# Server-Status prüfen
claude mcp list

# Server-Details
claude mcp get <name>

# In-Session Status (Tool-Anzahl + fehlerhafte Server)
/mcp

# Server manuell testen (bei Disconnect)
# Befehl aus .mcp.json manuell im Terminal ausführen → Fehler sehen
```

- **Best Practice:** JSON in `.mcp.json` vor Änderungen mit einem Linter validieren (Trailing Commas, fehlende Quotes → stille Fehler). `"workspace"` ist als Server-Name reserviert — vermeiden.
- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Changelog-Zeilen (MCP)

```
v2.1.149 (22.05.2026) — MCP: allowAllClaudeAiMcps Enterprise-Setting; /usage zeigt Kosten pro MCP-Server
v2.1.144 (19.05.2026) — MCP: paginierte tools/list Responses werden vollständig geladen; SVG-Images zu Disk
v2.1.121 (früher)     — MCP: alwaysLoad-Feld (ab v2.1.121); 3 Retries bei transient Initial-Connect-Fehlern
v2.1.64  (früher)     — MCP: authServerMetadataUrl für OAuth-Discovery-Override
~2025    (früher)     — MCP: Tool Search Standard (deferred loading); Channels-Feature; headersHelper; oauth.scopes
~2025    (früher)     — MCP: SSE deprecated → HTTP empfohlen; dynamische list_changed Notifications
```
