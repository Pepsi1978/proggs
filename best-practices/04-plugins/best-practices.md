# Plugins — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

---

## Plugin vs. Standalone-Konfiguration

- **Was:** Plugins sind eigenständige Verzeichnisse mit `.claude-plugin/plugin.json` und gebündelten Komponenten. Standalone-Konfigurationen liegen in `.claude/` des Projekts.
- **Best Practice:** Standalone für persönliche/Projekt-Experimente nutzen (kurze Skill-Namen wie `/hello`). Plugins erst dann, wenn etwas geteilt, versioniert oder über Marketplaces verteilt werden soll. Tipp: Erst als Standalone entwickeln, dann in ein Plugin umwandeln.
- **Wann Plugins:** Team-Sharing, mehrere Projekte, Marketplace-Distribution, Versionierung.
- **Wann Standalone:** Ein Projekt, persönlich, schnelle Iteration.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** 2026-04-02

---

## plugin.json — Manifest-Struktur (vollständiges Schema)

- **Was:** Das Manifest liegt in `.claude-plugin/plugin.json`. Es ist **optional** — Claude Code erkennt Komponenten auch ohne Manifest (Auto-Discovery). Einziges Pflichtfeld wenn vorhanden: `name`.
- **Komplettes Schema:**
  ```json
  {
    "name": "plugin-name",           // Pflicht (kebab-case, kein Leerzeichen)
    "displayName": "Plugin Name",    // Neu ab v2.1.143: menschenlesbarer Name für UI
    "version": "1.2.0",              // Optional — steuert Update-Verhalten
    "description": "...",
    "author": { "name": "...", "email": "...", "url": "..." },
    "homepage": "https://...",
    "repository": "https://github.com/...",
    "license": "MIT",
    "keywords": ["keyword1"],
    "$schema": "https://json.schemastore.org/claude-code-plugin-manifest.json",

    // Komponenten-Pfade (alle optional, überschreiben Defaults)
    "skills": "./custom/skills/",
    "commands": ["./custom/cmd.md"],
    "agents": ["./custom/agents/reviewer.md"],
    "hooks": "./config/hooks.json",
    "mcpServers": "./mcp-config.json",
    "outputStyles": "./styles/",
    "lspServers": "./.lsp.json",

    // Experimentell (Schema kann sich noch ändern)
    "experimental": {
      "themes": "./themes/",
      "monitors": "./monitors.json"
    },

    // Benutzer-Konfiguration (wird beim Aktivieren abgefragt)
    "userConfig": { "api_endpoint": { } },

    // Abhängigkeiten zu anderen Plugins
    "dependencies": [
      "helper-lib",
      { "name": "secrets-vault", "version": "~2.1.0" }
    ]
  }
  ```
- **Best Practice:** `$schema`-Feld einsetzen für Editor-Autocomplete. `displayName` (v2.1.143+) für saubere UI-Anzeige nutzen.
- **Wichtig:** Unbekannte Top-Level-Felder werden **ignoriert** (kein Fehler), falsche Typen sind Load-Fehler. Manifest kann parallel als npm `package.json` oder VS-Code-Extension-Manifest dienen.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## Plugin-Verzeichnisstruktur

- **Was:** Alle Komponenten-Verzeichnisse liegen an der **Plugin-Wurzel**, NICHT innerhalb von `.claude-plugin/`.
  ```
  my-plugin/
  ├── .claude-plugin/
  │   └── plugin.json          ← NUR die Manifest-Datei hier
  ├── skills/                  ← <name>/SKILL.md
  ├── commands/                ← flache .md-Dateien (Legacy)
  ├── agents/                  ← .md-Dateien
  ├── hooks/
  │   └── hooks.json
  ├── .mcp.json                ← MCP-Server-Konfiguration
  ├── .lsp.json                ← LSP-Server-Konfiguration
  ├── monitors/
  │   └── monitors.json
  ├── themes/                  ← experimentell
  ├── bin/                     ← Executables im Bash-PATH
  ├── settings.json            ← Default-Settings wenn Plugin aktiv
  └── README.md
  ```
- **Best Practice:** `skills/` für neue Plugins bevorzugen (statt `commands/`). `commands/` ist Legacy und wird nur noch für Rückwärtskompatibilität unterstützt. Häufiger Fehler: Skill-Verzeichnisse fälschlich in `.claude-plugin/` ablegen — dort gehört nur `plugin.json` rein.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** 2026-04-02

---

## Versions-Management und SHA-Pinning

- **Was:** Das Feld `version` in `plugin.json` steuert das Update-Verhalten.
  - **Mit `version`:** Nutzer erhalten Updates nur, wenn der Entwickler die Versions-Nummer bumpt.
  - **Ohne `version`:** Claude Code nutzt den Git-Commit-SHA als Version — jeder Commit zählt als neues Update.
  - Im **Community-Marketplace** (`anthropics/claude-plugins-community`) wird jedes genehmigte Plugin auf einen **spezifischen Commit-SHA** gepinnt. CI bumpt den Pin automatisch bei neuen Commits.
- **Best Practice:**
  - **Produktion / Marketplace:** Semantische Versionen (`"version": "1.2.0"`) nutzen — kontrolliertes Update-Verhalten für Nutzer.
  - **Interne Teams ohne Versionssteuerung:** Ohne `version`-Feld arbeiten wenn jeder Commit sofort verfügbar sein soll.
  - Separate `stable`- und `beta`-Branches für Release-Channels führen.
  - Im Marketplace-Eintrag darf auch eine SHA gepinnt werden — aber `plugin.json` gewinnt wenn beide gesetzt.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## Komponenten: Skills, Agents, Hooks, MCP, LSP, Monitors

### Skills

- Verzeichnis `skills/<name>/SKILL.md` → wird als `/plugin-name:skill-name` verfügbar.
- `$ARGUMENTS` im SKILL.md-Body für dynamische Eingabe.
- Frontmatter: `description` (für Claude-Erkennung) und `disable-model-invocation: true` (nur manuell auslösen).
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)

### Hooks (in `hooks/hooks.json`)

- Gleiches Format wie Benutzer-Hooks. Alle Events unterstützt: SessionStart, PreToolUse, PostToolUse, SubagentStart, SubagentStop, FileChanged, WorktreeCreate, CwdChanged, InstructionsLoaded etc.
- Hook-Typen: `command`, `http`, `mcp_tool`, `prompt`, `agent`.
- Pfad zu Plugin-Dateien via `${CLAUDE_PLUGIN_ROOT}`.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)

### MCP-Server (in `.mcp.json`)

- Starten automatisch wenn Plugin aktiv. `${CLAUDE_PLUGIN_ROOT}` und `${CLAUDE_PLUGIN_DATA}` als Variablen nutzbar.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)

### LSP-Server (in `.lsp.json`)

- Für Code-Intelligence (Diagnose, Go-To-Definition, Find References).
- Binary muss separat installiert sein — Plugin konfiguriert nur die Verbindung.
- Offizielle LSP-Plugins: `pyright-lsp`, `typescript-lsp`, `rust-analyzer-lsp`, `gopls-lsp`, `kotlin-lsp`, `csharp-lsp`, `swift-lsp` etc.
- **Best Practice:** Offizielle LSP-Plugins aus dem Marketplace nutzen, eigene nur für nicht abgedeckte Sprachen erstellen.
- **Quelle:** https://code.claude.com/docs/en/discover-plugins (offiziell)

### Background Monitors (experimentell, ab v2.1.105)

- Hintergrundprozesse die Logs/Status beobachten und Claude automatisch benachrichtigen.
- `when: "on-skill-invoke:<name>"` startet Monitor erst beim ersten Skill-Aufruf (ressourcenschonend).
- Monitors stoppen erst beim Session-Ende, nicht beim Deaktivieren des Plugins.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## Default-Settings mit Plugin ausliefern

- **Was:** `settings.json` an der Plugin-Wurzel setzt Default-Konfiguration wenn Plugin aktiv. Derzeit nur `agent` und `subagentStatusLine` unterstützt.
- **Best Practice:** `"agent": "security-reviewer"` aktiviert einen Plugin-Agent als Hauptthread — nützlich für spezialisierte Plugins (z.B. Security-Review-Plugin das immer den Review-Agent nutzt). Priorität: `settings.json` überschreibt `settings` aus `plugin.json`. Unbekannte Keys werden still ignoriert.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** 2026-04-02

---

## User-Konfiguration (userConfig)

- **Was:** `userConfig`-Feld in `plugin.json` lässt Claude Code den Nutzer beim Aktivieren nach Werten fragen (z.B. API-Endpoints, Tokens). Werte über `${user_config.feldname}` in Hooks, MCP und Monitors verfügbar.
- **Best Practice:** Statt Nutzer zu bitten, `settings.json` manuell zu bearbeiten, `userConfig` nutzen für interaktive Eingabe beim ersten Aktivieren. Keine Secrets hardcoden.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## Abhängigkeiten zwischen Plugins (dependencies)

- **Was:** `dependencies`-Array in `plugin.json` deklariert andere Plugins als Voraussetzung, optional mit Semver-Constraint.
  ```json
  "dependencies": [
    "helper-lib",
    { "name": "secrets-vault", "version": "~2.1.0" }
  ]
  ```
- **Best Practice:** Abhängigkeiten explizit deklarieren — Claude Code installiert sie automatisch mit. Im `/plugin`-Installed-Tab werden Plugins mit ungelösten Abhängigkeiten oben angezeigt.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## Installation: Scopes

- **Was:** Drei Scopes beim Installieren:

  | Scope | Datei | Zweck |
  |-------|-------|-------|
  | `user` | `~/.claude/settings.json` | Persönlich, alle Projekte (Default) |
  | `project` | `.claude/settings.json` | Team, via Git geteilt |
  | `local` | `.claude/settings.local.json` | Persönlich, dieses Projekt, gitignored |
  | `managed` | Managed Settings | Admins, read-only |

- **Best Practice:** Team-Plugins immer im `project`-Scope installieren (wird in `.claude/settings.json` gespeichert und committed). Persönliche Extras im `user`-Scope. CLI: `claude plugin install formatter@your-org --scope project`
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## ZIP-Install und URL-Loading (ab v2.1.128)

- **Was:** `--plugin-dir ./my-plugin.zip` lädt Plugin aus ZIP-Archiv (ab v2.1.128). `--plugin-url https://...` lädt ZIP von URL — nützlich für CI-Build-Artefakte.
- **Best Practice:**
  - `--plugin-dir` für lokale Entwicklung/Tests (überschreibt installierte gleichnamige Plugins für die Session).
  - `--plugin-url` für CI-Tests gegen gepackte Artefakte — Plugin wird nur für die Session geladen, nicht installiert.
  - Mehrere Plugins gleichzeitig: Flag wiederholen oder Space-getrennte URLs als ein Argument.
  - Nur vertrauenswürdige URLs verwenden — Plugins können beliebigen Code ausführen.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** 2026-04-02

---

## Marketplace erstellen und verwalten

- **Was:** Ein Marketplace ist eine `marketplace.json` in `.claude-plugin/marketplace.json` des Repos. Unterstützte Quellen: GitHub (`owner/repo`), Git-URLs, lokale Pfade, Remote-URLs.
- **marketplace.json Minimal-Struktur:**
  ```json
  {
    "name": "my-team-marketplace",
    "plugins": [
      { "name": "deployment-tools", "source": { "github": "myorg/deployment-plugin" } }
    ]
  }
  ```
- **Best Practice:**
  - **Private Repos** für interne Team-Marketplaces nutzen — Claude Code unterstützt SSH/HTTPS-Auth für Git-Quellen.
  - **Team-Marketplace in Projekt einbetten:** `extraKnownMarketplaces` in `.claude/settings.json` — Claude Code fragt Nutzer beim Vertrauen des Ordners automatisch zur Installation.
  - **Auto-Update** per `"autoUpdate": true` in `extraKnownMarketplaces` für org-weite automatische Updates.
  - Offizielle Anthropic-Marketplaces (`claude-plugins-official`) haben Auto-Update standardmäßig aktiv.
- **Quelle:** https://code.claude.com/docs/en/plugin-marketplaces (offiziell)
- **Stand:** 2026-04-02

---

## Validierung vor dem Veröffentlichen

- **Was:** `claude plugin validate ./my-plugin` führt die gleiche Prüfung wie die Submission-Pipeline aus.
- **Best Practice:**
  - **Immer vor dem Einreichen** validieren.
  - `--strict`-Flag in CI nutzen: behandelt Warnings (z.B. Tippfehler in Feldnamen) als Errors.
    ```bash
    claude plugin validate ./my-plugin --strict
    ```
  - Unbekannte Felder = Warning (Plugin lädt trotzdem). Falscher Typ = Error (Plugin lädt nicht).
  - Tippfehler (ein/zwei Zeichen von bekanntem Feld entfernt) werden als Hinweis in der Warning-Meldung vorgeschlagen.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## Community-Marketplace und Submission-Prozess

- **Was:** Zwei öffentliche Anthropic-Marketplaces:
  - `claude-plugins-official`: Kuratiert von Anthropic, kein Bewerbungsprozess, Anthropic entscheidet selbst über Aufnahme.
  - `claude-community` (`anthropics/claude-plugins-community`): Drittanbieter nach automatisierter Validierung + Safety-Screening. Jedes Plugin auf spezifischen Commit-SHA gepinnt. CI bumpt SHA automatisch.
- **Submission:** Über `claude.ai/settings/plugins/submit` oder `platform.claude.com/plugins/submit`.
- **Best Practice:**
  - `claude plugin validate` lokal ZUERST — die Review-Pipeline läuft dieselbe Prüfung.
  - Nach Genehmigung bis zu 24h warten (nächtlicher Sync) bis Plugin in `marketplace.json` erscheint. Prüfung: suche in `anthropics/claude-plugins-community/blob/main/.claude-plugin/marketplace.json`.
  - Community-Marketplace manuell hinzufügen: `/plugin marketplace add anthropics/claude-plugins-community`
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** 2026-04-02

---

## Sicherheit

- **Was:** Plugins können beliebigen Code mit vollen Nutzerrechten ausführen. Anthropic prüft nicht den Inhalt von MCP-Servern oder anderen Dateien in Community-Plugins.
- **Best Practice:**
  - Nur Plugins aus vertrauenswürdigen Quellen installieren.
  - Plugin-Quellcode vor Installation prüfen (GitHub-Repo, Commit-History, Stars/Forks).
  - Administratoren können erlaubte Marketplaces per **Managed Marketplace Restrictions** einschränken (verhindert unbefugte Marketplace-Ergänzungen durch Nutzer).
  - Plugins werden in `~/.claude/plugins/cache` kopiert — keine externen Pfad-Referenzen möglich.
  - Neu ab v2.1.145: **"Will install"**-Sektion im Plugin-Details-Dialog zeigt vor Installation exakt was hinzugefügt wird (Commands, Agents, Skills, Hooks, MCP/LSP-Server).
- **Quelle:** https://code.claude.com/docs/en/discover-plugins (offiziell)
- **Stand:** 2026-04-02

---

## Entwicklungs-Workflow (Kurzfassung)

1. **Entwickeln:** In `.claude/` als Standalone starten, mit `--plugin-dir ./my-plugin` testen.
2. **Iterieren:** `/reload-plugins` statt Neustart nach Änderungen nutzen.
3. **Validieren:** `claude plugin validate ./my-plugin --strict` vor jedem Commit/Push.
4. **Versionieren:** Semantische Versionen (`version`-Feld) für kontrollierte Updates im Marketplace.
5. **Teilen intern:** Marketplace-Repo erstellen, `extraKnownMarketplaces` in `.claude/settings.json` des Projekts.
6. **Veröffentlichen:** `claude plugin validate` → Submission-Formular → Community-Marketplace.

- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** 2026-04-02

---

## Wichtige CLI-Befehle (Referenz)

```bash
# Plugin lokal testen
claude --plugin-dir ./my-plugin
claude --plugin-dir ./my-plugin.zip            # ZIP ab v2.1.128
claude --plugin-url https://example.com/p.zip  # URL-Loading
claude --plugin-dir ./p1 --plugin-dir ./p2     # Mehrere Plugins

# In-Session (Slash-Befehle)
/plugin                                         # Plugin-Manager öffnen
/plugin install name@marketplace                # Installieren
/plugin disable name@marketplace                # Deaktivieren
/plugin enable name@marketplace                 # Reaktivieren
/plugin uninstall name@marketplace              # Entfernen
/plugin marketplace add anthropics/claude-code  # Marketplace hinzufügen
/plugin marketplace add https://gitlab.com/org/plugins.git#v1.0.0
/plugin marketplace update marketplace-name     # Katalog aktualisieren
/plugin marketplace list                        # Alle Marketplaces zeigen
/plugin marketplace remove marketplace-name
/reload-plugins                                 # Plugins neu laden ohne Neustart

# CLI (außerhalb der Session)
claude plugin validate ./my-plugin              # Validierung
claude plugin validate ./my-plugin --strict     # Warnings als Errors (CI)
claude plugin install formatter@org --scope project
claude plugin uninstall formatter@org --scope project

# Debugging
rm -rf ~/.claude/plugins/cache                  # Cache leeren (dann neu installieren)
```

- **Quelle:** https://code.claude.com/docs/en/discover-plugins (offiziell), https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02

---

## Neue Features nach Version (Referenz)

| Version | Feature |
|---------|---------|
| v2.1.105+ | Background Monitors in Plugins |
| v2.1.128+ | ZIP-Archiv als `--plugin-dir` Argument |
| v2.1.143+ | `displayName`-Feld in plugin.json; Context-Cost-Anzeige im Plugin-Detail |
| v2.1.144+ | "Last updated"-Datum im Plugin-Detail |
| v2.1.145+ | "Will install"-Sektion zeigt alle Komponenten vor Installation |

- **Quelle:** https://code.claude.com/docs/en/discover-plugins (offiziell)
- **Stand:** 2026-04-02
