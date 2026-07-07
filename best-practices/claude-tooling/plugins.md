# Plugins — Best Practices (Stand 2026-06-05, Claude Code 2.1.165)

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | externer Plugin-/MCP-Code | vor Installation komplett lesen + scannen (Prompt-Injection) | Sicherheit |
| 2 | plugin.json | vollstaendiges Manifest-Schema einhalten; validieren vor Veroeffentlichung | Manifest-Struktur |
| 3 | Versionen | SHA-Pinning fuer Reproduzierbarkeit; `claude plugin update` zieht ggf. stale Clone | Versions-Management |
| 4 | command-Hooks im Plugin | Pre/PostToolUse werden gedroppt → in user-`settings.json` definieren | Komponenten |
| 5 | Auslieferung | `defaultEnabled:false` fuer opt-in (v2.1.154) | defaultEnabled |
| 6 | `.claude/skills` | auto-geladen ohne Marketplace (v2.1.157) | Auto-Loading |
| 7 | .sh-Hooks im Plugin | verlieren `+x` bei Sync → `git update-index --chmod=+x` | Sicherheit |

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
    "defaultEnabled": false,         // NEU ab v2.1.154: Plugin deaktiviert installieren

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
- **Best Practice:** `$schema`-Feld einsetzen für Editor-Autocomplete. `displayName` (v2.1.143+) für saubere UI-Anzeige nutzen. `defaultEnabled: false` (v2.1.154+) für Plugins nutzen, die Kosten verursachen oder sich auf externe Dienste verbinden.
- **Wichtig:** Unbekannte Top-Level-Felder werden **ignoriert** (kein Fehler), falsche Typen sind Load-Fehler. Manifest kann parallel als npm `package.json` oder VS-Code-Extension-Manifest dienen.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02 (aktualisiert 2026-05-30)

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

## NEU v2.1.157: .claude/skills Auto-Loading — kein Marketplace nötig

- **Was:** Jeder Ordner unter einem skills-Verzeichnis, der eine `.claude-plugin/plugin.json` enthält, wird beim nächsten Session-Start **automatisch als Plugin** geladen — ohne Marketplace und ohne Install-Schritt. Der Plugin-Name lautet `<name>@skills-dir`.
- **Zwei unterstützte Scopes:**

  | Skills-Verzeichnis        | Scope    | Wann geladen                                                               |
  |---------------------------|----------|----------------------------------------------------------------------------|
  | `~/.claude/skills/`       | personal | In jedem Projekt, da der Pfad persönlich ist                               |
  | `<cwd>/.claude/skills/`   | project  | Nur nach Workspace-Trust-Dialog für diesen Ordner                          |

- **Unterschied zu Marketplace-Install:** Plugin wird **in-place geladen** (nicht in Plugin-Cache kopiert). Änderungen an `SKILL.md` greifen sofort; Änderungen an `hooks/`, `.mcp.json`, `agents/` etc. erst nach `/reload-plugins` oder Neustart.
- **Einschränkungen bei project-scope `@skills-dir`:**
  - MCP-Server brauchen Pro-Server-Genehmigung (wie bei project `.mcp.json`)
  - LSP-Server starten erst nach Workspace-Trust
  - Background Monitors laden **nicht**
  - Kein Walk-up zum Repo-Root — immer vom Repo-Root starten oder `/reload-plugins` nach `cd`
- **Best Practice:**
  - Personal-scope (`~/.claude/skills/`) für persönliche Helfer nutzen — keine Einschränkungen.
  - Project-scope (`.claude/skills/`) für Team-Plugins im Repo nutzen — per Git geteilt.
  - Plugin stoppen: Verzeichnis löschen oder `claude plugin disable my-tool@skills-dir`.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference#skills-directory-plugins (offiziell)
- **Stand:** 2026-05-29

---

## NEU v2.1.157: `claude plugin init` — Plugin scaffolden

- **Was:** Neuer CLI-Befehl, der ein Plugin-Gerüst in `~/.claude/skills/<name>/` anlegt. Beim nächsten Session-Start wird es automatisch als `<name>@skills-dir` geladen.
- **Syntax:**
  ```bash
  claude plugin init <name> [options]

  # Minimal (nur plugin.json + SKILL.md)
  claude plugin init my-helper

  # Mit extras (kombinierbar)
  claude plugin init my-helper --include skills
  claude plugin init my-helper --include agents
  claude plugin init my-helper --include hooks
  claude plugin init my-helper --include mcp
  claude plugin init my-helper --include lsp
  claude plugin init my-helper --include output-style
  claude plugin init my-helper --include channel  # MCP-basierter Channel
  ```
- **Was wird angelegt:** `.claude-plugin/plugin.json` + starter `SKILL.md` + optional gewählte Komponenten-Verzeichnisse.
- **Best Practice:**
  - Für schnelle persönliche Helfer: `claude plugin init my-helper` (minimal, kein `--include` nötig).
  - Für Plugins mit Hooks oder Agents: passende `--include`-Flags gleich beim Init mitgeben.
  - `<name>` darf keine Leerzeichen oder Pfad-Separatoren enthalten (wird Skill-Namespace und Verzeichnisname).
  - Danach **keine neuen Fenster öffnen nötig** — Plugin lädt beim nächsten Session-Start automatisch.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference#plugin-init (offiziell)
- **Stand:** 2026-05-29

---

## NEU v2.1.154: `defaultEnabled: false` — Plugin deaktiviert ausliefern

- **Was:** `defaultEnabled: false` in `plugin.json` lässt ein Plugin im installierten, aber deaktivierten Zustand. Der Nutzer aktiviert es manuell mit `claude plugin enable <plugin>` oder über `/plugin`.
- **Wann nutzen:** Plugins die Kosten verursachen, sich mit externen Diensten verbinden oder einen expliziten Opt-in rechtfertigen.
- **Precedence-Regeln:**
  1. **Nutzer-Setting** (`enabledPlugins` in settings): überschreibt `defaultEnabled` dauerhaft — auch über Plugin-Updates hinweg.
  2. **Dependency-Requirement**: wenn Plugin als Abhängigkeit eines aktiven Plugins benötigt wird, setzt Claude Code automatisch `true` und ignoriert `defaultEnabled`.
  3. **`defaultEnabled`**: greift nur, wenn keiner der beiden oben etwas gesetzt hat.
- **Marketplace-Eintrag**: `defaultEnabled` kann auch im Marketplace-Eintrag stehen — das hat Vorrang vor `plugin.json`.
- **Rückwärtskompatibilität:** Claude Code vor v2.1.154 ignoriert das Feld und aktiviert das Plugin beim Install.
- **Best Practice:**
  - Für Opt-in-Plugins (externe Dienste, teure Operationen): `"defaultEnabled": false` in `plugin.json`.
  - Für Standard-Helfer die immer nützlich sind: Feld weglassen (Default ist `true`).
  - **Nie** `defaultEnabled: false` für Abhängigkeits-Plugins setzen — Claude Code setzt das automatisch auf `true`, wenn der Plugin als Dependency aktiv wird.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference#default-enablement (offiziell)
- **Stand:** 2026-05-28

---

## NEU v2.1.157: `/plugin` Autocomplete

- **Was:** Der `/plugin`-Befehl hat jetzt Autocomplete für: Subcommands, installierte Plugin-Namen, und Plugins aus bekannten Marketplaces.
- **Best Practice:** Keine Aktion nötig — greift automatisch beim Tippen von `/plugin`. Nützlich um verfügbare Marketplace-Plugins zu entdecken ohne den Discover-Tab zu öffnen.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026-05-29

---

## NEU v2.1.157: `/plugin` Discover Tab — "Suggested for this directory"

- **Was:** Der Discover-Tab im `/plugin`-Manager pinnt Plugins oben an, deren Relevanz-Signale zum aktuellen Verzeichnis passen (z.B. ein Android-Plugin wenn `build.gradle.kts` vorhanden ist). Diese Plugins werden mit "suggested for this directory" annotiert.
- **Best Practice:** Beim Starten in einem neuen Projekttyp kurz `/plugin` öffnen — suggested Plugins sind oft nützliche LSP-Server oder Sprach-Spezifische Tools.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026-05-29

---

## NEU v2.1.153: `skipLfs` in Marketplace-Quellen

- **Was:** `github`- und `git`-Quellen in `marketplace.json` unterstützen jetzt ein `skipLfs: true`-Feld, das Git LFS-Downloads beim Klonen und Aktualisieren überspringt.
- **Wann nutzen:** Wenn ein Plugin-Repo große Binärdateien in Git LFS hat (z.B. Modelle, Assets), die für den Plugin-Betrieb nicht gebraucht werden.
- **Beispiel:**
  ```json
  {
    "plugins": [
      {
        "name": "my-plugin",
        "source": {
          "github": "myorg/my-plugin",
          "skipLfs": true
        }
      }
    ]
  }
  ```
- **Quelle:** https://github.com/anthropics/claude-code/releases/tag/v2.1.153 (offiziell)
- **Stand:** 2026-05-28

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
- **Achtung:** Monitors laden **nicht** bei project-scope `@skills-dir` Plugins.
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
- **NEU ab v2.1.154:** Abhängigkeits-Plugins werden automatisch aktiviert wenn ein abhängiges Plugin aktiviert wird — `defaultEnabled: false` greift dann nicht.
- **Quelle:** https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02 (aktualisiert 2026-05-30)

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
  - **NEU ab v2.1.153:** `skipLfs: true` in `github`/`git`-Quellen nutzen wenn das Plugin-Repo große LFS-Dateien hat die nicht gebraucht werden.
- **Quelle:** https://code.claude.com/docs/en/plugin-marketplaces (offiziell)
- **Stand:** 2026-04-02 (aktualisiert 2026-05-30)

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

### Workflow A: Plugin per `claude plugin init` (NEU v2.1.157 — empfohlen für persönliche Plugins)

1. `claude plugin init my-helper` → legt `~/.claude/skills/my-helper/` an
2. `SKILL.md` bearbeiten
3. Nächste Session starten → Plugin lädt automatisch als `my-helper@skills-dir`
4. Änderungen an SKILL.md greifen **sofort**; andere Komponenten nach `/reload-plugins`
5. Plugin stoppen: `claude plugin disable my-helper@skills-dir`

### Workflow B: Plugin mit `--plugin-dir` (für geteilte/Marketplace-Plugins)

1. **Entwickeln:** In `.claude/` als Standalone starten, mit `--plugin-dir ./my-plugin` testen.
2. **Iterieren:** `/reload-plugins` statt Neustart nach Änderungen nutzen.
3. **Validieren:** `claude plugin validate ./my-plugin --strict` vor jedem Commit/Push.
4. **Versionieren:** Semantische Versionen (`version`-Feld) für kontrollierte Updates im Marketplace.
5. **Teilen intern:** Marketplace-Repo erstellen, `extraKnownMarketplaces` in `.claude/settings.json` des Projekts.
6. **Veröffentlichen:** `claude plugin validate` → Submission-Formular → Community-Marketplace.

- **Quelle:** https://code.claude.com/docs/en/plugins (offiziell)
- **Stand:** 2026-04-02 (aktualisiert 2026-05-30)

---

## Wichtige CLI-Befehle (Referenz)

```bash
# Plugin scaffolden (NEU v2.1.157)
claude plugin init my-helper                     # Minimal in ~/.claude/skills/my-helper/
claude plugin init my-helper --include hooks     # Mit Hooks
claude plugin init my-helper --include agents    # Mit Agents
claude plugin init my-helper --include mcp       # Mit MCP-Server

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
claude plugin enable my-tool@skills-dir         # skills-dir Plugin aktivieren
claude plugin disable my-tool@skills-dir        # skills-dir Plugin deaktivieren

# Debugging
rm -rf ~/.claude/plugins/cache                  # Cache leeren (dann neu installieren)
```

- **Quelle:** https://code.claude.com/docs/en/discover-plugins (offiziell), https://code.claude.com/docs/en/plugins-reference (offiziell)
- **Stand:** 2026-04-02 (aktualisiert 2026-05-30)

---

## Neue Features nach Version (Referenz)

| Version | Feature |
|---------|---------|
| v2.1.105+ | Background Monitors in Plugins |
| v2.1.128+ | ZIP-Archiv als `--plugin-dir` Argument |
| v2.1.143+ | `displayName`-Feld in plugin.json; Context-Cost-Anzeige im Plugin-Detail |
| v2.1.144+ | "Last updated"-Datum im Plugin-Detail |
| v2.1.145+ | "Will install"-Sektion zeigt alle Komponenten vor Installation |
| v2.1.153+ | `skipLfs: true` in `github`/`git` Marketplace-Quellen |
| v2.1.154+ | `defaultEnabled: false` in plugin.json; Dependency-Auto-Enable |
| v2.1.157+ | `.claude/skills/`-Verzeichnis Auto-Loading ohne Marketplace |
| v2.1.157+ | `claude plugin init <name>` Scaffolding-Befehl |
| v2.1.157+ | `/plugin` Autocomplete (Subcommands, Plugin-Namen, Marketplace-Plugins) |
| v2.1.157+ | `/plugin` Discover Tab: "suggested for this directory" Kontextannotation |

- **Quelle:** https://code.claude.com/docs/en/discover-plugins (offiziell), https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026-05-30

---

<!-- CHECKPOINT: fertig — alle v2.1.153 bis v2.1.158 Plugin-Features dokumentiert. Nächste Recherche: v2.1.159+ oder Kategorie 5 (Hooks/Events). -->

---

### Update 2026-06-05 (Claude Code 2.1.165) — Plugins

**`/plugin list` mit `--enabled`/`--disabled`-Filtern (2.1.163)**
- **Was:** Neuer Built-in-Command listet installierte Plugins; `--enabled`/`--disabled` filtern.
- **Best Practice:** Plugin-Hygiene — `/plugin list --disabled` offenbart "Zombie-Plugins" (installiert, nie aktiviert), die beim Start Token kosten und die Prompt-Injection-Angriffsflaeche erhoehen. Bei unbekanntem Projekt `/plugin list --enabled` als Security-Check. Eignet sich als Basis fuer einen SessionStart-Diagnose-Hook (warnt, wenn ein Pflicht-Plugin aus ist).
- **Quelle:** code.claude.com/docs/en/plugins-reference `[offiziell]`

**Betrifft eigene Werkzeuge:** Wenn `bug-almanac-guard`, `session-guard` o.a. schweigen — zuerst `/plugin list --enabled` pruefen, ob deren Plugin ueberhaupt aktiv ist (deckt sich mit dem Feedback "Hook vorhanden != aktiv").
