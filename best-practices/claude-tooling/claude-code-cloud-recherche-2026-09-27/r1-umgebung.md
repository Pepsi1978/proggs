# Researcher 1: Umgebung und Laufzeit einer Cloud-Sitzung (Claude Code on the web)

Stand der Recherche: 27.09.2026, lokale CLI-Version 2.1.283.

## Befunde

### VM/Container-Architektur, OS

- "In Anthropic-hosted environments, each session gets a fresh virtual machine (VM) running Ubuntu 24.04 on x86_64, regardless of your own operating system and CPU architecture, with your repository cloned and common toolchains pre-installed." [offiziell] https://code.claude.com/docs/en/cloud-environments
- "Setup scripts... Scripts run as root on Ubuntu 24.04, so `apt install` und most language package managers work." [offiziell] https://code.claude.com/docs/en/cloud-environments
- Aus der Isolation/Sandbox-Übersichtsseite: "Cloud sessions: A cloud session runs in an isolated, Anthropic-managed virtual machine. A network proxy enforces a default allowlist, and a separate proxy holds your GitHub token outside the sandbox while issuing scoped credentials for repository access inside it." [offiziell] https://code.claude.com/docs/en/sandbox-environments
- "Each cloud session is separated from your machine and from other sessions through several layers: Isolated virtual machines... Network access controls... Credential protection... API credentials... Secure analysis: code is analyzed and modified within the session's isolated environment before creating PRs." [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- Ergänzend ein externer (nicht offizieller) Blogpost, der die technische Isolationsschicht genauer beschreibt: "Evidence points to a gVisor backed container running under a VM, with a PID 1 init-like binary called process_api which is responsible for resource management. Claude Code on the Web accepts natural language only. It does not give you a raw interactive shell from the web client." [extern] https://cw00h.github.io/posts/2025/10/claude-code-web-sandbox/ — Das ist eine unabhängige Reverse-Engineering-Analyse, nicht offiziell bestätigt; als "unsicher/detailliert, aber extern" markieren.
- Kein Shell-Zugriff für den Nutzer: "You don't get a shell into the session VM. Claude runs every command for you, so phrase the tasks in this section as requests in your prompt." [offiziell] https://code.claude.com/docs/en/cloud-environments

### Vorinstallierte Sprachen/Tools

Tabelle aus der offiziellen Doku (wörtlich übernommen) [offiziell] https://code.claude.com/docs/en/cloud-environments:

| Kategorie | Enthalten |
|---|---|
| Python | Python 3.x mit pip, poetry, uv, black, mypy, pytest, ruff |
| Node.js | 20, 21, 22, mit npm, yarn, pnpm, bun¹, eslint, prettier, chromedriver |
| Ruby | 3.1, 3.2, 3.3 mit gem, bundler, rbenv |
| PHP | 8.3 mit Composer |
| Java | OpenJDK 21 mit Maven und Gradle |
| Go | Go mit Modul-Unterstützung |
| Rust | rustc und cargo |
| C/C++ | GCC, Clang, cmake, ninja, conan |
| Docker | docker, dockerd, docker compose |
| Datenbanken | PostgreSQL 16, Redis 7.0 |
| Utilities | git, gh, jq, yq, ripgrep, tmux, vim, nano |

¹ "Bun is installed but has known proxy compatibility issues for package fetching."

- Node-Versionen liegen unter `/opt/node20`, `/opt/node21`, `/opt/node22`, Node 22 ist standardmäßig auf `PATH`.
- "To get the versions of most of the tools in this table, ask Claude to run `check-tools` in a cloud session." — ein vorinstalliertes Shell-Kommando auf der VM.
- .NET SDK und andere nicht gelistete Toolchains sind NICHT vorinstalliert, auch wenn deren Package-Registries (z.B. nuget.org) im Default-Allowlist stehen — Installation nur per Setup-Skript. [offiziell] https://code.claude.com/docs/en/cloud-environments
- `gh` CLI ist vorinstalliert und authentifiziert automatisch über den GitHub-Proxy (kein `gh auth login` nötig, `GH_TOKEN` liest ggf. den Platzhalter `proxy-injected`). [offiziell] ebenda
- PostgreSQL und Redis sind vorinstalliert, laufen aber standardmäßig NICHT — müssen explizit gestartet werden (`service postgresql start`, `service redis-server start`). [offiziell] ebenda

### Ressourcen (CPU/RAM/Disk)

Wörtliches Zitat: "Cloud sessions in Anthropic-hosted environments run with approximate resource ceilings that may change over time: 4 vCPUs, 16 GB of RAM, 30 GB of disk. The VM may stop tasks that need significantly more memory, such as large build jobs or memory-intensive tests." [offiziell] https://code.claude.com/docs/en/cloud-environments

### Zeit-/Timeout-Limits

Wörtlich [offiziell] https://code.claude.com/docs/en/cloud-environments:
- "Commands Claude runs: a cloud environment doesn't set its own command timeout, so the Bash tool's defaults apply. Claude waits 2 minutes for a command by default and can ask for up to 10 minutes. When a command reaches its timeout, Claude Code moves it to the background instead of stopping it, unless the command starts with `sleep`."
- "SessionStart hooks: Claude Code cancels a `command` hook after 600 seconds unless you set `timeout`, in seconds, on the hook entry. Claude Code doesn't enforce the timeout on a hook you run with `async: true`."
- "Setup script: a script that takes longer than roughly five minutes isn't cached."
- "Idle sessions: a session stops after a period of inactivity and its VM is reclaimed."
- Anpassbar über Umgebungsvariablen: `BASH_DEFAULT_TIMEOUT_MS` und `BASH_MAX_TIMEOUT_MS` (in Millisekunden), z.B. `BASH_DEFAULT_TIMEOUT_MS=600000` für 10 Minuten Standard.

Abschnitt "Environment expired" (Inaktivität) [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web:
- "Cloud sessions stop after a period of inactivity and the session's VM is reclaimed. A session counts as inactive while it waits for you to approve an MCP connector tool call or to sign in to an MCP server, and it can expire during that wait."
- "Reopen the session from claude.ai/code to provision a fresh VM with your conversation history restored. Background work that was still running when the VM was reclaimed, such as subagents and shell commands, isn't restored."
- Eine konkrete Zahl von Minuten/Stunden für "Inaktivität" nennt die Doku NICHT — nur "a period of inactivity" (als unsicher markiert, siehe unten).
- Klar getrennt davon: die allgemeinen Claude-Nutzungslimits (5-Stunden-Fenster, Wochenlimit) betreffen Nachrichten/Budget, NICHT die VM-Laufzeit selbst — das ist eine andere Ebene. [extern, zur Einordnung] https://sessionwatcher.com/guides/claude-code-rate-limits-explained — als Kontext, nicht als Quelle für VM-Timeouts verwendet.

### Was bleibt zwischen Sitzungen erhalten (Persistenz, Caches)

- "Setup script runs the first time you start a session in an environment. When setup completes within roughly five minutes, Anthropic snapshots the filesystem and reuses that snapshot as the starting point for later sessions. New sessions start with your dependencies, tools, and Docker images already on disk, and skip the setup script step." [offiziell] https://code.claude.com/docs/en/cloud-environments
- "The cache is a filesystem snapshot, so it keeps what the setup script writes to disk and loses anything that was only running. Packages you install, Docker images you pull, and files you write all carry over. A database the script started, a `docker compose up` stack, or any other background process doesn't."
- "The setup script runs again to rebuild the cache when you change the environment's setup script or allowed network hosts, and when the cache reaches its expiry after roughly seven days. Resuming an existing session never re-runs the setup script."
- Tabelle "What carries over from your setup" (Auszug, wörtlich übernommen wo zitiert) [offiziell] ebenda:
  - Repo-`CLAUDE.md`: Ja (Teil des Clones)
  - Repo-`.claude/settings.json` Hooks/Permission-Rules: Ja, bei Session mit EINEM Repository
  - Repo-`.mcp.json`: Ja, bei Session mit einem Repository
  - Repo-`.claude/rules/`, `.claude/skills/`, `.claude/agents/`, `.claude/commands/`: Ja
  - Plugins/Marketplaces aus `.claude/settings.json` (`enabledPlugins`, `extraKnownMarketplaces`): NEIN — "A cloud session doesn't install the plugins a repository turns on"
  - Organisations-Server-Managed-Settings: Ja (bei Sessionstart von Anthropic-Servern geladen)
  - User-`~/.claude/CLAUDE.md`, User-Skills/Agents/Commands: NEIN — liegen nur auf der eigenen Maschine
  - Plugins nur in User-Settings aktiviert: NEIN
  - MCP-Server via `claude mcp add` (Default-Scope/User-Scope): NEIN — nur `--scope project` (schreibt `.mcp.json`) wird übernommen
  - Transport-Variablen wie `NODE_EXTRA_CA_CERTS`, mTLS-Client-Zertifikat-Variablen: NEIN — werden ignoriert, Hinweis im Debug-Log
  - API-Keys/Tokens: nur auf Pro/Max als "API credentials" (siehe unten); sonst normale Env-Variable
  - Interaktive Auth wie AWS SSO: NEIN — nicht unterstützt

### Environments: Setup-Skripte, Umgebungsvariablen, Secrets

- Konfigurierbar pro "Cloud Environment" (Auswahl über den Umgebungs-Selector bei claude.ai/code): Netzwerkzugriff, Umgebungsvariablen, API-Credentials (Pro/Max), Setup-Skript. [offiziell] https://code.claude.com/docs/en/cloud-environments
- Umgebungsvariablen im `.env`-Format, einmal beim Sessionstart in echte Env-Variablen kopiert; laufende Sessions lesen Änderungen nicht nach. "Anyone who uses the environment can read the values." — also KEINE Secrets dort ablegen.
- API-Credentials (nur Pro/Max, nicht Team/Enterprise): API-Key/Token wird auf der Environment gespeichert; der "agent proxy" hängt ihn serverseitig an Requests für gelistete Hosts an, NACHDEM die Anfrage die VM verlassen hat — Claude/der Code in der Session sieht den Schlüssel nie. Ausnahmen ("requests that never get the credential"): GitHub (eigener Proxy), Anthropic API + öffentliche Package-Registries, und Setup-Skript-Requests (Setup läuft, bevor Claude Code überhaupt den Agent-Proxy kontaktiert).
- Setup-Skript: "A setup script is a Bash script that runs when a new cloud session starts, before Claude Code launches. ... Scripts run as root on Ubuntu 24.04". Muss mit Exit-Code 0 enden (`|| true` für unkritische Befehle), sollte unter ~5 Minuten bleiben (sonst kein Caching), braucht Netzwerkzugriff für Installationen.
- GitHub-Zugriff: eigener "GitHub proxy" hält echte Credentials außerhalb der VM; `git push` funktioniert nur gegen den aktuellen Arbeits-Branch der Session (Push Protection); GraphQL ist auf einen festen Satz von PR-Operationen beschränkt (alles andere → 403 mit Verweis auf REST-Fallback `gh api ...`).
- Security-Proxy: aller ausgehender Traffic einer Anthropic-gehosteten Session läuft durch einen HTTP/HTTPS-Proxy (Malicious-Request-Schutz, Rate Limiting, Content Filtering, DNS-Audit-Trail).
- Netzwerk-Zugriffslevel pro Environment: None / Trusted (Default, umfangreiche Allowlist an Registries/GitHub/Cloud-SDKs) / Full (alle Domains) / Custom (eigene Liste, optional plus Trusted-Defaults). Eine sehr lange Default-Allowlist ist dokumentiert (npm, PyPI, RubyGems, crates.io, Go-Proxy, Maven/Gradle, NuGet, Docker-Registries, Cloud-SDKs von AWS/GCP/Azure, Ubuntu-Repos etc.) [offiziell] ebenda.

### SessionStart-Hook vs. Setup-Skript (Installation von Abhängigkeiten)

Wörtliche Vergleichstabelle [offiziell] https://code.claude.com/docs/en/cloud-environments:

| | Setup-Skript | SessionStart-Hook |
|---|---|---|
| Wo konfiguriert | Environment-Dialog bei claude.ai/code (bzw. Admin-Seite für Shared Environments) | Settings-Datei, z.B. Repo-`.claude/settings.json` |
| Wann ausgeführt | Vor Start von Claude Code, wird bei gecachter Environment übersprungen | Nach Start von Claude Code, bei jeder Session inkl. Resume |
| Wo ausgeführt | Nur Cloud-Sessions | Lokal UND Cloud |

- Empfehlung: Setup-Skript für VM-Provisionierung (Toolchains, CLI-Tools, die nicht vorinstalliert sind); SessionStart-Hook für Projekt-Setup, das überall laufen soll (z.B. `npm install`).
- Cloud-only-Scoping bei SessionStart-Hooks nur über Prüfung von `CLAUDE_CODE_REMOTE != "true"` → früh `exit 0` (Beispielskript in der Doku, siehe Code-Beispiel unten).
- Limitation: Bei einer Session mit MEHREREN Repositories werden GAR KEINE Hooks aus `.claude/settings.json` geladen (auch keine SessionStart-Hooks) — dann nur Setup-Skript nutzbar.
- SessionStart-Hooks erzeugen bei jedem Sessionstart Latenz (kein Caching wie beim Setup-Skript) — Doku empfiehlt, vorher zu prüfen ob Abhängigkeiten schon vorhanden sind.
- Proxy-Kompatibilität: manche Package-Manager funktionieren nicht korrekt mit dem Security-Proxy, Bun ist ein bekanntes Beispiel.

Offizielles Codebeispiel (wörtlich) [offiziell] https://code.claude.com/docs/en/cloud-environments:

```json
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "startup|resume",
        "hooks": [
          {
            "type": "command",
            "command": "bash \"$CLAUDE_PROJECT_DIR\"/scripts/install_pkgs.sh"
          }
        ]
      }
    ]
  }
}
```

```bash
#!/bin/bash

if [ "$CLAUDE_CODE_REMOTE" != "true" ]; then
  exit 0
fi

npm install
pip install -r requirements.txt
exit 0
```

### CLAUDE_CODE_REMOTE und weitere relevante Umgebungsvariablen

- "The `$CLAUDE_CODE_REMOTE` environment variable is `\"true\"` in remote web environments and not set in the local CLI." [offiziell] https://code.claude.com/docs/en/hooks (Abschnitt "Command hook fields")
- "Claude Code v2.1.199 and later sets `$CLAUDE_CODE_BRIDGE_SESSION_ID` to the Remote Control session ID while the local session has an active Remote Control connection." — relevant zur Abgrenzung: Remote Control (Steuerung einer LOKALEN Session vom Handy/Browser aus) ist NICHT dasselbe wie eine Cloud-Session; dort läuft der Code weiter auf der eigenen Maschine, `CLAUDE_CODE_REMOTE` bleibt unset. [offiziell] https://code.claude.com/docs/en/cloud-environments ("Remote Control sessions connect the web and mobile interfaces to a session on your own machine, which uses your machine's network and files, not a cloud environment.")
- `CLAUDE_CODE_REMOTE_SESSION_ID`: "Each cloud session has a transcript URL on claude.ai, and the session can read its own ID from the `CLAUDE_CODE_REMOTE_SESSION_ID` environment variable." Format-Hinweis: Prefix `cse_`, umwandelbar in `session_` für die Transcript-URL. [offiziell] https://code.claude.com/docs/en/cloud-environments
- `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE`: wird von der Cloud-Session SELBST gesetzt (überschreibt einen eigenen Eintrag in den Environment-Variablen) — steuert, wann Auto-Compaction einsetzt. [offiziell] ebenda
- `BASH_DEFAULT_TIMEOUT_MS` / `BASH_MAX_TIMEOUT_MS`: konfigurierbar über Environment-Variablen, steuern Bash-Timeouts (Standard 120000ms / 600000ms). [offiziell] https://code.claude.com/docs/en/cloud-environments und https://code.claude.com/docs/en/env-vars
- `GH_TOKEN` / `GITHUB_TOKEN`: falls nicht selbst gesetzt und GitHub-Proxy authentifiziert, lesen beide als Platzhalter `proxy-injected` in Kommandos, die Claude ausführt — der Proxy ersetzt sie serverseitig bei ausgehenden GitHub-Requests. [offiziell] https://code.claude.com/docs/en/cloud-environments
- `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`: schaltet "Agent teams" (standardmäßig aus) in Cloud-Sessions frei. [offiziell] https://code.claude.com/docs/en/claude-code-on-the-web
- `CCR_FORCE_BUNDLE=1`: erzwingt Upload eines lokalen Repository-Bundles statt Clone via GitHub-Remote (bei `claude --cloud`). [offiziell] ebenda
- Weitere generelle (nicht cloud-spezifische, aber cloud-relevante) Variablen aus der `env-vars`-Referenz [offiziell] https://code.claude.com/docs/en/env-vars:
  - `API_TIMEOUT_MS` (Standard 600000ms, Max 2147483647)
  - `API_FORCE_IDLE_TIMEOUT` (Override des 5-Minuten-Idle-Timeouts für Streaming-Responses)
  - `CLAUDE_STREAM_IDLE_TIMEOUT_MS`, `CLAUDE_BYTE_STREAM_IDLE_TIMEOUT_MS` (v2.1.210+)
  - `CLAUDE_ASYNC_AGENT_STALL_TIMEOUT_MS` (Standard 600000ms, Stall-Timeout für Subagents)
  - `CLAUDE_AUTO_BACKGROUND_TASKS=1` (automatisches Backgrounding langlaufender Tasks)
  - `BASH_MAX_OUTPUT_LENGTH` (Standard 30000 Zeichen, Max 150000)
  - `CLAUDE_AFK_TIMEOUT_MS` / `CLAUDE_AFK_COUNTDOWN_MS` (Auto-Continue bei unbeantworteter `AskUserQuestion`)

### Erkennung "läuft die Session in der Cloud?"

Belastbare Erkennungsmerkmale, offiziell dokumentiert:
1. `CLAUDE_CODE_REMOTE === "true"` → zuverlässigster Indikator (nur in Cloud-Sessions gesetzt, lokal gar nicht vorhanden). [offiziell]
2. `CLAUDE_CODE_REMOTE_SESSION_ID` ist gesetzt (Präfix `cse_`). [offiziell]
3. Kein Zugriff auf `C:\Users\barwa\SK` bzw. das lokale Dateisystem des Nutzers — indirekter Hinweis, aber nicht offiziell als Erkennungsmerkmal dokumentiert, sondern aus den Projektregeln des Nutzers (AGENTS.md) und aus "Anything you've installed or configured only on your own machine isn't available in the session." [offiziell, indirekt] https://code.claude.com/docs/en/cloud-environments

### Changelog-Kontext (lokale CLI-Version 2.1.283, aktuellster Stand)

Auszug aus dem offiziellen GitHub-Changelog (wörtlich, cloud-relevante Einträge der letzten Versionen) [offiziell] https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md:

- 2.1.283: "Cloud Sessions: Verbesserte Hinzufügung von Repositories zu laufenden Cloud-Sitzungen; private Repos sind jetzt auch lesbar" / "Behoben: Cloud-Sitzungen wiederholten bereits abgeschlossene Schritte nach Server-Restarts"
- 2.1.282: "Cloud Sessions: GitHub Token erneuert sich jetzt automatisch nach ~8 Stunden" / "Routine-Zeitpläne starten standardmäßig einige Minuten nach der vollen Stunde"
- 2.1.281: "Repositories von verschiedenen GitHub-Besitzern können zu laufenden Sessions hinzugefügt werden" / Bugfix stündliche Routinen bei Halbstunden-Zeitzonen
- 2.1.280: "Standardmodell auf allen Plänen geändert (Opus statt Sonnet)" / "Dateilinks außerhalb des Arbeitsverzeichnisses sind jetzt deaktiviert"
- 2.1.277: "WebFetch und WebSearch zeigen nun Gründe für Ablehnung (Budget, Admin-Richtlinie)"

Bemerkenswert für den Bug-Kandidaten unten: Der GitHub-Token erneuert sich laut 2.1.282 "automatisch nach ~8 Stunden" — das deutet indirekt darauf hin, dass VOR dieser Version bei sehr langen Sessions (>8h) GitHub-Zugriff abbrechen konnte.

## BEST-PRACTICES-KANDIDATEN:

1. Für ein SessionStart-Hook-Skript, das nur in Cloud-Sessions Abhängigkeiten installieren soll, immer `if [ "$CLAUDE_CODE_REMOTE" != "true" ]; then exit 0; fi` als ersten Schritt verwenden — verhindert unnötige/störende Installationsläufe auf der lokalen Maschine. Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt "Install dependencies with a SessionStart hook")
2. Große/seltene Abhängigkeiten (Toolchains, Docker-Images, apt-Pakete) über ein Setup-Skript installieren statt über einen SessionStart-Hook, weil das Setup-Skript-Ergebnis als Filesystem-Snapshot gecacht wird (kein erneutes Installieren bei jeder Session) — SessionStart-Hooks laufen dagegen bei JEDEM Sessionstart erneut und kosten Latenz. Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt "Setup scripts vs. SessionStart hooks")
3. Setup-Skript unbedingt unter ~5 Minuten halten (sonst kein Caching) und mit Exit-Code 0 enden (`|| true` an unkritische Befehle hängen), sonst startet die Session gar nicht bzw. jede neue Session installiert alles neu. Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt "Script requirements")
4. Niemals Secrets/API-Keys in den "Environment variables" eines Cloud-Environments ablegen — jeder, der die Environment nutzt, kann sie im Klartext lesen. Auf Pro/Max stattdessen "API credentials" verwenden (Schlüssel bleibt außerhalb der Sandbox, wird serverseitig vom Agent-Proxy angehängt). Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitte "Set environment variables" und "Add API credentials")
5. Bei Sessions mit MEHREREN Repositories (z.B. Projekt-Threads) werden `.claude/settings.json`-Hooks und `.mcp.json` NICHT geladen — für solche Fälle Abhängigkeiten nur über das Setup-Skript der Environment bereitstellen, nicht über SessionStart-Hooks. Quelle: https://code.claude.com/docs/en/cloud-environments (Tabelle "What carries over from your setup" + Abschnitt "Limitations in cloud sessions")
6. Rechenintensive Builds/Tests im Blick behalten: VM-Limits sind ca. 4 vCPUs, 16 GB RAM, 30 GB Disk — bei speicherhungrigen Build-/Testläufen kann die VM den Task abbrechen; für größere Workloads Remote Control (eigene Hardware) oder Self-Hosted Environment nutzen. Quelle: https://code.claude.com/docs/en/cloud-environments (Abschnitt "Resource limits")

## BUG-KANDIDATEN:

- Symptom: Bun-Paketinstallationen (via SessionStart-Hook oder Setup-Skript) können in Cloud-Sessions fehlschlagen oder sich falsch verhalten.
  Ursache: "Bun is installed but has known proxy compatibility issues for package fetching" — der Security-Proxy, durch den aller ausgehender Traffic einer Anthropic-gehosteten Session läuft, ist mit manchen Package-Managern (explizit Bun genannt) nicht vollständig kompatibel.
  Version: aktueller Stand (27.09.2026), keine Versionsangabe für eine Behebung dokumentiert.
  Workaround: laut Doku keiner explizit genannt außer ggf. Ausweichen auf npm/yarn/pnpm oder auf ein Self-Hosted Environment (eigener Netzwerk-Grenzbereich statt Anthropic-Security-Proxy).
  URL: https://code.claude.com/docs/en/cloud-environments (Fußnote ¹ zu "Node.js" in der Tabelle "Installed tools", und Abschnitt "Limitations in cloud sessions" → "Proxy compatibility")

- Symptom: In sehr langen Cloud-Sessions (vor v2.1.282) konnte der GitHub-Zugriff nach längerer Laufzeit fehlschlagen.
  Ursache: laut Changelog-Eintrag zu 2.1.282 erneuert sich das GitHub-Token jetzt automatisch nach ~8 Stunden — impliziert, dass vor diesem Fix Tokens nach ca. 8 Stunden abliefen und nicht automatisch erneuert wurden.
  Version: behoben in 2.1.282 (Changelog-Formulierung ist knapp, keine explizite "vorher"-Beschreibung des Fehlerverhaltens vorhanden — daher mit Vorsicht als Kandidat, nicht als gesichertes Bug-Symptom).
  Workaround: auf Version ≥2.1.282 aktualisieren (aktuelle CLI-Version 2.1.283 hat den Fix bereits).
  URL: https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md (Eintrag 2.1.282)

## OFFEN/UNSICHER:

- Die exakte Dauer für "Idle sessions" / "Environment expired" (wie viele Minuten/Stunden Inaktivität bis die VM reclaimed wird) ist in der offiziellen Doku NICHT als Zahl genannt — nur "a period of inactivity". Keine belastbare Quelle für eine konkrete Zahl gefunden.
- Die genaue technische Isolationsschicht (gVisor, "process_api" als PID 1 etc.) stammt nur aus einer externen Reverse-Engineering-Quelle (cw00h.github.io) und ist NICHT offiziell von Anthropic bestätigt oder dokumentiert. Mit Vorsicht behandeln.
- Ob es eine absolute Obergrenze für die Gesamtlaufzeit einer einzelnen Cloud-Session gibt (unabhängig von Inaktivität), z.B. ein hartes Maximum nach X Stunden/Tagen, wurde in der offiziellen Doku nicht gefunden. Die "5-Stunden-Fenster" und "Wochenlimit" aus externen Quellen beziehen sich auf Nachrichten-/Nutzungsbudget des Abo-Plans, nicht auf eine VM-Laufzeitgrenze — diese beiden Konzepte sollten in der Zusammenfassung klar getrennt werden.
- Ob und wie genau `check-tools` als Kommando dokumentiert ist (vollständige Ausgabe/Versionsliste) wurde nicht im Detail geprüft, nur die Existenz und der Zweck sind bekannt.
- Für Team/Enterprise-Pläne fehlen "API credentials" aktuell ("not available yet") — unklar, ob/wann das nachgezogen wird.
