# Settings & Konfig — Best Practices (Stand 2026-06-05, Claude Code 2.1.165)

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | settings.json schreiben | JSON validieren + BOM-frei — ein Fehler/BOM killt ALLE Settings still | Konfigurationshierarchie |
| 2 | Permissions | Sperren nur via `deny` (`allow` ist KEINE Whitelist) | Permission-Modes |
| 3 | `effortLevel` | ueber das `effortLevel`-Setting, NIE `CLAUDE_CODE_EFFORT_LEVEL`-Env | effortLevel |
| 4 | Praezedenz | Managed > CLI > local > project > user; Permission-Regeln MERGEN | Konfigurationshierarchie |
| 5 | `model`/`outputStyle` | nicht live-reloaded — Neustart noetig | /model |
| 6 | user-level rules `paths:` | nie user-level (ignoriert) — nur projektweit | .claude/rules paths: |
| 7 | env-Vars | `NO_COLOR`/`FORCE_COLOR` wirken nur fuer Subprozesse (ab v2.1.143) | Umgebungsvariablen |
| 8 | `CLAUDE_CONFIG_DIR` gesetzt | eigener Login je Ordner (macOS: Schluesselbund-Hash) — spiegeln statt neu anmelden | CLAUDE_CONFIG_DIR |

---

## CLAUDE_CONFIG_DIR — ein Konfigurationsordner, eine Anmeldung

`CLAUDE_CONFIG_DIR` verschiebt nicht nur die Einstellungen, sondern die **komplette Identitaet**
der Sitzung: Login, Kontodaten, Sitzungshistorie, Projekte, Cache. Wer damit Profile baut
(z. B. OpenLauncher `minimal`/`standard`/`strict`), baut faktisch getrennte Benutzerkonten.

**Wo der Login liegt**

| Plattform | Speicherort |
|-----------|-------------|
| macOS | Schluesselbund, Dienst `Claude Code-credentials-<sha256(CLAUDE_CONFIG_DIR)[:8]>` — der Standardordner `~/.claude` nutzt den Namen **ohne** Suffix |
| Windows | Datei `<configdir>\.credentials.json` |

Dazu kommt die Kontoidentitaet (`oauthAccount`, `userID`, `hasCompletedOnboarding`) in
`.claude.json` — beim Standardordner als `~/.claude.json` **daneben**, bei gesetztem
`CLAUDE_CONFIG_DIR` **darin**. Fehlt eines von beidem, erscheint der Anmeldebildschirm.

**Konsequenzen fuer die Praxis**

- Jedes neue Profil verlangt genau eine Erstanmeldung — das ist kein Fehler, sondern die Bauart.
- Der Dienstname haengt am **Pfad-String**. Ein umbenanntes Home, ein verschobenes Repo oder ein
  abweichend geschriebener Pfad (`~/…` vs. `/Users/…`, Schrägstrich am Ende) erzeugt lautlos einen
  neuen Hash → wieder Anmeldung. Den Pfad im Startskript deshalb **immer identisch** aufbauen.
- Erwarteten Dienstnamen pruefen:
  `python3 -c "import hashlib;print(hashlib.sha256(b'<configdir>').hexdigest()[:8])"`,
  Bestand: `security dump-keychain | grep "Claude Code-credentials"`.
- Wer mehrere Profile mit **einem** Konto fahren will, spiegelt den Login aktiv
  (hier: `~/.claude/hooks/claude-login-sync.py`, eingehaengt im Startskript sowie als
  SessionStart-/SessionEnd-Hook). Beim Spiegeln nur dort schreiben, wo gar kein oder ein
  vollstaendig abgelaufener Login liegt — sonst ueberschreibt man ein bewusst getrenntes Konto
  oder einen laufenden Token-Refresh.
- Token beim Schreiben nie ueber `argv` uebergeben (steht in der Prozessliste) — auf macOS
  `security -i` von stdin fuettern.
- Getrennte Profile sind auch ein **Vorteil**: zwei Konten parallel ohne Ab- und Anmelden.

**Quelle:** eigener Vorfall 2026-08-27 (Claude Code 2.1.247, macOS) · vgl.
`bugs/claude-tooling/claude-config.md` §3.9

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
- **NEU ab v2.1.154 — CLAUDE_CODE_ALWAYS_ENABLE_EFFORT:** Wenn auf `1` gesetzt, sendet Claude Code den `effort`-Parameter auch an Modelle die ihn nicht offiziell unterstützen. Nützlich für Custom-Endpoints oder experimentelle Modelle. Standardmäßig wird der Parameter bei nicht-unterstützten Modellen weggelassen.
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell), https://github.com/anthropics/claude-code/issues/45453 (offiziell, Bug-Tracker)
- **Stand:** 2026-05-30 (CLAUDE_CODE_ALWAYS_ENABLE_EFFORT neu in v2.1.154)

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
  - `CLAUDE_CODE_ALWAYS_ENABLE_EFFORT` — Effort-Parameter auch an nicht-unterstützte Modelle senden (neu v2.1.154)
  - `OTEL_LOG_TOOL_DETAILS` — Wenn `1`, werden `tool_parameters` in `tool_decision`- und `tool_result`-Telemetrie-Events eingeschlossen (neu v2.1.157)
  - `CLAUDE_CODE_ENABLE_AUTO_MODE` — Auto-Mode auf Bedrock/Vertex/Foundry opt-in (neu v2.1.158, nur Opus 4.7/4.8)
- **DEPRECATED ab v2.1.154 (entfernt 2026-06-01):** `CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE` — nicht mehr verwenden. Wird mit v2.1.154 deprecated und am 1. Juni 2026 entfernt. Fast Mode ist jetzt allgemein verfügbar und wird über andere Mechanismen gesteuert.
- **WICHTIG (seit v2.1.143):** `NO_COLOR` und `FORCE_COLOR` müssen VOR dem Start von Claude Code gesetzt werden — nicht über den `env`-Key in settings.json.
- **Best Practice:** Team-übergreifende Env-Vars in Project-Settings `.claude/settings.json`. Geheime Tokens nur in `.claude/settings.local.json` oder über `apiKeyHelper`-Script, niemals in geteilten Dateien.
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-30 (CLAUDE_CODE_ALWAYS_ENABLE_EFFORT, OTEL_LOG_TOOL_DETAILS, CLAUDE_CODE_ENABLE_AUTO_MODE neu)

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
- **GEMESSENES Vollschema (Claude Code 2.1.246, Stand 2026-08-26)** — direkt aus dem echten
  stdin abgegriffen, nicht aus der Doku uebernommen. Die offizielle Feldliste oben ist
  unvollstaendig; diese Felder kommen tatsaechlich an:
  ```
  session_id · transcript_path · cwd · prompt_id · session_name · version
  effort.level                          -> "low"|"medium"|"high"|"xhigh" (LIVE-Wert von /effort,
                                           NICHT der Default aus settings.json)
  model.id · model.display_name
  workspace.current_dir · workspace.project_dir
  workspace.repo.host · .owner · .name  -> Git-Remote, z.B. github.com / Pepsi1978 / proggs
  output_style.name                     -> z.B. "Proactive"
  thinking.enabled                      -> true/false
  cost.total_cost_usd                   -> Float, z.B. 1.8090355 (siehe Float-Warnung unten)
  cost.total_duration_ms · cost.total_api_duration_ms
  cost.total_lines_added · cost.total_lines_removed
  context_window.total_input_tokens · .total_output_tokens · .context_window_size
  context_window.current_usage.{input,output,cache_creation_input,cache_read_input}_tokens
  context_window.used_percentage · .remaining_percentage
  rate_limits.five_hour.used_percentage · .resets_at   (Unix-Sekunden)
  rate_limits.seven_day.used_percentage · .resets_at   (Unix-Sekunden)
  ```
  - **Selbst nachmessen** (wenn eine neue CLI-Version neue Felder bringt): im Statusline-Script
    einmalig `[ -f /tmp/sl.dump ] || printf "%s" "$input" > /tmp/sl.dump` direkt nach
    `input=$(cat)` einfuegen, 1 s warten, dann
    `jq -r 'paths(scalars) as $p | "\($p|join("."))"' /tmp/sl.dump` — und die Zeile
    **sofort wieder entfernen**.
  - ⚠️ **`rate_limits` fehlt beim allerersten Aufruf einer Session** (kommt erst nach dem ersten
    API-Response). Wer daraus einen Cross-Session-State baut, muss diesen Zustand abfangen —
    sonst gewinnen alte State-Dateien die Aggregation (siehe `bugs/claude-tooling/claude-hooks.md` §13.8).
  - ⚠️ **Float-Warnung:** `cost.total_cost_usd` und `used_percentage` kommen als Float mit
    Artefakten (`55.00000000000001`). Die bash-3.2-builtin `printf "%.2f"` auf macOS scheitert
    daran und gibt **0** aus (§13.5). Loesung: in jq zu einer Ganzzahl runden (Cent bzw. Prozent)
    und in der Shell nur mit Integer-Arithmetik formatieren.
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
- **Stand:** 2026-08-26 (Vollschema gemessen an Claude Code 2.1.246; COLUMNS/LINES seit v2.1.153)

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

## Auto-Mode — Konfiguration & settings.json-Block (NEU / ERWEITERT in v2.1.158)

- **Was:** Auto-Mode lässt Claude Code ohne Permission-Prompts laufen, indem jeder Tool-Call durch einen KI-Sicherheits-Klassifizierer geroutet wird. Dieser blockiert irreversible, destruktive oder umgebungsfremde Aktionen automatisch.
- **Aktivierung (Anthropic API):** `/model`-Picker oder CLI-Flag `--enable-auto-mode`, dann Shift+Tab zum Wechseln.
- **NEU ab v2.1.158 — Bedrock / Vertex / Foundry:** Auto-Mode ist jetzt auch für Opus 4.7 und Opus 4.8 auf AWS Bedrock, Google Vertex AI und Microsoft Azure Foundry verfügbar. Opt-in über Umgebungsvariable:
  ```bash
  CLAUDE_CODE_ENABLE_AUTO_MODE=1
  ```
  Vorher war Auto-Mode ausschließlich über die Anthropic-API verfügbar.
- **Sicherheitsarchitektur:** Der Auto-Mode-Klassifizierer läuft als **zweite Sicherheitsstufe NACH** dem normalen Permissions-System. Aktionen die in `permissions.deny` stehen, werden bereits davor blockiert und erreichen den Klassifizierer nicht.
- **autoMode-Block in settings.json (Konfigurationsreferenz):**
  ```json
  {
    "autoMode": {
      "environment": [
        "$defaults",
        "Organization: Acme Corp. Primary use: software development",
        "Source control: github.com/acme-corp and all repos under it",
        "Trusted internal domains: *.internal.acme.com, api.acme.com"
      ],
      "allow": [
        "$defaults",
        "Deploying to staging namespace is allowed: isolated, resets nightly"
      ],
      "soft_deny": [
        "$defaults",
        "Never run db migrations outside migrations CLI"
      ],
      "hard_deny": [
        "$defaults",
        "Never send repository contents to third-party APIs"
      ]
    }
  }
  ```
- **$defaults-Platzhalter (KRITISCH):** In `environment`, `allow`, `soft_deny` und `hard_deny` können die eingebauten Standardregeln mit `"$defaults"` an einer beliebigen Position eingebunden werden. Wer `"$defaults"` weglässt, ersetzt die gesamte eingebaute Regelliste — inkl. Force-Push-Schutz, curl-pipe-bash-Schutz, Produktions-Deploy-Schutz. **Niemals eine Sektion ohne `"$defaults"` setzen, ohne vorher `claude auto-mode defaults` ausgeführt zu haben.**
- **Scope des autoMode-Blocks:** Wird aus `~/.claude/settings.json` (User), `.claude/settings.local.json` (per Projekt, gitignoriert), Managed-Settings und `--settings`-Flag gelesen. **Nicht** aus `.claude/settings.json` (shared Project-Settings) — verhindert, dass ein Repo eigene Auto-Mode-Allow-Regeln injiziert.
- **Diagnose-Kommandos:**
  ```bash
  claude auto-mode defaults   # Zeigt eingebaute Regeln als JSON
  claude auto-mode config     # Zeigt effektive Konfiguration nach Merge
  claude auto-mode critique   # KI-Feedback zu eigenen Regeln
  ```
- **Prioritäten im Klassifizierer:**
  1. `hard_deny` — blockiert immer, kein Override möglich
  2. `soft_deny` — blockiert, außer `allow` oder explizite Benutzerabsicht überschreibt
  3. `allow` — Ausnahme zu `soft_deny`
  4. Explizite Benutzerabsicht — überschreibt `soft_deny` (nicht `hard_deny`)
- **WICHTIG für Frank (bypassPermissions-User):** Auto-Mode und `bypassPermissions` erfüllen unterschiedliche Zwecke und schließen sich nicht gegenseitig aus:
  - `bypassPermissions` = kein Permission-Dialog, aber kein Sicherheits-Klassifizierer
  - Auto-Mode = kein Permission-Dialog + aktiver Sicherheits-Klassifizierer im Hintergrund
  - Für Solo-Entwickler mit vollem Verständnis der Risiken: `bypassPermissions` ist weiterhin die einfachere Wahl (kein Overhead durch Klassifizierer). Auto-Mode ist besonders nützlich in Team- oder Enterprise-Umgebungen oder wenn Bedrock/Vertex/Foundry genutzt wird.
- **Quelle:** https://code.claude.com/docs/en/auto-mode-config (offiziell), https://code.claude.com/docs/en/permission-modes (offiziell), https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026-05-30 (Bedrock/Vertex/Foundry-Support neu in v2.1.158)

---

## Workflow-Keyword-Trigger — Deaktivierung in /config (NEU ab v2.1.157/158)

- **Was:** Claude Code hat ein "Dynamic Workflows"-Feature das beim Erkennen des Schlüsselworts `workflow` (oder eines verwandten Ausdrucks) in einem Prompt automatisch einen Workflow-Modus auslöst. Ab v2.1.157/158 gibt es in `/config` eine neue Einstellung um dieses automatische Triggern zu deaktivieren.
- **Problem:** Das Wort "workflow" kommt in normalen Prompts häufig vor (z.B. "mein Git-Workflow", "CI/CD-Workflow erklären") — das ungewollte Auslösen des Workflow-Modus störte den normalen Gesprächsfluss.
- **Fix:** In `/config` die Option **"Workflow keyword trigger"** deaktivieren. Danach löst das Wort `workflow` in einem Prompt keinen Dynamic-Workflow mehr aus.
- **Hintergrund Dynamic Workflows (v2.1.154):** Anthropic hat mit Claude Opus 4.8 "Dynamic Workflows" eingeführt — ein Orchestrierungs-Feature für sehr große Aufgaben mit bis zu 1.000 Sub-Agenten. Der Workflow wird durch ein Schlüsselwort im Prompt gestartet.
- **Empfehlung:** Die Einstellung deaktivieren wenn das Wort `workflow` regelmäßig in normalen Prompts auftaucht (z.B. bei Voice-Diktation).
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell), https://techcrunch.com/2026/05/28/anthropic-releases-opus-4-8-with-new-dynamic-workflow-tool/ (extern)
- **Stand:** 2026-05-30

---

## agent-Feld in settings.json für Dispatched Sessions (NEU ab v2.1.154/158)

- **Was:** Das `agent`-Feld in `settings.json` wird ab v2.1.154/158 auch für **dispatched sessions** (Sub-Agenten, Hintergrund-Agenten, `/bg`-Läufe) berücksichtigt.
- **Vorheriges Verhalten:** Das `agent`-Feld galt nur für die primäre interaktive Session. Dispatched Sub-Agenten ignorierten es und nutzten den globalen Default.
- **Neues Verhalten:** Wenn eine Session via `--settings` ein JSON mit einem `agent`-Feld übergibt, gilt dieses auch für alle von dieser Session erzeugten Sub-Agenten (dispatched sessions).
- **Anwendungsfall:** Multi-Agenten-Workflows wo Sub-Agenten ein spezifisches Modell, Effort-Level oder andere agent-spezifische Settings erhalten sollen:
  ```json
  {
    "agent": {
      "model": "claude-sonnet-4-5",
      "effortLevel": "medium"
    }
  }
  ```
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026-05-30

---

## OTEL-Telemetrie — tool_parameters in Logs (NEU ab v2.1.157)

- **Was:** OpenTelemetry-Telemetrie-Events enthalten jetzt optional die vollständigen `tool_parameters` (Bash-Befehle, MCP/Skill-Namen, etc.).
- **Aktivierung:**
  ```bash
  OTEL_LOG_TOOL_DETAILS=1
  ```
  In settings.json:
  ```json
  {
    "env": {
      "OTEL_LOG_TOOL_DETAILS": "1"
    }
  }
  ```
- **Betrifft diese Events:**
  - `tool_decision` — Enthält jetzt `tool_parameters` (welche Parameter der Tool-Call hatte)
  - `tool_result` — Enthält jetzt ebenfalls `tool_parameters` (gated, nur wenn Variable gesetzt)
- **Vorheriges Verhalten:** `tool_parameters` wurden in Telemetrie-Events nicht mitgeloggt — nur Tool-Name und Ergebnis-Status.
- **Datenschutz-Hinweis:** `OTEL_LOG_TOOL_DETAILS=1` loggt Bash-Befehle im Klartext in die Telemetrie. Nicht in Umgebungen einschalten wo Befehle sensitive Daten enthalten könnten (z.B. Passworte als CLI-Argumente).
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell)
- **Stand:** 2026-05-30

---

## Neue Settings in v2.1.128–v2.1.158 (Änderungsprotokoll)

- **Was:** Zusammenfassung der neuesten Ergänzungen:
  - **v2.1.158 (2026-05-30):**
    - Auto-Mode auf Bedrock, Vertex, Foundry für Opus 4.7/4.8 via `CLAUDE_CODE_ENABLE_AUTO_MODE=1`
    - "Workflow keyword trigger"-Einstellung in `/config` zum Deaktivieren des automatischen Workflow-Starts
    - `agent`-Feld in settings.json wird jetzt für dispatched sessions berücksichtigt
    - Worktrees beim Agent-Ende entsperrt (für `git worktree remove/prune`)
    - PowerShell-Tool auf Bedrock/Vertex/Foundry (Windows) standardmäßig aktiv
  - **v2.1.157 (2026-05-29):**
    - `tool_decision`-Telemetrie-Events enthalten `tool_parameters` wenn `OTEL_LOG_TOOL_DETAILS=1`
    - `tool_parameters` in `tool_result`-Events ebenfalls hinter `OTEL_LOG_TOOL_DETAILS=1` gated
  - **v2.1.154 (2026-05-28):**
    - Claude Opus 4.8 als neues Modell mit xhigh Effort
    - Dynamic Workflows (`/workflows`) für Agent-Orchestrierung mit bis zu 1.000 Sub-Agenten
    - `CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE` **deprecated** (Entfernung am 2026-06-01)
    - `CLAUDE_CODE_ALWAYS_ENABLE_EFFORT=1` — Effort-Parameter auch an nicht-unterstützte Modelle senden
    - `MCP_TOOL_TIMEOUT` wird jetzt auch für Remote HTTP/SSE MCP-Server berücksichtigt
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
- **Stand:** 2026-05-30

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
- **NEU ab v2.1.158:** Von Claude Code verwaltete Worktrees werden beim Beenden des Agenten **entsperrt** (unlocked), sodass `git worktree remove` und `git worktree prune` diese danach aufräumen können. Vorher blieben die Worktrees gesperrt und mussten manuell entsperrt werden.
- **Best Practice:** Für parallele Coder-Agents `"bgIsolation": "worktree"` + `"symlinkDirectories": ["node_modules"]` in Project-Settings. Datei-Ownership je Agent strikt trennen, nie zwei Agents auf die gleiche Datei.
- **Quelle:** https://code.claude.com/docs/en/settings (offiziell)
- **Stand:** 2026-05-30 (Worktree-Unlocking neu in v2.1.158)

---

## Quellen-Übersicht

| URL | Typ |
|-----|-----|
| https://code.claude.com/docs/en/settings | offiziell |
| https://code.claude.com/docs/en/statusline | offiziell |
| https://code.claude.com/docs/en/changelog | offiziell |
| https://code.claude.com/docs/en/keybindings | offiziell |
| https://code.claude.com/docs/en/auto-mode-config | offiziell |
| https://code.claude.com/docs/en/permission-modes | offiziell |
| https://github.com/anthropics/claude-code/releases | offiziell |
| https://github.com/anthropics/claude-code/issues/23478 | offiziell (Bug-Tracker) |
| https://github.com/anthropics/claude-code/issues/21858 | offiziell (Bug-Tracker) |
| https://github.com/anthropics/claude-code/issues/17204 | offiziell (Bug-Tracker) |
| https://github.com/anthropics/claude-code/issues/45453 | offiziell (Bug-Tracker) |
| https://github.com/anthropics/claude-code/issues/41179 | offiziell (Bug-Tracker) |
| https://dev.classmethod.jp/en/articles/20260528-claude-code-updates-v2-1-153/ | extern |
| https://dev.classmethod.jp/en/articles/20260524-claude-code-updates-v2-1-152/ | extern |
| https://claudefa.st/blog/guide/mechanics/rules-directory | extern |
| https://cld-docs.onlinetool.cc/en/docs/claude-code/statusline.html | extern |
| https://www.eesel.ai/blog/settings-json-claude-code | extern |
| https://managed-settings.com/ | extern |
| https://techcrunch.com/2026/05/28/anthropic-releases-opus-4-8-with-new-dynamic-workflow-tool/ | extern |
| https://www.marktechpost.com/2026/05/28/anthropic-ships-claude-opus-4-8-alongside-dynamic-workflows-and-cheaper-fast-mode-with-workflows-capped-at-1000-subagents/ | extern |

<!-- CHECKPOINT: fertig — alle Einträge von v2.1.154 bis v2.1.158 eingearbeitet. Nächste Recherche: Auto-Mode Details wenn Frank auf Bedrock/Vertex wechselt; Dynamic Workflows /workflows Command-Referenz. -->

---

### Update 2026-06-05 (Claude Code 2.1.165) — Settings & Permissions

**1. Managed Settings `requiredMinimumVersion` / `requiredMaximumVersion` (2.1.163)**
- **Was:** Zwei neue Felder in `managed-settings.json`. Claude Code startet nicht, wenn die Version ausserhalb des Bereichs liegt, und verweist auf eine genehmigte Version.
- **macOS-Pfad:** `/Library/Application Support/ClaudeCode/managed-settings.json` (Drop-in: `.../managed-settings.d/*.json`). Managed Settings sind von keiner anderen Ebene ueberschreibbar.
- **Best Practice (Solo):** `requiredMinimumVersion` als Policy-Lock setzen, damit ein versehentliches `claude update` ein versionsabhaengiges Hook/Skills-Setup nicht bricht.
- **Quelle:** github.com/anthropics/claude-code/releases/tag/v2.1.163 `[offiziell]`

**2. Prompts vor Shell-Startup- & Build-Config-Dateien (2.1.160)**
- **Was:** Prompt vor Schreibzugriff auf `.zshenv`, `.zlogin`, `.bash_login`, `~/.config/git/`; im `acceptEdits`-Modus zusaetzlich vor `.npmrc`, `.yarnrc*`, `bunfig.toml`, `.bazelrc`, `.pre-commit-config.yaml`, `.devcontainer/`.
- **WICHTIG fuer bypassPermissions:** Der Prompt greift NICHT bei `bypassPermissions` — der Modus skippt alle Permission-Prompts. Franks Hooks in `~/.claude/` schreiben unveraendert ohne Prompt. Wer trotzdem Schutz will: PreToolUse-Hook, der Schreibzugriffe auf `.zshenv`/`.bash_login` prueft (Hook-Entscheidung gilt unabhaengig vom Modus).
- **Quelle:** code.claude.com/docs/en/permissions `[offiziell]`

**3. WebFetch-Permission-Vorrang (2.1.162)**
- **Was:** Explizite `WebFetch(domain:...)` deny/ask/allow-Regeln haben jetzt Vorrang vor eingebauten vorab-genehmigten Domains.
- **Best Practice:** Unerwuenschte Domains zuverlaessig per `"deny": ["WebFetch(domain:...)"]` sperrbar.
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**4. `CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE` entfernt (2.1.160)**
- **Was:** Env-Var ist jetzt No-op. Fast Mode laeuft ueber `/fast` (auf Opus 4.8 empfohlen).
- **Best Practice:** Falls der Eintrag noch im `env`-Block von `~/.claude/settings.json` steht — entfernen (toter Code).
- **Quelle:** code.claude.com/docs/en/fast-mode `[offiziell]`

**Betrifft eigene Werkzeuge:** Punkt 2 betrifft das `bypassPermissions`-Setup NICHT (Hooks schreiben weiter ohne Prompt). Punkt 1 (`requiredMinimumVersion`) ist als Update-Schutz nuetzlich. Punkt 4: evtl. toten Env-Eintrag entfernen.
