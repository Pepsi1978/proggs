# Best Practices: Settings & Konfiguration

> Recherchiert: 2026-05-25 | Claude Code v2.1.150 | Quellen: code.claude.com/docs

---

## Settings-Hierarchie (5 Ebenen)

**Was:** Settings werden in einer festen Prioritätsreihenfolge zusammengeführt. Höhere Ebenen überschreiben niedrigere.

**Best Practice:**
1. Managed Policy (`CLAUDE_CODE_MANAGED_SETTINGS_PATH`) — höchste Priorität, für Unternehmens-Deployments
2. CLI-Argumente (z.B. `--permission-mode`)
3. Lokale Settings (`~/.claude/settings.local.json`) — maschinen-spezifische Overrides, nicht committen
4. Projekt-Settings (`.claude/settings.json` im Repo) — geteilte Team-Einstellungen
5. User-Settings (`~/.claude/settings.json`) — persönliche Defaults, niedrigste Priorität

Deny-Regeln haben in ALLEN Ebenen Vorrang vor Allow-Regeln. Das Permission-System wird vom Claude-Code-Client durchgesetzt — NICHT vom Modell.

**Quelle:** code.claude.com/docs/en/settings, code.claude.com/docs/en/permissions  
**Stand:** v2.1.150

---

## Permission Modes (6 Modi)

**Was:** `defaultMode` in settings.json steuert das globale Genehmigungsverhalten.

**Best Practice:**

| Modus | Wann nutzen |
|-------|-------------|
| `default` | Standard — Claude fragt bei risikoreichen Aktionen |
| `acceptEdits` | Datei-Edits automatisch, aber Bash-Befehle werden gefragt |
| `plan` | Nur Lesen + Planen, kein Schreiben oder Ausführen |
| `auto` | Research Preview — automatischer Klassifizierer (Anthropic API + v2.1.83+ + Sonnet/Opus 4.6+) |
| `dontAsk` | Nie fragen — für CI/CD mit voller Kontrolle |
| `bypassPermissions` | Alle Beschränkungen deaktiviert — nur für vertrauenswürdige lokale Umgebungen |

**Wichtig für `auto`-Modus:**
- Nur via `~/.claude/settings.json` aktivierbar (nicht in Projekt-Settings)
- Separater Classifier-Agent prüft Aktionen bevor sie ausgeführt werden
- Fallback zu manueller Bestätigung nach 3 aufeinanderfolgenden oder 20 Gesamtblockierungen
- Erfordert Anthropic API (nicht über Claude.ai nutzbar)

**Quelle:** code.claude.com/docs/en/permission-modes  
**Stand:** v2.1.83+ (auto-Modus), v2.1.150

---

## effortLevel — Wichtige Falle

**Was:** `effortLevel` in settings.json steuert das Reasoning-Budget (low/medium/high/xhigh).

**Best Practice:**
- `effortLevel` in settings.json für den Default-Wert setzen
- **NIEMALS** `CLAUDE_CODE_EFFORT_LEVEL` als Umgebungsvariable setzen — das blockiert die `/effort`-Befehle während der Session
- Der session-guard sollte `effortLevel` in settings.json setzen, NICHT die Env-Var
- Default für neue Sessions: `high`

```json
{
  "effortLevel": "high"
}
```

**Quelle:** code.claude.com/docs/en/settings  
**Stand:** v2.1.150

---

## Protected Paths — Auto-Approve nie möglich

**Was:** Bestimmte Pfade können in keinem Modus automatisch genehmigt werden (außer bypassPermissions).

**Best Practice:** Diese Pfade NIE in Allow-Regeln aufnehmen — sie sind hart gesperrt:

- `~/.ssh/`
- `~/.aws/`
- `~/.config/`
- `~/.gnupg/`
- `/etc/`
- `~/.claude/` (Ausnahme: CLAUDE.md)
- Alle Pfade die Private Keys oder Credentials enthalten könnten

**Quelle:** code.claude.com/docs/en/permission-modes  
**Stand:** v2.1.150

---

## Permission Rules — Syntax & Best Practices

**Was:** Allow/Deny-Regeln für spezifische Tools, Pfade und Aktionen.

**Best Practice:**

```json
{
  "permissions": {
    "allow": [
      "Bash(git:*)",
      "Bash(npm run:*)",
      "Read(~/.claude/CLAUDE.md)"
    ],
    "deny": [
      "Bash(rm -rf:*)",
      "Write(~/.ssh/*)"
    ]
  }
}
```

- Tool-spezifische Regeln: `Tool(pattern:*)` oder `Tool(pattern)`
- Deny-Regeln überschreiben immer Allow-Regeln (auch in niedrigeren Ebenen)
- Regeln gelten für den Claude-Code-Client, nicht das Modell selbst
- Für Bash: Regeln basieren auf dem Befehlsprefix

**Quelle:** code.claude.com/docs/en/permissions  
**Stand:** v2.1.150

---

## statusLine — Statuszeile konfigurieren

**Was:** `statusLine` definiert ein Shell-Kommando dessen Ausgabe in der Claude-Code-Statuszeile angezeigt wird.

**Best Practice:**
```json
{
  "statusLine": "bash ~/.claude/hooks/statusline.sh"
}
```

- Typ muss `"command"` sein (nicht `"prompt"`)
- Hot Reload: Änderungen an der Skript-Datei werden sofort wirksam ohne Neustart
- Ausgabe sollte kurz sein (max ~100 Zeichen für gute Darstellung)
- Für Windows: PowerShell-Skript via `pwsh -File ~/.claude/hooks/statusline.ps1`

**Quelle:** code.claude.com/docs/en/settings  
**Stand:** v2.1.150

---

## Neue Settings in v2.1.x (Übersicht)

**Was:** Seit ca. Januar 2026 hinzugekommene Settings-Felder.

**Best Practice — wichtigste neue Felder:**

| Setting | Version | Zweck |
|---------|---------|-------|
| `autoMemoryEnabled` | v2.1.59+ | Auto-Memory-System aktivieren (MEMORY.md) |
| `autoMemoryDirectory` | v2.1.59+ | Alternativer Pfad für Memory-Dateien |
| `claudeMdExcludes` | Neu | Glob-Muster um CLAUDE.md-Dateien auszuschließen |
| `skillOverrides` | v2.1.129+ | Skills deaktivieren oder konfigurieren |
| `policyHelper` | v2.1.136+ | Hilft bei Permission-Regelkonfiguration |
| `parentSettingsBehavior` | v2.1.143+ | Steuert Vererbung von Parent-Settings |
| `maxSkillDescriptionChars` | Neu | Max Zeichen für Skill-Beschreibungen |
| `skillListingBudgetFraction` | Neu | Anteil des Kontextfensters für Skill-Listings |
| `tui` | Neu | Terminal UI Konfiguration |
| `viewMode` | Neu | Anzeigemodus (compact/verbose) |
| `alwaysThinkingEnabled` | Neu | Extended Thinking immer aktivieren |
| `showThinkingSummaries` | Neu | Thinking-Zusammenfassungen anzeigen |
| `worktree.bgIsolation` | Neu | Hintergrund-Isolierung für Worktrees |

**Quelle:** code.claude.com/docs/en/settings  
**Stand:** v2.1.150

---

## Sandboxing als Ergänzung zu Permissions

**Was:** OS-level Sandboxing (macOS Sandbox, Linux seccomp/namespaces) als zweite Sicherheitsschicht.

**Best Practice:**
- Permissions = Claude-Code-Client-Durchsetzung (Softlayer)
- Sandboxing = OS-Durchsetzung (Hardlayer)
- Beide zusammen für maximale Sicherheit in Multi-User- oder CI-Umgebungen
- Sandboxing verhindert auch, dass das Modell Restrictions umgeht
- `dontAsk` + Sandboxing = sichere Vollautomatisierung

**Quelle:** code.claude.com/docs/en/permissions  
**Stand:** v2.1.150

---

## PowerShell Tool (Windows)

**Was:** Neues Tool für native PowerShell-Ausführung unter Windows.

**Best Practice:**
- Aktivierung via Umgebungsvariable: `CLAUDE_CODE_USE_POWERSHELL_TOOL=1`
- Ermöglicht PowerShell-Befehle ohne Git Bash Umweg
- Besonders nützlich für Windows-spezifische Hooks und Verwaltungsaufgaben
- Separate Permission-Regeln für PowerShell-Tool nötig

**Quelle:** code.claude.com/docs/en/settings  
**Stand:** v2.1.150

---

## outputStyle — Ausgabeformat

**Was:** Steuert das Ausgabeformat von Claude Code.

**Best Practice:**
```json
{
  "outputStyle": "verbose"
}
```

- `verbose`: Detaillierte Ausgabe mit Tool-Aufrufen sichtbar (empfohlen für Entwicklung)
- `compact`: Weniger Ausgabe, nur Ergebnisse
- Standard: `verbose`
- Kann per CLI-Argument überschrieben werden: `--output-style compact`

**Quelle:** code.claude.com/docs/en/settings  
**Stand:** v2.1.150

---

## .claude/rules/ — Pfad-spezifische Regeln

**Was:** Markdown-Dateien in `.claude/rules/` werden kontextuell geladen — nur wenn relevante Dateien bearbeitet werden.

**Best Practice:**
```yaml
---
paths:
  - "src/android/**"
  - "*.kt"
---
# Android-spezifische Regeln
Verwende immer ktfmt für Formatierung.
```

- YAML-Frontmatter mit `paths:` Glob-Muster
- Regeln werden nur geladen wenn Pfade im Kontext relevant sind
- Reduziert Kontext-Overhead gegenüber allem in CLAUDE.md
- Verzeichnis: `.claude/rules/` im Projekt-Root

**Quelle:** code.claude.com/docs/en/memory  
**Stand:** v2.1.150
