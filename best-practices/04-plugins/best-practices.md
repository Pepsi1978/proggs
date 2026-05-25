# Plugins — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

## Überblick: Was sind Plugins?

- **Was:** Plugins sind eigenständige Verzeichnisse mit `.claude-plugin/plugin.json` Manifest, die Skills, Agents, Hooks, MCP-Server, LSP-Server und Background-Monitore bündeln und über Marketplaces verteilt werden können.
- **Best Practice:** Standalone-Konfiguration (`.claude/`) für Projekt-spezifisches, Plugins für alles was geteilt wird (Team/Community). Namespacing verhindert Konflikte: `/plugin-name:skill-name`.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

---

## Plugin-Manifest: plugin.json

- **Was:** Die Datei `.claude-plugin/plugin.json` im Plugin-Stammverzeichnis definiert Identität, Version und Metadaten.
- **Best Practice:** Vollständiges Schema (alle optionalen Felder nutzen):

```json
{
  "name": "mein-plugin",
  "description": "Kurze Beschreibung für den Plugin-Manager",
  "version": "1.2.0",
  "author": { "name": "Frank" },
  "homepage": "https://github.com/user/mein-plugin",
  "repository": "https://github.com/user/mein-plugin",
  "license": "MIT"
}
```

  - `name`: Eindeutiger Identifier + Skill-Namespace (Skills heißen `/mein-plugin:skill-name`)
  - `version`: Optional. Wenn gesetzt, bekommen Nutzer Updates nur beim Bump. Wenn fehlt, wird der Git-Commit-SHA als Version genutzt.
  - Skills/Agents/Hooks liegen NICHT in `.claude-plugin/` — nur `plugin.json` kommt rein!

- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

---

## Plugin-Verzeichnisstruktur

- **Was:** Alle Komponenten liegen im Plugin-Stammverzeichnis, niemals in `.claude-plugin/`.
- **Best Practice:** Vollständige Struktur:

```
mein-plugin/
├── .claude-plugin/
│   └── plugin.json          # NUR plugin.json hier!
├── skills/                  # Skills als <name>/SKILL.md Verzeichnisse
│   └── code-review/
│       └── SKILL.md
├── commands/                # Veraltet — stattdessen skills/ nutzen
├── agents/                  # Custom-Agent-Definitionen (Markdown)
├── hooks/
│   └── hooks.json           # Event-Handler-Konfiguration
├── .mcp.json                # MCP-Server-Konfiguration
├── .lsp.json                # LSP-Server-Konfiguration
├── monitors/
│   └── monitors.json        # Background-Monitore
├── bin/                     # Executables im PATH während Plugin aktiv
├── settings.json            # Standard-Settings beim Plugin-Aktivieren
└── README.md
```

- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

---

## Plugin-Komponenten

### Skills

- **Was:** Skills sind Verzeichnisse mit `SKILL.md` in `skills/`. Der Ordnername = Skill-Name.
- **Best Practice:** Immer `description`-Frontmatter schreiben — Claude nutzt es zur automatischen Tool-Auswahl. `$ARGUMENTS` für dynamische Eingaben nutzen.

```markdown
---
description: Überprüft Code auf Best Practices und Sicherheits-Probleme. Nutzen wenn Code reviewt, PRs geprüft oder Code-Qualität analysiert werden soll.
---
...
```

- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

### Agents

- **Was:** `agents/`-Verzeichnis mit Markdown-Dateien definiert Custom Sub-Agents.
- **Best Practice:** Mit `settings.json` im Plugin-Root kann ein Agent als Standard-Agent aktiviert werden: `{ "agent": "security-reviewer" }`. Nur `agent` und `subagentStatusLine` werden in `settings.json` unterstützt.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

### Background-Monitore

- **Was:** `monitors/monitors.json` definiert Hintergrund-Prozesse die Logs/Dateien/externe Status beobachten und Claude live benachrichtigen.
- **Best Practice:** Jede stdout-Zeile des `command` wird als Notification an Claude geliefert. Für Live-Log-Monitoring sehr praktisch.

```json
[{ "name": "error-log", "command": "tail -F ./logs/error.log", "description": "App error log" }]
```

- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

### MCP-Server in Plugins

- **Was:** `.mcp.json` im Plugin-Root oder `mcpServers`-Objekt direkt in `plugin.json` bundelt MCP-Server mit dem Plugin.
- **Best Practice:** `${CLAUDE_PLUGIN_ROOT}` für Plugin-relative Pfade, `${CLAUDE_PLUGIN_DATA}` für persistente Daten (überleben Plugin-Updates), `${CLAUDE_PROJECT_DIR}` für Projekt-Root nutzen. Nach Plugin-Enable/Disable: `/reload-plugins` ausführen.

```json
{
  "mcpServers": {
    "db-tools": {
      "command": "${CLAUDE_PLUGIN_ROOT}/servers/db-server",
      "env": { "DB_URL": "${DB_URL}" }
    }
  }
}
```

- **Quelle:** https://code.claude.com/docs/en/mcp (offiziell)
- **Stand:** Mai 2026

---

## Marketplace-System

### Marketplace-Datei erstellen

- **Was:** `marketplace.json` in `.claude-plugin/` eines Repository definiert eine Liste verteilbarer Plugins.
- **Best Practice:** SHA-Pinning für Produktionsumgebungen verwenden (`sha` statt `ref`), um reproduzierbare Installs zu garantieren:

```json
{
  "name": "Mein Team-Marketplace",
  "plugins": [
    {
      "name": "code-review-tools",
      "source": "https://github.com/user/plugin-repo",
      "sha": "abc123def456..."
    }
  ]
}
```

- **Quelle:** https://code.claude.com/docs/en/plugin-marketplaces (offiziell)
- **Stand:** Mai 2026

### Strict Mode

- **Was:** `strict: true` (Standard) = Plugin hat eigene `plugin.json` und verwaltet eigene Komponenten. `strict: false` = Marketplace-Betreiber hat volle Kontrolle über exponierte Dateien.
- **Best Practice:** `strict: true` für eigene Plugins. `strict: false` nur wenn Marketplace externe Plugins kuratiert/restrukturiert.
- **Quelle:** https://code.claude.com/docs/en/plugin-marketplaces (offiziell, extern erwähnt)
- **Stand:** Mai 2026

### Offizielle Marketplaces (Stand Mai 2026)

- **`claude-plugins-official`:** Standardmäßig in jedem Claude Code enthalten. 101 Plugins (Stand März 2026): 33 Anthropic-gebaut, 68 Partner-Plugins (GitHub, Playwright, Supabase, Figma, Vercel, Linear, Sentry, Stripe, Firebase, etc.)
- **`claude-community`:** Öffentlicher Community-Marketplace. Hinzufügen mit `/plugin marketplace add anthropics/claude-plugins-community`.
- **Einreichung:** Über https://claude.ai/settings/plugins/submit oder https://platform.claude.com/plugins/submit. Vorher `claude plugin validate` lokal ausführen.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

---

## Plugin-Entwicklungs-Workflow

### Lokal testen

- **Was:** `--plugin-dir` Flag lädt Plugin ohne Installation.
- **Best Practice:**

```bash
# Einzelnes Plugin
claude --plugin-dir ./mein-plugin

# Mehrere Plugins
claude --plugin-dir ./plugin-eins --plugin-dir ./plugin-zwei

# ZIP-Archiv (ab v2.1.128)
claude --plugin-dir ./mein-plugin.zip

# Remote ZIP-URL testen
claude --plugin-url https://ci.example.com/build/plugin.zip
```

  Änderungen während der Session: `/reload-plugins` — kein Neustart nötig.

- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

### Validierung vor Einreichung

```bash
claude plugin validate
```

- **Best Practice:** Immer vor Community-Einreichung und vor jedem Release ausführen. Ab v2.1.145 markiert es auch `skills:`-Einträge die auf Dateien statt Verzeichnisse zeigen.
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** Mai 2026

### Plugin-Dependencies (ab v2.1.143)

- **Was:** Plugins können von anderen Plugins abhängen. `claude plugin disable` weigert sich wenn abhängige Plugins aktiv sind.
- **Best Practice:** Dependencies im Plugin-Manifest deklarieren, um versehentliches Deaktivieren zu verhindern.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** Mai 2026

---

## Sicherheit & Trust-Modell

- **Was:** Plugins können MCP-Server, Skripte, Hooks und andere Software enthalten. Das Anthropic-Directory bedeutet nicht automatisch volles Vertrauen.
- **Best Practice:**
  - Plugin-Homepage und README vor Installation lesen
  - Team-Plugins über `.claude-plugin/marketplace.json` im eigenen Repo allowlisten
  - Neue Plugins per PR mit Security-Review einführen (wie reguläre Dependencies behandeln)
  - SHA-Pinning für Produktionsumgebungen — verhindert "works on my machine"-Drift
- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell) + extern bestätigt
- **Stand:** Mai 2026

---

## Plugin-Usage-Tracking (ab v2.1.149)

- **Was:** `/usage` zeigt jetzt Kostenaufschlüsselung pro Plugin und pro Skill.
- **Best Practice:** Teuren Plugins/Skills identifizieren und `alwaysLoad: true` (in MCP-Server-Config) nur für wirklich immer benötigte Server setzen.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** Mai 2026

---

## Bekannte Fallstricke

| Problem | Ursache | Fix |
|---------|---------|-----|
| Skills erscheinen nicht | `skills/` versehentlich in `.claude-plugin/` gelegt | Verzeichnisse im Plugin-Root, NUR `plugin.json` in `.claude-plugin/` |
| Plugin-MCP startet nicht | Server nach Plugin-Enable nicht neugeladen | `/reload-plugins` ausführen |
| Skill-Name-Konflikt | Zwei Plugins mit gleichem Skill-Namen | Plugin-Namen in `plugin.json` unterscheiden → Namespace-Prefix ändert sich |
| Version-Drift | Keine SHA-Pinning im Marketplace | `sha`-Feld statt `ref` in `marketplace.json` nutzen |

---

## Changelog-Zeilen (Plugins)

```
v2.1.149 (22.05.2026) — Plugins: /usage zeigt Kosten pro Plugin/Skill/MCP-Server
v2.1.147 (21.05.2026) — Plugins: Plugin-Agent mit mehrfachen Agent()-Deklarationen gefixt
v2.1.145 (19.05.2026) — Plugins: /plugin Browse zeigt Commands/Agents/Skills/Hooks/MCP/LSP; validate markiert skills:-Datei-Fehler
v2.1.144 (19.05.2026) — Plugins: Browse zeigt Update-Datum; Komponentenzählung nicht mehr verdoppelt
v2.1.143 (15.05.2026) — Plugins: Dependency-Enforcement; --plugin-dir akzeptiert .zip; Browse zeigt Token-Kosten
v2.1.128 (früher)    — Plugins: --plugin-dir akzeptiert ZIP-Archive
```
