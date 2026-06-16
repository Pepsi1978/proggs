# MCP-Server — Best Practices (Stand 2026-06-05, Claude Code 2.1.165)

> Quellen: Offizielle Claude Code Dokumentation (code.claude.com/docs/en/mcp, /managed-mcp)
> Recherche-Datum: 2026-05-28

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | `.mcp.json`-Pfade | absolute Pfade/Interpreter; plattformspezifisch (NIE automatisch vereinheitlichen) | .mcp.json |
| 2 | Transport | HTTP ist Standard, SSE veraltet | Transport-Typen |
| 3 | viele Server | Tool Search (deferred Schemas) gegen Kontext-Ueberlauf | Tool Search |
| 4 | Remote-Server | OAuth 2.0; in Cowork Anthropic-IPs allowlisten | OAuth 2.0 |
| 5 | Subagent-MCP | Frontmatter-MCP ist policy-konform (2.1.153) | Subagent-Frontmatter-MCP |
| 6 | Scopes | local / project / user (Terminologie geaendert); Dedup ueber Scopes | Scopes |
| 7 | MCP-Hook fuer Policy | umgehbar bei Disconnect → harte Policy als `command`-Hook | Managed MCP |

---

## NEU in 2.1.153: Stateful-Server-Reconnect-Loop behoben (Regression-Fix)

- **Was:** Stateful MCP-Server ohne den optionalen GET-SSE-Stream (d.h. Server die nur POST-basiertes Streamable-HTTP implementieren, ohne den GET-Stream für Server-Sent Events) liefen in v2.1.147–v2.1.152 in eine Reconnect-Schleife bei jedem `tools/list`-Aufruf.
- **Zeitraum:** Regression war in v2.1.147 eingeführt und wurde in v2.1.153 behoben.
- **Betroffene Setups:** Nur stateful HTTP-Server ohne GET-SSE-Stream-Implementierung. Stateless Server, Stdio-Server und Server mit vollständiger HTTP/SSE-Implementierung waren nicht betroffen.
- **Best Practice wenn du zwischen 2.1.147 und 2.1.152 war:**
  - Prüfe ob deine stateful HTTP-Server unerwartet langsam wurden oder wiederholte `tools/list`-Calls im Server-Log zeigen — das war das Symptom.
  - Update auf 2.1.153+ behebt das ohne Config-Änderung.
  - Falls du als Workaround auf SSE oder Stdio gewechselt hast, kannst du wieder zurück zu Streamable-HTTP wechseln.
- **Für MCP-Server-Autoren:** Es ist nicht zwingend nötig, den optionalen GET-SSE-Stream zu implementieren — Claude Code behandelt dessen Fehlen jetzt wieder korrekt.
- **Quelle:** [Changelog — code.claude.com](https://code.claude.com/docs/en/changelog) (offiziell), Stand: 2026-05-28

---

## NEU in 2.1.153: Subagent-Frontmatter-MCP jetzt policy-konform

- **Was:** MCP-Server, die in Subagent-Frontmatter (Agent-Tool) definiert werden, ignorierten bisher `--strict-mcp-config`, `--bare`, Remote-Mode, Enterprise Managed MCP Config und managed-settings Allow/Deny-Policies. Das war eine Sicherheitslücke — Policies wurden in subagent-spezifischen Setups umgangen.
- **Fix-Details:**
  - `--strict-mcp-config` entfernt jetzt NICHT mehr inline `mcpServers` aus explizit via `--agents` / SDK `agents` übergebenen Agent-Definitionen — das war ein ungewollter Seiteneffekt.
  - Geblockte Subagent-MCP-Server zeigen nun sichtbare Warnungen statt still zu versagen.
- **Best Practice für Enterprise:**
  - Wenn du `managed-mcp.json` mit Allow/Deny-Lists verwendest: Ab 2.1.153 greifen diese Policies auch für Subagent-definierte Server.
  - Teste nach Update ob bestehende Subagent-Definitionen mit inline `mcpServers` weiterhin wie erwartet funktionieren — die Interaktion mit `--strict-mcp-config` hat sich geändert.
  - Geblockte Server werden jetzt mit Warnung angezeigt statt still zu fehlen → bessere Debuggbarkeit.
- **Quelle:** [Changelog — code.claude.com](https://code.claude.com/docs/en/changelog) (offiziell), [DevelopersIO Artikel 2026-05-28](https://dev.classmethod.jp/en/articles/20260528-claude-code-updates-v2-1-153/) (extern), Stand: 2026-05-28

---

## NEU in 2.1.153: Auth-Benachrichtigung für MCP + Connectors konsolidiert

- **Was:** Bisher kamen beim Session-Start getrennte Benachrichtigungen für MCP-Server und Claude.ai Connectors die Authentifizierung benötigen. Diese sind jetzt in einer einzigen kombinierten Nachricht zusammengefasst.
- **Was das für dich bedeutet:** Weniger Startup-Spam wenn mehrere Server Auth brauchen. `/mcp` bleibt der primäre Weg zum Authentifizieren einzelner Server.
- **Best Practice:** Wenn Server nach Update keine Verbindung zeigen, `/mcp` prüfen — die kombinierte Nachricht zeigt alle Server die noch Auth brauchen.
- **Quelle:** [Changelog — code.claude.com](https://code.claude.com/docs/en/changelog) (offiziell), Stand: 2026-05-28

---

## NEU in 2.1.153: Tool-Progress in collapsed view jetzt sichtbar

- **Was:** MCP-Tool-Fortschrittsbenachrichtigungen wurden in der kollabierten Tool-Ansicht nicht angezeigt — behoben.
- **Relevanz für Server-Autoren:** Wenn dein Server MCP progress notifications sendet (z.B. für lange laufende Tools), sind diese jetzt auch sichtbar wenn der Nutzer die Tool-Ansicht eingeklappt hat.
- **Quelle:** [Changelog — code.claude.com](https://code.claude.com/docs/en/changelog) (offiziell), Stand: 2026-05-28

---

## NEU in 2.1.152: Plugin-MCP-Dedup-Fix (gleicher Command, andere ENV-Vars)

- **Was:** Plugin-MCP-Server mit identischem Command aber unterschiedlichen Umgebungsvariablen wurden fälschlicherweise als Duplikate behandelt und nur einmal gestartet. Das trat auf wenn ein Plugin mehrere MCP-Server mit derselben Binary aber unterschiedlichen `env`-Einträgen definierte (z.B. verschiedene API-Endpoints oder Credentials).
- **Hintergrund:** Die Dedup-Logik stammte aus v2.1.71 und verglich nur Command + URL, nicht die Umgebungsvariablen.
- **Fix:** Ab 2.1.152 werden Command, Args UND alle ENV-Variablen beim Dedup-Vergleich berücksichtigt.
- **Best Practice:**
  - Wenn du ein Plugin mit mehreren Instanzen desselben MCP-Servers (gleiche Binary, andere ENV) verwendest und Verbindungsprobleme hattest: Update auf 2.1.152+ behebt das ohne Config-Änderung.
  - Für Plugin-Autoren: ENV-Variablen sind jetzt Teil des Identitäts-Vergleichs — du kannst sicher mehrere Instanzen derselben Binary mit verschiedenen Konfigurationen definieren.
- **Quelle:** [Changelog — code.claude.com](https://code.claude.com/docs/en/changelog) (offiziell), [DevelopersIO Artikel 2026-05-24](https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/) (extern), [GitHub Issue #32549](https://github.com/anthropics/claude-code/issues/32549) (extern), Stand: 2026-05-28

---

## NEU in 2.1.152: Remote-MCP hinter Egress-Proxy behoben

- **Was:** Remote MCP-Server schlugen fehl wenn Claude Code in einer Remote-Session mit aktiviertem Egress-Proxy lief.
- **Best Practice:** Kein Workaround nötig ab 2.1.152 — Remote-MCP-Server funktionieren wieder in egress-proxy-geschützten Umgebungen (typisch: Corporate-Netzwerke, CI/CD).
- **Quelle:** [Changelog — code.claude.com](https://code.claude.com/docs/en/changelog) (offiziell), Stand: 2026-05-28

---

## NEU in 2.1.152: `allowAllClaudeAiMcps` Managed Setting

- **Was:** Neues `allowAllClaudeAiMcps`-Feld in `managed-mcp.json` das das Laden von Claude.ai Cloud MCP Connectors neben der normalen `managed-mcp.json`-Konfiguration erlaubt.
- **Best Practice für Enterprise-Admins:**
  - Standardmäßig sind Claude.ai Connectors und `managed-mcp.json` getrennt konfiguriert.
  - `allowAllClaudeAiMcps: true` in `managed-mcp.json` erlaubt Nutzern, ihre Claude.ai Connectors zusätzlich zu den verwalteten Servern zu verwenden.
  - Kombiniere mit `allowedMcpServers`/`deniedMcpServers` für feinere Kontrolle.
- **Quelle:** [Changelog — code.claude.com](https://code.claude.com/docs/en/changelog) (offiziell), Stand: 2026-05-28

---

## Transport-Typen: HTTP ist Standard, SSE veraltet

- **Was:** Claude Code unterstützt drei Transporte: `streamable-http` (Alias: `http`), `sse`, `stdio`.
- **Best Practice:** Immer HTTP (streamable-http) verwenden. SSE ist offiziell deprecated — bei neuen Servern nicht mehr einsetzen, bestehende SSE-Server migrieren wenn möglich. Stdio für lokale Prozesse die direkten System-Zugriff brauchen.
- **Wichtig:** In `.mcp.json` und `add-json`-Befehlen akzeptiert das `type`-Feld `streamable-http` als Alias für `http` — kopierte Doku-Beispiele funktionieren ohne Anpassung.
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

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
- **Stand:** 2026-05-28

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
- **Stand:** 2026-05-28

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
- **Stand:** 2026-05-28

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
- **Stand:** 2026-05-28

---

## MCP Resources: @ Mentions

- **Was:** MCP-Server können Resources exponieren, die per `@`-Mention referenziert werden, ähnlich wie Dateien. Format: `@server:protocol://resource/path`
- **Best Practice:**
  - `@` tippen für Autocomplete-Menü mit allen verfügbaren Resources
  - Mehrere Resources in einem Prompt kombinieren: `Compare @postgres:schema://users with @docs:file://database/user-model`
  - Resources werden automatisch als Attachments eingebunden wenn referenziert
  - Fuzzy-Search in der Autocomplete
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

---

## MCP Prompts als Commands

- **Was:** MCP-Server können Prompts exponieren die als `/mcp__servername__promptname`-Commands verfügbar werden
- **Best Practice:**
  - `/` tippen für alle verfügbaren Commands inkl. MCP-Prompts
  - Mit Argumenten: `/mcp__github__pr_review 456`
  - Server- und Prompt-Namen werden normalisiert (Leerzeichen → Unterstriche)
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

---

## Channels: Externe Events in die Session pushen

- **Was:** MCP-Server können mit `claude/channel`-Capability Nachrichten aktiv in die laufende Session pushen — Claude reagiert auf externe Events (CI-Ergebnisse, Monitoring-Alerts, Chat-Nachrichten) ohne dass der Nutzer aktiv sein muss.
- **Best Practice:**
  - Opt-in beim Start: `--channels`-Flag
  - Server muss `claude/channel`-Capability deklarieren
  - Offiziell unterstützte Channels: [https://code.claude.com/docs/en/channels](https://code.claude.com/docs/en/channels)
  - Für eigene Channels: [https://code.claude.com/docs/en/channels-reference](https://code.claude.com/docs/en/channels-reference)
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

---

## Managed MCP: Enterprise-Kontrolle

- **Was:** Administratoren können via `managed-mcp.json` zentral steuern welche MCP-Server Nutzer verwenden dürfen. Ab 2.1.152: neues `allowAllClaudeAiMcps`-Flag um Claude.ai Connectors neben managed Servern zu erlauben.
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
  7. **Claude.ai Connectors erlauben:** `allowAllClaudeAiMcps: true` — Nutzer können ihre Cloud-Connectors zusätzlich verwenden (neu in 2.1.152)
- **WICHTIG ab 2.1.153:** Managed-Policies greifen jetzt auch für MCP-Server in Subagent-Frontmatter — vorher wurden diese Policies in Subagent-Kontexten umgangen.
- **Best Practice für Allowlist/Denylist:**
  - Immer `serverUrl` für Remote-Server (mit Wildcards: `https://mcp.example.com/*`)
  - Immer `serverCommand` für Stdio-Server (exakter Match aller Argumente!)
  - `serverName` ist KEIN Sicherheitskontroll — Nutzer können jeden Server beliebig benennen
  - Secrets niemals in `managed-mcp.json` (Datei ist maschinenlesbar) → `${VAR}` Expansion oder OAuth/headersHelper
- **Validierung:** `claude mcp add --transport http test https://example.com/mcp` sollte mit Policy-Fehler fehlschlagen wenn `managed-mcp.json` aktiv
- **Monitoring:** `OTEL_LOG_TOOL_DETAILS=1` für OpenTelemetry-Export mit MCP Server/Tool-Namen
- **Quelle:** [https://code.claude.com/docs/en/managed-mcp](https://code.claude.com/docs/en/managed-mcp) (offiziell)
- **Stand:** 2026-05-28

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
- **Stand:** 2026-05-28

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
- **NEU ab 2.1.152:** Plugin-Server die sich nur durch ENV-Variablen unterscheiden werden nicht mehr dedupliziert — beide laufen.
- **Best Practice:** Server-Name `workspace` ist reserviert (interner Gebrauch) → bei eigenen Servern vermeiden
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

---

## Reconnection & Reliability

- **Was:** HTTP/SSE-Server werden bei Verbindungsabbruch automatisch reconnected (exponential backoff: bis 5 Versuche, Start bei 1 Sekunde, verdoppelt sich). Ab v2.1.121: auch Initial-Connection wird bis zu 3x wiederholt bei transienten Fehlern (5xx, Connection Refused, Timeout). Stdio-Server werden nicht automatisch reconnected.
- **NEU ab 2.1.153:** Stateful HTTP-Server ohne GET-SSE-Stream laufen nicht mehr in Reconnect-Schleifen bei `tools/list` (Regression-Fix aus 2.1.147).
- **Best Practice:**
  - Für produktive Remote-Server HTTP statt SSE verwenden (beide haben Reconnect-Logik, HTTP ist aktueller Standard)
  - Bei Authentifizierungs- oder 404-Fehlern kein Retry — Config-Problem, nicht transienter Fehler
  - `/mcp` zeigt Server-Status und pending/failed Verbindungen
- **Dynamische Tool-Updates:** MCP `list_changed` Notifications werden unterstützt — Server können Tools live aktualisieren ohne Reconnect
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

---

## Plugin-gebundene MCP-Server

- **Was:** Plugins können MCP-Server mitbringen — automatisch aktiv wenn Plugin aktiviert, keine manuelle Konfiguration nötig.
- **ENV-Variablen in Plugin-MCP-Configs:**
  - `${CLAUDE_PLUGIN_ROOT}` → Plugin-Verzeichnis für mitgelieferte Binaries
  - `${CLAUDE_PLUGIN_DATA}` → Persistente Daten die Plugin-Updates überleben
  - `${CLAUDE_PROJECT_DIR}` → Aktuelles Projekt-Root
- **NEU ab 2.1.152:** Mehrere Plugin-MCP-Server mit gleicher Binary aber unterschiedlichen ENV-Vars laufen jetzt korrekt parallel (Dedup-Fix).
- **Best Practice:** `/reload-plugins` ausführen wenn Plugin während Session aktiviert/deaktiviert
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

---

## MCP Elicitation: Strukturierte Eingabe mid-Task

- **Was:** Server können via Elicitation strukturierte Eingaben vom Nutzer anfordern während Claude arbeitet. Automatisch als interaktiver Dialog oder Browser-URL angezeigt — keine Konfiguration nötig.
- **Modi:**
  - Form-Modus: Claude Code zeigt Formularfelder (z.B. Username/Password)
  - URL-Modus: Browser-URL für OAuth/Approval-Flows
- **Best Practice:** `Elicitation`-Hook für automatische Responses in nicht-interaktiven Umgebungen
- **Quelle:** [https://code.claude.com/docs/en/mcp](https://code.claude.com/docs/en/mcp) (offiziell)
- **Stand:** 2026-05-28

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

---

### Update 2026-06-05 (Claude Code 2.1.165) — MCP

**1. Secret-Redaction in `claude mcp` (2.1.161)**
- **Was:** `claude mcp list/get/add` expandiert `${VAR}`-Referenzen nicht mehr und redigiert Credential-Header und URL-Secrets.
- **Best Practice:** `.mcp.json` nie mit Klartext-Secrets; Referenzform `"env": { "TOKEN": "${TOKEN}" }`, Variable im Shell-Environment setzen (`~/.zshrc`, launchd, oder SK-Ordner `$HOME/SK/<projekt>/.env`). `.mcp.json` ist commitbar, sobald nur noch `${VAR}`-Referenzen drinstehen.
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**2. per-Server `timeout` < 1000 ms (2.1.162)**
- **Was:** Werte < 1000 ms werden jetzt ignoriert (Fallback auf `MCP_TOOL_TIMEOUT`/Default) statt auf einen 1s-Watchdog gefloort, der jeden Tool-Call abbrach. `claude mcp get` annotiert das.
- **Best Practice:** Nie < 1000 setzen. Lokale stdio-Server: >= 30000 (30s); langsame externe APIs: 300000-600000. Server-Start-Timeout separat via `MCP_TIMEOUT`-Env.
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**3. `CLAUDE_CODE_SESSION_ID` in stdio-MCP bei `--resume` (2.1.163)**
- **Was:** Stdio-MCP-Server bekommen bei `--resume` dieselbe Session-ID wie Hooks/Bash.
- **Nutzen:** Zustandsbehaftete Server (eigene Logger, ClaudeWatch-artig) koennen Session-Korrelation betreiben und Kontext anhand der ID nachladen.
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**4. `/mcp` blendet ungenutzte claude.ai-Connectors aus (2.1.161)** — rein kosmetisch, kein Handlungsbedarf. `[offiziell]`

**Betrifft eigene Werkzeuge:** Ja — die macOS-`.mcp.json` auf `${VAR}`-Secret-Referenzen pruefen; jede Klartext-Credential auf `"${VARNAME}"` umstellen + Variable im Shell-Env exportieren.
