# MCP-Server — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Quellen: Offizielle Claude Code Dokumentation (code.claude.com/docs/en/mcp, /managed-mcp)
> Recherche-Datum: 2026-05-25

---

## Transport-Typen: HTTP ist Standard, SSE veraltet

- **Was:** Claude Code unterstützt drei Transporte: `streamable-http` (Alias: `http`), `sse`, `stdio`.
- **Best Practice:** Immer HTTP (streamable-http) verwenden. SSE ist offiziell deprecated — bei neuen Servern nicht mehr einsetzen, bestehende SSE-Server migrieren wenn möglich. Stdio für lokale Prozesse die direkten System-Zugriff brauchen.
- **Wichtig:** In `.mcp.json` und `add-json`-Befehlen akzeptiert das `type`-Feld `streamable-http` als Alias für `http` — kopierte Doku-Beispiele funktionieren ohne Anpassung.
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Scopes: local / project / user (Terminologie geändert!)

- **Was:** Drei Scope-Ebenen bestimmen wo ein MCP-Server gespeichert ist und ob er mit dem Team geteilt wird.
- **Scope-Umbenennung (WICHTIG):** Ältere Doku nannte die Scopes anders — die **neue offizielle Terminologie** ist:
  - `local` (Standard, früher: `project`) → Nur im aktuellen Projekt, nur für dich → gespeichert in `~/.claude.json`
  - `project` (früher: `global`?) → Im Projekt für alle via `.mcp.json` → in Versionskontrolle einzuchecken
  - `user` (früher: `global`) → Auf allen deinen Projekten, privat → gespeichert in `~/.claude.json`
- **Best Practice:**
  - `local` für experimentelle Server, persönliche Dev-Configs, Server mit Credentials
  - `project` für Team-Server die alle brauchen (`.mcp.json` einchecken, Secrets via ENV-Variablen)
  - `user` für persönliche Utility-Tools die projektübergreifend nützlich sind
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## .mcp.json: Format & ENV-Variable-Expansion

- **Was:** Project-scoped Server werden in `.mcp.json` im Projekt-Root gespeichert. Claude Code fragt zur Genehmigung vor dem ersten Laden (Security-Dialog). `claude mcp reset-project-choices` setzt Genehmigungen zurück.
- **Unterstützte ENV-Syntax:**
  - `${VAR}` → Expandiert zur ENV-Variable `VAR`
  - `${VAR:-default}` → Expandiert zu `VAR` wenn gesetzt, sonst `default`
- **Expansion funktioniert in:** `command`, `args`, `env`, `url`, `headers`
- **Best Practice für Teams:** Niemals Secrets in `.mcp.json` hartkodieren. Stattdessen:
  ```json
  {
    "mcpServers": {
      "api-server": {
        "type": "http",
        "url": "${API_BASE_URL:-https://api.example.com}/mcp",
        "headers": {
          "Authorization": "Bearer ${API_KEY}"
        }
      }
    }
  }
  ```
  Jeder Entwickler setzt `API_KEY` in seiner eigenen Umgebung. `.mcp.json` ist sicher commitbar.
- **Achtung:** Wenn eine erforderliche ENV-Variable nicht gesetzt ist und kein Default existiert, schlägt das Parsen der Config fehl.
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## OAuth 2.0 Authentication für Remote-Server

- **Was:** Claude Code unterstützt OAuth 2.0 für sichere Remote-Verbindungen. Authentifizierung wird via `/mcp`-Befehl ausgelöst, Tokens werden sicher gespeichert und automatisch erneuert.
- **Trigger:** Server antwortet mit `401 Unauthorized` oder `403 Forbidden` → Server wird in `/mcp` als "benötigt Authentifizierung" markiert.
- **Best Practice für verschiedene Auth-Szenarien:**
  - **Standard OAuth (dynamische Client-Registrierung):** `claude mcp add --transport http server https://...` → dann `/mcp` zum Einloggen
  - **Fixer Callback-Port** (wenn Server eine vorregistrierte Redirect-URI braucht): `--callback-port 8080`
  - **Vorkonfigurierte OAuth-Credentials:** `--client-id your-id --client-secret --callback-port 8080`
  - **Benutzerdefiniertes Auth-Schema (Kerberos, Short-lived Tokens etc.):** `headersHelper` verwenden — Befehl wird bei jeder Verbindung ausgeführt und seine JSON-Ausgabe als Headers gesetzt
  - **OAuth-Scopes einschränken:** `"oauth": { "scopes": "channels:read chat:write" }` in `.mcp.json` — sicherheitsrelevant wenn Server mehr Scopes anbietet als nötig
- **Wichtig:** `authServerMetadataUrl` ab v2.1.64 verfügbar zum Überschreiben der OAuth-Discovery-Chain
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Tool Search: Kontext-Optimierung bei vielen MCP-Servern (Standard seit 2.1.x)

- **Was:** Tool Search ist standardmäßig aktiviert. Nur Tool-Namen laden beim Session-Start — Tool-Definitionen werden on-demand geladen wenn Claude sie braucht. Hält den Kontext schlank auch bei vielen Servern.
- **Voraussetzung:** Modell muss `tool_reference` Blocks unterstützen (Sonnet 4+, Opus 4+ — Haiku nicht). Auf Vertex AI ab Sonnet 4.5/Opus 4.5. Deaktiviert bei Vertex AI by default und bei Custom `ANTHROPIC_BASE_URL`.
- **Best Practice:**
  - Standard-Setting (`ENABLE_TOOL_SEARCH` ungesetzt) für die meisten Setups
  - `alwaysLoad: true` in der Server-Config nur für die wenigen Tools die Claude auf jedem Turn braucht (blockiert Session-Start bis Server verbunden, max 5 Sekunden)
  - Für MCP-Server-Autoren: Klare `server instructions` schreiben — werden von Tool Search genutzt um relevante Tools zu finden (max 2KB, kritische Info vorne)
  - `ENABLE_TOOL_SEARCH=auto` für Threshold-Modus: Tools laden upfront wenn sie in 10% des Context-Windows passen
  - `ENABLE_TOOL_SEARCH=false` nur wenn Tool Search Probleme macht (lädt alle Tools upfront)
- **Einzelnes Tool immer laden:** `"_meta": { "anthropic/alwaysLoad": true }` im tools/list-Response
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## MCP Resources: @ Mentions

- **Was:** MCP-Server können Resources exponieren, die per `@`-Mention referenziert werden, ähnlich wie Dateien. Format: `@server:protocol://resource/path`
- **Best Practice:**
  - `@` tippen für Autocomplete-Menü mit allen verfügbaren Resources
  - Mehrere Resources in einem Prompt kombinieren: `Compare @postgres:schema://users with @docs:file://database/user-model`
  - Resources werden automatisch als Attachments eingebunden wenn referenziert
  - Fuzzy-Search in der Autocomplete
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## MCP Prompts als Commands

- **Was:** MCP-Server können Prompts exponieren die als `/mcp__servername__promptname`-Commands verfügbar werden
- **Best Practice:**
  - `/` tippen für alle verfügbaren Commands inkl. MCP-Prompts
  - Mit Argumenten: `/mcp__github__pr_review 456`
  - Server- und Prompt-Namen werden normalisiert (Leerzeichen → Unterstriche)
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Channels: Externe Events in die Session pushen

- **Was:** MCP-Server können mit `claude/channel`-Capability Nachrichten aktiv in die laufende Session pushen — Claude reagiert auf externe Events (CI-Ergebnisse, Monitoring-Alerts, Chat-Nachrichten) ohne dass der Nutzer aktiv sein muss.
- **Best Practice:**
  - Opt-in beim Start: `--channels`-Flag
  - Server muss `claude/channel`-Capability deklarieren
  - Offiziell unterstützte Channels: [https://code.claude.com/docs/en/channels](https://code.claude.com/docs/en/channels)
  - Für eigene Channels: [https://code.claude.com/docs/en/channels-reference](https://code.claude.com/docs/en/channels-reference)
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Managed MCP: Enterprise-Kontrolle

- **Was:** Administratoren können via `managed-mcp.json` zentral steuern welche MCP-Server Nutzer verwenden dürfen.
- **Pfade:**
  - macOS: `/Library/Application Support/ClaudeCode/managed-mcp.json`
  - Linux/WSL: `/etc/claude-code/managed-mcp.json`
  - Windows: `C:\Program Files\ClaudeCode\managed-mcp.json`
- **Kontrollmuster (von streng zu locker):**
  1. **MCP komplett deaktivieren:** `{ "mcpServers": {} }` — leeres Objekt blockiert alles
  2. **Festes Server-Set:** `managed-mcp.json` mit erlaubten Servern — Nutzer können nichts hinzufügen
  3. **Genehmigter Katalog:** `allowedMcpServers` + `allowManagedMcpServersOnly: true` — Nutzer wählen aus der Liste
  4. **Nur Plugin-Server:** `strictPluginOnlyCustomization` mit `mcp` in der Liste
  5. **Weiche Allowlist:** `allowedMcpServers` ohne `allowManagedMcpServersOnly` — Nutzer können erweitern
  6. **Nur Denylist:** `deniedMcpServers` — blockiert bekannte schlechte Server, Rest erlaubt
- **Best Practice für Allowlist/Denylist:**
  - Immer `serverUrl` für Remote-Server (mit Wildcards: `https://mcp.example.com/*`)
  - Immer `serverCommand` für Stdio-Server (exakter Match aller Argumente!)
  - `serverName` ist KEIN Sicherheitskontroll — Nutzer können jeden Server beliebig benennen
  - Secrets niemals in `managed-mcp.json` (Datei ist maschinenlesbar) → `${VAR}` Expansion oder OAuth/headersHelper
- **Validierung:** `claude mcp add --transport http test https://example.com/mcp` sollte mit Policy-Fehler fehlschlagen wenn `managed-mcp.json` aktiv
- **Monitoring:** `OTEL_LOG_TOOL_DETAILS=1` für OpenTelemetry-Export mit MCP Server/Tool-Namen
- **Quelle:** [https://code.claude.com/docs/en/managed-mcp](https://code.claude.com/docs/en/managed-mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Wichtige Umgebungsvariablen & Timeouts

- **Was:** Mehrere ENV-Variablen steuern MCP-Verhalten auf Session-Ebene.
- **Referenz:**

| Variable | Default | Bedeutung |
|---|---|---|
| `MCP_TIMEOUT` | 5000ms | Server-Startup-Timeout in ms (`MCP_TIMEOUT=10000 claude`) |
| `MAX_MCP_OUTPUT_TOKENS` | 25.000 | Max Token-Output pro Tool-Call |
| `ENABLE_TOOL_SEARCH` | unset (=auto on) | Tool-Search-Verhalten (true/false/auto/auto:N) |
| `ENABLE_CLAUDEAI_MCP_SERVERS` | true | Claude.ai Connectors in Claude Code laden |
| `MCP_CLIENT_SECRET` | - | OAuth Client Secret für CI/non-interaktive Umgebungen |

- **Per-Server Timeout:** `"timeout": 600000` (ms) in der Server-Konfiguration → überschreibt `MCP_TOOL_TIMEOUT` für diesen Server
- **Warnung bei großem Output:** Ab 10.000 Token zeigt Claude Code eine Warnung. Limit erhöhen mit `MAX_MCP_OUTPUT_TOKENS=50000`
- **Server-Autoren:** `"_meta": { "anthropic/maxResultSizeChars": 200000 }` im tools/list-Response für Tools die große Outputs brauchen (max 500.000 Chars)
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Scope-Hierarchie & Deduplizierung

- **Was:** Wenn derselbe Server in mehreren Scopes definiert ist, verbindet Claude Code ihn einmalig mit der höchsten Precedence.
- **Reihenfolge (höchste zuerst):**
  1. Local Scope
  2. Project Scope
  3. User Scope
  4. Plugin-bereitgestellte Server
  5. Claude.ai Connectors
- **Matching:** Scopes 1-3 matchen nach Name. Plugins und Connectors matchen nach Endpoint/URL.
- **Best Practice:** Server-Name `workspace` ist reserviert (interner Gebrauch) → bei eigenen Servern vermeiden
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Reconnection & Reliability

- **Was:** HTTP/SSE-Server werden bei Verbindungsabbruch automatisch reconnected (exponential backoff: bis 5 Versuche, Start bei 1 Sekunde, verdoppelt sich). Ab v2.1.121: auch Initial-Connection wird bis zu 3x wiederholt bei transienten Fehlern (5xx, Connection Refused, Timeout). Stdio-Server werden nicht automatisch reconnected.
- **Best Practice:**
  - Für produktive Remote-Server HTTP statt SSE verwenden (beide haben Reconnect-Logik, HTTP ist aktueller Standard)
  - Bei Authentifizierungs- oder 404-Fehlern kein Retry — Config-Problem, nicht transienter Fehler
  - `/mcp` zeigt Server-Status und pending/failed Verbindungen
- **Dynamische Tool-Updates:** MCP `list_changed` Notifications werden unterstützt — Server können Tools live aktualisieren ohne Reconnect
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Plugin-gebundene MCP-Server

- **Was:** Plugins können MCP-Server mitbringen — automatisch aktiv wenn Plugin aktiviert, keine manuelle Konfiguration nötig.
- **ENV-Variablen in Plugin-MCP-Configs:**
  - `${CLAUDE_PLUGIN_ROOT}` → Plugin-Verzeichnis für mitgelieferte Binaries
  - `${CLAUDE_PLUGIN_DATA}` → Persistente Daten die Plugin-Updates überleben
  - `${CLAUDE_PROJECT_DIR}` → Aktuelles Projekt-Root
- **Best Practice:** `/reload-plugins` ausführen wenn Plugin während Session aktiviert/deaktiviert
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## MCP Elicitation: Strukturierte Eingabe mid-Task

- **Was:** Server können via Elicitation strukturierte Eingaben vom Nutzer anfordern während Claude arbeitet. Automatisch als interaktiver Dialog oder Browser-URL angezeigt — keine Konfiguration nötig.
- **Modi:**
  - Form-Modus: Claude Code zeigt Formularfelder (z.B. Username/Password)
  - URL-Modus: Browser-URL für OAuth/Approval-Flows
- **Best Practice:** `Elicitation`-Hook für automatische Responses in nicht-interaktiven Umgebungen
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-25

---

## Praktische Kurzreferenz: häufige Befehle

```bash
# HTTP-Server hinzufügen (empfohlen)
claude mcp add --transport http notion https://mcp.notion.com/mcp

# Mit Bearer-Token
claude mcp add --transport http github https://api.githubcopilot.com/mcp/ \
  --header "Authorization: Bearer YOUR_PAT"

# Stdio-Server mit ENV-Variable
claude mcp add --transport stdio --env API_KEY=xxx my-server -- npx -y server-package

# Project-Scope (für Team)
claude mcp add --transport http paypal --scope project https://mcp.paypal.com/mcp

# User-Scope (alle Projekte)
claude mcp add --scope user --transport http hubspot https://mcp.hubspot.com/anthropic

# Von JSON
claude mcp add-json weather-api '{"type":"http","url":"https://api.weather.com/mcp"}'

# Von Claude Desktop importieren (macOS/WSL)
claude mcp add-from-claude-desktop

# Status prüfen
claude mcp list
claude mcp get github
/mcp  # In Claude Code Session
```
