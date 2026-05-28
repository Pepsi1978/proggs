# Settings & Konfiguration — Best Practices (Stand 2026-05-28, Claude Code 2.1.153)

---

## Konfigurationshierarchie & Scope-System

- **Was:** Claude Code nutzt ein 5-Ebenen-Scope-System mit festgelegter Priorität (höchste zuerst):
  1. **Managed** — Organisations/IT-Richtlinien, nicht überschreibbar, aus `/managed-settings.json` oder Registry/plist
  2. **CLI-Argumente** — Temporäre Session-Überschreibungen per Flag
  3. **Local** — `.claude/settings.local.json` (gitignoriert, nur für diese Maschine)
  4. **Project** — `.claude/settings.json` (per Git geteilt mit dem Team)
  5. **User** — `~/.claude/settings.json` (persönlich, gilt für alle Projekte)
- **Best Practice:** Trenne Sorgen klar nach Ebenen. Persönliche Präferenzen (Modell, Effort, Editor-Modus) in User-Settings. Team-Regeln (Permissions, MCP-Server, Attribution) in Project-Settings. API-Keys und lokale Overrides (bypassPermissions) in Local-Settings. **Niemals** Secrets in Project-Settings, da diese ins Git kommen.
- **Besonderheit Arrays:** Array-Werte wie `permissions.allow`, `permissions.deny` werden über alle Scopes zusammengeführt (nicht überschrieben). Niedrigere Scopes können also Einträge hinzufügen, ohne höhere zu löschen.
- **Hot-Reload:** Die meisten Keys (permissions, hooks, env, Credential-Helper) werden ohne Neustart neu geladen. Ausnahmen, die einen Neustart erfordern: `model`, `outputStyle` (da Teil des System-Prompts).
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-25

---

## Permission-Modes & Sicherheitskonfiguration

- **Was:** Das `permissions`-Objekt kontrolliert, welche Tool-Aufrufe automatisch erlaubt, blockiert oder nachgefragt werden. Kern-Keys: `allow`, `deny`, `ask`, `defaultMode`, `additionalDirectories`.
- **defaultMode-Werte:**
  - `"default"` — Bei jedem Tool-Aufruf nachfragen
  - `"acceptEdits"` — Datei-Edits automatisch genehmigen, Bash-Befehle nachfragen
  - `"plan"` — Plan-Übersicht vor Ausführung
  - `"bypassPermissions"` — Alle Checks überspringen (nur für erfahrene Solo-User in `.local.json`)
- **Permission-Regel-Syntax:**
  ```
  Bash(npm run *)            # Alle npm-run-Befehle
  Read(./.env)               # Genau diese Datei
  WebFetch(domain:github.com) # Nur diese Domain
  Edit(./src/**)             # Schreibzugriff auf src/
  ```
- **Auswertungsreihenfolge:** Deny wird zuerst geprüft, dann Ask, dann Allow — erster Treffer gewinnt.
- **Best Practice Sicherheit:** In Project-Settings `deny` für `.env*`, `secrets/**`, sensible Credentials setzen. `allow` für alle bekannten Build/Test-Befehle. `bypassPermissions` ausschließlich in `.claude/settings.local.json` (gitignoriert), nie in geteilten Settings.
- **Enterprise-Locking:** `disableBypassPermissionsMode: "disable"` verhindert, dass User-Level bypassPermissions aktiviert. `allowManagedPermissionRulesOnly: true` lässt nur Managed-Regeln zu.
- **ACHTUNG allow-Liste:** Eine nicht-leere `allow`-Liste in `permissions` wirkt als Whitelist-Blocker — alle nicht aufgelisteten Tools werden implizit blockiert, auch bei `bypassPermissions`. Wenn `bypassPermissions` aktiv ist, darf `allow` NICHT gesetzt sein (leere oder fehlende Liste).
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-25

---

## effortLevel — Reasoning-Tiefe steuern

- **Was:** `effortLevel` steuert wie viel erweitertes Denken (Extended Thinking) Claude vor einer Antwort investiert. Werte: `"low"`, `"medium"`, `"high"` (Standard), `"xhigh"`.
- **Verhalten:**
  - `"low"` — Schneller, günstiger, weniger Reasoning
  - `"medium"` — Ausgewogen
  - `"high"` — Standardwert, tiefes Reasoning
  - `"xhigh"` — Maximales Reasoning (entspricht alwaysThinkingEnabled)
- **Ergänzende Settings:**
  - `"alwaysThinkingEnabled": true` — Thinking immer aktiv (Äquivalent zu `xhigh`)
  - `"showThinkingSummaries": true` — Denkschritte im Terminal anzeigen
- **KRITISCH — NIEMALS per Env-Variable steuern:** `CLAUDE_CODE_EFFORT_LEVEL` als Umgebungsvariable blockiert `/effort`-Änderungen während der Session. Nur über `effortLevel` in settings.json oder den `/effort`-Befehl.
- **Session-Override:** `/effort low|medium|high|xhigh` während der Session ändert den Wert nur bis Session-Ende. Beim nächsten echten Start gilt wieder der settings.json-Wert.
- **Bekannter Bug (Issue #45453):** `effortLevel` in settings.json wird manchmal beim Start nicht angewendet. Workaround: SessionStart-Hook der den Wert explizit setzt (nur bei `source=startup` oder `source=clear`, nicht bei `source=compact` oder `source=resume`).
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell), https://github.com/anthropics/claude-code/issues/45453 (offiziell, Bug-Tracker)
- **Stand:** 2026-05-25

---

## Umgebungsvariablen (env-Key & externe Env-Vars)

- **Was:** Im `env`-Objekt in settings.json können beliebige Key-Value-Paare definiert werden, die Claude Code beim Start in die Session-Umgebung injiziert. Jedes Bash-Tool-Call und jedes Hook-Script kann diese Variablen lesen.
- **Syntax:**
  ```json
  {
    "env": {
      "MY_TOKEN": "abc",
      "CLAUDE_CODE_ENABLE_TELEMETRY": "1",
      "OTEL_METRICS_EXPORTER": "otlp"
    }
  }
  ```
- **Wichtige vordefinierte Env-Vars:**
  - `CLAUDE_CODE_ENABLE_TELEMETRY` — Telemetrie ein/aus (Standard: 1)
  - `ANTHROPIC_MODEL` — Standard-Modell überschreiben
  - `DISABLE_AUTOUPDATER` — Automatische Updates deaktivieren
  - `CLAUDE_CODE_DISABLE_AUTO_MEMORY` — Auto-Memory deaktivieren
  - `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE` — Schwellwert für Auto-Kompaktierung (Math.min-Clamp: kann Wert nur SENKEN, nie erhöhen; 100 = kompaktiert erst bei vollem Kontext; 85 war wegen Clamp wirkungslos)
  - `CLAUDE_CODE_SKIP_PROMPT_HISTORY` — Kein Transkript schreiben
  - `CLAUDE_CODE_NO_FLICKER` — Fullscreen-TUI aktivieren (Äquivalent zu `"tui": "fullscreen"`)
  - `CLAUDE_CODE_USE_POWERSHELL_TOOL` — PowerShell-Tool aktivieren (Windows)
  - `CLAUDE_CODE_DISABLE_THINKING` — Extended Thinking erzwungen deaktivieren
- **WICHTIG (seit v2.1.143):** `NO_COLOR` und `FORCE_COLOR` müssen VOR dem Start von Claude Code gesetzt werden — nicht über den `env`-Key in settings.json.
- **Best Practice:** Team-übergreifende Env-Vars in Project-Settings `.claude/settings.json`. Geheime Tokens nur in `.claude/settings.local.json` oder über `apiKeyHelper`-Script, niemals in geteilten Dateien.
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-25

---

## Statusline — Eigene Status-Leiste konfigurieren

- **Was:** Die Statusline ist eine anpassbare Leiste am unteren Rand von Claude Code. Ein Shell-Script empfängt JSON auf stdin und gibt eine (oder mehrere) Zeile(n) aus, die angezeigt werden.
- **Einrichten in settings.json:**
  ```json
  {
    "statusLine": {
      "type": "command",
      "command": "~/.claude/statusline.sh",
      "padding": 2
    }
  }
  ```
- **Verfügbare JSON-Felder (auf stdin):**
  - `hook_event_name`, `session_id`, `transcript_path`
  - `cwd`, `version`, `output_style`
  - `model` — Objekt mit `id` und `display_name`
  - `workspace` — Objekt mit `current_dir` und `project_dir`
  - `context_window` — Objekt mit `used_percentage` (und weiteren Feldern)
  - `cost` — Objekt mit `total_cost_usd`, `total_duration_ms`, `total_api_duration_ms`, `total_lines_added`, `total_lines_removed`
  - `vim` — Vim-Mode-Status
- **NEU ab v2.1.153 — COLUMNS und LINES Umgebungsvariablen:**
  - Claude Code setzt jetzt `COLUMNS` und `LINES` als Umgebungsvariablen **bevor** dein Script ausgeführt wird.
  - Hintergrund: Da Claude Code die Script-Ausgabe abfängt (statt das Script direkt mit dem Terminal zu verbinden), funktionieren `tput cols` und sprachebenenweite Breiten-Erkennung **nicht** von innen aus dem Script.
  - Lösung: Stattdessen `$COLUMNS` und `$LINES` lesen — diese enthalten die aktuelle Terminal-Größe.
  - **Auswirkung auf vorhandene Statusline-Scripts:** Wer bisher Workarounds für die fehlende Terminal-Breite hatte (z.B. hart codierte Breiten oder externe Mechanismen), kann jetzt auf `$COLUMNS` umsteigen.
  - Beispiel:
    ```bash
    #!/bin/bash
    input=$(cat)
    PCT=$(echo "$input" | jq -r '.context_window.used_percentage // 0' | cut -d. -f1)
    # Trennlinie genau so breit wie das Terminal
    SEPARATOR=$(printf '─%.0s' $(seq 1 $COLUMNS))
    echo "$SEPARATOR"
    echo "${PCT}% context"
    ```
- **HINWEIS:** Der Stop-Hook empfängt `context_window` NICHT (anders als die Statusbar). Für Kontext-Prozent im Stop-Hook: Statusline schreibt pro Session in `~/.claude/state/ctx-<session_id>`, Hook liest dort nach.
- **Refresh:** Aktualisierung bei jeder Nachrichtenänderung, maximal alle 300ms. ANSI-Farbcodes werden unterstützt.
- **Mehrzeilig:** Mehrere Print-Zeilen ergeben mehrere Status-Zeilen (z.B. Git-Info oben, Kontext-Bar unten).
- **Generierung per Command:** `/statusline` eintippen — Claude Code generiert den Script-Code automatisch.
- **refreshInterval:** Optionales Feld — lässt das Script alle N Sekunden zusätzlich zu Event-getriggerten Updates neu laufen. Nützlich für Uhranzeigen oder wenn Hintergrund-Subagenten den Git-Status ändern.
- **hideVimModeIndicator:** `true` unterdrückt den eingebauten `-- INSERT --`-Text falls dein Script `vim.mode` selbst rendert.
- **Quelle:** https://code.claude.com/docs/en/statusline (offiziell), https://cld-docs.onlinetool.cc/en/docs/claude-code/statusline.html (extern)
- **Stand:** 2026-05-28 (COLUMNS/LINES neu in v2.1.153)

---

## /model — Modell-Auswahl & Default-Verhalten (NEU ab v2.1.153)

- **Was:** Das `/model`-Kommando öffnet den Modell-Picker im Terminal. **Ab v2.1.153 geändertes Verhalten:** Die Auswahl wird jetzt als **Default für neue Sessions** gespeichert — identisch zum Verhalten im IDE-Modus.
- **Vorheriges Verhalten (bis v2.1.152):** Modell-Auswahl galt nur für die aktuelle Session.
- **Neues Verhalten (ab v2.1.153):**
  - `Enter` / `d` im Picker → Auswahl wird als **persistenter Default** gespeichert (neue Sessions starten mit diesem Modell).
  - `s` im Picker → Auswahl gilt **nur für die aktuelle Session** (temporär, kein Persistent-Write).
- **Konsequenzen für Workflows:**
  - Wer bisher im `/model`-Picker testweise auf ein anderes Modell gewechselt hat, setzt damit jetzt dauerhaft den Default — Achtung!
  - Für temporäre Experimente immer `s` drücken statt Enter.
  - Für dauerhaften Wechsel weiterhin Enter oder `d` nutzen.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell), https://github.com/anthropics/claude-code/releases (offiziell)
- **Stand:** 2026-05-28

---

## keybindings.json — modelPicker-Umbenennung (MIGRATION erforderlich ab v2.1.153)

- **Was:** Mit dem neuen `/model`-Verhalten (Default vs. Session-only) wurde die Keybinding-Aktion umbenannt.
- **MIGRATION PFLICHT:** Wer `modelPicker:setAsDefault` in `~/.claude/keybindings.json` oder in `~/proggs/claude-code-setup/` angepasst hat, muss umbenennen:
  ```json
  // ALT (bis v2.1.152):
  { "action": "modelPicker:setAsDefault", "key": "d" }

  // NEU (ab v2.1.153):
  { "action": "modelPicker:thisSessionOnly", "key": "s" }
  ```
- **Erklärung:** Die Aktion `setAsDefault` existiert nicht mehr (Default ist jetzt das normale Enter-Verhalten). Die neue Aktion `thisSessionOnly` entspricht dem alten "nur für diese Session"-Modus, jetzt auf `s` gelegt.
- **Prüfen:** `grep -r "modelPicker:setAsDefault" ~/.claude/` — falls nichts gefunden, ist keine Migration nötig.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell), https://code.claude.com/docs/en/keybindings (offiziell)
- **Stand:** 2026-05-28

---

## fallback-model — Automatische Modell-Degradierung (NEU ab v2.1.152)

- **Was:** `--fallback-model` ist ein CLI-Flag (und potenziell Settings-Key) das ein Ersatz-Modell konfiguriert, falls das primäre Modell nicht verfügbar ist.
- **Altes Verhalten (bis v2.1.151):** Wenn das primäre Modell nicht gefunden wird, schlagen **alle** Requests in der Session fehl.
- **Neues Verhalten (ab v2.1.152):** Claude Code wechselt automatisch für den Rest der Session auf das konfigurierte Fallback-Modell — statt jeden Request zu scheitern.
- **Bonus (v2.1.152):** `/bg` und `←-detach` übernehmen `--fallback-model` jetzt — Background-Worker degradieren bei Überlastung ebenfalls auf das Fallback statt hart zu scheitern.
- **Best Practice:**
  ```bash
  # Start mit Fallback konfiguriert:
  claude --model claude-opus-4-5 --fallback-model claude-sonnet-4-5
  ```
  Oder in `.claude/settings.json` über `ANTHROPIC_MODEL` und ein geplantes `fallbackModel`-Feld (noch nicht offiziell in Settings-JSON, primär CLI-Flag).
- **Wann sinnvoll:** Bei hoher API-Last, in CI/CD-Workflows wo Unterbrechungen kritisch sind, und bei Nacht-Jobs die ohne Aufsicht laufen.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell), https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/ (extern)
- **Stand:** 2026-05-28

---

## pluginSuggestionMarketplaces — Enterprise Managed Setting (NEU ab v2.1.152)

- **Was:** Neues Managed Setting `pluginSuggestionMarketplaces` für Admins. Erlaubt das Allowlisten von Organisations-Marketplaces, deren Plugins als kontextbezogene Hinweise vorgeschlagen werden dürfen.
- **Scope:** Nur `managed`-Ebene (nicht in User- oder Project-Settings setzbar).
- **Syntax (managed-settings.json):**
  ```json
  {
    "pluginSuggestionMarketplaces": [
      "https://marketplace.example.com/plugins"
    ]
  }
  ```
- **Zweck:** Admins können steuern, welche Plugin-Quellen in kontextbezogenen Tipps erscheinen — verhindert, dass externe oder inoffizielle Marketplaces in Empfehlungen auftauchen.
- **Best Practice:** In Unternehmensumgebungen explizit nur geprüfte interne Marketplaces listen. Bei Nicht-Konfiguration bleibt das Verhalten wie bisher.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell), https://managed-settings.com/ (extern)
- **Stand:** 2026-05-28

---

## claude update — Release-Channel-Verhalten (Bugfix v2.1.153)

- **Was:** `claude update` installiert jetzt die Version des konfigurierten Release-Channels statt immer `latest` — für npm-Installationen.
- **Problem vorher:** Bei npm-Installationen ignorierte `claude update` den konfigurierten Release-Channel und installierte stets die neueste Version, auch wenn ein Stable- oder Beta-Channel konfiguriert war.
- **Fix (v2.1.153):** Der konfigurierte Channel wird jetzt respektiert.
- **Relevanz:** Betrifft nur npm-Installationen (`npm install -g @anthropic-ai/claude-code`). Standalone-Binary-Installationen waren nicht betroffen.
- **Best Practice:** Release-Channel in Settings konfigurieren wenn Stabilität wichtiger ist als neueste Features:
  ```json
  { "releaseChannel": "stable" }
  ```
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026-05-28

---

## .claude/rules/ mit paths:-Frontmatter

- **Was:** Regeln in `.claude/rules/` können per YAML-Frontmatter auf bestimmte Dateipfade begrenzt werden. Die Regel wird dann nur geladen, wenn Claude an passenden Dateien arbeitet — reduziert Kontext-Rauschen.
- **Syntax:**
  ```yaml
  ---
  paths: src/api/**/*.ts
  ---
  # Hier stehen die API-Regeln
  ```
  Oder mehrere Pfade:
  ```yaml
  ---
  paths:
    - src/components/**/*.tsx
    - src/hooks/**/*.ts
  ---
  ```
- **Brace-Expansion:** `"src/**/*.{ts,tsx}"` und `"{src,lib}/**/*.ts"` werden unterstützt.
- **KRITISCHE BEKANNTE BUGS (Stand 2026-05-25):**
  1. **Write-Tool-Bug (Issue #23478):** Path-basierte Regeln werden nur beim LESEN von passenden Dateien injiziert — nicht beim SCHREIBEN/ERSTELLEN. Wichtige Erstellungsregeln deshalb ohne paths-Frontmatter lassen (immer laden).
  2. **Format-Bug (Issue #17204):** Die dokumentierte YAML-Listen-Syntax mit Anführungszeichen funktioniert in bestimmten Konfigurationen nicht. Die undokumentierte `globs:`-Alternative ist stabiler als `paths:`.
  3. **User-Level-Bug (Issue #21858):** Regeln mit paths-Frontmatter in `~/.claude/rules/` werden still ignoriert — nur Projekt-Level-Regeln (`.claude/rules/`) funktionieren zuverlässig.
- **Workarounds:**
  - Kritische Regeln ohne paths-Frontmatter definieren (immer laden, Kontext-Overhead bewusst akzeptieren)
  - Pfad-Scoping nur in `.claude/rules/` (Projekt-Ebene), nicht in `~/.claude/rules/` (User-Ebene)
  - `globs:` statt `paths:` versuchen falls Probleme auftreten
- **Best Practice:** Regel-Dateien klein und fokussiert halten (eine Sorge pro Datei). Namen beschreibend wählen. Regeln versionieren (sie sind Code). Skeptisch gegenüber User-Level path-Scoping bleiben — Projekt-Level ist zuverlässiger.
- **Quelle:** https://github.com/anthropics/claude-code/issues/21858 (offiziell, Bug-Tracker), https://github.com/anthropics/claude-code/issues/23478 (offiziell, Bug-Tracker), https://claudefa.st/blog/guide/mechanics/rules-directory (extern)
- **Stand:** 2026-05-25

---

## outputStyle & UI-Einstellungen

- **Was:** `outputStyle` ändert den System-Prompt um den Antwort-Stil global anzupassen. Weitere UI-Keys: `viewMode`, `tui`, `editorMode`, `language`.
- **ACHTUNG: outputStyle erfordert Neustart!** Ist Teil des System-Prompts — Änderungen werden nicht per Hot-Reload übernommen.
- **viewMode-Werte:** `"default"`, `"verbose"` (ausführlichere Ausgabe), `"focus"` (reduziert)
- **tui-Wert:** `"fullscreen"` aktiviert flimmerfreies Vollbild-Terminal-UI (Alternative: `CLAUDE_CODE_NO_FLICKER=1` vor dem Start setzen)
- **editorMode:** `"normal"` | `"vim"` — gilt für Inline-Edits im Terminal
- **language:** z.B. `"german"`, `"japanese"`, `"french"` — ändert die Interface-Sprache von Claude Code selbst
- **Spinner anpassen:**
  ```json
  {
    "spinnerVerbs": {
      "mode": "append",
      "verbs": ["Ponderiere", "Grübele"]
    }
  }
  ```
- **Notifications:** `"preferredNotifChannel"` — Werte: `"auto"`, `"terminal_bell"`, `"iterm2"`, `"iterm2_with_bell"`, `"kitty"`, `"ghostty"`, `"notifications_disabled"`
- **Best Practice:** `"tui": "fullscreen"` für stabilere Darstellung in Windows Terminal. `"language"` nicht auf Deutsch setzen wenn die Harness-Regeln und Code-Kommentare auf Englisch sind — Interface-Sprache und Antwortsprache in CLAUDE.md regeln ist präziser.
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-25

---

## Sandbox-Konfiguration (Datei- und Netzwerk-Isolation)

- **Was:** `sandbox`-Objekt in settings.json steuert Filesystem- und Netzwerk-Isolation für alle Claude-Code-Ausführungen.
- **Syntax (kompakt):**
  ```json
  {
    "sandbox": {
      "enabled": true,
      "failIfUnavailable": true,
      "filesystem": {
        "allowWrite": ["/tmp/build"],
        "denyRead": ["~/.aws/credentials"]
      },
      "network": {
        "allowedDomains": ["github.com", "*.npmjs.org"]
      }
    }
  }
  ```
- **Best Practice:** In CI/CD und Team-Umgebungen `sandbox.enabled: true` + restriktive Domain-Whitelist. `failIfUnavailable: true` stellt sicher, dass ohne funktionierende Sandbox gar nicht ausgeführt wird. `symlinkDirectories` in Worktree-Konfiguration nutzen um `node_modules` nicht zu kopieren.
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-25

---

## Neue Settings in v2.1.128–v2.1.153 (Änderungsprotokoll)

- **Was:** Zusammenfassung der neusten Ergänzungen seit ca. Anfang 2026:
  - **v2.1.153 (2026-05-28):**
    - `/model` speichert Auswahl jetzt als Default für neue Sessions (nicht mehr nur temporär)
    - `s`-Taste im Picker für session-only Wechsel; Keybinding `modelPicker:setAsDefault` → `modelPicker:thisSessionOnly` umbenennen
    - Statusline-Scripts erhalten `COLUMNS` und `LINES` als Umgebungsvariablen für Terminal-Breite
    - Bugfix: `claude update` (npm) installiert jetzt konfigurierten Release-Channel statt immer latest
  - **v2.1.152 (2026-05-27):**
    - Fallback-Model greift jetzt sessionweit wenn primäres Modell nicht gefunden wird (statt jeden Request zu scheitern)
    - `/bg` und Detach übernehmen `--fallback-model` für Background-Worker
    - `pluginSuggestionMarketplaces` — Neues Managed Setting für Admin-Allowlisting von Plugin-Marketplaces
    - `MessageDisplay` Hook Event — neue Möglichkeit, Ausgaben zu transformieren/ausblenden
    - `SessionStart` Hook kann jetzt `sessionTitle` und `reloadSkills: true` zurückgeben
  - **v2.1.150 (2026-05-23):** Nur interne Infrastruktur, keine User-sichtbaren Änderungen
  - **v2.1.149 (2026-05-22):** `allowAllClaudeAiMcps` (Enterprise) — lädt claude.ai Cloud-MCP-Konnektoren zusammen mit managed-mcp.json. `/usage` zeigt jetzt Kostenaufschlüsselung nach Skills/Subagents/Plugins. GFM-Task-Checkboxen (`- [ ]`) werden nun gerendert.
  - **v2.1.143:** `worktree.bgIsolation` — Datei-Isolierung für Hintergrund-Agenten (`"worktree"` oder `"none"`); `NO_COLOR`/`FORCE_COLOR` müssen jetzt vor Start gesetzt werden, nicht mehr im env-Key
  - **v2.1.136:** `policyHelper` — Ausführbares Script für dynamische Managed-Settings-Berechnung
  - **v2.1.133:** `parentSettingsBehavior` — Steuert Merge-Verhalten von IDE-Extension-Managed-Settings
  - **v2.1.129:** `skillOverrides` — Pro-Skill-Sichtbarkeits-Kontrolle (`"on"`, `"name-only"`, `"user-invocable-only"`, `"off"`)
  - **v2.1.128:** `disableRemoteControl` — Remote-Control-Feature komplett deaktivieren
- **Schema für Autocomplete empfohlen:**
  ```json
  { "$schema": "https://json.schemastore.org/claude-code-settings.json" }
  ```
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell), https://dev.classmethod.jp/en/articles/20260528-claude-code-updates-v2-1-153/ (extern), https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/ (extern)
- **Stand:** 2026-05-28

---

## Worktree-Konfiguration für parallele Sessions

- **Was:** `worktree`-Objekt steuert Git-Worktree-Verhalten bei isolierten Hintergrund-Agenten.
- **Syntax:**
  ```json
  {
    "worktree": {
      "baseRef": "fresh",
      "symlinkDirectories": ["node_modules", ".cache"],
      "sparsePaths": ["packages/my-app"],
      "bgIsolation": "worktree"
    }
  }
  ```
- **bgIsolation-Werte:**
  - `"worktree"` (Standard) — Hintergrund-Jobs dürfen Haupt-Checkout nicht bearbeiten, bis in Worktree gewechselt wird
  - `"none"` — Hintergrund-Jobs dürfen direkt im Working-Copy arbeiten
- **symlinkDirectories:** `node_modules` und `.cache` werden per Symlink geteilt statt kopiert → spart Speicherplatz und Build-Zeit bei parallelen Coder-Agents erheblich.
- **Best Practice:** Für parallele Coder-Agents `"bgIsolation": "worktree"` + `"symlinkDirectories": ["node_modules"]` in Project-Settings. Datei-Ownership je Agent strikt trennen, nie zwei Agents auf die gleiche Datei.
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-25

---

## Quellen-Übersicht

| URL | Typ |
|-----|-----|
| https://code.claude.com/docs/en/settings | offiziell |
| https://code.claude.com/docs/en/statusline | offiziell |
| https://code.claude.com/docs/en/changelog | offiziell |
| https://code.claude.com/docs/en/keybindings | offiziell |
| https://github.com/anthropics/claude-code/releases | offiziell |
| https://github.com/anthropics/claude-code/issues/23478 | offiziell (Bug-Tracker) |
| https://github.com/anthropics/claude-code/issues/21858 | offiziell (Bug-Tracker) |
| https://github.com/anthropics/claude-code/issues/17204 | offiziell (Bug-Tracker) |
| https://github.com/anthropics/claude-code/issues/45453 | offiziell (Bug-Tracker) |
| https://dev.classmethod.jp/en/articles/20260528-claude-code-updates-v2-1-153/ | extern |
| https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/ | extern |
| https://claudefa.st/blog/guide/mechanics/rules-directory | extern |
| https://cld-docs.onlinetool.cc/en/docs/claude-code/statusline.html | extern |
| https://www.eesel.ai/blog/settings-json-claude-code | extern |
| https://managed-settings.com/ | extern |
